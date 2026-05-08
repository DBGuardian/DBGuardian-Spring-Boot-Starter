package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务合同组内危废项实体
 * 对应表：BUSINESS_CONTRACT_WASTE_INFO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("BUSINESS_CONTRACT_WASTE_INFO")
public class BusinessContractWasteInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "危废信息编号", type = IdType.AUTO)
    private Integer wasteInfoId;

    @TableField("明细编号")
    private Integer wasteItemId;

    @TableField("行内顺序")
    private Integer innerRowNo;

    @TableField("来源废物项编号")
    private Integer sourceWasteItemId;

    @TableField("危废类型")
    private String wasteType;

    @TableField("废物代码")
    private String wasteCode;

    @TableField("危废名称")
    private String wasteName;

    /** 逻辑删除：0正常 1已删除 */
    @TableField("是否删除")
    private Integer deleted;
}
