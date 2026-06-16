import request from './request'

export const nlQuery = (data: any) => request.post('/query/nl', data)
export const sqlQuery = (sql: string) => request.post('/query/sql', sql)
export const getHistory = (conversationId: string) => request.get('/query/history', { params: { conversationId } })
