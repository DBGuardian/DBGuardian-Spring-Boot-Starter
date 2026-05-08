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
 * 库存表实体
 * 对应表：STOCK
 * 说明：入库单状态流转为"已结算"时，按废物代码+废物类别匹配库存记录，累加当前重量
 */
@Data
@TableName("STOCK")
public class Stock {

    /**
     * 库存编号
     */
    @TableId(value = "库存编号", type = IdType.AUTO)
    private Integer stockId;

    /**
     * 危废类别编码，如 HW49（入库审核通过时从危废条目冗余写入，固化快照）
     */
    @TableField("废物类别")
    private String wasteCategory;

    /**
     * 危险废物代码，如 900-039-49
     */
    @TableField("废物代码")
    private String wasteCode;

    /**
     * 危险废物名称
     */
    @TableField("废物名称")
    private String wasteName;

    /**
     * 当前库存重量（吨）
     */
    @TableField("当前重量")
    private BigDecimal currentWeight;

    /**
     * 辅助数量（桶/件等）- 暂不处理
     */
    @TableField("辅助数量")
    private Integer auxQuantity;

    /**
     * 辅助单位 - 暂不处理
     */
    @TableField("辅助单位")
    private String auxUnit;

    /**
     * 库存位置（库位）
     */
    @TableField("位置")
    private String location;

    /**
     * 创建时间
     */
    @TableField("创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("更新时间")
    private LocalDateTime updateTime;

    /**
     * 引用危废条目编号
     */
    @TableField("危废条目编号")
    private Integer hazardousWasteItemId;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;
}
