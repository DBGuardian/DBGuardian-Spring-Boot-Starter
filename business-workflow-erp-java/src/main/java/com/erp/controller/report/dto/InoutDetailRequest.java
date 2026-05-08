package com.erp.controller.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 出入库明细表查询请求
 */
@Data
@Schema(description = "出入库明细表查询请求")
public class InoutDetailRequest {

    @Schema(description = "分类：经营/自产/盘库", example = "经营")
    private String category;

    @Schema(description = "合同编号", example = "HT202401")
    private String contractNo;

    @Schema(description = "废物代码", example = "900-039-49")
    private String wasteCode;

    @Schema(description = "废物名称", example = "危险废物")
    private String wasteName;

    @Schema(description = "开始日期，格式 YYYY-MM-DD", example = "2024-01-01")
    private String dateStart;

    @Schema(description = "结束日期，格式 YYYY-MM-DD", example = "2024-12-31")
    private String dateEnd;

    @Schema(description = "勾选导出的行唯一键集合")
    private List<String> selectedKeys;

    @Schema(description = "分页页码，默认1", example = "1")
    private Integer page = 1;

    @Schema(description = "分页大小，默认10", example = "10")
    private Integer size = 10;
}
