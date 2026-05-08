package com.erp.controller.production.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 转移联单列表行响应（含废物子项列表）
 */
@Data
@ApiModel("转移联单列表行响应")
public class TransferManifestPageResponse {

    @ApiModelProperty("联单编号")
    private Integer 联单编号;

    @ApiModelProperty("广东省联单号")
    private String 广东省联单号;

    @ApiModelProperty("国家联单号")
    private String 国家联单号;

    @ApiModelProperty("产生单位")
    private String 产生单位;

    @ApiModelProperty("产废单位所属市")
    private String 产废单位所属市;

    @ApiModelProperty("产废单位所属区")
    private String 产废单位所属区;

    @ApiModelProperty("产废单位所属镇")
    private String 产废单位所属镇;

    @ApiModelProperty("发运人")
    private String 发运人;

    @ApiModelProperty("接收人")
    private String 接收人;

    @ApiModelProperty("接收单位")
    private String 接收单位;

    @ApiModelProperty("接收单位所属省")
    private String 接收单位所属省;

    @ApiModelProperty("接收单位所属市")
    private String 接收单位所属市;

    @ApiModelProperty("接收单位所属区")
    private String 接收单位所属区;

    @ApiModelProperty("许可证编号")
    private String 许可证编号;

    @ApiModelProperty("接收单位处理意见")
    private String 接收单位处理意见;

    @ApiModelProperty("接收日期")
    private String 接收日期;

    @ApiModelProperty("处置方式大类")
    private String 处置方式大类;

    @ApiModelProperty("处置方式小类")
    private String 处置方式小类;

    @ApiModelProperty("车牌号")
    private String 车牌号;

    @ApiModelProperty("承运人")
    private String 承运人;

    @ApiModelProperty("运输单位")
    private String 运输单位;

    @ApiModelProperty("运输开始时间")
    private String 运输开始时间;

    @ApiModelProperty("运输结束时间")
    private String 运输结束时间;

    @ApiModelProperty("计划转移时间")
    private String 计划转移时间;

    @ApiModelProperty("当前阶段")
    private String 当前阶段;

    @ApiModelProperty("补录类型")
    private String 补录类型;

    @ApiModelProperty("是否存在重大差异")
    private Integer 是否存在重大差异;

    @ApiModelProperty("重大差异简述")
    private String 重大差异简述;

    @ApiModelProperty("接收企业备注")
    private String 接收企业备注;

    @ApiModelProperty("是否作废")
    private Integer 是否作废;

    @ApiModelProperty("PDF文件编号")
    @JsonProperty("PDF文件编号")
    private Integer PDF文件编号;

    /**
     * PDF文件名称（连表查询获取，点击查看时直接预览，无需额外查询）
     */
    @ApiModelProperty("PDF文件名称")
    @JsonProperty("PDF文件名称")
    private String PDF文件名称;

    /**
     * PDF文件存储类型：本地/云端（连表查询获取）
     */
    @ApiModelProperty("PDF存储类型")
    @JsonProperty("PDF存储类型")
    private String PDF存储类型;

    /**
     * PDF本地存储路径（存储类型为本地时使用）
     */
    @ApiModelProperty("PDF本地存储路径")
    @JsonProperty("PDF本地存储路径")
    private String PDF本地存储路径;

    /**
     * PDF对象键（存储类型为云端时使用）
     */
    @ApiModelProperty("PDF对象键")
    @JsonProperty("PDF对象键")
    private String PDF对象键;

    /**
     * PDF文件访问URL（可直接用于预览）
     */
    @ApiModelProperty("PDF访问URL")
    @JsonProperty("PDF访问URL")
    private String PDF访问URL;

    @ApiModelProperty("创建时间")
    private String 创建时间;

    @ApiModelProperty("废物子项列表")
    private List<TransferManifestItemResponse> 废物子项;
}
