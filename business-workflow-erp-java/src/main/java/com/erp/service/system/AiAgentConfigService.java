package com.erp.service.system;

import com.erp.controller.system.dto.AiAgentConfigResponse;
import com.erp.controller.system.dto.AiAgentConfigSaveRequest;
import com.erp.controller.system.dto.AiAgentGlobalConfigResponse;
import com.erp.controller.system.dto.AiAgentGlobalConfigSaveRequest;

import java.util.List;

/**
 * 大模型智能体配置服务
 *
 * 负责智能体列表配置和全局限流配置的读写
 *
 * @author ERP
 */
public interface AiAgentConfigService {

    /**
     * 查询所有智能体配置列表
     *
     * @return 智能体配置列表
     */
    List<AiAgentConfigResponse> listAgents();

    /**
     * 保存或更新智能体配置
     *
     * @param request 保存请求
     */
    void saveAgent(AiAgentConfigSaveRequest request);

    /**
     * 删除指定智能体配置
     *
     * @param agentId 智能体编号
     */
    void deleteAgent(Integer agentId);

    /**
     * 设置指定智能体为默认智能体
     *
     * @param agentId 智能体编号
     */
    void setDefaultAgent(Integer agentId);

    /**
     * 获取全局限流配置
     *
     * @return 全局配置
     */
    AiAgentGlobalConfigResponse getGlobalConfig();

    /**
     * 保存全局限流配置（表为单行配置）
     *
     * @param request 保存请求
     */
    void saveGlobalConfig(AiAgentGlobalConfigSaveRequest request);
}





