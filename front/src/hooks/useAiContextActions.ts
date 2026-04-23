import { useMemo } from 'react'
import type { PageContext } from '@/hooks/usePageContext'

export interface AiQuickAction {
  label: string
  /** 相对角色前缀的路径，如 /product?detail=1&tab=script */
  pathSuffix: string
}

/**
 * 页面 → 推荐快捷入口（导航为主；后续可接 Agent API）
 */
export function useAiContextActions(prefix: string, ctx: PageContext): AiQuickAction[] {
  return useMemo(() => {
    const p = prefix
    switch (ctx.kind) {
      case 'product_list':
        return [
          { label: '打开内容库', pathSuffix: `${p}/content/library` },
          { label: '商品就绪度', pathSuffix: `${p}/product/readiness` },
        ]
      case 'product_detail':
        if (ctx.productId) {
          const id = ctx.productId
          return [
            { label: '话术与脚本', pathSuffix: `${p}/product?detail=${id}&tab=script` },
            { label: '效果评分', pathSuffix: `${p}/product?detail=${id}&tab=effectiveness` },
            { label: '直播关联', pathSuffix: `${p}/product?detail=${id}&tab=relations` },
          ]
        }
        return []
      case 'live_workbench':
        if (ctx.liveSessionId) {
          const sid = ctx.liveSessionId
          return [
            { label: '实时提词', pathSuffix: `${p}/live/realtime?sessionId=${sid}` },
            { label: '场次详情', pathSuffix: `${p}/live/sessions/${sid}` },
          ]
        }
        return []
      case 'shortvideo_viral':
        return [{ label: '短视频看板', pathSuffix: `${p}/shortvideo/dashboard` }]
      case 'knowledge':
        return [{ label: 'AI 智能体', pathSuffix: `${p}/agent` }]
      default:
        return [
          { label: '打开智能体', pathSuffix: `${p}/agent` },
          { label: '知识库', pathSuffix: `${p}/ai/knowledge` },
        ]
    }
  }, [prefix, ctx.kind, ctx.productId, ctx.liveSessionId])
}
