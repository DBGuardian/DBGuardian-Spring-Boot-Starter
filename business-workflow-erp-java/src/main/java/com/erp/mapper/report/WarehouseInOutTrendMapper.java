package com.erp.mapper.report;

import com.erp.controller.report.dto.WarehouseInOutTrendResponse.WarehouseInOutCountDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仓库出入库数量变动趋势 Mapper
 *
 * 功能描述：
 * - 按时间粒度聚合入库危废明细重量（单位：吨）
 * - 入库数量来源：WAREHOUSING_WASTE_ITEM.实际收运数量，按实际入库日期聚合
 * - 出库逻辑暂未实现，接口预留，当前由 Service 层返回 0
 */
@Mapper
public interface WarehouseInOutTrendMapper {

    /**
     * 按时间粒度查询区间内的入库危废重量汇总
     */
    List<WarehouseInOutCountDTO> selectInWeightByPeriod(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("granularity") String granularity);
}
