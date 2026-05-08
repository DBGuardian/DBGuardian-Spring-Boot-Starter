package com.erp.controller.finance;

import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundSummaryRequest;
import com.erp.controller.finance.dto.FundSummaryResponse;
import com.erp.service.finance.FundSummaryService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.common.annotation.RequirePagePermission;
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
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 汇总表控制器
 */
@Slf4j
@RestController
@RequestMapping("/fund")
@Api(tags = "汇总表管理")
@Validated
public class FundSummaryController {

    @Autowired
    private FundSummaryService fundSummaryService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 获取汇总表数据
     *
     * 接口名称：获取汇总表数据
     * 功能描述：查询多个账户或账户组合在指定账期的汇总数据
     * 接口地址：/api/fund/summary
     * 请求方式：GET
     *
     * 请求参数：
     * - account_ids：账户ID列表（可选，与group_id二选一）
     * - group_id：账户组合ID（可选，与account_ids二选一）
     * - period_id：账期ID（可选，与year+period二选一）
     * - year：年份（可选，与period_id二选一）
     * - period：期间（可选，与period_id二选一）
     *
     * 返回体 data：FundSummaryResponse
     */
    @RequirePagePermission("财务管理:资金管理:汇总表:页面")
    @GetMapping("/summary")
    @ApiOperation(value = "获取汇总表数据", notes = "查询多个账户或账户组合在指定账期的汇总数据")
    public Result<FundSummaryResponse> getSummary(
            @RequestParam(value = "organization_id", required = false) Long organizationId,
            @RequestParam(value = "period_id", required = false) Long periodId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "quarter", required = false) Integer quarter
    ) {
        try {
            FundSummaryRequest req = new FundSummaryRequest();
            req.setOrganizationId(organizationId);
            req.setPeriodId(periodId);
            req.setYear(year);
            req.setQuarter(quarter);

            FundSummaryResponse response = fundSummaryService.getSummary(req);
            return Result.success("获取汇总表数据成功", response);
        } catch (Exception e) {
            log.error("获取汇总表数据失败，organizationId={}, periodId={}, year={}, quarter={}", organizationId, periodId, year, quarter, e);
            return Result.error("获取汇总表数据失败：" + e.getMessage());
        }
    }

    /**
     * 导出汇总表Excel
     *
     * 接口名称：导出汇总表Excel
     * 功能描述：导出汇总表数据为Excel文件
     * 接口地址：/api/fund/summary/export
     * 请求方式：GET
     *
     * 请求参数：
     * - organization_id：组织ID（必填）
     * - period_id：账期ID（可选，与year+quarter二选一）
     * - year：年份（可选，与period_id二选一）
     * - quarter：季度（可选，与period_id二选一）
     *
     * 返回：Excel文件流
     */
    @GetMapping("/summary/export")
    @ApiOperation(value = "导出汇总表Excel", notes = "导出汇总表数据为Excel文件")
    public void exportSummary(
            @RequestParam(value = "organization_id", required = true) Long organizationId,
            @RequestParam(value = "period_id", required = false) Long periodId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "quarter", required = false) Integer quarter,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean exportSuccess = false;
        String errorMessage = null;

        try {
            FundSummaryRequest req = new FundSummaryRequest();
            req.setOrganizationId(organizationId);
            req.setPeriodId(periodId);
            req.setYear(year);
            req.setQuarter(quarter);

            FundSummaryResponse summaryData = fundSummaryService.getSummary(req);
            writeExport(summaryData, response);
            exportSuccess = true;
        } catch (Exception e) {
            log.error("导出汇总表Excel失败，organizationId={}, periodId={}, year={}, quarter={}", organizationId, periodId, year, quarter, e);
            errorMessage = e.getMessage();
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("导出失败：" + e.getMessage());
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("导出汇总表Excel：组织ID=%s，账期ID=%s，年份=%s，季度=%s",
                        organizationId, periodId, year, quarter);
                logRecordService.recordOperationLog("汇总表管理", "导出",
                        logContent, userId, ipAddress, exportSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录导出汇总表操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (exportSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SUMMARY_EXPORT", null, "汇总表已导出", "导出", userId);
                } catch (Exception msgEx) {
                    log.warn("发送汇总表导出通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 写入导出Excel - 支持科目级别汇总表
     */
    private void writeExport(FundSummaryResponse summaryData, HttpServletResponse response) throws Exception {
        // 设置响应头（统一使用时间戳格式，与日记账保持一致）
        String fileName = "汇总表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建支持中文的字体
            Font font = workbook.createFont();
            font.setFontName("宋体");
            font.setCharSet(Font.DEFAULT_CHARSET);

            // 创建单元格样式
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 创建表头样式（加粗）
            Font headerFont = workbook.createFont();
            headerFont.setFontName("宋体");
            headerFont.setCharSet(Font.DEFAULT_CHARSET);
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 创建数字格式样式
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(cellStyle);
            DataFormat dataFormat = workbook.createDataFormat();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            // 创建标题样式（加粗，居中）
            Font titleFont = workbook.createFont();
            titleFont.setFontName("宋体");
            titleFont.setCharSet(Font.DEFAULT_CHARSET);
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Sheet sheet = workbook.createSheet("汇总表");

            // 计算季度月份范围
            List<Integer> quarterMonths = getQuarterMonths(summaryData.getMonth());
            int totalColumns = 3 + quarterMonths.size() + 3; // 科目 + 期初 + 月份们 + 季度合计 + 期末 + 年度合计

            // 创建标题行
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("财务汇总表");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalColumns - 1));

            // 创建账期信息行
            Row periodRow = sheet.createRow(1);
            Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue("账期：" + summaryData.getYear() + "年第" + getQuarterString(summaryData.getMonth()) + "季度");
            periodCell.setCellStyle(cellStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, totalColumns - 1));

            // 创建表头
            Row headerRow = sheet.createRow(2);

            // 科目列
            Cell subjectCell = headerRow.createCell(0);
            subjectCell.setCellValue("科目（编码 / 名称）");
            subjectCell.setCellStyle(headerStyle);

            // 期初余额
            Cell openingCell = headerRow.createCell(1);
            openingCell.setCellValue("期初余额");
            openingCell.setCellStyle(headerStyle);

            // 月份列
            int colIndex = 2;
            for (Integer month : quarterMonths) {
                Cell monthCell = headerRow.createCell(colIndex++);
                monthCell.setCellValue(month + "月");
                monthCell.setCellStyle(headerStyle);
            }

            // 季度合计
            Cell quarterCell = headerRow.createCell(colIndex++);
            quarterCell.setCellValue("季度合计");
            quarterCell.setCellStyle(headerStyle);

            // 期末余额
            Cell closingCell = headerRow.createCell(colIndex++);
            closingCell.setCellValue("期末余额");
            closingCell.setCellStyle(headerStyle);

            // 年度合计
            Cell yearCell = headerRow.createCell(colIndex++);
            yearCell.setCellValue("年度合计");
            yearCell.setCellStyle(headerStyle);

            // 设置列宽
            sheet.setColumnWidth(0, 25 * 256); // 科目（编码/名称）
            sheet.setColumnWidth(1, 15 * 256); // 期初余额
            for (int i = 0; i < quarterMonths.size(); i++) {
                sheet.setColumnWidth(2 + i, 15 * 256); // 月份列
            }
            sheet.setColumnWidth(2 + quarterMonths.size(), 15 * 256); // 季度合计
            sheet.setColumnWidth(3 + quarterMonths.size(), 15 * 256); // 期末余额
            sheet.setColumnWidth(4 + quarterMonths.size(), 15 * 256); // 年度合计

            // 写入数据
            int rowIndex = 3;

            // 获取汇总数据行
            List<Map<String, Object>> rows = summaryData.getRows();
            if (rows != null && !rows.isEmpty()) {
                for (Map<String, Object> rowData : rows) {
                    Row row = sheet.createRow(rowIndex++);

                    // 科目列
                    String subjectDisplay = "";
                    String subjectCode = (String) rowData.get("subjectCode");
                    String subjectName = (String) rowData.get("subjectName");
                    if (subjectCode != null && !subjectCode.isEmpty()) {
                        subjectDisplay += subjectCode;
                    }
                    if (subjectName != null && !subjectName.isEmpty()) {
                        if (!subjectDisplay.isEmpty()) {
                            subjectDisplay += " ";
                        }
                        subjectDisplay += subjectName;
                    }
                    createCell(row, 0, subjectDisplay, cellStyle);

                    // 期初余额
                    BigDecimal openingBalance = (BigDecimal) rowData.get("openingBalance");
                    createCell(row, 1, openingBalance, numberStyle);

                    // 月份列
                    colIndex = 2;
                    for (Integer month : quarterMonths) {
                        String monthKey = "m" + month;
                        BigDecimal monthValue = (BigDecimal) rowData.get(monthKey);
                        createCell(row, colIndex++, monthValue, numberStyle);
                    }

                    // 季度合计
                    BigDecimal quarterTotal = (BigDecimal) rowData.get("quarterTotal");
                    createCell(row, colIndex++, quarterTotal, numberStyle);

                    // 期末余额
                    BigDecimal closingBalance = (BigDecimal) rowData.get("closingBalance");
                    createCell(row, colIndex++, closingBalance, numberStyle);

                    // 年度合计
                    BigDecimal yearTotal = (BigDecimal) rowData.get("yearTotal");
                    createCell(row, colIndex++, yearTotal, numberStyle);
                }
            }

            // 写入响应流
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        }
    }

    /**
     * 创建单元格并设置值
     */
    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 根据月份获取季度字符串
     */
    private String getQuarterString(Integer month) {
        if (month == null) return "";
        if (month <= 3) return "1";
        if (month <= 6) return "2";
        if (month <= 9) return "3";
        return "4";
    }

    /**
     * 根据月份获取季度包含的月份列表
     * 注意：这里的month参数是从FundSummaryServiceImpl中传递过来的targetMonth，
     * 它已经是季度对应的最后一个月（1季度->3月，2季度->6月等）
     */
    private List<Integer> getQuarterMonths(Integer month) {
        List<Integer> months = new ArrayList<>();
        if (month == null) return months;

        // 根据月份确定季度，然后返回该季度包含的所有月份
        int quarter;
        if (month <= 3) quarter = 1;
        else if (month <= 6) quarter = 2;
        else if (month <= 9) quarter = 3;
        else quarter = 4;

        int startMonth = (quarter - 1) * 3 + 1;
        months.add(startMonth);
        months.add(startMonth + 1);
        months.add(startMonth + 2);

        return months;
    }
}

