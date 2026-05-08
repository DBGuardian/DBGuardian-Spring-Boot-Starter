package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.controller.finance.dto.MergedWarehousingWasteDetailVO;
import com.erp.controller.settlement.dto.SettledWarehousingQuantityRequestDTO;
import com.erp.controller.settlement.dto.SettledWasteDetailVO;
import com.erp.controller.finance.dto.WarehousingWasteDetailVO;
import com.erp.entity.settlement.SettlementWasteDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 结算危废明细Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface SettlementWasteDetailMapper extends BaseMapper<SettlementWasteDetail> {

    /**
     * 查询指定入库单是否已被结算
     *
     * @param warehousingCodes 入库单号列表
     * @return 已结算的入库单号列表
     */
    List<String> selectSettledWarehousingCodes(@Param("warehousingCodes") List<String> warehousingCodes);

    /**
     * 根据入库单号列表获取入库危废明细
     *
     * @param warehousingCodes 入库单号列表
     * @return 入库危废明细列表
     */
    List<WarehousingWasteDetailVO> selectWarehousingWasteDetailsByCodes(@Param("warehousingCodes") List<String> warehousingCodes);

    /**
     * 根据入库单号获取同类项合并后的明细
     *
     * @param warehousingCodes 入库单号列表
     * @return 合并后的明细列表
     */
    List<MergedWarehousingWasteDetailVO> selectMergedWarehousingWasteDetails(@Param("warehousingCodes") List<String> warehousingCodes);

    /**
     * 查询已结算的废物明细记录
     *
     * @param contractCode 合同号
     * @param wasteCategory 废物类别
     * @param wasteName 废物名称
     * @param wasteCode 废物代码
     * @return 匹配的废物明细记录列表
     */
    List<SettlementWasteDetail> selectSettledWasteDetails(
        @Param("contractCode") String contractCode,
        @Param("wasteCategory") String wasteCategory,
        @Param("wasteName") String wasteName,
        @Param("wasteCode") String wasteCode
    );

    /**
     * 批量查询已结算的废物明细记录（合并同类项）
     *
     * @param contractCode 合同号
     * @param wasteItems 废物信息列表
     * @return 合并后的废物明细列表
     */
    List<SettledWasteDetailVO> selectMergedSettledWasteDetails(
        @Param("contractCode") String contractCode,
        @Param("wasteItems") List<SettledWarehousingQuantityRequestDTO.WasteItemRequest> wasteItems
    );

    /**
     * 根据结算单ID计算明细总金额
     *
     * @param settlementId 结算单ID
     * @return 明细总金额
     */
    java.math.BigDecimal selectTotalAmountBySettlementId(@Param("settlementId") Long settlementId);
}
