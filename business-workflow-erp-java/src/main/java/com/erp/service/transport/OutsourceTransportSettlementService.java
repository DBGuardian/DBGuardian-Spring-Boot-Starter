package com.erp.service.transport;

import com.erp.controller.transport.dto.*;
import com.erp.entity.transport.OutsourceTransportSettlementSlip;

import java.util.List;

/**
 * 委外运输结算 Service 接口
 */
public interface OutsourceTransportSettlementService {

    /**
     * 分页查询结算单列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    OutsourceSettlementPageResponse getPage(OutsourceSettlementPageRequest request);

    /**
     * 查询结算单详情
     *
     * @param settlementId 结算单编号
     * @return 结算单详情
     */
    OutsourceSettlementResponse getDetail(Integer settlementId);

    /**
     * 创建结算单（简单模式，仅创建单个结算单）
     * 后端自动根据选中的总磅单汇总计算结算数量和金额
     *
     * @param request 创建请求
     * @return 新增结算单的编号
     */
    Integer create(OutsourceSettlementSaveRequest request);

    /**
     * 新增结算单（支持收付款拆分）
     * - 空数据允许，只验证数据库必填字段
     * - 若包含收付款混合明细会自动拆分，生成两个结算单
     * - 价外服务归属收款单
     *
     * @param request 新增请求
     * @param operatorUserId 操作人ID
     * @return 创建结果（含拆分信息）
     */
    OutsourceSettlementCreateResultDTO createWithSplit(OutsourceSettlementCreateRequest request, Integer operatorUserId);

    /**
     * 更新结算单
     * 后端自动重新计算结算数量和金额
     *
     * @param settlementId 结算单编号
     * @param request 更新请求
     */
    void update(Integer settlementId, OutsourceSettlementSaveRequest request);

    /**
     * 更新结算单（使用 OutsourceSettlementCreateRequest）
     * 支持完整数据更新，包括价外服务
     *
     * @param settlementId 结算单编号
     * @param request 更新请求
     */
    void updateFromCreateRequest(Integer settlementId, OutsourceSettlementCreateRequest request);

    /**
     * 删除结算单（仅待审核状态可删除）
     *
     * @param settlementId 结算单编号
     */
    void delete(Integer settlementId);

    /**
     * 基于总磅单分页查询可结算记录
     * 每个总磅单代表一趟，返回趟数、重量、距离等汇总信息
     *
     * @param request 查询条件
     * @return 分页结果
     */
    SettlementSlipPageResponse getAvailableSlipsForSettlement(SettlementDispatchOrderRequest request);

    /**
     * 查询结算单关联的总磅单列表
     *
     * @param settlementId 结算单编号
     * @return 关联的总磅单列表
     */
    List<OutsourceTransportSettlementSlipVO> getSlipsBySettlementId(Integer settlementId);

    /**
     * 批量提交审核
     * 功能描述：批量提交多个委外运输结算单进行审核，只有待审核、已驳回状态的结算单才能提交审核
     * 提交后结算单状态改为"审核中"
     * 在OA审核记录表(OA_APPROVAL_RECORD)中有记录则审核次数+1，如没有则新增一条记录
     * 来源表中文名称为"委外运输结算"
     * 接口地址：POST /api/outsource-settlement/batch/submit-audit
     *
     * @param settlementIds 结算单编号列表
     * @param operatorUserId 操作人编码
     * @return 批量操作结果
     */
    OutsourceSettlementBatchOperationResult batchSubmitAudit(List<Integer> settlementIds, Integer operatorUserId);

    /**
     * 批量撤回审核
     * 功能描述：批量撤回多个审核中的委外运输结算单，只有审核中状态的结算单才能撤回
     * 撤回后结算单状态改为"待审核"
     * 在OA审核记录表(OA_APPROVAL_RECORD)中将审核状态改为"已撤回"，审核次数-1，最低为0
     * 接口地址：POST /api/outsource-settlement/batch/cancel-audit
     *
     * @param settlementIds 结算单编号列表
     * @param operatorUserId 操作人编码
     * @return 批量操作结果
     */
    OutsourceSettlementBatchOperationResult batchCancelAudit(List<Integer> settlementIds, Integer operatorUserId);

    /**
     * 批量删除结算单
     * 功能描述：批量删除多个委外运输结算单，只有待审核、已驳回状态的结算单才能删除
     * 接口地址：POST /api/outsource-settlement/batch/delete
     *
     * @param settlementIds 结算单编号列表
     * @param operatorUserId 操作人编码
     * @return 批量操作结果
     */
    OutsourceSettlementBatchOperationResult batchDelete(List<Integer> settlementIds, Integer operatorUserId);

    /**
     * 审核结算单
     * 功能描述：审核通过或驳回委外运输结算单，只有审核中状态可审核
     *
     * 审核通过时：
     * - 结算单状态改为"已审核"
     * - 记录审核意见、审核人编码、审核人姓名、审核时间
     * - OA审核记录表审核状态改为"已通过"，记录审核人编码、审核人姓名、审核时间
     *
     * 审核驳回时：
     * - 结算单状态改为"已驳回"
     * - 记录审核意见、审核人编码、审核人姓名、审核时间
     * - OA审核记录表审核状态改为"已驳回"，记录审核人编码、审核人姓名、审核时间
     *
     * 接口地址：POST /api/outsource-settlement/audit/{settlementId}
     *
     * @param settlementId 结算单编号
     * @param auditResult 审核结果（approved-通过/rejected-驳回）
     * @param auditOpinion 审核意见（必填）
     * @param auditorId 审核人编码
     * @param auditorName 审核人姓名
     */
    void auditSettlement(Integer settlementId, String auditResult, String auditOpinion, Integer auditorId, String auditorName);

    /**
     * 取消审核（撤回）
     * 功能描述：撤回审核中的结算单
     * 接口地址：POST /api/outsource-settlement/cancel-audit/{settlementId}
     *
     * @param settlementId 结算单编号
     * @param operatorUserId 操作人编码
     */
    void cancelAudit(Integer settlementId, Integer operatorUserId);
}
