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
 * 入库单实体
 * 对应表：WAREHOUSING
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("WAREHOUSING")
public class Warehousing extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 入库单编号
     */
    @TableId(value = "入库单编号", type = IdType.AUTO)
    private Integer warehousingId;

    /**
     * 入库单号（规则：RKD-YYYYMMDD-4位序号）
     */
    @TableField("入库单号")
    private String warehousingNo;

    /**
     * 总磅单号（关联的总磅单号，运输单号可通过总磅单获取）
     */
    @TableField("总磅单号")
    private String weighingSlipNo;

    /**
     * 收运运输单号（关联的收运运输单号，可通过收运运输单获取收运通知单号来获取合同号）
     */
    @TableField("收运运输单号")
    private String dispatchCode;

    /**
     * 入库时间
     */
    @TableField("入库时间")
    private LocalDateTime warehousingTime;

    /**
     * 仓管员编码（执行入库的仓管员编码）
     */
    @TableField("仓管员编码")
    private Integer warehouseKeeperId;

    /**
     * 审核时间（审核通过后生成结算单）
     */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    /**
     * 审核人编码（审核入库单的仓管员编码）
     */
    @TableField("审核人编码")
    private Integer auditorId;

    /**
     * 入库备注
     */
    @TableField("入库备注")
    private String remark;

    /**
     * 状态：待结算/结算中/已结算/已锁定
     */
    @TableField("状态")
    private String status;

    /**
     * 是否锁定（合同完结后自动锁定，锁定后不可修改、不可删除）
     */
    @TableField("是否锁定")
    private Boolean locked;

    /**
     * 锁定时间（合同完结时自动设置）
     */
    @TableField("锁定时间")
    private LocalDateTime lockTime;

    /**
     * 锁定人编码（系统自动锁定或管理员锁定）
     */
    @TableField("锁定人编码")
    private Integer lockUserId;

    /**
     * 锁定原因（如：合同已完结/合同已归档）
     */
    @TableField("锁定原因")
    private String lockReason;

    /**
     * 创建人编码（操作人ID，使用仓管员编码字段存储）
     * 对应数据库字段：仓管员编码
     */
    @TableField("仓管员编码")
    private Integer creatorId;

}

