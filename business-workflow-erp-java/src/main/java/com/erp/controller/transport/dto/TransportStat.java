package com.erp.controller.transport.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 运输统计数据
 */
@Data
public class TransportStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label;
    private String value;
    private String color;
    private String trend;
}

