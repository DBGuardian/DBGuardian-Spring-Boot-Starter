package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 运输合同查询响应DTO
 * 包含合同信息、关联车辆数、运输单号牌为空数量、结算单合同编号为空数量
 */
@Data
@ApiModel(description = "运输合同查询响应")
public class TransportContractQueryResponse {

    @ApiModelProperty("合同编号")
    private Integer contractId;

    @ApiModelProperty("合同单号")
    private String contractNo;

    @ApiModelProperty("承运方名称")
    private String carrierName;

    @ApiModelProperty("关联车辆数量")
    private Long vehicleCount;

    @ApiModelProperty("运输车辆号牌为空数量")
    private Long dispatchPlateEmptyCount;

    @ApiModelProperty("结算单合同编号为空数量")
    private Long settlementContractEmptyCount;

    @ApiModelProperty("关联车辆信息列表")
    private List<VehicleInfo> vehicleList;

    @Data
    @ApiModel(description = "车辆信息")
    public static class VehicleInfo {
        @ApiModelProperty("车辆编号")
        private Integer vehicleId;
        @ApiModelProperty("车牌号")
        private String plateNo;
    }
}
