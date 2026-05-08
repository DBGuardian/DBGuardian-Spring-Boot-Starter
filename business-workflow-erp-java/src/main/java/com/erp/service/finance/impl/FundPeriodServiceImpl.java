package com.erp.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundPeriodInitResponse;
import com.erp.controller.finance.dto.FundPeriodListItemResponse;
import com.erp.controller.finance.dto.FundPeriodPageRequest;
import com.erp.controller.finance.dto.FundPeriodResponse;
import com.erp.entity.finance.FundPeriod;
import com.erp.mapper.finance.FundPeriodMapper;
import com.erp.service.finance.FundPeriodService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 账期服务实现类
 *
 * 账期管理已重新设计，后续将重新实现相关功能
 */
@Slf4j
@Service
public class FundPeriodServiceImpl extends ServiceImpl<FundPeriodMapper, FundPeriod> implements FundPeriodService {

    /**
     * 初始化账期
     *
     * @param organizationId 组织ID
     * @param year 年份
     * @param overwrite 是否覆盖已存在的账期
     * @return 初始化结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundPeriodInitResponse initFundPeriods(Long organizationId, Integer year, Boolean overwrite) {
        log.info("开始初始化账期，组织ID：{}，年份：{}，覆盖：{}", organizationId, year, overwrite);

        // 获取当前登录用户作为创建人
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        Long creatorUserId = currentUserId != null ? currentUserId.longValue() : null;

        // 检查该组织该年份是否已有账期
        LambdaQueryWrapper<FundPeriod> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FundPeriod::getOrganizationId, organizationId)
                   .eq(FundPeriod::getYear, year);
        List<FundPeriod> existingPeriods = this.list(queryWrapper);

        if (!existingPeriods.isEmpty()) {
            if (!overwrite) {
                throw new BusinessException("该组织该年份已有账期数据，请选择覆盖选项或更换年份");
            }
            // 删除已存在的账期
            boolean removed = this.remove(queryWrapper);
            if (!removed) {
                log.warn("删除已存在的账期数据失败，组织ID：{}，年份：{}", organizationId, year);
            }
            log.info("删除已存在的账期数据，组织ID：{}，年份：{}，删除数量：{}", organizationId, year, existingPeriods.size());
        }

        // 生成12个月的账期数据
        List<FundPeriod> newPeriods = new ArrayList<>();
        List<FundPeriodResponse> periodResponses = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            FundPeriod period = new FundPeriod();
            period.setPeriodCode(String.format("%d%02d", year, month)); // 格式：YYYYMM
            period.setYear(year);
            period.setMonth(month);
            period.setOrganizationId(organizationId);
            period.setIsSettled(false); // 未结账状态
            period.setCreateUserId(creatorUserId);
            period.setCreateTime(LocalDateTime.now());
            period.setUpdateTime(LocalDateTime.now());
            period.setVersion(0);

            newPeriods.add(period);
        }

        // 避免与现有全局 periodCode 冲突（periodCode 在表上是全局唯一）
        List<String> codesToCheck = new ArrayList<>();
        for (FundPeriod p : newPeriods) {
            codesToCheck.add(p.getPeriodCode());
        }
        QueryWrapper<FundPeriod> codeQuery = new QueryWrapper<>();
        // 只检查当前组织下是否存在相同的账期编码，允许不同组织使用相同的账期编码
        codeQuery.eq("组织编号", organizationId).in("账期编码", codesToCheck);
        List<FundPeriod> conflictPeriods = this.list(codeQuery);
        if (!conflictPeriods.isEmpty()) {
            // 从待插入列表中过滤掉已存在的 periodCode，避免重复键错误
            List<String> existingCodes = new ArrayList<>();
            for (FundPeriod p : conflictPeriods) {
                existingCodes.add(p.getPeriodCode());
            }
            List<FundPeriod> filtered = new ArrayList<>();
            for (FundPeriod p : newPeriods) {
                if (!existingCodes.contains(p.getPeriodCode())) {
                    filtered.add(p);
                } else {
                    log.warn("初始化账期时跳过已存在的 periodCode={}", p.getPeriodCode());
                }
            }
            newPeriods = filtered;
        }

        // 批量保存（可能为空）
        if (!newPeriods.isEmpty()) {
            this.saveBatch(newPeriods);
        } else {
            log.info("没有新的账期需要插入（可能已存在相同 periodCode）");
        }

        // 转换为响应DTO
        for (FundPeriod period : newPeriods) {
            FundPeriodResponse response = new FundPeriodResponse();
            BeanUtils.copyProperties(period, response);
            periodResponses.add(response);
        }

        FundPeriodInitResponse result = new FundPeriodInitResponse();
        result.setYear(year);
        result.setOrganizationId(organizationId);
        result.setCreatedCount(newPeriods.size());
        result.setPeriods(periodResponses);

        log.info("账期初始化完成，组织ID：{}，年份：{}，创建数量：{}", organizationId, year, newPeriods.size());
        return result;
    }

    /**
     * 分页查询账期列表
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @Override
    public IPage<FundPeriodListItemResponse> getFundPeriodPage(FundPeriodPageRequest request) {
        log.debug("分页查询账期列表，查询条件：{}", request);

        // 创建分页对象
        Page<FundPeriodListItemResponse> page = new Page<>(
                request.getCurrent() != null ? request.getCurrent() : 1L,
                request.getSize() != null ? request.getSize() : 10L
        );

        // 调用Mapper进行分页查询
        return this.baseMapper.selectFundPeriodPage(page, request);
    }

    /**
     * 查询账期年份列表
     *
     * @return 年份列表
     */
    @Override
    public java.util.List<Integer> getFundPeriodYears() {
        log.debug("查询账期年份列表");

        // 调用Mapper查询年份列表
        java.util.List<Integer> years = this.baseMapper.selectFundPeriodYears();

        log.debug("查询到年份数量：{}", years != null ? years.size() : 0);

        return years;
    }

    /**
     * 结账
     *
     * @param periodId 账期编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settlePeriod(Long periodId) {
        log.info("开始结账，账期编号：{}", periodId);

        FundPeriod period = this.getById(periodId);
        if (period == null) {
            throw new BusinessException("账期不存在，periodId=" + periodId);
        }
        if (Boolean.TRUE.equals(period.getIsSettled())) {
            throw new BusinessException("该账期已结账，无需重复操作");
        }

        Integer currentUserId = SecurityUtil.getCurrentUserId();

        period.setIsSettled(true);
        period.setSettlementTime(LocalDateTime.now());
        period.setSettlementUserId(currentUserId != null ? currentUserId.longValue() : null);
        period.setUpdateTime(LocalDateTime.now());

        boolean updated = this.updateById(period);
        if (!updated) {
            throw new BusinessException("结账操作失败，请稍后重试");
        }

        log.info("结账成功，账期编号：{}，操作人：{}", periodId, currentUserId);
    }

    /**
     * 取消结账（反结账）
     *
     * @param periodId 账期编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseSettlePeriod(Long periodId) {
        log.info("开始取消结账，账期编号：{}", periodId);

        FundPeriod period = this.getById(periodId);
        if (period == null) {
            throw new BusinessException("账期不存在，periodId=" + periodId);
        }
        if (!Boolean.TRUE.equals(period.getIsSettled())) {
            throw new BusinessException("该账期尚未结账，无法取消结账");
        }

        period.setIsSettled(false);
        period.setSettlementTime(null);
        period.setSettlementUserId(null);
        period.setUpdateTime(LocalDateTime.now());

        boolean updated = this.updateById(period);
        if (!updated) {
            throw new BusinessException("取消结账操作失败，请稍后重试");
        }

        log.info("取消结账成功，账期编号：{}", periodId);
    }
}