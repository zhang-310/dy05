import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useLiveScriptCore } from '../hooks/useLiveScriptCore'

vi.mock('@/api/live', () => ({
  getSession: vi.fn().mockResolvedValue({
    id: 18,
    liveTitle: '晚场修护直播',
    scriptStyle: 'professional',
    sessionType: 'commerce',
  }),
}))

vi.mock('@/api/ai', () => ({
  getModelsByTaskCode: vi.fn().mockResolvedValue([{ id: 11, modelName: 'DeepSeek' }]),
  listAiModels: vi.fn().mockResolvedValue([{ id: 11, modelName: 'DeepSeek' }]),
}))

vi.mock('@/api/live-product', async () => {
  const actual = await vi.importActual<typeof import('@/api/live-product')>('@/api/live-product')
  return {
    ...actual,
    getProductsBySession: vi.fn(actual.getProductsBySession),
  }
})

vi.mock('@/api/live-script', async () => {
  const actual = await vi.importActual<typeof import('@/api/live-script')>('@/api/live-script')
  return {
    ...actual,
    getScriptsBySession: vi.fn(actual.getScriptsBySession),
    initScriptSlots: vi.fn().mockResolvedValue(undefined),
  }
})

vi.mock('@/utils/request', () => ({
  default: { post: vi.fn() },
}))

import * as request from '@/utils/request'

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

describe('useLiveScriptCore', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads wrapped products and scripts without leaking object responses to consumers', async () => {
    mockPost.mockImplementation(async (url: string) => {
      if (url === '/live/product/by-session') {
        return { data: { items: [{ id: '51', sessionId: '18', productId: '9001', productName: '修护精华', sales: '12' }] } }
      }
      if (url === '/live/script/by-session') {
        return { data: { records: [{ id: '41', sessionId: '18', productId: '9001', content: '讲清修护逻辑', sequenceNo: '1' }] } }
      }
      return []
    })

    const { result } = renderHook(() => useLiveScriptCore(18), { wrapper })

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.products).toEqual([
      expect.objectContaining({ id: 51, productId: 9001, productName: '修护精华', saleQuantity: 12 }),
    ])
    expect(result.current.scripts).toEqual([
      expect.objectContaining({ id: 41, sessionId: 18, productId: 9001, scriptContent: '讲清修护逻辑', sequenceNo: 1 }),
    ])
  })
})
