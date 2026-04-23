/**
 * 行业大脑与直播话术联动
 * 「应用到直播话术」时将趋势/热点词暂存，直播话术构建页加载时合并到 hotKeywords
 */
import { create } from 'zustand'

interface IndustryBrainStore {
  /** 待应用到直播话术的热点词（从行业大脑趋势/主播趋势复制） */
  pendingHotKeywords: string[]
  setPendingHotKeywords: (keywords: string[]) => void
  /** 消费并清空，返回之前的 pending */
  consumePendingHotKeywords: () => string[]
}

export const useIndustryBrainStore = create<IndustryBrainStore>((set, get) => ({
  pendingHotKeywords: [],
  setPendingHotKeywords: (keywords) => set({ pendingHotKeywords: keywords }),
  consumePendingHotKeywords: () => {
    const prev = get().pendingHotKeywords
    set({ pendingHotKeywords: [] })
    return prev
  },
}))
