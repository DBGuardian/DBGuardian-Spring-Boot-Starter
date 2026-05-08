package com.erp.controller.customer.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 供应商创建请求
 * 支持单个创建和批量创建
 */
@Data
public class SupplierCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单个创建数据
     */
    @Valid
    private SupplierCreateData data;

    /**
     * 批量创建数据
     */
    @Valid
    private List<SupplierCreateData> batchData;

    /**
     * 创建单个供应商的静态方法
     */
    public static SupplierCreateRequest single(@NotNull SupplierCreateData data) {
        SupplierCreateRequest request = new SupplierCreateRequest();
        request.setData(data);
        return request;
    }

    /**
     * 创建批量供应商的静态方法
     */
    public static SupplierCreateRequest batch(@NotNull List<SupplierCreateData> batchData) {
        SupplierCreateRequest request = new SupplierCreateRequest();
        request.setBatchData(batchData);
        return request;
    }

    /**
     * 获取要处理的数据列表（统一单个和批量逻辑）
     */
    public List<SupplierCreateData> getDataList() {
        if (batchData != null && !batchData.isEmpty()) {
            return batchData;
        } else if (data != null) {
            return Arrays.asList(data);
        }
        return new ArrayList<>();
    }

    /**
     * 判断是否为批量操作
     */
    public boolean isBatch() {
        return batchData != null && !batchData.isEmpty();
    }
}
