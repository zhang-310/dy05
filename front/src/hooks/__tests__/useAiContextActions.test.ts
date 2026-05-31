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
      { label: '实时面板', pathSuffix: '/admin/live/sessions/456/realtime' },
      { label: '场次详情', pathSuffix: '/admin/live/sessions/456' },
    ])
  })

  it('keeps org live actions inside registered org routes', () => {
    const ctx: PageContext = { kind: 'live_workbench', liveSessionId: 456 }
    const { result } = renderHook(() => useAiContextActions('/org', ctx))

    expect(result.current).toEqual([
      { label: '直播场次', pathSuffix: '/org/live/sessions' },
      { label: '场次详情', pathSuffix: '/org/live/sessions/456' },
    ])
  })

  it('keeps talent live actions inside registered talent routes', () => {
    const ctx: PageContext = { kind: 'live_workbench', liveSessionId: 456 }
    const { result } = renderHook(() => useAiContextActions('/talent', ctx))

    expect(result.current).toEqual([
      { label: '直播场次', pathSuffix: '/talent/live/sessions' },
      { label: '场次详情', pathSuffix: '/talent/live/sessions/456' },
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
      { label: 'AI 智能体', pathSuffix: '/admin/ai/agent/list' },
    ])
  })

  it('downgrades knowledge actions to role dashboard when role shell has no AI routes', () => {
    const ctx: PageContext = { kind: 'knowledge' }
    const { result: orgResult } = renderHook(() => useAiContextActions('/org', ctx))
    const { result: talentResult } = renderHook(() => useAiContextActions('/talent', ctx))

    expect(orgResult.current).toEqual([{ label: '角色工作台', pathSuffix: '/org/dashboard' }])
    expect(talentResult.current).toEqual([{ label: '角色工作台', pathSuffix: '/talent/dashboard' }])
  })

  it('returns default actions for unknown context', () => {
    const ctx: PageContext = { kind: 'other' as any }
    const { result } = renderHook(() => useAiContextActions(prefix, ctx))

    expect(result.current).toEqual([
      { label: '打开智能体', pathSuffix: '/admin/ai/agent/list' },
      { label: '知识库', pathSuffix: '/admin/ai/knowledge' },
    ])
  })

  it('uses only registered role-shell routes for generic org and talent contexts', () => {
    const ctx: PageContext = { kind: 'other' as any }
    const { result: orgResult } = renderHook(() => useAiContextActions('/org', ctx))
    const { result: talentResult } = renderHook(() => useAiContextActions('/talent', ctx))

    expect(orgResult.current).toEqual([
      { label: '机构工作台', pathSuffix: '/org/dashboard' },
      { label: '直播场次', pathSuffix: '/org/live/sessions' },
    ])
    expect(talentResult.current).toEqual([
      { label: '达人工作台', pathSuffix: '/talent/dashboard' },
      { label: '短视频项目', pathSuffix: '/talent/shortvideo' },
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

    expect(result.current[0].pathSuffix).toBe('/org/dashboard')
  })

  it('downgrades product detail actions for role shells without product routes', () => {
    const ctx: PageContext = { kind: 'product_detail', productId: 100 }
    const { result: orgResult } = renderHook(() => useAiContextActions('/org', ctx))
    const { result: talentResult } = renderHook(() => useAiContextActions('/talent', ctx))

    expect(orgResult.current.map((item) => item.pathSuffix)).toEqual(['/org/dashboard', '/org/analytics'])
    expect(talentResult.current.map((item) => item.pathSuffix)).toEqual(['/talent/dashboard', '/talent/shortvideo'])
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
