package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.production.WeighingSlip;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 总磅单Mapper
 */
@Mapper
public interface WeighingSlipMapper extends BaseMapper<WeighingSlip> {

    /**
     * 根据前缀查询最大总磅单号
     * @param prefix 前缀（如：ZBD-20250101-）
     * @return 最大总磅单号
     */
    String selectMaxWeighingSlipNoByPrefix(@Param("prefix") String prefix);

    /**
     * 根据总磅单号查询数量（用于唯一性校验）
     * @param weighingSlipNo 总磅单号
     * @return 数量
     */
    int countByWeighingSlipNo(@Param("weighingSlipNo") String weighingSlipNo);

    /**
     * 根据序号查询数量（用于唯一性校验）
     * @param sequence 序号
     * @return 数量
     */
    int countBySequence(@Param("sequence") String sequence);

    /**
     * 分页查询总磅单列表
     * @param page 分页对象
     * @param keyword 关键字（总磅单号/序号/车号）
     * @param weighingSlipNo 总磅单号
     * @param sequence 序号
     * @param plateNo 车号
     * @param status 状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @param creatorFilter 创建人编码过滤（null表示不限制）
     * @return 分页结果
     */
    IPage<WeighingSlip> selectWeighingSlipPage(
            Page<WeighingSlip> page,
            @Param("keyword") String keyword,
            @Param("weighingSlipNo") String weighingSlipNo,
            @Param("sequence") String sequence,
            @Param("plateNo") String plateNo,
            @Param("status") String status,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection,
            @Param("creatorFilter") Integer creatorFilter
    );

    /**
     * 根据总磅单号查询总磅单信息（使用自定义SQL，排除更新时间字段）
     * @param weighingSlipNo 总磅单号
     * @return 总磅单实体
     */
    WeighingSlip selectByWeighingSlipNo(@Param("weighingSlipNo") String weighingSlipNo);
}



