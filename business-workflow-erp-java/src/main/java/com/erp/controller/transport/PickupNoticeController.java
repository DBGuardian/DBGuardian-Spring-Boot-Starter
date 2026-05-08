package com.erp.controller.transport;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.controller.transport.dto.*;
import com.erp.service.production.PickupNoticeService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 收运通知单管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/transport/apply")
@Api(tags = "收运通知单管理")
public class PickupNoticeController {

    @Autowired
    private PickupNoticeService pickupNoticeService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 新增收运通知单
     */
    @RequireActionPermission("业务管理:收运通知:新增")
    @PostMapping("/create")
    @ApiOperation(value = "新增收运通知单", notes = "创建新的收运通知单，包含危废明细和附件信息")
    public Result<TransportApplyDetailResponse> createTransportApply(
            @Valid @RequestBody TransportApplyDetailRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            TransportApplyDetailResponse response = pickupNoticeService.createPickupNotice(request);
            logRecordService.recordOperationLog("收运通知单管理", "新增",
                    "收运通知单号=" + (response.getNoticeCode() != null ? response.getNoticeCode() : "ID=" + response.getNoticeId()),
                    userId, ipAddress, true, null);
            return Result.success("新增收运通知单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "新增",
                    "新增收运通知单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增收运通知单失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "新增",
                    "新增收运通知单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增收运通知单失败：" + e.getMessage());
        }
    }

    /**
     * 更新收运通知单
     */
    @RequireActionPermission("业务管理:收运通知:编辑")
    @PostMapping("/update")
    @ApiOperation(value = "更新收运通知单", notes = "更新收运通知单信息，包含危废明细和附件信息")
    public Result<TransportApplyDetailResponse> updateTransportApply(
            @Valid @RequestBody TransportApplyDetailRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            TransportApplyDetailResponse response = pickupNoticeService.updatePickupNotice(request);
            logRecordService.recordOperationLog("收运通知单管理", "更新",
                    "收运通知单号=" + (response.getNoticeCode() != null ? response.getNoticeCode() : "ID=" + response.getNoticeId()),
                    userId, ipAddress, true, null);
            return Result.success("更新收运通知单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "更新",
                    "更新收运通知单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新收运通知单失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "更新",
                    "更新收运通知单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新收运通知单失败：" + e.getMessage());
        }
    }

    /**
     * 获取收运通知单详情
     */
    @RequireActionPermission("业务管理:收运通知:查看")
    @GetMapping("/detail")
    @ApiOperation(value = "获取收运通知单详情", notes = "根据收运通知单号或编号查询详情")
    public Result<TransportApplyDetailResponse> getTransportApplyDetail(
            @RequestParam(required = false) String noticeCode,
            @RequestParam(required = false) Integer noticeId) {
        try {
            TransportApplyDetailResponse response;
            if (noticeId != null) {
                response = pickupNoticeService.getPickupNoticeDetailById(noticeId);
            } else if (noticeCode != null && !noticeCode.trim().isEmpty()) {
                response = pickupNoticeService.getPickupNoticeDetail(noticeCode);
            } else {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "收运通知单号或编号不能为空");
            }
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取收运通知单详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取详情失败：" + e.getMessage());
        }
    }

    /**
     * 收运通知单分页查询（收运通知页面专用）
     */
    @RequirePagePermission("业务管理:收运通知:页面")
    @GetMapping("/list")
    @ApiOperation(value = "收运通知单分页查询", notes = "支持按收运通知单号、合同号、客户、状态等条件筛选")
    public Result<?> getTransportApplyList(@Valid TransportApplyPageRequest request) {
        try {
            TransportApplyListResponse response = pickupNoticeService.getPickupNoticePage(request);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询收运通知单列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 车辆安排分页查询（车辆安排页面专用）
     */
    @RequirePagePermission("运输管理:车辆安排:页面")
    @GetMapping("/vehicle-arrange/list")
    @ApiOperation(value = "车辆安排分页查询", notes = "车辆安排页面专用，使用\"运输管理:车辆安排:页面\"权限编码查询viewScope")
    public Result<?> getVehicleArrangeList(@Valid TransportApplyPageRequest request) {
        try {
            TransportApplyListResponse response = pickupNoticeService.getVehicleArrangeNoticePage(request);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询车辆安排列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 提交收运通知单审核
     */
    @RequireActionPermission("业务管理:收运通知:提交审核")
    @PostMapping("/submit")
    @ApiOperation(value = "提交收运通知单审核", notes = "将收运通知单状态从'待审核'变更为'审核中'")
    public Result<Void> submitTransportApply(
            @Valid @RequestBody TransportApplySubmitRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            pickupNoticeService.submitPickupNotice(request.getNoticeCode());
            logRecordService.recordOperationLog("收运通知单管理", "提交审核",
                    "收运通知单号=" + request.getNoticeCode(),
                    userId, ipAddress, true, null);
            return Result.success("提交审核成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "提交审核",
                    "收运通知单号=" + request.getNoticeCode(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("提交收运通知单审核失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "提交审核",
                    "收运通知单号=" + request.getNoticeCode(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 撤回收运通知单
     */
    @RequireActionPermission("业务管理:收运通知:撤回")
    @PostMapping("/revoke")
    @ApiOperation(value = "撤回收运通知单", notes = "将收运通知单状态从'审核中'变更为'待审核'")
    public Result<Void> revokeTransportApply(
            @Valid @RequestBody TransportApplyRevokeRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            pickupNoticeService.revokePickupNotice(request.getNoticeCode());
            logRecordService.recordOperationLog("收运通知单管理", "撤回",
                    "收运通知单号=" + request.getNoticeCode(),
                    userId, ipAddress, true, null);
            return Result.success("撤回成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "撤回",
                    "收运通知单号=" + request.getNoticeCode(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("撤回收运通知单失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "撤回",
                    "收运通知单号=" + request.getNoticeCode(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "撤回失败：" + e.getMessage());
        }
    }

    /**
     * 审核收运通知单
     */
    @RequireActionPermission("业务管理:收运通知:审核")
    @PostMapping("/audit")
    @ApiOperation(value = "审核收运通知单", notes = "审核收运通知单并修改状态。审核中状态的收运通知单可审核为待调度（通过）或已驳回（驳回），驳回时审核意见必填")
    public Result<Void> auditTransportApply(
            @Valid @RequestBody TransportApplyAuditRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            pickupNoticeService.auditPickupNotice(request.getNoticeCode(), request.getAuditResult(), request.getAuditOpinion());
            logRecordService.recordOperationLog("收运通知单管理", "审核",
                    "收运通知单号=" + request.getNoticeCode() + "，审核结果=" + request.getAuditResult(),
                    userId, ipAddress, true, null);
            return Result.success("审核成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "审核",
                    "收运通知单号=" + request.getNoticeCode() + "，审核失败",
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审核收运通知单失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "审核",
                    "收运通知单号=" + request.getNoticeCode() + "，审核失败",
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "审核失败：" + e.getMessage());
        }
    }

    /**
     * 审核阶段补充/更新合同号
     */
    @PostMapping("/audit/bind-contract")
    @ApiOperation(value = "审核阶段补充合同号", notes = "仅审核中状态允许补充合同号，更新后清除待补标记")
    public Result<Void> bindContractDuringAudit(
            @Valid @RequestBody TransportApplyAuditBindContractRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            pickupNoticeService.bindContractDuringAudit(request.getNoticeCode(), request.getContractCode());
            logRecordService.recordOperationLog("收运通知单管理", "审核阶段补充合同",
                    "收运通知单号=" + request.getNoticeCode() + "，合同号=" + request.getContractCode(),
                    userId, ipAddress, true, null);
            return Result.success("补充合同成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "审核阶段补充合同",
                    "收运通知单号=" + request.getNoticeCode(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审核阶段补充合同失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "审核阶段补充合同",
                    "收运通知单号=" + request.getNoticeCode(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "补充合同失败：" + e.getMessage());
        }
    }

    /**
     * 删除收运通知单
     */
    @RequireActionPermission("业务管理:收运通知:删除")
    @DeleteMapping("/{noticeCode}")
    @ApiOperation(value = "删除收运通知单", notes = "删除待审核或已驳回状态的收运通知单")
    public Result<Void> deleteTransportApply(
            @PathVariable String noticeCode,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            pickupNoticeService.deletePickupNotice(noticeCode);
            logRecordService.recordOperationLog("收运通知单管理", "删除",
                    "收运通知单号=" + noticeCode,
                    userId, ipAddress, true, null);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "删除",
                    "收运通知单号=" + noticeCode,
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除收运通知单失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "删除",
                    "收运通知单号=" + noticeCode,
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        }
    }

    /**
     * 根据危废代码查询危废类别限额与已入库量
     */
    @GetMapping("/waste-category-limit")
    @ApiOperation(value = "查询危废类别限额与已入库量", notes = "根据废物代码列表返回对应类别的限额信息和限额期已入库量")
    public Result<List<WasteCategoryLimitResponse>> getWasteCategoryLimit(@RequestParam String codes) {
        if (codes == null || codes.trim().isEmpty()) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "废物代码不能为空");
        }
        try {
            List<String> codeList = Arrays.asList(codes.split(","));
            List<WasteCategoryLimitResponse> data = pickupNoticeService.getWasteCategoryLimits(codeList);
            return Result.success("查询成功", data);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询危废类别限额失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 导出车辆安排Excel（车辆安排页面专用）
     */
    @RequirePagePermission("运输管理:车辆安排:页面")
    @RequireActionPermission("运输管理:车辆安排:批量导出")
    @GetMapping("/vehicle-arrange/export")
    @ApiOperation(value = "导出车辆安排", notes = "根据筛选条件导出车辆安排Excel文件")
    public void exportVehicleArrange(TransportApplyPageRequest request, HttpServletResponse response,
                                     HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 导出接口不需要分页参数，设置为null避免验证错误
            request.setCurrent(null);
            request.setSize(null);
            List<TransportApplyPageResponse> list = pickupNoticeService.listVehicleArrangeNoticesForExport(request);
            writeVehicleArrangeExport(list, response);
            logRecordService.recordOperationLog("车辆安排管理", "导出",
                    "导出车辆安排列表，共" + list.size() + "条记录", userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("车辆安排管理", "导出",
                    "导出车辆安排列表失败", userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出车辆安排失败", e);
            logRecordService.recordOperationLog("车辆安排管理", "导出",
                    "导出车辆安排列表失败", userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    /**
     * 写入车辆安排导出Excel
     */
    private void writeVehicleArrangeExport(List<TransportApplyPageResponse> data, HttpServletResponse response) throws Exception {
        response.reset();
        String fileName = "车辆安排导出.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + encodedFileName + "\";filename*=UTF-8''" + encodedFileName);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建支持中文的字体
            Font font = workbook.createFont();
            font.setFontName("宋体");
            font.setCharSet(Font.DEFAULT_CHARSET);
            
            // 创建单元格样式
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);
            
            // 创建表头样式（加粗）
            Font headerFont = workbook.createFont();
            headerFont.setFontName("宋体");
            headerFont.setCharSet(Font.DEFAULT_CHARSET);
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            
            Sheet sheet = workbook.createSheet("车辆安排");
            // 根据前端表格字段定义导出列（车辆安排模块的列）
            String[] headers = {
                    "通知单号", "合同号", "客户名称", "统一社会信用代码", 
                    "现场联系人", "现场联系电话", "转运地址", "计划转移时间", 
                    "状态", "备注"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            
            for (int i = 0; i < data.size(); i++) {
                TransportApplyPageResponse item = data.get(i);
                Row row = sheet.createRow(i + 1);
                int col = 0;
                Cell cell;
                
                // 通知单号
                cell = row.createCell(col++);
                cell.setCellValue(item.getNoticeCode() == null ? "" : item.getNoticeCode());
                cell.setCellStyle(cellStyle);
                
                // 合同号
                cell = row.createCell(col++);
                cell.setCellValue(item.getContractCode() == null ? "" : item.getContractCode());
                cell.setCellStyle(cellStyle);
                
                // 客户名称
                cell = row.createCell(col++);
                cell.setCellValue(item.getCompanyName() == null ? "" : item.getCompanyName());
                cell.setCellStyle(cellStyle);
                
                // 统一社会信用代码
                cell = row.createCell(col++);
                cell.setCellValue(item.getCreditCode() == null ? "" : item.getCreditCode());
                cell.setCellStyle(cellStyle);
                
                // 现场联系人
                cell = row.createCell(col++);
                cell.setCellValue(item.getOnsiteContact() == null ? "" : item.getOnsiteContact());
                cell.setCellStyle(cellStyle);
                
                // 现场联系电话
                cell = row.createCell(col++);
                cell.setCellValue(item.getOnsitePhone() == null ? "" : item.getOnsitePhone());
                cell.setCellStyle(cellStyle);
                
                // 转运地址
                cell = row.createCell(col++);
                cell.setCellValue(item.getTransportAddress() == null ? "" : item.getTransportAddress());
                cell.setCellStyle(cellStyle);
                
                // 计划转移时间
                cell = row.createCell(col++);
                String transferTime = item.getTransferTime() == null ? 
                        (item.getPlanTransferDate() == null ? "" : item.getPlanTransferDate()) : item.getTransferTime();
                cell.setCellValue(transferTime);
                cell.setCellStyle(cellStyle);
                
                // 状态
                cell = row.createCell(col++);
                cell.setCellValue(item.getStatus() == null ? "" : item.getStatus());
                cell.setCellStyle(cellStyle);
                
                // 备注
                cell = row.createCell(col);
                cell.setCellValue(item.getRemark() == null ? "" : item.getRemark());
                cell.setCellStyle(cellStyle);
            }
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        }
    }

    /**
     * 批量提交收运通知单审核
     */
    @RequireActionPermission("业务管理:收运通知:批量提交审核")
    @PostMapping("/batch-submit")
    @ApiOperation(value = "批量提交收运通知单审核", notes = "将多个收运通知单状态从'待审核'或'已驳回'变更为'审核中'")
    public Result<Void> batchSubmitTransportApply(
            @Valid @RequestBody TransportApplyBatchSubmitRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            pickupNoticeService.batchSubmitPickupNotices(request.getNoticeCodes());
            logRecordService.recordOperationLog("收运通知单管理", "批量提交审核",
                    "批量提交收运通知单：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, true, null);
            return Result.success("批量提交审核成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "批量提交审核",
                    "批量提交收运通知单失败：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量提交收运通知单审核失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "批量提交审核",
                    "批量提交收运通知单失败：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量撤回收运通知单
     */
    @RequireActionPermission("业务管理:收运通知:批量撤回")
    @PostMapping("/batch-revoke")
    @ApiOperation(value = "批量撤回收运通知单", notes = "将多个收运通知单状态从'审核中'变更为'待审核'")
    public Result<Void> batchRevokeTransportApply(
            @Valid @RequestBody TransportApplyBatchRevokeRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            pickupNoticeService.batchRevokePickupNotices(request.getNoticeCodes());
            logRecordService.recordOperationLog("收运通知单管理", "批量撤回",
                    "批量撤回收运通知单：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, true, null);
            return Result.success("批量撤回成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "批量撤回",
                    "批量撤回收运通知单失败：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量撤回收运通知单失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "批量撤回",
                    "批量撤回收运通知单失败：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量撤回失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除收运通知单
     */
    @RequireActionPermission("业务管理:收运通知:批量删除")
    @DeleteMapping("/batch-delete")
    @ApiOperation(value = "批量删除收运通知单", notes = "批量删除待审核或已驳回状态的收运通知单")
    public Result<Void> batchDeleteTransportApply(
            @Valid @RequestBody TransportApplyBatchDeleteRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            pickupNoticeService.batchDeletePickupNotices(request.getNoticeCodes());
            logRecordService.recordOperationLog("收运通知单管理", "批量删除",
                    "批量删除收运通知单：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, true, null);
            return Result.success("批量删除成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("收运通知单管理", "批量删除",
                    "批量删除收运通知单失败：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量删除收运通知单失败", e);
            logRecordService.recordOperationLog("收运通知单管理", "批量删除",
                    "批量删除收运通知单失败：noticeCodes=" + request.getNoticeCodes(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量删除失败：" + e.getMessage());
        }
    }
}

