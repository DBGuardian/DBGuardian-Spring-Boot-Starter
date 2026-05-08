package com.erp.controller.customer.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商详情返回
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SupplierDetailResponse extends SupplierPageResponse {

    private static final long serialVersionUID = 1L;

    private Integer creatorId;
    private String creatorName;
}
