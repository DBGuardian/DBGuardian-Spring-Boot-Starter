package com.erp.service.transport.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.transport.dto.*;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.transport.TransportContract;
import com.erp.entity.transport.TransportContractVehicle;
import com.erp.mapper.transport.TransportContractMapper;
import com.erp.mapper.transport.TransportContractVehicleMapper;
import com.erp.mapper.transport.VehicleMapper;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.entity.transport.OutsourceTransportSettlement;
import com.erp.mapper.transport.OutsourceTransportSettlementMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.entity.system.Permission;
import com.erp.service.auth.AuthService;
import com.erp.service.oa.OaApprovalRecordService;
import com.erp.service.transport.TransportContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 运输合同 Service 实现
 */
@Slf4j
@Service
public class TransportContractServiceImpl implements TransportContractService {

    private static final String PAGE_CODE = "合同管理:委托运输合同:页面";

    @Autowired
    private TransportContractMapper transportContractMapper;

    @Autowired
    private TransportContractVehicleMapper contractVehicleMapper;

    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private OaApprovalRecordService oaApprovalRecordService;

    @Autowired
    private com.erp.mapper.oa.OaApprovalRecordMapper oaApprovalRecordMapper;

    @Autowired
    private OutsourceTransportSettlementMapper outsourceTransportSettlementMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    /** 单号生成锁（生产环境建议使用Redis分布式锁） */
    private final ReentrantLock contractNoLock = new ReentrantLock();

    /** 分页参数限制 */
    private static final long MAX_PAGE_SIZE = 100;
    private static final long DEFAULT_PAGE_SIZE = 10;

    /** 状态常量 */
    private static final String STATUS_DRAFT = "待审核";
    private static final String STATUS_REVIEWING = "审核中";
    private static final String STATUS_EXECUTING = "执行中";
    private static final String STATUS_REJECTED = "已驳回";
    private static final String STATUS_COMPLETED = "已完结";

    @Override
    public IPage<TransportContractPageResponse> getPage(TransportContractPageRequest request) {
        // 优化：分页参数范围校验
        long current = request.getCurrent() != null && request.getCurrent() > 0 ? request.getCurrent() : 1;
        long size = request.getSize() != null && request.getSize() > 0
            ? Math.min(request.getSize(), MAX_PAGE_SIZE)
            : DEFAULT_PAGE_SIZE;

        // 使用 ViewScopeHelper 解析视图范围
        String viewScope = ViewScopeHelper.resolveViewScope(PAGE_CODE, request.getViewScope());

        // 获取当前用户ID
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        // SELF 模式时添加创建人过滤条件，ALL 模式时不限制
        Integer creatorFilter = ViewScopeHelper.isSelfScope(viewScope) ? currentUserId : null;

        // 优化：合并查询，使用单次DB查询获取总数和数据
        List<TransportContract> records = transportContractMapper.selectPageList(
                request.getContractNo(),
                request.getCarrierName(),
                request.getSigningType(),
                request.getSettlementMethod(),
                request.getStatus(),
                request.getSignTimeStart(),
                request.getSignTimeEnd(),
                request.getSortField(),
                request.getSortOrder(),
                creatorFilter,
                (current - 1) * size,
                size
        );

        long total = transportContractMapper.selectPageCount(
                request.getContractNo(),
                request.getCarrierName(),
                request.getSigningType(),
                request.getSettlementMethod(),
                request.getStatus(),
                request.getSignTimeStart(),
                request.getSignTimeEnd(),
                creatorFilter
        );

        Page<TransportContractPageResponse> page = new Page<>(current, size, total);
        page.setRecords(records.stream().map(this::convertToPageResponse).collect(Collectors.toList()));
        return page;
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
        EmployeePermission permission = getEmployeePagePermission(currentUserId, PAGE_CODE);
        if (permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
            if (!Objects.equals(creatorId, currentUserId)) {
                throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(),
                        "您只能操作自己创建的运输合同");
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

    @Override
    public TransportContractDetailResponse getDetail(Integer contractId) {
        TransportContract contract = findByIdOrThrow(contractId);
        return convertToDetailResponse(contract);
    }

    @Override
    @Transactional
    public Integer create(TransportContractSaveRequest request) {
        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("联系人电话", request.getContactPhone());
        validatePhoneFormat("乙方联系电话", request.getPartyBContactPhone());

        TransportContract contract = new TransportContract();
        BeanUtils.copyProperties(request, contract);
        // 手动转换日期类型（BeanUtils无法自动转换String到LocalDate）
        contract.setSignTime(parseLocalDate(request.getSignTime()));
        contract.setValidFrom(parseLocalDate(request.getValidFrom()));
        contract.setValidTo(parseLocalDate(request.getValidTo()));

        // 优化：使用锁保证单号唯一性
        contractNoLock.lock();
        try {
            contract.setContractNo(generateContractNo());
        } finally {
            contractNoLock.unlock();
        }

        // 使用状态常量
        contract.setStatus(STATUS_DRAFT);

        // 设置创建人信息
        contract.setCreatorId(SecurityUtil.getCurrentUserId());
        contract.setCreatorName(SecurityUtil.getEmployeeName());

        transportContractMapper.insert(contract);

        // 同步处理车辆关联（新增模式：只添加新的，不重复添加已关联的）
        syncVehicleRelations(contract.getContractId(), request.getVehicleIds(), SecurityUtil.getCurrentUserId(), false);

        return contract.getContractId();
    }

    @Override
    @Transactional
    public void update(Integer contractId, TransportContractSaveRequest request) {
        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("联系人电话", request.getContactPhone());
        validatePhoneFormat("乙方联系电话", request.getPartyBContactPhone());

        TransportContract contract = findByIdOrThrow(contractId);

        // 使用状态常量校验
        if (!STATUS_DRAFT.equals(contract.getStatus()) && !STATUS_REJECTED.equals(contract.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_NOT_ALLOWED.getCode(), "当前状态不允许编辑");
        }

        // 操作范围校验（operateScope）
        validateOperateScope(contractId, contract.getCreatorId());

        BeanUtils.copyProperties(request, contract, "contractId", "contractNo", "creatorId", "creatorName", "createTime");
        // 手动转换日期类型
        contract.setSignTime(parseLocalDate(request.getSignTime()));
        contract.setValidFrom(parseLocalDate(request.getValidFrom()));
        contract.setValidTo(parseLocalDate(request.getValidTo()));

        transportContractMapper.updateById(contract);

        // 同步处理车辆关联（全量替换：先删除旧的，再添加新的）
        syncVehicleRelations(contractId, request.getVehicleIds(), SecurityUtil.getCurrentUserId(), true);
    }

    @Override
    @Transactional
    public void audit(Integer contractId, TransportContractAuditRequest request) {
        TransportContract contract = findByIdOrThrow(contractId);

        // 审核时状态必须是"审核中"
        if (!STATUS_REVIEWING.equals(contract.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_NOT_ALLOWED.getCode(), "只有审核中的合同才能审核通过");
        }

        // 审核通过时更新所有字段
        BeanUtils.copyProperties(request, contract, "contractId", "contractNo", "creatorId", "creatorName", "createTime", "deleted", "status");
        // 手动转换日期类型
        contract.setSignTime(parseLocalDate(request.getSignTime()));
        contract.setValidFrom(parseLocalDate(request.getValidFrom()));
        contract.setValidTo(parseLocalDate(request.getValidTo()));

        // 设置审核人信息
        contract.setStatus(STATUS_EXECUTING);
        contract.setAuditorId(SecurityUtil.getCurrentUserId());
        contract.setAuditorName(SecurityUtil.getEmployeeName());
        contract.setAuditTime(LocalDateTime.now());

        transportContractMapper.updateById(contract);

        // 同步处理车辆关联（全量替换）
        syncVehicleRelations(contractId, request.getVehicleIds(), SecurityUtil.getCurrentUserId(), true);
    }

    /**
     * 同步合同-车辆关联关系（全量替换）
     * @param isReplace 是否全量替换，true时先删除旧关联再添加新关联
     */
    private void syncVehicleRelations(Integer contractId, List<Integer> vehicleIds, Integer userId, boolean isReplace) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            // 如果是全量替换且没有新车辆，清空所有关联
            if (isReplace) {
                contractVehicleMapper.deleteByContractId(contractId);
            }
            return;
        }

        // 如果是全量替换，先删除旧关联
        if (isReplace) {
            contractVehicleMapper.deleteByContractId(contractId);
        }

        for (Integer vehicleId : vehicleIds) {
            // 校验车辆是否存在
            if (vehicleMapper.selectById(vehicleId) == null) {
                log.warn("车辆不存在，跳过关联：contractId={}, vehicleId={}", contractId, vehicleId);
                continue;
            }

            // 如果不是全量替换，检查是否已关联
            if (!isReplace) {
                Integer existId = contractVehicleMapper.checkExist(contractId, vehicleId);
                if (existId != null) {
                    log.warn("车辆已关联，跳过：contractId={}, vehicleId={}", contractId, vehicleId);
                    continue;
                }
            }

            // 创建关联记录
            TransportContractVehicle relation = new TransportContractVehicle();
            relation.setContractId(contractId);
            relation.setVehicleId(vehicleId);
            relation.setRelationTime(LocalDateTime.now());
            relation.setRelationUserId(userId);
            contractVehicleMapper.insert(relation);
        }
    }

    @Override
    @Transactional
    public void updateStatus(Integer contractId, TransportContractStatusRequest request) {
        TransportContract contract = findByIdOrThrow(contractId);

        // 操作范围校验（operateScope）
        validateOperateScope(contractId, contract.getCreatorId());

        // 状态流转校验
        validateStatusTransition(contract.getStatus(), request.getStatus());

        contract.setStatus(request.getStatus());

        // 审核通过：状态改为"已通过"，记录审核人信息
        if ("已通过".equals(request.getStatus())) {
            contract.setAuditorId(SecurityUtil.getCurrentUserId());
            contract.setAuditorName(SecurityUtil.getEmployeeName());
            contract.setAuditTime(LocalDateTime.now());
            // 审核意见存入运输合同表
            if (request.getAuditOpinion() != null && !request.getAuditOpinion().trim().isEmpty()) {
                contract.setAuditOpinion(request.getAuditOpinion());
            }
        }

        // 审核驳回：状态改为"已驳回"
        if (STATUS_REJECTED.equals(request.getStatus())) {
            contract.setAuditOpinion(request.getAuditOpinion());
        }

        transportContractMapper.updateById(contract);

        // 同步更新OA审核记录状态
        syncOaApprovalRecord(contractId, request.getStatus());
    }

    @Override
    @Transactional
    public void delete(Integer contractId) {
        TransportContract contract = findByIdOrThrow(contractId);

        // 使用状态常量校验
        if (!STATUS_DRAFT.equals(contract.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_NOT_ALLOWED.getCode(), "仅待审核状态的合同可删除");
        }

        // 操作范围校验（operateScope）
        validateOperateScope(contractId, contract.getCreatorId());

        contract.setDeleted(1);
        transportContractMapper.updateById(contract);
    }

    @Override
    @Transactional
    public TransportContractBatchStatusResponse batchUpdateStatus(List<Integer> contractIds, String status) {
        TransportContractBatchStatusResponse response = new TransportContractBatchStatusResponse();
        List<Integer> successIds = new java.util.ArrayList<>();
        List<Integer> failedIds = new java.util.ArrayList<>();
        Map<Integer, String> failedReasons = new java.util.HashMap<>();

        if (contractIds == null || contractIds.isEmpty()) {
            response.setSuccessIds(successIds);
            response.setFailedIds(failedIds);
            response.setFailedReasons(failedReasons);
            response.setAllSuccess(true);
            return response;
        }

        // operateScope预检查：operateScope=SELF时，仅允许操作自己创建的合同
        Integer currentUserId = null;
        String currentUserName = "未知";
        boolean isAdmin = false;
        EmployeePermission permission = null;
        try {
            currentUserId = SecurityUtil.getCurrentUserId();
            isAdmin = currentUserId != null && authService.isAdmin(currentUserId);
            if (currentUserId != null) {
                currentUserName = SecurityUtil.getEmployeeName();
            }
            if (!isAdmin) {
                permission = getEmployeePagePermission(currentUserId, PAGE_CODE);
            }
        } catch (Exception e) {
            log.warn("获取当前用户信息失败", e);
        }

        // 提交审核到OA：仅允许 待审核/已驳回 状态的合同提交（撤回后业务表状态为待审核）
        if ("审核中".equals(status)) {
            for (Integer contractId : contractIds) {
                try {
                    TransportContract contract = transportContractMapper.selectById(contractId);
                    if (contract == null || contract.getDeleted() == 1) {
                        failedIds.add(contractId);
                        failedReasons.put(contractId, "合同不存在或已删除");
                        continue;
                    }

                    // operateScope校验
                    if (!isAdmin && permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
                        if (!Objects.equals(contract.getCreatorId(), currentUserId)) {
                            failedIds.add(contractId);
                            failedReasons.put(contractId, "您只能操作自己创建的合同");
                            continue;
                        }
                    }

                    // 状态校验：仅待审核、已驳回状态可以提交审核
                    if (!STATUS_DRAFT.equals(contract.getStatus()) && !STATUS_REJECTED.equals(contract.getStatus())) {
                        failedIds.add(contractId);
                        failedReasons.put(contractId, "当前状态【" + contract.getStatus() + "】不允许提交审核，仅待审核、已驳回状态可以提交");
                        continue;
                    }

                    // 更新合同状态为审核中
                    contract.setStatus(STATUS_REVIEWING);
                    transportContractMapper.updateById(contract);

                    // 在OA审核记录表操作：查询是否有已撤回的记录
                    String sourceTable = "TRANSPORT_CONTRACT";

                    // 查询OA表中是否有已撤回的记录
                    OaApprovalRecord withdrawnRecord = oaApprovalRecordService.findWithdrawnBySource(sourceTable, contractId);
                    if (withdrawnRecord != null) {
                        // 有已撤回记录：重新激活该记录，状态改为待审核，审核次数+1
                        oaApprovalRecordService.reactivateWithdrawnRecord(sourceTable, contractId, currentUserId, currentUserName);
                        log.info("运输合同OA审核记录重新激活（已撤回）：contractId={}, contractNo={}", contractId, contract.getContractNo());
                    } else {
                        // 无已撤回记录：检查是否有待审核记录
                        OaApprovalRecord pendingRecord = oaApprovalRecordService.findPendingBySource(sourceTable, contractId);
                        if (pendingRecord == null) {
                            // 新建OA审核记录
                            String sourceTableName = "委外运输合同";
                            String sourceNo = contract.getContractNo();
                            String title = "委外运输合同：" + sourceNo;
                            oaApprovalRecordService.submit(sourceTable, contractId, sourceTableName, sourceNo, title, currentUserId, currentUserName);
                        } else {
                            log.warn("运输合同已存在待审核的OA审批记录：contractId={}, oaRecordId={}", contractId, pendingRecord.getApprovalRecordId());
                        }
                    }

                    successIds.add(contractId);
                    log.info("运输合同批量提交审核成功：contractId={}, contractNo={}, userId={}", contractId, contract.getContractNo(), currentUserId);
                } catch (Exception e) {
                    log.error("运输合同批量提交审核失败：contractId={}, error={}", contractId, e.getMessage(), e);
                    failedIds.add(contractId);
                    failedReasons.put(contractId, "操作失败：" + e.getMessage());
                }
            }
        } else if ("待审核".equals(status)) {
            // 撤回：仅审核中状态可以撤回
            for (Integer contractId : contractIds) {
                try {
                    TransportContract contract = transportContractMapper.selectById(contractId);
                    if (contract == null || contract.getDeleted() == 1) {
                        failedIds.add(contractId);
                        failedReasons.put(contractId, "合同不存在或已删除");
                        continue;
                    }

                    // operateScope校验
                    if (!isAdmin && permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
                        if (!Objects.equals(contract.getCreatorId(), currentUserId)) {
                            failedIds.add(contractId);
                            failedReasons.put(contractId, "您只能操作自己创建的合同");
                            continue;
                        }
                    }

                    if (!STATUS_REVIEWING.equals(contract.getStatus())) {
                        failedIds.add(contractId);
                        failedReasons.put(contractId, "当前状态【" + contract.getStatus() + "】不允许撤回，仅审核中状态可以撤回");
                        continue;
                    }

                    // 更新合同状态为待审核
                    contract.setStatus(STATUS_DRAFT);
                    transportContractMapper.updateById(contract);

                    // 查找并撤回最新的OA审核记录
                    OaApprovalRecord oaRecord = oaApprovalRecordService.findLatestBySource("TRANSPORT_CONTRACT", contractId);
                    if (oaRecord != null) {
                        oaApprovalRecordService.cancel(oaRecord.getApprovalRecordId(), "TRANSPORT_CONTRACT", contractId, currentUserId, "批量撤回运输合同审核");
                    }

                    successIds.add(contractId);
                    log.info("运输合同批量撤回成功：contractId={}, contractNo={}, userId={}", contractId, contract.getContractNo(), currentUserId);
                } catch (Exception e) {
                    log.error("运输合同批量撤回失败：contractId={}, error={}", contractId, e.getMessage(), e);
                    failedIds.add(contractId);
                    failedReasons.put(contractId, "操作失败：" + e.getMessage());
                }
            }
        } else {
            // 其他状态更新：暂不支持批量操作
            for (Integer contractId : contractIds) {
                failedIds.add(contractId);
                failedReasons.put(contractId, "不支持批量更新为状态【" + status + "】");
            }
        }

        response.setSuccessIds(successIds);
        response.setFailedIds(failedIds);
        response.setFailedReasons(failedReasons);
        response.setAllSuccess(failedIds.isEmpty());

        log.info("运输合同批量状态更新完成：目标状态={}, 成功={}, 失败={}", status, successIds.size(), failedIds.size());

        return response;
    }

    @Override
    public Map<String, Long> getStatistics() {
        Map<String, Long> result = transportContractMapper.selectStatusCounts();
        if (result == null) {
            result = new HashMap<>();
        }
        // 确保所有状态都有值
        result.putIfAbsent("draft", 0L);
        result.putIfAbsent("reviewing", 0L);
        result.putIfAbsent("executing", 0L);
        result.putIfAbsent("completed", 0L);
        // 计算总数
        long total = result.values().stream().mapToLong(Long::longValue).sum();
        result.put("total", total);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransportContractQueryResponse> getContractWithVehicleList() {
        // 查询合同基本信息
        List<Map<String, Object>> contractList = transportContractMapper.selectContractWithVehicleList();

        List<TransportContractQueryResponse> result = new java.util.ArrayList<>();

        for (Map<String, Object> contractMap : contractList) {
            TransportContractQueryResponse response = new TransportContractQueryResponse();

            Integer contractId = contractMap.get("contractId") != null
                ? ((Number) contractMap.get("contractId")).intValue() : null;
            String contractNo = contractMap.get("contractNo") != null
                ? contractMap.get("contractNo").toString() : null;
            String carrierName = contractMap.get("carrierName") != null
                ? contractMap.get("carrierName").toString() : null;

            response.setContractId(contractId);
            response.setContractNo(contractNo);
            response.setCarrierName(carrierName);

            // 查询关联的车辆列表
            List<ContractVehicleResponse> vehicleRelations = contractVehicleMapper.selectByContractId(contractId);
            List<TransportContractQueryResponse.VehicleInfo> vehicleInfoList = new java.util.ArrayList<>();

            for (ContractVehicleResponse relation : vehicleRelations) {
                TransportContractQueryResponse.VehicleInfo vehicleInfo = new TransportContractQueryResponse.VehicleInfo();
                vehicleInfo.setVehicleId(relation.getVehicleId());
                vehicleInfo.setPlateNo(relation.getPlateNo());
                vehicleInfoList.add(vehicleInfo);
            }
            response.setVehicleList(vehicleInfoList);
            response.setVehicleCount((long) vehicleInfoList.size());

            // 统计运输车辆号牌为空的数量
            Long plateEmptyCount = transportContractMapper.countDispatchPlateEmptyByContractId(contractId);
            response.setDispatchPlateEmptyCount(plateEmptyCount != null ? plateEmptyCount : 0L);

            // 统计结算单合同编号为空的记录数量
            // 使用合同单号进行匹配（OUTSOURCE_TRANSPORT_SETTLEMENT.合同单号 = TRANSPORT_CONTRACT.合同单号）
            LambdaQueryWrapper<OutsourceTransportSettlement> settlementWrapper = new LambdaQueryWrapper<>();
            settlementWrapper.eq(OutsourceTransportSettlement::getContractNo, contractNo)
                    .isNull(OutsourceTransportSettlement::getContractId);
            Long settlementEmptyCount = outsourceTransportSettlementMapper.selectCount(settlementWrapper);
            response.setSettlementContractEmptyCount(settlementEmptyCount != null ? settlementEmptyCount : 0L);

            result.add(response);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchContracts(String keyword, String viewScope) {
        // 使用 ViewScopeHelper 解析视图范围
        String resolvedScope = ViewScopeHelper.resolveViewScope(PAGE_CODE, viewScope);

        // 获取当前用户ID
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        // 判断是否为管理员，管理员不受限制
        boolean isAdmin = currentUserId != null && authService.isAdmin(currentUserId);

        // 获取创建人过滤条件
        Integer creatorId = null;
        if (!isAdmin && ViewScopeHelper.isSelfScope(resolvedScope)) {
            creatorId = currentUserId;
        }

        return transportContractMapper.searchContracts(keyword, resolvedScope, creatorId);
    }

    // ==================== 私有方法 ====================

    /**
     * 根据ID查询合同，不存在则抛出异常
     */
    private TransportContract findByIdOrThrow(Integer contractId) {
        if (contractId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_INVALID.getCode(), "合同ID不能为空");
        }
        TransportContract contract = transportContractMapper.selectById(contractId);
        if (contract == null || contract.getDeleted() == 1) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "运输合同不存在");
        }
        return contract;
    }

    /**
     * 状态流转校验
     */
    private void validateStatusTransition(String currentStatus, String targetStatus) {
        switch (currentStatus) {
            case STATUS_DRAFT:
                if (!STATUS_REVIEWING.equals(targetStatus) && !STATUS_REJECTED.equals(targetStatus)) {
                    throw new BusinessException(ResultCodeEnum.OPERATION_NOT_ALLOWED.getCode(), "待审核合同只能提交审核或直接驳回");
                }
                break;
            case STATUS_REVIEWING:
                // 审核中状态可以驳回，或审核通过（已通过）
                if (!STATUS_REJECTED.equals(targetStatus) && !"已通过".equals(targetStatus)) {
                    throw new BusinessException(ResultCodeEnum.OPERATION_NOT_ALLOWED.getCode(), "审核中合同只能审核通过或驳回");
                }
                break;
            case STATUS_EXECUTING:
                if (!STATUS_COMPLETED.equals(targetStatus)) {
                    throw new BusinessException(ResultCodeEnum.OPERATION_NOT_ALLOWED.getCode(), "执行中合同只能标记完结");
                }
                break;
            default:
                // 已完结、已归档状态不允许变更
                throw new BusinessException(ResultCodeEnum.OPERATION_NOT_ALLOWED.getCode(), "当前状态不允许变更");
        }
    }

    /**
     * 生成合同单号
     */
    private String generateContractNo() {
        String prefix = "TC-" + java.time.LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";

        // 查询当天最大编号
        TransportContract maxContract = transportContractMapper.selectOne(
                new LambdaQueryWrapper<TransportContract>()
                        .likeRight(TransportContract::getContractNo, prefix)
                        .orderByDesc(TransportContract::getContractNo)
                        .last("LIMIT 1")
        );

        int sequence = 1;
        if (maxContract != null && maxContract.getContractNo() != null) {
            String maxNo = maxContract.getContractNo();
            if (maxNo.length() > prefix.length()) {
                try {
                    sequence = Integer.parseInt(maxNo.substring(prefix.length())) + 1;
                } catch (NumberFormatException e) {
                    // 解析失败，从1开始
                }
            }
        }
        return prefix + String.format("%05d", sequence);
    }

    /**
     * 将字符串转换为LocalDate（支持 yyyy-MM-dd 和 yyyy-MM-ddTHH:mm:ss 格式）
     */
    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            // 移除时间部分（如果有）
            String datePart = dateStr.trim().split("T")[0];
            return LocalDate.parse(datePart);
        } catch (Exception e) {
            log.warn("日期转换失败: {} - {}", dateStr, e.getMessage());
            return null;
        }
    }

    private TransportContractPageResponse convertToPageResponse(TransportContract contract) {
        TransportContractPageResponse response = new TransportContractPageResponse();
        BeanUtils.copyProperties(contract, response);
        return response;
    }

    private TransportContractDetailResponse convertToDetailResponse(TransportContract contract) {
        TransportContractDetailResponse response = new TransportContractDetailResponse();
        BeanUtils.copyProperties(contract, response);
        // 日期类型转换
        if (contract.getSignTime() != null) {
            response.setSignTime(contract.getSignTime().toString());
        }
        if (contract.getValidFrom() != null) {
            response.setValidFrom(contract.getValidFrom().toString());
        }
        if (contract.getValidTo() != null) {
            response.setValidTo(contract.getValidTo().toString());
        }
        if (contract.getAuditTime() != null) {
            response.setAuditTime(contract.getAuditTime().toString().replace("T", " "));
        }
        if (contract.getCreateTime() != null) {
            response.setCreateTime(contract.getCreateTime().toString().replace("T", " "));
        }
        if (contract.getUpdateTime() != null) {
            response.setUpdateTime(contract.getUpdateTime().toString().replace("T", " "));
        }
        if (contract.getUnitPrice() != null) {
            response.setUnitPrice(contract.getUnitPrice().toString());
        }
        // 查询OA审核记录信息
        if (oaApprovalRecordService != null) {
            try {
                OaApprovalRecord oaRecord = oaApprovalRecordService.findLatestBySource("TRANSPORT_CONTRACT", contract.getContractId());
                if (oaRecord != null) {
                    response.setOaApprovalRecordId(oaRecord.getApprovalRecordId());
                    response.setOaApprovalNo(oaRecord.getApprovalNo());
                }
            } catch (Exception e) {
                log.warn("查询运输合同OA审核记录失败：contractId={}", contract.getContractId(), e);
            }
        }
        return response;
    }

    /**
     * 同步更新OA审核记录状态
     * 当运输合同审核通过或驳回时，同步更新对应的OA审核记录
     */
    private void syncOaApprovalRecord(Integer contractId, String newStatus) {
        try {
            // 查找该合同对应的最新OA审核记录
            OaApprovalRecord oaRecord = oaApprovalRecordService.findLatestBySource("TRANSPORT_CONTRACT", contractId);
            if (oaRecord == null) {
                log.warn("未找到运输合同对应的OA审核记录，contractId={}", contractId);
                return;
            }

            // 只有待审核状态才同步更新
            if (!"待审核".equals(oaRecord.getApprovalStatus())) {
                log.info("OA审核记录状态不是待审核，跳过同步，approvalRecordId={}, currentStatus={}",
                        oaRecord.getApprovalRecordId(), oaRecord.getApprovalStatus());
                return;
            }

            // 根据运输合同的新状态更新OA审核记录
            String oaStatus = null;
            if ("已通过".equals(newStatus)) {
                oaStatus = "已通过";
            } else if ("已驳回".equals(newStatus)) {
                oaStatus = "已驳回";
            }

            if (oaStatus != null) {
                Integer currentUserId = SecurityUtil.getCurrentUserId();
                String currentUserName = SecurityUtil.getEmployeeName();

                oaRecord.setApprovalStatus(oaStatus);
                oaRecord.setApproverId(currentUserId);
                oaRecord.setApproverName(currentUserName);
                oaRecord.setApprovalTime(LocalDateTime.now());
                oaApprovalRecordMapper.updateById(oaRecord);

                log.info("同步更新OA审核记录成功：approvalRecordId={}, newOaStatus={}, approverId={}",
                        oaRecord.getApprovalRecordId(), oaStatus, currentUserId);
            }
        } catch (Exception e) {
            // OA记录同步失败不影响主业务，仅仅是日志记录
            log.error("同步更新OA审核记录失败：contractId={}, newStatus={}, error={}",
                    contractId, newStatus, e.getMessage(), e);
        }
    }
}
