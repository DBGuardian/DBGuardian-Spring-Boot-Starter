package com.dbguardian.test;

import io.dbguardian.core.registry.DataSourceRegistry;
import io.dbguardian.spring.failover.DataSourceHealthChecker;
import io.dbguardian.spring.failover.FailoverController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class FailoverTest {

    @Autowired
    private DataSourceRegistry dataSourceRegistry;

    @Autowired
    private FailoverController failoverController;

    @Autowired
    private DataSourceHealthChecker dataSourceHealthChecker;

    @Test
    public void testFailoverBeansLoaded() {
        assertNotNull(dataSourceRegistry);
        assertNotNull(failoverController);
        assertNotNull(dataSourceHealthChecker);
    }

    @Test
    public void testRegistryInitialStats() {
        Map<String, Object> stats = dataSourceRegistry.getStats();
        assertNotNull(stats);
        assertTrue(stats.containsKey("masters"));
        assertTrue(stats.containsKey("slaves"));
    }

    @Test
    public void testHealthCheckerStatusShape() {
        Map<String, Object> status = dataSourceHealthChecker.getHealthStatus();
        assertNotNull(status);
        assertTrue(status.containsKey("running"));
        assertTrue(status.containsKey("masters"));
        assertTrue(status.containsKey("slaves"));
    }
}