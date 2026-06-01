# DBGuardian 测试项目 - Java 21 + Spring Boot 3.3 + MyBatis-Plus

## 项目概述

本项目是 DBGuardian 读写分离功能在 **Java 21 + Spring Boot 3.3 + MyBatis-Plus** 技术栈下的测试项目。

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | Java 21 |
| Spring Boot | 3.3.5 |
| MyBatis-Plus | 3.5.7 |
| MySQL | 8.0 |
| DBGuardian | 1.0.0 |

## 项目结构

```
dbguardian-test-java21-sb33-mbp/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/test/
│   │   │   ├── Application.java          # 启动类
│   │   │   ├── entity/
│   │   │   │   ├── User.java             # 用户实体
│   │   │   │   └── Order.java            # 订单实体
│   │   │   ├── mapper/
│   │   │   │   ├── UserMapper.java       # 用户Mapper
│   │   │   │   └── OrderMapper.java      # 订单Mapper
│   │   │   ├── service/
│   │   │   │   ├── UserService.java      # 用户服务接口
│   │   │   │   ├── OrderService.java     # 订单服务接口
│   │   │   │   └── impl/
│   │   │   │       ├── UserServiceImpl.java
│   │   │   │       └── OrderServiceImpl.java
│   │   │   └── controller/
│   │   │       ├── UserController.java   # 用户REST控制器
│   │   │       └── OrderController.java  # 订单REST控制器
│   │   └── resources/
│   │       ├── application.yml           # 主配置
│   │       └── db/
│   │           └── schema.sql           # 数据库脚本
│   └── test/
│       ├── java/com/test/
│       │   ├── DbGuardianUnitTest.java
│       │   ├── DbGuardianBasicTest.java
│       │   ├── ReadWriteSplittingTest.java
│       │   ├── FailoverTest.java
│       │   ├── CoordinationTest.java
│       │   └── DataSourceContextHolderTest.java
│       └── resources/
│           └── application-test.yml      # 测试配置
└── README.md
```

## 快速开始

### 1. 编译项目

```bash
cd dbguardian-test-java21-sb33-mbp
mvn clean compile
```

### 2. 运行测试

```bash
mvn test
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

### 4. 访问 API 文档

启动后访问: http://localhost:8081/api/doc.html

## API 接口

### 用户管理

| 方法 | 路径 | 描述 | 路由 |
|------|------|------|------|
| GET | /api/user/list | 获取用户列表 | 读 -> 从库 |
| GET | /api/user/{id} | 获取用户详情 | 读 -> 从库 |
| POST | /api/user | 创建用户 | 写 -> 主库 |
| PUT | /api/user/{id} | 更新用户 | 写 -> 主库 |
| DELETE | /api/user/{id} | 删除用户 | 写 -> 主库 |

### 订单管理

| 方法 | 路径 | 描述 | 路由 |
|------|------|------|------|
| GET | /api/order/list | 获取订单列表 | 读 -> 从库 |
| GET | /api/order/{id} | 获取订单详情 | 读 -> 从库 |
| GET | /api/order/user/{userId} | 获取用户的订单 | 读 -> 从库 |
| POST | /api/order | 创建订单 | 写 -> 主库 |
| PUT | /api/order/{id} | 更新订单 | 写 -> 主库 |
| DELETE | /api/order/{id} | 删除订单 | 写 -> 主库 |

## 测试用例

### 基础测试 (DbGuardianBasicTest)
- `contextLoads` - 验证 Spring Boot 上下文加载
- `testDbGuardianAutoConfiguration` - 验证 DBGuardian 自动配置
- `testMyBatisPlusIntegration` - 验证 MyBatis-Plus 集成
- `testDataSourceConfiguration` - 验证数据源配置

### 读写分离测试 (ReadWriteSplittingTest)
- `testSelectRouteToSlave` - 验证读操作路由到从库
- `testInsertRouteToMaster` - 验证写操作路由到主库
- `testTransactionContext` - 验证事务内操作
- `testMethodNameRoutingRules` - 验证方法名路由规则
- `testForceMaster` - 验证强制主库标记
- `testReadOnlyTransaction` - 验证只读事务

### 故障转移测试 (FailoverTest)
- `testCoordinationServiceAvailable` - 验证协调服务可用性
- `testCoordinationServiceStatus` - 验证协调服务状态
- `testFailoverConfiguration` - 验证故障转移配置
- `testMasterStatusGet` - 验证主从状态获取
- `testCoordinationServiceHealth` - 验证协调服务健康状态
- `testStatusSyncToRedis` - 验证状态同步到 Redis

### 协调服务测试 (CoordinationTest)
- `testCoordinationServiceBeanExists` - 验证协调服务 Bean
- `testFailoverControllerBeanExists` - 验证故障转移控制器 Bean
- `testDataSourceHealthCheckerBeanExists` - 验证数据源健康检查器 Bean
- `testRoutingEngineBeanExists` - 验证路由引擎 Bean
- `testTopologyRegistryBeanExists` - 验证拓扑注册器 Bean
- `testCapabilityRegistryBeanExists` - 验证能力注册器 Bean
- `testDataSourceRegistryBeanExists` - 验证数据源注册器 Bean
- `testFailoverOrchestratorBeanExists` - 验证故障转移编排器 Bean

## 配置说明

详细配置请参考 `src/main/resources/application.yml`。

主要配置项：
- `server.port` - 服务端口 (默认 8081)
- `spring.datasource.master` - 主库配置
- `spring.datasource.slave` - 从库配置
- `spring.data.redis` - Redis 配置（用于集群协调）

## 与 Spring Boot 3.2 版本对比

本项目与 `dbguardian-test-java17-sb32-mbp` 的主要区别：

| 配置项 | SB 3.2 | SB 3.3 |
|--------|--------|--------|
| Spring Boot 版本 | 3.2.5 | 3.3.5 |
| JDK 版本 | Java 17 | Java 21 |
| 服务端口 | 8081 | 8081 |
| 配置方式 | 相同 | 相同 |

## License

MIT
