package com.erp.config;

import java.util.*;

/**
 * 消息通知权限映射配置
 * 定义每种业务消息对应的权限编码，用于确定消息的接收者范围
 *
 * @author ERP System
 * @date 2025-04-30
 */
public class MessagePermissionMapping {

    /**
     * 消息业务类型与权限编码的映射关系
     * Key: 消息业务类型 (MESSAGE_BUSINESS_TYPE)
     * Value: 权限编码列表 (拥有任一权限的员工都能收到消息)
     */
    public static final Map<String, List<String>> BUSINESS_PERMISSION_MAPPING = new LinkedHashMap<>();

    /**
     * 管理员角色编码列表
     * 拥有这些角色的员工会自动接收所有业务消息
     */
    public static final Set<String> ADMIN_ROLE_CODES = new HashSet<>(Arrays.asList(
            "super_admin",      // 超级管理员
            "admin",           // 管理员
            "system_admin"     // 系统管理员
    ));

    static {
        // ========== 审批类消息通知（发送给OA审批人员）==========

        // 提交审批 - 发送给OA审批人员
        BUSINESS_PERMISSION_MAPPING.put("QUOTATION_SUBMIT",    // 报价单提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("QUOTATION_REVOKE",     // 报价单撤回
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("PICKUP_NOTICE_SUBMIT",  // 收运通知提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("PICKUP_NOTICE_REVOKE",  // 收运通知撤回
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_NOTICE_SUBMIT", // 开票通知提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_NOTICE_REVOKE", // 开票通知撤回
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_SUBMIT",       // 合同提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_REVOKE",        // 合同撤回
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_CHANGE_SUBMIT", // 合同变更提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_CHANGE_REVOKE", // 合同变更撤回
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("SETTLEMENT_SUBMIT",     // 结算单提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("SETTLEMENT_REVOKE",     // 结算单撤回
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WAREHOUSING_SUBMIT",    // 入库单提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WAREHOUSING_REVOKE",    // 入库单撤回
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("OUTBOUND_SUBMIT",       // 出库单提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("OUTBOUND_REVOKE",       // 出库单撤回
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("EMPLOYEE_REG_SUBMIT",   // 员工注册提交审批
                Arrays.asList("行政管理:OA审批:页面"));

        // ========== 审核结果消息通知（发送给各模块页面权限人员）==========

        // 审核通过/驳回 - 发送给各模块页面权限人员
        BUSINESS_PERMISSION_MAPPING.put("QUOTATION_AUDIT_RESULT",  // 报价单审核结果
                Arrays.asList("业务管理:客户报价:页面"));
        BUSINESS_PERMISSION_MAPPING.put("PICKUP_NOTICE_AUDIT_RESULT", // 收运通知审核结果
                Arrays.asList("业务管理:收运通知:页面"));
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_NOTICE_AUDIT_RESULT", // 开票通知审核结果
                Arrays.asList("业务管理:开票通知:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_AUDIT_RESULT",     // 合同审核结果
                Arrays.asList("合同管理:危险废物合同:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_CHANGE_AUDIT_RESULT", // 合同变更审核结果
                Arrays.asList("合同管理:合同变更:页面"));
        BUSINESS_PERMISSION_MAPPING.put("SETTLEMENT_AUDIT_RESULT",  // 结算单审核结果
                Arrays.asList("合同结算:危险废物结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WAREHOUSING_AUDIT_RESULT", // 入库单审核结果
                Arrays.asList("仓库管理:入库-入库单:页面"));
        BUSINESS_PERMISSION_MAPPING.put("OUTBOUND_AUDIT_RESULT",    // 出库单审核结果
                Arrays.asList("仓库管理:出库:页面"));
        BUSINESS_PERMISSION_MAPPING.put("EMPLOYEE_REG_AUDIT_RESULT", // 员工注册审核结果
                Arrays.asList("系统管理:注册管理:页面"));

        // ========== 新增/修改操作消息通知（发送给当前业务页面权限人员）==========

        // 档案管理
        BUSINESS_PERMISSION_MAPPING.put("CUSTOMER_CREATE",   // 新增客户
                Arrays.asList("档案管理:客户档案:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CUSTOMER_UPDATE",   // 更新客户
                Arrays.asList("档案管理:客户档案:页面"));
        BUSINESS_PERMISSION_MAPPING.put("SUPPLIER_CREATE",   // 新增供应商
                Arrays.asList("档案管理:供应商档案:页面"));
        BUSINESS_PERMISSION_MAPPING.put("SUPPLIER_UPDATE",   // 更新供应商
                Arrays.asList("档案管理:供应商档案:页面"));
        BUSINESS_PERMISSION_MAPPING.put("SUPPLIER_DELETE",   // 删除供应商
                Arrays.asList("档案管理:供应商档案:页面"));
        BUSINESS_PERMISSION_MAPPING.put("VEHICLE_CREATE",   // 新增车辆
                Arrays.asList("档案管理:车辆档案:页面"));
        BUSINESS_PERMISSION_MAPPING.put("VEHICLE_UPDATE",   // 更新车辆
                Arrays.asList("档案管理:车辆档案:页面"));
        BUSINESS_PERMISSION_MAPPING.put("VEHICLE_DELETE",   // 删除车辆
                Arrays.asList("档案管理:车辆档案:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WASTE_CODE_CREATE", // 新增危废条目
                Arrays.asList("档案管理:危废条目表:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WASTE_CODE_UPDATE", // 更新危废条目
                Arrays.asList("档案管理:危废条目表:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WASTE_CATEGORY_CONFIG_UPDATE", // 修改废物类别限额
                Arrays.asList("档案管理:废物类别限额:页面"));

        // 业务管理
        BUSINESS_PERMISSION_MAPPING.put("QUOTATION_CREATE",     // 新增报价单
                Arrays.asList("业务管理:客户报价:页面"));
        BUSINESS_PERMISSION_MAPPING.put("QUOTATION_UPDATE",     // 更新报价单
                Arrays.asList("业务管理:客户报价:页面"));
        BUSINESS_PERMISSION_MAPPING.put("PICKUP_NOTICE_CREATE", // 新增收运通知
                Arrays.asList("业务管理:收运通知:页面"));
        BUSINESS_PERMISSION_MAPPING.put("PICKUP_NOTICE_UPDATE", // 更新收运通知
                Arrays.asList("业务管理:收运通知:页面"));

        // 开票通知
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_NOTICE_CREATE", // 新增开票通知
                Arrays.asList("业务管理:开票通知:页面"));
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_NOTICE_UPDATE", // 更新开票通知
                Arrays.asList("业务管理:开票通知:页面"));

        // 合同管理
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_CREATE",        // 新增合同
                Arrays.asList("合同管理:危险废物合同:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_UPDATE",        // 更新合同
                Arrays.asList("合同管理:危险废物合同:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_CHANGE_CREATE",  // 新增合同变更
                Arrays.asList("合同管理:合同变更:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_CHANGE_UPDATE",  // 更新合同变更
                Arrays.asList("合同管理:合同变更:页面"));
        BUSINESS_PERMISSION_MAPPING.put("CONTRACT_FULFILLMENT_UPDATE", // 合同履行跟踪
                Arrays.asList("合同管理:合同履行:页面"));

        // 合同结算
        BUSINESS_PERMISSION_MAPPING.put("SETTLEMENT_CREATE",   // 新增结算单
                Arrays.asList("合同结算:危险废物结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("SETTLEMENT_UPDATE",   // 更新结算单
                Arrays.asList("合同结算:危险废物结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("BUSINESS_FEE_CREATE", // 新增业务费
                Arrays.asList("合同结算:业务费结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("BUSINESS_FEE_UPDATE", // 更新业务费
                Arrays.asList("合同结算:业务费结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("TRANSPORT_FEE_CREATE", // 新增运输费
                Arrays.asList("合同结算:运输费结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("TRANSPORT_FEE_UPDATE", // 更新运输费
                Arrays.asList("合同结算:运输费结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("DISPOSE_FEE_CREATE",   // 新增处置费
                Arrays.asList("合同结算:处置费结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("DISPOSE_FEE_UPDATE",   // 更新处置费
                Arrays.asList("合同结算:处置费结算:页面"));

        // 运输管理
        BUSINESS_PERMISSION_MAPPING.put("DISPATCH_ORDER_CREATE", // 生成运输单
                Arrays.asList("运输管理:车辆安排:页面"));
        BUSINESS_PERMISSION_MAPPING.put("TRANSPORT_DISPATCH_UPDATE", // 更新运输执行
                Arrays.asList("运输管理:运输执行:页面"));
        BUSINESS_PERMISSION_MAPPING.put("TRANSPORT_DISPATCH_WEIGHING", // 关联总磅单
                Arrays.asList("运输管理:运输执行:页面"));

        // 仓库管理
        BUSINESS_PERMISSION_MAPPING.put("WEIGHING_SLIP_CREATE", // 创建总磅单
                Arrays.asList("仓库管理:入库-总磅单:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WEIGHING_SLIP_UPDATE", // 更新总磅单
                Arrays.asList("仓库管理:入库-总磅单:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WAREHOUSING_CREATE",   // 新增入库单
                Arrays.asList("仓库管理:入库-入库单:页面"));
        BUSINESS_PERMISSION_MAPPING.put("WAREHOUSING_UPDATE",   // 更新入库单
                Arrays.asList("仓库管理:入库-入库单:页面"));
        BUSINESS_PERMISSION_MAPPING.put("OUTBOUND_CREATE",      // 新增出库单
                Arrays.asList("仓库管理:出库:页面"));
        BUSINESS_PERMISSION_MAPPING.put("OUTBOUND_UPDATE",      // 更新出库单
                Arrays.asList("仓库管理:出库:页面"));

        // 财务管理
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_OUTPUT_CREATE",   // 新增销项发票
                Arrays.asList("财务管理:发票管理:销项发票:页面"));
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_OUTPUT_UPDATE",   // 更新销项发票
                Arrays.asList("财务管理:发票管理:销项发票:页面"));
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_INPUT_CREATE",    // 新增进项发票
                Arrays.asList("财务管理:发票管理:进项发票:页面"));
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_INPUT_UPDATE",    // 更新进项发票
                Arrays.asList("财务管理:发票管理:进项发票:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_TRANSACTION_CREATE", // 新增资金流水
                Arrays.asList("财务管理:资金管理:日记账:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_TRANSACTION_UPDATE", // 更新资金流水
                Arrays.asList("财务管理:资金管理:日记账:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_ACCOUNT_CREATE",     // 新增资金账户
                Arrays.asList("财务管理:账户设置:账户管理:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_ACCOUNT_UPDATE",     // 更新资金账户
                Arrays.asList("财务管理:账户设置:账户管理:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_ACCOUNT_DELETE",     // 删除资金账户
                Arrays.asList("财务管理:账户设置:账户管理:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_SUBJECT_CREATE",     // 新增科目
                Arrays.asList("财务管理:账户设置:科目管理:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_SUBJECT_UPDATE",     // 更新科目
                Arrays.asList("财务管理:账户设置:科目管理:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_SUBJECT_DELETE",     // 删除科目
                Arrays.asList("财务管理:账户设置:科目管理:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_PERIOD_INIT",        // 初始化账期
                Arrays.asList("财务管理:账户设置:账期管理:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_PERIOD_SETTLE",      // 结账
                Arrays.asList("财务管理:账户设置:账期管理:页面"));
        BUSINESS_PERMISSION_MAPPING.put("FUND_PERIOD_REVERSE",      // 反结账
                Arrays.asList("财务管理:账户设置:账期管理:页面"));

        // 业务费结算
        BUSINESS_PERMISSION_MAPPING.put("BUSINESS_FEE_CREATE",     // 新增业务费
                Arrays.asList("合同结算:业务费结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("BUSINESS_FEE_UPDATE",     // 更新业务费
                Arrays.asList("合同结算:业务费结算:页面"));
        BUSINESS_PERMISSION_MAPPING.put("BUSINESS_FEE_SUBMIT",     // 业务费提交审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("BUSINESS_FEE_REVOKE",     // 业务费撤回审批
                Arrays.asList("行政管理:OA审批:页面"));
        BUSINESS_PERMISSION_MAPPING.put("BUSINESS_FEE_AUDIT_RESULT", // 业务费审核结果
                Arrays.asList("合同结算:业务费结算:页面"));

        // 发票管理
        BUSINESS_PERMISSION_MAPPING.put("INVOICE_FILE_SUPPLEMENT", // 发票文件补充
                Arrays.asList("财务管理:发票管理:销项发票:页面", "财务管理:发票管理:进项发票:页面"));

        // 平台管理
        BUSINESS_PERMISSION_MAPPING.put("TRANSFER_MANIFEST_CREATE", // 新增转移联单
                Arrays.asList("平台管理:转移联单:页面"));
        BUSINESS_PERMISSION_MAPPING.put("TRANSFER_MANIFEST_UPDATE", // 更新转移联单
                Arrays.asList("平台管理:转移联单:页面"));
    }

    /**
     * 获取业务类型对应的权限编码列表
     *
     * @param businessType 业务类型
     * @return 权限编码列表，如果不存在则返回空列表
     */
    public static List<String> getPermissionCodes(String businessType) {
        List<String> codes = BUSINESS_PERMISSION_MAPPING.get(businessType);
        return codes != null ? codes : Collections.emptyList();
    }

    /**
     * 判断是否为管理员角色
     *
     * @param roleCode 角色编码
     * @return 是否为管理员角色
     */
    public static boolean isAdminRole(String roleCode) {
        return ADMIN_ROLE_CODES.contains(roleCode);
    }
}
