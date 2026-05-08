package com.erp.service.common;

import com.erp.config.FileStorageProperties;
import com.erp.entity.common.File;
import com.erp.mapper.common.FileMapper;
import com.erp.service.common.impl.LocalFileStorageServiceImpl;
import com.erp.service.common.impl.OssFileStorageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件服务
 *
 * 负责统一路由到本地或 OSS 存储实现，并维护 FILE 表记录。
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Service
public class FileService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    @Qualifier("localFileStorageService")
    private LocalFileStorageServiceImpl localFileStorageService;

    @Autowired(required = false)
    @Qualifier("ossFileStorageService")
    private OssFileStorageServiceImpl ossFileStorageService;

    @Autowired
    private FileStorageProperties fileStorageProperties;

    /**
     * 上传文件并保存到数据库
     *
     * @param multipartFile 文件
     * @param businessType  业务类型
     * @param businessId    业务ID
     * @return 文件信息
     */
    @Transactional(rollbackFor = Exception.class)
    public File uploadAndSave(MultipartFile multipartFile, String businessType, Integer businessId) {
        // 根据全局存储类型选择存储服务
        FileStorageService storageService = getStorageService();

        // 上传文件
        File file = storageService.uploadFile(multipartFile, businessType, businessId);

        // 保存到数据库
        if (file.getUploadTime() == null) {
            file.setUploadTime(LocalDateTime.now());
        }
        file.setCreateTime(LocalDateTime.now());
        file.setUpdateTime(LocalDateTime.now());
        fileMapper.insert(file);

        log.info("文件上传并保存成功：fileId={}, fileName={}", file.getFileId(), file.getFileName());
        return file;
    }

    /**
     * 根据业务类型和业务ID查询文件列表
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 文件列表
     */
    public List<File> getFilesByBusiness(String businessType, Integer businessId) {
        return fileMapper.selectByBusinessTypeAndId(businessType, businessId);
    }

    /**
     * 根据文件ID获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件信息，如果不存在则返回null
     */
    public File getFileById(Integer fileId) {
        if (fileId == null) {
            return null;
        }
        return fileMapper.selectById(fileId);
    }

    /**
     * 删除文件（包括物理文件和数据库记录）
     *
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Integer fileId) {
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            return false;
        }

        // 根据 FILE 表中的存储类型选择具体实现，而不是全局配置
        FileStorageService storageService;
        String filePath;
        if ("本地".equals(file.getStorageType())) {
            storageService = localFileStorageService;
            filePath = file.getLocalPath();
        } else {
            // 视为云端存储
            storageService = ossFileStorageService != null ? ossFileStorageService : localFileStorageService;
            filePath = file.getObjectKey();
        }

        boolean deleted = storageService.deleteFile(filePath);

        // 删除数据库记录
        if (deleted) {
            fileMapper.deleteById(fileId);
            log.info("文件删除成功：fileId={}", fileId);
        }

        return deleted;
    }

    /**
     * 根据全局存储类型获取存储服务
     *
     * @return 存储服务
     */
    public FileStorageService getStorageService() {
        String type = fileStorageProperties.getType();
        if ("oss".equalsIgnoreCase(type) && ossFileStorageService != null) {
            return ossFileStorageService;
        }
        if (!"local".equalsIgnoreCase(type)) {
            log.warn("未知的文件存储类型：{}，已回退使用本地存储实现", type);
        }
        return localFileStorageService;
    }

    /**
     * 获取本地文件存储服务实例
     * 用于需要根据 FILE 表中 storageType 字段明确选择本地存储的场景（如导出ZIP时读取本地文件）
     *
     * @return 本地存储服务
     */
    public FileStorageService getLocalFileStorageService() {
        return localFileStorageService;
    }

    /**
     * 获取 OSS 文件存储服务实例
     * 用于需要根据 FILE 表中 storageType 字段明确选择 OSS 存储的场景（如导出ZIP时读取云端文件）
     *
     * @return OSS 存储服务，若未配置则返回 null
     */
    public FileStorageService getOssFileStorageService() {
        return ossFileStorageService;
    }

    /**
     * 将内存中的 byte[] 文件保存到存储介质并写入 FILE 表
     * 用于无法构造 MultipartFile 的场景（如 PDF 拆分后的子文件）
     *
     * @param bytes        文件字节数组
     * @param fileName     文件名（含扩展名）
     * @param businessType 业务类型（如 TRANSFER_MANIFEST）
     * @param businessId   关联业务 ID
     * @param uploaderId   上传人编码
     * @return 保存后的 File 实体（含自增 fileId）
     */
    @Transactional(rollbackFor = Exception.class)
    public File uploadBytesAndSave(byte[] bytes, String fileName, String businessType,
                                   Integer businessId, Integer uploaderId) {
        FileStorageService storageService = getStorageService();
        File file = storageService.uploadBytes(bytes, fileName, businessType, businessId);
        file.setUploaderId(uploaderId);
        file.setUploadTime(LocalDateTime.now());
        file.setCreateTime(LocalDateTime.now());
        file.setUpdateTime(LocalDateTime.now());
        fileMapper.insert(file);
        log.info("字节流文件上传并保存成功：fileId={}, fileName={}", file.getFileId(), file.getFileName());
        return file;
    }

    /**
     * 根据文件ID读取文件内容
     * 自动根据文件的存储类型选择本地或OSS存储读取
     *
     * @param fileId 文件ID
     * @return 文件字节数组
     * @throws RuntimeException 如果文件不存在或读取失败
     */
    public byte[] readFileById(Integer fileId) {
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在，文件ID：" + fileId);
        }
        return readFile(file);
    }

    /**
     * 根据文件ID获取文件URL
     *
     * @param fileId 文件ID
     * @return 文件访问URL，如果文件不存在则返回null
     */
    public String getFileUrl(Integer fileId) {
        if (fileId == null) {
            return null;
        }
        File file = fileMapper.selectById(fileId);
        if (file == null) {
            return null;
        }
        return getFileUrlByFile(file);
    }

    /**
     * 根据文件实体获取文件URL
     *
     * @param file 文件实体
     * @return 文件访问URL
     */
    public String getFileUrlByFile(File file) {
        if (file == null) {
            return null;
        }
        FileStorageService storageService;
        String filePath;

        if ("本地".equals(file.getStorageType())) {
            storageService = localFileStorageService;
            filePath = file.getLocalPath();
        } else if ("云端".equals(file.getStorageType())) {
            storageService = ossFileStorageService;
            filePath = file.getObjectKey();
        } else {
            return null;
        }

        if (storageService == null) {
            return null;
        }

        return storageService.getFileUrl(filePath);
    }

    /**
     * 根据文件实体读取文件内容
     * 自动根据文件的存储类型选择本地或OSS存储读取
     *
     * @param file 文件实体
     * @return 文件字节数组
     * @throws RuntimeException 如果读取失败
     */
    public byte[] readFile(File file) {
        if (file == null) {
            throw new RuntimeException("文件对象为空");
        }

        FileStorageService storageService;
        String filePath;

        if ("本地".equals(file.getStorageType())) {
            storageService = localFileStorageService;
            filePath = file.getLocalPath();
        } else if ("云端".equals(file.getStorageType())) {
            storageService = ossFileStorageService;
            filePath = file.getObjectKey();
        } else {
            throw new RuntimeException("未知的存储类型：" + file.getStorageType());
        }

        if (storageService == null) {
            throw new RuntimeException("存储服务未配置，存储类型：" + file.getStorageType());
        }

        try {
            return storageService.readFile(filePath);
        } catch (Exception e) {
            log.error("读取文件失败，fileId={}, storageType={}, filePath={}",
                    file.getFileId(), file.getStorageType(), filePath, e);
            throw new RuntimeException("读取文件失败：" + e.getMessage());
        }
    }
}

