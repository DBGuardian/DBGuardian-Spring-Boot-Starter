package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮箱通道配置实体
 *
 * @author ERP
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("EMAIL_CHANNEL_CONFIG")
public class EmailChannelConfig extends BaseEntity {

    /**
     * 配置编号
     */
    @TableId(value = "配置编号", type = IdType.AUTO)
    private Integer configId;

    /**
     * 显示名称
     */
    @TableField("显示名称")
    private String displayName;

    /**
     * 默认发件地址
     */
    @TableField("默认发件地址")
    private String fromAddress;

    /**
     * 回复地址
     */
    @TableField("回复地址")
    private String replyTo;

    /**
     * SMTP Host
     */
    @TableField("SMTP_HOST")
    private String smtpHost;

    /**
     * SMTP端口
     */
    @TableField("SMTP_PORT")
    private Integer smtpPort;

    /**
     * 认证方式
     */
    @TableField("认证方式")
    private String authMethod;

    /**
     * 登录账号
     */
    @TableField("登录账号")
    private String username;

    /**
     * 授权码密文（AES256）
     */
    @TableField("授权码密文")
    private String passwordCipher;

    /**
     * 加密策略
     */
    @TableField("加密策略")
    private String encryption;

    /**
     * 启用状态
     */
    @TableField("启用状态")
    private String status;

    /**
     * 每小时最大发送量
     */
    @TableField("每小时最大发送量")
    private Integer maxPerHour;

    /**
     * 每日最大发送量
     */
    @TableField("每日最大发送量")
    private Integer maxPerDay;

    /**
     * 更新人编码
     */
    @TableField("更新人编码")
    private Integer updatedBy;

    /**
     * 最后自检时间
     */
    @TableField("最后自检时间")
    private LocalDateTime lastSelfTestTime;
}

