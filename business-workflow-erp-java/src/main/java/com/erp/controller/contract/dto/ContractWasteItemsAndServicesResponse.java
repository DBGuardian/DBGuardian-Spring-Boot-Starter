package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 合同危废条目明细和价外服务响应
 */
@Data
@ApiModel("合同危废条目明细和价外服务响应")
public class ContractWasteItemsAndServicesResponse {

    @ApiModelProperty("危废条目明细列表（按合同条目分组）")
    private List<ContractNestedItemDTO> wasteItems;

    @ApiModelProperty("价外服务列表")
    private List<OutOfScopeServiceResponse> outOfScopeServices;


    @Data
    @ApiModel("价外服务响应")
    public static class OutOfScopeServiceResponse {
        @ApiModelProperty("价外服务编号")
        private Integer outOfScopeServiceId;
        @ApiModelProperty("项目")
        private String project;
        @ApiModelProperty("规格型号")
        private String spec;
        @ApiModelProperty("单位")
        private String unit;
        @ApiModelProperty("计划数量")
        private java.math.BigDecimal plannedQuantity;
        @ApiModelProperty("合同单价")
        private java.math.BigDecimal contractUnitPrice;
        @ApiModelProperty("状态")
        private String status;
    }
}
