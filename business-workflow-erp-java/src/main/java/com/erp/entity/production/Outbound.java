package com.erp.entity.production;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出库单实体
 * 对应表：OUTBOUND
 */
@Data
@TableName("OUTBOUND")
public class Outbound {

    private static final long serialVersionUID = 1L;

    /** 出库单编号 */
    @TableId(value = "出库单编号", type = IdType.AUTO)
    private Integer outboundId;

    /** 出库单号（规则：CKD-YYYYMMDD-4位序号） */
    @TableField("出库单号")
    private String outboundNo;

    /** 出库类型：处置出库/转移出库/销售出库/退货出库/其他出库 */
    @TableField("出库类型")
    private String outboundType;

    /** 实际出库时间 */
    @TableField("出库时间")
    private LocalDateTime outboundTime;

    /** 经办人编码 */
    @TableField("经办人编码")
    private Integer handlerId;

    /** 审核时间 */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    /** 审核人编码 */
    @TableField("审核人编码")
    private Integer auditorId;

    /** 审核意见 */
    @TableField("审核意见")
    private String auditOpinion;

    /** 关联合同号 */
    @TableField("关联合同号")
    private String contractCode;

    /** 关联客户编号 */
    @TableField("关联客户编号")
    private Integer customerId;

    /** 出库去向类型：客户/处置车间/外部单位/其他 */
    @TableField("出库去向类型")
    private String destinationType;

    /** 出库接收方或去向名称 */
    @TableField("出库去向名称")
    private String destinationName;

    /** 出库备注 */
    @TableField("出库备注")
    private String remark;

    /** 创建时间 */
    @TableField("创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("更新时间")
    private LocalDateTime updateTime;

    /** 状态：待审核/已审核/已作废/已锁定 */
    @TableField("状态")
    private String status;

    /** 是否锁定 */
    @TableField("是否锁定")
    private Boolean locked;

    /** 锁定时间 */
    @TableField("锁定时间")
    private LocalDateTime lockTime;

    /** 锁定人编码 */
    @TableField("锁定人编码")
    private Integer lockUserId;

    /** 锁定原因 */
    @TableField("锁定原因")
    private String lockReason;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
