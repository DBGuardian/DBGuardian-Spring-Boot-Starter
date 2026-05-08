package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.controller.settlement.dto.SettlementInvoiceSummaryResponse;
import com.erp.entity.settlement.SettlementInvoiceRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 结算单-发票关联关系Mapper接口
 * 用于查询结算单与发票的关联汇总信息
 *
 * @author ERP System
 * @date 2026-01-24
 */
@Mapper
public interface SettlementInvoiceRelMapper extends BaseMapper<SettlementInvoiceRel> {

    /**
     * 根据结算单编号查询发票汇总信息
     * 计算蓝字总额、红字总额、净额、可开蓝字金额等
     *
     * @param settlementId 结算单编号
     * @return 结算单发票汇总信息
     */
    SettlementInvoiceSummaryResponse selectSummaryBySettlementId(@Param("settlementId") Long settlementId);

    /**
     * 根据结算单编号查询关联发票的净额（蓝字总额 - 红字总额）
     * 用于更新结算单的已收金额
     *
     * @param settlementId 结算单编号
     * @return 净额（蓝字总额 - 红字总额）
     */
    BigDecimal selectNetAmountBySettlementId(@Param("settlementId") Long settlementId);
}