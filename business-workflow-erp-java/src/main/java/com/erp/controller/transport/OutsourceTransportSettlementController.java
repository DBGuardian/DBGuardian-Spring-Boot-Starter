package com.erp.controller.transport;

import com.erp.common.annotation.RequireActionPermission;
import com.erp.common.result.Result;
import com.erp.entity.system.Employee;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.controller.transport.dto.*;
import com.erp.service.transport.OutsourceTransportSettlementService;
import com.erp.common.util.SecurityUtil;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 委外运输结算 Controller
 */
@Slf4j
@Tag(name = "委外运输结算")
@RestController
@RequestMapping("/outsource-settlement")
@RequiredArgsConstructor
@Validated
public class OutsourceTransportSettlementController {

    private final OutsourceTransportSettlementService settlementService;
    private final ILogRecordService logRecordService;
    private final MessageNotificationService messageNotificationService;
    private final EmployeeMapper employeeMapper;

    @Operation(summary = "分页查询结算单列表")
    @GetMapping("/list")
    public Result<OutsourceSettlementPageResponse> getPage(OutsourceSettlementPageRequest request) {
        return Result.success(settlementService.getPage(request));
    }

    @Operation(summary = "查询结算单详情")
    @GetMapping("/{settlementId}")
    public Result<OutsourceSettlementResponse> getDetail(@PathVariable Integer settlementId) {
        OutsourceSettlementResponse response = settlementService.getDetail(settlementId);
        return Result.success(response);
    }

    /**
     * 新增委外运输结算单（支持收付款拆分）
     *
     * 功能描述：
     * - 保存主表+结算周期行+价外服务
     * - 若包含收付款混合明细会自动拆分，生成两个结算单
     * - 空数据允许，只验证数据库必填字段
     * - 价外服务归属收款单
     *
     * @param request 新增请求
     * @param httpRequest HTTP请求（用于获取IP）
     * @return 创建结果
     */
    @Operation(summary = "新增结算单（支持收付款拆分）")
    @PostMapping("/add")
    @RequireActionPermission("合同结算:运输费结算:编辑")
    public Result<OutsourceSettlementCreateResultDTO> addSettlement(@RequestBody OutsourceSettlementCreateRequest request,
                                                                   HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("新增委外运输结算单（收付款拆分），contractNo={}", request.getContractNo());
            OutsourceSettlementCreateResultDTO result = settlementService.createWithSplit(request, userId);

            logRecordService.recordOperationLog("运输费结算", "新增",
                    "新增运输结算单：结算单编号=" + result.getSettlementId(), userId, ipAddress, true, null);

            // 发送消息通知
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "TRANSPORT_SETTLEMENT_CREATE",
                        result.getSettlementId(),
                        String.format("运输结算单已添加：结算单编号=%s", result.getSettlementId()),
                        "新增",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送新增运输结算单通知失败", msgEx);
            }

            return Result.success("创建成功", result);
        } catch (Exception e) {
            logRecordService.recordOperationLog("运输费结算", "新增",
                    "新增运输结算单失败：contractNo=" + request.getContractNo(), userId, ipAddress, false, e.getMessage());
            return Result.error("新增运输结算单失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新结算单")
    @PutMapping("/{settlementId}")
    public Result<Void> update(@PathVariable Integer settlementId, @RequestBody OutsourceSettlementCreateRequest request) {
        settlementService.updateFromCreateRequest(settlementId, request);
        return Result.success();
    }

    @Operation(summary = "删除结算单")
    @DeleteMapping("/{settlementId}")
    public Result<Void> delete(@PathVariable Integer settlementId) {
        settlementService.delete(settlementId);
        return Result.success();
    }

    @Operation(summary = "查询可结算的总磅单列表")
    @GetMapping("/dispatch/available")
    public Result<SettlementSlipPageResponse> getAvailableSlipsForSettlement(SettlementDispatchOrderRequest request) {
        return Result.success(settlementService.getAvailableSlipsForSettlement(request));
    }

    @Operation(summary = "查询结算单关联的总磅单列表")
    @GetMapping("/{settlementId}/slips")
    public Result<List<OutsourceTransportSettlementSlipVO>> getSlipsBySettlementId(@PathVariable Integer settlementId) {
        return Result.success(settlementService.getSlipsBySettlementId(settlementId));
    }

    /**
     * 批量提交审核
     * 功能描述：批量提交多个委外运输结算单进行审核，只有待审核、已驳回状态的结算单才能提交审核
     * 提交后结算单状态改为"审核中"
     * 在OA审核记录表(OA_APPROVAL_RECORD)中有记录则审核次数+1，如没有则新增一条记录
     * 来源表中文名称为"委外运输结算"
     * 接口地址：POST /api/outsource-settlement/batch/submit-audit
     *
     * @param request 批量提交审核请求
     * @param httpRequest HTTP请求（用于获取IP）
     * @return 批量操作结果
     */
    @Operation(summary = "批量提交审核")
    @PostMapping("/batch/submit-audit")
    @RequireActionPermission("合同结算:运输费结算:编辑")
    public Result<OutsourceSettlementBatchOperationResult> batchSubmitAudit(
            @RequestBody OutsourceSettlementBatchRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量提交委外运输结算单审核请求：settlementIds={}", request.getSettlementIds());
            OutsourceSettlementBatchOperationResult result = settlementService.batchSubmitAudit(request.getSettlementIds(), userId);

            // 记录日志
            String logDetail = String.format("批量提交委外运输结算审核：成功=%d，失败=%d",
                    result.getSuccessCount(), result.getFailCount());
            logRecordService.recordOperationLog("运输费结算", "批量提交审核",
                    logDetail, userId, ipAddress, result.getSuccessCount() > 0, null);

            // 发送消息通知
            if (result.getSuccessCount() > 0) {
                try {
                    messageNotificationService.sendApprovalSubmitNotification(
                            "TRANSPORT_SETTLEMENT_SUBMIT",
                            null,
                            String.format("批量提交审核成功：成功=%d，失败=%d",
                                    result.getSuccessCount(), result.getFailCount()),
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送批量提交审核通知失败", msgEx);
                }
            }

            return Result.success("批量提交审核完成", result);
        } catch (Exception e) {
            log.error("批量提交委外运输结算审核系统异常：settlementIds={}", request.getSettlementIds(), e);
            logRecordService.recordOperationLog("运输费结算", "批量提交审核",
                    "批量提交委外运输结算审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error("批量提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量撤回审核
     * 功能描述：批量撤回多个审核中的委外运输结算单，只有审核中状态的结算单才能撤回
     * 撤回后结算单状态改为"待审核"
     * 在OA审核记录表(OA_APPROVAL_RECORD)中将审核状态改为"已撤回"，审核次数-1，最低为0
     * 接口地址：POST /api/outsource-settlement/batch/cancel-audit
     *
     * @param request 批量撤回审核请求
     * @param httpRequest HTTP请求（用于获取IP）
     * @return 批量操作结果
     */
    @Operation(summary = "批量撤回审核")
    @PostMapping("/batch/cancel-audit")
    @RequireActionPermission("合同结算:运输费结算:编辑")
    public Result<OutsourceSettlementBatchOperationResult> batchCancelAudit(
            @RequestBody OutsourceSettlementBatchRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量撤回委外运输结算单审核请求：settlementIds={}", request.getSettlementIds());
            OutsourceSettlementBatchOperationResult result = settlementService.batchCancelAudit(request.getSettlementIds(), userId);

            // 记录日志
            String logDetail = String.format("批量撤回委外运输结算审核：成功=%d，失败=%d",
                    result.getSuccessCount(), result.getFailCount());
            logRecordService.recordOperationLog("运输费结算", "批量撤回",
                    logDetail, userId, ipAddress, result.getSuccessCount() > 0, null);

            // 发送消息通知
            if (result.getSuccessCount() > 0) {
                try {
                    messageNotificationService.sendApprovalRevokeNotification(
                            "TRANSPORT_SETTLEMENT_REVOKE",
                            null,
                            String.format("批量撤回审核成功：成功=%d，失败=%d",
                                    result.getSuccessCount(), result.getFailCount()),
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送批量撤回审核通知失败", msgEx);
                }
            }

            return Result.success("批量撤回完成", result);
        } catch (Exception e) {
            log.error("批量撤回委外运输结算审核系统异常：settlementIds={}", request.getSettlementIds(), e);
            logRecordService.recordOperationLog("运输费结算", "批量撤回",
                    "批量撤回委外运输结算审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error("批量撤回审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除结算单
     * 功能描述：批量删除多个委外运输结算单，只有待审核、已驳回状态的结算单才能删除
     * 接口地址：POST /api/outsource-settlement/batch/delete
     *
     * @param request 批量删除请求
     * @param httpRequest HTTP请求（用于获取IP）
     * @return 批量操作结果
     */
    @Operation(summary = "批量删除")
    @PostMapping("/batch/delete")
    @RequireActionPermission("合同结算:运输费结算:编辑")
    public Result<OutsourceSettlementBatchOperationResult> batchDelete(
            @RequestBody OutsourceSettlementBatchRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量删除结算单请求：settlementIds={}", request.getSettlementIds());
            OutsourceSettlementBatchOperationResult result = settlementService.batchDelete(request.getSettlementIds(), userId);

            // 记录日志
            String logDetail = String.format("批量删除结算单：成功=%d，失败=%d",
                    result.getSuccessCount(), result.getFailCount());
            logRecordService.recordOperationLog("运输费结算", "批量删除",
                    logDetail, userId, ipAddress, result.getSuccessCount() > 0, null);

            return Result.success("批量删除完成", result);
        } catch (Exception e) {
            log.error("批量删除结算单系统异常：settlementIds={}", request.getSettlementIds(), e);
            logRecordService.recordOperationLog("运输费结算", "批量删除",
                    "批量删除结算单失败", userId, ipAddress, false, e.getMessage());
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 审核结算单
     * 功能描述：审核通过或驳回委外运输结算单，只有审核中状态可审核，审核意见必填
     *
     * 审核通过时：
     * - 结算单状态改为"已审核"
     * - 记录审核意见、审核人编码、审核人姓名、审核时间
     * - OA审核记录表审核状态改为"已通过"，记录审核人编码、审核人姓名、审核时间
     *
     * 审核驳回时：
     * - 结算单状态改为"已驳回"
     * - 记录审核意见、审核人编码、审核人姓名、审核时间
     * - OA审核记录表审核状态改为"已驳回"，记录审核人编码、审核人姓名、审核时间
     *
     * 接口地址：POST /api/outsource-settlement/audit/{settlementId}
     *
     * @param settlementId 结算单编号
     * @param request 审核请求（auditResult-审核结果，auditOpinion-审核意见必填）
     * @param httpRequest HTTP请求
     * @return 审核结果
     */
    @Operation(summary = "审核结算单")
    @PostMapping("/audit/{settlementId}")
    @RequireActionPermission("合同结算:运输费结算:审核")
    public Result<Void> auditSettlement(
            @PathVariable Integer settlementId,
            @Valid @RequestBody OutsourceSettlementAuditRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        // 从 Employee 表查询真实的员工姓名，而非登录账号
        String userName = getEmployeeNameFromEmployee(userId);
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("审核委外运输结算单请求：settlementId={}, auditResult={}", settlementId, request.getAuditResult());
            settlementService.auditSettlement(settlementId, request.getAuditResult(), request.getAuditOpinion(), userId, userName);

            String actionType = "approved".equals(request.getAuditResult()) ? "审核通过" : "驳回";
            logRecordService.recordOperationLog("运输费结算", actionType,
                    String.format("委外运输结算单审核：%s，审核意见：%s", actionType, request.getAuditOpinion()),
                    userId, ipAddress, true, null);

            return Result.success(actionType + "成功", null);
        } catch (Exception e) {
            log.error("审核结算单系统异常：settlementId={}", settlementId, e);
            logRecordService.recordOperationLog("运输费结算", "审核",
                    "审核结算单失败", userId, ipAddress, false, e.getMessage());
            return Result.error("审核失败：" + e.getMessage());
        }
    }

    /**
     * 取消审核（撤回）
     * 功能描述：撤回审核中的结算单
     * 接口地址：POST /api/outsource-settlement/cancel-audit/{settlementId}
     *
     * @param settlementId 结算单编号
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @Operation(summary = "取消审核")
    @PostMapping("/cancel-audit/{settlementId}")
    @RequireActionPermission("合同结算:运输费结算:编辑")
    public Result<Void> cancelAudit(
            @PathVariable Integer settlementId,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("取消审核请求：settlementId={}", settlementId);
            settlementService.cancelAudit(settlementId, userId);

            logRecordService.recordOperationLog("运输费结算", "取消审核",
                    "取消审核", userId, ipAddress, true, null);

            return Result.success("取消审核成功", null);
        } catch (Exception e) {
            log.error("取消审核系统异常：settlementId={}", settlementId, e);
            logRecordService.recordOperationLog("运输费结算", "取消审核",
                    "取消审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error("取消审核失败：" + e.getMessage());
        }
    }

    /**
     * 从 Employee 表查询员工姓名
     * @param employeeId 员工编码
     * @return 员工姓名，如果未找到则返回 null
     */
    private String getEmployeeNameFromEmployee(Integer employeeId) {
        if (employeeId == null) {
            return null;
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee != null && employee.getEmployeeName() != null) {
            return employee.getEmployeeName();
        }
        return null;
    }
}
