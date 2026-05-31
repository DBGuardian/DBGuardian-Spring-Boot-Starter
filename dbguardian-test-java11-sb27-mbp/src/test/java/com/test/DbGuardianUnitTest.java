package com.test;

import io.dbguardian.core.routing.DefaultRoutingPolicy;
import io.dbguardian.model.NodeModel;
import io.dbguardian.model.RoutingContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DBGuardian 单元测试（无需 Spring 上下文）
 * 测试核心组件和逻辑
 */
public class DbGuardianUnitTest {

    /**
     * TC-001: 测试默认路由策略读操作选择从库
     */
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

    /**
     * TC-002: 测试默认路由策略写操作选择主库
     */
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

    /**
     * TC-003: 测试强制主库优先级
     */
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

    /**
     * TC-004: 测试路由上下文默认值
     */
    @Test
    public void testRoutingContextDefaults() {
        RoutingContext context = new RoutingContext();
        assertFalse(context.isForceMaster());
        assertEquals("write", context.getOperation());
        assertEquals("unknown", context.getOrmType());
    }

    /**
     * TC-005: 测试读写方法判断逻辑
     */
    @Test
    public void testReadMethodDetection() {
        String[] readPrefixes = {"select", "get", "query", "find", "count", "list", "page", "search"};
        for (String prefix : readPrefixes) {
            assertTrue(isReadMethod(prefix + "Test"), prefix + "* 应该被识别为读方法");
        }

        String[] writePrefixes = {"insert", "save", "update", "delete", "remove", "add", "create", "modify"};
        for (String prefix : writePrefixes) {
            assertFalse(isReadMethod(prefix + "Test"), prefix + "* 应该被识别为写方法");
        }
    }

    /**
     * TC-006: 测试特殊情况
     */
    @Test
    public void testSpecialCases() {
        assertTrue(isReadMethod("selectOne"));
        assertTrue(isReadMethod("selectById"));
        assertTrue(isReadMethod("getOne"));
        assertFalse(isReadMethod("saveOrUpdate"));
        assertFalse(isReadMethod("deleteById"));
    }

    /**
     * TC-007: 测试字符串大小写不敏感
     */
    @Test
    public void testCaseInsensitive() {
        assertTrue(isReadMethod("SELECT"));
        assertTrue(isReadMethod("GetUser"));
        assertTrue(isReadMethod("QUERY"));
        assertTrue(isReadMethod("FindAll"));
    }

    /**
     * TC-008: 测试不匹配的方法名
     */
    @Test
    public void testNonMatchingMethods() {
        assertFalse(isReadMethod("execute"));
        assertFalse(isReadMethod("process"));
        assertFalse(isReadMethod("handle"));
    }

    /**
     * 辅助方法：判断是否为读方法
     */
    private boolean isReadMethod(String methodName) {
        String[] readPrefixes = {"select", "get", "query", "find", "count", "list", "page", "search"};
        String lowerName = methodName.toLowerCase();
        for (String prefix : readPrefixes) {
            if (lowerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private List<NodeModel> buildNodes() {
        List<NodeModel> nodes = new ArrayList<>();

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
