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

支持 MySQL、PostgreSQL 和 MariaDB。请先选择与 Spring Boot 版本匹配的 Starter，再按数据库选择一份配置示例。

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

#### 数据库方言 `pom.xml` 配置示例

按 Spring Boot 版本选择一个 Starter，再按数据库从下方选择一组「方言 + JDBC 驱动」依赖，一起放入应用 `pom.xml` 的 `<dependencies>` 中。方言与 Starter 使用相同版本。若 Spring Boot 未管理示例所用坐标的驱动版本（例如较早的 Spring Boot 2.7 小版本可能不管理 `com.mysql:mysql-connector-j`），请为该驱动依赖显式指定兼容的 `<version>`。

**MySQL**

```xml
<dependency>
    <groupId>io.dbguardian</groupId>
    <artifactId>dbguardian-dialect-mysql</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**PostgreSQL**

```xml
<dependency>
    <groupId>io.dbguardian</groupId>
    <artifactId>dbguardian-dialect-postgresql</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**MariaDB**

```xml
<dependency>
    <groupId>io.dbguardian</groupId>
    <artifactId>dbguardian-dialect-mariadb</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. 按数据库填写配置

选择下面的一份示例写入 `application.yml`。替换节点地址和库名，并在启动前设置 `DBGUARDIAN_DB_USERNAME`、`DBGUARDIAN_DB_PASSWORD`、`DBGUARDIAN_REPLICATION_USER` 和 `DBGUARDIAN_REPLICATION_PASSWORD`。两个 JDBC URL 分别指向两个实际节点。示例假设数据库侧已建立主从复制。

#### MySQL 配置示例

```yaml
spring:
  datasource:
    master:
      url: jdbc:mysql://mysql-primary:3306/appdb
      driver-class-name: com.mysql.cj.jdbc.Driver
      username: ${DBGUARDIAN_DB_USERNAME}
      password: ${DBGUARDIAN_DB_PASSWORD}
    slave:
      url: jdbc:mysql://mysql-replica:3306/appdb
      driver-class-name: com.mysql.cj.jdbc.Driver
      username: ${DBGUARDIAN_DB_USERNAME}
      password: ${DBGUARDIAN_DB_PASSWORD}
    replication:
      master-user: ${DBGUARDIAN_REPLICATION_USER}
      master-password: ${DBGUARDIAN_REPLICATION_PASSWORD}
    allow-degraded-startup: true
```

#### PostgreSQL 配置示例

下面以应用能在待恢复节点 `pg-a` 上本地执行 `pg_rewind` 为例。自动追赶要求恢复脚本和命令路径指向该节点上的实际文件，且执行账号具备操作 PostgreSQL 数据目录的权限。

```yaml
spring:
  datasource:
    master:
      url: jdbc:postgresql://pg-a:5432/appdb
      driver-class-name: org.postgresql.Driver
      username: ${DBGUARDIAN_DB_USERNAME}
      password: ${DBGUARDIAN_DB_PASSWORD}
    slave:
      url: jdbc:postgresql://pg-b:5432/appdb
      driver-class-name: org.postgresql.Driver
      username: ${DBGUARDIAN_DB_USERNAME}
      password: ${DBGUARDIAN_DB_PASSWORD}
    replication:
      master-user: ${DBGUARDIAN_REPLICATION_USER}
      master-password: ${DBGUARDIAN_REPLICATION_PASSWORD}
    recovery:
      enabled: true
      auto-failback: true
      postgresql:
        enabled: true
        keep-promoted-primary: true
        data-directory: /var/lib/postgresql/16/main
        config-file: /etc/postgresql/16/main/postgresql.conf
        script-path: /opt/dbguardian/scripts/pg-rewind-recover.sh
        pg-ctl-command: /usr/lib/postgresql/16/bin/pg_ctl
        psql-command: /usr/lib/postgresql/16/bin/psql
        pg-rewind-command: /usr/lib/postgresql/16/bin/pg_rewind
        local-host: 127.0.0.1
        local-port: 5432
        local-database: postgres
        startup-wait-seconds: 60
        command-timeout-seconds: 900
    allow-degraded-startup: true
```

`keep-promoted-primary: true` 保留故障期间升主的节点承接写入，原主追赶后作为从库。恢复脚本必须在待恢复数据库节点上执行；应用与数据库分开部署时，请参阅 [PostgreSQL 自动追赶说明](../DBGuardian-doc/doc/PostgreSQL自动追赶说明.md)配置远程执行或自动回切。恢复完成后，健康检查确认从库处于流复制状态（`streaming`），才恢复从库读路由。

#### MariaDB 配置示例

适用于 MariaDB 10.11+ 单主单从环境。请预先配置 GTID（全局事务标识）复制，并为两个节点设置不同的 `server_id`。

```yaml
spring:
  datasource:
    master:
      url: jdbc:mariadb://mariadb-primary:3306/appdb?connectTimeout=3000&socketTimeout=10000
      driver-class-name: org.mariadb.jdbc.Driver
      username: ${DBGUARDIAN_DB_USERNAME}
      password: ${DBGUARDIAN_DB_PASSWORD}
    slave:
      url: jdbc:mariadb://mariadb-replica:3306/appdb?connectTimeout=3000&socketTimeout=10000
      driver-class-name: org.mariadb.jdbc.Driver
      username: ${DBGUARDIAN_DB_USERNAME}
      password: ${DBGUARDIAN_DB_PASSWORD}
    replication:
      master-user: ${DBGUARDIAN_REPLICATION_USER}
      master-password: ${DBGUARDIAN_REPLICATION_PASSWORD}
    recovery:
      enabled: true
      auto-failback: true
      catchup-timeout-seconds: 300
      catchup-check-interval-seconds: 2
    gtid-protection:
      enabled: true
      block-slave-reads-on-risk: true
      max-allowed-lag-seconds: 5
    allow-degraded-startup: true
```

`auto-failback: true` 表示原主追平后尝试恢复原主写入。设为 `false` 时，升主节点继续承接写入，原主追平且复制健康后可承担读流量。所需权限、故障时的原主隔离与回切流程见 [MariaDB 方言使用说明](../DBGuardian-doc/doc/MariaDB方言使用说明.md)。

#### 通用可选配置

- **Redis 分布式协调**：多个应用实例需要同步节点状态时，添加 `spring-boot-starter-data-redis`。在 Spring Boot 2.7 的 `spring.redis` 或 Spring Boot 3.x 的 `spring.data.redis` 下设置 `host`、`port`、`password`、`database` 和 `timeout`。单实例可不配置 Redis。
- **连接池**：如需调节 HikariCP，可在 `spring.datasource.master.hikari` 和 `spring.datasource.slave.hikari` 下分别设置 `maximum-pool-size`、`minimum-idle`、`connection-timeout`、`idle-timeout` 和 `max-lifetime`。

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
- MySQL 5.7+ / 8.0+、PostgreSQL 或 MariaDB 10.11+
- Redis 6.x+（可选，用于分布式协调）

## 测试

### 单元测试

```bash
mvn test
```

### 本地集成测试

在独立测试环境准备所选数据库的主从复制，按[配置示例](#2-按数据库填写配置)启动应用。可从[测试项目总览](../DBGuardian-Test/README.md)选择对应示例，并按其说明验证查询、写入和故障切换。

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
