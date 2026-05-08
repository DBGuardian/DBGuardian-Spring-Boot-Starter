package com.erp.entity.transport;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车辆实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("VEHICLE")
public class Vehicle extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 车辆编号
     */
    @TableId(value = "车辆编号", type = IdType.AUTO)
    private Integer vehicleId;

    /**
     * 公司名称
     */
    @TableField("公司名称")
    private String companyName;

    /**
     * 公司地址
     */
    @TableField("公司地址")
    private String companyAddress;

    /**
     * 车牌号
     */
    @TableField("车牌号")
    private String plateNo;

    /**
     * 车辆类型
     */
    @TableField("车辆类型")
    private String vehicleType;

    /**
     * 核载吨位
     */
    @TableField("核载吨位")
    private BigDecimal loadCapacity;

    /**
     * 座位数
     */
    @TableField("座位数")
    private BigDecimal seatCount;

    /**
     * 车辆状态：空闲/在途/维修
     */
    @TableField("车辆状态")
    private String status;

    /**
     * 经营范围
     */
    @TableField("经营范围")
    private String operationScope;

    /**
     * 经营许可证号
     */
    @TableField("经营许可证号")
    private String operationLicenseNo;

    /**
     * 发证机关
     */
    @TableField("发证机关")
    private String issuingAuthority;

    /**
     * 发证日期
     */
    @TableField("发证日期")
    private LocalDate issuingDate;

    /**
     * 有效期至（经营许可证有效期）
     */
    @TableField("有效期至")
    private LocalDate licenseValidUntil;

    /**
     * 检验有效期至
     */
    @TableField("检验有效期至")
    private LocalDate inspectionValidUntil;

    /**
     * 技术等级评定日期
     */
    @TableField("技术等级评定日期")
    private LocalDate techLevelDate;

    /**
     * 车辆长度（mm）
     */
    @TableField("车辆长度_mm")
    private Integer vehicleLengthMm;

    /**
     * 车辆宽度（mm）
     */
    @TableField("车辆宽度_mm")
    private Integer vehicleWidthMm;

    /**
     * 车辆高度（mm）
     */
    @TableField("车辆高度_mm")
    private Integer vehicleHeightMm;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 备注
     */
    @TableField("备注")
    private String remarks;

    /**
     * 创建人姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String creatorName;

    /**
     * 车辆编号（用于前端显示，非数据库字段）
     */
    @TableField(exist = false)
    private String vehicleCode;
}

