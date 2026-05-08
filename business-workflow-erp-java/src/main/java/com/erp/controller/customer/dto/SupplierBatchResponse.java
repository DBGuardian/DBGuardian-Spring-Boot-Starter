package com.erp.controller.customer.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 供应商批量操作响应
 * 用于创建、更新、删除等批量操作的结果反馈
 */
@Data
public class SupplierBatchResponse implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 详细结果列表
     */
    private List<SupplierBatchResult> results = new ArrayList<>();

    /**
     * 操作类型（CREATE/UPDATE/DELETE）
     */
    private String operationType;

    /**
     * 操作耗时（毫秒）
     */
    private Long duration;

    /**
     * 创建成功的响应
     */
    public static SupplierBatchResponse success(List<SupplierBatchResult> results, String operationType) {
        SupplierBatchResponse response = new SupplierBatchResponse();
        response.setResults(results);
        response.setTotalCount(results.size());
        response.setSuccessCount((int) results.stream().filter(result -> result.getSuccess() != null && result.getSuccess()).count());
        response.setFailCount(response.getTotalCount() - response.getSuccessCount());
        response.setOperationType(operationType);
        return response;
    }

    /**
     * 创建单个操作成功的响应
     */
    public static SupplierBatchResponse singleSuccess(SupplierBatchResult result, String operationType) {
        return success(Arrays.asList(result), operationType);
    }

    /**
     * 判断操作是否完全成功
     */
    public boolean isAllSuccess() {
        return failCount == 0;
    }

    /**
     * 判断操作是否有失败
     */
    public boolean hasFailures() {
        return failCount > 0;
    }
}
