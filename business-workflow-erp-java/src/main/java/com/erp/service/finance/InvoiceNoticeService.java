package com.erp.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.finance.dto.*;
import com.erp.controller.settlement.dto.SettlementQueryResultDTO;

/**
 * 开票通知单服务接口
 *
 * @author ERP System
 * @date 2026-01-06
 */
public interface InvoiceNoticeService {

    /**
     * 获取开票通知单分页列表
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<InvoiceNoticePageResponse> getInvoiceNoticePageList(InvoiceNoticePageRequest request);

    /**
     * 获取开票通知单详情
     *
     * @param noticeId 通知单ID
     * @return 详情响应
     */
    InvoiceNoticeDetailResponse getInvoiceNoticeDetail(Integer noticeId);

    /**
     * 创建开票通知单
     *
     * @param request 创建请求
     * @return 创建结果
     */
    InvoiceNoticeCreateResponse createInvoiceNotice(InvoiceNoticeCreateRequest request);

    /**
     * 更新开票通知单
     *
     * @param noticeId 通知单ID
     * @param request  更新请求
     */
    void updateInvoiceNotice(Integer noticeId, InvoiceNoticeUpdateRequest request);

    /**
     * 提交审批
     *
     * @param noticeId 通知单ID
     * @param request  提交请求
     */
    void submitInvoiceNotice(Integer noticeId, InvoiceNoticeSubmitRequest request);

    /**
     * 批量提交审核
     *
     * @param noticeIds 通知单ID列表
     */
    void batchSubmitInvoiceNotice(java.util.List<Integer> noticeIds);

    /**
     * 批量撤回开票通知单
     *
     * @param noticeIds 通知单ID列表
     */
    void batchRevokeInvoiceNotice(java.util.List<Integer> noticeIds);

    /**
     * 撤销开票通知单
     * 将状态从待开票改为待审批
     *
     * @param noticeId 通知单ID
     */
    void cancelInvoiceNotice(Integer noticeId);

    /**
     * 审批通过开票通知单
     *
     * @param noticeId 通知单ID
     * @param request  审批请求
     */
    void approveInvoiceNotice(Integer noticeId, InvoiceNoticeApproveRequest request);

    /**
     * 驳回开票通知单
     *
     * @param noticeId 通知单ID
     * @param request  驳回请求
     */
    void rejectInvoiceNotice(Integer noticeId, InvoiceNoticeRejectRequest request);

    /**
     * 获取发票关联列表（待开票和已开票状态的通知单）
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<InvoiceNoticePageResponse> getInvoiceAssociateList(InvoiceNoticePageRequest request);

    /**
     * 关联发票
     * 支持差分保存：新增该新增的，删除该删除的
     *
     * @param noticeId 通知单ID
     * @param request  关联请求
     */
    void associateInvoice(Integer noticeId, InvoiceAssociateRequest request);
    
    /**
     * 验证发票能否关联到指定通知单（检查是否已被其他通知单或结算单绑定）
     *
     * @param noticeId 通知单ID
     * @param invoiceIds 发票ID列表
     * @return 冲突信息列表（无冲突则返回空列表）
     */
    java.util.List<String> validateInvoiceAssociations(Integer noticeId, java.util.List<Integer> invoiceIds);

    /**
     * 完成关联
     * 校验至少一张发票后，状态流转为"已开票"，回填已开票张数和金额汇总
     *
     * @param noticeId 通知单ID
     * @param request  完成请求
     */
    void completeInvoiceAssociate(Integer noticeId, InvoiceAssociateCompleteRequest request);

    /**
     * 根据合同查询可用于开票的结算单
     *
     * @param contractId 合同ID
     * @param invoiceType 开票类型：开票/作废
     * @param viewScope 数据范围：SELF=仅查看自己, ALL=查看全部, null=根据权限自动判断
     * @return 结算单查询结果列表
     */
    java.util.List<SettlementQueryResultDTO> getSettlementsByContract(Integer contractId, String invoiceType, String viewScope);
}

