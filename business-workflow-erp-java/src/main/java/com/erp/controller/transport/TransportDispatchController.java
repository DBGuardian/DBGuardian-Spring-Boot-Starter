package com.erp.controller.transport;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.controller.transport.dto.DispatchValidateResponse;
import com.erp.controller.transport.dto.TransportDispatchDetailRequest;
import com.erp.controller.transport.dto.TransportDispatchDetailResponse;
import com.erp.controller.transport.dto.TransportDispatchListResponse;
import com.erp.controller.transport.dto.TransportDispatchPageRequest;
import com.erp.controller.transport.dto.TransportDispatchPageResponse;
import com.erp.service.system.ILogRecordService;
import com.erp.service.transport.DispatchOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 运输单管理
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/transport/dispatch")
@Api(tags = "运输单管理")
public class TransportDispatchController {

    @Autowired
    private DispatchOrderService dispatchOrderService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 创建运输单
     */
    @RequireActionPermission("运输管理:车辆安排:生成运输单")
    @PostMapping("/create")
    @ApiOperation(value = "创建运输单", notes = "一张收运通知单仅可生成一张运输单")
    public Result<TransportDispatchDetailResponse> createDispatchOrder(@Valid @RequestBody TransportDispatchDetailRequest request,
                                                                        HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            TransportDispatchDetailResponse resp = dispatchOrderService.createDispatchOrder(request);
            // 记录操作日志
            logRecordService.recordOperationLog("运输单管理", "新增",
                    "创建运输单：运输单号=" + resp.getDispatchCode(), userId, ipAddress, true, null);
            return Result.success("创建成功", resp);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输单管理", "新增",
                    "创建运输单失败：收运通知单号=" + request.getNoticeCode(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("创建运输单失败", e);
            logRecordService.recordOperationLog("运输单管理", "新增",
                    "创建运输单失败：收运通知单号=" + request.getNoticeCode(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "创建运输单失败：" + e.getMessage());
        }
    }

    /**
     * 更新运输单
     */
    @RequireActionPermission("运输管理:运输执行:编辑")
    @PostMapping("/update")
    @ApiOperation(value = "更新运输单", notes = "仅未完成、未锁定的运输单可修改")
    public Result<TransportDispatchDetailResponse> updateDispatchOrder(@Valid @RequestBody TransportDispatchDetailRequest request,
                                                                       HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            TransportDispatchDetailResponse resp = dispatchOrderService.updateDispatchOrder(request);
            // 记录操作日志
            logRecordService.recordOperationLog("运输单管理", "编辑",
                    "更新运输单：运输单号=" + request.getDispatchCode(), userId, ipAddress, true, null);
            return Result.success("更新成功", resp);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输单管理", "编辑",
                    "更新运输单失败：运输单号=" + request.getDispatchCode(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新运输单失败", e);
            logRecordService.recordOperationLog("运输单管理", "编辑",
                    "更新运输单失败：运输单号=" + request.getDispatchCode(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新运输单失败：" + e.getMessage());
        }
    }

    /**
     * 获取运输单详情
     */
    @RequireActionPermission("运输管理:运输执行:查看")
    @GetMapping("/detail")
    @ApiOperation(value = "获取运输单详情", notes = "dispatchCode 和 noticeCode 至少传一个")
    public Result<TransportDispatchDetailResponse> getDispatchDetail(@RequestParam(required = false) String dispatchCode,
                                                                     @RequestParam(required = false) String noticeCode) {
        try {
            TransportDispatchDetailResponse resp = dispatchOrderService.getDispatchDetail(dispatchCode, noticeCode);
            return Result.success("查询成功", resp);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询运输单详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 校验运输单风险
     */
    @GetMapping("/validate")
    @ApiOperation(value = "校验运输单", notes = "校验合同缺失、超限等风险")
    public Result<DispatchValidateResponse> validate(@RequestParam(required = false) String noticeCode,
                                                     @RequestParam(required = false) String dispatchCode) {
        try {
            DispatchValidateResponse resp = dispatchOrderService.validateDispatchOrder(noticeCode, dispatchCode);
            return Result.success("校验成功", resp);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("校验运输单失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "校验失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询运输单列表
     */
    @RequirePagePermission("运输管理:运输执行:页面")
    @GetMapping("/list")
    @ApiOperation(value = "分页查询运输单列表", notes = "支持多条件查询和排序")
    public Result<TransportDispatchListResponse> getDispatchOrderList(
            @Valid TransportDispatchPageRequest request) {
        try {
            TransportDispatchListResponse resp = dispatchOrderService.getDispatchOrderList(request);
            return Result.success("查询成功", resp);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询运输单列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 打印运输单
     */
    @GetMapping("/print")
    @ApiOperation(value = "打印运输单", notes = "生成运输单PDF并返回打印URL，需要dispatch:print权限")
    // TODO: 添加权限校验注解，例如：@PreAuthorize("hasPermission('dispatch:print')")
    // 注意：如果项目使用了Spring Security，需要添加@PreAuthorize注解
    // 如果项目使用自定义权限系统，需要在方法开始处添加权限检查
    @RequireActionPermission("运输管理:运输执行:打印")
    public Result<String> printDispatchOrder(@RequestParam String dispatchCode, HttpServletRequest httpRequest) {
        // 权限校验：检查用户是否有打印运输单的权限
        // 注意：这里应该调用权限服务进行校验，例如：
        // if (!permissionService.hasPermission(SecurityUtil.getCurrentUserId(), "dispatch:print")) {
        //     return Result.error(ResultCodeEnum.FORBIDDEN.getCode(), "无权限执行此操作");
        // }
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            String printUrl = dispatchOrderService.generateDispatchOrderPdf(dispatchCode);
            // 记录操作日志
            logRecordService.recordOperationLog("运输单管理", "打印",
                    "打印运输单：运输单号=" + dispatchCode, userId, ipAddress, true, null);
            return Result.success("PDF生成成功", printUrl);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输单管理", "打印",
                    "打印运输单失败：运输单号=" + dispatchCode, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("打印运输单失败", e);
            logRecordService.recordOperationLog("运输单管理", "打印",
                    "打印运输单失败：运输单号=" + dispatchCode, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "打印失败：" + e.getMessage());
        }
    }

    /**
     * 批量打印运输单
     */
    @RequireActionPermission("运输管理:运输执行:批量打印")
    @PostMapping("/batch-print")
    @ApiOperation(value = "批量打印运输单", notes = "批量生成运输单PDF并返回打印URL")
    public Result<String> batchPrintDispatchOrder(@RequestBody List<String> dispatchCodes, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            String printUrl = dispatchOrderService.generateBatchDispatchOrderPdf(dispatchCodes);
            // 记录操作日志
            logRecordService.recordOperationLog("运输单管理", "批量打印",
                    "批量打印运输单：数量=" + dispatchCodes.size() + ", 运输单号=" + String.join(",", dispatchCodes), 
                    userId, ipAddress, true, null);
            return Result.success("批量PDF生成成功", printUrl);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输单管理", "批量打印",
                    "批量打印运输单失败：数量=" + dispatchCodes.size(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量打印运输单失败", e);
            logRecordService.recordOperationLog("运输单管理", "批量打印",
                    "批量打印运输单失败：数量=" + dispatchCodes.size(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量打印失败：" + e.getMessage());
        }
    }

    /**
     * 导出运输执行Excel
     */
    @RequireActionPermission("运输管理:运输执行:导出")
    @GetMapping("/export")
    @ApiOperation(value = "导出运输执行", notes = "根据筛选条件导出运输执行Excel文件")
    public void exportDispatchOrders(TransportDispatchPageRequest request, HttpServletResponse response,
                                     HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 导出接口不需要分页参数，使用默认分页参数
            List<TransportDispatchPageResponse> list = dispatchOrderService.listDispatchOrdersForExport(request);
            writeExport(list, response);
            // 记录导出操作日志
            logRecordService.recordOperationLog("运输单管理", "导出",
                    "导出运输执行列表，共" + list.size() + "条记录", userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输单管理", "导出",
                    "导出运输执行列表失败", userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出运输执行失败", e);
            logRecordService.recordOperationLog("运输单管理", "导出",
                    "导出运输执行列表失败", userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    /**
     * 写入导出Excel
     */
    private void writeExport(List<TransportDispatchPageResponse> data, HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建支持中文的字体
            Font font = workbook.createFont();
            font.setFontName("宋体");
            font.setCharSet(Font.DEFAULT_CHARSET);
            
            // 创建单元格样式
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);
            
            // 创建表头样式（加粗）
            Font headerFont = workbook.createFont();
            headerFont.setFontName("宋体");
            headerFont.setCharSet(Font.DEFAULT_CHARSET);
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            
            Sheet sheet = workbook.createSheet("运输执行");
            String[] headers = {
                    "运输单号", "收运通知单号", "承运单位", "承运单位电话",
                    "驾驶员姓名", "驾驶员电话", "车辆号牌", "运输起点", "运输终点",
                    "派车时间", "实际起运时间", "实际到达时间", "计划转移数量(吨)",
                    "状态"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            for (int i = 0; i < data.size(); i++) {
                TransportDispatchPageResponse item = data.get(i);
                Row row = sheet.createRow(i + 1);
                int col = 0;
                Cell cell;
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getDispatchCode() == null ? "" : item.getDispatchCode());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getNoticeCode() == null ? "" : item.getNoticeCode());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getCarrierName() == null ? "" : item.getCarrierName());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getCarrierPhone() == null ? "" : item.getCarrierPhone());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getDriverName() == null ? "" : item.getDriverName());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getDriverPhone() == null ? "" : item.getDriverPhone());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getPlateNo() == null ? "" : item.getPlateNo());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getStartPoint() == null ? "" : item.getStartPoint());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getEndPoint() == null ? "" : item.getEndPoint());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getDispatchAt() == null ? "" : item.getDispatchAt());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getDepartAt() == null ? "" : item.getDepartAt());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getArriveAt() == null ? "" : item.getArriveAt());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getPlanQuantityTon() == null ? "" : item.getPlanQuantityTon().toString());
                cell.setCellStyle(cellStyle);
                
                cell = row.createCell(col++);
                cell.setCellValue(item.getStatus() == null ? "" : item.getStatus());
                cell.setCellStyle(cellStyle);
            }
            String fileName = URLEncoder.encode("运输执行导出.xlsx", StandardCharsets.UTF_8.name());
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


