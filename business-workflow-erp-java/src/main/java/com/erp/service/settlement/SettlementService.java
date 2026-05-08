package com.erp.service.settlement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.common.result.Result;
import com.erp.controller.finance.dto.*;
import com.erp.controller.settlement.dto.*;
import com.erp.entity.settlement.Settlement;

import java.util.List;

/**
 * 结算单服务接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
public interface SettlementService extends IService<Settlement> {

    /**
     * 获取累积已结算量和合同计划总量
     *
     * @param contractCode 合同号
     * @param wasteCategory 废物类别
     * @return 累积数据
     */
    AccumulatedQuantityDTO getAccumulatedQuantity(String contractCode, String wasteCategory);


    /**
     * 获取入库单对应的危废明细
     *
     * @param warehousingCodes 入库单号列表
     * @return 入库危废明细列表
     */
    List<WarehousingWasteDetailVO> getWarehousingWasteDetailsByCodes(List<String> warehousingCodes);

    /**
     * 创建结算单
     *
     * @param createDTO 创建结算单DTO
     * @return 创建结果
     */
    Result<SettlementCreateResultDTO> createSettlement(SettlementCreateDTO createDTO);

    /**
     * 获取结算单详情
     *
     * @param settlementId 结算单ID
     * @return 结算单详情
     */
    SettlementDetailDTO getSettlementDetail(Long settlementId);

    /**
     * 获取结算单详情（包含关联数据）
     *
     * @param settlementId 结算单ID
     * @return 包含关联数据的结算单详情
     */
    SettlementDetailDTO getSettlementDetailWithRelatedData(Long settlementId);

    /**
     * 审核结算单
     *
     * @param settlementId 结算单ID
     * @param auditDTO 审核信息
     * @return 审核结果
     */
    Result<Void> auditSettlement(Long settlementId, SettlementAuditDTO auditDTO);

    /**
     * 结算单分页查询（通用分页数据结构，主要用于内部复用）
     *
     * @param request 分页查询请求
     * @return 分页结果（通用结算分页响应）
     */
    IPage<SettlementPageResponse> getSettlementPage(SettlementPageRequest request);

    /**
     * 收款结算分页查询
     *
     * <p>根据查询条件返回收款结算专用分页结果，用于“合同结算:危险废物结算-收款结算:页面”。</p>
     *
     * @param request 分页查询请求
     * @return 收款结算分页结果
     */
    IPage<ReceivableSettlementPageResponse> getReceivableSettlementPage(SettlementPageRequest request);

    /**
     * 付款结算分页查询
     *
     * <p>根据查询条件返回付款结算专用分页结果，用于“合同结算:危险废物结算-付款结算:页面”。</p>
     *
     * @param request 分页查询请求
     * @return 付款结算分页结果
     */
    IPage<PayableSettlementPageResponse> getPayableSettlementPage(SettlementPageRequest request);

    /**
     * 获取结算统计信息
     *
     * @param settlementType 结算类型（RECEIVABLE/ PAYABLE），为空则统计所有类型
     * @return 结算统计信息
     */
    SettlementStatisticsDTO getSettlementStatistics(String settlementType);

    /**
     * 生成结算单单号
     *
     * @return 结算单单号
     */
    String generateSettlementCode();

    /**
     * 查询已结算入库量
     *
     * @param requestDTO 请求参数
     * @return 已结算入库量数据
     */
    SettledWarehousingQuantityResponseDTO getSettledWarehousingQuantity(SettledWarehousingQuantityRequestDTO requestDTO);

    /**
     * 获取结算单审核数据
     *
     * @param settlementId 结算单ID
     * @return 结算单审核数据，包含所有审核所需的信息
     */ 
    SettlementAuditDataDTO getSettlementAuditData(Long settlementId);

    /**
     * 更新结算单
     *
     * @param settlementId 结算单ID
     * @param updateDTO 更新信息
     * @return 更新结果
     */
    Result<Void> updateSettlement(Long settlementId, SettlementUpdateDTO updateDTO);

    /**
     * 删除结算单
     * 只有特定状态（草稿、待审核、已驳回）的结算单才能删除
     * 删除时会将关联的入库单状态改为"待结算"
     * 整个操作在事务中完成，一步错误全部回滚
     *
     * @param settlementId 结算单ID
     * @return 删除结果
     */
    Result<Void> deleteSettlement(Long settlementId);

    /**
     * 批量删除结算单
     * 只有特定状态（草稿、待审核、已驳回）的结算单才能删除
     * 删除时会将关联的入库单状态改为"待结算"
     * 整个操作在事务中完成
     *
     * @param settlementIds 结算单ID列表
     * @return 删除结果
     */
    Result<Void> batchDeleteSettlements(List<Long> settlementIds);

    /**
     * 批量撤回结算单审核
     * 只有审核中状态的结算单才能撤回
     * 撤回时：
     * 1. 结算单状态改为"待审核"
     * 2. OA审核记录表状态改为"已撤回"，审核次数-1（最低为0）
     *
     * @param settlementIds 结算单ID列表
     * @return 撤回结果（包含成功数量和失败数量）
     */
    BatchOperationResult batchCancelAudit(List<Long> settlementIds);

    /**
     * 获取结算汇总导出数据（收款结算与付款结算的所有结算单，仅汇总表字段）
     *
     * @param creatorId 数据范围过滤：viewScope=SELF 时传入当前员工ID，ALL 时传 null
     * @return 结算汇总导出数据列表
     */
    List<SettlementExportSummaryDTO> getSettlementExportSummary(Integer creatorId);

    /**
     * 解析导出的创建人过滤条件（数据范围安全校验）
     * 根据当前员工对"危险废物结算"页面的 viewScope 配置，决定实际生效的 creatorId 过滤值：
     * - 超级管理员：始终返回 null（导出全部）
     * - viewScope=SELF：强制返回当前员工ID（仅导出自己的数据），忽略前端传入值
     * - viewScope=ALL：返回 null（导出全部），忽略前端传入值
     *
     * @param currentUserId  当前登录员工ID
     * @param creatorFilter  前端传入的 creatorFilter 参数（仅作参考，后端会覆盖）
     * @return 实际生效的创建人ID过滤值，null 表示不过滤
     */
    Integer resolveExportCreatorFilter(Integer currentUserId, Integer creatorFilter);

    /**
     * 获取结算明细导出数据（按量结算模式，危废明细）
     *
     * @return 结算明细导出数据列表
     */
    List<SettlementExportDetailDTO> getSettlementExportDetails();

    /**
     * 业务费创建专用 - 危废结算单分页查询
     * 支持按结算单编号、结算单单号、结算类型、状态、制单人名称、合同编号过滤
     * 结果中包含是否已生成业务结算单（hasBusinessFee）和业务结算单数量（businessFeeCount）
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<SettlementForBusinessFeePageResponse> getSettlementForBusinessFeePage(SettlementForBusinessFeePageRequest request);

    /**
     * 批量提交结算单审核
     * 只有待审核、已驳回状态的结算单才能提交审核
     * 提交后结算单状态改为"审核中"
     * 同时在OA审核记录表新增一条记录，来源表中文名称为"危险废物结算"
     *
     * @param settlementIds 结算单ID列表
     * @return 批量操作结果
     */
    BatchOperationResult batchSubmitAudit(List<Long> settlementIds);
}
