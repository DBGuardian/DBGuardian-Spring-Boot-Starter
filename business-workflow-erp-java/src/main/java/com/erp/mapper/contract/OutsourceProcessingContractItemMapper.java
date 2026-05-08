package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.OutsourceProcessingContractItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 委外处理合同报价组Mapper接口
 *
 * 对应表：OUTSOURCE_PROCESSING_CONTRACT_ITEM
 */
@Mapper
public interface OutsourceProcessingContractItemMapper extends BaseMapper<OutsourceProcessingContractItem> {

    /**
     * 根据合同编号查询报价组列表
     *
     * @param contractId 合同编号
     * @return 报价组列表
     */
    List<OutsourceProcessingContractItem> selectByContractId(@Param("contractId") Integer contractId);

    /**
     * 删除合同下的所有报价组
     *
     * @param contractId 合同编号
     */
    void deleteByContractId(@Param("contractId") Integer contractId);
}
