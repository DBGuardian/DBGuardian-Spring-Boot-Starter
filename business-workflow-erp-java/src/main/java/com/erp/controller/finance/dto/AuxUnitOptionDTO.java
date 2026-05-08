package com.erp.controller.finance.dto;

import lombok.Data;

/**
 * 辅助计量单位选项DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class AuxUnitOptionDTO {

    /**
     * 计量单位编码
     */
    private String unitCode;

    /**
     * 计量单位名称
     */
    private String unitName;

    /**
     * 计量单位类型
     */
    private String unitType;

    /**
     * 换算比例（相对于基本单位）
     */
    private Double conversionRatio;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 显示顺序
     */
    private Integer displayOrder;

    /**
     * 备注
     */
    private String remark;
}
