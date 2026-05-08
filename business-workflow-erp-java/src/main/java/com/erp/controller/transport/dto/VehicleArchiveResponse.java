package com.erp.controller.transport.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 车辆档案响应（包含统计信息）
 */
@Data
public class VehicleArchiveResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 统计数据
     */
    private List<TransportStat> stats;

    /**
     * 车辆列表
     */
    private List<VehiclePageResponse> records;

    /**
     * 总数
     */
    private Long total;

    /**
     * 当前页
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;
}

