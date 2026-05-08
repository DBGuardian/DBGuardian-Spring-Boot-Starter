package com.erp.controller.transport.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 车辆档案分页列表项响应
 *
 * 字段级权限控制已由字段注解迁移为外部配置，此处仅保留字段本身定义。
 */
@Data
public class VehiclePageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 车辆ID
     */
    private Integer vehicleId;

    /**
     * 车辆编号
     */
    private String vehicleCode;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 公司地址
     */
    private String companyAddress;

    /**
     * 车牌号
     */
    private String plateNo;

    /**
     * 车辆类型
     */
    private String vehicleType;

    /**
     * 载重
     */
    private BigDecimal loadCapacity;

    /**
     * 座位数
     */
    private BigDecimal seatCount;

    /**
     * 车辆状态
     */
    private String status;

    /**
     * 经营范围
     */
    private String operationScope;

    /**
     * 经营许可证号
     */
    private String operationLicenseNo;

    /**
     * 发证机关
     */
    private String issuingAuthority;

    /**
     * 发证日期
     */
    private LocalDate issuingDate;

    /**
     * 证件有效期
     */
    private LocalDate licenseValidUntil;

    /**
     * 年检有效期
     */
    private LocalDate inspectionValidUntil;

    /**
     * 技术等级评定日期
     */
    private LocalDate techLevelDate;

    /**
     * 车辆长度(mm)
     */
    private Integer vehicleLengthMm;

    /**
     * 车辆宽度(mm)
     */
    private Integer vehicleWidthMm;

    /**
     * 车辆高度(mm)
     */
    private Integer vehicleHeightMm;

    /**
     * 创建人ID
     */
    private Integer creatorId;

    /**
     * 创建人名称
     */
    private String creatorName;

    /**
     * 创建时间（字符串形式）
     */
    private String createdAt;

    /**
     * 更新时间（字符串形式）
     */
    private String updatedAt;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 已关联的运输合同列表
     */
    private List<ContractSimpleInfo> contracts;

    /**
     * 合同简单信息（用于 VehiclePageResponse.contracts）
     */
    @Data
    public static class ContractSimpleInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 合同编号
         */
        private Integer contractId;

        /**
         * 合同单号
         */
        private String contractNo;

        /**
         * 承运方名称
         */
        private String carrierName;
    }
}

