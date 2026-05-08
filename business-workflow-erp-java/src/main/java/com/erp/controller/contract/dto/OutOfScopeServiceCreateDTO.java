package com.erp.controller.contract.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 报价单/合同 价外服务 - 创建 DTO
 */
@Data
public class OutOfScopeServiceCreateDTO {
    private Integer outOfScopeServiceId;
    /**
     * 前端可能会以 `project` 字段发送，也有历史兼容使用 `serviceType`。
     * 两者均支持，保存时以 `project` 优先。
     */
    private String project;
    private String serviceType;
    private String spec;
    private String unit;
    private BigDecimal plannedQuantity;
    private BigDecimal contractUnitPrice;
    private Integer createdBy;
}


