package com.erp.controller.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 日记账Excel导入结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundDiaryImportResult {

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 成功导入数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 跳过数
     */
    private Integer skipCount;

    /**
     * 失败详情
     */
    private List<FailDetail> failDetails;

    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return failCount == null || failCount == 0;
    }

    /**
     * 获取失败数（兼容方法名）
     */
    public Integer getFailureCount() {
        return failCount;
    }

    /**
     * 获取跳过数
     */
    public Integer getSkipCount() {
        return skipCount;
    }

    /**
     * 失败详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailDetail {

        /**
         * 行号（从1开始）
         */
        private int rowNumber;

        /**
         * 错误信息
         */
        private String errorMessage;
    }
}