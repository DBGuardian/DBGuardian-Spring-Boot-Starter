package com.dbguardian.test;

import io.dbguardian.core.routing.DefaultRoutingPolicy;
import io.dbguardian.model.NodeModel;
import io.dbguardian.model.RoutingContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DbGuardianUnitTest {

    @Test
    public void testDefaultRoutingPolicyReadPrefersSlave() {
        DefaultRoutingPolicy policy = new DefaultRoutingPolicy();
        List<NodeModel> nodes = buildNodes();
        RoutingContext context = new RoutingContext();
        context.setOperation("read");

        NodeModel selected = policy.select(nodes, context);
        assertNotNull(selected);
        assertEquals("slave", selected.getRole().toLowerCase());
    }

    @Test
    public void testDefaultRoutingPolicyWritePrefersMaster() {
        DefaultRoutingPolicy policy = new DefaultRoutingPolicy();
        List<NodeModel> nodes = buildNodes();
        RoutingContext context = new RoutingContext();
        context.setOperation("write");

        NodeModel selected = policy.select(nodes, context);
        assertNotNull(selected);
        assertEquals("master", selected.getRole().toLowerCase());
    }

    @Test
    public void testForceMasterWins() {
        DefaultRoutingPolicy policy = new DefaultRoutingPolicy();
        List<NodeModel> nodes = buildNodes();
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        context.setForceMaster(true);

        NodeModel selected = policy.select(nodes, context);
        assertNotNull(selected);
        assertEquals("master", selected.getRole().toLowerCase());
    }

    @Test
    public void testRoutingContextDefaults() {
        RoutingContext context = new RoutingContext();
        assertFalse(context.isForceMaster());
        // operation 默认值为 "write"（安全默认值，走主库）
        assertEquals("write", context.getOperation());
        assertEquals("unknown", context.getOrmType());
    }

    private List<NodeModel> buildNodes() {
        List<NodeModel> nodes = new ArrayList<NodeModel>();

        NodeModel master = new NodeModel();
        master.setId("master-1");
        master.setRole("master");
        master.setWeight(100);
        master.setEnabled(true);
        nodes.add(master);

        NodeModel slave = new NodeModel();
        slave.setId("slave-1");
        slave.setRole("slave");
        slave.setWeight(100);
        slave.setEnabled(true);
        nodes.add(slave);

        return nodes;
    }
}