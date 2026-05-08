package com.erp.service.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用邮件发送请求
 *
 * @author ERP
 */
@Data
public class EmailSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 收件人列表
     */
    private List<String> toList = new ArrayList<>();

    /**
     * 邮件主题
     */
    private String subject;

    /**
     * 邮件正文
     */
    private String content;

    /**
     * 是否按HTML格式发送，默认true
     */
    private boolean html = true;
}






























