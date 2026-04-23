import { create } from 'zustand'

/** UX-02B：全局弹窗开关，逐步替代页面内分散的 open useState */
export const useModalStore = create<{
  openById: Record<string, boolean>
  setModalOpen: (id: string, open: boolean) => void
  toggleModal: (id: string) => void
}>((set) => ({
  openById: {},
  setModalOpen: (id, open) => set((s) => ({ openById: { ...s.openById, [id]: open } })),
  toggleModal: (id) => set((s) => ({ openById: { ...s.openById, [id]: !s.openById[id] } })),
}))

export const MODAL_PRODUCT_BATCH_SCRIPT_GEN = 'product-batch-script-gen'
