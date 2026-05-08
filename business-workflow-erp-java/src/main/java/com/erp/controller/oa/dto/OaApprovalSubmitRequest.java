package com.erp.controller.oa.dto;

import lombok.Data;

/**
 * OA审核提交请求DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
public class OaApprovalSubmitRequest {
    /**
     * 来源表名
     */
    private String sourceTable;

    /**
     * 来源记录ID
     */
    private Integer sourceId;

    /**
     * 来源表中文名称
     */
    private String sourceTableName;

    /**
     * 关联单号
     */
    private String sourceNo;

    /**
     * 审核标题
     */
    private String title;
}
