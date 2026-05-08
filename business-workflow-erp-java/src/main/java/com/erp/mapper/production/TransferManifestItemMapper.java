package com.erp.mapper.production;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.production.TransferManifestItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 转移联单子表 Mapper
 */
@Mapper
public interface TransferManifestItemMapper extends BaseMapper<TransferManifestItem> {

    /**
     * 根据联单编号查询子项
     *
     * @param manifestId 联单编号
     * @return 子项列表
     */
    List<TransferManifestItem> selectByManifestId(@Param("manifestId") Integer manifestId);

    /**
     * 根据多个联单编号批量查询子项（避免 N+1）
     *
     * @param manifestIds 联单编号列表
     * @return 子项列表
     */
    List<TransferManifestItem> selectByManifestIds(@Param("manifestIds") List<Integer> manifestIds);

    /**
     * 根据联单编号逻辑删除所有子项
     *
     * @param manifestId 联单编号
     * @return 影响行数
     */
    int deleteByManifestId(@Param("manifestId") Integer manifestId);

    /**
     * 批量插入转移联单子表（一次 SQL）
     */
    void batchInsert(@Param("list") List<TransferManifestItem> list);
}
