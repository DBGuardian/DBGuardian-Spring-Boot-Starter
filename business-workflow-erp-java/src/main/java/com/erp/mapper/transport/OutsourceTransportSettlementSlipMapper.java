package com.erp.mapper.transport;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.transport.OutsourceTransportSettlementSlip;
import com.erp.controller.transport.dto.OutsourceTransportSettlementSlipVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 委外运输结算-总磅单关联表 Mapper
 */
@Mapper
public interface OutsourceTransportSettlementSlipMapper extends BaseMapper<OutsourceTransportSettlementSlip> {

    /**
     * 根据结算单编号查询关联的总磅单列表
     *
     * @param settlementId 结算单编号
     * @return 关联的总磅单列表
     */
    List<OutsourceTransportSettlementSlip> selectBySettlementId(@Param("settlementId") Integer settlementId);

    /**
     * 根据总磅单编号查询关联的结算单信息
     *
     * @param slipId 总磅单编号
     * @return 关联的结算单信息
     */
    OutsourceTransportSettlementSlip selectBySlipId(@Param("slipId") Integer slipId);

    /**
     * 批量插入关联记录
     *
     * @param slips 关联记录列表
     * @return 影响行数
     */
    int batchInsert(@Param("slips") List<OutsourceTransportSettlementSlip> slips);

    /**
     * 删除结算单关联的总磅单记录
     *
     * @param settlementId 结算单编号
     * @return 影响行数
     */
    int deleteBySettlementId(@Param("settlementId") Integer settlementId);

    /**
     * 批量删除结算单关联的总磅单记录
     *
     * @param settlementIds 结算单编号列表
     * @return 影响行数
     */
    int deleteBySettlementIds(@Param("settlementIds") List<Integer> settlementIds);

    /**
     * 统计结算单关联的总磅单数量
     *
     * @param settlementId 结算单编号
     * @return 关联数量
     */
    Long countBySettlementId(@Param("settlementId") Integer settlementId);

    /**
     * 根据结算单编号查询总磅单ID列表（用于嵌套SELECT）
     *
     * @param settlementId 结算单编号
     * @return 总磅单ID列表
     */
    List<Integer> selectSlipIdsBySettlementId(@Param("settlementId") Integer settlementId);

    /**
     * 根据结算单编号查询总磅单VO列表（用于嵌套SELECT）
     *
     * @param settlementId 结算单编号
     * @return 总磅单VO列表
     */
    List<OutsourceTransportSettlementSlipVO> selectBySettlementIdForDetail(@Param("settlementId") Integer settlementId);
}
