package com.test;

import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DBGuardian 单元测试
 */
public class DbGuardianUnitTest {

    /**
     * TC-001: 测试 RoutingContext 创建
     */
    @Test
    public void testRoutingContextCreation() {
        RoutingContext context = new RoutingContext();
        assertNotNull(context);
    }

    /**
     * TC-002: 测试 RoutingContextHolder 设置
     */
    @Test
    public void testRoutingContextHolderSet() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        RoutingContextHolder.set(context);
        try {
            assertNotNull(RoutingContextHolder.get());
            assertEquals("read", RoutingContextHolder.get().getOperation());
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-003: 测试 RoutingContextHolder 清除
     */
    @Test
    public void testRoutingContextHolderClear() {
        RoutingContext context = new RoutingContext();
        context.setOperation("write");
        RoutingContextHolder.set(context);
        RoutingContextHolder.clear();
        assertNull(RoutingContextHolder.get());
    }

    /**
     * TC-004: 测试读操作标记
     */
    @Test
    public void testReadOperation() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        assertEquals("read", context.getOperation());
        assertFalse(context.isForceMaster());
    }

    /**
     * TC-005: 测试写操作标记
     */
    @Test
    public void testWriteOperation() {
        RoutingContext context = new RoutingContext();
        context.setOperation("write");
        assertEquals("write", context.getOperation());
    }

    /**
     * TC-006: 测试强制主库标记
     */
    @Test
    public void testForceMasterFlag() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        context.setForceMaster(true);
        assertTrue(context.isForceMaster());
    }

    /**
     * TC-007: 测试事务标记
     */
    @Test
    public void testTransactionalFlag() {
        RoutingContext context = new RoutingContext();
        context.setTransactional(true);
        assertTrue(context.isTransactional());
    }

    /**
     * TC-008: 测试只读事务标记
     */
    @Test
    public void testReadOnlyTransactionFlag() {
        RoutingContext context = new RoutingContext();
        context.setReadOnlyTransaction(true);
        assertTrue(context.isReadOnlyTransaction());
    }

    /**
     * TC-009: 测试标签设置
     */
    @Test
    public void testTags() {
        RoutingContext context = new RoutingContext();
        context.getTags().add("tag1");
        context.getTags().add("tag2");
        assertEquals(2, context.getTags().size());
        assertTrue(context.getTags().contains("tag1"));
        assertTrue(context.getTags().contains("tag2"));
    }

    /**
     * TC-010: 测试上下文覆盖
     */
    @Test
    public void testContextOverride() {
        RoutingContext context1 = new RoutingContext();
        context1.setOperation("read");
        RoutingContextHolder.set(context1);

        RoutingContext context2 = new RoutingContext();
        context2.setOperation("write");
        RoutingContextHolder.set(context2);

        assertEquals("write", RoutingContextHolder.get().getOperation());

        RoutingContextHolder.clear();
    }
}
