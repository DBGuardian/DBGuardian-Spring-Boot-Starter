package com.erp.service.production.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.production.dto.ImportPdfTransferManifestResponse;
import com.erp.controller.production.dto.ImportTransferManifestResponse;
import com.erp.controller.production.dto.TransferManifestExportRow;
import com.erp.controller.production.dto.TransferManifestItemResponse;
import com.erp.controller.production.dto.TransferManifestListResponse;
import com.erp.controller.production.dto.TransferManifestPageRequest;
import com.erp.controller.production.dto.TransferManifestPageResponse;
import com.erp.controller.production.dto.PdfImportTaskResult;
import com.erp.entity.common.File;
import com.erp.entity.production.TransferManifest;
import com.erp.entity.production.TransferManifestItem;
import com.erp.mapper.common.FileMapper;
import com.erp.mapper.production.TransferManifestItemMapper;
import com.erp.mapper.production.TransferManifestMapper;
import com.erp.service.common.FileService;
import com.erp.service.production.TransferManifestService;
import com.erp.service.production.ITransferManifestPdfAsyncService;
import com.erp.service.production.ITransferManifestPdfTaskService;
import com.erp.service.production.ITransferManifestPdfImportCoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;

/**
 * 转移联单服务实现
 */
@Slf4j
@Service
public class TransferManifestServiceImpl implements TransferManifestService {

    @Autowired
    private TransferManifestMapper transferManifestMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private TransferManifestItemMapper transferManifestItemMapper;

    @Autowired
    private ITransferManifestPdfAsyncService transferManifestPdfAsyncService;

    @Autowired
    private ITransferManifestPdfTaskService transferManifestPdfTaskService;

    @Autowired
    private ITransferManifestPdfImportCoreService transferManifestPdfImportCoreService;

    @Override
    public String submitPdfImportTask(MultipartFile file, Integer uploaderId) {
        byte[] fileBytes;
        String originalFilename;
        try {
            fileBytes = file.getBytes();
            originalFilename = file.getOriginalFilename();
        } catch (Exception e) {
            throw new RuntimeException("读取 PDF 文件失败：" + e.getMessage(), e);
        }

        String taskId = transferManifestPdfTaskService.createTask();
        transferManifestPdfAsyncService.executePdfImportAsync(taskId, fileBytes, originalFilename, uploaderId);
        return taskId;
    }

    @Override
    public PdfImportTaskResult getPdfImportTaskResult(String taskId) {
        return transferManifestPdfTaskService.getTaskResult(taskId);
    }

    @Override
    public Integer getPdfFileId(Integer manifestId) {
        if (manifestId == null) {
            return null;
        }
        TransferManifest manifest = transferManifestMapper.selectById(manifestId);
        return manifest == null ? null : manifest.getPdfFileId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer uploadPdf(Integer manifestId, MultipartFile file) {
        if (manifestId == null) {
            throw new RuntimeException("联单编号不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传 PDF 文件");
        }

        TransferManifest manifest = transferManifestMapper.selectById(manifestId);
        if (manifest == null || Integer.valueOf(1).equals(manifest.getIsDeleted())) {
            throw new RuntimeException("转移联单不存在");
        }

        Integer oldPdfFileId = manifest.getPdfFileId();
        Integer newPdfFileId = null;
        try {
            com.erp.entity.common.File uploadedFile = fileService.uploadAndSave(file, "TRANSFER_MANIFEST", manifestId);
            newPdfFileId = uploadedFile.getFileId();
            manifest.setPdfFileId(newPdfFileId);
            manifest.setUpdateTime(LocalDateTime.now());
            int updated = transferManifestMapper.updateById(manifest);
            if (updated <= 0) {
                throw new RuntimeException("更新联单 PDF 文件编号失败");
            }
            return newPdfFileId;
        } catch (Exception e) {
            if (newPdfFileId != null) {
                try {
                    fileService.deleteFile(newPdfFileId);
                } catch (Exception deleteEx) {
                    log.error("单条上传 PDF 回滚删除新文件失败：manifestId={}, fileId={}", manifestId, newPdfFileId, deleteEx);
                }
            }
            manifest.setPdfFileId(oldPdfFileId);
            throw new RuntimeException("上传 PDF 失败：" + e.getMessage(), e);
        }
    }

    @Override
    public TransferManifestListResponse getPage(TransferManifestPageRequest request) {
        int pageNum  = request.getPage()  != null ? request.getPage()  : 1;
        int pageSize = request.getSize()  != null ? request.getSize()  : 20;

        // 排序字段白名单，防止 SQL 注入
        String sortField = null;
        String sortOrder = null;
        Set<String> allowedSortFields = new HashSet<>(Arrays.asList(
                "联单编号", "广东省联单号", "国家联单号", "产生单位", "接收单位",
                "计划转移时间", "当前阶段", "是否作废", "创建时间"));
        if (request.getSortField() != null && allowedSortFields.contains(request.getSortField())) {
            sortField = request.getSortField();
            sortOrder = "desc".equalsIgnoreCase(request.getSortOrder()) ? "desc" : "asc";
        }

        Page<TransferManifestPageResponse> page = new Page<>(pageNum, pageSize);
        IPage<TransferManifestPageResponse> iPage = transferManifestMapper.selectManifestPage(
                page,
                request.get广东省联单号(),
                request.get国家联单号(),
                request.get产生单位(),
                request.get接收单位(),
                request.get车牌号(),
                request.get当前阶段(),
                request.get计划转移开始(),
                request.get计划转移结束(),
                sortField,
                sortOrder
        );

        List<TransferManifestPageResponse> records = iPage.getRecords();

        // 批量加载子项，避免 N+1
        if (records != null && !records.isEmpty()) {
            List<Integer> manifestIds = records.stream()
                    .map(TransferManifestPageResponse::get联单编号)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<TransferManifestItem> allItems = transferManifestItemMapper.selectByManifestIds(manifestIds);

            // 按联单编号分组
            Map<Integer, List<TransferManifestItem>> itemMap = allItems.stream()
                    .collect(Collectors.groupingBy(TransferManifestItem::getManifestId));

            // 填充子项到对应的主表行
            for (TransferManifestPageResponse row : records) {
                List<TransferManifestItem> items = itemMap.getOrDefault(row.get联单编号(), Collections.emptyList());
                row.set废物子项(items.stream().map(this::toItemResponse).collect(Collectors.toList()));
            }
        }

        TransferManifestListResponse response = new TransferManifestListResponse();
        response.setRecords(records);
        response.setTotal(iPage.getTotal());
        response.setCurrent((int) iPage.getCurrent());
        response.setSize((int) iPage.getSize());
        return response;
    }

    @Override
    public List<TransferManifestExportRow> getExportRows(List<Integer> manifestIds) {
        return transferManifestMapper.selectExportRows(manifestIds == null || manifestIds.isEmpty() ? null : manifestIds);
    }

    @Override
    public byte[] exportPdfZip(List<Integer> manifestIds) {
        List<TransferManifest> manifests;
        if (manifestIds == null || manifestIds.isEmpty()) {
            QueryWrapper<TransferManifest> wrapper = new QueryWrapper<>();
            wrapper.eq("是否删除", 0).orderByAsc("联单编号");
            manifests = transferManifestMapper.selectList(wrapper);
        } else {
            manifests = transferManifestMapper.selectBatchIds(manifestIds).stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !Integer.valueOf(1).equals(item.getIsDeleted()))
                    .sorted(Comparator.comparing(TransferManifest::getManifestId))
                    .collect(Collectors.toList());
        }

        if (manifests == null || manifests.isEmpty()) {
            return null;
        }

        Map<Integer, File> fileMap = fileMapper.selectBatchIds(
                        manifests.stream()
                                .map(TransferManifest::getPdfFileId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList()))
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(File::getFileId, item -> item, (a, b) -> a));

        if (fileMap.isEmpty()) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            Set<String> usedNames = new HashSet<>();
            int addedCount = 0;

            for (TransferManifest manifest : manifests) {
                Integer pdfFileId = manifest.getPdfFileId();
                if (pdfFileId == null) {
                    continue;
                }
                File file = fileMap.get(pdfFileId);
                if (file == null) {
                    continue;
                }

                byte[] fileBytes;
                try {
                    fileBytes = readStoredFileBytes(file);
                } catch (Exception ex) {
                    log.warn("读取联单 PDF 文件失败，已忽略：manifestId={}, fileId={}", manifest.getManifestId(), pdfFileId, ex);
                    continue;
                }
                if (fileBytes == null || fileBytes.length == 0) {
                    continue;
                }

                String entryName = buildZipEntryName(manifest, file, usedNames);
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                zipOutputStream.write(fileBytes);
                zipOutputStream.closeEntry();
                addedCount++;
            }

            if (addedCount == 0) {
                return null;
            }
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出 PDF ZIP 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将子表实体转为响应 DTO
     */
    private TransferManifestItemResponse toItemResponse(TransferManifestItem item) {
        TransferManifestItemResponse r = new TransferManifestItemResponse();
        r.set子项编号(item.getItemId());
        r.set联单编号(item.getManifestId());
        r.set废物类别(item.getWasteCategory());
        r.set废物代码(item.getWasteCode());
        r.set废物名称(item.getWasteName());
        r.set废物形态(item.getWasteForm());
        r.set包装方式(item.getPackagingMethod());
        r.set计划数量(item.getPlannedQuantity());
        r.set确认数量(item.getConfirmedQuantity());
        r.set计量单位(item.getUnit());
        return r;
    }

    private byte[] readStoredFileBytes(File file) {
        if (file == null) {
            return null;
        }
        if ("本地".equals(file.getStorageType())) {
            return fileService.getLocalFileStorageService().readFile(file.getLocalPath());
        }
        if (fileService.getOssFileStorageService() == null) {
            throw new RuntimeException("未配置云端文件存储服务");
        }
        return fileService.getOssFileStorageService().readFile(file.getObjectKey());
    }

    private String buildZipEntryName(TransferManifest manifest, File file, Set<String> usedNames) {
        String manifestNo = firstNonBlank(
                manifest.getGdManifestNo(),
                manifest.getNationalManifestNo()
        );
        String manifestIdText = manifest.getManifestId() == null ? "" : String.valueOf(manifest.getManifestId());
        String baseName = firstNonBlank(
                manifestNo.isEmpty() ? null : "转移联单-" + manifestNo,
                manifestIdText.isEmpty() ? null : "转移联单-联单编号" + manifestIdText,
                file.getFileId() == null ? null : "转移联单-文件编号" + file.getFileId(),
                "转移联单-PDF附件"
        );
        String extension = ".pdf";
        String originalName = file.getFileName();
        if (originalName != null) {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex);
            }
        }
        String safeBaseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String candidate = safeBaseName + extension;
        int index = 1;
        while (!usedNames.add(candidate)) {
            candidate = safeBaseName + "(" + index++ + ")" + extension;
        }
        return candidate;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    // =========================================================
    // Excel 列索引常量（仅作备用，实际解析改为按表头列名匹配）
    // =========================================================
    // （已废弃固定列索引方式，改为动态表头映射，见 parseExcel / parseRow）

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================
    // 内部临时数据结构
    // =========================================================

    /**
     * 解析后的单条行数据（包含主表+子表字段）
     */
    private static class ExcelRow {
        // 主表
        String gdManifestNo;
        String nationalManifestNo;
        String producer;
        String producerCity;
        String producerDistrict;
        String producerTown;
        String shipper;
        String receiver;
        String receivingUnit;
        String receivingProvince;
        String receivingCity;
        String receivingDistrict;
        String licenseNo;
        String receivingOpinion;
        LocalDate receivingDate;
        String disposalCategory;
        String disposalSubcategory;
        String licensePlate;
        String carrier;
        String transportUnit;
        LocalDateTime transportStartTime;
        LocalDateTime transportEndTime;
        LocalDate plannedTransferDate;
        String currentStage;
        String supplementType;
        Integer hasMajorDifference;
        String majorDifferenceDesc;
        String receivingRemark;
        Integer isVoided;
        // 子表
        String wasteCategory;
        String wasteCode;
        String wasteName;
        String wasteForm;
        String packagingMethod;
        BigDecimal plannedQuantity;
        BigDecimal confirmedQuantity;
        String unit;
        // 原始行号（1-based，含表头偏移）
        int rowNum;
    }

    // =========================================================
    // 公共接口实现
    // =========================================================

    /**
     * 批量导入转移联单（Excel）
     * <p>
     * 优化策略（减少数据库操作次数，避免 N+1）：
     * 1. 解析 Excel，按广东省联单号分组。
     * 2. 一次 IN 查询所有联单号是否已存在，冲突则整体回滚。
     * 3. 一次批量 INSERT 主表（useGeneratedKeys 回填 manifestId）。
     * 4. 一次批量 INSERT 所有子表记录。
     * 整个导入在同一事务内，任意失败全量回滚。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportTransferManifestResponse importFromExcel(MultipartFile file) {
        List<String> errorMessages = new ArrayList<>();

        // 1. 解析 Excel
        List<ExcelRow> rows;
        try {
            rows = parseExcel(file);
        } catch (Exception e) {
            log.error("Excel 文件解析失败", e);
            throw new RuntimeException("Excel 文件解析失败：" + e.getMessage(), e);
        }
        if (rows.isEmpty()) {
            return ImportTransferManifestResponse.builder()
                    .total(0).success(0).error(0)
                    .errorMessages(Collections.emptyList())
                    .build();
        }

        // 2. 按广东省联单号分组（保持原始顺序）
        Map<String, List<ExcelRow>> grouped = new LinkedHashMap<>();
        for (ExcelRow row : rows) {
            if (row.gdManifestNo == null || row.gdManifestNo.isEmpty()) {
                errorMessages.add("第 " + row.rowNum + " 行：广东省联单号为空，已跳过");
                continue;
            }
            grouped.computeIfAbsent(row.gdManifestNo, k -> new ArrayList<>()).add(row);
        }
        if (grouped.isEmpty()) {
            return ImportTransferManifestResponse.builder()
                    .total(0).success(0).error(0)
                    .errorMessages(errorMessages)
                    .build();
        }

        int totalManifests = grouped.size();
        List<String> allGdNos = new ArrayList<>(grouped.keySet());

        // 3. 一次查询：批量检测重复（1 次 DB 操作代替 N 次）
        List<String> existingNos = transferManifestMapper.selectExistingGdManifestNos(allGdNos);
        if (!existingNos.isEmpty()) {
            throw new RuntimeException(
                    "以下广东省联单号已存在，导入中止，所有数据已回滚：" + existingNos);
        }

        // 4. 构建主表实体列表（有序）
        List<TransferManifest> manifestList = new ArrayList<>(totalManifests);
        // 记录每条主表对应的原始行列表，便于后续构建子表
        List<List<ExcelRow>> manifestRowsList = new ArrayList<>(totalManifests);
        for (Map.Entry<String, List<ExcelRow>> entry : grouped.entrySet()) {
            manifestList.add(buildManifest(entry.getValue().get(0)));
            manifestRowsList.add(entry.getValue());
        }

        // 5. 一次批量 INSERT 主表（useGeneratedKeys 自动回填 manifestId）
        transferManifestMapper.batchInsert(manifestList);

        // 6. 构建子表实体列表（所有联单子项合并为一个大列表）
        List<TransferManifestItem> allItems = new ArrayList<>();
        for (int i = 0; i < manifestList.size(); i++) {
            Integer manifestId = manifestList.get(i).getManifestId();
            if (manifestId == null) {
                throw new RuntimeException("主表批量插入后未能回填 manifestId，导入中止，所有数据已回滚。");
            }
            for (ExcelRow itemRow : manifestRowsList.get(i)) {
                if (!isItemRowEmpty(itemRow)) {
                    allItems.add(buildItem(manifestId, itemRow));
                }
            }
        }

        // 7. 一次批量 INSERT 子表（所有子项合并，1 次 DB 操作）
        if (!allItems.isEmpty()) {
            transferManifestItemMapper.batchInsert(allItems);
        }

        return ImportTransferManifestResponse.builder()
                .total(totalManifests)
                .success(totalManifests)
                .error(0)
                .errorMessages(errorMessages)
                .build();
    }

    // =========================================================
    // 私有辅助方法
    // =========================================================

    /**
     * 读取并解析 Excel 文件，返回所有数据行（不含标题行）。
     * 采用按表头列名动态匹配策略：读取第0行表头，建立「列名→列索引」映射，
     * 从第1行起按列名取值，与列顺序无关。
     * 支持的列名与数据库字段名完全一致（含别名映射）。
     */
    private List<ExcelRow> parseExcel(MultipartFile file) throws Exception {
        List<ExcelRow> rows = new ArrayList<>();
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        try (InputStream is = file.getInputStream();
             Workbook workbook = filename.endsWith(".xlsx") ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            if (lastRow < 1) return rows;

            // 读取第0行表头，建立「列名（trim后）→列索引」映射
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> colIndex = new HashMap<>();
            if (headerRow != null) {
                for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
                    Cell cell = headerRow.getCell(c);
                    if (cell != null) {
                        String name = getCellStringValue(cell);
                        if (name != null && !name.trim().isEmpty()) {
                            colIndex.put(name.trim(), c);
                        }
                    }
                }
            }
            log.debug("Excel 表头列名映射：{}", colIndex);

            // 从第1行开始读取数据行
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowBlank(row)) continue;
                rows.add(parseRow(row, i + 1, colIndex));
            }
        }
        return rows;
    }

    /**
     * 按表头列名映射解析一行数据。
     * colIndex：表头列名 → 列索引，由 parseExcel 构建。
     * 列名与数据库字段名保持一致；同时支持部分常见别名。
     */
    private ExcelRow parseRow(Row row, int rowNum, Map<String, Integer> colIndex) {
        ExcelRow r = new ExcelRow();
        r.rowNum             = rowNum;
        // 主表字段（列名与 SQL 字段名一一对应）
        r.gdManifestNo       = col(row, colIndex, "广东省联单号");
        r.nationalManifestNo = col(row, colIndex, "国家联单号");
        r.producer           = col(row, colIndex, "产生单位");
        r.producerCity       = col(row, colIndex, "产废单位所属市");
        r.producerDistrict   = col(row, colIndex, "产废单位所属区");
        // Excel 中可能使用「产废单位所属区（县/镇）」作为列名
        if (r.producerDistrict == null) r.producerDistrict = col(row, colIndex, "产废单位所属区（县/镇）");
        r.producerTown       = col(row, colIndex, "产废单位所属镇");
        // Excel 中可能使用「产废单位所属镇（街道）」作为列名
        if (r.producerTown == null) r.producerTown = col(row, colIndex, "产废单位所属镇（街道）");
        r.shipper            = col(row, colIndex, "发运人");
        r.receiver           = col(row, colIndex, "接收人");
        r.receivingUnit      = col(row, colIndex, "接收单位");
        r.receivingProvince  = col(row, colIndex, "接收单位所属省");
        r.receivingCity      = col(row, colIndex, "接收单位所属市");
        r.receivingDistrict  = col(row, colIndex, "接收单位所属区");
        // Excel 中可能使用「接收单位所属区（县/镇）」作为列名
        if (r.receivingDistrict == null) r.receivingDistrict = col(row, colIndex, "接收单位所属区（县/镇）");
        r.licenseNo          = col(row, colIndex, "许可证编号");
        r.receivingOpinion   = col(row, colIndex, "接收单位处理意见");
        r.receivingDate      = colDate(row, colIndex, "接收日期");
        r.disposalCategory   = col(row, colIndex, "处置方式大类");
        r.disposalSubcategory= col(row, colIndex, "处置方式小类");
        r.licensePlate       = col(row, colIndex, "车牌号");
        r.carrier            = col(row, colIndex, "承运人");
        r.transportUnit      = col(row, colIndex, "运输单位");
        r.transportStartTime = colDateTime(row, colIndex, "运输开始时间");
        r.transportEndTime   = colDateTime(row, colIndex, "运输结束时间");
        r.plannedTransferDate= colDate(row, colIndex, "计划转移时间");
        r.currentStage       = col(row, colIndex, "当前阶段");
        r.supplementType     = col(row, colIndex, "补录类型");
        r.hasMajorDifference = parseBooleanAsInt(col(row, colIndex, "是否存在重大差异"));
        r.majorDifferenceDesc= col(row, colIndex, "重大差异简述");
        r.receivingRemark    = col(row, colIndex, "接收企业备注");
        r.isVoided           = parseBooleanAsInt(col(row, colIndex, "是否作废"));
        // 子表字段
        r.wasteCategory      = col(row, colIndex, "废物类别");
        r.wasteCode          = col(row, colIndex, "废物代码");
        r.wasteName          = col(row, colIndex, "废物名称");
        r.wasteForm          = col(row, colIndex, "废物形态");
        r.packagingMethod    = col(row, colIndex, "包装方式");
        r.plannedQuantity    = colDecimal(row, colIndex, "计划数量");
        r.confirmedQuantity  = colDecimal(row, colIndex, "确认数量");
        r.unit               = col(row, colIndex, "计量单位");
        return r;
    }

    /** 按列名取字符串值，列名不存在时返回 null */
    private String col(Row row, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null) return null;
        return getStringValue(row, idx);
    }

    /** 按列名取 LocalDate */
    private LocalDate colDate(Row row, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null) return null;
        return getLocalDateByIndex(row, idx);
    }

    /** 按列名取 LocalDateTime */
    private LocalDateTime colDateTime(Row row, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null) return null;
        return getLocalDateTimeByIndex(row, idx);
    }

    /** 按列名取 BigDecimal */
    private BigDecimal colDecimal(Row row, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null) return null;
        return getBigDecimalByIndex(row, idx);
    }

    /**
     * 从 ExcelRow 构建主表实体
     */
    private TransferManifest buildManifest(ExcelRow r) {
        TransferManifest m = new TransferManifest();
        m.setGdManifestNo(r.gdManifestNo);
        m.setNationalManifestNo(r.nationalManifestNo);
        m.setProducer(r.producer);
        m.setProducerCity(r.producerCity);
        m.setProducerDistrict(r.producerDistrict);
        m.setProducerTown(r.producerTown);
        m.setShipper(r.shipper);
        m.setReceiver(r.receiver);
        m.setReceivingUnit(r.receivingUnit);
        m.setReceivingProvince(r.receivingProvince);
        m.setReceivingCity(r.receivingCity);
        m.setReceivingDistrict(r.receivingDistrict);
        m.setLicenseNo(r.licenseNo);
        m.setReceivingOpinion(r.receivingOpinion);
        m.setReceivingDate(r.receivingDate);
        m.setDisposalCategory(r.disposalCategory);
        m.setDisposalSubcategory(r.disposalSubcategory);
        m.setLicensePlate(r.licensePlate);
        m.setCarrier(r.carrier);
        m.setTransportUnit(r.transportUnit);
        m.setTransportStartTime(r.transportStartTime);
        m.setTransportEndTime(r.transportEndTime);
        m.setPlannedTransferDate(r.plannedTransferDate);
        m.setCurrentStage(r.currentStage);
        m.setSupplementType(r.supplementType);
        m.setHasMajorDifference(r.hasMajorDifference != null ? r.hasMajorDifference : 0);
        m.setMajorDifferenceDesc(r.majorDifferenceDesc);
        m.setReceivingRemark(r.receivingRemark);
        m.setIsVoided(r.isVoided != null ? r.isVoided : 0);
        m.setIsDeleted(0);
        return m;
    }

    /**
     * 从 ExcelRow 构建子表实体
     */
    private TransferManifestItem buildItem(Integer manifestId, ExcelRow r) {
        TransferManifestItem item = new TransferManifestItem();
        item.setManifestId(manifestId);
        item.setWasteCategory(r.wasteCategory);
        item.setWasteCode(r.wasteCode);
        item.setWasteName(r.wasteName);
        item.setWasteForm(r.wasteForm);
        item.setPackagingMethod(r.packagingMethod);
        item.setPlannedQuantity(r.plannedQuantity);
        item.setConfirmedQuantity(r.confirmedQuantity);
        item.setUnit(r.unit);
        item.setIsDeleted(0);
        return item;
    }

    /**
     * 判断子项行的关键字段是否全为空（若全空则跳过子项插入）
     */
    private boolean isItemRowEmpty(ExcelRow r) {
        return isEmpty(r.wasteCategory)
                && isEmpty(r.wasteCode)
                && isEmpty(r.wasteName)
                && r.plannedQuantity == null
                && r.confirmedQuantity == null;
    }

    /**
     * 判断 POI 行是否为完全空行
     */
    private boolean isRowBlank(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    // =========================================================
    // 单元格值读取工具方法
    // =========================================================

    private String getStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        String val = getCellStringValue(cell);
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 日期型单元格，转为字符串
                    Date d = cell.getDateCellValue();
                    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
                }
                // 防止数值被格式化为科学计数法
                double dVal = cell.getNumericCellValue();
                if (dVal == Math.floor(dVal)) {
                    return String.valueOf((long) dVal);
                }
                return String.valueOf(dVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private LocalDate getLocalDate(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String str = getStringValue(row, colIndex);
        if (str == null) return null;
        // 尝试多种日期格式
        String[] fmts = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd", "yyyyMMdd"};
        for (String fmt : fmts) {
            try {
                return LocalDate.parse(str.length() > 10 ? str.substring(0, 10) : str,
                        DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {}
        }
        log.warn("无法解析日期字符串：{}", str);
        return null;
    }

    private LocalDateTime getLocalDateTime(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        String str = getStringValue(row, colIndex);
        if (str == null) return null;
        String[] fmts = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm"};
        for (String fmt : fmts) {
            try {
                return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(fmt));
            } catch (Exception ignored) {}
        }
        // 尝试只有日期部分
        LocalDate ld = getLocalDate(row, colIndex);
        return ld != null ? ld.atStartOfDay() : null;
    }

    private BigDecimal getBigDecimal(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String str = getStringValue(row, colIndex);
        if (str == null) return null;
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            log.warn("无法解析数值字符串：{}", str);
            return null;
        }
    }

    private Integer parseBooleanAsInt(String val) {
        if (val == null) return 0;
        String v = val.trim().toLowerCase();
        return (v.equals("是") || v.equals("1") || v.equals("true") || v.equals("yes")) ? 1 : 0;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ---- ByIndex 别名，供动态列名映射方法调用 ----
    private LocalDate getLocalDateByIndex(Row row, int colIndex) {
        return getLocalDate(row, colIndex);
    }

    private LocalDateTime getLocalDateTimeByIndex(Row row, int colIndex) {
        return getLocalDateTime(row, colIndex);
    }

    private BigDecimal getBigDecimalByIndex(Row row, int colIndex) {
        return getBigDecimal(row, colIndex);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPdfTransferManifestResponse importFromPdf(MultipartFile file, Integer uploaderId) {
        return transferManifestPdfImportCoreService.importFromPdf(file, uploaderId);
    }
}
