package com.erp.service.contract.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.contract.dto.*;
import com.erp.entity.contract.OutsourceProcessingContract;
import com.erp.entity.contract.OutsourceProcessingContractItem;
import com.erp.entity.contract.OutsourceProcessingContractWasteItem;
import com.erp.mapper.contract.OutsourceProcessingContractItemMapper;
import com.erp.mapper.contract.OutsourceProcessingContractMapper;
import com.erp.mapper.contract.OutsourceProcessingContractWasteItemMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.common.FileService;
import com.erp.service.contract.OutsourceProcessingContractBatchAuditResponse;
import com.erp.service.contract.OutsourceProcessingContractService;
import com.erp.service.system.ILogRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 委外处理合同服务实现
 *
 * @author ERP System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutsourceProcessingContractServiceImpl implements OutsourceProcessingContractService {

    private final OutsourceProcessingContractMapper contractMapper;
    private final OutsourceProcessingContractItemMapper itemMapper;
    private final OutsourceProcessingContractWasteItemMapper wasteItemMapper;
    private final ILogRecordService logRecordService;
    private final FileService fileService;
    private final AuthService authService;

    private static final String STATUS_PENDING_AUDIT = "待审核";
    private static final String STATUS_IN_AUDIT = "审核中";
    private static final String STATUS_APPROVED = "执行中";
    private static final String STATUS_REJECTED = "已驳回";
    private static final String STATUS_COMPLETED = "已完结";
    private static final String STATUS_ARCHIVED = "已归档";

    private static final String MOBILE_PHONE_REGEX = "^1[3-9]\\d{9}$";

    private void validateMobilePhone(String phone, String fieldName) {
        if (phone != null && !phone.isEmpty() && !phone.matches(MOBILE_PHONE_REGEX)) {
            throw new BusinessException(ResultCodeEnum.PARAM_INVALID.getCode(), fieldName + "格式不正确，应为11位手机号（如13800138000）或留空");
        }
    }

    @Override
    public IPage<OutsourceProcessingContractPageResponse> getContractPage(OutsourceProcessingContractPageRequest request) {
        Integer creatorFilter = resolveCreatorFilter(request.getViewScope(), "合同管理:委托处理合同:页面");

        Page<OutsourceProcessingContract> page = new Page<>(request.getCurrent(), request.getSize());

        IPage<OutsourceProcessingContract> pageResult = contractMapper.selectContractPage(
                page,
                request.getContractNo(),
                request.getPartyAName(),
                request.getContractStatus(),
                request.getSignTimeStart(),
                request.getSignTimeEnd(),
                request.getSortField(),
                request.getSortOrder(),
                creatorFilter
        );

        return pageResult.convert(this::convertToPageResponse);
    }

    private Integer resolveCreatorFilter(String viewScope, String pageCode) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        if (authService.isAdmin(currentUserId)) {
            return null;
        }

        String resolvedScope = com.erp.common.util.ViewScopeHelper.resolveViewScope(pageCode, viewScope);

        if (com.erp.common.util.ViewScopeHelper.isSelfScope(resolvedScope)) {
            return currentUserId;
        }

        return null;
    }

    @Override
    public OutsourceProcessingContractDetailResponse getContractDetail(Integer contractId) {
        OutsourceProcessingContract contract = contractMapper.selectDetailById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), ResultCodeEnum.DATA_NOT_FOUND.getMessage());
        }
        return convertToDetailResponse(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceProcessingContractDetailResponse createContract(OutsourceProcessingContractCreateRequest request, MultipartFile file) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String userName = SecurityUtil.getCurrentUsername();

        String contractNo = generateContractNo();

        Integer contractFileId = null;
        if (file != null && !file.isEmpty()) {
            com.erp.entity.common.File uploadedFile = fileService.uploadAndSave(file, "OUTSOURCE_CONTRACT", null);
            contractFileId = uploadedFile.getFileId();
        }

        validateMobilePhone(request.getPartyAContactPhone(), "甲方联系电话");
        validateMobilePhone(request.getPartyBContactPhone(), "乙方联系电话");

        OutsourceProcessingContract contract = new OutsourceProcessingContract();
        contract.setContractNo(contractNo);
        contract.setPartyAId(request.getPartyAId());
        contract.setPartyAName(request.getPartyAName());
        contract.setPartyACreditCode(request.getPartyACreditCode());
        contract.setPartyAContact(request.getPartyAContact());
        contract.setPartyAContactPhone(request.getPartyAContactPhone());
        contract.setPartyBName(request.getPartyBName());
        contract.setPartyBCreditCode(request.getPartyBCreditCode());
        contract.setPartyBContact(request.getPartyBContact());
        contract.setPartyBContactPhone(request.getPartyBContactPhone());
        contract.setOwnerEmployeeId(request.getOwnerEmployeeId());
        contract.setOwnerEmployeeName(request.getOwnerEmployeeName());
        contract.setFeeSettlementEnabled(request.getFeeSettlementEnabled());
        contract.setSignTime(request.getSignTime());
        contract.setValidFrom(request.getValidFrom());
        contract.setValidTo(request.getValidTo());
        contract.setContractStatus(STATUS_PENDING_AUDIT);
        contract.setRemark(request.getRemark());
        contract.setContractFileId(contractFileId);
        contract.setCreatorId(userId);
        contract.setCreatorName(userName);
        contract.setCreateTime(LocalDateTime.now());

        contractMapper.insert(contract);

        if (request.getQuotationItems() != null && !request.getQuotationItems().isEmpty()) {
            saveQuotationItems(contract.getContractId(), request.getQuotationItems());
        }

        if (request.getOutOfScopeServices() != null && !request.getOutOfScopeServices().isEmpty()) {
            saveOutOfScopeServices(contract.getContractId(), request.getOutOfScopeServices());
        }

        log.info("创建委外处理合同成功: contractNo={}, contractId={}", contractNo, contract.getContractId());
        return getContractDetail(contract.getContractId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceProcessingContractDetailResponse updateContract(Integer contractId, OutsourceProcessingContractUpdateRequest request, MultipartFile file) {
        OutsourceProcessingContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), ResultCodeEnum.DATA_NOT_FOUND.getMessage());
        }

        if (STATUS_APPROVED.equals(contract.getContractStatus()) ||
            STATUS_COMPLETED.equals(contract.getContractStatus()) ||
            STATUS_ARCHIVED.equals(contract.getContractStatus())) {
            throw new BusinessException(ResultCodeEnum.CONTRACT_LOCKED.getCode(), ResultCodeEnum.CONTRACT_LOCKED.getMessage());
        }

        if (file != null && !file.isEmpty()) {
            com.erp.entity.common.File uploadedFile = fileService.uploadAndSave(file, "OUTSOURCE_CONTRACT", contractId);
            contract.setContractFileId(uploadedFile.getFileId());
        }

        validateMobilePhone(request.getPartyAContactPhone(), "甲方联系电话");
        validateMobilePhone(request.getPartyBContactPhone(), "乙方联系电话");

        contract.setPartyAId(request.getPartyAId());
        contract.setPartyAName(request.getPartyAName());
        contract.setPartyACreditCode(request.getPartyACreditCode());
        contract.setPartyAContact(request.getPartyAContact());
        contract.setPartyAContactPhone(request.getPartyAContactPhone());
        contract.setPartyBName(request.getPartyBName());
        contract.setPartyBCreditCode(request.getPartyBCreditCode());
        contract.setPartyBContact(request.getPartyBContact());
        contract.setPartyBContactPhone(request.getPartyBContactPhone());
        contract.setOwnerEmployeeId(request.getOwnerEmployeeId());
        contract.setOwnerEmployeeName(request.getOwnerEmployeeName());
        contract.setFeeSettlementEnabled(request.getFeeSettlementEnabled());
        contract.setSignTime(request.getSignTime());
        contract.setValidFrom(request.getValidFrom());
        contract.setValidTo(request.getValidTo());
        contract.setRemark(request.getRemark());
        contract.setUpdateTime(LocalDateTime.now());

        contractMapper.updateById(contract);

        if (request.getQuotationItems() != null) {
            deleteQuotationItems(contractId);
            if (!request.getQuotationItems().isEmpty()) {
                saveQuotationItems(contractId, request.getQuotationItems());
            }
        }

        if (request.getOutOfScopeServices() != null) {
            deleteOutOfScopeServices(contractId);
            if (!request.getOutOfScopeServices().isEmpty()) {
                saveOutOfScopeServices(contractId, request.getOutOfScopeServices());
            }
        }

        log.info("更新委外处理合同成功: contractId={}", contractId);
        return getContractDetail(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceProcessingContractOperationResponse deleteContract(Integer contractId) {
        long startTime = System.currentTimeMillis();
        OutsourceProcessingContractOperationResponse response = new OutsourceProcessingContractOperationResponse();

        OutsourceProcessingContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), ResultCodeEnum.DATA_NOT_FOUND.getMessage());
        }

        if (!STATUS_PENDING_AUDIT.equals(contract.getContractStatus()) &&
            !STATUS_REJECTED.equals(contract.getContractStatus())) {
            throw new BusinessException(ResultCodeEnum.CONTRACT_CANNOT_DELETE.getCode(), ResultCodeEnum.CONTRACT_CANNOT_DELETE.getMessage());
        }

        deleteQuotationItems(contractId);
        deleteOutOfScopeServices(contractId);
        contractMapper.deleteById(contractId);

        log.info("删除委外处理合同成功: contractId={}", contractId);

        response.setTotalCount(1);
        response.setSuccessCount(1);
        response.setFailCount(0);
        response.setAllSuccess(true);
        response.setDuration(System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceProcessingContractOperationResponse batchDeleteContract(List<Integer> contractIds) {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;

        for (Integer contractId : contractIds) {
            try {
                deleteContract(contractId);
                successCount++;
            } catch (BusinessException e) {
                log.warn("删除合同失败: contractId={}, error={}", contractId, e.getMessage());
                failCount++;
            }
        }

        OutsourceProcessingContractOperationResponse response = new OutsourceProcessingContractOperationResponse();
        response.setTotalCount(contractIds.size());
        response.setSuccessCount(successCount);
        response.setFailCount(failCount);
        response.setAllSuccess(failCount == 0);
        response.setDuration(System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceProcessingContractDetailResponse updateContractStatus(Integer contractId, String contractStatus, String auditOpinion) {
        OutsourceProcessingContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), ResultCodeEnum.DATA_NOT_FOUND.getMessage());
        }

        Integer userId = SecurityUtil.getCurrentUserId();
        String userName = SecurityUtil.getCurrentUsername();

        contract.setContractStatus(contractStatus);
        contract.setAuditOpinion(auditOpinion);
        if (STATUS_APPROVED.equals(contractStatus) || STATUS_REJECTED.equals(contractStatus)) {
            contract.setAuditorId(userId);
            contract.setAuditorName(userName);
            contract.setAuditTime(LocalDateTime.now());
        }
        contract.setUpdateTime(LocalDateTime.now());

        contractMapper.updateById(contract);
        log.info("更新合同状态: contractId={}, status={}", contractId, contractStatus);
        return getContractDetail(contractId);
    }

    @Override
    public List<OutsourceProcessingContractSelectResponse> getContractSelectList(String keyword) {
        return contractMapper.selectContractSelectList(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceProcessingContractBatchAuditResponse batchSubmitAudit(List<Integer> contractIds) {
        long startTime = System.currentTimeMillis();
        List<Integer> successIds = new ArrayList<>();
        List<Integer> failedIds = new ArrayList<>();
        List<OutsourceProcessingContractBatchAuditResponse.FailedReason> failedReasons = new ArrayList<>();

        for (Integer contractId : contractIds) {
            try {
                OutsourceProcessingContract contract = contractMapper.selectById(contractId);
                if (contract == null) {
                    failedIds.add(contractId);
                    OutsourceProcessingContractBatchAuditResponse.FailedReason reason = new OutsourceProcessingContractBatchAuditResponse.FailedReason();
                    reason.setContractId(contractId);
                    reason.setReason("合同不存在");
                    failedReasons.add(reason);
                    continue;
                }

                if (!STATUS_PENDING_AUDIT.equals(contract.getContractStatus())) {
                    failedIds.add(contractId);
                    OutsourceProcessingContractBatchAuditResponse.FailedReason reason = new OutsourceProcessingContractBatchAuditResponse.FailedReason();
                    reason.setContractId(contractId);
                    reason.setReason("当前状态不允许提交审核");
                    failedReasons.add(reason);
                    continue;
                }

                contract.setContractStatus(STATUS_IN_AUDIT);
                contract.setUpdateTime(LocalDateTime.now());
                contractMapper.updateById(contract);
                successIds.add(contractId);
            } catch (Exception e) {
                failedIds.add(contractId);
                OutsourceProcessingContractBatchAuditResponse.FailedReason reason = new OutsourceProcessingContractBatchAuditResponse.FailedReason();
                reason.setContractId(contractId);
                reason.setReason(e.getMessage());
                failedReasons.add(reason);
            }
        }

        OutsourceProcessingContractBatchAuditResponse response = new OutsourceProcessingContractBatchAuditResponse();
        response.setTotalCount(contractIds.size());
        response.setSuccessCount(successIds.size());
        response.setFailCount(failedIds.size());
        response.setSuccessIds(successIds);
        response.setFailedIds(failedIds);
        response.setFailedReasons(failedReasons);
        response.setAllSuccess(failedIds.isEmpty());
        response.setOperationType("SUBMIT_AUDIT");
        response.setDuration(System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceProcessingContractBatchAuditResponse batchWithdrawAudit(List<Integer> contractIds) {
        long startTime = System.currentTimeMillis();
        List<Integer> successIds = new ArrayList<>();
        List<Integer> failedIds = new ArrayList<>();
        List<OutsourceProcessingContractBatchAuditResponse.FailedReason> failedReasons = new ArrayList<>();

        for (Integer contractId : contractIds) {
            try {
                OutsourceProcessingContract contract = contractMapper.selectById(contractId);
                if (contract == null) {
                    failedIds.add(contractId);
                    OutsourceProcessingContractBatchAuditResponse.FailedReason reason = new OutsourceProcessingContractBatchAuditResponse.FailedReason();
                    reason.setContractId(contractId);
                    reason.setReason("合同不存在");
                    failedReasons.add(reason);
                    continue;
                }

                if (!STATUS_IN_AUDIT.equals(contract.getContractStatus())) {
                    failedIds.add(contractId);
                    OutsourceProcessingContractBatchAuditResponse.FailedReason reason = new OutsourceProcessingContractBatchAuditResponse.FailedReason();
                    reason.setContractId(contractId);
                    reason.setReason("当前状态不允许撤回");
                    failedReasons.add(reason);
                    continue;
                }

                contract.setContractStatus(STATUS_PENDING_AUDIT);
                contract.setUpdateTime(LocalDateTime.now());
                contractMapper.updateById(contract);
                successIds.add(contractId);
            } catch (Exception e) {
                failedIds.add(contractId);
                OutsourceProcessingContractBatchAuditResponse.FailedReason reason = new OutsourceProcessingContractBatchAuditResponse.FailedReason();
                reason.setContractId(contractId);
                reason.setReason(e.getMessage());
                failedReasons.add(reason);
            }
        }

        OutsourceProcessingContractBatchAuditResponse response = new OutsourceProcessingContractBatchAuditResponse();
        response.setTotalCount(contractIds.size());
        response.setSuccessCount(successIds.size());
        response.setFailCount(failedIds.size());
        response.setSuccessIds(successIds);
        response.setFailedIds(failedIds);
        response.setFailedReasons(failedReasons);
        response.setAllSuccess(failedIds.isEmpty());
        response.setOperationType("WITHDRAW_AUDIT");
        response.setDuration(System.currentTimeMillis() - startTime);
        return response;
    }

    // ========== 私有方法 ==========

    private String generateContractNo() {
        // 格式：OPC-YYYYMMDD-XXXXX，与危废合同(HQ)格式保持一致
        String prefix = "OPC-" + java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        OutsourceProcessingContract maxContract = contractMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OutsourceProcessingContract>()
                        .likeRight(OutsourceProcessingContract::getContractNo, prefix)
                        .orderByDesc(OutsourceProcessingContract::getContractNo)
                        .last("LIMIT 1")
        );
        int sequence = 1;
        if (maxContract != null && maxContract.getContractNo() != null) {
            String maxNo = maxContract.getContractNo();
            if (maxNo.length() > prefix.length()) {
                try {
                    sequence = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
                } catch (NumberFormatException e) {
                    sequence = 1;
                }
            }
        }
        return prefix + String.format("%05d", sequence);
    }

    private OutsourceProcessingContractPageResponse convertToPageResponse(OutsourceProcessingContract contract) {
        OutsourceProcessingContractPageResponse response = new OutsourceProcessingContractPageResponse();
        response.setContractId(contract.getContractId());
        response.setContractNo(contract.getContractNo());
        response.setPartyAId(contract.getPartyAId());
        response.setPartyAName(contract.getPartyAName());
        response.setPartyACreditCode(contract.getPartyACreditCode());
        response.setPartyAContact(contract.getPartyAContact());
        response.setPartyAContactPhone(contract.getPartyAContactPhone());
        response.setPartyBName(contract.getPartyBName());
        response.setPartyBCreditCode(contract.getPartyBCreditCode());
        response.setPartyBContact(contract.getPartyBContact());
        response.setPartyBContactPhone(contract.getPartyBContactPhone());
        response.setOwnerEmployeeId(contract.getOwnerEmployeeId());
        response.setOwnerEmployeeName(contract.getOwnerEmployeeName());
        response.setFeeSettlementEnabled(contract.getFeeSettlementEnabled());
        response.setSignTime(contract.getSignTime());
        response.setValidFrom(contract.getValidFrom());
        response.setValidTo(contract.getValidTo());
        response.setContractStatus(contract.getContractStatus());
        response.setAuditorId(contract.getAuditorId());
        response.setAuditorName(contract.getAuditorName());
        response.setAuditTime(contract.getAuditTime());
        response.setAuditOpinion(contract.getAuditOpinion());
        response.setContractFileId(contract.getContractFileId());
        response.setRemark(contract.getRemark());
        response.setCreatorId(contract.getCreatorId());
        response.setCreatorName(contract.getCreatorName());
        response.setCreateTime(contract.getCreateTime());
        response.setUpdateTime(contract.getUpdateTime());
        response.setVersion(contract.getVersion());
        return response;
    }

    private OutsourceProcessingContractDetailResponse convertToDetailResponse(OutsourceProcessingContract contract) {
        OutsourceProcessingContractDetailResponse response = new OutsourceProcessingContractDetailResponse();
        response.setContractId(contract.getContractId());
        response.setContractNo(contract.getContractNo());
        response.setPartyAId(contract.getPartyAId());
        response.setPartyAName(contract.getPartyAName());
        response.setPartyACreditCode(contract.getPartyACreditCode());
        response.setPartyAContact(contract.getPartyAContact());
        response.setPartyAContactPhone(contract.getPartyAContactPhone());
        response.setPartyBName(contract.getPartyBName());
        response.setPartyBCreditCode(contract.getPartyBCreditCode());
        response.setPartyBContact(contract.getPartyBContact());
        response.setPartyBContactPhone(contract.getPartyBContactPhone());
        response.setOwnerEmployeeId(contract.getOwnerEmployeeId());
        response.setOwnerEmployeeName(contract.getOwnerEmployeeName());
        response.setFeeSettlementEnabled(contract.getFeeSettlementEnabled());
        response.setSignTime(contract.getSignTime());
        response.setValidFrom(contract.getValidFrom());
        response.setValidTo(contract.getValidTo());
        response.setContractStatus(contract.getContractStatus());
        response.setAuditorId(contract.getAuditorId());
        response.setAuditorName(contract.getAuditorName());
        response.setAuditTime(contract.getAuditTime());
        response.setAuditOpinion(contract.getAuditOpinion());
        response.setContractFileId(contract.getContractFileId());
        if (contract.getContractFileId() != null) {
            com.erp.entity.common.File file = fileService.getFileById(contract.getContractFileId());
            if (file != null) {
                response.setContractFileName(file.getFileName());
                response.setContractFileUrl(fileService.getFileUrlByFile(file));
            }
        }
        response.setRemark(contract.getRemark());
        response.setCreatorId(contract.getCreatorId());
        response.setCreatorName(contract.getCreatorName());
        response.setCreateTime(contract.getCreateTime());
        response.setUpdateTime(contract.getUpdateTime());

        List<OutsourceProcessingContractItem> items = itemMapper.selectByContractId(contract.getContractId());
        if (items != null && !items.isEmpty()) {
            response.setQuotationItems(items.stream().map(this::convertToItemResponse).collect(Collectors.toList()));
        }

        return response;
    }

    private OutsourceProcessingContractItemResponse convertToItemResponse(OutsourceProcessingContractItem item) {
        OutsourceProcessingContractItemResponse response = new OutsourceProcessingContractItemResponse();
        response.setItemId(item.getItemId());
        response.setContractId(item.getContractId());
        response.setRowNumber(item.getRowNumber());
        response.setPricingMode(item.getPricingMode());
        response.setPayer(item.getPayer());
        response.setPricingStatement(item.getPricingStatement());
        response.setSubtotalSummary(item.getSubtotalSummary());
        response.setRemark(item.getRemark());

        List<OutsourceProcessingContractWasteItem> wasteItems = wasteItemMapper.selectByItemId(item.getItemId());
        if (wasteItems != null && !wasteItems.isEmpty()) {
            response.setWastes(wasteItems.stream().map(this::convertToWasteItemResponse).collect(Collectors.toList()));
        }

        return response;
    }

    private OutsourceProcessingContractWasteItemResponse convertToWasteItemResponse(OutsourceProcessingContractWasteItem wasteItem) {
        OutsourceProcessingContractWasteItemResponse response = new OutsourceProcessingContractWasteItemResponse();
        response.setWasteItemId(wasteItem.getWasteItemId());
        response.setItemId(wasteItem.getItemId());
        response.setRowOrder(wasteItem.getRowOrder());
        response.setHazardousWasteItemId(wasteItem.getHazardousWasteItemId());
        response.setWasteCategory(wasteItem.getWasteCategory());
        response.setWasteCode(wasteItem.getWasteCode());
        response.setWasteName(wasteItem.getWasteName());
        response.setWasteState(wasteItem.getWasteState());
        response.setPlannedQuantity(wasteItem.getPlannedQuantity());
        response.setUnlimitedQuantity(wasteItem.getUnlimitedQuantity());
        response.setQuantityUnit(wasteItem.getQuantityUnit());
        response.setEnableAuxiliaryAccounting(wasteItem.getEnableAuxiliaryAccounting());
        response.setAuxUnit(wasteItem.getAuxUnit());
        response.setAuxPerBase(wasteItem.getAuxPerBase());
        response.setAuxQuantity(wasteItem.getAuxQuantity());
        response.setAuxUnitPrice(wasteItem.getAuxUnitPrice());
        response.setUnitPrice(wasteItem.getUnitPrice());
        response.setOverLimitPrice(wasteItem.getOverLimitPrice());
        response.setOverLimitUnit(wasteItem.getOverLimitUnit());
        response.setFloorPrice(wasteItem.getFloorPrice());
        response.setFloorPriceUnit(wasteItem.getFloorPriceUnit());
        response.setPricingStatement(wasteItem.getPricingStatement());
        response.setPayer(wasteItem.getPayer());
        response.setRemark(wasteItem.getRemark());
        return response;
    }

    private void saveQuotationItems(Integer contractId, List<OutsourceProcessingContractItemRequest> items) {
        for (int i = 0; i < items.size(); i++) {
            OutsourceProcessingContractItemRequest itemRequest = items.get(i);

            OutsourceProcessingContractItem item = new OutsourceProcessingContractItem();
            item.setContractId(contractId);
            item.setRowNumber(itemRequest.getRowNumber() != null ? itemRequest.getRowNumber() : i + 1);
            item.setPricingMode(itemRequest.getPricingMode());
            item.setPayer(itemRequest.getPayer());
            item.setPricingStatement(itemRequest.getPricingStatement());
            item.setRemark(itemRequest.getRemark());
            item.setIsDeleted(false);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());

            itemMapper.insert(item);

            if (itemRequest.getWastes() != null && !itemRequest.getWastes().isEmpty()) {
                saveWasteItems(item.getItemId(), itemRequest.getWastes());
            }
        }
    }

    private void saveWasteItems(Integer itemId, List<OutsourceProcessingContractWasteItemRequest> wastes) {
        for (int i = 0; i < wastes.size(); i++) {
            OutsourceProcessingContractWasteItemRequest wasteRequest = wastes.get(i);

            OutsourceProcessingContractWasteItem wasteItem = new OutsourceProcessingContractWasteItem();
            wasteItem.setItemId(itemId);
            wasteItem.setRowOrder(wasteRequest.getRowOrder() != null ? wasteRequest.getRowOrder() : i + 1);
            wasteItem.setHazardousWasteItemId(wasteRequest.getHazardousWasteItemId());
            wasteItem.setWasteCategory(wasteRequest.getWasteCategory());
            wasteItem.setWasteCode(wasteRequest.getWasteCode());
            wasteItem.setWasteName(wasteRequest.getWasteName());
            wasteItem.setWasteState(wasteRequest.getWasteState());
            wasteItem.setPlannedQuantity(wasteRequest.getPlannedQuantity());
            wasteItem.setUnlimitedQuantity(wasteRequest.getUnlimitedQuantity());
            wasteItem.setQuantityUnit(wasteRequest.getQuantityUnit());
            wasteItem.setEnableAuxiliaryAccounting(wasteRequest.getEnableAuxiliaryAccounting());
            wasteItem.setAuxUnit(wasteRequest.getAuxUnit());
            wasteItem.setAuxPerBase(wasteRequest.getAuxPerBase());
            wasteItem.setAuxQuantity(wasteRequest.getAuxQuantity());
            wasteItem.setAuxUnitPrice(wasteRequest.getAuxUnitPrice());
            wasteItem.setUnitPrice(wasteRequest.getUnitPrice());
            wasteItem.setOverLimitPrice(wasteRequest.getOverLimitPrice());
            wasteItem.setOverLimitUnit(wasteRequest.getOverLimitUnit());
            wasteItem.setFloorPrice(wasteRequest.getFloorPrice());
            wasteItem.setFloorPriceUnit(wasteRequest.getFloorPriceUnit());
            wasteItem.setPricingStatement(wasteRequest.getPricingStatement());
            wasteItem.setPayer(wasteRequest.getPayer());
            wasteItem.setRemark(wasteRequest.getRemark());
            wasteItem.setIsDeleted(false);
            wasteItem.setCreateTime(LocalDateTime.now());
            wasteItem.setUpdateTime(LocalDateTime.now());

            wasteItemMapper.insert(wasteItem);
        }
    }

    private void deleteQuotationItems(Integer contractId) {
        List<OutsourceProcessingContractItem> items = itemMapper.selectByContractId(contractId);
        if (items != null) {
            for (OutsourceProcessingContractItem item : items) {
                wasteItemMapper.deleteByItemId(item.getItemId());
            }
        }
        itemMapper.deleteByContractId(contractId);
    }

    private void deleteOutOfScopeServices(Integer contractId) {
    }

    private void saveOutOfScopeServices(Integer contractId, List<OutsourceProcessingContractOutOfScopeServiceRequest> services) {
    }
}
