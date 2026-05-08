package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

// validation intentionally relaxed to allow empty list for differential save
import java.util.List;

/**
 * 关联发票请求DTO
 *
 * @author ERP System
 * @date 2026-01-07
 */
@Data
@ApiModel("关联发票请求")
public class InvoiceAssociateRequest {

    @ApiModelProperty(value = "发票ID列表，允许为空（表示清空关联）", required = false)
    private List<Integer> invoiceIds;
}


