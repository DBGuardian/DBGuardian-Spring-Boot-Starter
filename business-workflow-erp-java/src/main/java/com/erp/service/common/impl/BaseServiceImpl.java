package com.erp.service.common.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.entity.common.BaseEntity;
import com.erp.service.common.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用服务实现基类
 * 提供乐观锁返回值自动检查功能
 *
 * @author ERP System
 * @date 2025-01-31
 * @param <M> Mapper类型
 * @param <T> 实体类型
 */
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T extends BaseEntity>
        extends ServiceImpl<M, T> implements BaseService<T> {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean updateByIdWithLockCheck(T entity) {
        int rows = this.baseMapper.updateById(entity);
        if (rows == 0) {
            String entityName = entity.getClass().getSimpleName();
            log.warn("乐观锁冲突：更新{}失败，记录已被其他事务修改", entityName);
            throw new BusinessException(
                ResultCodeEnum.ERROR.getCode(),
                "更新失败：记录已被其他用户修改，请刷新后重试"
            );
        }
        return true;
    }

    @Override
    public boolean removeByIdWithLockCheck(Long id) {
        int rows = this.baseMapper.deleteById(id);
        if (rows == 0) {
            log.warn("乐观锁冲突：删除ID={}的记录失败，记录不存在或已被删除", id);
            throw new BusinessException(
                ResultCodeEnum.ERROR.getCode(),
                "删除失败：记录不存在或已被删除"
            );
        }
        return true;
    }

    /**
     * 覆盖父类的 updateById，使用乐观锁检查版本
     */
    @Override
    public boolean updateById(T entity) {
        return updateByIdWithLockCheck(entity);
    }

    /**
     * 覆盖父类的 removeById(T entity)，使用乐观锁检查版本
     * 注意：由于不同实体的ID字段命名不同（如organizationId, settlementId等），
     * 我们直接使用 entity 对象本身调用 MyBatis-Plus 的 deleteById
     */
    @Override
    public boolean removeById(T entity) {
        if (entity == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "删除失败：实体不能为空");
        }
        // MyBatis-Plus 的 removeById(T entity) 会自动获取实体的主键值进行删除
        return removeByIdWithLockCheckEntity(entity);
    }

    /**
     * 使用乐观锁检查删除实体
     * 通过实体的主键删除并检查影响行数
     */
    private boolean removeByIdWithLockCheckEntity(T entity) {
        // 使用 MyBatis-Plus 的 deleteById(T entity) 方法
        // 该方法会根据实体类中的 @TableId 注解获取主键值
        int rows = this.baseMapper.deleteById(entity);
        if (rows == 0) {
            log.warn("乐观锁冲突：删除实体失败，实体类型={}", entity.getClass().getSimpleName());
            throw new BusinessException(
                ResultCodeEnum.ERROR.getCode(),
                "删除失败：记录不存在或已被删除"
            );
        }
        return true;
    }
}
