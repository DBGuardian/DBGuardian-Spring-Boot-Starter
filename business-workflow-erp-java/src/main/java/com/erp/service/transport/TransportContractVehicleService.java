package com.erp.service.transport;

import com.erp.controller.transport.dto.ContractVehicleResponse;

import java.util.List;

/**
 * 运输合同-车辆关联服务接口
 * 说明：该服务仅用于查询合同关联的车辆信息，车辆关联关系已在 TransportContractService 中同步管理
 */
public interface TransportContractVehicleService {

    /**
     * 根据合同编号查询关联车辆列表
     */
    List<ContractVehicleResponse> getVehiclesByContractId(Integer contractId);

    /**
     * 根据车辆编号查询关联合同列表
     */
    List<ContractVehicleResponse> getContractsByVehicleId(Integer vehicleId);
}
