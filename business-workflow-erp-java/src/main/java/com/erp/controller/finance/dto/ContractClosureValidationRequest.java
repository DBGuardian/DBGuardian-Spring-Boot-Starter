package com.erp.controller.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 合同闭环校验请求DTO
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Data
public class ContractClosureValidationRequest {

    /**
     * 校验类型列表
     * 可选值：TIME_SEQUENCE, AMOUNT_CONSISTENCY, ASSOCIATION_INTEGRITY, STATUS_CONSISTENCY
     */
    private List<String> checkTypes;
}
