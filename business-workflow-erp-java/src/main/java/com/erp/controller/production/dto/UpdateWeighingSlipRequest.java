package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新总磅单请求
 */
@Data
@ApiModel("更新总磅单请求")
public class UpdateWeighingSlipRequest {

    @ApiModelProperty(value = "总磅单号", required = true)
    @NotNull(message = "总磅单号不能为空")
    private String weighingSlipNo;

    @ApiModelProperty("序号（手动输入）")
    private String sequence;

    @ApiModelProperty(value = "日期（总磅日期，格式：yyyy-MM-dd）")
    private String date;

    @ApiModelProperty("一次过秤时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String firstWeighTime;

    @ApiModelProperty("二次过秤时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String secondWeighTime;

    @ApiModelProperty("车号（如：粤E70123）")
    private String plateNo;

    @ApiModelProperty("总重（kg）")
    private Double grossWeight;

    @ApiModelProperty("空重（kg）")
    private Double tareWeight;

    @ApiModelProperty("净重（kg），如果不提供则自动计算（总重-空重）")
    private Double netWeight;

    @ApiModelProperty("总磅单照片URL（通过文件上传接口获取）")
    private String photoUrl;

    @ApiModelProperty("关联运输单号列表")
    private List<String> dispatchCodes;

    @ApiModelProperty("状态：待细分/已细分")
    private String status;

    @ApiModelProperty("总磅备注")
    private String remark;
}

