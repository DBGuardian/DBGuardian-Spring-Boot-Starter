package com.erp.entity.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 客户跟进明细实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("CUSTOMER_FOLLOW_UP_DETAIL")
public class CustomerFollowUpDetail extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 跟进详细记录编号
     */
    @TableId(value = "跟进详细记录编号", type = IdType.AUTO)
    private Integer detailId;

    /**
     * 跟进记录编号
     */
    @TableField("跟进记录编号")
    private Integer followUpId;

    /**
     * 跟进时间
     */
    @TableField("跟进时间")
    private LocalDateTime followTime;

    /**
     * 跟进内容
     */
    @TableField("跟进内容")
    private String followContent;

    /**
     * 跟进状态：未完成/已完成
     */
    @TableField("跟进状态")
    private String followStatus;

    /**
     * 创建人编码（当前登录用户）
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 创建人姓名（非数据库字段，用于关联查询）
     */
    @TableField(exist = false)
    private String creatorName;
}
