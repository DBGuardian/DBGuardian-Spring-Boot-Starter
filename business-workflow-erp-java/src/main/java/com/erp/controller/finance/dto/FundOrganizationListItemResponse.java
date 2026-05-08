package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资金组织列表项响应
 */
@Data
@ApiModel("资金组织列表项")
public class FundOrganizationListItemResponse {

    @ApiModelProperty("组织ID")
    private Long organizationId;

    @ApiModelProperty("组织编码")
    private String organizationCode;

    @ApiModelProperty("组织名称")
    private String organizationName;

    @ApiModelProperty("组织描述")
    private String description;

    @ApiModelProperty("是否启用")
    private Boolean enabled;

    @ApiModelProperty("账户数量")
    private Integer accountCount;

    @ApiModelProperty("总余额")
    private String totalBalance;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("创建人姓名")
    private String createUserName;

    @ApiModelProperty("更新人姓名")
    private String updateUserName;

    @ApiModelProperty("创建人编码（前端用于 operateScope=SELF 时判断是否为自己创建）")
    private Long creatorId;
}
