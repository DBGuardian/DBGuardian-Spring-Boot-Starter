package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.settlement.dto.SettlementExportDetailDTO;
import com.erp.controller.settlement.dto.SettlementExportSummaryDTO;
import com.erp.controller.settlement.dto.SettlementPageResponse;
import com.erp.controller.settlement.dto.SettlementQueryResultDTO;
import com.erp.entity.settlement.Settlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 结算单Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface SettlementMapper extends BaseMapper<Settlement> {

    /**
     * 根据合同号和废物类别查询累积已结算量
     *
     * @param contractCode 合同号
     * @param wasteCategory 废物类别
     * @return 累积已结算量
     */
    BigDecimal selectAccumulatedSettledQuantity(@Param("contractCode") String contractCode, @Param("wasteCategory") String wasteCategory);

    /**
     * 根据合同号查询合同计划总量
     *
     * @param contractCode 合同号
     * @param wasteCategory 废物类别
     * @return 合同计划总量
     */
    BigDecimal selectContractPlanTotal(@Param("contractCode") String contractCode, @Param("wasteCategory") String wasteCategory);

    /**
     * 生成下一个结算单单号（全局序号）
     *
     * @return 下一个序号
     */
    Integer selectNextSettlementSequence();

    /**
     * 查询指定入库单是否已被结算
     *
     * @param warehousingCodes 入库单号列表
     * @return 已结算的入库单号列表
     */
    List<String> selectSettledWarehousingCodes(@Param("warehousingCodes") List<String> warehousingCodes);


    /**
     * 结算单分页查询
     *
     * @param page 分页对象
     * @param settlementType 结算类型
     * @param settlementCode 结算单单号（模糊匹配）
     * @param contractCode 合同号（模糊匹配）
     * @param customerName 客户名称（模糊匹配）
     * @param status 状态
     * @param creatorName 制单人名称（模糊匹配）
     * @param settlementStartFrom 结算周期起开始
     * @param settlementStartTo 结算周期起结束
     * @param settlementEndFrom 结算周期止开始
     * @param settlementEndTo 结算周期止结束
     * @param createTimeStart 创建时间开始
     * @param createTimeEnd 创建时间结束
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @param independentOnly 是否只查询独立数据（未关联合同的结算单）
     * @return 分页结果
     */
    IPage<SettlementPageResponse> selectSettlementPage(
            IPage<SettlementPageResponse> page,
            @Param("settlementType") String settlementType,
            @Param("settlementCode") String settlementCode,
            @Param("contractCode") String contractCode,
            @Param("customerName") String customerName,
            @Param("status") String status,
            @Param("creatorName") String creatorName,
            @Param("settlementStartFrom") LocalDate settlementStartFrom,
            @Param("settlementStartTo") LocalDate settlementStartTo,
            @Param("settlementEndFrom") LocalDate settlementEndFrom,
            @Param("settlementEndTo") LocalDate settlementEndTo,
            @Param("createTimeStart") LocalDateTime createTimeStart,
            @Param("createTimeEnd") LocalDateTime createTimeEnd,
            @Param("creatorIdFilter") Integer creatorIdFilter,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder,
            @Param("independentOnly") Boolean independentOnly
    );

    /**
     * 根据合同号查询合同编号（主表ID）
     *
     * @param contractCode 合同号
     * @return 合同编号
     */
    Integer selectContractIdByContractCode(@Param("contractCode") String contractCode);

    /**
     * 根据合同ID查询可用于开票的结算单
     *
     * @param contractId 合同ID
     * @param invoiceType 开票类型：开票/作废
     * @param viewScope 数据范围：SELF/ALL
     * @param creatorId 创建人ID（SELF模式下使用）
     * @return 结算单查询结果列表
     */
    List<SettlementQueryResultDTO> selectSettlementsByContractAndInvoiceType(
        @Param("contractId") Integer contractId,
        @Param("invoiceType") String invoiceType,
        @Param("viewScope") String viewScope,
        @Param("creatorId") Integer creatorId
    );

    /**
     * 获取结算统计信息
     *
     * @param settlementType 结算类型（可选）
     * @return 统计结果Map
     */
    Map<String, Object> selectSettlementStatistics(@Param("settlementType") String settlementType);

    /**
     * 结算汇总导出查询：收款结算和付款结算的所有结算单（仅汇总字段）
     *
     * @param creatorId 数据范围过滤：viewScope=SELF 时传入当前员工ID，ALL 时传 null
     * @return 结算汇总导出列表
     */
    List<SettlementExportSummaryDTO> selectSettlementExportSummary(@Param("creatorId") Integer creatorId);

    /**
     * 结算明细导出查询（按量结算模式）
     *
     * @return 结算明细导出列表
     */
    List<SettlementExportDetailDTO> selectSettlementExportDetails();

    /**
     * 业务费创建专用 - 危废结算单分页查询
     * 支持：结算单编号、结算单单号模糊、结算类型、状态、制单人名称模糊、合同编号过滤
     * 额外返回：是否已生成业务结算单（hasBusinessFee）、业务结算单数量（businessFeeCount）
     *
     * @param page    分页对象
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<com.erp.controller.settlement.dto.SettlementForBusinessFeePageResponse> selectSettlementForBusinessFeePage(
            com.baomidou.mybatisplus.core.metadata.IPage<com.erp.controller.settlement.dto.SettlementForBusinessFeePageResponse> page,
            @Param("req") com.erp.controller.settlement.dto.SettlementForBusinessFeePageRequest request
    );
}
