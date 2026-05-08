package com.erp.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.system.dto.HazardousWasteCreateRequest;
import com.erp.controller.system.dto.HazardousWasteDetailResponse;
import com.erp.controller.system.dto.HazardousWasteImportResponse;
import com.erp.controller.system.dto.HazardousWastePageRequest;
import com.erp.controller.system.dto.HazardousWastePageResponse;
import com.erp.controller.system.dto.HazardousWasteUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 危险废物名录业务服务
 */
public interface HazardousWasteItemService {

    /**
     * 分页查询
     *
     * @param request 查询条件
     * @return 分页数据
     */
    IPage<HazardousWastePageResponse> getWasteItemPage(HazardousWastePageRequest request);

    /**
     * 查询详情
     *
     * @param itemId 条目编号
     * @return 详情
     */
    HazardousWasteDetailResponse getWasteItemDetail(Integer itemId);

    /**
     * 新增危废条目
     *
     * @param request 请求参数
     */
    void createWasteItem(HazardousWasteCreateRequest request);

    /**
     * 更新危废条目
     *
     * @param itemId  条目编号
     * @param request 请求参数
     */
    void updateWasteItem(Integer itemId, HazardousWasteUpdateRequest request);

    /**
     * 删除危废条目
     *
     * @param itemId 条目编号
     */
    void deleteWasteItem(Integer itemId);

    /**
     * 批量删除危废条目
     *
     * @param ids 条目编号列表
     */
    void batchDeleteWasteItems(List<Integer> ids);

    /**
     * 批量导入
     *
     * @param file Excel文件
     * @return 导入结果
     */
    HazardousWasteImportResponse importWasteItems(MultipartFile file);

    /**
     * 导出数据
     *
     * @param request 查询条件
     * @return 导出列表
     */
    List<HazardousWasteDetailResponse> listForExport(HazardousWastePageRequest request);

    /**
     * 获取废物类别列表
     *
     * @return 废物类别列表
     */
    List<String> getWasteCategoryList();
}


