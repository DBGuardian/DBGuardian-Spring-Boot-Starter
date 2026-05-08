package com.erp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.system.dto.EmployeePageRequest;
import com.erp.controller.system.dto.EmployeePageResponse;
import com.erp.controller.system.dto.EmployeeCreateRequest;
import com.erp.controller.system.dto.EmployeeCreateResponse;
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
import com.erp.entity.common.File;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Employee;
import com.erp.entity.system.EmployeeRegistration;
import com.erp.entity.system.EmployeeRole;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.EmployeeRegistrationMapper;
import com.erp.service.common.FileService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.service.system.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.RedisTemplate;

import com.erp.common.constant.RedisConstant;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统管理服务实现类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Service
public class SystemServiceImpl implements SystemService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeRegistrationMapper employeeRegistrationMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileService fileService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @Autowired
    private ILogRecordService logRecordService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private com.erp.mapper.system.EmployeePermissionMapper employeePermissionMapper;
    
    @Autowired
    private com.erp.mapper.system.SystemMapper systemMapper;
    
    @Autowired
    private com.erp.mapper.system.PermissionMapper permissionMapper;

    /**
     * 员工注册
     *
     * @param request 注册请求
     * @return 注册响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeRegisterResponse registerEmployee(EmployeeRegisterRequest request) {
        // 1. 验证密码一致性
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "两次输入的密码不一致");
        }

        // 2. 检查手机号码是否已注册（检查EMPLOYEE_REGISTRATION表和EMPLOYEE表）
        EmployeeRegistration existingRegistration = employeeRegistrationMapper.selectByPhone(request.getPhone());
        if (existingRegistration != null) {
            throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "该手机号码已被注册");
        }

        Employee existingEmployee = employeeMapper.selectByLoginAccount(request.getPhone());
        if (existingEmployee != null) {
            throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "该手机号码已被注册");
        }

        // 3. 检查登录账号是否已存在
        EmployeeRegistration existingByAccount = employeeRegistrationMapper.selectByLoginAccount(request.getPhone());
        if (existingByAccount != null) {
            throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "该登录账号已被注册");
        }

        // 4. 处理身份证照片（选填）
        Integer idCardFrontFileId = null;
        Integer idCardBackFileId = null;

        // 只有上传了身份证正面照片才处理
        if (request.getIdCardFront() != null && !request.getIdCardFront().isEmpty()) {
            // 验证文件大小（5MB = 5 * 1024 * 1024 字节）
            long maxFileSize = 5 * 1024 * 1024;
            if (request.getIdCardFront().getSize() > maxFileSize) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证正面照片大小不能超过5MB");
            }
            // 验证文件格式（仅支持JPG/PNG）
            String frontFileName = request.getIdCardFront().getOriginalFilename();
            if (frontFileName != null && !isValidImageFormat(frontFileName)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证正面照片仅支持JPG/PNG格式");
            }
        }

        // 只有上传了身份证反面照片才处理
        if (request.getIdCardBack() != null && !request.getIdCardBack().isEmpty()) {
            // 验证文件大小（5MB = 5 * 1024 * 1024 字节）
            long maxFileSize = 5 * 1024 * 1024;
            if (request.getIdCardBack().getSize() > maxFileSize) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证反面照片大小不能超过5MB");
            }
            // 验证文件格式（仅支持JPG/PNG）
            String backFileName = request.getIdCardBack().getOriginalFilename();
            if (backFileName != null && !isValidImageFormat(backFileName)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证反面照片仅支持JPG/PNG格式");
            }
        }

        // 5. 创建员工注册实体
        EmployeeRegistration registration = new EmployeeRegistration();
        registration.setEmployeeName(request.getEmployeeName());
        registration.setDepartment(request.getDepartment());
        registration.setJobTitle(request.getJobTitle());
        registration.setPhone(request.getPhone());
        registration.setEmail(request.getEmail());
        registration.setLoginAccount(request.getPhone()); // 手机号码作为登录账号
        registration.setPassword(passwordEncoder.encode(request.getPassword())); // 密码加密
        registration.setIdCard(request.getIdCard());
        registration.setAuditStatus("待审核"); // 注册后状态为待审核
        registration.setPermissionStatus("待分配"); // 权限分配状态为待分配
        registration.setSubmitTime(LocalDateTime.now());
        registration.setCreateTime(LocalDateTime.now());
        registration.setUpdateTime(LocalDateTime.now());

        // 6. 先保存注册信息（获取注册编号）
        employeeRegistrationMapper.insert(registration);

        // 7. 上传身份证照片（选填）- 使用employee_registration业务类型关联EMPLOYEE_REGISTRATION表
        try {
            // 只有上传了身份证正面照片才上传
            if (request.getIdCardFront() != null && !request.getIdCardFront().isEmpty()) {
                log.info("开始上传身份证正面：registrationId={}, fileName={}, size={}",
                        registration.getRegistrationId(),
                        request.getIdCardFront().getOriginalFilename(),
                        request.getIdCardFront().getSize());
                File frontFile = fileService.uploadAndSave(
                        request.getIdCardFront(),
                        "employee_registration_id_card_front",
                        registration.getRegistrationId()
                );
                idCardFrontFileId = frontFile.getFileId();
                log.info("身份证正面上传成功：registrationId={}, fileId={}", registration.getRegistrationId(), idCardFrontFileId);
            } else {
                log.info("未上传身份证正面照片：registrationId={}", registration.getRegistrationId());
            }

            // 只有上传了身份证反面照片才上传
            if (request.getIdCardBack() != null && !request.getIdCardBack().isEmpty()) {
                log.info("开始上传身份证反面：registrationId={}, fileName={}, size={}",
                        registration.getRegistrationId(),
                        request.getIdCardBack().getOriginalFilename(),
                        request.getIdCardBack().getSize());
                File backFile = fileService.uploadAndSave(
                        request.getIdCardBack(),
                        "employee_registration_id_card_back",
                        registration.getRegistrationId()
                );
                idCardBackFileId = backFile.getFileId();
                log.info("身份证反面上传成功：registrationId={}, fileId={}", registration.getRegistrationId(), idCardBackFileId);
            } else {
                log.info("未上传身份证反面照片：registrationId={}", registration.getRegistrationId());
            }

            // 如果有上传照片，更新注册信息中的文件ID
            if (idCardFrontFileId != null || idCardBackFileId != null) {
                log.info("更新注册信息中的文件ID：registrationId={}, frontFileId={}, backFileId={}",
                        registration.getRegistrationId(), idCardFrontFileId, idCardBackFileId);
                registration.setIdCardFrontFileId(idCardFrontFileId);
                registration.setIdCardBackFileId(idCardBackFileId);
                int rows = employeeRegistrationMapper.updateById(registration);
                if (rows == 0) {
                    log.warn("更新注册信息失败（乐观锁冲突），registrationId={}", registration.getRegistrationId());
                } else {
                    log.info("注册信息文件ID更新成功：registrationId={}", registration.getRegistrationId());
                }
            }

        } catch (Exception e) {
            log.error("身份证照片上传失败", e);
            // 如果文件上传失败，回滚事务
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "身份证照片上传失败：" + e.getMessage());
        }

        // 8. 构建响应
        EmployeeRegisterResponse response = new EmployeeRegisterResponse();
        response.setEmployeeId(registration.getRegistrationId()); // 返回注册编号
        response.setEmployeeName(registration.getEmployeeName());
        response.setLoginAccount(registration.getLoginAccount());
        response.setEmployeeStatus(registration.getAuditStatus());
        response.setMessage("注册成功，等待管理员审核");

        // 9. 记录数据变更日志（员工注册）
        try {
            logRecordService.recordDataChangeLog("系统管理", "EMPLOYEE_REGISTRATION",
                    String.valueOf(registration.getRegistrationId()), "新增",
                    "员工注册：" + registration.getEmployeeName(),
                    null, registration, null, null, true, null);
        } catch (Exception e) {
            log.warn("记录员工注册数据变更日志失败", e);
        }

        log.info("员工注册成功：registrationId={}, employeeName={}", registration.getRegistrationId(), registration.getEmployeeName());
        return response;
    }

    /**
     * 分页查询员工注册信息
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @Override
    public IPage<EmployeeRegistrationPageResponse> getEmployeeRegistrationPage(EmployeeRegistrationPageRequest request) {
        // 创建分页对象
        Page<EmployeeRegistration> page = new Page<>(request.getCurrent(), request.getSize());

        // 调用Mapper进行分页查询
        IPage<EmployeeRegistration> registrationPage = employeeRegistrationMapper.selectPageList(
                page,
                request.getEmployeeName(),
                request.getDepartment(),
                request.getJobTitle(),
                request.getPhone(),
                request.getLoginAccount(),
                request.getAuditStatus(),
                request.getPermissionStatus()
        );

        // 转换为响应DTO
        List<EmployeeRegistrationPageResponse> responseList = new ArrayList<>();
        for (EmployeeRegistration registration : registrationPage.getRecords()) {
            EmployeeRegistrationPageResponse response = new EmployeeRegistrationPageResponse();
            response.setRegistrationId(registration.getRegistrationId());
            response.setEmployeeName(registration.getEmployeeName());
            response.setDepartment(registration.getDepartment());
            response.setJobTitle(registration.getJobTitle());
            response.setPhone(registration.getPhone());
            response.setEmail(registration.getEmail());
            response.setIdCard(registration.getIdCard());
            response.setLoginAccount(registration.getLoginAccount());
            response.setAuditStatus(registration.getAuditStatus());
            response.setPermissionStatus(registration.getPermissionStatus());
            response.setSubmitTime(registration.getSubmitTime());
            response.setAuditTime(registration.getAuditTime());
            response.setAuditorId(registration.getAuditorId());
            response.setAuditOpinion(registration.getAuditOpinion());
            response.setAssignerId(registration.getAssignerId());
            response.setAssignCompleteTime(registration.getAssignCompleteTime());
            response.setEmployeeId(registration.getEmployeeId());

            // 查询身份证正反面文件URL（支持两种场景）
            // 1. 注册时上传的文件：employee_registration_id_card_front/back + registrationId
            // 2. 审核时上传的文件：employee_id_card_front/back + employeeId
            // 设置身份证文件ID
            response.setIdCardFrontFileId(registration.getIdCardFrontFileId());
            response.setIdCardBackFileId(registration.getIdCardBackFileId());

            if (registration.getIdCardFrontFileId() != null) {
                // 先查询注册时上传的文件
                List<File> frontFiles = fileService.getFilesByBusiness(
                        "employee_registration_id_card_front",
                        registration.getRegistrationId()
                );
                // 如果没有找到，再查询审核时上传的文件
                if (frontFiles.isEmpty() && registration.getEmployeeId() != null) {
                    frontFiles = fileService.getFilesByBusiness(
                            "employee_id_card_front",
                            registration.getEmployeeId()
                    );
                }
                if (!frontFiles.isEmpty()) {
                    response.setIdCardFrontFileUrl(frontFiles.get(0).getFileUrl());
                    log.info("设置身份证正面URL：registrationId={}, url={}", registration.getRegistrationId(), frontFiles.get(0).getFileUrl());
                }
            }
            if (registration.getIdCardBackFileId() != null) {
                // 先查询注册时上传的文件
                List<File> backFiles = fileService.getFilesByBusiness(
                        "employee_registration_id_card_back",
                        registration.getRegistrationId()
                );
                // 如果没有找到，再查询审核时上传的文件
                if (backFiles.isEmpty() && registration.getEmployeeId() != null) {
                    backFiles = fileService.getFilesByBusiness(
                            "employee_id_card_back",
                            registration.getEmployeeId()
                    );
                }
                if (!backFiles.isEmpty()) {
                    response.setIdCardBackFileUrl(backFiles.get(0).getFileUrl());
                    log.info("设置身份证反面URL：registrationId={}, url={}", registration.getRegistrationId(), backFiles.get(0).getFileUrl());
                }
            }

            responseList.add(response);
        }

        // 创建分页响应对象
        Page<EmployeeRegistrationPageResponse> responsePage = new Page<>(registrationPage.getCurrent(), registrationPage.getSize(), registrationPage.getTotal());
        responsePage.setRecords(responseList);

        log.info("分页查询员工注册信息：current={}, size={}, total={}", request.getCurrent(), request.getSize(), responsePage.getTotal());
        return responsePage;
    }

    /**
     * 分页查询正式员工列表（从EMPLOYEE表读取）
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @Override
    public IPage<EmployeePageResponse> getEmployeePage(EmployeePageRequest request) {
        // 创建分页对象
        Page<Employee> page = new Page<>(request.getCurrent(), request.getSize());

        // 调用Mapper进行分页查询（使用EmployeeMapper查询EMPLOYEE表）
        IPage<Employee> employeePage = employeeMapper.selectPageList(
                page,
                request.getEmployeeName(),
                request.getDepartment(),
                request.getJobTitle(),
                request.getPhone(),
                request.getLoginAccount(),
                request.getEmployeeStatus(),
                request.getSortField(),
                request.getSortOrder()
        );

        // 转换为响应DTO
        List<EmployeePageResponse> responseList = new ArrayList<>();
        for (Employee employee : employeePage.getRecords()) {
            EmployeePageResponse response = new EmployeePageResponse();
            response.setEmployeeId(employee.getEmployeeId());
            response.setEmployeeName(employee.getEmployeeName());
            response.setDepartment(employee.getDepartment());
            response.setJobTitle(employee.getJobTitle());
            response.setPhone(employee.getPhone());
            response.setEmail(employee.getEmail());
            response.setLoginAccount(employee.getLoginAccount());
            response.setRole(employee.getRole());
            response.setEmployeeStatus(employee.getEmployeeStatus());
            response.setCreateTime(employee.getCreateTime());
            response.setUpdateTime(employee.getUpdateTime());
            response.setRemark(employee.getRemark());
            response.setIdCard(employee.getIdCard());
            // 填充身份证图片URL和文件ID
            response.setIdCardFrontUrl(fileService.getFileUrl(employee.getIdCardFrontFileId()));
            response.setIdCardBackUrl(fileService.getFileUrl(employee.getIdCardBackFileId()));
            response.setIdCardFrontFileId(employee.getIdCardFrontFileId());
            response.setIdCardBackFileId(employee.getIdCardBackFileId());
            responseList.add(response);
        }

        // 创建分页响应对象
        Page<EmployeePageResponse> responsePage = new Page<>(employeePage.getCurrent(), employeePage.getSize(), employeePage.getTotal());
        responsePage.setRecords(responseList);

        log.info("分页查询员工列表：current={}, size={}, total={}", request.getCurrent(), request.getSize(), responsePage.getTotal());
        return responsePage;
    }

    /**
     * 根据员工ID获取员工详情
     *
     * @param employeeId 员工ID
     * @return 员工实体
     */
    @Override
    public Employee getEmployeeById(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        return employeeMapper.selectById(employeeId);
    }

    /**
     * 获取员工下拉列表（支持关键词搜索）
     *
     * @param keyword 关键词（可选）
     * @return 员工下拉列表
     */
    @Override
    public List<Map<String, Object>> getEmployeeSelectList(String keyword) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<Employee>()
                .eq(Employee::getEmployeeStatus, "在职")
                .orderByAsc(Employee::getEmployeeName);
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Employee::getEmployeeName, keyword)
                    .or().like(Employee::getPhone, keyword));
        }
        List<Employee> list = employeeMapper.selectList(wrapper);
        return list.stream().map(emp -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("employeeId", emp.getEmployeeId());
            item.put("employeeName", emp.getEmployeeName());
            item.put("phone", emp.getPhone());
            item.put("idCard", emp.getIdCard());
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 验证图片格式是否为JPG或PNG
     *
     * @param fileName 文件名
     * @return 是否为有效格式
     */
    private boolean isValidImageFormat(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") || lowerFileName.endsWith(".png");
    }

    /**
     * 验证图片文件
     */
    private void validateImageFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            return;
        }
        // 验证文件大小（5MB）
        long maxFileSize = 5 * 1024 * 1024;
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), fieldName + "大小不能超过5MB");
        }
        // 验证文件格式
        String fileName = file.getOriginalFilename();
        if (fileName != null && !isValidImageFormat(fileName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), fieldName + "仅支持JPG/PNG格式");
        }
    }

    /**
     * 直接新增员工
     *
     * @param request 员工创建请求
     * @return 员工响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeCreateResponse createEmployee(EmployeeCreateRequest request,
                                               MultipartFile idCardFront, MultipartFile idCardBack) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "两次输入的密码不一致");
        }

        // 检查手机号/登录账号是否已存在
        Employee existingEmployee = employeeMapper.selectByLoginAccount(request.getPhone());
        if (existingEmployee != null) {
            throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "该手机号已存在");
        }

        // 构建员工实体
        Employee employee = new Employee();
        employee.setEmployeeName(request.getEmployeeName());
        employee.setDepartment(request.getDepartment());
        employee.setJobTitle(request.getJobTitle());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setIdCard(request.getIdCard());
        employee.setLoginAccount(request.getPhone());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setRole(request.getRole());
        employee.setEmployeeStatus("在职");
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        employeeMapper.insert(employee);

        // 处理身份证照片上传（选填）
        Integer idCardFrontFileId = null;
        Integer idCardBackFileId = null;

        try {
            // 只有上传了身份证正面照片才处理
            if (idCardFront != null && !idCardFront.isEmpty()) {
                // 验证文件大小（5MB）
                if (idCardFront.getSize() > 5 * 1024 * 1024) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证正面照片大小不能超过5MB");
                }
                // 验证文件格式
                String frontFileName = idCardFront.getOriginalFilename();
                if (frontFileName != null && !isValidImageFormat(frontFileName)) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证正面照片仅支持JPG/PNG格式");
                }
                File frontFile = fileService.uploadAndSave(
                        idCardFront,
                        "employee_id_card_front",
                        employee.getEmployeeId()
                );
                idCardFrontFileId = frontFile.getFileId();
            }

            // 只有上传了身份证反面照片才处理
            if (idCardBack != null && !idCardBack.isEmpty()) {
                // 验证文件大小（5MB）
                if (idCardBack.getSize() > 5 * 1024 * 1024) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证反面照片大小不能超过5MB");
                }
                // 验证文件格式
                String backFileName = idCardBack.getOriginalFilename();
                if (backFileName != null && !isValidImageFormat(backFileName)) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证反面照片仅支持JPG/PNG格式");
                }
                File backFile = fileService.uploadAndSave(
                        idCardBack,
                        "employee_id_card_back",
                        employee.getEmployeeId()
                );
                idCardBackFileId = backFile.getFileId();
            }

            // 如果有上传照片，使用 UpdateWrapper 更新员工信息中的文件ID
            if (idCardFrontFileId != null || idCardBackFileId != null) {
                LambdaUpdateWrapper<Employee> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Employee::getEmployeeId, employee.getEmployeeId());
                if (idCardFrontFileId != null) {
                    updateWrapper.set(Employee::getIdCardFrontFileId, idCardFrontFileId);
                }
                if (idCardBackFileId != null) {
                    updateWrapper.set(Employee::getIdCardBackFileId, idCardBackFileId);
                }
                employeeMapper.update(null, updateWrapper);
                log.info("更新员工身份证文件ID成功：employeeId={}, idCardFrontFileId={}, idCardBackFileId={}",
                        employee.getEmployeeId(), idCardFrontFileId, idCardBackFileId);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("身份证照片上传失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "身份证照片上传失败：" + e.getMessage());
        }

        EmployeeCreateResponse response = new EmployeeCreateResponse();
        response.setEmployeeId(employee.getEmployeeId());
        response.setEmployeeName(employee.getEmployeeName());
        response.setLoginAccount(employee.getLoginAccount());

        // 记录数据变更日志
        try {
            Integer currentUserId = getCurrentUserId();
            logRecordService.recordDataChangeLog("系统管理", "EMPLOYEE",
                    String.valueOf(employee.getEmployeeId()), "新增",
                    "新增员工：" + employee.getEmployeeName(),
                    null, employee, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录员工新增数据变更日志失败", e);
        }

        return response;
    }

    /**
     * 编辑员工信息
     *
     * @param employeeId 员工ID
     * @param request    更新请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(Integer employeeId, EmployeeUpdateRequest request,
                              MultipartFile idCardFront, MultipartFile idCardBack,
                              Boolean deleteIdCardFront, Boolean deleteIdCardBack) {
        log.info("updateEmployee 开始执行：employeeId={}, deleteIdCardFront={}, deleteIdCardBack={}, idCardFront={}, idCardBack={}",
                employeeId, deleteIdCardFront, deleteIdCardBack,
                idCardFront != null ? idCardFront.getOriginalFilename() : "null",
                idCardBack != null ? idCardBack.getOriginalFilename() : "null");

        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
        }

        log.info("当前员工身份证文件ID：idCardFrontFileId={}, idCardBackFileId={}",
                employee.getIdCardFrontFileId(), employee.getIdCardBackFileId());

        // 保存旧数据用于日志记录（在修改前获取完整数据）
        Employee oldEmployee = employeeMapper.selectById(employeeId);
        if (oldEmployee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
        }
        // 创建副本，避免后续修改影响旧数据
        Employee oldEmployeeCopy = new Employee();
        oldEmployeeCopy.setEmployeeId(oldEmployee.getEmployeeId());
        oldEmployeeCopy.setEmployeeName(oldEmployee.getEmployeeName());
        oldEmployeeCopy.setDepartment(oldEmployee.getDepartment());
        oldEmployeeCopy.setJobTitle(oldEmployee.getJobTitle());
        oldEmployeeCopy.setPhone(oldEmployee.getPhone());
        oldEmployeeCopy.setEmail(oldEmployee.getEmail());
        oldEmployeeCopy.setLoginAccount(oldEmployee.getLoginAccount());
        oldEmployeeCopy.setRole(oldEmployee.getRole());
        oldEmployeeCopy.setEmployeeStatus(oldEmployee.getEmployeeStatus());
        oldEmployeeCopy.setRemark(oldEmployee.getRemark());
        oldEmployeeCopy.setIdCard(oldEmployee.getIdCard());
        oldEmployeeCopy.setIdCardFrontFileId(oldEmployee.getIdCardFrontFileId());
        oldEmployeeCopy.setIdCardBackFileId(oldEmployee.getIdCardBackFileId());
        oldEmployeeCopy.setCreateTime(oldEmployee.getCreateTime());
        oldEmployeeCopy.setUpdateTime(oldEmployee.getUpdateTime());

        // 手机号更换需要校验唯一性（登录账号与手机号保持一致）
        if (!Objects.equals(employee.getPhone(), request.getPhone())) {
            Employee existed = employeeMapper.selectByLoginAccount(request.getPhone());
            if (existed != null && !Objects.equals(existed.getEmployeeId(), employeeId)) {
                throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "该手机号已存在");
            }
            employee.setLoginAccount(request.getPhone());
        }

        employee.setEmployeeName(request.getEmployeeName());
        employee.setDepartment(request.getDepartment());
        employee.setJobTitle(request.getJobTitle());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setEmployeeStatus(request.getEmployeeStatus());
        employee.setRemark(request.getRemark());
        employee.setIdCard(request.getIdCard());
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            employee.setRole(request.getRole());
        }
        employee.setUpdateTime(LocalDateTime.now());

        // 处理身份证正面照片（可能需要置空或上传新文件）
        boolean needClearFront = Boolean.TRUE.equals(deleteIdCardFront);
        boolean hasNewFrontFile = false;
        Integer newFrontFileId = null;
        log.info("处理身份证正面：needClearFront={}, 原始文件ID={}", needClearFront, employee.getIdCardFrontFileId());
        if (needClearFront && employee.getIdCardFrontFileId() != null) {
            Integer frontFileId = employee.getIdCardFrontFileId();
            employee.setIdCardFrontFileId(null);
            try {
                fileService.deleteFile(frontFileId);
            } catch (Exception e) {
                log.warn("删除身份证正面物理文件失败，数据库字段已置空：fileId={}, error={}", frontFileId, e.getMessage());
            }
        } else if (idCardFront != null && !idCardFront.isEmpty()) {
            // 上传新正面照片
            validateImageFile(idCardFront, "身份证正面照片");
            if (employee.getIdCardFrontFileId() != null) {
                Integer oldFileId = employee.getIdCardFrontFileId();
                try {
                    fileService.deleteFile(oldFileId);
                } catch (Exception e) {
                    log.warn("删除旧身份证正面物理文件失败：fileId={}, error={}", oldFileId, e.getMessage());
                }
            }
            File frontFile = fileService.uploadAndSave(
                    idCardFront, "employee_id_card_front", employee.getEmployeeId());
            newFrontFileId = frontFile.getFileId();
            employee.setIdCardFrontFileId(newFrontFileId);
            hasNewFrontFile = true;
        }

        // 处理身份证反面照片（可能需要置空或上传新文件）
        boolean needClearBack = Boolean.TRUE.equals(deleteIdCardBack);
        boolean hasNewBackFile = false;
        Integer newBackFileId = null;
        log.info("处理身份证反面：needClearBack={}, 原始文件ID={}", needClearBack, employee.getIdCardBackFileId());
        if (needClearBack && employee.getIdCardBackFileId() != null) {
            Integer backFileId = employee.getIdCardBackFileId();
            employee.setIdCardBackFileId(null);
            try {
                fileService.deleteFile(backFileId);
            } catch (Exception e) {
                log.warn("删除身份证反面物理文件失败，数据库字段已置空：fileId={}, error={}", backFileId, e.getMessage());
            }
        } else if (idCardBack != null && !idCardBack.isEmpty()) {
            // 上传新反面照片
            validateImageFile(idCardBack, "身份证反面照片");
            if (employee.getIdCardBackFileId() != null) {
                Integer oldFileId = employee.getIdCardBackFileId();
                try {
                    fileService.deleteFile(oldFileId);
                } catch (Exception e) {
                    log.warn("删除旧身份证反面物理文件失败：fileId={}, error={}", oldFileId, e.getMessage());
                }
            }
            File backFile = fileService.uploadAndSave(
                    idCardBack, "employee_id_card_back", employee.getEmployeeId());
            newBackFileId = backFile.getFileId();
            employee.setIdCardBackFileId(newBackFileId);
            hasNewBackFile = true;
        }

        // 使用 UpdateWrapper 确保字段都能被正确更新
        LambdaUpdateWrapper<Employee> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Employee::getEmployeeId, employeeId);
        // 设置需要更新的字段
        updateWrapper.set(Employee::getEmployeeName, employee.getEmployeeName());
        updateWrapper.set(Employee::getDepartment, employee.getDepartment());
        updateWrapper.set(Employee::getJobTitle, employee.getJobTitle());
        updateWrapper.set(Employee::getPhone, employee.getPhone());
        updateWrapper.set(Employee::getEmail, employee.getEmail());
        updateWrapper.set(Employee::getEmployeeStatus, employee.getEmployeeStatus());
        updateWrapper.set(Employee::getRemark, employee.getRemark());
        updateWrapper.set(Employee::getIdCard, employee.getIdCard());
        updateWrapper.set(Employee::getLoginAccount, employee.getLoginAccount());
        if (employee.getRole() != null) {
            updateWrapper.set(Employee::getRole, employee.getRole());
        }
        updateWrapper.set(Employee::getUpdateTime, LocalDateTime.now());
        // 如果需要清空身份证文件ID，使用 set 显式设置为 null
        if (needClearFront) {
            updateWrapper.set(Employee::getIdCardFrontFileId, null);
        }
        // 如果上传了新文件，也需要设置文件ID
        if (hasNewFrontFile && newFrontFileId != null) {
            updateWrapper.set(Employee::getIdCardFrontFileId, newFrontFileId);
        }
        if (needClearBack) {
            updateWrapper.set(Employee::getIdCardBackFileId, null);
        }
        // 如果上传了新文件，也需要设置文件ID
        if (hasNewBackFile && newBackFileId != null) {
            updateWrapper.set(Employee::getIdCardBackFileId, newBackFileId);
        }

        log.info("UpdateWrapper 设置完成：needClearFront={}, needClearBack={}, hasNewFrontFile={}, hasNewBackFile={}",
                needClearFront, needClearBack, hasNewFrontFile, hasNewBackFile);

        int updateRows = employeeMapper.update(null, updateWrapper);
        log.info("更新员工信息结果：updateRows={}", updateRows);
        if (updateRows == 0) {
            throw new BusinessException("更新员工信息失败：记录已被其他用户修改");
        }

        // 记录数据变更日志
        try {
            Integer currentUserId = getCurrentUserId();
            Employee newEmployee = employeeMapper.selectById(employeeId);
            logRecordService.recordDataChangeLog("系统管理", "EMPLOYEE",
                    String.valueOf(employeeId), "更新",
                    "更新员工信息：" + employee.getEmployeeName(),
                    oldEmployeeCopy, newEmployee, currentUserId, null, true, null);
        } catch (Exception e) {
            log.error("记录员工更新数据变更日志失败", e);
        }
    }

    /*
     * @param employeeId 员工ID
     * @param request    重置密码请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetEmployeePassword(Integer employeeId, ResetPasswordRequest request) {
        // 1. 验证两次密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "两次输入的密码不一致");
        }

        // 2. 查询员工是否存在
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
        }

        // 3. 加密新密码并更新
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        employee.setPassword(encodedPassword);
        employee.setUpdateTime(LocalDateTime.now());

        int rows = employeeMapper.updateById(employee);
        if (rows == 0) {
            throw new BusinessException("重置员工密码失败：记录已被其他用户修改");
        }

        log.info("重置员工密码成功：employeeId={}, employeeName={}", employeeId, employee.getEmployeeName());
    }

    /**
     * 审核通过员工注册
     *
     * @param registrationId 注册编号
     * @param request 修改后的员工注册信息（可选）
     * @return 审核结果（包含新创建的员工ID）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationApproveResponse approveRegistration(Integer registrationId, RegistrationApproveRequest request) {
        return approveRegistration(registrationId, request, null, null, false, false);
    }

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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationApproveResponse approveRegistration(Integer registrationId, RegistrationApproveRequest request,
                                                        MultipartFile idCardFront, MultipartFile idCardBack,
                                                        Boolean deleteIdCardFront, Boolean deleteIdCardBack) {
        // 1. 查询注册信息
        EmployeeRegistration registration = employeeRegistrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "注册信息不存在");
        }

        // 2. 检查审核状态
        if ("已通过".equals(registration.getAuditStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "该注册申请已审核通过，请在员工管理中查看");
        }
        if ("已拒绝".equals(registration.getAuditStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "该员工审核已被拒绝，无法通过审核");
        }

        // 3. 检查手机号/登录账号是否已存在于EMPLOYEE表（如果修改了手机号/账号需要特殊处理）
        if (request != null) {
            String newPhone = request.getPhone();
            if (newPhone != null && !newPhone.equals(registration.getPhone())) {
                // 手机号变更了，需要检查新手机号是否已被其他正式员工使用
                Employee employeeWithPhone = employeeMapper.selectByPhone(newPhone);
                if (employeeWithPhone != null) {
                    throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "该手机号已被其他员工使用");
                }
            }
        }

        // 4. 创建正式员工（保存旧数据用于日志记录）
        EmployeeRegistration oldRegistration = new EmployeeRegistration();
        oldRegistration.setRegistrationId(registration.getRegistrationId());
        oldRegistration.setEmployeeName(registration.getEmployeeName());
        oldRegistration.setDepartment(registration.getDepartment());
        oldRegistration.setJobTitle(registration.getJobTitle());
        oldRegistration.setPhone(registration.getPhone());
        oldRegistration.setEmail(registration.getEmail());
        oldRegistration.setLoginAccount(registration.getLoginAccount());
        oldRegistration.setIdCard(registration.getIdCard());
        oldRegistration.setAuditStatus(registration.getAuditStatus());
        oldRegistration.setPermissionStatus(registration.getPermissionStatus());
        oldRegistration.setSubmitTime(registration.getSubmitTime());
        oldRegistration.setCreateTime(registration.getCreateTime());
        oldRegistration.setUpdateTime(registration.getUpdateTime());

        Employee employee = new Employee();
        employee.setEmployeeName(registration.getEmployeeName());
        employee.setDepartment(registration.getDepartment());
        employee.setJobTitle(registration.getJobTitle());
        employee.setPhone(registration.getPhone());
        employee.setEmail(registration.getEmail());
        employee.setLoginAccount(registration.getLoginAccount());
        employee.setPassword(registration.getPassword()); // 密码已在注册时加密
        employee.setEmployeeStatus("在职");
        employee.setIdCard(registration.getIdCard()); // 身份证号码
        employee.setIdCardFrontFileId(registration.getIdCardFrontFileId()); // 身份证正面文件ID
        employee.setIdCardBackFileId(registration.getIdCardBackFileId()); // 身份证反面文件ID
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        // 5. 如果有修改请求，更新员工信息
        if (request != null) {
            if (request.getEmployeeName() != null) {
                employee.setEmployeeName(request.getEmployeeName());
            }
            if (request.getPhone() != null) {
                employee.setPhone(request.getPhone());
            }
            if (request.getEmail() != null) {
                employee.setEmail(request.getEmail());
            }
            if (request.getIdNumber() != null) {
                employee.setIdCard(request.getIdNumber());
            }
            if (request.getDepartment() != null) {
                employee.setDepartment(request.getDepartment());
            }
            if (request.getJobTitle() != null) {
                employee.setJobTitle(request.getJobTitle());
            }
        }

        employeeMapper.insert(employee);

        // 6. 如果有修改请求，同步更新 EMPLOYEE_REGISTRATION 表
        if (request != null) {
            if (request.getEmployeeName() != null) {
                registration.setEmployeeName(request.getEmployeeName());
            }
            if (request.getPhone() != null) {
                registration.setPhone(request.getPhone());
            }
            if (request.getEmail() != null) {
                registration.setEmail(request.getEmail());
            }
            if (request.getIdNumber() != null) {
                registration.setIdCard(request.getIdNumber());
            }
            if (request.getDepartment() != null) {
                registration.setDepartment(request.getDepartment());
            }
            if (request.getJobTitle() != null) {
                registration.setJobTitle(request.getJobTitle());
            }
        }

        // 7. 处理身份证照片上传/删除
        Integer newIdCardFrontFileId = null;
        Integer newIdCardBackFileId = null;
        boolean needUpdateEmployee = false;

        try {
            // 处理删除身份证正面照片
            if (Boolean.TRUE.equals(deleteIdCardFront)) {
                if (employee.getIdCardFrontFileId() != null) {
                    Integer frontFileId = employee.getIdCardFrontFileId();
                    employee.setIdCardFrontFileId(null);
                    registration.setIdCardFrontFileId(null);
                    newIdCardFrontFileId = null;
                    needUpdateEmployee = true;
                    try {
                        fileService.deleteFile(frontFileId);
                    } catch (Exception e) {
                        log.warn("删除身份证正面物理文件失败，数据库字段已置空：fileId={}, error={}", frontFileId, e.getMessage());
                    }
                }
            }
            // 处理上传身份证正面照片
            else if (idCardFront != null && !idCardFront.isEmpty()) {
                // 验证文件大小（5MB）
                if (idCardFront.getSize() > 5 * 1024 * 1024) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证正面照片大小不能超过5MB");
                }
                // 验证文件格式
                String frontFileName = idCardFront.getOriginalFilename();
                if (frontFileName != null && !isValidImageFormat(frontFileName)) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证正面照片仅支持JPG/PNG格式");
                }
                // 删除旧文件（如果有）
                if (employee.getIdCardFrontFileId() != null) {
                    Integer oldFileId = employee.getIdCardFrontFileId();
                    try {
                        fileService.deleteFile(oldFileId);
                    } catch (Exception e) {
                        log.warn("删除旧身份证正面物理文件失败：fileId={}, error={}", oldFileId, e.getMessage());
                    }
                }
                // 上传新文件
                File frontFile = fileService.uploadAndSave(
                        idCardFront, "employee_id_card_front", employee.getEmployeeId());
                newIdCardFrontFileId = frontFile.getFileId();
                employee.setIdCardFrontFileId(newIdCardFrontFileId);
                registration.setIdCardFrontFileId(newIdCardFrontFileId);
                needUpdateEmployee = true;
            }

            // 处理删除身份证反面照片
            if (Boolean.TRUE.equals(deleteIdCardBack)) {
                if (employee.getIdCardBackFileId() != null) {
                    Integer backFileId = employee.getIdCardBackFileId();
                    employee.setIdCardBackFileId(null);
                    registration.setIdCardBackFileId(null);
                    newIdCardBackFileId = null;
                    needUpdateEmployee = true;
                    try {
                        fileService.deleteFile(backFileId);
                    } catch (Exception e) {
                        log.warn("删除身份证反面物理文件失败，数据库字段已置空：fileId={}, error={}", backFileId, e.getMessage());
                    }
                }
            }
            // 处理上传身份证反面照片
            else if (idCardBack != null && !idCardBack.isEmpty()) {
                // 验证文件大小（5MB）
                if (idCardBack.getSize() > 5 * 1024 * 1024) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证反面照片大小不能超过5MB");
                }
                // 验证文件格式
                String backFileName = idCardBack.getOriginalFilename();
                if (backFileName != null && !isValidImageFormat(backFileName)) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "身份证反面照片仅支持JPG/PNG格式");
                }
                // 删除旧文件（如果有）
                if (employee.getIdCardBackFileId() != null) {
                    Integer oldFileId = employee.getIdCardBackFileId();
                    try {
                        fileService.deleteFile(oldFileId);
                    } catch (Exception e) {
                        log.warn("删除旧身份证反面物理文件失败：fileId={}, error={}", oldFileId, e.getMessage());
                    }
                }
                // 上传新文件
                File backFile = fileService.uploadAndSave(
                        idCardBack, "employee_id_card_back", employee.getEmployeeId());
                newIdCardBackFileId = backFile.getFileId();
                employee.setIdCardBackFileId(newIdCardBackFileId);
                registration.setIdCardBackFileId(newIdCardBackFileId);
                needUpdateEmployee = true;
            }

            // 如果有照片变更，更新员工信息
            if (needUpdateEmployee) {
                employeeMapper.updateById(employee);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("身份证照片处理失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "身份证照片处理失败：" + e.getMessage());
        }

        // 8. 更新注册信息的审核状态
        registration.setAuditStatus("已通过");
        registration.setAuditTime(LocalDateTime.now());
        registration.setEmployeeId(employee.getEmployeeId()); // 关联新创建的员工编码

        int rows = employeeRegistrationMapper.updateById(registration);
        if (rows == 0) {
            log.warn("更新注册审核状态失败（乐观锁冲突），registrationId={}", registrationId);
        }

        // 9. 发送注册成功消息给新员工
        try {
            String title = "员工账号注册成功";
            String content = String.format("恭喜您！您的员工账号【%s】已成功创建，登录账号为：%s。请妥善保管您的账号信息。",
                    employee.getEmployeeName(), employee.getLoginAccount());
            messageNotificationService.sendSystemMessage(title, content, employee.getEmployeeId());
            log.info("注册成功消息已发送：employeeId={}, employeeName={}",
                    employee.getEmployeeId(), employee.getEmployeeName());
        } catch (Exception e) {
            log.error("发送注册成功消息失败：employeeId={}, employeeName={}",
                    employee.getEmployeeId(), employee.getEmployeeName(), e);
            // 消息发送失败不影响主流程，只记录日志
        }

        // 10. 记录数据变更日志（审核通过）
        try {
            Integer currentUserId = getCurrentUserId();
            // 记录注册信息变更日志
            logRecordService.recordDataChangeLog("系统管理", "EMPLOYEE_REGISTRATION",
                    String.valueOf(registrationId), "审核通过",
                    "审核通过员工注册，注册ID=" + registrationId + "，员工ID=" + employee.getEmployeeId(),
                    oldRegistration, registration, currentUserId, null, true, null);
            // 记录新员工创建日志
            logRecordService.recordDataChangeLog("系统管理", "EMPLOYEE",
                    String.valueOf(employee.getEmployeeId()), "新增",
                    "审核通过创建员工：" + employee.getEmployeeName(),
                    null, employee, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录审核通过数据变更日志失败", e);
        }

        // 11. 构建响应
        RegistrationApproveResponse response = new RegistrationApproveResponse();
        response.setEmployeeId(employee.getEmployeeId());
        response.setEmployeeName(employee.getEmployeeName());
        response.setLoginAccount(employee.getLoginAccount());

        log.info("审核通过成功：registrationId={}, employeeId={}, employeeName={}",
                registrationId, employee.getEmployeeId(), employee.getEmployeeName());

        return response;
    }

    /**
     * 驳回员工注册
     *
     * @param registrationId 注册编号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRegistration(Integer registrationId) {
        // 1. 查询注册信息（保存旧数据用于日志记录）
        EmployeeRegistration registration = employeeRegistrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "注册信息不存在");
        }

        // 保存旧数据用于日志记录
        EmployeeRegistration oldRegistration = new EmployeeRegistration();
        oldRegistration.setRegistrationId(registration.getRegistrationId());
        oldRegistration.setEmployeeName(registration.getEmployeeName());
        oldRegistration.setDepartment(registration.getDepartment());
        oldRegistration.setJobTitle(registration.getJobTitle());
        oldRegistration.setPhone(registration.getPhone());
        oldRegistration.setEmail(registration.getEmail());
        oldRegistration.setLoginAccount(registration.getLoginAccount());
        oldRegistration.setIdCard(registration.getIdCard());
        oldRegistration.setAuditStatus(registration.getAuditStatus());
        oldRegistration.setPermissionStatus(registration.getPermissionStatus());
        oldRegistration.setSubmitTime(registration.getSubmitTime());
        oldRegistration.setCreateTime(registration.getCreateTime());
        oldRegistration.setUpdateTime(registration.getUpdateTime());

        // 2. 状态校验
        if ("已拒绝".equals(registration.getAuditStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "该注册申请已被驳回");
        }
        if ("已通过".equals(registration.getAuditStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "该注册申请已通过，无需重复操作");
        }

        // 3. 更新注册状态
        registration.setEmployeeId(null);
        registration.setAuditStatus("已拒绝");
        registration.setAuditTime(LocalDateTime.now());
        int rows = employeeRegistrationMapper.updateById(registration);
        if (rows == 0) {
            throw new BusinessException("驳回注册申请失败：记录已被其他用户修改");
        }

        // 4. 发送驳回消息给申请人
        try {
            String title = "员工账号注册申请被驳回";
            String content = String.format("很遗憾！您的员工账号【%s】注册申请已被驳回。如有疑问，请联系管理员。", registration.getEmployeeName());
            // 使用业务通知类型，关联到员工注册业务
            messageNotificationService.sendBusinessNotification(
                    "系统", title, content,
                    null, // receiverId（注册申请未关联正式员工ID）
                    null, // senderId
                    "EMPLOYEE_REGISTRATION", // 业务类型
                    registration.getRegistrationId() // 业务ID
            );
            log.info("注册驳回消息已发送：registrationId={}, employeeName={}",
                    registrationId, registration.getEmployeeName());
        } catch (Exception e) {
            log.error("发送注册驳回消息失败：registrationId={}, employeeName={}",
                    registrationId, registration.getEmployeeName(), e);
            // 消息发送失败不影响主流程，只记录日志
        }

        // 5. 记录数据变更日志（驳回）
        try {
            Integer currentUserId = getCurrentUserId();
            logRecordService.recordDataChangeLog("系统管理", "EMPLOYEE_REGISTRATION",
                    String.valueOf(registrationId), "驳回",
                    "驳回员工注册申请：" + registration.getEmployeeName(),
                    oldRegistration, registration, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录驳回数据变更日志失败", e);
        }

        log.info("注册申请已驳回：registrationId={}, employeeName={}", registrationId, registration.getEmployeeName());
    }

    /**
     * 获取当前登录用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        return userId;
    }

    @Override
    public java.util.Map<String, Object> getEmployeePermissions(Integer employeeId) {
        try {
            // 优先从缓存读取（permissionCodes），避免登录与后续读取重复计算
            final String viewCacheKey = RedisConstant.PERMISSION_PREFIX + "employee_view:" + employeeId;
            try {
                Object cached = redisTemplate.opsForValue().get(viewCacheKey);
                if (cached instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> cachedMap = (java.util.Map<String, Object>) cached;
                    if (cachedMap != null && !cachedMap.isEmpty()) {
                        return cachedMap;
                    }
                } else if (cached != null) {
                    // 缓存类型异常，删除避免污染
                    redisTemplate.delete(viewCacheKey);
                }
            } catch (Exception e) {
                log.warn("读取员工权限视图缓存失败 employeeId={}", employeeId, e);
            }

            // 1. 仅基于 employee_permission 表获取员工显式权限（不再通过员工角色、角色权限表推导）
            java.util.List<com.erp.entity.system.EmployeePermission> empPerms =
                    employeePermissionMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.system.EmployeePermission>()
                                    .eq("员工编码", employeeId));

            java.util.Set<String> permissionCodes = new java.util.HashSet<>();
            if (empPerms != null && !empPerms.isEmpty()) {
                // 2. 只查询员工实际拥有的权限ID对应的编码，不再全表扫描
                java.util.List<Integer> permIds = empPerms.stream()
                        .map(com.erp.entity.system.EmployeePermission::getPagePermissionId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());

                java.util.List<com.erp.entity.system.Permission> ownedPermissions =
                        permissionMapper.selectList(
                                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.system.Permission>()
                                        .in("权限编号", permIds)
                                        .isNotNull("权限编码")
                                        .ne("权限编码", ""));

                java.util.Map<Integer, String> idToCode = ownedPermissions.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.erp.entity.system.Permission::getPermissionId,
                                com.erp.entity.system.Permission::getPermissionCode,
                                (a, b) -> a
                        ));

                // 3. 基于员工显式权限构建最终权限编码集合
                for (com.erp.entity.system.EmployeePermission ep : empPerms) {
                    String code = idToCode.get(ep.getPagePermissionId());
                    if (code == null || code.trim().isEmpty()) {
                        continue;
                    }
                    permissionCodes.add(code.trim());
                }
            }

            // 4. 返回权限编码集合
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("permissionCodes", permissionCodes);

            // 写入缓存（默认30分钟；变更时由相关写入口主动删除）
            try {
                redisTemplate.opsForValue().set(viewCacheKey, resp, java.time.Duration.ofMinutes(30));
            } catch (Exception e) {
                log.warn("写入员工权限视图缓存失败 employeeId={}", employeeId, e);
            }
            return resp;
        } catch (Exception e) {
            log.error("获取员工权限失败 employeeId={}", employeeId, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEmployeePermissions(Integer employeeId, java.util.List<com.erp.controller.system.dto.EmployeePermissionDto> perms) {
        // 注意：此方法已废弃，新的权限模型使用 assignEmployeePagePermissions
        // 这里保留是为了向后兼容
        if (perms == null) return;
        
        // 读取旧的显式权限用于日志对比
        java.util.List<com.erp.entity.system.EmployeePermission> oldPerms = employeePermissionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.system.EmployeePermission>()
                        .eq("员工编码", employeeId));

        // 删除已有的针对该员工的显式权限
        int delCount = employeePermissionMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.system.EmployeePermission>()
                        .eq("员工编码", employeeId));
        log.info("删除员工显式权限：employeeId={}, deletedCount={}", employeeId, delCount);

        // 插入新的显式权限（使用新的字段结构）
        for (com.erp.controller.system.dto.EmployeePermissionDto dto : perms) {
            com.erp.entity.system.EmployeePermission ep = com.erp.entity.system.EmployeePermission.builder()
                    .employeeId(employeeId)
                    .pagePermissionId(dto.getPermissionId()) // 使用 pagePermissionId
                    .canView("ALLOW".equalsIgnoreCase(dto.getStatus()) ? 1 : 0)
                    .canEdit(0)
                    .viewScope("SELF")
                    .operateScope("SELF")
                    .version(0)
                    .build();
            employeePermissionMapper.insert(ep);
        }

        // 记录数据变更日志与操作日志，并刷新权限缓存
        try {
            Integer userId = getCurrentUserId();
            logRecordService.recordDataChangeLog("系统管理", "employee_permission", String.valueOf(employeeId),
                    "更新", "设置员工显式权限覆盖", oldPerms, perms, userId, null, true, null);
            logRecordService.recordOperationLog("系统管理", "设置员工权限", "employeeId=" + employeeId, userId, null, true, null);
        } catch (Exception e) {
            log.warn("记录员工权限变更日志失败", e);
        }
        try {
            redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee:" + employeeId);
            redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee_view:" + employeeId);
        } catch (Exception e) {
            log.warn("刷新员工权限缓存失败 employeeId={}", employeeId, e);
        }
    }

    /**
     * 获取员工已分配的角色列表
     *
     * @param employeeId 员工ID
     * @return 角色ID列表
     */
    @Override
    public List<Integer> getEmployeeRoles(Integer employeeId) {
        // 验证员工是否存在
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
        }
        
        // 查询员工已分配的角色ID列表
        List<Integer> roleIds = employeeRoleMapper.selectRoleIdsByEmployeeId(employeeId);
        return roleIds != null ? roleIds : Collections.emptyList();
    }

    /**
     * 分配员工角色（差分保存）
     *
     * @param employeeId 员工ID
     * @param roleIds    角色ID列表
     * @return 分配结果（包含新增、移除、保持不变的数量）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeRoleAssignResponse assignEmployeeRoles(Integer employeeId, List<Integer> roleIds) {
        // 验证员工是否存在
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
        }

        // 参数处理：如果roleIds为null，视为空列表
        if (roleIds == null) {
            roleIds = Collections.emptyList();
        }

        // 去重处理
        Set<Integer> newRoleIdSet = new HashSet<>(roleIds);
        List<Integer> newRoleIds = new ArrayList<>(newRoleIdSet);

        // 获取当前已分配的角色ID列表
        List<Integer> currentRoleIds = employeeRoleMapper.selectRoleIdsByEmployeeId(employeeId);
        if (currentRoleIds == null) {
            currentRoleIds = Collections.emptyList();
        }
        Set<Integer> currentRoleIdSet = new HashSet<>(currentRoleIds);

        // 计算需要新增的角色（在新列表中但不在当前列表中）
        List<Integer> toAdd = newRoleIds.stream()
                .filter(roleId -> !currentRoleIdSet.contains(roleId))
                .collect(Collectors.toList());

        // 计算需要删除的角色（在当前列表中但不在新列表中）
        List<Integer> toRemove = currentRoleIds.stream()
                .filter(roleId -> !newRoleIdSet.contains(roleId))
                .collect(Collectors.toList());

        // 计算保持不变的角色
        Set<Integer> unchangedSet = new HashSet<>(currentRoleIdSet);
        unchangedSet.retainAll(newRoleIdSet);
        int unchanged = unchangedSet.size();

        // 执行数据库操作
        try {
            // 删除需要移除的角色
            if (!toRemove.isEmpty()) {
                for (Integer roleId : toRemove) {
                    employeeRoleMapper.delete(
                            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EmployeeRole>()
                                    .eq("员工编码", employeeId)
                                    .eq("角色编号", roleId)
                    );
                }
            }

            // 新增需要添加的角色
            if (!toAdd.isEmpty()) {
                employeeRoleMapper.insertEmployeeRoles(employeeId, toAdd);
            }

            // 记录数据变更日志
            try {
                Integer currentUserId = getCurrentUserId();
                String changeDesc = String.format("分配员工角色：新增%d个，移除%d个，保持不变%d个", 
                        toAdd.size(), toRemove.size(), unchanged);
                logRecordService.recordDataChangeLog("系统管理", "EMPLOYEE_ROLE", 
                        String.valueOf(employeeId), "更新", changeDesc, 
                        currentRoleIds, newRoleIds, currentUserId, null, true, null);
            } catch (Exception e) {
                log.warn("记录员工角色分配数据变更日志失败", e);
            }

            // 构建响应
            EmployeeRoleAssignResponse response = new EmployeeRoleAssignResponse();
            response.setAdded(toAdd.size());
            response.setRemoved(toRemove.size());
            response.setUnchanged(unchanged);

            log.info("员工角色分配成功：employeeId={}, 新增={}, 移除={}, 保持不变={}", 
                    employeeId, toAdd.size(), toRemove.size(), unchanged);

            // 角色变更后需要刷新员工权限缓存
            try {
                redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee:" + employeeId);
                redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee_view:" + employeeId);
            } catch (Exception e) {
                log.warn("刷新员工权限缓存失败 employeeId={}", employeeId, e);
            }

            return response;
        } catch (Exception e) {
            log.error("分配员工角色失败：employeeId={}", employeeId, e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), 
                    "分配员工角色失败：" + e.getMessage());
        }
    }

    /**
     * 分配员工页面权限（差量保存）
     * 实现策略：
     * 1. 查询员工当前的所有页面权限
     * 2. 对比前端传入的权限列表，计算差异
     * 3. 批量新增、更新、删除，使用事务保证一致性
     * 4. 最大限度减少数据库操作次数
     * 
     * @param employeeId 员工ID
     * @param request    权限分配请求
     * @return 分配结果（包含新增、更新、删除、保持不变的数量）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeePermissionAssignResponse assignEmployeePagePermissions(Integer employeeId, EmployeePermissionAssignRequest request) {
        // 1. 验证员工是否存在
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
        }

        // 2. 参数处理
        if (request == null || request.getPermissions() == null) {
            request = EmployeePermissionAssignRequest.builder()
                    .permissions(Collections.emptyList())
                    .build();
        }

        // 3. 查询员工当前的所有页面权限
        List<EmployeePermission> currentPermissions = employeePermissionMapper.selectByEmployeeId(employeeId);
        if (currentPermissions == null) {
            currentPermissions = Collections.emptyList();
        }

        // 4. 构建当前权限的 Map（key: pagePermissionId, value: EmployeePermission）
        java.util.Map<Integer, EmployeePermission> currentPermMap = currentPermissions.stream()
                .collect(Collectors.toMap(
                        EmployeePermission::getPagePermissionId,
                        p -> p,
                        (a, b) -> a
                ));

        // 5. 构建新权限的 Map（key: pagePermissionId, value: PagePermissionItem）
        java.util.Map<Integer, EmployeePermissionAssignRequest.PagePermissionItem> newPermMap = 
                request.getPermissions().stream()
                        .collect(Collectors.toMap(
                                EmployeePermissionAssignRequest.PagePermissionItem::getPagePermissionId,
                                p -> p,
                                (a, b) -> a
                        ));

        // 6. 计算差异
        List<EmployeePermission> toInsert = new ArrayList<>();  // 需要新增的
        List<EmployeePermission> toUpdate = new ArrayList<>();  // 需要更新的
        List<Integer> toDelete = new ArrayList<>();             // 需要删除的（权限ID）
        int unchanged = 0;                                       // 保持不变的

        // 6.1 遍历新权限列表，找出需要新增和更新的
        for (EmployeePermissionAssignRequest.PagePermissionItem newItem : request.getPermissions()) {
            Integer pagePermissionId = newItem.getPagePermissionId();
            EmployeePermission currentPerm = currentPermMap.get(pagePermissionId);

            if (currentPerm == null) {
                // 当前不存在，需要新增
                EmployeePermission newPerm = EmployeePermission.builder()
                        .employeeId(employeeId)
                        .pagePermissionId(pagePermissionId)
                        .canView(newItem.getCanView())
                        .canEdit(newItem.getCanEdit())
                        .viewScope(newItem.getViewScope())
                        .operateScope(newItem.getOperateScope())
                        .version(0)
                        .build();
                toInsert.add(newPerm);
            } else {
                // 当前存在，检查是否需要更新
                boolean needUpdate = !Objects.equals(currentPerm.getCanView(), newItem.getCanView())
                        || !Objects.equals(currentPerm.getCanEdit(), newItem.getCanEdit())
                        || !Objects.equals(currentPerm.getViewScope(), newItem.getViewScope())
                        || !Objects.equals(currentPerm.getOperateScope(), newItem.getOperateScope());

                if (needUpdate) {
                    // 需要更新
                    EmployeePermission updatePerm = EmployeePermission.builder()
                            .employeeId(employeeId)
                            .pagePermissionId(pagePermissionId)
                            .canView(newItem.getCanView())
                            .canEdit(newItem.getCanEdit())
                            .viewScope(newItem.getViewScope())
                            .operateScope(newItem.getOperateScope())
                            .version(currentPerm.getVersion())
                            .build();
                    toUpdate.add(updatePerm);
                } else {
                    // 保持不变
                    unchanged++;
                }
            }
        }

        // 6.2 遍历当前权限列表，找出需要删除的（在当前列表中但不在新列表中）
        for (EmployeePermission currentPerm : currentPermissions) {
            if (!newPermMap.containsKey(currentPerm.getPagePermissionId())) {
                toDelete.add(currentPerm.getPagePermissionId());
            }
        }

        // 7. 执行数据库操作（批量操作，减少数据库交互次数）
        try {
            // 7.1 批量删除
            if (!toDelete.isEmpty()) {
                String permissionIdsCsv = toDelete.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                employeePermissionMapper.deleteByEmployeeAndPermissionIds(employeeId, permissionIdsCsv);
                log.info("批量删除员工权限：employeeId={}, count={}", employeeId, toDelete.size());
            }

            // 7.2 批量新增
            if (!toInsert.isEmpty()) {
                employeePermissionMapper.batchInsert(toInsert);
                log.info("批量新增员工权限：employeeId={}, count={}", employeeId, toInsert.size());
            }

            // 7.3 批量更新（MyBatis 不支持真正的批量 UPDATE，这里使用循环，但在同一事务中）
            if (!toUpdate.isEmpty()) {
                for (EmployeePermission perm : toUpdate) {
                    employeePermissionMapper.update(perm, 
                            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<EmployeePermission>()
                                    .eq("员工编码", perm.getEmployeeId())
                                    .eq("页面权限编号", perm.getPagePermissionId())
                    );
                }
                log.info("批量更新员工权限：employeeId={}, count={}", employeeId, toUpdate.size());
            }

            // 8. 记录数据变更日志
            try {
                Integer currentUserId = getCurrentUserId();
                String changeDesc = String.format("分配员工页面权限：新增%d个，更新%d个，删除%d个，保持不变%d个", 
                        toInsert.size(), toUpdate.size(), toDelete.size(), unchanged);
                logRecordService.recordDataChangeLog("系统管理", "employee_permission", 
                        String.valueOf(employeeId), "更新", changeDesc, 
                        currentPermissions, request.getPermissions(), currentUserId, null, true, null);
            } catch (Exception e) {
                log.warn("记录员工权限分配数据变更日志失败", e);
            }

            // 9. 刷新员工权限缓存
            try {
                redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee:" + employeeId);
                redisTemplate.delete(RedisConstant.PERMISSION_PREFIX + "employee_view:" + employeeId);
                log.info("刷新员工权限缓存：employeeId={}", employeeId);
            } catch (Exception e) {
                log.warn("刷新员工权限缓存失败 employeeId={}", employeeId, e);
            }

            // 10. 构建响应
            EmployeePermissionAssignResponse response = EmployeePermissionAssignResponse.builder()
                    .added(toInsert.size())
                    .updated(toUpdate.size())
                    .deleted(toDelete.size())
                    .unchanged(unchanged)
                    .build();

            log.info("员工页面权限分配成功：employeeId={}, 新增={}, 更新={}, 删除={}, 保持不变={}", 
                    employeeId, toInsert.size(), toUpdate.size(), toDelete.size(), unchanged);

            return response;
        } catch (Exception e) {
            log.error("分配员工页面权限失败：employeeId={}", employeeId, e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), 
                    "分配员工页面权限失败：" + e.getMessage());
        }
    }

    /**
     * 获取员工页面权限列表
     * 实现策略：
     * 1. 一次性查询员工的所有页面权限
     * 2. 一次性查询所有页面权限信息（用于补充权限名称、编码等）
     * 3. 在内存中组装数据，减少数据库查询次数
     * 
     * @param employeeId 员工ID
     * @return 员工页面权限详情列表
     */
    @Override
    public java.util.List<com.erp.controller.system.dto.EmployeePagePermissionDetail> getEmployeePagePermissions(Integer employeeId) {
        // 1. 验证员工是否存在
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "员工不存在");
        }

        // 2. 查询员工的所有页面权限
        List<EmployeePermission> employeePermissions = employeePermissionMapper.selectByEmployeeId(employeeId);
        if (employeePermissions == null || employeePermissions.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 提取所有页面权限ID
        List<Integer> pagePermissionIds = employeePermissions.stream()
                .map(EmployeePermission::getPagePermissionId)
                .collect(Collectors.toList());

        // 4. 一次性查询所有相关的权限信息（只查询页面类型的权限）
        java.util.List<com.erp.entity.system.Permission> permissions = permissionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.system.Permission>()
                        .in("权限编号", pagePermissionIds)
                        .eq("权限类型编号", 2) // 只查询页面类型
        );

        // 5. 构建权限ID到权限信息的映射
        java.util.Map<Integer, com.erp.entity.system.Permission> permissionMap = permissions.stream()
                .collect(Collectors.toMap(
                        com.erp.entity.system.Permission::getPermissionId,
                        p -> p,
                        (a, b) -> a
                ));

        // 6. 组装返回结果
        java.util.List<com.erp.controller.system.dto.EmployeePagePermissionDetail> result = new ArrayList<>();
        for (EmployeePermission ep : employeePermissions) {
            com.erp.entity.system.Permission permission = permissionMap.get(ep.getPagePermissionId());
            if (permission == null) {
                // 权限不存在或不是页面类型，跳过
                continue;
            }

            com.erp.controller.system.dto.EmployeePagePermissionDetail detail = 
                    com.erp.controller.system.dto.EmployeePagePermissionDetail.builder()
                            .pagePermissionId(ep.getPagePermissionId())
                            .permissionName(permission.getPermissionName())
                            .permissionCode(permission.getPermissionCode())
                            .pageMode(permission.getPageMode())
                            .canView(ep.getCanView())
                            .canEdit(ep.getCanEdit())
                            .viewScope(ep.getViewScope())
                            .operateScope(ep.getOperateScope())
                            .build();
            result.add(detail);
        }

        log.info("查询员工页面权限：employeeId={}, count={}", employeeId, result.size());
        return result;
    }
}

