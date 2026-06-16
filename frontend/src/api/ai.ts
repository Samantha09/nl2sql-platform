import request from './request'

export const convert = (data: any) => request.post('/ai/convert', data)
