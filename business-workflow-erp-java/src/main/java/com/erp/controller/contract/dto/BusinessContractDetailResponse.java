package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务合同详情响应
 * 对应 BUSINESS_CONTRACT 表冗余存储的字段，
 * 并返回业务合同报价组与组内危废项新结构。
 */
@Data
@ApiModel("业务合同详情响应")
public class BusinessContractDetailResponse {

    @Data
    @ApiModel("业务合同组内危废项响应")
    public static class WasteInfoDTO {
        @ApiModelProperty("危废项编号")
        private Integer wasteInfoId;

        @ApiModelProperty("所属报价组编号")
        private Integer wasteItemId;

        @ApiModelProperty("组内行号")
        private Integer innerRowNo;

        @ApiModelProperty("来源危废项编号")
        private Integer sourceWasteItemId;

        @ApiModelProperty("危废类型")
        private String wasteType;

        @ApiModelProperty("废物代码")
        private String wasteCode;

        @ApiModelProperty("危废名称")
        private String wasteName;
    }

    @Data
    @ApiModel("业务合同报价组响应")
    public static class WasteItemDTO {
        @ApiModelProperty("报价组编号")
        private Integer wasteItemId;

        @ApiModelProperty("所属合同编号")
        private Integer contractId;

        @ApiModelProperty("行号")
        private Integer rowNo;

        @ApiModelProperty("来源报价条目编号")
        private Integer sourceQuotationItemId;

        @ApiModelProperty("结算类型")
        private String settlementType;

        @ApiModelProperty("单价底价")
        private BigDecimal unitFloorPrice;

        @ApiModelProperty("合同底价")
        private BigDecimal contractFloorPrice;

        @ApiModelProperty("组内危废项列表")
        private List<WasteInfoDTO> wasteInfos;
    }

    @ApiModelProperty("合同主键ID")
    private Integer contractId;

    @ApiModelProperty("合同单号")
    private String contractNo;

    @ApiModelProperty("关联危废合同编号（一对一）")
    private Integer hazardousContractId;

    @ApiModelProperty("关联危废合同号（如 HQ-20250101-00001，由 JOIN CONTRACT 带出）")
    private String hazardousContractNo;

    // ── 业务员基本信息（冗余存储于 BUSINESS_CONTRACT）────────────────────────
    @ApiModelProperty("业务员编号（SALESPERSON.业务员编号，仅溯源）")
    private Integer salespersonId;

    @ApiModelProperty("业务员姓名")
    private String salespersonName;

    @ApiModelProperty("业务员联系电话")
    private String salespersonPhone;

    @ApiModelProperty("业务员身份证号")
    private String salespersonIdCard;

    // ── 甲方（合作公司）信息（冗余存储于 BUSINESS_CONTRACT）──────────────────
    @ApiModelProperty("甲方名称（合作公司全称）")
    private String partyAName;

    @ApiModelProperty("甲方统一社会信用代码")
    private String partyACreditCode;

    // ── 乙方（我方）信息（冗余存储于 BUSINESS_CONTRACT）──────────────────────
    @ApiModelProperty("乙方名称")
    private String partyBName;

    @ApiModelProperty("乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty("乙方联系人")
    private String partyBContactPerson;

    @ApiModelProperty("乙方联系电话")
    private String partyBContactPhone;

    // ── 收款卡信息 ────────────────────────────────────────────────────────────
    @ApiModelProperty("开户银行")
    private String bankName;

    @ApiModelProperty("银行卡号")
    private String cardNumber;

    @ApiModelProperty("账户名称")
    private String accountName;

    // ── 审核与状态 ────────────────────────────────────────────────────────────
    @ApiModelProperty("合同状态")
    private String status;

    @ApiModelProperty("审核意见")
    private String auditOpinion;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核人姓名")
    private String auditorName;

    @ApiModelProperty("审核时间")
    private LocalDateTime auditTime;

    // ── 合同文件 ──────────────────────────────────────────────────────────────
    @ApiModelProperty("合同文件编号（关联 FILE 表）")
    private Integer contractFileId;

    @ApiModelProperty("合同文件访问路径")
    private String contractFilePath;

    @ApiModelProperty("合同文件访问 URL（由 FILE 表 fileUrl 字段获取）")
    private String contractFileUrl;

    @ApiModelProperty("合同文件名称")
    private String contractFileName;

    // ── 合同期限 ──────────────────────────────────────────────────────────────
    @ApiModelProperty("合同签订时间")
    private String signTime;

    @ApiModelProperty("合同有效期开始日期")
    private String validFrom;

    @ApiModelProperty("合同有效期结束日期")
    private String validTo;

    // ── 其他 ──────────────────────────────────────────────────────────────────
    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建人编码")
    private Integer creatorId;

    @ApiModelProperty("创建人姓名")
    private String creatorName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("报价组列表（新结构）")
    private List<WasteItemDTO> wasteItems;
}
