package com.erp.service.system.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.controller.system.dto.LogDetailResponse;
import com.erp.controller.system.dto.LogExportRequest;
import com.erp.controller.system.dto.LogPageRequest;
import com.erp.controller.system.dto.LogPageResponse;
import com.erp.entity.system.Log;
import com.erp.mapper.system.LogMapper;
import com.erp.service.system.LogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日志管理服务实现类
 * 
 * @author ERP System
 * @date 2025-12-08
 */
@Service
public class LogServiceImpl implements LogService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LogServiceImpl.class);

    @Autowired
    private LogMapper logMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public IPage<LogPageResponse> getLogPage(LogPageRequest request) {
        Page<Log> page = new Page<>(request.getCurrent(), request.getSize());
        
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        
        IPage<Log> logPage = logMapper.selectLogPage(
                page,
                request.getKeyword(),
                request.getLogType(),
                request.getStatus(),
                request.getModule(),
                request.getIpAddress(),
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder()
        );
        
        return logPage.convert(this::convertToPageResponse);
    }

    @Override
    public IPage<LogPageResponse> getOperationLogPage(LogPageRequest request) {
        Page<Log> page = new Page<>(request.getCurrent(), request.getSize());
        
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        
        // 根据用户名查询用户编码列表（简化处理，实际应该通过用户服务查询）
        List<Integer> userIds = null; // TODO: 如果需要按用户名筛选，需要查询用户编码列表
        
        IPage<Log> logPage = logMapper.selectOperationLogPage(
                page,
                request.getKeyword(),
                request.getModule(),
                request.getOperationTypes(),
                userIds,
                request.getIpAddress(),
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder()
        );
        
        return logPage.convert(this::convertToPageResponse);
    }

    @Override
    public IPage<LogPageResponse> getDataChangeLogPage(LogPageRequest request) {
        Page<Log> page = new Page<>(request.getCurrent(), request.getSize());
        
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        
        List<Integer> userIds = null; // TODO: 如果需要按用户名筛选，需要查询用户编码列表
        
        IPage<Log> logPage = logMapper.selectDataChangeLogPage(
                page,
                request.getKeyword(),
                request.getModule(),
                request.getChangeTypes(),
                userIds,
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder()
        );
        
        return logPage.convert(this::convertToPageResponse);
    }

    @Override
    public IPage<LogPageResponse> getLoginLogPage(LogPageRequest request) {
        Page<Log> page = new Page<>(request.getCurrent(), request.getSize());
        
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        
        IPage<Log> logPage = logMapper.selectLoginLogPage(
                page,
                request.getKeyword(),
                request.getLoginResult(),
                request.getIpAddress(),
                startTime,
                endTime,
                request.getSortField(),
                request.getSortOrder()
        );
        
        return logPage.convert(this::convertToPageResponse);
    }

    @Override
    public LogDetailResponse getLogDetail(Integer logId) {
        Log log = logMapper.selectLogDetail(logId);
        if (log == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "日志不存在");
        }
        
        return convertToDetailResponse(log);
    }

    @Override
    public List<LogPageResponse> exportLogs(LogExportRequest request) {
        LocalDateTime startTime = parseDateTime(request.getStartTime());
        LocalDateTime endTime = parseDateTime(request.getEndTime());
        
        List<Log> logs = logMapper.selectLogsForExport(
                request.getKeyword(),
                request.getLogType(),
                request.getStatus(),
                request.getModule(),
                request.getIpAddress(),
                startTime,
                endTime,
                "selection".equals(request.getMode()) ? request.getLogIds() : null
        );
        
        List<LogPageResponse> responses = new ArrayList<>();
        for (Log log : logs) {
            responses.add(convertToPageResponse(log));
        }
        
        return responses;
    }

    /**
     * 转换为分页响应对象
     */
    private LogPageResponse convertToPageResponse(Log log) {
        LogPageResponse response = new LogPageResponse();
        response.setLogId(log.getLogId());
        response.setCode(formatLogCode(log.getLogId(), log.getOperationModule()));
        response.setType(inferLogType(log));
        response.setModule(log.getOperationModule());
        response.setAction(log.getOperationContent());
        response.setOperator(log.getOperatorName());
        response.setTime(log.getOperationTime());
        response.setIp(log.getIpAddress());
        response.setStatus(inferStatus(log));
        
        // 数据变更日志专用字段 - 从操作内容中提取
        if ("数据变更".equals(response.getType())) {
            // 从操作内容中提取表名和记录ID
            String content = log.getOperationContent();
            if (content != null) {
                // 提取记录ID（格式：更新客户：ID=123 或 新增客户：XX公司）
                String recordId = extractRecordId(content);
                response.setRecordId(recordId);
                
                // 根据操作模块推断表名
                String tableName = inferTableName(log.getOperationModule(), content);
                response.setTableName(tableName);
            }
        }
        
        // 操作日志专用字段
        if ("操作日志".equals(response.getType())) {
            response.setOperationType(inferOperationType(log));
        }
        
        // 登录日志专用字段
        if ("登录".equals(response.getType())) {
            response.setResult(inferLoginResult(log));
            response.setDurationMs(log.getDurationMs());
        }
        
        return response;
    }

    /**
     * 转换为详情响应对象
     */
    private LogDetailResponse convertToDetailResponse(Log log) {
        LogDetailResponse response = new LogDetailResponse();
        response.setLogId(log.getLogId());
        response.setCode(formatLogCode(log.getLogId(), log.getOperationModule()));
        response.setType(inferLogType(log));
        response.setModule(log.getOperationModule());
        response.setTableName(log.getTableName());
        response.setAction(log.getOperationContent());
        response.setOperator(log.getOperatorName());
        response.setTime(log.getOperationTime());
        response.setIp(log.getIpAddress());
        response.setStatus(inferStatus(log));
        response.setErrorMsg(log.getErrorMsg());
        
        // 解析JSON数据
        try {
            if (StrUtil.isNotBlank(log.getOldData())) {
                response.setOldData(objectMapper.readValue(log.getOldData(), new TypeReference<Map<String, Object>>() {}));
            }
            if (StrUtil.isNotBlank(log.getNewData())) {
                response.setNewData(objectMapper.readValue(log.getNewData(), new TypeReference<Map<String, Object>>() {}));
            }
        } catch (Exception e) {
            logger.warn("解析日志JSON数据失败: logId={}", log.getLogId(), e);
        }
        
        // 构建字段差异列表
        if (response.getOldData() != null || response.getNewData() != null) {
            response.setFieldDiffs(buildFieldDiffs(response.getOldData(), response.getNewData()));
        }
        
        return response;
    }

    /**
     * 推断日志类型
     */
    private String inferLogType(Log log) {
        if (log.getOperationModule() != null && log.getOperationModule().equals("login")) {
            return "登录";
        }
        // 判断是否为数据变更日志：原始数据和新数据都不为空
        if (log.getOldData() != null && log.getNewData() != null) {
            String oldData = log.getOldData().trim();
            String newData = log.getNewData().trim();
            // 排除空字符串，但允许空对象 "{}" 和 "已删除"
            if (!oldData.isEmpty() && !newData.isEmpty()) {
                return "数据变更";
            }
        }
        return "操作日志";
    }

    /**
     * 推断状态
     */
    private String inferStatus(Log log) {
        String content = log.getOperationContent();
        if (content == null) {
            return "unknown";
        }
        if (content.contains("失败") || content.contains("错误")) {
            return "failed";
        }
        if (content.contains("成功")) {
            return "success";
        }
        return "success"; // 默认成功
    }

    /**
     * 推断操作类型
     */
    private String inferOperationType(Log log) {
        String content = log.getOperationContent();
        if (content == null) {
            return null;
        }
        if (content.contains("新增")) {
            return "新增";
        }
        if (content.contains("编辑") || content.contains("更新")) {
            return "编辑";
        }
        if (content.contains("删除")) {
            return "删除";
        }
        if (content.contains("审核")) {
            return "审核";
        }
        if (content.contains("导出")) {
            return "导出";
        }
        return null;
    }

    /**
     * 推断登录结果
     */
    private String inferLoginResult(Log log) {
        String content = log.getOperationContent();
        if (content == null) {
            return "未知";
        }
        if (content.contains("成功")) {
            return "成功";
        }
        if (content.contains("失败") || content.contains("错误")) {
            return "失败";
        }
        return "未知";
    }

    /**
     * 格式化日志编号
     */
    private String formatLogCode(Integer logId, String module) {
        if (logId == null) {
            return null;
        }
        String prefix = "OP";
        if (module != null) {
            if (module.equals("login")) {
                prefix = "LG";
            } else if (module.contains("customer") || module.contains("客户")) {
                prefix = "DC";
            }
        }
        return String.format("%s%08d", prefix, logId);
    }

    /**
     * 构建字段差异列表
     */
    private List<LogDetailResponse.FieldDiff> buildFieldDiffs(Map<String, Object> oldData, Map<String, Object> newData) {
        List<LogDetailResponse.FieldDiff> diffs = new ArrayList<>();
        
        Set<String> allKeys = new java.util.HashSet<>();
        if (oldData != null) {
            allKeys.addAll(oldData.keySet());
        }
        if (newData != null) {
            allKeys.addAll(newData.keySet());
        }
        
        for (String key : allKeys) {
            LogDetailResponse.FieldDiff diff = new LogDetailResponse.FieldDiff();
            diff.setKey(key);
            
            Object oldVal = oldData != null ? oldData.get(key) : null;
            Object newVal = newData != null ? newData.get(key) : null;
            
            diff.setOldVal(oldVal);
            diff.setNewVal(newVal);
            
            if (oldVal == null && newVal != null) {
                diff.setStatus("added");
            } else if (oldVal != null && newVal == null) {
                diff.setStatus("removed");
            } else if (oldVal != null && !oldVal.equals(newVal)) {
                diff.setStatus("changed");
            } else {
                diff.setStatus("same");
            }
            
            diffs.add(diff);
        }
        
        return diffs;
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (StrUtil.isBlank(dateTimeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            logger.warn("解析日期时间失败: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 从操作内容中提取记录ID
     */
    private String extractRecordId(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        // 匹配格式：ID=123 或 ID：123
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("ID[=：](\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 根据操作模块和内容推断表名
     */
    private String inferTableName(String module, String content) {
        if (StrUtil.isBlank(module)) {
            return null;
        }
        // 根据操作模块推断表名
        if (module.contains("客户")) {
            return "CUSTOMER";
        } else if (module.contains("报价单")) {
            return "QUOTATION";
        } else if (module.contains("合同")) {
            return "CONTRACT";
        } else if (module.contains("结算单") || module.contains("结算")) {
            return "SETTLEMENT";
        } else if (module.contains("发票")) {
            return "INVOICE";
        } else if (module.contains("回单") || module.contains("收款")) {
            return "RECEIPT";
        } else if (module.contains("对账")) {
            return "RECONCILIATION";
        } else if (module.contains("员工")) {
            return "EMPLOYEE";
        } else if (module.contains("危险废物") || module.contains("废物名录")) {
            return "HAZARDOUS_WASTE_ITEM";
        } else if (module.contains("跟进")) {
            return "CUSTOMER_FOLLOW_UP";
        }
        return null;
    }
}

