package io.dbguardian.core.datasource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DataSourceWrapper {

    private final String id;
    private final String url;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final DataSource dataSource;
    private final int priority;
    private final int weight;
    private final boolean master;

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    private volatile boolean available = true;
    private volatile long lastHealthCheck = System.currentTimeMillis();
    private volatile long avgResponseTime;
    private volatile int maxFailuresBeforeMarkDown = 3;

    public DataSourceWrapper(String id,
                             String url,
                             String username,
                             String password,
                             String driverClassName,
                             int priority,
                             int weight,
                             boolean master,
                             DataSource dataSource) {
        this.id = id;
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.priority = priority;
        this.weight = weight;
        this.master = master;
        this.dataSource = dataSource;
    }

    public Connection getConnection() throws SQLException {
        activeConnections.incrementAndGet();
        totalConnections.incrementAndGet();
        try {
            return dataSource.getConnection();
        } finally {
            activeConnections.decrementAndGet();
        }
    }

    public boolean isHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException ex) {
            healthCheckFailed();
            return false;
        }
    }

    public void healthCheckSuccess() {
        consecutiveFailures.set(0);
        available = true;
        lastHealthCheck = System.currentTimeMillis();
    }

    public void healthCheckFailed() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= maxFailuresBeforeMarkDown) {
            available = false;
        }
        lastHealthCheck = System.currentTimeMillis();
    }

    public void markAvailable() {
        available = true;
        consecutiveFailures.set(0);
    }

    public void markUnavailable() {
        available = false;
    }

    public void incrementQueries() {
        totalQueries.incrementAndGet();
    }

    public void updateResponseTime(long responseTime) {
        if (avgResponseTime == 0) {
            avgResponseTime = responseTime;
        } else {
            avgResponseTime = (avgResponseTime * 9 + responseTime) / 10;
        }
    }

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
        return master;
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

    public void setMaxFailuresBeforeMarkDown(int maxFailuresBeforeMarkDown) {
        this.maxFailuresBeforeMarkDown = maxFailuresBeforeMarkDown;
    }
}