package com.erp.service.production;

/**
 * 转移联单 PDF 异步导入任务执行器接口
 *
 * @author ERP System
 */
public interface ITransferManifestPdfAsyncService {

    /**
     * 异步执行PDF导入任务
     *
     * @param taskId 任务ID
     * @param fileBytes PDF文件字节数组
     * @param originalFilename 原始文件名
     * @param uploaderId 上传人ID
     */
    void executePdfImportAsync(String taskId, byte[] fileBytes, String originalFilename, Integer uploaderId);
}
