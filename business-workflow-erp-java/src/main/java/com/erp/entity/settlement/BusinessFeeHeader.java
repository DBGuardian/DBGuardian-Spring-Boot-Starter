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
 * 业务结算单主表实体
 *
 * 对应表：BUSINESS_FEE_HEADER
 * 变更说明（2026-04-01）：
 *   - 主表仅保留基础信息与整体结算方向 settlementType
 *   - 删除 enablePaymentSelection、splitValuableWorthlessRows，相关配置改由前端本地展示逻辑处理
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("BUSINESS_FEE_HEADER")
public class BusinessFeeHeader extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "业务序号", type = IdType.AUTO)
    private Integer businessSeq;

    @TableField("业务单号")
    private String businessCode;

    @TableField("业务合同编号")
    private Integer businessContractId;

    @TableField("业务合同单号")
    private String businessContractNo;

    @TableField("业务员编码")
    private Integer salespersonId;

    @TableField("业务员姓名")
    private String salespersonName;

    @TableField("服务公司名称")
    private String serviceCompanyName;

    @TableField("结算类型")
    private String settlementType;

    @TableField("业务结算金额")
    private BigDecimal settlementAmount;

    @TableField("已收金额")
    private BigDecimal receivedAmount;

    @TableField("支付日期")
    private LocalDateTime paymentDate;

    @TableField("状态")
    private String status;

    @TableField("制单人编码")
    private Integer creatorId;

    @TableField("制单人名称")
    private String creatorName;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("审核人编码")
    private Integer auditorId;

    @TableField("审核时间")
    private LocalDateTime auditTime;

    @TableField("审核意见")
    private String auditOpinion;

    @TableField("备注")
    private String remark;

    @TableField("是否锁定")
    private Boolean isLocked;

    @TableField("锁定时间")
    private LocalDateTime lockTime;

    @TableField("锁定人编码")
    private Integer lockUserId;

    @TableField("version")
    private Integer version;

    @TableField("更新时间")
    private LocalDateTime updateTime;

    @TableField("更新人编码")
    private Integer updateUserId;
}
