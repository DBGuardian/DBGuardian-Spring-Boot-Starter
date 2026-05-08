package com.erp.service.settlement.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.settlement.dto.*;
import com.erp.entity.contract.BusinessContract;
import com.erp.entity.settlement.BusinessFeeHeader;
import com.erp.entity.settlement.BusinessFeeItem;
import com.erp.entity.settlement.BusinessFeeItemWasteInfo;
import com.erp.entity.settlement.BusinessFeeSettlementRel;
import com.erp.entity.settlement.SettlementWasteDetail;
import com.erp.entity.settlement.SettlementWasteInfo;
import com.erp.entity.system.Employee;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.mapper.contract.BusinessContractMapper;
import com.erp.mapper.finance.SettlementWasteDetailMapper;
import com.erp.mapper.settlement.SettlementWasteInfoMapper;
import com.erp.mapper.contract.OutOfScopeServiceMapper;
import com.erp.entity.contract.OutOfScopeService;
import com.erp.mapper.settlement.BusinessFeeHeaderMapper;
import com.erp.mapper.settlement.BusinessFeeItemMapper;
import com.erp.mapper.settlement.BusinessFeeSettlementRelMapper;
import com.erp.mapper.settlement.BusinessFeeItemWasteInfoMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.oa.OaApprovalRecordService;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.service.settlement.BusinessFeeService;
import com.erp.service.system.ILogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * 业务费服务实现类
 */
@Slf4j
@Service
public class BusinessFeeServiceImpl extends ServiceImpl<BusinessFeeHeaderMapper, BusinessFeeHeader>
        implements BusinessFeeService {

    @Autowired
    private BusinessFeeHeaderMapper businessFeeHeaderMapper;

    @Autowired
    private BusinessFeeItemMapper businessFeeItemMapper;

    @Autowired
    private BusinessFeeSettlementRelMapper businessFeeSettlementRelMapper;

    @Autowired
    private BusinessFeeItemWasteInfoMapper businessFeeItemWasteInfoMapper;

    @Autowired
    private OutOfScopeServiceMapper outOfScopeServiceMapper;

    @Autowired
    private SettlementWasteDetailMapper settlementWasteDetailMapper;

    @Autowired
    private SettlementWasteInfoMapper settlementWasteInfoMapper;

    @Autowired
    private BusinessContractMapper businessContractMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private OaApprovalRecordService oaApprovalRecordService;

    @Autowired
    private com.erp.service.system.MessageNotificationService messageNotificationService;

    /**
     * 业务费结算页面编码（用于权限控制）
     */
    private static final String BUSINESS_FEE_PAGE_CODE = "合同结算:业务费结算:页面";

    /**
     * 获取当前登录用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("未获取到当前登录用户信息");
        }
        return userId;
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
    public IPage<BusinessFeeListItemDTO> getBusinessFeePage(BusinessFeeQueryDTO queryDTO) {
        log.info("分页查询业务费列表，queryDTO={}", queryDTO);

        // 使用 ViewScopeHelper 解析视图范围
        String viewScope = ViewScopeHelper.resolveViewScope(BUSINESS_FEE_PAGE_CODE, queryDTO.getViewScope());

        // 获取当前用户ID
        Integer currentUserId = getCurrentUserId();

        // SELF 模式时添加创建人过滤条件，ALL 模式时不限制
        Integer creatorFilter = ViewScopeHelper.isSelfScope(viewScope) ? currentUserId : null;

        // 设置权限过滤条件
        queryDTO.setCreatorId(creatorFilter);

        // 构建分页参数
        Page<BusinessFeeListItemDTO> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 使用自定义连表查询
        IPage<BusinessFeeListItemDTO> dtoPage = businessFeeHeaderMapper.selectBusinessFeePage(page, queryDTO);

        return dtoPage;
    }

    @Override
    public BusinessFeeStatisticsDTO getBusinessFeeStatistics() {
        log.info("获取业务费统计信息");

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 应用数据范围控制（viewScope），与列表查询保持一致
        Integer creatorFilter = null;
        if (!admin) {
            // 获取当前员工对"业务费结算"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);

            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // 仅统计自己创建的业务费结算单
                creatorFilter = currentUserId;
                log.debug("应用数据范围控制：仅统计自己创建的业务费结算单，creatorFilter={}", creatorFilter);
            }
        }

        BusinessFeeStatisticsDTO statistics = new BusinessFeeStatisticsDTO();

        // 使用单条SQL分组统计，替代多次COUNT查询
        List<Map<String, Object>> statusCounts = businessFeeHeaderMapper.selectStatusStatistics(creatorFilter);

        // 初始化各状态计数
        long pendingAuditCount = 0;
        long auditingCount = 0;
        long auditedCount = 0;
        long rejectedCount = 0;
        long receivedCount = 0;

        // 解析统计结果
        for (Map<String, Object> row : statusCounts) {
            String status = (String) row.get("status");
            Long count = ((Number) row.get("count")).longValue();

            if ("待审核".equals(status)) {
                pendingAuditCount = count;
            } else if ("审核中".equals(status)) {
                auditingCount = count;
            } else if ("已审核".equals(status)) {
                auditedCount = count;
            } else if ("已驳回".equals(status)) {
                rejectedCount = count;
            } else if ("已收款".equals(status)) {
                receivedCount = count;
            }
        }

        statistics.setPendingAuditCount(pendingAuditCount);
        statistics.setAuditingCount(auditingCount);
        statistics.setAuditedCount(auditedCount);
        statistics.setRejectedCount(rejectedCount);
        statistics.setReceivedCount(receivedCount);

        // 计算总数量
        statistics.setTotalCount(pendingAuditCount + auditingCount + auditedCount + rejectedCount + receivedCount);

        return statistics;
    }

    @Override
    public BusinessFeeDetailDTO getBusinessFeeDetail(Integer id) {
        log.info("获取业务费详情，id={}", id);

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        BusinessFeeHeader entity = businessFeeHeaderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("业务费记录不存在");
        }

        // 应用数据范围控制（viewScope），与列表查询保持一致
        if (!admin) {
            // 获取当前员工对"业务费结算"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);

            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // 仅查看自己创建的业务费结算单（通过creatorId判断）
                if (!Objects.equals(entity.getCreatorId(), currentUserId)) {
                    throw new BusinessException("无权查看该业务费结算单");
                }
            }
            // 如果是 ALL 或没有配置，允许查看所有业务费结算单
        }

        return convertToDetailDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBusinessFee(Integer id, Integer deleteUserId) {
        log.info("删除业务费，id={}", id);

        BusinessFeeHeader entity = businessFeeHeaderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("业务费记录不存在");
        }

        // 检查状态是否允许删除
        if ("已审核".equals(entity.getStatus()) || "已收款".equals(entity.getStatus())) {
            throw new BusinessException("当前状态不允许删除");
        }

        // 保存旧数据用于日志记录
        BusinessFeeHeader oldData = businessFeeHeaderMapper.selectById(id);

        int rows = businessFeeHeaderMapper.deleteById(id);
        if (rows <= 0) {
            throw new BusinessException("删除业务费失败");
        }

        // 记录数据变更日志
        try {
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            String logContent = "删除业务费：业务编号=" + entity.getBusinessSeq();
            logRecordService.recordDataChangeLog("业务费结算", "BUSINESS_FEE",
                    String.valueOf(id),
                    "删除",
                    logContent,
                    oldData, null, currentUserId, null, true, null);
        } catch (Exception logEx) {
            log.warn("记录业务费删除数据变更日志失败，id={}", id, logEx);
        }

        log.info("业务费删除成功，id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAudit(Integer id, Integer operatorUserId) {
        log.info("提交审核，id={}, operatorUserId={}", id, operatorUserId);

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        BusinessFeeHeader entity = businessFeeHeaderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("业务费记录不存在");
        }

        // 操作范围校验
        if (!admin) {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);
            if (permission == null) {
                throw new BusinessException("您没有操作权限");
            }
            if (permission.getCanEdit() == null || permission.getCanEdit() == 0) {
                throw new BusinessException("您没有编辑权限");
            }
            if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                if (!Objects.equals(entity.getCreatorId(), currentUserId)) {
                    throw new BusinessException("您只能操作自己创建的业务费结算单");
                }
            }
        }

        // 只允许「待审核」或「已驳回」状态提交审核
        if (!"待审核".equals(entity.getStatus()) && !"已驳回".equals(entity.getStatus())) {
            throw new BusinessException("当前状态【" + entity.getStatus() + "】不允许提交审核，仅待审核或已驳回状态可提交");
        }

        entity.setStatus("审核中");
        entity.setUpdateTime(LocalDateTime.now());
        entity.setUpdateUserId(operatorUserId);

        int rows = businessFeeHeaderMapper.updateById(entity);
        if (rows <= 0) {
            throw new BusinessException("提交审核失败");
        }

        log.info("提交审核成功，id={}, 新状态=审核中", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAudit(Integer id, Integer operatorUserId) {
        log.info("取消审核，id={}, operatorUserId={}", id, operatorUserId);

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        BusinessFeeHeader entity = businessFeeHeaderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("业务费记录不存在");
        }

        // 操作范围校验
        if (!admin) {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);
            if (permission == null) {
                throw new BusinessException("您没有操作权限");
            }
            if (permission.getCanEdit() == null || permission.getCanEdit() == 0) {
                throw new BusinessException("您没有编辑权限");
            }
            if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                if (!Objects.equals(entity.getCreatorId(), currentUserId)) {
                    throw new BusinessException("您只能操作自己创建的业务费结算单");
                }
            }
        }

        // 只允许「审核中」状态取消审核
        if (!"审核中".equals(entity.getStatus())) {
            throw new BusinessException("当前状态【" + entity.getStatus() + "】不允许取消审核，仅审核中状态可取消");
        }

        entity.setStatus("待审核");
        entity.setUpdateTime(LocalDateTime.now());
        entity.setUpdateUserId(operatorUserId);

        int rows = businessFeeHeaderMapper.updateById(entity);
        if (rows <= 0) {
            throw new BusinessException("取消审核失败");
        }

        log.info("取消审核成功，id={}, 新状态=待审核", id);
    }

    @Override
    public SettlementWasteAggregateResponse getSettlementWarehousingAggregate(SettlementWasteAggregateRequest request) {

        log.info("批量查询危废结算单关联入库单聚合数据，settlementIds={}", request.getSettlementIds());

        if (request.getSettlementIds() == null || request.getSettlementIds().isEmpty()) {
            throw new BusinessException("结算单编号列表不能为空");
        }

        List<SettlementWasteAggregateItemDTO> items =
                businessFeeHeaderMapper.selectWarehousingAggregateBySettlementIds(request.getSettlementIds());

        SettlementWasteAggregateResponse response = new SettlementWasteAggregateResponse();
        response.setItems(items != null ? items : Collections.emptyList());
        response.setSettlementIds(request.getSettlementIds());

        log.info("聚合查询完成，结算单数={}，聚合明细行数={}",
                request.getSettlementIds().size(),
                response.getItems().size());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessFeeBatchOperationResult batchSubmitAudit(List<Integer> businessSeqs, Integer operatorUserId) {
        log.info("批量提交审核，businessSeqs={}, operatorUserId={}", businessSeqs, operatorUserId);

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        BusinessFeeBatchOperationResult result = new BusinessFeeBatchOperationResult();
        List<BusinessFeeBatchOperationResult.FailureItem> failures = new ArrayList<>();
        int successCount = 0;

        // 操作范围校验（非管理员需要检查 operateScope）
        EmployeePermission operatePermission = null;
        if (!admin) {
            operatePermission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);
            if (operatePermission == null) {
                throw new BusinessException("您没有操作权限");
            }
            if (operatePermission.getCanEdit() == null || operatePermission.getCanEdit() == 0) {
                throw new BusinessException("您没有编辑权限");
            }
        }

        // 获取当前用户信息
        Employee currentEmployee = employeeMapper.selectById(operatorUserId);
        String currentUserName = currentEmployee != null ? currentEmployee.getEmployeeName() : "未知";

        // 查询所有业务费
        List<BusinessFeeHeader> headers = businessFeeHeaderMapper.selectBatchIds(businessSeqs);
        Map<Integer, BusinessFeeHeader> headerMap = headers.stream()
                .collect(Collectors.toMap(BusinessFeeHeader::getBusinessSeq, Function.identity()));

        for (Integer businessSeq : businessSeqs) {
            BusinessFeeHeader header = headerMap.get(businessSeq);

            // 验证是否存在
            if (header == null) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, null, "业务费记录不存在"));
                continue;
            }

            // 操作范围校验（SELF）
            if (!admin && operatePermission != null && "SELF".equalsIgnoreCase(operatePermission.getOperateScope())) {
                if (!Objects.equals(header.getCreatorId(), currentUserId)) {
                    failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                            businessSeq, header.getBusinessCode(), "您只能操作自己创建的业务费结算单"));
                    continue;
                }
            }

            // 验证状态：仅待审核、已驳回状态可以提交审核（撤回后业务表状态为待审核）
            String currentStatus = header.getStatus();
            if (!"待审核".equals(currentStatus) && !"已驳回".equals(currentStatus)) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, header.getBusinessCode(),
                        "当前状态为【" + currentStatus + "】，仅待审核、已驳回状态可以提交审核"));
                continue;
            }

            try {
                OaApprovalRecord approvalRecord;
                String actionType;

                // 查询OA表中是否有已撤回的记录
                OaApprovalRecord withdrawnRecord = oaApprovalRecordService.findWithdrawnBySource("BUSINESS_FEE", businessSeq);
                if (withdrawnRecord != null) {
                    // 有已撤回记录：重新激活该记录，状态改为待审核，审核次数+1
                    approvalRecord = oaApprovalRecordService.reactivateWithdrawnRecord(
                            "BUSINESS_FEE",
                            businessSeq,
                            operatorUserId,
                            currentUserName
                    );
                    actionType = "重新激活";
                } else {
                    // 无已撤回记录：检查是否有待审核记录
                    OaApprovalRecord existingRecord = oaApprovalRecordService.findPendingBySource("BUSINESS_FEE", businessSeq);
                    if (existingRecord != null) {
                        failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                                businessSeq, header.getBusinessCode(), "该业务费已存在待审核的OA审批记录"));
                        continue;
                    }

                    // 新建OA审批记录
                    String businessTitle = String.format("业务合作结算：%s",
                            header.getBusinessCode() != null ? header.getBusinessCode() : "BF" + businessSeq);
                    approvalRecord = oaApprovalRecordService.submit(
                            "BUSINESS_FEE",
                            businessSeq,
                            "业务合作结算",
                            header.getBusinessCode(),
                            businessTitle,
                            operatorUserId,
                            currentUserName
                    );
                    actionType = "新建";
                }

                // 更新业务状态为审核中
                header.setStatus("审核中");
                header.setUpdateTime(LocalDateTime.now());
                header.setUpdateUserId(operatorUserId);
                businessFeeHeaderMapper.updateById(header);

                successCount++;
                log.info("业务费提交OA审核{}成功：businessSeq={}, approvalRecordId={}, actionType={}",
                        actionType, businessSeq, approvalRecord.getApprovalRecordId(), actionType);

            } catch (Exception e) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, header.getBusinessCode(), "提交审核失败：" + e.getMessage()));
                log.error("业务费提交OA审核失败：businessSeq={}", businessSeq, e);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);

        log.info("批量提交审核完成，成功={}，失败={}", successCount, failures.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessFeeBatchOperationResult batchCancelAudit(List<Integer> businessSeqs, Integer operatorUserId) {
        log.info("批量撤回审核，businessSeqs={}, operatorUserId={}", businessSeqs, operatorUserId);

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 操作范围校验（非管理员需要检查 operateScope）
        EmployeePermission operatePermission = null;
        if (!admin) {
            operatePermission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);
            if (operatePermission == null) {
                throw new BusinessException("您没有操作权限");
            }
            if (operatePermission.getCanEdit() == null || operatePermission.getCanEdit() == 0) {
                throw new BusinessException("您没有编辑权限");
            }
        }

        BusinessFeeBatchOperationResult result = new BusinessFeeBatchOperationResult();
        List<BusinessFeeBatchOperationResult.FailureItem> failures = new ArrayList<>();
        int successCount = 0;

        // 查询所有业务费
        List<BusinessFeeHeader> headers = businessFeeHeaderMapper.selectBatchIds(businessSeqs);
        Map<Integer, BusinessFeeHeader> headerMap = headers.stream()
                .collect(Collectors.toMap(BusinessFeeHeader::getBusinessSeq, Function.identity()));

        for (Integer businessSeq : businessSeqs) {
            BusinessFeeHeader header = headerMap.get(businessSeq);

            // 验证是否存在
            if (header == null) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, null, "业务费记录不存在"));
                continue;
            }

            // 操作范围校验（SELF）
            if (!admin && operatePermission != null && "SELF".equalsIgnoreCase(operatePermission.getOperateScope())) {
                if (!Objects.equals(header.getCreatorId(), currentUserId)) {
                    failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                            businessSeq, header.getBusinessCode(), "您只能操作自己创建的业务费结算单"));
                    continue;
                }
            }

            // 验证状态：仅审核中状态可以撤回
            String currentStatus = header.getStatus();
            if (!"审核中".equals(currentStatus)) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, header.getBusinessCode(),
                        "当前状态为【" + currentStatus + "】，仅审核中状态可以撤回"));
                continue;
            }

            try {
                // 查找最新的OA审批记录并撤回
                OaApprovalRecord latestRecord = oaApprovalRecordService.findLatestBySource("BUSINESS_FEE", businessSeq);
                if (latestRecord != null) {
                    oaApprovalRecordService.cancel(
                            latestRecord.getApprovalRecordId(),
                            "BUSINESS_FEE",
                            businessSeq,
                            operatorUserId,
                            "业务费批量撤回"
                    );
                }

                // 更新业务状态为待审核
                header.setStatus("待审核");
                header.setUpdateTime(LocalDateTime.now());
                header.setUpdateUserId(operatorUserId);
                businessFeeHeaderMapper.updateById(header);

                successCount++;
                log.info("业务费撤回审核成功：businessSeq={}", businessSeq);

            } catch (Exception e) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, header.getBusinessCode(), "撤回审核失败：" + e.getMessage()));
                log.error("业务费撤回审核失败：businessSeq={}", businessSeq, e);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);

        log.info("批量撤回审核完成，成功={}，失败={}", successCount, failures.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessFeeBatchOperationResult batchDelete(List<Integer> businessSeqs, Integer operatorUserId) {
        log.info("批量删除业务费，businessSeqs={}, operatorUserId={}", businessSeqs, operatorUserId);

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 操作范围校验（非管理员需要检查 operateScope）
        EmployeePermission operatePermission = null;
        if (!admin) {
            operatePermission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);
            if (operatePermission == null) {
                throw new BusinessException("您没有操作权限");
            }
            if (operatePermission.getCanEdit() == null || operatePermission.getCanEdit() == 0) {
                throw new BusinessException("您没有编辑权限");
            }
        }

        BusinessFeeBatchOperationResult result = new BusinessFeeBatchOperationResult();
        List<BusinessFeeBatchOperationResult.FailureItem> failures = new ArrayList<>();
        int successCount = 0;

        // 查询所有业务费
        List<BusinessFeeHeader> headers = businessFeeHeaderMapper.selectBatchIds(businessSeqs);
        Map<Integer, BusinessFeeHeader> headerMap = headers.stream()
                .collect(Collectors.toMap(BusinessFeeHeader::getBusinessSeq, Function.identity()));

        for (Integer businessSeq : businessSeqs) {
            BusinessFeeHeader header = headerMap.get(businessSeq);

            // 验证是否存在
            if (header == null) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, null, "业务费记录不存在"));
                continue;
            }

            // 操作范围校验（SELF）
            if (!admin && operatePermission != null && "SELF".equalsIgnoreCase(operatePermission.getOperateScope())) {
                if (!Objects.equals(header.getCreatorId(), currentUserId)) {
                    failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                            businessSeq, header.getBusinessCode(), "您只能操作自己创建的业务费结算单"));
                    continue;
                }
            }

            // 验证状态：仅待审核和已驳回状态可以删除
            String currentStatus = header.getStatus();
            if (!"待审核".equals(currentStatus) && !"已驳回".equals(currentStatus)) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, header.getBusinessCode(),
                        "当前状态为【" + currentStatus + "】，仅待审核和已驳回状态可以删除"));
                continue;
            }

            try {
                // 调用原有的删除方法
                deleteBusinessFee(businessSeq, operatorUserId);
                successCount++;
                log.info("业务费删除成功：businessSeq={}", businessSeq);

            } catch (Exception e) {
                failures.add(new BusinessFeeBatchOperationResult.FailureItem(
                        businessSeq, header.getBusinessCode(), "删除失败：" + e.getMessage()));
                log.error("业务费删除失败：businessSeq={}", businessSeq, e);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);

        log.info("批量删除完成，成功={}，失败={}", successCount, failures.size());
        return result;
    }

    /**
     * 转换为详情DTO
     */
    private BusinessFeeDetailDTO convertToDetailDTO(BusinessFeeHeader entity) {
        BusinessFeeDetailDTO dto = businessFeeHeaderMapper.selectBusinessFeeDetail(entity.getBusinessSeq());
        if (dto == null) {
            throw new BusinessException("业务费详情不存在");
        }

        // 复制/补充基础字段（确保与主表实体保持一致）
        dto.setBusinessSeq(entity.getBusinessSeq());
        dto.setBusinessCode(entity.getBusinessCode());
        dto.setServiceCompanyName(entity.getServiceCompanyName());
        dto.setSettlementType(entity.getSettlementType());
        dto.setSettlementAmount(entity.getSettlementAmount());
        dto.setReceivedAmount(entity.getReceivedAmount());
        dto.setPaymentDate(entity.getPaymentDate());
        dto.setStatus(entity.getStatus());
        dto.setAuditOpinion(entity.getAuditOpinion());
        dto.setRemark(entity.getRemark());
        dto.setIsLocked(entity.getIsLocked());
        dto.setLockTime(entity.getLockTime());
        dto.setCreatorName(entity.getCreatorName());
        dto.setCreateTime(entity.getCreateTime());
        dto.setAuditTime(entity.getAuditTime());

        // 设置业务合作合同信息（含收款银行卡，用于汇款）
        if (entity.getBusinessContractId() != null) {
            BusinessContract bc = businessContractMapper.selectById(entity.getBusinessContractId());
            if (bc != null) {
                BusinessFeeDetailDTO.BusinessContractInfoDTO bcInfo = new BusinessFeeDetailDTO.BusinessContractInfoDTO();
                bcInfo.setContractId(bc.getContractId());
                bcInfo.setContractNo(bc.getContractNo());
                bcInfo.setSalespersonName(bc.getSalespersonName());
                bcInfo.setBankName(bc.getBankName());
                bcInfo.setCardNumber(bc.getCardNumber());
                bcInfo.setAccountName(bc.getAccountName());
                bcInfo.setStatus(bc.getStatus());
                dto.setBusinessContractInfo(bcInfo);
            }
            // 冗余顶层字段，方便前端直接读取
            dto.setBusinessContractId(entity.getBusinessContractId());
        }

        // 核心详情聚合（settlementRels、businessFeeItems、wasteInfoList）已由 XML 完成
        List<BusinessFeeDetailDTO.SettlementRelInfoDTO> settlementRels = dto.getSettlementRels() != null
                ? dto.getSettlementRels()
                : Collections.emptyList();

        // 批量查询员工信息（减少N+1查询）
        // 收集所有需要查询的员工ID
        List<Integer> employeeIds = new ArrayList<>();
        if (entity.getSalespersonId() != null) {
            employeeIds.add(entity.getSalespersonId());
        }
        if (entity.getAuditorId() != null) {
            employeeIds.add(entity.getAuditorId());
        }
        if (entity.getLockUserId() != null) {
            employeeIds.add(entity.getLockUserId());
        }
        
        // 批量查询员工信息
        Map<Integer, Employee> employeeMap = Collections.emptyMap();
        if (!employeeIds.isEmpty()) {
            List<Employee> employees = employeeMapper.selectBatchIds(employeeIds);
            employeeMap = employees.stream()
                    .collect(Collectors.toMap(Employee::getEmployeeId, e -> e, (a, b) -> a));
        }

        // 设置业务员信息
        if (entity.getSalespersonId() != null) {
            BusinessFeeDetailDTO.EmployeeInfoDTO salespersonInfo = new BusinessFeeDetailDTO.EmployeeInfoDTO();
            salespersonInfo.setEmployeeId(entity.getSalespersonId());
            Employee emp = employeeMap.get(entity.getSalespersonId());
            if (emp != null) {
                salespersonInfo.setEmployeeName(emp.getEmployeeName());
            }
            dto.setSalespersonInfo(salespersonInfo);
        }

        // 设置制单人信息
        if (entity.getCreatorId() != null) {
            BusinessFeeDetailDTO.EmployeeInfoDTO creatorInfo = new BusinessFeeDetailDTO.EmployeeInfoDTO();
            creatorInfo.setEmployeeId(entity.getCreatorId());
            creatorInfo.setEmployeeName(entity.getCreatorName());
            dto.setCreatorInfo(creatorInfo);
        }

        // 设置审核人信息
        if (entity.getAuditorId() != null) {
            BusinessFeeDetailDTO.EmployeeInfoDTO auditorInfo = new BusinessFeeDetailDTO.EmployeeInfoDTO();
            auditorInfo.setEmployeeId(entity.getAuditorId());
            Employee emp = employeeMap.get(entity.getAuditorId());
            if (emp != null) {
                auditorInfo.setEmployeeName(emp.getEmployeeName());
            }
            dto.setAuditorInfo(auditorInfo);
        }

        // 设置锁定信息（仅当已锁定时）
        if (entity.getIsLocked() != null && entity.getIsLocked()) {
            BusinessFeeDetailDTO.LockInfoDTO lockInfo = new BusinessFeeDetailDTO.LockInfoDTO();
            lockInfo.setIsLocked(entity.getIsLocked());
            lockInfo.setLockTime(entity.getLockTime());
            if (entity.getLockUserId() != null) {
                Employee emp = employeeMap.get(entity.getLockUserId());
                if (emp != null) {
                    lockInfo.setLockUserName(emp.getEmployeeName());
                }
            }
            dto.setLockInfo(lockInfo);
        }

        List<BusinessFeeItemDTO> itemDTOs = dto.getBusinessFeeItems() != null
                ? dto.getBusinessFeeItems()
                : Collections.emptyList();
        List<BusinessFeeItem> items = itemDTOs.stream().map(itemDTO -> {
            BusinessFeeItem item = new BusinessFeeItem();
            item.setItemSeq(itemDTO.getItemSeq());
            item.setBusinessSeq(itemDTO.getBusinessSeq());
            item.setPaymentDirection(itemDTO.getPaymentDirection());
            item.setSettlementMode(itemDTO.getSettlementMode());
            item.setBaseUnitPrice(itemDTO.getBaseUnitPrice());
            item.setValuableUnitPrice(itemDTO.getValuableUnitPrice());
            item.setWorthlessUnitPrice(itemDTO.getWorthlessUnitPrice());
            item.setContractBasePrice(itemDTO.getContractBasePrice());
            item.setValuableContractBasePrice(itemDTO.getValuableContractBasePrice());
            item.setWorthlessContractBasePrice(itemDTO.getWorthlessContractBasePrice());
            item.setIntermediaryFee(itemDTO.getIntermediaryFee());
            item.setRebateRatio(itemDTO.getRebateRatio());
            item.setPayableAmount(itemDTO.getPayableAmount());
            item.setValuablePayableAmount(itemDTO.getValuablePayableAmount());
            item.setWorthlessPayableAmount(itemDTO.getWorthlessPayableAmount());
            item.setValuableWeight(itemDTO.getValuableWeight());
            item.setWorthlessWeight(itemDTO.getWorthlessWeight());
            item.setCargoSettlementAmount(itemDTO.getCargoSettlementAmount());
            item.setEnableAuxAccounting(itemDTO.getEnableAuxAccounting());
            item.setBasicQuantity(itemDTO.getBasicQuantity());
            item.setAuxiliaryQuantity(itemDTO.getAuxiliaryQuantity());
            item.setAuxiliaryUnit(itemDTO.getAuxiliaryUnit());
            item.setCreatorId(itemDTO.getCreatorId());
            item.setCreateTime(itemDTO.getCreateTime());
            item.setUpdaterId(itemDTO.getUpdaterId());
            item.setUpdateTime(itemDTO.getUpdateTime());
            return item;
        }).collect(Collectors.toList());

        // 批量查询结算危废明细
        // 策略：通过关联表获取所有关联的危废结算单，再汇总所有危废明细
        Map<Integer, SettlementWasteDetail> settlementDetailMap = new java.util.LinkedHashMap<>();
        Map<Integer, SettlementWasteInfo> settlementWasteInfoMap = new java.util.LinkedHashMap<>();
        if (!settlementRels.isEmpty()) {
            for (BusinessFeeDetailDTO.SettlementRelInfoDTO rel : settlementRels) {
                if (rel.getSettlementId() != null) {
                    QueryWrapper<SettlementWasteDetail> detailQuery = new QueryWrapper<>();
                    detailQuery.eq("结算单编号", rel.getSettlementId())
                               .orderByAsc("序号");
                    List<SettlementWasteDetail> details = settlementWasteDetailMapper.selectList(detailQuery);
                    for (SettlementWasteDetail d : details) {
                        settlementDetailMap.put(d.getDetailId().intValue(), d);
                    }
                    // 批量查询废物信息
                    if (!details.isEmpty()) {
                        List<Long> detailIds = details.stream()
                            .map(SettlementWasteDetail::getDetailId)
                            .collect(Collectors.toList());
                        List<SettlementWasteInfo> wasteInfos = settlementWasteInfoMapper.selectByDetailIds(detailIds);
                        for (SettlementWasteInfo info : wasteInfos) {
                            settlementWasteInfoMap.put(info.getDetailId().intValue(), info);
                        }
                    }
                }
            }
            log.info("从关联危废结算单查询危废明细，关联结算单数={}, 合计危废明细数={}",
                    settlementRels.size(), settlementDetailMap.size());
        } else {
            log.warn("业务费未关联任何危废结算单，无法查询结算危废明细，businessSeq={}", entity.getBusinessSeq());
        }

        // 回填结算危废明细中的核算数量字段（优先按危废子项来源明细编号回填）
        if (!settlementDetailMap.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                BusinessFeeItemDTO itemDTO = itemDTOs.get(i);
                if (itemDTO == null || itemDTO.getWasteInfoList() == null || itemDTO.getWasteInfoList().isEmpty()) {
                    continue;
                }
                Integer matchedDetailId = itemDTO.getWasteInfoList().stream()
                        .filter(Objects::nonNull)
                        .map(BusinessFeeItemDTO.BusinessFeeItemWasteInfoDTO::getSourceWasteDetailIds)
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                if (matchedDetailId == null) {
                    continue;
                }
                SettlementWasteDetail detail = settlementDetailMap.get(matchedDetailId);
                if (detail == null) {
                    continue;
                }
                itemDTO.setEnableAuxAccounting(detail.getEnableAuxiliaryAccounting());
                itemDTO.setBasicQuantity(detail.getBasicSettlementQuantity());
                itemDTO.setAuxiliaryQuantity(detail.getAuxiliarySettlementQuantity());
                itemDTO.setAuxiliaryUnit(detail.getAuxiliaryUnit());
            }
        }
        dto.setBusinessFeeItems(itemDTOs);

        // 查询入库明细列表（通过所有关联的危废结算单获取）
        List<WarehousingItemDTO> warehousingItems = new ArrayList<>();
        if (!settlementRels.isEmpty()) {
            Map<Integer, WarehousingItemDTO> warehousingByItemId = new java.util.LinkedHashMap<>();
            for (BusinessFeeDetailDTO.SettlementRelInfoDTO rel : settlementRels) {
                if (rel.getSettlementId() == null) continue;
                List<WarehousingItemDTO> rawItems = businessFeeHeaderMapper.selectWarehousingItems(rel.getSettlementId());
                for (WarehousingItemDTO w : rawItems) {
                    warehousingByItemId.putIfAbsent(w.getItemId(), w);
                }
            }
            warehousingItems = new ArrayList<>(warehousingByItemId.values());
            log.info("入库明细查询完成，关联结算单数={}, 入库明细数={}", settlementRels.size(), warehousingItems.size());
            dto.setWarehousingItems(warehousingItems);
        }

        // 组装统一的业务费危废详情列表
        List<BusinessFeeWasteDetailDTO> wasteDetailItems = buildWasteDetailItems(
                entity,
                items,
                itemDTOs,
                warehousingItems,
                settlementDetailMap,
                settlementWasteInfoMap
        );
        dto.setWasteDetailItems(wasteDetailItems);

        // 查询价外服务列表（businessType=BUSINESS_FEE，businessId=业务序号）
        List<OutOfScopeService> outOfScopeList = outOfScopeServiceMapper.selectByBusiness("BUSINESS_FEE", entity.getBusinessSeq());
        if (outOfScopeList != null && !outOfScopeList.isEmpty()) {
            List<BusinessFeeDetailDTO.OutOfScopeServiceItemDTO> outOfScopeDTOs = outOfScopeList.stream()
                    .map(os -> {
                        BusinessFeeDetailDTO.OutOfScopeServiceItemDTO osDTO = new BusinessFeeDetailDTO.OutOfScopeServiceItemDTO();
                        osDTO.setOutOfScopeServiceId(os.getOutOfScopeServiceId());
                        osDTO.setProject(os.getProject());
                        osDTO.setSpec(os.getSpec());
                        osDTO.setUnit(os.getUnit());
                        osDTO.setPlannedQuantity(os.getPlannedQuantity());
                        osDTO.setContractUnitPrice(os.getContractUnitPrice());
                        osDTO.setSettledQuantity(os.getSettledQuantity());
                        osDTO.setSettledUnitPrice(os.getSettledUnitPrice());
                        osDTO.setSettledAmount(os.getSettledAmount());
                        osDTO.setStatus(os.getStatus());
                        osDTO.setRemark(os.getRemark());
                        osDTO.setVersion(os.getVersion());
                        return osDTO;
                    })
                    .collect(Collectors.toList());
            dto.setOutOfScopeServices(outOfScopeDTOs);
        }

        return dto;
    }

    /**
     * 组装统一的业务费危废详情列表
     * @param settlementWasteInfoMap 结算危废信息映射（从SETTLEMENT_WASTE_INFO表查询）
     */
    private List<BusinessFeeWasteDetailDTO> buildWasteDetailItems(
            BusinessFeeHeader header,
            List<BusinessFeeItem> items,
            List<BusinessFeeItemDTO> itemDTOs,
            List<WarehousingItemDTO> warehousingItems,
            Map<Integer, SettlementWasteDetail> settlementDetailMap,
            Map<Integer, SettlementWasteInfo> settlementWasteInfoMap) {

        // 构建入库明细映射（按 settlementWasteDetailId 精确匹配）
        final Map<Integer, WarehousingItemDTO> warehousingByDetailIdMap;
        if (warehousingItems != null && !warehousingItems.isEmpty()) {
            warehousingByDetailIdMap = warehousingItems.stream()
                    .filter(w -> w.getSettlementWasteDetailId() != null)
                    .collect(Collectors.toMap(
                            w -> w.getSettlementWasteDetailId().intValue(),
                            w -> w,
                            (a, b) -> a));
        } else {
            warehousingByDetailIdMap = Collections.emptyMap();
        }

        if (items != null && !items.isEmpty()) {
            return items.stream().map(item -> {
                int index = items.indexOf(item);
                BusinessFeeItemDTO itemDTO = index < itemDTOs.size() ? itemDTOs.get(index) : null;
                return buildWasteDetailDTO(header, item, itemDTO, warehousingByDetailIdMap, settlementDetailMap, settlementWasteInfoMap);
            }).collect(Collectors.toList());
        }

        // 没有业务费明细但有结算危废明细时，按结算单危废明细组装
        if (!settlementDetailMap.isEmpty()) {
            return settlementDetailMap.values().stream().map(detail -> {
                BusinessFeeWasteDetailDTO dto = new BusinessFeeWasteDetailDTO();
                dto.setSettlementId(detail.getSettlementId() != null ? detail.getSettlementId().intValue() : null);
                dto.setSettlementWasteDetailId(detail.getDetailId() != null ? detail.getDetailId().intValue() : null);
                dto.setSettlementType(header.getSettlementType());
                // 从废物信息映射获取废物数据
                Integer detailIdKey = detail.getDetailId() != null ? detail.getDetailId().intValue() : null;
                SettlementWasteInfo wasteInfo = detailIdKey != null ? settlementWasteInfoMap.get(detailIdKey) : null;
                if (wasteInfo != null) {
                    dto.setSettlementWasteCategory(wasteInfo.getWasteCategory());
                    dto.setSettlementWasteCode(wasteInfo.getWasteCode());
                    dto.setSettlementWasteName(wasteInfo.getWasteName());
                }
                dto.setSettlementEnableAuxiliaryAccounting(detail.getEnableAuxiliaryAccounting());
                dto.setBasicSettlementQuantity(detail.getBasicSettlementQuantity());
                dto.setBasicSettlementUnit(detail.getBasicUnit());
                dto.setAuxiliarySettlementQuantity(detail.getAuxiliarySettlementQuantity());
                dto.setSettlementAuxiliaryUnit(detail.getAuxiliaryUnit());
                dto.setSettlementUnitPrice(detail.getUnitPrice());
                dto.setSettlementAmount(detail.getAmount());
                // 匹配入库明细
                Integer detailId = detail.getDetailId() != null ? detail.getDetailId().intValue() : null;
                WarehousingItemDTO warehousing = detailId != null ? warehousingByDetailIdMap.get(detailId) : null;
                if (warehousing != null) {
                    dto.setWarehousingItemId(warehousing.getItemId());
                    dto.setWarehousingNo(warehousing.getWarehousingNo());
                    dto.setWarehousingWasteName(warehousing.getWasteName());
                    dto.setWarehousingWasteCode(warehousing.getWasteCode());
                    dto.setWarehousingWasteCategory(warehousing.getWasteCategory());
                    dto.setValuableWeight(warehousing.getValuableWeight());
                    dto.setWorthlessWeight(warehousing.getWorthlessWeight());
                    dto.setWarehousingEnableAuxAccounting(warehousing.getEnableAuxAccounting());
                    dto.setWarehousingBasicQuantity(warehousing.getBasicQuantity());
                    dto.setWarehousingAuxiliaryQuantity(warehousing.getAuxiliaryQuantity());
                    dto.setWarehousingAuxiliaryUnit(warehousing.getAuxiliaryUnit());
                }
                return dto;
            }).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }


    /**
     * 构建单个业务费危废详情DTO
     * @param settlementWasteInfoMap 结算危废信息映射（从SETTLEMENT_WASTE_INFO表查询）
     */
    private BusinessFeeWasteDetailDTO buildWasteDetailDTO(
            BusinessFeeHeader header,
            BusinessFeeItem item,
            BusinessFeeItemDTO itemDTO,
            Map<Integer, WarehousingItemDTO> warehousingByDetailIdMap,
            Map<Integer, SettlementWasteDetail> settlementDetailMap,
            Map<Integer, SettlementWasteInfo> settlementWasteInfoMap) {

        BusinessFeeWasteDetailDTO dto = new BusinessFeeWasteDetailDTO();

        // 基础标识
        dto.setBusinessSeq(item.getBusinessSeq());
        dto.setBusinessFeeItemSeq(item.getItemSeq());

        // 业务费自身字段
        dto.setSettlementType(header.getSettlementType());
        dto.setPaymentDirection(item.getPaymentDirection());
        dto.setSettlementMode(item.getSettlementMode());

        dto.setBaseUnitPrice(item.getBaseUnitPrice());
        dto.setValuableUnitPrice(item.getValuableUnitPrice());
        dto.setWorthlessUnitPrice(item.getWorthlessUnitPrice());
        dto.setContractBasePrice(item.getContractBasePrice());
        dto.setValuableContractBasePrice(item.getValuableContractBasePrice());
        dto.setWorthlessContractBasePrice(item.getWorthlessContractBasePrice());
        dto.setIntermediaryFee(item.getIntermediaryFee());
        dto.setRebateRatio(item.getRebateRatio());
        dto.setPayableAmount(item.getPayableAmount());
        dto.setValuablePayableAmount(item.getValuablePayableAmount());
        dto.setWorthlessPayableAmount(item.getWorthlessPayableAmount());
        dto.setValuableWeight(item.getValuableWeight());
        dto.setWorthlessWeight(item.getWorthlessWeight());
        dto.setCargoSettlementAmount(item.getCargoSettlementAmount());
        dto.setEnableAuxAccounting(item.getEnableAuxAccounting());
        dto.setBasicQuantity(item.getBasicQuantity());
        dto.setAuxiliaryQuantity(item.getAuxiliaryQuantity());
        dto.setAuxiliaryUnit(item.getAuxiliaryUnit());

        // 危废快照统一从子表/wasteInfoList 回填；这样查看/修改模式能完整拿到 BUSINESS_FEE_ITEM_WASTE_INFO
        if (itemDTO != null && itemDTO.getWasteInfoList() != null && !itemDTO.getWasteInfoList().isEmpty()) {
            dto.setWasteInfoList(itemDTO.getWasteInfoList());
            BusinessFeeItemDTO.BusinessFeeItemWasteInfoDTO firstWasteInfo = itemDTO.getWasteInfoList().get(0);
            if (firstWasteInfo != null) {
                dto.setWasteCategory(firstWasteInfo.getWasteCategory());
                dto.setWasteCode(firstWasteInfo.getWasteCode());
                dto.setWasteName(firstWasteInfo.getWasteName());
            }
        }

        // 结算危废明细字段（按来源危废明细编号回填）
        Integer matchedDetailId = itemDTO != null && itemDTO.getWasteInfoList() != null
                ? itemDTO.getWasteInfoList().stream()
                .filter(Objects::nonNull)
                .map(BusinessFeeItemDTO.BusinessFeeItemWasteInfoDTO::getSourceWasteDetailIds)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null)
                : null;
        if (matchedDetailId != null) {
            dto.setSettlementWasteDetailId(matchedDetailId);
            SettlementWasteDetail detail = settlementDetailMap.get(matchedDetailId);
            if (detail != null) {
                // 从废物信息映射获取废物数据
                SettlementWasteInfo wasteInfo = settlementWasteInfoMap.get(matchedDetailId);
                if (wasteInfo != null) {
                    dto.setSettlementWasteCategory(wasteInfo.getWasteCategory());
                    dto.setSettlementWasteCode(wasteInfo.getWasteCode());
                    dto.setSettlementWasteName(wasteInfo.getWasteName());
                }
                dto.setSettlementEnableAuxiliaryAccounting(detail.getEnableAuxiliaryAccounting());
                dto.setBasicSettlementQuantity(detail.getBasicSettlementQuantity());
                dto.setBasicSettlementUnit(detail.getBasicUnit());
                dto.setAuxiliarySettlementQuantity(detail.getAuxiliarySettlementQuantity());
                dto.setSettlementAuxiliaryUnit(detail.getAuxiliaryUnit());
                dto.setSettlementUnitPrice(detail.getUnitPrice());
                dto.setSettlementAmount(detail.getAmount());
            } else {
                log.warn("业务费明细关联的结算危废明细不存在，itemSeq={}, matchedDetailId={}",
                        item.getItemSeq(), matchedDetailId);
            }
        }

        // 入库明细字段（通过来源危废明细编号匹配）
        if (matchedDetailId != null) {
            WarehousingItemDTO warehousing = warehousingByDetailIdMap.get(matchedDetailId);
            if (warehousing != null) {
                dto.setWarehousingItemId(warehousing.getItemId());
                dto.setWarehousingNo(warehousing.getWarehousingNo());
                dto.setWarehousingWasteName(warehousing.getWasteName());
                dto.setWarehousingWasteCode(warehousing.getWasteCode());
                dto.setWarehousingWasteCategory(warehousing.getWasteCategory());
                dto.setValuableWeight(warehousing.getValuableWeight());
                dto.setWorthlessWeight(warehousing.getWorthlessWeight());
                dto.setWarehousingEnableAuxAccounting(warehousing.getEnableAuxAccounting());
                dto.setWarehousingBasicQuantity(warehousing.getBasicQuantity());
                dto.setWarehousingAuxiliaryQuantity(warehousing.getAuxiliaryQuantity());
                dto.setWarehousingAuxiliaryUnit(warehousing.getAuxiliaryUnit());
            }
        }

        return dto;
    }



    /**
     * 保存业务费详情数据（主表+明细表）
     * 事务处理流程：
     * 1. 主表 UPSERT（新增或更新）
     * 2. 明细表全量比对（增删改）
     * 3. 价外服务表全量比对（增删改）
     * 4. 事务保证：任何一步失败，全部回滚
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessFeeCreateResultDTO createBusinessFeeDetail(BusinessFeeCreateDTO createDTO, Integer operatorUserId) {
        log.info("新增业务费详情数据，businessSeq={}, operatorUserId={}", createDTO.getBusinessSeq(), operatorUserId);

        if (createDTO.getBusinessSeq() != null) {
            throw new BusinessException("新增业务费时 businessSeq 必须为空");
        }

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        try {
            if (!admin) {
                EmployeePermission permission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);
                if (permission != null) {
                    if (permission.getCanEdit() == null || permission.getCanEdit() == 0) {
                        throw new BusinessException("您没有编辑权限");
                    }
                } else {
                    throw new BusinessException("您没有操作权限");
                }
            }

            List<BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO> itemDTOs = createDTO.getBusinessFeeItems();
            List<BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO> receivableItems = itemDTOs == null
                    ? Collections.emptyList()
                    : itemDTOs.stream()
                    .filter(i -> "RECEIVABLE".equals(i.getPaymentDirection()))
                    .collect(Collectors.toList());
            List<BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO> payableItems = itemDTOs == null
                    ? Collections.emptyList()
                    : itemDTOs.stream()
                    .filter(i -> "PAYABLE".equals(i.getPaymentDirection()))
                    .collect(Collectors.toList());

            boolean hasReceivable = !receivableItems.isEmpty();
            boolean hasPayable = !payableItems.isEmpty();
            if (!hasReceivable && !hasPayable) {
                throw new BusinessException("业务费明细缺少有效的收付款方向");
            }

            BusinessFeeCreateResultDTO createResult = new BusinessFeeCreateResultDTO();
            createResult.setSplit(hasReceivable && hasPayable);

            if (hasReceivable) {
                Integer receivableBusinessSeq = createSingleDirectionBusinessFee(
                        createDTO,
                        operatorUserId,
                        receivableItems,
                        "RECEIVABLE",
                        createResult.getSplit() ? "-R" : ""
                );
                createResult.setReceivableBusinessSeq(receivableBusinessSeq);
                if (!createResult.getSplit()) {
                    createResult.setBusinessSeq(receivableBusinessSeq);
                }
            }

            if (hasPayable) {
                Integer payableBusinessSeq = createSingleDirectionBusinessFee(
                        createDTO,
                        operatorUserId,
                        payableItems,
                        "PAYABLE",
                        createResult.getSplit() ? "-P" : ""
                );
                createResult.setPayableBusinessSeq(payableBusinessSeq);
                if (!createResult.getSplit()) {
                    createResult.setBusinessSeq(payableBusinessSeq);
                }
            }

            if (createResult.getSplit()) {
                createResult.setBusinessSeq(createResult.getReceivableBusinessSeq());
            }

            log.info("新增业务费详情数据成功，businessSeq={}, split={}, receivableBusinessSeq={}, payableBusinessSeq={}",
                    createResult.getBusinessSeq(),
                    createResult.getSplit(),
                    createResult.getReceivableBusinessSeq(),
                    createResult.getPayableBusinessSeq());

            // 记录数据变更日志
            try {
                currentUserId = SecurityUtil.getCurrentUserId();
                // 记录主业务费单
                if (createResult.getBusinessSeq() != null) {
                    BusinessFeeHeader newData = businessFeeHeaderMapper.selectById(createResult.getBusinessSeq());
                    logRecordService.recordDataChangeLog("业务费结算", "BUSINESS_FEE",
                            String.valueOf(createResult.getBusinessSeq()),
                            "新增",
                            "创建业务费：业务编号=" + createResult.getBusinessSeq() + "，split=" + createResult.getSplit(),
                            null, newData, currentUserId, null, true, null);
                }
                // 记录应收业务费单
                if (createResult.getReceivableBusinessSeq() != null && !createResult.getSplit()) {
                    BusinessFeeHeader receivableData = businessFeeHeaderMapper.selectById(createResult.getReceivableBusinessSeq());
                    if (receivableData != null) {
                        logRecordService.recordDataChangeLog("业务费结算", "BUSINESS_FEE",
                                String.valueOf(createResult.getReceivableBusinessSeq()),
                                "新增",
                                "创建应收业务费：业务编号=" + createResult.getReceivableBusinessSeq(),
                                null, receivableData, currentUserId, null, true, null);
                    }
                }
                // 记录应付业务费单
                if (createResult.getPayableBusinessSeq() != null) {
                    BusinessFeeHeader payableData = businessFeeHeaderMapper.selectById(createResult.getPayableBusinessSeq());
                    if (payableData != null) {
                        logRecordService.recordDataChangeLog("业务费结算", "BUSINESS_FEE",
                                String.valueOf(createResult.getPayableBusinessSeq()),
                                "新增",
                                "创建应付业务费：业务编号=" + createResult.getPayableBusinessSeq(),
                                null, payableData, currentUserId, null, true, null);
                    }
                }
            } catch (Exception logEx) {
                log.warn("记录业务费创建数据变更日志失败", logEx);
            }

            return createResult;

        } catch (BusinessException e) {
            log.error("新增业务费详情数据失败：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("新增业务费详情数据异常", e);
            throw new BusinessException("新增业务费详情数据失败：" + e.getMessage());
        }
    }

    private Integer createSingleDirectionBusinessFee(BusinessFeeCreateDTO createDTO,
                                                     Integer operatorUserId,
                                                     List<BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO> itemDTOs,
                                                     String settlementType,
                                                     String codeSuffix) {
        BusinessFeeHeader header = new BusinessFeeHeader();
        header.setBusinessCode(generateBusinessFeeCode() + codeSuffix);
        header.setStatus("待审核");
        header.setCreatorId(operatorUserId);
        Employee creator = employeeMapper.selectById(operatorUserId);
        header.setCreatorName(creator != null ? creator.getEmployeeName() : "");
        header.setCreateTime(LocalDateTime.now());
        header.setIsLocked(false);
        header.setVersion(0);
        updateHeaderFields(header, createDTO, operatorUserId);
        header.setSettlementType(settlementType);
        header.setSettlementAmount(calculateSettlementAmount(itemDTOs));
        header.setReceivedAmount(java.math.BigDecimal.ZERO);
        header.setUpdateTime(LocalDateTime.now());
        header.setUpdateUserId(operatorUserId);

        int insertCount = businessFeeHeaderMapper.insert(header);
        if (insertCount == 0) {
            throw new BusinessException("新增业务费主表失败");
        }
        Integer businessSeq = header.getBusinessSeq();
        log.info("单方向业务费主表创建成功，businessSeq={}, businessCode={}, settlementType={}, 明细数={}",
                businessSeq, header.getBusinessCode(), settlementType, itemDTOs.size());

        if (createDTO.getSettlementRels() != null) {
            processSettlementRels(businessSeq, createDTO.getSettlementRels(), operatorUserId);
        }
        processBusinessFeeItems(businessSeq, itemDTOs, operatorUserId);
        if (createDTO.getOutOfScopeServices() != null) {
            processOutOfScopeServices(businessSeq, createDTO.getOutOfScopeServices(), operatorUserId);
        }
        return businessSeq;
    }

    private java.math.BigDecimal calculateSettlementAmount(List<BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO> itemDTOs) {
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return itemDTOs.stream()
                .map(i -> i.getPayableAmount() != null ? i.getPayableAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBusinessFeeDetail(Integer businessSeq, BusinessFeeDetailUpdateDTO updateDTO, Integer operatorUserId) {
        log.info("修改业务费详情数据，businessSeq={}, operatorUserId={}", businessSeq, operatorUserId);

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        try {
            BusinessFeeHeader header = businessFeeHeaderMapper.selectById(businessSeq);
            if (header == null) {
                throw new BusinessException("业务费记录不存在，businessSeq=" + businessSeq);
            }

            if (!admin) {
                EmployeePermission permission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);
                if (permission != null) {
                    if (permission.getCanEdit() == null || permission.getCanEdit() == 0) {
                        throw new BusinessException("您没有编辑权限");
                    }
                    if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                        if (!Objects.equals(header.getCreatorId(), currentUserId)) {
                            throw new BusinessException("您只能编辑自己创建的业务费结算单");
                        }
                    }
                } else {
                    throw new BusinessException("您没有操作权限");
                }
            }

            if (updateDTO.getVersion() != null && !updateDTO.getVersion().equals(header.getVersion())) {
                throw new BusinessException("数据已被其他用户修改，请刷新后重试");
            }
            if ("已审核".equals(header.getStatus()) || "已收款".equals(header.getStatus())) {
                throw new BusinessException("当前状态不允许修改");
            }

            validateItemDirectionsForUpdate(header, updateDTO.getBusinessFeeItems());

            String originalSettlementType = header.getSettlementType();
            updateHeaderFields(header, updateDTO, operatorUserId);
            header.setBusinessSeq(businessSeq);
            header.setSettlementType(originalSettlementType);
            header.setUpdateTime(LocalDateTime.now());
            header.setUpdateUserId(operatorUserId);

            int updateCount = businessFeeHeaderMapper.updateById(header);
            if (updateCount == 0) {
                throw new BusinessException("更新业务费主表失败，可能数据已被修改");
            }

            if (updateDTO.getSettlementRels() != null) {
                processSettlementRels(businessSeq, updateDTO.getSettlementRels(), operatorUserId);
            }

            List<BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO> itemDTOs = updateDTO.getBusinessFeeItems();
            if (itemDTOs != null && !itemDTOs.isEmpty()) {
                processBusinessFeeItems(businessSeq, itemDTOs, operatorUserId);
            } else {
                QueryWrapper<BusinessFeeItem> deleteWrapper = new QueryWrapper<>();
                deleteWrapper.eq("业务序号", businessSeq);
                businessFeeItemMapper.delete(deleteWrapper);
                log.info("删除业务费所有明细，businessSeq={}", businessSeq);
            }

            if (updateDTO.getOutOfScopeServices() != null) {
                processOutOfScopeServices(businessSeq, updateDTO.getOutOfScopeServices(), operatorUserId);
            }

            // 记录数据变更日志
            try {
                currentUserId = SecurityUtil.getCurrentUserId();
                BusinessFeeHeader newData = businessFeeHeaderMapper.selectById(businessSeq);
                BusinessFeeHeader oldData = new BusinessFeeHeader();
                oldData.setStatus(header.getStatus());
                // 复制关键字段用于对比
                oldData.setBusinessSeq(businessSeq);
                String logContent = "更新业务费：业务编号=" + businessSeq;
                logRecordService.recordDataChangeLog("业务费结算", "BUSINESS_FEE",
                        String.valueOf(businessSeq),
                        "更新",
                        logContent,
                        oldData, newData, currentUserId, null, true, null);
            } catch (Exception logEx) {
                log.warn("记录业务费更新数据变更日志失败，businessSeq={}", businessSeq, logEx);
            }

            log.info("修改业务费详情数据成功，businessSeq={}", businessSeq);
        } catch (BusinessException e) {
            log.error("修改业务费详情数据失败：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("修改业务费详情数据异常", e);
            throw new BusinessException("修改业务费详情数据失败：" + e.getMessage());
        }
    }

    /**
     * 生成业务结算单号：BF-YYYYMMDD-XXXXX
     * XXXXX 为当天递增序号，从 00001 开始
     */
    private String generateBusinessFeeCode() {
        LocalDate currentDate = LocalDate.now();
        String dateStr = currentDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        Integer maxSequence = businessFeeHeaderMapper.selectMaxDailySequence(currentDate);
        int nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;
        return "BF-" + dateStr + "-" + String.format("%05d", nextSequence);
    }

    /**
     * 处理价外服务表（全量比对：有 ID 则更新，无 ID 则新增，库中多余的则删除）
     *
     * @param businessSeq      业务序号
     * @param dtoList          前端传入的价外服务列表
     * @param operatorUserId   操作人编码
     */
    private void processOutOfScopeServices(Integer businessSeq,
                                           List<BusinessFeeDetailSaveBaseDTO.OutOfScopeServiceDTO> dtoList,
                                           Integer operatorUserId) {
        // 全量替换策略：先删除该业务序号下所有旧记录，再批量插入新记录
        int deleted = outOfScopeServiceMapper.deleteByBusiness("BUSINESS_FEE", businessSeq);
        log.info("删除旧价外服务记录，businessSeq={}, 删除数={}", businessSeq, deleted);

        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }

        for (BusinessFeeDetailSaveBaseDTO.OutOfScopeServiceDTO dto : dtoList) {
            OutOfScopeService entity = new OutOfScopeService();
            entity.setBusinessType("BUSINESS_FEE");
            entity.setBusinessId(businessSeq);
            // 项目和规格型号字段在 SQL 中为 NOT NULL，用空字符串兜底
            entity.setProject(dto.getProject() != null ? dto.getProject() : "");
            entity.setSpec(dto.getSpec() != null ? dto.getSpec() : "");
            entity.setUnit(dto.getBasicUnit() != null ? dto.getBasicUnit() : "");
            entity.setPlannedQuantity(dto.getPlannedQuantity());
            entity.setContractUnitPrice(dto.getContractUnitPrice());
            entity.setSettledQuantity(dto.getBasicSettlementQuantity());
            entity.setSettledUnitPrice(dto.getUnitPrice());
            entity.setSettledAmount(dto.getAmount());
            entity.setStatus("ACTIVE");
            entity.setRemark(dto.getRemark());
            entity.setCreatedBy(operatorUserId);
            entity.setUpdatedBy(operatorUserId);
            outOfScopeServiceMapper.insert(entity);
        }
        log.info("价外服务全量保存完成，businessSeq={}, 保存数={}", businessSeq, dtoList.size());
    }

    /**
     * 更新主表字段
     */
    private void updateHeaderFields(BusinessFeeHeader header, BusinessFeeDetailSaveBaseDTO saveDTO, Integer operatorUserId) {
        if (saveDTO.getBusinessContractId() != null) {
            header.setBusinessContractId(saveDTO.getBusinessContractId());
        }
        if (saveDTO.getBusinessContractNo() != null) {
            header.setBusinessContractNo(saveDTO.getBusinessContractNo());
        }
        if (saveDTO.getSalespersonId() != null) {
            header.setSalespersonId(saveDTO.getSalespersonId());
        }
        if (saveDTO.getSalespersonName() != null) {
            header.setSalespersonName(saveDTO.getSalespersonName());
        }
        if (saveDTO.getServiceCompanyName() != null) {
            header.setServiceCompanyName(saveDTO.getServiceCompanyName());
        }
        if (saveDTO.getSettlementType() != null) {
            header.setSettlementType(saveDTO.getSettlementType());
        }
        if (saveDTO.getSettlementAmount() != null) {
            header.setSettlementAmount(saveDTO.getSettlementAmount());
        }
        if (saveDTO.getReceivedAmount() != null) {
            header.setReceivedAmount(saveDTO.getReceivedAmount());
        }
        if (saveDTO.getPaymentDate() != null) {
            header.setPaymentDate(saveDTO.getPaymentDate());
        }
        if (saveDTO.getStatus() != null) {
            header.setStatus(saveDTO.getStatus());
        }
        if (saveDTO.getAuditOpinion() != null) {
            header.setAuditOpinion(saveDTO.getAuditOpinion());
        }
        if (saveDTO.getRemark() != null) {
            header.setRemark(saveDTO.getRemark());
        }
        if (saveDTO.getIsLocked() != null) {
            header.setIsLocked(saveDTO.getIsLocked());
        }
    }

    /**
     * 处理业务费明细表（全量比对：增删改）
     */
    private void processBusinessFeeItems(Integer businessSeq,
                                         List<BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO> itemDTOs,
                                         Integer operatorUserId) {
        log.info("处理业务费明细表，businessSeq={}, 前端传入明细数={}", businessSeq, itemDTOs.size());

        // 1. 查询数据库中现有的所有明细
        QueryWrapper<BusinessFeeItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("业务序号", businessSeq);
        List<BusinessFeeItem> existingItems = businessFeeItemMapper.selectList(queryWrapper);
        Set<Integer> existingItemSeqs = existingItems.stream()
                .map(BusinessFeeItem::getItemSeq)
                .collect(Collectors.toSet());

        log.info("数据库现有明细数={}, itemSeqs={}", existingItems.size(), existingItemSeqs);

        // 2. 处理前端传来的明细（新增或更新）
        Set<Integer> processedItemSeqs = new HashSet<>();

        for (BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO itemDTO : itemDTOs) {
            Integer itemSeq = itemDTO.getItemSeq();

            if (itemSeq != null && itemSeq > 0) {
                // 更新明细
                BusinessFeeItem existingItem = businessFeeItemMapper.selectById(itemSeq);
                if (existingItem == null) {
                    throw new BusinessException("业务费明细不存在，itemSeq=" + itemSeq);
                }
                if (!existingItem.getBusinessSeq().equals(businessSeq)) {
                    throw new BusinessException("业务费明细不属于当前业务费，itemSeq=" + itemSeq);
                }
                updateItemFields(existingItem, itemDTO, operatorUserId);
                existingItem.setUpdateTime(LocalDateTime.now());
                existingItem.setUpdaterId(operatorUserId);
                int updateCount = businessFeeItemMapper.updateById(existingItem);
                if (updateCount == 0) {
                    throw new BusinessException("更新业务费明细失败，itemSeq=" + itemSeq);
                }
                // 处理总价包干子表
                processWasteInfoList(itemSeq, itemDTO.getWasteInfoList(), operatorUserId);
                processedItemSeqs.add(itemSeq);
                log.info("更新业务费明细成功，itemSeq={}", itemSeq);
            } else {
                // 新增明细
                BusinessFeeItem newItem = new BusinessFeeItem();
                newItem.setBusinessSeq(businessSeq);
                updateItemFields(newItem, itemDTO, operatorUserId);
                newItem.setCreatorId(operatorUserId);
                newItem.setCreateTime(LocalDateTime.now());
                newItem.setUpdaterId(operatorUserId);
                newItem.setUpdateTime(LocalDateTime.now());
                newItem.setVersion(0);
                int insertCount = businessFeeItemMapper.insert(newItem);
                if (insertCount == 0) {
                    throw new BusinessException("新增业务费明细失败");
                }
                // 处理总价包干子表
                processWasteInfoList(newItem.getItemSeq(), itemDTO.getWasteInfoList(), operatorUserId);
                processedItemSeqs.add(newItem.getItemSeq());
                log.info("新增业务费明细成功，itemSeq={}", newItem.getItemSeq());
            }
        }

        // 3. 删除数据库中存在但前端没传的明细及其子表
        Set<Integer> itemSeqsToDelete = new HashSet<>(existingItemSeqs);
        itemSeqsToDelete.removeAll(processedItemSeqs);
        if (!itemSeqsToDelete.isEmpty()) {
            // 先删子表
            for (Integer seq : itemSeqsToDelete) {
                QueryWrapper<BusinessFeeItemWasteInfo> wiq = new QueryWrapper<>();
                wiq.eq("明细序号", seq);
                businessFeeItemWasteInfoMapper.delete(wiq);
            }
            businessFeeItemMapper.deleteBatchIds(itemSeqsToDelete);
            log.info("删除业务费明细，数量={}, itemSeqs={}", itemSeqsToDelete.size(), itemSeqsToDelete);
        }

        log.info("处理业务费明细表完成，新增/更新={}, 删除={}", processedItemSeqs.size(), itemSeqsToDelete.size());
    }

    /**
     * 处理总价包干危废信息子表（BUSINESS_FEE_ITEM_WASTE_INFO）
     * 全量替换：先删后插
     */
    private void processWasteInfoList(Integer itemSeq,
                                       List<BusinessFeeDetailSaveBaseDTO.WasteInfoSaveDTO> wasteInfoList,
                                       Integer operatorUserId) {
        // 先删除该明细序号下的所有子表记录
        QueryWrapper<BusinessFeeItemWasteInfo> delWrapper = new QueryWrapper<>();
        delWrapper.eq("明细序号", itemSeq);
        businessFeeItemWasteInfoMapper.delete(delWrapper);

        if (wasteInfoList == null || wasteInfoList.isEmpty()) {
            return;
        }

        // 插入新数据
        int order = 1;
        for (BusinessFeeDetailSaveBaseDTO.WasteInfoSaveDTO wiDTO : wasteInfoList) {
            BusinessFeeItemWasteInfo wi = new BusinessFeeItemWasteInfo();
            wi.setItemSeq(itemSeq);
            wi.setRowOrder(wiDTO.getRowOrder() != null ? wiDTO.getRowOrder() : order);
            wi.setSourceWasteDetailIds(wiDTO.getSourceWasteDetailIds());
            wi.setWasteCategory(wiDTO.getWasteCategory());
            wi.setWasteCode(wiDTO.getWasteCode());
            wi.setWasteName(wiDTO.getWasteName());
            wi.setCreateTime(LocalDateTime.now());
            wi.setUpdateTime(LocalDateTime.now());
            wi.setVersion(0);
            businessFeeItemWasteInfoMapper.insert(wi);
            order++;
        }
        log.info("处理总价包干危废信息子表完成，itemSeq={}, 数量={}", itemSeq, wasteInfoList.size());
    }

    /**
     * 批量删除业务费明细对应的危废信息子表记录
     */
    private void deleteWasteInfoByItemSeqs(List<Integer> itemSeqs) {
        if (itemSeqs == null || itemSeqs.isEmpty()) {
            return;
        }
        for (Integer itemSeq : itemSeqs) {
            if (itemSeq == null) {
                continue;
            }
            QueryWrapper<BusinessFeeItemWasteInfo> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("明细序号", itemSeq);
            businessFeeItemWasteInfoMapper.delete(deleteWrapper);
        }
    }

    /**
     * 处理危废结算单关联表（全量比对：增删）
     */
    private void processSettlementRels(Integer businessSeq,
                                        List<BusinessFeeDetailSaveBaseDTO.SettlementRelSaveDTO> relDTOs,
                                        Integer operatorUserId) {
        log.info("处理危废结算单关联表，businessSeq={}, 前端传入数={}", businessSeq, relDTOs.size());

        // 查询现有关联
        List<BusinessFeeSettlementRel> existing =
                businessFeeSettlementRelMapper.selectByBusinessSeq(businessSeq);
        Set<Integer> existingIds = existing.stream()
                .filter(Objects::nonNull)
                .map(BusinessFeeSettlementRel::getRelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> existingSettlementIds = existing.stream()
                .filter(Objects::nonNull)
                .map(BusinessFeeSettlementRel::getSettlementId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> processedSettlementIds = new HashSet<>(existingSettlementIds);

        Set<Integer> processedRelIds = new HashSet<>();

        for (BusinessFeeDetailSaveBaseDTO.SettlementRelSaveDTO relDTO : relDTOs) {
            if (relDTO.getRelId() != null && existingIds.contains(relDTO.getRelId())) {
                // 已存在，仅更新备注
                BusinessFeeSettlementRel rel = existing.stream()
                        .filter(Objects::nonNull)
                        .filter(r -> Objects.equals(r.getRelId(), relDTO.getRelId()))
                        .findFirst().orElse(null);
                if (rel != null) {
                    if (relDTO.getRemark() != null) {
                        rel.setRemark(relDTO.getRemark());
                        businessFeeSettlementRelMapper.updateById(rel);
                    }
                    if (rel.getSettlementId() != null) {
                        processedSettlementIds.add(rel.getSettlementId());
                    }
                }
                processedRelIds.add(relDTO.getRelId());
            } else if (relDTO.getSettlementId() != null
                    && !existingSettlementIds.contains(relDTO.getSettlementId())
                    && !processedSettlementIds.contains(relDTO.getSettlementId())) {
                // 新增关联（确保 settlementId 不重复，避免唯一键冲突）
                BusinessFeeSettlementRel rel = new BusinessFeeSettlementRel();
                rel.setBusinessSeq(businessSeq);
                rel.setSettlementId(relDTO.getSettlementId());
                rel.setSettlementCode(relDTO.getSettlementCode());
                rel.setRemark(relDTO.getRemark());
                rel.setCreatorId(operatorUserId);
                rel.setCreateTime(LocalDateTime.now());
                try {
                    businessFeeSettlementRelMapper.insert(rel);
                    processedRelIds.add(rel.getRelId());
                    processedSettlementIds.add(relDTO.getSettlementId());
                    log.info("新增危废结算单关联成功，businessSeq={}, settlementId={}, relId={}",
                            businessSeq, relDTO.getSettlementId(), rel.getRelId());
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                        log.warn("危废结算单关联已存在，跳过插入，businessSeq={}, settlementId={}",
                                businessSeq, relDTO.getSettlementId());
                    } else {
                        throw e;
                    }
                }
            } else {
                // settlementId 已存在或前端传了重复的关联，跳过
                log.debug("跳过重复或已存在的危废结算单关联，businessSeq={}, settlementId={}",
                        businessSeq, relDTO.getSettlementId());
            }
        }

        // 删除不再关联的记录
        Set<Integer> toDelete = new HashSet<>(existingIds);
        toDelete.removeAll(processedRelIds);
        if (!toDelete.isEmpty()) {
            toDelete.forEach(relId -> businessFeeSettlementRelMapper.deleteById(relId));
            log.info("删除危废结算单关联记录，数量={}, relIds={}", toDelete.size(), toDelete);
        }
    }

    /**
     * 修改时校验明细方向必须与当前主表方向一致
     */
    private void validateItemDirectionsForUpdate(BusinessFeeHeader header,
                                                 List<BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO> itemDTOs) {
        if (header == null || itemDTOs == null || itemDTOs.isEmpty()) {
            return;
        }
        String expectedDirection = header.getSettlementType();
        if (!"RECEIVABLE".equals(expectedDirection) && !"PAYABLE".equals(expectedDirection)) {
            return;
        }
        for (BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO itemDTO : itemDTOs) {
            if (itemDTO == null || itemDTO.getPaymentDirection() == null) {
                continue;
            }
            if (!expectedDirection.equals(itemDTO.getPaymentDirection())) {
                throw new BusinessException(
                        "当前业务费单为" + ("RECEIVABLE".equals(expectedDirection) ? "收款" : "付款") +
                                "单，仅允许更新对应方向明细");
            }
        }
    }

    /**
     * 更新明细字段
     */
    private void updateItemFields(BusinessFeeItem item,
                                   BusinessFeeDetailSaveBaseDTO.BusinessFeeItemSaveDTO itemDTO,
                                   Integer operatorUserId) {
        if (itemDTO.getPaymentDirection() != null) {
            item.setPaymentDirection(itemDTO.getPaymentDirection());
        }
        if (itemDTO.getSettlementMode() != null) {
            item.setSettlementMode(itemDTO.getSettlementMode());
        }
        if (itemDTO.getBaseUnitPrice() != null) {
            item.setBaseUnitPrice(itemDTO.getBaseUnitPrice());
        }
        if (itemDTO.getValuableUnitPrice() != null) {
            item.setValuableUnitPrice(itemDTO.getValuableUnitPrice());
        }
        if (itemDTO.getWorthlessUnitPrice() != null) {
            item.setWorthlessUnitPrice(itemDTO.getWorthlessUnitPrice());
        }
        if (itemDTO.getContractBasePrice() != null) {
            item.setContractBasePrice(itemDTO.getContractBasePrice());
        }
        if (itemDTO.getValuableContractBasePrice() != null) {
            item.setValuableContractBasePrice(itemDTO.getValuableContractBasePrice());
        }
        if (itemDTO.getWorthlessContractBasePrice() != null) {
            item.setWorthlessContractBasePrice(itemDTO.getWorthlessContractBasePrice());
        }
        if (itemDTO.getIntermediaryFee() != null) {
            item.setIntermediaryFee(itemDTO.getIntermediaryFee());
        }
        if (itemDTO.getRebateRatio() != null) {
            item.setRebateRatio(itemDTO.getRebateRatio());
        }
        if (itemDTO.getPayableAmount() != null) {
            item.setPayableAmount(itemDTO.getPayableAmount());
        }
        if (itemDTO.getValuablePayableAmount() != null) {
            item.setValuablePayableAmount(itemDTO.getValuablePayableAmount());
        }
        if (itemDTO.getWorthlessPayableAmount() != null) {
            item.setWorthlessPayableAmount(itemDTO.getWorthlessPayableAmount());
        }
        if (itemDTO.getValuableWeight() != null) {
            item.setValuableWeight(itemDTO.getValuableWeight());
        }
        if (itemDTO.getWorthlessWeight() != null) {
            item.setWorthlessWeight(itemDTO.getWorthlessWeight());
        }
        if (itemDTO.getCargoSettlementAmount() != null) {
            item.setCargoSettlementAmount(itemDTO.getCargoSettlementAmount());
        }
        if (itemDTO.getEnableAuxAccounting() != null) {
            item.setEnableAuxAccounting(itemDTO.getEnableAuxAccounting());
        }
        if (itemDTO.getBasicQuantity() != null) {
            item.setBasicQuantity(itemDTO.getBasicQuantity());
        }
        if (itemDTO.getAuxiliaryQuantity() != null) {
            item.setAuxiliaryQuantity(itemDTO.getAuxiliaryQuantity());
        }
        if (itemDTO.getAuxiliaryUnit() != null) {
            item.setAuxiliaryUnit(itemDTO.getAuxiliaryUnit());
        }
    }

    /**
     * 审核业务费结算单（通过/驳回）
     * <p>
     * 通过时：
     * - BUSINESS_FEE_HEADER: 状态改为"已审核"，记录审核人编码、审核时间、审核意见
     * - OA_APPROVAL_RECORD: 状态改为"已通过"，记录审核人编码、审核人姓名、审核时间
     * <p>
     * 驳回时：
     * - BUSINESS_FEE_HEADER: 状态改为"已驳回"，记录审核人编码、审核时间、审核意见
     * - OA_APPROVAL_RECORD: 状态改为"已驳回"，记录审核人编码、审核人姓名、审核时间
     *
     * @param businessSeq 业务序号
     * @param auditResult 审核结果（通过/驳回）
     * @param auditOpinion 审核意见
     * @param operatorUserId 审核人编码
     * @param operatorUserName 审核人姓名
     * @param skipPermissionCheck 是否跳过权限检查（OA回调时使用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditBusinessFee(Integer businessSeq, String auditResult, String auditOpinion,
                                 Integer operatorUserId, String operatorUserName, boolean skipPermissionCheck) {
        log.info("审核业务费结算单，businessSeq={}, auditResult={}, operatorUserId={}", businessSeq, auditResult, operatorUserId);

        if (businessSeq == null) {
            throw new BusinessException("业务序号不能为空");
        }

        // 1. 查询业务费主表记录
        BusinessFeeHeader header = businessFeeHeaderMapper.selectById(businessSeq);
        if (header == null) {
            throw new BusinessException("业务费记录不存在，businessSeq=" + businessSeq);
        }

        // 2. 验证业务费状态：仅"审核中"状态可以审核
        if (!"审核中".equals(header.getStatus())) {
            throw new BusinessException("当前状态【" + header.getStatus() + "】不允许审核，仅审核中状态可进行审核操作");
        }

        // 3. 权限检查（OA回调时跳过）
        if (!skipPermissionCheck) {
            Integer currentUserId = getCurrentUserId();
            boolean admin = authService.isAdmin(currentUserId);

            if (!admin) {
                EmployeePermission permission = getEmployeePagePermission(currentUserId, BUSINESS_FEE_PAGE_CODE);
                if (permission == null) {
                    throw new BusinessException("您没有操作权限");
                }
                if (permission.getCanEdit() == null || permission.getCanEdit() == 0) {
                    throw new BusinessException("您没有编辑权限");
                }
            }
        }

        // 4. 验证审核结果
        if (!"通过".equals(auditResult) && !"驳回".equals(auditResult)) {
            throw new BusinessException("审核结果无效，仅支持【通过】或【驳回】操作");
        }

        // 5. 查找对应的OA审核记录
        OaApprovalRecord approvalRecord = oaApprovalRecordService.findPendingBySource("BUSINESS_FEE", businessSeq);
        if (approvalRecord == null) {
            throw new BusinessException("未找到对应的OA审核记录，无法完成审核操作");
        }

        // 6. 判断审核状态
        boolean isApproved = "通过".equals(auditResult);
        String newBusinessStatus = isApproved ? "已审核" : "已驳回";
        String newOaStatus = isApproved ? "已通过" : "已驳回";
        LocalDateTime now = LocalDateTime.now();

        // 7. 更新业务费主表（BLOOM_FEE_HEADER）
        header.setStatus(newBusinessStatus);
        header.setAuditorId(operatorUserId);
        header.setAuditTime(now);
        header.setAuditOpinion(auditOpinion);
        header.setUpdateTime(now);
        header.setUpdateUserId(operatorUserId);

        int updateCount = businessFeeHeaderMapper.updateById(header);
        if (updateCount == 0) {
            throw new BusinessException("更新业务费状态失败");
        }
        log.info("业务费主表状态更新成功，businessSeq={}, 原状态=审核中, 新状态={}", businessSeq, newBusinessStatus);

        // 8. 更新OA审核记录表（OA_APPROVAL_RECORD）
        oaApprovalRecordService.approve(
                approvalRecord.getApprovalRecordId(),
                "BUSINESS_FEE",
                businessSeq,
                auditResult,
                auditOpinion,
                operatorUserId,
                operatorUserName
        );

        // 9. 记录数据变更日志
        try {
            Integer currentUserId = operatorUserId;
            BusinessFeeHeader newData = businessFeeHeaderMapper.selectById(businessSeq);
            BusinessFeeHeader oldData = new BusinessFeeHeader();
            oldData.setBusinessSeq(businessSeq);
            oldData.setStatus("审核中");

            String logContent = String.format("审核业务费：业务编号=%d，审核结果=%s，审核意见=%s",
                    businessSeq, auditResult, auditOpinion != null ? auditOpinion : "");
            logRecordService.recordDataChangeLog("业务费结算", "BUSINESS_FEE",
                    String.valueOf(businessSeq),
                    isApproved ? "审核通过" : "审核驳回",
                    logContent,
                    oldData, newData, currentUserId, null, true, null);
        } catch (Exception logEx) {
            log.warn("记录业务费审核数据变更日志失败，businessSeq={}", businessSeq, logEx);
        }

        log.info("业务费结算单审核完成，businessSeq={}, 审核结果={}, 新业务状态={}, 新OA状态={}",
                businessSeq, auditResult, newBusinessStatus, newOaStatus);
    }
}

