import { get, post, put, del } from './request'
import type {
  SubscriptionRequest,
  CreateSubscriptionRequest,
  ReviewSubscriptionRequest,
  WhoAmI,
} from '@/types/subscription-request'

export const subscriptionRequestApi = {
  whoami: () => get<WhoAmI>('/subscription-requests/whoami'),

  listMine: () => get<SubscriptionRequest[]>('/subscription-requests/mine'),

  listAll: (status?: string) =>
    get<SubscriptionRequest[]>('/subscription-requests', { params: status ? { status } : {} }),

  create: (data: CreateSubscriptionRequest) => post<SubscriptionRequest>('/subscription-requests', data),

  cancel: (id: number) => del(`/subscription-requests/${id}`),

  review: (id: number, data: ReviewSubscriptionRequest) =>
    put<SubscriptionRequest>(`/subscription-requests/${id}/review`, data),
}
