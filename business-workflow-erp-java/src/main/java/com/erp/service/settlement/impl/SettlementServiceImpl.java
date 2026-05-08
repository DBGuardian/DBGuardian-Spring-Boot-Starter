package com.erp.service.settlement.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.enums.DeletableSettlementStatus;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.*;
import com.erp.controller.settlement.dto.*;
import com.erp.controller.settlement.dto.SettledWarehousingQuantityRequestDTO.WasteItemRequest;
import com.erp.controller.settlement.dto.SettledWarehousingQuantityResponseDTO.SettledQuantityItem;
import com.erp.controller.contract.dto.ContractWasteItemDTO;
import com.erp.controller.contract.dto.ContractDetailResponse;
import com.erp.controller.production.dto.WarehousingDetailResponse;
import com.erp.entity.settlement.Settlement;
import com.erp.entity.settlement.SettlementReference;
import com.erp.entity.settlement.SettlementWasteDetail;
import com.erp.entity.settlement.SettlementWasteInfo;
import com.erp.entity.contract.ContractWasteItem;
import com.erp.entity.contract.Contract;
import com.erp.mapper.finance.SettlementMapper;
import com.erp.mapper.finance.SettlementReferenceMapper;
import com.erp.mapper.finance.SettlementWasteDetailMapper;
import com.erp.mapper.contract.ContractMapper;
import com.erp.mapper.contract.ContractWasteItemMapper;
import com.erp.mapper.contract.OutOfScopeServiceMapper;
import com.erp.entity.contract.OutOfScopeService;
import com.erp.entity.production.Warehousing;
import com.erp.service.settlement.SettlementService;
import com.erp.service.finance.FinanceService;
import com.erp.service.oa.OaApprovalRecordService;
import com.erp.service.system.ILogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.HttpServletRequest;

import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.entity.system.SysConfig;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.mapper.oa.OaApprovalRecordMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.system.SysConfigService;

/**
 * 结算单服务实现
 * @author ERP System
 * @since 2025-01-01
 */
@Slf4j
@Service
public class SettlementServiceImpl extends ServiceImpl<SettlementMapper, Settlement> implements SettlementService {

    /**
     * 允许更新的字段白名单
     */
    private static final Set<String> ALLOWED_UPDATE_FIELDS;
    static {
        ALLOWED_UPDATE_FIELDS = new HashSet<>();
        ALLOWED_UPDATE_FIELDS.add("status");
        ALLOWED_UPDATE_FIELDS.add("sourceType");
        ALLOWED_UPDATE_FIELDS.add("receivedAmount");
        ALLOWED_UPDATE_FIELDS.add("remark");
        ALLOWED_UPDATE_FIELDS.add("isLocked");
    }

    @Autowired
    private SettlementMapper settlementMapper;

    @Autowired
    private SettlementWasteDetailMapper settlementWasteDetailMapper;

    @Autowired
    private com.erp.mapper.settlement.SettlementWasteInfoMapper settlementWasteInfoMapper;

    
    @Autowired
    private OutOfScopeServiceMapper outOfScopeServiceMapper;

    @Autowired
    private SettlementReferenceMapper settlementReferenceMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ContractWasteItemMapper contractWasteItemMapper;

    @Autowired
    private com.erp.mapper.contract.ContractItemMapper contractItemMapper;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private com.erp.service.contract.ContractService contractService;

    @Autowired
    private com.erp.service.production.WarehousingService warehousingService;

    @Autowired
    private com.erp.mapper.production.WarehousingMapper warehousingMapper;

    @Autowired
    private com.erp.mapper.production.WarehousingWasteItemMapper warehousingWasteItemMapper;

    @Autowired
    private com.erp.mapper.production.StockMapper stockMapper;

    @Autowired
    private com.erp.mapper.system.HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private com.erp.mapper.system.HazardousWasteCategoryMapper hazardousWasteCategoryMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;


    @Autowired
    private AuthService authService;

    @Autowired
    private OaApprovalRecordService oaApprovalRecordService;

    @Autowired
    private OaApprovalRecordMapper oaApprovalRecordMapper;

    @Autowired
    private com.erp.mapper.system.EmployeeMapper employeeMapper;

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 危险废物结算主页面编码（用于字段级权限 & 数据范围控制）
     */
    private static final String SETTLEMENT_PAGE_CODE = "合同结算:危险废物结算:页面";

    /**
     * 危险废物结算-收款结算页面编码（用于字段级权限 & 数据范围控制）
     */
    private static final String RECEIVABLE_SETTLEMENT_PAGE_CODE = "合同结算:危险废物结算-收款结算:页面";

    /**
     * 危险废物结算-付款结算页面编码（用于字段级权限 & 数据范围控制）
     */
    private static final String PAYABLE_SETTLEMENT_PAGE_CODE = "合同结算:危险废物结算-付款结算:页面";

    private static final String OA_SOURCE_TABLE = "SETTLEMENT";

    private static final String OA_SOURCE_TABLE_NAME = "结算单";

    /**
     * 创建或重置结算单OA审核记录
     */
    private void submitSettlementApproval(Settlement settlement) {
        if (settlement == null || settlement.getSettlementId() == null) {
            return;
        }

        Integer submitterId = SecurityUtil.getCurrentUserId();
        String submitterName = SecurityUtil.getEmployeeName();
        if (submitterId == null) {
            throw new BusinessException("无法获取当前提交人信息");
        }

        OaApprovalRecord latestRecord = oaApprovalRecordService.findLatestBySource(OA_SOURCE_TABLE, settlement.getSettlementId().intValue());
        if (latestRecord == null) {
            oaApprovalRecordService.submit(
                    OA_SOURCE_TABLE,
                    settlement.getSettlementId().intValue(),
                    OA_SOURCE_TABLE_NAME,
                    settlement.getSettlementCode(),
                    "结算单审核：" + settlement.getSettlementCode(),
                    submitterId,
                    submitterName
            );
            return;
        }

        if ("已驳回".equals(latestRecord.getApprovalStatus())) {
            oaApprovalRecordService.reactivateRejectedRecord(
                    OA_SOURCE_TABLE,
                    settlement.getSettlementId().intValue(),
                    submitterId,
                    submitterName
            );
            return;
        }

        latestRecord.setApprovalStatus("待审核");
        latestRecord.setApprovalCount((latestRecord.getApprovalCount() == null ? 0 : latestRecord.getApprovalCount()) + 1);
        latestRecord.setSubmitTime(LocalDateTime.now());
        latestRecord.setSubmitterId(submitterId);
        latestRecord.setSubmitterName(submitterName);
        latestRecord.setApproverId(null);
        latestRecord.setApproverName(null);
        latestRecord.setApprovalTime(null);
        latestRecord.setDeleted(0);
        oaApprovalRecordMapper.updateById(latestRecord);
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


    @Override
    public AccumulatedQuantityDTO getAccumulatedQuantity(String contractCode, String wasteCategory) {
        log.info("查询累积已结算量，contractCode={}, wasteCategory={}", contractCode, wasteCategory);

        BigDecimal accumulatedQuantity = settlementMapper.selectAccumulatedSettledQuantity(contractCode, wasteCategory);
        BigDecimal contractPlanTotal = settlementMapper.selectContractPlanTotal(contractCode, wasteCategory);

        return new AccumulatedQuantityDTO(accumulatedQuantity, contractPlanTotal != null ? contractPlanTotal : BigDecimal.ZERO);
    }

    @Override
    public List<SettlementExportSummaryDTO> getSettlementExportSummary(Integer creatorId) {
        log.info("查询结算汇总导出数据（收款结算与付款结算，汇总字段），creatorId={}", creatorId);
        return settlementMapper.selectSettlementExportSummary(creatorId);
    }

    @Override
    public List<SettlementExportDetailDTO> getSettlementExportDetails() {
        log.info("查询结算明细导出数据（按量结算模式，危废明细）");
        return settlementMapper.selectSettlementExportDetails();
    }

    @Override
    public List<WarehousingWasteDetailVO> getWarehousingWasteDetailsByCodes(List<String> warehousingCodes) {
        log.info("获取入库单对应的危废明细，warehousingCodes={}", warehousingCodes);

        return settlementWasteDetailMapper.selectWarehousingWasteDetailsByCodes(warehousingCodes);
    }


    @Override
    public SettlementDetailDTO getSettlementDetail(Long settlementId) {
        log.info("获取结算单详情，settlementId={}", settlementId);

        // 查询主表信息
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算单不存在");
        }

        // 构建返回DTO
        SettlementDetailDTO detailDTO = new SettlementDetailDTO();
        detailDTO.setSettlementId(settlement.getSettlementId());
        detailDTO.setSettlementCode(settlement.getSettlementCode());
        detailDTO.setContractCode(settlement.getContractCode());
        detailDTO.setContractId(settlement.getContractId());
        detailDTO.setSettlementType(settlement.getSettlementType());
        detailDTO.setSourceType(settlement.getSourceType());
        detailDTO.setSettlementPeriodStart(settlement.getSettlementPeriodStart());
        detailDTO.setSettlementPeriodEnd(settlement.getSettlementPeriodEnd());
        detailDTO.setTotalAmount(settlement.getSettlementAmount());
        detailDTO.setReceivedAmount(settlement.getReceivedAmount());
        detailDTO.setStatus(settlement.getStatus());
        detailDTO.setCreatorName(settlement.getCreatorName());
        detailDTO.setCreateTime(settlement.getCreateTime());
        detailDTO.setAuditorName(getAuditorNameById(settlement.getAuditorId()));
        detailDTO.setAuditTime(settlement.getAuditTime());
        detailDTO.setAuditOpinion(settlement.getAuditOpinion());
        detailDTO.setRemark(settlement.getRemark());
        detailDTO.setIsLocked(settlement.getIsLocked());

        // 查询辅助计量单位选项
        detailDTO.setAuxUnitOptions(getAuxUnitOptions());

        // 查询关联单号（从 SETTLEMENT_REFERENCE 表）
        detailDTO.setReferenceCodes(getReferenceCodesBySettlementId(settlementId));

        // 查询结算明细（按序号排序）
        QueryWrapper<SettlementWasteDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("结算单编号", settlementId).orderByAsc("序号");
        List<SettlementWasteDetail> details = settlementWasteDetailMapper.selectList(wrapper);

        // 批量查询废物信息
        List<Long> detailIds = details.stream().map(SettlementWasteDetail::getDetailId).collect(Collectors.toList());
        Map<Long, List<SettlementWasteInfo>> wasteInfoMap = new HashMap<>();
        if (!detailIds.isEmpty()) {
            List<SettlementWasteInfo> wasteInfoList = settlementWasteInfoMapper.selectByDetailIds(detailIds);
            wasteInfoMap = wasteInfoList.stream()
                .collect(Collectors.groupingBy(SettlementWasteInfo::getDetailId));
        }

        List<WasteSettlementDetailDTO> quantityItems = new ArrayList<>();
        List<WasteSettlementDetailDTO> lumpSumItems = new ArrayList<>();

        for (SettlementWasteDetail d : details) {
            WasteSettlementDetailDTO dto = new WasteSettlementDetailDTO();
            dto.setDetailId(d.getDetailId());
            dto.setSequence(d.getSequence());
            dto.setReceiveDate(d.getReceiveDate());
            dto.setEnableAuxiliaryAccounting(d.getEnableAuxiliaryAccounting());
            dto.setBasicSettlementQuantity(d.getBasicSettlementQuantity());
            dto.setBasicUnit(d.getBasicUnit());
            dto.setAuxiliarySettlementQuantity(d.getAuxiliarySettlementQuantity());
            dto.setAuxiliaryUnit(d.getAuxiliaryUnit());
            dto.setUnitPrice(d.getUnitPrice());
            dto.setSaveUnitPrice(d.getSaveUnitPrice());
            dto.setAuxiliaryUnitPrice(d.getAuxiliaryUnitPrice());
            dto.setAmount(d.getAmount());
            dto.setContractItemId(d.getContractItemId() != null ? Long.valueOf(d.getContractItemId()) : null);
            dto.setContractPlanTotal(d.getContractPlanTotal());
            dto.setSettledBasicQuantity(d.getSettledBasicQuantity());
            dto.setSettledAuxiliaryQuantity(d.getSettledAuxiliaryQuantity());
            dto.setCurrentAccumulatedQuantity(d.getCurrentAccumulatedQuantity());
            dto.setExcessQuantity(d.getExcessQuantity());
            dto.setExcessUnitPrice(d.getExcessUnitPrice());
            dto.setExcessAmount(d.getExcessAmount());
            dto.setRemark(d.getRemark());
            dto.setIsLocked(d.getIsLocked());

            // 补充关联来源信息
            dto.setSourceType(d.getSourceType());
            dto.setSourceCode(d.getSourceCode());
            dto.setGdManifestCode(d.getGdManifestCode());

            // 补充辅助核算相关字段
            dto.setAuxiliaryContractPlanTotal(d.getAuxiliaryContractPlanTotal());

            // 从废物信息表获取废物数据（支持多条危废信息）
            List<SettlementWasteInfo> wasteInfoList = wasteInfoMap.get(d.getDetailId());
            if (wasteInfoList != null && !wasteInfoList.isEmpty()) {
                List<SettlementWasteInfoDTO> wasteInfoDTOList = wasteInfoList.stream().map(wasteInfo -> {
                    SettlementWasteInfoDTO wasteInfoDTO = new SettlementWasteInfoDTO();
                    wasteInfoDTO.setWasteInfoId(wasteInfo.getWasteInfoId());
                    wasteInfoDTO.setDetailId(wasteInfo.getDetailId());
                    wasteInfoDTO.setWasteCategory(wasteInfo.getWasteCategory());
                    wasteInfoDTO.setWasteCode(wasteInfo.getWasteCode());
                    wasteInfoDTO.setWasteName(wasteInfo.getWasteName());
                    wasteInfoDTO.setSourceWasteItemId(wasteInfo.getSourceWasteItemId());
                    return wasteInfoDTO;
                }).collect(Collectors.toList());
                dto.setWasteInfoList(wasteInfoDTOList);
            }

            // 设置付款方：直接使用数据库存储的 RECEIVABLE/PAYABLE
            dto.setPayer(d.getPayer());

            // 判断结算模式（通过合同计划总量判断）
            boolean isLumpSum = d.getContractPlanTotal() != null && d.getContractPlanTotal().compareTo(BigDecimal.ZERO) > 0;
            if (isLumpSum) {
                lumpSumItems.add(dto);
            } else {
                quantityItems.add(dto);
            }
        }


        // 将明细按结算类型写入DTO（前端按settlementType区分甲/乙）
        if ("RECEIVABLE".equalsIgnoreCase(settlement.getSettlementType())) {
            detailDTO.setQuantityAItems(quantityItems);
            detailDTO.setLumpSumAItems(lumpSumItems);
            detailDTO.setQuantityBItems(new ArrayList<>());
            detailDTO.setLumpSumBItems(new ArrayList<>());
        } else {
            detailDTO.setQuantityBItems(quantityItems);
            detailDTO.setLumpSumBItems(lumpSumItems);
            detailDTO.setQuantityAItems(new ArrayList<>());
            detailDTO.setLumpSumAItems(new ArrayList<>());
        }

        // 查询价外服务表（OUT_OF_SCOPE_SERVICE）关联到该结算单
        // 使用 QueryWrapper 替代自定义 XML 映射方法，避免找不到绑定语句的问题
        QueryWrapper<OutOfScopeService> svcWrapper = new QueryWrapper<>();
        svcWrapper.eq("关联业务类型", "SETTLEMENT");
        svcWrapper.eq("关联业务单号", settlement.getSettlementId().intValue());
        List<OutOfScopeService> services = outOfScopeServiceMapper.selectList(svcWrapper);
        List<ServiceItemDTO> serviceDTOs = new ArrayList<>();
        if (services != null) {
            for (OutOfScopeService s : services) {
                ServiceItemDTO sd = new ServiceItemDTO();
                if (s.getCreatedAt() != null) {
                    sd.setReceiveDate(s.getCreatedAt().toLocalDate());
                }
                sd.setOutOfScopeServiceId(s.getOutOfScopeServiceId());
                sd.setProject(s.getProject());
                sd.setSpec(s.getSpec());
                sd.setBasicSettlementQuantity(s.getPlannedQuantity());
                sd.setBasicUnit(s.getUnit());
                sd.setUnitPrice(s.getContractUnitPrice());
                // 优先使用已结算金额，否则使用计划数量 * 单价
                if (s.getSettledAmount() != null) {
                    sd.setAmount(s.getSettledAmount());
                } else if (s.getPlannedQuantity() != null && s.getContractUnitPrice() != null) {
                    sd.setAmount(s.getPlannedQuantity().multiply(s.getContractUnitPrice()));
                }
                sd.setRemark(s.getRemark());
                serviceDTOs.add(sd);
            }
        }


        detailDTO.setServiceItems(serviceDTOs);

        // 补充关联数据查询
        try {
            // 根据sourceType决定入库单数据填充策略
            if ("warehousing".equals(settlement.getSourceType())) {
                // sourceType为warehousing时，从已处理的明细中提取入库单号并查询详情
                Set<String> warehousingCodeSet = details.stream()
                    .filter(d -> "warehousing".equals(d.getSourceType()) && StringUtils.hasText(d.getSourceCode()))
                    .map(SettlementWasteDetail::getSourceCode)
                    .collect(Collectors.toSet());

                if (!warehousingCodeSet.isEmpty()) {
                    List<String> warehousingCodes = new ArrayList<>(warehousingCodeSet);
                    List<AvailableWarehousingVO> warehousingDetails = financeService.getWarehousingDetailsByCodes(warehousingCodes);
                        detailDTO.setWarehousingCodes(warehousingDetails);
                } else {
                    detailDTO.setWarehousingCodes(new ArrayList<>());
                }
            } else {
                // sourceType为contract或其他情况，不需要查询入库单列表
                detailDTO.setWarehousingCodes(new ArrayList<>());
            }

            detailDTO.setTransportCodes(getRelatedTransportCodes(settlementId));
            detailDTO.setContractInfo(getContractBasicInfo(settlement.getContractCode()));

        } catch (Exception e) {
            log.warn("查询结算单关联数据失败，settlementId={}，error={}", settlementId, e.getMessage());
            // 不影响核心数据返回
        }

        return detailDTO;
    }

    @Override
    public SettlementDetailDTO getSettlementDetailWithRelatedData(Long settlementId) {
        return getSettlementDetail(settlementId);
    }


    /**
     * 获取结算单关联的运输记录编码
     */
    private List<String> getRelatedTransportCodes(Long settlementId) {
        try {
            log.debug("查询结算单关联运输记录，settlementId={}", settlementId);

            // 从SettlementReference表查询实际关联的运输记录
            List<String> codes = settlementReferenceMapper.selectTransportCodesBySettlementId(settlementId);

            if (codes != null && !codes.isEmpty()) {
                log.debug("找到关联运输记录 {} 个，settlementId={}", codes.size(), settlementId);
                return codes;
            }

            // 如果关联表中没有记录，尝试基于业务规则推导（向后兼容）
            log.debug("关联表中无运输记录，尝试业务规则推导，settlementId={}", settlementId);
            return inferTransportCodesFromDetails(settlementId);

        } catch (Exception e) {
            log.warn("查询结算单关联运输记录失败，settlementId={}，error={}", settlementId, e.getMessage());
            // 返回空列表实现优雅降级
            return new ArrayList<>();
        }
    }

    /**
     * 从明细数据推导运输记录编码（向后兼容处理）
     */
    private List<String> inferTransportCodesFromDetails(Long settlementId) {
        try {
            // 查询结算明细中的运输单号
            QueryWrapper<SettlementWasteDetail> wrapper = new QueryWrapper<>();
            wrapper.eq("结算单编号", settlementId)
                   .isNotNull("关联来源单号")
                   .eq("关联来源类型", "TRANSPORT")
                   .groupBy("关联来源单号");

            List<SettlementWasteDetail> details = settlementWasteDetailMapper.selectList(wrapper);
            List<String> codes = details.stream()
                .map(SettlementWasteDetail::getSourceCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

            log.debug("从明细数据推导出运输记录 {} 个，settlementId={}", codes.size(), settlementId);
            return codes;

        } catch (Exception e) {
            log.warn("从明细数据推导运输记录失败，settlementId={}", settlementId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取合同基本信息
     */
    private ContractBasicInfoDTO getContractBasicInfo(String contractCode) {
        if (!StringUtils.hasText(contractCode)) {
            return null;
        }

        try {
            log.debug("查询合同基本信息，contractCode={}", contractCode);

            // 调用ContractMapper查询合同基本信息
            Contract contract = contractMapper.selectBasicInfoByContractCode(contractCode);
            if (contract == null) {
                log.warn("未找到合同信息，contractCode={}", contractCode);
                return null;
            }

            ContractBasicInfoDTO contractInfo = new ContractBasicInfoDTO();
            contractInfo.setContractId(contract.getContractId().longValue());
            contractInfo.setContractCode(contract.getContractNo());

            // 直接从数据库字段获取客户名称（甲方名称）
            if (StringUtils.hasText(contract.getPartyAName())) {
                contractInfo.setCustomerName(contract.getPartyAName());
            }

            // 设置签订日期
            if (contract.getSignTime() != null) {
                contractInfo.setSignDate(contract.getSignTime());
            }

            return contractInfo;
        } catch (Exception e) {
            log.warn("查询合同信息失败，contractCode={}", contractCode, e);
            return null;
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> auditSettlement(Long settlementId, SettlementAuditDTO auditDTO) {
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            return Result.error("结算单不存在");
        }

        if (!"审核中".equals(settlement.getStatus())) {
            return Result.error("结算单状态不允许审核");
        }

        // 获取当前用户信息
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        String currentUserName = SecurityUtil.getEmployeeName();
        if (currentUserId == null) {
            return Result.error("无法获取当前用户信息");
        }

        // 查询审核人姓名（如果当前用户是员工）
        String approverName = currentUserName;
        try {
            com.erp.entity.system.Employee currentEmployee = employeeMapper.selectById(currentUserId);
            if (currentEmployee != null && currentEmployee.getEmployeeName() != null) {
                approverName = currentEmployee.getEmployeeName();
            }
        } catch (Exception e) {
            log.warn("查询当前员工信息失败，使用默认姓名：userId={}", currentUserId, e);
        }

        // 确定审核结果（通过/驳回）
        String auditStatus;
        if ("approved".equals(auditDTO.getAuditResult()) || "PASSED".equals(auditDTO.getAuditResult()) || "通过".equals(auditDTO.getAuditResult())) {
            auditStatus = "已审核";
            // 审核通过时验证结算单参数
            validateSettlementForAudit(settlement);
        } else {
            auditStatus = "已驳回";
        }

        // 更新结算单审核信息
        settlement.setStatus(auditStatus);
        settlement.setAuditorId(currentUserId);
        settlement.setAuditTime(LocalDateTime.now());
        settlement.setAuditOpinion(auditDTO.getAuditOpinion());
        settlement.setUpdateTime(LocalDateTime.now());

        // 执行更新并检查乐观锁
        int rows = settlementMapper.updateById(settlement);
        if (rows == 0) {
            throw new BusinessException("审核结算单失败：记录已被其他用户修改，请刷新后重试");
        }

        // 同步更新 OA 审核记录表
        try {
            // 查询结算单对应的待审核OA记录
            OaApprovalRecord oaRecord = oaApprovalRecordService.findPendingBySource(OA_SOURCE_TABLE, settlement.getSettlementId().intValue());

            if (oaRecord != null) {
                // 将结算单审核结果映射为OA审核结果（已审核->通过，已驳回->驳回）
                String oaResult = "已审核".equals(auditStatus) ? "通过" : "驳回";

                // 调用OA审核服务更新记录
                oaApprovalRecordService.approve(
                    oaRecord.getApprovalRecordId(),
                    OA_SOURCE_TABLE,
                    settlement.getSettlementId().intValue(),
                    oaResult,
                    auditDTO.getAuditOpinion(),
                    currentUserId,
                    approverName
                );

                log.info("同步更新OA审核记录成功：settlementId={}, oaRecordId={}, oaResult={}",
                    settlementId, oaRecord.getApprovalRecordId(), oaResult);
            } else {
                log.warn("未找到结算单对应的待审核OA记录：settlementId={}", settlementId);
            }
        } catch (Exception e) {
            log.error("同步更新OA审核记录失败：settlementId={}, auditStatus={}", settlementId, auditStatus, e);
            // OA记录更新失败不影响主流程
        }

        // 记录数据变更日志（状态变更：审核中 -> 已审核/已驳回）
        try {
            if (logRecordService != null) {
                Settlement oldSettlement = new Settlement();
                oldSettlement.setStatus("审核中");
                Settlement newSettlement = new Settlement();
                newSettlement.setStatus(auditStatus);
                newSettlement.setAuditOpinion(auditDTO.getAuditOpinion());
                logRecordService.recordDataChangeLog("结算单管理", "SETTLEMENT", String.valueOf(settlementId),
                        "审核", String.format("审核结算单：%s，审核意见=%s",
                                "已审核".equals(auditStatus) ? "审核通过" : "审核驳回",
                                auditDTO.getAuditOpinion()),
                        oldSettlement, newSettlement, currentUserId, null, true, null);
            }
        } catch (Exception logEx) {
            log.warn("记录结算单审核数据变更日志失败，settlementId={}", settlementId, logEx);
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateSettlement(Long settlementId, SettlementUpdateDTO updateDTO) {
        log.info("更新结算单，settlementId={}, updateDTO={}", settlementId, updateDTO);

        // 参数校验
        if (settlementId == null || settlementId <= 0) {
            return Result.error("无效的结算单ID");
        }
        if (updateDTO == null) {
            return Result.error("更新数据不能为空");
        }

        // 查询结算单
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            return Result.error("结算单不存在");
        }

        // 保存原始状态，用于判断是否是审核通过
        String originalStatus = settlement.getStatus();

        // 检查结算单状态是否允许更新
        if ("field".equals(updateDTO.getUpdateMode())) {
            // field模式下，检查是否是允许的状态流转
        if ("已审核".equals(settlement.getStatus()) || "已完结".equals(settlement.getStatus())) {
                boolean isAllowedTransition = isAllowedStatusTransition(settlement.getStatus(), updateDTO.getFieldUpdates());
                if (!isAllowedTransition) {
            return Result.error("结算单状态不允许更新");
                }
            }
        } else {
            // 完整修改模式：待审核/审核中状态允许修改
            if (!"待审核".equals(settlement.getStatus()) && !"审核中".equals(settlement.getStatus())) {
                return Result.error("仅待审核或审核中状态允许修改");
            }
        }

        // 根据更新模式处理更新逻辑
        if ("field".equals(updateDTO.getUpdateMode())) {
            // 字段更新模式
            updateSettlementFields(settlement, updateDTO);
        } else {
            // 完整更新或增量更新模式
            updateSettlementCompletely(settlement, updateDTO);
        }

        // 设置更新时间
        settlement.setUpdateTime(LocalDateTime.now());

        // 执行更新
        int updateResult = settlementMapper.updateById(settlement);
        if (updateResult <= 0) {
            return Result.error("更新结算单失败");
        }

        // ✅ 更新成功后检测特殊状态组合：待审核/已驳回提交审核时同步OA记录
        if (("待审核".equals(originalStatus) || "已驳回".equals(originalStatus)) &&
            "审核中".equals(settlement.getStatus())) {
            submitSettlementApproval(settlement);
        }

        // ✅ 更新成功后检测特殊状态组合：warehousing类型且状态为已审核
        if ("warehousing".equals(settlement.getSourceType()) &&
            "已审核".equals(settlement.getStatus())) {
            updateRelatedWarehousingStatusToSettled(settlementId, settlement);
        }

        // 记录数据变更日志
        try {
            if (logRecordService != null) {
                Settlement oldSettlement = new Settlement();
                oldSettlement.setStatus(settlement.getStatus());
                Settlement newSettlement = new Settlement();
                newSettlement.setStatus(settlement.getStatus());
                // 复制关键字段用于日志对比
                if (updateDTO.getFieldUpdates() != null) {
                    newSettlement.setRemark((String) updateDTO.getFieldUpdates().get("remark"));
                }
                logRecordService.recordDataChangeLog("结算单管理", "SETTLEMENT", String.valueOf(settlementId),
                        "更新", "更新结算单", oldSettlement, newSettlement, SecurityUtil.getCurrentUserId(), null, true, null);
            }
        } catch (Exception logEx) {
            log.warn("记录结算单数据变更日志失败，settlementId={}", settlementId, logEx);
        }

        log.info("结算单更新成功，settlementId={}", settlementId);
        return Result.success("更新结算单成功", null);
    }

    /**
     * 检查是否是允许的状态流转
     * @param currentStatus 当前状态
     * @param fieldUpdates 要更新的字段映射
     * @return true表示允许，false表示不允许
     */
    private boolean isAllowedStatusTransition(String currentStatus, Map<String, Object> fieldUpdates) {
        if (fieldUpdates == null || !fieldUpdates.containsKey("status")) {
            return false;
        }

        String newStatus = (String) fieldUpdates.get("status");

        // 允许的状态流转：待审核/已驳回 → 审核中
        if (Arrays.asList("待审核", "已驳回").contains(currentStatus) && "审核中".equals(newStatus)) {
            return true;
        }
        // 允许的状态流转：已审核 → 已结算
        if ("已审核".equals(currentStatus) && "已结算".equals(newStatus)) {
            return true;
        }

        // 可以在这里添加更多允许的状态流转
        // 例如：已结算 → 已收款 等

        return false;
    }

    /**
     * 字段更新模式 - 只更新指定的字段
     */
    private void updateSettlementFields(Settlement settlement, SettlementUpdateDTO updateDTO) {
        Map<String, Object> fieldUpdates = updateDTO.getFieldUpdates();

        // field模式下必须提供fieldUpdates
        if (fieldUpdates == null || fieldUpdates.isEmpty()) {
            throw new BusinessException("field模式下必须提供fieldUpdates参数，指定要更新的字段");
        }

        // 遍历并更新字段
        for (Map.Entry<String, Object> entry : fieldUpdates.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            if (!ALLOWED_UPDATE_FIELDS.contains(fieldName)) {
                log.warn("尝试更新不支持的字段: {}", fieldName);
                throw new BusinessException("不支持更新的字段：" + fieldName);
            }

            try {
                // 使用反射动态调用setter方法
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                java.lang.reflect.Method setter = Settlement.class.getMethod(setterName, getFieldType(fieldName));
                setter.invoke(settlement, convertValue(fieldValue, getFieldType(fieldName)));
            } catch (Exception e) {
                log.error("更新字段失败: fieldName={}, fieldValue={}, error={}",
                         fieldName, fieldValue, e.getMessage());
                throw new BusinessException("字段更新失败：" + fieldName + " - " + e.getMessage());
            }
        }
    }

    /**
     * 获取字段对应的Java类型
     */
    private Class<?> getFieldType(String fieldName) {
        switch (fieldName) {
            case "contractCode":
            case "sourceType":
            case "status":
            case "remark":
                return String.class;
            case "receivedAmount":
                return BigDecimal.class;
            case "isLocked":
                return Boolean.class;
            default:
                throw new BusinessException("未知字段类型：" + fieldName);
        }
    }

    /**
     * 转换字段值为对应类型
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal) {
                return value;
            } else {
                return new BigDecimal(value.toString());
            }
        } else if (targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else {
            return value;
        }
    }

    /**
     * 增量更新模式 - 更新所有可修改字段，支持明细数据增删改
     */
    private void updateSettlementCompletely(Settlement settlement, SettlementUpdateDTO updateDTO) {
        log.info("开始增量更新结算单，settlementId={}", settlement.getSettlementId());

        // 1. 更新Settlement主表基本信息
        updateSettlementBasicFields(settlement, updateDTO);

        // 2. 增量更新明细数据
        updateSettlementWasteDetails(settlement.getSettlementId(), updateDTO);

        // 3. 增量更新价外服务
        updateSettlementServices(settlement.getSettlementId(), updateDTO);

        // 4. 重新计算总金额
        recalculateSettlementAmount(settlement);

        log.info("增量更新结算单完成，settlementId={}", settlement.getSettlementId());
    }

    /**
     * 更新Settlement主表基本字段
     */
    private void updateSettlementBasicFields(Settlement settlement, SettlementUpdateDTO updateDTO) {
        boolean hasUpdates = false;

        // 更新合同号
        if (updateDTO.getContractCode() != null && !updateDTO.getContractCode().equals(settlement.getContractCode())) {
            settlement.setContractCode(updateDTO.getContractCode());
            hasUpdates = true;
        }

        // 更新来源类型
        if (updateDTO.getReferenceType() != null && !updateDTO.getReferenceType().equals(settlement.getSourceType())) {
            settlement.setSourceType(updateDTO.getReferenceType());
            hasUpdates = true;
        }

        // 更新备注
        if (updateDTO.getRemark() != null && !updateDTO.getRemark().equals(settlement.getRemark())) {
            settlement.setRemark(updateDTO.getRemark());
            hasUpdates = true;
        }

         // 更新状态
        if (updateDTO.getStatus() != null && !updateDTO.getStatus().equals(settlement.getStatus())) {
            settlement.setStatus(updateDTO.getStatus());
            hasUpdates = true;
        }

        // 更新结算周期
        if (updateDTO.getSettlementPeriod() != null && updateDTO.getSettlementPeriod().length >= 2) {
            try {
                LocalDateTime newStart;
                LocalDateTime newEnd;

                String startPeriodStr = updateDTO.getSettlementPeriod()[0];
                String endPeriodStr = updateDTO.getSettlementPeriod()[1];

                // 判断输入格式：如果是"yyyy-MM"格式，转换为月份的开始和结束
                if (startPeriodStr.matches("\\d{4}-\\d{2}")) {
                    // 月份格式，如"2026-01"
                    LocalDate startDate = LocalDate.parse(startPeriodStr + "-01",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    LocalDate endDate = LocalDate.parse(endPeriodStr + "-01",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")).plusMonths(1).minusDays(1);

                    newStart = startDate.atStartOfDay();
                    newEnd = endDate.atTime(23, 59, 59);
                } else {
                    // 完整日期时间格式，如"2026-01-01 00:00:00"
                    newStart = LocalDateTime.parse(startPeriodStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    newEnd = LocalDateTime.parse(endPeriodStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }

                if (!newStart.equals(settlement.getSettlementPeriodStart()) ||
                    !newEnd.equals(settlement.getSettlementPeriodEnd())) {
                    settlement.setSettlementPeriodStart(newStart);
                    settlement.setSettlementPeriodEnd(newEnd);
                    hasUpdates = true;
                }
            } catch (Exception e) {
                log.warn("解析结算周期失败：{}，错误：{}", updateDTO.getSettlementPeriod(), e.getMessage());
            }
        }

        if (hasUpdates) {
            log.debug("Settlement主表字段已更新，settlementId={}", settlement.getSettlementId());
        }
    }

    /**
     * 增量更新结算明细数据
     */
    private void updateSettlementWasteDetails(Long settlementId, SettlementUpdateDTO updateDTO) {
        // 处理删除操作
        if (updateDTO.getDeleteDetailIds() != null && !updateDTO.getDeleteDetailIds().isEmpty()) {
            for (Long detailId : updateDTO.getDeleteDetailIds()) {
                deleteWasteDetail(detailId);
            }
            log.debug("删除了{}个废物明细", updateDTO.getDeleteDetailIds().size());
        }

        // 处理按量结算明细
        if (updateDTO.getQuantityItems() != null && !updateDTO.getQuantityItems().isEmpty()) {
            updateWasteDetails(settlementId, updateDTO.getQuantityItems(), "按量结算");
        }

        // 处理总价包干明细
        if (updateDTO.getLumpSumItems() != null && !updateDTO.getLumpSumItems().isEmpty()) {
            updateWasteDetails(settlementId, updateDTO.getLumpSumItems(), "总价包干");
        }
    }

    /**
     * 更新废物明细数据（增量更新）
     */
    private void updateWasteDetails(Long settlementId, List<SettlementWasteDetailDTO> detailDTOs, String settlementMode) {
        log.debug("开始更新{}明细，settlementId={}，明细数量={}", settlementMode, settlementId, detailDTOs.size());

        for (SettlementWasteDetailDTO dto : detailDTOs) {
            String operation = dto.getOperation();

            // 如果operation为空，根据是否有detailId来判断是CREATE还是UPDATE
            if (operation == null || operation.trim().isEmpty()) {
                operation = (dto.getDetailId() != null) ? "UPDATE" : "CREATE";
            } else {
                operation = operation.toUpperCase();
            }

            switch (operation) {
                case "CREATE":
                    // 新增明细
                    createNewWasteDetail(settlementId, dto);
                    break;
                case "UPDATE":
                    // 更新现有明细
                    updateExistingWasteDetail(dto);
                    break;
                case "DELETE":
                    // 删除明细
                    deleteWasteDetail(dto.getDetailId());
                    break;
                default:
                    log.warn("未知的操作类型：{}，跳过处理", operation);
                    break;
            }
        }

        log.debug("{}明细更新完成，settlementId={}", settlementMode, settlementId);
    }

    /**
     * 更新现有废物明细
     * 注意：废物信息（废物类别、废物代码、废物名称）需要通过 updateWasteInfo 方法更新
     */
    private void updateExistingWasteDetail(SettlementWasteDetailDTO dto) {
        SettlementWasteDetail existing = settlementWasteDetailMapper.selectById(dto.getDetailId());
        if (existing != null) {
            // 只更新允许修改的字段
            if (dto.getRemark() != null) {
                existing.setRemark(dto.getRemark());
            }

            if (dto.getGdManifestCode() != null) {
                existing.setGdManifestCode(dto.getGdManifestCode());
            }
            if (dto.getBasicSettlementQuantity() != null) {
                existing.setBasicSettlementQuantity(dto.getBasicSettlementQuantity());
            }
            if (dto.getAuxiliarySettlementQuantity() != null) {
                existing.setAuxiliarySettlementQuantity(dto.getAuxiliarySettlementQuantity());
            }

            if (dto.getSettledBasicQuantity() != null) {
                existing.setSettledBasicQuantity(dto.getSettledBasicQuantity());
            }
            if (dto.getSettledAuxiliaryQuantity() != null) {
                existing.setSettledAuxiliaryQuantity(dto.getSettledAuxiliaryQuantity());
            }
            if (dto.getCurrentAccumulatedQuantity() != null) {
                existing.setCurrentAccumulatedQuantity(dto.getCurrentAccumulatedQuantity());
            }

            // 更新价格信息
            if (dto.getUnitPrice() != null) {
                existing.setUnitPrice(dto.getUnitPrice());
            }
            if (dto.getSaveUnitPrice() != null) {
                existing.setSaveUnitPrice(dto.getSaveUnitPrice());
            }
            if (dto.getAuxiliaryUnitPrice() != null) {
                existing.setAuxiliaryUnitPrice(dto.getAuxiliaryUnitPrice());
            }
            if (dto.getExcessUnitPrice() != null) {
                existing.setExcessUnitPrice(dto.getExcessUnitPrice());
            }
            if (dto.getExcessAmount() != null) {
                existing.setExcessAmount(dto.getExcessAmount());
            }
            if (dto.getAmount() != null) {
                existing.setAmount(dto.getAmount());
            }

            if (dto.getExcessQuantity() != null) {
                existing.setExcessQuantity(dto.getExcessQuantity());
            }

            // 更新付款方向
            if (dto.getPayer() != null) {
                existing.setPayer(dto.getPayer());
            }

            existing.setUpdateTime(LocalDateTime.now());
            int rows = settlementWasteDetailMapper.updateById(existing);
            if (rows == 0) {
                log.warn("更新废物明细失败（乐观锁冲突），detailId={}", dto.getDetailId());
            }
            log.debug("废物明细更新成功，detailId={}", dto.getDetailId());

            // 更新危废信息（SETTLEMENT_WASTE_INFO）
            if (dto.getWasteInfo() != null) {
                updateWasteInfo(dto.getDetailId(), dto.getWasteInfo());
            }
            // 支持更新多条废物信息
            if (dto.getWasteInfoList() != null && !dto.getWasteInfoList().isEmpty()) {
                for (SettlementWasteInfoDTO wasteInfoDTO : dto.getWasteInfoList()) {
                    updateWasteInfo(dto.getDetailId(), wasteInfoDTO);
                }
            }
        }
    }

    /**
     * 更新危废信息
     */
    private void updateWasteInfo(Long detailId, SettlementWasteInfoDTO wasteInfoDTO) {
        if (detailId == null || wasteInfoDTO == null) {
            return;
        }

        // 查询现有危废信息列表
        List<SettlementWasteInfo> existingList = settlementWasteInfoMapper.selectByDetailId(detailId);
        if (existingList != null && !existingList.isEmpty()) {
            // 取第一条记录更新
            SettlementWasteInfo existing = existingList.get(0);
            if (wasteInfoDTO.getWasteCategory() != null) {
                existing.setWasteCategory(wasteInfoDTO.getWasteCategory());
            }
            if (wasteInfoDTO.getWasteCode() != null) {
                existing.setWasteCode(wasteInfoDTO.getWasteCode());
            }
            if (wasteInfoDTO.getWasteName() != null) {
                existing.setWasteName(wasteInfoDTO.getWasteName());
            }
            if (wasteInfoDTO.getSourceWasteItemId() != null) {
                existing.setSourceWasteItemId(wasteInfoDTO.getSourceWasteItemId());
            }
            existing.setUpdateTime(LocalDateTime.now());
            settlementWasteInfoMapper.updateById(existing);
            log.debug("危废信息更新成功，detailId={}", detailId);
        }
    }

    /**
     * 删除废物明细
     */
    private void deleteWasteDetail(Long detailId) {
        if (detailId == null) {
            log.warn("删除废物明细失败：detailId为空");
            return;
        }

        SettlementWasteDetail existing = settlementWasteDetailMapper.selectById(detailId);
        if (existing != null) {
            // 删除危废信息（通过外键级联删除，或者手动删除）
            settlementWasteInfoMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SettlementWasteInfo>()
                .eq("结算明细编号", detailId));

            int result = settlementWasteDetailMapper.deleteById(detailId);
            if (result > 0) {
                log.debug("废物明细删除成功，detailId={}", detailId);
            } else {
                log.warn("废物明细删除失败，detailId={}", detailId);
            }
        } else {
            log.warn("要删除的废物明细不存在，detailId={}", detailId);
        }
    }

    /**
     * 创建新的废物明细
     * 注意：废物信息（废物类别、废物代码、废物名称）需要单独保存到 SETTLEMENT_WASTE_INFO 表
     */
    private void createNewWasteDetail(Long settlementId, SettlementWasteDetailDTO dto) {
        SettlementWasteDetail detail = new SettlementWasteDetail();
        detail.setSettlementId(settlementId);
        detail.setReceiveDate(dto.getReceiveDate());
        detail.setSourceType(dto.getSourceType());
        detail.setSourceCode(dto.getSourceCode());
        detail.setGdManifestCode(dto.getGdManifestCode());
        detail.setEnableAuxiliaryAccounting(dto.getEnableAuxiliaryAccounting());
        detail.setBasicSettlementQuantity(dto.getBasicSettlementQuantity());
        detail.setBasicUnit(dto.getBasicUnit());
        detail.setUnitPrice(dto.getUnitPrice());
        detail.setAuxiliarySettlementQuantity(dto.getAuxiliarySettlementQuantity());
        detail.setAuxiliaryUnit(dto.getAuxiliaryUnit());
        detail.setRemark(dto.getRemark());

        // 设置其他字段
        if (dto.getContractItemId() != null) {
            detail.setContractItemId(dto.getContractItemId());
        }
        if (dto.getContractPlanTotal() != null) {
            detail.setContractPlanTotal(dto.getContractPlanTotal());
        }
        if (dto.getAuxiliaryContractPlanTotal() != null) {
            detail.setAuxiliaryContractPlanTotal(dto.getAuxiliaryContractPlanTotal());
        }
        if (dto.getUnitPrice() != null) {
            detail.setUnitPrice(dto.getUnitPrice());
        }
        if (dto.getSaveUnitPrice() != null) {
            detail.setSaveUnitPrice(dto.getSaveUnitPrice());
        }
        if (dto.getAuxiliaryUnitPrice() != null) {
            detail.setAuxiliaryUnitPrice(dto.getAuxiliaryUnitPrice());
        }
        if (dto.getExcessUnitPrice() != null) {
            detail.setExcessUnitPrice(dto.getExcessUnitPrice());
        }
        if (dto.getExcessQuantity() != null) {
            detail.setExcessQuantity(dto.getExcessQuantity());
        }
        if (dto.getExcessAmount() != null) {
            detail.setExcessAmount(dto.getExcessAmount());
        }
        if (dto.getAmount() != null) {
            detail.setAmount(dto.getAmount());
        }

        detail.setIsLocked(false);

        settlementWasteDetailMapper.insert(detail);
        Long generatedDetailId = detail.getDetailId();
        log.debug("新增废物明细成功，settlementId={}，detailId={}", settlementId, generatedDetailId);

        // 保存危废信息到 SETTLEMENT_WASTE_INFO 表
        if (dto.getWasteInfo() != null) {
            saveWasteInfo(generatedDetailId, dto.getWasteInfo());
        }
        // 支持保存多条废物信息
        if (dto.getWasteInfoList() != null && !dto.getWasteInfoList().isEmpty()) {
            for (SettlementWasteInfoDTO wasteInfoDTO : dto.getWasteInfoList()) {
                saveWasteInfo(generatedDetailId, wasteInfoDTO);
            }
        }
    }

    /**
     * 增量更新价外服务
     */
    private void updateSettlementServices(Long settlementId, SettlementUpdateDTO updateDTO) {
        if (updateDTO.getServiceItems() != null || updateDTO.getDeleteServiceIds() != null) {
            log.debug("开始增量更新价外服务，settlementId={}，服务数量={}，删除数量={}",
                     settlementId,
                     updateDTO.getServiceItems() != null ? updateDTO.getServiceItems().size() : 0,
                     updateDTO.getDeleteServiceIds() != null ? updateDTO.getDeleteServiceIds().size() : 0);

            // 1. 处理删除操作
            if (updateDTO.getDeleteServiceIds() != null && !updateDTO.getDeleteServiceIds().isEmpty()) {
                for (Integer serviceId : updateDTO.getDeleteServiceIds()) {
                    deleteServiceItem(serviceId);
                }
                log.debug("删除了{}个价外服务项目", updateDTO.getDeleteServiceIds().size());
            }

            // 如果serviceItems为空，删除所有现有的价外服务
            if (updateDTO.getServiceItems() != null && updateDTO.getServiceItems().isEmpty() &&
                (updateDTO.getDeleteServiceIds() == null || updateDTO.getDeleteServiceIds().isEmpty())) {
                outOfScopeServiceMapper.deleteByBusiness("SETTLEMENT", settlementId.intValue());
                log.debug("删除了结算单{}的所有价外服务", settlementId);
            } else if (updateDTO.getServiceItems() != null) {
                // 2. 处理增量更新
                for (ServiceItemDTO serviceItem : updateDTO.getServiceItems()) {
                    String operation = serviceItem.getOperation();

                    // 如果operation为空，根据是否有serviceId来判断是CREATE还是UPDATE
                    if (operation == null || operation.trim().isEmpty()) {
                        operation = (serviceItem.getOutOfScopeServiceId() != null) ? "UPDATE" : "CREATE";
                    } else {
                        operation = operation.toUpperCase();
                    }

                    switch (operation) {
                        case "CREATE":
                            // 新增价外服务
                            createNewServiceItem(settlementId, serviceItem);
                            break;
                        case "UPDATE":
                            // 更新现有价外服务
                            updateExistingServiceItem(serviceItem);
                            break;
                        case "DELETE":
                            // 删除价外服务
                            deleteServiceItem(serviceItem.getOutOfScopeServiceId());
                            break;
                        default:
                            log.warn("未知的服务操作类型：{}，跳过处理", operation);
                            break;
                    }
                }
            }

            log.debug("价外服务增量更新完成，settlementId={}", settlementId);
        }
    }

    /**
     * 创建新的价外服务项目
     */
    private void createNewServiceItem(Long settlementId, ServiceItemDTO serviceItem) {
        // 获取当前用户ID
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        OutOfScopeService service = new OutOfScopeService();
        service.setBusinessType("SETTLEMENT");
        service.setBusinessId(settlementId.intValue());
        service.setProject(serviceItem.getProject());
        service.setSpec(serviceItem.getSpec());
        service.setUnit(serviceItem.getBasicUnit());
        service.setPlannedQuantity(serviceItem.getBasicSettlementQuantity());
        service.setContractUnitPrice(serviceItem.getUnitPrice());
        service.setSettledAmount(serviceItem.getAmount());
        service.setStatus("ACTIVE");
        service.setCreatedAt(now);
        service.setCreatedBy(currentUserId);
        service.setUpdatedAt(now);
        service.setUpdatedBy(currentUserId);
        service.setLocked(false);
        service.setRemark(serviceItem.getRemark());

        outOfScopeServiceMapper.insert(service);
        log.debug("新增价外服务项目成功，settlementId={}", settlementId);
    }

    /**
     * 更新现有的价外服务项目
     */
    private void updateExistingServiceItem(ServiceItemDTO serviceItem) {
        if (serviceItem.getOutOfScopeServiceId() == null) {
            log.warn("更新价外服务项目失败：serviceId为空");
            return;
        }

        OutOfScopeService existing = outOfScopeServiceMapper.selectById(serviceItem.getOutOfScopeServiceId());
        if (existing != null) {
            // 更新允许修改的字段
            if (serviceItem.getProject() != null) {
                existing.setProject(serviceItem.getProject());
            }
            if (serviceItem.getSpec() != null) {
                existing.setSpec(serviceItem.getSpec());
            }
            if (serviceItem.getBasicUnit() != null) {
                existing.setUnit(serviceItem.getBasicUnit());
            }
            if (serviceItem.getBasicSettlementQuantity() != null) {
                existing.setPlannedQuantity(serviceItem.getBasicSettlementQuantity());
            }
            if (serviceItem.getUnitPrice() != null) {
                existing.setContractUnitPrice(serviceItem.getUnitPrice());
            }
            if (serviceItem.getAmount() != null) {
                existing.setSettledAmount(serviceItem.getAmount());
            }
            if (serviceItem.getRemark() != null) {
                existing.setRemark(serviceItem.getRemark());
            }

            existing.setUpdatedAt(LocalDateTime.now());
            existing.setUpdatedBy(SecurityUtil.getCurrentUserId());

            int rows = outOfScopeServiceMapper.updateById(existing);
            if (rows == 0) {
                log.warn("更新价外服务项目失败（乐观锁冲突），serviceId={}", serviceItem.getOutOfScopeServiceId());
            }
            log.debug("价外服务项目更新成功，serviceId={}", serviceItem.getOutOfScopeServiceId());
        } else {
            log.warn("要更新的价外服务项目不存在，serviceId={}", serviceItem.getOutOfScopeServiceId());
        }
    }

    /**
     * 删除价外服务项目
     */
    private void deleteServiceItem(Integer serviceId) {
        if (serviceId == null) {
            log.warn("删除价外服务项目失败：serviceId为空");
            return;
        }

        OutOfScopeService existing = outOfScopeServiceMapper.selectById(serviceId);
        if (existing != null) {
            int result = outOfScopeServiceMapper.deleteById(serviceId);
            if (result > 0) {
                log.debug("价外服务项目删除成功，serviceId={}", serviceId);
            } else {
                log.warn("价外服务项目删除失败，serviceId={}", serviceId);
            }
        } else {
            log.warn("要删除的价外服务项目不存在，serviceId={}", serviceId);
        }
    }

    /**
     * 重新计算结算总金额
     */
    private void recalculateSettlementAmount(Settlement settlement) {
        Long settlementId = settlement.getSettlementId();

        // 计算明细总金额
        BigDecimal wasteDetailAmount = settlementWasteDetailMapper.selectTotalAmountBySettlementId(settlementId);
        if (wasteDetailAmount == null) {
            wasteDetailAmount = BigDecimal.ZERO;
        }

        // 计算价外服务总金额
        BigDecimal serviceAmount = outOfScopeServiceMapper.selectTotalAmountByBusiness("SETTLEMENT", settlementId.intValue());
        if (serviceAmount == null) {
            serviceAmount = BigDecimal.ZERO;
        }

        // 计算总金额
        BigDecimal totalAmount = wasteDetailAmount.add(serviceAmount);
        settlement.setSettlementAmount(totalAmount);

        log.debug("重新计算结算金额，settlementId={}，明细金额={}，服务金额={}，总金额={}",
                 settlementId, wasteDetailAmount, serviceAmount, totalAmount);
    }

    @Override
    public IPage<SettlementPageResponse> getSettlementPage(SettlementPageRequest request) {
        log.info("结算单分页查询（通用），request={}", request);

        // ===== 应用数据范围控制（viewScope） =====
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        Integer creatorIdFilter = null;

        if (!admin) {
            // 前端可通过 fieldPermissionPageCode 显式指定页面编码（危险废物结算主页/收款结算/付款结算Tab独立控制）
            String pageCodeForViewScope = request.getFieldPermissionPageCode();
            if (!org.springframework.util.StringUtils.hasText(pageCodeForViewScope)) {
                // 兼容旧逻辑：按结算类型推导默认页面编码
                if ("RECEIVABLE".equalsIgnoreCase(request.getSettlementType())) {
                    pageCodeForViewScope = RECEIVABLE_SETTLEMENT_PAGE_CODE;
                } else if ("PAYABLE".equalsIgnoreCase(request.getSettlementType())) {
                    pageCodeForViewScope = PAYABLE_SETTLEMENT_PAGE_CODE;
                } else {
                    pageCodeForViewScope = SETTLEMENT_PAGE_CODE;
                }
            }

            EmployeePermission permission = getEmployeePagePermission(currentUserId, pageCodeForViewScope);
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // 仅查看自己创建的结算单
                creatorIdFilter = currentUserId;
            }
        }

        // 创建分页对象
        Page<SettlementPageResponse> page = new Page<>(request.getCurrent(), request.getSize());

        // 构建查询条件
        return settlementMapper.selectSettlementPage(
                page,
                request.getSettlementType(),
                org.springframework.util.StringUtils.hasText(request.getSettlementCode()) ? request.getSettlementCode() : null,
                org.springframework.util.StringUtils.hasText(request.getContractCode()) ? request.getContractCode() : null,
                org.springframework.util.StringUtils.hasText(request.getCustomerName()) ? "%" + request.getCustomerName() + "%" : null,
                org.springframework.util.StringUtils.hasText(request.getStatus()) ? request.getStatus() : null,
                org.springframework.util.StringUtils.hasText(request.getCreatorName()) ? "%" + request.getCreatorName() + "%" : null,
                request.getSettlementStartFrom(),
                request.getSettlementStartTo(),
                request.getSettlementEndFrom(),
                request.getSettlementEndTo(),
                request.getCreateTimeStart(),
                request.getCreateTimeEnd(),
                creatorIdFilter,
                org.springframework.util.StringUtils.hasText(request.getSortField()) ? request.getSortField() : null,
                org.springframework.util.StringUtils.hasText(request.getSortOrder()) ? request.getSortOrder() : null,
                request.getIndependentOnly()
        );
    }

    @Override
    public IPage<ReceivableSettlementPageResponse> getReceivableSettlementPage(SettlementPageRequest request) {
        log.info("收款结算分页查询，request={}", request);
        // 强制限定结算类型为收款
        request.setSettlementType("RECEIVABLE");
        IPage<SettlementPageResponse> sourcePage = getSettlementPage(request);

        Page<ReceivableSettlementPageResponse> targetPage =
                new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());

        List<SettlementPageResponse> records = sourcePage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<ReceivableSettlementPageResponse> targetRecords = new ArrayList<>(records.size());
            for (SettlementPageResponse record : records) {
                targetRecords.add(ReceivableSettlementPageResponse.from(record));
            }
            targetPage.setRecords(targetRecords);
        }
        return targetPage;
    }

    @Override
    public IPage<PayableSettlementPageResponse> getPayableSettlementPage(SettlementPageRequest request) {
        log.info("付款结算分页查询，request={}", request);
        // 强制限定结算类型为付款
        request.setSettlementType("PAYABLE");
        IPage<SettlementPageResponse> sourcePage = getSettlementPage(request);

        Page<PayableSettlementPageResponse> targetPage =
                new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());

        List<SettlementPageResponse> records = sourcePage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<PayableSettlementPageResponse> targetRecords = new ArrayList<>(records.size());
            for (SettlementPageResponse record : records) {
                targetRecords.add(PayableSettlementPageResponse.from(record));
            }
            targetPage.setRecords(targetRecords);
        }
        return targetPage;
    }


    @Override
    public SettledWarehousingQuantityResponseDTO getSettledWarehousingQuantity(SettledWarehousingQuantityRequestDTO requestDTO) {
        log.info("查询已结算入库量，contractCode={}, 原始废物项数量={}",
                 requestDTO.getContractCode(),
                 requestDTO.getWasteItems() != null ? requestDTO.getWasteItems().size() : 0);

        // 预处理：合并同类废物项，避免重复查询
        List<WasteItemRequest> mergedWasteItems = mergeDuplicateWasteItems(requestDTO.getWasteItems());
        log.info("合并后废物项数量={}", mergedWasteItems.size());

        // 批量查询已结算废物明细（SQL层合并）
        List<SettledWasteDetailVO> settledWasteDetails = settlementWasteDetailMapper.selectMergedSettledWasteDetails(
            requestDTO.getContractCode(),
            mergedWasteItems
        );
        log.info("查询到已结算废物明细记录数量={}", settledWasteDetails.size());

        // 转换为响应格式
        List<SettledQuantityItem> resultList = convertToSettledQuantityItems(mergedWasteItems, settledWasteDetails);

        SettledWarehousingQuantityResponseDTO responseDTO = new SettledWarehousingQuantityResponseDTO();
        responseDTO.setData(resultList);

        return responseDTO;
    }

    /**
     * 合并请求中的同类废物项
     * @param wasteItems 原始废物项列表
     * @return 去重后的废物项列表
     */
    private List<WasteItemRequest> mergeDuplicateWasteItems(List<WasteItemRequest> wasteItems) {
        if (wasteItems == null || wasteItems.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用复合键进行分组：wasteCode + wasteCategory + wasteName
        Map<String, WasteItemRequest> mergedItems = new LinkedHashMap<>();

        for (WasteItemRequest item : wasteItems) {
            if (item == null || !StringUtils.hasText(item.getWasteCode())) {
                continue; // 跳过无效项
            }

            // 生成唯一键
            String uniqueKey = generateWasteItemKey(item);

            // 如果已存在，跳过（保留第一个出现的项）
            if (!mergedItems.containsKey(uniqueKey)) {
                mergedItems.put(uniqueKey, item);
            } else {
                log.debug("发现重复废物项，已合并: wasteCode={}, wasteCategory={}, wasteName={}",
                         item.getWasteCode(), item.getWasteCategory(), item.getWasteName());
            }
        }

        return new ArrayList<>(mergedItems.values());
    }

    /**
     * 将查询结果转换为响应格式
     * @param mergedWasteItems 合并后的废物项列表
     * @param settledWasteDetails SQL查询结果
     * @return 响应格式的已结算数量项列表
     */
    private List<SettledQuantityItem> convertToSettledQuantityItems(
            List<WasteItemRequest> mergedWasteItems,
            List<SettledWasteDetailVO> settledWasteDetails) {

        List<SettledQuantityItem> resultList = new ArrayList<>();

        // 创建查询结果的查找映射
        Map<String, SettledWasteDetailVO> settledWasteMap = new HashMap<>();
        for (SettledWasteDetailVO detail : settledWasteDetails) {
            String key = generateWasteItemKey(detail.getWasteCode(),
                                             detail.getWasteCategory(),
                                             detail.getWasteName());
            settledWasteMap.put(key, detail);
        }

        // 为每个请求的废物项构建响应
        for (WasteItemRequest wasteItem : mergedWasteItems) {
            String key = generateWasteItemKey(wasteItem);
            SettledWasteDetailVO settledDetail = settledWasteMap.get(key);

            // 构建响应项
            SettledQuantityItem item = new SettledQuantityItem();
            item.setWasteCategory(wasteItem.getWasteCategory());
            item.setWasteName(wasteItem.getWasteName());
            item.setWasteCode(wasteItem.getWasteCode());
            item.setCurrentRowIndex(wasteItem.getCurrentRowIndex());

            // 设置已结算数量（如果有查询结果）
            if (settledDetail != null) {
                item.setSettledBasicQuantity(settledDetail.getTotalBasicQuantity());
                item.setBasicUnit(settledDetail.getBasicUnit());
                item.setSettledAuxiliaryQuantity(settledDetail.getTotalAuxiliaryQuantity());
                item.setAuxiliaryUnit(settledDetail.getAuxiliaryUnit());
            } else {
                // 无结算记录时，设置默认值
                item.setSettledBasicQuantity(BigDecimal.ZERO);
                item.setBasicUnit("吨");
                item.setSettledAuxiliaryQuantity(BigDecimal.ZERO);
                item.setAuxiliaryUnit(null);
            }

            resultList.add(item);
        }

        return resultList;
    }

    /**
     * 生成废物项的唯一键（重载方法）
     * @param wasteCode 废物代码
     * @param wasteCategory 废物类别
     * @param wasteName 废物名称
     * @return 唯一键字符串
     */
    private String generateWasteItemKey(String wasteCode, String wasteCategory, String wasteName) {
        return String.format("%s|%s|%s",
                            wasteCode,
                            wasteCategory != null ? wasteCategory : "",
                            wasteName != null ? wasteName : "");
    }

    /**
     * 生成废物项的唯一键
     * @param item 废物项
     * @return 唯一键字符串
     */
    private String generateWasteItemKey(WasteItemRequest item) {
        return String.format("%s|%s|%s",
                            item.getWasteCode(),
                            item.getWasteCategory() != null ? item.getWasteCategory() : "",
                            item.getWasteName() != null ? item.getWasteName() : "");
    }

    @Override
    public SettlementStatisticsDTO getSettlementStatistics(String settlementType) {
        log.info("获取结算统计信息，settlementType={}", settlementType);

        SettlementStatisticsDTO statistics = new SettlementStatisticsDTO();

        // 调用Mapper执行SQL聚合查询
        Map<String, Object> result = settlementMapper.selectSettlementStatistics(settlementType);

        if (result != null && !result.isEmpty()) {
            // 设置统计结果，添加null值检查
            Object totalCountObj = result.get("totalCount");
            if (totalCountObj != null) {
                statistics.setTotalCount(Long.valueOf(totalCountObj.toString()));
            } else {
                statistics.setTotalCount(0L);
            }

            // 应收统计数据
            Object receivableTotalObj = result.get("receivableTotal");
            BigDecimal receivableTotal = receivableTotalObj != null ? new BigDecimal(receivableTotalObj.toString()) : BigDecimal.ZERO;

            Object receivablePaidObj = result.get("receivablePaid");
            BigDecimal receivablePaid = receivablePaidObj != null ? new BigDecimal(receivablePaidObj.toString()) : BigDecimal.ZERO;

            Object receivableUnpaidObj = result.get("receivableUnpaid");
            BigDecimal receivableUnpaid = receivableUnpaidObj != null ? new BigDecimal(receivableUnpaidObj.toString()) : BigDecimal.ZERO;

            // 应付统计数据
            Object payableTotalObj = result.get("payableTotal");
            BigDecimal payableTotal = payableTotalObj != null ? new BigDecimal(payableTotalObj.toString()) : BigDecimal.ZERO;

            Object payablePaidObj = result.get("payablePaid");
            BigDecimal payablePaid = payablePaidObj != null ? new BigDecimal(payablePaidObj.toString()) : BigDecimal.ZERO;

            Object payableUnpaidObj = result.get("payableUnpaid");
            BigDecimal payableUnpaid = payableUnpaidObj != null ? new BigDecimal(payableUnpaidObj.toString()) : BigDecimal.ZERO;

            statistics.setReceivableTotal(receivableTotal.setScale(2, RoundingMode.HALF_UP));
            statistics.setReceivablePaid(receivablePaid.setScale(2, RoundingMode.HALF_UP));
            statistics.setReceivableUnpaid(receivableUnpaid.setScale(2, RoundingMode.HALF_UP));
            statistics.setPayableTotal(payableTotal.setScale(2, RoundingMode.HALF_UP));
            statistics.setPayablePaid(payablePaid.setScale(2, RoundingMode.HALF_UP));
            statistics.setPayableUnpaid(payableUnpaid.setScale(2, RoundingMode.HALF_UP));
        }

        return statistics;
    }
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Result<SettlementCreateResultDTO> createSettlement(SettlementCreateDTO createDTO) {
        log.info("开始创建结算单事务，参数：{}", createDTO);

        try {
            // 创建结算单时不再验证参数，审核通过时再验证

            Contract contract = null;
            // 获取合同信息（如果提供了合同号）
            if (createDTO.getContractCode() != null && !createDTO.getContractCode().isEmpty()) {
                contract = getContractByCode(createDTO.getContractCode());
                if (contract == null) {
                    return Result.error("合同不存在：" + createDTO.getContractCode());
                }
            }

            List<SettlementCreateResultDTO.SettlementResult> settlementResults = new ArrayList<>();

            // 检查是否有甲方明细（按量结算甲方 + 总价包干甲方 + 价外服务）
            boolean hasPartyAItems = (createDTO.getQuantityAItems() != null && !createDTO.getQuantityAItems().isEmpty()) ||
                                   (createDTO.getLumpSumAItems() != null && !createDTO.getLumpSumAItems().isEmpty()) ||
                                   (createDTO.getServiceItems() != null && !createDTO.getServiceItems().isEmpty());

            // 检查是否有乙方明细（按量结算乙方 + 总价包干乙方）
            boolean hasPartyBItems = (createDTO.getQuantityBItems() != null && !createDTO.getQuantityBItems().isEmpty()) ||
                                   (createDTO.getLumpSumBItems() != null && !createDTO.getLumpSumBItems().isEmpty());

            // 创建甲方收款结算单
            if (hasPartyAItems) {
                SettlementCreateResultDTO.SettlementResult partyASettlement = createPartyASettlement(createDTO, contract);
                settlementResults.add(partyASettlement);
            }

            // 创建乙方付款结算单
            if (hasPartyBItems) {
                SettlementCreateResultDTO.SettlementResult partyBSettlement = createPartyBSettlement(createDTO, contract);
                settlementResults.add(partyBSettlement);
            }

            // 即使没有明细，也创建一个空的结算单主表
            if (settlementResults.isEmpty()) {
                SettlementCreateResultDTO.SettlementResult emptySettlement = createEmptySettlement(createDTO, contract);
                settlementResults.add(emptySettlement);
            }

            // 只有有 referenceCodes 时才更新关联入库单状态为"结算中"
            if (createDTO.getReferenceCodes() != null && !createDTO.getReferenceCodes().isEmpty()) {
                updateWarehousingStatusAfterSettlementCreation(createDTO);
            }

            SettlementCreateResultDTO result = new SettlementCreateResultDTO();
            result.setSettlements(settlementResults);
            result.setTotalSettlementCount(settlementResults.size());

            // 记录数据变更日志
            try {
                Integer currentUserId = SecurityUtil.getCurrentUserId();
                for (SettlementCreateResultDTO.SettlementResult sr : settlementResults) {
                    if (sr.getSettlementId() != null) {
                        Settlement newSettlement = settlementMapper.selectById(sr.getSettlementId());
                        logRecordService.recordDataChangeLog("结算单管理", "SETTLEMENT",
                                String.valueOf(sr.getSettlementId()),
                                "新增",
                                "创建结算单：结算单号=" + sr.getSettlementCode() + "，结算类型=" + sr.getSettlementType(),
                                null, newSettlement, currentUserId, null, true, null);
                    }
                }
            } catch (Exception logEx) {
                log.warn("记录结算单创建数据变更日志失败", logEx);
            }

            log.info("结算单创建事务成功提交，创建了 {} 张结算单", result.getTotalSettlementCount());

            return Result.success(result);

        } catch (BusinessException e) {
            log.error("创建结算单业务异常，将触发事务回滚，参数：{}，错误：{}", createDTO, e.getMessage());
            // 抛出异常触发事务回滚
            throw e;
        } catch (Exception e) {
            log.error("创建结算单系统异常，将触发事务回滚，参数：{}，错误：{}", createDTO, e.getMessage(), e);
            // 抛出异常触发事务回滚
            throw new BusinessException("创建结算单失败：" + e.getMessage());
        }
    }



    /**
     * 审核通过时验证结算单
     * @param settlement 结算单实体
     */
    private void validateSettlementForAudit(Settlement settlement) {
        if (settlement == null) {
            throw new BusinessException("结算单不存在");
        }
        if (!StringUtils.hasText(settlement.getContractCode())) {
            throw new BusinessException("结算单合同号不能为空");
        }
        if (!StringUtils.hasText(settlement.getSourceType())) {
            throw new BusinessException("结算单引用来源类型不能为空");
        }
    }

    /**
     * 根据合同号获取合同信息
     */
    private Contract getContractByCode(String contractCode) {
        QueryWrapper<Contract> wrapper = new QueryWrapper<>();
        wrapper.eq("合同号", contractCode);
        return contractMapper.selectOne(wrapper);
    }

    /**
     * 创建甲方收款结算单（包含按量结算甲方 + 总价包干甲方 + 价外服务）
     * 注意：此方法在事务范围内执行，所有数据库操作都会在同一个事务中
     */
    private SettlementCreateResultDTO.SettlementResult createPartyASettlement(SettlementCreateDTO createDTO, Contract contract) {


        // 创建主结算单
        Settlement settlement = buildSettlementEntity(createDTO, contract, "RECEIVABLE");
        settlement.setSourceType(createDTO.getReferenceType());
        settlement.setStatus("待审核");

        // 保存主结算单（事务内操作）
        settlementMapper.insert(settlement);

        // 保存关联记录（事务内操作）
        saveSettlementReferences(settlement.getSettlementId(), createDTO);

        // 保存甲方所有明细，并计算总金额（事务内操作）
        BigDecimal totalAmount = savePartyASettlementDetails(settlement.getSettlementId(), createDTO);

        // 更新总金额（事务内操作）
        settlement.setSettlementAmount(totalAmount);
        int rows = settlementMapper.updateById(settlement);
        if (rows == 0) {
            log.warn("更新结算单总金额失败（乐观锁冲突），settlementId={}", settlement.getSettlementId());
        }

        return buildSettlementResult(settlement);
    }

    /**
     * 创建乙方付款结算单（包含按量结算乙方 + 总价包干乙方）
     * 注意：此方法在事务范围内执行，所有数据库操作都会在同一个事务中
     */
    private SettlementCreateResultDTO.SettlementResult createPartyBSettlement(SettlementCreateDTO createDTO, Contract contract) {


        // 创建主结算单
        Settlement settlement = buildSettlementEntity(createDTO, contract, "PAYABLE");
        settlement.setSourceType(createDTO.getReferenceType());
        settlement.setStatus("待审核");

        // 保存主结算单（事务内操作）
        settlementMapper.insert(settlement);

        // 保存关联记录（事务内操作）
        saveSettlementReferences(settlement.getSettlementId(), createDTO);

        // 保存乙方所有明细，并计算总金额（事务内操作）
        BigDecimal totalAmount = savePartyBSettlementDetails(settlement.getSettlementId(), createDTO);

        // 更新总金额（事务内操作）
        settlement.setSettlementAmount(totalAmount);
        int rows = settlementMapper.updateById(settlement);
        if (rows == 0) {
            log.warn("更新结算单总金额失败（乐观锁冲突），settlementId={}", settlement.getSettlementId());
        }

        return buildSettlementResult(settlement);
    }

    /**
     * 创建空的结算单主表（无明细）
     * 注意：此方法在事务范围内执行
     */
    private SettlementCreateResultDTO.SettlementResult createEmptySettlement(SettlementCreateDTO createDTO, Contract contract) {
        // 创建主结算单（默认为甲方收款类型）
        Settlement settlement = buildSettlementEntity(createDTO, contract, "RECEIVABLE");
        settlement.setSourceType(createDTO.getReferenceType());
        settlement.setStatus("待审核");
        settlement.setSettlementAmount(BigDecimal.ZERO);

        // 保存主结算单（事务内操作）
        settlementMapper.insert(settlement);

        // 保存关联记录（事务内操作）
        saveSettlementReferences(settlement.getSettlementId(), createDTO);

        return buildSettlementResult(settlement);
    }

    /**
     * 保存甲方结算明细（按量结算甲方 + 总价包干甲方 + 价外服务）
     */
    private BigDecimal savePartyASettlementDetails(Long settlementId, SettlementCreateDTO createDTO) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 保存按量结算明细（甲方）
        if (createDTO.getQuantityAItems() != null && !createDTO.getQuantityAItems().isEmpty()) {
            totalAmount = totalAmount.add(saveQuantityWasteDetails(settlementId, createDTO.getQuantityAItems()));
        }

        // 保存总价包干明细（甲方）
        if (createDTO.getLumpSumAItems() != null && !createDTO.getLumpSumAItems().isEmpty()) {
            totalAmount = totalAmount.add(saveLumpSumWasteDetails(settlementId, createDTO.getLumpSumAItems()));
        }

        // 保存价外服务明细（价外服务属于甲方）
        if (createDTO.getServiceItems() != null && !createDTO.getServiceItems().isEmpty()) {
            totalAmount = totalAmount.add(saveServiceDetails(settlementId, createDTO.getServiceItems()));
        }

        return totalAmount;
    }

    /**
     * 保存乙方结算明细（按量结算乙方 + 总价包干乙方）
     */
    private BigDecimal savePartyBSettlementDetails(Long settlementId, SettlementCreateDTO createDTO) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 保存按量结算明细（乙方）
        if (createDTO.getQuantityBItems() != null && !createDTO.getQuantityBItems().isEmpty()) {
            totalAmount = totalAmount.add(saveQuantityWasteDetails(settlementId, createDTO.getQuantityBItems()));
        }

        // 保存总价包干明细（乙方）
        if (createDTO.getLumpSumBItems() != null && !createDTO.getLumpSumBItems().isEmpty()) {
            totalAmount = totalAmount.add(saveLumpSumWasteDetails(settlementId, createDTO.getLumpSumBItems()));
        }

        return totalAmount;
    }


    /**
     * 构建结算单实体
     */
    private Settlement buildSettlementEntity(SettlementCreateDTO createDTO, Contract contract, String settlementType) {
        Settlement settlement = new Settlement();
        settlement.setSettlementCode(generateSettlementCode());
        settlement.setContractCode(createDTO.getContractCode());
        if (contract != null) {
            settlement.setContractId(contract.getContractId());
        }
        settlement.setSettlementType(settlementType);
        settlement.setCreatorId(SecurityUtil.getCurrentUserId());
        settlement.setCreatorName(SecurityUtil.getEmployeeName());
  

        // 设置结算周期
        if (createDTO.getSettlementPeriod() != null && createDTO.getSettlementPeriod().length >= 2) {
            String[] period = createDTO.getSettlementPeriod();
            if (period[0] != null && !period[0].isEmpty()) {
                settlement.setSettlementPeriodStart(parsePeriodDate(period[0]));
            }
            if (period[1] != null && !period[1].isEmpty()) {
                settlement.setSettlementPeriodEnd(parsePeriodDate(period[1]));
            }
        }

        settlement.setRemark(createDTO.getRemark());
        settlement.setStatus("待审核");
        settlement.setIsLocked(false);

        return settlement;
    }

    /**
     * 保存结算关联记录
     */
    private void saveSettlementReferences(Long settlementId, SettlementCreateDTO createDTO) {
        if (createDTO.getReferenceCodes() != null && !createDTO.getReferenceCodes().isEmpty()) {
            for (String referenceCode : createDTO.getReferenceCodes()) {
                SettlementReference reference = new SettlementReference();
                reference.setSettlementId(settlementId);
                reference.setSourceType(createDTO.getReferenceType());
                reference.setSourceCode(referenceCode);
                settlementReferenceMapper.insert(reference);
            }
        }
    }

    /**
     * 保存包干子项明细（已简化，不再使用父子结构）
     * 注意：由于 SETTLEMENT_WASTE_DETAIL 表已移除父子层级结构，此方法仅保留用于兼容
     * 实际业务中不再使用包干子项明细
     * 注意：此方法会加入调用者的事务中，确保数据一致性
     */
    private void saveWasteSubDetails(Long parentDetailId, Long settlementId, List<SettlementWasteDetailDTO> subDetails, int parentSequence) {
        if (subDetails == null || subDetails.isEmpty()) {
            return;
        }

        // 子项序号从父项序号+1开始递增
        int subSequence = parentSequence + 1;

        for (SettlementWasteDetailDTO subItem : subDetails) {
            SettlementWasteDetail subDetail = new SettlementWasteDetail();
            subDetail.setSettlementId(settlementId);
            subDetail.setSequence(subSequence++); // 子项序号递增
            subDetail.setReceiveDate(subItem.getReceiveDate());
            subDetail.setSourceType(subItem.getSourceType());
            subDetail.setSourceCode(subItem.getSourceCode());
            subDetail.setGdManifestCode(subItem.getGdManifestCode());
            subDetail.setEnableAuxiliaryAccounting(subItem.getEnableAuxiliaryAccounting());

            // 设置结算相关字段
            subDetail.setBasicSettlementQuantity(subItem.getBasicSettlementQuantity());
            subDetail.setBasicUnit(subItem.getBasicUnit());
            subDetail.setUnitPrice(subItem.getUnitPrice());
            subDetail.setAmount(subItem.getAmount());

            // 事务内数据库插入操作
            settlementWasteDetailMapper.insert(subDetail);

            Long subDetailId = subDetail.getDetailId();
            log.debug("包干子项明细保存成功，子项明细编号：{}，父明细编号：{}，金额：{}", subDetailId, parentDetailId, subDetail.getAmount());

            // 保存危废信息到 SETTLEMENT_WASTE_INFO 表
            if (subItem.getWasteInfo() != null) {
                saveWasteInfo(subDetailId, subItem.getWasteInfo());
            }
        }
    }

    /**
     * 保存按量结算明细
     * 注意：废物类别、废物代码、废物名称已移至 SettlementWasteInfo 表
     */
    private BigDecimal saveQuantityWasteDetails(Long settlementId, List<SettlementWasteDetailDTO> items) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        int sequence = 1;

        for (SettlementWasteDetailDTO item : items) {
            SettlementWasteDetail detail = new SettlementWasteDetail();
            detail.setSettlementId(settlementId);
            detail.setSequence(sequence++);
            detail.setReceiveDate(item.getReceiveDate());
            detail.setSourceType(item.getSourceType());
            detail.setSourceCode(item.getSourceCode());
            detail.setGdManifestCode(item.getGdManifestCode());
            detail.setEnableAuxiliaryAccounting(item.getEnableAuxiliaryAccounting());
            detail.setContractItemId(item.getContractItemId());
            detail.setRemark(item.getRemark());
            detail.setSaveUnitPrice(item.getSaveUnitPrice());
            detail.setAuxiliaryUnitPrice(item.getAuxiliaryUnitPrice());

            // 设置结算相关字段
            detail.setBasicSettlementQuantity(item.getBasicSettlementQuantity());
            detail.setBasicUnit(item.getBasicUnit());
            detail.setAuxiliarySettlementQuantity(item.getAuxiliarySettlementQuantity());
            detail.setAuxiliaryUnit(item.getAuxiliaryUnit());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setAmount(item.getAmount());

            // 设置付款方向
            detail.setPayer(item.getPayer());

            // 计算金额
            if (detail.getAmount() != null) {
                totalAmount = totalAmount.add(detail.getAmount());
            }

            settlementWasteDetailMapper.insert(detail);

            // insert执行后，detailId会被自动设置到detail对象中
            Long generatedDetailId = detail.getDetailId();
            log.debug("按量结算明细保存成功，明细编号：{}，金额：{}", generatedDetailId, detail.getAmount());

            // 保存危废信息到 SETTLEMENT_WASTE_INFO 表
            if (item.getWasteInfo() != null) {
                saveWasteInfo(generatedDetailId, item.getWasteInfo());
            }
            // 支持保存多条废物信息
            if (item.getWasteInfoList() != null && !item.getWasteInfoList().isEmpty()) {
                for (SettlementWasteInfoDTO wasteInfoDTO : item.getWasteInfoList()) {
                    saveWasteInfo(generatedDetailId, wasteInfoDTO);
                }
            }
        }

        return totalAmount;
    }

    /**
     * 保存危废信息到 SETTLEMENT_WASTE_INFO 表
     *
     * @param detailId 结算明细编号
     * @param wasteInfoDTO 危废信息DTO
     */
    private void saveWasteInfo(Long detailId, SettlementWasteInfoDTO wasteInfoDTO) {
        if (wasteInfoDTO == null) {
            return;
        }

        SettlementWasteInfo wasteInfo = new SettlementWasteInfo();
        wasteInfo.setDetailId(detailId);
        wasteInfo.setWasteCategory(wasteInfoDTO.getWasteCategory());
        wasteInfo.setWasteCode(wasteInfoDTO.getWasteCode());
        wasteInfo.setWasteName(wasteInfoDTO.getWasteName());
        wasteInfo.setSourceWasteItemId(wasteInfoDTO.getSourceWasteItemId());

        settlementWasteInfoMapper.insert(wasteInfo);
        log.debug("危废信息保存成功，明细编号：{}，废物代码：{}", detailId, wasteInfoDTO.getWasteCode());
    }

    /**
     * 保存总价包干明细
     * 注意：废物类别、废物代码、废物名称已移至 SettlementWasteInfo 表
     */
    private BigDecimal saveLumpSumWasteDetails(Long settlementId, List<SettlementWasteDetailDTO> items) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        int sequence = 1;

        for (SettlementWasteDetailDTO item : items) {
            SettlementWasteDetail detail = new SettlementWasteDetail();
            detail.setSettlementId(settlementId);
            detail.setSequence(sequence++);
            detail.setReceiveDate(item.getReceiveDate());
            detail.setSourceType(item.getSourceType());
            detail.setSourceCode(item.getSourceCode());
            detail.setGdManifestCode(item.getGdManifestCode());
            detail.setEnableAuxiliaryAccounting(item.getEnableAuxiliaryAccounting());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setSaveUnitPrice(item.getSaveUnitPrice());
            detail.setAuxiliaryUnitPrice(item.getAuxiliaryUnitPrice());
            detail.setBasicSettlementQuantity(item.getBasicSettlementQuantity());
            detail.setBasicUnit(item.getBasicUnit());
            detail.setAuxiliarySettlementQuantity(item.getAuxiliarySettlementQuantity());
            detail.setAuxiliaryUnit(item.getAuxiliaryUnit());
            detail.setContractPlanTotal(item.getContractPlanTotal());
            detail.setAuxiliaryContractPlanTotal(item.getAuxiliaryContractPlanTotal());
            detail.setContractItemId(item.getContractItemId());
            detail.setSettledAuxiliaryQuantity(item.getSettledAuxiliaryQuantity());
            detail.setSettledBasicQuantity(item.getSettledBasicQuantity());
            detail.setCurrentAccumulatedQuantity(item.getCurrentAccumulatedQuantity());
            detail.setExcessQuantity(item.getExcessQuantity());
            detail.setExcessUnitPrice(item.getExcessUnitPrice());
            detail.setExcessAmount(item.getExcessAmount());
            detail.setRemark(item.getRemark());

            // 设置付款方向
            detail.setPayer(item.getPayer());

            // 总价包干的金额直接使用传入的金额
            detail.setAmount(item.getAmount());
            if (detail.getAmount() != null) {
                totalAmount = totalAmount.add(detail.getAmount());
            }

            // 事务内数据库插入操作
            settlementWasteDetailMapper.insert(detail);

            // insert执行后，由于使用IdType.AUTO，detailId会被自动设置到detail对象中
            Long generatedDetailId = detail.getDetailId();

            // 保存危废信息到 SETTLEMENT_WASTE_INFO 表
            if (item.getWasteInfo() != null) {
                saveWasteInfo(generatedDetailId, item.getWasteInfo());
            }
            // 支持保存多条废物信息
            if (item.getWasteInfoList() != null && !item.getWasteInfoList().isEmpty()) {
                for (SettlementWasteInfoDTO wasteInfoDTO : item.getWasteInfoList()) {
                    saveWasteInfo(generatedDetailId, wasteInfoDTO);
                }
            }
        }

        return totalAmount;
    }

    /**
     * 保存价外服务明细
     */
    private BigDecimal saveServiceDetails(Long settlementId, List<ServiceItemDTO> serviceItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 获取当前用户ID
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        for (ServiceItemDTO item : serviceItems) {
            OutOfScopeService service = new OutOfScopeService();
            service.setBusinessType("SETTLEMENT");
            service.setBusinessId(settlementId.intValue());
            service.setProject(item.getProject());
            service.setSpec(item.getSpec());
            service.setUnit(item.getBasicUnit());
            service.setPlannedQuantity(item.getBasicSettlementQuantity());
            service.setContractUnitPrice(item.getUnitPrice());
            service.setSettledAmount(item.getAmount()); // 使用已结算金额
            service.setStatus("ACTIVE");
            service.setCreatedAt(now);
            service.setCreatedBy(currentUserId);
            service.setUpdatedAt(now);
            service.setUpdatedBy(currentUserId);
            service.setLocked(false);
            service.setRemark(item.getRemark());

            if (service.getSettledAmount() != null) {
                totalAmount = totalAmount.add(service.getSettledAmount());
            }

            outOfScopeServiceMapper.insert(service);
            log.debug("价外服务保存成功，业务类型：SETTLEMENT，业务单号：{}，项目：{}", settlementId, item.getProject());
        }

        return totalAmount;
    }

    /**
     * 解析结算周期日期
     */
    private LocalDateTime parsePeriodDate(String periodStr) {
        try {
            // 假设输入格式为 YYYY-MM
            LocalDate date = LocalDate.parse(periodStr + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return date.atStartOfDay();
        } catch (Exception e) {
            log.warn("解析结算周期日期失败：{}", periodStr, e);
            return null;
        }
    }

    /**
     * 构建结算结果
     */
    private SettlementCreateResultDTO.SettlementResult buildSettlementResult(Settlement settlement) {
        SettlementCreateResultDTO.SettlementResult result = new SettlementCreateResultDTO.SettlementResult();
        result.setSettlementId(settlement.getSettlementId());
        result.setSettlementCode(settlement.getSettlementCode());
        result.setTotalAmount(settlement.getSettlementAmount());
        result.setSettlementType(settlement.getSettlementType());
        return result;
    }

    /**
     * 生成结算单单号
     */
    @Override
    public String generateSettlementCode() {
        // 生成格式：JSD-YYYYMMDD-XXXX（4位序列号）
        String dateStr = DateUtil.format(LocalDateTime.now(), "yyyyMMdd");
        String prefix = "JSD-" + dateStr + "-";

        // 查询当天最大的序列号
        QueryWrapper<Settlement> wrapper = new QueryWrapper<>();
        wrapper.like("结算单单号", prefix).orderByDesc("结算单单号").last("limit 1");

        Settlement lastSettlement = settlementMapper.selectOne(wrapper);
        int sequence = 1;

        if (lastSettlement != null && lastSettlement.getSettlementCode() != null) {
            String lastCode = lastSettlement.getSettlementCode();
            if (lastCode.startsWith(prefix)) {
                String seqStr = lastCode.substring(prefix.length());
                try {
                    sequence = Integer.parseInt(seqStr) + 1;
                } catch (NumberFormatException e) {
                    // 解析失败，使用默认序列号
                }
            }
        }

        return String.format("%s%04d", prefix, sequence);
    }

    /**
     * 将入库单编码列表转换为ID列表
     * @param codes 入库单编码列表，不应为null
     * @return 入库单ID列表，过滤掉不存在的编码。返回空列表如果输入为空或所有编码都不存在
     */
    private List<Integer> convertWarehousingCodesToIds(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> ids = new ArrayList<>();
        for (String code : codes) {
            if (StringUtils.hasText(code)) {
                // 根据入库单编码查询入库单信息
                com.erp.entity.production.Warehousing warehousing = warehousingMapper.selectByWarehousingNo(code);

                if (warehousing != null && warehousing.getWarehousingId() != null) {
                    ids.add(warehousing.getWarehousingId());
                } else {
                    log.warn("入库单编码 {} 不存在，跳过状态更新", code);
                }
            }
        }

        return ids;
    }

    /**
     * 结算单创建过程中更新入库单状态
     * @param createDTO 结算单创建请求
     * @throws BusinessException 当入库单编码不存在或状态更新失败时抛出异常
     */
    private void updateWarehousingStatusAfterSettlementCreation(SettlementCreateDTO createDTO) {
        // 只有当引用类型为入库单时才需要更新状态
        if (!"warehousing".equals(createDTO.getReferenceType())) {
            return;
        }

        List<String> referenceCodes = createDTO.getReferenceCodes();
        if (referenceCodes == null || referenceCodes.isEmpty()) {
            log.info("引用编码列表为空，无需更新入库单状态");
            return;
        }

        log.info("开始更新入库单状态，引用编码数量：{}", referenceCodes.size());

        // 将入库单编码转换为ID列表
        List<Integer> warehousingIds = convertWarehousingCodesToIds(referenceCodes);
        if (warehousingIds.isEmpty()) {
            throw new BusinessException("没有找到有效的入库单ID，无法创建结算单。引用编码：" + referenceCodes);
        }

        // 获取当前操作用户ID
        Integer operatorId = SecurityUtil.getCurrentUserId();
        if (operatorId == null) {
            throw new BusinessException("无法获取当前操作用户ID");
        }

        // 批量更新入库单状态为"结算中"
        int updatedCount = warehousingService.batchUpdateWarehousingStatus(
            warehousingIds,
            com.erp.common.enums.WarehousingStatusEnum.SETTLING.getValue(),
            operatorId
        );

        log.info("入库单状态更新完成，成功更新 {} 个入库单状态为'结算中'", updatedCount);

        if (updatedCount != warehousingIds.size()) {
            throw new BusinessException(String.format("入库单状态更新数量不匹配，期望更新 %d 个，实际更新 %d 个",
                warehousingIds.size(), updatedCount));
        }
    }

    @Override
    public SettlementAuditDataDTO getSettlementAuditData(Long settlementId) {
        long startTime = System.currentTimeMillis();
        log.info("获取结算单审核数据，settlementId={}", settlementId);

        // 参数校验
        if (settlementId == null || settlementId <= 0) {
            throw new BusinessException("无效的结算单ID");
        }

        // 创建审核数据DTO
        SettlementAuditDataDTO auditData = new SettlementAuditDataDTO();

        // 设置结算单ID
        auditData.setSettlementId(settlementId);

        long queryStartTime = System.currentTimeMillis();
        // 查询结算单主表信息
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算单不存在");
        }
        long queryEndTime = System.currentTimeMillis();
        log.debug("主表查询耗时: {}ms", (queryEndTime - queryStartTime));

        // 直接设置结算单基础字段
        auditData.setSettlementCode(settlement.getSettlementCode());
        auditData.setContractCode(settlement.getContractCode());
        auditData.setContractId(settlement.getContractId());
        auditData.setSettlementType(settlement.getSettlementType());
        auditData.setSourceType(settlement.getSourceType());
        auditData.setSettlementPeriodStart(settlement.getSettlementPeriodStart());
        auditData.setSettlementPeriodEnd(settlement.getSettlementPeriodEnd());
        auditData.setTotalAmount(settlement.getSettlementAmount());
        auditData.setReceivedAmount(settlement.getReceivedAmount());
        auditData.setStatus(settlement.getStatus());
        auditData.setCreatorName(settlement.getCreatorName());
        auditData.setCreateTime(settlement.getCreateTime());
        auditData.setAuditorName(getAuditorNameById(settlement.getAuditorId()));
        auditData.setAuditTime(settlement.getAuditTime());
        auditData.setRemark(settlement.getRemark());
        auditData.setIsLocked(settlement.getIsLocked());

        // 构建按量结算和总价包干明细列表（分类）
        // 注意：由于父子结构已移除，通过 contractPlanTotal > 0 判断是否为总价包干模式
        List<WasteSettlementDetailDTO> allSettlementDetails = buildWasteSettlementDetailDTOs(settlementId);

        // 按结算模式分类（通过 contractPlanTotal > 0 判断）
        List<WasteSettlementDetailDTO> quantityItems = new ArrayList<>();
        List<WasteSettlementDetailDTO> lumpSumItems = new ArrayList<>();

        for (WasteSettlementDetailDTO detail : allSettlementDetails) {
            // 通过合同计划总量判断结算模式：> 0 为总价包干，否则为按量结算
            boolean isLumpSum = detail.getContractPlanTotal() != null
                && detail.getContractPlanTotal().compareTo(BigDecimal.ZERO) > 0;

            if (isLumpSum) {
                lumpSumItems.add(detail);
            } else {
                quantityItems.add(detail);
            }
        }

        auditData.setQuantityItems(quantityItems);
        auditData.setLumpSumItems(lumpSumItems);
       
        // 构建价外服务明细
        List<ServiceItemDTO> serviceItems = buildServiceItemDTOs(settlementId);
        auditData.setServiceItems(serviceItems);
      
        // 初始化变量
        List<WarehousingDetailResponse> warehousingCodes;
        List<String> transportCodes;
        List<ContractDetailResponse> contractInfo;

        // 根据sourceType条件化获取数据
        String sourceType = settlement.getSourceType();

        if ("WAREHOUSING".equals(sourceType)) {
            warehousingCodes = buildWarehousingCodes(settlementId);
            
            transportCodes = new ArrayList<>();
            
            contractInfo = buildContractInfo(settlement.getContractCode());
         
        } else if ("CONTRACT".equals(sourceType)) {
            warehousingCodes = new ArrayList<>();
            
            transportCodes = buildTransportCodes(settlementId);
           
            contractInfo = buildContractInfo(settlement.getContractCode());
          
        } else {
            // 其他sourceType情况，获取所有数据作为默认行为
            warehousingCodes = buildWarehousingCodes(settlementId);
            
            transportCodes = buildTransportCodes(settlementId);
           
            contractInfo = buildContractInfo(settlement.getContractCode());
    }

        // 设置到auditData
        auditData.setWarehousingCodes(warehousingCodes);
        auditData.setTransportCodes(transportCodes);
        auditData.setContractInfo(contractInfo);

        // 构建辅助计量单位选项
        long auxStart = System.currentTimeMillis();
        List<AuxUnitOptionDTO> auxUnitOptions = buildAuxUnitOptions();
        auditData.setAuxUnitOptions(auxUnitOptions);
        log.debug("辅助计量单位选项查询耗时: {}ms, 数量: {}", (System.currentTimeMillis() - auxStart), auxUnitOptions.size());

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("结算单审核数据构建完成，settlementId={}, 总耗时: {}ms, " +
                "按量结算: {}, 总价包干: {}, 服务项: {}, 入库记录: {}, 运输记录: {}",
                settlementId, totalTime, quantityItems.size(), lumpSumItems.size(),
                serviceItems.size(), warehousingCodes.size(), transportCodes.size());

        return auditData;
    }



    /**
     * 构建WasteSettlementDetailDTO列表 - 优化版本
     * 使用批量查询避免N+1查询问题
     */
    private List<WasteSettlementDetailDTO> buildWasteSettlementDetailDTOs(Long settlementId) {
        // 查询结算明细数据
        List<SettlementWasteDetail> settlementDetails = settlementWasteDetailMapper.selectList(
            new QueryWrapper<SettlementWasteDetail>()
                .eq("结算单编号", settlementId)
                .orderByAsc("序号")
        );

        if (settlementDetails.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询payer信息，避免N+1查询
        List<Integer> contractItemIds = settlementDetails.stream()
            .map(SettlementWasteDetail::getContractItemId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        final Map<Integer, String> payerMap = new HashMap<>();
        if (!contractItemIds.isEmpty()) {
            // 使用新的批量查询方法
            List<com.erp.controller.finance.dto.ContractItemPayerDTO> payerDTOs =
                contractWasteItemMapper.selectPayerByContractItemIds(contractItemIds);

            // 转换为Map
            payerDTOs.forEach(dto -> payerMap.put(dto.getContractItemId(), dto.getPayer()));

            log.debug("批量查询到 {} 个合同条目的payer信息", payerMap.size());
        }

        // 转换DTO，使用预查询的payer信息
        return settlementDetails.stream()
            .map(detail -> convertToWasteSettlementDetailDTO(detail, payerMap))
            .collect(Collectors.toList());
    }


    /**
     * 转换SettlementWasteDetail为WasteSettlementDetailDTO
     * 注意：废物信息（废物类别、废物代码、废物名称）需要从 SETTLEMENT_WASTE_INFO 表获取
     */
    private WasteSettlementDetailDTO convertToWasteSettlementDetailDTO(SettlementWasteDetail detail, Map<Integer, String> payerMap) {
        WasteSettlementDetailDTO dto = new WasteSettlementDetailDTO();
        dto.setReceiveDate(detail.getReceiveDate());
        dto.setRemark(detail.getRemark());
        dto.setSourceType(detail.getSourceType());
        dto.setSourceCode(detail.getSourceCode());
        dto.setGdManifestCode(detail.getGdManifestCode());
        dto.setContractItemId(detail.getContractItemId() != null ? detail.getContractItemId().longValue() : null);
        dto.setBasicSettlementQuantity(detail.getBasicSettlementQuantity());
        dto.setBasicUnit(detail.getBasicUnit());
        dto.setAuxiliarySettlementQuantity(detail.getAuxiliarySettlementQuantity());
        dto.setAuxiliaryUnit(detail.getAuxiliaryUnit());
        dto.setContractPlanTotal(detail.getContractPlanTotal());
        dto.setAuxiliaryContractPlanTotal(detail.getAuxiliaryContractPlanTotal());
        dto.setSettledBasicQuantity(detail.getSettledBasicQuantity());
        dto.setSettledAuxiliaryQuantity(detail.getSettledAuxiliaryQuantity());
        dto.setCurrentAccumulatedQuantity(detail.getCurrentAccumulatedQuantity());
        dto.setUnitPrice(detail.getUnitPrice());
        dto.setSaveUnitPrice(detail.getSaveUnitPrice());
        dto.setAuxiliaryUnitPrice(detail.getAuxiliaryUnitPrice());
        dto.setExcessUnitPrice(detail.getExcessUnitPrice());
        dto.setExcessAmount(detail.getExcessAmount());
        dto.setAmount(detail.getAmount());
        dto.setEnableAuxiliaryAccounting(detail.getEnableAuxiliaryAccounting());
        dto.setPayer(payerMap.get(detail.getContractItemId()));
        dto.setDetailId(detail.getDetailId());
        dto.setSequence(detail.getSequence());
        dto.setExcessQuantity(detail.getExcessQuantity());
        dto.setIsLocked(detail.getIsLocked());

        // 查询危废信息并组装到 wasteInfoList 字段（支持多条危废信息）
        if (detail.getDetailId() != null) {
            List<SettlementWasteInfo> wasteInfoList = settlementWasteInfoMapper.selectByDetailId(detail.getDetailId());
            if (wasteInfoList != null && !wasteInfoList.isEmpty()) {
                List<SettlementWasteInfoDTO> wasteInfoDTOList = wasteInfoList.stream().map(wasteInfo -> {
                    SettlementWasteInfoDTO wasteInfoDTO = new SettlementWasteInfoDTO();
                    wasteInfoDTO.setWasteInfoId(wasteInfo.getWasteInfoId());
                    wasteInfoDTO.setDetailId(wasteInfo.getDetailId());
                    wasteInfoDTO.setWasteCategory(wasteInfo.getWasteCategory());
                    wasteInfoDTO.setWasteCode(wasteInfo.getWasteCode());
                    wasteInfoDTO.setWasteName(wasteInfo.getWasteName());
                    wasteInfoDTO.setSourceWasteItemId(wasteInfo.getSourceWasteItemId());
                    return wasteInfoDTO;
                }).collect(Collectors.toList());
                dto.setWasteInfoList(wasteInfoDTOList);
            }
        }

        return dto;
    }

    /**
     * 根据用户ID获取审核人姓名
     */
    private String getAuditorNameById(Integer auditorId) {
        if (auditorId == null) {
            return null;
        }
        try {
            com.erp.entity.system.Employee employee = employeeMapper.selectById(auditorId);
            return employee != null ? employee.getEmployeeName() : null;
        } catch (Exception e) {
            log.warn("查询审核人姓名失败，auditorId={}, error={}", auditorId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取辅助计量单位选项列表
     * 从系统配置 UNIT 中读取，并转换为 DTO 格式
     */
    private List<AuxUnitOptionDTO> getAuxUnitOptions() {
        try {
            SysConfig config = sysConfigService.getByName("UNIT");
            if (config == null || config.getValue() == null || config.getValue().isEmpty()) {
                log.debug("系统配置 UNIT 为空或不存在");
                return new ArrayList<>();
            }

            // 配置值为 JSON 数组格式，如 ["桶", "袋", "车"]
            String value = config.getValue().trim();
            List<AuxUnitOptionDTO> options = new ArrayList<>();

            // 尝试解析为 JSON 数组
            if (value.startsWith("[")) {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<String> units = objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                for (int i = 0; i < units.size(); i++) {
                    AuxUnitOptionDTO dto = new AuxUnitOptionDTO();
                    dto.setUnitCode(units.get(i));
                    dto.setUnitName(units.get(i));
                    dto.setUnitType("辅助单位");
                    dto.setEnabled(true);
                    dto.setDisplayOrder(i + 1);
                    options.add(dto);
                }
            } else {
                // 兼容逗号分隔的格式
                String[] units = value.split("[,，]");
                for (int i = 0; i < units.length; i++) {
                    String unit = units[i].trim();
                    if (!unit.isEmpty()) {
                        AuxUnitOptionDTO dto = new AuxUnitOptionDTO();
                        dto.setUnitCode(unit);
                        dto.setUnitName(unit);
                        dto.setUnitType("辅助单位");
                        dto.setEnabled(true);
                        dto.setDisplayOrder(i + 1);
                        options.add(dto);
                    }
                }
            }

            log.debug("获取辅助计量单位选项完成，数量={}", options.size());
            return options;
        } catch (Exception e) {
            log.warn("获取辅助计量单位选项失败，error={}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取结算单关联的所有单号（从 SETTLEMENT_REFERENCE 表）
     * 包括入库单号和运输单号
     */
    private Map<String, List<String>> getReferenceCodesBySettlementId(Long settlementId) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        try {
            // 查询入库单号
            List<String> warehousingCodes = settlementReferenceMapper.selectWarehousingCodesBySettlementId(settlementId);
            result.put("WAREHOUSING", warehousingCodes != null ? warehousingCodes : new ArrayList<>());

            // 查询运输单号
            List<String> transportCodes = settlementReferenceMapper.selectTransportCodesBySettlementId(settlementId);
            result.put("TRANSPORT", transportCodes != null ? transportCodes : new ArrayList<>());

            log.debug("获取结算单关联单号完成，settlementId={}, 入库单={}, 运输单={}",
                    settlementId, result.get("WAREHOUSING").size(), result.get("TRANSPORT").size());
        } catch (Exception e) {
            log.warn("获取结算单关联单号失败，settlementId={}, error={}", settlementId, e.getMessage());
            result.put("WAREHOUSING", new ArrayList<>());
            result.put("TRANSPORT", new ArrayList<>());
        }
        return result;
    }

    /**
     * 构建价外服务明细列表
     */
    private List<ServiceItemDTO> buildServiceItemDTOs(Long settlementId) {
        // 查询价外服务表（OUT_OF_SCOPE_SERVICE）关联到该结算单
        QueryWrapper<OutOfScopeService> svcWrapper = new QueryWrapper<>();
        svcWrapper.eq("关联业务类型", "SETTLEMENT");
        svcWrapper.eq("关联业务单号", settlementId.intValue());
        List<OutOfScopeService> services = outOfScopeServiceMapper.selectList(svcWrapper);

        List<ServiceItemDTO> serviceDTOs = new ArrayList<>();
        if (services != null) {
            for (OutOfScopeService s : services) {
                ServiceItemDTO sd = new ServiceItemDTO();
                if (s.getCreatedAt() != null) {
                    sd.setReceiveDate(s.getCreatedAt().toLocalDate());
                }
                sd.setOutOfScopeServiceId(s.getOutOfScopeServiceId());
                sd.setProject(s.getProject());
                sd.setSpec(s.getSpec());
                sd.setBasicSettlementQuantity(s.getPlannedQuantity());
                sd.setBasicUnit(s.getUnit());
                sd.setUnitPrice(s.getContractUnitPrice());
                // 价外服务属于甲方
                sd.setPayer("甲方");
                // 优先使用已结算金额，否则使用计划数量 * 单价
                if (s.getSettledAmount() != null) {
                    sd.setAmount(s.getSettledAmount());
                } else if (s.getPlannedQuantity() != null && s.getContractUnitPrice() != null) {
                    sd.setAmount(s.getPlannedQuantity().multiply(s.getContractUnitPrice()));
                }
                sd.setRemark(s.getRemark());
                serviceDTOs.add(sd);
            }
        }

        return serviceDTOs;
    }

    /**
     * 构建关联入库记录列表 - 优化版本：使用批量查询
     */
    private List<WarehousingDetailResponse> buildWarehousingCodes(Long settlementId) {
        // 查询结算单关联的入库记录
        List<String> warehousingCodes = settlementReferenceMapper.selectWarehousingCodesBySettlementId(settlementId);

        if (warehousingCodes == null || warehousingCodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用异步批处理机制并行查询入库单信息
        List<java.util.concurrent.CompletableFuture<Warehousing>> warehousingFutures = warehousingCodes.stream()
            .map(code -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return warehousingMapper.selectByWarehousingNo(code);
                } catch (Exception e) {
                    log.warn("异步查询入库单信息失败，入库单号={}, error={}", code, e.getMessage());
                    return null;
                }
            }))
            .collect(Collectors.toList());

        // 等待所有异步查询完成
        List<Warehousing> warehousings = warehousingFutures.stream()
            .map(java.util.concurrent.CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (warehousings.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取入库单ID列表
        List<Integer> warehousingIds = warehousings.stream()
            .map(Warehousing::getWarehousingId)
            .collect(Collectors.toList());

        // 使用批量查询方法获取详情
        return warehousingService.getWarehousingDetailsBatch(warehousingIds);
    }

    /**
     * 构建关联运输记录编码列表
     */
    private List<String> buildTransportCodes(Long settlementId) {
        // 查询结算单关联的运输记录编码
        return settlementReferenceMapper.selectTransportCodesBySettlementId(settlementId);
    }

    /**
     * 构建合同信息列表
     */
    private List<ContractDetailResponse> buildContractInfo(String contractCode) {
        if (contractCode == null || contractCode.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 根据合同编码查询合同基本信息
            Contract contract = contractMapper.selectBasicInfoByContractCode(contractCode);
            if (contract == null) {
                log.warn("未找到合同信息，contractCode={}", contractCode);
                return new ArrayList<>();
            }

            // 调用合同服务获取完整的合同详情
            ContractDetailResponse contractDetail = contractService.getContractDetail(contract.getContractId());
            if (contractDetail == null) {
                log.warn("获取合同详情失败，contractId={}", contract.getContractId());
                return new ArrayList<>();
            }

            List<ContractDetailResponse> result = new ArrayList<>();
            result.add(contractDetail);
            return result;

        } catch (Exception e) {
            log.error("查询合同信息失败，contractCode={}, error={}", contractCode, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建辅助计量单位选项列表
     */
    private List<AuxUnitOptionDTO> buildAuxUnitOptions() {
        // TODO: 查询辅助计量单位选项
        // 这里可能需要查询字典表或配置表
        return new ArrayList<>();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteSettlement(Long settlementId) {
        log.info("开始删除结算单，settlementId={}", settlementId);

        // 1. 获取当前用户信息（用于日志记录）
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        String clientIp = logRecordService.getClientIp(request);

        Settlement settlement = null;
        try {
            // 2. 验证结算单状态并获取完整数据（用于日志记录）
            settlement = validateSettlementCanBeDeleted(settlementId);

            // 3. 查询关联的入库单编码
            List<String> warehousingCodes = getRelatedWarehousingCodes(settlementId);
            log.info("结算单关联的入库单数量：{}", warehousingCodes.size());

            // 4. 执行删除操作
            executeDeletion(settlementId, warehousingCodes, settlement);

            // 5. 记录成功日志
            logRecordService.recordOperationLog(
                "合同结算", "删除",
                "删除结算单 " + settlement.getSettlementCode(),
                currentUserId, clientIp, true, null
            );

            // 6. 记录数据变更日志
            logRecordService.recordDataChangeLog(
                "合同结算", "SETTLEMENT", settlementId.toString(),
                "删除", "删除结算单 " + settlement.getSettlementCode(),
                settlement, null, // 原始数据，新数据为null表示删除
                currentUserId, clientIp, true, null
            );

            log.info("结算单删除成功，settlementId={}", settlementId);
            return Result.success();

        } catch (Exception e) {
            log.error("删除结算单失败，settlementId={}", settlementId, e);

            // 记录失败日志
            String settlementCode = settlement != null ? settlement.getSettlementCode() : "未知";
            logRecordService.recordOperationLog(
                "合同结算", "删除",
                "删除结算单 " + settlementCode,
                currentUserId, clientIp, false, e.getMessage()
            );

            throw e; // 重新抛出异常，确保事务回滚
        }
    }

    /**
     * 批量删除结算单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDeleteSettlements(List<Long> settlementIds) {
        log.info("开始批量删除结算单，settlementIds={}", settlementIds);

        if (settlementIds == null || settlementIds.isEmpty()) {
            return Result.error("请选择要删除的结算单");
        }

        // 获取当前用户信息
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        String clientIp = logRecordService.getClientIp(request);

        int successCount = 0;
        int failCount = 0;
        List<String> failedReasons = new ArrayList<>();

        for (Long settlementId : settlementIds) {
            try {
                // 验证结算单状态和制单人权限
                Settlement settlement = validateSettlementCanBeDeleted(settlementId);

                // 校验制单人权限：只有制单人可以删除自己创建的结算单
                if (!currentUserId.equals(settlement.getCreatorId())) {
                    failCount++;
                    String reason = "ID " + settlementId + ": 不是制单人，无权删除该结算单";
                    failedReasons.add(reason);
                    log.warn("无权删除结算单：settlementId={}，当前用户={}，制单人={}", settlementId, currentUserId, settlement.getCreatorId());
                    continue;
                }

                // 查询关联的入库单编码
                List<String> warehousingCodes = getRelatedWarehousingCodes(settlementId);

                // 执行删除操作
                executeDeletion(settlementId, warehousingCodes, settlement);

                // 记录成功日志
                logRecordService.recordOperationLog(
                    "合同结算", "批量删除",
                    "批量删除结算单 " + settlement.getSettlementCode(),
                    currentUserId, clientIp, true, null
                );

                successCount++;

            } catch (Exception e) {
                failCount++;
                String reason = "ID " + settlementId + ": " + (e instanceof BusinessException ? e.getMessage() : "删除失败");
                failedReasons.add(reason);
                log.warn("批量删除结算单失败：settlementId={}，原因：{}", settlementId, e.getMessage());
            }
        }

        log.info("批量删除结算单完成，成功={}，失败={}", successCount, failCount);

        if (failCount > 0) {
            String errorMsg = "部分结算单删除失败：\n" + String.join("\n", failedReasons);
            return Result.error(errorMsg);
        }

        return Result.success("批量删除成功，共删除 " + successCount + " 个结算单", null);
    }

    /**
     * 验证结算单是否可以被删除
     * @param settlementId 结算单ID
     * @return 结算单实体
     */
    private Settlement validateSettlementCanBeDeleted(Long settlementId) {
        // 1. 检查结算单是否存在
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算单不存在");
        }

        // 2. 检查结算单状态是否允许删除
        if (!DeletableSettlementStatus.isDeletable(settlement.getStatus())) {
            throw new BusinessException("结算单状态不允许删除，当前状态: " + settlement.getStatus());
        }

        // 3. 检查是否被锁定
        if (Boolean.TRUE.equals(settlement.getIsLocked())) {
            throw new BusinessException("结算单已被锁定，不允许删除");
        }

        log.info("结算单验证通过，settlementId={}, status={}", settlementId, settlement.getStatus());
        return settlement;
    }

    /**
     * 获取结算单关联的入库单编码
     * @param settlementId 结算单ID
     * @return 入库单编码列表
     */
    private List<String> getRelatedWarehousingCodes(Long settlementId) {
        QueryWrapper<SettlementWasteDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("结算单编号", settlementId)
               .isNotNull("关联来源单号")
               .eq("关联来源类型", "warehousing")
               .select("关联来源单号");

        List<SettlementWasteDetail> details = settlementWasteDetailMapper.selectList(wrapper);
        return details.stream()
                     .map(SettlementWasteDetail::getSourceCode)
                     .filter(StringUtils::hasText)
                     .distinct()
                     .collect(Collectors.toList());
    }

    /**
     * 执行删除操作
     * @param settlementId 结算单ID
     * @param warehousingCodes 关联的入库单编码
     * @param settlement 结算单实体
     */
    private void executeDeletion(Long settlementId, List<String> warehousingCodes, Settlement settlement) {
        // 1. 将关联的入库单状态改为"待结算"（仅当结算单来源类型为WAREHOUSING时）
        if ("warehousing".equals(settlement.getSourceType()) && !warehousingCodes.isEmpty()) {
            int updatedCount = warehousingMapper.batchUpdateStatusToPendingSettlement(warehousingCodes);
            log.info("更新入库单状态完成，影响数量：{}", updatedCount);
        }

        // 2. 删除结算明细
        QueryWrapper<SettlementWasteDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("结算单编号", settlementId);
        int detailDeleted = settlementWasteDetailMapper.delete(detailWrapper);
        log.info("删除结算明细完成，删除数量：{}", detailDeleted);

        // 3. 删除结算关联记录
        QueryWrapper<SettlementReference> referenceWrapper = new QueryWrapper<>();
        referenceWrapper.eq("结算单编号", settlementId);
        int referenceDeleted = settlementReferenceMapper.delete(referenceWrapper);
        log.info("删除结算关联记录完成，删除数量：{}", referenceDeleted);

        // 4. 删除价外服务记录
        QueryWrapper<OutOfScopeService> serviceWrapper = new QueryWrapper<>();
        serviceWrapper.eq("关联业务类型", "SETTLEMENT");
        serviceWrapper.eq("关联业务单号", settlementId.intValue());
        int serviceDeleted = outOfScopeServiceMapper.delete(serviceWrapper);
        log.info("删除价外服务记录完成，删除数量：{}", serviceDeleted);

        // 5. 删除结算单主表记录
        int settlementDeleted = settlementMapper.deleteById(settlementId);
        if (settlementDeleted != 1) {
            throw new BusinessException("删除结算单主表记录失败");
        }
        log.info("删除结算单主表记录完成");
    }

    /**
     * 结算单审核后更新关联入库单状态为已结算
     * @param settlementId 结算单ID
     * @param settlement 结算单实体（用于日志记录）
     */
    private void updateRelatedWarehousingStatusToSettled(Long settlementId, Settlement settlement) {
        // 获取当前用户信息
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        String clientIp = logRecordService.getClientIp(request);

        // 1. 查询关联的入库单编码
        List<String> warehousingCodes = getRelatedWarehousingCodesForSettlement(settlementId);
        if (warehousingCodes.isEmpty()) {
            log.debug("结算单 {} 没有关联的入库单，跳过状态同步", settlementId);
            return;
        }

        log.info("结算单审核后开始同步入库单状态，settlementId={}，入库单数量={}", settlementId, warehousingCodes.size());

        // 2. 批量更新入库单状态为"已结算"
        List<Integer> warehousingIds = convertWarehousingCodesToIds(warehousingCodes);
        int updatedCount = warehousingService.batchUpdateWarehousingStatus(
            warehousingIds,
            com.erp.common.enums.WarehousingStatusEnum.SETTLED.getValue(),
            currentUserId
        );

        // 2.1 同步更新库存表：按危废条目编号匹配，累加基本核算重量（实际收运数量）
        // 注意：此处不做 try-catch，库存同步失败应触发整体事务回滚，保证数据一致性
        syncStockOnSettled(warehousingIds);

        // 3. 记录操作日志（日志失败不影响主流程）
        try {
            logRecordService.recordOperationLog(
                "合同结算", "状态同步",
                "结算单审核同步入库单状态：结算单号=" + settlement.getSettlementCode() + "，入库单数量=" + warehousingCodes.size(),
                currentUserId, clientIp, true, null
            );

            // 4. 记录数据变更日志（为每个入库单记录）
            for (String warehousingCode : warehousingCodes) {
                Map<String, Object> oldWarehousingData = new HashMap<>();
                oldWarehousingData.put("status", "结算中");
                Map<String, Object> newWarehousingData = new HashMap<>();
                newWarehousingData.put("status", "已结算");
                logRecordService.recordDataChangeLog(
                    "合同结算", "WAREHOUSING", warehousingCode,
                    "状态同步", "结算单审核后状态更新：结算单号=" + settlement.getSettlementCode(),
                    oldWarehousingData, newWarehousingData,
                    currentUserId, clientIp, true, null
                );
            }
        } catch (Exception logEx) {
            log.warn("结算单审核同步日志记录失败，settlementId={}，error={}", settlementId, logEx.getMessage());
        }

        log.info("结算单审核同步入库单状态完成，settlementId={}，成功更新 {} 个入库单", settlementId, updatedCount);
    }

    /**
     * 入库单变为已结算时，同步更新库存表的基本核算重量（当前重量）
     * 匹配规则：按废物代码 + 废物类别匹配 STOCK 表
     * 更新规则：当前重量 += 实际收运数量（actualQty）
     * 如果库存记录不存在则新建
     * 优化：批量预加载危废条目和类别，避免 N+1 查询；乐观锁冲突抛出异常触发事务回滚
     *
     * @param warehousingIds 已结算的入库单编号列表
     */
    private void syncStockOnSettled(List<Integer> warehousingIds) {
        if (warehousingIds == null || warehousingIds.isEmpty()) {
            return;
        }

        // 1. 批量查询所有入库单的危废明细
        List<com.erp.entity.production.WarehousingWasteItem> items =
                warehousingWasteItemMapper.selectByWarehousingIds(warehousingIds);
        if (items == null || items.isEmpty()) {
            log.debug("入库单危废明细为空，跳过库存同步，warehousingIds={}", warehousingIds);
            return;
        }

        // 2. 收集所有有效的危废条目编号，批量预加载 HazardousWasteItem
        List<Integer> hazItemIds = items.stream()
                .filter(i -> i.getActualQty() != null && i.getHazardousWasteItemId() != null)
                .map(com.erp.entity.production.WarehousingWasteItem::getHazardousWasteItemId)
                .distinct()
                .collect(Collectors.toList());
        if (hazItemIds.isEmpty()) {
            log.debug("所有危废明细缺少actualQty或hazardousWasteItemId，跳过库存同步");
            return;
        }

        Map<Integer, com.erp.entity.system.HazardousWasteItem> hazItemMap =
                hazardousWasteItemMapper.selectBatchIds(hazItemIds).stream()
                        .collect(Collectors.toMap(
                                com.erp.entity.system.HazardousWasteItem::getItemId,
                                h -> h));

        // 3. 收集所有类别编号，批量预加载 HazardousWasteCategory
        List<Integer> categoryIds = hazItemMap.values().stream()
                .map(com.erp.entity.system.HazardousWasteItem::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> categoryCodeMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            hazardousWasteCategoryMapper.selectBatchIds(categoryIds)
                    .forEach(c -> categoryCodeMap.put(c.getCategoryId(), c.getWasteCategory()));
        }

        log.info("开始同步库存，入库单数量={}，危废明细数量={}", warehousingIds.size(), items.size());

        // 4. 同一批次内按「废物代码+废物类别+废物名称」合并累加，避免同类多条明细重复查库/乐观锁冲突
        Map<String, BigDecimal> mergedWeightMap = new LinkedHashMap<>();
        // key -> "wasteCode|wasteCategory|wasteName"，value -> 累加重量
        // 同时保留一份 key -> HazardousWasteItem 供新建库存时使用
        Map<String, com.erp.entity.system.HazardousWasteItem> keyToHazItem = new LinkedHashMap<>();

        for (com.erp.entity.production.WarehousingWasteItem item : items) {
            if (item.getActualQty() == null || item.getHazardousWasteItemId() == null) {
                continue;
            }
            com.erp.entity.system.HazardousWasteItem hazItem = hazItemMap.get(item.getHazardousWasteItemId());
            if (hazItem == null) {
                log.warn("未找到危废条目，hazardousWasteItemId={}，跳过", item.getHazardousWasteItemId());
                continue;
            }
            String wasteCode = hazItem.getWasteCode();
            String wasteCategory = categoryCodeMap.get(hazItem.getCategoryId());
            String wasteName = hazItem.getWasteName();
            if (!StringUtils.hasText(wasteCode) || !StringUtils.hasText(wasteCategory)) {
                log.warn("危废条目缺少废物代码或废物类别，hazardousWasteItemId={}，跳过", item.getHazardousWasteItemId());
                continue;
            }
            String key = wasteCode + "|" + wasteCategory + "|" + (wasteName == null ? "" : wasteName);
            mergedWeightMap.merge(key, item.getActualQty(), BigDecimal::add);
            keyToHazItem.putIfAbsent(key, hazItem);
        }

        // 5. 按合并后的 key 逐条更新或新建库存
        for (Map.Entry<String, BigDecimal> entry : mergedWeightMap.entrySet()) {
            String key = entry.getKey();
            BigDecimal totalQty = entry.getValue();
            String[] parts = key.split("\\|", -1);
            String wasteCode = parts[0];
            String wasteCategory = parts[1];
            String wasteName = parts.length > 2 ? parts[2] : null;

            com.erp.entity.system.HazardousWasteItem hazItem = keyToHazItem.get(key);

            com.erp.entity.production.Stock stock =
                    stockMapper.selectByWasteCodeAndCategoryAndName(wasteCode, wasteCategory, wasteName);

            if (stock != null) {
                // 使用 updateById + @Version 乐观锁，MyBatis-Plus 自动处理版本号
                stock.setCurrentWeight(stock.getCurrentWeight().add(totalQty));
                int rows = stockMapper.updateById(stock);
                if (rows == 0) {
                    throw new BusinessException(
                            "库存更新冲突（乐观锁），请重试。stockId=" + stock.getStockId()
                            + "，wasteCode=" + wasteCode);
                }
                log.debug("库存累加成功，stockId={}，wasteCode={}，增加={}",
                        stock.getStockId(), wasteCode, totalQty);
            } else {
                // 不存在则新建库存记录（createTime/updateTime 由数据库默认值填充）
                com.erp.entity.production.Stock newStock = new com.erp.entity.production.Stock();
                newStock.setWasteCategory(wasteCategory);
                newStock.setWasteCode(wasteCode);
                newStock.setWasteName(wasteName);
                newStock.setCurrentWeight(totalQty);
                newStock.setHazardousWasteItemId(hazItem.getItemId());
                newStock.setVersion(0);
                stockMapper.insert(newStock);
                log.debug("新建库存记录，wasteCode={}，wasteCategory={}，初始重量={}",
                        wasteCode, wasteCategory, totalQty);
            }
        }

        log.info("库存同步完成，处理危废明细数量={}", items.size());
    }

    /**
     * 获取结算单关联的入库单编码列表
     * @param settlementId 结算单ID
     * @return 入库单编码列表
     */
    private List<String> getRelatedWarehousingCodesForSettlement(Long settlementId) {
        try {
            log.debug("查询结算单关联入库单编码，settlementId={}", settlementId);

            // 从SettlementReference表查询实际关联的入库单编码
            List<String> codes = settlementReferenceMapper.selectWarehousingCodesBySettlementId(settlementId);

            if (codes != null && !codes.isEmpty()) {
                log.debug("找到关联入库单 {} 个，settlementId={}", codes.size(), settlementId);
                return codes;
            }

            // 如果关联表中没有记录，尝试基于业务规则推导（向后兼容）
            log.debug("关联表中无入库单记录，尝试业务规则推导，settlementId={}", settlementId);
            return inferWarehousingCodesFromDetails(settlementId);

        } catch (Exception e) {
            log.warn("查询结算单关联入库单编码失败，settlementId={}，error={}", settlementId, e.getMessage());
            // 返回空列表实现优雅降级
            return new ArrayList<>();
        }
    }

    /**
     * 从明细数据推导入库单编码（向后兼容处理）
     */
    private List<String> inferWarehousingCodesFromDetails(Long settlementId) {
        try {
            // 查询结算明细中的入库单号
            QueryWrapper<SettlementWasteDetail> wrapper = new QueryWrapper<>();
            wrapper.eq("结算单编号", settlementId)
                   .isNotNull("关联来源单号")
                   .eq("关联来源类型", "warehousing")
                   .select("关联来源单号");

            List<SettlementWasteDetail> details = settlementWasteDetailMapper.selectList(wrapper);
            List<String> codes = details.stream()
                .map(SettlementWasteDetail::getSourceCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

            log.debug("从明细数据推导出入库单编码 {} 个，settlementId={}", codes.size(), settlementId);
            return codes;

        } catch (Exception e) {
            log.warn("从明细数据推导入库单编码失败，settlementId={}", settlementId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析导出结算汇总表的创建人过滤条件（数据范围安全校验）
     * <p>
     * 根据当前员工对「危险废物结算」页面的 viewScope 配置，决定实际生效的 creatorId 过滤值：
     * <ul>
     *   <li>超级管理员：始终返回 null（导出全部）</li>
     *   <li>viewScope=SELF：强制返回当前员工ID，忽略前端传入值，防止越权</li>
     *   <li>viewScope=ALL 或无配置：返回 null（导出全部）</li>
     * </ul>
     * </p>
     *
     * @param currentUserId  当前登录员工ID
     * @param creatorFilter  前端传入的 creatorFilter 参数（仅作参考，后端会覆盖）
     * @return 实际生效的创建人ID过滤值，null 表示不过滤
     */
    @Override
    public Integer resolveExportCreatorFilter(Integer currentUserId, Integer creatorFilter) {
        if (currentUserId == null) {
            return null;
        }
        // 超级管理员直接导出全部
        if (authService.isAdmin(currentUserId)) {
            return null;
        }
        // 获取当前员工对「危险废物结算」页面的权限配置
        final String PAGE_CODE = "合同结算:危险废物结算:页面";
        try {
            com.erp.controller.auth.dto.PagePermissionConfigResponse config =
                    authService.getMyPagePermission(PAGE_CODE);
            if (config == null) {
                // 无权限配置，按最小权限原则：仅导出自己的数据
                log.warn("[resolveExportCreatorFilter] 未找到页面权限配置，employeeId={}，pageCode={}，降级为仅导出自己数据",
                        currentUserId, PAGE_CODE);
                return currentUserId;
            }
            String viewScope = config.getViewScope();
            if ("SELF".equalsIgnoreCase(viewScope)) {
                // viewScope=SELF：强制只导出自己的数据
                log.debug("[resolveExportCreatorFilter] viewScope=SELF，强制 creatorId={}" , currentUserId);
                return currentUserId;
            }
            // viewScope=ALL 或其他：不过滤
            log.debug("[resolveExportCreatorFilter] viewScope={}，导出全部数据", viewScope);
            return null;
        } catch (Exception e) {
            log.error("[resolveExportCreatorFilter] 获取页面权限配置异常，employeeId={}，降级为仅导出自己数据",
                    currentUserId, e);
            return currentUserId;
        }
    }

    /**
     * 批量撤回结算单审核
     * 只有审核中状态的结算单才能撤回
     * 撤回时：
     * 1. 结算单状态改为"待审核"
     * 2. OA审核记录表状态改为"已撤回"，审核次数-1（最低为0）
     */
    @Override
    public BatchOperationResult batchCancelAudit(List<Long> settlementIds) {
        log.info("批量撤回结算单审核，settlementIds={}", settlementIds);

        if (settlementIds == null || settlementIds.isEmpty()) {
            return BatchOperationResult.fail(0);
        }

        int successCount = 0;
        int failCount = 0;
        List<BatchOperationResult.FailureDetail> failures = new java.util.ArrayList<>();

        // 获取当前用户ID
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        // 检查是否为管理员
        boolean isAdmin = authService.isAdmin(currentUserId);
        log.info("批量撤回结算单审核权限检查：currentUserId={}, isAdmin={}", currentUserId, isAdmin);

        for (Long settlementId : settlementIds) {
            try {
                // 查询结算单
                Settlement settlement = settlementMapper.selectById(settlementId);
                if (settlement == null) {
                    failures.add(new BatchOperationResult.FailureDetail(
                            settlementId, null, "结算单不存在"));
                    failCount++;
                    continue;
                }

                // 验证状态：只有审核中状态才能撤回
                if (!"审核中".equals(settlement.getStatus())) {
                    failures.add(new BatchOperationResult.FailureDetail(
                            settlementId, settlement.getSettlementCode(),
                            "当前状态为【" + settlement.getStatus() + "】，仅审核中状态可撤回"));
                    failCount++;
                    continue;
                }

                // 验证锁定状态
                if (Boolean.TRUE.equals(settlement.getIsLocked())) {
                    failures.add(new BatchOperationResult.FailureDetail(
                            settlementId, settlement.getSettlementCode(), "结算单已锁定，无法撤回"));
                    failCount++;
                    continue;
                }

                // 非管理员需要校验制单人权限：只有制单人可以撤回自己创建的结算单
                if (!isAdmin) {
                    if (!currentUserId.equals(settlement.getCreatorId())) {
                        failures.add(new BatchOperationResult.FailureDetail(
                                settlementId, settlement.getSettlementCode(), "不是制单人，无权撤回该结算单"));
                        failCount++;
                        continue;
                    }
                }

                // 1. 更新结算单状态为"待审核"
                settlement.setStatus("待审核");
                settlement.setUpdateTime(LocalDateTime.now());
                settlement.setUpdateUserId(currentUserId);

                int rows = settlementMapper.updateById(settlement);
                if (rows == 0) {
                    failures.add(new BatchOperationResult.FailureDetail(
                            settlementId, settlement.getSettlementCode(), "更新失败，记录已被其他用户修改"));
                    failCount++;
                    continue;
                }

                // 2. 更新OA审核记录表
                try {
                    OaApprovalRecord pendingOaRecord = oaApprovalRecordService.findPendingBySource(
                            OA_SOURCE_TABLE, settlement.getSettlementId().intValue());

                    if (pendingOaRecord != null) {
                        // 审核次数减1，最低为0
                        Integer currentApprovalCount = pendingOaRecord.getApprovalCount() == null ? 0 : pendingOaRecord.getApprovalCount();
                        pendingOaRecord.setApprovalCount(Math.max(currentApprovalCount - 1, 0));
                        pendingOaRecord.setApprovalStatus("已撤回");
                        pendingOaRecord.setApprovalTime(LocalDateTime.now());
                        oaApprovalRecordMapper.updateById(pendingOaRecord);

                        log.info("OA审核记录撤回成功：settlementId={}, oaRecordId={}, newApprovalCount={}",
                                settlementId, pendingOaRecord.getApprovalRecordId(), pendingOaRecord.getApprovalCount());
                    } else {
                        log.warn("未找到结算单对应的待审核OA记录，跳过OA记录更新：settlementId={}", settlementId);
                    }
                } catch (Exception oaEx) {
                    log.error("更新OA审核记录失败：settlementId={}", settlementId, oaEx);
                    // OA记录更新失败不影响主流程，仅记录日志
                }

                // 记录数据变更日志
                try {
                    Settlement oldSettlement = new Settlement();
                    oldSettlement.setStatus("审核中");
                    Settlement newSettlement = new Settlement();
                    newSettlement.setStatus("待审核");
                    logRecordService.recordDataChangeLog("结算单管理", "SETTLEMENT", String.valueOf(settlementId),
                            "撤回审核", "批量撤回结算单审核，结算单号=" + settlement.getSettlementCode(),
                            oldSettlement, newSettlement, currentUserId, null, true, null);
                } catch (Exception logEx) {
                    log.warn("记录结算单撤回审核数据变更日志失败，settlementId={}", settlementId, logEx);
                }

                successCount++;

            } catch (Exception e) {
                log.error("批量撤回结算单审核异常：settlementId={}", settlementId, e);
                failures.add(new BatchOperationResult.FailureDetail(
                        settlementId, null, "系统异常：" + e.getMessage()));
                failCount++;
            }
        }

        log.info("批量撤回结算单审核完成：successCount={}, failCount={}", successCount, failCount);

        return BatchOperationResult.of(successCount, failCount, failures);
    }

    /**
     * 批量提交结算单审核
     * 只有待审核、已驳回状态的结算单才能提交审核
     * 提交后结算单状态改为"审核中"
     * 同时在OA审核记录表新增一条记录，来源表中文名称为"危险废物结算"
     */
    @Override
    public BatchOperationResult batchSubmitAudit(List<Long> settlementIds) {
        log.info("批量提交结算单审核，settlementIds={}", settlementIds);

        if (settlementIds == null || settlementIds.isEmpty()) {
            return BatchOperationResult.fail(0);
        }

        int successCount = 0;
        int failCount = 0;
        List<BatchOperationResult.FailureDetail> failures = new java.util.ArrayList<>();

        // 获取当前用户ID
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        String currentUserName = SecurityUtil.getEmployeeName();
        // 检查是否为管理员
        boolean isAdmin = authService.isAdmin(currentUserId);
        log.info("批量提交结算单审核权限检查：currentUserId={}, isAdmin={}", currentUserId, isAdmin);

        for (Long settlementId : settlementIds) {
            try {
                // 查询结算单
                Settlement settlement = settlementMapper.selectById(settlementId);
                if (settlement == null) {
                    failures.add(new BatchOperationResult.FailureDetail(
                            settlementId, null, "结算单不存在"));
                    failCount++;
                    continue;
                }

                // 验证状态：只有待审核、已驳回状态才能提交审核（撤回后业务表状态为待审核）
                String currentStatus = settlement.getStatus();
                if (!"待审核".equals(currentStatus) && !"已驳回".equals(currentStatus)) {
                    failures.add(new BatchOperationResult.FailureDetail(
                            settlementId, settlement.getSettlementCode(),
                            "当前状态为【" + currentStatus + "】，仅待审核、已驳回状态可提交审核"));
                    failCount++;
                    continue;
                }

                // 验证锁定状态
                if (Boolean.TRUE.equals(settlement.getIsLocked())) {
                    failures.add(new BatchOperationResult.FailureDetail(
                            settlementId, settlement.getSettlementCode(), "结算单已锁定，无法提交审核"));
                    failCount++;
                    continue;
                }

                // 非管理员需要校验制单人权限：只有制单人可以提交自己创建的结算单
                if (!isAdmin) {
                    if (!currentUserId.equals(settlement.getCreatorId())) {
                        failures.add(new BatchOperationResult.FailureDetail(
                                settlementId, settlement.getSettlementCode(), "不是制单人，无权提交审核该结算单"));
                        failCount++;
                        continue;
                    }
                }

                // 更新结算单状态为"审核中"
                settlement.setStatus("审核中");
                settlement.setUpdateTime(LocalDateTime.now());
                settlement.setUpdateUserId(currentUserId);

                int rows = settlementMapper.updateById(settlement);
                if (rows == 0) {
                    failures.add(new BatchOperationResult.FailureDetail(
                            settlementId, settlement.getSettlementCode(), "更新失败，记录已被其他用户修改"));
                    failCount++;
                    continue;
                }

                // 在OA审核记录表操作：查询是否有已撤回的记录
                try {
                    OaApprovalRecord oaRecord;
                    // 查询OA表中是否有已撤回的记录
                    OaApprovalRecord withdrawnRecord = oaApprovalRecordService.findWithdrawnBySource(
                            OA_SOURCE_TABLE, settlement.getSettlementId().intValue());
                    if (withdrawnRecord != null) {
                        // 有已撤回记录：重新激活该记录，状态改为待审核，审核次数+1
                        oaRecord = oaApprovalRecordService.reactivateWithdrawnRecord(
                                OA_SOURCE_TABLE,
                                settlement.getSettlementId().intValue(),
                                currentUserId,
                                currentUserName
                        );
                        log.info("OA审核记录重新激活（已撤回）：settlementId={}, oaRecordId={}, newApprovalCount={}",
                                settlementId, oaRecord.getApprovalRecordId(), oaRecord.getApprovalCount());
                    } else {
                        // 无已撤回记录：检查是否有待审核记录
                        OaApprovalRecord pendingRecord = oaApprovalRecordService.findPendingBySource(
                                OA_SOURCE_TABLE, settlement.getSettlementId().intValue());
                        if (pendingRecord != null) {
                            log.warn("结算单已存在待审核的OA审批记录：settlementId={}, oaRecordId={}", settlementId, pendingRecord.getApprovalRecordId());
                        } else {
                            // 新建OA审核记录
                            oaRecord = oaApprovalRecordService.submit(
                                    OA_SOURCE_TABLE,
                                    settlement.getSettlementId().intValue(),
                                    "危险废物结算",
                                    settlement.getSettlementCode(),
                                    "危险废物结算：" + settlement.getSettlementCode(),
                                    currentUserId,
                                    currentUserName
                            );
                            log.info("OA审核记录创建成功：settlementId={}, status=审核中", settlementId);
                        }
                    }
                } catch (Exception oaEx) {
                    log.error("OA审核记录操作失败：settlementId={}", settlementId, oaEx);
                }

                // 记录数据变更日志
                try {
                    if (logRecordService != null) {
                        Settlement oldSettlement = new Settlement();
                        oldSettlement.setStatus(currentStatus);
                        Settlement newSettlement = new Settlement();
                        newSettlement.setStatus("审核中");
                        logRecordService.recordDataChangeLog("结算单管理", "SETTLEMENT", String.valueOf(settlementId),
                                "提交审核", "批量提交结算单审核", oldSettlement, newSettlement, currentUserId, null, true, null);
                    }
                } catch (Exception logEx) {
                    log.warn("记录结算单数据变更日志失败，settlementId={}", settlementId, logEx);
                }

                successCount++;
                log.info("结算单提交审核成功：settlementId={}, oldStatus={}, newStatus=审核中",
                        settlementId, currentStatus);

            } catch (Exception e) {
                log.error("批量提交结算单审核异常：settlementId={}", settlementId, e);
                failures.add(new BatchOperationResult.FailureDetail(
                        settlementId, null, "系统异常：" + e.getMessage()));
                failCount++;
            }
        }

        log.info("批量提交结算单审核完成：successCount={}, failCount={}", successCount, failCount);

        return BatchOperationResult.of(successCount, failCount, failures);
    }

    /**
     * 业务费创建专用 - 危废结算单分页查询
     */
    @Override
    public IPage<com.erp.controller.settlement.dto.SettlementForBusinessFeePageResponse> getSettlementForBusinessFeePage(
            com.erp.controller.settlement.dto.SettlementForBusinessFeePageRequest request) {
        log.info("业务费创建专用结算单分页查询，request={}", request);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.erp.controller.settlement.dto.SettlementForBusinessFeePageResponse> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(request.getCurrent(), request.getSize());
        return settlementMapper.selectSettlementForBusinessFeePage(page, request);
    }

}

