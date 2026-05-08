package com.erp.service.production;

import com.erp.controller.production.dto.ImportPdfTransferManifestResponse;
import com.erp.controller.production.dto.ImportTransferManifestResponse;
import com.erp.controller.production.dto.PdfImportTaskResult;
import com.erp.controller.production.dto.TransferManifestListResponse;
import com.erp.controller.production.dto.TransferManifestExportRow;
import com.erp.controller.production.dto.TransferManifestPageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 转移联单服务接口
 */
public interface TransferManifestService {

    /**
     * 分页查询转移联单列表（含废物子项）
     *
     * @param request 查询参数
     * @return 分页结果
     */
    TransferManifestListResponse getPage(TransferManifestPageRequest request);

    /**
     * 导出转移联单 Excel 数据
     *
     * @param manifestIds 勾选的联单编号；为空时导出全部
     * @return 导出行数据
     */
    List<TransferManifestExportRow> getExportRows(List<Integer> manifestIds);

    /**
     * 批量导出转移联单 PDF 附件 ZIP
     *
     * @param manifestIds 勾选的联单编号；为空时导出全部
     * @return ZIP 文件字节数组；无可导出文件时返回 null
     */
    byte[] exportPdfZip(List<Integer> manifestIds);

    /**
     * 批量导入转移联单（Excel）
     *
     * @param file Excel 文件（.xlsx / .xls）
     * @return 导入结果（总数/成功数/失败数/错误明细）
     */
    ImportTransferManifestResponse importFromExcel(MultipartFile file);

    /**
     * 异步提交 PDF 导入任务，立即返回任务 ID
     *
     * @param file       PDF 文件
     * @param uploaderId 当前登录用户编码
     * @return 任务 ID（taskId）
     */
    String submitPdfImportTask(MultipartFile file, Integer uploaderId);

    /**
     * 查询 PDF 导入任务状态
     *
     * @param taskId 任务 ID
     * @return 任务状态对象，不存在时返回 null
     */
    PdfImportTaskResult getPdfImportTaskResult(String taskId);

    /**
     * 查询单条联单的 PDF 文件编号
     *
     * @param manifestId 联单编号
     * @return PDF 文件编号，不存在时返回 null
     */
    Integer getPdfFileId(Integer manifestId);

    /**
     * 上传并替换单条联单的 PDF 附件
     *
     * @param manifestId 联单编号
     * @param file PDF 文件
     * @return 新的 PDF 文件编号
     */
    Integer uploadPdf(Integer manifestId, MultipartFile file);

    /**
     * 批量导入转移联单 PDF 附件（同步执行，供异步包装层调用）
     *
     * @param file      PDF 文件（.pdf）
     * @param uploaderId 当前登录用户编码
     * @return 导入结果
     */
    ImportPdfTransferManifestResponse importFromPdf(MultipartFile file, Integer uploaderId);
}
