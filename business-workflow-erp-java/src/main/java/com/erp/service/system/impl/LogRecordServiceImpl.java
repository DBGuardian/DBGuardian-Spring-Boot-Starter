package com.erp.service.system.impl;

import com.erp.entity.system.Log;
import com.erp.mapper.system.LogMapper;
import com.erp.service.system.ILogRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 日志记录服务实现
 *
 * @author ERP System
 * @date 2025-12-08
 */
@Slf4j
@Service
public class LogRecordServiceImpl implements ILogRecordService {

    @Autowired
    private LogMapper logMapper;

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    @Async("logTaskExecutor")
    public void recordOperationLog(String module, String operationType, String content,
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
            LogRecordServiceImpl.log.error("记录操作日志失败", e);
        }
    }

    @Override
    public void recordOperationLogSync(String module, String operationType, String content,
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
            LogRecordServiceImpl.log.error("记录操作日志失败", e);
        }
    }

    @Override
    @Async("logTaskExecutor")
    public void recordDataChangeLog(String module, String tableName, String recordId,
                                    String operationType, String content,
                                    Object oldData, Object newData,
                                    Integer userId, String ipAddress, boolean success, String errorMsg) {
        try {
            LogRecordServiceImpl.log.debug("开始记录数据变更日志 - 模块：{}，操作类型：{}", module, operationType);

            Log log = new Log();
            log.setUserId(userId);
            log.setOperationModule(module);
            log.setOperationContent(buildOperationContent(operationType, content, success, errorMsg));
            log.setOperationTime(LocalDateTime.now());
            log.setIpAddress(ipAddress);

            if ("删除".equals(operationType)) {
                if (oldData != null) {
                    log.setOldData(objectMapper.writeValueAsString(oldData));
                }
                log.setNewData("已删除");
            } else if ("更新".equals(operationType) || "编辑".equals(operationType)) {
                Map<String, Object> changedFields = getChangedFields(oldData, newData);

                if (!changedFields.isEmpty()) {
                    Map<String, Object> oldChangedData = new HashMap<>();
                    Map<String, Object> newChangedData = new HashMap<>();

                    Map<String, Object> oldMap = convertToMap(oldData);
                    Map<String, Object> newMap = convertToMap(newData);

                    for (String field : changedFields.keySet()) {
                        if (oldMap.containsKey(field)) {
                            oldChangedData.put(field, oldMap.get(field));
                        }
                        if (newMap.containsKey(field)) {
                            newChangedData.put(field, newMap.get(field));
                        }
                    }

                    log.setOldData(objectMapper.writeValueAsString(oldChangedData));
                    log.setNewData(objectMapper.writeValueAsString(newChangedData));
                } else {
                    log.setOldData("{}");
                    log.setNewData("{}");
                }
            } else {
                if (newData != null) {
                    log.setNewData(objectMapper.writeValueAsString(newData));
                    log.setOldData("{}");
                }
            }

            if (log.getOldData() == null && log.getNewData() == null) {
                LogRecordServiceImpl.log.warn("数据变更日志 - oldData和newData都为空");
            }

            logMapper.insert(log);
        } catch (Exception e) {
            LogRecordServiceImpl.log.error("记录数据变更日志失败", e);
        }
    }

    @Override
    public void recordDataChangeLogSync(String module, String tableName, String recordId,
                                        String operationType, String content,
                                        Object oldData, Object newData,
                                        Integer userId, String ipAddress, boolean success, String errorMsg) {
        recordDataChangeLog(module, tableName, recordId, operationType, content,
                oldData, newData, userId, ipAddress, success, errorMsg);
    }

    @Override
    @Async("logTaskExecutor")
    public void recordLoginLog(String username, Integer userId, String ipAddress,
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
            LogRecordServiceImpl.log.error("记录登录日志失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return objectMapper.convertValue(obj, Map.class);
    }

    private Map<String, Object> getChangedFields(Object oldData, Object newData) {
        Map<String, Object> changedFields = new HashMap<>();

        if (oldData == null && newData == null) {
            return changedFields;
        }

        Map<String, Object> oldMap = convertToMap(oldData);
        Map<String, Object> newMap = convertToMap(newData);

        Set<String> allKeys = new java.util.HashSet<>();
        allKeys.addAll(oldMap.keySet());
        allKeys.addAll(newMap.keySet());

        for (String key : allKeys) {
            Object oldValue = oldMap.get(key);
            Object newValue = newMap.get(key);
            if (!isEqual(oldValue, newValue)) {
                changedFields.put(key, true);
            }
        }
        return changedFields;
    }

    private boolean isEqual(Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) {
            return true;
        }
        if (oldValue == null || newValue == null) {
            return false;
        }
        if (oldValue.getClass() == newValue.getClass()) {
            if (oldValue instanceof String) {
                return oldValue.equals(newValue);
            }
            return oldValue.equals(newValue);
        }
        String oldStr = String.valueOf(oldValue).trim();
        String newStr = String.valueOf(newValue).trim();
        if (oldStr.equals(newStr)) {
            return true;
        }
        try {
            Double oldNum = Double.parseDouble(oldStr);
            Double newNum = Double.parseDouble(newStr);
            return Math.abs(oldNum - newNum) < 0.0001;
        } catch (NumberFormatException e) {
            return oldStr.equals(newStr);
        }
    }

    private String buildOperationContent(String operationType, String content, boolean success, String errorMsg) {
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

        String result = sb.toString();
        if (result.length() > 500) {
            result = result.substring(0, 497) + "...";
        }
        return result;
    }

    private String buildLoginContent(String username, boolean success, String errorMsg) {
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

        String result = sb.toString();
        if (result.length() > 500) {
            result = result.substring(0, 497) + "...";
        }
        return result;
    }
}
