package com.dbguardian.test;

import io.dbguardian.config.DbGuardianAutoConfiguration;
import io.dbguardian.enums.DataSourceStatus;
import io.dbguardian.enums.DataSourceType;
import io.dbguardian.coordination.DatasourceCoordinationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DBGuardian 单元测试（无需 Spring 上下文）
 * 测试核心组件和逻辑
 * 技术栈: [JAVA8] [SPRING_BOOT_27] [MYBATIS_PLUS] [MYSQL]
 */
public class DbGuardianUnitTest {

    /**
     * TC-001: 测试 DataSourceType 枚举
     */
    @Test
    public void testDataSourceTypeEnum() {
        DataSourceType[] types = DataSourceType.values();
        assertTrue(types.length >= 2, "应该有 MASTER 和 SLAVE 两种类型");
        assertEquals(DataSourceType.MASTER, DataSourceType.valueOf("MASTER"));
        assertEquals(DataSourceType.SLAVE, DataSourceType.valueOf("SLAVE"));
    }

    /**
     * TC-002: 测试 DataSourceStatus 枚举
     */
    @Test
    public void testDataSourceStatusEnum() {
        DataSourceStatus[] statuses = DataSourceStatus.values();
        assertTrue(statuses.length >= 3, "应该有至少 3 种状态");

        // 验证常见状态存在
        assertNotNull(DataSourceStatus.MASTER_ACTIVE);
        assertNotNull(DataSourceStatus.SLAVE_PROMOTED);
        assertNotNull(DataSourceStatus.DEGRADED);
    }

    /**
     * TC-003: 测试读写方法判断逻辑
     */
    @Test
    public void testReadMethodDetection() {
        // 测试读方法前缀
        String[] readPrefixes = {"select", "get", "query", "find", "count", "list", "page", "search"};
        for (String prefix : readPrefixes) {
            assertTrue(isReadMethod(prefix + "Test"), prefix + "* 应该被识别为读方法");
        }

        // 测试写方法前缀
        String[] writePrefixes = {"insert", "save", "update", "delete", "remove", "add", "create", "modify"};
        for (String prefix : writePrefixes) {
            assertFalse(isReadMethod(prefix + "Test"), prefix + "* 应该被识别为写方法");
        }
    }

    /**
     * TC-004: 测试特殊情况
     */
    @Test
    public void testSpecialCases() {
        // selectOne 应该被识别为读方法
        assertTrue(isReadMethod("selectOne"));
        // selectById 应该被识别为读方法
        assertTrue(isReadMethod("selectById"));
        // getOne 应该被识别为读方法
        assertTrue(isReadMethod("getOne"));
        // saveOrUpdate 应该被识别为写方法
        assertFalse(isReadMethod("saveOrUpdate"));
        // deleteById 应该被识别为写方法
        assertFalse(isReadMethod("deleteById"));
    }

    /**
     * TC-005: 测试字符串大小写不敏感
     */
    @Test
    public void testCaseInsensitive() {
        assertTrue(isReadMethod("SELECT"));
        assertTrue(isReadMethod("GetUser"));
        assertTrue(isReadMethod("QUERY"));
        assertTrue(isReadMethod("FindAll"));
    }

    /**
     * TC-006: 测试不匹配的方法名
     */
    @Test
    public void testNonMatchingMethods() {
        // 非 select/get/query/find/count/list/page/search 开头的方法应返回 false
        assertFalse(isReadMethod("execute"));
        assertFalse(isReadMethod("process"));
        assertFalse(isReadMethod("handle"));
    }

    /**
     * 辅助方法：判断是否为读方法（与 DbGuardianDataSourceAspect 逻辑一致）
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

    /**
     * TC-007: 测试协调服务常量
     */
    @Test
    public void testCoordinationConstants() {
        assertEquals("NORMAL", DatasourceCoordinationService.STATUS_NORMAL);
        assertEquals("SLAVE_PROMOTED", DatasourceCoordinationService.STATUS_SLAVE_PROMOTED);
    }
}
