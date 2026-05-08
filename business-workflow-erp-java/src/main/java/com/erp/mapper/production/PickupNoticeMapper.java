package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.production.PickupNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收运通知单Mapper接口
 *
 * 对应表：PICKUP_NOTICE
 */
@Mapper
public interface PickupNoticeMapper extends BaseMapper<PickupNotice> {

    /**
     * 收运通知单分页查询
     *
     * @param page           分页对象
     * @param noticeCode     收运通知单号（模糊）
     * @param companyName    产生单位名称（模糊）
     * @param contractCode   合同号（模糊）
     * @param customerId     客户编码
     * @param onsiteContact  现场联系人（模糊）
     * @param onsitePhone    现场联系电话（模糊）
     * @param status         状态
     * @param locked         是否锁定
     * @param creatorId      创建人编码
     * @param startTime      创建时间开始
     * @param endTime        创建时间结束
     * @param sortField      排序字段
     * @param sortOrder      排序方向：asc/desc
     * @return 分页结果
     */
    IPage<PickupNotice> selectPickupNoticePage(
            Page<PickupNotice> page,
            @Param("noticeCode") String noticeCode,
            @Param("companyName") String companyName,
            @Param("contractCode") String contractCode,
            @Param("customerId") Integer customerId,
            @Param("onsiteContact") String onsiteContact,
            @Param("onsitePhone") String onsitePhone,
            @Param("status") String status,
            @Param("locked") Boolean locked,
            @Param("creatorId") Integer creatorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 根据收运通知单号查询详情
     *
     * @param noticeCode 收运通知单号
     * @return 收运通知单实体（包含关联信息）
     */
    PickupNotice selectDetailByNoticeCode(@Param("noticeCode") String noticeCode);

    /**
     * 根据收运通知单编号查询详情
     *
     * @param noticeId 收运通知单编号
     * @return 收运通知单实体（包含关联信息）
     */
    PickupNotice selectDetailById(@Param("noticeId") Integer noticeId);

    /**
     * 根据合同号查询收运通知单列表
     *
     * @param contractCode 合同号
     * @return 收运通知单列表
     */
    List<PickupNotice> selectByContractCode(@Param("contractCode") String contractCode);

    /**
     * 根据客户编码查询收运通知单列表
     *
     * @param customerId 客户编码
     * @return 收运通知单列表
     */
    List<PickupNotice> selectByCustomerId(@Param("customerId") Integer customerId);

    /**
     * 检查收运通知单号是否存在
     *
     * @param noticeCode 收运通知单号
     * @return 存在返回1，不存在返回0
     */
    int countByNoticeCode(@Param("noticeCode") String noticeCode);

    /**
     * 查询指定前缀下最大的收运通知单号
     *
     * @param prefix 单号前缀（如 SYTTD-20250101-）
     * @return 当前最大单号，若无则返回 null
     */
    String selectMaxNoticeCodeByPrefix(@Param("prefix") String prefix);

    /**
     * 根据危废类别编号和时间范围统计已入库量
     *
     * @param categoryId 危废类别编号
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 已入库量总和（吨）
     */
    java.math.BigDecimal selectInboundAmountByCategoryAndTimeRange(
            @Param("categoryId") Integer categoryId,
            @Param("startTime") java.util.Date startTime,
            @Param("endTime") java.util.Date endTime);
}

