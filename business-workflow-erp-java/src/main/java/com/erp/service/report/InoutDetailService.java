package com.erp.service.report;

import com.erp.controller.report.dto.InoutDetailRequest;
import com.erp.controller.report.dto.InoutDetailResponse;
import com.erp.controller.report.dto.InoutExcelRow;

import java.util.List;

/**
 * 出入库明细表 Service
 * 
 * 功能描述：
 * - 获取出入库明细表数据（支持Redis缓存）
 * - 重新计算出入库明细表（清除缓存）
 * - 清除所有出入库缓存
 * 
 * 缓存策略：
 * - 首次加载：自动计算数据并缓存到Redis
 * - 后续加载：直接从Redis加载缓存数据
 * - 手动刷新：点击"重新计算"按钮，清除缓存并重新计算
 * 
 * 查询优化：
 * - 使用JOIN连表查询获取所有数据（1次查询）
 * - 批量查询补充数据（1-2次查询）
 * - 在内存中计算库存数量（避免多次数据库查询）
 * - 总查询次数：2-3次
 */
public interface InoutDetailService {

  /**
   * 获取出入库明细表数据
   * 
   * 流程：
   * 1. 先检查缓存
   * 2. 缓存不存在则计算并缓存
   * 3. 返回数据
   */
  InoutDetailResponse getDetailList(InoutDetailRequest request);

  /**
   * 重新计算数据（清除缓存）
   */
  InoutDetailResponse recalculateDetailList(InoutDetailRequest request);

  /**
   * 清除所有出入库缓存
   */
  void clearAllCache();

  /**
   * 查询全量数据并展平为 Excel 行列表（用于导出，不分页）
   */
  List<InoutExcelRow> queryAllForExport(InoutDetailRequest request);
}
