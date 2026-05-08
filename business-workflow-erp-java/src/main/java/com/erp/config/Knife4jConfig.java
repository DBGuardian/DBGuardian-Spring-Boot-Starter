package com.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * Knife4j配置类
 * 
 * 注意：@EnableSwagger2WebMvc 虽然标记为deprecated，但对于Knife4j 3.0.3版本是必需的
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Configuration
@EnableSwagger2WebMvc
@SuppressWarnings("deprecation")
public class Knife4jConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.erp.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("危险废物处理企业ERP管理系统API文档")
                .description("危险废物处理企业ERP管理系统接口文档")
                .version("1.0.0")
                .contact(new Contact("ERP System", "", "support@erp.com"))
                .build();
    }
}

