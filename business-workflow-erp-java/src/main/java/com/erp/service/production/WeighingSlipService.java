package com.erp.service.production;

import com.erp.controller.production.dto.CreateWeighingSlipRequest;
import com.erp.controller.production.dto.UpdateWeighingSlipRequest;
import com.erp.controller.production.dto.WeighingSlipInfoResponse;
import com.erp.controller.production.dto.WeighingSlipListResponse;
import com.erp.controller.production.dto.WeighingSlipPageRequest;

/**
 * 总磅单服务接口
 */
public interface WeighingSlipService {

    /**
     * 创建总磅单
     * @param request 创建请求
     * @return 总磅单信息
     */
    WeighingSlipInfoResponse createWeighingSlip(CreateWeighingSlipRequest request);

    /**
     * 根据总磅单号获取总磅单信息
     * @param weighingSlipNo 总磅单号
     * @return 总磅单信息
     */
    WeighingSlipInfoResponse getWeighingSlipInfo(String weighingSlipNo);

    /**
     * 更新总磅单
     * @param request 更新请求
     * @return 总磅单信息
     */
    WeighingSlipInfoResponse updateWeighingSlip(UpdateWeighingSlipRequest request);

    /**
     * 分页查询总磅单列表
     * @param request 查询请求
     * @return 总磅单列表响应
     */
    WeighingSlipListResponse getWeighingSlipPage(WeighingSlipPageRequest request);
}



