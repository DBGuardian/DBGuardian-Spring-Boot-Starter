package com.erp.controller.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 状态权限检查请求DTO
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Data
public class StatusPermissionCheckRequest {

    /**
     * 当前状态
     */
    @NotBlank(message = "当前状态不能为空")
    private String currentStatus;

    /**
     * 目标状态
     */
    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    /**
     * 合同金额（用于权限分级）
     */
    private BigDecimal contractAmount;

    /**
     * 用户ID（可选，用于具体权限检查）
     */
    private Integer userId;
}
