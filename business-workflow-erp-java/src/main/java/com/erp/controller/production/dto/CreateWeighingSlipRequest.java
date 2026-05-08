package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建总磅单请求
 */
@Data
@ApiModel("创建总磅单请求")
public class CreateWeighingSlipRequest {

    @ApiModelProperty("序号（手动输入，可选）")
    private String sequence;

    @ApiModelProperty(value = "日期（总磅日期，格式：yyyy-MM-dd）", required = true)
    @NotNull(message = "日期不能为空")
    private String date;

    @ApiModelProperty("一次过秤时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String firstWeighTime;

    @ApiModelProperty("二次过秤时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String secondWeighTime;

    @ApiModelProperty(value = "车号（如：粤E70123）", required = true)
    @NotNull(message = "车号不能为空")
    private String plateNo;

    @ApiModelProperty("总重（kg）")
    private Double grossWeight;

    @ApiModelProperty("空重（kg）")
    private Double tareWeight;

    @ApiModelProperty("净重（kg），如果不提供则自动计算（总重-空重）")
    private Double netWeight;

    @ApiModelProperty("总磅单照片URL（通过文件上传接口获取）")
    private String photoUrl;

    @ApiModelProperty(value = "关联运输单号列表", required = true)
    @NotEmpty(message = "至少需要关联一个运输单号")
    private List<String> dispatchCodes;

    @ApiModelProperty("总磅备注")
    private String remark;
}



