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
 * 权限实体，对应表 PERMISSION
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("PERMISSION")
public class Permission extends BaseEntity {

    @TableId("权限编号")
    private Integer permissionId;

    @TableField("权限名称")
    private String permissionName;

    @TableField("权限描述")
    private String permissionDescription;

    @TableField("权限类型编号")
    private Integer permissionTypeId;

    @TableField("权限编码")
    private String permissionCode;

    /**
     * 页面模式
     * 仅对 PAGE 类型权限生效：
     * - VIEW_ONLY = 仅查看页面
     * - LIST      = 可编辑列表页
     * 其他类型为 NULL
     */
    @TableField("页面模式")
    private String pageMode;

    @TableField("父权限编号")
    private Integer parentPermissionId;
    
    @TableField("创建时间")
    private java.time.LocalDateTime createTime;

    @TableField("更新时间")
    private java.time.LocalDateTime updateTime;
}



