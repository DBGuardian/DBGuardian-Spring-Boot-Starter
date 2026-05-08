package com.erp.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.erp.controller.contract.dto.ContractDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ContractWordGenerator {

    private ContractWordGenerator() {
    }

    public static void generateWord(ContractDetailResponse contractDetail, OutputStream outputStream) throws IOException {
        try {
            WordprocessingMLPackage wordPackage = Docx4jHelper.createPackage();
            MainDocumentPart documentPart = wordPackage.getMainDocumentPart();

            createTitle(documentPart, "危险废物处置服务报价单");
            createBasicInfoSection(documentPart, contractDetail);
            createDetailTable(documentPart, contractDetail);
            Docx4jHelper.addBlankLines(documentPart, 1);
            createOutOfScopeServicesTable(documentPart, contractDetail);
            createRemarkSection(documentPart, contractDetail);
            createSignatureSection(documentPart, contractDetail);

            wordPackage.save(outputStream);
            outputStream.flush();
        } catch (Exception e) {
            throw new IOException("生成Word文档失败", e);
        }
    }

    private static void createTitle(MainDocumentPart documentPart, String title) {
        P paragraph = Docx4jHelper.createParagraph(documentPart);
        Docx4jHelper.setAlignment(paragraph, "CENTER");
        R run = Docx4jHelper.createRun(paragraph);
        Docx4jHelper.setBold(run, true);
        Docx4jHelper.setFontSize(run, 20);
        Docx4jHelper.setText(run, title);
        Docx4jHelper.addBreak(run);
    }

    private static void createBasicInfoSection(MainDocumentPart documentPart, ContractDetailResponse detail) {
        P paragraph = Docx4jHelper.createParagraph(documentPart);
        Docx4jHelper.setAlignment(paragraph, "LEFT");
        R run = Docx4jHelper.createRun(paragraph);
        Docx4jHelper.setFontSize(run, 11);
        appendLabelValue(run, "合同号：", detail.getContractNo());
    }

    private static void appendLabelValue(R run, String label, String value) {
        P paragraph = (P) run.getParent();
        Docx4jHelper.setBold(run, false);
        Docx4jHelper.setText(run, label);
        R boldRun = Docx4jHelper.createRun(paragraph);
        Docx4jHelper.setBold(boldRun, true);
        Docx4jHelper.setText(boldRun, StrUtil.isNotBlank(value) ? value : "-");
        R normalRun = Docx4jHelper.createRun(paragraph);
        Docx4jHelper.setBold(normalRun, false);
        Docx4jHelper.addBreak(normalRun);
    }

    private static void createDetailTable(MainDocumentPart documentPart, ContractDetailResponse detail) {
        Tbl table = Docx4jHelper.createTable(documentPart);

        Tr header = Docx4jHelper.createTableRow(table);
        Tc h0 = Docx4jHelper.createTableCell(header);
        Docx4jHelper.setCellText(h0, "序号");
        Docx4jHelper.setCellNoWrap(h0, true);
        Docx4jHelper.setCellHorizontalAlignment(h0, "CENTER");
        Tc h1 = Docx4jHelper.createTableCell(header);
        Docx4jHelper.setCellText(h1, "废物类别");
        Docx4jHelper.setCellNoWrap(h1, true);
        Docx4jHelper.setCellHorizontalAlignment(h1, "CENTER");
        Tc h2 = Docx4jHelper.createTableCell(header);
        Docx4jHelper.setCellText(h2, "废物代码");
        Docx4jHelper.setCellNoWrap(h2, true);
        Docx4jHelper.setCellHorizontalAlignment(h2, "CENTER");
        Tc h3 = Docx4jHelper.createTableCell(header);
        Docx4jHelper.setCellText(h3, "废物名称");
        Docx4jHelper.setCellNoWrap(h3, true);
        Docx4jHelper.setCellHorizontalAlignment(h3, "CENTER");
        Tc h4 = Docx4jHelper.createTableCell(header);
        Docx4jHelper.setCellText(h4, "废物形态");
        Docx4jHelper.setCellNoWrap(h4, true);
        Docx4jHelper.setCellHorizontalAlignment(h4, "CENTER");
        Tc h5 = Docx4jHelper.createTableCell(header);
        Docx4jHelper.setCellText(h5, "计划转移数量");
        Docx4jHelper.setCellNoWrap(h5, true);
        Docx4jHelper.setCellHorizontalAlignment(h5, "CENTER");
        Tc h6 = Docx4jHelper.createTableCell(header);
        Docx4jHelper.setCellText(h6, "金额");
        Docx4jHelper.setCellNoWrap(h6, true);
        Docx4jHelper.setCellHorizontalAlignment(h6, "CENTER");
        Tc h7 = Docx4jHelper.createTableCell(header);
        Docx4jHelper.setCellText(h7, "付款方");
        Docx4jHelper.setCellNoWrap(h7, true);
        Docx4jHelper.setCellHorizontalAlignment(h7, "CENTER");

        int[] widthRatios = {2, 5, 5, 5,5, 6, 4, 4};
        Docx4jHelper.setColumnWidthsByRatios(table, widthRatios, documentPart);

        List<RowData> rows = buildRowData(detail);
        for (RowData rowData : rows) {
            Tr row = Docx4jHelper.createTableRow(table);
            String[] cols = rowData.getColumns();
            boolean isSubtotalOrTotal = "小计".equals(cols[0]) || "合计".equals(cols[0]);

            if (isSubtotalOrTotal) {
                Tc cell0 = Docx4jHelper.createTableCell(row);
                Docx4jHelper.setCellTextWithStyle(cell0, cols[0], 11);
                Docx4jHelper.setCellGridSpan(cell0, 5);

                Tc cell1 = Docx4jHelper.createTableCell(row);
                Docx4jHelper.setCellTextWithStyle(cell1, cols[5], 11);
                Docx4jHelper.setCellNoWrap(cell1, false);
                Docx4jHelper.setCellHorizontalAlignment(cell1, "RIGHT");

                Tc cell2 = Docx4jHelper.createTableCell(row);
                Docx4jHelper.setCellTextWithStyle(cell2, cols[6], 11);
                Docx4jHelper.setCellNoWrap(cell2, false);
                Docx4jHelper.setCellHorizontalAlignment(cell2, "CENTER");

                Tc cell3 = Docx4jHelper.createTableCell(row);
                Docx4jHelper.setCellTextWithStyle(cell3, cols[7], 11);
                Docx4jHelper.setCellHorizontalAlignment(cell3, "CENTER");
            } else {
                for (int i = 0; i < cols.length; i++) {
                    Tc cell = Docx4jHelper.createTableCell(row);
                    Docx4jHelper.setCellTextWithStyle(cell, cols[i], 11);
                    String alignment = "CENTER";
                    if (i == 3 || i == 6) {
                        alignment = "LEFT";
                    } else if (i == 5) {
                        alignment = "RIGHT";
                    }
                    Docx4jHelper.setCellHorizontalAlignment(cell, alignment);
                    if (i == 3 || i == 5 || i == 6) {
                        Docx4jHelper.setCellNoWrap(cell, false);
                    } else {
                        Docx4jHelper.setCellNoWrap(cell, true);
                    }
                }
            }
        }
    }

    private static List<RowData> buildRowData(ContractDetailResponse detail) {
        List<RowData> rows = new ArrayList<>();
        List<String> subtotalJsonList = new ArrayList<>();
        int index = 1;
        if (detail.getQuotationItems() != null) {
            for (ContractDetailResponse.ContractItemResponse item : detail.getQuotationItems()) {
                if (item.getWasteItems() == null || item.getWasteItems().isEmpty()) {
                    continue;
                }
                boolean isPackage = "总价包干".equals(item.getQuotationMode()) || "PACKAGE".equals(item.getQuotationMode());
                String itemPricing = item.getPricingPlan();
                String itemPayer = StrUtil.nullToDefault(item.getPayer(), "-");

                for (ContractDetailResponse.ContractWasteItemResponse waste : item.getWasteItems()) {
                    String[] columns = new String[8];
                    columns[0] = String.valueOf(index++);
                    columns[1] = defaultString(waste.getWasteCategory());
                    columns[2] = defaultString(waste.getWasteCode());
                    columns[3] = defaultString(waste.getHazardousWaste());
                    columns[4] = defaultString(waste.getForm());
                    columns[5] = buildQuantityCell(waste);
                    columns[6] = isPackage
                            ? buildPackageAmountCell(itemPricing, waste)
                            : buildAmountCell(waste);
                    columns[7] = isPackage
                            ? (StrUtil.isNotBlank(itemPayer) ? itemPayer : "-")
                            : (StrUtil.isNotBlank(waste.getPayer()) ? waste.getPayer() : "-");
                    rows.add(RowData.detail(columns));
                }

                if (StrUtil.isNotBlank(item.getSubtotalSummary())) {
                    rows.add(RowData.subtotal(buildSubtotalLabel(item.getSubtotalSummary())));
                    subtotalJsonList.add(item.getSubtotalSummary());
                }
            }
        }

        if (!subtotalJsonList.isEmpty()) {
            rows.add(RowData.total(buildTotalLabel(subtotalJsonList)));
        }
        return rows;
    }

    private static String buildQuantityCell(ContractDetailResponse.ContractWasteItemResponse waste) {
        if (waste.getPlannedQuantity() == null) {
            return "-";
        }
        BigDecimal planned = waste.getPlannedQuantity();
        if (planned.compareTo(BigDecimal.valueOf(-1)) == 0) {
            return "不限量";
        }
        if (Boolean.TRUE.equals(waste.getEnableAuxiliaryAccounting())) {
            String baseQtyStr = "-";
            if (waste.getBaseQuantity() != null) {
                baseQtyStr = waste.getBaseQuantity().stripTrailingZeros().toPlainString() + " 吨";
            } else if (planned != null) {
                baseQtyStr = planned.stripTrailingZeros().toPlainString() + " 吨";
            }
            String auxQtyStr = "-";
            if (waste.getAuxQuantity() != null) {
                auxQtyStr = waste.getAuxQuantity().stripTrailingZeros().toPlainString();
                if (StrUtil.isNotBlank(waste.getAuxUnit())) {
                    auxQtyStr += " " + waste.getAuxUnit();
                }
            }
            return baseQtyStr + "\n" + auxQtyStr;
        }
        String qty = planned.stripTrailingZeros().toPlainString();
        if (StrUtil.isNotBlank(waste.getUnit())) {
            qty += " " + waste.getUnit();
        } else if (waste.getBaseQuantity() != null) {
            qty += "（≈" + waste.getBaseQuantity().stripTrailingZeros().toPlainString() + " 吨）";
        }
        return qty;
    }

    private static String buildAmountCell(ContractDetailResponse.ContractWasteItemResponse waste) {
        if (waste.getPricingPlan() == null || waste.getPricingPlan().trim().isEmpty()) {
            StringBuilder fallback = new StringBuilder();
            if (Boolean.TRUE.equals(waste.getEnableAuxiliaryAccounting())
                    && waste.getAuxUnitPrice() != null
                    && StrUtil.isNotBlank(waste.getAuxUnit())) {
                fallback.append(waste.getAuxUnitPrice().stripTrailingZeros().toPlainString()).append("元/").append(waste.getAuxUnit());
            } else if (waste.getBaseUnitPrice() != null) {
                fallback.append(waste.getBaseUnitPrice().stripTrailingZeros().toPlainString()).append("元/吨");
            }
            return fallback.length() > 0 ? fallback.toString() : "-";
        }
        String pricingPlan = waste.getPricingPlan().trim();
        StringBuilder amountBuilder = new StringBuilder();
        amountBuilder.append(pricingPlan).append("元/吨");
        if (Boolean.TRUE.equals(waste.getEnableAuxiliaryAccounting())
                && waste.getAuxUnitPrice() != null
                && StrUtil.isNotBlank(waste.getAuxUnit())) {
            amountBuilder.append("\n")
                    .append(waste.getAuxUnitPrice().stripTrailingZeros().toPlainString())
                    .append("元/")
                    .append(waste.getAuxUnit());
        } else if (waste.getBaseUnitPrice() != null) {
            if (amountBuilder.length() == 0) {
                amountBuilder.append(waste.getBaseUnitPrice().stripTrailingZeros().toPlainString()).append("元/吨");
            }
        }
        return amountBuilder.toString();
    }

    private static String buildPackageAmountCell(String parentPricingPlan, ContractDetailResponse.ContractWasteItemResponse waste) {
        String wastePricingPlan = waste != null ? waste.getPricingPlan() : null;
        String displayPlan = StrUtil.isNotBlank(parentPricingPlan) ? parentPricingPlan
                : (StrUtil.isNotBlank(wastePricingPlan) ? wastePricingPlan.trim() : null);

        if (displayPlan == null) {
            return "-";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(displayPlan);

        boolean auxEnabled = waste != null && Boolean.TRUE.equals(waste.getEnableAuxiliaryAccounting());
        builder.append("\n");
        if (waste != null && waste.getBaseUnitPrice() != null) {
            builder.append(waste.getBaseUnitPrice().stripTrailingZeros().toPlainString()).append("元/吨");
        } else if (waste != null && StrUtil.isNotBlank(waste.getPricingPlan())) {
            builder.append(waste.getPricingPlan().trim()).append("元/吨");
        } else if (waste != null && waste.getBaseQuantity() != null) {
            builder.append(waste.getBaseQuantity().stripTrailingZeros().toPlainString()).append(" 吨");
        } else {
            builder.append("-");
        }

        if (auxEnabled) {
            builder.append("\n");
            if (waste.getAuxUnitPrice() != null) {
                builder.append(waste.getAuxUnitPrice().stripTrailingZeros().toPlainString()).append("元/");
                builder.append(StrUtil.isNotBlank(waste.getAuxUnit()) ? waste.getAuxUnit() : "-");
            } else {
                builder.append("-").append("元/").append(StrUtil.isNotBlank(waste.getAuxUnit()) ? waste.getAuxUnit() : "-");
            }
        }

        return builder.toString();
    }

    private static void createOutOfScopeServicesTable(MainDocumentPart documentPart, ContractDetailResponse detail) {
        if (detail == null || detail.getOutOfScopeServices() == null || detail.getOutOfScopeServices().isEmpty()) {
            return;
        }

        Tbl table = Docx4jHelper.createTable(documentPart);

        Tr header = Docx4jHelper.createTableRow(table);
        Docx4jHelper.setCellText(Docx4jHelper.createTableCell(header), "序号");
        Docx4jHelper.setCellText(Docx4jHelper.createTableCell(header), "项目");
        Docx4jHelper.setCellText(Docx4jHelper.createTableCell(header), "规格型号");
        Docx4jHelper.setCellText(Docx4jHelper.createTableCell(header), "单位");
        Docx4jHelper.setCellText(Docx4jHelper.createTableCell(header), "计划数量");
        Docx4jHelper.setCellText(Docx4jHelper.createTableCell(header), "合同单价");

        int[] widthRatios = {2, 5, 5, 2, 4, 4};
        Docx4jHelper.setColumnWidthsByRatios(table, widthRatios, documentPart);

        int idx = 1;
        for (com.erp.controller.contract.dto.QuotationDetailResponse.OutOfScopeServiceResponse svc : detail.getOutOfScopeServices()) {
            Tr row = Docx4jHelper.createTableRow(table);
            Docx4jHelper.setCellText(Docx4jHelper.createTableCell(row), String.valueOf(idx++));
            Docx4jHelper.setCellText(Docx4jHelper.createTableCell(row), defaultString(svc.getProject()));
            Docx4jHelper.setCellText(Docx4jHelper.createTableCell(row), defaultString(svc.getSpec()));
            Docx4jHelper.setCellText(Docx4jHelper.createTableCell(row), defaultString(svc.getUnit()));
            String planned = "-";
            if (svc.getPlannedQuantity() != null) {
                planned = svc.getPlannedQuantity().stripTrailingZeros().toPlainString();
            }
            Docx4jHelper.setCellText(Docx4jHelper.createTableCell(row), planned);

            String contractPrice = "-";
            if (svc.getContractUnitPrice() != null) {
                contractPrice = svc.getContractUnitPrice().stripTrailingZeros().toPlainString();
                if (StrUtil.isNotBlank(svc.getUnit())) {
                    contractPrice = contractPrice + "元/" + svc.getUnit();
                } else {
                    contractPrice = contractPrice + "元";
                }
            }
            Docx4jHelper.setCellText(Docx4jHelper.createTableCell(row), contractPrice);
        }
    }

    private static String buildSubtotalLabel(String subtotalJson) {
        if (StrUtil.isBlank(subtotalJson)) {
            return "/";
        }
        try {
            JSONArray jsonArray = JSONUtil.parseArray(subtotalJson);
            if (jsonArray == null || jsonArray.isEmpty()) {
                return "/";
            }
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String unit = jsonObject.getStr("unit");
                BigDecimal total = jsonObject.getBigDecimal("total");
                if (total != null && total.compareTo(BigDecimal.valueOf(-1)) == 0) {
                    if (StrUtil.isNotBlank(unit)) {
                        parts.add("不限量");
                    } else {
                        parts.add("不限量");
                    }
                } else {
                    String v = total != null ? total.stripTrailingZeros().toPlainString() : "0";
                    if (StrUtil.isNotBlank(unit)) {
                        v = v + " " + unit;
                    }
                    parts.add(v);
                }
            }
            List<String> deduped = new ArrayList<>();
            boolean sawUnlimited = false;
            for (String it : parts) {
                if (it != null && it.contains("不限量")) {
                    if (!sawUnlimited) {
                        deduped.add("不限量");
                        sawUnlimited = true;
                    }
                } else {
                    deduped.add(it);
                }
            }
            return String.join("、", deduped);
        } catch (Exception e) {
            log.warn("解析小计摘要JSON失败：{}", subtotalJson, e);
            return "/";
        }
    }

    private static String buildTotalLabel(List<String> subtotalJsonList) {
        if (subtotalJsonList == null || subtotalJsonList.isEmpty()) {
            return "不限量";
        }

        Map<String, BigDecimal> unitTotalMap = new HashMap<>();
        java.util.Set<String> unlimitedUnits = new java.util.LinkedHashSet<>();
        java.util.List<String> unitOrder = new ArrayList<>();

        try {
            for (String subtotalJson : subtotalJsonList) {
                if (StrUtil.isBlank(subtotalJson)) {
                    continue;
                }
                JSONArray jsonArray = JSONUtil.parseArray(subtotalJson);
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
            log.warn("解析小计JSON数据失败：{}", e.getMessage());
            return "不限量";
        }

        if (unitOrder.isEmpty()) {
            return "不限量";
        }

        List<String> parts = new ArrayList<>();
        for (String unit : unitOrder) {
            if (unlimitedUnits.contains(unit)) {
                parts.add("不限量");
            } else {
                BigDecimal val = unitTotalMap.getOrDefault(unit, BigDecimal.ZERO);
                parts.add(val.stripTrailingZeros().toPlainString() + " " + unit);
            }
        }
        return String.join("、", parts);
    }

    private static void createRemarkSection(MainDocumentPart documentPart, ContractDetailResponse detail) {
        String remark = buildRemarkContent(detail);
        if (StrUtil.isBlank(remark)) {
            return;
        }
        P paragraph = Docx4jHelper.createParagraph(documentPart);
        Docx4jHelper.setAlignment(paragraph, "LEFT");

        R labelRun = Docx4jHelper.createRun(paragraph);
        Docx4jHelper.setFontSize(labelRun, 11);
        Docx4jHelper.setBold(labelRun, true);
        Docx4jHelper.setText(labelRun, "备注：");

        String[] lines = remark.replace("\r\n", "\n").split("\n");
        R contentRun = Docx4jHelper.createRun(paragraph);
        Docx4jHelper.setFontSize(contentRun, 11);
        Docx4jHelper.setBold(contentRun, false);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (StrUtil.isBlank(line)) {
                continue;
            }
            if (i == 0) {
                Docx4jHelper.setText(contentRun, line.trim());
            } else {
                Docx4jHelper.addBreak(contentRun);
                Docx4jHelper.setText(contentRun, line.trim());
            }
        }
    }

    private static String buildRemarkContent(ContractDetailResponse detail) {
        String remark = detail.getRemark();
        if (remark == null) {
            remark = "";
        }
        remark = remark.trim();
        String servicePeriod = buildServicePeriod(detail);
        if (StrUtil.isNotBlank(servicePeriod) && StrUtil.isNotBlank(remark) && remark.contains("服务期限")) {
            try {
                remark = remark.replaceAll("(?s)服务期限.*?(?=\\r?\\n|$)", servicePeriod);
            } catch (Exception ignore) { }
        }
        boolean hasServicePeriod = false;
        if (StrUtil.isNotBlank(servicePeriod)) {
            hasServicePeriod = remark.contains("服务期限") || remark.contains(servicePeriod);
        }
        StringBuilder builder = new StringBuilder();
        if (StrUtil.isNotBlank(remark)) {
            builder.append(remark);
        }
        if (!hasServicePeriod && StrUtil.isNotBlank(servicePeriod)) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(servicePeriod);
        }
        return builder.toString();
    }

    private static String buildServicePeriod(ContractDetailResponse detail) {
        if (detail.getValidFrom() == null || detail.getValidTo() == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
        String validFromStr = detail.getValidFrom().format(formatter);
        String validToStr = detail.getValidTo().format(formatter);
        return "服务期限：" + validFromStr + "至" + validToStr;
    }

    private static void createSignatureSection(MainDocumentPart documentPart, ContractDetailResponse detail) {
        Tbl table = Docx4jHelper.createTable(documentPart);
        Docx4jHelper.setTableWidthToA4(table);
        Docx4jHelper.removeTableBorders(table);

        Tr row = Docx4jHelper.createTableRow(table);
        Tc cellA = Docx4jHelper.createTableCell(row);
        Tc cellB = Docx4jHelper.createTableCell(row);

        fillPartyASignatureCell(cellA);
        fillPartyBSignatureCell(cellB);
    }

    private static void fillPartyASignatureCell(Tc cell) {
        Docx4jHelper.setCellText(cell, "甲方(盖章):\n法定代表人(签字或盖章):\n业务联系人:\n联系电话:");
    }

    private static void fillPartyBSignatureCell(Tc cell) {
        Docx4jHelper.setCellText(cell, "乙方(盖章):\n法定代表人(签字或盖章):\n业务联系人:\n联系电话:\n业务咨询电话:");
    }

    private static String defaultString(Object obj) {
        return Objects.toString(obj, "-");
    }

    private static class RowData {
        private final String[] columns;

        private RowData(String[] columns) {
            this.columns = columns;
        }

        private static RowData detail(String[] columns) {
            return new RowData(columns);
        }

        private static RowData subtotal(String subtotalLabel) {
            String[] columns = new String[8];
            columns[0] = "小计";
            columns[1] = "";
            columns[2] = "";
            columns[3] = "";
            columns[4] = "";
            columns[5] = subtotalLabel;
            columns[6] = "/";
            columns[7] = "/";
            return new RowData(columns);
        }

        private static RowData total(String totalLabel) {
            String[] columns = new String[8];
            columns[0] = "合计";
            columns[1] = "";
            columns[2] = "";
            columns[3] = "";
            columns[4] = "";
            columns[5] = totalLabel;
            columns[6] = "/";
            columns[7] = "/";
            return new RowData(columns);
        }

        private String[] getColumns() {
            return columns;
        }
    }
}
