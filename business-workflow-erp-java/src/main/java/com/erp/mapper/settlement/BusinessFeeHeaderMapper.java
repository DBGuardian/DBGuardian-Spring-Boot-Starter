package com.erp.mapper.settlement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.settlement.dto.*;
import com.erp.entity.settlement.BusinessFeeHeader;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 业务费主表Mapper接口
 * 变更说明（2026-04-01）：
 *   - selectBusinessFeeDetail 收敛为详情核心聚合查询
 *   - 核心聚合包含主表、关联结算单、业务费明细、明细危废子表
 */
@Mapper
public interface BusinessFeeHeaderMapper extends BaseMapper<BusinessFeeHeader> {

    /**
     * 分页查询业务费列表（连表查询）
     */
    IPage<BusinessFeeListItemDTO> selectBusinessFeePage(
            Page<BusinessFeeListItemDTO> page,
            @Param("query") BusinessFeeQueryDTO queryDTO);

    /**
     * 查询业务费详情核心聚合
     * 包含主表、关联结算单、业务费明细、明细危废子表
     */
    BusinessFeeDetailDTO selectBusinessFeeDetail(@Param("businessSeq") Integer businessSeq);

    /**
     * 查询业务费明细列表（供详情核心聚合的 collection 子查询使用）
     */
    List<BusinessFeeItemDTO> selectBusinessFeeItems(@Param("businessSeq") Integer businessSeq);

    /**
     * 查询关联的危废结算单详情列表（供详情核心聚合的 collection 子查询使用）
     *
     * @param businessSeq 业务序号
     */
    List<BusinessFeeDetailDTO.SettlementRelInfoDTO> selectSettlementRelsByBusinessSeq(
            @Param("businessSeq") Integer businessSeq);

    /**
     * 查询入库明细列表（通过危废结算单编号关联获取）
     *
     * @param settlementId 危废结算单编号
     */
    List<WarehousingItemDTO> selectWarehousingItems(@Param("settlementId") Integer settlementId);

    /**
     * 查询指定日期业务结算单的最大流水号
     *
     * @param currentDate 当前日期
     * @return 最大流水号，不存在则返回 null
     */
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(业务单号, '-', 3), '-', -1) AS UNSIGNED)) FROM BUSINESS_FEE_HEADER WHERE 业务单号 LIKE CONCAT('BF-', DATE_FORMAT(#{currentDate}, '%Y%m%d'), '-%')")
    Integer selectMaxDailySequence(@Param("currentDate") LocalDate currentDate);

    /**
     * 批量查询危废结算单关联入库单聚合数据
     * <p>
     * 按废物类别+废物代码+废物名称分组，合并有价类/无价类重量，
     * 结算字段取分组内首条 SETTLEMENT_WASTE_DETAIL 记录。
     *
     * @param settlementIds 危废结算单编号列表
     * @return 聚合结果列表
     */
    List<SettlementWasteAggregateItemDTO> selectWarehousingAggregateBySettlementIds(
            @Param("settlementIds") List<Long> settlementIds);

    /**
     * 统计各状态业务费数量（单条SQL分组统计）
     *
     * @param creatorId 制单人编码（用于权限过滤，可为null）
     */
    @Select("<script>" +
            "SELECT 状态 as status, COUNT(*) as count FROM BUSINESS_FEE_HEADER" +
            "<where>" +
            "<if test='creatorId != null'>" +
            "AND 制单人编码 = #{creatorId}" +
            "</if>" +
            "</where>" +
            "GROUP BY 状态" +
            "</script>")
    List<Map<String, Object>> selectStatusStatistics(@Param("creatorId") Integer creatorId);
}
