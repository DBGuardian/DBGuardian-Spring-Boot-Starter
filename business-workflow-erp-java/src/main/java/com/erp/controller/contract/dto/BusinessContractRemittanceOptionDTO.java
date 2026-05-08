package com.erp.controller.contract.dto;

import lombok.Data;

/**
 * 汇款用业务合同选项 DTO
 * 专用于业务费结算关联业务合同时的搜索下拉，一次返回收款卡完整信息
 */
@Data
public class BusinessContractRemittanceOptionDTO {

    /** 合同主键ID */
    private Integer contractId;

    /** 合同单号 */
    private String contractNo;

    /** 关联危废合同ID */
    private Integer hazardousContractId;

    /** 业务员姓名 */
    private String salespersonName;

    /** 甲方名称（合作公司） */
    private String partyAName;

    /** 合同状态 */
    private String status;

    /** 开户银行 */
    private String bankName;

    /** 银行卡号 */
    private String cardNumber;

    /** 账户名称 */
    private String accountName;
}
