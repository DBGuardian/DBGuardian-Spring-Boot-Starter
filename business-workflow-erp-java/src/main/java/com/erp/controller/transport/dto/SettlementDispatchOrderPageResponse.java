package com.erp.controller.transport.dto;

import lombok.Data;

import java.util.List;

/**
 * 可结算派车单分页响应
 */
@Data
public class SettlementDispatchOrderPageResponse {

    /**
     * 派车单列表
     */
    private List<SettlementDispatchOrderResponse> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 每页数量
     */
    private Integer size;

    /**
     * 当前页
     */
    private Integer current;

    /**
     * 总页数
     */
    private Integer pages;
}
