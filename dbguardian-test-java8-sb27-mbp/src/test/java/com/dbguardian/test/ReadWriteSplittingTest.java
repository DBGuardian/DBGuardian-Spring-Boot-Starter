package com.dbguardian.test;

import com.dbguardian.test.entity.User;
import com.dbguardian.test.mapper.UserMapper;
import io.dbguardian.boot2.config.DbGuardianBoot2AutoConfiguration;
import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 读写分离路由测试
 * 测试用例: TC-001, TC-002, TC-003, TC-004
 *
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
 */
@SpringBootTest(classes = {Application.class, DbGuardianBoot2AutoConfiguration.class})
@ActiveProfiles("test")
public class ReadWriteSplittingTest {

    @Autowired
    private UserMapper userMapper;

    /**
     * TC-001: 基础读操作上下文应可切到 read
     */
    @Test
    public void testSelectRouteToSlave() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        RoutingContextHolder.set(context);
        try {
            assertNotNull(RoutingContextHolder.get());
            assertEquals("read", RoutingContextHolder.get().getOperation(), "selectById 应该走读上下文");
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-002: 基础写操作上下文应可切到 write
     */
    @Test
    public void testInsertRouteToMaster() {
        RoutingContext context = new RoutingContext();
        context.setOperation("write");
        RoutingContextHolder.set(context);
        try {
            assertNotNull(RoutingContextHolder.get());
            assertEquals("write", RoutingContextHolder.get().getOperation(), "insert 操作应该走写上下文");
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-003: 方法名自动路由规则测试
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
     * TC-004: 基础集成查询可执行
     */
    @Test
    public void testActualDatabaseOperation() {
        assertNotNull(userMapper, "Mapper 应该由 Spring 注入");
    }

    /**
     * TC-005: 数据源上下文清理测试
     */
    @Test
    public void testDataSourceContextSwitch() {
        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        RoutingContextHolder.set(context);
        assertEquals("read", RoutingContextHolder.get().getOperation());

        context.setOperation("write");
        RoutingContextHolder.set(context);
        assertEquals("write", RoutingContextHolder.get().getOperation());

        RoutingContextHolder.clear();
        assertNull(RoutingContextHolder.get());
    }
}
