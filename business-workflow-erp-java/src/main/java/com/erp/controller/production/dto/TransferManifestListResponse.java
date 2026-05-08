package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 转移联单分页列表响应
 */
@Data
@ApiModel("转移联单分页列表响应")
public class TransferManifestListResponse {

    @ApiModelProperty("联单记录列表")
    private List<TransferManifestPageResponse> records;

    @ApiModelProperty("总记录数")
    private Long total;

    @ApiModelProperty("当前页码")
    private Integer current;

    @ApiModelProperty("每页数量")
    private Integer size;
}
