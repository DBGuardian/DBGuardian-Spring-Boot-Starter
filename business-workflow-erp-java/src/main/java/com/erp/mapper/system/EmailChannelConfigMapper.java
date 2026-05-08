package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.system.EmailChannelConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件通道配置Mapper
 *
 * @author ERP
 */
@Mapper
public interface EmailChannelConfigMapper extends BaseMapper<EmailChannelConfig> {

    /**
     * 查询最新的邮箱配置
     *
     * @return 配置信息
     */
    EmailChannelConfig selectLatest();
}

