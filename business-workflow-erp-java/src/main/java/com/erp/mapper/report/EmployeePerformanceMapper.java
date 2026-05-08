package com.erp.mapper.report;

import com.erp.controller.report.dto.EmployeePerformancePieResponse.EmployeeRawDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工业绩占比饼图 Mapper
 *
 * 功能描述：
 * - 按3个维度查询各在职员工的业绩数据
 * - CONTRACT_COUNT:    CONTRACT.创建人编码 → COUNT 合同数量
 * - WAREHOUSE_WEIGHT:  WAREHOUSING.仓管员编码 → SUM WAREHOUSING_WASTE_ITEM.实际收运数量
 * - SETTLEMENT_AMOUNT: SETTLEMENT.制单人编码 → SUM 结算金额
 */
@Mapper
public interface EmployeePerformanceMapper {

    /**
     * 查询各员工合同签订数量
     *
     * 统计口径：CONTRACT.签订时间（DATETIME）
     * 关联字段：CONTRACT.创建人编码 → EMPLOYEE.员工编码
     * 排除：合同状态 IN ('待审核', '已拒绝', '草稿') 或 签订时间 IS NULL
     *
     * @param startDate 开始日期（YYYY-MM-DD，含）
     * @param endDate   结束日期（YYYY-MM-DD，含）
     * @return 各员工合同签订数量列表
     */
    List<EmployeeRawDTO> selectContractCountByEmployee(
            @Param("startDate") String startDate,
            @Param("endDate")   String endDate);

    /**
     * 查询各员工入库重量（吨）
     *
     * 统计口径：WAREHOUSING_WASTE_ITEM.实际入库日期
     * 关联字段：WAREHOUSING.仓管员编码 → EMPLOYEE.员工编码
     * 过滤：实际收运数量 IS NOT NULL AND 实际收运数量 > 0
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 各员工入库重量列表
     */
    List<EmployeeRawDTO> selectWarehouseWeightByEmployee(
            @Param("startDate") String startDate,
            @Param("endDate")   String endDate);

    /**
     * 查询各员工结算金额（元）
     *
     * 统计口径：SETTLEMENT.创建时间
     * 关联字段：SETTLEMENT.制单人编码 → EMPLOYEE.员工编码
     * 过滤：结算类型 = 'RECEIVABLE'，状态 NOT IN ('草稿', '已驳回')
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 各员工结算金额列表
     */
    List<EmployeeRawDTO> selectSettlementAmountByEmployee(
            @Param("startDate") String startDate,
            @Param("endDate")   String endDate);
}
