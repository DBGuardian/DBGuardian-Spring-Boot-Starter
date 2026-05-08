package com.erp.mapper.transport;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.transport.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 车辆管理Mapper接口
 *
 * @author ERP
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {

    /**
     * 车辆分页查询
     *
     * @param page          分页对象
     * @param plateNo       车牌号（模糊搜索）
     * @param vehicleCode   车辆编号（模糊搜索）
     * @param companyName   公司名称（模糊搜索）
     * @param status        车辆状态（精确匹配）
     * @param creatorFilter 创建人过滤（用于数据范围控制）
     * @param orderBy       排序字段
     * @param orderDirection 排序方向（asc/desc）
     * @return 分页结果
     */
    IPage<Vehicle> selectVehiclePage(
            Page<Vehicle> page,
            @Param("plateNo") String plateNo,
            @Param("vehicleCode") String vehicleCode,
            @Param("companyName") String companyName,
            @Param("status") String status,
            @Param("creatorFilter") Integer creatorFilter,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );

    /**
     * 查询详情
     *
     * @param vehicleId 车辆ID
     * @return 车辆信息
     */
    Vehicle selectDetailById(@Param("vehicleId") Integer vehicleId);

    /**
     * 根据车牌号查询车辆
     *
     * @param plateNo 车牌号
     * @return 车辆信息
     */
    Vehicle selectByPlateNo(@Param("plateNo") String plateNo);

    /**
     * 查询车辆列表（用于导出）
     *
     * @param plateNo       车牌号（模糊搜索）
     * @param vehicleCode   车辆编号（模糊搜索）
     * @param companyName   公司名称（模糊搜索）
     * @param status        车辆状态（精确匹配）
     * @param creatorFilter 创建人过滤（viewScope=SELF 时传入当前员工ID，ALL 时传 null）
     * @param orderBy       排序字段
     * @param orderDirection 排序方向（asc/desc）
     * @return 车辆列表
     */
    List<Vehicle> selectVehicleList(
            @Param("plateNo") String plateNo,
            @Param("vehicleCode") String vehicleCode,
            @Param("companyName") String companyName,
            @Param("status") String status,
            @Param("creatorFilter") Integer creatorFilter,
            @Param("orderBy") String orderBy,
            @Param("orderDirection") String orderDirection
    );

    /**
     * 批量插入车辆
     *
     * @param vehicles 车辆列表
     */
    void insertBatch(@Param("vehicles") List<Vehicle> vehicles);

    /**
     * 查询已存在的车牌号
     *
     * @param plateNos 车牌号集合
     * @return 已存在的车牌号
     */
    List<String> selectExistingPlateNos(@Param("plateNos") List<String> plateNos);

}

