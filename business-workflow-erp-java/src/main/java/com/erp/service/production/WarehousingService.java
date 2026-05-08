package com.erp.service.production;

import com.erp.controller.production.dto.BatchCreateWarehousingRequest;
import com.erp.controller.production.dto.BatchCreateWarehousingResponse;
import com.erp.controller.production.dto.UpdateWarehousingRequest;
import com.erp.controller.production.dto.WarehousingDetailResponse;
import com.erp.controller.production.dto.WarehousingListResponse;
import com.erp.controller.production.dto.WarehousingPageRequest;
import com.erp.controller.production.dto.WarehousingPageResponse;
import com.erp.controller.production.dto.WarehousingStat;
import com.erp.controller.production.dto.WarehousingWithSettlementVO;

import java.util.List;
import java.util.Map;

/**
 * 入库单服务接口
 */
public interface WarehousingService {

    /**
     * 批量创建入库单
     * @param request 批量创建请求
     * @return 创建结果
     */
    BatchCreateWarehousingResponse batchCreateWarehousing(BatchCreateWarehousingRequest request);

    /**
     * 分页查询入库单列表
     * @param request 查询请求
     * @return 入库单列表响应
     */
    WarehousingListResponse getWarehousingPage(WarehousingPageRequest request);

    /**
     * 获取入库单详情
     * @param warehousingId 入库单编号
     * @return 入库单详情
     */
    WarehousingDetailResponse getWarehousingDetail(Integer warehousingId);

    /**
     * 批量获取入库单详情
     * @param warehousingIds 入库单ID列表
     * @return 入库单详情列表
     */
    List<WarehousingDetailResponse> getWarehousingDetailsBatch(List<Integer> warehousingIds);

    /**
     * 更新入库单
     * @param request 更新请求
     * @return 更新后的入库单详情
     */
    WarehousingDetailResponse updateWarehousing(UpdateWarehousingRequest request);


    /**
     * 删除入库单
     * @param warehousingId 入库单编号
     */
    void deleteWarehousing(Integer warehousingId);

    /**
     * 更新入库单状态
     * @param warehousingId 入库单编号
     * @param status 新状态
     * @param operatorId 操作人ID
     * @return 更新是否成功
     */
    boolean updateWarehousingStatus(Integer warehousingId, String status, Integer operatorId);

    /**
     * 批量更新入库单状态
     * @param warehousingIds 入库单编号列表
     * @param status 新状态
     * @param operatorId 操作人ID
     * @return 更新行数
     */
    int batchUpdateWarehousingStatus(List<Integer> warehousingIds, String status, Integer operatorId);

    /**
     * 获取状态统计信息
     * @return 各状态的统计信息
     */
    Map<String, Long> getStatusStatistics();

    /**
     * 根据合同号获取入库单列表（含业务链和结算状态）
     * 业务链：收运通知单 → 运输单 → 入库单
     *
     * @param contractCode 合同号
     * @return 入库单列表（含结算状态）
     */
    List<WarehousingWithSettlementVO> getWarehousingWithChainByContract(String contractCode);
}


