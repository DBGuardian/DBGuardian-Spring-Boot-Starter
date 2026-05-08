package com.erp.controller.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.contract.dto.*;
import com.erp.controller.contract.dto.ContractBatchSubmitAuditRequest;
import com.erp.controller.contract.dto.ContractBatchSubmitAuditResponse;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.service.contract.ContractService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.erp.util.ContractWordGenerator;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 合同管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/contract")
@Api(tags = "合同管理")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 新增合同（从客户列表生成报价单并上传合同PDF）
     */
    @RequireActionPermission({
            "合同管理:合同变更:新增",
            "合同管理:危险废物合同:合同订立"
    })
    @PostMapping(consumes = {"multipart/form-data"})
    @ApiOperation(value = "新增合同", notes = "从客户列表页面点击生成报价单按钮新增合同，并上传合同PDF文件")
    public Result<ContractDetailResponse> createContract(
            @Valid @RequestPart("contract") ContractCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            ContractDetailResponse response = contractService.createContract(request, file);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("合同管理", "新增", 
                    "新增合同：合同号=" + (response.getContractNo() != null ? response.getContractNo() : "ID=" + response.getContractId()), 
                    userId, ipAddress, true, null);
            return Result.success("新增合同成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "新增", 
                    "新增合同失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增合同失败", e);
            logRecordService.recordOperationLog("合同管理", "新增", 
                    "新增合同失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增合同失败：" + e.getMessage());
        }
    }

    /**
     * 合同分页查询
     */
    @RequirePagePermission({
            "合同管理:危险废物合同:页面",
            "合同管理:合同订立:页面",
            "合同管理:合同变更:页面",
            "合同管理:合同履行:页面"
    })
    @GetMapping("/list")
    @ApiOperation(value = "合同分页查询", notes = "支持按客户名称（模糊）、合同状态（精确）、签订时间范围、有效期范围、文档状态筛选")
    public Result<IPage<ContractPageResponse>> getContractPage(@Valid ContractPageRequest request) {
        try {
            IPage<ContractPageResponse> page = contractService.getContractPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    /**
     * 合同号模糊查询（用于运输申请等场景）
     */
    @RequirePagePermission({
            "财务管理:发票管理:页面",
            "业务管理:开票通知:页面",
            "业务管理:收运通知:页面",
            "财务管理:资金管理:日记账:页面"
    })
    @GetMapping("/search")
    @ApiOperation(value = "合同号模糊查询", notes = "根据合同号关键字模糊查询合同列表，用于运输申请等场景的合同选择")
    public Result<IPage<ContractPageResponse>> searchContracts(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String keyword,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String viewScope,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") @Min(1) long current,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") @Min(1) long size) {
        try {
            ContractPageRequest request = new ContractPageRequest();
            request.setCurrent(current);
            request.setSize(size);
            // 如果有关键字，同时搜索合同号和客户名称
            if (keyword != null && !keyword.trim().isEmpty()) {
                request.setEnterpriseName(keyword.trim());
            }
            IPage<ContractPageResponse> page = contractService.searchContracts(keyword, viewScope, current, size);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("合同号搜索失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "搜索失败：" + e.getMessage());
        }
    }

    /**
     * 获取合同统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取合同统计信息", notes = "获取合同统计数据，包含总数、执行中、已完结、待审核等")
    public Result<ContractStatistics> getContractStatistics() {
        try {
            ContractStatistics statistics = contractService.getContractStatistics();
            return Result.success("查询成功", statistics);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取合同统计信息失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 合同详情
     */
    @GetMapping("/{contractId}")
    @ApiOperation(value = "合同详情", notes = "根据合同编号查询详情")
    public Result<ContractDetailResponse> getContractDetail(@PathVariable("contractId") Integer contractId) {
        try {
            ContractDetailResponse response = contractService.getContractDetail(contractId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取合同详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取合同详情失败：" + e.getMessage());
        }
    }

    /**
     * 更新合同（支持重新上传合同PDF）
     */
    @RequireActionPermission("合同管理:合同变更:编辑")
    @PutMapping(value = "/{contractId}", consumes = {"multipart/form-data"})
    @ApiOperation(value = "编辑合同", notes = "编辑合同基础信息，可重新上传合同PDF")
    public Result<Void> updateContract(
            @PathVariable Integer contractId,
            @Valid @RequestPart("contract") ContractUpdateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            request.setContractId(contractId);
            contractService.updateContract(request, file);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("合同管理", "编辑", 
                    "更新合同：ID=" + contractId, userId, ipAddress, true, null);
            return Result.success("更新合同成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "编辑", 
                    "更新合同：ID=" + contractId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新合同失败", e);
            logRecordService.recordOperationLog("合同管理", "编辑", 
                    "更新合同：ID=" + contractId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新合同失败：" + e.getMessage());
        }
    }

    /**
     * 生成合同PDF
     */
    @RequireActionPermission("合同管理:合同变更:生成文档")
    @PostMapping("/{contractId}/pdf")
    @ApiOperation(value = "生成合同PDF", notes = "为指定合同生成PDF文件")
    public Result<ContractDetailResponse> generatePdf(@PathVariable("contractId") Integer contractId,
                                                        HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            ContractDetailResponse response = contractService.generatePdf(contractId);
            // 记录生成操作日志
            logRecordService.recordOperationLog("合同管理", "生成", 
                    "生成合同PDF：ID=" + contractId, userId, ipAddress, true, null);
            return Result.success("PDF生成成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "生成", 
                    "生成合同PDF：ID=" + contractId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("生成合同PDF失败", e);
            logRecordService.recordOperationLog("合同管理", "生成", 
                    "生成合同PDF：ID=" + contractId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成PDF失败：" + e.getMessage());
        }
    }

    /**
     * 更新合同状态（审核状态变更）
     */
    @RequireActionPermission("合同管理:合同变更:审核")
    @PutMapping("/{contractId}/status")
    @ApiOperation(value = "更新合同状态", notes = "更新合同状态（待审核、执行中、已完结、已归档、已驳回等），状态变更后会发送消息通知")
    public Result<Void> updateContractStatus(
            @PathVariable("contractId") Integer contractId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ContractStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            request.setContractId(contractId);
            // 获取旧状态用于日志记录
            ContractDetailResponse oldContract = contractService.getContractDetail(contractId);
            String oldStatus = oldContract.getContractStatus();
            
            contractService.updateContractStatus(request.getContractId(), request.getContractStatus(), request.getAuditOpinion());
            
            // 记录状态变更操作日志
            String content = String.format("更新合同状态：ID=%d，%s → %s", contractId, oldStatus, request.getContractStatus());
            if (request.getAuditOpinion() != null && !request.getAuditOpinion().isEmpty()) {
                content += "，审核意见：" + request.getAuditOpinion();
            }
            logRecordService.recordOperationLog("合同管理", "状态变更", content, userId, ipAddress, true, null);
            return Result.success("合同状态更新成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "状态变更", 
                    "更新合同状态：ID=" + contractId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新合同状态失败", e);
            logRecordService.recordOperationLog("合同管理", "状态变更", 
                    "更新合同状态：ID=" + contractId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新合同状态失败：" + e.getMessage());
        }
    }

    /**
     * 导出合同Word文档
     */
    @RequireActionPermission("合同管理:合同变更:导出文档")
    @GetMapping("/{contractId}/word")
    @ApiOperation(value = "导出合同Word", notes = "为指定合同生成并下载Word文档")
    public void exportContractWord(@PathVariable("contractId") Integer contractId,
                                    HttpServletResponse response,
                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 操作范围控制：仅允许在“仅操作自己”范围内导出自己创建的合同文档
            // 实际的 operateScope 配置与校验逻辑在 Service 层已处理，这里只做数据归属的快速防御性校验
            try {
                ContractDetailResponse detailForScope = contractService.getContractDetail(contractId);
                Integer creatorId = detailForScope.getCreatorId();
                if (creatorId != null && !creatorId.equals(userId)) {
                    // 具体 SELF / ALL 判定由 Service 层依据 EmployeePermission 决定；
                    // 这里仅在“明显不是自己创建”的情况下给予友好提示，防止误操作。
                    log.info("导出合同Word时检测到当前用户不是创建人：contractId={}, creatorId={}, currentUser={}",
                            contractId, creatorId, userId);
                }
            } catch (Exception e) {
                // 不中断导出流程，仅记录日志
                log.warn("导出合同Word前的操作范围快速校验失败：contractId={}, error={}", contractId, e.getMessage());
            }

            ContractDetailResponse detail = contractService.getContractDetail(contractId);
            // 生成文件名：合同_客户名称_合同号.docx，无号时用 contract-{ID}.docx
            String customerName = "";
            if (detail.getCustomerSnapshot() != null && detail.getCustomerSnapshot().getCustomerName() != null) {
                customerName = detail.getCustomerSnapshot().getCustomerName();
            }
            String contractNo = detail.getContractNo();
            String baseName = (contractNo != null && !contractNo.trim().isEmpty()) ? contractNo : "contract-" + contractId;
            String fileName = customerName.isEmpty() ? baseName : "合同_" + customerName + "_" + baseName;
            // 过滤非法字符
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_") + ".docx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());

            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);

            try (ServletOutputStream outputStream = response.getOutputStream()) {
                ContractWordGenerator.generateWord(detail, outputStream);
                outputStream.flush();
            }
            // 记录导出操作日志
            logRecordService.recordOperationLog("合同管理", "导出", 
                    "导出合同Word：ID=" + contractId, userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "导出", 
                    "导出合同Word：ID=" + contractId, userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出合同Word失败", e);
            logRecordService.recordOperationLog("合同管理", "导出", 
                    "导出合同Word：ID=" + contractId, userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "导出Word失败：" + e.getMessage());
        }
    }

    /**
     * 获取合同执行进度
     *
     * <p>说明：该接口作为“合同履行”列表下的审批流/执行进度详情入口，
     * 不再单独绑定页面级权限编码，权限由“合同管理:合同履行:审批流”等动作级权限在前端控制入口按钮。</p>
     */
    @RequireActionPermission("合同管理:合同履行:审批流")
    @GetMapping("/{contractId}/progress")
    @ApiOperation(value = "获取合同执行进度", notes = "查询合同执行进度，包括合同创建、合同审核、收运通知、入库完成、结算完成、合同完成等状态")
    public Result<ContractProgressResponse> getContractProgress(@PathVariable("contractId") Integer contractId) {
        try {
            ContractProgressResponse response = contractService.getContractProgress(contractId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取合同执行进度失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取合同执行进度失败：" + e.getMessage());
        }
    }

    /**
     * 获取合同的危废条目明细和价外服务
     */
    @GetMapping("/{contractId}/waste-items")
    @ApiOperation(value = "获取合同危废条目明细和价外服务", notes = "根据合同编号获取合同包含的所有危废条目明细信息和价外服务，用于结算单自动填充")
    public Result<ContractWasteItemsAndServicesResponse> getContractWasteItems(@PathVariable("contractId") Integer contractId) {
        try {
            ContractWasteItemsAndServicesResponse response = contractService.getContractWasteItems(contractId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取合同危废条目和价外服务失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取合同危废条目和价外服务失败：" + e.getMessage());
        }
    }

    /**
     * 批量寄件
     */
    @RequireActionPermission("合同管理:合同变更:批量寄件")
    @PostMapping("/batch-send-mail")
    @ApiOperation(value = "批量寄件", notes = "批量确认合同寄件时间")
    public Result<Void> batchSendMail(
            @Valid @org.springframework.web.bind.annotation.RequestBody ContractBatchMailRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            contractService.batchSendMail(request);
            logRecordService.recordOperationLog("合同管理", "批量寄件",
                    "批量寄件：contractIds=" + request.getContractIds(),
                    userId, ipAddress, true, null);
            return Result.success("批量寄件成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "批量寄件",
                    "批量寄件失败：contractIds=" + request.getContractIds(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量寄件失败", e);
            logRecordService.recordOperationLog("合同管理", "批量寄件",
                    "批量寄件失败：contractIds=" + request.getContractIds(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量寄件失败：" + e.getMessage());
        }
    }

    /**
     * 批量收件
     */
    @RequireActionPermission("合同管理:合同变更:批量收件")
    @PostMapping("/batch-receive-mail")
    @ApiOperation(value = "批量收件", notes = "批量确认合同收件时间")
    public Result<Void> batchReceiveMail(
            @Valid @org.springframework.web.bind.annotation.RequestBody ContractBatchMailRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            contractService.batchReceiveMail(request);
            logRecordService.recordOperationLog("合同管理", "批量收件",
                    "批量收件：contractIds=" + request.getContractIds(),
                    userId, ipAddress, true, null);
            return Result.success("批量收件成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "批量收件",
                    "批量收件失败：contractIds=" + request.getContractIds(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量收件失败", e);
            logRecordService.recordOperationLog("合同管理", "批量收件",
                    "批量收件失败：contractIds=" + request.getContractIds(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量收件失败：" + e.getMessage());
        }
    }

    /**
     * 业务费结算列表专用合同查询接口
     * 功能描述：查询用于业务费结算的合同列表，返回合同号、甲方名称、联系人、合同状态、有效期等字段
     * 以及未关联结算单的入库单数量（unlinkedInboundCount）
     * 仅返回执行中和已完结状态的合同
     */
    @GetMapping("/settlement/list")
    @ApiOperation(value = "业务费结算合同列表查询", notes = "查询用于业务费结算的合同列表，返回合同信息及未关联结算单的入库单数量，仅返回执行中和已完结状态的合同")
    public Result<ContractSettlementListResponse> getContractForSettlement() {
        try {
            ContractSettlementListResponse response = contractService.getContractForSettlement();
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询业务费结算合同列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 批量提交审核
     * 功能描述：批量将合同提交OA审核，创建审核记录，将合同状态改为审核中
     */
    @RequireActionPermission("合同管理:合同变更:批量提交审核")
    @PostMapping("/batch-submit-audit")
    @ApiOperation(value = "批量提交审核", notes = "批量将合同提交OA审核，创建审核记录，将合同状态改为待审核")
    public Result<ContractBatchSubmitAuditResponse> batchSubmitAudit(
            @Valid @org.springframework.web.bind.annotation.RequestBody ContractBatchSubmitAuditRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            ContractBatchSubmitAuditResponse response = contractService.batchSubmitAudit(request);
            logRecordService.recordOperationLog("合同管理", "批量提交审核",
                    "批量提交审核：contractIds=" + request.getContractIds(),
                    userId, ipAddress, true, null);
            return Result.success("批量提交审核完成", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "批量提交审核",
                    "批量提交审核失败：contractIds=" + request.getContractIds(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量提交审核失败", e);
            logRecordService.recordOperationLog("合同管理", "批量提交审核",
                    "批量提交审核失败：contractIds=" + request.getContractIds(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量提交审核失败：" + e.getMessage());
        }
    }

    /**
     * 批量撤回审核
     * 功能描述：批量撤回审核中的合同，将合同状态改回待审核，并同步更新OA审核记录为已撤回
     */
    @RequireActionPermission("合同管理:合同变更:批量撤回审核")
    @PostMapping("/batch-withdraw-audit")
    @ApiOperation(value = "批量撤回审核", notes = "批量撤回审核中的合同，将合同状态改为待审核，同时将OA审核状态改为已撤回、审核次数减1且最小为0")
    public Result<ContractBatchSubmitAuditResponse> batchWithdrawAudit(
            @Valid @org.springframework.web.bind.annotation.RequestBody ContractBatchSubmitAuditRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            ContractBatchSubmitAuditResponse response = contractService.batchWithdrawAudit(request);
            logRecordService.recordOperationLog("合同管理", "批量撤回审核",
                    "批量撤回审核：contractIds=" + request.getContractIds(),
                    userId, ipAddress, true, null);
            return Result.success("批量撤回审核完成", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("合同管理", "批量撤回审核",
                    "批量撤回审核失败：contractIds=" + request.getContractIds(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量撤回审核失败", e);
            logRecordService.recordOperationLog("合同管理", "批量撤回审核",
                    "批量撤回审核失败：contractIds=" + request.getContractIds(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量撤回审核失败：" + e.getMessage());
        }
    }

    /**
     * 危废合同下拉列表
     * 功能描述：专门为下拉选择场景设计的轻量接口，只返回合同ID、合同号、企业名称三个字段
     * 入参：keyword - 合同号或企业名称的模糊搜索关键字
     *       viewScope - 数据范围（SELF/ALL），下拉选择场景应传ALL
     */
    @GetMapping("/select/list")
    @ApiOperation(value = "危废合同下拉列表", notes = "专门为下拉选择场景设计的轻量接口，只返回合同ID、合同号、企业名称三个字段")
    public Result<List<ContractSelectResponse>> getContractSelectList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String viewScope) {
        try {
            List<ContractSelectResponse> list = contractService.getContractSelectList(keyword, viewScope);
            return Result.success("查询成功", list);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询危废合同下拉列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }
}





