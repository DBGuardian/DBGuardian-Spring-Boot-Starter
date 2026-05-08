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
 * 收运通知单实体
 *
 * 对应表：PICKUP_NOTICE
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("PICKUP_NOTICE")
public class PickupNotice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 收运通知单编号
     */
    @TableId(value = "收运通知单编号", type = IdType.AUTO)
    private Integer noticeId;

    /**
     * 收运通知单号（业务编码），规则：SYTTD+YYYYMMDDhhmm+4位随机数
     */
    @TableField("收运通知单号")
    private String noticeCode;

    /**
     * 合同号
     */
    @TableField("合同号")
    private String contractCode;

    /**
     * 合同待补标记
     */
    @TableField("合同待补")
    private Boolean contractPending;

    /**
     * 合同补齐时间
     */
    @TableField("合同补齐时间")
    private LocalDateTime contractFixTime;

    /**
     * 客户编码
     */
    @TableField("客户编码")
    private Integer customerId;

    /**
     * 单位名称（产生单位名称/发货单位）
     */
    @TableField("单位名称")
    private String companyName;

    /**
     * 统一社会信用代码
     */
    @TableField("统一社会信用代码")
    private String creditCode;

    /**
     * 运输地址
     */
    @TableField("运输地址")
    private String transportAddress;

    /**
     * 现场联系人
     */
    @TableField("现场联系人")
    private String onsiteContact;

    /**
     * 现场联系电话
     */
    @TableField("现成联系电话")
    private String onsitePhone;

    /**
     * 应急联系电话
     */
    @TableField("应急联系电话")
    private String emergencyPhone;

    /**
     * 业务联系人
     * 注意：数据库表中没有此字段，但前端需要，通过合同或客户信息关联获取
     */
    @TableField(exist = false)
    private String businessContact;

    /**
     * 业务联系电话
     * 注意：数据库表中没有此字段，但前端需要，通过合同或客户信息关联获取
     */
    @TableField(exist = false)
    private String businessPhone;

    /**
     * 转移交付时间（计划转移交付时间/装运时间）
     */
    @TableField("转移交付时间")
    private LocalDateTime planTransferDate;

    /**
     * 提交申请日期
     * 注意：数据库表中没有此字段，但前端需要，使用提交时间字段
     */
    @TableField(exist = false)
    private LocalDateTime submitDate;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 危废详情文件（附件路径/编号）
     */
    @TableField("危废详情文件")
    private String wasteDetailFile;

    /**
     * 产废单位二维码
     */
    @TableField("产废单位二维码")
    private String qrCode;

    /**
     * 状态：未提交/审核中/审核失败/待调度/已派单/已完成/已取消
     */
    @TableField("状态")
    private String status;

    /**
     * 提交时间
     */
    @TableField("提交时间")
    private LocalDateTime submittedAt;

    /**
     * 审核时间
     */
    @TableField("审核时间")
    private LocalDateTime auditedAt;

    /**
     * 审核人编码
     */
    @TableField("审核人编码")
    private Integer auditorId;

    /**
     * 审核人姓名（非数据库字段，通过JOIN查询获取）
     */
    @TableField(exist = false)
    private String auditorName;

    /**
     * 审核意见，记录审核通过或驳回的原因/说明
     */
    @TableField("审核意见")
    private String auditOpinion;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer creatorId;

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

    /**
     * 更新时间
     */
    @TableField("更新时间")
    private LocalDateTime updateTime;
}

