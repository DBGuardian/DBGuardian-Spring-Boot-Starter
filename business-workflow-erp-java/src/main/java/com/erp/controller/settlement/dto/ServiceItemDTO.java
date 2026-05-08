package com.erp.controller.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价外服务明细DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class ServiceItemDTO {

    /**
     * 价外服务ID（用于增量更新）
     */
    private Integer outOfScopeServiceId;

    /**
     * 接收日期
     */
    private LocalDate receiveDate;

    /**
     * 项目名称
     */
    private String project;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 基本结算数量
     */
    private BigDecimal basicSettlementQuantity;

    /**
     * 基本计量单位
     */
    private String basicUnit;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 付款方：甲方/乙方
     */
    private String payer;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作类型：CREATE（新增）/UPDATE（修改）/DELETE（删除）
     * 用于增量更新时标识记录的操作类型
     */
    private String operation;
}
