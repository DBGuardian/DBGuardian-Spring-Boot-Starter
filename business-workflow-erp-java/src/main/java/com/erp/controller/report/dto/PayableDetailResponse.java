package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 应付账款明细表响应参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayableDetailResponse {
  private List<PayableContractRow> records;
  private Long total;
  private Boolean fromCache;
  private String cacheTime;
}
