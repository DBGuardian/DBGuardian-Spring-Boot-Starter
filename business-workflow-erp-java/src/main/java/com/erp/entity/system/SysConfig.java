package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统通用配置实体，对应表 SYS_CONFIG
 *
 * 用于存储配置文件风格的 JSON/长文本配置，不作为业务外键主数据使用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SYS_CONFIG")
public class SysConfig extends BaseEntity {

    /**
     * 创建时间 - 数据库表中不存在此字段，排除映射
     */
    @TableField(exist = false)
    private LocalDateTime createTime;

    /**
     * 配置主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置名称/逻辑键，例如：UNIT、OUT_OF_SCOPE_SERVICES
     */
    @TableField("name")
    private String name;

    /**
     * 配置内容：JSON 或 长文本
     */
    @TableField("value")
    private String value;

    /**
     * 备注说明
     */
    @TableField("remark")
    private String remark;

    /**
     * 最后修改人用户ID（关联员工编码，但不强制外键）
     */
    @TableField("modifier_id")
    private Long modifierId;

    /**
     * 最后修改时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}


