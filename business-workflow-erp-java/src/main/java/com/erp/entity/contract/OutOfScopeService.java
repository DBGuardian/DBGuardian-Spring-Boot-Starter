package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价外费用实体 对应表：OUT_OF_SCOPE_SERVICE
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("OUT_OF_SCOPE_SERVICE")
public class OutOfScopeService extends BaseEntity {

    @TableId(value = "价外费用编号", type = IdType.AUTO)
    private Integer outOfScopeServiceId;

    @TableField("关联业务类型")
    private String businessType;

    @TableField("关联业务单号")
    private Integer businessId;

    @TableField("项目")
    private String project;

    @TableField("规格型号")
    private String spec;

    @TableField("单位")
    private String unit;

    @TableField("计划数量")
    private BigDecimal plannedQuantity;

    @TableField("合同单价")
    private BigDecimal contractUnitPrice;

    @TableField("对账结算数量")
    private BigDecimal settledQuantity;

    @TableField("对账结算单价")
    private BigDecimal settledUnitPrice;

    @TableField("对账结算金额")
    private BigDecimal settledAmount;

    @TableField("状态")
    private String status;

    /**
     * 创建时间（子类独立映射，覆盖 BaseEntity.createTime 的 \"创建时间\" 映射）
     * BaseEntity 中 createTime 映射 \"创建时间\"，此处显式定义避免歧义
     */
    @TableField("创建时间")
    private LocalDateTime createdAt;

    @TableField("创建人编码")
    private Integer createdBy;

    /**
     * 更新时间（子类独立映射，覆盖 BaseEntity.updateTime 的 \"更新时间\" 映射）
     */
    @TableField("更新时间")
    private LocalDateTime updatedAt;

    @TableField("更新人编码")
    private Integer updatedBy;

    @TableField("是否锁定")
    private Boolean locked;

    @TableField("锁定时间")
    private LocalDateTime lockedAt;

    @TableField("锁定人编码")
    private Integer lockedBy;

    @TableField("锁定原因")
    private String lockReason;

    @TableField("备注")
    private String remark;

}