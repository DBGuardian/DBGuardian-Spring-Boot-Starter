package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.production.PickupNoticeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收运通知单危废明细Mapper接口
 *
 * 对应表：PICKUP_NOTICE_ITEM
 */
@Mapper
public interface PickupNoticeItemMapper extends BaseMapper<PickupNoticeItem> {

    /**
     * 根据收运通知单号查询危废明细列表
     *
     * @param noticeCode 收运通知单号
     * @return 危废明细列表
     */
    List<PickupNoticeItem> selectByNoticeCode(@Param("noticeCode") String noticeCode);

    /**
     * 根据收运通知单号列表批量查询危废明细
     *
     * @param noticeCodes 收运通知单号列表
     * @return 危废明细列表
     */
    List<PickupNoticeItem> selectByNoticeCodes(@Param("noticeCodes") List<String> noticeCodes);

    /**
     * 根据收运通知单号删除危废明细
     *
     * @param noticeCode 收运通知单号
     * @return 删除数量
     */
    int deleteByNoticeCode(@Param("noticeCode") String noticeCode);

    /**
     * 根据收运通知单号列表批量删除危废明细
     *
     * @param noticeCodes 收运通知单号列表
     * @return 删除数量
     */
    int deleteByNoticeCodes(@Param("noticeCodes") List<String> noticeCodes);

    /**
     * 根据合同危废明细编号查询收运通知单明细
     *
     * @param contractWasteItemId 合同危废明细编号
     * @return 收运通知单明细列表
     */
    List<PickupNoticeItem> selectByContractWasteItemId(@Param("contractWasteItemId") Integer contractWasteItemId);
}

