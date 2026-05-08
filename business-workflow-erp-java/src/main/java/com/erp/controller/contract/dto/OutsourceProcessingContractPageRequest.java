package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 委外处理合同分页查询请求
 */
@Data
@ApiModel("委外处理合同分页查询请求")
public class OutsourceProcessingContractPageRequest {

    @ApiModelProperty(value = "当前页码", example = "1")
    private Long current = 1L;

    @ApiModelProperty(value = "每页数量", example = "10")
    private Long size = 10L;

    @ApiModelProperty(value = "合同单号（模糊搜索）")
    private String contractNo;

    @ApiModelProperty(value = "甲方名称（模糊搜索）")
    private String partyAName;

    @ApiModelProperty(value = "合同状态")
    private String contractStatus;

    @ApiModelProperty(value = "签订时间开始")
    private LocalDateTime signTimeStart;

    @ApiModelProperty(value = "签订时间结束")
    private LocalDateTime signTimeEnd;

    @ApiModelProperty(value = "排序字段")
    private String sortField;

    @ApiModelProperty(value = "排序方向：asc/desc")
    private String sortOrder;

    /**
     * 数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断
     */
    @ApiModelProperty(value = "数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}
