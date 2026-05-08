package com.erp.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigPageRequest;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigResponse;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigUpdateRequest;
import com.erp.controller.system.dto.HazardousWasteBatchDeleteRequest;
import com.erp.controller.system.dto.HazardousWasteCreateRequest;
import com.erp.controller.system.dto.HazardousWasteDetailResponse;
import com.erp.controller.system.dto.HazardousWasteImportResponse;
import com.erp.controller.system.dto.HazardousWastePageRequest;
import com.erp.controller.system.dto.HazardousWastePageResponse;
import com.erp.controller.system.dto.HazardousWasteUpdateRequest;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.service.system.HazardousWasteItemService;
import com.erp.service.system.HazardousWasteCategoryService;
import com.erp.service.system.ILogRecordService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 危险废物名录控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/waste-code")
@Api(tags = "危险废物名录管理")
public class WasteCodeController {

    @Autowired
    private HazardousWasteItemService hazardousWasteItemService;

    @Autowired
    private HazardousWasteCategoryService hazardousWasteCategoryService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 危险废物名录分页查询（基础字典类数据）
     *
     * 说明：
     * - 该接口会在多个业务页面被复用（例如：收运通知、客户报价等页面的危废条目下拉搜索）。
     * - 因此这里采用“页面权限白名单”方式允许跨页面复用，而不是强制要求用户必须拥有“危废条目表维护页”的页面权限。
     * - 同时兼容历史/别名页面编码（如：档案管理:危险废物名录:页面）。
     */
    @RequirePagePermission({
            // 档案管理（主维护入口）
            "档案管理:危废条目表:页面",
            // 兼容旧编码（前端路由仍在使用）
            "档案管理:危险废物名录:页面",
            // 系统管理（只读视角）
            "系统管理:危险废物名录:页面",
            // 业务页面复用（危废条目远程搜索/回填）
            "业务管理:收运通知:页面",
            "业务管理:客户报价:页面",
            // 合同管理（危废条目搜索/回填）
            "合同管理:危险废物合同:页面"
    })
    @GetMapping("/list")
    @ApiOperation(value = "分页查询危险废物名录", notes = "支持按关键字、危险特性、废物类别、行业来源筛选")
    public Result<IPage<HazardousWastePageResponse>> getWasteCodePage(
            @Valid HazardousWastePageRequest request) {
        try {
            IPage<HazardousWastePageResponse> page = hazardousWasteItemService.getWasteItemPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("分页查询危废名录失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/categories")
    @ApiOperation(value = "获取废物类别列表", notes = "获取所有可用的废物类别，用于下拉选项")
    public Result<List<String>> getWasteCategoryList() {
        try {
            List<String> categories = hazardousWasteItemService.getWasteCategoryList();
            return Result.success("查询成功", categories);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询废物类别列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @RequirePagePermission("档案管理:废物类别限额:页面")
    @GetMapping("/category/config")
    @ApiOperation(value = "分页查询废物类别限额配置", notes = "支持按废物类别筛选")
    public Result<IPage<HazardousWasteCategoryConfigResponse>> getWasteCategoryConfig(
            @Valid HazardousWasteCategoryConfigPageRequest request) {
        try {
            IPage<HazardousWasteCategoryConfigResponse> page =
                    hazardousWasteCategoryService.getCategoryConfigPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("分页查询废物类别配置失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @PutMapping("/category/config")
    @ApiOperation(value = "更新废物类别限额配置", notes = "只允许修改限额、开始时间、结束时间三个字段")
    public Result<Void> updateWasteCategoryConfig(@Validated @RequestBody HazardousWasteCategoryConfigUpdateRequest request,
                                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            hazardousWasteCategoryService.updateCategoryConfig(request);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "更新废物类别限额配置：" + request.getWasteCategory() + "，限额=" + request.getLimitAmount() + "吨", 
                    userId, ipAddress, true, null);
            return Result.success("更新成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "更新废物类别限额配置：" + request.getWasteCategory(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新废物类别配置失败", e);
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "更新废物类别限额配置：" + request.getWasteCategory(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新失败：" + e.getMessage());
        }
    }

    @GetMapping("/{itemId}")
    @ApiOperation(value = "危险废物名录详情", notes = "根据条目编号获取详情和引用统计")
    public Result<HazardousWasteDetailResponse> getWasteCodeDetail(@PathVariable Integer itemId) {
        try {
            HazardousWasteDetailResponse response = hazardousWasteItemService.getWasteItemDetail(itemId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询危废名录详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @RequireActionPermission("档案管理:危废条目表:新增")
    @PostMapping
    @ApiOperation(value = "新增危险废物名录", notes = "废物代码唯一")
    public Result<Void> createWasteCode(@Validated @RequestBody HazardousWasteCreateRequest request,
                                         HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            hazardousWasteItemService.createWasteItem(request);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("系统管理", "新增", 
                    "新增危险废物名录：" + request.getWasteCode() + " " + request.getWasteName(), 
                    userId, ipAddress, true, null);
            return Result.success("新增成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("系统管理", "新增", 
                    "新增危险废物名录：" + request.getWasteCode(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增危废名录失败", e);
            logRecordService.recordOperationLog("系统管理", "新增", 
                    "新增危险废物名录：" + request.getWasteCode(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增失败：" + e.getMessage());
        }
    }

    @RequireActionPermission("档案管理:危废条目表:编辑")
    @PutMapping("/{itemId}")
    @ApiOperation(value = "更新危险废物名录", notes = "更新基础字段并保持废物代码唯一")
    public Result<Void> updateWasteCode(@PathVariable Integer itemId,
                                        @Validated @RequestBody HazardousWasteUpdateRequest request,
                                        HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            hazardousWasteItemService.updateWasteItem(itemId, request);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "更新危险废物名录：ID=" + itemId + "，废物代码=" + request.getWasteCode(), 
                    userId, ipAddress, true, null);
            return Result.success("更新成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "更新危险废物名录：ID=" + itemId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新危废名录失败", e);
            logRecordService.recordOperationLog("系统管理", "编辑", 
                    "更新危险废物名录：ID=" + itemId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新失败：" + e.getMessage());
        }
    }

    @RequireActionPermission("档案管理:危废条目表:批量删除")
    @DeleteMapping("/{itemId}")
    @ApiOperation(value = "删除危险废物名录", notes = "删除前会校验业务引用数量")
    public Result<Void> deleteWasteCode(@PathVariable Integer itemId,
                                         HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 获取删除前的数据用于日志记录
            HazardousWasteDetailResponse oldData = hazardousWasteItemService.getWasteItemDetail(itemId);
            hazardousWasteItemService.deleteWasteItem(itemId);
            // 记录操作日志和数据变更日志
            logRecordService.recordOperationLog("系统管理", "删除",
                    "删除危险废物名录：ID=" + itemId + "，废物代码=" + (oldData != null ? oldData.getWasteCode() : ""),
                    userId, ipAddress, true, null);
            // 记录数据变更日志（删除操作）
            if (oldData != null) {
                logRecordService.recordDataChangeLog("系统管理", "HAZARDOUS_WASTE_ITEM",
                        String.valueOf(itemId), "删除",
                        "删除危险废物名录：" + oldData.getWasteCode(),
                        oldData, null, userId, ipAddress, true, null);
            }
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("系统管理", "删除",
                    "删除危险废物名录：ID=" + itemId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除危废名录失败", e);
            logRecordService.recordOperationLog("系统管理", "删除",
                    "删除危险废物名录：ID=" + itemId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        }
    }

    @RequireActionPermission("档案管理:危废条目表:批量删除")
    @PostMapping("/batch-delete")
    @ApiOperation(value = "批量删除危险废物名录", notes = "删除前会校验业务引用数量，使用事务批量删除减少数据库压力")
    public Result<Void> batchDeleteWasteCodes(@Validated @RequestBody HazardousWasteBatchDeleteRequest request,
                                               HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            List<Integer> ids = request.getIds();
            hazardousWasteItemService.batchDeleteWasteItems(ids);
            // 记录操作日志
            logRecordService.recordOperationLog("系统管理", "批量删除",
                    "批量删除危险废物名录：共" + ids.size() + "条记录，ID列表=" + ids,
                    userId, ipAddress, true, null);
            return Result.success("批量删除成功，共删除" + ids.size() + "条", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("系统管理", "批量删除",
                    "批量删除危险废物名录失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量删除危废名录失败", e);
            logRecordService.recordOperationLog("系统管理", "批量删除",
                    "批量删除危险废物名录失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量删除失败：" + e.getMessage());
        }
    }

    @RequireActionPermission("档案管理:危废条目表:批量导入")
    @PostMapping("/batch-import")
    @ApiOperation(value = "批量导入危险废物名录", notes = "支持Excel导入，表头为：废物类别、废物类别名称、行业来源、废物代码、危险废物、危险特性")
    public Result<HazardousWasteImportResponse> importWasteCodes(@RequestParam("file") MultipartFile file,
                                                                 HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            HazardousWasteImportResponse response = hazardousWasteItemService.importWasteItems(file);
            // 记录导入操作日志
            String content = String.format("批量导入危险废物名录：总记录数=%d，成功=%d，失败=%d", 
                    response.getTotalCount(), response.getSuccessCount(), response.getFailCount());
            logRecordService.recordOperationLog("系统管理", "导入", content, userId, ipAddress, true, null);
            return Result.success("导入完成", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("系统管理", "导入", 
                    "批量导入危险废物名录失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("导入危废名录失败", e);
            logRecordService.recordOperationLog("系统管理", "导入", 
                    "批量导入危险废物名录失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "导入失败：" + e.getMessage());
        }
    }

    @RequirePagePermission("档案管理:危废条目表:页面")
    @GetMapping("/export")
    @ApiOperation(value = "导出危险废物名录", notes = "根据筛选条件导出Excel，viewScope=SELF时只导出自己创建的数据")
    public void exportWasteCodes(@Valid HazardousWastePageRequest request, HttpServletResponse response,
                                  HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        List<HazardousWasteDetailResponse> list = hazardousWasteItemService.listForExport(request);
        try {
            writeExport(list, response);
            // 记录导出操作日志
            logRecordService.recordOperationLog("系统管理", "导出", 
                    "导出危险废物名录，共" + list.size() + "条记录", userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("系统管理", "导出", 
                    "导出危险废物名录失败", userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出危废名录失败", e);
            logRecordService.recordOperationLog("系统管理", "导出", 
                    "导出危险废物名录失败", userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    @GetMapping("/export-template")
    @ApiOperation(value = "下载导入模板", notes = "导入模板包含示例数据")
    public void downloadTemplate(HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导入模板");
            // 与 HazardousWasteItemServiceImpl.buildEntityFromRow 中列顺序保持一致
            String[] headers = {
                    "废物类别",
                    "废物类别名称",
                    "行业来源",
                    "废物代码",
                    "危险废物",
                    "危险特性"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("HW01");
            sampleRow.createCell(1).setCellValue("医疗废物 / 感染性废物");
            sampleRow.createCell(2).setCellValue("卫生");
            sampleRow.createCell(3).setCellValue("841-001-01");
            sampleRow.createCell(4).setCellValue("感染性废物");
            sampleRow.createCell(5).setCellValue("IN");
            writeWorkbookToResponse(workbook, response, "危险废物名录导入模板.xlsx");
        } catch (Exception e) {
            log.error("导出危废模板失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "下载模板失败：" + e.getMessage());
        }
    }

    private void writeExport(List<HazardousWasteDetailResponse> data, HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("危废名录");
            // 与导入模板保持一致：导出文件可直接导入
            String[] headers = {
                    "废物类别",
                    "废物类别名称",
                    "行业来源",
                    "废物代码",
                    "危险废物",
                    "危险特性"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            for (int i = 0; i < data.size(); i++) {
                HazardousWasteDetailResponse item = data.get(i);
                Row row = sheet.createRow(i + 1);
                int col = 0;
                row.createCell(col++).setCellValue(item.getWasteCategory() == null ? "" : item.getWasteCategory());
                row.createCell(col++).setCellValue(item.getWasteCategoryName() == null ? "" : item.getWasteCategoryName());
                row.createCell(col++).setCellValue(item.getIndustrySource() == null ? "" : item.getIndustrySource());
                row.createCell(col++).setCellValue(item.getWasteCode() == null ? "" : item.getWasteCode());
                row.createCell(col++).setCellValue(item.getWasteName() == null ? "" : item.getWasteName());
                row.createCell(col).setCellValue(item.getHazardCharacteristic() == null ? "" : item.getHazardCharacteristic());
            }
            writeWorkbookToResponse(workbook, response, "危险废物名录导出.xlsx");
        }
    }

    private void writeWorkbookToResponse(Workbook workbook, HttpServletResponse response, String fileName) throws Exception {
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedName);
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            outputStream.flush();
        }
    }
}


