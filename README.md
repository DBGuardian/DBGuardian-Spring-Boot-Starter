<div align="center">
  <img src="https://github.com/DBGuardian/DBGuardian-doc/blob/main/logo.png?raw=true" alt="DBGuardian Logo" width="220" />

  # DBGuardian Spring Boot Starter

  ![v1.0.0](https://img.shields.io/badge/version-v1.0.0-green)
  ![GitHub stars](https://img.shields.io/github/stars/DBGuardian/DBGuardian-Spring-Boot-Starter?style=flat-square)
  ![GitHub forks](https://img.shields.io/github/forks/DBGuardian/DBGuardian-Spring-Boot-Starter?style=flat-square)
  ![GitHub issues](https://img.shields.io/github/issues/DBGuardian/DBGuardian-Spring-Boot-Starter?style=flat-square)
  ![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)
</div>

Spring Boot 数据库高可用 Starter，提供读写分离、自动故障转移与降级启动能力。

该仓库同时包含两个 Starter 模块：

- `dbguardian-boot2-starter`：适用于 Spring Boot 2.7.x，Redis 使用 `spring.redis` 配置前缀。
- `dbguardian-boot3-starter`：适用于 Spring Boot 3.x，Redis 使用 `spring.data.redis` 配置前缀。

两个模块共用核心故障转移、路由和 PostgreSQL 恢复能力。

## 功能特性

- **读写分离**: 自动将读操作路由到从库，写操作路由到主库
- **自动故障转移**: 主库故障时自动将从库升为主库
- **分布式协调**: 基于 Redis 实现多实例状态同步
- **健康检查**: 定时检测主从库健康状态
- **原主库恢复**: 支持原主库恢复后作为从库追赶数据
- **降级启动**: 允许数据库不可用时降级启动

## 快速开始

### 1. 选择并添加依赖

Spring Boot 2.7.x：

```xml
<dependency>
    <groupId>io.dbguardian</groupId>
    <artifactId>dbguardian-boot2-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Spring Boot 3.x：

```xml
<dependency>
    <groupId>io.dbguardian</groupId>
    <artifactId>dbguardian-boot3-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置文件

以下示例使用 Spring Boot 2 的 Redis 前缀 `spring.redis`。Spring Boot 3 请将同一段 Redis 配置放在 `spring.data.redis` 下，其余数据源配置保持一致。

```yaml
spring:
  application:
    name: your-app-name

  # 允许循环依赖（DBGuardian 与 MyBatis-Plus 的数据源配置循环）
  main:
    allow-circular-references: true

  # 主库数据源配置
  datasource:
    master:
      url: jdbc:mysql://master-host:3306/mydb?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: root
      password: secret
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000

    # 从库数据源配置
    slave:
      url: jdbc:mysql://slave-host:3306/mydb?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: root
      password: secret
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000

    # 主从复制配置
    replication:
      master-host: master-host
      master-port: 3306
      master-user: repl
      master-password: repl_secret
      auto-reconnect: true

    # 允许降级启动（数据库不可用时）
    allow-degraded-startup: true

  # Redis配置（用于分布式协调）
  redis:
    host: redis-host
    port: 6379
    password: redis_password
    database: 0
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 50
        max-wait: 3000ms
        max-idle: 20
        min-idle: 5
```

### 3. 自动使用

```java
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    // 读操作 - 自动路由到从库
    public User getUser(Long id) {
        return userMapper.selectById(id);  // select → 从库
    }

    // 写操作 - 自动路由到主库
    public void createUser(User user) {
        userMapper.insert(user);  // insert → 主库
    }
}
```

## 配置说明

### 完整配置

```yaml
spring:
  application:
    name: your-app-name

  # 允许循环依赖（DBGuardian 与 MyBatis-Plus 的数据源配置循环）
  main:
    allow-circular-references: true

  datasource:
    # 允许降级启动
    allow-degraded-startup: true

    # 主库配置
    master:
      url: jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000

    # 从库配置
    slave:
      url: jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000

    # 主从复制配置
    replication:
      master-host: localhost
      master-port: 3306
      master-user: repl
      master-password: repl_password
      auto-reconnect: true

    # 允许降级启动（数据库不可用时）
    allow-degraded-startup: true

  # Redis配置（分布式协调）
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
    timeout: 10000ms
```

## PostgreSQL 自动追赶、远程执行与读路由恢复

PostgreSQL 在从库提升为主库后，原主通常已处于不同的时间线，不能通过 JDBC 复制 SQL 直接重新挂接。DBGuardian 会在故障恢复阶段调用 `pg_rewind`，使待恢复节点追赶当前主库并作为 standby 启动。

恢复方向始终是：**待恢复节点跟随当前主库**。`pg_rewind` 脚本必须在待恢复 PostgreSQL 节点本机执行：应用与数据库同机时可本地执行；分离部署时，应通过 SSH 或其他远程代理将脚本调度到该节点。

读路由由异步健康检查控制：

1. 从库不可用或复制异常时，将其移出可读从库池，读请求改路由到主库。
2. `pg_rewind` 或复制重建成功不代表从库立即可读，读请求仍保持在主库。
3. 后续健康检查确认 `pg_stat_wal_receiver.status = streaming` 后，才将从库加回读池并恢复从库读路由。

`keep-promoted-primary=true`（默认）表示保留故障期间升主的节点；旧主追赶后成为它的从库。设置为 `false` 时，旧主追平后会执行回切：旧主升回主库，当前主再追赶并回挂到旧主。

### 本地执行示例

```yaml
spring:
  datasource:
    master:
      url: jdbc:postgresql://pg-a:5432/appdb
      username: app_user
      password: ${DBGUARDIAN_DB_PASSWORD}
      driver-class-name: org.postgresql.Driver
    slave:
      url: jdbc:postgresql://pg-b:5432/appdb
      username: app_user
      password: ${DBGUARDIAN_DB_PASSWORD}
      driver-class-name: org.postgresql.Driver
    replication:
      master-user: repl_user
      master-password: ${DBGUARDIAN_REPL_PASSWORD}
    recovery:
      enabled: true
      auto-failback: true
      postgresql:
        enabled: true
        keep-promoted-primary: true
        # 以下路径和本地连接均属于待恢复节点 pg-a。
        data-directory: /var/lib/postgresql/16/main
        config-file: /etc/postgresql/16/main/postgresql.conf
        script-path: /opt/dbguardian/scripts/pg-rewind-recover.sh
        pg-ctl-command: /usr/lib/postgresql/16/bin/pg_ctl
        psql-command: /usr/lib/postgresql/16/bin/psql
        pg-rewind-command: /usr/lib/postgresql/16/bin/pg_rewind
        local-host: 127.0.0.1
        local-port: 5432
        local-database: appdb
        primary-slot-name: appdb_slot
        extra-primary-conn-info: "target_session_attrs=read-write"
        startup-wait-seconds: 60
        command-timeout-seconds: 900
```

此模式要求应用进程能够访问 `pg-a` 的数据目录，并有权限以 PostgreSQL 系统用户执行脚本和相关工具。

### 远程执行与回切示例

```yaml
spring:
  datasource:
    replication:
      master-host: 192.168.3.150
      master-port: 5432
      master-user: repl
      master-password: ${DBGUARDIAN_REPL_PASSWORD}
      auto-reconnect: true
    recovery:
      enabled: true
      auto-failback: true
      postgresql:
        enabled: true
        keep-promoted-primary: false

        # 原主恢复为从库时，在原主节点 192.168.3.150 执行。
        data-directory: /var/lib/postgresql/16/main
        config-file: /etc/postgresql/16/main/postgresql.conf
        script-path: /opt/dbguardian/scripts/pg-rewind-recover.sh
        remote-execution-enabled: true
        remote-executor-command: >
          ssh ubuntu@192.168.3.150 "sudo -n -u postgres env {env_sh} bash {script_path_sh}"
        remote-executor-password: ${DBGUARDIAN_SSH_PASSWORD}
        pg-ctl-command: /usr/lib/postgresql/16/bin/pg_ctl
        psql-command: /usr/lib/postgresql/16/bin/psql
        pg-rewind-command: /usr/lib/postgresql/16/bin/pg_rewind
        local-host: 127.0.0.1
        local-port: 5432
        local-database: appdb
        primary-slot-name: appdb_slot
        extra-primary-conn-info: "target_session_attrs=read-write"
        startup-wait-seconds: 60
        command-timeout-seconds: 900

        # 回切时恢复从库，在恢复从库节点 192.168.3.151 执行。
        # failback-* 是历史命名，当前代表恢复从库一侧的执行配置。
        failback-data-directory: /var/lib/postgresql/16/main
        failback-config-file: /etc/postgresql/16/main/postgresql.conf
        failback-script-path: /opt/dbguardian/scripts/pg-rewind-recover.sh
        failback-remote-execution-enabled: true
        failback-remote-executor-command: >
          ssh ubuntu@192.168.3.151 "sudo -n -u postgres env {env_sh} bash {script_path_sh}"
        failback-remote-executor-password: ${DBGUARDIAN_SSH_PASSWORD}
        failback-pg-ctl-command: /usr/lib/postgresql/16/bin/pg_ctl
        failback-psql-command: /usr/lib/postgresql/16/bin/psql
        failback-pg-rewind-command: /usr/lib/postgresql/16/bin/pg_rewind
        failback-local-host: 127.0.0.1
        failback-local-port: 5432
        failback-local-database: appdb
        failback-primary-slot-name: appdb_slot
        failback-extra-primary-conn-info: "target_session_attrs=read-write"
        failback-startup-wait-seconds: 60
        failback-command-timeout-seconds: 900
```

远程命令模板中的 `{env_sh}` 和 `{script_path_sh}` 必须原样保留，由执行器在运行时替换。`remote-executor-password` 只用于 SSH 登录，不能作为 `sudo` 密码；生产环境应优先使用 SSH 密钥，并为专用 SSH 用户配置仅允许恢复脚本的 `sudo -n -u postgres` 权限。

从库恢复完成后，可在该节点确认状态：

```sql
SELECT pg_is_in_recovery();
SELECT status FROM pg_stat_wal_receiver;
```

只有 `pg_is_in_recovery()` 为 `true` 且 `status` 为 `streaming`，DBGuardian 的后续健康检查才会恢复从库读路由。

完整部署、权限和故障排查说明见 `DBGuardian-doc/doc/PostgreSQL自动追赶说明.md`。

## 工作原理

### 读写分离

```
┌─────────────────────────────────────────────────────┐
│                   应用层                             │
├─────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────┐   │
│  │          DbGuardianDataSourceAspect          │   │
│  │   方法名检测: select/get/query → 从库        │   │
│  │              其他 → 主库                      │   │
│  └─────────────────────────────────────────────┘   │
│                       │                            │
│                       ▼                            │
│  ┌─────────────────────────────────────────────┐   │
│  │        RoutingDataSource                     │   │
│  │   根据 ThreadLocal 上下文切换数据源           │   │
│  └─────────────────────────────────────────────┘   │
└───────────────────────┬────────────────────────────┘
                        │
          ┌─────────────┴─────────────┐
          ▼                           ▼
┌─────────────────┐         ┌─────────────────┐
│   Master Pool   │         │   Slave Pool    │
│   (HikariCP)    │         │   (HikariCP)    │
└────────┬────────┘         └────────┬────────┘
         │                           │
         ▼                           ▼
┌─────────────────┐         ┌─────────────────┐
│     Master DB   │◄─复制───│     Slave DB    │
│   (写操作)      │         │   (读操作)      │
└─────────────────┘         └─────────────────┘
```

### Spring JDBC 路由

`JdbcTemplate` 与 `NamedParameterJdbcTemplate` 会自动接入 DBGuardian 的读写路由：

- 查询类方法（如 `query`、`queryForObject`、`queryForList`）进入从库读路由。
- 更新类方法（如 `update`、`batchUpdate`、`execute`）进入主库写路由。
- 每次 JDBC 调用结束后都会清理线程上下文，避免跨请求污染。
- 该适配复用统一的 `DataSource` 和路由数据源，不绑定具体数据库实现。

使用 Spring Boot 默认注入的 `JdbcTemplate` 即可，不需要额外开启注解或替换模板对象。

### 故障转移

```
1. 健康检查发现主库不可用
          ↓
2. 尝试获取 Redis 分布式锁
          ↓
3. 将从库提升为主库
          ↓
4. 广播状态变更
          ↓
5. 其他实例收到通知，同步状态
          ↓
6. 原主库恢复后，作为从库追赶数据
```

## 依赖要求

- Spring Boot 2.7.x 使用 `dbguardian-boot2-starter`
- Spring Boot 3.x 使用 `dbguardian-boot3-starter`
- Java 8 / 11 / 17 / 21，按 Spring Boot 版本选择兼容组合
- MySQL 5.7+ / 8.0+ 或 PostgreSQL
- Redis 6.x+（可选，用于分布式协调）

## 测试

### 单元测试

```bash
mvn test
```

### 本地集成测试

1. 确保 MySQL 主从复制已配置
2. 配置 `src/test/resources/application.yml`：
```yaml
spring:
  datasource:
    master:
      url: jdbc:mysql://localhost:3306/test
      username: root
      password: your_password
    slave:
      url: jdbc:mysql://localhost:3306/test
      username: root
      password: your_password
    replication:
      master-user: repl
      master-password: repl_password
    allow-degraded-startup: true
  redis:
    host: localhost
    port: 6379
```

### 在已有项目中测试

1. 添加依赖
2. 配置主从数据源
3. 启动应用，观察日志：
```
=== DBGuardian 读写分离配置初始化 ===
=== 初始数据源状态: MASTER_ACTIVE, 主库: 可用, 从库: 可用 ===
```

4. 执行查询操作，检查是否路由到从库
5. 执行写入操作，检查是否路由到主库

## License

MIT

## 捐助

如果这个项目对你有帮助，欢迎支持持续开发。

<div align="center">
  <img src="https://github.com/DBGuardian/DBGuardian-doc/blob/main/1780427856871.jpg?raw=true" alt="捐助二维码" width="360" />
</div>

## 定制化服务

如果你的项目需要以下定制化需求，欢迎联系我们：
- 专用数据库版本支持（如 PostgreSQL、Oracle 等）
- 更多 ORM 框架集成（如 JPA、Hibernate、JdbcTemplate 等）
- 多主库、多从库等复杂架构支持
- 其他定制化功能开发

联系方式：
- QQ：664235822
- 邮箱：664235822@qq.com
