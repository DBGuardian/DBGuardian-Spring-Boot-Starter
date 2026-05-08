package com.erp.service.dashboard.impl;

import com.erp.controller.dashboard.dto.DashboardDataResponse;
import com.erp.entity.system.Employee;
import com.erp.mapper.dashboard.DashboardMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.service.dashboard.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作台服务实现类
 *
 * @author ERP System
 * @date 2026-01-12
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public DashboardDataResponse getDashboardData(Integer userId) {
        log.info("获取工作台数据，用户ID: {}", userId);
        DashboardDataResponse response = new DashboardDataResponse();

        // 1. 获取用户信息
        Employee employee = employeeMapper.selectById(userId);
        log.info("查询到的员工信息: {}", employee);
        if (employee != null) {
            DashboardDataResponse.UserInfo userInfo = new DashboardDataResponse.UserInfo();
            userInfo.setName(employee.getEmployeeName());
            userInfo.setDepartment(employee.getDepartment());
            userInfo.setPosition(employee.getJobTitle());
            response.setUserInfo(userInfo);
            log.info("设置用户信息: {}", userInfo);
        } else {
            log.warn("未找到员工信息，用户ID: {}", userId);
        }

        // 2. 获取统计数据（一次性查询）
        DashboardDataResponse.Statistics statistics = dashboardMapper.getStatistics();
        log.info("查询到的统计数据: {}", statistics);
        response.setStatistics(statistics);

        // 3. 获取待办事项（一次性查询）
        List<DashboardDataResponse.TodoItem> todoList = dashboardMapper.getTodoList();
        response.setTodoList(todoList);

        // 4. 获取消息通知（一次性查询）
        List<DashboardDataResponse.MessageItem> messageList = dashboardMapper.getMessageList(userId);
        // 处理消息时间显示
        if (messageList != null) {
            for (DashboardDataResponse.MessageItem message : messageList) {
                if (message.getTime() != null && !message.getTime().isEmpty()) {
                    message.setTime(formatRelativeTime(message.getTime()));
                }
            }
        }
        response.setMessageList(messageList);

        return response;
    }

    /**
     * 格式化相对时间
     *
     * @param timeStr 时间字符串（格式：yyyy-MM-dd HH:mm:ss）
     * @return 相对时间字符串（如：2小时前、1天前）
     */
    private String formatRelativeTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return "";
        }
        try {
            LocalDateTime messageTime = LocalDateTime.parse(timeStr.replace(" ", "T"));
            LocalDateTime now = LocalDateTime.now();
            long hours = ChronoUnit.HOURS.between(messageTime, now);
            long days = ChronoUnit.DAYS.between(messageTime, now);

            if (days > 0) {
                return days + "天前";
            } else if (hours > 0) {
                return hours + "小时前";
            } else {
                long minutes = ChronoUnit.MINUTES.between(messageTime, now);
                if (minutes > 0) {
                    return minutes + "分钟前";
                } else {
                    return "刚刚";
                }
            }
        } catch (Exception e) {
            log.warn("格式化时间失败：{}", timeStr, e);
            return timeStr;
        }
    }
}

