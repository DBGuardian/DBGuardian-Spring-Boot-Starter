package com.erp.mapper.customer;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.customer.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 供应商管理Mapper接口
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    /**
     * 根据统一社会信用代码查询供应商
     *
     * @param creditCode 信用代码
     * @return 供应商信息
     */
    Supplier selectByCreditCode(@Param("creditCode") String creditCode);

    /**
     * 按统一社会信用代码精确查询供应商列表（用于检测唯一性）
     *
     * @param creditCode 信用代码
     * @return 供应商列表
     */
    List<Supplier> selectListByCreditCode(@Param("creditCode") String creditCode);

    /**
     * 供应商分页查询
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
     * @param supplierCode     供应商编码（模糊）
     * @param supplierStatus   供应商状态（精确匹配）
     * @param accountName      账户名称（模糊）
     * @param accountNumber    账户号码（模糊）
     * @param bankName         开户银行（模糊）
     * @param creatorId        创建人编码（用于权限过滤）
     * @param orderBy          排序字段
     * @param orderDirection   排序方向（asc/desc）
     * @return 分页结果
     */
    IPage<com.erp.controller.customer.dto.SupplierPageResponse> selectSupplierPage(
            Page<com.erp.controller.customer.dto.SupplierPageResponse> page,
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
            @Param("supplierCode") String supplierCode,
            @Param("supplierStatus") String supplierStatus,
            @Param("accountName") String accountName,
            @Param("accountNumber") String accountNumber,
            @Param("bankName") String bankName,
            @Param("creatorId") Integer creatorId,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );

    /**
     * 查询导出列表
     *
     * @param enterpriseName   企业名称
     * @param creditCode       统一社会信用代码
     * @param ownerEmployeeId  业务员编码
     * @param ownerEmployee    业务员（名称或编码，模糊搜索）
     * @param contactPhone     联系电话
     * @param phone            电话
     * @param contactPerson    联系人
     * @param address          地址
     * @param legalRepresentative 法定代表人
     * @param formerNames      曾用名
     * @param supplierCode     供应商编码（模糊）
     * @param supplierStatus   供应商状态（精确匹配）
     * @param accountName      账户名称
     * @param accountNumber    账户号码
     * @param bankName         开户银行
     * @param orderBy          排序字段
     * @param orderDirection   排序方向（asc/desc）
     * @return 供应商列表
     */
    List<Supplier> selectSupplierList(
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
            @Param("supplierCode") String supplierCode,
            @Param("supplierStatus") String supplierStatus,
            @Param("accountName") String accountName,
            @Param("accountNumber") String accountNumber,
            @Param("bankName") String bankName,
            @Param("creatorId") Integer creatorId,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );

    /**
     * 查询详情
     *
     * @param supplierId 供应商ID
     * @return 供应商
     */
    Supplier selectDetailById(@Param("supplierId") Integer supplierId);

    /**
     * 批量插入供应商
     *
     * @param suppliers 供应商列表
     */
    void insertBatch(@Param("suppliers") List<Supplier> suppliers);

    /**
     * 查询已存在的统一社会信用代码
     *
     * @param creditCodes 信用代码集合
     * @return 已存在的信用代码
     */
    List<String> selectExistingCreditCodes(@Param("creditCodes") List<String> creditCodes);

    /**
     * 批量更新供应商状态
     *
     * @param supplierIds 供应商ID列表
     * @param supplierStatus 新状态
     * @return 更新的行数
     */
    int updateBatchStatus(@Param("supplierIds") List<Integer> supplierIds,
                         @Param("supplierStatus") String supplierStatus);

    /**
     * 获取供应商统计信息
     *
     * @return 统计数据
     */
    SupplierStatistics selectSupplierStatistics();

    /**
     * 根据企业名称模糊查询供应商
     *
     * @param enterpriseName 企业名称
     * @return 供应商列表
     */
    List<Supplier> selectByEnterpriseName(@Param("enterpriseName") String enterpriseName);
}
