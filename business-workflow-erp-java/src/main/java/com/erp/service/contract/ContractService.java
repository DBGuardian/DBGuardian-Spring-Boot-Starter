package com.erp.service.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.contract.dto.ContractBatchMailRequest;
import com.erp.controller.contract.dto.ContractBatchSubmitAuditRequest;
import com.erp.controller.contract.dto.ContractBatchSubmitAuditResponse;
import com.erp.controller.contract.dto.ContractCreateRequest;
import com.erp.controller.contract.dto.ContractDetailResponse;
import com.erp.controller.contract.dto.ContractPageRequest;
import com.erp.controller.contract.dto.ContractPageResponse;
import com.erp.controller.contract.dto.ContractSelectResponse;
import com.erp.controller.contract.dto.ContractUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 合同管理服务接口
 */
public interface ContractService {

    /**
     * 新增合同（从客户列表生成报价单并上传合同PDF）
     *
     * @param request      合同及报价单信息
     * @param contractFile 合同PDF文件
     * @return 合同详情
     */
    ContractDetailResponse createContract(ContractCreateRequest request, MultipartFile contractFile);

    /**
     * 更新合同（支持重新上传合同PDF）
     *
     * @param request      合同信息
     * @param contractFile 合同PDF文件（可为空，为空则不变更）
     */
    void updateContract(ContractUpdateRequest request, MultipartFile contractFile);

    /**
     * 合同详情
     *
     * @param contractId 合同编号
     * @return 详情
     */
    ContractDetailResponse getContractDetail(Integer contractId);

    /**
     * 合同分页查询
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<ContractPageResponse> getContractPage(ContractPageRequest request);

    /**
     * 合同号模糊查询（用于运输申请等场景）
     *
     * @param keyword 搜索关键字（合同号或客户名称）
     * @param viewScope 数据范围：SELF/ALL/null
     * @param current 当前页码
     * @param size 每页数量
     * @return 分页结果
     */
    IPage<ContractPageResponse> searchContracts(String keyword, String viewScope, long current, long size);

    /**
     * 获取合同统计信息
     *
     * @return 统计信息
     */
    com.erp.controller.contract.dto.ContractStatistics getContractStatistics();

    /**
     * 生成合同PDF
     *
     * @param contractId 合同编号
     * @return 合同详情（包含最新PDF信息）
     */
    ContractDetailResponse generatePdf(Integer contractId);

    /**
     * 更新合同状态（审核状态变更）
     *
     * @param contractId 合同编号
     * @param contractStatus 合同状态
     * @param auditOpinion 审核意见（可选）
     */
    void updateContractStatus(Integer contractId, String contractStatus, String auditOpinion);

    /**
     * 获取合同执行进度
     *
     * @param contractId 合同编号
     * @return 合同执行进度
     */
    com.erp.controller.contract.dto.ContractProgressResponse getContractProgress(Integer contractId);

    /**
     * 获取合同的危废条目明细和价外服务
     *
     * @param contractId 合同编号
     * @return 合同危废条目明细和价外服务
     */
    com.erp.controller.contract.dto.ContractWasteItemsAndServicesResponse getContractWasteItems(Integer contractId);

    /**
     * 批量寄件
     *
     * @param request 批量寄件请求
     */
    void batchSendMail(ContractBatchMailRequest request);

    /**
     * 批量收件
     *
     * @param request 批量收件请求
     */
    void batchReceiveMail(ContractBatchMailRequest request);

    /**
     * 批量更新合同寄件/收件时间（通用方法，支持寄件或收件）
     *
     * @param request 批量寄件/收件请求
     */
    void batchUpdateMailDate(ContractBatchMailRequest request);

    /**
     * 业务费结算专用合同查询
     *
     * 查询用于业务费结算的合同列表，仅返回执行中和已完结状态的合同
     * 同时返回每个合同下未关联结算单的入库单数量（unlinkedInboundCount）
     *
     * @return 合同列表响应
     */
    com.erp.controller.contract.dto.ContractSettlementListResponse getContractForSettlement();

    /**
     * 批量提交审核
     * 
     * @param request 批量提交审核请求
     * @return 批量提交结果
     */
    ContractBatchSubmitAuditResponse batchSubmitAudit(ContractBatchSubmitAuditRequest request);

    /**
     * 批量撤回审核
     *
     * @param request 批量撤回审核请求
     * @return 批量撤回结果
     */
    ContractBatchSubmitAuditResponse batchWithdrawAudit(ContractBatchSubmitAuditRequest request);

    /**
     * 危废合同下拉列表
     *
     * @param keyword 搜索关键字（合同号或企业名称模糊搜索）
     * @param viewScope 数据范围（SELF/ALL），下拉选择场景应传ALL
     * @return 合同下拉列表
     */
    List<com.erp.controller.contract.dto.ContractSelectResponse> getContractSelectList(String keyword, String viewScope);
}




