package com.erp.service.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.controller.finance.dto.FundSubjectListItemResponse;
import com.erp.controller.finance.dto.FundSubjectOption;
import com.erp.controller.finance.dto.FundSubjectPageRequest;
import com.erp.entity.finance.FundSubject;

import java.util.List;

/**
 * 会计科目服务接口
 *
 * 提供科目创建、更新、删除、查询等功能。
 */
public interface FundSubjectService extends IService<FundSubject> {

    /**
     * 创建会计科目
     *
     * @param subjectCode               科目编码
     * @param subjectName               科目名称
     * @param subjectCategory           科目类别：流动资产、非流动资产
     * @param balanceDirection          余额方向：收入、支出
     * @param isCashSubject             是否为现金科目
     * @param auxiliaryAccounting       辅助核算
     * @param quantityAccounting        数量核算
     * @param foreignCurrencyAccounting 外币核算
     * @param remark                    备注
     * @return 创建后的会计科目实体
     */
    FundSubject createSubject(String subjectCode, String subjectName, String subjectCategory,
                             String balanceDirection, Boolean isCashSubject, Boolean auxiliaryAccounting,
                             Boolean quantityAccounting, Boolean foreignCurrencyAccounting, String remark);

    /**
     * 创建会计科目（带上级科目）
     *
     * @param parentSubjectCode         上级科目编码
     * @param subjectCode               科目编码
     * @param subjectName               科目名称
     * @param subjectCategory           科目类别：流动资产、非流动资产
     * @param balanceDirection          余额方向：收入、支出
     * @param isCashSubject             是否为现金科目
     * @param auxiliaryAccounting       辅助核算
     * @param quantityAccounting        数量核算
     * @param foreignCurrencyAccounting 外币核算
     * @param remark                    备注
     * @return 创建后的会计科目实体
     */
    FundSubject createSubjectWithParent(String parentSubjectCode, String subjectCode, String subjectName,
                                       String subjectCategory, String balanceDirection, Boolean isCashSubject,
                                       Boolean auxiliaryAccounting, Boolean quantityAccounting,
                                       Boolean foreignCurrencyAccounting, String remark);

    /**
     * 分页查询会计科目列表
     *
     * @param page    分页参数
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<FundSubjectListItemResponse> getSubjectPage(Page<?> page, FundSubjectPageRequest request);

    /**
     * 更新会计科目
     *
     * @param subjectId                 科目ID
     * @param subjectCode               科目编码
     * @param subjectName               科目名称
     * @param subjectCategory           科目类别：流动资产、非流动资产
     * @param balanceDirection          余额方向：收入、支出
     * @param isCashSubject             是否为现金科目
     * @param auxiliaryAccounting       辅助核算
     * @param quantityAccounting        数量核算
     * @param foreignCurrencyAccounting 外币核算
     * @param enabled                   是否启用
     * @param remark                    备注
     * @param version                   版本号（乐观锁）
     * @return 更新后的会计科目实体
     */
    FundSubject updateSubject(Long subjectId, String subjectCode, String subjectName, String subjectCategory,
                             String balanceDirection, Boolean isCashSubject, Boolean auxiliaryAccounting,
                             Boolean quantityAccounting, Boolean foreignCurrencyAccounting, Boolean enabled,
                             String remark, Integer version);

    /**
     * 删除会计科目
     *
     * @param subjectId 科目ID
     */
    void deleteSubject(Long subjectId);

    /**
     * 批量删除会计科目
     *
     * @param subjectIds 科目ID列表
     * @return 成功删除的数量
     */
    int batchDeleteSubjects(List<Long> subjectIds);

    /**
     * 批量更新科目状态
     *
     * @param subjectIds 科目ID列表
     * @param enabled    是否启用
     */
    void batchUpdateSubjectStatus(List<Long> subjectIds, Boolean enabled);

    /**
     * 获取科目选项列表（用于下拉选择）
     *
     * @return 科目选项列表
     */
    List<FundSubjectOption> getSubjectOptions();

    /**
     * 搜索科目选项（支持科目编码和科目名称模糊搜索）
     *
     * @param keyword 搜索关键词（可为null，表示获取所有）
     * @param limit   返回结果数量限制
     * @return 科目选项列表
     */
    List<FundSubjectOption> searchSubjectOptions(String keyword, Integer limit);

    /**
     * 根据科目编码查询科目
     *
     * @param subjectCode 科目编码
     * @return 科目信息
     */
    FundSubject getSubjectByCode(String subjectCode);

    /**
     * 查询子科目列表
     *
     * @param parentSubjectCode 上级科目编码
     * @return 子科目列表
     */
    List<FundSubject> getChildSubjects(String parentSubjectCode);

    /**
     * 获取科目树形结构数据
     *
     * @return 树形结构科目数据
     */
    List<FundSubjectListItemResponse> getSubjectTree();

    /**
     * 生成科目编码
     *
     * @param parentSubjectCode 上级科目编码，如果为null则生成一级科目编码
     * @return 生成的科目编码
     */
    String generateSubjectCode(String parentSubjectCode);

    /**
     * 生成全科目名称
     *
     * @param parentSubjectCode 上级科目编码
     * @param subjectName 当前科目名称
     * @return 全科目名称
     */
    String generateFullSubjectName(String parentSubjectCode, String subjectName);

    /**
     * 计算科目级次
     *
     * @param parentSubjectCode 上级科目编码
     * @return 科目级次
     */
    Integer calculateSubjectLevel(String parentSubjectCode);

    /**
     * 下载科目导入模板
     *
     * @param response HTTP响应对象
     */
    void downloadSubjectTemplate(javax.servlet.http.HttpServletResponse response);

    /**
     * 从Excel文件导入科目数据
     *
     * @param file Excel文件
     * @return 导入结果信息
     */
    com.erp.common.result.ImportResult importSubjectsFromExcel(org.springframework.web.multipart.MultipartFile file);

    /**
     * 导出科目数据为Excel文件
     *
     * @param request 查询条件
     * @param response HTTP响应对象
     */
    void exportSubjectsToExcel(com.erp.controller.finance.dto.FundSubjectPageRequest request, javax.servlet.http.HttpServletResponse response);
}