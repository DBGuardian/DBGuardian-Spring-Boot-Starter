package com.erp.controller.report.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 出入库明细表响应
 */
@Data
@Schema(description = "出入库明细表响应")
public class InoutDetailResponse {

    @Schema(description = "分页数据")
    private IPage<InoutDetailRow> page;

    @Schema(description = "数据列表")
    private List<InoutDetailRow> records;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "当前页码")
    private Integer current;

    @Schema(description = "分页大小")
    private Integer size;
}
