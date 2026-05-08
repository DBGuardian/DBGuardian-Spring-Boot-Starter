package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 角色实体（对应表 ROLE）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ROLE")
public class Role extends BaseEntity {

    @TableId("角色编号")
    private Integer roleId;

    @TableField("角色名称")
    private String roleName;

    @TableField("角色编码")
    private String roleCode;

    @TableField("角色描述")
    private String roleDesc;

    @TableField("protected_flag")
    private Integer protectedFlag;

    @TableField("protected_reason")
    private String protectedReason;

    @TableField("protected_by")
    private Integer protectedBy;

    @TableField("protected_at")
    private java.time.LocalDateTime protectedAt;

    // ROLE表没有创建时间和更新时间字段，需要排除
    @TableField(exist = false)
    private java.time.LocalDateTime createTime;

    @TableField(exist = false)
    private java.time.LocalDateTime updateTime;
}



