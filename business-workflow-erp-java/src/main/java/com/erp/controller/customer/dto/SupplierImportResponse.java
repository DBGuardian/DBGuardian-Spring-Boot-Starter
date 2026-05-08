package com.erp.controller.customer.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 供应商导入结果
 */
@Data
public class SupplierImportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总行数
     */
    private int totalCount;

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failCount;

    /**
     * 错误明细
     */
    private List<SupplierImportError> errors = new ArrayList<>();
}
