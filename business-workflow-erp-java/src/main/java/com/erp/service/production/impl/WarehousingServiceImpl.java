package com.erp.service.production.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.production.dto.BatchCreateWarehousingRequest;
import com.erp.controller.production.dto.BatchCreateWarehousingResponse;
import com.erp.controller.production.dto.UpdateWarehousingRequest;
import com.erp.controller.production.dto.WarehousingDetailResponse;
import com.erp.controller.production.dto.WarehousingItemResponse;
import com.erp.controller.production.dto.WarehousingListResponse;
import com.erp.controller.production.dto.WarehousingPageRequest;
import com.erp.controller.production.dto.WarehousingPageResponse;
import com.erp.controller.production.dto.WarehousingStat;
import com.erp.controller.production.dto.WarehousingWithSettlementVO;
import com.erp.entity.production.PickupNotice;
import com.erp.entity.production.PickupNoticeItem;
import com.erp.entity.production.Warehousing;
import com.erp.entity.production.WarehousingWasteItem;
import com.erp.entity.system.Employee;
import com.erp.mapper.production.PickupNoticeItemMapper;
import com.erp.mapper.production.PickupNoticeMapper;
import com.erp.mapper.production.WarehousingMapper;
import com.erp.mapper.production.WarehousingWasteItemMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.service.production.WarehousingService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 入库单服务实现
 */
@Slf4j
@Service
public class WarehousingServiceImpl implements WarehousingService {

    @Autowired
    private WarehousingMapper warehousingMapper;

    @Autowired
    private WarehousingWasteItemMapper warehousingWasteItemMapper;

    @Autowired
    private PickupNoticeItemMapper pickupNoticeItemMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private PickupNoticeMapper pickupNoticeMapper;

    @Autowired
    private com.erp.mapper.contract.ContractMapper contractMapper;

    @Autowired
    private com.erp.mapper.customer.CustomerMapper customerMapper;

    @Autowired
    private com.erp.service.contract.ContractApprovalFlowService contractApprovalFlowService;

    @Autowired
    private com.erp.mapper.production.WeighingSlipMapper weighingSlipMapper;

    @Autowired
    private com.erp.mapper.production.WeighingSlipDispatchMapper weighingSlipDispatchMapper;

    @Autowired
    private com.erp.mapper.transport.DispatchOrderMapper dispatchOrderMapper;

    @Autowired
    private com.erp.mapper.system.HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private com.erp.mapper.contract.ContractWasteItemMapper contractWasteItemMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    private static final String WAREHOUSING_BUSINESS_TYPE = "WAREHOUSING";
    private static final String WAREHOUSING_NO_PREFIX = "RKD";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchCreateWarehousingResponse batchCreateWarehousing(BatchCreateWarehousingRequest request) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "用户未登录");
        }

        if (request.getWarehousingList() == null || request.getWarehousingList().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "至少需要创建一个入库单");
        }

        // 校验总磅单状态：只有"待细分"状态的总磅单才能创建入库单
        if (StrUtil.isNotBlank(request.getWeighingSlipNo())) {
            com.erp.entity.production.WeighingSlip weighingSlip = weighingSlipMapper.selectByWeighingSlipNo(request.getWeighingSlipNo());
            if (weighingSlip == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "总磅单不存在：" + request.getWeighingSlipNo());
            }
            if ("已细分".equals(weighingSlip.getStatus())) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), 
                        "总磅单状态为\"已细分\"，不允许再次创建入库单。总磅单号：" + request.getWeighingSlipNo());
            }
            
            // 校验入库单的运输单是否都关联到该总磅单（前端传入 dispatchCode）
            List<String> dispatchCodesFromRequest = request.getWarehousingList().stream()
                    .map(BatchCreateWarehousingRequest.WarehousingForm::getDispatchCode)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());

            if (!dispatchCodesFromRequest.isEmpty()) {
                // 获取总磅单关联的运输单号列表
                List<String> weighingSlipDispatchCodes = weighingSlipDispatchMapper.selectDispatchCodesByWeighingSlipId(weighingSlip.getWeighingSlipId());

                if (weighingSlipDispatchCodes.isEmpty()) {
                    throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                            "总磅单未关联任何运输单，无法创建入库单。总磅单号：" + request.getWeighingSlipNo());
                }

                for (String dispatchCode : dispatchCodesFromRequest) {
                    if (!weighingSlipDispatchCodes.contains(dispatchCode)) {
                        throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                                String.format("运输单 %s 未关联到总磅单 %s，无法创建入库单", dispatchCode, request.getWeighingSlipNo()));
                    }
                }
            }
        }

        BatchCreateWarehousingResponse response = new BatchCreateWarehousingResponse();
        List<BatchCreateWarehousingResponse.WarehousingInfo> warehousingList = new ArrayList<>();

        // 批量查询：已通过前端提供的 dispatchCode 列表，查询对应的收运通知单明细（通过 dispatch -> notice 反查）
        List<String> dispatchCodes = request.getWarehousingList().stream()
                .map(BatchCreateWarehousingRequest.WarehousingForm::getDispatchCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        // dispatchCode -> noticeCode 映射
        Map<String, String> dispatchCodeToNoticeCodeMap = new java.util.HashMap<>();
        if (!dispatchCodes.isEmpty()) {
            List<com.erp.entity.transport.DispatchOrder> dispatchOrders = dispatchOrderMapper.selectList(
                    new LambdaQueryWrapper<com.erp.entity.transport.DispatchOrder>()
                            .in(com.erp.entity.transport.DispatchOrder::getDispatchCode, dispatchCodes)
            );
            dispatchCodeToNoticeCodeMap = dispatchOrders.stream()
                    .filter(d -> StrUtil.isNotBlank(d.getDispatchCode()) && StrUtil.isNotBlank(d.getNoticeCode()))
                    .collect(Collectors.toMap(
                            com.erp.entity.transport.DispatchOrder::getDispatchCode,
                            com.erp.entity.transport.DispatchOrder::getNoticeCode,
                            (existing, replacement) -> existing
                    ));
        }

        // 用 noticeCodes 批量查询收运通知单明细
        List<String> noticeCodes = dispatchCodeToNoticeCodeMap.values().stream().distinct().collect(Collectors.toList());
        Map<String, List<PickupNoticeItem>> noticeItemMap = pickupNoticeItemMapper.selectByNoticeCodes(noticeCodes)
                .stream()
                .collect(Collectors.groupingBy(PickupNoticeItem::getNoticeCode));

        // 为每个收运通知单创建入库单
        // 如果任何一个入库单创建失败，将抛出异常，整个事务回滚
        for (BatchCreateWarehousingRequest.WarehousingForm form : request.getWarehousingList()) {
            // 生成入库单号
            String warehousingNo = generateWarehousingNo();

            // 获取运输单号（直接使用前端传入的运输单号）
            String dispatchCode = form.getDispatchCode();
            // 对应的收运通知单号（可能为空）
            String noticeCode = dispatchCodeToNoticeCodeMap.get(dispatchCode);

            // 创建入库单
            Warehousing warehousing = new Warehousing();
            warehousing.setWarehousingNo(warehousingNo);
            warehousing.setWeighingSlipNo(request.getWeighingSlipNo());
            // 将运输单号存储到入库单的运输单号字段
            warehousing.setDispatchCode(dispatchCode);
            warehousing.setWarehousingTime(form.getWarehousingTime() != null 
                    ? form.getWarehousingTime() 
                    : request.getWarehousingTime());
            warehousing.setWarehouseKeeperId(form.getWarehouseKeeperId() != null 
                    ? form.getWarehouseKeeperId() 
                    : request.getWarehouseKeeperId());
            warehousing.setRemark(form.getRemark() != null
                    ? form.getRemark()
                    : request.getRemark());
            warehousing.setStatus("待结算");
            warehousing.setLocked(false);
            warehousing.setCreateTime(LocalDateTime.now());
            warehousing.setUpdateTime(null); // 创建时更新时间置空

            warehousingMapper.insert(warehousing);

            // 创建入库单危废明细
            List<PickupNoticeItem> noticeItems = noticeItemMap.getOrDefault(noticeCode, new ArrayList<>());
            Map<Integer, PickupNoticeItem> itemMap = noticeItems.stream()
                    .collect(Collectors.toMap(PickupNoticeItem::getItemId, item -> item));

            for (BatchCreateWarehousingRequest.WarehousingItemForm itemForm : form.getItems()) {
                PickupNoticeItem noticeItem = itemMap.get(itemForm.getPickupNoticeItemId());
                if (noticeItem == null) {
                    throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(),
                            "收运通知单明细不存在：pickupNoticeItemId=" + itemForm.getPickupNoticeItemId());
                }

                WarehousingWasteItem wasteItem = new WarehousingWasteItem();
                wasteItem.setWarehousingId(warehousing.getWarehousingId());
                wasteItem.setPickupNoticeItemId(itemForm.getPickupNoticeItemId());
                wasteItem.setHazardousWasteItemId(noticeItem.getHazardousWasteItemId());
                wasteItem.setWasteName(noticeItem.getWasteName());
                wasteItem.setWasteCode(noticeItem.getWasteCode());
                wasteItem.setForm(noticeItem.getForm());
                // 继承危险特性（从收运通知单明细）
                wasteItem.setHazardFeature(noticeItem.getHazardFeature());
                wasteItem.setPlannedQty(noticeItem.getPlannedQtyTon());
                // 继承收运通知单明细的辅助核算相关字段
                wasteItem.setMeasureUnit(noticeItem.getMeasureUnit());
                wasteItem.setEnableAuxiliaryAccounting(noticeItem.getEnableAuxiliaryAccounting());
                wasteItem.setAuxUnit(noticeItem.getAuxUnit());
                wasteItem.setAuxPerBase(noticeItem.getAuxPerBase());
                wasteItem.setAuxQuantity(noticeItem.getAuxQuantity());
                wasteItem.setActualQty(itemForm.getActualQty());
                // 新增：保存实际收运辅助数量（桶/袋等）
                wasteItem.setActualAuxQuantity(itemForm.getActualAuxQuantity());
                wasteItem.setDifferenceReason(itemForm.getDifferenceReason());
                wasteItem.setValuableWeight(itemForm.getValuableWeight());
                wasteItem.setValuelessWeight(itemForm.getValuelessWeight());
                wasteItem.setCreateTime(LocalDateTime.now());

                warehousingWasteItemMapper.insert(wasteItem);
            }

            // 创建成功，添加到响应列表
            BatchCreateWarehousingResponse.WarehousingInfo info = new BatchCreateWarehousingResponse.WarehousingInfo();
            info.setWarehousingNo(warehousingNo);
            info.setDispatchCode(dispatchCode);
            warehousingList.add(info);

            log.info("创建入库单成功：warehousingNo={}, dispatchCode={}", warehousingNo, dispatchCode);
        }

        // 创建入库单后，更新总磅单状态为"已细分"
        if (StrUtil.isNotBlank(request.getWeighingSlipNo())) {
            com.erp.entity.production.WeighingSlip weighingSlip = weighingSlipMapper.selectByWeighingSlipNo(request.getWeighingSlipNo());
            if (weighingSlip != null && !"已细分".equals(weighingSlip.getStatus())) {
                weighingSlip.setStatus("已细分");
                int rows = weighingSlipMapper.updateById(weighingSlip);
                if (rows == 0) {
                    log.warn("更新总磅单状态失败（乐观锁冲突），weighingSlipNo={}", request.getWeighingSlipNo());
                }
                log.info("总磅单状态已更新为已细分：{}", request.getWeighingSlipNo());
            }
        }

        // 记录批量创建入库单的数据变更日志
        try {
            log.info("准备记录批量创建入库单的数据变更日志 - 共创建{}个入库单", warehousingList.size());
            for (BatchCreateWarehousingResponse.WarehousingInfo info : warehousingList) {
                logRecordService.recordDataChangeLog("入库单管理", "WAREHOUSING",
                        String.valueOf(info.getWarehousingNo()),
                        "新增",
                        "批量创建入库单：入库单号=" + info.getWarehousingNo() + "，运输单号=" + info.getDispatchCode(),
                        null, info, currentUserId, null, true, null);
            }
            log.info("批量创建入库单的数据变更日志记录完成 - 共记录{}条", warehousingList.size());
        } catch (Exception e) {
            log.error("记录批量创建入库单数据变更日志失败", e);
            // 数据变更日志记录失败不影响主业务流程
        }

        // 创建入库单后，更新相关运输单和收运通知单状态为"已完成"
        // 获取本次创建入库单涉及的所有运输单号
        Set<String> uniqueDispatchCodes = request.getWarehousingList().stream()
                .map(com.erp.controller.production.dto.BatchCreateWarehousingRequest.WarehousingForm::getDispatchCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        // 获取本次创建入库单涉及的所有收运通知单号
        Set<String> uniqueNoticeCodes = dispatchCodeToNoticeCodeMap.values().stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        // 批量更新运输单状态为"已完成"
        for (String dispatchCode : uniqueDispatchCodes) {
            try {
                // 查询运输单
                com.erp.entity.transport.DispatchOrder dispatchOrder = dispatchOrderMapper.selectByDispatchCode(dispatchCode);
                if (dispatchOrder != null && !"已完成".equals(dispatchOrder.getStatus())
                        && !"已取消".equals(dispatchOrder.getStatus())) {
                    // 更新运输单状态为"已完成"
                    dispatchOrder.setStatus("已完成");
                    dispatchOrder.setUpdateTime(LocalDateTime.now());
                    int rows = dispatchOrderMapper.updateById(dispatchOrder);
                    if (rows == 0) {
                        log.warn("更新运输单状态失败（乐观锁冲突），dispatchCode={}", dispatchCode);
                    }

                    log.info("运输单状态已更新为已完成：dispatchCode={}", dispatchCode);

                    // 记录状态变更日志
                    logRecordService.recordOperationLog("入库单管理", "状态同步",
                            "入库单创建完成，运输单状态更新：已到达→已完成，运输单号=" + dispatchCode,
                            currentUserId, null, true, null);
                }
            } catch (Exception e) {
                log.error("更新运输单状态失败：dispatchCode={}", dispatchCode, e);
                // 不抛出异常，避免影响主要业务流程
            }
        }

        // 批量更新收运通知单状态为"已完成"
        // 注意：这个操作必须在事务中，要么全部成功，要么全部回滚
        for (String noticeCode : uniqueNoticeCodes) {
            // 查询收运通知单
            com.erp.entity.production.PickupNotice pickupNotice = pickupNoticeMapper.selectDetailByNoticeCode(noticeCode);
            if (pickupNotice != null && "已派单".equals(pickupNotice.getStatus())) {
                // 更新收运通知单状态为"已完成"
                pickupNotice.setStatus("已完成");
                pickupNotice.setUpdateTime(LocalDateTime.now());
                int rows = pickupNoticeMapper.updateById(pickupNotice);
                if (rows == 0) {
                    log.warn("更新收运通知单状态失败（乐观锁冲突），noticeCode={}", noticeCode);
                }

                log.info("收运通知单状态已更新为已完成：noticeCode={}", noticeCode);

                // 记录状态变更日志
                logRecordService.recordOperationLog("入库单管理", "状态同步",
                        "入库单创建完成，收运通知单状态更新：已派单→已完成，收运通知单号=" + noticeCode,
                        currentUserId, null, true, null);
            }
        }

        response.setWarehousingList(warehousingList);

        return response;
    }

    /**
     * 生成入库单号（规则：RKD-YYYYMMDD-4位序号）
     */
    private String generateWarehousingNo() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String prefix = WAREHOUSING_NO_PREFIX + "-" + datePart + "-";
        String maxCode = warehousingMapper.selectMaxWarehousingNoByPrefix(prefix);
        int nextSeq = 1;
        if (StrUtil.isNotBlank(maxCode) && maxCode.length() > prefix.length()) {
            String seq = maxCode.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seq) + 1;
            } catch (NumberFormatException e) {
                log.warn("解析入库单号序列失败: {}", maxCode, e);
            }
        }
        String warehousingNo = prefix + String.format("%04d", nextSeq);
        int retry = 0;
        while (warehousingMapper.countByWarehousingNo(warehousingNo) > 0 && retry < 20) {
            nextSeq++;
            warehousingNo = prefix + String.format("%04d", nextSeq);
            retry++;
        }
        if (retry >= 20) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成入库单号失败，请重试");
        }
        log.debug("生成入库单号：{}", warehousingNo);
        return warehousingNo;
    }

    @Override
    public WarehousingListResponse getWarehousingPage(WarehousingPageRequest request) {
        try {
            // 设置默认值
            Integer page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
            Integer size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 20;

            Page<WarehousingPageResponse> pageObj = new Page<>(page, size);

            // 页面权限编码
            String pageCode = "仓库管理:入库-入库单:页面";

            // 使用 ViewScopeHelper 解析视图范围
            String viewScope = ViewScopeHelper.resolveViewScope(pageCode, request.getViewScope());

            // 获取当前用户ID
            Integer currentUserId = SecurityUtil.getCurrentUserId();

            // SELF 模式时添加创建人过滤条件，ALL 模式时不限制
            Integer creatorFilter = ViewScopeHelper.isSelfScope(viewScope) ? currentUserId : null;

            // 处理空字符串，转换为null
            String keyword = normalize(request.getKeyword());
            String weighingSlipNo = normalize(request.getWeighingSlipNo());
            String dispatchCode = normalize(request.getDispatchCode());
            String status = normalize(request.getStatus());
            String startTime = normalize(request.getStartTime());
            String endTime = normalize(request.getEndTime());
            String orderBy = normalize(request.getOrderBy());
            String orderDirection = normalize(request.getOrderDirection());

            IPage<WarehousingPageResponse> entityPage = warehousingMapper.selectWarehousingPage(
                    pageObj,
                    keyword,
                    weighingSlipNo,
                    dispatchCode,
                    status,
                    startTime,
                    endTime,
                    orderBy,
                    orderDirection,
                    creatorFilter,
                    request.getIndependentOnly()
            );

            List<WarehousingPageResponse> records = entityPage.getRecords();
            if (CollectionUtils.isEmpty(records)) {
                WarehousingListResponse response = new WarehousingListResponse();
                response.setStats(buildStats());
                response.setRecords(new ArrayList<>());
                response.setTotal(entityPage.getTotal());
                response.setCurrent((int) entityPage.getCurrent());
                response.setSize((int) entityPage.getSize());
                return response;
            }

            WarehousingListResponse response = new WarehousingListResponse();
            response.setStats(buildStats());
            response.setRecords(records);
            response.setTotal(entityPage.getTotal());
            response.setCurrent((int) entityPage.getCurrent());
            response.setSize((int) entityPage.getSize());

            return response;
        } catch (Exception e) {
            log.error("查询入库单列表异常，异常类型：{}，异常信息：{}",
                    e.getClass().getName(), e.getMessage() != null ? e.getMessage() : "null", e);
            throw e;
        }
    }

    /**
     * 构建统计信息
     */
    private List<WarehousingStat> buildStats() {
        try {
            Map<String, Long> statusStats = getStatusStatistics();
            return Arrays.asList(
                createStat("待结算", statusStats.getOrDefault("待结算", 0L), "warning"),
                createStat("结算中", statusStats.getOrDefault("结算中", 0L), "info"),
                createStat("已结算", statusStats.getOrDefault("已结算", 0L), "success"),
                createStat("已锁定", statusStats.getOrDefault("已锁定", 0L), "info"),
                createStat("总数", statusStats.values().stream().mapToLong(Long::longValue).sum(), "info")
            );
        } catch (Exception e) {
            log.error("构建统计信息失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 创建统计对象
     */
    private WarehousingStat createStat(String label, long value, String color) {
        WarehousingStat stat = new WarehousingStat();
        stat.setLabel(label);
        stat.setValue(String.valueOf(value));
        stat.setColor(color);
        return stat;
    }

    /**
     * 规范化字符串（去除空格，空字符串转为null）
     */
    private String normalize(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public WarehousingDetailResponse getWarehousingDetail(Integer warehousingId) {
        if (warehousingId == null || warehousingId <= 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "入库单编号无效");
        }

        // 查询入库单基本信息
        Warehousing warehousing = warehousingMapper.selectByWarehousingId(warehousingId);
        if (warehousing == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "入库单不存在");
        }

        // 构建响应对象
        WarehousingDetailResponse response = new WarehousingDetailResponse();
        response.setWarehousingId(warehousing.getWarehousingId());
        response.setWarehousingNo(warehousing.getWarehousingNo());
        response.setWeighingSlipNo(warehousing.getWeighingSlipNo());
        response.setDispatchCode(warehousing.getDispatchCode());
        
        if (warehousing.getWarehousingTime() != null) {
            response.setWarehousingTime(warehousing.getWarehousingTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        response.setWarehouseKeeperId(warehousing.getWarehouseKeeperId());
        
        if (warehousing.getAuditTime() != null) {
            response.setAuditTime(warehousing.getAuditTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        response.setAuditorId(warehousing.getAuditorId());
        response.setRemark(warehousing.getRemark());
        response.setLocked(warehousing.getLocked());
        
        if (warehousing.getCreateTime() != null) {
            response.setCreateTime(warehousing.getCreateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        if (warehousing.getUpdateTime() != null) {
            response.setUpdateTime(warehousing.getUpdateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        if (warehousing.getLockTime() != null) {
            response.setLockTime(warehousing.getLockTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        response.setLockUserId(warehousing.getLockUserId());
        response.setLockReason(warehousing.getLockReason());

        // 查询仓管员信息
        if (warehousing.getWarehouseKeeperId() != null) {
            Employee keeper = employeeMapper.selectById(warehousing.getWarehouseKeeperId());
            if (keeper != null) {
                response.setWarehouseKeeperName(keeper.getEmployeeName());
            }
        }

        // 查询审核人信息
        if (warehousing.getAuditorId() != null) {
            Employee auditor = employeeMapper.selectById(warehousing.getAuditorId());
            if (auditor != null) {
                response.setAuditorName(auditor.getEmployeeName());
            }
        }

    
        response.setStatus(warehousing.getStatus());

        // 检查是否锁定
        if (warehousing.getLocked() != null && warehousing.getLocked()) {
            response.setStatus("已锁定");
        }

        // 查询合同号和客户名称（通过运输单号关联到收运通知单）
        if (StrUtil.isNotBlank(warehousing.getDispatchCode())) {
            try {
                com.erp.entity.transport.DispatchOrder dispatchOrder = dispatchOrderMapper.selectOne(
                        new LambdaQueryWrapper<com.erp.entity.transport.DispatchOrder>()
                                .eq(com.erp.entity.transport.DispatchOrder::getDispatchCode, warehousing.getDispatchCode())
                                .last("LIMIT 1")
                );
                if (dispatchOrder != null && StrUtil.isNotBlank(dispatchOrder.getNoticeCode())) {
                    com.erp.entity.production.PickupNotice pickupNotice = pickupNoticeMapper.selectOne(
                            new LambdaQueryWrapper<com.erp.entity.production.PickupNotice>()
                                    .eq(com.erp.entity.production.PickupNotice::getNoticeCode, dispatchOrder.getNoticeCode())
                    );
                    if (pickupNotice != null) {
                        response.setContractCode(pickupNotice.getContractCode());
                        if (pickupNotice.getCustomerId() != null) {
                            try {
                                com.erp.entity.customer.Customer customer = customerMapper.selectById(pickupNotice.getCustomerId());
                                if (customer != null) {
                                    response.setCustomerName(customer.getEnterpriseName());
                                }
                            } catch (Exception e) {
                                log.warn("查询客户信息失败：customerId={}", pickupNotice.getCustomerId(), e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("查询运输单/收运通知单信息失败：dispatchCode={}", warehousing.getDispatchCode(), e);
            }
        }

        // 查询危废明细
        List<WarehousingWasteItem> items = warehousingWasteItemMapper.selectByWarehousingId(warehousingId);
        List<WarehousingItemResponse> itemResponses = new ArrayList<>();
        if (!CollectionUtils.isEmpty(items)) {
            for (WarehousingWasteItem item : items) {
                WarehousingItemResponse itemResponse = new WarehousingItemResponse();
                itemResponse.setItemId(item.getItemId());
                itemResponse.setPickupNoticeItemId(item.getPickupNoticeItemId());
                itemResponse.setHazardousWasteItemId(item.getHazardousWasteItemId());
                // 查询危废类型（废物类别）
                if (item.getHazardousWasteItemId() != null) {
                    try {
                        com.erp.entity.system.HazardousWasteItem hazardousWasteItem = hazardousWasteItemMapper.selectDetailById(item.getHazardousWasteItemId());
                        if (hazardousWasteItem != null) {
                            itemResponse.setWasteCategory(hazardousWasteItem.getWasteCategory());
                        }
                    } catch (Exception e) {
                        log.warn("查询危废条目废物类别失败：hazardousWasteItemId={}", item.getHazardousWasteItemId(), e);
                    }
                }
                itemResponse.setWasteName(item.getWasteName());
                itemResponse.setWasteCode(item.getWasteCode());
                itemResponse.setForm(item.getForm());
                itemResponse.setHazardFeature(item.getHazardFeature());
                itemResponse.setPlannedQty(item.getPlannedQty());
                // 返回辅助核算相关字段
                itemResponse.setMeasureUnit(item.getMeasureUnit());
                itemResponse.setEnableAuxiliaryAccounting(item.getEnableAuxiliaryAccounting());
                itemResponse.setAuxUnit(item.getAuxUnit());
                itemResponse.setAuxPerBase(item.getAuxPerBase());
                itemResponse.setAuxQuantity(item.getAuxQuantity());
                itemResponse.setActualAuxQuantity(item.getActualAuxQuantity());
                itemResponse.setActualQty(item.getActualQty());
                itemResponse.setDifferenceReason(item.getDifferenceReason());
                // 设置有价无价重量字段
                itemResponse.setValuableWeight(item.getValuableWeight());
                itemResponse.setValuelessWeight(item.getValuelessWeight());

                if (item.getCreateTime() != null) {
                    itemResponse.setActualWarehousingDate(item.getCreateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }

                if (item.getUpdateTime() != null) {
                    itemResponse.setUpdateTime(item.getUpdateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }

                itemResponses.add(itemResponse);
            }
        }
        response.setItems(itemResponses);

        return response;
    }

    @Override
    public List<WarehousingDetailResponse> getWarehousingDetailsBatch(List<Integer> warehousingIds) {
        if (warehousingIds == null || warehousingIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 使用Mapper的批量查询方法，实现真正的数据库级批量查询
            List<WarehousingDetailResponse> results = warehousingMapper.selectWarehousingDetailsBatch(warehousingIds);

            // 对结果进行分组整理，确保每个入库单的明细信息正确聚合
            Map<Integer, WarehousingDetailResponse> warehousingMap = new java.util.HashMap<>();

            for (WarehousingDetailResponse detail : results) {
                Integer warehousingId = detail.getWarehousingId();

                // 如果该入库单还未添加到结果中，则创建新的响应对象
                if (!warehousingMap.containsKey(warehousingId)) {
                    WarehousingDetailResponse newDetail = new WarehousingDetailResponse();
                    newDetail.setWarehousingId(detail.getWarehousingId());
                    newDetail.setWarehousingNo(detail.getWarehousingNo());
                    newDetail.setWeighingSlipNo(detail.getWeighingSlipNo());
                    newDetail.setDispatchCode(detail.getDispatchCode());
                    newDetail.setContractCode(detail.getContractCode());
                    newDetail.setCustomerName(detail.getCustomerName());
                    newDetail.setWarehousingTime(detail.getWarehousingTime());
                    newDetail.setWarehouseKeeperId(detail.getWarehouseKeeperId());
                    newDetail.setWarehouseKeeperName(detail.getWarehouseKeeperName());
                    newDetail.setAuditTime(detail.getAuditTime());
                    newDetail.setAuditorId(detail.getAuditorId());
                    newDetail.setAuditorName(detail.getAuditorName());
                    newDetail.setRemark(detail.getRemark());
                    newDetail.setStatus(detail.getStatus());
                    newDetail.setLocked(detail.getLocked());
                    newDetail.setLockTime(detail.getLockTime());
                    newDetail.setLockUserId(detail.getLockUserId());
                    newDetail.setLockReason(detail.getLockReason());
                    newDetail.setCreateTime(detail.getCreateTime());
                    newDetail.setUpdateTime(detail.getUpdateTime());
                    newDetail.setItems(new ArrayList<>());
                    warehousingMap.put(warehousingId, newDetail);
                }

                // 将明细项添加到对应的入库单中
                if (detail.getItems() != null && !detail.getItems().isEmpty()) {
                    WarehousingDetailResponse existingDetail = warehousingMap.get(warehousingId);
                    existingDetail.getItems().addAll(detail.getItems());
                }
            }

            List<WarehousingDetailResponse> finalResults = new ArrayList<>(warehousingMap.values());

            log.debug("批量查询入库单详情完成：请求数量={}, 返回数量={}", warehousingIds.size(), finalResults.size());
            return finalResults;

        } catch (Exception e) {
            log.error("批量查询入库单详情失败：warehousingIds={}, error={}", warehousingIds, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarehousingDetailResponse updateWarehousing(UpdateWarehousingRequest request) {
        if (request == null || request.getWarehousingId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "入库单编号不能为空");
        }

        // 查询入库单
        Warehousing warehousing = warehousingMapper.selectByWarehousingId(request.getWarehousingId());
        if (warehousing == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "入库单不存在");
        }

        // 检查是否可编辑（只有未锁定的入库单才能编辑）
        if (warehousing.getLocked() != null && warehousing.getLocked()) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "已锁定的入库单不能修改");
        }

        // 检查入库单状态：已结算的入库单不允许修改
        if (warehousing.getAuditTime() != null) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "已结算的入库单不能修改");
        }

        // 保存旧数据用于日志记录
        WarehousingDetailResponse oldDetail = null;
        try {
            oldDetail = getWarehousingDetail(request.getWarehousingId());
        } catch (Exception e) {
            log.warn("获取入库单旧数据失败，无法记录数据变更日志：warehousingId={}", request.getWarehousingId(), e);
        }
        // 调试日志：输出请求明细，确认辅助核算字段是否传入
        if (log.isInfoEnabled()) {
            log.info("updateWarehousing - received items: {}", request.getItems());
        }

        // 更新基本信息
        if (StrUtil.isNotBlank(request.getWeighingSlipNo())) {
            // 校验新的总磅单号是否存在
            com.erp.entity.production.WeighingSlip newWeighingSlip = weighingSlipMapper.selectByWeighingSlipNo(request.getWeighingSlipNo());
            if (newWeighingSlip == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), 
                        "总磅单不存在：" + request.getWeighingSlipNo());
            }
            
            // 如果入库单有运输单号，校验新的总磅单是否关联了该运输单
            String dispatchCodeToCheck = StrUtil.isNotBlank(request.getDispatchCode())
                    ? request.getDispatchCode()
                    : warehousing.getDispatchCode();

            if (StrUtil.isNotBlank(dispatchCodeToCheck)) {
                // 查询运输单
                com.erp.entity.transport.DispatchOrder dispatchOrder = dispatchOrderMapper.selectOne(
                        new LambdaQueryWrapper<com.erp.entity.transport.DispatchOrder>()
                                .eq(com.erp.entity.transport.DispatchOrder::getDispatchCode, dispatchCodeToCheck)
                                .last("LIMIT 1")
                );

                if (dispatchOrder != null && StrUtil.isNotBlank(dispatchOrder.getDispatchCode())) {
                    // 获取新总磅单关联的运输单号列表
                    List<String> weighingSlipDispatchCodes = weighingSlipDispatchMapper.selectDispatchCodesByWeighingSlipId(newWeighingSlip.getWeighingSlipId());

                    if (!weighingSlipDispatchCodes.contains(dispatchOrder.getDispatchCode())) {
                        throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                                String.format("总磅单 %s 未关联运输单 %s，无法更新入库单",
                                        request.getWeighingSlipNo(), dispatchOrder.getDispatchCode()));
                    }
                }
            }
            
            warehousing.setWeighingSlipNo(request.getWeighingSlipNo());
        }
        if (StrUtil.isNotBlank(request.getDispatchCode())) {
            warehousing.setDispatchCode(request.getDispatchCode());
        }
        if (StrUtil.isNotBlank(request.getWarehousingTime())) {
            warehousing.setWarehousingTime(LocalDateTime.parse(request.getWarehousingTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (request.getWarehouseKeeperId() != null) {
            warehousing.setWarehouseKeeperId(request.getWarehouseKeeperId());
        }
        if (request.getRemark() != null) {
            warehousing.setRemark(request.getRemark());
        }

        // 更新时设置更新时间为当前时间
        warehousing.setUpdateTime(LocalDateTime.now());

        int rows = warehousingMapper.updateById(warehousing);
        if (rows == 0) {
            throw new BusinessException("更新入库单失败：记录已被其他用户修改");
        }

        // 更新危废明细
        if (!CollectionUtils.isEmpty(request.getItems())) {
            // 查询现有明细ID集合
            List<WarehousingWasteItem> existingItems = warehousingWasteItemMapper.selectByWarehousingId(request.getWarehousingId());
            List<Integer> existingItemIds = existingItems.stream()
                    .map(WarehousingWasteItem::getItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // 收集请求中的itemId
            List<Integer> requestItemIds = request.getItems().stream()
                    .map(com.erp.controller.production.dto.UpdateWarehousingItemRequest::getItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // 删除不在请求中的明细（已删除的明细）
            List<Integer> toDeleteIds = existingItemIds.stream()
                    .filter(id -> !requestItemIds.contains(id))
                    .collect(Collectors.toList());
            if (!toDeleteIds.isEmpty()) {
                for (Integer id : toDeleteIds) {
                    rows = warehousingWasteItemMapper.deleteById(id);
                    if (rows == 0) {
                        log.warn("删除入库单明细失败，itemId={}", id);
                    }
                }
            }

            // 更新或插入明细
            for (com.erp.controller.production.dto.UpdateWarehousingItemRequest itemRequest : request.getItems()) {
                WarehousingWasteItem item = new WarehousingWasteItem();
                item.setWarehousingId(request.getWarehousingId());
                item.setPickupNoticeItemId(itemRequest.getPickupNoticeItemId());

                // 设置危废条目编号：优先使用前端传递的值，其次从收运通知单明细，最后从废物代码查询
                Integer hazardousWasteItemId = itemRequest.getHazardousWasteItemId();
                if (hazardousWasteItemId == null && itemRequest.getPickupNoticeItemId() != null) {
                    // 2. 从收运通知单明细查询
                    PickupNoticeItem noticeItem = pickupNoticeItemMapper.selectById(itemRequest.getPickupNoticeItemId());
                    if (noticeItem != null) {
                        hazardousWasteItemId = noticeItem.getHazardousWasteItemId();
                    }
                }
                if (hazardousWasteItemId == null && StrUtil.isNotBlank(itemRequest.getWasteCode())) {
                    // 3. 根据废物代码从HAZARDOUS_WASTE_ITEM表查询
                    com.erp.entity.system.HazardousWasteItem wasteItem =
                        hazardousWasteItemMapper.selectOne(
                            new LambdaQueryWrapper<com.erp.entity.system.HazardousWasteItem>()
                                .eq(com.erp.entity.system.HazardousWasteItem::getWasteCode, itemRequest.getWasteCode().trim())
                        );
                    if (wasteItem != null) {
                        hazardousWasteItemId = wasteItem.getItemId();
                    }
                }
                item.setHazardousWasteItemId(hazardousWasteItemId);

                item.setWasteName(itemRequest.getWasteName());
                item.setWasteCode(itemRequest.getWasteCode());
                item.setForm(itemRequest.getForm());

                // 设置危险特性：优先使用前端传递的值，其次使用收运通知单明细中的危险特性
                if (itemRequest.getHazardFeature() != null && !itemRequest.getHazardFeature().trim().isEmpty()) {
                    // 前端传了危险特性，直接使用
                    item.setHazardFeature(itemRequest.getHazardFeature().trim());
                } else {
                    // 前端没传或为空，从收运通知单明细获取
                    if (itemRequest.getPickupNoticeItemId() != null) {
                        PickupNoticeItem noticeItem = pickupNoticeItemMapper.selectById(itemRequest.getPickupNoticeItemId());
                        if (noticeItem != null) {
                            item.setHazardFeature(noticeItem.getHazardFeature());
                        }
                    }
                }

                item.setPlannedQty(itemRequest.getPlannedQty());
                // 保存辅助核算相关字段（严格处理 null/false）
                item.setMeasureUnit(itemRequest.getMeasureUnit());
                Boolean enableAux = itemRequest.getEnableAuxiliaryAccounting();
                if (Boolean.TRUE.equals(enableAux)) {
                    // 明确启用：保存请求中提供的辅助字段
                    item.setEnableAuxiliaryAccounting(true);
                    item.setAuxUnit(itemRequest.getAuxUnit());
                    item.setAuxPerBase(itemRequest.getAuxPerBase());
                    item.setAuxQuantity(itemRequest.getAuxQuantity());
                    item.setActualAuxQuantity(itemRequest.getActualAuxQuantity());
                } else {
                    // 未明确启用或明确禁用：将辅助核算标记为 false 并清空辅助字段，避免遗留旧值
                    item.setEnableAuxiliaryAccounting(false);
                    item.setAuxUnit(null);
                    item.setAuxPerBase(null);
                    item.setAuxQuantity(null);
                    item.setActualAuxQuantity(null);
                }
                item.setActualQty(itemRequest.getActualQty());
                item.setDifferenceReason(itemRequest.getDifferenceReason());
                item.setValuableWeight(itemRequest.getValuableWeight());
                item.setValuelessWeight(itemRequest.getValuelessWeight());
                
                if (itemRequest.getItemId() != null && existingItemIds.contains(itemRequest.getItemId())) {
                    // 更新已有明细
                    item.setItemId(itemRequest.getItemId());
                    rows = warehousingWasteItemMapper.updateById(item);
                    if (rows == 0) {
                        log.warn("更新入库单明细失败（乐观锁冲突），itemId={}", itemRequest.getItemId());
                    }
                } else {
                    // 插入新明细
                    warehousingWasteItemMapper.insert(item);
                }
            }

            // 校验实际入库总量与总磅净重差异（如果总磅单号存在）
            if (StrUtil.isNotBlank(warehousing.getWeighingSlipNo())) {
                com.erp.entity.production.WeighingSlip weighingSlip = weighingSlipMapper.selectByWeighingSlipNo(warehousing.getWeighingSlipNo());
                if (weighingSlip != null && weighingSlip.getNetWeight() != null) {
                    // 计算当前入库单的实际入库总量（吨）
                    double currentActualTotal = request.getItems().stream()
                            .filter(item -> item.getActualQty() != null)
                            .mapToDouble(item -> item.getActualQty().doubleValue())
                            .sum();

                    // 查询同一总磅单下的其他入库单的实际入库总量（吨）
                    List<Warehousing> otherWarehousings = warehousingMapper.selectList(
                            new LambdaQueryWrapper<Warehousing>()
                                    .eq(Warehousing::getWeighingSlipNo, warehousing.getWeighingSlipNo())
                                    .ne(Warehousing::getWarehousingId, request.getWarehousingId())
                    );

                    double otherActualTotal = 0.0;
                    for (Warehousing other : otherWarehousings) {
                        List<WarehousingWasteItem> otherItems = warehousingWasteItemMapper.selectList(
                                new LambdaQueryWrapper<WarehousingWasteItem>()
                                        .eq(WarehousingWasteItem::getWarehousingId, other.getWarehousingId())
                        );
                        otherActualTotal += otherItems.stream()
                                .filter(item -> item.getActualQty() != null)
                                .mapToDouble(item -> item.getActualQty().doubleValue())
                                .sum();
                    }

                    // 计算总实际入库量（吨）
                    double totalActualQty = currentActualTotal + otherActualTotal;

                    // 总磅净重（kg）转换为吨
                    double netWeightInTon = weighingSlip.getNetWeight().doubleValue() / 1000.0;

                    // 计算差异（吨）
                    double diff = Math.abs(totalActualQty - netWeightInTon);

                    // 如果差异超过总磅净重的5%或超过0.1吨，记录警告日志
                    // 注意：这里不阻止更新，因为业务规则允许差异，但需要记录
                    if (diff > 0.1 || (netWeightInTon > 0 && diff / netWeightInTon > 0.05)) {
                        log.warn("入库单更新时发现实际入库总量与总磅净重存在较大差异。总磅单号：{}，总磅净重：{}吨，实际入库总量：{}吨，差异：{}吨",
                                warehousing.getWeighingSlipNo(), netWeightInTon, totalActualQty, diff);
                    }
                }
            }
        }

        // 返回更新后的详情
        WarehousingDetailResponse newDetail = getWarehousingDetail(request.getWarehousingId());

        // 记录数据变更日志
        try {
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            if (oldDetail != null) {
                log.info("准备记录数据变更日志 - warehousingId={}, warehousingNo={}", 
                        request.getWarehousingId(), newDetail.getWarehousingNo());
                logRecordService.recordDataChangeLog("入库单管理", "WAREHOUSING",
                        String.valueOf(request.getWarehousingId()),
                        "更新",
                        "更新入库单：入库单号=" + newDetail.getWarehousingNo(),
                        oldDetail, newDetail, currentUserId, null, true, null);
                log.info("数据变更日志记录完成 - warehousingId={}, warehousingNo={}", 
                        request.getWarehousingId(), newDetail.getWarehousingNo());
            } else {
                log.warn("oldDetail为空，无法记录数据变更日志 - warehousingId={}", request.getWarehousingId());
            }
        } catch (Exception e) {
            log.error("记录入库单更新数据变更日志失败 - warehousingId={}", request.getWarehousingId(), e);
            // 数据变更日志记录失败不影响主业务流程
        }

        return newDetail;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehousing(Integer warehousingId) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        
        // 查询入库单
        Warehousing warehousing = warehousingMapper.selectByWarehousingId(warehousingId);
        if (warehousing == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "入库单不存在");
        }

        // 检查是否锁定
        if (Boolean.TRUE.equals(warehousing.getLocked())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "该入库单已锁定，无法删除");
        }

        // 检查是否已结算（已结算的入库单不能删除）
        if (warehousing.getAuditTime() != null) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "该入库单已结算，无法删除");
        }

        // 保存删除前的数据用于日志记录
        WarehousingDetailResponse beforeDelete = null;
        try {
            beforeDelete = getWarehousingDetail(warehousingId);
        } catch (Exception e) {
            log.warn("获取入库单删除前数据失败：warehousingId={}", warehousingId, e);
        }

        // 删除关联的危废明细
        LambdaQueryWrapper<WarehousingWasteItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WarehousingWasteItem::getWarehousingId, warehousingId);
        int delItems = warehousingWasteItemMapper.delete(deleteWrapper);
        log.info("删除入库单关联危废明细：warehousingId={}, deletedCount={}", warehousingId, delItems);

        // 删除入库单主记录
        int rows = warehousingMapper.deleteById(warehousingId);
        if (rows == 0) {
            log.warn("删除入库单主记录失败，warehousingId={}", warehousingId);
        }

        log.info("删除入库单成功：warehousingId={}, warehousingNo={}, operator={}",
                warehousingId, warehousing.getWarehousingNo(), currentUserId);

        // 记录数据变更日志
        try {
            log.info("准备记录删除入库单的数据变更日志 - warehousingId={}, warehousingNo={}", 
                    warehousingId, warehousing.getWarehousingNo());
            logRecordService.recordDataChangeLog("入库单管理", "WAREHOUSING",
                    String.valueOf(warehousingId),
                    "删除",
                    "删除入库单：入库单号=" + warehousing.getWarehousingNo() + "，运输单号=" + warehousing.getDispatchCode(),
                    beforeDelete, null, currentUserId, null, true, null);
            log.info("删除入库单的数据变更日志记录完成 - warehousingId={}, warehousingNo={}", 
                    warehousingId, warehousing.getWarehousingNo());
        } catch (Exception e) {
            log.error("记录删除入库单数据变更日志失败 - warehousingId={}", warehousingId, e);
            // 数据变更日志记录失败不影响主业务流程
        }
    }

    @Override
    public boolean updateWarehousingStatus(Integer warehousingId, String status, Integer operatorId) {
        // 校验状态值合法性
        try {
            com.erp.common.enums.WarehousingStatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "无效的状态值: " + status);
        }

        // 获取旧状态用于日志
        Warehousing oldWarehousing = warehousingMapper.selectByWarehousingId(warehousingId);
        if (oldWarehousing == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "入库单不存在");
        }

        // 更新状态
        int result = warehousingMapper.updateStatus(warehousingId, status, operatorId);

        // 记录状态变更日志
        if (result > 0) {
            logRecordService.recordOperationLog("入库单管理", "状态变更",
                String.format("入库单状态变更：%s → %s", oldWarehousing.getStatus(), status),
                operatorId, null, true, null);
        }

        return result > 0;
    }

    @Override
    public int batchUpdateWarehousingStatus(List<Integer> warehousingIds, String status, Integer operatorId) {
        // 校验状态值合法性
        try {
            com.erp.common.enums.WarehousingStatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "无效的状态值: " + status);
        }

        if (warehousingIds == null || warehousingIds.isEmpty()) {
            return 0;
        }

        // 批量更新状态
        int result = warehousingMapper.batchUpdateStatus(warehousingIds, status, operatorId);

        // 记录批量状态变更日志
        if (result > 0) {
            logRecordService.recordOperationLog("入库单管理", "批量状态变更",
                String.format("批量更新入库单状态为: %s，数量: %d", status, result),
                operatorId, null, true, null);
        }

        return result;
    }

    @Override
    public Map<String, Long> getStatusStatistics() {
        List<java.util.Map<String, Object>> stats = warehousingMapper.countByStatus();
        Map<String, Long> result = new java.util.HashMap<>();

        // 初始化所有状态为0
        for (String statusValue : com.erp.common.enums.WarehousingStatusEnum.getAllValues()) {
            result.put(statusValue, 0L);
        }

        // 填充实际统计数据
        for (java.util.Map<String, Object> stat : stats) {
            String status = (String) stat.get("status");
            Long count = ((Number) stat.get("count")).longValue();
            result.put(status, count);
        }

        return result;
    }

    @Override
    public List<WarehousingWithSettlementVO> getWarehousingWithChainByContract(String contractCode) {
        if (StrUtil.isBlank(contractCode)) {
            return new ArrayList<>();
        }
        return warehousingMapper.selectWarehousingWithChainByContract(contractCode);
    }
}

