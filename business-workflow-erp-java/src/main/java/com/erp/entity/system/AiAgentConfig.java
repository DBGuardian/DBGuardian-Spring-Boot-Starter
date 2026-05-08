package com.erp.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大模型智能体配置实体
 *
 * 对应表：AI_AGENT_CONFIG
 *
 * @author ERP
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("AI_AGENT_CONFIG")
public class AiAgentConfig extends BaseEntity {

    /**
     * 智能体编号
     */
    @TableId(value = "智能体编号", type = IdType.AUTO)
    private Integer agentId;

    /**
     * 智能体编码，系统内部唯一标识
     */
    @TableField("智能体编码")
    private String agentCode;

    /**
     * 智能体名称
     */
    @TableField("智能体名称")
    private String agentName;

    /**
     * 提供方（DEEPSEEK、ARK、OPENAI 等）
     */
    @TableField("提供方")
    private String provider;

    /**
     * 接口地址（Base URL）
     */
    @TableField("接口地址")
    private String baseUrl;

    /**
     * 模型名称或ID
     */
    @TableField("模型名称")
    private String modelName;

    /**
     * API Key 密文（AES 加密后存储）
     */
    @TableField("API密钥密文")
    private String apiKeyCipher;

    /**
     * 启用状态：ENABLED / DISABLED
     */
    @TableField("启用状态")
    private String status;

    /**
     * 是否默认智能体：0-否，1-是
     */
    @TableField("默认智能体标记")
    private Integer isDefault;

    /**
     * 排序号
     */
    @TableField("排序号")
    private Integer sortOrder;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer createUserId;

    /**
     * 更新人编码
     */
    @TableField("更新人编码")
    private Integer updateUserId;
}





