package com.dbguardian.test;

import io.dbguardian.core.RoutingEngine;
import io.dbguardian.core.TopologyRegistry;
import io.dbguardian.model.NodeModel;
import io.dbguardian.model.RoutingContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class ReadWriteSplittingTest {

    @Autowired
    private RoutingEngine routingEngine;

    @Autowired
    private TopologyRegistry topologyRegistry;

    @Test
    public void testReadRouteToSlave() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");

        NodeModel selected = routingEngine.route(topologyRegistry.getNodes(), context);
        assertNotNull(selected);
        assertEquals("slave", selected.getRole().toLowerCase(), "读流量应优先命中从节点");
    }

    @Test
    public void testWriteRouteToMaster() {
        RoutingContext context = new RoutingContext();
        context.setOperation("write");

        NodeModel selected = routingEngine.route(topologyRegistry.getNodes(), context);
        assertNotNull(selected);
        assertEquals("master", selected.getRole().toLowerCase(), "写流量应命中主节点");
    }

    @Test
    public void testTransactionalRouteToMaster() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        context.setTransactional(true);
        context.setReadOnlyTransaction(false);

        NodeModel selected = routingEngine.route(topologyRegistry.getNodes(), context);
        assertNotNull(selected);
        assertEquals("master", selected.getRole().toLowerCase(), "事务内非只读流量应命中主节点");
    }

    @Test
    public void testForceMasterRoute() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        context.setForceMaster(true);

        NodeModel selected = routingEngine.route(topologyRegistry.getNodes(), context);
        assertNotNull(selected);
        assertEquals("master", selected.getRole().toLowerCase(), "强制主库标记应命中主节点");
    }

    @Test
    public void testTopologyCandidatePool() {
        List<NodeModel> nodes = topologyRegistry.getNodes();
        assertEquals(2, nodes.size());
        assertTrue(nodes.stream().anyMatch(node -> "master".equalsIgnoreCase(node.getRole())));
        assertTrue(nodes.stream().anyMatch(node -> "slave".equalsIgnoreCase(node.getRole())));
    }
}