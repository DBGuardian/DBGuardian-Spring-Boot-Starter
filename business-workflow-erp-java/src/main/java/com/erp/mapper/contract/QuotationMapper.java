package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.contract.Quotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报价单Mapper接口
 *
 * 对应表：QUOTATION
 */
@Mapper
public interface QuotationMapper extends BaseMapper<Quotation> {

    /**
     * 查询报价单详情
     *
     * @param quotationId 报价单编号
     * @return 报价单详情
     */
    Quotation selectQuotationDetail(Integer quotationId);

    /**
     * 分页查询报价单
     *
     * @param page 分页对象
     * @param keyword 关键词（客户名称/报价单号）
     * @param quotationNoSearch 报价单编号（模糊查询）
     * @param customerName 客户名称（模糊查询）
     * @param customerId 客户编码
     * @param quotationStatus 报价状态
     * @param pricingMode 计价方式：PACKAGE(总价包干)/UNIT(按量结算)/MIXED(组合计价)
     * @param quotationNo 报价单号
     * @param internalCode 内部编号
     * @param creatorName 创建人姓名
     * @param validFrom 有效期开始
     * @param validTo 有效期结束
     * @param pdfGenerated 是否已生成PDF
     * @param creatorFilter 创建人过滤（数据范围控制，null表示不过滤）
     * @param sortField 排序字段
     * @param sortOrder 排序方向：asc/desc
     * @return 分页结果
     */
    IPage<Quotation> selectQuotationPage(
            Page<Quotation> page,
            @Param("keyword") String keyword,
            @Param("quotationNoSearch") String quotationNoSearch,
            @Param("customerName") String customerName,
            @Param("customerId") Integer customerId,
            @Param("quotationStatus") String quotationStatus,
            @Param("pricingMode") String pricingMode,
            @Param("quotationNo") String quotationNo,
            @Param("internalCode") String internalCode,
            @Param("creatorName") String creatorName,
            @Param("validFrom") java.time.LocalDateTime validFrom,
            @Param("validTo") java.time.LocalDateTime validTo,
            @Param("pdfGenerated") Boolean pdfGenerated,
            @Param("creatorFilter") Integer creatorFilter,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 根据客户编码查询报价单列表
     *
     * @param customerId 客户编码
     * @return 报价单列表
     */
    List<Quotation> selectByCustomerId(Integer customerId);
}



