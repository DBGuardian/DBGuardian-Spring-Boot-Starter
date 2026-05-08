package com.erp.service.contract.impl;

import com.erp.common.enums.ContractClosureStatus;
import com.erp.service.contract.IContractStatusPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 合同状态权限检查服务实现
 *
 * @author ERP System
 */
@Slf4j
@Service
public class ContractStatusPermissionServiceImpl implements IContractStatusPermissionService {

    @Override
    public boolean requiresSpecialPermission(Object contract, ContractClosureStatus currentStatus, ContractClosureStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }

        if (targetStatus.getOrder() < currentStatus.getOrder()) {
            return true;
        }

        return false;
    }

    @Override
    public String getApprovalRecommendation(ContractClosureStatus currentStatus, ContractClosureStatus targetStatus,
                                          BigDecimal contractAmount) {
        return "需要审批";
    }
}
