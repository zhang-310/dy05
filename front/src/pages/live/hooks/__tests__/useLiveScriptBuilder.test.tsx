/**
 * useLiveScriptBuilder 测试：生成流程、编辑状态
 */
import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { SnackbarProvider } from 'notistack'
import { ToastProvider } from '@/contexts/ToastContext'
import { useLiveScriptBuilder } from '../useLiveScriptBuilder'
import * as liveApi from '@/api/live'
import * as aiApi from '@/api/ai'

vi.mock('@/api/live', () => ({
  getSession: vi.fn(),
  getProductsBySession: vi.fn(),
  getScriptsBySession: vi.fn(),
  initScriptSlots: vi.fn(),
  rebuildScriptSlots: vi.fn(),
  checkGenerateFullInProgress: vi.fn(),
}))
vi.mock('@/api/ai', () => ({
  listAiModels: vi.fn(),
  getModelsByTaskCode: vi.fn(),
}))

function wrapper({ children }: { children: React.ReactNode }) {
  return (
    <SnackbarProvider maxSnack={3}>
      <ToastProvider>{children}</ToastProvider>
    </SnackbarProvider>
  )
}

describe('useLiveScriptBuilder', () => {
  beforeEach(() => {
    vi.mocked(liveApi.getSession).mockResolvedValue({
      id: 1,
      liveTitle: '测试场次',
      status: 0,
      scriptStyle: '',
    } as never)
    vi.mocked(liveApi.getProductsBySession).mockResolvedValue([])
    vi.mocked(liveApi.getScriptsBySession).mockResolvedValue([])
    vi.mocked(aiApi.getModelsByTaskCode).mockResolvedValue([])
  })

  it('loads session data when sessionId is valid', async () => {
    const { result } = renderHook(() => useLiveScriptBuilder(1), { wrapper })
    await waitFor(() => expect(liveApi.getSession).toHaveBeenCalledWith(1), { timeout: 3000 })
    await waitFor(() => {
      expect(result.current.session).not.toBeNull()
      expect(result.current.session?.liveTitle).toBe('测试场次')
    })
  })

  it('returns loading false after data loaded', async () => {
    const { result } = renderHook(() => useLiveScriptBuilder(1), { wrapper })
    await waitFor(() => expect(result.current.loading).toBe(false), { timeout: 3000 })
    expect(result.current.products).toEqual([])
    expect(result.current.scripts).toEqual([])
  })

  it('exposes editingId from scriptEditor', async () => {
    const { result } = renderHook(() => useLiveScriptBuilder(1), { wrapper })
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(typeof result.current.editingId).toBe('object')
  })
})
