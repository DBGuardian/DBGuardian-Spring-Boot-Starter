package com.erp.mapper.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.entity.common.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface FileMapper extends BaseMapper<File> {

    /**
     * 根据业务类型和业务ID查询文件列表
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 文件列表
     */
    List<File> selectByBusinessTypeAndId(@Param("businessType") String businessType, @Param("businessId") Integer businessId);

    /**
     * 根据业务类型和业务ID删除文件
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 删除数量
     */
    int deleteByBusinessTypeAndId(@Param("businessType") String businessType, @Param("businessId") Integer businessId);

    /**
     * 根据业务类型和业务ID集合查询文件列表
     *
     * @param businessType 业务类型
     * @param businessIds  业务ID集合
     * @return 文件列表
     */
    List<File> selectByBusinessTypeAndIds(@Param("businessType") String businessType,
                                          @Param("businessIds") List<Integer> businessIds);
}







































