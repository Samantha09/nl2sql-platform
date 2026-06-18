import { ChartType, KBItem, Series } from '../types';
import { QueryResult } from '../api/types';

/** 查询结果视图模型：mock 命中与后端返回统一归一到此形状，供 QueryView 渲染 */
export interface ResultVM {
  intent: string;
  tables: string[];
  sql: string;
  cols: string[];
  rows: (string | number)[][];
  chart: ChartType;
  data: Series[];
  /** 数据来源：real=后端真实返回，mock=本地兜底 */
  source: 'real' | 'mock';
}

const CHART_TYPES: ChartType[] = ['line', 'bar', 'donut'];
const asChart = (t?: string): ChartType =>
  CHART_TYPES.includes(t as ChartType) ? (t as ChartType) : 'bar';

const isNum = (v: unknown): v is number => typeof v === 'number';

/** 从后端行数据派生图表 Series：取首个文本列作标签、首个数值列作值 */
function deriveSeries(cols: string[], rows: (string | number)[][]): Series[] {
  if (!cols.length || !rows.length) return [];
  const labelIdx = 0;
  const valueIdx = rows[0].findIndex((v, i) => i !== labelIdx && isNum(v));
  if (valueIdx < 0) return [];
  return rows.map(r => ({ label: String(r[labelIdx]), value: Number(r[valueIdx]) }));
}

/** mock 命中的 KBItem → ResultVM */
export function fromKB(hit: KBItem): ResultVM {
  return {
    intent: hit.intent,
    tables: hit.tables,
    sql: hit.sql,
    cols: hit.cols,
    rows: hit.rows,
    chart: hit.chart,
    data: hit.data,
    source: 'mock',
  };
}

/** 后端 QueryResult → ResultVM */
export function fromQueryResult(res: QueryResult): ResultVM {
  const cols = res.data.length ? Object.keys(res.data[0]) : [];
  const rows = res.data.map(row => cols.map(c => row[c] as string | number));
  return {
    intent: '查询结果',
    tables: [],
    sql: res.sql,
    cols,
    rows,
    chart: asChart(res.chartType),
    data: deriveSeries(cols, rows),
    source: 'real',
  };
}
