package com.erp.mapper.customer;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.controller.customer.dto.CustomerFollowUpDetailResponse;
import com.erp.entity.customer.CustomerFollowUpDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户跟进明细Mapper接口
 *
 * @author ERP
 */
@Mapper
public interface CustomerFollowUpDetailMapper extends BaseMapper<CustomerFollowUpDetail> {

    /**
     * 根据跟进记录ID查询明细列表
     *
     * @param followUpId 跟进记录编号
     * @return 明细列表
     */
    List<CustomerFollowUpDetail> selectByFollowUpId(@Param("followUpId") Integer followUpId);

    /**
     * 根据跟进记录ID删除所有明细
     *
     * @param followUpId 跟进记录编号
     * @return 删除的记录数
     */
    int deleteByFollowUpId(@Param("followUpId") Integer followUpId);

    /**
     * 根据明细ID删除
     *
     * @param detailId 明细编号
     * @return 删除的记录数
     */
    int deleteByDetailId(@Param("detailId") Integer detailId);

    /**
     * 批量删除明细
     *
     * @param detailIds 明细ID列表
     * @return 删除的记录数
     */
    int deleteBatchByIds(@Param("detailIds") List<Integer> detailIds);

    /**
     * 查询明细详情（包含创建人姓名）
     *
     * @param detailId 明细编号
     * @return 明细详情
     */
    CustomerFollowUpDetailResponse selectDetailWithCreator(@Param("detailId") Integer detailId);
}
