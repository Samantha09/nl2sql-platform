# 数据库结构扫描功能设计规格

- 日期：2026-06-21
- 模块：schema-service（依赖 common）
- 分支：feat/nacos-config-encryption（后续可拆独立分支实现）

## 1. 背景与目标

`schema-service` 当前的 `DataSourceService.scanTables()` / `getTableDetail()` 返回 Mock 数据。
本功能用真实的多数据库结构扫描替换 Mock：连接用户配置的目标库，抽取表/列/主键/外键/索引/唯一约束等元数据，
并持久化到平台自身数据库，供 ai-service 生成 SQL、query-service 校验、前端展示使用。

设计目标（按优先级）：

1. **可扩展**：新增一种数据库 = 新增一个实现类，对既有代码零侵入。
2. **可读**：抽象层契约清晰，方言专属逻辑各自内聚，下游永不感知方言差异。
3. **规范一致**：遵循 `CLAUDE.md` 约定——枚举实现 `IEnum<C>`、异常走 `BaseException + IResultCode`、
   文案进 i18n、常量集中，严禁硬编码魔法值。

## 2. 范围

**本期落地**：
- MySQL 一种数据库的完整实现。
- 完整关系元数据：表（名+注释）、列（名/类型/注释/可空/默认值/序号/是否主键）、主键、外键、索引、唯一约束。
- 按需短连接（每次扫描建连、用完即关）。
- 扫描结果持久化到平台库，重扫描覆盖式更新。

**抽象但本期不实现**：PostgreSQL / Oracle / SQL Server / 达梦等——仅保证 SPI 接口能平滑接入。

**明确不做（YAGNI）**：
- 连接池缓存（按需短连接已够，扫描为低频操作）。
- 增量 diff / schema 版本历史（持久化为覆盖式，后续可在此模型上叠加）。
- 业务语义标注（持久化模型预留，不在本期实现 UI/接口）。
- 通用 `DatabaseMetaData` 兜底基类（方案 C，当前仅 MySQL 用不上）。

## 3. 整体结构

```
com.nl2sql.schema
├── enums
│   └── DbType.java                 数据库类型枚举（implements IEnum<String>）
├── exception
│   └── SchemaResultCode.java       schema-service 领域错误码（implements IResultCode）
├── scanner                         ← 抽象层
│   ├── DatabaseScanner.java        SPI 接口：一种数据库一份实现
│   ├── ScannerRegistry.java        按 DbType 分发，Spring 自动收集实现
│   ├── ScanContext.java            一次扫描的输入（连接信息 + 已解密密码）
│   ├── model/                      方言无关的元数据模型（抽象层"通用语言"）
│   │   ├── SchemaMetadata.java
│   │   ├── TableMetadata.java
│   │   ├── ColumnMetadata.java
│   │   ├── IndexMetadata.java
│   │   └── ForeignKeyMetadata.java
│   └── impl
│       └── MySqlDatabaseScanner.java   方言专属 SQL 实现（本期唯一落地）
├── entity                          ← 持久化层（对齐已存在的 schema.sql 表）
│   ├── DataSourceConfig.java       （已存在，type 字段语义即 DbType.code）
│   ├── SchemaCache.java            映射 schema_cache：每(数据源,表)一行，元数据存 JSON 列
│   └── TableListCache.java         映射 table_list_cache：每数据源一行表名列表 JSON
├── repository
│   ├── SchemaCacheRepository.java
│   └── TableListCacheRepository.java
├── service
│   ├── DataSourceService.java      （已存在，scanTables/getTableDetail 改为真实实现）
│   └── SchemaScanService.java      编排：解析数据源→调度 scanner→持久化
└── controller
    └── DataSourceController.java    （已存在，端点签名基本不变）
```

## 4. 抽象层（核心契约）

### 4.1 `DbType` 枚举

```java
public enum DbType implements IEnum<String> {
    MYSQL("mysql", "MySQL");
    // PostgreSQL/Oracle 等后续追加

    private final String code;
    private final String desc;
    // 构造、getCode、getDesc
    // 静态 of(String code) 复用 IEnum.of(DbType.class, code)
}
```

`DataSourceConfig.type`（String）的取值即 `DbType.code`。读取时用 `IEnum.of(DbType.class, type)`
反查，查不到抛 `BaseException(SchemaResultCode.DB_TYPE_UNSUPPORTED)`。

### 4.2 `ScanContext`（扫描输入）

不可变值对象，承载一次扫描所需的全部连接信息。密码在进入 scanner 前**已解密**——
scanner 不接触加密细节，解密由 `SchemaScanService` 用 `SecureConfigEncryptor` 完成。

```java
public record ScanContext(
    DbType type, String host, int port, String databaseName,
    String username, String password   // 明文，已解密
) {}
```

### 4.3 `DatabaseScanner`（SPI 接口）

```java
public interface DatabaseScanner {
    boolean supports(DbType type);
    SchemaMetadata scan(ScanContext context);     // 全量：表+列+主键+外键+索引
    List<String> listTables(ScanContext context);  // 轻量：仅表名
}
```

### 4.4 `ScannerRegistry`（分发）

```java
@Component
public class ScannerRegistry {
    private final List<DatabaseScanner> scanners;  // Spring 注入所有 @Component 实现
    public DatabaseScanner resolve(DbType type) {
        return scanners.stream().filter(s -> s.supports(type)).findFirst()
            .orElseThrow(() -> new BaseException(SchemaResultCode.DB_TYPE_UNSUPPORTED));
    }
}
```

### 4.5 JDBC URL 拼接

JDBC URL 拼法是方言差异点（MySQL `jdbc:mysql://h:p/db?...`，PG `jdbc:postgresql://...`）。
**决策**：作为各方言 scanner 实现内部的私有方法，不单列接口/类——URL 拼接与该方言的连接逻辑强相关，
内聚在实现类里更可读。

## 5. 元数据模型（方言无关，scanner 返回值）

纯 POJO（Lombok `@Data`），不带 JPA 注解，与持久化实体解耦：

```java
class SchemaMetadata { List<TableMetadata> tables; }

class TableMetadata {
    String name; String comment;
    List<ColumnMetadata> columns;
    List<String> primaryKeys;            // 列名，按序
    List<IndexMetadata> indexes;
    List<ForeignKeyMetadata> foreignKeys;
}

class ColumnMetadata {
    String name; String type;            // 原文类型，如 varchar(255)
    String comment; boolean nullable;
    String defaultValue; int ordinalPosition;
}

class IndexMetadata {
    String name; boolean unique;
    List<String> columns;                // 组成列，按序
}

class ForeignKeyMetadata {
    String name;
    List<String> columns;                // 本表列
    String referencedTable;
    List<String> referencedColumns;      // 引用表列，与 columns 一一对应
}
```

## 6. 持久化模型

**复用 `schema.sql` 中已预设计的两张表**（`ddl-auto: update` 已会维护，无需新增表）：

`schema_cache`（每「数据源 + 表」一行，元数据以 JSON 列存储）：

| 列 | 来源 |
|---|---|
| data_source_id, table_name | 定位键，唯一约束 `uk_ds_table` |
| table_comment | `TableMetadata.comment` |
| column_json | `List<ColumnMetadata>` 序列化 |
| primary_key_json | `List<String> primaryKeys` 序列化 |
| foreign_key_json | `List<ForeignKeyMetadata>` 序列化 |
| index_json | `List<IndexMetadata>` 序列化 |
| row_estimate | `information_schema.tables.table_rows`（MySQL 估算） |
| version | 每次重扫描该表 +1 |
| cached_at | 扫描时间戳 |

`table_list_cache`（每数据源一行）：`table_json` 存表名列表 JSON，`cached_at` 时间戳。

**JSON 序列化**：复用 Spring 容器里的 Jackson `ObjectMapper`。第 5 节的方言无关模型 POJO
直接序列化进 `*_json` 列，读出时反序列化回 POJO——元数据模型与持久化彻底解耦，
未来模型加字段不需改表结构。

**重扫描策略（覆盖式 upsert，事务内）**：
1. 对每张扫描到的表，按 `(data_source_id, table_name)` 查 `schema_cache`：存在则更新各 JSON 列并 `version+1`，
   不存在则插入 `version=1`。
2. 删除本次扫描结果中已不存在的表对应的 `schema_cache` 行（目标库已删表的清理）。
3. upsert `table_list_cache` 的表名列表。

整个过程 `@Transactional`，保证失败不留半截数据。

> 为何用 JSON 列而非正规化四表：NL2SQL 的访问模式是「取某表的完整 schema 整体喂给 LLM」，
> 整表读写恰好是 JSON blob 的强项；且该模型为 schema.sql 原作者预设计，遵循既有约定。
> 代价是无法按列做 SQL 查询——本期无此需求（YAGNI），未来若需要再正规化。

## 7. 连接与安全

- `DataSourceConfig.passwordEncrypted` 存的是 `ENC(...)` 密文（与 Nacos 配置加密同一套 `SecureConfigEncryptor`）。
- `SchemaScanService` 取数据源后：`SecureConfigEncryptor.isEncrypted(pwd)` 为真则
  `decrypt(pwd, SecureConfigEncryptor.getKeyFromEnv())`，否则视为明文（兼容旧数据）。
- 解密后的明文仅存在于 `ScanContext` 内存中，扫描结束即随对象回收，**不落库、不打日志**。
- 连接用 `DriverManager.getConnection(url, user, password)` 建短连接，`try-with-resources` 确保关闭。
- 连接超时：URL 上带 `connectTimeout`/`socketTimeout`（MySQL）防止扫描卡死。

## 8. MySQL 实现（`MySqlDatabaseScanner`）

`supports(DbType.MYSQL)`。URL：`jdbc:mysql://{host}:{port}/{db}?useInformationSchema=true&connectTimeout=5000&socketTimeout=30000`。
全部元数据查 `information_schema`（保证注释/类型原文/外键精确）：

- 表+注释：`SELECT table_name, table_comment FROM information_schema.tables WHERE table_schema=?`
- 列：`SELECT column_name, column_type, column_comment, is_nullable, column_default, ordinal_position, column_key FROM information_schema.columns WHERE table_schema=? AND table_name=? ORDER BY ordinal_position`（`column_key='PRI'` 即主键）
- 索引：`SELECT index_name, non_unique, seq_in_index, column_name FROM information_schema.statistics WHERE table_schema=? AND table_name=? ORDER BY index_name, seq_in_index`
- 外键：`SELECT constraint_name, column_name, referenced_table_name, referenced_column_name FROM information_schema.key_column_usage WHERE table_schema=? AND table_name=? AND referenced_table_name IS NOT NULL ORDER BY constraint_name, ordinal_position`

`table_schema` 参数即 `databaseName`。所有 SQL 用 `PreparedStatement` 参数化，杜绝注入。
表名等标识符仅来自库自身元数据、不来自用户输入，无拼接注入面。

## 9. Service 与 Controller 改造

- `SchemaScanService.scan(dataSourceId)`：查 `DataSourceConfig` → 解析 `DbType` → 解密 → 建 `ScanContext`
  → `registry.resolve(type).scan(ctx)` → 覆盖式 upsert 到 `schema_cache`/`table_list_cache` → 返回表名列表。
- `DataSourceService.scanTables(id)`：改为读 `table_list_cache`（已扫描则反序列化 `table_json`）；未扫描返回空列表。
- `DataSourceService.getTableDetail(id, tableName)`：查 `schema_cache` 行，反序列化各 JSON 列组装成
  `TableSchemaDTO`。`TableSchemaDTO` 需扩展字段以容纳外键/索引（在现有 `tableName/tableComment/columns/primaryKeys`
  基础上增加 `indexes`、`foreignKeys`），保持前端既有字段兼容、只做增量扩展。
- Controller 端点签名不变（`POST /scan/{id}`、`GET /{id}/tables`、`GET /{id}/tables/{tableName}`）；
  `POST /scan/{id}` 改为触发真实扫描+持久化。

## 10. 错误处理

新增 `SchemaResultCode implements IResultCode`，错误码用 schema-service 领域band `11xxx`（避开 common 的 HTTP 语义区间）：

| 枚举 | code | i18n key | 兜底中文 |
|---|---|---|---|
| DB_TYPE_UNSUPPORTED | 11001 | schema.db_type_unsupported | 不支持的数据库类型 |
| DATASOURCE_NOT_FOUND | 11002 | schema.datasource_not_found | 数据源不存在 |
| SCAN_CONNECT_FAILED | 11003 | schema.scan_connect_failed | 无法连接目标数据库 |
| SCAN_EXECUTE_FAILED | 11004 | schema.scan_execute_failed | 扫描执行失败 |

i18n 文案补进 `common/src/main/resources/i18n/messages_zh_CN.properties` 与 `_en.properties`。
连接/SQL 异常在 scanner 内捕获 `SQLException`，包成 `BaseException(SCAN_CONNECT_FAILED/SCAN_EXECUTE_FAILED, msg, cause)`，
由 `GlobalExceptionHandler` 统一转 `R`。**不向前端泄露原始 SQLException 堆栈**（仅记日志）。

## 11. 缓存

沿用现有 `CacheNames.SCHEMA_TABLES` / `SCHEMA_TABLE`。`POST /scan/{id}` 成功后需 `@CacheEvict`
清除该数据源的两个缓存区，保证下次读到最新结构。

## 12. 测试策略

- **`SchemaScanServiceTest`（单元，Mockito）**：mock `DatabaseScanner`/`ScannerRegistry`/repositories，
  验证编排逻辑——解密被调用、覆盖式删除+插入顺序、异常映射。
- **`ScannerRegistryTest`（单元）**：注入多个假 scanner，验证 `resolve` 命中/未命中抛 `DB_TYPE_UNSUPPORTED`。
- **`DbTypeTest`（单元）**：`of()` 反查命中/不命中。
- **`MySqlDatabaseScannerIT`（集成）**：MySQL 的 `information_schema` 行为 H2 无法等价模拟，
  故用 Testcontainers MySQL（新增 test 依赖）建真实库、灌入带注释/外键/索引的样例表，
  断言抽取出的 `SchemaMetadata` 正确。用 `@Testcontainers(disabledWithoutDocker = true)`，无 Docker 时优雅跳过。
- **持久化层**：用现有 H2 test 配置验证 `SchemaCacheRepository`/`TableListCacheRepository` 的 upsert
  与按 `(data_source_id, table_name)` 查询、JSON 列读写往返。
- 遵循项目 TDD：先写失败测试，再实现。

**实现期发现的两处环境适配（已落地）**：
- `application-test.yml` 加 `spring.sql.init.mode=never`：测试用 H2 由 Hibernate `ddl-auto` 依实体建表，
  不执行 MySQL 语法的 `schema.sql`（`CREATE DATABASE` 等在 H2 不兼容）。
- schema-service 的 surefire 配置固定 `api.version=1.40`：Testcontainers 1.19.7 内置 docker-java
  默认按 API 1.32 协商，新版 Docker Engine（≥29，最低 API 1.40）会拒绝。1.40 自 Docker 19.03 起广泛支持。

## 13. 建表 DDL

**无需新增表**：持久化复用 `schema-service/src/main/resources/schema.sql` 中已存在的 `schema_cache`
与 `table_list_cache`，`ddl-auto: update` 已维护。本期仅需为这两张表创建对应的 JPA 实体与 repository。
注意这两张表用自有的 `cached_at`/`version` 列、**没有** `BaseEntity` 的 `created_at`/`updated_at`，
故实体直接映射既有列、**不继承 `BaseEntity`**，避免 `ddl-auto` 给既有表加多余列。

## 14. 未来扩展点

- 新增数据库：实现 `DatabaseScanner` + `supports(新DbType)`，在 `DbType` 加枚举值，补 JDBC 驱动依赖。注册表与下游零改动。
- schema 版本/diff：在覆盖式持久化前快照旧版本。
- 业务语义标注：`schema_cache` 增标注 JSON 列，或旁挂独立标注表。
- 通用 `DatabaseMetaData` 兜底基类（方案 C）：作为 `DatabaseScanner` 的抽象基类，新方言无专属 SQL 时继承获得基线。
