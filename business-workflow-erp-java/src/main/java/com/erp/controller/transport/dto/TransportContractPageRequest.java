package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 运输合同分页查询请求
 */
@Data
@ApiModel(description = "运输合同分页查询请求")
public class TransportContractPageRequest {

    @ApiModelProperty("当前页")
    private Integer current;

    @ApiModelProperty("每页条数")
    private Integer size;

    @ApiModelProperty("合同单号（模糊）")
    private String contractNo;

    @ApiModelProperty("承运方名称（模糊）")
    private String carrierName;

    @ApiModelProperty("签约类型")
    private String signingType;

    @ApiModelProperty("结算方式")
    private String settlementMethod;

    @ApiModelProperty("合同状态")
    private String status;

    @ApiModelProperty("签订时间起")
    private String signTimeStart;

    @ApiModelProperty("签订时间止")
    private String signTimeEnd;

    @ApiModelProperty("排序字段")
    private String sortField;

    @ApiModelProperty("排序方向（asc/desc）")
    private String sortOrder;

    /**
     * 数据范围过滤：制单人ID
     * 后端根据员工viewScope配置强制填充，用于viewScope=SELF时仅查看自己创建的合同
     */
    @ApiModelProperty(value = "数据范围过滤（制单人ID，后端根据viewScope控制）", hidden = true)
    private Integer creatorFilter;

    @ApiModelProperty("数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}
