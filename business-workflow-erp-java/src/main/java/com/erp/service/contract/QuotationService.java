package com.erp.service.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.contract.dto.QuotationAuditRequest;
import com.erp.controller.contract.dto.QuotationBatchAuditRequest;
import com.erp.controller.contract.dto.QuotationCreateRequest;
import com.erp.controller.contract.dto.QuotationDetailResponse;
import com.erp.controller.contract.dto.QuotationPageRequest;
import com.erp.controller.contract.dto.QuotationPageResponse;
import com.erp.controller.contract.dto.QuotationUpdateRequest;
import com.erp.service.contract.dto.OaApprovalSubmitResult;

import java.util.List;

/**
 * 报价单管理服务接口
 */
public interface QuotationService {

    /**
     * 新增报价单
     *
     * @param request 报价单信息
     * @return 报价单详情
     */
    QuotationDetailResponse createQuotation(QuotationCreateRequest request);

    /**
     * 更新报价单
     *
     * @param request 报价单信息
     */
    void updateQuotation(QuotationUpdateRequest request);

    /**
     * 报价单详情
     *
     * @param quotationId 报价单编号
     * @return 详情
     */
    QuotationDetailResponse getQuotationDetail(Integer quotationId);

    /**
     * 报价单分页查询
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<QuotationPageResponse> getQuotationPage(QuotationPageRequest request);

    /**
     * 审核报价单
     *
     * @param request 审核信息
     */
    void auditQuotation(QuotationAuditRequest request);

    /**
     * 批量审核报价单
     *
     * @param request 批量审核信息
     */
    void batchAudit(QuotationBatchAuditRequest request);

    /**
     * 导出报价单列表
     *
     * @param request 查询条件
     * @return 报价单列表
     */
    List<QuotationPageResponse> listQuotationsForExport(QuotationPageRequest request);

    /**
     * 生成报价单PDF
     *
     * @param quotationId 报价单编号
     * @return 报价单详情（包含PDF信息）
     */
    QuotationDetailResponse generatePdf(Integer quotationId);

    /**
     * 提交报价单审核
     * 将报价单提交到OA审批系统，创建OA审批记录，并更新报价单状态为"待审批"
     *
     * @param quotationId 报价单编号
     * @return OA审批记录ID和审批编号
     */
    OaApprovalSubmitResult submitForApproval(Integer quotationId);

    /**
     * 批量撤回报价单
     * 将"审核中"状态的报价单撤回为"待审核"状态
     *
     * @param quotationIds 报价单ID列表
     */
    void batchRevoke(List<Integer> quotationIds);
}































