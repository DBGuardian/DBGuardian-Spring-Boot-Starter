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
 * 消息通知实体
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("MESSAGE")
public class Message extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 消息编号
     */
    @TableId(value = "消息编号", type = IdType.AUTO)
    private Integer messageId;

    /**
     * 消息类型：预警消息/业务通知/系统消息
     */
    @TableField("消息类型")
    private String messageType;

    /**
     * 消息分类：合同/客户/财务/生产/系统等
     */
    @TableField("消息分类")
    private String messageCategory;

    /**
     * 消息标题
     */
    @TableField("消息标题")
    private String messageTitle;

    /**
     * 消息内容
     */
    @TableField("消息内容")
    private String messageContent;

    /**
     * 消息优先级：紧急/高/中/低
     */
    @TableField("消息优先级")
    private String messagePriority;

    /**
     * 接收人编码
     */
    @TableField("接收人编码")
    private Integer receiverId;

    /**
     * 发送人编码
     */
    @TableField("发送人编码")
    private Integer senderId;

    /**
     * 关联业务类型
     */
    @TableField("关联业务类型")
    private String businessType;

    /**
     * 关联业务ID
     */
    @TableField("关联业务ID")
    private Integer businessId;

    /**
     * 消息状态：未读/已读/已删除
     */
    @TableField("消息状态")
    private String messageStatus;

    /**
     * 已读时间
     */
    @TableField("已读时间")
    private LocalDateTime readTime;

    /**
     * 删除时间
     */
    @TableField("删除时间")
    private LocalDateTime deleteTime;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    // 非数据库字段
    @TableField(exist = false)
    private String receiverName;

    @TableField(exist = false)
    private String senderName;

    /**
     * 排除BaseEntity中的updateTime字段（MESSAGE表没有更新时间字段）
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;
}

































