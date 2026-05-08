package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.system.EmployeeRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工角色关联 Mapper
 *
 * @author ERP
 */
@Mapper
public interface EmployeeRoleMapper extends BaseMapper<EmployeeRole> {

    /**
     * 批量插入员工角色关系
     *
     * @param employeeId 员工ID
     * @param roleIds    角色ID列表
     */
    void insertEmployeeRoles(@Param("employeeId") Integer employeeId, @Param("roleIds") List<Integer> roleIds);


    /**
     * 根据员工ID删除角色关系
     *
     * @param employeeId 员工ID
     */
    void deleteByEmployeeId(@Param("employeeId") Integer employeeId);


    /**
     * 查询员工的角色名称列表
     *
     * @param employeeId 员工ID
     * @return 角色名称集合
     */
    List<String> selectRoleNamesByEmployeeId(@Param("employeeId") Integer employeeId);

    /**
     * 查询员工的角色ID列表
     *
     * @param employeeId 员工ID
     * @return 角色ID集合
     */
    List<Integer> selectRoleIdsByEmployeeId(@Param("employeeId") Integer employeeId);

}








