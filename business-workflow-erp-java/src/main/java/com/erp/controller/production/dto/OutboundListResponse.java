package com.erp.controller.production.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 出库单列表响应
 */
@Data
@ApiModel("出库单列表响应")
public class OutboundListResponse {

    @ApiModelProperty("统计信息")
    private List<OutboundStat> stats;

    @ApiModelProperty("记录列表")
    private List<OutboundPageResponse> records;

    @ApiModelProperty("总记录数")
    private Long total;

    @ApiModelProperty("当前页码")
    private Integer current;

    @ApiModelProperty("每页数量")
    private Integer size;

    /**
     * 统计信息
     */
    @Data
    public static class OutboundStat {
        private String label;
        private String value;
        private String color;
    }

    /**
     * 出库单列表行
     */
    @Data
    public static class OutboundPageResponse {

        private Integer outboundId;
        private String outboundNo;
        private String outboundType;
        private String outboundTime;
        private String contractCode;
        private String customerName;
        private String destinationType;
        private String destinationName;
        private String wasteName;
        private String wasteCode;
        private String location;
        private BigDecimal outboundQty;
        private BigDecimal outboundAuxQty;
        private String auxUnit;
        private BigDecimal beforeStockQty;
        private BigDecimal afterStockQty;
        private Integer handlerId;
        private String handlerName;
        private String auditTime;
        private String auditorName;
        private String status;
        private Boolean locked;
        private String remark;
    }
}
