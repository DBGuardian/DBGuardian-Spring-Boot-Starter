package com.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 协调服务测试
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class CoordinationTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * TC-001: 测试协调服务 Bean 存在
     */
    @Test
    public void testCoordinationServiceBeanExists() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("datasourceCoordinationService"));
    }

    /**
     * TC-002: 测试故障转移控制器 Bean 存在
     */
    @Test
    public void testFailoverControllerBeanExists() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("failoverController"));
    }

    /**
     * TC-003: 测试数据源健康检查器 Bean 存在
     */
    @Test
    public void testDataSourceHealthCheckerBeanExists() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("dataSourceHealthChecker"));
    }

    /**
     * TC-004: 测试路由引擎 Bean 存在
     */
    @Test
    public void testRoutingEngineBeanExists() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("routingEngine"));
    }

    /**
     * TC-005: 测试拓扑注册器 Bean 存在
     */
    @Test
    public void testTopologyRegistryBeanExists() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("topologyRegistry"));
    }

    /**
     * TC-006: 测试能力注册器 Bean 存在
     */
    @Test
    public void testCapabilityRegistryBeanExists() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("capabilityRegistry"));
    }

    /**
     * TC-007: 测试数据源注册器 Bean 存在
     */
    @Test
    public void testDataSourceRegistryBeanExists() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("dataSourceRegistry"));
    }

    /**
     * TC-008: 测试故障转移编排器 Bean 存在
     */
    @Test
    public void testFailoverOrchestratorBeanExists() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("failoverOrchestrator"));
    }
}
