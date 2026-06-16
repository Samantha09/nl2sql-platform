# Schema 查看器页面设计

## 目标

提供一个可视化界面浏览已连接数据源的 Schema 信息，支持 MySQL、ClickHouse、PostgreSQL 等多种数据源类型。

## 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│  Schema 查看器          [数据源 ▼]  [🔄 刷新 Schema]        │
├──────────────┬──────────────────────────────────────────────┤
│  🔍 搜索表... │  表名: orders                                │
│              │  注释: 订单表                                 │
│  📁 数据源    │                                              │
│  ├─ mysql_1  │  主键: id                                    │
│  └─ click_1  │                                              │
│              │  📋 字段信息                                  │
│  表列表       │  | 字段名 | 类型 | 可空 | 默认值 | 注释 |      │
│  ├─ users    │                                              │
│  ├─ orders   │  🔑 索引信息                                  │
│  └─ products │  | 索引名 | 字段 | 是否唯一 |                 │
└──────────────┴──────────────────────────────────────────────┘
```

## 数据结构

### 数据源 DataSource

```typescript
interface DataSource {
  id: number
  name: string
  type: 'mysql' | 'clickhouse' | 'postgresql'
  host: string
  port: number
  databaseName: string
}
```

### 表信息 TableInfo

```typescript
interface TableInfo {
  name: string
  comment?: string
  rowCount?: number
}
```

### 字段信息 ColumnInfo

```typescript
interface ColumnInfo {
  name: string
  type: string
  comment?: string
  nullable: boolean
  defaultValue?: string
  isPrimaryKey: boolean
}
```

### 索引信息 IndexInfo

```typescript
interface IndexInfo {
  name: string
  columns: string[]
  unique: boolean
}
```

### 表详情 TableSchema

```typescript
interface TableSchema {
  tableName: string
  tableComment?: string
  columns: ColumnInfo[]
  primaryKeys: string[]
  indexes: IndexInfo[]
  foreignKeys?: ForeignKeyInfo[]
}

interface ForeignKeyInfo {
  name: string
  column: string
  referencedTable: string
  referencedColumn: string
}
```

## 交互

1. **数据源切换**：顶部下拉选择已配置的数据源，切换后重新加载表列表。
2. **表搜索**：左侧搜索框实时过滤表名。
3. **表选择**：点击表名，右侧展示详情。
4. **刷新**：点击刷新按钮触发后端重新扫描 Schema。

## 后端配合（明天）

- 扩展 `TableSchemaDTO` 增加 `indexes`、`foreignKeys`、`nullable`、`defaultValue` 等字段
- `DataSourceConfig` 增加 `type` 字段支持多种数据库
- `schema-service` 根据 `type` 使用对应 JDBC 驱动扫描真实 Schema
- 新增 `GET /api/schema/{datasourceId}/tables` 获取表列表
- 新增/改造 `GET /api/schema/{datasourceId}/tables/{tableName}` 返回完整表详情
