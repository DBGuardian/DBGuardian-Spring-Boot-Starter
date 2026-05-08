package com.erp.service.system;

import com.erp.entity.system.SysConfig;

/**
 * 系统通用配置服务
 *
 * 封装按 name 读取/更新配置的基础能力，供业务模块与系统配置页面复用。
 */
public interface SysConfigService {

    /**
     * 根据配置名称获取配置
     *
     * @param name 配置名称
     * @return 配置实体，可能为 null
     */
    SysConfig getByName(String name);

    /**
     * 根据配置名称保存或更新配置内容
     *
     * @param name       配置名称
     * @param value      配置内容（JSON 或 文本）
     * @param remark     备注说明
     * @param modifierId 最后修改人ID
     * @return 最新配置实体
     */
    SysConfig saveOrUpdate(String name, String value, String remark, Long modifierId);
}





