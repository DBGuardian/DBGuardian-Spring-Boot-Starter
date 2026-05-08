package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 邮件通道配置保存请求
 *
 * @author ERP
 */
@Data
public class EmailChannelConfigSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "显示名称不能为空")
    @Size(max = 50, message = "显示名称不能超过50个字符")
    private String displayName;

    @NotBlank(message = "默认发件地址不能为空")
    @Email(message = "默认发件地址格式不正确")
    private String fromAddress;

    @Email(message = "回复地址格式不正确")
    private String replyTo;

    @NotBlank(message = "SMTP主机不能为空")
    private String smtpHost;

    @NotNull(message = "SMTP端口不能为空")
    @Min(value = 1, message = "SMTP端口最小为1")
    @Max(value = 65535, message = "SMTP端口最大为65535")
    private Integer smtpPort;

    @NotBlank(message = "认证方式不能为空")
    private String authMethod;

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 150, message = "登录账号不能超过150个字符")
    private String username;

    @Size(min = 8, max = 64, message = "授权码长度需在8-64之间")
    private String password;

    @NotBlank(message = "加密策略不能为空")
    private String encryption;

    @NotBlank(message = "启用状态不能为空")
    @Pattern(regexp = "ENABLED|DISABLED", message = "启用状态只能是ENABLED或DISABLED")
    private String status;

    @NotNull(message = "每小时最大发送量不能为空")
    @Min(value = 1, message = "每小时最大发送量至少为1")
    @Max(value = 10000, message = "每小时最大发送量不能超过10000")
    private Integer maxPerHour;

    @NotNull(message = "每日最大发送量不能为空")
    @Min(value = 1, message = "每日最大发送量至少为1")
    @Max(value = 100000, message = "每日最大发送量不能超过100000")
    private Integer maxPerDay;
}

