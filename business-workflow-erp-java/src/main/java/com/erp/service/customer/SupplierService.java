package com.erp.service.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.customer.dto.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 供应商管理服务接口
 * 支持供应商的增删改查、导入导出、分页查询等功能
 * 采用统一批处理接口设计，支持单个和批量操作
 */
public interface SupplierService {

    /**
     * 分页查询供应商列表
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<SupplierPageResponse> getSupplierPage(SupplierPageRequest request);

    /**
     * 获取供应商详情
     *
     * @param supplierId 供应商ID
     * @return 供应商详情
     */
    SupplierDetailResponse getSupplierDetail(Integer supplierId);

    /**
     * 创建供应商（支持单个和批量创建）
     *
     * @param request 创建请求
     * @return 批量操作响应
     */
    SupplierBatchResponse createSupplier(SupplierCreateRequest request);

    /**
     * 更新供应商（支持单个和批量更新）
     *
     * @param request 更新请求
     * @return 批量操作响应
     */
    SupplierBatchResponse updateSupplier(SupplierUpdateRequest request);

    /**
     * 删除供应商（支持单个和批量删除）
     *
     * @param request 删除请求
     * @return 批量操作响应
     */
    SupplierBatchResponse deleteSupplier(SupplierDeleteRequest request);

    /**
     * 导入供应商数据
     *
     * @param file Excel文件
     * @return 导入结果
     */
    SupplierImportResponse importSuppliers(MultipartFile file);

    /**
     * 导出供应商数据
     *
     * @param request 导出条件
     * @param response HTTP响应
     */
    void exportSuppliers(SupplierPageRequest request, HttpServletResponse response);

    /**
     * 根据供应商编码查询供应商
     *
     * @param supplierCode 供应商编码
     * @return 供应商信息
     */
    SupplierDetailResponse getSupplierByCode(String supplierCode);

    /**
     * 根据企业名称查询供应商列表
     *
     * @param enterpriseName 企业名称
     * @return 供应商列表
     */
    List<SupplierPageResponse> getSuppliersByEnterpriseName(String enterpriseName);

    /**
     * 批量更新供应商状态
     *
     * @param supplierIds 供应商ID列表
     * @param status 新状态
     * @return 批量操作响应
     */
    SupplierBatchResponse batchUpdateStatus(List<Integer> supplierIds, String status);

    /**
     * 验证供应商编码是否唯一
     *
     * @param creditCode 统一社会信用代码
     * @param excludeSupplierId 排除的供应商ID（用于更新时验证）
     * @return 是否唯一
     */
    boolean isCreditCodeUnique(String creditCode, Integer excludeSupplierId);

    /**
     * 获取供应商统计信息
     *
     * @return 统计数据
     */
    SupplierStatisticsResponse getSupplierStatistics();

    /**
     * 供应商下拉列表查询
     * 专门为下拉选择场景设计的轻量接口，仅返回下拉所需字段
     *
     * @param keyword 搜索关键词（企业名称、信用代码）
     * @param status 供应商状态（默认查询正常状态）
     * @return 供应商下拉选项列表
     */
    List<SupplierSelectResponse> getSupplierSelectList(String keyword, String status);
}
