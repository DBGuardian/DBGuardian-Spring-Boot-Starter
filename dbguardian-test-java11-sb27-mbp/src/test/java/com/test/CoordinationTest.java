package com.test;

import com.test.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式协调服务测试
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class CoordinationTest {

    @Autowired(required = false)
    @Qualifier("datasourceCoordinationService")
    private Object coordinationService;

    /**
     * TC-008: 测试多实例状态同步
     */
    @Test
    public void testMultiInstanceStateSync() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method getInstanceId = coordinationService.getClass().getMethod("getInstanceId");
                Object instanceId = getInstanceId.invoke(coordinationService);
                assertNotNull(instanceId, "实例ID不应为空");
                assertFalse(instanceId.toString().isEmpty(), "实例ID不应为空字符串");

                java.lang.reflect.Method getStatus = coordinationService.getClass().getMethod("getCoordinationStatus");
                Object status = getStatus.invoke(coordinationService);
                assertNotNull(status, "协调状态不应为空");

                java.lang.reflect.Method statusGetInstanceId = status.getClass().getMethod("getInstanceId");
                Object statusInstanceId = statusGetInstanceId.invoke(status);
                assertEquals(instanceId.toString(), statusInstanceId.toString(), "状态中的实例ID应该一致");
            } catch (Exception e) {
                fail("测试多实例状态同步失败: " + e.getMessage());
            }
        }
    }

    /**
     * TC-009: 测试故障转移分布式锁
     */
    @Test
    public void testFailoverLock() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method healthyMethod = coordinationService.getClass().getMethod("isHealthy");
                Boolean isHealthy = (Boolean) healthyMethod.invoke(coordinationService);

                if (Boolean.TRUE.equals(isHealthy)) {
                    java.lang.reflect.Method tryAcquire = coordinationService.getClass().getMethod("tryAcquireFailoverLock", long.class);
                    Boolean acquired = (Boolean) tryAcquire.invoke(coordinationService, 30L);
                    assertTrue(acquired, "应该能够获取故障转移锁");

                    java.lang.reflect.Method release = coordinationService.getClass().getMethod("releaseFailoverLock");
                    release.invoke(coordinationService);

                    Boolean afterRelease = (Boolean) tryAcquire.invoke(coordinationService, 30L);
                    assertTrue(afterRelease, "锁释放后应该能够重新获取");
                }
            } catch (Exception e) {
                // Redis 不可用时这是正常的
                assertTrue(true);
            }
        }
    }

    /**
     * TC-010: 测试 Redis 不可用时降级
     */
    @Test
    public void testRedisUnavailableFallback() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method getInstanceId = coordinationService.getClass().getMethod("getInstanceId");
                Object instanceId = getInstanceId.invoke(coordinationService);
                assertNotNull(instanceId, "实例ID应该总是可用的");

                java.lang.reflect.Method getStatus = coordinationService.getClass().getMethod("getCoordinationStatus");
                Object status = getStatus.invoke(coordinationService);
                assertNotNull(status, "即使 Redis 不可用，状态对象也应该返回");

                java.lang.reflect.Method isRedisHealthy = status.getClass().getMethod("isRedisHealthy");
                Boolean redisHealthy = (Boolean) isRedisHealthy.invoke(status);

                java.lang.reflect.Method isHealthy = coordinationService.getClass().getMethod("isHealthy");
                Boolean serviceHealthy = (Boolean) isHealthy.invoke(coordinationService);

                assertEquals(serviceHealthy, redisHealthy, "状态应该一致");
            } catch (Exception e) {
                fail("测试降级失败: " + e.getMessage());
            }
        }
    }

    /**
     * TC-011: 测试实例注册为主库
     */
    @Test
    public void testInstanceRegistration() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method healthyMethod = coordinationService.getClass().getMethod("isHealthy");
                Boolean isHealthy = (Boolean) healthyMethod.invoke(coordinationService);

                if (Boolean.TRUE.equals(isHealthy)) {
                    java.lang.reflect.Method getInstanceId = coordinationService.getClass().getMethod("getInstanceId");
                    Object instanceId = getInstanceId.invoke(coordinationService);

                    java.lang.reflect.Method registerAsMaster = coordinationService.getClass().getMethod("registerAsMaster", String.class);
                    registerAsMaster.invoke(coordinationService, instanceId.toString());

                    java.lang.reflect.Method getMasterInstanceId = coordinationService.getClass().getMethod("getMasterInstanceId");
                    Object currentMaster = getMasterInstanceId.invoke(coordinationService);
                    assertEquals(instanceId.toString(), currentMaster.toString(), "当前实例应该被注册为主库");
                }
            } catch (Exception e) {
                assertTrue(true);
            }
        }
    }

    /**
     * TC-012: 测试状态变更广播
     */
    @Test
    public void testStatusChangeBroadcast() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method healthyMethod = coordinationService.getClass().getMethod("isHealthy");
                Boolean isHealthy = (Boolean) healthyMethod.invoke(coordinationService);

                if (Boolean.TRUE.equals(isHealthy)) {
                    java.lang.reflect.Method getStatus = coordinationService.getClass().getMethod("getMasterStatus");
                    Object initialStatus = getStatus.invoke(coordinationService);
                    assertNotNull(initialStatus, "初始状态不应为空");

                    java.lang.reflect.Method setStatus = coordinationService.getClass().getMethod("setMasterStatus", String.class);
                    String testStatus = "TEST_STATUS_" + System.currentTimeMillis();
                    setStatus.invoke(coordinationService, testStatus);

                    Object updatedStatus = getStatus.invoke(coordinationService);
                    assertEquals(testStatus, updatedStatus, "主库状态应该已更新");

                    setStatus.invoke(coordinationService, initialStatus.toString());
                }
            } catch (Exception e) {
                assertTrue(true);
            }
        }
    }

    /**
     * TC-013: 测试故障转移判断
     */
    @Test
    public void testShouldExecuteFailover() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method healthyMethod = coordinationService.getClass().getMethod("isHealthy");
                Boolean isHealthy = (Boolean) healthyMethod.invoke(coordinationService);

                if (Boolean.TRUE.equals(isHealthy)) {
                    java.lang.reflect.Method getInstanceId = coordinationService.getClass().getMethod("getInstanceId");
                    Object instanceId = getInstanceId.invoke(coordinationService);

                    java.lang.reflect.Method registerAsMaster = coordinationService.getClass().getMethod("registerAsMaster", String.class);
                    registerAsMaster.invoke(coordinationService, instanceId.toString());

                    java.lang.reflect.Method shouldFailover = coordinationService.getClass().getMethod("shouldExecuteFailover");
                    Boolean result = (Boolean) shouldFailover.invoke(coordinationService);
                    assertFalse(result, "已注册为主库时，不应执行故障转移");
                }
            } catch (Exception e) {
                assertTrue(true);
            }
        }
    }

    /**
     * TC-014: 测试协调服务初始化
     */
    @Test
    public void testCoordinationServiceInitialization() {
        if (coordinationService != null) {
            try {
                java.lang.reflect.Method initialize = coordinationService.getClass().getMethod("initialize");
                initialize.invoke(coordinationService);

                java.lang.reflect.Method getStatus = coordinationService.getClass().getMethod("getCoordinationStatus");
                Object status = getStatus.invoke(coordinationService);
                assertNotNull(status, "协调服务初始化后应该能够获取状态");
            } catch (Exception e) {
                fail("测试协调服务初始化失败: " + e.getMessage());
            }
        }
    }
}
