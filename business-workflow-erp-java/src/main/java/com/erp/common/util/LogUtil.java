package com.erp.common.util;

import com.erp.entity.system.Log;
import com.erp.mapper.system.LogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日志工具类
 * 提供便捷的日志记录方法
 * 
 * @author ERP System
 * @date 2025-12-08
 */
@Component
public class LogUtil {

    private static LogMapper logMapper;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public void setLogMapper(LogMapper logMapper) {
        LogUtil.logMapper = logMapper;
    }

    /**
     * 获取客户端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            // X-Forwarded-For 可能包含多个IP，取第一个
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 记录操作日志
     * 
     * @param module 操作模块（如：客户管理、合同管理）
     * @param operationType 操作类型（新增、编辑、删除、审核、导出、导入等）
     * @param content 操作内容（简要描述）
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param success 是否成功
     * @param errorMsg 错误信息（失败时）
     */
    public static void recordOperationLog(String module, String operationType, String content, 
                                          Integer userId, String ipAddress, boolean success, String errorMsg) {
        try {
            Log log = new Log();
            log.setUserId(userId);
            log.setOperationModule(module);
            log.setOperationContent(buildOperationContent(operationType, content, success, errorMsg));
            log.setOperationTime(LocalDateTime.now());
            log.setIpAddress(ipAddress);
            
            logMapper.insert(log);
        } catch (Exception e) {
            // 日志记录失败不应该影响主业务流程，只记录错误日志
            org.slf4j.LoggerFactory.getLogger(LogUtil.class).error("记录操作日志失败", e);
        }
    }

    /**
     * 记录数据变更日志
     * 
     * @param module 操作模块
     * @param tableName 数据表名
     * @param recordId 记录ID
     * @param operationType 操作类型（新增、更新、删除）
     * @param content 操作内容
     * @param oldData 变更前数据
     * @param newData 变更后数据
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param success 是否成功
     * @param errorMsg 错误信息（失败时）
     */
    public static void recordDataChangeLog(String module, String tableName, String recordId,
                                          String operationType, String content,
                                          Object oldData, Object newData,
                                          Integer userId, String ipAddress, boolean success, String errorMsg) {
        try {
            Log log = new Log();
            log.setUserId(userId);
            log.setOperationModule(module);
            log.setOperationContent(buildOperationContent(operationType, content, success, errorMsg));
            log.setOperationTime(LocalDateTime.now());
            log.setIpAddress(ipAddress);
            
            // 序列化数据为JSON
            if (oldData != null) {
                log.setOldData(objectMapper.writeValueAsString(oldData));
            }
            if (newData != null) {
                log.setNewData(objectMapper.writeValueAsString(newData));
            }
            
            logMapper.insert(log);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(LogUtil.class).error("记录数据变更日志失败", e);
        }
    }

    /**
     * 记录登录日志
     * 
     * @param username 用户名
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param success 是否成功
     * @param errorMsg 错误信息（失败时）
     * @param durationMs 耗时（毫秒）
     */
    public static void recordLoginLog(String username, Integer userId, String ipAddress, 
                                     boolean success, String errorMsg, Long durationMs) {
        try {
            Log log = new Log();
            log.setUserId(userId);
            log.setOperationModule("login");
            log.setOperationContent(buildLoginContent(username, success, errorMsg));
            log.setOperationTime(LocalDateTime.now());
            log.setIpAddress(ipAddress);
            
            logMapper.insert(log);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(LogUtil.class).error("记录登录日志失败", e);
        }
    }

    /**
     * 构建操作内容
     */
    private static String buildOperationContent(String operationType, String content, boolean success, String errorMsg) {
        StringBuilder sb = new StringBuilder();
        if (operationType != null) {
            sb.append(operationType);
        }
        if (content != null) {
            if (sb.length() > 0) {
                sb.append("：");
            }
            sb.append(content);
        }
        if (success) {
            sb.append("成功");
        } else {
            sb.append("失败");
            if (errorMsg != null) {
                sb.append("，").append(errorMsg);
            }
        }
        return sb.toString();
    }

    /**
     * 构建登录内容
     */
    private static String buildLoginContent(String username, boolean success, String errorMsg) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户").append(username);
        if (success) {
            sb.append("登录成功");
        } else {
            sb.append("登录失败");
            if (errorMsg != null) {
                sb.append("，").append(errorMsg);
            }
        }
        return sb.toString();
    }
}











































































































































