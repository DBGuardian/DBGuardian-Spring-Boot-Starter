package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算单分页查询请求
 */
@Data
@ApiModel("结算单分页查询请求")
public class SettlementPageRequest {

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
     * 结算类型：RECEIVABLE=收款 / PAYABLE=付款
     */
    @ApiModelProperty(value = "结算类型：RECEIVABLE=收款 / PAYABLE=付款", required = true)
    @NotBlank(message = "结算类型不能为空")
    private String settlementType;

    /**
     * 结算单单号（模糊匹配）
     */
    @ApiModelProperty(value = "结算单单号（模糊匹配）")
    private String settlementCode;

    /**
     * 合同号（模糊匹配）
     */
    @ApiModelProperty(value = "合同号（模糊匹配）")
    private String contractCode;

    /**
     * 客户名称（模糊匹配）
     */
    @ApiModelProperty(value = "客户名称（模糊匹配）")
    private String customerName;

    /**
     * 状态（精确匹配）
     */
    @ApiModelProperty(value = "状态")
    private String status;

    /**
     * 制单人名称（模糊匹配）
     */
    @ApiModelProperty(value = "制单人名称（模糊匹配）")
    private String creatorName;

    /**
     * 结算周期起开始（格式：yyyy-MM-dd）
     */
    @ApiModelProperty(value = "结算周期起开始（格式：yyyy-MM-dd）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate settlementStartFrom;

    /**
     * 结算周期起结束（格式：yyyy-MM-dd）
     */
    @ApiModelProperty(value = "结算周期起结束（格式：yyyy-MM-dd）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate settlementStartTo;

    /**
     * 结算周期止开始（格式：yyyy-MM-dd）
     */
    @ApiModelProperty(value = "结算周期止开始（格式：yyyy-MM-dd）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate settlementEndFrom;

    /**
     * 结算周期止结束（格式：yyyy-MM-dd）
     */
    @ApiModelProperty(value = "结算周期止结束（格式：yyyy-MM-dd）")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate settlementEndTo;

    /**
     * 创建时间开始（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "创建时间开始（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeStart;

    /**
     * 创建时间结束（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "创建时间结束（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;

    /**
     * 排序字段：settlementId/settlementCode/contractCode/settlementAmount/receivedAmount/status/createTime
     */
    @ApiModelProperty(value = "排序字段")
    private String sortField;

    /**
     * 排序方向：asc/desc
     */
    @ApiModelProperty(value = "排序方向：asc/desc")
    private String sortOrder;

    /**
     * 预留扩展字段（如有需要可在此处新增）
     */
    @ApiModelProperty(value = "字段级权限页面编码，用于启用字段级权限和数据范围控制（例如：合同结算:危险废物结算-收款结算:页面）")
    private String fieldPermissionPageCode;

    /**
     * 是否只查询独立数据（未关联合同的结算单）
     */
    @ApiModelProperty(value = "是否只查询独立数据（未关联合同的结算单）")
    private Boolean independentOnly;
}
