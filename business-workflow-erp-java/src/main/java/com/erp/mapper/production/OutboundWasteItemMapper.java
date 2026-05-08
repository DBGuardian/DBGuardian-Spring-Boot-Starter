package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.production.OutboundWasteItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 出库单危废明细 Mapper
 */
@Mapper
public interface OutboundWasteItemMapper extends BaseMapper<OutboundWasteItem> {

    /**
     * 根据出库单编号查询危废明细列表
     */
    List<OutboundWasteItem> selectByOutboundId(@Param("outboundId") Integer outboundId);

    /**
     * 根据出库单编号删除所有危废明细
     */
    int deleteByOutboundId(@Param("outboundId") Integer outboundId);
}
