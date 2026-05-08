package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.system.HazardousWasteItem;
import com.erp.mapper.system.domain.HazardousWasteReferenceStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 危险废物名录Mapper
 */
@Mapper
public interface HazardousWasteItemMapper extends BaseMapper<HazardousWasteItem> {

    /**
     * 分页查询
     *
     * @param page                分页对象
     * @param keyword             关键词
     * @param hazardCharacteristic 危险特性
     * @param wasteCategory       废物类别
     * @param industrySource      行业来源
     * @param wasteCode           废物代码
     * @param wasteName           危险废物
     * @param orderBy             排序字段
     * @param orderDirection      排序方向（asc/desc）
     * @return 分页结果
     */
    IPage<HazardousWasteItem> selectWasteItemPage(Page<HazardousWasteItem> page,
                                                  @Param("keyword") String keyword,
                                                  @Param("hazardCharacteristic") String hazardCharacteristic,
                                                  @Param("wasteCategory") String wasteCategory,
                                                  @Param("wasteCategoryName") String wasteCategoryName,
                                                  @Param("industrySource") String industrySource,
                                                  @Param("wasteCode") String wasteCode,
                                                  @Param("wasteName") String wasteName,
                                                  @Param("available") Boolean available,
                                                  @Param("creatorFilter") Integer creatorFilter,
                                                  @Param("orderBy") String orderBy,
                                                  @Param("orderDirection") String orderDirection);

    /**
     * 列表查询
     *
     * @param keyword                 关键词
     * @param hazardCharacteristic    危险特性
     * @param wasteCategory           废物类别
     * @param industrySource          行业来源
     * @param wasteCode               废物代码
     * @param wasteName               危险废物
     * @param orderBy                 排序字段
     * @param orderDirection           排序方向（asc/desc）
     * @return 列表
     */
    List<HazardousWasteItem> selectWasteItemList(@Param("keyword") String keyword,
                                                 @Param("hazardCharacteristic") String hazardCharacteristic,
                                                 @Param("wasteCategory") String wasteCategory,
                                                 @Param("wasteCategoryName") String wasteCategoryName,
                                                 @Param("industrySource") String industrySource,
                                                 @Param("wasteCode") String wasteCode,
                                                 @Param("wasteName") String wasteName,
                                                 @Param("available") Boolean available,
                                                 @Param("creatorFilter") Integer creatorFilter,
                                                 @Param("orderBy") String orderBy,
                                                 @Param("orderDirection") String orderDirection);

    /**
     * 查询详情
     *
     * @param itemId 条目编号
     * @return 详情
     */
    HazardousWasteItem selectDetailById(@Param("itemId") Integer itemId);

    /**
     * 根据废物代码查询
     *
     * @param wasteCode 废物代码
     * @return 条目
     */
    HazardousWasteItem selectByWasteCode(@Param("wasteCode") String wasteCode);

    /**
     * 查询已存在的废物代码
     *
     * @param codes 代码集合
     * @return 已存在的代码
     */
    List<String> selectExistingWasteCodes(@Param("codes") List<String> codes);

    /**
     * 批量插入
     *
     * @param items 条目集合
     */
    void insertBatch(@Param("items") List<HazardousWasteItem> items);

    /**
     * 引用统计
     *
     * @param itemIds 条目编号集合
     * @return 统计结果
     */
    List<HazardousWasteReferenceStat> selectReferenceStats(@Param("itemIds") List<Integer> itemIds);

    /**
     * 新增危废条目（同时写入废物类别编号）
     *
     * @param item 条目实体
     */
    void insertWithCategory(HazardousWasteItem item);

    /**
     * 更新危废条目（同时更新废物类别编号）
     *
     * @param item 条目实体
     * @return 受影响的行数
     */
    int updateByIdWithCategory(HazardousWasteItem item);

    /**
     * 统计指定废物类别下的危废条目数量
     *
     * @param categoryId 废物类别编号
     * @return 数量
     */
    Long countByCategoryId(@Param("categoryId") Integer categoryId);

    /**
     * 获取废物类别列表
     *
     * @return 废物类别列表（去重、排序）
     */
    List<String> selectWasteCategoryList();

    /**
     * 批量查询详情
     *
     * @param ids 条目编号集合
     * @return 详情列表
     */
    List<HazardousWasteItem> selectDetailByIds(@Param("ids") List<Integer> ids);

    /**
     * 批量删除
     *
     * @param ids 条目编号集合
     * @return 删除数量
     */
    int deleteBatch(@Param("ids") List<Integer> ids);
}


