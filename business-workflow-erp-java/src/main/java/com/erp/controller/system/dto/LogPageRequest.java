package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 日志分页查询请求
 * 
 * @author ERP System
 * @date 2025-12-08
 */
@Data
public class LogPageRequest {

    /**
     * 页码
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer current;

    /**
     * 每页数量
     */
    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer size;

    /**
     * 关键字（操作人/模块/内容）
     */
    private String keyword;

    /**
     * 日志类型：操作日志/数据变更/登录（全部日志查询使用）
     */
    private String logType;

    /**
     * 状态：success/failed（全部日志查询使用）
     */
    private String status;

    /**
     * 操作模块（全部日志查询使用）
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

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向：asc/desc
     */
    private String sortOrder;

    // ========== 操作日志专用字段 ==========

    /**
     * 操作类型列表（新增/编辑/删除/审核/导出）
     */
    private List<String> operationTypes;

    // ========== 数据变更日志专用字段 ==========

    /**
     * 数据表名
     */
    private String tableName;

    /**
     * 记录ID
     */
    private String recordId;

    /**
     * 变更类型列表（新增/更新/删除）
     */
    private List<String> changeTypes;

    // ========== 登录日志专用字段 ==========

    /**
     * 登录结果：成功/失败
     */
    private String loginResult;
}











































































































































