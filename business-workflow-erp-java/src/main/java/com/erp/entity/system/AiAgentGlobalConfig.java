package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 大模型全局配置实体
 *
 * 对应表：AI_AGENT_GLOBAL_CONFIG（全局仅一条记录）
 *
 * @author ERP
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("AI_AGENT_GLOBAL_CONFIG")
public class AiAgentGlobalConfig extends BaseEntity {

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
     * 每秒最大请求数（QPS 上限）
     */
    @TableField("每秒最大请求数")
    private Integer maxRequestsPerSecond;

    /**
     * 每次请求最大文本长度（字符数）
     */
    @TableField("每次请求最大文本长度")
    private Integer maxTextLengthPerRequest;

    /**
     * 每次请求最大段落数
     */
    @TableField("每次请求最大段落数")
    private Integer maxParagraphsPerRequest;

    /**
     * 单会话最大消息条数
     */
    @TableField("单会话最大消息条数")
    private Integer maxConversationMessages;

    /**
     * 单会话累计字符上限
     */
    @TableField("单会话累计字符上限")
    private Integer maxConversationChars;
}



