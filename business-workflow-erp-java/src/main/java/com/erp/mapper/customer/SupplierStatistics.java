package com.erp.mapper.customer;

import lombok.Data;

import java.io.Serializable;

/**
 * 供应商统计数据
 * 用于Mapper返回统计信息
 */
@Data
public class SupplierStatistics implements Serializable {

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
