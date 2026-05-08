package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.system.Log;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志管理Mapper接口
 * 
 * 对应表：LOG
 * 
 * @author ERP System
 * @date 2025-12-08
 */
@Mapper
public interface LogMapper extends BaseMapper<Log> {

    /**
     * 日志分页查询（全部日志）
     * 
     * @param page 分页对象
     * @param keyword 关键字（操作人/模块/内容）
     * @param logType 日志类型：操作日志/数据变更/登录
     * @param status 状态：success/failed
     * @param module 操作模块
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param sortField 排序字段
     * @param sortOrder 排序方向：asc/desc
     * @return 分页结果
     */
    IPage<Log> selectLogPage(
            Page<Log> page,
            @Param("keyword") String keyword,
            @Param("logType") String logType,
            @Param("status") String status,
            @Param("module") String module,
            @Param("ipAddress") String ipAddress,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 操作日志分页查询
     * 
     * @param page 分页对象
     * @param keyword 关键字（操作人/模块/内容）
     * @param module 操作模块
     * @param operationTypes 操作类型列表（新增/编辑/删除/审核/导出）
     * @param userIds 用户编码列表
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param sortField 排序字段
     * @param sortOrder 排序方向：asc/desc
     * @return 分页结果
     */
    IPage<Log> selectOperationLogPage(
            Page<Log> page,
            @Param("keyword") String keyword,
            @Param("module") String module,
            @Param("operationTypes") List<String> operationTypes,
            @Param("userIds") List<Integer> userIds,
            @Param("ipAddress") String ipAddress,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 数据变更日志分页查询
     * 
     * @param page 分页对象
     * @param keyword 关键字（操作人/模块/字段）
     * @param module 操作模块
     * @param changeTypes 变更类型列表（新增/更新/删除）
     * @param userIds 用户编码列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param sortField 排序字段
     * @param sortOrder 排序方向：asc/desc
     * @return 分页结果
     */
    IPage<Log> selectDataChangeLogPage(
            Page<Log> page,
            @Param("keyword") String keyword,
            @Param("module") String module,
            @Param("changeTypes") List<String> changeTypes,
            @Param("userIds") List<Integer> userIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 登录日志分页查询
     * 
     * @param page 分页对象
     * @param keyword 关键字（用户名）
     * @param loginResult 登录结果：成功/失败
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param sortField 排序字段
     * @param sortOrder 排序方向：asc/desc
     * @return 分页结果
     */
    IPage<Log> selectLoginLogPage(
            Page<Log> page,
            @Param("keyword") String keyword,
            @Param("loginResult") String loginResult,
            @Param("ipAddress") String ipAddress,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 根据日志编号查询日志详情
     * 
     * @param logId 日志编号
     * @return 日志详情
     */
    Log selectLogDetail(@Param("logId") Integer logId);

    /**
     * 根据条件查询日志列表（用于导出）
     * 
     * @param keyword 关键字
     * @param logType 日志类型
     * @param status 状态
     * @param module 操作模块
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param logIds 日志编号列表（用于导出选中）
     * @return 日志列表
     */
    List<Log> selectLogsForExport(
            @Param("keyword") String keyword,
            @Param("logType") String logType,
            @Param("status") String status,
            @Param("module") String module,
            @Param("ipAddress") String ipAddress,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("logIds") List<Integer> logIds
    );
}











































































































































