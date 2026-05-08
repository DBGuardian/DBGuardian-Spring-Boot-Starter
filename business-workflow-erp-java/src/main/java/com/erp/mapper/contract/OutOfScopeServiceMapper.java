package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.OutOfScopeService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 价外服务Mapper接口
 */
@Mapper
public interface OutOfScopeServiceMapper extends BaseMapper<OutOfScopeService> {

    /**
     * 根据业务类型和业务ID查询价外服务列表
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 价外服务列表
     */
    List<OutOfScopeService> selectByBusiness(@Param("businessType") String businessType, @Param("businessId") Integer businessId);

    /**
     * 根据业务类型和业务ID删除价外服务
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 删除的记录数
     */
    int deleteByBusiness(@Param("businessType") String businessType, @Param("businessId") Integer businessId);

    /**
     * 根据业务类型和业务ID计算总金额
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 总金额
     */
    java.math.BigDecimal selectTotalAmountByBusiness(@Param("businessType") String businessType, @Param("businessId") Integer businessId);
}