package com.erp.controller.oa.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 业务详情响应DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "业务详情响应")
public class BusinessDetailResponse {

    @ApiModelProperty(value = "路由路径")
    private String routePath;

    @ApiModelProperty(value = "业务数据")
    private BusinessData businessData;

    @Data
    public static class BusinessData {
        @ApiModelProperty(value = "业务ID")
        private Integer id;

        @ApiModelProperty(value = "业务单号")
        private String code;
    }
}
