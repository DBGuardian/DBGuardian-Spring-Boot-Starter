# DBGuardian 测试项目 - Java11 / Spring Boot 2.7 / MyBatis-Plus

> **技术栈**: `[JAVA11]` `[SPRING_BOOT_27]` `[MYBATIS_PLUS]` `[MYSQL]`

这是一个用于测试 DBGuardian 读写分离功能的最小化 Spring Boot 项目。

## 技术栈

| 技术 | 版本 | 说明 |
|-----|------|------|
| Java | 11 | JDK |
| Spring Boot | 2.7.18 | Web 框架 |
| MyBatis-Plus | 3.5.3.1 | ORM 框架 |
| MySQL | 8.0 | 数据库 |
| Redis | 6.x / 7.x | 分布式协调 |

## 项目结构

```
dbguardian-test-java11-sb27-mbp/
├── pom.xml
├── README.md
└── src/
    ├── main/
    └── test/
```

## 快速开始

1. 在主库和从库执行 `src/main/resources/db/schema.sql`。
2. 编辑 `src/main/resources/application.yml` 和 `src/test/resources/application-test.yml`。
3. 先在仓库根目录构建 DBGuardian 核心模块。
4. 进入当前项目执行 `mvn spring-boot:run` 或 `mvn test`。

## 测试重点

- 读请求路由到从库
- 写请求路由到主库
- 事务内强制主库
- 主从故障转移
- Redis 协调状态同步

## 相关规划

- `dbguardian-test-java8-sb27-mbp`：已完成基线项目
- `dbguardian-test-java11-sb27-mbp`：Java 11 版本
- `dbguardian-test-java8-sb27-mybatis`：原生 MyBatis 版本

详细规划见 `../doc/测试项目规划.md`。