package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.controller.production.dto.TransferManifestExportRow;
import com.erp.controller.production.dto.TransferManifestPageResponse;
import com.erp.entity.production.TransferManifest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 转移联单主表 Mapper
 */
@Mapper
public interface TransferManifestMapper extends BaseMapper<TransferManifest> {

    /**
     * 分页查询转移联单（含动态过滤/排序）
     */
    IPage<TransferManifestPageResponse> selectManifestPage(
            Page<TransferManifestPageResponse> page,
            @Param("广东省联单号") String gdManifestNo,
            @Param("国家联单号") String nationalManifestNo,
            @Param("产生单位") String producer,
            @Param("接收单位") String receivingUnit,
            @Param("车牌号") String licensePlate,
            @Param("当前阶段") String currentStage,
            @Param("计划转移开始") String plannedStart,
            @Param("计划转移结束") String plannedEnd,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 根据广东省联单号查询（用于导入时去重）
     */
    TransferManifest selectByGdManifestNo(@Param("gdManifestNo") String gdManifestNo);

    /**
     * 根据联单号数字串查询联单（用于 PDF 导入时按数字匹配）
     * 传入从 PDF 页面提取的数字串，与广东省联单号、国家联单号做 LIKE 匹配
     */
    List<TransferManifest> selectByManifestNoDigits(@Param("digits") String digits);

    /**
     * 批量更新联单 PDF 文件编号
     */
    void batchUpdatePdfFileId(@Param("list") java.util.List<TransferManifest> list);

    /**
     * 批量插入转移联单主表（一次 SQL，useGeneratedKeys 回填主键）
     */
    void batchInsert(@Param("list") List<TransferManifest> list);

    /**
     * 批量查询广东省联单号是否已存在（用于导入前去重预检）
     */
    List<String> selectExistingGdManifestNos(@Param("gdNos") List<String> gdNos);

    /**
     * 根据多个数字串批量匹配联单（PDF 导入一次查库，避免逐条 LIKE）
     * 返回广东省联单号或国家联单号包含任一数字串的联单
     */
    List<TransferManifest> selectByManifestNoDigitsBatch(@Param("digitsList") List<String> digitsList);

    /**
     * 导出 Excel 查询
     * 勾选了 manifestIds 时仅导出对应主表；未勾选时导出全部。
     * 同一联单有多个子项时，每个子项单独一行。
     */
    List<TransferManifestExportRow> selectExportRows(@Param("manifestIds") List<Integer> manifestIds);
}
