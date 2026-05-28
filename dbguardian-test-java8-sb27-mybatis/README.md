# DBGuardian 测试项目 - Java8 / Spring Boot 2.7 / MyBatis

> **技术栈**: `[JAVA8]` `[SPRING_BOOT_27]` `[MYBATIS]` `[MYSQL]`

这是一个用于测试 DBGuardian 读写分离功能的最小化 Spring Boot 项目。

## 技术栈

| 技术 | 版本 | 说明 |
|-----|------|------|
| Java | 8 | JDK |
| Spring Boot | 2.7.18 | Web 框架 |
| MyBatis | 2.3.1 | ORM 框架 |
| MySQL | 8.0 | 数据库 |
| Redis | 6.x / 7.x | 分布式协调 |

## 项目结构

```
dbguardian-test-java8-sb27-mybatis/
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

## 说明

这个项目保留了和基线项目一致的业务轮廓，但把 ORM 层收缩成原生 MyBatis 接口，便于验证 DBGuardian 与非 Plus 形态的兼容性。

详细规划见 `../doc/测试项目规划.md`。