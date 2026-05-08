package com.erp.service.customer.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.customer.dto.CustomerCreateRequest;
import com.erp.controller.customer.dto.CustomerDetailResponse;
import com.erp.controller.customer.dto.CustomerImportError;
import com.erp.controller.customer.dto.CustomerImportResponse;
import com.erp.controller.customer.dto.CustomerPageRequest;
import com.erp.controller.customer.dto.CustomerPageResponse;
import com.erp.controller.customer.dto.CustomerUpdateRequest;
import com.erp.controller.customer.dto.CustomerQuotationResponse;
import com.erp.controller.customer.dto.CustomerQuotationHierarchicalResponse;
import com.erp.controller.customer.dto.CustomerContractResponse;
import com.erp.controller.customer.dto.CustomerFollowResponse;
import com.erp.entity.customer.Customer;
import com.erp.entity.contract.Quotation;
import com.erp.entity.contract.QuotationItem;
import com.erp.entity.contract.QuotationWasteItem;
import com.erp.entity.contract.Contract;
import com.erp.entity.system.Employee;
import com.erp.entity.system.HazardousWasteItem;
import com.erp.mapper.customer.CustomerMapper;
import com.erp.mapper.contract.QuotationMapper;
import com.erp.mapper.contract.QuotationItemMapper;
import com.erp.mapper.contract.QuotationWasteItemMapper;
import com.erp.mapper.contract.ContractMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.mapper.system.HazardousWasteItemMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.customer.CustomerService;
import com.erp.service.system.ILogRecordService;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户管理服务实现
 */
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private QuotationMapper quotationMapper;

    @Autowired
    private QuotationItemMapper quotationItemMapper;

    @Autowired
    private QuotationWasteItemMapper quotationWasteItemMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AuthService authService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerDetailResponse createCustomer(CustomerCreateRequest request) {
        Integer currentUserId = getCurrentUserId();
        // 新增客户时，业务员编码固定为当前登录用户
        Integer ownerId = currentUserId;
        validateOwner(ownerId);

        Customer existed = customerMapper.selectByCreditCode(request.getCreditCode());
        if (existed != null) {
            throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "统一社会信用代码已存在");
        }

        Customer customer = buildCustomerEntity(request, ownerId, currentUserId);
        customerMapper.insert(customer);
        log.info("创建客户成功：customerId={}, operator={}", customer.getCustomerId(), currentUserId);
        
        // 记录数据变更日志
        try {
            CustomerDetailResponse detail = convertToDetail(customerMapper.selectDetailById(customer.getCustomerId()));
            logRecordService.recordDataChangeLog("客户管理", "CUSTOMER", 
                    String.valueOf(customer.getCustomerId()), "新增", 
                    "新增客户：" + customer.getEnterpriseName(), 
                    null, detail, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录客户新增数据变更日志失败", e);
        }
        
        Customer detail = customerMapper.selectDetailById(customer.getCustomerId());
        return convertToDetail(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(CustomerUpdateRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        Customer customer = customerMapper.selectDetailById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
        }

        // 应用操作范围控制（operateScope）
        if (!admin) {
            // 获取当前员工对"客户档案"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:客户档案:页面");
            
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    // 仅能操作自己创建的客户
                    if (!Objects.equals(customer.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能编辑自己创建的客户");
                    }
                }
            }
            // 如果是 ALL 或没有配置，不添加限制
        }

        Integer targetOwnerId = customer.getOwnerEmployeeId();
            if (request.getOwnerEmployeeId() != null) {
                if (!admin && !Objects.equals(request.getOwnerEmployeeId(), currentUserId)) {
                    throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "无权分配其他业务员的客户");
            }
            targetOwnerId = request.getOwnerEmployeeId();
        } else if (!admin) {
            targetOwnerId = currentUserId;
        }
        validateOwner(targetOwnerId);

        if (!StrUtil.equalsIgnoreCase(request.getCreditCode(), customer.getCreditCode())) {
            Customer existed = customerMapper.selectByCreditCode(request.getCreditCode());
            if (existed != null && !Objects.equals(existed.getCustomerId(), customer.getCustomerId())) {
                throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "统一社会信用代码已存在");
            }
        }

        // 保存旧数据用于日志记录（必须在修改customer对象之前获取）
        CustomerDetailResponse oldDetail = null;
        try {
            oldDetail = convertToDetail(customerMapper.selectDetailById(customer.getCustomerId()));
        } catch (Exception e) {
            log.warn("获取客户旧数据失败，将跳过数据变更日志记录", e);
        }

        customer.setEnterpriseName(request.getEnterpriseName());
        customer.setCreditCode(request.getCreditCode());
        customer.setAddress(request.getAddress());
        customer.setPhone(normalize(request.getPhone()));
        customer.setLegalRepresentative(normalize(request.getLegalRepresentative()));
        customer.setContactPerson(request.getContactPerson());
        customer.setContactPhone(request.getContactPhone());
        customer.setFormerNames(normalize(request.getFormerNames()));
        customer.setCustomerStatus(normalize(request.getCustomerStatus()));
        customer.setOwnerEmployeeId(targetOwnerId);
        customer.setRemark(request.getRemark());
        customer.setUpdateTime(LocalDateTime.now());

        int rows = customerMapper.updateById(customer);
        if (rows == 0) {
            throw new BusinessException("更新客户失败：记录已被其他用户修改");
        }
        log.info("更新客户成功：customerId={}, operator={}", customer.getCustomerId(), currentUserId);
        
        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                CustomerDetailResponse newDetail = convertToDetail(customerMapper.selectDetailById(customer.getCustomerId()));
                logRecordService.recordDataChangeLog("客户管理", "CUSTOMER", 
                        String.valueOf(customer.getCustomerId()), "更新", 
                        "更新客户：" + customer.getEnterpriseName(), 
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.error("记录客户更新数据变更日志失败", e);
            }
        }
    }

    @Override
    public CustomerDetailResponse getCustomerDetail(Integer customerId) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        Customer customer = customerMapper.selectDetailById(customerId);
        if (customer == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
        }
        
        // 应用数据范围控制（viewScope），与列表查询保持一致
        if (!admin) {
            // 获取当前员工对"客户档案"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:客户档案:页面");
            
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // 仅查看自己创建的客户（通过creatorId判断，而不是ownerEmployeeId）
                if (!Objects.equals(customer.getCreatorId(), currentUserId)) {
                    throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "无权查看该客户");
                }
            }
            // 如果是 ALL 或没有配置，允许查看所有客户
        }
        
        return convertToDetail(customer);
    }

    @Override
    public IPage<CustomerPageResponse> getCustomerPage(CustomerPageRequest request) {
        // 数据范围权限控制
        Integer creatorFilter = resolveCreatorFilter(request.getViewScope(), "档案管理:客户档案:页面");

        // 查询正式客户
        Page<Customer> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<Customer> entityPage = customerMapper.selectCustomerPage(
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
                request.getCustomerCode(),
                request.getCustomerStatus(),
                request.getOrderBy(),
                request.getOrderDirection(),
                creatorFilter
        );

        // 转换为响应对象
        List<CustomerPageResponse> responses = entityPage.getRecords().stream()
                .map(this::convertToPage)
                .collect(Collectors.toList());

        Page<CustomerPageResponse> responsePage =
                new Page<>(request.getCurrent(), request.getSize(), entityPage.getTotal());
        responsePage.setRecords(responses);
        return responsePage;
    }

    @Override
    public List<CustomerDetailResponse> listCustomersForExport(CustomerPageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 导出数据范围控制（viewScope）
        // 后端主动校验，忽略前端传入的 creatorFilter，防止越权
        Integer creatorFilter;
        if (admin) {
            // 超级管理员：导出全部，忽略前端传入值
            creatorFilter = null;
        } else {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:客户档案:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // viewScope=SELF：强制只导出自己创建的数据
                creatorFilter = currentUserId;
            } else if (permission == null) {
                // 无权限配置：最小权限原则，只导出自己的数据
                creatorFilter = currentUserId;
            } else {
                // viewScope=ALL：忽略前端传入的 creatorFilter，强制导出全部
                creatorFilter = null;
            }
        }
        
        List<Customer> list = customerMapper.selectCustomerList(
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
                request.getCustomerCode(),
                request.getCustomerStatus(),
                creatorFilter,
                request.getOrderBy(),
                request.getOrderDirection()
        );
        return list.stream().map(this::convertToDetail).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerImportResponse importCustomers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请上传Excel文件");
        }
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        CustomerImportResponse response = new CustomerImportResponse();
        List<Customer> preparedList = new ArrayList<>();
        List<CustomerImportError> errors = response.getErrors();
        Map<String, Integer> creditRowMap = new HashMap<>();
        Set<String> seenCreditCodes = new HashSet<>();
        Map<Integer, List<Integer>> ownerRowMap = new HashMap<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Excel中没有可用的Sheet");
            }
            int totalRow = sheet.getLastRowNum();
            int processedRows = 0;
            for (int i = 1; i <= totalRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                processedRows++;
                int rowNumber = i + 1;
                try {
                    Customer customer = buildCustomerFromRow(row, admin, currentUserId);
                    if (customer == null) {
                        continue;
                    }
                    String creditCode = customer.getCreditCode();
                    if (seenCreditCodes.contains(creditCode)) {
                        addImportError(errors, rowNumber, "Excel中统一社会信用代码重复");
                        continue;
                    }
                    seenCreditCodes.add(creditCode);
                    creditRowMap.put(creditCode, rowNumber);
                    ownerRowMap.computeIfAbsent(customer.getOwnerEmployeeId(), key -> new ArrayList<>()).add(rowNumber);
                    preparedList.add(customer);
                } catch (BusinessException ex) {
                    addImportError(errors, rowNumber, ex.getMessage());
                }
            }
            response.setTotalCount(processedRows);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入客户失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导入失败：" + e.getMessage());
        }

        if (!preparedList.isEmpty()) {
            List<String> creditCodes = preparedList.stream()
                    .map(Customer::getCreditCode)
                    .distinct()
                    .collect(Collectors.toList());
            List<String> existedCodes = customerMapper.selectExistingCreditCodes(creditCodes);
            if (!CollectionUtils.isEmpty(existedCodes)) {
                for (String code : existedCodes) {
                    Integer rowNumber = creditRowMap.getOrDefault(code, -1);
                    if (rowNumber != -1) {
                        addImportError(response.getErrors(), rowNumber, "统一社会信用代码已存在于系统");
                    }
                }
                Iterator<Customer> iterator = preparedList.iterator();
                while (iterator.hasNext()) {
                    Customer next = iterator.next();
                    if (existedCodes.contains(next.getCreditCode())) {
                        iterator.remove();
                    }
                }
            }
            filterInvalidOwners(preparedList, ownerRowMap, response.getErrors());
        }

        if (!preparedList.isEmpty()) {
            preparedList.forEach(item -> {
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
            });
            customerMapper.insertBatch(preparedList);
            log.info("批量导入客户成功：count={}, operator={}", preparedList.size(), currentUserId);
        }

        response.setSuccessCount(preparedList.size());
        response.setFailCount(response.getErrors().size());
        return response;
    }

    private Customer buildCustomerFromRow(Row row, boolean admin, Integer currentUserId) {
        String enterpriseName = readCellString(row.getCell(0));
        String creditCode = readCellString(row.getCell(1));
        String address = readCellString(row.getCell(2));
        String phone = readCellString(row.getCell(3));
        String legalRepresentative = readCellString(row.getCell(4));
        String contactPerson = readCellString(row.getCell(5));
        String contactPhone = readCellString(row.getCell(6));
        String formerNames = readCellString(row.getCell(7));
        String remark = readCellString(row.getCell(8));

        if (StrUtil.isBlank(enterpriseName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "企业名称不能为空");
        }
        if (StrUtil.isBlank(creditCode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "统一社会信用代码不能为空");
        }
        if (StrUtil.isBlank(contactPerson)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "联系人不能为空");
        }
        if (StrUtil.isBlank(contactPhone)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "联系电话不能为空");
        }

        Customer customer = new Customer();
        customer.setEnterpriseName(enterpriseName.trim());
        customer.setCreditCode(creditCode.trim());
        customer.setAddress(normalize(address));
        customer.setPhone(normalize(phone));
        customer.setLegalRepresentative(normalize(legalRepresentative));
        customer.setContactPerson(contactPerson.trim());
        customer.setContactPhone(contactPhone.trim());
        customer.setFormerNames(normalize(formerNames));
        // 导入模板未提供状态列，默认设置为“正常”
        customer.setCustomerStatus("正常");
        customer.setOwnerEmployeeId(currentUserId);
        customer.setCreatorId(currentUserId);
        customer.setRemark(normalize(remark));
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        return customer;
    }

    private Customer buildCustomerEntity(CustomerCreateRequest request, Integer ownerId, Integer currentUserId) {
        Customer customer = new Customer();
        customer.setEnterpriseName(request.getEnterpriseName().trim());
        customer.setCreditCode(request.getCreditCode().trim());
        customer.setAddress(normalize(request.getAddress()));
        customer.setPhone(normalize(request.getPhone()));
        customer.setLegalRepresentative(normalize(request.getLegalRepresentative()));
        customer.setContactPerson(request.getContactPerson().trim());
        customer.setContactPhone(request.getContactPhone().trim());
        customer.setFormerNames(normalize(request.getFormerNames()));
        // 客户状态：前端未传时默认“正常”
        String status = normalize(request.getCustomerStatus());
        customer.setCustomerStatus(status != null ? status : "正常");
        customer.setOwnerEmployeeId(ownerId);
        customer.setCreatorId(currentUserId);
        customer.setRemark(normalize(request.getRemark()));
        customer.setCreateTime(LocalDateTime.now());
        customer.setUpdateTime(LocalDateTime.now());
        return customer;
    }

    private CustomerDetailResponse convertToDetail(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerDetailResponse response = new CustomerDetailResponse();
        response.setCustomerId(customer.getCustomerId());
        response.setEnterpriseName(customer.getEnterpriseName());
        response.setCreditCode(customer.getCreditCode());
        response.setAddress(customer.getAddress());
        response.setPhone(customer.getPhone());
        response.setLegalRepresentative(customer.getLegalRepresentative());
        response.setContactPerson(customer.getContactPerson());
        response.setContactPhone(customer.getContactPhone());
        response.setFormerNames(customer.getFormerNames());
        response.setCustomerStatus(customer.getCustomerStatus());
        response.setOwnerEmployeeId(customer.getOwnerEmployeeId());
        response.setOwnerEmployeeName(customer.getOwnerEmployeeName());
        response.setCreatorId(customer.getCreatorId());
        response.setCreatorName(customer.getCreatorName());
        response.setRemark(customer.getRemark());
        response.setCreateTime(customer.getCreateTime());
        response.setUpdateTime(customer.getUpdateTime());
        return response;
    }

    private CustomerPageResponse convertToPage(Customer customer) {
        CustomerPageResponse response = new CustomerPageResponse();
        response.setCustomerId(customer.getCustomerId());
        response.setEnterpriseName(customer.getEnterpriseName());
        response.setCreditCode(customer.getCreditCode());
        response.setAddress(customer.getAddress());
        response.setPhone(customer.getPhone());
        response.setLegalRepresentative(customer.getLegalRepresentative());
        response.setContactPerson(customer.getContactPerson());
        response.setContactPhone(customer.getContactPhone());
        response.setFormerNames(customer.getFormerNames());
        response.setCustomerStatus(customer.getCustomerStatus());
        response.setOwnerEmployeeId(customer.getOwnerEmployeeId());
        response.setOwnerEmployeeName(customer.getOwnerEmployeeName());
        // 操作人信息：通过创建人编码关联员工表已在 Mapper 中完成，这里直接回传给前端
        response.setCreatorId(customer.getCreatorId());
        response.setCreatorName(customer.getCreatorName());
        response.setCreateTime(customer.getCreateTime());
        response.setUpdateTime(customer.getUpdateTime());
        response.setRemark(customer.getRemark());
        return response;
    }

    private Integer determineOwnerId(Integer requestOwnerId, Integer currentUserId, boolean admin) {
        if (!admin && requestOwnerId != null && !Objects.equals(requestOwnerId, currentUserId)) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "无权指定其他业务员");
        }
        if (admin) {
            return requestOwnerId != null ? requestOwnerId : currentUserId;
        }
        return currentUserId;
    }

    private void validateOwner(Integer ownerId) {
        if (ownerId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "业务员不能为空");
        }
        Employee employee = employeeMapper.selectById(ownerId);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "业务员不存在");
        }
    }

    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录");
        }
        return userId;
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


    private void filterInvalidOwners(List<Customer> customers,
                                     Map<Integer, List<Integer>> ownerRowMap,
                                     List<CustomerImportError> errors) {
        if (CollectionUtils.isEmpty(customers) || ownerRowMap == null || ownerRowMap.isEmpty()) {
            return;
        }
        List<Integer> ownerIds = new ArrayList<>(ownerRowMap.keySet());
        List<Employee> owners = employeeMapper.selectBatchIds(ownerIds);
        Set<Integer> existed = owners == null ? new HashSet<>() :
                owners.stream()
                        .filter(Objects::nonNull)
                        .map(Employee::getEmployeeId)
                        .collect(Collectors.toSet());
        List<Integer> missingOwnerIds = ownerIds.stream()
                .filter(id -> !existed.contains(id))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(missingOwnerIds)) {
            return;
        }
        for (Integer ownerId : missingOwnerIds) {
            List<Integer> rows = ownerRowMap.get(ownerId);
            if (!CollectionUtils.isEmpty(rows)) {
                rows.forEach(row -> addImportError(errors, row, "业务员编码不存在"));
            }
        }
        customers.removeIf(customer -> missingOwnerIds.contains(customer.getOwnerEmployeeId()));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private void addImportError(List<CustomerImportError> errors, int rowNumber, String message) {
        CustomerImportError error = new CustomerImportError();
        error.setRowIndex(rowNumber);
        error.setMessage(message);
        errors.add(error);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < 9; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && StrUtil.isNotBlank(readCellString(cell))) {
                return false;
            }
        }
        return true;
    }

    private String readCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return StrUtil.trimToNull(cell.getStringCellValue());
            case NUMERIC:
                // 日期类型处理
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    java.time.LocalDateTime dt = cell.getLocalDateTimeCellValue();
                    return StrUtil.trimToNull(dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                // 数值：去掉小数尾零并避免科学计数法
                java.math.BigDecimal bd = new java.math.BigDecimal(cell.getNumericCellValue());
                String numStr = bd.stripTrailingZeros().toPlainString();
                return StrUtil.trimToNull(numStr);
            case BOOLEAN:
                return StrUtil.trimToNull(Boolean.toString(cell.getBooleanCellValue()));
            case FORMULA:
                try {
                    org.apache.poi.ss.usermodel.FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    org.apache.poi.ss.usermodel.CellValue cv = evaluator.evaluate(cell);
                    if (cv == null) return null;
                    switch (cv.getCellType()) {
                        case STRING: return StrUtil.trimToNull(cv.getStringValue());
                        case NUMERIC:
                            java.math.BigDecimal bd2 = new java.math.BigDecimal(cv.getNumberValue());
                            return StrUtil.trimToNull(bd2.stripTrailingZeros().toPlainString());
                        case BOOLEAN: return StrUtil.trimToNull(Boolean.toString(cv.getBooleanValue()));
                        default: return StrUtil.trimToNull(cell.toString());
                    }
                } catch (Exception e) {
                    return StrUtil.trimToNull(cell.toString());
                }
            default:
                return StrUtil.trimToNull(cell.toString());
        }
    }

    @Override
    public List<CustomerQuotationHierarchicalResponse> getCustomerQuotations(Integer customerId) {
        // 验证客户是否存在
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
        }

        // 查询该客户的所有报价单
        List<Quotation> quotations = quotationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Quotation>()
                        .eq(Quotation::getCustomerId, customerId)
                        .orderByDesc(Quotation::getCreateTime)
        );

        if (CollectionUtils.isEmpty(quotations)) {
            return new ArrayList<>();
        }

        // 批量查询报价条目（按报价单分组）
        List<Integer> quotationIds = quotations.stream()
                .map(Quotation::getQuotationId)
                .collect(Collectors.toList());
        
        Map<Integer, List<QuotationItem>> quotationItemMapByQuotation = new HashMap<>();
        for (Integer quotationId : quotationIds) {
            List<QuotationItem> items = quotationItemMapper.selectByQuotationId(quotationId);
            if (!CollectionUtils.isEmpty(items)) {
                quotationItemMapByQuotation.put(quotationId, items);
            }
        }

        if (quotationItemMapByQuotation.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询所有报价条目
        List<QuotationItem> allQuotationItems = new ArrayList<>();
        for (List<QuotationItem> items : quotationItemMapByQuotation.values()) {
            allQuotationItems.addAll(items);
        }

        // 批量查询危废条目明细
        List<Integer> quotationItemIds = allQuotationItems.stream()
                .map(QuotationItem::getQuotationItemId)
                .collect(Collectors.toList());
        
        List<QuotationWasteItem> wasteItems = new ArrayList<>();
        if (!CollectionUtils.isEmpty(quotationItemIds)) {
            wasteItems = quotationWasteItemMapper.selectByQuotationItemIds(quotationItemIds);
        }

        // 批量查询危废条目信息（用于获取危废条目名称）
        Set<Integer> hazardousWasteItemIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(wasteItems)) {
            hazardousWasteItemIds = wasteItems.stream()
                    .map(QuotationWasteItem::getHazardousWasteItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        Map<Integer, HazardousWasteItem> hazardousWasteItemMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(hazardousWasteItemIds)) {
            List<HazardousWasteItem> hazardousWasteItems = hazardousWasteItemMapper.selectBatchIds(hazardousWasteItemIds);
            hazardousWasteItemMap = hazardousWasteItems.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(HazardousWasteItem::getItemId, item -> item, (a, b) -> a));
        }

        // 构建危废条目明细映射（按报价条目编号分组）
        Map<Integer, List<QuotationWasteItem>> wasteItemMapByQuotationItem = new HashMap<>();
        if (!CollectionUtils.isEmpty(wasteItems)) {
            wasteItemMapByQuotationItem = wasteItems.stream()
                    .collect(Collectors.groupingBy(QuotationWasteItem::getQuotationItemId));
        }

        // 转换为层级结构响应对象
        final Map<Integer, HazardousWasteItem> finalHazardousWasteItemMap = hazardousWasteItemMap;
        final Map<Integer, List<QuotationWasteItem>> finalWasteItemMapByQuotationItem = wasteItemMapByQuotationItem;

        List<CustomerQuotationHierarchicalResponse> result = new ArrayList<>();
        
        // 遍历每个报价单
        for (Quotation quotation : quotations) {
            List<QuotationItem> quotationItems = quotationItemMapByQuotation.get(quotation.getQuotationId());
            if (CollectionUtils.isEmpty(quotationItems)) {
                continue;
            }

            CustomerQuotationHierarchicalResponse quotationResponse = new CustomerQuotationHierarchicalResponse();
            quotationResponse.setQuotationId(quotation.getQuotationId());
            quotationResponse.setQuotationNo(quotation.getQuotationNo());
            quotationResponse.setQuotationStatus(quotation.getQuotationStatus());
            quotationResponse.setCreateTime(quotation.getCreateTime());

            // 构建报价条目列表
            List<CustomerQuotationHierarchicalResponse.QuotationItemResponse> itemResponses = new ArrayList<>();
            
            for (QuotationItem quotationItem : quotationItems) {
                CustomerQuotationHierarchicalResponse.QuotationItemResponse itemResponse = 
                        new CustomerQuotationHierarchicalResponse.QuotationItemResponse();
                itemResponse.setQuotationItemId(quotationItem.getQuotationItemId());
                itemResponse.setQuotationMode(quotationItem.getQuotationMode());
                itemResponse.setPayer(quotationItem.getPayer());
                itemResponse.setPricingPlan(quotationItem.getPricingPlan());
                itemResponse.setRemark(quotationItem.getRemark());

                // 构建危废条目明细列表
                List<QuotationWasteItem> itemWasteItems = finalWasteItemMapByQuotationItem.get(quotationItem.getQuotationItemId());
                List<CustomerQuotationHierarchicalResponse.QuotationWasteItemResponse> wasteItemResponses = new ArrayList<>();
                
                if (!CollectionUtils.isEmpty(itemWasteItems)) {
                    for (QuotationWasteItem wasteItem : itemWasteItems) {
                        CustomerQuotationHierarchicalResponse.QuotationWasteItemResponse wasteItemResponse = 
                                new CustomerQuotationHierarchicalResponse.QuotationWasteItemResponse();
                        wasteItemResponse.setQuotationWasteItemId(wasteItem.getQuotationWasteItemId());
                        wasteItemResponse.setHazardousWasteItemId(wasteItem.getHazardousWasteItemId());
                        wasteItemResponse.setWasteCategory(wasteItem.getWasteCategory());
                        wasteItemResponse.setIndustrySource(wasteItem.getIndustrySource());
                        wasteItemResponse.setWasteCode(wasteItem.getWasteCode());
                        wasteItemResponse.setForm(wasteItem.getForm());
                        wasteItemResponse.setUnit(wasteItem.getUnit());
                        wasteItemResponse.setPlannedQuantity(wasteItem.getPlannedQuantity());
                        wasteItemResponse.setPayer(wasteItem.getPayer());
                        wasteItemResponse.setPricingPlan(wasteItem.getPricingPlan());
                        wasteItemResponse.setRemark(wasteItem.getRemark());

                        // 设置危废条目名称（直接使用 QUOTATION_WASTE_ITEM 表的废物名称字段）
                        wasteItemResponse.setHazardousWaste(wasteItem.getHazardousWaste());

                        // 设置辅助核算相关字段
                        wasteItemResponse.setEnableAuxiliaryAccounting(wasteItem.getEnableAuxiliaryAccounting());
                        wasteItemResponse.setAuxUnit(wasteItem.getAuxUnit());
                        wasteItemResponse.setAuxPerBase(wasteItem.getAuxPerBase());
                        wasteItemResponse.setAuxQuantity(wasteItem.getAuxQuantity());
                        wasteItemResponse.setAuxUnitPrice(wasteItem.getAuxUnitPrice());
                        wasteItemResponse.setFloorPriceRemark(wasteItem.getFloorPriceRemark());

                        wasteItemResponses.add(wasteItemResponse);
                    }
                }
                
                itemResponse.setWasteItems(wasteItemResponses);
                itemResponses.add(itemResponse);
            }

            quotationResponse.setItems(itemResponses);
            result.add(quotationResponse);
        }

        return result;
    }

    @Override
    public List<CustomerContractResponse> getCustomerContracts(Integer customerId) {
        // 验证客户是否存在
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
        }

        // 查询该客户的所有合同（使用自定义查询方法）
        List<Contract> contracts = contractMapper.selectByCustomerId(customerId);

        if (CollectionUtils.isEmpty(contracts)) {
            return new ArrayList<>();
        }

        // 转换为响应对象
        return contracts.stream()
                .map(contract -> {
                    CustomerContractResponse response = new CustomerContractResponse();
                    response.setContractId(contract.getContractId());
                    response.setContractAmount(contract.getContractAmount());
                    response.setSignTime(contract.getSignTime());
                    response.setContractStatus(contract.getContractStatus());
                    response.setValidFrom(contract.getValidFrom());
                    response.setValidTo(contract.getValidTo());
                    response.setCreateTime(contract.getCreateTime());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerFollowResponse> getCustomerFollows(Integer customerId) {
        // 验证客户是否存在
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
        }

        // TODO: 暂时返回空列表，等跟进记录表创建后再实现
        // 目前数据库中没有客户跟进表，所以暂时返回空列表
        return new ArrayList<>();
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
}


