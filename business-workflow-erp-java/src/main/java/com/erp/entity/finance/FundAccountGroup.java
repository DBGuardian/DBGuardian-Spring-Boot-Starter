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
 * 账户组合实体（合并组织）
 *
 * 对应表：FUND_ACCOUNT_GROUP
 *
 * 字段说明参考《资金管理实现说明》文档：
 * - 组合编号：主键，自增
 * - 组合编码：业务编号（如：GRP-20260101-0001）
 * - 组合名称：如"主账户组合"、"备用金组合"
 * - 账户编号列表：JSON数组，存储账户ID列表
 * - 是否启用：1-启用，0-停用
 * - 备注：补充说明
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("FUND_ACCOUNT_GROUP")
public class FundAccountGroup extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组合编号（主键，自增）
     */
    @TableId(value = "组合编号", type = IdType.AUTO)
    private Long groupId;

    /**
     * 组合编码（业务编号，如：GRP-20260101-0001）
     */
    @TableField("组合编码")
    private String groupCode;

    /**
     * 组合名称（如：主账户组合、备用金组合）
     */
    @TableField("组合名称")
    private String groupName;

    /**
     * 账户编号列表（JSON数组，如：[1,2,3]）
     */
    @TableField("账户编号列表")
    private String accountIdsJson;

    /**
     * 是否启用（1-启用，0-停用）
     */
    @TableField("是否启用")
    private Boolean enabled;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer createUserId;

    /**
     * 更新人编码
     */
    @TableField("更新人编码")
    private Integer updateUserId;
}





































































