package com.erp.controller.production;

import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.production.dto.ImportPdfTransferManifestResponse;
import com.erp.controller.production.dto.ImportTransferManifestResponse;
import com.erp.controller.production.dto.PdfImportTaskResult;
import com.erp.controller.production.dto.TransferManifestListResponse;
import com.erp.controller.production.dto.TransferManifestExportRow;
import com.erp.controller.production.dto.TransferManifestPageRequest;
import com.erp.service.production.TransferManifestService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 转移联单管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/platform/transfer-manifest")
@Api(tags = "转移联单管理")
public class TransferManifestController {

    @Autowired
    private TransferManifestService transferManifestService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 分页查询转移联单列表
     */
    @RequirePagePermission("平台管理:转移联单:页面")
    @GetMapping("/list")
    @ApiOperation(value = "分页查询转移联单列表", notes = "支持按广东省联单号、国家联单号、产生单位、接收单位、车牌号、当前阶段、计划转移时间范围过滤，含废物子项")
    public Result<TransferManifestListResponse> getList(TransferManifestPageRequest request) {
        try {
            if (request.getPage() == null || request.getPage() <= 0) request.setPage(1);
            if (request.getSize() == null || request.getSize() <= 0) request.setSize(20);
            TransferManifestListResponse response = transferManifestService.getPage(request);
            return Result.success("查询成功", response);
        } catch (Exception e) {
            log.error("查询转移联单列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 批量导出转移联单（Excel）
     */
    @RequirePagePermission("平台管理:转移联单:页面")
    @GetMapping("/export/excel")
    @ApiOperation(value = "批量导出转移联单（Excel）", notes = "未传 ids 时导出全部，传 ids 时导出勾选项；同一联单多个子项各占一行")
    public void exportExcel(@RequestParam(value = "ids", required = false) java.util.List<Integer> ids,
                            HttpServletRequest httpRequest,
                            HttpServletResponse response) throws IOException {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        java.util.List<TransferManifestExportRow> rows = transferManifestService.getExportRows(ids);

        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("转移联单");
        String[] headers = new String[] {
                "广东省联单号", "国家联单号", "产生单位", "产废单位所属市", "产废单位所属区（县/镇）", "产废单位所属镇（街道）",
                "废物类别", "废物代码", "废物名称", "废物形态", "包装方式", "计划数量", "确认数量", "计量单位",
                "发运人", "接收人", "计划转移时间", "接收日期", "处置方式大类", "处置方式小类", "车牌号", "承运人",
                "运输开始时间", "运输结束时间", "运输单位", "接收单位", "接收单位所属省", "接收单位所属市", "接收单位所属区（县/镇）",
                "许可证编号", "接收单位处理意见", "是否存在重大差异", "重大差异简述", "接收企业备注", "是否作废", "当前阶段", "补录类型"
        };
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        int rowIndex = 1;
        for (TransferManifestExportRow item : rows) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex++);
            String[] values = new String[] {
                    nvl(item.get广东省联单号()),
                    nvl(item.get国家联单号()),
                    nvl(item.get产生单位()),
                    nvl(item.get产废单位所属市()),
                    nvl(item.get产废单位所属区()),
                    nvl(item.get产废单位所属镇()),
                    nvl(item.get废物类别()),
                    nvl(item.get废物代码()),
                    nvl(item.get废物名称()),
                    nvl(item.get废物形态()),
                    nvl(item.get包装方式()),
                    item.get计划数量() == null ? "" : item.get计划数量().stripTrailingZeros().toPlainString(),
                    item.get确认数量() == null ? "" : item.get确认数量().stripTrailingZeros().toPlainString(),
                    nvl(item.get计量单位()),
                    nvl(item.get发运人()),
                    nvl(item.get接收人()),
                    nvl(item.get计划转移时间()),
                    nvl(item.get接收日期()),
                    nvl(item.get处置方式大类()),
                    nvl(item.get处置方式小类()),
                    nvl(item.get车牌号()),
                    nvl(item.get承运人()),
                    nvl(item.get运输开始时间()),
                    nvl(item.get运输结束时间()),
                    nvl(item.get运输单位()),
                    nvl(item.get接收单位()),
                    nvl(item.get接收单位所属省()),
                    nvl(item.get接收单位所属市()),
                    nvl(item.get接收单位所属区()),
                    nvl(item.get许可证编号()),
                    nvl(item.get接收单位处理意见()),
                    statusText(item.get是否存在重大差异(), "否", "是"),
                    nvl(item.get重大差异简述()),
                    nvl(item.get接收企业备注()),
                    statusText(item.get是否作废(), "正常", "已作废"),
                    nvl(item.get当前阶段()),
                    nvl(item.get补录类型())
            };
            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(values[i]);
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1024, 255 * 256));
        }

        String fileName = URLEncoder.encode("转移联单导出.xlsx", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
        workbook.write(response.getOutputStream());
        workbook.close();

        logRecordService.recordOperationLog("转移联单管理", "导出",
                String.format("导出转移联单Excel：共%d条记录", rows.size()),
                userId, ipAddress, true, null);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String statusText(Integer value, String zeroText, String oneText) {
        if (value == null) {
            return "";
        }
        if (value == 0) {
            return zeroText;
        }
        if (value == 1) {
            return oneText;
        }
        return String.valueOf(value);
    }

    /**
     * 批量导出转移联单 PDF（ZIP）
     */
    @RequirePagePermission("平台管理:转移联单:页面")
    @GetMapping("/export/pdf")
    @ApiOperation(value = "批量导出转移联单 PDF（ZIP）", notes = "未传 ids 时遍历全部，传 ids 时导出勾选项；仅打包存在 PDF 文件且能成功读取的附件")
    public void exportPdf(@RequestParam(value = "ids", required = false) List<Integer> ids,
                          HttpServletRequest httpRequest,
                          HttpServletResponse response) throws IOException {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        byte[] zipBytes = transferManifestService.exportPdfZip(ids);
        if (zipBytes == null || zipBytes.length == 0) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"没有对应的文件\"}");
            logRecordService.recordOperationLog("转移联单管理", "导出",
                    "导出转移联单PDF失败：没有对应的文件",
                    userId, ipAddress, false, "没有找到对应的PDF文件");
            return;
        }

        String fileName = URLEncoder.encode("转移联单PDF附件.zip", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
        response.setContentLength(zipBytes.length);
        response.getOutputStream().write(zipBytes);
        response.getOutputStream().flush();

        logRecordService.recordOperationLog("转移联单管理", "导出",
                String.format("导出转移联单PDF：共%d字节", zipBytes.length),
                userId, ipAddress, true, null);
    }

    /**
     * 批量导入转移联单（Excel）
     */
    @RequirePagePermission("平台管理:转移联单:页面")
    @PostMapping("/import/excel")
    @ApiOperation(value = "批量导入转移联单（Excel）",
            notes = "上传含主表及废物子项数据的 Excel 文件，按广东省联单号分组，事务统一写入，任意失败全量回滚")
    public Result<ImportTransferManifestResponse> importExcel(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        // 基本参数校验
        if (file == null || file.isEmpty()) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "请上传 Excel 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "仅支持 .xlsx / .xls 格式的 Excel 文件");
        }
        // 限制 20MB
        if (file.getSize() > 20L * 1024 * 1024) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "文件大小不能超过 20MB");
        }

        try {
            ImportTransferManifestResponse response = transferManifestService.importFromExcel(file);

            String logContent = String.format(
                    "批量导入转移联单（Excel）：文件=%s，共%d条，成功%d条，失败%d条",
                    file.getOriginalFilename(),
                    response.getTotal(),
                    response.getSuccess(),
                    response.getError());
            logRecordService.recordOperationLog("转移联单管理", "导入", logContent, userId, ipAddress, true, null);

            return Result.success("导入完成", response);

        } catch (Exception e) {
            log.error("批量导入转移联单失败，文件={}", file.getOriginalFilename(), e);
            logRecordService.recordOperationLog("转移联单管理", "导入",
                    "批量导入转移联单失败，文件=" + file.getOriginalFilename(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "导入失败：" + e.getMessage());
        }
    }

    /**
     * 批量导入转移联单 PDF 附件
     * <p>
     * 接收整体 PDF 文件，按「危险废物转移联单」标题自动分页拆分，
     * 提取每页数字序列与数据库联单号匹配，将拆分子文件写入 FILE 表并回写联单记录。
     * 整个操作在同一事务内完成，任意匹配/上传失败则全量回滚。
     * </p>
     *
     * @param file        PDF 文件（.pdf），最大 100MB
     * @param httpRequest HTTP 请求（用于获取客户端 IP）
     * @return 导入结果（总分页数 / 匹配数 / 未匹配数 / 未匹配详情）
     */
    @RequirePagePermission("平台管理:转移联单:页面")
    @PostMapping("/import/pdf")
    @ApiOperation(value = "批量导入转移联单 PDF 附件",
            notes = "上传整体 PDF，按联单标题分页，提取数字匹配联单记录，拆分子文件写入 FILE 表，事务统一提交，任意失败全量回滚")
    public Result<Map<String, String>> importPdf(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        if (file == null || file.isEmpty()) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "请上传 PDF 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".pdf")) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "仅支持 .pdf 格式文件");
        }
        if (file.getSize() > 100L * 1024 * 1024) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "文件大小不能超过 100MB");
        }

        try {
            // 异步提交：立即返回 taskId，前端轮询 /import/pdf/status/{taskId}
            String taskId = transferManifestService.submitPdfImportTask(file, userId);
            logRecordService.recordOperationLog("转移联单管理", "PDF导入",
                    "提交 PDF 导入任务，文件=" + file.getOriginalFilename() + "，taskId=" + taskId,
                    userId, ipAddress, true, null);
            return Result.success("PDF 导入任务已提交", java.util.Collections.singletonMap("taskId", taskId));

        } catch (Exception e) {
            log.error("提交 PDF 导入任务失败，文件={}", file.getOriginalFilename(), e);
            logRecordService.recordOperationLog("转移联单管理", "PDF导入",
                    "提交 PDF 导入任务失败，文件=" + file.getOriginalFilename(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "PDF 导入任务提交失败：" + e.getMessage());
        }
    }

    @RequirePagePermission("平台管理:转移联单:页面")
    @PostMapping("/{manifestId}/upload/pdf")
    @ApiOperation(value = "上传单条联单 PDF 附件", notes = "上传单个 PDF 文件，直接替换联单的 PDF文件编号")
    public Result<Map<String, Integer>> uploadPdf(
            @PathVariable Integer manifestId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        if (file == null || file.isEmpty()) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "请上传 PDF 文件");
        }
        try {
            Integer pdfFileId = transferManifestService.uploadPdf(manifestId, file);
            logRecordService.recordOperationLog("转移联单管理", "上传PDF",
                    "上传联单 PDF 成功，联单编号=" + manifestId + "，文件=" + file.getOriginalFilename(),
                    userId, ipAddress, true, null);
            return Result.success("上传成功", java.util.Collections.singletonMap("pdfFileId", pdfFileId));
        } catch (Exception e) {
            log.error("上传联单 PDF 失败，manifestId={}, file={}", manifestId, file.getOriginalFilename(), e);
            logRecordService.recordOperationLog("转移联单管理", "上传PDF",
                    "上传联单 PDF 失败，联单编号=" + manifestId + "，文件=" + file.getOriginalFilename(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "上传失败：" + e.getMessage());
        }
    }

    @RequirePagePermission("平台管理:转移联单:页面")
    @GetMapping("/{manifestId}/pdf-file-id")
    @ApiOperation(value = "查询联单PDF文件编号", notes = "用于点击查看时判断是否直接预览 PDF")
    public Result<Map<String, Integer>> getPdfFileId(@PathVariable Integer manifestId) {
        Integer pdfFileId = transferManifestService.getPdfFileId(manifestId);
        return Result.success("查询成功", java.util.Collections.singletonMap("pdfFileId", pdfFileId));
    }

    /**
     * 查询 PDF 导入任务状态（前端轮询接口）
     *
     * @param taskId 任务 ID（由 /import/pdf 返回）
     * @return 任务状态：PENDING / RUNNING / SUCCESS / FAILED
     */
    @RequirePagePermission("平台管理:转移联单:页面")
    @GetMapping("/import/pdf/status/{taskId}")
    @ApiOperation(value = "查询 PDF 导入任务状态", notes = "轮询接口，返回 PENDING/RUNNING/SUCCESS/FAILED 及结果")
    public Result<PdfImportTaskResult> getPdfImportStatus(@PathVariable String taskId) {
        PdfImportTaskResult result = transferManifestService.getPdfImportTaskResult(taskId);
        if (result == null) {
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "任务不存在或已过期");
        }
        return Result.success("查询成功", result);
    }
}
