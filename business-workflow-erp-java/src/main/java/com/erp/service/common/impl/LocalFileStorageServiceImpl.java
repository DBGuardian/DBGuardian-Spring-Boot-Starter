package com.erp.service.common.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.erp.config.FileStorageProperties;
import com.erp.entity.common.File;
import com.erp.service.common.FileStorageService;
import com.erp.util.FileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地文件存储服务实现
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Service("localFileStorageService")
public class LocalFileStorageServiceImpl implements FileStorageService {

    /**
     * 文件存储配置
     */
    private final FileStorageProperties fileStorageProperties;

    @Autowired
    public LocalFileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    /**
     * 获取本地存储根路径，未配置时回退到默认 D:/erp
     */
    private String getLocalStoragePath() {
        String path = fileStorageProperties.getLocal() != null ? fileStorageProperties.getLocal().getPath() : null;
        if (StrUtil.isBlank(path)) {
            path = "D:/erp";
        }
        return path;
    }

    @Override
    public File uploadFile(MultipartFile multipartFile, String businessType, Integer businessId) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            // 根据业务类型确定允许的文件类型
            FileValidator.FileType[] allowedTypes = getAllowedFileTypes(businessType);
            
            // 验证文件（包括扩展名、MIME类型和文件头）
            FileValidator.ValidationResult validationResult = FileValidator.validate(multipartFile, allowedTypes);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getMessage());
            }

            FileValidator.FileType detectedType = validationResult.getFileType();
            
            // 获取原始文件名
            String originalFileName = multipartFile.getOriginalFilename();
            if (StrUtil.isBlank(originalFileName)) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            // 获取文件扩展名（使用验证后的类型）
            String fileExtension = getExtensionFromFileType(detectedType);

            // 生成唯一文件名
            String fileName = IdUtil.fastUUID() + fileExtension;

            // 构建文件存储路径：{localRoot}/{businessType}/{yyyy/MM/dd}/{fileName}
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = businessType + "/" + datePath + "/" + fileName;
            String fullPath = getLocalStoragePath() + "/" + relativePath;

            // 创建目录
            Path filePath = Paths.get(fullPath);
            Files.createDirectories(filePath.getParent());

            // 保存文件
            multipartFile.transferTo(filePath.toFile());

            // 构建文件实体
            File file = new File();
            file.setFileName(fileName);
            file.setFileType(getFileTypeFromFileType(detectedType));
            file.setFileSize(multipartFile.getSize());
            file.setStorageType("本地");
            file.setLocalPath(relativePath);
            file.setFileUrl("/api/file/download?path=" + relativePath);
            file.setBusinessModule(getBusinessModuleFromType(businessType));
            file.setBusinessId(businessId);
            file.setBusinessType(businessType);
            file.setFileStatus("正常");
            file.setUploadTime(java.time.LocalDateTime.now());

            log.info("文件上传成功：{}", fullPath);
            return file;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return false;
        }

        try {
            // 规范化并验证文件路径，防止路径遍历攻击
            Path normalizedPath = normalizeAndValidatePath(filePath);
            if (normalizedPath == null) {
                log.warn("文件路径不安全或无效：{}", filePath);
                return false;
            }
            
            java.io.File file = normalizedPath.toFile();
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("文件删除成功：{}", normalizedPath);
                }
                return deleted;
            }
            return false;
        } catch (Exception e) {
            log.error("文件删除失败：{}", filePath, e);
            return false;
        }
    }

    /**
     * 规范化并验证文件路径，防止路径遍历攻击
     *
     * @param relativePath 相对路径
     * @return 规范化后的完整路径，如果路径不安全则返回null
     */
    private Path normalizeAndValidatePath(String relativePath) {
        if (StrUtil.isBlank(relativePath)) {
            return null;
        }

        try {
            // 规范化存储根目录路径
            Path basePath = Paths.get(getLocalStoragePath()).normalize().toAbsolutePath();
            
            // 规范化相对路径
            Path relativePathObj = Paths.get(relativePath).normalize();
            
            // 检查相对路径是否包含危险字符
            String normalizedRelative = relativePathObj.toString();
            // 检查是否包含路径遍历字符或绝对路径
            if (normalizedRelative.contains("..") || 
                normalizedRelative.startsWith("/") || 
                normalizedRelative.startsWith("\\") ||
                (normalizedRelative.length() > 1 && normalizedRelative.charAt(1) == ':')) {
                log.warn("检测到不安全的路径：{}", relativePath);
                return null;
            }
            
            // 构建完整路径并规范化
            Path fullPath = basePath.resolve(relativePathObj).normalize();
            
            // 验证最终路径是否在允许的存储目录范围内
            Path absoluteFullPath = fullPath.toAbsolutePath();
            Path absoluteBasePath = basePath.toAbsolutePath();
            
            if (!absoluteFullPath.startsWith(absoluteBasePath)) {
                log.warn("路径超出允许范围：{} (规范化后: {})", relativePath, absoluteFullPath);
                return null;
            }
            
            return absoluteFullPath;
        } catch (Exception e) {
            log.error("路径规范化失败：{}", relativePath, e);
            return null;
        }
    }

    @Override
    public String getFileUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        return "/api/file/download?path=" + filePath;
    }

    @Override
    public File uploadBytes(byte[] bytes, String fileName, String businessType, Integer businessId) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String bt = (businessType == null || businessType.isEmpty()) ? "OTHER" : businessType;
            String relativePath = bt + "/" + datePath + "/" + fileName;
            String fullPath = getLocalStoragePath() + "/" + relativePath;
            Path filePath2 = Paths.get(fullPath);
            Files.createDirectories(filePath2.getParent());
            Files.write(filePath2, bytes);

            File file = new File();
            file.setFileName(fileName);
            file.setFileType("PDF");
            file.setFileSize((long) bytes.length);
            file.setStorageType("本地");
            file.setLocalPath(relativePath);
            file.setFileUrl("/api/file/download?path=" + relativePath);
            file.setBusinessModule(getBusinessModuleFromType(bt));
            file.setBusinessId(businessId);
            file.setBusinessType(bt);
            file.setFileStatus("正常");
            file.setUploadTime(java.time.LocalDateTime.now());
            log.info("字节流文件保存成功：{}", fullPath);
            return file;
        } catch (IOException e) {
            log.error("字节流文件保存失败", e);
            throw new RuntimeException("字节流文件保存失败：" + e.getMessage());
        }
    }

    @Override
    public byte[] readFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        try {
            // 规范化并验证文件路径
            Path normalizedPath = normalizeAndValidatePath(filePath);
            if (normalizedPath == null) {
                throw new IllegalArgumentException("文件路径不安全或无效：" + filePath);
            }

            java.io.File file = normalizedPath.toFile();
            if (!file.exists()) {
                throw new IllegalArgumentException("文件不存在：" + filePath);
            }

            if (!file.isFile()) {
                throw new IllegalArgumentException("路径不是文件：" + filePath);
            }

            // 读取文件内容
            return Files.readAllBytes(normalizedPath);
        } catch (IOException e) {
            log.error("读取文件失败：{}", filePath, e);
            throw new RuntimeException("读取文件失败：" + e.getMessage());
        }
    }

    /**
     * 根据业务类型获取允许的文件类型
     */
    private FileValidator.FileType[] getAllowedFileTypes(String businessType) {
        if (StrUtil.isBlank(businessType)) {
            // 默认允许所有白名单类型
            return null;
        }
        
        String type = businessType.toUpperCase();
        if (type.contains("CONTRACT")) {
            // 合同只允许PDF
            return new FileValidator.FileType[]{FileValidator.FileType.PDF};
        } else if (type.contains("EMPLOYEE") || type.contains("REGISTRATION")) {
            // 员工注册只允许图片
            return new FileValidator.FileType[]{
                FileValidator.FileType.JPEG,
                FileValidator.FileType.PNG,
                FileValidator.FileType.GIF,
                FileValidator.FileType.BMP
            };
        } else if (type.contains("CUSTOMER")) {
            // 客户资质允许图片和PDF
            return new FileValidator.FileType[]{
                FileValidator.FileType.PDF,
                FileValidator.FileType.JPEG,
                FileValidator.FileType.PNG,
                FileValidator.FileType.GIF,
                FileValidator.FileType.BMP
            };
        } else if (type.contains("TRANSPORT_APPLY") || type.contains("TRANSPORT")) {
            // 收运通知单允许图片和视频
            return new FileValidator.FileType[]{
                FileValidator.FileType.JPEG,
                FileValidator.FileType.PNG,
                FileValidator.FileType.GIF,
                FileValidator.FileType.BMP,
                FileValidator.FileType.MP4,
                FileValidator.FileType.MOV,
                FileValidator.FileType.AVI,
                FileValidator.FileType.MKV,
                FileValidator.FileType.WMV,
                FileValidator.FileType.FLV
            };
        } else if (type.contains("INVOICE") || businessType.contains("发票")) {
            // 发票：允许PDF、OFD、XML以及常见图片格式
            return new FileValidator.FileType[]{
                FileValidator.FileType.PDF,
                FileValidator.FileType.OFD,
                FileValidator.FileType.XML,
                FileValidator.FileType.JPEG,
                FileValidator.FileType.PNG
            };
        }
        
        // 默认允许所有白名单类型
        return null;
    }

    /**
     * 根据FileType获取文件扩展名
     */
    private String getExtensionFromFileType(FileValidator.FileType fileType) {
        if (fileType == null) {
            return "";
        }
        String extensions = fileType.getExtensions();
        // 取第一个扩展名
        if (extensions.contains(",")) {
            return "." + extensions.split(",")[0];
        }
        return "." + extensions;
    }

    /**
     * 根据FileType获取文件类型描述
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
     * 根据业务类型获取业务模块
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

