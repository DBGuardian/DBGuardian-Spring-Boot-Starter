package com.erp.service.system;

/**
 * 消息通知服务接口
 * 
 * @author ERP System
 * @date 2025-11-27
 */
public interface MessageNotificationService {

    /**
     * 发送预警消息
     * 
     * @param category 消息分类
     * @param title 消息标题
     * @param content 消息内容
     * @param priority 消息优先级
     * @param receiverId 接收人ID
     * @param businessType 关联业务类型
     * @param businessId 关联业务ID
     */
    void sendAlert(String category, String title, String content, 
                  String priority, Integer receiverId, 
                  String businessType, Integer businessId);

    /**
     * 发送业务通知
     * 
     * @param category 消息分类
     * @param title 消息标题
     * @param content 消息内容
     * @param receiverId 接收人ID
     * @param senderId 发送人ID
     * @param businessType 关联业务类型
     * @param businessId 关联业务ID
     */
    void sendBusinessNotification(String category, String title, String content,
                                Integer receiverId, Integer senderId,
                                String businessType, Integer businessId);

    /**
     * 发送系统消息
     * 
     * @param title 消息标题
     * @param content 消息内容
     * @param receiverId 接收人ID
     */
    void sendSystemMessage(String title, String content, Integer receiverId);

    /**
     * 批量发送预警消息
     * 
     * @param category 消息分类
     * @param title 消息标题
     * @param content 消息内容
     * @param priority 消息优先级
     * @param receiverIds 接收人ID列表
     * @param businessType 关联业务类型
     * @param businessId 关联业务ID
     */
    void sendBatchAlert(String category, String title, String content,
                       String priority, Integer[] receiverIds,
                       String businessType, Integer businessId);

    /**
     * 发送合同到期预警
     * 
     * @param contractId 合同ID
     * @param contractTitle 合同标题
     * @param daysLeft 剩余天数
     * @param receiverId 接收人ID
     */
    void sendContractExpiryAlert(Integer contractId, String contractTitle, 
                               int daysLeft, Integer receiverId);

    /**
     * 发送应收款逾期预警
     * 
     * @param settlementId 结算单ID
     * @param customerName 客户名称
     * @param amount 逾期金额
     * @param overdueDays 逾期天数
     * @param receiverId 接收人ID
     */
    void sendOverduePaymentAlert(Integer settlementId, String customerName,
                               String amount, int overdueDays, Integer receiverId);

    /**
     * 发送审核通知
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param businessTitle 业务标题
     * @param action 操作类型（提交审核/审核通过/审核拒绝）
     * @param receiverId 接收人ID
     * @param senderId 发送人ID
     */
    void sendAuditNotification(String businessType, Integer businessId, String businessTitle,
                             String action, Integer receiverId, Integer senderId);

    // ========== 基于权限的消息通知方法 ==========

    /**
     * 根据权限发送业务通知
     * 自动根据业务类型获取有对应权限的员工，发送消息给他们
     *
     * @param businessType 业务类型（如：CONTRACT, PICKUP_NOTICE_SUBMIT, QUOTATION_AUDIT_RESULT 等）
     * @param businessId 业务ID
     * @param title 消息标题
     * @param content 消息内容
     * @param senderId 发送人ID（会被排除，不收到消息）
     * @param category 消息分类（如：合同/客户/财务等）
     */
    void sendBusinessNotificationByPermission(String businessType, Integer businessId,
            String title, String content, Integer senderId, String category);

    /**
     * 根据权限批量发送业务通知
     * 自动根据业务类型获取有对应权限的员工，发送消息给他们
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param title 消息标题
     * @param content 消息内容
     * @param senderId 发送人ID（会被排除，不收到消息）
     * @param category 消息分类
     * @param priority 消息优先级（可选）
     */
    void sendBusinessNotificationByPermission(String businessType, Integer businessId,
            String title, String content, Integer senderId, String category, String priority);

    /**
     * 发送审批提交通知
     * 发送给OA审批人员
     *
     * @param businessType 业务类型（如：CONTRACT_SUBMIT, SETTLEMENT_SUBMIT 等）
     * @param businessId 业务ID
     * @param businessTitle 业务标题
     * @param senderId 发送人ID
     */
    void sendApprovalSubmitNotification(String businessType, Integer businessId,
            String businessTitle, Integer senderId);

    /**
     * 发送审批撤回通知
     * 发送给OA审批人员
     *
     * @param businessType 业务类型（如：CONTRACT_REVOKE, SETTLEMENT_REVOKE 等）
     * @param businessId 业务ID
     * @param businessTitle 业务标题
     * @param senderId 发送人ID
     */
    void sendApprovalRevokeNotification(String businessType, Integer businessId,
            String businessTitle, Integer senderId);

    /**
     * 发送审核结果通知
     * 发送给各模块页面权限人员
     *
     * @param businessType 业务类型（如：CONTRACT_AUDIT_RESULT, SETTLEMENT_AUDIT_RESULT 等）
     * @param businessId 业务ID
     * @param businessTitle 业务标题
     * @param action 审核结果（审核通过/审核驳回）
     * @param senderId 发送人ID（如审核人）
     */
    void sendAuditResultNotification(String businessType, Integer businessId,
            String businessTitle, String action, Integer senderId);

    /**
     * 发送新增/修改操作通知
     * 发送给有当前业务页面权限的人员
     *
     * @param businessType 业务类型（如：CUSTOMER_CREATE, CONTRACT_UPDATE 等）
     * @param businessId 业务ID
     * @param businessTitle 业务标题
     * @param action 操作类型（新增/修改）
     * @param senderId 发送人ID
     */
    void sendBusinessOperationNotification(String businessType, Integer businessId,
            String businessTitle, String action, Integer senderId);
}
