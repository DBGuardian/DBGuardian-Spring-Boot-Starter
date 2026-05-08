package com.erp.service.production.impl;

import com.erp.controller.production.dto.ImportPdfTransferManifestResponse;
import com.erp.service.production.ITransferManifestPdfAsyncService;
import com.erp.service.production.ITransferManifestPdfTaskService;
import com.erp.service.production.ITransferManifestPdfImportCoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 转移联单 PDF 异步导入任务执行器实现
 *
 * @author ERP System
 */
@Slf4j
@Service
public class TransferManifestPdfAsyncServiceImpl implements ITransferManifestPdfAsyncService {

    @Autowired
    private ITransferManifestPdfTaskService transferManifestPdfTaskService;

    @Autowired
    private ITransferManifestPdfImportCoreService transferManifestPdfImportCoreService;

    @Override
    @Async("pdfImportExecutor")
    public void executePdfImportAsync(String taskId, byte[] fileBytes,
                                     String originalFilename, Integer uploaderId) {
        transferManifestPdfTaskService.markRunning(taskId, "正在解析 PDF 文件...");
        try {
            ImportPdfTransferManifestResponse result =
                    transferManifestPdfImportCoreService.importFromPdfBytes(fileBytes, originalFilename, uploaderId);
            transferManifestPdfTaskService.markSuccess(taskId, result);
        } catch (Exception e) {
            log.error("PDF 异步导入任务失败，taskId={}", taskId, e);
            String errorMessage = e.getMessage() == null ? "PDF 导入失败" : e.getMessage();
            transferManifestPdfTaskService.markFailed(taskId, errorMessage);
        }
    }
}
