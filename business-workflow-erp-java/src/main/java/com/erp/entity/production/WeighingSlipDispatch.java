package com.erp.entity.production;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 总磅单-运输单关联实体
 * 对应表：WEIGHING_SLIP_DISPATCH
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("WEIGHING_SLIP_DISPATCH")
public class WeighingSlipDispatch extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 关联编号
     */
    @TableId(value = "关联编号", type = IdType.AUTO)
    private Integer relationId;

    /**
     * 总磅单编号（关联的总磅单 WEIGHING_SLIP.总磅单编号）
     */
    @TableField("总磅单编号")
    private Integer weighingSlipId;

    /**
     * 运输单号（关联的运输单号 DISPATCH_ORDER.运输单号）
     */
    @TableField("运输单号")
    private String dispatchCode;

    /**
     * 排除 BaseEntity 中的 updateTime 字段（WEIGHING_SLIP_DISPATCH 表中没有更新时间字段）
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;
}



