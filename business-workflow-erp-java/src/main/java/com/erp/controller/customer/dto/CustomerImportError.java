package com.erp.controller.customer.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量导入错误信息
 */
@Data
public class CustomerImportError implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Excel中的行号（从1开始）
     */
    private int rowIndex;

    /**
     * 错误描述
     */
    private String message;
}




