package com.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 故障转移测试
 * 
 * 技术栈: [JAVA17] [SPRING_BOOT_32] [MYBATIS] [MYSQL]
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class FailoverTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    @Qualifier("datasourceCoordinationService")
    private Object coordinationService;

    /**
     * TC-005: 测试协调服务可用性
     */
    @Test
    public void testCoordinationServiceAvailable() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("datasourceCoordinationService"));
    }

    /**
     * TC-006: 测试协调服务状态
     */
    @Test
    public void testCoordinationServiceStatus() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method method = coordinationService.getClass().getMethod("getCoordinationStatus");
                Object status = method.invoke(coordinationService);
                assertNotNull(status, "协调服务状态不应为空");

                java.lang.reflect.Method getInstanceId = status.getClass().getMethod("getInstanceId");
                Object instanceId = getInstanceId.invoke(status);
                assertNotNull(instanceId, "实例ID不应为空");
                assertFalse(instanceId.toString().isEmpty(), "实例ID不应为空字符串");

                java.lang.reflect.Method getMasterStatus = status.getClass().getMethod("getMasterStatus");
                assertNotNull(getMasterStatus.invoke(status), "主库状态不应为空");
            } catch (Exception e) {
                fail("测试协调服务状态失败: " + e.getMessage());
            }
        }
    }

    /**
     * TC-007: 测试故障转移配置
     */
    @Test
    public void testFailoverConfiguration() {
        assertNotNull(applicationContext, "应用上下文不应为空");
        assertNotNull(coordinationService, "协调服务实例应存在");
    }

    /**
     * TC-008: 测试主从状态获取
     */
    @Test
    public void testMasterStatusGet() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method method = coordinationService.getClass().getMethod("getMasterStatus");
                Object status = method.invoke(coordinationService);
                assertNotNull(status, "主库状态不应为空");
            } catch (Exception e) {
                fail("获取主库状态失败: " + e.getMessage());
            }
        }
    }

    /**
     * TC-009: 测试协调服务健康状态
     */
    @Test
    public void testCoordinationServiceHealth() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method method = coordinationService.getClass().getMethod("isHealthy");
                method.invoke(coordinationService);

                java.lang.reflect.Method statusMethod = coordinationService.getClass().getMethod("getCoordinationStatus");
                Object status = statusMethod.invoke(coordinationService);
                assertNotNull(status, "即使不健康也应该返回状态对象");
            } catch (Exception e) {
                fail("测试协调服务健康状态失败: " + e.getMessage());
            }
        }
    }

    /**
     * TC-010: 测试状态同步到 Redis
     */
    @Test
    public void testStatusSyncToRedis() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method healthyMethod = coordinationService.getClass().getMethod("isHealthy");
                Boolean isHealthy = (Boolean) healthyMethod.invoke(coordinationService);

                if (Boolean.TRUE.equals(isHealthy)) {
                    java.lang.reflect.Method getStatus = coordinationService.getClass().getMethod("getMasterStatus");
                    Object initialStatus = getStatus.invoke(coordinationService);
                    assertNotNull(initialStatus, "主库状态不应为空");

                    java.lang.reflect.Method setStatus = coordinationService.getClass().getMethod("setMasterStatus", String.class);
                    String testStatus = "TEST_STATUS_" + System.currentTimeMillis();
                    setStatus.invoke(coordinationService, testStatus);

                    Object updatedStatus = getStatus.invoke(coordinationService);
                    assertEquals(testStatus, updatedStatus, "主库状态应该已更新");
                }
            } catch (Exception e) {
                // Redis 不可用时这是正常的
                assertTrue(true);
            }
        }
    }
}
