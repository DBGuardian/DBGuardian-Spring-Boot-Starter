package io.dbguardian.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DBGuardian 集群配置属性
 * 支持单主单从、单主多从、多主多从等多种集群拓扑
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource.dbguardian")
public class ClusterDataSourceProperties {

    /** 是否启用 DBGuardian */
    private boolean enabled = true;

    /** 集群模式 */
    private ClusterMode clusterMode = ClusterMode.SINGLE_MASTER;

    /** 主库配置（支持多个） */
    private List<MasterConfig> masters = new ArrayList<>();

    /** 从库配置（支持多个） */
    private List<SlaveConfig> slaves = new ArrayList<>();

    /** 负载均衡配置 */
    private LoadBalanceConfig loadBalance = new LoadBalanceConfig();

    /** 故障转移配置 */
    private FailoverConfig failover = new FailoverConfig();

    /** 集群模式枚举 */
    public enum ClusterMode {
        /** 单主单从 */
        SINGLE_MASTER,
        /** 单主多从 */
        SINGLE_MASTER_MULTI_SLAVE,
        /** 多主多从 */
        MULTI_MASTER_MULTI_SLAVE,
        /** 多主无从（分库写入） */
        MULTI_MASTER_NO_SLAVE,
        /** 双主双从 */
        DUAL_MASTER_DUAL_SLAVE
    }

    /**
     * 主库配置
     */
    @Data
    public static class MasterConfig {
        /** 主库唯一标识 */
        private String id;

        /** JDBC URL */
        private String url;

        /** 用户名 */
        private String username;

        /** 密码 */
        private String password;

        /** 驱动类名 */
        private String driverClassName = "com.mysql.cj.jdbc.Driver";

        /** 优先级（数值越高优先级越高） */
        private int priority = 100;

        /** 故障转移目标主库ID */
        private String peerId;

        /** HikariCP 配置 */
        private HikariConfig hikari = new HikariConfig();
    }

    /**
     * 从库配置
     */
    @Data
    public static class SlaveConfig {
        /** 从库唯一标识 */
        private String id;

        /** JDBC URL */
        private String url;

        /** 用户名 */
        private String username;

        /** 密码 */
        private String password;

        /** 驱动类名 */
        private String driverClassName = "com.mysql.cj.jdbc.Driver";

        /** 负载均衡权重（数值越高被选中概率越大） */
        private int weight = 100;

        /** 所属主库ID（用于多主多从场景） */
        private String masterRef;

        /** 是否启用 */
        private boolean enabled = true;

        /** HikariCP 配置 */
        private HikariConfig hikari = new HikariConfig();
    }

    /**
     * HikariCP 连接池配置
     */
    @Data
    public static class HikariConfig {
        /** 最大连接数 */
        private int maximumPoolSize = 20;

        /** 最小空闲连接数 */
        private int minimumIdle = 5;

        /** 连接超时（毫秒） */
        private long connectionTimeout = 30000;

        /** 空闲超时（毫秒） */
        private long idleTimeout = 600000;

        /** 最大生命周期（毫秒） */
        private long maxLifetime = 1800000;

        /** 连接池名称 */
        private String poolName;
    }

    /**
     * 负载均衡配置
     */
    @Data
    public static class LoadBalanceConfig {
        /** 读操作负载均衡策略 */
        private ReadStrategy readStrategy = ReadStrategy.WEIGHTED_ROUND_ROBIN;

        /** 写操作路由策略 */
        private WriteStrategy writeStrategy = WriteStrategy.HASH;

        /** 分片键列名 */
        private String shardingColumn;

        /** 最小权重阈值（低于此值不参与负载均衡） */
        private int minWeightThreshold = 10;
    }

    /**
     * 读操作负载均衡策略
     */
    public enum ReadStrategy {
        /** 轮询 */
        ROUND_ROBIN,
        /** 加权轮询 */
        WEIGHTED_ROUND_ROBIN,
        /** 随机 */
        RANDOM,
        /** 加权随机 */
        WEIGHTED_RANDOM,
        /** 最少连接 */
        LEAST_CONNECTIONS,
        /** 一致性哈希 */
        CONSISTENT_HASH
    }

    /**
     * 写操作路由策略
     */
    public enum WriteStrategy {
        /** 哈希分片 */
        HASH,
        /** 随机路由 */
        RANDOM,
        /** 始终路由到主库 */
        PRIMARY_ONLY,
        /** 广播到所有主库 */
        BROADCAST
    }

    /**
     * 故障转移配置
     */
    @Data
    public static class FailoverConfig {
        /** 是否启用故障转移 */
        private boolean enabled = true;

        /** 健康检查间隔（秒） */
        private int checkIntervalSeconds = 30;

        /** 从库健康检查间隔（秒） */
        private int slaveCheckIntervalSeconds = 5;

        /** 最大重试次数 */
        private int maxRetries = 3;

        /** 故障转移超时（秒） */
        private int failoverTimeoutSeconds = 60;

        /** 故障转移时是否降级原主库 */
        private boolean demoteOnFailover = true;

        /** 连续失败多少次后标记为不可用 */
        private int maxFailuresBeforeMarkDown = 3;
    }
}
