package com.erp.service.system;

import java.util.List;
import java.util.Set;

/**
 * 员工权限查询服务接口
 * 用于查询员工拥有的权限，以便确定消息通知的接收者范围
 *
 * @author ERP System
 * @date 2025-04-30
 */
public interface EmployeePermissionService {

    /**
     * 根据权限编码查询有该权限的所有员工ID
     * 包括通过角色继承的权限和员工直接分配的权限
     *
     * @param permissionCode 权限编码（如：合同管理:危险废物合同:页面）
     * @return 有该权限的员工ID列表
     */
    List<Integer> getEmployeeIdsByPermissionCode(String permissionCode);

    /**
     * 根据多个权限编码查询有任一权限的所有员工ID
     * 员工只需要拥有其中一个权限即可收到消息
     *
     * @param permissionCodes 权限编码列表
     * @return 有任一权限的员工ID列表
     */
    List<Integer> getEmployeeIdsByPermissionCodes(List<String> permissionCodes);

    /**
     * 查询指定员工是否拥有某个权限
     *
     * @param employeeId 员工ID
     * @param permissionCode 权限编码
     * @return 是否拥有该权限
     */
    boolean hasPermission(Integer employeeId, String permissionCode);

    /**
     * 获取管理员角色的所有员工ID
     * 管理员会接收所有业务消息
     *
     * @return 管理员角色员工ID列表
     */
    List<Integer> getAdminEmployeeIds();

    /**
     * 查询员工拥有的所有权限编码
     *
     * @param employeeId 员工ID
     * @return 权限编码集合
     */
    Set<String> getEmployeePermissions(Integer employeeId);

    /**
     * 根据业务类型获取消息通知的接收者ID列表
     * 包括：
     * 1. 有对应权限的员工
     * 2. 管理员角色员工（强制接收）
     * 3. 排除消息发送者本人
     *
     * @param businessType 业务类型
     * @param senderId 发送者ID（会被排除，不收到消息）
     * @return 接收者ID列表
     */
    List<Integer> getMessageReceivers(String businessType, Integer senderId);
}
