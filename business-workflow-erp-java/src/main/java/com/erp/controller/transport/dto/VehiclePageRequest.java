package com.erp.controller.transport.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 车辆档案分页查询请求
 */
@Data
public class VehiclePageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页
     */
    private Long current = 1L;

    /**
     * 每页大小
     */
    private Long size = 10L;

    /**
     * 车牌号（模糊搜索）
     */
    private String plateNo;

    /**
     * 车辆编号（模糊搜索）
     */
    private String vehicleCode;

    /**
     * 公司名称（模糊搜索）
     */
    private String companyName;

    /**
     * 车辆状态（精确匹配：空闲/在途/维修）
     */
    private String status;

    /**
     * 创建人过滤（用于导出时的数据范围控制）
     * viewScope=SELF 时由后端强制覆盖为当前员工ID，viewScope=ALL 时为 null
     * 前端在 viewScope=SELF 时传入当前员工ID，后端会在 Service 层做安全校验并强制覆盖
     */
    private Integer creatorFilter;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 排序方向（asc: 升序, desc: 降序）
     */
    private String orderDirection;

    /**
     * 是否包含关联合同信息
     * 用于车辆选择弹窗，标识车辆已关联的运输合同
     */
    private Boolean includeContracts;

    /**
     * 数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断
     */
    private String viewScope;
}

