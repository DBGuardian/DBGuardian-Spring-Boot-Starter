package com.erp.service.transport.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.transport.dto.DispatchOverLimitItemResponse;
import com.erp.controller.transport.dto.DispatchValidateResponse;
import com.erp.controller.transport.dto.TransportDispatchDetailRequest;
import com.erp.controller.transport.dto.TransportDispatchDetailResponse;
import com.erp.controller.transport.dto.TransportDispatchListResponse;
import com.erp.controller.transport.dto.TransportDispatchPageRequest;
import com.erp.controller.transport.dto.TransportDispatchPageResponse;
import com.erp.controller.transport.dto.TransportStat;
import com.erp.entity.common.File;
import com.erp.entity.production.PickupNotice;
import com.erp.entity.production.PickupNoticeItem;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.entity.transport.DispatchOrder;
import com.erp.entity.transport.DispatchOrderNotice;
import com.erp.mapper.common.FileMapper;
import com.erp.mapper.production.PickupNoticeMapper;
import com.erp.mapper.production.PickupNoticeItemMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.mapper.transport.DispatchOrderMapper;
import com.erp.mapper.transport.DispatchOrderNoticeMapper;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.service.auth.AuthService;
import com.erp.service.transport.DispatchOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 运输单服务实现
 */
@Slf4j
@Service
public class DispatchOrderServiceImpl implements DispatchOrderService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DISPATCH_CODE_PREFIX = "YSD";
    private static final String MOBILE_PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 验证手机号格式：要么为空，要么为11位手机号
     */
    private void validateMobilePhone(String phone, String fieldName) {
        if (phone != null && !phone.isEmpty() && !phone.matches(MOBILE_PHONE_REGEX)) {
            throw new BusinessException(ResultCodeEnum.PARAM_INVALID.getCode(), fieldName + "格式不正确，应为11位手机号（如13800138000）或留空");
        }
    }

    @Autowired
    private DispatchOrderMapper dispatchOrderMapper;

    @Autowired
    private DispatchOrderNoticeMapper dispatchOrderNoticeMapper;

    @Autowired
    private PickupNoticeMapper pickupNoticeMapper;

    @Autowired
    private PickupNoticeItemMapper pickupNoticeItemMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @Autowired
    private AuthService authService;

    @org.springframework.beans.factory.annotation.Value("${file.storage.local.path:D:/erp}")
    private String localStoragePath;

    private static final String DISPATCH_ORDER_BUSINESS_TYPE = "DISPATCH_ORDER";
    private static final String DISPATCH_ORDER_BUSINESS_MODULE = "运输管理";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransportDispatchDetailResponse createDispatchOrder(TransportDispatchDetailRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        if (StrUtil.isBlank(request.getNoticeCode())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "收运通知单号不能为空");
        }

        // 检查通知单是否存在（使用selectDetailByNoticeCode获取完整信息，包括创建人）
        PickupNotice pickupNotice = pickupNoticeMapper.selectDetailByNoticeCode(request.getNoticeCode());
        if (pickupNotice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        // 页面级权限兜底校验（operateScope/canEdit），避免仅靠前端控制
        // 对齐 docs/系统权限全量扫描实现文档.md：运输管理-车辆安排-页面 的 operateScope/canEdit 定义
        if (!admin) {
            EmployeePermission pagePermission = getEmployeePagePermission(currentUserId, "运输管理:车辆安排:页面");
            if (pagePermission != null) {
                // canEdit=0：只读，不允许“生成运输单”
                if (pagePermission.getCanEdit() != null && pagePermission.getCanEdit() == 0) {
                    throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "当前页面为只读权限，无法生成运输单");
                }

                // operateScope=SELF：仅允许对自己创建的收运通知单生成运输单
                if ("SELF".equalsIgnoreCase(pagePermission.getOperateScope())) {
                    if (!Objects.equals(pickupNotice.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能对自己创建的收运通知单生成运输单");
                    }
                }
            }
        }

        // 一对一校验（使用SELECT FOR UPDATE确保并发安全）
        // 通过锁定已存在的运输单记录，确保同一收运通知单不会被并发创建多个运输单
        DispatchOrder existingDispatch = dispatchOrderMapper.selectByNoticeCodeForUpdate(request.getNoticeCode());
        if (existingDispatch != null) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "该收运通知单已生成运输单");
        }

        // 验证手机号格式：要么为空，要么为11位手机号
        validateMobilePhone(request.getCarrierPhone(), "承运单位电话");
        validateMobilePhone(request.getDriverPhone(), "驾驶员电话");
        validateMobilePhone(request.getEscortPhone(), "押运员电话");

        // 生成运输单号
        String dispatchCode = generateDispatchCode();

        DispatchOrder entity = buildEntityFromRequest(request, dispatchCode);
        entity.setNoticeCode(request.getNoticeCode());
        entity.setDispatcherId(currentUserId);
        entity.setStatus(StrUtil.blankToDefault(request.getStatus(), "待运输"));
        entity.setLocked(false);
        entity.setCreateTime(LocalDateTime.now());

        dispatchOrderMapper.insert(entity);

        saveOrUpdateNotice(request, dispatchCode, true);

        // 更新收运通知单状态为"已派单"
        // 允许从"待调度"或"审核中"状态更新为"已派单"（根据业务规则，创建运输单即表示已派单）
        if (pickupNotice != null) {
            String currentStatus = pickupNotice.getStatus();
            // 允许从"待调度"或"审核中"状态更新为"已派单"
            if ("待调度".equals(currentStatus) || "审核中".equals(currentStatus)) {
                pickupNotice.setStatus("已派单");
                int rows = pickupNoticeMapper.updateById(pickupNotice);
                if (rows == 0) {
                    log.warn("更新收运通知单状态失败（乐观锁冲突），noticeCode={}", request.getNoticeCode());
                }
                log.info("收运通知单状态已从 {} 更新为已派单: {}", currentStatus, request.getNoticeCode());
            } else if (!"已派单".equals(currentStatus) && !"已完成".equals(currentStatus) && !"已取消".equals(currentStatus)) {
                // 如果状态不是已派单、已完成、已取消，记录警告日志但不阻止创建运输单
                log.warn("收运通知单状态为 {}，创建运输单时未更新状态: {}", currentStatus, request.getNoticeCode());
            }
        }

        TransportDispatchDetailResponse response = buildResponse(entity, pickupNotice, dispatchOrderNoticeMapper.selectByDispatchCode(dispatchCode), Collections.emptyList(), false);

        // 发送创建通知（使用基于权限的通知方法）
        try {
            String customerName = pickupNotice != null && pickupNotice.getCompanyName() != null 
                    ? pickupNotice.getCompanyName() : "未知客户";
            
            // 发送通知给运输单创建人（调度员）
            String dispatcherContent = String.format("您已成功创建运输单【%s】，收运通知单号：%s，客户：%s，请及时安排运输。", 
                    dispatchCode, request.getNoticeCode(), customerName);
            // 使用基于权限的通知方法
            messageNotificationService.sendBusinessNotificationByPermission(
                    "DISPATCH_ORDER_CREATE",
                    entity.getDispatchId(),
                    String.format("运输单【%s】", dispatchCode),
                    dispatcherContent,
                    currentUserId,
                    "运输"
            );
            log.info("运输单创建通知已发送：dispatchCode={}", dispatchCode);
            
            // 发送通知给收运通知单创建人（业务员），如果创建人不同
            if (pickupNotice != null && pickupNotice.getCreatorId() != null 
                    && !pickupNotice.getCreatorId().equals(currentUserId)) {
                String businessContent = String.format("您创建的收运通知单【%s】已生成运输单【%s】，客户：%s，请关注运输进度。", 
                        request.getNoticeCode(), dispatchCode, customerName);
                messageNotificationService.sendBusinessNotificationByPermission(
                        "DISPATCH_ORDER_CREATE",
                        entity.getDispatchId(),
                        String.format("运输单【%s】", dispatchCode),
                        businessContent,
                        currentUserId,
                        "运输"
                );
                log.info("运输单创建通知已发送（业务员）：dispatchCode={}", dispatchCode);
            }
        } catch (Exception e) {
            log.error("发送运输单创建通知失败：dispatchCode={}", dispatchCode, e);
        }

        // 记录数据变更日志
        try {
            TransportDispatchDetailResponse newData = buildResponse(entity, pickupNotice,
                    dispatchOrderNoticeMapper.selectByDispatchCode(dispatchCode), Collections.emptyList(), false);
            logRecordService.recordDataChangeLog("运输管理", "DISPATCH_ORDER",
                    String.valueOf(entity.getDispatchId()),
                    "新增",
                    "创建运输单：运输单号=" + dispatchCode,
                    null, newData, currentUserId, null, true, null);
        } catch (Exception logEx) {
            log.warn("记录运输单创建数据变更日志失败，dispatchCode={}", dispatchCode, logEx);
        }

        return response;
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
     * 获取员工在指定页面下的权限配置（含 viewScope / operateScope / canEdit）
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        if (employeeId == null || StrUtil.isBlank(pageCode)) {
            return null;
        }
        try {
            Permission permission = permissionMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Permission>()
                            // 2 = 页面级权限（与全局权限体系保持一致）
                            .eq(Permission::getPermissionTypeId, 2)
                            .eq(Permission::getPermissionCode, pageCode)
            );
            if (permission == null || permission.getPermissionId() == null) {
                return null;
            }
            return employeePermissionMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmployeePermission>()
                            .eq(EmployeePermission::getEmployeeId, employeeId)
                            .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
            );
        } catch (Exception e) {
            log.warn("查询员工页面权限失败，employeeId={}, pageCode={}", employeeId, pageCode, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransportDispatchDetailResponse updateDispatchOrder(TransportDispatchDetailRequest request) {
        if (StrUtil.isBlank(request.getDispatchCode())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "运输单号不能为空");
        }
        DispatchOrder exist = dispatchOrderMapper.selectByDispatchCode(request.getDispatchCode());
        if (exist == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "运输单不存在");
        }
        if (Boolean.TRUE.equals(exist.getLocked()) || "已完成".equals(exist.getStatus()) || "已取消".equals(exist.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "单据已锁定或已完成，无法修改");
        }

        // 验证手机号格式：要么为空，要么为11位手机号
        validateMobilePhone(request.getCarrierPhone(), "承运单位电话");
        validateMobilePhone(request.getDriverPhone(), "驾驶员电话");
        validateMobilePhone(request.getEscortPhone(), "押运员电话");

        // 保存旧数据用于日志记录
        TransportDispatchDetailResponse oldDetail = null;
        try {
            PickupNotice oldPickupNotice = pickupNoticeMapper.selectDetailByNoticeCode(exist.getNoticeCode());
            DispatchOrderNotice oldNotice = dispatchOrderNoticeMapper.selectByDispatchCode(exist.getDispatchCode());
            oldDetail = buildResponse(exist, oldPickupNotice, oldNotice, Collections.emptyList(), false);
        } catch (Exception e) {
            log.warn("获取运输单旧数据失败，将跳过数据变更日志记录", e);
        }

        applyRequestToEntity(request, exist);
        exist.setUpdateTime(LocalDateTime.now());
        int rows = dispatchOrderMapper.updateById(exist);
        if (rows == 0) {
            throw new BusinessException("更新运输单失败：记录已被其他用户修改");
        }

        saveOrUpdateNotice(request, exist.getDispatchCode(), false);

        PickupNotice pickupNotice = pickupNoticeMapper.selectDetailByNoticeCode(exist.getNoticeCode());
        DispatchOrderNotice notice = dispatchOrderNoticeMapper.selectByDispatchCode(exist.getDispatchCode());
        TransportDispatchDetailResponse newDetail = buildResponse(exist, pickupNotice, notice, Collections.emptyList(), false);

        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                Integer currentUserId = SecurityUtil.getCurrentUserId();
                logRecordService.recordDataChangeLog("运输单管理", "DISPATCH_ORDER",
                        String.valueOf(exist.getDispatchId()),
                        "更新",
                        "更新运输单：运输单号=" + request.getDispatchCode(),
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.error("记录运输单更新数据变更日志失败", e);
            }
        }

        // 发送更新通知（使用基于权限的通知方法）
        try {
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            Integer creatorId = exist.getDispatcherId();
            if (creatorId != null && !creatorId.equals(currentUserId)) {
                String customerName = pickupNotice != null && pickupNotice.getCompanyName() != null 
                        ? pickupNotice.getCompanyName() : "未知客户";
                String content = String.format("运输单【%s】已被修改，收运通知单号：%s，客户：%s，请查看详情。", 
                        request.getDispatchCode(), exist.getNoticeCode(), customerName);
                // 使用基于权限的通知方法
                messageNotificationService.sendBusinessNotificationByPermission(
                        "TRANSPORT_DISPATCH_UPDATE",
                        exist.getDispatchId(),
                        String.format("运输单【%s】", request.getDispatchCode()),
                        content,
                        currentUserId,
                        "运输"
                );
                log.info("运输单修改通知已发送：dispatchCode={}", request.getDispatchCode());
            }
        } catch (Exception e) {
            log.error("发送运输单修改通知失败：dispatchCode={}", request.getDispatchCode(), e);
        }

        return newDetail;
    }

    @Override
    public TransportDispatchDetailResponse getDispatchDetail(String dispatchCode, String noticeCode) {
        DispatchOrder order = null;
        if (StrUtil.isNotBlank(dispatchCode)) {
            order = dispatchOrderMapper.selectByDispatchCode(dispatchCode);
        } else if (StrUtil.isNotBlank(noticeCode)) {
            order = dispatchOrderMapper.selectByNoticeCode(noticeCode);
        }
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "运输单不存在");
        }
        DispatchOrderNotice notice = dispatchOrderNoticeMapper.selectByDispatchCode(order.getDispatchCode());
        PickupNotice pickupNotice = pickupNoticeMapper.selectDetailByNoticeCode(order.getNoticeCode());
        DispatchValidateResponse validate = validateDispatchOrder(order.getNoticeCode(), order.getDispatchCode());
        return buildResponse(order, pickupNotice, notice,
                validate.getOverLimitItems(), Objects.equals(Boolean.TRUE, validate.getOverLimit()));
    }

    @Override
    public DispatchValidateResponse validateDispatchOrder(String noticeCode, String dispatchCode) {
        String targetNoticeCode = noticeCode;
        if (StrUtil.isBlank(targetNoticeCode) && StrUtil.isNotBlank(dispatchCode)) {
            DispatchOrder order = dispatchOrderMapper.selectByDispatchCode(dispatchCode);
            if (order != null) {
                targetNoticeCode = order.getNoticeCode();
            }
        }
        if (StrUtil.isBlank(targetNoticeCode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "收运通知单号不能为空");
        }
        PickupNotice notice = pickupNoticeMapper.selectDetailByNoticeCode(targetNoticeCode);
        if (notice == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "收运通知单不存在");
        }

        boolean contractMissing = StrUtil.isBlank(notice.getContractCode()) || Boolean.TRUE.equals(notice.getContractPending());

        DispatchValidateResponse resp = new DispatchValidateResponse();
        resp.setContractMissing(contractMissing);
        resp.setOverLimit(false);
        resp.setOverLimitItems(new ArrayList<>());
        return resp;
    }

    private DispatchOrder buildEntityFromRequest(TransportDispatchDetailRequest request, String dispatchCode) {
        DispatchOrder entity = new DispatchOrder();
        entity.setDispatchCode(dispatchCode);
        applyRequestToEntity(request, entity);
        return entity;
    }

    private void applyRequestToEntity(TransportDispatchDetailRequest request, DispatchOrder entity) {
        entity.setCarrierName(request.getCarrierName());
        entity.setOperationLicenseNo(request.getOperationLicenseNo());
        entity.setCarrierAddress(request.getCarrierAddress());
        entity.setCarrierPhone(request.getCarrierPhone());
        entity.setDriverName(request.getDriverName());
        entity.setDriverPhone(request.getDriverPhone());
        entity.setTransportTool(request.getTransportTool());
        entity.setVehicleId(request.getVehicleId());
        entity.setPlateNo(request.getPlateNo());
        entity.setStartPoint(request.getStartPoint());
        entity.setDispatcherRemark(request.getDispatcherRemark());
        entity.setStatus(StrUtil.blankToDefault(request.getStatus(), entity.getStatus()));
        entity.setPlanQuantityTon(request.getPlanQuantityTon());
        entity.setDispatchAt(parseDateTime(request.getDispatchAt()));
        entity.setDepartAt(parseDateTime(request.getDepartAt()));
        // 非表字段放在 notice 表中
    }

    private void saveOrUpdateNotice(TransportDispatchDetailRequest request, String dispatchCode, boolean insert) {
        dispatchOrderNoticeMapper.deleteByDispatchCode(dispatchCode);
        DispatchOrderNotice notice = new DispatchOrderNotice();
        notice.setDispatchCode(dispatchCode);
        notice.setNoticeCode(request.getNoticeCode());
        notice.setEndPoint(request.getEndPoint());
        notice.setArriveAt(parseDateTime(request.getArriveAt()));
        notice.setEstimatedWeight(request.getPlanQuantityTon());
        notice.setTransportDistance(request.getTransportDistance());
        notice.setCreateTime(LocalDateTime.now());
        if (!insert) {
            notice.setUpdateTime(LocalDateTime.now());
        }
        dispatchOrderNoticeMapper.insert(notice);
    }

    private String generateDispatchCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = DISPATCH_CODE_PREFIX + "-" + datePart + "-";
        String maxCode = dispatchOrderMapper.selectMaxDispatchCodeByPrefix(prefix);
        int nextSeq = 1;
        if (StrUtil.isNotBlank(maxCode) && maxCode.length() > prefix.length()) {
            String seq = maxCode.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seq) + 1;
            } catch (NumberFormatException e) {
                log.warn("解析运输单号序列失败: {}", maxCode, e);
            }
        }
        String code = prefix + String.format("%04d", nextSeq);
        int retry = 0;
        while (dispatchOrderMapper.countByDispatchCode(code) > 0 && retry < 20) {
            nextSeq++;
            code = prefix + String.format("%04d", nextSeq);
            retry++;
        }
        if (retry >= 20) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成运输单号失败，请重试");
        }
        return code;
    }

    private LocalDateTime parseDateTime(String val) {
        if (StrUtil.isBlank(val)) {
            return null;
        }
        try {
            return LocalDateTime.parse(val, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", val, e);
            return null;
        }
    }

    private String formatDateTime(LocalDateTime val) {
        return val == null ? null : val.format(DATE_TIME_FORMATTER);
    }

    private TransportDispatchDetailResponse buildResponse(DispatchOrder order,
                                                          PickupNotice pickupNotice,
                                                          DispatchOrderNotice notice,
                                                          List<DispatchOverLimitItemResponse> overLimitItems,
                                                          boolean overLimit) {
        TransportDispatchDetailResponse resp = new TransportDispatchDetailResponse();
        resp.setDispatchId(order.getDispatchId());
        resp.setDispatchCode(order.getDispatchCode());
        resp.setNoticeCode(order.getNoticeCode());
        resp.setCarrierName(order.getCarrierName());
        resp.setOperationLicenseNo(order.getOperationLicenseNo());
        resp.setCarrierAddress(order.getCarrierAddress());
        resp.setCarrierPhone(order.getCarrierPhone());
        resp.setDriverName(order.getDriverName());
        resp.setDriverPhone(order.getDriverPhone());
        // 押运员字段已删除，设置为null
        resp.setEscortName(null);
        resp.setEscortPhone(null);
        resp.setTransportTool(order.getTransportTool());
        resp.setVehicleId(order.getVehicleId());
        resp.setPlateNo(order.getPlateNo());
        resp.setStartPoint(order.getStartPoint());
        resp.setDispatchAt(formatDateTime(order.getDispatchAt()));
        resp.setDepartAt(formatDateTime(order.getDepartAt()));
        if (notice != null) {
            resp.setEndPoint(notice.getEndPoint());
            resp.setArriveAt(formatDateTime(notice.getArriveAt()));
            resp.setPlanQuantityTon(notice.getEstimatedWeight());
            resp.setTransportDistance(notice.getTransportDistance());
        } else {
            resp.setPlanQuantityTon(order.getPlanQuantityTon());
        }
        resp.setDispatcherRemark(order.getDispatcherRemark());
        resp.setStatus(order.getStatus());
        resp.setLocked(order.getLocked());
        resp.setLockReason(order.getLockReason());
        resp.setLockTime(formatDateTime(order.getLockTime()));
        resp.setCreatedAt(formatDateTime(order.getCreateTime()));
        resp.setUpdatedAt(formatDateTime(order.getUpdateTime()));
        resp.setOverLimit(overLimit);
        resp.setOverLimitItems(overLimitItems);

        if (pickupNotice != null) {
            resp.setContractCode(pickupNotice.getContractCode());
            resp.setContractPending(pickupNotice.getContractPending());
            resp.setContractMissing(StrUtil.isBlank(pickupNotice.getContractCode()) || Boolean.TRUE.equals(pickupNotice.getContractPending()));
        }

        // 调度员姓名
        if (order.getDispatcherId() != null) {
            try {
                resp.setDispatcherName(
                        employeeMapper.selectById(order.getDispatcherId()) != null
                                ? employeeMapper.selectById(order.getDispatcherId()).getEmployeeName()
                                : null
                );
            } catch (Exception ignore) {
                resp.setDispatcherName(null);
            }
        }
        // 调度员编码
        resp.setDispatcherId(order.getDispatcherId());
        // 锁定人编码
        resp.setLockUserId(order.getLockUserId());

        // 查询危废明细
        if (pickupNotice != null && StrUtil.isNotBlank(pickupNotice.getNoticeCode())) {
            try {
                List<PickupNoticeItem> wasteItems = pickupNoticeItemMapper.selectByNoticeCode(pickupNotice.getNoticeCode());
                if (wasteItems != null && !wasteItems.isEmpty()) {
                    List<TransportDispatchDetailResponse.WasteItemDetail> wasteItemDetails = wasteItems.stream()
                            .map(item -> {
                                TransportDispatchDetailResponse.WasteItemDetail detail = new TransportDispatchDetailResponse.WasteItemDetail();
                                detail.setWasteName(item.getWasteName());
                                detail.setWasteCode(item.getWasteCode());
                                detail.setHazardFeature(item.getHazardFeature());
                                detail.setForm(item.getForm());
                                detail.setHazardousComponentName(item.getHazardousComponentName());
                                detail.setPackageType(item.getPackageType());
                                detail.setPackageQty(item.getPackageQty());
                                detail.setPlannedQtyTon(item.getPlannedQtyTon());
                                // 继承收运通知单的辅助核算相关字段
                                detail.setMeasureUnit(item.getMeasureUnit());
                                detail.setEnableAuxiliaryAccounting(item.getEnableAuxiliaryAccounting());
                                detail.setAuxUnit(item.getAuxUnit());
                                detail.setAuxPerBase(item.getAuxPerBase());
                                detail.setAuxQuantity(item.getAuxQuantity());
                                // 设置危废条目编号和危废类别
                                detail.setHazardousWasteItemId(item.getHazardousWasteItemId());
                                detail.setWasteCategory(item.getWasteCategory());
                                return detail;
                            })
                            .collect(Collectors.toList());
                    resp.setWasteItems(wasteItemDetails);
                } else {
                    resp.setWasteItems(new ArrayList<>());
                }
            } catch (Exception e) {
                log.warn("查询危废明细失败：noticeCode={}", pickupNotice.getNoticeCode(), e);
                resp.setWasteItems(new ArrayList<>());
            }

            // 查询常用单位二维码
            try {
                if (StrUtil.isNotBlank(pickupNotice.getQrCode())) {
                    Integer qrcodeFileId = null;
                    try {
                        qrcodeFileId = Integer.parseInt(pickupNotice.getQrCode());
                    } catch (NumberFormatException e) {
                        // 如果是JSON数组，尝试解析
                        try {
                            List<Integer> qrcodeFileIds = JSONUtil.toList(pickupNotice.getQrCode(), Integer.class);
                            if (!qrcodeFileIds.isEmpty()) {
                                qrcodeFileId = qrcodeFileIds.get(0);
                            }
                        } catch (Exception ex) {
                            log.warn("解析二维码文件ID失败：qrCode={}", pickupNotice.getQrCode(), ex);
                        }
                    }
                    
                    if (qrcodeFileId != null) {
                        File qrcodeFile = fileMapper.selectById(qrcodeFileId);
                        if (qrcodeFile != null && StrUtil.isNotBlank(qrcodeFile.getLocalPath())) {
                            // 构建完整的文件路径
                            String qrcodeFilePath = localStoragePath + "/" + qrcodeFile.getLocalPath();
                            resp.setQrcodeFilePath(qrcodeFilePath);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("查询二维码文件失败：noticeCode={}", pickupNotice.getNoticeCode(), e);
            }
        } else {
            resp.setWasteItems(new ArrayList<>());
        }

        return resp;
    }

    @Override
    public TransportDispatchListResponse getDispatchOrderList(TransportDispatchPageRequest request) {
        Page<DispatchOrder> page = new Page<>(request.getCurrent(), request.getSize());

        // 转换日期字符串
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (StrUtil.isNotBlank(request.getStartTime())) {
            startTime = parseDateTime(request.getStartTime());
        }
        if (StrUtil.isNotBlank(request.getEndTime())) {
            endTime = parseDateTime(request.getEndTime());
        }

        // 运输执行页面权限编码
        String pageCode = "运输管理:运输执行:页面";

        // 使用 ViewScopeHelper 解析视图范围
        String viewScope = ViewScopeHelper.resolveViewScope(pageCode, request.getViewScope());

        // 获取当前用户ID
        Integer currentUserId = getCurrentUserId();

        // SELF 模式时添加创建人过滤条件，ALL 模式时不限制
        Integer creatorFilter = ViewScopeHelper.isSelfScope(viewScope) ? currentUserId : null;

        IPage<DispatchOrder> dispatchPage = dispatchOrderMapper.selectDispatchOrderPage(
                page,
                request.getDispatchCode(),
                request.getNoticeCode(),
                request.getContractCode(),
                request.getCarrierName(),
                request.getDriverName(),
                request.getPlateNo(),
                request.getStatus(),
                request.getLocked(),
                request.getDispatcherId(),
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder(),
                creatorFilter
        );

        // 转换为响应DTO
        List<TransportDispatchPageResponse> records = dispatchPage.getRecords().stream()
                .map(this::buildPageResponse)
                .collect(Collectors.toList());

        // 计算统计数据
        List<TransportStat> stats = calculateStats();

        TransportDispatchListResponse response = new TransportDispatchListResponse();
        response.setStats(stats);
        response.setRecords(records);
        response.setTotal(dispatchPage.getTotal());
        response.setCurrent(dispatchPage.getCurrent());
        response.setSize(dispatchPage.getSize());

        return response;
    }

    @Override
    public List<TransportDispatchPageResponse> listDispatchOrdersForExport(TransportDispatchPageRequest request) {
        // 使用分页查询方法，但设置一个很大的分页大小来获取所有数据
        Page<DispatchOrder> page = new Page<>(1, Integer.MAX_VALUE);

        // 转换日期字符串
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (StrUtil.isNotBlank(request.getStartTime())) {
            startTime = parseDateTime(request.getStartTime());
        }
        if (StrUtil.isNotBlank(request.getEndTime())) {
            endTime = parseDateTime(request.getEndTime());
        }

        // 获取当前用户ID
        Integer currentUserId = getCurrentUserId();
        // 应用数据范围控制（viewScope）
        // 安全策略：后端以权限配置为准，前端传入的 creatorFilter 只能缩小范围，不能扩大范围
        Integer creatorFilter = null;
        if (!authService.isAdmin(currentUserId)) {
            // 获取当前员工对"运输执行"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "运输管理:运输执行:页面");

            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // viewScope=SELF：后端强制只看自己的数据，忽略前端传参
                creatorFilter = currentUserId;
            } else {
                // viewScope=ALL 或无配置：允许导出全部，但若前端显式传入 creatorFilter 则尊重前端过滤
                // （前端在 viewScope=SELF 时传入 currentEmployeeId，此处作为双重保险）
                if (request.getCreatorFilter() != null) {
                    // 安全校验：前端只能过滤自己的数据，不能冒充他人
                    if (request.getCreatorFilter().equals(currentUserId)) {
                        creatorFilter = request.getCreatorFilter();
                    } else {
                        log.warn("导出运输执行：前端传入 creatorFilter={} 与当前用户 {} 不一致，已忽略",
                                request.getCreatorFilter(), currentUserId);
                    }
                }
            }
        }

        IPage<DispatchOrder> dispatchPage = dispatchOrderMapper.selectDispatchOrderPage(
                page,
                request.getDispatchCode(),
                request.getNoticeCode(),
                request.getContractCode(),
                request.getCarrierName(),
                request.getDriverName(),
                request.getPlateNo(),
                request.getStatus(),
                request.getLocked(),
                request.getDispatcherId(),
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder(),
                creatorFilter
        );

        // 转换为响应DTO
        return dispatchPage.getRecords().stream()
                .map(this::buildPageResponse)
                .collect(Collectors.toList());
    }

    /**
     * 构建分页响应对象
     */
    private TransportDispatchPageResponse buildPageResponse(DispatchOrder order) {
        TransportDispatchPageResponse resp = new TransportDispatchPageResponse();
        resp.setDispatchId(order.getDispatchId());
        resp.setDispatchCode(order.getDispatchCode());
        resp.setNoticeCode(order.getNoticeCode());
        resp.setContractCode(order.getContractCode());
        resp.setCarrierName(order.getCarrierName());
        resp.setCarrierPhone(order.getCarrierPhone());
        resp.setDriverName(order.getDriverName());
        resp.setDriverPhone(order.getDriverPhone());
        resp.setPlateNo(order.getPlateNo());
        resp.setDispatcherId(order.getDispatcherId());
        // 设置创建人编码（用于前端权限判断），运输单以调度员编码作为操作员
        resp.setCreatorId(order.getDispatcherId());
        resp.setOperationLicenseNo(order.getOperationLicenseNo());
        resp.setCarrierAddress(order.getCarrierAddress());
        resp.setTransportTool(order.getTransportTool());
        resp.setVehicleId(order.getVehicleId());
        resp.setDispatcherRemark(order.getDispatcherRemark());
        resp.setStartPoint(order.getStartPoint());
        resp.setEndPoint(order.getEndPoint());
        resp.setDispatchAt(formatDateTime(order.getDispatchAt()));
        resp.setDepartAt(formatDateTime(order.getDepartAt()));
        resp.setArriveAt(formatDateTime(order.getArriveAt()));
        resp.setPlanQuantityTon(order.getPlanQuantityTon());
        resp.setStatus(order.getStatus());
        resp.setLocked(order.getLocked());
        resp.setLockReason(order.getLockReason());
        resp.setLockTime(formatDateTime(order.getLockTime()));
        resp.setLockUserId(order.getLockUserId());
        resp.setCreatedAt(formatDateTime(order.getCreateTime()));
        resp.setUpdatedAt(formatDateTime(order.getUpdateTime()));

        // 总榜单编号（已关联的总榜单号）
        resp.setWeighingSlipCode(order.getWeighingSlipCode());

        // 调度员姓名
        if (order.getDispatcherId() != null) {
            try {
                resp.setDispatcherName(
                        employeeMapper.selectById(order.getDispatcherId()) != null
                                ? employeeMapper.selectById(order.getDispatcherId()).getEmployeeName()
                                : null
                );
            } catch (Exception ignore) {
                resp.setDispatcherName(null);
            }
        }

        return resp;
    }

    /**
     * 计算统计数据
     */
    private List<TransportStat> calculateStats() {
        List<TransportStat> stats = new ArrayList<>();
        
        // 查询所有运输单用于统计
        List<DispatchOrder> allOrders = dispatchOrderMapper.selectList(null);
        
        long totalCount = allOrders.size();
        long pendingCount = allOrders.stream().filter(o -> "待运输".equals(o.getStatus())).count();
        long inTransitCount = allOrders.stream().filter(o -> "运输中".equals(o.getStatus())).count();
        long arrivedCount = allOrders.stream().filter(o -> "已到达".equals(o.getStatus())).count();
        long completedCount = allOrders.stream().filter(o -> "已完成".equals(o.getStatus())).count();
        long cancelledCount = allOrders.stream().filter(o -> "已取消".equals(o.getStatus())).count();
        
        TransportStat stat1 = new TransportStat();
        stat1.setLabel("待运输");
        stat1.setValue(String.valueOf(pendingCount));
        stat1.setColor("warning");
        stats.add(stat1);
        
        TransportStat stat2 = new TransportStat();
        stat2.setLabel("运输中");
        stat2.setValue(String.valueOf(inTransitCount));
        stat2.setColor("primary");
        stats.add(stat2);
        
        TransportStat stat3 = new TransportStat();
        stat3.setLabel("已到达");
        stat3.setValue(String.valueOf(arrivedCount));
        stat3.setColor("info");
        stats.add(stat3);
        
        TransportStat stat4 = new TransportStat();
        stat4.setLabel("已完成");
        stat4.setValue(String.valueOf(completedCount));
        stat4.setColor("success");
        stats.add(stat4);
        
        TransportStat stat5 = new TransportStat();
        stat5.setLabel("已取消");
        stat5.setValue(String.valueOf(cancelledCount));
        stat5.setColor("info");
        stats.add(stat5);
        
        TransportStat stat6 = new TransportStat();
        stat6.setLabel("总数");
        stat6.setValue(String.valueOf(totalCount));
        stat6.setColor("primary");
        stats.add(stat6);
        
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateDispatchOrderPdf(String dispatchCode) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        if (StrUtil.isBlank(dispatchCode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "运输单号不能为空");
        }

        // 查询运输单
        DispatchOrder order = dispatchOrderMapper.selectByDispatchCode(dispatchCode);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "运输单不存在");
        }

        // 获取运输单详情
        TransportDispatchDetailResponse dispatchDetail = getDispatchDetail(dispatchCode, null);

        // 查询该运输单的所有PDF文件记录
        List<File> existingFiles = fileMapper.selectByBusinessTypeAndId(DISPATCH_ORDER_BUSINESS_TYPE, order.getDispatchId());

        // 物理删除所有旧记录和文件
        if (existingFiles != null && !existingFiles.isEmpty()) {
            for (File file : existingFiles) {
                // 删除实际文件
                if (file.getLocalPath() != null && !file.getLocalPath().trim().isEmpty()) {
                    try {
                        String fullPath = localStoragePath + "/" + file.getLocalPath();
                        java.io.File fileObj = new java.io.File(fullPath);
                        if (fileObj.exists() && fileObj.isFile()) {
                            boolean deleted = fileObj.delete();
                            if (deleted) {
                                log.info("删除运输单PDF文件成功：fileId={}, filePath={}", file.getFileId(), fullPath);
                            }
                        }
                    } catch (Exception e) {
                        log.error("删除运输单PDF文件异常：fileId={}, filePath={}", file.getFileId(), file.getLocalPath(), e);
                    }
                }
                // 物理删除数据库记录
                try {
                    fileMapper.deleteById(file.getFileId());
                } catch (Exception e) {
                    log.error("删除运输单PDF数据库记录异常：fileId={}", file.getFileId(), e);
                }
            }
        }

        // 生成PDF文件名：使用运输单号.pdf
        String pdfFileName = dispatchCode + ".pdf";

        // 构建文件存储路径
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = DISPATCH_ORDER_BUSINESS_TYPE + "/" + datePath + "/" + pdfFileName;
        String fullPath = localStoragePath + "/" + relativePath;

        // 创建目录
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(fullPath);
            java.nio.file.Files.createDirectories(filePath.getParent());

            // 生成PDF文件
            com.erp.util.DispatchOrderPdfGenerator.generatePdf(dispatchDetail, fullPath);

            // 获取文件大小
            java.io.File pdfFileObj = new java.io.File(fullPath);
            long fileSize = pdfFileObj.length();

            // 创建文件记录
            File pdfFile = new File();
            pdfFile.setFileName(pdfFileName);
            pdfFile.setFileType("PDF");
            pdfFile.setFileSize(fileSize);
            pdfFile.setStorageType("本地");
            pdfFile.setLocalPath(relativePath);
            pdfFile.setFileUrl(""); // 先设置为空，插入后更新
            pdfFile.setBusinessModule(DISPATCH_ORDER_BUSINESS_MODULE);
            pdfFile.setBusinessId(order.getDispatchId());
            pdfFile.setBusinessType(DISPATCH_ORDER_BUSINESS_TYPE);
            pdfFile.setFileStatus("正常");
            pdfFile.setUploadTime(LocalDateTime.now());
            pdfFile.setUploaderId(currentUserId);
            pdfFile.setCreateTime(LocalDateTime.now());
            pdfFile.setUpdateTime(LocalDateTime.now());
            fileMapper.insert(pdfFile);

            // 更新fileUrl
            pdfFile.setFileUrl("/api/file/preview/" + pdfFile.getFileId());
            fileMapper.updateById(pdfFile);

            log.info("运输单PDF生成成功：dispatchCode={}, pdfFileId={}, filePath={}, fileSize={}, operator={}",
                    dispatchCode, pdfFile.getFileId(), fullPath, fileSize, currentUserId);

            // 返回打印URL（预览URL，用于打印）
            return "/api/file/preview/" + pdfFile.getFileId();

        } catch (Exception e) {
            log.error("生成运输单PDF失败：dispatchCode={}", dispatchCode, e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成PDF失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateBatchDispatchOrderPdf(List<String> dispatchCodes) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        if (dispatchCodes == null || dispatchCodes.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "运输单号列表不能为空");
        }

        // 查询所有运输单
        List<TransportDispatchDetailResponse> dispatchDetails = new java.util.ArrayList<>();
        for (String dispatchCode : dispatchCodes) {
            if (StrUtil.isBlank(dispatchCode)) {
                continue;
            }
            try {
                TransportDispatchDetailResponse detail = getDispatchDetail(dispatchCode, null);
                dispatchDetails.add(detail);
            } catch (Exception e) {
                log.warn("获取运输单详情失败：dispatchCode={}", dispatchCode, e);
                // 继续处理其他运输单，不中断整个流程
            }
        }

        if (dispatchDetails.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "没有找到有效的运输单");
        }

        // 生成合并后的PDF文件名：使用第一个运输单号_批量打印_时间戳.pdf
        String firstDispatchCode = dispatchDetails.get(0).getDispatchCode();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String pdfFileName = firstDispatchCode + "_批量打印_" + timestamp + ".pdf";

        // 构建文件存储路径
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = DISPATCH_ORDER_BUSINESS_TYPE + "/batch/" + datePath + "/" + pdfFileName;
        String fullPath = localStoragePath + "/" + relativePath;

        // 创建目录
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(fullPath);
            java.nio.file.Files.createDirectories(filePath.getParent());

            // 批量生成PDF文件（合并）
            com.erp.util.DispatchOrderPdfGenerator.generateBatchPdf(dispatchDetails, fullPath);

            // 获取文件大小
            java.io.File pdfFileObj = new java.io.File(fullPath);
            long fileSize = pdfFileObj.length();

            // 创建文件记录（批量打印不关联具体的运输单，businessId设为null）
            File pdfFile = new File();
            pdfFile.setFileName(pdfFileName);
            pdfFile.setFileType("PDF");
            pdfFile.setFileSize(fileSize);
            pdfFile.setStorageType("本地");
            pdfFile.setLocalPath(relativePath);
            pdfFile.setFileUrl(""); // 先设置为空，插入后更新
            pdfFile.setBusinessModule(DISPATCH_ORDER_BUSINESS_MODULE);
            pdfFile.setBusinessId(null); // 批量打印不关联具体业务ID
            pdfFile.setBusinessType(DISPATCH_ORDER_BUSINESS_TYPE);
            pdfFile.setFileStatus("正常");
            pdfFile.setUploadTime(LocalDateTime.now());
            pdfFile.setUploaderId(currentUserId);
            pdfFile.setCreateTime(LocalDateTime.now());
            pdfFile.setUpdateTime(LocalDateTime.now());
            fileMapper.insert(pdfFile);

            // 更新fileUrl
            pdfFile.setFileUrl("/api/file/preview/" + pdfFile.getFileId());
            fileMapper.updateById(pdfFile);

            log.info("批量运输单PDF生成成功：dispatchCodes={}, pdfFileId={}, filePath={}, fileSize={}, operator={}",
                    dispatchCodes, pdfFile.getFileId(), fullPath, fileSize, currentUserId);

            // 返回打印URL（预览URL，用于打印）
            return "/api/file/preview/" + pdfFile.getFileId();

        } catch (Exception e) {
            log.error("批量生成运输单PDF失败：dispatchCodes={}", dispatchCodes, e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量生成PDF失败：" + e.getMessage());
        }
    }
}


