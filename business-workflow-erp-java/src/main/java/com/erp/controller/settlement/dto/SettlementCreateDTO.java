package com.erp.controller.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 创建结算单请求DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class SettlementCreateDTO {

    /**
     * 合同号
     */
  
    private String contractCode;


    /**
     * 引用来源类型：contract/warehousing/transport
     */
    @NotBlank(message = "引用来源类型不能为空")
    private String referenceType;

    /**
     * 引用来源单号列表
     */
    private List<String> referenceCodes;

    /**
     * 结算周期：[开始日期, 结束日期]
     * 前端以 "YYYY-MM" 字符串数组传入，例如 ["2026-01", "2026-01"]
     */
    private String[] settlementPeriod;

    /**
     * 备注
     */
    private String remark;

    /**
     * 甲方按量结算明细
     */
    private List<SettlementWasteDetailDTO> quantityAItems;

    /**
     * 乙方按量结算明细
     */
    private List<SettlementWasteDetailDTO> quantityBItems;

    /**
     * 甲方总价包干明细
     */
    private List<SettlementWasteDetailDTO> lumpSumAItems;

    /**
     * 乙方总价包干明细
     */
    private List<SettlementWasteDetailDTO> lumpSumBItems;

    /**
     * 价外服务明细
     */
    private List<ServiceItemDTO> serviceItems;

}
