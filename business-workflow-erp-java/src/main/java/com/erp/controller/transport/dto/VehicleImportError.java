package com.erp.controller.transport.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 车辆导入错误信息
 */
@Data
public class VehicleImportError implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行号（从1开始，包含表头）
     */
    private int rowIndex;

    /**
     * 错误信息
     */
    private String message;
}

