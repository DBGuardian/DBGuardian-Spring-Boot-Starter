package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除收运通知单请求
 */
@Data
@ApiModel("批量删除收运通知单请求")
public class TransportApplyBatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的收运通知单")
    @ApiModelProperty("收运通知单号列表")
    private List<String> noticeCodes;
}
