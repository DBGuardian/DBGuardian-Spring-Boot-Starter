package com.erp.controller.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 出入库明细表Excel导出行数据
 * 用于生成合并单元格的Excel文件
 */
@Data
@Schema(description = "出入库明细表Excel导出行数据")
public class InoutExcelRow {

    @Schema(description = "序号")
    private Integer sequenceNo;

    @Schema(description = "分类：经营/自产/盘库")
    private String category;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "对方名称")
    private String partyName;

    @Schema(description = "业务员")
    private String businessPerson;

    @Schema(description = "车牌号")
    private String plateNumber;

    @Schema(description = "总磅单号")
    private String weighingSlipNo;

    @Schema(description = "废物类别")
    private String wasteCategory;

    @Schema(description = "废物代码")
    private String wasteCode;

    @Schema(description = "废物名称")
    private String wasteName;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "入库数量")
    private BigDecimal inboundQty;

    @Schema(description = "出库数量")
    private BigDecimal outboundQty;

    @Schema(description = "库存数量")
    private BigDecimal stockQty;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "行唯一键")
    private String rowKey;
}
