package com.erp.controller.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会计科目列表项响应 DTO
 */
@Data
@ApiModel("会计科目列表项响应")
public class FundSubjectListItemResponse {

    /**
     * 科目编号
     */
    @ApiModelProperty(value = "科目编号", example = "1")
    @JsonProperty("subjectId")
    private Long subjectId;

    /**
     * 科目编码
     */
    @ApiModelProperty(value = "科目编码", example = "1001")
    @JsonProperty("subjectCode")
    private String subjectCode;

    /**
     * 科目名称
     */
    @ApiModelProperty(value = "科目名称", example = "库存现金")
    @JsonProperty("subjectName")
    private String subjectName;

    /**
     * 全科目名称
     */
    @ApiModelProperty(value = "全科目名称", example = "库存现金")
    @JsonProperty("fullSubjectName")
    private String fullSubjectName;

    /**
     * 科目级次
     */
    @ApiModelProperty(value = "科目级次", example = "1")
    @JsonProperty("subjectLevel")
    private Integer subjectLevel;

    /**
     * 上级科目编码
     */
    @ApiModelProperty(value = "上级科目编码", example = "1001")
    @JsonProperty("parentSubjectCode")
    private String parentSubjectCode;

    /**
     * 科目类别：流动资产、非流动资产
     */
    @ApiModelProperty(value = "科目类别：流动资产、非流动资产", example = "流动资产")
    @JsonProperty("subjectCategory")
    private String subjectCategory;

    /**
     * 余额方向：收入、支出
     */
    @ApiModelProperty(value = "余额方向：收入、支出", example = "收入")
    @JsonProperty("balanceDirection")
    private String balanceDirection;

    /**
     * 是否为现金科目
     */
    @ApiModelProperty(value = "是否为现金科目", example = "true")
    @JsonProperty("isCashSubject")
    private Boolean isCashSubject;

    /**
     * 辅助核算
     */
    @ApiModelProperty(value = "辅助核算", example = "false")
    @JsonProperty("auxiliaryAccounting")
    private Boolean auxiliaryAccounting;

    /**
     * 数量核算
     */
    @ApiModelProperty(value = "数量核算", example = "false")
    @JsonProperty("quantityAccounting")
    private Boolean quantityAccounting;

    /**
     * 外币核算
     */
    @ApiModelProperty(value = "外币核算", example = "false")
    @JsonProperty("foreignCurrencyAccounting")
    private Boolean foreignCurrencyAccounting;

    /**
     * 是否启用
     */
    @ApiModelProperty(value = "是否启用", example = "true")
    @JsonProperty("enabled")
    private Boolean enabled;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", example = "科目备注信息")
    @JsonProperty("remark")
    private String remark;

    // 手动添加getter方法确保Lombok正确生成
    public Boolean getIsCashSubject() {
        return isCashSubject;
    }

    public Boolean getAuxiliaryAccounting() {
        return auxiliaryAccounting;
    }

    public Boolean getQuantityAccounting() {
        return quantityAccounting;
    }

    public Boolean getForeignCurrencyAccounting() {
        return foreignCurrencyAccounting;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 创建人姓名
     */
    @ApiModelProperty(value = "创建人姓名", example = "管理员")
    @JsonProperty("createUserName")
    private String createUserName;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间", example = "2024-01-01 12:00:00")
    @JsonProperty("createTime")
    private String createTime;

    /**
     * 更新人姓名
     */
    @ApiModelProperty(value = "更新人姓名", example = "管理员")
    @JsonProperty("updateUserName")
    private String updateUserName;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间", example = "2024-01-01 12:00:00")
    @JsonProperty("updateTime")
    private String updateTime;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号", example = "0")
    @JsonProperty("version")
    private Integer version;

    /**
     * 子节点
     */
    @ApiModelProperty(value = "子节点")
    @JsonProperty("children")
    private List<FundSubjectListItemResponse> children = new ArrayList<>();
}