package com.erp.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.system.dto.EmailChannelConfigResponse;
import com.erp.controller.system.dto.EmailChannelConfigSaveRequest;
import com.erp.controller.system.dto.EmailChannelTestSendRequest;
import com.erp.controller.system.dto.EmailChannelTestSendResponse;
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
import com.erp.common.util.SecurityUtil;
import com.erp.entity.system.SysConfig;
import com.erp.controller.system.dto.SysConfigSaveRequest;
import com.erp.service.system.SysConfigService;
import com.erp.service.system.SystemService;
import com.erp.service.system.EmailChannelService;
import com.erp.service.system.ILogRecordService;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.erp.controller.system.dto.EmployeePermissionDto;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理控制器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/system")
@Api(tags = "系统管理")
public class SystemController {

    @Autowired
    private SystemService systemService;

    @Autowired
    private EmailChannelService emailChannelService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private SysConfigService sysConfigService;

    // All permission/role operations delegated to SystemService

    /**
     * 员工注册
     *
     * @param request 注册请求（使用@ModelAttribute接收multipart/form-data格式，包含文件上传）
     * @return 注册响应
     */
    @PostMapping("/employee/register")
    @ApiOperation(value = "员工注册", notes = "员工提交注册信息（包含身份证照片），等待管理员审核。支持multipart/form-data格式")
    public Result<EmployeeRegisterResponse> registerEmployee(@Valid @ModelAttribute EmployeeRegisterRequest request,
                                                               HttpServletRequest httpRequest) {
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("收到员工注册请求：employeeName={}, phone={}", request.getEmployeeName(), request.getPhone());
            EmployeeRegisterResponse response = systemService.registerEmployee(request);
            log.info("员工注册成功：registrationId={}", response.getEmployeeId());
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("系统管理", "新增", 
                    "员工注册：" + request.getEmployeeName(), null, ipAddress, true, null);
            return Result.success("注册成功，等待管理员审核", response);
        } catch (com.erp.common.exception.BusinessException e) {
            log.warn("员工注册业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "新增", 
                    "员工注册：" + request.getEmployeeName(), null, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("员工注册失败", e);
            logRecordService.recordOperationLog("系统管理", "新增", 
                    "员工注册：" + request.getEmployeeName(), null, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "注册失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询员工注册信息
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @RequirePagePermission("系统管理:注册管理:页面")
    @GetMapping("/employee/registrations")
    @ApiOperation(value = "分页查询员工注册信息", notes = "从EMPLOYEE_REGISTRATION表读取数据，支持按员工姓名、部门、岗位、手机号码、登录账号、审核状态、权限分配状态进行筛选查询")
    public Result<IPage<EmployeeRegistrationPageResponse>> getEmployeeRegistrationPage(@Valid EmployeeRegistrationPageRequest request) {
        try {
            log.info("分页查询员工注册信息：current={}, size={}, employeeName={}, auditStatus={}",
                    request.getCurrent(), request.getSize(), request.getEmployeeName(), request.getAuditStatus());
            IPage<EmployeeRegistrationPageResponse> page = systemService.getEmployeeRegistrationPage(request);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("分页查询员工注册信息失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询正式员工列表（从EMPLOYEE表读取）
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @RequirePagePermission({
            "人事管理:员工档案:页面",
            "系统管理:员工管理:页面",
            "档案管理:客户档案:页面",
            "合同管理:危险废物合同:页面",
            "合同管理:合同订立:页面"
    })
    @GetMapping("/employee/list")
    @ApiOperation(value = "分页查询正式员工列表", notes = "从EMPLOYEE表读取数据，支持按员工姓名、部门、岗位、手机号码、登录账号、员工状态进行筛选查询")
    public Result<IPage<EmployeePageResponse>> getEmployeePage(@Valid EmployeePageRequest request) {
        try {
            log.info("分页查询员工列表：current={}, size={}, employeeName={}, employeeStatus={}",
                    request.getCurrent(), request.getSize(), request.getEmployeeName(), request.getEmployeeStatus());
            IPage<EmployeePageResponse> page = systemService.getEmployeePage(request);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("分页查询员工列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取员工下拉列表（支持关键词搜索）
     * GET /api/system/employee/select
     */
    @GetMapping("/employee/select")
    @ApiOperation(value = "获取员工下拉列表", notes = "获取员工下拉选择列表，支持关键词模糊搜索")
    public Result<java.util.List<java.util.Map<String, Object>>> getEmployeeSelect(@RequestParam(required = false) String keyword) {
        try {
            log.info("获取员工下拉列表：keyword={}", keyword);
            java.util.List<java.util.Map<String, Object>> list = systemService.getEmployeeSelectList(keyword);
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("获取员工下拉列表失败 keyword={}", keyword, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取员工详情
     * GET /api/system/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    @ApiOperation(value = "获取员工详情", notes = "根据员工ID获取员工详细信息")
    public Result<Employee> getEmployeeDetail(@PathVariable Integer employeeId) {
        try {
            log.info("获取员工详情：employeeId={}", employeeId);
            Employee employee = systemService.getEmployeeById(employeeId);
            if (employee == null) {
                return Result.error(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
            }
            return Result.success("查询成功", employee);
        } catch (Exception e) {
            log.error("获取员工详情失败 employeeId={}", employeeId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 返回员工最终权限视图（含员工层 ALLOW/DENY 覆盖与合并后字段可见性）
     * GET /api/system/employees/{employeeId}/permissions
     */
    @RequirePagePermission("人事管理:权限分配:页面")
    @GetMapping("/employees/{employeeId}/permissions")
    @ApiOperation("获取员工最终权限视图（含员工层覆盖）")
    public Result<java.util.Map<String, Object>> getEmployeePermissions(@PathVariable Integer employeeId) {
        try {
            return Result.success("查询成功", systemService.getEmployeePermissions(employeeId));
        } catch (Exception e) {
            log.error("获取员工权限失败 employeeId={}", employeeId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 设置员工显式权限覆盖（ALLOW/DENY）
     * POST /api/system/employees/{employeeId}/permissions
     */
    @PostMapping("/employees/{employeeId}/permissions")
    @ApiOperation("设置员工显式权限覆盖（ALLOW/DENY）")
    public Result<Void> setEmployeePermissions(@PathVariable Integer employeeId, @RequestBody java.util.List<EmployeePermissionDto> req) {
        try {
            systemService.setEmployeePermissions(employeeId, req);
            return Result.success("设置成功", null);
        } catch (Exception e) {
            log.error("设置员工权限失败 employeeId={}", employeeId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "设置失败：" + e.getMessage());
        }
    }

    /**
     * 编辑员工信息
     *
     * @param employeeId 员工ID
     * @param request    更新请求
     * @return 更新结果
     */
    @RequireActionPermission("人事管理:员工档案:编辑")
    @PutMapping("/employee/{employeeId}")
    @ApiOperation(value = "编辑员工信息", notes = "更新EMPLOYEE表并同步角色信息，支持上传、删除身份证照片")
    public Result<Void> updateEmployee(@PathVariable Integer employeeId,
                                       @ModelAttribute @Validated EmployeeUpdateRequest request,
                                       @RequestPart(value = "idCardFront", required = false) MultipartFile idCardFront,
                                       @RequestPart(value = "idCardBack", required = false) MultipartFile idCardBack,
                                       @RequestParam(value = "deleteIdCardFront", required = false, defaultValue = "false") Boolean deleteIdCardFront,
                                       @RequestParam(value = "deleteIdCardBack", required = false, defaultValue = "false") Boolean deleteIdCardBack,
                                       HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            systemService.updateEmployee(employeeId, request, idCardFront, idCardBack, deleteIdCardFront, deleteIdCardBack);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("系统管理", "编辑",
                    "更新员工信息：ID=" + employeeId, userId, ipAddress, true, null);
            return Result.success("更新员工成功", null);
        } catch (BusinessException e) {
            log.warn("编辑员工业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "编辑",
                    "更新员工信息：ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("编辑员工失败", e);
            logRecordService.recordOperationLog("系统管理", "编辑",
                    "更新员工信息：ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新员工失败：" + e.getMessage());
        }
    }

    /**
     * 直接新增员工（支持上传身份证图片）
     *
     * @param request 员工创建请求
     * @param idCardFront 身份证正面图片（可选）
     * @param idCardBack 身份证反面图片（可选）
     * @return 创建结果
     */
    @RequireActionPermission("人事管理:员工档案:新增")
    @PostMapping("/employee")
    @ApiOperation(value = "新增员工信息", notes = "直接向EMPLOYEE表写入员工信息，支持绑定角色和上传身份证图片")
    public Result<EmployeeCreateResponse> createEmployee(@ModelAttribute @Validated EmployeeCreateRequest request,
                                                         @RequestPart(value = "idCardFront", required = false) MultipartFile idCardFront,
                                                         @RequestPart(value = "idCardBack", required = false) MultipartFile idCardBack,
                                                         HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            EmployeeCreateResponse response = systemService.createEmployee(request, idCardFront, idCardBack);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("系统管理", "新增",
                    "新增员工：" + response.getEmployeeName(), userId, ipAddress, true, null);
            return Result.success("新增员工成功", response);
        } catch (BusinessException e) {
            log.warn("新增员工业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "新增",
                    "新增员工：" + request.getEmployeeName(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增员工失败", e);
            logRecordService.recordOperationLog("系统管理", "新增",
                    "新增员工：" + request.getEmployeeName(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增员工失败：" + e.getMessage());
        }
    }

    /**
     * 重置员工密码
     *
     * @param employeeId 员工ID
     * @param request    重置密码请求
     * @return 重置结果
     */
    @RequireActionPermission("人事管理:员工档案:重置密码")
    @PutMapping("/employee/{employeeId}/password")
    @ApiOperation(value = "重置员工密码", notes = "重置指定员工的登录密码")
    public Result<Void> resetEmployeePassword(@PathVariable Integer employeeId,
                                              @Validated @RequestBody ResetPasswordRequest request,
                                              HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("重置员工密码：employeeId={}", employeeId);
            systemService.resetEmployeePassword(employeeId, request);
            // 记录敏感操作日志
            logRecordService.recordOperationLog("系统管理", "重置密码", 
                    "重置员工密码：员工ID=" + employeeId, userId, ipAddress, true, null);
            return Result.success("重置密码成功", null);
        } catch (BusinessException e) {
            log.warn("重置密码业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "重置密码", 
                    "重置员工密码：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("重置密码失败", e);
            logRecordService.recordOperationLog("系统管理", "重置密码", 
                    "重置员工密码：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "重置密码失败：" + e.getMessage());
        }
    }

    /**
     * 审核通过员工注册
     *
     * @param registrationId 注册编号
     * @return 审核结果（包含新创建的员工ID）
     */
    @RequireActionPermission("人事管理:员工档案:编辑")
    @PostMapping("/employee/registration/{registrationId}/approve")
    @ApiOperation(value = "审核通过员工注册", notes = "将注册状态改为已通过，并将注册信息同步到EMPLOYEE表创建正式员工，支持审核时修改注册信息和上传/删除身份证图片")
    public Result<RegistrationApproveResponse> approveRegistration(@PathVariable Integer registrationId,
                                                                    @RequestParam(value = "employeeName", required = false) String employeeName,
                                                                    @RequestParam(value = "phone", required = false) String phone,
                                                                    @RequestParam(value = "email", required = false) String email,
                                                                    @RequestParam(value = "idNumber", required = false) String idNumber,
                                                                    @RequestParam(value = "department", required = false) String department,
                                                                    @RequestParam(value = "jobTitle", required = false) String jobTitle,
                                                                    @RequestPart(value = "idCardFront", required = false) MultipartFile idCardFront,
                                                                    @RequestPart(value = "idCardBack", required = false) MultipartFile idCardBack,
                                                                    @RequestParam(value = "deleteIdCardFront", required = false, defaultValue = "false") Boolean deleteIdCardFront,
                                                                    @RequestParam(value = "deleteIdCardBack", required = false, defaultValue = "false") Boolean deleteIdCardBack,
                                                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("审核通过员工注册：registrationId={}, employeeName={}, phone={}, deleteIdCardFront={}, deleteIdCardBack={}", registrationId, employeeName, phone, deleteIdCardFront, deleteIdCardBack);
            RegistrationApproveRequest request = new RegistrationApproveRequest();
            request.setEmployeeName(employeeName);
            request.setPhone(phone);
            request.setEmail(email);
            request.setIdNumber(idNumber);
            request.setDepartment(department);
            request.setJobTitle(jobTitle);
            RegistrationApproveResponse response = systemService.approveRegistration(registrationId, request, idCardFront, idCardBack, deleteIdCardFront, deleteIdCardBack);
            // 记录审核操作日志
            logRecordService.recordOperationLog("系统管理", "审核",
                    "审核通过员工注册：注册ID=" + registrationId + "，员工ID=" + response.getEmployeeId(),
                    userId, ipAddress, true, null);
            return Result.success("审核通过，员工账号已创建", response);
        } catch (BusinessException e) {
            log.warn("审核通过业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "审核",
                    "审核通过员工注册：注册ID=" + registrationId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审核通过失败", e);
            logRecordService.recordOperationLog("系统管理", "审核",
                    "审核通过员工注册：注册ID=" + registrationId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "审核通过失败：" + e.getMessage());
        }
    }

    /**
     * 驳回员工注册
     *
     * @param registrationId 注册编号
     */
    @PostMapping("/employee/registration/{registrationId}/reject")
    @ApiOperation(value = "驳回员工注册", notes = "将注册状态改为已拒绝，如已创建正式员工则删除对应账号")
    public Result<Void> rejectRegistration(@PathVariable Integer registrationId,
                                            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("驳回员工注册：registrationId={}", registrationId);
            systemService.rejectRegistration(registrationId);
            // 记录审核操作日志
            logRecordService.recordOperationLog("系统管理", "审核", 
                    "驳回员工注册：注册ID=" + registrationId, userId, ipAddress, true, null);
            return Result.success("已驳回该注册申请", null);
        } catch (BusinessException e) {
            log.warn("驳回注册业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "审核", 
                    "驳回员工注册：注册ID=" + registrationId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("驳回员工注册失败", e);
            logRecordService.recordOperationLog("系统管理", "审核", 
                    "驳回员工注册：注册ID=" + registrationId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "驳回失败：" + e.getMessage());
        }
    }

    /**
     * 获取邮件通道配置
     */
    @RequirePagePermission("系统管理:邮件配置:页面")
    @GetMapping("/email-channel/config")
    @ApiOperation(value = "获取邮件通道配置", notes = "查询最新SMTP配置详情")
    public Result<EmailChannelConfigResponse> getEmailChannelConfig() {
        EmailChannelConfigResponse response = emailChannelService.getChannelConfig();
        return Result.success("查询成功", response);
    }

    /**
     * 保存邮件通道配置
     */
    @PostMapping("/email-channel/config")
    @ApiOperation(value = "保存邮件通道配置", notes = "保存配置并刷新缓存")
    public Result<EmailChannelConfigResponse> saveEmailChannelConfig(
            @Validated @RequestBody EmailChannelConfigSaveRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            EmailChannelConfigResponse response = emailChannelService.saveChannelConfig(request);
            // 记录敏感配置操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "保存邮件通道配置", userId, ipAddress, true, null);
            return Result.success("保存成功", response);
        } catch (BusinessException e) {
            log.warn("保存邮件配置业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "保存邮件通道配置", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("保存邮件配置失败", e);
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "保存邮件通道配置", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "保存失败：" + e.getMessage());
        }
    }

    /**
     * 邮件通道自检
     */
    @PostMapping("/email-channel/test-send")
    @ApiOperation(value = "邮件通道自检", notes = "发送测试邮件验证SMTP连通性")
    public Result<EmailChannelTestSendResponse> testEmailChannel(
            @Validated @RequestBody EmailChannelTestSendRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            EmailChannelTestSendResponse response = emailChannelService.testSend(request);
            // 记录操作日志
            logRecordService.recordOperationLog("系统管理", "测试", 
                    "测试发送邮件：" + request.getTargetEmail(), userId, ipAddress, true, null);
            return Result.success("测试邮件已发送", response);
        } catch (BusinessException e) {
            log.warn("邮件自检业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "测试", 
                    "测试发送邮件：" + request.getTargetEmail(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("邮件自检失败", e);
            logRecordService.recordOperationLog("系统管理", "测试", 
                    "测试发送邮件：" + request.getTargetEmail(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "测试失败：" + e.getMessage());
        }
    }

    /**
     * 根据配置名称获取系统配置
     *
     * @param name 配置名称，例如：UNIT、OUT_OF_SCOPE_SERVICES
     * @return 配置信息
     */
    @GetMapping("/config/{name}")
    @ApiOperation(value = "获取系统配置", notes = "根据配置名称从SYS_CONFIG表中读取配置内容")
    public Result<SysConfig> getSysConfig(@PathVariable String name) {
        SysConfig config = sysConfigService.getByName(name);
        return Result.success("查询成功", config);
    }

    /**
     * 批量获取系统配置
     *
     * @param names 配置名称列表，例如：["UNIT", "OUT_OF_SCOPE_SERVICES"]
     * @return 配置信息Map，key为配置名称，value为配置对象
     * 
     * 注意：此接口不要求页面权限，因为读取系统配置（如单位列表、价外服务配置等）是业务功能需要的，
     * 所有用户都应该能够读取这些配置数据，而不需要系统管理权限。
     */
    @PostMapping("/config/batch")
    @ApiOperation(value = "批量获取系统配置", notes = "根据配置名称列表批量从SYS_CONFIG表中读取配置内容")
    public Result<Map<String, SysConfig>> getBatchSysConfigs(@RequestBody List<String> names) {
        Map<String, SysConfig> result = new HashMap<>();
        if (names != null && !names.isEmpty()) {
            for (String name : names) {
                if (name != null && !name.trim().isEmpty()) {
                    SysConfig config = sysConfigService.getByName(name.trim());
                    if (config != null) {
                        result.put(name, config);
                    }
                }
            }
        }
        return Result.success("查询成功", result);
    }

    /**
     * 保存或更新系统配置
     *
     * @param name   配置名称
     * @param value  配置内容（JSON 或 文本）
     * @param remark 备注说明
     * @return 最新配置信息
     */
    @PostMapping("/config/{name}")
    @ApiOperation(value = "保存系统配置", notes = "根据配置名称在SYS_CONFIG表中保存或更新配置内容")
    public Result<SysConfig> saveSysConfig(@PathVariable String name,
                                           @RequestBody SysConfigSaveRequest request,
                                           HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            SysConfig config = sysConfigService.saveOrUpdate(name, request.getValue(), request.getRemark(),
                    userId == null ? null : userId.longValue());
            logRecordService.recordOperationLog("系统管理", "编辑",
                    "保存系统配置：" + name, userId, ipAddress, true, null);
            return Result.success("保存成功", config);
        } catch (com.erp.common.exception.BusinessException e) {
            log.warn("保存系统配置业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "编辑",
                    "保存系统配置：" + name, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("保存系统配置失败", e);
            logRecordService.recordOperationLog("系统管理", "编辑",
                    "保存系统配置：" + name, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "保存失败：" + e.getMessage());
        }
    }

    /**
     * 获取员工已分配的角色列表
     *
     * @param employeeId 员工ID
     * @return 角色ID列表
     */
    @GetMapping("/employee/{employeeId}/roles")
    @ApiOperation(value = "获取员工已分配的角色列表", notes = "获取指定员工已分配的所有角色编号列表")
    public Result<List<Integer>> getEmployeeRoles(@PathVariable Integer employeeId,
                                                   HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("获取员工角色列表：employeeId={}", employeeId);
            List<Integer> roleIds = systemService.getEmployeeRoles(employeeId);
            logRecordService.recordOperationLog("系统管理", "查询", 
                    "获取员工角色列表：员工ID=" + employeeId, userId, ipAddress, true, null);
            return Result.success("查询成功", roleIds);
        } catch (BusinessException e) {
            log.warn("获取员工角色列表业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "查询", 
                    "获取员工角色列表：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取员工角色列表失败", e);
            logRecordService.recordOperationLog("系统管理", "查询", 
                    "获取员工角色列表：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 分配员工角色（差分保存）
     *
     * @param employeeId 员工ID
     * @param request    角色分配请求（包含角色ID列表）
     * @return 分配结果
     */
    @RequireActionPermission("人事管理:员工档案:分配角色")
    @PostMapping("/employee/{employeeId}/roles")
    @ApiOperation(value = "分配员工角色", notes = "为员工分配角色，实现差分保存（新增该新增的，删除该删除的），使用事务处理确保数据一致性")
    public Result<EmployeeRoleAssignResponse> assignEmployeeRoles(@PathVariable Integer employeeId,
                                                                    @RequestBody Map<String, List<Integer>> request,
                                                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("分配员工角色：employeeId={}, roleIds={}", employeeId, request.get("roleIds"));
            List<Integer> roleIds = request.get("roleIds");
            EmployeeRoleAssignResponse response = systemService.assignEmployeeRoles(employeeId, roleIds);
            logRecordService.recordOperationLog("系统管理", "分配角色", 
                    String.format("分配员工角色：员工ID=%d，新增=%d，移除=%d，保持不变=%d", 
                            employeeId, response.getAdded(), response.getRemoved(), response.getUnchanged()), 
                    userId, ipAddress, true, null);
            return Result.success("分配成功", response);
        } catch (BusinessException e) {
            log.warn("分配员工角色业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "分配角色", 
                    "分配员工角色：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("分配员工角色失败", e);
            logRecordService.recordOperationLog("系统管理", "分配角色", 
                    "分配员工角色：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "分配失败：" + e.getMessage());
        }
    }

    /**
     * 分配员工页面权限（差量保存）
     *
     * @param employeeId 员工ID
     * @param request    权限分配请求
     * @return 分配结果
     */
    @RequireActionPermission("人事管理:员工档案:分配角色")
    @PostMapping("/employee/{employeeId}/page-permissions")
    @ApiOperation(value = "分配员工页面权限", notes = "为员工分配页面级权限，实现差量保存（新增该新增的，更新该更新的，删除该删除的），批量事务保存，最大限度减少数据库存取次数")
    public Result<EmployeePermissionAssignResponse> assignEmployeePagePermissions(
            @PathVariable Integer employeeId,
            @Validated @RequestBody EmployeePermissionAssignRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("分配员工页面权限：employeeId={}, permissionCount={}", 
                    employeeId, request.getPermissions() != null ? request.getPermissions().size() : 0);
            
            EmployeePermissionAssignResponse response = systemService.assignEmployeePagePermissions(employeeId, request);
            
            logRecordService.recordOperationLog("系统管理", "分配权限", 
                    String.format("分配员工页面权限：员工ID=%d，新增=%d，更新=%d，删除=%d，保持不变=%d", 
                            employeeId, response.getAdded(), response.getUpdated(), 
                            response.getDeleted(), response.getUnchanged()), 
                    userId, ipAddress, true, null);
            
            return Result.success("分配成功", response);
        } catch (BusinessException e) {
            log.warn("分配员工页面权限业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "分配权限", 
                    "分配员工页面权限：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("分配员工页面权限失败", e);
            logRecordService.recordOperationLog("系统管理", "分配权限", 
                    "分配员工页面权限：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "分配失败：" + e.getMessage());
        }
    }

    /**
     * 获取员工页面权限列表
     *
     * @param employeeId 员工ID
     * @return 员工页面权限详情列表
     */
    @RequirePagePermission("人事管理:员工档案:页面")
    @GetMapping("/employee/{employeeId}/page-permissions")
    @ApiOperation(value = "获取员工页面权限列表", notes = "查询员工的所有页面级权限配置，包含可查看、可编辑、数据范围、操作范围等信息")
    public Result<List<com.erp.controller.system.dto.EmployeePagePermissionDetail>> getEmployeePagePermissions(
            @PathVariable Integer employeeId,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("查询员工页面权限：employeeId={}", employeeId);
            
            List<com.erp.controller.system.dto.EmployeePagePermissionDetail> permissions = 
                    systemService.getEmployeePagePermissions(employeeId);
            
            logRecordService.recordOperationLog("系统管理", "查询", 
                    "查询员工页面权限：员工ID=" + employeeId, userId, ipAddress, true, null);
            
            return Result.success("查询成功", permissions);
        } catch (BusinessException e) {
            log.warn("查询员工页面权限业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("系统管理", "查询", 
                    "查询员工页面权限：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询员工页面权限失败", e);
            logRecordService.recordOperationLog("系统管理", "查询", 
                    "查询员工页面权限：员工ID=" + employeeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }
}





