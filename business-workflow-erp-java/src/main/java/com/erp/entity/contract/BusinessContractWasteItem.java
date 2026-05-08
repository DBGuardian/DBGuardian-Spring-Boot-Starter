package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 业务合同报价组实体
 * 对应表：BUSINESS_CONTRACT_WASTE_ITEM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("BUSINESS_CONTRACT_WASTE_ITEM")
public class BusinessContractWasteItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "明细编号", type = IdType.AUTO)
    private Integer wasteItemId;

    @TableField("合同编号")
    private Integer contractId;

    @TableField("行号")
    private Integer rowNo;

    @TableField("来源报价项编号")
    private Integer sourceQuotationItemId;

    @TableField("结算类型")
    private String settlementType;

    @TableField("单价底价")
    private BigDecimal unitFloorPrice;

    @TableField("合同底价")
    private BigDecimal contractFloorPrice;

    /** 逻辑删除：0正常 1已删除 */
    @TableField("是否删除")
    private Integer deleted;
}
