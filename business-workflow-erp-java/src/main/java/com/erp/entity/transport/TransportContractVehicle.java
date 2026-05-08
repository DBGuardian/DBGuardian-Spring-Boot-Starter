package com.erp.entity.transport;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运输合同-车辆关联实体
 */
@Data
@TableName("TRANSPORT_CONTRACT_VEHICLE")
public class TransportContractVehicle {

    @TableId(type = IdType.AUTO)
    @TableField("关联编号")
    private Integer relationId;

    @TableField("合同编号")
    private Integer contractId;

    @TableField("车辆编号")
    private Integer vehicleId;

    @TableField("关联时间")
    private LocalDateTime relationTime;

    @TableField("关联人编码")
    private Integer relationUserId;

    @TableField("备注")
    private String remark;
}
