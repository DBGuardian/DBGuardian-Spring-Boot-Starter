package com.erp.controller.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 更新危险废物名录请求
 */
@Data
public class HazardousWasteUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 废物类别
     */
    @NotBlank(message = "废物类别不能为空")
    private String wasteCategory;

    /**
     * 废物类别名称
     */
    @NotBlank(message = "废物类别名称不能为空")
    private String wasteCategoryName;

    /**
     * 行业来源
     */
    @NotBlank(message = "行业来源不能为空")
    private String industrySource;

    /**
     * 废物代码
     */
    @NotBlank(message = "废物代码不能为空")
    private String wasteCode;

    /**
     * 危险废物名称
     */
    @NotBlank(message = "危险废物名称不能为空")
    private String wasteName;

    /**
     * 危险特性
     */
    private String hazardCharacteristic;

    /**
     * 是否可用
     */
    private Boolean available;
}


