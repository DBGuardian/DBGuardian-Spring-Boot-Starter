package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.BusinessContractWasteInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务合同组内危废项 Mapper
 */
@Mapper
public interface BusinessContractWasteInfoMapper extends BaseMapper<BusinessContractWasteInfo> {

    /**
     * 根据报价组ID查询组内危废项列表
     */
    List<BusinessContractWasteInfo> selectByWasteItemId(Integer wasteItemId);

    /**
     * 根据报价组ID列表查询组内危废项列表
     */
    List<BusinessContractWasteInfo> selectByWasteItemIds(@Param("wasteItemIds") List<Integer> wasteItemIds);

    /**
     * 根据报价组ID删除组内危废项
     */
    int deleteByWasteItemId(Integer wasteItemId);

    /**
     * 根据报价组ID列表批量删除组内危废项
     */
    int deleteByWasteItemIds(@Param("wasteItemIds") List<Integer> wasteItemIds);
}
