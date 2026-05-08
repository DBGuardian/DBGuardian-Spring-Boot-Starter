package com.erp.mapper.transport;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.transport.TransportContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 运输合同 Mapper
 *
 * <p>基础 CRUD 由 BaseMapper 提供；此处声明自定义分页查询方法。</p>
 */
@Mapper
public interface TransportContractMapper extends BaseMapper<TransportContract> {

    /**
     * 分页查询运输合同列表
     *
     * @param contractNo        合同单号（模糊）
     * @param carrierName        承运方名称（模糊）
     * @param signingType        签约类型（精确）
     * @param settlementMethod   结算方式（精确）
     * @param status             合同状态（精确）
     * @param signTimeStart     签订时间起
     * @param signTimeEnd       签订时间止
     * @param sortField         排序字段
     * @param sortOrder         排序方向（asc/desc）
     * @param creatorFilter     制单人ID过滤（viewScope=SELF时传入当前用户ID，viewScope=ALL时传null）
     * @param offset            偏移量
     * @param size              每页条数
     * @return 合同列表
     */
    List<TransportContract> selectPageList(
            @Param("contractNo")        String contractNo,
            @Param("carrierName")       String carrierName,
            @Param("signingType")      String signingType,
            @Param("settlementMethod") String settlementMethod,
            @Param("status")           String status,
            @Param("signTimeStart")    String signTimeStart,
            @Param("signTimeEnd")      String signTimeEnd,
            @Param("sortField")        String sortField,
            @Param("sortOrder")        String sortOrder,
            @Param("creatorFilter")    Integer creatorFilter,
            @Param("offset")           long offset,
            @Param("size")             long size
    );

    /**
     * 查询总数（条件与 selectPageList 保持一致）
     */
    long selectPageCount(
            @Param("contractNo")        String contractNo,
            @Param("carrierName")       String carrierName,
            @Param("signingType")      String signingType,
            @Param("settlementMethod") String settlementMethod,
            @Param("status")           String status,
            @Param("signTimeStart")    String signTimeStart,
            @Param("signTimeEnd")      String signTimeEnd,
            @Param("creatorFilter")    Integer creatorFilter
    );

    /**
     * 统计各状态合同数量
     *
     * @return 各状态合同数量 {draft, reviewing, executing, completed}
     */
    Map<String, Long> selectStatusCounts();

    /**
     * 运输合同查询（关联车辆信息）
     * 查询合同编号、合同单号、承运方名称
     * 关联查询 TRANSPORT_CONTRACT_VEHICLE 车辆编号
     * 关联 VEHICLE 获取车牌号
     * 关联 DISPATCH_ORDER 统计运输车辆号牌为空的数量
     *
     * @return 合同查询结果列表
     */
    List<Map<String, Object>> selectContractWithVehicleList();

    /**
     * 统计指定合同关联的车辆在 DISPATCH_ORDER 中运输车辆号牌为空的记录数量
     * 通过 TRANSPORT_CONTRACT_VEHICLE 的车辆编号关联 VEHICLE 的车牌号
     * 与 DISPATCH_ORDER 的运输车辆号牌相等
     *
     * @param contractId 合同编号
     * @return 运输车辆号牌为空的数量
     */
    Long countDispatchPlateEmptyByContractId(@Param("contractId") Integer contractId);

    /**
     * 搜索运输合同（下拉框用）
     * 支持按合同单号、承运方名称搜索
     *
     * @param keyword 搜索关键字
     * @param viewScope 视图范围：SELF-仅自己创建，ALL-全部
     * @param creatorId 创建人编号（当 viewScope=SELF 时使用）
     * @return 合同列表（仅含contractId, contractNo, carrierName）
     */
    List<Map<String, Object>> searchContracts(
            @Param("keyword") String keyword,
            @Param("viewScope") String viewScope,
            @Param("creatorId") Integer creatorId);
}
