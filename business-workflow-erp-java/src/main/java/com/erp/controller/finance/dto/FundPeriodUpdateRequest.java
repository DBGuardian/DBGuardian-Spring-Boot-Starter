package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 更新账期请求DTO
 */
@Data
@ApiModel("更新账期请求")
public class FundPeriodUpdateRequest {

    @ApiModelProperty(value = "开始日期（格式：YYYY-MM-DD）", example = "2026-01-01", required = true)
    @NotNull(message = "开始日期不能为空")
    @JsonProperty("start_date")
    private LocalDate startDate;

    @ApiModelProperty(value = "结束日期（格式：YYYY-MM-DD）", example = "2026-01-31", required = true)
    @NotNull(message = "结束日期不能为空")
    @JsonProperty("end_date")
    private LocalDate endDate;
}


