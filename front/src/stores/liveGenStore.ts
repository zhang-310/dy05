import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface FlowStep {
  label: string
  status: 'pending' | 'loading' | 'done' | 'error'
  detail?: string
}

interface LiveGenState {
  genLoading: boolean
  genStyle: string
  useKbRef: boolean
  selectedModelId: string
  ipType: string
  materialType: string
  scriptModule: string
  retentionStrategy: string
  interactionLevel: string
  fullGenProgress: number
  flowSteps: FlowStep[]
  flowPanelVisible: boolean
  cancellingGen: boolean

  setGenLoading: (v: boolean) => void
  setGenStyle: (v: string) => void
  setUseKbRef: (v: boolean) => void
  setSelectedModelId: (v: string) => void
  setIpType: (v: string) => void
  setMaterialType: (v: string) => void
  setScriptModule: (v: string) => void
  setRetentionStrategy: (v: string) => void
  setInteractionLevel: (v: string) => void
  setFullGenProgress: (v: number) => void
  setFlowSteps: (steps: FlowStep[]) => void
  setFlowPanelVisible: (v: boolean) => void
  setCancellingGen: (v: boolean) => void
  resetGeneration: () => void
}

const defaultState = {
  genLoading: false,
  genStyle: 'conversational',
  useKbRef: true,
  selectedModelId: '',
  ipType: 'general',
  materialType: 'script',
  scriptModule: 'full',
  retentionStrategy: 'story',
  interactionLevel: 'medium',
  fullGenProgress: 0,
  flowSteps: [] as FlowStep[],
  flowPanelVisible: false,
  cancellingGen: false,
}

export const useLiveGenStore = create<LiveGenState>()(
  persist(
    (set) => ({
      ...defaultState,
      setGenLoading: (genLoading) => set({ genLoading }),
      setGenStyle: (genStyle) => set({ genStyle }),
      setUseKbRef: (useKbRef) => set({ useKbRef }),
      setSelectedModelId: (selectedModelId) => set({ selectedModelId }),
      setIpType: (ipType) => set({ ipType }),
      setMaterialType: (materialType) => set({ materialType }),
      setScriptModule: (scriptModule) => set({ scriptModule }),
      setRetentionStrategy: (retentionStrategy) => set({ retentionStrategy }),
      setInteractionLevel: (interactionLevel) => set({ interactionLevel }),
      setFullGenProgress: (fullGenProgress) => set({ fullGenProgress }),
      setFlowSteps: (flowSteps) => set({ flowSteps }),
      setFlowPanelVisible: (flowPanelVisible) => set({ flowPanelVisible }),
      setCancellingGen: (cancellingGen) => set({ cancellingGen }),
      resetGeneration: () => set({
        genLoading: false,
        fullGenProgress: 0,
        flowSteps: [],
        flowPanelVisible: false,
        cancellingGen: false,
      }),
    }),
    { name: 'dy02-live-gen' }
  )
)
