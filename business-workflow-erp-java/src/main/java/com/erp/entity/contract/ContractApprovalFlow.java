package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 合同审批流实体
 *
 * 对应表：CONTRACT_APPROVAL_FLOW
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("CONTRACT_APPROVAL_FLOW")
public class ContractApprovalFlow extends BaseEntity {

    /**
     * 更新时间 - 数据库表中不存在此字段，排除映射
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;

    /**
     * 审批记录编号
     */
    @TableId(value = "审批记录编号", type = IdType.AUTO)
    private Integer approvalFlowId;

    /**
     * 合同编号
     */
    @TableField("合同编号")
    private Integer contractId;

    /**
     * 节点名称（如：合同创建、合同审核等）
     */
    @TableField("节点名称")
    private String nodeName;

    /**
     * 审批人编码
     */
    @TableField("审批人编码")
    private Integer approverId;

    /**
     * 审批结果：PENDING/APPROVED/REJECTED
     */
    @TableField("审批结果")
    private String approvalResult;

    /**
     * 审批意见
     */
    @TableField("审批意见")
    private String approvalOpinion;

    /**
     * 审批时间
     */
    @TableField("审批时间")
    private LocalDateTime approvalTime;

    /**
     * 创建时间
     */
    @TableField("创建时间")
    private LocalDateTime createTime;
}
















