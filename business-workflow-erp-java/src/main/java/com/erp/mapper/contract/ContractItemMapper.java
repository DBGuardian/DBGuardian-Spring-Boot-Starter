package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.ContractItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合同条目Mapper接口
 *
 * 对应表：CONTRACT_ITEM
 */
@Mapper
public interface ContractItemMapper extends BaseMapper<ContractItem> {

    /**
     * 根据合同编号查询合同条目列表
     *
     * @param contractId 合同编号
     * @return 合同条目列表
     */
    List<ContractItem> selectByContractId(@Param("contractId") Integer contractId);

    /**
     * 根据合同编号列表批量查询合同条目
     *
     * @param contractIds 合同编号列表
     * @return 合同条目列表
     */
    List<ContractItem> selectByContractIds(@Param("contractIds") List<Integer> contractIds);

    /**
     * 根据合同编号删除合同条目
     *
     * @param contractId 合同编号
     * @return 删除数量
     */
    int deleteByContractId(Integer contractId);

    /**
     * 根据合同编号查询合同条目及其危废条目明细（联合查询）
     *
     * @param contractId 合同编号
     * @return 合同条目及其危废条目明细的联合结果
     */
    List<com.erp.controller.contract.dto.ContractItemWithWasteItems> selectItemsWithWasteItemsByContractId(@Param("contractId") Integer contractId);

    /**
     * 查询被合同引用的报价条目编号列表
     *
     * @param quotationItemIds 报价条目编号列表
     * @return 被引用的报价条目编号列表
     */
    List<Integer> selectReferencedQuotationItemIds(List<Integer> quotationItemIds);
}



