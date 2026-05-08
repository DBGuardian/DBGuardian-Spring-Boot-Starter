package com.erp.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OA审核记录实体类
 *
 * 对应表：OA_APPROVAL_RECORD
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("OA_APPROVAL_RECORD")
public class OaApprovalRecord extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审核记录编号（主键）
     */
    @TableId(value = "审核记录编号", type = IdType.AUTO)
    private Integer approvalRecordId;

    /**
     * 审核编号，格式：OA-YYYYMMDD-XXXX
     */
    @TableField("审核编号")
    private String approvalNo;

    /**
     * 来源表名（数据库英文表名）
     */
    @TableField("来源表名")
    private String sourceTable;

    /**
     * 来源表中文名称，如合同信息表、报价单信息表
     */
    @TableField("来源表中文名称")
    private String sourceTableName;

    /**
     * 来源记录编号（业务表主键）
     */
    @TableField("来源记录编号")
    private Integer sourceId;

    /**
     * 关联单号（如合同编号、报价单号等）
     */
    @TableField("关联单号")
    private String sourceNo;

    /**
     * 审核标题（自动生成）
     */
    @TableField("审核标题")
    private String title;

    /**
     * 提交人编码，关联EMPLOYEE表
     */
    @TableField("提交人编码")
    private Integer submitterId;

    /**
     * 提交人姓名快照
     */
    @TableField("提交人姓名")
    private String submitterName;

    /**
     * 审核人编码，关联EMPLOYEE表
     */
    @TableField("审核人编码")
    private Integer approverId;

    /**
     * 审核人姓名快照
     */
    @TableField("审核人姓名")
    private String approverName;

    /**
     * 审核状态：待审核/已通过/已驳回/已撤回
     */
    @TableField("审核状态")
    private String approvalStatus;

    /**
     * 审核次数（第N次提交）
     */
    @TableField("审核次数")
    private Integer approvalCount;

    /**
     * 提交时间
     */
    @TableField("提交时间")
    private LocalDateTime submitTime;

    /**
     * 审核时间
     */
    @TableField("审核时间")
    private LocalDateTime approvalTime;

    /**
     * 是否删除（逻辑删除：0正常 1已删除）
     */
    @TableField("是否删除")
    private Integer deleted;
}
