import request from './request'

export interface DataSource {
  id: number
  name: string
  type: 'mysql' | 'clickhouse' | 'postgresql'
  host: string
  port: number
  databaseName: string
}

export interface TableInfo {
  name: string
  comment?: string
  rowCount?: number
}

export interface ColumnInfo {
  name: string
  type: string
  comment?: string
  nullable: boolean
  defaultValue?: string
  isPrimaryKey: boolean
}

export interface IndexInfo {
  name: string
  columns: string[]
  unique: boolean
}

export interface ForeignKeyInfo {
  name: string
  column: string
  referencedTable: string
  referencedColumn: string
}

export interface TableSchema {
  tableName: string
  tableComment?: string
  columns: ColumnInfo[]
  primaryKeys: string[]
  indexes: IndexInfo[]
  foreignKeys?: ForeignKeyInfo[]
}

export const listDataSources = () => request.get('/schema/datasource/list')
export const addDataSource = (data: Partial<DataSource>) => request.post('/schema/datasource', data)
export const scanTables = (id: number) => request.post(`/schema/scan/${id}`)
export const getTableDetail = (dsId: number, tableName: string) =>
  request.get(`/schema/${dsId}/tables/${tableName}`)

// 明天对接真实接口时使用
export const listTables = (dsId: number) => request.get(`/schema/${dsId}/tables`)
