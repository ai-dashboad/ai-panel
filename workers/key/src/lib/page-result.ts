// Ported from ai-panel-common/src/main/java/com/aipanel/common/domain/PageResult.java
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  totalPages: number
  list: T[]
  hasNext: boolean
  hasPrevious: boolean
}

export function pageResultOf<T>(list: T[], pageNum: number, pageSize: number, total: number): PageResult<T> {
  const totalPages = pageSize > 0 ? Math.ceil(total / pageSize) : 0
  return {
    pageNum,
    pageSize,
    total,
    totalPages,
    list,
    hasNext: pageNum < totalPages,
    hasPrevious: pageNum > 1,
  }
}
