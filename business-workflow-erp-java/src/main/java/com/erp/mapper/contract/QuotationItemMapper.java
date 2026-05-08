package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.QuotationItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报价条目Mapper接口
 *
 * 对应表：QUOTATION_ITEM
 */
@Mapper
public interface QuotationItemMapper extends BaseMapper<QuotationItem> {

    /**
     * 根据报价单编号查询报价条目列表
     *
     * @param quotationId 报价单编号
     * @return 报价条目列表
     */
    List<QuotationItem> selectByQuotationId(Integer quotationId);

    /**
     * 根据报价单编号列表批量查询报价条目
     *
     * @param quotationIds 报价单编号列表
     * @return 报价条目列表
     */
    List<QuotationItem> selectByQuotationIds(@Param("quotationIds") List<Integer> quotationIds);

    /**
     * 根据报价单编号删除报价条目
     *
     * @param quotationId 报价单编号
     * @return 删除数量
     */
    int deleteByQuotationId(Integer quotationId);
}































