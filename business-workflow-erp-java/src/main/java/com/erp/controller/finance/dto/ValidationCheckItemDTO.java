package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 校验项目DTO
 *
 * @author ERP System
 * @date 2025-02-05
 */
@Data
@ApiModel(description = "校验项目信息")
public class ValidationCheckItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 校验项目ID
     */
    @ApiModelProperty(value = "校验项目ID", example = "CHECK_001")
    private String checkId;

    /**
     * 校验类型
     */
    @ApiModelProperty(value = "校验类型", example = "TIME_SEQUENCE")
    private String checkType;

    /**
     * 校验项目名称
     */
    @ApiModelProperty(value = "校验项目名称", example = "合同时间顺序校验")
    private String checkName;

    /**
     * 校验状态
     */
    @ApiModelProperty(value = "校验状态", example = "PASS")
    private String status;

    /**
     * 校验消息
     */
    @ApiModelProperty(value = "校验消息", example = "时间顺序校验通过")
    private String message;

    /**
     * 校验详情数据
     */
    @ApiModelProperty(value = "校验详情数据")
    private Map<String, Object> details;

    /**
     * 建议操作
     */
    @ApiModelProperty(value = "建议操作", example = "无需操作")
    private String suggestedAction;
}
