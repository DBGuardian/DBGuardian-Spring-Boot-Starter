package com.erp.mapper.settlement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.settlement.BusinessFeeItemWasteInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务结算明细危废信息子表 Mapper
 */
@Mapper
public interface BusinessFeeItemWasteInfoMapper extends BaseMapper<BusinessFeeItemWasteInfo> {

    /**
     * 按明细序号查询该明细行下的所有危废信息
     *
     * @param itemSeq 明细序号
     * @return 危废信息列表
     */
    List<BusinessFeeItemWasteInfo> selectByItemSeq(@Param("itemSeq") Integer itemSeq);

    /**
     * 按多个明细序号批量查询危废信息（减少 N+1 查询）
     *
     * @param itemSeqs 明细序号列表
     * @return 危废信息列表
     */
    List<BusinessFeeItemWasteInfo> selectByItemSeqs(@Param("itemSeqs") List<Integer> itemSeqs);
}
