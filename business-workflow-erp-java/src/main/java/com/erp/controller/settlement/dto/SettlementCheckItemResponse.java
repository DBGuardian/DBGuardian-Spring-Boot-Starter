package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 检查项设置响应DTO
 */
@Data
@ApiModel("检查项设置响应")
public class SettlementCheckItemResponse {

    @ApiModelProperty(value = "检查项列表")
    private List<CheckItem> checkItems;

    /**
     * 检查项
     */
    @Data
    @ApiModel("检查项")
    public static class CheckItem {
        @ApiModelProperty(value = "检查项ID", example = "1")
        private Long checkItemId;

        @ApiModelProperty(value = "检查项名称", example = "检查期初余额")
        private String checkItemName;

        @ApiModelProperty(value = "检查项编码", example = "CHECK_INITIAL_BALANCE")
        private String checkItemCode;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;

        @ApiModelProperty(value = "检查项说明", example = "检查期初余额是否与系统计算一致")
        private String description;

        @ApiModelProperty(value = "排序号", example = "1")
        private Integer sortOrder;
    }
}

