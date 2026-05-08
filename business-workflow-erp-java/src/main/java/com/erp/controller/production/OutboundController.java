package com.erp.controller.production;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.production.dto.AuditOutboundRequest;
import com.erp.controller.production.dto.CreateOutboundRequest;
import com.erp.controller.production.dto.OutboundDetailResponse;
import com.erp.controller.production.dto.OutboundListResponse;
import com.erp.controller.production.dto.OutboundPageRequest;
import com.erp.service.production.OutboundService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 出库单管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/warehouse/outbound")
@Api(tags = "出库单管理")
public class OutboundController {

    @Autowired
    private OutboundService outboundService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 创建出库单
     */
    @PostMapping("/create")
    @ApiOperation(value = "创建出库单", notes = "新增出库单，包含出库危废明细，后端自动生成出库单号，状态默认为待审核")
    public Result<OutboundDetailResponse> createOutbound(
            @Valid @RequestBody CreateOutboundRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ip = logRecordService.getClientIp(httpRequest);
        try {
            OutboundDetailResponse response = outboundService.createOutbound(request);
            logRecordService.recordOperationLog("出库单管理", "新增",
                    "创建出库单：出库单号=" + response.getOutboundNo(), userId, ip, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "OUTBOUND_CREATE",
                        response.getOutboundId(),
                        String.format("出库单【%s】", response.getOutboundNo()),
                        "新增",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送创建出库单通知失败", msgEx);
            }
            return Result.success("创建出库单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("出库单管理", "新增",
                    "创建出库单失败", userId, ip, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("创建出库单失败", e);
            logRecordService.recordOperationLog("出库单管理", "新增",
                    "创建出库单失败", userId, ip, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "创建出库单失败：" + e.getMessage());
        }
    }

    /**
     * 更新出库单
     */
    @PostMapping("/update")
    @ApiOperation(value = "更新出库单", notes = "更新出库单信息，仅限待审核状态")
    public Result<OutboundDetailResponse> updateOutbound(
            @Valid @RequestBody CreateOutboundRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ip = logRecordService.getClientIp(httpRequest);
        try {
            OutboundDetailResponse response = outboundService.updateOutbound(request);
            logRecordService.recordOperationLog("出库单管理", "修改",
                    "更新出库单：出库单号=" + response.getOutboundNo(), userId, ip, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "OUTBOUND_UPDATE",
                        response.getOutboundId(),
                        String.format("出库单【%s】", response.getOutboundNo()),
                        "更新",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送更新出库单通知失败", msgEx);
            }
            return Result.success("更新出库单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("出库单管理", "修改",
                    "更新出库单失败", userId, ip, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新出库单失败", e);
            logRecordService.recordOperationLog("出库单管理", "修改",
                    "更新出库单失败", userId, ip, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新出库单失败：" + e.getMessage());
        }
    }

    /**
     * 获取出库单详情
     */
    @GetMapping("/detail")
    @ApiOperation(value = "获取出库单详情", notes = "根据出库单编号或出库单号获取详细信息")
    public Result<OutboundDetailResponse> getOutboundDetail(
            @RequestParam(required = false) Integer outboundId,
            @RequestParam(required = false) String outboundNo,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ip = logRecordService.getClientIp(httpRequest);
        try {
            OutboundDetailResponse response = outboundService.getOutboundDetail(outboundId, outboundNo);
            return Result.success("获取出库单详情成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("出库单管理", "查看",
                    "获取出库单详情失败", userId, ip, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取出库单详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取出库单详情失败：" + e.getMessage());
        }
    }

    /**
     * 审核出库单
     */
    @PostMapping("/audit")
    @ApiOperation(value = "审核出库单", notes = "审核通过后扣减库存并回写出库明细扣减前后快照")
    public Result<Void> auditOutbound(
            @Valid @RequestBody AuditOutboundRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ip = logRecordService.getClientIp(httpRequest);
        try {
            outboundService.auditOutbound(request);
            logRecordService.recordOperationLog("出库单管理", "审核",
                    "审核出库单：出库单号=" + request.getOutboundNo() + "，结果=" + request.getAuditResult(),
                    userId, ip, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                String auditAction = "通过".equals(request.getAuditResult()) ? "审核通过" : "审核驳回";
                messageNotificationService.sendAuditResultNotification(
                        "OUTBOUND_AUDIT_RESULT",
                        null,
                        String.format("出库单【%s】", request.getOutboundNo()),
                        auditAction,
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送审核出库单通知失败", msgEx);
            }
            return Result.success("出库单审核成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("出库单管理", "审核",
                    "审核出库单失败：出库单号=" + request.getOutboundNo(),
                    userId, ip, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审核出库单失败", e);
            logRecordService.recordOperationLog("出库单管理", "审核",
                    "审核出库单失败：出库单号=" + request.getOutboundNo(),
                    userId, ip, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "审核出库单失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询出库单列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "分页查询出库单列表", notes = "支持按关键词、出库类型、状态、去向类型、时间范围等条件筛选")
    public Result<OutboundListResponse> getOutboundList(OutboundPageRequest request) {
        try {
            if (request == null) request = new OutboundPageRequest();
            if (request.getPage() == null || request.getPage() <= 0) request.setPage(1);
            if (request.getSize() == null || request.getSize() <= 0) request.setSize(20);
            OutboundListResponse response = outboundService.getOutboundPage(request);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询出库单列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }
}
