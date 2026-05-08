package com.erp.entity.transport;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运输单明细实体（与收运通知单一对一）
 * 对应表：DISPATCH_ORDER_NOTICE
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("DISPATCH_ORDER_NOTICE")
public class DispatchOrderNotice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "运输单明细编号", type = IdType.AUTO)
    private Integer id;

    @TableField("运输单号")
    private String dispatchCode;

    @TableField("收运通知单号")
    private String noticeCode;

    @TableField("运输终点")
    private String endPoint;

    @TableField("实际到达时间")
    private LocalDateTime arriveAt;

    @TableField("预计重量")
    private Double estimatedWeight;

    @TableField("运输距离")
    private BigDecimal transportDistance;
}


