package com.erp.mapper.settlement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.settlement.BusinessFeeSettlementRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务结算单 ↔ 危废结算单 关联表 Mapper
 */
@Mapper
public interface BusinessFeeSettlementRelMapper extends BaseMapper<BusinessFeeSettlementRel> {

    /**
     * 按业务序号查询关联的所有危废结算单
     *
     * @param businessSeq 业务序号
     * @return 关联记录列表
     */
    @Select("SELECT * FROM BUSINESS_FEE_SETTLEMENT_REL WHERE 业务序号 = #{businessSeq} ORDER BY 关联编号 ASC")
    List<BusinessFeeSettlementRel> selectByBusinessSeq(@Param("businessSeq") Integer businessSeq);

    /**
     * 按危废结算单编号查询关联的业务序号列表
     *
     * @param settlementId 危废结算单编号
     * @return 关联记录列表
     */
    @Select("SELECT * FROM BUSINESS_FEE_SETTLEMENT_REL WHERE 危废结算单编号 = #{settlementId}")
    List<BusinessFeeSettlementRel> selectBySettlementId(@Param("settlementId") Integer settlementId);
}
