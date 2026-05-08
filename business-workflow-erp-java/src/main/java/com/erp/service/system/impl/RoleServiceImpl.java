package com.erp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.controller.system.dto.RoleInfoDTO;
import com.erp.entity.system.EmployeeRole;
import com.erp.entity.system.Role;
import com.erp.entity.system.RolePermission;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.SystemMapper;
import com.erp.mapper.system.RoleMapper;
import com.erp.mapper.system.RolePermissionMapper;
import com.erp.service.system.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import com.erp.common.constant.RedisConstant;
import com.erp.controller.system.dto.RoleUpdateRequest;
import com.erp.service.system.ILogRecordService;
import com.erp.common.util.SecurityUtil;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private SystemMapper systemMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<Role> listAll() {
        return roleMapper.selectList(new QueryWrapper<>());
    }

    @Override
    public List<RoleInfoDTO> listAllWithDetails(String roleCode, String roleName, String roleDesc) {
        // 构建查询条件
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(roleCode)) {
            queryWrapper.like("角色编码", roleCode.trim());
        }
        if (StringUtils.hasText(roleName)) {
            queryWrapper.like("角色名称", roleName.trim());
        }
        if (StringUtils.hasText(roleDesc)) {
            queryWrapper.like("角色描述", roleDesc.trim());
        }

        // 查询角色
        List<Role> roles = roleMapper.selectList(queryWrapper);

        return roles.stream().map(role -> {
            // 查询角色权限数量
            long permissionCount = rolePermissionMapper.selectCount(
                new QueryWrapper<RolePermission>().eq("角色编号", role.getRoleId())
            );

            // 查询角色用户数量
            long userCount = employeeRoleMapper.selectCount(
                new QueryWrapper<EmployeeRole>().eq("角色编号", role.getRoleId())
            );

            // 转换为DTO
            return RoleInfoDTO.builder()
                    .roleId(role.getRoleId())
                    .roleName(role.getRoleName())
                    .roleCode(role.getRoleCode())
                    .roleDesc(role.getRoleDesc())
                    .protectedFlag(role.getProtectedFlag())
                    .permissionCount((int) permissionCount)
                    .userCount((int) userCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(Role role) {
        // 生成唯一的角色编码
        String roleCode = generateRoleCode(role.getRoleName());

        // 设置默认值
        role.setRoleCode(roleCode);
        role.setProtectedFlag(0);  // 默认非保护角色
        role.setProtectedReason(null);
        role.setProtectedBy(SecurityUtil.getCurrentUserId());  // 当前登录员工ID
        role.setProtectedAt(java.time.LocalDateTime.now());   // 当前时间
        // version字段由BaseEntity中的@Version注解自动处理，初始值为0

        roleMapper.insert(role);
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("角色管理", "新增角色", "roleId=" + role.getRoleId() + ", code=" + roleCode, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录新增角色操作日志失败", e);
        }
        // 精确刷新：删除该角色的权限缓存（如果存在）
        try {
            redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "role:" + role.getRoleId());
        } catch (Exception e) {
            log.warn("删除角色权限缓存失败 roleId={}", role.getRoleId(), e);
        }
        return role.getRoleId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, RoleUpdateRequest request) {
        Role r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "角色不存在");

        // 只更新允许的字段：角色名称和描述
        r.setRoleName(request.getRoleName());
        r.setRoleDesc(request.getRoleDesc());

        int rows = roleMapper.updateById(r);
        if (rows == 0) {
            throw new BusinessException("更新角色失败：记录已被其他用户修改");
        }
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("角色管理", "更新角色", "roleId=" + id, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录更新角色操作日志失败", e);
        }
        // 精确刷新：删除该角色缓存及其成员的员工缓存
        invalidateCachesByRoleId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> delete(Integer id, boolean confirm) {
        Role r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "角色不存在");

        // 检查是否是系统保护角色
        if (r.getProtectedFlag() != null && r.getProtectedFlag() == 1) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "系统保护角色，不允许删除");
        }

        // 统计有多少员工被分配了这个角色
        long affected = employeeRoleMapper.selectCount(new QueryWrapper<EmployeeRole>().eq("角色编号", id));
        if (!confirm) return Collections.singletonMap("affectedEmployeeCount", affected);

        // 在单个事务中执行所有删除操作
        try {
            // 1. 删除角色权限关联（一次SQL执行）
            rolePermissionMapper.delete(new QueryWrapper<RolePermission>().eq("角色编号", id));

            // 2. 删除员工角色关联（一次SQL执行）
            employeeRoleMapper.delete(new QueryWrapper<EmployeeRole>().eq("角色编号", id));

            // 3. 删除角色本身（一次SQL执行）
            int delRows = roleMapper.deleteById(id);
            if (delRows == 0) {
                throw new BusinessException("删除角色失败：记录不存在或已被删除");
            }

            // 记录操作日志
            try {
                Integer userId = SecurityUtil.getCurrentUserId();
                logRecordService.recordOperationLog("角色管理", "删除角色", "roleId=" + id + ", affectedEmployees=" + affected, userId, null, true, null);
            } catch (Exception e) {
                log.warn("记录删除角色操作日志失败", e);
            }

            // 精确刷新：删除该角色缓存及其成员的员工缓存
            invalidateCachesByRoleId(id);

            return Collections.singletonMap("success", true);
        } catch (Exception e) {
            log.error("删除角色失败，事务回滚 roleId={}", id, e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除角色失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要删除的角色");
        }

        // 1. 批量查询所有角色（一次查询）
        List<Role> roles = roleMapper.selectBatchIds(ids);
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "角色不存在");
        }

        // 检查是否有受保护的角色
        List<String> protectedRoles = roles.stream()
                .filter(r -> r.getProtectedFlag() != null && r.getProtectedFlag() == 1)
                .map(Role::getRoleName)
                .collect(Collectors.toList());
        if (!protectedRoles.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(),
                    "系统保护角色不能删除：" + String.join("、", protectedRoles));
        }

        // 2. 收集需要清理的关联数据
        Set<Integer> roleIdSet = new HashSet<>(ids);

        try {
            // 3. 删除角色权限关联（一次SQL执行，使用 IN 子句）
            rolePermissionMapper.delete(new QueryWrapper<RolePermission>().in("角色编号", ids));

            // 4. 删除员工角色关联（一次SQL执行，使用 IN 子句）
            employeeRoleMapper.delete(new QueryWrapper<EmployeeRole>().in("角色编号", ids));

            // 5. 批量删除角色（一次SQL执行，使用 IN 子句）
            int deleteCount = roleMapper.deleteBatchIds(ids);
            log.info("批量删除角色成功：请求数量={}，实际删除={}", ids.size(), deleteCount);

            // 6. 记录操作日志
            try {
                Integer userId = SecurityUtil.getCurrentUserId();
                logRecordService.recordOperationLog("角色管理", "批量删除角色",
                        "批量删除角色，数量=" + ids.size() + "，ID列表=" + ids, userId, null, true, null);
            } catch (Exception e) {
                log.warn("记录批量删除角色操作日志失败", e);
            }

            // 7. 清理相关缓存
            for (Integer roleId : ids) {
                invalidateCachesByRoleId(roleId);
            }
        } catch (Exception e) {
            log.error("批量删除角色失败，事务回滚，ids={}", ids, e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量删除角色失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRolePermissions(Integer id, List<Integer> permissionIds) {
        // 1. 查询当前角色已有的权限ID列表（一次SQL查询）
        List<RolePermission> existingRolePermissions = rolePermissionMapper.selectList(
            new QueryWrapper<RolePermission>().eq("角色编号", id)
        );
        Set<Integer> existingPermissionIds = existingRolePermissions.stream()
            .map(RolePermission::getPermissionId)
            .collect(Collectors.toSet());
        
        int oldPermissionCount = existingPermissionIds.size();
        log.info("角色当前权限：roleId={}, count={}, permissionIds={}", id, oldPermissionCount, existingPermissionIds);

        // 2. 处理新权限ID列表（去重、去null）
        Set<Integer> newPermissionIds = (permissionIds != null && !permissionIds.isEmpty())
            ? permissionIds.stream()
                .filter(pid -> pid != null)
                .collect(Collectors.toSet())
            : new HashSet<>();
        
        int newPermissionCount = newPermissionIds.size();
        log.info("新权限列表：roleId={}, count={}, permissionIds={}", id, newPermissionCount, newPermissionIds);

        // 3. 计算需要删除的权限（已有权限 - 新权限）
        Set<Integer> toDeletePermissionIds = existingPermissionIds.stream()
            .filter(pid -> !newPermissionIds.contains(pid))
            .collect(Collectors.toSet());

        // 4. 计算需要新增的权限（新权限 - 已有权限）
        Set<Integer> toAddPermissionIds = newPermissionIds.stream()
            .filter(pid -> !existingPermissionIds.contains(pid))
            .collect(Collectors.toSet());

        log.info("权限差分计算：roleId={}, 需删除={}, 需新增={}", 
            id, toDeletePermissionIds.size(), toAddPermissionIds.size());

        // 5. 批量删除需要删除的权限（一次SQL执行，如果有需要删除的）
        if (!toDeletePermissionIds.isEmpty()) {
            List<Integer> deleteList = new ArrayList<>(toDeletePermissionIds);
            rolePermissionMapper.deleteBatch(id, deleteList);
            log.info("批量删除角色权限：roleId={}, deletedCount={}, permissionIds={}", 
                id, deleteList.size(), deleteList);
        }

        // 6. 批量插入需要新增的权限（一次SQL执行，如果有需要新增的）
        if (!toAddPermissionIds.isEmpty()) {
            List<RolePermission> rolePermissionsToAdd = toAddPermissionIds.stream()
                .map(permissionId -> RolePermission.builder()
                    .roleId(id)
                    .permissionId(permissionId)
                    .build())
                .collect(Collectors.toList());
            
            rolePermissionMapper.insertBatch(rolePermissionsToAdd);
            log.info("批量插入角色权限：roleId={}, insertedCount={}, permissionIds={}", 
                id, rolePermissionsToAdd.size(), toAddPermissionIds);
        }

        // 7. 如果没有任何变更，记录日志
        if (toDeletePermissionIds.isEmpty() && toAddPermissionIds.isEmpty()) {
            log.info("角色权限无变更：roleId={}, permissionCount={}", id, oldPermissionCount);
        } else {
            log.info("角色权限更新完成：roleId={}, 权限数量: {} -> {}", id, oldPermissionCount, newPermissionCount);
        }

        // 8. 记录操作日志
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            String operationDetail = String.format("roleId=%d, 权限数量: %d -> %d, 删除=%d, 新增=%d",
                id, oldPermissionCount, newPermissionCount, 
                toDeletePermissionIds.size(), toAddPermissionIds.size());
            logRecordService.recordOperationLog("角色管理", "设置角色权限", operationDetail, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录设置角色权限操作日志失败", e);
        }

        // 9. 刷新缓存（如果有变更）
        if (!toDeletePermissionIds.isEmpty() || !toAddPermissionIds.isEmpty()) {
            invalidateCachesByRoleId(id);
        }
    }

    /**
     * 删除角色级缓存和该角色下所有员工的权限缓存
     */
    private void invalidateCachesByRoleId(Integer roleId) {
        try {
            // 删除角色级缓存
            redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "role:" + roleId);
        } catch (Exception e) {
            log.warn("删除角色权限缓存失败 roleId={}", roleId, e);
        }
        try {
            // 删除该角色下所有员工的缓存
            List<EmployeeRole> ers = employeeRoleMapper.selectList(new QueryWrapper<EmployeeRole>().eq("角色编号", roleId));
            for (EmployeeRole er : ers) {
                if (er.getEmployeeId() != null) {
                    try {
                        redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee:" + er.getEmployeeId());
                        redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee_view:" + er.getEmployeeId());
                    } catch (Exception ex) {
                        log.warn("删除员工权限缓存失败 employeeId={}", er.getEmployeeId(), ex);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("按角色刷新员工权限缓存失败 roleId={}", roleId, e);
        }
    }

    @Override
    public List<Integer> getRolePermissions(Integer id) {
        List<RolePermission> rps = rolePermissionMapper.selectList(new QueryWrapper<RolePermission>().eq("角色编号", id));
        return rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getRoleMembers(Integer id) {
        List<EmployeeRole> ers = employeeRoleMapper.selectList(new QueryWrapper<EmployeeRole>().eq("角色编号", id));
        return ers.stream().map(EmployeeRole::getEmployeeId).collect(Collectors.toList());
    }

    @Override
    public List<String> batchGetPermissionCodesByRoleIds(List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> distinctRoleIds = roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> permissionCodes = systemMapper.selectPermissionCodesByRoleIds(distinctRoleIds);
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionCodes.stream()
                .filter(code -> code != null && !code.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 生成唯一的角色编码
     * 角色编码格式：JS[日期][编号]，例如：JS20250101001
     * JS开头 + 年月日日期 + 3位编号（从001开始递增）
     */
    private String generateRoleCode(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "角色名称不能为空");
        }

        // 生成当前日期字符串，格式：yyyyMMdd
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 查询今天已存在的角色编码，找到最大的编号
        String todayPrefix = "JS" + today;
        List<Role> todayRoles = roleMapper.selectList(new QueryWrapper<Role>()
                .likeRight("角色编码", todayPrefix)
                .orderByDesc("角色编码"));

        // 找到今天最大的编号
        int maxNumber = 0;
        for (Role role : todayRoles) {
            String code = role.getRoleCode();
            if (code != null && code.startsWith(todayPrefix) && code.length() == todayPrefix.length() + 3) {
                try {
                    String numberStr = code.substring(todayPrefix.length());
                    int number = Integer.parseInt(numberStr);
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析失败的编码
                }
            }
        }

        // 生成新的编号，从001开始
        int newNumber = maxNumber + 1;
        String numberStr = String.format("%03d", newNumber);

        // 组合最终的编码
        String finalCode = todayPrefix + numberStr;

        // 双重检查确保唯一性（以防并发）
        int maxAttempts = 100;
        int attempts = 0;
        while (roleMapper.selectCount(new QueryWrapper<Role>().eq("角色编码", finalCode)) > 0 && attempts < maxAttempts) {
            newNumber++;
            numberStr = String.format("%03d", newNumber);
            finalCode = todayPrefix + numberStr;
            attempts++;
        }

        if (attempts >= maxAttempts) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "无法生成唯一的角色编码，请稍后重试");
        }

        return finalCode;
    }
}


