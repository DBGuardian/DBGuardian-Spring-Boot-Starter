package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 结账检查项设置实体类
 * 对应数据库表：FUND_SETTLEMENT_CHECK_ITEM（需要创建）
 * 
 * 注意：如果不想创建新表，也可以使用配置表或JSON配置存储
 */
@Data
@TableName("FUND_SETTLEMENT_CHECK_ITEM")
public class FundSettlementCheckItem {

    /**
     * 检查项编号（主键，自增）
     */
    @TableId(value = "检查项编号", type = IdType.AUTO)
    private Long checkItemId;

    /**
     * 检查项名称（如：检查期初余额、检查期末余额、检查银行对账余额）
     */
    @TableField("检查项名称")
    private String checkItemName;

    /**
     * 检查项编码（如：CHECK_INITIAL_BALANCE、CHECK_FINAL_BALANCE、CHECK_BANK_BALANCE）
     */
    @TableField("检查项编码")
    private String checkItemCode;

    /**
     * 是否启用（1-启用，0-禁用）
     */
    @TableField("是否启用")
    private Boolean enabled;

    /**
     * 检查项说明
     */
    @TableField("检查项说明")
    private String description;

    /**
     * 排序号
     */
    @TableField("排序号")
    private Integer sortOrder;

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
}

