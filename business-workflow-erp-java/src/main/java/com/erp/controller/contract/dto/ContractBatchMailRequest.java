package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量寄件/收件请求
 */
@Data
@ApiModel("批量寄件/收件请求")
public class ContractBatchMailRequest {

    @NotEmpty(message = "请选择要操作的合同")
    @ApiModelProperty("合同编号列表")
    private List<Integer> contractIds;

    @ApiModelProperty("寄件时间")
    private String sendDate;

    @ApiModelProperty("收件时间")
    private String receiveDate;
}
