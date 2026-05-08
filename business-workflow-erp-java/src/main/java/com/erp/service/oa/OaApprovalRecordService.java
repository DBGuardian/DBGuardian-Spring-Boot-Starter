package com.erp.service.oa;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.entity.oa.OaApprovalRecord;

/**
 * OA审核记录服务接口
 *
 * @author ERP System
 * @date 2026-04-06
 */
public interface OaApprovalRecordService {

    /**
     * OA审核记录统计DTO
     */
    class OaApprovalStatistics {
        private Long totalCount;
        private Long pendingCount;
        private Long submittedCount;
        private Long passedCount;
        private Long rejectedCount;
        private Long withdrawnCount;
        private Long longPendingCount;

        public Long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Long totalCount) {
            this.totalCount = totalCount;
        }

        public Long getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(Long pendingCount) {
            this.pendingCount = pendingCount;
        }

        public Long getSubmittedCount() {
            return submittedCount;
        }

        public void setSubmittedCount(Long submittedCount) {
            this.submittedCount = submittedCount;
        }

        public Long getPassedCount() {
            return passedCount;
        }

        public void setPassedCount(Long passedCount) {
            this.passedCount = passedCount;
        }

        public Long getRejectedCount() {
            return rejectedCount;
        }

        public void setRejectedCount(Long rejectedCount) {
            this.rejectedCount = rejectedCount;
        }

        public Long getWithdrawnCount() {
            return withdrawnCount;
        }

        public void setWithdrawnCount(Long withdrawnCount) {
            this.withdrawnCount = withdrawnCount;
        }

        public Long getLongPendingCount() {
            return longPendingCount;
        }

        public void setLongPendingCount(Long longPendingCount) {
            this.longPendingCount = longPendingCount;
        }
    }

    /**
     * 获取OA审核统计信息
     *
     * @param viewScope 视图范围：pending-待我审核、submitted-我发起的、processed-已处理、all-全部
     * @param submitterId 提交人ID（当前用户）
     * @param approverId 审核人ID（当前用户）
     * @return 统计数据
     */
    OaApprovalStatistics getStatistics(String viewScope, Integer submitterId, Integer approverId);

    /**
     * 分页查询OA审核记录列表
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param keyword 关键字搜索
     * @param businessType 业务类型
     * @param approvalStatus 审核状态
     * @param viewScope 视图范围
     * @param submitterId 提交人ID
     * @param approverId 审核人ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param unapprovedDays 未审核天数
     * @return 分页结果
     */
    IPage<OaApprovalRecord> getApprovalPage(
            Integer page,
            Integer pageSize,
            String keyword,
            String businessType,
            String approvalStatus,
            String viewScope,
            Integer submitterId,
            Integer approverId,
            String startDate,
            String endDate,
            Integer unapprovedDays
    );

    /**
     * 获取OA审核详情
     *
     * @param approvalRecordId 审核记录ID
     * @return OA审核记录
     */
    OaApprovalRecord getApprovalDetail(Integer approvalRecordId);

    /**
     * 执行审核操作（通过/驳回）
     *
     * @param approvalRecordId 审核记录ID
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @param result 审核结果：通过/驳回
     * @param opinion 审核意见
     * @param approverId 审核人ID
     * @param approverName 审核人姓名
     * @return 更新后的审核记录
     */
    OaApprovalRecord approve(
            Integer approvalRecordId,
            String sourceTable,
            Integer sourceId,
            String result,
            String opinion,
            Integer approverId,
            String approverName
    );

    /**
     * 提交审核
     *
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @param sourceTableName 来源表中文名称
     * @param sourceNo 关联单号
     * @param title 审核标题
     * @param submitterId 提交人ID
     * @param submitterName 提交人姓名
     * @return 新创建的审核记录
     */
    OaApprovalRecord submit(
            String sourceTable,
            Integer sourceId,
            String sourceTableName,
            String sourceNo,
            String title,
            Integer submitterId,
            String submitterName
    );

    /**
     * 重新提交审核（驳回后重新提交）
     *
     * @param originalRecordId 原审核记录ID
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @param submitterId 提交人ID
     * @param submitterName 提交人姓名
     * @return 新创建的审核记录
     */
    OaApprovalRecord resubmit(
            Integer originalRecordId,
            String sourceTable,
            Integer sourceId,
            Integer submitterId,
            String submitterName
    );

    /**
     * 根据来源查询待审核的OA审批记录
     *
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @return 待审核的OA审批记录，如果没有则返回null
     */
    OaApprovalRecord findPendingBySource(String sourceTable, Integer sourceId);

    /**
     * 根据来源查询最新的OA审批记录（包括已驳回状态）
     *
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @return 最新的OA审批记录，如果没有则返回null
     */
    OaApprovalRecord findLatestBySource(String sourceTable, Integer sourceId);

    /**
     * 根据来源查询已撤回状态的OA审批记录
     *
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @return 已撤回状态的OA审批记录，如果没有则返回null
     */
    OaApprovalRecord findWithdrawnBySource(String sourceTable, Integer sourceId);

    /**
     * 重新激活已驳回的OA审批记录
     * 将已驳回状态的记录重新激活为待审核，审核次数+1，提交时间更新
     *
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @param submitterId 提交人ID
     * @param submitterName 提交人姓名
     * @return 更新后的OA审批记录
     */
    OaApprovalRecord reactivateRejectedRecord(
            String sourceTable,
            Integer sourceId,
            Integer submitterId,
            String submitterName
    );

    /**
     * 重新激活已撤回的OA审批记录
     * 将已撤回状态的记录重新激活为待审核，审核次数+1，提交时间更新
     *
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @param submitterId 提交人ID
     * @param submitterName 提交人姓名
     * @return 更新后的OA审批记录
     */
    OaApprovalRecord reactivateWithdrawnRecord(
            String sourceTable,
            Integer sourceId,
            Integer submitterId,
            String submitterName
    );

    /**
     * 撤回OA审批记录
     * 无论审核次数是否为1，都保留该记录；审核次数减1后最小为0，状态改为已撤回
     *
     * @param approvalRecordId 审核记录ID
     * @param sourceTable 来源表名
     * @param sourceId 来源记录ID
     * @param operatorId 操作人ID
     * @param reason 撤回原因
     */
    void cancel(Integer approvalRecordId, String sourceTable, Integer sourceId, Integer operatorId, String reason);
}
