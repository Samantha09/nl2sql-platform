# 测试基础设施（A 阶段）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `common`、`schema-service`、`query-service`、`ai-service`、`gateway-service` 建立测试骨架，使 `mvn test` 全模块无外部依赖跑通。

**Architecture:** 在根 pom 统一管理测试依赖版本，各模块按需引入 `spring-boot-starter-test` 或最小测试依赖；common 写纯单元测试，各服务用 `@WebMvcTest` 做 Controller 切片测试，Service 用 Mockito 单元测试；通过 `application-test.yml` 关闭 Nacos/Redis/RabbitMQ 连接。

**Tech Stack:** Java 17, Spring Boot 3.2.5, JUnit 5, Mockito, AssertJ, Spring Boot Test, Maven Surefire.

## Global Constraints

- 本期**不引入 Testcontainers**（B 阶段再做）。
- 本期**不写 Repository / MQ Listener / Cache 集成测试**。
- 不改动生产代码行为；仅当发现阻碍测试的 bug 时才修复。
- 所有测试类使用 `@DisplayName` 中文说明意图。
- Controller 测试统一断言 `R<T>` JSON：`{"code":200,"message":"success","data":...}`。
- 单元/切片测试类名以 `Test` 结尾；未来集成测试以 `IT` 结尾。
- 测试运行不依赖 Docker / MySQL / Redis / RabbitMQ。

---

## Task 1: 根 `pom.xml` 增加测试依赖版本管理

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Consumes: 无
- Produces: 子模块可引用 `spring-boot-starter-test` 和 Testcontainers BOM 版本

- [ ] **Step 1: 在 `dependencyManagement` 中追加测试依赖**

在 `pom.xml` 的 `<dependencyManagement>...</dependencies>` 内，现有三个 BOM 之后追加：

```xml
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-test</artifactId>
                <version>${spring-boot.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>1.19.7</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.h2database</groupId>
                <artifactId>h2</artifactId>
                <version>2.2.224</version>
            </dependency>
```

- [ ] **Step 2: 验证根 pom 格式**

Run: `mvn -q -N validate`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore(test): 根 pom 增加测试依赖版本管理"
```

---

## Task 2: `common/pom.xml` 增加最小测试依赖

**Files:**
- Modify: `common/pom.xml`

**Interfaces:**
- Consumes: 根 pom 中的依赖版本管理
- Produces: common 模块可进行 JUnit + Mockito + AssertJ 测试

- [ ] **Step 1: 在 `dependencies` 末尾追加测试依赖**

```xml
        <!-- 测试依赖：保持最小，不引入完整 spring-boot-starter-test -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
```

> 说明：common 里的 `@EnableConfigurationProperties` 测试需要 Spring Boot Test 的部分支持，因此引入 `spring-boot-starter-test`，但排除 vintage 引擎避免冲突。

- [ ] **Step 2: 编译 common**

Run: `mvn -q -pl common compile test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add common/pom.xml
git commit -m "chore(test): common 模块增加最小测试依赖"
```

---

## Task 3: 各服务 `pom.xml` 增加测试依赖

**Files:**
- Modify: `schema-service/pom.xml`, `query-service/pom.xml`, `ai-service/pom.xml`, `gateway-service/pom.xml`

**Interfaces:**
- Consumes: 根 pom 中的 `spring-boot-starter-test` 版本
- Produces: 各服务可进行 `@WebMvcTest` / `@SpringBootTest`

- [ ] **Step 1: 在四个服务的 `pom.xml` 中，`lombok` 依赖之后追加**

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: 编译四个服务**

Run: `mvn -q -pl schema-service,query-service,ai-service,gateway-service -am test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add schema-service/pom.xml query-service/pom.xml ai-service/pom.xml gateway-service/pom.xml
git commit -m "chore(test): 各服务增加 spring-boot-starter-test 与 h2 依赖"
```

---

## Task 4: `common` 单元测试 — `R` / `ResultCode` / `BaseException` / `PageResult`

**Files:**
- Create: `common/src/test/java/com/nl2sql/common/RTest.java`
- Create: `common/src/test/java/com/nl2sql/common/enums/ResultCodeTest.java`
- Create: `common/src/test/java/com/nl2sql/common/exception/BaseExceptionTest.java`
- Create: `common/src/test/java/com/nl2sql/common/PageResultTest.java`

**Interfaces:**
- Consumes: `R.java`, `ResultCode.java`, `BaseException.java`, `PageResult.java`
- Produces: 4 个可运行的单元测试类

- [ ] **Step 1: 创建 `RTest.java`**

```java
package com.nl2sql.common;

import com.nl2sql.common.enums.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("R - 统一响应封装")
class RTest {

    @Test
    @DisplayName("ok() 应返回 code=200, message=success, data=null")
    void shouldReturnSuccessWithoutData() {
        R<Void> r = R.ok();
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMessage()).isEqualTo("success");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("ok(data) 应携带数据")
    void shouldReturnSuccessWithData() {
        R<String> r = R.ok("hello");
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMessage()).isEqualTo("success");
        assertThat(r.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("error(message) 应返回 code=500")
    void shouldReturnDefaultError() {
        R<Void> r = R.error("fail");
        assertThat(r.getCode()).isEqualTo(500);
        assertThat(r.getMessage()).isEqualTo("fail");
        assertThat(r.getData()).isNull();
    }

    @Test
    @DisplayName("error(code, message) 应返回自定义错误码")
    void shouldReturnErrorWithCustomCode() {
        R<Void> r = R.error(404, "not found");
        assertThat(r.getCode()).isEqualTo(404);
        assertThat(r.getMessage()).isEqualTo("not found");
    }

    @Test
    @DisplayName("error(IResultCode) 应从结果码构造错误响应")
    void shouldReturnErrorFromResultCode() {
        R<Void> r = R.error(ResultCode.BAD_REQUEST);
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMessage()).isEqualTo("请求参数错误");
    }
}
```

- [ ] **Step 2: 创建 `ResultCodeTest.java`**

```java
package com.nl2sql.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResultCode - 通用错误码")
class ResultCodeTest {

    @Test
    @DisplayName("所有枚举应携带正确的 code、i18nKey、desc")
    void shouldHaveCorrectMetadata() {
        assertThat(ResultCode.SUCCESS.getCode()).isEqualTo(200);
        assertThat(ResultCode.SUCCESS.getI18nKey()).isEqualTo("result.success");
        assertThat(ResultCode.SUCCESS.getDesc()).isEqualTo("操作成功");

        assertThat(ResultCode.BAD_REQUEST.getCode()).isEqualTo(400);
        assertThat(ResultCode.BAD_REQUEST.getI18nKey()).isEqualTo("result.bad_request");
        assertThat(ResultCode.BAD_REQUEST.getDesc()).isEqualTo("请求参数错误");

        assertThat(ResultCode.INTERNAL_ERROR.getCode()).isEqualTo(500);
        assertThat(ResultCode.SERVICE_UNAVAILABLE.getCode()).isEqualTo(503);
    }
}
```

- [ ] **Step 3: 创建 `BaseExceptionTest.java`**

```java
package com.nl2sql.common.exception;

import com.nl2sql.common.enums.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseException - 业务异常父类")
class BaseExceptionTest {

    @Test
    @DisplayName("以结果码构造时应取结果码描述与 code")
    void shouldBuildFromResultCode() {
        BaseException ex = new BaseException(ResultCode.BAD_REQUEST);
        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).isEqualTo("请求参数错误");
    }

    @Test
    @DisplayName("以结果码+自定义消息构造时应覆盖默认描述")
    void shouldBuildFromResultCodeWithCustomMessage() {
        BaseException ex = new BaseException(ResultCode.BAD_REQUEST, "参数不合法");
        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).isEqualTo("参数不合法");
    }

    @Test
    @DisplayName("以自定义 code+消息构造时应保留传入值")
    void shouldBuildFromCustomCode() {
        BaseException ex = new BaseException(999, "custom");
        assertThat(ex.getCode()).isEqualTo(999);
        assertThat(ex.getMessage()).isEqualTo("custom");
    }

    @Test
    @DisplayName("仅以消息构造时应使用 500 默认 code")
    void shouldUseDefault500Code() {
        BaseException ex = new BaseException("server error");
        assertThat(ex.getCode()).isEqualTo(500);
        assertThat(ex.getMessage()).isEqualTo("server error");
    }
}
```

- [ ] **Step 4: 创建 `PageResultTest.java`**

```java
package com.nl2sql.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResult - 统一分页结果")
class PageResultTest {

    @Test
    @DisplayName("of 工厂应正确计算总页数")
    void shouldCalculatePages() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 10, 1, 2);
        assertThat(result.getRecords()).containsExactly("a", "b");
        assertThat(result.getTotal()).isEqualTo(10);
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getPages()).isEqualTo(5);
    }

    @Test
    @DisplayName("empty 工厂应返回空记录")
    void shouldReturnEmptyPage() {
        PageResult<String> result = PageResult.empty(1, 10);
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("pageSize 为 0 时总页数应为 0")
    void shouldHandleZeroPageSize() {
        PageResult<String> result = PageResult.of(List.of(), 5, 1, 0);
        assertThat(result.getPages()).isEqualTo(0);
    }
}
```

- [ ] **Step 5: 运行 common 测试**

Run: `mvn -q -pl common test`
Expected: Tests run: 13, Failures: 0, Errors: 0

- [ ] **Step 6: Commit**

```bash
git add common/src/test
git commit -m "test(common): R/ResultCode/BaseException/PageResult 单元测试"
```

---

## Task 5: `common` 配置属性测试 — `CacheProperties` / `MqProperties`

**Files:**
- Create: `common/src/test/java/com/nl2sql/common/cache/CachePropertiesTest.java`
- Create: `common/src/test/java/com/nl2sql/common/mq/MqPropertiesTest.java`
- Create: `common/src/test/resources/application-test.yml`

**Interfaces:**
- Consumes: `CacheProperties.java`, `MqProperties.java`
- Produces: 2 个可运行的配置属性测试类 + 测试 YAML

- [ ] **Step 1: 创建 `common/src/test/resources/application-test.yml`**

```yaml
nl2sql:
  cache:
    key-prefix: "test:"
    default-ttl: 5m
    null-ttl: 30s
    cache-null-values: false
    ttls:
      schema:table: 30m
      ds:list: 5m
  mq:
    max-retries: 5
    initial-interval: 500
    multiplier: 1.5
    max-interval: 5000
```

- [ ] **Step 2: 创建 `CachePropertiesTest.java`**

```java
package com.nl2sql.common.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheProperties - 缓存配置属性绑定")
@SpringBootTest(classes = CacheProperties.class)
@EnableConfigurationProperties(CacheProperties.class)
@ActiveProfiles("test")
class CachePropertiesTest {

    @Autowired
    private CacheProperties properties;

    @Test
    @DisplayName("应绑定自定义 keyPrefix、defaultTtl、nullTtl、cacheNullValues")
    void shouldBindCustomValues() {
        assertThat(properties.getKeyPrefix()).isEqualTo("test:");
        assertThat(properties.getDefaultTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getNullTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.isCacheNullValues()).isFalse();
    }

    @Test
    @DisplayName("应绑定按 cacheName 的 TTL 覆盖")
    void shouldBindPerCacheTtls() {
        assertThat(properties.getTtls())
                .containsEntry("schema:table", Duration.ofMinutes(30))
                .containsEntry("ds:list", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("multiLevel 默认应关闭")
    void shouldDisableMultiLevelByDefault() {
        assertThat(properties.getMultiLevel().isEnabled()).isFalse();
        assertThat(properties.getMultiLevel().getLocalTtls()).isEmpty();
    }
}
```

- [ ] **Step 3: 创建 `MqPropertiesTest.java`**

```java
package com.nl2sql.common.mq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MqProperties - MQ 配置属性绑定")
@SpringBootTest(classes = MqProperties.class)
@EnableConfigurationProperties(MqProperties.class)
@ActiveProfiles("test")
class MqPropertiesTest {

    @Autowired
    private MqProperties properties;

    @Test
    @DisplayName("应绑定自定义重试参数")
    void shouldBindCustomValues() {
        assertThat(properties.getMaxRetries()).isEqualTo(5);
        assertThat(properties.getInitialInterval()).isEqualTo(500L);
        assertThat(properties.getMultiplier()).isEqualTo(1.5);
        assertThat(properties.getMaxInterval()).isEqualTo(5000L);
    }
}
```

- [ ] **Step 4: 运行 common 测试**

Run: `mvn -q -pl common test`
Expected: Tests run: 19, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**

```bash
git add common/src/test
git commit -m "test(common): CacheProperties/MqProperties 配置属性测试"
```

---

## Task 6: `schema-service` 测试配置 + Controller/Service 测试

**Files:**
- Create: `schema-service/src/test/resources/application-test.yml`
- Create: `schema-service/src/test/java/com/nl2sql/schema/controller/DataSourceControllerTest.java`
- Create: `schema-service/src/test/java/com/nl2sql/schema/service/DataSourceServiceTest.java`

**Interfaces:**
- Consumes: `DataSourceController.java`, `DataSourceService.java`, `DataSourceConfig.java`, `DataSourceRepository.java`
- Produces: schema-service 可独立跑通测试

- [ ] **Step 1: 创建 `schema-service/src/test/resources/application-test.yml`**

```yaml
spring:
  application:
    name: schema-service-test
  cloud:
    nacos:
      discovery:
        enabled: false
  main:
    allow-bean-definition-overriding: true
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

logging:
  level:
    root: warn
```

- [ ] **Step 2: 创建 `DataSourceControllerTest.java`**

```java
package com.nl2sql.schema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.service.DataSourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DataSourceController - 数据源接口切片测试")
@WebMvcTest(DataSourceController.class)
@ActiveProfiles("test")
class DataSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataSourceService dataSourceService;

    @Test
    @DisplayName("POST /api/schema/datasource 应返回创建后的数据源")
    void shouldCreateDataSource() throws Exception {
        DataSourceConfig config = new DataSourceConfig();
        config.setId(1L);
        config.setName("订单库");
        config.setType("mysql");

        when(dataSourceService.create(any(DataSourceConfig.class))).thenReturn(config);

        mockMvc.perform(post("/api/schema/datasource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("订单库"));
    }

    @Test
    @DisplayName("GET /api/schema/datasource/list 应返回数据源列表")
    void shouldListDataSources() throws Exception {
        DataSourceConfig config = new DataSourceConfig();
        config.setId(1L);
        config.setName("订单库");

        when(dataSourceService.list()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/schema/datasource/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("订单库"));
    }

    @Test
    @DisplayName("DELETE /api/schema/datasource/{id} 应返回成功响应")
    void shouldDeleteDataSource() throws Exception {
        mockMvc.perform(delete("/api/schema/datasource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
```

- [ ] **Step 3: 创建 `DataSourceServiceTest.java`**

```java
package com.nl2sql.schema.service;

import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.repository.DataSourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("DataSourceService - 数据源业务逻辑")
@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock
    private DataSourceRepository repository;

    @InjectMocks
    private DataSourceService service;

    @Test
    @DisplayName("create 应调用 repository.save")
    void shouldSaveWhenCreate() {
        DataSourceConfig config = new DataSourceConfig();
        config.setName("订单库");
        when(repository.save(config)).thenReturn(config);

        DataSourceConfig result = service.create(config);

        assertThat(result.getName()).isEqualTo("订单库");
        verify(repository).save(config);
    }

    @Test
    @DisplayName("list 应返回 repository.findAll 的结果")
    void shouldListAll() {
        DataSourceConfig config = new DataSourceConfig();
        config.setName("订单库");
        when(repository.findAll()).thenReturn(List.of(config));

        List<DataSourceConfig> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("订单库");
    }

    @Test
    @DisplayName("delete 应调用 repository.deleteById")
    void shouldDeleteById() {
        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("scanTables 应返回固定 Mock 表名列表")
    void shouldReturnMockTables() {
        List<String> tables = service.scanTables(1L);
        assertThat(tables).containsExactly("users", "orders", "products");
    }

    @Test
    @DisplayName("getTableDetail 应返回带字段信息的 Mock DTO")
    void shouldReturnMockTableDetail() {
        TableSchemaDTO dto = service.getTableDetail(1L, "users");

        assertThat(dto.getTableName()).isEqualTo("users");
        assertThat(dto.getColumns()).hasSize(2);
        assertThat(dto.getPrimaryKeys()).containsExactly("id");
    }
}
```

- [ ] **Step 4: 运行 schema-service 测试**

Run: `mvn -q -pl schema-service test`
Expected: Tests run: 7, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**

```bash
git add schema-service/src/test
git commit -m "test(schema): DataSourceController/DataSourceService 切片与单元测试"
```

---

## Task 7: `query-service` 测试配置 + Controller/Service 测试

**Files:**
- Create: `query-service/src/test/resources/application-test.yml`
- Create: `query-service/src/test/java/com/nl2sql/query/controller/QueryControllerTest.java`
- Create: `query-service/src/test/java/com/nl2sql/query/service/QueryServiceTest.java`

**Interfaces:**
- Consumes: `QueryController.java`, `QueryService.java`, `QueryRequest.java`, `QueryResult.java`, `QueryHistory.java`, `QueryHistoryRepository.java`
- Produces: query-service 可独立跑通测试

- [ ] **Step 1: 创建 `query-service/src/test/resources/application-test.yml`**

```yaml
spring:
  application:
    name: query-service-test
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

- [ ] **Step 2: 创建 `QueryControllerTest.java`**

```java
package com.nl2sql.query.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.common.PageResult;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.service.QueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("QueryController - 查询接口切片测试")
@WebMvcTest(QueryController.class)
@ActiveProfiles("test")
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QueryService queryService;

    @Test
    @DisplayName("POST /api/query/nl 应返回 NL 查询结果")
    void shouldQueryByNaturalLanguage() throws Exception {
        QueryRequest request = new QueryRequest();
        request.setUserId(1L);
        request.setDataSourceId(1L);
        request.setNaturalLanguage("本月销售额");

        QueryResult result = new QueryResult();
        result.setSql("SELECT * FROM orders LIMIT 100;");
        result.setData(List.of(Map.of("id", 1)));
        result.setTotalCount(1);

        when(queryService.queryByNaturalLanguage(any(QueryRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/query/nl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sql").value("SELECT * FROM orders LIMIT 100;"));
    }

    @Test
    @DisplayName("POST /api/query/sql 应返回 SQL 执行结果")
    void shouldQueryBySql() throws Exception {
        QueryResult result = new QueryResult();
        result.setSql("SELECT 1");
        result.setTotalCount(1);

        when(queryService.queryBySql("SELECT 1")).thenReturn(result);

        mockMvc.perform(post("/api/query/sql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("SELECT 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sql").value("SELECT 1"));
    }

    @Test
    @DisplayName("GET /api/query/history 应返回历史列表")
    void shouldListHistory() throws Exception {
        QueryHistory history = new QueryHistory();
        history.setId(1L);
        history.setNaturalLanguage("本月销售额");

        when(queryService.history("conv-1")).thenReturn(List.of(history));

        mockMvc.perform(get("/api/query/history")
                        .param("conversationId", "conv-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].naturalLanguage").value("本月销售额"));
    }

    @Test
    @DisplayName("GET /api/query/history/page 应返回分页历史")
    void shouldPageHistory() throws Exception {
        PageResult<QueryHistory> pageResult = PageResult.of(List.of(), 0, 1, 10);

        when(queryService.historyPage("conv-1", 1, 10)).thenReturn(pageResult);

        mockMvc.perform(get("/api/query/history/page")
                        .param("conversationId", "conv-1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
```

- [ ] **Step 3: 创建 `QueryServiceTest.java`**

```java
package com.nl2sql.query.service;

import com.nl2sql.common.PageResult;
import com.nl2sql.common.mq.MqConst;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.repository.QueryHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("QueryService - 查询业务逻辑")
@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private QueryHistoryRepository historyRepository;

    @InjectMocks
    private QueryService queryService;

    @Test
    @DisplayName("queryByNaturalLanguage 应发送 NL2SQLEvent 并保存历史")
    void shouldSendEventAndSaveHistory() {
        QueryRequest request = new QueryRequest();
        request.setUserId(1L);
        request.setDataSourceId(1L);
        request.setNaturalLanguage("本月销售额");
        request.setConversationId("conv-1");

        QueryResult result = queryService.queryByNaturalLanguage(request);

        assertThat(result.getSql()).isEqualTo("SELECT * FROM orders LIMIT 100;");
        assertThat(result.getTotalCount()).isEqualTo(2);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(MqConst.Nl2Sql.EXCHANGE), eq(MqConst.Nl2Sql.ROUTING_KEY), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).hasFieldOrPropertyWithValue("naturalLanguage", "本月销售额");

        verify(historyRepository).save(any(QueryHistory.class));
    }

    @Test
    @DisplayName("queryBySql 应直接返回 Mock 结果")
    void shouldReturnMockForSql() {
        QueryResult result = queryService.queryBySql("SELECT 1");
        assertThat(result.getSql()).isEqualTo("SELECT 1");
        assertThat(result.getData()).containsExactly(Map.of("result", "mock"));
    }

    @Test
    @DisplayName("history 应调用 repository 按 conversationId 查询")
    void shouldFindHistoryByConversationId() {
        QueryHistory history = new QueryHistory();
        history.setConversationId("conv-1");
        when(historyRepository.findByConversationIdOrderByCreatedAtDesc("conv-1"))
                .thenReturn(List.of(history));

        List<QueryHistory> result = queryService.history("conv-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConversationId()).isEqualTo("conv-1");
    }

    @Test
    @DisplayName("historyPage 应返回正确分页结构")
    void shouldReturnHistoryPage() {
        QueryHistory history = new QueryHistory();
        history.setId(1L);
        when(historyRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv-1"), any()))
                .thenReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 10), 1));

        PageResult<QueryHistory> result = queryService.historyPage("conv-1", 1, 10);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getRecords()).hasSize(1);
    }
}
```

- [ ] **Step 4: 运行 query-service 测试**

Run: `mvn -q -pl query-service test`
Expected: Tests run: 8, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**

```bash
git add query-service/src/test
git commit -m "test(query): QueryController/QueryService 切片与单元测试"
```

---

## Task 8: `ai-service` 测试配置 + Controller/Service 测试

**Files:**
- Create: `ai-service/src/test/resources/application-test.yml`
- Create: `ai-service/src/test/java/com/nl2sql/ai/controller/AiControllerTest.java`
- Create: `ai-service/src/test/java/com/nl2sql/ai/service/MockLLMServiceTest.java`

**Interfaces:**
- Consumes: `AiController.java`, `MockLLMService.java`, `ConvertRequest.java`
- Produces: ai-service 可独立跑通测试

- [ ] **Step 1: 创建 `ai-service/src/test/resources/application-test.yml`**

```yaml
spring:
  application:
    name: ai-service-test
  cloud:
    nacos:
      discovery:
        enabled: false
  main:
    allow-bean-definition-overriding: true
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration

logging:
  level:
    root: warn
```

- [ ] **Step 2: 创建 `AiControllerTest.java`**

```java
package com.nl2sql.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.ai.dto.ConvertRequest;
import com.nl2sql.ai.service.MockLLMService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AiController - AI 接口切片测试")
@WebMvcTest(AiController.class)
@ActiveProfiles("test")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MockLLMService mockLLMService;

    @Test
    @DisplayName("POST /api/ai/convert 应返回转换后的 SQL")
    void shouldConvertNaturalLanguage() throws Exception {
        ConvertRequest request = new ConvertRequest();
        request.setDataSourceId(1L);
        request.setNaturalLanguage("本月销售额");

        when(mockLLMService.convert("本月销售额", 1L))
                .thenReturn("SELECT product_name, SUM(amount) FROM orders GROUP BY product_name");

        mockMvc.perform(post("/api/ai/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data")
                        .value("SELECT product_name, SUM(amount) FROM orders GROUP BY product_name"));
    }

    @Test
    @DisplayName("POST /api/ai/validate 应校验 SQL 是否以 SELECT 开头")
    void shouldValidateSelectSql() throws Exception {
        mockMvc.perform(post("/api/ai/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("SELECT * FROM users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /api/ai/validate 对非 SELECT 语句应返回 false")
    void shouldInvalidateNonSelectSql() throws Exception {
        mockMvc.perform(post("/api/ai/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("DELETE FROM users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }
}
```

- [ ] **Step 3: 创建 `MockLLMServiceTest.java`**

```java
package com.nl2sql.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockLLMService - Mock LLM 转换逻辑")
class MockLLMServiceTest {

    private final MockLLMService service = new MockLLMService();

    @ParameterizedTest(name = "[{0}] 应转换为 [{1}]")
    @CsvSource({
            "本月销售, SELECT product_name, SUM(amount) AS total_sales FROM orders GROUP BY product_name ORDER BY total_sales DESC LIMIT 10;",
            "sales report, SELECT product_name, SUM(amount) AS total_sales FROM orders GROUP BY product_name ORDER BY total_sales DESC LIMIT 10;",
            "用户总数, SELECT COUNT(*) AS user_count FROM users;",
            "user count, SELECT COUNT(*) AS user_count FROM users;",
            "随便查一下, SELECT * FROM orders LIMIT 100;"
    })
    @DisplayName("convert 应根据关键词返回对应 SQL")
    void shouldConvertByKeyword(String naturalLanguage, String expectedSql) {
        String sql = service.convert(naturalLanguage, 1L);
        assertThat(sql).isEqualTo(expectedSql);
    }
}
```

- [ ] **Step 4: 运行 ai-service 测试**

Run: `mvn -q -pl ai-service test`
Expected: Tests run: 6, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**

```bash
git add ai-service/src/test
git commit -m "test(ai): AiController/MockLLMService 切片与单元测试"
```

---

## Task 9: `gateway-service` 上下文加载测试

**Files:**
- Create: `gateway-service/src/test/resources/application-test.yml`
- Create: `gateway-service/src/test/java/com/nl2sql/gateway/GatewayApplicationTest.java`

**Interfaces:**
- Consumes: `GatewayApplication.java`
- Produces: gateway-service 可加载 Spring 上下文

- [ ] **Step 1: 创建 `gateway-service/src/test/resources/application-test.yml`**

```yaml
spring:
  application:
    name: gateway-service-test
  cloud:
    nacos:
      discovery:
        enabled: false
    gateway:
      discovery:
        locator:
          enabled: false
  main:
    web-application-type: reactive

logging:
  level:
    root: warn
```

- [ ] **Step 2: 创建 `GatewayApplicationTest.java`**

```java
package com.nl2sql.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("GatewayApplication - 网关上下文加载")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class GatewayApplicationTest {

    @Test
    @DisplayName("应能正常加载 Spring 上下文")
    void contextLoads() {
        // 只要上下文启动成功即通过
    }
}
```

- [ ] **Step 3: 运行 gateway-service 测试**

Run: `mvn -q -pl gateway-service test`
Expected: Tests run: 1, Failures: 0, Errors: 0

- [ ] **Step 4: Commit**

```bash
git add gateway-service/src/test
git commit -m "test(gateway): Gateway 上下文加载测试"
```

---

## Task 10: 全项目 `mvn test` 验证

**Files:**
- 无新增文件

**Interfaces:**
- Consumes: 前面所有任务产出的测试代码
- Produces: 全模块测试通过结论

- [ ] **Step 1: 执行全项目测试**

Run: `mvn -q test`
Expected: 5 个模块全部 BUILD SUCCESS，总测试数 ≥ 41

- [ ] **Step 2: 如失败，按模块单独排查**

Run: `mvn -pl <module> test` 逐个模块定位失败用例。
常见阻塞点：
- Nacos 未关闭 → 检查 `application-test.yml` 中 `nacos.discovery.enabled: false`。
- Redis/RabbitMQ 自动配置连接失败 → 检查 `autoconfigure.exclude`。
- Gateway 端口冲突 → 使用 `WebEnvironment.MOCK`。

- [ ] **Step 3: Commit（如有修复）**

```bash
git add -A
git commit -m "test: 全模块测试基础设施骨架补齐"
```

---

## Self-Review Checklist

- [ ] Spec 覆盖：所有 A 阶段清单条目均已对应到 Task 4-9。
- [ ] Placeholder 扫描：无 TBD/TODO/"实现 later"/"类似 Task N"。
- [ ] 类型一致性：所有 mock 类型、方法签名与源码一致。
- [ ] 路径正确：所有文件路径均为绝对项目内路径。
- [ ] 命令可执行：所有 Maven 命令可在项目根目录直接运行。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-21-testing-infrastructure-plan.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** - Dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.

Which approach?
