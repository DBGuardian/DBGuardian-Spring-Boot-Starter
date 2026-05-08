package com.erp.service.system.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.system.dto.HazardousWasteCreateRequest;
import com.erp.controller.system.dto.HazardousWasteDetailResponse;
import com.erp.controller.system.dto.HazardousWasteImportError;
import com.erp.controller.system.dto.HazardousWasteImportResponse;
import com.erp.controller.system.dto.HazardousWastePageRequest;
import com.erp.controller.system.dto.HazardousWastePageResponse;
import com.erp.controller.system.dto.HazardousWasteUpdateRequest;
import com.erp.entity.system.HazardousWasteCategory;
import com.erp.entity.system.HazardousWasteItem;
import com.erp.mapper.system.HazardousWasteCategoryMapper;
import com.erp.mapper.system.HazardousWasteItemMapper;
import com.erp.mapper.system.domain.HazardousWasteReferenceStat;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.service.system.HazardousWasteItemService;
import com.erp.service.auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 危险废物名录业务实现
 */
@Slf4j
@Service
public class HazardousWasteItemServiceImpl implements HazardousWasteItemService {

    private static final Set<String> SUPPORTED_CHARACTERISTICS;

    static {
        Set<String> temp = new HashSet<>();
        temp.add("IN");
        temp.add("T");
        temp.add("C");
        temp.add("I");
        temp.add("R");
        SUPPORTED_CHARACTERISTICS = Collections.unmodifiableSet(temp);
    }

    @Autowired
    private HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private HazardousWasteCategoryMapper hazardousWasteCategoryMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private AuthService authService;
    @Override
    public IPage<HazardousWastePageResponse> getWasteItemPage(HazardousWastePageRequest request) {
        // 危废条目是公共基础数据，所有用户都可以查看，不进行数据范围过滤
        // 注意：危废条目作为国家危废名录/固废目录的基础数据，应该对所有用户可见

        Page<HazardousWasteItem> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<HazardousWasteItem> entityPage = hazardousWasteItemMapper.selectWasteItemPage(
                page,
                normalize(request.getKeyword()),
                normalize(request.getHazardCharacteristic()),
                normalize(request.getWasteCategory()),
                normalize(request.getWasteCategoryName()),
                normalize(request.getIndustrySource()),
                normalize(request.getWasteCode()),
                normalize(request.getWasteName()),
                request.getAvailable(),
                null,  // 危废条目不做创建人过滤，所有用户可见
                request.getOrderBy(),
                request.getOrderDirection()
        );
        List<HazardousWasteItem> records = entityPage.getRecords();
        Map<Integer, HazardousWasteReferenceStat> statMap = buildReferenceStatMap(records);
        List<HazardousWastePageResponse> responses = records.stream()
                .map(item -> convertToPageResponse(item, statMap.get(item.getItemId())))
                .collect(Collectors.toList());
        Page<HazardousWastePageResponse> responsePage =
                new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        responsePage.setRecords(responses);
        return responsePage;
    }

    @Override
    public HazardousWasteDetailResponse getWasteItemDetail(Integer itemId) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        
        HazardousWasteItem item = hazardousWasteItemMapper.selectDetailById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "危废条目不存在");
        }
        
        // 应用数据范围控制（viewScope），与列表查询保持一致
        if (!admin) {
            // 获取当前员工对"危废条目表"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:危废条目表:页面");
            
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // 仅查看自己创建的危废条目
                if (!Objects.equals(item.getCreatorId(), currentUserId)) {
                    throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "无权查看该危废条目");
                }
            }
            // 如果是 ALL 或没有配置，允许查看所有危废条目
        }
        
        Map<Integer, HazardousWasteReferenceStat> statMap = buildReferenceStatMap(Collections.singletonList(item));
        return convertToDetailResponse(item, statMap.get(item.getItemId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createWasteItem(HazardousWasteCreateRequest request) {
        HazardousWasteItem existed = hazardousWasteItemMapper.selectByWasteCode(request.getWasteCode().trim());
        if (existed != null) {
            throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "废物代码已存在");
        }
        String normalizedCategory = requireAndNormalize(request.getWasteCategory(), "废物类别不能为空");
        String normalizedCategoryName = resolveCategoryName(request.getWasteCategoryName(), normalizedCategory);
        HazardousWasteCategory category = ensureAndGetCategory(normalizedCategory, normalizedCategoryName);
        HazardousWasteItem item = buildEntityFromRequest(category,
                request.getIndustrySource(), request.getWasteCode(), request.getWasteName(), request.getHazardCharacteristic(), request.getAvailable());
        // 设置创建人编码
        Integer creatorId = SecurityUtil.getCurrentUserId();
        item.setCreatorId(creatorId);
        hazardousWasteItemMapper.insertWithCategory(item);
        log.info("创建危废条目成功：itemId={}", item.getItemId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWasteItem(Integer itemId, HazardousWasteUpdateRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        HazardousWasteItem existed = hazardousWasteItemMapper.selectDetailById(itemId);
        if (existed == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "危废条目不存在");
        }

        // 应用操作范围控制（operateScope）
        if (!admin) {
            // 获取当前员工对"危废条目表"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:危废条目表:页面");
            
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    // 仅能操作自己创建的危废条目
                    if (!Objects.equals(existed.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能编辑自己创建的危废条目");
                    }
                }
            }
            // 如果是 ALL 或没有配置，不添加限制
        }

        String newCode = request.getWasteCode().trim();
        if (!StrUtil.equalsIgnoreCase(newCode, existed.getWasteCode())) {
            HazardousWasteItem duplicated = hazardousWasteItemMapper.selectByWasteCode(newCode);
            if (duplicated != null && !Objects.equals(duplicated.getItemId(), itemId)) {
                throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "废物代码已存在");
            }
        }
        String normalizedCategory = requireAndNormalize(request.getWasteCategory(), "废物类别不能为空");
        String normalizedCategoryName = resolveCategoryName(request.getWasteCategoryName(), normalizedCategory);
        HazardousWasteCategory category = ensureAndGetCategory(normalizedCategory, normalizedCategoryName);
        HazardousWasteItem update = buildEntityFromRequest(category,
                request.getIndustrySource(), request.getWasteCode(), request.getWasteName(), request.getHazardCharacteristic(), request.getAvailable());
        update.setItemId(itemId);
        // 继承BaseEntity后，MyBatis-Plus会自动处理乐观锁，无需手动设置version
        int rows = hazardousWasteItemMapper.updateByIdWithCategory(update);
        if (rows == 0) {
            throw new BusinessException("更新危废条目失败：记录已被其他用户修改");
        }
        log.info("更新危废条目成功：itemId={}", itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWasteItem(Integer itemId) {
        HazardousWasteItem existed = hazardousWasteItemMapper.selectDetailById(itemId);
        if (existed == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "危废条目不存在");
        }
        Map<Integer, HazardousWasteReferenceStat> statMap =
                buildReferenceStatMap(Collections.singletonList(existed));
        HazardousWasteReferenceStat stat = statMap.get(itemId);
        long totalReferences = 0L;
        if (stat != null) {
            totalReferences = safeValue(stat.getCustomerCount())
                    + safeValue(stat.getQuotationCount())
                    + safeValue(stat.getWarehousingCount())
                    + safeValue(stat.getStockCount());
        }
        if (totalReferences > 0) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(),
                    "该危废条目已被业务数据引用，无法删除");
        }
        Long sameCategoryCount = null;
        Integer categoryId = existed.getCategoryId();
        if (categoryId != null) {
            sameCategoryCount = hazardousWasteItemMapper.countByCategoryId(categoryId);
        }
        int delRows = hazardousWasteItemMapper.deleteById(itemId);
        if (delRows == 0) {
            throw new BusinessException("删除危废条目失败：记录不存在或已被删除");
        }
        if (categoryId != null && sameCategoryCount != null && sameCategoryCount <= 1) {
            // 删除末级条目后同步清理无引用的废物类别，避免孤儿数据
            hazardousWasteCategoryMapper.deleteById(categoryId);
            log.info("废物类别无其他引用，已同步删除：categoryId={}", categoryId);
        }
        log.info("删除危废条目成功：itemId={}", itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteWasteItems(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要删除的条目");
        }

        // 1. 批量查询所有要删除的条目（一次查询）
        List<HazardousWasteItem> existedList = hazardousWasteItemMapper.selectDetailByIds(ids);

        if (CollectionUtils.isEmpty(existedList)) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "危废条目不存在");
        }

        // 检查是否有不存在的ID
        Set<Integer> existedIds = existedList.stream()
                .map(HazardousWasteItem::getItemId)
                .collect(Collectors.toSet());
        List<Integer> notFoundIds = ids.stream()
                .filter(id -> !existedIds.contains(id))
                .collect(Collectors.toList());
        if (!notFoundIds.isEmpty()) {
            log.warn("部分ID不存在：{}", notFoundIds);
        }

        // 2. 批量查询引用统计（一次查询）
        Map<Integer, HazardousWasteReferenceStat> statMap = buildReferenceStatMap(existedList);

        // 3. 检查是否有被引用的条目
        List<String> referencedItems = new ArrayList<>();
        long totalReferences = 0L;
        for (HazardousWasteItem item : existedList) {
            HazardousWasteReferenceStat stat = statMap.get(item.getItemId());
            if (stat != null) {
                long itemReferences = safeValue(stat.getCustomerCount())
                        + safeValue(stat.getQuotationCount())
                        + safeValue(stat.getWarehousingCount())
                        + safeValue(stat.getStockCount());
                if (itemReferences > 0) {
                    referencedItems.add(item.getWasteCode() + "(" + item.getWasteName() + ")");
                }
                totalReferences += itemReferences;
            }
        }

        if (!referencedItems.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(),
                    "以下危废条目已被业务数据引用，无法删除：" + String.join("、", referencedItems));
        }

        // 4. 收集需要清理的废物类别（类别下只有待删除条目）
        Map<Integer, Long> categoryItemCount = existedList.stream()
                .filter(item -> item.getCategoryId() != null)
                .collect(Collectors.groupingBy(HazardousWasteItem::getCategoryId, Collectors.counting()));

        List<Integer> categoriesToDelete = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : categoryItemCount.entrySet()) {
            Integer categoryId = entry.getKey();
            Long deleteCount = entry.getValue();
            Long totalCount = hazardousWasteItemMapper.countByCategoryId(categoryId);
            if (totalCount != null && totalCount.equals(deleteCount)) {
                // 该类别下所有条目都要被删除
                categoriesToDelete.add(categoryId);
            }
        }

        // 5. 批量删除危废条目（一次删除）
        int deleteCount = hazardousWasteItemMapper.deleteBatch(ids);
        log.info("批量删除危废条目成功：请求数量={}，实际删除={}", ids.size(), deleteCount);

        // 6. 批量清理无引用的废物类别（一次删除）
        if (!categoriesToDelete.isEmpty()) {
            int categoryDeleteCount = hazardousWasteCategoryMapper.deleteBatch(categoriesToDelete);
            log.info("批量删除无引用废物类别成功：类别数量={}，实际删除={}", categoriesToDelete.size(), categoryDeleteCount);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HazardousWasteImportResponse importWasteItems(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请上传Excel文件");
        }
        HazardousWasteImportResponse response = new HazardousWasteImportResponse();
        List<HazardousWasteItem> preparedList = new ArrayList<>();
        Map<String, Integer> codeRowMap = new HashMap<>();
        Set<String> seenCodes = new HashSet<>();
        List<HazardousWasteImportError> errors = response.getErrors();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Excel中没有可用的Sheet");
            }
            int lastRow = sheet.getLastRowNum();
            int processed = 0;
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                processed++;
                int rowNumber = i + 1;
                try {
                    HazardousWasteItem item = buildEntityFromRow(row);
                    if (item == null) {
                        continue;
                    }
                    String code = item.getWasteCode();
                    if (seenCodes.contains(code)) {
                        addImportError(errors, rowNumber, code, "Excel中废物代码重复");
                        continue;
                    }
                    seenCodes.add(code);
                    codeRowMap.put(code, rowNumber);
                    preparedList.add(item);
                } catch (BusinessException ex) {
                    addImportError(errors, rowNumber, null, ex.getMessage());
                }
            }
            response.setTotalCount(processed);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入危废条目失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "导入失败：" + e.getMessage());
        }

        if (!preparedList.isEmpty()) {
            List<String> codes = preparedList.stream()
                    .map(HazardousWasteItem::getWasteCode)
                    .distinct()
                    .collect(Collectors.toList());
            List<String> existedCodes = hazardousWasteItemMapper.selectExistingWasteCodes(codes);
            if (!CollectionUtils.isEmpty(existedCodes)) {
                for (String code : existedCodes) {
                    Integer rowNumber = codeRowMap.getOrDefault(code, -1);
                    if (rowNumber != -1) {
                        addImportError(errors, rowNumber, code, "废物代码已存在于系统");
                    }
                }
                preparedList.removeIf(item -> existedCodes.contains(item.getWasteCode()));
            }
        }

        if (!preparedList.isEmpty()) {
            Map<String, String> categoryNameMap = preparedList.stream()
                    .filter(item -> StrUtil.isNotBlank(item.getWasteCategory()))
                    .collect(Collectors.toMap(
                            item -> item.getWasteCategory().trim(),
                            item -> resolveCategoryName(item.getWasteCategoryName(), item.getWasteCategory().trim()),
                            (existing, replacement) -> existing));
            Map<String, HazardousWasteCategory> categoryMap = ensureAndGetCategoryMap(categoryNameMap);
            for (HazardousWasteItem item : preparedList) {
                String categoryKey = normalize(item.getWasteCategory());
                HazardousWasteCategory category = categoryMap.get(categoryKey);
                if (category == null) {
                    Integer rowNumber = codeRowMap.getOrDefault(item.getWasteCode(), -1);
                    addImportError(errors, rowNumber == -1 ? 0 : rowNumber, item.getWasteCode(), "废物类别不存在");
                    item.setCategoryId(null);
                    continue;
                }
                item.setCategoryId(category.getCategoryId());
                item.setWasteCategory(category.getWasteCategory());
                item.setWasteCategoryName(category.getWasteCategoryName());
            }
            preparedList.removeIf(item -> item.getCategoryId() == null);
        }

        if (!preparedList.isEmpty()) {
            // 批量设置创建人编码
            Integer creatorId = SecurityUtil.getCurrentUserId();
            for (HazardousWasteItem item : preparedList) {
                item.setCreatorId(creatorId);
            }
            hazardousWasteItemMapper.insertBatch(preparedList);
            log.info("批量导入危废条目成功，count={}", preparedList.size());
        }
        response.setSuccessCount(preparedList.size());
        response.setFailCount(response.getErrors().size());
        return response;
    }

    @Override
    public List<HazardousWasteDetailResponse> listForExport(HazardousWastePageRequest request) {
        // 危废条目是公共基础数据，所有用户都可以查看和导出，不进行数据范围过滤

        List<HazardousWasteItem> list = hazardousWasteItemMapper.selectWasteItemList(
                normalize(request.getKeyword()),
                normalize(request.getHazardCharacteristic()),
                normalize(request.getWasteCategory()),
                normalize(request.getWasteCategoryName()),
                normalize(request.getIndustrySource()),
                normalize(request.getWasteCode()),
                normalize(request.getWasteName()),
                request.getAvailable(),
                null,  // 危废条目不做创建人过滤，所有用户可见
                request.getOrderBy(),
                request.getOrderDirection()
        );
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Integer, HazardousWasteReferenceStat> statMap = buildReferenceStatMap(list);
        return list.stream()
                .map(item -> convertToDetailResponse(item, statMap.get(item.getItemId())))
                .collect(Collectors.toList());
    }

    private HazardousWasteItem buildEntityFromRequest(HazardousWasteCategory category,
                                                      String industrySource,
                                                      String wasteCode,
                                                      String wasteName,
                                                      String hazardCharacteristic,
                                                      Boolean available) {
        if (category == null || category.getCategoryId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "废物类别无效");
        }
        HazardousWasteItem item = new HazardousWasteItem();
        item.setCategoryId(category.getCategoryId());
        item.setWasteCategory(category.getWasteCategory());
        item.setWasteCategoryName(category.getWasteCategoryName());
        item.setIndustrySource(requireAndNormalize(industrySource, "行业来源不能为空"));
        item.setWasteCode(requireAndNormalize(wasteCode, "废物代码不能为空"));
        item.setWasteName(requireAndNormalize(wasteName, "危险废物名称不能为空"));
        item.setHazardCharacteristic(normalizeCharacteristic(hazardCharacteristic));
        item.setAvailable(available == null ? Boolean.FALSE : available);
        return item;
    }

    private HazardousWasteItem buildEntityFromRow(Row row) {
        String wasteCategory = readCellString(row.getCell(0));
        String wasteCategoryName = readCellString(row.getCell(1));
        String industrySource = readCellString(row.getCell(2));
        String wasteCode = readCellString(row.getCell(3));
        String wasteName = readCellString(row.getCell(4));
        String hazardCharacteristic = readCellString(row.getCell(5));

        HazardousWasteItem item = new HazardousWasteItem();
        item.setWasteCategory(requireAndNormalize(wasteCategory, "废物类别不能为空"));
        item.setWasteCategoryName(resolveCategoryName(wasteCategoryName, wasteCategory));
        item.setIndustrySource(requireAndNormalize(industrySource, "行业来源不能为空"));
        item.setWasteCode(requireAndNormalize(wasteCode, "废物代码不能为空"));
        item.setWasteName(requireAndNormalize(wasteName, "危险废物名称不能为空"));
        item.setHazardCharacteristic(normalizeCharacteristic(hazardCharacteristic));
        item.setAvailable(Boolean.TRUE);
        return item;
    }

    private Map<Integer, HazardousWasteReferenceStat> buildReferenceStatMap(List<HazardousWasteItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyMap();
        }
        List<Integer> itemIds = items.stream()
                .map(HazardousWasteItem::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(itemIds)) {
            return Collections.emptyMap();
        }
        List<HazardousWasteReferenceStat> stats = hazardousWasteItemMapper.selectReferenceStats(itemIds);
        if (CollectionUtils.isEmpty(stats)) {
            return Collections.emptyMap();
        }
        return stats.stream().collect(Collectors.toMap(HazardousWasteReferenceStat::getItemId, stat -> stat));
    }

    private HazardousWastePageResponse convertToPageResponse(HazardousWasteItem item,
                                                             HazardousWasteReferenceStat stat) {
        HazardousWastePageResponse response = new HazardousWastePageResponse();
        response.setItemId(item.getItemId());
        response.setWasteCategory(item.getWasteCategory());
        response.setWasteCategoryName(item.getWasteCategoryName());
        response.setIndustrySource(item.getIndustrySource());
        response.setWasteCode(item.getWasteCode());
        response.setWasteName(item.getWasteName());
        response.setHazardCharacteristic(item.getHazardCharacteristic());
        response.setAvailable(item.getAvailable());
        // 创建人编码（操作人ID）
        response.setCreatorId(item.getCreatorId());
        // 创建人姓名（通过 EMPLOYEE 连表获取）
        response.setCreateUserName(item.getCreatorName());
        if (stat != null) {
            response.setCustomerCount(safeValue(stat.getCustomerCount()));
            response.setQuotationCount(safeValue(stat.getQuotationCount()));
            response.setWarehousingCount(safeValue(stat.getWarehousingCount()));
            response.setStockCount(safeValue(stat.getStockCount()));
        }
        return response;
    }

    private HazardousWasteDetailResponse convertToDetailResponse(HazardousWasteItem item,
                                                                 HazardousWasteReferenceStat stat) {
        HazardousWasteDetailResponse response = new HazardousWasteDetailResponse();
        response.setItemId(item.getItemId());
        response.setWasteCategory(item.getWasteCategory());
        response.setWasteCategoryName(item.getWasteCategoryName());
        response.setIndustrySource(item.getIndustrySource());
        response.setWasteCode(item.getWasteCode());
        response.setWasteName(item.getWasteName());
        response.setHazardCharacteristic(item.getHazardCharacteristic());
        response.setAvailable(item.getAvailable());
        if (stat != null) {
            response.setCustomerCount(safeValue(stat.getCustomerCount()));
            response.setQuotationCount(safeValue(stat.getQuotationCount()));
            response.setWarehousingCount(safeValue(stat.getWarehousingCount()));
            response.setStockCount(safeValue(stat.getStockCount()));
        }
        return response;
    }

    private String normalizeCharacteristic(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String cleaned = value.replace("，", ",")
                .replace("、", ",")
                .replace("/", ",")
                .replace("|", ",")
                .replace("\\", ",");
        List<String> tokens = StrUtil.split(cleaned, ',');
        if (CollUtil.isEmpty(tokens)) {
            return null;
        }
        List<String> normalizedCodes = new ArrayList<>();
        for (String token : tokens) {
            String upper = StrUtil.trim(token).toUpperCase();
            if (StrUtil.isBlank(upper)) {
                continue;
            }
            if (!SUPPORTED_CHARACTERISTICS.contains(upper)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                        "危险特性取值仅支持: In/T/C/I/R");
            }
            normalizedCodes.add(upper);
        }
        if (normalizedCodes.isEmpty()) {
            return null;
        }
        return String.join("/", normalizedCodes);
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i <= 5; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && StrUtil.isNotBlank(readCellString(cell))) {
                return false;
            }
        }
        return true;
    }

    private void addImportError(List<HazardousWasteImportError> errors, int rowNumber, String wasteCode, String message) {
        HazardousWasteImportError error = new HazardousWasteImportError();
        error.setRowIndex(rowNumber);
        error.setWasteCode(wasteCode);
        error.setMessage(message);
        errors.add(error);
    }

    private String readCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return StrUtil.trimToNull(cell.toString());
    }

    private long safeValue(Long value) {
        return value == null ? 0L : value;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private String requireAndNormalize(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), message);
        }
        return normalized;
    }

    private void ensureWasteCategoryExists(Map<String, String> categoryNameMap) {
        if (CollectionUtils.isEmpty(categoryNameMap)) {
            return;
        }
        List<HazardousWasteCategory> existedList =
                hazardousWasteCategoryMapper.selectByWasteCategories(new ArrayList<>(categoryNameMap.keySet()));
        Set<String> existedNames = CollectionUtils.isEmpty(existedList)
                ? Collections.emptySet()
                : existedList.stream()
                .map(HazardousWasteCategory::getWasteCategory)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
        List<HazardousWasteCategory> toInsert = new ArrayList<>();
        // 统一获取当前登录用户，用于设置创建人编码
        Integer creatorId = SecurityUtil.getCurrentUserId();
        for (Map.Entry<String, String> entry : categoryNameMap.entrySet()) {
            String name = entry.getKey();
            if (StrUtil.isBlank(name)) {
                continue;
            }
            String trimmed = name.trim();
            if (existedNames.contains(trimmed)) {
                continue;
            }
            HazardousWasteCategory category = new HazardousWasteCategory();
            category.setWasteCategory(trimmed);
            category.setWasteCategoryName(resolveCategoryName(entry.getValue(), trimmed));
            // 设置创建人编码为当前登录用户
            category.setCreatorId(creatorId);
            // 新增时：限额默认 999999，开始时间为当前时间，结束时间为开始时间一年后
            category.setLimitAmount(new BigDecimal("999999"));
            Date now = new Date();
            category.setLimitStartTime(now);
            LocalDateTime end = LocalDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault()).plusYears(1);
            category.setLimitEndTime(Date.from(end.atZone(ZoneId.systemDefault()).toInstant()));
            toInsert.add(category);
        }
        if (!toInsert.isEmpty()) {
            hazardousWasteCategoryMapper.insertBatch(toInsert);
        }
    }

    private HazardousWasteCategory ensureAndGetCategory(String wasteCategory, String wasteCategoryName) {
        Map<String, String> categoryNameMap = new HashMap<>(1);
        String key = requireAndNormalize(wasteCategory, "废物类别不能为空");
        categoryNameMap.put(key, resolveCategoryName(wasteCategoryName, key));
        Map<String, HazardousWasteCategory> categoryMap = ensureAndGetCategoryMap(categoryNameMap);
        HazardousWasteCategory category = categoryMap.get(key);
        if (category == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "废物类别不存在");
        }
        return category;
    }

    private Map<String, HazardousWasteCategory> ensureAndGetCategoryMap(Map<String, String> categoryNameMap) {
        ensureWasteCategoryExists(categoryNameMap);
        if (CollectionUtils.isEmpty(categoryNameMap)) {
            return Collections.emptyMap();
        }
        List<HazardousWasteCategory> existedList =
                hazardousWasteCategoryMapper.selectByWasteCategories(new ArrayList<>(categoryNameMap.keySet()));
        if (CollectionUtils.isEmpty(existedList)) {
            return Collections.emptyMap();
        }
        return existedList.stream()
                .filter(item -> StrUtil.isNotBlank(item.getWasteCategory()))
                .collect(Collectors.toMap(
                        item -> item.getWasteCategory().trim(),
                        item -> item,
                        (existing, replacement) -> existing));
    }

    private String resolveCategoryName(String candidate, String fallback) {
        String normalized = normalize(candidate);
        if (normalized != null) {
            return normalized;
        }
        return fallback;
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录");
        }
        return userId;
    }


    /**
     * 获取员工的页面权限配置
     * 
     * @param employeeId 员工ID
     * @param pageCode 页面权限编码
     * @return 员工页面权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        try {
            // 从数据库查询页面权限ID
            Permission permission = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getPermissionCode, pageCode)
                    .eq(Permission::getPermissionTypeId, 2) // 2 = 页面级权限
            );
            
            if (permission == null) {
                return null;
            }

            // 查询员工页面权限配置
            EmployeePermission employeePermission = employeePermissionMapper.selectOne(
                new LambdaQueryWrapper<EmployeePermission>()
                    .eq(EmployeePermission::getEmployeeId, employeeId)
                    .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
            );
            
            return employeePermission;
        } catch (Exception e) {
            log.error("获取员工页面权限配置失败：employeeId={}, pageCode={}", employeeId, pageCode, e);
            return null;
        }
    }

    @Override
    public List<String> getWasteCategoryList() {
        return hazardousWasteItemMapper.selectWasteCategoryList();
    }
}


