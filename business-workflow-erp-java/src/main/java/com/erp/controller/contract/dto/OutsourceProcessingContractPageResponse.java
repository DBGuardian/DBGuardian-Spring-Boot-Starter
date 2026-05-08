package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 委外处理合同分页响应（不含明细，用于列表展示）
 */
@Data
@ApiModel(description = "委外处理合同分页响应")
public class OutsourceProcessingContractPageResponse {

    @ApiModelProperty(value = "合同ID", example = "1")
    private Integer contractId;

    @ApiModelProperty(value = "合同编号", example = "OPC-20260506-00001")
    private String contractNo;

    @ApiModelProperty(value = "甲方ID", example = "1")
    private Integer partyAId;

    @ApiModelProperty(value = "甲方名称", example = "XX危险废物处理有限公司")
    private String partyAName;

    @ApiModelProperty(value = "甲方统一社会信用代码", example = "91110000XXXXXXX")
    private String partyACreditCode;

    @ApiModelProperty(value = "甲方联系人", example = "张三")
    private String partyAContact;

    @ApiModelProperty(value = "甲方联系电话", example = "13800138000")
    private String partyAContactPhone;

    @ApiModelProperty(value = "乙方名称", example = "XX再生资源有限公司")
    private String partyBName;

    @ApiModelProperty(value = "乙方统一社会信用代码", example = "91110000XXXXXXX")
    private String partyBCreditCode;

    @ApiModelProperty(value = "乙方联系人", example = "李四")
    private String partyBContact;

    @ApiModelProperty(value = "乙方联系电话", example = "13900139000")
    private String partyBContactPhone;

    @ApiModelProperty(value = "业务员ID", example = "1001")
    private Integer ownerEmployeeId;

    @ApiModelProperty(value = "业务员名称", example = "王五")
    private String ownerEmployeeName;

    @ApiModelProperty(value = "业务费用结算", example = "true")
    private Boolean feeSettlementEnabled;

    @ApiModelProperty(value = "签订时间")
    private LocalDateTime signTime;

    @ApiModelProperty(value = "有效期起")
    private LocalDateTime validFrom;

    @ApiModelProperty(value = "有效期至")
    private LocalDateTime validTo;

    @ApiModelProperty(value = "合同状态", example = "执行中")
    private String contractStatus;

    @ApiModelProperty(value = "审核人ID", example = "1002")
    private Integer auditorId;

    @ApiModelProperty(value = "审核人名称", example = "赵六")
    private String auditorName;

    @ApiModelProperty(value = "审核时间")
    private LocalDateTime auditTime;

    @ApiModelProperty(value = "审核意见", example = "同意")
    private String auditOpinion;

    @ApiModelProperty(value = "合同文件ID", example = "1")
    private Integer contractFileId;

    @ApiModelProperty(value = "备注", example = "备注信息")
    private String remark;

    @ApiModelProperty(value = "创建人ID", example = "1001")
    private Integer creatorId;

    @ApiModelProperty(value = "创建人名称", example = "系统管理员")
    private String creatorName;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "版本号", example = "1")
    private Integer version;
}
