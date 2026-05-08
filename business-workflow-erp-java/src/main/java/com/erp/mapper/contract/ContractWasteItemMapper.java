package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.ContractWasteItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 合同危废条目明细Mapper接口
 *
 * 对应表：CONTRACT_WASTE_ITEM
 */
@Mapper
public interface ContractWasteItemMapper extends BaseMapper<ContractWasteItem> {

    /**
     * 根据合同条目编号查询危废条目明细列表
     *
     * @param contractItemId 合同条目编号
     * @return 危废条目明细列表
     */
    List<ContractWasteItem> selectByContractItemId(Integer contractItemId);

    /**
     * 根据合同条目编号列表批量查询危废条目明细
     *
     * @param contractItemIds 合同条目编号列表
     * @return 危废条目明细列表
     */
    List<ContractWasteItem> selectByContractItemIds(List<Integer> contractItemIds);

    /**
     * 根据合同条目编号删除危废条目明细
     *
     * @param contractItemId 合同条目编号
     * @return 删除数量
     */
    int deleteByContractItemId(Integer contractItemId);

    /**
     * 根据合同条目编号列表批量删除危废条目明细
     *
     * @param contractItemIds 合同条目编号列表
     * @return 删除数量
     */
    int deleteByContractItemIds(List<Integer> contractItemIds);

    /**
     * 查询被合同引用的报价危废明细编号列表
     *
     * @param quotationWasteItemIds 报价危废明细编号列表
     * @return 被引用的报价危废明细编号列表
     */
    List<Integer> selectReferencedQuotationWasteItemIds(List<Integer> quotationWasteItemIds);

    /**
     * 根据合同条目编号列表批量查询payer信息
     * 用于优化N+1查询问题，避免循环查询
     *
     * @param contractItemIds 合同条目编号列表
     * @return 包含contractItemId和payer的映射列表
     */
    List<com.erp.controller.finance.dto.ContractItemPayerDTO> selectPayerByContractItemIds(@org.apache.ibatis.annotations.Param("contractItemIds") List<Integer> contractItemIds);

    /**
     * 根据合同号查询合同废物明细列表
     * 通过JOIN ContractItem表来关联合同号
     *
     * @param contractCode 合同号
     * @return 合同废物明细列表
     */
    List<ContractWasteItem> selectByContractCode(@org.apache.ibatis.annotations.Param("contractCode") String contractCode);
}



