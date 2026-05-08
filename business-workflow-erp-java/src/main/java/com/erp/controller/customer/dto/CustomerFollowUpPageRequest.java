package com.erp.controller.customer.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户跟进分页查询请求
 */
@Data
@ApiModel("客户跟进分页查询请求")
public class CustomerFollowUpPageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页
     */
    @ApiModelProperty("当前页码")
    private Long current = 1L;

    /**
     * 每页大小
     */
    @ApiModelProperty("每页数量")
    private Long size = 10L;

    /**
     * 客户编码（精确匹配）
     */
    @ApiModelProperty("客户编码（精确匹配）")
    private Integer customerId;

    /**
     * 开始时间（格式：yyyy-MM-dd）
     */
    @ApiModelProperty("开始日期")
    private String startDate;

    /**
     * 结束时间（格式：yyyy-MM-dd）
     */
    @ApiModelProperty("结束日期")
    private String endDate;

    /**
     * 联系人姓名（模糊匹配）
     */
    @ApiModelProperty("联系人姓名（模糊匹配）")
    private String contactName;

    /**
     * 联系人电话（模糊匹配）
     */
    @ApiModelProperty("联系人电话（模糊匹配）")
    private String contactPhone;

    /**
     * 排序字段
     */
    @ApiModelProperty("排序字段")
    private String orderBy;

    /**
     * 排序方向（asc: 升序, desc: 降序）
     */
    @ApiModelProperty("排序方向")
    private String orderDirection;

    /**
     * 数据范围过滤：前端在 viewScope=SELF 时传入当前员工ID，ALL 时不传。
     * 后端会在 Service 层对此参数进行安全校验并强制覆盖，防止越权。
     */
    @ApiModelProperty("数据范围过滤（创建人编码，前端viewScope=SELF时传入）")
    private Integer creatorFilter;
}
