package com.erp.entity.production;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 转移联单主表实体
 * 对应表：TRANSFER_MANIFEST
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("TRANSFER_MANIFEST")
public class TransferManifest extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 联单编号（主键，自增）
     */
    @TableId(value = "联单编号", type = IdType.AUTO)
    private Integer manifestId;

    /**
     * 广东省固废监管平台联单号
     */
    @TableField("广东省联单号")
    private String gdManifestNo;

    /**
     * 国家固废监管平台联单号
     */
    @TableField("国家联单号")
    private String nationalManifestNo;

    // ---- 产废单位信息 ----

    /**
     * 产废单位名称
     */
    @TableField("产生单位")
    private String producer;

    /**
     * 产废单位所属地级市
     */
    @TableField("产废单位所属市")
    private String producerCity;

    /**
     * 产废单位所属区/县
     */
    @TableField("产废单位所属区")
    private String producerDistrict;

    /**
     * 产废单位所属镇/街道
     */
    @TableField("产废单位所属镇")
    private String producerTown;

    /**
     * 发运人姓名
     */
    @TableField("发运人")
    private String shipper;

    // ---- 接收单位信息 ----

    /**
     * 接收人姓名
     */
    @TableField("接收人")
    private String receiver;

    /**
     * 接收单位名称
     */
    @TableField("接收单位")
    private String receivingUnit;

    /**
     * 接收单位所属省
     */
    @TableField("接收单位所属省")
    private String receivingProvince;

    /**
     * 接收单位所属地级市
     */
    @TableField("接收单位所属市")
    private String receivingCity;

    /**
     * 接收单位所属区/县/镇
     */
    @TableField("接收单位所属区")
    private String receivingDistrict;

    /**
     * 接收单位危废经营许可证编号
     */
    @TableField("许可证编号")
    private String licenseNo;

    /**
     * 接收单位处理意见
     */
    @TableField("接收单位处理意见")
    private String receivingOpinion;

    /**
     * 实际接收日期
     */
    @TableField("接收日期")
    private LocalDate receivingDate;

    // ---- 处置方式 ----

    /**
     * 处置方式大类
     */
    @TableField("处置方式大类")
    private String disposalCategory;

    /**
     * 处置方式小类
     */
    @TableField("处置方式小类")
    private String disposalSubcategory;

    // ---- 运输信息 ----

    /**
     * 运输车辆车牌号
     */
    @TableField("车牌号")
    private String licensePlate;

    /**
     * 承运人姓名
     */
    @TableField("承运人")
    private String carrier;

    /**
     * 运输单位名称
     */
    @TableField("运输单位")
    private String transportUnit;

    /**
     * 运输开始时间
     */
    @TableField("运输开始时间")
    private LocalDateTime transportStartTime;

    /**
     * 运输结束时间
     */
    @TableField("运输结束时间")
    private LocalDateTime transportEndTime;

    // ---- 计划与状态 ----

    /**
     * 计划转移日期
     */
    @TableField("计划转移时间")
    private LocalDate plannedTransferDate;

    /**
     * 联单当前流转阶段（待发运/运输中/已接收/已完结）
     */
    @TableField("当前阶段")
    private String currentStage;

    /**
     * 补录类型（正常/补录）
     */
    @TableField("补录类型")
    private String supplementType;

    /**
     * 是否存在重大差异（0否 1是）
     */
    @TableField("是否存在重大差异")
    private Integer hasMajorDifference;

    /**
     * 重大差异简述
     */
    @TableField("重大差异简述")
    private String majorDifferenceDesc;

    /**
     * 接收企业备注
     */
    @TableField("接收企业备注")
    private String receivingRemark;

    /**
     * 联单是否已作废（0正常 1已作废）
     */
    @TableField("是否作废")
    private Integer isVoided;

    /**
     * 联单 PDF 文件编号（关联 FILE 表）
     */
    @TableField("PDF文件编号")
    private Integer pdfFileId;

    /**
     * 逻辑删除（0正常 1已删除）
     */
    @TableField("是否删除")
    private Integer isDeleted;
}
