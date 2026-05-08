package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("业务员分页查询请求")
public class SalespersonPageRequest {

    @ApiModelProperty("页码（默认1）")
    private Integer current = 1;

    @ApiModelProperty("每页条数（默认10）")
    private Integer size = 10;

    @ApiModelProperty("业务员姓名关键词")
    private String salespersonName;

    @ApiModelProperty("甲方名称关键词")
    private String partyAName;

    @ApiModelProperty("联系电话")
    private String salespersonPhone;

    @ApiModelProperty("搜索关键词（用于 search 接口）")
    private String keyword;

    /**
     * 数据范围过滤：创建人编码
     * 后端根据员工viewScope配置强制填充，用于viewScope=SELF时仅查看自己创建的业务员
     */
    @ApiModelProperty(value = "数据范围过滤（创建人编码，后端根据viewScope控制）", hidden = true)
    private Integer creatorFilter;
}
        