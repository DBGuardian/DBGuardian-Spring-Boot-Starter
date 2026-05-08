package com.erp.controller.oa;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.contract.dto.QuotationAuditRequest;
import com.erp.controller.oa.dto.*;
import com.erp.controller.transport.dto.TransportContractStatusRequest;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.entity.system.Employee;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.service.contract.QuotationService;
import com.erp.service.oa.OaApprovalRecordService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.transport.TransportContractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * OA审核记录控制器
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/oa/approval")
@Api(tags = "OA审批管理")
public class OaApprovalController {

    @Autowired
    private OaApprovalRecordService oaApprovalRecordService;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired(required = false)
    private QuotationService quotationService;

    @Autowired(required = false)
    private TransportContractService transportContractService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 获取OA审核统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取OA审核统计", notes = "获取OA审批中心顶部统计数据")
    public Result<OaApprovalRecordService.OaApprovalStatistics> getStatistics(
            @RequestParam(required = false) String viewScope,
            @RequestParam(required = false) Integer submitterId,
            @RequestParam(required = false) Integer approverId
    ) {
        try {
            OaApprovalRecordService.OaApprovalStatistics statistics = oaApprovalRecordService.getStatistics(
                    viewScope, submitterId, approverId);
            return Result.success("获取统计数据成功", statistics);
        } catch (Exception e) {
            log.error("获取OA审核统计失败", e);
            return Result.error("获取统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询OA审核记录列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取OA审核列表", notes = "分页查询OA审批台账")
    public Result<Map<String, Object>> getApprovalList(OaApprovalListRequest request) {
        try {
            IPage<OaApprovalRecord> page = oaApprovalRecordService.getApprovalPage(
                    request.getPage(),
                    request.getPageSize(),
                    request.getKeyword(),
                    request.getBusinessType(),
                    request.getApprovalStatus(),
                    request.getViewScope(),
                    request.getSubmitterId(),
                    request.getApproverId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getUnapprovedDays()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("list", page.getRecords());
            result.put("total", page.getTotal());
            result.put("page", page.getCurrent());
            result.put("pageSize", page.getSize());

            return Result.success("获取审核列表成功", result);
        } catch (Exception e) {
            log.error("获取OA审核列表失败", e);
            return Result.error("获取审核列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取OA审核详情
     */
    @GetMapping("/detail/{approvalId}")
    @ApiOperation(value = "获取OA审核详情", notes = "根据审批记录ID查询详情")
    public Result<OaApprovalDetailResponse> getApprovalDetail(@PathVariable("approvalId") Integer approvalId) {
        try {
            OaApprovalRecord record = oaApprovalRecordService.getApprovalDetail(approvalId);

            OaApprovalDetailResponse response = new OaApprovalDetailResponse();
            response.setRecord(record);
            response.setDetails(null); // 审批历史暂不实现

            return Result.success("获取审核详情成功", response);
        } catch (Exception e) {
            log.error("获取OA审核详情失败", e);
            return Result.error("获取审核详情失败：" + e.getMessage());
        }
    }

    /**
     * 执行OA审核操作（通过/驳回）
     */
    @PostMapping("/approve")
    @ApiOperation(value = "执行OA审核", notes = "对待审核单据执行通过或驳回操作")
    public Result<ApproveOaApprovalResponse> approve(@Validated @RequestBody ApproveOaApprovalRequest request,
                                                       HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        // 从 Employee 表查询真实的员工姓名，而非登录账号
        String userName = getEmployeeNameFromEmployee(userId);
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean approveSuccess = false;
        String errorMessage = null;

        try {
            OaApprovalRecord updatedRecord = oaApprovalRecordService.approve(
                    request.getApprovalRecordId(),
                    request.getSourceTable(),
                    request.getSourceId(),
                    request.getResult(),
                    request.getOpinion(),
                    userId,
                    userName
            );
            approveSuccess = true;

            // 审核成功后，回调业务表更新状态
            afterApproveCallback(request.getSourceTable(), request.getSourceId(), request.getResult(), request.getOpinion(), userId, userName);

            ApproveOaApprovalResponse response = new ApproveOaApprovalResponse();
            response.setSuccess(true);
            response.setUpdatedRecord(updatedRecord);

            return Result.success("审核操作成功", response);
        } catch (Exception e) {
            log.error("执行OA审核失败", e);
            errorMessage = e.getMessage();
            return Result.error("审核操作失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("执行OA审核：审批记录ID=%s，业务类型=%s，结果=%s，审批意见=%s",
                        request.getApprovalRecordId(), request.getSourceTable(),
                        request.getResult(), request.getOpinion());
                logRecordService.recordOperationLog("OA审批管理", "审核",
                        logContent, userId, ipAddress, approveSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录OA审核操作日志失败", logEx);
            }
        }
    }

    /**
     * 审核完成后的业务回调
     * 根据业务类型，调用对应的Service更新业务表状态
     */
    private void afterApproveCallback(String sourceTable, Integer sourceId, String result, String opinion, Integer approverId, String approverName) {
        if (sourceTable == null || sourceId == null) {
            return;
        }

        try {
            switch (sourceTable.toUpperCase()) {
                case "QUOTATION":
                    // 报价单审核回调
                    if (quotationService != null) {
                        QuotationAuditRequest auditRequest = new QuotationAuditRequest();
                        auditRequest.setQuotationId(sourceId);
                        // OA审核结果：pass -> 已通过，reject -> 已拒绝
                        auditRequest.setAuditResult("通过".equals(result) ? "已通过" : "已拒绝");
                        auditRequest.setAuditOpinion(opinion);
                        // OA回调时跳过权限检查
                        auditRequest.setSkipPermissionCheck(true);
                        quotationService.auditQuotation(auditRequest);
                        log.info("OA审核回调报价单成功：quotationId={}, result={}", sourceId, result);
                    }
                    break;
                case "CONTRACT":
                    // 合同审核回调（后续可扩展）
                    log.info("OA审核合同回调待实现：contractId={}", sourceId);
                    break;
                case "TRANSPORT_CONTRACT":
                    // 运输合同审核回调
                    if (transportContractService != null) {
                        TransportContractStatusRequest statusRequest = new TransportContractStatusRequest();
                        // OA审核结果：通过 -> 执行中，驳回 -> 已驳回
                        statusRequest.setStatus("通过".equals(result) ? "执行中" : "已驳回");
                        statusRequest.setAuditOpinion(opinion);
                        transportContractService.updateStatus(sourceId, statusRequest);
                        log.info("OA审核回调运输合同成功：contractId={}, result={}", sourceId, result);
                    }
                    break;
                default:
                    log.info("OA审核回调未知业务类型：sourceTable={}, sourceId={}", sourceTable, sourceId);
                    break;
            }
        } catch (Exception e) {
            log.error("OA审核回调业务表失败：sourceTable={}, sourceId={}", sourceTable, sourceId, e);
            // 回调失败不影响审核主流程，只记录日志
        }
    }

    /**
     * 提交审核
     */
    @PostMapping("/submit")
    @ApiOperation(value = "提交审核", notes = "业务单据提交审核，在OA审核表创建记录")
    public Result<SubmitApprovalResponse> submit(@Validated @RequestBody SubmitApprovalRequest request,
                                                   HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean submitSuccess = false;
        String errorMessage = null;
        OaApprovalRecord record = null;

        try {
            record = oaApprovalRecordService.submit(
                    request.getSourceTable(),
                    request.getSourceId(),
                    request.getSourceTableName(),
                    request.getSourceNo(),
                    request.getTitle(),
                    request.getSubmitterId(),
                    request.getSubmitterName()
            );
            submitSuccess = true;

            SubmitApprovalResponse response = new SubmitApprovalResponse();
            response.setApprovalRecordId(record.getApprovalRecordId());
            response.setApprovalNo(record.getApprovalNo());

            return Result.success("提交审核成功", response);
        } catch (Exception e) {
            log.error("提交审核失败", e);
            errorMessage = e.getMessage();
            return Result.error("提交审核失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("提交OA审核：业务类型=%s，业务ID=%s，单据号=%s",
                        request.getSourceTable(), request.getSourceId(), request.getSourceNo());
                logRecordService.recordOperationLog("OA审批管理", "提交审核",
                        logContent, userId, ipAddress, submitSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录提交审核操作日志失败", logEx);
            }
        }
    }

    /**
     * 重新提交审核
     */
    @PostMapping("/resubmit")
    @ApiOperation(value = "重新提交审核", notes = "驳回后重新提交审核")
    public Result<ResubmitApprovalResponse> resubmit(@Validated @RequestBody ResubmitApprovalRequest request,
                                                     HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean resubmitSuccess = false;
        String errorMessage = null;
        OaApprovalRecord record = null;

        try {
            record = oaApprovalRecordService.resubmit(
                    request.getOriginalRecordId(),
                    request.getSourceTable(),
                    request.getSourceId(),
                    request.getSubmitterId(),
                    request.getSubmitterName()
            );
            resubmitSuccess = true;

            ResubmitApprovalResponse response = new ResubmitApprovalResponse();
            response.setNewApprovalRecordId(record.getApprovalRecordId());
            response.setApprovalNo(record.getApprovalNo());
            response.setApprovalCount(record.getApprovalCount());

            return Result.success("重新提交审核成功", response);
        } catch (Exception e) {
            log.error("重新提交审核失败", e);
            errorMessage = e.getMessage();
            return Result.error("重新提交审核失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("重新提交OA审核：原审批记录ID=%s，业务类型=%s，业务ID=%s",
                        request.getOriginalRecordId(), request.getSourceTable(), request.getSourceId());
                logRecordService.recordOperationLog("OA审批管理", "重新提交",
                        logContent, userId, ipAddress, resubmitSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录重新提交审核操作日志失败", logEx);
            }
        }
    }

    /**
     * 获取业务审核跳转信息
     */
    @GetMapping("/business-detail")
    @ApiOperation(value = "获取业务审核跳转信息", notes = "根据业务类型与业务主键返回对应审核页面路由")
    public Result<BusinessDetailResponse> getBusinessDetail(
            @RequestParam String sourceTable,
            @RequestParam Integer sourceId
    ) {
        try {
            // 根据业务类型映射路由
            String routePath = getRoutePathByBusinessType(sourceTable);

            BusinessDetailResponse response = new BusinessDetailResponse();
            response.setRoutePath(routePath);
            response.setBusinessData(new BusinessDetailResponse.BusinessData());
            response.getBusinessData().setId(sourceId);

            return Result.success("获取业务详情跳转信息成功", response);
        } catch (Exception e) {
            log.error("获取业务审核跳转信息失败", e);
            return Result.error("获取业务详情跳转信息失败：" + e.getMessage());
        }
    }

    /**
     * 根据业务类型获取路由路径
     */
    private String getRoutePathByBusinessType(String sourceTable) {
        if (sourceTable == null) {
            return "/";
        }

        switch (sourceTable.toUpperCase()) {
            case "CONTRACT":
                return "/app/contracts/audit";
            case "QUOTATION":
                return "/app/contracts/quotations/detail";
            case "TRANSPORT":
                return "/app/transport/apply/audit";
            case "OUTBOUND":
                return "/app/warehouse/outbound/audit";
            case "WAREHOUSING":
                return "/app/warehouse/warehousing/detail";
            case "SETTLEMENT":
                return "/app/finance/settlement/audit";
            case "INVOICE":
                return "/app/finance/invoice-notices/approve";
            case "BUSINESS_FEE":
                return "/app/finance/business-fee/detail";
            case "EMPLOYEE_REGISTRATION":
                return "/app/system/employee-registration/audit";
            case "TRANSPORT_CONTRACT":
                return "/app/contracts/transport-contract/detail";
            default:
                return "/";
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
