package com.erp.service.settlement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.settlement.dto.*;

import java.util.List;

/**
 * 业务费服务接口
 */
public interface BusinessFeeService {

    /**
     * 分页查询业务费列表
     */
    IPage<BusinessFeeListItemDTO> getBusinessFeePage(BusinessFeeQueryDTO queryDTO);

    /**
     * 获取业务费统计信息
     */
    BusinessFeeStatisticsDTO getBusinessFeeStatistics();

    /**
     * 获取业务费详情
     */
    BusinessFeeDetailDTO getBusinessFeeDetail(Integer id);

    /**
     * 删除业务费
     */
    void deleteBusinessFee(Integer id, Integer deleteUserId);

    /**
     * 新增业务费详情数据（主表+明细表）
     * 仅用于首次创建，若包含双方向明细会自动拆分
     *
     * @param createDTO      新增请求DTO
     * @param operatorUserId 操作人ID
     * @return 新增结果
     */
    BusinessFeeCreateResultDTO createBusinessFeeDetail(BusinessFeeCreateDTO createDTO, Integer operatorUserId);

    /**
     * 修改业务费详情数据（单方向更新）
     *
     * @param businessSeq    业务序号
     * @param updateDTO      修改请求DTO
     * @param operatorUserId 操作人ID
     */
    void updateBusinessFeeDetail(Integer businessSeq, BusinessFeeDetailUpdateDTO updateDTO, Integer operatorUserId);

    /**
     * 提交审核
     * 将草稿/已驳回状态的业务费结算单提交为「待审核」
     *
     * @param id             业务序号
     * @param operatorUserId 操作人ID
     */
    void submitAudit(Integer id, Integer operatorUserId);

    /**
     * 取消审核
     * 将「待审核」状态的业务费结算单撤回为「草稿」
     *
     * @param id             业务序号
     * @param operatorUserId 操作人ID
     */
    void cancelAudit(Integer id, Integer operatorUserId);

    /**
     * 批量查询危废结算单关联入库单聚合数据
     * <p>
     * 传入多个危废结算单编号，查询每个结算单关联的入库单危废明细，
     * 按废物类别+废物代码+废物名称合并有价类/无价类重量，返回聚合后的明细行列表。
     * 用于业务费「从关联结算单导入」时自动填充废物信息和重量数据。
     *
     * @param request 请求参数（含 settlementIds 列表）
     * @return 聚合后的明细行列表
     */
    SettlementWasteAggregateResponse getSettlementWarehousingAggregate(SettlementWasteAggregateRequest request);

    /**
     * 批量提交审核
     * 将多个草稿/已驳回状态的业务费结算单提交为「审核中」
     *
     * @param businessSeqs 业务序号列表
     * @param operatorUserId 操作人ID
     * @return 批量操作结果
     */
    BusinessFeeBatchOperationResult batchSubmitAudit(List<Integer> businessSeqs, Integer operatorUserId);

    /**
     * 批量撤回审核
     * 将多个审核中状态的业务费结算单撤回为「待审核」
     *
     * @param businessSeqs 业务序号列表
     * @param operatorUserId 操作人ID
     * @return 批量操作结果
     */
    BusinessFeeBatchOperationResult batchCancelAudit(List<Integer> businessSeqs, Integer operatorUserId);

    /**
     * 批量删除业务费
     * 删除多个草稿/待审核/已驳回状态的业务费结算单
     *
     * @param businessSeqs 业务序号列表
     * @param operatorUserId 操作人ID
     * @return 批量操作结果
     */
    BusinessFeeBatchOperationResult batchDelete(List<Integer> businessSeqs, Integer operatorUserId);

    /**
     * 审核业务费结算单（通过/驳回）
     * <p>
     * 通过时：
     * - BUSINESS_FEE_HEADER: 状态改为"已审核"，记录审核人编码、审核时间、审核意见
     * - OA_APPROVAL_RECORD: 状态改为"已通过"，记录审核人编码、审核人姓名、审核时间
     * <p>
     * 驳回时：
     * - BUSINESS_FEE_HEADER: 状态改为"已驳回"，记录审核人编码、审核时间、审核意见
     * - OA_APPROVAL_RECORD: 状态改为"已驳回"，记录审核人编码、审核人姓名、审核时间
     *
     * @param businessSeq 业务序号
     * @param auditResult 审核结果（通过/驳回）
     * @param auditOpinion 审核意见
     * @param operatorUserId 审核人编码
     * @param operatorUserName 审核人姓名
     * @param skipPermissionCheck 是否跳过权限检查（OA回调时使用）
     */
    void auditBusinessFee(Integer businessSeq, String auditResult, String auditOpinion,
                          Integer operatorUserId, String operatorUserName, boolean skipPermissionCheck);
}
