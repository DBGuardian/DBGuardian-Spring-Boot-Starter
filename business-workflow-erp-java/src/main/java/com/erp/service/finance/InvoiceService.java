package com.erp.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.controller.finance.dto.InvoicePageRequest;
import com.erp.controller.finance.dto.InvoicePageResponse;
import com.erp.entity.finance.Invoice;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 发票服务接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
public interface InvoiceService extends IService<Invoice> {

    /**
     * 从ZIP文件批量导入发票
     *
     * @param zipFile ZIP文件
     * @param invoiceStatus 发票状态（进项发票/销项发票）
     * @return 批量导入结果
     */
    Map<String, Object> batchImportFromZip(MultipartFile zipFile, String invoiceStatus);

    /**
     * Excel导入发票（表格导入）
     *
     * @param excelFile Excel文件
     * @param invoiceStatus 发票状态（进项发票/销项发票）
     * @return 批量导入结果
     */
    Map<String, Object> excelImportInvoice(MultipartFile excelFile, String invoiceStatus);

    /**
     * 创建发票
     *
     * @param invoiceData 发票数据
     * @return 创建结果
     */
    Map<String, Object> createInvoice(Map<String, Object> invoiceData);

    /**
     * 发票分页查询
     *
     * @param request 查询条件
     * @return 发票分页结果
     */
    IPage<InvoicePageResponse> getInvoicePage(InvoicePageRequest request);

    /**
     * 根据发票ID查询发票详情（包含所有字段和明细）
     *
     * @param invoiceId 发票ID
     * @return 发票详情数据
     */
    Map<String, Object> getInvoiceDetail(Integer invoiceId);

    /**
     * 更新发票
     *
     * @param invoiceId 发票ID
     * @param invoiceData 发票数据
     * @return 更新结果
     */
    Map<String, Object> updateInvoice(Integer invoiceId, Map<String, Object> invoiceData);

    /**
     * 批量文件补充（PDF/图片），按发票号匹配已有发票并补充附件
     *
     * @param files         文件数组（数量上限50个）
     * @param invoiceStatus 发票状态（进项发票/销项发票）
     * @return 补充结果统计信息
     */
    Map<String, Object> fileSupplement(MultipartFile[] files, String invoiceStatus);

    /**
     * 查询发票文件编号
     * 查询发票是否有PDF、图片文件
     *
     * @param invoiceId 发票ID
     * @return 文件编号信息（pdfFileId, imageFileId）
     */
    Map<String, Object> getInvoiceFileIds(Integer invoiceId);

    /**
     * 上传单个发票文件
     * 为指定发票上传单个PDF或图片文件
     *
     * @param invoiceId 发票ID
     * @param file      文件
     * @param fileType  文件类型（pdf/image）
     * @return 上传结果
     */
    Map<String, Object> uploadInvoiceFile(Integer invoiceId, MultipartFile file, String fileType);

    /**
     * 导出发票Excel
     * 根据发票ID列表导出发票数据为Excel文件，包含发票基本信息和明细信息（明细分行显示）
     *
     * @param invoiceIds    发票ID列表
     * @param invoiceStatus 发票状态（进项发票/销项发票）
     * @return Excel文件字节数组
     */
    byte[] exportInvoiceExcel(List<Integer> invoiceIds, String invoiceStatus);

    /**
     * 导出发票文件
     * 根据发票ID列表导出发票相关文件为ZIP压缩包
     *
     * @param invoiceIds    发票ID列表
     * @param invoiceStatus 发票状态（进项发票/销项发票）
     * @return ZIP文件字节数组
     */
    byte[] exportInvoiceFiles(List<Integer> invoiceIds, String invoiceStatus);

    /**
     * 全部导出发票Excel
     * 直接导出系统中的全部发票数据为Excel文件
     *
     * @param invoiceStatus 发票状态（进项发票/销项发票，可选，不传则导出全部）
     * @param creatorId     创建人员工ID（viewScope=SELF 时传入，只导出该员工创建的数据；为 null 时导出全部）
     * @return Excel文件字节数组
     */
    byte[] exportAllInvoiceExcel(String invoiceStatus, Integer creatorId);

    /**
     * 全部导出发票文件
     * 直接导出系统中的全部发票相关文件为ZIP压缩包
     *
     * @param invoiceStatus 发票状态（进项发票/销项发票，可选，不传则导出全部）
     * @param creatorId     创建人员工ID（viewScope=SELF 时传入，只导出该员工创建的数据；为 null 时导出全部）
     * @return ZIP文件字节数组
     */
    byte[] exportAllInvoiceFiles(String invoiceStatus, Integer creatorId);

    /**
     * 获取发票关联记录
     *
     * @param invoiceId 发票ID
     * @return 关联记录信息
     */
    Map<String, Object> getInvoiceAssociations(Integer invoiceId);
    
    /**
     * 更新发票的合同/结算单关联信息
     *
     * @param invoiceId 发票ID
     * @param data      关联信息（例如 contractId，settlementIds 等）
     */
    void updateInvoiceAssociations(Integer invoiceId, Map<String, Object> data);
    
    /**
     * updateInvoiceAssociations 行为说明：
     * - 前端应提交 payload 包含：
     *   - contractId (可选)
     *   - settlementIds: number[] （可选，若明确要删除所有结算关联，请传空数组 []）
     * - 后端处理语义：
     *   1) 若前端传入空的 settlementIds 且数据库存在现有关联，则删除该发票在 SETTLEMENT_INVOICE_REL 中的所有关联记录（以及在检测到变更时删除 INVOICE_NOTICE_INVOICE 的登记），用于“删除关联”的场景。
     *   2) 若前端传入 settlementIds 非空，则按差分处理：删除 existing − new 的 rel，新增 new − existing 的 rel（status='BOUND'，noticeId=null）。
     *   3) 若检测到合同或结算单发生变更（changed=true），后端会删除该发票在 INVOICE_NOTICE_INVOICE 中的登记记录（表示开票通知单登记需被清理）。
     */
}

