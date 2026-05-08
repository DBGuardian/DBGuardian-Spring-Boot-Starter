package com.erp.entity.settlement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 结算危废信息实体
 *
 * 对应表：SETTLEMENT_WASTE_INFO
 * 业务关系：
 * 1. 与 SETTLEMENT_WASTE_DETAIL 一对一：一条明细对应一条危废信息
 * 2. 保存危废类型、废物代码、危废名称等基础信息
 * 3. 来源废物项编号仅用于审查溯源，允许为空
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SETTLEMENT_WASTE_INFO")
public class SettlementWasteInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 危废信息编号（主键，自增）
     */
    @TableId(value = "危废信息编号", type = IdType.AUTO)
    private Long wasteInfoId;

    /**
     * 结算明细编号（FK，关联 SETTLEMENT_WASTE_DETAIL.明细编号）
     */
    @TableField("结算明细编号")
    private Long detailId;

    /**
     * 来源废物项编号（仅溯源，允许为空）
     */
    @TableField("来源废物项编号")
    private Integer sourceWasteItemId;

    /**
     * 废物类别（如 HW08）
     */
    @TableField("废物类别")
    private String wasteCategory;

    /**
     * 废物代码（如 900-214-08）
     */
    @TableField("废物代码")
    private String wasteCode;

    /**
     * 废物名称
     */
    @TableField("废物名称")
    private String wasteName;

    /**
     * 逻辑删除标记：0正常 1已删除
     */
    @TableField("是否删除")
    private Integer deleted;
}
