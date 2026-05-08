package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新入库单请求
 */
@Data
@ApiModel("更新入库单请求")
public class UpdateWarehousingRequest {

    @ApiModelProperty(value = "入库单编号", required = true)
    @NotNull(message = "入库单编号不能为空")
    private Integer warehousingId;

    @ApiModelProperty(value = "入库单号")
    private String warehousingNo;

    @ApiModelProperty(value = "总磅单号", required = true)
    @NotBlank(message = "总磅单号不能为空")
    private String weighingSlipNo;

    @ApiModelProperty(value = "运输单号", required = true)
    @NotBlank(message = "运输单号不能为空")
    private String dispatchCode;

    @ApiModelProperty(value = "入库时间", required = true)
    @NotBlank(message = "入库时间不能为空")
    private String warehousingTime;

    @ApiModelProperty(value = "仓管员编码", required = true)
    @NotNull(message = "仓管员编码不能为空")
    private Integer warehouseKeeperId;

    @ApiModelProperty("入库备注")
    private String remark;

    @ApiModelProperty(value = "入库单危废明细列表", required = true)
    @NotEmpty(message = "至少需要一条危废明细")
    @Valid
    private List<UpdateWarehousingItemRequest> items;
}


