package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 邮件通道自检请求
 *
 * @author ERP
 */
@Data
public class EmailChannelTestSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标邮箱不能为空")
    @Email(message = "目标邮箱格式不正确")
    private String targetEmail;

    @NotBlank(message = "邮件主题不能为空")
    @Size(max = 80, message = "邮件主题不能超过80个字符")
    private String subject;

    @NotBlank(message = "邮件内容不能为空")
    @Size(max = 500, message = "邮件内容不能超过500个字符")
    private String message;
}






























