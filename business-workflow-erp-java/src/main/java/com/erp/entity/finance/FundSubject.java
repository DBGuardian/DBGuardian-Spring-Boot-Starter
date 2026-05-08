package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 会计科目实体
 *
 * 对应表：FUND_SUBJECT
 *
 * 字段说明参考《科目管理设计方案》文档：
 * - 科目编号：主键，自增
 * - 科目编码：业务编号，如：1001、1002等
 * - 科目名称：如"库存现金"、"银行存款"等
 * - 科目类别：流动资产、非流动资产
 * - 余额方向：收入、支出
 * - 是否为现金科目：1-是，0-否
 * - 辅助核算：1-启用，0-不启用
 * - 数量核算：1-启用，0-不启用
 * - 外币核算：1-启用，0-不启用
 * - 是否启用：1-启用，0-停用
 * - 备注：补充说明
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("FUND_SUBJECT")
public class FundSubject extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科目编号（主键，自增）
     */
    @TableId(value = "科目编号", type = IdType.AUTO)
    private Long subjectId;

    /**
     * 科目编码（业务编号，如：1001、1002等）
     */
    @TableField("科目编码")
    private String subjectCode;

    /**
     * 科目名称（如：库存现金、银行存款等）
     */
    @TableField("科目名称")
    private String subjectName;

    /**
     * 上级科目编码
     */
    @TableField("上级科目编码")
    private String parentSubjectCode;

    /**
     * 科目级次
     */
    @TableField("科目级次")
    private Integer subjectLevel;

    /**
     * 全科目名称
     */
    @TableField("全科目名称")
    private String fullSubjectName;

    /**
     * 科目类别：流动资产、非流动资产
     */
    @TableField("科目类别")
    private String subjectCategory;

    /**
     * 余额方向：收入、支出
     */
    @TableField("余额方向")
    private String balanceDirection;

    /**
     * 是否为现金科目（1-是，0-否）
     */
    @TableField("是否为现金科目")
    private Boolean isCashSubject;

    /**
     * 辅助核算（1-启用，0-不启用）
     */
    @TableField("辅助核算")
    private Boolean auxiliaryAccounting;

    /**
     * 数量核算（1-启用，0-不启用）
     */
    @TableField("数量核算")
    private Boolean quantityAccounting;

    /**
     * 外币核算（1-启用，0-不启用）
     */
    @TableField("外币核算")
    private Boolean foreignCurrencyAccounting;

    /**
     * 是否启用（1-启用，0-停用）
     */
    @TableField("是否启用")
    private Boolean enabled;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer createUserId;

    /**
     * 更新人编码
     */
    @TableField("更新人编码")
    private Integer updateUserId;

    /**
     * 创建人姓名
     */
    @TableField("创建人")
    private String createUserName;

    /**
     * 更新人姓名
     */
    @TableField("更新人")
    private String updateUserName;
}