package com.erp.mapper.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.contract.BusinessContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务合同 Mapper
 *
 * <p>基础 CRUD 由 BaseMapper 提供；此处只声明自定义分页查询方法。</p>
 * 设计原则：业务员信息冗余存储于 BUSINESS_CONTRACT 表，分页查询直接读本表，无需 JOIN SALESPERSON。
 */
@Mapper
public interface BusinessContractMapper extends BaseMapper<BusinessContract> {

    /**
     * 分页查询合同列表（直接查 BUSINESS_CONTRACT 冗余字段，支持按业务员姓名/甲方名称筛选和排序）
     *
     * @param contractNo      合同单号（模糊）
     * @param salespersonName 业务员姓名（模糊，匹配 BUSINESS_CONTRACT.业务员姓名）
     * @param companyName     甲方公司名称（模糊，匹配 BUSINESS_CONTRACT.甲方名称）
     * @param status          合同状态（精确）
     * @param signTimeStart   创建时间起
     * @param signTimeEnd     创建时间止
     * @param sortField       排序字段（contractId/contractNo/salespersonName/companyName/status）
     * @param sortOrder       排序方向（asc/desc）
     * @param creatorFilter   制单人ID过滤（viewScope=SELF时传入当前用户ID，viewScope=ALL时传null）
     * @param offset          偏移量
     * @param size            每页条数
     * @return 合同列表
     */
    List<BusinessContract> selectPageList(
            @Param("contractNo")      String contractNo,
            @Param("salespersonName") String salespersonName,
            @Param("companyName")     String companyName,
            @Param("status")          String status,
            @Param("signTimeStart")   String signTimeStart,
            @Param("signTimeEnd")     String signTimeEnd,
            @Param("sortField")       String sortField,
            @Param("sortOrder")       String sortOrder,
            @Param("creatorFilter")   Integer creatorFilter,
            @Param("offset")          long   offset,
            @Param("size")            long   size
    );

    /**
     * 查询总数（条件与 selectPageList 保持一致）
     */
    long selectPageCount(
            @Param("contractNo")      String contractNo,
            @Param("salespersonName") String salespersonName,
            @Param("companyName")     String companyName,
            @Param("status")          String status,
            @Param("signTimeStart")   String signTimeStart,
            @Param("signTimeEnd")     String signTimeEnd,
            @Param("creatorFilter")   Integer creatorFilter
    );

    /**
     * 根据危废合同编号查询其合同号（用于详情接口补充展示信息，避免前端额外请求）
     *
     * @param hazardousContractId 危废合同编号
     * @return 危废合同号（如 HQ-20250101-00001），不存在时返回 null
     */
    String selectHazardousContractNo(@Param("hazardousContractId") Integer hazardousContractId);

    /**
     * 业务费结算专用合同列表查询（不分页）
     *
     * <p>返回字段：
     *   BUSINESS_CONTRACT.合同编号（businessContractId）、
     *   BUSINESS_CONTRACT.合同单号（businessContractNo）、
     *   BUSINESS_CONTRACT.业务员姓名（salespersonName）、
     *   BUSINESS_CONTRACT.合同状态（status）、
     *   CONTRACT.合同编号（contractId）、
     *   CONTRACT.合同号（contractNo）、
     *   CONTRACT.甲方名称（partyAName）
     * </p>
     * <p>仅返回状态为"执行中"或"已完结"的业务合同，按创建时间倒序排列。</p>
     *
     * @return 合同列表（含关联合同信息）
     */
    List<java.util.Map<String, Object>> selectForSettlement();

    /**
     * 统计游离业务费结算单数量（未关联业务合同的独立业务费结算单）
     *
     * @return 游离业务费结算单数量
     */
    long countOrphanBusinessFeeSettlements();
}
