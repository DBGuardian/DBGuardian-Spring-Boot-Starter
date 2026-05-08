package com.erp.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.system.dto.*;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageService;
import com.erp.common.annotation.RequirePagePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 消息通知控制器
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/message")
@Api(tags = "消息通知管理")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 分页查询消息列表
     */
    @RequirePagePermission("个人中心:页面")
    @GetMapping("/list")
    @ApiOperation(value = "分页查询消息列表", notes = "支持按消息类型、状态、优先级、关键词筛选")
    public Result<IPage<MessagePageResponse>> getMessagePage(@Valid MessagePageRequest request) {
        try {
            IPage<MessagePageResponse> page = messageService.getMessagePage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询消息列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取消息详情
     */
    @GetMapping("/{messageId}")
    @ApiOperation(value = "获取消息详情", notes = "查看消息详情时自动标记为已读")
    public Result<MessageDetailResponse> getMessageDetail(@PathVariable Integer messageId) {
        try {
            MessageDetailResponse response = messageService.getMessageDetail(messageId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取消息详情失败: messageId={}", messageId, e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/{messageId}/read")
    @ApiOperation(value = "标记消息为已读")
    public Result<String> markAsRead(@PathVariable Integer messageId, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean markSuccess = false;
        String errorMessage = null;

        try {
            messageService.markAsRead(messageId);
            markSuccess = true;
            return Result.success("操作成功");
        } catch (BusinessException e) {
            errorMessage = e.getMessage();
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("标记消息已读失败: messageId={}", messageId, e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "操作失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("标记消息已读：消息ID=%s", messageId);
                logRecordService.recordOperationLog("个人中心", "已读",
                        logContent, userId, ipAddress, markSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录标记已读操作日志失败", logEx);
            }
        }
    }

    /**
     * 批量标记消息为已读
     */
    @PutMapping("/batch-read")
    @ApiOperation(value = "批量标记消息为已读")
    public Result<String> batchMarkAsRead(@RequestBody List<Integer> messageIds, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean markSuccess = false;
        String errorMessage = null;

        try {
            messageService.batchMarkAsRead(messageIds);
            markSuccess = true;
            return Result.success("操作成功");
        } catch (BusinessException e) {
            errorMessage = e.getMessage();
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量标记消息已读失败: messageIds={}", messageIds, e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "操作失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("批量标记消息已读：消息IDs=%s，数量=%s",
                        messageIds, messageIds != null ? messageIds.size() : 0);
                logRecordService.recordOperationLog("个人中心", "批量已读",
                        logContent, userId, ipAddress, markSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录批量已读操作日志失败", logEx);
            }
        }
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    @ApiOperation(value = "删除消息", notes = "软删除，消息不会真正删除")
    public Result<String> deleteMessage(@PathVariable Integer messageId, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;

        try {
            messageService.deleteMessage(messageId);
            deleteSuccess = true;
            return Result.success("删除成功");
        } catch (BusinessException e) {
            errorMessage = e.getMessage();
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除消息失败: messageId={}", messageId, e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("删除消息：消息ID=%s", messageId);
                logRecordService.recordOperationLog("个人中心", "删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录删除消息操作日志失败", logEx);
            }
        }
    }

    /**
     * 批量删除消息
     */
    @DeleteMapping("/batch")
    @ApiOperation(value = "批量删除消息", notes = "软删除，消息不会真正删除")
    public Result<String> batchDeleteMessages(@RequestBody List<Integer> messageIds, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;

        try {
            messageService.batchDeleteMessages(messageIds);
            deleteSuccess = true;
            return Result.success("删除成功");
        } catch (BusinessException e) {
            errorMessage = e.getMessage();
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量删除消息失败: messageIds={}", messageIds, e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("批量删除消息：消息IDs=%s，数量=%s",
                        messageIds, messageIds != null ? messageIds.size() : 0);
                logRecordService.recordOperationLog("个人中心", "批量删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录批量删除消息操作日志失败", logEx);
            }
        }
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    @ApiOperation(value = "获取未读消息数量", notes = "用于前端消息角标显示")
    public Result<Integer> getUnreadCount() {
        try {
            int count = messageService.getUnreadCount();
            return Result.success("查询成功", count);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取未读消息数量失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取消息统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取消息统计信息", notes = "包含各类型、优先级、状态的消息数量统计")
    public Result<MessageStatisticsResponse> getMessageStatistics() {
        try {
            MessageStatisticsResponse response = messageService.getMessageStatistics();
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取消息统计信息失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 全部标记为已读
     */
    @PutMapping("/mark-all-read")
    @ApiOperation(value = "全部标记为已读", notes = "将当前用户的所有未读消息标记为已读")
    public Result<String> markAllAsRead(HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean markSuccess = false;
        String errorMessage = null;

        try {
            messageService.markAllAsRead();
            markSuccess = true;
            return Result.success("操作成功");
        } catch (BusinessException e) {
            errorMessage = e.getMessage();
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("全部标记已读失败", e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "操作失败：" + e.getMessage());
        } finally {
            try {
                String logContent = "全部标记消息已读";
                logRecordService.recordOperationLog("个人中心", "全部已读",
                        logContent, userId, ipAddress, markSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录全部已读操作日志失败", logEx);
            }
        }
    }

    /**
     * 清空所有消息
     */
    @DeleteMapping("/clear-all")
    @ApiOperation(value = "清空所有消息", notes = "软删除当前用户的所有消息")
    public Result<String> clearAllMessages(HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean clearSuccess = false;
        String errorMessage = null;

        try {
            messageService.clearAllMessages();
            clearSuccess = true;
            return Result.success("清空成功");
        } catch (BusinessException e) {
            errorMessage = e.getMessage();
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("清空消息失败", e);
            errorMessage = e.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "清空失败：" + e.getMessage());
        } finally {
            try {
                String logContent = "清空所有消息";
                logRecordService.recordOperationLog("个人中心", "清空",
                        logContent, userId, ipAddress, clearSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录清空消息操作日志失败", logEx);
            }
        }
    }
}

































