package com.erp.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.system.dto.EmployeeCreateRequest;
import com.erp.controller.system.dto.EmployeeCreateResponse;
import com.erp.controller.system.dto.EmployeePageRequest;
import com.erp.controller.system.dto.EmployeePageResponse;
import com.erp.controller.system.dto.EmployeeRegisterRequest;
import com.erp.controller.system.dto.EmployeeRegisterResponse;
import com.erp.controller.system.dto.EmployeeRegistrationPageRequest;
import com.erp.controller.system.dto.EmployeeRegistrationPageResponse;
import com.erp.controller.system.dto.EmployeeUpdateRequest;
import com.erp.controller.system.dto.ResetPasswordRequest;
import com.erp.controller.system.dto.RegistrationApproveRequest;
import com.erp.controller.system.dto.RegistrationApproveResponse;
import com.erp.controller.system.dto.EmployeeRoleAssignResponse;
import com.erp.controller.system.dto.EmployeePermissionAssignRequest;
import com.erp.controller.system.dto.EmployeePermissionAssignResponse;
import com.erp.entity.system.Employee;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统管理服务接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
public interface SystemService {

    /**
     * 员工注册
     *
     * @param request 注册请求
     * @return 注册响应
     */
    EmployeeRegisterResponse registerEmployee(EmployeeRegisterRequest request);

    /**
     * 分页查询员工注册信息
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<EmployeeRegistrationPageResponse> getEmployeeRegistrationPage(EmployeeRegistrationPageRequest request);

    /**
     * 分页查询员工列表
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<EmployeePageResponse> getEmployeePage(EmployeePageRequest request);

    /**
     * 根据员工ID获取员工详情
     *
     * @param employeeId 员工ID
     * @return 员工实体
     */
    Employee getEmployeeById(Integer employeeId);

    /**
     * 获取员工下拉列表（支持关键词搜索）
     *
     * @param keyword 关键词（可选）
     * @return 员工下拉列表
     */
    java.util.List<java.util.Map<String, Object>> getEmployeeSelectList(String keyword);

    /**
     * 直接新增员工（支持上传身份证图片）
     *
     * @param request 员工创建请求
     * @param idCardFront 身份证正面照片（选填）
     * @param idCardBack 身份证反面照片（选填）
     * @return 创建结果
     */
    EmployeeCreateResponse createEmployee(EmployeeCreateRequest request,
                                          MultipartFile idCardFront, MultipartFile idCardBack);

    /**
     * 编辑员工信息
     *
     * @param employeeId 员工ID
     * @param request    更新请求
     * @param idCardFront 身份证正面照片（选填）
     * @param idCardBack 身份证反面照片（选填）
     * @param deleteIdCardFront 是否删除身份证正面照片
     * @param deleteIdCardBack 是否删除身份证反面照片
     */
    void updateEmployee(Integer employeeId, EmployeeUpdateRequest request,
                        MultipartFile idCardFront, MultipartFile idCardBack,
                        Boolean deleteIdCardFront, Boolean deleteIdCardBack);

    /**
     * 重置员工密码
     *
     * @param employeeId 员工ID
     * @param request    重置密码请求
     */
    void resetEmployeePassword(Integer employeeId, ResetPasswordRequest request);

    /**
     * 审核通过员工注册
     *
     * @param registrationId 注册编号
     * @param request 修改后的员工注册信息（可选）
     * @return 审核结果（包含新创建的员工ID）
     */
    RegistrationApproveResponse approveRegistration(Integer registrationId, RegistrationApproveRequest request);

    /**
     * 审核通过员工注册（支持上传/删除身份证图片）
     *
     * @param registrationId 注册编号
     * @param request 修改后的员工注册信息（可选）
     * @param idCardFront 身份证正面照片（可选）
     * @param idCardBack 身份证反面照片（可选）
     * @param deleteIdCardFront 是否删除身份证正面照片
     * @param deleteIdCardBack 是否删除身份证反面照片
     * @return 审核结果（包含新创建的员工ID）
     */
    RegistrationApproveResponse approveRegistration(Integer registrationId, RegistrationApproveRequest request,
                                                   MultipartFile idCardFront, MultipartFile idCardBack,
                                                   Boolean deleteIdCardFront, Boolean deleteIdCardBack);

    /**
     * 驳回员工注册
     *
     * @param registrationId 注册编号
     */
    void rejectRegistration(Integer registrationId);

    /**
     * 获取员工最终权限视图（含员工层覆盖）
     *
     * @param employeeId 员工ID
     * @return map 包含 permissionCodes
     */
    java.util.Map<String, Object> getEmployeePermissions(Integer employeeId);

    /**
     * 设置员工显式权限覆盖（ALLOW/DENY）
     *
     * @param employeeId 员工ID
     * @param perms      员工权限 DTO 列表
     */
    void setEmployeePermissions(Integer employeeId, java.util.List<com.erp.controller.system.dto.EmployeePermissionDto> perms);

    /**
     * 获取员工已分配的角色列表
     *
     * @param employeeId 员工ID
     * @return 角色ID列表
     */
    java.util.List<Integer> getEmployeeRoles(Integer employeeId);

    /**
     * 分配员工角色（差分保存）
     *
     * @param employeeId 员工ID
     * @param roleIds    角色ID列表
     * @return 分配结果（包含新增、移除、保持不变的数量）
     */
    EmployeeRoleAssignResponse assignEmployeeRoles(Integer employeeId, java.util.List<Integer> roleIds);

    /**
     * 分配员工页面权限（差量保存）
     * 
     * @param employeeId 员工ID
     * @param request    权限分配请求
     * @return 分配结果（包含新增、更新、删除、保持不变的数量）
     */
    EmployeePermissionAssignResponse assignEmployeePagePermissions(Integer employeeId, EmployeePermissionAssignRequest request);

    /**
     * 获取员工页面权限列表
     * 
     * @param employeeId 员工ID
     * @return 员工页面权限详情列表
     */
    java.util.List<com.erp.controller.system.dto.EmployeePagePermissionDetail> getEmployeePagePermissions(Integer employeeId);
}





