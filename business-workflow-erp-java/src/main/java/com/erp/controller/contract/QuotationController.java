package com.erp.controller.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.contract.dto.*;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.service.contract.QuotationService;
import com.erp.service.system.ILogRecordService;
import com.erp.util.QuotationWordGenerator;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报价单管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/quotation")
@Api(tags = "报价单管理")
public class QuotationController {

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 新增报价单
     */
    @RequireActionPermission("业务管理:客户报价:新增")
    @PostMapping
    @ApiOperation(value = "新增报价单", notes = "创建新的报价单，支持总价包干和按量结算两种模式")
    public Result<QuotationDetailResponse> createQuotation(@Valid @RequestBody QuotationCreateRequest request,
                                                            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            QuotationDetailResponse response = quotationService.createQuotation(request);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("报价单管理", "新增", 
                    "新增报价单：报价单号=" + (response.getQuotationNo() != null ? response.getQuotationNo() : "ID=" + response.getQuotationId()), 
                    userId, ipAddress, true, null);
            return Result.success("新增报价单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "新增", 
                    "新增报价单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增报价单失败", e);
            logRecordService.recordOperationLog("报价单管理", "新增", 
                    "新增报价单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增报价单失败：" + e.getMessage());
        }
    }

    /**
     * 更新报价单
     */
    @RequireActionPermission("业务管理:客户报价:编辑")
    @PutMapping("/{quotationId}")
    @ApiOperation(value = "更新报价单", notes = "更新报价单信息，只有待审核或审核中状态的报价单可以修改")
    public Result<Void> updateQuotation(
            @PathVariable("quotationId") Integer quotationId,
            @Valid @RequestBody QuotationUpdateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            request.setQuotationId(quotationId);
            quotationService.updateQuotation(request);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("报价单管理", "编辑", 
                    "更新报价单：ID=" + quotationId, userId, ipAddress, true, null);
            return Result.success("更新报价单成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "编辑", 
                    "更新报价单：ID=" + quotationId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新报价单失败", e);
            logRecordService.recordOperationLog("报价单管理", "编辑", 
                    "更新报价单：ID=" + quotationId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新报价单失败：" + e.getMessage());
        }
    }

    /**
     * 报价单详情
     */
    @GetMapping("/{quotationId}")
    @ApiOperation(value = "报价单详情", notes = "根据报价单编号查询详情")
    public Result<QuotationDetailResponse> getQuotationDetail(@PathVariable("quotationId") Integer quotationId) {
        try {
            QuotationDetailResponse response = quotationService.getQuotationDetail(quotationId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询报价单详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 报价单分页查询
     */
    @RequirePagePermission("业务管理:客户报价:页面")
    @GetMapping("/list")
    @ApiOperation(value = "报价单分页查询", notes = "支持报价单编号、客户名称模糊查询，可筛选报价状态、计价方式、有效期、PDF状态")
    public Result<IPage<QuotationPageResponse>> getQuotationPage(@Valid QuotationPageRequest request) {
        try {
            IPage<QuotationPageResponse> page = quotationService.getQuotationPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询报价单列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 导出报价单（必须在/{quotationId}之前，避免路由冲突）
     */
    @RequireActionPermission("业务管理:客户报价:导出")
    @GetMapping("/export")
    @ApiOperation(value = "导出报价单", notes = "根据筛选条件导出报价单Excel")
    public void exportQuotations(@Valid QuotationPageRequest request, HttpServletResponse response,
                                 HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            List<QuotationPageResponse> list = quotationService.listQuotationsForExport(request);
            writeExport(list, response);
            // 记录导出操作日志
            logRecordService.recordOperationLog("报价单管理", "导出", 
                    "导出报价单列表，共" + list.size() + "条记录", userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "导出", 
                    "导出报价单列表失败", userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出报价单失败", e);
            logRecordService.recordOperationLog("报价单管理", "导出", 
                    "导出报价单列表失败", userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    /**
     * 生成报价单PDF
     */
    @RequireActionPermission("业务管理:客户报价:生成文档")
    @PostMapping("/{quotationId}/pdf")
    @ApiOperation(value = "生成报价单PDF", notes = "为指定报价单生成PDF文件")
    public Result<QuotationDetailResponse> generatePdf(@PathVariable("quotationId") Integer quotationId,
                                                         HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            QuotationDetailResponse response = quotationService.generatePdf(quotationId);
            // 记录生成操作日志
            logRecordService.recordOperationLog("报价单管理", "生成", 
                    "生成报价单PDF：ID=" + quotationId, userId, ipAddress, true, null);
            return Result.success("PDF生成成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "生成", 
                    "生成报价单PDF：ID=" + quotationId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("生成报价单PDF失败", e);
            logRecordService.recordOperationLog("报价单管理", "生成", 
                    "生成报价单PDF：ID=" + quotationId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成PDF失败：" + e.getMessage());
        }
    }

    /**
     * 导出报价单Word文档
     */
    @RequireActionPermission("业务管理:客户报价:导出文档")
    @GetMapping("/{quotationId}/word")
    @ApiOperation(value = "导出报价单Word", notes = "为指定报价单生成并下载Word文档")
    public void exportQuotationWord(@PathVariable("quotationId") Integer quotationId,
                                    HttpServletResponse response,
                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            QuotationDetailResponse detail = quotationService.getQuotationDetail(quotationId);
            // 生成文件名：报价单_客户名称_报价单号.docx，无号时用 quotation-{ID}.docx
            String customerName = "";
            if (detail.getCustomerSnapshot() != null && detail.getCustomerSnapshot().getCustomerName() != null) {
                customerName = detail.getCustomerSnapshot().getCustomerName();
            }
            String quotationNo = detail.getQuotationNo();
            String baseName = (quotationNo != null && !quotationNo.trim().isEmpty()) ? quotationNo : "quotation-" + quotationId;
            String fileName = customerName.isEmpty() ? baseName : "报价单_" + customerName + "_" + baseName;
            // 过滤非法字符
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_") + ".docx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());

            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);

            try (ServletOutputStream outputStream = response.getOutputStream()) {
                QuotationWordGenerator.generateWord(detail, outputStream);
                outputStream.flush();
            }
            // 记录导出操作日志
            logRecordService.recordOperationLog("报价单管理", "导出", 
                    "导出报价单Word：ID=" + quotationId, userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "导出", 
                    "导出报价单Word：ID=" + quotationId, userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出报价单Word失败", e);
            logRecordService.recordOperationLog("报价单管理", "导出", 
                    "导出报价单Word：ID=" + quotationId, userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "导出Word失败：" + e.getMessage());
        }
    }

    /**
     * 审核报价单
     */
    @RequireActionPermission("业务管理:客户报价:批量审核")
    @PostMapping("/audit")
    @ApiOperation(value = "审核报价单", notes = "审核报价单并修改状态。审核中状态的报价单可审核为已通过或已驳回，驳回时审核意见必填。其他状态也可通过此接口修改状态（已失效状态除外）")
    public Result<Void> auditQuotation(@Valid @RequestBody QuotationAuditRequest request,
                                       HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            quotationService.auditQuotation(request);
            // 记录审核操作日志
            String auditResult = "已通过".equals(request.getAuditResult()) ? "通过" : 
                                "已驳回".equals(request.getAuditResult()) ? "驳回" : request.getAuditResult();
            String content = String.format("审核报价单：ID=%d，审核结果=%s", request.getQuotationId(), auditResult);
            if (request.getAuditOpinion() != null && !request.getAuditOpinion().isEmpty()) {
                content += "，审核意见：" + request.getAuditOpinion();
            }
            logRecordService.recordOperationLog("报价单管理", "审核", content, userId, ipAddress, true, null);
            return Result.success("审核报价单成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "审核", 
                    "审核报价单：ID=" + request.getQuotationId(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审核报价单失败", e);
            logRecordService.recordOperationLog("报价单管理", "审核", 
                    "审核报价单：ID=" + request.getQuotationId(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "审核失败：" + e.getMessage());
        }
    }

    /**
     * 提交报价单到OA审批
     */
    @RequireActionPermission("业务管理:客户报价:批量提交审核")
    @PostMapping("/submit-approval")
    @ApiOperation(value = "提交报价单到OA审批", notes = "将报价单提交到OA审批系统，创建OA审批记录")
    public Result<com.erp.service.contract.dto.OaApprovalSubmitResult> submitForApproval(
            @RequestParam Integer quotationId,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            com.erp.service.contract.dto.OaApprovalSubmitResult result = quotationService.submitForApproval(quotationId);
            // 记录操作日志
            logRecordService.recordOperationLog("报价单管理", "提交OA审批",
                    "提交报价单到OA审批：quotationId=" + quotationId, userId, ipAddress, true, null);
            return Result.success("提交OA审批成功", result);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "提交OA审批",
                    "提交报价单到OA审批：quotationId=" + quotationId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("提交报价单到OA审批失败", e);
            logRecordService.recordOperationLog("报价单管理", "提交OA审批",
                    "提交报价单到OA审批：quotationId=" + quotationId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "提交OA审批失败：" + e.getMessage());
        }
    }

    /**
     * 批量审核报价单
     */
    @RequireActionPermission("业务管理:客户报价:批量审核")
    @PostMapping("/batch-audit")
    @ApiOperation(value = "批量审核报价单", notes = "批量审核待审批状态的报价单。审核结果应用于所有选中的报价单")
    public Result<Void> batchAudit(@Valid @RequestBody QuotationBatchAuditRequest request,
                                   HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            if (request.getQuotationIds() == null || request.getQuotationIds().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要审核的报价单");
            }
            quotationService.batchAudit(request);
            // 记录批量审核操作日志
            String auditResult = "已通过".equals(request.getAuditResult()) ? "通过" :
                                "已驳回".equals(request.getAuditResult()) ? "驳回" : request.getAuditResult();
            String content = String.format("批量审核报价单：数量=%d，审核结果=%s", request.getQuotationIds().size(), auditResult);
            if (request.getAuditOpinion() != null && !request.getAuditOpinion().isEmpty()) {
                content += "，审核意见：" + request.getAuditOpinion();
            }
            logRecordService.recordOperationLog("报价单管理", "批量审核", content, userId, ipAddress, true, null);
            return Result.success("批量审核成功，共审核" + request.getQuotationIds().size() + "个报价单", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "批量审核",
                    "批量审核报价单：数量=" + (request.getQuotationIds() != null ? request.getQuotationIds().size() : 0), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量审核报价单失败", e);
            logRecordService.recordOperationLog("报价单管理", "批量审核",
                    "批量审核报价单：数量=" + (request.getQuotationIds() != null ? request.getQuotationIds().size() : 0), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量撤回报价单
     */
    @RequireActionPermission("业务管理:客户报价:批量撤回")
    @PostMapping("/batch-revoke")
    @ApiOperation(value = "批量撤回报价单", notes = "将审核中状态的报价单撤回为待审核状态")
    public Result<Void> batchRevoke(@Valid @RequestBody QuotationBatchRevokeRequest request,
                                     HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            if (request.getQuotationIds() == null || request.getQuotationIds().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要撤回的报价单");
            }
            quotationService.batchRevoke(request.getQuotationIds());
            // 记录批量撤回操作日志
            logRecordService.recordOperationLog("报价单管理", "批量撤回",
                    "批量撤回报价单：数量=" + request.getQuotationIds().size(), userId, ipAddress, true, null);
            return Result.success("批量撤回成功，共撤回" + request.getQuotationIds().size() + "个报价单", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("报价单管理", "批量撤回",
                    "批量撤回报价单：数量=" + (request.getQuotationIds() != null ? request.getQuotationIds().size() : 0), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量撤回报价单失败", e);
            logRecordService.recordOperationLog("报价单管理", "批量撤回",
                    "批量撤回报价单：数量=" + (request.getQuotationIds() != null ? request.getQuotationIds().size() : 0), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量撤回失败：" + e.getMessage());
        }
    }

    /**
     * 写入导出Excel
     */
    private void writeExport(List<QuotationPageResponse> data, HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("报价单信息");
            String[] headers = {
                "报价单号",
                "客户名称",
                "报价状态",
                "计价方式",
                "有效期开始",
                "有效期结束",
                "备注"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < data.size(); i++) {
                QuotationPageResponse item = data.get(i);
                Row row = sheet.createRow(i + 1);
                int col = 0;
                row.createCell(col++).setCellValue(item.getQuotationNo() == null ? "" : item.getQuotationNo());
                row.createCell(col++).setCellValue(item.getCustomerName() == null ? "" : item.getCustomerName());
                row.createCell(col++).setCellValue(item.getQuotationStatus() == null ? "" : item.getQuotationStatus());
                row.createCell(col++).setCellValue(item.getPricingMode() == null ? "" : item.getPricingMode());
                row.createCell(col++).setCellValue(item.getValidFrom() == null ? "" : formatter.format(item.getValidFrom()));
                row.createCell(col++).setCellValue(item.getValidTo() == null ? "" : formatter.format(item.getValidTo()));
                row.createCell(col).setCellValue(item.getRemark() == null ? "" : item.getRemark());
            }
            String fileName = URLEncoder.encode("报价单导出.xlsx", StandardCharsets.UTF_8.name());
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


