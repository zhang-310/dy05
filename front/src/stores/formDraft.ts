import { create } from 'zustand'

/** UX-02B：表单草稿键值（如分步向导、长表单防丢） */
export const useFormDraftStore = create<{
  drafts: Record<string, unknown>
  setDraft: (key: string, value: unknown) => void
  clearDraft: (key: string) => void
}>((set) => ({
  drafts: {},
  setDraft: (key, value) => set((s) => ({ drafts: { ...s.drafts, [key]: value } })),
  clearDraft: (key) =>
    set((s) => {
      const { [key]: _removed, ...rest } = s.drafts
      return { drafts: rest }
    }),
}))

export const FORM_DRAFT_ONBOARDING_STEP = 'onboarding-wizard-step'
