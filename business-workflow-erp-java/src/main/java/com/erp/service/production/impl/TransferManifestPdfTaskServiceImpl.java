package com.erp.service.production.impl;

import com.erp.controller.production.dto.ImportPdfTransferManifestResponse;
import com.erp.controller.production.dto.PdfImportTaskResult;
import com.erp.service.production.ITransferManifestPdfTaskService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PDF 导入任务状态服务实现
 *
 * @author ERP System
 */
@Service
public class TransferManifestPdfTaskServiceImpl implements ITransferManifestPdfTaskService {

    private static final long TASK_TTL_MS = 60 * 60 * 1000L;
    private final ConcurrentHashMap<String, PdfImportTaskResult> taskCache = new ConcurrentHashMap<>();

    @Override
    public String createTask() {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        PdfImportTaskResult taskResult = new PdfImportTaskResult();
        taskResult.setTaskId(taskId);
        taskResult.setStatus(PdfImportTaskResult.Status.PENDING);
        taskResult.setProgress("任务已提交，等待处理...");
        taskCache.put(taskId, taskResult);
        cleanupExpiredTasks();
        return taskId;
    }

    @Override
    public PdfImportTaskResult getTaskResult(String taskId) {
        return taskCache.get(taskId);
    }

    @Override
    public void markRunning(String taskId, String progress) {
        PdfImportTaskResult taskResult = taskCache.get(taskId);
        if (taskResult == null) {
            return;
        }
        taskResult.setStatus(PdfImportTaskResult.Status.RUNNING);
        taskResult.setProgress(progress);
        taskResult.setErrorMessage(null);
    }

    @Override
    public void markSuccess(String taskId, ImportPdfTransferManifestResponse result) {
        PdfImportTaskResult taskResult = taskCache.get(taskId);
        if (taskResult == null) {
            return;
        }
        taskResult.setResult(result);
        taskResult.setStatus(PdfImportTaskResult.Status.SUCCESS);
        taskResult.setProgress("处理完成");
        taskResult.setErrorMessage(null);
    }

    @Override
    public void markFailed(String taskId, String errorMessage) {
        PdfImportTaskResult taskResult = taskCache.get(taskId);
        if (taskResult == null) {
            return;
        }
        taskResult.setStatus(PdfImportTaskResult.Status.FAILED);
        taskResult.setErrorMessage(errorMessage);
        taskResult.setProgress("处理失败：" + errorMessage);
    }

    private void cleanupExpiredTasks() {
        taskCache.entrySet().removeIf(e ->
                System.currentTimeMillis() - e.getValue().getCreatedAt() > TASK_TTL_MS);
    }
}
