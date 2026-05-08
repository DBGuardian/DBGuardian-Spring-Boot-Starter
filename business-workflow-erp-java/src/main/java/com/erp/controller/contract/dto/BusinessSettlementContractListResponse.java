package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 业务合同列表专用查询响应
 *
 * 功能描述：业务合同列表专用查询接口
 * 数据来源：BUSINESS_CONTRACT（合同编号、合同单号、业务员姓名、业务员电话、业务员身份证号、甲方名称、甲方统一社会信用代码、合同状态）
 *          LEFT JOIN CONTRACT（合同编号、合同号）
 * 不分页，一次返回全部记录
 */
@Data
@ApiModel("业务合同列表专用查询响应")
public class BusinessSettlementContractListResponse {

    @ApiModelProperty("合同记录列表")
    private List<BusinessSettlementContractRecord> records;

    @ApiModelProperty("游离业务费结算单数量（未关联业务合同的业务结算单）")
    private Integer orphanBusinessFeeCount;

    /**
     * 单条合同记录
     */
    @Data
    @ApiModel("业务合同列表记录")
    public static class BusinessSettlementContractRecord {

        // ── BUSINESS_CONTRACT 字段 ────────────────────────────────────────

        @ApiModelProperty("业务合同主键ID（BUSINESS_CONTRACT.合同编号）")
        private Integer businessContractId;

        @ApiModelProperty("业务合同单号（BC-YYYYMMDD-XXXXX）")
        private String businessContractNo;

        @ApiModelProperty("业务员姓名（BUSINESS_CONTRACT.业务员姓名）")
        private String salespersonName;

        @ApiModelProperty("业务员电话（BUSINESS_CONTRACT.业务员电话）")
        private String salespersonPhone;

        @ApiModelProperty("业务员身份证号（BUSINESS_CONTRACT.业务员身份证号）")
        private String salespersonIdCard;

        @ApiModelProperty("甲方名称（BUSINESS_CONTRACT.甲方名称）")
        private String partyAName;

        @ApiModelProperty("甲方统一社会信用代码（BUSINESS_CONTRACT.甲方统一社会信用代码）")
        private String partyACreditCode;

        @ApiModelProperty("合同状态（业务合同状态）")
        private String status;

        // ── CONTRACT 字段（通过 危废合同编号 LEFT JOIN 获得）────────────────────

        @ApiModelProperty("关联危废合同编号（CONTRACT.合同编号）")
        private Integer contractId;

        @ApiModelProperty("关联危废合同号（CONTRACT.合同号）")
        private String contractNo;

        @ApiModelProperty("当前业务合同下未关联业务结算单的危废结算单数量")
        private Integer unlinkedSettlementCount;
    }
}
