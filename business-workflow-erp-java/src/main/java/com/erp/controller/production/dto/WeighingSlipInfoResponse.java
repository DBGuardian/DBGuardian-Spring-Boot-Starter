package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 总磅单信息响应
 */
@Data
@ApiModel("总磅单信息响应")
public class WeighingSlipInfoResponse {

    @ApiModelProperty("总磅单编号")
    private Integer weighingSlipId;

    @ApiModelProperty("总磅单号")
    private String weighingSlipNo;

    @ApiModelProperty("序号")
    private String sequence;

    @ApiModelProperty("日期（格式：yyyy-MM-dd）")
    private String date;

    @ApiModelProperty("一次过秤时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String firstWeighTime;

    @ApiModelProperty("二次过秤时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String secondWeighTime;

    @ApiModelProperty("车号")
    private String plateNo;

    @ApiModelProperty("总重（kg）")
    private Double grossWeight;

    @ApiModelProperty("空重（kg）")
    private Double tareWeight;

    @ApiModelProperty("净重（kg）")
    private Double netWeight;

    @ApiModelProperty("总磅单照片URL")
    private String photoUrl;

    @ApiModelProperty("状态：待细分/已细分")
    private String status;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人名称")
    private String creatorName;

    @ApiModelProperty("总磅备注")
    private String remark;

    @ApiModelProperty("创建时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String createTime;
    
    @ApiModelProperty("更新时间（格式：yyyy-MM-dd HH:mm:ss）")
    private String updateTime;

    @ApiModelProperty("运输单号（第一个关联的运输单号，用于显示）")
    private String dispatchCode;

    @ApiModelProperty("关联的运输单号列表")
    private List<String> dispatchCodes;

    @ApiModelProperty("关联的收运通知单列表（用于生成入库单）")
    private List<PickupNoticeForWarehousing> pickupNoticeList;
}



