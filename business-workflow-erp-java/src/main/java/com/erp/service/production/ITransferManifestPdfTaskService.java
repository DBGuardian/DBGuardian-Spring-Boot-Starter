package com.erp.service.production;

import com.erp.controller.production.dto.ImportPdfTransferManifestResponse;
import com.erp.controller.production.dto.PdfImportTaskResult;

/**
 * PDF 导入任务状态服务接口
 *
 * @author ERP System
 */
public interface ITransferManifestPdfTaskService {

    /**
     * 创建新的导入任务
     *
     * @return 任务ID
     */
    String createTask();

    /**
     * 获取任务结果
     *
     * @param taskId 任务ID
     * @return 任务结果
     */
    PdfImportTaskResult getTaskResult(String taskId);

    /**
     * 标记任务为运行中
     *
     * @param taskId 任务ID
     * @param progress 当前进度描述
     */
    void markRunning(String taskId, String progress);

    /**
     * 标记任务为成功
     *
     * @param taskId 任务ID
     * @param result 导入结果
     */
    void markSuccess(String taskId, ImportPdfTransferManifestResponse result);

    /**
     * 标记任务为失败
     *
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void markFailed(String taskId, String errorMessage);
}
