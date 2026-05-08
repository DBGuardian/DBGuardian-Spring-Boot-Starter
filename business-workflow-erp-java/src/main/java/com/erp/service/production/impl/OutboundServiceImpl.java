package com.erp.service.production.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.production.dto.AuditOutboundRequest;
import com.erp.controller.production.dto.CreateOutboundRequest;
import com.erp.controller.production.dto.OutboundDetailResponse;
import com.erp.controller.production.dto.OutboundListResponse;
import com.erp.controller.production.dto.OutboundPageRequest;
import com.erp.entity.customer.Customer;
import com.erp.entity.production.Outbound;
import com.erp.entity.production.OutboundWasteItem;
import com.erp.entity.production.Stock;
import com.erp.entity.system.Employee;
import com.erp.mapper.customer.CustomerMapper;
import com.erp.mapper.production.OutboundMapper;
import com.erp.mapper.production.OutboundWasteItemMapper;
import com.erp.mapper.production.StockMapper;
import com.erp.mapper.production.WarehousingWasteItemMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.service.production.OutboundService;
import com.erp.service.system.ILogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 出库单服务实现
 */
@Slf4j
@Service
public class OutboundServiceImpl implements OutboundService {

    @Autowired
    private OutboundMapper outboundMapper;

    @Autowired
    private OutboundWasteItemMapper outboundWasteItemMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private WarehousingWasteItemMapper warehousingWasteItemMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ILogRecordService logRecordService;

    private static final String OUTBOUND_NO_PREFIX = "CKD";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─── 生成出库单号 ──────────────────────────────────────────────────────────────

    private String generateOutboundNo() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String prefix = OUTBOUND_NO_PREFIX + "-" + datePart + "-";
        String maxCode = outboundMapper.selectMaxOutboundNoByPrefix(prefix);
        int nextSeq = 1;
        if (StrUtil.isNotBlank(maxCode) && maxCode.length() > prefix.length()) {
            try {
                nextSeq = Integer.parseInt(maxCode.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                log.warn("解析出库单号序列失败: {}", maxCode, e);
            }
        }
        String outboundNo = prefix + String.format("%04d", nextSeq);
        int retry = 0;
        while (outboundMapper.countByOutboundNo(outboundNo) > 0 && retry < 20) {
            outboundNo = prefix + String.format("%04d", ++nextSeq);
            retry++;
        }
        if (retry >= 20) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成出库单号失败，请重试");
        }
        return outboundNo;
    }

    // ─── 创建出库单 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundDetailResponse createOutbound(CreateOutboundRequest request) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "用户未登录");
        }

        // 校验经办人存在
        Employee handler = employeeMapper.selectById(request.getHandlerId());
        if (handler == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "经办人不存在");
        }

        // 创建出库单主表
        Outbound outbound = new Outbound();
        outbound.setOutboundNo(generateOutboundNo());
        outbound.setOutboundType(request.getOutboundType());
        outbound.setOutboundTime(LocalDateTime.parse(request.getOutboundTime(), DT_FORMATTER));
        outbound.setHandlerId(request.getHandlerId());
        outbound.setContractCode(normalize(request.getContractCode()));
        outbound.setCustomerId(request.getCustomerId());
        outbound.setDestinationType(normalize(request.getDestinationType()));
        outbound.setDestinationName(normalize(request.getDestinationName()));
        outbound.setRemark(normalize(request.getRemark()));
        outbound.setStatus("待审核");
        outbound.setLocked(false);
        outbound.setCreateTime(LocalDateTime.now());
        outbound.setVersion(0);

        outboundMapper.insert(outbound);
        log.info("出库单创建成功：outboundNo={}", outbound.getOutboundNo());

        // 创建危废明细
        saveItems(outbound.getOutboundId(), request.getItems());

        // 返回创建后的详情
        OutboundDetailResponse newDetail = getOutboundDetail(outbound.getOutboundId());

        // 记录数据变更日志
        try {
            logRecordService.recordDataChangeLog("出库单管理", "OUTBOUND",
                    String.valueOf(outbound.getOutboundId()),
                    "新增",
                    "创建出库单：出库单号=" + outbound.getOutboundNo(),
                    null, newDetail, currentUserId, null, true, null);
        } catch (Exception e) {
            log.error("记录出库单创建数据变更日志失败", e);
        }

        return newDetail;
    }

    // ─── 更新出库单 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundDetailResponse updateOutbound(CreateOutboundRequest request) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "用户未登录");
        }

        if (request.getOutboundId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "出库单编号不能为空");
        }

        Outbound outbound = outboundMapper.selectByOutboundId(request.getOutboundId());
        if (outbound == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "出库单不存在");
        }
        if (Boolean.TRUE.equals(outbound.getLocked())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "已锁定的出库单不能修改");
        }
        if (!"待审核".equals(outbound.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "仅待审核状态的出库单可以修改，当前状态：" + outbound.getStatus());
        }

        // 保存旧数据用于日志记录
        OutboundDetailResponse oldDetail = null;
        try {
            oldDetail = getOutboundDetail(request.getOutboundId());
        } catch (Exception e) {
            log.warn("获取出库单旧数据失败，无法记录数据变更日志：outboundId={}", request.getOutboundId(), e);
        }

        // 更新主表字段
        if (StrUtil.isNotBlank(request.getOutboundType())) outbound.setOutboundType(request.getOutboundType());
        if (StrUtil.isNotBlank(request.getOutboundTime())) {
            outbound.setOutboundTime(LocalDateTime.parse(request.getOutboundTime(), DT_FORMATTER));
        }
        if (request.getHandlerId() != null) outbound.setHandlerId(request.getHandlerId());
        outbound.setContractCode(normalize(request.getContractCode()));
        outbound.setCustomerId(request.getCustomerId());
        outbound.setDestinationType(normalize(request.getDestinationType()));
        outbound.setDestinationName(normalize(request.getDestinationName()));
        if (request.getRemark() != null) outbound.setRemark(request.getRemark());
        outbound.setUpdateTime(LocalDateTime.now());

        int rows = outboundMapper.updateById(outbound);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新出库单失败，记录已被其他用户修改");
        }

        // 更新明细：删除旧的再重建
        if (!CollectionUtils.isEmpty(request.getItems())) {
            // 收集请求中有 itemId 的
            List<Integer> requestItemIds = request.getItems().stream()
                    .map(CreateOutboundRequest.OutboundItemRequest::getItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 查询现有明细
            List<OutboundWasteItem> existingItems = outboundWasteItemMapper.selectByOutboundId(request.getOutboundId());
            List<Integer> existingItemIds = existingItems.stream()
                    .map(OutboundWasteItem::getItemId)
                    .collect(Collectors.toList());

            // 删除不在请求中的旧明细
            existingItemIds.stream()
                    .filter(id -> !requestItemIds.contains(id))
                    .forEach(id -> outboundWasteItemMapper.deleteById(id));

            // 更新或插入明细
            for (CreateOutboundRequest.OutboundItemRequest itemReq : request.getItems()) {
                OutboundWasteItem item = buildItem(request.getOutboundId(), itemReq);
                if (itemReq.getItemId() != null && existingItemIds.contains(itemReq.getItemId())) {
                    item.setItemId(itemReq.getItemId());
                    outboundWasteItemMapper.updateById(item);
                } else {
                    outboundWasteItemMapper.insert(item);
                }
            }
        }

        // 返回更新后的详情
        OutboundDetailResponse newDetail = getOutboundDetail(request.getOutboundId());

        // 记录数据变更日志
        try {
            if (oldDetail != null) {
                logRecordService.recordDataChangeLog("出库单管理", "OUTBOUND",
                        String.valueOf(request.getOutboundId()),
                        "更新",
                        "更新出库单：出库单号=" + outbound.getOutboundNo(),
                        oldDetail, newDetail, currentUserId, null, true, null);
            }
        } catch (Exception e) {
            log.error("记录出库单更新数据变更日志失败", e);
        }

        return newDetail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOutbound(AuditOutboundRequest request) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "用户未登录");
        }

        String auditResult = normalize(request.getAuditResult());
        String auditOpinion = normalize(request.getAuditOpinion());
        if (!"通过".equals(auditResult) && !"拒绝".equals(auditResult)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "审核结果仅支持：通过/拒绝");
        }
        if (StrUtil.isBlank(auditOpinion)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "审核意见不能为空");
        }

        Outbound outbound = outboundMapper.selectByOutboundNo(request.getOutboundNo());
        if (outbound == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "出库单不存在");
        }
        if (Boolean.TRUE.equals(outbound.getLocked())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "该出库单已锁定，无法审核");
        }
        if (!"待审核".equals(outbound.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "当前状态不是待审核，无法重复审核");
        }

        List<OutboundWasteItem> items = outboundWasteItemMapper.selectByOutboundId(outbound.getOutboundId());
        validateAuditRequiredFields(outbound, items);

        LocalDateTime now = LocalDateTime.now();
        outbound.setAuditorId(currentUserId);
        outbound.setAuditTime(now);
        outbound.setUpdateTime(now);

        if ("拒绝".equals(auditResult)) {
            outbound.setStatus("已作废");
            int rows = outboundMapper.updateById(outbound);
            if (rows == 0) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "审核失败，出库单已被其他用户修改");
            }
            return;
        }

        for (OutboundWasteItem item : items) {
            Stock stock = resolveStockForAudit(item);
            item.setStockId(stock.getStockId());

            BigDecimal beforeStockQty = defaultZero(stock.getCurrentWeight());
            BigDecimal outboundQty = defaultZero(item.getOutboundQty());
            BigDecimal afterStockQty = beforeStockQty.subtract(outboundQty);
            if (afterStockQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                        "明细“" + item.getWasteName() + "”扣减后库存重量不能为负数");
            }

            BigDecimal beforeAuxQty = defaultZero(integerToDecimal(stock.getAuxQuantity()));
            BigDecimal outboundAuxQty = defaultZero(item.getOutboundAuxQty());
            BigDecimal afterAuxQty = beforeAuxQty.subtract(outboundAuxQty);
            if (afterAuxQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                        "明细“" + item.getWasteName() + "”扣减后辅助数量不能为负数");
            }

            item.setBeforeStockQty(beforeStockQty);
            item.setAfterStockQty(afterStockQty);
            item.setBeforeAuxQty(beforeAuxQty);
            item.setAfterAuxQty(afterAuxQty);
            item.setUpdateTime(now);
            outboundWasteItemMapper.updateById(item);

            stock.setCurrentWeight(afterStockQty);
            stock.setAuxQuantity(afterAuxQty.intValue());
            stock.setUpdateTime(now);
            stockMapper.updateById(stock);
        }

        outbound.setStatus("已审核");
        int rows = outboundMapper.updateById(outbound);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "审核失败，出库单已被其他用户修改");
        }
    }

    // ─── 小工具方法 ─────────────────────────────────────────────────────────────

    private void validateAuditRequiredFields(Outbound outbound, List<OutboundWasteItem> items) {
        if (outbound.getOutboundTime() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "出库时间不能为空");
        }
        if (outbound.getHandlerId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "经办人不能为空");
        }
        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "危废明细不能为空");
        }

        for (int i = 0; i < items.size(); i++) {
            OutboundWasteItem item = items.get(i);
            String prefix = "第" + (i + 1) + "条危废明细";
            if (item.getStockId() == null && item.getWarehousingItemId() == null
                    && item.getHazardousWasteItemId() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                        prefix + "缺少库存关联信息，无法审核");
            }
            if (StrUtil.isBlank(item.getWasteName())) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), prefix + "的废物名称不能为空");
            }
            if (StrUtil.isBlank(item.getWasteCode())) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), prefix + "的废物代码不能为空");
            }
            if (item.getOutboundQty() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), prefix + "的出库数量不能为空");
            }
            if (item.getOutboundQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), prefix + "的出库数量必须大于0");
            }
            if (Boolean.TRUE.equals(item.getEnableAuxiliaryAccounting()) && item.getOutboundAuxQty() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), prefix + "启用了辅助核算，出库辅助数量不能为空");
            }
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Stock resolveStockForAudit(OutboundWasteItem item) {
        if (item.getStockId() != null) {
            Stock stock = stockMapper.selectById(item.getStockId());
            if (stock != null) {
                return stock;
            }
        }

        String location = normalize(item.getLocation());
        Integer hazardousWasteItemId = item.getHazardousWasteItemId();
        if (hazardousWasteItemId == null && item.getWarehousingItemId() != null) {
            com.erp.entity.production.WarehousingWasteItem warehousingWasteItem = warehousingWasteItemMapper.selectById(item.getWarehousingItemId());
            if (warehousingWasteItem != null) {
                hazardousWasteItemId = warehousingWasteItem.getHazardousWasteItemId();
            }
        }

        List<Stock> matchedStocks = stockMapper.selectAuditMatchStocks(
                hazardousWasteItemId,
                item.getWasteCode(),
                item.getWasteName(),
                location
        );

        if (CollectionUtils.isEmpty(matchedStocks)) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(),
                    buildAuditStockMatchMessage(item, "未匹配到库存记录"));
        }
        if (matchedStocks.size() > 1) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    buildAuditStockMatchMessage(item, "匹配到多条库存记录，无法自动确定库存"));
        }
        return matchedStocks.get(0);
    }

    private String buildAuditStockMatchMessage(OutboundWasteItem item, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("明细“").append(item.getWasteName()).append("”").append(reason)
                .append("（废物代码：").append(item.getWasteCode());
        if (item.getHazardousWasteItemId() != null) {
            sb.append("，危废条目编号：").append(item.getHazardousWasteItemId());
        }
        if (StrUtil.isNotBlank(item.getLocation())) {
            sb.append("，库位：").append(item.getLocation());
        }
        sb.append("）");
        return sb.toString();
    }

    private BigDecimal integerToDecimal(Integer value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value.longValue());
    }

    private void saveItems(Integer outboundId, List<CreateOutboundRequest.OutboundItemRequest> itemRequests) {
        if (CollectionUtils.isEmpty(itemRequests)) return;
        for (CreateOutboundRequest.OutboundItemRequest req : itemRequests) {
            OutboundWasteItem item = buildItem(outboundId, req);
            outboundWasteItemMapper.insert(item);
        }
    }

    private OutboundWasteItem buildItem(Integer outboundId, CreateOutboundRequest.OutboundItemRequest req) {
        OutboundWasteItem item = new OutboundWasteItem();
        item.setOutboundId(outboundId);
        item.setStockId(req.getStockId());
        item.setWarehousingItemId(req.getWarehousingItemId());
        item.setHazardousWasteItemId(req.getHazardousWasteItemId());
        item.setWasteName(req.getWasteName());
        item.setWasteCode(req.getWasteCode());
        item.setHazardFeature(normalize(req.getHazardFeature()));
        item.setForm(normalize(req.getForm()));
        item.setLocation(normalize(req.getLocation()));
        item.setMeasureUnit(StrUtil.isNotBlank(req.getMeasureUnit()) ? req.getMeasureUnit() : "吨");
        Boolean enableAux = Boolean.TRUE.equals(req.getEnableAuxiliaryAccounting());
        item.setEnableAuxiliaryAccounting(enableAux);
        if (enableAux) {
            item.setAuxUnit(req.getAuxUnit());
            item.setAuxPerBase(req.getAuxPerBase());
            item.setOutboundAuxQty(req.getOutboundAuxQty());
        } else {
            item.setAuxUnit(null);
            item.setAuxPerBase(null);
            item.setOutboundAuxQty(null);
        }
        item.setOutboundQty(req.getOutboundQty());
        item.setValuableWeight(req.getValuableWeight());
        item.setValuelessWeight(req.getValuelessWeight());
        item.setOutboundReason(normalize(req.getOutboundReason()));
        item.setCreateTime(LocalDateTime.now());
        item.setVersion(0);
        return item;
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ─── 查询出库单详情 ────────────────────────────────────────────────────────

    @Override
    public OutboundDetailResponse getOutboundDetail(Integer outboundId) {
        if (outboundId == null || outboundId <= 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "出库单编号无效");
        }

        OutboundDetailResponse response = outboundMapper.selectDetailByOutboundId(outboundId);
        if (response == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "出库单不存在");
        }
        if (response.getItems() == null) {
            response.setItems(new ArrayList<>());
        }
        return response;
    }

    @Override
    public OutboundDetailResponse getOutboundDetail(Integer outboundId, String outboundNo) {
        if (outboundId == null && StrUtil.isBlank(outboundNo)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "出库单编号或出库单号至少提供一个");
        }

        if (outboundId == null) {
            Outbound outbound = outboundMapper.selectByOutboundNo(outboundNo.trim());
            if (outbound == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "出库单不存在");
            }
            outboundId = outbound.getOutboundId();
        }

        return getOutboundDetail(outboundId);
    }

    // ─── 分页查询出库单列表 ────────────────────────────────────────────────────

    @Override
    public OutboundListResponse getOutboundPage(OutboundPageRequest request) {
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 20;

        Page<OutboundListResponse.OutboundPageResponse> pageObj = new Page<>(page, size);
        IPage<OutboundListResponse.OutboundPageResponse> result = outboundMapper.selectOutboundPage(
                pageObj,
                normalize(request.getKeyword()),
                normalize(request.getOutboundType()),
                normalize(request.getStatus()),
                normalize(request.getDestinationType()),
                normalize(request.getStartTime()),
                normalize(request.getEndTime()),
                normalize(request.getOrderBy()),
                normalize(request.getOrderDirection())
        );

        OutboundListResponse resp = new OutboundListResponse();
        resp.setStats(buildStats());
        resp.setRecords(result.getRecords());
        resp.setTotal(result.getTotal());
        resp.setCurrent((int) result.getCurrent());
        resp.setSize((int) result.getSize());
        return resp;
    }

    private List<OutboundListResponse.OutboundStat> buildStats() {
        try {
            Map<String, Long> counts = getStatusCounts();
            return Arrays.asList(
                    makeStat("待审核", counts.getOrDefault("待审核", 0L), "warning"),
                    makeStat("已审核", counts.getOrDefault("已审核", 0L), "success"),
                    makeStat("已作废", counts.getOrDefault("已作废", 0L), "info"),
                    makeStat("已锁定", counts.getOrDefault("已锁定", 0L), "danger"),
                    makeStat("总数", counts.values().stream().mapToLong(Long::longValue).sum(), "info")
            );
        } catch (Exception e) {
            log.error("构建出库单统计信息失败", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Long> getStatusCounts() {
        List<Map<String, Object>> rows = outboundMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Outbound>()
                        .select("`状态` AS status", "COUNT(*) AS cnt")
                        .groupBy("`状态`")
        );
        Map<String, Long> result = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            Long cnt = row.get("cnt") instanceof Number ? ((Number) row.get("cnt")).longValue() : 0L;
            result.put(status, cnt);
        }
        return result;
    }

    private OutboundListResponse.OutboundStat makeStat(String label, long value, String color) {
        OutboundListResponse.OutboundStat s = new OutboundListResponse.OutboundStat();
        s.setLabel(label);
        s.setValue(String.valueOf(value));
        s.setColor(color);
        return s;
    }
}
