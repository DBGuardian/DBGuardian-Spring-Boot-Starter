package com.erp.mapper.report;

import com.erp.controller.report.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应收账款明细表 Mapper
 * 
 * 功能描述：
 * - 使用 JOIN 连表查询获取所有数据
 * - 批量查询补充数据
 * - 优化查询性能
 */
@Mapper
public interface ReceivableDetailMapper {

  /**
   * 获取符合条件的合同ID列表（用于分页）
   * 
   * 分页策略：
   * - 先查询符合条件的合同ID列表（应用分页）
   * - 再根据合同ID查询完整的树形数据（不分页）
   * - 这样可以确保每个合同的所有结算单、发票、应收明细都被返回
   */
  List<Long> selectContractIds(ReceivableDetailRequest request);

  /**
   * 获取应收账款明细表完整数据（一次查询）
   * 
   * 使用 JOIN 连表查询：
   * - CONTRACT 合同表
   * - SETTLEMENT 结算单表
   * - INVOICE 发票表
   * - SETTLEMENT_INVOICE_REL 应收明细关联表
   * 
   * 过滤条件：
   * - 结算类型为 RECEIVABLE（收款）
   * - 支持按合同编号、客户名称、日期范围筛选
   * - 不分页（根据合同ID列表查询完整数据）
   */
  List<ReceivableDetailDTO> selectDetailListWithJoin(@Param("contractIds") List<Long> contractIds);

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

  /**
   * 批量获取结算单的已收金额（一次查询）
   * 
   * 已收金额来源：
   * - 来自 SETTLEMENT 表的已收金额字段
   * - 或通过 SETTLEMENT_INVOICE_REL 表的关联金额求和
   */
  List<SettlementReceivedAmountDTO> selectReceivedAmountsBySettlementIds(@Param("settlementIds") List<Long> settlementIds);

  /**
   * 获取符合条件的合同总数（用于分页）
   * 
   * 功能描述：
   * - 查询符合条件的合同总数
   * - 用于前端分页显示
   */
  Long countContractIds(ReceivableDetailRequest request);
}
