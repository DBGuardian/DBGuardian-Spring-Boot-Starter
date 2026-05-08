package com.erp.controller.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 客户跟进明细创建请求
 */
@Data
@ApiModel("客户跟进明细创建请求")
public class CustomerFollowUpDetailCreateRequest {

    @ApiModelProperty(value = "跟进记录编号", required = true)
    @NotNull(message = "跟进记录编号不能为空")
    private Integer followUpId;

    @ApiModelProperty(value = "跟进时间", required = true)
    @NotNull(message = "跟进时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followTime;

    @ApiModelProperty(value = "跟进内容", required = true)
    @NotBlank(message = "跟进内容不能为空")
    private String followContent;

    @ApiModelProperty("跟进状态：未完成/已完成（创建时默认未完成）")
    private String followStatus;

    @ApiModelProperty("备注")
    private String remark;
}
