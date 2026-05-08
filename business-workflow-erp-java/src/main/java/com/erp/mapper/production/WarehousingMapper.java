package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.production.dto.WarehousingDetailResponse;
import com.erp.controller.production.dto.WarehousingPageResponse;
import com.erp.controller.production.dto.WarehousingWithSettlementVO;
import com.erp.entity.production.Warehousing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 入库单 Mapper
 */
@Mapper
public interface WarehousingMapper extends BaseMapper<Warehousing> {

    /**
     * 统计入库单号是否存在
     */
    int countByWarehousingNo(@Param("warehousingNo") String warehousingNo);

    /**
     * 查询指定前缀下的最大入库单号
     */
    String selectMaxWarehousingNoByPrefix(@Param("prefix") String prefix);

    /**
     * 分页查询入库单列表
     */
    IPage<WarehousingPageResponse> selectWarehousingPage(
            Page<WarehousingPageResponse> page,
            @Param("keyword") String keyword,
            @Param("weighingSlipNo") String weighingSlipNo,
            @Param("dispatchCode") String dispatchCode,
            @Param("status") String status,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection,
            @Param("creatorFilter") Integer creatorFilter,
            @Param("independentOnly") Boolean independentOnly
    );

    /**
     * 根据入库单编号查询
     */
    Warehousing selectByWarehousingId(@Param("warehousingId") Integer warehousingId);

    /**
     * 根据入库单号查询入库单信息
     */
    Warehousing selectByWarehousingNo(@Param("warehousingNo") String warehousingNo);

    /**
     * 根据结算单ID查询关联的入库记录编码列表
     */
    List<String> selectWarehousingCodesBySettlementId(@Param("settlementId") Long settlementId);

    /**
     * 更新入库单状态
     * @param warehousingId 入库单编号
     * @param status 新状态
     * @param operatorId 操作人ID
     * @return 更新行数
     */
    int updateStatus(@Param("warehousingId") Integer warehousingId,
                    @Param("status") String status,
                    @Param("operatorId") Integer operatorId);

    /**
     * 批量更新入库单状态
     * @param warehousingIds 入库单编号列表
     * @param status 新状态
     * @param operatorId 操作人ID
     * @return 更新行数
     */
    int batchUpdateStatus(@Param("warehousingIds") List<Integer> warehousingIds,
                         @Param("status") String status,
                         @Param("operatorId") Integer operatorId);

    /**
     * 按状态统计入库单数量
     * @return 状态统计结果
     */
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 根据入库单编码列表批量查询入库单信息
     * 用于优化N+1查询问题，避免循环查询
     *
     * @param warehousingNos 入库单编码列表
     * @return 入库单列表
     */
    List<Warehousing> selectBatchByNos(@Param("warehousingNos") List<String> warehousingNos);

    /**
     * 批量查询入库单详情信息，包括关联的合同、运输和辅计量数据
     * 用于优化结算单审核页面的查询性能，实现真正的数据库级批量查询
     *
     * @param warehousingIds 入库单ID列表
     * @return 入库单详情响应列表
     */
    List<com.erp.controller.production.dto.WarehousingDetailResponse> selectWarehousingDetailsBatch(@Param("warehousingIds") List<Integer> warehousingIds);

    /**
     * 查询最近30天内有审核记录的入库单ID列表
     * 用于缓存预热，预加载活跃的入库单数据
     *
     * @return 入库单ID列表
     */
    List<Integer> selectActiveWarehousingIdsLast30Days();

    /**
     * 批量更新入库单状态为待结算
     * 用于结算单删除时，将关联的入库单状态回退到待结算状态
     *
     * @param codes 入库单编码列表
     * @return 更新行数
     */
    int batchUpdateStatusToPendingSettlement(@Param("codes") List<String> codes);

    /**
     * 批量更新入库单状态为已结算
     * 用于结算单审核通过时，将关联的入库单状态更新为已结算状态
     *
     * @param codes 入库单编码列表
     * @param operatorId 操作人ID
     * @return 更新行数
     */
    int batchUpdateStatusToSettled(@Param("codes") List<String> codes, @Param("operatorId") Integer operatorId);

    /**
     * 根据合同号获取入库单列表（含业务链和结算状态）
     * 业务链：通知单 → 运输单 → 入库单
     * 返回每个入库单的关联信息，以及是否已结算的标识
     *
     * @param contractCode 合同号
     * @return 入库单列表（含结算状态）
     */
    List<WarehousingWithSettlementVO> selectWarehousingWithChainByContract(@Param("contractCode") String contractCode);
}


