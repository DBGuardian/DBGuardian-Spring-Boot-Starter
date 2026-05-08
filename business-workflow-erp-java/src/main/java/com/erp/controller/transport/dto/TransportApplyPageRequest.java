package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 收运通知单分页查询请求
 */
@Data
@ApiModel("收运通知单分页查询请求")
public class TransportApplyPageRequest {

    @ApiModelProperty(value = "当前页码", required = true, example = "1")
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码必须大于0")
    private Long current;

    @ApiModelProperty(value = "每页数量", required = true, example = "10")
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    private Long size;

    @ApiModelProperty("收运通知单号（模糊查询）")
    private String noticeCode;

    @ApiModelProperty("产生单位名称（模糊查询）")
    private String companyName;

    @ApiModelProperty("合同号（模糊查询）")
    private String contractCode;

    @ApiModelProperty("客户编码")
    private Integer customerId;

    @ApiModelProperty("现场联系人（模糊查询）")
    private String onsiteContact;

    @ApiModelProperty("现场联系电话（模糊查询）")
    private String onsitePhone;

    @ApiModelProperty("状态：未提交/审核中/审核失败/待调度/已派单/已完成/已取消")
    private String status;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建时间开始（格式：YYYY-MM-DD HH:mm:ss）")
    private String startTime;

    @ApiModelProperty("创建时间结束（格式：YYYY-MM-DD HH:mm:ss）")
    private String endTime;

    @ApiModelProperty("排序字段（默认：创建时间）")
    private String sortField;

    @ApiModelProperty("排序方向：asc/desc（默认：desc）")
    private String sortOrder;

    @ApiModelProperty("数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;

}

