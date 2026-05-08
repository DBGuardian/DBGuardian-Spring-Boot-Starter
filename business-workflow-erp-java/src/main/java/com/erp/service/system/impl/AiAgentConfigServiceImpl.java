package com.erp.service.system.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.AesCryptoUtil;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.system.dto.AiAgentConfigResponse;
import com.erp.controller.system.dto.AiAgentConfigSaveRequest;
import com.erp.controller.system.dto.AiAgentGlobalConfigResponse;
import com.erp.controller.system.dto.AiAgentGlobalConfigSaveRequest;
import com.erp.entity.system.AiAgentConfig;
import com.erp.entity.system.AiAgentGlobalConfig;
import com.erp.mapper.system.AiAgentConfigMapper;
import com.erp.mapper.system.AiAgentGlobalConfigMapper;
import com.erp.service.system.AiAgentConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 大模型智能体配置服务实现
 *
 * @author ERP
 */
@Slf4j
@Service
public class AiAgentConfigServiceImpl implements AiAgentConfigService {

    @Value("${ai.agent.aes-key:}")
    private String aesKey;

    @Autowired
    private AiAgentConfigMapper aiAgentConfigMapper;

    @Autowired
    private AiAgentGlobalConfigMapper aiAgentGlobalConfigMapper;

    @Override
    public List<AiAgentConfigResponse> listAgents() {
        List<AiAgentConfig> entities = aiAgentConfigMapper.selectAllOrderBySort();
        List<AiAgentConfigResponse> result = new ArrayList<>();
        for (AiAgentConfig entity : entities) {
            AiAgentConfigResponse resp = new AiAgentConfigResponse();
            resp.setAgentId(entity.getAgentId());
            resp.setAgentCode(entity.getAgentCode());
            resp.setAgentName(entity.getAgentName());
            resp.setProvider(entity.getProvider());
            resp.setBaseUrl(entity.getBaseUrl());
            resp.setModelName(entity.getModelName());
            resp.setStatus(entity.getStatus());
            resp.setDefaultAgent(entity.getIsDefault() != null && entity.getIsDefault() == 1);
            resp.setSortOrder(entity.getSortOrder());
            resp.setRemark(entity.getRemark());
            resp.setCreateTime(entity.getCreateTime());
            resp.setUpdateTime(entity.getUpdateTime());
            result.add(resp);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAgent(AiAgentConfigSaveRequest request) {
        validateAgentRequest(request);

        AiAgentConfig entity;
        boolean isNew = request.getAgentId() == null;
        if (isNew) {
            entity = new AiAgentConfig();
        } else {
            entity = aiAgentConfigMapper.selectById(request.getAgentId());
            if (entity == null) {
                throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "智能体配置不存在");
            }
        }

        // 唯一编码校验
        QueryWrapper<AiAgentConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("智能体编码", request.getAgentCode());
        if (!isNew) {
            wrapper.ne("智能体编号", request.getAgentId());
        }
        AiAgentConfig existed = aiAgentConfigMapper.selectOne(wrapper);
        if (existed != null) {
            throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "智能体编码已存在");
        }

        Integer currentUserId = SecurityUtil.getCurrentUserId();

        entity.setAgentCode(request.getAgentCode());
        entity.setAgentName(request.getAgentName());
        entity.setProvider(request.getProvider());
        entity.setBaseUrl(request.getBaseUrl());
        entity.setModelName(request.getModelName());
        entity.setStatus(request.getStatus());
        entity.setSortOrder(request.getSortOrder());
        entity.setRemark(request.getRemark());
        if (entity.getCreateUserId() == null) {
            entity.setCreateUserId(currentUserId);
        }
        entity.setUpdateUserId(currentUserId);

        if (StrUtil.isNotBlank(request.getApiKey())) {
            if (StrUtil.isBlank(aesKey)) {
                throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "未配置AI加密密钥，请联系管理员");
            }
            entity.setApiKeyCipher(AesCryptoUtil.encrypt(request.getApiKey(), aesKey));
        } else if (entity.getAgentId() == null && StrUtil.isBlank(entity.getApiKeyCipher())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "新建智能体时必须配置API Key");
        }

        if (isNew) {
            aiAgentConfigMapper.insert(entity);
        } else {
            int rows = aiAgentConfigMapper.updateById(entity);
            if (rows == 0) {
                throw new BusinessException("更新智能体配置失败：记录已被其他用户修改");
            }
        }

        // 处理默认智能体标记
        if (Boolean.TRUE.equals(request.getDefaultAgent())) {
            aiAgentConfigMapper.clearDefaultFlag();
            aiAgentConfigMapper.setDefaultAgent(entity.getAgentId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(Integer agentId) {
        if (agentId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "智能体编号不能为空");
        }
        AiAgentConfig entity = aiAgentConfigMapper.selectById(agentId);
        if (entity == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "智能体配置不存在");
        }
        int rows = aiAgentConfigMapper.deleteById(agentId);
        if (rows == 0) {
            throw new BusinessException("删除智能体配置失败：记录不存在或已被删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAgent(Integer agentId) {
        if (agentId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "智能体编号不能为空");
        }
        AiAgentConfig entity = aiAgentConfigMapper.selectById(agentId);
        if (entity == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "智能体配置不存在");
        }
        aiAgentConfigMapper.clearDefaultFlag();
        aiAgentConfigMapper.setDefaultAgent(agentId);
    }

    @Override
    public AiAgentGlobalConfigResponse getGlobalConfig() {
        AiAgentGlobalConfig entity = aiAgentGlobalConfigMapper.selectSingleton();
        AiAgentGlobalConfigResponse resp = new AiAgentGlobalConfigResponse();
        if (entity == null) {
            // 如果尚未初始化，返回默认值（与前端默认值保持一致）
            resp.setMaxRequestsPerSecond(5);
            resp.setMaxTextLengthPerRequest(1200);
            resp.setMaxParagraphsPerRequest(4);
            resp.setMaxConversationMessages(20);
            resp.setMaxConversationChars(6000);
            return resp;
        }
        resp.setMaxRequestsPerSecond(entity.getMaxRequestsPerSecond());
        resp.setMaxTextLengthPerRequest(entity.getMaxTextLengthPerRequest());
        resp.setMaxParagraphsPerRequest(entity.getMaxParagraphsPerRequest());
        resp.setMaxConversationMessages(entity.getMaxConversationMessages());
        resp.setMaxConversationChars(entity.getMaxConversationChars());
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGlobalConfig(AiAgentGlobalConfigSaveRequest request) {
        AiAgentGlobalConfig existed = aiAgentGlobalConfigMapper.selectSingleton();
        if (existed == null) {
            AiAgentGlobalConfig entity = new AiAgentGlobalConfig();
            entity.setMaxRequestsPerSecond(request.getMaxRequestsPerSecond());
            entity.setMaxTextLengthPerRequest(request.getMaxTextLengthPerRequest());
            entity.setMaxParagraphsPerRequest(request.getMaxParagraphsPerRequest());
            entity.setMaxConversationMessages(request.getMaxConversationMessages());
            entity.setMaxConversationChars(request.getMaxConversationChars());
            aiAgentGlobalConfigMapper.insert(entity);
        } else {
            existed.setMaxRequestsPerSecond(request.getMaxRequestsPerSecond());
            existed.setMaxTextLengthPerRequest(request.getMaxTextLengthPerRequest());
            existed.setMaxParagraphsPerRequest(request.getMaxParagraphsPerRequest());
            existed.setMaxConversationMessages(request.getMaxConversationMessages());
            existed.setMaxConversationChars(request.getMaxConversationChars());
            aiAgentGlobalConfigMapper.updateSingleton(existed);
        }
    }

    private void validateAgentRequest(AiAgentConfigSaveRequest request) {
        if (!"ENABLED".equals(request.getStatus()) && !"DISABLED".equals(request.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "启用状态不合法");
        }
    }
}


