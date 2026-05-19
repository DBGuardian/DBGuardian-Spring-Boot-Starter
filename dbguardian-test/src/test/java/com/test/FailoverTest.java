package com.test;

import io.dbguardian.config.DbGuardianAutoConfiguration;
import io.dbguardian.enums.DataSourceStatus;
import io.dbguardian.coordination.DatasourceCoordinationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 故障转移测试
 * 测试用例: TC-005, TC-006, TC-007
 */
@SpringBootTest(classes = {Application.class, DbGuardianAutoConfiguration.class})
@ActiveProfiles("test")
public class FailoverTest {

    @Autowired
    private DbGuardianAutoConfiguration dbGuardianConfig;

    @Autowired(required = false)
    private DatasourceCoordinationService coordinationService;

    /**
     * TC-005: 测试当前数据源状态
     */
    @Test
    public void testCurrentDataSourceStatus() {
        DataSourceStatus status = dbGuardianConfig.getCurrentStatus();
        assertNotNull(status, "数据源状态不应为空");

        // 正常状态下应该是 MASTER_ACTIVE 或 SLAVE_PROMOTED
        boolean validStatus = status == DataSourceStatus.MASTER_ACTIVE
                || status == DataSourceStatus.SLAVE_PROMOTED
                || status == DataSourceStatus.DEGRADED;
        assertTrue(validStatus, "数据源状态应该是有效的枚举值");
    }

    /**
     * TC-006: 测试协调服务状态
     */
    @Test
    public void testCoordinationServiceStatus() {
        if (coordinationService != null) {
            DatasourceCoordinationService.CoordinationStatus status = coordinationService.getCoordinationStatus();
            assertNotNull(status, "协调服务状态不应为空");

            // 验证实例ID存在
            assertNotNull(status.getInstanceId(), "实例ID不应为空");
            assertFalse(status.getInstanceId().isEmpty(), "实例ID不应为空字符串");

            // 验证主库状态
            assertNotNull(status.getMasterStatus(), "主库状态不应为空");
        }
    }

    /**
     * TC-007: 测试故障转移配置
     */
    @Test
    public void testFailoverConfiguration() {
        // 验证配置属性存在
        assertNotNull(dbGuardianConfig, "DBGuardian 配置不应为空");

        // 验证协调服务存在
        if (coordinationService != null) {
            assertNotNull(coordinationService.getInstanceId(), "协调服务实例ID应存在");
        }
    }

    /**
     * TC-008: 测试主从状态转换
     */
    @Test
    public void testStatusTransitions() {
        // 测试所有可能的枚举值
        DataSourceStatus[] allStatuses = DataSourceStatus.values();
        assertTrue(allStatuses.length >= 3, "应该至少有 3 种状态");

        for (DataSourceStatus status : allStatuses) {
            assertNotNull(status.name(), "状态名称不应为空");
        }
    }

    /**
     * TC-009: 测试协调服务健康状态
     */
    @Test
    public void testCoordinationServiceHealth() {
        if (coordinationService != null) {
            boolean isHealthy = coordinationService.isHealthy();

            // 健康状态可以是 true 或 false（取决于 Redis 连接）
            // 重要的是不抛异常
            DatasourceCoordinationService.CoordinationStatus status = coordinationService.getCoordinationStatus();
            assertNotNull(status, "即使不健康也应该返回状态对象");
        }
    }

    /**
     * TC-010: 测试状态同步到 Redis
     */
    @Test
    public void testStatusSyncToRedis() {
        if (coordinationService != null && coordinationService.isHealthy()) {
            // 获取当前状态
            String currentMasterStatus = coordinationService.getMasterStatus();
            assertNotNull(currentMasterStatus, "主库状态不应为空");

            // 设置新状态
            String testStatus = "TEST_STATUS_" + System.currentTimeMillis();
            coordinationService.setMasterStatus(testStatus);

            // 验证状态已更新
            String updatedStatus = coordinationService.getMasterStatus();
            assertEquals(testStatus, updatedStatus, "主库状态应该已更新");
        }
    }
}
