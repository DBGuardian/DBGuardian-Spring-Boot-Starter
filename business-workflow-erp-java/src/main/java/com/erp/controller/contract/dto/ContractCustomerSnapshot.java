package com.erp.controller.contract.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户信息快照（合同/报价单共用）
 */
@Data
@ApiModel("客户信息快照")
public class ContractCustomerSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("客户编码（为空表示临时客户）")
    private Integer customerId;

    @ApiModelProperty("快照类型：EXISTING/TEMPORARY")
    private String customerType;

    @ApiModelProperty("客户名称/甲方名称")
    private String customerName;

    @ApiModelProperty("统一社会信用代码")
    private String creditCode;

    @ApiModelProperty("地址")
    private String address;

    @ApiModelProperty("联系人")
    private String contactPerson;

    @ApiModelProperty("联系电话")
    private String contactPhone;

    @ApiModelProperty("法定代表人")
    private String legalRepresentative;

    @ApiModelProperty("业务员编码")
    private Integer ownerEmployeeId;

    @ApiModelProperty("业务员姓名")
    private String ownerEmployeeName;

    @ApiModelProperty("备注/补充信息")
    private String remark;

    @ApiModelProperty("快照时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime snapshotTime;
}

