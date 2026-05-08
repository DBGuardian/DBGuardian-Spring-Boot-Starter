package com.erp.controller.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户跟进明细响应
 */
@Data
@ApiModel("客户跟进明细响应")
public class CustomerFollowUpDetailResponse {

    @ApiModelProperty("跟进详细记录编号")
    private Integer detailId;

    @ApiModelProperty("跟进记录编号")
    private Integer followUpId;

    @ApiModelProperty("跟进时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followTime;

    @ApiModelProperty("跟进内容")
    private String followContent;

    @ApiModelProperty("跟进状态：未完成/已完成")
    private String followStatus;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人姓名")
    private String creatorName;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty("备注")
    private String remark;
}
