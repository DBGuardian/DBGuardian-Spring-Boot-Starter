package com.test;

import com.test.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DBGuardian 基础功能测试
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class DbGuardianBasicTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    @Qualifier("datasourceCoordinationService")
    private Object coordinationService;

    /**
     * 测试应用上下文加载成功
     */
    @Test
    public void testApplicationContextLoaded() {
        assertNotNull(applicationContext);
    }

    /**
     * 测试协调服务 bean 存在
     */
    @Test
    public void testCoordinationServiceBeanPresent() {
        assertTrue(applicationContext.containsBean("datasourceCoordinationService"));
    }

    /**
     * 测试协调服务可注入
     */
    @Test
    public void testCoordinationServiceInjectable() {
        assertNotNull(coordinationService, "协调服务应该被注入");
    }

    /**
     * 测试协调服务实例ID生成
     */
    @Test
    public void testInstanceIdGenerated() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method method = coordinationService.getClass().getMethod("getInstanceId");
                Object instanceId = method.invoke(coordinationService);
                assertNotNull(instanceId, "实例ID不应为空");
                assertFalse(instanceId.toString().isEmpty(), "实例ID不应为空字符串");
            } catch (Exception e) {
                fail("获取实例ID失败: " + e.getMessage());
            }
        }
    }

    /**
     * 测试协调服务健康状态
     */
    @Test
    public void testCoordinationServiceHealth() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method method = coordinationService.getClass().getMethod("isHealthy");
                method.invoke(coordinationService);
                // Redis 可能不可用，所以不强制要求健康
                java.lang.reflect.Method statusMethod = coordinationService.getClass().getMethod("getCoordinationStatus");
                Object status = statusMethod.invoke(coordinationService);
                assertNotNull(status, "协调状态不应为空");
            } catch (Exception e) {
                fail("获取协调状态失败: " + e.getMessage());
            }
        }
    }

    /**
     * 测试协调状态对象
     */
    @Test
    public void testCoordinationStatus() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method method = coordinationService.getClass().getMethod("getCoordinationStatus");
                Object status = method.invoke(coordinationService);
                assertNotNull(status, "协调状态不应为空");

                java.lang.reflect.Method getInstanceId = status.getClass().getMethod("getInstanceId");
                assertNotNull(getInstanceId.invoke(status), "实例ID不应为空");
            } catch (Exception e) {
                fail("测试协调状态失败: " + e.getMessage());
            }
        }
    }
}
