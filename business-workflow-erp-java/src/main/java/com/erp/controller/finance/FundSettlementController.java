package com.erp.controller.finance;

import com.erp.controller.settlement.dto.*;
import com.erp.service.finance.FundSettlementService;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 结账管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/fund/settlement")
@Api(tags = "结账管理")
public class FundSettlementController {

    @Autowired
    private FundSettlementService fundSettlementService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 检查并结账
     * 功能描述：检查指定组织的未结账账期余额，如果通过则结账（目前只实现检查部分）
     * 入参：SettlementCheckAndSettleRequest
     * 返回参数：SettlementCheckAndSettleResponse
     * url地址：/api/fund/settlement/check-and-settle
     * 请求方式：POST
     */
    @PostMapping("/check-and-settle")
    @ApiOperation("检查并结账")
    public Result<SettlementCheckAndSettleResponse> checkAndSettle(
            @RequestBody SettlementCheckAndSettleRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean checkSuccess = false;
        String errorMessage = null;
        SettlementCheckAndSettleResponse response = null;

        try {
            log.info("检查并结账，organizationId={}", request.getOrganizationId());
            response = fundSettlementService.checkAndSettle(request);
            checkSuccess = true;

            // 发送消息通知
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "FUND_SETTLEMENT_CHECK", request.getOrganizationId().intValue(), "资金结算检查完成", "检查", userId);
            } catch (Exception msgEx) {
                log.warn("发送资金结算检查通知失败", msgEx);
            }

            return Result.success("检查完成", response);
        } catch (Exception e) {
            log.error("检查并结账失败，organizationId={}", request.getOrganizationId(), e);
            errorMessage = e.getMessage();
            return Result.error("检查失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("检查并结账：组织ID=%s", request.getOrganizationId());
                logRecordService.recordOperationLog("结账管理", "检查结账",
                        logContent, userId, ipAddress, checkSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录检查结账操作日志失败", logEx);
            }
        }
    }

    /**
     * 获取检查项设置
     * 功能描述：获取结账检查项设置
     * 入参：无
     * 返回参数：SettlementCheckItemResponse
     * url地址：/api/fund/settlement/check-items
     * 请求方式：GET
     */
    @GetMapping("/check-items")
    @ApiOperation("获取检查项设置")
    public Result<SettlementCheckItemResponse> getCheckItems() {
        try {
            SettlementCheckItemResponse response = fundSettlementService.getCheckItems();
            return Result.success("获取检查项设置成功", response);
        } catch (Exception e) {
            log.error("获取检查项设置失败", e);
            return Result.error("获取检查项设置失败：" + e.getMessage());
        }
    }

    /**
     * 更新检查项设置
     * 功能描述：更新结账检查项设置
     * 入参：SettlementCheckItemUpdateRequest
     * 返回参数：无
     * url地址：/api/fund/settlement/check-items
     * 请求方式：POST
     */
    @PostMapping("/check-items")
    @ApiOperation("更新检查项设置")
    public Result<Void> updateCheckItems(@RequestBody SettlementCheckItemUpdateRequest request,
                                         HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean updateSuccess = false;
        String errorMessage = null;

        try {
            fundSettlementService.updateCheckItems(request);
            updateSuccess = true;
            return Result.success("更新检查项设置成功", null);
        } catch (Exception e) {
            log.error("更新检查项设置失败", e);
            errorMessage = e.getMessage();
            return Result.error("更新检查项设置失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                logRecordService.recordOperationLog("结账管理", "更新",
                        "更新检查项设置", userId, ipAddress, updateSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录更新检查项操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (updateSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SETTLEMENT_UPDATE", null, "结账检查项已更新", "更新", userId);
                } catch (Exception msgEx) {
                    log.warn("发送检查项更新通知失败", msgEx);
                }
            }
        }
    }

    
    
    /**
     * 单个账期结账
     * 功能描述：对指定的单个账期进行结账
     * 入参：SettlementRequest (使用periodId字段)
     * 返回参数：SettlementResponse
     * url地址：/api/fund/settlement/settle-period
     * 请求方式：POST
     */
    @PostMapping("/settle-period")
    @ApiOperation("单个账期结账")
    public Result<SettlementResponse> settlePeriod(@RequestBody SettlementRequest request,
                                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean settleSuccess = false;
        String errorMessage = null;
        SettlementResponse response = null;

        try {
            log.info("单个账期结账，periodId={}", request.getPeriodId());
            response = fundSettlementService.settlePeriod(request);
            settleSuccess = true;
            return Result.success("结账成功", response);
        } catch (Exception e) {
            log.error("单个账期结账失败，periodId={}", request.getPeriodId(), e);
            errorMessage = e.getMessage();
            return Result.error("结账失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("单个账期结账：账期ID=%s", request.getPeriodId());
                logRecordService.recordOperationLog("结账管理", "结账",
                        logContent, userId, ipAddress, settleSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录结账操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (settleSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SETTLEMENT_SETTLE", request.getPeriodId().intValue(), "账期已结账", "结账", userId);
                } catch (Exception msgEx) {
                    log.warn("发送结账通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 单个账期反结账
     * 功能描述：对指定的单个账期进行反结账
     * 入参：SettlementReverseRequest (使用periodId字段)
     * 返回参数：SettlementReverseResponse
     * url地址：/api/fund/settlement/reverse-period
     * 请求方式：POST
     */
    @PostMapping("/reverse-period")
    @ApiOperation("单个账期反结账")
    public Result<SettlementReverseResponse> reverseSettlementPeriod(@RequestBody SettlementReverseRequest request,
                                                                      HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean reverseSuccess = false;
        String errorMessage = null;
        SettlementReverseResponse response = null;

        try {
            log.info("单个账期反结账，periodId={}", request.getPeriodId());
            response = fundSettlementService.reverseSettlementPeriod(request);
            reverseSuccess = true;
            return Result.success("反结账成功", response);
        } catch (Exception e) {
            log.error("单个账期反结账失败，periodId={}", request.getPeriodId(), e);
            errorMessage = e.getMessage();
            return Result.error("反结账失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("单个账期反结账：账期ID=%s", request.getPeriodId());
                logRecordService.recordOperationLog("结账管理", "反结账",
                        logContent, userId, ipAddress, reverseSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录反结账操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (reverseSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_SETTLEMENT_REVERSE", request.getPeriodId().intValue(), "账期已反结账", "反结账", userId);
                } catch (Exception msgEx) {
                    log.warn("发送反结账通知失败", msgEx);
                }
            }
        }
    }
}

