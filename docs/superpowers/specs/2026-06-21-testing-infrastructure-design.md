# 测试基础设施设计规格

> 创建：2026-06-21  
> 背景：NL2SQL 平台后端基础设施骨架已补齐，但全项目零 `src/test`。本规格定义第一阶段测试基础设施的目标、范围与实施标准，使 `mvn test` 能在所有后端模块跑通，并为后续 Testcontainers 集成测试预留扩展点。

---

## 1. 目标

1. 为 `common`、`schema-service`、`query-service`、`ai-service`、`gateway-service` 建立可运行的测试骨架。
2. 让 `mvn test` 在每个模块都能无外部依赖（无需 Docker / MySQL / Redis / RabbitMQ）地通过。
3. 覆盖三类测试：
   - **单元测试**：不启动 Spring，JUnit 5 + Mockito。
   - **Spring 配置属性测试**：轻量 Spring 上下文，验证 `@ConfigurationProperties` 绑定。
   - **Controller 切片测试**：`@WebMvcTest`，mock Service，验证接口与统一响应 `R<T>`。
4. 形成可复制的测试约定，后续业务功能随写随测。

---

## 2. 非目标

- 本期**不引入 Testcontainers**（B 阶段再做）。
- 本期**不写 Repository / MQ Listener / Cache 集成测试**。
- 不改动生产代码行为；仅当发现阻碍测试的 bug 时才修复。

---

## 3. 测试策略

按测试金字塔分三层推进：

| 层级 | 技术 | 适用场景 | 本期是否做 |
|------|------|----------|------------|
| 单元测试 | JUnit 5 + AssertJ + Mockito | common 工具类、Service 分支逻辑 | ✅ 是 |
| 切片测试 | `@WebMvcTest`、`@EnableConfigurationProperties` | Controller、配置属性 | ✅ 是 |
| 集成测试 | `@SpringBootTest` + Testcontainers | Repository、Listener、Cache 完整链路 | ❌ 下期 |

---

## 4. Maven 配置

### 4.1 根 `pom.xml`

在 `<dependencyManagement>` 中显式引入测试依赖版本（由 Spring Boot BOM / Testcontainers BOM 管理）：

- `org.springframework.boot:spring-boot-starter-test`
- `org.testcontainers:testcontainers-bom`（为 B 阶段预留版本管理）

### 4.2 各服务 `pom.xml`

新增 `test` scope 依赖：

- `spring-boot-starter-test`
- `com.h2database:h2`（为后续可能的数据库切片测试占位，本期暂不使用）

### 4.3 `common/pom.xml`

不引入完整 `spring-boot-starter-test`，仅引入最小测试依赖：

- `org.junit.jupiter:junit-jupiter`
- `org.mockito:mockito-core`
- `org.assertj:assertj-core`

保持 common 模块轻量。

---

## 5. 目录与命名约定

```
<module>/src/test/java/com/nl2sql/<module>/
    controller/XxxControllerTest.java    # @WebMvcTest
    service/XxxServiceTest.java          # Mockito 单元测试
    XxxPropertiesTest.java               # @EnableConfigurationProperties

<module>/src/test/resources/
    application-test.yml                 # 测试 profile 最小配置
```

约定：

- 单元/切片测试类名以 `Test` 结尾，由 Maven Surefire 自动识别。
- 未来集成测试类名以 `IT` 结尾，由 Maven Failsafe 识别。
- 所有测试使用 `@DisplayName` 中文说明测试意图。
- Controller 测试统一断言 `R<T>` JSON：`{"code":200,"message":"success","data":...}`。

---

## 6. 测试配置 `application-test.yml`

每个服务测试资源目录下新增 `application-test.yml`，统一关闭外部依赖：

```yaml
spring:
  application:
    name: ${服务名}-test
  cloud:
    nacos:
      discovery:
        enabled: false
  main:
    allow-bean-definition-overriding: true
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

logging:
  level:
    root: warn
```

避免 Spring Boot 上下文启动时尝试连接 Nacos / Redis / RabbitMQ。

---

## 7. A 阶段测试清单

### 7.1 common 模块

| 测试类 | 类型 | 覆盖点 |
|--------|------|--------|
| `RTest` | 单元 | `ok()` / `ok(T)` / `error(String)` / `error(int, String)` / `error(IResultCode)` |
| `ResultCodeTest` | 单元 | 各枚举的 `code`、`i18nKey`、`desc` |
| `BaseExceptionTest` | 单元 | 各构造器、`getCode()` |
| `CachePropertiesTest` | 属性绑定 | 默认值、`ttls` 自定义绑定 |
| `MqPropertiesTest` | 属性绑定 | 默认值（maxRetries、initialInterval 等） |
| `PageResultTest` | 单元 | `of()` 工厂、分页字段计算 |

### 7.2 schema-service

| 测试类 | 类型 | 覆盖点 |
|--------|------|--------|
| `DataSourceControllerTest` | @WebMvcTest | POST `/api/schema/datasource`、GET `/api/schema/datasource/list`、DELETE `/api/schema/datasource/{id}`；断言 `R<T>` |
| `DataSourceServiceTest` | Mockito | `create` / `list` / `delete` 调用 Repository；`scanTables` / `getTableDetail` 返回 Mock 数据 |

### 7.3 query-service

| 测试类 | 类型 | 覆盖点 |
|--------|------|--------|
| `QueryControllerTest` | @WebMvcTest | POST `/api/query/nl`、POST `/api/query/sql`、GET `/api/query/history`、GET `/api/query/history/page` |
| `QueryServiceTest` | Mockito | `queryByNaturalLanguage` 发送 MQ 事件；`saveHistory` 默认值填充；`queryBySql` 返回 Mock |

### 7.4 ai-service

| 测试类 | 类型 | 覆盖点 |
|--------|------|--------|
| `AiControllerTest` | @WebMvcTest | POST `/api/ai/convert`、POST `/api/ai/validate` |
| `MockLLMServiceTest` | 单元 | 关键词分支（销售/用户/默认） |

### 7.5 gateway-service

| 测试类 | 类型 | 覆盖点 |
|--------|------|--------|
| `GatewayApplicationTest` | @SpringBootTest | 验证 Gateway 上下文能正常启动 |

---

## 8. 关键外部依赖的 mock 策略

| 依赖 | 测试层 | mock 方式 |
|------|--------|-----------|
| `DataSourceRepository` / `QueryHistoryRepository` | Service 单元测试 | `@Mock` + Mockito |
| `RabbitTemplate` | `QueryServiceTest` | `@Mock` |
| `MockLLMService` | `AiControllerTest` | `@MockBean` |
| `DataSourceService` | `DataSourceControllerTest` | `@MockBean` |
| `QueryService` | `QueryControllerTest` | `@MockBean` |

---

## 9. B 阶段预览：Testcontainers 集成测试

本期不实施，但在架构上预留：

1. 根 pom 引入 `org.testcontainers:testcontainers-bom`。
2. 各服务增加 `testcontainers-junit-jupiter`、`mysql`、`rabbitmq` 模块。
3. 新建 `AbstractIntegrationTest` 基类，统一声明并启动：
   - MySQL 容器
   - Redis 容器
   - RabbitMQ 容器
4. 测试类命名 `*IT.java`，使用 `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)`。
5. 典型测试：
   - `QueryHistoryRepositoryIT`
   - `NL2SQLListenerIT`
   - `CacheEvictionIT`

---

## 10. 验收标准

1. 执行 `mvn test` 时，5 个后端模块全部编译通过且测试通过。
2. 每个新增测试类均包含 `@DisplayName`。
3. Controller 测试统一断言 `R<T>` 的 `code`、`message`、`data` 字段。
4. 测试运行不依赖本地 Docker、MySQL、Redis、RabbitMQ。
5. 不引入生产代码行为变更（修 bug 除外）。

---

## 11. 风险与应对

| 风险 | 应对 |
|------|------|
| Nacos 导致上下文启动失败 | `application-test.yml` 关闭 `nacos.discovery.enabled` |
| RabbitMQ / Redis 自动配置连接失败 | `application-test.yml` 排除对应 `AutoConfiguration` |
| Gateway 测试因路由加载慢而超时 | 使用 `webEnvironment = MOCK`，不监听真实端口 |
| common 缺少 Spring 测试依赖 | common 单独引入 junit-jupiter + mockito + assertj |

---

## 12. 参考

- [2026-06-18-infra-backlog.md](../plans/2026-06-18-infra-backlog.md)
- [Spring Boot Testing Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
