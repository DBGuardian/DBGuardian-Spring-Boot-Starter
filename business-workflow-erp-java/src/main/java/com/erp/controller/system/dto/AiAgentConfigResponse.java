package com.erp.controller.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大模型智能体配置查询响应
 *
 * @author ERP
 */
@Data
public class AiAgentConfigResponse {

    private Integer agentId;

    private String agentCode;

    private String agentName;

    private String provider;

    private String baseUrl;

    private String modelName;

    /**
     * 是否启用：ENABLED / DISABLED
     */
    private String status;

    /**
     * 是否默认智能体：true-是，false-否
     */
    private Boolean defaultAgent;

    private Integer sortOrder;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}





