package com.erp.service.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.contract.dto.SalespersonCreateRequest;
import com.erp.controller.contract.dto.SalespersonDetailResponse;
import com.erp.controller.contract.dto.SalespersonPageRequest;
import com.erp.controller.contract.dto.SalespersonPageResponse;
import com.erp.controller.contract.dto.SalespersonSelectResponse;

import java.util.List;

/**
 * 业务员 Service 接口
 */
public interface SalespersonService {

    /** 分页查询 */
    IPage<SalespersonPageResponse> getPage(SalespersonPageRequest request);

    /** 关键词搜索（用于下拉选择） */
    IPage<SalespersonPageResponse> search(SalespersonPageRequest request);

    /** 获取下拉列表（支持关键词模糊搜索） */
    List<SalespersonSelectResponse> getSelectList(String keyword);

    /** 查询详情 */
    SalespersonDetailResponse getDetail(Integer salespersonId);

    /** 新增业务员，返回主键 */
    Integer create(SalespersonCreateRequest request);

    /** 更新业务员 */
    void update(Integer salespersonId, SalespersonCreateRequest request);

    /** 逻辑删除 */
    void delete(Integer salespersonId);

    /** 批量逻辑删除 */
    void batchDelete(List<Integer> salespersonIds);
}
