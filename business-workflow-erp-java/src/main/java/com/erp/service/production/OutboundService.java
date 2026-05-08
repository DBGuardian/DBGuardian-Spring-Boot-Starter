package com.erp.service.production;

import com.erp.controller.production.dto.AuditOutboundRequest;
import com.erp.controller.production.dto.CreateOutboundRequest;
import com.erp.controller.production.dto.OutboundDetailResponse;
import com.erp.controller.production.dto.OutboundListResponse;
import com.erp.controller.production.dto.OutboundPageRequest;

/**
 * 出库单服务接口
 */
public interface OutboundService {

    /**
     * 创建出库单
     * @param request 创建请求
     * @return 创建后的出库单详情
     */
    OutboundDetailResponse createOutbound(CreateOutboundRequest request);

    /**
     * 更新出库单（仅限待审核状态）
     * @param request 更新请求
     * @return 更新后的出库单详情
     */
    OutboundDetailResponse updateOutbound(CreateOutboundRequest request);

    /**
     * 审核出库单
     * @param request 审核请求
     */
    void auditOutbound(AuditOutboundRequest request);

    /**
     * 获取出库单详情
     * @param outboundId 出库单编号
     * @return 出库单详情
     */
    OutboundDetailResponse getOutboundDetail(Integer outboundId);

    /**
     * 根据出库单编号或出库单号获取出库单详情
     * @param outboundId 出库单编号
     * @param outboundNo 出库单号
     * @return 出库单详情
     */
    OutboundDetailResponse getOutboundDetail(Integer outboundId, String outboundNo);

    /**
     * 分页查询出库单列表
     * @param request 查询请求
     * @return 出库单列表响应
     */
    OutboundListResponse getOutboundPage(OutboundPageRequest request);
}
