package com.erp.mapper.report;

import com.erp.controller.report.dto.InoutDetailRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 出入库明细表 Mapper
 * 
 * 查询优化：
 * 1. 使用原生SQL JOIN查询获取入库数据（1次查询）
 * 2. 批量查询出库数据并按废物代码+名称分组（1次查询）
 * 3. 在内存中计算库存数量
 */
@Mapper
public interface InoutDetailMapper {

  /**
   * 查询入库明细数据（JOIN多表）
   * 
   * 关联路径：
   * WAREHOUSING_WASTE_ITEM → WAREHOUSING → WEIGHING_SLIP → VEHICLE
   * WAREHOUSING → PICKUP_NOTICE → CONTRACT → EMPLOYEE
   * WAREHOUSING_WASTE_ITEM → HAZARDOUS_WASTE_CATEGORY
   * 
   * @param category 分类：经营/自产/盘库
   * @param contractNo 合同编号
   * @param wasteCode 废物代码
   * @param wasteName 废物名称
   * @param dateStart 开始日期
   * @param dateEnd 结束日期
   * @return 入库明细列表
   */
  List<InoutDetailRow> queryInboundDetail(
      @Param("category") String category,
      @Param("contractNo") String contractNo,
      @Param("wasteCode") String wasteCode,
      @Param("wasteName") String wasteName,
      @Param("dateStart") String dateStart,
      @Param("dateEnd") String dateEnd
  );

  /**
   * 查询出库数据并按废物代码+名称分组
   * 
   * 返回格式：
   * {
   *   "wasteKey": "废物代码|废物名称",
   *   "outboundQty": 出库数量
   * }
   * 
   * @return 出库数据Map列表
   */
  List<Map<String, Object>> queryOutboundGrouped();

  /**
   * 查询所有入库明细（不分页，用于导出）
   */
  List<InoutDetailRow> queryAllInboundDetail(
      @Param("category") String category,
      @Param("contractNo") String contractNo,
      @Param("wasteCode") String wasteCode,
      @Param("wasteName") String wasteName,
      @Param("dateStart") String dateStart,
      @Param("dateEnd") String dateEnd
  );
}
