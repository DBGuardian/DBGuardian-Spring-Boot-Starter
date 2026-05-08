package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 报价单分页查询请求
 */
@Data
@ApiModel("报价单分页查询请求")
public class QuotationPageRequest {

    @ApiModelProperty(value = "当前页", required = true, example = "1")
    @NotNull(message = "当前页不能为空")
    @Min(value = 1, message = "当前页必须大于0")
    private Long current;

    @ApiModelProperty(value = "每页数量", required = true, example = "10")
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    private Long size;

    @ApiModelProperty(value = "关键词（客户名称/报价单号）")
    private String keyword;

    @ApiModelProperty(value = "报价单编号（模糊查询）")
    private String quotationNoSearch;

    @ApiModelProperty(value = "客户名称（模糊查询）")
    private String customerName;

    @ApiModelProperty(value = "客户编码")
    private Integer customerId;

    @ApiModelProperty(value = "报价状态：待审批/已通过/已驳回/已失效")
    private String quotationStatus;

    @ApiModelProperty(value = "计价方式：PACKAGE(总价包干)/UNIT(按量结算)/MIXED(组合计价)")
    private String pricingMode;

    @ApiModelProperty(value = "报价单号（内部编号）")
    private String quotationNo;

    @ApiModelProperty(value = "内部编号")
    private String internalCode;

    @ApiModelProperty(value = "创建人姓名")
    private String creatorName;

    @ApiModelProperty(value = "有效期开始（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @ApiModelProperty(value = "有效期结束（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;

    @ApiModelProperty(value = "是否已生成PDF")
    private Boolean pdfGenerated;

    @ApiModelProperty(value = "排序字段：quotationId/quotationCode/customerName/pricingMode/quotationStatus/totalQuantity/validFrom/createTime")
    private String sortField;

    @ApiModelProperty(value = "排序方向：asc/desc")
    private String sortOrder;

    @ApiModelProperty(value = "数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}


