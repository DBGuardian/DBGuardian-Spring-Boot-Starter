package io.dbguardian.enums;

/**
 * 数据源状态枚举
 */
public enum DataSourceStatus {
    /**
     * 主库活跃 - 正常运行状态，读操作路由到从库，写操作路由到主库
     */
    MASTER_ACTIVE,

    /**
     * 从库已升为主库 - 故障转移状态，所有操作路由到新主库
     */
    SLAVE_PROMOTED,

    /**
     * 降级模式 - 主从库都不可用，仅保留连接能力
     */
    DEGRADED
}
