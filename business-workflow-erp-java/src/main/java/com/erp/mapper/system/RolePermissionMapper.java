package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.system.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 批量插入角色权限关联
     *
     * @param rolePermissions 角色权限关联列表
     */
    void insertBatch(@Param("rolePermissions") List<RolePermission> rolePermissions);

    /**
     * 批量删除角色权限关联（根据角色ID和权限ID列表）
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     */
    void deleteBatch(@Param("roleId") Integer roleId, @Param("permissionIds") List<Integer> permissionIds);
}



