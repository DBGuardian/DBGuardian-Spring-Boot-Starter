package com.erp.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

import java.time.Duration;

/**
 * Lettuce客户端配置自定义器
 * 用于优化Redis连接超时和命令超时设置
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Configuration
public class CustomLettuceClientConfigurationBuilderCustomizer implements LettuceClientConfigurationBuilderCustomizer {

    @Override
    public void customize(LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigurationBuilder) {
        // 配置客户端选项
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(10))  // 连接超时10秒
                        .keepAlive(true)  // 启用TCP keepalive
                        .tcpNoDelay(true)  // 禁用Nagle算法，减少延迟
                        .build())
                .timeoutOptions(TimeoutOptions.builder()
                        .fixedTimeout(Duration.ofSeconds(10))  // 命令超时10秒
                        .build())
                .autoReconnect(true)  // 自动重连
                .pingBeforeActivateConnection(true)  // 激活连接前ping
                .build();

        clientConfigurationBuilder.clientOptions(clientOptions);
        
        log.info("Lettuce客户端配置完成：连接超时10秒，命令超时10秒，启用自动重连");
    }
}

