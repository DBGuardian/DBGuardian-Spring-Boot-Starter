package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.system.EmployeePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeePermissionMapper extends BaseMapper<EmployeePermission> {

    /**
     * 根据员工ID和权限ID列表删除员工权限关系
     */
    int deleteByEmployeeAndPermissionIds(@Param("employeeId") Integer employeeId, @Param("permissionIdsCsv") String permissionIdsCsv);

    /**
     * 批量插入员工权限
     */
    int batchInsert(@Param("list") List<EmployeePermission> list);

    /**
     * 批量更新员工权限
     */
    int batchUpdate(@Param("list") List<EmployeePermission> list);

    /**
     * 根据员工ID查询所有页面权限
     */
    List<EmployeePermission> selectByEmployeeId(@Param("employeeId") Integer employeeId);
}


