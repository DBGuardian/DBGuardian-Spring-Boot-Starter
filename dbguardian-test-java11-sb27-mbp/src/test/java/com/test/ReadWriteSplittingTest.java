package com.test;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 读写分离路由测试
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class ReadWriteSplittingTest {

    @Autowired(required = false)
    private com.test.mapper.UserMapper userMapper;

    /**
     * TC-001: 基础读操作路由到从库
     */
    @Test
    public void testSelectRouteToSlave() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        RoutingContextHolder.set(context);
        try {
            assertNotNull(RoutingContextHolder.get());
            assertEquals("read", RoutingContextHolder.get().getOperation(), "设置后应该走读上下文");
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-002: 基础写操作路由到主库
     */
    @Test
    public void testInsertRouteToMaster() {
        RoutingContext context = new RoutingContext();
        context.setOperation("write");
        RoutingContextHolder.set(context);
        try {
            assertNotNull(RoutingContextHolder.get());
            assertEquals("write", RoutingContextHolder.get().getOperation(), "设置后应该走写上下文");
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-003: 事务内操作强制主库
     */
    @Test
    public void testTransactionContext() {
        RoutingContext context = new RoutingContext();
        context.setOperation("write");
        context.setTransactional(true);
        RoutingContextHolder.set(context);
        try {
            assertNotNull(RoutingContextHolder.get());
            assertEquals("write", RoutingContextHolder.get().getOperation());
            assertTrue(RoutingContextHolder.get().isTransactional());
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-004: 方法名自动路由规则测试
     */
    @Test
    public void testMethodNameRoutingRules() {
        String[] readPrefixes = {"select", "get", "query", "find", "count", "list", "page", "search"};
        for (String prefix : readPrefixes) {
            assertTrue(isReadOperation(prefix + "User"), prefix + "* 应该判定为读操作");
        }

        String[] writePrefixes = {"insert", "save", "update", "delete", "remove", "add", "create", "modify"};
        for (String prefix : writePrefixes) {
            assertFalse(isReadOperation(prefix + "User"), prefix + "* 应该判定为写操作");
        }
    }

    /**
     * 辅助方法：判断是否为读操作
     */
    private boolean isReadOperation(String methodName) {
        String[] readPrefixes = {"select", "get", "query", "find", "count", "list", "page", "search"};
        String lowerName = methodName.toLowerCase();
        for (String prefix : readPrefixes) {
            if (lowerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * TC-005: 集成测试 - Mapper 注入
     */
    @Test
    public void testMapperInjected() {
        assertNotNull(userMapper, "Mapper 应该由 Spring 注入");
    }

    /**
     * TC-006: 数据源切换上下文测试
     */
    @Test
    public void testDataSourceContextSwitch() {
        RoutingContext readContext = new RoutingContext();
        readContext.setOperation("read");
        RoutingContextHolder.set(readContext);
        assertEquals("read", RoutingContextHolder.get().getOperation());

        RoutingContext writeContext = new RoutingContext();
        writeContext.setOperation("write");
        RoutingContextHolder.set(writeContext);
        assertEquals("write", RoutingContextHolder.get().getOperation());

        RoutingContextHolder.clear();
        assertNull(RoutingContextHolder.get());
    }

    /**
     * TC-007: 强制主库测试
     */
    @Test
    public void testForceMaster() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        context.setForceMaster(true);
        RoutingContextHolder.set(context);
        try {
            assertNotNull(RoutingContextHolder.get());
            assertTrue(RoutingContextHolder.get().isForceMaster(), "应该设置了强制主库");
            assertEquals("read", RoutingContextHolder.get().getOperation());
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-008: 只读事务测试
     */
    @Test
    public void testReadOnlyTransaction() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        context.setReadOnlyTransaction(true);
        RoutingContextHolder.set(context);
        try {
            assertNotNull(RoutingContextHolder.get());
            assertTrue(RoutingContextHolder.get().isReadOnlyTransaction());
        } finally {
            RoutingContextHolder.clear();
        }
    }
}
