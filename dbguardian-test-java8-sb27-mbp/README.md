# DBGuardian 测试项目 - Java8 / Spring Boot 2.7 / MyBatis-Plus

> **技术栈**: `[JAVA8]` `[SPRING_BOOT_27]` `[MYBATIS_PLUS]` `[MYSQL]`

这是一个用于测试 DBGuardian 读写分离功能的最小化 Spring Boot 项目。

## 技术栈

| 技术 | 版本 | 说明 |
|-----|------|------|
| Java | 8 | JDK |
| Spring Boot | 2.7.18 | Web 框架 |
| MyBatis-Plus | 3.5.3.1 | ORM 框架 |
| MySQL | 8.0 | 数据库 |
| Redis | 6.x / 7.x | 分布式协调 |

## 项目结构

```
dbguardian-test-java8-sb27-mbp/
├── pom.xml                          # Maven 配置
├── README.md                        # 本文件
└── src/
    ├── main/
    │   ├── java/com/dbguardian/test/
    │   │   ├── Application.java              # 应用入口
    │   │   ├── controller/
    │   │   │   ├── UserController.java       # 用户控制器
    │   │   │   └── DatasourceController.java # 数据源状态监控
    │   │   ├── entity/
    │   │   │   ├── User.java                 # 用户实体
    │   │   │   └── Order.java                # 订单实体
    │   │   ├── mapper/
    │   │   │   ├── UserMapper.java           # 用户Mapper
    │   │   │   └── OrderMapper.java          # 订单Mapper
    │   │   └── service/
    │   │       ├── UserService.java          # 用户服务
    │   │       └── impl/
    │   │           └── UserServiceImpl.java  # 用户服务实现
    │   └── resources/
    │       ├── application.yml               # 应用配置
    │       └── db/
    │           └── schema.sql                 # 数据库初始化脚本
    └── test/
        └── java/com/dbguardian/test/
            └── ReadWriteSplittingTest.java   # 读写分离测试
```

## 快速开始

### 1. 初始化数据库

在主库和从库都执行 `src/main/resources/db/schema.sql` 脚本。

### 2. 修改配置

编辑 `src/main/resources/application.yml`，确保数据库和 Redis 配置正确：

```yaml
spring:
  datasource:
    dbguardian:
      enabled: true

      # 主库配置
      master:
        url: jdbc:mysql://YOUR_MASTER_HOST:3306/dbguardian_test
        username: YOUR_USERNAME
        password: YOUR_PASSWORD

      # 从库配置
      slave:
        url: jdbc:mysql://YOUR_SLAVE_HOST:3306/dbguardian_test
        username: YOUR_USERNAME
        password: YOUR_PASSWORD

  # Redis 配置（用于分布式协调）
  redis:
    host: YOUR_REDIS_HOST
    port: 6379
    password: YOUR_PASSWORD
```

### 3. 构建项目

```bash
# 先构建 DBGuardian 核心模块
cd ../
mvn clean package -DskipTests

# 启动测试项目
cd dbguardian-test
mvn spring-boot:run
```

### 4. 访问接口

- API文档：http://localhost:8081/api/doc.html
- 用户列表：http://localhost:8081/api/user/list
- 数据源状态：http://localhost:8081/api/datasource/status

## 测试用例

| 测试用例 | 说明 |
|---------|------|
| `testReadFromSlave` | 验证 SELECT 路由到从库 |
| `testWriteToMaster` | 验证 INSERT/UPDATE/DELETE 路由到主库 |
| `testTransactionReadWrite` | 验证事务内操作路由到主库 |
| `testMasterFailover` | 验证主库故障自动切换 |
| `testSlaveFailover` | 验证从库故障自动切换 |

## 相关测试项目

| 项目 | 技术栈 | 状态 |
|-----|-------|------|
| `dbguardian-test-java8-sb27-mbp` | Java8 + SB2.7 + MyBatis-Plus | ✅ 已完成 |
| `dbguardian-test-java17-sb32-jpa` | Java17 + SB3.2 + JPA | 📋 规划 |
| `dbguardian-test-java17-sb32-mbp-pg` | Java17 + SB3.2 + MyBatis-Plus + PostgreSQL | 📋 规划 |

详细测试项目规划请参考 [测试项目规划](../doc/测试项目规划.md)。

## 扩展功能

这个项目设计为可扩展的测试环境，可以轻松添加：

- 多主库配置
- 多从库配置
- 集群配置
- 分库分表配置
- 其他数据源类型（如 PostgreSQL）

如需扩展功能，请参考 DBGuardian 核心模块的配置和注解。

---

**项目版本**: 1.0.0
**最后更新**: 2026-05-19
