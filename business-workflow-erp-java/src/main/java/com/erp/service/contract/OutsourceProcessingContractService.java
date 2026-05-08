package com.erp.service.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.contract.dto.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 委外处理合同服务接口
 */
public interface OutsourceProcessingContractService {

    /**
     * 委外处理合同分页查询
     *
     * @param request 查询条件
     * @return 分页结果（不含明细）
     */
    IPage<OutsourceProcessingContractPageResponse> getContractPage(OutsourceProcessingContractPageRequest request);

    /**
     * 获取委外处理合同详情
     *
     * @param contractId 合同编号
     * @return 合同详情
     */
    OutsourceProcessingContractDetailResponse getContractDetail(Integer contractId);

    /**
     * 新增委外处理合同
     *
     * @param request 新增请求
     * @param file 合同PDF文件
     * @return 合同详情
     */
    OutsourceProcessingContractDetailResponse createContract(OutsourceProcessingContractCreateRequest request, MultipartFile file);

    /**
     * 更新委外处理合同
     *
     * @param contractId 合同编号
     * @param request 更新请求
     * @param file 合同PDF文件
     * @return 更新后的合同详情
     */
    OutsourceProcessingContractDetailResponse updateContract(Integer contractId, OutsourceProcessingContractUpdateRequest request, MultipartFile file);

    /**
     * 删除委外处理合同
     *
     * @param contractId 合同编号
     * @return 删除结果
     */
    OutsourceProcessingContractOperationResponse deleteContract(Integer contractId);

    /**
     * 批量删除委外处理合同
     *
     * @param contractIds 合同编号列表
     * @return 删除结果
     */
    OutsourceProcessingContractOperationResponse batchDeleteContract(java.util.List<Integer> contractIds);

    /**
     * 更新委外处理合同状态
     *
     * @param contractId 合同编号
     * @param contractStatus 新状态
     * @param auditOpinion 审核意见
     * @return 更新后的合同详情
     */
    OutsourceProcessingContractDetailResponse updateContractStatus(Integer contractId, String contractStatus, String auditOpinion);

    /**
     * 获取委外处理合同下拉列表
     *
     * @param keyword 搜索关键字
     * @return 下拉列表
     */
    java.util.List<OutsourceProcessingContractSelectResponse> getContractSelectList(String keyword);

    /**
     * 批量提交审核
     *
     * @param contractIds 合同编号列表
     * @return 提交结果
     */
    OutsourceProcessingContractBatchAuditResponse batchSubmitAudit(java.util.List<Integer> contractIds);

    /**
     * 批量撤回审核
     *
     * @param contractIds 合同编号列表
     * @return 撤回结果
     */
    OutsourceProcessingContractBatchAuditResponse batchWithdrawAudit(java.util.List<Integer> contractIds);
}
