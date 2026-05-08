package com.erp.service.contract;

import lombok.Data;

import java.util.List;

/**
 * 委外处理合同批量审核响应
 * 统一响应格式，包含统计信息
 */
@Data
public class OutsourceProcessingContractBatchAuditResponse {

    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 成功的合同ID列表
     */
    private List<Integer> successIds;

    /**
     * 失败的合同ID列表
     */
    private List<Integer> failedIds;

    /**
     * 失败原因列表
     */
    private List<FailedReason> failedReasons;

    /**
     * 是否全部成功
     */
    private Boolean allSuccess;

    /**
     * 操作耗时（毫秒）
     */
    private Long duration;

    /**
     * 操作类型：SUBMIT_AUDIT / WITHDRAW_AUDIT
     */
    private String operationType;

    @Data
    public static class FailedReason {
        /**
         * 合同ID
         */
        private Integer contractId;

        /**
         * 失败原因
         */
        private String reason;
    }
}
