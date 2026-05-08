package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.system.HazardousWasteCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 危险废物类别 Mapper
 */
@Mapper
public interface HazardousWasteCategoryMapper extends BaseMapper<HazardousWasteCategory> {

    /**
     * 根据废物类别名称查询
     *
     * @param wasteCategory 废物类别
     * @return 类别记录
     */
    HazardousWasteCategory selectByWasteCategory(@Param("wasteCategory") String wasteCategory);

    /**
     * 根据废物类别名称集合批量查询
     *
     * @param categories 废物类别集合
     * @return 类别列表
     */
    List<HazardousWasteCategory> selectByWasteCategories(@Param("categories") List<String> categories);

    /**
     * 批量插入类别
     *
     * @param list 类别集合
     */
    void insertBatch(@Param("list") List<HazardousWasteCategory> list);

    /**
     * 批量删除
     *
     * @param ids 类别编号集合
     * @return 删除数量
     */
    int deleteBatch(@Param("ids") List<Integer> ids);
}










