package com.erp.util;

import cn.hutool.core.util.StrUtil;
import com.erp.controller.finance.dto.FundDiaryResponse;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfCopy;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

/**
 * 日记账PDF生成工具类
 *
 * @author ERP System
 */
@Slf4j
public class FundDiaryPdfGenerator {

    private static BaseFont baseFont;
    private static Font TITLE_FONT;
    private static Font SUBTITLE_FONT;
    private static Font HEADER_FONT;
    private static Font NORMAL_FONT;
    private static Font BOLD_FONT;

    static {
        try {
            // 优先尝试使用Windows系统字体（SimSun）
            String os = System.getProperty("os.name").toLowerCase();
            String fontPath = null;
            
            if (os.contains("windows")) {
                // Windows系统字体路径
                fontPath = "C:/Windows/Fonts/simsun.ttc,1"; // SimSun字体
            } else if (os.contains("linux")) {
                // Linux系统字体路径
                fontPath = "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"; // 文泉驿字体
            }
            
            if (fontPath != null) {
                try {
                    baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                    log.info("成功加载系统字体：{}", fontPath);
                } catch (Exception e) {
                    log.warn("加载系统字体失败：{}，尝试使用iText内置字体", fontPath, e);
                    // 尝试使用iText内置的中文字体
                    baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
                }
            } else {
                // 使用iText内置的中文字体
                baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            }
            
            TITLE_FONT = new Font(baseFont, 22, Font.BOLD);
            SUBTITLE_FONT = new Font(baseFont, 10, Font.NORMAL);
            HEADER_FONT = new Font(baseFont, 12, Font.BOLD);
            NORMAL_FONT = new Font(baseFont, 10, Font.NORMAL);
            BOLD_FONT = new Font(baseFont, 10, Font.BOLD);
            
        } catch (Exception e) {
            log.error("初始化PDF字体失败，使用默认字体（可能不支持中文）", e);
            try {
                // 最后尝试使用默认字体（可能不支持中文）
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                TITLE_FONT = new Font(baseFont, 22, Font.BOLD);
                SUBTITLE_FONT = new Font(baseFont, 10, Font.NORMAL);
                HEADER_FONT = new Font(baseFont, 12, Font.BOLD);
                NORMAL_FONT = new Font(baseFont, 10, Font.NORMAL);
                BOLD_FONT = new Font(baseFont, 10, Font.BOLD);
                log.warn("使用默认字体，中文可能显示为乱码");
            } catch (Exception e2) {
                log.error("初始化默认字体也失败", e2);
                throw new RuntimeException("PDF字体初始化失败", e2);
            }
        }
    }

    /**
     * 生成日记账PDF文件
     *
     * @param diaryResponse 日记账响应数据
     * @param filePath      文件保存路径
     * @throws DocumentException PDF生成异常
     * @throws IOException       IO异常
     */
    public static void generatePdf(FundDiaryResponse diaryResponse, String filePath)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30); // 横向A4
        PdfWriter.getInstance(document, new FileOutputStream(filePath));

        document.open();

        try {
            // 添加标题
            addTitle(document, "资金日记账");

            // 添加账户和账期信息
            addAccountAndPeriodInfo(document, diaryResponse);

            // 添加表格
            addDiaryTable(document, diaryResponse);

        } finally {
            document.close();
        }
    }

    /**
     * 添加标题
     */
    private static void addTitle(Document document, String title) throws DocumentException {
        Paragraph titlePara = new Paragraph(title, TITLE_FONT);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(15);
        document.add(titlePara);
    }

    /**
     * 添加账户和账期信息
     */
    private static void addAccountAndPeriodInfo(Document document, FundDiaryResponse diaryResponse) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        try {
            infoTable.setWidths(new float[]{1.0f, 2.0f, 1.0f, 2.0f});
        } catch (Exception ignored) {}
        infoTable.setSpacingAfter(10);

        addTableRow(infoTable, "账户名称", StrUtil.blankToDefault(diaryResponse.getAccountName(), "—"));
        addTableRow(infoTable, "账户编码", StrUtil.blankToDefault(diaryResponse.getAccountCode(), "—"));
        addTableRow(infoTable, "账期", diaryResponse.getYear() + "年第" + diaryResponse.getMonth() + "期");
        addTableRow(infoTable, "账期编码", StrUtil.blankToDefault(diaryResponse.getPeriodCode(), "—"));

        document.add(infoTable);
    }

    /**
     * 添加日记账表格
     */
    private static void addDiaryTable(Document document, FundDiaryResponse diaryResponse) throws DocumentException {
        // 创建表格：日期、流水编码、摘要、收入、支出、余额方向、余额
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{1.0f, 1.2f, 2.0f, 1.2f, 1.2f, 1.0f, 1.2f});
        } catch (Exception ignored) {}

        // 表头
        addTableHeader(table, "日期");
        addTableHeader(table, "流水编码");
        addTableHeader(table, "摘要");
        addTableHeader(table, "收入");
        addTableHeader(table, "支出");
        addTableHeader(table, "余额方向");
        addTableHeader(table, "余额");

        DecimalFormat df = new DecimalFormat("#,##0.00");

        // 期初余额行
        if (diaryResponse.getInitialBalance() != null) {
            addInitialBalanceRow(table, diaryResponse, df);
        }

        // 流水明细
        if (diaryResponse.getTransactions() != null && !diaryResponse.getTransactions().isEmpty()) {
            for (FundDiaryResponse.TransactionDetailInfo transaction : diaryResponse.getTransactions()) {
                addTransactionRow(table, transaction, df);
            }
        }

        // 本期合计行
        if (diaryResponse.getPeriodTotal() != null) {
            addPeriodTotalRow(table, diaryResponse, df);
        }

        // 本年累计行
        if (diaryResponse.getYearTotal() != null) {
            addYearTotalRow(table, diaryResponse, df);
        }

        document.add(table);
    }

    /**
     * 添加表头单元格
     */
    private static void addTableHeader(PdfPTable table, String header) {
        PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
    }

    /**
     * 添加期初余额行
     */
    private static void addInitialBalanceRow(PdfPTable table, FundDiaryResponse diaryResponse, DecimalFormat df) {
        FundDiaryResponse.InitialBalanceInfo initialBalance = diaryResponse.getInitialBalance();
        
        // 日期
        addTableCell(table, "", NORMAL_FONT, Element.ALIGN_CENTER, BaseColor.LIGHT_GRAY);
        // 流水编码
        addTableCell(table, "", NORMAL_FONT, Element.ALIGN_CENTER, BaseColor.LIGHT_GRAY);
        // 摘要
        addTableCell(table, "期初余额", BOLD_FONT, Element.ALIGN_LEFT, BaseColor.LIGHT_GRAY);
        // 收入
        addTableCell(table, "", NORMAL_FONT, Element.ALIGN_RIGHT, BaseColor.LIGHT_GRAY);
        // 支出
        addTableCell(table, "", NORMAL_FONT, Element.ALIGN_RIGHT, BaseColor.LIGHT_GRAY);
        // 余额方向
        addTableCell(table, initialBalance.getDirection(), NORMAL_FONT, Element.ALIGN_CENTER, BaseColor.LIGHT_GRAY);
        // 余额
        addTableCell(table, df.format(initialBalance.getAmount()), BOLD_FONT, Element.ALIGN_RIGHT, BaseColor.LIGHT_GRAY);
    }

    /**
     * 添加流水明细行
     */
    private static void addTransactionRow(PdfPTable table, FundDiaryResponse.TransactionDetailInfo transaction, DecimalFormat df) {
        // 日期
        addTableCell(table, transaction.getTransactionDate(), NORMAL_FONT, Element.ALIGN_CENTER, null);
        // 流水编码
        addTableCell(table, StrUtil.blankToDefault(transaction.getTransactionCode(), "—"), NORMAL_FONT, Element.ALIGN_CENTER, null);
        // 摘要
        addTableCell(table, StrUtil.blankToDefault(transaction.getSummary(), "—"), NORMAL_FONT, Element.ALIGN_LEFT, null);
        // 收入
        String income = transaction.getIncome() != null ? df.format(transaction.getIncome()) : "—";
        addTableCell(table, income, NORMAL_FONT, Element.ALIGN_RIGHT, null);
        // 支出
        String expenditure = transaction.getExpenditure() != null ? df.format(transaction.getExpenditure()) : "—";
        addTableCell(table, expenditure, NORMAL_FONT, Element.ALIGN_RIGHT, null);
        // 余额方向
        addTableCell(table, StrUtil.blankToDefault(transaction.getDirection(), "—"), NORMAL_FONT, Element.ALIGN_CENTER, null);
        // 余额
        addTableCell(table, df.format(transaction.getBalance()), NORMAL_FONT, Element.ALIGN_RIGHT, null);
    }

    /**
     * 添加本期合计行
     */
    private static void addPeriodTotalRow(PdfPTable table, FundDiaryResponse diaryResponse, DecimalFormat df) {
        FundDiaryResponse.PeriodTotalInfo periodTotal = diaryResponse.getPeriodTotal();
        
        // 日期
        addTableCell(table, "", NORMAL_FONT, Element.ALIGN_CENTER, BaseColor.YELLOW);
        // 流水编码
        addTableCell(table, "", NORMAL_FONT, Element.ALIGN_CENTER, BaseColor.YELLOW);
        // 摘要
        addTableCell(table, "本期合计", BOLD_FONT, Element.ALIGN_LEFT, BaseColor.YELLOW);
        // 收入
        String income = periodTotal.getIncome() != null ? df.format(periodTotal.getIncome()) : "—";
        addTableCell(table, income, BOLD_FONT, Element.ALIGN_RIGHT, BaseColor.YELLOW);
        // 支出
        String expenditure = periodTotal.getExpenditure() != null ? df.format(periodTotal.getExpenditure()) : "—";
        addTableCell(table, expenditure, BOLD_FONT, Element.ALIGN_RIGHT, BaseColor.YELLOW);
        // 余额方向
        addTableCell(table, periodTotal.getDirection(), NORMAL_FONT, Element.ALIGN_CENTER, BaseColor.YELLOW);
        // 余额
        addTableCell(table, df.format(periodTotal.getBalance()), BOLD_FONT, Element.ALIGN_RIGHT, BaseColor.YELLOW);
    }

    /**
     * 添加本年累计行
     */
    private static void addYearTotalRow(PdfPTable table, FundDiaryResponse diaryResponse, DecimalFormat df) {
        FundDiaryResponse.YearTotalInfo yearTotal = diaryResponse.getYearTotal();
        
        // 日期
        addTableCell(table, "", NORMAL_FONT, Element.ALIGN_CENTER, new BaseColor(252, 231, 243));
        // 流水编码
        addTableCell(table, "", NORMAL_FONT, Element.ALIGN_CENTER, new BaseColor(252, 231, 243));
        // 摘要
        addTableCell(table, "本年累计", BOLD_FONT, Element.ALIGN_LEFT, new BaseColor(252, 231, 243));
        // 收入
        String income = yearTotal.getIncome() != null ? df.format(yearTotal.getIncome()) : "—";
        addTableCell(table, income, BOLD_FONT, Element.ALIGN_RIGHT, new BaseColor(252, 231, 243));
        // 支出
        String expenditure = yearTotal.getExpenditure() != null ? df.format(yearTotal.getExpenditure()) : "—";
        addTableCell(table, expenditure, BOLD_FONT, Element.ALIGN_RIGHT, new BaseColor(252, 231, 243));
        // 余额方向
        addTableCell(table, yearTotal.getDirection(), NORMAL_FONT, Element.ALIGN_CENTER, new BaseColor(252, 231, 243));
        // 余额
        addTableCell(table, df.format(yearTotal.getBalance()), BOLD_FONT, Element.ALIGN_RIGHT, new BaseColor(252, 231, 243));
    }

    /**
     * 添加表格单元格
     */
    private static void addTableCell(PdfPTable table, String text, Font font, int alignment, BaseColor backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(StrUtil.blankToDefault(text, "—"), font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        if (backgroundColor != null) {
            cell.setBackgroundColor(backgroundColor);
        }
        table.addCell(cell);
    }

    /**
     * 添加表格行（用于信息表格）
     */
    private static void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setPadding(5);
        labelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(StrUtil.blankToDefault(value, "—"), NORMAL_FONT));
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    /**
     * 批量生成日记账PDF并合并为一个文件
     *
     * @param diaryResponses 日记账响应数据列表
     * @param filePath        合并后的PDF文件保存路径
     * @throws DocumentException PDF生成异常
     * @throws IOException       IO异常
     */
    public static void generateBatchPdf(List<FundDiaryResponse> diaryResponses, String filePath)
            throws DocumentException, IOException {
        if (diaryResponses == null || diaryResponses.isEmpty()) {
            throw new IllegalArgumentException("日记账列表不能为空");
        }

        // 如果只有一个，直接生成
        if (diaryResponses.size() == 1) {
            generatePdf(diaryResponses.get(0), filePath);
            return;
        }

        // 多个日记账，先分别生成临时PDF，然后合并
        List<String> tempFiles = new java.util.ArrayList<>();
        try {
            // 为每个日记账生成临时PDF
            for (int i = 0; i < diaryResponses.size(); i++) {
                FundDiaryResponse diaryResponse = diaryResponses.get(i);
                String tempFilePath = filePath + ".temp." + i + ".pdf";
                generatePdf(diaryResponse, tempFilePath);
                tempFiles.add(tempFilePath);
            }

            // 合并所有临时PDF
            mergePdfFiles(tempFiles, filePath);
        } finally {
            // 清理临时文件
            for (String tempFile : tempFiles) {
                try {
                    java.io.File file = new java.io.File(tempFile);
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception e) {
                    log.warn("删除临时PDF文件失败：{}", tempFile, e);
                }
            }
        }
    }

    /**
     * 合并多个PDF文件为一个PDF
     *
     * @param inputFiles 输入PDF文件路径列表
     * @param outputFile 输出PDF文件路径
     * @throws DocumentException PDF操作异常
     * @throws IOException       IO异常
     */
    private static void mergePdfFiles(List<String> inputFiles, String outputFile)
            throws DocumentException, IOException {
        Document document = new Document();
        PdfCopy copy = new PdfCopy(document, new FileOutputStream(outputFile));
        document.open();

        try {
            for (String inputFile : inputFiles) {
                PdfReader reader = new PdfReader(inputFile);
                int numberOfPages = reader.getNumberOfPages();

                for (int page = 1; page <= numberOfPages; page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }

                reader.close();
            }
        } finally {
            document.close();
        }
    }
}

