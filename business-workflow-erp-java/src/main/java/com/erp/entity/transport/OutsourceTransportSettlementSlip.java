package com.erp.entity.transport;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 委外运输结算-总磅单关联表实体
 * 对应表：OTS_SLIP_RELATION
 *
 * 业务说明：
 * - 一个结算单可以关联多个总磅单（一趟一趟地结算）
 * - 一个总磅单只能被一个结算单关联（避免重复结算）
 * - 通过总磅单可以获取：车号、净重、运输单、收运通知单、合同等信息
 */
@Data
@TableName("OTS_SLIP_RELATION")
public class OutsourceTransportSettlementSlip {

    /**
     * 关联记录主键ID
     */
    @TableId(value = "关联编号", type = IdType.AUTO)
    private Integer relationId;

    /**
     * 关联的结算单编号
     */
    @TableField("结算单编号")
    private Integer settlementId;

    /**
     * 关联的结算单单号（冗余字段）
     */
    @TableField("结算单单号")
    private String settlementNo;

    /**
     * 关联的总磅单编号
     */
    @TableField("总磅单编号")
    private Integer slipId;

    /**
     * 关联的总磅单号（冗余字段）
     */
    @TableField("总磅单单号")
    private String slipCode;

    /**
     * 创建时间
     */
    @TableField("创建时间")
    private LocalDateTime createTime;

    /**
     * 乐观锁版本号
     */
    @TableField("version")
    private Integer version;
}
