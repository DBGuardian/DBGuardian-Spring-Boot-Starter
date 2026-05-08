package com.erp.controller.system;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.system.dto.EmailChannelConfigResponse;
import com.erp.controller.system.dto.EmailChannelConfigSaveRequest;
import com.erp.controller.system.dto.EmailChannelTestSendRequest;
import com.erp.controller.system.dto.EmailChannelTestSendResponse;
import com.erp.service.system.EmailChannelService;
import com.erp.common.annotation.RequirePagePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 邮件通道配置控制器
 *
 * @author ERP
 */
@Slf4j
@RestController
@RequestMapping("/email-channel")
@Api(tags = "邮件通道配置")
@Validated
public class EmailChannelController {

    @Autowired
    private EmailChannelService emailChannelService;

    /**
     * 获取SMTP配置
     */
    @RequirePagePermission("系统管理:邮件配置:页面")
    @GetMapping("/config")
    @ApiOperation(value = "获取SMTP配置", notes = "返回邮件通道配置详情")
    public Result<EmailChannelConfigResponse> getConfig() {
        EmailChannelConfigResponse response = emailChannelService.getChannelConfig();
        return Result.success("查询成功", response);
    }

    /**
     * 保存SMTP配置
     */
    @PostMapping("/config")
    @ApiOperation(value = "保存SMTP配置", notes = "保存并刷新Redis缓存")
    public Result<EmailChannelConfigResponse> saveConfig(@Valid @RequestBody EmailChannelConfigSaveRequest request) {
        try {
            log.info("保存SMTP配置：displayName={}, status={}", request.getDisplayName(), request.getStatus());
            EmailChannelConfigResponse response = emailChannelService.saveChannelConfig(request);
            return Result.success("保存成功", response);
        } catch (BusinessException ex) {
            log.warn("保存SMTP配置业务异常：{}", ex.getMessage());
            return Result.error(ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("保存SMTP配置失败", ex);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "保存失败：" + ex.getMessage());
        }
    }

    /**
     * 发送测试邮件
     */
    @PostMapping("/test-send")
    @ApiOperation(value = "发送测试邮件", notes = "使用当前配置发送自检邮件")
    public Result<EmailChannelTestSendResponse> testSend(@Valid @RequestBody EmailChannelTestSendRequest request) {
        try {
            EmailChannelTestSendResponse response = emailChannelService.testSend(request);
            return Result.success("测试邮件已发送", response);
        } catch (BusinessException ex) {
            log.warn("测试邮件发送业务异常：{}", ex.getMessage());
            return Result.error(ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("测试邮件发送失败", ex);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "测试邮件发送失败：" + ex.getMessage());
        }
    }
}






























