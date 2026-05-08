package com.erp.service.production.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.production.dto.CreateWeighingSlipRequest;
import com.erp.controller.production.dto.UpdateWeighingSlipRequest;
import com.erp.controller.production.dto.WeighingSlipInfoResponse;
import com.erp.controller.production.dto.WeighingSlipListResponse;
import com.erp.controller.production.dto.WeighingSlipPageRequest;
import com.erp.controller.production.dto.WeighingSlipPageResponse;
import com.erp.controller.production.dto.WeighingSlipStat;
import com.erp.entity.common.File;
import com.erp.entity.production.WeighingSlip;
import com.erp.entity.production.WeighingSlipDispatch;
import com.erp.entity.system.Employee;
import com.erp.entity.transport.DispatchOrder;
import com.erp.entity.transport.DispatchOrderNotice;
import com.erp.mapper.common.FileMapper;
import com.erp.mapper.production.PickupNoticeItemMapper;
import com.erp.mapper.production.PickupNoticeMapper;
import com.erp.mapper.production.WeighingSlipDispatchMapper;
import com.erp.mapper.production.WeighingSlipMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.HazardousWasteItemMapper;
import com.erp.mapper.transport.DispatchOrderMapper;
import com.erp.mapper.transport.DispatchOrderNoticeMapper;
import com.erp.entity.production.PickupNotice;
import com.erp.entity.production.PickupNoticeItem;
import com.erp.entity.customer.Customer;
import com.erp.entity.system.HazardousWasteItem;
import com.erp.mapper.customer.CustomerMapper;
import com.erp.controller.production.dto.PickupNoticeForWarehousing;
import com.erp.service.production.WeighingSlipService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 总磅单服务实现
 */
@Slf4j
@Service
public class WeighingSlipServiceImpl implements WeighingSlipService {

    @Autowired
    private WeighingSlipMapper weighingSlipMapper;

    @Autowired
    private WeighingSlipDispatchMapper weighingSlipDispatchMapper;

    @Autowired
    private DispatchOrderMapper dispatchOrderMapper;

    @Autowired
    private DispatchOrderNoticeMapper dispatchOrderNoticeMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private PickupNoticeMapper pickupNoticeMapper;

    @Autowired
    private PickupNoticeItemMapper pickupNoticeItemMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private FileMapper fileMapper;

    private static final String WEIGHING_SLIP_BUSINESS_TYPE = "WEIGHING_SLIP";
    private static final String WEIGHING_SLIP_NO_PREFIX = "ZBD-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成总磅单号
     * 规则：ZBD-YYYYMMDD-4位序号
     */
    private String generateWeighingSlipNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = WEIGHING_SLIP_NO_PREFIX + datePart + "-";

        // 查询当日当前最大编号
        String maxWeighingSlipNo = weighingSlipMapper.selectMaxWeighingSlipNoByPrefix(prefix);

        int nextSeq = 1;
        if (StrUtil.isNotBlank(maxWeighingSlipNo) && maxWeighingSlipNo.length() > prefix.length()) {
            // 截取末尾4位序号并递增
            try {
                String seqPart = maxWeighingSlipNo.substring(maxWeighingSlipNo.length() - 4);
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (Exception e) {
                log.warn("解析最大总磅单号序号失败：{}", maxWeighingSlipNo, e);
            }
        }

        String weighingSlipNo = prefix + String.format("%04d", nextSeq);

        // 若并发导致重复，则继续自增直到唯一或达到重试上限
        int retryCount = 0;
        while (weighingSlipMapper.countByWeighingSlipNo(weighingSlipNo) > 0 && retryCount < 20) {
            nextSeq++;
            weighingSlipNo = prefix + String.format("%04d", nextSeq);
            retryCount++;
        }

        if (retryCount >= 20) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成总磅单号失败，请重试");
        }

        log.debug("生成总磅单号：{}", weighingSlipNo);
        return weighingSlipNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeighingSlipInfoResponse createWeighingSlip(CreateWeighingSlipRequest request) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "用户未登录");
        }

        // 验证序号唯一性（如果提供）
        if (StrUtil.isNotBlank(request.getSequence())) {
            int count = weighingSlipMapper.countBySequence(request.getSequence().trim());
            if (count > 0) {
                throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "序号已存在，请使用其他序号");
            }
        }

        // 验证关联的运输单号
        if (request.getDispatchCodes() == null || request.getDispatchCodes().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "至少需要关联一个运输单号");
        }

        // 验证运输单是否存在且未关联总磅单
        for (String dispatchCode : request.getDispatchCodes()) {
            DispatchOrder dispatchOrder = dispatchOrderMapper.selectOne(
                    new LambdaQueryWrapper<DispatchOrder>()
                            .eq(DispatchOrder::getDispatchCode, dispatchCode)
            );
            if (dispatchOrder == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "运输单不存在：" + dispatchCode);
            }

            // 检查运输单是否已关联总磅单
            int relationCount = weighingSlipDispatchMapper.countByDispatchCode(dispatchCode);
            if (relationCount > 0) {
                throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), 
                        "运输单已关联总磅单：" + dispatchCode);
            }
        }

        // 生成总磅单号
        String weighingSlipNo = generateWeighingSlipNo();

        // 构建总磅单实体
        WeighingSlip weighingSlip = new WeighingSlip();
        weighingSlip.setWeighingSlipNo(weighingSlipNo);
        weighingSlip.setSequence(StrUtil.isNotBlank(request.getSequence()) ? request.getSequence().trim() : null);
        
        // 转换日期
        if (StrUtil.isNotBlank(request.getDate())) {
            weighingSlip.setWeighingDate(LocalDate.parse(request.getDate(), DATE_FORMATTER));
        } else {
            weighingSlip.setWeighingDate(LocalDate.now());
        }

        // 转换时间
        if (StrUtil.isNotBlank(request.getFirstWeighTime())) {
            weighingSlip.setFirstWeighTime(LocalDateTime.parse(request.getFirstWeighTime(), DATE_TIME_FORMATTER));
        }
        if (StrUtil.isNotBlank(request.getSecondWeighTime())) {
            weighingSlip.setSecondWeighTime(LocalDateTime.parse(request.getSecondWeighTime(), DATE_TIME_FORMATTER));
        }

        weighingSlip.setPlateNo(request.getPlateNo());
        
        // 转换重量（前端传的是kg，数据库也是kg）
        if (request.getGrossWeight() != null) {
            weighingSlip.setGrossWeight(BigDecimal.valueOf(request.getGrossWeight()));
        }
        if (request.getTareWeight() != null) {
            weighingSlip.setTareWeight(BigDecimal.valueOf(request.getTareWeight()));
        }
        if (request.getNetWeight() != null) {
            weighingSlip.setNetWeight(BigDecimal.valueOf(request.getNetWeight()));
        } else if (request.getGrossWeight() != null && request.getTareWeight() != null) {
            // 自动计算净重
            BigDecimal netWeight = BigDecimal.valueOf(request.getGrossWeight())
                    .subtract(BigDecimal.valueOf(request.getTareWeight()));
            weighingSlip.setNetWeight(netWeight);
        }

        // 处理总磅单照片引用：优先作为 FILE 表的 fileId 进行关联，兼容历史 URL/路径格式
        if (StrUtil.isNotBlank(request.getPhotoUrl())) {
            String photoRef = request.getPhotoUrl().trim();
            // 如果是纯数字，视为 FILE.文件编号
            if (photoRef.matches("\\d+")) {
                weighingSlip.setPhotoUrl(photoRef);
            } else {
                // 兼容历史：保存相对路径（与更新逻辑保持一致）
                String normalizedPath = normalizePhotoPath(photoRef, weighingSlipNo);
                weighingSlip.setPhotoUrl(normalizedPath);
            }
        }

        weighingSlip.setStatus("待细分");
        weighingSlip.setCreatorId(currentUserId);
        weighingSlip.setRemark(request.getRemark());

        // 保存总磅单
        // 确保创建时更新时间为空
        weighingSlip.setUpdateTime(null);
        weighingSlipMapper.insert(weighingSlip);

        // 如果照片引用为 FILE.文件编号，则建立 FILE 与总磅单的业务关联
        bindPhotoFileToWeighingSlip(weighingSlip);

        // 保存总磅单-运输单关联关系
        for (String dispatchCode : request.getDispatchCodes()) {
            WeighingSlipDispatch relation = new WeighingSlipDispatch();
            relation.setWeighingSlipId(weighingSlip.getWeighingSlipId());
            relation.setDispatchCode(dispatchCode);
            weighingSlipDispatchMapper.insert(relation);
        }

        // 更新关联的运输单状态为"已到达"，并更新运输单明细的实际到达时间
        LocalDateTime currentTime = LocalDateTime.now();
        for (String dispatchCode : request.getDispatchCodes()) {
            try {
                // 更新运输单状态为"已到达"
                DispatchOrder dispatchOrder = dispatchOrderMapper.selectByDispatchCode(dispatchCode);
                if (dispatchOrder != null && !"已到达".equals(dispatchOrder.getStatus())
                        && !"已完成".equals(dispatchOrder.getStatus())
                        && !"已取消".equals(dispatchOrder.getStatus())) {
                    dispatchOrder.setStatus("已到达");
                    dispatchOrder.setUpdateTime(currentTime);
                    int rows = dispatchOrderMapper.updateById(dispatchOrder);
                    if (rows == 0) {
                        log.warn("更新运输单状态失败（乐观锁冲突），dispatchCode={}", dispatchCode);
                    }
                    log.info("运输单状态已更新为已到达：dispatchCode={}", dispatchCode);
                }

                // 更新运输单明细的实际到达时间
                DispatchOrderNotice notice = dispatchOrderNoticeMapper.selectByDispatchCode(dispatchCode);
                if (notice != null) {
                    notice.setArriveAt(currentTime);
                    notice.setUpdateTime(currentTime);
                    int noticeRows = dispatchOrderNoticeMapper.updateById(notice);
                    if (noticeRows == 0) {
                        log.warn("更新运输单明细到达时间失败（乐观锁冲突），dispatchCode={}", dispatchCode);
                    }
                    log.info("运输单明细实际到达时间已更新：dispatchCode={}, arriveAt={}", dispatchCode, currentTime);
                } else {
                    // 如果运输单明细不存在，创建一条记录
                    notice = new DispatchOrderNotice();
                    notice.setDispatchCode(dispatchCode);
                    // 从运输单获取收运通知单号
                    if (dispatchOrder != null && StrUtil.isNotBlank(dispatchOrder.getNoticeCode())) {
                        notice.setNoticeCode(dispatchOrder.getNoticeCode());
                    }
                    notice.setArriveAt(currentTime);
                    notice.setCreateTime(currentTime);
                    notice.setUpdateTime(null);
                    dispatchOrderNoticeMapper.insert(notice);
                    log.info("创建运输单明细并设置实际到达时间：dispatchCode={}, arriveAt={}", dispatchCode, currentTime);
                }
            } catch (Exception e) {
                log.error("更新运输单状态或实际到达时间失败：dispatchCode={}", dispatchCode, e);
                // 继续处理其他运输单，不中断整个流程
            }
        }

        log.info("创建总磅单成功：总磅单号={}, 关联运输单数={}", weighingSlipNo, request.getDispatchCodes().size());

        // 构建返回结果
        WeighingSlipInfoResponse response = buildWeighingSlipInfoResponse(weighingSlip);

        // 记录数据变更日志
        try {
            logRecordService.recordDataChangeLog("生产管理", "WEIGHING_SLIP",
                    String.valueOf(weighingSlip.getWeighingSlipId()),
                    "新增",
                    "创建总磅单：总磅单号=" + weighingSlipNo + "，序号=" + weighingSlip.getSequence(),
                    null, response, currentUserId, null, true, null);
        } catch (Exception logEx) {
            log.warn("记录总磅单创建数据变更日志失败，weighingSlipNo={}", weighingSlipNo, logEx);
        }

        return response;
    }

    @Override
    public WeighingSlipInfoResponse getWeighingSlipInfo(String weighingSlipNo) {
        // 使用自定义查询方法，避免 MyBatis-Plus 自动查询包含不存在的更新时间字段
        WeighingSlip weighingSlip = weighingSlipMapper.selectByWeighingSlipNo(weighingSlipNo);

        if (weighingSlip == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "总磅单不存在：" + weighingSlipNo);
        }

        return buildWeighingSlipInfoResponse(weighingSlip);
    }

    /**
     * 构建总磅单信息响应
     */
    private WeighingSlipInfoResponse buildWeighingSlipInfoResponse(WeighingSlip weighingSlip) {
        WeighingSlipInfoResponse response = new WeighingSlipInfoResponse();
        response.setWeighingSlipId(weighingSlip.getWeighingSlipId());
        response.setWeighingSlipNo(weighingSlip.getWeighingSlipNo());
        response.setSequence(weighingSlip.getSequence());
        
        if (weighingSlip.getWeighingDate() != null) {
            response.setDate(weighingSlip.getWeighingDate().format(DATE_FORMATTER));
        }
        
        if (weighingSlip.getFirstWeighTime() != null) {
            response.setFirstWeighTime(weighingSlip.getFirstWeighTime().format(DATE_TIME_FORMATTER));
        }
        
        if (weighingSlip.getSecondWeighTime() != null) {
            response.setSecondWeighTime(weighingSlip.getSecondWeighTime().format(DATE_TIME_FORMATTER));
        }
        
        response.setPlateNo(weighingSlip.getPlateNo());
        
        if (weighingSlip.getGrossWeight() != null) {
            response.setGrossWeight(weighingSlip.getGrossWeight().doubleValue());
        }
        
        if (weighingSlip.getTareWeight() != null) {
            response.setTareWeight(weighingSlip.getTareWeight().doubleValue());
        }
        
        if (weighingSlip.getNetWeight() != null) {
            response.setNetWeight(weighingSlip.getNetWeight().doubleValue());
        }
        
        response.setPhotoUrl(weighingSlip.getPhotoUrl());
        response.setStatus(weighingSlip.getStatus());
        response.setCreatorId(weighingSlip.getCreatorId());
        response.setRemark(weighingSlip.getRemark());
        
        if (weighingSlip.getCreateTime() != null) {
            response.setCreateTime(weighingSlip.getCreateTime().format(DATE_TIME_FORMATTER));
        }
        if (weighingSlip.getUpdateTime() != null) {
            response.setUpdateTime(weighingSlip.getUpdateTime().format(DATE_TIME_FORMATTER));
        }

        // 查询创建人名称
        if (weighingSlip.getCreatorId() != null) {
            Employee creator = employeeMapper.selectById(weighingSlip.getCreatorId());
            if (creator != null) {
                response.setCreatorName(creator.getEmployeeName());
            }
        }

        // 查询关联的运输单号列表
        List<String> dispatchCodes = weighingSlipDispatchMapper.selectDispatchCodesByWeighingSlipId(
                weighingSlip.getWeighingSlipId());
        response.setDispatchCodes(dispatchCodes);

        // 如果有运输单号，查询第一个运输单的详细信息（用于显示）
        if (!dispatchCodes.isEmpty()) {
            response.setDispatchCode(dispatchCodes.get(0));
        }

        // 加载关联的收运通知单列表和危废明细
        List<PickupNoticeForWarehousing> pickupNoticeList = loadPickupNoticeList(dispatchCodes);
        response.setPickupNoticeList(pickupNoticeList);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeighingSlipInfoResponse updateWeighingSlip(UpdateWeighingSlipRequest request) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        
        // 查询总磅单（使用自定义查询方法，避免包含不存在的更新时间字段）
        WeighingSlip weighingSlip = weighingSlipMapper.selectByWeighingSlipNo(request.getWeighingSlipNo());

        if (weighingSlip == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "总磅单不存在：" + request.getWeighingSlipNo());
        }

        // 已细分状态不能修改
        if ("已细分".equals(weighingSlip.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "已细分的总磅单不能修改");
        }

        // 保存旧数据用于日志记录
        WeighingSlipInfoResponse oldDetail = null;
        try {
            oldDetail = buildWeighingSlipInfoResponse(weighingSlip);
        } catch (Exception e) {
            log.warn("获取总磅单旧数据失败，将跳过数据变更日志记录", e);
        }

        // 更新基本信息
        if (StrUtil.isNotBlank(request.getSequence())) {
            // 检查序号是否重复（排除当前总磅单）
            // 如果新序号与当前序号不同，需要检查是否已被其他总磅单使用
            if (!request.getSequence().equals(weighingSlip.getSequence())) {
                int count = weighingSlipMapper.countBySequence(request.getSequence());
                if (count > 0) {
                    throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "序号已存在：" + request.getSequence());
                }
            }
            weighingSlip.setSequence(request.getSequence());
        }

        if (StrUtil.isNotBlank(request.getDate())) {
            try {
                weighingSlip.setWeighingDate(LocalDate.parse(request.getDate(), DATE_FORMATTER));
            } catch (Exception e) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "日期格式错误，应为：yyyy-MM-dd");
            }
        }

        if (StrUtil.isNotBlank(request.getFirstWeighTime())) {
            try {
                weighingSlip.setFirstWeighTime(LocalDateTime.parse(request.getFirstWeighTime(), DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "一次过秤时间格式错误，应为：yyyy-MM-dd HH:mm:ss");
            }
        }

        if (StrUtil.isNotBlank(request.getSecondWeighTime())) {
            try {
                weighingSlip.setSecondWeighTime(LocalDateTime.parse(request.getSecondWeighTime(), DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "二次过秤时间格式错误，应为：yyyy-MM-dd HH:mm:ss");
            }
        }

        if (StrUtil.isNotBlank(request.getPlateNo())) {
            weighingSlip.setPlateNo(request.getPlateNo());
        }

        if (request.getGrossWeight() != null) {
            weighingSlip.setGrossWeight(BigDecimal.valueOf(request.getGrossWeight()));
        }

        if (request.getTareWeight() != null) {
            weighingSlip.setTareWeight(BigDecimal.valueOf(request.getTareWeight()));
        }

        // 计算净重
        if (weighingSlip.getGrossWeight() != null && weighingSlip.getTareWeight() != null) {
            BigDecimal netWeight = weighingSlip.getGrossWeight().subtract(weighingSlip.getTareWeight());
            if (netWeight.compareTo(BigDecimal.ZERO) < 0) {
                netWeight = BigDecimal.ZERO;
            }
            weighingSlip.setNetWeight(netWeight);
        } else if (request.getNetWeight() != null) {
            weighingSlip.setNetWeight(BigDecimal.valueOf(request.getNetWeight()));
        }

        if (StrUtil.isNotBlank(request.getPhotoUrl())) {
            String photoRef = request.getPhotoUrl().trim();
            // 如果是纯数字，视为 FILE.文件编号，直接保存
            if (photoRef.matches("\\d+")) {
                weighingSlip.setPhotoUrl(photoRef);
            } else {
                // 兼容历史：处理 URL/路径，仅保存相对路径部分
                String normalizedPath = normalizePhotoPath(photoRef, request.getWeighingSlipNo());
                weighingSlip.setPhotoUrl(normalizedPath);
            }
        }

        if (StrUtil.isNotBlank(request.getStatus())) {
            weighingSlip.setStatus(request.getStatus());
        }

        if (request.getRemark() != null) {
            weighingSlip.setRemark(request.getRemark());
        }

        // 更新总磅单，设置更新时间为现在
        weighingSlip.setUpdateTime(LocalDateTime.now());
        int rows = weighingSlipMapper.updateById(weighingSlip);
        if (rows == 0) {
            throw new BusinessException("更新总磅单失败：记录已被其他用户修改");
        }

        // 更新关联的运输单
        if (request.getDispatchCodes() != null) {
            // 去重：移除重复的运输单号
            List<String> uniqueDispatchCodes = request.getDispatchCodes().stream()
                    .filter(code -> StrUtil.isNotBlank(code))
                    .distinct()
                    .collect(Collectors.toList());

            // 先验证所有运输单是否存在，并检查是否已被其他总磅单关联
            // 这样可以提前发现问题，避免部分关联成功部分失败的情况
            for (String dispatchCode : uniqueDispatchCodes) {
                DispatchOrder dispatchOrder = dispatchOrderMapper.selectOne(
                        new LambdaQueryWrapper<DispatchOrder>()
                                .eq(DispatchOrder::getDispatchCode, dispatchCode)
                );
                if (dispatchOrder == null) {
                    throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "运输单不存在：" + dispatchCode);
                }

                // 检查运输单是否已被其他总磅单关联（排除当前总磅单）
                WeighingSlipDispatch existingDispatch = weighingSlipDispatchMapper.selectOne(
                        new LambdaQueryWrapper<WeighingSlipDispatch>()
                                .eq(WeighingSlipDispatch::getDispatchCode, dispatchCode)
                );
                if (existingDispatch != null && !existingDispatch.getWeighingSlipId().equals(weighingSlip.getWeighingSlipId())) {
                    throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), 
                            "运输单已被其他总磅单关联：" + dispatchCode + "（总磅单ID：" + existingDispatch.getWeighingSlipId() + "）");
                }
            }

            // 删除旧的关联（在验证通过后）
            int oldDispatchCount = weighingSlipDispatchMapper.delete(
                    new LambdaQueryWrapper<WeighingSlipDispatch>()
                            .eq(WeighingSlipDispatch::getWeighingSlipId, weighingSlip.getWeighingSlipId())
            );
            log.info("删除旧运输单关联：weighingSlipId={}, deletedCount={}", weighingSlip.getWeighingSlipId(), oldDispatchCount);

            // 添加新的关联
            if (!uniqueDispatchCodes.isEmpty()) {
                for (String dispatchCode : uniqueDispatchCodes) {
                    // 再次检查（防止并发问题）
                    WeighingSlipDispatch existingDispatch = weighingSlipDispatchMapper.selectOne(
                            new LambdaQueryWrapper<WeighingSlipDispatch>()
                                    .eq(WeighingSlipDispatch::getDispatchCode, dispatchCode)
                    );
                    if (existingDispatch != null && !existingDispatch.getWeighingSlipId().equals(weighingSlip.getWeighingSlipId())) {
                        throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), 
                                "运输单已被其他总磅单关联（并发冲突）：" + dispatchCode);
                    }

                    // 创建关联记录
                    WeighingSlipDispatch dispatch = new WeighingSlipDispatch();
                    dispatch.setWeighingSlipId(weighingSlip.getWeighingSlipId());
                    dispatch.setDispatchCode(dispatchCode);
                    weighingSlipDispatchMapper.insert(dispatch);
                }
            }
        }

        log.info("更新总磅单成功：总磅单号={}", weighingSlip.getWeighingSlipNo());

        // 重新查询并构建响应（使用自定义查询方法，避免包含不存在的更新时间字段）
        WeighingSlip updated = weighingSlipMapper.selectByWeighingSlipNo(weighingSlip.getWeighingSlipNo());
        WeighingSlipInfoResponse newDetail = buildWeighingSlipInfoResponse(updated);

        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                logRecordService.recordDataChangeLog("总磅单管理", "WEIGHING_SLIP",
                        String.valueOf(weighingSlip.getWeighingSlipId()),
                        "更新",
                        "更新总磅单：总磅单号=" + request.getWeighingSlipNo(),
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.warn("记录总磅单更新数据变更日志失败", e);
            }
        }

        return newDetail;
    }

    /**
     * 规范化总磅单照片路径：
     * - 支持 /api/file/download?path=... 格式，仅提取 path 参数
     * - 支持直接存储相对路径 WEIGHING_SLIP/2025/12/13/xxx.png
     * - 控制最大长度，避免超过数据库字段限制
     *
     * @param rawPhotoUrl    原始照片URL或路径
     * @param weighingSlipNo 总磅单号（用于日志）
     * @return 规范化后的相对路径
     */
    private String normalizePhotoPath(String rawPhotoUrl, String weighingSlipNo) {
        if (StrUtil.isBlank(rawPhotoUrl)) {
            return null;
        }

        String photoUrl = rawPhotoUrl;

        // 如果包含查询参数，提取 path 参数的值
        if (photoUrl.contains("?path=")) {
            String pathParam = photoUrl.substring(photoUrl.indexOf("?path=") + 6);
            // 如果path参数后面还有其他参数，只取第一个参数的值
            if (pathParam.contains("&")) {
                pathParam = pathParam.substring(0, pathParam.indexOf("&"));
            }
            // URL解码（如果有编码）
            try {
                pathParam = java.net.URLDecoder.decode(pathParam, "UTF-8");
            } catch (Exception e) {
                // 解码失败，使用原始值
            }
            photoUrl = pathParam;
        } else if (photoUrl.startsWith("/api/file/download")) {
            // 如果只是路径部分，去掉前缀
            photoUrl = photoUrl.replace("/api/file/download", "");
        }

        // 如果URL太长，截断到合理长度（假设数据库字段为VARCHAR(255)，保守起见使用200）
        // 如果路径仍然太长，只保留文件名部分
        if (photoUrl.length() > 200) {
            // 尝试提取文件名
            int lastSlash = photoUrl.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < photoUrl.length() - 1) {
                String fileName = photoUrl.substring(lastSlash + 1);
                // 如果文件名也过长，截断
                if (fileName.length() > 200) {
                    fileName = fileName.substring(0, 200);
                }
                // 保留目录结构的前缀（最多100字符）+ 文件名
                String dirPrefix = photoUrl.substring(0, Math.min(lastSlash, 100));
                photoUrl = dirPrefix + "/" + fileName;
            } else {
                // 没有目录结构，直接截断
                photoUrl = photoUrl.substring(0, 200);
            }
            log.warn("总磅单照片路径过长，已截断：总磅单号={}，原始长度={}", weighingSlipNo, rawPhotoUrl.length());
        }

        return photoUrl;
    }

    /**
     * 将总磅单照片字段（如果是 FILE.文件编号）与 FILE 表建立业务关联。
     *
     * @param weighingSlip 总磅单实体
     */
    private void bindPhotoFileToWeighingSlip(WeighingSlip weighingSlip) {
        if (weighingSlip == null || weighingSlip.getWeighingSlipId() == null) {
            return;
        }
        String photoRef = weighingSlip.getPhotoUrl();
        if (StrUtil.isBlank(photoRef)) {
            return;
        }
        // 仅当照片字段是纯数字时，视为 FILE.文件编号
        if (!photoRef.matches("\\d+")) {
            return;
        }
        try {
            Integer fileId = Integer.valueOf(photoRef);
            File fileEntity = fileMapper.selectById(fileId);
            if (fileEntity == null) {
                log.warn("根据总磅单照片字段未找到文件记录：weighingSlipId={}, fileId={}", weighingSlip.getWeighingSlipId(), fileId);
                return;
            }
            // 建立业务关联
            fileEntity.setBusinessType(WEIGHING_SLIP_BUSINESS_TYPE);
            fileEntity.setBusinessId(weighingSlip.getWeighingSlipId());
            // 业务模块如果为空，可按需填充一个通用值，避免覆盖其他模块自定义值
            if (StrUtil.isBlank(fileEntity.getBusinessModule())) {
                fileEntity.setBusinessModule("生产");
            }
            fileMapper.updateById(fileEntity);
            log.info("已将文件与总磅单建立关联：weighingSlipId={}, fileId={}", weighingSlip.getWeighingSlipId(), fileId);
        } catch (Exception e) {
            log.error("绑定总磅单照片文件关联失败：weighingSlipId={}, photoRef={}", weighingSlip.getWeighingSlipId(), photoRef, e);
        }
    }

    @Override
    public WeighingSlipListResponse getWeighingSlipPage(WeighingSlipPageRequest request) {
        try {
            // 设置默认值
            Long current = request.getCurrent() != null && request.getCurrent() > 0 ? request.getCurrent() : 1L;
            Long size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 10L;

            // 页面权限编码
            String pageCode = "仓库管理:入库-总磅单:页面";

            // 使用 ViewScopeHelper 解析视图范围
            String viewScope = ViewScopeHelper.resolveViewScope(pageCode, request.getViewScope());

            // 获取当前用户ID
            Integer currentUserId = SecurityUtil.getCurrentUserId();

            // SELF 模式时添加创建人过滤条件，ALL 模式时不限制
            Integer creatorFilter = ViewScopeHelper.isSelfScope(viewScope) ? currentUserId : null;

            Page<WeighingSlip> page = new Page<>(current, size);

            // 处理空字符串，转换为null
            String keyword = normalize(request.getKeyword());
            String weighingSlipNo = normalize(request.getWeighingSlipNo());
            String sequence = normalize(request.getSequence());
            String plateNo = normalize(request.getPlateNo());
            String status = normalize(request.getStatus());
            String startDate = normalize(request.getStartDate());
            String endDate = normalize(request.getEndDate());

            IPage<WeighingSlip> entityPage = weighingSlipMapper.selectWeighingSlipPage(
                    page,
                    keyword,
                    weighingSlipNo,
                    sequence,
                    plateNo,
                    status,
                    startDate,
                    endDate,
                    request.getOrderBy(),
                    request.getOrderDirection(),
                    creatorFilter
            );

            List<WeighingSlip> records = entityPage.getRecords();
            if (CollectionUtils.isEmpty(records)) {
                WeighingSlipListResponse response = new WeighingSlipListResponse();
                response.setStats(buildStats());
                response.setRecords(new ArrayList<>());
                response.setTotal(entityPage.getTotal());
                response.setCurrent(entityPage.getCurrent());
                response.setSize(entityPage.getSize());
                return response;
            }

            // 批量查询创建人和关联的运输单号
            Set<Integer> creatorIds = records.stream()
                    .filter(ws -> ws != null)
                    .map(WeighingSlip::getCreatorId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());

            final Map<Integer, Employee> employeeMap;
            if (!CollectionUtils.isEmpty(creatorIds)) {
                List<Employee> employees = employeeMapper.selectBatchIds(new ArrayList<>(creatorIds));
                if (employees != null && !employees.isEmpty()) {
                    employeeMap = employees.stream()
                            .filter(e -> e != null && e.getEmployeeId() != null)
                            .collect(Collectors.toMap(Employee::getEmployeeId, e -> e, (a, b) -> a));
                } else {
                    employeeMap = new HashMap<>();
                }
            } else {
                employeeMap = new HashMap<>();
            }

            // 批量查询关联的运输单号
            Map<Integer, List<String>> dispatchCodesMap = records.stream()
                    .filter(ws -> ws != null && ws.getWeighingSlipId() != null)
                    .collect(Collectors.toMap(
                            WeighingSlip::getWeighingSlipId,
                            weighingSlip -> {
                                List<String> dispatchCodes = weighingSlipDispatchMapper.selectDispatchCodesByWeighingSlipId(
                                        weighingSlip.getWeighingSlipId());
                                return dispatchCodes != null ? dispatchCodes : new ArrayList<>();
                            },
                            (a, b) -> a
                    ));

            // 转换为响应DTO
            List<WeighingSlipPageResponse> responseList = records.stream()
                    .filter(ws -> ws != null && ws.getWeighingSlipId() != null)
                    .map(weighingSlip -> convertToPageResponse(weighingSlip, employeeMap, dispatchCodesMap))
                    .collect(Collectors.toList());

            WeighingSlipListResponse response = new WeighingSlipListResponse();
            response.setStats(buildStats());
            response.setRecords(responseList);
            response.setTotal(entityPage.getTotal());
            response.setCurrent(entityPage.getCurrent());
            response.setSize(entityPage.getSize());

            return response;
        } catch (Exception e) {
            log.error("查询总磅单列表异常，异常类型：{}，异常信息：{}", 
                    e.getClass().getName(), e.getMessage() != null ? e.getMessage() : "null", e);
            throw e;
        }
    }

    /**
     * 转换为分页响应DTO
     */
    private WeighingSlipPageResponse convertToPageResponse(
            WeighingSlip weighingSlip,
            Map<Integer, Employee> employeeMap,
            Map<Integer, List<String>> dispatchCodesMap) {
        WeighingSlipPageResponse response = new WeighingSlipPageResponse();
        response.setWeighingSlipId(weighingSlip.getWeighingSlipId());
        response.setWeighingSlipNo(weighingSlip.getWeighingSlipNo());
        response.setSequence(weighingSlip.getSequence());
        response.setWeighingDate(weighingSlip.getWeighingDate());
        response.setFirstWeighTime(weighingSlip.getFirstWeighTime());
        response.setSecondWeighTime(weighingSlip.getSecondWeighTime());
        response.setPlateNo(weighingSlip.getPlateNo());
        response.setGrossWeight(weighingSlip.getGrossWeight());
        response.setTareWeight(weighingSlip.getTareWeight());
        response.setNetWeight(weighingSlip.getNetWeight());
        response.setPhotoUrl(weighingSlip.getPhotoUrl());
        response.setStatus(weighingSlip.getStatus());
        response.setCreatorId(weighingSlip.getCreatorId());
        response.setRemark(weighingSlip.getRemark());
        response.setCreateTime(weighingSlip.getCreateTime());
        response.setUpdateTime(weighingSlip.getUpdateTime());

        // 设置创建人名称
        if (weighingSlip.getCreatorId() != null && employeeMap.containsKey(weighingSlip.getCreatorId())) {
            Employee employee = employeeMap.get(weighingSlip.getCreatorId());
            response.setCreatorName(employee.getEmployeeName());
        }

        // 设置关联的运输单号列表
        List<String> dispatchCodes = dispatchCodesMap.getOrDefault(weighingSlip.getWeighingSlipId(), new ArrayList<>());
        response.setDispatchCodes(dispatchCodes);

        return response;
    }

    /**
     * 构建统计信息
     */
    private List<WeighingSlipStat> buildStats() {
        List<WeighingSlipStat> stats = new ArrayList<>();
        try {
            // 统计待细分数量
            long pendingCount = weighingSlipMapper.selectCount(
                    new LambdaQueryWrapper<WeighingSlip>().eq(WeighingSlip::getStatus, "待细分")
            );
            WeighingSlipStat pendingStat = new WeighingSlipStat();
            pendingStat.setLabel("待细分");
            pendingStat.setValue(String.valueOf(pendingCount));
            pendingStat.setColor("warning");
            stats.add(pendingStat);

            // 统计已细分数量
            long completedCount = weighingSlipMapper.selectCount(
                    new LambdaQueryWrapper<WeighingSlip>().eq(WeighingSlip::getStatus, "已细分")
            );
            WeighingSlipStat completedStat = new WeighingSlipStat();
            completedStat.setLabel("已细分");
            completedStat.setValue(String.valueOf(completedCount));
            completedStat.setColor("success");
            stats.add(completedStat);

            // 统计总数
            long totalCount = weighingSlipMapper.selectCount(null);
            WeighingSlipStat totalStat = new WeighingSlipStat();
            totalStat.setLabel("总数");
            totalStat.setValue(String.valueOf(totalCount));
            totalStat.setColor("info");
            stats.add(totalStat);
        } catch (Exception e) {
            log.error("构建统计信息失败", e);
            // 即使统计失败，也返回空列表，不影响主查询
        }
        return stats;
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

    /**
     * 加载关联的收运通知单列表和危废明细
     *
     * @param dispatchCodes 运输单号列表
     * @return 收运通知单列表
     */
    private List<PickupNoticeForWarehousing> loadPickupNoticeList(List<String> dispatchCodes) {
        List<PickupNoticeForWarehousing> result = new ArrayList<>();
        
        if (CollectionUtils.isEmpty(dispatchCodes)) {
            return result;
        }

        // 通过运输单号查询收运通知单号
        Map<String, String> dispatchToNoticeMap = new HashMap<>();
        for (String dispatchCode : dispatchCodes) {
            try {
                DispatchOrder dispatchOrder = dispatchOrderMapper.selectByDispatchCode(dispatchCode);
                if (dispatchOrder != null && StrUtil.isNotBlank(dispatchOrder.getNoticeCode())) {
                    dispatchToNoticeMap.put(dispatchCode, dispatchOrder.getNoticeCode());
                }
            } catch (Exception e) {
                log.warn("查询运输单失败：dispatchCode={}", dispatchCode, e);
            }
        }

        if (dispatchToNoticeMap.isEmpty()) {
            return result;
        }

        // 批量查询收运通知单
        List<String> noticeCodes = new ArrayList<>(dispatchToNoticeMap.values());
        Map<String, PickupNotice> noticeMap = new HashMap<>();
        for (String noticeCode : noticeCodes) {
            try {
                PickupNotice notice = pickupNoticeMapper.selectOne(
                        new LambdaQueryWrapper<PickupNotice>()
                                .eq(PickupNotice::getNoticeCode, noticeCode)
                );
                if (notice != null) {
                    noticeMap.put(noticeCode, notice);
                }
            } catch (Exception e) {
                log.warn("查询收运通知单失败：noticeCode={}", noticeCode, e);
            }
        }

        // 批量查询危废明细
        Map<String, List<PickupNoticeItem>> itemMap = new HashMap<>();
        try {
            List<PickupNoticeItem> allItems = pickupNoticeItemMapper.selectByNoticeCodes(noticeCodes);
            for (PickupNoticeItem item : allItems) {
                itemMap.computeIfAbsent(item.getNoticeCode(), k -> new ArrayList<>()).add(item);
            }
        } catch (Exception e) {
            log.warn("批量查询危废明细失败：noticeCodes={}", noticeCodes, e);
        }

        // 批量查询危废品目详情（用于获取废物类别）
        Map<Integer, HazardousWasteItem> hazardousWasteItemMap = new HashMap<>();
        try {
            // 收集所有危废品目ID
            List<Integer> hazardousWasteItemIds = itemMap.values().stream()
                    .flatMap(List::stream)
                    .map(PickupNoticeItem::getHazardousWasteItemId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (!hazardousWasteItemIds.isEmpty()) {
                List<HazardousWasteItem> hazardousWasteItems = hazardousWasteItemMapper.selectDetailByIds(hazardousWasteItemIds);
                for (HazardousWasteItem hwItem : hazardousWasteItems) {
                    hazardousWasteItemMap.put(hwItem.getItemId(), hwItem);
                }
            }
        } catch (Exception e) {
            log.warn("批量查询危废品目详情失败", e);
        }

        // 构建结果列表（按运输单号顺序）
        for (String dispatchCode : dispatchCodes) {
            String noticeCode = dispatchToNoticeMap.get(dispatchCode);
            if (StrUtil.isBlank(noticeCode)) {
                continue;
            }

            PickupNotice notice = noticeMap.get(noticeCode);
            if (notice == null) {
                continue;
            }

            PickupNoticeForWarehousing pickupNoticeInfo = new PickupNoticeForWarehousing();
            pickupNoticeInfo.setPickupNoticeId(notice.getNoticeId());
            pickupNoticeInfo.setPickupNoticeNo(notice.getNoticeCode());
            pickupNoticeInfo.setDispatchCode(dispatchCode);
            pickupNoticeInfo.setContractCode(notice.getContractCode());
            pickupNoticeInfo.setContractPending(notice.getContractPending());
            if (notice.getContractFixTime() != null) {
                pickupNoticeInfo.setContractFixTime(notice.getContractFixTime().format(DATE_TIME_FORMATTER));
            }
            pickupNoticeInfo.setCustomerId(notice.getCustomerId());
            pickupNoticeInfo.setCompanyName(notice.getCompanyName());
            pickupNoticeInfo.setCreditCode(notice.getCreditCode());
            pickupNoticeInfo.setTransportAddress(notice.getTransportAddress());
            pickupNoticeInfo.setOnsiteContact(notice.getOnsiteContact());
            pickupNoticeInfo.setOnsitePhone(notice.getOnsitePhone());
            pickupNoticeInfo.setEmergencyPhone(notice.getEmergencyPhone());
            if (notice.getPlanTransferDate() != null) {
                pickupNoticeInfo.setTransferDeliveryTime(notice.getPlanTransferDate().format(DATE_TIME_FORMATTER));
            }
            pickupNoticeInfo.setRemark(notice.getRemark());
            pickupNoticeInfo.setWasteDetailFile(notice.getWasteDetailFile());
            pickupNoticeInfo.setQrCode(notice.getQrCode());
            pickupNoticeInfo.setStatus(notice.getStatus());
            if (notice.getSubmittedAt() != null) {
                pickupNoticeInfo.setSubmittedAt(notice.getSubmittedAt().format(DATE_TIME_FORMATTER));
            }
            if (notice.getAuditedAt() != null) {
                pickupNoticeInfo.setAuditedAt(notice.getAuditedAt().format(DATE_TIME_FORMATTER));
            }
            pickupNoticeInfo.setAuditorId(notice.getAuditorId());
            pickupNoticeInfo.setCreatorId(notice.getCreatorId());
            if (notice.getCreateTime() != null) {
                pickupNoticeInfo.setCreatedAt(notice.getCreateTime().format(DATE_TIME_FORMATTER));
            }
            if (notice.getUpdateTime() != null) {
                pickupNoticeInfo.setUpdateTime(notice.getUpdateTime().format(DATE_TIME_FORMATTER));
            }
            pickupNoticeInfo.setLocked(notice.getLocked());
            if (notice.getLockTime() != null) {
                pickupNoticeInfo.setLockTime(notice.getLockTime().format(DATE_TIME_FORMATTER));
            }
            pickupNoticeInfo.setLockUserId(notice.getLockUserId());
            pickupNoticeInfo.setLockReason(notice.getLockReason());

            // 查询客户名称
            if (notice.getCustomerId() != null) {
                try {
                    Customer customer = customerMapper.selectById(notice.getCustomerId());
                    if (customer != null) {
                        pickupNoticeInfo.setCustomerName(customer.getEnterpriseName());
                    }
                } catch (Exception e) {
                    log.warn("查询客户信息失败：customerId={}", notice.getCustomerId(), e);
                }
            }

            // 查询审核人名称
            if (notice.getAuditorId() != null) {
                try {
                    Employee auditor = employeeMapper.selectById(notice.getAuditorId());
                    if (auditor != null) {
                        pickupNoticeInfo.setAuditorName(auditor.getEmployeeName());
                    }
                } catch (Exception e) {
                    log.warn("查询审核人信息失败：auditorId={}", notice.getAuditorId(), e);
                }
            }

            // 查询创建人名称
            if (notice.getCreatorId() != null) {
                try {
                    Employee creator = employeeMapper.selectById(notice.getCreatorId());
                    if (creator != null) {
                        pickupNoticeInfo.setCreatorName(creator.getEmployeeName());
                    }
                } catch (Exception e) {
                    log.warn("查询创建人信息失败：creatorId={}", notice.getCreatorId(), e);
                }
            }

            // 加载危废明细
            List<PickupNoticeItem> items = itemMap.getOrDefault(noticeCode, new ArrayList<>());
            List<PickupNoticeForWarehousing.WasteItem> wasteItems = new ArrayList<>();
            for (PickupNoticeItem item : items) {
                PickupNoticeForWarehousing.WasteItem wasteItem = new PickupNoticeForWarehousing.WasteItem();
                wasteItem.setPickupNoticeItemId(item.getItemId());
                wasteItem.setWasteName(item.getWasteName());
                wasteItem.setWasteCode(item.getWasteCode());
                wasteItem.setHazardFeature(item.getHazardFeature());
                wasteItem.setHazardousWasteItemId(item.getHazardousWasteItemId());
                // 设置废物类别（从危废品目表联表查询获取）
                if (item.getHazardousWasteItemId() != null) {
                    HazardousWasteItem hwItem = hazardousWasteItemMap.get(item.getHazardousWasteItemId());
                    if (hwItem != null) {
                        wasteItem.setWasteCategory(hwItem.getWasteCategory());
                    }
                }
                wasteItem.setPlannedQtyTon(item.getPlannedQtyTon());
                wasteItem.setPackageType(item.getPackageType());
                wasteItem.setPackageQty(item.getPackageQty());
                wasteItem.setForm(item.getForm());
                // 继承收运通知单明细的辅助核算相关字段
                wasteItem.setMeasureUnit(item.getMeasureUnit());
                wasteItem.setEnableAuxiliaryAccounting(item.getEnableAuxiliaryAccounting());
                wasteItem.setAuxUnit(item.getAuxUnit());
                wasteItem.setAuxPerBase(item.getAuxPerBase());
                wasteItem.setAuxQuantity(item.getAuxQuantity());
                if (item.getCreateTime() != null) {
                    wasteItem.setCreatedAt(item.getCreateTime().format(DATE_TIME_FORMATTER));
                }
                if (item.getUpdateTime() != null) {
                    wasteItem.setUpdateTime(item.getUpdateTime().format(DATE_TIME_FORMATTER));
                }
                wasteItems.add(wasteItem);
            }
            pickupNoticeInfo.setItems(wasteItems);

            result.add(pickupNoticeInfo);
        }

        return result;
    }
}

