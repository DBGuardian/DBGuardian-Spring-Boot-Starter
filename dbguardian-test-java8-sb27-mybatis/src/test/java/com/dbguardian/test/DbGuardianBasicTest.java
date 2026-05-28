package com.dbguardian.test;

import io.dbguardian.core.CapabilityRegistry;
import io.dbguardian.core.FailoverOrchestrator;
import io.dbguardian.core.RoutingEngine;
import io.dbguardian.core.TopologyRegistry;
import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.model.NodeModel;
import io.dbguardian.spring.failover.DataSourceHealthChecker;
import io.dbguardian.spring.failover.FailoverController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
    public void testTopologyLoadedFromProperties() {
        List<NodeModel> nodes = topologyRegistry.getNodes();
        assertEquals(2, nodes.size(), "测试拓扑应包含 2 个节点");
        assertEquals("master", nodes.get(0).getRole().toLowerCase());
        assertEquals("slave", nodes.get(1).getRole().toLowerCase());
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