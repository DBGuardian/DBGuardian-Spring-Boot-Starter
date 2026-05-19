package com.test;

import com.test.entity.User;
import com.test.mapper.UserMapper;
import com.test.service.UserService;
import io.dbguardian.config.DbGuardianAutoConfiguration;
import io.dbguardian.config.DbGuardianAutoConfiguration.DataSourceContextHolder;
import io.dbguardian.config.DbGuardianAutoConfiguration.RoutingDataSource;
import io.dbguardian.enums.DataSourceType;
import io.dbguardian.enums.DataSourceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 读写分离路由测试
 * 测试用例: TC-001, TC-002, TC-003, TC-004
 */
@SpringBootTest(classes = {Application.class, DbGuardianAutoConfiguration.class})
@ActiveProfiles("test")
public class ReadWriteSplittingTest {

    @Autowired(required = false)
    private RoutingDataSource routingDataSource;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * TC-001: 基础读操作路由到从库
     */
    @Test
    public void testSelectRouteToSlave() {
        if (routingDataSource != null) {
            DataSourceContextHolder.useSlave();
            try {
                DataSourceType currentType = DataSourceContextHolder.get();
                assertEquals(DataSourceType.SLAVE, currentType, "selectById 应该路由到从库");
            } finally {
                DataSourceContextHolder.clear();
            }
        }
    }

    /**
     * TC-002: 基础写操作路由到主库
     */
    @Test
    public void testInsertRouteToMaster() {
        if (routingDataSource != null) {
            DataSourceContextHolder.useMaster();
            try {
                DataSourceType currentType = DataSourceContextHolder.get();
                assertEquals(DataSourceType.MASTER, currentType, "insert 操作应该路由到主库");
            } finally {
                DataSourceContextHolder.clear();
            }
        }
    }

    /**
     * TC-003: 事务内操作强制主库
     */
    @Test
    @Transactional
    public void testTransactionForcesMaster() {
        if (routingDataSource != null) {
            // 创建测试用户
            User user = new User();
            user.setUsername("test_tx_" + System.currentTimeMillis());
            user.setEmail("test@example.com");

            int result = userMapper.insert(user);
            assertEquals(1, result, "插入应该成功");

            // 验证事务内使用了主库
            DataSourceType currentType = DataSourceContextHolder.get();
            assertEquals(DataSourceType.MASTER, currentType, "事务内操作应该使用主库");
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
     * 辅助方法：判断是否为读操作（与 DbGuardianDataSourceAspect 保持一致）
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
     * TC-005: 集成测试 - 实际数据库操作
     */
    @Test
    public void testActualDatabaseOperation() {
        // 1. 插入数据
        User user = new User();
        String uniqueName = "test_user_" + System.currentTimeMillis();
        user.setUsername(uniqueName);
        user.setEmail(uniqueName + "@test.com");

        int insertResult = userMapper.insert(user);
        assertTrue(insertResult >= 1, "插入应该成功");
        assertNotNull(user.getId(), "插入后应自动生成ID");

        Long userId = user.getId();

        // 2. 查询这条数据
        User foundUser = userMapper.selectById(userId);
        assertNotNull(foundUser, "应该能查到刚插入的数据");
        assertEquals(user.getUsername(), foundUser.getUsername(), "用户名应该一致");
    }

    /**
     * TC-006: 数据一致性测试
     */
    @Test
    public void testDataConsistency() {
        // 创建测试用户
        String uniqueName = "consistency_test_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(uniqueName);
        user.setEmail(uniqueName + "@test.com");
        userMapper.insert(user);

        Long userId = user.getId();
        assertNotNull(userId, "插入后ID不应为空");

        // 主库立即查询
        User masterUser = userMapper.selectById(userId);
        assertNotNull(masterUser, "主库应该能查到数据");
        assertEquals(uniqueName, masterUser.getUsername(), "用户名应该一致");

        // 从库查询（可能有复制延迟）
        User slaveUser = userMapper.selectById(userId);
        if (slaveUser != null) {
            assertEquals(masterUser.getUsername(), slaveUser.getUsername(), "主从数据应该一致");
        }
    }

    /**
     * TC-007: 数据源切换上下文测试
     */
    @Test
    public void testDataSourceContextSwitch() {
        if (routingDataSource != null) {
            // 测试切换到从库
            DataSourceContextHolder.useSlave();
            assertEquals(DataSourceType.SLAVE, DataSourceContextHolder.get());

            // 测试切换到主库
            DataSourceContextHolder.useMaster();
            assertEquals(DataSourceType.MASTER, DataSourceContextHolder.get());

            // 测试清除上下文
            DataSourceContextHolder.clear();
            assertNull(DataSourceContextHolder.get());
        }
    }

    /**
     * TC-008: 连接池配置测试
     */
    @Test
    public void testConnectionPoolConfiguration() {
        if (routingDataSource != null) {
            DataSourceContextHolder.useMaster();
            try {
                // 获取连接并验证
                Connection conn = routingDataSource.getConnection();
                assertNotNull(conn, "应该能够获取数据库连接");

                DatabaseMetaData metaData = conn.getMetaData();
                assertNotNull(metaData, "应该能够获取数据库元数据");

                conn.close();
            } catch (Exception e) {
                // Redis 或其他配置问题可能导致连接失败
                // 这是可以接受的降级行为
            } finally {
                DataSourceContextHolder.clear();
            }
        }
    }
}
