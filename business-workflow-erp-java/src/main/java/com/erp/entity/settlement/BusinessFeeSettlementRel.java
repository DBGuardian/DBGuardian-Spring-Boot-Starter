package com.erp.entity.settlement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务结算单 ↔ 危废结算单 关联表实体
 *
 * 对应表：BUSINESS_FEE_SETTLEMENT_REL
 * 业务关系说明：
 *   一个业务结算单可关联一个或多个危废结算单（SETTLEMENT）
 *   危废结算单也可被多个业务结算单引用（如拆分业务场景）
 */
@Data
@TableName("BUSINESS_FEE_SETTLEMENT_REL")
public class BusinessFeeSettlementRel {

    private static final long serialVersionUID = 1L;

    /**
     * 关联编号（主键，自增）
     */
    @TableId(value = "关联编号", type = IdType.AUTO)
    private Integer relId;

    /**
     * 关联 BUSINESS_FEE_HEADER.业务序号
     */
    @TableField("业务序号")
    private Integer businessSeq;

    /**
     * 关联 SETTLEMENT.结算单编号
     */
    @TableField("危废结算单编号")
    private Integer settlementId;

    /**
     * 危废结算单单号（冗余快照，便于展示）
     */
    @TableField("危废结算单单号")
    private String settlementCode;

    /**
     * 关联时间
     */
    @TableField("创建时间")
    private LocalDateTime createTime;

    /**
     * 关联人员工编码
     */
    @TableField("创建人编码")
    private Integer creatorId;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;
}
