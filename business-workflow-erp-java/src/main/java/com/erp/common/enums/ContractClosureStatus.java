package com.erp.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 合同闭环状态枚举
 * 定义合同业务流程的状态机，支持状态异常校验和权限控制
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Getter
@AllArgsConstructor
public enum ContractClosureStatus {

    /**
     * 草稿状态 - 合同初始创建状态
     */
    DRAFT("草稿", 1),

    /**
     * 已签订状态 - 合同审核通过并签订
     */
    SIGNED("已签订", 2),

    /**
     * 执行中状态 - 合同开始执行服务
     */
    EXECUTING("执行中", 3),

    /**
     * 执行完成状态 - 服务执行完毕
     */
    EXECUTED("执行完成", 4),

    /**
     * 部分收款状态 - 已收到部分款项
     */
    PARTIAL_PAYMENT("部分收款", 5),

    /**
     * 全额收款状态 - 已收到全部款项
     */
    FULL_PAYMENT("全额收款", 6),

    /**
     * 已开票状态 - 发票已开具完成
     */
    INVOICED("已开票", 7),

    /**
     * 归档状态 - 业务完成并归档
     */
    ARCHIVED("归档", 8),

    /**
     * 已完结状态 - 兼容现有状态
     */
    COMPLETED("已完结", 9),

    /**
     * 待审核状态 - 兼容现有状态
     */
    PENDING_REVIEW("待审核", 0),

    /**
     * 已通过状态 - 兼容现有状态
     */
    APPROVED("已通过", 2);

    /**
     * 状态显示名称
     */
    private final String displayName;

    /**
     * 状态顺序号，用于状态流转校验
     */
    private final int order;

    /**
     * 根据显示名称获取状态枚举
     * @param displayName 显示名称
     * @return 状态枚举，找不到返回null
     */
    public static ContractClosureStatus fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }

        for (ContractClosureStatus status : values()) {
            if (status.getDisplayName().equals(displayName.trim())) {
                return status;
            }
        }
        return null;
    }

    /**
     * 获取状态的风险等级
     * @return 风险等级：LOW, MEDIUM, HIGH, CRITICAL
     */
    public String getRiskLevel() {
        switch (this) {
            case DRAFT:
            case PENDING_REVIEW:
                return "LOW";
            case SIGNED:
            case APPROVED:
            case EXECUTING:
                return "MEDIUM";
            case EXECUTED:
            case PARTIAL_PAYMENT:
                return "HIGH";
            case FULL_PAYMENT:
            case INVOICED:
            case ARCHIVED:
            case COMPLETED:
                return "CRITICAL";
            default:
                return "MEDIUM";
        }
    }
}