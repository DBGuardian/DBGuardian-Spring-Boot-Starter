package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.finance.dto.InvoiceNoticePageRequest;
import com.erp.controller.finance.dto.InvoiceNoticePageResponse;
import com.erp.entity.finance.InvoiceNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 开票通知单Mapper接口
 *
 * 对应表：INVOICE_NOTICE
 *
 * @author ERP System
 * @date 2026-01-06
 */
@Mapper
public interface InvoiceNoticeMapper extends BaseMapper<InvoiceNotice> {

    /**
     * 根据前缀查询最大通知单号
     * 用于生成新的通知单号
     *
     * @param prefix 前缀（如：KPTZ-20260106-）
     * @return 最大通知单号
     */
    String selectMaxNoticeNoByPrefix(@Param("prefix") String prefix);

    /**
     * 根据通知单号统计数量
     * 用于检查通知单号是否已存在
     *
     * @param noticeNo 通知单号
     * @return 数量
     */
    int countByNoticeNo(@Param("noticeNo") String noticeNo);

    /**
     * 分页查询开票通知单列表（支持关联查询结算单信息）
     *
     * @param page 分页对象
     * @param request 查询请求
     * @param creatorFilter 创建人过滤（viewScope=SELF时传当前员工编码；ALL时传null）
     * @return 分页结果
     */
    IPage<InvoiceNoticePageResponse> selectPageWithSettlement(@Param("page") Page<InvoiceNoticePageResponse> page,
                                                              @Param("request") InvoiceNoticePageRequest request,
                                                              @Param("creatorFilter") Integer creatorFilter);

    /**
     * 查询开票通知单详情（包含关联的发票列表和客户信息）
     *
     * @param noticeId 通知单ID
     * @return 详情响应对象
     */
    com.erp.controller.finance.dto.InvoiceNoticeDetailResponse selectDetailWithInvoices(@Param("noticeId") Integer noticeId);
}

