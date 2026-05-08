package com.erp.controller.settlement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.*;
import com.erp.controller.settlement.dto.*;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.service.finance.FinanceService;
import com.erp.service.finance.InvoiceNoticeService;
import com.erp.service.settlement.SettlementService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 财务管理控制器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/finance")
@Api(tags = "财务管理")
@Validated
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private InvoiceNoticeService invoiceNoticeService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 获取合同关联的可结算运输记录
     */
    @GetMapping("/transports/{contractCode}")
    @ApiOperation(value = "获取合同关联的可结算运输记录",
                  notes = "根据合同号获取关联的通知单→运输单，且这些运输单未被其他结算单引用")
    public Result<TransportRecordResponseDTO> getTransportRecords(@PathVariable String contractCode) {
        try {
            List<TransportRecordDTO> transportRecords = financeService.getTransportRecordsByContract(contractCode);
            TransportRecordResponseDTO response = new TransportRecordResponseDTO();
            response.setTransportRecords(transportRecords);
            return Result.success("获取运输记录成功", response);
        } catch (Exception e) {
            log.error("获取运输记录失败：contractCode={}", contractCode, e);
            return Result.error("获取运输记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取合同关联的可结算入库单
     */
    @GetMapping("/settlements/available-warehousing/{contractCode}")
    @ApiOperation(value = "获取合同关联的可结算入库单", notes = "根据合同号获取关联的通知单→运输单→入库单，且这些入库单未被其他结算单引用")
    public Result<List<AvailableWarehousingVO>> getAvailableWarehousing(@PathVariable String contractCode) {
        try {
            List<AvailableWarehousingVO> data = financeService.getAvailableWarehousingByContract(contractCode);
            return Result.success("获取可结算入库单成功", data);
        } catch (Exception e) {
            log.error("获取可结算入库单失败：contractCode={}", contractCode, e);
            return Result.error("获取可结算入库单失败：" + e.getMessage());
        }
    }

    /**
     * 获取入库单对应的危废明细
     */
    @PostMapping("/warehousing-waste-details")
    @ApiOperation(value = "获取入库单对应的危废明细", notes = "根据入库单号列表获取对应的危废明细，用于结算单合并")
    public Result<List<WarehousingWasteDetailVO>> getWarehousingWasteDetails(@RequestBody List<String> warehousingCodes) {
        try {
            List<WarehousingWasteDetailVO> data = financeService.getWarehousingWasteDetailsByCodes(warehousingCodes);
            return Result.success("获取入库单危废明细成功", data);
        } catch (Exception e) {
            log.error("获取入库单危废明细失败：warehousingCodes={}", warehousingCodes, e);
            return Result.error("获取入库单危废明细失败：" + e.getMessage());
        }
    }

    /**
     * 获取累积已结算量和合同计划总量
     */
    @GetMapping("/settlements/accumulated-quantity/{contractCode}/{wasteCategory}")
    @ApiOperation(value = "获取累积已结算量", notes = "根据合同号和废物类别获取累积已结算量和合同计划总量")
    public Result<AccumulatedQuantityDTO> getAccumulatedQuantity(
            @PathVariable @NotBlank(message = "合同号不能为空") String contractCode,
            @PathVariable @NotBlank(message = "废物类别不能为空") String wasteCategory) {
        try {
            AccumulatedQuantityDTO data = settlementService.getAccumulatedQuantity(contractCode, wasteCategory);
            return Result.success("获取累积已结算量成功", data);
        } catch (Exception e) {
            log.error("获取累积已结算量失败：contractCode={}, wasteCategory={}", contractCode, wasteCategory, e);
            return Result.error("获取累积已结算量失败：" + e.getMessage());
        }
    }

    /**
     * 导出结算汇总表（收款结算 + 付款结算，汇总字段 + 按量结算明细）
     *
     * 功能描述：
     * 1. 导出所有结算单的汇总信息（收款结算 + 付款结算，不分页）；
     * 2. 对于“按量结算”模式的危废明细，在同一张表中按明细粒度展开，
     *    左侧为结算单汇总字段，右侧为明细字段；
     * 3. 同一结算单的汇总字段（结算单单号、合同号、甲方信息、结算类型、关联来源、金额、状态、制单人）做纵向合并。
     * 接口地址：/api/finance/settlements/export-summary
     * 请求方式：GET
     *
     * 返回：Excel 文件流（单 Sheet）
     */
    @RequireActionPermission({
            "合同结算:危险废物结算-收款结算:导出汇总表",
            "合同结算:危险废物结算-付款结算:导出汇总表"
    })
    @GetMapping("/settlements/export-summary")
    @ApiOperation(value = "导出结算汇总表", notes = "导出收款结算和付款结算的所有结算单汇总信息（仅汇总字段）")
    public void exportSettlementSummary(
            @RequestParam(value = "creatorFilter", required = false) Integer creatorFilter,
            HttpServletRequest request,
            HttpServletResponse response) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean exportSuccess = false;
        String errorMessage = null;

        // 后端安全校验数据范围：根据当前员工的 viewScope 决定是否限制 creatorId
        // 超级管理员始终导出全部；非管理员由权限系统在 viewScope=SELF 时强制过滤
        Integer resolvedCreatorId = settlementService.resolveExportCreatorFilter(userId, creatorFilter);

        try {
            List<SettlementExportSummaryDTO> summaryList = settlementService.getSettlementExportSummary(resolvedCreatorId);
            List<SettlementExportDetailDTO> detailList = settlementService.getSettlementExportDetails();
            writeSettlementSummaryExcel(summaryList, detailList, response);
            exportSuccess = true;
        } catch (Exception e) {
            log.error("导出结算汇总表失败", e);
            errorMessage = e.getMessage();
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("导出失败：" + e.getMessage());
            } catch (Exception ex) {
                log.error("写入结算汇总导出错误响应失败", ex);
            }
        } finally {
            // 记录操作日志
            try {
                String logContent = "导出结算汇总表";
                logRecordService.recordOperationLog("结算单管理", "导出",
                        logContent, userId, ipAddress, exportSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录导出结算汇总表操作日志失败", logEx);
            }

            // 发送消息通知（使用基于权限的通知方法，仅成功时）
            if (exportSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "SETTLEMENT_SUMMARY",
                            null,
                            "结算汇总表已导出",
                            "导出",
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送结算汇总表导出通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 写入结算汇总表 Excel（汇总 + 按量结算明细，单 Sheet 展示）
     */
    private void writeSettlementSummaryExcel(List<SettlementExportSummaryDTO> summaryList,
                                             List<SettlementExportDetailDTO> detailList,
                                             HttpServletResponse response) throws Exception {
        String fileName = "结算汇总表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            // 基础字体
            Font font = workbook.createFont();
            font.setFontName("宋体");
            font.setCharSet(Font.DEFAULT_CHARSET);

            // 通用单元格样式
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 表头样式
            Font headerFont = workbook.createFont();
            headerFont.setFontName("宋体");
            headerFont.setCharSet(Font.DEFAULT_CHARSET);
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.cloneStyleFrom(cellStyle);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 金额/数量数字样式
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(cellStyle);
            DataFormat dataFormat = workbook.createDataFormat();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            // ===== 单 Sheet：结算汇总 + 按量结算明细 =====
            Sheet summarySheet = workbook.createSheet("结算汇总表");

            Row summaryHeaderRow = summarySheet.createRow(0);
            String[] headers = new String[]{
                    // 汇总字段（结算单级）
                    "结算单单号",          // 0
                    "合同号",              // 1
                    "甲方名称",            // 2
                    "甲方统一社会信用代码", // 3
                    "结算类型",            // 4
                    "关联来源",            // 5
                    "结算金额",            // 6
                    "已收/已付金额",        // 7
                    "状态",                // 8
                    "制单人",              // 9
                    // 明细字段（按量结算危废明细）
                    "接收日期",            // 10
                    "明细关联来源类型",    // 11
                    "明细关联来源单号",    // 12
                    "广东省联单号",        // 13
                    "废物类别",            // 14
                    "废物代码",            // 15
                    "废物名称",            // 16
                    "结算模式",            // 17
                    "基本结算数量",        // 18
                    "基本计量单位",        // 19
                    "辅助结算数量",        // 20
                    "辅助计量单位",        // 21
                    "合同计划总量",        // 22
                    "辅助合同计划总量",    // 23
                    "累积已结算量",        // 24
                    "累积辅助已结算量",    // 25
                    "本次累积量",          // 26
                    "超出量",              // 27
                    "结算单价",            // 28
                    "超出单价",            // 29
                    "超出金额",            // 30
                    "金额"                 // 31
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = summaryHeaderRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 将按量结算明细按结算单单号分组（用于行展开）
            Map<String, List<SettlementExportDetailDTO>> detailMap = new HashMap<>();
            if (detailList != null && !detailList.isEmpty()) {
                for (SettlementExportDetailDTO detail : detailList) {
                    if (detail == null || detail.getSettlementCode() == null) {
                        continue;
                    }
                    String code = detail.getSettlementCode();
                    detailMap.computeIfAbsent(code, k -> new ArrayList<>()).add(detail);
                }
            }

            // 写入数据：以汇总列表为主，按明细粒度展开
            int rowIndex = 1;
            if (summaryList != null && !summaryList.isEmpty()) {
                for (SettlementExportSummaryDTO item : summaryList) {
                    String settlementCode = item.getSettlementCode();
                    List<SettlementExportDetailDTO> settlementDetails =
                            (settlementCode != null ? detailMap.get(settlementCode) : null);

                    int groupStartRow = rowIndex;

                    if (settlementDetails == null || settlementDetails.isEmpty()) {
                        // 没有按量结算明细的结算单：仅输出一行汇总，明细列为空
                        Row row = summarySheet.createRow(rowIndex++);
                        int col = 0;

                        // 汇总字段
                        Cell c0 = row.createCell(col++);
                        c0.setCellValue(item.getSettlementCode() != null ? item.getSettlementCode() : "");
                        c0.setCellStyle(cellStyle);

                        Cell c1 = row.createCell(col++);
                        c1.setCellValue(item.getContractCode() != null ? item.getContractCode() : "");
                        c1.setCellStyle(cellStyle);

                        Cell c2 = row.createCell(col++);
                        c2.setCellValue(item.getPartyAName() != null ? item.getPartyAName() : "");
                        c2.setCellStyle(cellStyle);

                        Cell c3 = row.createCell(col++);
                        c3.setCellValue(item.getPartyASocialCreditCode() != null ? item.getPartyASocialCreditCode() : "");
                        c3.setCellStyle(cellStyle);

                        Cell c4 = row.createCell(col++);
                        c4.setCellValue(item.getSettlementType() != null ? item.getSettlementType() : "");
                        c4.setCellStyle(cellStyle);

                        Cell c5 = row.createCell(col++);
                        c5.setCellValue(item.getSourceType() != null ? item.getSourceType() : "");
                        c5.setCellStyle(cellStyle);

                        Cell c6 = row.createCell(col++);
                        BigDecimal amount = item.getSettlementAmount();
                        c6.setCellValue(amount != null ? amount.doubleValue() : 0D);
                        c6.setCellStyle(numberStyle);

                        Cell c7 = row.createCell(col++);
                        BigDecimal received = item.getReceivedAmount();
                        c7.setCellValue(received != null ? received.doubleValue() : 0D);
                        c7.setCellStyle(numberStyle);

                        Cell c8 = row.createCell(col++);
                        c8.setCellValue(item.getStatus() != null ? item.getStatus() : "");
                        c8.setCellStyle(cellStyle);

                        Cell c9 = row.createCell(col++);
                        c9.setCellValue(item.getCreatorName() != null ? item.getCreatorName() : "");
                        c9.setCellStyle(cellStyle);
                        // 明细列（10~28）留空即可
                    } else {
                        // 存在按量结算明细：按明细条数展开多行，左侧汇总信息相同
                        // 记录当前结算单下每条明细对应的行号，用于后续按“父明细”分组合并
                        List<Integer> detailRowIndexList = new ArrayList<>();

                        for (SettlementExportDetailDTO detail : settlementDetails) {
                            Row row = summarySheet.createRow(rowIndex++);
                            int col = 0;

                            // 汇总字段（结算单级）
                            Cell c0 = row.createCell(col++);
                            c0.setCellValue(item.getSettlementCode() != null ? item.getSettlementCode() : "");
                            c0.setCellStyle(cellStyle);

                            Cell c1 = row.createCell(col++);
                            c1.setCellValue(item.getContractCode() != null ? item.getContractCode() : "");
                            c1.setCellStyle(cellStyle);

                            Cell c2 = row.createCell(col++);
                            c2.setCellValue(item.getPartyAName() != null ? item.getPartyAName() : "");
                            c2.setCellStyle(cellStyle);

                            Cell c3 = row.createCell(col++);
                            c3.setCellValue(item.getPartyASocialCreditCode() != null ? item.getPartyASocialCreditCode() : "");
                            c3.setCellStyle(cellStyle);

                            Cell c4 = row.createCell(col++);
                            c4.setCellValue(item.getSettlementType() != null ? item.getSettlementType() : "");
                            c4.setCellStyle(cellStyle);

                            Cell c5 = row.createCell(col++);
                            c5.setCellValue(item.getSourceType() != null ? item.getSourceType() : "");
                            c5.setCellStyle(cellStyle);

                            Cell c6 = row.createCell(col++);
                            BigDecimal amount = item.getSettlementAmount();
                            c6.setCellValue(amount != null ? amount.doubleValue() : 0D);
                            c6.setCellStyle(numberStyle);

                            Cell c7 = row.createCell(col++);
                            BigDecimal received = item.getReceivedAmount();
                            c7.setCellValue(received != null ? received.doubleValue() : 0D);
                            c7.setCellStyle(numberStyle);

                            Cell c8 = row.createCell(col++);
                            c8.setCellValue(item.getStatus() != null ? item.getStatus() : "");
                            c8.setCellStyle(cellStyle);

                            Cell c9 = row.createCell(col++);
                            c9.setCellValue(item.getCreatorName() != null ? item.getCreatorName() : "");
                            c9.setCellStyle(cellStyle);

                            // 明细字段（按量结算）
                            Cell c10 = row.createCell(col++);
                            c10.setCellValue(detail.getReceiveDate() != null ? detail.getReceiveDate() : "");
                            c10.setCellStyle(cellStyle);

                            Cell c11 = row.createCell(col++);
                            c11.setCellValue(detail.getDetailSourceType() != null ? detail.getDetailSourceType() : "");
                            c11.setCellStyle(cellStyle);

                            Cell c12 = row.createCell(col++);
                            c12.setCellValue(detail.getSourceOrderCode() != null ? detail.getSourceOrderCode() : "");
                            c12.setCellStyle(cellStyle);

                            Cell c13 = row.createCell(col++);
                            c13.setCellValue(detail.getProvinceManifestCode() != null ? detail.getProvinceManifestCode() : "");
                            c13.setCellStyle(cellStyle);

                            Cell c14 = row.createCell(col++);
                            c14.setCellValue(detail.getWasteCategory() != null ? detail.getWasteCategory() : "");
                            c14.setCellStyle(cellStyle);

                            Cell c15 = row.createCell(col++);
                            c15.setCellValue(detail.getWasteCode() != null ? detail.getWasteCode() : "");
                            c15.setCellStyle(cellStyle);

                            Cell c16 = row.createCell(col++);
                            c16.setCellValue(detail.getWasteName() != null ? detail.getWasteName() : "");
                            c16.setCellStyle(cellStyle);

                            Cell c17 = row.createCell(col++);
                            c17.setCellValue(detail.getSettlementMode() != null ? detail.getSettlementMode() : "");
                            c17.setCellStyle(cellStyle);

                            Cell c18 = row.createCell(col++);
                            BigDecimal baseQty = detail.getBaseQuantity();
                            if (baseQty != null) {
                                c18.setCellValue(baseQty.doubleValue());
                                c18.setCellStyle(numberStyle);
                            } else {
                                c18.setCellValue("");
                                c18.setCellStyle(cellStyle);
                            }

                            Cell c19 = row.createCell(col++);
                            c19.setCellValue(detail.getBaseUnit() != null ? detail.getBaseUnit() : "");
                            c19.setCellStyle(cellStyle);

                            Cell c20 = row.createCell(col++);
                            BigDecimal auxQty = detail.getAuxQuantity();
                            if (auxQty != null) {
                                c20.setCellValue(auxQty.doubleValue());
                                c20.setCellStyle(numberStyle);
                            } else {
                                c20.setCellValue("");
                                c20.setCellStyle(cellStyle);
                            }

                            Cell c21 = row.createCell(col++);
                            c21.setCellValue(detail.getAuxUnit() != null ? detail.getAuxUnit() : "");
                            c21.setCellStyle(cellStyle);

                            Cell c22 = row.createCell(col++);
                            BigDecimal contractPlan = detail.getContractPlanTotal();
                            if (contractPlan != null) {
                                c22.setCellValue(contractPlan.doubleValue());
                                c22.setCellStyle(numberStyle);
                            } else {
                                c22.setCellValue("");
                                c22.setCellStyle(cellStyle);
                            }

                            Cell c23 = row.createCell(col++);
                            BigDecimal auxContractPlan = detail.getAuxContractPlanTotal();
                            if (auxContractPlan != null) {
                                c23.setCellValue(auxContractPlan.doubleValue());
                                c23.setCellStyle(numberStyle);
                            } else {
                                c23.setCellValue("");
                                c23.setCellStyle(cellStyle);
                            }

                            Cell c24 = row.createCell(col++);
                            BigDecimal accQty = detail.getAccumulatedQuantity();
                            if (accQty != null) {
                                c24.setCellValue(accQty.doubleValue());
                                c24.setCellStyle(numberStyle);
                            } else {
                                c24.setCellValue("");
                                c24.setCellStyle(cellStyle);
                            }

                            Cell c25 = row.createCell(col++);
                            BigDecimal auxAccQty = detail.getAuxAccumulatedQuantity();
                            if (auxAccQty != null) {
                                c25.setCellValue(auxAccQty.doubleValue());
                                c25.setCellStyle(numberStyle);
                            } else {
                                c25.setCellValue("");
                                c25.setCellStyle(cellStyle);
                            }

                            Cell c26 = row.createCell(col++);
                            BigDecimal currentAcc = detail.getCurrentAccumulatedQuantity();
                            if (currentAcc != null) {
                                c26.setCellValue(currentAcc.doubleValue());
                                c26.setCellStyle(numberStyle);
                            } else {
                                c26.setCellValue("");
                                c26.setCellStyle(cellStyle);
                            }

                            Cell c27 = row.createCell(col++);
                            BigDecimal exceedQty = detail.getExceedQuantity();
                            if (exceedQty != null) {
                                c27.setCellValue(exceedQty.doubleValue());
                                c27.setCellStyle(numberStyle);
                            } else {
                                c27.setCellValue("");
                                c27.setCellStyle(cellStyle);
                            }

                            Cell c28 = row.createCell(col++);
                            BigDecimal price = detail.getPrice();
                            if (price != null) {
                                c28.setCellValue(price.doubleValue());
                                c28.setCellStyle(numberStyle);
                            } else {
                                c28.setCellValue("");
                                c28.setCellStyle(cellStyle);
                            }

                            Cell c29 = row.createCell(col++);
                            BigDecimal exceedPrice = detail.getExceedPrice();
                            if (exceedPrice != null) {
                                c29.setCellValue(exceedPrice.doubleValue());
                                c29.setCellStyle(numberStyle);
                            } else {
                                c29.setCellValue("");
                                c29.setCellStyle(cellStyle);
                            }

                            Cell c30 = row.createCell(col);
                            BigDecimal exceedAmount = detail.getExceedAmount();
                            if (exceedAmount != null) {
                                c30.setCellValue(exceedAmount.doubleValue());
                                c30.setCellStyle(numberStyle);
                            } else {
                                c30.setCellValue("");
                                c30.setCellStyle(cellStyle);
                            }

                            // 金额列（明细金额）
                            Cell c31 = row.createCell(++col);
                            BigDecimal detailAmount = detail.getAmount();
                            if (detailAmount != null) {
                                c31.setCellValue(detailAmount.doubleValue());
                                c31.setCellStyle(numberStyle);
                            } else {
                                c31.setCellValue("");
                                c31.setCellStyle(cellStyle);
                            }

                            // 记录当前明细所在的行号
                            detailRowIndexList.add(row.getRowNum());
                        }

                        // 对同一“父明细”的明细行，在除废物三列外的其它明细字段进行纵向合并
                        if (settlementDetails.size() > 1) {
                            int[] mergeCols = new int[]{
                                    10, 11, 12, 13,    // 接收日期、明细关联来源类型、明细关联来源单号、广东省联单号
                                    17,                // 结算模式
                                    18, 19,            // 基本结算数量、基本计量单位
                                    20, 21,            // 辅助结算数量、辅助计量单位
                                    22, 23,            // 合同计划总量、辅助合同计划总量
                                    24, 25,            // 累积已结算量、累积辅助已结算量
                                    26, 27,            // 本次累积量、超出量
                                    28, 29, 30, 31     // 结算单价、超出单价、超出金额、金额
                            };

                            int detailSize = settlementDetails.size();
                            int i = 0;
                            while (i < detailSize) {
                                SettlementExportDetailDTO current = settlementDetails.get(i);
                                int groupStartRowForDetail = detailRowIndexList.get(i);
                                int groupEndRowForDetail = groupStartRowForDetail;

                                // 仅对“合同级 + 总价包干”的明细进行父明细级行合并
                                String headerSourceType = item.getSourceType();
                                if (!"总价包干".equals(current.getSettlementMode())
                                        || headerSourceType == null
                                        || !"CONTRACT".equalsIgnoreCase(headerSourceType)) {
                                    i++;
                                    continue;
                                }

                                int j = i + 1;
                                while (j < detailSize) {
                                    SettlementExportDetailDTO next = settlementDetails.get(j);

                                    // 只要有一项关键字段不同，就认为不是同一个父明细分组
                                    boolean sameParent =
                                            Objects.equals(current.getReceiveDate(), next.getReceiveDate())
                                                    && Objects.equals(current.getDetailSourceType(), next.getDetailSourceType())
                                                    && Objects.equals(current.getSourceOrderCode(), next.getSourceOrderCode())
                                                    && Objects.equals(current.getProvinceManifestCode(), next.getProvinceManifestCode())
                                                    && Objects.equals(current.getSettlementMode(), next.getSettlementMode())
                                                    && Objects.equals(current.getBaseQuantity(), next.getBaseQuantity())
                                                    && Objects.equals(current.getBaseUnit(), next.getBaseUnit())
                                                    && Objects.equals(current.getAuxQuantity(), next.getAuxQuantity())
                                                    && Objects.equals(current.getAuxUnit(), next.getAuxUnit())
                                                    && Objects.equals(current.getContractPlanTotal(), next.getContractPlanTotal())
                                                    && Objects.equals(current.getAuxContractPlanTotal(), next.getAuxContractPlanTotal())
                                                    && Objects.equals(current.getAccumulatedQuantity(), next.getAccumulatedQuantity())
                                                    && Objects.equals(current.getAuxAccumulatedQuantity(), next.getAuxAccumulatedQuantity())
                                                    && Objects.equals(current.getCurrentAccumulatedQuantity(), next.getCurrentAccumulatedQuantity())
                                                    && Objects.equals(current.getExceedQuantity(), next.getExceedQuantity())
                                                    && Objects.equals(current.getPrice(), next.getPrice())
                                                    && Objects.equals(current.getExceedPrice(), next.getExceedPrice())
                                                    && Objects.equals(current.getExceedAmount(), next.getExceedAmount())
                                                    && Objects.equals(current.getAmount(), next.getAmount());

                                    if (!sameParent) {
                                        break;
                                    }

                                    groupEndRowForDetail = detailRowIndexList.get(j);
                                    j++;
                                }

                                // 同一父明细下至少有两行危废明细时，才执行纵向合并
                                if (groupEndRowForDetail > groupStartRowForDetail) {
                                    for (int mergeCol : mergeCols) {
                                        summarySheet.addMergedRegion(new CellRangeAddress(
                                                groupStartRowForDetail, groupEndRowForDetail, mergeCol, mergeCol));
                                    }
                                }

                                i = j;
                            }
                        }

                        // 对同一结算单的汇总列（0~9）进行纵向合并
                        int groupEndRow = rowIndex - 1;
                        if (groupEndRow > groupStartRow) {
                            for (int col = 0; col <= 9; col++) {
                                summarySheet.addMergedRegion(new CellRangeAddress(groupStartRow, groupEndRow, col, col));
                            }
                        }
                    }
                }
            }

            // 自适应列宽
            for (int i = 0; i < headers.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    /**
     * 创建结算单
     * settlementService.createSettlement(createDTO);
     */
    @RequireActionPermission({
            "合同结算:危险废物结算-收款结算:新增",
            "合同结算:危险废物结算-付款结算:新增"
    })
    @PostMapping("/settlements")
    @ApiOperation(value = "创建结算单", notes = "根据合同数据创建结算单，支持根据付款方自动生成收款/付款结算单")
    public Result<SettlementCreateResultDTO> createSettlement(@RequestBody @Validated SettlementCreateDTO createDTO,
                                                               HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            Result<SettlementCreateResultDTO> result = settlementService.createSettlement(createDTO);

            if (result.isSuccess() && result.getData() != null) {
                String settlementCode = result.getData().getSettlementCode();
                logRecordService.recordOperationLog("结算单管理", "新增",
                        "创建结算单成功：结算单号=" + (settlementCode != null ? settlementCode : "未知"),
                        userId, ipAddress, true, null);

                // 发送消息通知（使用基于权限的通知方法）
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "SETTLEMENT_CREATE",
                            result.getData().getSettlementId() != null ? result.getData().getSettlementId().intValue() : null,
                            String.format("结算单已创建：合同号=%s，结算单号=%s",
                                    createDTO.getContractCode(), settlementCode != null ? settlementCode : "未知"),
                            "新增",
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送结算单创建通知失败", msgEx);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("创建结算单失败：contractCode={}", createDTO.getContractCode(), e);
            logRecordService.recordOperationLog("结算单管理", "新增",
                    "创建结算单失败：合同号=" + createDTO.getContractCode(), userId, ipAddress, false, e.getMessage());
            return Result.error("创建结算单失败：" + e.getMessage());
        }
    }

    /**
     * 获取结算单详情
     */
    @RequireActionPermission({
            "合同结算:危险废物结算-收款结算:查看",
            "合同结算:危险废物结算-付款结算:查看"
    })
    @GetMapping("/settlements/{settlementId}")
    @ApiOperation(value = "获取结算单详情", notes = "根据结算单ID获取完整的结算单信息，包括明细和价外服务")
    public Result<SettlementDetailDTO> getSettlementDetail(@PathVariable Long settlementId) {
        // 参数验证
        if (settlementId == null || settlementId <= 0) {
            return Result.error("无效的结算单ID");
        }

        try {
            SettlementDetailDTO data = settlementService.getSettlementDetail(settlementId);
            return Result.success("获取结算单详情成功", data);
        } catch (Exception e) {
            log.error("获取结算单详情失败：settlementId={}", settlementId, e);
            return Result.error("获取结算单详情失败：" + e.getMessage());
        }
    }

    /**
     * 审核结算单
     */
    @RequireActionPermission({
            "合同结算:危险废物结算-收款结算:审核",
            "合同结算:危险废物结算-付款结算:审核"
    })
    @PostMapping("/settlements/{settlementId}/audit")
    @ApiOperation(value = "审核结算单", notes = "审核结算单，更新状态为已审核或已拒绝")
    public Result<Void> auditSettlement(
            @PathVariable Long settlementId,
            @RequestBody @Validated SettlementAuditDTO auditDTO,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            Result<Void> result = settlementService.auditSettlement(settlementId, auditDTO);

            if (result.isSuccess()) {
                String action = ("approved".equals(auditDTO.getAuditResult()) || "PASSED".equals(auditDTO.getAuditResult()) || "通过".equals(auditDTO.getAuditResult())) ? "审核通过" : "审核驳回";
                logRecordService.recordOperationLog("结算单管理", "审核",
                        action + "结算单：结算单ID=" + settlementId + "，审核意见=" + auditDTO.getAuditOpinion(),
                        userId, ipAddress, true, null);

                // 发送消息通知（使用基于权限的通知方法）
                try {
                    String auditAction = ("approved".equals(auditDTO.getAuditResult()) || "PASSED".equals(auditDTO.getAuditResult()) || "通过".equals(auditDTO.getAuditResult())) ? "审核通过" : "审核驳回";
                    messageNotificationService.sendAuditResultNotification(
                            "SETTLEMENT_AUDIT_RESULT",
                            settlementId != null ? settlementId.intValue() : null,
                            String.format("结算单ID=%d", settlementId),
                            auditAction,
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送结算单审核通知失败", msgEx);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("审核结算单失败：settlementId={}", settlementId, e);
            logRecordService.recordOperationLog("结算单管理", "审核",
                    "审核结算单失败：结算单ID=" + settlementId, userId, ipAddress, false, e.getMessage());
            return Result.error("审核结算单失败：" + e.getMessage());
        }
    }

    /**
     * 结算单分页查询
     */
    @RequirePagePermission({
            "合同结算:危险废物结算:页面",
            "合同结算:危险废物结算-收款结算:页面",
            "合同结算:危险废物结算-付款结算:页面"
    })
    @GetMapping("/settlements/list")
    @ApiOperation(value = "结算单分页查询", notes = "支持按结算类型、结算单单号、合同号、客户名称、状态、制单人、结算周期、创建时间等条件筛选。")
    public Result<IPage<?>> getSettlementPage(@Valid SettlementPageRequest request) {
        try {
            IPage<SettlementPageResponse> page = settlementService.getSettlementPage(request);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("结算单分页查询失败：request={}", request, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取结算统计信息
     */
    @GetMapping("/settlement-statistics")
    @ApiOperation(value = "获取结算统计信息", notes = "根据结算类型获取结算单的统计信息（总数、总金额、已付金额、未付金额、逾期数量等）")
    public Result<SettlementStatisticsDTO> getSettlementStatistics(
            @RequestParam(name = "settlementType", required = false) String settlementType) {
        try {
            SettlementStatisticsDTO statistics = settlementService.getSettlementStatistics(settlementType);
            return Result.success("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取结算统计信息失败", e);
            return Result.error("获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 查询已结算入库量
     */
    @PostMapping("/settlements/settled-warehousing-quantity")
    @ApiOperation(value = "查询已结算入库量", notes = "根据合同号和废物信息查询已结算的入库量")
    public Result<SettledWarehousingQuantityResponseDTO> getSettledWarehousingQuantity(
            @RequestBody @Validated SettledWarehousingQuantityRequestDTO requestDTO) {
        try {
            SettledWarehousingQuantityResponseDTO data = settlementService.getSettledWarehousingQuantity(requestDTO);
            return Result.success("查询已结算入库量成功", data);
        } catch (Exception e) {
            log.error("查询已结算入库量失败：contractCode={}", requestDTO.getContractCode(), e);
            return Result.error("查询已结算入库量失败：" + e.getMessage());
        }
    }

    /**
     * 更新结算单
     */
    @PutMapping("/settlements/{settlementId}")
    @ApiOperation(value = "更新结算单", notes = "根据结算单ID更新结算单信息，支持字段更新和完整更新两种模式")
    public Result<Void> updateSettlement(
            @PathVariable Long settlementId,
            @RequestBody @Validated SettlementUpdateDTO updateDTO,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            Result<Void> result = settlementService.updateSettlement(settlementId, updateDTO);

            if (result.isSuccess()) {
                logRecordService.recordOperationLog("结算单管理", "更新",
                        "更新结算单：结算单ID=" + settlementId, userId, ipAddress, true, null);

                // 发送消息通知（使用基于权限的通知方法）
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "SETTLEMENT_UPDATE",
                            settlementId != null ? settlementId.intValue() : null,
                            String.format("结算单已更新：结算单ID=%d", settlementId),
                            "更新",
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送结算单更新通知失败", msgEx);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("更新结算单失败：settlementId={}", settlementId, e);
            logRecordService.recordOperationLog("结算单管理", "更新",
                    "更新结算单失败：结算单ID=" + settlementId, userId, ipAddress, false, e.getMessage());
            return Result.error("更新结算单失败：" + e.getMessage());
        }
    }

    /**
     * 获取结算单审核数据
     */
    @GetMapping("/settlements/{settlementId}/audit-data")
    @ApiOperation(value = "获取结算单审核数据", notes = "获取结算单审核所需的所有数据，包括基本信息、合同数据、结算明细、入库数据和对比结果")
    public Result<SettlementAuditDataDTO> getSettlementAuditData(@PathVariable Long settlementId) {
        try {
            SettlementAuditDataDTO data = settlementService.getSettlementAuditData(settlementId);
            return Result.success("获取审核数据成功", data);
        } catch (Exception e) {
            log.error("获取审核数据失败：settlementId={}", settlementId, e);
            return Result.error("获取审核数据失败：" + e.getMessage());
        }
    }

    /**
     * 根据合同查询结算单
     * 功能描述：根据合同ID和开票类型查询可用于开票的结算单列表
     * 入参：contractId (路径参数), invoiceType (查询参数：开票/作废)
     * 返回参数：分页的结算单列表
     * url地址：/api/finance/settlements/by-contract/{contractId}
     * 请求方式：GET
     */
    @GetMapping("/settlements/by-contract/{contractId}")
    @ApiOperation(value = "根据合同查询结算单", notes = "根据合同ID和开票类型查询可用于开票的结算单列表")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<SettlementQueryResultDTO>> getSettlementsByContract(
            @PathVariable @NotNull(message = "合同ID不能为空") Integer contractId,
            @RequestParam @NotNull(message = "开票类型不能为空") String invoiceType,
            @RequestParam(required = false) String viewScope,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "50") Integer size) {
        try {
            // 创建分页对象
            com.baomidou.mybatisplus.core.metadata.IPage<SettlementQueryResultDTO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);

            // 查询数据
            List<SettlementQueryResultDTO> settlements = invoiceNoticeService.getSettlementsByContract(contractId, invoiceType, viewScope);

            // 设置分页结果
            page.setRecords(settlements);
            page.setTotal(settlements.size());
            page.setCurrent(current);
            page.setSize(size);
            page.setPages((long) Math.ceil((double) settlements.size() / size));

            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询结算单失败：contractId={}, invoiceType={}", contractId, invoiceType, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询结算单失败：" + e.getMessage());
        }
    }

    /**
     * 删除结算单
     */
    @DeleteMapping("/settlements/{settlementId}")
    @ApiOperation(value = "删除结算单", notes = "删除指定结算单，只有待审核/已拒绝状态才能删除，删除时会将关联的入库单状态改为待结算")
    public Result<Void> deleteSettlement(
            @PathVariable @NotNull(message = "结算单ID不能为空")
            @Min(value = 1, message = "结算单ID必须大于0") Long settlementId,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("删除结算单请求：settlementId={}", settlementId);
            Result<Void> result = settlementService.deleteSettlement(settlementId);

            if (result.isSuccess()) {
                logRecordService.recordOperationLog("结算单管理", "删除",
                        "删除结算单：结算单ID=" + settlementId, userId, ipAddress, true, null);

                // 发送消息通知（使用基于权限的通知方法）
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "SETTLEMENT_DELETE",
                            settlementId != null ? settlementId.intValue() : null,
                            String.format("结算单已删除：结算单ID=%d", settlementId),
                            "删除",
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送结算单删除通知失败", msgEx);
                }
            }
            return result;
        } catch (BusinessException e) {
            log.warn("删除结算单业务异常：settlementId={}, message={}", settlementId, e.getMessage());
            logRecordService.recordOperationLog("结算单管理", "删除",
                    "删除结算单失败：结算单ID=" + settlementId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除结算单系统异常：settlementId={}", settlementId, e);
            logRecordService.recordOperationLog("结算单管理", "删除",
                    "删除结算单失败：结算单ID=" + settlementId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除结算单失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除结算单
     */
    @DeleteMapping("/settlements/batch-delete")
    @ApiOperation(value = "批量删除结算单", notes = "批量删除结算单，只有待审核/已拒绝状态才能删除，删除时会将关联的入库单状态改为待结算")
    public Result<Void> batchDeleteSettlements(
            @RequestBody @Validated SettlementBatchDeleteRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量删除结算单请求：settlementIds={}", request.getSettlementIds());
            Result<Void> result = settlementService.batchDeleteSettlements(request.getSettlementIds());

            if (result.isSuccess()) {
                logRecordService.recordOperationLog("结算单管理", "批量删除",
                        "批量删除结算单：结算单IDs=" + request.getSettlementIds(), userId, ipAddress, true, null);

                // 发送消息通知（使用基于权限的通知方法）
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "SETTLEMENT_DELETE",
                            null,
                            String.format("已批量删除 %d 个结算单", request.getSettlementIds().size()),
                            "删除",
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送批量删除结算单通知失败", msgEx);
                }
            }
            return result;
        } catch (BusinessException e) {
            log.warn("批量删除结算单业务异常：settlementIds={}, message={}", request.getSettlementIds(), e.getMessage());
            logRecordService.recordOperationLog("结算单管理", "批量删除",
                    "批量删除结算单失败：结算单IDs=" + request.getSettlementIds(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量删除结算单系统异常：settlementIds={}", request.getSettlementIds(), e);
            logRecordService.recordOperationLog("结算单管理", "批量删除",
                    "批量删除结算单失败：结算单IDs=" + request.getSettlementIds(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量删除结算单失败：" + e.getMessage());
        }
    }

    /**
     * 批量撤回结算单审核
     * 功能描述：批量撤回多个审核中的结算单
     * - 只有审核中状态的结算单才能撤回
     * - 撤回后结算单状态改为"待审核"
     * - OA审核记录表状态改为"已撤回"，审核次数-1（最低为0）
     * 接口地址：POST /api/finance/settlements/batch-cancel-audit
     * 请求方式：POST
     * 请求参数：{ settlementIds: number[] }
     * 返回参数：{ successCount: number, failCount: number, failures: [{ settlementId, settlementCode, reason }] }
     */
    @PostMapping("/settlements/batch-cancel-audit")
    @ApiOperation(value = "批量撤回结算单审核", notes = "批量撤回多个审核中的结算单，只有审核中状态才能撤回，撤回后状态改为待审核")
    public Result<BatchOperationResult> batchCancelAudit(
            @RequestBody @Validated SettlementBatchDeleteRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量撤回结算单审核请求：settlementIds={}", request.getSettlementIds());

            BatchOperationResult result = settlementService.batchCancelAudit(request.getSettlementIds());

            if (result.isAllSuccess()) {
                logRecordService.recordOperationLog("结算单管理", "批量撤回",
                        "批量撤回结算单审核成功：成功 " + result.getSuccessCount() + " 个",
                        userId, ipAddress, true, null);

                // 发送消息通知（使用基于权限的通知方法）
                try {
                    messageNotificationService.sendApprovalRevokeNotification(
                            "SETTLEMENT_REVOKE",
                            null,
                            String.format("已批量撤回 %d 个结算单审核", result.getSuccessCount()),
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送批量撤回结算单通知失败", msgEx);
                }
            } else {
                // 部分失败时记录警告日志
                StringBuilder failReasons = new StringBuilder();
                if (result.getFailures() != null && !result.getFailures().isEmpty()) {
                    for (int i = 0; i < Math.min(5, result.getFailures().size()); i++) {
                        BatchOperationResult.FailureDetail detail = result.getFailures().get(i);
                        failReasons.append(detail.getSettlementCode() != null ? detail.getSettlementCode() : detail.getSettlementId())
                                .append(": ").append(detail.getReason()).append("; ");
                    }
                    if (result.getFailures().size() > 5) {
                        failReasons.append("...共 ").append(result.getFailures().size()).append(" 条失败");
                    }
                }

                logRecordService.recordOperationLog("结算单管理", "批量撤回",
                        "批量撤回结算单审核部分失败：成功 " + result.getSuccessCount() + " 个，失败 " + result.getFailCount() + " 个。原因：" + failReasons,
                        userId, ipAddress, false, null);
            }

            return Result.success("批量撤回完成", result);
        } catch (Exception e) {
            log.error("批量撤回结算单审核系统异常：settlementIds={}", request.getSettlementIds(), e);
            logRecordService.recordOperationLog("结算单管理", "批量撤回",
                    "批量撤回结算单审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量撤回结算单审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量提交结算单审核
     * 功能描述：批量提交多个结算单进行审核
     * - 只有待审核、已驳回状态的结算单才能提交审核
     * - 提交后结算单状态改为"审核中"
     * - 同时在OA审核记录表新增记录，来源表中文名称为"危险废物结算"
     * 接口地址：POST /api/finance/settlements/batch-submit-audit
     * 请求方式：POST
     * 请求参数：{ settlementIds: number[] }
     * 返回参数：{ successCount: number, failCount: number, failures: [{ settlementId, settlementCode, reason }] }
     */
    @PostMapping("/settlements/batch-submit-audit")
    @ApiOperation(value = "批量提交结算单审核", notes = "批量提交多个结算单进行审核，只有待审核、已驳回状态才能提交，提交后状态改为审核中")
    public Result<BatchOperationResult> batchSubmitAudit(
            @RequestBody @Validated SettlementBatchDeleteRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量提交结算单审核请求：settlementIds={}", request.getSettlementIds());

            BatchOperationResult result = settlementService.batchSubmitAudit(request.getSettlementIds());

            if (result.isAllSuccess()) {
                logRecordService.recordOperationLog("结算单管理", "批量提交审核",
                        "批量提交结算单审核成功：成功 " + result.getSuccessCount() + " 个",
                        userId, ipAddress, true, null);

                // 发送消息通知（使用基于权限的通知方法）
                try {
                    messageNotificationService.sendApprovalSubmitNotification(
                            "SETTLEMENT_SUBMIT",
                            null,
                            String.format("已批量提交 %d 个结算单审核", result.getSuccessCount()),
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送批量提交结算单通知失败", msgEx);
                }
            } else {
                // 部分失败时记录警告日志
                StringBuilder failReasons = new StringBuilder();
                if (result.getFailures() != null && !result.getFailures().isEmpty()) {
                    for (int i = 0; i < Math.min(5, result.getFailures().size()); i++) {
                        BatchOperationResult.FailureDetail detail = result.getFailures().get(i);
                        failReasons.append(detail.getSettlementCode() != null ? detail.getSettlementCode() : detail.getSettlementId())
                                .append(": ").append(detail.getReason()).append("; ");
                    }
                    if (result.getFailures().size() > 5) {
                        failReasons.append("...共 ").append(result.getFailures().size()).append(" 条失败");
                    }
                }

                logRecordService.recordOperationLog("结算单管理", "批量提交审核",
                        "批量提交结算单审核部分失败：成功 " + result.getSuccessCount() + " 个，失败 " + result.getFailCount() + " 个。原因：" + failReasons,
                        userId, ipAddress, false, null);
            }

            return Result.success("批量提交审核完成", result);
        } catch (Exception e) {
            log.error("批量提交结算单审核系统异常：settlementIds={}", request.getSettlementIds(), e);
            logRecordService.recordOperationLog("结算单管理", "批量提交审核",
                    "批量提交结算单审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量提交结算单审核失败：" + e.getMessage());
        }
    }

    /**
     * 获取入库记录对应的危废明细（带合同匹配信息）
     * 功能描述：后端统一处理入库数据与合同数据的匹配，返回完整匹配数据
     * 双层锁定：通过入库单信息列表（warehousingList）中每个对象的入库单号和入库单编号同时匹配
     * 接口地址：/finance/warehousing-waste-details-with-contract
     * 请求方式：POST
     */
    @PostMapping("/warehousing-waste-details-with-contract")
    @ApiOperation(value = "获取入库危废明细（含合同匹配）",
                  notes = "根据入库单信息列表（入库单号+入库单编号）和合同编号/合同号，获取入库危废明细并关联合同信息（后端统一处理匹配逻辑）")
    public Result<WarehousingWasteDetailWithContractResponse> getWarehousingWasteDetailsWithContract(
            @RequestBody @Validated WarehousingWasteDetailWithContractRequest request) {
        try {
            WarehousingWasteDetailWithContractResponse response =
                    financeService.getWarehousingWasteDetailsWithContract(
                            request.getWarehousingList(),
                            request.getContractId(),
                            request.getContractNo()
                    );
            return Result.success("获取入库危废明细成功", response);
        } catch (Exception e) {
            log.error("获取入库危废明细失败：warehousingList={}, contractId={}, contractNo={}",
                    request.getWarehousingList(), request.getContractId(), request.getContractNo(), e);
            return Result.error("获取入库危废明细失败：" + e.getMessage());
        }
    }
}

