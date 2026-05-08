package com.erp.service.contract;

/**
 * 合同审批流服务接口
 */
public interface ContractApprovalFlowService {

    /**
     * 创建审批流记录（合同创建时调用）
     *
     * @param contractId 合同编号
     * @param creatorId 创建人编码
     */
    void createContractCreationFlow(Integer contractId, Integer creatorId);

    /**
     * 更新审批流记录为合同审核（合同审核通过时调用）
     *
     * @param contractId 合同编号
     * @param approverId 审批人编码
     * @param approvalOpinion 审批意见
     */
    void updateToContractAuditFlow(Integer contractId, Integer approverId, String approvalOpinion);

    /**
     * 创建收运通知审批流记录（创建收运通知单时调用）
     *
     * @param contractId 合同编号
     * @param creatorId 创建人编码
     */
    void createPickupNoticeFlow(Integer contractId, Integer creatorId);

    /**
     * 创建入库完成审批流记录（入库单审核通过时调用）
     *
     * @param contractId 合同编号
     * @param auditorId 审核人编码
     */
    void createWarehousingFlow(Integer contractId, Integer auditorId);
}
















