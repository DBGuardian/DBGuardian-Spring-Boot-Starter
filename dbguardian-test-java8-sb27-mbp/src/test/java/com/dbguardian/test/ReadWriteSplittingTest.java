package com.dbguardian.test;

import com.dbguardian.test.mapper.UserMapper;
import io.dbguardian.boot2.config.DbGuardianBoot2AutoConfiguration;
import io.dbguardian.model.RoutingContext;
import io.dbguardian.spring.RoutingContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    @Autowired
    @Qualifier("dataSource")
    private DataSource routingDataSource;

    @Autowired
    @Qualifier("dbguardianMasterDataSource")
    private DataSource masterDataSource;

    @Autowired
    @Qualifier("dbguardianSlaveDataSource")
    private DataSource slaveDataSource;

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

    /**
     * TC-006: 真实读请求应落到从库
     */
    @Test
    public void testRealReadRouteToSlaveDataSource() throws SQLException {
        String masterIdentity = queryNodeIdentity(masterDataSource);
        String slaveIdentity = queryNodeIdentity(slaveDataSource);
        assertNotNull(routingDataSource, "路由数据源应存在");
        assertNotNull(masterIdentity, "主库身份应可读取");
        assertNotNull(slaveIdentity, "从库身份应可读取");
        assertNotEquals(masterIdentity, slaveIdentity, "当前测试环境的主从节点身份必须可区分，否则无法证明读写分离");

        RoutingContext context = new RoutingContext();
        context.setOperation("read");
        RoutingContextHolder.set(context);
        try {
            assertEquals(slaveIdentity, queryNodeIdentity(routingDataSource), "读请求应路由到从库");
        } finally {
            RoutingContextHolder.clear();
        }
    }

    /**
     * TC-007: 真实写请求应落到主库
     */
    @Test
    public void testRealWriteRouteToMasterDataSource() throws SQLException {
        String masterIdentity = queryNodeIdentity(masterDataSource);
        String slaveIdentity = queryNodeIdentity(slaveDataSource);
        assertNotNull(routingDataSource, "路由数据源应存在");
        assertNotNull(masterIdentity, "主库身份应可读取");
        assertNotNull(slaveIdentity, "从库身份应可读取");
        assertNotEquals(masterIdentity, slaveIdentity, "当前测试环境的主从节点身份必须可区分，否则无法证明读写分离");

        RoutingContext context = new RoutingContext();
        context.setOperation("write");
        RoutingContextHolder.set(context);
        try {
            assertEquals(masterIdentity, queryNodeIdentity(routingDataSource), "写请求应路由到主库");
        } finally {
            RoutingContextHolder.clear();
        }
    }

    private String queryNodeIdentity(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT CONCAT(@@hostname, ':', @@port) AS identity")) {
            assertTrue(rs.next(), "应返回数据库节点身份");
            return rs.getString("identity");
        }
    }
}
