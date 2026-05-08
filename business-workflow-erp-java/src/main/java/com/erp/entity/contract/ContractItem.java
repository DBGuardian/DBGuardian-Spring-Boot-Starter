package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 合同条目实体
 *
 * 对应表：CONTRACT_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("CONTRACT_ITEM")
public class ContractItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 合同条目编号
     */
    @TableId(value = "合同条目编号", type = IdType.AUTO)
    private Integer contractItemId;

    /**
     * 合同编号
     */
    @TableField("合同编号")
    private Integer contractId;

    /**
     * 报价条目编号（来源的报价条目，用于追溯）
     */
    @TableField("报价条目编号")
    private Integer quotationItemId;

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
     * 低价备注（在新增合同时处理低价备注，PACKAGE模式必填，UNIT模式可空）
     */
    @TableField("低价备注")
    private String floorPriceRemark;

    /**
     * 小计摘要（记录各计量单位小计，JSON数组形式 [{unit,total}]，为合同锁定后的价格快照）
     */
    @TableField("小计摘要")
    private String subtotalSummary;
}




