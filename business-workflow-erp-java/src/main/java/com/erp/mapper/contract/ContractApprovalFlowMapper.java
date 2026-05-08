package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.ContractApprovalFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合同审批流Mapper
 */
@Mapper
public interface ContractApprovalFlowMapper extends BaseMapper<ContractApprovalFlow> {

    /**
     * 根据合同编号查询审批流记录
     *
     * @param contractId 合同编号
     * @return 审批流记录列表
     */
    List<ContractApprovalFlow> selectByContractId(@Param("contractId") Integer contractId);

    /**
     * 根据合同编号和节点名称查询审批流记录
     *
     * @param contractId 合同编号
     * @param nodeName 节点名称
     * @return 审批流记录
     */
    ContractApprovalFlow selectByContractIdAndNodeName(@Param("contractId") Integer contractId, 
                                                       @Param("nodeName") String nodeName);
}
















