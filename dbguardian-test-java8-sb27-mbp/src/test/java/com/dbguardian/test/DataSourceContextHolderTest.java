package com.dbguardian.test;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataSourceContextHolder 单元测试
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
 */
public class DataSourceContextHolderTest {

    /**
     * TC-001: 测试上下文切换
     */
    @Test
    public void testContextSwitch() {
        assertNull(RoutingContextHolder.get());

        RoutingContext masterContext = new RoutingContext();
        masterContext.setOperation("write");
        RoutingContextHolder.set(masterContext);
        assertEquals("write", RoutingContextHolder.get().getOperation());

        RoutingContext slaveContext = new RoutingContext();
        slaveContext.setOperation("read");
        RoutingContextHolder.set(slaveContext);
        assertEquals("read", RoutingContextHolder.get().getOperation());

        RoutingContextHolder.clear();
        assertNull(RoutingContextHolder.get());
    }

    /**
     * TC-002: 测试线程隔离
     */
    @Test
    public void testThreadIsolation() throws InterruptedException {
        RoutingContext context = new RoutingContext();
        context.setOperation("write");
        RoutingContextHolder.set(context);

        Thread newThread = new Thread(() -> {
            assertNull(RoutingContextHolder.get(),
                       "新线程应该有独立的上下文");
            RoutingContext readContext = new RoutingContext();
            readContext.setOperation("read");
            RoutingContextHolder.set(readContext);
            assertEquals("read",
                         RoutingContextHolder.get().getOperation());
            RoutingContextHolder.clear();
        });

        newThread.start();
        newThread.join();

        assertEquals("write",
                     RoutingContextHolder.get().getOperation());

        // 清理
        RoutingContextHolder.clear();
    }

    /**
     * TC-003: 测试 set 方法
     */
    @Test
    public void testSetMethod() {
        RoutingContext masterContext = new RoutingContext();
        masterContext.setOperation("write");
        RoutingContextHolder.set(masterContext);
        assertEquals("write",
                     RoutingContextHolder.get().getOperation());

        RoutingContext slaveContext = new RoutingContext();
        slaveContext.setOperation("read");
        RoutingContextHolder.set(slaveContext);
        assertEquals("read",
                     RoutingContextHolder.get().getOperation());

        // 清理
        RoutingContextHolder.clear();
    }
}
