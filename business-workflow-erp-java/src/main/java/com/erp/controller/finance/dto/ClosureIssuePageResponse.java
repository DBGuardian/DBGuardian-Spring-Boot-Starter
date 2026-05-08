package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 闭环问题分页响应DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "闭环问题分页响应")
public class ClosureIssuePageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 问题记录列表
     */
    @ApiModelProperty(value = "问题记录列表")
    private List<ClosureIssueDTO> records;

    /**
     * 总记录数
     */
    @ApiModelProperty(value = "总记录数", example = "100")
    private Long total;

    /**
     * 页大小
     */
    @ApiModelProperty(value = "页大小", example = "10")
    private Integer size;

    /**
     * 当前页
     */
    @ApiModelProperty(value = "当前页", example = "1")
    private Integer current;

    /**
     * 总页数
     */
    @ApiModelProperty(value = "总页数", example = "10")
    private Integer pages;
}
