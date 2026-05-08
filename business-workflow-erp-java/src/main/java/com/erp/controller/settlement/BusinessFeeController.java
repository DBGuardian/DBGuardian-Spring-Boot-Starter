package com.erp.controller.settlement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.controller.settlement.dto.*;
import com.erp.service.settlement.BusinessFeeService;
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
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * 业务费管理控制器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/settlement/business-fee")
@Api(tags = "业务费管理")
@Validated
public class BusinessFeeController {

    @Autowired
    private BusinessFeeService businessFeeService;

    @Autowired
    private com.erp.service.settlement.SettlementService settlementService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 分页查询业务费列表
     */
    @RequirePagePermission({
            "合同结算:业务费结算:页面",
            "合同结算:运输费结算:页面",
            "合同结算:处置费结算:页面"
    })
    @GetMapping("/page")
    @ApiOperation("分页查询业务费列表")
    public Result<IPage<BusinessFeeListItemDTO>> getBusinessFeePage(
            @RequestParam(defaultValue = "1") @Min(1) Integer current,
            @RequestParam(defaultValue = "10") @Min(1) Integer size,
            @RequestParam(required = false) String settlementType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessCode,
            @RequestParam(required = false) Integer businessContractId,
            @RequestParam(required = false) Integer dangerousSettlementId,
            @RequestParam(required = false) Boolean independentOnly,
            @RequestParam(required = false) String viewScope) {

        log.info("分页查询业务费列表，current={}, size={}, settlementType={}, status={}",
                current, size, settlementType, status);

        BusinessFeeQueryDTO queryDTO = new BusinessFeeQueryDTO();
        queryDTO.setCurrent(current);
        queryDTO.setSize(size);
        queryDTO.setSettlementType(settlementType);
        queryDTO.setStatus(status);
        queryDTO.setBusinessCode(businessCode);
        queryDTO.setBusinessContractId(businessContractId);
        queryDTO.setDangerousSettlementId(dangerousSettlementId);
        queryDTO.setIndependentOnly(independentOnly);
        queryDTO.setViewScope(viewScope);

        IPage<BusinessFeeListItemDTO> pageResult = businessFeeService.getBusinessFeePage(queryDTO);

        return Result.success("查询成功", pageResult);
    }

    /**
     * 获取业务费统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation("获取业务费统计信息")
    public Result<BusinessFeeStatisticsDTO> getStatistics() {
        log.info("获取业务费统计信息");

        BusinessFeeStatisticsDTO statistics = businessFeeService.getBusinessFeeStatistics();

        return Result.success("获取统计信息成功", statistics);
    }

    /**
     * 获取业务费详情
     */
    @GetMapping("/{id}")
    @ApiOperation("获取业务费详情")
    @RequireActionPermission("合同结算:业务费结算:查看")
    public Result<BusinessFeeDetailDTO> getDetail(@PathVariable @NotNull @Min(1) Integer id) {
        log.info("获取业务费详情，id={}", id);

        BusinessFeeDetailDTO detail = businessFeeService.getBusinessFeeDetail(id);

        return Result.success("获取详情成功", detail);
    }

    /**
     * 删除业务费
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除业务费")
    public Result<Void> deleteBusinessFee(@PathVariable @NotNull @Min(1) Integer id,
                                         HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("删除业务费，id={}", id);
            businessFeeService.deleteBusinessFee(id, userId);
            logRecordService.recordOperationLog("业务费结算", "删除",
                    "删除业务费：id=" + id, userId, ipAddress, true, null);
            return Result.success();
        } catch (Exception e) {
            logRecordService.recordOperationLog("业务费结算", "删除",
                    "删除业务费失败：id=" + id, userId, ipAddress, false, e.getMessage());
            return Result.error("删除业务费失败：" + e.getMessage());
        }
    }

    /**
     * 提交审核
     */
    @PostMapping("/{id}/submit-audit")
    @ApiOperation("提交审核")
    @RequireActionPermission("合同结算:业务费结算:编辑")
    public Result<Void> submitAudit(@PathVariable @NotNull @Min(1) Integer id,
                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("提交审核，id={}", id);
            businessFeeService.submitAudit(id, userId);
            logRecordService.recordOperationLog("业务费结算", "提交审核",
                    "提交业务费审核：id=" + id, userId, ipAddress, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendApprovalSubmitNotification(
                        "BUSINESS_FEE_SUBMIT",
                        id,
                        String.format("业务费待审核：id=%d", id),
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送提交审核通知失败", msgEx);
            }
            return Result.success();
        } catch (Exception e) {
            logRecordService.recordOperationLog("业务费结算", "提交审核",
                    "提交业务费审核失败：id=" + id, userId, ipAddress, false, e.getMessage());
            return Result.error("提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 取消审核
     */
    @PostMapping("/{id}/cancel-audit")
    @ApiOperation("取消审核")
    @RequireActionPermission("合同结算:业务费结算:编辑")
    public Result<Void> cancelAudit(@PathVariable @NotNull @Min(1) Integer id,
                                   HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("取消审核，id={}", id);
            businessFeeService.cancelAudit(id, userId);
            logRecordService.recordOperationLog("业务费结算", "取消审核",
                    "取消业务费审核：id=" + id, userId, ipAddress, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendApprovalRevokeNotification(
                        "BUSINESS_FEE_REVOKE",
                        id,
                        String.format("业务费已取消审核：id=%d", id),
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送取消审核通知失败", msgEx);
            }
            return Result.success();
        } catch (Exception e) {
            logRecordService.recordOperationLog("业务费结算", "取消审核",
                    "取消业务费审核失败：id=" + id, userId, ipAddress, false, e.getMessage());
            return Result.error("取消审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量提交审核
     * 功能描述：批量提交多个业务费结算单进行审核，只有待审核、已驳回状态的结算单才能提交审核
     * 提交后结算单状态改为"审核中"
     * 接口地址：POST /api/settlement/business-fee/batch-submit-audit
     * 请求方式：POST
     * 请求参数：{ businessSeqs: number[] }
     * 返回参数：{ successCount: number, failCount: number, failures: [{ businessSeq, businessCode, reason }] }
     */
    @PostMapping("/batch-submit-audit")
    @ApiOperation("批量提交审核")
    @RequireActionPermission("合同结算:业务费结算:编辑")
    public Result<BusinessFeeBatchOperationResult> batchSubmitAudit(
            @RequestBody @Validated BusinessFeeBatchRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量提交业务费审核请求：businessSeqs={}", request.getBusinessSeqs());
            BusinessFeeBatchOperationResult result = businessFeeService.batchSubmitAudit(request.getBusinessSeqs(), userId);

            // 记录日志
            String logDetail = String.format("批量提交业务费审核：成功=%d，失败=%d",
                    result.getSuccessCount(), result.getFailCount());
            logRecordService.recordOperationLog("业务费结算", "批量提交审核",
                    logDetail, userId, ipAddress, result.getSuccessCount() > 0, null);

            // 发送消息通知（使用基于权限的通知方法）
            if (result.getSuccessCount() > 0) {
                try {
                    messageNotificationService.sendApprovalSubmitNotification(
                            "BUSINESS_FEE_SUBMIT",
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
            log.error("批量提交业务费审核系统异常：businessSeqs={}", request.getBusinessSeqs(), e);
            logRecordService.recordOperationLog("业务费结算", "批量提交审核",
                    "批量提交业务费审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error("批量提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量撤回审核
     * 功能描述：批量撤回多个审核中的业务费结算单，只有审核中状态的结算单才能撤回
     * 撤回后结算单状态改为"待审核"
     * 接口地址：POST /api/settlement/business-fee/batch-cancel-audit
     * 请求方式：POST
     * 请求参数：{ businessSeqs: number[] }
     * 返回参数：{ successCount: number, failCount: number, failures: [{ businessSeq, businessCode, reason }] }
     */
    @PostMapping("/batch-cancel-audit")
    @ApiOperation("批量撤回审核")
    @RequireActionPermission("合同结算:业务费结算:编辑")
    public Result<BusinessFeeBatchOperationResult> batchCancelAudit(
            @RequestBody @Validated BusinessFeeBatchRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量撤回业务费审核请求：businessSeqs={}", request.getBusinessSeqs());
            BusinessFeeBatchOperationResult result = businessFeeService.batchCancelAudit(request.getBusinessSeqs(), userId);

            // 记录日志
            String logDetail = String.format("批量撤回业务费审核：成功=%d，失败=%d",
                    result.getSuccessCount(), result.getFailCount());
            logRecordService.recordOperationLog("业务费结算", "批量撤回",
                    logDetail, userId, ipAddress, result.getSuccessCount() > 0, null);

            // 发送消息通知（使用基于权限的通知方法）
            if (result.getSuccessCount() > 0) {
                try {
                    messageNotificationService.sendApprovalRevokeNotification(
                            "BUSINESS_FEE_REVOKE",
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
            log.error("批量撤回业务费审核系统异常：businessSeqs={}", request.getBusinessSeqs(), e);
            logRecordService.recordOperationLog("业务费结算", "批量撤回",
                    "批量撤回业务费审核失败", userId, ipAddress, false, e.getMessage());
            return Result.error("批量撤回审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除业务费
     * 功能描述：批量删除多个业务费结算单，只有草稿、待审核、已驳回状态的结算单才能删除
     * 接口地址：POST /api/settlement/business-fee/batch-delete
     * 请求方式：POST
     * 请求参数：{ businessSeqs: number[] }
     * 返回参数：{ successCount: number, failCount: number, failures: [{ businessSeq, businessCode, reason }] }
     */
    @PostMapping("/batch-delete")
    @ApiOperation("批量删除业务费")
    @RequireActionPermission("合同结算:业务费结算:编辑")
    public Result<BusinessFeeBatchOperationResult> batchDelete(
            @RequestBody @Validated BusinessFeeBatchRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("批量删除业务费请求：businessSeqs={}", request.getBusinessSeqs());
            BusinessFeeBatchOperationResult result = businessFeeService.batchDelete(request.getBusinessSeqs(), userId);

            // 记录日志
            String logDetail = String.format("批量删除业务费：成功=%d，失败=%d",
                    result.getSuccessCount(), result.getFailCount());
            logRecordService.recordOperationLog("业务费结算", "批量删除",
                    logDetail, userId, ipAddress, result.getSuccessCount() > 0, null);

            return Result.success("批量删除完成", result);
        } catch (Exception e) {
            log.error("批量删除业务费系统异常：businessSeqs={}", request.getBusinessSeqs(), e);
            logRecordService.recordOperationLog("业务费结算", "批量删除",
                    "批量删除业务费失败", userId, ipAddress, false, e.getMessage());
            return Result.error("批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 新增业务费详情数据（主表+明细表）
     */
    @PostMapping("/add")
    @ApiOperation("新增业务费详情数据")
    @RequireActionPermission("合同结算:业务费结算:编辑")
    public Result<Map<String, Object>> addBusinessFeeDetail(@RequestBody @Valid BusinessFeeCreateDTO createDTO,
                                                            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("新增业务费详情数据，businessSeq={}", createDTO.getBusinessSeq());
            BusinessFeeCreateResultDTO createResult = businessFeeService.createBusinessFeeDetail(createDTO, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("businessSeq", createResult.getBusinessSeq());
            result.put("split", createResult.getSplit());
            result.put("receivableBusinessSeq", createResult.getReceivableBusinessSeq());
            result.put("payableBusinessSeq", createResult.getPayableBusinessSeq());

            logRecordService.recordOperationLog("业务费结算", "新增",
                    "新增业务费：业务编号=" + createResult.getBusinessSeq(), userId, ipAddress, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "BUSINESS_FEE_CREATE",
                        null,
                        String.format("业务费已添加：业务编号=%s", createResult.getBusinessSeq()),
                        "新增",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送新增业务费通知失败", msgEx);
            }
            return Result.success("保存成功", result);
        } catch (Exception e) {
            logRecordService.recordOperationLog("业务费结算", "新增",
                    "新增业务费失败：businessSeq=" + createDTO.getBusinessSeq(), userId, ipAddress, false, e.getMessage());
            return Result.error("新增业务费失败：" + e.getMessage());
        }
    }

    /**
     * 修改业务费详情数据（单方向更新）
     */
    @PutMapping("/save/{id}")
    @ApiOperation("修改业务费详情数据")
    @RequireActionPermission("合同结算:业务费结算:编辑")
    public Result<Void> updateBusinessFeeDetail(
            @PathVariable @NotNull @Min(1) Integer id,
            @RequestBody @Valid BusinessFeeDetailUpdateDTO updateDTO,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("修改业务费详情数据，id={}", id);
            businessFeeService.updateBusinessFeeDetail(id, updateDTO, userId);
            logRecordService.recordOperationLog("业务费结算", "编辑",
                    "编辑业务费：id=" + id, userId, ipAddress, true, null);
            // 发送消息通知（使用基于权限的通知方法）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "BUSINESS_FEE_UPDATE",
                        id,
                        String.format("业务费已更新：id=%d", id),
                        "更新",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送更新业务费通知失败", msgEx);
            }
            return Result.success();
        } catch (Exception e) {
            logRecordService.recordOperationLog("业务费结算", "编辑",
                    "编辑业务费失败：id=" + id, userId, ipAddress, false, e.getMessage());
            return Result.error("编辑业务费失败：" + e.getMessage());
        }
    }

    /**
     * 业务费创建专用 - 危废结算单分页查询
     * 支持按结算单编号、结算单单号、结算类型、状态、制单人名称、合同编号过滤
     * 结果包含是否已生成业务结算单（hasBusinessFee）和业务结算单数量（businessFeeCount）
     */
    @GetMapping("/settlement-list")
    @ApiOperation("业务费创建专用-危废结算单分页查询")
    @RequirePagePermission({
            "合同结算:业务费结算:页面",
            "合同结算:运输费结算:页面",
            "合同结算:处置费结算:页面"
    })
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<com.erp.controller.settlement.dto.SettlementForBusinessFeePageResponse>> getSettlementForBusinessFeePage(
            @RequestParam(defaultValue = "1") @Min(1) Integer current,
            @RequestParam(defaultValue = "10") @Min(1) Integer size,
            @RequestParam(required = false) Long settlementId,
            @RequestParam(required = false) String settlementCode,
            @RequestParam(required = false) String settlementType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) Integer contractId,
            @RequestParam(required = false) Integer businessContractId,
            @RequestParam(required = false) Boolean unlinkedHazardousContractOnly) {


        SettlementForBusinessFeePageRequest request =
                new SettlementForBusinessFeePageRequest();
        request.setCurrent(current);
        request.setSize(size);
        request.setSettlementId(settlementId);
        request.setSettlementCode(settlementCode);
        request.setSettlementType(settlementType);
        request.setStatus(status);
        request.setCreatorName(creatorName);
        request.setContractId(contractId);
        request.setBusinessContractId(businessContractId);
        request.setUnlinkedHazardousContractOnly(unlinkedHazardousContractOnly);

        long startTime = System.currentTimeMillis();
        IPage<SettlementForBusinessFeePageResponse> result =
                settlementService.getSettlementForBusinessFeePage(request);
        log.info("业务费创建专用结算单查询完成，耗时 {} ms，contractId={}, businessContractId={}, 返回记录数={}",
                System.currentTimeMillis() - startTime, contractId, businessContractId, result.getTotal());

        return Result.success("查询成功", result);
    }

    /**
     * 批量查询危废结算单关联入库单聚合数据
     * 按废物类别+废物代码+废物名称合并有价类/无价类重量，用于业务费「从关联结算单导入」时自动填充废物信息
     */
    @PostMapping("/warehousing-aggregate")
    @ApiOperation("批量查询危废结算单关联入库单聚合数据")
    public Result<SettlementWasteAggregateResponse> getSettlementWarehousingAggregate(
            @RequestBody @Valid SettlementWasteAggregateRequest request) {

        log.info("批量查询危废结算单关联入库单聚合数据，settlementIds={}", request.getSettlementIds());

        SettlementWasteAggregateResponse result =
                businessFeeService.getSettlementWarehousingAggregate(request);

        return Result.success("查询成功", result);
    }

    /**
     * 审核业务费结算单（通过/驳回）
     * <p>
     * 通过时：
     * - BUSINESS_FEE_HEADER: 状态改为"已审核"，记录审核人编码、审核时间、审核意见
     * - OA_APPROVAL_RECORD: 状态改为"已通过"，记录审核人编码、审核人姓名、审核时间
     * <p>
     * 驳回时：
     * - BUSINESS_FEE_HEADER: 状态改为"已驳回"，记录审核人编码、审核时间、审核意见
     * - OA_APPROVAL_RECORD: 状态改为"已驳回"，记录审核人编码、审核人姓名、审核时间
     * <p>
     * 接口地址：POST /api/settlement/business-fee/{id}/audit
     * 请求方式：POST
     * 请求参数：路径参数 id，body { auditResult: 'approved' | 'rejected', auditOpinion: string }
     * 返回参数：{ success: boolean, message: string }
     */
    @PostMapping("/{id}/audit")
    @ApiOperation("审核业务费结算单")
    @RequireActionPermission("合同结算:业务费结算:审核")
    public Result<String> auditBusinessFee(
            @PathVariable("id") @NotNull Integer id,
            @RequestBody @Valid BusinessFeeAuditRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String userName = SecurityUtil.getEmployeeName();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        try {
            log.info("审核业务费结算单请求，businessSeq={}, auditResult={}, auditOpinion={}",
                    id, request.getAuditResult(), request.getAuditOpinion());

            // 转换审核结果：前端 approved -> 通过，rejected -> 驳回
            String auditResult = "approved".equals(request.getAuditResult()) ? "通过" : "驳回";

            businessFeeService.auditBusinessFee(
                    id,
                    auditResult,
                    request.getAuditOpinion(),
                    userId,
                    userName,
                    Boolean.TRUE.equals(request.getSkipPermissionCheck())
            );

            String logAction = "approved".equals(request.getAuditResult()) ? "审核通过" : "审核驳回";
            String logDetail = String.format("审核业务费结算单：businessSeq=%d，审核结果=%s，审核意见=%s",
                    id, auditResult,
                    request.getAuditOpinion() != null ? request.getAuditOpinion() : "");

            logRecordService.recordOperationLog("业务费结算", logAction,
                    logDetail, userId, ipAddress, true, null);

            // 发送消息通知（使用基于权限的通知方法）
            try {
                String auditAction = "approved".equals(request.getAuditResult()) ? "审核通过" : "审核驳回";
                messageNotificationService.sendAuditResultNotification(
                        "BUSINESS_FEE_AUDIT_RESULT",
                        id,
                        String.format("业务费【%s】", id),
                        auditAction,
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送业务费审核通知失败", msgEx);
            }

            return Result.success("审核操作完成", "success");
        } catch (Exception e) {
            log.error("审核业务费结算单失败，businessSeq={}", id, e);
            logRecordService.recordOperationLog("业务费结算", "审核",
                    "审核业务费结算单失败：businessSeq=" + id,
                    userId, ipAddress, false, e.getMessage());
            return Result.error("审核失败：" + e.getMessage());
        }
    }
}
