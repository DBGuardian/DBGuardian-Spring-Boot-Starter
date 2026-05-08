package com.erp.controller.finance;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.result.ImportResult;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.*;
import com.erp.entity.finance.FundSubject;
import com.erp.service.finance.FundSubjectService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.common.annotation.RequireSimplePageEdit;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * 会计科目管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/fund/subjects")
@Api(tags = "会计科目管理")
@Validated
public class FundSubjectController {

    @Autowired
    private FundSubjectService fundSubjectService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 分页查询科目列表
     */
    @RequirePagePermission("财务管理:账户设置:科目管理:页面")
    @GetMapping
    @ApiOperation("分页查询科目列表")
    public Result<IPage<FundSubjectListItemResponse>> getSubjectPage(
            @Valid FundSubjectPageRequest request) {

        log.info("分页查询科目列表：{}", request);

        Page<FundSubjectListItemResponse> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<FundSubjectListItemResponse> result = fundSubjectService.getSubjectPage(page, request);

        // 转换时间格式
        result.getRecords().forEach(item -> {
            if (item.getCreateTime() != null) {
                item.setCreateTime(DateUtil.format(DateUtil.parse(item.getCreateTime()), "yyyy-MM-dd HH:mm:ss"));
            }
            if (item.getUpdateTime() != null) {
                item.setUpdateTime(DateUtil.format(DateUtil.parse(item.getUpdateTime()), "yyyy-MM-dd HH:mm:ss"));
            }
        });

        return Result.success(result);
    }

    /**
     * 创建科目
     */
    @RequireSimplePageEdit("财务管理:账户设置:科目管理:页面")
    @PostMapping
    @ApiOperation("创建科目")
    public Result<FundSubject> createSubject(@Valid @RequestBody FundSubjectCreateRequest request,
                                              HttpServletRequest httpRequest) {

        log.info("创建科目：{}", request);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean createSuccess = false;
        String errorMessage = null;
        FundSubject subject = null;

        try {
            subject = fundSubjectService.createSubject(
                    request.getSubjectCode(),
                    request.getSubjectName(),
                    request.getSubjectCategory(),
                    request.getBalanceDirection(),
                    request.getIsCashSubject(),
                    request.getAuxiliaryAccounting(),
                    request.getQuantityAccounting(),
                    request.getForeignCurrencyAccounting(),
                    request.getRemark()
            );
            createSuccess = true;
            return Result.success(subject);
        } catch (Exception e) {
            log.error("创建科目失败：{}", request, e);
            errorMessage = e.getMessage();
            return Result.error("创建科目失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("创建科目：科目编码=%s，科目名称=%s",
                        request.getSubjectCode(), request.getSubjectName());
                logRecordService.recordOperationLog("会计科目管理", "新增",
                        logContent, userId, ipAddress, createSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录创建科目操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (createSuccess && subject != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SUBJECT_CREATE", subject.getSubjectId() != null ? subject.getSubjectId().intValue() : null, "会计科目已创建", "新增", userId);
                } catch (Exception msgEx) {
                    log.warn("发送科目创建通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 更新科目
     */
    @RequireSimplePageEdit("财务管理:账户设置:科目管理:页面")
    @PutMapping("/{subjectId}")
    @ApiOperation("更新科目")
    public Result<FundSubject> updateSubject(
            @PathVariable Long subjectId,
            @Valid @RequestBody FundSubjectUpdateRequest request,
            HttpServletRequest httpRequest) {

        log.info("更新科目：id={}, request={}", subjectId, request);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean updateSuccess = false;
        String errorMessage = null;
        FundSubject subject = null;

        try {
            subject = fundSubjectService.updateSubject(
                    request.getSubjectId(),
                    request.getSubjectCode(),
                    request.getSubjectName(),
                    request.getSubjectCategory(),
                    request.getBalanceDirection(),
                    request.getIsCashSubject(),
                    request.getAuxiliaryAccounting(),
                    request.getQuantityAccounting(),
                    request.getForeignCurrencyAccounting(),
                    request.getEnabled(),
                    request.getRemark(),
                    request.getVersion()
            );
            updateSuccess = true;
            return Result.success(subject);
        } catch (Exception e) {
            log.error("更新科目失败：id={}, request={}", subjectId, request, e);
            errorMessage = e.getMessage();
            return Result.error("更新科目失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("更新科目：科目ID=%s，科目名称=%s",
                        subjectId, request.getSubjectName());
                logRecordService.recordOperationLog("会计科目管理", "更新",
                        logContent, userId, ipAddress, updateSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录更新科目操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (updateSuccess && subject != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SUBJECT_UPDATE", subjectId != null ? subjectId.intValue() : null, "会计科目已更新", "更新", userId);
                } catch (Exception msgEx) {
                    log.warn("发送科目更新通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 删除科目
     */
    @RequireSimplePageEdit("财务管理:账户设置:科目管理:页面")
    @DeleteMapping("/{subjectId}")
    @ApiOperation("删除科目")
    public Result<Void> deleteSubject(@PathVariable Long subjectId, HttpServletRequest httpRequest) {

        log.info("删除科目：id={}", subjectId);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;

        try {
            fundSubjectService.deleteSubject(subjectId);
            deleteSuccess = true;
            return Result.success();
        } catch (Exception e) {
            log.error("删除科目失败：id={}", subjectId, e);
            errorMessage = e.getMessage();
            return Result.error("删除科目失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("删除科目：科目ID=%s", subjectId);
                logRecordService.recordOperationLog("会计科目管理", "删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录删除科目操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (deleteSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SUBJECT_DELETE", subjectId != null ? subjectId.intValue() : null, "会计科目已删除", "删除", userId);
                } catch (Exception msgEx) {
                    log.warn("发送科目删除通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 批量删除科目
     */
    @RequireSimplePageEdit("财务管理:账户设置:科目管理:页面")
    @DeleteMapping("/batch")
    @ApiOperation("批量删除科目")
    public Result<Integer> batchDeleteSubjects(@RequestBody List<Long> subjectIds, HttpServletRequest httpRequest) {

        log.info("批量删除科目：subjectIds={}", subjectIds);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;
        int successCount = 0;

        try {
            successCount = fundSubjectService.batchDeleteSubjects(subjectIds);
            deleteSuccess = true;
            return Result.success("成功删除" + successCount + "个科目", successCount);
        } catch (Exception e) {
            log.error("批量删除科目失败：subjectIds={}", subjectIds, e);
            errorMessage = e.getMessage();
            return Result.error("批量删除科目失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("批量删除科目：成功=%s，科目IDs=%s",
                        successCount, subjectIds != null ? subjectIds.toString() : "[]");
                logRecordService.recordOperationLog("会计科目管理", "批量删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录批量删除科目操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (deleteSuccess && successCount > 0) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SUBJECT_DELETE", null, "会计科目已批量删除", "批量删除", userId);
                } catch (Exception msgEx) {
                    log.warn("发送批量删除科目通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 批量更新科目状态
     */
    @PutMapping("/batch/status")
    @ApiOperation("批量更新科目状态")
    public Result<Void> batchUpdateSubjectStatus(
            @Valid @RequestBody FundSubjectBatchUpdateRequest request,
            HttpServletRequest httpRequest) {

        log.info("批量更新科目状态：{}", request);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean updateSuccess = false;
        String errorMessage = null;

        try {
            fundSubjectService.batchUpdateSubjectStatus(
                    request.getSubjectIds(),
                    request.getEnabled()
            );
            updateSuccess = true;
            return Result.success();
        } catch (Exception e) {
            log.error("批量更新科目状态失败：{}", request, e);
            errorMessage = e.getMessage();
            return Result.error("批量更新科目状态失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                int count = request.getSubjectIds() != null ? request.getSubjectIds().size() : 0;
                String status = Boolean.TRUE.equals(request.getEnabled()) ? "启用" : "禁用";
                String logContent = String.format("批量更新科目状态：%s，科目数量=%s", status, count);
                logRecordService.recordOperationLog("会计科目管理", "批量更新",
                        logContent, userId, ipAddress, updateSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录批量更新科目状态操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (updateSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SUBJECT_UPDATE", null, "会计科目状态已批量更新", "批量更新", userId);
                } catch (Exception msgEx) {
                    log.warn("发送科目状态更新通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 获取科目选项列表（用于下拉选择）
     */
    @GetMapping("/options")
    @ApiOperation("获取科目选项列表")
    public Result<List<FundSubjectOption>> getSubjectOptions() {

        log.info("获取科目选项列表");

        List<FundSubjectOption> options = fundSubjectService.getSubjectOptions();

        return Result.success(options);
    }

    /**
     * 搜索科目选项（用于下拉选择，支持科目编码模糊搜索）
     */
    @GetMapping("/search")
    @ApiOperation("搜索科目选项")
    public Result<List<FundSubjectOption>> searchSubjectOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {

        log.info("搜索科目选项：keyword={}, limit={}", keyword, limit);

        List<FundSubjectOption> options = fundSubjectService.searchSubjectOptions(keyword, limit);

        return Result.success(options);
    }

    /**
     * 根据科目编码查询科目
     */
    @GetMapping("/code/{subjectCode}")
    @ApiOperation("根据科目编码查询科目")
    public Result<FundSubject> getSubjectByCode(@PathVariable String subjectCode) {

        log.info("根据科目编码查询科目：{}", subjectCode);

        FundSubject subject = fundSubjectService.getSubjectByCode(subjectCode);

        return Result.success(subject);
    }

    /**
     * 查询子科目列表
     */
    @GetMapping("/{subjectCode}/children")
    @ApiOperation("查询子科目列表")
    public Result<List<FundSubject>> getChildSubjects(@PathVariable String subjectCode) {

        log.info("查询子科目列表：parentSubjectCode={}", subjectCode);

        List<FundSubject> children = fundSubjectService.getChildSubjects(subjectCode);

        return Result.success(children);
    }

    /**
     * 获取科目树形结构数据
     */
    @RequirePagePermission("财务管理:账户设置:科目管理:页面")
    @GetMapping("/tree")
    @ApiOperation("获取科目树形结构数据")
    public Result<List<FundSubjectListItemResponse>> getSubjectTree() {

        log.info("获取科目树形结构数据");

        List<FundSubjectListItemResponse> tree = fundSubjectService.getSubjectTree();

        return Result.success(tree);
    }

    /**
     * 生成科目编码
     */
    @GetMapping("/generate-code")
    @ApiOperation("生成科目编码")
    public Result<String> generateSubjectCode(@RequestParam(required = false) String parentSubjectCode) {

        log.info("生成科目编码：parentSubjectCode={}", parentSubjectCode);

        String subjectCode = fundSubjectService.generateSubjectCode(parentSubjectCode);

        return Result.success(subjectCode);
    }

    /**
     * 创建子科目
     */
    @PostMapping("/{parentSubjectCode}/children")
    @ApiOperation("创建子科目")
    public Result<FundSubject> createChildSubject(
            @PathVariable String parentSubjectCode,
            @Valid @RequestBody FundSubjectCreateRequest request,
            HttpServletRequest httpRequest) {

        log.info("创建子科目：parentSubjectCode={}, request={}", parentSubjectCode, request);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean createSuccess = false;
        String errorMessage = null;
        FundSubject subject = null;

        try {
            subject = fundSubjectService.createSubjectWithParent(
                    parentSubjectCode,
                    request.getSubjectCode(),
                    request.getSubjectName(),
                    request.getSubjectCategory(),
                    request.getBalanceDirection(),
                    request.getIsCashSubject(),
                    request.getAuxiliaryAccounting(),
                    request.getQuantityAccounting(),
                    request.getForeignCurrencyAccounting(),
                    request.getRemark()
            );
            createSuccess = true;
            return Result.success(subject);
        } catch (Exception e) {
            log.error("创建子科目失败：parentSubjectCode={}, request={}", parentSubjectCode, request, e);
            errorMessage = e.getMessage();
            return Result.error("创建子科目失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("创建子科目：父科目编码=%s，科目编码=%s，科目名称=%s",
                        parentSubjectCode, request.getSubjectCode(), request.getSubjectName());
                logRecordService.recordOperationLog("会计科目管理", "新增",
                        logContent, userId, ipAddress, createSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录创建子科目操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (createSuccess && subject != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SUBJECT_CREATE", subject.getSubjectId() != null ? subject.getSubjectId().intValue() : null, "会计子科目已创建", "新增", userId);
                } catch (Exception msgEx) {
                    log.warn("发送子科目创建通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 下载科目导入模板
     */
    @GetMapping("/template/download")
    @ApiOperation("下载科目导入模板")
    public void downloadSubjectTemplate(HttpServletRequest httpRequest, HttpServletResponse response) {

        log.info("下载科目导入模板");
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        try {
            fundSubjectService.downloadSubjectTemplate(response);

            logRecordService.recordOperationLog("会计科目管理", "导出",
                    "下载科目导入模板",
                    userId, ipAddress, true, null);
        } catch (Exception e) {
            log.error("下载科目导入模板失败", e);
            logRecordService.recordOperationLog("会计科目管理", "导出",
                    "下载科目导入模板失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            // 在异常处理中返回错误响应
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"下载模板失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 导入科目数据
     */
    @RequireSimplePageEdit("财务管理:账户设置:科目管理:页面")
    @PostMapping("/import")
    @ApiOperation("导入科目数据")
    public Result<ImportResult> importSubjects(@RequestParam("file") MultipartFile file,
                                                HttpServletRequest httpRequest) {

        log.info("收到导入请求，文件：{}，大小：{}，类型：{}",
                 file.getOriginalFilename(), file.getSize(), file.getContentType());
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean importSuccess = false;
        String errorMessage = null;
        ImportResult result = null;

        // 验证文件
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 验证文件类型
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return Result.error("只支持Excel文件（.xlsx或.xls格式）");
        }

        // 验证文件大小（限制10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件大小不能超过10MB");
        }

        try {
            result = fundSubjectService.importSubjectsFromExcel(file);
            log.info("导入处理完成，结果：总计{}条，成功{}条，失败{}条",
                     result.getTotalRecords(), result.getSuccessCount(), result.getFailureCount());

            if (result.isAllSuccess()) {
                log.info("导入完全成功");
                importSuccess = true;
                return Result.success("导入完成，共导入" + result.getSuccessCount() + "条数据", result);
            } else {
                log.info("导入部分成功，失败原因：{}", result.getErrorMessages());
                importSuccess = true; // 部分成功也视为成功
                return Result.success("导入完成，成功" + result.getSuccessCount() + "条，失败" + result.getFailureCount() + "条", result);
            }
        } catch (Exception e) {
            log.error("导入科目数据失败", e);
            errorMessage = e.getMessage();
            return Result.error("导入失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("导入科目数据：文件=%s，总计=%s，成功=%s，失败=%s",
                        fileName,
                        result != null ? result.getTotalRecords() : 0,
                        result != null ? result.getSuccessCount() : 0,
                        result != null ? result.getFailureCount() : 0);
                logRecordService.recordOperationLog("会计科目管理", "导入",
                        logContent, userId, ipAddress, importSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录导入科目操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (importSuccess && result != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SUBJECT_CREATE", null, "会计科目已导入", "导入", userId);
                } catch (Exception msgEx) {
                    log.warn("发送科目导入通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 导出科目数据为Excel文件
     */
    @RequirePagePermission("财务管理:账户设置:科目管理:页面")
    @GetMapping("/export")
    @ApiOperation("导出科目数据为Excel文件")
    public void exportSubjectsToExcel(
            @Valid FundSubjectPageRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        log.info("导出科目数据：{}", request);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        try {
            fundSubjectService.exportSubjectsToExcel(request, response);

            logRecordService.recordOperationLog("会计科目管理", "导出",
                    "导出科目数据",
                    userId, ipAddress, true, null);
        } catch (Exception e) {
            log.error("导出科目数据失败", e);
            logRecordService.recordOperationLog("会计科目管理", "导出",
                    "导出科目数据失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            // 在异常处理中返回错误响应
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }
}