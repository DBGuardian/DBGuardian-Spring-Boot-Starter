package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.production.WarehousingWasteItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 入库单危废明细 Mapper
 */
@Mapper
public interface WarehousingWasteItemMapper extends BaseMapper<WarehousingWasteItem> {

    /**
     * 根据入库单编号查询危废明细列表
     */
    List<WarehousingWasteItem> selectByWarehousingId(@Param("warehousingId") Integer warehousingId);

    /**
     * 根据入库单编号列表批量查询危废明细
     * 用于优化N+1查询问题，避免循环查询
     *
     * @param warehousingIds 入库单编号列表
     * @return 危废明细列表
     */
    List<WarehousingWasteItem> selectByWarehousingIds(@Param("warehousingIds") List<Integer> warehousingIds);
}


