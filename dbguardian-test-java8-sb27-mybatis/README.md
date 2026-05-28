# DBGuardian 测试项目 - Java8 / Spring Boot 2.7 / MyBatis

> 技术栈: `[JAVA8]` `[SPRING_BOOT_27]` `[MYBATIS]`

这个样例现在用于验证 `dbguardian-boot2-starter` 的接入是否成立，重点不再是旧单体 starter 的直接行为，而是新多模块结构下的装配边界、配置绑定和基础路由决策。

## 当前验证范围

- Boot2 starter 自动装配可用
- `dbguardian.*` 配置可绑定为拓扑模型
- 默认路由策略可根据读/写/事务/强制主库做节点选择
- 协调服务、故障控制器、健康检查器能被装配
- 应用自身 MyBatis 数据访问仍可通过测试环境单数据源跑通

## 配置分层

### 运行态

- `src/main/resources/application.yml`
- 使用应用自己的 `spring.datasource`
- 同时提供 `dbguardian.nodes` 作为拓扑描述

### 测试态

- `src/test/resources/application-test.yml`
- 使用 H2 内存库承接 MyBatis 访问
- 使用 `dbguardian.nodes` 验证 Boot2 starter 的配置绑定和路由链

## 快速使用

1. 先在仓库根目录执行 `mvn install -DskipTests`
2. 进入当前项目目录
3. 执行 `mvn test`
4. 如果只看 starter 接入是否成立，优先关注：
   - `DbGuardianBasicTest`
   - `ReadWriteSplittingTest`
   - `CoordinationTest`
   - `FailoverTest`

## 说明

这个项目已经从“旧 starter 直连验证”切换为“新 Boot2 starter 接入验证”。