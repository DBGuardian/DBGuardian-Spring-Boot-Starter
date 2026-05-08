package com.erp.controller.report;

import com.erp.common.result.Result;
import com.erp.controller.report.dto.InoutDetailRequest;
import com.erp.controller.report.dto.InoutDetailResponse;
import com.erp.controller.report.dto.InoutExcelRow;
import com.erp.service.report.InoutDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 出入库明细表 Controller
 * 
 * 功能描述：
 * - 获取出入库明细表数据（支持缓存）
 * - 重新计算出入库明细表（清除缓存）
 * - 清除所有出入库缓存
 * - 导出Excel
 */
@Slf4j
@Tag(name = "出入库明细表", description = "出入库明细表相关接口")
@RestController
@RequestMapping("/report/inout")
@RequiredArgsConstructor
public class InoutDetailController {

  private final InoutDetailService inoutDetailService;

  /**
   * 获取出入库明细表
   * 
   * 功能描述：获取出入库明细数据（支持分页、筛选、缓存）
   * 
   * 入参：InoutDetailRequest
   * 返回参数：Result<InoutDetailResponse>
   * url地址：/api/report/inout/detail-list
   * 请求方式：POST
   */
  @Operation(summary = "获取出入库明细表", description = "获取出入库明细数据，支持分页、筛选、缓存")
  @PostMapping("/detail-list")
  public Result<InoutDetailResponse> getDetailList(@RequestBody InoutDetailRequest request) {
    InoutDetailResponse response = inoutDetailService.getDetailList(request);
    return Result.success(response);
  }

  /**
   * 重新计算出入库明细表（清除缓存）
   * 
   * 功能描述：清除缓存并重新计算出入库明细数据
   * 
   * 入参：InoutDetailRequest
   * 返回参数：Result<InoutDetailResponse>
   * url地址：/api/report/inout/detail-list/recalculate
   * 请求方式：POST
   */
  @Operation(summary = "重新计算出入库明细表", description = "清除缓存并重新计算出入库明细数据")
  @PostMapping("/detail-list/recalculate")
  public Result<InoutDetailResponse> recalculateDetailList(@RequestBody InoutDetailRequest request) {
    InoutDetailResponse response = inoutDetailService.recalculateDetailList(request);
    return Result.success(response);
  }

  /**
   * 清除所有出入库缓存
   * 
   * 功能描述：管理员操作，清除所有出入库相关缓存
   * 
   * 入参：无
   * 返回参数：Result<Map<String, Object>>
   * url地址：/api/report/inout/cache/clear
   * 请求方式：POST
   */
  @Operation(summary = "清除所有出入库缓存", description = "管理员操作，清除所有出入库相关缓存")
  @PostMapping("/cache/clear")
  public Result<Map<String, Object>> clearCache() {
    inoutDetailService.clearAllCache();
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("message", "所有出入库缓存已清除");
    return Result.success(result);
  }

  /**
   * 导出出入库明细表 Excel
   *
   * 接口地址：POST /api/report/inout/export-excel
   * 功能描述：根据筛选条件导出出入库明细表Excel文件
   * 入参：InoutDetailRequest（筛选条件，不含分页参数）
   * 返回：Excel 文件流
   * 请求方式：POST
   */
  @Operation(summary = "导出出入库明细表 Excel", description = "根据筛选条件导出出入库明细表Excel文件")
  @PostMapping("/export-excel")
  public void exportExcel(@RequestBody InoutDetailRequest request,
                          HttpServletResponse response) {
    try {
      // 1. 查询全量数据
      List<InoutExcelRow> excelRows = inoutDetailService.queryAllForExport(request);

      // 2. 生成 Excel
      byte[] data = buildExcel(excelRows);

      // 3. 设置响应头
      String fileName = "出入库明细表_"
          + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
          + ".xlsx";
      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.setHeader("Content-Disposition",
          "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, "UTF-8"));
      response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
      response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
      response.getOutputStream().write(data);
      response.getOutputStream().flush();

    } catch (Exception e) {
      log.error("导出出入库明细表 Excel 失败", e);
      try {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
      } catch (IOException ex) {
        log.error("发送错误响应失败", ex);
      }
    }
  }

  // ─────────────────────────────────────────────────────────────────
  // Excel 构建工具方法
  // ─────────────────────────────────────────────────────────────────

  private static final int COL_SEQ = 0;
  private static final int COL_CATEGORY = 1;
  private static final int COL_CONTRACT_NO = 2;
  private static final int COL_PARTY_NAME = 3;
  private static final int COL_BUSINESS_PERSON = 4;
  private static final int COL_PLATE_NUMBER = 5;
  private static final int COL_WEIGHING_SLIP = 6;
  private static final int COL_WASTE_CATEGORY = 7;
  private static final int COL_WASTE_CODE = 8;
  private static final int COL_WASTE_NAME = 9;
  private static final int COL_UNIT = 10;
  private static final int COL_INBOUND_QTY = 11;
  private static final int COL_OUTBOUND_QTY = 12;
  private static final int COL_STOCK_QTY = 13;
  private static final int COL_REMARK = 14;

  private static final String[] HEADERS = {
      "序号", "分类", "合同编号", "对方名称", "业务员", "车牌号", "总磅单号",
      "废物类别", "废物代码", "废物名称", "计量单位", "入库数量", "出库数量", "库存数量", "备注"
  };

  private byte[] buildExcel(List<InoutExcelRow> excelRows) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("出入库明细表");

      CellStyle headerStyle = createHeaderStyle(workbook);
      CellStyle centeredDataStyle = createDataStyle(workbook, HorizontalAlignment.CENTER);
      CellStyle leftAlignedDataStyle = createDataStyle(workbook, HorizontalAlignment.LEFT);
      CellStyle amountStyle = createAmountStyle(workbook);
      CellStyle centeredSummaryStyle = createSummaryStyle(workbook, HorizontalAlignment.CENTER, false);
      CellStyle leftAlignedSummaryStyle = createSummaryStyle(workbook, HorizontalAlignment.LEFT, false);
      CellStyle amountSummaryStyle = createSummaryStyle(workbook, HorizontalAlignment.RIGHT, true);

      // 表头
      Row headerRow = sheet.createRow(0);
      headerRow.setHeightInPoints(25);
      for (int i = 0; i < HEADERS.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(HEADERS[i]);
        cell.setCellStyle(headerStyle);
      }

      // 数据行
      int rowIndex = 1;
      for (InoutExcelRow excelRow : excelRows) {
        Row row = sheet.createRow(rowIndex);

        setCell(row, COL_SEQ, String.valueOf(excelRow.getSequenceNo()), centeredDataStyle);
        setCell(row, COL_CATEGORY, excelRow.getCategory(), centeredDataStyle);
        setCell(row, COL_CONTRACT_NO, excelRow.getContractNo(), centeredDataStyle);
        setCell(row, COL_PARTY_NAME, excelRow.getPartyName(), leftAlignedDataStyle);
        setCell(row, COL_BUSINESS_PERSON, excelRow.getBusinessPerson(), centeredDataStyle);
        setCell(row, COL_PLATE_NUMBER, excelRow.getPlateNumber(), centeredDataStyle);
        setCell(row, COL_WEIGHING_SLIP, excelRow.getWeighingSlipNo(), centeredDataStyle);
        setCell(row, COL_WASTE_CATEGORY, excelRow.getWasteCategory(), centeredDataStyle);
        setCell(row, COL_WASTE_CODE, excelRow.getWasteCode(), centeredDataStyle);
        setCell(row, COL_WASTE_NAME, excelRow.getWasteName(), leftAlignedDataStyle);
        setCell(row, COL_UNIT, excelRow.getUnit(), centeredDataStyle);
        setAmountCell(row, COL_INBOUND_QTY, excelRow.getInboundQty(), amountStyle);
        setAmountCell(row, COL_OUTBOUND_QTY, excelRow.getOutboundQty(), amountStyle);
        setAmountCell(row, COL_STOCK_QTY, excelRow.getStockQty(), amountStyle);
        setCell(row, COL_REMARK, excelRow.getRemark(), leftAlignedDataStyle);

        rowIndex++;
      }

      // 合计行
      if (!excelRows.isEmpty()) {
        Row summaryRow = sheet.createRow(rowIndex);
        
        BigDecimal inboundQty = BigDecimal.ZERO;
        BigDecimal outboundQty = BigDecimal.ZERO;
        BigDecimal stockQty = BigDecimal.ZERO;

        for (InoutExcelRow row : excelRows) {
          if (row.getInboundQty() != null) inboundQty = inboundQty.add(row.getInboundQty());
          if (row.getOutboundQty() != null) outboundQty = outboundQty.add(row.getOutboundQty());
          if (row.getStockQty() != null) stockQty = stockQty.add(row.getStockQty());
        }

        // 为合计行的所有列设置样式
        setCell(summaryRow, COL_SEQ, "合计", centeredSummaryStyle);
        setCell(summaryRow, COL_CATEGORY, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_CONTRACT_NO, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_PARTY_NAME, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_BUSINESS_PERSON, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_PLATE_NUMBER, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_WEIGHING_SLIP, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_WASTE_CATEGORY, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_WASTE_CODE, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_WASTE_NAME, "-", centeredSummaryStyle);
        setCell(summaryRow, COL_UNIT, "-", centeredSummaryStyle);
        setAmountCell(summaryRow, COL_INBOUND_QTY, inboundQty, amountSummaryStyle);
        setAmountCell(summaryRow, COL_OUTBOUND_QTY, outboundQty, amountSummaryStyle);
        setAmountCell(summaryRow, COL_STOCK_QTY, stockQty, amountSummaryStyle);
        setCell(summaryRow, COL_REMARK, "-", centeredSummaryStyle);
      }

      // 列宽自适应
      for (int i = 0; i < HEADERS.length; i++) {
        sheet.autoSizeColumn(i);
        if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private void setCell(Row row, int col, String value, CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(value == null ? "" : value);
    cell.setCellStyle(style);
  }

  private void setAmountCell(Row row, int col, BigDecimal value, CellStyle style) {
    Cell cell = row.createCell(col);
    if (value != null) {
      cell.setCellValue(value.doubleValue());
    } else {
      cell.setCellValue(0.0);
    }
    cell.setCellStyle(style);
  }

  private CellStyle createHeaderStyle(XSSFWorkbook wb) {
    CellStyle style = wb.createCellStyle();
    XSSFFont font = wb.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
    style.setFillForegroundColor(new XSSFColor(new byte[]{0x1E, 0x3A, 0x5F}, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    setBorder(style);
    return style;
  }

  private CellStyle createDataStyle(XSSFWorkbook wb, HorizontalAlignment alignment) {
    CellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF}, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(alignment);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    setBorder(style);
    return style;
  }

  private CellStyle createAmountStyle(XSSFWorkbook wb) {
    CellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF}, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
    setBorder(style);
    return style;
  }

  private CellStyle createSummaryStyle(XSSFWorkbook wb, HorizontalAlignment alignment, boolean numericFormat) {
    CellStyle style = wb.createCellStyle();
    XSSFFont font = wb.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xF5,(byte)0xF5,(byte)0xF5}, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(alignment);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    if (numericFormat) {
      style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
    }
    setBorder(style);
    return style;
  }

  private void setBorder(CellStyle style) {
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
  }
}
