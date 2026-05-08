package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 业务合同分页查询响应（列表行）
 * 字段对应 BUSINESS_CONTRACT 表冗余存储的字段，不含 SALESPERSON 独有字段
 * （业务员部门、职务、甲方联系人、甲方联系电话、客户编码不在 BUSINESS_CONTRACT 表中）
 */
@Data
@ApiModel("业务合同列表项")
public class BusinessContractPageResponse {

    @ApiModelProperty("合同主键ID")
    private Integer contractId;

    @ApiModelProperty("合同单号")
    private String contractNo;

    @ApiModelProperty("关联危废合同编号（一对一）")
    private Integer hazardousContractId;

    @ApiModelProperty("关联危废合同单号（HQ-YYYYMMDD-XXXXX）")
    private String hazardousContractNo;

    // ── 业务员摘要（冗余存储于 BUSINESS_CONTRACT）────────────────────────────
    @ApiModelProperty("业务员编号")
    private Integer salespersonId;

    @ApiModelProperty("业务员姓名")
    private String salespersonName;

    @ApiModelProperty("业务员电话")
    private String salespersonPhone;

    @ApiModelProperty("业务员身份证号")
    private String salespersonIdCard;

    @ApiModelProperty("甲方名称（合作公司）")
    private String companyName;

    @ApiModelProperty("甲方统一社会信用代码")
    private String partyACreditCode;

    // ── 收款卡信息 ────────────────────────────────────────────────────────────
    @ApiModelProperty("开户银行")
    private String bankName;

    @ApiModelProperty("银行卡号")
    private String cardNumber;

    @ApiModelProperty("账户名称")
    private String accountName;

    // ── 合同信息 ──────────────────────────────────────────────────────────────
    @ApiModelProperty("合同状态")
    private String status;

    @ApiModelProperty("审核意见")
    private String auditOpinion;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核人姓名")
    private String auditorName;

    @ApiModelProperty("审核时间")
    private String auditTime;

    @ApiModelProperty("制单人ID")
    private Integer creatorId;

    @ApiModelProperty("创建人姓名")
    private String creatorName;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("合同文件编号")
    private Integer contractFileId;

    @ApiModelProperty("合同文件访问路径")
    private String contractFilePath;

    @ApiModelProperty("合同签订时间")
    private String signTime;

    @ApiModelProperty("合同有效期结束日期")
    private String validTo;

    @ApiModelProperty("备注")
    private String remark;
}
