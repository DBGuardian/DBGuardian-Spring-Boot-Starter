package com.erp.service.system;

import com.erp.controller.system.dto.RoleInfoDTO;
import com.erp.controller.system.dto.RoleUpdateRequest;
import com.erp.entity.system.Role;

import java.util.List;
import java.util.Map;

public interface RoleService {
    List<Role> listAll();
    List<RoleInfoDTO> listAllWithDetails(String roleCode, String roleName, String roleDesc);
    Integer create(Role role);
    void update(Integer id, RoleUpdateRequest request);
    Map<String, Object> delete(Integer id, boolean confirm);
    void batchDelete(List<Integer> ids);
    void setRolePermissions(Integer id, List<Integer> permissionIds);
    List<Integer> getRolePermissions(Integer id);
    List<Integer> getRoleMembers(Integer id);

    /**
     * 批量获取多个角色的权限编码并集（去重）
     *
     * @param roleIds 角色ID列表
     * @return 权限编码集合
     */
    List<String> batchGetPermissionCodesByRoleIds(List<Integer> roleIds);
}


