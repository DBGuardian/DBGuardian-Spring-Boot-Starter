package com.erp.controller.transport.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 委外运输结算单响应
 */
@Data
public class OutsourceSettlementResponse {

    /**
     * 结算单编号
     */
    private Integer settlementId;

    /**
     * 结算单编号（系统生成，格式如 OTSS-yyyyMMdd-XXXX）
     */
    private String settlementNo;

    // ==================== 关联信息 ====================

    /**
     * 关联的运输合同编号
     */
    private Integer contractId;

    /**
     * 关联的运输合同单号
     */
    private String contractNo;

    // ==================== 承运方信息 ====================

    /**
     * 承运方名称
     */
    private String carrierName;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 开户银行
     */
    private String bankName;

    /**
     * 银行卡号
     */
    private String cardNumber;

    /**
     * 银行账户名称
     */
    private String accountName;

    // ==================== 结算周期 ====================

    /**
     * 结算周期开始日期
     */
    private LocalDate settlementPeriodStart;

    /**
     * 结算周期结束日期
     */
    private LocalDate settlementPeriodEnd;

    // ==================== 结算信息 ====================

    /**
     * 结算方式：按趟次/按重量/按距离
     */
    private String settlementMethod;

    /**
     * 计量单位：趟/吨/公里
     */
    private String unit;

    /**
     * 结算数量
     */
    private BigDecimal settlementQuantity;

    /**
     * 结算单价
     */
    private BigDecimal settlementPrice;

    /**
     * 结算金额
     */
    private BigDecimal settlementAmount;

    /**
     * 已付款金额
     */
    private BigDecimal paidAmount;

    /**
     * 未付款金额
     */
    private BigDecimal unpaidAmount;

    // ==================== 关联总磅单 ====================

    /**
     * 关联的总磅单ID列表
     */
    private List<Integer> slipIds;

    /**
     * 关联的总磅单信息列表（用于展示）
     */
    private List<OutsourceTransportSettlementSlipVO> slips;

    // ==================== 状态与审核 ====================

    /**
     * 状态
     */
    private String status;

    /**
     * 审核意见
     */
    private String auditOpinion;

    /**
     * 审核人员工编码
     */
    private Integer auditorId;

    /**
     * 审核人姓名
     */
    private String auditorName;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    // ==================== 付款信息 ====================

    /**
     * 付款方向：收款/付款
     */
    private String paymentDirection;

    // ==================== 其他 ====================

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人员工编码
     */
    private Integer creatorId;

    /**
     * 创建人姓名
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 更新人员工编码
     */
    private Integer updaterId;

    /**
     * 是否锁定
     */
    private Boolean locked;

    /**
     * 锁定时间
     */
    private LocalDateTime lockTime;

    /**
     * 锁定人编码
     */
    private Integer lockUserId;

    /**
     * 锁定原因
     */
    private String lockReason;

    /**
     * 逻辑删除标记
     */
    private Integer deleted;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 派车单简单信息
     */
    @Data
    public static class DispatchOrderSimple {
        /**
         * 派车单编号
         */
        private Integer dispatchId;

        /**
         * 派车单号
         */
        private String dispatchNo;

        /**
         * 派车日期
         */
        private LocalDate dispatchDate;

        /**
         * 车牌号
         */
        private String licensePlate;

        /**
         * 司机姓名
         */
        private String driverName;

        /**
         * 运输起点
         */
        private String transportStart;

        /**
         * 运输终点
         */
        private String transportEnd;

        /**
         * 运输数量（吨）
         */
        private BigDecimal transportQuantity;

        /**
         * 运输距离（公里）
         */
        private BigDecimal transportDistance;

        /**
         * 完成时间
         */
        private LocalDateTime completionTime;
    }
}
