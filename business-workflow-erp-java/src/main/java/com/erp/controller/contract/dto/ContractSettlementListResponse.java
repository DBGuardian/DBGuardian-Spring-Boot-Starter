package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务费结算专用合同列表响应
 * 
 * 用于业务费结算页面左侧合同列表查询，返回执行中和已完结状态的合同
 */
@Data
@ApiModel("业务费结算合同列表响应")
public class ContractSettlementListResponse {

    @ApiModelProperty("合同记录列表")
    private List<ContractSettlementRecord> records;

    @ApiModelProperty("未关联危废合同的危废入库单数量")
    private Integer unlinkedWarehousingCount;

    @ApiModelProperty("未关联危废合同的危废结算单数量")
    private Integer unlinkedSettlementCount;

    /**
     * 业务费结算合同记录
     */
    @Data
    @ApiModel("业务费结算合同记录")
    public static class ContractSettlementRecord {

        @ApiModelProperty("合同编号")
        private Integer contractId;

        @ApiModelProperty("合同号（业务可见的合同编号：HQ-YYYYMMDD-XXXXX）")
        private String contractNo;

        @ApiModelProperty("甲方名称")
        private String partyAName;

        @ApiModelProperty("甲方联系人")
        private String partyAContact;

        @ApiModelProperty("甲方联系电话")
        private String partyAContactPhone;

        @ApiModelProperty("甲方统一社会信用代码")
        private String partyACreditCode;

        @ApiModelProperty("签订时间")
        private LocalDateTime signTime;

        @ApiModelProperty("合同状态")
        private String contractStatus;

        @ApiModelProperty("合同有效期开始")
        private LocalDateTime validFrom;

        @ApiModelProperty("合同有效期结束")
        private LocalDateTime validTo;

        @ApiModelProperty("总入库单数量")
        private Integer totalInboundCount;

        @ApiModelProperty("未关联结算单的入库单数量")
        private Integer unlinkedInboundCount;
    }
}
