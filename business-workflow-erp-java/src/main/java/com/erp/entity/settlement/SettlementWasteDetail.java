package com.erp.entity.settlement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算危废明细实体
 *
 * 对应表：SETTLEMENT_WASTE_DETAIL
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SETTLEMENT_WASTE_DETAIL")
public class SettlementWasteDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 明细编号（主键，自增）
     */
    @TableId(value = "明细编号", type = IdType.AUTO)
    private Long detailId;

    /**
     * 结算单编号（FK）
     */
    @TableField("结算单编号")
    private Long settlementId;

    /**
     * 序号（行内顺序，结算单内唯一）
     */
    @TableField("序号")
    private Integer sequence;

    /**
     * 接收日期
     */
    @TableField("接收日期")
    private LocalDate receiveDate;

    /**
     * 关联来源类型（WAREHOUSING/TRANSPORT/CONTRACT）
     */
    @TableField("关联来源类型")
    private String sourceType;

    /**
     * 关联来源单号（入库单号/运输单号）
     */
    @TableField("关联来源单号")
    private String sourceCode;

    /**
     * 广东省联单号
     */
    @TableField("广东省联单号")
    private String gdManifestCode;

    /**
     * 是否启用辅助核算
     */
    @TableField("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    /**
     * 基本结算数量
     *
     * 字段复用说明：
     * - 按量结算：存储实际结算数量
     * - 合同级总价包干：存储执行标识数量（1=1次/1年等）
     * - 关联运输记录总价包干：存储实际结算数量
     */
    @TableField("基本结算数量")
    private BigDecimal basicSettlementQuantity;

    /**
     * 基本计量单位
     *
     * 字段复用说明：
     * - 按量结算：存储实际计量单位（如"吨"）
     * - 合同级总价包干：存储执行单位（如"年"、"次"）
     * - 关联运输记录总价包干：存储实际计量单位
     */
    @TableField("基本计量单位")
    private String basicUnit;

    /**
     * 辅助结算数量（桶/袋/车等辅助单位）
     */
    @TableField("辅助结算数量")
    private BigDecimal auxiliarySettlementQuantity;

    /**
     * 辅助计量单位（如：桶、袋、车）
     */
    @TableField("辅助计量单位")
    private String auxiliaryUnit;

    /**
     * 合同条目ID（仅溯源）
     */
    @TableField("合同条目ID")
    private Integer contractItemId;

    /**
     * 合同计划总量（包干使用）
     */
    @TableField("合同计划总量")
    private BigDecimal contractPlanTotal;

    /**
     * 辅助合同计划总量（包干使用）
     */
    @TableField("辅助合同计划总量")
    private BigDecimal auxiliaryContractPlanTotal;

    /**
     * 累积已结算量
     */
    @TableField("累积已结算量")
    private BigDecimal settledBasicQuantity;

    /**
     * 辅助累积已结算量
     */
    @TableField("辅助累积已结算量")
    private BigDecimal settledAuxiliaryQuantity;

    /**
     * 本次累积量（本次结算后的累积总量，后端计算）
     */
    @TableField("本次累积量")
    private BigDecimal currentAccumulatedQuantity;

    /**
     * 超出量（本次明细的超出量）
     */
    @TableField("超出量")
    private BigDecimal excessQuantity;

    /**
     * 结算单价
     */
    @TableField("结算单价")
    private BigDecimal unitPrice;

    /**
     * 超出单价
     */
    @TableField("超出单价")
    private BigDecimal excessUnitPrice;

    /**
     * 保存原始单价
     */
    @TableField("保存原始单价")
    private BigDecimal saveUnitPrice;

    /**
     * 保存辅助单价
     */
    @TableField("保存辅助单价")
    private BigDecimal auxiliaryUnitPrice;

    /**
     * 超出金额（本次明细的超量金额）
     */
    @TableField("超出金额")
    private BigDecimal excessAmount;

    /**
     * 金额（本次明细的总金额）
     */
    @TableField("金额")
    private BigDecimal amount;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 是否锁定
     */
    @TableField("是否锁定")
    private Boolean isLocked;

    /**
     * 锁定时间
     */
    @TableField("锁定时间")
    private LocalDateTime lockTime;

    /**
     * 锁定人编码
     */
    @TableField("锁定人编码")
    private Integer lockUserId;

    /**
     * 付款方向：RECEIVABLE=收款，PAYABLE=付款
     */
    @TableField("付款方向")
    private String payer;

    /**
     * 更新人编码
     */
    @TableField("更新人编码")
    private Integer updateUserId;
}
