package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.production.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存表 Mapper
 */
@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 根据废物代码、废物类别、废物名称查询库存记录
     * 用于入库单已结算时匹配库存并累加重量
     *
     * @param wasteCode     废物代码
     * @param wasteCategory 危废类别编码
     * @param wasteName     废物名称
     * @return 库存记录，不存在则返回 null
     */
    Stock selectByWasteCodeAndCategoryAndName(
            @Param("wasteCode") String wasteCode,
            @Param("wasteCategory") String wasteCategory,
            @Param("wasteName") String wasteName
    );

    /**
     * 审核出库单时按多字段匹配库存记录
     */
    List<Stock> selectAuditMatchStocks(
            @Param("hazardousWasteItemId") Integer hazardousWasteItemId,
            @Param("wasteCode") String wasteCode,
            @Param("wasteName") String wasteName,
            @Param("location") String location
    );
}
