package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建/更新出库单请求
 */
@Data
@ApiModel("创建出库单请求")
public class CreateOutboundRequest {

    @ApiModelProperty("出库单编号（更新时使用）")
    private Integer outboundId;

    @ApiModelProperty(value = "出库类型", required = true)
    @NotBlank(message = "出库类型不能为空")
    private String outboundType;

    @ApiModelProperty(value = "出库时间", required = true)
    @NotBlank(message = "出库时间不能为空")
    private String outboundTime;

    @ApiModelProperty(value = "经办人编码", required = true)
    @NotNull(message = "经办人编码不能为空")
    private Integer handlerId;

    @ApiModelProperty("关联合同号")
    private String contractCode;

    @ApiModelProperty("关联客户编号")
    private Integer customerId;

    @ApiModelProperty("出库去向类型")
    private String destinationType;

    @ApiModelProperty("出库去向名称")
    private String destinationName;

    @ApiModelProperty("出库备注")
    private String remark;

    @ApiModelProperty(value = "出库危废明细列表", required = true)
    @NotEmpty(message = "至少需要一条危废明细")
    @Valid
    private List<OutboundItemRequest> items;

    /**
     * 出库危废明细请求
     */
    @Data
    @ApiModel("出库危废明细请求")
    public static class OutboundItemRequest {

        @ApiModelProperty("出库危废明细编号（更新时使用）")
        private Integer itemId;

        @ApiModelProperty("库存编号")
        private Integer stockId;

        @ApiModelProperty("入库危废明细编号")
        private Integer warehousingItemId;

        @ApiModelProperty("危废条目编号")
        private Integer hazardousWasteItemId;

        @ApiModelProperty(value = "废物名称", required = true)
        @NotBlank(message = "废物名称不能为空")
        private String wasteName;

        @ApiModelProperty(value = "废物代码", required = true)
        @NotBlank(message = "废物代码不能为空")
        private String wasteCode;

        @ApiModelProperty("危险特性")
        private String hazardFeature;

        @ApiModelProperty("废物形态")
        private String form;

        @ApiModelProperty("库位")
        private String location;

        @ApiModelProperty("基本计量单位")
        private String measureUnit;

        @ApiModelProperty("是否启用辅助核算")
        private Boolean enableAuxiliaryAccounting;

        @ApiModelProperty("辅助计量单位")
        private String auxUnit;

        @ApiModelProperty("辅助单位每基础单位数量")
        private BigDecimal auxPerBase;

        @ApiModelProperty(value = "出库数量（吨）", required = true)
        @NotNull(message = "出库数量不能为空")
        private BigDecimal outboundQty;

        @ApiModelProperty("出库辅助数量")
        private BigDecimal outboundAuxQty;

        @ApiModelProperty("有价类重量（吨）")
        private BigDecimal valuableWeight;

        @ApiModelProperty("无价类重量（吨）")
        private BigDecimal valuelessWeight;

        @ApiModelProperty("出库原因")
        private String outboundReason;
    }
}
