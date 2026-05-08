package com.erp.entity.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报价单实体
 *
 * 对应表：QUOTATION
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("QUOTATION")
public class Quotation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 报价单编号
     */
    @TableId(value = "报价单编号", type = IdType.AUTO)
    private Integer quotationId;

    /**
     * 报价单号（业务可见的报价单编号）
     * 生成规则:QT-YYYYMMDD-XXXXX，其中：
     * - QT：报价单前缀
     * - YYYYMMDD：当前日期（年月日）
     * - XXXXX：5位序号，从00001开始，按日期递增
     * 
     * 说明：
     * - 报价单号由后端自动生成，前端不生成临时编号
     * - 如果前端提供了internalCode或quotationNo，会优先使用（需验证唯一性）
     * - 如果未提供，系统自动按规则生成
     */
    @TableField("报价单号")
    private String quotationNo;

    /**
     * 客户编码
     */
    @TableField("客户编码")
    private Integer customerId;

    /**
     * 客户快照（JSON）
     */
    @TableField("customer_snapshot")
    private String customerSnapshot;

    /**
     * 甲方名称（默认使用客户企业名称，可手动覆盖）
     */
    @TableField("甲方名称")
    private String partyAName;

    /**
     * 甲方联系人
     */
    @TableField("甲方联系人")
    private String partyAContact;

    /**
     * 甲方联系电话
     */
    @TableField("甲方联系电话")
    private String partyAContactPhone;

    /**
     * 甲方统一社会信用代码
     */
    @TableField("甲方统一社会信用代码")
    private String partyACreditCode;

    /**
     * 乙方名称（本公司名称，可编辑）
     */
    @TableField("乙方名称")
    private String partyBName;

    /**
     * 乙方联系人
     */
    @TableField("乙方联系人")
    private String partyBContact;

    /**
     * 乙方联系电话
     */
    @TableField("乙方联系电话")
    private String partyBContactPhone;

    /**
     * 乙方统一社会信用代码
     */
    @TableField("乙方统一社会信用代码")
    private String partyBCreditCode;

    /**
     * 报价状态：待审批/审批中/已通过/已驳回/已失效
     */
    @TableField("报价状态")
    private String quotationStatus;

    /**
     * 审核人编码
     */
    @TableField("审核人编码")
    private Integer auditorId;

    /**
     * 审核人名称
     */
    @TableField("审核人名称")
    private String auditorName;

    /**
     * 审核意见
     */
    @TableField("审核意见")
    private String auditOpinion;

    /**
     * 审核时间
     */
    @TableField("审核时间")
    private LocalDateTime auditTime;

    /**
     * 有效期开始
     */
    @TableField("有效期开始")
    private LocalDateTime validFrom;

    /**
     * 有效期结束
     */
    @TableField("有效期结束")
    private LocalDateTime validTo;

    /**
     * 报价日期
     */
    @TableField("报价日期")
    private LocalDate quotationDate;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    /**
     * 创建人编码
     */
    @TableField("创建人编码")
    private Integer creatorId;
}



