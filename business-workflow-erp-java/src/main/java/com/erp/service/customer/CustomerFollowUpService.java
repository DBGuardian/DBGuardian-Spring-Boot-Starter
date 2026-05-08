package com.erp.service.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.customer.dto.CustomerFollowUpCreateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpPageRequest;
import com.erp.controller.customer.dto.CustomerFollowUpResponse;
import com.erp.controller.customer.dto.CustomerFollowUpUpdateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpUpdateWithDetailsRequest;
import com.erp.controller.customer.dto.CustomerFollowUpWithDetailsResponse;

import java.util.List;

/**
 * 客户跟进服务接口
 */
public interface CustomerFollowUpService {

    /**
     * 查询当前用户的客户跟进记录列表
     *
     * @return 跟进记录列表
     */
    List<CustomerFollowUpResponse> getCurrentUserFollowUps();

    /**
     * 新增客户跟进记录
     *
     * @param request 请求参数
     * @return 跟进记录详情
     */
    CustomerFollowUpResponse createFollowUp(CustomerFollowUpCreateRequest request);

    /**
     * 根据客户ID查询跟进记录列表
     *
     * @param customerId 客户编码
     * @return 跟进记录列表
     */
    List<CustomerFollowUpResponse> getFollowUpsByCustomerId(Integer customerId);

    /**
     * 分页查询当前用户的客户跟进记录（支持多条件筛选、模糊查询、排序）
     * 查询的业务员必须为当前登录用户
     *
     * @param request 分页查询请求参数
     * @return 分页结果
     */
    IPage<CustomerFollowUpResponse> getFollowUpPage(CustomerFollowUpPageRequest request);

    /**
     * 批量删除客户跟进记录
     * 只能删除当前登录用户创建的记录
     *
     * @param followUpIds 跟进记录ID列表
     * @return 删除的记录数
     */
    int deleteBatchByIds(List<Integer> followUpIds);

    /**
     * 更新客户跟进记录
     *
     * @param request 更新请求参数
     * @return 更新后的跟进记录
     */
    CustomerFollowUpResponse updateFollowUp(CustomerFollowUpUpdateRequest request);

    /**
     * 更新客户跟进记录（包含明细差分修改）
     *
     * @param request 更新请求参数（包含基础信息和明细修改）
     * @return 更新后的跟进记录（包含明细）
     */
    CustomerFollowUpWithDetailsResponse updateFollowUpWithDetails(CustomerFollowUpUpdateWithDetailsRequest request);

    /**
     * 导出客户跟进记录Excel
     *
     * @param request 筛选参数
     * @return 跟进记录列表（包含明细，用于导出）
     */
    List<CustomerFollowUpWithDetailsResponse> exportFollowUps(CustomerFollowUpPageRequest request);
}

