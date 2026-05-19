package com.test;

import io.dbguardian.config.DbGuardianAutoConfiguration;
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
        assertNull(DbGuardianAutoConfiguration.DataSourceContextHolder.get());

        // 切换到主库
        DbGuardianAutoConfiguration.DataSourceContextHolder.useMaster();
        assertEquals(io.dbguardian.enums.DataSourceType.MASTER,
                     DbGuardianAutoConfiguration.DataSourceContextHolder.get());

        // 切换到从库
        DbGuardianAutoConfiguration.DataSourceContextHolder.useSlave();
        assertEquals(io.dbguardian.enums.DataSourceType.SLAVE,
                     DbGuardianAutoConfiguration.DataSourceContextHolder.get());

        // 清除上下文
        DbGuardianAutoConfiguration.DataSourceContextHolder.clear();
        assertNull(DbGuardianAutoConfiguration.DataSourceContextHolder.get());
    }

    /**
     * TC-002: 测试线程隔离
     */
    @Test
    public void testThreadIsolation() throws InterruptedException {
        // 主线程设置为主库
        DbGuardianAutoConfiguration.DataSourceContextHolder.useMaster();

        // 新线程应该是独立的
        Thread newThread = new Thread(() -> {
            assertNull(DbGuardianAutoConfiguration.DataSourceContextHolder.get(),
                       "新线程应该有独立的上下文");
            DbGuardianAutoConfiguration.DataSourceContextHolder.useSlave();
            assertEquals(io.dbguardian.enums.DataSourceType.SLAVE,
                         DbGuardianAutoConfiguration.DataSourceContextHolder.get());
        });

        newThread.start();
        newThread.join();

        // 主线程状态不受影响
        assertEquals(io.dbguardian.enums.DataSourceType.MASTER,
                     DbGuardianAutoConfiguration.DataSourceContextHolder.get());

        // 清理
        DbGuardianAutoConfiguration.DataSourceContextHolder.clear();
    }

    /**
     * TC-003: 测试 set 方法
     */
    @Test
    public void testSetMethod() {
        DbGuardianAutoConfiguration.DataSourceContextHolder.set(io.dbguardian.enums.DataSourceType.MASTER);
        assertEquals(io.dbguardian.enums.DataSourceType.MASTER,
                     DbGuardianAutoConfiguration.DataSourceContextHolder.get());

        DbGuardianAutoConfiguration.DataSourceContextHolder.set(io.dbguardian.enums.DataSourceType.SLAVE);
        assertEquals(io.dbguardian.enums.DataSourceType.SLAVE,
                     DbGuardianAutoConfiguration.DataSourceContextHolder.get());

        // 清理
        DbGuardianAutoConfiguration.DataSourceContextHolder.clear();
    }
}
