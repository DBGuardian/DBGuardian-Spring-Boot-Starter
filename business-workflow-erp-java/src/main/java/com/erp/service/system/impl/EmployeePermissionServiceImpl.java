package com.erp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.config.MessagePermissionMapping;
import com.erp.entity.system.Employee;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.system.EmployeePermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 员工权限查询服务实现类
 * 使用 employee_permission 表获取员工直接分配的页面权限
 * 管理员角色通过 Employee.role 字段判断
 *
 * @author ERP System
 * @date 2025-04-30
 */
@Slf4j
@Service
public class EmployeePermissionServiceImpl implements EmployeePermissionService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    /**
     * 权限编码到员工ID列表的缓存
     * Key: 权限编码
     * Value: 拥有该权限的员工ID列表
     */
    private final Map<String, List<Integer>> permissionCache = new ConcurrentHashMap<>();

    /**
     * 管理员角色员工ID列表缓存
     */
    private volatile List<Integer> adminEmployeeCache = Collections.emptyList();

    /**
     * 员工ID到权限编码集合的缓存
     * Key: 员工ID
     * Value: 权限编码集合
     */
    private final Map<Integer, Set<String>> employeePermissionCache = new ConcurrentHashMap<>();

    /**
     * 缓存刷新间隔（毫秒），默认5分钟
     */
    private static final long CACHE_REFRESH_INTERVAL = 5 * 60 * 1000;

    /**
     * 最后刷新时间
     */
    private volatile long lastRefreshTime = 0;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 刷新权限缓存
     */
    private void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRefreshTime > CACHE_REFRESH_INTERVAL) {
            synchronized (this) {
                if (currentTime - lastRefreshTime > CACHE_REFRESH_INTERVAL) {
                    refreshCache();
                }
            }
        }
    }

    /**
     * 刷新所有缓存
     * 权限来源：employee_permission 表（员工直接分配的页面权限，canView=1）
     * 管理员判断：Employee.role == "超级管理员"
     */
    private synchronized void refreshCache() {
        try {
            // 1. 查询所有权限
            List<Permission> allPermissions = permissionMapper.selectList(null);
            Map<Integer, String> permissionIdToCode = allPermissions.stream()
                    .collect(Collectors.toMap(Permission::getPermissionId, Permission::getPermissionCode, (a, b) -> a));

            // 2. 查询所有员工（在岗）
            List<Employee> allEmployees = employeeMapper.selectList(
                    new LambdaQueryWrapper<Employee>().eq(Employee::getEmployeeStatus, "在职")
            );

            // 3. 查询所有员工-页面权限关联（只包含canView=1的记录）
            List<EmployeePermission> allEmployeePermissions = employeePermissionMapper.selectList(null);

            // 构建映射：员工ID -> 有查看权限的页面权限ID列表
            Map<Integer, List<Integer>> employeeIdToPermissionIds = new HashMap<>();
            for (EmployeePermission ep : allEmployeePermissions) {
                if (ep.getEmployeeId() != null && ep.getPagePermissionId() != null) {
                    // 只记录canView=1的权限
                    if (ep.getCanView() != null && ep.getCanView() == 1) {
                        employeeIdToPermissionIds.computeIfAbsent(ep.getEmployeeId(), k -> new ArrayList<>()).add(ep.getPagePermissionId());
                    }
                }
            }

            // 初始化权限-员工映射
            Map<String, List<Integer>> permEmpMap = new HashMap<>();
            for (Permission p : allPermissions) {
                if (p.getPermissionCode() != null) {
                    permEmpMap.put(p.getPermissionCode(), new ArrayList<>());
                }
            }

            Map<Integer, Set<String>> empPermMap = new HashMap<>();
            List<Integer> adminIds = new ArrayList<>();

            // 4. 遍历每个员工，构建权限映射
            for (Employee emp : allEmployees) {
                Set<String> perms = new HashSet<>();

                // 检查是否为管理员角色（通过 Employee.role 字段判断）
                if ("超级管理员".equals(emp.getRole())) {
                    adminIds.add(emp.getEmployeeId());
                }

                // 获取该员工直接分配的页面权限（仅canView=1）
                List<Integer> pagePermissionIds = employeeIdToPermissionIds.getOrDefault(emp.getEmployeeId(), Collections.emptyList());
                for (Integer pagePermId : pagePermissionIds) {
                    String permCode = permissionIdToCode.get(pagePermId);
                    if (StringUtils.hasText(permCode)) {
                        perms.add(permCode);
                        permEmpMap.computeIfAbsent(permCode, k -> new ArrayList<>()).add(emp.getEmployeeId());
                    }
                }

                empPermMap.put(emp.getEmployeeId(), perms);
            }

            // 5. 更新缓存
            permissionCache.clear();
            permissionCache.putAll(permEmpMap);

            employeePermissionCache.clear();
            employeePermissionCache.putAll(empPermMap);

            adminEmployeeCache = adminIds;

            lastRefreshTime = System.currentTimeMillis();
            log.info("员工权限缓存刷新完成: 权限数量={}, 员工数量={}, 管理员数量={}, 员工-权限关联数量={}",
                    allPermissions.size(), allEmployees.size(), adminIds.size(), allEmployeePermissions.size());

        } catch (Exception e) {
            log.error("刷新员工权限缓存失败", e);
        }
    }

    @Override
    public List<Integer> getEmployeeIdsByPermissionCode(String permissionCode) {
        refreshCacheIfNeeded();

        List<Integer> result = permissionCache.get(permissionCode);
        return result != null ? new ArrayList<>(result) : Collections.emptyList();
    }

    @Override
    public List<Integer> getEmployeeIdsByPermissionCodes(List<String> permissionCodes) {
        if (CollectionUtils.isEmpty(permissionCodes)) {
            return Collections.emptyList();
        }

        refreshCacheIfNeeded();

        Set<Integer> resultSet = new HashSet<>();
        for (String code : permissionCodes) {
            List<Integer> employees = permissionCache.get(code);
            if (!CollectionUtils.isEmpty(employees)) {
                resultSet.addAll(employees);
            }
        }

        return new ArrayList<>(resultSet);
    }

    @Override
    public boolean hasPermission(Integer employeeId, String permissionCode) {
        refreshCacheIfNeeded();

        Set<String> permissions = employeePermissionCache.get(employeeId);
        return permissions != null && permissions.contains(permissionCode);
    }

    @Override
    public List<Integer> getAdminEmployeeIds() {
        refreshCacheIfNeeded();
        return new ArrayList<>(adminEmployeeCache);
    }

    @Override
    public Set<String> getEmployeePermissions(Integer employeeId) {
        refreshCacheIfNeeded();

        Set<String> permissions = employeePermissionCache.get(employeeId);
        return permissions != null ? new HashSet<>(permissions) : Collections.emptySet();
    }

    @Override
    public List<Integer> getMessageReceivers(String businessType, Integer senderId) {
        // 1. 获取业务类型对应的权限编码
        List<String> permissionCodes = MessagePermissionMapping.getPermissionCodes(businessType);

        if (CollectionUtils.isEmpty(permissionCodes)) {
            log.warn("未找到业务类型对应的权限配置: businessType={}", businessType);
            return Collections.emptyList();
        }

        // 2. 获取有对应权限的员工ID
        Set<Integer> receivers = new HashSet<>(getEmployeeIdsByPermissionCodes(permissionCodes));

        // 3. 添加管理员（强制接收所有消息）
        receivers.addAll(getAdminEmployeeIds());

        // 4. 排除发送者本人
        if (senderId != null) {
            receivers.remove(senderId);
        }

        return new ArrayList<>(receivers);
    }

    /**
     * 手动刷新缓存（供外部调用，如权限变更时）
     */
    public void forceRefreshCache() {
        refreshCache();
    }
}
