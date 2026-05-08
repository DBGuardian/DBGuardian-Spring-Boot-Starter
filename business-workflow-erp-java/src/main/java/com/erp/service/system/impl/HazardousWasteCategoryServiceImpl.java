package com.erp.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigPageRequest;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigResponse;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigUpdateRequest;
import com.erp.entity.system.Employee;
import com.erp.entity.system.HazardousWasteCategory;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.mapper.system.HazardousWasteCategoryMapper;
import com.erp.mapper.system.HazardousWasteItemMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.util.SecurityUtil;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.service.system.HazardousWasteCategoryService;
import com.erp.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 危险废物类别配置业务实现
 */
@Service
public class HazardousWasteCategoryServiceImpl implements HazardousWasteCategoryService {

    @Autowired
    private HazardousWasteCategoryMapper hazardousWasteCategoryMapper;

    @Autowired
    private HazardousWasteItemMapper hazardousWasteItemMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private AuthService authService;
    @Override
    public HazardousWasteCategoryConfigResponse getCategoryConfig(String wasteCategory) {
        if (wasteCategory == null || wasteCategory.trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "废物类别不能为空");
        }
        HazardousWasteCategory category =
                hazardousWasteCategoryMapper.selectByWasteCategory(wasteCategory.trim());
        if (category == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "废物类别不存在");
        }
        HazardousWasteCategoryConfigResponse response = convertToResponse(category);
        // 补充创建人姓名（操作人）
        Integer creatorId = category.getCreatorId();
        if (creatorId != null) {
            Employee employee = employeeMapper.selectById(creatorId);
            if (employee != null) {
                response.setCreateUserName(employee.getEmployeeName());
            }
        }
        return response;
    }

    @Override
    public IPage<HazardousWasteCategoryConfigResponse> getCategoryConfigPage(
            HazardousWasteCategoryConfigPageRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "分页请求不能为空");
        }
        Long current = request.getCurrent() == null ? 1L : request.getCurrent();
        Long size = request.getSize() == null ? 10L : request.getSize();
        if (current <= 0) {
            current = 1L;
        }
        if (size <= 0) {
            size = 10L;
        }

        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);
        
        // 应用数据范围控制（viewScope）
        Integer creatorFilter = null;
        if (!admin) {
            // 获取当前员工对"废物类别限额"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:废物类别限额:页面");
            
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getViewScope())) {
                    // 仅查看自己创建的废物类别限额配置
                    creatorFilter = currentUserId;
                }
            }
            // 如果是 ALL 或没有配置，不添加限制（creatorFilter = null）
        }

        Page<HazardousWasteCategory> page = new Page<>(current, size);
        LambdaQueryWrapper<HazardousWasteCategory> wrapper = new LambdaQueryWrapper<>();
        
        // 数据范围过滤：仅查看自己创建的废物类别限额配置
        if (creatorFilter != null) {
            wrapper.eq(HazardousWasteCategory::getCreatorId, creatorFilter);
        }
        
        if (request.getWasteCategory() != null && !request.getWasteCategory().trim().isEmpty()) {
            wrapper.like(HazardousWasteCategory::getWasteCategory, request.getWasteCategory().trim());
        }
        if (request.getWasteCategoryName() != null && !request.getWasteCategoryName().trim().isEmpty()) {
            wrapper.like(HazardousWasteCategory::getWasteCategoryName, request.getWasteCategoryName().trim());
        }

        // 排序处理
        String orderBy = request.getOrderBy();
        String orderDirection = request.getOrderDirection();
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            boolean isAsc = "asc".equalsIgnoreCase(orderDirection);
            switch (orderBy.trim()) {
                case "categoryId":
                    wrapper.orderBy(true, isAsc, HazardousWasteCategory::getCategoryId);
                    break;
                case "wasteCategory":
                    wrapper.orderBy(true, isAsc, HazardousWasteCategory::getWasteCategory);
                    break;
                case "wasteCategoryName":
                    wrapper.orderBy(true, isAsc, HazardousWasteCategory::getWasteCategoryName);
                    break;
                default:
                    wrapper.orderByDesc(HazardousWasteCategory::getCategoryId);
            }
        } else {
            wrapper.orderByDesc(HazardousWasteCategory::getCategoryId);
        }

        IPage<HazardousWasteCategory> categoryPage =
                hazardousWasteCategoryMapper.selectPage(page, wrapper);

        List<HazardousWasteCategory> categories =
                categoryPage.getRecords() == null ? Collections.emptyList() : categoryPage.getRecords();

        // 批量解析创建人姓名，避免循环单条查询
        Map<Integer, String> creatorNameMap = resolveCreatorNames(categories);

        List<HazardousWasteCategoryConfigResponse> records = new ArrayList<>();
        if (!categories.isEmpty()) {
            for (HazardousWasteCategory category : categories) {
                HazardousWasteCategoryConfigResponse response = convertToResponse(category);
                Integer creatorId = category.getCreatorId();
                if (creatorId != null) {
                    response.setCreateUserName(creatorNameMap.get(creatorId));
                }
                records.add(response);
            }
        }

        Page<HazardousWasteCategoryConfigResponse> resultPage =
                new Page<>(categoryPage.getCurrent(), categoryPage.getSize(), categoryPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategoryConfig(HazardousWasteCategoryConfigUpdateRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        if (request == null || request.getWasteCategory() == null
                || request.getWasteCategory().trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "废物类别不能为空");
        }
        HazardousWasteCategory category =
                hazardousWasteCategoryMapper.selectByWasteCategory(request.getWasteCategory().trim());
        if (category == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "废物类别不存在");
        }

        // 应用操作范围控制（operateScope）
        if (!admin) {
            // 获取当前员工对"废物类别限额"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:废物类别限额:页面");
            
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    // 仅能操作自己创建的废物类别
                    if (!Objects.equals(category.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能修改自己创建的废物类别限额配置");
                    }
                }
            }
            // 如果是 ALL 或没有配置，不添加限制
        }

        category.setLimitAmount(request.getLimitAmount());
        category.setLimitStartTime(request.getLimitStartTime());
        category.setLimitEndTime(request.getLimitEndTime());
        int rows = hazardousWasteCategoryMapper.updateById(category);
        if (rows == 0) {
            throw new BusinessException("更新危废类别失败：记录已被其他用户修改");
        }
    }

    /**
     * 将类别实体转换为响应对象，并补充危废条目数量
     */
    private HazardousWasteCategoryConfigResponse convertToResponse(HazardousWasteCategory category) {
        HazardousWasteCategoryConfigResponse response = new HazardousWasteCategoryConfigResponse();
        if (category == null) {
            return response;
        }
        response.setCategoryId(category.getCategoryId());
        response.setWasteCategory(category.getWasteCategory());
        response.setWasteCategoryName(category.getWasteCategoryName());
        // 创建人编码（操作人ID）
        response.setCreatorId(category.getCreatorId());
        response.setLimitAmount(category.getLimitAmount());
        response.setLimitStartTime(category.getLimitStartTime());
        response.setLimitEndTime(category.getLimitEndTime());
        response.setWasteItemCount(resolveWasteItemCount(category.getCategoryId()));
        return response;
    }

    private long resolveWasteItemCount(Integer categoryId) {
        if (categoryId == null) {
            return 0L;
        }
        Long count = hazardousWasteItemMapper.countByCategoryId(categoryId);
        return count == null ? 0L : count;
    }

    /**
     * 批量根据创建人编码解析员工姓名
     *
     * @param categories 类别列表
     * @return 创建人编码 -> 员工姓名 映射
     */
    private Map<Integer, String> resolveCreatorNames(List<HazardousWasteCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Integer> creatorIds = new HashSet<>();
        for (HazardousWasteCategory category : categories) {
            if (category != null && category.getCreatorId() != null) {
                creatorIds.add(category.getCreatorId());
            }
        }
        if (creatorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Employee> employees = employeeMapper.selectBatchIds(creatorIds);
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, String> result = new HashMap<>(employees.size());
        for (Employee employee : employees) {
            if (employee != null && employee.getEmployeeId() != null) {
                result.put(employee.getEmployeeId(), employee.getEmployeeName());
            }
        }
        return result;
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录");
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
            return null;
        }
    }
}
