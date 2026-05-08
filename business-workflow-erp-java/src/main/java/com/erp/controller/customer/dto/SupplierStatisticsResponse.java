package com.erp.controller.customer.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 供应商统计信息响应
 */
@Data
public class SupplierStatisticsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 供应商总数
     */
    private Integer totalCount;

    /**
     * 正常状态供应商数量
     */
    private Integer activeCount;

    /**
     * 停用状态供应商数量
     */
    private Integer inactiveCount;
}
