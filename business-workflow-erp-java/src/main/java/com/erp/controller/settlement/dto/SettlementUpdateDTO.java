package com.erp.controller.settlement.dto;

import lombok.Data;

import javax.validation.constraints.AssertTrue;
import java.util.List;
import java.util.Map;

/**
 * 修改结算单请求DTO
 *
 * @author ERP System
 * @date 2025-01-01
 *
 * 验证规则：
 * - 所有字段都可以为空（新增/修改时）
 * - 只有填写了数据才验证数据格式
 */
@Data
public class SettlementUpdateDTO {

    /**
     * 更新模式：field（单字段更新）/full（兼容旧接口，实际使用增量更新）/incremental（增量更新）
     */
    private String updateMode;

    /**
     * 结算单ID（必填）
     */
    private Long settlementId;

    /**
     * 合同号
     */
    private String contractCode;

    /**
     * 引用来源类型：contract/warehousing/transport
     */
    private String referenceType;

    /**
     * 引用来源单号列表
     */
    private List<String> referenceCodes;

    /**
     * 结算周期：[开始日期, 结束日期]
     */
    private String[] settlementPeriod;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态
     */
    private String status;

    /**
     * 要更新的字段映射（field模式使用）
     */
    private Map<String, Object> fieldUpdates;

    /**
     * 按量结算明细
     */
    private List<SettlementWasteDetailDTO> quantityItems;

    /**
     * 总价包干明细
     */
    private List<SettlementWasteDetailDTO> lumpSumItems;

    /**
     * 价外服务明细
     */
    private List<ServiceItemDTO> serviceItems;

    /**
     * 要删除的价外服务ID列表
     */
    private List<Integer> deleteServiceIds;

    /**
     * 要删除的废物明细ID列表
     */
    private List<Long> deleteDetailIds;

    /**
     * field模式验证：当updateMode为field时，fieldUpdates不能为空
     */
    @AssertTrue(message = "修改字段为空，请重试")
    public boolean isFieldUpdatesValid() {
        if ("field".equals(this.updateMode)) {
            return this.fieldUpdates != null && !this.fieldUpdates.isEmpty();
        }
        return true;
    }

    /**
     * 验证结算单ID
     */
    @AssertTrue(message = "结算单ID不能为空")
    public boolean isSettlementIdValid() {
        if (!"field".equals(this.updateMode)) {
            return this.settlementId != null && this.settlementId > 0;
        }
        return true;
    }
}
