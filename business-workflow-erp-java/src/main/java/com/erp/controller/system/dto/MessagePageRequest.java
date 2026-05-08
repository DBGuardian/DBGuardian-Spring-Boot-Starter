package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 消息分页查询请求
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Data
public class MessagePageRequest {

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
     * 消息类型：全部/预警消息/业务通知/系统消息
     */
    private String messageType;

    /**
     * 消息状态：全部/已读/未读
     */
    private String messageStatus;

    /**
     * 消息优先级：全部/紧急/高/中/低
     */
    private String messagePriority;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 关键词搜索（标题、内容）
     */
    private String keyword;
}


































