package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量导入银行回单请求
 */
@Data
@ApiModel(description = "批量导入银行回单请求")
public class BatchImportReceiptRequest {

    @NotNull(message = "账户ID不能为空")
    @ApiModelProperty(value = "账户ID", required = true)
    private Long accountId;

    @NotNull(message = "账期ID不能为空")
    @ApiModelProperty(value = "账期ID", required = true)
    private Long periodId;

    @NotEmpty(message = "回单文件列表不能为空")
    @ApiModelProperty(value = "回单文件列表", required = true)
    private List<ReceiptFileItem> files;

    @Data
    @ApiModel(description = "回单文件项")
    public static class ReceiptFileItem {
        @ApiModelProperty(value = "文件名（不含路径）", required = true)
        private String fileName;

        @ApiModelProperty(value = "文件字节数组（Base64编码）", required = true)
        private String fileContent;
    }
}
