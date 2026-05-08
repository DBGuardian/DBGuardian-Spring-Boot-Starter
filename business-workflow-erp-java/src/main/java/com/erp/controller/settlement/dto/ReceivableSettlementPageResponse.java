package com.erp.controller.settlement.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单分页响应（收款结算）
 *
 * <p>对应页面：合同结算:危险废物结算-收款结算:页面</p>
 */
@Data
@ApiModel("结算单分页响应（收款结算）")
public class ReceivableSettlementPageResponse {

    @ApiModelProperty("结算单编号")
    private Long settlementId;

    @ApiModelProperty("结算单单号")
    private String settlementCode;

    @ApiModelProperty("合同号")
    private String contractCode;

    @ApiModelProperty("合同编号")
    private Integer contractId;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("结算类型：RECEIVABLE=收款 / PAYABLE=付款")
    private String settlementType;

    @ApiModelProperty("关联来源类型：CONTRACT/WAREHOUSING/TRANSPORT")
    private String sourceType;

    @ApiModelProperty("结算周期起")
    private LocalDateTime settlementStartDate;

    @ApiModelProperty("结算周期止")
    private LocalDateTime settlementEndDate;

    @ApiModelProperty("结算金额")
    private BigDecimal settlementAmount;

    @ApiModelProperty("已收金额")
    private BigDecimal receivedAmount;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("制单人编码")
    private Integer creatorId;

    @ApiModelProperty("制单人名称")
    private String creatorName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("审核人编码")
    private Integer auditorId;

    @ApiModelProperty("审核时间")
    private LocalDateTime auditTime;

    @ApiModelProperty("审核意见")
    private String auditOpinion;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("是否锁定")
    private Boolean locked;

    @ApiModelProperty("锁定时间")
    private LocalDateTime lockTime;

    @ApiModelProperty("锁定人编码")
    private Integer lockUserId;

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("更新人编码")
    private Integer updateUserId;

    /**
     * 从通用结算分页响应复制字段，便于统一组装数据后再区分权限视图。
     */
    public static ReceivableSettlementPageResponse from(SettlementPageResponse source) {
        if (source == null) {
            return null;
        }
        ReceivableSettlementPageResponse target = new ReceivableSettlementPageResponse();
        target.setSettlementId(source.getSettlementId());
        target.setSettlementCode(source.getSettlementCode());
        target.setContractCode(source.getContractCode());
        target.setContractId(source.getContractId());
        target.setCustomerName(source.getCustomerName());
        target.setSettlementType(source.getSettlementType());
        target.setSourceType(source.getSourceType());
        target.setSettlementStartDate(source.getSettlementStartDate());
        target.setSettlementEndDate(source.getSettlementEndDate());
        target.setSettlementAmount(source.getSettlementAmount());
        target.setReceivedAmount(source.getReceivedAmount());
        target.setStatus(source.getStatus());
        target.setCreatorId(source.getCreatorId());
        target.setCreatorName(source.getCreatorName());
        target.setCreateTime(source.getCreateTime());
        target.setAuditorId(source.getAuditorId());
        target.setAuditTime(source.getAuditTime());
        target.setAuditOpinion(source.getAuditOpinion());
        target.setRemark(source.getRemark());
        target.setLocked(source.getLocked());
        target.setLockTime(source.getLockTime());
        target.setLockUserId(source.getLockUserId());
        target.setVersion(source.getVersion());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateUserId(source.getUpdateUserId());
        return target;
    }
}

