package com.erp.mapper.transport;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.entity.transport.OutsourceTransportSettlement;
import com.erp.controller.transport.dto.OutsourceSettlementResponse;
import com.erp.controller.transport.dto.SettlementSlipResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 委外运输结算单 Mapper
 */
@Mapper
public interface OutsourceTransportSettlementMapper extends BaseMapper<OutsourceTransportSettlement> {

    /**
     * 统计合同编号为空的结算单数量（游离数据）
     *
     * @return 合同编号为空的数量
     */
    Long countByContractIdIsNull();

    /**
     * 分页查询结算单列表
     *
     * @param page 分页对象
     * @param contractId 合同编号（可选）
     * @param contractNo 合同单号（可选）
     * @param status 状态（可选）
     * @param carrierName 承运方名称（可选，模糊查询）
     * @param settlementNo 结算单编号（可选，模糊查询）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 分页结果
     */
    IPage<OutsourceTransportSettlement> selectPageList(
            IPage<OutsourceTransportSettlement> page,
            @Param("contractId") Integer contractId,
            @Param("contractNo") String contractNo,
            @Param("status") String status,
            @Param("carrierName") String carrierName,
            @Param("settlementNo") String settlementNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 分页查询统计
     *
     * @param contractId 合同编号（可选）
     * @param contractNo 合同单号（可选）
     * @param status 状态（可选）
     * @param carrierName 承运方名称（可选，模糊查询）
     * @param settlementNo 结算单编号（可选，模糊查询）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 记录总数
     */
    Long selectPageCount(
            @Param("contractId") Integer contractId,
            @Param("contractNo") String contractNo,
            @Param("status") String status,
            @Param("carrierName") String carrierName,
            @Param("settlementNo") String settlementNo,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 根据ID查询详情（包含关联的总磅单信息）
     *
     * @param settlementId 结算单编号
     * @return 结算单详情（含总磅单列表）
     */
    OutsourceSettlementResponse selectDetailById(@Param("settlementId") Integer settlementId);

    /**
     * 统计指定状态的结算单数量
     *
     * @param status 状态
     * @return 数量
     */
    Integer countByStatus(@Param("status") String status);

    /**
     * 基于总磅单分页查询可结算记录
     * 每个总磅单代表一趟，关联的总磅单数量就是趟数
     *
     * @param page 分页对象
     * @param contractCode 合同单号（可选）
     * @param searchKeyword 搜索关键词（可选，车牌号/司机姓名/总磅单号）
     * @param includeUnrelated 是否包含未关联合同的总磅单
     * @param excludeSettled 是否排除已关联结算单的总磅单
     * @return 分页结果
     */
    IPage<SettlementSlipResponse> selectAvailableSlipsForSettlement(
            IPage<?> page,
            @Param("contractCode") String contractCode,
            @Param("searchKeyword") String searchKeyword,
            @Param("includeUnrelated") Boolean includeUnrelated,
            @Param("excludeSettled") Boolean excludeSettled
    );

    /**
     * 统计可结算的总磅单数量
     *
     * @param contractCode 合同单号（可选）
     * @param searchKeyword 搜索关键词（可选）
     * @param includeUnrelated 是否包含未关联合同的总磅单
     * @param excludeSettled 是否排除已关联结算单的总磅单
     * @return 数量
     */
    Long countAvailableSlipsForSettlement(
            @Param("contractCode") String contractCode,
            @Param("searchKeyword") String searchKeyword,
            @Param("includeUnrelated") Boolean includeUnrelated,
            @Param("excludeSettled") Boolean excludeSettled
    );
}
