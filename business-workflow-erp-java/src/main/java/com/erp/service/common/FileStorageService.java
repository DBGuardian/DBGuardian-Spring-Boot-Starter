package com.erp.service.common;

import com.erp.entity.common.File;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param multipartFile 文件
     * @param businessType  业务类型
     * @param businessId    业务ID
     * @return 文件信息
     */
    File uploadFile(MultipartFile multipartFile, String businessType, Integer businessId);

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    boolean deleteFile(String filePath);

    /**
     * 获取文件访问URL
     *
     * @param filePath 文件路径
     * @return 文件访问URL
     */
    String getFileUrl(String filePath);

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件字节数组
     */
    byte[] readFile(String filePath);

    /**
     * 将内存中的 byte[] 上传并返回 File 实体（不写数据库）
     *
     * @param bytes        文件字节数组
     * @param fileName     文件名（含扩展名）
     * @param businessType 业务类型
     * @param businessId   关联业务 ID
     * @return File 实体（未持久化）
     */
    File uploadBytes(byte[] bytes, String fileName, String businessType, Integer businessId);
}







































