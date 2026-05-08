package com.erp.controller.finance;

import com.erp.common.result.Result;
import com.erp.controller.finance.dto.BatchImportReceiptRequest;
import com.erp.controller.finance.dto.BatchImportReceiptResponse;
import com.erp.controller.finance.dto.FundTransactionCreateRequest;
import com.erp.controller.settlement.dto.SettlementAssociateRequest;
import com.erp.entity.finance.FundTransaction;
import com.erp.service.finance.FundTransactionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import com.erp.common.util.SecurityUtil;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageService;
import com.erp.service.system.dto.MessageDTO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 资金流水控制器
 */
@Slf4j
@RestController
@RequestMapping("/fund")
@Api(tags = "资金流水管理")
@Validated
public class FundTransactionController {

    @Autowired
    private FundTransactionService fundTransactionService;
    @Autowired
    private com.erp.service.common.FileService fileService;
    @Autowired
    private ILogRecordService logRecordService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private com.erp.mapper.finance.SettlementFundTransactionRelMapper settlementFundTransactionRelMapper;
    @Autowired
    private com.erp.mapper.finance.SettlementMapper settlementMapper;

    /**
     * 创建资金流水（新增日记账）
     *
     * 接口名称：创建资金流水
     * 功能描述：创建新的资金流水记录
     * 接口地址：/api/fund/transactions
     * 请求方式：POST
     *
     * 请求体（JSON）：
     * {
     *   "account_id": 1,
     *   "period_id": 3,
     *   "transaction_date": "2023-03-31",
     *   "transaction_type": "EXPENDITURE",
     *   "amount": 600.00,
     *   "counterparty": "供应商A",
     *   "summary": "支付运费",
     *   "fund_category": "运费",
     *   "internal_transfer": false,
     *   "related_account_id": null,
     *   "remark": "备注信息"
     * }
     *
     * 返回体 data：FundTransaction
     */
    @PostMapping("/transactions")
    @ApiOperation(value = "创建资金流水", notes = "创建新的资金流水记录（新增日记账）")
    public Result<FundTransaction> createTransaction(
            @RequestBody @Valid FundTransactionCreateRequest request
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        try {
            FundTransaction transaction = fundTransactionService.createTransaction(request);
            // Controller 层记录操作日志
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "创建",
                        "创建流水：" + transaction.getTransactionCode(), userId, ip, true, null);
            } catch (Exception logEx) {
                log.warn("记录创建资金流水操作日志失败", logEx);
            }
            // 发送消息通知（非阻塞）
            try {
                MessageDTO dto = MessageDTO.createBusinessNotification("财务", "资金流水已创建",
                        "流水号：" + transaction.getTransactionCode(), transaction.getCreateBy().intValue(), userId,
                        "FUND_TRANSACTION", transaction.getTransactionId().intValue());
                messageService.processMessage(dto);
            } catch (Exception msgEx) {
                log.warn("发送创建资金流水消息失败", msgEx);
            }
            return Result.success("创建资金流水成功", transaction);
        } catch (Exception e) {
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "创建", "创建流水失败", userId, ip, false, e.getMessage());
            } catch (Exception ignore) {
                log.warn("记录创建失败日志时异常", ignore);
            }
            log.error("创建资金流水失败，accountId={}, periodId={}, transactionDate={}",
                    request.getAccountId(), request.getPeriodId(),
                    request.getTransactionDate(), e);
            return Result.error("创建资金流水失败：" + e.getMessage());
        }
    }

    /**
     * 上传回单文件并关联到资金流水
     *
     * 接口地址：POST /fund/transactions/{transactionId}/upload-receipt
     * 入参：
     *  - transactionId: 路径参数
     *  - file: MultipartFile
     *
     * 返回：上传后的文件信息（fileId, fileUrl, fileName）
     */
    @PostMapping("/transactions/{transactionId}/upload-receipt")
    @io.swagger.annotations.ApiOperation(value = "上传回单文件并关联资金流水", notes = "上传回单并将文件记录关联到指定的资金流水")
    public Result<java.util.Map<String, Object>> uploadReceiptFile(
            @PathVariable("transactionId") Long transactionId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            javax.servlet.http.HttpServletRequest request) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 调用通用文件上传服务，将 businessType 设为 FUND_RECEIPT，业务ID 设置为 transactionId
            com.erp.entity.common.File fileEntity = fileService.uploadAndSave(file, "FUND_RECEIPT", transactionId.intValue());

            // 关联到资金流水（只记录文件编号，不写回单编号）
            fundTransactionService.attachReceiptFile(transactionId, fileEntity.getFileId(), fileEntity.getFileName());

            // 记录操作日志（若有日志服务）
            // 记录操作日志：使用项目现有的 LogRecordService 和 SecurityUtil
            try {
                Integer userId = SecurityUtil.getCurrentUserId();
                String ip = logRecordService.getClientIp(request);
                String content = "上传回单文件：" + fileEntity.getFileName() + "，流水ID：" + transactionId;
                logRecordService.recordOperationLog("资金流水", "上传回单", content, userId, ip, true, null);
            } catch (Exception e) {
                log.warn("记录回单上传日志失败", e);
            }

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("fileId", fileEntity.getFileId());
            data.put("fileUrl", fileEntity.getFileUrl());
            data.put("fileName", fileEntity.getFileName());
            return Result.success("回单上传并关联成功", data);
        } catch (Exception e) {
            log.error("上传回单失败，transactionId={}", transactionId, e);
            return Result.error("上传回单失败：" + e.getMessage());
        }
    }

    /**
     * 批量导入银行回单
     *
     * 接口地址：POST /fund/transactions/batch-import-receipts
     * 功能：上传多个PDF文件，通过PDFBox解析文本匹配回单编号，自动关联到对应的资金流水
     * 入参：
     *  - accountId: 账户ID
     *  - periodId: 账期ID
     *  - files: 文件列表（文件名和Base64编码的文件内容）
     *
     * 返回：导入结果（成功/失败详情）
     */
    @PostMapping("/transactions/batch-import-receipts")
    @ApiOperation(value = "批量导入银行回单", notes = "上传多个PDF文件，通过PDF文本匹配回单编号，自动关联到对应的资金流水")
    public Result<BatchImportReceiptResponse> batchImportReceipts(
            @RequestBody @Valid BatchImportReceiptRequest request,
            javax.servlet.http.HttpServletRequest httpRequest) {
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            log.info("批量导入银行回单，accountId={}, periodId={}, fileCount={}",
                    request.getAccountId(), request.getPeriodId(),
                    request.getFiles() != null ? request.getFiles().size() : 0);

            BatchImportReceiptResponse response = fundTransactionService.batchImportReceipts(
                    request.getAccountId(),
                    request.getPeriodId(),
                    request.getFiles(),
                    userId);

            // 记录操作日志
            try {
                String ip = logRecordService.getClientIp(httpRequest);
                String content = String.format("批量导入银行回单：总文件数=%d, 成功=%d, 失败=%d, 未匹配=%d",
                        response.getTotalCount(), response.getSuccessCount(),
                        response.getFailCount(), response.getNotFoundCount());
                logRecordService.recordOperationLog("资金流水", "批量导入回单", content, userId, ip, true, null);
            } catch (Exception logEx) {
                log.warn("记录批量导入回单日志失败", logEx);
            }

            return Result.success("批量导入完成", response);
        } catch (Exception e) {
            log.error("批量导入银行回单失败，accountId={}, periodId={}",
                    request.getAccountId(), request.getPeriodId(), e);
            return Result.error("批量导入银行回单失败：" + e.getMessage());
        }
    }

    /**
     * 下载回单文件（通过流水ID）
     *
     * 接口地址：GET /fund/transactions/{transactionId}/download-receipt
     * 功能：根据资金流水ID，查找关联的回单文件ID并重定向到文件下载接口
     */
    @GetMapping("/transactions/{transactionId}/download-receipt")
    @ApiOperation(value = "下载回单文件", notes = "根据资金流水ID下载关联的回单文件")
    public ResponseEntity<Void> downloadReceiptByTransaction(@PathVariable("transactionId") Long transactionId) {
        try {
            FundTransaction tx = fundTransactionService.getTransactionById(transactionId);
            if (tx == null) {
                return ResponseEntity.notFound().build();
            }
            Integer fileId = tx.getReceiptFile();
            if (fileId == null) {
                return ResponseEntity.notFound().build();
            }
            // 重定向到文件下载接口（文件服务统一入口）
            String location = "/file/download/" + fileId;
            return ResponseEntity.status(302).header(org.springframework.http.HttpHeaders.LOCATION, location).build();
        } catch (Exception e) {
            log.error("下载回单失败，transactionId={}", transactionId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 更新资金流水（修改日记账）
     *
     * 接口名称：更新资金流水
     * 功能描述：更新资金流水记录
     * 接口地址：/api/fund/transactions/{transactionId}
     * 请求方式：POST
     *
     * 请求体（JSON）：同创建接口
     *
     * 返回体 data：FundTransaction
     */
    @PostMapping("/transactions/{transactionId}")
    @ApiOperation(value = "更新资金流水", notes = "更新资金流水记录（修改日记账）")
    public Result<FundTransaction> updateTransaction(
            @PathVariable("transactionId") Long transactionId,
            @RequestBody @Valid FundTransactionCreateRequest request
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        try {
            FundTransaction transaction = fundTransactionService.updateTransaction(transactionId, request);
            // Controller 层记录操作日志
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "编辑",
                        "更新流水：" + transaction.getTransactionCode(), userId, ip, true, null);
            } catch (Exception logEx) {
                log.warn("记录更新资金流水操作日志失败", logEx);
            }
            // 发送消息通知（非阻塞）
            try {
                MessageDTO dto = MessageDTO.createBusinessNotification("财务", "资金流水已更新",
                        "流水号：" + transaction.getTransactionCode(), transaction.getCreateBy().intValue(), userId,
                        "FUND_TRANSACTION", transaction.getTransactionId().intValue());
                messageService.processMessage(dto);
            } catch (Exception msgEx) {
                log.warn("发送更新资金流水消息失败", msgEx);
            }
            return Result.success("更新资金流水成功", transaction);
        } catch (Exception e) {
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "编辑", "更新流水失败", userId, ip, false, e.getMessage());
            } catch (Exception ignore) {
                log.warn("记录更新失败日志时异常", ignore);
            }
            log.error("更新资金流水失败，transactionId={}, accountId={}, periodId={}",
                    transactionId, request.getAccountId(), request.getPeriodId(), e);
            return Result.error("更新资金流水失败：" + e.getMessage());
        }
    }

    /**
     * 删除资金流水（删除日记账）
     *
     * 接口名称：删除资金流水
     * 功能描述：删除资金流水记录
     * 接口地址：/api/fund/transactions/{transactionId}
     * 请求方式：DELETE
     *
     * 返回体 data：{ transactionId: number }
     */
    @DeleteMapping("/transactions/{transactionId}")
    @ApiOperation(value = "删除资金流水", notes = "删除资金流水记录（删除日记账）")
    public Result<Map<String, Long>> deleteTransaction(
            @PathVariable("transactionId") Long transactionId
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        try {
            fundTransactionService.deleteTransaction(transactionId);
            Map<String, Long> result = new HashMap<>();
            result.put("transactionId", transactionId);
            // 记录操作日志
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "删除", "删除流水，ID：" + transactionId, userId, ip, true, null);
            } catch (Exception logEx) {
                log.warn("记录删除资金流水操作日志失败", logEx);
            }
            // 发送消息（通知相关人员）
            try {
                MessageDTO dto = MessageDTO.createBusinessNotification("财务", "资金流水已删除",
                        "流水ID：" + transactionId, null, userId, "FUND_TRANSACTION", transactionId.intValue());
                messageService.processMessage(dto);
            } catch (Exception msgEx) {
                log.warn("发送删除资金流水消息失败", msgEx);
            }
            return Result.success("删除资金流水成功", result);
        } catch (Exception e) {
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "删除", "删除流水失败，ID：" + transactionId, userId, ip, false, e.getMessage());
            } catch (Exception ignore) {
                log.warn("记录删除失败日志时异常", ignore);
            }
            log.error("删除资金流水失败，transactionId={}", transactionId, e);
            return Result.error("删除资金流水失败：" + e.getMessage());
        }
    }

    /**
     * 查询账户的流水记录（用于关联流水编码选择）
     *
     * 接口名称：查询账户流水记录
     * 功能描述：查询指定账户的所有流水记录，用于内部往来的关联流水编码选择
     * 接口地址：/api/fund/transactions/account/{accountId}
     * 请求方式：GET
     *
     * 请求参数：
     * - accountId：账户ID（路径参数）
     * - periodId：账期ID（可选，用于筛选特定账期的流水）
     *
     * 返回体 data：流水记录列表
     */
    @GetMapping("/transactions/account/{accountId}")
    @ApiOperation(value = "查询账户流水记录", notes = "查询指定账户的所有流水记录，用于关联流水编码选择")
    public Result<java.util.List<Map<String, Object>>> getAccountTransactions(
            @PathVariable("accountId") Long accountId,
            @RequestParam(value = "periodId", required = false) Long periodId
    ) {
        try {
            java.util.List<FundTransaction> transactions = fundTransactionService.getAccountTransactions(accountId, periodId);

            // 转换为前端需要的格式
            java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (FundTransaction tx : transactions) {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("transactionId", tx.getTransactionId());
                item.put("transactionCode", tx.getTransactionCode());
                item.put("transactionDate", tx.getTransactionDate().toString());
                item.put("summary", tx.getSummary());
                item.put("amount", tx.getAmount());
                item.put("direction", tx.getTransactionType().equals("INCOME") ? "收入" : "支出");
                result.add(item);
            }

            return Result.success("查询账户流水记录成功", result);
        } catch (Exception e) {
            log.error("查询账户流水记录失败，accountId={}, periodId={}", accountId, periodId, e);
            return Result.error("查询账户流水记录失败：" + e.getMessage());
        }
    }

    /**
     * 将结算单关联到资金流水（批量）
     * POST /fund/transactions/{transactionId}/associate-settlements
     */
    @PostMapping("/transactions/{transactionId}/associate-settlements")
    @ApiOperation(value = "将结算单关联到资金流水", notes = "把多个结算单关联到指定资金流水，若结算单已被其他流水关联则校验失败")
    public Result<Void> associateSettlements(@PathVariable("transactionId") Long transactionId,
                                             @RequestBody SettlementAssociateRequest request) {
        Integer userId = SecurityUtil.getCurrentUserId();
        try {
            fundTransactionService.associateSettlements(transactionId, request == null ? null : request.getSettlementRelations());
            // 记录操作日志
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "关联结算单", "关联结算单，流水ID：" + transactionId, userId, ip, true, null);
            } catch (Exception logEx) {
                log.warn("记录关联结算单操作日志失败", logEx);
            }
            // 发送消息通知（非阻塞）
            try {
                MessageDTO dto = MessageDTO.createBusinessNotification("财务", "流水关联结算单",
                        "流水ID：" + transactionId + " 已关联结算单", null, userId, "FUND_TRANSACTION", transactionId.intValue());
                messageService.processMessage(dto);
            } catch (Exception msgEx) {
                log.warn("发送关联结算单消息失败", msgEx);
            }
            return Result.success("关联成功", null);
        } catch (com.erp.common.exception.BusinessException be) {
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "关联结算单", "关联结算单校验失败", userId, ip, false, be.getMessage());
            } catch (Exception ignore) {
                log.warn("记录关联失败日志时异常", ignore);
            }
            log.warn("关联结算单校验失败：transactionId={}, error={}", transactionId, be.getMessage());
            return Result.error(be.getMessage());
        } catch (Exception e) {
            try {
                String ip = logRecordService.getClientIp(null);
                logRecordService.recordOperationLog("资金流水", "关联结算单", "关联结算单异常", userId, ip, false, e.getMessage());
            } catch (Exception ignore) {
                log.warn("记录关联异常日志时异常", ignore);
            }
            log.error("关联结算单失败，transactionId={}", transactionId, e);
            return Result.error("关联结算单失败：" + e.getMessage());
        }
    }

    /**
     * 查询指定资金流水已关联的结算单列表（连表显示结算信息与关联金额）
     *
     * GET /fund/transactions/{transactionId}/associated-settlements
     */
    @GetMapping("/transactions/{transactionId}/associated-settlements")
    @ApiOperation(value = "查询资金流水已关联的结算单", notes = "根据资金流水ID查询已关联的结算单列表")
    public Result<java.util.List<java.util.Map<String, Object>>> getAssociatedSettlements(
            @PathVariable("transactionId") Long transactionId) {
        try {
            java.util.List<java.util.Map<String, Object>> result = fundTransactionService.getAssociatedSettlements(transactionId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询已关联结算单失败，transactionId={}", transactionId, e);
            return Result.error("查询已关联结算单失败：" + e.getMessage());
        }
    }
}


