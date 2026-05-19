package io.dbguardian.loadbalance;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据源包装器
 * 封装单个数据源及其元数据，用于负载均衡决策
 */
public class DataSourceWrapper {

    private final String id;
    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final DataSource dataSource;
    private final int priority;
    private final int weight;
    private final boolean isMaster;

    // 运行时指标
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong totalQueries = new AtomicLong(0);
    private volatile boolean available = true;
    private volatile long lastHealthCheck = System.currentTimeMillis();
    private volatile long avgResponseTime = 0;

    // 健康检查相关
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private int maxFailuresBeforeMarkDown = 3;

    public DataSourceWrapper(String id, String url, String username, String password,
                            String driverClassName, int priority, int weight, boolean isMaster,
                            DataSource dataSource) {
        this.id = id;
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.priority = priority;
        this.weight = weight;
        this.isMaster = isMaster;
        this.dataSource = dataSource;
    }

    /**
     * 获取连接
     */
    public Connection getConnection() throws SQLException {
        activeConnections.incrementAndGet();
        totalConnections.incrementAndGet();
        try {
            return dataSource.getConnection();
        } finally {
            activeConnections.decrementAndGet();
        }
    }

    /**
     * 健康检测
     */
    public boolean isHealthy() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            consecutiveFailures.incrementAndGet();
            if (consecutiveFailures.get() >= maxFailuresBeforeMarkDown) {
                available = false;
            }
            return false;
        }
    }

    /**
     * 健康检测成功，重置失败计数
     */
    public void healthCheckSuccess() {
        consecutiveFailures.set(0);
        available = true;
        lastHealthCheck = System.currentTimeMillis();
    }

    /**
     * 健康检测失败
     */
    public void healthCheckFailed() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= maxFailuresBeforeMarkDown) {
            available = false;
        }
        lastHealthCheck = System.currentTimeMillis();
    }

    /**
     * 标记为可用
     */
    public void markAvailable() {
        this.available = true;
        this.consecutiveFailures.set(0);
    }

    /**
     * 标记为不可用
     */
    public void markUnavailable() {
        this.available = false;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public int getPriority() {
        return priority;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public long getTotalConnections() {
        return totalConnections.get();
    }

    public long getTotalQueries() {
        return totalQueries.get();
    }

    public boolean isAvailable() {
        return available;
    }

    public long getLastHealthCheck() {
        return lastHealthCheck;
    }

    public long getAvgResponseTime() {
        return avgResponseTime;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    public int getMaxFailuresBeforeMarkDown() {
        return maxFailuresBeforeMarkDown;
    }

    public void setMaxFailuresBeforeMarkDown(int maxFailures) {
        this.maxFailuresBeforeMarkDown = maxFailures;
    }

    /**
     * 增加查询计数
     */
    public void incrementQueries() {
        totalQueries.incrementAndGet();
    }

    /**
     * 更新平均响应时间
     */
    public void updateResponseTime(long responseTime) {
        // 简单移动平均
        if (avgResponseTime == 0) {
            avgResponseTime = responseTime;
        } else {
            avgResponseTime = (avgResponseTime * 9 + responseTime) / 10;
        }
    }

    @Override
    public String toString() {
        return String.format("DataSourceWrapper[id=%s, url=%s, available=%s, activeConn=%d, weight=%d, priority=%d]",
                id, url, available, activeConnections.get(), weight, priority);
    }
}
