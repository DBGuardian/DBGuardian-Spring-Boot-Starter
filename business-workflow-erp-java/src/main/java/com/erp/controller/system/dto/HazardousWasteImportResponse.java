package com.erp.controller.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 危险废物名录导入结果
 */
@Data
public class HazardousWasteImportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 处理总行数
     */
    private Integer totalCount = 0;

    /**
     * 成功数量
     */
    private Integer successCount = 0;

    /**
     * 失败数量
     */
    private Integer failCount = 0;

    /**
     * 错误详情
     */
    private List<HazardousWasteImportError> errors = new ArrayList<>();
}


