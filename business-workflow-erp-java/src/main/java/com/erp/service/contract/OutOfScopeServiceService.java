package com.erp.service.contract;

import com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO;
import com.erp.entity.contract.OutOfScopeService;

import java.util.List;


/**
 * 价外服务管理服务接口
 */
public interface OutOfScopeServiceService {

    /**
     * 根据报价单ID查询价外服务列表
     *
     * @param quotationId 报价单ID
     * @return 价外服务列表
     */
    List<OutOfScopeService> listByQuotation(Integer quotationId);

    /**
     * 为报价单新增价外服务（批量）
     *
     * @param quotationId 报价单ID
     * @param services 价外服务列表
     * @param createdBy 创建者ID
     * @return 新增的价外服务列表
     */
    List<OutOfScopeService> createForQuotation(Integer quotationId, List<OutOfScopeServiceCreateDTO> services, Integer createdBy);

    /**
     * 根据合同ID查询价外服务列表
     *
     * @param contractId 合同ID
     * @return 价外服务列表
     */
    List<OutOfScopeService> listByContract(Integer contractId);

    /**
     * 为合同新增价外服务（批量）
     *
     * @param contractId 合同ID
     * @param services 价外服务列表
     * @param createdBy 创建者ID
     * @return 新增的价外服务列表
     */
    List<OutOfScopeService> createForContract(Integer contractId, List<OutOfScopeServiceCreateDTO> services, Integer createdBy);

    /**
     * 更新指定价外服务
     *
     * @param id 价外服务ID
     * @param dto 更新数据
     * @param updatedBy 更新者ID
     * @return 更新后的价外服务
     */
    OutOfScopeService update(Integer id, OutOfScopeServiceCreateDTO dto, Integer updatedBy);

    /**
     * 删除指定价外服务
     *
     * @param id 价外服务ID
     */
    void delete(Integer id);
}
