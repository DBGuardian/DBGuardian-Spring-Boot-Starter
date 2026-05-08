package com.erp.service.contract;

import com.erp.common.enums.ContractClosureStatus;

import java.math.BigDecimal;

/**
 * 合同状态权限检查服务接口
 *
 * @author ERP System
 */
public interface IContractStatusPermissionService {

    /**
     * 检查状态变更是否需要特殊权限
     *
     * @param contract 合同对象
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否需要特殊权限
     */
    boolean requiresSpecialPermission(Object contract, ContractClosureStatus currentStatus, ContractClosureStatus targetStatus);

    /**
     * 获取审批建议
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @param contractAmount 合同金额
     * @return 审批建议
     */
    String getApprovalRecommendation(ContractClosureStatus currentStatus, ContractClosureStatus targetStatus, BigDecimal contractAmount);
}
