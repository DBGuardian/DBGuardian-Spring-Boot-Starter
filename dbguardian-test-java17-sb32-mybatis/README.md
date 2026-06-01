# DBGuardian 测试项目 - Java17 / Spring Boot 3.2 / MyBatis

> 技术栈: `[JAVA17]` `[SPRING_BOOT_32]` `[MYBATIS]` `[MYSQL]`

这个测试项目用于验证 `dbguardian-boot3-starter` 在 Java 17 + Spring Boot 3.2 + MyBatis 环境下的读写分离功能。

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | Java 17 |
| Spring Boot | 3.2.5 |
| MyBatis | 3.0.4 |
| MySQL | 8.0 |
| DBGuardian | dbguardian-boot3-starter |

## 快速使用

### 1. 前提条件

- JDK 17+
- Maven 3.8+
- MySQL 8.0 主从环境
- Redis 服务

### 2. 初始化数据库

在主库和从库执行 `src/main/resources/db/schema.sql`：

```bash
mysql -h 192.168.3.150 -u root -p < src/main/resources/db/schema.sql
mysql -h 192.168.3.151 -u root -p < src/main/resources/db/schema.sql
```

### 3. 编译测试

```bash
# 先在仓库根目录安装依赖
mvn install -DskipTests

# 进入测试项目目录
cd dbguardian-test-java17-sb32-mybatis

# 编译
mvn compile

# 运行单元测试
mvn test
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

访问 API 文档：http://localhost:8081/api/doc.html

## 项目结构

```
dbguardian-test-java17-sb32-mybatis/
├── pom.xml                          # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/com/test/
│   │   │   ├── Application.java      # 启动类
│   │   │   ├── entity/              # 实体类
│   │   │   ├── mapper/              # Mapper 接口
│   │   │   ├── service/             # 服务层
│   │   │   └── controller/          # 控制器
│   │   └── resources/
│   │       ├── application.yml       # 主配置
│   │       └── db/schema.sql        # 数据库初始化
│   └── test/
│       ├── java/com/test/
│       │   ├── DbGuardianBasicTest.java
│       │   ├── DbGuardianUnitTest.java
│       │   ├── DataSourceContextHolderTest.java
│       │   ├── ReadWriteSplittingTest.java
│       │   ├── FailoverTest.java
│       │   └── CoordinationTest.java
│       └── resources/
│           └── application-test.yml # 测试配置
└── README.md
```

## 测试用例

| 测试类 | 说明 |
|--------|------|
| `DbGuardianUnitTest` | 单元测试：RoutingContext API |
| `DataSourceContextHolderTest` | 单元测试：上下文持有者 |
| `ReadWriteSplittingTest` | 集成测试：读写分离功能 |
| `FailoverTest` | 集成测试：故障转移功能 |
| `CoordinationTest` | 集成测试：负载均衡功能 |

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user` | 创建用户（写 → 主库） |
| GET | `/user/{id}` | 获取用户（读 → 从库） |
| GET | `/user/list` | 获取列表（读 → 从库） |

## 配置说明

主要配置项位于 `application.yml`：

```yaml
spring:
  datasource:
    master:           # 主库配置
    slave:            # 从库配置
    replication:      # 主从复制配置
  data:
    redis:            # Redis 配置（协调服务）
```
