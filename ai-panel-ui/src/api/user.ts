import { get, post, put } from './request'
import type { LoginRequest, RegisterRequest, UserInfo, LoginResponse } from '@/types/user'

export const userApi = {
  login: (data: LoginRequest) => post<LoginResponse>('/auth/login', data),

  register: (data: RegisterRequest) => post<UserInfo>('/auth/register', data),

  logout: () => post('/auth/logout'),

  getUserInfo: () => get<UserInfo>('/users/me'),

  updateUserInfo: (data: Partial<UserInfo>) => put<UserInfo>('/users/me', data),

  // Backend: PUT /api/v1/users/me with { currentPassword, newPassword } (UserController#updateMe)
  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    put<UserInfo>('/users/me', data),

  getPlan: () => get<{ planType: string; quota: number; used: number }>('/users/me/plan'),
}
