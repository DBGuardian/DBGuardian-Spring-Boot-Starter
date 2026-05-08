package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 合同关联车辆响应
 */
@Data
@ApiModel("合同关联车辆响应")
public class ContractVehicleResponse {

    @ApiModelProperty("关联编号")
    private Integer relationId;

    @ApiModelProperty("合同编号")
    private Integer contractId;

    @ApiModelProperty("合同单号")
    private String contractNo;

    @ApiModelProperty("承运方名称")
    private String carrierName;

    @ApiModelProperty("车辆编号")
    private Integer vehicleId;

    @ApiModelProperty("车牌号")
    private String plateNo;

    @ApiModelProperty("车辆类型")
    private String vehicleType;

    @ApiModelProperty("车辆状态")
    private String vehicleStatus;

    @ApiModelProperty("所属公司")
    private String companyName;

    @ApiModelProperty("核载吨位")
    private Double loadCapacity;

    @ApiModelProperty("关联时间")
    private String relationTime;

    @ApiModelProperty("关联人编码")
    private Integer relationUserId;

    @ApiModelProperty("备注")
    private String remark;
}
