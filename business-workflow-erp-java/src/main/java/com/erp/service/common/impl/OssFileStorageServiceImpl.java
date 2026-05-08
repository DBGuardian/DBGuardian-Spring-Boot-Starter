package com.erp.service.common.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.erp.config.FileStorageProperties;
import com.erp.entity.common.File;
import com.erp.service.common.FileStorageService;
import com.erp.util.FileValidator;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * OSS 文件存储服务实现（对象存储，如腾讯云 COS）
 *
 * 说明：
 * 1. 目前主要完成对象键命名规则、FILE 实体字段赋值、URL 生成等“元数据逻辑”；
 * 2. 与实际云厂商 SDK 的对接（真正的文件上传、删除、读取）可在允许引入依赖后补充；
 * 3. 若未正确配置 OSS 参数，本实现会在关键操作时给出明确异常提示，避免静默失败。
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Service("ossFileStorageService")
public class OssFileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    @Autowired
    public OssFileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public File uploadFile(MultipartFile multipartFile, String businessType, Integer businessId) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 基本配置校验
        FileStorageProperties.Oss oss = fileStorageProperties.getOss();
        if (oss == null || StrUtil.isBlank(oss.getBucketName()) || StrUtil.isBlank(oss.getEndpoint())) {
            log.error("OSS 配置不完整，bucketName 或 endpoint 为空，无法上传文件");
            throw new RuntimeException("OSS 配置不完整，请先在配置文件中完善 file.storage.oss.* 参数");
        }

        try {
            // 根据业务类型确定允许的文件类型（这里复用白名单校验逻辑，与本地存储保持一致）
            FileValidator.FileType[] allowedTypes = null;
            FileValidator.ValidationResult validationResult = FileValidator.validate(multipartFile, allowedTypes);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getMessage());
            }

            FileValidator.FileType detectedType = validationResult.getFileType();

            // 原始文件名校验
            String originalFileName = multipartFile.getOriginalFilename();
            if (StrUtil.isBlank(originalFileName)) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            // 计算扩展名（以检测出的文件类型为准）
            String fileExtension = getExtensionFromFileType(detectedType);

            // 生成唯一文件名
            String fileName = IdUtil.fastUUID() + fileExtension;

            // 对象键命名规则：{businessType}/{yyyy/MM/dd}/{fileName}
            if (StrUtil.isBlank(businessType)) {
                businessType = "OTHER";
            }
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = businessType + "/" + datePath + "/" + fileName;

            // 使用腾讯云 COS SDK 上传文件（手动管理 COSClient 生命周期）
            COSClient cosClient = buildCosClient(oss);
            try (InputStream inputStream = multipartFile.getInputStream()) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(multipartFile.getSize());
                metadata.setContentType(multipartFile.getContentType());

                PutObjectRequest putObjectRequest = new PutObjectRequest(oss.getBucketName(), objectKey, inputStream, metadata);
                cosClient.putObject(putObjectRequest);
                log.info("文件已上传至腾讯云 COS，bucketName={}，objectKey={}", oss.getBucketName(), objectKey);
            } finally {
                try {
                    if (cosClient != null) {
                        cosClient.shutdown();
                    }
                } catch (Exception e) {
                    log.warn("关闭 COSClient 失败", e);
                }
            }

            // 构建文件访问 URL
            String fileUrl = buildFileUrl(oss, objectKey);

            // 构建 FILE 实体（仅填充与存储相关字段，其余业务字段由上层 FileService 补齐或保持为空）
            File file = new File();
            file.setFileName(fileName);
            file.setFileType(getFileTypeFromFileType(detectedType));
            file.setFileSize(multipartFile.getSize());
            file.setStorageType("云端");
            file.setBucketName(oss.getBucketName());
            file.setObjectKey(objectKey);
            file.setRegion(oss.getRegion());
            file.setFileUrl(fileUrl);
            // 统一业务模块赋值规则，保持与本地存储实现一致，避免数据库非空约束错误
            file.setBusinessModule(getBusinessModuleFromType(businessType));
            file.setBusinessId(businessId);
            file.setBusinessType(businessType);
            file.setFileStatus("正常");
            file.setUploadTime(java.time.LocalDateTime.now());

            return file;
        } catch (RuntimeException e) {
            // 透传业务/参数异常
            throw e;
        } catch (Exception e) {
            log.error("OSS 文件上传失败", e);
            throw new RuntimeException("OSS 文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return false;
        }
        FileStorageProperties.Oss oss = fileStorageProperties.getOss();
        if (oss == null || StrUtil.isBlank(oss.getBucketName()) || StrUtil.isBlank(oss.getEndpoint())) {
            log.error("OSS 配置不完整，无法删除对象，objectKey={}", filePath);
            return false;
        }

        COSClient cosClient = null;
        try {
            cosClient = buildCosClient(oss);
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(oss.getBucketName(), filePath);
            cosClient.deleteObject(deleteObjectRequest);
            log.info("已从腾讯云 COS 删除对象，bucketName={}，objectKey={}", oss.getBucketName(), filePath);
            return true;
        } catch (Exception e) {
            log.error("删除腾讯云 COS 对象失败，objectKey={}", filePath, e);
            return false;
        } finally {
            if (cosClient != null) {
                try {
                    cosClient.shutdown();
                } catch (Exception ex) {
                    log.warn("关闭 COSClient 失败", ex);
                }
            }
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        FileStorageProperties.Oss oss = fileStorageProperties.getOss();
        if (oss == null || StrUtil.isBlank(oss.getBucketName()) || StrUtil.isBlank(oss.getEndpoint())) {
            log.error("OSS 配置不完整，无法生成文件访问 URL，objectKey={}", filePath);
            return null;
        }
        return buildFileUrl(oss, filePath);
    }

    @Override
    public File uploadBytes(byte[] bytes, String fileName, String businessType, Integer businessId) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        FileStorageProperties.Oss oss = fileStorageProperties.getOss();
        if (oss == null || StrUtil.isBlank(oss.getBucketName()) || StrUtil.isBlank(oss.getEndpoint())) {
            throw new RuntimeException("OSS 配置不完整，请先在配置文件中完善 file.storage.oss.* 参数");
        }
        try {
            String bt = (businessType == null || businessType.isEmpty()) ? "OTHER" : businessType;
            String datePath = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = bt + "/" + datePath + "/" + fileName;

            COSClient cosClient = buildCosClient(oss);
            try {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(bytes.length);
                metadata.setContentType("application/pdf");
                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(bytes);
                PutObjectRequest putObjectRequest = new PutObjectRequest(oss.getBucketName(), objectKey, inputStream, metadata);
                cosClient.putObject(putObjectRequest);
                log.info("字节流文件已上传至腾讯云 COS，objectKey={}", objectKey);
            } finally {
                try { cosClient.shutdown(); } catch (Exception e) { log.warn("关闭 COSClient 失败", e); }
            }

            File file = new File();
            file.setFileName(fileName);
            file.setFileType("PDF");
            file.setFileSize((long) bytes.length);
            file.setStorageType("云端");
            file.setBucketName(oss.getBucketName());
            file.setObjectKey(objectKey);
            file.setRegion(oss.getRegion());
            file.setFileUrl(buildFileUrl(oss, objectKey));
            file.setBusinessModule(getBusinessModuleFromType(bt));
            file.setBusinessId(businessId);
            file.setBusinessType(bt);
            file.setFileStatus("正常");
            file.setUploadTime(java.time.LocalDateTime.now());
            return file;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("OSS 字节流文件上传失败", e);
            throw new RuntimeException("OSS 字节流文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public byte[] readFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        FileStorageProperties.Oss oss = fileStorageProperties.getOss();
        if (oss == null || StrUtil.isBlank(oss.getBucketName()) || StrUtil.isBlank(oss.getEndpoint())) {
            log.error("OSS 配置不完整，无法读取对象，objectKey={}", filePath);
            throw new RuntimeException("OSS 配置不完整，无法读取文件");
        }

        COSClient cosClient = null;
        try {
            cosClient = buildCosClient(oss);
            COSObject cosObject = cosClient.getObject(oss.getBucketName(), filePath);
            try (COSObjectInputStream cosInputStream = cosObject.getObjectContent();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = cosInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("从腾讯云 COS 读取文件失败，objectKey={}", filePath, e);
            throw new RuntimeException("从 OSS 读取文件失败：" + e.getMessage());
        } finally {
            if (cosClient != null) {
                try {
                    cosClient.shutdown();
                } catch (Exception ex) {
                    log.warn("关闭 COSClient 失败", ex);
                }
            }
        }
    }

    /**
     * 根据 FileType 获取文件扩展名
     */
    private String getExtensionFromFileType(FileValidator.FileType fileType) {
        if (fileType == null) {
            return "";
        }
        String extensions = fileType.getExtensions();
        if (extensions.contains(",")) {
            return "." + extensions.split(",")[0];
        }
        return "." + extensions;
    }

    /**
     * 根据 FileType 获取 FILE 表中的文件类型描述
     */
    private String getFileTypeFromFileType(FileValidator.FileType fileType) {
        if (fileType == null) {
            return "其他";
        }
        switch (fileType) {
            case PDF:
                return "PDF";
            case JPEG:
            case PNG:
            case GIF:
            case BMP:
                return "图片";
            case MP4:
            case MOV:
            case AVI:
            case MKV:
            case WMV:
            case FLV:
                return "视频";
            case DOCX:
                return "Word文档";
            case XLSX:
                return "Excel文档";
            case ZIP:
                return "压缩文件";
            default:
                return "其他";
        }
    }

    /**
     * 构建 OSS 文件访问 URL
     * 规则：https://{bucket}.{endpoint}/{objectKey}
     * 若 endpoint 已包含协议前缀，则会自动去掉前缀。
     */
    private String buildFileUrl(FileStorageProperties.Oss oss, String objectKey) {
        String endpoint = oss.getEndpoint();
        if (StrUtil.isBlank(endpoint)) {
            return null;
        }
        // 去掉协议前缀
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            int index = endpoint.indexOf("://");
            endpoint = endpoint.substring(index + 3);
        }
        // 去掉可能的末尾斜杠
        endpoint = StrUtil.removeSuffix(endpoint, "/");
        return "https://" + oss.getBucketName() + "." + endpoint + "/" + objectKey;
    }

    /**
     * 构建腾讯云 COS 客户端
     */
    private COSClient buildCosClient(FileStorageProperties.Oss oss) {
        COSCredentials cred = new BasicCOSCredentials(oss.getAccessKeyId(), oss.getAccessKeySecret());
        Region region = new Region(oss.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(cred, clientConfig);
    }

    /**
     * 根据业务类型获取业务模块
     * 说明：保持与本地存储实现中的映射规则一致，确保 FILE.业务模块 字段始终有合理值
     */
    private String getBusinessModuleFromType(String businessType) {
        if (StrUtil.isBlank(businessType)) {
            return "其他";
        }
        String type = businessType.toLowerCase();
        if (type.contains("employee_registration") || type.contains("employee")) {
            return "员工注册";
        } else if (type.contains("contract")) {
            return "合同";
        } else if (type.contains("reconciliation")) {
            return "对账";
        } else if (type.contains("invoice")) {
            return "发票";
        } else if (type.contains("fund") || type.contains("receipt")) {
            return "财务";
        } else if (type.contains("customer")) {
            return "客户资质";
        } else if (type.contains("weighing_slip") || type.contains("weighing")) {
            return "总磅单";
        } else if (type.contains("warehousing")) {
            return "入库单";
        } else if (type.contains("dispatch_order") || type.contains("dispatch")) {
            return "运输单";
        } else if (type.contains("transport_apply") || type.contains("transport")) {
            return "收运通知单";
        } else {
            return "其他";
        }
    }
}
