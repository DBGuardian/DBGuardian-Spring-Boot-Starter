package com.erp.controller.customer.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 供应商更新请求
 * 统一使用数组格式，单个更新也是长度为1的数组
 */
@Data
public class SupplierUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 更新数据列表（单个更新也是长度为1的数组）
     */
    @Valid
    @NotNull(message = "更新数据不能为空")
    private List<SupplierUpdateData> batchData;

    /**
     * 创建单个更新请求的静态方法
     */
    public static SupplierUpdateRequest single(@NotNull SupplierUpdateData data) {
        SupplierUpdateRequest request = new SupplierUpdateRequest();
        request.setBatchData(Arrays.asList(data));
        return request;
    }

    /**
     * 创建批量更新请求的静态方法
     */
    public static SupplierUpdateRequest batch(@NotNull List<SupplierUpdateData> batchData) {
        SupplierUpdateRequest request = new SupplierUpdateRequest();
        request.setBatchData(batchData);
        return request;
    }

    /**
     * 获取要处理的数据列表
     */
    public List<SupplierUpdateData> getDataList() {
        return batchData != null ? batchData : new ArrayList<>();
    }

    /**
     * 判断是否为批量操作（数组长度大于1）
     */
    public boolean isBatch() {
        return batchData != null && batchData.size() > 1;
    }

    /**
     * 判断是否为单个操作（数组长度等于1）
     */
    public boolean isSingle() {
        return batchData != null && batchData.size() == 1;
    }
}
