package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除危废条目请求
 */
@Data
@ApiModel("批量删除危废条目请求")
public class HazardousWasteBatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的条目")
    @ApiModelProperty("条目编号列表")
    private List<Integer> ids;
}
