package com.dbguardian.test;

import io.dbguardian.core.CapabilityRegistry;
import io.dbguardian.core.FailoverOrchestrator;
import io.dbguardian.core.RoutingEngine;
import io.dbguardian.core.TopologyRegistry;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.spring.failover.DataSourceHealthChecker;
import io.dbguardian.spring.failover.FailoverController;
import lombok.var;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class DbGuardianBasicTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CapabilityRegistry capabilityRegistry;

    @Autowired
    private TopologyRegistry topologyRegistry;

    @Autowired
    private RoutingEngine routingEngine;

    @Autowired
    private FailoverOrchestrator failoverOrchestrator;

    @Autowired
    private DataSourceRegistry dataSourceRegistry;

    @Autowired
    private FailoverController failoverController;

    @Autowired
    private DataSourceHealthChecker dataSourceHealthChecker;

    @Autowired(required = false)
    @Qualifier("datasourceCoordinationService")
    private Object coordinationService;

    @Test
    public void testApplicationContextLoaded() {
        assertNotNull(capabilityRegistry);
        assertNotNull(topologyRegistry);
        assertNotNull(routingEngine);
        assertNotNull(failoverOrchestrator);
        assertNotNull(dataSourceRegistry);
        assertNotNull(failoverController);
        assertNotNull(dataSourceHealthChecker);
    }

    @Test
    public void testDataSourceRegistryLoaded() {
        // 验证数据源注册中心已正确加载主从数据源
        assertNotNull(dataSourceRegistry, "数据源注册中心不应为空");

        // 验证主库已注册
        var master = dataSourceRegistry.getMaster("master");
        assertNotNull(master, "主库应已注册");

        // 验证从库已注册
        var slave = dataSourceRegistry.getSlave("slave");
        assertNotNull(slave, "从库应已注册");
    }

    @Test
    public void testDefaultRoutingPolicyRegistered() {
        assertFalse(capabilityRegistry.getRoutingPolicies().isEmpty(), "应至少注册一个默认路由策略");
    }

    @Test
    public void testCoordinationServiceAvailable() {
        assertNotNull(coordinationService, "协调服务应被装配");
        assertTrue(applicationContext.containsBean("datasourceCoordinationService"));
    }
}