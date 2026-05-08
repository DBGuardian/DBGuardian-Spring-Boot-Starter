package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.production.WeighingSlipDispatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 总磅单-运输单关联Mapper
 */
@Mapper
public interface WeighingSlipDispatchMapper extends BaseMapper<WeighingSlipDispatch> {

    /**
     * 根据总磅单编号查询关联的运输单号列表
     * @param weighingSlipId 总磅单编号
     * @return 运输单号列表
     */
    List<String> selectDispatchCodesByWeighingSlipId(@Param("weighingSlipId") Integer weighingSlipId);

    /**
     * 根据运输单号查询是否已关联总磅单
     * @param dispatchCode 运输单号
     * @return 关联记录数量
     */
    int countByDispatchCode(@Param("dispatchCode") String dispatchCode);
}



