package com.erp.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigPageRequest;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigResponse;
import com.erp.controller.system.dto.HazardousWasteCategoryConfigUpdateRequest;

/**
 * 危险废物类别配置业务服务
 */
public interface HazardousWasteCategoryService {

    /**
     * 查询废物类别的限额配置
     *
     * @param wasteCategory 废物类别名称
     * @return 配置信息
     */
    HazardousWasteCategoryConfigResponse getCategoryConfig(String wasteCategory);

    /**
     * 分页查询废物类别限额配置
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<HazardousWasteCategoryConfigResponse> getCategoryConfigPage(
            HazardousWasteCategoryConfigPageRequest request);

    /**
     * 更新废物类别的限额配置（仅支持限额、开始时间、结束时间）
     *
     * @param request 更新请求
     */
    void updateCategoryConfig(HazardousWasteCategoryConfigUpdateRequest request);
}
