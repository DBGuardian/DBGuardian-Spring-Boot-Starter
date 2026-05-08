package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.finance.InvoiceNoticeInvoice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 开票通知单-发票登记明细Mapper接口
 *
 * 对应表：INVOICE_NOTICE_INVOICE
 *
 * @author ERP System
 * @date 2026-01-07
 */
@Mapper
public interface InvoiceNoticeInvoiceMapper extends BaseMapper<InvoiceNoticeInvoice> {

    /**
     * 根据通知单ID查询所有关联的发票明细
     *
     * @param noticeId 通知单ID
     * @return 发票明细列表
     */
    List<InvoiceNoticeInvoice> selectByNoticeId(@Param("noticeId") Integer noticeId);

    /**
     * 根据发票ID查询关联记录
     *
     * @param invoiceId 发票ID
     * @return 关联记录列表
     */
    List<InvoiceNoticeInvoice> selectByInvoiceId(@Param("invoiceId") Integer invoiceId);

    /**
     * 根据通知单ID删除所有关联的发票明细
     *
     * @param noticeId 通知单ID
     * @return 删除数量
     */
    int deleteByNoticeId(@Param("noticeId") Integer noticeId);
}


