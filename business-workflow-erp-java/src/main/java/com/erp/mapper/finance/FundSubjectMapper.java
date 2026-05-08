package com.erp.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.finance.dto.FundSubjectListItemResponse;
import com.erp.controller.finance.dto.FundSubjectOption;
import com.erp.controller.finance.dto.FundSubjectPageRequest;
import com.erp.entity.finance.FundSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会计科目 Mapper
 *
 * 对应表：FUND_SUBJECT
 */
@Mapper
public interface FundSubjectMapper extends BaseMapper<FundSubject> {

    /**
     * 分页查询会计科目列表（关联创建人/更新人）
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<FundSubjectListItemResponse> selectFundSubjectPage(Page<?> page,
                                                             @Param("query") FundSubjectPageRequest request);

    /**
     * 查询启用的会计科目选项列表（用于下拉选择）
     *
     * @return 科目选项列表
     */
    List<FundSubjectOption> selectFundSubjectOptions();

    /**
     * 根据科目编码查询科目信息
     *
     * @param subjectCode 科目编码
     * @return 科目信息
     */
    FundSubject selectBySubjectCode(@Param("subjectCode") String subjectCode);

    /**
     * 批量更新科目状态
     *
     * @param subjectIds  科目ID列表
     * @param enabled     是否启用
     * @param updateUserId 更新人编码
     * @return 更新行数
     */
    int batchUpdateEnabled(@Param("subjectIds") List<Long> subjectIds,
                          @Param("enabled") Boolean enabled,
                          @Param("updateUserId") Integer updateUserId);

    /**
     * 查询子科目列表
     *
     * @param parentSubjectCode 上级科目编码
     * @return 子科目列表
     */
    List<FundSubject> selectChildSubjects(@Param("parentSubjectCode") String parentSubjectCode);

    /**
     * 递归查询所有子科目编码
     *
     * @param parentSubjectCode 上级科目编码
     * @return 所有子科目编码列表
     */
    List<String> selectAllChildSubjectCodes(@Param("parentSubjectCode") String parentSubjectCode);

    /**
     * 查询科目树形结构数据
     *
     * @return 树形结构科目数据
     */
    List<FundSubjectListItemResponse> selectSubjectTree();
}