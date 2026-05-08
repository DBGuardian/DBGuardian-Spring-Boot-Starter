package com.erp.entity.common;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件实体类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("FILE")
public class File extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 文件编号
     */
    @TableId(type = IdType.AUTO, value = "文件编号")
    private Integer fileId;

    /**
     * 文件名称
     */
    @TableField("文件名称")
    private String fileName;

    /**
     * 文件类型：PDF/图片/其他
     */
    @TableField("文件类型")
    private String fileType;

    /**
     * 文件大小（字节）
     */
    @TableField("文件大小")
    private Long fileSize;

    /**
     * 存储类型：本地/云端（腾讯云COS）
     */
    @TableField("存储类型")
    private String storageType;

    /**
     * 本地存储路径（存储类型为"本地"时使用）
     */
    @TableField("本地存储路径")
    private String localPath;

    /**
     * 存储桶名称（Bucket），存储类型为"云端"时使用
     */
    @TableField("存储桶名称")
    private String bucketName;

    /**
     * 对象键（Object Key），存储类型为"云端"时使用
     */
    @TableField("对象键")
    private String objectKey;

    /**
     * 存储区域（Region），如：ap-guangzhou（广州）
     */
    @TableField("存储区域")
    private String region;

    /**
     * 文件访问URL
     */
    @TableField("访问URL")
    private String fileUrl;

    /**
     * 访问密钥ID（SecretId），用于生成临时访问URL（可选）
     */
    @TableField("访问密钥ID")
    private String accessKeyId;

    /**
     * 业务模块：合同/对账/发票/客户资质/其他
     */
    @TableField("业务模块")
    private String businessModule;

    /**
     * 关联业务ID，如：合同编号、对账单编号、发票编号、客户编码等
     */
    @TableField("关联业务ID")
    private Integer businessId;

    /**
     * 关联业务类型：CONTRACT/RECONCILIATION/INVOICE/CUSTOMER_QUALIFICATION/OTHER
     */
    @TableField("关联业务类型")
    private String businessType;

    /**
     * 文件MD5值，用于文件完整性校验
     */
    @TableField("文件MD5")
    private String fileMd5;

    /**
     * 文件SHA256值，用于文件完整性校验和防篡改
     */
    @TableField("文件SHA256")
    private String fileSha256;

    /**
     * 文件上传时间
     */
    @TableField("上传时间")
    private java.time.LocalDateTime uploadTime;

    /**
     * 上传人编码，关联EMPLOYEE表
     */
    @TableField("上传人编码")
    private Integer uploaderId;

    /**
     * 文件状态：正常/已删除/已归档
     */
    @TableField("文件状态")
    private String fileStatus;

    /**
     * 删除时间（软删除）
     */
    @TableField("删除时间")
    private java.time.LocalDateTime deleteTime;

    /**
     * 备注
     */
    @TableField("备注")
    private String remark;
}

