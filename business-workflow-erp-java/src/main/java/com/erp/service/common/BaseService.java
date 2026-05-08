package com.erp.service.common;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 通用服务接口
 * 扩展 IService，提供乐观锁检查功能
 *
 * @author ERP System
 * @date 2025-01-31
 */
public interface BaseService<T> extends IService<T> {

    /**
     * 带乐观锁检查的更新
     * 如果更新失败（返回0），抛出业务异常
     *
     * @param entity 实体对象
     * @return true 更新成功
     * @throws com.erp.common.exception.BusinessException 乐观锁冲突时抛出
     */
    boolean updateByIdWithLockCheck(T entity);

    /**
     * 带乐观锁检查的删除（根据ID）
     * 如果删除失败（返回0），抛出业务异常
     *
     * @param id 主键ID
     * @return true 删除成功
     * @throws com.erp.common.exception.BusinessException 记录不存在时抛出
     */
    boolean removeByIdWithLockCheck(Long id);
}
