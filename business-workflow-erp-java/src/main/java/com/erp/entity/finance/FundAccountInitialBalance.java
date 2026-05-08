package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户期初余额实体类
 * 对应数据库表：FUND_ACCOUNT_INITIAL_BALANCE
 */
@Data
@TableName("FUND_ACCOUNT_INITIAL_BALANCE")
public class FundAccountInitialBalance {

    /**
     * 期初余额编号（主键，自增）
     */
    @TableId(value = "期初余额编号", type = IdType.AUTO)
    private Long initialBalanceId;

    /**
     * 账户编号
     */
    @TableField("账户编号")
    private Long accountId;

    /**
     * 账期编号
     */
    @TableField("账期编号")
    private Long periodId;

    /**
     * 期初余额
     */
    @TableField("期初余额")
    private BigDecimal initialBalance;

    /**
     * 余额方向：收入、支出
     */
    @TableField("余额方向")
    private String balanceDirection;

    /**
     * 银行对账余额
     */
    @TableField("银行对账余额")
    private BigDecimal bankBalance;

    /**
     * 创建时间
     */
    @TableField("创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("更新时间")
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;
}


