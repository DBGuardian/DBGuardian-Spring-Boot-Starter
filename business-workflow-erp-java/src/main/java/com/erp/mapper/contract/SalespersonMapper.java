package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.Salesperson;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务员基本信息 Mapper
 *
 * 对应表：SALESPERSON
 */
@Mapper
public interface SalespersonMapper extends BaseMapper<Salesperson> {
}
