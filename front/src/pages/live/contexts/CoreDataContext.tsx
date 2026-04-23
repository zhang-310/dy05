import { createContext, useContext } from 'react'
import type { LiveSession, LiveScript } from '@/api/live'
import type { LiveProduct } from '@/api/live-product'

export interface CoreDataValue {
  session: LiveSession | null
  sessionLoading: boolean
  products: LiveProduct[]
  scripts: LiveScript[]
  readiness: Record<string, unknown> | null
  refetchSession: () => void
  refetchProducts: () => void
  refetchScripts: () => void
}

export const CoreDataContext = createContext<CoreDataValue | null>(null)

export function useCoreData(): CoreDataValue {
  const ctx = useContext(CoreDataContext)
  if (!ctx) throw new Error('useCoreData must be used within CoreDataProvider')
  return ctx
}
