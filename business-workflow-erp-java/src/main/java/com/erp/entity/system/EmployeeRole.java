package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 员工-角色关联（对应表 EMPLOYEE_ROLE）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("EMPLOYEE_ROLE")
public class EmployeeRole extends BaseEntity {

    @TableField("员工编码")
    private Integer employeeId;

    @TableField("角色编号")
    private Integer roleId;

    // EMPLOYEE_ROLE表没有创建时间和更新时间字段，需要排除
    @TableField(exist = false)
    private java.time.LocalDateTime createTime;

    @TableField(exist = false)
    private java.time.LocalDateTime updateTime;
}

































