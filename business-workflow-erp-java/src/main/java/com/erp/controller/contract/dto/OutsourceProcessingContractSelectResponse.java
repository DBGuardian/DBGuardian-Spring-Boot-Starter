package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 委外处理合同下拉选项响应
 * 用于下拉框等轻量级选择场景
 */
@Data
@ApiModel(description = "委外处理合同下拉选项响应")
public class OutsourceProcessingContractSelectResponse {

    @ApiModelProperty(value = "合同ID", example = "1")
    private Integer contractId;

    @ApiModelProperty(value = "合同编号", example = "OPC-20260506-00001")
    private String contractNo;

    @ApiModelProperty(value = "甲方（供应商）编码", example = "1")
    private Integer partyAId;

    @ApiModelProperty(value = "甲方（供应商）名称", example = "XX危险废物处理有限公司")
    private String partyAName;

    @ApiModelProperty(value = "甲方统一社会信用代码", example = "91110000XXXXXXX")
    private String partyACreditCode;

    @ApiModelProperty(value = "甲方联系人", example = "张三")
    private String partyAContact;

    @ApiModelProperty(value = "甲方联系电话", example = "13800138000")
    private String partyAContactPhone;

    @ApiModelProperty(value = "乙方名称", example = "XX再生资源有限公司")
    private String partyBName;

    @ApiModelProperty(value = "业务员名称", example = "李四")
    private String ownerEmployeeName;

    @ApiModelProperty(value = "签订时间")
    private LocalDateTime signTime;

    @ApiModelProperty(value = "有效期起")
    private LocalDateTime validFrom;

    @ApiModelProperty(value = "有效期至")
    private LocalDateTime validTo;

    @ApiModelProperty(value = "合同状态", example = "执行中")
    private String contractStatus;
}
