package com.erp.controller.transport.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 委外运输结算单分页查询请求
 */
@Data
public class OutsourceSettlementPageRequest {

    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 每页数量
     */
    private Integer size = 10;

    /**
     * 合同编号（可选）
     */
    private Integer contractId;

    /**
     * 合同单号（可选）
     */
    private String contractNo;

    /**
     * 结算单编号（可选，模糊查询）
     */
    private String settlementNo;

    /**
     * 承运方名称（可选，模糊查询）
     */
    private String carrierName;

    /**
     * 状态（可选）
     */
    private String status;

    /**
     * 开始日期（可选）
     */
    private LocalDate startDate;

    /**
     * 结束日期（可选）
     */
    private LocalDate endDate;

    /**
     * 是否查询游离数据（合同编号为空）
     */
    private Boolean isOrphan = false;

    /**
     * 结算方向：RECEIVABLE-收款，PAYABLE-付款
     */
    private String settlementDirection;

    /**
     * 字段权限页面编码（用于后端数据范围过滤）
     */
    private String fieldPermissionPageCode;
}
