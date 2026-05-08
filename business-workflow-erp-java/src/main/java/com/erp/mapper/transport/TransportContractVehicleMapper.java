package com.erp.mapper.transport;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.controller.transport.dto.ContractVehicleResponse;
import com.erp.entity.transport.TransportContractVehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 运输合同-车辆关联 Mapper
 */
@Mapper
public interface TransportContractVehicleMapper extends BaseMapper<TransportContractVehicle> {

    /**
     * 根据合同编号查询关联车辆列表
     */
    List<ContractVehicleResponse> selectByContractId(@Param("contractId") Integer contractId);

    /**
     * 根据车辆编号查询关联合同列表
     */
    List<ContractVehicleResponse> selectByVehicleId(@Param("vehicleId") Integer vehicleId);

    /**
     * 检查是否已关联
     */
    Integer checkExist(@Param("contractId") Integer contractId, @Param("vehicleId") Integer vehicleId);

    /**
     * 删除关联关系
     */
    int deleteByContractAndVehicle(@Param("contractId") Integer contractId, @Param("vehicleId") Integer vehicleId);

    /**
     * 根据合同编号删除所有关联
     */
    int deleteByContractId(@Param("contractId") Integer contractId);

    /**
     * 查询合同已关联的车辆ID列表
     */
    List<Integer> selectVehicleIdsByContractId(@Param("contractId") Integer contractId);

    /**
     * 批量查询车辆关联的合同信息（Map<vehicleId, contractList>）
     */
    List<ContractVehicleResponse> selectContractsByVehicleIds(@Param("vehicleIds") List<Integer> vehicleIds);
}
