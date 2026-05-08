package com.erp.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.controller.system.dto.LogDetailResponse;
import com.erp.controller.system.dto.LogExportRequest;
import com.erp.controller.system.dto.LogPageRequest;
import com.erp.controller.system.dto.LogPageResponse;

import java.util.List;

/**
 * 日志管理服务接口
 * 
 * @author ERP System
 * @date 2025-12-08
 */
public interface LogService {

    /**
     * 分页查询全部日志
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<LogPageResponse> getLogPage(LogPageRequest request);

    /**
     * 分页查询操作日志
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<LogPageResponse> getOperationLogPage(LogPageRequest request);

    /**
     * 分页查询数据变更日志
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<LogPageResponse> getDataChangeLogPage(LogPageRequest request);

    /**
     * 分页查询登录日志
     * 
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<LogPageResponse> getLoginLogPage(LogPageRequest request);

    /**
     * 获取日志详情
     * 
     * @param logId 日志编号
     * @return 日志详情
     */
    LogDetailResponse getLogDetail(Integer logId);

    /**
     * 导出日志列表
     * 
     * @param request 导出请求
     * @return 日志列表
     */
    List<LogPageResponse> exportLogs(LogExportRequest request);
}
