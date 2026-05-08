package com.erp.mapper.finance;

import com.erp.controller.finance.dto.AvailableWarehousingVO;
import com.erp.controller.finance.dto.SettledQuantityDTO;
import com.erp.controller.finance.dto.TransportRecordDTO;
import com.erp.controller.finance.dto.WarehousingWasteDetailVO;
import com.erp.controller.finance.dto.WarehousingWasteDetailWithContractRequest;
import com.erp.controller.finance.dto.WarehousingWasteDetailWithContractVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 财务管理Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface FinanceMapper {
    // TODO: 定义财务管理相关数据访问方法
    // 注意：等有对应的实体类后，再继承 BaseMapper<Entity>

    /**
     * 根据合同号获取可结算的入库单列表
     * 业务链：合同 → 收运通知单 → 运输单 → 入库单（未被其他结算单引用）
     *
     * @param contractCode 合同号
     * @return 可结算入库单列表
     */
    List<AvailableWarehousingVO> selectAvailableWarehousingByContract(@Param("contractCode") String contractCode);

    /**
     * 根据入库单号列表获取入库单危废明细
     *
     * @param warehousingCodes 入库单号列表
     * @return 入库单危废明细列表
     */
    List<WarehousingWasteDetailVO> selectWarehousingWasteDetailsByCodes(@Param("warehousingCodes") List<String> warehousingCodes);

    /**
     * 根据入库单号列表查询入库单详情
     *
     * @param warehousingCodes 入库单号列表
     * @return 入库单详情列表
     */
    List<AvailableWarehousingVO> selectWarehousingDetailsByCodes(@Param("warehousingCodes") List<String> warehousingCodes);

    /**
     * 根据合同号查询运输记录
     *
     * @param contractCode 合同号
     * @return 运输记录列表
     */
    List<TransportRecordDTO> selectTransportRecordsByContract(@Param("contractCode") String contractCode);

    /**
     * 获取入库危废明细（含合同匹配信息）
     * 后端统一处理入库数据与合同数据的匹配
     * 匹配条件：危废条目编号 + 废物名称 + 废物代码
     * 关联校验：contractId + contractNo + 入库单编号（双层锁定：入库单号+入库单编号必须同时匹配）
     *
     * @param warehousingList 入库单信息列表（每个对象包含入库单号和入库单编号）
     * @param contractId 合同编号（自增主键）
     * @param contractNo 合同号（业务可见编号）
     * @return 匹配后的完整数据列表
     */
    List<WarehousingWasteDetailWithContractVO> selectWarehousingWasteDetailsWithContract(
            @Param("warehousingList") List<WarehousingWasteDetailWithContractRequest.WarehousingItemDTO> warehousingList,
            @Param("contractId") Integer contractId,
            @Param("contractNo") String contractNo);

    /**
     * 查询已结算入库量（总价包干专用）
     *
     * @param contractId 合同编号（自增主键）
     * @param contractNo 合同号（业务可见编号）
     * @param wasteCode 废物代码
     * @return 已结算量信息
     */
    SettledQuantityDTO selectSettledQuantityByContractAndWasteCode(
            @Param("contractId") Integer contractId,
            @Param("contractNo") String contractNo,
            @Param("wasteCode") String wasteCode);
}











































