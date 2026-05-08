package com.erp.controller.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日志分页响应
 * 
 * @author ERP System
 * @date 2025-12-08
 */
@Data
public class LogPageResponse {

    /**
     * 日志编号
     */
    private Integer logId;

    /**
     * 日志编号（格式化：OP20250101001）
     */
    private String code;

    /**
     * 日志类型：操作日志/数据变更/登录
     */
    private String type;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作内容摘要
     */
    private String action;

    /**
     * 操作人姓名
     */
    private String operator;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime time;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 状态：success/failed
     */
    private String status;

    /**
     * 数据表名（数据变更日志专用）
     */
    private String tableName;

    /**
     * 记录ID（数据变更日志专用）
     */
    private String recordId;

    /**
     * 操作类型（操作日志专用：新增/编辑/删除/审核/导出）
     */
    private String operationType;

    /**
     * 登录结果（登录日志专用：成功/失败）
     */
    private String result;

    /**
     * 耗时（毫秒，登录日志专用）
     */
    private Long durationMs;
}











































































































































