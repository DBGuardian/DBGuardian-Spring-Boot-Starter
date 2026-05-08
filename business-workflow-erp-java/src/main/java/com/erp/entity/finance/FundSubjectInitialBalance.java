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
 * 科目级期初余额实体
 * 对应表：FUND_SUBJECT_INITIAL_BALANCE
 */
@Data
@TableName("FUND_SUBJECT_INITIAL_BALANCE")
public class FundSubjectInitialBalance {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("组织编号")
    private Long organizationId;

    @TableField("账期编号")
    private Long periodId;

    @TableField("科目编号")
    private Long subjectId;

    @TableField("账户编号")
    private Long accountId;

    @TableField("期初余额")
    private BigDecimal initialBalance;

    @TableField("余额方向")
    private String balanceDirection;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("更新时间")
    private LocalDateTime updateTime;

    @Version
    @TableField("version")
    private Integer version;
}

