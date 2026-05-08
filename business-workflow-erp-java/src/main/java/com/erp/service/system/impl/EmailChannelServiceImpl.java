package com.erp.service.system.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.erp.common.constant.RedisConstant;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.AesCryptoUtil;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.system.dto.EmailChannelConfigResponse;
import com.erp.controller.system.dto.EmailChannelConfigSaveRequest;
import com.erp.controller.system.dto.EmailChannelTestSendRequest;
import com.erp.controller.system.dto.EmailChannelTestSendResponse;
import com.erp.entity.system.EmailChannelConfig;
import com.erp.mapper.system.EmailChannelConfigMapper;
import com.erp.service.system.EmailChannelService;
import com.erp.service.system.dto.EmailSendRequest;
import com.erp.service.system.dto.EmailSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 邮件通道配置服务实现
 *
 * @author ERP
 */
@Slf4j
@Service
public class EmailChannelServiceImpl implements EmailChannelService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String DEFAULT_ENCRYPTION = "SSL";
    private static final int DEFAULT_SMTP_PORT = 465;
    private static final int DEFAULT_MAX_PER_HOUR = 200;
    private static final int DEFAULT_MAX_PER_DAY = 2000;

    @Value("${email.channel.aes-key}")
    private String aesKey;

    @Value("${email.channel.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;

    @Autowired
    private EmailChannelConfigMapper emailChannelConfigMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public EmailChannelConfigResponse getChannelConfig() {
        EmailChannelConfigResponse cached = (EmailChannelConfigResponse) redisTemplate.opsForValue()
                .get(RedisConstant.EMAIL_CHANNEL_CONFIG_KEY);
        if (cached != null) {
            return cached;
        }

        EmailChannelConfig latest = emailChannelConfigMapper.selectLatest();
        EmailChannelConfigResponse response = buildResponse(latest);
        redisTemplate.opsForValue().set(RedisConstant.EMAIL_CHANNEL_CONFIG_KEY, response, cacheTtlSeconds, TimeUnit.SECONDS);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailChannelConfigResponse saveChannelConfig(EmailChannelConfigSaveRequest request) {
        validateRequest(request);
        EmailChannelConfig latest = emailChannelConfigMapper.selectLatest();
        EmailChannelConfig config = latest == null ? new EmailChannelConfig() : latest;

        boolean isNew = config.getConfigId() == null;
        LocalDateTime now = LocalDateTime.now();

        config.setDisplayName(request.getDisplayName());
        config.setFromAddress(request.getFromAddress());
        config.setReplyTo(request.getReplyTo());
        config.setSmtpHost(request.getSmtpHost());
        config.setSmtpPort(request.getSmtpPort());
        config.setAuthMethod(request.getAuthMethod());
        config.setUsername(request.getUsername());
        config.setEncryption(request.getEncryption());
        config.setStatus(request.getStatus());
        config.setMaxPerHour(request.getMaxPerHour());
        config.setMaxPerDay(request.getMaxPerDay());
        config.setUpdatedBy(SecurityUtil.getCurrentUserId());
        config.setUpdateTime(now);
        if (isNew) {
            config.setCreateTime(now);
        }

        if (StrUtil.isNotBlank(request.getPassword())) {
            config.setPasswordCipher(AesCryptoUtil.encrypt(request.getPassword(), aesKey));
        } else if (!STATUS_ENABLED.equals(request.getStatus()) && StrUtil.isBlank(config.getPasswordCipher())) {
            // 禁用状态允许密码为空，但如果后续启用需要重新设置密码
            log.info("邮箱配置保存：禁用状态下未传入授权码，沿用旧值");
        }

        if (isNew) {
            emailChannelConfigMapper.insert(config);
        } else {
            int rows = emailChannelConfigMapper.updateById(config);
            if (rows == 0) {
                throw new BusinessException("更新邮箱配置失败：记录已被其他用户修改");
            }
        }

        EmailChannelConfigResponse response = buildResponse(config);
        refreshCache(response);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailChannelTestSendResponse testSend(EmailChannelTestSendRequest request) {
        EmailChannelConfig config = loadActiveConfig();
        EmailSendRequest sendRequest = new EmailSendRequest();
        sendRequest.setToList(Collections.singletonList(request.getTargetEmail()));
        sendRequest.setSubject(request.getSubject());
        sendRequest.setContent(request.getMessage());
        sendRequest.setHtml(false);

        EmailSendResult sendResult = doSend(sendRequest, config);

        LocalDateTime now = LocalDateTime.now();
        config.setLastSelfTestTime(now);
        config.setUpdateTime(now);
        config.setUpdatedBy(SecurityUtil.getCurrentUserId());
        int rows = emailChannelConfigMapper.updateById(config);
        if (rows == 0) {
            throw new BusinessException("更新邮箱配置失败：记录已被其他用户修改");
        }
        refreshCache(buildResponse(config));

        EmailChannelTestSendResponse response = new EmailChannelTestSendResponse();
        response.setTrackingId(sendResult.getTrackingId());
        response.setStatus(sendResult.getStatus());
        response.setQueuedAt(sendResult.getQueuedAt());
        response.setTargetEmail(request.getTargetEmail());
        return response;
    }

    @Override
    public EmailSendResult sendEmail(EmailSendRequest request) {
        EmailChannelConfig config = loadActiveConfig();
        return doSend(request, config);
    }

    private void validateRequest(EmailChannelConfigSaveRequest request) {
        if (request.getMaxPerDay() < request.getMaxPerHour()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "每日最大发送量不能小于每小时最大发送量");
        }
        EmailChannelConfig existing = emailChannelConfigMapper.selectLatest();
        boolean hasPasswordInput = StrUtil.isNotBlank(request.getPassword());
        boolean hasPasswordStored = existing != null && StrUtil.isNotBlank(existing.getPasswordCipher());
        if (STATUS_ENABLED.equals(request.getStatus()) && !hasPasswordInput && !hasPasswordStored) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "启用通道时必须提供授权码");
        }
    }

    private EmailSendResult doSend(EmailSendRequest request, EmailChannelConfig config) {
        validateSendRequest(request);
        List<String> targets = normalizeTargets(request.getToList());
        if (targets.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "收件人不能为空");
        }
        ensureChannelEnabled(config);
        String password = AesCryptoUtil.decrypt(config.getPasswordCipher(), aesKey);
        MailAccount account = buildMailAccount(config, password);

        LocalDateTime now = LocalDateTime.now();
        String trackingId;
        try {
            trackingId = MailUtil.send(account, targets, request.getSubject(), request.getContent(), request.isHtml());
        } catch (Exception ex) {
            log.error("发送邮件失败", ex);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "邮件发送失败：" + ex.getMessage());
        }

        EmailSendResult result = new EmailSendResult();
        result.setTrackingId(StrUtil.isNotBlank(trackingId) ? trackingId : IdUtil.fastSimpleUUID());
        result.setStatus("SENT");
        result.setQueuedAt(now);
        result.setTargetEmails(new ArrayList<>(targets));
        return result;
    }

    private EmailChannelConfig loadActiveConfig() {
        EmailChannelConfig config = emailChannelConfigMapper.selectLatest();
        if (config == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "尚未配置SMTP账号，无法发送邮件");
        }
        ensureChannelEnabled(config);
        if (StrUtil.isBlank(config.getPasswordCipher())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "邮件通道未配置授权码，无法发送邮件");
        }
        return config;
    }

    private void ensureChannelEnabled(EmailChannelConfig config) {
        if (!STATUS_ENABLED.equals(config.getStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "邮件通道已停用，无法发送邮件");
        }
    }

    private void validateSendRequest(EmailSendRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请求不能为空");
        }
        if (StrUtil.isBlank(request.getSubject())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "邮件主题不能为空");
        }
        if (StrUtil.isBlank(request.getContent())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "邮件内容不能为空");
        }
    }

    private List<String> normalizeTargets(List<String> rawList) {
        if (CollUtil.isEmpty(rawList)) {
            return Collections.emptyList();
        }
        List<String> targets = new ArrayList<>();
        for (String item : rawList) {
            if (StrUtil.isNotBlank(item)) {
                targets.add(item.trim());
            }
        }
        return targets;
    }

    private MailAccount buildMailAccount(EmailChannelConfig config, String password) {
        MailAccount account = new MailAccount();
        account.setHost(config.getSmtpHost());
        account.setPort(config.getSmtpPort() != null ? config.getSmtpPort() : DEFAULT_SMTP_PORT);
        account.setAuth(true);
        account.setUser(config.getUsername());
        account.setPass(password);
        account.setFrom(StrUtil.isNotBlank(config.getDisplayName())
                ? String.format("%s <%s>", config.getDisplayName(), config.getFromAddress())
                : config.getFromAddress());
        account.setCharset(StandardCharsets.UTF_8);
        account.setTimeout(10000);
        account.setConnectionTimeout(10000);
        account.setWriteTimeout(10000);

        String encryption = config.getEncryption();
        if (DEFAULT_ENCRYPTION.equalsIgnoreCase(encryption)) {
            account.setSslEnable(true);
        } else if ("STARTTLS".equalsIgnoreCase(encryption)) {
            account.setStarttlsEnable(true);
        } else {
            account.setSslEnable(false);
            account.setStarttlsEnable(false);
        }
        return account;
    }

    private EmailChannelConfigResponse buildResponse(EmailChannelConfig config) {
        EmailChannelConfigResponse response = new EmailChannelConfigResponse();
        if (config == null) {
            response.setDisplayName("");
            response.setFromAddress("");
            response.setReplyTo("");
            response.setSmtpHost("");
            response.setSmtpPort(DEFAULT_SMTP_PORT);
            response.setAuthMethod("LOGIN");
            response.setUsername("");
            response.setEncryption(DEFAULT_ENCRYPTION);
            response.setStatus(STATUS_ENABLED);
            response.setMaxPerHour(DEFAULT_MAX_PER_HOUR);
            response.setMaxPerDay(DEFAULT_MAX_PER_DAY);
            response.setHasPassword(false);
            return response;
        }
        response.setConfigId(config.getConfigId());
        response.setDisplayName(config.getDisplayName());
        response.setFromAddress(config.getFromAddress());
        response.setReplyTo(config.getReplyTo());
        response.setSmtpHost(config.getSmtpHost());
        response.setSmtpPort(config.getSmtpPort());
        response.setAuthMethod(config.getAuthMethod());
        response.setUsername(config.getUsername());
        response.setEncryption(config.getEncryption());
        response.setStatus(config.getStatus());
        response.setMaxPerHour(config.getMaxPerHour());
        response.setMaxPerDay(config.getMaxPerDay());
        response.setHasPassword(StrUtil.isNotBlank(config.getPasswordCipher()));
        response.setUpdatedAt(config.getUpdateTime());
        response.setUpdatedBy(config.getUpdatedBy());
        response.setLastSelfTestTime(config.getLastSelfTestTime());
        return response;
    }

    private void refreshCache(EmailChannelConfigResponse response) {
        redisTemplate.delete(RedisConstant.EMAIL_CHANNEL_CONFIG_KEY);
        redisTemplate.opsForValue().set(RedisConstant.EMAIL_CHANNEL_CONFIG_KEY, response, cacheTtlSeconds, TimeUnit.SECONDS);
    }
}

