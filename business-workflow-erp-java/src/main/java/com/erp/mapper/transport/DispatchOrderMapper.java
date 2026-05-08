package com.erp.mapper.transport;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.transport.DispatchOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 运输单 Mapper
 */
@Mapper
public interface DispatchOrderMapper extends BaseMapper<DispatchOrder> {

    /**
     * 根据运输单号查询
     */
    DispatchOrder selectByDispatchCode(@Param("dispatchCode") String dispatchCode);

    /**
     * 根据收运通知单号查询
     */
    DispatchOrder selectByNoticeCode(@Param("noticeCode") String noticeCode);

    /**
     * 查询指定前缀下的最大运输单号
     */
    String selectMaxDispatchCodeByPrefix(@Param("prefix") String prefix);

    /**
     * 统计运输单号是否存在
     */
    int countByDispatchCode(@Param("dispatchCode") String dispatchCode);

    /**
     * 统计收运通知单是否已生成运输单
     */
    int countByNoticeCode(@Param("noticeCode") String noticeCode);

    /**
     * 使用行锁查询收运通知单是否已生成运输单（用于并发控制）
     * 通过锁定收运通知单记录，确保并发安全
     *
     * @param noticeCode 收运通知单号
     * @return 已存在的运输单，如果不存在返回null
     */
    DispatchOrder selectByNoticeCodeForUpdate(@Param("noticeCode") String noticeCode);

    /**
     * 运输单分页查询
     *
     * @param page           分页对象
     * @param dispatchCode  运输单号（模糊）
     * @param noticeCode    收运通知单号（模糊）
     * @param contractCode  合同号（模糊）
     * @param carrierName   承运单位名称（模糊）
     * @param driverName    驾驶员姓名（模糊）
     * @param plateNo       车辆号牌（模糊）
     * @param status         状态
     * @param locked         是否锁定
     * @param dispatcherId  调度员编码
     * @param startTime      创建时间开始
     * @param endTime        创建时间结束
     * @param sortField      排序字段
     * @param sortOrder      排序方向：asc/desc
     * @param creatorFilter  数据范围过滤：调度员编码（仅查看自己创建的数据）
     * @return 分页结果
     */
    IPage<DispatchOrder> selectDispatchOrderPage(
            Page<DispatchOrder> page,
            @Param("dispatchCode") String dispatchCode,
            @Param("noticeCode") String noticeCode,
            @Param("contractCode") String contractCode,
            @Param("carrierName") String carrierName,
            @Param("driverName") String driverName,
            @Param("plateNo") String plateNo,
            @Param("status") String status,
            @Param("locked") Boolean locked,
            @Param("dispatcherId") Integer dispatcherId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder,
            @Param("creatorFilter") Integer creatorFilter
    );
}


