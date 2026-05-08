package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * 业务费结算专用合同查询请求
 * 
 * 功能描述：为业务费结算页面提供专用的合同查询接口，仅返回必要字段（合同号、甲方名称、合同状态）
 */
@Data
@ApiModel("业务费结算专用合同查询请求")
public class BusinessFeeSettlementContractPageRequest {

    /**
     * 当前页码
     */
    @ApiModelProperty(value = "当前页码，从1开始", example = "1")
    @Min(value = 1, message = "当前页码必须大于等于1")
    private long current = 1;

    /**
     * 每页数量
     */
    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于等于1")
    private long size = 10;

    /**
     * 合同号（模糊匹配）
     */
    @ApiModelProperty(value = "合同号（模糊匹配）")
    private String contractNo;

    /**
     * 甲方名称（模糊匹配）
     */
    @ApiModelProperty(value = "甲方名称（模糊匹配）")
    private String partyAName;

    /**
     * 合同状态：待审核/执行中/已完结/已归档/已驳回
     */
    @ApiModelProperty(value = "合同状态")
    private String contractStatus;

    /**
     * 签订时间开始（格式：yyyy-MM-dd）
     */
    @ApiModelProperty(value = "签订时间开始（格式：yyyy-MM-dd）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime signTimeStart;

    /**
     * 签订时间结束（格式：yyyy-MM-dd）
     */
    @ApiModelProperty(value = "签订时间结束（格式：yyyy-MM-dd）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime signTimeEnd;

    /**
     * 排序字段：contractNo/partyAName/contractStatus/signTime
     */
    @ApiModelProperty(value = "排序字段")
    private String sortField;

    /**
     * 排序方向：asc/desc
     */
    @ApiModelProperty(value = "排序方向：asc/desc")
    private String sortOrder;
}
