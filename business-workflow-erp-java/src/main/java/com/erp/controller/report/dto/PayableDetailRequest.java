package com.erp.controller.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 应付账款明细表请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayableDetailRequest {
  private Integer page;
  private Integer size;
  private String contractNo;
  private String partyBName;
  private String settlementType;
  private String pricingMode;
  private String dateStart;
  private String dateEnd;
  private String sortField;
  private String sortOrder;
  private List<String> selectedKeys;

  /**
   * 获取分页偏移量
   */
  public Integer getOffset() {
    if (page == null || page < 1) {
      page = 1;
    }
    if (size == null || size < 1) {
      size = 20;
    }
    return (page - 1) * size;
  }
}
