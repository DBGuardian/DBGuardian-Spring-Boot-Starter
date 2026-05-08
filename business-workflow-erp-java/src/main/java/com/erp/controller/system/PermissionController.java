package com.erp.controller.system;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.system.dto.*;
import com.erp.service.system.PermissionService;
import com.erp.service.system.ILogRecordService;
import com.erp.common.annotation.RequirePagePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 权限相关控制器
 * 重新实现：统一日志、异常处理风格，与 SystemController/WasteCodeController 保持一致
 */
@Slf4j
@RestController
@RequestMapping("/permission")
@Api(tags = "权限管理")
@Validated
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 权限分页查询
     */
    @RequirePagePermission({
            "系统管理:权限管理:页面",
            "人事管理:权限分配:页面",
            "系统管理:角色管理:页面",
            "人事管理:角色管理:页面"
    })
    @GetMapping("/list")
    @ApiOperation(value = "权限分页查询", notes = "支持按权限类型ID、权限名称（模糊）、权限编码（模糊）、父权限ID筛选")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<PermissionResponse>> getPermissionPage(@Valid PermissionPageRequest request) {
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<PermissionResponse> page = permissionService.getPermissionPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @RequirePagePermission({
            "系统管理:权限管理:页面",
            "人事管理:权限分配:页面",
            "系统管理:角色管理:页面",
            "人事管理:角色管理:页面"
    })
    @ApiOperation("获取权限树")
    @GetMapping("/tree")
    public Result<List<PermissionTreeNode>> tree() {
        try {
            log.info("构建权限树");
            List<PermissionTreeNode> tree = permissionService.tree();
            return Result.success("查询成功", tree);
        } catch (BusinessException e) {
            log.warn("构建权限树业务异常：{}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("构建权限树失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取权限详情")
    @GetMapping("/{id}")
    public Result<PermissionResponse> get(@PathVariable Integer id) {
        try {
            log.info("查询权限详情 id={}", id);
            PermissionResponse resp = permissionService.get(id);
            if (resp == null) return Result.error(ResultCodeEnum.NOT_FOUND.getCode(), "权限不存在");
            return Result.success("查询成功", resp);
        } catch (BusinessException e) {
            log.warn("查询权限详情业务异常 id={} {}", id, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询权限失败 id={}", id, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("创建权限")
    @PostMapping("")
    public Result<Integer> create(@RequestBody PermissionRequest req, HttpServletRequest request) {
        try {
            log.info("创建权限 name={}", req.getPermissionName());
            Integer id = permissionService.create(req);
            logRecordService.recordOperationLog("权限管理", "创建权限", "permissionId=" + id, null, request.getRemoteAddr(), true, null);
            return Result.success("创建成功", id);
        } catch (BusinessException e) {
            log.warn("创建权限业务异常：{}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("创建权限失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "创建失败：" + e.getMessage());
        }
    }

    @ApiOperation("更新权限")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody PermissionRequest req, HttpServletRequest request) {
        try {
            log.info("更新权限 id={}", id);
            permissionService.update(id, req);
            logRecordService.recordOperationLog("权限管理", "更新权限", "id=" + id, null, request.getRemoteAddr(), true, null);
            return Result.success("更新成功", null);
        } catch (BusinessException e) {
            log.warn("更新权限业务异常 id={} {}", id, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新权限失败 id={}", id, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新失败：" + e.getMessage());
        }
    }

    @ApiOperation("删除权限")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id, HttpServletRequest request) {
        try {
            log.info("删除权限 id={}", id);
            permissionService.delete(id);
            logRecordService.recordOperationLog("权限管理", "删除权限", "id=" + id, null, request.getRemoteAddr(), true, null);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            log.warn("删除权限业务异常 id={} {}", id, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除权限失败 id={}", id, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        }
    }

    @ApiOperation("模块下新增页面权限")
    @PostMapping("/{moduleId}/pages")
    public Result<List<Integer>> addPagesToModule(@PathVariable Integer moduleId, @RequestBody List<PermissionRequest> pages, HttpServletRequest request) {
        try {
            log.info("模块下新增页面 moduleId={} count={}", moduleId, pages == null ? 0 : pages.size());
            List<Integer> createdIds = permissionService.addPagesToModule(moduleId, pages);
            logRecordService.recordOperationLog("权限管理", "模块新增页面", "moduleId=" + moduleId, null, request.getRemoteAddr(), true, null);
            return Result.success("创建成功", createdIds);
        } catch (BusinessException e) {
            log.warn("模块下新增页面业务异常 moduleId={} {}", moduleId, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("模块下新增页面失败 moduleId={}", moduleId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "创建失败：" + e.getMessage());
        }
    }

    @ApiOperation("从模块中移除页面")
    @DeleteMapping("/{moduleId}/pages/{pageId}")
    public Result<Object> removePageFromModule(@PathVariable Integer moduleId, @PathVariable Integer pageId,
                                               @RequestParam(name = "confirm", required = false, defaultValue = "false") boolean confirm,
                                               HttpServletRequest request) {
        try {
            log.info("从模块移除页面 moduleId={} pageId={} confirm={}", moduleId, pageId, confirm);
            java.util.Map<String, Object> result = permissionService.removePageFromModule(moduleId, pageId, confirm);
            logRecordService.recordOperationLog("权限管理", "移除页面", "moduleId=" + moduleId + " pageId=" + pageId, null, request.getRemoteAddr(), true, null);
            return Result.success(confirm ? "移除成功" : "请确认删除", result);
        } catch (BusinessException e) {
            log.warn("从模块移除页面业务异常 moduleId={} pageId={} {}", moduleId, pageId, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("从模块移除页面失败 moduleId={} pageId={}", moduleId, pageId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "移除失败：" + e.getMessage());
        }
    }

    @ApiOperation("批量更新模块页面关联关系")
    @PostMapping("/{moduleId}/pages/batch-update")
    public Result<Void> batchUpdateModulePageAssociations(@PathVariable Integer moduleId,
                                                          @RequestBody java.util.Map<String, java.util.List<String>> req,
                                                          HttpServletRequest request) {
        try {
            java.util.List<String> addedIds = req.get("addedIds");
            java.util.List<String> removedIds = req.get("removedIds");

            if (addedIds == null) addedIds = new java.util.ArrayList<>();
            if (removedIds == null) removedIds = new java.util.ArrayList<>();

            log.info("批量更新模块页面关联 moduleId={} addedCount={} removedCount={}",
                     moduleId, addedIds.size(), removedIds.size());

            permissionService.batchUpdateModulePageAssociations(moduleId, addedIds, removedIds);

            logRecordService.recordOperationLog("权限管理", "批量更新模块页面关联",
                "moduleId=" + moduleId + " added=" + addedIds.size() + " removed=" + removedIds.size(),
                null, request.getRemoteAddr(), true, null);

            return Result.success("批量更新成功", null);
        } catch (BusinessException e) {
            log.warn("批量更新模块页面关联业务异常 moduleId={} {}", moduleId, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量更新模块页面关联失败 moduleId={}", moduleId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量更新失败：" + e.getMessage());
        }
    }

    @ApiOperation("批量更新页面字段关联关系")
    @PostMapping("/{pageId}/fields/batch-update")
    public Result<Void> batchUpdatePageFieldAssociations(@PathVariable Integer pageId,
                                                         @RequestBody java.util.Map<String, java.util.List<String>> req,
                                                         HttpServletRequest request) {
        try {
            java.util.List<String> addedIds = req.get("addedIds");
            java.util.List<String> removedIds = req.get("removedIds");

            if (addedIds == null) addedIds = new java.util.ArrayList<>();
            if (removedIds == null) removedIds = new java.util.ArrayList<>();

            log.info("批量更新页面字段关联 pageId={} addedIds={} removedIds={}",
                     pageId, addedIds, removedIds);
            log.info("批量更新页面字段关联 pageId={} addedCount={} removedCount={}",
                     pageId, addedIds.size(), removedIds.size());

            permissionService.batchUpdatePageFieldAssociations(pageId, addedIds, removedIds);

            logRecordService.recordOperationLog("权限管理", "批量更新页面字段关联",
                "pageId=" + pageId + " added=" + addedIds.size() + " removed=" + removedIds.size(),
                null, request.getRemoteAddr(), true, null);

            return Result.success("批量更新成功", null);
        } catch (BusinessException e) {
            log.warn("批量更新页面字段关联业务异常 pageId={} {}", pageId, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量更新页面字段关联失败 pageId={}", pageId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量更新失败：" + e.getMessage());
        }
    }

    @ApiOperation("批量删除权限")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody PermissionBatchDeleteRequest req, HttpServletRequest request) {
        try {
            if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要删除的权限");
            }
            log.info("批量删除权限 count={}", req.getIds().size());
            permissionService.batchDelete(req.getIds());
            logRecordService.recordOperationLog("权限管理", "批量删除权限", "count=" + req.getIds().size(), null, request.getRemoteAddr(), true, null);
            return Result.success("批量删除成功，共删除" + req.getIds().size() + "个权限", null);
        } catch (BusinessException e) {
            log.warn("批量删除权限业务异常：{}", e.getMessage());
            logRecordService.recordOperationLog("权限管理", "批量删除权限", null, null, request.getRemoteAddr(), false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量删除权限失败", e);
            logRecordService.recordOperationLog("权限管理", "批量删除权限", null, null, request.getRemoteAddr(), false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量删除失败：" + e.getMessage());
        }
    }
}


