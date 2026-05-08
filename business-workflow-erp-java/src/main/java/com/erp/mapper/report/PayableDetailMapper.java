package com.erp.mapper.report;

import com.erp.controller.report.dto.ContractPricingModeDTO;
import com.erp.controller.report.dto.PayableDetailRequest;
import com.erp.controller.report.dto.PayableDetailDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应付账款明细表 Mapper
 */
@Mapper
public interface PayableDetailMapper {

  /**
   * 获取符合条件的合同ID列表（用于分页）
   */
  List<Long> selectContractIds(PayableDetailRequest request);

  /**
   * 获取指定合同的完整应付账款明细数据
   */
  List<PayableDetailDTO> selectDetailListWithJoin(@Param("contractIds") List<Long> contractIds);

  /**
   * 获取指定合同的所有结算单
   */
  List<PayableDetailDTO> selectSettlementsByContractIds(@Param("contractIds") List<Long> contractIds);

  /**
   * 获取指定结算单的所有发票和应付明细
   */
  List<PayableDetailDTO> selectInvoicesBySettlementIds(@Param("settlementIds") List<Long> settlementIds);

  /**
   * 批量获取合同的计价方式（一次查询）
   * 
   * 计价方式计算规则：
   * - 根据合同的危废明细中的计价方式判断
   * - 如果只有总价包干 → 显示"总价包干"
   * - 如果只有按量结算 → 显示"按量结算"
   * - 如果两者都有 → 显示"混合计价"
   */
  List<ContractPricingModeDTO> selectPricingModesByContractIds(@Param("contractIds") List<Long> contractIds);
}
