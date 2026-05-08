package com.erp.service.finance.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.finance.dto.*;
import com.erp.controller.settlement.dto.SettlementInvoiceSummaryResponse;
import com.erp.controller.settlement.dto.SettlementQueryResultDTO;
import com.erp.entity.contract.Contract;
import com.erp.entity.finance.InvoiceNotice;
import com.erp.entity.system.Employee;
import com.erp.mapper.contract.ContractMapper;
import com.erp.mapper.finance.InvoiceNoticeInvoiceMapper;
import com.erp.mapper.finance.InvoiceNoticeMapper;
import com.erp.mapper.finance.InvoiceMapper;
import com.erp.mapper.finance.SettlementMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.mapper.oa.OaApprovalRecordMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.oa.OaApprovalRecordService;
import com.erp.service.finance.InvoiceNoticeService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.entity.finance.Invoice;
import com.erp.entity.finance.InvoiceNoticeInvoice;
import com.erp.entity.settlement.Settlement;
import com.erp.entity.settlement.SettlementFundTransactionRel;
import com.erp.entity.settlement.SettlementInvoiceRel;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.util.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 开票通知单服务实现
 *
 * @author ERP System
 * @date 2026-01-06
 */
@Slf4j
@Service
public class InvoiceNoticeServiceImpl implements InvoiceNoticeService {

    @Autowired
    private InvoiceNoticeMapper invoiceNoticeMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private InvoiceNoticeInvoiceMapper invoiceNoticeInvoiceMapper;

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Autowired
    private SettlementMapper settlementMapper;

    @Autowired
    private com.erp.mapper.finance.SettlementInvoiceRelMapper settlementInvoiceRelMapper;

    @Autowired
    private com.erp.mapper.finance.SettlementFundTransactionRelMapper settlementFundTransactionRelMapper;

    @Autowired(required = false)
    private ILogRecordService logRecordService;

    @Autowired(required = false)
    private MessageNotificationService messageNotificationService;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private OaApprovalRecordService oaApprovalRecordService;

    @Autowired
    private OaApprovalRecordMapper oaApprovalRecordMapper;

    @Autowired
    private AuthService authService;

    private static final Set<String> ALLOW_UPDATE_STATUSES =
            new HashSet<>(Arrays.asList("待审核", "已驳回", "审核中"));

    private static final String INVOICE_NOTICE_PAGE_CODE = "业务管理:开票通知:页面";

    private static final String OA_SOURCE_TABLE = "INVOICE_NOTICE";

    private static final String OA_SOURCE_TABLE_NAME = "开票通知单";

    /**
     * 创建或重置OA审核记录
     */
    private void submitOaApprovalForInvoiceNotice(InvoiceNotice invoiceNotice, Integer submitterId, String submitterName) {
        OaApprovalRecord latestRecord = oaApprovalRecordService.findLatestBySource(OA_SOURCE_TABLE, invoiceNotice.getNoticeId());
        if (latestRecord == null) {
            oaApprovalRecordService.submit(
                    OA_SOURCE_TABLE,
                    invoiceNotice.getNoticeId(),
                    OA_SOURCE_TABLE_NAME,
                    invoiceNotice.getNoticeNo(),
                    "开票通知单审核：" + invoiceNotice.getNoticeNo(),
                    submitterId,
                    submitterName
            );
            return;
        }

        if ("已驳回".equals(latestRecord.getApprovalStatus())) {
            oaApprovalRecordService.reactivateRejectedRecord(
                    OA_SOURCE_TABLE,
                    invoiceNotice.getNoticeId(),
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
     * 获取员工的页面权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        try {
            Permission permission = permissionMapper.selectOne(
                    new LambdaQueryWrapper<Permission>()
                            .eq(Permission::getPermissionCode, pageCode)
                            .eq(Permission::getPermissionTypeId, 2)
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
            log.error("获取员工页面权限配置失败：employeeId=" + employeeId + ", pageCode=" + pageCode, e);
            return null;
        }
    }

    /**
     * 校验写操作权限：canEdit 与 operateScope
     *
     * <p>动作权限（RequireActionPermission）已在切面拦截，这里补充页面级配置的强约束。</p>
     */
    private void checkPageOperatePermission(Integer currentUserId, InvoiceNotice invoiceNotice, String actionName) {
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        if (invoiceNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }
        // 超级管理员不受页面级数据范围限制
        if (authService.isAdmin(currentUserId)) {
            return;
        }

        EmployeePermission permission = getEmployeePagePermission(currentUserId, INVOICE_NOTICE_PAGE_CODE);
        if (permission == null) {
            // 未配置时按默认 ALL/可编辑 处理（与 AuthServiceImpl 的默认值对齐）
            return;
        }

        if (permission.getCanEdit() != null && permission.getCanEdit() == 0) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "当前账号为只读权限，无法" + actionName);
        }

        if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
            Integer creatorId = invoiceNotice.getCreateUserId();
            if (creatorId == null || !creatorId.equals(currentUserId)) {
                throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "仅允许操作自己创建的开票通知单");
            }
        }
    }

    /**
     * 校验查看权限：viewScope=SELF 时不可查看他人数据（详情/关联相关查询等）
     */
    private void checkPageViewPermission(Integer currentUserId, InvoiceNotice invoiceNotice) {
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        if (invoiceNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }
        if (authService.isAdmin(currentUserId)) {
            return;
        }
        EmployeePermission permission = getEmployeePagePermission(currentUserId, INVOICE_NOTICE_PAGE_CODE);
        if (permission == null) {
            return;
        }
        if ("SELF".equalsIgnoreCase(permission.getViewScope())) {
            Integer creatorId = invoiceNotice.getCreateUserId();
            if (creatorId == null || !creatorId.equals(currentUserId)) {
                throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "仅允许查看自己创建的开票通知单");
            }
        }
    }


    @Override
    public IPage<InvoiceNoticePageResponse> getInvoiceNoticePageList(InvoiceNoticePageRequest request) {
        log.info("查询开票通知单分页列表，current={}, size={}", request.getCurrent(), request.getSize());

        // 使用 ViewScopeHelper 解析视图范围
        String viewScope = ViewScopeHelper.resolveViewScope(INVOICE_NOTICE_PAGE_CODE, request.getViewScope());

        Integer creatorFilter = null;
        // SELF 模式需要添加创建人过滤条件
        if (ViewScopeHelper.isSelfScope(viewScope)) {
            Integer currentUserId = getCurrentUserId();
            creatorFilter = currentUserId;
        }
        // ALL 模式不添加限制，查询全部数据

        // 构建分页对象
        Page<InvoiceNoticePageResponse> page = new Page<>(request.getCurrent(), request.getSize());

        // 执行分页查询（使用自定义SQL，支持关联查询结算单信息）
        IPage<InvoiceNoticePageResponse> responsePage = invoiceNoticeMapper.selectPageWithSettlement(page, request, creatorFilter);

        log.info("查询开票通知单分页列表成功，total={}", responsePage.getTotal());
        return responsePage;
    }

    /**
     * 将前端排序字段名映射到数据库列名
     */
    private String mapSortFieldToDbColumn(String sortField) {
        if (sortField == null) {
            return null;
        }
        // 字段名映射：前端字段名 -> 数据库字段名
        switch (sortField) {
            case "noticeNo":
                return "开票通知单号";
            case "contractNo":
                return "合同号";
            case "contractName":
                return "合同名称";
            case "customerName":
                return "客户名称";
            case "status":
                return "状态";
            case "mainSettlementNo":
                return "主结算单编号";
            case "invoiceType":
                return "开票类型";
            case "applicantName":
                return "申请人姓名";
            case "approverName":
                return "审批人姓名";
            case "handlerName":
                return "办理人姓名";
            case "createTime":
            case "createdAt":
                return "创建时间";
            case "issuedAt":
                return "开票完成时间";
            case "invoiceCount":
                return "已开票张数";
            case "totalAmount":
                return "已开票价税合计";
            default:
                log.warn("未知的排序字段：{}", sortField);
                return null;
        }
    }

    @Override
    public InvoiceNoticeDetailResponse getInvoiceNoticeDetail(Integer noticeId) {
        log.info("获取开票通知单详情，noticeId={}", noticeId);

        Integer currentUserId = getCurrentUserId();
        InvoiceNotice invoiceNotice = invoiceNoticeMapper.selectById(noticeId);
        if (invoiceNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }
        // viewScope=SELF 强校验（避免绕过列表过滤直接访问详情）
        checkPageViewPermission(currentUserId, invoiceNotice);

        // 使用连表查询获取详情（包含关联的发票列表和客户信息）
        InvoiceNoticeDetailResponse response = invoiceNoticeMapper.selectDetailWithInvoices(noticeId);
        if (response == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }

        // 设置前端需要的字段名（createdAt和updatedAt）
        if (response.getCreateTime() != null) {
            response.setCreatedAt(response.getCreateTime());
        }
        if (response.getUpdateTime() != null) {
            response.setUpdatedAt(response.getUpdateTime());
        }

        // 如果发票列表为null，初始化为空列表
        if (response.getInvoices() == null) {
            response.setInvoices(new java.util.ArrayList<>());
        }

        log.info("获取开票通知单详情成功，noticeId={}, 关联发票数量={}", noticeId, response.getInvoices().size());
        return response;
    }

    /**
     * 转换为分页响应对象
     */
    private InvoiceNoticePageResponse convertToPageResponse(InvoiceNotice invoiceNotice) {
        InvoiceNoticePageResponse response = new InvoiceNoticePageResponse();
        BeanUtils.copyProperties(invoiceNotice, response);
        // 字段名映射
        response.setNoticeId(invoiceNotice.getNoticeId());
        response.setNoticeNo(invoiceNotice.getNoticeNo());
        response.setContractId(invoiceNotice.getContractId());
        response.setContractNo(invoiceNotice.getContractNo());
        response.setContractName(invoiceNotice.getContractName());
        response.setCustomerId(invoiceNotice.getCustomerId());
        response.setCustomerName(invoiceNotice.getCustomerName());
        response.setMainSettlementId(invoiceNotice.getMainSettlementId());
        response.setMainSettlementCode(invoiceNotice.getMainSettlementId() != null ? invoiceNotice.getMainSettlementId().toString() : null);
        response.setBoundSettlementSummary(invoiceNotice.getBoundSettlementSummary());
        response.setInvoiceType(invoiceNotice.getInvoiceType());
        response.setRemark(invoiceNotice.getRemark());
        response.setStatus(invoiceNotice.getStatus());
        response.setApplicantId(invoiceNotice.getApplicantId());
        response.setApplicantName(invoiceNotice.getApplicantName());
        response.setApproverId(invoiceNotice.getApproverId());
        response.setApproverName(invoiceNotice.getApproverName());
        response.setHandlerId(invoiceNotice.getHandlerId());
        response.setHandlerName(invoiceNotice.getHandlerName());
        response.setInvoiceCount(invoiceNotice.getInvoiceCount());
        response.setTotalAmount(invoiceNotice.getTotalAmount());
        response.setIssuedAt(invoiceNotice.getIssuedAt());
        response.setCreateTime(invoiceNotice.getCreateTime());
        response.setCreateUserId(invoiceNotice.getCreateUserId());
        response.setUpdateTime(invoiceNotice.getUpdateTime());
        response.setUpdateUserId(invoiceNotice.getUpdateUserId());
        response.setVersion(invoiceNotice.getVersion());
        return response;
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 生成开票通知单号
     * 规则：KPTZ-YYYYMMDD-XXXX（4位序号）
     */
    private String generateNoticeNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "KPTZ-" + datePart + "-";

        // 查询当日当前最大编号
        String maxNoticeNo = invoiceNoticeMapper.selectMaxNoticeNoByPrefix(prefix);

        int nextSeq = 1;
        if (StrUtil.isNotBlank(maxNoticeNo) && maxNoticeNo.length() > prefix.length()) {
            // 截取末尾4位序号并递增
            try {
                String seqPart = maxNoticeNo.substring(maxNoticeNo.length() - 4);
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (Exception e) {
                log.warn("解析最大开票通知单号序号失败：{}", maxNoticeNo, e);
            }
        }

        String noticeNo = prefix + String.format("%04d", nextSeq);

        // 若并发导致重复，则继续自增直到唯一或达到重试上限
        int retryCount = 0;
        while (invoiceNoticeMapper.countByNoticeNo(noticeNo) > 0 && retryCount < 20) {
            nextSeq++;
            noticeNo = prefix + String.format("%04d", nextSeq);
            retryCount++;
        }

        if (retryCount >= 20) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成开票通知单号失败，请重试");
        }

        log.debug("生成开票通知单号：{}", noticeNo);
        return noticeNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvoiceNoticeCreateResponse createInvoiceNotice(InvoiceNoticeCreateRequest request) {
        log.info("创建开票通知单，contractId={}, status={}", request.getContractId(), request.getStatus());

        Integer currentUserId = getCurrentUserId();

        // 验证必填字段
        if (request.getContractId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "合同ID不能为空");
        }

        // 验证合同是否存在
        Contract contract = contractMapper.selectById(request.getContractId());
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "合同不存在");
        }

        // 确定状态：默认创建为待审核状态
        String status = "待审核";

        // 生成通知单号
        String noticeNo = generateNoticeNo();

        // 获取当前用户姓名
        Employee employee = employeeMapper.selectById(currentUserId);
        String applicantName = employee != null ? employee.getEmployeeName() : null;

        // 创建实体对象并保存到数据库
        InvoiceNotice invoiceNotice = new InvoiceNotice();
        invoiceNotice.setNoticeNo(noticeNo);
        invoiceNotice.setContractId(request.getContractId());
        invoiceNotice.setContractNo(request.getContractNo() != null ? request.getContractNo() : contract.getContractNo());
        invoiceNotice.setContractName(request.getContractName() != null ? request.getContractName() : contract.getPartyAName());
        invoiceNotice.setCustomerId(request.getCustomerId());
        invoiceNotice.setCustomerName(request.getCustomerName());
        invoiceNotice.setMainSettlementId(request.getMainSettlementId());
        invoiceNotice.setBoundSettlementSummary(request.getBoundSettlementSummary());
        invoiceNotice.setInvoiceType(request.getInvoiceType() != null ? request.getInvoiceType() : "开票");
        invoiceNotice.setRemark(request.getRemark());
        invoiceNotice.setStatus(status);
        invoiceNotice.setApplicantId(currentUserId);
        invoiceNotice.setApplicantName(applicantName);
        invoiceNotice.setCreateTime(LocalDateTime.now());
        invoiceNotice.setUpdateTime(LocalDateTime.now());
        invoiceNotice.setCreateUserId(currentUserId);
        invoiceNotice.setUpdateUserId(currentUserId);
        // version字段由BaseEntity的@Version注解自动处理，不需要手动设置
        invoiceNoticeMapper.insert(invoiceNotice);

        // 返回结果
        InvoiceNoticeCreateResponse response = new InvoiceNoticeCreateResponse();
        response.setNoticeId(invoiceNotice.getNoticeId());
        response.setNoticeNo(invoiceNotice.getNoticeNo());

        log.info("创建开票通知单成功，noticeNo={}, status={}", noticeNo, status);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoiceNotice(Integer noticeId, InvoiceNoticeUpdateRequest request) {
        log.info("更新开票通知单，noticeId={}", noticeId);

        Integer currentUserId = getCurrentUserId();

        // 保存旧数据用于日志记录
        InvoiceNoticeDetailResponse oldDetail = null;
        try {
            oldDetail = getInvoiceNoticeDetail(noticeId);
        } catch (Exception e) {
            log.warn("查询开票通知单旧数据失败，将跳过数据变更日志记录，noticeId={}", noticeId, e);
        }

        // 查询通知单
        InvoiceNotice invoiceNotice = invoiceNoticeMapper.selectById(noticeId);
        if (invoiceNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }

        // operateScope/canEdit 强校验
        checkPageOperatePermission(currentUserId, invoiceNotice, "修改");

        // 只有允许的状态可以更新（待审核、已驳回、审核中）
        if (!ALLOW_UPDATE_STATUSES.contains(invoiceNotice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不允许更新");
        }

        // 验证合同是否存在
        Contract contract = contractMapper.selectById(request.getContractId());
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "合同不存在");
        }

        // 更新字段
        invoiceNotice.setContractId(request.getContractId());
        invoiceNotice.setContractNo(request.getContractNo() != null ? request.getContractNo() : contract.getContractNo());
        invoiceNotice.setContractName(request.getContractName() != null ? request.getContractName() : contract.getPartyAName());
        invoiceNotice.setCustomerId(request.getCustomerId());
        invoiceNotice.setCustomerName(request.getCustomerName());
        invoiceNotice.setMainSettlementId(request.getMainSettlementId());
        invoiceNotice.setBoundSettlementSummary(request.getBoundSettlementSummary());
        invoiceNotice.setInvoiceType(request.getInvoiceType());
        invoiceNotice.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getStatus())) {
            invoiceNotice.setStatus(request.getStatus());
        }
        invoiceNotice.setUpdateTime(LocalDateTime.now());
        invoiceNotice.setUpdateUserId(currentUserId);
        // version字段由BaseEntity的@Version注解自动处理，不需要手动设置
        int rows = invoiceNoticeMapper.updateById(invoiceNotice);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(),
                "更新开票通知单失败：记录已被其他用户修改");
        }

        // 记录数据变更日志
        try {
            InvoiceNoticeDetailResponse newDetail = getInvoiceNoticeDetail(noticeId);
            String logContent = "更新开票通知单：通知单号=" + invoiceNotice.getNoticeNo();
            if (logRecordService != null && oldDetail != null) {
                logRecordService.recordDataChangeLog("开票通知单管理", "INVOICE_NOTICE", String.valueOf(noticeId),
                        "更新", logContent, oldDetail, newDetail, currentUserId, null, true, null);
            }
        } catch (Exception logException) {
            // 日志记录失败不应该影响主业务流程
            log.warn("记录开票通知单更新数据变更日志失败", logException);
        }

        log.info("更新开票通知单成功，noticeId={}", noticeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoiceNotice(Integer noticeId, InvoiceNoticeSubmitRequest request) {
        log.info("提交审批，noticeId={}", noticeId);

        Integer currentUserId = getCurrentUserId();

        // 查询通知单
        InvoiceNotice invoiceNotice = invoiceNoticeMapper.selectById(noticeId);
        if (invoiceNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }

        checkPageOperatePermission(currentUserId, invoiceNotice, "提交审批");

        // 只有待审核或已驳回状态可以提交审核
        if (!"待审核".equals(invoiceNotice.getStatus()) && !"已驳回".equals(invoiceNotice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不允许提交审核");
        }

        // 获取当前用户姓名
        Employee employee = employeeMapper.selectById(currentUserId);
        String submitterName = employee != null ? employee.getEmployeeName() : null;

        // 更新状态为审核中
        String oldStatus = invoiceNotice.getStatus();
        invoiceNotice.setStatus("审核中");
        invoiceNotice.setUpdateTime(LocalDateTime.now());
        invoiceNotice.setUpdateUserId(currentUserId);
        // version字段由BaseEntity的@Version注解自动处理，不需要手动设置
        int rows = invoiceNoticeMapper.updateById(invoiceNotice);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(),
                "提交开票通知单失败：记录已被其他用户修改");
        }

        submitOaApprovalForInvoiceNotice(invoiceNotice, currentUserId, submitterName);

        // 记录数据变更日志（状态变更）
        try {
            InvoiceNotice oldNotice = new InvoiceNotice();
            oldNotice.setStatus(oldStatus);
            InvoiceNotice newNotice = new InvoiceNotice();
            newNotice.setStatus("审核中");
            if (logRecordService != null) {
                logRecordService.recordDataChangeLog("开票通知单管理", "INVOICE_NOTICE", String.valueOf(noticeId),
                        "提交审批", "提交开票通知单审批：通知单号=" + invoiceNotice.getNoticeNo(),
                        oldNotice, newNotice, currentUserId, null, true, null);
            }
        } catch (Exception logException) {
            log.warn("记录开票通知单提交审批数据变更日志失败", logException);
        }

        log.info("提交审批成功，noticeId={}", noticeId);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSubmitInvoiceNotice(List<Integer> noticeIds) {
        log.info("批量提交审核开票通知单，noticeIds={}", noticeIds);

        if (CollectionUtils.isEmpty(noticeIds)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "通知单ID列表不能为空");
        }

        List<Integer> distinctNoticeIds = noticeIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(distinctNoticeIds)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "通知单ID列表不能为空");
        }

        for (Integer noticeId : distinctNoticeIds) {
            submitInvoiceNotice(noticeId, new InvoiceNoticeSubmitRequest());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRevokeInvoiceNotice(List<Integer> noticeIds) {
        log.info("批量撤回审核开票通知单，noticeIds={}", noticeIds);

        if (CollectionUtils.isEmpty(noticeIds)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "通知单ID列表不能为空");
        }

        Integer currentUserId = getCurrentUserId();
        List<Integer> distinctNoticeIds = noticeIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(distinctNoticeIds)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "通知单ID列表不能为空");
        }

        List<InvoiceNotice> invoiceNotices = invoiceNoticeMapper.selectBatchIds(distinctNoticeIds);
        Map<Integer, InvoiceNotice> noticeMap = invoiceNotices.stream()
                .collect(Collectors.toMap(InvoiceNotice::getNoticeId, Function.identity()));

        for (Integer noticeId : distinctNoticeIds) {
            InvoiceNotice invoiceNotice = noticeMap.get(noticeId);
            if (invoiceNotice == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在：" + noticeId);
            }

            checkPageOperatePermission(currentUserId, invoiceNotice, "批量撤回审核");

            if (!"审核中".equals(invoiceNotice.getStatus())) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "仅审核中状态的开票通知单允许批量撤回");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (Integer noticeId : distinctNoticeIds) {
            InvoiceNotice invoiceNotice = noticeMap.get(noticeId);
            String oldStatus = invoiceNotice.getStatus();

            invoiceNotice.setStatus("待审核");
            invoiceNotice.setUpdateTime(now);
            invoiceNotice.setUpdateUserId(currentUserId);
            int rows = invoiceNoticeMapper.updateById(invoiceNotice);
            if (rows == 0) {
                throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(),
                        "批量撤回审核失败：记录已被其他用户修改");
            }

            OaApprovalRecord approvalRecord = oaApprovalRecordService.findLatestBySource(OA_SOURCE_TABLE, noticeId);
            if (approvalRecord != null) {
                Integer currentApprovalCount = approvalRecord.getApprovalCount() == null ? 0 : approvalRecord.getApprovalCount();
                approvalRecord.setApprovalStatus("已撤回");
                approvalRecord.setApprovalCount(Math.max(currentApprovalCount - 1, 0));
                approvalRecord.setApprovalTime(now);
                oaApprovalRecordMapper.updateById(approvalRecord);
            }

            try {
                InvoiceNotice oldNotice = new InvoiceNotice();
                oldNotice.setStatus(oldStatus);
                InvoiceNotice newNotice = new InvoiceNotice();
                newNotice.setStatus("待审核");
                if (logRecordService != null) {
                    logRecordService.recordDataChangeLog("开票通知单管理", "INVOICE_NOTICE", String.valueOf(noticeId),
                            "批量撤回审核", "批量撤回审核开票通知单：通知单号=" + invoiceNotice.getNoticeNo(),
                            oldNotice, newNotice, currentUserId, null, true, null);
                }
            } catch (Exception logException) {
                log.warn("记录开票通知单批量撤回数据变更日志失败", logException);
            }
        }

        log.info("批量撤回审核开票通知单成功，noticeIds={}", distinctNoticeIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInvoiceNotice(Integer noticeId) {
        log.info("撤销开票通知单，noticeId={}", noticeId);

        Integer currentUserId = getCurrentUserId();

        // 查询通知单
        InvoiceNotice invoiceNotice = invoiceNoticeMapper.selectById(noticeId);
        if (invoiceNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }

        checkPageOperatePermission(currentUserId, invoiceNotice, "撤销开票");

        // 只有待开票状态可以撤销开票
        String status = invoiceNotice.getStatus();
        if (!"待开票".equals(status)) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不允许撤销开票");
        }

        // 更新状态为待审核
        invoiceNotice.setStatus("待审核");
        invoiceNotice.setUpdateTime(LocalDateTime.now());
        invoiceNotice.setUpdateUserId(currentUserId);
        // version字段由BaseEntity的@Version注解自动处理，不需要手动设置
        int rows = invoiceNoticeMapper.updateById(invoiceNotice);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(),
                "撤销开票通知单失败：记录已被其他用户修改");
        }

        // 记录数据变更日志（状态变更：待开票 -> 待审核）
        try {
            InvoiceNotice oldNotice = new InvoiceNotice();
            oldNotice.setStatus("待开票");
            InvoiceNotice newNotice = new InvoiceNotice();
            newNotice.setStatus("待审核");
            if (logRecordService != null) {
                logRecordService.recordDataChangeLog("开票通知单管理", "INVOICE_NOTICE", String.valueOf(noticeId),
                        "撤销开票", "撤销开票通知单：通知单号=" + invoiceNotice.getNoticeNo(),
                        oldNotice, newNotice, currentUserId, null, true, null);
            }
        } catch (Exception logException) {
            log.warn("记录开票通知单撤销数据变更日志失败", logException);
        }

        log.info("撤销开票通知单成功，noticeId={}", noticeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveInvoiceNotice(Integer noticeId, InvoiceNoticeApproveRequest request) {
        log.info("审批通过开票通知单，noticeId={}", noticeId);

        Integer currentUserId = getCurrentUserId();

        // 查询通知单
        InvoiceNotice invoiceNotice = invoiceNoticeMapper.selectById(noticeId);
        if (invoiceNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }

        checkPageOperatePermission(currentUserId, invoiceNotice, "审批");

        // 只有待审核或审核中状态可以审批
        if (!"待审核".equals(invoiceNotice.getStatus()) && !"审核中".equals(invoiceNotice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不允许审批");
        }

        // 获取当前用户姓名
        Employee employee = employeeMapper.selectById(currentUserId);
        String approverName = employee != null ? employee.getEmployeeName() : null;

        // 更新状态为待开票，并保存审批信息
        String oldStatus = invoiceNotice.getStatus();
        invoiceNotice.setStatus("待开票");
        invoiceNotice.setApproverId(currentUserId);
        invoiceNotice.setApproverName(approverName);
        invoiceNotice.setApprovalOpinion(request.getOpinion());
        invoiceNotice.setUpdateTime(LocalDateTime.now());
        invoiceNotice.setUpdateUserId(currentUserId);
        // version字段由BaseEntity的@Version注解自动处理，不需要手动设置
        int rows = invoiceNoticeMapper.updateById(invoiceNotice);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(),
                "审批开票通知单失败：记录已被其他用户修改");
        }

        // 更新OA审核记录
        OaApprovalRecord oaApprovalRecord = oaApprovalRecordService.findPendingBySource(OA_SOURCE_TABLE, noticeId);
        if (oaApprovalRecord == null) {
            oaApprovalRecord = oaApprovalRecordService.findLatestBySource(OA_SOURCE_TABLE, noticeId);
        }
        if (oaApprovalRecord != null) {
            oaApprovalRecord.setApproverId(currentUserId);
            oaApprovalRecord.setApproverName(approverName);
            oaApprovalRecord.setApprovalStatus("已通过");
            oaApprovalRecord.setApprovalTime(LocalDateTime.now());
            oaApprovalRecordMapper.updateById(oaApprovalRecord);
        }

        // 记录数据变更日志（状态变更：审核中/待审核 -> 待开票）
        try {
            InvoiceNotice oldNotice = new InvoiceNotice();
            oldNotice.setStatus(oldStatus);
            InvoiceNotice newNotice = new InvoiceNotice();
            newNotice.setStatus("待开票");
            newNotice.setApproverId(currentUserId);
            newNotice.setApproverName(approverName);
            newNotice.setApprovalOpinion(request.getOpinion());
            if (logRecordService != null) {
                logRecordService.recordDataChangeLog("开票通知单管理", "INVOICE_NOTICE", String.valueOf(noticeId),
                        "审批通过", "审批通过开票通知单：通知单号=" + invoiceNotice.getNoticeNo() + "，审批意见=" + request.getOpinion(),
                        oldNotice, newNotice, currentUserId, null, true, null);
            }
        } catch (Exception logException) {
            log.warn("记录开票通知单审批通过数据变更日志失败", logException);
        }

        log.info("审批通过开票通知单成功，noticeId={}", noticeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectInvoiceNotice(Integer noticeId, InvoiceNoticeRejectRequest request) {
        log.info("驳回开票通知单，noticeId={}", noticeId);

        Integer currentUserId = getCurrentUserId();

        // 查询通知单
        InvoiceNotice invoiceNotice = invoiceNoticeMapper.selectById(noticeId);
        if (invoiceNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }

        checkPageOperatePermission(currentUserId, invoiceNotice, "驳回");

        // 只有待审核或审核中状态可以驳回
        if (!"待审核".equals(invoiceNotice.getStatus()) && !"审核中".equals(invoiceNotice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不允许驳回");
        }

        // 获取当前用户姓名
        Employee employee = employeeMapper.selectById(currentUserId);
        String approverName = employee != null ? employee.getEmployeeName() : null;

        // 更新状态为已驳回，并保存审批信息
        String oldStatus = invoiceNotice.getStatus();
        invoiceNotice.setStatus("已驳回");
        invoiceNotice.setApproverId(currentUserId);
        invoiceNotice.setApproverName(approverName);
        invoiceNotice.setApprovalOpinion(request.getReason());
        invoiceNotice.setUpdateTime(LocalDateTime.now());
        invoiceNotice.setUpdateUserId(currentUserId);
        // version字段由BaseEntity的@Version注解自动处理，不需要手动设置
        int rows = invoiceNoticeMapper.updateById(invoiceNotice);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(),
                "驳回开票通知单失败：记录已被其他用户修改");
        }

        // 更新OA审核记录
        OaApprovalRecord oaApprovalRecord = oaApprovalRecordService.findPendingBySource(OA_SOURCE_TABLE, noticeId);
        if (oaApprovalRecord == null) {
            oaApprovalRecord = oaApprovalRecordService.findLatestBySource(OA_SOURCE_TABLE, noticeId);
        }
        if (oaApprovalRecord != null) {
            oaApprovalRecord.setApproverId(currentUserId);
            oaApprovalRecord.setApproverName(approverName);
            oaApprovalRecord.setApprovalStatus("已驳回");
            oaApprovalRecord.setApprovalTime(LocalDateTime.now());
            oaApprovalRecordMapper.updateById(oaApprovalRecord);
        }

        // 记录数据变更日志（状态变更：审核中/待审核 -> 已驳回）
        try {
            InvoiceNotice oldNotice = new InvoiceNotice();
            oldNotice.setStatus(oldStatus);
            InvoiceNotice newNotice = new InvoiceNotice();
            newNotice.setStatus("已驳回");
            newNotice.setApproverId(currentUserId);
            newNotice.setApproverName(approverName);
            newNotice.setApprovalOpinion(request.getReason());
            if (logRecordService != null) {
                logRecordService.recordDataChangeLog("开票通知单管理", "INVOICE_NOTICE", String.valueOf(noticeId),
                        "驳回", "驳回开票通知单：通知单号=" + invoiceNotice.getNoticeNo() + "，驳回原因=" + request.getReason(),
                        oldNotice, newNotice, currentUserId, null, true, null);
            }
        } catch (Exception logException) {
            log.warn("记录开票通知单驳回数据变更日志失败", logException);
        }

        log.info("驳回开票通知单成功，noticeId={}", noticeId);
    }

    @Override
    public IPage<InvoiceNoticePageResponse> getInvoiceAssociateList(InvoiceNoticePageRequest request) {
        log.info("查询发票关联列表，current={}, size={}", request.getCurrent(), request.getSize());

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        Integer creatorFilter = null;
        if (!admin) {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, INVOICE_NOTICE_PAGE_CODE);
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                creatorFilter = currentUserId;
            }
        }

        // 构建分页对象
        Page<InvoiceNotice> page = new Page<>(request.getCurrent(), request.getSize());

        // 构建查询条件
        QueryWrapper<InvoiceNotice> queryWrapper = new QueryWrapper<>();

        // 固定查询状态为"待开票"或"已开票"
        queryWrapper.in("状态", "待开票", "已开票");

        if (creatorFilter != null) {
            queryWrapper.eq("创建人编码", creatorFilter);
        }

        // 开票通知单号（模糊匹配）
        if (StringUtils.hasText(request.getNoticeNo())) {
            queryWrapper.like("开票通知单号", request.getNoticeNo());
        }

        // 合同号（模糊匹配）
        if (StringUtils.hasText(request.getContractNo())) {
            queryWrapper.like("合同号", request.getContractNo());
        }

        // 合同名称（模糊匹配）
        if (StringUtils.hasText(request.getContractName())) {
            queryWrapper.like("合同名称", request.getContractName());
        }

        // 客户名称（模糊匹配）
        if (StringUtils.hasText(request.getCustomerName())) {
            queryWrapper.like("客户名称", request.getCustomerName());
        }

        // 状态（精确匹配）- 如果前端传入了状态，进一步过滤
        if (StringUtils.hasText(request.getStatus())) {
            if ("待开票".equals(request.getStatus()) || "已开票".equals(request.getStatus())) {
                queryWrapper.eq("状态", request.getStatus());
            }
        }

        // 申请人编码
        if (request.getApplicantId() != null) {
            queryWrapper.eq("申请人编码", request.getApplicantId());
        }

        // 申请人姓名（模糊匹配）
        if (StringUtils.hasText(request.getApplicantName())) {
            queryWrapper.like("申请人姓名", request.getApplicantName());
        }

        // 办理人编码
        if (request.getHandlerId() != null) {
            queryWrapper.eq("办理人编码", request.getHandlerId());
        }

        // 创建时间范围
        if (request.getStartTime() != null) {
            queryWrapper.ge("创建时间", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            queryWrapper.le("创建时间", request.getEndTime());
        }

        // 开票完成时间范围
        if (request.getIssuedAtStart() != null) {
            queryWrapper.ge("开票完成时间", request.getIssuedAtStart());
        }
        if (request.getIssuedAtEnd() != null) {
            queryWrapper.le("开票完成时间", request.getIssuedAtEnd());
        }

        // 处理排序
        if (StringUtils.hasText(request.getSortField()) && StringUtils.hasText(request.getSortOrder())) {
            // 字段名映射：前端字段名 -> 数据库字段名
            String dbField = mapSortFieldToDbColumn(request.getSortField());
            if (dbField != null) {
                if ("asc".equalsIgnoreCase(request.getSortOrder())) {
                    queryWrapper.orderByAsc(dbField);
                } else if ("desc".equalsIgnoreCase(request.getSortOrder())) {
                    queryWrapper.orderByDesc(dbField);
                }
            }
        } else {
            // 默认按创建时间倒序
            queryWrapper.orderByDesc("创建时间");
        }

        // 执行分页查询
        IPage<InvoiceNotice> invoiceNoticePage = invoiceNoticeMapper.selectPage(page, queryWrapper);

        // 转换为响应对象
        List<InvoiceNoticePageResponse> responseList = invoiceNoticePage.getRecords().stream()
                .map(this::convertToPageResponse)
                .collect(Collectors.toList());

        // 构建分页响应
        Page<InvoiceNoticePageResponse> responsePage = new Page<>(invoiceNoticePage.getCurrent(), invoiceNoticePage.getSize(), invoiceNoticePage.getTotal());
        responsePage.setRecords(responseList);

        log.info("查询发票关联列表成功，total={}", invoiceNoticePage.getTotal());
        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void associateInvoice(Integer noticeId, InvoiceAssociateRequest request) {
        // normalize invoiceIds to support null or empty list (null -> empty)
        List<Integer> invoiceIds = request.getInvoiceIds() == null ? Collections.emptyList() : request.getInvoiceIds();
        log.info("关联发票：noticeId={}, invoiceIds={}", noticeId, invoiceIds);

        // 1. 查询通知单，校验状态
        InvoiceNotice notice = invoiceNoticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }

        Integer currentUserId = getCurrentUserId();
        checkPageOperatePermission(currentUserId, notice, "关联发票");

        // 校验状态：允许"待开票"和"已开票"状态关联发票
        if (!"待开票".equals(notice.getStatus()) && !"已开票".equals(notice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                "只有待开票或已开票状态的通知单才能关联发票，当前状态：" + notice.getStatus());
        }

        // 2. 获取结算单ID
        Integer settlementId = notice.getMainSettlementId();
        if (settlementId == null) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "通知单未绑定结算单，无法关联发票");
        }

        // 3. 查询结算单信息，用于金额校验
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "结算单不存在");
        }

        // 4. 查询当前已关联的发票明细（通知单级别）
        QueryWrapper<InvoiceNoticeInvoice> existingWrapper = new QueryWrapper<>();
        existingWrapper.eq("开票通知单编号", noticeId);
        List<InvoiceNoticeInvoice> existingInvoices = invoiceNoticeInvoiceMapper.selectList(existingWrapper);
        List<Integer> existingInvoiceIds = existingInvoices.stream()
            .map(InvoiceNoticeInvoice::getInvoiceId)
            .filter(id -> id != null)
            .collect(Collectors.toList());

        // 5. 查询当前通知单已关联的结算单-发票关系（只处理当前通知单的关联记录）
        QueryWrapper<SettlementInvoiceRel> existingRelWrapper = new QueryWrapper<>();
        existingRelWrapper.eq("结算单编号", settlementId)
            .eq("开票通知单编号", noticeId)
            .eq("status", "ASSOCIATED");
        List<SettlementInvoiceRel> existingRelations = settlementInvoiceRelMapper.selectList(existingRelWrapper);
        List<Integer> existingRelInvoiceIds = existingRelations.stream()
            .map(SettlementInvoiceRel::getInvoiceId)
            .filter(id -> id != null)
            .collect(Collectors.toList());

        // 6. 计算需要新增和删除的发票
        List<Integer> newInvoiceIds = invoiceIds.stream()
            .filter(id -> !existingInvoiceIds.contains(id))
            .collect(Collectors.toList());

        List<Integer> deleteInvoiceIds = existingInvoiceIds.stream()
            .filter(id -> !invoiceIds.contains(id))
            .collect(Collectors.toList());

        // 7. 计算需要新增和删除的结算单-发票关系
        List<Integer> newRelInvoiceIds = invoiceIds.stream()
            .filter(id -> !existingRelInvoiceIds.contains(id))
            .collect(Collectors.toList());

        List<Integer> deleteRelInvoiceIds = existingRelInvoiceIds.stream()
            .filter(id -> !invoiceIds.contains(id))
            .collect(Collectors.toList());

        // 8. 校验发票是否存在且属于同一合同
        if (!newInvoiceIds.isEmpty()) {
            List<Invoice> invoices = invoiceMapper.selectBatchIds(newInvoiceIds);
            if (invoices.size() != newInvoiceIds.size()) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "部分发票不存在");
            }

            // 校验发票是否属于同一合同（通过发票状态和购买方/销售方信息判断）
            // 这里简化处理，实际应该根据业务规则校验
            for (Invoice invoice : invoices) {
                // 可以添加合同关联校验逻辑
            }
        }

        // 9. 金额校验：根据发票类型校验可开蓝字金额和可开红字金额
        if (!newRelInvoiceIds.isEmpty()) {
            // 查询新增发票信息
            List<Invoice> newInvoices = invoiceMapper.selectBatchIds(newRelInvoiceIds);

            // 查询当前结算单的发票汇总信息
            SettlementInvoiceSummaryResponse summary = settlementInvoiceRelMapper.selectSummaryBySettlementId(settlementId.longValue());
            if (summary == null) {
                // 如果没有汇总信息，初始化默认值
                summary = new SettlementInvoiceSummaryResponse();
                summary.setSettlementId(settlementId.longValue());
                summary.setSettlementAmount(settlement.getSettlementAmount());
                summary.setBlueSum(BigDecimal.ZERO);
                summary.setRedSum(BigDecimal.ZERO);
                summary.setNetAmount(BigDecimal.ZERO);
                summary.setCanInvoiceAmount(settlement.getSettlementAmount());
                summary.setCanRedAmount(BigDecimal.ZERO);
            }

            // 分离蓝字和红字发票进行校验
            List<Invoice> blueInvoices = newInvoices.stream()
                .filter(invoice -> "蓝字".equals(invoice.getInvoiceNature()))
                .collect(Collectors.toList());

            List<Invoice> redInvoices = newInvoices.stream()
                .filter(invoice -> "红字".equals(invoice.getInvoiceNature()))
                .collect(Collectors.toList());

            // 校验蓝字发票：可开蓝字金额必须大于0
            if (!blueInvoices.isEmpty()) {
                BigDecimal blueTotalAmount = blueInvoices.stream()
                    .map(invoice -> invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (summary.getCanInvoiceAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                        String.format("当前可开蓝字金额为0，不允许关联蓝字发票"));
                }

                if (blueTotalAmount.compareTo(summary.getCanInvoiceAmount()) > 0) {
                    throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                        String.format("蓝字发票总额(%.2f)超过可开蓝字金额(%.2f)，不允许关联",
                            blueTotalAmount, summary.getCanInvoiceAmount()));
                }

                log.info("蓝字发票校验通过：可开蓝字金额={}, 新增蓝字发票总额={}",
                    summary.getCanInvoiceAmount(), blueTotalAmount);
            }

            // 校验红字发票：可开红字金额必须大于0
            if (!redInvoices.isEmpty()) {
                BigDecimal redTotalAmount = redInvoices.stream()
                    .map(invoice -> invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (summary.getCanRedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                        String.format("当前可开红字金额为0，不允许关联红字发票"));
                }

                if (redTotalAmount.compareTo(summary.getCanRedAmount()) > 0) {
                    throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                        String.format("红字发票总额(%.2f)超过可开红字金额(%.2f)，不允许关联",
                            redTotalAmount, summary.getCanRedAmount()));
                }

                log.info("红字发票校验通过：可开红字金额={}, 新增红字发票总额={}",
                    summary.getCanRedAmount(), redTotalAmount);
            }

            // 最终校验：净额不能超过结算单金额
            BigDecimal newInvoiceTotal = newInvoices.stream()
                .map(invoice -> {
                    BigDecimal amount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
                    return "红字".equals(invoice.getInvoiceNature()) ? amount.negate() : amount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal newNetAmount = summary.getNetAmount().add(newInvoiceTotal);

            if (newNetAmount.compareTo(settlement.getSettlementAmount()) > 0) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    String.format("关联发票后净额(%.2f)超过结算单金额(%.2f)，不允许关联",
                        newNetAmount, settlement.getSettlementAmount()));
            }

            log.info("金额校验全部通过：settlementId={}, 当前净额={}, 新增净额={}, 新增后净额={}",
                settlementId, summary.getNetAmount(), newInvoiceTotal, newNetAmount);
        }

        // 10. 删除需要删除的通知单关联记录
        if (!deleteInvoiceIds.isEmpty()) {
            QueryWrapper<InvoiceNoticeInvoice> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("开票通知单编号", noticeId)
                .in("发票编号", deleteInvoiceIds);
            invoiceNoticeInvoiceMapper.delete(deleteWrapper);
            log.info("删除通知单关联发票：noticeId={}, invoiceIds={}", noticeId, deleteInvoiceIds);
        }

        // 11. 删除需要删除的结算单-发票关联记录（只删除当前通知单的关联记录）
        if (!deleteRelInvoiceIds.isEmpty()) {
            QueryWrapper<SettlementInvoiceRel> deleteRelWrapper = new QueryWrapper<>();
            deleteRelWrapper.eq("结算单编号", settlementId)
                .eq("开票通知单编号", noticeId)
                .in("发票编号", deleteRelInvoiceIds)
                .eq("status", "ASSOCIATED");
            settlementInvoiceRelMapper.delete(deleteRelWrapper);
            log.info("删除结算单发票关联：settlementId={}, noticeId={}, invoiceIds={}", settlementId, noticeId, deleteRelInvoiceIds);
        }

        // 12. 新增需要新增的通知单关联记录
        currentUserId = SecurityUtil.getCurrentUserId();
        for (Integer invoiceId : newInvoiceIds) {
            Invoice invoice = invoiceMapper.selectById(invoiceId);
            if (invoice == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(),
                    "发票不存在，发票ID：" + invoiceId);
            }

            // 检查发票号码是否已被其他通知单关联
            QueryWrapper<InvoiceNoticeInvoice> checkWrapper = new QueryWrapper<>();
            checkWrapper.eq("发票号码", invoice.getInvoiceNumber())
                .ne("开票通知单编号", noticeId);
            long count = invoiceNoticeInvoiceMapper.selectCount(checkWrapper);
            if (count > 0) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "发票号码 " + invoice.getInvoiceNumber() + " 已被其他通知单关联");
            }

            // 创建通知单关联记录
            InvoiceNoticeInvoice noticeInvoice = new InvoiceNoticeInvoice();
            noticeInvoice.setNoticeId(noticeId);
            noticeInvoice.setInvoiceId(invoiceId);
            noticeInvoice.setInvoiceNumber(invoice.getInvoiceNumber());
            noticeInvoice.setInvoiceCode(invoice.getInvoiceCode());
            noticeInvoice.setInvoiceDate(invoice.getInvoiceDate() != null ?
                invoice.getInvoiceDate().toLocalDate() : LocalDate.now());
            noticeInvoice.setInvoiceType(invoice.getInvoiceType());
            noticeInvoice.setInvoiceForm(invoice.getInvoiceForm());
            noticeInvoice.setAmount(invoice.getAmount());
            noticeInvoice.setTaxAmount(invoice.getTaxAmount());
            noticeInvoice.setTotalAmount(invoice.getTotalAmount());
            noticeInvoice.setBuyerName(invoice.getBuyerName());
            noticeInvoice.setBuyerCreditCode(invoice.getBuyerCreditCode());
            noticeInvoice.setSellerName(invoice.getSellerName());
            noticeInvoice.setSellerCreditCode(invoice.getSellerCreditCode());
            noticeInvoice.setRemark(invoice.getRemark());
            noticeInvoice.setCreateUserId(currentUserId);

            invoiceNoticeInvoiceMapper.insert(noticeInvoice);
            log.info("新增通知单关联发票：noticeId={}, invoiceId={}, invoiceNumber={}",
                noticeId, invoiceId, invoice.getInvoiceNumber());
        }

        // 13. 新增需要新增的结算单-发票关联记录
        for (Integer invoiceId : newRelInvoiceIds) {
            Invoice invoice = invoiceMapper.selectById(invoiceId);
            if (invoice == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(),
                    "发票不存在，发票ID：" + invoiceId);
            }

            // 创建结算单-发票关联记录
            SettlementInvoiceRel rel = new SettlementInvoiceRel();
            rel.setSettlementId(settlementId);
            rel.setInvoiceId(invoiceId);
            rel.setNoticeId(noticeId);  // 添加开票通知单编号
            rel.setRelAmount(invoice.getTotalAmount());
            rel.setRelType("INVOICE");
            rel.setRelTime(LocalDateTime.now());
            rel.setCreateUserId(currentUserId);
            rel.setRemark("通过开票通知单关联");
            rel.setStatus("ASSOCIATED");
            rel.setVersion(0);

            settlementInvoiceRelMapper.insert(rel);
            log.info("新增结算单发票关联：settlementId={}, invoiceId={}, amount={}",
                settlementId, invoiceId, invoice.getTotalAmount());
        }

        // 7. 更新通知单（MyBatis-Plus会自动递增version）
        notice.setUpdateUserId(currentUserId);
        notice.setUpdateTime(LocalDateTime.now());
        int updateCount = invoiceNoticeMapper.updateById(notice);
        if (updateCount == 0) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(), 
                "数据已被修改，请刷新后重试");
        }

        // 8. 发送消息通知
        try {
            // 记录数据变更日志（关联发票变更）
            try {
                InvoiceNotice oldNotice = new InvoiceNotice();
                oldNotice.setInvoiceCount(existingInvoices.size());
                InvoiceNotice newNotice = new InvoiceNotice();
                newNotice.setInvoiceCount(invoiceIds.size());
                if (logRecordService != null) {
                    logRecordService.recordDataChangeLog("开票通知单管理", "INVOICE_NOTICE", String.valueOf(noticeId),
                            "关联发票", String.format("关联发票：通知单号=%s，新增%d张，移除%d张",
                                    notice.getNoticeNo(), newInvoiceIds.size(), deleteInvoiceIds.size()),
                            oldNotice, newNotice, currentUserId, null, true, null);
                }
            } catch (Exception logException) {
                log.warn("记录开票通知单关联发票数据变更日志失败", logException);
            }

        } catch (Exception e) {
            log.warn("关联发票处理失败，noticeId={}", noticeId, e);
        }

        // 5. 更新结算单的已收金额（如果有资金流水关联）
        try {
            updateSettlementReceivedAmount(settlementId);
        } catch (Exception e) {
            log.warn("更新结算单已收金额失败，noticeId=" + noticeId + ", settlementId=" + settlementId, e);
            // 不影响主流程，只记录警告
        }

        log.info("关联发票完成：noticeId={}, 新增={}, 删除={}", noticeId, newInvoiceIds.size(), deleteInvoiceIds.size());
    }

    @Override
    public java.util.List<String> validateInvoiceAssociations(Integer noticeId, java.util.List<Integer> invoiceIds) {
        java.util.List<String> conflicts = new ArrayList<>();

        // 校验通知单存在并获取结算单
        InvoiceNotice notice = invoiceNoticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }
        Integer settlementId = notice.getMainSettlementId();

        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return conflicts;
        }

        // 1) 检查是否已被其他通知单关联（INVOICE_NOTICE_INVOICE）
        QueryWrapper<InvoiceNoticeInvoice> q1 = new QueryWrapper<>();
        q1.in("发票编号", invoiceIds)
          .ne("开票通知单编号", noticeId);
        List<InvoiceNoticeInvoice> existingNoticeInvoices = invoiceNoticeInvoiceMapper.selectList(q1);
        if (existingNoticeInvoices != null && !existingNoticeInvoices.isEmpty()) {
            java.util.Map<Integer, Integer> invoiceToNotice = new java.util.HashMap<>();
            for (InvoiceNoticeInvoice ini : existingNoticeInvoices) {
                invoiceToNotice.put(ini.getInvoiceId(), ini.getNoticeId());
            }
            for (Integer invId : invoiceIds) {
                if (invoiceToNotice.containsKey(invId)) {
                    conflicts.add(String.format("发票ID %d 已被通知单 %d 关联", invId, invoiceToNotice.get(invId)));
                }
            }
        }

        // 2) 检查是否已被其他结算单关联（SETTLEMENT_INVOICE_REL）
        if (settlementId != null) {
            QueryWrapper<SettlementInvoiceRel> q2 = new QueryWrapper<>();
            q2.in("发票编号", invoiceIds)
              .ne("结算单编号", settlementId)
              .in("status", java.util.Arrays.asList("BOUND","ASSOCIATED"));
            List<SettlementInvoiceRel> existingRels = settlementInvoiceRelMapper.selectList(q2);
            if (existingRels != null && !existingRels.isEmpty()) {
                java.util.Map<Integer, Integer> invoiceToSettlement = new java.util.HashMap<>();
                for (SettlementInvoiceRel rel : existingRels) {
                    invoiceToSettlement.put(rel.getInvoiceId(), rel.getSettlementId());
                }
                for (Integer invId : invoiceIds) {
                    if (invoiceToSettlement.containsKey(invId)) {
                        conflicts.add(String.format("发票ID %d 已被结算单 %d 关联", invId, invoiceToSettlement.get(invId)));
                    }
                }
            }
        }

        return conflicts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeInvoiceAssociate(Integer noticeId, InvoiceAssociateCompleteRequest request) {
        log.info("完成关联：noticeId={}", noticeId);

        // 1. 查询通知单，校验状态
        InvoiceNotice notice = invoiceNoticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "开票通知单不存在");
        }

        Integer currentUserId = getCurrentUserId();
        checkPageOperatePermission(currentUserId, notice, "完成关联");

        // 校验状态：只有"待开票"状态才能完成关联
        if (!"待开票".equals(notice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), 
                "只有待开票状态的通知单才能完成关联，当前状态：" + notice.getStatus());
        }

        // version字段由MyBatis-Plus的@Version注解自动处理，不需要手动设置

        // 2. 查询已关联的发票明细
        QueryWrapper<InvoiceNoticeInvoice> wrapper = new QueryWrapper<>();
        wrapper.eq("开票通知单编号", noticeId);
        List<InvoiceNoticeInvoice> invoices = invoiceNoticeInvoiceMapper.selectList(wrapper);

        // 3. 校验至少关联一张发票
        if (invoices.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), 
                "至少需要关联一张发票才能完成关联");
        }

        // 4. 计算已开票张数和金额汇总
        int invoiceCount = invoices.size();
        java.math.BigDecimal totalAmount = invoices.stream()
            .map(InvoiceNoticeInvoice::getTotalAmount)
            .filter(amount -> amount != null)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 5. 更新通知单状态和汇总信息
        currentUserId = SecurityUtil.getCurrentUserId();
        notice.setStatus("已开票");
        notice.setInvoiceCount(invoiceCount);
        notice.setTotalAmount(totalAmount);
        notice.setIssuedAt(LocalDateTime.now());
        notice.setHandlerId(currentUserId);
        
        // 获取办理人姓名
        Employee handler = employeeMapper.selectById(currentUserId);
        if (handler != null) {
            notice.setHandlerName(handler.getEmployeeName());
        }

        notice.setUpdateUserId(currentUserId);
        notice.setUpdateTime(LocalDateTime.now());
        // MyBatis-Plus会自动递增version并校验乐观锁
        int updateCount = invoiceNoticeMapper.updateById(notice);
        if (updateCount == 0) {
            throw new BusinessException(ResultCodeEnum.DATA_CONFLICT.getCode(), 
                "数据已被修改，请刷新后重试");
        }

        // 发送消息通知
        try {
            // 记录数据变更日志（状态变更：待开票 -> 已开票）
            try {
                InvoiceNotice oldNotice = new InvoiceNotice();
                oldNotice.setStatus("待开票");
                InvoiceNotice newNotice = new InvoiceNotice();
                newNotice.setStatus("已开票");
                newNotice.setInvoiceCount(invoiceCount);
                newNotice.setTotalAmount(totalAmount);
                if (logRecordService != null) {
                    logRecordService.recordDataChangeLog("开票通知单管理", "INVOICE_NOTICE", String.valueOf(noticeId),
                            "完成关联", String.format("完成关联：通知单号=%s，已开票%d张，价税合计%s元",
                                    notice.getNoticeNo(), invoiceCount, totalAmount.toString()),
                            oldNotice, newNotice, currentUserId, null, true, null);
                }
            } catch (Exception logException) {
                log.warn("记录开票通知单完成关联数据变更日志失败", logException);
            }

        } catch (Exception e) {
            log.warn("完成关联处理失败，noticeId={}", noticeId, e);
        }

        log.info("完成关联成功：noticeId={}, invoiceCount={}, totalAmount={}",
            noticeId, invoiceCount, totalAmount);
    }

    /**
     * 更新结算单的已收金额
     * 逻辑：
     * 1. 首先检查 SETTLEMENT_FUND_TRANSACTION_REL 表是否有该结算单的资金流水关联
     * 2. 如果没有关联，不做任何操作
     * 3. 如果有关联，查询 SETTLEMENT_INVOICE_REL 表中该结算单所有发票的关联金额
     *    蓝字相加，红字相减，得到净额
     * 4. 更新结算单的已收金额字段
     *
     * @param settlementId 结算单ID（可能为null）
     */
    private void updateSettlementReceivedAmount(Integer settlementId) {
        if (settlementId == null) {
            log.debug("结算单ID为空，跳过更新已收金额");
            return;
        }

        // 1. 检查 SETTLEMENT_FUND_TRANSACTION_REL 表是否有该结算单的资金流水关联
        QueryWrapper<SettlementFundTransactionRel> fundRelWrapper = new QueryWrapper<>();
        fundRelWrapper.eq("结算单编号", settlementId.longValue());
        Long fundRelCount = settlementFundTransactionRelMapper.selectCount(fundRelWrapper);

        if (fundRelCount == null || fundRelCount == 0) {
            log.info("结算单没有资金流水关联，跳过更新已收金额，settlementId={}", settlementId);
            return;
        }

        log.info("结算单有资金流水关联，开始更新已收金额，settlementId={}, 关联记录数={}",
            settlementId, fundRelCount);

        // 2. 查询 SETTLEMENT_INVOICE_REL 表中该结算单所有发票的关联金额（蓝字 - 红字）
        BigDecimal netAmount = settlementInvoiceRelMapper.selectNetAmountBySettlementId(settlementId.longValue());
        if (netAmount == null) {
            netAmount = BigDecimal.ZERO;
        }

        log.info("结算单发票关联净额计算完成，settlementId={}, netAmount={}", settlementId, netAmount);

        // 3. 更新结算单的已收金额字段
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            log.warn("结算单不存在，无法更新已收金额，settlementId={}", settlementId);
            return;
        }

        settlement.setReceivedAmount(netAmount);
        settlement.setUpdateTime(LocalDateTime.now());
        settlement.setUpdateUserId(SecurityUtil.getCurrentUserId());

        int updateResult = settlementMapper.updateById(settlement);
        if (updateResult > 0) {
            log.info("结算单已收金额更新成功，settlementId={}, receivedAmount={}", settlementId, netAmount);
        } else {
            log.warn("结算单已收金额更新失败，settlementId={}", settlementId);
        }
    }

    /**
     * 发送开票通知单创建通知（待审批状态）
     * 使用基于权限的通知方法，发送给OA审批人员
     */
    private void sendInvoiceNoticeCreatedNotification(InvoiceNotice invoiceNotice, Integer senderId) {
        try {
            log.info("开始发送开票通知单创建通知：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), invoiceNotice.getNoticeNo());
            
            String noticeNo = invoiceNotice.getNoticeNo() != null ? invoiceNotice.getNoticeNo() : "未知";
            
            // 使用基于权限的通知方法，发送给OA审批人员
            messageNotificationService.sendApprovalSubmitNotification(
                    "INVOICE_NOTICE_SUBMIT",
                    invoiceNotice.getNoticeId(),
                    String.format("开票通知单【%s】", noticeNo),
                    senderId
            );
            
            log.info("开票通知单创建通知已发送：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), noticeNo);
        } catch (Exception e) {
            log.error("发送开票通知单创建通知异常：noticeId={}", invoiceNotice.getNoticeId(), e);
        }
    }

    /**
     * 发送开票通知单提交审批通知
     * 使用基于权限的通知方法，发送给OA审批人员
     */
    private void sendInvoiceNoticeSubmittedNotification(InvoiceNotice invoiceNotice, Integer senderId) {
        try {
            log.info("开始发送开票通知单提交审批通知：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), invoiceNotice.getNoticeNo());
            
            String noticeNo = invoiceNotice.getNoticeNo() != null ? invoiceNotice.getNoticeNo() : "未知";
            
            // 使用基于权限的通知方法，发送给OA审批人员
            messageNotificationService.sendApprovalSubmitNotification(
                    "INVOICE_NOTICE_SUBMIT",
                    invoiceNotice.getNoticeId(),
                    String.format("开票通知单【%s】", noticeNo),
                    senderId
            );
            
            log.info("开票通知单提交审批通知已发送：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), noticeNo);
        } catch (Exception e) {
            log.error("发送开票通知单提交审批通知异常：noticeId={}", invoiceNotice.getNoticeId(), e);
        }
    }

    /**
     * 发送开票通知单审批通过通知
     * 使用基于权限的通知方法，发送给有开票通知页面权限的人员
     */
    private void sendInvoiceNoticeApprovedNotification(InvoiceNotice invoiceNotice, Integer senderId) {
        try {
            log.info("开始发送开票通知单审批通过通知：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), invoiceNotice.getNoticeNo());
            
            String noticeNo = invoiceNotice.getNoticeNo() != null ? invoiceNotice.getNoticeNo() : "未知";
            
            // 使用基于权限的通知方法
            messageNotificationService.sendAuditResultNotification(
                    "INVOICE_NOTICE_AUDIT_RESULT",
                    invoiceNotice.getNoticeId(),
                    String.format("开票通知单【%s】", noticeNo),
                    "审核通过",
                    senderId
            );
            
            log.info("开票通知单审批通过通知已发送：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), noticeNo);
        } catch (Exception e) {
            log.error("发送开票通知单审批通过通知异常：noticeId={}", invoiceNotice.getNoticeId(), e);
        }
    }

    /**
     * 发送开票通知单驳回通知
     * 使用基于权限的通知方法，发送给有开票通知页面权限的人员
     */
    private void sendInvoiceNoticeRejectedNotification(InvoiceNotice invoiceNotice, Integer senderId, String reason) {
        try {
            log.info("开始发送开票通知单驳回通知：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), invoiceNotice.getNoticeNo());
            
            String noticeNo = invoiceNotice.getNoticeNo() != null ? invoiceNotice.getNoticeNo() : "未知";
            
            // 使用基于权限的通知方法
            messageNotificationService.sendAuditResultNotification(
                    "INVOICE_NOTICE_AUDIT_RESULT",
                    invoiceNotice.getNoticeId(),
                    String.format("开票通知单【%s】", noticeNo),
                    "审核驳回",
                    senderId
            );
            
            log.info("开票通知单驳回通知已发送：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), noticeNo);
        } catch (Exception e) {
            log.error("发送开票通知单驳回通知异常：noticeId={}", invoiceNotice.getNoticeId(), e);
        }
    }

    /**
     * 发送开票通知单完成关联通知
     * 使用基于权限的通知方法，发送给有开票通知页面权限的人员
     */
    private void sendInvoiceNoticeCompletedNotification(InvoiceNotice invoiceNotice, Integer senderId) {
        try {
            log.info("开始发送开票通知单完成关联通知：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), invoiceNotice.getNoticeNo());
            
            String noticeNo = invoiceNotice.getNoticeNo() != null ? invoiceNotice.getNoticeNo() : "未知";
            
            // 使用基于权限的通知方法
            messageNotificationService.sendBusinessOperationNotification(
                    "INVOICE_NOTICE_UPDATE",
                    invoiceNotice.getNoticeId(),
                    String.format("开票通知单【%s】", noticeNo),
                    "完成关联",
                    senderId
            );
            
            log.info("开票通知单完成关联通知已发送：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), noticeNo);
        } catch (Exception e) {
            log.error("发送开票通知单完成关联通知异常：noticeId={}", invoiceNotice.getNoticeId(), e);
        }
    }

    /**
     * 发送开票通知单关联发票通知
     * 使用基于权限的通知方法，发送给有开票通知页面权限的人员
     */
    private void sendInvoiceNoticeAssociatedNotification(InvoiceNotice invoiceNotice, Integer senderId, 
                                                         int addedCount, int deletedCount) {
        try {
            log.info("开始发送开票通知单关联发票通知：noticeId={}, noticeNo={}, 新增={}, 删除={}", 
                    invoiceNotice.getNoticeId(), invoiceNotice.getNoticeNo(), addedCount, deletedCount);
            
            String noticeNo = invoiceNotice.getNoticeNo() != null ? invoiceNotice.getNoticeNo() : "未知";
            
            // 如果没有新增也没有删除，不发送通知
            if (addedCount <= 0 && deletedCount <= 0) {
                log.info("开票通知单关联发票无变化，跳过通知：noticeId={}", invoiceNotice.getNoticeId());
                return;
            }
            
            String action = (addedCount > 0 && deletedCount > 0) ? "更新关联" : (addedCount > 0 ? "关联发票" : "移除关联");
            
            // 使用基于权限的通知方法
            messageNotificationService.sendBusinessOperationNotification(
                    "INVOICE_NOTICE_UPDATE",
                    invoiceNotice.getNoticeId(),
                    String.format("开票通知单【%s】", noticeNo),
                    action,
                    senderId
            );
            
            log.info("开票通知单关联发票通知已发送：noticeId={}, noticeNo={}", 
                    invoiceNotice.getNoticeId(), noticeNo);
        } catch (Exception e) {
            log.error("发送开票通知单关联发票通知异常：noticeId={}", invoiceNotice.getNoticeId(), e);
        }
    }

    @Override
    public List<SettlementQueryResultDTO> getSettlementsByContract(Integer contractId, String invoiceType, String viewScope) {
        log.info("根据合同查询结算单，contractId={}, invoiceType={}, viewScope={}", contractId, invoiceType, viewScope);

        // 验证合同存在
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        // 验证开票类型
        if (!"开票".equals(invoiceType) && !"作废".equals(invoiceType)) {
            throw new BusinessException("开票类型必须是'开票'或'作废'");
        }

        // 解析数据范围
        String resolvedViewScope = ViewScopeHelper.resolveViewScope("开票通知", viewScope);
        Integer creatorId = null;
        if (ViewScopeHelper.isSelfScope(resolvedViewScope)) {
            creatorId = getCurrentUserId();
        }

        // 查询结算单列表
        List<SettlementQueryResultDTO> settlements = settlementMapper.selectSettlementsByContractAndInvoiceType(contractId, invoiceType, resolvedViewScope, creatorId);

        log.info("查询到{}个可用于{}的结算单", settlements.size(), invoiceType);
        return settlements;
    }
}

