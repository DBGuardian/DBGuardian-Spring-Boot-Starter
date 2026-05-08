package com.erp.entity.finance;

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
 * 发票明细实体类
 * 
 * 对应表：INVOICE_ITEM
 * 
 * 业务关系说明：
 * 1. 一个发票可以包含多个明细项
 * 2. 存储发票上的商品明细信息，包括商品名称、规格、数量、单价、金额、税率等
 * 3. 当发票设置为"录入发票明细"为TRUE时，必须录入明细信息
 * 
 * @author ERP System
 * @date 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("INVOICE_ITEM")
public class InvoiceItem extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细编号
     */
    @TableId(value = "明细编号", type = IdType.AUTO)
    private Integer itemId;

    /**
     * 发票编号
     * 关联的发票编号，外键关联INVOICE表
     */
    @TableField("发票编号")
    private Integer invoiceId;

    /**
     * 商品名称
     */
    @TableField("商品名称")
    private String productName;

    /**
     * 规格型号
     */
    @TableField("规格型号")
    private String specification;

    /**
     * 单位
     * 计量单位
     */
    @TableField("单位")
    private String unit;

    /**
     * 数量
     */
    @TableField("数量")
    private BigDecimal quantity;

    /**
     * 单价
     * 单价（不含税）
     */
    @TableField("单价")
    private BigDecimal unitPrice;

    /**
     * 金额
     * 金额（不含税）
     */
    @TableField("金额")
    private BigDecimal amount;

    /**
     * 税率
     * 税率，如0.13表示13%
     */
    @TableField("税率")
    private BigDecimal taxRate;

    /**
     * 商品税额
     */
    @TableField("商品税额")
    private BigDecimal productTaxAmount;

    /**
     * 税收分类编码
     */
    @TableField("税收分类编码")
    private String taxClassificationCode;

    /**
     * 含税金额
     */
    @TableField("含税金额")
    private BigDecimal taxIncludedAmount;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;
}

