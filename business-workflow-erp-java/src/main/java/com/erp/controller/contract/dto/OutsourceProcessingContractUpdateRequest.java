package com.erp.controller.contract.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 委外处理合同更新请求
 */
@Data
@ApiModel("委外处理合同更新请求")
public class OutsourceProcessingContractUpdateRequest {

    // ========== 甲方信息 ==========

    @ApiModelProperty(value = "甲方编码（供应商编码）")
    private Integer partyAId;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "甲方统一社会信用代码")
    private String partyACreditCode;

    @ApiModelProperty(value = "甲方联系人")
    private String partyAContact;

    @ApiModelProperty(value = "甲方联系电话（选填）")
    private String partyAContactPhone;

    // ========== 乙方信息 ==========

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty(value = "乙方联系人")
    private String partyBContact;

    @ApiModelProperty(value = "乙方联系电话（选填）")
    private String partyBContactPhone;

    // ========== 业务员 ==========

    @ApiModelProperty(value = "业务员编号")
    private Integer ownerEmployeeId;

    @ApiModelProperty(value = "业务员姓名")
    private String ownerEmployeeName;

    // ========== 业务费用 ==========

    @ApiModelProperty(value = "是否启用业务费用结算")
    private Boolean feeSettlementEnabled;

    // ========== 合同期限 ==========

    @ApiModelProperty(value = "签订时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signTime;

    @ApiModelProperty(value = "有效期开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @ApiModelProperty(value = "有效期结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;

    // ========== 备注 ==========

    @ApiModelProperty(value = "备注")
    private String remark;

    // ========== 报价明细 ==========

    @ApiModelProperty(value = "报价明细列表")
    @Valid
    private List<OutsourceProcessingContractItemRequest> quotationItems;

    // ========== 价外服务 ==========

    @ApiModelProperty(value = "价外服务列表")
    @Valid
    private List<OutsourceProcessingContractOutOfScopeServiceRequest> outOfScopeServices;
}
