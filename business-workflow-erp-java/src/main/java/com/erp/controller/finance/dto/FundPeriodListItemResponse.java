package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账期列表项响应DTO
 */
@Data
@ApiModel("账期列表项")
public class FundPeriodListItemResponse {

    @ApiModelProperty(value = "账期编号", example = "1")
    private Long periodId;

    @ApiModelProperty(value = "账期编码", example = "202601")
    private String periodCode;

    @ApiModelProperty(value = "年份", example = "2026")
    private Integer year;

    @ApiModelProperty(value = "月份（1-12）", example = "1")
    private Integer month;

    @ApiModelProperty(value = "账期开始日期", example = "2026-01-01")
    private String startDate;

    @ApiModelProperty(value = "账期结束日期", example = "2026-01-31")
    private String endDate;

    @ApiModelProperty(value = "组织ID", example = "1")
    private Long organizationId;

    @ApiModelProperty(value = "组织名称", example = "广州分公司")
    private String organizationName;

    @ApiModelProperty(value = "是否已结账", example = "false")
    private Boolean isSettled;

    @ApiModelProperty(value = "结账时间", example = "2026-02-01 10:00:00")
    private LocalDateTime settlementTime;

    @ApiModelProperty(value = "结账人ID", example = "1")
    private Long settlementUserId;

    @ApiModelProperty(value = "创建人姓名", example = "张三")
    private String createUserName;

    @ApiModelProperty(value = "创建时间", example = "2026-01-01 10:00:00")
    private String createTime;

    @ApiModelProperty(value = "更新时间", example = "2026-01-01 10:00:00")
    private String updateTime;
}