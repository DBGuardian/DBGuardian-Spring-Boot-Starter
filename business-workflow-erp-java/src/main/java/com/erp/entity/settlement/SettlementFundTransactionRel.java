package com.erp.entity.settlement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单 - 资金流水 关联实体
 * 对应表：SETTLEMENT_FUND_TRANSACTION_REL
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SETTLEMENT_FUND_TRANSACTION_REL")
public class SettlementFundTransactionRel extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "关系编号", type = IdType.AUTO)
    private Long relId;

    @TableField("结算单编号")
    private Long settlementId;

    @TableField("资金流水编号")
    private Long transactionId;

    @TableField("关联金额")
    private BigDecimal relAmount;

    @TableField("关联时间")
    private LocalDateTime relTime;

    @TableField("创建人编码")
    private Integer createUserId;

    @TableField("备注")
    private String remark;

    // 表中无 status 列，保留字段但不映射到数据库（避免生成包含 status 的 SQL）
    @TableField(value = "status", exist = false)
    private String status;
    // 表中无 创建时间/更新时间 列，shadow 父类字段以避免被 MyBatis 选取
    @TableField(value = "创建时间", exist = false)
    private java.time.LocalDateTime createTime;

    @TableField(value = "更新时间", exist = false)
    private java.time.LocalDateTime updateTime;
}

