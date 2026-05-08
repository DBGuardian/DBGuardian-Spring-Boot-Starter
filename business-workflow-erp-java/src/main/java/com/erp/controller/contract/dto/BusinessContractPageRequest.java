package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 业务合同分页查询请求
 */
@Data
@ApiModel("业务合同分页查询请求")
public class BusinessContractPageRequest {

    /** 当前页码，从 1 开始 */
    @ApiModelProperty(value = "当前页码", example = "1")
    @Min(value = 1, message = "页码必须大于等于1")
    private long current = 1;

    /** 每页条数 */
    @ApiModelProperty(value = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数必须大于等于1")
    private long size = 10;

    /** 合同单号（模糊） */
    @ApiModelProperty(value = "合同单号（模糊）")
    private String contractNo;

    /** 业务员姓名（模糊） */
    @ApiModelProperty(value = "业务员姓名（模糊）")
    private String salespersonName;

    /** 甲方公司名称（模糊，匹配 SALESPERSON.甲方名称） */
    @ApiModelProperty(value = "甲方公司名称（模糊）")
    private String companyName;

    /** 合同状态：待审核/执行中/已完结/已驳回 */
    @ApiModelProperty(value = "合同状态")
    private String status;

    /** 创建时间起（格式：yyyy-MM-dd） */
    @ApiModelProperty(value = "创建时间起（yyyy-MM-dd）")
    private String signTimeStart;

    /** 创建时间止（格式：yyyy-MM-dd） */
    @ApiModelProperty(value = "创建时间止（yyyy-MM-dd）")
    private String signTimeEnd;

    /** 排序字段：contractId / contractNo / salespersonName / companyName / status / createTime */
    @ApiModelProperty(value = "排序字段")
    private String sortField;

    /** 排序方向：asc / desc */
    @ApiModelProperty(value = "排序方向（asc/desc）")
    private String sortOrder;

    /**
     * 数据范围过滤：制单人ID
     * 后端根据员工viewScope配置强制填充，用于viewScope=SELF时仅查看自己创建的合同
     */
    @ApiModelProperty(value = "数据范围过滤（制单人ID，后端根据viewScope控制）", hidden = true)
    private Integer creatorFilter;

    /**
     * 数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断
     */
    @ApiModelProperty(value = "数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}
