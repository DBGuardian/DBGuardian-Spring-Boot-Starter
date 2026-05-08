package com.erp.controller.transport.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车辆档案创建请求
 */
@Data
public class VehicleCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @NotBlank(message = "公司地址不能为空")
    private String companyAddress;

    @NotBlank(message = "车牌号不能为空")
    private String plateNo;

    @NotBlank(message = "车辆类型不能为空")
    private String vehicleType;

    private BigDecimal loadCapacity;

    private BigDecimal seatCount;

    private String status = "空闲";

    private String operationScope;

    private String operationLicenseNo;

    private String issuingAuthority;

    private LocalDate issuingDate;

    private LocalDate licenseValidUntil;

    private LocalDate inspectionValidUntil;

    private LocalDate techLevelDate;

    private Integer vehicleLengthMm;

    private Integer vehicleWidthMm;

    private Integer vehicleHeightMm;

    private String remarks;
}

