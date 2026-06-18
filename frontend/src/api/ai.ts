import { http } from './http';
import { ConvertRequest } from './types';

/** 自然语言转 SQL（不执行） */
export const convert = (req: ConvertRequest) =>
  http.post<string>('/ai/convert', req);

/** SQL 安全校验 */
export const validate = (sql: string) =>
  http.post<boolean>('/ai/validate', sql, { rawText: true });
