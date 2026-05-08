package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.BusinessContractWasteItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 业务合同报价组 Mapper
 */
@Mapper
public interface BusinessContractWasteItemMapper extends BaseMapper<BusinessContractWasteItem> {

    /**
     * 根据业务合同ID查询报价组列表
     */
    List<BusinessContractWasteItem> selectByContractId(Integer contractId);

    /**
     * 根据业务合同ID删除报价组
     */
    int deleteByContractId(Integer contractId);
}
