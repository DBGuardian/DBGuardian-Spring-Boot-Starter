package com.erp.controller.finance.dto;

import lombok.Data;

import java.util.List;

/**
 * 状态校验请求DTO
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Data
public class StatusValidationRequest {

    /**
     * 合同ID列表，不传则校验所有合同
     */
    private List<Long> contractIds;
}
