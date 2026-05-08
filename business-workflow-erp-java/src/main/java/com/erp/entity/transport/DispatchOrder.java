package com.erp.entity.transport;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 运输单实体
 * 对应表：DISPATCH_ORDER
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("DISPATCH_ORDER")
public class DispatchOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 运输单编号
     */
    @TableId(value = "运输单编号", type = IdType.AUTO)
    private Integer dispatchId;

    /**
     * 运输单号
     */
    @TableField("运输单号")
    private String dispatchCode;

    /**
     * 调度员编码
     */
    @TableField("调度员编码")
    private Integer dispatcherId;

    /**
     * 收运通知单号
     */
    @TableField("收运通知单号")
    private String noticeCode;

    /**
     * 承运单位名称
     */
    @TableField("承运单位名称")
    private String carrierName;

    /**
     * 营运证件号
     */
    @TableField("营运证件号")
    private String operationLicenseNo;

    /**
     * 承运单位地址
     */
    @TableField("承运单位地址")
    private String carrierAddress;

    /**
     * 承运单位联系电话
     */
    @TableField("承运单位联系电话")
    private String carrierPhone;

    /**
     * 驾驶员姓名
     */
    @TableField("驾驶员姓名")
    private String driverName;

    /**
     * 驾驶员联系电话
     */
    @TableField("驾驶员联系电话")
    private String driverPhone;

    /**
     * 运输工具
     */
    @TableField("运输工具")
    private String transportTool;

    /**
     * 车辆编号
     */
    @TableField("车辆编号")
    private Integer vehicleId;

    /**
     * 运输车辆号牌
     */
    @TableField("运输车辆号牌")
    private String plateNo;

    /**
     * 运输起点
     */
    @TableField("运输起点")
    private String startPoint;

    /**
     * 实际起运时间
     */
    @TableField("实际起运时间")
    private LocalDateTime departAt;

    /**
     * 总计划转移数量（吨）
     */
    @TableField("总计划转移数量")
    private Double planQuantityTon;

    /**
     * 派车时间
     */
    @TableField("派车时间")
    private LocalDateTime dispatchAt;

    /**
     * 调度备注
     */
    @TableField("调度备注")
    private String dispatcherRemark;

    /**
     * 状态：待运输/运输中/已到达/已完成/已取消
     */
    @TableField("状态")
    private String status;

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

    /* ------------- 非数据库字段 ------------- */

    @TableField(exist = false)
    private String endPoint;

    @TableField(exist = false)
    private LocalDateTime arriveAt;

    @TableField(exist = false)
    private Boolean contractPending;

    @TableField(exist = false)
    private String contractCode;

    @TableField(exist = false)
    private Boolean contractMissing;

    @TableField(exist = false)
    private Boolean overLimit;

    @TableField(exist = false)
    private String weighingSlipCode;
}


