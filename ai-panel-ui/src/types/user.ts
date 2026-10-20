export interface UserInfo {
  id: number
  username: string
  email: string
  displayName?: string
  avatarUrl?: string
  timezone?: string
  language?: string
  status: number
  planType: string
  planExpiresAt?: string
  createdAt: string
  lastLoginAt?: string
}

export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface UserPlan {
  planType: 'FREE' | 'BASIC' | 'PRO' | 'ENTERPRISE'
  monthlyQuota: number
  usedQuota: number
  rateLimitPerMinute: number
  rateLimitPerDay: number
  maxApiKeys: number
  startDate: string
  endDate: string
}
