package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日志实体类
 *
 * 对应表：LOG
 *
 * @author ERP System
 * @date 2025-12-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("LOG")
public class Log extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 创建时间 - 数据库表中不存在此字段，排除映射
     */
    @TableField(exist = false)
    private LocalDateTime createTime;

    /**
     * 更新时间 - 数据库表中不存在此字段，排除映射
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;

    /**
     * 日志编号
     */
    @TableId(value = "日志编号", type = IdType.AUTO)
    private Integer logId;

    /**
     * 用户编码（操作人）
     */
    @TableField("用户编码")
    private Integer userId;

    /**
     * 操作模块
     */
    @TableField("操作模块")
    private String operationModule;

    /**
     * 操作内容
     */
    @TableField("操作内容")
    private String operationContent;

    /**
     * 原始数据（JSON格式）
     */
    @TableField("原始数据")
    private String oldData;

    /**
     * 新数据（JSON格式）
     */
    @TableField("新数据")
    private String newData;

    /**
     * 操作时间
     */
    @TableField("操作时间")
    private LocalDateTime operationTime;

    /**
     * IP地址
     */
    @TableField("IP地址")
    private String ipAddress;

    /**
     * 日志类型：操作日志/数据变更/登录（扩展字段，从操作模块和操作内容推断）
     */
    @TableField(exist = false)
    private String logType;

    /**
     * 操作状态：success/failed（扩展字段，从操作内容推断）
     */
    @TableField(exist = false)
    private String status;

    /**
     * 操作人姓名（关联查询）
     */
    @TableField(exist = false)
    private String operatorName;

    /**
     * 操作人登录账号（关联查询）
     */
    @TableField(exist = false)
    private String loginAccount;

    /**
     * 错误信息（扩展字段，从操作内容推断）
     */
    @TableField(exist = false)
    private String errorMsg;

    /**
     * 数据表名（数据变更日志专用，从操作模块推断）
     */
    @TableField(exist = false)
    private String tableName;

    /**
     * 记录ID（数据变更日志专用，从操作内容推断）
     */
    @TableField(exist = false)
    private String recordId;

    /**
     * 操作类型（操作日志专用：新增/编辑/删除/审核/导出）
     */
    @TableField(exist = false)
    private String operationType;

    /**
     * 登录结果（登录日志专用：成功/失败）
     */
    @TableField(exist = false)
    private String loginResult;

    /**
     * 耗时（毫秒，登录日志专用）
     */
    @TableField(exist = false)
    private Long durationMs;
}





































































































