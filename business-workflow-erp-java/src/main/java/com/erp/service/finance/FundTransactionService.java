package com.erp.service.finance;

import com.erp.controller.finance.dto.FundTransactionCreateRequest;
import com.erp.controller.settlement.dto.SettlementAssociateRequest;
import com.erp.entity.finance.FundTransaction;

/**
 * 资金流水服务接口
 */
public interface FundTransactionService {

    /**
     * 创建资金流水
     *
     * @param request 创建请求
     * @return 创建的流水记录
     */
    FundTransaction createTransaction(FundTransactionCreateRequest request);

    /**
     * 更新资金流水
     *
     * @param transactionId 流水ID
     * @param request 更新请求
     * @return 更新后的流水记录
     */
    FundTransaction updateTransaction(Long transactionId, FundTransactionCreateRequest request);

    /**
     * 删除资金流水
     *
     * @param transactionId 流水ID
     */
    void deleteTransaction(Long transactionId);

    /**
     * 关联回单文件到资金流水
     *
     * @param transactionId 流水ID
     * @param fileId 回单文件ID
     * @param fileName 文件名（可选，用于记录或显示）
     */
    void attachReceiptFile(Long transactionId, Integer fileId, String fileName);

    /**
     * 根据ID查询资金流水
     *
     * @param transactionId 流水ID
     * @return FundTransaction 或 null
     */
    FundTransaction getTransactionById(Long transactionId);

    /**
     * 查询账户的流水记录（用于关联流水编码选择）
     *
     * @param accountId 账户ID
     * @param periodId 账期ID（可选，用于筛选特定账期的流水）
     * @return 流水记录列表
     */
    java.util.List<FundTransaction> getAccountTransactions(Long accountId, Long periodId);
    
    /**
     * 查询指定资金流水已关联的结算单（包含关联金额）
     *
     * @param transactionId 流水ID
     * @return 已关联的结算单信息列表（Map格式，包含settlementId, settlementCode, settlementType, settlementPeriodStart, settlementPeriodEnd, settlementAmount, status, relAmount）
     */
    java.util.List<java.util.Map<String, Object>> getAssociatedSettlements(Long transactionId);
    
    /**
     * 将结算单关联到资金流水（批量）
     *
     * @param transactionId 流水ID
     * @param settlementRelations 关联列表
     */
    void associateSettlements(Long transactionId, java.util.List<SettlementAssociateRequest.SettlementRelation> settlementRelations);

    /**
     * 批量导入银行回单（通过PDF文本匹配回单编号）
     *
     * @param accountId 账户ID
     * @param periodId 账期ID
     * @param files 文件列表（文件名和Base64内容）
     * @param uploaderId 上传人ID
     * @return 导入结果
     */
    com.erp.controller.finance.dto.BatchImportReceiptResponse batchImportReceipts(
            Long accountId, Long periodId,
            java.util.List<com.erp.controller.finance.dto.BatchImportReceiptRequest.ReceiptFileItem> files,
            Integer uploaderId);

    /**
     * 根据账户ID和账期ID查询所有流水（用于回单匹配）
     *
     * @param accountId 账户ID
     * @param periodId 账期ID
     * @return 流水列表
     */
    java.util.List<FundTransaction> getTransactionsByAccountAndPeriod(Long accountId, Long periodId);
}


