package com.erp.service.system.impl;

import com.erp.config.RabbitMQConfig;
import com.erp.service.system.EmployeePermissionService;
import com.erp.service.system.MessageNotificationService;
import com.erp.service.system.MessageService;
import com.erp.service.system.dto.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息通知服务实现类
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Slf4j
@Service
public class MessageNotificationServiceImpl implements MessageNotificationService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageService messageService;

    @Override
    public void sendAlert(String category, String title, String content, 
                         String priority, Integer receiverId, 
                         String businessType, Integer businessId) {
        try {
            MessageDTO messageDTO = MessageDTO.createAlert(category, title, content, 
                                                         priority, receiverId, 
                                                         businessType, businessId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.ALERT_EXCHANGE, 
                                        RabbitMQConfig.ALERT_ROUTING_KEY, 
                                        messageDTO);
            log.info("预警消息发送成功: receiverId={}, title={}", receiverId, title);
        } catch (Exception e) {
            log.error("预警消息发送失败: receiverId={}, title={}", receiverId, title, e);
        }
    }

    @Override
    public void sendBusinessNotification(String category, String title, String content,
                                       Integer receiverId, Integer senderId,
                                       String businessType, Integer businessId) {
        try {
            MessageDTO messageDTO = MessageDTO.createBusinessNotification(category, title, content,
                                                                        receiverId, senderId,
                                                                        businessType, businessId);
            log.info("准备发送业务通知到RabbitMQ: receiverId={}, title={}, businessType={}, businessId={}", 
                    receiverId, title, businessType, businessId);
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.BUSINESS_EXCHANGE, 
                                        RabbitMQConfig.BUSINESS_ROUTING_KEY, 
                                        messageDTO);
            
            log.info("业务通知发送到RabbitMQ成功: receiverId={}, title={}", receiverId, title);
        } catch (Exception e) {
            log.error("业务通知发送到RabbitMQ失败: receiverId={}, title={}, 错误信息: {}", 
                    receiverId, title, e.getMessage(), e);
            // 如果RabbitMQ发送失败，尝试直接保存到数据库（降级方案）
            try {
                log.warn("RabbitMQ发送失败，尝试直接保存消息到数据库: receiverId={}, title={}", receiverId, title);
                MessageDTO messageDTO = MessageDTO.createBusinessNotification(category, title, content,
                                                                            receiverId, senderId,
                                                                            businessType, businessId);
                // 直接调用MessageService保存消息
                messageService.processMessage(messageDTO);
                log.info("消息已直接保存到数据库: receiverId={}, title={}", receiverId, title);
            } catch (Exception dbException) {
                log.error("直接保存消息到数据库也失败: receiverId={}, title={}, 错误信息: {}", 
                        receiverId, title, dbException.getMessage(), dbException);
            }
        }
    }

    @Override
    public void sendSystemMessage(String title, String content, Integer receiverId) {
        try {
            MessageDTO messageDTO = MessageDTO.createSystemMessage(title, content, receiverId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.SYSTEM_EXCHANGE, 
                                        RabbitMQConfig.SYSTEM_ROUTING_KEY, 
                                        messageDTO);
            log.info("系统消息发送成功: receiverId={}, title={}", receiverId, title);
        } catch (Exception e) {
            log.error("系统消息发送失败: receiverId={}, title={}", receiverId, title, e);
        }
    }

    @Override
    public void sendBatchAlert(String category, String title, String content,
                              String priority, Integer[] receiverIds,
                              String businessType, Integer businessId) {
        for (Integer receiverId : receiverIds) {
            sendAlert(category, title, content, priority, receiverId, businessType, businessId);
        }
    }

    @Override
    public void sendContractExpiryAlert(Integer contractId, String contractTitle, 
                                      int daysLeft, Integer receiverId) {
        String title = "合同到期预警";
        String content = String.format("合同《%s》将在%d天后到期，请及时处理续签事宜。", 
                                     contractTitle, daysLeft);
        String priority = daysLeft <= 7 ? "紧急" : "高";
        
        sendAlert("合同", title, content, priority, receiverId, "CONTRACT", contractId);
    }

    @Override
    public void sendOverduePaymentAlert(Integer settlementId, String customerName,
                                      String amount, int overdueDays, Integer receiverId) {
        String title = "应收款逾期预警";
        String content = String.format("客户【%s】的应收款%s已逾期%d天，请及时催收。", 
                                     customerName, amount, overdueDays);
        String priority = "紧急";
        
        sendAlert("财务", title, content, priority, receiverId, "SETTLEMENT", settlementId);
    }

    @Override
    public void sendAuditNotification(String businessType, Integer businessId, String businessTitle,
                                    String action, Integer receiverId, Integer senderId) {
        String title = getAuditNotificationTitle(businessType, action);
        String content = String.format("%s《%s》%s，请及时处理。",
                                     getBusinessTypeName(businessType), businessTitle, action);

        sendBusinessNotification(getBusinessCategory(businessType), title, content,
                               receiverId, senderId, businessType, businessId);
    }

    // ========== 基于权限的消息通知实现 ==========

    @Autowired
    private EmployeePermissionService employeePermissionService;

    @Override
    public void sendBusinessNotificationByPermission(String businessType, Integer businessId,
            String title, String content, Integer senderId, String category) {
        sendBusinessNotificationByPermission(businessType, businessId, title, content, senderId, category, "中");
    }

    @Override
    public void sendBusinessNotificationByPermission(String businessType, Integer businessId,
            String title, String content, Integer senderId, String category, String priority) {
        // 获取有权限的接收者
        List<Integer> receiverIds = employeePermissionService.getMessageReceivers(businessType, senderId);

        if (receiverIds.isEmpty()) {
            log.warn("没有找到符合条件的消息接收者: businessType={}, senderId={}", businessType, senderId);
            return;
        }

        log.info("根据权限发送业务通知: businessType={}, receiverIds={}, title={}",
                businessType, receiverIds.size(), title);

        // 发送给每个接收者
        for (Integer receiverId : receiverIds) {
            try {
                sendBusinessNotification(category, title, content, receiverId, senderId, businessType, businessId);
            } catch (Exception e) {
                log.error("发送业务通知失败: receiverId={}, title={}", receiverId, title, e);
            }
        }
    }

    @Override
    public void sendApprovalSubmitNotification(String businessType, Integer businessId,
            String businessTitle, Integer senderId) {
        String title = getBusinessTypeName(businessType) + "提交审批通知";
        String content = String.format("您有一条%s《%s》需要审批，请及时处理。",
                getBusinessTypeName(businessType), businessTitle);

        sendBusinessNotificationByPermission(businessType, businessId, title, content, senderId, "审批通知", "高");
    }

    @Override
    public void sendApprovalRevokeNotification(String businessType, Integer businessId,
            String businessTitle, Integer senderId) {
        String title = getBusinessTypeName(businessType) + "撤回通知";
        String content = String.format("%s《%s》已被申请人撤回，请忽略此审批。",
                getBusinessTypeName(businessType), businessTitle);

        sendBusinessNotificationByPermission(businessType, businessId, title, content, senderId, "审批通知", "中");
    }

    @Override
    public void sendAuditResultNotification(String businessType, Integer businessId,
            String businessTitle, String action, Integer senderId) {
        String title = getBusinessTypeName(businessType) + action + "通知";
        String content = String.format("您提交的%s《%s》已%s，请知悉。",
                getBusinessTypeName(businessType), businessTitle, action);

        sendBusinessNotificationByPermission(businessType, businessId, title, content, senderId, "审批结果");
    }

    @Override
    public void sendBusinessOperationNotification(String businessType, Integer businessId,
            String businessTitle, String action, Integer senderId) {
        String title = getBusinessTypeName(businessType) + action + "通知";
        String content = String.format("%s《%s》已%s，请知悉。",
                getBusinessTypeName(businessType), businessTitle, action);

        sendBusinessNotificationByPermission(businessType, businessId, title, content, senderId, "业务通知");
    }

    /**
     * 获取审核通知标题
     */
    private String getAuditNotificationTitle(String businessType, String action) {
        String typeName = getBusinessTypeName(businessType);
        return typeName + action + "通知";
    }

    /**
     * 获取业务类型中文名称
     */
    private String getBusinessTypeName(String businessType) {
        switch (businessType) {
            // 合同相关
            case "CONTRACT":
            case "CONTRACT_SUBMIT":
            case "CONTRACT_REVOKE":
            case "CONTRACT_AUDIT_RESULT":
            case "CONTRACT_CREATE":
            case "CONTRACT_UPDATE":
                return "合同";
            case "CONTRACT_CHANGE":
            case "CONTRACT_CHANGE_SUBMIT":
            case "CONTRACT_CHANGE_REVOKE":
            case "CONTRACT_CHANGE_AUDIT_RESULT":
            case "CONTRACT_CHANGE_CREATE":
            case "CONTRACT_CHANGE_UPDATE":
                return "合同变更";
            case "CONTRACT_FULFILLMENT":
            case "CONTRACT_FULFILLMENT_UPDATE":
                return "合同履行";

            // 报价单
            case "QUOTATION":
            case "QUOTATION_SUBMIT":
            case "QUOTATION_AUDIT_RESULT":
            case "QUOTATION_CREATE":
            case "QUOTATION_UPDATE":
                return "报价单";

            // 收运通知
            case "PICKUP_NOTICE":
            case "PICKUP_NOTICE_SUBMIT":
            case "PICKUP_NOTICE_REVOKE":
            case "PICKUP_NOTICE_AUDIT_RESULT":
            case "PICKUP_NOTICE_CREATE":
            case "PICKUP_NOTICE_UPDATE":
                return "收运通知";

            // 开票通知
            case "INVOICE_NOTICE":
            case "INVOICE_NOTICE_SUBMIT":
            case "INVOICE_NOTICE_REVOKE":
            case "INVOICE_NOTICE_AUDIT_RESULT":
            case "INVOICE_NOTICE_CREATE":
            case "INVOICE_NOTICE_UPDATE":
                return "开票通知";

            // 运输相关
            case "DISPATCH_ORDER":
            case "DISPATCH_ORDER_CREATE":
                return "运输单";
            case "TRANSPORT_DISPATCH":
            case "TRANSPORT_DISPATCH_UPDATE":
            case "TRANSPORT_DISPATCH_WEIGHING":
                return "运输执行";
            case "WEIGHING_SLIP":
            case "WEIGHING_SLIP_CREATE":
            case "WEIGHING_SLIP_UPDATE":
                return "总磅单";

            // 仓库相关
            case "WAREHOUSING":
            case "WAREHOUSING_SUBMIT":
            case "WAREHOUSING_REVOKE":
            case "WAREHOUSING_AUDIT_RESULT":
            case "WAREHOUSING_CREATE":
            case "WAREHOUSING_UPDATE":
                return "入库单";
            case "OUTBOUND":
            case "OUTBOUND_SUBMIT":
            case "OUTBOUND_REVOKE":
            case "OUTBOUND_AUDIT_RESULT":
            case "OUTBOUND_CREATE":
            case "OUTBOUND_UPDATE":
                return "出库单";

            // 结算相关
            case "SETTLEMENT":
            case "SETTLEMENT_SUBMIT":
            case "SETTLEMENT_REVOKE":
            case "SETTLEMENT_AUDIT_RESULT":
            case "SETTLEMENT_CREATE":
            case "SETTLEMENT_UPDATE":
                return "结算单";
            case "BUSINESS_FEE":
            case "BUSINESS_FEE_CREATE":
            case "BUSINESS_FEE_UPDATE":
                return "业务费";
            case "TRANSPORT_FEE":
            case "TRANSPORT_FEE_CREATE":
            case "TRANSPORT_FEE_UPDATE":
                return "运输费";
            case "DISPOSE_FEE":
            case "DISPOSE_FEE_CREATE":
            case "DISPOSE_FEE_UPDATE":
                return "处置费";

            // 发票
            case "INVOICE":
            case "INVOICE_OUTPUT":
            case "INVOICE_OUTPUT_CREATE":
            case "INVOICE_OUTPUT_UPDATE":
                return "销项发票";
            case "INVOICE_INPUT":
            case "INVOICE_INPUT_CREATE":
            case "INVOICE_INPUT_UPDATE":
                return "进项发票";

            // 资金相关
            case "FUND_TRANSACTION":
            case "FUND_TRANSACTION_CREATE":
            case "FUND_TRANSACTION_UPDATE":
                return "资金流水";
            case "FUND_ACCOUNT":
            case "FUND_ACCOUNT_CREATE":
            case "FUND_ACCOUNT_UPDATE":
                return "资金账户";
            case "FUND_SUBJECT":
            case "FUND_SUBJECT_CREATE":
            case "FUND_SUBJECT_UPDATE":
                return "科目";
            case "FUND_PERIOD":
            case "FUND_PERIOD_INIT":
            case "FUND_PERIOD_SETTLE":
            case "FUND_PERIOD_REVERSE":
                return "账期";

            // 档案相关
            case "CUSTOMER":
            case "CUSTOMER_CREATE":
            case "CUSTOMER_UPDATE":
                return "客户";
            case "SUPPLIER":
            case "SUPPLIER_CREATE":
            case "SUPPLIER_UPDATE":
            case "SUPPLIER_DELETE":
                return "供应商";
            case "VEHICLE":
            case "VEHICLE_CREATE":
            case "VEHICLE_UPDATE":
            case "VEHICLE_DELETE":
                return "车辆";
            case "WASTE_CODE":
            case "WASTE_CODE_CREATE":
            case "WASTE_CODE_UPDATE":
                return "危废条目";
            case "WASTE_CATEGORY_CONFIG":
            case "WASTE_CATEGORY_CONFIG_UPDATE":
                return "废物类别限额";

            // 平台管理
            case "TRANSFER_MANIFEST":
            case "TRANSFER_MANIFEST_CREATE":
            case "TRANSFER_MANIFEST_UPDATE":
                return "转移联单";

            // 员工注册
            case "EMPLOYEE_REGISTRATION":
            case "EMPLOYEE_REG_SUBMIT":
            case "EMPLOYEE_REG_AUDIT_RESULT":
                return "员工注册";

            // 对账单
            case "RECONCILIATION":
                return "对账单";

            default:
                return "业务单据";
        }
    }

    /**
     * 获取业务分类
     */
    private String getBusinessCategory(String businessType) {
        // 档案管理
        if (businessType.startsWith("CUSTOMER") || businessType.startsWith("SUPPLIER")
                || businessType.startsWith("VEHICLE") || businessType.startsWith("WASTE_CODE")
                || businessType.startsWith("WASTE_CATEGORY_CONFIG")) {
            return "档案";
        }
        // 业务管理
        if (businessType.startsWith("QUOTATION") || businessType.startsWith("PICKUP_NOTICE")
                || businessType.startsWith("INVOICE_NOTICE")) {
            return "业务";
        }
        // 合同管理
        if (businessType.startsWith("CONTRACT") || businessType.startsWith("CONTRACT_")) {
            return "合同";
        }
        // 结算
        if (businessType.startsWith("SETTLEMENT") || businessType.startsWith("BUSINESS_FEE")
                || businessType.startsWith("TRANSPORT_FEE") || businessType.startsWith("DISPOSE_FEE")
                || businessType.startsWith("RECONCILIATION")) {
            return "结算";
        }
        // 运输管理
        if (businessType.startsWith("DISPATCH_ORDER") || businessType.startsWith("TRANSPORT_DISPATCH")) {
            return "运输";
        }
        // 仓库管理
        if (businessType.startsWith("WEIGHING_SLIP") || businessType.startsWith("WAREHOUSING")
                || businessType.startsWith("OUTBOUND")) {
            return "仓库";
        }
        // 财务管理
        if (businessType.startsWith("INVOICE_") || businessType.startsWith("FUND_")) {
            return "财务";
        }
        // 平台管理
        if (businessType.startsWith("TRANSFER_MANIFEST")) {
            return "平台";
        }
        // 员工注册
        if (businessType.startsWith("EMPLOYEE_REG")) {
            return "系统";
        }
        return "其他";
    }
}

































