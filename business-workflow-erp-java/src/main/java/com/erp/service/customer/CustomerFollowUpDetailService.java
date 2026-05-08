package com.erp.service.customer;

import com.erp.controller.customer.dto.CustomerFollowUpDetailCreateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpDetailResponse;
import com.erp.controller.customer.dto.CustomerFollowUpDetailUpdateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpWithDetailsResponse;

import java.util.List;

/**
 * 客户跟进明细服务接口
 */
public interface CustomerFollowUpDetailService {

    /**
     * 获取跟进记录及明细
     *
     * @param followUpId 跟进记录编号
     * @return 带明细的跟进记录
     */
    CustomerFollowUpWithDetailsResponse getFollowUpWithDetails(Integer followUpId);

    /**
     * 查询跟进记录的所有明细
     *
     * @param followUpId 跟进记录编号
     * @return 明细列表
     */
    List<CustomerFollowUpDetailResponse> getDetailsByFollowUpId(Integer followUpId);

    /**
     * 新增跟进明细
     *
     * @param request 明细创建请求
     * @return 新增的明细
     */
    CustomerFollowUpDetailResponse createDetail(CustomerFollowUpDetailCreateRequest request);

    /**
     * 更新跟进明细
     *
     * @param request 明细更新请求
     * @return 更新后的明细
     */
    CustomerFollowUpDetailResponse updateDetail(CustomerFollowUpDetailUpdateRequest request);

    /**
     * 完成跟进明细（将状态修改为已完成）
     *
     * @param detailId 明细编号
     * @return 完成后的明细
     */
    CustomerFollowUpDetailResponse completeDetail(Integer detailId);

    /**
     * 删除跟进明细
     *
     * @param detailId 明细编号
     * @return 删除的记录数
     */
    int deleteDetail(Integer detailId);

    /**
     * 批量删除跟进明细
     *
     * @param detailIds 明细ID列表
     * @return 删除的记录数
     */
    int deleteDetailsBatch(List<Integer> detailIds);
}
