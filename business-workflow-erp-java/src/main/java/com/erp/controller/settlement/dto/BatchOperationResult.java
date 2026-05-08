package com.erp.controller.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量操作结果DTO
 * 用于返回批量操作的统计信息（成功数量、失败数量、失败详情等）
 *
 * @author ERP System
 * @date 2026-04-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult {

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failCount;

    /**
     * 失败原因列表
     */
    private java.util.List<FailureDetail> failures;

    /**
     * 失败详情内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureDetail {
        /**
         * 结算单ID
         */
        private Long settlementId;

        /**
         * 结算单号
         */
        private String settlementCode;

        /**
         * 失败原因
         */
        private String reason;
    }

    /**
     * 创建成功结果
     */
    public static BatchOperationResult success(int count) {
        return new BatchOperationResult(count, 0, null);
    }

    /**
     * 创建失败结果
     */
    public static BatchOperationResult fail(int count) {
        return new BatchOperationResult(0, count, null);
    }

    /**
     * 创建混合结果
     */
    public static BatchOperationResult of(int successCount, int failCount, java.util.List<FailureDetail> failures) {
        return new BatchOperationResult(successCount, failCount, failures);
    }

    /**
     * 判断是否有失败
     */
    public boolean hasFailures() {
        return failCount > 0;
    }

    /**
     * 判断是否全部成功
     */
    public boolean isAllSuccess() {
        return failCount == 0;
    }

    /**
     * 获取总数量
     */
    public int getTotalCount() {
        return successCount + failCount;
    }
}
