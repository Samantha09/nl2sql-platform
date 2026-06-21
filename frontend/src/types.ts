export type ChartType = 'line' | 'bar' | 'donut';
export type ViewKey = 'overview' | 'schema' | 'query' | 'charts';
export type SchemaMode = 'struct' | 'data';

export interface Series {
  label: string;
  value: number;
}

export interface Column {
  name: string;
  type: string;
  pk?: boolean;
  fk?: string;
  nullable: boolean;
  /** 列注释（真实扫描时可能为空字符串） */
  comment?: string;
}

export interface TableMeta {
  rows: number;
  cols: Column[];
  rels: string[];
  data: (string | number)[][];
  /** 表注释（真实扫描数据才有） */
  comment?: string;
  /** 唯一/普通索引（真实扫描数据才有） */
  indexes?: { name: string; unique: boolean; columns: string[] }[];
}

export interface KBItem {
  keys: string[];
  label: string;
  intent: string;
  tables: string[];
  sql: string;
  cols: string[];
  rows: (string | number)[][];
  chart: ChartType;
  data: Series[];
}
