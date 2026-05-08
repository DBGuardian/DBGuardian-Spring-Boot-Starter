package com.erp.service.customer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.customer.dto.CustomerFollowUpCreateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpDetailCreateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpDetailResponse;
import com.erp.controller.customer.dto.CustomerFollowUpDetailUpdateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpPageRequest;
import com.erp.controller.customer.dto.CustomerFollowUpResponse;
import com.erp.controller.customer.dto.CustomerFollowUpUpdateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpUpdateWithDetailsRequest;
import com.erp.controller.customer.dto.CustomerFollowUpWithDetailsResponse;
import com.erp.entity.customer.Customer;
import com.erp.entity.customer.CustomerFollowUp;
import com.erp.entity.customer.CustomerFollowUpDetail;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.mapper.customer.CustomerFollowUpDetailMapper;
import com.erp.mapper.customer.CustomerFollowUpMapper;
import com.erp.mapper.customer.CustomerMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.customer.CustomerFollowUpDetailService;
import com.erp.service.customer.CustomerFollowUpService;
import com.erp.service.system.ILogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * 客户跟进服务实现
 */
@Slf4j
@Service
public class CustomerFollowUpServiceImpl implements CustomerFollowUpService {

    @Autowired
    private CustomerFollowUpMapper customerFollowUpMapper;

    @Autowired
    private CustomerFollowUpDetailMapper customerFollowUpDetailMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerFollowUpDetailService customerFollowUpDetailService;

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


    /**
     * 获取员工的页面权限配置
     * 
     * @param employeeId 员工ID
     * @param pageCode 页面权限编码
     * @return 员工页面权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        try {
            // 从数据库查询页面权限ID
            Permission permission = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getPermissionCode, pageCode)
                    .eq(Permission::getPermissionTypeId, 2) // 2 = 页面级权限
            );
            
            if (permission == null) {
                return null;
            }

            // 查询员工页面权限配置
            EmployeePermission employeePermission = employeePermissionMapper.selectOne(
                new LambdaQueryWrapper<EmployeePermission>()
                    .eq(EmployeePermission::getEmployeeId, employeeId)
                    .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
            );
            
            return employeePermission;
        } catch (Exception e) {
            log.error("获取员工页面权限配置失败：employeeId={}, pageCode={}", employeeId, pageCode, e);
            return null;
        }
    }

    @Override
    public List<CustomerFollowUpResponse> getCurrentUserFollowUps() {
        Integer currentUserId = getCurrentUserId();
        List<CustomerFollowUp> followUps = customerFollowUpMapper.selectByEmployeeId(currentUserId);
        return followUps.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerFollowUpResponse createFollowUp(CustomerFollowUpCreateRequest request) {
        Integer currentUserId = getCurrentUserId();

        // 验证客户是否存在
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "客户不存在");
        }

        // 创建跟进记录
        CustomerFollowUp followUp = new CustomerFollowUp();
        followUp.setCustomerId(request.getCustomerId());
        followUp.setEmployeeId(currentUserId);
        followUp.setContactName(request.getContactName());
        followUp.setContactPhone(request.getContactPhone());
        followUp.setCreatorId(currentUserId);
        followUp.setRemark(request.getRemark());
        followUp.setCreateTime(LocalDateTime.now());

        int result = customerFollowUpMapper.insert(followUp);
        if (result <= 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增客户跟进记录失败");
        }

        // 重新查询完整的跟进记录（包含关联信息）
        List<CustomerFollowUp> followUps = customerFollowUpMapper.selectByCustomerIdAndEmployeeId(
                followUp.getCustomerId(), followUp.getEmployeeId());
        CustomerFollowUp savedFollowUp = followUps.stream()
                .filter(f -> f.getFollowUpId().equals(followUp.getFollowUpId()))
                .findFirst()
                .orElse(followUp);
        return convertToResponse(savedFollowUp);
    }

    @Override
    public List<CustomerFollowUpResponse> getFollowUpsByCustomerId(Integer customerId) {
        if (customerId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "客户ID不能为空");
        }
        List<CustomerFollowUp> followUps = customerFollowUpMapper.selectByCustomerId(customerId);
        return followUps.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<CustomerFollowUpResponse> getFollowUpPage(CustomerFollowUpPageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        
        // 应用数据范围控制（viewScope）
        Integer creatorFilter = null;
        if (!admin) {
            // 获取当前员工对"客户跟进"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:客户跟进:页面");
            
            if (permission != null) {
                log.debug("获取到员工页面权限配置：employeeId={}, viewScope={}, operateScope={}, canEdit={}", 
                    currentUserId, permission.getViewScope(), permission.getOperateScope(), permission.getCanEdit());
                
                // 检查是否可以操作（canEdit=0表示不可操作）
                if (permission.getCanEdit() != null && permission.getCanEdit() == 0) {
                    // 只读权限，但仍可查看数据
                }
                
                if ("SELF".equalsIgnoreCase(permission.getViewScope())) {
                    // 仅查看自己创建的跟进记录
                    creatorFilter = currentUserId;
                    log.debug("应用数据范围控制：仅查看自己创建的跟进记录，creatorFilter={}", creatorFilter);
                } else {
                    log.debug("数据范围控制：查看全部跟进记录（viewScope={}）", permission.getViewScope());
                }
            } else {
                log.debug("未获取到员工页面权限配置，employeeId={}，使用默认查询逻辑（业务员编码过滤）", currentUserId);
            }
            // 如果是 ALL 或没有配置，不添加限制（creatorFilter = null）
        } else {
            log.debug("当前用户为超级管理员，不应用数据范围控制");
        }

        // 构建分页参数
        Page<CustomerFollowUp> page = new Page<>(request.getCurrent(), request.getSize());

        // 执行分页查询
        IPage<CustomerFollowUp> followUpPage = customerFollowUpMapper.selectPageByEmployeeId(
                page,
                currentUserId,
                request.getCustomerId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getContactName(),
                request.getContactPhone(),
                creatorFilter,  // 数据范围过滤参数
                request.getOrderBy(),
                request.getOrderDirection()
        );

        // 转换为响应对象
        List<CustomerFollowUpResponse> records = followUpPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 返回新的分页结果
        Page<CustomerFollowUpResponse> responsePage = new Page<>(followUpPage.getCurrent(), followUpPage.getSize(), followUpPage.getTotal());
        responsePage.setRecords(records);
        return responsePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatchByIds(List<Integer> followUpIds) {
        if (followUpIds == null || followUpIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要删除的跟进记录");
        }
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 应用操作范围控制（operateScope）
        EmployeePermission permission = null;
        if (!admin) {
            // 获取当前员工对"客户跟进"页面的权限配置
            permission = getEmployeePagePermission(currentUserId, "业务管理:客户跟进:页面");
        }

        // 预验证：检查所有跟进记录是否存在和权限
        for (Integer followUpId : followUpIds) {
            CustomerFollowUp followUp = customerFollowUpMapper.selectById(followUpId);
            if (followUp == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "跟进记录不存在，ID：" + followUpId + "，批量操作已取消");
            }
            
            // 应用操作范围控制（operateScope）
            if (!admin) {
                if (permission != null) {
                    if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                        // 仅能操作自己创建的跟进记录
                        if (!Objects.equals(followUp.getCreatorId(), currentUserId)) {
                            throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能删除自己创建的跟进记录，ID：" + followUpId);
                        }
                    }
                }
                // 如果是 ALL 或没有配置，不添加限制
            }
        }

        // 查询要删除的数据用于日志记录
        CustomerFollowUp followUpToDelete = customerFollowUpMapper.selectById(followUpIds.get(0));
        String followUpInfo = followUpToDelete != null ? "客户ID=" + followUpToDelete.getCustomerId() : "未知";

        // 先删除所有关联的明细
        for (Integer followUpId : followUpIds) {
            customerFollowUpDetailMapper.deleteByFollowUpId(followUpId);
        }

        // 再删除跟进记录
        int result = customerFollowUpMapper.deleteBatchByIds(followUpIds, currentUserId);
        if (result <= 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除客户跟进记录失败");
        }

        // 记录数据变更日志（删除操作）
        try {
            logRecordService.recordDataChangeLog("客户管理", "CUSTOMER_FOLLOW_UP",
                    followUpIds.toString(), "删除",
                    "批量删除客户跟进记录：" + result + "条，客户ID=" + followUpInfo,
                    followUpToDelete, null, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录客户跟进记录删除数据变更日志失败", e);
        }

        log.info("用户ID={}批量删除了{}条客户跟进记录，IDs={}", currentUserId, result, followUpIds);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerFollowUpResponse updateFollowUp(CustomerFollowUpUpdateRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 验证跟进记录是否存在
        CustomerFollowUp followUp = customerFollowUpMapper.selectById(request.getFollowUpId());
        if (followUp == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "跟进记录不存在");
        }

        // 应用操作范围控制（operateScope）
        if (!admin) {
            // 获取当前员工对"客户跟进"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:客户跟进:页面");
            
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    // 仅能操作自己创建的跟进记录
                    if (!Objects.equals(followUp.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能编辑自己创建的跟进记录");
                    }
                }
            }
            // 如果是 ALL 或没有配置，不添加限制
        }

        // 验证客户是否存在
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "客户不存在");
        }

        // 保存旧数据用于日志记录
        CustomerFollowUp oldFollowUp = customerFollowUpMapper.selectById(request.getFollowUpId());

        // 更新跟进记录
        followUp.setCustomerId(request.getCustomerId());
        followUp.setContactName(request.getContactName());
        followUp.setContactPhone(request.getContactPhone());
        followUp.setRemark(request.getRemark());
        followUp.setUpdateTime(LocalDateTime.now());

        int result = customerFollowUpMapper.updateById(followUp);
        if (result <= 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新客户跟进记录失败");
        }

        // 重新查询完整的跟进记录（包含关联信息）
        List<CustomerFollowUp> followUps = customerFollowUpMapper.selectByCustomerIdAndEmployeeId(
                followUp.getCustomerId(), followUp.getEmployeeId());
        CustomerFollowUp updatedFollowUp = followUps.stream()
                .filter(f -> f.getFollowUpId().equals(followUp.getFollowUpId()))
                .findFirst()
                .orElse(followUp);

        // 记录数据变更日志（更新操作）
        try {
            CustomerFollowUp newFollowUp = customerFollowUpMapper.selectById(request.getFollowUpId());
            logRecordService.recordDataChangeLog("客户管理", "CUSTOMER_FOLLOW_UP",
                    String.valueOf(request.getFollowUpId()), "更新",
                    "更新客户跟进记录：ID=" + request.getFollowUpId(),
                    oldFollowUp, newFollowUp, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录客户跟进记录更新数据变更日志失败", e);
        }

        return convertToResponse(updatedFollowUp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerFollowUpWithDetailsResponse updateFollowUpWithDetails(CustomerFollowUpUpdateWithDetailsRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 验证跟进记录是否存在
        CustomerFollowUp followUp = customerFollowUpMapper.selectById(request.getFollowUpId());
        if (followUp == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "跟进记录不存在");
        }

        // 应用操作范围控制（operateScope）
        if (!admin) {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:客户跟进:页面");
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    if (!Objects.equals(followUp.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能编辑自己创建的跟进记录");
                    }
                }
            }
        }

        // 验证客户是否存在
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "客户不存在");
        }

        // 保存旧数据用于日志记录
        CustomerFollowUp oldFollowUp = customerFollowUpMapper.selectById(request.getFollowUpId());

        // 更新主记录基础信息
        followUp.setCustomerId(request.getCustomerId());
        followUp.setContactName(request.getContactName());
        followUp.setContactPhone(request.getContactPhone());
        followUp.setRemark(request.getRemark());
        followUp.setUpdateTime(LocalDateTime.now());
        customerFollowUpMapper.updateById(followUp);

        // 处理明细差分修改
        // 1. 删除需要删除的明细
        if (request.getDeletedDetailIds() != null && !request.getDeletedDetailIds().isEmpty()) {
            for (Integer detailId : request.getDeletedDetailIds()) {
                CustomerFollowUpDetail detail = customerFollowUpDetailMapper.selectById(detailId);
                if (detail != null && "已完成".equals(detail.getFollowStatus())) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "已完成的明细不能删除，ID：" + detailId);
                }
            }
            customerFollowUpDetailMapper.deleteBatchByIds(request.getDeletedDetailIds());
        }

        // 2. 新增需要新增的明细
        if (request.getAddedDetails() != null && !request.getAddedDetails().isEmpty()) {
            for (CustomerFollowUpDetailCreateRequest addRequest : request.getAddedDetails()) {
                CustomerFollowUpDetail detail = new CustomerFollowUpDetail();
                detail.setFollowUpId(request.getFollowUpId());
                detail.setFollowTime(addRequest.getFollowTime());
                detail.setFollowContent(addRequest.getFollowContent());
                detail.setFollowStatus(addRequest.getFollowStatus() != null ? addRequest.getFollowStatus() : "未完成");
                detail.setCreatorId(currentUserId);
                detail.setRemark(addRequest.getRemark());
                detail.setCreateTime(LocalDateTime.now());
                customerFollowUpDetailMapper.insert(detail);
            }
        }

        // 3. 更新需要更新的明细
        if (request.getUpdatedDetails() != null && !request.getUpdatedDetails().isEmpty()) {
            for (CustomerFollowUpDetailUpdateRequest updateRequest : request.getUpdatedDetails()) {
                CustomerFollowUpDetail detail = customerFollowUpDetailMapper.selectById(updateRequest.getDetailId());
                if (detail == null) {
                    throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "更新的明细不存在，ID：" + updateRequest.getDetailId());
                }
                detail.setFollowTime(updateRequest.getFollowTime());
                detail.setFollowContent(updateRequest.getFollowContent());
                detail.setFollowStatus(updateRequest.getFollowStatus());
                detail.setRemark(updateRequest.getRemark());
                detail.setUpdateTime(LocalDateTime.now());
                customerFollowUpDetailMapper.updateById(detail);
            }
        }

        // 记录数据变更日志
        try {
            CustomerFollowUp newFollowUp = customerFollowUpMapper.selectById(request.getFollowUpId());
            logRecordService.recordDataChangeLog("客户管理", "CUSTOMER_FOLLOW_UP",
                    String.valueOf(request.getFollowUpId()), "更新",
                    "更新客户跟进记录（包含明细）：ID=" + request.getFollowUpId(),
                    oldFollowUp, newFollowUp, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录客户跟进记录更新数据变更日志失败", e);
        }

        // 返回完整的跟进记录（包含明细）
        return customerFollowUpDetailService.getFollowUpWithDetails(request.getFollowUpId());
    }

    @Override
    public List<CustomerFollowUpWithDetailsResponse> exportFollowUps(CustomerFollowUpPageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 导出数据范围控制（viewScope）
        // 注意：后端主动校验并强制覆盖 creatorFilter，防止前端漏传或篡改越权
        Integer creatorFilter;
        if (admin) {
            // 超级管理员导出全部
            creatorFilter = null;
        } else {
            // 获取当前员工对"客户跟进"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "业务管理:客户跟进:页面");
            if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                // viewScope=SELF：强制只导出自己创建的数据，防止前端漏传或篡改
                creatorFilter = currentUserId;
                log.debug("导出数据范围控制：仅导出自己创建的跟进记录，creatorFilter={}", creatorFilter);
            } else if (permission == null) {
                // 无权限配置时，默认只能导出自己的数据（最小权限原则）
                creatorFilter = currentUserId;
                log.debug("导出无权限配置，默认仅导出自己创建的跟进记录，creatorFilter={}", creatorFilter);
            } else {
                // viewScope=ALL：导出全部数据，忽略前端传入的 creatorFilter
                creatorFilter = null;
                log.debug("导出数据范围控制：查看全部（viewScope={}），不过滤创建人", permission.getViewScope());
            }
        }

        // 不分页，查询所有符合条件的数据
        Page<CustomerFollowUp> page = new Page<>(1, Integer.MAX_VALUE);
        IPage<CustomerFollowUp> followUpPage = customerFollowUpMapper.selectPageByEmployeeId(
                page,
                currentUserId,
                request.getCustomerId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getContactName(),
                request.getContactPhone(),
                creatorFilter,  // 数据范围过滤参数（后端强制校验后的值）
                request.getOrderBy(),
                request.getOrderDirection()
        );

        // 转换为带明细的响应对象
        return followUpPage.getRecords().stream()
                .map(followUp -> {
                    CustomerFollowUpWithDetailsResponse response = new CustomerFollowUpWithDetailsResponse();
                    BeanUtils.copyProperties(followUp, response);
                    response.setCreatorId(followUp.getCreatorId());
                    // 查询明细
                    List<CustomerFollowUpDetailResponse> details = customerFollowUpDetailService.getDetailsByFollowUpId(followUp.getFollowUpId());
                    response.setDetails(details);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应对象
     */
    private CustomerFollowUpResponse convertToResponse(CustomerFollowUp followUp) {
        CustomerFollowUpResponse response = new CustomerFollowUpResponse();
        BeanUtils.copyProperties(followUp, response);
        // 确保creatorId字段被正确设置（前端需要用于权限判断）
        response.setCreatorId(followUp.getCreatorId());
        return response;
    }
}

