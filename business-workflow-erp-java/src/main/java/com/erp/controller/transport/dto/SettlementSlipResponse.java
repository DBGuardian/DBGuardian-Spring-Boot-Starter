package com.erp.controller.transport.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算单关联总磅单响应（每条记录代表一趟）
 */
@Data
public class SettlementSlipResponse {

    /**
     * 总磅单编号
     */
    private Integer slipId;

    /**
     * 总磅单号
     */
    private String slipCode;

    /**
     * 总磅单日期
     */
    private LocalDate slipDate;

    /**
     * 车牌号
     */
    private String licensePlate;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 实际净重（吨）
     */
    private BigDecimal netWeight;

    /**
     * 本趟包含的派车单号列表
     */
    private List<String> dispatchCodes;

    /**
     * 本趟包含的派车单号字符串（数据库GROUP_CONCAT结果，用于Service层转换）
     */
    private String dispatchCodesStr;

    /**
     * 完成时间（过磅时间）
     */
    private LocalDateTime completionTime;

    /**
     * 关联合同号
     */
    private String contractCode;

    /**
     * 已关联的结算单号（如果有）
     */
    private String relatedSettlementNo;

    /**
     * 是否已关联结算单
     */
    private Boolean isSettled;
}
