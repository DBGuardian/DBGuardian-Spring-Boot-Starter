package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 大模型智能体配置保存请求
 *
 * 用于新增或编辑单个智能体配置
 *
 * @author ERP
 */
@Data
public class AiAgentConfigSaveRequest {

    /**
     * 智能体编号，编辑时必填，新建时为空
     */
    private Integer agentId;

    /**
     * 智能体编码，系统内部唯一
     */
    @NotBlank(message = "智能体编码不能为空")
    private String agentCode;

    /**
     * 智能体名称
     */
    @NotBlank(message = "智能体名称不能为空")
    private String agentName;

    /**
     * 提供方
     */
    @NotBlank(message = "提供方不能为空")
    private String provider;

    /**
     * 接口地址
     */
    @NotBlank(message = "接口地址不能为空")
    private String baseUrl;

    /**
     * 模型名称或ID
     */
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    /**
     * API Key 明文（为空则不更新已有密文）
     */
    private String apiKey;

    /**
     * 启用状态：ENABLED / DISABLED
     */
    @NotBlank(message = "启用状态不能为空")
    private String status;

    /**
     * 是否默认智能体
     */
    @NotNull(message = "是否默认智能体不能为空")
    private Boolean defaultAgent;

    /**
     * 排序号
     */
    @NotNull(message = "排序号不能为空")
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remark;
}





