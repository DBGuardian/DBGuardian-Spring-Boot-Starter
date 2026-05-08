package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量审核报价单请求
 */
@Data
@ApiModel("批量审核报价单请求")
public class QuotationBatchAuditRequest {

    @NotEmpty(message = "请选择要审核的报价单")
    @ApiModelProperty("报价单编号列表")
    private List<Integer> quotationIds;

    @NotNull(message = "请选择审核结果")
    @ApiModelProperty("审核结果：已通过/已驳回")
    private String auditResult;

    @ApiModelProperty("审核意见")
    private String auditOpinion;
}
