package com.erp.mapper.oa;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.oa.OaApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * OA审核记录Mapper接口
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Mapper
public interface OaApprovalRecordMapper extends BaseMapper<OaApprovalRecord> {

    /**
     * 分页查询OA审核记录列表
     *
     * @param page 分页对象
     * @param keyword 关键字（搜索审核编号、关联单号、标题、提交人姓名、审核人姓名）
     * @param businessType 业务类型（精确匹配）
     * @param approvalStatus 审核状态（精确匹配）
     * @param viewScope 视图范围：pending-待我审核、submitted-我发起的、processed-已处理、all-全部
     * @param submitterId 提交人ID（精确匹配）
     * @param approverId 审核人ID（精确匹配）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param unapprovedDays 未审核天数（超过此天数未审核）
     * @return 分页结果
     */
    IPage<OaApprovalRecord> selectPageList(
            Page<OaApprovalRecord> page,
            @Param("keyword") String keyword,
            @Param("businessType") String businessType,
            @Param("approvalStatus") String approvalStatus,
            @Param("viewScope") String viewScope,
            @Param("submitterId") Integer submitterId,
            @Param("approverId") Integer approverId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("unapprovedDays") Integer unapprovedDays
    );

    /**
     * 统计OA审核记录数量
     *
     * @param viewScope 视图范围
     * @param submitterId 提交人ID
     * @param approverId 审核人ID
     * @param approvalStatus 审核状态
     * @param submitTime 提交时间（用于计算超时）
     * @return 统计数量
     */
    Long selectCount(
            @Param("viewScope") String viewScope,
            @Param("submitterId") Integer submitterId,
            @Param("approverId") Integer approverId,
            @Param("approvalStatus") String approvalStatus,
            @Param("submitTime") LocalDateTime submitTime
    );
}
