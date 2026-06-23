import { http } from './http';
import { DataSourceConfig, DataSourceInput, TableSchemaDTO } from './types';

/** 数据源列表 */
export const listDataSources = () =>
  http.get<DataSourceConfig[]>('/schema/datasource/list');

/** 新增数据源 */
export const addDataSource = (config: DataSourceInput) =>
  http.post<DataSourceConfig>('/schema/datasource', config);

/** 编辑数据源 */
export const updateDataSource = (id: number, config: DataSourceInput) =>
  http.put<DataSourceConfig>(`/schema/datasource/${id}`, config);

/** 删除数据源 */
export const deleteDataSource = (id: number) =>
  http.del<void>(`/schema/datasource/${id}`);

/** 扫描表列表，返回按数据库名分组的表名列表 */
export const scanTables = (datasourceId: number) =>
  http.post<Record<string, string[]>>(`/schema/scan/${datasourceId}`);

/** 表列表（按数据库名分组） */
export const listTables = (datasourceId: number) =>
  http.get<Record<string, string[]>>(`/schema/${datasourceId}/tables`);

/** 表结构详情 */
export const getTableDetail = (datasourceId: number, databaseName: string, tableName: string) =>
  http.get<TableSchemaDTO>(`/schema/${datasourceId}/tables/${databaseName}/${tableName}`);
