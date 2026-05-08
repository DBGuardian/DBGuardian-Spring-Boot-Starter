package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 员工-页面级权限配置（对应表 employee_permission）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("employee_permission")
public class EmployeePermission implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableField("员工编码")
    private Integer employeeId;

    @TableField("页面权限编号")
    private Integer pagePermissionId;

    @TableField("可查看")
    private Integer canView;

    @TableField("可编辑")
    private Integer canEdit;

    @TableField("数据范围")
    private String viewScope;

    @TableField("操作范围")
    private String operateScope;

    @Version
    @TableField("version")
    private Integer version;
}



