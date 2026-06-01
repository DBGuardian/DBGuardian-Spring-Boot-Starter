# DBGuardian 测试项目 - Java 17 + Spring Boot 3.1 + MyBatis-Plus

## 项目概述

本项目是 DBGuardian 读写分离功能在 **Java 17 + Spring Boot 3.1 + MyBatis-Plus** 技术栈下的测试项目。

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | Java 17 |
| Spring Boot | 3.1.12 |
| MyBatis-Plus | 3.5.7 |
| MySQL | 8.0 |
| DBGuardian | 1.0.0 |

## 项目结构

```
dbguardian-test-java17-sb31-mbp/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/test/
│   │   │   ├── Application.java          # 启动类
│   │   │   ├── entity/
│   │   │   │   └── User.java             # 用户实体
│   │   │   ├── mapper/
│   │   │   │   └── UserMapper.java       # 用户Mapper
│   │   │   ├── service/
│   │   │   │   ├── UserService.java      # 服务接口
│   │   │   │   └── impl/
│   │   │   │       └── UserServiceImpl.java
│   │   │   └── controller/
│   │   │       └── UserController.java    # REST控制器
│   │   └── resources/
│   │       ├── application.yml           # 主配置
│   │       └── db/
│   │           └── schema.sql            # 数据库脚本
│   └── test/
│       ├── java/com/test/
│       │   ├── DbGuardianBasicTest.java
│       │   ├── ReadWriteSplittingTest.java
│       │   ├── FailoverTest.java
│       │   ├── LoadBalanceTest.java
│       │   └── DataSourceContextHolderTest.java
│       └── resources/
│           └── application-test.yml      # 测试配置
└── README.md
```

## 快速开始

### 1. 编译项目

```bash
cd dbguardian-test-java17-sb31-mbp
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

启动后访问: http://localhost:8082/api/doc.html

## API 接口

| 方法 | 路径 | 描述 | 路由 |
|------|------|------|------|
| GET | /api/user/list | 获取用户列表 | 读 -> 从库 |
| GET | /api/user/{id} | 获取用户详情 | 读 -> 从库 |
| POST | /api/user | 创建用户 | 写 -> 主库 |
| PUT | /api/user/{id} | 更新用户 | 写 -> 主库 |
| DELETE | /api/user/{id} | 删除用户 | 写 -> 主库 |

## 测试用例

### 基础测试
- `DbGuardianBasicTest.contextLoads` - 验证 Spring Boot 上下文加载

### 读写分离测试
- `ReadWriteSplittingTest.testDataSourceConfigured` - 验证数据源配置
- `ReadWriteSplittingTest.testWriteOperation` - 验证写操作
- `ReadWriteSplittingTest.testReadOperation` - 验证读操作

### 故障转移测试
- `FailoverTest.testDataSourceRegistryExists` - 验证数据源注册器
- `FailoverTest.testFailoverControllerExists` - 验证故障转移控制器
- `FailoverTest.testFailoverOrchestratorExists` - 验证故障转移编排器

### 负载均衡测试
- `LoadBalanceTest.testRoutingDataSourceType` - 验证路由数据源类型
- `LoadBalanceTest.testMultipleReadOperationsDistribution` - 验证多次读操作分发
- `LoadBalanceTest.testWriteReadConsistency` - 验证写读一致性

## 配置说明

详细配置请参考 `src/main/resources/application.yml`。

主要配置项：
- `server.port` - 服务端口 (默认 8082)
- `spring.datasource.master` - 主库配置
- `spring.datasource.slave` - 从库配置
- `spring.data.redis` - Redis 配置（用于集群协调）

## 与 Spring Boot 3.0 版本对比

本项目与 `dbguardian-test-java17-sb30-mbp` 的主要区别：

| 配置项 | SB 3.0 | SB 3.1 |
|--------|--------|--------|
| Spring Boot 版本 | 3.0.13 | 3.1.12 |
| 服务端口 | 8081 | 8082 |
| 配置方式 | 相同 | 相同 |

## License

MIT
