package com.erp.entity.settlement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务结算明细危废信息子表实体
 *
 * 对应表：BUSINESS_FEE_ITEM_WASTE_INFO
 * 业务关系说明：
 *   所有结算模式均通过本表统一维护危废信息快照，不再在主明细表冗余危废名称/代码/类别。
 *   sourceWasteDetailIds 表示当前危废子项对应的来源危废明细编号数组快照，仅用于保存溯源数据与页面回显，可为空。
 */
@Data
@TableName("BUSINESS_FEE_ITEM_WASTE_INFO")
public class BusinessFeeItemWasteInfo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "危废信息编号", type = IdType.AUTO)
    private Integer wasteInfoId;

    @TableField("明细序号")
    private Integer itemSeq;

    @TableField("行内顺序")
    private Integer rowOrder;

    @TableField(value = "来源危废明细编号", typeHandler = JacksonTypeHandler.class)
    private List<Integer> sourceWasteDetailIds;

    @TableField("废物类别")
    private String wasteCategory;

    @TableField("废物代码")
    private String wasteCode;

    @TableField("废物名称")
    private String wasteName;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("更新时间")
    private LocalDateTime updateTime;

    @TableField("version")
    private Integer version;
}
