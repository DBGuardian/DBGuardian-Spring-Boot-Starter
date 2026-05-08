package com.erp.controller.transport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 危废类别限额与已入库量响应
 */
@Data
public class WasteCategoryLimitResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 对应的危废代码集合（入参中匹配到此类别的所有代码）
     */
    private List<String> wasteCodes;

    /**
     * 废物类别编码，如 HW01
     */
    private String wasteCategory;

    /**
     * 废物类别名称
     */
    private String wasteCategoryName;

    /**
     * 限额（吨）
     */
    private BigDecimal limitAmount;

    /**
     * 限额开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date limitStartTime;

    /**
     * 限额结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date limitEndTime;

    /**
     * 限额时间段内已入库量（吨）
     */
    private BigDecimal inboundAmount;
}


