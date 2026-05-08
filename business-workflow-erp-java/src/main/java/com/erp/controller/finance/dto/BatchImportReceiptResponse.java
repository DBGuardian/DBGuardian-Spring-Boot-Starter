package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入银行回单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "批量导入银行回单响应")
public class BatchImportReceiptResponse {

    @ApiModelProperty(value = "总文件数")
    private int totalCount;

    @ApiModelProperty(value = "成功匹配并导入的文件数")
    private int successCount;

    @ApiModelProperty(value = "匹配失败的文件数")
    private int failCount;

    @ApiModelProperty(value = "未找到匹配流水的文件数")
    private int notFoundCount;

    @ApiModelProperty(value = "导入成功详情")
    private List<ImportSuccessItem> successList;

    @ApiModelProperty(value = "导入失败详情")
    private List<ImportFailItem> failList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "导入成功项")
    public static class ImportSuccessItem {
        @ApiModelProperty(value = "文件名")
        private String fileName;

        @ApiModelProperty(value = "匹配的流水ID")
        private Long transactionId;

        @ApiModelProperty(value = "匹配的流水编码")
        private String transactionCode;

        @ApiModelProperty(value = "文件ID")
        private Integer fileId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "导入失败项")
    public static class ImportFailItem {
        @ApiModelProperty(value = "文件名")
        private String fileName;

        @ApiModelProperty(value = "失败原因")
        private String reason;
    }
}
