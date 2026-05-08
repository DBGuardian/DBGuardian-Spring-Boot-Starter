package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 系统配置保存请求
 */
@Data
@ApiModel("SysConfigSaveRequest")
public class SysConfigSaveRequest {

    @ApiModelProperty(value = "配置内容（JSON 或 文本）", required = true)
    private String value;

    @ApiModelProperty(value = "配置备注说明", required = false)
    private String remark;
}



