package com.erp.controller.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同基本信息DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class ContractBasicInfoDTO {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 合同号
     */
    private String contractCode;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 签订日期
     */
    private LocalDateTime signDate;

    /**
     * 合同金额
     */
    private BigDecimal contractAmount;

    /**
     * 合同状态
     */
    private String status;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 备注
     */
    private String remark;
}
