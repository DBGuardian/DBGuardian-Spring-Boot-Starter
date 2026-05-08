package com.erp.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZIP文件处理工具类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Component
public class ZipFileProcessor {

    /**
     * 解压ZIP文件（支持多种编码格式）
     *
     * @param zipFile ZIP文件
     * @return 文件名 -> 文件内容的映射
     */
    public Map<String, byte[]> extractZipFile(MultipartFile zipFile) throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        
        // 尝试多种编码格式解压
        Charset[] charsets = {
            Charset.forName("GBK"),           // Windows中文系统默认编码
            StandardCharsets.UTF_8,            // UTF-8编码
            Charset.forName("GB18030"),        // 中文扩展编码
            StandardCharsets.ISO_8859_1        // ISO编码
        };
        
        Exception lastException = null;
        
        for (Charset charset : charsets) {
            try {
                files = tryExtractWithCharset(zipFile, charset);
                if (!files.isEmpty()) {
                    log.info("ZIP文件解压成功，使用编码：{}，共 {} 个文件", charset.name(), files.size());
                    return files;
                }
            } catch (Exception e) {
                lastException = e;
                log.debug("使用编码 {} 解压失败，尝试下一个编码", charset.name());
            }
        }
        
        // 所有编码都失败，抛出异常
        String errorMsg = "ZIP文件解压失败，已尝试所有支持的编码格式（GBK, UTF-8, GB18030, ISO-8859-1）";
        log.error(errorMsg);
        throw new Exception(errorMsg, lastException);
    }
    
    /**
     * 使用指定编码尝试解压ZIP文件
     */
    private Map<String, byte[]> tryExtractWithCharset(MultipartFile zipFile, Charset charset) throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream(), charset)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();
                    byte[] fileData = readEntryData(zis);
                    files.put(fileName, fileData);
                    log.debug("解压文件：{}，大小：{} bytes，编码：{}", fileName, fileData.length, charset.name());
                }
                zis.closeEntry();
            }
        }
        
        return files;
    }

    /**
     * 创建ZIP文件
     *
     * @param files 文件名 -> 文件数据的映射
     * @param outputStream 输出流
     * @throws Exception 打包异常
     */
    public void createZipFile(Map<String, byte[]> files, OutputStream outputStream) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                String fileName = entry.getKey();
                byte[] fileData = entry.getValue();

                // 创建ZIP条目
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);

                // 写入文件数据
                zos.write(fileData);
                zos.closeEntry();

                log.debug("添加文件到ZIP：{}，大小：{} bytes", fileName, fileData.length);
            }
        }

        log.info("ZIP文件创建完成，共 {} 个文件", files.size());
    }

    /**
     * 读取ZIP条目数据
     */
    private byte[] readEntryData(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }
}

