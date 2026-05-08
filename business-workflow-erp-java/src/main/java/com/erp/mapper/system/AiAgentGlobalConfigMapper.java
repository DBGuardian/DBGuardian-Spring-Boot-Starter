package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.system.AiAgentGlobalConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 大模型全局配置 Mapper
 *
 * AI_AGENT_GLOBAL_CONFIG 表为全局单行配置
 *
 * @author ERP
 */
@Mapper
public interface AiAgentGlobalConfigMapper extends BaseMapper<AiAgentGlobalConfig> {

    /**
     * 查询全局唯一配置
     *
     * @return 全局配置，可能为 null
     */
    AiAgentGlobalConfig selectSingleton();

    /**
     * 更新全局唯一配置（不依赖主键，直接更新全表）
     *
     * @param entity 配置实体
     * @return 影响行数
     */
    int updateSingleton(AiAgentGlobalConfig entity);
}



