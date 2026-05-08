package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结账检查记录实体类
 * 对应数据库表：FUND_SETTLEMENT_CHECK
 */
@Data
@TableName("FUND_SETTLEMENT_CHECK")
public class FundSettlementCheck {

    /**
     * 检查编号（主键，自增）
     */
    @TableId(value = "检查编号", type = IdType.AUTO)
    private Long checkId;

    /**
     * 账期编号
     */
    @TableField("账期编号")
    private Long periodId;

    /**
     * 账户编号
     */
    @TableField("账户编号")
    private Long accountId;

    /**
     * 期初余额（系统计算的）
     */
    @TableField("期初余额")
    private BigDecimal initialBalance;

    /**
     * 期末余额（系统计算的）
     */
    @TableField("期末余额")
    private BigDecimal finalBalance;

    /**
     * 银行对账余额
     */
    @TableField("银行对账余额")
    private BigDecimal bankBalance;


    /**
     * 检查时间
     */
    @TableField("检查时间")
    private LocalDateTime checkTime;

    /**
     * 检查人编码
     */
    @TableField("检查人编码")
    private Long checkUserId;
}

