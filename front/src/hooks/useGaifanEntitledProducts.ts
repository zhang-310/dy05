import { useEffect, useState } from 'react'
import { GAIFAN_PRODUCT_ROUTES, listGaifanProducts, type GaifanProductSummary } from '@/api/gaifan-catalog'

export type GaifanNavItem = GaifanProductSummary & { path?: string }

/**
 * 从 /api/v1/product/list 加载可售产品，用于官网与按 productCode 过滤导航。
 */
export function useGaifanEntitledProducts() {
  const [products, setProducts] = useState<GaifanNavItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    listGaifanProducts()
      .then((list) => {
        if (cancelled) return
        setProducts(
          list
            .filter((p) => p.enabled !== false)
            .map((p) => ({ ...p, path: GAIFAN_PRODUCT_ROUTES[p.code] }))
        )
      })
      .catch(() => {
        if (!cancelled) setProducts([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return { products, loading, hasProduct: (code: string) => products.some((p) => p.code === code) }
}
