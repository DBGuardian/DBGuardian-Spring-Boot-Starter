package com.erp.mapper.dashboard;

import com.erp.controller.dashboard.dto.DashboardDataResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作台Mapper接口
 *
 * @author ERP System
 * @date 2026-01-12
 */
@Mapper
public interface DashboardMapper {

    /**
     * 获取统计数据
     *
     * @return 统计数据
     */
    DashboardDataResponse.Statistics getStatistics();

    /**
     * 获取待办事项列表
     *
     * @return 待办事项列表
     */
    List<DashboardDataResponse.TodoItem> getTodoList();

    /**
     * 获取消息通知列表
     *
     * @param userId 用户ID
     * @return 消息通知列表
     */
    List<DashboardDataResponse.MessageItem> getMessageList(@Param("userId") Integer userId);
}


