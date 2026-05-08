package com.erp.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.erp.controller.contract.dto.ContractDetailResponse;
import com.erp.controller.contract.dto.QuotationDetailResponse;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合同PDF生成工具类
 *
 * @author ERP System
 */
@Slf4j
public class ContractPdfGenerator {

    private static BaseFont baseFont;
    private static Font TITLE_FONT;
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
            
            TITLE_FONT = new Font(baseFont, 20, Font.BOLD);
            HEADER_FONT = new Font(baseFont, 12, Font.BOLD);
            NORMAL_FONT = new Font(baseFont, 10, Font.NORMAL);
            
        } catch (Exception e) {
            log.error("初始化PDF字体失败，使用默认字体（可能不支持中文）", e);
            try {
                // 最后尝试使用默认字体（可能不支持中文）
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                TITLE_FONT = new Font(baseFont, 20, Font.BOLD);
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
     * 生成合同PDF文件
     *
     * @param contractDetail 合同详情
     * @param filePath        文件保存路径
     * @throws DocumentException PDF生成异常
     * @throws IOException       IO异常
     */
    public static void generatePdf(ContractDetailResponse contractDetail, String filePath)
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));

        document.open();

        try {
            // 添加标题
            addTitle(document, "危险废物处置服务报价单");

            // 添加合同号
            addContractNo(document, contractDetail.getContractNo());

            // 添加表格
            addTable(document, contractDetail);

            // 添加备注（自动检查并添加服务期限信息）
            addRemark(document, contractDetail);

            // 添加签名区域
            addSignatureArea(document, contractDetail);

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
     * 添加合同号
     */
    private static void addContractNo(Document document, String contractNo) throws DocumentException {
        if (contractNo != null && !contractNo.trim().isEmpty()) {
            Paragraph contractNoPara = new Paragraph("合同号：" + contractNo, NORMAL_FONT);
            contractNoPara.setAlignment(Element.ALIGN_RIGHT);
            contractNoPara.setSpacingAfter(15);
            document.add(contractNoPara);
        }
    }

    /**
     * 添加表格
     */
    private static void addTable(Document document, ContractDetailResponse contractDetail) throws DocumentException {
        // 序号、废物类别、废物代码、废物名称、废物形态、计划转移数量、金额、付款方
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 2f, 2f, 2.0f, 2f, 3f, 2f, 2f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);
        table.setKeepTogether(true); // 保持表格完整性

        // 表头
        addTableHeader(table);

        // 表格内容
        int serialNo = 1;

        // 总价包干模式下，构建组合后的金额和付款方字符串
        ArrayList<String> combinedAmountStr = new ArrayList<String>();
        ArrayList<String> combinedPayerStr = new ArrayList<String>();
        
        // 存储所有行的数据，用于后续合并单元格
        List<TableRowData> rowDataList = new ArrayList<>();
        
        // 收集所有小计JSON数据，用于计算合计
        List<String> allSubtotalSummaryList = new ArrayList<>();

        if (contractDetail.getQuotationItems() != null) {
            for (ContractDetailResponse.ContractItemResponse item : contractDetail.getQuotationItems()) {
                if (item.getWasteItems() != null && !item.getWasteItems().isEmpty()) {
                    // 判断是否为总价包干模式
                    boolean isPackageMode = "总价包干".equals(item.getQuotationMode()) || "PACKAGE".equals(item.getQuotationMode());
                    
                    // 当前条目的组合字符串（用于传递给 addTableRow）
                    String currentAmountStr = null;
                    String currentPayerStr = null;
                    
                    if (isPackageMode) {
                        // 构建金额字符串：contractItemId | pricingPlan（使用 | 作为分隔符）
                        StringBuilder amountBuilder = new StringBuilder();
                        if (item.getContractItemId() != null) {
                            amountBuilder.append(item.getContractItemId());
                        }
                        if (item.getPricingPlan() != null && !item.getPricingPlan().trim().isEmpty()) {
                            if (amountBuilder.length() > 0) {
                                amountBuilder.append("|");
                            }
                            amountBuilder.append(item.getPricingPlan());
                        }
                        if (amountBuilder.length() > 0) {
                            currentAmountStr = amountBuilder.toString();
                            // 添加到 ArrayList 中保存
                            combinedAmountStr.add(currentAmountStr);
                        }
                        
                        // 构建付款方字符串：contractItemId | payer（使用 | 作为分隔符）
                        StringBuilder payerBuilder = new StringBuilder();
                        if (item.getContractItemId() != null) {
                            payerBuilder.append(item.getContractItemId());
                        }
                        if (item.getPayer() != null && !item.getPayer().trim().isEmpty()) {
                            if (payerBuilder.length() > 0) {
                                payerBuilder.append("|");
                            }
                            payerBuilder.append(item.getPayer());
                        }
                        if (payerBuilder.length() > 0) {
                            currentPayerStr = payerBuilder.toString();
                            // 添加到 ArrayList 中保存
                            combinedPayerStr.add(currentPayerStr);
                        }
                    }
                    
                    // 每个危废条目一行，但总价包干模式下金额和付款方从合同条目读取
                    for (ContractDetailResponse.ContractWasteItemResponse wasteItem : item.getWasteItems()) {
                        // 保存行数据
                        TableRowData rowData = new TableRowData();
                        rowData.setSerialNo(serialNo++);
                        rowData.setWasteItem(wasteItem);
                        rowData.setQuotationMode(item.getQuotationMode());
                        // 不再保存 contractItem 到 rowData（未在后续处理使用）
                        rowData.setAmountStr(currentAmountStr);
                        rowData.setPayerStr(currentPayerStr);
                        rowData.setSubtotalRow(false);
                        rowDataList.add(rowData);
                    }
                    
                    // 添加小计摘要行标记（如果有），并收集小计JSON数据
                    if (StrUtil.isNotBlank(item.getSubtotalSummary())) {
                        TableRowData subtotalRowData = new TableRowData();
                        subtotalRowData.setSubtotalRow(true);
                        subtotalRowData.setSubtotalSummary(item.getSubtotalSummary());
                        rowDataList.add(subtotalRowData);
                        // 收集小计JSON数据
                        allSubtotalSummaryList.add(item.getSubtotalSummary());
                    }
                }
            }
        }
        
        // 添加所有行，并处理单元格合并
        addTableRowsWithMerge(table, rowDataList, combinedAmountStr, combinedPayerStr);

        // 添加合计行（根据小计JSON数据计算）
        addTotalRow(table, allSubtotalSummaryList);

        document.add(table);

        // 如果存在价外服务，追加单独表格显示（与报价单保持一致）
        addOutOfScopeServicesTable(document, contractDetail);

    }

    /**
     * 在合同PDF中添加价外服务表格
     */
    private static void addOutOfScopeServicesTable(Document document, ContractDetailResponse contractDetail) throws DocumentException {
        if (contractDetail == null || contractDetail.getOutOfScopeServices() == null || contractDetail.getOutOfScopeServices().isEmpty()) {
            return;
        }

        PdfPTable outTable = new PdfPTable(6);
        outTable.setWidthPercentage(100);
        outTable.setSpacingBefore(6);
        outTable.setSpacingAfter(10);
        outTable.setWidths(new float[]{1f, 2.5f, 2.5f, 1f, 2f, 2f});

        // 表头（移除了“状态”列）
        String[] headers = {"序号", "项目", "规格型号", "单位", "计划数量", "合同单价"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            cell.setBackgroundColor(new BaseColor(245, 245, 245));
            cell.setBorderWidth(0.5f);
            outTable.addCell(cell);
        }

        int idx = 1;
        for (QuotationDetailResponse.OutOfScopeServiceResponse svc : contractDetail.getOutOfScopeServices()) {
            addCell(outTable, String.valueOf(idx++), Element.ALIGN_CENTER);
            addCell(outTable, svc.getProject() != null ? svc.getProject() : "-", Element.ALIGN_LEFT);
            addCell(outTable, svc.getSpec() != null ? svc.getSpec() : "-", Element.ALIGN_LEFT);
            addCell(outTable, svc.getUnit() != null ? svc.getUnit() : "-", Element.ALIGN_CENTER);

            String plannedQty = "-";
            if (svc.getPlannedQuantity() != null) {
                plannedQty = svc.getPlannedQuantity().stripTrailingZeros().toPlainString();
            }
            addCell(outTable, plannedQty, Element.ALIGN_RIGHT);

            String contractPrice = "-";
            if (svc.getContractUnitPrice() != null) {
                contractPrice = svc.getContractUnitPrice().stripTrailingZeros().toPlainString();
                if (StrUtil.isNotBlank(svc.getUnit())) {
                    contractPrice = contractPrice + "元/" + svc.getUnit();
                } else {
                    contractPrice = contractPrice + "元";
                }
            }
            addCell(outTable, contractPrice, Element.ALIGN_RIGHT);
        }

        document.add(outTable);
    }

    /**
     * 添加表头
     */
    private static void addTableHeader(PdfPTable table) throws DocumentException {
        String[] headers = {"序号", "废物类别", "废物代码", "废物名称", "废物形态", "计划转移数量", "金额", "付款方"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(8);
            cell.setBackgroundColor(new BaseColor(240, 240, 240)); // 浅灰色背景
            cell.setBorderWidth(0.5f); // 边框宽度
            cell.setNoWrap(true); // 防止表头换行
            table.addCell(cell);
        }
    }

    /**
     * 添加表格行
     * @param table PDF表格
     * @param serialNo 序号
     * @param wasteItem 危废条目
     * @param quotationMode 报价模式
     * @param contractItem 合同条目（总价包干模式时传入，用于读取计价方案和付款方）
     * @param combinedAmountStr 组合后的金额字符串（总价包干模式：contractItemId + pricingPlan）
     * @param combinedPayerStr 组合后的付款方字符串（总价包干模式：contractItemId + payer）
     */
    // NOTE: addTableRow removed - functionality consolidated into addTableRowsWithMerge.

    /**
     * 添加小计摘要行
     * 
     * @param table PDF表格
     * @param subtotalSummaryJson 小计摘要JSON字符串，格式：[{unit,total}]
     */
    private static void addSubtotalSummaryRows(PdfPTable table, String subtotalSummaryJson) throws DocumentException {
        if (StrUtil.isBlank(subtotalSummaryJson)) {
            return;
        }
        
        try {
            // 解析JSON数组
            JSONArray jsonArray = JSONUtil.parseArray(subtotalSummaryJson);
            if (jsonArray == null || jsonArray.isEmpty()) {
                return;
            }
            
            // 合并所有小计项为一行显示
            List<String> subtotalItems = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String unit = jsonObject.getStr("unit");
                BigDecimal total = jsonObject.getBigDecimal("total");

                String subtotalItemStr;
                if (total != null && total.compareTo(BigDecimal.valueOf(-1)) == 0) {
                    if (StrUtil.isNotBlank(unit)) {
                        subtotalItemStr = "不限量";
                    } else {
                        subtotalItemStr = "不限量";
                    }
                } else {
                    subtotalItemStr = total != null ? total.stripTrailingZeros().toPlainString() : "0";
                    if (StrUtil.isNotBlank(unit)) {
                        subtotalItemStr += " " + unit;
                    }
                }
                subtotalItems.add(subtotalItemStr);
            }
            
            // 合并前5列显示"小计"
            PdfPCell subtotalCell = new PdfPCell(new Phrase("小计", HEADER_FONT));
            subtotalCell.setColspan(5);
            subtotalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            subtotalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            subtotalCell.setPadding(8);
            subtotalCell.setBackgroundColor(new BaseColor(245, 245, 245)); // 浅灰色背景
            subtotalCell.setBorderWidth(0.5f); // 边框宽度
            table.addCell(subtotalCell);
            
            // 小计数量（合并所有小计项，用顿号分隔）
            // 去重处理：若存在多个不限量项（包含"不限量"关键字），合并为单个 "不限量"
            List<String> deduped = new ArrayList<>();
            boolean sawUnlimited = false;
            for (String it : subtotalItems) {
                if (it != null && it.contains("不限量")) {
                    if (!sawUnlimited) {
                        deduped.add("不限量");
                        sawUnlimited = true;
                    } else {
                        // skip duplicate unlimited entry
                    }
                } else {
                    deduped.add(it);
                }
            }
            String subtotalQuantityStr = String.join("、", deduped);
            addCell(table, subtotalQuantityStr, Element.ALIGN_RIGHT, HEADER_FONT);
            
            // 金额列显示"/"
            addCell(table, "/", Element.ALIGN_CENTER, HEADER_FONT);
            
            // 付款方列显示"/"
            addCell(table, "/", Element.ALIGN_CENTER, HEADER_FONT);
        } catch (Exception e) {
            log.warn("解析小计摘要JSON失败：{}", subtotalSummaryJson, e);
            // 解析失败时不添加小计行，不影响PDF生成
        }
    }

    /**
     * 添加合计行
     * 根据小计JSON数据计算合计，相同unit进行加法运算，不同unit用顿号分开
     * 
     * @param table PDF表格
     * @param allSubtotalSummaryList 所有小计JSON数据列表，格式：[{unit,total}]
     */
    private static void addTotalRow(PdfPTable table, List<String> allSubtotalSummaryList) throws DocumentException {
        // 合并前5列显示"合计"
        PdfPCell totalCell = new PdfPCell(new Phrase("合计", HEADER_FONT));
        totalCell.setColspan(5);
        totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalCell.setPadding(8);
        totalCell.setBackgroundColor(new BaseColor(240, 240, 240)); // 浅灰色背景
        totalCell.setBorderWidth(0.5f); // 边框宽度
        table.addCell(totalCell);

        // 根据小计JSON数据计算合计
        String totalQuantityStr = calculateTotalFromSubtotalSummary(allSubtotalSummaryList);
        addCell(table, totalQuantityStr, Element.ALIGN_RIGHT, HEADER_FONT);

        // 金额列显示"/"
        addCell(table, "/", Element.ALIGN_CENTER, HEADER_FONT);

        // 付款方列显示"/"
        addCell(table, "/", Element.ALIGN_CENTER, HEADER_FONT);
    }
    
    /**
     * 根据小计JSON数据计算合计
     * 如果unit相同，则进行加法运算；如果unit不同，则通过顿号分开
     * 
     * @param allSubtotalSummaryList 所有小计JSON数据列表，格式：[{unit,total}]
     * @return 合计字符串，例如："100.5 吨、20 桶"
     */
    private static String calculateTotalFromSubtotalSummary(List<String> allSubtotalSummaryList) {
        if (allSubtotalSummaryList == null || allSubtotalSummaryList.isEmpty()) {
            return "不限量";
        }

        Map<String, BigDecimal> unitTotalMap = new HashMap<>();
        java.util.Set<String> unlimitedUnits = new java.util.LinkedHashSet<>();
        java.util.List<String> unitOrder = new ArrayList<>();

        try {
            for (String subtotalSummaryJson : allSubtotalSummaryList) {
                if (StrUtil.isBlank(subtotalSummaryJson)) {
                    continue;
                }
                JSONArray jsonArray = JSONUtil.parseArray(subtotalSummaryJson);
                if (jsonArray == null || jsonArray.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String unit = jsonObject.getStr("unit");
                    BigDecimal total = jsonObject.getBigDecimal("total");
                    if (StrUtil.isBlank(unit) || total == null) {
                        continue;
                    }
                    if (!unitOrder.contains(unit)) {
                        unitOrder.add(unit);
                    }
                    if (total.compareTo(BigDecimal.valueOf(-1)) == 0) {
                        unlimitedUnits.add(unit);
                        continue;
                    }
                    unitTotalMap.put(unit, unitTotalMap.getOrDefault(unit, BigDecimal.ZERO).add(total));
                }
            }
        } catch (Exception e) {
            log.warn("解析小计JSON数据失败，使用默认合计：{}", e.getMessage());
            return "不限量";
        }

        if (unitOrder.isEmpty()) {
            return "不限量";
        }

        List<String> totalItems = new ArrayList<>();
        for (String unit : unitOrder) {
            if (unlimitedUnits.contains(unit)) {
                totalItems.add("不限量");
            } else {
                BigDecimal total = unitTotalMap.getOrDefault(unit, BigDecimal.ZERO);
                totalItems.add(total.stripTrailingZeros().toPlainString() + " " + unit);
            }
        }

        return String.join("、", totalItems);
    }

    /**
     * 创建支持多行文本的单元格
     */
    private static PdfPCell createMultiLineCell(String text, Font font) throws DocumentException {
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
            return new PdfPCell(paragraph);
        } else {
            return new PdfPCell(new Phrase(text, font));
        }
    }

    /**
     * 添加单元格（使用默认字体，禁止换行）
     */
    private static void addCell(PdfPTable table, String text, int alignment) throws DocumentException {
        addCell(table, text, alignment, NORMAL_FONT, false);
    }

    /**
     * 添加单元格（指定字体，控制是否允许换行）
     * @param table PDF表格
     * @param text 文本内容
     * @param alignment 对齐方式
     * @param font 字体
     * @param allowWrap 是否允许自动换行
     */
    private static void addCell(PdfPTable table, String text, int alignment, Font font, boolean allowWrap) throws DocumentException {
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
            cell = new PdfPCell(new Phrase(text, font));
        }
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderWidth(0.5f); // 边框宽度
        // 根据 allowWrap 参数控制是否允许换行
        cell.setNoWrap(!allowWrap);
        table.addCell(cell);
    }

    /**
     * 添加单元格（指定字体，默认禁止换行，保持向后兼容）
     */
    private static void addCell(PdfPTable table, String text, int alignment, Font font) throws DocumentException {
        addCell(table, text, alignment, font, false);
    }

    /**
     * 添加备注（自动检查并添加服务期限信息）
     */
    private static void addRemark(Document document, ContractDetailResponse contractDetail) throws DocumentException {
        String remark = contractDetail.getRemark();
        if (remark == null) {
            remark = "";
        }
        remark = remark.trim();
        
        // 构建服务期限信息
        String servicePeriod = buildServicePeriod(contractDetail);
        
        // 若备注中已有“服务期限”相关文本，直接替换为数据库中的标准服务期
        if (StrUtil.isNotBlank(servicePeriod) && StrUtil.isNotBlank(remark) && remark.contains("服务期限")) {
            try {
                remark = remark.replaceAll("(?s)服务期限.*?(?=\\r?\\n|$)", servicePeriod);
            } catch (Exception ignore) { }
        }
        // 检查备注中是否已包含服务期限信息
        boolean hasServicePeriod = false;
        if (StrUtil.isNotBlank(servicePeriod)) {
            // 检查备注中是否包含"服务期限"关键词
            hasServicePeriod = remark.contains("服务期限") || remark.contains(servicePeriod);
        }
        
        // 构建完整的备注内容
        StringBuilder remarkBuilder = new StringBuilder();
        if (StrUtil.isNotBlank(remark)) {
            // 拆分备注内容并按序号排序（支持换行或同一行内的"1、2、..."）
            List<String> lines = splitRemarkLines(remark);
            List<String> numbered = new ArrayList<>();
            List<String> others = new ArrayList<>();
            for (String line : lines) {
                if (StrUtil.isBlank(line)) {
                    continue;
                }
                String trimLine = line.trim();
                if (extractRemarkOrder(trimLine) > 0) {
                    numbered.add(trimLine);
                } else {
                    others.add(trimLine);
                }
            }
            // 按数字前缀排序
            numbered.sort((a, b) -> Integer.compare(extractRemarkOrder(a), extractRemarkOrder(b)));
            for (int i = 0; i < numbered.size(); i++) {
                if (i > 0) {
                    remarkBuilder.append("\n");
                }
                remarkBuilder.append(numbered.get(i));
            }
            for (String line : others) {
                if (remarkBuilder.length() > 0) {
                    remarkBuilder.append("\n");
                }
                remarkBuilder.append(line);
            }
        }
        
        // 如果备注中没有服务期限信息，且存在服务期限，则自动添加
        if (!hasServicePeriod && StrUtil.isNotBlank(servicePeriod)) {
            if (remarkBuilder.length() > 0) {
                remarkBuilder.append("\n");
            }
            remarkBuilder.append(servicePeriod);
        }
        
        // 如果备注内容为空，则不添加备注段落
        if (remarkBuilder.length() == 0) {
            return;
        }
        
        // 处理长备注，自动换行
        Paragraph remarkPara = new Paragraph("备注：" + remarkBuilder.toString(), NORMAL_FONT);
        remarkPara.setSpacingBefore(10);
        remarkPara.setSpacingAfter(8);
        remarkPara.setFirstLineIndent(0); // 首行缩进
        document.add(remarkPara);
    }
    
    /**
     * 构建服务期限信息
     * 格式：服务期限：2025年1月1日至2025年12月31日
     */
    private static String buildServicePeriod(ContractDetailResponse contractDetail) {
        if (contractDetail.getValidFrom() == null || contractDetail.getValidTo() == null) {
            return null;
        }
        
        // 格式化日期：yyyy年MM月dd日
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
        String validFromStr = contractDetail.getValidFrom().format(formatter);
        String validToStr = contractDetail.getValidTo().format(formatter);
        
        return "服务期限：" + validFromStr + "至" + validToStr;
    }

    /**
     * 将备注内容拆分为多条记录：
     * 1) 如果包含换行，则按行拆分；
     * 2) 否则尝试按 "数字+分隔符(、 . ) ）" 的模式在同一行内拆分。
     */
    private static List<String> splitRemarkLines(String remark) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isBlank(remark)) {
            return result;
        }
        String normalized = remark.replace("\r\n", "\n").trim();
        // 情况1：存在换行，直接按行拆分
        if (normalized.contains("\n")) {
            String[] arr = normalized.split("\\r?\\n");
            for (String s : arr) {
                if (StrUtil.isNotBlank(s)) {
                    result.add(s.trim());
                }
            }
            return result;
        }

        // 情况2：单行场景，尝试识别 "1、"、"2." 等编号并进行切分
        Pattern p = Pattern.compile("(\\d+)([、\\.）)])");
        Matcher m = p.matcher(normalized);
        List<Integer> startIndexList = new ArrayList<>();
        while (m.find()) {
            startIndexList.add(m.start());
        }
        // 如果没找到编号，整段作为一条
        if (startIndexList.isEmpty() || startIndexList.get(0) != 0) {
            result.add(normalized);
            return result;
        }

        for (int i = 0; i < startIndexList.size(); i++) {
            int start = startIndexList.get(i);
            int end = (i == startIndexList.size() - 1) ? normalized.length() : startIndexList.get(i + 1);
            String seg = normalized.substring(start, end).trim();
            if (StrUtil.isNotBlank(seg)) {
                result.add(seg);
            }
        }
        if (result.isEmpty()) {
            result.add(normalized);
        }
        return result;
    }

    /**
     * 提取备注行前缀中的数字序号，例如：
     * "1、xxxx" -> 1, "2. xxxx" -> 2，其它返回 0
     */
    private static int extractRemarkOrder(String line) {
        if (StrUtil.isBlank(line)) {
            return 0;
        }
        String trim = line.trim();
        int i = 0;
        while (i < trim.length() && Character.isDigit(trim.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return 0;
        }
        // 检查数字后是否紧跟常见分隔符（如 "、"、"."、")" 等），不是也视为普通文本
        if (i < trim.length()) {
            char c = trim.charAt(i);
            if (c != '、' && c != '.' && c != ')' && c != '）') {
                return 0;
            }
        }
        try {
            return Integer.parseInt(trim.substring(0, i));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 添加签名区域
     */
    private static void addSignatureArea(Document document, ContractDetailResponse contractDetail)
            throws DocumentException {
        // 创建签名表格
        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(100);
        signatureTable.setWidths(new float[]{1f, 1f});
        signatureTable.setSpacingBefore(12);
        signatureTable.setSpacingAfter(6);
        signatureTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // 创建甲方签名单元格
        PdfPCell partyACell = createPartyASignatureCell();
        signatureTable.addCell(partyACell);

        // 创建乙方签名单元格
        PdfPCell partyBCell = createPartyBSignatureCell();
        signatureTable.addCell(partyBCell);

        document.add(signatureTable);
    }

    /**
     * 创建甲方签名单元格
     * 格式：甲方(盖章):、法定代表人(签字或盖章):、业务联系人:、联系电话:
     */
    private static PdfPCell createPartyASignatureCell() {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(15);
        cell.setMinimumHeight(120);
        cell.setBorder(Rectangle.NO_BORDER);

        // 创建内部表格
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        // 使用黑色字体（NORMAL_FONT已经是黑色）
        Font blackFont = new Font(baseFont, 10, Font.NORMAL, BaseColor.BLACK);

        // 甲方(盖章):
        PdfPCell cell1 = new PdfPCell(new Phrase("甲方(盖章):", blackFont));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setPaddingBottom(8);
        innerTable.addCell(cell1);

        // 法定代表人(签字或盖章):
        PdfPCell cell2 = new PdfPCell(new Phrase("法定代表人(签字或盖章):", blackFont));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setPaddingBottom(8);
        innerTable.addCell(cell2);

        // 业务联系人:
        PdfPCell cell3 = new PdfPCell(new Phrase("业务联系人:", blackFont));
        cell3.setBorder(Rectangle.NO_BORDER);
        cell3.setPaddingBottom(8);
        innerTable.addCell(cell3);

        // 联系电话:
        PdfPCell cell4 = new PdfPCell(new Phrase("联系电话:", blackFont));
        cell4.setBorder(Rectangle.NO_BORDER);
        cell4.setPaddingBottom(5);
        innerTable.addCell(cell4);

        cell.addElement(innerTable);
        return cell;
    }

    /**
     * 创建乙方签名单元格
     * 格式：乙方(盖章):、法定代表人(签字或盖章):、业务联系人:、联系电话:、业务咨询电话:
     */
    private static PdfPCell createPartyBSignatureCell() {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(15);
        cell.setMinimumHeight(120);
        cell.setBorder(Rectangle.NO_BORDER);

        // 创建内部表格
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        // 使用黑色字体（NORMAL_FONT已经是黑色）
        Font blackFont = new Font(baseFont, 10, Font.NORMAL, BaseColor.BLACK);

        // 乙方(盖章):
        PdfPCell cell1 = new PdfPCell(new Phrase("乙方(盖章):", blackFont));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setPaddingBottom(8);
        innerTable.addCell(cell1);

        // 法定代表人(签字或盖章):
        PdfPCell cell2 = new PdfPCell(new Phrase("法定代表人(签字或盖章):", blackFont));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setPaddingBottom(8);
        innerTable.addCell(cell2);

        // 业务联系人:
        PdfPCell cell3 = new PdfPCell(new Phrase("业务联系人:", blackFont));
        cell3.setBorder(Rectangle.NO_BORDER);
        cell3.setPaddingBottom(8);
        innerTable.addCell(cell3);

        // 联系电话:
        PdfPCell cell4 = new PdfPCell(new Phrase("联系电话:", blackFont));
        cell4.setBorder(Rectangle.NO_BORDER);
        cell4.setPaddingBottom(8);
        innerTable.addCell(cell4);

        // 业务咨询电话:
        PdfPCell cell5 = new PdfPCell(new Phrase("业务咨询电话:", blackFont));
        cell5.setBorder(Rectangle.NO_BORDER);
        cell5.setPaddingBottom(5);
        innerTable.addCell(cell5);

        cell.addElement(innerTable);
        return cell;
    }
    
    /**
     * 表格行数据
     */
    private static class TableRowData {
        private int serialNo;
        private ContractDetailResponse.ContractWasteItemResponse wasteItem;
        private String quotationMode;
            // contractItem removed (not used in processing)
        private String amountStr;
        private String payerStr;
        private boolean isSubtotalRow; // 是否为小计行
        private String subtotalSummary; // 小计摘要JSON字符串
        
        public int getSerialNo() { return serialNo; }
        public void setSerialNo(int serialNo) { this.serialNo = serialNo; }
        public ContractDetailResponse.ContractWasteItemResponse getWasteItem() { return wasteItem; }
        public void setWasteItem(ContractDetailResponse.ContractWasteItemResponse wasteItem) { this.wasteItem = wasteItem; }
        public String getQuotationMode() { return quotationMode; }
        public void setQuotationMode(String quotationMode) { this.quotationMode = quotationMode; }
        // setContractItem removed (not used)
        public String getAmountStr() { return amountStr; }
        public void setAmountStr(String amountStr) { this.amountStr = amountStr; }
        public String getPayerStr() { return payerStr; }
        public void setPayerStr(String payerStr) { this.payerStr = payerStr; }
        public boolean isSubtotalRow() { return isSubtotalRow; }
        public void setSubtotalRow(boolean subtotalRow) { isSubtotalRow = subtotalRow; }
        public String getSubtotalSummary() { return subtotalSummary; }
        public void setSubtotalSummary(String subtotalSummary) { this.subtotalSummary = subtotalSummary; }
    }
    
    /**
     * 添加表格行并处理单元格合并
     */
    private static void addTableRowsWithMerge(PdfPTable table, List<TableRowData> rowDataList, 
                                               ArrayList<String> combinedAmountStr, 
                                               ArrayList<String> combinedPayerStr) throws DocumentException {
        if (rowDataList == null || rowDataList.isEmpty()) {
            return;
        }
        
        // 计算每行的金额和付款方字符串（排除小计行）
        List<String> amountList = new ArrayList<>();
        List<String> payerList = new ArrayList<>();
        for (TableRowData rowData : rowDataList) {
            if (rowData.isSubtotalRow()) {
                // 小计行不参与合并计算
                amountList.add(null);
                payerList.add(null);
                continue;
            }
            
            String amountStr = "-";
            // 判断是否为总价包干模式
            boolean isPackageMode = "总价包干".equals(rowData.getQuotationMode()) || "PACKAGE".equals(rowData.getQuotationMode());
            
            if (isPackageMode && rowData.getAmountStr() != null && !rowData.getAmountStr().trim().isEmpty()) {
                // 总价包干模式：优先使用父节点（contractItem 的计价方案），若为空则使用当前危废条目的 pricingPlan
                String baseAmountStr = rowData.getAmountStr();
                String parentPricingPlan = removeContractItemId(baseAmountStr);

                // contract waste item type differs, but has similar fields; use the contract wasteItem directly
                ContractDetailResponse.ContractWasteItemResponse wi = rowData.getWasteItem();
                String wastePricingPlan = wi != null ? wi.getPricingPlan() : null;
                String displayPlan = StrUtil.isNotBlank(parentPricingPlan) ? parentPricingPlan
                        : (StrUtil.isNotBlank(wastePricingPlan) ? wastePricingPlan.trim() : null);

                StringBuilder pkgAmountBuilder = new StringBuilder();
                if (StrUtil.isNotBlank(displayPlan)) {
                    pkgAmountBuilder.append(displayPlan);
                }

                boolean auxEnabled = wi != null && Boolean.TRUE.equals(wi.getEnableAuxiliaryAccounting());

                if (auxEnabled) {
                    // 始终显示三行：父节点计价方案 / 基础单价或占位 / 辅助单价或占位
                    pkgAmountBuilder.append("\n");
                    if (wi.getBaseUnitPrice() != null) {
                        pkgAmountBuilder.append(wi.getBaseUnitPrice().stripTrailingZeros().toPlainString())
                                .append("元/吨");
                    } else if (StrUtil.isNotBlank(wi.getPricingPlan())) {
                        pkgAmountBuilder.append(wi.getPricingPlan().trim()).append("元/吨");
                    } else if (wi.getBaseQuantity() != null) {
                        pkgAmountBuilder.append(wi.getBaseQuantity().stripTrailingZeros().toPlainString()).append(" 吨");
                    } else {
                        pkgAmountBuilder.append("-");
                    }

                    pkgAmountBuilder.append("\n");
                    if (wi.getAuxUnitPrice() != null) {
                        pkgAmountBuilder.append(wi.getAuxUnitPrice().stripTrailingZeros().toPlainString())
                                .append("元/")
                                .append(StrUtil.isNotBlank(wi.getAuxUnit()) ? wi.getAuxUnit() : "-");
                    } else {
                        pkgAmountBuilder.append("-").append("元/").append(StrUtil.isNotBlank(wi != null ? wi.getAuxUnit() : null) ? wi.getAuxUnit() : "-");
                    }
                } else {
                    // 未启用辅助核算：显示父节点 + 基础单价（若无则尝试 pricingPlan/baseQuantity 或占位）
                    pkgAmountBuilder.append("\n");
                    if (wi.getBaseUnitPrice() != null) {
                        pkgAmountBuilder.append(wi.getBaseUnitPrice().stripTrailingZeros().toPlainString())
                                .append("元/吨");
                    } else if (StrUtil.isNotBlank(wi.getPricingPlan())) {
                        pkgAmountBuilder.append(wi.getPricingPlan().trim()).append("元/吨");
                    } else if (wi.getBaseQuantity() != null) {
                        pkgAmountBuilder.append(wi.getBaseQuantity().stripTrailingZeros().toPlainString()).append(" 吨");
                    } else {
                        pkgAmountBuilder.append("-");
                    }
                }

                amountStr = pkgAmountBuilder.length() > 0 ? pkgAmountBuilder.toString() : (displayPlan != null ? displayPlan : "-");
            } else if (!isPackageMode && rowData.getWasteItem().getPricingPlan() != null && !rowData.getWasteItem().getPricingPlan().trim().isEmpty()) {
                // 按量结算模式：使用 formatAmount（基础元/吨，并可追加辅助单价）
                amountStr = formatAmount(rowData.getWasteItem(), null);
            } else if (rowData.getAmountStr() != null && !rowData.getAmountStr().trim().isEmpty()) {
                // 其他情况，使用传入的金额字符串
                amountStr = removeContractItemId(rowData.getAmountStr());
            }
            amountList.add(amountStr);
            
            String payerStr = "-";
            if (rowData.getPayerStr() != null && !rowData.getPayerStr().trim().isEmpty()) {
                payerStr = rowData.getPayerStr();
            } else if (rowData.getWasteItem().getPayer() != null && !rowData.getWasteItem().getPayer().trim().isEmpty()) {
                payerStr = rowData.getWasteItem().getPayer();
            }
            payerList.add(payerStr);
        }
        
        // 计算每行的合并行数（rowspan），排除小计行
        int[] amountRowspan = calculateRowspan(amountList, combinedAmountStr);
        int[] payerRowspan = calculateRowspan(payerList, combinedPayerStr);
        
        // 添加行
        for (int i = 0; i < rowDataList.size(); i++) {
            TableRowData rowData = rowDataList.get(i);
            
            // 如果是小计行，直接添加小计行（subtotalSummary 为空时显示“不限量”）
            if (rowData.isSubtotalRow()) {
                String subtotalJson = rowData.getSubtotalSummary();
                if (StrUtil.isBlank(subtotalJson)) {
                    // 当 subtotalSummary 为空时，直接显示“不限量”小计行
                    PdfPCell subtotalCell = new PdfPCell(new Phrase("小计", HEADER_FONT));
                    subtotalCell.setColspan(5);
                    subtotalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    subtotalCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    subtotalCell.setPadding(8);
                    subtotalCell.setBackgroundColor(new BaseColor(245, 245, 245)); // 浅灰色背景
                    subtotalCell.setBorderWidth(0.5f); // 边框宽度
                    table.addCell(subtotalCell);

                    addCell(table, "不限量", Element.ALIGN_RIGHT, HEADER_FONT);
                    addCell(table, "/", Element.ALIGN_CENTER, HEADER_FONT);
                    addCell(table, "/", Element.ALIGN_CENTER, HEADER_FONT);
                } else {
                    addSubtotalSummaryRows(table, subtotalJson);
                }
                continue;
            }
            
            // 序号
            addCell(table, String.valueOf(rowData.getSerialNo()), Element.ALIGN_CENTER);
            
            // 废物类别
            addCell(table, rowData.getWasteItem().getWasteCategory() != null ? rowData.getWasteItem().getWasteCategory() : "-", Element.ALIGN_CENTER);

            // 废物代码
            addCell(table, rowData.getWasteItem().getWasteCode() != null ? rowData.getWasteItem().getWasteCode() : "-", Element.ALIGN_CENTER);
            
            // 废物名称（允许换行）
            addCell(table, rowData.getWasteItem().getHazardousWaste() != null ? rowData.getWasteItem().getHazardousWaste() : "-", Element.ALIGN_LEFT, NORMAL_FONT, true);
            
            // 废物形态
            addCell(table, rowData.getWasteItem().getForm() != null ? rowData.getWasteItem().getForm() : "-", Element.ALIGN_CENTER);
            
            // 计划转移数量（支持无限量与辅助计量展示，基础单位固定为 吨）
            String quantityStr = "-";
            if (rowData.getWasteItem().getPlannedQuantity() != null) {
                BigDecimal planned = rowData.getWasteItem().getPlannedQuantity();
                if (planned.compareTo(BigDecimal.valueOf(-1)) == 0) {
                    quantityStr = "无限量";
                } else if (Boolean.TRUE.equals(rowData.getWasteItem().getEnableAuxiliaryAccounting())) {
                    String baseQtyStr = "-";
                    if (rowData.getWasteItem().getBaseQuantity() != null) {
                        baseQtyStr = rowData.getWasteItem().getBaseQuantity().stripTrailingZeros().toPlainString() + " 吨";
                    } else if (planned != null) {
                        baseQtyStr = planned.stripTrailingZeros().toPlainString() + " 吨";
                    }
                    String auxQtyStr = "-";
                    if (rowData.getWasteItem().getAuxQuantity() != null) {
                        auxQtyStr = rowData.getWasteItem().getAuxQuantity().stripTrailingZeros().toPlainString();
                        if (StrUtil.isNotBlank(rowData.getWasteItem().getAuxUnit())) {
                            auxQtyStr += " " + rowData.getWasteItem().getAuxUnit();
                        }
                    }
                    quantityStr = baseQtyStr + "\n" + auxQtyStr;
                } else {
                    String qty = planned.stripTrailingZeros().toPlainString();
                    if (StrUtil.isNotBlank(rowData.getWasteItem().getUnit())) {
                        qty += " " + rowData.getWasteItem().getUnit();
                    } else if (rowData.getWasteItem().getBaseQuantity() != null) {
                        qty += "（≈" + rowData.getWasteItem().getBaseQuantity().stripTrailingZeros().toPlainString() + " 吨）";
                    }
                    quantityStr = qty;
                }
            }
            // 计划转移数量（允许换行）
            addCell(table, quantityStr, Element.ALIGN_RIGHT, NORMAL_FONT, true);
            
            // 金额（需要合并的单元格）
            String amountStr = amountList.get(i);
            if (amountStr != null) {
                // 如果包含多行（父级计价方案 + 超量单价），将其标注为“包年/包次/包月：... 超量：...”
                String displayAmountStr = amountStr;
                try {
                    String[] parts = amountStr.split("\\r?\\n");
                    if (parts.length >= 2) {
                        String first = parts[0].trim();
                        String prefix;
                        if (first.contains("次") || first.contains("/次")) {
                            prefix = "包次：";
                        } else if (first.contains("年") || first.contains("/年")) {
                            prefix = "包年：";
                        } else if (first.contains("月") || first.contains("/月")) {
                            prefix = "包月：";
                        } else {
                            prefix = "包年：";
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append(prefix).append(first);
                        // 第一条超量内联显示（如果存在）
                        String second = parts[1].trim();
                        if (second.length() > 0) {
                            sb.append(" ").append("超量：").append(second);
                        }
                        // 其余部分（从第三行开始）每行单独换行显示
                        for (int p = 2; p < parts.length; p++) {
                            String part = parts[p].trim();
                            if (part.length() == 0) {
                                continue;
                            }
                            sb.append("\n").append(part);
                        }
                        displayAmountStr = sb.toString();
                    }
                } catch (Exception ignore) {
                    displayAmountStr = amountStr;
                }

                if (amountRowspan[i] > 1) {
                    PdfPCell amountCell = createMultiLineCell(displayAmountStr, NORMAL_FONT);
                    amountCell.setRowspan(amountRowspan[i]);
                    amountCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    amountCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    amountCell.setPadding(6);
                    amountCell.setBorderWidth(0.5f);
                    amountCell.setNoWrap(false); // 金额允许换行
                    table.addCell(amountCell);
                } else if (amountRowspan[i] == 1) {
                    // 金额（允许换行）
                    addCell(table, displayAmountStr, Element.ALIGN_LEFT, NORMAL_FONT, true);
                }
            }
            
            // 付款方（需要合并的单元格）
            String payerStr = payerList.get(i);
            if (payerStr != null) {
                // 判断是否为总价包干模式（值在 combinedPayerStr 中）
                boolean isPackagePayer = combinedPayerStr.contains(payerStr);
                
                if (payerRowspan[i] > 1) {
                    // 需要合并，去掉 contractItemId，只显示 payer
                    String mergedPayerStr = removeContractItemId(payerStr);
                    PdfPCell payerCell = new PdfPCell(new Phrase(mergedPayerStr, NORMAL_FONT));
                    payerCell.setRowspan(payerRowspan[i]);
                    payerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    payerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    payerCell.setPadding(6);
                    payerCell.setBorderWidth(0.5f);
                    payerCell.setNoWrap(true); // 防止付款方换行
                    table.addCell(payerCell);
                } else if (payerRowspan[i] == 1) {
                    // 单行数据
                    if (isPackagePayer) {
                        // 总价包干模式，即使只有一条数据也要去掉 contractItemId
                        String displayPayerStr = removeContractItemId(payerStr);
                        addCell(table, displayPayerStr, Element.ALIGN_CENTER);
                    } else {
                        // 按量结算模式，显示完整内容
                        addCell(table, payerStr, Element.ALIGN_CENTER);
                    }
                }
                // payerRowspan[i] == 0 表示被合并，跳过
            }
        }
    }
    
    /**
     * 计算每行的合并行数
     * @param valueList 值列表（可能包含 null，表示小计行）
     * @param combinedList 需要合并的值列表（combinedAmountStr 或 combinedPayerStr）
     * @return 每行的 rowspan 数组，0 表示被合并（由前面的行合并），1 表示不合并，>1 表示合并的行数
     */
    private static int[] calculateRowspan(List<String> valueList, ArrayList<String> combinedList) {
        int[] rowspan = new int[valueList.size()];
        
        for (int i = 0; i < valueList.size(); i++) {
            String currentValue = valueList.get(i);
            
            // 如果是 null（小计行），不合并
            if (currentValue == null) {
                rowspan[i] = 1;
                continue;
            }
            
            // 如果当前值不在需要合并的列表中，不合并
            if (!combinedList.contains(currentValue)) {
                rowspan[i] = 1;
                continue;
            }
            
            // 检查前面的行是否已经合并了当前值（跳过 null 值）
            boolean isMerged = false;
            for (int j = i - 1; j >= 0; j--) {
                if (valueList.get(j) == null) {
                    // 遇到小计行，停止检查（小计行会打断合并）
                    break;
                }
                if (rowspan[j] > 1 && currentValue.equals(valueList.get(j))) {
                    // 检查是否还在合并范围内
                    int mergeEnd = j + rowspan[j];
                    // 检查合并范围内是否有小计行，如果有则调整合并结束位置
                    for (int k = j + 1; k < mergeEnd && k < valueList.size(); k++) {
                        if (valueList.get(k) == null) {
                            mergeEnd = k;
                            break;
                        }
                    }
                    if (i < mergeEnd) {
                        isMerged = true;
                        break;
                    }
                }
            }
            
            if (isMerged) {
                // 当前行被前面的行合并了
                rowspan[i] = 0;
                continue;
            }
            
            // 计算连续相同值的行数（遇到 null 或不同值则停止）
            int count = 1;
            for (int j = i + 1; j < valueList.size(); j++) {
                String nextValue = valueList.get(j);
                if (nextValue == null) {
                    // 遇到小计行，停止合并
                    break;
                }
                if (currentValue.equals(nextValue)) {
                    count++;
                } else {
                    break;
                }
            }
            
            rowspan[i] = count;
        }
        
        return rowspan;
    }
    
    /**
     * 从组合字符串中移除 contractItemId，只保留后面的内容
     * 例如："18|1500元/吨" -> "1500元/吨"
     *      "20|甲方" -> "甲方"
     * 
     * @param combinedStr 组合字符串（格式：contractItemId | 内容，使用 | 作为分隔符）
     * @return 去掉 contractItemId 后的字符串
     */
    private static String removeContractItemId(String combinedStr) {
        if (StrUtil.isBlank(combinedStr)) {
            return combinedStr;
        }
        
        // 查找第一个 | 的位置
        int separatorIndex = combinedStr.indexOf("|");
        if (separatorIndex > 0) {
            // 如果找到 |，返回 | 后面的内容
            String result = combinedStr.substring(separatorIndex + 1).trim();
            return StrUtil.isNotBlank(result) ? result : combinedStr;
        }
        
        // 如果没有 |，返回原字符串（可能不是组合格式）
        return combinedStr;
    }

    /**
     * 格式化合同危废条目的计划数量（复用报价单的显示逻辑）
     */
    // formatQuantity removed (was unused)

    /**
     * 格式化合同危废条目的金额/单价显示（复用报价单逻辑）
     */
    private static String formatAmount(ContractDetailResponse.ContractWasteItemResponse wasteItem, String parentPricingPlan) {
        // parentPricingPlan: 当总价包干模式时传入父级计价方案，否则为 null/""，但我们主要处理按量结算情况
        if (wasteItem == null) {
            return "-";
        }
        // 按量结算优先展示计价方案作为基础单价（元/吨），若启用辅助核算并存在辅助单价，追加辅助单价
        if (parentPricingPlan != null && !parentPricingPlan.trim().isEmpty()) {
            // package mode handled elsewhere
            return parentPricingPlan;
        }

        StringBuilder amountBuilder = new StringBuilder();
        if (wasteItem.getPricingPlan() != null && !wasteItem.getPricingPlan().trim().isEmpty()) {
            amountBuilder.append(wasteItem.getPricingPlan().trim()).append("元/吨");
        }

        if (Boolean.TRUE.equals(wasteItem.getEnableAuxiliaryAccounting())
                && wasteItem.getAuxUnitPrice() != null
                && StrUtil.isNotBlank(wasteItem.getAuxUnit())) {
            if (amountBuilder.length() > 0) {
                amountBuilder.append("\n");
            }
            amountBuilder.append(wasteItem.getAuxUnitPrice().stripTrailingZeros().toPlainString())
                    .append("元/")
                    .append(wasteItem.getAuxUnit());
        } else if (wasteItem.getBaseUnitPrice() != null) {
            if (amountBuilder.length() == 0) {
                amountBuilder.append(wasteItem.getBaseUnitPrice().stripTrailingZeros().toPlainString()).append("元/吨");
            }
        }

        // 回退：如果没有任何信息，尝试显示 "-" 或 pricingPlan 原文
        if (amountBuilder.length() == 0) {
            return "-";
        }
        return amountBuilder.toString();
    }
}



