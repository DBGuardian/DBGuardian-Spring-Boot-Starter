package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.production.dto.OutboundDetailResponse;
import com.erp.controller.production.dto.OutboundListResponse;
import com.erp.entity.production.Outbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 出库单 Mapper
 */
@Mapper
public interface OutboundMapper extends BaseMapper<Outbound> {

    /**
     * 根据出库单号查询出库单
     */
    Outbound selectByOutboundNo(@Param("outboundNo") String outboundNo);

    /**
     * 根据出库单编号查询出库单
     */
    Outbound selectByOutboundId(@Param("outboundId") Integer outboundId);

    /**
     * 根据出库单编号查询出库详情（主表 + 明细一对多）
     */
    OutboundDetailResponse selectDetailByOutboundId(@Param("outboundId") Integer outboundId);

    /**
     * 查询指定前缀的最大出库单号（用于生成序号）
     */
    String selectMaxOutboundNoByPrefix(@Param("prefix") String prefix);

    /**
     * 统计出库单号是否已存在
     */
    int countByOutboundNo(@Param("outboundNo") String outboundNo);

    /**
     * 分页查询出库单列表
     */
    IPage<OutboundListResponse.OutboundPageResponse> selectOutboundPage(
            Page<OutboundListResponse.OutboundPageResponse> page,
            @Param("keyword") String keyword,
            @Param("outboundType") String outboundType,
            @Param("status") String status,
            @Param("destinationType") String destinationType,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );
}
