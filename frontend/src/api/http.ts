import { ApiResult } from './types';

/** 网关基址，开发期由 Vite proxy 转发到 http://localhost:8080 */
const BASE_URL = '/api';

/** 后端业务异常：携带 code，便于上层区分处理 */
export class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  params?: Record<string, string | number | undefined>;
  /** 纯文本请求体（如 /query/sql 直接收 String），跳过 JSON.stringify */
  rawText?: boolean;
  signal?: AbortSignal;
}

function buildUrl(path: string, params?: RequestOptions['params']): string {
  const url = BASE_URL + path;
  if (!params) return url;
  const qs = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&');
  return qs ? `${url}?${qs}` : url;
}

/** 统一请求封装：自动解包 R<T>，非 200 业务码抛 ApiError */
async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, params, rawText, signal } = opts;

  const headers: Record<string, string> = {};
  let payload: BodyInit | undefined;
  if (body !== undefined) {
    if (rawText) {
      headers['Content-Type'] = 'text/plain';
      payload = String(body);
    } else {
      headers['Content-Type'] = 'application/json';
      payload = JSON.stringify(body);
    }
  }

  let resp: Response;
  try {
    resp = await fetch(buildUrl(path, params), { method, headers, body: payload, signal });
  } catch (e) {
    if ((e as Error).name === 'AbortError') throw e;
    throw new ApiError(-1, '网络请求失败，请检查后端服务是否已启动');
  }

  if (!resp.ok) {
    throw new ApiError(resp.status, `HTTP ${resp.status} ${resp.statusText}`);
  }

  const json = (await resp.json()) as ApiResult<T>;
  if (json.code !== 200) {
    throw new ApiError(json.code, json.message || '请求失败');
  }
  return json.data;
}

export const http = {
  get: <T>(path: string, params?: RequestOptions['params'], signal?: AbortSignal) =>
    request<T>(path, { method: 'GET', params, signal }),
  post: <T>(path: string, body?: unknown, opts?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...opts, method: 'POST', body }),
  del: <T>(path: string, params?: RequestOptions['params']) =>
    request<T>(path, { method: 'DELETE', params }),
};
