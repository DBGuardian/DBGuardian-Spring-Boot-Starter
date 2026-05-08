package com.erp.controller.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.contract.dto.SalespersonCreateRequest;
import com.erp.controller.contract.dto.SalespersonDetailResponse;
import com.erp.controller.contract.dto.SalespersonPageRequest;
import com.erp.controller.contract.dto.SalespersonPageResponse;
import com.erp.controller.contract.dto.SalespersonSelectResponse;
import com.erp.service.contract.SalespersonService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 业务员档案控制器
 */
@Slf4j
@RestController
@RequestMapping("/salesperson")
@Api(tags = "业务员档案管理")
public class SalespersonController {

    @Autowired
    private SalespersonService salespersonService;

    @Autowired
    private ILogRecordService logRecordService;

    @RequirePagePermission("合同管理:业务合作合同:业务员信息:页面")
    @GetMapping("/list")
    @ApiOperation(value = "业务员分页查询", notes = "支持viewScope数据范围过滤")
    public Result<IPage<SalespersonPageResponse>> getPage(SalespersonPageRequest request) {
        try {
            // 注入当前员工ID用于后端viewScope数据范围控制
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            request.setCreatorFilter(currentUserId);
            return Result.success("查询成功", salespersonService.getPage(request));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询业务员列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/select")
    @ApiOperation("获取业务员下拉列表（支持关键词模糊搜索）")
    public Result<List<SalespersonSelectResponse>> getSelectList(
            @ApiParam(value = "搜索关键词") @RequestParam(required = false) String keyword) {
        try {
            return Result.success("查询成功", salespersonService.getSelectList(keyword));
        } catch (Exception e) {
            log.error("获取业务员下拉列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/{salespersonId}")
    @ApiOperation(value = "查询业务员详情", notes = "支持viewScope数据范围校验")
    public Result<SalespersonDetailResponse> getDetail(@PathVariable Integer salespersonId) {
        try {
            return Result.success("查询成功", salespersonService.getDetail(salespersonId));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询业务员详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @RequirePagePermission("合同管理:业务合作合同:业务员信息:页面")
    @PostMapping
    @ApiOperation("新增业务员")
    public Result<java.util.Map<String, Object>> create(@RequestBody SalespersonCreateRequest request,
                                                         HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            Integer id = salespersonService.create(request);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("salespersonId", id);
            logRecordService.recordOperationLog("业务员档案", "新增",
                    "新增业务员，姓名=" + request.getSalespersonName() + "，甲方=" + request.getPartyAName(),
                    userId, ipAddress, true, null);
            return Result.success("新增成功", data);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务员档案", "新增",
                    "新增业务员失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增业务员失败", e);
            logRecordService.recordOperationLog("业务员档案", "新增",
                    "新增业务员失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增失败：" + e.getMessage());
        }
    }

    @RequirePagePermission("合同管理:业务合作合同:业务员信息:页面")
    @PostMapping("/{salespersonId}/update")
    @ApiOperation(value = "更新业务员", notes = "支持operateScope操作范围校验")
    public Result<Void> update(@PathVariable Integer salespersonId,
                               @RequestBody SalespersonCreateRequest request,
                               HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            salespersonService.update(salespersonId, request);
            logRecordService.recordOperationLog("业务员档案", "更新",
                    "更新业务员，ID=" + salespersonId + "，姓名=" + request.getSalespersonName(),
                    userId, ipAddress, true, null);
            return Result.success("更新成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务员档案", "更新",
                    "更新业务员失败，ID=" + salespersonId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新业务员失败", e);
            logRecordService.recordOperationLog("业务员档案", "更新",
                    "更新业务员失败，ID=" + salespersonId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新失败：" + e.getMessage());
        }
    }

    @RequirePagePermission("合同管理:业务合作合同:业务员信息:页面")
    @PostMapping("/{salespersonId}/delete")
    @ApiOperation(value = "删除业务员", notes = "支持operateScope操作范围校验")
    public Result<Void> delete(@PathVariable Integer salespersonId, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            salespersonService.delete(salespersonId);
            logRecordService.recordOperationLog("业务员档案", "删除",
                    "删除业务员，ID=" + salespersonId,
                    userId, ipAddress, true, null);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务员档案", "删除",
                    "删除业务员失败，ID=" + salespersonId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除业务员失败", e);
            logRecordService.recordOperationLog("业务员档案", "删除",
                    "删除业务员失败，ID=" + salespersonId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        }
    }

    @RequirePagePermission("合同管理:业务合作合同:业务员信息:页面")
    @PostMapping("/batch/delete")
    @ApiOperation(value = "批量删除业务员", notes = "支持operateScope操作范围校验")
    public Result<Void> batchDelete(@RequestBody java.util.Map<String, Object> request, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            @SuppressWarnings("unchecked")
            List<Integer> salespersonIds = (List<Integer>) request.get("salespersonIds");
            salespersonService.batchDelete(salespersonIds);
            logRecordService.recordOperationLog("业务员档案", "批量删除",
                    "批量删除业务员，共" + (salespersonIds != null ? salespersonIds.size() : 0) + "个",
                    userId, ipAddress, true, null);
            return Result.success("批量删除成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务员档案", "批量删除",
                    "批量删除业务员失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量删除业务员失败", e);
            logRecordService.recordOperationLog("业务员档案", "批量删除",
                    "批量删除业务员失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量删除失败：" + e.getMessage());
        }
    }
}
