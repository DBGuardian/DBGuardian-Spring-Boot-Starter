package com.test;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源上下文测试
 * 
 * 技术栈: [JAVA17] [SPRING_BOOT_32] [MYBATIS] [MYSQL]
 */
public class DataSourceContextHolderTest {

    /**
     * TC-001: 测试上下文初始化为空
     */
    @Test
    public void testInitialContextIsNull() {
        RoutingContextHolder.clear();
        assertNull(RoutingContextHolder.get());
    }

    /**
     * TC-002: 测试读写上下文切换
     */
    @Test
    public void testReadWriteSwitch() {
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
     * TC-003: 测试上下文属性继承
     */
    @Test
    public void testContextProperties() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        context.setForceMaster(true);
        context.setTransactional(true);
        context.setReadOnlyTransaction(true);

        RoutingContextHolder.set(context);
        try {
            RoutingContext stored = RoutingContextHolder.get();
            assertNotNull(stored);
            assertEquals("read", stored.getOperation());
            assertTrue(stored.isForceMaster());
            assertTrue(stored.isTransactional());
            assertTrue(stored.isReadOnlyTransaction());
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-004: 测试线程隔离
     */
    @Test
    public void testThreadIsolation() throws InterruptedException {
        RoutingContext mainContext = new RoutingContext();
        mainContext.setOperation("read");
        RoutingContextHolder.set(mainContext);

        Thread thread = new Thread(() -> {
            assertNull(RoutingContextHolder.get(), "子线程不应该继承父线程的上下文");
        });
        thread.start();
        thread.join();

        assertEquals("read", RoutingContextHolder.get().getOperation(), "主线程上下文应该保持");
        RoutingContextHolder.clear();
    }

    /**
     * TC-005: 测试多层上下文嵌套
     */
    @Test
    public void testNestedContext() {
        RoutingContext context1 = new RoutingContext();
        context1.setOperation("read");
        RoutingContextHolder.set(context1);
        assertEquals("read", RoutingContextHolder.get().getOperation());

        RoutingContext context2 = new RoutingContext();
        context2.setOperation("write");
        RoutingContextHolder.set(context2);
        assertEquals("write", RoutingContextHolder.get().getOperation());

        RoutingContextHolder.clear();
        assertNull(RoutingContextHolder.get());
    }
}
