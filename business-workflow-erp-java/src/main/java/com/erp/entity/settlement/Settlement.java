package com.erp.entity.settlement;

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
 * 结算主表实体
 *
 * 对应表：SETTLEMENT
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SETTLEMENT")
public class Settlement extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 结算单编号（主键，自增）
     */
    @TableId(value = "结算单编号", type = IdType.AUTO)
    private Long settlementId;

    /**
     * 结算单单号（业务编号，如 JSD-20251228-0001）
     */
    @TableField("结算单单号")
    private String settlementCode;

    /**
     * 合同号
     */
    @TableField("合同号")
    private String contractCode;

    /**
     * 合同编号（可选，用于关联合同主表）
     */
    @TableField("合同编号")
    private Integer contractId;

    /**
     * 结算类型：RECEIVABLE=收款 / PAYABLE=付款
     */
    @TableField("结算类型")
    private String settlementType;

    /**
     * 关联来源类型：CONTRACT/WAREHOUSING/TRANSPORT
     */
    @TableField("关联来源类型")
    private String sourceType;

    /**
     * 结算周期起
     */
    @TableField("结算周期起")
    private LocalDateTime settlementPeriodStart;

    /**
     * 结算周期止
     */
    @TableField("结算周期止")
    private LocalDateTime settlementPeriodEnd;

    /**
     * 结算金额（合计，由后端/DB 计算并写入）
     */
    @TableField("结算金额")
    private BigDecimal settlementAmount;

    /**
     * 已收/已付金额
     */
    @TableField("已收金额")
    private BigDecimal receivedAmount;

    /**
     * 状态：待审核/审核中/已审核/已拒绝/已结算 等
     */
    @TableField("状态")
    private String status;

    /**
     * 制单人编码
     */
    @TableField("制单人编码")
    private Integer creatorId;

    /**
     * 制单人名称
     */
    @TableField("制单人名称")
    private String creatorName;

    /**
     * 审核人编码
     */
    @TableField("审核人编码")
    private Integer auditorId;

    /**
     * 审核时间
     */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    /**
     * 审核意见
     */
    @TableField("审核意见")
    private String auditOpinion;

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
     * 更新人编码
     */
    @TableField("更新人编码")
    private Integer updateUserId;
}
