package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 合同危废条目明细实体
 *
 * 对应表：CONTRACT_WASTE_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("CONTRACT_WASTE_ITEM")
public class ContractWasteItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 合同危废明细编号
     */
    @TableId(value = "合同危废明细编号", type = IdType.AUTO)
    private Integer contractWasteItemId;

    /**
     * 合同条目编号
     */
    @TableField("合同条目编号")
    private Integer contractItemId;

    /**
     * 报价危废明细编号（来源报价危废明细，用于追溯）
     */
    @TableField("报价危废明细编号")
    private Integer quotationWasteItemId;

    /**
     * 危废条目编号（引用国家危废名录/固废目录条目）
     */
    @TableField("危废条目编号")
    private Integer hazardousWasteItemId;

    /**
     * 废物类别（HW/SW 类别名称，如：HW01 医疗废物）
     * 注意：此字段不存在于数据库表中，通过危废条目编号关联查询获取
     */
    @TableField(exist = false)
    private String wasteCategory;

    /**
     * 行业来源（卫生、炼铁等）
     * 注意：此字段不存在于数据库表中，通过危废条目编号关联查询获取
     */
    @TableField(exist = false)
    private String industrySource;

    /**
     * 废物代码（官方公布的废物代码，如：841-001-01）
     * 注意：此字段不存在于数据库表中，通过危废条目编号关联查询获取
     */
    @TableField(exist = false)
    private String wasteCode;

    /**
     * 危险特性（In/T/C/I/R 等）
     * 注意：此字段不存在于数据库表中，通过危废条目编号关联查询获取
     */
    @TableField(exist = false)
    private String hazardFeature;

    /**
     * 危险废物（危废或固废名称）
     * 对应数据库字段：废物名称
     */
    @TableField("废物名称")
    private String hazardousWaste;

    /**
     * 形态（固态/液态/气态/半固态等）
     * 对应数据库字段：废物形态
     */
    @TableField("废物形态")
    private String form;

    /**
     * 计量单位（吨/桶/个等，需与合同口径一致）
     */
    @TableField("计量单位")
    private String unit;

    /**
     * 计划数量（计划转移数量，合同约定数量）
     * 对应数据库字段：计划转移数量
     * 业务约定：值为 -1 表示不限量
     */
    @TableField("计划转移数量")
    private BigDecimal plannedQuantity;

    // ====== 基础/辅助计量单位与换算关系（用于统一按吨结算与表达） ======

    /**
     * 是否启用辅助核算，默认不启用
     * 对应数据库字段：是否启用辅助核算
     */
    @TableField("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    /**
     * 辅助计量单位（业务友好展示单位，如桶/袋/车等）
     * 对应数据库字段：辅助计量单位
     */
    @TableField("辅助计量单位")
    private String auxUnit;

    /**
     * 辅助单位每基础单位数量（1基础计量单位≈多少辅助单位，例如1吨≈10桶）
     * 对应数据库字段：辅助单位每基础单位数量
     */
    @TableField("辅助单位每基础单位数量")
    private BigDecimal auxPerBase;

    /**
     * 按辅助计量单位表达的数量，通常对应页面上的桶/袋等数量
     * 对应数据库字段：辅助数量
     */
    @TableField("辅助数量")
    private BigDecimal auxQuantity;

    /**
     * 辅助计价单价（元/辅助计量单位，如元/桶）
     * 对应数据库字段：辅助单价
     */
    @TableField("辅助单价")
    private BigDecimal auxUnitPrice;

    /**
     * 基础计量单位（如吨），用于在合同详情中统一表达基础数量
     * 注意：此字段不存在于数据库表中，根据计量单位及换算关系推导
     */
    @TableField(exist = false)
    private String baseUnit;

    /**
     * 基础计量数量（按基础计量单位换算后的数量，如吨）
     * 注意：此字段不存在于数据库表中，根据计划转移数量及换算关系推导
     */
    @TableField(exist = false)
    private BigDecimal baseQuantity;

    /**
     * 基础计价单价（元/基础计量单位，如元/吨）
     * 注意：此字段不存在于数据库表中，根据单价及换算关系推导
     */
    @TableField(exist = false)
    private BigDecimal baseUnitPrice;

    /**
     * 单价（合同约定单价，元/计量单位）
     * 从报价复制后可在合同中微调
     */
    @TableField("单价")
    private BigDecimal unitPrice;

    /**
     * 金额（合同约定金额，作为结算参考快照）
     */
    @TableField("金额")
    private BigDecimal amount;

    /**
     * 付款方（甲方/乙方，父级合同条目为UNIT时必填，PACKAGE时可空）
     */
    @TableField("付款方")
    private String payer;

    /**
     * 计价方案（危废条目级计价方案描述，按量结算时使用）
     */
    @TableField("计价方案")
    private String pricingPlan;

    /**
     * 低价备注（低价说明等备注信息）
     */
    @TableField("低价备注")
    private String floorPriceRemark;
}










































