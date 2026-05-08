package com.erp.entity.common;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 创建时间
     * 注意：如果子类对应的表没有此字段，需要在子类中使用 @TableField(exist = false) 排除
     */
    @TableField("创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 注意：如果子类对应的表没有此字段，需要在子类中使用 @TableField(exist = false) 排除
     */
    @TableField("更新时间")
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     * 使用 MyBatis-Plus 的 @Version 注解启用乐观锁自动处理
     */
    @Version
    @TableField("version")
    private Integer version;
}











































