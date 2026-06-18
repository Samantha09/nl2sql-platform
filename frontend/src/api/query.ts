import { http } from './http';
import { QueryRequest, QueryResult, QueryHistory } from './types';

/** 自然语言查询 → SQL 执行结果 */
export const nlQuery = (req: QueryRequest, signal?: AbortSignal) =>
  http.post<QueryResult>('/query/nl', req, { signal });

/** 直接执行 SQL */
export const sqlQuery = (sql: string) =>
  http.post<QueryResult>('/query/sql', sql, { rawText: true });

/** 按会话查询历史记录 */
export const getHistory = (conversationId: string) =>
  http.get<QueryHistory[]>('/query/history', { conversationId });
