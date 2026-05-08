package com.erp.controller.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日志详情响应
 * 
 * @author ERP System
 * @date 2025-12-08
 */
@Data
public class LogDetailResponse {

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
     * 数据表名（数据变更日志专用）
     */
    private String tableName;

    /**
     * 操作内容
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
     * 错误信息
     */
    private String errorMsg;

    /**
     * 原始数据（JSON格式，解析为Map）
     */
    private Map<String, Object> oldData;

    /**
     * 新数据（JSON格式，解析为Map）
     */
    private Map<String, Object> newData;

    /**
     * 字段差异列表（用于前端展示）
     */
    private java.util.List<FieldDiff> fieldDiffs;

    /**
     * 字段差异
     */
    @Data
    public static class FieldDiff {
        /**
         * 字段名
         */
        private String key;

        /**
         * 旧值
         */
        private Object oldVal;

        /**
         * 新值
         */
        private Object newVal;

        /**
         * 变化状态：added/removed/changed/same
         */
        private String status;
    }
}

