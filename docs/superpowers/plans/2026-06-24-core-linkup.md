# 核心链路打通（NL → SQL → 真实执行）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 NL2SQL 主链路从两端 mock 改造为真实可用——ai-service 接 MiniMax 生成 SQL，query-service 真实连接目标库执行 SQL 并返回真实结果。

**Architecture:** 同步 Feign 链路打通：`query-service →(Feign)→ ai-service.convert →(Feign)→ schema-service 取 schema 上下文 + MiniMax LLM → SQL`；`query-service →(Feign)→ schema-service 取连接信息 → DriverManager 执行 → 真实结果`。MQ 异步链路保留不动。跨服务共享 DTO 与 Feign 客户端接口集中在 `common`。

**Tech Stack:** Spring Boot 3.2.5 / Spring Cloud 2023.0.0 / Spring Cloud OpenFeign（新引入）/ Java 17 / RestClient / MySQL JDBC / H2（测试）/ JUnit5 + Mockito + AssertJ。

**关联设计：** [2026-06-24-core-linkup-design.md](../specs/2026-06-24-core-linkup-design.md)

---

## 全局约定

- **包根**：`com.nl2sql.common.*`、`com.nl2sql.{ai,query,schema}.*`
- **服务名**（Feign `name`）：`ai-service` / `query-service` / `schema-service`（即各服务 `spring.application.name`）
- **错误码 band**：ai 12xxx、query 13xxx（schema 既存 11xxx，common HTTP 语义码 2xx/4xx/5xx）
- **测试模式**：Controller 用 `@WebMvcTest` + `@MockBean` + MockMvc；Service 用 `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` + AssertJ；均 `@ActiveProfiles("test")`。slice 测试不启动服务发现，故不依赖 Nacos。
- **配置属性类**手写 getter/setter（跟随 `CacheProperties` 风格）；DTO 用 Lombok `@Data`（跟随 `event` 包风格）。
- 每个 Task 结尾 `git commit`；提交前对应模块 `mvn -q -pl <module> -am compile` 通过。

---

## Phase 1 — common：共享契约层（DTO + Feign 接口 + 错误码 + i18n）

### Task 1：common 引入 OpenFeign 依赖（optional）

**Files:**
- Modify: `common/pom.xml`

- [ ] **Step 1：在 `common/pom.xml` 的 `<dependencies>` 段（`spring-boot-starter-amqp` 依赖之后）新增 OpenFeign，标 optional**

```xml
        <!-- OpenFeign：声明式跨服务调用，optional，仅业务服务激活 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
            <optional>true</optional>
        </dependency>
```

- [ ] **Step 2：编译 common 验证依赖可解析（版本由 spring-cloud-dependencies BOM 管理）**

Run: `mvn -q -pl common -am compile`
Expected: BUILD SUCCESS

- [ ] **Step 3：Commit**

```bash
git add common/pom.xml
git commit -m "build(common): 引入 spring-cloud-starter-openfeign（optional）"
```

---

### Task 2：common 共享 DTO（ConvertRequest / DataSourceConnectionDTO / SchemaContextDTO）

**Files:**
- Create: `common/src/main/java/com/nl2sql/common/dto/ConvertRequest.java`
- Create: `common/src/main/java/com/nl2sql/common/dto/DataSourceConnectionDTO.java`
- Create: `common/src/main/java/com/nl2sql/common/dto/SchemaContextDTO.java`

- [ ] **Step 1：新建 `ConvertRequest`（跨服务 NL→SQL 请求体，替换原 ai-service 内的同名 DTO）**

```java
package com.nl2sql.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * NL → SQL 转换请求。ai-service 与 query-service 共享，供 Feign 调用与控制器入参统一。
 */
@Data
public class ConvertRequest implements Serializable {

    /** 目标数据源 ID */
    @NotNull(message = "数据源 ID 不能为空")
    private Long dataSourceId;

    /** 自然语言查询内容 */
    @NotBlank(message = "自然语言查询内容不能为空")
    private String naturalLanguage;

    /** 目标库名：缺省时由调用方按数据源唯一库解析，多库时必填 */
    private String databaseName;
}
```

- [ ] **Step 2：新建 `DataSourceConnectionDTO`（schema-service 内部返回的目标库连接信息，密码为服务内解密后的明文，仅内网传输）**

```java
package com.nl2sql.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 目标数据源连接信息。schema-service 解密后经内网 Feign 返回，供 query-service 建连执行。
 * password 为明文，仅存活于内存与内网传输，不落库、不打日志。
 */
@Data
public class DataSourceConnectionDTO implements Serializable {

    /** 数据库类型，取 {@code DataSourceConfig.type}（如 "mysql"） */
    private String type;

    /** 主机 */
    private String host;

    /** 端口 */
    private Integer port;

    /** 该数据源关联的库名列表 */
    private List<String> databaseNames;

    /** 连接用户名 */
    private String username;

    /** 已解密的明文密码 */
    private String password;
}
```

- [ ] **Step 3：新建 `SchemaContextDTO`（喂给 LLM 的精简表结构）**

```java
package com.nl2sql.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 精简 schema 上下文，供 LLM 生成 SQL。由 schema-service 从已持久化的 schema_cache 组装。
 */
@Data
public class SchemaContextDTO implements Serializable {

    /** 库名 */
    private String database;

    /** 表清单 */
    private List<TableBrief> tables = new ArrayList<>();

    /** 单表摘要 */
    @Data
    public static class TableBrief implements Serializable {
        /** 表名 */
        private String tableName;
        /** 表注释 */
        private String tableComment;
        /** 列清单 */
        private List<ColumnBrief> columns = new ArrayList<>();
    }

    /** 列摘要 */
    @Data
    public static class ColumnBrief implements Serializable {
        /** 列名 */
        private String name;
        /** 方言原文类型，如 varchar(255) */
        private String type;
        /** 列注释 */
        private String comment;
    }
}
```

- [ ] **Step 4：编译 common**

Run: `mvn -q -pl common -am compile`
Expected: BUILD SUCCESS

- [ ] **Step 5：Commit**

```bash
git add common/src/main/java/com/nl2sql/common/dto/
git commit -m "feat(common): 新增跨服务共享 DTO（ConvertRequest/DataSourceConnectionDTO/SchemaContextDTO）"
```

---

### Task 3：common Feign 客户端接口（SchemaServiceClient / AiServiceClient）

**Files:**
- Create: `common/src/main/java/com/nl2sql/common/feign/SchemaServiceClient.java`
- Create: `common/src/main/java/com/nl2sql/common/feign/AiServiceClient.java`

- [ ] **Step 1：新建 `SchemaServiceClient`（调 schema-service 内部接口）**

```java
package com.nl2sql.common.feign;

import com.nl2sql.common.R;
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.SchemaContextDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * schema-service 内部接口客户端。仅供服务间调用，对应 /api/schema/internal/**。
 */
@FeignClient(name = "schema-service", path = "/api/schema/internal")
public interface SchemaServiceClient {

    /** 取目标数据源解密后的连接信息 */
    @GetMapping("/datasource/{id}/connection")
    R<DataSourceConnectionDTO> getConnection(@PathVariable("id") Long id);

    /** 取指定库的精简 schema 上下文 */
    @GetMapping("/datasource/{id}/schema")
    R<SchemaContextDTO> getSchema(@PathVariable("id") Long id, @RequestParam("database") String database);
}
```

- [ ] **Step 2：新建 `AiServiceClient`（调 ai-service NL→SQL）**

```java
package com.nl2sql.common.feign;

import com.nl2sql.common.R;
import com.nl2sql.common.dto.ConvertRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * ai-service NL → SQL 客户端。
 */
@FeignClient(name = "ai-service", path = "/api/ai")
public interface AiServiceClient {

    @PostMapping("/convert")
    R<String> convert(@RequestBody ConvertRequest request);
}
```

- [ ] **Step 3：编译 common**

Run: `mvn -q -pl common -am compile`
Expected: BUILD SUCCESS

- [ ] **Step 4：Commit**

```bash
git add common/src/main/java/com/nl2sql/common/feign/
git commit -m "feat(common): 新增 SchemaServiceClient/AiServiceClient Feign 接口"
```

---

### Task 4：AiResultCode / QueryResultCode + i18n 文案

**Files:**
- Create: `ai-service/src/main/java/com/nl2sql/ai/exception/AiResultCode.java`
- Create: `query-service/src/main/java/com/nl2sql/query/exception/QueryResultCode.java`
- Modify: `common/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `common/src/main/resources/i18n/messages_en.properties`

- [ ] **Step 1：新建 `AiResultCode`（12xxx band）**

```java
package com.nl2sql.ai.exception;

import com.nl2sql.common.enums.IResultCode;

/**
 * ai-service 领域错误码。编码用 12xxx band，避开 common HTTP 语义区间与其他服务 band。
 */
public enum AiResultCode implements IResultCode {

    AI_LLM_UNAVAILABLE(12001, "ai.llm_unavailable", "AI 服务暂不可用");

    private final Integer code;
    private final String i18nKey;
    private final String desc;

    AiResultCode(Integer code, String i18nKey, String desc) {
        this.code = code;
        this.i18nKey = i18nKey;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public String getI18nKey() {
        return i18nKey;
    }
}
```

- [ ] **Step 2：新建 `QueryResultCode`（13xxx band）**

```java
package com.nl2sql.query.exception;

import com.nl2sql.common.enums.IResultCode;

/**
 * query-service 领域错误码。编码用 13xxx band。
 */
public enum QueryResultCode implements IResultCode {

    QUERY_DB_TYPE_UNSUPPORTED(13001, "query.db_type_unsupported", "暂不支持该数据库类型"),
    QUERY_DATASOURCE_CONNECT_FAILED(13002, "query.datasource_connect_failed", "无法连接目标数据库"),
    QUERY_SQL_EXECUTE_FAILED(13003, "query.sql_execute_failed", "SQL 执行失败"),
    QUERY_DATABASE_NOT_SPECIFIED(13004, "query.database_not_specified", "数据源含多个库，需指定 databaseName");

    private final Integer code;
    private final String i18nKey;
    private final String desc;

    QueryResultCode(Integer code, String i18nKey, String desc) {
        this.code = code;
        this.i18nKey = i18nKey;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public String getI18nKey() {
        return i18nKey;
    }
}
```

- [ ] **Step 3：在 `messages_zh_CN.properties` 末尾追加（`schema.scan_execute_failed=扫描执行失败` 这一行之后）**

```properties
ai.llm_unavailable=AI 服务暂不可用
query.db_type_unsupported=暂不支持该数据库类型
query.datasource_connect_failed=无法连接目标数据库
query.sql_execute_failed=SQL 执行失败
query.database_not_specified=数据源含多个库，需指定 databaseName
```

- [ ] **Step 4：在 `messages_en.properties` 末尾追加（`schema.scan_execute_failed=Schema scan failed` 这一行之后）**

```properties
ai.llm_unavailable=AI service is temporarily unavailable
query.db_type_unsupported=Unsupported database type
query.datasource_connect_failed=Cannot connect to target database
query.sql_execute_failed=SQL execution failed
query.database_not_specified=Data source has multiple databases; databaseName is required
```

- [ ] **Step 5：编译三个服务验证错误码可解析**

Run: `mvn -q -pl common,ai-service,query-service -am compile`
Expected: BUILD SUCCESS

- [ ] **Step 6：Commit**

```bash
git add ai-service/src/main/java/com/nl2sql/ai/exception/ \
        query-service/src/main/java/com/nl2sql/query/exception/ \
        common/src/main/resources/i18n/
git commit -m "feat(common,ai,query): 新增 ai/query 领域错误码与 i18n 文案"
```

---

## Phase 2 — schema-service：内部接口（连接信息 / 精简 schema）

### Task 5：DataSourceService 增 getConnection / getSchemaContext

**Files:**
- Modify: `schema-service/src/main/java/com/nl2sql/schema/service/DataSourceService.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/service/DataSourceServiceInternalTest.java`

- [ ] **Step 1：在 `DataSourceService` 顶部 import 段补充**

```java
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.common.encrypt.SecureConfigEncryptor;
import com.nl2sql.schema.scanner.model.ColumnMetadata;
```
（`ColumnMetadata` 可能已 import；若已存在则跳过该行。）

- [ ] **Step 2：写失败测试 `DataSourceServiceInternalTest`**

```java
package com.nl2sql.schema.service;

import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("DataSourceService - 内部接口（连接信息/精简 schema）")
@ExtendWith(MockitoExtension.class)
class DataSourceServiceInternalTest {

    @Mock private DataSourceRepository repository;
    @Mock private SchemaCacheRepository schemaCacheRepository;
    @Mock private TableListCacheRepository tableListCacheRepository;
    private DataSourceService service;

    @BeforeEach
    void setUp() {
        // 用真实 ObjectMapper 反序列化 schema_cache 的 columnJson，避免 mock TypeReference 的引用相等匹配脆弱
        service = new DataSourceService(repository, schemaCacheRepository, tableListCacheRepository, new ObjectMapper());
    }

    private DataSourceConfig ds(long id) {
        DataSourceConfig d = new DataSourceConfig();
        d.setId(id);
        d.setType("mysql");
        d.setHost("h");
        d.setPort(3306);
        d.setDatabaseNames(List.of("shop"));
        d.setUsername("u");
        d.setPasswordEncrypted("plainpwd"); // 非 ENC(...)，直接当明文
        return d;
    }

    @Test
    @DisplayName("getConnection 应返回解密后的连接信息（非 ENC 密文按明文返回）")
    void shouldReturnConnectionWithPlaintextPassword() {
        when(repository.findById(1L)).thenReturn(Optional.of(ds(1L)));
        DataSourceConnectionDTO conn = service.getConnection(1L);
        assertThat(conn.getHost()).isEqualTo("h");
        assertThat(conn.getType()).isEqualTo("mysql");
        assertThat(conn.getDatabaseNames()).containsExactly("shop");
        assertThat(conn.getPassword()).isEqualTo("plainpwd");
    }

    @Test
    @DisplayName("getConnection 数据源不存在应抛 DATASOURCE_NOT_FOUND")
    void shouldThrowWhenDatasourceMissing() {
        when(repository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getConnection(2L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(SchemaResultCode.DATASOURCE_NOT_FOUND.getDesc());
    }

    @Test
    @DisplayName("getSchemaContext 应把 schema_cache 组装为精简 schema")
    void shouldBuildContextFromCache() throws Exception {
        SchemaCache c = new SchemaCache();
        c.setTableName("orders");
        c.setTableComment("订单表");
        c.setColumnJson("[{\"name\":\"id\",\"type\":\"bigint\",\"comment\":\"主键\"}]");
        when(schemaCacheRepository.findByDataSourceIdAndDatabaseName(1L, "shop"))
                .thenReturn(List.of(c));

        SchemaContextDTO ctx = service.getSchemaContext(1L, "shop");
        assertThat(ctx.getDatabase()).isEqualTo("shop");
        assertThat(ctx.getTables()).hasSize(1);
        assertThat(ctx.getTables().get(0).getTableName()).isEqualTo("orders");
        assertThat(ctx.getTables().get(0).getColumns().get(0).getName()).isEqualTo("id");
    }
}
```

- [ ] **Step 3：运行测试，确认失败（方法未定义）**

Run: `mvn -q -pl schema-service -am test -Dtest=DataSourceServiceInternalTest`
Expected: 编译失败 / 方法 getConnection/getSchemaContext 未定义

- [ ] **Step 4：在 `DataSourceService` 内实现两个方法（放在 `getTableDetail` 之后、私有 `toDto` 之前）**

```java
    /** 内部接口：返回解密后的目标库连接信息，供 query-service 建连执行。 */
    public DataSourceConnectionDTO getConnection(Long dataSourceId) {
        DataSourceConfig ds = repository.findById(dataSourceId)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));
        DataSourceConnectionDTO dto = new DataSourceConnectionDTO();
        dto.setType(ds.getType());
        dto.setHost(ds.getHost());
        dto.setPort(ds.getPort());
        dto.setDatabaseNames(ds.getDatabaseNames());
        dto.setUsername(ds.getUsername());
        dto.setPassword(decryptPassword(ds.getPasswordEncrypted()));
        return dto;
    }

    /** 内部接口：从已持久化的 schema_cache 组装精简 schema，供 ai-service 喂 LLM。 */
    public SchemaContextDTO getSchemaContext(Long dataSourceId, String databaseName) {
        List<SchemaCache> caches = schemaCacheRepository
                .findByDataSourceIdAndDatabaseName(dataSourceId, databaseName);
        SchemaContextDTO dto = new SchemaContextDTO();
        dto.setDatabase(databaseName);
        dto.setTables(caches.stream().map(this::toTableBrief).toList());
        return dto;
    }

    private SchemaContextDTO.TableBrief toTableBrief(SchemaCache cache) {
        SchemaContextDTO.TableBrief brief = new SchemaContextDTO.TableBrief();
        brief.setTableName(cache.getTableName());
        brief.setTableComment(cache.getTableComment());
        List<ColumnMetadata> cols = readList(cache.getColumnJson(),
                new TypeReference<List<ColumnMetadata>>() {});
        brief.setColumns(cols.stream().map(this::toColumnBrief).toList());
        return brief;
    }

    private SchemaContextDTO.ColumnBrief toColumnBrief(ColumnMetadata m) {
        SchemaContextDTO.ColumnBrief c = new SchemaContextDTO.ColumnBrief();
        c.setName(m.getName());
        c.setType(m.getType());
        c.setComment(m.getComment());
        return c;
    }

    private String decryptPassword(String stored) {
        if (SecureConfigEncryptor.isEncrypted(stored)) {
            return SecureConfigEncryptor.decrypt(stored, SecureConfigEncryptor.getKeyFromEnv());
        }
        return stored;
    }
```
（`readList` / `BaseException` / `SchemaResultCode` / `TypeReference` 已在该类 import 或可用，无需新增。）

- [ ] **Step 5：运行测试，确认通过**

Run: `mvn -q -pl schema-service -am test -Dtest=DataSourceServiceInternalTest`
Expected: 3 tests PASS

- [ ] **Step 6：Commit**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/service/DataSourceService.java \
        schema-service/src/test/java/com/nl2sql/schema/service/DataSourceServiceInternalTest.java
git commit -m "feat(schema): DataSourceService 增 getConnection/getSchemaContext 内部接口"
```

---

### Task 6：DataSourceController 暴露两个 /internal 接口

**Files:**
- Modify: `schema-service/src/main/java/com/nl2sql/schema/controller/DataSourceController.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/controller/DataSourceInternalControllerTest.java`

- [ ] **Step 1：写失败测试**

```java
package com.nl2sql.schema.controller;

import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.schema.service.DataSourceService;
import com.nl2sql.schema.service.SchemaScanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DataSourceController - 内部接口")
@WebMvcTest(DataSourceController.class)
@ActiveProfiles("test")
class DataSourceInternalControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DataSourceService service;
    @MockBean private SchemaScanService scanService;

    @Test
    @DisplayName("GET /api/schema/internal/datasource/{id}/connection 应返回连接信息")
    void shouldReturnConnection() throws Exception {
        DataSourceConnectionDTO dto = new DataSourceConnectionDTO();
        dto.setHost("h"); dto.setPort(3306); dto.setType("mysql");
        when(service.getConnection(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/schema/internal/datasource/1/connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.host").value("h"));
    }

    @Test
    @DisplayName("GET /api/schema/internal/datasource/{id}/schema 应返回精简 schema")
    void shouldReturnSchema() throws Exception {
        SchemaContextDTO ctx = new SchemaContextDTO();
        ctx.setDatabase("shop");
        ctx.setTables(List.of());
        when(service.getSchemaContext(1L, "shop")).thenReturn(ctx);

        mockMvc.perform(get("/api/schema/internal/datasource/1/schema").param("database", "shop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.database").value("shop"));
    }
}
```

- [ ] **Step 2：运行测试，确认失败（接口不存在 → 404）**

Run: `mvn -q -pl schema-service -am test -Dtest=DataSourceInternalControllerTest`
Expected: 2 tests FAIL（404）

- [ ] **Step 3：在 `DataSourceController` 增两个接口（import `DataSourceConnectionDTO` / `SchemaContextDTO`，放在 `tableDetail` 之后）**

```java
    /** 内部接口：返回解密后的连接信息（仅服务间调用，鉴权后置）。 */
    @GetMapping("/internal/datasource/{id}/connection")
    public R<DataSourceConnectionDTO> connection(@PathVariable Long id) {
        return R.ok(service.getConnection(id));
    }

    /** 内部接口：返回精简 schema 上下文（仅服务间调用，鉴权后置）。 */
    @GetMapping("/internal/datasource/{id}/schema")
    public R<SchemaContextDTO> schema(@PathVariable Long id, @RequestParam String database) {
        return R.ok(service.getSchemaContext(id, database));
    }
```
（补充 import `org.springframework.web.bind.annotation.RequestParam` 若未存在。）

- [ ] **Step 4：运行测试，确认通过**

Run: `mvn -q -pl schema-service -am test -Dtest=DataSourceInternalControllerTest`
Expected: 2 tests PASS

- [ ] **Step 5：schema-service 全量测试无回归**

Run: `mvn -q -pl schema-service -am test`
Expected: BUILD SUCCESS，全部通过

- [ ] **Step 6：Commit**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/controller/DataSourceController.java \
        schema-service/src/test/java/com/nl2sql/schema/controller/DataSourceInternalControllerTest.java
git commit -m "feat(schema): 暴露 internal 连接信息/精简 schema 接口"
```

---

## Phase 3 — ai-service：接入 MiniMax LLM

### Task 7：LlmProperties + 配置

**Files:**
- Create: `ai-service/src/main/java/com/nl2sql/ai/config/LlmProperties.java`
- Create: `ai-service/src/main/java/com/nl2sql/ai/config/LlmAutoConfiguration.java`
- Modify: `ai-service/src/main/resources/application-local.yml`

- [ ] **Step 1：新建 `LlmProperties`（手写 getter/setter，跟随 `CacheProperties` 风格）**

```java
package com.nl2sql.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * LLM 可配置项，前缀 {@code nl2sql.llm}。MiniMax 走 Anthropic Messages 兼容端点。
 *
 * <pre>
 * nl2sql:
 *   llm:
 *     base-url: https://api.minimaxi.com/anthropic
 *     api-key: ENC(...)          # 或 ${MINIMAX_API_KEY}
 *     model: MiniMax-M2.7
 *     max-tokens: 1024
 *     timeout: 30s
 *     fallback-to-mock: false    # LLM 失败时是否降级到 MockLLMService
 * </pre>
 */
@ConfigurationProperties(prefix = "nl2sql.llm")
public class LlmProperties {

    /** Anthropic Messages 兼容端点 baseUrl */
    private String baseUrl = "https://api.minimaxi.com/anthropic";

    /** API Key，支持 ENC(...) 加密密文（启动时自动解密） */
    private String apiKey = "";

    /** 模型名 */
    private String model = "MiniMax-M2.7";

    /** 最大生成 token 数 */
    private int maxTokens = 1024;

    /** 请求超时 */
    private Duration timeout = Duration.ofSeconds(30);

    /** LLM 失败时是否降级到 MockLLMService */
    private boolean fallbackToMock = false;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public boolean isFallbackToMock() { return fallbackToMock; }
    public void setFallbackToMock(boolean fallbackToMock) { this.fallbackToMock = fallbackToMock; }
}
```

- [ ] **Step 2：新建 `LlmAutoConfiguration`（注册 properties + RestClient bean）**

```java
package com.nl2sql.ai.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** 注册 LLM 配置与 RestClient。 */
@AutoConfiguration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmAutoConfiguration {

    @Bean
    public RestClient llmRestClient(LlmProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration t = properties.getTimeout();
        factory.setConnectTimeout((int) t.toMillis());
        factory.setReadTimeout((int) t.toMillis());
        return RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }
}
```

- [ ] **Step 3：注册自动装配。创建 `ai-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`**

```
com.nl2sql.ai.config.LlmAutoConfiguration
```
（若该文件已存在，则追加该行。）

- [ ] **Step 4：在 `application-local.yml` 的 `nl2sql:` 段下追加 llm 配置**

```yaml
nl2sql:
  mq:
    max-retries: 3
    initial-interval: 1000
    multiplier: 2.0
    max-interval: 10000
  llm:
    base-url: https://api.minimaxi.com/anthropic
    api-key: ${MINIMAX_API_KEY:}
    model: MiniMax-M2.7
    max-tokens: 1024
    timeout: 30s
    fallback-to-mock: false
```

- [ ] **Step 5：编译 ai-service**

Run: `mvn -q -pl ai-service -am compile`
Expected: BUILD SUCCESS

- [ ] **Step 6：Commit**

```bash
git add ai-service/src/main/java/com/nl2sql/ai/config/ \
        ai-service/src/main/resources/META-INF/ \
        ai-service/src/main/resources/application-local.yml
git commit -m "feat(ai): LlmProperties + RestClient 自动配置"
```

---

### Task 8：LlmClient 接口 + AnthropicMessagesLlmClient 实现

**Files:**
- Create: `ai-service/src/main/java/com/nl2sql/ai/llm/LlmClient.java`
- Create: `ai-service/src/main/java/com/nl2sql/ai/llm/AnthropicMessagesLlmClient.java`
- Test: `ai-service/src/test/java/com/nl2sql/ai/llm/AnthropicMessagesLlmClientTest.java`

- [ ] **Step 1：新建 `LlmClient` 接口**

```java
package com.nl2sql.ai.llm;

/** LLM 抽象，便于切换 provider 或在测试中替换为 mock。 */
public interface LlmClient {

    /**
     * 对话补全。
     *
     * @param systemPrompt 系统提示（schema 上下文 + 角色约束）
     * @param userPrompt   用户自然语言问句
     * @return LLM 生成的文本（期望为 SQL）
     */
    String chat(String systemPrompt, String userPrompt);
}
```

- [ ] **Step 2：写失败测试（用 MockRestServiceServer 校验请求构造与响应解析）**

```java
package com.nl2sql.ai.llm;

import com.nl2sql.ai.config.LlmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("AnthropicMessagesLlmClient - 请求构造与响应解析")
class AnthropicMessagesLlmClientTest {

    private AnthropicMessagesLlmClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        LlmProperties props = new LlmProperties();
        props.setBaseUrl("https://api.minimaxi.com/anthropic");
        props.setApiKey("sk-test-key");
        props.setModel("MiniMax-M2.7");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AnthropicMessagesLlmClient(builder.baseUrl(props.getBaseUrl()).build(), props);
    }

    @Test
    @DisplayName("chat 应携带 x-api-key/anthropic-version 头并解析 content[0].text")
    void shouldCallEndpointAndParseText() {
        server.expect(requestTo("https://api.minimaxi.com/anthropic/v1/messages"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-api-key", "sk-test-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(
                        "{\"content\":[{\"type\":\"text\",\"text\":\"SELECT 1;\"}]}",
                        MediaType.APPLICATION_JSON));

        String sql = client.chat("你是 SQL 生成器", "查一下销售额");

        server.verify();
        assertThat(sql).isEqualTo("SELECT 1;");
    }

    @Test
    @DisplayName("chat 收到空 content 应抛 IllegalStateException")
    void shouldThrowOnEmptyContent() {
        server.expect(requestTo("https://api.minimaxi.com/anthropic/v1/messages"))
                .andRespond(withSuccess("{\"content\":[]}", MediaType.APPLICATION_JSON));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.chat("s", "u"))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 3：运行测试，确认失败（类未定义）**

Run: `mvn -q -pl ai-service -am test -Dtest=AnthropicMessagesLlmClientTest`
Expected: 编译失败（AnthropicMessagesLlmClient 未定义）

- [ ] **Step 4：新建 `AnthropicMessagesLlmClient` 实现**

```java
package com.nl2sql.ai.llm;

import com.nl2sql.ai.config.LlmProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages 兼容端点实现（MiniMax）。POST {baseUrl}/v1/messages，
 * 携带 x-api-key + anthropic-version，响应取 content[0].text。
 */
@Component
public class AnthropicMessagesLlmClient implements LlmClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final LlmProperties properties;

    public AnthropicMessagesLlmClient(RestClient llmRestClient, LlmProperties properties) {
        this.restClient = llmRestClient;
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "max_tokens", properties.getMaxTokens(),
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );
        Map<String, Object> resp = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", properties.getApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(Map.class);
        if (resp == null) {
            throw new IllegalStateException("LLM 响应为空");
        }
        List<Map<String, Object>> content = (List<Map<String, Object>>) resp.get("content");
        if (content == null || content.isEmpty()) {
            throw new IllegalStateException("LLM 响应 content 为空");
        }
        Object text = content.get(0).get("text");
        if (text == null) {
            throw new IllegalStateException("LLM 响应 content[0].text 为空");
        }
        return text.toString();
    }
}
```

- [ ] **Step 5：运行测试，确认通过**

Run: `mvn -q -pl ai-service -am test -Dtest=AnthropicMessagesLlmClientTest`
Expected: 2 tests PASS

- [ ] **Step 6：Commit**

```bash
git add ai-service/src/main/java/com/nl2sql/ai/llm/ \
        ai-service/src/test/java/com/nl2sql/ai/llm/
git commit -m "feat(ai): LlmClient 抽象与 Anthropic Messages 实现"
```

---

### Task 9：SchemaContextBuilder（拼 DDL 摘要）

**Files:**
- Create: `ai-service/src/main/java/com/nl2sql/ai/llm/SchemaContextBuilder.java`
- Test: `ai-service/src/test/java/com/nl2sql/ai/llm/SchemaContextBuilderTest.java`

- [ ] **Step 1：写失败测试**

```java
package com.nl2sql.ai.llm;

import com.nl2sql.common.R;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.common.feign.SchemaServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("SchemaContextBuilder - DDL 摘要拼接")
@ExtendWith(MockitoExtension.class)
class SchemaContextBuilderTest {

    @Mock private SchemaServiceClient schemaServiceClient;
    @InjectMocks private SchemaContextBuilder builder;

    @Test
    @DisplayName("build 应把表/列拼成中文 schema 描述")
    void shouldBuildContextText() {
        SchemaContextDTO ctx = new SchemaContextDTO();
        ctx.setDatabase("shop");
        SchemaContextDTO.TableBrief t = new SchemaContextDTO.TableBrief();
        t.setTableName("orders");
        t.setTableComment("订单表");
        SchemaContextDTO.ColumnBrief c = new SchemaContextDTO.ColumnBrief();
        c.setName("amount"); c.setType("decimal(10,2)");
        t.getColumns().add(c);
        ctx.getTables().add(t);
        when(schemaServiceClient.getSchema(1L, "shop")).thenReturn(R.ok(ctx));

        String text = builder.build(1L, "shop");

        assertThat(text).contains("shop").contains("orders").contains("amount decimal(10,2)").contains("订单表");
    }
}
```

- [ ] **Step 2：运行测试，确认失败**

Run: `mvn -q -pl ai-service -am test -Dtest=SchemaContextBuilderTest`
Expected: 编译失败（SchemaContextBuilder 未定义）

- [ ] **Step 3：新建 `SchemaContextBuilder`**

```java
package com.nl2sql.ai.llm;

import com.nl2sql.common.R;
import com.nl2sql.common.dto.SchemaContextDTO;
import com.nl2sql.common.feign.SchemaServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/** 调 schema-service 取精简 schema，拼成喂给 LLM 的系统提示文本。 */
@Component
@RequiredArgsConstructor
public class SchemaContextBuilder {

    private static final String SYSTEM_SUFFIX =
            "\n\n请仅基于以上表结构生成 MySQL 查询 SQL，只返回 SQL 语句本身，不要解释。";

    private final SchemaServiceClient schemaServiceClient;

    /** 取 schema 并拼成 DDL 摘要；取不到时返回空串（交由上层决定是否继续）。 */
    public String build(Long dataSourceId, String databaseName) {
        R<SchemaContextDTO> resp = schemaServiceClient.getSchema(dataSourceId, databaseName);
        if (resp == null || resp.getData() == null) {
            return "";
        }
        return toText(resp.getData());
    }

    private String toText(SchemaContextDTO ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("数据库 ").append(ctx.getDatabase()).append(" 包含以下表结构：\n\n");
        for (SchemaContextDTO.TableBrief t : ctx.getTables()) {
            sb.append("表 ").append(t.getTableName());
            if (t.getTableComment() != null && !t.getTableComment().isBlank()) {
                sb.append("（").append(t.getTableComment()).append("）");
            }
            sb.append("：");
            String cols = t.getColumns().stream()
                    .map(c -> c.getName() + " " + c.getType())
                    .collect(Collectors.joining(", "));
            sb.append(cols).append("\n");
        }
        sb.append(SYSTEM_SUFFIX);
        return sb.toString();
    }
}
```

- [ ] **Step 4：运行测试，确认通过**

Run: `mvn -q -pl ai-service -am test -Dtest=SchemaContextBuilderTest`
Expected: 1 test PASS

- [ ] **Step 5：Commit**

```bash
git add ai-service/src/main/java/com/nl2sql/ai/llm/SchemaContextBuilder.java \
        ai-service/src/test/java/com/nl2sql/ai/llm/SchemaContextBuilderTest.java
git commit -m "feat(ai): SchemaContextBuilder 拼接 LLM schema 上下文"
```

---

### Task 10：Nl2SqlConvertService + ConvertRequest 迁移 + AiController 改造 + @EnableFeignClients + pom

**Files:**
- Create: `ai-service/src/main/java/com/nl2sql/ai/service/Nl2SqlConvertService.java`
- Delete: `ai-service/src/main/java/com/nl2sql/ai/dto/ConvertRequest.java`（迁移到 common.dto）
- Modify: `ai-service/src/main/java/com/nl2sql/ai/controller/AiController.java`
- Modify: `ai-service/src/main/java/com/nl2sql/ai/AiApplication.java`
- Modify: `ai-service/pom.xml`
- Modify: `ai-service/src/test/java/com/nl2sql/ai/controller/AiControllerTest.java`
- Modify: `ai-service/src/test/java/com/nl2sql/ai/service/MockLLMServiceTest.java`
- Test: `ai-service/src/test/java/com/nl2sql/ai/service/Nl2SqlConvertServiceTest.java`

- [ ] **Step 1：在 `ai-service/pom.xml` `<dependencies>` 段新增（非 optional，激活 Feign）**

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
```

- [ ] **Step 2：`AiApplication` 加 `@EnableFeignClients`，扫描 common.feign 包**

```java
package com.nl2sql.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nl2sql.common.feign")
@SpringBootApplication
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
```

- [ ] **Step 3：写失败测试 `Nl2SqlConvertServiceTest`（mock LlmClient + SchemaContextBuilder）**

```java
package com.nl2sql.ai.service;

import com.nl2sql.ai.config.LlmProperties;
import com.nl2sql.ai.llm.LlmClient;
import com.nl2sql.ai.llm.SchemaContextBuilder;
import com.nl2sql.common.dto.ConvertRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("Nl2SqlConvertService - NL→SQL 整合")
@ExtendWith(MockitoExtension.class)
class Nl2SqlConvertServiceTest {

    @Mock private LlmClient llmClient;
    @Mock private SchemaContextBuilder schemaContextBuilder;
    @Mock private MockLLMService mockLLMService;
    private final LlmProperties properties = new LlmProperties();
    @InjectMocks private Nl2SqlConvertService service;

    @BeforeEach
    void setUp() {
        properties.setFallbackToMock(false);
        service = new Nl2SqlConvertService(llmClient, schemaContextBuilder, mockLLMService, properties);
    }

    @Test
    @DisplayName("convert 应拼上下文并调用 LLM 返回 SQL")
    void shouldCallLlmWithContext() {
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("查销售额");
        req.setDatabaseName("shop");
        when(schemaContextBuilder.build(1L, "shop")).thenReturn("ctx");
        when(llmClient.chat("ctx", "查销售额")).thenReturn("SELECT * FROM orders;");

        assertThat(service.convert(req)).isEqualTo("SELECT * FROM orders;");
    }

    @Test
    @DisplayName("LLM 异常且 fallbackToMock=true 时降级到 MockLLMService")
    void shouldFallbackToMockWhenEnabled() {
        properties.setFallbackToMock(true);
        ConvertRequest req = new ConvertRequest();
        req.setDataSourceId(1L);
        req.setNaturalLanguage("销售额");
        req.setDatabaseName("shop");
        when(schemaContextBuilder.build(1L, "shop")).thenReturn("ctx");
        when(llmClient.chat("ctx", "销售额")).thenThrow(new IllegalStateException("boom"));
        when(mockLLMService.convert("销售额", 1L)).thenReturn("SELECT 1;");

        assertThat(service.convert(req)).isEqualTo("SELECT 1;");
    }
}
```

- [ ] **Step 4：运行测试，确认失败（Nl2SqlConvertService 未定义）**

Run: `mvn -q -pl ai-service -am test -Dtest=Nl2SqlConvertServiceTest`
Expected: 编译失败

- [ ] **Step 5：新建 `Nl2SqlConvertService`**

```java
package com.nl2sql.ai.service;

import com.nl2sql.ai.config.LlmProperties;
import com.nl2sql.ai.exception.AiResultCode;
import com.nl2sql.ai.llm.LlmClient;
import com.nl2sql.ai.llm.SchemaContextBuilder;
import com.nl2sql.common.dto.ConvertRequest;
import com.nl2sql.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** NL → SQL 整合：取 schema 上下文 → 调 LLM → 失败按配置降级或报错。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Nl2SqlConvertService {

    private final LlmClient llmClient;
    private final SchemaContextBuilder schemaContextBuilder;
    private final MockLLMService mockLLMService;
    private final LlmProperties properties;

    public String convert(ConvertRequest request) {
        try {
            String context = schemaContextBuilder.build(request.getDataSourceId(), request.getDatabaseName());
            return llmClient.chat(context, request.getNaturalLanguage());
        } catch (Exception e) {
            if (properties.isFallbackToMock()) {
                log.warn("LLM 调用失败，降级到 MockLLMService: {}", e.getMessage());
                return mockLLMService.convert(request.getNaturalLanguage(), request.getDataSourceId());
            }
            throw new BaseException(AiResultCode.AI_LLM_UNAVAILABLE,
                    AiResultCode.AI_LLM_UNAVAILABLE.getMessage(), e);
        }
    }
}
```

- [ ] **Step 6：删除 `ai-service/src/main/java/com/nl2sql/ai/dto/ConvertRequest.java`（已迁移到 `common.dto.ConvertRequest`）**

Run: `git rm ai-service/src/main/java/com/nl2sql/ai/dto/ConvertRequest.java`

- [ ] **Step 7：改造 `AiController`，注入 `Nl2SqlConvertService`，import 改为 `com.nl2sql.common.dto.ConvertRequest`**

```java
package com.nl2sql.ai.controller;

import com.nl2sql.ai.service.Nl2SqlConvertService;
import com.nl2sql.common.R;
import com.nl2sql.common.dto.ConvertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final Nl2SqlConvertService convertService;

    @PostMapping("/convert")
    public R<String> convert(@Valid @RequestBody ConvertRequest request) {
        return R.ok(convertService.convert(request));
    }

    @PostMapping("/validate")
    public R<Boolean> validate(@RequestBody String sql) {
        return R.ok(sql.toLowerCase().startsWith("select"));
    }
}
```

- [ ] **Step 8：更新 `AiControllerTest`——改为 mock `Nl2SqlConvertService`，import 改 `common.dto.ConvertRequest`**

把测试中：
- `@MockBean private MockLLMService mockLLMService;` → `@MockBean private Nl2SqlConvertService convertService;`
- import `com.nl2sql.ai.service.MockLLMService` → `com.nl2sql.ai.service.Nl2SqlConvertService`、`com.nl2sql.ai.dto.ConvertRequest` → `com.nl2sql.common.dto.ConvertRequest`
- `shouldConvertNaturalLanguage` 内的桩改为：
```java
        when(convertService.convert(any(com.nl2sql.common.dto.ConvertRequest.class)))
                .thenReturn("SELECT product_name, SUM(amount) FROM orders GROUP BY product_name");
```
（补 import `static org.mockito.ArgumentMatchers.any;`；删除原 `when(mockLLMService.convert(...))` 行。`validate` 两个测试不变。）

- [ ] **Step 9：`MockLLMServiceTest` 无需改（仍测 `MockLLMService` 本身，不依赖 ConvertRequest）。编译并跑 ai-service 全量测试**

Run: `mvn -q -pl ai-service -am test`
Expected: BUILD SUCCESS，全部通过（含原 MockLLMServiceTest、AiControllerTest、新增三个测试类）

- [ ] **Step 10：Commit**

```bash
git add ai-service/
git commit -m "feat(ai): Nl2SqlConvertService 整合 LLM+上下文，迁移 ConvertRequest，启用 Feign"
```

---

## Phase 4 — query-service：真实执行 SQL

### Task 11：SqlExecutor + H2 单测

**Files:**
- Create: `query-service/src/main/java/com/nl2sql/query/executor/SqlExecutor.java`
- Test: `query-service/src/test/java/com/nl2sql/query/executor/SqlExecutorTest.java`

- [ ] **Step 1：写失败测试（用 H2 内存库验证 ResultSet 解析与 chartType 推断）**

```java
package com.nl2sql.query.executor;

import com.nl2sql.query.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlExecutor - 执行与结果解析（H2 内存库）")
class SqlExecutorTest {

    private SqlExecutor executor;
    private Connection h2;

    @BeforeEach
    void setUp() throws SQLException {
        executor = new SqlExecutor();
        h2 = DriverManager.getConnection("jdbc:h2:mem:test;MODE=MySQL", "sa", "");
        try (Statement st = h2.createStatement()) {
            st.execute("CREATE TABLE orders (id INT, product_name VARCHAR(50), amount INT)");
            st.execute("INSERT INTO orders VALUES (1,'A',100),(2,'B',200)");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        h2.close();
    }

    @Test
    @DisplayName("doExecute 应把结果集转为 List<Map> 并统计行数")
    void shouldMapResultSet() throws SQLException {
        QueryResult r = executor.doExecute(h2, "SELECT product_name, amount FROM orders ORDER BY id");
        assertThat(r.getData()).hasSize(2);
        assertThat(r.getData().get(0)).containsEntry("PRODUCT_NAME", "A").containsEntry("AMOUNT", 100);
        assertThat(r.getTotalCount()).isEqualTo(2);
        assertThat(r.getExecuteTimeMs()).isNotNull();
    }

    @Test
    @DisplayName("单数值列结果应推断为 bar 图")
    void shouldInferBarChartForSingleNumericColumn() throws SQLException {
        QueryResult r = executor.doExecute(h2, "SELECT SUM(amount) AS total FROM orders");
        assertThat(r.getChartType()).isEqualTo("bar");
    }

    @Test
    @DisplayName("多列结果应推断为 table")
    void shouldInferTableForMultiColumn() throws SQLException {
        QueryResult r = executor.doExecute(h2, "SELECT * FROM orders");
        assertThat(r.getChartType()).isEqualTo("table");
    }
}
```
注：H2 默认 `MODE=MySQL` 下 `getColumnLabel` 返回大写列名，故断言用大写键。

- [ ] **Step 2：运行测试，确认失败（SqlExecutor 未定义）**

Run: `mvn -q -pl query-service -am test -Dtest=SqlExecutorTest`
Expected: 编译失败

- [ ] **Step 3：新建 `SqlExecutor`（建 MySQL 连接 + 可测的 `doExecute(Connection,...)`）**

```java
package com.nl2sql.query.executor;

import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.exception.QueryResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在目标库执行 SQL 并解析结果。MySQL 即连即用；核心解析逻辑 {@link #doExecute} 可注入连接单测。
 * 本期无只读护栏（见设计文档「已知风险」）：被查库应使用只读账号。
 */
@Slf4j
@Component
public class SqlExecutor {

    private static final String MYSQL_URL_TEMPLATE =
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=30000";

    /** 建连并执行；连接失败与执行失败分别映射不同错误码。 */
    public QueryResult execute(DataSourceConnectionDTO conn, String database, String sql) {
        if (conn == null || !"mysql".equalsIgnoreCase(conn.getType())) {
            throw new BaseException(QueryResultCode.QUERY_DB_TYPE_UNSUPPORTED);
        }
        String url = String.format(MYSQL_URL_TEMPLATE, conn.getHost(), conn.getPort(), database);
        try (Connection jdbc = DriverManager.getConnection(url, conn.getUsername(), conn.getPassword())) {
            return doExecute(jdbc, sql);
        } catch (SQLException e) {
            throw new BaseException(QueryResultCode.QUERY_DATASOURCE_CONNECT_FAILED,
                    QueryResultCode.QUERY_DATASOURCE_CONNECT_FAILED.getMessage(), e);
        }
    }

    /** 解析 ResultSet → QueryResult。包级可见以支持注入连接的单测。 */
    QueryResult doExecute(Connection jdbc, String sql) throws SQLException {
        long start = System.currentTimeMillis();
        try (Statement stmt = jdbc.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= n; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            QueryResult result = new QueryResult();
            result.setSql(sql);
            result.setData(rows);
            result.setTotalCount(rows.size());
            result.setExecuteTimeMs(System.currentTimeMillis() - start);
            result.setChartType(inferChartType(md, n));
            return result;
        } catch (SQLException e) {
            throw new BaseException(QueryResultCode.QUERY_SQL_EXECUTE_FAILED,
                    QueryResultCode.QUERY_SQL_EXECUTE_FAILED.getMessage(), e);
        }
    }

    /** 简单启发：恰好两列且第二列为数值 → bar；否则 table。 */
    private String inferChartType(ResultSetMetaData md, int n) throws SQLException {
        if (n == 2) {
            String t = md.getColumnTypeName(2).toUpperCase();
            if (t.contains("INT") || t.contains("DECIMAL") || t.contains("DOUBLE")
                    || t.contains("FLOAT") || t.contains("NUMERIC")) {
                return "bar";
            }
        }
        return "table";
    }
}
```

- [ ] **Step 4：运行测试，确认通过**

Run: `mvn -q -pl query-service -am test -Dtest=SqlExecutorTest`
Expected: 3 tests PASS

- [ ] **Step 5：Commit**

```bash
git add query-service/src/main/java/com/nl2sql/query/executor/ \
        query-service/src/test/java/com/nl2sql/query/executor/
git commit -m "feat(query): SqlExecutor 真实执行 SQL 并解析结果集"
```

---

### Task 12：QueryRequest 加 databaseName + QueryService 改造 + @EnableFeignClients + pom + 测试改造

**Files:**
- Modify: `query-service/pom.xml`
- Modify: `query-service/src/main/java/com/nl2sql/query/QueryApplication.java`
- Modify: `query-service/src/main/java/com/nl2sql/query/dto/QueryRequest.java`
- Modify: `query-service/src/main/java/com/nl2sql/query/service/QueryService.java`
- Modify: `query-service/src/test/java/com/nl2sql/query/service/QueryServiceTest.java`

- [ ] **Step 1：`query-service/pom.xml` `<dependencies>` 段新增**

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
```

- [ ] **Step 2：`QueryApplication` 加 `@EnableFeignClients`**

```java
package com.nl2sql.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.nl2sql.common.feign")
@SpringBootApplication
public class QueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryApplication.class, args);
    }
}
```

- [ ] **Step 3：`QueryRequest` 加 `databaseName` 字段**

在类中 `conversationId` 字段后追加：

```java
    /** 目标库名：缺省时按数据源唯一库解析，多库必填 */
    private String databaseName;
```

- [ ] **Step 4：改造 `QueryService.queryByNaturalLanguage` 为真实同步链路。完整替换该类**

```java
package com.nl2sql.query.service;

import com.nl2sql.common.PageResult;
import com.nl2sql.common.dto.ConvertRequest;
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.enums.ResultCode;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.common.feign.AiServiceClient;
import com.nl2sql.common.feign.SchemaServiceClient;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.executor.SqlExecutor;
import com.nl2sql.query.repository.QueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final AiServiceClient aiServiceClient;
    private final SchemaServiceClient schemaServiceClient;
    private final SqlExecutor sqlExecutor;
    private final QueryHistoryRepository historyRepository;

    /** 同步真实链路：取连接 → 确定 database → LLM 生成 SQL → 执行 → 存历史。 */
    public QueryResult queryByNaturalLanguage(QueryRequest request) {
        DataSourceConnectionDTO conn = schemaServiceClient
                .getConnection(request.getDataSourceId()).getData();
        String database = resolveDatabase(request, conn);

        ConvertRequest convertReq = new ConvertRequest();
        convertReq.setDataSourceId(request.getDataSourceId());
        convertReq.setNaturalLanguage(request.getNaturalLanguage());
        convertReq.setDatabaseName(database);
        String sql = aiServiceClient.convert(convertReq).getData();

        QueryResult result = sqlExecutor.execute(conn, database, sql);
        saveHistory(request, result, null);
        return result;
    }

    /** 解析目标库：显式指定优先；否则数据源唯一库自动选用；多库报错。 */
    private String resolveDatabase(QueryRequest request, DataSourceConnectionDTO conn) {
        if (request.getDatabaseName() != null && !request.getDatabaseName().isBlank()) {
            return request.getDatabaseName();
        }
        List<String> dbs = conn.getDatabaseNames();
        if (dbs == null || dbs.isEmpty()) {
            throw new BaseException(ResultCode.BAD_REQUEST, "数据源未配置任何库");
        }
        if (dbs.size() == 1) {
            return dbs.get(0);
        }
        throw new BaseException(com.nl2sql.query.exception.QueryResultCode.QUERY_DATABASE_NOT_SPECIFIED);
    }

    /** 直接执行 SQL（本轮保留原行为，未接真实执行——后续可扩展）。 */
    public QueryResult queryBySql(String sql) {
        QueryResult result = new QueryResult();
        result.setSql(sql);
        result.setData(List.of(Map.of("result", "mock")));
        result.setTotalCount(1);
        result.setExecuteTimeMs(50L);
        result.setChartType("table");
        return result;
    }

    @Cacheable(cacheNames = com.nl2sql.common.cache.CacheNames.QUERY_HISTORY,
            key = "#conversationId", unless = "#conversationId == null")
    public List<QueryHistory> history(String conversationId) {
        return historyRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
    }

    public PageResult<QueryHistory> historyPage(String conversationId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(pageNum - 1, 0), pageSize);
        Page<QueryHistory> page = historyRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize);
    }

    @CacheEvict(cacheNames = com.nl2sql.common.cache.CacheNames.QUERY_HISTORY,
            key = "#request.conversationId", condition = "#request.conversationId != null")
    void saveHistory(QueryRequest request, QueryResult result, String error) {
        QueryHistory h = new QueryHistory();
        h.setConversationId(request.getConversationId() != null
                ? request.getConversationId() : UUID.randomUUID().toString());
        h.setUserId(request.getUserId() != null ? request.getUserId() : 0L);
        h.setDataSourceId(request.getDataSourceId() != null ? request.getDataSourceId() : 0L);
        h.setNaturalLanguage(request.getNaturalLanguage());
        h.setGeneratedSql(result.getSql());
        h.setSqlExecuted(result.getSql());
        h.setExecuteTimeMs(result.getExecuteTimeMs());
        h.setResultCount(result.getTotalCount());
        h.setStatus(error == null ? "success" : "failed");
        h.setErrorMessage(error);
        h.setChartType(result.getChartType());
        historyRepository.save(h);
    }
}
```

- [ ] **Step 5：改写 `QueryServiceTest` 适配新依赖（mock AiServiceClient/SchemaServiceClient/SqlExecutor）**

完整替换测试类：

```java
package com.nl2sql.query.service;

import com.nl2sql.common.R;
import com.nl2sql.common.PageResult;
import com.nl2sql.common.dto.DataSourceConnectionDTO;
import com.nl2sql.common.dto.ConvertRequest;
import com.nl2sql.common.feign.AiServiceClient;
import com.nl2sql.common.feign.SchemaServiceClient;
import com.nl2sql.query.dto.QueryRequest;
import com.nl2sql.query.dto.QueryResult;
import com.nl2sql.query.entity.QueryHistory;
import com.nl2sql.query.executor.SqlExecutor;
import com.nl2sql.query.repository.QueryHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("QueryService - 真实同步链路")
@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock private AiServiceClient aiServiceClient;
    @Mock private SchemaServiceClient schemaServiceClient;
    @Mock private SqlExecutor sqlExecutor;
    @Mock private QueryHistoryRepository historyRepository;
    @InjectMocks private QueryService queryService;

    @Test
    @DisplayName("queryByNaturalLanguage 应走 convert→execute→存历史")
    void shouldRunRealLink() {
        QueryRequest request = new QueryRequest();
        request.setDataSourceId(1L);
        request.setNaturalLanguage("本月销售额");

        DataSourceConnectionDTO conn = new DataSourceConnectionDTO();
        conn.setType("mysql");
        conn.setDatabaseNames(List.of("shop"));
        when(schemaServiceClient.getConnection(1L)).thenReturn(R.ok(conn));
        when(aiServiceClient.convert(any(ConvertRequest.class))).thenReturn(R.ok("SELECT * FROM orders;"));

        QueryResult exec = new QueryResult();
        exec.setSql("SELECT * FROM orders;");
        exec.setData(List.of(Map.of("id", 1)));
        exec.setTotalCount(1);
        exec.setChartType("table");
        exec.setExecuteTimeMs(10L);
        when(sqlExecutor.execute(conn, "shop", "SELECT * FROM orders;")).thenReturn(exec);

        QueryResult result = queryService.queryByNaturalLanguage(request);

        assertThat(result.getSql()).isEqualTo("SELECT * FROM orders;");
        ArgumentCaptor<ConvertRequest> captor = ArgumentCaptor.forClass(ConvertRequest.class);
        verify(aiServiceClient).convert(captor.capture());
        assertThat(captor.getValue().getDatabaseName()).isEqualTo("shop");
        verify(historyRepository).save(any(QueryHistory.class));
    }

    @Test
    @DisplayName("queryBySql 保持原有 mock 行为")
    void shouldReturnMockForSql() {
        QueryResult result = queryService.queryBySql("SELECT 1");
        assertThat(result.getSql()).isEqualTo("SELECT 1");
        assertThat(result.getData()).containsExactly(Map.of("result", "mock"));
    }

    @Test
    @DisplayName("history 应按 conversationId 查询")
    void shouldFindHistory() {
        QueryHistory h = new QueryHistory();
        h.setConversationId("conv-1");
        when(historyRepository.findByConversationIdOrderByCreatedAtDesc("conv-1")).thenReturn(List.of(h));
        assertThat(queryService.history("conv-1")).hasSize(1);
    }

    @Test
    @DisplayName("historyPage 应返回分页结构")
    void shouldReturnHistoryPage() {
        QueryHistory h = new QueryHistory();
        h.setId(1L);
        when(historyRepository.findByConversationIdOrderByCreatedAtDesc(eq("conv-1"), any()))
                .thenReturn(new PageImpl<>(List.of(h), PageRequest.of(0, 10), 1));
        PageResult<QueryHistory> p = queryService.historyPage("conv-1", 1, 10);
        assertThat(p.getTotal()).isEqualTo(1);
        assertThat(p.getRecords()).hasSize(1);
    }
}
```

- [ ] **Step 6：运行 query-service 全量测试**

Run: `mvn -q -pl query-service -am test`
Expected: BUILD SUCCESS，全部通过（`QueryServiceTest`、`QueryControllerTest` 等无回归）

- [ ] **Step 7：Commit**

```bash
git add query-service/
git commit -m "feat(query): QueryService 改为真实同步链路（convert→execute），启用 Feign"
```

---

## Phase 5 — 集成验证

### Task 13：全量编译 + 全量测试

**Files:** 无（仅验证）

- [ ] **Step 1：全量编译**

Run: `mvn -q -DskipTests clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2：全量测试**

Run: `mvn -q test`
Expected: BUILD SUCCESS，所有模块测试通过，无回归

- [ ] **Step 3：若全绿，记录通过；若有失败，修复后再提交（无需新 commit 若无代码变更）**

---

### Task 14：配置收尾 + 端到端验证指引

**Files:**
- Modify: `docs/dev/operations.md`（追加 LLM 配置说明，可选）

- [ ] **Step 1：确认 Nacos 配置 `nl2sql-ai-service.yml`（生产）含 `nl2sql.llm` 段。若用 local profile，已在 Task 7 写入 `application-local.yml`。**

- [ ] **Step 2：在 `docs/dev/operations.md` 追加一节「核心链路验证」，内容：**

````markdown
## 核心链路验证（NL → SQL → 真实执行）

前置：
1. 中间件已起：`docker compose up -d`（mysql / redis / rabbitmq / nacos）。
2. 配置 `MINIMAX_API_KEY`（或在 Nacos/local 用 `ENC(...)` 加密后的 key）。
3. 在 schema-service 录入一个 MySQL 数据源并触发扫描（`POST /api/schema/scan/{id}`），确保 `schema_cache` 有数据。
4. 启动 gateway / schema / query / ai 四个服务。

验证：
```bash
curl -X POST http://localhost:8080/api/query/nl \
  -H 'Content-Type: application/json' \
  -d '{"dataSourceId":1,"databaseName":"<你的库>","naturalLanguage":"查询订单总金额"}'
```
期望返回 `code=200`，`data.data` 为真实查询结果（非 mock）。LLM 不可用应返回 `code=12001`。

> 安全提示：本期无只读护栏，被查库请使用只读账号。
````

- [ ] **Step 3：Commit**

```bash
git add docs/dev/operations.md
git commit -m "docs(ops): 补充核心链路端到端验证说明"
```

---

## 完成标准（对照设计 spec）

- [ ] ai-service 接入 MiniMax（Anthropic Messages 端点），`MockLLMService` 降级为可选 fallback。
- [ ] query-service 真实连接目标库执行 SQL，返回真实结果。
- [ ] NL2SQL 携带 schema 上下文（从 `schema_cache` 读取，不重连目标库）。
- [ ] 错误码 `AI_LLM_UNAVAILABLE` / `QUERY_DATASOURCE_CONNECT_FAILED` / `QUERY_SQL_EXECUTE_FAILED` 等走 i18n，不硬编码中文。
- [ ] 新增单测/切片测试全绿，现有测试无回归。
- [ ] 端到端：配真实 key 后 `/api/query/nl` 返回真实数据。
