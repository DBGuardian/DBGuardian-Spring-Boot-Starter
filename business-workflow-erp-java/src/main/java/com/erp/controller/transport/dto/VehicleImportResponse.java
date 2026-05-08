package com.erp.controller.transport.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 车辆导入结果
 */
@Data
public class VehicleImportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总行数
     */
    private int totalCount;

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failCount;

    /**
     * 错误明细
     */
    private List<VehicleImportError> errors = new ArrayList<>();
}

