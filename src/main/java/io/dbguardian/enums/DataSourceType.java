package io.dbguardian.enums;

/**
 * 数据源类型枚举
 */
public enum DataSourceType {
    /**
     * 主库 - 用于写操作
     */
    MASTER,

    /**
     * 从库 - 用于读操作
     */
    SLAVE
}
