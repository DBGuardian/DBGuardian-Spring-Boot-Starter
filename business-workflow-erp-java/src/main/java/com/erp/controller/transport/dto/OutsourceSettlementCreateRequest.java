package com.erp.controller.transport.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 委外运输结算单新增请求DTO（支持收付款拆分）
 *
 * 变更说明（2026-05-01）：
 *   - 参考业务费结算的新增接口模式
 *   - 支持收付款混合明细自动拆分
 *   - 空数据允许，只验证数据库必填字段
 *   - 价外服务归属收款
 */
@Data
public class OutsourceSettlementCreateRequest {

    /**
     * 关联的运输合同编号（可为空，表示游离数据）
     */
    private Integer contractId;

    /**
     * 关联的运输合同单号
     */
    private String contractNo;

    /**
     * 承运方名称
     */
    private String carrierName;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 开户银行
     */
    private String bankName;

    /**
     * 银行卡号
     */
    private String cardNumber;

    /**
     * 银行账户名称
     */
    private String accountName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 版本号（用于乐观锁）
     */
    private Integer version;

    /**
     * 结算周期行列表
     * 包含结算周期、结算方式、结算数量、单价、金额等
     * 如果包含收付款两个方向，后端自动拆分生成两个结算单
     */
    private List<SettlementRowDTO> settlementRows;

    /**
     * 价外服务列表（归属收款单）
     */
    private List<OutOfScopeServiceDTO> outOfScopeServices;

    /**
     * 关联的总磅单列表
     * 用于创建结算单时关联总磅单
     */
    private List<SlipDTO> slips;

    @Data
    public static class SlipDTO {
        /**
         * 总磅单编号
         */
        private Integer slipId;

        /**
         * 总磅单单号
         */
        private String slipCode;
    }

    @Data
    public static class SettlementRowDTO {
        /**
         * 明细编号（更新时必填，新增时为空）
         */
        private Integer rowId;

        /**
         * 收付款方向：RECEIVABLE（收款）/ PAYABLE（付款）
         */
        private String settlementType;

        /**
         * 结算周期开始日期
         */
        private LocalDate settlementPeriodStart;

        /**
         * 结算周期结束日期
         */
        private LocalDate settlementPeriodEnd;

        /**
         * 结算方式：按趟次结算/按重量结算/按距离结算
         */
        private String settlementMethod;

        /**
         * 计量单位：趟/吨/公里
         */
        private String unit;

        /**
         * 结算数量
         */
        private BigDecimal settlementQuantity;

        /**
         * 结算单价
         */
        private BigDecimal settlementPrice;

        /**
         * 结算金额
         */
        private BigDecimal settlementAmount;
    }

    @Data
    public static class OutOfScopeServiceDTO {
        /**
         * 价外服务编号（更新时必填，新增时为空）
         */
        private Integer outOfScopeServiceId;

        /**
         * 项目名称
         */
        private String project;

        /**
         * 规格型号
         */
        private String spec;

        /**
         * 单位
         */
        private String basicUnit;

        /**
         * 计划数量
         */
        private BigDecimal plannedQuantity;

        /**
         * 合同单价
         */
        private BigDecimal contractUnitPrice;

        /**
         * 结算数量
         */
        private BigDecimal settlementQuantity;

        /**
         * 结算单价
         */
        private BigDecimal unitPrice;

        /**
         * 结算金额
         */
        private BigDecimal amount;

        /**
         * 备注
         */
        private String remark;
    }
}
