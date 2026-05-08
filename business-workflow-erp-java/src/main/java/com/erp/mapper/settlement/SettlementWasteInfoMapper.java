package com.erp.mapper.settlement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.settlement.SettlementWasteInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 结算危废信息Mapper接口
 *
 * 对应表：SETTLEMENT_WASTE_INFO
 */
@Mapper
public interface SettlementWasteInfoMapper extends BaseMapper<SettlementWasteInfo> {

    /**
     * 根据结算明细ID查询危废信息（支持多条）
     *
     * @param detailId 结算明细编号
     * @return 危废信息列表
     */
    List<SettlementWasteInfo> selectByDetailId(@Param("detailId") Long detailId);

    /**
     * 根据结算明细ID列表批量查询危废信息
     *
     * @param detailIds 结算明细编号列表
     * @return 危废信息列表
     */
    List<SettlementWasteInfo> selectByDetailIds(@Param("detailIds") List<Long> detailIds);

    /**
     * 根据结算单ID查询所有关联的危废信息
     *
     * @param settlementId 结算单编号
     * @return 危废信息列表
     */
    List<SettlementWasteInfo> selectBySettlementId(@Param("settlementId") Long settlementId);

    /**
     * 批量插入危废信息
     *
     * @param wasteInfoList 危废信息列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<SettlementWasteInfo> wasteInfoList);
}
