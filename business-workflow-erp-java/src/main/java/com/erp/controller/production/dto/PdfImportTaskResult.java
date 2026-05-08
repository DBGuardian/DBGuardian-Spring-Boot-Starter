package com.erp.controller.production.dto;

import lombok.Data;

/**
 * PDF 导入异步任务状态
 */
@Data
public class PdfImportTaskResult {

    public enum Status { PENDING, RUNNING, SUCCESS, FAILED }

    /** 任务 ID */
    private String taskId;

    /** 任务状态 */
    private Status status = Status.PENDING;

    /** 进度描述（如：正在解析第 3/20 页）*/
    private String progress;

    /** 成功时的导入结果 */
    private ImportPdfTransferManifestResponse result;

    /** 失败时的错误信息 */
    private String errorMessage;

    /** 创建时间（毫秒时间戳，用于过期清理）*/
    private long createdAt = System.currentTimeMillis();
}
