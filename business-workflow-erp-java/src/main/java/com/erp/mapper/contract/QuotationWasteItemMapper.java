package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.QuotationWasteItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 报价危废条目明细Mapper接口
 *
 * 对应表：QUOTATION_WASTE_ITEM
 */
@Mapper
public interface QuotationWasteItemMapper extends BaseMapper<QuotationWasteItem> {

    /**
     * 根据报价条目编号查询危废条目明细列表
     *
     * @param quotationItemId 报价条目编号
     * @return 危废条目明细列表
     */
    List<QuotationWasteItem> selectByQuotationItemId(Integer quotationItemId);

    /**
     * 根据报价条目编号列表批量查询危废条目明细
     *
     * @param quotationItemIds 报价条目编号列表
     * @return 危废条目明细列表
     */
    List<QuotationWasteItem> selectByQuotationItemIds(List<Integer> quotationItemIds);

    /**
     * 根据报价条目编号删除危废条目明细
     *
     * @param quotationItemId 报价条目编号
     * @return 删除数量
     */
    int deleteByQuotationItemId(Integer quotationItemId);

    /**
     * 根据报价条目编号列表批量删除危废条目明细
     *
     * @param quotationItemIds 报价条目编号列表
     * @return 删除数量
     */
    int deleteByQuotationItemIds(List<Integer> quotationItemIds);
}
































