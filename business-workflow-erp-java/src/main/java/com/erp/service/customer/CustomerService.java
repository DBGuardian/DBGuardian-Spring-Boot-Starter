package com.erp.service.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.customer.dto.CustomerCreateRequest;
import com.erp.controller.customer.dto.CustomerDetailResponse;
import com.erp.controller.customer.dto.CustomerImportResponse;
import com.erp.controller.customer.dto.CustomerPageRequest;
import com.erp.controller.customer.dto.CustomerPageResponse;
import com.erp.controller.customer.dto.CustomerUpdateRequest;
import com.erp.controller.customer.dto.CustomerQuotationResponse;
import com.erp.controller.customer.dto.CustomerQuotationHierarchicalResponse;
import com.erp.controller.customer.dto.CustomerContractResponse;
import com.erp.controller.customer.dto.CustomerFollowResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 客户管理服务接口
 */
public interface CustomerService {

    /**
     * 新增客户
     *
     * @param request 请求参数
     * @return 客户详情
     */
    CustomerDetailResponse createCustomer(CustomerCreateRequest request);

    /**
     * 更新客户信息
     *
     * @param request 请求参数
     */
    void updateCustomer(CustomerUpdateRequest request);

    /**
     * 客户详情
     *
     * @param customerId 客户ID
     * @return 详情
     */
    CustomerDetailResponse getCustomerDetail(Integer customerId);

    /**
     * 客户分页
     *
     * @param request 条件
     * @return 分页结果
     */
    IPage<CustomerPageResponse> getCustomerPage(CustomerPageRequest request);

    /**
     * 导出数据
     *
     * @param request 条件
     * @return 列表
     */
    List<CustomerDetailResponse> listCustomersForExport(CustomerPageRequest request);

    /**
     * 批量导入客户
     *
     * @param file Excel文件
     * @return 导入结果
     */
    CustomerImportResponse importCustomers(MultipartFile file);

    /**
     * 获取客户报价记录
     *
     * @param customerId 客户ID
     * @return 报价记录列表（层级结构）
     */
    List<CustomerQuotationHierarchicalResponse> getCustomerQuotations(Integer customerId);

    /**
     * 获取客户合同记录
     *
     * @param customerId 客户ID
     * @return 合同记录列表
     */
    List<CustomerContractResponse> getCustomerContracts(Integer customerId);

    /**
     * 获取客户跟进记录
     *
     * @param customerId 客户ID
     * @return 跟进记录列表
     */
    List<CustomerFollowResponse> getCustomerFollows(Integer customerId);
}










