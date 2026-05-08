package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 总磅单分页查询响应
 */
@Data
@ApiModel("总磅单分页查询响应")
public class WeighingSlipPageResponse {

    @ApiModelProperty("总磅单编号")
    private Integer weighingSlipId;

    @ApiModelProperty("总磅单号")
    private String weighingSlipNo;

    @ApiModelProperty("序号")
    private String sequence;

    @ApiModelProperty("日期")
    private LocalDate weighingDate;

    @ApiModelProperty("一次过秤时间")
    private LocalDateTime firstWeighTime;

    @ApiModelProperty("二次过秤时间")
    private LocalDateTime secondWeighTime;

    @ApiModelProperty("车号")
    private String plateNo;

    @ApiModelProperty("总重（kg）")
    private BigDecimal grossWeight;

    @ApiModelProperty("空重（kg）")
    private BigDecimal tareWeight;

    @ApiModelProperty("净重（kg）")
    private BigDecimal netWeight;

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

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
    
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("关联的运输单号列表")
    private List<String> dispatchCodes;
}

