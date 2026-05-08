package com.erp.controller.finance;

import com.erp.common.enums.ContractClosureStatus;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.*;
import com.erp.service.contract.IContractStatusPermissionService;
import com.erp.service.finance.BusinessClosureValidationService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.hutool.core.date.DateUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 业务闭环监控控制器
 *
 * 提供业务闭环校验、状态异常发现、权限检查、问题导出等完整功能
 *
 * API接口列表：
 * - GET /finance/closure/issues - 获取闭环问题列表
 * - GET /finance/closure/issues/export - 导出问题列表为Excel
 * - POST /finance/closure/validate - 执行全量业务闭环校验
 * - POST /finance/closure/validate/{contractId} - 校验指定合同的业务闭环
 * - POST /finance/closure/validate/status-consistency - 执行状态一致性校验
 * - POST /finance/closure/batch-validate - 批量校验多个合同
 * - GET /finance/closure/validation-results/{contractId} - 获取指定合同的校验结果
 * - POST /finance/closure/fix-validation - 修复校验异常
 * - POST /finance/closure/check-status-permission - 检查状态变更权限
 * - GET /finance/closure/dashboard/stats - 获取看板统计数据
 * - GET /finance/closure/dashboard/trends - 获取趋势数据
 * - GET /finance/closure/statistics - 获取校验统计数据
 *
 * @author ERP System
 * @date 2025-02-06
 */
@Slf4j
@RestController
@RequestMapping("/finance/closure")
@Api(tags = "业务闭环监控")
@Validated
public class BusinessClosureController {

    @Autowired
    private BusinessClosureValidationService closureValidationService;

    @Autowired
    private ILogRecordService logRecordService;


    /**
     * 执行全量业务闭环校验
     */
    @PostMapping("/validate")
    @ApiOperation(value = "执行全量业务闭环校验",
                  notes = "执行所有类型的闭环校验，包括时间顺序、金额一致性、数据关联、状态一致性等")
    public Result<ClosureValidationResponse> executeFullValidation(
            @Valid @RequestBody ClosureValidationRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean validateSuccess = false;
        String errorMessage = null;

        try {
            log.info("开始执行全量业务闭环校验，请求参数：{}", request);
            ClosureValidationResponse response = closureValidationService.executeFullValidation(request);
            validateSuccess = true;
            log.info("全量业务闭环校验完成，发现{}个问题", response.getIssuesFound());
            return Result.success("校验完成", response);
        } catch (Exception e) {
            log.error("全量业务闭环校验失败", e);
            errorMessage = e.getMessage();
            return Result.error("校验失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("执行全量业务闭环校验：合同ID=%s，校验类型=%s",
                        request.getContractId(), request.getValidateType());
                logRecordService.recordOperationLog("业务闭环监控", "校验",
                        logContent, userId, ipAddress, validateSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录全量校验操作日志失败", logEx);
            }
        }
    }


    /**
     * 获取闭环问题列表
     */
    @GetMapping("/issues")
    @ApiOperation(value = "获取闭环问题列表",
                  notes = "分页查询闭环校验发现的问题，支持多种筛选条件")
    public Result<ClosureIssuePageResponse> getClosureIssues(
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String contractCode,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Boolean forceRefresh,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {

        try {
            log.debug("查询闭环问题列表，筛选条件：issueType={}, riskLevel={}, businessType={}, contractCode={}, dateRange={}, forceRefresh={}, current={}, size={}",
                     issueType, riskLevel, businessType, contractCode, dateRange, forceRefresh, current, size);

            ClosureIssuePageResponse response = closureValidationService.getClosureIssues(
                    issueType, riskLevel, businessType, contractCode, dateRange, forceRefresh, current, size);

            return Result.success("查询成功", response);
        } catch (Exception e) {
            log.error("查询闭环问题列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }



    /**
     * 获取校验统计数据
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取校验统计数据",
                  notes = "获取业务闭环校验的统计信息，包括问题数量、解决率等")
    public Result<ClosureStatisticsResponse> getClosureStatistics(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long organizationId) {

        try {
            log.debug("获取闭环校验统计数据，时间范围：{}，组织ID：{}", dateRange, organizationId);

            // 这里应该实现具体的统计逻辑
            // 暂时返回空响应，实际需要调用统计服务
            ClosureStatisticsResponse response = new ClosureStatisticsResponse();
            response.setDateRange(dateRange);
            response.setOrganizationId(organizationId);

            return Result.success("获取统计数据成功", response);
        } catch (Exception e) {
            log.error("获取闭环校验统计数据失败", e);
            return Result.error("获取统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取看板统计数据
     */
    @GetMapping("/dashboard/stats")
    @ApiOperation(value = "获取看板统计数据",
                  notes = "获取业务闭环监控看板所需的统计指标数据")
    public Result<ClosureDashboardStats> getDashboardStats(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) Long organizationId) {

        try {
            log.debug("获取看板统计数据，时间范围：{}，组织ID：{}", dateRange, organizationId);

            ClosureDashboardStats stats = closureValidationService.getDashboardStats(dateRange, organizationId);
            return Result.success("获取看板数据成功", stats);
        } catch (Exception e) {
            log.error("获取看板统计数据失败", e);
            return Result.error("获取看板数据失败：" + e.getMessage());
        }
    }





    /**
     * 导出业务闭环问题列表
     */
    @GetMapping("/issues/export")
    @ApiOperation(value = "导出业务闭环问题列表",
                  notes = "导出业务闭环监控中发现的问题列表为Excel文件，支持按条件筛选")
    public void exportClosureIssues(
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String contractCode,
            @RequestParam(required = false) String dateRange,
            HttpServletRequest request,
            HttpServletResponse response) {

        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean exportSuccess = false;
        String errorMessage = null;

        try {
            log.info("导出业务闭环问题Excel，筛选条件：issueType={}, riskLevel={}, businessType={}, contractCode={}, dateRange={}",
                     issueType, riskLevel, businessType, contractCode, dateRange);

            // 调用服务导出数据
            byte[] excelData = closureValidationService.exportClosureIssues(
                    issueType, riskLevel, businessType, contractCode, dateRange);

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = "业务闭环问题报告_" + DateUtil.format(new java.util.Date(), "yyyyMMdd_HHmmss") + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8"));

            // 写入响应流
            response.getOutputStream().write(excelData);
            response.getOutputStream().flush();

            exportSuccess = true;
            log.info("业务闭环问题Excel导出成功，文件名：{}", fileName);

        } catch (Exception e) {
            log.error("导出业务闭环问题Excel失败", e);
            errorMessage = e.getMessage();
            try {
                response.setStatus(500);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("导出业务闭环问题：问题类型=%s，风险等级=%s，业务类型=%s，合同编码=%s",
                        issueType, riskLevel, businessType, contractCode);
                logRecordService.recordOperationLog("业务闭环监控", "导出",
                        logContent, userId, ipAddress, exportSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录导出闭环问题操作日志失败", logEx);
            }
        }
    }
}
