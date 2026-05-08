package com.erp.controller.transport.dto;

import lombok.Data;

import java.util.List;

/**
 * 委外运输结算单分页响应
 */
@Data
public class OutsourceSettlementPageResponse {

    /**
     * 记录列表
     */
    private List<OutsourceSettlementResponse> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 每页数量
     */
    private Integer size;

    /**
     * 当前页码
     */
    private Integer current;

    /**
     * 总页数
     */
    private Integer pages;
}
