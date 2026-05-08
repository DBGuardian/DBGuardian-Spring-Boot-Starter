package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 账期实体类
 * 对应数据库表：FUND_PERIOD
 */
@Data
@TableName("FUND_PERIOD")
public class FundPeriod {

    /**
     * 账期编号（主键，自增）
     */
    @TableId(value = "账期编号", type = IdType.AUTO)
    private Long periodId;

    /**
     * 账期编码（如：202303）
     */
    @TableField("账期编码")
    private String periodCode;

    /**
     * 年份（如：2023）
     */
    @TableField("年份")
    private Integer year;

    /**
     * 月份（1-12，表示1月到12月）
     */
    @TableField("月份")
    private Integer month;

    /**
     * 组织编号
     */
    @TableField("组织编号")
    private Long organizationId;

    /**
     * 是否已结账（1-已结账，0-未结账）
     */
    @TableField("是否已结账")
    private Boolean isSettled;

    /**
     * 结账时间
     */
    @TableField("结账时间")
    private LocalDateTime settlementTime;

    /**
     * 结账人编码
     */
    @TableField("结账人编码")
    private Long settlementUserId;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Long createUserId;

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

    /**
     * 获取账期开始日期（每月1日）
     * @return 开始日期
     */
    public LocalDate getStartDate() {
        if (this.year == null || this.month == null) {
            return null;
        }
        return LocalDate.of(this.year, this.month, 1);
    }

    /**
     * 获取账期结束日期（每月最后一天）
     * @return 结束日期
     */
    public LocalDate getEndDate() {
        if (this.year == null || this.month == null) {
            return null;
        }
        YearMonth yearMonth = YearMonth.of(this.year, this.month);
        return yearMonth.atEndOfMonth();
    }
}

