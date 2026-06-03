<div align="center">
  <img src="https://github.com/DBGuardian/DBGuardian-doc/blob/main/logo.png?raw=true" alt="DBGuardian Logo" width="220" />

  # DBGuardian Spring Boot Starter

  ![v1.0.0](https://img.shields.io/badge/version-v1.0.0-green)
  ![GitHub stars](https://img.shields.io/github/stars/DBGuardian/DBGuardian-Spring-Boot-Starter?style=flat-square)
  ![GitHub forks](https://img.shields.io/github/forks/DBGuardian/DBGuardian-Spring-Boot-Starter?style=flat-square)
  ![GitHub issues](https://img.shields.io/github/issues/DBGuardian/DBGuardian-Spring-Boot-Starter?style=flat-square)
  ![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)
</div>

Spring Boot 数据库高可用 Starter - 读写分离 + 自动故障转移

## 功能特性

- **读写分离**: 自动将读操作路由到从库，写操作路由到主库
- **自动故障转移**: 主库故障时自动将从库升为主库
- **分布式协调**: 基于 Redis 实现多实例状态同步
- **健康检查**: 定时检测主从库健康状态
- **原主库恢复**: 支持原主库恢复后作为从库追赶数据
- **降级启动**: 允许数据库不可用时降级启动

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.dbguardian</groupId>
    <artifactId>dbguardian-boot2-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置文件

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

- Spring Boot 2.7.x
- MySQL 5.7+ / 8.0+
- Redis 6.x+ (可选，用于分布式协调)

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
- 专用数据库版本支持（如 PostgreSQL、Oracle、SQL Server 等）
- 更多 ORM 框架集成（如 JPA、Hibernate、JdbcTemplate 等）
- 多主库、多从库等复杂架构支持
- 其他定制化功能开发

联系方式：
- QQ：664235822
- 邮箱：664235822@qq.com
