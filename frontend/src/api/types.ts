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
}
