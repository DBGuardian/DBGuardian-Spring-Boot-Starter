package com.erp.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundPeriodInitRequest;
import com.erp.controller.finance.dto.FundPeriodInitResponse;
import com.erp.controller.finance.dto.FundPeriodListItemResponse;
import com.erp.controller.finance.dto.FundPeriodPageRequest;
import com.erp.service.finance.FundPeriodService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireSimplePageEdit;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 账期管理控制器
 *
 * 账期管理已重新设计，后续将重新实现相关功能
 */
@Slf4j
@RestController
@RequestMapping("/fund")
@Api(tags = "账期管理")
@Validated
public class FundPeriodController {

    @Resource
    private FundPeriodService fundPeriodService;

    @Resource
    private ILogRecordService logRecordService;

    @Resource
    private MessageNotificationService messageNotificationService;

    /**
     * 初始化账期
     *
     * @param request 初始化账期请求
     * @return 初始化结果
     */
    @RequireSimplePageEdit("财务管理:账户设置:账期管理:页面")
    @PostMapping("/periods/init")
    @ApiOperation("初始化账期")
    public Result<FundPeriodInitResponse> initFundPeriods(@RequestBody @Validated FundPeriodInitRequest request,
                                                          HttpServletRequest httpRequest) {
        log.info("接收到初始化账期请求：{}", request);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean initSuccess = false;
        String errorMessage = null;
        FundPeriodInitResponse response = null;

        try {
            response = fundPeriodService.initFundPeriods(
                request.getOrganizationId(),
                request.getYear(),
                request.getOverwrite()
            );
            initSuccess = true;

            log.info("初始化账期成功，组织ID：{}，年份：{}，创建数量：{}",
                    request.getOrganizationId(), request.getYear(), response.getCreatedCount());

            return Result.success(response);
        } catch (Exception e) {
            log.error("初始化账期失败，组织ID：{}，年份：{}，错误：{}",
                     request.getOrganizationId(), request.getYear(), e.getMessage(), e);
            errorMessage = e.getMessage();
            return Result.error(500, "初始化账期失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("初始化账期：组织ID=%s，年份=%s，覆盖=%s，创建数量=%s",
                        request.getOrganizationId(), request.getYear(),
                        request.getOverwrite() != null ? request.getOverwrite() : false,
                        response != null ? response.getCreatedCount() : 0);
                logRecordService.recordOperationLog("账期管理", "新增",
                        logContent, userId, ipAddress, initSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录初始化账期操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (initSuccess && response != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_PERIOD_INIT", null, "账期已初始化", "初始化", userId);
                } catch (Exception msgEx) {
                    log.warn("发送账期初始化通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 分页查询账期列表
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @RequirePagePermission({
            "财务管理:账户设置:页面",
            "财务管理:账户设置:账期管理:页面",
            "财务管理:资金管理:日记账:页面",
            "财务管理:资金管理:汇总表:页面"
    })
    @PostMapping("/periods/page")
    @ApiOperation("分页查询账期列表")
    public Result<IPage<FundPeriodListItemResponse>> getFundPeriodPage(@RequestBody @Validated FundPeriodPageRequest request) {
        log.info("接收到分页查询账期列表请求：{}", request);

        try {
            IPage<FundPeriodListItemResponse> resultPage = fundPeriodService.getFundPeriodPage(request);

            log.info("分页查询账期列表成功，组织ID：{}，返回数量：{}",
                     request.getOrganizationId(), resultPage != null && resultPage.getRecords() != null 
                     ? resultPage.getRecords().size() : 0);

            return Result.success(resultPage);
        } catch (Exception e) {
            log.error("分页查询账期列表失败，组织ID：{}，错误：{}",
                     request.getOrganizationId(), e.getMessage(), e);
            return Result.error(500, "分页查询账期列表失败：" + e.getMessage());
        }
    }

    /**
     * 查询账期年份列表
     *
     * @return 年份列表
     */
    @RequirePagePermission({
            "财务管理:账户设置:页面",
            "财务管理:账户设置:账期管理:页面",
            "财务管理:资金管理:日记账:页面",
            "财务管理:资金管理:汇总表:页面"
    })
    @GetMapping("/periods/years")
    @ApiOperation("查询账期年份列表")
    public Result<java.util.List<Integer>> getFundPeriodYears() {
        log.info("接收到查询账期年份列表请求");

        try {
            java.util.List<Integer> years = fundPeriodService.getFundPeriodYears();

            log.info("查询账期年份列表成功，返回年份数量：{}", years.size());

            return Result.success(years);
        } catch (Exception e) {
            log.error("查询账期年份列表失败，错误：{}", e.getMessage(), e);
            return Result.error(500, "查询账期年份列表失败：" + e.getMessage());
        }
    }

    /**
     * 结账
     *
     * @param periodId 账期编号
     * @return 操作结果
     */
    @RequireSimplePageEdit("财务管理:账户设置:账期管理:页面")
    @PostMapping("/periods/{periodId}/settle")
    @ApiOperation("结账")
    public Result<String> settlePeriod(@PathVariable Long periodId,
                                     HttpServletRequest httpRequest) {
        log.info("接收到结账请求，账期编号：{}", periodId);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean success = false;
        String errorMessage = null;

        try {
            fundPeriodService.settlePeriod(periodId);
            success = true;
            log.info("结账成功，账期编号：{}", periodId);

            // 发送消息通知
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "FUND_PERIOD_SETTLE", periodId.intValue(), "账期已结账", "结账", userId);
            } catch (Exception msgEx) {
                log.warn("发送结账通知失败", msgEx);
            }

            return Result.success("结账成功");
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("结账失败，账期编号：{}，错误：{}", periodId, e.getMessage(), e);
            return Result.error(500, "结账失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("结账：账期编号=%s", periodId);
                logRecordService.recordOperationLog("账期管理", "结账",
                        logContent, userId, ipAddress, success, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录结账操作日志失败", logEx);
            }
        }
    }

    /**
     * 取消结账（反结账）
     *
     * @param periodId 账期编号
     * @return 操作结果
     */
    @RequireSimplePageEdit("财务管理:账户设置:账期管理:页面")
    @PostMapping("/periods/{periodId}/reverse-settle")
    @ApiOperation("取消结账")
    public Result<String> reverseSettlePeriod(@PathVariable Long periodId,
                                             HttpServletRequest httpRequest) {
        log.info("接收到取消结账请求，账期编号：{}", periodId);
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean success = false;
        String errorMessage = null;

        try {
            fundPeriodService.reverseSettlePeriod(periodId);
            success = true;
            log.info("取消结账成功，账期编号：{}", periodId);

            // 发送消息通知
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "FUND_PERIOD_REVERSE", periodId.intValue(), "账期已取消结账", "反结账", userId);
            } catch (Exception msgEx) {
                log.warn("发送取消结账通知失败", msgEx);
            }

            return Result.success("取消结账成功");
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("取消结账失败，账期编号：{}，错误：{}", periodId, e.getMessage(), e);
            return Result.error(500, "取消结账失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("取消结账：账期编号=%s", periodId);
                logRecordService.recordOperationLog("账期管理", "取消结账",
                        logContent, userId, ipAddress, success, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录取消结账操作日志失败", logEx);
            }
        }
    }
}

