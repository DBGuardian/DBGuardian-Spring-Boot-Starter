package com.erp.service.transport.impl;

import com.erp.controller.transport.dto.ContractVehicleResponse;
import com.erp.mapper.transport.TransportContractVehicleMapper;
import com.erp.service.transport.TransportContractVehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 运输合同-车辆关联服务实现
 * 说明：该服务仅用于查询合同关联的车辆信息，车辆关联关系已在 TransportContractService 中同步管理
 */
@Slf4j
@Service
public class TransportContractVehicleServiceImpl implements TransportContractVehicleService {

    @Autowired
    private TransportContractVehicleMapper vehicleMapper;

    @Override
    public List<ContractVehicleResponse> getVehiclesByContractId(Integer contractId) {
        return vehicleMapper.selectByContractId(contractId);
    }

    @Override
    public List<ContractVehicleResponse> getContractsByVehicleId(Integer vehicleId) {
        return vehicleMapper.selectByVehicleId(vehicleId);
    }
}
