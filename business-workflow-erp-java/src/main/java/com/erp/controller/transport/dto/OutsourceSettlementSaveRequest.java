package com.erp.controller.transport.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 委外运输结算单保存/创建请求
 */
@Data
public class OutsourceSettlementSaveRequest {

    /**
     * 结算单编号（更新时必填）
     */
    private Integer settlementId;

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
     * 结算周期开始日期
     */
    private LocalDate settlementPeriodStart;

    /**
     * 结算周期结束日期
     */
    private LocalDate settlementPeriodEnd;

    /**
     * 结算方式：按趟次/按重量/按距离
     */
    private String settlementMethod;

    /**
     * 计量单位：趟/吨/公里
     */
    private String unit;

    /**
     * 结算单价（元/趟 或 元/吨 或 元/公里）
     */
    private java.math.BigDecimal settlementPrice;

    /**
     * 结算数量（结算数量 × 结算单价 = 结算金额）
     */
    private java.math.BigDecimal settlementQuantity;

    /**
     * 结算金额 = 结算数量 × 结算单价
     */
    private java.math.BigDecimal settlementAmount;

    /**
     * 结算周期行列表（用于更新结算明细）
     */
    private List<OutsourceSettlementCreateRequest.SettlementRowDTO> settlementRows;

    /**
     * 价外服务列表（归属收款单）
     */
    private List<OutsourceSettlementCreateRequest.OutOfScopeServiceDTO> outOfScopeServices;

    /**
     * 关联的总磅单列表
     */
    private List<OutsourceSettlementCreateRequest.SlipDTO> slips;

    /**
     * 备注
     */
    private String remark;
}
