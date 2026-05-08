package com.erp.controller.settlement.dto;

import com.erp.controller.finance.dto.AuxUnitOptionDTO;
import com.erp.controller.finance.dto.WasteSettlementDetailDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Import required DTO types
import com.erp.controller.contract.dto.ContractDetailResponse;
import com.erp.controller.production.dto.WarehousingDetailResponse;


/**
 * 结算单审核DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class SettlementAuditDataDTO {

    /**
     * 结算单ID
     */
    private Long settlementId;

    /**
     * 结算单号
     */
    private String settlementCode;

    /**
     * 合同号
     */
    private String contractCode;

   /**
     * 合同编号
     */
    private Integer contractId;


    /**
     * 结算类型
     */
    private String settlementType;

    /**
     * 引用来源类型
     */
    private String sourceType;

    /**
     * 结算周期开始
     */
    private LocalDateTime settlementPeriodStart;

    /**
     * 结算周期结束
     */
    private LocalDateTime settlementPeriodEnd;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 已收金额
     */
    private BigDecimal receivedAmount;

    /**
     * 状态
     */
    private String status;

    /**
     * 制单人
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 审核人
     */
    private String auditorName;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否锁定
     */
    private Boolean isLocked;

    /**
     * 按量结算明细
     */
    private List<WasteSettlementDetailDTO> quantityItems;

    /**
     * 总价包干明细
     */
    private List<WasteSettlementDetailDTO> lumpSumItems;

    /**
     * 价外服务明细
     */
    private List<ServiceItemDTO> serviceItems;

    /**
     * 关联入库记录详情列表
     */
    private List<WarehousingDetailResponse> warehousingCodes;

    /**
     * 关联运输记录编码列表
     */
    private List<String> transportCodes;


    /**
     * 合同信息
     */
    private List<ContractDetailResponse> contractInfo;

    /**
     * 辅助计量单位选项
     */
    private List<AuxUnitOptionDTO> auxUnitOptions;
}
