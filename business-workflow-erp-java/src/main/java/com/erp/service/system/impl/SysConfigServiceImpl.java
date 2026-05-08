package com.erp.service.system.impl;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.entity.system.SysConfig;
import com.erp.mapper.system.SysConfigMapper;
import com.erp.service.system.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统通用配置服务实现
 */
@Slf4j
@Service
public class SysConfigServiceImpl implements SysConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Override
    public SysConfig getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "配置名称不能为空");
        }
        return sysConfigMapper.selectByName(name.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfig saveOrUpdate(String name, String value, String remark, Long modifierId) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "配置名称不能为空");
        }
        if (value == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "配置内容不能为空");
        }
        String trimmedName = name.trim();

        SysConfig existing = sysConfigMapper.selectByName(trimmedName);

        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setName(trimmedName);
            config.setValue(value);
            config.setRemark(remark);
            config.setModifierId(modifierId);
            config.setUpdatedAt(java.time.LocalDateTime.now());
            sysConfigMapper.insert(config);
            log.info("新增系统配置：name={}", trimmedName);
            return config;
        } else {
            existing.setValue(value);
            existing.setRemark(remark);
            existing.setModifierId(modifierId);
            existing.setUpdatedAt(java.time.LocalDateTime.now());
            int rows = sysConfigMapper.updateById(existing);
            if (rows == 0) {
                log.warn("更新系统配置失败（乐观锁冲突），name={}", trimmedName);
            }
            log.info("更新系统配置：name={}", trimmedName);
            return existing;
        }
    }
}


