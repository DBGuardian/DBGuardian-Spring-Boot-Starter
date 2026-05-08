package com.erp.controller.oa.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * OA审核列表查询请求DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@ApiModel(description = "OA审核列表查询请求")
public class OaApprovalListRequest {

    @ApiModelProperty(value = "页码", example = "1")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize = 10;

    @ApiModelProperty(value = "关键字（搜索审核编号、关联单号、标题、提交人姓名、审核人姓名）")
    private String keyword;

    @ApiModelProperty(value = "业务类型：CONTRACT/QUOTATION/TRANSPORT/OUTBOUND/WAREHOUSING/SETTLEMENT/INVOICE/BUSINESS_FEE/EMPLOYEE_REGISTRATION")
    private String businessType;

    @ApiModelProperty(value = "审核状态：待审核/已通过/已驳回")
    private String approvalStatus;

    @ApiModelProperty(value = "视图范围：pending-待我审核、submitted-我发起的、processed-已处理、all-全部")
    private String viewScope;

    @ApiModelProperty(value = "提交人ID")
    private Integer submitterId;

    @ApiModelProperty(value = "审核人ID")
    private Integer approverId;

    @ApiModelProperty(value = "开始日期（格式：yyyy-MM-dd）")
    private String startDate;

    @ApiModelProperty(value = "结束日期（格式：yyyy-MM-dd）")
    private String endDate;

    @ApiModelProperty(value = "未审核天数（超过此天数未审核）")
    private Integer unapprovedDays;
}
