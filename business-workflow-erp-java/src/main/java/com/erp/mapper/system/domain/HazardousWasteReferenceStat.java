package com.erp.mapper.system.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 危险废物条目引用统计结果
 */
@Data
public class HazardousWasteReferenceStat implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 危废条目编号
     */
    private Integer itemId;

    /**
     * 客户引用数量
     */
    private Long customerCount;

    /**
     * 报价单引用数量
     */
    private Long quotationCount;

    /**
     * 入库单引用数量
     */
    private Long warehousingCount;

    /**
     * 库存引用数量
     */
    private Long stockCount;
}


