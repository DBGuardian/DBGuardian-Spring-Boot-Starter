package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 更新检查项设置请求DTO
 */
@Data
@ApiModel("更新检查项设置请求")
public class SettlementCheckItemUpdateRequest {

    @ApiModelProperty(value = "检查项列表")
    private List<CheckItemUpdate> checkItems;

    /**
     * 检查项更新
     */
    @Data
    @ApiModel("检查项更新")
    public static class CheckItemUpdate {
        @ApiModelProperty(value = "检查项ID", example = "1")
        private Long checkItemId;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;
    }
}

