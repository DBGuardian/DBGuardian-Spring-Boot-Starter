package com.erp.service.transport;

import com.erp.controller.transport.dto.DispatchValidateResponse;
import com.erp.controller.transport.dto.TransportDispatchDetailRequest;
import com.erp.controller.transport.dto.TransportDispatchDetailResponse;
import com.erp.controller.transport.dto.TransportDispatchListResponse;
import com.erp.controller.transport.dto.TransportDispatchPageRequest;
import com.erp.controller.transport.dto.TransportDispatchPageResponse;

import java.util.List;

/**
 * 运输单服务
 */
public interface DispatchOrderService {

    /**
     * 创建运输单
     */
    TransportDispatchDetailResponse createDispatchOrder(TransportDispatchDetailRequest request);

    /**
     * 更新运输单
     */
    TransportDispatchDetailResponse updateDispatchOrder(TransportDispatchDetailRequest request);

    /**
     * 获取运输单详情
     */
    TransportDispatchDetailResponse getDispatchDetail(String dispatchCode, String noticeCode);

    /**
     * 校验运输单风险（合同缺失 / 超限）
     */
    DispatchValidateResponse validateDispatchOrder(String noticeCode, String dispatchCode);

    /**
     * 分页查询运输单列表
     */
    TransportDispatchListResponse getDispatchOrderList(TransportDispatchPageRequest request);

    /**
     * 生成运输单PDF并返回打印URL
     */
    String generateDispatchOrderPdf(String dispatchCode);

    /**
     * 批量生成运输单PDF并返回打印URL
     */
    String generateBatchDispatchOrderPdf(List<String> dispatchCodes);

    /**
     * 查询运输单列表用于导出（不分页，返回所有符合条件的数据）
     *
     * @param request 查询请求（筛选条件）
     * @return 运输单列表
     */
    List<TransportDispatchPageResponse> listDispatchOrdersForExport(TransportDispatchPageRequest request);
}


