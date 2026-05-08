package com.erp.controller.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 危险废物类别限额配置更新请求
 */
@Data
public class HazardousWasteCategoryConfigUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 废物类别
     */
    @NotBlank(message = "废物类别不能为空")
    private String wasteCategory;

    /**
     * 限额
     */
    @NotNull(message = "限额不能为空")
    private BigDecimal limitAmount;

    /**
     * 限额开始时间
     */
    @NotNull(message = "限额开始时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date limitStartTime;

    /**
     * 限额结束时间
     */
    @NotNull(message = "限额结束时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date limitEndTime;
}






