import request from './request'

export const listDataSources = () => request.get('/schema/datasource/list')
export const addDataSource = (data: any) => request.post('/schema/datasource', data)
export const scanTables = (id: number) => request.post(`/schema/scan/${id}`)
export const getTableDetail = (dsId: number, tableName: string) => request.get(`/schema/${dsId}/tables/${tableName}`)
