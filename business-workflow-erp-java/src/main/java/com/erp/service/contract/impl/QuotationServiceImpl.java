package com.erp.service.contract.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.contract.dto.ContractCustomerSnapshot;
import com.erp.controller.contract.dto.QuotationAuditRequest;
import com.erp.controller.contract.dto.QuotationBatchAuditRequest;
import com.erp.controller.contract.dto.QuotationCreateRequest;
import com.erp.controller.contract.dto.QuotationDetailResponse;
import com.erp.controller.contract.dto.QuotationPageRequest;
import com.erp.controller.contract.dto.QuotationPageResponse;
import com.erp.controller.contract.dto.QuotationUpdateRequest;
import com.erp.entity.common.File;
import com.erp.entity.contract.Quotation;
import com.erp.entity.contract.QuotationItem;
import com.erp.entity.contract.QuotationWasteItem;
import com.erp.entity.customer.Customer;
import com.erp.entity.system.Employee;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.mapper.common.FileMapper;
import com.erp.mapper.contract.ContractItemMapper;
import com.erp.mapper.contract.ContractWasteItemMapper;
import com.erp.mapper.contract.QuotationItemMapper;
import com.erp.mapper.contract.QuotationMapper;
import com.erp.mapper.contract.QuotationWasteItemMapper;
import com.erp.mapper.customer.CustomerMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.common.FileService;
import com.erp.service.common.FileStorageService;
import com.erp.service.auth.AuthService;
import com.erp.service.contract.QuotationService;
import com.erp.service.contract.dto.OaApprovalSubmitResult;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.service.oa.OaApprovalRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.multipart.MultipartFile;

import com.erp.service.common.impl.LocalFileStorageServiceImpl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报价单管理服务实现
 */
@Slf4j
@Service
public class QuotationServiceImpl implements QuotationService {

    private static final String QUOTATION_BUSINESS_TYPE = "QUOTATION";
    private static final String QUOTATION_BUSINESS_MODULE = "报价单";

    @Autowired
    private QuotationMapper quotationMapper;

    @Autowired
    private QuotationItemMapper quotationItemMapper;

    @Autowired
    private QuotationWasteItemMapper quotationWasteItemMapper;

    @Autowired
    private ContractItemMapper contractItemMapper;

    @Autowired
    private ContractWasteItemMapper contractWasteItemMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @Value("${file.storage.local.path:D:/erp}")
    private String localStoragePath;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ILogRecordService logRecordService;
    
    @Autowired
    private com.erp.mapper.contract.OutOfScopeServiceMapper outOfScopeServiceMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private OaApprovalRecordService oaApprovalRecordService;

    @Autowired
    private com.erp.mapper.system.HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private com.erp.mapper.system.HazardousWasteCategoryMapper hazardousWasteCategoryMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    /**
     * 获取当前登录用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 校验手机号格式：必须为空或11位手机号
     *
     * @param fieldName 字段名称（用于错误提示）
     * @param phone 手机号
     */
    private void validatePhoneFormat(String fieldName, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return; // 允许为空
        }
        // 11位手机号，以1开头
        if (!phone.matches("^1\\d{10}$")) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    fieldName + "格式不正确，应为手机号（如13800138000）或留空");
        }
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
    @Transactional(rollbackFor = Exception.class)
    public QuotationDetailResponse createQuotation(QuotationCreateRequest request) {
        Integer currentUserId = getCurrentUserId();

        // 验证客户（如提供）是否存在；允许通过 customerSnapshot 支持临时客户抬头
        Customer customer = null;
        ContractCustomerSnapshot snapshot = sanitizeCustomerSnapshot(request.getCustomerSnapshot());
        Integer resolvedCustomerId = request.getCustomerId();
        if (resolvedCustomerId != null) {
            customer = customerMapper.selectById(resolvedCustomerId);
            if (customer == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
            }
        } else {
            // customerId 为空时，尝试根据快照中的信用代码匹配客户
            customer = tryResolveCustomerByCreditCode(snapshot);
            if (customer != null) {
                resolvedCustomerId = customer.getCustomerId();
            }
        }

        // 验证报价条目列表
        if (CollectionUtils.isEmpty(request.getItems())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "至少需要一条报价条目");
        }

        // 报价单号直接由系统生成（忽略前端传入的编号）
        String quotationNo = generateQuotationNo();
        
        // 处理日期字符串转换（前端发送的是字符串格式）
        LocalDateTime validFrom = convertToLocalDateTime(request.getValidFrom());
        LocalDateTime validTo = convertToLocalDateTime(request.getValidTo());

        // 转换日期字段
        LocalDate quotationDate = convertToLocalDate(request.getQuotationDate());
        if (quotationDate == null) {
            quotationDate = LocalDate.now();
        }

        // 甲方信息（默认使用客户档案，可被覆盖；若无正式客户则使用快照/前端抬头）
        String partyAName = StrUtil.isNotBlank(request.getPartyAName())
                ? request.getPartyAName().trim()
                : (customer != null ? customer.getEnterpriseName() : null);
        String partyAContact = StrUtil.isNotBlank(request.getPartyAContact())
                ? request.getPartyAContact().trim()
                : (customer != null && StrUtil.isNotBlank(customer.getContactPerson()) ? customer.getContactPerson() : null);
        String partyAContactPhone = StrUtil.isNotBlank(request.getPartyAContactPhone())
                ? request.getPartyAContactPhone().trim()
                : (customer != null && StrUtil.isNotBlank(customer.getContactPhone()) ? customer.getContactPhone() : null);
        // 甲方统一社会信用代码（优先使用请求中的信息，如果没有则从客户信息中获取）
        String partyACreditCode = StrUtil.isNotBlank(request.getPartyACreditCode())
                ? request.getPartyACreditCode().trim()
                : (customer != null && StrUtil.isNotBlank(customer.getCreditCode()) ? customer.getCreditCode() : null);

        // 乙方信息必填
        String partyBName = StrUtil.nullToEmpty(request.getPartyBName()).trim();
        String partyBContact = StrUtil.nullToEmpty(request.getPartyBContact()).trim();
        String partyBContactPhone = StrUtil.nullToEmpty(request.getPartyBContactPhone()).trim();
        String partyBCreditCode = StrUtil.trim(request.getPartyBCreditCode());

        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("甲方联系电话", partyAContactPhone);
        validatePhoneFormat("乙方联系电话", partyBContactPhone);

        // 客户快照：完全由前端构造，后端仅做简单清洗与透传；未提供则不写入（保持为null）
        String customerSnapshotJson = snapshot != null ? serializeCustomerSnapshot(snapshot) : null;

        // 创建报价单
        Quotation quotation = new Quotation();
        quotation.setQuotationNo(quotationNo);
        quotation.setCustomerId(resolvedCustomerId);
        quotation.setCustomerSnapshot(customerSnapshotJson);
        quotation.setPartyAName(partyAName);
        quotation.setPartyAContact(partyAContact);
        quotation.setPartyAContactPhone(partyAContactPhone);
        quotation.setPartyACreditCode(partyACreditCode);
        quotation.setPartyBName(partyBName);
        quotation.setPartyBContact(partyBContact);
        quotation.setPartyBContactPhone(partyBContactPhone);
        quotation.setPartyBCreditCode(partyBCreditCode);
        quotation.setQuotationStatus("待审核");
        quotation.setQuotationDate(quotationDate);
        quotation.setValidFrom(validFrom);
        quotation.setValidTo(validTo);
        quotation.setRemark(request.getRemark());
        quotation.setCreatorId(currentUserId);
        quotation.setCreateTime(LocalDateTime.now());
        quotationMapper.insert(quotation);

        // 创建报价条目和危废条目明细
        createQuotationItems(quotation.getQuotationId(), request.getItems());

        // ==== 创建阶段：批量新增价外服务（如果前端传入） ====
        List<com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO> incomingServices = request.getOutOfScopeServices();
        if (incomingServices != null && !incomingServices.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            try {
                for (com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO dto : incomingServices) {
                    com.erp.entity.contract.OutOfScopeService newEntity = new com.erp.entity.contract.OutOfScopeService();
                    newEntity.setBusinessType(QUOTATION_BUSINESS_TYPE);
                    newEntity.setBusinessId(quotation.getQuotationId());
                    String projectValue = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
                    newEntity.setProject(projectValue != null ? projectValue : "");
                    newEntity.setSpec(dto.getSpec());
                    newEntity.setUnit(dto.getUnit());
                    newEntity.setPlannedQuantity(dto.getPlannedQuantity());
                    newEntity.setContractUnitPrice(dto.getContractUnitPrice());
                    newEntity.setStatus("ACTIVE");
                    newEntity.setCreatedAt(now);
                    newEntity.setCreatedBy(currentUserId);
                    outOfScopeServiceMapper.insert(newEntity);
                }
            } catch (Exception e) {
                log.error("批量创建价外服务失败，触发回滚：quotationId={}, error={}", quotation.getQuotationId(), e.getMessage(), e);
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量创建价外服务失败：" + e.getMessage());
            }
        }

        // 发送审核通知给审核人员
        try {
            sendQuotationAuditNotification(quotation, customer, currentUserId);
        } catch (Exception e) {
            log.error("发送报价单审核通知失败：quotationId={}, quotationNo={}", 
                    quotation.getQuotationId(), quotationNo, e);
            // 消息发送失败不影响主流程，只记录日志
        }

        log.info("创建报价单成功：quotationId={}, quotationNo={}, customerId={}, operator={}",
                quotation.getQuotationId(), quotationNo, resolvedCustomerId, currentUserId);

        // 记录数据变更日志
        try {
            QuotationDetailResponse detail = getQuotationDetail(quotation.getQuotationId());
            logRecordService.recordDataChangeLog("报价单管理", "QUOTATION", 
                    String.valueOf(quotation.getQuotationId()), "新增", 
                    "新增报价单：" + quotationNo, 
                    null, detail, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录报价单新增数据变更日志失败", e);
        }

        return getQuotationDetail(quotation.getQuotationId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuotation(QuotationUpdateRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 查询报价单
        Quotation quotation = quotationMapper.selectById(request.getQuotationId());
        if (quotation == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "报价单不存在");
        }

        // 应用操作范围控制（operateScope）
        if (!admin) {
            // 获取当前员工对"客户报价"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:客户报价:页面");
            
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    // 仅能操作自己创建的报价单
                    if (!Objects.equals(quotation.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能编辑自己创建的报价单");
                    }
                }
            }
            // 如果是 ALL 或没有配置，不添加限制
        }

        // 保存旧数据用于日志记录
        QuotationDetailResponse oldDetail = null;
        try {
            oldDetail = getQuotationDetail(request.getQuotationId());
        } catch (Exception e) {
            log.warn("获取报价单旧数据失败，将跳过数据变更日志记录", e);
        }

        // 验证状态：只有待审核或审核中状态的报价单可以修改
        String currentStatus = quotation.getQuotationStatus();
        if (!"待审核".equals(currentStatus) && !"审核中".equals(currentStatus)) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(), "只有待审核或审核中状态的报价单可以修改");
        }

        // 验证报价单号是否已存在（如果修改了报价单号）
        if (request.getQuotationNo() != null && !request.getQuotationNo().equals(quotation.getQuotationNo())) {
            Quotation existingQuotation = quotationMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Quotation>()
                            .eq(Quotation::getQuotationNo, request.getQuotationNo())
            );
            if (existingQuotation != null) {
                throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "报价单号已存在");
            }
        }

        // 验证报价条目列表
        if (CollectionUtils.isEmpty(request.getItems())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "至少需要一条报价条目");
        }

        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("甲方联系电话", request.getPartyAContactPhone());
        validatePhoneFormat("乙方联系电话", request.getPartyBContactPhone());

        // 转换日期格式
        LocalDateTime validFrom = convertToLocalDateTime(request.getValidFrom());
        LocalDateTime validTo = convertToLocalDateTime(request.getValidTo());
        LocalDate quotationDate = convertToLocalDate(request.getQuotationDate());

        // 先清洗快照，用于信用代码匹配及后续透传
        ContractCustomerSnapshot updatedSnapshot = sanitizeCustomerSnapshot(request.getCustomerSnapshot());
        Integer resolvedCustomerId = quotation.getCustomerId();
        if (resolvedCustomerId == null) {
            // customerId 为空时，尝试根据快照中的信用代码匹配客户，匹配到则回填
            Customer matched = tryResolveCustomerByCreditCode(updatedSnapshot);
            if (matched != null) {
                resolvedCustomerId = matched.getCustomerId();
            }
        }

        // 客户快照：仅当前端显式传入时才覆盖，未传入则保持原值不变
        if (updatedSnapshot != null) {
            quotation.setCustomerSnapshot(serializeCustomerSnapshot(updatedSnapshot));
        }

        // 更新报价单
        quotation.setQuotationNo(request.getQuotationNo() != null ? request.getQuotationNo() : quotation.getQuotationNo());
        if (resolvedCustomerId != null) {
            quotation.setCustomerId(resolvedCustomerId);
        }
        if (quotationDate != null) {
            quotation.setQuotationDate(quotationDate);
        }
        quotation.setValidFrom(validFrom);
        quotation.setValidTo(validTo);
        quotation.setRemark(request.getRemark());
        if (StrUtil.isNotBlank(request.getPartyAName())) {
            quotation.setPartyAName(request.getPartyAName().trim());
        }
        if (StrUtil.isNotBlank(request.getPartyAContact())) {
            quotation.setPartyAContact(request.getPartyAContact().trim());
        }
        if (StrUtil.isNotBlank(request.getPartyAContactPhone())) {
            quotation.setPartyAContactPhone(request.getPartyAContactPhone().trim());
        }
        if (StrUtil.isNotBlank(request.getPartyACreditCode())) {
            quotation.setPartyACreditCode(request.getPartyACreditCode().trim());
        }
        if (StrUtil.isNotBlank(request.getPartyBName())) {
            quotation.setPartyBName(request.getPartyBName().trim());
        }
        if (StrUtil.isNotBlank(request.getPartyBContact())) {
            quotation.setPartyBContact(request.getPartyBContact().trim());
        }
        if (StrUtil.isNotBlank(request.getPartyBContactPhone())) {
            quotation.setPartyBContactPhone(request.getPartyBContactPhone().trim());
        }
        if (StrUtil.isNotBlank(request.getPartyBCreditCode())) {
            quotation.setPartyBCreditCode(request.getPartyBCreditCode().trim());
        }
        quotation.setUpdateTime(LocalDateTime.now());
        int rows = quotationMapper.updateById(quotation);
        if (rows == 0) {
            throw new BusinessException("更新报价单失败：记录已被其他用户修改");
        }

        // 差分更新报价条目和危废明细（保留原有 ID，避免序号变化）
        // 1) 读取当前数据库中的旧记录
        List<QuotationItem> oldItems = quotationItemMapper.selectByQuotationId(quotation.getQuotationId());
        List<Integer> oldItemIds = Collections.emptyList();
        List<Integer> oldWasteItemIds = Collections.emptyList();
        if (!CollectionUtils.isEmpty(oldItems)) {
            oldItemIds = oldItems.stream().map(QuotationItem::getQuotationItemId).collect(Collectors.toList());
            List<QuotationWasteItem> oldWasteItems = quotationWasteItemMapper.selectByQuotationItemIds(oldItemIds);
            oldWasteItemIds = oldWasteItems.stream()
                    .map(QuotationWasteItem::getQuotationWasteItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 2) 调用创建/更新逻辑，但传入 kept 集合以记录哪些 ID 被保留（更新或新建）
        Set<Integer> keptItemIds = new HashSet<>();
        Set<Integer> keptWasteIds = new HashSet<>();
        createQuotationItemsForUpdate(quotation.getQuotationId(), request.getItems(), keptItemIds, keptWasteIds);

        // 3) 计算需删除的旧记录（在 old 列表中但不在 kept 列表中），并继续保留被合同引用的记录
        List<Integer> itemIdsToDelete = oldItemIds.stream()
                .filter(id -> !keptItemIds.contains(id))
                .collect(Collectors.toList());
        List<Integer> wasteItemIdsToDelete = oldWasteItemIds.stream()
                .filter(id -> !keptWasteIds.contains(id))
                .collect(Collectors.toList());

        if (!itemIdsToDelete.isEmpty() || !wasteItemIdsToDelete.isEmpty()) {
            // 查询哪些记录被合同引用，排除被引用的记录
            List<Integer> referencedItemIds = itemIdsToDelete.isEmpty()
                    ? Collections.emptyList()
                    : contractItemMapper.selectReferencedQuotationItemIds(itemIdsToDelete);
            List<Integer> referencedWasteItemIds = wasteItemIdsToDelete.isEmpty()
                    ? Collections.emptyList()
                    : contractWasteItemMapper.selectReferencedQuotationWasteItemIds(wasteItemIdsToDelete);

            List<Integer> finalWasteDeletes = wasteItemIdsToDelete.stream()
                    .filter(id -> !referencedWasteItemIds.contains(id))
                    .collect(Collectors.toList());
            List<Integer> finalItemDeletes = itemIdsToDelete.stream()
                    .filter(id -> !referencedItemIds.contains(id))
                    .collect(Collectors.toList());

            if (!finalWasteDeletes.isEmpty()) {
                log.info("删除 {} 条未被引用的报价危废明细（子记录）", finalWasteDeletes.size());
                quotationWasteItemMapper.deleteBatchIds(finalWasteDeletes);
            } else {
                log.info("没有可删除的未被引用危废明细或全部被合同引用，跳过删除");
            }
            if (!finalItemDeletes.isEmpty()) {
                log.info("删除 {} 条未被引用的报价条目（父记录）", finalItemDeletes.size());
                quotationItemMapper.deleteBatchIds(finalItemDeletes);
            } else {
                log.info("没有可删除的未被引用报价条目或全部被合同引用，跳过删除");
            }
        }
        // ===== 同步：更新被保留（更新或新增）的报价条目与危废明细的更新时间 =====
        try {
            LocalDateTime now = LocalDateTime.now();
            if (!keptItemIds.isEmpty()) {
                for (Integer keptItemId : keptItemIds) {
                    // 使用 UpdateWrapper 仅更新更新时间字段，避免意外覆盖其他列
                    com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<QuotationItem> uw =
                            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
                    uw.set("更新时间", now).eq("报价条目编号", keptItemId);
                    quotationItemMapper.update(null, uw);
                }
            }
            if (!keptWasteIds.isEmpty()) {
                for (Integer keptWasteId : keptWasteIds) {
                    com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<QuotationWasteItem> uw2 =
                            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
                    uw2.set("更新时间", now).eq("报价危废明细编号", keptWasteId);
                    quotationWasteItemMapper.update(null, uw2);
                }
            }
        } catch (Exception e) {
            log.warn("同步报价条目/明细更新时间失败，quotationId={}", quotation.getQuotationId(), e);
        }

        // ===== 处理价外服务的差分同步（新增/更新/删除） =====
        try {
            List<com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO> incomingServices = request.getOutOfScopeServices();
            if (incomingServices != null) {
                // 查询当前数据库中的价外服务
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.contract.OutOfScopeService> wrapper =
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                wrapper.eq("关联业务类型", QUOTATION_BUSINESS_TYPE).eq("关联业务单号", quotation.getQuotationId());
                List<com.erp.entity.contract.OutOfScopeService> existingServices =
                        outOfScopeServiceMapper.selectList(wrapper);

                Map<Integer, com.erp.entity.contract.OutOfScopeService> existingMap = existingServices.stream()
                        .filter(s -> s.getOutOfScopeServiceId() != null)
                        .collect(Collectors.toMap(com.erp.entity.contract.OutOfScopeService::getOutOfScopeServiceId, s -> s));

                // 要保留的 id 列表
                Set<Integer> keepIds = existingMap.keySet().stream().collect(Collectors.toSet());
                keepIds.clear();

                LocalDateTime now = LocalDateTime.now();

                for (com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO dto : incomingServices) {
                    if (dto.getOutOfScopeServiceId() != null) {
                        com.erp.entity.contract.OutOfScopeService exist = existingMap.get(dto.getOutOfScopeServiceId());
                        if (exist != null) {
                            // 更新字段
                            String updatedProject = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
                            exist.setProject(updatedProject != null ? updatedProject : exist.getProject());
                            exist.setSpec(dto.getSpec() != null ? dto.getSpec() : exist.getSpec());
                            exist.setUnit(dto.getUnit() != null ? dto.getUnit() : exist.getUnit());
                            exist.setPlannedQuantity(dto.getPlannedQuantity() != null ? dto.getPlannedQuantity() : exist.getPlannedQuantity());
                            exist.setContractUnitPrice(dto.getContractUnitPrice() != null ? dto.getContractUnitPrice() : exist.getContractUnitPrice());
                            exist.setUpdatedAt(now);
                            exist.setUpdatedBy(currentUserId);
                            int itemRows = outOfScopeServiceMapper.updateById(exist);
                            if (itemRows == 0) {
                                log.warn("更新非标服务失败（乐观锁冲突），id={}", exist.getOutOfScopeServiceId());
                            }
                            keepIds.add(exist.getOutOfScopeServiceId());
                        } else {
                            // id 不存在，按新增处理
                            com.erp.entity.contract.OutOfScopeService newEntity = new com.erp.entity.contract.OutOfScopeService();
                            newEntity.setBusinessType(QUOTATION_BUSINESS_TYPE);
                            newEntity.setBusinessId(quotation.getQuotationId());
                            String newProject = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
                            newEntity.setProject(newProject != null ? newProject : "");
                            newEntity.setSpec(dto.getSpec());
                            newEntity.setUnit(dto.getUnit());
                            newEntity.setPlannedQuantity(dto.getPlannedQuantity());
                            newEntity.setContractUnitPrice(dto.getContractUnitPrice());
                            newEntity.setStatus("ACTIVE");
                            newEntity.setCreatedAt(now);
                            newEntity.setCreatedBy(currentUserId);
                            outOfScopeServiceMapper.insert(newEntity);
                            if (newEntity.getOutOfScopeServiceId() != null) {
                                keepIds.add(newEntity.getOutOfScopeServiceId());
                            }
                        }
                    } else {
                        // 新增
                        com.erp.entity.contract.OutOfScopeService newEntity = new com.erp.entity.contract.OutOfScopeService();
                        newEntity.setBusinessType(QUOTATION_BUSINESS_TYPE);
                        newEntity.setBusinessId(quotation.getQuotationId());
                        String addedProject = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
                        newEntity.setProject(addedProject != null ? addedProject : "");
                        newEntity.setSpec(dto.getSpec());
                        newEntity.setUnit(dto.getUnit());
                        newEntity.setPlannedQuantity(dto.getPlannedQuantity());
                        newEntity.setContractUnitPrice(dto.getContractUnitPrice());
                        newEntity.setStatus("ACTIVE");
                        newEntity.setCreatedAt(now);
                        newEntity.setCreatedBy(currentUserId);
                        outOfScopeServiceMapper.insert(newEntity);
                        if (newEntity.getOutOfScopeServiceId() != null) {
                            keepIds.add(newEntity.getOutOfScopeServiceId());
                        }
                    }
                }

                // 删除那些不在 keepIds 中的 existing 服务
                for (com.erp.entity.contract.OutOfScopeService s : existingServices) {
                    if (s.getOutOfScopeServiceId() != null && !keepIds.contains(s.getOutOfScopeServiceId())) {
                        outOfScopeServiceMapper.deleteById(s.getOutOfScopeServiceId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("同步价外服务差异时发生错误，但不影响主要报价单更新流程", e);
        }

        log.info("更新报价单成功：quotationId={}, operator={}", quotation.getQuotationId(), currentUserId);
        
        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                QuotationDetailResponse newDetail = getQuotationDetail(quotation.getQuotationId());
                logRecordService.recordDataChangeLog("报价单管理", "QUOTATION", 
                        String.valueOf(quotation.getQuotationId()), "更新", 
                        "更新报价单：" + quotation.getQuotationNo(), 
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.warn("记录报价单更新数据变更日志失败", e);
            }
        }
    }

    @Override
    /**
     * 获取报价单详情
     */
    public QuotationDetailResponse getQuotationDetail(Integer quotationId) {
        Quotation quotation = quotationMapper.selectQuotationDetail(quotationId);
        if (quotation == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "报价单不存在");
        }

        QuotationDetailResponse response = new QuotationDetailResponse();

        // 1. 构建基本报价单信息
        buildBasicQuotationInfo(response, quotation);

        // 2. 构建客户信息
        Customer customer = buildCustomerInfo(response, quotation);

        // 3. 构建创建者信息
        buildCreatorInfo(response, quotation);

        // 4. 构建文件信息
        buildFileInfo(response, quotationId);

        // 5. 构建报价条目
        buildQuotationItems(response, quotationId);

        // 6. 构建价外服务
        buildOutOfScopeServices(response, quotationId);

        return response;
    }

    /**
     * 构建基本报价单信息
     */
    private void buildBasicQuotationInfo(QuotationDetailResponse response, Quotation quotation) {
        BeanUtils.copyProperties(quotation, response);
        // 特殊字段处理
        response.setQuotationId(quotation.getQuotationId());
        response.setQuotationNo(quotation.getQuotationNo());
        response.setCustomerId(quotation.getCustomerId());
        response.setPartyACreditCode(quotation.getPartyACreditCode());
        response.setPartyAName(quotation.getPartyAName());
        response.setPartyAContact(quotation.getPartyAContact());
        response.setPartyAContactPhone(quotation.getPartyAContactPhone());
        response.setPartyBCreditCode(quotation.getPartyBCreditCode());
        response.setPartyBName(quotation.getPartyBName());
        response.setPartyBContact(quotation.getPartyBContact());
        response.setPartyBContactPhone(quotation.getPartyBContactPhone());
    }

    /**
     * 构建客户信息
     */
    private Customer buildCustomerInfo(QuotationDetailResponse response, Quotation quotation) {
        Customer customer = quotation.getCustomerId() != null
                ? customerMapper.selectById(quotation.getCustomerId())
                : null;

        // 设置客户快照（优先 JSON，其次客户档案）
        ContractCustomerSnapshot snapshot = deserializeCustomerSnapshot(quotation.getCustomerSnapshot());
        if (snapshot == null && customer != null) {
            snapshot = buildCustomerSnapshotFromCustomer(customer);
        }
        response.setCustomerSnapshot(snapshot);

        if (customer != null) {
            response.setCustomerName(customer.getEnterpriseName());
        }
        if ((response.getCustomerName() == null || response.getCustomerName().trim().isEmpty())
                && snapshot != null) {
            response.setCustomerName(snapshot.getCustomerName());
        }

        return customer;
    }

    /**
     * 构建创建者信息
     */
    private void buildCreatorInfo(QuotationDetailResponse response, Quotation quotation) {
        if (quotation.getCreatorId() != null) {
            Employee creator = employeeMapper.selectById(quotation.getCreatorId());
            if (creator != null) {
                response.setCreatorName(creator.getEmployeeName());
            }
        }
    }

    /**
     * 构建文件信息
     */
    private void buildFileInfo(QuotationDetailResponse response, Integer quotationId) {
        File pdfFile = findLatestQuotationFile(quotationId);
        if (pdfFile != null) {
            response.setPdfFileId(pdfFile.getFileId());
            response.setPdfFileUrl(pdfFile.getFileUrl());
            response.setPdfFileName(pdfFile.getFileName());
        }
    }

    /**
     * 构建报价条目列表
     */
    private void buildQuotationItems(QuotationDetailResponse response, Integer quotationId) {
        List<QuotationItem> items = quotationItemMapper.selectByQuotationId(quotationId);
        if (CollectionUtils.isEmpty(items)) {
            response.setItems(Collections.emptyList());
            response.setTotalQuantity(BigDecimal.ZERO);
            return;
        }

        List<Integer> itemIds = items.stream()
                .map(QuotationItem::getQuotationItemId)
                .collect(Collectors.toList());

        // 批量查询危废条目明细
        List<QuotationWasteItem> wasteItems = quotationWasteItemMapper.selectByQuotationItemIds(itemIds);
        Map<Integer, List<QuotationWasteItem>> wasteItemMap = wasteItems.stream()
                .collect(Collectors.groupingBy(QuotationWasteItem::getQuotationItemId));

        // 批量填充危废条目关联字段
        if (!CollectionUtils.isEmpty(wasteItems)) {
            fillQuotationWasteItemAssociationFields(wasteItems);
        }

        List<QuotationDetailResponse.QuotationItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (QuotationItem item : items) {
            QuotationDetailResponse.QuotationItemResponse itemResponse = buildQuotationItemResponse(item, wasteItemMap);
            itemResponses.add(itemResponse);

            // 计算总数量
            List<QuotationWasteItem> itemWasteItems = wasteItemMap.get(item.getQuotationItemId());
            if (!CollectionUtils.isEmpty(itemWasteItems)) {
                BigDecimal itemTotal = itemWasteItems.stream()
                        .map(QuotationWasteItem::getPlannedQuantity)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalQuantity = totalQuantity.add(itemTotal);
            }
        }

        response.setItems(itemResponses);
        response.setTotalQuantity(totalQuantity);
    }

    /**
     * 构建单个报价条目响应
     */
    private QuotationDetailResponse.QuotationItemResponse buildQuotationItemResponse(
            QuotationItem item,
            Map<Integer, List<QuotationWasteItem>> wasteItemMap) {

        QuotationDetailResponse.QuotationItemResponse itemResponse = new QuotationDetailResponse.QuotationItemResponse();
        BeanUtils.copyProperties(item, itemResponse);

        // 设置危废条目明细
        List<QuotationWasteItem> itemWasteItems = wasteItemMap.get(item.getQuotationItemId());
        if (!CollectionUtils.isEmpty(itemWasteItems)) {
            List<QuotationDetailResponse.QuotationWasteItemResponse> wasteItemResponses = itemWasteItems.stream()
                    .map(this::convertWasteItemResponse)
                    .collect(Collectors.toList());
            // 将数据库中的创建/更新时间回填到响应对象（保持顺序一致）
            for (int i = 0; i < itemWasteItems.size() && i < wasteItemResponses.size(); i++) {
                QuotationWasteItem w = itemWasteItems.get(i);
                QuotationDetailResponse.QuotationWasteItemResponse wr = wasteItemResponses.get(i);
                wr.setCreateTime(w.getCreateTime());
                wr.setUpdateTime(w.getUpdateTime());
            }
            itemResponse.setWasteItems(wasteItemResponses);
        } else {
            itemResponse.setWasteItems(new ArrayList<>());
        }

        return itemResponse;
    }

    /**
     * 构建价外服务列表
     */
    private void buildOutOfScopeServices(QuotationDetailResponse response, Integer quotationId) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.contract.OutOfScopeService> ossWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            ossWrapper.eq("关联业务类型", QUOTATION_BUSINESS_TYPE).eq("关联业务单号", quotationId);
            List<com.erp.entity.contract.OutOfScopeService> ossList = outOfScopeServiceMapper.selectList(ossWrapper);

            if (!CollectionUtils.isEmpty(ossList)) {
                List<QuotationDetailResponse.OutOfScopeServiceResponse> ossResponses =
                        ossList.stream().map(this::buildOutOfScopeServiceResponse).collect(Collectors.toList());
                response.setOutOfScopeServices(ossResponses);
            } else {
                response.setOutOfScopeServices(new ArrayList<>());
            }
        } catch (Exception e) {
            log.warn("加载价外服务失败，不影响详情返回，quotationId={}", quotationId, e);
            response.setOutOfScopeServices(new ArrayList<>());
        }
    }

    /**
     * 构建单个价外服务响应
     */
    private QuotationDetailResponse.OutOfScopeServiceResponse buildOutOfScopeServiceResponse(
            com.erp.entity.contract.OutOfScopeService service) {
        QuotationDetailResponse.OutOfScopeServiceResponse response = new QuotationDetailResponse.OutOfScopeServiceResponse();
        BeanUtils.copyProperties(service, response);
        return response;
    }

    /**
     * 批量填充报价单危废条目的关联字段（废物类别、行业来源、废物代码、危险特性）
     */
    private void fillQuotationWasteItemAssociationFields(List<QuotationWasteItem> wasteItems) {
        if (CollectionUtils.isEmpty(wasteItems)) {
            return;
        }

        // 收集所有危废条目编号
        Set<Integer> hazardousWasteItemIds = wasteItems.stream()
                .map(QuotationWasteItem::getHazardousWasteItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(hazardousWasteItemIds)) {
            return;
        }

        // 批量查询危废条目信息
        List<com.erp.entity.system.HazardousWasteItem> hazardousWasteItems =
                hazardousWasteItemMapper.selectBatchIds(hazardousWasteItemIds);

        if (CollectionUtils.isEmpty(hazardousWasteItems)) {
            return;
        }

        // 构建危废条目信息的Map
        Map<Integer, com.erp.entity.system.HazardousWasteItem> hazardousWasteItemMap =
                hazardousWasteItems.stream()
                        .collect(Collectors.toMap(
                                com.erp.entity.system.HazardousWasteItem::getItemId,
                                Function.identity(),
                                (oldVal, newVal) -> newVal));

        // 为每个报价危废条目填充关联字段
        for (QuotationWasteItem wasteItem : wasteItems) {
            if (wasteItem.getHazardousWasteItemId() != null) {
                com.erp.entity.system.HazardousWasteItem hazardousWasteItem =
                        hazardousWasteItemMap.get(wasteItem.getHazardousWasteItemId());

                if (hazardousWasteItem != null) {
                    wasteItem.setIndustrySource(hazardousWasteItem.getIndustrySource());
                    wasteItem.setWasteCode(hazardousWasteItem.getWasteCode());

                    // 废物类别需要通过关联HAZARDOUS_WASTE_CATEGORY表获取
                    if (hazardousWasteItem.getCategoryId() != null) {
                        com.erp.entity.system.HazardousWasteCategory category =
                                hazardousWasteCategoryMapper.selectById(hazardousWasteItem.getCategoryId());
                        if (category != null) {
                            wasteItem.setWasteCategory(category.getWasteCategory());
                        }
                    }
                }
            }
        }
    }

    @Override
    public IPage<QuotationPageResponse> getQuotationPage(QuotationPageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 列表查询数据范围控制（viewScope）
        // 安全策略：无权限配置时默认仅查看自己的数据（最小权限原则）
        Integer creatorFilter;
        String viewScope = request.getViewScope();

        if (admin) {
            // 超级管理员：查看全部
            creatorFilter = null;
            log.debug("当前用户为超级管理员，不应用数据范围控制");
        } else {
            // 前端传入viewScope时直接使用，未传入时根据用户权限自动判断
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:客户报价:页面");

            // 解析视图范围
            String effectiveScope = ViewScopeHelper.resolveViewScope("业务管理:客户报价:页面", viewScope);

            if (ViewScopeHelper.isSelfScope(effectiveScope)) {
                // viewScope=SELF：仅查看自己创建的报价单
                creatorFilter = currentUserId;
                log.debug("应用数据范围控制：仅查看自己创建的报价单，viewScope={}", effectiveScope);
            } else {
                // viewScope=ALL：查看全部报价单
                creatorFilter = null;
                log.debug("数据范围控制：查看全部报价单（viewScope={}）", effectiveScope);
            }
        }

        Page<Quotation> page = new Page<>(request.getCurrent(), request.getSize());

        // 处理空字符串，转换为null
        String quotationStatus = request.getQuotationStatus();
        if (quotationStatus != null && quotationStatus.trim().isEmpty()) {
            quotationStatus = null;
        }

        String quotationNo = request.getQuotationNo();
        if (quotationNo != null && quotationNo.trim().isEmpty()) {
            quotationNo = null;
        }

        String keyword = request.getKeyword();
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        String quotationNoSearch = request.getQuotationNoSearch();
        if (quotationNoSearch != null && quotationNoSearch.trim().isEmpty()) {
            quotationNoSearch = null;
        }

        String customerName = request.getCustomerName();
        if (customerName != null && customerName.trim().isEmpty()) {
            customerName = null;
        }

        String pricingMode = request.getPricingMode();
        if (pricingMode != null && pricingMode.trim().isEmpty()) {
            pricingMode = null;
        }

        String internalCode = request.getInternalCode();
        if (internalCode != null && internalCode.trim().isEmpty()) {
            internalCode = null;
        }

        String creatorName = request.getCreatorName();
        if (creatorName != null && creatorName.trim().isEmpty()) {
            creatorName = null;
        }

        IPage<Quotation> entityPage = quotationMapper.selectQuotationPage(
                page,
                keyword,
                quotationNoSearch,
                customerName,
                request.getCustomerId(),
                quotationStatus,
                pricingMode,
                quotationNo,
                internalCode,
                creatorName,
                request.getValidFrom(),
                request.getValidTo(),
                request.getPdfGenerated(),
                creatorFilter,
                request.getSortField(),
                request.getSortOrder()
        );

        List<Quotation> records = entityPage.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        }

        List<Integer> quotationIds = records.stream()
                .map(Quotation::getQuotationId)
                .collect(Collectors.toList());

        // 批量查询客户和创建人
        Set<Integer> customerIds = records.stream()
                .map(Quotation::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> creatorIds = records.stream()
                .map(Quotation::getCreatorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<Integer, Customer> customerMap;
        if (!CollectionUtils.isEmpty(customerIds)) {
            List<Customer> customers = customerMapper.selectBatchIds(customerIds);
            customerMap = customers.stream()
                    .collect(Collectors.toMap(Customer::getCustomerId, customer -> customer, (a, b) -> a));
        } else {
            customerMap = new HashMap<>();
        }

        final Map<Integer, Employee> employeeMap;
        if (!CollectionUtils.isEmpty(creatorIds)) {
            List<Employee> employees = employeeMapper.selectBatchIds(creatorIds);
            employeeMap = employees.stream()
                    .collect(Collectors.toMap(Employee::getEmployeeId, employee -> employee, (a, b) -> a));
        } else {
            employeeMap = new HashMap<>();
        }

        // 批量查询报价条目及危废条目
        List<QuotationItem> quotationItems = quotationItemMapper.selectByQuotationIds(quotationIds);
        Map<Integer, List<QuotationItem>> quotationItemMap = quotationItems.stream()
                .collect(Collectors.groupingBy(QuotationItem::getQuotationId));

        List<Integer> allItemIds = quotationItems.stream()
                .map(QuotationItem::getQuotationItemId)
                .collect(Collectors.toList());
        final Map<Integer, List<QuotationWasteItem>> wasteItemMap;
        if (!CollectionUtils.isEmpty(allItemIds)) {
            List<QuotationWasteItem> wasteItems = quotationWasteItemMapper.selectByQuotationItemIds(allItemIds);
            wasteItemMap = wasteItems.stream()
                    .collect(Collectors.groupingBy(QuotationWasteItem::getQuotationItemId));
        } else {
            wasteItemMap = Collections.emptyMap();
        }

        Map<Integer, File> quotationFileMap = loadQuotationFileMap(quotationIds);

        // 转换为响应对象
        List<QuotationPageResponse> responseList = records.stream()
                .map(quotation -> {
                    QuotationPageResponse response = new QuotationPageResponse();
                    response.setQuotationId(quotation.getQuotationId());
                    response.setQuotationNo(quotation.getQuotationNo());
                    response.setInternalCode(quotation.getQuotationNo()); // 内部编号等同于报价单号
                    response.setQuotationCode(quotation.getQuotationNo()); // 报价单编号等同于报价单号
                    response.setCustomerId(quotation.getCustomerId());
                    Customer customer = customerMap.get(quotation.getCustomerId());
                    // 解析并返回 customer_snapshot（优先 JSON，其次客户档案）
                    ContractCustomerSnapshot snapshot = deserializeCustomerSnapshot(quotation.getCustomerSnapshot());
                    if (snapshot == null && customer != null) {
                        snapshot = buildCustomerSnapshotFromCustomer(customer);
                    }
                    response.setCustomerSnapshot(snapshot);
                    response.setCustomerName(customer != null ? customer.getEnterpriseName() : null);
                    if ((response.getCustomerName() == null || response.getCustomerName().trim().isEmpty())
                            && snapshot != null) {
                        response.setCustomerName(snapshot.getCustomerName());
                    }
                    response.setPartyAName(quotation.getPartyAName());
                    response.setPartyAContact(quotation.getPartyAContact());
                    response.setPartyAContactPhone(quotation.getPartyAContactPhone());
                    response.setPartyBName(quotation.getPartyBName());
                    response.setPartyBContact(quotation.getPartyBContact());
                    response.setPartyBContactPhone(quotation.getPartyBContactPhone());
                    response.setPartyBCreditCode(quotation.getPartyBCreditCode());
                    response.setQuotationStatus(quotation.getQuotationStatus());
                    response.setQuotationDate(quotation.getQuotationDate());
                    response.setValidFrom(quotation.getValidFrom());
                    response.setValidTo(quotation.getValidTo());
                    Employee creator = employeeMap.get(quotation.getCreatorId());
                    response.setCreatorName(creator != null ? creator.getEmployeeName() : null);
                    response.setCreatorId(quotation.getCreatorId());
                    response.setCreateTime(quotation.getCreateTime());
                    response.setUpdateTime(quotation.getUpdateTime());
                    response.setRemark(quotation.getRemark());

                    // 设置审核信息
                    response.setAuditorId(quotation.getAuditorId());
                    response.setAuditorName(quotation.getAuditorName());
                    response.setAuditOpinion(quotation.getAuditOpinion());
                    response.setAuditTime(quotation.getAuditTime());

                    // 设置报价模式（从报价条目中获取）
                    List<QuotationItem> items = quotationItemMap.get(quotation.getQuotationId());
                    if (!CollectionUtils.isEmpty(items)) {
                        Set<String> modes = items.stream()
                                .map(QuotationItem::getQuotationMode)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        if (modes.size() == 1) {
                            String mode = modes.iterator().next();
                            if ("总价包干".equals(mode)) {
                                response.setPricingMode("PACKAGE");
                            } else if ("按量结算".equals(mode)) {
                                response.setPricingMode("UNIT");
                            } else {
                                response.setPricingMode("MIXED");
                            }
                        } else if (modes.size() > 1) {
                            response.setPricingMode("MIXED");
                        }
                    } else {
                        response.setPricingMode("MIXED");
                    }

                    // 设置PDF信息
                    File pdfFile = quotationFileMap.get(quotation.getQuotationId());
                    if (pdfFile != null) {
                        response.setPdfFileId(pdfFile.getFileId());
                        response.setPdfGenerated(true);
                        response.setPdfUrl(pdfFile.getFileUrl());
                        response.setPdfFileName(pdfFile.getFileName());
                    } else {
                        response.setPdfGenerated(false);
                    }

                    // 提取计价方案信息（使用上面已定义的items变量）
                    Map<String, Object> pricingInfo = extractPricingPlanInfo(items, wasteItemMap);
                    if (pricingInfo != null) {
                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            String highlightsJson = objectMapper.writeValueAsString(pricingInfo.get("highlights"));
                            response.setPricingHighlights(highlightsJson);
                            response.setPricingSummary((String) pricingInfo.get("summary"));
                        } catch (Exception e) {
                            log.warn("构建计价方案信息失败: {}", e.getMessage());
                        }
                    }

                    return response;
                })
                .collect(Collectors.toList());

        Page<QuotationPageResponse> responsePage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        responsePage.setRecords(responseList);
        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditQuotation(QuotationAuditRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 查询报价单
        Quotation quotation = quotationMapper.selectById(request.getQuotationId());
        if (quotation == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "报价单不存在");
        }

        // 应用操作范围控制（operateScope）
        // 注意：批量审核权限不受操作范围限制，但单个审核需要检查操作范围
        // skipPermissionCheck=true 时跳过权限检查（OA回调场景）
        Boolean skipPermissionCheck = request.getSkipPermissionCheck();
        if (!admin && !Boolean.TRUE.equals(skipPermissionCheck)) {
            // 获取当前员工对"客户报价"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:客户报价:页面");

            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    // 仅能操作自己创建的报价单
                    if (!Objects.equals(quotation.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能审核自己创建的报价单");
                    }
                }
            }
            // 如果是 ALL 或没有配置，不添加限制
        }

        // 验证审核结果
        String auditResult = request.getAuditResult();
        if (auditResult == null || auditResult.trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "审核结果不能为空");
        }

        // 验证审核结果值：支持的状态值
        List<String> validStatuses = Arrays.asList("待审核", "审核中", "已通过", "已驳回", "已失效");
        if (!validStatuses.contains(auditResult)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "审核结果必须是：待审核、审核中、已通过、已驳回、已失效");
        }

        // 验证状态转换的合理性
        String currentStatus = quotation.getQuotationStatus();
        if ("已失效".equals(currentStatus)) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(), "已失效的报价单不能修改状态");
        }

        // 如果是从审核中状态审核，审核结果必须是已通过或已驳回
        if ("审核中".equals(currentStatus)) {
            if (!"已通过".equals(auditResult) && !"已驳回".equals(auditResult)) {
                throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(),
                        "审核中状态的报价单审核结果必须是：已通过或已驳回");
            }
        }

        // 如果驳回时，审核意见必填
        if ("已驳回".equals(auditResult)) {
            String auditOpinion = request.getAuditOpinion();
            if (auditOpinion == null || auditOpinion.trim().isEmpty()) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), 
                        "驳回时审核意见不能为空，请详细说明驳回原因");
            }
        }

        // 更新报价单状态
        quotation.setQuotationStatus(auditResult);

        // 设置审核人信息
        quotation.setAuditorId(currentUserId);
        Employee currentEmployee = employeeMapper.selectById(currentUserId);
        if (currentEmployee != null) {
            quotation.setAuditorName(currentEmployee.getEmployeeName());
        }

        // 设置审核意见和审核时间
        String auditOpinion = request.getAuditOpinion();
        quotation.setAuditOpinion(auditOpinion);
        quotation.setAuditTime(LocalDateTime.now());

        // 保存审核意见到备注字段（如果提供了审核意见）
        if (auditOpinion != null && !auditOpinion.trim().isEmpty()) {
            // 如果原有备注不为空，追加审核意见；否则直接设置
            String originalRemark = quotation.getRemark();
            if (originalRemark != null && !originalRemark.trim().isEmpty()) {
                // 追加审核意见，格式：原备注 + "\n[审核意见] " + 审核意见
                quotation.setRemark(originalRemark + "\n[审核意见] " + auditOpinion.trim());
            } else {
                quotation.setRemark("[审核意见] " + auditOpinion.trim());
            }
        }

        quotation.setUpdateTime(LocalDateTime.now());
        int rows = quotationMapper.updateById(quotation);
        if (rows == 0) {
            throw new BusinessException("更新报价单状态失败：记录已被其他用户修改");
        }

        // 同步更新 OA 审核记录表状态
        try {
            updateOaApprovalRecord(quotation.getQuotationId(), auditResult, currentUserId, currentEmployee != null ? currentEmployee.getEmployeeName() : null);
        } catch (Exception e) {
            log.error("同步更新OA审核记录失败：quotationId={}, auditResult={}", quotation.getQuotationId(), auditResult, e);
            // OA记录更新失败不影响主流程
        }

        // 发送审核结果通知给报价单创建人
        try {
            sendQuotationAuditResultNotification(quotation, auditResult, auditOpinion, currentUserId);
        } catch (Exception e) {
            log.error("发送报价单审核结果通知失败：quotationId={}, auditResult={}", 
                    request.getQuotationId(), auditResult, e);
            // 消息发送失败不影响主流程，只记录日志
        }

        log.info("审核报价单成功：quotationId={}, currentStatus={}, auditResult={}, auditOpinion={}, operator={}",
                request.getQuotationId(), currentStatus, auditResult, 
                auditOpinion != null ? auditOpinion : "无", currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(QuotationBatchAuditRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        List<Integer> quotationIds = request.getQuotationIds();
        String auditResult = request.getAuditResult();
        String auditOpinion = request.getAuditOpinion();

        // 验证审核结果
        if (auditResult == null || auditResult.trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "审核结果不能为空");
        }

        // 验证审核结果值
        List<String> validStatuses = Arrays.asList("待审核", "审核中", "已通过", "已驳回", "已失效");
        if (!validStatuses.contains(auditResult)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                    "审核结果必须是：待审核、审核中、已通过、已驳回、已失效");
        }

        // 如果驳回时，审核意见必填
        if ("已驳回".equals(auditResult)) {
            if (auditOpinion == null || auditOpinion.trim().isEmpty()) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), 
                        "驳回时审核意见不能为空，请详细说明驳回原因");
            }
        }

        // 批量查询报价单
        List<Quotation> quotations = quotationMapper.selectBatchIds(quotationIds);
        if (quotations == null || quotations.size() != quotationIds.size()) {
            Set<Integer> foundIds = quotations.stream().map(Quotation::getQuotationId).collect(Collectors.toSet());
            List<Integer> notFoundIds = quotationIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "部分报价单不存在: " + notFoundIds);
        }

        // 应用操作范围控制（operateScope）
        // 注意：批量审核权限不受操作范围限制，但需要检查每个报价单的操作范围
        if (!admin) {
            // 获取当前员工对"客户报价"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:客户报价:页面");
            
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    // 仅能操作自己创建的报价单
                    for (Quotation quotation : quotations) {
                        if (!Objects.equals(quotation.getCreatorId(), currentUserId)) {
                            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), 
                                    "您只能批量审核自己创建的报价单，报价单[" + quotation.getQuotationNo() + "]不是您创建的");
                        }
                    }
                }
            }
            // 如果是 ALL 或没有配置，不添加限制
        }

        // 验证所有报价单状态
        List<String> invalidStatuses = new ArrayList<>();
        for (Quotation quotation : quotations) {
            String currentStatus = quotation.getQuotationStatus();
            if ("已失效".equals(currentStatus)) {
                invalidStatuses.add("报价单[" + quotation.getQuotationNo() + "]已失效");
            }
            // 如果是从审核中状态审核，审核结果必须是已通过或已驳回
            if ("审核中".equals(currentStatus)) {
                if (!"已通过".equals(auditResult) && !"已驳回".equals(auditResult)) {
                    invalidStatuses.add("报价单[" + quotation.getQuotationNo() + "]审核中状态审核结果必须是已通过或已驳回");
                }
            }
        }

        if (!invalidStatuses.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(), String.join("；", invalidStatuses));
        }

        // 批量更新报价单状态
        LocalDateTime now = LocalDateTime.now();
        // 获取当前员工信息
        Employee currentEmployee = employeeMapper.selectById(currentUserId);
        String auditorName = currentEmployee != null ? currentEmployee.getEmployeeName() : null;

        for (Quotation quotation : quotations) {
            quotation.setQuotationStatus(auditResult);

            // 设置审核人信息
            quotation.setAuditorId(currentUserId);
            quotation.setAuditorName(auditorName);
            quotation.setAuditOpinion(auditOpinion);
            quotation.setAuditTime(now);

            // 保存审核意见到备注字段
            if (auditOpinion != null && !auditOpinion.trim().isEmpty()) {
                String originalRemark = quotation.getRemark();
                if (originalRemark != null && !originalRemark.trim().isEmpty()) {
                    quotation.setRemark(originalRemark + "\n[批量审核意见] " + auditOpinion.trim());
                } else {
                    quotation.setRemark("[批量审核意见] " + auditOpinion.trim());
                }
            }

            quotation.setUpdateTime(now);
            int batchRows = quotationMapper.updateById(quotation);
            if (batchRows == 0) {
                log.warn("批量审核更新报价单失败（乐观锁冲突），quotationId={}", quotation.getQuotationId());
            }
        }

        // 发送审核结果通知
        for (Quotation quotation : quotations) {
            try {
                sendQuotationAuditResultNotification(quotation, auditResult, auditOpinion, currentUserId);
            } catch (Exception e) {
                log.error("发送报价单审核结果通知失败：quotationId={}, auditResult={}",
                        quotation.getQuotationId(), auditResult, e);
            }
        }

        // 同步更新 OA 审核记录表状态
        for (Quotation quotation : quotations) {
            try {
                updateOaApprovalRecord(quotation.getQuotationId(), auditResult, currentUserId, auditorName);
            } catch (Exception e) {
                log.error("同步更新OA审核记录失败：quotationId={}, auditResult={}",
                        quotation.getQuotationId(), auditResult, e);
            }
        }

        log.info("批量审核报价单成功：数量={}, auditResult={}, auditOpinion={}, operator={}",
                quotationIds.size(), auditResult,
                auditOpinion != null ? auditOpinion : "无", currentUserId);
    }

    /**
     * 创建报价条目和危废条目明细（新增时使用）
     */
    private void createQuotationItems(Integer quotationId, List<? extends QuotationCreateRequest.QuotationItemRequest> items) {
        createQuotationItemsInternal(quotationId, items, false, null, null);
    }

    /**
     * 创建报价条目和危废条目明细（更新时使用）
     */
    private void createQuotationItemsForUpdate(Integer quotationId, List<? extends QuotationUpdateRequest.QuotationItemRequest> items,
                                              Set<Integer> keptItemIds, Set<Integer> keptWasteIds) {
        createQuotationItemsInternal(quotationId, items, true, keptItemIds, keptWasteIds);
    }

    /**
     * 创建报价条目和危废条目明细（内部方法）
     */
    private void createQuotationItemsInternal(Integer quotationId, List<?> items, boolean isUpdate,
                                              Set<Integer> keptItemIds, Set<Integer> keptWasteIds) {
        for (Object itemObj : items) {
            // 处理新增和更新的不同请求类型
            String quotationMode;
            String payer;
            String pricingPlan;
            String remark;
            List<?> wasteItems;

            if (isUpdate) {
                QuotationUpdateRequest.QuotationItemRequest itemRequest = (QuotationUpdateRequest.QuotationItemRequest) itemObj;
                quotationMode = itemRequest.getQuotationMode();
                payer = itemRequest.getPayer();
                pricingPlan = itemRequest.getPricingPlan();
                remark = itemRequest.getRemark();
                wasteItems = itemRequest.getWasteItems();
            } else {
                QuotationCreateRequest.QuotationItemRequest itemRequest = (QuotationCreateRequest.QuotationItemRequest) itemObj;
                quotationMode = itemRequest.getQuotationMode();
                if (quotationMode == null || quotationMode.trim().isEmpty()) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "报价模式不能为空");
                }
                // 将前端的 PACKAGE/UNIT 转换为中文
                if ("PACKAGE".equals(quotationMode)) {
                    quotationMode = "总价包干";
                } else if ("UNIT".equals(quotationMode)) {
                    quotationMode = "按量结算";
                }
                payer = itemRequest.getPayer();
                // 前端字段映射：pricingStatement -> pricingPlan
                pricingPlan = itemRequest.getPricingPlan();
                remark = itemRequest.getRemark();
                // 前端字段映射：wastes -> wasteItems
                wasteItems = itemRequest.getWasteItems();
            }
            // 验证报价模式
            if (!"总价包干".equals(quotationMode) && !"按量结算".equals(quotationMode)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "报价模式必须是：总价包干或按量结算");
            }

            // 验证按量结算时必须有危废条目明细
            if ("按量结算".equals(quotationMode)) {
                if (CollectionUtils.isEmpty(wasteItems)) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "按量结算模式必须至少有一条危废条目明细");
                }
            }

            // 创建或更新报价条目
            QuotationItem item = null;
            Integer quotationItemId = null;
            if (isUpdate) {
                QuotationUpdateRequest.QuotationItemRequest itemRequest = (QuotationUpdateRequest.QuotationItemRequest) itemObj;
                quotationItemId = itemRequest.getQuotationItemId();
                if (quotationItemId != null) {
                    // 更新现有报价条目
                    item = quotationItemMapper.selectById(quotationItemId);
                    if (item != null && quotationId.equals(item.getQuotationId())) {
                        // 验证报价条目属于当前报价单
                        item.setQuotationMode(quotationMode);
                        item.setPayer(payer);
                        if ("总价包干".equals(quotationMode)) {
                            if (StrUtil.isNotBlank(pricingPlan)) {
                                item.setPricingPlan(pricingPlan);
                            }
                            item.setRemark(remark);
                        } else {
                            // 按量结算模式：清空条目表的计价方案和备注
                            item.setPricingPlan(null);
                            item.setRemark(null);
                        }
                        String calculatedSubtotalSummary = calculateSubtotalSummary(quotationMode, wasteItems, isUpdate);
                        item.setSubtotalSummary(calculatedSubtotalSummary);
                        item.setUpdateTime(LocalDateTime.now());
                        int itemRows = quotationItemMapper.updateById(item);
                        if (itemRows == 0) {
                            log.warn("更新报价条目失败（乐观锁冲突），itemId={}", item.getQuotationItemId());
                        }
                        // 记录保留的条目ID
                        if (keptItemIds != null) {
                            keptItemIds.add(item.getQuotationItemId());
                        }
                    } else {
                        // 报价条目不存在或不属于当前报价单，创建新记录
                        item = null;
                        quotationItemId = null;
                    }
                }
            }
            if (item == null) {
                // 创建新报价条目
                item = new QuotationItem();
                item.setQuotationId(quotationId);
                item.setQuotationMode(quotationMode);
                item.setPayer(payer);
                // 总价包干时，计价方案和备注在条目表
                if ("总价包干".equals(quotationMode)) {
                    // 总价包干模式：保存完整的计价方案字符串（包含单位，如"1500元/每年"）
                    // 前端已经拼接好完整字符串，直接保存
                    if (StrUtil.isNotBlank(pricingPlan)) {
                        item.setPricingPlan(pricingPlan);
                    }
                    item.setRemark(remark);
                }
                // 计算小计摘要（按计量单位汇总）
                String calculatedSubtotalSummary = calculateSubtotalSummary(quotationMode, wasteItems, isUpdate);
                item.setSubtotalSummary(calculatedSubtotalSummary);
                item.setCreateTime(LocalDateTime.now());
                quotationItemMapper.insert(item);
                quotationItemId = item.getQuotationItemId();
                // 记录保留的条目ID（新创建的也视为保留）
                if (keptItemIds != null) {
                    keptItemIds.add(item.getQuotationItemId());
                }
            }

            // 创建或更新危废条目明细
            // 注意：无论是"总价包干"还是"按量结算"模式，都需要保存危废条目明细
            // "总价包干"模式：计价方案和备注在报价条目表，但危废条目明细用于记录包含的危废
            // "按量结算"模式：计价方案和备注在危废条目明细表
            if (!CollectionUtils.isEmpty(wasteItems)) {
                for (Object wasteItemObj : wasteItems) {
                    QuotationWasteItem wasteItem = null;
                    Integer quotationWasteItemId = null;
                    
                    if (isUpdate) {
                        QuotationUpdateRequest.QuotationWasteItemRequest wasteItemRequest = (QuotationUpdateRequest.QuotationWasteItemRequest) wasteItemObj;
                        quotationWasteItemId = wasteItemRequest.getQuotationWasteItemId();
                        if (quotationWasteItemId != null) {
                            // 更新现有危废条目明细
                            wasteItem = quotationWasteItemMapper.selectById(quotationWasteItemId);
                            if (wasteItem != null && quotationItemId.equals(wasteItem.getQuotationItemId())) {
                                // 验证危废条目明细属于当前报价条目
                                wasteItem.setHazardousWasteItemId(wasteItemRequest.getHazardousWasteItemId());
                                wasteItem.setHazardousWaste(wasteItemRequest.getHazardousWaste());
                                wasteItem.setForm(wasteItemRequest.getForm());
                                wasteItem.setUnit(wasteItemRequest.getUnit());
                                wasteItem.setPlannedQuantity(wasteItemRequest.getPlannedQuantity());
                                // 单价和金额（透传前端已算好的字段）
                                wasteItem.setUnitPrice(wasteItemRequest.getUnitPrice());
                                wasteItem.setAmount(wasteItemRequest.getAmount());
                                // 基础/辅助计量单位与换算关系（透传前端已算好的字段）
                                wasteItem.setEnableAuxiliaryAccounting(wasteItemRequest.getEnableAuxiliaryAccounting());
                                wasteItem.setBaseUnit(wasteItemRequest.getBaseUnit());
                                wasteItem.setAuxUnit(wasteItemRequest.getAuxUnit());
                                wasteItem.setAuxPerBase(wasteItemRequest.getAuxPerBase());
                                wasteItem.setBaseQuantity(wasteItemRequest.getBaseQuantity());
                                wasteItem.setAuxQuantity(wasteItemRequest.getAuxQuantity());
                                wasteItem.setBaseUnitPrice(wasteItemRequest.getBaseUnitPrice());
                                wasteItem.setAuxUnitPrice(wasteItemRequest.getAuxUnitPrice());
                                // 按量结算时，计价方案、备注和付款方在危废条目明细表
                                if ("按量结算".equals(quotationMode)) {
                                    // 提取计价方案数值（去掉单位，只保存数值），并据此补全基础/辅助单价
                                    String pricingPlanValue = extractNumericValue(wasteItemRequest.getPricingPlan());
                                    wasteItem.setPricingPlan(pricingPlanValue);
                                    enrichWasteItemUnitPrices(wasteItem, pricingPlanValue);
                                    wasteItem.setRemark(wasteItemRequest.getRemark());
                                    wasteItem.setPayer(wasteItemRequest.getPayer());
                                    // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                                    String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                                    if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                                        String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                                        floorPriceRemark = remarkParts[0];
                                    }
                                    wasteItem.setFloorPriceRemark(floorPriceRemark);
                                } else {
                                    // 总价包干模式：超量单价保存在计价方案字段（只保存数值）
                                    String overLimitPrice = extractNumericValue(wasteItemRequest.getPricingPlan());
                                    wasteItem.setPricingPlan(overLimitPrice);
                                    wasteItem.setRemark(wasteItemRequest.getRemark());
                                    // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                                    String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                                    if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                                        String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                                        floorPriceRemark = remarkParts[0];
                                    }
                                    wasteItem.setFloorPriceRemark(floorPriceRemark);
                                }
                                int wasteRows = quotationWasteItemMapper.updateById(wasteItem);
                                if (wasteRows == 0) {
                                    log.warn("更新危废明细失败（乐观锁冲突），wasteId={}", wasteItem.getQuotationWasteItemId());
                                }
                                // 记录保留的废物ID
                                if (keptWasteIds != null) {
                                    keptWasteIds.add(wasteItem.getQuotationWasteItemId());
                                }
                                continue; // 已更新，跳过插入
                            } else {
                                // 危废条目明细不存在或不属于当前报价条目，创建新记录
                                wasteItem = null;
                                quotationWasteItemId = null;
                            }
                        }
                    }
                    
                    // 创建新危废条目明细
                    if (wasteItem == null) {
                        wasteItem = new QuotationWasteItem();
                        if (isUpdate) {
                            QuotationUpdateRequest.QuotationWasteItemRequest wasteItemRequest = (QuotationUpdateRequest.QuotationWasteItemRequest) wasteItemObj;
                            wasteItem.setHazardousWasteItemId(wasteItemRequest.getHazardousWasteItemId());
                            wasteItem.setHazardousWaste(wasteItemRequest.getHazardousWaste());
                            wasteItem.setForm(wasteItemRequest.getForm());
                            wasteItem.setUnit(wasteItemRequest.getUnit());
                            wasteItem.setPlannedQuantity(wasteItemRequest.getPlannedQuantity());
                            // 单价和金额（透传前端已算好的字段）
                            wasteItem.setUnitPrice(wasteItemRequest.getUnitPrice());
                            wasteItem.setAmount(wasteItemRequest.getAmount());
                            // 基础/辅助计量单位与换算关系
                            wasteItem.setEnableAuxiliaryAccounting(wasteItemRequest.getEnableAuxiliaryAccounting());
                            wasteItem.setBaseUnit(wasteItemRequest.getBaseUnit());
                            wasteItem.setAuxUnit(wasteItemRequest.getAuxUnit());
                            wasteItem.setAuxPerBase(wasteItemRequest.getAuxPerBase());
                            wasteItem.setBaseQuantity(wasteItemRequest.getBaseQuantity());
                            wasteItem.setAuxQuantity(wasteItemRequest.getAuxQuantity());
                            wasteItem.setBaseUnitPrice(wasteItemRequest.getBaseUnitPrice());
                            wasteItem.setAuxUnitPrice(wasteItemRequest.getAuxUnitPrice());
                            // 按量结算时，计价方案、备注和付款方在危废条目明细表
                            if ("按量结算".equals(quotationMode)) {
                                // 提取计价方案数值（去掉单位，只保存数值），并据此补全基础/辅助单价
                                String pricingPlanValue = extractNumericValue(wasteItemRequest.getPricingPlan());
                                wasteItem.setPricingPlan(pricingPlanValue);
                                enrichWasteItemUnitPrices(wasteItem, pricingPlanValue);
                                wasteItem.setRemark(wasteItemRequest.getRemark());
                                wasteItem.setPayer(wasteItemRequest.getPayer());
                                // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                                String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                                if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                                    String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                                    floorPriceRemark = remarkParts[0];
                                }
                                wasteItem.setFloorPriceRemark(floorPriceRemark);
                            } else {
                                // 总价包干模式：超量单价保存在计价方案字段（只保存数值）
                                String overLimitPrice = extractNumericValue(wasteItemRequest.getPricingPlan());
                                wasteItem.setPricingPlan(overLimitPrice);
                                wasteItem.setRemark(wasteItemRequest.getRemark());
                                // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                                String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                                if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                                    String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                                    floorPriceRemark = remarkParts[0];
                                }
                                wasteItem.setFloorPriceRemark(floorPriceRemark);
                            }
                        } else {
                            QuotationCreateRequest.QuotationWasteItemRequest wasteItemRequest = (QuotationCreateRequest.QuotationWasteItemRequest) wasteItemObj;
                            wasteItem.setHazardousWasteItemId(wasteItemRequest.getHazardousWasteItemId());
                            // 前端字段映射：wasteName -> hazardousWaste
                            wasteItem.setHazardousWaste(wasteItemRequest.getHazardousWaste());
                            // 前端字段映射：wasteState -> form
                            wasteItem.setForm(wasteItemRequest.getForm());
                            // 前端字段映射：quantityUnit -> unit
                            wasteItem.setUnit(wasteItemRequest.getUnit());
                            wasteItem.setPlannedQuantity(wasteItemRequest.getPlannedQuantity());
                            // 单价和金额（透传前端已算好的字段）
                            wasteItem.setUnitPrice(wasteItemRequest.getUnitPrice());
                            wasteItem.setAmount(wasteItemRequest.getAmount());
                            // 基础/辅助计量单位与换算关系
                            wasteItem.setEnableAuxiliaryAccounting(wasteItemRequest.getEnableAuxiliaryAccounting());
                            wasteItem.setBaseUnit(wasteItemRequest.getBaseUnit());
                            wasteItem.setAuxUnit(wasteItemRequest.getAuxUnit());
                            wasteItem.setAuxPerBase(wasteItemRequest.getAuxPerBase());
                            wasteItem.setBaseQuantity(wasteItemRequest.getBaseQuantity());
                            wasteItem.setAuxQuantity(wasteItemRequest.getAuxQuantity());
                            wasteItem.setBaseUnitPrice(wasteItemRequest.getBaseUnitPrice());
                            wasteItem.setAuxUnitPrice(wasteItemRequest.getAuxUnitPrice());
                            // 按量结算时，计价方案、备注和付款方在危废条目明细表
                            if ("按量结算".equals(quotationMode)) {
                                // 前端字段映射：pricingStatement -> pricingPlan
                                // 提取计价方案数值（去掉单位，只保存数值），并据此补全基础/辅助单价
                                String pricingPlanValue = extractNumericValue(wasteItemRequest.getPricingPlan());
                                wasteItem.setPricingPlan(pricingPlanValue);
                                enrichWasteItemUnitPrices(wasteItem, pricingPlanValue);
                                wasteItem.setRemark(wasteItemRequest.getRemark());
                                wasteItem.setPayer(wasteItemRequest.getPayer());
                                // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                                String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                                if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                                    String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                                    floorPriceRemark = remarkParts[0];
                                }
                                wasteItem.setFloorPriceRemark(floorPriceRemark);
                            } else {
                                // 总价包干模式：超量单价保存在计价方案字段（只保存数值）
                                // 前端通过 pricingStatement 传递超量单价（格式：数值+单位，如 "500元/吨"）
                                String overLimitPrice = extractNumericValue(wasteItemRequest.getPricingPlan());
                                wasteItem.setPricingPlan(overLimitPrice);
                                wasteItem.setRemark(wasteItemRequest.getRemark());
                                // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                                String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                                if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                                    String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                                    floorPriceRemark = remarkParts[0];
                                }
                                wasteItem.setFloorPriceRemark(floorPriceRemark);
                            }
                        }
                        wasteItem.setQuotationItemId(quotationItemId);
                        wasteItem.setCreateTime(LocalDateTime.now());
                        quotationWasteItemMapper.insert(wasteItem);
                        // 记录保留的废物ID（新建即保留）
                        if (keptWasteIds != null) {
                            keptWasteIds.add(wasteItem.getQuotationWasteItemId());
                        }
                    }
                }
            }
        }
    }

    /**
     * 批量加载报价单的最新PDF文件
     */
    private Map<Integer, File> loadQuotationFileMap(List<Integer> quotationIds) {
        if (CollectionUtils.isEmpty(quotationIds)) {
            return new HashMap<>();
        }
        List<File> files = fileMapper.selectByBusinessTypeAndIds(QUOTATION_BUSINESS_TYPE, quotationIds);
        if (CollectionUtils.isEmpty(files)) {
            return new HashMap<>();
        }
        Map<Integer, File> result = new HashMap<>();
        for (File file : files) {
            if ("已删除".equals(file.getFileStatus())) {
                continue;
            }
            Integer businessId = file.getBusinessId();
            if (businessId == null) {
                continue;
            }
            result.putIfAbsent(businessId, file);
        }
        return result;
    }

    /**
     * 查询最新有效的报价单PDF（物理删除后，只返回正常状态的记录）
     */
    private File findLatestQuotationFile(Integer quotationId) {
        List<File> files = fileMapper.selectByBusinessTypeAndId(QUOTATION_BUSINESS_TYPE, quotationId);
        if (CollectionUtils.isEmpty(files)) {
            return null;
        }
        // 由于改为物理删除，理论上只应该有一条正常状态的记录
        return files.stream()
                .filter(file -> "正常".equals(file.getFileStatus()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 转换日期对象为 LocalDate
     */
    private LocalDate convertToLocalDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        if (dateObj instanceof LocalDate) {
            return (LocalDate) dateObj;
        }
        if (dateObj instanceof String) {
            String dateStr = ((String) dateObj).trim();
            if (StrUtil.isBlank(dateStr)) {
                return null;
            }
            try {
                Date date = DateUtil.parseDate(dateStr);
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) {
                log.warn("日期格式解析失败：{}", dateStr, e);
                return null;
            }
        }
        if (dateObj instanceof Date) {
            return ((Date) dateObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (dateObj instanceof LocalDateTime) {
            return ((LocalDateTime) dateObj).toLocalDate();
        }
        return null;
    }

    /**
     * 转换危废条目明细响应
     */
    private QuotationDetailResponse.QuotationWasteItemResponse convertWasteItemResponse(QuotationWasteItem wasteItem) {
        QuotationDetailResponse.QuotationWasteItemResponse response = new QuotationDetailResponse.QuotationWasteItemResponse();
        response.setQuotationWasteItemId(wasteItem.getQuotationWasteItemId());
        response.setHazardousWasteItemId(wasteItem.getHazardousWasteItemId());
        response.setWasteCategory(wasteItem.getWasteCategory());
        response.setIndustrySource(wasteItem.getIndustrySource());
        response.setWasteCode(wasteItem.getWasteCode());
        response.setHazardousWaste(wasteItem.getHazardousWaste());
        response.setForm(wasteItem.getForm());
        response.setUnit(wasteItem.getUnit());
        response.setPlannedQuantity(wasteItem.getPlannedQuantity());
        response.setUnitPrice(wasteItem.getUnitPrice());
        response.setAmount(wasteItem.getAmount());
        // 基础/辅助计量单位与换算关系
        response.setEnableAuxiliaryAccounting(wasteItem.getEnableAuxiliaryAccounting());
        response.setBaseUnit(wasteItem.getBaseUnit());
        response.setAuxUnit(wasteItem.getAuxUnit());
        response.setAuxPerBase(wasteItem.getAuxPerBase());
        response.setBaseQuantity(wasteItem.getBaseQuantity());
        response.setAuxQuantity(wasteItem.getAuxQuantity());
        response.setBaseUnitPrice(wasteItem.getBaseUnitPrice());
        response.setAuxUnitPrice(wasteItem.getAuxUnitPrice());
        response.setPricingPlan(wasteItem.getPricingPlan());
        response.setPayer(wasteItem.getPayer());
        response.setFloorPriceRemark(wasteItem.getFloorPriceRemark());
        response.setRemark(wasteItem.getRemark());
        return response;
    }

    /**
     * 根据计价方案数值、计量单位和换算关系，在报价危废明细上补全基础/辅助单价
     * <p>
     * 约定：
     * - pricingNumeric 为不带单位的数值字符串（例如 "1500"）
     * - unit 为计量单位（"吨" 或 桶/袋/车等），决定该数值是基础单价还是辅助单价
     * - auxPerBase 表示 1 吨 ≈ N 辅助单位
     * </p>
     */
    private void enrichWasteItemUnitPrices(QuotationWasteItem wasteItem, String pricingNumeric) {
        if (StrUtil.isBlank(pricingNumeric)) {
            return;
        }
        java.math.BigDecimal numeric;
        try {
            numeric = new java.math.BigDecimal(pricingNumeric.trim());
        } catch (Exception e) {
            return;
        }
        String unit = wasteItem.getUnit();
        java.math.BigDecimal auxPerBase = wasteItem.getAuxPerBase();
        // 计量单位为吨：该数值视为基础单价
        if ("吨".equals(unit)) {
            wasteItem.setBaseUnitPrice(numeric);
            if (auxPerBase != null && auxPerBase.compareTo(java.math.BigDecimal.ZERO) > 0) {
                // 1 吨 ≈ N 辅助单位 → 单个辅助单位价格 = 吨价 / N
                java.math.BigDecimal auxPrice = numeric.divide(auxPerBase, 6, java.math.RoundingMode.HALF_UP);
                wasteItem.setAuxUnitPrice(auxPrice);
            }
        } else {
            // 计量单位为辅助单位：该数值视为辅助单价
            wasteItem.setAuxUnitPrice(numeric);
            if (auxPerBase != null && auxPerBase.compareTo(java.math.BigDecimal.ZERO) > 0) {
                // 1 吨 ≈ N 辅助单位 → 吨价 = 单个辅助单位价格 * N
                java.math.BigDecimal basePrice = numeric.multiply(auxPerBase).setScale(6, java.math.RoundingMode.HALF_UP);
                wasteItem.setBaseUnitPrice(basePrice);
            }
        }
    }

    /**
     * 转换日期对象为 LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        if (dateObj instanceof LocalDateTime) {
            return (LocalDateTime) dateObj;
        }
        if (dateObj instanceof String) {
            String dateStr = (String) dateObj;
            if (dateStr.trim().isEmpty()) {
                return null;
            }
            try {
                // 尝试解析多种日期格式
                Date date = DateUtil.parse(dateStr);
                return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            } catch (Exception e) {
                log.warn("日期格式解析失败：{}", dateStr, e);
                return null;
            }
        }
        if (dateObj instanceof Date) {
            Date date = (Date) dateObj;
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }

    /**
     * 生成报价单号
     * 生成规则：QT-YYYYMMDD-XXXXX
     * - QT：报价单前缀
     * - YYYYMMDD：当前日期（年月日）
     * - XXXXX：5位序号，从00001开始，按日期递增
     * 
     * 说明：
     * - 报价单号由后端自动生成，前端不生成临时编号
     * - 如果前端提供了internalCode或quotationNo，会优先使用（需验证唯一性）
     * - 如果未提供，系统自动按此规则生成
     * 
     * @return 生成的报价单号，格式：QT-YYYYMMDD-XXXXX
     */
    private String generateQuotationNo() {
        // 格式：QT-YYYYMMDD-XXXXX
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "QT-" + dateStr + "-";
        
        // 查询当天最大的报价单号
        Quotation maxQuotation = quotationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Quotation>()
                        .likeRight(Quotation::getQuotationNo, prefix)
                        .orderByDesc(Quotation::getQuotationNo)
                        .last("LIMIT 1")
        );
        
        int sequence = 1;
        if (maxQuotation != null && maxQuotation.getQuotationNo() != null) {
            String maxNo = maxQuotation.getQuotationNo();
            // 确保字符串长度足够，避免越界
            if (maxNo.length() > prefix.length()) {
                String sequenceStr = maxNo.substring(prefix.length());
                try {
                    sequence = Integer.parseInt(sequenceStr) + 1;
                } catch (NumberFormatException e) {
                    log.warn("解析报价单号序号失败：maxNo={}, prefix={}", maxNo, prefix, e);
                    sequence = 1;
                }
            }
        }
        
        // 生成5位序号，从00001开始
        String quotationNo = prefix + String.format("%05d", sequence);
        log.debug("生成报价单号：{}", quotationNo);
        return quotationNo;
    }

    @Override
    public List<QuotationPageResponse> listQuotationsForExport(QuotationPageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 导出数据范围控制（viewScope）
        // 安全策略：后端主动校验并强制覆盖前端传入的 viewScope，防止越权
        Integer creatorFilter;
        String viewScope = request.getViewScope();

        if (admin) {
            // 超级管理员：导出全部
            creatorFilter = null;
        } else {
            // 前端传入viewScope时直接使用，未传入时根据用户权限自动判断
            String effectiveScope = ViewScopeHelper.resolveViewScope("业务管理:客户报价:页面", viewScope);

            if (ViewScopeHelper.isSelfScope(effectiveScope)) {
                // viewScope=SELF：强制只导出自己创建的数据
                creatorFilter = currentUserId;
            } else {
                // viewScope=ALL：导出全部数据
                creatorFilter = null;
            }
        }
        
        // 使用分页查询，但设置一个很大的size来获取所有数据
        Page<Quotation> page = new Page<>(1, Integer.MAX_VALUE);

        // 处理空字符串，转换为null
        String quotationStatus = request.getQuotationStatus();
        if (quotationStatus != null && quotationStatus.trim().isEmpty()) {
            quotationStatus = null;
        }

        String quotationNo = request.getQuotationNo();
        if (quotationNo != null && quotationNo.trim().isEmpty()) {
            quotationNo = null;
        }

        String keyword = request.getKeyword();
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        String quotationNoSearch = request.getQuotationNoSearch();
        if (quotationNoSearch != null && quotationNoSearch.trim().isEmpty()) {
            quotationNoSearch = null;
        }

        String customerName = request.getCustomerName();
        if (customerName != null && customerName.trim().isEmpty()) {
            customerName = null;
        }

        String pricingMode = request.getPricingMode();
        if (pricingMode != null && pricingMode.trim().isEmpty()) {
            pricingMode = null;
        }

        String internalCode = request.getInternalCode();
        if (internalCode != null && internalCode.trim().isEmpty()) {
            internalCode = null;
        }

        String creatorName = request.getCreatorName();
        if (creatorName != null && creatorName.trim().isEmpty()) {
            creatorName = null;
        }

        IPage<Quotation> entityPage = quotationMapper.selectQuotationPage(
                page,
                keyword,
                quotationNoSearch,
                customerName,
                request.getCustomerId(),
                quotationStatus,
                pricingMode,
                quotationNo,
                internalCode,
                creatorName,
                request.getValidFrom(),
                request.getValidTo(),
                request.getPdfGenerated(),
                creatorFilter,  // 数据范围过滤参数
                request.getSortField(),
                request.getSortOrder()
        );

        List<Quotation> records = entityPage.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return new ArrayList<>();
        }

        List<Integer> quotationIds = records.stream()
                .map(Quotation::getQuotationId)
                .collect(Collectors.toList());

        // 批量查询客户和创建人
        Set<Integer> customerIds = records.stream()
                .map(Quotation::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Integer> creatorIds = records.stream()
                .map(Quotation::getCreatorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<Integer, Customer> customerMap;
        if (!CollectionUtils.isEmpty(customerIds)) {
            List<Customer> customers = customerMapper.selectBatchIds(customerIds);
            customerMap = customers.stream()
                    .collect(Collectors.toMap(Customer::getCustomerId, customer -> customer, (a, b) -> a));
        } else {
            customerMap = new HashMap<>();
        }

        final Map<Integer, Employee> employeeMap;
        if (!CollectionUtils.isEmpty(creatorIds)) {
            List<Employee> employees = employeeMapper.selectBatchIds(creatorIds);
            employeeMap = employees.stream()
                    .collect(Collectors.toMap(Employee::getEmployeeId, employee -> employee, (a, b) -> a));
        } else {
            employeeMap = new HashMap<>();
        }

        // 批量查询报价条目及危废条目
        List<QuotationItem> quotationItems = quotationItemMapper.selectByQuotationIds(quotationIds);
        Map<Integer, List<QuotationItem>> quotationItemMap = quotationItems.stream()
                .collect(Collectors.groupingBy(QuotationItem::getQuotationId));

        List<Integer> allItemIds = quotationItems.stream()
                .map(QuotationItem::getQuotationItemId)
                .collect(Collectors.toList());
        final Map<Integer, List<QuotationWasteItem>> wasteItemMap;
        if (!CollectionUtils.isEmpty(allItemIds)) {
            List<QuotationWasteItem> wasteItems = quotationWasteItemMapper.selectByQuotationItemIds(allItemIds);
            wasteItemMap = wasteItems.stream()
                    .collect(Collectors.groupingBy(QuotationWasteItem::getQuotationItemId));
        } else {
            wasteItemMap = Collections.emptyMap();
        }

        Map<Integer, File> quotationFileMap = loadQuotationFileMap(quotationIds);

        // 转换为响应对象
        return records.stream()
                .map(quotation -> {
                    QuotationPageResponse response = new QuotationPageResponse();
                    response.setQuotationId(quotation.getQuotationId());
                    response.setQuotationNo(quotation.getQuotationNo());
                    response.setInternalCode(quotation.getQuotationNo());
                    response.setQuotationCode(quotation.getQuotationNo());

                    Customer customer = quotation.getCustomerId() != null
                            ? customerMap.get(quotation.getCustomerId())
                            : null;

                    response.setQuotationStatus(quotation.getQuotationStatus());
                     response.setPartyAName(quotation.getPartyAName());
                     response.setPartyAContact(quotation.getPartyAContact());
                     response.setPartyAContactPhone(quotation.getPartyAContactPhone());
                     response.setPartyBName(quotation.getPartyBName());
                     response.setQuotationDate(quotation.getQuotationDate());
                    response.setValidFrom(quotation.getValidFrom());
                    response.setValidTo(quotation.getValidTo());
                    response.setRemark(quotation.getRemark());
                    response.setCreateTime(quotation.getCreateTime());

                    // 设置报价模式（从报价条目获取）
                    List<QuotationItem> items = quotationItemMap.get(quotation.getQuotationId());
                    if (!CollectionUtils.isEmpty(items)) {
                        // 如果所有条目都是同一模式，使用该模式；否则使用MIXED
                        Set<String> modes = items.stream()
                                .map(QuotationItem::getQuotationMode)
                                .collect(Collectors.toSet());
                        if (modes.size() == 1) {
                            String mode = modes.iterator().next();
                            if ("总价包干".equals(mode)) {
                                response.setPricingMode("PACKAGE");
                            } else if ("按量结算".equals(mode)) {
                                response.setPricingMode("UNIT");
                            } else {
                                response.setPricingMode("MIXED");
                            }
                        } else {
                            response.setPricingMode("MIXED");
                        }
                    } else {
                        response.setPricingMode("MIXED");
                    }

                    Employee creator = employeeMap.get(quotation.getCreatorId());
                    if (creator != null) {
                        response.setCreatorName(creator.getEmployeeName());
                    }

                    // 设置PDF信息
                    File pdfFile = quotationFileMap.get(quotation.getQuotationId());
                    if (pdfFile != null) {
                        response.setPdfFileId(pdfFile.getFileId());
                        response.setPdfGenerated(true);
                        response.setPdfUrl(pdfFile.getFileUrl());
                        response.setPdfFileName(pdfFile.getFileName());
                    } else {
                        response.setPdfGenerated(false);
                    }

                    // 设置客户快照
                    ContractCustomerSnapshot snapshot = deserializeCustomerSnapshot(quotation.getCustomerSnapshot());
                    if (snapshot == null && customer != null) {
                        snapshot = buildCustomerSnapshotFromCustomer(customer);
                    }
                    response.setCustomerSnapshot(snapshot);
                    if (customer != null) {
                        response.setCustomerName(customer.getEnterpriseName());
                        response.setCustomerId(customer.getCustomerId());
                    }
                    if ((response.getCustomerName() == null || response.getCustomerName().trim().isEmpty())
                            && snapshot != null) {
                        response.setCustomerName(snapshot.getCustomerName());
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationDetailResponse generatePdf(Integer quotationId) {
        Integer currentUserId = getCurrentUserId();

        // 查询报价单
        Quotation quotation = quotationMapper.selectById(quotationId);
        if (quotation == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "报价单不存在");
        }

        // 验证报价单号是否存在
        if (quotation.getQuotationNo() == null || quotation.getQuotationNo().trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "报价单号不能为空，无法生成PDF");
        }

        // 查询该报价单的所有PDF文件记录（包括已删除状态的），统一通过 FileService 删除，支持本地与云端
        List<File> existingFiles = fileMapper.selectByBusinessTypeAndId(QUOTATION_BUSINESS_TYPE, quotationId);
        if (!CollectionUtils.isEmpty(existingFiles)) {
            for (File file : existingFiles) {
                try {
                    boolean deleted = fileService.deleteFile(file.getFileId());
                    log.info("删除报价单PDF文件与记录：fileId={}, quotationId={}, deleted={}",
                            file.getFileId(), quotationId, deleted);
                } catch (Exception e) {
                    log.error("删除报价单PDF文件或记录异常：fileId={}, quotationId={}", file.getFileId(), quotationId, e);
                }
            }
        }

        // 获取报价单详情
        QuotationDetailResponse quotationDetail = getQuotationDetail(quotationId);

        // 生成PDF文件名：报价单_客户名称_报价单号.pdf
        String customerName = "";
        if (quotationDetail.getCustomerSnapshot() != null
                && quotationDetail.getCustomerSnapshot().getCustomerName() != null) {
            customerName = quotationDetail.getCustomerSnapshot().getCustomerName();
        }
        String quotationNo = quotation.getQuotationNo();
        String pdfFileName = customerName.isEmpty()
                ? quotationNo + ".pdf"
                : "报价单_" + customerName + "_" + quotationNo + ".pdf";
        // 过滤文件名中的非法字符
        pdfFileName = pdfFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 始终先在本地生成一个临时 PDF 文件，后续根据存储类型决定是直接使用还是上传到 OSS
        String localStoragePath = getLocalStoragePath();
        String datePath = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = QUOTATION_BUSINESS_TYPE + "/" + datePath + "/" + pdfFileName;
        String fullPath = localStoragePath + "/" + relativePath;

        // 创建目录
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(fullPath);
            java.nio.file.Files.createDirectories(filePath.getParent());

            // 生成本地 PDF 文件
            com.erp.util.QuotationPdfGenerator.generatePdf(quotationDetail, fullPath);

            java.io.File pdfFileObj = new java.io.File(fullPath);
            long fileSize = pdfFileObj.length();

            // 根据当前全局文件存储类型选择实际存储位置（本地 / OSS 等）
            FileStorageService storageService = fileService.getStorageService();
            File storedFile;

            // 如果是对象存储，实现为：将本地生成的 PDF 封装为 MultipartFile，通过 FileStorageService 上传至 OSS
            if (storageService != null && !(storageService instanceof LocalFileStorageServiceImpl)) {
                MultipartFile multipartFile = new LocalFileMultipartFile(pdfFileObj, "file", pdfFileName, "application/pdf");
                storedFile = storageService.uploadFile(multipartFile, QUOTATION_BUSINESS_TYPE, quotationId);

                // 填充通用业务字段
                storedFile.setBusinessModule(QUOTATION_BUSINESS_MODULE);
                storedFile.setBusinessId(quotationId);
                storedFile.setBusinessType(QUOTATION_BUSINESS_TYPE);
                storedFile.setFileStatus("正常");
                storedFile.setUploadTime(LocalDateTime.now());
                storedFile.setUploaderId(currentUserId);
                storedFile.setCreateTime(LocalDateTime.now());
                storedFile.setUpdateTime(LocalDateTime.now());

                fileMapper.insert(storedFile);

                // 对象存储上传成功后，本地临时文件可以删除
                boolean deleted = pdfFileObj.delete();
                if (!deleted) {
                    log.warn("删除本地临时报价单PDF文件失败：path={}", fullPath);
                }

                log.info("报价单PDF已上传到对象存储：quotationId={}, fileId={}, objectKey={}, url={}",
                        quotationId, storedFile.getFileId(), storedFile.getObjectKey(), storedFile.getFileUrl());
            } else {
                // 默认走本地存储（兼容原有逻辑）
                File pdfFile = new File();
                pdfFile.setFileName(pdfFileName);
                pdfFile.setFileType("PDF");
                pdfFile.setFileSize(fileSize);
                pdfFile.setStorageType("本地");
                pdfFile.setLocalPath(relativePath);
                pdfFile.setFileUrl("/api/file/download?path=" + relativePath);
                pdfFile.setBusinessModule(QUOTATION_BUSINESS_MODULE);
                pdfFile.setBusinessId(quotationId);
                pdfFile.setBusinessType(QUOTATION_BUSINESS_TYPE);
                pdfFile.setFileStatus("正常");
                pdfFile.setUploadTime(LocalDateTime.now());
                pdfFile.setUploaderId(currentUserId);
                pdfFile.setCreateTime(LocalDateTime.now());
                pdfFile.setUpdateTime(LocalDateTime.now());
                fileMapper.insert(pdfFile);

                storedFile = pdfFile;

                log.info("报价单PDF生成并保存在本地：quotationId={}, fileId={}, filePath={}, fileSize={}",
                        quotationId, pdfFile.getFileId(), fullPath, fileSize);
            }

        } catch (Exception e) {
            log.error("生成报价单PDF失败：quotationId={}", quotationId, e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "生成PDF失败：" + e.getMessage());
        }

        // 返回报价单详情（包含最新PDF信息）
        return getQuotationDetail(quotationId);
    }

    /**
     * 获取本地存储路径
     */
    private String getLocalStoragePath() {
        return localStoragePath;
    }

    /**
     * 基于本地文件的 MultipartFile 简单实现，用于将生成的 PDF 上传到对象存储
     */
    private static class LocalFileMultipartFile implements MultipartFile {

        private final java.io.File file;
        private final String name;
        private final String originalFilename;
        private final String contentType;

        private LocalFileMultipartFile(java.io.File file, String name, String originalFilename, String contentType) {
            this.file = file;
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return file == null || !file.exists() || file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws java.io.IOException {
            return java.nio.file.Files.readAllBytes(file.toPath());
        }

        @Override
        public java.io.InputStream getInputStream() throws java.io.IOException {
            return new java.io.FileInputStream(file);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.createDirectories(dest.toPath().getParent());
            java.nio.file.Files.copy(this.file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ================= 客户快照相关 =================

    private ContractCustomerSnapshot buildQuotationCustomerSnapshot(Customer customer,
                                                                    QuotationCreateRequest request,
                                                                    String resolvedPartyAName,
                                                                    String resolvedPartyAContact,
                                                                    String resolvedPartyAContactPhone,
                                                                    String resolvedPartyACreditCode) {
        if (customer != null) {
            // 优先根据正式客户生成
            ContractCustomerSnapshot snapshot = buildCustomerSnapshotFromCustomer(customer);
            // 如果前端抬头覆盖了客户档案，则同步覆盖快照中的名称/联系人/电话/统一社会信用代码
            if (StrUtil.isNotBlank(resolvedPartyAName)) {
                snapshot.setCustomerName(resolvedPartyAName.trim());
            }
            if (StrUtil.isNotBlank(resolvedPartyAContact)) {
                snapshot.setContactPerson(resolvedPartyAContact.trim());
            }
            if (StrUtil.isNotBlank(resolvedPartyAContactPhone)) {
                snapshot.setContactPhone(resolvedPartyAContactPhone.trim());
            }
            if (StrUtil.isNotBlank(resolvedPartyACreditCode)) {
                snapshot.setCreditCode(resolvedPartyACreditCode.trim());
            }
            return snapshot;
        }
        // 无正式客户时，允许通过 customerSnapshot 或甲方字段生成临时快照
        ContractCustomerSnapshot fromReq = sanitizeCustomerSnapshot(request.getCustomerSnapshot());
        if (fromReq != null) {
            // 如果前端提供了统一社会信用代码，覆盖快照中的值
            if (StrUtil.isNotBlank(resolvedPartyACreditCode)) {
                fromReq.setCreditCode(resolvedPartyACreditCode.trim());
            }
            return applyCustomerSnapshotDefaults(fromReq, fromReq.getCustomerType(), null);
        }
        // 回退到甲方字段
        if (StrUtil.isBlank(resolvedPartyAName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "临时客户名称不能为空");
        }
        ContractCustomerSnapshot snapshot = new ContractCustomerSnapshot();
        snapshot.setCustomerName(resolvedPartyAName.trim());
        snapshot.setContactPerson(StrUtil.trim(resolvedPartyAContact));
        snapshot.setContactPhone(StrUtil.trim(resolvedPartyAContactPhone));
        snapshot.setCreditCode(StrUtil.trim(resolvedPartyACreditCode));
        snapshot.setCustomerType("TEMPORARY");
        return applyCustomerSnapshotDefaults(snapshot, "TEMPORARY", null);
    }

    private ContractCustomerSnapshot resolveQuotationCustomerSnapshotForUpdate(Quotation quotation,
                                                                               QuotationUpdateRequest request) {
        ContractCustomerSnapshot fromReq = sanitizeCustomerSnapshot(request.getCustomerSnapshot());
        if (fromReq != null) {
            // 如果前端提供了统一社会信用代码，覆盖快照中的值
            if (StrUtil.isNotBlank(request.getPartyACreditCode())) {
                fromReq.setCreditCode(request.getPartyACreditCode().trim());
            }
            return applyCustomerSnapshotDefaults(fromReq, fromReq.getCustomerType(), null);
        }
        // 若未显式传入，但甲方抬头/联系人/电话/统一社会信用代码被修改，同步更新快照
        if (StrUtil.isNotBlank(request.getPartyAName()) || 
            StrUtil.isNotBlank(request.getPartyAContact()) || 
            StrUtil.isNotBlank(request.getPartyAContactPhone()) ||
            StrUtil.isNotBlank(request.getPartyACreditCode())) {
            // 解析现有快照
            ContractCustomerSnapshot existingSnapshot = deserializeCustomerSnapshot(quotation.getCustomerSnapshot());
            if (existingSnapshot != null) {
                // 更新快照中的字段
                if (StrUtil.isNotBlank(request.getPartyAName())) {
                    existingSnapshot.setCustomerName(request.getPartyAName().trim());
                }
                if (StrUtil.isNotBlank(request.getPartyAContact())) {
                    existingSnapshot.setContactPerson(request.getPartyAContact().trim());
                }
                if (StrUtil.isNotBlank(request.getPartyAContactPhone())) {
                    existingSnapshot.setContactPhone(request.getPartyAContactPhone().trim());
                }
                if (StrUtil.isNotBlank(request.getPartyACreditCode())) {
                    existingSnapshot.setCreditCode(request.getPartyACreditCode().trim());
                }
                return existingSnapshot;
            }
        }
        return null;
    }

    private ContractCustomerSnapshot buildCustomerSnapshotFromCustomer(Customer customer) {
        if (customer == null) {
            return null;
        }
        ContractCustomerSnapshot snapshot = new ContractCustomerSnapshot();
        snapshot.setCustomerId(customer.getCustomerId());
        snapshot.setCustomerName(customer.getEnterpriseName());
        snapshot.setCreditCode(customer.getCreditCode());
        snapshot.setAddress(customer.getAddress());
        snapshot.setContactPerson(customer.getContactPerson());
        snapshot.setContactPhone(customer.getContactPhone());
        snapshot.setLegalRepresentative(customer.getLegalRepresentative());
        snapshot.setOwnerEmployeeId(customer.getOwnerEmployeeId());
        snapshot.setRemark(customer.getRemark());
        snapshot.setCustomerType("EXISTING");
        return applyCustomerSnapshotDefaults(snapshot, "EXISTING", customer);
    }

    private ContractCustomerSnapshot sanitizeCustomerSnapshot(ContractCustomerSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        ContractCustomerSnapshot copy = new ContractCustomerSnapshot();
        copy.setCustomerId(snapshot.getCustomerId());
        copy.setCustomerName(StrUtil.trim(snapshot.getCustomerName()));
        copy.setCreditCode(StrUtil.trim(snapshot.getCreditCode()));
        copy.setAddress(StrUtil.trim(snapshot.getAddress()));
        copy.setContactPerson(StrUtil.trim(snapshot.getContactPerson()));
        copy.setContactPhone(StrUtil.trim(snapshot.getContactPhone()));
        copy.setLegalRepresentative(StrUtil.trim(snapshot.getLegalRepresentative()));
        copy.setOwnerEmployeeId(snapshot.getOwnerEmployeeId());
        copy.setOwnerEmployeeName(StrUtil.trim(snapshot.getOwnerEmployeeName()));
        copy.setRemark(StrUtil.trim(snapshot.getRemark()));
        copy.setCustomerType(snapshot.getCustomerType());
        return copy;
    }

    private ContractCustomerSnapshot applyCustomerSnapshotDefaults(ContractCustomerSnapshot snapshot,
                                                                   String preferredType,
                                                                   Customer customer) {
        if (snapshot == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "客户快照不能为空");
        }
        if (StrUtil.isBlank(snapshot.getCustomerName())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "客户快照缺少客户名称");
        }
        snapshot.setCustomerName(snapshot.getCustomerName().trim());
        String type = StrUtil.isBlank(preferredType) ? null : preferredType.trim().toUpperCase();
        if (!"EXISTING".equals(type) && !"TEMPORARY".equals(type)) {
            type = customer != null ? "EXISTING" : "TEMPORARY";
        }
        snapshot.setCustomerType(type);
        snapshot.setSnapshotTime(LocalDateTime.now());
        if (customer != null) {
            snapshot.setCustomerId(customer.getCustomerId());
            snapshot.setOwnerEmployeeId(customer.getOwnerEmployeeId());
        }
        return snapshot;
    }

    /**
     * 尝试根据信用代码匹配唯一客户，匹配成功返回客户，否则返回null。
     */
    private Customer tryResolveCustomerByCreditCode(ContractCustomerSnapshot snapshot) {
        if (snapshot == null || StrUtil.isBlank(snapshot.getCreditCode())) {
            return null;
        }
        String creditCode = StrUtil.trim(snapshot.getCreditCode());
        if (StrUtil.isBlank(creditCode)) {
            return null;
        }
        List<Customer> customers = customerMapper.selectListByCreditCode(creditCode);
        if (customers == null || customers.size() != 1) {
            return null;
        }
        return customers.get(0);
    }

    private String serializeCustomerSnapshot(ContractCustomerSnapshot snapshot) {
        if (snapshot == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("报价单客户快照序列化失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "报价单客户快照序列化失败");
        }
    }

    private ContractCustomerSnapshot deserializeCustomerSnapshot(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ContractCustomerSnapshot.class);
        } catch (Exception e) {
            log.warn("报价单客户快照解析失败，原始值：{}", json, e);
            return null;
        }
    }

    /**
     * 发送报价单审核结果通知
     * 
     * @param quotation 报价单
     * @param auditResult 审核结果（已通过/已驳回）
     * @param auditOpinion 审核意见
     * @param senderId 发送人ID（审核人）
     */
    private void sendQuotationAuditResultNotification(Quotation quotation, String auditResult, 
                                                      String auditOpinion, Integer senderId) {
        try {
            // 获取报价单创建人
            Integer creatorId = quotation.getCreatorId();
            if (creatorId == null) {
                log.warn("报价单创建人为空，无法发送审核结果通知：quotationId={}", quotation.getQuotationId());
                return;
            }

            // 验证创建人是否存在
            Employee creator = employeeMapper.selectById(creatorId);
            if (creator == null) {
                log.warn("报价单创建人不存在，无法发送审核结果通知：quotationId={}, creatorId={}", 
                        quotation.getQuotationId(), creatorId);
                return;
            }

            // 构建消息内容
            String title;
            String content;
            
            if ("已通过".equals(auditResult)) {
                title = "报价单审核通过通知";
                content = String.format("您提交的报价单《%s》已通过审核。", quotation.getQuotationNo());
                if (auditOpinion != null && !auditOpinion.trim().isEmpty()) {
                    content += String.format("审核意见：%s", auditOpinion.trim());
                }
            } else if ("已驳回".equals(auditResult)) {
                title = "报价单审核驳回通知";
                content = String.format("您提交的报价单《%s》审核未通过。", quotation.getQuotationNo());
                if (auditOpinion != null && !auditOpinion.trim().isEmpty()) {
                    content += String.format("驳回原因：%s", auditOpinion.trim());
                } else {
                    content += "请查看详情了解具体原因。";
                }
            } else {
                // 其他状态不发送通知
                return;
            }

            // 发送业务通知（使用基于权限的通知方法）
            messageNotificationService.sendAuditResultNotification(
                    "QUOTATION_AUDIT_RESULT",
                    quotation.getQuotationId(),
                    String.format("报价单【%s】", quotation.getQuotationNo()),
                    "已通过".equals(auditResult) ? "审核通过" : "审核驳回",
                    senderId
            );

            log.info("报价单审核结果通知已发送：quotationId={}, quotationNo={}, auditResult={}",
                    quotation.getQuotationId(), quotation.getQuotationNo(), auditResult);
        } catch (Exception e) {
            log.error("发送报价单审核结果通知异常：quotationId={}, auditResult={}", 
                    quotation.getQuotationId(), auditResult, e);
        }
    }

    /**
     * 发送报价单审核通知（提交审批时）
     * 发送给OA审批人员
     * 
     * @param quotation 报价单
     * @param customer 客户
     * @param senderId 发送人ID
     */
    private void sendQuotationAuditNotification(Quotation quotation, Customer customer, Integer senderId) {
        try {
            log.info("开始发送报价单审核通知：quotationId={}, quotationNo={}, senderId={}", 
                    quotation.getQuotationId(), quotation.getQuotationNo(), senderId);

            // 使用基于权限的通知方法，发送给OA审批人员
            String businessTitle = String.format("报价单【%s】", quotation.getQuotationNo());
            messageNotificationService.sendApprovalSubmitNotification(
                    "QUOTATION_SUBMIT",
                    quotation.getQuotationId(),
                    businessTitle,
                    senderId
            );

            log.info("报价单审核通知已发送：quotationId={}, quotationNo={}", 
                    quotation.getQuotationId(), quotation.getQuotationNo());
        } catch (Exception e) {
            log.error("发送报价单审核通知异常：quotationId={}, 错误: {}", quotation.getQuotationId(), e.getMessage(), e);
        }
    }

    /**
     * 从字符串中提取数值（去掉单位）
     * 例如："1500元/吨" -> "1500", "500元/每吨" -> "500"
     * 如果无法提取数字（如只有单位字符串"元/吨"），返回null
     */
    private String extractNumericValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        // 匹配开头的数字（包括小数）
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(value.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        // 如果无法提取数字，返回null而不是原字符串，避免保存单位字符串
        return null;
    }

    /**
     * 从备注中解析底价备注
     * 返回数组：[底价备注数值, 其他备注]
     */
    private String[] parseRemarkForFloorPrice(String remark) {
        if (StrUtil.isBlank(remark)) {
            return new String[]{null, null};
        }
        // 匹配 "底价备注：数值单位" 或 "底价备注：数值"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("底价备注[：:]\\s*([^；]*?)(?:；|$)");
        java.util.regex.Matcher matcher = pattern.matcher(remark);
        String floorPriceRemark = null;
        String otherRemark = remark;
        if (matcher.find()) {
            floorPriceRemark = matcher.group(1).trim();
            // 提取底价备注中的数值（去掉单位）
            floorPriceRemark = extractNumericValue(floorPriceRemark);
            // 从原备注中移除底价备注部分
            otherRemark = remark.replaceAll("底价备注[：:]\\s*[^；]*；?\\s*", "").trim();
            if (StrUtil.isBlank(otherRemark)) {
                otherRemark = null;
            }
        }
        return new String[]{floorPriceRemark, otherRemark};
    }

    /**
     * 计算小计摘要（按计量单位汇总）
     * 格式：JSON数组形式 [{unit,total}]
     * unit为子序号的计量单位字段，total为子序号的计划转移数量字段
     */
    private String calculateSubtotalSummary(String quotationMode, List<?> wasteItems, boolean isUpdate) {
        if (CollectionUtils.isEmpty(wasteItems)) {
            return null;
        }
        // 按计量单位分组，计算每个单位的计划转移数量总和
        Map<String, BigDecimal> unitTotalMap = new HashMap<>();
        // 用于收集不限量项（以序号为单位、total = -1）
        List<Map<String, Object>> unlimitedEntries = new ArrayList<>();
        for (int i = 0; i < wasteItems.size(); i++) {
            Object wasteItemObj = wasteItems.get(i);
            String unit = null;
            BigDecimal plannedQuantity = null;
            if (isUpdate) {
                QuotationUpdateRequest.QuotationWasteItemRequest wasteItemRequest =
                        (QuotationUpdateRequest.QuotationWasteItemRequest) wasteItemObj;
                unit = wasteItemRequest.getUnit();
                plannedQuantity = wasteItemRequest.getPlannedQuantity();
            } else {
                QuotationCreateRequest.QuotationWasteItemRequest wasteItemRequest =
                        (QuotationCreateRequest.QuotationWasteItemRequest) wasteItemObj;
                unit = wasteItemRequest.getUnit();
                plannedQuantity = wasteItemRequest.getPlannedQuantity();
            }
            // 如果为不限量（plannedQuantity == -1），则以序号表示该小计项并标记 total = -1
            if (plannedQuantity != null && plannedQuantity.compareTo(java.math.BigDecimal.valueOf(-1)) == 0) {
                Map<String, Object> unlimited = new HashMap<>();
                unlimited.put("unit", "序号" + String.valueOf(i + 1));
                unlimited.put("total", -1);
                unlimitedEntries.add(unlimited);
                continue;
            }
            // 常规数量累加（非不限量）
            if (StrUtil.isNotBlank(unit) && plannedQuantity != null) {
                unitTotalMap.merge(unit, plannedQuantity, BigDecimal::add);
            }
        }
        if (unitTotalMap.isEmpty() && unlimitedEntries.isEmpty()) {
            return null;
        }
        // 构建JSON数组：先加入按单位汇总的数值项，再追加不限量项（保留序号表示）
        List<Map<String, Object>> subtotalList = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : unitTotalMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("unit", entry.getKey());
            item.put("total", entry.getValue());
            subtotalList.add(item);
        }
        // 保持 unlimitedEntries 的顺序（按请求序号）
        if (!unlimitedEntries.isEmpty()) {
            subtotalList.addAll(unlimitedEntries);
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(subtotalList);
        } catch (Exception e) {
            log.error("生成小计摘要JSON失败", e);
            return null;
        }
    }

    /**
     * 从报价条目中提取计价方案信息
     * 返回包含highlights（高亮标签列表）和summary（摘要）的Map
     */
    private Map<String, Object> extractPricingPlanInfo(
            List<QuotationItem> items,
            Map<Integer, List<QuotationWasteItem>> wasteItemMap) {
        if (CollectionUtils.isEmpty(items)) {
            return null;
        }

        List<Map<String, Object>> highlights = new ArrayList<>();
        StringBuilder summaryBuilder = new StringBuilder();

        // 统计不同报价模式的数量
        long packageCount = items.stream()
                .filter(item -> "总价包干".equals(item.getQuotationMode()))
                .count();
        long unitCount = items.stream()
                .filter(item -> "按量结算".equals(item.getQuotationMode()))
                .count();

        // 处理总价包干模式的计价方案
        if (packageCount > 0) {
            for (QuotationItem item : items) {
                if (!"总价包干".equals(item.getQuotationMode())) {
                    continue;
                }
                String pricingPlan = item.getPricingPlan();
                if (StrUtil.isNotBlank(pricingPlan)) {
                    // 尝试解析计价方案，提取关键信息
                    // 格式可能是："1500元/年"、"包年 1500元"、"年度包干含固定额度，超额部分按吨结算"等
                    if (pricingPlan.contains("包年") || pricingPlan.contains("年度")) {
                        // 提取包年价格
                        String price = extractNumericValue(pricingPlan);
                        if (StrUtil.isNotBlank(price)) {
                            Map<String, Object> highlight = new HashMap<>();
                            highlight.put("label", "包年");
                            highlight.put("value", price + " 元/年");
                            highlight.put("tone", "primary");
                            highlights.add(highlight);
                        }
                    }
                    if (pricingPlan.contains("超量") || pricingPlan.contains("超额")) {
                        // 提取超量价格
                        String price = extractNumericValue(pricingPlan);
                        if (StrUtil.isNotBlank(price)) {
                            Map<String, Object> highlight = new HashMap<>();
                            highlight.put("label", "超量");
                            highlight.put("value", price + " 元/吨");
                            highlight.put("tone", "danger");
                            highlights.add(highlight);
                        }
                    }
                }
            }
            if (packageCount > 0 && summaryBuilder.length() == 0) {
                summaryBuilder.append("年度包干含固定额度");
                if (unitCount > 0) {
                    summaryBuilder.append("，超额部分按吨结算");
                }
            }
        }

        // 处理按量结算模式的计价方案
        if (unitCount > 0) {
            Map<String, Integer> priceCountMap = new HashMap<>();
            for (QuotationItem item : items) {
                if (!"按量结算".equals(item.getQuotationMode())) {
                    continue;
                }
                List<QuotationWasteItem> wasteItems = wasteItemMap.get(item.getQuotationItemId());
                if (!CollectionUtils.isEmpty(wasteItems)) {
                    for (QuotationWasteItem wasteItem : wasteItems) {
                        String pricingPlan = wasteItem.getPricingPlan();
                        if (StrUtil.isNotBlank(pricingPlan)) {
                            // 提取价格数值
                            String price = extractNumericValue(pricingPlan);
                            if (StrUtil.isNotBlank(price)) {
                                String priceKey = price + " 元/吨";
                                priceCountMap.put(priceKey, priceCountMap.getOrDefault(priceKey, 0) + 1);
                            }
                        }
                    }
                }
            }
            // 构建按量结算的高亮标签
            if (!priceCountMap.isEmpty()) {
                // 如果只有一个价格，显示为标准价
                if (priceCountMap.size() == 1) {
                    String price = priceCountMap.keySet().iterator().next();
                    Map<String, Object> highlight = new HashMap<>();
                    highlight.put("label", "标准价");
                    highlight.put("value", price);
                    highlight.put("tone", "success");
                    highlights.add(highlight);
                } else {
                    // 多个价格时，显示最常见的价格
                    String mostCommonPrice = priceCountMap.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    if (StrUtil.isNotBlank(mostCommonPrice)) {
                        Map<String, Object> highlight = new HashMap<>();
                        highlight.put("label", "标准价");
                        highlight.put("value", mostCommonPrice);
                        highlight.put("tone", "success");
                        highlights.add(highlight);
                    }
                }
            }
            if (unitCount > 0 && summaryBuilder.length() == 0) {
                summaryBuilder.append("按实际处置量结算");
            }
        }

        // 组合计价模式
        if (packageCount > 0 && unitCount > 0) {
            summaryBuilder.setLength(0);
            summaryBuilder.append("组合计价：常规量包干，超量按吨");
        }

        // 如果没有提取到任何信息，使用默认摘要
        if (highlights.isEmpty() && summaryBuilder.length() == 0) {
            if (packageCount > 0) {
                summaryBuilder.append("总价包干模式");
            } else if (unitCount > 0) {
                summaryBuilder.append("按量结算模式");
            } else {
                summaryBuilder.append("组合计价模式");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("highlights", highlights);
        result.put("summary", summaryBuilder.toString());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OaApprovalSubmitResult submitForApproval(Integer quotationId) {
        Integer currentUserId = getCurrentUserId();

        // 查询报价单
        Quotation quotation = quotationMapper.selectById(quotationId);
        if (quotation == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "报价单不存在");
        }

        // 验证状态：只有"待审核"或"已驳回"状态的报价单才能提交审核
        String currentStatus = quotation.getQuotationStatus();
        if (!"待审核".equals(currentStatus) && !"已驳回".equals(currentStatus)) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(),
                    "只有待审核或已驳回状态的报价单才能提交审核");
        }

        // 获取当前员工信息
        Employee employee = employeeMapper.selectById(currentUserId);
        String employeeName = employee != null ? employee.getEmployeeName() : "";

        // 调用OA审批服务提交审核
        com.erp.entity.oa.OaApprovalRecord oaRecord;
        if ("已驳回".equals(currentStatus)) {
            // 已驳回状态：重新激活原有的OA审批记录（审核次数+1，状态改为待审核）
            oaRecord = oaApprovalRecordService.reactivateRejectedRecord(
                    "QUOTATION",
                    quotationId,
                    currentUserId,
                    employeeName
            );
        } else {
            // 待审核状态：创建新的OA审批记录
            // 先检查是否已有待审核的OA审批记录（防止重复提交）
            com.erp.entity.oa.OaApprovalRecord existingOaRecord =
                    oaApprovalRecordService.findPendingBySource("QUOTATION", quotationId);
            if (existingOaRecord != null) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                        "该报价单已在OA审批中，请勿重复提交");
            }

            oaRecord = oaApprovalRecordService.submit(
                    "QUOTATION",                    // 来源表名
                    quotationId,                     // 来源记录ID
                    "报价单信息表",                  // 来源表中文名称
                    quotation.getQuotationNo(),     // 关联单号（报价单号）
                    "报价单审核：" + quotation.getQuotationNo(), // 审核标题
                    currentUserId,                   // 提交人ID
                    employeeName                    // 提交人姓名
            );
        }

        // 更新报价单状态为"审核中"
        quotation.setQuotationStatus("审核中");
        quotation.setUpdateTime(LocalDateTime.now());
        quotationMapper.updateById(quotation);

        // 发送通知给有OA审批权限的人员
        try {
            String businessTitle = String.format("报价单【%s】",
                    quotation.getQuotationNo() != null ? quotation.getQuotationNo() : "BJ" + quotationId);
            messageNotificationService.sendApprovalSubmitNotification(
                    "QUOTATION_SUBMIT",
                    quotationId,
                    businessTitle,
                    currentUserId
            );
        } catch (Exception e) {
            log.warn("发送报价单提交审核通知失败（不影响主流程）：quotationId={}", quotationId, e);
        }

        // 构建返回结果
        OaApprovalSubmitResult result = new OaApprovalSubmitResult();
        result.setApprovalRecordId(oaRecord.getApprovalRecordId());
        result.setApprovalNo(oaRecord.getApprovalNo());

        log.info("报价单提交OA审核成功：quotationId={}, oaRecordId={}, approvalNo={}, approvalCount={}, submitter={}",
                quotationId, oaRecord.getApprovalRecordId(), oaRecord.getApprovalNo(), oaRecord.getApprovalCount(), currentUserId);

        return result;
    }

    /**
     * 同步更新OA审核记录表状态
     *
     * @param quotationId 报价单编号
     * @param auditResult 审核结果（已通过/已驳回）
     * @param approverId 审核人ID
     * @param approverName 审核人姓名
     */
    private void updateOaApprovalRecord(Integer quotationId, String auditResult, Integer approverId, String approverName) {
        try {
            // 查询报价单对应的待审核OA记录
            com.erp.entity.oa.OaApprovalRecord oaRecord =
                oaApprovalRecordService.findPendingBySource("QUOTATION", quotationId);

            if (oaRecord == null) {
                log.warn("未找到报价单对应的待审核OA记录：quotationId={}", quotationId);
                return;
            }

            // 将报价单审核结果映射为OA审核结果（已通过->通过，已驳回->驳回）
            // approve()方法期望接收 "通过" 或 "驳回"
            String oaResult = "已通过".equals(auditResult) ? "通过" : "驳回";

            // 调用OA审核服务更新记录
            oaApprovalRecordService.approve(
                oaRecord.getApprovalRecordId(),
                "QUOTATION",
                quotationId,
                oaResult,
                null,
                approverId,
                approverName
            );

            log.info("同步更新OA审核记录成功：quotationId={}, oaRecordId={}, oaResult={}",
                quotationId, oaRecord.getApprovalRecordId(), oaResult);

        } catch (Exception e) {
            log.error("同步更新OA审核记录失败：quotationId={}, auditResult={}", quotationId, auditResult, e);
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRevoke(List<Integer> quotationIds) {
        if (quotationIds == null || quotationIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要撤回的报价单");
        }

        Integer currentUserId = getCurrentUserId();
        List<Quotation> quotations = quotationMapper.selectBatchIds(quotationIds);

        if (quotations.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "未找到要撤回的报价单");
        }

        for (Quotation quotation : quotations) {
            // 验证状态：只有"审核中"状态的报价单才能撤回
            if (!"审核中".equals(quotation.getQuotationStatus())) {
                throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(),
                        "报价单[" + quotation.getQuotationNo() + "]不是审核中状态，无法撤回");
            }
        }

        // 批量更新状态为"待审核"
        for (Quotation quotation : quotations) {
            quotation.setQuotationStatus("待审核");
            quotation.setUpdateTime(LocalDateTime.now());
            quotationMapper.updateById(quotation);

            // 取消关联的OA审批记录
            try {
                com.erp.entity.oa.OaApprovalRecord oaRecord =
                        oaApprovalRecordService.findPendingBySource("QUOTATION", quotation.getQuotationId());
                if (oaRecord != null) {
                    oaApprovalRecordService.cancel(oaRecord.getApprovalRecordId(), "QUOTATION",
                            quotation.getQuotationId(), currentUserId, "报价单撤回");
                }
            } catch (Exception e) {
                log.warn("取消OA审批记录失败：quotationId={}", quotation.getQuotationId(), e);
            }

            // 发送撤回通知给有OA审批权限的人员
            try {
                String businessTitle = String.format("报价单【%s】",
                        quotation.getQuotationNo() != null ? quotation.getQuotationNo() : "BJ" + quotation.getQuotationId());
                messageNotificationService.sendApprovalRevokeNotification(
                        "QUOTATION_REVOKE",
                        quotation.getQuotationId(),
                        businessTitle,
                        currentUserId
                );
            } catch (Exception e) {
                log.warn("发送报价单撤回通知失败（不影响主流程）：quotationId={}", quotation.getQuotationId(), e);
            }
        }

        log.info("批量撤回报价单成功：quotationIds={}, operator={}", quotationIds, currentUserId);
    }
}

