package com.erp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.controller.system.dto.*;
import com.erp.entity.system.Permission;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.system.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import com.erp.common.constant.RedisConstant;
import com.erp.service.system.ILogRecordService;
import com.erp.common.util.SecurityUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;


    @Autowired
    private com.erp.mapper.system.RolePermissionMapper rolePermissionMapper;

    @Autowired
    private com.erp.mapper.system.EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private com.erp.mapper.system.EmployeePermissionMapper employeePermissionMapper;
    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<PermissionResponse> list(Integer typeId, String keyword) {
        Map<String, Object> params = new HashMap<>();
        params.put("typeId", typeId);
        params.put("keyword", keyword);
        return permissionMapper.selectByCondition(params);
    }

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<PermissionResponse> listPage(Integer typeId, String keyword, Integer current, Integer size) {
        Map<String, Object> params = new HashMap<>();
        params.put("typeId", typeId);
        params.put("keyword", keyword);
        long cur = current == null || current <= 0 ? 1L : current.longValue();
        long sz = size == null || size <= 0 ? 10L : size.longValue();
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PermissionResponse> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(cur, sz);
        return permissionMapper.selectPermissionPage(page, params);
    }

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<PermissionResponse> getPermissionPage(PermissionPageRequest request) {
        Map<String, Object> params = new HashMap<>();

        // 权限类型筛选 - 支持单个值或多个值的OR查询
        if (request.getPermissionTypeIds() != null && !request.getPermissionTypeIds().isEmpty()) {
            // 优先使用 permissionTypeIds 进行OR查询
            params.put("permissionTypeIds", request.getPermissionTypeIds());
        } else if (request.getPermissionTypeId() != null) {
            // 向后兼容：使用单个 permissionTypeId
            params.put("permissionTypeId", request.getPermissionTypeId());
        }

        // 页面模式精确匹配（仅对页面权限生效，SQL 中直接按列等值匹配）
        params.put("pageMode", request.getPageMode());

        // 权限名称模糊匹配
        params.put("permissionName", request.getPermissionName());

        // 权限描述模糊匹配
        params.put("permissionDescription", request.getPermissionDescription());

        // 父权限名称模糊匹配
        params.put("parentPermissionName", request.getParentPermissionName());

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PermissionResponse> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(request.getCurrent(), request.getSize());
        return permissionMapper.selectPermissionPage(page, params);
    }

    @Override
    public List<PermissionTreeNode> tree() {
        List<Permission> all = permissionMapper.selectList(new QueryWrapper<>());
        Map<Integer, PermissionTreeNode> nodeMap = all.stream().map(p -> {
            PermissionTreeNode n = new PermissionTreeNode();
            n.setPermissionId(p.getPermissionId());
            n.setPermissionName(p.getPermissionName());
            n.setPermissionCode(p.getPermissionCode());
            n.setPermissionTypeId(p.getPermissionTypeId());
            n.setPageMode(p.getPageMode());
            n.setChildren(new ArrayList<>());
            return n;
        }).collect(Collectors.toMap(PermissionTreeNode::getPermissionId, n -> n));
        List<PermissionTreeNode> roots = new ArrayList<>();
        for (Permission p : all) {
            PermissionTreeNode node = nodeMap.get(p.getPermissionId());
            if (p.getParentPermissionId() == null) roots.add(node);
            else {
                PermissionTreeNode parent = nodeMap.get(p.getParentPermissionId());
                if (parent != null) parent.getChildren().add(node);
                else roots.add(node);
            }
        }
        return roots;
    }

    @Override
    public PermissionResponse get(Integer id) {
        Permission p = permissionMapper.selectById(id);
        if (p == null) return null;

        String parentPermissionName = null;
        if (p.getParentPermissionId() != null) {
            Permission parent = permissionMapper.selectById(p.getParentPermissionId());
            if (parent != null) {
                parentPermissionName = parent.getPermissionName();
            }
        }

        return PermissionResponse.builder()
                .permissionId(p.getPermissionId())
                .permissionName(p.getPermissionName())
                .permissionDescription(p.getPermissionDescription())
                .permissionTypeId(p.getPermissionTypeId())
                .permissionCode(p.getPermissionCode())
                .pageMode(p.getPageMode())
                .parentPermissionId(p.getParentPermissionId())
                .parentPermissionName(parentPermissionName)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(PermissionRequest req) {
        // 生成唯一的权限编码
        String permissionCode = generatePermissionCode(req.getPermissionName(), req.getPermissionTypeId(), req.getParentPermissionId());

        Permission p = Permission.builder()
                .permissionName(req.getPermissionName())
                .permissionDescription(req.getPermissionDescription())
                .permissionTypeId(req.getPermissionTypeId())
                .permissionCode(permissionCode)
                .parentPermissionId(req.getParentPermissionId())
                .createTime(java.time.LocalDateTime.now()) // 设置创建时间为当前时间
                .build();
        permissionMapper.insert(p);
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("权限管理", "新增权限", "permissionId=" + p.getPermissionId() + ", code=" + permissionCode, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录新增权限操作日志失败", e);
        }
        return p.getPermissionId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, PermissionRequest req) {
        Permission p = permissionMapper.selectById(id);
        if (p == null) throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "权限不存在");

        // 检查是否需要重新生成权限编码（当权限类型或父权限发生变化时）
        String newPermissionCode = p.getPermissionCode();
        if (!Objects.equals(p.getPermissionTypeId(), req.getPermissionTypeId()) ||
            !Objects.equals(p.getParentPermissionId(), req.getParentPermissionId())) {
            newPermissionCode = generatePermissionCode(req.getPermissionName(), req.getPermissionTypeId(), req.getParentPermissionId());
        }

        p.setPermissionName(req.getPermissionName());
        p.setPermissionDescription(req.getPermissionDescription());
        p.setPermissionTypeId(req.getPermissionTypeId());
        p.setPermissionCode(newPermissionCode);
        p.setParentPermissionId(req.getParentPermissionId());
        p.setUpdateTime(java.time.LocalDateTime.now()); // 设置更新时间为当前时间
        int rows = permissionMapper.updateById(p);
        if (rows == 0) {
            throw new BusinessException("更新权限失败：记录已被其他用户修改");
        }
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("权限管理", "更新权限", "permissionId=" + id + ", code=" + p.getPermissionCode(), userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录更新权限操作日志失败", e);
        }
        // 精确刷新：清理使用到该权限的角色与员工缓存
        invalidateCachesByPermissionId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Permission p = permissionMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "权限不存在");
        }

        // 检查权限是否被角色使用
        List<Integer> roleIds = permissionMapper.selectRoleIdsByPermissionId(id);
        List<Integer> employeeIds = permissionMapper.selectEmployeeIdsByPermissionId(id);

        if (!roleIds.isEmpty() || !employeeIds.isEmpty()) {
            String message = "该权限正在被使用，无法删除。";
            if (!roleIds.isEmpty()) {
                message += " 被 " + roleIds.size() + " 个角色使用";
            }
            if (!employeeIds.isEmpty()) {
                message += " 被 " + employeeIds.size() + " 个员工直接使用";
            }
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), message);
        }

        // 删除父权限时，先清空子权限的父权限编号（只清空子权限一层）
        permissionMapper.clearChildParentPermissionIds(id);

        // 删除权限记录
        int delRows = permissionMapper.deleteById(id);
        if (delRows == 0) {
            throw new BusinessException("删除权限失败：记录不存在或已被删除");
        }

        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("权限管理", "删除权限", "permissionId=" + id, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录删除权限操作日志失败", e);
        }

        // 精确刷新：清理使用到该权限的角色与员工缓存
        invalidateCachesByPermissionId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 1. 批量查询所有权限（一次查询）
        List<Permission> permissions = permissionMapper.selectBatchIds(ids);
        if (permissions == null || permissions.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "权限不存在");
        }

        // 检查是否有权限被使用
        for (Permission p : permissions) {
            List<Integer> roleIds = permissionMapper.selectRoleIdsByPermissionId(p.getPermissionId());
            List<Integer> employeeIds = permissionMapper.selectEmployeeIdsByPermissionId(p.getPermissionId());

            if (!roleIds.isEmpty() || !employeeIds.isEmpty()) {
                String message = "权限 [" + p.getPermissionName() + "] 正在被使用，无法删除。";
                if (!roleIds.isEmpty()) {
                    message += " 被 " + roleIds.size() + " 个角色使用";
                }
                if (!employeeIds.isEmpty()) {
                    message += " 被 " + employeeIds.size() + " 个员工直接使用";
                }
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), message);
            }
        }

        // 3. 批量删除父权限时，清空子权限的父权限编号
        for (Integer id : ids) {
            permissionMapper.clearChildParentPermissionIds(id);
        }

        // 4. 批量删除权限（一次SQL执行）
        int deleteCount = permissionMapper.deleteBatchIds(ids);
        log.info("批量删除权限成功：请求数量={}，实际删除={}", ids.size(), deleteCount);

        // 5. 记录操作日志
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("权限管理", "批量删除权限",
                    "count=" + ids.size() + " ids=" + ids, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录批量删除权限操作日志失败", e);
        }

        // 6. 清理相关缓存
        for (Integer id : ids) {
            invalidateCachesByPermissionId(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Integer> addPagesToModule(Integer moduleId, List<PermissionRequest> pages) {
        List<Integer> createdIds = new ArrayList<>();
        for (PermissionRequest pr : pages) {
            // 生成唯一的权限编码
            String permissionCode = generatePermissionCode(pr.getPermissionName(), pr.getPermissionTypeId(), moduleId);

            Permission p = Permission.builder()
                    .permissionName(pr.getPermissionName())
                    .permissionDescription(pr.getPermissionDescription())
                    .permissionTypeId(pr.getPermissionTypeId())
                    .permissionCode(permissionCode)
                    .parentPermissionId(moduleId)
                    .createTime(java.time.LocalDateTime.now()) // 设置创建时间为当前时间
                    .build();
            permissionMapper.insert(p);
            createdIds.add(p.getPermissionId());
        }
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("权限管理", "批量新增页面到模块", "moduleId=" + moduleId, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录批量新增页面到模块操作日志失败", e);
        }
        try {
            java.util.Set<String> keys = redisTemplate.keys(RedisConstant.PERMISSION_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("刷新权限缓存失败", e);
        }
        return createdIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> removePageFromModule(Integer moduleId, Integer pageId, boolean confirm) {
        Permission p = permissionMapper.selectById(pageId);
        if (p == null) throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "页面不存在");
        if (p.getParentPermissionId() == null || !p.getParentPermissionId().equals(moduleId))
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "页面不属于指定模块");

        // 计算受影响的角色和员工数量
        List<Integer> affectedRoleIds = permissionMapper.selectRoleIdsByPermissionId(pageId);
        List<Integer> affectedEmployeeIds = permissionMapper.selectEmployeeIdsByPermissionId(pageId);

        // 如果有角色使用该页面权限，需要统计这些角色的员工数量
        int totalAffectedEmployees = affectedEmployeeIds.size();
        if (!affectedRoleIds.isEmpty()) {
            // 对于每个受影响的角色，统计其成员数量
            for (Integer roleId : affectedRoleIds) {
                List<com.erp.entity.system.EmployeeRole> employeeRoles = employeeRoleMapper.selectList(
                    new QueryWrapper<com.erp.entity.system.EmployeeRole>().eq("角色编号", roleId)
                );
                totalAffectedEmployees += employeeRoles.size();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("affectedRoleCount", affectedRoleIds.size());
        result.put("affectedEmployeeCount", totalAffectedEmployees);
        result.put("affectedRoleIds", affectedRoleIds);
        result.put("affectedEmployeeIds", affectedEmployeeIds);

        // 如果未确认操作，只返回统计信息
        if (!confirm) {
            result.put("confirmed", false);
            return result;
        }

        // 执行移除操作
        p.setParentPermissionId(null);
        p.setUpdateTime(java.time.LocalDateTime.now()); // 设置更新时间为当前时间
        int rows = permissionMapper.updateById(p);
        if (rows == 0) {
            log.warn("移除页面权限失败（乐观锁冲突），pageId={}", pageId);
        }

        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("权限管理", "从模块移除页面", "moduleId=" + moduleId + ", pageId=" + pageId, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录从模块移除页面操作日志失败", e);
        }

        // 精确刷新：清理使用到该页面权限的角色与员工缓存
        invalidateCachesByPermissionId(pageId);

        result.put("confirmed", true);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateModulePageAssociations(Integer moduleId, List<String> addedIds, List<String> removedIds) {
        // 验证模块存在且为模块类型
        Permission module = permissionMapper.selectById(moduleId);
        if (module == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "模块不存在");
        }
        if (!Objects.equals(module.getPermissionTypeId(), 1)) { // 1=模块权限
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "指定的权限不是模块类型");
        }

        // 处理新增关联：设置页面的 parentPermissionId 为模块ID
        if (!addedIds.isEmpty()) {
            List<Integer> pageIds = addedIds.stream()
                .map(id -> {
                    try {
                        return Integer.parseInt(id);
                    } catch (NumberFormatException e) {
                        throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "无效的页面ID: " + id);
                    }
                })
                .collect(Collectors.toList());

            // 批量验证页面权限存在且为页面类型
            List<Permission> pages = permissionMapper.selectBatchIds(pageIds);
            if (pages.size() != pageIds.size()) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "部分页面权限不存在");
            }
            for (Permission p : pages) {
                if (!Objects.equals(p.getPermissionTypeId(), 2)) { // 2=页面权限
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "权限 " + p.getPermissionId() + " 不是页面类型");
                }
            }

            // 批量更新 parentPermissionId
            permissionMapper.batchUpdateParentPermissionId(pageIds, moduleId);
        }

        // 处理删除关联：清除页面的 parentPermissionId
        if (!removedIds.isEmpty()) {
            List<Integer> pageIds = removedIds.stream()
                .map(id -> {
                    try {
                        return Integer.parseInt(id);
                    } catch (NumberFormatException e) {
                        throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "无效的页面ID: " + id);
                    }
                })
                .collect(Collectors.toList());

            // 验证这些页面确实属于该模块
            List<Permission> pages = permissionMapper.selectBatchIds(pageIds);
            for (Permission p : pages) {
                if (!Objects.equals(p.getParentPermissionId(), moduleId)) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(),
                        "页面 " + p.getPermissionId() + " 不属于模块 " + moduleId);
                }
            }

            // 批量清除 parentPermissionId
            permissionMapper.batchUpdateParentPermissionId(pageIds, null);
        }

        // 记录操作日志
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("权限管理", "批量更新模块页面关联",
                "moduleId=" + moduleId + " added=" + addedIds.size() + " removed=" + removedIds.size(),
                userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录批量更新模块页面关联操作日志失败", e);
        }

        // 刷新相关缓存
        try {
            Set<String> keys = redisTemplate.keys(RedisConstant.PERMISSION_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("刷新权限缓存失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdatePageFieldAssociations(Integer pageId, List<String> addedIds, List<String> removedIds) {
        log.info("batchUpdatePageFieldAssociations 开始执行: pageId={}, addedIds={}, removedIds={}", pageId, addedIds, removedIds);

        // 验证页面存在且为页面类型
        Permission page = permissionMapper.selectById(pageId);
        if (page == null) {
            log.error("页面不存在: pageId={}", pageId);
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "页面不存在");
        }
        if (!Objects.equals(page.getPermissionTypeId(), 2)) { // 2=页面权限
            log.error("指定的权限不是页面类型: pageId={}, type={}", pageId, page.getPermissionTypeId());
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "指定的权限不是页面类型");
        }
        log.info("页面验证通过: {} (ID: {}, 编码: {})", page.getPermissionName(), page.getPermissionId(), page.getPermissionCode());

        // 处理新增关联
        if (!addedIds.isEmpty()) {
            for (String key : addedIds) {
                if (key == null || key.trim().isEmpty()) continue;

                Permission childPermission = null;
                String fieldName = null;

                // 尝试作为permissionId查找（动作权限使用ID）
                try {
                    Integer permissionId = Integer.parseInt(key);
                    childPermission = permissionMapper.selectById(permissionId);
                    if (childPermission != null) {
                        fieldName = childPermission.getPermissionCode();
                    }
                } catch (NumberFormatException e) {
                    // 如果不是数字，尝试作为permissionCode查找（字段权限使用编码）
                    List<Permission> permissions = permissionMapper.selectList(
                        new QueryWrapper<Permission>().eq("权限编码", key)
                    );
                    if (!permissions.isEmpty()) {
                        childPermission = permissions.get(0);
                        fieldName = key;
                    }
                }

                if (childPermission == null) {
                    log.warn("未找到权限: {}", key);
                    continue;
                }

                // 验证权限类型
                if (!Objects.equals(childPermission.getPermissionTypeId(), 3) &&
                    !Objects.equals(childPermission.getPermissionTypeId(), 4)) {
                    log.warn("权限 {} 不是动作或字段类型", childPermission.getPermissionId());
                    continue;
                }

                // 检查权限是否已经被关联到其他页面
                if (childPermission.getParentPermissionId() != null &&
                    !Objects.equals(childPermission.getParentPermissionId(), pageId)) {
                    log.warn("权限 {} 已被关联到其他页面 {}，将重新关联", key, childPermission.getParentPermissionId());
                }

                // 更新权限的父权限编号
                try {
                    log.info("设置权限 {} 的父权限编号为 {}", key, pageId);
                    int updateResult = permissionMapper.updateParentPermissionId(childPermission.getPermissionId(), pageId);
                    log.info("updateParentPermissionId 返回结果: {}", updateResult);

                    if (updateResult == 0) {
                        log.error("更新权限 {} 失败", key);
                    } else {
                        log.info("权限 {} 父权限编号设置成功", key);
                    }
                } catch (Exception e) {
                    log.error("更新权限 {} 失败: {}", key, e.getMessage(), e);
                    throw e; // 重新抛出异常
                }
                // 动作权限只需要父子关系，无需额外处理
            }
        }

        // 处理删除关联
        if (!removedIds.isEmpty()) {
            log.info("开始处理删除关联，removedIds: {}", removedIds);
            for (String key : removedIds) {
                log.info("处理删除权限: {}", key);
                if (key == null || key.trim().isEmpty()) {
                    log.warn("权限key为空，跳过");
                    continue;
                }

                Permission childPermission = null;
                String fieldName = null;

                // 尝试作为permissionId查找（动作权限使用ID）
                try {
                    Integer permissionId = Integer.parseInt(key);
                    log.info("尝试作为permissionId查找: {}", permissionId);
                    childPermission = permissionMapper.selectById(permissionId);
                    if (childPermission != null) {
                        fieldName = childPermission.getPermissionCode();
                        log.info("通过permissionId找到权限: {} (编码: {})", childPermission.getPermissionId(), fieldName);
                    } else {
                        log.warn("通过permissionId未找到权限: {}", permissionId);
                    }
                } catch (NumberFormatException e) {
                    log.info("不是数字，尝试作为permissionCode查找: {}", key);
                    // 如果不是数字，尝试作为permissionCode查找（字段权限使用编码）
                    List<Permission> permissions = permissionMapper.selectList(
                        new QueryWrapper<Permission>().eq("权限编码", key)
                    );
                    if (!permissions.isEmpty()) {
                        childPermission = permissions.get(0);
                        fieldName = key;
                        log.info("通过permissionCode找到权限: {} (ID: {})", fieldName, childPermission.getPermissionId());
                    } else {
                        log.warn("通过permissionCode未找到权限: {}", key);
                    }
                }

                if (childPermission == null) {
                    log.error("未找到权限: {}，跳过删除操作", key);
                    continue;
                }

                // 验证权限类型
                if (!Objects.equals(childPermission.getPermissionTypeId(), 3) &&
                    !Objects.equals(childPermission.getPermissionTypeId(), 4)) {
                    log.warn("权限 {} 不是动作或字段类型: {}", key, childPermission.getPermissionTypeId());
                    continue;
                }

                // 验证权限确实属于当前页面
                if (!Objects.equals(childPermission.getParentPermissionId(), pageId)) {
                    log.warn("权限 {} 不属于页面 {}，当前父权限: {}，但仍将强制清除", key, pageId, childPermission.getParentPermissionId());
                    // 注意：即使不属于当前页面，如果前端要求删除，我们也应该清除其父权限编号
                    // 这可能是因为数据状态不一致或前端状态与后端不匹配
                }

                // 清除权限的父权限编号
                try {
                    log.info("清除权限 {} 的父权限编号，更新前值: {}", key, childPermission.getParentPermissionId());

                    // 使用直接SQL更新，避免MyBatis-Plus的缓存或乐观锁问题
                    int updateResult = permissionMapper.updateParentPermissionId(childPermission.getPermissionId(), null);
                    log.info("updateParentPermissionId 返回结果: {}", updateResult);

                    if (updateResult == 0) {
                        log.error("updateParentPermissionId 返回0，权限 {} 更新失败", key);
                    } else {
                        log.info("权限 {} 父权限编号清除成功", key);
                        // 重新查询确认更新结果
                        Permission checkPermission = permissionMapper.selectById(childPermission.getPermissionId());
                        log.info("重新查询权限 {} 的父权限编号: {}", key, checkPermission != null ? checkPermission.getParentPermissionId() : "权限不存在");
                    }
                } catch (Exception e) {
                    log.error("清除权限 {} 的父权限编号失败: {}", key, e.getMessage(), e);
                    throw e; // 重新抛出异常
                }
                // 动作权限的父子关系已清除，无需额外处理
            }
        }

        // 记录操作日志
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            logRecordService.recordOperationLog("权限管理", "批量更新页面字段关联",
                "pageId=" + pageId + " added=" + addedIds.size() + " removed=" + removedIds.size(),
                userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录批量更新页面字段关联操作日志失败", e);
        }

        // 刷新相关缓存
        try {
            Set<String> keys = redisTemplate.keys(RedisConstant.PERMISSION_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("刷新权限缓存失败", e);
        }
    }

    /**
     * 根据权限编号（页面/动作）刷新相关缓存：包含直接分配了该权限的角色以及这些角色下的员工缓存，
     * 以及对该权限有显式员工覆盖的员工缓存。
     */
    private void invalidateCachesByPermissionId(Integer permissionId) {
        try {
            // 1) 找到所有包含该权限的角色
            List<com.erp.entity.system.RolePermission> rps = rolePermissionMapper.selectList(new QueryWrapper<com.erp.entity.system.RolePermission>().eq("权限编号", permissionId));
            Set<Integer> roleIds = rps.stream().map(com.erp.entity.system.RolePermission::getRoleId).collect(Collectors.toSet());

            // 2) 删除角色级缓存 key: permission:role:{roleId}
            for (Integer rid : roleIds) {
                try {
                    redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "role:" + rid);
                } catch (Exception e) {
                    log.warn("删除角色权限缓存失败 roleId={}", rid, e);
                }
                // 删除该角色下的员工缓存
                List<com.erp.entity.system.EmployeeRole> ers = employeeRoleMapper.selectList(new QueryWrapper<com.erp.entity.system.EmployeeRole>().eq("角色编号", rid));
                for (com.erp.entity.system.EmployeeRole er : ers) {
                    try {
                        if (er.getEmployeeId() != null) {
                            redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee:" + er.getEmployeeId());
                            redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee_view:" + er.getEmployeeId());
                        }
                    } catch (Exception ex) {
                        log.warn("删除员工权限缓存失败 employeeId={}", er.getEmployeeId(), ex);
                    }
                }
            }

            // 3) 对有显式员工覆盖的员工也需要清理（员工级覆写）
            List<com.erp.entity.system.EmployeePermission> eps = employeePermissionMapper.selectList(new QueryWrapper<com.erp.entity.system.EmployeePermission>().eq("权限编号", permissionId));
            for (com.erp.entity.system.EmployeePermission ep : eps) {
                try {
                    if (ep.getEmployeeId() != null) {
                        redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee:" + ep.getEmployeeId());
                        redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee_view:" + ep.getEmployeeId());
                    }
                } catch (Exception ex) {
                    log.warn("删除员工权限缓存失败 employeeId={}", ep.getEmployeeId(), ex);
                }
            }
        } catch (Exception e) {
            log.warn("按权限ID刷新缓存失败 permissionId={}", permissionId, e);
        }
    }

    @Override
    public String generatePermissionCode(String permissionName, Integer permissionTypeId, Integer parentPermissionId) {
        if (permissionName == null || permissionName.trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "权限名称不能为空");
        }

        // 权限编码格式：module:page:action，根据权限类型和父权限关系生成
        String baseCode = permissionName.trim().toLowerCase().replace(" ", "_").replace("-", "_");

        // 根据权限类型生成编码
        if (permissionTypeId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "权限类型不能为空");
        }

        // 基于权限类型ID进行判断（根据数据库中常见的权限类型）
        // 1: MODULE, 2: PAGE, 3: ACTION, 4: FIELD
        String generatedCode;

        if (permissionTypeId == 1) { // MODULE
            // 模块级权限：直接使用权限名称的小写形式
            generatedCode = baseCode;
        } else if (permissionTypeId == 2) { // PAGE
            // 页面级权限：父权限编码:权限名称:page
            if (parentPermissionId == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "页面权限必须指定父权限（模块）");
            }
            Permission parentPermission = permissionMapper.selectById(parentPermissionId);
            if (parentPermission == null || parentPermission.getPermissionCode() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "父权限不存在或权限编码为空");
            }
            generatedCode = parentPermission.getPermissionCode() + ":" + baseCode + ":page";
        } else if (permissionTypeId == 3) { // ACTION
            // 动作级权限：父权限编码:权限名称
            if (parentPermissionId == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "动作权限必须指定父权限（页面）");
            }
            Permission parentPermission = permissionMapper.selectById(parentPermissionId);
            if (parentPermission == null || parentPermission.getPermissionCode() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "父权限不存在或权限编码为空");
            }
            generatedCode = parentPermission.getPermissionCode() + ":" + baseCode;
        } else if (permissionTypeId == 4) { // FIELD
            // 字段级权限：父权限编码:权限名称
            if (parentPermissionId == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "字段权限必须指定父权限（页面或动作）");
            }
            Permission parentPermission = permissionMapper.selectById(parentPermissionId);
            if (parentPermission == null || parentPermission.getPermissionCode() == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "父权限不存在或权限编码为空");
            }
            generatedCode = parentPermission.getPermissionCode() + ":" + baseCode;
        } else {
            // 其他类型：使用通用格式
            if (parentPermissionId != null) {
                Permission parentPermission = permissionMapper.selectById(parentPermissionId);
                if (parentPermission != null && parentPermission.getPermissionCode() != null) {
                    generatedCode = parentPermission.getPermissionCode() + ":" + baseCode;
                } else {
                    generatedCode = baseCode;
                }
            } else {
                generatedCode = baseCode;
            }
        }

        // 检查权限编码是否已存在，如果存在则添加数字后缀
        String finalCode = generatedCode;
        int counter = 1;
        while (permissionMapper.selectCount(new QueryWrapper<Permission>().eq("权限编码", finalCode)) > 0) {
            finalCode = generatedCode + "_" + counter;
            counter++;
            // 防止无限循环，最多尝试100次
            if (counter > 100) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "无法生成唯一的权限编码，请检查权限名称");
            }
        }

        return finalCode;
    }
}


