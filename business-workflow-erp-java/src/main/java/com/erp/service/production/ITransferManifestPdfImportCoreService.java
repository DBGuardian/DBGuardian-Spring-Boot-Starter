package com.erp.service.production;

import com.erp.controller.production.dto.ImportPdfTransferManifestResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 转移联单 PDF 导入核心服务接口
 *
 * @author ERP System
 */
public interface ITransferManifestPdfImportCoreService {

    /**
     * 从PDF字节数组导入
     *
     * @param fileBytes PDF文件字节数组
     * @param originalFilename 原始文件名
     * @param uploaderId 上传人ID
     * @return 导入结果
     */
    ImportPdfTransferManifestResponse importFromPdfBytes(byte[] fileBytes, String originalFilename, Integer uploaderId);

    /**
     * 从MultipartFile导入
     *
     * @param file PDF文件
     * @param uploaderId 上传人ID
     * @return 导入结果
     */
    ImportPdfTransferManifestResponse importFromPdf(MultipartFile file, Integer uploaderId);

    /**
     * PDF 导入核心逻辑
     *
     * @param fullDoc PDDocument文档对象
     * @param uploaderId 上传人ID
     * @return 导入结果
     */
    ImportPdfTransferManifestResponse doPdfImport(org.apache.pdfbox.pdmodel.PDDocument fullDoc, Integer uploaderId);
}
