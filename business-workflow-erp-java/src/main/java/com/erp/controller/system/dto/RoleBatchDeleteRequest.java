package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除角色请求
 */
@Data
@ApiModel("批量删除角色请求")
public class RoleBatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的角色")
    @ApiModelProperty("角色编号列表")
    private List<Integer> ids;
}
