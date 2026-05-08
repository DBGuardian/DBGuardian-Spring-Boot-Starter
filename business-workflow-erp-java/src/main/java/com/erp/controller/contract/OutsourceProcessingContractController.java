package com.erp.controller.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.contract.dto.*;
import com.erp.service.contract.OutsourceProcessingContractBatchAuditResponse;
import com.erp.service.contract.OutsourceProcessingContractService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 委外处理合同控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/outsource-processing-contract")
@Api(tags = "委外处理合同管理")
@RequiredArgsConstructor
public class OutsourceProcessingContractController {

    private final OutsourceProcessingContractService outsourceProcessingContractService;
    private final ILogRecordService logRecordService;
    private final MessageNotificationService messageNotificationService;

    /**
     * 分页查询
     */
    @RequirePagePermission("合同管理:委托处理合同:页面")
    @GetMapping("/list")
    @ApiOperation(value = "分页查询委外处理合同")
    public Result<IPage<OutsourceProcessingContractPageResponse>> getContractPage(
            @ModelAttribute OutsourceProcessingContractPageRequest request) {
        try {
            return Result.success("查询成功", outsourceProcessingContractService.getContractPage(request));
        } catch (Exception e) {
            log.error("分页查询委外处理合同失败: {}", e.getMessage(), e);
            return Result.error(ResultCodeEnum.ERROR.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 详情
     */
    @RequirePagePermission("合同管理:委托处理合同:页面")
    @GetMapping("/{contractId}")
    @ApiOperation(value = "获取委外处理合同详情")
    public Result<OutsourceProcessingContractDetailResponse> getContractDetail(
            @PathVariable Integer contractId) {
        try {
            return Result.success("查询成功", outsourceProcessingContractService.getContractDetail(contractId));
        } catch (BusinessException e) {
            log.error("获取合同详情失败: contractId={}, error={}", contractId, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取合同详情失败: contractId={}, error={}", contractId, e.getMessage(), e);
            return Result.error(ResultCodeEnum.ERROR.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 新增
     */
    @RequirePagePermission("合同管理:委托处理合同:页面")
    @PostMapping(consumes = {"multipart/form-data"})
    @ApiOperation(value = "新增委外处理合同")
    public Result<OutsourceProcessingContractDetailResponse> createContract(
            @Valid @RequestPart("contract") OutsourceProcessingContractCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            OutsourceProcessingContractDetailResponse response = outsourceProcessingContractService.createContract(request, file);
            logRecordService.recordOperationLog("委外处理合同", "新增",
                    "新增委外处理合同：" + response.getContractNo(), userId, ipAddress, true, null);
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "OUTSOURCE_CONTRACT_CREATE",
                        response.getContractId(),
                        response.getContractNo(),
                        "新增",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送新增合同通知失败", msgEx);
            }
            return Result.success("新增成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("委外处理合同", "新增",
                    "新增委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增委外处理合同失败: {}", e.getMessage(), e);
            logRecordService.recordOperationLog("委外处理合同", "新增",
                    "新增委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "新增失败：" + e.getMessage());
        }
    }

    /**
     * 更新
     */
    @RequirePagePermission("合同管理:委托处理合同:页面")
    @PutMapping(value = "/{contractId}", consumes = {"multipart/form-data"})
    @ApiOperation(value = "更新委外处理合同")
    public Result<OutsourceProcessingContractDetailResponse> updateContract(
            @PathVariable Integer contractId,
            @Valid @RequestPart("contract") OutsourceProcessingContractUpdateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            OutsourceProcessingContractDetailResponse response = outsourceProcessingContractService.updateContract(contractId, request, file);
            logRecordService.recordOperationLog("委外处理合同", "编辑",
                    "编辑委外处理合同：" + response.getContractNo(), userId, ipAddress, true, null);
            return Result.success("更新成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("委外处理合同", "编辑",
                    "编辑委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新委外处理合同失败: contractId={}, error={}", contractId, e.getMessage(), e);
            logRecordService.recordOperationLog("委外处理合同", "编辑",
                    "编辑委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除
     */
    @RequirePagePermission("合同管理:委托处理合同:删除")
    @DeleteMapping("/{contractId}")
    @ApiOperation(value = "删除委外处理合同")
    public Result<OutsourceProcessingContractOperationResponse> deleteContract(
            @PathVariable Integer contractId, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            OutsourceProcessingContractOperationResponse response = outsourceProcessingContractService.deleteContract(contractId);
            logRecordService.recordOperationLog("委外处理合同", "删除",
                    "删除委外处理合同：contractId=" + contractId, userId, ipAddress, true, null);
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "OUTSOURCE_CONTRACT_DELETE",
                        contractId,
                        "contractId=" + contractId,
                        "删除",
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送删除合同通知失败", msgEx);
            }
            return Result.success("删除成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("委外处理合同", "删除",
                    "删除委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除委外处理合同失败: contractId={}, error={}", contractId, e.getMessage(), e);
            logRecordService.recordOperationLog("委外处理合同", "删除",
                    "删除委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除
     */
    @RequirePagePermission("合同管理:委托处理合同:删除")
    @DeleteMapping("/batch-delete")
    @ApiOperation(value = "批量删除委外处理合同")
    public Result<OutsourceProcessingContractOperationResponse> batchDeleteContract(
            @RequestBody List<Integer> contractIds, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            OutsourceProcessingContractOperationResponse response = outsourceProcessingContractService.batchDeleteContract(contractIds);
            String message = response.getAllSuccess() ? "批量删除成功" : "批量删除完成，部分失败";
            logRecordService.recordOperationLog("委外处理合同", "批量删除",
                    message + "：成功" + response.getSuccessCount() + "条，失败" + response.getFailCount() + "条",
                    userId, ipAddress, response.getAllSuccess(), null);
            return Result.success(message, response);
        } catch (Exception e) {
            log.error("批量删除委外处理合同失败: {}", e.getMessage(), e);
            logRecordService.recordOperationLog("委外处理合同", "批量删除",
                    "批量删除委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "批量删除失败：" + e.getMessage());
        }
    }

    /**
     * 更新状态
     */
    @RequirePagePermission("合同管理:委托处理合同:审核")
    @PutMapping("/{contractId}/status")
    @ApiOperation(value = "更新委外处理合同状态")
    public Result<OutsourceProcessingContractDetailResponse> updateContractStatus(
            @PathVariable Integer contractId,
            @RequestBody OutsourceProcessingContractStatusRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            OutsourceProcessingContractDetailResponse response = outsourceProcessingContractService.updateContractStatus(
                    contractId, request.getContractStatus(), request.getAuditOpinion());
            logRecordService.recordOperationLog("委外处理合同", "审核",
                    "审核委外处理合同：" + response.getContractNo() + "，状态=" + request.getContractStatus(),
                    userId, ipAddress, true, null);
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "OUTSOURCE_CONTRACT_AUDIT",
                        contractId,
                        response.getContractNo(),
                        request.getContractStatus(),
                        userId
                );
            } catch (Exception msgEx) {
                log.warn("发送审核通知失败", msgEx);
            }
            return Result.success("状态更新成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("委外处理合同", "审核",
                    "审核委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新合同状态失败: contractId={}, error={}", contractId, e.getMessage(), e);
            logRecordService.recordOperationLog("委外处理合同", "审核",
                    "审核委外处理合同失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "状态更新失败：" + e.getMessage());
        }
    }

    /**
     * 下拉列表 - 返回精简字段
     */
    @RequirePagePermission("合同管理:委托处理合同:页面")
    @GetMapping("/select/list")
    @ApiOperation(value = "获取委外处理合同下拉列表")
    public Result<List<OutsourceProcessingContractSelectResponse>> getContractSelectList(
            @RequestParam(required = false) String keyword) {
        try {
            return Result.success("查询成功", outsourceProcessingContractService.getContractSelectList(keyword));
        } catch (Exception e) {
            log.error("查询下拉列表失败: {}", e.getMessage(), e);
            return Result.error(ResultCodeEnum.ERROR.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 批量提交审核
     */
    @RequirePagePermission("合同管理:委托处理合同:审核")
    @PostMapping("/batch-submit-audit")
    @ApiOperation(value = "批量提交审核")
    public Result<OutsourceProcessingContractBatchAuditResponse> batchSubmitAudit(
            @RequestBody OutsourceProcessingContractBatchAuditRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            OutsourceProcessingContractBatchAuditResponse response =
                    outsourceProcessingContractService.batchSubmitAudit(request.getContractIds());
            String message = response.getAllSuccess() ? "批量提交审核成功" : "批量提交审核完成，部分失败";
            logRecordService.recordOperationLog("委外处理合同", "批量提交审核",
                    message + "：成功" + response.getSuccessIds().size() + "条，失败" + response.getFailedIds().size() + "条",
                    userId, ipAddress, response.getAllSuccess(), null);
            return Result.success(message, response);
        } catch (Exception e) {
            log.error("批量提交审核失败: {}", e.getMessage(), e);
            logRecordService.recordOperationLog("委外处理合同", "批量提交审核",
                    "批量提交审核失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "批量提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量撤回审核
     */
    @RequirePagePermission("合同管理:委托处理合同:审核")
    @PostMapping("/batch-withdraw-audit")
    @ApiOperation(value = "批量撤回审核")
    public Result<OutsourceProcessingContractBatchAuditResponse> batchWithdrawAudit(
            @RequestBody OutsourceProcessingContractBatchAuditRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            OutsourceProcessingContractBatchAuditResponse response =
                    outsourceProcessingContractService.batchWithdrawAudit(request.getContractIds());
            String message = response.getAllSuccess() ? "批量撤回审核成功" : "批量撤回审核完成，部分失败";
            logRecordService.recordOperationLog("委外处理合同", "批量撤回审核",
                    message + "：成功" + response.getSuccessIds().size() + "条，失败" + response.getFailedIds().size() + "条",
                    userId, ipAddress, response.getAllSuccess(), null);
            return Result.success(message, response);
        } catch (Exception e) {
            log.error("批量撤回审核失败: {}", e.getMessage(), e);
            logRecordService.recordOperationLog("委外处理合同", "批量撤回审核",
                    "批量撤回审核失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.ERROR.getCode(), "批量撤回审核失败：" + e.getMessage());
        }
    }
}
