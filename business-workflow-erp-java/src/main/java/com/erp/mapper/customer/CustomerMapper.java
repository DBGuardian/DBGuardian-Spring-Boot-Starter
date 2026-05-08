package com.erp.mapper.customer;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.customer.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户管理Mapper接口
 *
 * @author ERP
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 根据统一社会信用代码查询客户
     *
     * @param creditCode 信用代码
     * @return 客户信息
     */
    Customer selectByCreditCode(@Param("creditCode") String creditCode);

    /**
     * 按统一社会信用代码精确查询客户列表（用于检测唯一性）
     *
     * @param creditCode 信用代码
     * @return 客户列表
     */
    List<Customer> selectListByCreditCode(@Param("creditCode") String creditCode);

    /**
     * 客户分页查询
     *
     * @param page             分页对象
     * @param enterpriseName   企业名称（模糊）
     * @param creditCode       社会信用代码（模糊）
     * @param ownerEmployeeId  业务员编码
     * @param ownerEmployee    业务员（名称或编码，模糊搜索）
     * @param contactPhone     联系电话（模糊）
     * @param phone            电话（模糊）
     * @param contactPerson    联系人（模糊）
     * @param address          地址（模糊）
     * @param legalRepresentative 法定代表人（模糊）
     * @param formerNames      曾用名（模糊）
     * @param customerCode     客户编码（模糊）
     * @param customerStatus   客户状态（精确匹配）
     * @param orderBy          排序字段
     * @param orderDirection   排序方向（asc/desc）
     * @return 分页结果
     */
    IPage<Customer> selectCustomerPage(
            Page<Customer> page,
            @Param("enterpriseName") String enterpriseName,
            @Param("creditCode") String creditCode,
            @Param("ownerEmployeeId") Integer ownerEmployeeId,
            @Param("ownerEmployee") String ownerEmployee,
            @Param("contactPhone") String contactPhone,
            @Param("phone") String phone,
            @Param("contactPerson") String contactPerson,
            @Param("address") String address,
            @Param("legalRepresentative") String legalRepresentative,
            @Param("formerNames") String formerNames,
            @Param("customerCode") String customerCode,
            @Param("customerStatus") String customerStatus,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection,
            @Param("creatorFilter") Integer creatorFilter
    );

    /**
     * 查询导出列表
     *
     * @param enterpriseName  企业名称
     * @param creditCode      统一社会信用代码
     * @param ownerEmployeeId 业务员编码
     * @param ownerEmployee   业务员（名称或编码，模糊搜索）
     * @param contactPhone    联系电话
     * @param phone           电话
     * @param contactPerson   联系人
     * @param address         地址
     * @param legalRepresentative 法定代表人
     * @param formerNames     曾用名
     * @param customerCode    客户编码（模糊）
     * @param customerStatus  客户状态（精确匹配）
     * @param creatorId       创建人编码（用于权限过滤）
     * @param orderBy         排序字段
     * @param orderDirection  排序方向（asc/desc）
     * @return 客户列表
     */
    List<Customer> selectCustomerList(
            @Param("enterpriseName") String enterpriseName,
            @Param("creditCode") String creditCode,
            @Param("ownerEmployeeId") Integer ownerEmployeeId,
            @Param("ownerEmployee") String ownerEmployee,
            @Param("contactPhone") String contactPhone,
            @Param("phone") String phone,
            @Param("contactPerson") String contactPerson,
            @Param("address") String address,
            @Param("legalRepresentative") String legalRepresentative,
            @Param("formerNames") String formerNames,
            @Param("customerCode") String customerCode,
            @Param("customerStatus") String customerStatus,
            @Param("creatorId") Integer creatorId,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );

    /**
     * 查询详情
     *
     * @param customerId 客户ID
     * @return 客户
     */
    Customer selectDetailById(@Param("customerId") Integer customerId);

    /**
     * 批量插入客户
     *
     * @param customers 客户列表
     */
    void insertBatch(@Param("customers") List<Customer> customers);

    /**
     * 查询已存在的统一社会信用代码
     *
     * @param creditCodes 信用代码集合
     * @return 已存在的信用代码
     */
    List<String> selectExistingCreditCodes(@Param("creditCodes") List<String> creditCodes);

    /**
     * 查询临时客户（从合同和报价单的customer_snapshot中提取）
     * 支持按客户名称模糊查询
     *
     * @param enterpriseName 企业名称（模糊）
     * @return 临时客户列表（customerId为null）
     */
    List<Customer> selectTemporaryCustomers(@Param("enterpriseName") String enterpriseName);
}









