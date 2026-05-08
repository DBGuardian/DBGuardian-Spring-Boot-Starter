package com.erp.mapper.report;

import com.erp.controller.report.dto.WasteLimitAnalysisResponse.WasteCategoryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 危险废物限额分析 Mapper
 *
 * 功能描述：
 * - 查询废物类别及其限额
 * - 查询各废物类别的合同签订量（计划转移数量）
 * - 查询各废物类别的实际收运量（实际入库数量）
 * - 查询各废物类别的已生成联单量（确认数量）
 */
@Mapper
public interface WasteLimitAnalysisMapper {

    /**
     * 查询所有有效废物类别（按限额升序排列）
     *
     * @param wasteCategories 废物类别筛选（如 ["HW01", "HW02"]），为空则查询所有
     * @return 废物类别列表（含 categoryId, categoryCode, categoryName, limit）
     */
    List<WasteCategoryDTO> selectAllCategories(@Param("wasteCategories") String[] wasteCategories);

    /**
     * 查询指定日期范围内各废物类别的合同签订量（计划转移数量，单位：吨）
     *
     * 统计口径：
     * - 合同状态：已通过、执行中、已完结、已归档
     * - 来源表：CONTRACT_WASTE_ITEM → CONTRACT_ITEM → CONTRACT
     *
     * @param startDate 开始日期（YYYY-MM-DD，含）
     * @param endDate   结束日期（YYYY-MM-DD，含）
     * @return 按废物类别汇总的计划转移数量（categoryCode, planned）
     */
    List<WasteCategoryDTO> selectPlannedByPeriod(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("wasteCategories") String[] wasteCategories);

    /**
     * 查询指定日期范围内各废物类别的实际收运量（实际入库数量，单位：吨）
     *
     * 统计口径：
     * - 来源表：WAREHOUSING_WASTE_ITEM（入库单危废明细）
     *
     * @param startDate 开始日期（YYYY-MM-DD，含）
     * @param endDate   结束日期（YYYY-MM-DD，含）
     * @return 按废物类别汇总的实际收运数量（categoryCode, actual）
     */
    List<WasteCategoryDTO> selectActualByPeriod(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("wasteCategories") String[] wasteCategories);

    /**
     * 查询指定日期范围内各废物类别的已生成联单量（确认数量，单位：吨）
     *
     * 统计口径：
     * - 来源表：TRANSFER_MANIFEST_ITEM（转��联单子表）
     *
     * @param startDate 开始日期（YYYY-MM-DD，含）
     * @param endDate   结束日期（YYYY-MM-DD，含）
     * @return 按废物类别汇总的确认数量（categoryCode, manifest）
     */
    List<WasteCategoryDTO> selectManifestByPeriod(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("wasteCategories") String[] wasteCategories);
}
