import { useMemo } from 'react'
import { useLocation, useSearchParams, matchPath } from 'react-router-dom'

function isProductBasePath(pathname: string): boolean {
  return /\/product$/.test(pathname) && !pathname.includes('/product/readiness')
}

export type AiPageContextKind =
  | 'product_list'
  | 'product_detail'
  | 'live_workbench'
  | 'shortvideo_viral'
  | 'knowledge'
  | 'generic'

export interface PageContext {
  pathname: string
  search: string
  /** 当前页业务类型（供 AI 助手等使用） */
  kind: AiPageContextKind
  /** 解析到的实体 ID（若有） */
  productId: number | null
  liveSessionId: number | null
  /** 简短展示用 */
  headline: string
}

/**
 * 路由上下文：pathname + 实体解析（商品 / 直播场次等）
 */
export function usePageContext(): PageContext {
  const location = useLocation()
  const [sp] = useSearchParams()

  return useMemo(() => {
    const pathname = location.pathname
    const search = location.search
    const detail = sp.get('detail')
    const productFromQuery = detail ? parseInt(detail, 10) : NaN

    let kind: AiPageContextKind = 'generic'
    let productId: number | null = null
    let liveSessionId: number | null = null
    let headline = '当前工作区'

    if (isProductBasePath(pathname)) {
      if (Number.isFinite(productFromQuery) && productFromQuery > 0) {
        kind = 'product_detail'
        productId = productFromQuery
        headline = `商品 #${productId}`
      } else {
        kind = 'product_list'
        headline = '商品列表'
      }
    }

    const wb = matchPath({ path: '/admin/live/workbench/:id', end: false }, pathname)
      || matchPath({ path: '/org/live/workbench/:id', end: false }, pathname)
      || matchPath({ path: '/talent/live/workbench/:id', end: false }, pathname)
    if (wb?.params?.id) {
      const sid = parseInt(wb.params.id, 10)
      if (Number.isFinite(sid)) {
        kind = 'live_workbench'
        liveSessionId = sid
        headline = `直播场次 #${sid}`
      }
    }

    if (pathname.includes('/shortvideo/viral')) {
      kind = 'shortvideo_viral'
      headline = '爆款库'
    }

    if (pathname.includes('/ai/knowledge')) {
      kind = 'knowledge'
      headline = '知识库'
    }

    return { pathname, search, kind, productId, liveSessionId, headline }
  }, [location.pathname, location.search, sp])
}
