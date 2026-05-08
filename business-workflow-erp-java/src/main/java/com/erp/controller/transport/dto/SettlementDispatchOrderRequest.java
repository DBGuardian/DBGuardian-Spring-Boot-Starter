package com.erp.controller.transport.dto;

import lombok.Data;

/**
 * 可结算派车单分页查询请求
 */
@Data
public class SettlementDispatchOrderRequest {

    /**
     * 当前页
     */
    private Integer current = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 合同单号（精确匹配）
     */
    private String contractCode;

    /**
     * 搜索关键词（车牌号/司机姓名/派车单号模糊搜索）
     */
    private String searchKeyword;

    /**
     * 是否关联查询
     * true: 通过合同关联的车辆编号匹配派车单
     * false/null: 通过收运通知单关联的合同号匹配派车单
     */
    private Boolean isRelated;

    /**
     * 是否包含未关联运输合同的总磅单（孤儿数据）
     * 当 selectedEntryType === 'orphan' 时设置为 true
     */
    private Boolean includeUnrelated;

    /**
     * 是否排除已关联结算单的总磅单
     * 用于结算页面筛选"可结算"数据
     */
    private Boolean excludeSettled;
}
