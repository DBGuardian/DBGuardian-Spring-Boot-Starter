package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.finance.dto.InvoicePageRequest;
import com.erp.controller.finance.dto.InvoicePageResponse;
import com.erp.entity.finance.Invoice;
import com.erp.entity.finance.InvoiceItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 发票Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {

    /**
     * 根据发票号码查询发票
     *
     * @param invoiceNumber 发票号码
     * @return 发票信息
     */
    Invoice selectByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    /**
     * 插入发票明细
     *
     * @param item 发票明细
     */
    void insertItem(InvoiceItem item);

    /**
     * 根据发票编号查询发票明细列表
     *
     * @param invoiceId 发票编号
     * @return 发票明细列表
     */
    List<InvoiceItem> selectItemsByInvoiceId(@Param("invoiceId") Integer invoiceId);

    /**
     * 根据发票编号删除发票明细
     *
     * @param invoiceId 发票编号
     */
    void deleteItemsByInvoiceId(@Param("invoiceId") Integer invoiceId);

    /**
     * 根据明细编号删除发票明细
     *
     * @param itemId 明细编号
     */
    void deleteItemById(@Param("itemId") Integer itemId);

    /**
     * 更新发票明细
     *
     * @param item 发票明细
     */
    void updateItem(InvoiceItem item);

    /**
     * 发票分页查询
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 发票分页结果
     */
    IPage<InvoicePageResponse> selectInvoicePage(
            Page<InvoicePageResponse> page,
            @Param("req") InvoicePageRequest request);

    /**
     * 根据发票ID查询发票详情（连表查询，包含明细）
     *
     * @param invoiceId 发票编号
     * @return 发票详情（包含明细列表）
     */
    com.erp.controller.finance.dto.InvoiceDetailDTO selectInvoiceDetailWithItems(@Param("invoiceId") Integer invoiceId);

    /**
     * 根据发票ID列表查询发票明细
     *
     * @param invoiceIds 发票ID列表
     * @return 发票明细列表
     */
    List<InvoiceItem> selectItemsByInvoiceIds(@Param("invoiceIds") List<Integer> invoiceIds);

    /**
     * 查询发票的关联记录
     *
     * @param invoiceId 发票ID
     * @return 关联记录列表
     */
    List<Map<String, Object>> selectInvoiceAssociations(@Param("invoiceId") Integer invoiceId);
}

