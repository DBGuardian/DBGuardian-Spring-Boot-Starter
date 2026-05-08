package com.erp.controller.transport;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.controller.transport.dto.VehicleArchiveResponse;
import com.erp.controller.transport.dto.VehicleCreateRequest;
import com.erp.controller.transport.dto.VehicleDetailResponse;
import com.erp.controller.transport.dto.VehicleImportResponse;
import com.erp.controller.transport.dto.VehiclePageRequest;
import com.erp.controller.transport.dto.VehicleUpdateRequest;
import com.erp.service.transport.VehicleService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 车辆管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/transport/vehicle")
@Api(tags = "车辆管理")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @RequirePagePermission({
            "档案管理:车辆档案:页面",
            "档案管理:设备档案:页面",
            "运输管理:运输执行:页面",
            "运输管理:车辆安排:页面"
    })
    @GetMapping("/archive")
    @ApiOperation(value = "获取车辆档案列表", notes = "分页查询车辆档案，支持关键字搜索和状态筛选")
    public Result<VehicleArchiveResponse> getVehicleArchive(
            VehiclePageRequest request) {
        try {
            VehicleArchiveResponse response = vehicleService.getVehicleArchive(request);
            return Result.success("获取车辆档案成功", response);
        } catch (Exception e) {
            return Result.error(500, "获取车辆档案失败：" + e.getMessage());
        }
    }

    @GetMapping("/{vehicleId}")
    @ApiOperation(value = "获取车辆详情", notes = "根据车辆ID获取车辆详细信息")
    public Result<VehicleDetailResponse> getVehicleDetail(@PathVariable Integer vehicleId) {
        try {
            VehicleDetailResponse response = vehicleService.getVehicleDetail(vehicleId);
            return Result.success("获取车辆详情成功", response);
        } catch (Exception e) {
            return Result.error(500, "获取车辆详情失败：" + e.getMessage());
        }
    }

    @PostMapping
    @ApiOperation(value = "新增车辆", notes = "创建车辆档案")
    @RequireActionPermission("档案管理:车辆档案:新增")
    public Result<VehicleDetailResponse> createVehicle(@Valid @RequestBody VehicleCreateRequest request,
                                                          HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            VehicleDetailResponse response = vehicleService.createVehicle(request);
            logRecordService.recordOperationLog("车辆管理", "新增",
                    "新增车辆：" + request.getPlateNo(), userId, ipAddress, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "VEHICLE_CREATE",
                        response.getVehicleId(),
                        String.format("车辆已添加：车牌号=%s", request.getPlateNo()),
                        "新增",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送新增车辆通知失败", msgEx);
            }
            return Result.success("新增车辆成功", response);
        } catch (Exception e) {
            logRecordService.recordOperationLog("车辆管理", "新增",
                    "新增车辆：" + request.getPlateNo(), userId, ipAddress, false, e.getMessage());
            return Result.error(500, "新增车辆失败：" + e.getMessage());
        }
    }

    @PutMapping("/{vehicleId}")
    @ApiOperation(value = "更新车辆", notes = "更新车辆档案信息")
    @RequireActionPermission("档案管理:车辆档案:编辑")
    public Result<Void> updateVehicle(@PathVariable Integer vehicleId,
                                       @Valid @RequestBody VehicleUpdateRequest request,
                                       HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            request.setVehicleId(vehicleId);
            vehicleService.updateVehicle(request);
            logRecordService.recordOperationLog("车辆管理", "编辑",
                    "编辑车辆：" + vehicleId, userId, ipAddress, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "VEHICLE_UPDATE",
                        vehicleId,
                        String.format("车辆信息已更新：车辆ID=%d", vehicleId),
                        "更新",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送更新车辆通知失败", msgEx);
            }
            return Result.success("更新车辆成功", null);
        } catch (Exception e) {
            logRecordService.recordOperationLog("车辆管理", "编辑",
                    "编辑车辆：" + vehicleId, userId, ipAddress, false, e.getMessage());
            return Result.error(500, "更新车辆失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{vehicleId}")
    @ApiOperation(value = "删除车辆", notes = "删除车辆档案")
    public Result<Void> deleteVehicle(@PathVariable Integer vehicleId, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            vehicleService.deleteVehicle(vehicleId);
            logRecordService.recordOperationLog("车辆管理", "删除",
                    "删除车辆：" + vehicleId, userId, ipAddress, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "VEHICLE_DELETE",
                        vehicleId,
                        String.format("车辆已删除：车辆ID=%d", vehicleId),
                        "删除",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送删除车辆通知失败", msgEx);
            }
            return Result.success("删除车辆成功", null);
        } catch (Exception e) {
            logRecordService.recordOperationLog("车辆管理", "删除",
                    "删除车辆：" + vehicleId, userId, ipAddress, false, e.getMessage());
            return Result.error(500, "删除车辆失败：" + e.getMessage());
        }
    }

    @PostMapping("/import")
    @ApiOperation(value = "批量导入车辆", notes = "支持上传Excel文件批量创建车辆信息")
    @RequireActionPermission("档案管理:车辆档案:导入")
    public Result<VehicleImportResponse> importVehicles(@RequestParam("file") MultipartFile file,
                                                          HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            VehicleImportResponse response = vehicleService.importVehicles(file);
            // 记录导入操作日志
            String content = String.format("批量导入车辆：总记录数=%d，成功=%d，失败=%d",
                    response.getTotalCount(), response.getSuccessCount(), response.getFailCount());
            logRecordService.recordOperationLog("车辆管理", "导入", content, userId, ipAddress, true, null);
            return Result.success("导入完成", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("车辆管理", "导入",
                    "批量导入车辆失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("导入车辆失败", e);
            logRecordService.recordOperationLog("车辆管理", "导入",
                    "批量导入车辆失败", userId, ipAddress, false, e.getMessage());
            return Result.error(500, "导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/import/template")
    @ApiOperation(value = "下载车辆导入模板", notes = "下载前导入格式一致的Excel模板")
    public void exportImportTemplate(HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("车辆导入模板");
            // 与 VehicleServiceImpl.buildVehicleFromRow 中列顺序保持一致
            String[] headers = {
                    "公司名称",
                    "公司地址",
                    "车牌号",
                    "车辆类型",
                    "核载吨位",
                    "座位数",
                    "车辆状态",
                    "经营范围",
                    "经营许可证号",
                    "发证机关",
                    "发证日期",
                    "有效期至",
                    "检验有效期至",
                    "技术等级评定日期",
                    "车辆长度(mm)",
                    "车辆宽度(mm)",
                    "车辆高度(mm)",
                    "备注"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            String fileName = URLEncoder.encode("车辆导入模板.xlsx", StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("下载车辆导入模板失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出模板失败：" + e.getMessage());
        }
    }

    @GetMapping("/export")
    @ApiOperation(value = "导出车辆", notes = "根据筛选条件导出车辆Excel")
    @RequireActionPermission("档案管理:车辆档案:导出")
    public void exportVehicles(@Valid VehiclePageRequest request, HttpServletResponse response,
                               HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            List<VehicleDetailResponse> list = vehicleService.listVehiclesForExport(request);
            writeExport(list, response);
            // 记录导出操作日志
            logRecordService.recordOperationLog("车辆管理", "导出",
                    "导出车辆列表，共" + list.size() + "条记录", userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("车辆管理", "导出",
                    "导出车辆列表失败", userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出车辆失败", e);
            logRecordService.recordOperationLog("车辆管理", "导出",
                    "导出车辆列表失败", userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    private void writeExport(List<VehicleDetailResponse> data, HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("车辆信息");
            String[] headers = {
                    "公司名称",
                    "公司地址",
                    "车牌号",
                    "车辆类型",
                    "核载吨位",
                    "座位数",
                    "车辆状态",
                    "经营范围",
                    "经营许可证号",
                    "发证机关",
                    "发证日期",
                    "有效期至",
                    "检验有效期至",
                    "技术等级评定日期",
                    "车辆长度(mm)",
                    "车辆宽度(mm)",
                    "车辆高度(mm)",
                    "备注"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (int i = 0; i < data.size(); i++) {
                VehicleDetailResponse item = data.get(i);
                Row row = sheet.createRow(i + 1);
                int col = 0;
                row.createCell(col++).setCellValue(item.getCompanyName() == null ? "" : item.getCompanyName());
                row.createCell(col++).setCellValue(item.getCompanyAddress() == null ? "" : item.getCompanyAddress());
                row.createCell(col++).setCellValue(item.getPlateNo() == null ? "" : item.getPlateNo());
                row.createCell(col++).setCellValue(item.getVehicleType() == null ? "" : item.getVehicleType());
                row.createCell(col++).setCellValue(item.getLoadCapacity() == null ? "" : item.getLoadCapacity().toString());
                row.createCell(col++).setCellValue(item.getSeatCount() == null ? "" : item.getSeatCount().toString());
                row.createCell(col++).setCellValue(item.getStatus() == null ? "" : item.getStatus());
                row.createCell(col++).setCellValue(item.getOperationScope() == null ? "" : item.getOperationScope());
                row.createCell(col++).setCellValue(item.getOperationLicenseNo() == null ? "" : item.getOperationLicenseNo());
                row.createCell(col++).setCellValue(item.getIssuingAuthority() == null ? "" : item.getIssuingAuthority());
                row.createCell(col++).setCellValue(item.getIssuingDate() == null ? "" : dateFormatter.format(item.getIssuingDate()));
                row.createCell(col++).setCellValue(item.getLicenseValidUntil() == null ? "" : dateFormatter.format(item.getLicenseValidUntil()));
                row.createCell(col++).setCellValue(item.getInspectionValidUntil() == null ? "" : dateFormatter.format(item.getInspectionValidUntil()));
                row.createCell(col++).setCellValue(item.getTechLevelDate() == null ? "" : dateFormatter.format(item.getTechLevelDate()));
                row.createCell(col++).setCellValue(item.getVehicleLengthMm() == null ? "" : item.getVehicleLengthMm().toString());
                row.createCell(col++).setCellValue(item.getVehicleWidthMm() == null ? "" : item.getVehicleWidthMm().toString());
                row.createCell(col++).setCellValue(item.getVehicleHeightMm() == null ? "" : item.getVehicleHeightMm().toString());
                row.createCell(col).setCellValue(item.getRemarks() == null ? "" : item.getRemarks());
            }
            String fileName = URLEncoder.encode("车辆信息导出.xlsx", StandardCharsets.UTF_8.name());
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

