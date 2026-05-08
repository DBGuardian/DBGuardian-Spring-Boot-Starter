package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 资金组织实体
 *
 * 对应表：fund_organization
 *
 * 用于实现组织-账户的层次结构管理：
 * - 组织作为一级实体，可以包含多个账户
 * - 支持组织的联系信息和基本信息管理
 * - 通过关联表与账户建立多对多关系
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fund_organization")
public class FundOrganization extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组织ID（主键，自增）
     */
    @TableId(value = "组织编号", type = IdType.AUTO)
    private Long organizationId;

    /**
     * 组织编码（业务编号，如：ORG-20260101-0001）
     */
    @TableField("组织编码")
    private String organizationCode;

    /**
     * 组织名称
     */
    @TableField("组织名称")
    private String organizationName;

    /**
     * 组织描述
     */
    @TableField("描述")
    private String description;

    /**
     * 是否启用
     */
    @TableField("是否启用")
    private Boolean enabled;

    /**
     * 创建用户ID
     */
    @TableField("创建人编码")
    private Long createUserId;

    /**
     * 更新用户ID
     */
    @TableField("更新人编码")
    private Long updateUserId;

    /**
     * 账户数量（非数据库字段，用于前端显示）
     */
    @TableField(exist = false)
    private Integer accountCount;
}
