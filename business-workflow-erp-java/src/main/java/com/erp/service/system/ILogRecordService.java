package com.erp.service.system;

import javax.servlet.http.HttpServletRequest;

/**
 * 日志记录服务接口
 *
 * @author ERP System
 * @date 2025-12-08
 */
public interface ILogRecordService {

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    String getClientIp(HttpServletRequest request);

    /**
     * 记录操作日志（异步）
     *
     * @param module 操作模块
     * @param operationType 操作类型
     * @param content 操作内容
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param success 是否成功
     * @param errorMsg 错误信息
     */
    void recordOperationLog(String module, String operationType, String content,
                            Integer userId, String ipAddress, boolean success, String errorMsg);

    /**
     * 记录操作日志（同步版本，用于需要等待日志写入完成的场景）
     *
     * @param module 操作模块
     * @param operationType 操作类型
     * @param content 操作内容
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param success 是否成功
     * @param errorMsg 错误信息
     */
    void recordOperationLogSync(String module, String operationType, String content,
                                Integer userId, String ipAddress, boolean success, String errorMsg);

    /**
     * 记录数据变更日志（异步）
     *
     * @param module 操作模块
     * @param tableName 表名
     * @param recordId 记录ID
     * @param operationType 操作类型
     * @param content 操作内容
     * @param oldData 变更前数据
     * @param newData 变更后数据
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param success 是否成功
     * @param errorMsg 错误信息
     */
    void recordDataChangeLog(String module, String tableName, String recordId,
                             String operationType, String content,
                             Object oldData, Object newData,
                             Integer userId, String ipAddress, boolean success, String errorMsg);

    /**
     * 记录数据变更日志（同步版本）
     *
     * @param module 操作模块
     * @param tableName 表名
     * @param recordId 记录ID
     * @param operationType 操作类型
     * @param content 操作内容
     * @param oldData 变更前数据
     * @param newData 变更后数据
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param success 是否成功
     * @param errorMsg 错误信息
     */
    void recordDataChangeLogSync(String module, String tableName, String recordId,
                                 String operationType, String content,
                                 Object oldData, Object newData,
                                 Integer userId, String ipAddress, boolean success, String errorMsg);

    /**
     * 记录登录日志（异步）
     *
     * @param username 用户名
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param success 是否成功
     * @param errorMsg 错误信息
     * @param durationMs 登录耗时
     */
    void recordLoginLog(String username, Integer userId, String ipAddress,
                        boolean success, String errorMsg, Long durationMs);
}
