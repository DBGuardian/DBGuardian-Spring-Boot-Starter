package com.erp.controller.finance;

import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundDiaryImportResult;
import com.erp.controller.finance.dto.FundDiaryRequest;
import com.erp.controller.finance.dto.FundDiaryResponse;
import com.erp.service.finance.FundDiaryService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.common.annotation.RequirePagePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 日记账控制器
 */
@Slf4j
@RestController
@RequestMapping("/fund")
@Api(tags = "日记账管理")
@Validated
public class FundDiaryController {

    @Autowired
    private FundDiaryService fundDiaryService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 获取日记账明细
     *
     * 接口名称：获取日记账明细
     * 功能描述：查询指定账户在指定账期的明细账数据
     * 接口地址：/api/fund/diary
     * 请求方式：GET
     *
     * 请求参数：
     * - account_id：账户ID（必填）
     * - organization_id：组织ID（必填）
     * - period_id：账期ID（可选，与year+period二选一）
     * - year：年份（可选，与period_id二选一）
     * - period：期间（可选，与period_id二选一）
     * - date_range_start：日期范围开始（可选）
     * - date_range_end：日期范围结束（可选）
     * - summary：摘要（可选，模糊查询）
     * - counterparty_name：往来单位账户名称（可选，模糊查询）
     *
     * 返回体 data：FundDiaryResponse
     */
    @RequirePagePermission("财务管理:资金管理:日记账:页面")
    @GetMapping("/diary")
    @ApiOperation(value = "获取日记账明细", notes = "查询指定账户在指定账期的明细账数据")
    public Result<FundDiaryResponse> getDiary(
            @RequestParam("account_id") Long accountId,
            @RequestParam("organization_id") Long organizationId,
            @RequestParam(value = "period_id", required = false) Long periodId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "period", required = false) Integer period,
            @RequestParam(value = "date_range_start", required = false) String dateRangeStart,
            @RequestParam(value = "date_range_end", required = false) String dateRangeEnd,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "counterparty_name", required = false) String counterpartyName
    ) {
        try {
            FundDiaryRequest req = new FundDiaryRequest();
            req.setAccountId(accountId);
            req.setOrganizationId(organizationId);
            req.setPeriodId(periodId);
            req.setYear(year);
            req.setMonth(period);
            req.setDateRangeStart(dateRangeStart);
            req.setDateRangeEnd(dateRangeEnd);
            req.setSummary(summary);
            req.setCounterpartyName(counterpartyName);

            FundDiaryResponse response = fundDiaryService.getDiary(req);
            return Result.success("获取日记账明细成功", response);
        } catch (Exception e) {
            log.error("获取日记账明细失败，accountId={}, periodId={}", accountId, periodId, e);
            return Result.error("获取日记账明细失败：" + e.getMessage());
        }
    }

    /**
     * 打印日记账
     *
     * 接口名称：打印日记账
     * 功能描述：生成日记账PDF文件并返回预览URL
     * 接口地址：/api/fund/diary/print
     * 请求方式：GET
     *
     * 请求参数：
     * - account_id：账户ID（必填）
     * - period_id：账期ID（可选，与year+period二选一）
     * - year：年份（可选，与period_id二选一）
     * - period：期间（可选，与period_id二选一）
     * - date_range_start：日期范围开始（可选）
     * - date_range_end：日期范围结束（可选）
     * - summary：摘要（可选，模糊查询）
     * - counterparty_name：往来单位账户名称（可选，模糊查询）
     *
     * 返回体 data：PDF文件预览URL（String）
     */
    @GetMapping("/diary/print")
    @ApiOperation(value = "打印日记账", notes = "生成日记账PDF文件并返回预览URL")
    public Result<String> printDiary(
            @RequestParam("account_id") Long accountId,
            @RequestParam(value = "period_id", required = false) Long periodId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "period", required = false) Integer period,
            @RequestParam(value = "date_range_start", required = false) String dateRangeStart,
            @RequestParam(value = "date_range_end", required = false) String dateRangeEnd,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "counterparty_name", required = false) String counterpartyName,
            HttpServletRequest request
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean printSuccess = false;
        String errorMessage = null;

        try {
            FundDiaryRequest req = new FundDiaryRequest();
            req.setAccountId(accountId);
            req.setPeriodId(periodId);
            req.setYear(year);
            req.setMonth(period);
            req.setDateRangeStart(dateRangeStart);
            req.setDateRangeEnd(dateRangeEnd);
            req.setSummary(summary);
            req.setCounterpartyName(counterpartyName);

            String previewUrl = fundDiaryService.generateDiaryPdf(req);
            printSuccess = true;
            return Result.success("PDF生成成功", previewUrl);
        } catch (Exception e) {
            log.error("打印日记账失败，accountId={}, periodId={}", accountId, periodId, e);
            errorMessage = e.getMessage();
            return Result.error("打印日记账失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("打印日记账：账户ID=%s，账期ID=%s，年份=%s，月份=%s",
                        accountId, periodId, year, period);
                logRecordService.recordOperationLog("日记账管理", "打印",
                        logContent, userId, ipAddress, printSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录打印日记账操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (printSuccess) {
                try {
                    String title = "日记账已生成PDF";
                    String content = String.format("日记账PDF已生成：账户ID=%s，年份=%s，月份=%s",
                            accountId, year, period);
                    messageNotificationService.sendBusinessNotification("财务", title, content, null, userId, "FUND_DIARY", null);
                } catch (Exception msgEx) {
                    log.warn("发送日记账PDF生成通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 连续打印日记账（批量打印）
     *
     * 接口名称：连续打印日记账
     * 功能描述：批量生成多个账户的日记账PDF文件并合并为一个PDF，返回预览URL
     * 接口地址：/api/fund/diary/batch-print
     * 请求方式：POST
     *
     * 请求参数：
     * ```json
     * [
     *   {
     *     "account_id": 1,
     *     "period_id": 3,
     *     "date_range_start": "2023-03-01",
     *     "date_range_end": "2023-03-31",
     *     "summary": "",
     *     "counterparty": ""
     *   },
     *   {
     *     "account_id": 2,
     *     "period_id": 3,
     *     "date_range_start": "2023-03-01",
     *     "date_range_end": "2023-03-31",
     *     "summary": "",
     *     "counterparty": ""
     *   }
     * ]
     * ```
     *
     * 返回体 data：PDF文件预览URL（String）
     */
    @PostMapping("/diary/batch-print")
    @ApiOperation(value = "连续打印日记账", notes = "批量生成多个账户的日记账PDF文件并合并为一个PDF")
    public Result<String> batchPrintDiary(@RequestBody java.util.List<FundDiaryRequest> requests,
                                          HttpServletRequest request) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean printSuccess = false;
        String errorMessage = null;

        try {
            if (requests == null || requests.isEmpty()) {
                return Result.error("账户列表不能为空");
            }

            String previewUrl = fundDiaryService.generateBatchDiaryPdf(requests);
            printSuccess = true;
            return Result.success("批量PDF生成成功", previewUrl);
        } catch (Exception e) {
            log.error("连续打印日记账失败，requests={}", requests, e);
            errorMessage = e.getMessage();
            return Result.error("连续打印日记账失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                int accountCount = requests != null ? requests.size() : 0;
                String logContent = String.format("批量打印日记账：账户数量=%d", accountCount);
                logRecordService.recordOperationLog("日记账管理", "批量打印",
                        logContent, userId, ipAddress, printSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录批量打印日记账操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (printSuccess) {
                try {
                    String title = "日记账已批量生成PDF";
                    String content = String.format("日记账批量PDF已生成：账户数量=%d", requests != null ? requests.size() : 0);
                    messageNotificationService.sendBusinessNotification("财务", title, content, null, userId, "FUND_DIARY", null);
                } catch (Exception msgEx) {
                    log.warn("发送日记账批量PDF生成通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 下载日记账导入模板
     *
     * 接口名称：下载日记账导入模板
     * 功能描述：下载Excel格式的日记账导入模板文件
     * 接口地址：/api/fund/diary/template/download
     * 请求方式：GET
     *
     * 请求参数：无
     *
     * 返回体：Excel文件流
     */
    @GetMapping("/diary/template/download")
    @ApiOperation(value = "下载日记账导入模板", notes = "下载Excel格式的日记账导入模板文件")
    public void downloadFundDiaryTemplate(HttpServletResponse response, HttpServletRequest request) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean downloadSuccess = false;
        String errorMessage = null;

        try {
            fundDiaryService.downloadFundDiaryTemplate(response);
            downloadSuccess = true;
        } catch (Exception e) {
            log.error("下载日记账导入模板失败", e);
            errorMessage = e.getMessage();
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"下载模板失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        } finally {
            // 记录操作日志
            try {
                logRecordService.recordOperationLog("日记账管理", "下载模板",
                        "下载日记账导入模板", userId, ipAddress, downloadSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录下载模板操作日志失败", logEx);
            }
        }
    }

    /**
     * Excel导入日记账数据
     *
     * 接口名称：Excel导入日记账数据
     * 功能描述：上传Excel文件导入日记账数据，全量事务：任意一条失败则整体回滚
     * 接口地址：/api/fund/diary/excel-import
     * 请求方式：POST
     *
     * 请求参数：
     * - excelFile：Excel文件（form-data）
     * - accountId：账户ID（form-data）
     * - periodId：账期ID（form-data）
     *
     * 返回体 data：导入结果统计信息
     */
    @PostMapping("/diary/excel-import")
    @ApiOperation(value = "Excel导入日记账数据", notes = "上传Excel文件导入日记账数据，全量事务：任意一条失败则整体回滚")
    public Result<FundDiaryImportResult> excelImportFundDiary(
            @RequestParam("excelFile") org.springframework.web.multipart.MultipartFile excelFile,
            @RequestParam("accountId") Long accountId,
            @RequestParam("periodId") Long periodId,
            HttpServletRequest request
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean importSuccess = false;
        String errorMessage = null;
        FundDiaryImportResult importResult = null;

        try {
            log.info("开始Excel导入日记账数据，accountId={}, periodId={}, fileName={}",
                    accountId, periodId, excelFile.getOriginalFilename());

            importResult = fundDiaryService.excelImportFundDiary(excelFile, accountId, periodId);
            importSuccess = true;
            return Result.success("导入完成", importResult);
        } catch (Exception e) {
            log.error("Excel导入日记账数据失败，accountId={}, periodId={}, fileName={}",
                    accountId, periodId, excelFile.getOriginalFilename(), e);
            errorMessage = e.getMessage();
            return Result.error("导入失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("Excel导入日记账：账户ID=%s，账期ID=%s，文件名=%s，成功=%s",
                        accountId, periodId, excelFile.getOriginalFilename(), importSuccess);
                logRecordService.recordOperationLog("日记账管理", "导入",
                        logContent, userId, ipAddress, importSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录导入日记账操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (importSuccess && importResult != null) {
                try {
                    String title = "日记账已导入";
                    String content = String.format("日记账已导入：账户ID=%s，成功导入%d条，失败%d条，跳过%d条",
                            accountId,
                            importResult.getSuccessCount() != null ? importResult.getSuccessCount() : 0,
                            importResult.getFailureCount() != null ? importResult.getFailureCount() : 0,
                            importResult.getSkipCount() != null ? importResult.getSkipCount() : 0);
                    messageNotificationService.sendBusinessNotification("财务", title, content, null, userId, "FUND_DIARY", null);
                } catch (Exception msgEx) {
                    log.warn("发送日记账导入通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 导出日记账Excel文件
     *
     * 接口名称：导出日记账Excel文件
     * 功能描述：根据查询条件导出日记账数据为Excel文件
     * 接口地址：/api/fund/diary/excel-export
     * 请求方式：GET
     *
     * 请求参数：
     * - account_id：账户ID（必填）
     * - organization_id：组织ID（必填）
     * - period_id：账期ID（可选，与year+period二选一）
     * - year：年份（可选，与period_id二选一）
     * - period：期间（可选，与period_id二选一）
     * - date_range_start：日期范围开始（可选）
     * - date_range_end：日期范围结束（可选）
     * - summary：摘要（可选，模糊查询）
     * - counterparty_name：往来单位账户名称（可选，模糊查询）
     *
     * 返回体：Excel文件流
     */
    @GetMapping("/diary/excel-export")
    @ApiOperation(value = "导出日记账Excel文件", notes = "根据查询条件导出日记账数据为Excel文件")
    public void exportFundDiaryExcel(
            @RequestParam("account_id") Long accountId,
            @RequestParam("organization_id") Long organizationId,
            @RequestParam(value = "period_id", required = false) Long periodId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "period", required = false) Integer period,
            @RequestParam(value = "date_range_start", required = false) String dateRangeStart,
            @RequestParam(value = "date_range_end", required = false) String dateRangeEnd,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "counterparty_name", required = false) String counterpartyName,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean exportSuccess = false;
        String errorMessage = null;

        try {
            FundDiaryRequest req = new FundDiaryRequest();
            req.setAccountId(accountId);
            req.setOrganizationId(organizationId);
            req.setPeriodId(periodId);
            req.setYear(year);
            req.setMonth(period);
            req.setDateRangeStart(dateRangeStart);
            req.setDateRangeEnd(dateRangeEnd);
            req.setSummary(summary);
            req.setCounterpartyName(counterpartyName);

            fundDiaryService.exportFundDiaryExcel(req, response);
            exportSuccess = true;
        } catch (Exception e) {
            log.error("导出日记账Excel失败，accountId={}, periodId={}", accountId, periodId, e);
            errorMessage = e.getMessage();
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("导出日记账Excel：账户ID=%s，账期ID=%s，年份=%s，月份=%s",
                        accountId, periodId, year, period);
                logRecordService.recordOperationLog("日记账管理", "导出",
                        logContent, userId, ipAddress, exportSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录导出日记账操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (exportSuccess) {
                try {
                    String title = "日记账已导出";
                    String content = String.format("日记账已导出Excel：账户ID=%s，年份=%s，月份=%s",
                            accountId, year, period);
                    messageNotificationService.sendBusinessNotification("财务", title, content, null, userId, "FUND_DIARY", null);
                } catch (Exception msgEx) {
                    log.warn("发送日记账导出通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 导出日记账回单文件（ZIP包）
     *
     * 接口名称：导出日记账回单文件
     * 功能描述：从当前账期中查询哪些流水有回单文件编号，然后从服务器中找到这些文件并打包成ZIP下载。如果回单文件编号有值但是服务器中没有对应文件，则忽略，找下一个。如果找到的文件为0个，才弹出提示，没有找到对应的回单文件。
     * 接口地址：/api/fund/diary/receipts-export
     * 请求方式：GET
     *
     * 请求参数：
     * - period_id：账期ID（必填）
     * - organization_id：组织ID（必填）
     *
     * 返回体：ZIP文件流
     */
    @GetMapping("/diary/receipts-export")
    @ApiOperation(value = "导出日记账回单文件", notes = "从当前账期中查询哪些流水有回单文件编号，然后从服务器中找到这些文件并打包成ZIP下载")
    public void exportFundDiaryReceipts(
            @RequestParam("period_id") Long periodId,
            @RequestParam("organization_id") Long organizationId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(request);
        boolean exportSuccess = false;
        String errorMessage = null;

        try {
            fundDiaryService.exportFundDiaryReceipts(periodId, organizationId, response);
            exportSuccess = true;
        } catch (Exception e) {
            log.error("导出日记账回单文件失败，periodId={}, organizationId={}", periodId, organizationId, e);
            errorMessage = e.getMessage();
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("导出日记账回单文件：账期ID=%s，组织ID=%s", periodId, organizationId);
                logRecordService.recordOperationLog("日记账管理", "导出回单",
                        logContent, userId, ipAddress, exportSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录导出回单文件操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (exportSuccess) {
                try {
                    String title = "日记账回单已导出";
                    String content = String.format("日记账回单已导出：账期ID=%s，组织ID=%s", periodId, organizationId);
                    messageNotificationService.sendBusinessNotification("财务", title, content, null, userId, "FUND_DIARY", null);
                } catch (Exception msgEx) {
                    log.warn("发送日记账回单导出通知失败", msgEx);
                }
            }
        }
    }
}

