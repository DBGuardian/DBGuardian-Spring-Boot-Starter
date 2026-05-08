package com.erp.controller.production.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 转移联单 Excel 导出行
 * 同一主表存在多个子项时，每个子项单独导出一行。
 */
@Data
public class TransferManifestExportRow {

    private Integer 联单编号;
    private String 广东省联单号;
    private String 国家联单号;
    private String 产生单位;
    private String 产废单位所属市;
    private String 产废单位所属区;
    private String 产废单位所属镇;
    private String 发运人;
    private String 接收人;
    private String 接收单位;
    private String 接收单位所属省;
    private String 接收单位所属市;
    private String 接收单位所属区;
    private String 许可证编号;
    private String 接收单位处理意见;
    private String 接收日期;
    private String 处置方式大类;
    private String 处置方式小类;
    private String 车牌号;
    private String 承运人;
    private String 运输单位;
    private String 运输开始时间;
    private String 运输结束时间;
    private String 计划转移时间;
    private String 当前阶段;
    private String 补录类型;
    private Integer 是否存在重大差异;
    private String 重大差异简述;
    private String 接收企业备注;
    private Integer 是否作废;
    private Integer PDF文件编号;

    private Integer 子项编号;
    private String 废物类别;
    private String 废物代码;
    private String 废物名称;
    private String 废物形态;
    private String 包装方式;
    private BigDecimal 计划数量;
    private BigDecimal 确认数量;
    private String 计量单位;
}
