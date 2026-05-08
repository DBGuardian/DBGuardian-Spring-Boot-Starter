package com.erp.entity.production;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出库单危废明细实体
 * 对应表：OUTBOUND_WASTE_ITEM
 */
@Data
@TableName("OUTBOUND_WASTE_ITEM")
public class OutboundWasteItem {

    private static final long serialVersionUID = 1L;

    /** 出库危废明细编号 */
    @TableId(value = "出库危废明细编号", type = IdType.AUTO)
    private Integer itemId;

    /** 出库单编号 */
    @TableField("出库单编号")
    private Integer outboundId;

    /** 库存编号 */
    @TableField("库存编号")
    private Integer stockId;

    /** 入库危废明细编号 */
    @TableField("入库危废明细编号")
    private Integer warehousingItemId;

    /** 危废条目编号 */
    @TableField("危废条目编号")
    private Integer hazardousWasteItemId;

    /** 废物类别快照 */
    @TableField("废物类别")
    private String wasteCategory;

    /** 废物名称 */
    @TableField("废物名称")
    private String wasteName;

    /** 废物代码 */
    @TableField("废物代码")
    private String wasteCode;

    /** 危险特性 */
    @TableField("危险特性")
    private String hazardFeature;

    /** 废物形态 */
    @TableField("废物形态")
    private String form;

    /** 库位 */
    @TableField("库位")
    private String location;

    /** 基本计量单位 */
    @TableField("基本计量单位")
    private String measureUnit;

    /** 是否启用辅助核算 */
    @TableField("是否启用辅助核算")
    private Boolean enableAuxiliaryAccounting;

    /** 辅助计量单位 */
    @TableField("辅助计量单位")
    private String auxUnit;

    /** 辅助单位每基础单位数量 */
    @TableField("辅助单位每基础单位数量")
    private BigDecimal auxPerBase;

    /** 出库数量（吨） */
    @TableField("出库数量")
    private BigDecimal outboundQty;

    /** 出库辅助数量 */
    @TableField("出库辅助数量")
    private BigDecimal outboundAuxQty;

    /** 扣减前库存重量 */
    @TableField("扣减前库存重量")
    private BigDecimal beforeStockQty;

    /** 扣减后库存重量 */
    @TableField("扣减后库存重量")
    private BigDecimal afterStockQty;

    /** 扣减前辅助数量 */
    @TableField("扣减前辅助数量")
    private BigDecimal beforeAuxQty;

    /** 扣减后辅助数量 */
    @TableField("扣减后辅助数量")
    private BigDecimal afterAuxQty;

    /** 有价类重量（吨） */
    @TableField("有价类重量")
    private BigDecimal valuableWeight;

    /** 无价类重量（吨） */
    @TableField("无价类重量")
    private BigDecimal valuelessWeight;

    /** 出库原因 */
    @TableField("出库原因")
    private String outboundReason;

    /** 创建时间 */
    @TableField("创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("更新时间")
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
