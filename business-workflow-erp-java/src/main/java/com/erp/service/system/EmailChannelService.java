package com.erp.service.system;

import com.erp.controller.system.dto.EmailChannelConfigResponse;
import com.erp.controller.system.dto.EmailChannelConfigSaveRequest;
import com.erp.controller.system.dto.EmailChannelTestSendRequest;
import com.erp.controller.system.dto.EmailChannelTestSendResponse;
import com.erp.service.system.dto.EmailSendRequest;
import com.erp.service.system.dto.EmailSendResult;

/**
 * 邮件通道配置服务
 *
 * @author ERP
 */
public interface EmailChannelService {

    /**
     * 获取当前邮件通道配置
     *
     * @return 配置响应
     */
    EmailChannelConfigResponse getChannelConfig();

    /**
     * 保存或更新邮件通道配置
     *
     * @param request 配置请求
     * @return 更新后的配置
     */
    EmailChannelConfigResponse saveChannelConfig(EmailChannelConfigSaveRequest request);

    /**
     * 发送测试邮件
     *
     * @param request 测试请求
     * @return 测试结果
     */
    EmailChannelTestSendResponse testSend(EmailChannelTestSendRequest request);

    /**
     * 通用邮件发送（可供其它业务复用）
     *
     * @param request 发送请求
     * @return 发送结果
     */
    EmailSendResult sendEmail(EmailSendRequest request);
}

