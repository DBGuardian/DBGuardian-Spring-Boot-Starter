package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.contract.dto.ContractSelectResponse;
import com.erp.entity.contract.Contract;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合同管理Mapper接口
 *
 * 对应表：CONTRACT
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    /**
     * 合同分页查询
     *
     * @param page             分页对象
     * @param enterpriseName   客户名称（模糊）
     * @param contractStatus   合同状态（单个状态，向后兼容）
     * @param contractStatuses 合同状态列表（多选状态）
     * @param signTimeStart    签订时间开始
     * @param signTimeEnd      签订时间结束
     * @param validFrom        有效期开始
     * @param validTo          有效期结束
     * @param pdfGenerated     是否已生成PDF
     * @param creatorFilter    创建人编码过滤（用于viewScope=SELF时仅查看自己创建的合同）
     * @param sortField        排序字段
     * @param sortOrder        排序方向：asc/desc
     * @return 分页结果
     */
    IPage<Contract> selectContractPage(
            Page<Contract> page,
            @Param("enterpriseName") String enterpriseName,
            @Param("contractStatus") String contractStatus,
            @Param("contractStatuses") List<String> contractStatuses,
            @Param("signTimeStart") java.time.LocalDateTime signTimeStart,
            @Param("signTimeEnd") java.time.LocalDateTime signTimeEnd,
            @Param("validFrom") java.time.LocalDateTime validFrom,
            @Param("validTo") java.time.LocalDateTime validTo,
            @Param("pdfGenerated") Boolean pdfGenerated,
            @Param("creatorFilter") Integer creatorFilter,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 合同详情
     *
     * @param contractId 合同编号
     * @return 合同实体（包含关联客户、审核人等扩展字段）
     */
    Contract selectDetailById(@Param("contractId") Integer contractId);

    /**
     * 根据客户ID查询合同列表（用于客户详情页）
     *
     * @param customerId 客户编码
     * @return 合同列表
     */
    List<Contract> selectByCustomerId(@Param("customerId") Integer customerId);

    /**
     * 根据时间范围查询合同列表（用于统计，只查询合同金额字段）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 合同列表（只包含合同金额字段）
     */
    List<Contract> selectByTimeRange(
            @Param("startTime") java.time.LocalDateTime startTime,
            @Param("endTime") java.time.LocalDateTime endTime
    );

    /**
     * 查询合同是否有收运通知单
     *
     * @param contractId 合同编号
     * @return 收运通知单数量
     */
    Integer countPickupNoticesByContractId(@Param("contractId") Integer contractId);

    /**
     * 查询合同是否有已完成的入库单
     *
     * @param contractId 合同编号
     * @return 已完成入库单数量
     */
    Integer countCompletedWarehousingByContractId(@Param("contractId") Integer contractId);

    /**
     * 查询合同是否有已完成的结算单
     *
     * @param contractId 合同编号
     * @return 已完成结算单数量
     */
    Integer countCompletedSettlementsByContractId(@Param("contractId") Integer contractId);

    /**
     * 查询合同最早收运通知单的创建时间
     *
     * @param contractId 合同编号
     * @return 最早创建时间
     */
    java.time.LocalDateTime getEarliestPickupNoticeTime(@Param("contractId") Integer contractId);

    /**
     * 查询合同最早完成入库单的入库时间
     *
     * @param contractId 合同编号
     * @return 最早入库时间
     */
    java.time.LocalDateTime getEarliestWarehousingTime(@Param("contractId") Integer contractId);

    /**
     * 查询合同最早完成结算单的结算时间
     *
     * @param contractId 合同编号
     * @return 最早结算时间
     */
    java.time.LocalDateTime getEarliestSettlementTime(@Param("contractId") Integer contractId);

    /**
     * 合同号模糊查询（用于运输申请等场景）
     *
     * @param page    分页对象
     * @param keyword 搜索关键字（合同号或客户名称）
     * @param viewScope 数据范围：SELF/ALL
     * @param creatorId 创建人ID（SELF模式下使用）
     * @return 分页结果
     */
    IPage<Contract> selectContractSearch(
            Page<Contract> page,
            @Param("keyword") String keyword,
            @Param("viewScope") String viewScope,
            @Param("creatorId") Integer creatorId
    );

    /**
     * 根据合同ID获取合同的价外服务
     *
     * @param contractId 合同编号
     * @return 价外服务列表
     */
    List<com.erp.controller.contract.dto.ContractWasteItemsAndServicesResponse.OutOfScopeServiceResponse> selectContractOutOfScopeServicesByContractId(@Param("contractId") Integer contractId);

    /**
     * 根据合同条目ID获取危废条目明细
     *
     * @param contractItemId 合同条目编号
     * @return 合同危废条目列表
     */
    List<com.erp.controller.contract.dto.ContractWasteItemDTO> selectContractWasteItemsByContractItemId(@Param("contractItemId") Integer contractItemId);

    /**
     * 根据合同编码查询合同基本信息
     *
     * @param contractCode 合同编码
     * @return 合同基本信息
     */
    Contract selectBasicInfoByContractCode(@Param("contractCode") String contractCode);

    /**
     * 查询业务费结算合同列表（包含未关联入库单数量）
     * 仅返回状态为"执行中"或"已完结"的合同
     *
     * @return 合同结算列表记录
     */
    List<com.erp.controller.contract.dto.ContractSettlementListResponse.ContractSettlementRecord>
            selectSettlementListWithUnlinkedInboundCount();

    /**
     * 统计未关联危废合同的危废入库单数量
     * 入库单通过 收运运输单号->运输单->收运通知单->合同号 关联
     * 未关联指：入库单关联的合同号在 CONTRACT 表中不存在
     *
     * @return 未关联入库单数量
     */
    Integer countUnlinkedWarehousing();

    /**
     * 统计未关联危废合同的危废结算单数量
     * 结算单通过 合同编号 关联
     * 未关联指：结算单的合同编号在 CONTRACT 表中不存在
     *
     * @return 未关联结算单数量
     */
    Integer countUnlinkedSettlement();

    /**
     * 危废合同下拉列表
     * 专门为下拉选择场景设计的轻量查询，只返回必要的3-5个字段
     *
     * @param keyword 搜索关键字（合同号或企业名称模糊搜索，可为null）
     * @param creatorId 创建人ID过滤（null表示不限制）
     * @return 合同下拉列表
     */
    List<ContractSelectResponse> selectContractSelectList(@Param("keyword") String keyword,
                                                          @Param("creatorId") Integer creatorId);
}




