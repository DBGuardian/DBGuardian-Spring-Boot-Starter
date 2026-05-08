package com.erp.service.transport.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.transport.dto.*;
import com.erp.entity.contract.OutOfScopeService;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.entity.system.Employee;
import com.erp.entity.transport.OutsourceTransportSettlement;
import com.erp.entity.transport.OutsourceTransportSettlementSlip;
import com.erp.common.exception.BusinessException;
import com.erp.mapper.contract.OutOfScopeServiceMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.mapper.transport.OutsourceTransportSettlementMapper;
import com.erp.mapper.transport.OutsourceTransportSettlementSlipMapper;
import com.erp.service.oa.OaApprovalRecordService;
import com.erp.service.auth.AuthService;
import com.erp.service.transport.OutsourceTransportSettlementService;
import com.erp.common.util.SecurityUtil;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 委外运输结算 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutsourceTransportSettlementServiceImpl implements OutsourceTransportSettlementService {

    private final OutsourceTransportSettlementMapper settlementMapper;
    private final OutsourceTransportSettlementSlipMapper slipRelationMapper;
    private final OutOfScopeServiceMapper outOfScopeServiceMapper;
    private final ObjectMapper objectMapper;
    private final OaApprovalRecordService oaApprovalRecordService;
    private final EmployeeMapper employeeMapper;
    private final PermissionMapper permissionMapper;
    private final EmployeePermissionMapper employeePermissionMapper;
    private final AuthService authService;

    /**
     * 页面权限编码常量
     */
    private static final String TRANSPORT_SETTLEMENT_PAGE_CODE = "合同结算:运输费结算:页面";

    @Override
    public OutsourceSettlementPageResponse getPage(OutsourceSettlementPageRequest request) {
        // ===== 应用数据范围控制（viewScope） =====
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        Integer creatorIdFilter = null;

        if (!admin) {
            // 前端可通过 fieldPermissionPageCode 显式指定页面编码
            String pageCodeForViewScope = request.getFieldPermissionPageCode();
            if (!StringUtils.hasText(pageCodeForViewScope)) {
                pageCodeForViewScope = TRANSPORT_SETTLEMENT_PAGE_CODE;
            }

            EmployeePermission permission = getEmployeePagePermission(currentUserId, pageCodeForViewScope);
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // 仅查看自己创建的结算单
                creatorIdFilter = currentUserId;
            }
        }

        IPage<OutsourceTransportSettlement> page = new Page<>(request.getCurrent(), request.getSize());

        LambdaQueryWrapper<OutsourceTransportSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutsourceTransportSettlement::getDeleted, 0);

        // 数据范围过滤：仅查看自己创建的结算单
        if (creatorIdFilter != null) {
            wrapper.eq(OutsourceTransportSettlement::getCreatorId, creatorIdFilter);
        }

        // 根据条件筛选
        if (request.getContractId() != null) {
            wrapper.eq(OutsourceTransportSettlement::getContractId, request.getContractId());
        }
        if (StringUtils.hasText(request.getContractNo())) {
            wrapper.eq(OutsourceTransportSettlement::getContractNo, request.getContractNo());
        }
        if (StringUtils.hasText(request.getSettlementNo())) {
            wrapper.like(OutsourceTransportSettlement::getSettlementNo, request.getSettlementNo());
        }
        if (StringUtils.hasText(request.getCarrierName())) {
            wrapper.like(OutsourceTransportSettlement::getCarrierName, request.getCarrierName());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(OutsourceTransportSettlement::getStatus, request.getStatus());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(OutsourceTransportSettlement::getSettlementPeriodStart, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(OutsourceTransportSettlement::getSettlementPeriodEnd, request.getEndDate());
        }

        // 结算方向过滤（RECEIVABLE=收款，PAYABLE=付款）
        if (StringUtils.hasText(request.getSettlementDirection())) {
            wrapper.eq(OutsourceTransportSettlement::getPaymentDirection, request.getSettlementDirection());
        }

        // 游离数据查询（合同编号为空）
        if (Boolean.TRUE.equals(request.getIsOrphan())) {
            wrapper.isNull(OutsourceTransportSettlement::getContractId);
        }

        wrapper.orderByDesc(OutsourceTransportSettlement::getCreateTime);

        IPage<OutsourceTransportSettlement> resultPage = settlementMapper.selectPage(page, wrapper);

        OutsourceSettlementPageResponse response = new OutsourceSettlementPageResponse();
        response.setRecords(resultPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        response.setTotal( resultPage.getTotal());
        response.setSize((int) resultPage.getSize());
        response.setCurrent((int) resultPage.getCurrent());
        response.setPages((int) resultPage.getPages());

        return response;
    }

    /**
     * 获取员工的页面权限配置（包含 viewScope / operateScope / canEdit）
     *
     * @param employeeId 员工ID
     * @param pageCode  页面权限编码
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
            log.warn("获取员工页面权限失败，employeeId={}, pageCode={}", employeeId, pageCode, e);
            return null;
        }
    }

    @Override
    public OutsourceSettlementResponse getDetail(Integer settlementId) {
        log.info("查询结算单详情，settlementId={}", settlementId);

        // 查询结算单详情（包含关联的总磅单信息，由Mapper通过嵌套SELECT自动关联）
        OutsourceSettlementResponse response = settlementMapper.selectDetailById(settlementId);

        if (response == null) {
            throw new BusinessException("结算单不存在");
        }

        // 如果 slips 为空，初始化 slipIds
        if (response.getSlipIds() == null && response.getSlips() != null) {
            response.setSlipIds(response.getSlips().stream()
                    .map(OutsourceTransportSettlementSlipVO::getSlipId)
                    .collect(Collectors.toList()));
        }

        // 计算未付款金额
        if (response.getSettlementAmount() != null && response.getPaidAmount() != null) {
            response.setUnpaidAmount(response.getSettlementAmount().subtract(response.getPaidAmount()));
        } else {
            response.setUnpaidAmount(response.getSettlementAmount());
        }

        log.info("查询结算单详情成功，settlementId={}, settlementNo={}, carrierName={}, slipCount={}",
                response.getSettlementId(), response.getSettlementNo(), response.getCarrierName(),
                response.getSlips() != null ? response.getSlips().size() : 0);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(OutsourceSettlementSaveRequest request) {
        OutsourceTransportSettlement settlement = new OutsourceTransportSettlement();

        // 生成结算单编号
        String settlementNo = generateSettlementNo();
        settlement.setSettlementNo(settlementNo);

        // 填充基本信息
        fillBasicInfo(settlement, request);

        // 计算结算数量和金额（从总磅单汇总）
        calculateSettlementAmount(settlement, request);

        // 设置状态为待审核
        settlement.setStatus("待审核");

        // 设置创建人信息
        settlement.setCreatorId(SecurityUtil.getCurrentUserId());
        settlement.setCreatorName(SecurityUtil.getEmployeeName());

        settlementMapper.insert(settlement);

        // 关联总磅单
        if (request.getSlips() != null && !request.getSlips().isEmpty()) {
            saveSlipRelations(settlement.getSettlementId(), settlement.getSettlementNo(), request.getSlips());
        }

        return settlement.getSettlementId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer settlementId, OutsourceSettlementSaveRequest request) {
        OutsourceTransportSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算单不存在");
        }
        if (!"待审核".equals(settlement.getStatus()) && !"已驳回".equals(settlement.getStatus())) {
            throw new BusinessException("只有待审核或已驳回的结算单可以修改");
        }

        // 填充基本信息
        fillBasicInfo(settlement, request);

        // 重新计算结算数量和金额
        calculateSettlementAmount(settlement, request);

        settlementMapper.updateById(settlement);

        // 更新总磅单关联：先删除旧关联，再插入新关联
        slipRelationMapper.deleteBySettlementId(settlementId);
        if (request.getSlips() != null && !request.getSlips().isEmpty()) {
            saveSlipRelations(settlementId, settlement.getSettlementNo(), request.getSlips());
        }

        // 更新价外服务关联：先删除旧关联，再插入新关联
        saveOutOfScopeServices(settlementId, request.getOutOfScopeServices());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFromCreateRequest(Integer settlementId, OutsourceSettlementCreateRequest request) {
        OutsourceTransportSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算单不存在");
        }
        if (!"待审核".equals(settlement.getStatus()) && !"已驳回".equals(settlement.getStatus())) {
            throw new BusinessException("只有待审核或已驳回的结算单可以修改");
        }

        // 填充基本信息
        settlement.setContractId(request.getContractId());
        settlement.setContractNo(request.getContractNo());
        settlement.setCarrierName(request.getCarrierName());
        settlement.setContactPerson(request.getContactPerson());
        settlement.setContactPhone(request.getContactPhone());
        settlement.setBankName(request.getBankName());
        settlement.setCardNumber(request.getCardNumber());
        settlement.setAccountName(request.getAccountName());
        settlement.setRemark(request.getRemark());

        // 从结算周期行中获取结算周期等信息
        List<OutsourceSettlementCreateRequest.SettlementRowDTO> rows = request.getSettlementRows();
        if (rows != null && !rows.isEmpty()) {
            OutsourceSettlementCreateRequest.SettlementRowDTO firstRow = rows.get(0);
            settlement.setSettlementPeriodStart(firstRow.getSettlementPeriodStart());
            settlement.setSettlementPeriodEnd(firstRow.getSettlementPeriodEnd());
            settlement.setSettlementMethod(firstRow.getSettlementMethod());
            settlement.setUnit(firstRow.getUnit());
            settlement.setSettlementPrice(firstRow.getSettlementPrice());
            settlement.setSettlementQuantity(firstRow.getSettlementQuantity());
            settlement.setSettlementAmount(firstRow.getSettlementAmount());
        }

        settlementMapper.updateById(settlement);

        // 更新总磅单关联：先删除旧关联，再插入新关联
        slipRelationMapper.deleteBySettlementId(settlementId);
        if (request.getSlips() != null && !request.getSlips().isEmpty()) {
            saveSlipRelations(settlementId, settlement.getSettlementNo(), request.getSlips());
        }

        // 更新价外服务关联：先删除旧关联，再插入新关联
        saveOutOfScopeServices(settlementId, request.getOutOfScopeServices());

        log.info("更新结算单成功（含价外服务），settlementId={}", settlementId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer settlementId) {
        OutsourceTransportSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算单不存在");
        }
        if (!"待审核".equals(settlement.getStatus()) && !"已驳回".equals(settlement.getStatus())) {
            throw new BusinessException("只有待审核或已驳回的结算单可以删除");
        }

        settlement.setDeleted(1);
        settlementMapper.updateById(settlement);
    }

    @Override
    public SettlementSlipPageResponse getAvailableSlipsForSettlement(SettlementDispatchOrderRequest request) {
        IPage<SettlementSlipResponse> page = new Page<>(request.getCurrent(), request.getSize());

        IPage<SettlementSlipResponse> resultPage = settlementMapper.selectAvailableSlipsForSettlement(
                page,
                request.getContractCode(),
                request.getSearchKeyword(),
                request.getIncludeUnrelated(),
                request.getExcludeSettled()
        );

        // 转换派车单号列表
        List<SettlementSlipResponse> records = resultPage.getRecords().stream()
                .map(slip -> {
                    String codesStr = slip.getDispatchCodesStr();
                    if (codesStr != null && !codesStr.isEmpty()) {
                        slip.setDispatchCodes(Arrays.asList(codesStr.split(",")));
                    } else {
                        slip.setDispatchCodes(new ArrayList<>());
                    }
                    return slip;
                })
                .collect(Collectors.toList());

        SettlementSlipPageResponse response = new SettlementSlipPageResponse();
        response.setRecords(records);
        response.setTotal(resultPage.getTotal());
        response.setSize((int) resultPage.getSize());
        response.setCurrent((int) resultPage.getCurrent());
        response.setPages((int) resultPage.getPages());

        return response;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成结算单编号
     */
    private String generateSettlementNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "OTSS-" + dateStr;
        // 查询当天最大的编号（排除带后缀的分割单，如 -R, -P）
        LambdaQueryWrapper<OutsourceTransportSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("结算单单号 LIKE {0}", prefix + "%")
                .apply("结算单单号 NOT LIKE {0}", prefix + "-R%")
                .apply("结算单单号 NOT LIKE {0}", prefix + "-P%")
                .orderByDesc(OutsourceTransportSettlement::getSettlementId)
                .last("LIMIT 1");

        OutsourceTransportSettlement lastSettlement = settlementMapper.selectOne(wrapper);

        int seq = 1;
        if (lastSettlement != null) {
            String lastNo = lastSettlement.getSettlementNo();
            String lastSeq = lastNo.substring(lastNo.lastIndexOf("-") + 1);
            try {
                seq = Integer.parseInt(lastSeq) + 1;
            } catch (NumberFormatException ignored) {
            }
        }

        return String.format("OTSS-%s-%04d", dateStr, seq);
    }

    /**
     * 填充基本信息
     * 优先从 settlementRows 中获取结算周期等信息
     */
    private void fillBasicInfo(OutsourceTransportSettlement settlement, OutsourceSettlementSaveRequest request) {
        settlement.setContractId(request.getContractId());
        settlement.setContractNo(request.getContractNo());
        settlement.setCarrierName(request.getCarrierName());
        settlement.setContactPerson(request.getContactPerson());
        settlement.setContactPhone(request.getContactPhone());
        settlement.setBankName(request.getBankName());
        settlement.setCardNumber(request.getCardNumber());
        settlement.setAccountName(request.getAccountName());
        settlement.setRemark(request.getRemark());

        // 优先从 settlementRows 中获取结算周期等信息
        List<OutsourceSettlementCreateRequest.SettlementRowDTO> rows = request.getSettlementRows();
        if (rows != null && !rows.isEmpty()) {
            OutsourceSettlementCreateRequest.SettlementRowDTO firstRow = rows.get(0);
            settlement.setSettlementPeriodStart(firstRow.getSettlementPeriodStart());
            settlement.setSettlementPeriodEnd(firstRow.getSettlementPeriodEnd());
            settlement.setSettlementMethod(firstRow.getSettlementMethod());
            settlement.setUnit(firstRow.getUnit());
            settlement.setSettlementPrice(firstRow.getSettlementPrice());
        } else {
            // 备用：从顶层字段获取
            settlement.setSettlementPeriodStart(request.getSettlementPeriodStart());
            settlement.setSettlementPeriodEnd(request.getSettlementPeriodEnd());
            settlement.setSettlementMethod(request.getSettlementMethod());
            settlement.setUnit(request.getUnit());
            settlement.setSettlementPrice(request.getSettlementPrice());
        }
    }

    /**
     * 设置结算数量和金额
     * 优先从 settlementRows 中获取，备用从顶层字段获取
     */
    private void calculateSettlementAmount(OutsourceTransportSettlement settlement, OutsourceSettlementSaveRequest request) {
        List<OutsourceSettlementCreateRequest.SlipDTO> slips = request.getSlips();
        if (slips == null || slips.isEmpty()) {
            settlement.setSettlementQuantity(BigDecimal.ZERO);
            settlement.setSettlementAmount(BigDecimal.ZERO);
            return;
        }

        // 优先从 settlementRows 中获取结算数量和金额
        List<OutsourceSettlementCreateRequest.SettlementRowDTO> rows = request.getSettlementRows();
        if (rows != null && !rows.isEmpty()) {
            OutsourceSettlementCreateRequest.SettlementRowDTO firstRow = rows.get(0);
            settlement.setSettlementQuantity(firstRow.getSettlementQuantity() != null ? firstRow.getSettlementQuantity() : BigDecimal.ZERO);
            settlement.setSettlementAmount(firstRow.getSettlementAmount() != null ? firstRow.getSettlementAmount() : BigDecimal.ZERO);
        } else {
            // 备用：从顶层字段获取
            settlement.setSettlementQuantity(request.getSettlementQuantity() != null ? request.getSettlementQuantity() : BigDecimal.ZERO);
            settlement.setSettlementAmount(request.getSettlementAmount() != null ? request.getSettlementAmount() : BigDecimal.ZERO);
        }
    }

    /**
     * 设置结算数量和金额（用于 OutsourceSettlementCreateRequest）
     * 直接使用前端传递的值
     */
    private void calculateSettlementAmountForCreate(OutsourceTransportSettlement settlement, OutsourceSettlementCreateRequest request) {
        if (request.getSlips() == null || request.getSlips().isEmpty()) {
            settlement.setSettlementQuantity(BigDecimal.ZERO);
            settlement.setSettlementAmount(BigDecimal.ZERO);
            return;
        }

        // 直接使用结算周期行中前端传递的值
        List<OutsourceSettlementCreateRequest.SettlementRowDTO> rows = request.getSettlementRows();
        if (rows != null && !rows.isEmpty()) {
            OutsourceSettlementCreateRequest.SettlementRowDTO firstRow = rows.get(0);
            settlement.setSettlementQuantity(firstRow.getSettlementQuantity() != null ? firstRow.getSettlementQuantity() : BigDecimal.ZERO);
            settlement.setSettlementAmount(firstRow.getSettlementAmount() != null ? firstRow.getSettlementAmount() : BigDecimal.ZERO);
        } else {
            settlement.setSettlementQuantity(BigDecimal.ZERO);
            settlement.setSettlementAmount(BigDecimal.ZERO);
        }
    }

    /**
     * 保存结算单与总磅单的关联关系
     */
    private void saveSlipRelations(Integer settlementId, String settlementNo, List<OutsourceSettlementCreateRequest.SlipDTO> slips) {
        List<OutsourceTransportSettlementSlip> relations = new ArrayList<>();
        for (OutsourceSettlementCreateRequest.SlipDTO slipDTO : slips) {
            OutsourceTransportSettlementSlip relation = new OutsourceTransportSettlementSlip();
            relation.setSettlementId(settlementId);
            relation.setSettlementNo(settlementNo);
            relation.setSlipId(slipDTO.getSlipId());
            relation.setSlipCode(slipDTO.getSlipCode());
            relation.setVersion(0);
            relations.add(relation);
        }
        slipRelationMapper.batchInsert(relations);
    }

    /**
     * 保存价外服务关联关系
     *
     * @param settlementId 结算单ID
     * @param services 价外服务列表
     */
    private void saveOutOfScopeServices(Integer settlementId, List<OutsourceSettlementCreateRequest.OutOfScopeServiceDTO> services) {
        // 先删除旧的价外服务关联
        outOfScopeServiceMapper.deleteByBusiness("SETTLEMENT", settlementId);

        if (services == null || services.isEmpty()) {
            return;
        }

        // 插入新的价外服务记录
        List<OutOfScopeService> entities = new ArrayList<>();
        for (OutsourceSettlementCreateRequest.OutOfScopeServiceDTO dto : services) {
            OutOfScopeService entity = new OutOfScopeService();
            entity.setBusinessType("SETTLEMENT");
            entity.setBusinessId(settlementId);
            entity.setProject(dto.getProject());
            entity.setSpec(dto.getSpec());
            entity.setUnit(dto.getBasicUnit());
            entity.setPlannedQuantity(dto.getPlannedQuantity());
            entity.setContractUnitPrice(dto.getContractUnitPrice());
            entity.setSettledQuantity(dto.getSettlementQuantity());
            entity.setSettledUnitPrice(dto.getUnitPrice());
            entity.setSettledAmount(dto.getAmount());
            entity.setStatus("ACTIVE");
            entity.setRemark(dto.getRemark());
            entity.setLocked(false);
            entity.setCreatedBy(SecurityUtil.getCurrentUserId());
            entities.add(entity);
        }

        for (OutOfScopeService entity : entities) {
            outOfScopeServiceMapper.insert(entity);
        }

        log.info("保存价外服务成功，settlementId={}, count={}", settlementId, entities.size());
    }

    // 删除重复方法，调用上面的 saveSlipRelations 即可

    /**
     * 转换为响应对象（用于列表查询）
     */
    private OutsourceSettlementResponse convertToResponse(OutsourceTransportSettlement settlement) {
        OutsourceSettlementResponse response = new OutsourceSettlementResponse();

        response.setSettlementId(settlement.getSettlementId());
        response.setSettlementNo(settlement.getSettlementNo());
        response.setContractId(settlement.getContractId());
        response.setContractNo(settlement.getContractNo());
        response.setCarrierName(settlement.getCarrierName());
        response.setContactPerson(settlement.getContactPerson());
        response.setContactPhone(settlement.getContactPhone());
        response.setBankName(settlement.getBankName());
        response.setCardNumber(settlement.getCardNumber());
        response.setAccountName(settlement.getAccountName());
        response.setSettlementPeriodStart(settlement.getSettlementPeriodStart());
        response.setSettlementPeriodEnd(settlement.getSettlementPeriodEnd());
        response.setSettlementMethod(settlement.getSettlementMethod());
        response.setUnit(settlement.getUnit());
        response.setSettlementQuantity(settlement.getSettlementQuantity());
        response.setSettlementPrice(settlement.getSettlementPrice());
        response.setSettlementAmount(settlement.getSettlementAmount());
        response.setPaidAmount(settlement.getPaidAmount());

        // 计算未付款金额
        if (settlement.getSettlementAmount() != null && settlement.getPaidAmount() != null) {
            response.setUnpaidAmount(settlement.getSettlementAmount().subtract(settlement.getPaidAmount()));
        } else {
            response.setUnpaidAmount(settlement.getSettlementAmount());
        }

        // 查询关联的总磅单列表
        List<OutsourceTransportSettlementSlip> slips = slipRelationMapper.selectBySettlementId(settlement.getSettlementId());
        response.setSlipIds(slips.stream().map(OutsourceTransportSettlementSlip::getSlipId).collect(Collectors.toList()));
        response.setSlips(slips.stream().map(slip -> {
            OutsourceTransportSettlementSlipVO vo = new OutsourceTransportSettlementSlipVO();
            vo.setRelationId(slip.getRelationId());
            vo.setSettlementId(slip.getSettlementId());
            vo.setSettlementNo(slip.getSettlementNo());
            vo.setSlipId(slip.getSlipId());
            vo.setSlipCode(slip.getSlipCode());
            vo.setCreateTime(slip.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));

        response.setStatus(settlement.getStatus());
        response.setAuditOpinion(settlement.getAuditOpinion());
        response.setAuditorId(settlement.getAuditorId());
        response.setAuditorName(settlement.getAuditorName());
        response.setAuditTime(settlement.getAuditTime());
        response.setPaymentDirection(settlement.getPaymentDirection());
        response.setRemark(settlement.getRemark());
        response.setCreatorId(settlement.getCreatorId());
        response.setCreatorName(settlement.getCreatorName());
        response.setCreateTime(settlement.getCreateTime());
        response.setUpdateTime(settlement.getUpdateTime());
        response.setUpdaterId(settlement.getUpdaterId());
        response.setLocked(settlement.getLocked());
        response.setLockTime(settlement.getLockTime());
        response.setLockUserId(settlement.getLockUserId());
        response.setLockReason(settlement.getLockReason());
        response.setDeleted(settlement.getDeleted());
        response.setVersion(settlement.getVersion());

        return response;
    }

    // ==================== 收付款拆分新增方法 ====================

    /**
     * 新增结算单（支持收付款拆分）
     *
     * 逻辑说明：
     * - 空数据允许，只验证数据库必填字段
     * - 若包含收付款混合明细会自动拆分，生成两个结算单
     * - 价外服务归属收款单
     *
     * @param request 新增请求
     * @param operatorUserId 操作人ID
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceSettlementCreateResultDTO createWithSplit(OutsourceSettlementCreateRequest request, Integer operatorUserId) {
        log.info("新增委外运输结算单（收付款拆分），operatorUserId={}", operatorUserId);

        OutsourceSettlementCreateResultDTO result = new OutsourceSettlementCreateResultDTO();

        // 解析结算周期行
        List<OutsourceSettlementCreateRequest.SettlementRowDTO> rows = request.getSettlementRows();
        List<OutsourceSettlementCreateRequest.SettlementRowDTO> receivableRows = new ArrayList<>();
        List<OutsourceSettlementCreateRequest.SettlementRowDTO> payableRows = new ArrayList<>();

        if (rows != null && !rows.isEmpty()) {
            for (OutsourceSettlementCreateRequest.SettlementRowDTO row : rows) {
                if ("RECEIVABLE".equals(row.getSettlementType())) {
                    receivableRows.add(row);
                } else if ("PAYABLE".equals(row.getSettlementType())) {
                    payableRows.add(row);
                }
            }
        }

        boolean hasReceivable = !receivableRows.isEmpty();
        boolean hasPayable = !payableRows.isEmpty();
        boolean hasOutOfScopeServices = request.getOutOfScopeServices() != null && !request.getOutOfScopeServices().isEmpty();

        // 判断是否需要拆分：有收款行+有付款行，或者有价外服务
        boolean shouldSplit = (hasReceivable && hasPayable) || hasOutOfScopeServices;
        result.setSplit(shouldSplit);

        // 获取价外服务列表（只归属收款单）
        List<OutsourceSettlementCreateRequest.OutOfScopeServiceDTO> outOfScopeServices = request.getOutOfScopeServices();

        // 计算总金额（收款行+价外服务 vs 付款行）
        BigDecimal receivableAmount = calculateRowsAmount(receivableRows);
        BigDecimal payableAmount = calculateRowsAmount(payableRows);
        BigDecimal outOfScopeAmount = calculateOutOfScopeAmount(outOfScopeServices);

        // 创建收款结算单
        if (hasReceivable) {
            Integer receivableSettlementId = createSingleSettlement(
                    request, receivableRows, "RECEIVABLE",
                    shouldSplit ? "-R" : "",
                    receivableAmount.add(outOfScopeAmount),
                    outOfScopeServices,
                    operatorUserId
            );
            result.setReceivableSettlementId(receivableSettlementId);
            result.setSettlementId(receivableSettlementId);
            result.setBusinessSeq(receivableSettlementId);
        } else if (hasOutOfScopeServices || !hasPayable) {
            // 空数据或有价外服务无收款行时：创建空白收款单
            Integer receivableSettlementId = createSingleSettlement(
                    request, Collections.emptyList(), "RECEIVABLE",
                    shouldSplit ? "-R" : "",
                    outOfScopeAmount,
                    outOfScopeServices,
                    operatorUserId
            );
            result.setReceivableSettlementId(receivableSettlementId);
            result.setSettlementId(receivableSettlementId);
            result.setBusinessSeq(receivableSettlementId);
        }

        // 创建付款结算单
        if (hasPayable) {
            Integer payableSettlementId = createSingleSettlement(
                    request, payableRows, "PAYABLE",
                    shouldSplit ? "-P" : "",
                    payableAmount,
                    null, // 付款单不含价外服务
                    operatorUserId
            );
            result.setPayableSettlementId(payableSettlementId);
            if (!shouldSplit) {
                result.setSettlementId(payableSettlementId);
            }
        }

        log.info("新增委外运输结算单成功，settlementId={}, split={}, receivable={}, payable={}",
                result.getSettlementId(), result.getSplit(),
                result.getReceivableSettlementId(), result.getPayableSettlementId());

        return result;
    }

    /**
     * 创建单个结算单
     */
    private Integer createSingleSettlement(OutsourceSettlementCreateRequest request,
                                           List<OutsourceSettlementCreateRequest.SettlementRowDTO> rows,
                                           String settlementType,
                                           String codeSuffix,
                                           BigDecimal settlementAmount,
                                           List<OutsourceSettlementCreateRequest.OutOfScopeServiceDTO> outOfScopeServices,
                                           Integer operatorUserId) {
        OutsourceTransportSettlement settlement = new OutsourceTransportSettlement();

        // 生成结算单编号
        String settlementNo = generateSettlementNo() + codeSuffix;
        settlement.setSettlementNo(settlementNo);

        // 填充基本信息
        settlement.setContractId(request.getContractId());
        settlement.setContractNo(request.getContractNo());
        settlement.setCarrierName(request.getCarrierName());
        settlement.setContactPerson(request.getContactPerson());
        settlement.setContactPhone(request.getContactPhone());
        settlement.setBankName(request.getBankName());
        settlement.setCardNumber(request.getCardNumber());
        settlement.setAccountName(request.getAccountName());
        settlement.setRemark(request.getRemark());
        settlement.setPaymentDirection(settlementType);

        // 从结算周期行中取第一条的结算信息
        if (rows != null && !rows.isEmpty()) {
            OutsourceSettlementCreateRequest.SettlementRowDTO firstRow = rows.get(0);
            settlement.setSettlementPeriodStart(firstRow.getSettlementPeriodStart());
            settlement.setSettlementPeriodEnd(firstRow.getSettlementPeriodEnd());
            settlement.setSettlementMethod(firstRow.getSettlementMethod());
            settlement.setUnit(firstRow.getUnit());
            settlement.setSettlementPrice(firstRow.getSettlementPrice());
            settlement.setSettlementQuantity(firstRow.getSettlementQuantity());
        }

        // 设置结算金额
        settlement.setSettlementAmount(settlementAmount != null ? settlementAmount : BigDecimal.ZERO);
        settlement.setPaidAmount(BigDecimal.ZERO);

        // 设置状态为待审核
        settlement.setStatus("待审核");
        settlement.setLocked(false);
        settlement.setDeleted(0);
        settlement.setVersion(0);

        // 设置创建人信息
        settlement.setCreatorId(operatorUserId);
        settlement.setCreatorName(SecurityUtil.getEmployeeName());
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());

        settlementMapper.insert(settlement);

        // 关联总磅单
        if (request.getSlips() != null && !request.getSlips().isEmpty()) {
            saveSlipRelations(settlement.getSettlementId(), settlement.getSettlementNo(), request.getSlips());
        }

        // 保存价外服务关联
        if (outOfScopeServices != null && !outOfScopeServices.isEmpty()) {
            saveOutOfScopeServices(settlement.getSettlementId(), outOfScopeServices);
        }

        log.info("创建单个结算单成功，settlementId={}, settlementNo={}, settlementType={}, settlementAmount={}",
                settlement.getSettlementId(), settlement.getSettlementNo(), settlementType, settlement.getSettlementAmount());

        return settlement.getSettlementId();
    }

    /**
     * 计算结算周期行的总金额
     */
    private BigDecimal calculateRowsAmount(List<OutsourceSettlementCreateRequest.SettlementRowDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OutsourceSettlementCreateRequest.SettlementRowDTO row : rows) {
            if (row.getSettlementAmount() != null) {
                total = total.add(row.getSettlementAmount());
            }
        }
        return total;
    }

    /**
     * 计算价外服务的总金额
     */
    private BigDecimal calculateOutOfScopeAmount(List<OutsourceSettlementCreateRequest.OutOfScopeServiceDTO> services) {
        if (services == null || services.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OutsourceSettlementCreateRequest.OutOfScopeServiceDTO service : services) {
            if (service.getAmount() != null) {
                total = total.add(service.getAmount());
            }
        }
        return total;
    }

    @Override
    public List<OutsourceTransportSettlementSlipVO> getSlipsBySettlementId(Integer settlementId) {
        List<OutsourceTransportSettlementSlip> slips = slipRelationMapper.selectBySettlementId(settlementId);
        return slips.stream().map(this::convertToSlipVO).collect(Collectors.toList());
    }

    /**
     * 转换为VO对象
     */
    private OutsourceTransportSettlementSlipVO convertToSlipVO(OutsourceTransportSettlementSlip slip) {
        OutsourceTransportSettlementSlipVO vo = new OutsourceTransportSettlementSlipVO();
        vo.setRelationId(slip.getRelationId());
        vo.setSettlementId(slip.getSettlementId());
        vo.setSettlementNo(slip.getSettlementNo());
        vo.setSlipId(slip.getSlipId());
        vo.setSlipCode(slip.getSlipCode());
        vo.setCreateTime(slip.getCreateTime());
        return vo;
    }

    /**
     * 批量提交审核
     * 功能描述：批量提交多个委外运输结算单进行审核，只有待审核、已驳回状态的结算单才能提交审核
     * 提交后结算单状态改为"已审核"
     * 在OA审核记录表(OA_APPROVAL_RECORD)中有记录则审核次数+1，如没有则新增一条记录
     * 来源表中文名称为"委外运输结算"
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceSettlementBatchOperationResult batchSubmitAudit(List<Integer> settlementIds, Integer operatorUserId) {
        log.info("批量提交审核，settlementIds={}, operatorUserId={}", settlementIds, operatorUserId);

        OutsourceSettlementBatchOperationResult result = new OutsourceSettlementBatchOperationResult();
        List<OutsourceSettlementBatchOperationResult.FailureItem> failures = new ArrayList<>();
        int successCount = 0;

        // 检查是否为管理员
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = authService.isAdmin(currentUserId);
        log.info("批量提交审核权限检查：currentUserId={}, isAdmin={}", currentUserId, isAdmin);

        // 获取当前用户信息
        Employee currentEmployee = employeeMapper.selectById(operatorUserId);
        String currentUserName = currentEmployee != null ? currentEmployee.getEmployeeName() : "未知";

        // 查询所有结算单
        List<OutsourceTransportSettlement> settlements = settlementMapper.selectBatchIds(settlementIds);
        Map<Integer, OutsourceTransportSettlement> settlementMap = settlements.stream()
                .collect(Collectors.toMap(OutsourceTransportSettlement::getSettlementId, Function.identity()));

        for (Integer settlementId : settlementIds) {
            OutsourceTransportSettlement settlement = settlementMap.get(settlementId);

            // 验证是否存在
            if (settlement == null) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, null, "结算单记录不存在"));
                continue;
            }

            // 验证状态：仅待审核、已驳回状态可以提交审核
            String currentStatus = settlement.getStatus();
            if (!"待审核".equals(currentStatus) && !"已驳回".equals(currentStatus)) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, settlement.getSettlementNo(),
                        "当前状态为【" + currentStatus + "】，仅待审核、已驳回状态可以提交审核"));
                continue;
            }

            // 非管理员需要校验制单人权限
            if (!isAdmin) {
                // 校验制单人权限：只有制单人可以提交自己创建的结算单
                if (settlement.getCreatorId() != null && !settlement.getCreatorId().equals(currentUserId)) {
                    failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                            settlementId, settlement.getSettlementNo(),
                            "不是制单人，无权提交审核该结算单"));
                    continue;
                }
            }

            try {
                OaApprovalRecord approvalRecord;
                String actionType;

                // 查询OA表中是否有已撤回的记录
                OaApprovalRecord withdrawnRecord = oaApprovalRecordService.findWithdrawnBySource("OUTSOURCE_TRANSPORT_SETTLEMENT", settlementId);
                if (withdrawnRecord != null) {
                    // 有已撤回记录：重新激活该记录，状态改为待审核，审核次数+1
                    approvalRecord = oaApprovalRecordService.reactivateWithdrawnRecord(
                            "OUTSOURCE_TRANSPORT_SETTLEMENT",
                            settlementId,
                            operatorUserId,
                            currentUserName
                    );
                    actionType = "重新激活（已撤回）";
                } else if ("已驳回".equals(currentStatus)) {
                    // 已驳回状态：调用reactivateRejectedRecord，审核次数+1
                    approvalRecord = oaApprovalRecordService.reactivateRejectedRecord(
                            "OUTSOURCE_TRANSPORT_SETTLEMENT",
                            settlementId,
                            operatorUserId,
                            currentUserName
                    );
                    actionType = "重新提交（已驳回）";
                } else {
                    // 无已撤回/已驳回记录：检查是否有待审核记录
                    OaApprovalRecord existingRecord = oaApprovalRecordService.findPendingBySource("OUTSOURCE_TRANSPORT_SETTLEMENT", settlementId);
                    if (existingRecord != null) {
                        failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                                settlementId, settlement.getSettlementNo(), "该结算单已存在待审核的OA审批记录"));
                        continue;
                    }

                    // 新建OA审批记录，来源表中文名称为"委外运输结算"
                    String approvalTitle = String.format("委外运输结算：%s",
                            settlement.getSettlementNo() != null ? settlement.getSettlementNo() : "OTSS" + settlementId);
                    approvalRecord = oaApprovalRecordService.submit(
                            "OUTSOURCE_TRANSPORT_SETTLEMENT",
                            settlementId,
                            "委外运输结算",
                            settlement.getSettlementNo(),
                            approvalTitle,
                            operatorUserId,
                            currentUserName
                    );
                    actionType = "新建";
                }

                // 更新结算单状态为审核中
                settlement.setStatus("审核中");
                settlement.setUpdateTime(LocalDateTime.now());
                settlement.setUpdaterId(operatorUserId);
                settlementMapper.updateById(settlement);

                successCount++;
                log.info("委外运输结算单提交OA审核{}成功：settlementId={}, approvalRecordId={}, actionType={}",
                        actionType, settlementId, approvalRecord.getApprovalRecordId(), actionType);

            } catch (Exception e) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, settlement.getSettlementNo(), "提交审核失败：" + e.getMessage()));
                log.error("委外运输结算单提交OA审核失败：settlementId={}", settlementId, e);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);

        log.info("批量提交审核完成，成功={}，失败={}", successCount, failures.size());
        return result;
    }

    /**
     * 批量撤回审核
     * 功能描述：批量撤回多个审核中的委外运输结算单，只有审核中状态的结算单才能撤回
     * 撤回后结算单状态改为"待审核"
     * 在OA审核记录表(OA_APPROVAL_RECORD)中将审核状态改为"已撤回"，审核次数-1，最低为0
     * 接口地址：POST /api/outsource-settlement/batch/cancel-audit
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceSettlementBatchOperationResult batchCancelAudit(List<Integer> settlementIds, Integer operatorUserId) {
        log.info("批量撤回审核，settlementIds={}, operatorUserId={}", settlementIds, operatorUserId);

        OutsourceSettlementBatchOperationResult result = new OutsourceSettlementBatchOperationResult();
        List<OutsourceSettlementBatchOperationResult.FailureItem> failures = new ArrayList<>();
        int successCount = 0;

        // 检查是否为管理员
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = authService.isAdmin(currentUserId);
        log.info("批量撤回审核权限检查：currentUserId={}, isAdmin={}", currentUserId, isAdmin);

        // 查询所有结算单
        List<OutsourceTransportSettlement> settlements = settlementMapper.selectBatchIds(settlementIds);
        Map<Integer, OutsourceTransportSettlement> settlementMap = settlements.stream()
                .collect(Collectors.toMap(OutsourceTransportSettlement::getSettlementId, Function.identity()));

        for (Integer settlementId : settlementIds) {
            OutsourceTransportSettlement settlement = settlementMap.get(settlementId);

            // 验证是否存在
            if (settlement == null) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, null, "结算单记录不存在"));
                continue;
            }

            // 验证状态：仅审核中状态可以撤回
            String currentStatus = settlement.getStatus();
            if (!"审核中".equals(currentStatus)) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, settlement.getSettlementNo(),
                        "当前状态为【" + currentStatus + "】，仅审核中状态可以撤回"));
                continue;
            }

            // 非管理员需要校验制单人权限
            if (!isAdmin) {
                // 校验制单人权限：只有制单人可以撤回自己创建的结算单
                if (settlement.getCreatorId() != null && !settlement.getCreatorId().equals(currentUserId)) {
                    failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                            settlementId, settlement.getSettlementNo(),
                            "不是制单人，无权撤回该结算单"));
                    continue;
                }
            }

            try {
                // 查找最新的OA审批记录并撤回
                OaApprovalRecord latestRecord = oaApprovalRecordService.findLatestBySource("OUTSOURCE_TRANSPORT_SETTLEMENT", settlementId);
                if (latestRecord != null) {
                    oaApprovalRecordService.cancel(
                            latestRecord.getApprovalRecordId(),
                            "OUTSOURCE_TRANSPORT_SETTLEMENT",
                            settlementId,
                            operatorUserId,
                            "委外运输结算单批量撤回"
                    );
                }

                // 更新结算单状态为待审核
                settlement.setStatus("待审核");
                settlement.setUpdateTime(LocalDateTime.now());
                settlement.setUpdaterId(operatorUserId);
                settlementMapper.updateById(settlement);

                successCount++;
                log.info("委外运输结算单撤回审核成功：settlementId={}", settlementId);

            } catch (Exception e) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, settlement.getSettlementNo(), "撤回审核失败：" + e.getMessage()));
                log.error("委外运输结算单撤回审核失败：settlementId={}", settlementId, e);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);

        log.info("批量撤回审核完成，成功={}，失败={}", successCount, failures.size());
        return result;
    }

    /**
     * 批量删除结算单
     * 功能描述：批量删除多个委外运输结算单，只有待审核、已驳回状态的结算单才能删除
     * 接口地址：POST /api/outsource-settlement/batch/delete
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutsourceSettlementBatchOperationResult batchDelete(List<Integer> settlementIds, Integer operatorUserId) {
        log.info("批量删除结算单，settlementIds={}, operatorUserId={}", settlementIds, operatorUserId);

        OutsourceSettlementBatchOperationResult result = new OutsourceSettlementBatchOperationResult();
        List<OutsourceSettlementBatchOperationResult.FailureItem> failures = new ArrayList<>();
        int successCount = 0;

        // 查询所有结算单
        List<OutsourceTransportSettlement> settlements = settlementMapper.selectBatchIds(settlementIds);
        Map<Integer, OutsourceTransportSettlement> settlementMap = settlements.stream()
                .collect(Collectors.toMap(OutsourceTransportSettlement::getSettlementId, Function.identity()));

        for (Integer settlementId : settlementIds) {
            OutsourceTransportSettlement settlement = settlementMap.get(settlementId);

            // 验证是否存在
            if (settlement == null) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, null, "结算单记录不存在"));
                continue;
            }

            // 验证状态：仅待审核、已驳回状态可以删除
            String currentStatus = settlement.getStatus();
            if (!"待审核".equals(currentStatus) && !"已驳回".equals(currentStatus)) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, settlement.getSettlementNo(),
                        "当前状态为【" + currentStatus + "】，仅待审核、已驳回状态可以删除"));
                continue;
            }

            try {
                // 逻辑删除
                settlement.setDeleted(1);
                settlement.setUpdateTime(LocalDateTime.now());
                settlement.setUpdaterId(operatorUserId);
                settlementMapper.updateById(settlement);

                successCount++;
                log.info("委外运输结算单删除成功：settlementId={}", settlementId);

            } catch (Exception e) {
                failures.add(new OutsourceSettlementBatchOperationResult.FailureItem(
                        settlementId, settlement.getSettlementNo(), "删除失败：" + e.getMessage()));
                log.error("委外运输结算单删除失败：settlementId={}", settlementId, e);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);

        log.info("批量删除完成，成功={}，失败={}", successCount, failures.size());
        return result;
    }

    /**
     * 审核结算单
     * 功能描述：审核通过或驳回委外运输结算单
     * 审核意见必填，只有审核中状态可审核
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSettlement(Integer settlementId, String auditResult, String auditOpinion, Integer auditorId, String auditorName) {
        log.info("审核委外运输结算单，settlementId={}, auditResult={}, auditorId={}", settlementId, auditResult, auditorId);

        OutsourceTransportSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算单记录不存在");
        }

        // 验证状态：仅审核中状态可以审核
        if (!"审核中".equals(settlement.getStatus())) {
            throw new BusinessException("当前状态为【" + settlement.getStatus() + "】，仅审核中状态可以审核");
        }

        // 验证审核意见必填
        if (!StringUtils.hasText(auditOpinion)) {
            throw new BusinessException("审核意见不能为空");
        }

        // 验证审核结果
        if (!"approved".equals(auditResult) && !"rejected".equals(auditResult)) {
            throw new BusinessException("审核结果无效，仅支持 approved（通过）或 rejected（驳回）");
        }

        // 更新结算单状态和审核信息
        if ("approved".equals(auditResult)) {
            settlement.setStatus("已审核");
        } else {
            settlement.setStatus("已驳回");
        }
        settlement.setAuditOpinion(auditOpinion);
        settlement.setAuditorId(auditorId);
        settlement.setAuditorName(auditorName);
        settlement.setAuditTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());
        settlementMapper.updateById(settlement);

        // 更新OA审批记录状态
        OaApprovalRecord latestRecord = oaApprovalRecordService.findLatestBySource("OUTSOURCE_TRANSPORT_SETTLEMENT", settlementId);
        if (latestRecord != null) {
            String result = "approved".equals(auditResult) ? "通过" : "驳回";
            oaApprovalRecordService.approve(
                    latestRecord.getApprovalRecordId(),
                    "OUTSOURCE_TRANSPORT_SETTLEMENT",
                    settlementId,
                    result,
                    auditOpinion,
                    auditorId,
                    auditorName
            );
        }

        log.info("审核委外运输结算单完成，settlementId={}, 新状态={}", settlementId, settlement.getStatus());
    }

    /**
     * 取消审核（撤回）
     * 功能描述：撤回审核中的结算单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAudit(Integer settlementId, Integer operatorUserId) {
        log.info("取消审核，settlementId={}, operatorUserId={}", settlementId, operatorUserId);

        OutsourceTransportSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException("结算单记录不存在");
        }

        // 验证状态：仅审核中状态可以取消审核
        if (!"审核中".equals(settlement.getStatus())) {
            throw new BusinessException("当前状态为【" + settlement.getStatus() + "】，仅审核中状态可以取消审核");
        }

        // 查找最新的OA审批记录并撤回
        OaApprovalRecord latestRecord = oaApprovalRecordService.findLatestBySource("OUTSOURCE_TRANSPORT_SETTLEMENT", settlementId);
        if (latestRecord != null) {
            oaApprovalRecordService.cancel(
                    latestRecord.getApprovalRecordId(),
                    "OUTSOURCE_TRANSPORT_SETTLEMENT",
                    settlementId,
                    operatorUserId,
                    "委外运输结算单取消审核"
            );
        }

        // 更新结算单状态为待审核
        settlement.setStatus("待审核");
        settlement.setUpdateTime(LocalDateTime.now());
        settlement.setUpdaterId(operatorUserId);
        settlementMapper.updateById(settlement);

        log.info("取消审核完成，settlementId={}, 新状态=待审核", settlementId);
    }
}
