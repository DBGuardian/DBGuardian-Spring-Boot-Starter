package com.erp.controller.customer.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 供应商批量操作结果
 * 表示单个供应商的处理结果
 */
@Data
public class SupplierBatchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 供应商ID
     */
    private Integer supplierId;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息（失败时填写）
     */
    private String errorMessage;

    /**
     * 返回数据（成功时填写）
     */
    private SupplierDetailResponse data;

    /**
     * 处理耗时（毫秒）
     */
    private Long duration;

    /**
     * 创建成功结果的静态方法
     */
    public static SupplierBatchResult success(Integer supplierId, SupplierDetailResponse data) {
        SupplierBatchResult result = new SupplierBatchResult();
        result.setSupplierId(supplierId);
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    /**
     * 创建失败结果的静态方法
     */
    public static SupplierBatchResult failure(Integer supplierId, String errorMessage) {
        SupplierBatchResult result = new SupplierBatchResult();
        result.setSupplierId(supplierId);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * 创建成功结果（带耗时）的静态方法
     */
    public static SupplierBatchResult success(Integer supplierId, SupplierDetailResponse data, Long duration) {
        SupplierBatchResult result = success(supplierId, data);
        result.setDuration(duration);
        return result;
    }

    /**
     * 创建失败结果（带耗时）的静态方法
     */
    public static SupplierBatchResult failure(Integer supplierId, String errorMessage, Long duration) {
        SupplierBatchResult result = failure(supplierId, errorMessage);
        result.setDuration(duration);
        return result;
    }
}
