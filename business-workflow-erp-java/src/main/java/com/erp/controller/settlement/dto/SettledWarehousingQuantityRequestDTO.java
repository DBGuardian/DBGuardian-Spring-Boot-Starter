package com.erp.controller.settlement.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 已结算入库量查询请求DTO
 *
 * @author ERP System
 * @date 2025-01-02
 */
@Data
public class SettledWarehousingQuantityRequestDTO {

    /**
     * 合同号
     */
    @NotBlank(message = "合同号不能为空")
    private String contractCode;

    /**
     * 废物信息列表
     */
    @NotEmpty(message = "废物信息列表不能为空")
    private List<WasteItemRequest> wasteItems;

    /**
     * 废物信息请求类
     */
    @Data
    public static class WasteItemRequest {
        /**
         * 废物类别
         */
        @NotBlank(message = "废物类别不能为空")
        private String wasteCategory;

        /**
         * 废物名称
         */
        @NotBlank(message = "废物名称不能为空")
        private String wasteName;

        /**
         * 废物代码
         */
        @NotBlank(message = "废物代码不能为空")
        private String wasteCode;

        /**
         * 当前表格行数
         */
        private Integer currentRowIndex;
    }
}
