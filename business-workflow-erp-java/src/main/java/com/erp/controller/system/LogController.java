package com.erp.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.system.dto.LogDetailResponse;
import com.erp.controller.system.dto.LogExportRequest;
import com.erp.controller.system.dto.LogPageRequest;
import com.erp.controller.system.dto.LogPageResponse;
import com.erp.common.util.SecurityUtil;
import com.erp.service.system.LogService;
import com.erp.service.system.ILogRecordService;
import com.erp.common.annotation.RequirePagePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志管理控制器
 * 
 * @author ERP System
 * @date 2025-12-08
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/log")
@Api(tags = "日志管理")
public class LogController {

    @Autowired
    private LogService logService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 分页查询全部日志
     */
    @RequirePagePermission("系统管理:系统日志:页面")
    @PostMapping("/page/all")
    @ApiOperation(value = "分页查询全部日志", notes = "支持按关键字、类型、状态、模块、IP、时间范围筛选")
    public Result<IPage<LogPageResponse>> getLogPage(@Valid @RequestBody LogPageRequest request) {
        try {
            IPage<LogPageResponse> page = logService.getLogPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询全部日志失败", e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询操作日志
     */
    @RequirePagePermission("系统管理:系统日志:页面")
    @PostMapping("/page/operation")
    @ApiOperation(value = "分页查询操作日志", notes = "支持按关键字、模块、操作类型、IP、时间范围筛选")
    public Result<IPage<LogPageResponse>> getOperationLogPage(@Valid @RequestBody LogPageRequest request) {
        try {
            IPage<LogPageResponse> page = logService.getOperationLogPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询操作日志失败", e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询数据变更日志
     */
    @RequirePagePermission("系统管理:系统日志:页面")
    @PostMapping("/page/data-change")
    @ApiOperation(value = "分页查询数据变更日志", notes = "支持按关键字、数据表、记录ID、变更类型、时间范围筛选")
    public Result<IPage<LogPageResponse>> getDataChangeLogPage(@Valid @RequestBody LogPageRequest request) {
        try {
            IPage<LogPageResponse> page = logService.getDataChangeLogPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询数据变更日志失败", e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询登录日志
     */
    @RequirePagePermission("系统管理:系统日志:页面")
    @PostMapping("/page/login")
    @ApiOperation(value = "分页查询登录日志", notes = "支持按关键字、登录结果、IP、时间范围筛选")
    public Result<IPage<LogPageResponse>> getLoginLogPage(@Valid @RequestBody LogPageRequest request) {
        try {
            IPage<LogPageResponse> page = logService.getLoginLogPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询登录日志失败", e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取日志详情
     */
    @GetMapping("/{logId}")
    @ApiOperation(value = "获取日志详情", notes = "根据日志编号获取日志详情，包括字段差异对比")
    public Result<LogDetailResponse> getLogDetail(@PathVariable Integer logId) {
        try {
            LogDetailResponse response = logService.getLogDetail(logId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询日志详情失败: logId={}", logId, e);
            return Result.error(500, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 导出日志
     */
    @PostMapping("/export")
    @ApiOperation(value = "导出日志", notes = "支持导出全部或选中的日志，返回Excel文件")
    public void exportLogs(@Valid @RequestBody LogExportRequest request,
                            HttpServletRequest httpRequest,
                            HttpServletResponse response) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            List<LogPageResponse> logs = logService.exportLogs(request);
            // 写入Excel文件
            writeExport(logs, response);
            // 记录导出操作日志
            String content = String.format("导出日志：模式=%s，共%d条记录", 
                    request.getMode(), logs.size());
            logRecordService.recordOperationLog("系统管理", "导出", content, userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("系统管理", "导出", 
                    "导出日志失败", userId, ipAddress, false, e.getMessage());
            log.error("导出日志失败", e);
            throw e;
        } catch (Exception e) {
            log.error("导出日志失败", e);
            logRecordService.recordOperationLog("系统管理", "导出", 
                    "导出日志失败", userId, ipAddress, false, e.getMessage());
            throw new BusinessException(500, "导出失败：" + e.getMessage());
        }
    }

    /**
     * 写入导出Excel
     */
    private void writeExport(List<LogPageResponse> data, HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("系统日志");
            String[] headers = {
                "日志编号", "类型", "模块/表", "操作内容", "操作人", 
                "操作时间", "IP地址", "状态", "操作类型", "登录结果"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < data.size(); i++) {
                LogPageResponse item = data.get(i);
                Row row = sheet.createRow(i + 1);
                int col = 0;
                
                row.createCell(col++).setCellValue(item.getCode() == null ? "" : item.getCode());
                row.createCell(col++).setCellValue(item.getType() == null ? "" : item.getType());
                row.createCell(col++).setCellValue(item.getModule() == null ? "" : item.getModule());
                row.createCell(col++).setCellValue(item.getAction() == null ? "" : item.getAction());
                row.createCell(col++).setCellValue(item.getOperator() == null ? "" : item.getOperator());
                row.createCell(col++).setCellValue(
                    item.getTime() == null ? "" : formatter.format(item.getTime()));
                row.createCell(col++).setCellValue(item.getIp() == null ? "" : item.getIp());
                row.createCell(col++).setCellValue(
                    item.getStatus() == null ? "" : 
                    ("success".equals(item.getStatus()) ? "成功" : 
                     "failed".equals(item.getStatus()) ? "失败" : "未知"));
                row.createCell(col++).setCellValue(item.getOperationType() == null ? "" : item.getOperationType());
                row.createCell(col).setCellValue(item.getResult() == null ? "" : item.getResult());
            }
            
            String fileName = URLEncoder.encode("系统日志导出.xlsx", StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        }
    }
}

