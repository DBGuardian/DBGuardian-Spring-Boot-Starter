package com.erp.mapper.customer;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.entity.customer.CustomerFollowUp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户跟进Mapper接口
 *
 * @author ERP
 */
@Mapper
public interface CustomerFollowUpMapper extends BaseMapper<CustomerFollowUp> {

    /**
     * 查询当前用户的客户跟进记录列表
     *
     * @param employeeId 业务员编码（当前登录用户）
     * @return 跟进记录列表
     */
    List<CustomerFollowUp> selectByEmployeeId(@Param("employeeId") Integer employeeId);

    /**
     * 查询指定客户的跟进记录列表
     *
     * @param customerId 客户编码
     * @return 跟进记录列表
     */
    List<CustomerFollowUp> selectByCustomerId(@Param("customerId") Integer customerId);

    /**
     * 查询当前用户对指定客户的跟进记录列表
     *
     * @param customerId 客户编码
     * @param employeeId 业务员编码（当前登录用户）
     * @return 跟进记录列表
     */
    List<CustomerFollowUp> selectByCustomerIdAndEmployeeId(@Param("customerId") Integer customerId,
                                                            @Param("employeeId") Integer employeeId);

    /**
     * 分页查询当前用户的客户跟进记录（支持多条件筛选、排序）
     *
     * @param page        分页参数
     * @param employeeId  业务员编码（当前登录用户）
     * @param customerId  客户编码（精确匹配）
     * @param startDate   开始时间（yyyy-MM-dd）
     * @param endDate     结束时间（yyyy-MM-dd）
     * @param contactName 联系人姓名（模糊匹配）
     * @param contactPhone 联系人电话（模糊匹配）
     * @param creatorFilter 创建人编码过滤（数据范围控制，null表示不限制）
     * @param orderBy     排序字段
     * @param orderDirection 排序方向（asc/desc）
     * @return 分页结果
     */
    IPage<CustomerFollowUp> selectPageByEmployeeId(
            IPage<CustomerFollowUp> page,
            @Param("employeeId") Integer employeeId,
            @Param("customerId") Integer customerId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("contactName") String contactName,
            @Param("contactPhone") String contactPhone,
            @Param("creatorFilter") Integer creatorFilter,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );

    /**
     * 批量删除客户跟进记录
     *
     * @param followUpIds 跟进记录ID列表
     * @param employeeId  业务员编码（当前登录用户，只能删除自己创建的记录）
     * @return 删除的记录数
     */
    int deleteBatchByIds(@Param("followUpIds") List<Integer> followUpIds, @Param("employeeId") Integer employeeId);
}
