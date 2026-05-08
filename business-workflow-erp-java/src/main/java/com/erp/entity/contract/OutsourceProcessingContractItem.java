package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 委外处理合同报价组实体
 *
 * 对应表：OUTSOURCE_PROCESSING_CONTRACT_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("OUTSOURCE_PROCESSING_CONTRACT_ITEM")
public class OutsourceProcessingContractItem extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条目编号
     */
    @TableId(value = "条目编号", type = IdType.AUTO)
    private Integer itemId;

    /**
     * 合同编号
     */
    @TableField("合同编号")
    private Integer contractId;

    /**
     * 行号
     */
    @TableField("行号")
    private Integer rowNumber;

    /**
     * 计价方式（UNIT-按量结算，PACKAGE-总价包干）
     */
    @TableField("计价方式")
    private String pricingMode;

    /**
     * 付款方（甲方/乙方）
     */
    @TableField("付款方")
    private String payer;

    /**
     * 计价方案描述
     */
    @TableField("计价方案")
    private String pricingStatement;

/**
     * 小计摘要（JSON数组形式）
     */
    @TableField("小计摘要")
    private String subtotalSummary;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 是否删除
     */
    @TableField("是否删除")
    private Boolean isDeleted;
}
