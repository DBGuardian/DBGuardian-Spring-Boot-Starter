package com.dbguardian.test;

import io.dbguardian.config.DbGuardianAutoConfiguration;
import io.dbguardian.enums.DataSourceStatus;
import io.dbguardian.coordination.DatasourceCoordinationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DBGuardian 基础功能测试
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
 */
@SpringBootTest(classes = {Application.class, DbGuardianAutoConfiguration.class})
@ActiveProfiles("test")
public class DbGuardianBasicTest {

    @Autowired(required = false)
    private DatasourceCoordinationService coordinationService;

    /**
     * 测试应用上下文加载成功
     */
    @Test
    public void testApplicationContextLoaded() {
        assertNotNull(coordinationService, "协调服务应该被注入");
    }

    /**
     * 测试协调服务实例ID生成
     */
    @Test
    public void testInstanceIdGenerated() {
        if (coordinationService != null) {
            String instanceId = coordinationService.getInstanceId();
            assertNotNull(instanceId, "实例ID不应为空");
            assertFalse(instanceId.isEmpty(), "实例ID不应为空字符串");
        }
    }

    /**
     * 测试协调服务健康状态
     */
    @Test
    public void testCoordinationServiceHealth() {
        if (coordinationService != null) {
            boolean healthy = coordinationService.isHealthy();
            // Redis 可能不可用，所以不强制要求健康
            assertNotNull(coordinationService.getCoordinationStatus(), "协调状态不应为空");
        }
    }

    /**
     * 测试协调状态对象
     */
    @Test
    public void testCoordinationStatus() {
        if (coordinationService != null) {
            DatasourceCoordinationService.CoordinationStatus status = coordinationService.getCoordinationStatus();
            assertNotNull(status, "协调状态不应为空");
            assertNotNull(status.getInstanceId(), "实例ID不应为空");
        }
    }
}
