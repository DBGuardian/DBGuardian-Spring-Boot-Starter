package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 危险废物名录导入错误信息
 */
@Data
public class HazardousWasteImportError implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Excel行号
     */
    private Integer rowIndex;

    /**
     * 废物代码
     */
    private String wasteCode;

    /**
     * 错误信息
     */
    private String message;
}


