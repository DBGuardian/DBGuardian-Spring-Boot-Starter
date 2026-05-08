package com.erp.controller.finance;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.controller.finance.dto.*;
import com.erp.controller.settlement.dto.SettlementInvoiceSummaryResponse;
import com.erp.service.finance.InvoiceNoticeService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 开票通知单管理控制器
 *
 * @author ERP System
 * @date 2026-01-06
 */
@Slf4j
@RestController
@RequestMapping("/finance/invoice-notices")
@Api(tags = "开票通知单管理")
@Validated
public class InvoiceNoticeController {

    @Autowired
    private InvoiceNoticeService invoiceNoticeService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @Autowired
    private com.erp.mapper.finance.SettlementInvoiceRelMapper settlementInvoiceRelMapper;

    /**
     * 获取开票通知单分页列表
     * 功能描述：分页查询开票通知单列表，支持多条件筛选
     * 入参：InvoiceNoticePageRequest
     * 返回参数：分页结果
     * url地址：/api/finance/invoice-notices
     * 请求方式：GET
     */
    @RequirePagePermission("业务管理:开票通知:页面")
    @GetMapping
    @ApiOperation(value = "获取开票通知单分页列表", notes = "分页查询开票通知单列表，支持多条件筛选")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InvoiceNoticePageResponse>> getInvoiceNoticeList(
            @Valid InvoiceNoticePageRequest request) {
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<InvoiceNoticePageResponse> page = invoiceNoticeService.getInvoiceNoticePageList(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询开票通知单列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询开票通知单列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取开票通知单详情
     * 功能描述：根据通知单ID查询开票通知单详细信息
     * 入参：noticeId
     * 返回参数：InvoiceNoticeDetailResponse
     * url地址：/api/finance/invoice-notices/{noticeId}
     * 请求方式：GET
     */
    @RequireActionPermission("业务管理:开票通知:查看")
    @GetMapping("/{noticeId}")
    @ApiOperation(value = "获取开票通知单详情", notes = "根据通知单ID查询开票通知单详细信息")
    public Result<InvoiceNoticeDetailResponse> getInvoiceNoticeDetail(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId) {
        try {
            InvoiceNoticeDetailResponse response = invoiceNoticeService.getInvoiceNoticeDetail(noticeId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取开票通知单详情失败：noticeId={}", noticeId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取开票通知单详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建开票通知单
     * 功能描述：创建新的开票通知单，创建后默认进入待审核状态
     * 入参：InvoiceNoticeCreateRequest（status字段保留兼容，创建后统一为待审核状态）
     * 返回参数：创建结果
     * url地址：/api/finance/invoice-notices
     * 请求方式：POST
     */
    @RequireActionPermission("业务管理:开票通知:新增")
    @PostMapping
    @ApiOperation(value = "创建开票通知单", notes = "创建新的开票通知单，创建后默认进入待审核状态")
    public Result<InvoiceNoticeCreateResponse> createInvoiceNotice(
            @Valid @RequestBody InvoiceNoticeCreateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            InvoiceNoticeCreateResponse response = invoiceNoticeService.createInvoiceNotice(request);
            String action = "保存";
            logRecordService.recordOperationLog("开票通知单管理", "新增",
                    action + "：通知单号=" + (response.getNoticeNo() != null ? response.getNoticeNo() : "ID=" + response.getNoticeId()),
                    userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "INVOICE_NOTICE_CREATE",
                        response.getNoticeId(),
                        String.format("开票通知单已创建：通知单号=%s",
                                response.getNoticeNo() != null ? response.getNoticeNo() : "ID=" + response.getNoticeId()),
                        "新增",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送开票通知单创建通知失败", msgEx);
            }

            String message = "保存成功";
            return Result.success(message, response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "新增",
                    "创建开票通知单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("创建开票通知单失败", e);
            logRecordService.recordOperationLog("开票通知单管理", "新增",
                    "创建开票通知单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "创建开票通知单失败：" + e.getMessage());
        }
    }

    /**
     * 更新开票通知单
     * 功能描述：更新待审核、审核中或已驳回状态的通知单
     * 入参：{ noticeId: number, data: InvoiceNoticeUpdateRequest }
     * 返回参数：更新结果
     * url地址：/api/finance/invoice-notices/{noticeId}/update
     * 请求方式：POST
     * 注意：乐观锁版本号由MyBatis-Plus的@Version注解自动处理，不需要显式传入version字段
     */
    @RequireActionPermission("业务管理:开票通知:修改")
    @PostMapping("/{noticeId}/update")
    @ApiOperation(value = "更新开票通知单", notes = "更新待审核、审核中或已驳回状态的通知单")
    public Result<Void> updateInvoiceNotice(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            @Valid @RequestBody InvoiceNoticeUpdateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            invoiceNoticeService.updateInvoiceNotice(noticeId, request);
            logRecordService.recordOperationLog("开票通知单管理", "更新",
                    "更新开票通知单：通知单ID=" + noticeId, userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "INVOICE_NOTICE_UPDATE",
                        noticeId,
                        String.format("开票通知单已更新：通知单ID=%d", noticeId),
                        "更新",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送开票通知单更新通知失败", msgEx);
            }

            return Result.success("更新成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "更新",
                    "更新开票通知单失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新开票通知单失败：noticeId={}", noticeId, e);
            logRecordService.recordOperationLog("开票通知单管理", "更新",
                    "更新开票通知单失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新开票通知单失败：" + e.getMessage());
        }
    }

    /**
     * 提交审批
     * 功能描述：提交开票通知单进行审批
     * 入参：{ noticeId: number, data: InvoiceNoticeSubmitRequest }
     * 返回参数：提交结果
     * url地址：/api/finance/invoice-notices/{noticeId}/submit
     * 请求方式：POST
     */
    @RequireActionPermission("业务管理:开票通知:审批")
    @PostMapping("/{noticeId}/submit")
    @ApiOperation(value = "提交审批", notes = "提交开票通知单进行审批，提交后状态进入审核中")
    public Result<Void> submitInvoiceNotice(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            @Valid @RequestBody InvoiceNoticeSubmitRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            invoiceNoticeService.submitInvoiceNotice(noticeId, request);
            logRecordService.recordOperationLog("开票通知单管理", "提交审批",
                    "提交审批：通知单ID=" + noticeId, userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendApprovalSubmitNotification(
                        "INVOICE_NOTICE_SUBMIT",
                        noticeId,
                        String.format("开票通知单已提交审批：通知单ID=%d", noticeId),
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送开票通知单提交审批通知失败", msgEx);
            }

            // 提交后进入审核中状态
            return Result.success("提交审批成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "提交审批",
                    "提交审批失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("提交审批失败：noticeId={}", noticeId, e);
            logRecordService.recordOperationLog("开票通知单管理", "提交审批",
                    "提交审批失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "提交审批失败：" + e.getMessage());
        }
    }

    /**
     * 批量提交审核
     * 功能描述：批量提交开票通知单进行审核，仅支持待审核/已驳回状态
     * 入参：{ noticeIds: number[] }
     * 返回参数：提交结果
     * url地址：/api/finance/invoice-notices/batch/submit
     * 请求方式：POST
     */
    @RequireActionPermission("业务管理:开票通知:审批")
    @PostMapping("/batch/submit")
    @ApiOperation(value = "批量提交审核", notes = "批量提交开票通知单进行审核，仅支持待审核或已驳回状态，提交后状态进入审核中")
    public Result<Void> batchSubmitInvoiceNotice(
            @RequestBody java.util.Map<String, java.util.List<Integer>> request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        java.util.List<Integer> noticeIds = request.get("noticeIds");
        try {
            if (noticeIds == null || noticeIds.isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "通知单ID列表不能为空");
            }
            invoiceNoticeService.batchSubmitInvoiceNotice(noticeIds);
            logRecordService.recordOperationLog("开票通知单管理", "批量提交审核",
                    "批量提交审核：通知单数量=" + noticeIds.size(), userId, ipAddress, true, null);
            return Result.success("批量提交审核成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "批量提交审核",
                    "批量提交审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量提交审核失败：noticeIds={}", noticeIds, e);
            logRecordService.recordOperationLog("开票通知单管理", "批量提交审核",
                    "批量提交审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量撤回审核
     * 功能描述：批量撤回审核中的开票通知单，将状态改回待审核，同时将OA审核状态改为已撤回
     * 入参：{ noticeIds: number[] }
     * 返回参数：撤回结果
     * url地址：/api/finance/invoice-notices/batch/revoke
     * 请求方式：POST
     */
    @RequireActionPermission("业务管理:开票通知:批量撤回")
    @PostMapping("/batch/revoke")
    @ApiOperation(value = "批量撤回审核", notes = "批量撤回审核中的开票通知单，将通知单状态改为待审核，同时将OA审核状态改为已撤回、审核次数减1且最小为0")
    public Result<Void> batchRevokeInvoiceNotice(
            @RequestBody java.util.Map<String, java.util.List<Integer>> request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        java.util.List<Integer> noticeIds = request.get("noticeIds");
        try {
            if (noticeIds == null || noticeIds.isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "通知单ID列表不能为空");
            }
            invoiceNoticeService.batchRevokeInvoiceNotice(noticeIds);
            logRecordService.recordOperationLog("开票通知单管理", "批量撤回审核",
                    "批量撤回审核：通知单数量=" + noticeIds.size(), userId, ipAddress, true, null);
            return Result.success("批量撤回审核成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "批量撤回审核",
                    "批量撤回审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量撤回审核失败：noticeIds={}", noticeIds, e);
            logRecordService.recordOperationLog("开票通知单管理", "批量撤回审核",
                    "批量撤回审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量撤回审核失败：" + e.getMessage());
        }
    }

    /**
     * 撤销开票通知单
     * 功能描述：撤销开票通知单，将状态从待开票改为待审核
     * 入参：noticeId
     * 返回参数：撤销结果
     * url地址：/api/finance/invoice-notices/{noticeId}/cancel
     * 请求方式：POST
     */
    @RequireActionPermission("业务管理:开票通知:撤销开票")
    @PostMapping("/{noticeId}/cancel")
    @ApiOperation(value = "取消/关闭开票通知单", notes = "取消或关闭开票通知单（当前仅待开票允许）")
    public Result<Void> cancelInvoiceNotice(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            invoiceNoticeService.cancelInvoiceNotice(noticeId);
            logRecordService.recordOperationLog("开票通知单管理", "撤销开票",
                    "撤销开票通知单：通知单ID=" + noticeId, userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "INVOICE_NOTICE_UPDATE",
                        noticeId,
                        String.format("开票通知单已撤销：通知单ID=%d", noticeId),
                        "撤销",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送开票通知单撤销通知失败", msgEx);
            }

            return Result.success("撤销开票成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "撤销开票",
                    "撤销开票通知单失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("撤销开票通知单失败：noticeId={}", noticeId, e);
            logRecordService.recordOperationLog("开票通知单管理", "撤销开票",
                    "撤销开票通知单失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "取消开票通知单失败：" + e.getMessage());
        }
    }

    /**
     * 审批通过开票通知单
     * 功能描述：审批通过开票通知单，更新状态为待开票，并保存审批意见
     * 入参：{ noticeId: number, data: InvoiceNoticeApproveRequest }
     * 返回参数：审批结果
     * url地址：/api/finance/invoice-notices/{noticeId}/approve
     * 请求方式：POST
     */
    @RequireActionPermission("业务管理:开票通知:审批")
    @PostMapping("/{noticeId}/approve")
    @ApiOperation(value = "审批通过开票通知单", notes = "审批通过开票通知单，更新状态为待开票，并保存审批意见")
    public Result<Void> approveInvoiceNotice(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            @Valid @RequestBody InvoiceNoticeApproveRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            invoiceNoticeService.approveInvoiceNotice(noticeId, request);
            logRecordService.recordOperationLog("开票通知单管理", "审批通过",
                    "审批通过开票通知单：通知单ID=" + noticeId + "，审批意见=" + request.getOpinion(), userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendAuditResultNotification(
                        "INVOICE_NOTICE_AUDIT_RESULT",
                        noticeId,
                        String.format("开票通知单ID=%d", noticeId),
                        "审核通过",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送开票通知单审批通过通知失败", msgEx);
            }

            return Result.success("审批通过成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "审批通过",
                    "审批通过开票通知单失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审批通过开票通知单失败：noticeId={}", noticeId, e);
            logRecordService.recordOperationLog("开票通知单管理", "审批通过",
                    "审批通过开票通知单失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "审批通过失败：" + e.getMessage());
        }
    }

    /**
     * 拒绝开票通知单
     * 功能描述：驳回开票通知单，更新状态为已驳回，并保存驳回原因
     * 入参：{ noticeId: number, data: InvoiceNoticeRejectRequest }
     * 返回参数：驳回结果
     * url地址：/api/finance/invoice-notices/{noticeId}/reject
     * 请求方式：POST
     */
    @RequireActionPermission("业务管理:开票通知:审批")
    @PostMapping("/{noticeId}/reject")
    @ApiOperation(value = "驳回开票通知单", notes = "驳回开票通知单，更新状态为已驳回，并保存驳回原因")
    public Result<Void> rejectInvoiceNotice(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            @Valid @RequestBody InvoiceNoticeRejectRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            invoiceNoticeService.rejectInvoiceNotice(noticeId, request);
            logRecordService.recordOperationLog("开票通知单管理", "驳回",
                    "驳回开票通知单：通知单ID=" + noticeId + "，驳回原因=" + request.getReason(), userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendAuditResultNotification(
                        "INVOICE_NOTICE_AUDIT_RESULT",
                        noticeId,
                        String.format("开票通知单ID=%d", noticeId),
                        "审核驳回",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送开票通知单驳回通知失败", msgEx);
            }

            return Result.success("驳回成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "驳回",
                    "驳回开票通知单失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("驳回开票通知单失败：noticeId={}", noticeId, e);
            logRecordService.recordOperationLog("开票通知单管理", "驳回",
                    "驳回开票通知单失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "驳回失败：" + e.getMessage());
        }
    }

    /**
     * 获取发票关联列表（待开票和已开票状态的通知单）
     * 功能描述：查询待开票和已开票状态的开票通知单列表，用于发票关联操作
     * 入参：InvoiceNoticePageRequest
     * 返回参数：分页结果
     * url地址：/api/finance/invoice-notices/associate
     * 请求方式：GET
     */
    @RequireActionPermission("业务管理:开票通知:关联发票")
    @GetMapping("/associate")
    @ApiOperation(value = "获取发票关联列表", notes = "查询待开票和已开票状态的开票通知单列表，用于发票关联操作")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InvoiceNoticePageResponse>> getInvoiceAssociateList(
            @Valid InvoiceNoticePageRequest request) {
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<InvoiceNoticePageResponse> page = invoiceNoticeService.getInvoiceAssociateList(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询发票关联列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询发票关联列表失败：" + e.getMessage());
        }
    }

    /**
     * 关联发票
     * 功能描述：将已存在的发票关联到开票通知单，支持差分保存（新增该新增的，删除该删除的）
     * 入参：{ noticeId: number, data: InvoiceAssociateRequest { invoiceIds: number[] } }
     * 返回参数：关联结果
     * url地址：/api/finance/invoice-notices/{noticeId}/associate
     * 请求方式：POST
     * 注意：乐观锁版本号由MyBatis-Plus的@Version注解自动处理，不需要显式传入version字段
     */
    @RequireActionPermission("业务管理:开票通知:关联发票")
    @PostMapping("/{noticeId}/associate")
    @ApiOperation(value = "关联发票", notes = "将已存在的发票关联到开票通知单，支持差分保存")
    public Result<Void> associateInvoice(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            @Valid @RequestBody InvoiceAssociateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            invoiceNoticeService.associateInvoice(noticeId, request);
            logRecordService.recordOperationLog("开票通知单管理", "关联发票",
                    "关联发票：通知单ID=" + noticeId + "，发票数量=" + request.getInvoiceIds().size(), userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "INVOICE_NOTICE_UPDATE",
                        noticeId,
                        String.format("开票通知单已关联发票：通知单ID=%d，发票数量=%d", noticeId, request.getInvoiceIds().size()),
                        "关联发票",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送开票通知单关联发票通知失败", msgEx);
            }

            return Result.success("关联发票成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "关联发票",
                    "关联发票失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("关联发票失败：noticeId={}", noticeId, e);
            logRecordService.recordOperationLog("开票通知单管理", "关联发票",
                    "关联发票失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "关联发票失败：" + e.getMessage());
        }
    }

    /**
     * 验证发票能否关联（是否被其他通知单或结算单占用）
     * POST /api/finance/invoice-notices/{noticeId}/validate-invoices
     */
    @RequireActionPermission("业务管理:开票通知:关联发票")
    @PostMapping("/{noticeId}/validate-invoices")
    @ApiOperation(value = "验证发票能否关联", notes = "在前端选择发票前调用，返回冲突信息列表")
    public Result<java.util.List<String>> validateInvoiceAssociations(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            @RequestBody InvoiceAssociateRequest request) {
        try {
            java.util.List<String> conflicts = invoiceNoticeService.validateInvoiceAssociations(noticeId, request.getInvoiceIds());
            return Result.success("验证完成", conflicts);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("验证发票关联失败：noticeId={}", noticeId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "验证发票关联失败：" + e.getMessage());
        }
    }

    /**
     * 完成关联
     * 功能描述：完成发票关联，更新通知单状态为已开票，并回填已开票张数和金额汇总
     * 入参：{ noticeId: number, data: InvoiceAssociateCompleteRequest {} }
     * 返回参数：完成结果
     * url地址：/api/finance/invoice-notices/{noticeId}/complete
     * 请求方式：POST
     * 注意：乐观锁版本号由MyBatis-Plus的@Version注解自动处理，不需要显式传入version字段
     */
    @RequireActionPermission("业务管理:开票通知:关联发票")
    @PostMapping("/{noticeId}/complete")
    @ApiOperation(value = "完成关联", notes = "完成发票关联，更新通知单状态为已开票，并回填已开票张数和金额汇总")
    public Result<Void> completeInvoiceAssociate(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            @Valid @RequestBody InvoiceAssociateCompleteRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            invoiceNoticeService.completeInvoiceAssociate(noticeId, request);
            logRecordService.recordOperationLog("开票通知单管理", "完成关联",
                    "完成关联：通知单ID=" + noticeId, userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "INVOICE_NOTICE_UPDATE",
                        noticeId,
                        String.format("开票通知单已完成关联：通知单ID=%d", noticeId),
                        "完成关联",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送开票通知单完成关联通知失败", msgEx);
            }

            return Result.success("完成关联成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("开票通知单管理", "完成关联",
                    "完成关联失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("完成关联失败：noticeId={}", noticeId, e);
            logRecordService.recordOperationLog("开票通知单管理", "完成关联",
                    "完成关联失败：通知单ID=" + noticeId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "完成关联失败：" + e.getMessage());
        }
    }

    /**
     * 获取结算单发票汇总信息
     * 功能描述：根据通知单ID和结算单编号查询结算单的发票关联汇总信息，包括蓝字总额、红字总额、净额、可开蓝字金额等
     * 入参：noticeId（通知单ID）、settlementId（结算单编号）
     * 返回参数：SettlementInvoiceSummaryResponse
     * url地址：/api/finance/invoice-notices/{noticeId}/settlement/{settlementId}/summary
     * 请求方式：GET
     */
    @GetMapping("/{noticeId}/settlement/{settlementId}/summary")
    @ApiOperation(value = "获取结算单发票汇总信息", notes = "查询结算单的发票关联汇总信息，包括蓝字/红字金额计算")
    public Result<SettlementInvoiceSummaryResponse> getSettlementInvoiceSummary(
            @PathVariable @NotNull(message = "通知单ID不能为空") Integer noticeId,
            @PathVariable @NotNull(message = "结算单编号不能为空") Long settlementId) {
        try {
            // 验证通知单存在
            com.erp.controller.finance.dto.InvoiceNoticeDetailResponse notice = invoiceNoticeService.getInvoiceNoticeDetail(noticeId);
            if (notice == null) {
                return Result.error(ResultCodeEnum.NOT_FOUND.getCode(), "通知单不存在");
            }

            // 查询结算单发票汇总信息
            SettlementInvoiceSummaryResponse summary = settlementInvoiceRelMapper.selectSummaryBySettlementId(settlementId);
            if (summary == null) {
                return Result.error(ResultCodeEnum.NOT_FOUND.getCode(), "结算单不存在");
            }

            return Result.success("查询成功", summary);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取结算单发票汇总信息失败：noticeId={}, settlementId={}", noticeId, settlementId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取结算单发票汇总信息失败：" + e.getMessage());
        }
    }

}

