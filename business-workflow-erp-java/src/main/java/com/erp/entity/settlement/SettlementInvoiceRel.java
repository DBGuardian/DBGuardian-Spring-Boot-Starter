package com.erp.entity.settlement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单-发票关联关系实体类
 *
 * 对应表：SETTLEMENT_INVOICE_REL
 *
 * @author ERP System
 * @date 2026-01-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SETTLEMENT_INVOICE_REL")
public class SettlementInvoiceRel extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关系编号
     */
    @TableId(value = "关系编号", type = IdType.AUTO)
    private Integer relId;

    /**
     * 结算单编号
     */
    @TableField("结算单编号")
    private Integer settlementId;

    /**
     * 发票编号
     */
    @TableField("发票编号")
    private Integer invoiceId;

    /**
     * 开票通知单编号
     */
    @TableField("开票通知单编号")
    private Integer noticeId;

    /**
     * 关联金额
     */
    @TableField("关联金额")
    private BigDecimal relAmount;

    /**
     * 关联类型
     */
    @TableField("关联类型")
    private String relType;

    /**
     * 关联时间
     */
    @TableField("关联时间")
    private LocalDateTime relTime;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer createUserId;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 关联状态
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间（表中不存在此字段，排除）
     */
    @TableField(value = "创建时间", exist = false)
    private LocalDateTime createTime;

    /**
     * 更新时间（表中不存在此字段，排除）
     */
    @TableField(value = "更新时间", exist = false)
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     */
    @TableField("version")
    @Version
    private Integer version;
}