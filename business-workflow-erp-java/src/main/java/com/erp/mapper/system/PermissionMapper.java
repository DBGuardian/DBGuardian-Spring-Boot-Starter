package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.system.dto.PermissionResponse;
import com.erp.entity.system.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 分页查询权限列表（包含父权限名称）
     * @param page 分页参数
     * @param params 查询参数
     * @return 分页结果
     */
    IPage<PermissionResponse> selectPermissionPage(Page<PermissionResponse> page, @Param("params") Map<String, Object> params);

    /**
     * 根据条件查询权限列表（包含父权限名称）
     * @param params 查询参数
     * @return 权限列表
     */
    List<PermissionResponse> selectByCondition(@Param("params") Map<String, Object> params);

    /**
     * 查询所有权限（用于构建权限树）
     * @return 所有权限列表
     */
    List<Permission> selectAllPermissions();

    /**
     * 批量更新权限的父权限ID
     * @param permissionIds 权限ID列表
     * @param parentPermissionId 父权限ID（可为null）
     * @return 更新的行数
     */
    int batchUpdateParentPermissionId(@Param("permissionIds") List<Integer> permissionIds,
                                     @Param("parentPermissionId") Integer parentPermissionId);

    /**
     * 更新单个权限的父权限ID
     * @param permissionId 权限ID
     * @param parentPermissionId 父权限ID（可为null）
     * @return 更新的行数
     */
    int updateParentPermissionId(@Param("permissionId") Integer permissionId,
                                @Param("parentPermissionId") Integer parentPermissionId);

    /**
     * 查询指定权限被哪些角色使用
     * @param permissionId 权限ID
     * @return 使用该权限的角色ID列表
     */
    List<Integer> selectRoleIdsByPermissionId(@Param("permissionId") Integer permissionId);

    /**
     * 查询指定权限被哪些员工直接使用（员工级权限覆盖）
     * @param permissionId 权限ID
     * @return 使用该权限的员工ID列表
     */
    List<Integer> selectEmployeeIdsByPermissionId(@Param("permissionId") Integer permissionId);

    /**
     * 批量清空指定父权限的所有子权限的父权限编号
     * @param parentPermissionId 父权限ID
     * @return 更新的行数
     */
    int clearChildParentPermissionIds(@Param("parentPermissionId") Integer parentPermissionId);

}



