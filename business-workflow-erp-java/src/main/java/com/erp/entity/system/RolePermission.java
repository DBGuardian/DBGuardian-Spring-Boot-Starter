package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 角色-权限关联（对应表 ROLE_PERMISSION）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ROLE_PERMISSION")
public class RolePermission extends BaseEntity {

    @TableId(value = "角色编号", type = IdType.INPUT)
    private Integer roleId;

    @TableField("权限编号")
    private Integer permissionId;

    // ROLE_PERMISSION表没有创建时间和更新时间字段，需要排除
    @TableField(exist = false)
    private java.time.LocalDateTime createTime;

    @TableField(exist = false)
    private java.time.LocalDateTime updateTime;
}



