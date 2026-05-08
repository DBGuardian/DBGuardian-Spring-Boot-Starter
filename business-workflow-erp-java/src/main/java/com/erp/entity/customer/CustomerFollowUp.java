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
 * 客户跟进实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("CUSTOMER_FOLLOW_UP")
public class CustomerFollowUp extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 跟进记录编号
     */
    @TableId(value = "跟进记录编号", type = IdType.AUTO)
    private Integer followUpId;

    /**
     * 客户编码
     */
    @TableField("客户编码")
    private Integer customerId;

    /**
     * 业务员编码（当前登录用户）
     */
    @TableField("业务员编码")
    private Integer employeeId;

    /**
     * 联系人姓名
     */
    @TableField("联系人姓名")
    private String contactName;

    /**
     * 联系人电话
     */
    @TableField("联系人电话")
    private String contactPhone;

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
     * 客户名称（非数据库字段）
     */
    @TableField(exist = false)
    private String customerName;

    /**
     * 业务员姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String employeeName;

    /**
     * 创建人姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String creatorName;
}
