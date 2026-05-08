package com.erp.controller.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.controller.customer.dto.*;
import com.erp.service.customer.SupplierService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.common.util.SecurityUtil;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 供应商管理控制器
 * 支持供应商的增删改查、导入导出、分页查询等功能
 */
@RestController
@RequestMapping("/supplier")
@Api(tags = "供应商管理")
@Slf4j
@Validated
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 供应商档案主列表查询
     *
     * @param request 查询条件
     */
    @RequirePagePermission("档案管理:供应商档案:页面")
    @PostMapping("/list")
    @ApiOperation("分页查询供应商列表")
    public Result<IPage<SupplierPageResponse>> getSupplierPage(
            @RequestBody @Valid SupplierPageRequest request) {
        try {
            log.info("分页查询供应商列表，参数：{}", request);
            IPage<SupplierPageResponse> result = supplierService.getSupplierPage(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询供应商列表失败，参数：{}，错误：{}", request, e.getMessage(), e);
            return Result.error(ResultCodeEnum.ERROR.getCode(), "查询供应商列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取供应商详情
     */
    @RequireActionPermission("档案管理:供应商档案:查看")
    @GetMapping("/{supplierId}")
    @ApiOperation("获取供应商详情")
    public Result<SupplierDetailResponse> getSupplierDetail(@PathVariable Integer supplierId) {
        try {
            log.info("获取供应商详情，供应商ID：{}", supplierId);
            SupplierDetailResponse result = supplierService.getSupplierDetail(supplierId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取供应商详情失败，供应商ID：{}，错误：{}", supplierId, e.getMessage(), e);
            return Result.error(ResultCodeEnum.ERROR.getCode(), "获取供应商详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建供应商（支持单个和批量创建）
     */
    @RequireActionPermission("档案管理:供应商档案:新增")
    @PostMapping
    @ApiOperation("创建供应商")
    public Result<SupplierBatchResponse> createSupplier(@RequestBody @Valid SupplierCreateRequest request,
                                                         HttpServletRequest httpRequest) {
        try {
            log.info("创建供应商，参数：{}", request);
            SupplierBatchResponse result = supplierService.createSupplier(request);
            logRecordService.recordOperationLog("供应商管理", "新增",
                    "创建供应商 - 成功创建" + result.getSuccessCount() + "个供应商",
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "SUPPLIER_CREATE",
                        null,
                        String.format("成功创建%d个供应商", result.getSuccessCount()),
                        "新增",
                        SecurityUtil.getCurrentUserId()
                );
            } catch (Exception msgEx) {
                log.warn("发送创建供应商通知失败", msgEx);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建供应商失败，参数：{}，错误：{}", request, e.getMessage(), e);
            logRecordService.recordOperationLog("供应商管理", "新增",
                    "创建供应商失败：" + e.getMessage(),
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "创建供应商失败：" + e.getMessage());
        }
    }

    /**
     * 更新供应商（支持单个和批量更新）
     */
    @RequireActionPermission("档案管理:供应商档案:编辑")
    @PutMapping("/batch-update")
    @ApiOperation("批量更新供应商")
    public Result<SupplierBatchResponse> updateSupplier(@RequestBody @Valid SupplierUpdateRequest request,
                                                         HttpServletRequest httpRequest) {
        try {
            log.info("批量更新供应商，参数：{}", request);
            SupplierBatchResponse result = supplierService.updateSupplier(request);
            logRecordService.recordOperationLog("供应商管理", "更新",
                    "批量更新供应商 - 总数:" + (result.getTotalCount() != null ? result.getTotalCount() : 0) + " - 成功:" + (result.getSuccessCount() != null ? result.getSuccessCount() : 0),
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "SUPPLIER_UPDATE",
                        null,
                        String.format("批量更新供应商：总数=%d，成功=%d",
                                result.getTotalCount() != null ? result.getTotalCount() : 0,
                                result.getSuccessCount() != null ? result.getSuccessCount() : 0),
                        "更新",
                        SecurityUtil.getCurrentUserId()
                );
            } catch (Exception msgEx) {
                log.warn("发送批量更新供应商通知失败", msgEx);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量更新供应商失败，参数：{}，错误：{}", request, e.getMessage(), e);
            logRecordService.recordOperationLog("供应商管理", "更新",
                    "批量更新供应商失败：" + e.getMessage(),
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "批量更新供应商失败：" + e.getMessage());
        }
    }

    /**
     * 删除供应商（支持单个和批量删除）
     */
    @DeleteMapping("/{supplierId}")
    @ApiOperation("删除供应商")
    public Result<SupplierBatchResponse> deleteSupplier(@PathVariable Integer supplierId,
                                                        @RequestParam(required = false, defaultValue = "true") Boolean softDelete,
                                                        HttpServletRequest httpRequest) {
        try {
            log.info("删除供应商，供应商ID：{}，软删除：{}", supplierId, softDelete);
            SupplierDeleteRequest request = new SupplierDeleteRequest();
            request.setSupplierId(supplierId);
            request.setSoftDelete(softDelete);
            SupplierBatchResponse result = supplierService.deleteSupplier(request);
            logRecordService.recordOperationLog("供应商管理", "删除",
                    "删除供应商 - ID:" + supplierId + " - 成功删除供应商",
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "SUPPLIER_DELETE",
                        supplierId,
                        String.format("供应商已删除：supplierId=%d", supplierId),
                        "删除",
                        SecurityUtil.getCurrentUserId()
                );
            } catch (Exception msgEx) {
                log.warn("发送删除供应商通知失败", msgEx);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("删除供应商失败，供应商ID：{}，错误：{}", supplierId, e.getMessage(), e);
            logRecordService.recordOperationLog("供应商管理", "删除",
                    "删除供应商失败：" + e.getMessage(),
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "删除供应商失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除供应商
     */
    @DeleteMapping("/batch")
    @ApiOperation("批量删除供应商")
    public Result<SupplierBatchResponse> batchDeleteSuppliers(@RequestBody @Valid SupplierDeleteRequest request,
                                                               HttpServletRequest httpRequest) {
        try {
            log.info("批量删除供应商，参数：{}", request);
            SupplierBatchResponse result = supplierService.deleteSupplier(request);
            logRecordService.recordOperationLog("供应商管理", "批量删除",
                    "批量删除供应商 - 成功删除" + result.getSuccessCount() + "个供应商",
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "SUPPLIER_DELETE",
                        null,
                        String.format("供应商已批量删除：成功删除%d个供应商", result.getSuccessCount()),
                        "删除",
                        SecurityUtil.getCurrentUserId()
                );
            } catch (Exception msgEx) {
                log.warn("发送批量删除供应商通知失败", msgEx);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量删除供应商失败，参数：{}，错误：{}", request, e.getMessage(), e);
            logRecordService.recordOperationLog("供应商管理", "批量删除",
                    "批量删除供应商失败：" + e.getMessage(),
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "批量删除供应商失败：" + e.getMessage());
        }
    }

    /**
     * 导入供应商数据
     */
    @RequireActionPermission("档案管理:供应商档案:批量导入")
    @PostMapping("/import")
    @ApiOperation("导入供应商数据")
    public Result<SupplierImportResponse> importSuppliers(@RequestParam("file") MultipartFile file,
                                                          HttpServletRequest httpRequest) {
        try {
            log.info("导入供应商数据，文件名：{}", file.getOriginalFilename());
            SupplierImportResponse result = supplierService.importSuppliers(file);
            logRecordService.recordOperationLog("供应商管理", "导入",
                    "导入供应商数据 - 成功导入" + result.getSuccessCount() + "条数据",
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), true, null);
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入供应商数据失败，文件名：{}，错误：{}", file.getOriginalFilename(), e.getMessage(), e);
            logRecordService.recordOperationLog("供应商管理", "导入",
                    "导入供应商数据失败：" + e.getMessage(),
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "导入供应商数据失败：" + e.getMessage());
        }
    }

    /**
     * 导出供应商数据
     * 权限说明：导出属于只读操作，只需具备页面查看权限（canView=1）即可，无需 canEdit=1。
     * 使用页面级权限注解而非动作级，确保后端强制校验生效（ActionPermissionAspect 已禁用）。
     */
    @RequirePagePermission("档案管理:供应商档案:页面")
    @GetMapping("/export")
    @ApiOperation("导出供应商数据")
    public void exportSuppliers(SupplierPageRequest request, HttpServletResponse response,
                                 HttpServletRequest httpRequest) {
        try {
            log.info("导出供应商数据，参数：{}", request);

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("供应商数据_" +
                    java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx",
                    "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            supplierService.exportSuppliers(request, response);

            logRecordService.recordOperationLog("供应商管理", "导出",
                    "导出供应商数据 - 成功导出供应商数据",
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), true, null);

        } catch (Exception e) {
            log.error("导出供应商数据失败，参数：{}，错误：{}", request, e.getMessage(), e);
            logRecordService.recordOperationLog("供应商管理", "导出",
                    "导出供应商数据失败：" + e.getMessage(),
                    SecurityUtil.getCurrentUserId(), logRecordService.getClientIp(httpRequest), false, e.getMessage());
            throw new BusinessException("导出供应商数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取供应商统计信息
     */
    @GetMapping("/stats")
    @ApiOperation("获取供应商统计信息")
    public Result<SupplierStatisticsResponse> getSupplierStatistics() {
        try {
            log.info("获取供应商统计信息");
            SupplierStatisticsResponse result = supplierService.getSupplierStatistics();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取供应商统计信息失败，错误：{}", e.getMessage(), e);
            return Result.error(ResultCodeEnum.ERROR.getCode(), "获取供应商统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 供应商下拉列表查询
     * 专门为下拉选择场景设计的轻量接口，仅返回下拉所需字段
     */
    @GetMapping("/select/list")
    @ApiOperation("供应商下拉列表查询")
    public Result<List<SupplierSelectResponse>> getSupplierSelectList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            log.info("查询供应商下拉列表，关键词：{}，状态：{}", keyword, status);
            List<SupplierSelectResponse> result = supplierService.getSupplierSelectList(keyword, status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询供应商下拉列表失败，关键词：{}，错误：{}", keyword, e.getMessage(), e);
            return Result.error(ResultCodeEnum.ERROR.getCode(), "查询供应商下拉列表失败：" + e.getMessage());
        }
    }

    /**
     * 下载供应商导入模板
     */
    @GetMapping("/import/template")
    @ApiOperation("下载供应商导入模板")
    public void downloadSupplierImportTemplate(HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            log.info("下载供应商导入模板");

            Sheet sheet = workbook.createSheet("供应商导入模板");

            // 与 SupplierServiceImpl.parseExcelRow 中列顺序保持一致
            String[] headers = {
                    "企业名称",
                    "统一社会信用代码",
                    "地址",
                    "电话",
                    "法定代表人",
                    "联系人",
                    "联系电话",
                    "曾用名",
                    "账户名称",
                    "账户号码",
                    "开户银行",
                    "备注"
            };

            // 创建表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);

                // 设置列宽
                if (i == 2 || i == 3 || i == 8 || i == 12) { // 统一社会信用代码、地址、曾用名、备注列
                    sheet.setColumnWidth(i, 25 * 256);
                } else {
                    sheet.setColumnWidth(i, 18 * 256);
                }
            }

            // 设置响应头
            String fileName = URLEncoder.encode("供应商导入模板.xlsx", StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            // 写入响应流
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            logRecordService.recordOperationLog("supplier", "download_template",
                    "下载供应商导入模板",
                    SecurityUtil.getCurrentUserId(), null, true, null);

        } catch (Exception e) {
            log.error("下载供应商导入模板失败", e);
            throw new BusinessException("下载模板失败：" + e.getMessage());
        }
    }
}
