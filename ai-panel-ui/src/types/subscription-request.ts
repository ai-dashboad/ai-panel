export type SubscriptionService = 'claude' | 'chatgpt' | 'other'
export type SubscriptionRequestStatus = 'pending' | 'approved' | 'rejected' | 'cancelled'

export interface SubscriptionRequest {
  id: number
  userId: number
  username: string | null
  email: string
  service: SubscriptionService
  note: string | null
  status: SubscriptionRequestStatus
  adminNote: string | null
  reviewedBy: number | null
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateSubscriptionRequest {
  email: string
  service: SubscriptionService
  note?: string
}

export interface ReviewSubscriptionRequest {
  status: 'approved' | 'rejected'
  adminNote?: string
}

export interface WhoAmI {
  userId: number
  username: string | null
  isAdmin: boolean
}
