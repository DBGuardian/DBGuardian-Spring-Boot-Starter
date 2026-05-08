package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除权限请求
 */
@Data
@ApiModel("批量删除权限请求")
public class PermissionBatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的权限")
    @ApiModelProperty("权限编号列表")
    private List<Integer> ids;
}
