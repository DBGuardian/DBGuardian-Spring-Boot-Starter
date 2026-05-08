package com.erp.controller.common;

import cn.hutool.core.util.StrUtil;
import com.erp.common.result.Result;
import com.erp.entity.common.File;
import com.erp.mapper.common.FileMapper;
import com.erp.service.common.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件下载控制器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/file")
@Api(tags = "文件管理")
public class FileController {

    @Value("${file.storage.local.path:D:/erp}")
    private String localStoragePath;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileService fileService;

    /**
     * 获取文件详情
     *
     * @param fileId 文件ID
     * @return 文件详情信息
     */
    @GetMapping("/{fileId}")
    @ApiOperation(value = "获取文件详情", notes = "根据文件ID获取文件详情，包括访问URL")
    public Result<File> getFileDetail(@PathVariable Integer fileId) {
        if (fileId == null) {
            return Result.error("文件ID不能为空");
        }

        File fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            return Result.error("文件不存在");
        }

        // 获取文件的访问URL
        String fileUrl = fileService.getFileUrl(fileId);
        fileEntity.setFileUrl(fileUrl);

        return Result.success("查询成功", fileEntity);
    }

    private ResponseEntity<Resource> proxyRemoteFile(File fileEntity, boolean preview) {
        try {
            if (StrUtil.isBlank(fileEntity.getFileUrl())) {
                log.warn("云端文件URL为空：fileId={}", fileEntity.getFileId());
                return ResponseEntity.notFound().build();
            }

            URL url = new URL(fileEntity.getFileUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                log.warn("云端文件获取失败：fileId={}, statusCode={}, url={}", fileEntity.getFileId(), statusCode, fileEntity.getFileUrl());
                return ResponseEntity.status(statusCode).build();
            }

            String contentType = connection.getContentType();
            if (StrUtil.isBlank(contentType)) {
                String fileName = fileEntity.getFileName() == null ? "" : fileEntity.getFileName().toLowerCase();
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            long contentLength = connection.getContentLengthLong();
            if (contentLength >= 0) {
                headers.setContentLength(contentLength);
            }
            if (preview && "application/pdf".equalsIgnoreCase(contentType)) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileEntity.getFileName() + "\"");
            } else {
                headers.add(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileEntity.getFileName() + "\"");
            }
            setSecurityHeaders(headers, preview);

            InputStream inputStream = connection.getInputStream();
            Resource resource = new org.springframework.core.io.InputStreamResource(inputStream);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            log.error("代理云端文件失败：fileId={}, preview={}", fileEntity.getFileId(), preview, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 设置安全响应头，防止MIME类型嗅探等攻击
     *
     * @param headers HTTP响应头
     * @param isPreview 是否为预览模式（预览模式使用SAMEORIGIN，下载模式使用DENY）
     */
    private void setSecurityHeaders(HttpHeaders headers, boolean isPreview) {
        // X-Content-Type-Options: 防止MIME类型嗅探攻击
        headers.add("X-Content-Type-Options", "nosniff");
        
        // X-Frame-Options: 防止点击劫持攻击
        // 预览模式使用SAMEORIGIN允许同源嵌入，下载模式使用DENY完全禁止
        headers.add("X-Frame-Options", isPreview ? "SAMEORIGIN" : "DENY");
        
        // X-XSS-Protection: XSS保护（虽然已过时，但仍有浏览器支持）
        headers.add("X-XSS-Protection", "1; mode=block");
        
        // Content-Security-Policy: 内容安全策略
        // 对于文件下载/预览，限制只允许同源资源
        if (isPreview) {
            // 预览模式：允许同源嵌入，允许data URI（用于PDF预览等）
            headers.add("Content-Security-Policy", "default-src 'self'; frame-ancestors 'self'");
        } else {
            // 下载模式：完全禁止嵌入
            headers.add("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");
        }
        
        // Referrer-Policy: 控制referrer信息
        headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    /**
     * 下载文件
     *
     * @param path 文件相对路径
     * @return 文件资源
     */
    @GetMapping("/download")
    @ApiOperation(value = "下载文件", notes = "根据文件路径下载文件")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) {
        try {
            if (StrUtil.isBlank(path)) {
                return ResponseEntity.badRequest().build();
            }

            // 构建完整文件路径
            String fullPath = localStoragePath + "/" + path;
            Path filePath = Paths.get(fullPath);
            java.io.File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                log.warn("文件不存在：{}", fullPath);
                return ResponseEntity.notFound().build();
            }

            // 读取文件
            Resource resource = new FileSystemResource(file);

            // 获取文件MIME类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(file.length());
            // 添加安全响应头
            setSecurityHeaders(headers, false);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("文件下载失败：{}", path, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 通过文件ID下载文件
     *
     * @param fileId 文件ID
     * @return 文件资源
     */
    @GetMapping("/download/{fileId}")
    @ApiOperation(value = "通过文件ID下载文件", notes = "根据文件ID下载文件")
    public ResponseEntity<Resource> downloadFileById(@PathVariable Integer fileId) {
        try {
            if (fileId == null) {
                return ResponseEntity.badRequest().build();
            }

            File fileEntity = fileMapper.selectById(fileId);
            if (fileEntity == null) {
                log.warn("文件不存在：fileId={}", fileId);
                return ResponseEntity.notFound().build();
            }

            // 根据存储类型选择文件路径
            String filePath;
            if ("本地".equals(fileEntity.getStorageType())) {
                filePath = fileEntity.getLocalPath();
            } else {
                return proxyRemoteFile(fileEntity, false);
            }

            if (StrUtil.isBlank(filePath)) {
                log.warn("文件路径为空：fileId={}", fileId);
                return ResponseEntity.notFound().build();
            }

            // 构建完整文件路径
            String fullPath = "本地".equals(fileEntity.getStorageType()) 
                    ? localStoragePath + "/" + filePath 
                    : filePath;
            Path pathObj = Paths.get(fullPath);
            java.io.File file = pathObj.toFile();

            if (!file.exists() || !file.isFile()) {
                log.warn("文件不存在：{}", fullPath);
                return ResponseEntity.notFound().build();
            }

            // 读取文件
            Resource resource = new FileSystemResource(file);

            // 获取文件MIME类型
            String contentType = Files.probeContentType(pathObj);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + fileEntity.getFileName() + "\"");
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(file.length());
            // 添加安全响应头
            setSecurityHeaders(headers, false);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("文件下载失败：fileId={}", fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 通过文件ID预览文件
     *
     * @param fileId 文件ID
     * @return 文件资源
     */
    @GetMapping("/preview/{fileId}")
    @ApiOperation(value = "预览文件", notes = "根据文件ID预览文件（PDF等）")
    public ResponseEntity<Resource> previewFileById(@PathVariable Integer fileId) {
        try {
            if (fileId == null) {
                return ResponseEntity.badRequest().build();
            }

            File fileEntity = fileMapper.selectById(fileId);
            if (fileEntity == null) {
                log.warn("文件不存在：fileId={}", fileId);
                return ResponseEntity.notFound().build();
            }

            // 根据存储类型选择文件路径
            String filePath;
            if ("本地".equals(fileEntity.getStorageType())) {
                filePath = fileEntity.getLocalPath();
            } else {
                return proxyRemoteFile(fileEntity, true);
            }

            if (StrUtil.isBlank(filePath)) {
                log.warn("文件路径为空：fileId={}", fileId);
                return ResponseEntity.notFound().build();
            }

            // 构建完整文件路径
            String fullPath = "本地".equals(fileEntity.getStorageType()) 
                    ? localStoragePath + "/" + filePath 
                    : filePath;
            Path pathObj = Paths.get(fullPath);
            java.io.File file = pathObj.toFile();

            if (!file.exists() || !file.isFile()) {
                log.warn("文件不存在：{}", fullPath);
                return ResponseEntity.notFound().build();
            }

            // 读取文件
            Resource resource = new FileSystemResource(file);

            // 获取文件MIME类型
            String contentType = Files.probeContentType(pathObj);
            if (contentType == null) {
                // 根据文件扩展名判断
                String fileName = fileEntity.getFileName().toLowerCase();
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            // 设置响应头（预览模式，不强制下载）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(file.length());
            // 对于PDF，使用inline模式以便在浏览器中预览
            if ("application/pdf".equals(contentType)) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                        "inline; filename=\"" + fileEntity.getFileName() + "\"");
            }
            // 添加安全响应头（预览模式）
            setSecurityHeaders(headers, true);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("文件预览失败：fileId={}", fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 文件上传接口
     *
     * @param file 上传的文件
     * @param businessType 业务类型（可选，默认为 TRANSPORT_APPLY）
     *                     支持的类型：TRANSPORT_APPLY（收运通知单）、WEIGHING_SLIP（总磅单）、CONTRACT（合同）、INVOICE（发票）等
     * @return 文件信息
     */
    @PostMapping("/uploads")
    @ApiOperation(value = "文件上传", notes = "通用文件上传接口，返回文件ID和URL。支持通过businessType参数指定业务类型")
    public Result<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessType", required = false, defaultValue = "TRANSPORT_APPLY") String businessType) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 验证业务类型（如果提供）
            if (StrUtil.isNotBlank(businessType)) {
                // 业务类型验证：允许常见的业务类型
                String[] allowedTypes = {"TRANSPORT_APPLY", "WEIGHING_SLIP", "CONTRACT", "QUOTATION",
                                         "WAREHOUSING", "DISPATCH_ORDER", "CUSTOMER", "INVOICE", "EMPLOYEE_ID_CARD"};
                boolean isValidType = false;
                for (String allowedType : allowedTypes) {
                    if (allowedType.equalsIgnoreCase(businessType)) {
                        isValidType = true;
                        break;
                    }
                }
                if (!isValidType) {
                    log.warn("未知的业务类型：{}，使用默认类型 TRANSPORT_APPLY", businessType);
                    businessType = "TRANSPORT_APPLY";
                }
            } else {
                businessType = "TRANSPORT_APPLY";
            }

            // 使用 FileService 上传文件
            // 业务ID暂时为null，后续在保存业务数据时关联
            File fileEntity = fileService.uploadAndSave(file, businessType, null);

            // 构建返回数据，符合前端期望的格式
            Map<String, String> data = new HashMap<>();
            data.put("fileId", String.valueOf(fileEntity.getFileId()));
            data.put("fileUrl", fileEntity.getFileUrl() != null ? fileEntity.getFileUrl() :
                    "/api/file/download?path=" + fileEntity.getLocalPath());

            return Result.success("文件上传成功", data);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 操作结果
     */
    @DeleteMapping("/{fileId}")
    @ApiOperation(value = "删除文件", notes = "根据文件ID删除文件记录和物理文件")
    public Result<Void> deleteFile(@PathVariable Integer fileId) {
        try {
            if (fileId == null) {
                return Result.error("文件ID不能为空");
            }

            // 文件不存在也返回成功（幂等性）
            fileService.deleteFile(fileId);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除文件失败：fileId={}", fileId, e);
            return Result.error("删除文件失败：" + e.getMessage());
        }
    }
}

