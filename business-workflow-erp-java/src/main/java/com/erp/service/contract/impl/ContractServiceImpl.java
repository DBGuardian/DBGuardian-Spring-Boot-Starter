package com.erp.service.contract.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.contract.dto.*;
import org.springframework.cache.annotation.Cacheable;
import com.erp.entity.common.File;
import com.erp.entity.contract.Contract;
import com.erp.entity.contract.BusinessContract;
import com.erp.entity.contract.ContractApprovalFlow;
import com.erp.entity.contract.ContractItem;
import com.erp.entity.contract.ContractWasteItem;
import com.erp.entity.contract.Salesperson;
import com.erp.entity.oa.OaApprovalRecord;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.entity.customer.Customer;
import com.erp.entity.system.Employee;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.mapper.common.FileMapper;
import com.erp.mapper.contract.BusinessContractMapper;
import com.erp.mapper.contract.ContractMapper;
import com.erp.mapper.customer.CustomerMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.common.FileService;
import com.erp.service.auth.AuthService;
import com.erp.service.contract.BusinessContractService;
import com.erp.service.contract.ContractService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.service.oa.OaApprovalRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.erp.service.common.FileStorageService;

/**
 * 合同管理服务实现
 */
@Slf4j
@Service
public class ContractServiceImpl implements ContractService {

    private static final String CONTRACT_BUSINESS_TYPE = "CONTRACT";
    private static final String QUOTATION_BUSINESS_TYPE = "QUOTATION";
    private static final String CONTRACT_BUSINESS_MODULE = "合同";

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private BusinessContractMapper businessContractMapper;

    @Autowired
    private com.erp.mapper.contract.ContractItemMapper contractItemMapper;

    @Autowired
    private com.erp.mapper.contract.ContractWasteItemMapper contractWasteItemMapper;

    @Autowired
    private com.erp.mapper.contract.OutOfScopeServiceMapper outOfScopeServiceMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @Autowired
    private AuthService authService;
    
    @Autowired
    private OaApprovalRecordService oaApprovalRecordService;
    
    @Autowired
    private com.erp.service.contract.ContractApprovalFlowService contractApprovalFlowService;

    @Autowired
    private com.erp.mapper.contract.ContractApprovalFlowMapper contractApprovalFlowMapper;

    @Autowired
    private com.erp.service.contract.BusinessContractService businessContractService;

    @Autowired
    private com.erp.mapper.contract.SalespersonMapper salespersonMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private com.erp.mapper.production.PickupNoticeItemMapper pickupNoticeItemMapper;

    @Autowired
    private com.erp.mapper.system.HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private com.erp.mapper.system.HazardousWasteCategoryMapper hazardousWasteCategoryMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @org.springframework.beans.factory.annotation.Value("${file.storage.local.path:D:/erp}")
    private String localStoragePath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractDetailResponse createContract(ContractCreateRequest request, MultipartFile contractFile) {
        Integer currentUserId = getCurrentUserId();

        // 如果提供了客户ID，验证客户是否存在；如果没有提供，允许手填甲方信息
        Integer customerId = null;
        Customer customer = null;
        ContractCustomerSnapshot customerSnapshot = sanitizeSnapshot(request.getCustomerSnapshot());
        if (request.getCustomerId() != null) {
            customer = customerMapper.selectDetailById(request.getCustomerId());
            if (customer == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
            }
            customerId = request.getCustomerId();
        } else {
            // customerId 为空时，尝试根据快照中的信用代码匹配客户
            customer = tryResolveCustomerByCreditCode(customerSnapshot);
            if (customer != null) {
                customerId = customer.getCustomerId();
            }
        }
        // 客户快照：完全由前端构造，后端只做简单清洗与透传；未提供则不写入（保持为null）
        String customerSnapshotJson = customerSnapshot != null ? serializeSnapshot(customerSnapshot) : null;

        // 验证报价项列表
        if (CollectionUtils.isEmpty(request.getQuotationItems())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "至少需要一条废物报价信息");
        }

        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("甲方联系电话", request.getPartyAContactPhone());
        validatePhoneFormat("乙方联系电话", request.getPartyBContactPhone());

        Integer quotationId = null;

        // 上传合同PDF
        Integer contractFileId = null;
        if (contractFile != null && !contractFile.isEmpty()) {
            File file = fileService.uploadAndSave(contractFile, "CONTRACT", null);
            contractFileId = file.getFileId();
        }

        // 创建合同（不再关联报价单）
        Contract contract = buildContractEntity(request, customerId,
                quotationId, contractFileId, currentUserId, customerSnapshotJson);
        contractMapper.insert(contract);
        log.info("创建合同成功：contractId={}, customerId={}, operator={}",
                contract.getContractId(), contract.getCustomerId(), currentUserId);

        // 创建合同条目和危废条目明细（合同特有的数据）
        createContractItemsInternal(contract.getContractId(), request.getQuotationItems(), null, currentUserId);

        // 创建审批流记录（合同创建）
        try {
            contractApprovalFlowService.createContractCreationFlow(contract.getContractId(), currentUserId);
        } catch (Exception e) {
            log.error("创建合同审批流记录失败：contractId={}, contractNo={}", 
                    contract.getContractId(), contract.getContractNo(), e);
            // 审批流创建失败不影响主流程，只记录日志
        }

        // 发送合同生成通知给合同相关人员（如果有客户信息）
        if (customer != null) {
            try {
                sendContractCreatedNotification(contract, customer, currentUserId);
            } catch (Exception e) {
                log.error("发送合同生成通知失败：contractId={}, contractNo={}", 
                        contract.getContractId(), contract.getContractNo(), e);
                // 消息发送失败不影响主流程，只记录日志
            }
        }

        ContractDetailResponse response = buildDetailResponse(contract, customer, contractFileId);
        
        // ==== 创建阶段：如果前端传入价外服务，则一并保存到 OUT_OF_SCOPE_SERVICE 表（合同业务类型） ====
        try {
            List<com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO> incomingServices = request.getOutOfScopeServices();
            if (incomingServices != null && !incomingServices.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                for (com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO dto : incomingServices) {
                    com.erp.entity.contract.OutOfScopeService entity = new com.erp.entity.contract.OutOfScopeService();
                    entity.setBusinessType(CONTRACT_BUSINESS_TYPE);
                    entity.setBusinessId(contract.getContractId());
                    String projectValue = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
                    entity.setProject(projectValue != null ? projectValue : "");
                    entity.setSpec(dto.getSpec());
                    entity.setUnit(dto.getUnit());
                    entity.setPlannedQuantity(dto.getPlannedQuantity());
                    entity.setContractUnitPrice(dto.getContractUnitPrice());
                    entity.setStatus("ACTIVE");
                    entity.setCreatedAt(now);
                    entity.setCreatedBy(currentUserId);
                    outOfScopeServiceMapper.insert(entity);
                }
            }
        } catch (Exception e) {
            log.error("创建合同时批量保存价外服务失败：contractId={}, error={}", contract.getContractId(), e.getMessage(), e);
            // 不抛出，创建合同本身不受影响；如果需要严格回滚可改为抛出 BusinessException
        }

        // 记录数据变更日志
        try {
            logRecordService.recordDataChangeLog("合同管理", "CONTRACT", 
                    String.valueOf(contract.getContractId()), "新增", 
                    "新增合同：" + contract.getContractNo(), 
                    null, response, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录合同新增数据变更日志失败", e);
        }
        
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(ContractUpdateRequest request, MultipartFile contractFile) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        Contract contract = contractMapper.selectById(request.getContractId());
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        // 应用操作范围控制（operateScope），与前端“仅操作自己/操作全部”配置保持一致
        if (!admin) {
            EmployeePermission permission =
                    getEmployeePagePermission(currentUserId, "合同管理:合同变更:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
                // 仅能编辑自己创建的合同
                if (!Objects.equals(contract.getCreatorId(), currentUserId)) {
                    throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能编辑自己创建的合同");
                }
            }
            // operateScope=ALL 或未配置时，不额外限制，具体动作权限已由前端按钮和切面控制
        }

        // 保存旧数据用于日志记录
        ContractDetailResponse oldDetail = null;
        try {
            oldDetail = getContractDetail(request.getContractId());
        } catch (Exception e) {
            log.warn("获取合同旧数据失败，将跳过数据变更日志记录", e);
        }

        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("甲方联系电话", request.getPartyAContactPhone());
        validatePhoneFormat("乙方联系电话", request.getPartyBContactPhone());

        // 更新客户编码（如果提供了新的客户ID）
        Customer newCustomer = null;
        if (request.getCustomerId() != null) {
            // 验证新客户是否存在
            newCustomer = customerMapper.selectDetailById(request.getCustomerId());
            if (newCustomer == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
            }
            contract.setCustomerId(request.getCustomerId());
        } else {
            // customerId 为空时，尝试根据快照中的信用代码匹配客户，匹配到则回填
            ContractCustomerSnapshot snapshotForMatch = sanitizeSnapshot(request.getCustomerSnapshot());
            Customer matched = tryResolveCustomerByCreditCode(snapshotForMatch);
            if (matched != null) {
                contract.setCustomerId(matched.getCustomerId());
                newCustomer = matched;
            }
        }

        if (request.getPartyAName() != null) {
            contract.setPartyAName(request.getPartyAName());
        }
        if (request.getPartyAContact() != null) {
            contract.setPartyAContact(request.getPartyAContact());
        }
        if (request.getPartyAContactPhone() != null) {
            contract.setPartyAContactPhone(request.getPartyAContactPhone());
        }
        if (request.getPartyACreditCode() != null) {
            contract.setPartyACreditCode(request.getPartyACreditCode());
        }
        if (request.getPartyBName() != null) {
            contract.setPartyBName(request.getPartyBName());
        }
        if (request.getPartyBContact() != null) {
            contract.setPartyBContact(request.getPartyBContact());
        }
        if (request.getPartyBContactPhone() != null) {
            contract.setPartyBContactPhone(request.getPartyBContactPhone());
        }
        if (request.getPartyBCreditCode() != null) {
            contract.setPartyBCreditCode(request.getPartyBCreditCode());
        }
        
        if (request.getContractAmount() != null) {
            contract.setContractAmount(request.getContractAmount());
        }
        if (request.getFeeSettlementEnabled() != null) {
            contract.setFeeSettlementEnabled(request.getFeeSettlementEnabled());
        }
        if (request.getSignTime() != null) {
            contract.setSignTime(request.getSignTime());
        }
        if (request.getContractStatus() != null) {
            contract.setContractStatus(request.getContractStatus());
        }
        if (request.getValidFrom() != null) {
            contract.setValidFrom(request.getValidFrom());
        }
        if (request.getValidTo() != null) {
            contract.setValidTo(request.getValidTo());
        }
        if (request.getNumberGenerationMode() != null) {
            contract.setNumberGenerationMode(request.getNumberGenerationMode());
        }
        if (request.getSendDate() != null) {
            contract.setSendDate(request.getSendDate());
        }
        if (request.getReceiveDate() != null) {
            contract.setReceiveDate(request.getReceiveDate());
        }
        if (request.getScanFilePath() != null) {
            contract.setScanFilePath(request.getScanFilePath());
        }
        if (request.getOwnerEmployeeId() != null) {
            // 校验业务员编码：ownerEmployeeId 是 SALESPERSON.业务员编号
            Integer ownerEmployeeId = request.getOwnerEmployeeId();
            if (ownerEmployeeId > 0) {
                // 验证业务员编号在 SALESPERSON 表中是否存在
                Salesperson salesperson = salespersonMapper.selectById(ownerEmployeeId);
                if (salesperson != null) {
                    log.info("更新合同时业务员编号验证通过：salespersonId={}, salespersonName={}", ownerEmployeeId, salesperson.getSalespersonName());
                    contract.setOwnerEmployeeId(ownerEmployeeId);
                } else {
                    log.warn("更新合同时传入的业务员编号在业务员表中不存在：ownerEmployeeId={}，跳过设置", ownerEmployeeId);
                }
            } else {
                log.warn("更新合同时传入的业务员编码无效：ownerEmployeeId={}，跳过设置", ownerEmployeeId);
            }
        }
        if (request.getRemark() != null) {
            contract.setRemark(request.getRemark());
        }

        // 客户快照：仅当前端显式传入时才覆盖，未传入则保持原值不变
        ContractCustomerSnapshot snapshotFromRequest = sanitizeSnapshot(request.getCustomerSnapshot());
        if (snapshotFromRequest != null) {
            contract.setCustomerSnapshot(serializeSnapshot(snapshotFromRequest));
        }

        // 替换合同文件
        Integer oldFileId = null;
        if (contractFile != null && !contractFile.isEmpty()) {
            oldFileId = contract.getContractFileId();
            File file = fileService.uploadAndSave(contractFile, "CONTRACT", null);
            contract.setContractFileId(file.getFileId());
        }

        contract.setUpdateTime(LocalDateTime.now());
        int contractRows = contractMapper.updateById(contract);
        if (contractRows == 0) {
            throw new BusinessException("更新合同失败：记录已被其他用户修改");
        }

        // 更新合同条目和危废条目明细（如果提供了quotationItems）
        if (request.getQuotationItems() != null && !request.getQuotationItems().isEmpty()) {
            // 查询旧的合同条目和危废条目明细
            List<ContractItem> oldItems = contractItemMapper.selectByContractId(contract.getContractId());
            
            // 收集前端传来的所有合同条目ID和危废明细ID
            Set<Integer> newItemIds = new HashSet<>();
            Set<Integer> newWasteItemIds = new HashSet<>();
            for (ContractUpdateRequest.ContractItemRequest itemRequest : request.getQuotationItems()) {
                if (itemRequest.getContractItemId() != null) {
                    newItemIds.add(itemRequest.getContractItemId());
                }
                if (!CollectionUtils.isEmpty(itemRequest.getWasteItems())) {
                    for (ContractUpdateRequest.ContractWasteItemRequest wasteRequest : itemRequest.getWasteItems()) {
                        if (wasteRequest.getContractWasteItemId() != null) {
                            newWasteItemIds.add(wasteRequest.getContractWasteItemId());
                        }
                    }
                }
            }
            
            log.info("合同更新 - 收集到的ID：contractId={}, newItemIds={}, newWasteItemIds={}", 
                    contract.getContractId(), newItemIds, newWasteItemIds);
            
            if (!CollectionUtils.isEmpty(oldItems)) {
                List<Integer> oldItemIds = oldItems.stream()
                        .map(ContractItem::getContractItemId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                
                // 查询所有旧的合同危废明细
                List<ContractWasteItem> oldWasteItems = contractWasteItemMapper.selectByContractItemIds(oldItemIds);
                
                // 找出需要删除的危废明细（不在新数据中的旧记录）
                List<Integer> wasteItemIdsToDelete = new ArrayList<>();
                if (!CollectionUtils.isEmpty(oldWasteItems)) {
                    List<Integer> oldWasteItemIds = oldWasteItems.stream()
                            .map(ContractWasteItem::getContractWasteItemId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    log.info("合同更新 - 旧的危废明细ID：contractId={}, oldWasteItemIds={}", 
                            contract.getContractId(), oldWasteItemIds);
                    
                    for (ContractWasteItem oldWasteItem : oldWasteItems) {
                        Integer wasteItemId = oldWasteItem.getContractWasteItemId();
                        if (wasteItemId != null && !newWasteItemIds.contains(wasteItemId)) {
                            wasteItemIdsToDelete.add(wasteItemId);
                        }
                    }
                    
                    log.info("合同更新 - 需要删除的危废明细ID：contractId={}, wasteItemIdsToDelete={}", 
                            contract.getContractId(), wasteItemIdsToDelete);
                }
                
                // 查询哪些需要删除的合同危废明细被收运通知单引用，并将关联字段置空
                if (!CollectionUtils.isEmpty(wasteItemIdsToDelete)) {
                    for (Integer wasteItemId : wasteItemIdsToDelete) {
                        List<com.erp.entity.production.PickupNoticeItem> referencedItems = 
                                pickupNoticeItemMapper.selectByContractWasteItemId(wasteItemId);
                        if (!CollectionUtils.isEmpty(referencedItems)) {
                            // 将收运通知单明细的合同危废明细编号字段置空，而不是删除记录
                            for (com.erp.entity.production.PickupNoticeItem item : referencedItems) {
                                try {
                                    com.erp.entity.production.PickupNoticeItem updateItem =
                                            new com.erp.entity.production.PickupNoticeItem();
                                    updateItem.setItemId(item.getItemId());
                                    updateItem.setContractWasteItemId(null); // 将关联字段置空
                                    int rows = pickupNoticeItemMapper.updateById(updateItem);
                                    if (rows == 0) {
                                        log.warn("清空收运通知单明细关联失败（乐观锁冲突）：itemId={}", item.getItemId());
                                    }
                                    log.info("清空收运通知单明细的合同危废明细关联：itemId={}, contractWasteItemId={}",
                                            item.getItemId(), wasteItemId);
                                } catch (Exception e) {
                                    log.error("清空收运通知单明细关联失败：itemId={}, contractWasteItemId={}, error={}",
                                            item.getItemId(), wasteItemId, e.getMessage(), e);
                                    // 继续处理其他记录，不中断流程
                                }
                            }
                        }
                    }
                    
                    // 删除不在新数据中的旧合同危废明细（现在外键约束已解除）
                    for (Integer wasteItemId : wasteItemIdsToDelete) {
                        try {
                            int deletedCount = contractWasteItemMapper.deleteById(wasteItemId);
                            if (deletedCount > 0) {
                                log.info("成功删除合同危废明细：wasteItemId={}", wasteItemId);
                            } else {
                                log.warn("删除合同危废明细失败，记录不存在：wasteItemId={}", wasteItemId);
                            }
                        } catch (Exception e) {
                            // 如果仍然遇到外键约束错误，再次尝试清空关联
                            if (e.getMessage() != null && e.getMessage().contains("foreign key constraint")) {
                                log.warn("删除合同危废明细时遇到外键约束，尝试再次清空关联：wasteItemId={}", wasteItemId);
                                try {
                                    List<com.erp.entity.production.PickupNoticeItem> remainingRefs =
                                            pickupNoticeItemMapper.selectByContractWasteItemId(wasteItemId);
                                    if (!CollectionUtils.isEmpty(remainingRefs)) {
                                        for (com.erp.entity.production.PickupNoticeItem item : remainingRefs) {
                                            com.erp.entity.production.PickupNoticeItem updateItem =
                                                    new com.erp.entity.production.PickupNoticeItem();
                                            updateItem.setItemId(item.getItemId());
                                            updateItem.setContractWasteItemId(null);
                                            int rows = pickupNoticeItemMapper.updateById(updateItem);
                                            if (rows == 0) {
                                                log.warn("清空收运通知单明细关联失败（乐观锁冲突）：itemId={}", item.getItemId());
                                            }
                                        }
                                        // 再次尝试删除危废明细
                                        int delRows = contractWasteItemMapper.deleteById(wasteItemId);
                                        if (delRows > 0) {
                                            log.info("重试后成功删除合同危废明细：wasteItemId={}", wasteItemId);
                                        }
                                    }
                                } catch (Exception retryException) {
                                    log.error("重试删除合同危废明细仍然失败：wasteItemId={}, error={}", 
                                            wasteItemId, retryException.getMessage(), retryException);
                                }
                            } else {
                                log.error("删除合同危废明细异常：wasteItemId={}, error={}", wasteItemId, e.getMessage(), e);
                            }
                            // 继续删除其他记录，不中断流程
                        }
                    }
                }
                
                // 删除危废明细后，重新查询剩余的危废明细，用于判断合同条目是否可以删除
                List<ContractWasteItem> remainingWasteItems = contractWasteItemMapper.selectByContractItemIds(oldItemIds);
                
                // 找出需要删除的合同条目（不在新数据中的旧记录，且该条目下没有剩余的危废明细）
                List<Integer> itemIdsToDelete = new ArrayList<>();
                for (ContractItem oldItem : oldItems) {
                    Integer itemId = oldItem.getContractItemId();
                    if (itemId != null && !newItemIds.contains(itemId)) {
                        // 检查该条目是否还有剩余的危废明细（基于删除后的状态）
                        boolean hasRemainingWasteItems = remainingWasteItems.stream()
                                .anyMatch(w -> Objects.equals(w.getContractItemId(), itemId));
                        if (!hasRemainingWasteItems) {
                            itemIdsToDelete.add(itemId);
                            log.info("合同更新 - 识别到需要删除的合同条目：itemId={}, 该条目下没有剩余的危废明细", itemId);
                        } else {
                            log.info("合同更新 - 跳过删除合同条目：itemId={}, 该条目下还有剩余的危废明细", itemId);
                        }
                    }
                }
                
                log.info("合同更新 - 需要删除的合同条目ID：contractId={}, itemIdsToDelete={}, count={}", 
                        contract.getContractId(), itemIdsToDelete, itemIdsToDelete.size());
                
                // 删除不在新数据中的旧合同条目
                if (!CollectionUtils.isEmpty(itemIdsToDelete)) {
                    for (Integer itemId : itemIdsToDelete) {
                        try {
                            int deletedCount = contractItemMapper.deleteById(itemId);
                            if (deletedCount > 0) {
                                log.info("成功删除合同条目：itemId={}", itemId);
                            } else {
                                log.warn("删除合同条目失败，记录不存在：itemId={}", itemId);
                            }
                        } catch (Exception e) {
                            log.error("删除合同条目异常：itemId={}, error={}", itemId, e.getMessage(), e);
                            // 继续删除其他记录，不中断流程
                        }
                    }
                }
            }
            
            // 创建或更新合同条目和危废条目明细（对于传了ID的记录会更新，没传ID的记录会插入）
            createContractItemsForUpdate(contract.getContractId(), request.getQuotationItems(), null, currentUserId);
        }
        
        // ===== 处理合同的价外服务差分同步（新增/更新/删除） =====
        try {
            List<com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO> incomingServices = request.getOutOfScopeServices();
            if (incomingServices != null) {
                // 查询当前数据库中的合同价外服务
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.contract.OutOfScopeService> wrapper =
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                wrapper.eq("关联业务类型", CONTRACT_BUSINESS_TYPE).eq("关联业务单号", contract.getContractId());
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
                            // 更新字段（优先使用 dto.project，其次兼容 dto.serviceType）
                            String updatedProject = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
                            exist.setProject(updatedProject != null ? updatedProject : exist.getProject());
                            exist.setSpec(dto.getSpec() != null ? dto.getSpec() : exist.getSpec());
                            exist.setUnit(dto.getUnit() != null ? dto.getUnit() : exist.getUnit());
                            exist.setPlannedQuantity(dto.getPlannedQuantity() != null ? dto.getPlannedQuantity() : exist.getPlannedQuantity());
                            exist.setContractUnitPrice(dto.getContractUnitPrice() != null ? dto.getContractUnitPrice() : exist.getContractUnitPrice());
                            exist.setUpdatedAt(now);
                            exist.setUpdatedBy(currentUserId);
                            int rows = outOfScopeServiceMapper.updateById(exist);
                            if (rows == 0) {
                                log.warn("更新价外服务失败（乐观锁冲突），serviceId={}", exist.getOutOfScopeServiceId());
                            }
                            keepIds.add(exist.getOutOfScopeServiceId());
                        } else {
                            // id 不存在，按新增处理
                            com.erp.entity.contract.OutOfScopeService newEntity = new com.erp.entity.contract.OutOfScopeService();
                            newEntity.setBusinessType(CONTRACT_BUSINESS_TYPE);
                            newEntity.setBusinessId(contract.getContractId());
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
                        newEntity.setBusinessType(CONTRACT_BUSINESS_TYPE);
                        newEntity.setBusinessId(contract.getContractId());
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
                        int delRows = outOfScopeServiceMapper.deleteById(s.getOutOfScopeServiceId());
                        if (delRows == 0) {
                            log.warn("删除价外服务失败，serviceId={}", s.getOutOfScopeServiceId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("同步合同价外服务差异失败，准备回滚：contractId={}, error={}", contract.getContractId(), e.getMessage(), e);
            throw new com.erp.common.exception.BusinessException(com.erp.common.enums.ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "同步合同价外服务差异失败：" + e.getMessage());
        }
        
        log.info("更新合同成功：contractId={}, operator={}", contract.getContractId(), currentUserId);
        
        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                ContractDetailResponse newDetail = getContractDetail(contract.getContractId());
                logRecordService.recordDataChangeLog("合同管理", "CONTRACT", 
                        String.valueOf(contract.getContractId()), "更新", 
                        "更新合同：" + contract.getContractNo(), 
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.warn("记录合同更新数据变更日志失败", e);
            }
        }
        
        // 在更新合同后再删除旧文件，避免外键约束冲突
        if (oldFileId != null) {
            try {
                fileService.deleteFile(oldFileId);
            } catch (Exception e) {
                log.warn("删除旧文件失败：fileId={}, error={}", oldFileId, e.getMessage());
                // 文件删除失败不影响合同更新，只记录警告日志
            }
        }

        // 发送合同更新通知
        try {
            Customer customer = customerMapper.selectDetailById(contract.getCustomerId());
            sendContractUpdatedNotification(contract, customer, currentUserId);
        } catch (Exception e) {
            log.error("发送合同更新通知失败：contractId={}, contractNo={}",
                    contract.getContractId(), contract.getContractNo(), e);
            // 消息发送失败不影响主流程，只记录日志
        }
    }

    @Override
    @Cacheable(value = "contractDetail", key = "#contractId", unless = "#result == null")
    public ContractDetailResponse getContractDetail(Integer contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }
        Customer customer = customerMapper.selectDetailById(contract.getCustomerId());
        
        Integer fileId = contract.getContractFileId();
        return buildDetailResponse(contract, customer, fileId);
    }

    @Override
    public IPage<ContractPageResponse> getContractPage(ContractPageRequest request) {
        // 数据范围权限控制
        // 说明：合同列表接口同时服务于"危险废物合同/合同订立/合同变更/合同履行"等页面。
        // 默认按照父页面 `合同管理:危险废物合同:页面` 的配置控制数据范围；
        // 当请求中显式传入 fieldPermissionPageCode 时，优先使用该页面编码控制数据范围，
        // 以便"合同变更/合同履行"等页面可以独立配置 viewScope。
        String pageCodeForViewScope = request.getFieldPermissionPageCode();
        if (StrUtil.isBlank(pageCodeForViewScope)) {
            pageCodeForViewScope = "合同管理:危险废物合同:页面";
        }
        Integer creatorFilter = resolveCreatorFilter(request.getViewScope(), pageCodeForViewScope);

        Page<Contract> page = new Page<>(request.getCurrent(), request.getSize());
        
        // 处理空字符串，转换为null，避免SQL查询问题
        String enterpriseName = request.getEnterpriseName();
        if (enterpriseName != null && enterpriseName.trim().isEmpty()) {
            enterpriseName = null;
        } else if (enterpriseName != null) {
            enterpriseName = enterpriseName.trim();
        }
        
        String contractStatus = request.getContractStatus();
        if (contractStatus != null && contractStatus.trim().isEmpty()) {
            contractStatus = null;
        }
        
        IPage<Contract> entityPage = contractMapper.selectContractPage(
                page,
                enterpriseName,
                contractStatus,
                request.getContractStatuses(),
                request.getSignTimeStart(),
                request.getSignTimeEnd(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getPdfGenerated(),
                creatorFilter,
                request.getSortField(),
                request.getSortOrder()
        );

        List<Contract> records = entityPage.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        }

        // 批量查询客户、审核人和创建人
        Set<Integer> customerIds = records.stream()
                .map(Contract::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Customer> customerMap = CollectionUtils.isEmpty(customerIds)
                ? null
                : customerMapper.selectBatchIds(customerIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Customer::getCustomerId, Function.identity(), (a, b) -> a));

        Set<Integer> auditorIds = records.stream()
                .map(Contract::getAuditorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Employee> auditorMap = CollectionUtils.isEmpty(auditorIds)
                ? null
                : employeeMapper.selectBatchIds(auditorIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Employee::getEmployeeId, Function.identity(), (a, b) -> a));

        Set<Integer> creatorIds = records.stream()
                .map(Contract::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Employee> creatorMap = CollectionUtils.isEmpty(creatorIds)
                ? null
                : employeeMapper.selectBatchIds(creatorIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Employee::getEmployeeId, Function.identity(), (a, b) -> a));

        // 批量查询业务员信息（ownerEmployeeId 存储的是 SALESPERSON.业务员编号）
        Set<Integer> ownerEmployeeIds = records.stream()
                .map(Contract::getOwnerEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Salesperson> ownerEmployeeMap = CollectionUtils.isEmpty(ownerEmployeeIds)
                ? null
                : salespersonMapper.selectBatchIds(ownerEmployeeIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Salesperson::getSalespersonId, Function.identity(), (a, b) -> a));

        // 批量查询PDF文件信息
        Set<Integer> contractIds = records.stream()
                .map(Contract::getContractId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, File> contractFileMap = CollectionUtils.isEmpty(contractIds)
                ? new HashMap<>()
                : fileMapper.selectByBusinessTypeAndIds(CONTRACT_BUSINESS_TYPE, new ArrayList<>(contractIds))
                .stream()
                .filter(Objects::nonNull)
                .filter(file -> "正常".equals(file.getFileStatus())) // 只查询正常状态的文件
                .collect(Collectors.toMap(
                        File::getBusinessId,
                        Function.identity(),
                        (a, b) -> a, // 如果有多个文件，取第一个
                        HashMap::new
                ));

        List<ContractPageResponse> responseList = records.stream()
                .map(contract -> {
                    ContractPageResponse resp = new ContractPageResponse();
                    resp.setContractId(contract.getContractId());
                    resp.setContractNo(contract.getContractNo());
                    resp.setCustomerId(contract.getCustomerId());
                    resp.setContractAmount(contract.getContractAmount());
                    resp.setFeeSettlementEnabled(contract.getFeeSettlementEnabled());
                    resp.setSignTime(contract.getSignTime());
                    resp.setContractStatus(contract.getContractStatus());
                    resp.setValidFrom(contract.getValidFrom());
                    resp.setValidTo(contract.getValidTo());
                    resp.setNumberGenerationMode(contract.getNumberGenerationMode());
                    resp.setAuditTime(contract.getAuditTime());
                    resp.setAuditOpinion(contract.getAuditOpinion());
                    resp.setSendDate(contract.getSendDate());
                    resp.setReceiveDate(contract.getReceiveDate());
                    resp.setAuditorId(contract.getAuditorId());
                    resp.setContractFileId(contract.getContractFileId());
                    resp.setContractPdfFileId(contract.getContractPdfFileId());
                    resp.setScanFilePath(contract.getScanFilePath());
                    resp.setCreatorId(contract.getCreatorId());
                    resp.setRemark(contract.getRemark());
                    resp.setCreateTime(contract.getCreateTime());
                    resp.setUpdateTime(contract.getUpdateTime());
                    resp.setPartyAName(contract.getPartyAName());
                    resp.setPartyAContact(contract.getPartyAContact());
                    resp.setPartyAContactPhone(contract.getPartyAContactPhone());
                    resp.setPartyACreditCode(contract.getPartyACreditCode());
                    resp.setPartyBName(contract.getPartyBName());
                    resp.setPartyBContact(contract.getPartyBContact());
                    resp.setPartyBContactPhone(contract.getPartyBContactPhone());
                    resp.setPartyBCreditCode(contract.getPartyBCreditCode());

                    if (customerMap != null && contract.getCustomerId() != null) {
                        Customer c = customerMap.get(contract.getCustomerId());
                        if (c != null) {
                            resp.setEnterpriseName(c.getEnterpriseName());
                        }
                    }
                    if (auditorMap != null && contract.getAuditorId() != null) {
                        Employee auditor = auditorMap.get(contract.getAuditorId());
                        if (auditor != null) {
                            resp.setAuditorName(auditor.getEmployeeName());
                        }
                    }
                    if (creatorMap != null && contract.getCreatorId() != null) {
                        Employee creator = creatorMap.get(contract.getCreatorId());
                        if (creator != null) {
                            resp.setCreatorName(creator.getEmployeeName());
                        }
                    }

                    // 设置业务员信息（ownerEmployeeId 存储的是 SALESPERSON.业务员编号）
                    resp.setOwnerEmployeeId(contract.getOwnerEmployeeId());
                    if (ownerEmployeeMap != null && contract.getOwnerEmployeeId() != null) {
                        Salesperson owner = ownerEmployeeMap.get(contract.getOwnerEmployeeId());
                        if (owner != null) {
                            resp.setOwnerEmployeeName(owner.getSalespersonName());
                        }
                    }

                    // 设置PDF信息
                    File pdfFile = contractFileMap.get(contract.getContractId());
                    if (pdfFile != null) {
                        resp.setPdfFileId(pdfFile.getFileId());
                        resp.setPdfGenerated(true);
                        resp.setPdfUrl(pdfFile.getFileUrl());
                        resp.setPdfFileName(pdfFile.getFileName());
                    } else {
                        resp.setPdfGenerated(false);
                    }

                    Customer snapshotCustomer = customerMap != null ? customerMap.get(contract.getCustomerId()) : null;
                    ContractCustomerSnapshot pageSnapshot = getSnapshotOrFallback(contract, snapshotCustomer);
                    resp.setCustomerSnapshot(pageSnapshot);
                    if ((resp.getEnterpriseName() == null || resp.getEnterpriseName().trim().isEmpty())
                            && pageSnapshot != null) {
                        resp.setEnterpriseName(pageSnapshot.getCustomerName());
                    }

                    return resp;
                })
                .collect(Collectors.toList());

        Page<ContractPageResponse> responsePage =
                new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        responsePage.setRecords(responseList);
        return responsePage;
    }

    @Override
    public IPage<ContractPageResponse> searchContracts(String keyword, String viewScope, long current, long size) {
        Page<Contract> page = new Page<>(current, size);
        
        // 处理空字符串，转换为null
        String searchKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchKeyword = keyword.trim();
        }
        
        // 解析数据范围
        String resolvedViewScope = ViewScopeHelper.resolveViewScope("合同管理:合同:页面", viewScope);
        Integer creatorId = null;
        if (ViewScopeHelper.isSelfScope(resolvedViewScope)) {
            creatorId = SecurityUtil.getCurrentUserId();
        }
        
        IPage<Contract> entityPage = contractMapper.selectContractSearch(page, searchKeyword, resolvedViewScope, creatorId);

        List<Contract> records = entityPage.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        }

        // 批量查询客户和审核人
        Set<Integer> customerIds = records.stream()
                .map(Contract::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Customer> customerMap = CollectionUtils.isEmpty(customerIds)
                ? null
                : customerMapper.selectBatchIds(customerIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Customer::getCustomerId, Function.identity(), (a, b) -> a));

        Set<Integer> auditorIds = records.stream()
                .map(Contract::getAuditorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Employee> auditorMap = CollectionUtils.isEmpty(auditorIds)
                ? null
                : employeeMapper.selectBatchIds(auditorIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Employee::getEmployeeId, Function.identity(), (a, b) -> a));

        // 批量查询PDF文件信息
        Set<Integer> contractIds = records.stream()
                .map(Contract::getContractId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, File> contractFileMap = CollectionUtils.isEmpty(contractIds)
                ? new HashMap<>()
                : fileMapper.selectByBusinessTypeAndIds(CONTRACT_BUSINESS_TYPE, new ArrayList<>(contractIds))
                .stream()
                .filter(Objects::nonNull)
                .filter(file -> "正常".equals(file.getFileStatus()))
                .collect(Collectors.toMap(
                        File::getBusinessId,
                        Function.identity(),
                        (a, b) -> a,
                        HashMap::new
                ));

        List<ContractPageResponse> responseList = records.stream()
                .map(contract -> {
                    ContractPageResponse resp = new ContractPageResponse();
                    resp.setContractId(contract.getContractId());
                    resp.setContractNo(contract.getContractNo());
                    resp.setCustomerId(contract.getCustomerId());
                    resp.setContractAmount(contract.getContractAmount());
                    resp.setFeeSettlementEnabled(contract.getFeeSettlementEnabled());
                    resp.setSignTime(contract.getSignTime());
                    resp.setContractStatus(contract.getContractStatus());
                    resp.setValidFrom(contract.getValidFrom());
                    resp.setValidTo(contract.getValidTo());
                    resp.setNumberGenerationMode(contract.getNumberGenerationMode());
                    resp.setAuditTime(contract.getAuditTime());
                    resp.setAuditorId(contract.getAuditorId());
                    resp.setContractFileId(contract.getContractFileId());
                    resp.setContractPdfFileId(contract.getContractPdfFileId());
                    resp.setScanFilePath(contract.getScanFilePath());
                    resp.setCreatorId(contract.getCreatorId());
                    resp.setRemark(contract.getRemark());
                    resp.setCreateTime(contract.getCreateTime());
                    resp.setUpdateTime(contract.getUpdateTime());
                    resp.setPartyAName(contract.getPartyAName());
                    resp.setPartyAContact(contract.getPartyAContact());
                    resp.setPartyAContactPhone(contract.getPartyAContactPhone());
                    resp.setPartyACreditCode(contract.getPartyACreditCode());
                    resp.setPartyBName(contract.getPartyBName());
                    resp.setPartyBContact(contract.getPartyBContact());
                    resp.setPartyBContactPhone(contract.getPartyBContactPhone());
                    resp.setPartyBCreditCode(contract.getPartyBCreditCode());

                    if (customerMap != null && contract.getCustomerId() != null) {
                        Customer c = customerMap.get(contract.getCustomerId());
                        if (c != null) {
                            resp.setEnterpriseName(c.getEnterpriseName());
                        }
                    } else if (contract.getCustomerSnapshot() != null) {
                        // 从快照中提取客户名称
                        try {
                            ContractCustomerSnapshot snapshot = objectMapper.readValue(
                                    contract.getCustomerSnapshot(), ContractCustomerSnapshot.class);
                            if (snapshot != null && snapshot.getCustomerName() != null) {
                                resp.setEnterpriseName(snapshot.getCustomerName());
                            }
                        } catch (JsonProcessingException e) {
                            log.warn("解析客户快照失败：contractId={}", contract.getContractId(), e);
                        }
                    }

                    if (auditorMap != null && contract.getAuditorId() != null) {
                        Employee e = auditorMap.get(contract.getAuditorId());
                        if (e != null) {
                            resp.setAuditorName(e.getEmployeeName());
                        }
                    }

                    // 设置PDF文件信息
                    File pdfFile = contractFileMap.get(contract.getContractId());
                    if (pdfFile != null) {
                        resp.setPdfGenerated(true);
                        resp.setPdfFileId(pdfFile.getFileId());
                        resp.setPdfUrl(pdfFile.getFileUrl());
                        resp.setPdfFileName(pdfFile.getFileName());
                    } else {
                        resp.setPdfGenerated(false);
                    }

                    // 设置客户快照
                    Customer snapshotCustomer = customerMap != null ? customerMap.get(contract.getCustomerId()) : null;
                    ContractCustomerSnapshot pageSnapshot = getSnapshotOrFallback(contract, snapshotCustomer);
                    resp.setCustomerSnapshot(pageSnapshot);

                    return resp;
                })
                .collect(Collectors.toList());

        Page<ContractPageResponse> responsePage =
                new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        responsePage.setRecords(responseList);
        return responsePage;
    }

    @Override
    public com.erp.controller.contract.dto.ContractWasteItemsAndServicesResponse getContractWasteItems(Integer contractId) {
        if (contractId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "合同编号不能为空");
        }

        // 验证合同是否存在
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        // 查询合同下的所有合同条目
        List<ContractItem> contractItems = contractItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ContractItem>()
                        .eq("合同编号", contractId)
        );

        List<com.erp.controller.contract.dto.ContractNestedItemDTO> wasteItemGroups = new ArrayList<>();

        // 为每个合同条目构建分组数据
        for (ContractItem contractItem : contractItems) {
            // 查询该合同条目下的所有危废条目
            List<ContractWasteItemDTO> contractWasteItems = contractMapper.selectContractWasteItemsByContractItemId(contractItem.getContractItemId());

            // 转换为详情DTO（即使为空列表也要添加分组，保持与合同详情接口一致）
            List<com.erp.controller.contract.dto.ContractNestedItemDTO.WasteItemDetailDTO> wasteItemDetails =
                    contractWasteItems.stream().map(item -> convertToWasteItemDetailDTO(item, contractItem)).collect(Collectors.toList());

            // 构建分组DTO
            com.erp.controller.contract.dto.ContractNestedItemDTO groupDTO =
                    new com.erp.controller.contract.dto.ContractNestedItemDTO();
            groupDTO.setContractItemId(contractItem.getContractItemId());
            
            groupDTO.setQuotationItemId(contractItem.getQuotationItemId());
            groupDTO.setQuotationMode("总价包干".equals(contractItem.getQuotationMode()) ? "PACKAGE" : "UNIT");
            groupDTO.setPayer(contractItem.getPayer());
            groupDTO.setPricingPlan(contractItem.getPricingPlan());
            groupDTO.setFloorPriceRemark(contractItem.getFloorPriceRemark());
            groupDTO.setSubtotalSummary(contractItem.getSubtotalSummary());
            groupDTO.setWasteItemDetails(wasteItemDetails);

            wasteItemGroups.add(groupDTO);
        }

        // 查询价外服务
        List<com.erp.controller.contract.dto.ContractWasteItemsAndServicesResponse.OutOfScopeServiceResponse> outOfScopeServices =
                contractMapper.selectContractOutOfScopeServicesByContractId(contractId);

        // 构建响应对象
        com.erp.controller.contract.dto.ContractWasteItemsAndServicesResponse response =
                new com.erp.controller.contract.dto.ContractWasteItemsAndServicesResponse();
        response.setWasteItems(wasteItemGroups);
        response.setOutOfScopeServices(outOfScopeServices);

        return response;
    }

    /**
     * 将ContractWasteItemDTO转换为WasteItemDetailDTO
     */
    private com.erp.controller.contract.dto.ContractNestedItemDTO.WasteItemDetailDTO convertToWasteItemDetailDTO(ContractWasteItemDTO source, ContractItem contractItem) {
        com.erp.controller.contract.dto.ContractNestedItemDTO.WasteItemDetailDTO target =
                new com.erp.controller.contract.dto.ContractNestedItemDTO.WasteItemDetailDTO();

        target.setContractWasteItemId(source.getContractWasteItemId());
        target.setQuotationWasteItemId(null); // 暂时设为null，根据实际业务逻辑设置
        target.setHazardousWasteItemId(source.getHazardousWasteItemId());
        target.setWasteCode(source.getWasteCode()); // 添加废物代码字段
        target.setHazardousWaste(source.getHazardousWaste());
        target.setWasteForm(source.getWasteForm());
        target.setWasteCategory(source.getWasteCategory());
        target.setPlannedQuantity(source.getPlannedQuantity());
        target.setUnit(source.getUnit());
        target.setEnableAuxiliaryAccounting(source.getEnableAuxiliaryAccounting());
        target.setAuxUnit(source.getAuxUnit());
        target.setAuxPerBase(source.getAuxPerBase());
        target.setAuxQuantity(source.getAuxQuantity());
        target.setAuxUnitPrice(source.getAuxUnitPrice());
        target.setUnitPrice(source.getUnitPrice());
        target.setAmount(source.getAmount());

        // 根据报价模式设置付款方
        if ("总价包干".equals(contractItem.getQuotationMode())) {
            target.setPayer(contractItem.getPayer());
        } else {
            target.setPayer(source.getPayer());
        }

        target.setPricingPlan(source.getPricingPlan());
        target.setFloorPriceRemark(source.getFloorPriceRemark());

        return target;
    }

    /**
     * 生成合同号
     * 格式：HQ-YYYYMMDD-XXXXX，其中YYYYMMDD为当前日期，XXXXX为序号，序号从00001开始
     */
    private String generateContractNo() {
        // 格式：HQ-YYYYMMDD-XXXXX
        String prefix = "HQ-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        // 查询当天最大的合同号
        Contract maxContract = contractMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .likeRight(Contract::getContractNo, prefix)
                        .orderByDesc(Contract::getContractNo)
                        .last("LIMIT 1")
        );
        int sequence = 1;
        if (maxContract != null && maxContract.getContractNo() != null) {
            String maxNo = maxContract.getContractNo();
            if (maxNo.length() > prefix.length()) {
                String sequenceStr = maxNo.substring(prefix.length());
                try {
                    sequence = Integer.parseInt(sequenceStr) + 1;
                } catch (NumberFormatException e) {
                    sequence = 1;
                }
            }
        }
        return prefix + String.format("%05d", sequence);
    }

    private Contract buildContractEntity(ContractCreateRequest request,
                                         Integer customerId,
                                         Integer quotationId,
                                         Integer contractFileId,
                                         Integer creatorId,
                                         String customerSnapshotJson) {
        LocalDateTime now = LocalDateTime.now();
        Contract contract = new Contract();
        // 自动生成合同号
        contract.setContractNo(generateContractNo());
        contract.setCustomerId(customerId);
        contract.setCustomerSnapshot(customerSnapshotJson);
        // 不再写入报价单编号
        contract.setPartyAName(request.getPartyAName());
        contract.setPartyAContact(request.getPartyAContact());
        contract.setPartyAContactPhone(request.getPartyAContactPhone());
        contract.setPartyACreditCode(request.getPartyACreditCode());
        contract.setPartyBName(request.getPartyBName());
        contract.setPartyBContact(request.getPartyBContact());
        contract.setPartyBContactPhone(request.getPartyBContactPhone());
        contract.setPartyBCreditCode(request.getPartyBCreditCode());
        contract.setContractAmount(request.getContractAmount());
        contract.setFeeSettlementEnabled(Boolean.TRUE.equals(request.getFeeSettlementEnabled()));
        contract.setSignTime(request.getSignTime());
        contract.setContractStatus("待审核");
        contract.setValidFrom(request.getValidFrom());
        contract.setValidTo(request.getValidTo());
        // 编号生成方式，默认为 BEFORE_APPROVAL
        contract.setNumberGenerationMode(request.getNumberGenerationMode() != null 
                ? request.getNumberGenerationMode() 
                : "BEFORE_APPROVAL");
        contract.setAuditorId(null);
        contract.setAuditTime(null);
        contract.setSendDate(request.getSendDate());
        contract.setReceiveDate(request.getReceiveDate());
        contract.setContractFileId(contractFileId);
        contract.setContractPdfFileId(null); // 创建时PDF文件编号为空，审批后生成
        contract.setScanFilePath(null); // 创建时扫描件路径为空
        contract.setCreatorId(creatorId);
        contract.setOwnerEmployeeId(request.getOwnerEmployeeId());
        contract.setRemark(request.getRemark());
        contract.setCreateTime(now);
        contract.setUpdateTime(now);
        return contract;
    }

    /**
     * 构建合同详情响应
     */
    private ContractDetailResponse buildDetailResponse(Contract contract,
                                                       Customer customer,
                                                       Integer fileId) {
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        ContractDetailResponse resp = new ContractDetailResponse();

        // 1. 构建基本合同信息
        buildBasicContractInfo(resp, contract);

        // 2. 构建客户信息
        buildCustomerInfo(resp, contract, customer);

        // 3. 构建员工信息
        buildEmployeeInfo(resp, contract);

        // 4. 构建文件信息
        buildFileInfo(resp, contract, fileId);

        // 5. 构建合同条目
        buildContractItems(resp, contract);

        // 6. 构建价外服务
        buildOutOfScopeServices(resp, contract);

        return resp;
    }

    /**
     * 构建基本合同信息
     */
    private void buildBasicContractInfo(ContractDetailResponse resp, Contract contract) {
        BeanUtils.copyProperties(contract, resp);
        // 特殊字段处理
        resp.setContractId(contract.getContractId());
        resp.setContractNo(contract.getContractNo());
        resp.setCustomerId(contract.getCustomerId());
        resp.setPartyAName(contract.getPartyAName());
        resp.setPartyAContact(contract.getPartyAContact());
        resp.setPartyAContactPhone(contract.getPartyAContactPhone());
        resp.setPartyACreditCode(contract.getPartyACreditCode());
        resp.setPartyBName(contract.getPartyBName());
        resp.setPartyBContact(contract.getPartyBContact());
        resp.setPartyBContactPhone(contract.getPartyBContactPhone());
        resp.setPartyBCreditCode(contract.getPartyBCreditCode());
    }

    /**
     * 构建客户信息
     */
    private void buildCustomerInfo(ContractDetailResponse resp, Contract contract, Customer customer) {
        if (customer != null && StrUtil.isNotBlank(customer.getEnterpriseName())) {
            resp.setEnterpriseName(customer.getEnterpriseName());
        }

        ContractCustomerSnapshot detailSnapshot = getSnapshotOrFallback(contract, customer);
        resp.setCustomerSnapshot(detailSnapshot);

        if ((resp.getEnterpriseName() == null || resp.getEnterpriseName().trim().isEmpty()) && detailSnapshot != null) {
            resp.setEnterpriseName(detailSnapshot.getCustomerName());
        }
    }

    /**
     * 构建员工信息（创建者和审核者）
     */
    private void buildEmployeeInfo(ContractDetailResponse resp, Contract contract) {
        Set<Integer> employeeIds = new HashSet<>();
        if (contract.getCreatorId() != null) {
            employeeIds.add(contract.getCreatorId());
        }
        if (contract.getAuditorId() != null) {
            employeeIds.add(contract.getAuditorId());
        }

        Map<Integer, Employee> employeeMap = batchQueryAndMap(employeeIds,
                employeeMapper::selectBatchIds,
                Employee::getEmployeeId);

        // 设置创建者姓名
        if (contract.getCreatorId() != null) {
            Employee creator = employeeMap.get(contract.getCreatorId());
            if (creator != null) {
                resp.setCreatorName(creator.getEmployeeName());
            }
        }

        // 设置审核者姓名
        if (contract.getAuditorId() != null) {
            Employee auditor = employeeMap.get(contract.getAuditorId());
            if (auditor != null) {
                resp.setAuditorName(auditor.getEmployeeName());
            }
        }

        // 设置业务员信息（ownerEmployeeId 现在存储的是 SALESPERSON.业务员编号）
        resp.setOwnerEmployeeId(contract.getOwnerEmployeeId());
        if (contract.getOwnerEmployeeId() != null) {
            Salesperson salesperson = salespersonMapper.selectById(contract.getOwnerEmployeeId());
            if (salesperson != null) {
                resp.setOwnerEmployeeName(salesperson.getSalespersonName());
                log.info("合同详情 - 从SALESPERSON表获取业务员名称：salespersonId={}, salespersonName={}",
                        salesperson.getSalespersonId(), salesperson.getSalespersonName());
            } else {
                log.warn("合同详情 - 未找到业务员档案：ownerEmployeeId={}", contract.getOwnerEmployeeId());
            }
        }
    }

    /**
     * 构建文件信息
     */
    private void buildFileInfo(ContractDetailResponse resp, Contract contract, Integer fileId) {
        Set<Integer> fileIds = new HashSet<>();
        if (fileId != null) {
            fileIds.add(fileId);
        }
        if (contract.getContractFileId() != null) {
            fileIds.add(contract.getContractFileId());
        }
        if (contract.getContractPdfFileId() != null) {
            fileIds.add(contract.getContractPdfFileId());
        }

        Map<Integer, File> fileMap = batchQueryAndMap(fileIds,
                fileMapper::selectBatchIds,
                File::getFileId);

        // 设置合同扫描件信息
        if (fileId != null) {
            File file = fileMap.get(fileId);
            if (file != null) {
                resp.setContractFileUrl(file.getFileUrl());
                resp.setContractFileName(file.getFileName());
            }
        } else if (contract.getContractFileId() != null) {
            File file = fileMap.get(contract.getContractFileId());
            if (file != null) {
                resp.setContractFileUrl(file.getFileUrl());
                resp.setContractFileName(file.getFileName());
            }
        }

        // 设置合同PDF文件信息
        if (contract.getContractPdfFileId() != null) {
            File pdfFile = fileMap.get(contract.getContractPdfFileId());
            if (pdfFile != null) {
                resp.setContractPdfFileUrl(pdfFile.getFileUrl());
                resp.setContractPdfFileName(pdfFile.getFileName());
            }
        }
    }

    /**
     * 构建合同条目列表
     */
    private void buildContractItems(ContractDetailResponse resp, Contract contract) {
        // 使用联合查询一次性获取合同条目及其危废条目明细，减少数据库查询次数
        List<com.erp.controller.contract.dto.ContractItemWithWasteItems> itemsWithWasteItems =
                contractItemMapper.selectItemsWithWasteItemsByContractId(contract.getContractId());

        if (CollectionUtils.isEmpty(itemsWithWasteItems)) {
            resp.setQuotationItems(Collections.emptyList());
            return;
        }

        // 收集所有危废条目明细，用于批量填充关联字段
        List<ContractWasteItem> allWasteItems = new ArrayList<>();
        for (com.erp.controller.contract.dto.ContractItemWithWasteItems item : itemsWithWasteItems) {
            if (item.getWasteItems() != null) {
                allWasteItems.addAll(item.getWasteItems());
            }
        }

        // 批量填充废物类别等关联字段
        if (!CollectionUtils.isEmpty(allWasteItems)) {
            fillWasteItemAssociationFields(allWasteItems);
        }

        // 将联合查询结果按合同条目分组并构建响应对象
        Map<Integer, com.erp.controller.contract.dto.ContractItemWithWasteItems> itemMap = new HashMap<>();
        Map<Integer, List<ContractWasteItem>> wasteItemMap = new HashMap<>();

        // 一次性遍历结果，构建两个Map
        for (com.erp.controller.contract.dto.ContractItemWithWasteItems item : itemsWithWasteItems) {
            Integer itemId = item.getContractItemId();
            itemMap.putIfAbsent(itemId, item);

            // 为每个合同条目收集危废条目明细
            wasteItemMap.computeIfAbsent(itemId, k -> new ArrayList<>())
                       .addAll(item.getWasteItems() != null ? item.getWasteItems() : Collections.emptyList());
        }

        // 构建响应对象
        List<ContractDetailResponse.ContractItemResponse> contractItemResponses = new ArrayList<>();
        for (com.erp.controller.contract.dto.ContractItemWithWasteItems contractItem : itemMap.values()) {
            List<ContractWasteItem> wasteItems = wasteItemMap.getOrDefault(contractItem.getContractItemId(), Collections.emptyList());
            ContractDetailResponse.ContractItemResponse itemResponse = buildContractItemResponse(contractItem, wasteItems);
            contractItemResponses.add(itemResponse);
        }

        resp.setQuotationItems(contractItemResponses);
    }

    /**
     * 构建单个合同条目响应
     */
    private ContractDetailResponse.ContractItemResponse buildContractItemResponse(
            com.erp.controller.contract.dto.ContractItemWithWasteItems contractItem,
            List<ContractWasteItem> wasteItems) {

        ContractDetailResponse.ContractItemResponse itemResponse = new ContractDetailResponse.ContractItemResponse();

        // 基本字段映射
        BeanUtils.copyProperties(contractItem, itemResponse);
        itemResponse.setContractNumber(contractItem.getContractId());

        // 计价方式转换
        String quotationMode = normalizeQuotationMode(contractItem.getQuotationMode());
        itemResponse.setQuotationMode(quotationMode);

        // 计价方案处理
        if ("总价包干".equals(quotationMode)) {
            itemResponse.setPricingPlan(contractItem.getPricingPlan());
        } else {
            itemResponse.setPricingPlan(null);
        }

        // 备注字段处理
        itemResponse.setRemark(null); // 合同条目表没有备注字段

        // 构建危废条目明细
        List<ContractDetailResponse.ContractWasteItemResponse> wasteItemResponses = new ArrayList<>();
        if (!CollectionUtils.isEmpty(wasteItems)) {
            for (ContractWasteItem wasteItem : wasteItems) {
                ContractDetailResponse.ContractWasteItemResponse wasteItemResponse = buildContractWasteItemResponse(wasteItem);
                wasteItemResponses.add(wasteItemResponse);
            }
        }
        itemResponse.setWasteItems(wasteItemResponses);

        return itemResponse;
    }

    /**
     * 构建单个合同危废条目响应
     */
    private ContractDetailResponse.ContractWasteItemResponse buildContractWasteItemResponse(ContractWasteItem wasteItem) {
        ContractDetailResponse.ContractWasteItemResponse wasteItemResponse = new ContractDetailResponse.ContractWasteItemResponse();
        BeanUtils.copyProperties(wasteItem, wasteItemResponse);
        // 计价方案回退逻辑：优先使用数据库中明细级的计价方案字段；若为空且存在单价，则回退为单价字符串，保证前端能显示数值
        if (StrUtil.isNotBlank(wasteItem.getPricingPlan())) {
            wasteItemResponse.setPricingPlan(wasteItem.getPricingPlan());
        } else if (wasteItem.getUnitPrice() != null) {
            wasteItemResponse.setPricingPlan(wasteItem.getUnitPrice().stripTrailingZeros().toPlainString());
        } else {
            wasteItemResponse.setPricingPlan(null);
        }

        // 底价备注：从危废明细的字段直接返回（数据库字段为 CONTRACT_WASTE_ITEM.低价备注）
        wasteItemResponse.setFloorPriceRemark(wasteItem.getFloorPriceRemark());

        // 备注字段处理（表中无通用备注字段，保持响应字段为 null）
        wasteItemResponse.setRemark(null);
        return wasteItemResponse;
    }

    /**
     * 构建价外服务列表
     */
    private void buildOutOfScopeServices(ContractDetailResponse resp, Contract contract) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.contract.OutOfScopeService> ossWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            ossWrapper.eq("关联业务类型", CONTRACT_BUSINESS_TYPE).eq("关联业务单号", contract.getContractId());
            List<com.erp.entity.contract.OutOfScopeService> ossList = outOfScopeServiceMapper.selectList(ossWrapper);

            if (!CollectionUtils.isEmpty(ossList)) {
                List<com.erp.controller.contract.dto.QuotationDetailResponse.OutOfScopeServiceResponse> ossResponses =
                        ossList.stream().map(this::buildOutOfScopeServiceResponse).collect(Collectors.toList());
                resp.setOutOfScopeServices(ossResponses);
            } else {
                resp.setOutOfScopeServices(new ArrayList<>());
            }
        } catch (Exception e) {
            log.warn("加载合同价外服务失败，不影响详情返回，contractId={}", contract.getContractId(), e);
            resp.setOutOfScopeServices(new ArrayList<>());
        }
    }

    /**
     * 构建单个价外服务响应
     */
    private com.erp.controller.contract.dto.QuotationDetailResponse.OutOfScopeServiceResponse buildOutOfScopeServiceResponse(
            com.erp.entity.contract.OutOfScopeService service) {
        com.erp.controller.contract.dto.QuotationDetailResponse.OutOfScopeServiceResponse response =
                new com.erp.controller.contract.dto.QuotationDetailResponse.OutOfScopeServiceResponse();
        BeanUtils.copyProperties(service, response);
        return response;
    }

    /**
     * 标准化计价方式
     */
    private String normalizeQuotationMode(String quotationMode) {
        if (StrUtil.isBlank(quotationMode)) {
            return "按量结算";
        }
        switch (quotationMode) {
            case "PACKAGE":
                return "总价包干";
            case "UNIT":
                return "按量结算";
            default:
                return quotationMode;
        }
    }

    /**
     * 批量填充危废条目的关联字段（废物类别、行业来源、废物代码、危险特性）
     */
    private void fillWasteItemAssociationFields(List<ContractWasteItem> wasteItems) {
        if (CollectionUtils.isEmpty(wasteItems)) {
            return;
        }

        // 收集所有危废条目编号
        Set<Integer> hazardousWasteItemIds = wasteItems.stream()
                .map(ContractWasteItem::getHazardousWasteItemId)
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

        // 为每个合同危废条目填充关联字段
        for (ContractWasteItem wasteItem : wasteItems) {
            if (wasteItem.getHazardousWasteItemId() != null) {
                com.erp.entity.system.HazardousWasteItem hazardousWasteItem =
                        hazardousWasteItemMap.get(wasteItem.getHazardousWasteItemId());

                if (hazardousWasteItem != null) {
                    wasteItem.setIndustrySource(hazardousWasteItem.getIndustrySource());
                    wasteItem.setWasteCode(hazardousWasteItem.getWasteCode());
                    wasteItem.setHazardFeature(hazardousWasteItem.getHazardCharacteristic());

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

    /**
     * 通用批量查询和Map构建方法
     */
    private <T, K> Map<K, T> batchQueryAndMap(Collection<K> ids,
                                             Function<Collection<K>, List<T>> queryFunction,
                                             Function<T, K> keyExtractor) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }

        List<T> entities = queryFunction.apply(ids);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyMap();
        }

        return entities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(keyExtractor, Function.identity(), (oldVal, newVal) -> newVal));
    }

    @Override
    public ContractStatistics getContractStatistics() {
        ContractStatistics statistics = new ContractStatistics();
        
        // 统计合同总数
        Long totalCount = contractMapper.selectCount(null);
        statistics.setTotal(totalCount != null ? totalCount.intValue() : 0);
        
        // 统计执行中数量
        Long executingCount = contractMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractStatus, "执行中")
        );
        statistics.setExecuting(executingCount != null ? executingCount.intValue() : 0);
        
        // 统计已完结数量
        Long completedCount = contractMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractStatus, "已完结")
        );
        statistics.setCompleted(completedCount != null ? completedCount.intValue() : 0);
        
        // 统计待审核数量
        Long pendingAuditCount = contractMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractStatus, "待审核")
        );
        statistics.setPendingAudit(pendingAuditCount != null ? pendingAuditCount.intValue() : 0);

        // 统计审核中数量
        Long auditingCount = contractMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractStatus, "审核中")
        );
        statistics.setAuditing(auditingCount != null ? auditingCount.intValue() : 0);
        
        // 统计本月新增合同数
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        
        Long monthlyNewCount = contractMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract>()
                        .ge(Contract::getCreateTime, monthStart)
                        .le(Contract::getCreateTime, monthEnd)
        );
        statistics.setMonthlyNew(monthlyNewCount != null ? monthlyNewCount.intValue() : 0);
        
        // 统计本月合同金额（使用自定义查询方法）
        List<Contract> monthlyContracts = contractMapper.selectByTimeRange(monthStart, monthEnd);
        BigDecimal monthlyAmount = monthlyContracts.stream()
                .map(Contract::getContractAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.setMonthlyAmount(monthlyAmount);
        
        return statistics;
    }

    /**
     * 发送合同生成通知
     * 使用基于权限的通知方法，发送给有合同页面权限的人员
     */
    private void sendContractCreatedNotification(Contract contract, Customer customer, Integer senderId) {
        try {
            log.info("开始发送合同生成通知：contractId={}, contractNo={}", 
                    contract.getContractId(), contract.getContractNo());
            
            String businessTitle = String.format("合同【%s】", contract.getContractNo());
            
            // 使用基于权限的通知方法
            messageNotificationService.sendBusinessOperationNotification(
                    "CONTRACT_CREATE",
                    contract.getContractId(),
                    businessTitle,
                    "新增",
                    senderId
            );
            
            log.info("合同生成通知已发送：contractId={}, contractNo={}", 
                    contract.getContractId(), contract.getContractNo());
        } catch (Exception e) {
            log.error("发送合同生成通知异常：contractId={}, contractNo={}", 
                    contract.getContractId(), contract.getContractNo(), e);
        }
    }

    /**
     * 发送合同更新通知
     * 使用基于权限的通知方法，发送给有合同页面权限的人员
     */
    private void sendContractUpdatedNotification(Contract contract, Customer customer, Integer senderId) {
        try {
            log.info("开始发送合同更新通知：contractId={}, contractNo={}", 
                    contract.getContractId(), contract.getContractNo());
            
            String businessTitle = String.format("合同【%s】", contract.getContractNo());
            
            // 使用基于权限的通知方法
            messageNotificationService.sendBusinessOperationNotification(
                    "CONTRACT_UPDATE",
                    contract.getContractId(),
                    businessTitle,
                    "更新",
                    senderId
            );
            
            log.info("合同更新通知已发送：contractId={}, contractNo={}", 
                    contract.getContractId(), contract.getContractNo());
        } catch (Exception e) {
            log.error("发送合同更新通知异常：contractId={}, contractNo={}", 
                    contract.getContractId(), contract.getContractNo(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractStatus(Integer contractId, String contractStatus, String auditOpinion) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 获取当前用户姓名
        String approverName = null;
        Employee currentEmployee = employeeMapper.selectById(currentUserId);
        if (currentEmployee != null) {
            approverName = currentEmployee.getEmployeeName();
        }

        // 验证合同是否存在
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        // 应用操作范围控制（operateScope）：仅允许在“仅操作自己”范围内审核自己创建的合同
        if (!admin) {
            EmployeePermission permission =
                    getEmployeePagePermission(currentUserId, "合同管理:合同变更:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
                if (!Objects.equals(contract.getCreatorId(), currentUserId)) {
                    throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能审核自己创建的合同");
                }
            }
        }
        
        // 验证状态是否有效
        String[] validStatuses = {"待审核", "已通过", "执行中", "已完结", "已归档", "已驳回"};
        boolean isValidStatus = false;
        for (String status : validStatuses) {
            if (status.equals(contractStatus)) {
                isValidStatus = true;
                break;
            }
        }
        if (!isValidStatus) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), 
                    "合同状态无效，有效状态为：待审核、已通过、执行中、已完结、已归档、已驳回");
        }
        
        // 记录原状态
        String oldStatus = contract.getContractStatus();
        
        // 更新合同状态
        contract.setContractStatus(contractStatus);
        contract.setAuditorId(currentUserId);
        contract.setAuditTime(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        
        // 更新审核意见到专门的审核意见字段
        if (auditOpinion != null && !auditOpinion.trim().isEmpty()) {
            contract.setAuditOpinion(auditOpinion);
        }
        
        // 更新数据库
        int rows = contractMapper.updateById(contract);
        if (rows == 0) {
            throw new BusinessException("更新合同状态失败：记录已被其他用户修改");
        }
        
        log.info("合同状态更新成功：contractId={}, oldStatus={}, newStatus={}, auditorId={}",
                contractId, oldStatus, contractStatus, currentUserId);

        // 更新OA审批记录
        OaApprovalRecord pendingRecord = oaApprovalRecordService.findPendingBySource("CONTRACT", contractId);
        if (pendingRecord != null) {
            String oaResult = "已通过".equals(contractStatus) || "执行中".equals(contractStatus) ? "通过" : "驳回";
            oaApprovalRecordService.approve(
                    pendingRecord.getApprovalRecordId(),
                    "CONTRACT",
                    contractId,
                    oaResult,
                    auditOpinion,
                    currentUserId,
                    approverName
            );
            log.info("更新OA审批记录成功：contractId={}, oaRecordId={}, newStatus={}",
                    contractId, pendingRecord.getApprovalRecordId(), contractStatus);
        }

        // 如果状态变为"已通过"或"执行中"（审核通过），更新审批流记录为"合同审核"
        if ("已通过".equals(contractStatus) || "执行中".equals(contractStatus)) {
            contractApprovalFlowService.updateToContractAuditFlow(contractId, currentUserId, auditOpinion);
            log.info("更新合同审批流记录成功：contractId={}, contractNo={}", contractId, contract.getContractNo());

            // 创建关联的业务合同（仅当"业务费用结算"开关开启时）
            if (Boolean.TRUE.equals(contract.getFeeSettlementEnabled())) {
                createBusinessContractFromContract(contract);
            } else {
                log.info("业务费用结算未开启，不创建业务合同：contractId={}", contract.getContractId());
            }
        }
        
        // 发送状态变更消息（失败不影响事务）
        try {
            sendContractStatusChangeNotification(contract, oldStatus, contractStatus, currentUserId);
        } catch (Exception e) {
            log.error("发送合同状态变更通知失败：contractId={}, contractNo={}", contractId, contract.getContractNo(), e);
        }
    }

    /**
     * 发送合同状态变更通知
     * 根据状态变更类型使用不同的通知方法
     */
    private void sendContractStatusChangeNotification(Contract contract, String oldStatus, 
                                                     String newStatus, Integer senderId) {
        try {
            log.info("开始发送合同状态变更通知：contractId={}, contractNo={}, oldStatus={}, newStatus={}, senderId={}", 
                    contract.getContractId(), contract.getContractNo(), oldStatus, newStatus, senderId);
            
            String businessTitle = String.format("合同【%s】", contract.getContractNo());
            
            // 根据新状态选择通知类型
            if ("待审核".equals(newStatus)) {
                // 提交审批 - 发送给OA审批人员
                messageNotificationService.sendApprovalSubmitNotification(
                        "CONTRACT_SUBMIT",
                        contract.getContractId(),
                        businessTitle,
                        senderId
                );
                log.info("合同提交审批通知已发送：contractId={}", contract.getContractId());
            } else if ("审核通过".equals(newStatus) || "已通过".equals(newStatus)) {
                // 审核通过 - 发送给有合同页面权限的人员
                messageNotificationService.sendAuditResultNotification(
                        "CONTRACT_AUDIT_RESULT",
                        contract.getContractId(),
                        businessTitle,
                        "审核通过",
                        senderId
                );
                log.info("合同审核通过通知已发送：contractId={}", contract.getContractId());
            } else if ("已驳回".equals(newStatus)) {
                // 审核驳回 - 发送给有合同页面权限的人员
                messageNotificationService.sendAuditResultNotification(
                        "CONTRACT_AUDIT_RESULT",
                        contract.getContractId(),
                        businessTitle,
                        "审核驳回",
                        senderId
                );
                log.info("合同审核驳回通知已发送：contractId={}", contract.getContractId());
            } else {
                // 其他状态变更 - 发送给有合同页面权限的人员
                messageNotificationService.sendBusinessOperationNotification(
                        "CONTRACT_UPDATE",
                        contract.getContractId(),
                        businessTitle,
                        "状态变更",
                        senderId
                );
                log.info("合同状态变更通知已发送：contractId={}, newStatus={}", contract.getContractId(), newStatus);
            }
        } catch (Exception e) {
            log.error("发送合同状态变更通知异常：contractId={}", contract.getContractId(), e);
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "当前未登录或登录已失效");
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


    /**
     * 获取员工的页面权限配置（包含 viewScope / operateScope / canEdit）
     *
     * @param employeeId 员工ID
     * @param pageCode   页面权限编码
     * @return 员工页面权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        try {
            Permission permission = permissionMapper.selectOne(
                    new LambdaQueryWrapper<Permission>()
                            .eq(Permission::getPermissionCode, pageCode)
                            .eq(Permission::getPermissionTypeId, 2) // 2 = 页面级权限
            );

            if (permission == null) {
                return null;
            }

            return employeePermissionMapper.selectOne(
                    new LambdaQueryWrapper<EmployeePermission>()
                            .eq(EmployeePermission::getEmployeeId, employeeId)
                            .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
            );
        } catch (Exception e) {
            log.error("获取员工页面权限配置失败：employeeId={}, pageCode={}", employeeId, pageCode, e);
            return null;
        }
    }

    /**
     * 创建合同条目和危废条目明细（内部方法）
     */
    private void createContractItemsInternal(Integer contractId,
                                            List<ContractCreateRequest.ContractItemRequest> items,
                                            Integer quotationId,
                                            Integer creatorId) {
        if (items == null || items.isEmpty()) {
            log.info("合同创建 - 没有合同条目需要创建：contractId={}", contractId);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            ContractCreateRequest.ContractItemRequest itemRequest = items.get(i);
            // 前端字段映射：pricingMode -> quotationMode
            String quotationMode = itemRequest.getPricingMode();
            if (quotationMode == null || quotationMode.trim().isEmpty()) {
                // 宽松验证模式下，允许不填写计价方式，使用默认值
                log.info("合同创建 - 条目 #{} 未指定计价方式，使用默认值'按量结算'", i + 1);
                quotationMode = "UNIT"; // 默认按量结算
            }
            // 将前端的 PACKAGE/UNIT 转换为中文
            if ("PACKAGE".equals(quotationMode)) {
                quotationMode = "总价包干";
            } else if ("UNIT".equals(quotationMode)) {
                quotationMode = "按量结算";
            }

            // 验证报价模式
            if (!"总价包干".equals(quotationMode) && !"按量结算".equals(quotationMode)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "报价模式必须是：总价包干或按量结算");
            }

            // 不再关联报价条目
            Integer quotationItemId = null;

            // 创建合同条目
            ContractItem contractItem = new ContractItem();
            contractItem.setContractId(contractId);
            contractItem.setQuotationItemId(quotationItemId);
            contractItem.setQuotationMode(quotationMode);
            contractItem.setPayer(itemRequest.getPayer());
            
            // 总价包干时，计价方案和低价备注在条目表
            if ("总价包干".equals(quotationMode)) {
                // 总价包干模式：保存完整的计价方案字符串（包含单位，如"1500元/每年"）
                String pricingPlan = itemRequest.getPricingPlan();
                if (StrUtil.isNotBlank(pricingPlan)) {
                    contractItem.setPricingPlan(pricingPlan);
                }
                // 从第一个危废条目明细中提取低价备注（总价包干模式下，低价备注在条目表）
                if (!CollectionUtils.isEmpty(itemRequest.getWasteItems())) {
                    ContractCreateRequest.ContractWasteItemRequest firstWaste = itemRequest.getWasteItems().get(0);
                    // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                    String floorPriceRemark = firstWaste.getFloorPriceRemark();
                    if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(firstWaste.getRemark())) {
                        String[] remarkParts = parseRemarkForFloorPrice(firstWaste.getRemark());
                        floorPriceRemark = remarkParts[0];
                    }
                    contractItem.setFloorPriceRemark(floorPriceRemark);
                }
            }
            contractItem.setSubtotalSummary(itemRequest.getSubtotalSummary());
            contractItem.setCreateTime(LocalDateTime.now());
            contractItemMapper.insert(contractItem);

            // 创建合同危废条目明细
            if (!CollectionUtils.isEmpty(itemRequest.getWasteItems())) {
                for (int j = 0; j < itemRequest.getWasteItems().size(); j++) {
                    ContractCreateRequest.ContractWasteItemRequest wasteItemRequest = itemRequest.getWasteItems().get(j);
                    ContractWasteItem contractWasteItem = new ContractWasteItem();
                    contractWasteItem.setContractItemId(contractItem.getContractItemId());
                    
                    // 不再关联报价危废明细
                    contractWasteItem.setQuotationWasteItemId(null);
                    
                    contractWasteItem.setHazardousWasteItemId(wasteItemRequest.getHazardousWasteItemId());
                    contractWasteItem.setHazardousWaste(wasteItemRequest.getHazardousWaste());
                    contractWasteItem.setForm(wasteItemRequest.getForm());
                    contractWasteItem.setUnit(wasteItemRequest.getUnit());
                    contractWasteItem.setPlannedQuantity(wasteItemRequest.getPlannedQuantity());
                    // 是否启用辅助核算：优先使用显式字段，未传时根据是否存在辅助计量单位推断
                    Boolean enableAux = wasteItemRequest.getEnableAuxiliaryAccounting();
                    if (enableAux == null) {
                        enableAux = StrUtil.isNotBlank(wasteItemRequest.getAuxUnit());
                    }
                    contractWasteItem.setEnableAuxiliaryAccounting(enableAux);
                    // 基础/辅助计量单位与换算关系（直接按请求透传，后续如需折算可在此集中处理）
                    contractWasteItem.setBaseUnit(wasteItemRequest.getBaseUnit());
                    contractWasteItem.setAuxUnit(wasteItemRequest.getAuxUnit());
                    contractWasteItem.setAuxPerBase(wasteItemRequest.getAuxPerBase());
                    contractWasteItem.setBaseQuantity(wasteItemRequest.getBaseQuantity());
                    contractWasteItem.setAuxQuantity(wasteItemRequest.getAuxQuantity());
                    contractWasteItem.setUnitPrice(wasteItemRequest.getUnitPrice());
                    // 先设置前端传入的辅助单价（不限量时，auxPerBase为null，需要直接保存auxUnitPrice）
                    contractWasteItem.setAuxUnitPrice(wasteItemRequest.getAuxUnitPrice());
                    // 根据单价与计量单位、换算关系补全基础/辅助单价（如果前端未传auxUnitPrice，则通过计算得到）
                    enrichContractWasteItemUnitPrices(contractWasteItem);
                    contractWasteItem.setAmount(wasteItemRequest.getAmount());
                    
                    // 按量结算时，计价方案、付款方和低价备注在危废条目明细表
                    if ("按量结算".equals(quotationMode)) {
                        // 按量结算模式：计价方案只保存数值，不包含单位（单位在计量单位字段中）
                        String pricingPlan = wasteItemRequest.getPricingPlan();
                        if (StrUtil.isNotBlank(pricingPlan)) {
                            // 提取数值部分
                            String pricingPlanValue = extractNumericValue(pricingPlan);
                            contractWasteItem.setPricingPlan(pricingPlanValue);
                        }
                        contractWasteItem.setPayer(wasteItemRequest.getPayer());
                        // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                        String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                        if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                            String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                            floorPriceRemark = remarkParts[0];
                        }
                        contractWasteItem.setFloorPriceRemark(floorPriceRemark);
                    } else {
                        // 总价包干模式：超量单价保存在计价方案字段（只保存数值，不包含单位）
                        String overLimitPrice = wasteItemRequest.getPricingPlan();
                        if (StrUtil.isNotBlank(overLimitPrice)) {
                            // 提取数值部分
                            String overLimitPriceValue = extractNumericValue(overLimitPrice);
                            contractWasteItem.setPricingPlan(overLimitPriceValue);
                        }
                        // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                        String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                        if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                            String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                            floorPriceRemark = remarkParts[0];
                        }
                        contractWasteItem.setFloorPriceRemark(floorPriceRemark);
                    }
                    
                    contractWasteItem.setCreateTime(LocalDateTime.now());
                    contractWasteItemMapper.insert(contractWasteItem);
                }
            }
        }
    }

    /**
     * 提取字符串中的数值（去掉单位）
     * 例如："1500元/吨" -> "1500", "500元/每吨" -> "500"
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
        return value.trim();
    }

    /**
     * 解析备注，提取底价备注和其他备注
     * 返回数组：[底价备注, 其他备注]
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
     * 根据单价数值、计量单位和换算关系，在合同危废明细上补全基础/辅助单价
     * <p>
     * 约定：
     * - unitPrice 为不带单位的数值（元/计量单位）
     * - unit 为计量单位（"吨" 或 桶/袋/车等），决定该数值是基础单价还是辅助单价
     * - auxPerBase 表示 1 吨 ≈ N 辅助单位
     * </p>
     */
    private void enrichContractWasteItemUnitPrices(ContractWasteItem wasteItem) {
        java.math.BigDecimal unitPrice = wasteItem.getUnitPrice();
        if (unitPrice == null) {
            return;
        }
        String unit = wasteItem.getUnit();
        java.math.BigDecimal auxPerBase = wasteItem.getAuxPerBase();
        // 如果前端已经传了辅助单价，优先使用前端的值（不限量时，auxPerBase为null，前端会直接传auxUnitPrice）
        java.math.BigDecimal existingAuxUnitPrice = wasteItem.getAuxUnitPrice();
        
        if ("吨".equals(unit)) {
            wasteItem.setBaseUnitPrice(unitPrice);
            // 只有当auxPerBase存在且前端未传auxUnitPrice时，才通过计算得到
            if (auxPerBase != null && auxPerBase.compareTo(java.math.BigDecimal.ZERO) > 0 
                    && existingAuxUnitPrice == null) {
                java.math.BigDecimal auxPrice = unitPrice.divide(auxPerBase, 6, java.math.RoundingMode.HALF_UP);
                wasteItem.setAuxUnitPrice(auxPrice);
            }
        } else {
            // 如果前端未传auxUnitPrice，则unitPrice就是auxUnitPrice
            if (existingAuxUnitPrice == null) {
                wasteItem.setAuxUnitPrice(unitPrice);
            }
            // 如果auxPerBase存在，计算baseUnitPrice
            if (auxPerBase != null && auxPerBase.compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal basePrice = unitPrice.multiply(auxPerBase).setScale(6, java.math.RoundingMode.HALF_UP);
                wasteItem.setBaseUnitPrice(basePrice);
            }
        }
    }

    /**
     * 创建合同条目和危废条目明细（用于更新）
     */
    private void createContractItemsForUpdate(Integer contractId,
                                             List<ContractUpdateRequest.ContractItemRequest> items,
                                             Integer quotationId,
                                             Integer creatorId) {
        if (items == null || items.isEmpty()) {
            log.info("合同更新 - 没有合同条目需要处理：contractId={}", contractId);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            ContractUpdateRequest.ContractItemRequest itemRequest = items.get(i);
            String quotationMode = itemRequest.getQuotationMode();
            if (quotationMode == null || quotationMode.trim().isEmpty()) {
                // 宽松验证模式下，允许不填写计价方式，使用默认值
                log.info("合同更新 - 条目 #{} 未指定计价方式，使用默认值'按量结算'", i + 1);
                quotationMode = "按量结算"; // 默认按量结算
            }

            // 不再关联报价条目
            Integer quotationItemId = null;

            // 创建或更新合同条目
            ContractItem contractItem = new ContractItem();
            if (itemRequest.getContractItemId() != null) {
                contractItem.setContractItemId(itemRequest.getContractItemId());
            }
            contractItem.setContractId(contractId);
            contractItem.setQuotationItemId(quotationItemId);
            contractItem.setQuotationMode(quotationMode);
            contractItem.setPayer(itemRequest.getPayer());
            
            // 总价包干时，计价方案和低价备注在条目表
            if ("总价包干".equals(quotationMode)) {
                // 总价包干模式：保存完整的计价方案字符串（包含单位，如"1500元/每年"）
                String pricingPlan = itemRequest.getPricingPlan();
                if (StrUtil.isNotBlank(pricingPlan)) {
                    contractItem.setPricingPlan(pricingPlan);
                }
                // 优先使用floorPriceRemark字段，如果没有则从第一个危废条目明细中提取
                String floorPriceRemark = itemRequest.getFloorPriceRemark();
                if (StrUtil.isBlank(floorPriceRemark) && !CollectionUtils.isEmpty(itemRequest.getWasteItems())) {
                    ContractUpdateRequest.ContractWasteItemRequest firstWaste = itemRequest.getWasteItems().get(0);
                    if (StrUtil.isNotBlank(firstWaste.getRemark())) {
                        String[] remarkParts = parseRemarkForFloorPrice(firstWaste.getRemark());
                        floorPriceRemark = remarkParts[0];
                    }
                }
                contractItem.setFloorPriceRemark(floorPriceRemark);
            }
            contractItem.setSubtotalSummary(itemRequest.getSubtotalSummary());
            contractItem.setCreateTime(LocalDateTime.now());
            contractItem.setUpdateTime(LocalDateTime.now());
            
            if (itemRequest.getContractItemId() != null) {
                int rows = contractItemMapper.updateById(contractItem);
                if (rows == 0) {
                    log.warn("更新合同明细失败（乐观锁冲突），itemId={}", itemRequest.getContractItemId());
                }
            } else {
                contractItemMapper.insert(contractItem);
            }

            // 创建或更新合同危废条目明细
            if (!CollectionUtils.isEmpty(itemRequest.getWasteItems())) {
                for (int j = 0; j < itemRequest.getWasteItems().size(); j++) {
                    ContractUpdateRequest.ContractWasteItemRequest wasteItemRequest = itemRequest.getWasteItems().get(j);
                    ContractWasteItem contractWasteItem = new ContractWasteItem();
                    if (wasteItemRequest.getContractWasteItemId() != null) {
                        contractWasteItem.setContractWasteItemId(wasteItemRequest.getContractWasteItemId());
                    }
                    contractWasteItem.setContractItemId(contractItem.getContractItemId());
                    
                    // 获取对应的报价危废明细编号（用于追溯）
                    contractWasteItem.setQuotationWasteItemId(null);
                    
                    contractWasteItem.setHazardousWasteItemId(wasteItemRequest.getHazardousWasteItemId());
                    contractWasteItem.setHazardousWaste(wasteItemRequest.getHazardousWaste());
                    contractWasteItem.setForm(wasteItemRequest.getForm());
                    contractWasteItem.setUnit(wasteItemRequest.getUnit());
                    contractWasteItem.setPlannedQuantity(wasteItemRequest.getPlannedQuantity());
                    // 是否启用辅助核算：优先使用显式字段，未传时根据是否存在辅助计量单位推断
                    Boolean enableAux = wasteItemRequest.getEnableAuxiliaryAccounting();
                    if (enableAux == null) {
                        enableAux = StrUtil.isNotBlank(wasteItemRequest.getAuxUnit());
                    }
                    contractWasteItem.setEnableAuxiliaryAccounting(enableAux);
                    // 基础/辅助计量单位与换算关系（直接按请求透传，后续如需折算可在此集中处理）
                    contractWasteItem.setBaseUnit(wasteItemRequest.getBaseUnit());
                    contractWasteItem.setAuxUnit(wasteItemRequest.getAuxUnit());
                    contractWasteItem.setAuxPerBase(wasteItemRequest.getAuxPerBase());
                    contractWasteItem.setBaseQuantity(wasteItemRequest.getBaseQuantity());
                    contractWasteItem.setAuxQuantity(wasteItemRequest.getAuxQuantity());
                    contractWasteItem.setUnitPrice(wasteItemRequest.getUnitPrice());
                    // 先设置前端传入的辅助单价（不限量时，auxPerBase为null，需要直接保存auxUnitPrice）
                    contractWasteItem.setAuxUnitPrice(wasteItemRequest.getAuxUnitPrice());
                    // 根据单价与计量单位、换算关系补全基础/辅助单价（如果前端未传auxUnitPrice，则通过计算得到）
                    enrichContractWasteItemUnitPrices(contractWasteItem);
                    contractWasteItem.setAmount(wasteItemRequest.getAmount());
                    
                    // 按量结算时，计价方案、付款方和低价备注在危废条目明细表
                    if ("按量结算".equals(quotationMode)) {
                        // 按量结算模式：计价方案只保存数值，不包含单位（单位在计量单位字段中）
                        String pricingPlan = wasteItemRequest.getPricingPlan();
                        if (StrUtil.isNotBlank(pricingPlan)) {
                            // 提取数值部分
                            String pricingPlanValue = extractNumericValue(pricingPlan);
                            contractWasteItem.setPricingPlan(pricingPlanValue);
                        }
                        contractWasteItem.setPayer(wasteItemRequest.getPayer());
                        // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                        String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                        if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                            String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                            floorPriceRemark = remarkParts[0];
                        }
                        contractWasteItem.setFloorPriceRemark(floorPriceRemark);
                    } else {
                        // 总价包干模式：超量单价保存在计价方案字段（只保存数值，不包含单位）
                        String overLimitPrice = wasteItemRequest.getPricingPlan();
                        if (StrUtil.isNotBlank(overLimitPrice)) {
                            // 提取数值部分
                            String overLimitPriceValue = extractNumericValue(overLimitPrice);
                            contractWasteItem.setPricingPlan(overLimitPriceValue);
                        }
                        // 优先使用floorPriceRemark字段，如果没有则从remark中解析
                        String floorPriceRemark = wasteItemRequest.getFloorPriceRemark();
                        if (StrUtil.isBlank(floorPriceRemark) && StrUtil.isNotBlank(wasteItemRequest.getRemark())) {
                            String[] remarkParts = parseRemarkForFloorPrice(wasteItemRequest.getRemark());
                            floorPriceRemark = remarkParts[0];
                        }
                        contractWasteItem.setFloorPriceRemark(floorPriceRemark);
                    }
                    
                    contractWasteItem.setCreateTime(LocalDateTime.now());
                    contractWasteItem.setUpdateTime(LocalDateTime.now());
                    
                    if (wasteItemRequest.getContractWasteItemId() != null) {
                        int rows = contractWasteItemMapper.updateById(contractWasteItem);
                        if (rows == 0) {
                            log.warn("更新合同危废明细失败（乐观锁冲突），wasteItemId={}", wasteItemRequest.getContractWasteItemId());
                        }
                    } else {
                        contractWasteItemMapper.insert(contractWasteItem);
                    }
                }
            }
        }
    }

    private ContractCustomerSnapshot buildCustomerSnapshotForCreate(Customer customer,
                                                                    ContractCreateRequest request) {
        if (customer != null) {
            return buildSnapshotFromCustomer(customer);
        }
        ContractCustomerSnapshot snapshotFromRequest = sanitizeSnapshot(request.getCustomerSnapshot());
        if (snapshotFromRequest != null) {
            return applySnapshotDefaults(snapshotFromRequest, snapshotFromRequest.getCustomerType(), null);
        }
        return buildSnapshotFromTemporaryRequest(request);
    }

    private ContractCustomerSnapshot resolveSnapshotForUpdate(Contract contract,
                                                              ContractUpdateRequest request,
                                                              Customer newCustomer) {
        ContractCustomerSnapshot snapshotFromRequest = sanitizeSnapshot(request.getCustomerSnapshot());
        if (snapshotFromRequest != null) {
            return applySnapshotDefaults(snapshotFromRequest, snapshotFromRequest.getCustomerType(), newCustomer);
        }
        if (newCustomer != null && !Objects.equals(contract.getCustomerId(), newCustomer.getCustomerId())) {
            return buildSnapshotFromCustomer(newCustomer);
        }
        return null;
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

    private ContractCustomerSnapshot buildSnapshotFromCustomer(Customer customer) {
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
        return applySnapshotDefaults(snapshot, "EXISTING", customer);
    }

    private ContractCustomerSnapshot buildSnapshotFromTemporaryRequest(ContractCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "临时客户信息不能为空");
        }
        if (StrUtil.isBlank(request.getPartyAName())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "临时客户名称不能为空");
        }
        ContractCustomerSnapshot snapshot = new ContractCustomerSnapshot();
        snapshot.setCustomerName(request.getPartyAName().trim());
        snapshot.setCreditCode(StrUtil.trim(request.getPartyACreditCode()));
        snapshot.setContactPerson(StrUtil.trim(request.getPartyAContact()));
        snapshot.setContactPhone(StrUtil.trim(request.getPartyAContactPhone()));
        snapshot.setRemark(StrUtil.trim(request.getRemark()));
        snapshot.setCustomerType("TEMPORARY");
        return applySnapshotDefaults(snapshot, "TEMPORARY", null);
    }

    private ContractCustomerSnapshot sanitizeSnapshot(ContractCustomerSnapshot snapshot) {
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

    private ContractCustomerSnapshot applySnapshotDefaults(ContractCustomerSnapshot snapshot,
                                                           String preferredType,
                                                           Customer customer) {
        if (snapshot == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "客户快照不能为空");
        }
        if (StrUtil.isBlank(snapshot.getCustomerName())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "客户快照缺少客户名称");
        }
        snapshot.setCustomerName(snapshot.getCustomerName().trim());
        String resolvedType = StrUtil.isBlank(preferredType) ? null : preferredType.trim().toUpperCase();
        if (!"EXISTING".equals(resolvedType) && !"TEMPORARY".equals(resolvedType)) {
            resolvedType = customer != null ? "EXISTING" : "TEMPORARY";
        }
        snapshot.setCustomerType(resolvedType);
        snapshot.setSnapshotTime(LocalDateTime.now());
        if (customer != null) {
            snapshot.setCustomerId(customer.getCustomerId());
            snapshot.setOwnerEmployeeId(customer.getOwnerEmployeeId());
        }
        return snapshot;
    }

    private String serializeSnapshot(ContractCustomerSnapshot snapshot) {
        if (snapshot == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("客户快照序列化失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "客户快照序列化失败");
        }
    }

    private ContractCustomerSnapshot deserializeSnapshot(String snapshotJson) {
        if (StrUtil.isBlank(snapshotJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshotJson, ContractCustomerSnapshot.class);
        } catch (Exception e) {
            log.warn("客户快照解析失败，原始值：{}", snapshotJson, e);
            return null;
        }
    }

    private ContractCustomerSnapshot getSnapshotOrFallback(Contract contract, Customer customer) {
        ContractCustomerSnapshot snapshot = deserializeSnapshot(contract.getCustomerSnapshot());
        if (snapshot == null && customer != null) {
            snapshot = buildSnapshotFromCustomer(customer);
        }
        return snapshot;
    }

    private String resolveCustomerName(Contract contract, Customer customer) {
        if (customer != null && StrUtil.isNotBlank(customer.getEnterpriseName())) {
            return customer.getEnterpriseName();
        }
        ContractCustomerSnapshot snapshot = deserializeSnapshot(contract.getCustomerSnapshot());
        if (snapshot != null && StrUtil.isNotBlank(snapshot.getCustomerName())) {
            return snapshot.getCustomerName();
        }
        return "未知客户";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractDetailResponse generatePdf(Integer contractId) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 查询合同
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        // 应用操作范围控制（operateScope）：仅允许在“仅操作自己”范围内为自己创建的合同生成文档
        if (!admin) {
            EmployeePermission permission =
                    getEmployeePagePermission(currentUserId, "合同管理:合同变更:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
                if (!Objects.equals(contract.getCreatorId(), currentUserId)) {
                    throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能为自己创建的合同生成文档");
                }
            }
        }

        // 验证合同号是否存在
        if (contract.getContractNo() == null || contract.getContractNo().trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "合同号不能为空，无法生成PDF");
        }

        // 查询该合同的所有PDF文件记录（包括已删除状态的），统一通过 FileService 删除，支持本地与云端
        List<File> existingFiles = fileMapper.selectByBusinessTypeAndId(CONTRACT_BUSINESS_TYPE, contractId);
        if (!CollectionUtils.isEmpty(existingFiles)) {
            for (File file : existingFiles) {
                try {
                    boolean deleted = fileService.deleteFile(file.getFileId());
                    log.info("删除合同PDF文件与记录：fileId={}, contractId={}, deleted={}",
                            file.getFileId(), contractId, deleted);
                } catch (Exception e) {
                    log.error("删除合同PDF文件或记录异常：fileId={}, contractId={}", file.getFileId(), contractId, e);
                }
            }
        }

        // 获取合同详情（用于渲染 PDF 内容）
        ContractDetailResponse contractDetail = getContractDetail(contractId);

        // 生成PDF文件名：合同_客户名称_合同号.pdf
        String customerName = "";
        if (contractDetail.getCustomerSnapshot() != null
                && contractDetail.getCustomerSnapshot().getCustomerName() != null) {
            customerName = contractDetail.getCustomerSnapshot().getCustomerName();
        }
        String contractNo = contract.getContractNo();
        String pdfFileName = customerName.isEmpty()
                ? contractNo + ".pdf"
                : "合同_" + customerName + "_" + contractNo + ".pdf";
        // 过滤文件名中的非法字符
        pdfFileName = pdfFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 始终先在本地生成一个临时 PDF 文件，后续根据存储类型决定是直接使用还是上传到 OSS
        String localStoragePath = getLocalStoragePath();
        String datePath = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = CONTRACT_BUSINESS_TYPE + "/" + datePath + "/" + pdfFileName;
        String fullPath = localStoragePath + "/" + relativePath;

        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(fullPath);
            java.nio.file.Files.createDirectories(filePath.getParent());

            // 生成本地 PDF 文件
            com.erp.util.ContractPdfGenerator.generatePdf(contractDetail, fullPath);

            java.io.File pdfFileObj = new java.io.File(fullPath);
            long fileSize = pdfFileObj.length();

            // 根据当前全局文件存储类型选择实际存储位置（本地 / OSS 等）
            FileStorageService storageService = fileService.getStorageService();
            File storedFile;

            // 如果是对象存储，实现为：将本地生成的 PDF 封装为 MultipartFile，通过 FileStorageService 上传至 OSS
            if (storageService != null && !(storageService instanceof com.erp.service.common.impl.LocalFileStorageServiceImpl)) {
                MultipartFile multipartFile = new LocalFileMultipartFile(pdfFileObj, "file", pdfFileName, "application/pdf");
                storedFile = storageService.uploadFile(multipartFile, CONTRACT_BUSINESS_TYPE, contractId);

                // 填充通用业务字段
                storedFile.setBusinessModule(CONTRACT_BUSINESS_MODULE);
                storedFile.setBusinessId(contractId);
                storedFile.setBusinessType(CONTRACT_BUSINESS_TYPE);
                storedFile.setFileStatus("正常");
                storedFile.setUploadTime(LocalDateTime.now());
                storedFile.setUploaderId(currentUserId);
                storedFile.setCreateTime(LocalDateTime.now());
                storedFile.setUpdateTime(LocalDateTime.now());

                fileMapper.insert(storedFile);

                // 对象存储上传成功后，本地临时文件可以删除
                boolean deleted = pdfFileObj.delete();
                if (!deleted) {
                    log.warn("删除本地临时合同PDF文件失败：path={}", fullPath);
                }

                log.info("合同PDF已上传到对象存储：contractId={}, fileId={}, objectKey={}, url={}",
                        contractId, storedFile.getFileId(), storedFile.getObjectKey(), storedFile.getFileUrl());
            } else {
                // 默认走本地存储（兼容原有逻辑）
                File pdfFile = new File();
                pdfFile.setFileName(pdfFileName);
                pdfFile.setFileType("PDF");
                pdfFile.setFileSize(fileSize);
                pdfFile.setStorageType("本地");
                pdfFile.setLocalPath(relativePath);
                pdfFile.setFileUrl("/api/file/download?path=" + relativePath);
                pdfFile.setBusinessModule(CONTRACT_BUSINESS_MODULE);
                pdfFile.setBusinessId(contractId);
                pdfFile.setBusinessType(CONTRACT_BUSINESS_TYPE);
                pdfFile.setFileStatus("正常");
                pdfFile.setUploadTime(LocalDateTime.now());
                pdfFile.setUploaderId(currentUserId);
                pdfFile.setCreateTime(LocalDateTime.now());
                pdfFile.setUpdateTime(LocalDateTime.now());
                fileMapper.insert(pdfFile);

                storedFile = pdfFile;

                log.info("合同PDF生成并保存在本地：contractId={}, fileId={}, filePath={}, fileSize={}",
                        contractId, pdfFile.getFileId(), fullPath, fileSize);
            }

        } catch (Exception e) {
            log.error("生成合同PDF失败：contractId={}", contractId, e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "生成PDF失败：" + e.getMessage());
        }

        // 返回合同详情（包含最新PDF信息）
        return getContractDetail(contractId);
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

    /**
     * 获取本地存储路径
     */
    private String getLocalStoragePath() {
        return localStoragePath;
    }

    @Override
    public com.erp.controller.contract.dto.ContractProgressResponse getContractProgress(Integer contractId) {
        // 验证合同是否存在
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        // ==== 操作范围控制（operateScope） ====
        // 说明：
        // 1. 本接口作为“合同履行”页面下的【审批流】详情入口，对应动作级权限：合同管理:合同履行:审批流（在 Controller 上通过 @RequireActionPermission 已强制校验）。
        // 2. 此处仅根据页面级配置的 operateScope 做“仅操作自己 / 操作全部”的行级控制：
        //    - 超级管理员：视为 operateScope = ALL，直接放行；
        //    - operateScope = SELF：仅允许查看“自己创建”的合同审批流程（通过 creatorId 判断）；
        //    - operateScope = ALL 或未配置：不额外限制（已通过动作级权限 + 前端按钮控制入口）。
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        if (!admin && currentUserId != null) {
            EmployeePermission permission =
                    getEmployeePagePermission(currentUserId, "合同管理:合同履行:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
                if (!Objects.equals(contract.getCreatorId(), currentUserId)) {
                    throw new BusinessException(
                            ResultCodeEnum.PERMISSION_DENIED.getCode(),
                            "您只能查看自己创建的合同审批流程"
                    );
                }
            }
        }

        com.erp.controller.contract.dto.ContractProgressResponse response = 
                new com.erp.controller.contract.dto.ContractProgressResponse();
        response.setContractId(contractId);
        response.setContractNo(contract.getContractNo());

        List<com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep> steps = new ArrayList<>();

        // 1. 合同创建（始终显示）
        ContractApprovalFlow creationFlow = contractApprovalFlowMapper.selectByContractIdAndNodeName(
                contractId, "合同创建");
        com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep creationStep = 
                new com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep();
        creationStep.setStepName("合同创建");
        if (creationFlow != null) {
            creationStep.setCompleted(true);
            creationStep.setCompletedTime(creationFlow.getCreateTime());
            creationStep.setStepDescription("合同已创建");
            Employee creator = employeeMapper.selectById(creationFlow.getApproverId());
            creationStep.setOperatorName(creator != null ? creator.getEmployeeName() : null);
        } else {
            // 如果没有审批流数据，标记为未开始（后续会根据后续步骤状态自动更新）
            creationStep.setCompleted(false);
            creationStep.setStepDescription("未开始");
        }
        steps.add(creationStep);

        // 2. 合同审核
        ContractApprovalFlow auditFlow = contractApprovalFlowMapper.selectByContractIdAndNodeName(
                contractId, "合同审核");
        com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep auditStep = 
                new com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep();
        auditStep.setStepName("合同审核");
        if (auditFlow != null && "APPROVED".equals(auditFlow.getApprovalResult())) {
            auditStep.setCompleted(true);
            auditStep.setCompletedTime(auditFlow.getApprovalTime());
            auditStep.setStepDescription("合同审核通过");
            Employee auditor = employeeMapper.selectById(auditFlow.getApproverId());
            auditStep.setOperatorName(auditor != null ? auditor.getEmployeeName() : null);
            auditStep.setRemark(auditFlow.getApprovalOpinion());
        } else {
            auditStep.setCompleted(false);
            auditStep.setStepDescription("等待审核");
        }
        steps.add(auditStep);

        // 3. 收运通知（始终显示，优先根据审批流记录判断，如果没有则根据收运通知单数量判断）
        ContractApprovalFlow pickupFlow = contractApprovalFlowMapper.selectByContractIdAndNodeName(
                contractId, "收运通知");
        com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep pickupStep = 
                new com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep();
        pickupStep.setStepName("收运通知");
        if (pickupFlow != null && "APPROVED".equals(pickupFlow.getApprovalResult())) {
            // 优先使用审批流记录
            pickupStep.setCompleted(true);
            pickupStep.setCompletedTime(pickupFlow.getApprovalTime());
            pickupStep.setStepDescription("收运通知已创建");
            Employee creator = employeeMapper.selectById(pickupFlow.getApproverId());
            pickupStep.setOperatorName(creator != null ? creator.getEmployeeName() : null);
        } else {
            // 如果没有审批流记录，根据收运通知单数量判断（兼容旧数据）
            Integer pickupNoticeCount = contractMapper.countPickupNoticesByContractId(contractId);
            LocalDateTime earliestPickupTime = contractMapper.getEarliestPickupNoticeTime(contractId);
            if (pickupNoticeCount != null && pickupNoticeCount > 0) {
                pickupStep.setCompleted(true);
                pickupStep.setCompletedTime(earliestPickupTime);
                pickupStep.setStepDescription(String.format("已创建 %d 条收运通知", pickupNoticeCount));
            } else {
                pickupStep.setCompleted(false);
                pickupStep.setStepDescription("未开始");
            }
        }
        steps.add(pickupStep);

        // 4. 入库完成（始终显示，优先根据审批流记录判断，如果没有则根据已完成入库单数量判断）
        ContractApprovalFlow warehousingFlow = contractApprovalFlowMapper.selectByContractIdAndNodeName(
                contractId, "入库完成");
        com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep warehousingStep = 
                new com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep();
        warehousingStep.setStepName("入库完成");
        if (warehousingFlow != null && "APPROVED".equals(warehousingFlow.getApprovalResult())) {
            // 优先使用审批流记录
            warehousingStep.setCompleted(true);
            warehousingStep.setCompletedTime(warehousingFlow.getApprovalTime());
            warehousingStep.setStepDescription("入库已完成");
            Employee auditor = employeeMapper.selectById(warehousingFlow.getApproverId());
            warehousingStep.setOperatorName(auditor != null ? auditor.getEmployeeName() : null);
        } else {
            // 如果没有审批流记录，根据已完成入库单数量判断（兼容旧数据）
            Integer warehousingCount = contractMapper.countCompletedWarehousingByContractId(contractId);
            LocalDateTime earliestWarehousingTime = contractMapper.getEarliestWarehousingTime(contractId);
            if (warehousingCount != null && warehousingCount > 0) {
                warehousingStep.setCompleted(true);
                warehousingStep.setCompletedTime(earliestWarehousingTime);
                warehousingStep.setStepDescription(String.format("已完成 %d 条入库", warehousingCount));
            } else {
                warehousingStep.setCompleted(false);
                warehousingStep.setStepDescription("未开始");
            }
        }
        steps.add(warehousingStep);

        // 5. 结算完成（始终显示，根据是否有已完成结算单判断是否完成）
        Integer settlementCount = contractMapper.countCompletedSettlementsByContractId(contractId);
        LocalDateTime earliestSettlementTime = contractMapper.getEarliestSettlementTime(contractId);
        com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep settlementStep = 
                new com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep();
        settlementStep.setStepName("结算完成");
        if (settlementCount != null && settlementCount > 0) {
            settlementStep.setCompleted(true);
            settlementStep.setCompletedTime(earliestSettlementTime);
            settlementStep.setStepDescription(String.format("已完成 %d 条结算", settlementCount));
        } else {
            settlementStep.setCompleted(false);
            settlementStep.setStepDescription("未开始");
        }
        steps.add(settlementStep);

        // 6. 合同完成（始终显示）
        com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep completionStep = 
                new com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep();
        completionStep.setStepName("合同完成");
        if ("已完结".equals(contract.getContractStatus())) {
            completionStep.setCompleted(true);
            completionStep.setCompletedTime(contract.getUpdateTime());
            completionStep.setStepDescription("合同已完结");
        } else {
            completionStep.setCompleted(false);
            completionStep.setStepDescription("未开始");
        }
        steps.add(completionStep);

        // 修复逻辑：如果后续流程已完成，前面的流程也应该标记为已完成
        // 从后往前遍历，如果某个步骤已完成，则将其前面的所有步骤都标记为已完成
        for (int i = steps.size() - 1; i >= 0; i--) {
            com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep currentStep = steps.get(i);
            if (currentStep.getCompleted()) {
                // 如果当前步骤已完成，将前面的所有步骤都标记为已完成
                for (int j = i - 1; j >= 0; j--) {
                    com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep prevStep = steps.get(j);
                    if (!prevStep.getCompleted()) {
                        prevStep.setCompleted(true);
                        // 如果步骤描述是"未开始"或"等待审核"，更新为"已完成"
                        String prevDesc = prevStep.getStepDescription();
                        if ("未开始".equals(prevDesc) || "等待审核".equals(prevDesc)) {
                            prevStep.setStepDescription("已完成");
                        }
                        // 如果步骤没有完成时间，尝试使用后续步骤的时间
                        if (prevStep.getCompletedTime() == null && currentStep.getCompletedTime() != null) {
                            prevStep.setCompletedTime(currentStep.getCompletedTime());
                        }
                    }
                }
                // 找到第一个已完成的步骤后，前面的步骤都已处理，可以跳出循环
                break;
            }
        }

        response.setSteps(steps);

        // 计算整体进度百分比
        long completedCount = steps.stream().filter(com.erp.controller.contract.dto.ContractProgressResponse.ProgressStep::getCompleted).count();
        int progressPercentage = (int) Math.round((completedCount * 100.0) / steps.size());
        response.setProgressPercentage(progressPercentage);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSendMail(ContractBatchMailRequest request) {
        Integer currentUserId = getCurrentUserId();

        if (CollectionUtils.isEmpty(request.getContractIds())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要寄件的合同");
        }

        String sendDateStr = request.getSendDate();
        if (StrUtil.isBlank(sendDateStr)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "寄件时间不能为空");
        }

        List<Contract> contractsToSend = contractMapper.selectBatchIds(request.getContractIds());
        if (contractsToSend.size() != request.getContractIds().size()) {
            Set<Integer> foundIds = contractsToSend.stream().map(Contract::getContractId).collect(Collectors.toSet());
            List<Integer> notFoundIds = request.getContractIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "部分合同不存在: " + notFoundIds);
        }

        // 验证并收集已寄件的合同
        List<Integer> alreadySentIds = new ArrayList<>();
        for (Contract contract : contractsToSend) {
            if (contract.getSendDate() != null) {
                alreadySentIds.add(contract.getContractId());
            }
        }
        if (!alreadySentIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(),
                    "以下合同已确认寄件时间，无法重复操作: " + alreadySentIds);
        }

        // 批量更新寄件时间
        LocalDateTime sendDate = convertToLocalDateTime(sendDateStr);
        if (sendDate == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "寄件时间格式无效");
        }

        for (Contract contract : contractsToSend) {
            contract.setSendDate(sendDate);
            contract.setUpdateTime(LocalDateTime.now());
            int rows = contractMapper.updateById(contract);
            if (rows == 0) {
                log.warn("更新合同寄件信息失败（乐观锁冲突），contractId={}", contract.getContractId());
            }

            // 发送通知
            try {
                String content = String.format("合同【%s】已确认寄件，请及时查收。",
                        contract.getContractNo() != null ? contract.getContractNo() : contract.getContractId());
                messageNotificationService.sendBusinessNotification(
                        "合同",
                        "合同已寄件",
                        content,
                        contract.getCreatorId(),
                        currentUserId,
                        "CONTRACT_MAIL",
                        contract.getContractId()
                );
            } catch (Exception e) {
                log.error("发送合同寄件通知失败：contractId={}", contract.getContractId(), e);
            }
        }

        log.info("批量寄件成功：contractIds={}, sendDate={}, operator={}",
                request.getContractIds(), sendDateStr, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReceiveMail(ContractBatchMailRequest request) {
        Integer currentUserId = getCurrentUserId();

        if (CollectionUtils.isEmpty(request.getContractIds())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要收件的合同");
        }

        String receiveDateStr = request.getReceiveDate();
        if (StrUtil.isBlank(receiveDateStr)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "收件时间不能为空");
        }

        List<Contract> contractsToReceive = contractMapper.selectBatchIds(request.getContractIds());
        if (contractsToReceive.size() != request.getContractIds().size()) {
            Set<Integer> foundIds = contractsToReceive.stream().map(Contract::getContractId).collect(Collectors.toSet());
            List<Integer> notFoundIds = request.getContractIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "部分合同不存在: " + notFoundIds);
        }

        LocalDateTime receiveDate = convertToLocalDateTime(receiveDateStr);
        if (receiveDate == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "收件时间格式无效");
        }

        // 验证每个合同的寄件时间和收件时间
        List<Integer> invalidContractIds = new ArrayList<>();
        for (Contract contract : contractsToReceive) {
            if (contract.getSendDate() == null) {
                invalidContractIds.add(contract.getContractId());
                continue;
            }
            if (contract.getReceiveDate() != null) {
                invalidContractIds.add(contract.getContractId());
                continue;
            }
            if (receiveDate.isBefore(contract.getSendDate())) {
                invalidContractIds.add(contract.getContractId());
            }
        }
        if (!invalidContractIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(),
                    "以下合同不符合收件条件（未寄件、已收件或收件时间早于寄件时间）: " + invalidContractIds);
        }

        // 批量更新收件时间
        for (Contract contract : contractsToReceive) {
            contract.setReceiveDate(receiveDate);
            contract.setUpdateTime(LocalDateTime.now());
            int rows = contractMapper.updateById(contract);
            if (rows == 0) {
                log.warn("更新合同收件信息失败（乐观锁冲突），contractId={}", contract.getContractId());
            }

            // 发送通知
            try {
                String content = String.format("合同【%s】已确认收件。",
                        contract.getContractNo() != null ? contract.getContractNo() : contract.getContractId());
                messageNotificationService.sendBusinessNotification(
                        "合同",
                        "合同已收件",
                        content,
                        contract.getCreatorId(),
                        currentUserId,
                        "CONTRACT_MAIL",
                        contract.getContractId()
                );
            } catch (Exception e) {
                log.error("发送合同收件通知失败：contractId={}", contract.getContractId(), e);
            }
        }

        log.info("批量收件成功：contractIds={}, receiveDate={}, operator={}",
                request.getContractIds(), receiveDateStr, currentUserId);
    }

    /**
     * 批量更新合同寄件/收件时间（通用方法，支持寄件或收件）
     *
     * @param request 批量寄件/收件请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateMailDate(ContractBatchMailRequest request) {
        Integer currentUserId = getCurrentUserId();

        if (CollectionUtils.isEmpty(request.getContractIds())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要操作的合同");
        }

        // 寄件时间和收件时间不能同时为空，也不能同时有值
        if (StrUtil.isBlank(request.getSendDate()) && StrUtil.isBlank(request.getReceiveDate())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "寄件时间或收件时间至少需要填写一项");
        }
        if (StrUtil.isNotBlank(request.getSendDate()) && StrUtil.isNotBlank(request.getReceiveDate())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "寄件时间和收件时间不能同时填写");
        }

        List<Contract> contractsToUpdate = contractMapper.selectBatchIds(request.getContractIds());
        if (contractsToUpdate.size() != request.getContractIds().size()) {
            Set<Integer> foundIds = contractsToUpdate.stream().map(Contract::getContractId).collect(Collectors.toSet());
            List<Integer> notFoundIds = request.getContractIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "部分合同不存在: " + notFoundIds);
        }

        List<Integer> failedContractIds = new ArrayList<>();
        for (Contract contract : contractsToUpdate) {
            if (StrUtil.isNotBlank(request.getSendDate())) {
                // 批量寄件
                if (contract.getSendDate() != null) {
                    failedContractIds.add(contract.getContractId());
                    continue; // 已有寄件时间，跳过
                }
                contract.setSendDate(convertToLocalDateTime(request.getSendDate()));
            } else {
                // 批量收件
                if (contract.getReceiveDate() != null) {
                    failedContractIds.add(contract.getContractId());
                    continue; // 已有收件时间，跳过
                }
                if (contract.getSendDate() == null) {
                    failedContractIds.add(contract.getContractId());
                    continue; // 未寄件，无法收件
                }
                LocalDateTime sendTime = contract.getSendDate();
                LocalDateTime receiveTime = convertToLocalDateTime(request.getReceiveDate());
                if (receiveTime == null || receiveTime.isBefore(sendTime)) {
                    failedContractIds.add(contract.getContractId());
                    continue; // 收件时间无效或早于寄件时间
                }
                contract.setReceiveDate(receiveTime);
            }
            contract.setUpdateTime(LocalDateTime.now());
            contractMapper.updateById(contract);

            // 发送通知
            try {
                String action = StrUtil.isNotBlank(request.getSendDate()) ? "寄件" : "收件";
                String content = String.format("合同【%s】已批量%s，请查看详情。",
                        contract.getContractNo() != null ? contract.getContractNo() : contract.getContractId(), action);
                messageNotificationService.sendBusinessNotification(
                        "合同",
                        "合同" + action + "更新",
                        content,
                        contract.getCreatorId(),
                        currentUserId,
                        "CONTRACT_MAIL",
                        contract.getContractId()
                );
            } catch (Exception e) {
                log.error("发送合同寄件/收件通知失败：contractId={}", contract.getContractId(), e);
            }
        }

        if (CollUtil.isNotEmpty(failedContractIds)) {
            throw new BusinessException(ResultCodeEnum.BUSINESS_ERROR.getCode(),
                    "以下合同因状态原因无法批量操作（已操作或不符合条件）：" + failedContractIds);
        }

        log.info("批量更新合同寄件/收件时间成功：contractIds={}, sendDate={}, receiveDate={}, operator={}",
                request.getContractIds(), request.getSendDate(), request.getReceiveDate(), currentUserId);
    }

    /**
     * 将字符串转换为LocalDateTime
     *
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime对象，解析失败返回null
     */
    private LocalDateTime convertToLocalDateTime(String dateTimeStr) {
        if (StrUtil.isBlank(dateTimeStr)) {
            return null;
        }
        try {
            // 尝试解析 ISO 格式
            return LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e1) {
            try {
                // 尝试解析 yyyy-MM-dd HH:mm:ss 格式
                return LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                try {
                    // 尝试解析 yyyy-MM-dd HH:mm 格式
                    return LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                } catch (Exception e3) {
                    log.warn("日期时间解析失败：{}", dateTimeStr);
                    return null;
                }
            }
        }
    }

    /**
     * 业务费结算专用合同查询
     * 
     * 查询用于业务费结算的合同列表，仅返回执行中和已完结状态的合同
     * 同时返回游离数据统计：
     *   - 未关联危废合同的危废入库单数量
     *   - 未关联危废合同的危废结算单数量
     *
     * @return 合同列表响应
     */
    @Override
    public com.erp.controller.contract.dto.ContractSettlementListResponse getContractForSettlement() {
        try {
            com.erp.controller.contract.dto.ContractSettlementListResponse response =
                    new com.erp.controller.contract.dto.ContractSettlementListResponse();

            // 查询执行中和已完结的合同（带未关联入库单数量统计）
            List<com.erp.controller.contract.dto.ContractSettlementListResponse.ContractSettlementRecord> records =
                    contractMapper.selectSettlementListWithUnlinkedInboundCount();
            response.setRecords(records);

            // 统计未关联危废合同的游离数据数量
            Integer unlinkedWarehousingCount = contractMapper.countUnlinkedWarehousing();
            Integer unlinkedSettlementCount = contractMapper.countUnlinkedSettlement();
            response.setUnlinkedWarehousingCount(unlinkedWarehousingCount != null ? unlinkedWarehousingCount : 0);
            response.setUnlinkedSettlementCount(unlinkedSettlementCount != null ? unlinkedSettlementCount : 0);

            return response;
        } catch (Exception e) {
            log.error("查询业务费结算合同列表失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                "查询合同列表失败：" + e.getMessage());
        }
    }

    /**
     * 批量提交审核
     * 
     * 批量将合同提交OA审核，创建审核记录，将合同状态改为待审核
     *
     * @param request 批量提交审核请求
     * @return 批量提交结果
     */
    @Override
    public ContractBatchSubmitAuditResponse batchSubmitAudit(ContractBatchSubmitAuditRequest request) {
        Integer currentUserId = getCurrentUserId();
        List<Integer> contractIds = request.getContractIds();
        
        List<Integer> successIds = new ArrayList<>();
        List<Integer> failedIds = new ArrayList<>();
        List<ContractBatchSubmitAuditResponse.FailedReason> failedReasons = new ArrayList<>();
        
        // 查询所有合同
        List<Contract> contracts = contractMapper.selectBatchIds(contractIds);
        Map<Integer, Contract> contractMap = contracts.stream()
                .collect(Collectors.toMap(Contract::getContractId, Function.identity()));
        
        // 获取当前用户信息
        Employee currentEmployee = employeeMapper.selectById(currentUserId);
        String currentUserName = currentEmployee != null ? currentEmployee.getEmployeeName() : "未知";
        
        for (Integer contractId : contractIds) {
            Contract contract = contractMap.get(contractId);
            
            // 验证合同是否存在
            if (contract == null) {
                failedIds.add(contractId);
                ContractBatchSubmitAuditResponse.FailedReason reason = 
                    new ContractBatchSubmitAuditResponse.FailedReason();
                reason.setContractId(contractId);
                reason.setReason("合同不存在");
                failedReasons.add(reason);
                continue;
            }
            
            // 验证合同状态：仅待审核、已驳回状态可以提交审核（撤回后业务表状态为待审核）
            String currentStatus = contract.getContractStatus();
            if (!"待审核".equals(currentStatus) && !"已驳回".equals(currentStatus)) {
                failedIds.add(contractId);
                ContractBatchSubmitAuditResponse.FailedReason reason =
                    new ContractBatchSubmitAuditResponse.FailedReason();
                reason.setContractId(contractId);
                reason.setReason("当前状态为【" + currentStatus + "】，仅待审核、已驳回状态可以提交审核");
                failedReasons.add(reason);
                continue;
            }

            try {
                // 提交到OA审批
                OaApprovalRecord approvalRecord;
                String actionType;

                // 查询OA表中是否有已撤回的记录
                OaApprovalRecord withdrawnRecord = oaApprovalRecordService.findWithdrawnBySource("CONTRACT", contractId);
                if (withdrawnRecord != null) {
                    // 有已撤回记录：重新激活该记录，状态改为待审核，审核次数+1
                    approvalRecord = oaApprovalRecordService.reactivateWithdrawnRecord(
                            "CONTRACT",
                            contractId,
                            currentUserId,
                            currentUserName
                    );
                    actionType = "重新激活";
                } else {
                    // 无已撤回记录：检查是否有待审核记录
                    OaApprovalRecord existingRecord = oaApprovalRecordService.findPendingBySource("CONTRACT", contractId);
                    if (existingRecord != null) {
                        failedIds.add(contractId);
                        ContractBatchSubmitAuditResponse.FailedReason reason =
                            new ContractBatchSubmitAuditResponse.FailedReason();
                        reason.setContractId(contractId);
                        reason.setReason("该合同已存在待审核的OA审批记录");
                        failedReasons.add(reason);
                        continue;
                    }

                    // 新建OA审批记录
                    String contractTitle = String.format("危险废物合同：%s",
                            contract.getContractNo() != null ? contract.getContractNo() : "HT" + contractId);
                    approvalRecord = oaApprovalRecordService.submit(
                            "CONTRACT",
                            contractId,
                            "危险废物合同",
                            contract.getContractNo(),
                            contractTitle,
                            currentUserId,
                            currentUserName
                    );
                    actionType = "新建";
                }
                
                // 更新合同状态为审核中
                contract.setContractStatus("审核中");
                contract.setUpdateTime(LocalDateTime.now());
                contractMapper.updateById(contract);
                
                // 记录审批流（合同创建节点）
                try {
                    contractApprovalFlowService.createContractCreationFlow(contractId, currentUserId);
                } catch (Exception e) {
                    log.warn("记录合同审批流失败（不影响主流程）：contractId={}", contractId, e);
                }
                
                // 发送通知给相关人员（发送给有OA审批权限的人员）
                try {
                    String businessTitle = String.format("合同【%s】",
                            contract.getContractNo() != null ? contract.getContractNo() : "HT" + contractId);
                    messageNotificationService.sendApprovalSubmitNotification(
                            "CONTRACT_SUBMIT",
                            contractId,
                            businessTitle,
                            currentUserId
                    );
                } catch (Exception e) {
                    log.warn("发送合同提交审核通知失败（不影响主流程）：contractId={}", contractId, e);
                }
                
                successIds.add(contractId);
                log.info("合同提交OA审核{}成功：contractId={}, approvalRecordId={}, approvalCount={}, operator={}",
                        actionType, contractId, approvalRecord.getApprovalRecordId(), approvalRecord.getApprovalCount(), currentUserId);
                
            } catch (Exception e) {
                failedIds.add(contractId);
                ContractBatchSubmitAuditResponse.FailedReason reason = 
                    new ContractBatchSubmitAuditResponse.FailedReason();
                reason.setContractId(contractId);
                reason.setReason("提交审核失败：" + e.getMessage());
                failedReasons.add(reason);
                log.error("合同提交OA审核失败：contractId={}", contractId, e);
            }
        }
        
        // 构建响应
        ContractBatchSubmitAuditResponse response = new ContractBatchSubmitAuditResponse();
        response.setSuccessIds(successIds);
        response.setFailedIds(failedIds);
        response.setFailedReasons(failedReasons);
        response.setAllSuccess(failedIds.isEmpty());
        
        log.info("批量提交审核完成：successCount={}, failedCount={}, operator={}",
                successIds.size(), failedIds.size(), currentUserId);
        
        return response;
    }

    /**
     * 批量撤回审核
     *
     * 批量将审核中的合同撤回到待审核状态，并同步撤回对应的OA审批记录。
     * 规则：
     * 1. 只有合同状态为“审核中”的合同才允许撤回；
     * 2. 对应OA记录必须存在“待审核”的记录；
     * 3. OA撤回后状态改为“已撤回”，审核次数减1，最小为0；
     * 4. 合同状态由“审核中”改回“待审核”。
     *
     * @param request 批量撤回审核请求
     * @return 批量撤回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractBatchSubmitAuditResponse batchWithdrawAudit(ContractBatchSubmitAuditRequest request) {
        Integer currentUserId = getCurrentUserId();
        List<Integer> contractIds = request.getContractIds();

        List<Integer> successIds = new ArrayList<>();
        List<Integer> failedIds = new ArrayList<>();
        List<ContractBatchSubmitAuditResponse.FailedReason> failedReasons = new ArrayList<>();

        List<Contract> contracts = contractMapper.selectBatchIds(contractIds);
        Map<Integer, Contract> contractMap = contracts.stream()
                .collect(Collectors.toMap(Contract::getContractId, Function.identity()));

        for (Integer contractId : contractIds) {
            Contract contract = contractMap.get(contractId);

            if (contract == null) {
                failedIds.add(contractId);
                ContractBatchSubmitAuditResponse.FailedReason reason =
                        new ContractBatchSubmitAuditResponse.FailedReason();
                reason.setContractId(contractId);
                reason.setReason("合同不存在");
                failedReasons.add(reason);
                continue;
            }

            String currentStatus = contract.getContractStatus();
            if (!"审核中".equals(currentStatus)) {
                failedIds.add(contractId);
                ContractBatchSubmitAuditResponse.FailedReason reason =
                        new ContractBatchSubmitAuditResponse.FailedReason();
                reason.setContractId(contractId);
                reason.setReason("当前状态为【" + currentStatus + "】，仅审核中的合同才可以撤回");
                failedReasons.add(reason);
                continue;
            }

            try {
                OaApprovalRecord pendingRecord = oaApprovalRecordService.findPendingBySource("CONTRACT", contractId);
                if (pendingRecord == null) {
                    failedIds.add(contractId);
                    ContractBatchSubmitAuditResponse.FailedReason reason =
                            new ContractBatchSubmitAuditResponse.FailedReason();
                    reason.setContractId(contractId);
                    reason.setReason("未找到待审核的OA审批记录，无法撤回");
                    failedReasons.add(reason);
                    continue;
                }

                oaApprovalRecordService.cancel(
                        pendingRecord.getApprovalRecordId(),
                        "CONTRACT",
                        contractId,
                        currentUserId,
                        "批量撤回合同审核"
                );

                contract.setContractStatus("待审核");
                contract.setUpdateTime(LocalDateTime.now());
                int rows = contractMapper.updateById(contract);
                if (rows == 0) {
                    throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(), "合同记录已被修改，请刷新后重试");
                }

                // 发送通知给相关人员（发送给有OA审批权限的人员）
                try {
                    String businessTitle = String.format("合同【%s】",
                            contract.getContractNo() != null ? contract.getContractNo() : "HT" + contractId);
                    messageNotificationService.sendApprovalRevokeNotification(
                            "CONTRACT_REVOKE",
                            contractId,
                            businessTitle,
                            currentUserId
                    );
                } catch (Exception e) {
                    log.warn("发送合同撤回审核通知失败（不影响主流程）：contractId={}", contractId, e);
                }

                successIds.add(contractId);
                log.info("合同撤回审核成功：contractId={}, approvalRecordId={}, operator={}",
                        contractId, pendingRecord.getApprovalRecordId(), currentUserId);
            } catch (Exception e) {
                failedIds.add(contractId);
                ContractBatchSubmitAuditResponse.FailedReason reason =
                        new ContractBatchSubmitAuditResponse.FailedReason();
                reason.setContractId(contractId);
                reason.setReason("撤回审核失败：" + e.getMessage());
                failedReasons.add(reason);
                log.error("合同撤回审核失败：contractId={}", contractId, e);
            }
        }

        ContractBatchSubmitAuditResponse response = new ContractBatchSubmitAuditResponse();
        response.setSuccessIds(successIds);
        response.setFailedIds(failedIds);
        response.setFailedReasons(failedReasons);
        response.setAllSuccess(failedIds.isEmpty());

        log.info("批量撤回审核完成：successCount={}, failedCount={}, operator={}",
                successIds.size(), failedIds.size(), currentUserId);

        return response;
    }

    /**
     * 根据危废合同创建业务合同
     * 当危废合同审核通过时，自动创建关联的业务合同
     * 
     * @param contract 危废合同
     */
    private void createBusinessContractFromContract(Contract contract) {
        try {
            log.info("开始创建关联业务合同：contractId={}, contractNo={}",
                    contract.getContractId(), contract.getContractNo());

            // 防重复检查：如果已存在关联的业务合同，跳过创建
            BusinessContract existingBc = businessContractMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BusinessContract>()
                            .eq(BusinessContract::getHazardousContractId, contract.getContractId())
                            .eq(BusinessContract::getDeleted, 0)
                            .last("LIMIT 1")
            );
            if (existingBc != null) {
                log.info("该危废合同已存在关联的业务合同，跳过创建：hazardousContractId={}, businessContractId={}",
                        contract.getContractId(), existingBc.getContractId());
                return;
            }

            // 构建业务合同创建请求
            BusinessContractCreateRequest request = new BusinessContractCreateRequest();
            
            // 关联危废合同
            request.setHazardousContractId(contract.getContractId());
            
            // 从 customer_snapshot JSON 解析业务员信息
            ContractCustomerSnapshot customerSnapshot = parseCustomerSnapshot(contract.getCustomerSnapshot());
            
            // 业务员信息（优先从 customer_snapshot 获取，其次从 contract.ownerEmployeeId 获取）
            Integer ownerEmployeeId = null;
            String ownerEmployeeName = null;
            
            if (customerSnapshot != null) {
                ownerEmployeeId = customerSnapshot.getOwnerEmployeeId();
                ownerEmployeeName = customerSnapshot.getOwnerEmployeeName();
                log.info("从customer_snapshot获取业务员信息：ownerEmployeeId={}, ownerEmployeeName={}",
                        ownerEmployeeId, ownerEmployeeName);
            }
            
            // 如果customer_snapshot中没有，则使用contract的ownerEmployeeId（现在存储的是 SALESPERSON.业务员编号）
            if (ownerEmployeeId == null && contract.getOwnerEmployeeId() != null) {
                ownerEmployeeId = contract.getOwnerEmployeeId();
                // 从SALESPERSON表获取业务员姓名
                Salesperson salesperson = salespersonMapper.selectById(ownerEmployeeId);
                if (salesperson != null) {
                    ownerEmployeeName = salesperson.getSalespersonName();
                }
                log.info("从contract.ownerEmployeeId获取业务员信息：ownerEmployeeId={}, ownerEmployeeName={}",
                        ownerEmployeeId, ownerEmployeeName);
            }
            
            // 尝试从SALESPERSON表查询完整业务员信息
            // 注意：ownerEmployeeId 已经是 SALESPERSON.业务员编号，直接用 ID 查询即可
            Salesperson salesperson = null;
            if (ownerEmployeeId != null) {
                salesperson = salespersonMapper.selectById(ownerEmployeeId);
                if (salesperson != null) {
                    log.info("从SALESPERSON表查询到业务员档案：salespersonId={}, salespersonName={}, employeeId={}",
                            salesperson.getSalespersonId(), salesperson.getSalespersonName(), salesperson.getEmployeeId());
                }
            }
            
            // 设置业务员信息（优先使用SALESPERSON表的完整信息）
            if (salesperson != null) {
                request.setSalespersonId(salesperson.getSalespersonId());
                request.setSalespersonName(salesperson.getSalespersonName());
                request.setSalespersonPhone(salesperson.getSalespersonPhone());
                request.setSalespersonIdCard(salesperson.getSalespersonIdCard());
                
                // 优先使用SALESPERSON表的甲方信息
                request.setPartyAName(StrUtil.isNotBlank(salesperson.getPartyAName()) 
                        ? salesperson.getPartyAName() : contract.getPartyAName());
                request.setPartyACreditCode(StrUtil.isNotBlank(salesperson.getPartyACreditCode()) 
                        ? salesperson.getPartyACreditCode() : contract.getPartyACreditCode());
                
                // 优先使用SALESPERSON表的乙方信息
                request.setPartyBName(StrUtil.isNotBlank(salesperson.getPartyBName()) 
                        ? salesperson.getPartyBName() : contract.getPartyBName());
                request.setPartyBCreditCode(StrUtil.isNotBlank(salesperson.getPartyBCreditCode()) 
                        ? salesperson.getPartyBCreditCode() : contract.getPartyBCreditCode());
                request.setPartyBContactPerson(StrUtil.isNotBlank(salesperson.getPartyBContactPerson()) 
                        ? salesperson.getPartyBContactPerson() : contract.getPartyBContact());
                request.setPartyBContactPhone(StrUtil.isNotBlank(salesperson.getPartyBContactPhone()) 
                        ? salesperson.getPartyBContactPhone() : contract.getPartyBContactPhone());
                
                // 优先使用SALESPERSON表的收款卡信息
                request.setBankName(salesperson.getBankName());
                request.setCardNumber(salesperson.getCardNumber());
                request.setAccountName(salesperson.getAccountName());
            } else {
                // 没有查到SALESPERSON，使用合同的基本信息
                request.setSalespersonName(ownerEmployeeName);
                
                // 甲方信息（从合同获取）
                request.setPartyAName(contract.getPartyAName());
                request.setPartyACreditCode(contract.getPartyACreditCode());
                
                // 乙方信息（从合同获取）
                request.setPartyBName(contract.getPartyBName());
                request.setPartyBCreditCode(contract.getPartyBCreditCode());
                request.setPartyBContactPerson(contract.getPartyBContact());
                request.setPartyBContactPhone(contract.getPartyBContactPhone());
            }
            
            // 合同期限
            if (contract.getSignTime() != null) {
                request.setSignTime(contract.getSignTime().toLocalDate().toString());
            }
            if (contract.getValidFrom() != null) {
                request.setValidFrom(contract.getValidFrom().toLocalDate().toString());
            }
            if (contract.getValidTo() != null) {
                request.setValidTo(contract.getValidTo().toLocalDate().toString());
            }
            
            // 备注
            request.setRemark(contract.getRemark());
            
            // 调用业务合同服务创建
            Integer businessContractId = businessContractService.create(request, null);
            
            log.info("创建关联业务合同成功：hazardousContractId={}, businessContractId={}",
                    contract.getContractId(), businessContractId);
            
        } catch (Exception e) {
            log.error("创建关联业务合同失败：contractId={}, contractNo={}, error={}",
                    contract.getContractId(), contract.getContractNo(), e.getMessage(), e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "创建关联业务合同失败：" + e.getMessage());
        }
    }

    /**
     * 解析客户信息快照JSON
     * 
     * @param customerSnapshot JSON字符串
     * @return 解析后的客户快照对象，如果解析失败返回null
     */
    private ContractCustomerSnapshot parseCustomerSnapshot(String customerSnapshot) {
        if (StrUtil.isBlank(customerSnapshot)) {
            return null;
        }
        try {
            return objectMapper.readValue(customerSnapshot, ContractCustomerSnapshot.class);
        } catch (Exception e) {
            log.warn("解析customer_snapshot失败：{}", customerSnapshot, e);
            return null;
        }
    }

    /**
     * 危废合同下拉列表
     * 专门为下拉选择场景设计的轻量接口，只返回合同ID、合同号、企业名称三个字段
     *
     * @param keyword 搜索关键字（合同号或企业名称模糊搜索）
     * @param viewScope 数据范围（SELF/ALL），下拉选择场景应传ALL
     * @return 合同下拉列表
     */
    @Override
    public List<ContractSelectResponse> getContractSelectList(String keyword, String viewScope) {
        try {
            log.info("查询危废合同下拉列表，keyword={}, viewScope={}", keyword, viewScope);

            // 解析数据范围
            String resolvedViewScope = ViewScopeHelper.resolveViewScope("合同管理:危险废物合同:页面", viewScope);
            Integer creatorId = null;
            if (ViewScopeHelper.isSelfScope(resolvedViewScope)) {
                creatorId = SecurityUtil.getCurrentUserId();
            }

            List<ContractSelectResponse> list = contractMapper.selectContractSelectList(keyword, creatorId);
            log.info("查询到 {} 条合同记录", list.size());
            return list;
        } catch (Exception e) {
            log.error("查询危废合同下拉列表失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "查询合同下拉列表失败：" + e.getMessage());
        }
    }
}



