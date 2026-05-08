package com.erp.controller.settlement.dto;

import lombok.Data;

/**
 * 结算危废信息DTO
 *
 * 对应表：SETTLEMENT_WASTE_INFO
 * 用于传递危废类别、废物代码、废物名称等基础信息
 *
 * 验证规则：
 * - 所有字段都可以为空（新增/修改时）
 * - 只有填写了数据才验证数据格式
 * - 审核时需要完整的危废信息
 */
@Data
public class SettlementWasteInfoDTO {

    /**
     * 危废信息编号
     */
    private Long wasteInfoId;

    /**
     * 结算明细编号
     */
    private Long detailId;

    /**
     * 来源废物项编号（仅溯源，允许为空）
     */
    private Integer sourceWasteItemId;

    /**
     * 废物类别（如 HW08）
     */
    private String wasteCategory;

    /**
     * 废物代码（如 900-214-08）
     */
    private String wasteCode;

    /**
     * 废物名称
     */
    private String wasteName;
}
