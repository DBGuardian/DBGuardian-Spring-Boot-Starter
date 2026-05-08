package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 委外处理合同详情响应
 */
@Data
@ApiModel("委外处理合同详情响应")
public class OutsourceProcessingContractDetailResponse {

    // ========== 合同基本信息 ==========

    @ApiModelProperty(value = "合同编号")
    private Integer contractId;

    @ApiModelProperty(value = "合同单号")
    private String contractNo;

    // ========== 甲方信息 ==========

    @ApiModelProperty(value = "甲方编码")
    private Integer partyAId;

    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    @ApiModelProperty(value = "甲方统一社会信用代码")
    private String partyACreditCode;

    @ApiModelProperty(value = "甲方联系人")
    private String partyAContact;

    @ApiModelProperty(value = "甲方联系电话")
    private String partyAContactPhone;

    // ========== 乙方信息 ==========

    @ApiModelProperty(value = "乙方名称")
    private String partyBName;

    @ApiModelProperty(value = "乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty(value = "乙方联系人")
    private String partyBContact;

    @ApiModelProperty(value = "乙方联系电话")
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
    private LocalDateTime signTime;

    @ApiModelProperty(value = "有效期开始")
    private LocalDateTime validFrom;

    @ApiModelProperty(value = "有效期结束")
    private LocalDateTime validTo;

    // ========== 审核信息 ==========

    @ApiModelProperty(value = "合同状态")
    private String contractStatus;

    @ApiModelProperty(value = "审核人编码")
    private Integer auditorId;

    @ApiModelProperty(value = "审核人姓名")
    private String auditorName;

    @ApiModelProperty(value = "审核时间")
    private LocalDateTime auditTime;

    @ApiModelProperty(value = "审核意见")
    private String auditOpinion;

    // ========== 合同文件 ==========

    @ApiModelProperty(value = "合同文件编号")
    private Integer contractFileId;

    @ApiModelProperty(value = "合同文件名称（从 FILE 表关联查询）")
    private String contractFileName;

    @ApiModelProperty(value = "合同文件URL（从 FILE 表关联查询）")
    private String contractFileUrl;

    // ========== 其他 ==========

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建人编码")
    private Integer creatorId;

    @ApiModelProperty(value = "创建人姓名")
    private String creatorName;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    // ========== 报价明细 ==========

    @ApiModelProperty(value = "报价明细列表")
    private List<OutsourceProcessingContractItemResponse> quotationItems;

    // ========== 价外服务 ==========

    @ApiModelProperty(value = "价外服务列表")
    private List<OutsourceProcessingContractOutOfScopeServiceResponse> outOfScopeServices;
}
