import { renderHook } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { useAiContextActions } from '../useAiContextActions'
import type { PageContext } from '../usePageContext'

describe('useAiContextActions', () => {
  const prefix = '/admin'

  it('returns product list actions', () => {
    const ctx: PageContext = { kind: 'product_list' }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([
      { label: '打开内容库', pathSuffix: '/admin/content/library' },
      { label: '商品就绪度', pathSuffix: '/admin/product/readiness' },
    ])
  })

  it('returns product detail actions with productId', () => {
    const ctx: PageContext = { kind: 'product_detail', productId: 123 }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([
      { label: '话术与脚本', pathSuffix: '/admin/product?detail=123&tab=script' },
      { label: '效果评分', pathSuffix: '/admin/product?detail=123&tab=effectiveness' },
      { label: '直播关联', pathSuffix: '/admin/product?detail=123&tab=relations' },
    ])
  })

  it('returns empty array for product detail without productId', () => {
    const ctx: PageContext = { kind: 'product_detail' }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([])
  })

  it('returns live workbench actions with liveSessionId', () => {
    const ctx: PageContext = { kind: 'live_workbench', liveSessionId: 456 }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([
      { label: '实时提词', pathSuffix: '/admin/live/realtime?sessionId=456' },
      { label: '场次详情', pathSuffix: '/admin/live/sessions/456' },
    ])
  })

  it('returns empty array for live workbench without liveSessionId', () => {
    const ctx: PageContext = { kind: 'live_workbench' }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([])
  })

  it('returns shortvideo viral actions', () => {
    const ctx: PageContext = { kind: 'shortvideo_viral' }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([
      { label: '短视频看板', pathSuffix: '/admin/shortvideo/dashboard' },
    ])
  })

  it('returns knowledge actions', () => {
    const ctx: PageContext = { kind: 'knowledge' }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([
      { label: 'AI 智能体', pathSuffix: '/admin/agent' },
    ])
  })

  it('returns default actions for unknown context', () => {
    const ctx: PageContext = { kind: 'other' as any }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([
      { label: '打开智能体', pathSuffix: '/admin/agent' },
      { label: '知识库', pathSuffix: '/admin/ai/knowledge' },
    ])
  })

  it('memoizes result when dependencies do not change', () => {
    const ctx: PageContext = { kind: 'product_list' }
    const { result, rerender } = renderHook(() => useAiContextActions(prefix, ctx))

    const firstResult = result.current
    rerender()
    const secondResult = result.current

    expect(firstResult).toBe(secondResult)
  })

  it('updates result when prefix changes', () => {
    const ctx: PageContext = { kind: 'product_list' }
    const { result, rerender } = renderHook(
      ({ prefix }) => useAiContextActions(prefix, ctx),
      { initialProps: { prefix: '/admin' } }
    )

    expect(result.current[0].pathSuffix).toBe('/admin/content/library')

    rerender({ prefix: '/org' })

    expect(result.current[0].pathSuffix).toBe('/org/content/library')
  })

  it('updates result when context kind changes', () => {
    const { result, rerender } = renderHook(
      ({ ctx }) => useAiContextActions(prefix, ctx),
      { initialProps: { ctx: { kind: 'product_list' } as PageContext } }
    )

    expect(result.current).toHaveLength(2)
    expect(result.current[0].label).toBe('打开内容库')

    rerender({ ctx: { kind: 'knowledge' } as PageContext })

    expect(result.current).toHaveLength(1)
    expect(result.current[0].label).toBe('AI 智能体')
  })

  it('updates result when productId changes', () => {
    const { result, rerender } = renderHook(
      ({ ctx }) => useAiContextActions(prefix, ctx),
      { initialProps: { ctx: { kind: 'product_detail', productId: 100 } as PageContext } }
    )

    expect(result.current[0].pathSuffix).toContain('detail=100')

    rerender({ ctx: { kind: 'product_detail', productId: 200 } as PageContext })

    expect(result.current[0].pathSuffix).toContain('detail=200')
  })
})
