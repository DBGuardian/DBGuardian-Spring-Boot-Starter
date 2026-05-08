package com.erp.controller.contract.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同分页查询请求
 */
@Data
@ApiModel("合同分页查询请求")
public class ContractPageRequest {

    /**
     * 当前页码
     */
    @ApiModelProperty(value = "当前页码，从1开始", example = "1")
    @Min(value = 1, message = "当前页码必须大于等于1")
    private long current = 1;

    /**
     * 每页数量
     */
    @ApiModelProperty(value = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于等于1")
    private long size = 10;

    /**
     * 客户名称（模糊匹配）
     */
    @ApiModelProperty(value = "客户名称（模糊匹配）")
    private String enterpriseName;

    /**
     * 合同状态：待审核/执行中/已完结/已归档/已驳回
     */
    @ApiModelProperty(value = "合同状态")
    private String contractStatus;

    /**
     * 合同状态列表（支持多选）
     */
    @ApiModelProperty(value = "合同状态列表")
    private List<String> contractStatuses;

    /**
     * 签订时间开始（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "签订时间开始（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signTimeStart;

    /**
     * 签订时间结束（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "签订时间结束（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signTimeEnd;

    /**
     * 有效期开始（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "有效期开始（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    /**
     * 有效期结束（格式：yyyy-MM-dd HH:mm:ss）
     */
    @ApiModelProperty(value = "有效期结束（格式：yyyy-MM-dd HH:mm:ss）")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;

    /**
     * 是否已生成PDF
     */
    @ApiModelProperty(value = "是否已生成PDF")
    private Boolean pdfGenerated;

    /**
     * 排序字段：contractId/enterpriseName/contractAmount/signTime/contractStatus/validTo
     */
    @ApiModelProperty(value = "排序字段")
    private String sortField;

    /**
     * 排序方向：asc/desc
     */
    @ApiModelProperty(value = "排序方向：asc/desc")
    private String sortOrder;

    /**
     * 字段级/数据范围权限页面编码
     *
     * <p>示例：合同管理:合同变更:页面 / 合同管理:合同履行:页面 等。</p>
     * <p>当前用于在统一列表接口中，根据不同页面的页面级权限配置独立控制数据范围。</p>
     */
    @ApiModelProperty(value = "字段级/数据范围权限页面编码（如：合同管理:合同变更:页面）")
    private String fieldPermissionPageCode;

    /**
     * 数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断
     */
    @ApiModelProperty(value = "数据查看范围：SELF=仅查看自己, ALL=查看全部, 空=根据权限自动判断")
    private String viewScope;
}



