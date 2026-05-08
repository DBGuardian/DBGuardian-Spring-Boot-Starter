package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同分页通用响应
 *
 * <p>
 * 底层统一的合同分页 DTO，仅承载字段本身，不再绑定字段级权限编码。
 * 合同变更 / 合同履行页面的字段级权限，分别通过
 * {@link ContractChangePageResponse} 与 {@link ContractPerformancePageResponse}
 * 两个专用 DTO 承载，避免同一字段上同时声明多套权限编码导致冲突。
 * </p>
 */
@Data
@ApiModel("合同分页通用响应")
public class ContractPageResponse {

    @ApiModelProperty("合同编号")
    private Integer contractId;

    @ApiModelProperty("合同号（业务可见的合同编号：HQ-YYYYMMDD-XXXXX）")
    private String contractNo;

    @ApiModelProperty("客户编码")
    private Integer customerId;

    @ApiModelProperty("客户名称")
    private String enterpriseName;

    @ApiModelProperty("客户快照")
    private ContractCustomerSnapshot customerSnapshot;

    @ApiModelProperty("甲方名称")
    private String partyAName;

    @ApiModelProperty("甲方联系人")
    private String partyAContact;

    @ApiModelProperty("甲方联系电话")
    private String partyAContactPhone;

    @ApiModelProperty("甲方统一社会信用代码")
    private String partyACreditCode;

    @ApiModelProperty("乙方名称")
    private String partyBName;

    @ApiModelProperty("乙方联系人")
    private String partyBContact;

    @ApiModelProperty("乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty("乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty("合同金额")
    private BigDecimal contractAmount;

    @ApiModelProperty("是否启用业务费用结算")
    private Boolean feeSettlementEnabled;

    @ApiModelProperty("签订时间")
    private LocalDateTime signTime;

    @ApiModelProperty("合同状态")
    private String contractStatus;

    @ApiModelProperty("合同有效期开始")
    private LocalDateTime validFrom;

    @ApiModelProperty("合同有效期结束")
    private LocalDateTime validTo;

    @ApiModelProperty("编号生成方式（BEFORE_APPROVAL/AFTER_APPROVAL）")
    private String numberGenerationMode;

    @ApiModelProperty("审核时间")
    private LocalDateTime auditTime;

    @ApiModelProperty("审核意见")
    private String auditOpinion;

    @ApiModelProperty("寄件日期（合同寄出日期）")
    private LocalDateTime sendDate;

    @ApiModelProperty("收件日期（合同收件日期）")
    private LocalDateTime receiveDate;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核人姓名")
    private String auditorName;

    @ApiModelProperty("合同扫描件文件编号")
    private Integer contractFileId;

    @ApiModelProperty("合同PDF文件编号")
    private Integer contractPdfFileId;

    @ApiModelProperty("扫描件路径")
    private String scanFilePath;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人姓名")
    private String creatorName;

    @ApiModelProperty("业务员编码")
    private Integer ownerEmployeeId;

    @ApiModelProperty("业务员姓名")
    private String ownerEmployeeName;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("PDF文件编号")
    private Integer pdfFileId;

    @ApiModelProperty("是否已生成PDF")
    private Boolean pdfGenerated;

    @ApiModelProperty("PDF文件URL")
    private String pdfUrl;

    @ApiModelProperty("PDF文件名")
    private String pdfFileName;
}



