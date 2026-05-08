package com.erp.util;

import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;

import java.math.BigInteger;

/**
 * Docx4j工具类，封装常用的Word文档操作
 */
public class Docx4jHelper {

    private static final ObjectFactory factory;
    static {
        try {
            factory = Context.getWmlObjectFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize ObjectFactory", e);
        }
    }

    private Docx4jHelper() {
    }

    /**
     * 创建Word文档包
     *
     * @return WordprocessingMLPackage
     * @throws Exception 创建异常
     */
    public static WordprocessingMLPackage createPackage() throws Exception {
        return WordprocessingMLPackage.createPackage();
    }

    /**
     * 创建段落
     *
     * @param documentPart 文档部分
     * @return 段落对象
     */
    public static P createParagraph(MainDocumentPart documentPart) {
        P paragraph = factory.createP();
        documentPart.addObject(paragraph);
        return paragraph;
    }

    /**
     * 创建文本运行
     *
     * @param paragraph 段落
     * @return 文本运行对象
     */
    public static R createRun(P paragraph) {
        R run = factory.createR();
        RPr runProperties = factory.createRPr();
        run.setRPr(runProperties);
        paragraph.getContent().add(run);
        return run;
    }

    /**
     * 设置文本内容
     *
     * @param run  文本运行
     * @param text 文本内容
     */
    public static void setText(R run, String text) {
        Text textObj = factory.createText();
        textObj.setValue(text);
        run.getContent().add(textObj);
    }

    /**
     * 设置加粗
     *
     * @param run  文本运行
     * @param bold 是否加粗
     */
    public static void setBold(R run, boolean bold) {
        RPr rpr = run.getRPr();
        if (rpr == null) {
            rpr = factory.createRPr();
            run.setRPr(rpr);
        }
        BooleanDefaultTrue b = factory.createBooleanDefaultTrue();
        b.setVal(bold);
        rpr.setB(b);
    }

    /**
     * 设置字号
     *
     * @param run     文本运行
     * @param fontSize 字号（单位：半磅，如20号字体=40半磅）
     */
    public static void setFontSize(R run, int fontSize) {
        RPr rpr = run.getRPr();
        if (rpr == null) {
            rpr = factory.createRPr();
            run.setRPr(rpr);
        }
        HpsMeasure size = factory.createHpsMeasure();
        size.setVal(BigInteger.valueOf(fontSize * 2)); // 转换为半磅
        rpr.setSz(size);
    }

    /**
     * 设置字体颜色
     *
     * @param run  文本运行
     * @param color 颜色值（如"000000"表示黑色）
     */
    public static void setColor(R run, String color) {
        RPr rpr = run.getRPr();
        if (rpr == null) {
            rpr = factory.createRPr();
            run.setRPr(rpr);
        }
        Color colorObj = factory.createColor();
        colorObj.setVal(color);
        rpr.setColor(colorObj);
    }

    /**
     * 设置段落对齐方式
     *
     * @param paragraph  段落
     * @param alignment 对齐方式（LEFT, CENTER, RIGHT, JUSTIFY）
     */
    public static void setAlignment(P paragraph, String alignment) {
        PPr ppr = factory.createPPr();
        Jc jc = factory.createJc();
        switch (alignment.toUpperCase()) {
            case "CENTER":
                jc.setVal(JcEnumeration.CENTER);
                break;
            case "RIGHT":
                jc.setVal(JcEnumeration.RIGHT);
                break;
            case "JUSTIFY":
                jc.setVal(JcEnumeration.BOTH);
                break;
            case "LEFT":
            default:
                jc.setVal(JcEnumeration.LEFT);
                break;
        }
        ppr.setJc(jc);
        paragraph.setPPr(ppr);
    }

    /**
     * 添加换行符
     *
     * @param run 文本运行
     */
    public static void addBreak(R run) {
        Br br = factory.createBr();
        run.getContent().add(br);
    }

    /**
     * 创建表格
     *
     * @param documentPart 文档部分
     * @return 表格对象
     */
    public static Tbl createTable(MainDocumentPart documentPart) {
        Tbl table = factory.createTbl();
        documentPart.addObject(table);
        return table;
    }

    /**
     * 从文档中查找第一个表格（模板表格）
     *
     * @param documentPart 文档部分
     * @return 第一个表格，如果没有则返回null
     */
    public static Tbl findFirstTable(MainDocumentPart documentPart) {
        if (documentPart == null) {
            return null;
        }
        try {
            java.util.List<Object> tables = documentPart.getJAXBNodesViaXPath("//w:tbl", true);
            if (tables != null && !tables.isEmpty()) {
                Object first = tables.get(0);
                if (first instanceof Tbl) {
                    return (Tbl) first;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * 从文档中查找所有表格
     *
     * @param documentPart 文档部分
     * @return 表格列表
     */
    public static java.util.List<Tbl> findAllTables(MainDocumentPart documentPart) {
        java.util.List<Tbl> result = new java.util.ArrayList<>();
        if (documentPart == null) {
            return result;
        }
        try {
            java.util.List<Object> tables = documentPart.getJAXBNodesViaXPath("//w:tbl", true);
            if (tables != null) {
                for (Object obj : tables) {
                    if (obj instanceof Tbl) {
                        result.add((Tbl) obj);
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    /**
     * 创建表格行
     *
     * @param table 表格
     * @return 表格行对象
     */
    public static Tr createTableRow(Tbl table) {
        Tr row = factory.createTr();
        table.getContent().add(row);
        return row;
    }

    /**
     * 创建表格单元格
     *
     * @param row 表格行
     * @return 表格单元格对象
     */
    public static Tc createTableCell(Tr row) {
        Tc cell = factory.createTc();
        row.getContent().add(cell);
        return cell;
    }

    /**
     * 设置单元格文本
     *
     * @param cell 单元格
     * @param text 文本内容
     */
    public static void setCellText(Tc cell, String text) {
        // 清空单元格内容
        cell.getContent().clear();
        
        P paragraph = factory.createP();
        cell.getContent().add(paragraph);
        
        if (text != null && text.contains("\n")) {
            // 包含换行符，需要分段处理
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                R run = factory.createR();
                Text textObj = factory.createText();
                textObj.setValue(lines[i]);
                run.getContent().add(textObj);
                paragraph.getContent().add(run);
                
                if (i < lines.length - 1) {
                    // 添加换行符
                    Br br = factory.createBr();
                    R breakRun = factory.createR();
                    breakRun.getContent().add(br);
                    paragraph.getContent().add(breakRun);
                }
            }
        } else {
            // 不包含换行符，直接设置文本
            R run = factory.createR();
            Text textObj = factory.createText();
            textObj.setValue(text != null ? text : "");
            run.getContent().add(textObj);
            paragraph.getContent().add(run);
        }
    }

    /**
     * 设置单元格文本（带样式）
     *
     * @param cell     单元格
     * @param text     文本内容
     * @param fontSize 字号
     */
    public static void setCellTextWithStyle(Tc cell, String text, int fontSize) {
        setCellText(cell, text);
        // 设置段落中的字体大小
        if (!cell.getContent().isEmpty() && cell.getContent().get(0) instanceof P) {
            P paragraph = (P) cell.getContent().get(0);
            if (!paragraph.getContent().isEmpty() && paragraph.getContent().get(0) instanceof R) {
                R run = (R) paragraph.getContent().get(0);
                setFontSize(run, fontSize);
            }
        }
    }

    /**
     * 设置表格宽度为页面宽度（100%）
     *
     * @param table 表格
     */
    public static void setTableFullWidth(Tbl table) {
        TblPr tblPr = factory.createTblPr();
        TblWidth tblWidth = factory.createTblWidth();
        tblWidth.setType("pct"); // 百分比类型
        tblWidth.setW(BigInteger.valueOf(5000)); // 5000 表示 100%
        tblPr.setTblW(tblWidth);
        table.setTblPr(tblPr);
    }

    /**
     * 为单元格设置宽度
     *
     * @param cell 单元格
     * @param type 宽度类型（例如 "dxa" 或 "pct"）
     * @param w    宽度值
     */
    public static void setCellWidth(Tc cell, String type, BigInteger w) {
        TcPr tcPr = cell.getTcPr();
        if (tcPr == null) {
            tcPr = factory.createTcPr();
            cell.setTcPr(tcPr);
        }
        TblWidth tcW = factory.createTblWidth();
        tcW.setType(type);
        tcW.setW(w);
        tcPr.setTcW(tcW);
    }

    /**
     * 从模板表格获取列宽数组
     *
     * @param templateTable 模板表格（从中读取列宽）
     * @return 列宽数组（twips），如果无法获取则返回null
     */
    public static long[] getColumnWidthsFromTemplate(Tbl templateTable) {
        if (templateTable == null || templateTable.getContent() == null || templateTable.getContent().isEmpty()) {
            return null;
        }

        // 获取第一行（表头）
        Object firstRowObj = templateTable.getContent().get(0);
        if (!(firstRowObj instanceof Tr)) {
            return null;
        }
        Tr headerRow = (Tr) firstRowObj;

        // 统计列数
        int colCount = 0;
        for (Object obj : headerRow.getContent()) {
            if (obj instanceof Tc) {
                colCount++;
            }
        }

        if (colCount == 0) {
            return null;
        }

        long[] widths = new long[colCount];

        // 读取 tblGrid 中的列宽（优先使用）
        if (templateTable.getTblGrid() != null && templateTable.getTblGrid().getGridCol() != null) {
            java.util.List<org.docx4j.wml.TblGridCol> gridCols = templateTable.getTblGrid().getGridCol();
            for (int i = 0; i < colCount && i < gridCols.size(); i++) {
                if (gridCols.get(i).getW() != null) {
                    widths[i] = gridCols.get(i).getW().longValue();
                }
            }
        }

        // 如果 tblGrid 为空或部分列宽为0，读取单元格宽度
        int idx = 0;
        for (Object obj : headerRow.getContent()) {
            if (obj instanceof Tc && idx < colCount) {
                Tc cell = (Tc) obj;
                TcPr tcPr = cell.getTcPr();
                if (tcPr != null && tcPr.getTcW() != null && tcPr.getTcW().getW() != null) {
                    if (widths[idx] == 0) {
                        widths[idx] = tcPr.getTcW().getW().longValue();
                    }
                }
            }
            idx++;
        }

        return widths;
    }

    /**
     * 根据模板表格的列宽设置目标表格的列宽（设置表头行单元格的宽度）
     *
     * @param targetTable      目标表格
     * @param columnWidths     列宽数组（twips），如果为null则平均分配
     * @param documentPart     文档部分（用于计算页面可用宽度）
     */
    public static void applyColumnWidthsFromTemplate(Tbl targetTable, long[] columnWidths, MainDocumentPart documentPart) {
        if (targetTable == null) {
            return;
        }

        int cols = columnWidths != null ? columnWidths.length : 0;
        if (cols == 0) {
            // 没有指定列宽，使用平均分配
            if (documentPart != null) {
                setEqualColumnWidths(targetTable, 8, documentPart); // 默认8列
            }
            return;
        }

        // 计算总宽度
        long totalWidth = 0;
        for (long w : columnWidths) {
            totalWidth += w;
        }

        // 确保表格有 tblPr
        TblPr tblPr = targetTable.getTblPr();
        if (tblPr == null) {
            tblPr = factory.createTblPr();
            targetTable.setTblPr(tblPr);
        }

        // 设置表格总宽度
        try {
            TblWidth tableWidth = factory.createTblWidth();
            tableWidth.setType("dxa");
            tableWidth.setW(BigInteger.valueOf(totalWidth));
            tblPr.setTblW(tableWidth);
        } catch (Exception ignore) {
        }

        // 设置 tblGrid
        TblGrid tblGrid = factory.createTblGrid();
        for (long w : columnWidths) {
            TblGridCol gridCol = factory.createTblGridCol();
            gridCol.setW(BigInteger.valueOf(Math.max(1, w)));
            tblGrid.getGridCol().add(gridCol);
        }
        try {
            targetTable.setTblGrid(tblGrid);
        } catch (Exception ignore) {
        }

        // 设置表头行单元格的宽度
        if (targetTable.getContent() == null || targetTable.getContent().isEmpty()) {
            return;
        }
        Object firstRowObj = targetTable.getContent().get(0);
        if (!(firstRowObj instanceof Tr)) {
            return;
        }
        Tr headerRow = (Tr) firstRowObj;
        int idx = 0;
        for (Object obj : headerRow.getContent()) {
            if (obj instanceof Tc && idx < cols) {
                Tc cell = (Tc) obj;
                setCellWidth(cell, "dxa", BigInteger.valueOf(Math.max(1, columnWidths[idx])));
                idx++;
            }
        }
    }

    /**
     * 根据列宽比例设置表格列宽
     *
     * @param targetTable      目标表格
     * @param widthRatios      列宽比例数组（例如 [1, 2, 1] 表示第一列1份，第二列2份，第三列1份）
     * @param documentPart      文档部分（用于计算页面可用宽度）
     */
    public static void setColumnWidthsByRatios(Tbl targetTable, int[] widthRatios, MainDocumentPart documentPart) {
        if (targetTable == null || widthRatios == null || widthRatios.length == 0) {
            return;
        }

        // 计算可用页面宽度
        long a4Twips = 11906L;
        long pageWidthTwips = a4Twips;
        long leftMargin = 0L;
        long rightMargin = 0L;
        try {
            if (documentPart != null && documentPart.getJaxbElement() != null && documentPart.getJaxbElement().getBody() != null) {
                org.docx4j.wml.SectPr sectPr = documentPart.getJaxbElement().getBody().getSectPr();
                if (sectPr != null) {
                    if (sectPr.getPgSz() != null && sectPr.getPgSz().getW() != null) {
                        pageWidthTwips = sectPr.getPgSz().getW().longValue();
                    }
                    if (sectPr.getPgMar() != null) {
                        if (sectPr.getPgMar().getLeft() != null) leftMargin = sectPr.getPgMar().getLeft().longValue();
                        if (sectPr.getPgMar().getRight() != null) rightMargin = sectPr.getPgMar().getRight().longValue();
                    }
                }
            }
        } catch (Exception ignore) {
        }

        long availableTwips = pageWidthTwips - (leftMargin + rightMargin);
        if (availableTwips <= 0) {
            availableTwips = a4Twips;
        }

        // 计算总比例
        int totalRatio = 0;
        for (int r : widthRatios) {
            totalRatio += r;
        }

        // 计算每列宽度
        long[] columnWidths = new long[widthRatios.length];
        long baseWidth = availableTwips / totalRatio;
        int remainder = (int) (availableTwips % totalRatio);

        for (int i = 0; i < widthRatios.length; i++) {
            columnWidths[i] = baseWidth * widthRatios[i];
            // 分配余数
            if (remainder > 0) {
                columnWidths[i]++;
                remainder--;
            }
        }

        // 应用列宽
        applyColumnWidthsFromTemplate(targetTable, columnWidths, documentPart);
    }

    /**
     * 将表格按列数平均分配列宽（生成 tblGrid 并为表头单元格设置宽度）
     * 要求：在创建完表头行并生成相应的单元格后调用
     *
     * @param table 表格
     * @param cols  列数
     */
    public static void setEqualColumnWidths(Tbl table, int cols, MainDocumentPart documentPart) {
        if (cols <= 0) return;
        // 确保表为固定布局
        TblPr tblPr = table.getTblPr();
        if (tblPr == null) {
            tblPr = factory.createTblPr();
            table.setTblPr(tblPr);
        }
        // 为兼容不同 docx4j 版本，未使用 TblLayout（部分版本无此工厂方法）。
        // 通过设置 tblGrid 与单元格宽度来保证布局稳定。

        // 计算可用页面宽度（twips） = 页面宽度 - 左右页边距
        long a4Twips = 11906L;
        long pageWidthTwips = a4Twips;
        long leftMargin = 0L;
        long rightMargin = 0L;
        try {
            if (documentPart != null && documentPart.getJaxbElement() != null && documentPart.getJaxbElement().getBody() != null) {
                org.docx4j.wml.SectPr sectPr = documentPart.getJaxbElement().getBody().getSectPr();
                if (sectPr != null) {
                    if (sectPr.getPgSz() != null && sectPr.getPgSz().getW() != null) {
                        pageWidthTwips = sectPr.getPgSz().getW().longValue();
                    }
                    if (sectPr.getPgMar() != null) {
                        if (sectPr.getPgMar().getLeft() != null) leftMargin = sectPr.getPgMar().getLeft().longValue();
                        if (sectPr.getPgMar().getRight() != null) rightMargin = sectPr.getPgMar().getRight().longValue();
                    }
                }
            }
        } catch (Exception ignore) {
        }
        long availableTwips = pageWidthTwips - (leftMargin + rightMargin);
        if (availableTwips <= 0) {
            availableTwips = a4Twips;
        }
        long base = Math.max(1L, availableTwips / cols);
        long rem = availableTwips - base * cols; // remainder (>=0)
        TblGrid tblGrid = factory.createTblGrid();
        long[] colWidths = new long[cols];
        for (int i = 0; i < cols; i++) {
            long w = base + (i < rem ? 1L : 0L);
            colWidths[i] = w;
            TblGridCol gridCol = factory.createTblGridCol();
            gridCol.setW(BigInteger.valueOf(w));
            tblGrid.getGridCol().add(gridCol);
        }
        try {
            table.setTblGrid(tblGrid);
        } catch (Exception ignore) {
        }

        // 同时设置表的绝对总宽（dxa 单位，twips），总宽 = availableTwips（使用可用页面宽度更精确）
        try {
            TblWidth tableWidth = factory.createTblWidth();
            tableWidth.setType("dxa");
            tableWidth.setW(BigInteger.valueOf(availableTwips));
            tblPr.setTblW(tableWidth);
            table.setTblPr(tblPr);
        } catch (Exception ignore) {
        }

        // 为表头已创建的单元格设置单元格宽（dxa 单位，twips）
        if (table.getContent() == null || table.getContent().isEmpty()) {
            return;
        }
        Object firstRowObj = table.getContent().get(0);
        if (!(firstRowObj instanceof Tr)) {
            return;
        }
        Tr header = (Tr) firstRowObj;
        int assigned = 0;
        for (Object obj : header.getContent()) {
            if (obj instanceof Tc) {
                Tc cell = (Tc) obj;
                long w = (assigned < colWidths.length) ? colWidths[assigned] : base;
                setCellWidth(cell, "dxa", BigInteger.valueOf(w));
                assigned++;
                if (assigned >= cols) break;
            }
        }
    }

    /**
     * 设置表格宽度为A4纸宽度
     *
     * @param table 表格
     */
    public static void setTableWidthToA4(Tbl table) {
        TblPr tblPr = factory.createTblPr();
        TblWidth tblWidth = factory.createTblWidth();
        tblWidth.setType("pct"); // twips 单位
        // A4纸宽度：11906 twips (8.27英寸 * 1440 twips/inch)
        tblWidth.setW(BigInteger.valueOf(5000));
        tblPr.setTblW(tblWidth);
        table.setTblPr(tblPr);
    }

    /**
     * 在文档中添加若干空行（用于表格间间隔）
     *
     * @param documentPart 文档主体
     * @param lines        空行数
     */
    public static void addBlankLines(MainDocumentPart documentPart, int lines) {
        if (lines <= 0 || documentPart == null) return;
        for (int i = 0; i < lines; i++) {
            P p = factory.createP();
            documentPart.addObject(p);
            // 添加一个换行运行以确保段落高度显示为空行
            R run = factory.createR();
            Br br = factory.createBr();
            run.getContent().add(br);
            p.getContent().add(run);
        }
    }

    /**
     * 设置单元格合并跨度（GridSpan）
     *
     * @param cell     单元格
     * @param span     跨越的列数
     */
    public static void setCellGridSpan(Tc cell, int span) {
        TcPr tcPr = cell.getTcPr();
        if (tcPr == null) {
            tcPr = factory.createTcPr();
            cell.setTcPr(tcPr);
        }
        TcPrInner.GridSpan gridSpan = factory.createTcPrInnerGridSpan();
        gridSpan.setVal(BigInteger.valueOf(span));
        tcPr.setGridSpan(gridSpan);
    }

    /**
     * 合并单元格（水平合并）
     *
     * @param table   表格
     * @param row     行索引
     * @param fromCol 起始列索引
     * @param toCol   结束列索引
     */
    public static void mergeCellsHorizontally(Tbl table, int row, int fromCol, int toCol) {
        Tr tableRow = (Tr) table.getContent().get(row);
        
        for (int colIndex = fromCol; colIndex <= toCol; colIndex++) {
            Tc cell = (Tc) tableRow.getContent().get(colIndex);
            TcPr tcPr = cell.getTcPr();
            if (tcPr == null) {
                tcPr = factory.createTcPr();
                cell.setTcPr(tcPr);
            }
            
            if (colIndex == fromCol) {
                // 起始单元格：设置合并跨度
                TcPrInner.GridSpan gridSpan = factory.createTcPrInnerGridSpan();
                gridSpan.setVal(BigInteger.valueOf(toCol - fromCol + 1));
                tcPr.setGridSpan(gridSpan);
            } else {
                // 被合并的单元格：清空内容，设置HMerge为CONTINUE
                cell.getContent().clear();
                TcPrInner.HMerge hMerge = factory.createTcPrInnerHMerge();
                hMerge.setVal("continue"); // 字符串常量
                tcPr.setHMerge(hMerge);
            }
        }
    }

    /**
     * 移除表格边框
     *
     * @param table 表格
     */
    public static void removeTableBorders(Tbl table) {
        TblPr tblPr = table.getTblPr();
        if (tblPr == null) {
            tblPr = factory.createTblPr();
            table.setTblPr(tblPr);
        }

        TblBorders borders = factory.createTblBorders();
        CTBorder noneBorder = factory.createCTBorder();
        noneBorder.setVal(STBorder.NONE);
        noneBorder.setSz(BigInteger.valueOf(0));

        borders.setTop(noneBorder);
        borders.setBottom(noneBorder);
        borders.setLeft(noneBorder);
        borders.setRight(noneBorder);
        borders.setInsideH(noneBorder);
        borders.setInsideV(noneBorder);

        tblPr.setTblBorders(borders);
    }

    /**
     * 设置单元格是否禁止换行
     *
     * @param cell   单元格
     * @param noWrap true=禁止换行，false=允许换行
     */
    public static void setCellNoWrap(Tc cell, boolean noWrap) {
        TcPr tcPr = cell.getTcPr();
        if (tcPr == null) {
            tcPr = factory.createTcPr();
            cell.setTcPr(tcPr);
        }
        if (noWrap) {
            // 禁止换行：创建并设置 noWrap 属性
            BooleanDefaultTrue noWrapValue = factory.createBooleanDefaultTrue();
            tcPr.setNoWrap(noWrapValue);
        } else {
            // 允许换行：移除 noWrap 属性
            tcPr.setNoWrap(null);
        }
    }

    /**
     * 设置单元格水平对齐方式（文本对齐），同时设置垂直居中
     *
     * @param cell      单元格
     * @param alignment 对齐方式（LEFT, CENTER, RIGHT, JUSTIFY）
     */
    public static void setCellHorizontalAlignment(Tc cell, String alignment) {
        // 水平对齐：通过设置单元格内段落的对齐方式来实现文本对齐
        JcEnumeration jcEnum = JcEnumeration.LEFT;
        switch (alignment.toUpperCase()) {
            case "CENTER":
                jcEnum = JcEnumeration.CENTER;
                break;
            case "RIGHT":
                jcEnum = JcEnumeration.RIGHT;
                break;
            case "JUSTIFY":
                jcEnum = JcEnumeration.BOTH;
                break;
            case "LEFT":
            default:
                jcEnum = JcEnumeration.LEFT;
                break;
        }

        // 如果单元格为空，先创建段落
        if (cell.getContent().isEmpty()) {
            P paragraph = factory.createP();
            cell.getContent().add(paragraph);
        }

        // 设置所有段落的对齐方式
        for (Object obj : cell.getContent()) {
            if (obj instanceof P) {
                P paragraph = (P) obj;
                PPr ppr = paragraph.getPPr();
                if (ppr == null) {
                    ppr = factory.createPPr();
                    paragraph.setPPr(ppr);
                }
                Jc jc = factory.createJc();
                jc.setVal(jcEnum);
                ppr.setJc(jc);
            }
        }

        // 同时设置垂直居中
        setCellVerticalAlignment(cell, "CENTER");
    }

    /**
     * 设置单元格垂直对齐方式
     *
     * @param cell      单元格
     * @param alignment 垂直对齐方式（TOP, CENTER, BOTTOM）
     */
    public static void setCellVerticalAlignment(Tc cell, String alignment) {
        TcPr tcPr = cell.getTcPr();
        if (tcPr == null) {
            tcPr = factory.createTcPr();
            cell.setTcPr(tcPr);
        }

        org.docx4j.wml.CTVerticalJc vAlign = factory.createCTVerticalJc();
        switch (alignment.toUpperCase()) {
            case "TOP":
                vAlign.setVal(org.docx4j.wml.STVerticalJc.TOP);
                break;
            case "BOTTOM":
                vAlign.setVal(org.docx4j.wml.STVerticalJc.BOTTOM);
                break;
            case "CENTER":
            default:
                vAlign.setVal(org.docx4j.wml.STVerticalJc.CENTER);
                break;
        }
        tcPr.setVAlign(vAlign);
    }

    /**
     * 设置单元格水平和垂直居中（常用组合）
     *
     * @param cell 单元格
     */
    public static void setCellCenter(Tc cell) {
        setCellHorizontalAlignment(cell, "CENTER");
        setCellVerticalAlignment(cell, "CENTER");
    }
}

