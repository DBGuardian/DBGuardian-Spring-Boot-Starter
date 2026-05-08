package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 入库危废明细（含合同匹配信息）请求DTO
 * 用于后端统一处理入库数据与合同数据的匹配
 */
@Data
@ApiModel("入库危废明细（含合同匹配信息）请求参数")
public class WarehousingWasteDetailWithContractRequest {

    /**
     * 入库单信息列表（用于双层锁定：入库单号 + 入库单编号必须同时匹配）
     */
    @NotEmpty(message = "入库单列表不能为空")
    @Valid
    @ApiModelProperty(value = "入库单列表（双层锁定：入库单号+入库单编号必须同时匹配）", required = true)
    private List<WarehousingItemDTO> warehousingList;

    @NotNull(message = "合同编号不能为空")
    @ApiModelProperty(value = "合同编号（自增主键）", required = true)
    private Integer contractId;

    @NotBlank(message = "合同号不能为空")
    @ApiModelProperty(value = "合同号（业务可见编号，如：HQ-YYYYMMDD-XXXXX）", required = true)
    private String contractNo;

    /**
     * 入库单信息DTO（用于双层锁定）
     */
    @Data
    @ApiModel("入库单信息")
    public static class WarehousingItemDTO {

        @NotBlank(message = "入库单号不能为空")
        @ApiModelProperty(value = "入库单号（业务可见编号）", required = true)
        private String warehousingCode;

        @NotNull(message = "入库单编号不能为空")
        @ApiModelProperty(value = "入库单编号（自增主键）", required = true)
        private Integer warehousingId;
    }
}
