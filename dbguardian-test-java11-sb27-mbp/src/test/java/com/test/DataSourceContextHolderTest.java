package com.test;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataSourceContextHolder 单元测试
 */
public class DataSourceContextHolderTest {

    /**
     * TC-001: 测试上下文切换
     */
    @Test
    public void testContextSwitch() {
        // 测试初始状态为 null
        assertNull(RoutingContextHolder.get());

        // 切换到读上下文
        RoutingContext readContext = new RoutingContext();
        readContext.setOperation("read");
        RoutingContextHolder.set(readContext);
        assertNotNull(RoutingContextHolder.get());
        assertEquals("read", RoutingContextHolder.get().getOperation());

        // 切换到写上下文
        RoutingContext writeContext = new RoutingContext();
        writeContext.setOperation("write");
        RoutingContextHolder.set(writeContext);
        assertNotNull(RoutingContextHolder.get());
        assertEquals("write", RoutingContextHolder.get().getOperation());

        // 清除上下文
        RoutingContextHolder.clear();
        assertNull(RoutingContextHolder.get());
    }

    /**
     * TC-002: 测试线程隔离
     */
    @Test
    public void testThreadIsolation() throws InterruptedException {
        // 主线程设置为读上下文
        RoutingContext readContext = new RoutingContext();
        readContext.setOperation("read");
        RoutingContextHolder.set(readContext);

        // 新线程应该是独立的
        Thread newThread = new Thread(() -> {
            assertNull(RoutingContextHolder.get(), "新线程应该有独立的上下文");
            RoutingContext newContext = new RoutingContext();
            newContext.setOperation("read");
            RoutingContextHolder.set(newContext);
            assertNotNull(RoutingContextHolder.get());
            assertEquals("read", RoutingContextHolder.get().getOperation());
            RoutingContextHolder.clear();
        });

        newThread.start();
        newThread.join();

        // 主线程状态不受影响
        assertEquals("read", RoutingContextHolder.get().getOperation());

        // 清理
        RoutingContextHolder.clear();
    }

    /**
     * TC-003: 测试 forceMaster 设置
     */
    @Test
    public void testForceMaster() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        context.setForceMaster(true);
        RoutingContextHolder.set(context);
        assertNotNull(RoutingContextHolder.get());
        assertTrue(RoutingContextHolder.get().isForceMaster());
        assertEquals("read", RoutingContextHolder.get().getOperation());

        // 清理
        RoutingContextHolder.clear();
    }

    /**
     * TC-004: 测试读写操作区分
     */
    @Test
    public void testReadWriteOperations() {
        // 测试读操作
        RoutingContext readContext = new RoutingContext();
        readContext.setOperation("read");
        RoutingContextHolder.set(readContext);
        assertEquals("read", RoutingContextHolder.get().getOperation());
        assertFalse(RoutingContextHolder.get().isForceMaster());

        // 测试写操作
        RoutingContext writeContext = new RoutingContext();
        writeContext.setOperation("write");
        RoutingContextHolder.set(writeContext);
        assertEquals("write", RoutingContextHolder.get().getOperation());

        // 清理
        RoutingContextHolder.clear();
    }
}
