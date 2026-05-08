package com.erp.controller.production.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 转移联单 Excel 批量导入响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportTransferManifestResponse {

    /**
     * 解析到的总联单数
     */
    private Integer total;

    /**
     * 成功导入的联单数
     */
    private Integer success;

    /**
     * 导入失败的联单数
     */
    private Integer error;

    /**
     * 错误详情列表（每项格式："第X行：原因"）
     */
    private List<String> errorMessages;
}
