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
}

export interface TableMeta {
  rows: number;
  cols: Column[];
  rels: string[];
  data: (string | number)[][];
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
