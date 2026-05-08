package com.erp.controller.transport.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算单关联派车单响应
 */
@Data
public class SettlementDispatchOrderResponse {

    /**
     * 派车单编号
     */
    private Integer dispatchId;

    /**
     * 派车单号
     */
    private String dispatchCode;

    /**
     * 派车日期
     */
    private LocalDate dispatchDate;

    /**
     * 车牌号
     */
    private String licensePlate;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 运输起点
     */
    private String transportStart;

    /**
     * 运输终点
     */
    private String transportEnd;

    /**
     * 运输数量（吨）
     */
    private BigDecimal transportQuantity;

    /**
     * 运输距离（公里）
     */
    private BigDecimal transportDistance;

    /**
     * 完成时间
     */
    private LocalDateTime completionTime;

    /**
     * 关联合同号
     */
    private String contractCode;
}
