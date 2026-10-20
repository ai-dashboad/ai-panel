import { z } from 'zod'
import type { SubscriptionRequestRow } from './db/schema'

export const createRequestSchema = z.object({
  email: z.string().email('A valid email is required'),
  service: z.enum(['claude', 'chatgpt', 'other']),
  note: z.string().max(500).optional(),
})
export type CreateRequestInput = z.infer<typeof createRequestSchema>

export const reviewRequestSchema = z.object({
  status: z.enum(['approved', 'rejected']),
  adminNote: z.string().max(500).optional(),
})
export type ReviewRequestInput = z.infer<typeof reviewRequestSchema>

export function toResponse(row: SubscriptionRequestRow) {
  return {
    id: row.id,
    userId: row.userId,
    username: row.username,
    email: row.email,
    service: row.service,
    note: row.note,
    status: row.status,
    adminNote: row.adminNote,
    reviewedBy: row.reviewedBy,
    reviewedAt: row.reviewedAt,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt,
  }
}
