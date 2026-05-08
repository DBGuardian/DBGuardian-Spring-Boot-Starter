package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.system.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统通用配置表 Mapper
 *
 * 用于按 name 读取和更新配置，不作为业务外键主数据使用。
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置名称查询配置
     *
     * @param name 配置名称
     * @return 配置实体
     */
    SysConfig selectByName(String name);
}





