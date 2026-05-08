package com.erp.mapper.system;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统管理Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface SystemMapper {
    // TODO: 定义系统管理相关数据访问方法
    // 注意：等有对应的实体类后，再继承 BaseMapper<Entity>

    /**
     * 统计当删除某个权限（页面）时，受影响的员工数量（通过角色关联）
     *
     * @param permissionId 权限编号（页面）
     * @return 受影响的员工数量
     */
    int countEmployeesAffectedByPermission(@Param("permissionId") Integer permissionId);

    /**
     * 查询员工继承自角色的权限编码列表
     */
    java.util.List<String> selectPermissionCodesByEmployeeRoles(@Param("employeeId") Integer employeeId);

    /**
     * 按角色ID列表批量查询权限编码集合（多角色并集，去重）
     *
     * @param roleIds 角色ID列表
     * @return 权限编码集合
     */
    List<String> selectPermissionCodesByRoleIds(@Param("roleIds") List<Integer> roleIds);

    /**
     * 一次连表查询判断员工是否为管理员：
     * 同时检查 employee.角色 字段和关联角色名称，返回 1（是）或 0（否）
     *
     * @param employeeId 员工ID
     * @return 1 表示是管理员，0 表示不是
     */
    int isAdminByEmployeeId(@Param("employeeId") Integer employeeId);
}











































