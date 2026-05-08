package com.erp.entity.production;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 总磅单实体
 * 对应表：WEIGHING_SLIP
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("WEIGHING_SLIP")
public class WeighingSlip extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 总磅单编号
     */
    @TableId(value = "总磅单编号", type = IdType.AUTO)
    private Integer weighingSlipId;

    /**
     * 总磅单号（规则：ZBD-YYYYMMDD-4位序号）
     */
    @TableField("总磅单号")
    private String weighingSlipNo;

    /**
     * 序号（手动输入）
     */
    @TableField("序号")
    private String sequence;

    /**
     * 日期（总磅日期）
     */
    @TableField("日期")
    private LocalDate weighingDate;

    /**
     * 一次过秤时间（第一次过磅时间，一般对应总重）
     */
    @TableField("一次过秤时间")
    private LocalDateTime firstWeighTime;

    /**
     * 二次过秤时间（第二次过磅时间，一般对应空重）
     */
    @TableField("二次过秤时间")
    private LocalDateTime secondWeighTime;

    /**
     * 车号（车号牌，如：粤E70123）
     */
    @TableField("车号")
    private String plateNo;

    /**
     * 总重（kg），车辆满载到达时的重量
     */
    @TableField("总重")
    private BigDecimal grossWeight;

    /**
     * 空重（kg），车辆卸货后空车的重量
     */
    @TableField("空重")
    private BigDecimal tareWeight;

    /**
     * 净重（kg），自动总重 - 空重，计算得出
     */
    @TableField("净重")
    private BigDecimal netWeight;

    /**
     * 总磅单照片路径
     */
    @TableField("总磅单照片")
    private String photoUrl;

    /**
     * 状态：待细分/已细分
     */
    @TableField("状态")
    private String status;

    /**
     * 创建人编码（创建总磅的人员编码）
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 总磅备注信息
     */
    @TableField("总磅备注")
    private String remark;

}



