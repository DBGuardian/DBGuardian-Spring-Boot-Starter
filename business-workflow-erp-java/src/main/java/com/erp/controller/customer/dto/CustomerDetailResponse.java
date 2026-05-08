package com.erp.controller.customer.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户详情返回
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CustomerDetailResponse extends CustomerPageResponse {

    private static final long serialVersionUID = 1L;

    private Integer creatorId;
    private String creatorName;
    private String remark;
}




