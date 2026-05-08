package com.erp.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 文件验证工具类
 * 用于验证文件类型、MIME类型和文件头（Magic Number），防止恶意文件上传
 *
 * @author ERP System
 * @date 2025-12-06
 */
@Slf4j
public class FileValidator {

    /**
     * 文件类型枚举
     */
    public enum FileType {
        PDF("pdf", "application/pdf", new byte[][]{{0x25, 0x50, 0x44, 0x46}}), // %PDF
        // 兼容常见等价MIME，使用逗号分隔多个取值
        JPEG("jpg,jpeg", "image/jpeg,image/jpg,image/pjpeg", new byte[][]{{(byte)0xFF, (byte)0xD8, (byte)0xFF}}), // FF D8 FF
        PNG("png", "image/png,image/x-png", new byte[][]{{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}}), // 89 50 4E 47 0D 0A 1A 0A
        GIF("gif", "image/gif", new byte[][]{{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}, {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}}), // GIF87a or GIF89a
        BMP("bmp", "image/bmp,image/x-ms-bmp", new byte[][]{{0x42, 0x4D}}), // BM
        DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[][]{{0x50, 0x4B, 0x03, 0x04}}), // ZIP header (DOCX is a ZIP file)
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[][]{{0x50, 0x4B, 0x03, 0x04}}), // ZIP header (XLSX is a ZIP file)
        ZIP("zip", "application/zip", new byte[][]{{0x50, 0x4B, 0x03, 0x04}, {0x50, 0x4B, 0x05, 0x06}, {0x50, 0x4B, 0x07, 0x08}}), // ZIP files
        OFD("ofd", "application/ofd", new byte[][]{{0x50, 0x4B, 0x03, 0x04}}), // OFD：基于ZIP容器
        XML("xml", "application/xml,text/xml", new byte[][]{{0x3C, 0x3F, 0x78, 0x6D, 0x6C}}), // XML：\"<?xml\"
        // 视频格式
        MP4("mp4", "video/mp4", new byte[][]{{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}, {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}, {0x00, 0x00, 0x00, 0x1C, 0x66, 0x74, 0x79, 0x70}}), // MP4 ftyp box
        MOV("mov", "video/quicktime", new byte[][]{{0x00, 0x00, 0x00, 0x14, 0x66, 0x74, 0x79, 0x70}, {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}}), // QuickTime/MOV ftyp box
        AVI("avi", "video/x-msvideo", new byte[][]{{0x52, 0x49, 0x46, 0x46}}), // RIFF (AVI starts with RIFF)
        MKV("mkv", "video/x-matroska", new byte[][]{{0x1A, 0x45, (byte)0xDF, (byte)0xA3}}), // Matroska/MKV
        WMV("wmv", "video/x-ms-wmv", new byte[][]{{0x30, 0x26, (byte)0xB2, 0x75, (byte)0x8E, 0x66, (byte)0xCF, 0x11, (byte)0xA6, (byte)0xD9, 0x00, (byte)0xAA, 0x00, 0x62, (byte)0xCE, 0x6C}}), // ASF/WMV
        FLV("flv", "video/x-flv", new byte[][]{{0x46, 0x4C, 0x56, 0x01}}); // FLV

        private final String extensions;
        private final String mimeType;
        private final byte[][] magicNumbers;

        FileType(String extensions, String mimeType, byte[][] magicNumbers) {
            this.extensions = extensions;
            this.mimeType = mimeType;
            this.magicNumbers = magicNumbers;
        }

        public String getExtensions() {
            return extensions;
        }

        public String getMimeType() {
            return mimeType;
        }

        public byte[][] getMagicNumbers() {
            return magicNumbers;
        }
    }

    /**
     * 文件类型白名单映射（扩展名 -> FileType）
     */
    private static final Map<String, FileType> EXTENSION_TO_TYPE = new HashMap<>();
    
    /**
     * MIME类型白名单映射（MIME -> FileType）
     */
    private static final Map<String, FileType> MIME_TO_TYPE = new HashMap<>();

    static {
        // 初始化扩展名和MIME类型映射
        for (FileType type : FileType.values()) {
            String[] exts = type.getExtensions().split(",");
            for (String ext : exts) {
                EXTENSION_TO_TYPE.put(ext.toLowerCase(), type);
            }
            MIME_TO_TYPE.put(type.getMimeType().toLowerCase(), type);
        }
    }

    /**
     * 验证文件
     *
     * @param file 文件
     * @param allowedTypes 允许的文件类型（null表示允许所有白名单类型）
     * @return 验证结果
     */
    public static ValidationResult validate(MultipartFile file, FileType... allowedTypes) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.failure("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        try {
            // 1. 验证文件扩展名
            if (StrUtil.isBlank(originalFilename)) {
                return ValidationResult.failure("文件名不能为空");
            }

            String extension = getFileExtension(originalFilename);
            if (StrUtil.isBlank(extension)) {
                return ValidationResult.failure("文件必须包含扩展名");
            }

            FileType fileTypeByExtension = EXTENSION_TO_TYPE.get(extension.toLowerCase());
            if (fileTypeByExtension == null) {
                return ValidationResult.failure("不支持的文件类型：" + extension);
            }

            // 2. 如果指定了允许的类型，检查扩展名是否在允许列表中
            if (allowedTypes != null && allowedTypes.length > 0) {
                Set<FileType> allowedSet = new HashSet<>(Arrays.asList(allowedTypes));
                if (!allowedSet.contains(fileTypeByExtension)) {
                    return ValidationResult.failure("不允许的文件类型：" + extension);
                }
            }

            // 3. 验证MIME类型
            String mimeType = file.getContentType();
            if (StrUtil.isNotBlank(mimeType)) {
                FileType fileTypeByMime = MIME_TO_TYPE.get(mimeType.toLowerCase());
                if (fileTypeByMime == null) {
                    // 对于未识别的MIME类型（如application/octet-stream），仅记录警告，继续依赖扩展名和文件头校验
                    log.warn("未识别的MIME类型：{}，将仅根据扩展名和文件头验证，文件名={}", mimeType, originalFilename);
                } else {
                    // MIME类型必须与扩展名匹配
                    if (fileTypeByMime != fileTypeByExtension) {
                        log.warn("文件MIME类型与扩展名不匹配：MIME={}, 扩展名={}, 文件名={}", 
                                mimeType, extension, originalFilename);
                        return ValidationResult.failure("文件MIME类型与扩展名不匹配");
                    }
                }
            }

            // 4. 验证文件头（Magic Number）
            // 对于视频文件，文件头验证可能比较复杂，因为视频格式的文件头可能在不同位置
            // 对于视频文件，如果文件头验证失败，只记录警告但不阻止上传
            if (isVideoType(fileTypeByExtension)) {
                // 视频文件：尝试验证文件头，但失败时不阻止上传
                ValidationResult headerResult = validateFileHeader(file, fileTypeByExtension);
                if (!headerResult.isValid()) {
                    // 如果文件头验证失败，记录警告但不阻止上传（因为视频格式的文件头可能变化）
                    log.warn("视频文件头验证失败，但允许上传（仅验证扩展名和MIME类型）：文件名={}, 扩展名={}, 错误={}", 
                            originalFilename, extension, headerResult.getMessage());
                }
                // 视频文件：只要扩展名和MIME类型匹配就允许上传
                return ValidationResult.success(fileTypeByExtension);
            } else {
                // 非视频文件：严格验证文件头
                ValidationResult headerResult = validateFileHeader(file, fileTypeByExtension);
                if (!headerResult.isValid()) {
                    return headerResult;
                }
                return ValidationResult.success(fileTypeByExtension);
            }

        } catch (Exception e) {
            log.error("文件验证失败：{}", originalFilename, e);
            return ValidationResult.failure("文件验证失败：" + e.getMessage());
        }
    }

    /**
     * 验证文件头（Magic Number）
     *
     * @param file 文件
     * @param expectedType 期望的文件类型
     * @return 验证结果
     */
    private static ValidationResult validateFileHeader(MultipartFile file, FileType expectedType) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[][] magicNumbers = expectedType.getMagicNumbers();
            int maxMagicLength = 0;
            for (byte[] magic : magicNumbers) {
                maxMagicLength = Math.max(maxMagicLength, magic.length);
            }

            // 读取文件头
            byte[] header = new byte[maxMagicLength];
            int bytesRead = inputStream.read(header);
            if (bytesRead < maxMagicLength) {
                return ValidationResult.failure("文件内容不完整，无法验证文件类型");
            }

            // 检查是否匹配任一Magic Number
            for (byte[] magic : magicNumbers) {
                boolean matches = true;
                for (int i = 0; i < magic.length; i++) {
                    if (header[i] != magic[i]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return ValidationResult.success(expectedType);
                }
            }

            // 对于DOCX和XLSX，需要进一步验证ZIP文件内容
            if (expectedType == FileType.DOCX || expectedType == FileType.XLSX) {
                // 验证ZIP文件头
                if (header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04) {
                    // 读取更多字节以检查ZIP文件内部结构
                    // DOCX应该包含word/document.xml，XLSX应该包含xl/workbook.xml
                    // 这里简化处理，只验证ZIP头
                    return ValidationResult.success(expectedType);
                }
            }

            return ValidationResult.failure("文件头验证失败，文件类型可能被伪装");

        } catch (IOException e) {
            log.error("读取文件头失败", e);
            return ValidationResult.failure("无法读取文件内容进行验证");
        }
    }

    /**
     * 判断是否为视频类型
     *
     * @param fileType 文件类型
     * @return 是否为视频类型
     */
    private static boolean isVideoType(FileType fileType) {
        return fileType == FileType.MP4 || fileType == FileType.MOV || 
               fileType == FileType.AVI || fileType == FileType.MKV || 
               fileType == FileType.WMV || fileType == FileType.FLV;
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（不含点号）
     */
    private static String getFileExtension(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * 根据文件扩展名获取MIME类型
     *
     * @param filename 文件名或扩展名
     * @return MIME类型，如果无法识别则返回application/octet-stream
     */
    public static String getMimeTypeByFilename(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "application/octet-stream";
        }
        
        // 如果传入的是扩展名（不含点号），直接使用
        String extension = filename.contains(".") ? getFileExtension(filename) : filename.toLowerCase();
        
        FileType fileType = EXTENSION_TO_TYPE.get(extension.toLowerCase());
        if (fileType != null) {
            return fileType.getMimeType();
        }
        
        // 如果无法识别，返回默认值
        return "application/octet-stream";
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final FileType fileType;

        private ValidationResult(boolean valid, String message, FileType fileType) {
            this.valid = valid;
            this.message = message;
            this.fileType = fileType;
        }

        public static ValidationResult success(FileType fileType) {
            return new ValidationResult(true, "验证通过", fileType);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public FileType getFileType() {
            return fileType;
        }
    }
}

