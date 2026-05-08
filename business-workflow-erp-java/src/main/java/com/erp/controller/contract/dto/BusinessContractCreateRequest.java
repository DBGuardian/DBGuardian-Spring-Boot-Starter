package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增/更新业务合同请求
 * 业务员完整档案（部门、职务、甲方联系人、甲方联系电话、客户编码）存于 SALESPERSON 表，
 * 本请求仅包含可冗余存储到 BUSINESS_CONTRACT 表的字段，以及业务合同明细新结构。
 */
@Data
@ApiModel("业务合同创建/更新请求")
public class BusinessContractCreateRequest {

    @Data
    @ApiModel("业务合同组内危废项请求")
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
    @ApiModel("业务合同报价组请求")
    public static class WasteItemDTO {
        @ApiModelProperty("报价组编号")
        private Integer wasteItemId;

        @ApiModelProperty("行号")
        private Integer rowNo;

        @ApiModelProperty("来源报价条目编号")
        private Integer sourceQuotationItemId;

        @ApiModelProperty("结算类型：按量结算/总价包干")
        private String settlementType;

        @ApiModelProperty("单价底价")
        private BigDecimal unitFloorPrice;

        @ApiModelProperty("合同底价")
        private BigDecimal contractFloorPrice;

        @ApiModelProperty("组内危废项列表")
        private List<WasteInfoDTO> wasteInfos;
    }

    // ── 关联危废合同 ───────────────────────────────────────────────────────────
    @ApiModelProperty("关联危废合同编号（危废合同保存时自动传入，手动创建时为空）")
    private Integer hazardousContractId;

    // ── 业务员基本信息（冗余写入 BUSINESS_CONTRACT）───────────────────────────
    @ApiModelProperty(value = "业务员姓名", required = true)
    private String salespersonName;

    @ApiModelProperty("业务员编号（仅溯源）")
    private Integer salespersonId;

    @ApiModelProperty("业务员联系电话（选填）")
    private String salespersonPhone;

    @ApiModelProperty("业务员身份证号")
    private String salespersonIdCard;

    // ── 甲方（合作公司）信息（冗余写入 BUSINESS_CONTRACT）────────────────────
    @ApiModelProperty(value = "甲方名称（合作公司全称，可选）", required = true)
    private String partyAName;

    @ApiModelProperty("甲方统一社会信用代码（可选）")
    private String partyACreditCode;

    // ── 乙方（我方）信息（冗余写入 BUSINESS_CONTRACT）────────────────────────
    @ApiModelProperty("乙方名称")
    private String partyBName;

    @ApiModelProperty("乙方统一社会信用代码")
    private String partyBCreditCode;

    @ApiModelProperty("乙方联系人")
    private String partyBContactPerson;

    @ApiModelProperty("乙方联系电话（选填）")
    private String partyBContactPhone;

    // ── 收款卡信息（单条，冗余写入 BUSINESS_CONTRACT）────────────────────────
    @ApiModelProperty("开户银行")
    private String bankName;

    @ApiModelProperty("银行卡号")
    private String cardNumber;

    @ApiModelProperty("账户名称")
    private String accountName;

    // ── 合同期限 ──────────────────────────────────────────────────────────────
    @ApiModelProperty("合同签订时间（yyyy-MM-dd）")
    private String signTime;

    @ApiModelProperty("合同有效期开始日期（yyyy-MM-dd）")
    private String validFrom;

    @ApiModelProperty("合同有效期结束日期（yyyy-MM-dd）")
    private String validTo;

    // ── 其他 ──────────────────────────────────────────────────────────────────
    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("报价组列表（新结构）")
    private List<WasteItemDTO> wasteItems;
}
