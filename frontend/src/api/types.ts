// 后端接口契约类型（对齐 common.R<T> 与各服务 DTO）

/** 统一响应包装，对应后端 com.nl2sql.common.R<T> */
export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

/** 对应 query-service QueryRequest */
export interface QueryRequest {
  userId?: number;
  dataSourceId?: number;
  naturalLanguage: string;
  conversationId?: string;
}

/** 对应 query-service QueryResult */
export interface QueryResult {
  sql: string;
  data: Array<Record<string, unknown>>;
  totalCount: number;
  executeTimeMs: number;
  chartType: string;
}

/** 对应 ai-service ConvertRequest */
export interface ConvertRequest {
  dataSourceId?: number;
  naturalLanguage: string;
}

/** 对应 query-service QueryHistory 实体 */
export interface QueryHistory {
  id: number;
  conversationId: string;
  userId: number;
  dataSourceId: number;
  naturalLanguage: string;
  generatedSql: string;
  sqlExecuted: string;
  executeTimeMs: number;
  resultCount: number;
  status: string;
  errorMessage?: string;
  chartType: string;
  createdAt: string;
}

/** 对应 schema-service DataSourceConfig 实体 */
export interface DataSourceConfig {
  id: number;
  name: string;
  type: string;
  host: string;
  port: number;
  databaseName: string;
  /** 用户名（列表接口会返回） */
  username?: string;
}

/** 新增数据源的表单入参（password 明文，后端按需加密存储） */
export interface DataSourceInput {
  name: string;
  type: string;
  host: string;
  port: number;
  databaseName: string;
  username: string;
  /** 明文密码，提交到后端 passwordEncrypted 字段 */
  passwordEncrypted: string;
}

/** 对应 schema-service TableSchemaDTO.ColumnInfo */
export interface ColumnInfo {
  name: string;
  type: string;
  comment: string;
  nullable: boolean;
  defaultValue: string | null;
}

/** 对应 schema-service TableSchemaDTO.IndexInfo */
export interface IndexInfo {
  name: string;
  unique: boolean;
  columns: string[];
}

/** 对应 schema-service TableSchemaDTO.ForeignKeyInfo */
export interface ForeignKeyInfo {
  name: string;
  columns: string[];
  referencedTable: string;
  referencedColumns: string[];
}

/** 对应 schema-service TableSchemaDTO（单表结构详情） */
export interface TableSchemaDTO {
  tableName: string;
  tableComment: string;
  columns: ColumnInfo[];
  primaryKeys: string[];
  indexes: IndexInfo[];
  foreignKeys: ForeignKeyInfo[];
}
