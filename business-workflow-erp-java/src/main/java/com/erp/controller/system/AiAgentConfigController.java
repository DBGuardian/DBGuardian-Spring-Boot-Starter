package com.erp.controller.system;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.system.dto.AiAgentConfigResponse;
import com.erp.controller.system.dto.AiAgentConfigSaveRequest;
import com.erp.controller.system.dto.AiAgentGlobalConfigResponse;
import com.erp.controller.system.dto.AiAgentGlobalConfigSaveRequest;
import com.erp.service.system.AiAgentConfigService;
import com.erp.service.system.ILogRecordService;
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
 * 大模型智能体配置控制器
 *
 * 放在系统管理模块下，提供智能体列表和全局配置的管理接口
 *
 * @author ERP
 */
@Slf4j
@RestController
@RequestMapping("/system/ai-agent")
@Api(tags = "大模型AI配置")
@Validated
public class AiAgentConfigController {

    @Autowired
    private AiAgentConfigService aiAgentConfigService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 查询智能体列表
     */
    @RequirePagePermission("系统管理:AI设置:页面")
    @GetMapping("/list")
    @ApiOperation(value = "查询智能体列表", notes = "按排序号返回所有已配置的大模型智能体")
    public Result<List<AiAgentConfigResponse>> listAgents() {
        List<AiAgentConfigResponse> list = aiAgentConfigService.listAgents();
        return Result.success("查询成功", list);
    }

    /**
     * 保存或更新智能体配置
     */
    @PostMapping("/save")
    @ApiOperation(value = "保存智能体配置", notes = "用于新增或编辑单个智能体配置")
    public Result<Void> saveAgent(@Valid @RequestBody AiAgentConfigSaveRequest request,
                                    HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean saveSuccess = false;
        String errorMessage = null;

        try {
            aiAgentConfigService.saveAgent(request);
            saveSuccess = true;
            return Result.success("保存成功", null);
        } catch (BusinessException ex) {
            log.warn("保存智能体配置业务异常：{}", ex.getMessage());
            errorMessage = ex.getMessage();
            return Result.error(ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("保存智能体配置失败", ex);
            errorMessage = ex.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "保存失败：" + ex.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("保存AI智能体配置：智能体名称=%s，智能体ID=%s",
                        request.getAgentName(), request.getAgentId());
                logRecordService.recordOperationLog("AI智能体配置", request.getAgentId() != null ? "更新" : "新增",
                        logContent, userId, ipAddress, saveSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录保存AI智能体配置操作日志失败", logEx);
            }
        }
    }

    /**
     * 删除智能体配置
     */
    @PostMapping("/delete/{agentId}")
    @ApiOperation(value = "删除智能体配置", notes = "按智能体编号删除配置")
    public Result<Void> deleteAgent(@PathVariable("agentId") Integer agentId,
                                     HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;

        try {
            aiAgentConfigService.deleteAgent(agentId);
            deleteSuccess = true;
            return Result.success("删除成功", null);
        } catch (BusinessException ex) {
            log.warn("删除智能体配置业务异常：{}", ex.getMessage());
            errorMessage = ex.getMessage();
            return Result.error(ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("删除智能体配置失败", ex);
            errorMessage = ex.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + ex.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("删除AI智能体配置：智能体ID=%s", agentId);
                logRecordService.recordOperationLog("AI智能体配置", "删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录删除AI智能体配置操作日志失败", logEx);
            }
        }
    }

    /**
     * 设置默认智能体
     */
    @PostMapping("/set-default/{agentId}")
    @ApiOperation(value = "设置默认智能体", notes = "将指定智能体标记为默认使用")
    public Result<Void> setDefaultAgent(@PathVariable("agentId") Integer agentId,
                                        HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean setSuccess = false;
        String errorMessage = null;

        try {
            aiAgentConfigService.setDefaultAgent(agentId);
            setSuccess = true;
            return Result.success("设置成功", null);
        } catch (BusinessException ex) {
            log.warn("设置默认智能体业务异常：{}", ex.getMessage());
            errorMessage = ex.getMessage();
            return Result.error(ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("设置默认智能体失败", ex);
            errorMessage = ex.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "设置失败：" + ex.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("设置默认AI智能体：智能体ID=%s", agentId);
                logRecordService.recordOperationLog("AI智能体配置", "设置默认",
                        logContent, userId, ipAddress, setSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录设置默认AI智能体操作日志失败", logEx);
            }
        }
    }

    /**
     * 查询全局限流配置
     */
    @GetMapping("/global-config")
    @ApiOperation(value = "查询全局配置", notes = "查询每秒最大请求数、每次请求最大文本长度、每次请求最大段落数")
    public Result<AiAgentGlobalConfigResponse> getGlobalConfig() {
        AiAgentGlobalConfigResponse response = aiAgentConfigService.getGlobalConfig();
        return Result.success("查询成功", response);
    }

    /**
     * 保存全局限流配置
     */
    @PostMapping("/global-config")
    @ApiOperation(value = "保存全局配置", notes = "保存每秒最大请求数、每次请求最大文本长度、每次请求最大段落数")
    public Result<Void> saveGlobalConfig(@Valid @RequestBody AiAgentGlobalConfigSaveRequest request,
                                         HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean saveSuccess = false;
        String errorMessage = null;

        try {
            aiAgentConfigService.saveGlobalConfig(request);
            saveSuccess = true;
            return Result.success("保存成功", null);
        } catch (BusinessException ex) {
            log.warn("保存AI全局配置业务异常：{}", ex.getMessage());
            errorMessage = ex.getMessage();
            return Result.error(ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("保存AI全局配置失败", ex);
            errorMessage = ex.getMessage();
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "保存失败：" + ex.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("保存AI全局配置：每秒最大请求数=%s，每次最大文本长度=%s，每次最大段落数=%s",
                        request.getMaxRequestsPerSecond(), request.getMaxTextLengthPerRequest(), request.getMaxParagraphsPerRequest());
                logRecordService.recordOperationLog("AI智能体配置", "更新全局配置",
                        logContent, userId, ipAddress, saveSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录保存AI全局配置操作日志失败", logEx);
            }
        }
    }
}


