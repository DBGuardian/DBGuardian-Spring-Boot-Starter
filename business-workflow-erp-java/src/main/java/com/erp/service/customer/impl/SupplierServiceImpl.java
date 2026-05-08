package com.erp.service.customer.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.customer.dto.*;
import com.erp.entity.customer.Supplier;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.mapper.customer.SupplierMapper;
import com.erp.mapper.customer.SupplierStatistics;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.customer.SupplierService;
import com.erp.service.system.ILogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供应商管理服务实现
 * 支持批量操作，统一处理单个和批量请求
 */
@Slf4j
@Service
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private AuthService authService;

    @Override
    public IPage<SupplierPageResponse> getSupplierPage(SupplierPageRequest request) {
        log.info("分页查询供应商，参数：{}", request);

        // 数据范围权限控制
        Integer creatorFilter = resolveCreatorFilter(request.getViewScope(), "档案管理:供应商档案:页面");

        Page<SupplierPageResponse> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<SupplierPageResponse> supplierPage = supplierMapper.selectSupplierPage(
                page,
                request.getEnterpriseName(),
                request.getCreditCode(),
                request.getOwnerEmployeeId(),
                request.getOwnerEmployee(),
                request.getContactPhone(),
                request.getPhone(),
                request.getContactPerson(),
                request.getAddress(),
                request.getLegalRepresentative(),
                request.getFormerNames(),
                request.getSupplierCode(),
                request.getSupplierStatus(),
                request.getAccountName(),
                request.getAccountNumber(),
                request.getBankName(),
                creatorFilter,
                request.getOrderBy(),
                request.getOrderDirection()
        );

        return supplierPage;
    }

    @Override
    public SupplierDetailResponse getSupplierDetail(Integer supplierId) {
        log.info("获取供应商详情，供应商ID：{}", supplierId);

        Supplier supplier = supplierMapper.selectDetailById(supplierId);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }

        return convertToDetailResponse(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierBatchResponse createSupplier(SupplierCreateRequest request) {
        log.info("创建供应商，参数：{}", request);

        long startTime = System.currentTimeMillis();
        List<SupplierBatchResult> results = new ArrayList<>();

        // 获取要创建的数据列表
        List<SupplierCreateData> dataList = request.getDataList();
        if (dataList.isEmpty()) {
            throw new BusinessException("创建请求中没有有效的供应商数据");
        }

        // 预验证：检查所有数据的唯一性，如果有重复则全部失败
        for (SupplierCreateData data : dataList) {
            if (!isCreditCodeUnique(data.getCreditCode(), null)) {
                throw new BusinessException("统一社会信用代码已存在：" + data.getCreditCode() + "，批量操作已取消");
            }
        }

        // 执行创建：任何一个失败都会触发事务回滚
        for (SupplierCreateData data : dataList) {
            long itemStartTime = System.currentTimeMillis();

            // 创建供应商实体（这里不再捕获异常，让异常传播出去）
            Supplier supplier = convertToEntity(data);
            Integer currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                throw new BusinessException("无法获取当前用户信息，请重新登录");
            }
            supplier.setCreatorId(currentUserId);

            // 保存到数据库
            supplierMapper.insert(supplier);

            // 记录数据变更日志（新增）
            try {
                currentUserId = getCurrentUserId();
                logRecordService.recordDataChangeLog("供应商管理", "SUPPLIER", String.valueOf(supplier.getSupplierId()),
                        "新增", "创建供应商：" + supplier.getEnterpriseName(),
                        null, supplier, currentUserId, null, true, null);
            } catch (Exception e) {
                log.warn("记录供应商新增数据变更日志失败", e);
            }

            // 转换为响应对象
            SupplierDetailResponse responseData = convertToDetailResponse(supplier);
            results.add(SupplierBatchResult.success(supplier.getSupplierId(), responseData,
                    System.currentTimeMillis() - itemStartTime));
        }

        long duration = System.currentTimeMillis() - startTime;
        SupplierBatchResponse response = SupplierBatchResponse.success(results, "CREATE");
        response.setDuration(duration);

        log.info("批量创建供应商完成，总数：{}，成功：{}，耗时：{}ms",
                results.size(), response.getSuccessCount(), duration);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierBatchResponse updateSupplier(SupplierUpdateRequest request) {
        log.info("更新供应商，参数：{}", request);

        // 后端行级权限控制：所有"编辑"操作仅允许创建人本人执行
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("无法获取当前用户信息，请重新登录");
        }

        long startTime = System.currentTimeMillis();
        List<SupplierBatchResult> results = new ArrayList<>();

        // 获取要更新的数据列表
        List<SupplierUpdateData> updateDataList = request.getDataList();
        if (updateDataList.isEmpty()) {
            throw new BusinessException("更新请求中没有有效的供应商数据");
        }

        // 判断是单条更新还是批量更新
        boolean isBatchUpdate = request.isBatch();
        String operationType = isBatchUpdate ? "批量更新" : "更新";

        // 预验证：批量查询所有涉及的供应商记录
        List<Integer> supplierIds = updateDataList.stream()
                .map(SupplierUpdateData::getSupplierId)
                .distinct()
                .collect(Collectors.toList());

        List<Supplier> existingSuppliers = supplierMapper.selectBatchIds(supplierIds);
        Map<Integer, Supplier> supplierMap = existingSuppliers.stream()
                .collect(Collectors.toMap(Supplier::getSupplierId, supplier -> supplier));

        // 检查所有供应商是否存在和唯一性
        for (SupplierUpdateData updateData : updateDataList) {
            Integer supplierId = updateData.getSupplierId();
            Supplier existingSupplier = supplierMap.get(supplierId);

            if (existingSupplier == null) {
                throw new BusinessException("供应商不存在，ID：" + supplierId + "，" + operationType + "操作已取消");
            }

            // 验证统一社会信用代码唯一性（只有当统一社会信用代码真正发生变化时才检查）
            if (updateData.getCreditCode() != null &&
                !Objects.equals(updateData.getCreditCode(), existingSupplier.getCreditCode())) {
                if (!isCreditCodeUnique(updateData.getCreditCode(), supplierId)) {
                    throw new BusinessException("统一社会信用代码已存在：" + updateData.getCreditCode() + "，" + operationType + "操作已取消");
                }
            }
        }

        // 应用操作范围控制（operateScope）
        boolean admin = authService.isAdmin(currentUserId);
        EmployeePermission permission = null;
        if (!admin) {
            // 获取当前员工对"供应商档案"页面的权限配置
            permission = getEmployeePagePermission(currentUserId, "档案管理:供应商档案:页面");
        }

        // 执行更新：任何一个失败都会触发事务回滚
        for (SupplierUpdateData updateData : updateDataList) {
            long itemStartTime = System.currentTimeMillis();
            Integer supplierId = updateData.getSupplierId();

            // 获取当前记录
            Supplier existingSupplier = supplierMapper.selectById(supplierId);
            if (existingSupplier == null) {
                throw new BusinessException("供应商不存在，ID：" + supplierId);
            }

            // 应用操作范围控制（operateScope）
            if (!admin) {
                if (permission != null) {
                    if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                        // 仅能操作自己创建的供应商
                        if (!Objects.equals(existingSupplier.getCreatorId(), currentUserId)) {
                            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能编辑自己创建的供应商");
                        }
                    }
                }
                // 如果是 ALL 或没有配置，不添加限制
            }

            // 仅更新非null字段（支持部分更新）
            Supplier oldSupplier = new Supplier();
            oldSupplier.setSupplierId(supplierId);
            if (updateData.getEnterpriseName() != null) {
                existingSupplier.setEnterpriseName(updateData.getEnterpriseName());
            }
            if (updateData.getCreditCode() != null) {
                existingSupplier.setCreditCode(updateData.getCreditCode());
            }
            if (updateData.getAddress() != null) {
                existingSupplier.setAddress(updateData.getAddress());
            }
            if (updateData.getPhone() != null) {
                existingSupplier.setPhone(updateData.getPhone());
            }
            if (updateData.getLegalRepresentative() != null) {
                existingSupplier.setLegalRepresentative(updateData.getLegalRepresentative());
            }
            if (updateData.getContactPerson() != null) {
                existingSupplier.setContactPerson(updateData.getContactPerson());
            }
            if (updateData.getContactPhone() != null) {
                existingSupplier.setContactPhone(updateData.getContactPhone());
            }
            if (updateData.getFormerNames() != null) {
                existingSupplier.setFormerNames(updateData.getFormerNames());
            }
            if (updateData.getSupplierStatus() != null) {
                existingSupplier.setSupplierStatus(updateData.getSupplierStatus());
            }
            if (updateData.getOwnerEmployeeId() != null) {
                existingSupplier.setOwnerEmployeeId(updateData.getOwnerEmployeeId());
            }
            if (updateData.getRemark() != null) {
                existingSupplier.setRemark(updateData.getRemark());
            }
            if (updateData.getAccountName() != null) {
                existingSupplier.setAccountName(updateData.getAccountName());
            }
            if (updateData.getAccountNumber() != null) {
                existingSupplier.setAccountNumber(updateData.getAccountNumber());
            }
            if (updateData.getBankName() != null) {
                existingSupplier.setBankName(updateData.getBankName());
            }

            // 保存更新前的数据用于日志记录
            Supplier beforeUpdate = supplierMapper.selectById(supplierId);

            existingSupplier.setUpdateTime(LocalDateTime.now());

            // 使用MyBatis-Plus的乐观锁机制
            int rows = supplierMapper.updateById(existingSupplier);
            if (rows == 0) {
                throw new BusinessException("供应商数据已被其他用户修改，请刷新页面后重试");
            }

            // 获取更新后的数据
            Supplier updatedSupplier = supplierMapper.selectById(supplierId);

            // 记录数据变更日志（更新）
            try {
                currentUserId = getCurrentUserId();
                logRecordService.recordDataChangeLog("供应商管理", "SUPPLIER", String.valueOf(supplierId),
                        "更新", "更新供应商：" + existingSupplier.getEnterpriseName(),
                        beforeUpdate, updatedSupplier, currentUserId, null, true, null);
            } catch (Exception e) {
                log.warn("记录供应商更新数据变更日志失败", e);
            }

            SupplierDetailResponse responseData = convertToDetailResponse(updatedSupplier);
            results.add(SupplierBatchResult.success(supplierId, responseData,
                    System.currentTimeMillis() - itemStartTime));
        }

        long duration = System.currentTimeMillis() - startTime;
        SupplierBatchResponse response = SupplierBatchResponse.success(results, "UPDATE");
        response.setDuration(duration);

        String operationDesc = isBatchUpdate ? "批量更新供应商" : "更新供应商";
        log.info("{}完成，总数：{}，成功：{}，耗时：{}ms",
                operationDesc, results.size(), response.getSuccessCount(), duration);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierBatchResponse deleteSupplier(SupplierDeleteRequest request) {
        log.info("删除供应商，参数：{}", request);

        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("无法获取当前用户信息，请重新登录");
        }

        // 应用操作范围控制（operateScope）
        boolean admin = authService.isAdmin(currentUserId);
        EmployeePermission permission = null;
        if (!admin) {
            // 获取当前员工对"供应商档案"页面的权限配置
            permission = getEmployeePagePermission(currentUserId, "档案管理:供应商档案:页面");
        }

        long startTime = System.currentTimeMillis();
        List<SupplierBatchResult> results = new ArrayList<>();

        // 获取要删除的供应商ID列表
        List<Integer> supplierIds = request.getSupplierIdsToDelete();
        if (supplierIds.isEmpty()) {
            throw new BusinessException("删除请求中没有有效的供应商ID");
        }

        // 预验证：检查所有供应商是否存在和权限
        for (Integer supplierId : supplierIds) {
            Supplier supplier = supplierMapper.selectById(supplierId);
            if (supplier == null) {
                throw new BusinessException("供应商不存在，ID：" + supplierId + "，批量操作已取消");
            }
            
            // 应用操作范围控制（operateScope）
            if (!admin) {
                if (permission != null) {
                    if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                        // 仅能操作自己创建的供应商
                        if (!Objects.equals(supplier.getCreatorId(), currentUserId)) {
                            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能删除自己创建的供应商，ID：" + supplierId);
                        }
                    }
                }
                // 如果是 ALL 或没有配置，不添加限制
            }
        }

        // 执行删除：任何一个失败都会触发事务回滚
        for (Integer supplierId : supplierIds) {
            long itemStartTime = System.currentTimeMillis();

            // 获取删除前的数据用于日志记录
            Supplier beforeDelete = supplierMapper.selectById(supplierId);

            // 执行删除（这里不再捕获异常，让异常传播出去）
            if (request.getSoftDelete()) {
                // 软删除：更新状态
                Supplier supplier = supplierMapper.selectById(supplierId);
                supplier.setSupplierStatus("已删除");
                supplier.setUpdateTime(LocalDateTime.now());
                supplierMapper.updateById(supplier);

                // 记录数据变更日志（软删除）
                if (beforeDelete != null) {
                    try {
                        currentUserId = getCurrentUserId();
                        logRecordService.recordDataChangeLog("供应商管理", "SUPPLIER", String.valueOf(supplierId),
                                "删除", "软删除供应商：" + supplier.getEnterpriseName(),
                                beforeDelete, supplier, currentUserId, null, true, null);
                    } catch (Exception e) {
                        log.warn("记录供应商软删除数据变更日志失败", e);
                    }
                }
            } else {
                // 硬删除
                supplierMapper.deleteById(supplierId);

                // 记录数据变更日志（硬删除）
                if (beforeDelete != null) {
                    try {
                        currentUserId = getCurrentUserId();
                        logRecordService.recordDataChangeLog("供应商管理", "SUPPLIER", String.valueOf(supplierId),
                                "删除", "删除供应商：" + beforeDelete.getEnterpriseName(),
                                beforeDelete, null, currentUserId, null, true, null);
                    } catch (Exception e) {
                        log.warn("记录供应商删除数据变更日志失败", e);
                    }
                }
            }

            results.add(SupplierBatchResult.success(supplierId, null,
                    System.currentTimeMillis() - itemStartTime));
        }

        long duration = System.currentTimeMillis() - startTime;
        SupplierBatchResponse response = SupplierBatchResponse.success(results, "DELETE");
        response.setDuration(duration);

        log.info("批量删除供应商完成，总数：{}，成功：{}，耗时：{}ms",
                results.size(), response.getSuccessCount(), duration);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierImportResponse importSuppliers(MultipartFile file) {
        log.info("开始导入供应商数据，文件名：{}", file.getOriginalFilename());

        SupplierImportResponse response = new SupplierImportResponse();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<SupplierCreateData> importData = new ArrayList<>();
            List<SupplierImportError> errors = new ArrayList<>();

            // 从第二行开始读取数据（第一行是表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    SupplierCreateData data = parseExcelRow(row);
                    importData.add(data);
                } catch (Exception e) {
                    SupplierImportError error = new SupplierImportError();
                    error.setRowIndex(i + 1);
                    error.setMessage("解析失败：" + e.getMessage());
                    errors.add(error);
                }
            }

            response.setTotalCount(importData.size());

            // 为没有指定业务员的数据设置默认业务员（当前登录用户）
            Integer currentUserId = getCurrentUserId();
            for (SupplierCreateData data : importData) {
                if (data.getOwnerEmployeeId() == null) {
                    data.setOwnerEmployeeId(currentUserId);
                }
            }

            // 批量验证和创建
            SupplierCreateRequest createRequest = SupplierCreateRequest.batch(importData);
            SupplierBatchResponse createResponse = createSupplier(createRequest);

            response.setSuccessCount(createResponse.getSuccessCount());
            response.setFailCount(createResponse.getFailCount() + errors.size());

            // 合并错误信息
            response.getErrors().addAll(errors);
            createResponse.getResults().stream()
                    .filter(result -> result.getSuccess() == null || !result.getSuccess())
                    .forEach(result -> {
                        SupplierImportError error = new SupplierImportError();
                        error.setMessage("供应商ID：" + result.getSupplierId() + " - " + result.getErrorMessage());
                        response.getErrors().add(error);
                    });

            log.info("供应商数据导入完成，总数：{}，成功：{}，失败：{}",
                    response.getTotalCount(), response.getSuccessCount(), response.getFailCount());

        } catch (Exception e) {
            log.error("导入供应商数据失败", e);
            throw new BusinessException("导入失败：" + e.getMessage());
        }

        return response;
    }

    @Override
    public void exportSuppliers(SupplierPageRequest request, HttpServletResponse response) {
        log.info("导出供应商数据，参数：{}", request);

        try {
            // 数据范围控制（viewScope）：与列表查询保持一致
            // viewScope=SELF 时，仅导出当前用户创建的供应商；viewScope=ALL 或管理员不做限制
            Integer currentUserId = getCurrentUserId();
            boolean admin = authService.isAdmin(currentUserId);
            Integer creatorFilter = null;
            if (!admin) {
                EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:供应商档案:页面");
                if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                    creatorFilter = currentUserId;
                }
            }

            // 查询数据
            List<Supplier> suppliers = supplierMapper.selectSupplierList(
                    request.getEnterpriseName(),
                    request.getCreditCode(),
                    request.getOwnerEmployeeId(),
                    request.getOwnerEmployee(),
                    request.getContactPhone(),
                    request.getPhone(),
                    request.getContactPerson(),
                    request.getAddress(),
                    request.getLegalRepresentative(),
                    request.getFormerNames(),
                    request.getSupplierCode(),
                    request.getSupplierStatus(),
                    request.getAccountName(),
                    request.getAccountNumber(),
                    request.getBankName(),
                    creatorFilter,
                    request.getOrderBy(),
                    request.getOrderDirection()
            );

            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("供应商数据");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "企业名称", "统一社会信用代码", "地址", "电话",
                    "法定代表人", "联系人", "联系电话", "曾用名", "账户名称", "账户号码", "开户银行", "备注"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 填充数据
            for (int i = 0; i < suppliers.size(); i++) {
                Supplier supplier = suppliers.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(StrUtil.blankToDefault(supplier.getEnterpriseName(), ""));
                row.createCell(1).setCellValue(StrUtil.blankToDefault(supplier.getCreditCode(), ""));
                row.createCell(2).setCellValue(StrUtil.blankToDefault(supplier.getAddress(), ""));
                row.createCell(3).setCellValue(StrUtil.blankToDefault(supplier.getPhone(), ""));
                row.createCell(4).setCellValue(StrUtil.blankToDefault(supplier.getLegalRepresentative(), ""));
                row.createCell(5).setCellValue(StrUtil.blankToDefault(supplier.getContactPerson(), ""));
                row.createCell(6).setCellValue(StrUtil.blankToDefault(supplier.getContactPhone(), ""));
                row.createCell(7).setCellValue(StrUtil.blankToDefault(supplier.getFormerNames(), ""));
                row.createCell(8).setCellValue(StrUtil.blankToDefault(supplier.getAccountName(), ""));
                row.createCell(9).setCellValue(StrUtil.blankToDefault(supplier.getAccountNumber(), ""));
                row.createCell(10).setCellValue(StrUtil.blankToDefault(supplier.getBankName(), ""));
                row.createCell(11).setCellValue(StrUtil.blankToDefault(supplier.getRemark(), ""));
            }

            // 设置列宽
            for (int i = 0; i < headers.length; i++) {
                if (i == 1 || i == 2 || i == 7 || i == 11) { // 统一社会信用代码、地址、曾用名、备注列设置更大宽度
                    sheet.setColumnWidth(i, 6000);
                } else {
                    sheet.setColumnWidth(i, 4000);
                }
            }

            // 输出文件
            try (ServletOutputStream out = response.getOutputStream()) {
                workbook.write(out);
            }
            workbook.close();

            log.info("供应商数据导出完成，共导出{}条记录", suppliers.size());

        } catch (Exception e) {
            log.error("导出供应商数据失败", e);
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public SupplierDetailResponse getSupplierByCode(String supplierCode) {
        log.info("根据编码查询供应商，编码：{}", supplierCode);

        try {
            // 将字符串编码转换为Integer ID
            Integer supplierId = Integer.valueOf(supplierCode);
            Supplier supplier = supplierMapper.selectById(supplierId);
            if (supplier == null) {
                return null;
            }
            return convertToDetailResponse(supplier);
        } catch (NumberFormatException e) {
            log.warn("供应商编码格式无效：{}", supplierCode);
            return null;
        }
    }

    @Override
    public List<SupplierPageResponse> getSuppliersByEnterpriseName(String enterpriseName) {
        log.info("根据企业名称查询供应商，企业名称：{}", enterpriseName);

        List<Supplier> suppliers = supplierMapper.selectByEnterpriseName(enterpriseName);
        return suppliers.stream()
                .map(this::convertToPageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierBatchResponse batchUpdateStatus(List<Integer> supplierIds, String status) {
        log.info("批量更新供应商状态，供应商IDs：{}，新状态：{}", supplierIds, status);

        if (supplierIds == null || supplierIds.isEmpty()) {
            throw new BusinessException("供应商ID列表不能为空");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new BusinessException("供应商状态不能为空");
        }

        // 后端行级权限控制：状态变更仅允许创建人本人执行
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("无法获取当前用户信息，请重新登录");
        }

        long startTime = System.currentTimeMillis();
        List<SupplierBatchResult> results = new ArrayList<>();

        try {
            int successCount = 0;

            // 逐个更新供应商状态，实现乐观锁检查
            for (Integer supplierId : supplierIds) {
                long itemStartTime = System.currentTimeMillis();

                try {
                    // 获取当前记录
                    Supplier existingSupplier = supplierMapper.selectById(supplierId);
                    if (existingSupplier == null) {
                        results.add(SupplierBatchResult.failure(supplierId, "供应商不存在"));
                        continue;
                    }

                    // 仅允许操作自己创建的供应商
                    if (!Objects.equals(existingSupplier.getCreatorId(), currentUserId)) {
                        results.add(SupplierBatchResult.failure(supplierId, "您只能操作自己创建的供应商"));
                        continue;
                    }

                    // 检查状态是否已经是目标状态
                    if (status.equals(existingSupplier.getSupplierStatus())) {
                        // 已经是目标状态，算作成功
                        SupplierDetailResponse responseData = convertToDetailResponse(existingSupplier);
                        results.add(SupplierBatchResult.success(supplierId, responseData,
                                System.currentTimeMillis() - itemStartTime));
                        successCount++;
                        continue;
                    }

                    // 保存更新前的状态
                    String beforeStatus = existingSupplier.getSupplierStatus();

                    // 更新状态和时间戳
                    existingSupplier.setSupplierStatus(status);
                    existingSupplier.setUpdateTime(LocalDateTime.now());

                    // 使用MyBatis-Plus的乐观锁机制更新
                    int rows = supplierMapper.updateById(existingSupplier);
                    if (rows > 0) {
                        // 获取更新后的数据
                        Supplier updatedSupplier = supplierMapper.selectById(supplierId);

                        // 记录数据变更日志（状态更新）
                        try {
                            currentUserId = getCurrentUserId();
                            logRecordService.recordDataChangeLog("供应商管理", "SUPPLIER", String.valueOf(supplierId),
                                    "更新", "批量更新供应商状态：" + beforeStatus + " -> " + status,
                                    existingSupplier, updatedSupplier, currentUserId, null, true, null);
                        } catch (Exception e) {
                            log.warn("记录供应商状态更新数据变更日志失败", e);
                        }

                        // 更新成功
                        SupplierDetailResponse responseData = convertToDetailResponse(updatedSupplier);
                        results.add(SupplierBatchResult.success(supplierId, responseData,
                                System.currentTimeMillis() - itemStartTime));
                        successCount++;
                    } else {
                        // 乐观锁冲突
                        results.add(SupplierBatchResult.failure(supplierId, "数据已被其他用户修改"));
                    }

                } catch (Exception e) {
                    log.warn("更新供应商状态失败，supplierId: {}, error: {}", supplierId, e.getMessage());
                    results.add(SupplierBatchResult.failure(supplierId, "更新失败：" + e.getMessage()));
                }
            }

            log.info("批量更新供应商状态完成，成功：{}，失败：{}", successCount, supplierIds.size() - successCount);

        } catch (Exception e) {
            log.error("批量更新供应商状态失败", e);
            for (Integer supplierId : supplierIds) {
                results.add(SupplierBatchResult.failure(supplierId, "更新失败：" + e.getMessage()));
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        SupplierBatchResponse response = SupplierBatchResponse.success(results, "BATCH_UPDATE_STATUS");
        response.setDuration(duration);

        return response;
    }

    @Override
    public SupplierStatisticsResponse getSupplierStatistics() {
        log.info("获取供应商统计信息");

        SupplierStatistics stats = supplierMapper.selectSupplierStatistics();
        if (stats == null) {
            // 如果没有统计数据，返回默认值
            stats = new SupplierStatistics();
            stats.setTotalCount(0);
            stats.setActiveCount(0);
            stats.setInactiveCount(0);
        }

        SupplierStatisticsResponse response = new SupplierStatisticsResponse();
        response.setTotalCount(stats.getTotalCount());
        response.setActiveCount(stats.getActiveCount());
        response.setInactiveCount(stats.getInactiveCount());

        return response;
    }

    @Override
    public boolean isCreditCodeUnique(String creditCode, Integer excludeSupplierId) {
        if (StrUtil.isBlank(creditCode)) {
            return true;
        }

        List<Supplier> existingSuppliers = supplierMapper.selectListByCreditCode(creditCode);
        if (CollectionUtils.isEmpty(existingSuppliers)) {
            return true;
        }

        // 如果没有排除ID，则不允许重复
        if (excludeSupplierId == null) {
            return false;
        }

        // 检查是否只有被排除的记录
        return existingSuppliers.stream()
                .allMatch(supplier -> Objects.equals(supplier.getSupplierId(), excludeSupplierId));
    }

    // 辅助方法：获取当前用户ID
    private Integer getCurrentUserId() {
        return SecurityUtil.getCurrentUserId();
    }

    /**
     * 根据视图范围解析创建人过滤条件
     *
     * @param viewScope 数据查看范围（SELF/ALL/null）
     * @param pageCode  页面权限编码
     * @return 创建人ID（仅SELF模式返回当前用户ID），其他情况返回null
     */
    private Integer resolveCreatorFilter(String viewScope, String pageCode) {
        Integer currentUserId = getCurrentUserId();

        // 管理员拥有全部权限
        if (authService.isAdmin(currentUserId)) {
            return null;
        }

        // 使用ViewScopeHelper解析视图范围
        String resolvedScope = com.erp.common.util.ViewScopeHelper.resolveViewScope(pageCode, viewScope);

        // SELF模式：仅查看自己创建的数据
        if (com.erp.common.util.ViewScopeHelper.isSelfScope(resolvedScope)) {
            return currentUserId;
        }

        // ALL模式：查看全部数据
        return null;
    }


    // 辅助方法：获取员工页面权限配置
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

    // 辅助方法：转换DTO到实体
    private Supplier convertToEntity(SupplierCreateData data) {
        Supplier supplier = new Supplier();
        supplier.setEnterpriseName(data.getEnterpriseName());
        supplier.setCreditCode(data.getCreditCode());
        supplier.setAddress(data.getAddress());
        supplier.setPhone(data.getPhone());
        supplier.setLegalRepresentative(data.getLegalRepresentative());
        supplier.setContactPerson(data.getContactPerson());
        supplier.setContactPhone(data.getContactPhone());
        supplier.setFormerNames(data.getFormerNames());
        supplier.setSupplierStatus(StrUtil.blankToDefault(data.getSupplierStatus(), "正常"));
        supplier.setOwnerEmployeeId(data.getOwnerEmployeeId());
        supplier.setRemark(data.getRemark());
        supplier.setAccountName(data.getAccountName());
        supplier.setAccountNumber(data.getAccountNumber());
        supplier.setBankName(data.getBankName());
        supplier.setCreateTime(LocalDateTime.now());
        supplier.setUpdateTime(LocalDateTime.now());
        return supplier;
    }

    // 辅助方法：转换实体到分页响应
    private SupplierPageResponse convertToPageResponse(Supplier supplier) {
        SupplierPageResponse response = new SupplierPageResponse();
        response.setSupplierId(supplier.getSupplierId());
        response.setEnterpriseName(supplier.getEnterpriseName());
        response.setCreditCode(supplier.getCreditCode());
        response.setAddress(supplier.getAddress());
        response.setPhone(supplier.getPhone());
        response.setLegalRepresentative(supplier.getLegalRepresentative());
        response.setContactPerson(supplier.getContactPerson());
        response.setContactPhone(supplier.getContactPhone());
        response.setFormerNames(supplier.getFormerNames());
        response.setSupplierStatus(supplier.getSupplierStatus());
        response.setOwnerEmployeeId(supplier.getOwnerEmployeeId());
        response.setOwnerEmployeeName(supplier.getOwnerEmployeeName());
        // 操作人名称：通过创建人编码关联员工表（在 Mapper 中已完成），这里用于单条明细转换保持字段一致
        response.setCreatorName(supplier.getCreatorName());
        response.setCreateTime(supplier.getCreateTime());
        response.setUpdateTime(supplier.getUpdateTime());
        response.setRemark(supplier.getRemark());
        response.setAccountName(supplier.getAccountName());
        response.setAccountNumber(supplier.getAccountNumber());
        response.setBankName(supplier.getBankName());
        response.setVersion(supplier.getVersion());
        return response;
    }

    // 辅助方法：转换实体到详情响应
    private SupplierDetailResponse convertToDetailResponse(Supplier supplier) {
        SupplierDetailResponse response = new SupplierDetailResponse();
        // 复制所有分页响应的字段
        SupplierPageResponse pageResponse = convertToPageResponse(supplier);
        org.springframework.beans.BeanUtils.copyProperties(pageResponse, response);

        response.setCreatorId(supplier.getCreatorId());
        response.setCreatorName(supplier.getCreatorName());
        return response;
    }

    // 辅助方法：解析Excel行数据
    private SupplierCreateData parseExcelRow(Row row) {
        SupplierCreateData data = new SupplierCreateData();

        // Excel列顺序：供应商编码(跳过), 企业名称, 统一社会信用代码, 地址, 电话, 法定代表人, 联系人, 联系电话,
        // 曾用名, 账户名称, 账户号码, 开户银行, 备注
        // 注意：供应商编码自动生成，供应商状态默认为"正常"，业务员默认为当前用户
        data.setEnterpriseName(getCellValueAsString(row.getCell(0)));
        data.setCreditCode(getCellValueAsString(row.getCell(1)));
        data.setAddress(getCellValueAsString(row.getCell(2)));
        data.setPhone(getCellValueAsString(row.getCell(3)));
        data.setLegalRepresentative(getCellValueAsString(row.getCell(4)));
        data.setContactPerson(getCellValueAsString(row.getCell(5)));
        data.setContactPhone(getCellValueAsString(row.getCell(6)));
        data.setFormerNames(getCellValueAsString(row.getCell(7)));
        data.setSupplierStatus("正常"); // 默认状态
        // 业务员会在导入时统一设置
        data.setAccountName(getCellValueAsString(row.getCell(8)));
        data.setAccountNumber(getCellValueAsString(row.getCell(9)));
        data.setBankName(getCellValueAsString(row.getCell(10)));
        data.setRemark(getCellValueAsString(row.getCell(11)));

        return data;
    }

    // 辅助方法：获取单元格字符串值（使用 DataFormatter 保持原始格式，避免科学计数法） 
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        DataFormatter formatter = new DataFormatter();
        String text = formatter.formatCellValue(cell);
        if (text == null) return null;
        text = text.trim();
        return text.length() == 0 ? null : text;
    }

    // 辅助方法：获取单元格整数值（支持字符串与数字格式）
    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        String text = getCellValueAsString(cell);
        if (text == null) return null;
        try {
            BigDecimal bd = new BigDecimal(text);
            return bd.intValue();
        } catch (Exception e) {
            try {
                return Integer.parseInt(text);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Override
    public List<SupplierSelectResponse> getSupplierSelectList(String keyword, String status) {
        log.info("查询供应商下拉列表，关键词：{}，状态：{}", keyword, status);

        // 构建查询条件
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();

        // 只查询正常状态的供应商
        if (StrUtil.isBlank(status)) {
            wrapper.eq(Supplier::getSupplierStatus, "正常");
        } else {
            wrapper.eq(Supplier::getSupplierStatus, status);
        }

        // 关键词搜索：匹配企业名称或信用代码
        if (StrUtil.isNotBlank(keyword)) {
            String searchKeyword = "%" + keyword.trim() + "%";
            wrapper.and(w -> w
                    .like(Supplier::getEnterpriseName, searchKeyword)
                    .or()
                    .like(Supplier::getCreditCode, searchKeyword)
            );
        }

        // 按企业名称排序
        wrapper.orderByAsc(Supplier::getEnterpriseName);

        // 限制返回数量，避免下拉数据过多
        wrapper.last("LIMIT 100");

        List<Supplier> suppliers = supplierMapper.selectList(wrapper);

        // 转换为下拉响应
        return suppliers.stream().map(supplier -> {
            SupplierSelectResponse response = new SupplierSelectResponse();
            response.setSupplierId(supplier.getSupplierId());
            response.setEnterpriseName(supplier.getEnterpriseName());
            response.setCreditCode(supplier.getCreditCode());
            response.setContactPerson(supplier.getContactPerson());
            response.setContactPhone(supplier.getContactPhone());
            response.setAddress(supplier.getAddress());
            response.setSupplierStatus(supplier.getSupplierStatus());
            return response;
        }).collect(Collectors.toList());
    }
}
