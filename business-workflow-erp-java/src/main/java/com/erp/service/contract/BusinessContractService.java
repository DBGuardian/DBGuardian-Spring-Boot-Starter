package com.erp.service.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.contract.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 业务合作合同 Service 接口
 */
public interface BusinessContractService {

    /**
     * 分页查询
     */
    IPage<BusinessContractPageResponse> getPage(BusinessContractPageRequest request);

    /**
     * 新增合同（支持上传合同文件）
     *
     * @return 新增合同的主键 contractId
     */
    Integer create(BusinessContractCreateRequest request, MultipartFile file);

    /**
     * 查询详情
     */
    BusinessContractDetailResponse getDetail(Integer contractId);

    /**
     * 更新合同（支持重新上传合同文件）
     */
    void update(Integer contractId, BusinessContractCreateRequest request, MultipartFile file);

    /**
     * 更新合同状态
     */
    void updateStatus(Integer contractId, BusinessContractStatusRequest request);

    /**
     * 逻辑删除合同（仅待审核状态可删除）
     */
    void delete(Integer contractId);

    /**
     * 搜索汇款用业务合同选项
     * 按关键词模糊匹配合同单号、甲方名称、业务员姓名，仅返回执行中合同，一次携带完整收款卡信息
     *
     * @param keyword 搜索关键词（可为空，返回全部执行中合同，最多20条）
     * @param viewScope 视图范围：SELF-仅自己创建，ALL-全部（可为空，默认为SELF）
     * @return 合同选项列表
     */
    List<BusinessContractRemittanceOptionDTO> searchRemittanceOptions(String keyword, String viewScope);

    /**
     * 根据危废合同ID查询关联业务合同
     * 危废合同与业务合同一对一，若不存在则返回 null
     *
     * @param hazardousContractId 危废合同编号
     * @return 关联业务合同详情，不存在时返回 null
     */
    BusinessContractDetailResponse getByHazardousContractId(Integer hazardousContractId);

    /**
     * 查询各状态合同数量统计
     *
     * @return map，包含 executing（执行中）、pendingAudit（待审核）、completed（已完结）
     */
    java.util.Map<String, Long> getStatistics();

    /**
     * 业务费结算专用合同列表查询（不分页）
     *
     * <p>返回状态为"执行中"或"已完结"的业务合同，每条记录包含：
     *   BUSINESS_CONTRACT 的合同编号、合同单号、业务员姓名、合同状态；
     *   JOIN CONTRACT 获取合同编号、合同号、甲方名称。
     * </p>
     *
     * @return 合同列表响应
     */
    com.erp.controller.contract.dto.BusinessSettlementContractListResponse getSettlementList();

    /**
     * 根据危废合同信息自动创建关联业务合同（仅含业务员基本信息）
     * 危废合同保存时若开启业务费结算开关，后端调用此方法自动生成一对一业务合同
     *
     * @param hazardousContractId 危废合同编号
     * @param salespersonId       业务员员工编码（可为空）
     * @param salespersonName     业务员姓名
     * @param salespersonPhone    业务员电话（可为空）
     * @return 新建业务合同主键 contractId
     */
    Integer createByHazardousContract(Integer hazardousContractId, Integer salespersonId,
                                      String salespersonName, String salespersonPhone);

    /**
     * 批量更新合同状态
     * 支持批量提交审核（待审核/已驳回 → 审核中）、批量撤回（审核中 → 待审核）等
     *
     * @param contractIds 合同ID列表
     * @param status     目标状态
     * @return 批量操作结果，包含成功ID列表和失败信息
     */
    com.erp.service.contract.dto.BusinessContractBatchUpdateResponse batchUpdateStatus(
            java.util.List<Integer> contractIds, String status);
}
