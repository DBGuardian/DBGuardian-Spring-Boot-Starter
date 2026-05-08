# DBGuardian 测试项目

这是一个用于测试 DBGuardian 读写分离功能的最小化 Spring Boot 项目。

## 项目结构

```
dbguardian-test/
├── src/main/java/com/test/
│   ├── Application.java              # 应用入口
│   ├── controller/
│   │   ├── UserController.java       # 用户控制器（测试读写分离）
│   │   └── DatasourceController.java # 数据源状态监控
│   ├── entity/
│   │   ├── User.java                 # 用户实体
│   │   └── Order.java                # 订单实体
│   ├── mapper/
│   │   ├── UserMapper.java           # 用户Mapper
│   │   └── OrderMapper.java          # 订单Mapper
│   └── service/
│       ├── UserService.java           # 用户服务接口
│       └── impl/
│           └── UserServiceImpl.java  # 用户服务实现
├── src/main/resources/
│   ├── application.yml               # 应用配置
│   └── db/
│       └── schema.sql                 # 数据库初始化脚本
└── pom.xml                           # Maven配置
```

## 快速开始

### 1. 初始化数据库

在主库和从库都执行 `src/main/resources/db/schema.sql` 脚本。

### 2. 修改配置

编辑 `application.yml`，确保数据库和 Redis 配置正确：

```yaml
spring:
  datasource:
    master:
      url: jdbc:mysql://YOUR_MASTER_HOST:3306/dbguardian_test
      username: YOUR_USERNAME
      password: YOUR_PASSWORD
    slave:
      url: jdbc:mysql://YOUR_SLAVE_HOST:3306/dbguardian_test
      username: YOUR_USERNAME
      password: YOUR_PASSWORD
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
- 用户接口：http://localhost:8081/api/user/list
- 数据源状态：http://localhost:8081/api/datasource/status

## 测试读写分离

### 创建用户（写操作）
```bash
curl -X POST http://localhost:8081/api/user \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@test.com","phone":"13900000000"}'
```

### 查询用户（读操作）
```bash
curl http://localhost:8081/api/user/list
curl http://localhost:8081/api/user/1
```

### 查看日志

观察控制台日志，应该能看到：
- 写操作路由到主库
- 读操作路由到从库

## 扩展功能

这个项目设计为可扩展的测试环境，可以轻松添加：

- 多主库配置
- 多从库配置
- 集群配置
- 分库分表配置
- 其他数据源类型（如 PostgreSQL）

如需扩展功能，请参考 DBGuardian 核心模块的配置和注解。
