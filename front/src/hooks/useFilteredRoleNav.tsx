import { useMemo, type ReactNode } from 'react'
import type { NavChild, NavGroup } from '@/layouts/roleNavigation'
import { ADMIN_NAV_GROUPS, TALENT_NAV_ITEMS } from '@/layouts/roleNavigation'
import { GAIFAN_PRODUCT_ROUTES, type GaifanProductSummary } from '@/api/gaifan-catalog'
import { useGaifanEntitledProducts } from '@/hooks/useGaifanEntitledProducts'
import { shortvideoRoutes } from '@/constants/shortvideoRoutes'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import VideoLibraryIcon from '@mui/icons-material/VideoLibrary'
import BusinessIcon from '@mui/icons-material/Business'
import TheaterComedyIcon from '@mui/icons-material/TheaterComedy'
import FaceRetouchingNaturalIcon from '@mui/icons-material/FaceRetouchingNatural'

/** 导航 path → 可售 productCode（无映射则始终显示） */
const PATH_PRODUCT_CODE: Record<string, string> = {
  '/admin/gaifan/douyin-ops-commander': 'douyin-ops',
  '/admin/shortvideo/insights': 'video-insight',
  '/admin/shortvideo/projects': 'shortvideo-maker',
  '/admin/ai/digital-human': 'digital-human',
  '/admin/shortvideo/drama': 'drama-ai',
  '/admin/ai/knowledge': 'knowledge-base',
  '/admin/ai/knowledge/search': 'knowledge-base',
  '/admin/ai/photo-avatar': 'photo-avatar-video',
  '/talent/douyin/accounts': 'douyin-ops',
  [shortvideoRoutes.insights]: 'video-insight',
  '/talent/shortvideo': 'shortvideo-maker',
  '/talent/shortvideo/drama': 'drama-ai',
  '/talent/shortvideo/photo-avatar': 'photo-avatar-video',
  '/admin/gaifan/mcp': 'video-insight',
  [shortvideoRoutes.quickGenerate]: 'shortvideo-maker',
  [shortvideoRoutes.scriptPlanning]: 'shortvideo-maker',
  [shortvideoRoutes.materialProduction]: 'shortvideo-maker',
  [shortvideoRoutes.materialPrepare]: 'shortvideo-maker',
  [shortvideoRoutes.shotList]: 'shortvideo-maker',
  [shortvideoRoutes.editing]: 'shortvideo-maker',
  [shortvideoRoutes.publish]: 'shortvideo-maker',
  [shortvideoRoutes.collect]: 'video-insight',
  [shortvideoRoutes.viralVideos]: 'video-insight',
  [shortvideoRoutes.personaFusion]: 'shortvideo-maker',
  [shortvideoRoutes.workbench]: 'shortvideo-maker',
  [shortvideoRoutes.daily]: 'shortvideo-maker',
}

const PRODUCT_ICONS: Record<string, ReactNode> = {
  'douyin-ops': <SmartToyIcon fontSize="small" />,
  'video-insight': <VideoLibraryIcon fontSize="small" />,
  'shortvideo-maker': <VideoLibraryIcon fontSize="small" />,
  'digital-human': <SmartToyIcon fontSize="small" />,
  'drama-ai': <TheaterComedyIcon fontSize="small" />,
  'photo-avatar-video': <FaceRetouchingNaturalIcon fontSize="small" />,
  'knowledge-base': <BusinessIcon fontSize="small" />,
}

function filterByEntitlement(items: NavChild[], hasProduct: (code: string) => boolean): NavChild[] {
  return items.filter((item) => {
    const code = PATH_PRODUCT_CODE[item.path]
    return !code || hasProduct(code)
  })
}

function productNavChildren(products: Array<GaifanProductSummary & { path?: string }>): NavChild[] {
  const seen = new Set<string>()
  return products
    .filter((p) => p.path && !seen.has(p.path) && seen.add(p.path))
    .map((p) => ({
      label: p.name,
      icon: PRODUCT_ICONS[p.code] ?? <SmartToyIcon fontSize="small" />,
      path: p.path!,
      section: '可售产品',
    }))
}

export function useAdminNavGroups(): NavGroup[] {
  const { products, hasProduct, loading } = useGaifanEntitledProducts()

  return useMemo(() => {
    const skipFilter = loading || products.length === 0
    return ADMIN_NAV_GROUPS.map((group) => {
      if (group.key === 'gaifan' && group.children) {
        const base = skipFilter ? group.children : filterByEntitlement(group.children, hasProduct)
        const extra = skipFilter ? [] : productNavChildren(products)
        const merged = [...base]
        for (const child of extra) {
          if (!merged.some((c) => c.path === child.path)) merged.push(child)
        }
        return { ...group, children: merged }
      }
      if (group.children) {
        return { ...group, children: skipFilter ? group.children : filterByEntitlement(group.children, hasProduct) }
      }
      return group
    })
  }, [products, hasProduct, loading])
}

export function useTalentNavItems(): NavChild[] {
  const { products, hasProduct, loading } = useGaifanEntitledProducts()

  return useMemo(() => {
    const skipFilter = loading || products.length === 0
    const base = skipFilter ? TALENT_NAV_ITEMS : filterByEntitlement(TALENT_NAV_ITEMS, hasProduct)
    const extras: NavChild[] = []
    if (!skipFilter) {
      for (const p of products) {
        const path = GAIFAN_PRODUCT_ROUTES[p.code]
        if (path?.startsWith('/talent') && !base.some((b) => b.path === path)) {
          extras.push({
            label: p.name,
            icon: PRODUCT_ICONS[p.code] ?? <VideoLibraryIcon fontSize="small" />,
            path,
          })
        }
      }
    }
    return [...base, ...extras]
  }, [products, hasProduct, loading])
}
