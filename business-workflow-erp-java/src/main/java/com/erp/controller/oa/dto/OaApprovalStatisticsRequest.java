package com.erp.controller.oa.dto;

import lombok.Data;

/**
 * OA审核记录统计请求DTO
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Data
public class OaApprovalStatisticsRequest {
    /**
     * 视图范围：pending-待我审核、submitted-我发起的、processed-已处理、all-全部
     */
    private String viewScope;
}
