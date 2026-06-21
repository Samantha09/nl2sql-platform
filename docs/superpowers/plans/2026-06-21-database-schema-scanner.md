# 数据库结构扫描功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用真实的多数据库结构扫描替换 schema-service 的 Mock，本期落地 MySQL，抽象层为未来方言扩展留好 SPI。

**Architecture:** `DatabaseScanner` SPI 接口 + `ScannerRegistry` 按 `DbType` 分发；MySQL 实现用 `information_schema` 方言 SQL 抽取方言无关的元数据模型；`SchemaScanService` 负责解密连接、调度扫描、覆盖式持久化到既有 `schema_cache`/`table_list_cache`（JSON 列）。

**Tech Stack:** Spring Boot 3.2.5 / JPA / JUnit5 + Mockito / Testcontainers(MySQL) / H2(持久化测试) / Jackson。

设计依据：`docs/superpowers/specs/2026-06-21-database-schema-scanner-design.md`

---

## 文件结构

新建（schema-service `src/main/java/com/nl2sql/schema/`）：
- `enums/DbType.java` — 数据库类型枚举
- `exception/SchemaResultCode.java` — 领域错误码
- `scanner/model/{SchemaMetadata,TableMetadata,ColumnMetadata,IndexMetadata,ForeignKeyMetadata}.java`
- `scanner/ScanContext.java`、`scanner/DatabaseScanner.java`、`scanner/ScannerRegistry.java`
- `scanner/impl/MySqlDatabaseScanner.java`
- `entity/{SchemaCache,TableListCache}.java`
- `repository/{SchemaCacheRepository,TableListCacheRepository}.java`
- `service/SchemaScanService.java`

修改：
- `dto/TableSchemaDTO.java` — 扩展 nullable/defaultValue/indexes/foreignKeys
- `service/DataSourceService.java` — scanTables/getTableDetail 改读持久化
- `controller/DataSourceController.java` — `POST /scan/{id}` 触发真实扫描 + `@CacheEvict`
- `common/src/main/resources/i18n/messages*.properties` — 新增 `schema.*` 文案
- `schema-service/pom.xml` — 新增 testcontainers(mysql) test 依赖

---

## Task 1: `DbType` 枚举

**Files:**
- Create: `schema-service/src/main/java/com/nl2sql/schema/enums/DbType.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/enums/DbTypeTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.nl2sql.schema.enums;

import com.nl2sql.common.enums.IEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DbType - 数据库类型枚举")
class DbTypeTest {

    @Test
    @DisplayName("getCode/getDesc 返回稳定编码与可读描述")
    void shouldExposeCodeAndDesc() {
        assertThat(DbType.MYSQL.getCode()).isEqualTo("mysql");
        assertThat(DbType.MYSQL.getDesc()).isEqualTo("MySQL");
    }

    @Test
    @DisplayName("of 按编码反查命中")
    void shouldResolveByCode() {
        Optional<DbType> found = IEnum.of(DbType.class, "mysql");
        assertThat(found).contains(DbType.MYSQL);
    }

    @Test
    @DisplayName("of 未知编码返回空")
    void shouldReturnEmptyForUnknown() {
        assertThat(IEnum.of(DbType.class, "oracle")).isEmpty();
        assertThat(IEnum.of(DbType.class, (String) null)).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl schema-service test -Dtest=DbTypeTest`
Expected: 编译失败 / FAIL（`DbType` 不存在）

- [ ] **Step 3: 实现 `DbType`**

```java
package com.nl2sql.schema.enums;

import com.nl2sql.common.enums.IEnum;

/**
 * 支持的数据库类型。code 即 {@code DataSourceConfig.type} 的取值。
 * 新增方言：此处加枚举值 → 提供对应 {@code DatabaseScanner} 实现 → 补 JDBC 驱动依赖。
 */
public enum DbType implements IEnum<String> {

    MYSQL("mysql", "MySQL");

    private final String code;
    private final String desc;

    DbType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl schema-service test -Dtest=DbTypeTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/enums/DbType.java \
        schema-service/src/test/java/com/nl2sql/schema/enums/DbTypeTest.java
git commit -m "feat(schema): 新增 DbType 数据库类型枚举"
```

---

## Task 2: `SchemaResultCode` 领域错误码 + i18n 文案

**Files:**
- Create: `schema-service/src/main/java/com/nl2sql/schema/exception/SchemaResultCode.java`
- Modify: `common/src/main/resources/i18n/messages_zh_CN.properties`、`messages_en.properties`、`messages.properties`
- Test: `schema-service/src/test/java/com/nl2sql/schema/exception/SchemaResultCodeTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.nl2sql.schema.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SchemaResultCode - 领域错误码")
class SchemaResultCodeTest {

    @Test
    @DisplayName("错误码落在 schema 领域 band（11xxx）且绑定 i18n key")
    void shouldExposeCodeAndKey() {
        assertThat(SchemaResultCode.DB_TYPE_UNSUPPORTED.getCode()).isEqualTo(11001);
        assertThat(SchemaResultCode.DB_TYPE_UNSUPPORTED.getI18nKey()).isEqualTo("schema.db_type_unsupported");
        assertThat(SchemaResultCode.SCAN_CONNECT_FAILED.getDesc()).isEqualTo("无法连接目标数据库");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl schema-service test -Dtest=SchemaResultCodeTest`
Expected: FAIL（`SchemaResultCode` 不存在）

- [ ] **Step 3: 实现 `SchemaResultCode`**

```java
package com.nl2sql.schema.exception;

import com.nl2sql.common.enums.IResultCode;

/**
 * schema-service 领域错误码。编码用 11xxx band，避开 common {@code ResultCode} 的 HTTP 语义区间。
 */
public enum SchemaResultCode implements IResultCode {

    DB_TYPE_UNSUPPORTED(11001, "schema.db_type_unsupported", "不支持的数据库类型"),
    DATASOURCE_NOT_FOUND(11002, "schema.datasource_not_found", "数据源不存在"),
    SCAN_CONNECT_FAILED(11003, "schema.scan_connect_failed", "无法连接目标数据库"),
    SCAN_EXECUTE_FAILED(11004, "schema.scan_execute_failed", "扫描执行失败");

    private final Integer code;
    private final String i18nKey;
    private final String desc;

    SchemaResultCode(Integer code, String i18nKey, String desc) {
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

- [ ] **Step 4: 追加 i18n 文案**

`common/src/main/resources/i18n/messages_zh_CN.properties` 末尾追加：
```properties
schema.db_type_unsupported=不支持的数据库类型
schema.datasource_not_found=数据源不存在
schema.scan_connect_failed=无法连接目标数据库
schema.scan_execute_failed=扫描执行失败
```

`common/src/main/resources/i18n/messages_en.properties` 末尾追加：
```properties
schema.db_type_unsupported=Unsupported database type
schema.datasource_not_found=Data source not found
schema.scan_connect_failed=Cannot connect to target database
schema.scan_execute_failed=Schema scan failed
```

`common/src/main/resources/i18n/messages.properties` 末尾追加（兜底=中文）：
```properties
schema.db_type_unsupported=不支持的数据库类型
schema.datasource_not_found=数据源不存在
schema.scan_connect_failed=无法连接目标数据库
schema.scan_execute_failed=扫描执行失败
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -q -pl schema-service test -Dtest=SchemaResultCodeTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/exception/SchemaResultCode.java \
        schema-service/src/test/java/com/nl2sql/schema/exception/SchemaResultCodeTest.java \
        common/src/main/resources/i18n/messages*.properties
git commit -m "feat(schema): 新增 SchemaResultCode 领域错误码与 i18n 文案"
```

---

## Task 3: 方言无关元数据模型

**Files:**
- Create: `schema-service/src/main/java/com/nl2sql/schema/scanner/model/SchemaMetadata.java`
- Create: `.../model/TableMetadata.java`、`ColumnMetadata.java`、`IndexMetadata.java`、`ForeignKeyMetadata.java`

无独立单测（纯数据载体，由 Task 6/8 间接覆盖）。

- [ ] **Step 1: 创建五个 POJO**

`SchemaMetadata.java`:
```java
package com.nl2sql.schema.scanner.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** 一次扫描的完整结果（方言无关）。 */
@Data
public class SchemaMetadata {
    private List<TableMetadata> tables = new ArrayList<>();
}
```

`TableMetadata.java`:
```java
package com.nl2sql.schema.scanner.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** 单表的完整结构。 */
@Data
public class TableMetadata {
    private String name;
    private String comment;
    private long rowEstimate;
    private List<ColumnMetadata> columns = new ArrayList<>();
    private List<String> primaryKeys = new ArrayList<>();
    private List<IndexMetadata> indexes = new ArrayList<>();
    private List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
}
```

`ColumnMetadata.java`:
```java
package com.nl2sql.schema.scanner.model;

import lombok.Data;

/** 列结构。type 为方言原文，如 varchar(255)。 */
@Data
public class ColumnMetadata {
    private String name;
    private String type;
    private String comment;
    private boolean nullable;
    private String defaultValue;
    private int ordinalPosition;
}
```

`IndexMetadata.java`:
```java
package com.nl2sql.schema.scanner.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** 索引。 */
@Data
public class IndexMetadata {
    private String name;
    private boolean unique;
    private List<String> columns = new ArrayList<>();
}
```

`ForeignKeyMetadata.java`:
```java
package com.nl2sql.schema.scanner.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** 外键：本表 columns 一一对应 referencedTable.referencedColumns。 */
@Data
public class ForeignKeyMetadata {
    private String name;
    private List<String> columns = new ArrayList<>();
    private String referencedTable;
    private List<String> referencedColumns = new ArrayList<>();
}
```

- [ ] **Step 2: 编译确认**

Run: `mvn -q -pl schema-service test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/scanner/model/
git commit -m "feat(schema): 新增方言无关的元数据模型"
```

---

## Task 4: `ScanContext` 扫描输入

**Files:**
- Create: `schema-service/src/main/java/com/nl2sql/schema/scanner/ScanContext.java`

- [ ] **Step 1: 创建 record**

```java
package com.nl2sql.schema.scanner;

import com.nl2sql.schema.enums.DbType;

/**
 * 一次扫描的输入。password 为已解密明文，仅存活于内存，不落库不打日志。
 */
public record ScanContext(
        DbType type,
        String host,
        int port,
        String databaseName,
        String username,
        String password
) {}
```

- [ ] **Step 2: 编译确认**

Run: `mvn -q -pl schema-service test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/scanner/ScanContext.java
git commit -m "feat(schema): 新增 ScanContext 扫描输入值对象"
```

---

## Task 5: `DatabaseScanner` SPI 接口 + `ScannerRegistry` 分发

**Files:**
- Create: `schema-service/src/main/java/com/nl2sql/schema/scanner/DatabaseScanner.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/scanner/ScannerRegistry.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/scanner/ScannerRegistryTest.java`

- [ ] **Step 1: 创建接口**

```java
package com.nl2sql.schema.scanner;

import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.scanner.model.SchemaMetadata;

import java.util.List;

/** 数据库结构扫描 SPI：一种数据库一份实现。 */
public interface DatabaseScanner {

    /** 该实现负责哪种数据库。 */
    boolean supports(DbType type);

    /** 全量扫描：表 + 列 + 主键 + 外键 + 索引。 */
    SchemaMetadata scan(ScanContext context);

    /** 轻量：仅列出表名。 */
    List<String> listTables(ScanContext context);
}
```

- [ ] **Step 2: 写 `ScannerRegistry` 失败测试**

```java
package com.nl2sql.schema.scanner;

import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScannerRegistry - 按 DbType 分发")
class ScannerRegistryTest {

    /** 测试用假 scanner，仅声明支持 MYSQL。 */
    static class FakeMySqlScanner implements DatabaseScanner {
        @Override public boolean supports(DbType type) { return type == DbType.MYSQL; }
        @Override public SchemaMetadata scan(ScanContext c) { return new SchemaMetadata(); }
        @Override public List<String> listTables(ScanContext c) { return List.of(); }
    }

    @Test
    @DisplayName("resolve 命中支持的类型")
    void shouldResolveSupported() {
        ScannerRegistry registry = new ScannerRegistry(List.of(new FakeMySqlScanner()));
        assertThat(registry.resolve(DbType.MYSQL)).isInstanceOf(FakeMySqlScanner.class);
    }

    @Test
    @DisplayName("resolve 未命中抛 DB_TYPE_UNSUPPORTED")
    void shouldThrowWhenUnsupported() {
        ScannerRegistry registry = new ScannerRegistry(List.of());
        assertThatThrownBy(() -> registry.resolve(DbType.MYSQL))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getCode())
                        .isEqualTo(SchemaResultCode.DB_TYPE_UNSUPPORTED.getCode()));
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `mvn -q -pl schema-service test -Dtest=ScannerRegistryTest`
Expected: FAIL（`ScannerRegistry` 不存在）

- [ ] **Step 4: 实现 `ScannerRegistry`**

```java
package com.nl2sql.schema.scanner;

import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.exception.SchemaResultCode;
import org.springframework.stereotype.Component;

import java.util.List;

/** 收集所有 {@link DatabaseScanner} 实现，按 {@link DbType} 分发。 */
@Component
public class ScannerRegistry {

    private final List<DatabaseScanner> scanners;

    public ScannerRegistry(List<DatabaseScanner> scanners) {
        this.scanners = scanners;
    }

    public DatabaseScanner resolve(DbType type) {
        return scanners.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new BaseException(SchemaResultCode.DB_TYPE_UNSUPPORTED));
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -q -pl schema-service test -Dtest=ScannerRegistryTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/scanner/DatabaseScanner.java \
        schema-service/src/main/java/com/nl2sql/schema/scanner/ScannerRegistry.java \
        schema-service/src/test/java/com/nl2sql/schema/scanner/ScannerRegistryTest.java
git commit -m "feat(schema): 新增 DatabaseScanner SPI 与 ScannerRegistry 分发"
```

---

## Task 6: `MySqlDatabaseScanner` 实现 + Testcontainers 集成测试

**Files:**
- Modify: `schema-service/pom.xml`（新增 testcontainers mysql + junit-jupiter test 依赖）
- Create: `schema-service/src/main/java/com/nl2sql/schema/scanner/impl/MySqlDatabaseScanner.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/scanner/impl/MySqlDatabaseScannerIT.java`

- [ ] **Step 1: 加测试依赖到 `schema-service/pom.xml`**

在 `<dependencies>` 内（紧邻已有 h2 test 依赖之后）追加：
```xml
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
```
（版本由 root pom 的 testcontainers-bom 管理，无需写 version。mysql-connector-j 已是 runtime 依赖。）

- [ ] **Step 2: 写失败集成测试**

```java
package com.nl2sql.schema.scanner.impl;

import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.scanner.ScanContext;
import com.nl2sql.schema.scanner.model.ColumnMetadata;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import com.nl2sql.schema.scanner.model.TableMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("MySqlDatabaseScanner - 真实 MySQL 元数据抽取")
class MySqlDatabaseScannerIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("shop")
            .withUsername("root_test")
            .withPassword("test_pwd");

    @BeforeAll
    static void seed() throws Exception {
        try (Connection c = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
                    name VARCHAR(100) NOT NULL COMMENT '用户名',
                    email VARCHAR(200) NULL,
                    UNIQUE KEY uk_email (email)
                ) COMMENT='用户表'
                """);
            s.execute("""
                CREATE TABLE orders (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL COMMENT '下单用户',
                    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
                ) COMMENT='订单表'
                """);
        }
    }

    private ScanContext ctx() {
        return new ScanContext(DbType.MYSQL, MYSQL.getHost(),
                MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT), "shop",
                MYSQL.getUsername(), MYSQL.getPassword());
    }

    @Test
    @DisplayName("scan 抽取表注释、列(含可空/注释)、主键、唯一索引、外键")
    void shouldScanFullMetadata() {
        SchemaMetadata meta = new MySqlDatabaseScanner().scan(ctx());

        TableMetadata users = meta.getTables().stream()
                .filter(t -> t.getName().equals("users")).findFirst().orElseThrow();
        assertThat(users.getComment()).isEqualTo("用户表");
        assertThat(users.getPrimaryKeys()).containsExactly("id");

        ColumnMetadata name = users.getColumns().stream()
                .filter(col -> col.getName().equals("name")).findFirst().orElseThrow();
        assertThat(name.getType()).isEqualTo("varchar(100)");
        assertThat(name.getComment()).isEqualTo("用户名");
        assertThat(name.isNullable()).isFalse();

        assertThat(users.getIndexes()).anySatisfy(idx -> {
            assertThat(idx.getName()).isEqualTo("uk_email");
            assertThat(idx.isUnique()).isTrue();
            assertThat(idx.getColumns()).containsExactly("email");
        });

        TableMetadata orders = meta.getTables().stream()
                .filter(t -> t.getName().equals("orders")).findFirst().orElseThrow();
        assertThat(orders.getForeignKeys()).anySatisfy(fk -> {
            assertThat(fk.getColumns()).containsExactly("user_id");
            assertThat(fk.getReferencedTable()).isEqualTo("users");
            assertThat(fk.getReferencedColumns()).containsExactly("id");
        });
    }

    @Test
    @DisplayName("listTables 返回全部表名")
    void shouldListTables() {
        assertThat(new MySqlDatabaseScanner().listTables(ctx()))
                .containsExactlyInAnyOrder("users", "orders");
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `mvn -q -pl schema-service test -Dtest=MySqlDatabaseScannerIT`
Expected: FAIL（`MySqlDatabaseScanner` 不存在）。需本机有 Docker。

- [ ] **Step 4: 实现 `MySqlDatabaseScanner`**

```java
package com.nl2sql.schema.scanner.impl;

import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.scanner.DatabaseScanner;
import com.nl2sql.schema.scanner.ScanContext;
import com.nl2sql.schema.scanner.model.ColumnMetadata;
import com.nl2sql.schema.scanner.model.ForeignKeyMetadata;
import com.nl2sql.schema.scanner.model.IndexMetadata;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import com.nl2sql.schema.scanner.model.TableMetadata;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MySQL 方言实现：全部元数据查 information_schema。 */
@Component
public class MySqlDatabaseScanner implements DatabaseScanner {

    private static final String URL_TEMPLATE =
            "jdbc:mysql://%s:%d/%s?useInformationSchema=true&connectTimeout=5000&socketTimeout=30000"
            + "&useSSL=false&allowPublicKeyRetrieval=true";

    @Override
    public boolean supports(DbType type) {
        return type == DbType.MYSQL;
    }

    @Override
    public List<String> listTables(ScanContext ctx) {
        try (Connection conn = open(ctx)) {
            return queryTableNames(conn, ctx.databaseName());
        } catch (SQLException e) {
            throw connectFailed(e);
        }
    }

    @Override
    public SchemaMetadata scan(ScanContext ctx) {
        SchemaMetadata meta = new SchemaMetadata();
        try (Connection conn = open(ctx)) {
            String db = ctx.databaseName();
            for (TableMetadata table : queryTables(conn, db)) {
                table.setColumns(queryColumns(conn, db, table));
                table.setIndexes(queryIndexes(conn, db, table.getName()));
                table.setForeignKeys(queryForeignKeys(conn, db, table.getName()));
                meta.getTables().add(table);
            }
            return meta;
        } catch (SQLException e) {
            throw executeFailed(e);
        }
    }

    private Connection open(ScanContext ctx) throws SQLException {
        String url = String.format(URL_TEMPLATE, ctx.host(), ctx.port(), ctx.databaseName());
        return DriverManager.getConnection(url, ctx.username(), ctx.password());
    }

    private List<String> queryTableNames(Connection conn, String db) throws SQLException {
        String sql = "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name";
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("table_name"));
                }
            }
        }
        return names;
    }

    private List<TableMetadata> queryTables(Connection conn, String db) throws SQLException {
        String sql = "SELECT table_name, table_comment, table_rows FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name";
        List<TableMetadata> tables = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableMetadata t = new TableMetadata();
                    t.setName(rs.getString("table_name"));
                    t.setComment(nullToEmpty(rs.getString("table_comment")));
                    t.setRowEstimate(rs.getLong("table_rows"));
                    tables.add(t);
                }
            }
        }
        return tables;
    }

    private List<ColumnMetadata> queryColumns(Connection conn, String db, TableMetadata table)
            throws SQLException {
        String sql = "SELECT column_name, column_type, column_comment, is_nullable, "
                + "column_default, ordinal_position, column_key FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
        List<ColumnMetadata> columns = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            ps.setString(2, table.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ColumnMetadata c = new ColumnMetadata();
                    c.setName(rs.getString("column_name"));
                    c.setType(rs.getString("column_type"));
                    c.setComment(nullToEmpty(rs.getString("column_comment")));
                    c.setNullable("YES".equalsIgnoreCase(rs.getString("is_nullable")));
                    c.setDefaultValue(rs.getString("column_default"));
                    c.setOrdinalPosition(rs.getInt("ordinal_position"));
                    columns.add(c);
                    if ("PRI".equalsIgnoreCase(rs.getString("column_key"))) {
                        table.getPrimaryKeys().add(c.getName());
                    }
                }
            }
        }
        return columns;
    }

    private List<IndexMetadata> queryIndexes(Connection conn, String db, String tableName)
            throws SQLException {
        String sql = "SELECT index_name, non_unique, seq_in_index, column_name "
                + "FROM information_schema.statistics WHERE table_schema = ? AND table_name = ? "
                + "ORDER BY index_name, seq_in_index";
        Map<String, IndexMetadata> byName = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("index_name");
                    IndexMetadata idx = byName.computeIfAbsent(name, n -> {
                        IndexMetadata m = new IndexMetadata();
                        m.setName(n);
                        return m;
                    });
                    idx.setUnique(rs.getInt("non_unique") == 0);
                    idx.getColumns().add(rs.getString("column_name"));
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    private List<ForeignKeyMetadata> queryForeignKeys(Connection conn, String db, String tableName)
            throws SQLException {
        String sql = "SELECT constraint_name, column_name, referenced_table_name, "
                + "referenced_column_name FROM information_schema.key_column_usage "
                + "WHERE table_schema = ? AND table_name = ? AND referenced_table_name IS NOT NULL "
                + "ORDER BY constraint_name, ordinal_position";
        Map<String, ForeignKeyMetadata> byName = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, db);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("constraint_name");
                    ForeignKeyMetadata fk = byName.computeIfAbsent(name, n -> {
                        ForeignKeyMetadata m = new ForeignKeyMetadata();
                        m.setName(n);
                        return m;
                    });
                    fk.setReferencedTable(rs.getString("referenced_table_name"));
                    fk.getColumns().add(rs.getString("column_name"));
                    fk.getReferencedColumns().add(rs.getString("referenced_column_name"));
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private BaseException connectFailed(SQLException e) {
        return new BaseException(SchemaResultCode.SCAN_CONNECT_FAILED,
                SchemaResultCode.SCAN_CONNECT_FAILED.getMessage(), e);
    }

    private BaseException executeFailed(SQLException e) {
        return new BaseException(SchemaResultCode.SCAN_EXECUTE_FAILED,
                SchemaResultCode.SCAN_EXECUTE_FAILED.getMessage(), e);
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -q -pl schema-service test -Dtest=MySqlDatabaseScannerIT`
Expected: PASS（首次运行会拉取 mysql:8.0 镜像，较慢）

- [ ] **Step 6: 提交**

```bash
git add schema-service/pom.xml \
        schema-service/src/main/java/com/nl2sql/schema/scanner/impl/MySqlDatabaseScanner.java \
        schema-service/src/test/java/com/nl2sql/schema/scanner/impl/MySqlDatabaseScannerIT.java
git commit -m "feat(schema): 实现 MySqlDatabaseScanner 并以 Testcontainers 验证"
```

---

## Task 7: 持久化实体与 Repository

**Files:**
- Create: `schema-service/src/main/java/com/nl2sql/schema/entity/SchemaCache.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/entity/TableListCache.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/repository/SchemaCacheRepository.java`
- Create: `schema-service/src/main/java/com/nl2sql/schema/repository/TableListCacheRepository.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/repository/SchemaCacheRepositoryTest.java`

实体直接映射既有 `schema_cache`/`table_list_cache` 列，**不继承 `BaseEntity`**（这两张表用 `cached_at`/`version`，无 `created_at`/`updated_at`）。

- [ ] **Step 1: 创建 `SchemaCache` 实体**

```java
package com.nl2sql.schema.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 映射 schema_cache：每(数据源,表)一行，结构以 JSON 列存储。 */
@Data
@Entity
@Table(name = "schema_cache",
        uniqueConstraints = @UniqueConstraint(name = "uk_ds_table",
                columnNames = {"data_source_id", "table_name"}))
public class SchemaCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "table_comment", length = 500)
    private String tableComment;

    @Column(name = "column_json", columnDefinition = "TEXT")
    private String columnJson;

    @Column(name = "primary_key_json", columnDefinition = "TEXT")
    private String primaryKeyJson;

    @Column(name = "foreign_key_json", columnDefinition = "TEXT")
    private String foreignKeyJson;

    @Column(name = "index_json", columnDefinition = "TEXT")
    private String indexJson;

    @Column(name = "row_estimate")
    private Long rowEstimate;

    @Column(name = "cached_at")
    private LocalDateTime cachedAt;

    @Column(name = "version")
    private Integer version;
}
```

- [ ] **Step 2: 创建 `TableListCache` 实体**

```java
package com.nl2sql.schema.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 映射 table_list_cache：每数据源一行表名列表 JSON。 */
@Data
@Entity
@Table(name = "table_list_cache")
public class TableListCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "table_json", columnDefinition = "TEXT")
    private String tableJson;

    @Column(name = "cached_at")
    private LocalDateTime cachedAt;
}
```

- [ ] **Step 3: 创建 Repository**

`SchemaCacheRepository.java`:
```java
package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.SchemaCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchemaCacheRepository extends JpaRepository<SchemaCache, Long> {

    Optional<SchemaCache> findByDataSourceIdAndTableName(Long dataSourceId, String tableName);

    List<SchemaCache> findByDataSourceId(Long dataSourceId);

    void deleteByDataSourceIdAndTableNameIn(Long dataSourceId, List<String> tableNames);
}
```

`TableListCacheRepository.java`:
```java
package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.TableListCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TableListCacheRepository extends JpaRepository<TableListCache, Long> {

    Optional<TableListCache> findByDataSourceId(Long dataSourceId);
}
```

- [ ] **Step 4: 写持久化测试**

```java
package com.nl2sql.schema.repository;

import com.nl2sql.schema.entity.SchemaCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SchemaCacheRepository - 结构缓存持久化")
class SchemaCacheRepositoryTest {

    @Autowired
    private SchemaCacheRepository repository;

    private SchemaCache cache(Long dsId, String table) {
        SchemaCache c = new SchemaCache();
        c.setDataSourceId(dsId);
        c.setTableName(table);
        c.setTableComment("注释");
        c.setColumnJson("[]");
        c.setPrimaryKeyJson("[\"id\"]");
        c.setForeignKeyJson("[]");
        c.setIndexJson("[]");
        c.setRowEstimate(0L);
        c.setVersion(1);
        c.setCachedAt(LocalDateTime.now());
        return c;
    }

    @Test
    @DisplayName("按数据源+表名查询命中")
    void shouldFindByDataSourceAndTable() {
        repository.save(cache(1L, "users"));

        Optional<SchemaCache> found = repository.findByDataSourceIdAndTableName(1L, "users");

        assertThat(found).isPresent();
        assertThat(found.get().getPrimaryKeyJson()).isEqualTo("[\"id\"]");
    }

    @Test
    @DisplayName("按数据源列出全部表缓存")
    void shouldListByDataSource() {
        repository.save(cache(2L, "a"));
        repository.save(cache(2L, "b"));

        assertThat(repository.findByDataSourceId(2L)).hasSize(2);
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -q -pl schema-service test -Dtest=SchemaCacheRepositoryTest`
Expected: PASS（`@DataJpaTest` 用 H2 自动建表）

- [ ] **Step 6: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/entity/SchemaCache.java \
        schema-service/src/main/java/com/nl2sql/schema/entity/TableListCache.java \
        schema-service/src/main/java/com/nl2sql/schema/repository/SchemaCacheRepository.java \
        schema-service/src/main/java/com/nl2sql/schema/repository/TableListCacheRepository.java \
        schema-service/src/test/java/com/nl2sql/schema/repository/SchemaCacheRepositoryTest.java
git commit -m "feat(schema): 新增 schema_cache/table_list_cache 实体与 repository"
```

---

## Task 8: `SchemaScanService` 编排（解密 + 扫描 + 持久化）

**Files:**
- Create: `schema-service/src/main/java/com/nl2sql/schema/service/SchemaScanService.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/service/SchemaScanServiceTest.java`

- [ ] **Step 1: 写失败测试（Mockito，验证编排）**

```java
package com.nl2sql.schema.service;

import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.entity.TableListCache;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import com.nl2sql.schema.scanner.DatabaseScanner;
import com.nl2sql.schema.scanner.ScannerRegistry;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import com.nl2sql.schema.scanner.model.TableMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SchemaScanService - 扫描编排")
@ExtendWith(MockitoExtension.class)
class SchemaScanServiceTest {

    @Mock DataSourceRepository dataSourceRepository;
    @Mock SchemaCacheRepository schemaCacheRepository;
    @Mock TableListCacheRepository tableListCacheRepository;
    @Mock ScannerRegistry scannerRegistry;
    @Mock DatabaseScanner scanner;

    SchemaScanService service;

    @BeforeEach
    void setUp() {
        service = new SchemaScanService(dataSourceRepository, schemaCacheRepository,
                tableListCacheRepository, scannerRegistry, new ObjectMapper());
    }

    private DataSourceConfig ds() {
        DataSourceConfig d = new DataSourceConfig();
        d.setId(1L);
        d.setType("mysql");
        d.setHost("localhost");
        d.setPort(3306);
        d.setDatabaseName("shop");
        d.setUsername("u");
        d.setPasswordEncrypted("plainpwd"); // 非 ENC()，按明文处理，避免依赖环境密钥
        return d;
    }

    private SchemaMetadata oneTable() {
        TableMetadata t = new TableMetadata();
        t.setName("users");
        t.setComment("用户表");
        SchemaMetadata m = new SchemaMetadata();
        m.getTables().add(t);
        return m;
    }

    @Test
    @DisplayName("scan 解析类型→调度 scanner→持久化 schema_cache 与 table_list_cache，返回表名")
    void shouldScanAndPersist() {
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(ds()));
        when(scannerRegistry.resolve(DbType.MYSQL)).thenReturn(scanner);
        when(scanner.scan(any())).thenReturn(oneTable());
        when(schemaCacheRepository.findByDataSourceIdAndTableName(1L, "users"))
                .thenReturn(Optional.empty());
        when(schemaCacheRepository.findByDataSourceId(1L)).thenReturn(List.of());

        List<String> tables = service.scan(1L);

        assertThat(tables).containsExactly("users");
        ArgumentCaptor<SchemaCache> cap = ArgumentCaptor.forClass(SchemaCache.class);
        verify(schemaCacheRepository).save(cap.capture());
        assertThat(cap.getValue().getTableName()).isEqualTo("users");
        assertThat(cap.getValue().getVersion()).isEqualTo(1);
        verify(tableListCacheRepository).save(any(TableListCache.class));
    }

    @Test
    @DisplayName("重扫描已存在的表 version 自增")
    void shouldBumpVersionOnRescan() {
        SchemaCache existing = new SchemaCache();
        existing.setDataSourceId(1L);
        existing.setTableName("users");
        existing.setVersion(3);
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(ds()));
        when(scannerRegistry.resolve(DbType.MYSQL)).thenReturn(scanner);
        when(scanner.scan(any())).thenReturn(oneTable());
        when(schemaCacheRepository.findByDataSourceIdAndTableName(1L, "users"))
                .thenReturn(Optional.of(existing));
        when(schemaCacheRepository.findByDataSourceId(1L)).thenReturn(List.of(existing));

        service.scan(1L);

        assertThat(existing.getVersion()).isEqualTo(4);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl schema-service test -Dtest=SchemaScanServiceTest`
Expected: FAIL（`SchemaScanService` 不存在）

- [ ] **Step 3: 实现 `SchemaScanService`**

```java
package com.nl2sql.schema.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.common.encrypt.SecureConfigEncryptor;
import com.nl2sql.common.enums.IEnum;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.entity.TableListCache;
import com.nl2sql.schema.enums.DbType;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import com.nl2sql.schema.scanner.ScanContext;
import com.nl2sql.schema.scanner.ScannerRegistry;
import com.nl2sql.schema.scanner.model.SchemaMetadata;
import com.nl2sql.schema.scanner.model.TableMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 扫描编排：解密连接信息 → 调度 scanner → 覆盖式持久化。 */
@Service
public class SchemaScanService {

    private final DataSourceRepository dataSourceRepository;
    private final SchemaCacheRepository schemaCacheRepository;
    private final TableListCacheRepository tableListCacheRepository;
    private final ScannerRegistry scannerRegistry;
    private final ObjectMapper objectMapper;

    public SchemaScanService(DataSourceRepository dataSourceRepository,
                             SchemaCacheRepository schemaCacheRepository,
                             TableListCacheRepository tableListCacheRepository,
                             ScannerRegistry scannerRegistry,
                             ObjectMapper objectMapper) {
        this.dataSourceRepository = dataSourceRepository;
        this.schemaCacheRepository = schemaCacheRepository;
        this.tableListCacheRepository = tableListCacheRepository;
        this.scannerRegistry = scannerRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<String> scan(Long dataSourceId) {
        DataSourceConfig ds = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));

        DbType type = IEnum.of(DbType.class, ds.getType())
                .orElseThrow(() -> new BaseException(SchemaResultCode.DB_TYPE_UNSUPPORTED));

        ScanContext ctx = new ScanContext(type, ds.getHost(), ds.getPort(),
                ds.getDatabaseName(), ds.getUsername(), decrypt(ds.getPasswordEncrypted()));

        SchemaMetadata meta = scannerRegistry.resolve(type).scan(ctx);

        persist(dataSourceId, meta);

        return meta.getTables().stream().map(TableMetadata::getName).toList();
    }

    private void persist(Long dataSourceId, SchemaMetadata meta) {
        Set<String> scanned = meta.getTables().stream()
                .map(TableMetadata::getName).collect(Collectors.toSet());

        // 1. upsert 每张表
        LocalDateTime now = LocalDateTime.now();
        for (TableMetadata t : meta.getTables()) {
            SchemaCache cache = schemaCacheRepository
                    .findByDataSourceIdAndTableName(dataSourceId, t.getName())
                    .orElseGet(() -> {
                        SchemaCache c = new SchemaCache();
                        c.setDataSourceId(dataSourceId);
                        c.setTableName(t.getName());
                        c.setVersion(0);
                        return c;
                    });
            cache.setTableComment(t.getComment());
            cache.setColumnJson(toJson(t.getColumns()));
            cache.setPrimaryKeyJson(toJson(t.getPrimaryKeys()));
            cache.setForeignKeyJson(toJson(t.getForeignKeys()));
            cache.setIndexJson(toJson(t.getIndexes()));
            cache.setRowEstimate(t.getRowEstimate());
            cache.setVersion(cache.getVersion() == null ? 1 : cache.getVersion() + 1);
            cache.setCachedAt(now);
            schemaCacheRepository.save(cache);
        }

        // 2. 清理目标库已删除的表
        List<SchemaCache> stale = schemaCacheRepository.findByDataSourceId(dataSourceId).stream()
                .filter(c -> !scanned.contains(c.getTableName()))
                .toList();
        if (!stale.isEmpty()) {
            schemaCacheRepository.deleteAll(stale);
        }

        // 3. upsert 表名列表
        TableListCache list = tableListCacheRepository.findByDataSourceId(dataSourceId)
                .orElseGet(() -> {
                    TableListCache l = new TableListCache();
                    l.setDataSourceId(dataSourceId);
                    return l;
                });
        list.setTableJson(toJson(scanned.stream().sorted().toList()));
        list.setCachedAt(now);
        tableListCacheRepository.save(list);
    }

    private String decrypt(String stored) {
        if (SecureConfigEncryptor.isEncrypted(stored)) {
            return SecureConfigEncryptor.decrypt(stored, SecureConfigEncryptor.getKeyFromEnv());
        }
        return stored;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BaseException(SchemaResultCode.SCAN_EXECUTE_FAILED,
                    SchemaResultCode.SCAN_EXECUTE_FAILED.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl schema-service test -Dtest=SchemaScanServiceTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/service/SchemaScanService.java \
        schema-service/src/test/java/com/nl2sql/schema/service/SchemaScanServiceTest.java
git commit -m "feat(schema): 新增 SchemaScanService 编排扫描与覆盖式持久化"
```

---

## Task 9: `TableSchemaDTO` 扩展 + `DataSourceService` 改读持久化

**Files:**
- Modify: `schema-service/src/main/java/com/nl2sql/schema/dto/TableSchemaDTO.java`
- Modify: `schema-service/src/main/java/com/nl2sql/schema/service/DataSourceService.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/service/DataSourceServiceTest.java`（追加用例）

- [ ] **Step 1: 扩展 `TableSchemaDTO`**

完整替换为：
```java
package com.nl2sql.schema.dto;

import lombok.Data;

import java.util.List;

@Data
public class TableSchemaDTO {
    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;
    private List<String> primaryKeys;
    private List<IndexInfo> indexes;
    private List<ForeignKeyInfo> foreignKeys;

    @Data
    public static class ColumnInfo {
        private String name;
        private String type;
        private String comment;
        private boolean nullable;
        private String defaultValue;
    }

    @Data
    public static class IndexInfo {
        private String name;
        private boolean unique;
        private List<String> columns;
    }

    @Data
    public static class ForeignKeyInfo {
        private String name;
        private List<String> columns;
        private String referencedTable;
        private List<String> referencedColumns;
    }
}
```

- [ ] **Step 2: 写失败测试（追加到 `DataSourceServiceTest`）**

在现有 `DataSourceServiceTest` 类中追加字段与用例（保留现有 `repository` mock 与测试）：

```java
    @Mock
    private com.nl2sql.schema.repository.SchemaCacheRepository schemaCacheRepository;

    @Mock
    private com.nl2sql.schema.repository.TableListCacheRepository tableListCacheRepository;

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("getTableDetail 从 schema_cache 反序列化组装 DTO")
    void shouldAssembleDetailFromCache() throws Exception {
        com.nl2sql.schema.entity.SchemaCache cache = new com.nl2sql.schema.entity.SchemaCache();
        cache.setTableName("users");
        cache.setTableComment("用户表");
        cache.setColumnJson("[{\"name\":\"id\",\"type\":\"bigint\",\"comment\":\"主键\",\"nullable\":false,\"defaultValue\":null,\"ordinalPosition\":1}]");
        cache.setPrimaryKeyJson("[\"id\"]");
        cache.setIndexJson("[]");
        cache.setForeignKeyJson("[]");
        when(schemaCacheRepository.findByDataSourceIdAndTableName(1L, "users"))
                .thenReturn(java.util.Optional.of(cache));

        TableSchemaDTO dto = service.getTableDetail(1L, "users");

        assertThat(dto.getTableName()).isEqualTo("users");
        assertThat(dto.getTableComment()).isEqualTo("用户表");
        assertThat(dto.getColumns()).hasSize(1);
        assertThat(dto.getColumns().get(0).getName()).isEqualTo("id");
        assertThat(dto.getPrimaryKeys()).containsExactly("id");
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("scanTables 从 table_list_cache 反序列化表名")
    void shouldReadTablesFromCache() {
        com.nl2sql.schema.entity.TableListCache list = new com.nl2sql.schema.entity.TableListCache();
        list.setTableJson("[\"users\",\"orders\"]");
        when(tableListCacheRepository.findByDataSourceId(1L))
                .thenReturn(java.util.Optional.of(list));

        assertThat(service.scanTables(1L)).containsExactly("users", "orders");
    }
```

并把现有 `@InjectMocks private DataSourceService service;` 保留——Mockito 会把新增的两个 `@Mock` 一并注入。

- [ ] **Step 3: 运行测试确认失败**

Run: `mvn -q -pl schema-service test -Dtest=DataSourceServiceTest`
Expected: FAIL（构造参数不匹配 / 方法仍返回 Mock）

- [ ] **Step 4: 改写 `DataSourceService`**

完整替换为（保留 create/list/delete，重写 scanTables/getTableDetail 改读缓存）：
```java
package com.nl2sql.schema.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nl2sql.common.cache.CacheNames;
import com.nl2sql.common.exception.BaseException;
import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.entity.SchemaCache;
import com.nl2sql.schema.exception.SchemaResultCode;
import com.nl2sql.schema.repository.DataSourceRepository;
import com.nl2sql.schema.repository.SchemaCacheRepository;
import com.nl2sql.schema.repository.TableListCacheRepository;
import com.nl2sql.schema.scanner.model.ColumnMetadata;
import com.nl2sql.schema.scanner.model.ForeignKeyMetadata;
import com.nl2sql.schema.scanner.model.IndexMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceRepository repository;
    private final SchemaCacheRepository schemaCacheRepository;
    private final TableListCacheRepository tableListCacheRepository;
    private final ObjectMapper objectMapper;

    @CacheEvict(cacheNames = CacheNames.DS_LIST, allEntries = true)
    public DataSourceConfig create(DataSourceConfig config) {
        return repository.save(config);
    }

    @Cacheable(cacheNames = CacheNames.DS_LIST)
    public List<DataSourceConfig> list() {
        return repository.findAll();
    }

    @CacheEvict(cacheNames = CacheNames.DS_LIST, allEntries = true)
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Cacheable(cacheNames = CacheNames.SCHEMA_TABLES, key = "#dataSourceId")
    public List<String> scanTables(Long dataSourceId) {
        return tableListCacheRepository.findByDataSourceId(dataSourceId)
                .map(c -> readList(c.getTableJson(), new TypeReference<List<String>>() {}))
                .orElseGet(List::of);
    }

    @Cacheable(cacheNames = CacheNames.SCHEMA_TABLE, key = "#dataSourceId + ':' + #tableName")
    public TableSchemaDTO getTableDetail(Long dataSourceId, String tableName) {
        SchemaCache cache = schemaCacheRepository
                .findByDataSourceIdAndTableName(dataSourceId, tableName)
                .orElseThrow(() -> new BaseException(SchemaResultCode.DATASOURCE_NOT_FOUND));
        return toDto(cache);
    }

    private TableSchemaDTO toDto(SchemaCache cache) {
        TableSchemaDTO dto = new TableSchemaDTO();
        dto.setTableName(cache.getTableName());
        dto.setTableComment(cache.getTableComment());
        dto.setPrimaryKeys(readList(cache.getPrimaryKeyJson(), new TypeReference<List<String>>() {}));
        dto.setColumns(readList(cache.getColumnJson(), new TypeReference<List<ColumnMetadata>>() {})
                .stream().map(this::toColumnInfo).toList());
        dto.setIndexes(readList(cache.getIndexJson(), new TypeReference<List<IndexMetadata>>() {})
                .stream().map(this::toIndexInfo).toList());
        dto.setForeignKeys(readList(cache.getForeignKeyJson(), new TypeReference<List<ForeignKeyMetadata>>() {})
                .stream().map(this::toFkInfo).toList());
        return dto;
    }

    private TableSchemaDTO.ColumnInfo toColumnInfo(ColumnMetadata m) {
        TableSchemaDTO.ColumnInfo c = new TableSchemaDTO.ColumnInfo();
        c.setName(m.getName());
        c.setType(m.getType());
        c.setComment(m.getComment());
        c.setNullable(m.isNullable());
        c.setDefaultValue(m.getDefaultValue());
        return c;
    }

    private TableSchemaDTO.IndexInfo toIndexInfo(IndexMetadata m) {
        TableSchemaDTO.IndexInfo i = new TableSchemaDTO.IndexInfo();
        i.setName(m.getName());
        i.setUnique(m.isUnique());
        i.setColumns(m.getColumns());
        return i;
    }

    private TableSchemaDTO.ForeignKeyInfo toFkInfo(ForeignKeyMetadata m) {
        TableSchemaDTO.ForeignKeyInfo f = new TableSchemaDTO.ForeignKeyInfo();
        f.setName(m.getName());
        f.setColumns(m.getColumns());
        f.setReferencedTable(m.getReferencedTable());
        f.setReferencedColumns(m.getReferencedColumns());
        return f;
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> ref) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, ref);
        } catch (JsonProcessingException e) {
            throw new BaseException(SchemaResultCode.SCAN_EXECUTE_FAILED,
                    SchemaResultCode.SCAN_EXECUTE_FAILED.getMessage(), e);
        }
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -q -pl schema-service test -Dtest=DataSourceServiceTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/dto/TableSchemaDTO.java \
        schema-service/src/main/java/com/nl2sql/schema/service/DataSourceService.java \
        schema-service/src/test/java/com/nl2sql/schema/service/DataSourceServiceTest.java
git commit -m "feat(schema): TableSchemaDTO 扩展关系信息，DataSourceService 改读持久化缓存"
```

---

## Task 10: Controller 接入真实扫描 + 缓存失效

**Files:**
- Modify: `schema-service/src/main/java/com/nl2sql/schema/controller/DataSourceController.java`
- Test: `schema-service/src/test/java/com/nl2sql/schema/controller/DataSourceControllerTest.java`（追加 scan 用例）

- [ ] **Step 1: 写失败测试（追加到 `DataSourceControllerTest`）**

先查看现有测试的注入风格（MockMvc 还是直接调用），按相同风格追加：

```java
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("POST /scan/{id} 调用 SchemaScanService.scan 并返回表名")
    void shouldTriggerRealScan() {
        when(scanService.scan(1L)).thenReturn(java.util.List.of("users", "orders"));

        com.nl2sql.common.R<java.util.List<String>> r = controller.scan(1L);

        assertThat(r.getData()).containsExactly("users", "orders");
        verify(scanService).scan(1L);
    }
```
（若现有测试用 `@InjectMocks DataSourceController controller`，追加 `@Mock SchemaScanService scanService;`。）

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl schema-service test -Dtest=DataSourceControllerTest`
Expected: FAIL（controller 未注入 `SchemaScanService`）

- [ ] **Step 3: 改写 `DataSourceController`**

完整替换为：
```java
package com.nl2sql.schema.controller;

import com.nl2sql.common.R;
import com.nl2sql.common.cache.CacheNames;
import com.nl2sql.schema.dto.TableSchemaDTO;
import com.nl2sql.schema.entity.DataSourceConfig;
import com.nl2sql.schema.service.DataSourceService;
import com.nl2sql.schema.service.SchemaScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService service;
    private final SchemaScanService scanService;

    @PostMapping("/datasource")
    public R<DataSourceConfig> add(@RequestBody DataSourceConfig config) {
        return R.ok(service.create(config));
    }

    @GetMapping("/datasource/list")
    public R<List<DataSourceConfig>> list() {
        return R.ok(service.list());
    }

    @DeleteMapping("/datasource/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    /** 触发真实扫描并持久化；清除该数据源的表列表与表详情缓存。 */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.SCHEMA_TABLES, key = "#datasourceId"),
            @CacheEvict(cacheNames = CacheNames.SCHEMA_TABLE, allEntries = true)
    })
    @PostMapping("/scan/{datasourceId}")
    public R<List<String>> scan(@PathVariable Long datasourceId) {
        return R.ok(scanService.scan(datasourceId));
    }

    @GetMapping("/{datasourceId}/tables")
    public R<List<String>> tables(@PathVariable Long datasourceId) {
        return R.ok(service.scanTables(datasourceId));
    }

    @GetMapping("/{datasourceId}/tables/{tableName}")
    public R<TableSchemaDTO> tableDetail(@PathVariable Long datasourceId, @PathVariable String tableName) {
        return R.ok(service.getTableDetail(datasourceId, tableName));
    }
}
```

注意：`@CacheEvict` 在同类自调用场景无效，但此处由 Spring 代理外部调用 controller 方法，注解生效。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl schema-service test -Dtest=DataSourceControllerTest`
Expected: PASS

- [ ] **Step 5: 全量回归**

Run: `mvn -q -pl schema-service test`
Expected: 全部 PASS（含集成测试，需 Docker）

- [ ] **Step 6: 提交**

```bash
git add schema-service/src/main/java/com/nl2sql/schema/controller/DataSourceController.java \
        schema-service/src/test/java/com/nl2sql/schema/controller/DataSourceControllerTest.java
git commit -m "feat(schema): scan 端点接入真实扫描并失效相关缓存"
```

---

## Task 11: 端到端构建校验

- [ ] **Step 1: 全工程构建**

Run: `mvn -q -DskipTests=false clean install -pl common,schema-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 2: 确认无构建产物入库**

Run: `git status --porcelain`
Expected: 无 `target/`、`*.class` 等被跟踪

---

## Self-Review

**Spec coverage:**
- §4.1 DbType → Task 1 ✓
- §4.2 ScanContext → Task 4 ✓
- §4.3 DatabaseScanner → Task 5 ✓
- §4.4 ScannerRegistry → Task 5 ✓
- §4.5 JDBC URL（内聚 scanner 内） → Task 6 ✓
- §5 元数据模型 → Task 3 ✓
- §6 持久化（schema_cache/table_list_cache、JSON、覆盖式 upsert、version） → Task 7、Task 8 ✓
- §7 连接与安全（解密、短连接、超时） → Task 6（URL 超时）、Task 8（解密） ✓
- §8 MySQL information_schema SQL → Task 6 ✓
- §9 Service/Controller 改造 → Task 9、Task 10 ✓
- §10 错误处理（SchemaResultCode + i18n） → Task 2、各 service ✓
- §11 缓存失效 → Task 10 ✓
- §12 测试策略 → 各 Task 内含 ✓

**Placeholder scan:** 无 TBD/TODO；所有代码步骤含完整代码。

**Type consistency:**
- `SchemaScanService` 构造参数顺序（dataSourceRepository, schemaCacheRepository, tableListCacheRepository, scannerRegistry, objectMapper）在 Task 8 测试与实现一致 ✓
- `ColumnMetadata`/`IndexMetadata`/`ForeignKeyMetadata` 字段名在 Task 3 定义，Task 6 填充、Task 9 读取一致 ✓
- `SchemaCacheRepository.findByDataSourceIdAndTableName` / `findByDataSourceId` 在 Task 7 定义，Task 8/9 调用一致 ✓
- `DbType.MYSQL`、`SchemaResultCode.*` 跨任务引用一致 ✓

**风险备注：** Task 6 集成测试依赖本机 Docker。若执行环境无 Docker，该测试会失败/报错——执行时若无 Docker，可临时 `-Dtest=!MySqlDatabaseScannerIT` 跳过它跑其余单测，但 Task 6 实现仍须完成。
