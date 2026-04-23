import { create } from 'zustand'

/** UX-02B：跨页保留列表页码（如从详情返回仍停留在原页） */
export const usePaginationStore = create<{
  pageByKey: Record<string, number>
  setPage: (key: string, page: number) => void
}>((set) => ({
  pageByKey: {},
  setPage: (key, page) => set((s) => ({ pageByKey: { ...s.pageByKey, [key]: page } })),
}))
