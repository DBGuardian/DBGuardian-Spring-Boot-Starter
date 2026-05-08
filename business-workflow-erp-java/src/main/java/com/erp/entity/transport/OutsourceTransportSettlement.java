package com.erp.entity.transport;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 委外运输结算单实体
 * 对应表：OUTSOURCE_TRANSPORT_SETTLEMENT
 *
 * 业务说明：
 * - 记录委外运输业务的结算信息
 * - 关联运输合同，记录结算周期、结算金额等
 * - 支持按趟次、按重量、按距离等多种结算方式
 * - 关联派车单通过 JSON 字段存储，后端自动汇总计算结算数量和金额
 */
@Data
@TableName("OUTSOURCE_TRANSPORT_SETTLEMENT")
public class OutsourceTransportSettlement {

    /**
     * 结算单主键ID
     */
    @TableId(value = "结算单编号", type = IdType.AUTO)
    private Integer settlementId;

    /**
     * 结算单编号（系统生成，格式如 OTSS-yyyyMMdd-XXXX）
     */
    @TableField("结算单单号")
    private String settlementNo;

    // ==================== 关联信息 ====================

    /**
     * 关联的运输合同编号（可为空，表示游离数据）
     */
    @TableField("合同编号")
    private Integer contractId;

    /**
     * 关联的运输合同单号
     */
    @TableField("合同单号")
    private String contractNo;

    // ==================== 承运方信息 ====================

    /**
     * 承运方名称
     */
    @TableField("承运方名称")
    private String carrierName;

    /**
     * 联系人
     */
    @TableField("联系人")
    private String contactPerson;

    /**
     * 联系电话
     */
    @TableField("联系电话")
    private String contactPhone;

    /**
     * 开户银行
     */
    @TableField("银行名称")
    private String bankName;

    /**
     * 银行卡号
     */
    @TableField("银行卡号")
    private String cardNumber;

    /**
     * 银行账户名称
     */
    @TableField("账户名称")
    private String accountName;

    // ==================== 结算周期 ====================

    /**
     * 结算周期开始日期
     */
    @TableField("结算周期起")
    private LocalDate settlementPeriodStart;

    /**
     * 结算周期结束日期
     */
    @TableField("结算周期止")
    private LocalDate settlementPeriodEnd;

    // ==================== 结算信息（核心） ====================

    /**
     * 结算方式：按趟次/按重量/按距离
     */
    @TableField("结算方式")
    private String settlementMethod;

    /**
     * 计量单位：趟/吨/公里
     */
    @TableField("计量单位")
    private String unit;

    /**
     * 结算数量（由派车单汇总计算）
     */
    @TableField("结算数量")
    private BigDecimal settlementQuantity;

    /**
     * 结算单价（元/趟 或 元/吨 或 元/公里）
     */
    @TableField("结算单价")
    private BigDecimal settlementPrice;

    /**
     * 结算金额 = 结算数量 × 结算单价
     */
    @TableField("结算金额")
    private BigDecimal settlementAmount;

    /**
     * 已付款金额
     */
    @TableField("已付金额")
    private BigDecimal paidAmount;

    // ==================== 状态与审核 ====================

    /**
     * 状态：待审核/审核中/已审核/已驳回/已付款
     */
    @TableField("状态")
    private String status;

    /**
     * 审核意见
     */
    @TableField("审核意见")
    private String auditOpinion;

    /**
     * 审核人员工编码
     */
    @TableField("审核人编码")
    private Integer auditorId;

    /**
     * 审核人姓名
     */
    @TableField("审核人姓名")
    private String auditorName;

    /**
     * 审核时间
     */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    // ==================== 付款信息 ====================

    /**
     * 付款方向：收款/付款
     */
    @TableField("付款方向")
    private String paymentDirection;

    // ==================== 锁定信息 ====================

    /**
     * 是否锁定
     */
    @TableField("是否锁定")
    private Boolean locked;

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
     * 锁定原因
     */
    @TableField("锁定原因")
    private String lockReason;

    // ==================== 其他 ====================

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 创建人员工编码
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 创建人姓名
     */
    @TableField("创建人姓名")
    private String creatorName;

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
     * 更新人员工编码
     */
    @TableField("更新人编码")
    private Integer updaterId;

    /**
     * 逻辑删除：0正常 1已删除
     */
    @TableField("是否删除")
    private Integer deleted;

    /**
     * 乐观锁版本号
     */
    @TableField("version")
    private Integer version;
}
