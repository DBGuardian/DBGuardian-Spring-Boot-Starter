package com.erp.controller.finance.dto;

import lombok.Data;

/**
 * 状态权限检查响应DTO
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Data
public class StatusPermissionCheckResponse {

    /**
     * 当前状态
     */
    private String currentStatus;

    /**
     * 目标状态
     */
    private String targetStatus;

    /**
     * 是否需要特殊权限
     */
    private boolean requiresSpecialPermission;

    /**
     * 审批建议
     */
    private String approvalRecommendation;

    /**
     * 所需审批级别
     * 0-不需要审批，1-业务主管审批，2-财务总监审批，3-总经理审批
     */
    private int requiredApprovalLevel;
}
