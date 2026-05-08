package com.erp.service.contract.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.contract.dto.*;
import com.erp.entity.common.File;
import com.erp.entity.contract.BusinessContract;
import com.erp.entity.contract.BusinessContractWasteInfo;
import com.erp.entity.contract.BusinessContractWasteItem;
import com.erp.entity.contract.Salesperson;
import com.erp.entity.system.Employee;
import com.erp.entity.system.EmployeePermission;
import com.erp.mapper.contract.BusinessContractMapper;
import com.erp.mapper.contract.BusinessContractWasteInfoMapper;
import com.erp.mapper.contract.BusinessContractWasteItemMapper;
import com.erp.mapper.contract.SalespersonMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.entity.system.Permission;
import com.erp.service.auth.AuthService;
import com.erp.service.common.FileService;
import com.erp.service.contract.BusinessContractService;
import com.erp.service.contract.dto.BusinessContractBatchUpdateResponse;
import com.erp.service.system.ILogRecordService;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.service.oa.OaApprovalRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 业务合同 Service 实现
 *
 * 设计原则：
 *   - BUSINESS_CONTRACT 表冗余存储所有业务关键字段（业务员信息、甲乙方公司、收款卡）
 *   - SALESPERSON 表作为业务员档案独立维护，关联编号仅用于审查溯源
 *   - 新增/更新合同时：同步将业务员信息写入 BUSINESS_CONTRACT 冗余字段
 *   - 详情加载/提交时优先使用新结构：wasteItems / wasteInfos
 *   - 即使 SALESPERSON 记录被删除，合同数据仍完整可用
 */
@Slf4j
@Service
public class BusinessContractServiceImpl implements BusinessContractService {

    private static final String BUSINESS_TYPE = "BUSINESS_CONTRACT";
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BusinessContractMapper businessContractMapper;

    @Autowired
    private BusinessContractWasteItemMapper businessContractWasteItemMapper;

    @Autowired
    private BusinessContractWasteInfoMapper businessContractWasteInfoMapper;

    @Autowired
    private SalespersonMapper salespersonMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private OaApprovalRecordService oaApprovalRecordService;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public IPage<BusinessContractPageResponse> getPage(BusinessContractPageRequest request) {
        long offset = (request.getCurrent() - 1) * request.getSize();
        Integer creatorFilter = resolveCreatorFilter(request.getViewScope());

        List<BusinessContract> records = businessContractMapper.selectPageList(
                emptyToNull(request.getContractNo()),
                emptyToNull(request.getSalespersonName()),
                emptyToNull(request.getCompanyName()),
                emptyToNull(request.getStatus()),
                emptyToNull(request.getSignTimeStart()),
                emptyToNull(request.getSignTimeEnd()),
                emptyToNull(request.getSortField()),
                emptyToNull(request.getSortOrder()),
                creatorFilter,
                offset,
                request.getSize()
        );

        long total = businessContractMapper.selectPageCount(
                emptyToNull(request.getContractNo()),
                emptyToNull(request.getSalespersonName()),
                emptyToNull(request.getCompanyName()),
                emptyToNull(request.getStatus()),
                emptyToNull(request.getSignTimeStart()),
                emptyToNull(request.getSignTimeEnd()),
                creatorFilter
        );

        List<BusinessContractPageResponse> responseList = new ArrayList<>(records.size());
        for (BusinessContract bc : records) {
            responseList.add(toPageResponse(bc));
        }

        Page<BusinessContractPageResponse> page = new Page<>(request.getCurrent(), request.getSize());
        page.setTotal(total);
        page.setRecords(responseList);
        return page;
    }

    /**
     * 根据viewScope解析creatorFilter
     * viewScope=SELF时强制返回当前用户ID，viewScope=ALL时返回null（查看全部）
     *
     * @param viewScope 前端传入的viewScope（SELF/ALL/null）
     * @return 创建人ID（仅SELF模式返回当前用户ID），其他情况返回null
     */
    private Integer resolveCreatorFilter(String viewScope) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        // 管理员拥有全部权限
        if (currentUserId != null && authService.isAdmin(currentUserId)) {
            log.debug("[resolveCreatorFilter] 管理员用户，不应用数据范围控制");
            return null;
        }

        // 使用ViewScopeHelper解析视图范围
        String resolvedScope = com.erp.common.util.ViewScopeHelper.resolveViewScope(
                "合同管理:业务合作合同:业务合同:页面", viewScope);

        // SELF模式：仅查看自己创建的数据
        if (com.erp.common.util.ViewScopeHelper.isSelfScope(resolvedScope)) {
            log.debug("[resolveCreatorFilter] viewScope=SELF，强制creatorFilter={}", currentUserId);
            return currentUserId;
        }

        // ALL模式：查看全部数据
        log.debug("[resolveCreatorFilter] viewScope=ALL，查看全部数据");
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(BusinessContractCreateRequest request, MultipartFile file) {
        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("业务员联系电话", request.getSalespersonPhone());
        validatePhoneFormat("乙方联系电话", request.getPartyBContactPhone());

        BusinessContract bc = buildEntity(request);
        bc.setStatus("待审核");
        bc.setDeleted(0);
        bc.setContractNo(generateContractNo());

        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            bc.setCreatorId(userId);
            if (userId != null) {
                Employee creator = employeeMapper.selectById(userId);
                if (creator != null) {
                    bc.setCreatorName(creator.getEmployeeName());
                }
            }
        } catch (Exception ignored) {
            log.warn("新增业务合同时无法获取当前用户信息");
        }

        Integer salespersonId = upsertSalesperson(request, request.getSalespersonId());
        bc.setSalespersonId(salespersonId);

        businessContractMapper.insert(bc);
        saveWasteStructure(bc.getContractId(), request.getWasteItems());
        log.info("新增业务合同成功，contractId={}, contractNo={}", bc.getContractId(), bc.getContractNo());

        // 记录数据变更日志（新增）
        try {
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            logRecordService.recordDataChangeLog("业务合作合同", "BUSINESS_CONTRACT", String.valueOf(bc.getContractId()),
                    "新增", "新增业务合作合同，甲方=" + bc.getPartyAName(),
                    null, bc, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录业务合同新增数据变更日志失败", e);
        }

        if (file != null && !file.isEmpty()) {
            saveContractFile(bc, file);
        }
        return bc.getContractId();
    }

    @Override
    public BusinessContractDetailResponse getDetail(Integer contractId) {
        BusinessContract bc = businessContractMapper.selectById(contractId);
        if (isDeleted(bc)) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        // 补查关联危废合同号，避免前端为展示合同信息卡片而额外发起请求
        if (bc.getHazardousContractId() != null) {
            try {
                String hazardousContractNo = businessContractMapper.selectHazardousContractNo(bc.getHazardousContractId());
                bc.setHazardousContractNo(hazardousContractNo);
            } catch (Exception e) {
                log.warn("补查危废合同号失败：hazardousContractId={}", bc.getHazardousContractId(), e);
            }
        }

        BusinessContractDetailResponse resp = toDetailResponse(bc);
        resp.setWasteItems(loadWasteStructure(contractId));

        if (bc.getContractFileId() != null) {
            try {
                List<File> files = fileService.getFilesByBusiness(BUSINESS_TYPE, bc.getContractId());
                if (!files.isEmpty()) {
                    File f = files.get(0);
                    resp.setContractFileUrl(f.getFileUrl());
                    resp.setContractFileName(f.getFileName());
                }
            } catch (Exception e) {
                log.warn("查询业务合同文件信息失败：contractId={}", contractId, e);
            }
        }
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer contractId, BusinessContractCreateRequest request, MultipartFile file) {
        BusinessContract existing = businessContractMapper.selectById(contractId);
        if (isDeleted(existing)) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }
        if (!"待审核".equals(existing.getStatus()) && !"已驳回".equals(existing.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "仅待审核或已驳回状态的合同可以编辑");
        }

        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("业务员联系电话", request.getSalespersonPhone());
        validatePhoneFormat("乙方联系电话", request.getPartyBContactPhone());

        // 操作范围校验（operateScope）
        validateOperateScope(contractId, existing.getCreatorId());

        Integer salespersonId = upsertSalesperson(request, existing.getSalespersonId());

        BusinessContract bc = buildEntity(request);
        bc.setContractId(contractId);
        bc.setSalespersonId(salespersonId);
        businessContractMapper.updateById(bc);
        replaceWasteStructure(contractId, request.getWasteItems());
        log.info("更新业务合同成功，contractId={}", contractId);

        // 记录数据变更日志（更新）
        try {
            BusinessContract afterUpdate = businessContractMapper.selectById(contractId);
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            logRecordService.recordDataChangeLog("业务合作合同", "BUSINESS_CONTRACT", String.valueOf(contractId),
                    "更新", "更新业务合作合同，甲方=" + existing.getPartyAName(),
                    existing, afterUpdate, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录业务合同更新数据变更日志失败", e);
        }

        if (file != null && !file.isEmpty()) {
            if (existing.getContractFileId() != null) {
                try {
                    fileService.deleteFile(existing.getContractFileId());
                } catch (Exception e) {
                    log.warn("删除旧合同文件失败：fileId={}", existing.getContractFileId(), e);
                }
            }
            saveContractFile(bc, file);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer contractId, BusinessContractStatusRequest request) {
        BusinessContract existing = businessContractMapper.selectById(contractId);
        if (isDeleted(existing)) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        // 操作范围校验（operateScope）
        validateOperateScope(contractId, existing.getCreatorId());

        // ── 状态机校验：合法的状态流转 ──────────────────────────────
        // 待审核    → 审核中（提交审核）
        // 已驳回    → 审核中（修改后重新提交）
        // 审核中    → 执行中（审核通过）
        // 审核中    → 已驳回（审核拒绝）
        // 执行中    → 已完结（标记完结）
        // 已完结    → 已归档（归档）
        String currentStatus = existing.getStatus();
        String targetStatus  = request.getStatus();
        boolean valid = false;
        switch (currentStatus) {
            case "待审核": valid = "审核中".equals(targetStatus); break;
            case "已驳回": valid = "审核中".equals(targetStatus); break;
            case "审核中": valid = "执行中".equals(targetStatus) || "已驳回".equals(targetStatus); break;
            case "执行中": valid = "已完结".equals(targetStatus); break;
            case "已完结": valid = "已归档".equals(targetStatus); break;
            default: valid = false;
        }
        if (!valid) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    String.format("不允许将合同从「%s」变更为「%s」", currentStatus, targetStatus));
        }

        // 拒绝时审核意见必填
        if ("已驳回".equals(targetStatus) &&
                (request.getAuditOpinion() == null || request.getAuditOpinion().trim().isEmpty())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "审核拒绝时审核意见不能为空");
        }

        BusinessContract bc = new BusinessContract();
        bc.setContractId(contractId);
        bc.setStatus(targetStatus);
        bc.setAuditOpinion(request.getAuditOpinion());

        // 提交审核时清空上次审核信息；审核通过/拒绝时记录审核人
        if ("审核中".equals(targetStatus)) {
            bc.setAuditorId(null);
            bc.setAuditorName(null);
            bc.setAuditTime(null);
            bc.setAuditOpinion(null);
        } else if ("执行中".equals(targetStatus) || "已驳回".equals(targetStatus)) {
            bc.setAuditTime(java.time.LocalDateTime.now());
            try {
                Integer userId = SecurityUtil.getCurrentUserId();
                bc.setAuditorId(userId);
                if (userId != null) {
                    Employee auditor = employeeMapper.selectById(userId);
                    if (auditor != null) {
                        bc.setAuditorName(auditor.getEmployeeName());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        businessContractMapper.updateById(bc);
        log.info("更新业务合同状态成功，contractId={}, {} → {}", contractId, currentStatus, targetStatus);

        // ── 同步更新 OA 审批记录（审核通过/审核拒绝时）─────────────────────────────────
        if ("执行中".equals(targetStatus) || "已驳回".equals(targetStatus)) {
            try {
                OaApprovalRecord pendingRecord = oaApprovalRecordService.findPendingBySource("BUSINESS_CONTRACT", contractId);
                if (pendingRecord != null) {
                    String oaResult = "执行中".equals(targetStatus) ? "通过" : "驳回";
                    oaApprovalRecordService.approve(
                            pendingRecord.getApprovalRecordId(),
                            "BUSINESS_CONTRACT",
                            contractId,
                            oaResult,
                            request.getAuditOpinion(),
                            bc.getAuditorId(),
                            bc.getAuditorName()
                    );
                    log.info("业务合同审核后同步更新OA审批记录成功：contractId={}, oaRecordId={}, result={}",
                            contractId, pendingRecord.getApprovalRecordId(), oaResult);
                }
            } catch (Exception e) {
                log.error("同步更新OA审批记录失败：contractId={}", contractId, e);
                // 不影响主流程，仅记录日志
            }
        }

        // 记录数据变更日志（状态更新）
        try {
            BusinessContract afterUpdate = businessContractMapper.selectById(contractId);
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            logRecordService.recordDataChangeLog("业务合作合同", "BUSINESS_CONTRACT", String.valueOf(contractId),
                    "状态更新", "业务合作合同状态更新：" + currentStatus + " -> " + targetStatus,
                    existing, afterUpdate, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录业务合同状态更新数据变更日志失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer contractId) {
        BusinessContract existing = businessContractMapper.selectById(contractId);
        if (isDeleted(existing)) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }
        if (!"待审核".equals(existing.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "仅待审核状态的合同可以删除");
        }

        // 操作范围校验（operateScope）
        validateOperateScope(contractId, existing.getCreatorId());

        replaceWasteStructure(contractId, Collections.emptyList());
        BusinessContract del = new BusinessContract();
        del.setContractId(contractId);
        del.setDeleted(1);
        businessContractMapper.updateById(del);
        log.info("删除业务合同成功，contractId={}", contractId);

        // 记录数据变更日志（删除）
        try {
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            logRecordService.recordDataChangeLog("业务合作合同", "BUSINESS_CONTRACT", String.valueOf(contractId),
                    "删除", "删除业务合作合同，合同单号=" + existing.getContractNo(),
                    existing, del, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录业务合同删除数据变更日志失败", e);
        }
    }

    @Override
    public List<BusinessContractRemittanceOptionDTO> searchRemittanceOptions(String keyword, String viewScope) {
        LambdaQueryWrapper<BusinessContract> wrapper = new LambdaQueryWrapper<BusinessContract>()
                .eq(BusinessContract::getStatus, "执行中")
                .eq(BusinessContract::getDeleted, 0)
                .orderByDesc(BusinessContract::getCreateTime);

        // 应用数据范围过滤
        Integer creatorFilter = resolveCreatorFilter(viewScope);
        if (creatorFilter != null) {
            wrapper.eq(BusinessContract::getCreatorId, creatorFilter);
        }

        int limit = (keyword != null && !keyword.trim().isEmpty()) ? 100 : 20;
        wrapper.last("LIMIT " + limit);

        List<BusinessContract> list = businessContractMapper.selectList(wrapper);
        final String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim().toLowerCase() : null;

        List<BusinessContractRemittanceOptionDTO> result = new ArrayList<>();
        for (BusinessContract bc : list) {
            if (kw != null) {
                String contractNo = bc.getContractNo() != null ? bc.getContractNo().toLowerCase() : "";
                String spName = bc.getSalespersonName() != null ? bc.getSalespersonName().toLowerCase() : "";
                String partyAName = bc.getPartyAName() != null ? bc.getPartyAName().toLowerCase() : "";
                if (!contractNo.contains(kw) && !spName.contains(kw) && !partyAName.contains(kw)) {
                    continue;
                }
            }

            BusinessContractRemittanceOptionDTO dto = new BusinessContractRemittanceOptionDTO();
            dto.setContractId(bc.getContractId());
            dto.setContractNo(bc.getContractNo());
            dto.setHazardousContractId(bc.getHazardousContractId());
            dto.setSalespersonName(bc.getSalespersonName());
            dto.setPartyAName(bc.getPartyAName());
            dto.setStatus(bc.getStatus());
            dto.setBankName(bc.getBankName());
            dto.setCardNumber(bc.getCardNumber());
            dto.setAccountName(bc.getAccountName());
            result.add(dto);
        }
        log.debug("查询到 {} 条业务合同记录（关键字={}, viewScope={}）", result.size(), keyword, viewScope);
        return result;
    }

    @Override
    public com.erp.controller.contract.dto.BusinessSettlementContractListResponse getSettlementList() {
        List<java.util.Map<String, Object>> rows = businessContractMapper.selectForSettlement();
        List<com.erp.controller.contract.dto.BusinessSettlementContractListResponse.BusinessSettlementContractRecord> records =
                new ArrayList<>(rows.size());
        for (java.util.Map<String, Object> row : rows) {
            com.erp.controller.contract.dto.BusinessSettlementContractListResponse.BusinessSettlementContractRecord rec =
                    new com.erp.controller.contract.dto.BusinessSettlementContractListResponse.BusinessSettlementContractRecord();
            rec.setBusinessContractId(toInteger(row.get("businessContractId")));
            rec.setBusinessContractNo(toStr(row.get("businessContractNo")));
            rec.setSalespersonName(toStr(row.get("salespersonName")));
            rec.setSalespersonPhone(toStr(row.get("salespersonPhone")));
            rec.setSalespersonIdCard(toStr(row.get("salespersonIdCard")));
            rec.setPartyAName(toStr(row.get("partyAName")));
            rec.setPartyACreditCode(toStr(row.get("partyACreditCode")));
            rec.setStatus(toStr(row.get("status")));
            rec.setContractId(toInteger(row.get("contractId")));
            rec.setContractNo(toStr(row.get("contractNo")));
            rec.setUnlinkedSettlementCount(toInteger(row.get("unlinkedSettlementCount")));
            records.add(rec);
        }
        com.erp.controller.contract.dto.BusinessSettlementContractListResponse response =
                new com.erp.controller.contract.dto.BusinessSettlementContractListResponse();
        response.setRecords(records);
        response.setOrphanBusinessFeeCount((int) businessContractMapper.countOrphanBusinessFeeSettlements());
        log.info("业务合同列表专用查询完成，共 {} 条，游离业务费结算单: {}",
                records.size(), response.getOrphanBusinessFeeCount());
        return response;
    }

    /** 安全转 Integer */
    private Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    /** 安全转 String */
    private String toStr(Object val) {
        return val == null ? null : val.toString();
    }

    @Override
    public BusinessContractDetailResponse getByHazardousContractId(Integer hazardousContractId) {
        if (hazardousContractId == null) {
            return null;
        }
        BusinessContract bc = businessContractMapper.selectOne(
                new LambdaQueryWrapper<BusinessContract>()
                        .eq(BusinessContract::getHazardousContractId, hazardousContractId)
                        .eq(BusinessContract::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (bc == null) {
            return null;
        }
        BusinessContractDetailResponse resp = toDetailResponse(bc);
        resp.setWasteItems(loadWasteStructure(bc.getContractId()));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createByHazardousContract(Integer hazardousContractId, Integer salespersonId,
                                             String salespersonName, String salespersonPhone) {
        BusinessContract existing = businessContractMapper.selectOne(
                new LambdaQueryWrapper<BusinessContract>()
                        .eq(BusinessContract::getHazardousContractId, hazardousContractId)
                        .eq(BusinessContract::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return existing.getContractId();
        }

        Salesperson spRecord = null;
        if (salespersonId != null) {
            spRecord = salespersonMapper.selectById(salespersonId);
        }

        Integer resolvedSalespersonId = salespersonId;
        if (resolvedSalespersonId == null && salespersonName != null) {
            Salesperson sp = new Salesperson();
            sp.setSalespersonName(salespersonName);
            sp.setSalespersonPhone(salespersonPhone);
            sp.setDeleted(0);
            try {
                Integer userId = SecurityUtil.getCurrentUserId();
                sp.setCreatorId(userId);
                if (userId != null) {
                    Employee creator = employeeMapper.selectById(userId);
                    if (creator != null) {
                        sp.setCreatorName(creator.getEmployeeName());
                    }
                }
            } catch (Exception ignored) {
                log.warn("自动创建业务员记录时无法获取当前用户信息");
            }
            salespersonMapper.insert(sp);
            resolvedSalespersonId = sp.getSalespersonId();
            spRecord = sp;
        }

        BusinessContract bc = new BusinessContract();
        bc.setHazardousContractId(hazardousContractId);
        bc.setSalespersonId(resolvedSalespersonId);

        if (spRecord != null) {
            bc.setSalespersonName(spRecord.getSalespersonName());
            bc.setSalespersonPhone(spRecord.getSalespersonPhone());
            bc.setSalespersonIdCard(spRecord.getSalespersonIdCard());
            bc.setPartyAName(spRecord.getPartyAName());
            bc.setPartyACreditCode(spRecord.getPartyACreditCode());
            bc.setPartyBName(spRecord.getPartyBName());
            bc.setPartyBCreditCode(spRecord.getPartyBCreditCode());
            bc.setPartyBContactPerson(spRecord.getPartyBContactPerson());
            bc.setPartyBContactPhone(spRecord.getPartyBContactPhone());
            bc.setBankName(spRecord.getBankName());
            bc.setCardNumber(spRecord.getCardNumber());
            bc.setAccountName(spRecord.getAccountName());
        } else {
            bc.setSalespersonName(salespersonName);
            bc.setSalespersonPhone(salespersonPhone);
        }

        bc.setStatus("待审核");
        bc.setDeleted(0);
        bc.setContractNo(generateContractNo());

        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            bc.setCreatorId(userId);
            if (userId != null) {
                Employee creator = employeeMapper.selectById(userId);
                if (creator != null) {
                    bc.setCreatorName(creator.getEmployeeName());
                }
            }
        } catch (Exception ignored) {
            log.warn("自动创建业务合同时无法获取当前用户信息");
        }

        businessContractMapper.insert(bc);
        log.info("危废合同 {} 自动创建业务合同，contractId={}", hazardousContractId, bc.getContractId());
        return bc.getContractId();
    }

    private void saveContractFile(BusinessContract bc, MultipartFile file) {
        try {
            File saved = fileService.uploadAndSave(file, BUSINESS_TYPE, bc.getContractId());
            BusinessContract update = new BusinessContract();
            update.setContractId(bc.getContractId());
            update.setContractFileId(saved.getFileId());
            update.setContractFilePath(saved.getFileUrl());
            businessContractMapper.updateById(update);
            bc.setContractFileId(saved.getFileId());
            bc.setContractFilePath(saved.getFileUrl());
        } catch (Exception e) {
            log.error("业务合同文件上传失败：contractId={}", bc.getContractId(), e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "合同文件上传失败：" + e.getMessage());
        }
    }

    private Integer upsertSalesperson(BusinessContractCreateRequest req, Integer existingSalespersonId) {
        Salesperson sp;
        Integer resolvedId = req.getSalespersonId() != null ? req.getSalespersonId() : existingSalespersonId;
        if (resolvedId != null) {
            sp = salespersonMapper.selectById(resolvedId);
            if (sp == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "业务员档案不存在");
            }
        } else {
            sp = new Salesperson();
        }

        sp.setSalespersonName(req.getSalespersonName());
        sp.setSalespersonPhone(req.getSalespersonPhone());
        sp.setSalespersonIdCard(req.getSalespersonIdCard());
        sp.setPartyAName(req.getPartyAName());
        sp.setPartyACreditCode(req.getPartyACreditCode());
        sp.setPartyBName(req.getPartyBName());
        sp.setPartyBCreditCode(req.getPartyBCreditCode());
        sp.setPartyBContactPerson(req.getPartyBContactPerson());
        sp.setPartyBContactPhone(req.getPartyBContactPhone());
        sp.setBankName(req.getBankName());
        sp.setCardNumber(req.getCardNumber());
        sp.setAccountName(req.getAccountName());
        sp.setRemark(req.getRemark());
        sp.setDeleted(0);

        if (sp.getSalespersonId() == null) {
            try {
                Integer userId = SecurityUtil.getCurrentUserId();
                sp.setCreatorId(userId);
                if (userId != null) {
                    Employee creator = employeeMapper.selectById(userId);
                    if (creator != null) {
                        sp.setCreatorName(creator.getEmployeeName());
                    }
                }
            } catch (Exception ignored) {
                log.warn("保存业务员记录时无法获取当前用户信息");
            }
            salespersonMapper.insert(sp);
        } else {
            salespersonMapper.updateById(sp);
        }
        return sp.getSalespersonId();
    }

    private BusinessContract buildEntity(BusinessContractCreateRequest req) {
        BusinessContract bc = new BusinessContract();
        bc.setHazardousContractId(req.getHazardousContractId());
        bc.setSalespersonName(req.getSalespersonName());
        bc.setSalespersonPhone(req.getSalespersonPhone());
        bc.setSalespersonIdCard(req.getSalespersonIdCard());
        bc.setPartyAName(req.getPartyAName());
        bc.setPartyACreditCode(req.getPartyACreditCode());
        bc.setPartyBName(req.getPartyBName());
        bc.setPartyBCreditCode(req.getPartyBCreditCode());
        bc.setPartyBContactPerson(req.getPartyBContactPerson());
        bc.setPartyBContactPhone(req.getPartyBContactPhone());
        bc.setBankName(req.getBankName());
        bc.setCardNumber(req.getCardNumber());
        bc.setAccountName(req.getAccountName());
        bc.setRemark(req.getRemark());
        bc.setSignTime(parseDate(req.getSignTime()));
        bc.setValidFrom(parseDate(req.getValidFrom()));
        bc.setValidTo(parseDate(req.getValidTo()));
        return bc;
    }

    private void saveWasteStructure(Integer contractId, List<BusinessContractCreateRequest.WasteItemDTO> wasteItems) {
        if (wasteItems == null || wasteItems.isEmpty()) {
            return;
        }

        for (int i = 0; i < wasteItems.size(); i++) {
            BusinessContractCreateRequest.WasteItemDTO itemDto = wasteItems.get(i);
            if (!hasWasteItemContent(itemDto)) {
                continue;
            }

            BusinessContractWasteItem item = new BusinessContractWasteItem();
            item.setContractId(contractId);
            item.setRowNo(itemDto.getRowNo() != null ? itemDto.getRowNo() : i + 1);
            item.setSourceQuotationItemId(itemDto.getSourceQuotationItemId());
            item.setSettlementType(itemDto.getSettlementType());
            item.setUnitFloorPrice(itemDto.getUnitFloorPrice());
            item.setContractFloorPrice(itemDto.getContractFloorPrice());
            businessContractWasteItemMapper.insert(item);

            List<BusinessContractCreateRequest.WasteInfoDTO> wasteInfos = itemDto.getWasteInfos();
            if (wasteInfos == null || wasteInfos.isEmpty()) {
                continue;
            }

            for (int j = 0; j < wasteInfos.size(); j++) {
                BusinessContractCreateRequest.WasteInfoDTO infoDto = wasteInfos.get(j);
                if (!hasWasteInfoContent(infoDto)) {
                    continue;
                }
                BusinessContractWasteInfo info = new BusinessContractWasteInfo();
                info.setWasteItemId(item.getWasteItemId());
                info.setInnerRowNo(infoDto.getInnerRowNo() != null ? infoDto.getInnerRowNo() : j + 1);
                info.setSourceWasteItemId(infoDto.getSourceWasteItemId());
                info.setWasteType(infoDto.getWasteType());
                info.setWasteCode(infoDto.getWasteCode());
                info.setWasteName(infoDto.getWasteName());
                businessContractWasteInfoMapper.insert(info);
            }
        }
    }

    private void replaceWasteStructure(Integer contractId, List<BusinessContractCreateRequest.WasteItemDTO> wasteItems) {
        List<BusinessContractWasteItem> existingItems = businessContractWasteItemMapper.selectByContractId(contractId);
        if (existingItems != null && !existingItems.isEmpty()) {
            List<Integer> wasteItemIds = existingItems.stream()
                    .map(BusinessContractWasteItem::getWasteItemId)
                    .collect(Collectors.toList());
            if (!wasteItemIds.isEmpty()) {
                businessContractWasteInfoMapper.deleteByWasteItemIds(wasteItemIds);
            }
        }
        businessContractWasteItemMapper.deleteByContractId(contractId);
        saveWasteStructure(contractId, wasteItems);
    }

    private List<BusinessContractDetailResponse.WasteItemDTO> loadWasteStructure(Integer contractId) {
        List<BusinessContractWasteItem> wasteItems = businessContractWasteItemMapper.selectByContractId(contractId);
        if (wasteItems == null || wasteItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> wasteItemIds = wasteItems.stream()
                .map(BusinessContractWasteItem::getWasteItemId)
                .collect(Collectors.toList());
        List<BusinessContractWasteInfo> wasteInfos = wasteItemIds.isEmpty()
                ? Collections.emptyList()
                : businessContractWasteInfoMapper.selectByWasteItemIds(wasteItemIds);

        Map<Integer, List<BusinessContractWasteInfo>> infoMap = new LinkedHashMap<>();
        for (BusinessContractWasteInfo info : wasteInfos) {
            infoMap.computeIfAbsent(info.getWasteItemId(), key -> new ArrayList<>()).add(info);
        }

        List<BusinessContractDetailResponse.WasteItemDTO> result = new ArrayList<>();
        for (BusinessContractWasteItem item : wasteItems) {
            BusinessContractDetailResponse.WasteItemDTO dto = new BusinessContractDetailResponse.WasteItemDTO();
            dto.setWasteItemId(item.getWasteItemId());
            dto.setContractId(item.getContractId());
            dto.setRowNo(item.getRowNo());
            dto.setSourceQuotationItemId(item.getSourceQuotationItemId());
            dto.setSettlementType(item.getSettlementType());
            dto.setUnitFloorPrice(item.getUnitFloorPrice());
            dto.setContractFloorPrice(item.getContractFloorPrice());

            List<BusinessContractDetailResponse.WasteInfoDTO> infoDtos = new ArrayList<>();
            for (BusinessContractWasteInfo info : infoMap.getOrDefault(item.getWasteItemId(), Collections.emptyList())) {
                BusinessContractDetailResponse.WasteInfoDTO infoDto = new BusinessContractDetailResponse.WasteInfoDTO();
                infoDto.setWasteInfoId(info.getWasteInfoId());
                infoDto.setWasteItemId(info.getWasteItemId());
                infoDto.setInnerRowNo(info.getInnerRowNo());
                infoDto.setSourceWasteItemId(info.getSourceWasteItemId());
                infoDto.setWasteType(info.getWasteType());
                infoDto.setWasteCode(info.getWasteCode());
                infoDto.setWasteName(info.getWasteName());
                infoDtos.add(infoDto);
            }
            dto.setWasteInfos(infoDtos);
            result.add(dto);
        }
        return result;
    }

    private boolean hasWasteItemContent(BusinessContractCreateRequest.WasteItemDTO itemDto) {
        if (itemDto == null) {
            return false;
        }
        if (itemDto.getSettlementType() != null && !itemDto.getSettlementType().trim().isEmpty()) {
            return true;
        }
        if (itemDto.getUnitFloorPrice() != null || itemDto.getContractFloorPrice() != null) {
            return true;
        }
        List<BusinessContractCreateRequest.WasteInfoDTO> wasteInfos = itemDto.getWasteInfos();
        if (wasteInfos == null || wasteInfos.isEmpty()) {
            return false;
        }
        for (BusinessContractCreateRequest.WasteInfoDTO infoDto : wasteInfos) {
            if (hasWasteInfoContent(infoDto)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasWasteInfoContent(BusinessContractCreateRequest.WasteInfoDTO infoDto) {
        if (infoDto == null) {
            return false;
        }
        return notBlank(infoDto.getWasteType()) || notBlank(infoDto.getWasteCode()) || notBlank(infoDto.getWasteName());
    }

    private BusinessContractPageResponse toPageResponse(BusinessContract bc) {
        BusinessContractPageResponse resp = new BusinessContractPageResponse();
        resp.setContractId(bc.getContractId());
        resp.setContractNo(bc.getContractNo());
        resp.setHazardousContractId(bc.getHazardousContractId());
        resp.setHazardousContractNo(bc.getHazardousContractNo());
        resp.setHazardousContractNo(bc.getHazardousContractNo());
        resp.setSalespersonId(bc.getSalespersonId());
        resp.setSalespersonName(bc.getSalespersonName());
        resp.setSalespersonPhone(bc.getSalespersonPhone());
        resp.setSalespersonIdCard(bc.getSalespersonIdCard());
        resp.setCompanyName(bc.getPartyAName());
        resp.setPartyACreditCode(bc.getPartyACreditCode());
        resp.setBankName(bc.getBankName());
        resp.setCardNumber(bc.getCardNumber());
        resp.setAccountName(bc.getAccountName());
        resp.setStatus(bc.getStatus());
        resp.setAuditOpinion(bc.getAuditOpinion());
        resp.setAuditorId(bc.getAuditorId());
        resp.setAuditorName(bc.getAuditorName());
        if (bc.getAuditTime() != null) {
            resp.setAuditTime(bc.getAuditTime().format(DATETIME_FMT));
        }
        resp.setCreatorId(bc.getCreatorId());
        resp.setCreatorName(bc.getCreatorName());
        resp.setRemark(bc.getRemark());
        resp.setContractFileId(bc.getContractFileId());
        resp.setContractFilePath(bc.getContractFilePath());
        if (bc.getSignTime() != null) {
            resp.setSignTime(bc.getSignTime().toString());
        }
        if (bc.getValidTo() != null) {
            resp.setValidTo(bc.getValidTo().toString());
        }
        if (bc.getCreateTime() != null) {
            resp.setCreateTime(bc.getCreateTime().format(DATETIME_FMT));
        }
        return resp;
    }

    private BusinessContractDetailResponse toDetailResponse(BusinessContract bc) {
        BusinessContractDetailResponse resp = new BusinessContractDetailResponse();
        resp.setContractId(bc.getContractId());
        resp.setContractNo(bc.getContractNo());
        resp.setHazardousContractId(bc.getHazardousContractId());
        resp.setSalespersonId(bc.getSalespersonId());
        resp.setSalespersonName(bc.getSalespersonName());
        resp.setSalespersonPhone(bc.getSalespersonPhone());
        resp.setSalespersonIdCard(bc.getSalespersonIdCard());
        resp.setPartyAName(bc.getPartyAName());
        resp.setPartyACreditCode(bc.getPartyACreditCode());
        resp.setPartyBName(bc.getPartyBName());
        resp.setPartyBCreditCode(bc.getPartyBCreditCode());
        resp.setPartyBContactPerson(bc.getPartyBContactPerson());
        resp.setPartyBContactPhone(bc.getPartyBContactPhone());
        resp.setBankName(bc.getBankName());
        resp.setCardNumber(bc.getCardNumber());
        resp.setAccountName(bc.getAccountName());
        resp.setStatus(bc.getStatus());
        resp.setAuditOpinion(bc.getAuditOpinion());
        resp.setAuditorId(bc.getAuditorId());
        resp.setAuditorName(bc.getAuditorName());
        resp.setAuditTime(bc.getAuditTime());
        if (bc.getSignTime() != null) {
            resp.setSignTime(bc.getSignTime().toString());
        }
        if (bc.getValidFrom() != null) {
            resp.setValidFrom(bc.getValidFrom().toString());
        }
        if (bc.getValidTo() != null) {
            resp.setValidTo(bc.getValidTo().toString());
        }
        resp.setContractFileId(bc.getContractFileId());
        resp.setContractFilePath(bc.getContractFilePath());
        resp.setRemark(bc.getRemark());
        resp.setCreatorId(bc.getCreatorId());
        resp.setCreatorName(bc.getCreatorName());
        resp.setCreateTime(bc.getCreateTime());
        resp.setUpdateTime(bc.getUpdateTime());
        return resp;
    }

    private LocalDate parseDate(String value) {
        if (!notBlank(value)) return null;
        String trimmed = value.trim();
        // 兼容 "yyyy-MM-dd HH:mm:ss" 或 "yyyy-MM-dd HH:mm" 格式，截取日期部分
        if (trimmed.length() > 10 && trimmed.charAt(10) == ' ') {
            trimmed = trimmed.substring(0, 10);
        }
        return LocalDate.parse(trimmed);
    }

    private static String emptyToNull(String val) {
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }

    private boolean notBlank(String val) {
        return val != null && !val.trim().isEmpty();
    }

    private static boolean isDeleted(BusinessContract bc) {
        return bc == null || Integer.valueOf(1).equals(bc.getDeleted());
    }

    @Override
    public java.util.Map<String, Long> getStatistics() {
        long executing = businessContractMapper.selectCount(
                new LambdaQueryWrapper<BusinessContract>()
                        .eq(BusinessContract::getStatus, "执行中")
                        .eq(BusinessContract::getDeleted, 0));
        long draft = businessContractMapper.selectCount(
                new LambdaQueryWrapper<BusinessContract>()
                        .eq(BusinessContract::getStatus, "待审核")
                        .eq(BusinessContract::getDeleted, 0));
        long reviewing = businessContractMapper.selectCount(
                new LambdaQueryWrapper<BusinessContract>()
                        .eq(BusinessContract::getStatus, "审核中")
                        .eq(BusinessContract::getDeleted, 0));
        long completed = businessContractMapper.selectCount(
                new LambdaQueryWrapper<BusinessContract>()
                        .eq(BusinessContract::getStatus, "已完结")
                        .eq(BusinessContract::getDeleted, 0));
        long archived = businessContractMapper.selectCount(
                new LambdaQueryWrapper<BusinessContract>()
                        .eq(BusinessContract::getStatus, "已归档")
                        .eq(BusinessContract::getDeleted, 0));
        long rejected = businessContractMapper.selectCount(
                new LambdaQueryWrapper<BusinessContract>()
                        .eq(BusinessContract::getStatus, "已驳回")
                        .eq(BusinessContract::getDeleted, 0));
        java.util.Map<String, Long> result = new java.util.HashMap<>();
        result.put("draft", draft);
        result.put("reviewing", reviewing);
        result.put("pendingAudit", reviewing);  // 兼容旧字段
        result.put("executing", executing);
        result.put("completed", completed);
        result.put("archived", archived);
        result.put("rejected", rejected);
        return result;
    }

    private String generateContractNo() {
        String prefix = "BC-" + java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        BusinessContract maxContract = businessContractMapper.selectOne(
                new LambdaQueryWrapper<BusinessContract>()
                        .likeRight(BusinessContract::getContractNo, prefix)
                        .orderByDesc(BusinessContract::getContractNo)
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

    /**
     * 批量更新合同状态
     *
     * 支持以下场景：
     * 1. 批量提交审核：待审核/已驳回 → 审核中，同时在OA审核记录表创建记录
     * 2. 批量撤回：审核中 → 待审核，同时更新OA审核记录
     *
     * @param contractIds 合同ID列表
     * @param status     目标状态
     * @return 批量操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessContractBatchUpdateResponse batchUpdateStatus(List<Integer> contractIds, String status) {
        if (contractIds == null || contractIds.isEmpty()) {
            BusinessContractBatchUpdateResponse response = new BusinessContractBatchUpdateResponse();
            response.setSuccessIds(new ArrayList<>());
            response.setFailedIds(new ArrayList<>());
            response.setAllSuccess(true);
            return response;
        }

        Integer currentUserId = null;
        String currentUserName = "未知";
        try {
            currentUserId = SecurityUtil.getCurrentUserId();
            if (currentUserId != null) {
                Employee employee = employeeMapper.selectById(currentUserId);
                if (employee != null) {
                    currentUserName = employee.getEmployeeName();
                }
            }
        } catch (Exception e) {
            log.warn("获取当前用户信息失败", e);
        }

        List<Integer> successIds = new ArrayList<>();
        List<Integer> failedIds = new ArrayList<>();
        List<BusinessContractBatchUpdateResponse.FailedReason> failedReasons = new ArrayList<>();

        // 查询所有合同
        List<BusinessContract> contracts = businessContractMapper.selectBatchIds(contractIds);
        Map<Integer, BusinessContract> contractMap = contracts.stream()
                .collect(Collectors.toMap(BusinessContract::getContractId, Function.identity()));

        // 操作范围校验（operateScope）- 批量操作时预先检查
        boolean isAdmin = currentUserId != null && authService.isAdmin(currentUserId);
        EmployeePermission permission = null;
        if (!isAdmin) {
            permission = getEmployeePagePermission(currentUserId, "合同管理:业务合作合同:业务合同:页面");
        }
        final EmployeePermission finalPermission = permission;
        final boolean finalIsAdmin = isAdmin;
        final Integer finalCurrentUserId = currentUserId;

        for (Integer contractId : contractIds) {
            BusinessContract contract = contractMap.get(contractId);
            if (contract == null || isDeleted(contract)) {
                continue;
            }
            // operateScope=SELF时，仅允许操作自己创建的合同
            if (!finalIsAdmin && finalPermission != null && "SELF".equalsIgnoreCase(finalPermission.getOperateScope())) {
                if (!Objects.equals(contract.getCreatorId(), finalCurrentUserId)) {
                    failedIds.add(contractId);
                    failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(
                            contractId, "您只能操作自己创建的合同"));
                    contractMap.remove(contractId);
                }
            }
        }

        for (Integer contractId : contractIds) {
            BusinessContract contract = contractMap.get(contractId);

            // 验证合同是否存在
            if (contract == null || isDeleted(contract)) {
                failedIds.add(contractId);
                failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(contractId, "合同不存在"));
                continue;
            }

            String currentStatus = contract.getStatus();

            // 批量提交审核：待审核/已驳回 → 审核中（撤回后业务表状态为待审核）
            if ("审核中".equals(status)) {
                if (!"待审核".equals(currentStatus) && !"已驳回".equals(currentStatus)) {
                    failedIds.add(contractId);
                    failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(
                            contractId, "当前状态为【" + currentStatus + "】，仅待审核、已驳回状态可以提交审核"));
                    continue;
                }

                try {
                    OaApprovalRecord approvalRecord;
                    String actionType;

                    // 查询OA表中是否有已撤回的记录
                    OaApprovalRecord withdrawnRecord = oaApprovalRecordService.findWithdrawnBySource("BUSINESS_CONTRACT", contractId);
                    if (withdrawnRecord != null) {
                        // 有已撤回记录：重新激活该记录，状态改为待审核，审核次数+1
                        approvalRecord = oaApprovalRecordService.reactivateWithdrawnRecord(
                                "BUSINESS_CONTRACT",
                                contractId,
                                currentUserId,
                                currentUserName
                        );
                        actionType = "重新激活";
                    } else {
                        // 无已撤回记录：检查是否已有待审核的OA审批记录
                        OaApprovalRecord existingRecord = oaApprovalRecordService.findPendingBySource("BUSINESS_CONTRACT", contractId);
                        if (existingRecord != null) {
                            failedIds.add(contractId);
                            failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(
                                    contractId, "该合同已存在待审核的OA审批记录"));
                            continue;
                        }

                        // 创建新的OA审批记录
                        String contractTitle = String.format("业务合作合同：%s",
                                contract.getContractNo() != null ? contract.getContractNo() : "BC" + contractId);
                        approvalRecord = oaApprovalRecordService.submit(
                                "BUSINESS_CONTRACT",
                                contractId,
                                "业务合作合同",
                                contract.getContractNo(),
                                contractTitle,
                                currentUserId,
                                currentUserName
                        );
                        actionType = "新建";
                    }

                    // 更新合同状态为审核中
                    BusinessContract update = new BusinessContract();
                    update.setContractId(contractId);
                    update.setStatus("审核中");
                    update.setAuditorId(null);
                    update.setAuditorName(null);
                    update.setAuditTime(null);
                    update.setAuditOpinion(null);
                    businessContractMapper.updateById(update);

                    successIds.add(contractId);
                    log.info("业务合同提交OA审核{}成功：contractId={}, approvalRecordId={}, approvalCount={}",
                            actionType, contractId, approvalRecord.getApprovalRecordId(), approvalRecord.getApprovalCount());

                } catch (Exception e) {
                    failedIds.add(contractId);
                    failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(
                            contractId, "提交审核失败：" + e.getMessage()));
                    log.error("业务合同提交OA审核失败：contractId={}", contractId, e);
                }

            // 批量撤回：审核中 → 待审核
            } else if ("待审核".equals(status)) {
                if (!"审核中".equals(currentStatus)) {
                    failedIds.add(contractId);
                    failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(
                            contractId, "当前状态为【" + currentStatus + "】，仅审核中的合同可以撤回"));
                    continue;
                }

                try {
                    OaApprovalRecord pendingRecord = oaApprovalRecordService.findPendingBySource("BUSINESS_CONTRACT", contractId);
                    if (pendingRecord == null) {
                        failedIds.add(contractId);
                        failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(
                                contractId, "未找到待审核的OA审批记录，无法撤回"));
                        continue;
                    }

                    // 撤回OA审批记录
                    oaApprovalRecordService.cancel(pendingRecord.getApprovalRecordId(), "BUSINESS_CONTRACT",
                            contractId, currentUserId, "批量撤回");

                    // 更新合同状态为待审核
                    BusinessContract update = new BusinessContract();
                    update.setContractId(contractId);
                    update.setStatus("待审核");
                    businessContractMapper.updateById(update);

                    successIds.add(contractId);
                    log.info("业务合同撤回成功：contractId={}, approvalRecordId={}", contractId, pendingRecord.getApprovalRecordId());

                } catch (Exception e) {
                    failedIds.add(contractId);
                    failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(
                            contractId, "撤回失败：" + e.getMessage()));
                    log.error("业务合同撤回失败：contractId={}", contractId, e);
                }

            // 其他状态变更（如执行中、已完结等）
            } else {
                // 调用现有的状态更新逻辑
                BusinessContractStatusRequest statusRequest = new BusinessContractStatusRequest();
                statusRequest.setStatus(status);
                try {
                    updateStatus(contractId, statusRequest);
                    successIds.add(contractId);
                } catch (Exception e) {
                    failedIds.add(contractId);
                    failedReasons.add(new BusinessContractBatchUpdateResponse.FailedReason(
                            contractId, "状态更新失败：" + e.getMessage()));
                    log.error("业务合同状态更新失败：contractId={}", contractId, e);
                }
            }
        }

        // 构建响应
        BusinessContractBatchUpdateResponse response = new BusinessContractBatchUpdateResponse();
        response.setSuccessIds(successIds);
        response.setFailedIds(failedIds);
        response.setFailedReasons(failedReasons);
        response.setAllSuccess(failedIds.isEmpty());

        log.info("业务合同批量状态更新完成：目标状态={}, successCount={}, failedCount={}",
                status, successIds.size(), failedIds.size());

        return response;
    }

    /**
     * 获取员工对指定页面的权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        if (employeeId == null || pageCode == null) {
            return null;
        }
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
            return employeePermissionMapper.selectOne(
                new LambdaQueryWrapper<EmployeePermission>()
                    .eq(EmployeePermission::getEmployeeId, employeeId)
                    .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
            );
        } catch (Exception e) {
            log.warn("查询员工页面权限配置失败：employeeId={}, pageCode={}, error={}",
                    employeeId, pageCode, e.getMessage());
            return null;
        }
    }

    /**
     * 校验操作范围（operateScope）
     * operateScope=SELF时，仅允许操作自己创建的数据
     */
    private void validateOperateScope(Integer contractId, Integer creatorId) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "用户未登录");
        }
        boolean isAdmin = authService.isAdmin(currentUserId);
        if (isAdmin) {
            return; // 管理员不限制
        }
        EmployeePermission permission = getEmployeePagePermission(currentUserId, "合同管理:业务合作合同:业务合同:页面");
        if (permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
            if (!Objects.equals(creatorId, currentUserId)) {
                throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(),
                        "您只能操作自己创建的合同");
            }
        }
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
}
