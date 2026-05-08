package com.erp.service.production.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.transport.dto.TransportApplyDetailRequest;
import com.erp.controller.transport.dto.TransportApplyDetailResponse;
import com.erp.controller.transport.dto.TransportApplyItemRequest;
import com.erp.controller.transport.dto.TransportApplyItemResponse;
import com.erp.controller.transport.dto.TransportApplyListResponse;
import com.erp.controller.transport.dto.TransportApplyPageRequest;
import com.erp.controller.transport.dto.TransportApplyPageResponse;
import com.erp.controller.transport.dto.TransportAttachmentRequest;
import com.erp.controller.transport.dto.TransportStat;
import com.erp.controller.transport.dto.WasteCategoryLimitResponse;
import com.erp.entity.contract.Contract;
import com.erp.entity.customer.Customer;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.entity.production.PickupNotice;
import com.erp.entity.production.PickupNoticeItem;
import com.erp.entity.system.Employee;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.HazardousWasteCategory;
import com.erp.entity.system.HazardousWasteItem;
import com.erp.entity.system.Permission;
import com.erp.mapper.contract.ContractMapper;
import com.erp.mapper.contract.ContractWasteItemMapper;
import com.erp.mapper.customer.CustomerMapper;
import com.erp.mapper.production.PickupNoticeItemMapper;
import com.erp.mapper.production.PickupNoticeMapper;
import com.erp.entity.common.File;
import com.erp.mapper.common.FileMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.HazardousWasteCategoryMapper;
import com.erp.mapper.system.HazardousWasteItemMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.production.PickupNoticeService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.erp.controller.transport.dto.TransportAttachmentResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收运通知单服务实现
 */
@Slf4j
@Service
public class PickupNoticeServiceImpl implements PickupNoticeService {

    @Autowired
    private PickupNoticeMapper pickupNoticeMapper;

    @Autowired
    private PickupNoticeItemMapper pickupNoticeItemMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ContractWasteItemMapper contractWasteItemMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @Autowired
    private HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private HazardousWasteCategoryMapper hazardousWasteCategoryMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private com.erp.mapper.transport.DispatchOrderMapper dispatchOrderMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.erp.service.contract.ContractApprovalFlowService contractApprovalFlowService;

    @Autowired
    private com.erp.service.oa.OaApprovalRecordService oaApprovalRecordService;

    @Autowired
    private com.erp.mapper.oa.OaApprovalRecordMapper oaApprovalRecordMapper;

    @org.springframework.beans.factory.annotation.Value("${file.storage.local.path:D:/erp}")
    private String localStoragePath;

    /**
     * 业务类型常量
     */
    private static final String PICKUP_NOTICE_BUSINESS_TYPE = "PICKUP_NOTICE";

    /**
     * 生成收运通知单号
     * 规则：SYTTD-YYYYMMDD-当日递增4位序号（共19个字符）
     * 注意：数据库字段长度需要为VARCHAR(25)或更大
     */
    private String generateNoticeCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SYTTD-" + datePart + "-";

        // 查询当日当前最大编号
        String maxNoticeCode = pickupNoticeMapper.selectMaxNoticeCodeByPrefix(prefix);

        int nextSeq = 1;
        if (StrUtil.isNotBlank(maxNoticeCode)) {
            // 截取末尾4位序号并递增
            try {
                String seqPart = maxNoticeCode.substring(maxNoticeCode.length() - 4);
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (Exception e) {
                log.warn("解析最大收运通知单号序号失败：{}", maxNoticeCode, e);
            }
        }

        String noticeCode = prefix + String.format("%04d", nextSeq);

        // 若并发导致重复，则继续自增直到唯一或达到重试上限
        int retryCount = 0;
        while (pickupNoticeMapper.countByNoticeCode(noticeCode) > 0 && retryCount < 20) {
            nextSeq++;
            noticeCode = prefix + String.format("%04d", nextSeq);
            retryCount++;
        }

        if (retryCount >= 20) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成收运通知单号失败，请重试");
        }

        // 验证生成的编号长度（应该是19个字符）
        if (noticeCode.length() != 19) {
            log.warn("生成的收运通知单号长度异常：{}，长度：{}", noticeCode, noticeCode.length());
        }
        log.debug("生成收运通知单号：{}", noticeCode);
        return noticeCode;
    }

    /**
     * 生成OA审核编号
     * 规则：OA-YYYYMMDD-XXXX（当日4位递增序号）
     */
    private String generateApprovalNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "OA-" + datePart + "-";

        // 查询当日最大审核编号
        QueryWrapper<OaApprovalRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("审核编号", prefix);
        queryWrapper.orderByDesc("审核记录编号");
        queryWrapper.last("LIMIT 1");
        OaApprovalRecord maxRecord = oaApprovalRecordMapper.selectOne(queryWrapper);

        int nextSeq = 1;
        if (maxRecord != null && StrUtil.isNotBlank(maxRecord.getApprovalNo())) {
            try {
                String seqPart = maxRecord.getApprovalNo().substring(prefix.length());
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (Exception e) {
                log.warn("解析最大审核编号序号失败：{}", maxRecord.getApprovalNo(), e);
            }
        }

        return prefix + String.format("%04d", nextSeq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransportApplyDetailResponse createPickupNotice(TransportApplyDetailRequest request) {
        Integer currentUserId = getCurrentUserId();

        // 归一化合同号，空白视为null，避免外键写入空串
        String contractCode = StrUtil.isBlank(request.getContractCode()) ? null : request.getContractCode().trim();

        // 验证合同号（如果提供）
        Contract contract = null;
        if (StrUtil.isNotBlank(contractCode)) {
            contract = contractMapper.selectOne(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getContractNo, contractCode)
            );
            if (contract == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
            }
        }

        // 验证客户（如果提供）
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerMapper.selectById(request.getCustomerId());
            if (customer == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
            }
        }

        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("现场联系电话", request.getOnsitePhone());
        validatePhoneFormat("业务联系电话", request.getBusinessPhone());
        validatePhoneFormat("应急联系电话", request.getEmergencyPhone());

        // 生成收运通知单号
        String noticeCode = generateNoticeCode();

        // 构建收运通知单实体
        PickupNotice notice = new PickupNotice();
        notice.setNoticeCode(noticeCode);
        notice.setContractCode(contractCode);
        notice.setContractPending(request.getContractPending() != null ? request.getContractPending() : true);
        notice.setCustomerId(request.getCustomerId());
        notice.setCompanyName(request.getCompanyName());
        notice.setCreditCode(request.getCreditCode());
        notice.setTransportAddress(request.getTransportAddress());
        notice.setOnsiteContact(request.getOnsiteContact());
        notice.setOnsitePhone(request.getOnsitePhone());
        notice.setEmergencyPhone(request.getEmergencyPhone());
        
        // 转换日期字段
        if (StrUtil.isNotBlank(request.getPlanTransferDate())) {
            notice.setPlanTransferDate(convertToLocalDateTime(request.getPlanTransferDate()));
        }
        if (StrUtil.isNotBlank(request.getSubmitDate())) {
            notice.setSubmittedAt(convertToLocalDateTime(request.getSubmitDate()));
        }
        
        notice.setRemark(request.getRemark());
        notice.setStatus("待审核");
        notice.setLocked(false);
        notice.setCreatorId(currentUserId);
        notice.setCreateTime(LocalDateTime.now());

        // 保存收运通知单
        pickupNoticeMapper.insert(notice);

        // 保存危废明细（如果有）
        if (!CollectionUtils.isEmpty(request.getItems())) {
            savePickupNoticeItems(noticeCode, request.getItems());
        }

        // 保存附件（无论是否为空，都要处理）
        saveAttachments(noticeCode, request.getAttachments() != null ? request.getAttachments() : new ArrayList<>());

        log.info("创建收运通知单成功：noticeCode={}, operator={}", noticeCode, currentUserId);

        // 记录操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "新增",
                    "新增收运通知单：收运通知单号=" + noticeCode,
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录收运通知单新增操作日志失败", e);
        }

        // 记录数据变更日志
        try {
            TransportApplyDetailResponse newDetail = buildDetailResponse(notice, customer, contract);
            logRecordService.recordDataChangeLog("收运通知单管理", "PICKUP_NOTICE",
                    String.valueOf(notice.getNoticeId()),
                    "新增",
                    "新增收运通知单：收运通知单号=" + noticeCode,
                    null, newDetail, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录收运通知单新增数据变更日志失败", e);
        }

        // 如果关联了合同，创建收运通知审批流记录（同步更新合同履行模块）
        if (contract != null && contract.getContractId() != null) {
            try {
                contractApprovalFlowService.createPickupNoticeFlow(contract.getContractId(), currentUserId);
            } catch (Exception e) {
                log.error("创建收运通知审批流记录失败：contractId={}, contractNo={}", 
                        contract.getContractId(), contract.getContractNo(), e);
                // 审批流创建失败不影响主流程，只记录日志
            }
        }

        // 发送创建通知给创建人（使用基于权限的通知方法）
        try {
            String customerName = customer != null && customer.getEnterpriseName() != null 
                    ? customer.getEnterpriseName() : "未知客户";
            // 使用基于权限的通知方法，发送给有收运通知页面权限的人员
            messageNotificationService.sendBusinessNotificationByPermission(
                    "PICKUP_NOTICE_CREATE",
                    notice.getNoticeId(),
                    String.format("收运通知单【%s】", noticeCode),
                    String.format("您已成功创建收运通知单【%s】，客户：%s，请及时提交审核。", 
                            noticeCode, customerName),
                    currentUserId,
                    "生产"
            );
            log.info("收运通知单创建通知已发送：noticeCode={}", noticeCode);
        } catch (Exception e) {
            log.error("发送收运通知单创建通知失败：noticeCode={}", noticeCode, e);
        }

        return buildDetailResponse(notice, customer, contract);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransportApplyDetailResponse updatePickupNotice(TransportApplyDetailRequest request) {
        Integer currentUserId = getCurrentUserId();
        
        if (StrUtil.isBlank(request.getNoticeCode()) && request.getNoticeId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "收运通知单号或编号不能为空");
        }

        // 查询收运通知单
        PickupNotice notice = null;
        if (request.getNoticeId() != null) {
            notice = pickupNoticeMapper.selectDetailById(request.getNoticeId());
        } else {
            notice = pickupNoticeMapper.selectDetailByNoticeCode(request.getNoticeCode());
        }

        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        // 保存旧数据用于日志记录（必须在修改notice对象之前获取）
        TransportApplyDetailResponse oldDetail = null;
        try {
            Customer oldCustomer = null;
            if (notice.getCustomerId() != null) {
                oldCustomer = customerMapper.selectById(notice.getCustomerId());
            }
            Contract oldContract = null;
            if (StrUtil.isNotBlank(notice.getContractCode())) {
                oldContract = contractMapper.selectOne(
                        new LambdaQueryWrapper<Contract>()
                                .eq(Contract::getContractNo, notice.getContractCode())
                );
            }
            oldDetail = buildDetailResponse(notice, oldCustomer, oldContract);
        } catch (Exception e) {
            log.warn("获取收运通知单旧数据失败，将跳过数据变更日志记录", e);
        }

        // 检查是否锁定
        if (Boolean.TRUE.equals(notice.getLocked())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "收运通知单已锁定，无法修改");
        }

        // 检查状态（只有待审核或已驳回状态才能修改）
        if (!"待审核".equals(notice.getStatus()) && !"已驳回".equals(notice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不允许修改");
        }

        // 校验手机号格式：必须为空或11位手机号
        validatePhoneFormat("现场联系电话", request.getOnsitePhone());
        validatePhoneFormat("业务联系电话", request.getBusinessPhone());
        validatePhoneFormat("应急联系电话", request.getEmergencyPhone());

        // 归一化合同号，空白视为null
        String contractCode = StrUtil.isBlank(request.getContractCode()) ? null : request.getContractCode().trim();

        // 验证合同号（如果提供）
        Contract contract = null;
        if (StrUtil.isNotBlank(contractCode)) {
            contract = contractMapper.selectOne(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getContractNo, contractCode)
            );
            if (contract == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
            }
        }

        // 验证客户（如果提供）
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerMapper.selectById(request.getCustomerId());
            if (customer == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "客户不存在");
            }
        }

        // 更新收运通知单字段
        if (contractCode == null) {
            notice.setContractCode(null);
        } else {
            notice.setContractCode(contractCode);
        }
        if (request.getContractPending() != null) {
            notice.setContractPending(request.getContractPending());
        }
        if (request.getCustomerId() != null) {
            notice.setCustomerId(request.getCustomerId());
        }
        if (StrUtil.isNotBlank(request.getCompanyName())) {
            notice.setCompanyName(request.getCompanyName());
        }
        if (StrUtil.isNotBlank(request.getCreditCode())) {
            notice.setCreditCode(request.getCreditCode());
        }
        if (StrUtil.isNotBlank(request.getTransportAddress())) {
            notice.setTransportAddress(request.getTransportAddress());
        }
        if (StrUtil.isNotBlank(request.getOnsiteContact())) {
            notice.setOnsiteContact(request.getOnsiteContact());
        }
        if (StrUtil.isNotBlank(request.getOnsitePhone())) {
            notice.setOnsitePhone(request.getOnsitePhone());
        }
        if (StrUtil.isNotBlank(request.getEmergencyPhone())) {
            notice.setEmergencyPhone(request.getEmergencyPhone());
        }
        if (StrUtil.isNotBlank(request.getPlanTransferDate())) {
            notice.setPlanTransferDate(convertToLocalDateTime(request.getPlanTransferDate()));
        }
        if (StrUtil.isNotBlank(request.getRemark())) {
            notice.setRemark(request.getRemark());
        }
        notice.setUpdateTime(LocalDateTime.now());

        // 更新收运通知单
        int rows = pickupNoticeMapper.updateById(notice);
        if (rows == 0) {
            log.warn("更新收运通知单失败（乐观锁冲突），noticeCode={}", notice.getNoticeCode());
        }

        // 更新危废明细（先删除旧的，再插入新的）
        if (request.getItems() != null) {
            pickupNoticeItemMapper.deleteByNoticeCode(notice.getNoticeCode());
            savePickupNoticeItems(notice.getNoticeCode(), request.getItems());
        }

        // 更新附件（无论是否为空，都要处理，以便清空已删除的附件）
        saveAttachments(notice.getNoticeCode(), request.getAttachments() != null ? request.getAttachments() : new ArrayList<>());
        // 单独刷新该通知单下所有明细的更新时间为当前时间，以反映用户的修改操作
        try {
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PickupNoticeItem> uw =
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            uw.set("更新时间", LocalDateTime.now()).eq("收运通知单号", notice.getNoticeCode());
            pickupNoticeItemMapper.update(null, uw);
        } catch (Exception e) {
            log.warn("批量刷新收运通知单明细更新时间失败：noticeCode={}", notice.getNoticeCode(), e);
        }

        log.info("更新收运通知单成功：noticeCode={}, operator={}", notice.getNoticeCode(), currentUserId);

        // 发送修改通知（使用基于权限的通知方法）
        try {
            String customerName = customer != null && customer.getEnterpriseName() != null 
                    ? customer.getEnterpriseName() : "未知客户";
            // 使用基于权限的通知方法
            messageNotificationService.sendBusinessNotificationByPermission(
                    "PICKUP_NOTICE_UPDATE",
                    notice.getNoticeId(),
                    String.format("收运通知单【%s】", notice.getNoticeCode()),
                    String.format("收运通知单【%s】已被修改，客户：%s，请查看详情。", 
                            notice.getNoticeCode(), customerName),
                    currentUserId,
                    "生产"
            );
            log.info("收运通知单修改通知已发送：noticeCode={}", notice.getNoticeCode());
        } catch (Exception e) {
            log.error("发送收运通知单修改通知失败：noticeCode={}", notice.getNoticeCode(), e);
        }

        // 构建新数据用于日志记录
        TransportApplyDetailResponse newDetail = buildDetailResponse(notice, customer, contract);

        // 记录操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "更新",
                    "更新收运通知单：收运通知单号=" + notice.getNoticeCode(),
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录收运通知单更新操作日志失败", e);
        }

        // 记录数据变更日志
        try {
            if (oldDetail != null) {
                log.info("准备记录数据变更日志 - noticeCode={}, oldDetail不为空", notice.getNoticeCode());
                logRecordService.recordDataChangeLog("收运通知单管理", "PICKUP_NOTICE",
                        String.valueOf(notice.getNoticeId()),
                        "更新",
                        "更新收运通知单：收运通知单号=" + notice.getNoticeCode(),
                        oldDetail, newDetail, currentUserId, null, true, null);
                log.info("数据变更日志记录完成 - noticeCode={}", notice.getNoticeCode());
            } else {
                log.warn("oldDetail为空，无法记录数据变更日志 - noticeCode={}", notice.getNoticeCode());
                // 即使oldDetail为空，也记录操作日志（至少记录有更新操作）
                logRecordService.recordOperationLog("收运通知单管理", "更新",
                        "更新收运通知单：收运通知单号=" + notice.getNoticeCode() + "（无法获取旧数据，仅记录操作）",
                        currentUserId, null, true, null);
            }
        } catch (Exception e) {
            log.error("记录收运通知单更新数据变更日志失败 - noticeCode={}", notice.getNoticeCode(), e);
            // 即使数据变更日志失败，也确保操作日志被记录
            try {
                logRecordService.recordOperationLog("收运通知单管理", "更新",
                        "更新收运通知单：收运通知单号=" + notice.getNoticeCode() + "（数据变更日志记录失败）",
                        currentUserId, null, true, null);
            } catch (Exception ex) {
                log.error("记录操作日志也失败", ex);
            }
        }

        return newDetail;
    }

    @Override
    public TransportApplyDetailResponse getPickupNoticeDetail(String noticeCode) {
        PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        Customer customer = null;
        if (notice.getCustomerId() != null) {
            customer = customerMapper.selectById(notice.getCustomerId());
        }

        Contract contract = null;
        if (StrUtil.isNotBlank(notice.getContractCode())) {
            contract = contractMapper.selectOne(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getContractNo, notice.getContractCode())
            );
        }

        return buildDetailResponse(notice, customer, contract);
    }

    @Override
    public TransportApplyDetailResponse getPickupNoticeDetailById(Integer noticeId) {
        PickupNotice notice = pickupNoticeMapper.selectDetailById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        Customer customer = null;
        if (notice.getCustomerId() != null) {
            customer = customerMapper.selectById(notice.getCustomerId());
        }

        Contract contract = null;
        if (StrUtil.isNotBlank(notice.getContractCode())) {
            contract = contractMapper.selectOne(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getContractNo, notice.getContractCode())
            );
        }

        return buildDetailResponse(notice, customer, contract);
    }

    @Override
    public TransportApplyListResponse getPickupNoticePage(TransportApplyPageRequest request) {
        Integer currentUserId = getCurrentUserId();

        // 收运通知页面权限编码
        String pageCode = "业务管理:收运通知:页面";

        // 使用 ViewScopeHelper 解析视图范围
        String viewScope = ViewScopeHelper.resolveViewScope(pageCode, request.getViewScope());

        // SELF 模式时添加创建人过滤条件，ALL 模式时不限制
        Integer creatorFilter = ViewScopeHelper.isSelfScope(viewScope) ? currentUserId : null;

        Page<PickupNotice> page = new Page<>(request.getCurrent(), request.getSize());

        // 转换日期字符串
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (StrUtil.isNotBlank(request.getStartTime())) {
            startTime = convertToLocalDateTime(request.getStartTime());
        }
        if (StrUtil.isNotBlank(request.getEndTime())) {
            endTime = convertToLocalDateTime(request.getEndTime());
        }

        IPage<PickupNotice> noticePage = pickupNoticeMapper.selectPickupNoticePage(
                page,
                request.getNoticeCode(),
                request.getCompanyName(),
                request.getContractCode(),
                request.getCustomerId(),
                request.getOnsiteContact(),
                request.getOnsitePhone(),
                request.getStatus(),
                request.getLocked(),
                creatorFilter,
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder()
        );

        // 转换为响应DTO
        List<TransportApplyPageResponse> records = noticePage.getRecords().stream()
                .map(this::buildPageResponse)
                .collect(Collectors.toList());

        // 计算统计数据
        List<TransportStat> stats = calculateStats();

        // 构建响应
        TransportApplyListResponse response = new TransportApplyListResponse();
        response.setStats(stats);
        response.setRecords(records);
        response.setTotal(noticePage.getTotal());
        response.setCurrent(noticePage.getCurrent());
        response.setSize(noticePage.getSize());

        return response;
    }

    /**
     * 计算统计数据
     */
    private List<TransportStat> calculateStats() {
        List<TransportStat> stats = new ArrayList<>();
        
        // 查询各状态的收运通知单数量
        long totalCount = pickupNoticeMapper.selectCount(null);
        long pendingAuditCount = pickupNoticeMapper.selectCount(
                new LambdaQueryWrapper<PickupNotice>().eq(PickupNotice::getStatus, "待审核")
        );
        long auditingCount = pickupNoticeMapper.selectCount(
                new LambdaQueryWrapper<PickupNotice>().eq(PickupNotice::getStatus, "审核中")
        );
        long pendingDispatchCount = pickupNoticeMapper.selectCount(
                new LambdaQueryWrapper<PickupNotice>().eq(PickupNotice::getStatus, "待调度")
        );
        long completedCount = pickupNoticeMapper.selectCount(
                new LambdaQueryWrapper<PickupNotice>().eq(PickupNotice::getStatus, "已完成")
        );

        TransportStat stat1 = new TransportStat();
        stat1.setLabel("总数");
        stat1.setValue(String.valueOf(totalCount));
        stat1.setColor("primary");
        stats.add(stat1);

        TransportStat stat2 = new TransportStat();
        stat2.setLabel("待审核");
        stat2.setValue(String.valueOf(pendingAuditCount));
        stat2.setColor("info");
        stats.add(stat2);

        TransportStat stat3 = new TransportStat();
        stat3.setLabel("审核中");
        stat3.setValue(String.valueOf(auditingCount));
        stat3.setColor("warning");
        stats.add(stat3);

        TransportStat stat4 = new TransportStat();
        stat4.setLabel("待调度");
        stat4.setValue(String.valueOf(pendingDispatchCount));
        stat4.setColor("primary");
        stats.add(stat4);

        TransportStat stat5 = new TransportStat();
        stat5.setLabel("已完成");
        stat5.setValue(String.valueOf(completedCount));
        stat5.setColor("success");
        stats.add(stat5);

        return stats;
    }

    @Override
    public List<TransportApplyPageResponse> listPickupNoticesForExport(TransportApplyPageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 应用数据范围控制（viewScope），与分页列表保持一致
        Integer creatorFilter = request.getCreatorId();
        if (!admin) {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:收运通知:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                creatorFilter = currentUserId;
            }
        }

        // 使用分页查询方法，但设置一个很大的分页大小来获取所有数据
        Page<PickupNotice> page = new Page<>(1, Integer.MAX_VALUE);
        
        // 转换日期字符串
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (StrUtil.isNotBlank(request.getStartTime())) {
            startTime = convertToLocalDateTime(request.getStartTime());
        }
        if (StrUtil.isNotBlank(request.getEndTime())) {
            endTime = convertToLocalDateTime(request.getEndTime());
        }

        IPage<PickupNotice> noticePage = pickupNoticeMapper.selectPickupNoticePage(
                page,
                request.getNoticeCode(),
                request.getCompanyName(),
                request.getContractCode(),
                request.getCustomerId(),
                request.getOnsiteContact(),
                request.getOnsitePhone(),
                request.getStatus(),
                request.getLocked(),
                creatorFilter,
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder()
        );

        // 转换为响应DTO
        return noticePage.getRecords().stream()
                .map(this::buildPageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TransportApplyListResponse getVehicleArrangeNoticePage(TransportApplyPageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 使用车辆安排页面专属权限编码查询 viewScope
        Integer creatorFilter = request.getCreatorId();
        if (!admin) {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "运输管理:车辆安排:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                creatorFilter = currentUserId;
            }
        }

        Page<PickupNotice> page = new Page<>(request.getCurrent(), request.getSize());

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (StrUtil.isNotBlank(request.getStartTime())) {
            startTime = convertToLocalDateTime(request.getStartTime());
        }
        if (StrUtil.isNotBlank(request.getEndTime())) {
            endTime = convertToLocalDateTime(request.getEndTime());
        }

        IPage<PickupNotice> noticePage = pickupNoticeMapper.selectPickupNoticePage(
                page,
                request.getNoticeCode(),
                request.getCompanyName(),
                request.getContractCode(),
                request.getCustomerId(),
                request.getOnsiteContact(),
                request.getOnsitePhone(),
                request.getStatus(),
                request.getLocked(),
                creatorFilter,
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder()
        );

        List<TransportApplyPageResponse> records = noticePage.getRecords().stream()
                .map(this::buildPageResponse)
                .collect(Collectors.toList());

        TransportApplyListResponse response = new TransportApplyListResponse();
        response.setRecords(records);
        response.setTotal(noticePage.getTotal());
        response.setCurrent(noticePage.getCurrent());
        response.setSize(noticePage.getSize());
        response.setStats(calculateStats());
        return response;
    }

    @Override
    public List<TransportApplyPageResponse> listVehicleArrangeNoticesForExport(TransportApplyPageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 导出与列表保持一致，使用页面级权限（运输管理:车辆安排:页面）的 viewScope 控制数据范围
        Integer creatorFilter = request.getCreatorId();
        if (!admin) {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "运输管理:车辆安排:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                creatorFilter = currentUserId;
            }
        }

        Page<PickupNotice> page = new Page<>(1, Integer.MAX_VALUE);

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (StrUtil.isNotBlank(request.getStartTime())) {
            startTime = convertToLocalDateTime(request.getStartTime());
        }
        if (StrUtil.isNotBlank(request.getEndTime())) {
            endTime = convertToLocalDateTime(request.getEndTime());
        }

        IPage<PickupNotice> noticePage = pickupNoticeMapper.selectPickupNoticePage(
                page,
                request.getNoticeCode(),
                request.getCompanyName(),
                request.getContractCode(),
                request.getCustomerId(),
                request.getOnsiteContact(),
                request.getOnsitePhone(),
                request.getStatus(),
                request.getLocked(),
                creatorFilter,
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder()
        );

        return noticePage.getRecords().stream()
                .map(this::buildPageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPickupNotice(String noticeCode) {
        Integer currentUserId = getCurrentUserId();
        
        PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        // 检查是否锁定
        if (Boolean.TRUE.equals(notice.getLocked())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "收运通知单已锁定，无法提交");
        }

        // 检查状态（待审核或已驳回才允许提交）
        if (!"待审核".equals(notice.getStatus()) && !"已驳回".equals(notice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不允许提交");
        }

        // 验证必填字段
        if (StrUtil.isBlank(notice.getTransportAddress())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "运输地址不能为空");
        }
        if (StrUtil.isBlank(notice.getOnsiteContact())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "现场联系人不能为空");
        }
        if (StrUtil.isBlank(notice.getOnsitePhone())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "现场联系电话不能为空");
        }
        // 应急联系电话为可选字段，允许为空。若需在未来强制校验，可在此处添加校验逻辑。
        if (notice.getPlanTransferDate() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "计划收运日期不能为空");
        }

        // 验证危废明细
        List<PickupNoticeItem> items = pickupNoticeItemMapper.selectByNoticeCode(noticeCode);
        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "至少需要一条危废明细");
        }

        // 保存旧数据用于日志记录
        TransportApplyDetailResponse oldDetail = null;
        try {
            Customer oldCustomer = null;
            if (notice.getCustomerId() != null) {
                oldCustomer = customerMapper.selectById(notice.getCustomerId());
            }
            Contract oldContract = null;
            if (StrUtil.isNotBlank(notice.getContractCode())) {
                oldContract = contractMapper.selectOne(
                        new LambdaQueryWrapper<Contract>()
                                .eq(Contract::getContractNo, notice.getContractCode())
                );
            }
            oldDetail = buildDetailResponse(notice, oldCustomer, oldContract);
        } catch (Exception e) {
            log.warn("获取收运通知单旧数据失败，将跳过数据变更日志记录", e);
        }

        // 更新状态
        String oldStatus = notice.getStatus();
        notice.setStatus("审核中");
        notice.setSubmittedAt(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        int rows = pickupNoticeMapper.updateById(notice);
        if (rows == 0) {
            throw new BusinessException("提交收运通知单失败：记录已被其他用户修改");
        }

        // 处理OA审批记录
        try {
            // 获取当前用户信息
            String currentUserName = "未知用户";
            try {
                Employee currentEmployee = employeeMapper.selectById(currentUserId);
                if (currentEmployee != null && StrUtil.isNotBlank(currentEmployee.getEmployeeName())) {
                    currentUserName = currentEmployee.getEmployeeName();
                }
            } catch (Exception e) {
                log.warn("获取当前用户姓名失败：{}", currentUserId, e);
            }

            // 查询是否已有该收运通知单的OA审批记录（待审核或已驳回状态）
            OaApprovalRecord existingRecord = oaApprovalRecordService.findLatestBySource(
                    PICKUP_NOTICE_BUSINESS_TYPE, notice.getNoticeId());

            if (existingRecord == null) {
                // 不存在OA审批记录，新增一条
                OaApprovalRecord newRecord = new OaApprovalRecord();
                newRecord.setApprovalNo(generateApprovalNo());
                newRecord.setSourceTable(PICKUP_NOTICE_BUSINESS_TYPE);
                newRecord.setSourceTableName("收运通知单表");
                newRecord.setSourceId(notice.getNoticeId());
                newRecord.setSourceNo(notice.getNoticeCode());
                newRecord.setTitle("收运通知单审核：" + notice.getNoticeCode());
                newRecord.setSubmitterId(currentUserId);
                newRecord.setSubmitterName(currentUserName);
                newRecord.setApprovalStatus("待审核");
                newRecord.setApprovalCount(1);
                newRecord.setSubmitTime(LocalDateTime.now());
                newRecord.setDeleted(0);

                oaApprovalRecordMapper.insert(newRecord);
                log.info("创建收运通知单OA审批记录成功：noticeCode={}, approvalNo={}",
                        notice.getNoticeCode(), newRecord.getApprovalNo());
            } else {
                // 已存在OA审批记录，审核次数+1，状态改为待审核
                existingRecord.setApprovalStatus("待审核");
                existingRecord.setApprovalCount(existingRecord.getApprovalCount() + 1);
                existingRecord.setSubmitTime(LocalDateTime.now());
                existingRecord.setApproverId(null);
                existingRecord.setApproverName(null);
                existingRecord.setApprovalTime(null);

                oaApprovalRecordMapper.updateById(existingRecord);
                log.info("更新收运通知单OA审批记录成功：noticeCode={}, approvalCount={}",
                        notice.getNoticeCode(), existingRecord.getApprovalCount());
            }
        } catch (Exception e) {
            log.error("处理OA审批记录失败：noticeCode={}", noticeCode, e);
            // OA记录处理失败不影响主流程
        }

        log.info("提交收运通知单审核成功：noticeCode={}, operator={}", noticeCode, currentUserId);

        // 记录操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "提交审核",
                    "提交收运通知单审核：收运通知单号=" + noticeCode + "，状态变更：" + oldStatus + "→审核中",
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录收运通知单提交审核操作日志失败", e);
        }

        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                PickupNotice updatedNotice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
                Customer newCustomer = oldDetail.getCustomerId() != null ? customerMapper.selectById(oldDetail.getCustomerId()) : null;
                Contract newContract = StrUtil.isNotBlank(updatedNotice.getContractCode()) ? 
                        contractMapper.selectOne(new LambdaQueryWrapper<Contract>().eq(Contract::getContractNo, updatedNotice.getContractCode())) : null;
                TransportApplyDetailResponse newDetail = buildDetailResponse(updatedNotice, newCustomer, newContract);
                logRecordService.recordDataChangeLog("收运通知单管理", "PICKUP_NOTICE",
                        String.valueOf(notice.getNoticeId()),
                        "提交审核",
                        "提交收运通知单审核：收运通知单号=" + noticeCode + "，状态变更：" + oldStatus + "→审核中",
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.error("记录收运通知单提交审核数据变更日志失败", e);
            }
        }

        // 发送提交审核通知给审核人员
        try {
            sendPickupNoticeAuditNotification(notice, currentUserId, "提交审核");
        } catch (Exception e) {
            log.error("发送收运通知单提交审核通知失败：noticeCode={}", noticeCode, e);
            // 消息发送失败不影响主流程
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokePickupNotice(String noticeCode) {
        Integer currentUserId = getCurrentUserId();
        
        PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        // 检查状态（只有审核中状态才能撤回）
        if (!"审核中".equals(notice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不允许撤回");
        }

        // 保存旧数据用于日志记录
        TransportApplyDetailResponse oldDetail = null;
        try {
            Customer oldCustomer = null;
            if (notice.getCustomerId() != null) {
                oldCustomer = customerMapper.selectById(notice.getCustomerId());
            }
            Contract oldContract = null;
            if (StrUtil.isNotBlank(notice.getContractCode())) {
                oldContract = contractMapper.selectOne(
                        new LambdaQueryWrapper<Contract>()
                                .eq(Contract::getContractNo, notice.getContractCode())
                );
            }
            oldDetail = buildDetailResponse(notice, oldCustomer, oldContract);
        } catch (Exception e) {
            log.warn("获取收运通知单旧数据失败，将跳过数据变更日志记录", e);
        }

        // 更新状态
        String oldStatus = notice.getStatus();
        notice.setStatus("待审核");
        notice.setSubmittedAt(null);
        notice.setUpdateTime(LocalDateTime.now());
        int rows = pickupNoticeMapper.updateById(notice);
        if (rows == 0) {
            throw new BusinessException("撤回收运通知单失败：记录已被其他用户修改");
        }

        log.info("撤回收运通知单成功：noticeCode={}, operator={}", noticeCode, currentUserId);

        // 记录操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "撤回",
                    "撤回收运通知单：收运通知单号=" + noticeCode + "，状态变更：" + oldStatus + "→待审核",
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录收运通知单撤回操作日志失败", e);
        }

        // 发送撤回通知（使用基于权限的通知方法）
        try {
            Integer creatorId = notice.getCreatorId();
            if (creatorId != null) {
                String customerName = "未知客户";
                if (notice.getCustomerId() != null) {
                    Customer customer = customerMapper.selectById(notice.getCustomerId());
                    if (customer != null && customer.getEnterpriseName() != null) {
                        customerName = customer.getEnterpriseName();
                    }
                }
                // 使用基于权限的通知方法
                messageNotificationService.sendApprovalRevokeNotification(
                        "PICKUP_NOTICE_REVOKE",
                        notice.getNoticeId(),
                        String.format("收运通知单【%s】", noticeCode),
                        currentUserId
                );
                log.info("收运通知单撤回通知已发送：noticeCode={}", noticeCode);
            }
        } catch (Exception e) {
            log.error("发送收运通知单撤回通知失败：noticeCode={}", noticeCode, e);
        }

        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                PickupNotice updatedNotice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
                Customer newCustomer = oldDetail.getCustomerId() != null ? customerMapper.selectById(oldDetail.getCustomerId()) : null;
                Contract newContract = StrUtil.isNotBlank(updatedNotice.getContractCode()) ? 
                        contractMapper.selectOne(new LambdaQueryWrapper<Contract>().eq(Contract::getContractNo, updatedNotice.getContractCode())) : null;
                TransportApplyDetailResponse newDetail = buildDetailResponse(updatedNotice, newCustomer, newContract);
                logRecordService.recordDataChangeLog("收运通知单管理", "PICKUP_NOTICE",
                        String.valueOf(notice.getNoticeId()),
                        "撤回",
                        "撤回收运通知单：收运通知单号=" + noticeCode + "，状态变更：" + oldStatus + "→待审核",
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.error("记录收运通知单撤回数据变更日志失败", e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPickupNotice(String noticeCode, String auditResult, String auditOpinion) {
        Integer currentUserId = getCurrentUserId();
        
        PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        // 检查状态（只有审核中状态才能审核）
        if (!"审核中".equals(notice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), 
                    "当前状态为\"" + notice.getStatus() + "\"，只有审核中状态的收运通知单才能进行审核操作");
        }

        // 验证审核结果
        if (StrUtil.isBlank(auditResult)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "审核结果不能为空");
        }

        // 验证审核结果值：支持的状态值
        List<String> validStatuses = Arrays.asList("待调度", "已驳回");
        if (!validStatuses.contains(auditResult)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), 
                    "审核结果必须是：待调度（通过）或已驳回（驳回）");
        }

        // 如果拒绝时，审核意见必填
        if ("已驳回".equals(auditResult)) {
            if (StrUtil.isBlank(auditOpinion)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), 
                        "拒绝时审核意见不能为空，请详细说明拒绝原因");
            }
        }

        // 保存旧数据用于日志记录
        TransportApplyDetailResponse oldDetail = null;
        Customer oldCustomer = null;
        Contract oldContract = null;
        try {
            if (notice.getCustomerId() != null) {
                oldCustomer = customerMapper.selectById(notice.getCustomerId());
            }
            if (StrUtil.isNotBlank(notice.getContractCode())) {
                oldContract = contractMapper.selectOne(
                        new LambdaQueryWrapper<Contract>()
                                .eq(Contract::getContractNo, notice.getContractCode())
                );
            }
            oldDetail = buildDetailResponse(notice, oldCustomer, oldContract);
        } catch (Exception e) {
            log.warn("获取收运通知单旧数据失败，将跳过数据变更日志记录", e);
        }

        // 更新状态
        notice.setStatus(auditResult);
        notice.setAuditedAt(LocalDateTime.now());
        notice.setAuditorId(currentUserId);
        notice.setUpdateTime(LocalDateTime.now());

        // 保存审核意见到独立的审核意见字段
        if (StrUtil.isNotBlank(auditOpinion)) {
            notice.setAuditOpinion(auditOpinion.trim());
        }

        int rows = pickupNoticeMapper.updateById(notice);
        if (rows == 0) {
            throw new BusinessException("审核收运通知单失败：记录已被其他用户修改");
        }

        log.info("审核收运通知单成功：noticeCode={}, auditResult={}, auditOpinion={}, operator={}", 
                noticeCode, auditResult, auditOpinion, currentUserId);

        // 记录操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "审核",
                    "审核收运通知单：收运通知单号=" + noticeCode + "，审核结果=" + auditResult,
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录收运通知单审核操作日志失败", e);
        }

        // 同步更新 OA 审核记录表状态
        try {
            // 根据来源表名和来源记录ID查找待审核的OA记录
            com.erp.entity.oa.OaApprovalRecord oaRecord =
                    oaApprovalRecordService.findPendingBySource("PICKUP_NOTICE", notice.getNoticeId());

            if (oaRecord != null) {
                // 获取审核人姓名
                String approverName = null;
                if (currentUserId != null) {
                    try {
                        com.erp.entity.system.Employee employee = employeeMapper.selectById(currentUserId);
                        if (employee != null) {
                            approverName = employee.getEmployeeName();
                        }
                    } catch (Exception e) {
                        log.warn("获取审核人姓名失败：userId={}", currentUserId, e);
                    }
                }

                // 将审核结果映射为OA审核结果（待调度->通过，已驳回->驳回）
                String oaResult = "待调度".equals(auditResult) ? "通过" : "驳回";

                oaApprovalRecordService.approve(
                        oaRecord.getApprovalRecordId(),
                        "PICKUP_NOTICE",
                        notice.getNoticeId(),
                        oaResult,
                        auditOpinion,
                        currentUserId,
                        approverName
                );

                log.info("同步更新OA审核记录成功：noticeId={}, oaRecordId={}, oaResult={}",
                        notice.getNoticeId(), oaRecord.getApprovalRecordId(), oaResult);
            } else {
                log.warn("未找到收运通知单对应的待审核OA记录：noticeId={}", notice.getNoticeId());
            }
        } catch (Exception e) {
            log.error("同步更新OA审核记录失败：noticeId={}, auditResult={}", notice.getNoticeId(), auditResult, e);
            // OA记录更新失败不影响主流程
        }

        // 发送审核结果通知给创建人
        try {
            String action = "待调度".equals(auditResult) ? "审核通过" : "审核驳回";
            String businessTitle = String.format("收运通知单【%s】", noticeCode);
            // 使用基于权限的通知方法，发送给有收运通知页面权限的人员
            messageNotificationService.sendAuditResultNotification(
                    "PICKUP_NOTICE_AUDIT_RESULT",
                    notice.getNoticeId(),
                    businessTitle,
                    action,
                    currentUserId
            );
            log.info("收运通知单审核结果通知已发送：noticeCode={}, action={}", noticeCode, action);
        } catch (Exception e) {
            log.error("发送收运通知单审核结果通知失败：noticeCode={}", noticeCode, e);
        }

        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                // 重新查询更新后的数据
                PickupNotice updatedNotice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
                Customer newCustomer = oldCustomer;
                Contract newContract = oldContract;
                
                // 如果客户或合同信息可能已变更，重新查询
                if (updatedNotice != null) {
                    if (updatedNotice.getCustomerId() != null && 
                        (oldCustomer == null || !updatedNotice.getCustomerId().equals(oldCustomer.getCustomerId()))) {
                        newCustomer = customerMapper.selectById(updatedNotice.getCustomerId());
                    }
                    if (StrUtil.isNotBlank(updatedNotice.getContractCode()) && 
                        (oldContract == null || !updatedNotice.getContractCode().equals(oldContract.getContractNo()))) {
                        newContract = contractMapper.selectOne(
                                new LambdaQueryWrapper<Contract>()
                                        .eq(Contract::getContractNo, updatedNotice.getContractCode())
                        );
                    }
                }
                
                TransportApplyDetailResponse newDetail = buildDetailResponse(
                        updatedNotice != null ? updatedNotice : notice, 
                        newCustomer, 
                        newContract);
                logRecordService.recordDataChangeLog("收运通知单管理", "PICKUP_NOTICE",
                        String.valueOf(notice.getNoticeId()),
                        "审核",
                        "审核收运通知单：收运通知单号=" + noticeCode + "，审核结果=" + auditResult,
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.error("记录收运通知单审核数据变更日志失败", e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindContractDuringAudit(String noticeCode, String contractCode) {
        Integer currentUserId = getCurrentUserId();

        if (StrUtil.isBlank(noticeCode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "收运通知单号不能为空");
        }
        if (StrUtil.isBlank(contractCode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "合同号不能为空");
        }

        PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        // 锁定校验
        if (Boolean.TRUE.equals(notice.getLocked())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "收运通知单已锁定，无法补充合同");
        }

        // 状态校验：仅审核中允许补充合同
        if (!"审核中".equals(notice.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "仅审核中状态可补充合同号");
        }

        // 校验合同是否存在
        Contract contract = contractMapper.selectOne(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractNo, contractCode.trim())
        );
        if (contract == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "合同不存在");
        }

        notice.setContractCode(contractCode.trim());
        // 保存旧数据用于日志记录
        TransportApplyDetailResponse oldDetail = null;
        try {
            Customer oldCustomer = null;
            if (notice.getCustomerId() != null) {
                oldCustomer = customerMapper.selectById(notice.getCustomerId());
            }
            Contract oldContract = null;
            if (StrUtil.isNotBlank(notice.getContractCode())) {
                oldContract = contractMapper.selectOne(
                        new LambdaQueryWrapper<Contract>()
                                .eq(Contract::getContractNo, notice.getContractCode())
                );
            }
            oldDetail = buildDetailResponse(notice, oldCustomer, oldContract);
        } catch (Exception e) {
            log.warn("获取收运通知单旧数据失败，将跳过数据变更日志记录", e);
        }

        notice.setContractPending(false);
        notice.setUpdateTime(LocalDateTime.now());
        int rows = pickupNoticeMapper.updateById(notice);
        if (rows == 0) {
            log.warn("审核阶段补充合同失败（乐观锁冲突），noticeCode={}", noticeCode);
        }

        log.info("审核阶段补充合同成功：noticeCode={}, contractCode={}, operator={}", noticeCode, contractCode, currentUserId);

        // 记录操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "审核阶段补充合同",
                    "审核阶段补充合同：收运通知单号=" + noticeCode + "，合同号=" + contractCode,
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录收运通知单审核阶段补充合同操作日志失败", e);
        }

        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                PickupNotice updatedNotice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
                Customer newCustomer = oldDetail.getCustomerId() != null ? customerMapper.selectById(oldDetail.getCustomerId()) : null;
                TransportApplyDetailResponse newDetail = buildDetailResponse(updatedNotice, newCustomer, contract);
                logRecordService.recordDataChangeLog("收运通知单管理", "PICKUP_NOTICE",
                        String.valueOf(notice.getNoticeId()),
                        "审核阶段补充合同",
                        "审核阶段补充合同：收运通知单号=" + noticeCode + "，合同号=" + contractCode,
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.error("记录收运通知单审核阶段补充合同数据变更日志失败", e);
            }
        }

        // 发送补充合同通知给创建人
        try {
            Integer creatorId = notice.getCreatorId();
            if (creatorId != null) {
                String customerName = "未知客户";
                if (notice.getCustomerId() != null) {
                    Customer customer = customerMapper.selectById(notice.getCustomerId());
                    if (customer != null && customer.getEnterpriseName() != null) {
                        customerName = customer.getEnterpriseName();
                    }
                }
                String content = String.format("收运通知单【%s】已补充合同号【%s】，客户：%s，请查看详情。", 
                        noticeCode, contractCode, customerName);
                // 使用基于权限的通知方法
                messageNotificationService.sendBusinessNotificationByPermission(
                        "PICKUP_NOTICE_UPDATE",
                        notice.getNoticeId(),
                        "收运通知单已补充合同",
                        content,
                        currentUserId,
                        "生产"
                );
                log.info("收运通知单补充合同通知已发送：noticeCode={}", noticeCode);
            }
        } catch (Exception e) {
            log.error("发送收运通知单补充合同通知失败：noticeCode={}", noticeCode, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePickupNotice(String noticeCode) {
        Integer currentUserId = getCurrentUserId();
        
        PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        // 检查是否锁定
        if (Boolean.TRUE.equals(notice.getLocked())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "收运通知单已锁定，无法删除");
        }

        // 检查状态（只有待审核或已驳回状态才能删除）
        String status = notice.getStatus();
        if (!"待审核".equals(status) && !"已驳回".equals(status)) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), 
                    "只有待审核或已驳回状态的收运通知单可以删除");
        }

        // 检查是否已生成运输单（如果已生成运输单，不允许删除）
        int dispatchOrderCount = dispatchOrderMapper.countByNoticeCode(noticeCode);
        if (dispatchOrderCount > 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), 
                    "该收运通知单已生成运输单，无法删除");
        }

        // 删除附件（先查询附件列表，然后删除物理文件和数据库记录）
        try {
            List<File> attachments = fileMapper.selectByBusinessTypeAndId("TRANSPORT_APPLY", notice.getNoticeId());
            if (attachments != null && !attachments.isEmpty()) {
                for (File file : attachments) {
                    // 删除物理文件（如果存在）
                    if (file.getLocalPath() != null && !file.getLocalPath().trim().isEmpty()) {
                        try {
                            String fullPath = localStoragePath + "/" + file.getLocalPath();
                            java.io.File fileObj = new java.io.File(fullPath);
                            if (fileObj.exists() && fileObj.isFile()) {
                                boolean deleted = fileObj.delete();
                                if (deleted) {
                                    log.info("删除收运通知单附件物理文件成功：fileId={}, filePath={}", 
                                            file.getFileId(), fullPath);
                                } else {
                                    log.warn("删除收运通知单附件物理文件失败：fileId={}, filePath={}", 
                                            file.getFileId(), fullPath);
                                }
                            }
                        } catch (Exception e) {
                            log.error("删除收运通知单附件物理文件异常：fileId={}, filePath={}", 
                                    file.getFileId(), file.getLocalPath(), e);
                        }
                    }
                    // 物理删除数据库记录
                    try {
                        fileMapper.deleteById(file.getFileId());
                        log.info("删除收运通知单附件数据库记录成功：fileId={}, noticeCode={}", 
                                file.getFileId(), noticeCode);
                    } catch (Exception e) {
                        log.error("删除收运通知单附件数据库记录异常：fileId={}, noticeCode={}", 
                                file.getFileId(), noticeCode, e);
                    }
                }
                log.info("删除收运通知单附件成功：noticeCode={}, attachmentCount={}", 
                        noticeCode, attachments.size());
            }
        } catch (Exception e) {
            log.warn("删除收运通知单附件失败：noticeCode={}", noticeCode, e);
            // 附件删除失败不影响主流程，继续执行
        }

        // 删除危废明细
        pickupNoticeItemMapper.deleteByNoticeCode(noticeCode);

        // 删除收运通知单
        int rows = pickupNoticeMapper.deleteById(notice.getNoticeId());
        if (rows == 0) {
            log.warn("删除收运通知单失败，noticeId={}", notice.getNoticeId());
        }

        log.info("删除收运通知单成功：noticeCode={}, operator={}", noticeCode, currentUserId);

        // 记录操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "删除",
                    "删除收运通知单：收运通知单号=" + noticeCode,
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录收运通知单删除操作日志失败", e);
        }
    }

    /**
     * 保存收运通知单危废明细
     */
    private void savePickupNoticeItems(String noticeCode, List<TransportApplyItemRequest> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        for (TransportApplyItemRequest itemRequest : items) {
            // 跳过空明细（前端可能传递空对象）
            if (itemRequest == null) {
                continue;
            }
            // 如果废物名称和代码都为空，跳过
            if (StrUtil.isBlank(itemRequest.getWasteName()) && StrUtil.isBlank(itemRequest.getWasteCode())) {
                continue;
            }
            PickupNoticeItem item = new PickupNoticeItem();
            item.setNoticeCode(noticeCode);
            item.setContractWasteItemId(itemRequest.getContractWasteItemId());

            // 设置危废条目编号：优先使用前端传递的值，其次从合同查询，最后从废物代码查询
            if (itemRequest.getHazardousWasteItemId() != null) {
                // 1. 优先使用前端传递的危废条目编号
                item.setHazardousWasteItemId(itemRequest.getHazardousWasteItemId());
            } else if (itemRequest.getContractWasteItemId() != null) {
                // 2. 从合同危废明细中查询
                com.erp.entity.contract.ContractWasteItem contractWasteItem =
                    contractWasteItemMapper.selectById(itemRequest.getContractWasteItemId());
                if (contractWasteItem != null && contractWasteItem.getHazardousWasteItemId() != null) {
                    item.setHazardousWasteItemId(contractWasteItem.getHazardousWasteItemId());
                }
            } else if (StrUtil.isNotBlank(itemRequest.getWasteCode())) {
                // 3. 根据废物代码从HAZARDOUS_WASTE_ITEM表查询
                com.erp.entity.system.HazardousWasteItem wasteItem =
                    hazardousWasteItemMapper.selectOne(
                        new LambdaQueryWrapper<com.erp.entity.system.HazardousWasteItem>()
                            .eq(com.erp.entity.system.HazardousWasteItem::getWasteCode, itemRequest.getWasteCode().trim())
                    );
                if (wasteItem != null) {
                    item.setHazardousWasteItemId(wasteItem.getItemId());
                }
            }

            item.setWasteName(itemRequest.getWasteName());
            item.setWasteCode(itemRequest.getWasteCode());
            item.setHazardFeature(itemRequest.getHazardFeature());
            item.setForm(itemRequest.getForm());
            item.setHazardousComponentName(itemRequest.getHazardousComponentName());
            
            // ========== 基本核算相关字段 ==========
            // 计划转移数量（基本核算数量）：-1表示不限量
            if (itemRequest.getPlannedQtyTon() != null) {
                item.setPlannedQtyTon(itemRequest.getPlannedQtyTon());
            } else {
                // 如果没有提供，默认为0（不限量时前端会传-1）
                item.setPlannedQtyTon(java.math.BigDecimal.ZERO);
            }
            // 基本计量单位
            item.setMeasureUnit(itemRequest.getMeasureUnit());
            
            // ========== 辅助核算相关字段 ==========
            // 是否启用辅助核算
            item.setEnableAuxiliaryAccounting(itemRequest.getEnableAuxiliaryAccounting() != null 
                    ? itemRequest.getEnableAuxiliaryAccounting() : false);
            // 辅助计量单位
            item.setAuxUnit(itemRequest.getAuxUnit());
            // 辅助单位每基础单位数量（换算比例）
            item.setAuxPerBase(itemRequest.getAuxPerBase());
            // 辅助数量（辅助核算数量）
            // 兼容处理：如果 auxQuantity 为空但 auxUnitQty 有值，使用 auxUnitQty
            if (itemRequest.getAuxQuantity() != null) {
                item.setAuxQuantity(itemRequest.getAuxQuantity());
            } else if (itemRequest.getAuxUnitQty() != null) {
                item.setAuxQuantity(itemRequest.getAuxUnitQty());
            }
            
            // ========== 兼容性字段（将辅助核算数据同步到包装字段） ==========
            // 将辅助核算数据存到包装字段（用于兼容数据库表结构）
            // auxQuantity -> packageQty, auxUnit -> packageType
            if (item.getAuxQuantity() != null) {
                item.setPackageQty(item.getAuxQuantity());
            }
            if (item.getAuxUnit() != null) {
                item.setPackageType(item.getAuxUnit());
            }
            
            item.setCreateTime(LocalDateTime.now());
            pickupNoticeItemMapper.insert(item);
        }
    }

    /**
     * 保存附件
     * 将附件信息保存到FILE表，并在PICKUP_NOTICE表的字段中存储文件ID列表（JSON格式）
     */
    private void saveAttachments(String noticeCode, List<TransportAttachmentRequest> attachments) {
        // 查询收运通知单，获取noticeId
        PickupNotice notice = pickupNoticeMapper.selectOne(
                new LambdaQueryWrapper<PickupNotice>()
                        .eq(PickupNotice::getNoticeCode, noticeCode)
        );
        if (notice == null) {
            log.warn("收运通知单不存在，无法保存附件：noticeCode={}", noticeCode);
            return;
        }

        // 分离货物照片/视频和二维码
        List<Integer> cargoFileIds = new ArrayList<>();
        List<Integer> qrcodeFileIds = new ArrayList<>();

        // 如果附件列表不为空，处理附件
        if (!CollectionUtils.isEmpty(attachments)) {
            for (TransportAttachmentRequest attachment : attachments) {
                if (StrUtil.isBlank(attachment.getFileId())) {
                    continue;
                }

                try {
                    Integer fileId = Integer.parseInt(attachment.getFileId());
                    
                    // 更新FILE表的业务关联信息
                    File file = fileMapper.selectById(fileId);
                    if (file != null) {
                        file.setBusinessId(notice.getNoticeId());
                        file.setBusinessType("TRANSPORT_APPLY");
                        file.setBusinessModule("收运通知单");
                        fileMapper.updateById(file);
                    }

                    // 根据附件类型分类
                    if ("cargo".equals(attachment.getType())) {
                        cargoFileIds.add(fileId);
                    } else if ("qrcode".equals(attachment.getType())) {
                        qrcodeFileIds.add(fileId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("文件ID格式错误：fileId={}", attachment.getFileId());
                }
            }
        }

        // 更新收运通知单的附件字段
        // 危废详情文件字段存储货物照片/视频的文件ID列表（JSON数组）
        // 产废单位二维码字段存储二维码的文件ID（单个ID字符串）
        notice.setWasteDetailFile(cargoFileIds.isEmpty() ? null : JSONUtil.toJsonStr(cargoFileIds));
        notice.setQrCode(qrcodeFileIds.isEmpty() ? null : String.valueOf(qrcodeFileIds.get(0)));
        
        pickupNoticeMapper.updateById(notice);
        
        log.info("保存附件成功：noticeCode={}, cargoCount={}, qrcodeCount={}", 
                noticeCode, cargoFileIds.size(), qrcodeFileIds.size());
    }

    /**
     * 构建详情响应
     */
    private TransportApplyDetailResponse buildDetailResponse(PickupNotice notice, Customer customer, Contract contract) {
        TransportApplyDetailResponse response = new TransportApplyDetailResponse();
        response.setNoticeId(notice.getNoticeId());
        response.setNoticeCode(notice.getNoticeCode());
        response.setContractCode(notice.getContractCode());
        // 如果有关联合同，设置合同ID
        if (contract != null) {
            response.setContractId(contract.getContractId());
        }
        response.setContractPending(notice.getContractPending());
        if (notice.getContractFixTime() != null) {
            response.setContractFixTime(notice.getContractFixTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        response.setCustomerId(notice.getCustomerId());
        if (customer != null) {
            response.setCustomerName(customer.getEnterpriseName());
        }
        response.setCompanyName(notice.getCompanyName());
        response.setCreditCode(notice.getCreditCode());
        response.setTransportAddress(notice.getTransportAddress());
        response.setOnsiteContact(notice.getOnsiteContact());
        response.setOnsitePhone(notice.getOnsitePhone());
        response.setEmergencyPhone(notice.getEmergencyPhone());
        
        // 业务联系人和电话从合同或客户信息中获取
        if (contract != null) {
            response.setBusinessContact(contract.getPartyAContact());
            response.setBusinessPhone(contract.getPartyAContactPhone());
        } else if (customer != null) {
            response.setBusinessContact(customer.getContactPerson());
            response.setBusinessPhone(customer.getContactPhone());
        }
        
        if (notice.getPlanTransferDate() != null) {
            response.setPlanTransferDate(notice.getPlanTransferDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (notice.getSubmittedAt() != null) {
            response.setSubmitDate(notice.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        response.setRemark(notice.getRemark());
        response.setStatus(notice.getStatus());
        if (notice.getSubmittedAt() != null) {
            response.setSubmittedAt(notice.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (notice.getAuditedAt() != null) {
            response.setAuditedAt(notice.getAuditedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        response.setAuditOpinion(notice.getAuditOpinion());
        response.setLocked(notice.getLocked());
        response.setLockReason(notice.getLockReason());
        if (notice.getCreateTime() != null) {
            response.setCreatedAt(notice.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (notice.getUpdateTime() != null) {
            response.setUpdateTime(notice.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        // 直接将审核人/创建人编码、附件字段及锁定时间/人编码返回给前端，便于页面展示和权限判断
        response.setAuditorId(notice.getAuditorId());
        response.setCreatorId(notice.getCreatorId());
        response.setWasteDetailFile(notice.getWasteDetailFile());
        response.setQrCode(notice.getQrCode());
        if (notice.getLockTime() != null) {
            response.setLockTime(notice.getLockTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        response.setLockUserId(notice.getLockUserId());

        // 查询创建人和审核人信息
        if (notice.getCreatorId() != null) {
            Employee creator = employeeMapper.selectById(notice.getCreatorId());
            if (creator != null) {
                response.setCreatorName(creator.getEmployeeName());
            }
        }
        if (notice.getAuditorId() != null) {
            Employee auditor = employeeMapper.selectById(notice.getAuditorId());
            if (auditor != null) {
                response.setAuditorName(auditor.getEmployeeName());
            }
        }

        // 查询危废明细
        List<PickupNoticeItem> items = pickupNoticeItemMapper.selectByNoticeCode(notice.getNoticeCode());
        List<TransportApplyItemResponse> itemResponses = items.stream()
                .map(this::buildItemResponse)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        // 查询附件列表
        response.setAttachments(loadAttachments(notice));

        return response;
    }

    /**
     * 加载附件列表
     * 从数据库字段中读取文件ID，然后查询FILE表获取文件信息
     */
    private List<TransportAttachmentResponse> loadAttachments(PickupNotice notice) {
        List<TransportAttachmentResponse> attachments = new ArrayList<>();
        
        if (notice == null) {
            return attachments;
        }

        // 读取货物照片/视频（从危废详情文件字段）
        if (StrUtil.isNotBlank(notice.getWasteDetailFile())) {
            try {
                List<Integer> cargoFileIds = JSONUtil.toList(notice.getWasteDetailFile(), Integer.class);
                for (Integer fileId : cargoFileIds) {
                    File file = fileMapper.selectById(fileId);
                    if (file != null) {
                        TransportAttachmentResponse attachment = new TransportAttachmentResponse();
                        attachment.setFileId(String.valueOf(file.getFileId()));
                        attachment.setFileName(file.getFileName());
                        attachment.setFileUrl(file.getFileUrl() != null ? file.getFileUrl() : 
                                "/api/file/download?path=" + file.getLocalPath());
                        attachment.setType("cargo");
                        attachment.setSize(file.getFileSize());
                        attachments.add(attachment);
                    }
                }
            } catch (Exception e) {
                log.warn("解析货物照片/视频文件ID失败：wasteDetailFile={}", notice.getWasteDetailFile(), e);
            }
        }

        // 读取二维码（从产废单位二维码字段）
        if (StrUtil.isNotBlank(notice.getQrCode())) {
            try {
                // 二维码字段可能存储单个文件ID（字符串）或JSON数组
                Integer qrcodeFileId = null;
                try {
                    qrcodeFileId = Integer.parseInt(notice.getQrCode());
                } catch (NumberFormatException e) {
                    // 如果是JSON数组，尝试解析
                    List<Integer> qrcodeFileIds = JSONUtil.toList(notice.getQrCode(), Integer.class);
                    if (!qrcodeFileIds.isEmpty()) {
                        qrcodeFileId = qrcodeFileIds.get(0);
                    }
                }
                
                if (qrcodeFileId != null) {
                    File file = fileMapper.selectById(qrcodeFileId);
                    if (file != null) {
                        TransportAttachmentResponse attachment = new TransportAttachmentResponse();
                        attachment.setFileId(String.valueOf(file.getFileId()));
                        attachment.setFileName(file.getFileName());
                        attachment.setFileUrl(file.getFileUrl() != null ? file.getFileUrl() : 
                                "/api/file/download?path=" + file.getLocalPath());
                        attachment.setType("qrcode");
                        attachment.setSize(file.getFileSize());
                        attachments.add(attachment);
                    }
                }
            } catch (Exception e) {
                log.warn("解析二维码文件ID失败：qrCode={}", notice.getQrCode(), e);
            }
        }

        return attachments;
    }

    /**
     * 构建分页响应
     */
    private TransportApplyPageResponse buildPageResponse(PickupNotice notice) {
        TransportApplyPageResponse response = new TransportApplyPageResponse();
        response.setNoticeId(notice.getNoticeId());
        response.setNoticeCode(notice.getNoticeCode());
        response.setContractCode(notice.getContractCode());
        response.setContractPending(notice.getContractPending());
        response.setCustomerId(notice.getCustomerId());
        // 列表需要返回创建人编码，用于前端基于“操作范围(SELF/ALL)”做行级控制
        response.setCreatorId(notice.getCreatorId());
        // 同步返回审核人/锁定人编码，便于前端展示或权限判断（字段存在则返回）
        response.setAuditorId(notice.getAuditorId());
        response.setLockUserId(notice.getLockUserId());
        if (notice.getLockTime() != null) {
            response.setLockTime(notice.getLockTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        response.setContractFixTime(notice.getContractFixTime() != null
                ? notice.getContractFixTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null);
        
        // 设置客户名称：优先使用通知单中的单位名称，如果为空则从客户表获取
        String companyName = notice.getCompanyName();
        if (StrUtil.isBlank(companyName) && notice.getCustomerId() != null) {
            try {
                Customer customer = customerMapper.selectById(notice.getCustomerId());
                if (customer != null && StrUtil.isNotBlank(customer.getEnterpriseName())) {
                    companyName = customer.getEnterpriseName();
                }
            } catch (Exception e) {
                log.warn("获取客户信息失败，customerId={}", notice.getCustomerId(), e);
            }
        }
        response.setCompanyName(StrUtil.blankToDefault(companyName, ""));
        
        response.setCreditCode(notice.getCreditCode());
        response.setTransportAddress(notice.getTransportAddress());
        response.setOnsiteContact(notice.getOnsiteContact());
        response.setOnsitePhone(notice.getOnsitePhone());
        response.setEmergencyPhone(notice.getEmergencyPhone());
        if (notice.getPlanTransferDate() != null) {
            String planDate = notice.getPlanTransferDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            response.setPlanTransferDate(planDate);
            response.setTransferTime(planDate); // 兼容前端字段
        }
        response.setStatus(notice.getStatus());
        if (notice.getSubmittedAt() != null) {
            response.setSubmittedAt(notice.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (notice.getAuditedAt() != null) {
            response.setAuditedAt(notice.getAuditedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        response.setAuditOpinion(notice.getAuditOpinion());
        response.setLocked(notice.getLocked());
        response.setLockReason(notice.getLockReason());
        response.setRemark(notice.getRemark());
        if (notice.getCreateTime() != null) {
            response.setCreatedAt(notice.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (notice.getUpdateTime() != null) {
            response.setUpdateTime(notice.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        // 查询创建人和审核人信息
        if (notice.getCreatorId() != null) {
            Employee creator = employeeMapper.selectById(notice.getCreatorId());
            if (creator != null) {
                response.setCreatorName(creator.getEmployeeName());
            }
        }
        if (notice.getAuditorId() != null) {
            Employee auditor = employeeMapper.selectById(notice.getAuditorId());
            if (auditor != null) {
                response.setAuditorName(auditor.getEmployeeName());
            }
        }

        return response;
    }

    /**
     * 构建危废明细响应
     */
    private TransportApplyItemResponse buildItemResponse(PickupNoticeItem item) {
        TransportApplyItemResponse response = new TransportApplyItemResponse();
        
        // ========== 基本信息字段 ==========
        response.setId(item.getItemId());
        response.setWasteName(item.getWasteName());
        response.setWasteCode(item.getWasteCode());
        response.setHazardFeature(item.getHazardFeature());
        response.setForm(item.getForm());
        response.setHazardousComponentName(item.getHazardousComponentName());
        response.setContractWasteItemId(item.getContractWasteItemId());
        
        // ========== 基本核算相关字段 ==========
        // 计划转移数量（基本核算数量）：-1表示不限量
        response.setPlannedQtyTon(item.getPlannedQtyTon());
        // 基本计量单位
        response.setMeasureUnit(item.getMeasureUnit());
        
        // ========== 辅助核算相关字段 ==========
        // 是否启用辅助核算
        response.setEnableAuxiliaryAccounting(item.getEnableAuxiliaryAccounting() != null 
                ? item.getEnableAuxiliaryAccounting() : false);
        // 辅助计量单位
        // 优先从 auxUnit 读取，如果为空则从 packageType 读取（兼容旧数据）
        if (item.getAuxUnit() != null) {
            response.setAuxUnit(item.getAuxUnit());
        } else if (item.getPackageType() != null) {
            // 如果 auxUnit 为空，从 packageType 读取（兼容旧数据）
            response.setAuxUnit(item.getPackageType());
        }
        // 辅助单位每基础单位数量（换算比例）
        response.setAuxPerBase(item.getAuxPerBase());
        // 辅助数量（辅助核算数量）
        // 优先从 auxQuantity 读取，如果为空则从 packageQty 读取（兼容旧数据）
        if (item.getAuxQuantity() != null) {
            response.setAuxQuantity(item.getAuxQuantity());
        } else if (item.getPackageQty() != null) {
            // 如果 auxQuantity 为空，从 packageQty 读取（兼容旧数据）
            response.setAuxQuantity(item.getPackageQty());
        }
        
        // ========== 兼容性字段 ==========
        // 兼容处理：将 auxQuantity 映射到 auxUnitQty（用于兼容旧前端）
        if (response.getAuxQuantity() != null) {
            response.setAuxUnitQty(response.getAuxQuantity());
        }
        // 兼容处理：保留 packageType 和 packageQty 字段（但前端不再使用）
        response.setPackageType(item.getPackageType());
        response.setPackageQty(item.getPackageQty());
        // 危废条目编号
        response.setHazardousWasteItemId(item.getHazardousWasteItemId());
        // 危废类别编码
        response.setWasteCategory(item.getWasteCategory());
        if (item.getCreateTime() != null) {
            response.setCreatedAt(item.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (item.getUpdateTime() != null) {
            response.setUpdateTime(item.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return response;
    }

    /**
     * 转换日期字符串为LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        try {
            // 尝试解析多种日期格式
            if (dateStr.length() == 10) {
                // yyyy-MM-dd格式
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        .atStartOfDay();
            } else if (dateStr.length() == 19) {
                // yyyy-MM-dd HH:mm:ss格式
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                return LocalDateTime.parse(dateStr);
            }
        } catch (Exception e) {
            log.warn("日期格式解析失败：{}", dateStr, e);
            return null;
        }
    }

    /**
     * 发送收运通知单审核通知
     * 根据操作类型发送给相应权限的人员：
     * - 提交审核/撤回：发送给OA审批人员
     * - 审核通过/审核驳回：发送给有收运通知页面权限的人员
     * 
     * @param notice 收运通知单
     * @param senderId 发送人ID
     * @param action 操作类型（提交审核/撤回/审核通过/审核驳回）
     */
    private void sendPickupNoticeAuditNotification(PickupNotice notice, Integer senderId, String action) {
        try {
            log.info("开始发送收运通知单审核通知：noticeId={}, noticeCode={}, senderId={}, action={}",
                    notice.getNoticeId(), notice.getNoticeCode(), senderId, action);

            // 构建业务标题
            String businessTitle = String.format("收运通知单【%s】", notice.getNoticeCode());

            // 根据操作类型选择不同的通知方法
            if ("提交审核".equals(action)) {
                // 提交审批通知 - 发送给OA审批人员
                messageNotificationService.sendApprovalSubmitNotification(
                        "PICKUP_NOTICE_SUBMIT",
                        notice.getNoticeId(),
                        businessTitle,
                        senderId
                );
                log.info("收运通知单提交审核通知已发送：noticeId={}", notice.getNoticeId());
            } else if ("撤回".equals(action)) {
                // 撤回通知 - 发送给OA审批人员
                messageNotificationService.sendApprovalRevokeNotification(
                        "PICKUP_NOTICE_REVOKE",
                        notice.getNoticeId(),
                        businessTitle,
                        senderId
                );
                log.info("收运通知单撤回通知已发送：noticeId={}", notice.getNoticeId());
            } else if ("审核通过".equals(action) || "审核驳回".equals(action)) {
                // 审核结果通知 - 发送给有收运通知页面权限的人员
                messageNotificationService.sendAuditResultNotification(
                        "PICKUP_NOTICE_AUDIT_RESULT",
                        notice.getNoticeId(),
                        businessTitle,
                        action,
                        senderId
                );
                log.info("收运通知单审核结果通知已发送：noticeId={}, action={}", notice.getNoticeId(), action);
            } else {
                log.warn("未知的操作类型，无法发送通知：action={}", action);
            }
        } catch (Exception e) {
            log.error("发送收运通知单审核通知异常：noticeId={}, action={}", 
                    notice.getNoticeId(), action, e);
        }
    }

    @Override
    public List<WasteCategoryLimitResponse> getWasteCategoryLimits(List<String> wasteCodes) {
        if (CollectionUtils.isEmpty(wasteCodes)) {
            return new ArrayList<>();
        }

        // 去除空白并去重
        List<String> normalizedCodes = wasteCodes.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(normalizedCodes)) {
            return new ArrayList<>();
        }

        // 查询危废代码对应的类别
        List<HazardousWasteItem> wasteItems = hazardousWasteItemMapper.selectList(
                new LambdaQueryWrapper<HazardousWasteItem>()
                        .in(HazardousWasteItem::getWasteCode, normalizedCodes)
        );
        if (CollectionUtils.isEmpty(wasteItems)) {
            return new ArrayList<>();
        }

        // 分类聚合：categoryId -> codes
        Map<Integer, List<String>> categoryCodeMap = new HashMap<>();
        for (HazardousWasteItem item : wasteItems) {
            if (item.getCategoryId() == null) {
                continue;
            }
            categoryCodeMap.computeIfAbsent(item.getCategoryId(), k -> new ArrayList<>())
                    .add(item.getWasteCode());
        }
        if (categoryCodeMap.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> categoryIds = categoryCodeMap.keySet();
        List<HazardousWasteCategory> categories = hazardousWasteCategoryMapper.selectBatchIds(categoryIds);
        if (CollectionUtils.isEmpty(categories)) {
            return new ArrayList<>();
        }

        List<WasteCategoryLimitResponse> result = new ArrayList<>();
        for (HazardousWasteCategory category : categories) {
            WasteCategoryLimitResponse resp = new WasteCategoryLimitResponse();
            resp.setWasteCodes(categoryCodeMap.getOrDefault(category.getCategoryId(), new ArrayList<>()));
            resp.setWasteCategory(category.getWasteCategory());
            resp.setWasteCategoryName(category.getWasteCategoryName());
            resp.setLimitAmount(category.getLimitAmount());
            resp.setLimitStartTime(category.getLimitStartTime());
            resp.setLimitEndTime(category.getLimitEndTime());
            // 计算限额时间段内的已入库量
            BigDecimal inboundAmount = pickupNoticeMapper.selectInboundAmountByCategoryAndTimeRange(
                    category.getCategoryId(),
                    category.getLimitStartTime(),
                    category.getLimitEndTime()
            );
            resp.setInboundAmount(inboundAmount != null ? inboundAmount : BigDecimal.ZERO);
            result.add(resp);
        }

        // 按废物类别编码排序，便于前端展示稳定
        result.sort((a, b) -> {
            if (a.getWasteCategory() == null && b.getWasteCategory() == null) {
                return 0;
            }
            if (a.getWasteCategory() == null) {
                return 1;
            }
            if (b.getWasteCategory() == null) {
                return -1;
            }
            return a.getWasteCategory().compareTo(b.getWasteCategory());
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSubmitPickupNotices(List<String> noticeCodes) {
        if (CollectionUtils.isEmpty(noticeCodes)) {
            return;
        }
        Integer currentUserId = getCurrentUserId();

        // 批量查询所有要提交的收运通知单
        List<PickupNotice> noticesToSubmit = new ArrayList<>();
        for (String noticeCode : noticeCodes) {
            PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
            if (notice != null) {
                noticesToSubmit.add(notice);
            }
        }

        if (noticesToSubmit.size() != noticeCodes.size()) {
            Set<String> foundCodes = noticesToSubmit.stream()
                    .map(PickupNotice::getNoticeCode)
                    .collect(Collectors.toSet());
            List<String> notFoundCodes = noticeCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .collect(Collectors.toList());
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(),
                    "以下收运通知单不存在: " + notFoundCodes);
        }

        // 检查是否有不符合条件的记录
        List<String> invalidCodes = new ArrayList<>();
        for (PickupNotice notice : noticesToSubmit) {
            if (Boolean.TRUE.equals(notice.getLocked())) {
                invalidCodes.add(notice.getNoticeCode() + "(已锁定)");
            } else if (!"待审核".equals(notice.getStatus()) && !"已驳回".equals(notice.getStatus())) {
                invalidCodes.add(notice.getNoticeCode() + "(" + notice.getStatus() + ")");
            }
        }
        if (!invalidCodes.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "以下收运通知单不符合提交条件: " + invalidCodes);
        }

        // 批量更新状态为审核中
        LocalDateTime now = LocalDateTime.now();

        // 获取当前用户信息（用于OA审批记录）
        String currentUserName = "未知用户";
        try {
            Employee currentEmployee = employeeMapper.selectById(currentUserId);
            if (currentEmployee != null && StrUtil.isNotBlank(currentEmployee.getEmployeeName())) {
                currentUserName = currentEmployee.getEmployeeName();
            }
        } catch (Exception e) {
            log.warn("获取当前用户姓名失败：{}", currentUserId, e);
        }

        for (PickupNotice notice : noticesToSubmit) {
            String oldStatus = notice.getStatus();
            notice.setStatus("审核中");
            notice.setSubmittedAt(now);
            notice.setUpdateTime(now);
            pickupNoticeMapper.updateById(notice);

            // 处理OA审批记录
            try {
                // 查询是否已有该收运通知单的OA审批记录（待审核或已驳回状态）
                OaApprovalRecord existingRecord = oaApprovalRecordService.findLatestBySource(
                        PICKUP_NOTICE_BUSINESS_TYPE, notice.getNoticeId());

                if (existingRecord == null) {
                    // 不存在OA审批记录，新增一条
                    OaApprovalRecord newRecord = new OaApprovalRecord();
                    newRecord.setApprovalNo(generateApprovalNo());
                    newRecord.setSourceTable(PICKUP_NOTICE_BUSINESS_TYPE);
                    newRecord.setSourceTableName("收运通知单表");
                    newRecord.setSourceId(notice.getNoticeId());
                    newRecord.setSourceNo(notice.getNoticeCode());
                    newRecord.setTitle("收运通知单审核：" + notice.getNoticeCode());
                    newRecord.setSubmitterId(currentUserId);
                    newRecord.setSubmitterName(currentUserName);
                    newRecord.setApprovalStatus("待审核");
                    newRecord.setApprovalCount(1);
                    newRecord.setSubmitTime(now);
                    newRecord.setDeleted(0);

                    // 使用Mapper直接保存
                    oaApprovalRecordMapper.insert(newRecord);

                    log.info("创建收运通知单OA审批记录成功：noticeCode={}, approvalNo={}",
                            notice.getNoticeCode(), newRecord.getApprovalNo());
                } else {
                    // 已存在OA审批记录，审核次数+1，状态改为待审核
                    existingRecord.setApprovalStatus("待审核");
                    existingRecord.setApprovalCount(existingRecord.getApprovalCount() + 1);
                    existingRecord.setSubmitTime(now);
                    existingRecord.setApproverId(null);
                    existingRecord.setApproverName(null);
                    existingRecord.setApprovalTime(null);

                    // 使用Mapper直接更新
                    oaApprovalRecordMapper.updateById(existingRecord);

                    log.info("更新收运通知单OA审批记录成功：noticeCode={}, approvalCount={}",
                            notice.getNoticeCode(), existingRecord.getApprovalCount());
                }
            } catch (Exception e) {
                log.error("处理OA审批记录失败：noticeCode={}", notice.getNoticeCode(), e);
                // OA记录处理失败不影响主流程
            }

            log.info("批量提交收运通知单审核成功：noticeCode={}, operator={}", notice.getNoticeCode(), currentUserId);

            // 发送提交审核通知
            try {
                sendPickupNoticeAuditNotification(notice, currentUserId, "提交审核");
            } catch (Exception e) {
                log.error("发送收运通知单提交审核通知失败：noticeCode={}", notice.getNoticeCode(), e);
            }
        }

        // 记录批量操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "批量提交审核",
                    "批量提交收运通知单审核：noticeCodes=" + noticeCodes,
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录批量提交审核操作日志失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRevokePickupNotices(List<String> noticeCodes) {
        if (CollectionUtils.isEmpty(noticeCodes)) {
            return;
        }
        Integer currentUserId = getCurrentUserId();

        // 批量查询所有要撤回的收运通知单
        List<PickupNotice> noticesToRevoke = new ArrayList<>();
        for (String noticeCode : noticeCodes) {
            PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
            if (notice != null) {
                noticesToRevoke.add(notice);
            }
        }

        if (noticesToRevoke.size() != noticeCodes.size()) {
            Set<String> foundCodes = noticesToRevoke.stream()
                    .map(PickupNotice::getNoticeCode)
                    .collect(Collectors.toSet());
            List<String> notFoundCodes = noticeCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .collect(Collectors.toList());
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(),
                    "以下收运通知单不存在: " + notFoundCodes);
        }

        // 检查是否有不符合条件的记录
        List<String> invalidCodes = new ArrayList<>();
        for (PickupNotice notice : noticesToRevoke) {
            if (Boolean.TRUE.equals(notice.getLocked())) {
                invalidCodes.add(notice.getNoticeCode() + "(已锁定)");
            } else if (!"审核中".equals(notice.getStatus())) {
                invalidCodes.add(notice.getNoticeCode() + "(" + notice.getStatus() + ")");
            }
        }
        if (!invalidCodes.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "以下收运通知单不符合撤回条件: " + invalidCodes);
        }

        // 批量更新状态为待审核，同时更新OA审核记录
        LocalDateTime now = LocalDateTime.now();
        for (PickupNotice notice : noticesToRevoke) {
            String oldStatus = notice.getStatus();
            notice.setStatus("待审核");
            notice.setSubmittedAt(null);
            notice.setUpdateTime(now);
            pickupNoticeMapper.updateById(notice);

            // 更新OA审核记录：审核次数减1（最低为0），状态改为已撤回
            try {
                OaApprovalRecord existingRecord = oaApprovalRecordService.findPendingBySource(
                        PICKUP_NOTICE_BUSINESS_TYPE, notice.getNoticeId());
                if (existingRecord != null) {
                    Integer currentApprovalCount = existingRecord.getApprovalCount() == null ? 0 : existingRecord.getApprovalCount();
                    existingRecord.setApprovalCount(Math.max(currentApprovalCount - 1, 0));
                    existingRecord.setApprovalStatus("已撤回");
                    existingRecord.setApprovalTime(now);
                    oaApprovalRecordMapper.updateById(existingRecord);
                    log.info("批量撤回更新OA审核记录：noticeCode={}, approvalRecordId={}, newApprovalCount={}",
                            notice.getNoticeCode(), existingRecord.getApprovalRecordId(), existingRecord.getApprovalCount());
                }
            } catch (Exception e) {
                log.error("更新OA审核记录失败：noticeCode={}", notice.getNoticeCode(), e);
            }

            log.info("批量撤回收运通知单成功：noticeCode={}, operator={}", notice.getNoticeCode(), currentUserId);

            // 发送撤回通知给创建人
            try {
                Integer creatorId = notice.getCreatorId();
                if (creatorId != null) {
                    String customerName = "未知客户";
                    if (notice.getCustomerId() != null) {
                        Customer customer = customerMapper.selectById(notice.getCustomerId());
                        if (customer != null && customer.getEnterpriseName() != null) {
                            customerName = customer.getEnterpriseName();
                        }
                    }
                    // 使用基于权限的通知方法
                    messageNotificationService.sendApprovalRevokeNotification(
                            "PICKUP_NOTICE_REVOKE",
                            notice.getNoticeId(),
                            String.format("收运通知单【%s】", notice.getNoticeCode()),
                            currentUserId
                    );
                }
            } catch (Exception e) {
                log.error("发送收运通知单撤回通知失败：noticeCode={}", notice.getNoticeCode(), e);
            }
        }

        // 记录批量操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "批量撤回",
                    "批量撤回收运通知单：noticeCodes=" + noticeCodes,
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录批量撤回操作日志失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePickupNotices(List<String> noticeCodes) {
        if (CollectionUtils.isEmpty(noticeCodes)) {
            return;
        }
        Integer currentUserId = getCurrentUserId();

        // 批量查询所有要删除的收运通知单
        List<PickupNotice> noticesToDelete = new ArrayList<>();
        for (String noticeCode : noticeCodes) {
            PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
            if (notice != null) {
                noticesToDelete.add(notice);
            }
        }

        if (noticesToDelete.size() != noticeCodes.size()) {
            Set<String> foundCodes = noticesToDelete.stream()
                    .map(PickupNotice::getNoticeCode)
                    .collect(Collectors.toSet());
            List<String> notFoundCodes = noticeCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .collect(Collectors.toList());
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(),
                    "以下收运通知单不存在: " + notFoundCodes);
        }

        // 检查是否有不符合条件的记录
        List<String> invalidCodes = new ArrayList<>();
        for (PickupNotice notice : noticesToDelete) {
            if (Boolean.TRUE.equals(notice.getLocked())) {
                invalidCodes.add(notice.getNoticeCode() + "(已锁定)");
            } else if (!"待审核".equals(notice.getStatus()) && !"已驳回".equals(notice.getStatus())) {
                invalidCodes.add(notice.getNoticeCode() + "(" + notice.getStatus() + ")");
            }
        }
        if (!invalidCodes.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "以下收运通知单不符合删除条件: " + invalidCodes);
        }

        // 批量删除收运通知单
        for (PickupNotice notice : noticesToDelete) {
            try {
                deletePickupNotice(notice.getNoticeCode());
            } catch (Exception e) {
                log.error("删除收运通知单失败：noticeCode={}", notice.getNoticeCode(), e);
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                        "删除收运通知单失败：" + notice.getNoticeCode() + "，" + e.getMessage());
            }
        }

        // 记录批量操作日志
        try {
            logRecordService.recordOperationLog("收运通知单管理", "批量删除",
                    "批量删除收运通知单：noticeCodes=" + noticeCodes,
                    currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录批量删除操作日志失败", e);
        }
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "用户未登录");
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
     * @param pageCode   页面权限编码
     * @return 员工页面权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        try {
            // 查询页面级权限定义
            Permission permission = permissionMapper.selectOne(
                    new LambdaQueryWrapper<Permission>()
                            .eq(Permission::getPermissionCode, pageCode)
                            .eq(Permission::getPermissionTypeId, 2) // 2 = 页面级权限
            );
            if (permission == null) {
                return null;
            }

            // 查询员工在该页面下的权限配置（含 operateScope / canEdit 等）
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

}


