package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 合同统计信息
 */
@Data
@ApiModel("合同统计信息")
public class ContractStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "合同总数")
    private Integer total;

    @ApiModelProperty(value = "执行中数量")
    private Integer executing;

    @ApiModelProperty(value = "已完结数量")
    private Integer completed;

    @ApiModelProperty(value = "待审核数量")
    private Integer pendingAudit;

    @ApiModelProperty(value = "审核中数量")
    private Integer auditing;

    @ApiModelProperty(value = "本月新增合同数")
    private Integer monthlyNew;

    @ApiModelProperty(value = "本月合同金额")
    private BigDecimal monthlyAmount;
}



