package com.erp.controller.report;

import com.erp.common.result.Result;
import com.erp.controller.report.dto.ReceivableDetailRequest;
import com.erp.controller.report.dto.ReceivableDetailResponse;
import com.erp.controller.report.dto.ReceivableExcelRow;
import com.erp.service.report.ReceivableDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 应收账款明细表 Controller
 * 
 * 功能描述：
 * - 获取应收账款明细表数据（支持缓存）
 * - 重新计算应收账款明细表（清除缓存）
 * - 清除所有应收账款缓存
 */
@Slf4j
@Tag(name = "应收账款明细表", description = "应收账款明细表相关接口")
@RestController
@RequestMapping("/report/receivables")
@RequiredArgsConstructor
public class ReceivableDetailController {

  private final ReceivableDetailService receivableDetailService;

  /**
   * 获取应收账款明细表
   * 
   * 功能描述：获取合同、结算单、发票、应收明细的树形数据（仅返回结算类型为RECEIVABLE的数据）
   * 
   * 入参：ReceivableDetailRequest
   * 返回参数：Result<ReceivableDetailResponse>
   * url地址：/api/report/receivables/detail-list
   * 请求方式：POST
   */
  @Operation(summary = "获取应收账款明细表", description = "获取合同、结算单、发票、应收明细的树形数据")
  @PostMapping("/detail-list")
  public Result<ReceivableDetailResponse> getDetailList(@RequestBody ReceivableDetailRequest request) {
    ReceivableDetailResponse response = receivableDetailService.getDetailList(request);
    return Result.success(response);
  }

  /**
   * 重新计算应收账款明细表（清除缓存）
   * 
   * 功能描述：清除缓存并重新计算应收账款明细数据
   * 
   * 入参：ReceivableDetailRequest
   * 返回参数：Result<ReceivableDetailResponse>
   * url地址：/api/report/receivables/detail-list/recalculate
   * 请求方式：POST
   */
  @Operation(summary = "重新计算应收账款明细表", description = "清除缓存并重新计算应收账款明细数据")
  @PostMapping("/detail-list/recalculate")
  public Result<ReceivableDetailResponse> recalculateDetailList(@RequestBody ReceivableDetailRequest request) {
    ReceivableDetailResponse response = receivableDetailService.recalculateDetailList(request);
    return Result.success(response);
  }

  /**
   * 清除所有应收账款缓存
   * 
   * 功能描述：管理员操作，清除所有应收账款相关缓存
   * 
   * 入参：无
   * 返回参数：Result<Map<String, Object>>
   * url地址：/api/report/receivables/cache/clear
   * 请求方式：POST
   */
  @Operation(summary = "清除所有应收账款缓存", description = "管理员操作，清除所有应收账款相关缓存")
  @PostMapping("/cache/clear")
  public Result<Map<String, Object>> clearCache() {
    receivableDetailService.clearAllCache();
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("message", "所有应收账款缓存已清除");
    return Result.success(result);
  }

  /**
   * 导出应收账款明细表 Excel（合并单元格模式）
   *
   * 接口地址：POST /api/report/receivables/export-excel
   * 功能描述：根据筛选条件导出应收账款明细表，支持树形勾选节点优先导出；合同列按合同合并，结算单列按结算单合并，
   *           发票与应收明细合并为同一行
   * 入参：ReceivableDetailRequest（筛选条件，不含分页参数）
   * 返回：Excel 文件流
   * 请求方式：POST
   */
  @Operation(summary = "导出应收账款明细表 Excel", description = "合并单元格模式，合同列按合同合并，结算单列按结算单合并")
  @PostMapping("/export-excel")
  public void exportExcel(@RequestBody ReceivableDetailRequest request,
                          HttpServletResponse response) {
    try {
      // 1. 查询全量数据并展平
      List<ReceivableExcelRow> excelRows = receivableDetailService.queryAllForExport(request);

      // 2. 生成 Excel
      byte[] data = buildExcel(excelRows);

      // 3. 设置响应头
      String fileName = "应收账款明细表_"
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
      log.error("导出应收账款明细表 Excel 失败", e);
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

  /** 列索引常量（0-based） */
  private static final int COL_SEQ           = 0;
  private static final int COL_SETTLE_TYPE   = 1;
  private static final int COL_PRICING_MODE  = 2;
  private static final int COL_PARTY_A       = 3;
  private static final int COL_CONTRACT_NO   = 4;
  private static final int COL_SIGN_DATE     = 5;
  private static final int COL_CONTRACT_AMT  = 6;
  private static final int COL_CONTRACT_BIZ  = 7;
  private static final int COL_SETTLE_CODE   = 8;
  private static final int COL_SETTLE_PERIOD = 9;
  private static final int COL_SETTLE_AMT    = 10;
  private static final int COL_SETTLE_BIZ    = 11;
  private static final int COL_INV_NO        = 12;
  private static final int COL_INV_DATE      = 13;
  private static final int COL_INV_AMT       = 14;
  private static final int COL_TAX_AMT       = 15;
  private static final int COL_TOTAL_AMT     = 16;
  private static final int COL_INV_BIZ       = 17;
  private static final int COL_RECEIVABLE    = 18;
  private static final int COL_RECEIVED      = 19;
  private static final int COL_RECV_DATE     = 20;
  private static final int COL_OUTSTANDING   = 21;
  private static final int COL_DAYS_PAYMENT  = 22;
  private static final int COL_ACCOUNT_AGE   = 23;

  private static final String[] HEADERS = {
      "序号", "结算类型", "合同类型", "客户名称", "合同编号", "合同签订日期", "合同金额", "业务员（合同）",
      "结算单编号", "结算期间", "结算金额", "业务员（结算单）",
      "发票号码", "开票日期", "发票金额", "税额", "价税合计", "业务员（发票）",
      "应收金额", "已收金额", "收款日期", "未收金额", "回款天数", "应收账龄（天）"
  };

  private byte[] buildExcel(List<ReceivableExcelRow> excelRows) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("应收账款明细表");

      CellStyle headerStyle   = createHeaderStyle(workbook);
      CellStyle contractStyle = createBgStyle(workbook, new byte[]{(byte)0xEB,(byte)0xF3,(byte)0xFB});
      CellStyle settleStyle   = createBgStyle(workbook, new byte[]{(byte)0xF0,(byte)0xF7,(byte)0xEC});
      CellStyle invoiceStyle  = createBgStyle(workbook, new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF});
      CellStyle arStyle       = createBgStyle(workbook, new byte[]{(byte)0xFF,(byte)0xFB,(byte)0xF0});
      CellStyle amountStyle   = createAmountStyle(workbook);
      CellStyle dateStyle     = createDateStyle(workbook);
      CellStyle summaryStyle  = createSummaryStyle(workbook);

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
      for (ReceivableExcelRow excelRow : excelRows) {
        Row row = sheet.createRow(rowIndex);

        // 合同字段（A~H）
        if (excelRow.getContractRowSpan() > 0) {
          setCell(row, COL_SEQ,          String.valueOf(excelRow.getContractSeq()), contractStyle);
          setCell(row, COL_SETTLE_TYPE,  excelRow.getSettlementType(),  contractStyle);
          setCell(row, COL_PRICING_MODE, excelRow.getPricingMode(),     contractStyle);
          setCell(row, COL_PARTY_A,      excelRow.getPartyAName(),      contractStyle);
          setCell(row, COL_CONTRACT_NO,  excelRow.getContractNo(),      contractStyle);
          setDateCell(row, COL_SIGN_DATE, excelRow.getContractSignDate(), dateStyle);
          setAmountCell(row, COL_CONTRACT_AMT, excelRow.getContractAmount(), amountStyle);
          setCell(row, COL_CONTRACT_BIZ, excelRow.getContractBizPerson(), contractStyle);
          if (excelRow.getContractRowSpan() > 1) {
            int span = excelRow.getContractRowSpan();
            for (int col = COL_SEQ; col <= COL_CONTRACT_BIZ; col++) {
              sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + span - 1, col, col));
            }
          }
        }

        // 结算单字段（I~L）
        if (excelRow.getSettlementRowSpan() > 0) {
          setCell(row, COL_SETTLE_CODE,   excelRow.getSettlementCode(),     settleStyle);
          setCell(row, COL_SETTLE_PERIOD, excelRow.getSettlementPeriod(),   settleStyle);
          setAmountCell(row, COL_SETTLE_AMT, excelRow.getSettlementAmount(), amountStyle);
          setCell(row, COL_SETTLE_BIZ,    excelRow.getSettlementBizPerson(), settleStyle);
          if (excelRow.getSettlementRowSpan() > 1) {
            int span = excelRow.getSettlementRowSpan();
            for (int col = COL_SETTLE_CODE; col <= COL_SETTLE_BIZ; col++) {
              sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex + span - 1, col, col));
            }
          }
        }

        // 发票字段（M~R）
        setCell(row, COL_INV_NO,     excelRow.getInvoiceNumber(),  invoiceStyle);
        setDateCell(row, COL_INV_DATE, excelRow.getInvoiceDate(),  dateStyle);
        setAmountCell(row, COL_INV_AMT,   excelRow.getInvoiceAmount(),  amountStyle);
        setAmountCell(row, COL_TAX_AMT,   excelRow.getTaxAmount(),      amountStyle);
        setAmountCell(row, COL_TOTAL_AMT, excelRow.getTotalAmount(),    amountStyle);
        setCell(row, COL_INV_BIZ,    excelRow.getInvoiceBizPerson(), invoiceStyle);

        // 应收明细字段（S~X）
        setAmountCell(row, COL_RECEIVABLE,  excelRow.getReceivableAmount(),  amountStyle);
        setAmountCell(row, COL_RECEIVED,    excelRow.getReceivedAmount(),    amountStyle);
        setDateCell(row, COL_RECV_DATE, excelRow.getReceivedDate(), dateStyle);
        setAmountCell(row, COL_OUTSTANDING, excelRow.getOutstandingAmount(), amountStyle);
        setIntCell(row, COL_DAYS_PAYMENT, excelRow.getDaysToPayment(), arStyle);
        setIntCell(row, COL_ACCOUNT_AGE,  excelRow.getAccountAge(),   arStyle);

        rowIndex++;
      }

      // 合计行
      if (!excelRows.isEmpty()) {
        Row summaryRow = sheet.createRow(rowIndex);
        setCell(summaryRow, COL_SEQ, "合计", summaryStyle);
        BigDecimal contractAmt = BigDecimal.ZERO, settleAmt = BigDecimal.ZERO;
        BigDecimal invAmt = BigDecimal.ZERO, taxAmt = BigDecimal.ZERO, totalAmt = BigDecimal.ZERO;
        BigDecimal receivable = BigDecimal.ZERO, received = BigDecimal.ZERO, outstanding = BigDecimal.ZERO;
        for (ReceivableExcelRow r : excelRows) {
          if (r.getContractRowSpan() > 0 && r.getContractAmount() != null)
            contractAmt = contractAmt.add(r.getContractAmount());
          if (r.getSettlementRowSpan() > 0 && r.getSettlementAmount() != null)
            settleAmt = settleAmt.add(r.getSettlementAmount());
          if (r.getInvoiceAmount()    != null) invAmt      = invAmt.add(r.getInvoiceAmount());
          if (r.getTaxAmount()        != null) taxAmt      = taxAmt.add(r.getTaxAmount());
          if (r.getTotalAmount()      != null) totalAmt    = totalAmt.add(r.getTotalAmount());
          if (r.getReceivableAmount() != null) receivable  = receivable.add(r.getReceivableAmount());
          if (r.getReceivedAmount()   != null) received    = received.add(r.getReceivedAmount());
          if (r.getOutstandingAmount()!= null) outstanding = outstanding.add(r.getOutstandingAmount());
        }
        setAmountCell(summaryRow, COL_CONTRACT_AMT, contractAmt, summaryStyle);
        setAmountCell(summaryRow, COL_SETTLE_AMT,   settleAmt,   summaryStyle);
        setAmountCell(summaryRow, COL_INV_AMT,      invAmt,      summaryStyle);
        setAmountCell(summaryRow, COL_TAX_AMT,      taxAmt,      summaryStyle);
        setAmountCell(summaryRow, COL_TOTAL_AMT,    totalAmt,    summaryStyle);
        setAmountCell(summaryRow, COL_RECEIVABLE,   receivable,  summaryStyle);
        setAmountCell(summaryRow, COL_RECEIVED,     received,    summaryStyle);
        setAmountCell(summaryRow, COL_OUTSTANDING,  outstanding, summaryStyle);
      }

      // 列宽自适应（最小 3000，防止中文列过窄）
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
    if (value == null) return;
    Cell cell = row.createCell(col);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private void setAmountCell(Row row, int col, BigDecimal value, CellStyle style) {
    if (value == null) return;
    Cell cell = row.createCell(col);
    cell.setCellValue(value.doubleValue());
    cell.setCellStyle(style);
  }

  private void setDateCell(Row row, int col, LocalDate date, CellStyle style) {
    if (date == null) return;
    Cell cell = row.createCell(col);
    cell.setCellValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    cell.setCellStyle(style);
  }

  private void setIntCell(Row row, int col, Integer value, CellStyle style) {
    if (value == null) return;
    Cell cell = row.createCell(col);
    cell.setCellValue(value);
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

  private CellStyle createBgStyle(XSSFWorkbook wb, byte[] rgb) {
    CellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(rgb, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    setBorder(style);
    return style;
  }

  private CellStyle createAmountStyle(XSSFWorkbook wb) {
    CellStyle style = wb.createCellStyle();
    DataFormat fmt = wb.createDataFormat();
    style.setDataFormat(fmt.getFormat("#,##0.00"));
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    setBorder(style);
    return style;
  }

  private CellStyle createDateStyle(XSSFWorkbook wb) {
    CellStyle style = wb.createCellStyle();
    DataFormat fmt = wb.createDataFormat();
    style.setDataFormat(fmt.getFormat("yyyy-mm-dd"));
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    setBorder(style);
    return style;
  }

  private CellStyle createSummaryStyle(XSSFWorkbook wb) {
    CellStyle style = wb.createCellStyle();
    XSSFFont font = wb.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xD9,(byte)0xD9,(byte)0xD9}, null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    DataFormat fmt = wb.createDataFormat();
    style.setDataFormat(fmt.getFormat("#,##0.00"));
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

