package com.erp.controller.system;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.result.Result;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.system.dto.RoleBatchDeleteRequest;
import com.erp.controller.system.dto.RoleCreateRequest;
import com.erp.controller.system.dto.RoleInfoDTO;
import com.erp.controller.system.dto.RolePermissionsBatchGetRequest;
import com.erp.controller.system.dto.RoleUpdateRequest;
import com.erp.entity.system.Role;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 角色管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/system/roles")
@Api(tags = "角色管理")
@Validated
public class RoleController {

    @Autowired
    private com.erp.service.system.RoleService roleService;

    @Autowired
    private ILogRecordService logRecordService;

    @RequirePagePermission({
            "人事管理:角色管理:页面",
            "系统管理:角色管理:页面",
            "人事管理:员工档案:页面",
            "系统管理:员工管理:页面"
    })
    @ApiOperation("分页/列表查询角色（包含权限数量和用户数量）")
    @GetMapping("")
    public Result<List<RoleInfoDTO>> list(
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleDesc) {
        try {
            return Result.success("查询成功", roleService.listAllWithDetails(roleCode, roleName, roleDesc));
        } catch (Exception e) {
            log.error("查询角色失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("创建角色")
    @PostMapping("")
    public Result<Integer> create(@RequestBody @Validated RoleCreateRequest request, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean createSuccess = false;
        String errorMessage = null;

        try {
            Role role = Role.builder()
                    .roleName(request.getRoleName())
                    .roleDesc(request.getRoleDesc())
                    .build();

            Integer roleId = roleService.create(role);
            createSuccess = true;
            return Result.success("创建成功", roleId);
        } catch (Exception e) {
            log.error("创建角色失败", e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "创建失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("新增角色：角色名称=%s，描述=%s",
                        request.getRoleName(), request.getRoleDesc());
                logRecordService.recordOperationLog("角色管理", "新增",
                        logContent, userId, ipAddress, createSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录新增角色操作日志失败", logEx);
            }
        }
    }

    @ApiOperation("更新角色")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody @Validated RoleUpdateRequest request,
                               HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean updateSuccess = false;
        String errorMessage = null;

        try {
            roleService.update(id, request);
            updateSuccess = true;
            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新角色失败 id={}", id, e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("更新角色：角色ID=%s，角色名称=%s",
                        id, request.getRoleName());
                logRecordService.recordOperationLog("角色管理", "更新",
                        logContent, userId, ipAddress, updateSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录更新角色操作日志失败", logEx);
            }
        }
    }

    @ApiOperation("删除角色（返回受影响员工数并需二次确认）")
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable Integer id,
                                 @RequestParam(name = "confirm", required = false, defaultValue = "false") boolean confirm,
                                 HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;

        try {
            Object result = roleService.delete(id, confirm);
            deleteSuccess = true;
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除角色失败 id={}", id, e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("删除角色：角色ID=%s，二次确认=%s",
                        id, confirm);
                logRecordService.recordOperationLog("角色管理", "删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录删除角色操作日志失败", logEx);
            }
        }
    }

    @ApiOperation("批量删除角色")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@Validated @RequestBody RoleBatchDeleteRequest request,
                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;

        try {
            roleService.batchDelete(request.getIds());
            deleteSuccess = true;
            return Result.success("批量删除成功，共删除" + request.getIds().size() + "个角色", null);
        } catch (Exception e) {
            log.error("批量删除角色失败", e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量删除失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("批量删除角色：角色IDs=%s，数量=%s",
                        request.getIds(), request.getIds() != null ? request.getIds().size() : 0);
                logRecordService.recordOperationLog("角色管理", "批量删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录批量删除角色操作日志失败", logEx);
            }
        }
    }

    @ApiOperation("为角色批量设置权限（permissionIds）")
    @PostMapping("/{id}/permissions")
    public Result<Void> setRolePermissions(@PathVariable Integer id,
                                           @RequestBody List<Integer> permissionIds,
                                           HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean setSuccess = false;
        String errorMessage = null;

        try {
            roleService.setRolePermissions(id, permissionIds);
            setSuccess = true;
            return Result.success("设置成功", null);
        } catch (Exception e) {
            log.error("设置角色权限失败 roleId={}", id, e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "设置失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("分配角色权限：角色ID=%s，权限数量=%s",
                        id, permissionIds != null ? permissionIds.size() : 0);
                logRecordService.recordOperationLog("角色管理", "权限变更",
                        logContent, userId, ipAddress, setSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录分配角色权限操作日志失败", logEx);
            }
        }
    }

    @ApiOperation("获取角色的权限列表（permissionIds）")
    @GetMapping("/{id}/permissions")
    public Result<List<Integer>> getRolePermissions(@PathVariable Integer id) {
        try {
            return Result.success("查询成功", roleService.getRolePermissions(id));
        } catch (Exception e) {
            log.error("查询角色权限失败 roleId={}", id, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取角色成员（分页简化为全部）")
    @GetMapping("/{id}/members")
    public Result<List<Integer>> getRoleMembers(@PathVariable Integer id) {
        try {
            return Result.success("查询成功", roleService.getRoleMembers(id));
        } catch (Exception e) {
            log.error("查询角色成员失败 roleId={}", id, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("批量查询多个角色的权限编码并集（去重）")
    @PostMapping("/permissions:batchGet")
    public Result<List<String>> batchGetRolePermissionCodes(@Validated @RequestBody RolePermissionsBatchGetRequest request) {
        try {
            return Result.success("查询成功", roleService.batchGetPermissionCodesByRoleIds(request.getRoleIds()));
        } catch (Exception e) {
            log.error("批量查询角色权限编码失败 roleIds={}", request == null ? null : request.getRoleIds(), e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }
}



