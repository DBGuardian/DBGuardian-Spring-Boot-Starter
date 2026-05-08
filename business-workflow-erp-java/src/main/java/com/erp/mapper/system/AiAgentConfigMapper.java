package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.system.AiAgentConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 大模型智能体配置 Mapper
 *
 * @author ERP
 */
@Mapper
public interface AiAgentConfigMapper extends BaseMapper<AiAgentConfig> {

    /**
     * 查询所有智能体配置，按排序号和主键倒序
     *
     * @return 配置列表
     */
    List<AiAgentConfig> selectAllOrderBySort();

    /**
     * 将所有记录的默认智能体标记清零
     */
    int clearDefaultFlag();

    /**
     * 设置指定记录为默认智能体
     *
     * @param agentId 智能体编号
     * @return 影响行数
     */
    int setDefaultAgent(@Param("agentId") Integer agentId);
}





