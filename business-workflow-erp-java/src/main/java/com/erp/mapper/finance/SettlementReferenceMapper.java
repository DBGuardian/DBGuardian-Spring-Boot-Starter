package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.settlement.SettlementReference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 结算关联表Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface SettlementReferenceMapper extends BaseMapper<SettlementReference> {

    /**
     * 根据结算单ID查询关联的入库记录编码列表
     *
     * @param settlementId 结算单ID
     * @return 入库记录编码列表
     */
    List<String> selectWarehousingCodesBySettlementId(@Param("settlementId") Long settlementId);

    /**
     * 根据结算单ID查询关联的运输记录编码列表
     *
     * @param settlementId 结算单ID
     * @return 运输记录编码列表
     */
    List<String> selectTransportCodesBySettlementId(@Param("settlementId") Long settlementId);
}
