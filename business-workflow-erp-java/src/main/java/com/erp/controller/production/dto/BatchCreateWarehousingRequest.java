package com.erp.controller.production.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量创建入库单请求
 */
@Data
@ApiModel("批量创建入库单请求")
public class BatchCreateWarehousingRequest {

    @ApiModelProperty(value = "总磅单号", required = true)
    @NotBlank(message = "总磅单号不能为空")
    private String weighingSlipNo;

    @ApiModelProperty(value = "入库时间", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime warehousingTime;

    @ApiModelProperty(value = "仓管员编码", required = true)
    private Integer warehouseKeeperId;

    @ApiModelProperty("入库备注")
    private String remark;

    @ApiModelProperty(value = "入库单列表", required = true)
    @NotEmpty(message = "至少需要创建一个入库单")
    @Valid
    private List<WarehousingForm> warehousingList;

    /**
     * 单个入库单表单
     */
    @Data
    @ApiModel("单个入库单表单")
    public static class WarehousingForm {
        @ApiModelProperty(value = "运输单号", required = true)
        @NotBlank(message = "运输单号不能为空")
        private String dispatchCode;

        @ApiModelProperty(value = "入库时间", required = true)
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime warehousingTime;

        @ApiModelProperty(value = "仓管员编码", required = true)
        private Integer warehouseKeeperId;

        @ApiModelProperty("入库备注")
        private String remark;

        @ApiModelProperty(value = "危废明细列表", required = true)
        @NotEmpty(message = "至少需要一条危废明细")
        @Valid
        private List<WarehousingItemForm> items;
    }

    /**
     * 入库单危废明细表单
     */
    @Data
    @ApiModel("入库单危废明细表单")
    public static class WarehousingItemForm {
        @ApiModelProperty(value = "收运通知单明细编号", required = true)
        private Integer pickupNoticeItemId;

        @ApiModelProperty(value = "实际收运数量（吨）", required = true)
        private java.math.BigDecimal actualQty;
        
        @ApiModelProperty("实际收运辅助数量（桶/袋等）")
        private java.math.BigDecimal actualAuxQuantity;

        @ApiModelProperty("差异原因")
        private String differenceReason;

        @ApiModelProperty("有价类重量（吨，可回收利用部分，保留6位小数）")
        private java.math.BigDecimal valuableWeight;

        @ApiModelProperty("无价类重量（吨，不可回收利用部分，保留6位小数）")
        private java.math.BigDecimal valuelessWeight;
    }
}


