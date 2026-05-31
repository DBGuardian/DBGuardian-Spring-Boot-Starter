# DBGuardian 测试项目 - Java 17 / Spring Boot 3.0 / MyBatis-Plus

## 项目概述

本项目是 DBGuardian 读写分离功能的测试项目，用于验证 DBGuardian 在 Java 17 + Spring Boot 3.0 + MyBatis-Plus 环境下的功能。

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | 17 |
| Spring Boot | 3.0.13 |
| MyBatis-Plus | 3.5.7 |
| MySQL | 8.0 |
| Redis | - |
| DBGuardian | dbguardian-boot3-starter |

## 项目结构

```
dbguardian-test-java17-sb30-mbp/
├── pom.xml                                    # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/com/test/
│   │   │   ├── Application.java              # 启动类
│   │   │   ├── entity/                       # 实体类
│   │   │   │   ├── User.java
│   │   │   │   └── Order.java
│   │   │   ├── mapper/                       # Mapper 接口
│   │   │   │   ├── UserMapper.java
│   │   │   │   └── OrderMapper.java
│   │   │   ├── service/                     # 服务层
│   │   │   │   ├── UserService.java
│   │   │   │   └── impl/UserServiceImpl.java
│   │   │   └── controller/                   # 控制器
│   │   │       ├── UserController.java
│   │   │       └── DatasourceController.java
│   │   └── resources/
│   │       ├── application.yml               # 主配置
│   │       └── db/schema.sql                 # 数据库脚本
│   └── test/
│       ├── java/com/test/
│       │   ├── ReadWriteSplittingTest.java   # 读写分离测试
│       │   ├── FailoverTest.java             # 故障转移测试
│       │   ├── DbGuardianBasicTest.java      # 基础功能测试
│       │   ├── DbGuardianUnitTest.java      # 单元测试
│       │   ├── DataSourceContextHolderTest.java # 上下文测试
│       │   └── CoordinationTest.java         # 协调服务测试
│       └── resources/
│           └── application-test.yml           # 测试配置
└── README.md
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis (可选，用于协调服务)

### 2. 数据库配置

在 MySQL 主从库执行数据库初始化脚本：

```bash
mysql -h 192.168.3.150 -u root -p < src/main/resources/db/schema.sql
mysql -h 192.168.3.151 -u root -p < src/main/resources/db/schema.sql
```

### 3. 构建项目

```bash
mvn clean install
```

### 4. 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ReadWriteSplittingTest
```

### 5. 启动应用

```bash
mvn spring-boot:run
```

应用启动后访问：`http://localhost:8081/api/doc.html` (Knife4j API 文档)

## API 接口

### 用户管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/user/list | 获取用户列表 (读 -> 从库) |
| GET | /api/user/{id} | 获取用户详情 (读 -> 从库) |
| POST | /api/user | 创建用户 (写 -> 主库) |
| PUT | /api/user/{id} | 更新用户 (写 -> 主库) |
| DELETE | /api/user/{id} | 删除用户 (写 -> 主库) |

### 数据源监控

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/datasource/status | 获取数据源协调状态 |
| GET | /api/datasource/health | 健康检查 |

## 测试用例

### 读写分离测试 (ReadWriteSplittingTest)

| 用例 | 描述 |
|------|------|
| TC-001 | 基础读操作路由到从库 |
| TC-002 | 基础写操作路由到主库 |
| TC-003 | 事务内操作强制主库 |
| TC-004 | 方法名自动路由规则测试 |
| TC-005 | Mapper 注入测试 |
| TC-006 | 数据源切换上下文测试 |
| TC-007 | 强制主库测试 |
| TC-008 | 只读事务测试 |

### 故障转移测试 (FailoverTest)

| 用例 | 描述 |
|------|------|
| TC-005 | 测试协调服务可用性 |
| TC-006 | 测试协调服务状态 |
| TC-007 | 测试故障转移配置 |
| TC-008 | 测试主从状态获取 |
| TC-009 | 测试协调服务健康状态 |
| TC-010 | 测试状态同步到 Redis |

### 协调服务测试 (CoordinationTest)

| 用例 | 描述 |
|------|------|
| TC-001 | 测试协调服务 Bean 存在 |
| TC-002 | 测试故障转移控制器 Bean 存在 |
| TC-003 | 测试数据源健康检查器 Bean 存在 |
| TC-004 | 测试路由引擎 Bean 存在 |
| TC-005 | 测试拓扑注册器 Bean 存在 |
| TC-006 | 测试能力注册器 Bean 存在 |
| TC-007 | 测试数据源注册器 Bean 存在 |
| TC-008 | 测试故障转移编排器 Bean 存在 |

## 配置说明

### 主数据源配置

```yaml
spring:
  datasource:
    master:
      url: jdbc:mysql://192.168.3.150:3306/dbguardian_test
      username: root
      password: zhuzhou9uu897@
```

### 从数据源配置

```yaml
spring:
  datasource:
    slave:
      url: jdbc:mysql://192.168.3.151:3306/dbguardian_test
      username: root
      password: zhuzhou9uu897@
```

### Redis 配置 (可选)

```yaml
spring:
  redis:
    host: 192.168.3.150
    port: 6379
    password: zhuzhou9uu897@
```

## 故障排除

### 1. 编译错误

确保使用 JDK 17+：

```bash
java -version
mvn clean install -DskipTests
```

### 2. 测试失败

检查数据库连接配置是否正确：

```bash
mysql -h 192.168.3.150 -u root -p -e "SELECT 1"
mysql -h 192.168.3.151 -u root -p -e "SELECT 1"
```

### 3. Redis 连接失败

Redis 是可选组件，用于协调服务。如果不需要多实例协调，可以注释掉 Redis 配置。

## 许可证

本项目是 DBGuardian 的一部分，使用 MIT 许可证。
