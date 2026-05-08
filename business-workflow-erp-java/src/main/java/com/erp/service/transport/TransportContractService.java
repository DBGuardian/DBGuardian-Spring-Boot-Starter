package com.erp.service.transport;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.transport.dto.*;
import com.erp.entity.transport.TransportContract;

import java.util.List;
import java.util.Map;

/**
 * 运输合同 Service 接口
 */
public interface TransportContractService {

    /**
     * 批量更新运输合同状态
     *
     * @param contractIds 合同ID列表
     * @param status 目标状态
     * @return 批量更新结果，包含成功ID列表、失败ID列表、失败原因
     */
    TransportContractBatchStatusResponse batchUpdateStatus(List<Integer> contractIds, String status);

    /**
     * 分页查询运输合同列表
     */
    IPage<TransportContractPageResponse> getPage(TransportContractPageRequest request);

    /**
     * 查询运输合同详情
     */
    TransportContractDetailResponse getDetail(Integer contractId);

    /**
     * 新增运输合同（只验证数据库必填字段）
     *
     * @return 新增合同的 contractId
     */
    Integer create(TransportContractSaveRequest request);

    /**
     * 更新运输合同（只验证数据库必填字段）
     */
    void update(Integer contractId, TransportContractSaveRequest request);

    /**
     * 审核通过运输合同（包含完整字段验证）
     */
    void audit(Integer contractId, TransportContractAuditRequest request);

    /**
     * 更新合同状态
     */
    void updateStatus(Integer contractId, TransportContractStatusRequest request);

    /**
     * 逻辑删除合同（仅待审核状态可删除）
     */
    void delete(Integer contractId);

    /**
     * 查询各状态合同数量统计
     *
     * @return map，包含 draft（待审核）、reviewing（审核中）、executing（执行中）、completed（已完结）
     */
    Map<String, Long> getStatistics();

    /**
     * 运输合同查询（关联车辆统计）
     * 查询 TRANSPORT_CONTRACT 表的合同编号、合同单号、承运方名称
     * 关联查询 TRANSPORT_CONTRACT_VEHICLE 车辆编号关联 VEHICLE 获取车牌号
     * 统计 DISPATCH_ORDER 中运输车辆号牌与合同关联车辆号牌相等但车辆编号为空的记录数量
     * 统计 OUTSOURCE_TRANSPORT_SETTLEMENT 中合同编号为空的记录数量
     *
     * @return 合同查询结果列表
     */
    List<TransportContractQueryResponse> getContractWithVehicleList();

    /**
     * 搜索运输合同（下拉框用）
     * 支持按合同单号、承运方名称搜索
     *
     * @param keyword 搜索关键字
     * @param viewScope 视图范围：SELF-仅自己创建，ALL-全部（可为空，默认为SELF）
     * @return 合同列表（仅含contractId, contractNo, carrierName）
     */
    List<Map<String, Object>> searchContracts(String keyword, String viewScope);
}
