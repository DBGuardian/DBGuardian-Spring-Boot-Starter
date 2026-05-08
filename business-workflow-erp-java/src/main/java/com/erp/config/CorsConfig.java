package com.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有域名进行跨域调用（使用 OriginPattern 支持通配符）
        config.addAllowedOriginPattern("*");
        // 允许所有请求头（包括Authorization）
        config.addAllowedHeader("*");
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 允许携带凭证（注意：当 allowCredentials 为 true 时，不能使用 "*" 作为 allowedOrigin，但可以使用 allowedOriginPattern）
        config.setAllowCredentials(true);
        // 暴露响应头
        config.addExposedHeader("Authorization");
        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}











































