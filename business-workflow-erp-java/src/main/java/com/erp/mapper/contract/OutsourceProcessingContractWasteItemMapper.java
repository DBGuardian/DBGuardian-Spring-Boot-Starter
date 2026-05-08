package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.OutsourceProcessingContractWasteItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 委外处理合同废物明细Mapper接口
 *
 * 对应表：OUTSOURCE_PROCESSING_CONTRACT_WASTE_ITEM
 */
@Mapper
public interface OutsourceProcessingContractWasteItemMapper extends BaseMapper<OutsourceProcessingContractWasteItem> {

    /**
     * 根据条目编号查询废物明细列表
     *
     * @param itemId 条目编号
     * @return 废物明细列表
     */
    List<OutsourceProcessingContractWasteItem> selectByItemId(@Param("itemId") Integer itemId);

    /**
     * 删除条目下的所有废物明细
     *
     * @param itemId 条目编号
     */
    void deleteByItemId(@Param("itemId") Integer itemId);

    /**
     * 根据合同编号查询所有废物明细
     *
     * @param contractId 合同编号
     * @return 废物明细列表
     */
    List<OutsourceProcessingContractWasteItem> selectByContractId(@Param("contractId") Integer contractId);
}
