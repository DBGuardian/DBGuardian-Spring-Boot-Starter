package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 批量创建入库单响应
 */
@Data
@ApiModel("批量创建入库单响应")
public class BatchCreateWarehousingResponse {

    @ApiModelProperty("创建的入库单列表（全部成功时返回）")
    private List<WarehousingInfo> warehousingList;

    /**
     * 入库单信息
     */
    @Data
    @ApiModel("入库单信息")
    public static class WarehousingInfo {
        @ApiModelProperty("入库单号")
        private String warehousingNo;

        @ApiModelProperty("运输单号")
        private String dispatchCode;
    }
}


