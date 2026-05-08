package com.erp.controller.oa.dto;

import lombok.Data;

/**
 * OA审核操作请求DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
public class OaApprovalApproveRequest {
    /**
     * 审核记录ID
     */
    private Integer approvalRecordId;

    /**
     * 来源表名
     */
    private String sourceTable;

    /**
     * 来源记录ID
     */
    private Integer sourceId;

    /**
     * 审核结果：通过/驳回
     */
    private String result;

    /**
     * 审核意见
     */
    private String opinion;
}
