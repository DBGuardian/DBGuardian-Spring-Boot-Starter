package com.erp.util;

import cn.hutool.core.util.StrUtil;
import com.erp.controller.transport.dto.TransportDispatchDetailResponse;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfCopy;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 运输单PDF生成工具类
 *
 * @author ERP System
 */
@Slf4j
public class DispatchOrderPdfGenerator {

    private static BaseFont baseFont;
    private static Font TITLE_FONT;
    private static Font SUBTITLE_FONT;
    private static Font HEADER_FONT;
    private static Font NORMAL_FONT;

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
            
        } catch (Exception e) {
            log.error("初始化PDF字体失败，使用默认字体（可能不支持中文）", e);
            try {
                // 最后尝试使用默认字体（可能不支持中文）
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            TITLE_FONT = new Font(baseFont, 22, Font.BOLD);
            SUBTITLE_FONT = new Font(baseFont, 10, Font.NORMAL);
            HEADER_FONT = new Font(baseFont, 12, Font.BOLD);
            NORMAL_FONT = new Font(baseFont, 10, Font.NORMAL);
                log.warn("使用默认字体，中文可能显示为乱码");
            } catch (Exception e2) {
                log.error("初始化默认字体也失败", e2);
                throw new RuntimeException("PDF字体初始化失败", e2);
            }
        }
    }

    /**
     * 生成运输单PDF文件
     *
     * @param dispatchDetail 运输单详情
     * @param filePath        文件保存路径
     * @throws DocumentException PDF生成异常
     * @throws IOException       IO异常
     */
    public static void generatePdf(TransportDispatchDetailResponse dispatchDetail, String filePath)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));

        document.open();

        try {
            // 添加标题（大标题居中）
            addTitle(document, "危险废物转移联单");

            // 顶部区域：左侧基本信息，右侧二维码（如果有）
            addTopSectionWithQrcode(document, dispatchDetail);

            // 添加运输信息表格
            addTransportInfoTable(document, dispatchDetail);

            // 添加危废明细表格
            addWasteItemsTable(document, dispatchDetail);

            // 添加备注信息
            addRemark(document, dispatchDetail);

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
        titlePara.setSpacingAfter(20);
        document.add(titlePara);
    }

    /**
     * 添加运输单号
     */
    private static void addDispatchCode(Document document, String dispatchCode) throws DocumentException {
        if (StrUtil.isNotBlank(dispatchCode)) {
            Paragraph codePara = new Paragraph("运输单号：" + dispatchCode, NORMAL_FONT);
            codePara.setAlignment(Element.ALIGN_RIGHT);
            codePara.setSpacingAfter(15);
            document.add(codePara);
        }
    }

    /**
     * 顶部区域：左侧基本信息，右侧二维码（如果有）
     */
    private static void addTopSectionWithQrcode(Document document, TransportDispatchDetailResponse detail) throws DocumentException {
        PdfPTable topTable = new PdfPTable(2);
        topTable.setWidthPercentage(100);
        topTable.setWidths(new float[]{3.0f, 1.0f});
        topTable.setSpacingAfter(10);

        // 左侧：基本信息表格（无表头，用作嵌入）
        PdfPTable basicTable = buildBasicInfoInnerTable(detail);
        PdfPCell leftCell = new PdfPCell(basicTable);
        leftCell.setPadding(0);
        leftCell.setBorderWidth(1);
        topTable.addCell(leftCell);

        // 右侧：二维码或空白占位
        PdfPCell rightCell = new PdfPCell();
        rightCell.setPadding(6);
        rightCell.setBorderWidth(1);
        String qrcodeFilePath = detail.getQrcodeFilePath();
        if (StrUtil.isNotBlank(qrcodeFilePath)) {
            try {
                java.io.File qrcodeFile = new java.io.File(qrcodeFilePath);
                if (qrcodeFile.exists() && qrcodeFile.isFile()) {
                    Image qrcodeImage = Image.getInstance(qrcodeFilePath);
                    float imageSize = 120f;
                    qrcodeImage.scaleAbsolute(imageSize, imageSize);
                    qrcodeImage.setAlignment(Image.ALIGN_CENTER);
                    // 将图片放入单元格
                    rightCell.addElement(qrcodeImage);
                }
            } catch (Exception e) {
                log.warn("加载二维码失败：{}", qrcodeFilePath, e);
                // 如果加载失败，保留空白占位
            }
        }
        topTable.addCell(rightCell);

        // 如果有运输单号，显示在右下角小字（使用 subtitle）
        if (StrUtil.isNotBlank(detail.getDispatchCode())) {
            Paragraph codePara = new Paragraph("运输单号：" + detail.getDispatchCode(), SUBTITLE_FONT);
            codePara.setAlignment(Element.ALIGN_RIGHT);
            PdfPCell codeHolder = new PdfPCell();
            codeHolder.setBorder(Rectangle.NO_BORDER);
            codeHolder.addElement(codePara);
            // 将 codeHolder 放入左侧下方（append as separate row）
            PdfPTable wrapper = new PdfPTable(1);
            wrapper.setWidthPercentage(100);
            wrapper.addCell(new PdfPCell(topTable));
            wrapper.addCell(codeHolder);
            document.add(wrapper);
            return;
        }

        document.add(topTable);
    }

    /**
     * 构建不带标题的基本信息内部表格，便于在顶栏嵌入
     */
    private static PdfPTable buildBasicInfoInnerTable(TransportDispatchDetailResponse detail) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{1.0f, 2.0f});
        } catch (Exception ignored) {}
        table.setSpacingBefore(0);
        table.setSpacingAfter(0);

        // 使用加粗表头行作为左上角小标题
        PdfPCell headerCell = new PdfPCell(new Phrase("单位/承运信息", HEADER_FONT));
        headerCell.setColspan(2);
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setPadding(6);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        addTableRow(table, "收运通知单号", detail.getNoticeCode());
        addTableRow(table, "合同号", StrUtil.blankToDefault(detail.getContractCode(), "未关联"));
        addTableRow(table, "承运单位", StrUtil.blankToDefault(detail.getCarrierName(), "—"));
        addTableRow(table, "营运证件号", StrUtil.blankToDefault(detail.getOperationLicenseNo(), "—"));
        addTableRow(table, "承运单位电话", StrUtil.blankToDefault(detail.getCarrierPhone(), "—"));
        addTableRow(table, "驾驶员/电话", StrUtil.blankToDefault(detail.getDriverName(), "—")
                + " / " + StrUtil.blankToDefault(detail.getDriverPhone(), "—"));
        addTableRow(table, "车辆号牌", StrUtil.blankToDefault(detail.getPlateNo(), "—"));

        return table;
    }

    /**
     * 添加基本信息表格
     */
    private static void addBasicInfoTable(Document document, TransportDispatchDetailResponse detail) throws DocumentException {
        // 直接复用内部构建方法，保留垂直间距
        PdfPTable table = buildBasicInfoInnerTable(detail);
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);
        document.add(table);
    }

    /**
     * 添加运输信息表格
     */
    private static void addTransportInfoTable(Document document, TransportDispatchDetailResponse detail) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.0f, 2.0f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);

        // 表头
        PdfPCell headerCell = new PdfPCell(new Phrase("运输信息", HEADER_FONT));
        headerCell.setColspan(2);
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setPadding(8);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        // 运输起点
        addTableRow(table, "运输起点", StrUtil.blankToDefault(detail.getStartPoint(), "—"));
        
        // 运输终点
        addTableRow(table, "运输终点", StrUtil.blankToDefault(detail.getEndPoint(), "—"));
        
        // 派车时间
        addTableRow(table, "派车时间", StrUtil.blankToDefault(detail.getDispatchAt(), "—"));
        
        // 实际起运时间
        addTableRow(table, "实际起运时间", StrUtil.blankToDefault(detail.getDepartAt(), "—"));
        
        // 实际到达时间
        addTableRow(table, "实际到达时间", StrUtil.blankToDefault(detail.getArriveAt(), "—"));
        
        // 计划转移数量
        String planQty = detail.getPlanQuantityTon() != null 
            ? String.format("%.2f 吨", detail.getPlanQuantityTon()) 
            : "—";
        addTableRow(table, "计划转移数量", planQty);
        
        // 状态
        addTableRow(table, "状态", StrUtil.blankToDefault(detail.getStatus(), "—"));

        document.add(table);
    }

    /**
     * 添加表格行
     */
    private static void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(BaseColor.WHITE);
        labelCell.setBorderWidth(1);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(StrUtil.blankToDefault(value, "—"), NORMAL_FONT));
        valueCell.setPadding(6);
        valueCell.setBorderWidth(1);
        table.addCell(valueCell);
    }

    /**
     * 添加危废明细表格
     * 参考合同单和报价单PDF生成器的成熟实现
     */
    private static void addWasteItemsTable(Document document, TransportDispatchDetailResponse detail) throws DocumentException {
        List<TransportDispatchDetailResponse.WasteItemDetail> wasteItems = detail.getWasteItems();
        if (wasteItems == null || wasteItems.isEmpty()) {
            return;
        }

        Paragraph wasteTitle = new Paragraph("危废明细", HEADER_FONT);
        wasteTitle.setSpacingBefore(10);
        wasteTitle.setSpacingAfter(10);
        document.add(wasteTitle);

        // 创建表格，8列：序号、废物类别、废物代码、废物名称、废物形态、包装/辅助计量、计划转移数量(吨)、是否启用辅助核算
        // 参考合同单和报价单PDF生成器的表格结构，去掉金额和付款方字段
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        // 优化列宽比例，使表格更美观，参考合同单PDF生成器
        table.setWidths(new float[]{0.6f, 1.0f, 1.3f, 2.2f, 0.9f, 1.4f, 1.4f, 1.0f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);
        table.setKeepTogether(true); // 保持表格完整性

        // 表头 - 参考合同单PDF生成器的样式
        addTableHeader(table);

        // 数据行 - 使用addCell方法，参考合同单PDF生成器的实现
        int index = 1;
        for (TransportDispatchDetailResponse.WasteItemDetail item : wasteItems) {
            // 序号
            addCell(table, String.valueOf(index++), Element.ALIGN_CENTER);

            // 废物类别 - 运输单没有废物类别，使用形态作为类别或显示"-"
            String wasteCategory = item.getForm() != null ? item.getForm() : "-";
            addCell(table, wasteCategory, Element.ALIGN_LEFT);

            // 废物代码
            addCell(table, item.getWasteCode(), Element.ALIGN_LEFT);

            // 废物名称
            addCell(table, item.getWasteName(), Element.ALIGN_LEFT);

            // 废物形态
            addCell(table, item.getForm(), Element.ALIGN_CENTER);

            // 包装/辅助计量（优先显示辅助核算信息，参考合同单PDF生成器逻辑）
            String packageInfo = getPackageDisplayInfo(item);
            addCell(table, packageInfo, Element.ALIGN_LEFT);

            // 计划转移数量(吨)
            String plannedQtyStr = "-";
            if (item.getPlannedQtyTon() != null) {
                plannedQtyStr = String.format("%.2f", item.getPlannedQtyTon());
            }
            addCell(table, plannedQtyStr, Element.ALIGN_RIGHT);

            // 是否启用辅助核算
            String enableAuxStr = "-";
            if (item.getEnableAuxiliaryAccounting() != null) {
                enableAuxStr = item.getEnableAuxiliaryAccounting() ? "是" : "否";
            }
            addCell(table, enableAuxStr, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    /**
     * 添加表头
     * 参考合同单PDF生成器的表头样式
     */
    private static void addTableHeader(PdfPTable table) throws DocumentException {
        String[] headers = {"序号", "废物类别", "废物代码", "废物名称", "废物形态", "包装/辅助计量", "计划转移数量(吨)", "启用辅助核算"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(8);
            cell.setBackgroundColor(new BaseColor(240, 240, 240)); // 浅灰色背景
            cell.setBorderWidth(0.5f); // 边框宽度
            table.addCell(cell);
        }
    }

    /**
     * 获取包装/辅助计量信息的显示字符串
     * 参考合同单PDF生成器的逻辑，优先显示辅助核算信息
     */
    private static String getPackageDisplayInfo(TransportDispatchDetailResponse.WasteItemDetail item) {
        if (item == null) {
            return "-";
        }

        // 优先显示辅助核算信息（如果启用且有辅助数量）
        if (item.getEnableAuxiliaryAccounting() != null && item.getEnableAuxiliaryAccounting()) {
            if (item.getAuxQuantity() != null && item.getAuxUnit() != null) {
                return item.getAuxQuantity().toString() + " " + item.getAuxUnit();
            } else if (item.getAuxUnit() != null) {
                return item.getAuxUnit();
            }
        }

        // 回退到包装信息
        if (item.getPackageQty() != null && item.getPackageType() != null) {
            return item.getPackageQty().toString() + " " + item.getPackageType();
        } else if (item.getPackageType() != null) {
            return item.getPackageType();
        }

        return "-";
    }

    /**
     * 创建表格单元格
     * 参考合同单PDF生成器的实现
     */
    private static PdfPCell createTableCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(StrUtil.blankToDefault(content, "-"), NORMAL_FONT));
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    /**
     * 添加单元格（指定对齐方式）
     * 参考合同单PDF生成器的addCell方法
     */
    private static void addCell(PdfPTable table, String text, int alignment) throws DocumentException {
        addCell(table, text, alignment, NORMAL_FONT);
    }

    /**
     * 添加单元格（指定字体，支持多行文本）
     * 参考合同单PDF生成器的addCell方法
     */
    private static void addCell(PdfPTable table, String text, int alignment, Font font) throws DocumentException {
        PdfPCell cell;
        if (text != null && text.contains("\n")) {
            // 包含换行符，使用 Paragraph 支持多行显示
            Paragraph paragraph = new Paragraph();
            String[] lines = text.split("\n", -1); // 使用 -1 保留空行
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    paragraph.add(new com.itextpdf.text.Chunk(com.itextpdf.text.Chunk.NEWLINE));
                }
                paragraph.add(new Phrase(lines[i], font));
            }
            cell = new PdfPCell(paragraph);
        } else {
            cell = new PdfPCell(new Phrase(StrUtil.blankToDefault(text, "-"), font));
        }

        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    /**
     * 格式化危险特性显示
     */
    private static String formatHazardFeature(String hazardFeature) {
        if (StrUtil.isBlank(hazardFeature)) {
            return "";
        }
        // 危险特性代码映射
        java.util.Map<String, String> hazardFeatureLabelMap = new java.util.HashMap<>();
        hazardFeatureLabelMap.put("T", "毒性");
        hazardFeatureLabelMap.put("C", "腐蚀性");
        hazardFeatureLabelMap.put("I", "易燃性");
        hazardFeatureLabelMap.put("R", "反应性");
        hazardFeatureLabelMap.put("In", "感染性");

        String[] codes = hazardFeature.split("[/，\\s]+");
        java.util.List<String> labels = new java.util.ArrayList<>();
        for (String code : codes) {
            String trimmedCode = code.trim();
            if (StrUtil.isNotBlank(trimmedCode)) {
                String label = hazardFeatureLabelMap.get(trimmedCode);
                labels.add(label != null ? label : trimmedCode);
            }
        }
        return String.join("、", labels);
    }

    /**
     * 添加常用单位二维码
     */
    private static void addQrcodeImage(Document document, TransportDispatchDetailResponse detail) throws DocumentException {
        String qrcodeFilePath = detail.getQrcodeFilePath();
        if (StrUtil.isBlank(qrcodeFilePath)) {
            return;
        }

        try {
            java.io.File qrcodeFile = new java.io.File(qrcodeFilePath);
            if (!qrcodeFile.exists() || !qrcodeFile.isFile()) {
                log.warn("二维码文件不存在：{}", qrcodeFilePath);
                return;
            }

            // 创建图片
            Image qrcodeImage = Image.getInstance(qrcodeFilePath);
            
            // 设置图片大小（例如：80x80像素，可以根据需要调整）
            float imageSize = 80f;
            qrcodeImage.scaleAbsolute(imageSize, imageSize);
            
            // 设置图片对齐方式（右对齐）
            qrcodeImage.setAlignment(Image.ALIGN_RIGHT);

            // 添加标题
            Paragraph qrcodeTitle = new Paragraph("常用单位二维码", HEADER_FONT);
            qrcodeTitle.setSpacingBefore(10);
            qrcodeTitle.setSpacingAfter(5);
            document.add(qrcodeTitle);

            // 添加图片
            document.add(qrcodeImage);
            
            // 添加间距
            Paragraph spacing = new Paragraph();
            spacing.setSpacingAfter(10);
            document.add(spacing);

        } catch (Exception e) {
            log.error("添加二维码图片失败：{}", qrcodeFilePath, e);
            // 二维码添加失败不影响PDF生成，继续执行
        }
    }

    /**
     * 添加备注信息
     */
    private static void addRemark(Document document, TransportDispatchDetailResponse detail) throws DocumentException {
        if (StrUtil.isNotBlank(detail.getDispatcherRemark())) {
            Paragraph remarkTitle = new Paragraph("调度备注：", HEADER_FONT);
            remarkTitle.setSpacingBefore(10);
            document.add(remarkTitle);

            Paragraph remarkContent = new Paragraph(detail.getDispatcherRemark(), NORMAL_FONT);
            remarkContent.setSpacingAfter(10);
            remarkContent.setFirstLineIndent(20);
            document.add(remarkContent);
        }
    }

    /**
     * 批量生成运输单PDF并合并为一个文件
     *
     * @param dispatchDetails 运输单详情列表
     * @param filePath        合并后的PDF文件保存路径
     * @throws DocumentException PDF生成异常
     * @throws IOException       IO异常
     */
    public static void generateBatchPdf(List<TransportDispatchDetailResponse> dispatchDetails, String filePath)
            throws DocumentException, IOException {
        if (dispatchDetails == null || dispatchDetails.isEmpty()) {
            throw new IllegalArgumentException("运输单列表不能为空");
        }

        // 如果只有一个，直接生成
        if (dispatchDetails.size() == 1) {
            generatePdf(dispatchDetails.get(0), filePath);
            return;
        }

        // 多个运输单，先分别生成临时PDF，然后合并
        List<String> tempFiles = new java.util.ArrayList<>();
        try {
            // 为每个运输单生成临时PDF
            for (int i = 0; i < dispatchDetails.size(); i++) {
                TransportDispatchDetailResponse detail = dispatchDetails.get(i);
                String tempFilePath = filePath + ".temp." + i + ".pdf";
                generatePdf(detail, tempFilePath);
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




