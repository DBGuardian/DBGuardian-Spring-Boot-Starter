package com.erp.controller.customer.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 供应商删除请求
 * 支持单个删除和批量删除
 */
@Data
public class SupplierDeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单个删除的供应商ID
     */
    private Integer supplierId;

    /**
     * 批量删除的供应商ID列表
     */
    private List<Integer> supplierIds;

    /**
     * 是否软删除（默认为true）
     */
    private Boolean softDelete = true;

    /**
     * 创建单个删除请求的静态方法
     */
    public static SupplierDeleteRequest single(Integer supplierId, Boolean softDelete) {
        SupplierDeleteRequest request = new SupplierDeleteRequest();
        request.setSupplierId(supplierId);
        request.setSoftDelete(softDelete != null ? softDelete : true);
        return request;
    }

    /**
     * 创建批量删除请求的静态方法
     */
    public static SupplierDeleteRequest batch(List<Integer> supplierIds, Boolean softDelete) {
        SupplierDeleteRequest request = new SupplierDeleteRequest();
        request.setSupplierIds(supplierIds);
        request.setSoftDelete(softDelete != null ? softDelete : true);
        return request;
    }

    /**
     * 获取要删除的供应商ID列表（统一单个和批量逻辑）
     */
    public List<Integer> getSupplierIdsToDelete() {
        if (supplierIds != null && !supplierIds.isEmpty()) {
            return supplierIds;
        } else if (supplierId != null) {
            return Arrays.asList(supplierId);
        }
        return new ArrayList<>();
    }

    /**
     * 判断是否为批量操作
     */
    public boolean isBatch() {
        return supplierIds != null && !supplierIds.isEmpty();
    }
}
