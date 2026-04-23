import { create } from 'zustand'

type SortStrategy = 'manual' | 'type' | 'alpha' | 'price'

interface LiveProductState {
  selectedIds: number[]
  sortStrategy: SortStrategy

  setSelected: (ids: number[]) => void
  toggleSelected: (id: number) => void
  clearSelected: () => void
  setSortStrategy: (strategy: SortStrategy) => void
}

export const useLiveProductStore = create<LiveProductState>()((set, get) => ({
  selectedIds: [],
  sortStrategy: 'manual',

  setSelected: (selectedIds) => set({ selectedIds }),
  toggleSelected: (id) => {
    const { selectedIds } = get()
    if (selectedIds.includes(id)) {
      set({ selectedIds: selectedIds.filter((x) => x !== id) })
    } else {
      set({ selectedIds: [...selectedIds, id] })
    }
  },
  clearSelected: () => set({ selectedIds: [] }),
  setSortStrategy: (sortStrategy) => set({ sortStrategy }),
}))
