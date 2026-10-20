import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/modules/user'
import router from '@/router'

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      // The user id is derived server-side from this JWT (gateway / workers);
      // never send a client-chosen X-User-ID header.
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  (response) => {
    const { code, message } = response.data as ApiResponse

    if (code === 0 || code === 200) {
      return response.data
    }

    // Handle specific error codes
    if (code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push({ name: 'Login' })
      ElMessage.error('Session expired, please login again')
    } else if (code === 403) {
      ElMessage.error('Permission denied')
    } else if (code === 429) {
      ElMessage.error('Rate limit exceeded, please try again later')
    } else {
      ElMessage.error(message || 'Request failed')
    }

    return Promise.reject(new Error(message || 'Request failed'))
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push({ name: 'Login' })
      }
      ElMessage.error(data?.message || `Request failed: ${status}`)
    } else if (error.request) {
      ElMessage.error('Network error, please check your connection')
    } else {
      ElMessage.error(error.message || 'Request failed')
    }
    return Promise.reject(error)
  }
)

export default request

// Typed request methods
export const get = <T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> => {
  return request.get(url, config) as Promise<ApiResponse<T>>
}

export const post = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> => {
  return request.post(url, data, config) as Promise<ApiResponse<T>>
}

export const put = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> => {
  return request.put(url, data, config) as Promise<ApiResponse<T>>
}

export const del = <T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> => {
  return request.delete(url, config) as Promise<ApiResponse<T>>
}
