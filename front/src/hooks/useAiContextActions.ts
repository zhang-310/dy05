import { useMemo } from 'react'
import type { PageContext } from '@/hooks/usePageContext'

export interface AiQuickAction {
  label: string
  /** 相对角色前缀的路径，如 /product?detail=1&tab=script */
  pathSuffix: string
}

function roleDashboard(prefix: string) {
  if (prefix === '/org') return '/org/dashboard'
  if (prefix === '/talent') return '/talent/dashboard'
  if (prefix === '/user') return '/user/dashboard'
  return '/admin/dashboard'
}

function liveSessionsPath(prefix: string) {
  if (prefix === '/org') return '/org/live/sessions'
  if (prefix === '/talent') return '/talent/live/sessions'
  if (prefix === '/admin') return '/org/live/sessions'
  return '/user/dashboard'
}

function liveSessionDetailPath(prefix: string, sessionId: number) {
  return `${liveSessionsPath(prefix)}/${sessionId}`
}

function productListActions(prefix: string): AiQuickAction[] {
  if (prefix === '/admin') {
    return [
      { label: '机构商品管理', pathSuffix: '/org/product/list' },
      { label: '机构内容库', pathSuffix: '/org/content/library' },
    ]
  }
  if (prefix === '/org') {
    return [
      { label: '机构工作台', pathSuffix: '/org/dashboard' },
      { label: '机构数据分析', pathSuffix: '/org/analytics' },
    ]
  }
  return [
    { label: '达人工作台', pathSuffix: '/talent/dashboard' },
    { label: '短视频项目', pathSuffix: '/talent/shortvideo' },
  ]
}

function shortvideoActions(prefix: string): AiQuickAction[] {
  if (prefix === '/admin') return [{ label: '达人短视频看板', pathSuffix: '/talent/shortvideo/dashboard' }]
  if (prefix === '/talent') return [{ label: '短视频项目', pathSuffix: '/talent/shortvideo' }]
  if (prefix === '/user') return [{ label: '短视频项目', pathSuffix: '/user/shortvideo' }]
  return [{ label: '机构工作台', pathSuffix: '/org/dashboard' }]
}

function knowledgeActions(prefix: string): AiQuickAction[] {
  if (prefix === '/admin') return [{ label: 'AI 智能体', pathSuffix: '/admin/ai/agent/list' }]
  return [{ label: '角色工作台', pathSuffix: roleDashboard(prefix) }]
}

function genericActions(prefix: string): AiQuickAction[] {
  if (prefix === '/admin') {
    return [
      { label: '打开智能体', pathSuffix: '/admin/ai/agent/list' },
      { label: '知识库', pathSuffix: '/admin/ai/knowledge' },
    ]
  }
  if (prefix === '/org') {
    return [
      { label: '机构工作台', pathSuffix: '/org/dashboard' },
      { label: '直播场次', pathSuffix: '/org/live/sessions' },
    ]
  }
  return [
    { label: '达人工作台', pathSuffix: '/talent/dashboard' },
    { label: '短视频项目', pathSuffix: '/talent/shortvideo' },
  ]
}

/**
 * 页面 → 推荐快捷入口（导航为主；后续可接 Agent API）
 */
export function useAiContextActions(prefix: string, ctx: PageContext): AiQuickAction[] {
  return useMemo(() => {
    switch (ctx.kind) {
      case 'product_list':
        return productListActions(prefix)
      case 'product_detail':
        if (ctx.productId) {
          const id = ctx.productId
          if (prefix !== '/org') return productListActions(prefix)
          return [
            { label: '话术与脚本', pathSuffix: `/org/product?detail=${id}&tab=script` },
            { label: '效果评分', pathSuffix: `/org/product?detail=${id}&tab=effectiveness` },
            { label: '直播关联', pathSuffix: `/org/product?detail=${id}&tab=relations` },
          ]
        }
        return []
      case 'live_workbench':
        if (ctx.liveSessionId) {
          const sid = ctx.liveSessionId
          const actions: AiQuickAction[] = [
            { label: '场次详情', pathSuffix: liveSessionDetailPath(prefix, sid) },
          ]
          if (prefix === '/org') {
            actions.unshift({ label: '实时面板', pathSuffix: `/org/live/sessions/${sid}/realtime` })
          } else {
            actions.unshift({ label: '直播场次', pathSuffix: liveSessionsPath(prefix) })
          }
          return actions
        }
        return []
      case 'shortvideo_viral':
        return shortvideoActions(prefix)
      case 'knowledge':
        return knowledgeActions(prefix)
      default:
        return genericActions(prefix)
    }
  }, [prefix, ctx.kind, ctx.productId, ctx.liveSessionId])
}
