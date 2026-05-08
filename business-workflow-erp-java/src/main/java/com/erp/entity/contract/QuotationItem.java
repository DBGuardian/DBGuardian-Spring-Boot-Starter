package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报价条目实体
 *
 * 对应表：QUOTATION_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("QUOTATION_ITEM")
public class QuotationItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 报价条目编号
     */
    @TableId(value = "报价条目编号", type = IdType.AUTO)
    private Integer quotationItemId;

    /**
     * 报价单编号
     */
    @TableField("报价单编号")
    private Integer quotationId;

    /**
     * 报价模式：总价包干/按量结算
     */
    @TableField("报价模式")
    private String quotationMode;

    /**
     * 付款方：甲方/乙方/共同
     */
    @TableField("付款方")
    private String payer;

    /**
     * 计价方案（条目级计价方案描述，总价包干时使用）
     */
    @TableField("计价方案")
    private String pricingPlan;

    /**
     * 备注（条目级备注，总价包干时使用）
     */
    @TableField("备注")
    private String remark;

    /**
     * 小计摘要（记录各计量单位小计，JSON数组形式 [{unit,total}]）
     */
    @TableField("小计摘要")
    private String subtotalSummary;
}













