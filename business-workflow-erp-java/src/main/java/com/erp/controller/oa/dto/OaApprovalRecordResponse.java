package com.erp.controller.oa.dto;

import lombok.Data;

/**
 * OA审核记录响应DTO - 用于列表展示
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
public class OaApprovalRecordResponse {
    /**
     * 审核记录编号
     */
    private Integer approvalRecordId;

    /**
     * 审核编号
     */
    private String approvalNo;

    /**
     * 来源表名
     */
    private String sourceTable;

    /**
     * 来源表中文名称
     */
    private String sourceTableName;

    /**
     * 来源记录编号
     */
    private Integer sourceId;

    /**
     * 关联单号
     */
    private String sourceNo;

    /**
     * 审核标题
     */
    private String title;

    /**
     * 提交人编码
     */
    private Integer submitterId;

    /**
     * 提交人姓名
     */
    private String submitterName;

    /**
     * 审核人编码
     */
    private Integer approverId;

    /**
     * 审核人姓名
     */
    private String approverName;

    /**
     * 审核状态
     */
    private String approvalStatus;

    /**
     * 审核次数
     */
    private Integer approvalCount;

    /**
     * 提交时间
     */
    private String submitTime;

    /**
     * 审核时间
     */
    private String approvalTime;
}
