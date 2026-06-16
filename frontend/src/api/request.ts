
import axios, { AxiosError } from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.response.use(
  (response) => response.data,
  (error: AxiosError) => {
    const responseData = error.response?.data as { message?: string } | undefined
    const message = responseData?.message
      || error.message
      || '请求失败，请检查网络连接'

    return Promise.reject(new Error(message))
  }
)

export default request
