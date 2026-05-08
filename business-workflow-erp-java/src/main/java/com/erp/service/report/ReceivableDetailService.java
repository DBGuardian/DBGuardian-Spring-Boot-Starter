package com.erp.service.report;

import com.erp.controller.report.dto.ReceivableDetailRequest;
import com.erp.controller.report.dto.ReceivableDetailResponse;
import com.erp.controller.report.dto.ReceivableExcelRow;

import java.util.List;

/**
 * 应收账款明细表 Service
 * 
 * 功能描述：
 * - 获取应收账款明细表数据（支持Redis缓存）
 * - 重新计算应收账款明细表（清除缓存）
 * - 清除所有应收账款缓存
 * 
 * 缓存策略：
 * - 首次加载：自动计算数据并缓存到Redis
 * - 后续加载：直接从Redis加载缓存数据
 * - 手动刷新：点击"重新计算"按钮，清除缓存并重新计算
 * 
 * 查询优化：
 * - 使用JOIN连表查询获取所有数据（1次查询）
 * - 批量查询补充数据（2次查询）
 * - 在内存中构建树形结构（避免多次数据库查询）
 * - 总查询次数：3次
 */
public interface ReceivableDetailService {

  /**
   * 获取应收账款明细表数据
   * 
   * 流程：
   * 1. 先检查缓存
   * 2. 缓存不存在则计算并缓存
   * 3. 返回数据
   */
  ReceivableDetailResponse getDetailList(ReceivableDetailRequest request);

  /**
   * 重新计算数据（清除缓存）
   */
  ReceivableDetailResponse recalculateDetailList(ReceivableDetailRequest request);

  /**
   * 清除所有应收账款缓存
   */
  void clearAllCache();

  /**
   * 查询全量数据并展平为 Excel 行列表（用于导出，不分页）
   *
   * 展平规则：
   * - contractRowSpan = 该合同下所有结算单的发票数量之和
   * - settlementRowSpan = 该结算单下的发票数量
   * - 发票与应收明细 1:1，合并在同一行
   * - rowSpan = 0 表示被上方单元格合并，写入时跳过
   * - selectedKeys 有值时，仅导出勾选节点及其必要上下文
   */
  List<ReceivableExcelRow> queryAllForExport(ReceivableDetailRequest request);
}
