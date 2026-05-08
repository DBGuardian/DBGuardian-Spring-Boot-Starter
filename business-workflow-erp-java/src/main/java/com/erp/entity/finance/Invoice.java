package com.erp.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.entity.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票实体类
 * 
 * 对应表：INVOICE
 * 
 * @author ERP System
 * @date 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("INVOICE")
public class Invoice extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 发票编号
     */
    @TableId(value = "发票编号", type = IdType.AUTO)
    private Integer invoiceId;

    /**
     * 发票类型
     * 增值税专用发票/普通发票
     */
    @TableField("发票类型")
    private String invoiceType;

    /**
     * 发票形式
     * 数电发票/电子发票/纸质发票
     */
    @TableField("发票形式")
    private String invoiceForm;

    /**
     * 发票性质
     * 红字/蓝字
     */
    @TableField("发票性质")
    private String invoiceNature;

    /**
     * 发票状态
     * 进项发票/销项发票
     */
    @TableField("发票状态")
    private String invoiceStatus;

    /**
     * 金额
     * 发票金额（元）
     */
    @TableField("金额")
    private BigDecimal amount;

    /**
     * 税额
     * 税额（元）
     */
    @TableField("税额")
    private BigDecimal taxAmount;

    /**
     * 价税合计
     * 价税合计（元），金额+税额
     */
    @TableField("价税合计")
    private BigDecimal totalAmount;

    /**
     * 价税合计大写
     */
    @TableField("价税合计大写")
    private String totalAmountInChinese;

    /**
     * 不含税金额
     * 不含税金额（元）
     */
    @TableField("不含税金额")
    private BigDecimal taxExcludedAmount;

    /**
     * 发票号码
     * 发票号码，唯一标识
     */
    @TableField("发票号码")
    private String invoiceNumber;

    /**
     * 发票代码
     */
    @TableField("发票代码")
    private String invoiceCode;

    /**
     * 开票日期
     */
    @TableField("开票日期")
    private LocalDateTime invoiceDate;

    // ========== 购买方信息（销项发票时为客户信息） ==========

    /**
     * 购买方名称
     * 购买方名称（销项发票时为客户名称）
     */
    @TableField("购买方名称")
    private String buyerName;

    /**
     * 购买方统一社会信用代码
     */
    @TableField("购买方统一社会信用代码")
    private String buyerCreditCode;

    /**
     * 购买方地址
     */
    @TableField("购买方地址")
    private String buyerAddress;

    /**
     * 购买方电话
     */
    @TableField("购买方电话")
    private String buyerPhone;

    /**
     * 购买方开户行
     */
    @TableField("购买方开户行")
    private String buyerBankName;

    /**
     * 购买方账号
     */
    @TableField("购买方账号")
    private String buyerBankAccount;

    // ========== 销售方信息（进项发票时为本企业信息） ==========

    /**
     * 销售方名称
     * 销售方名称（进项发票时为本企业名称）
     */
    @TableField("销售方名称")
    private String sellerName;

    /**
     * 销售方统一社会信用代码
     */
    @TableField("销售方统一社会信用代码")
    private String sellerCreditCode;

    /**
     * 销售方地址
     */
    @TableField("销售方地址")
    private String sellerAddress;

    /**
     * 销售方电话
     */
    @TableField("销售方电话")
    private String sellerPhone;

    /**
     * 销售方开户行
     */
    @TableField("销售方开户行")
    private String sellerBankName;

    /**
     * 销售方账号
     */
    @TableField("销售方账号")
    private String sellerBankAccount;

    /**
     * 录入发票明细
     * 是否录入发票明细：TRUE-录入明细，FALSE-不录入明细
     */
    @TableField("录入发票明细")
    private Boolean recordDetails;

    /**
     * 发票扫描件PDF文件编号
     * 关联FILE表
     */
    @TableField("发票扫描件PDF文件编号")
    private Integer pdfFileId;

    /**
     * 发票扫描件图片文件编号
     * 纸质发票用，支持JPG/PNG格式，关联FILE表
     */
    @TableField("发票扫描件图片文件编号")
    private Integer imageFileId;

    /**
     * 开票人名称
     * 开具发票的财务人员名称
     */
    @TableField("开票人名称")
    private String issuerName;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;

    // ========== 锁定字段：合同完结后锁定，不可篡改 ==========

    /**
     * 是否锁定
     * 是否锁定：合同完结后自动锁定，锁定后不可修改、不可删除
     */
    @TableField("是否锁定")
    private Boolean isLocked;

    /**
     * 锁定时间
     * 锁定时间，合同完结时自动设置
     */
    @TableField("锁定时间")
    private LocalDateTime lockTime;

    /**
     * 锁定人编码
     * 锁定人编码，系统自动锁定或管理员锁定，关联EMPLOYEE表
     */
    @TableField("锁定人编码")
    private Integer lockerId;

    /**
     * 锁定原因
     * 锁定原因，如：合同已完结/合同已归档
     */
    @TableField("锁定原因")
    private String lockReason;

    /**
     * 创建人编码
     * 创建人编码，关联EMPLOYEE表
     */
    @TableField("创建人编码")
    private Integer creatorId;
}
