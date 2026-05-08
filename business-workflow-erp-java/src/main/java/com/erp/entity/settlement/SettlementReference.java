package com.erp.entity.settlement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 结算关联表实体
 *
 * 对应表：SETTLEMENT_REFERENCE
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SETTLEMENT_REFERENCE")
public class SettlementReference extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 关联记录编号（主键，自增）
     */
    @TableId(value = "关联记录编号", type = IdType.AUTO)
    private Long referenceId;

    /**
     * 结算单编号（FK）
     */
    @TableField("结算单编号")
    private Long settlementId;

    /**
     * 关联来源类型：WAREHOUSING/TRANSPORT/CONTRACT
     */
    @TableField("关联来源类型")
    private String sourceType;

    /**
     * 关联来源单号（入库单号或运输单号或其他引用编号）
     */
    @TableField("关联来源单号")
    private String sourceCode;

    /**
     * 排除 BaseEntity 中的字段（SETTLEMENT_REFERENCE 表中没有这些字段）
     */
    @TableField(exist = false)
    private java.time.LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer version;
}
