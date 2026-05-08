package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.contract.dto.OutsourceProcessingContractPageResponse;
import com.erp.controller.contract.dto.OutsourceProcessingContractSelectResponse;
import com.erp.entity.contract.OutsourceProcessingContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 委外处理合同Mapper接口
 *
 * 对应表：OUTSOURCE_PROCESSING_CONTRACT
 */
@Mapper
public interface OutsourceProcessingContractMapper extends BaseMapper<OutsourceProcessingContract> {

    /**
     * 委外处理合同分页查询
     */
    IPage<OutsourceProcessingContract> selectContractPage(
            Page<OutsourceProcessingContract> page,
            @Param("contractNo") String contractNo,
            @Param("partyAName") String partyAName,
            @Param("contractStatus") String contractStatus,
            @Param("signTimeStart") java.time.LocalDateTime signTimeStart,
            @Param("signTimeEnd") java.time.LocalDateTime signTimeEnd,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder,
            @Param("creatorFilter") Integer creatorFilter
    );

    /**
     * 委外处理合同详情
     */
    OutsourceProcessingContract selectDetailById(@Param("contractId") Integer contractId);

    /**
     * 委外处理合同下拉列表
     */
    List<OutsourceProcessingContractSelectResponse> selectContractSelectList(@Param("keyword") String keyword);
}
