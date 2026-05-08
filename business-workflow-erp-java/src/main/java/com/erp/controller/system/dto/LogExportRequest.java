package com.erp.controller.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 日志导出请求
 * 
 * @author ERP System
 * @date 2025-12-08
 */
@Data
public class LogExportRequest {

    /**
     * 导出模式：all-全部，selection-选中
     */
    private String mode;

    /**
     * 选中的日志编号列表（mode=selection时使用）
     */
    private List<Integer> logIds;

    /**
     * 关键字（操作人/模块/内容）
     */
    private String keyword;

    /**
     * 日志类型：操作日志/数据变更/登录
     */
    private String logType;

    /**
     * 状态：success/failed
     */
    private String status;

    /**
     * 操作模块
     */
    private String module;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 开始时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String startTime;

    /**
     * 结束时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String endTime;
}











































































































































