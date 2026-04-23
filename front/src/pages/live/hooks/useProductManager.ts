import { useState, useCallback, useEffect } from 'react'
import { useDebouncedValue } from '@/hooks/useDebouncedCallback'
import { useToast } from '@/contexts/ToastContext'
import {
  initScriptSlots,
  type LiveScript,
} from '@/api/live'
import {
  batchAddProducts,
  batchSortProducts,
  deleteLiveProduct,
  saveLiveProduct,
  type LiveProduct,
} from '@/api/live-product'
import { searchProducts, inferProductType } from '@/api/product'
import type { ProductLibraryItemVO } from '@/types/product'
import { normalizePageResult } from '@/utils/pageResult'
import {
  parseProductTypes,
  formatProductTypes,
} from '@/pages/live/components/constants'

export interface UseProductManagerDeps {
  sessionId: number | ''
  products: LiveProduct[]
  setProducts: React.Dispatch<React.SetStateAction<LiveProduct[]>>
  scripts: LiveScript[]
  setScripts: React.Dispatch<React.SetStateAction<LiveScript[]>>
  sortedProducts: LiveProduct[]
  loadData: () => Promise<void>
  /** 产品重排成功后调用（如 rebuildSlots + loadData） */
  onAfterProductSort?: () => Promise<void>
}

export function useProductManager({
  sessionId,
  products,
  setProducts,
  scripts,
  setScripts,
  sortedProducts,
  loadData,
  onAfterProductSort,
}: UseProductManagerDeps) {
  const toast = useToast()

  const [addProductOpen, setAddProductOpen] = useState(false)
  const [batchProductOpen, setBatchProductOpen] = useState(false)
  const [productList, setProductList] = useState<ProductLibraryItemVO[]>([])
  const [productListTotal, setProductListTotal] = useState(0)
  const [productListPage, setProductListPage] = useState(0)
  const [productListHasMore, setProductListHasMore] = useState(false)
  const [productSearch, setProductSearch] = useState('')
  const [selectedProductToAdd, setSelectedProductToAdd] = useState<ProductLibraryItemVO | null>(null)
  const [addProductTypeLoading, setAddProductTypeLoading] = useState(false)
  const [addProductTypeSelected, setAddProductTypeSelected] = useState<string[]>([])
  const [sorting, setSorting] = useState(false)
  const productSearchDebounced = useDebouncedValue(productSearch, 350)
  const PAGE_SIZE = 50

  const loadProductList = useCallback((page = 0, append = false) => {
    searchProducts({ page, rows: PAGE_SIZE, productName: productSearchDebounced || undefined })
      .then((res) => {
        const { list, total } = normalizePageResult<ProductLibraryItemVO>(res)
        const items = list ?? []
        if (append) {
          setProductList((prev) => [...prev, ...items])
        } else {
          setProductList(items)
        }
        setProductListTotal(total ?? 0)
        setProductListPage(page)
        setProductListHasMore(items.length >= PAGE_SIZE)
      })
      .catch(() => {
        if (!append) setProductList([])
        setProductListHasMore(false)
      })
  }, [productSearchDebounced])

  const loadMoreProducts = useCallback(() => {
    if (!productListHasMore) return
    loadProductList(productListPage + 1, true)
  }, [productListHasMore, productListPage, loadProductList])

  useEffect(() => {
    if (addProductOpen) loadProductList()
  }, [addProductOpen, loadProductList])

  const handleSelectProductToAdd = async (product: ProductLibraryItemVO) => {
    const productId = product.id
    if (!productId) return
    setSelectedProductToAdd(product)
    setAddProductTypeLoading(true)
    setAddProductTypeSelected([])
    try {
      const inferred = await inferProductType(productId)
      const types = inferred ? parseProductTypes(inferred) : ['flat']
      setAddProductTypeSelected(types.length > 0 ? types : ['flat'])
    } catch {
      setAddProductTypeSelected(['flat'])
    } finally {
      setAddProductTypeLoading(false)
    }
  }

  const handleConfirmAddProduct = async () => {
    if (!sessionId || typeof sessionId !== 'number' || !selectedProductToAdd) return
    const productId = selectedProductToAdd.id
    if (!productId) return
    const maxPos = products.reduce((m, p) => Math.max(m, p.position ?? 0), 0)
    const payload = {
      sessionId,
      productId,
      productName: selectedProductToAdd.productName,
      position: maxPos + 1,
      productType: formatProductTypes(addProductTypeSelected),
    }
    setSelectedProductToAdd(null)
    setAddProductOpen(false)
    try {
      const savedId = await saveLiveProduct(payload)
      const newProduct: LiveProduct = {
        id: savedId,
        ...payload,
        createTime: new Date().toISOString(),
        price: selectedProductToAdd.price,
        imageUrl: selectedProductToAdd.imageUrl ?? selectedProductToAdd.mainImage,
        productCategory: selectedProductToAdd.productCategory ?? selectedProductToAdd.category,
      }
      setProducts((prev) => [...prev, newProduct])
      await initScriptSlots(sessionId)
      await loadData()
      toast('添加成功', 'success')
    } catch (e) {
      toast(e instanceof Error ? e.message : '添加失败', 'error')
    }
  }

  const handleBatchAdd = async (
    items: Array<{ productId: number; productName: string; productType: string; productScriptId?: number }>
  ) => {
    if (!sessionId || typeof sessionId !== 'number' || items.length === 0) return
    try {
      const count = await batchAddProducts(
        sessionId,
        items.map((i) => ({
          productId: i.productId,
          productName: i.productName,
          productType: i.productType,
          ...(i.productScriptId != null && i.productScriptId > 0 ? { productScriptId: i.productScriptId } : {}),
        }))
      )
      await initScriptSlots(sessionId)
      await loadData()
      toast(`成功添加 ${count} 个产品`, 'success')
      setBatchProductOpen(false)
    } catch (e) {
      toast(e instanceof Error ? e.message : '批量添加失败', 'error')
    }
  }

  const handleRemoveProduct = async (row: LiveProduct) => {
    const id = row.id
    if (!id) return
    const productId = row.productId
    const prevProducts = products
    const prevScripts = scripts
    setProducts((prev) => prev.filter((p) => p.id !== id))
    if (productId) {
      setScripts((prev) => prev.filter((s) => !(s.scriptType === 'product' && s.productId === productId)))
    }
    try {
      await deleteLiveProduct(id)
      toast('移除成功', 'success')
    } catch (e) {
      setProducts(prevProducts)
      setScripts(prevScripts)
      toast(e instanceof Error ? e.message : '移除失败', 'error')
    }
  }

  const handleMoveProduct = async (row: LiveProduct, direction: 'up' | 'down') => {
    if (!sessionId || typeof sessionId !== 'number') return
    const idx = sortedProducts.findIndex((p) => p.id === row.id)
    if (idx < 0) return
    const swapIdx = direction === 'up' ? idx - 1 : idx + 1
    if (swapIdx < 0 || swapIdx >= sortedProducts.length) return
    const newOrder = [...sortedProducts]
    ;[newOrder[idx], newOrder[swapIdx]] = [newOrder[swapIdx], newOrder[idx]]
    const productIds = newOrder.map((p) => p.id).filter((id): id is number => Boolean(id))
    const prevProducts = products
    setProducts(newOrder.map((p, i) => ({ ...p, position: i })))
    setSorting(true)
    try {
      await batchSortProducts(sessionId, productIds)
      if (onAfterProductSort) {
        await onAfterProductSort()
      }
      toast('排序已更新', 'success')
    } catch (e) {
      setProducts(prevProducts)
      toast(e instanceof Error ? e.message : '排序失败', 'error')
    } finally {
      setSorting(false)
    }
  }

  /** 批量提交排序：接收新顺序的产品 ID 数组 */
  const handleSubmitSort = async (productIds: number[]) => {
    if (!sessionId || typeof sessionId !== 'number' || productIds.length === 0) return
    const prevProducts = products
    // 按 productIds 顺序重建 products 数组
    const idMap = new Map(sortedProducts.map((p) => [p.id, p]))
    const reordered = productIds.reduce<LiveProduct[]>((result, id, i) => {
        const product = idMap.get(id)
        if (product) {
          result.push({ ...product, position: i })
        }
        return result
      }, [])
    setProducts(reordered)
    setSorting(true)
    try {
      await batchSortProducts(sessionId, productIds)
      if (onAfterProductSort) {
        await onAfterProductSort()
      }
      toast('排序已更新', 'success')
    } catch (e) {
      setProducts(prevProducts)
      toast(e instanceof Error ? e.message : '排序失败', 'error')
    } finally {
      setSorting(false)
    }
  }

  const handleUpdateProductType = async (row: LiveProduct, types: string[]) => {
    const id = row.id
    if (!id) return
    const newType = formatProductTypes(types)
    const prevProducts = products
    setProducts((prev) => prev.map((p) => p.id === id ? { ...p, productType: newType } : p))
    try {
      await saveLiveProduct({
        id,
        sessionId: row.sessionId,
        productId: row.productId,
        productName: row.productName,
        position: row.position,
        productType: newType,
        ...(row.productScriptId != null && row.productScriptId > 0
          ? { productScriptId: row.productScriptId }
          : {}),
      })
      toast('产品类型已更新', 'success')
    } catch (e) {
      setProducts(prevProducts)
      toast(e instanceof Error ? e.message : '更新失败', 'error')
    }
  }

  const handleBatchRemoveProducts = useCallback(async (ids: number[]) => {
    if (!sessionId || typeof sessionId !== 'number' || ids.length === 0) return
    const prevProducts = [...products]
    const prevScripts = [...scripts]
    const productIdsToRemove = new Set(products.filter((p) => ids.includes(p.id)).map((p) => p.productId))
    setProducts((prev) => prev.filter((p) => !ids.includes(p.id)))
    setScripts((prev) => prev.filter((s) => !(s.scriptType === 'product' && productIdsToRemove.has(s.productId ?? -1))))
    try {
      await Promise.all(ids.map((id) => deleteLiveProduct(id)))
      await loadData()
      toast(`已移除 ${ids.length} 个产品`, 'success')
    } catch (e) {
      setProducts(prevProducts)
      setScripts(prevScripts)
      toast(e instanceof Error ? e.message : '批量移除失败', 'error')
    }
  }, [sessionId, products, scripts, loadData, toast])

  const handleProductClick = useCallback((productId: number) => {
    const idx = sortedProducts.findIndex((p) => p.productId === productId)
    if (idx < 0) return
    const sectionKey = `product-${idx}`
    // 返回 sectionKey，由 useLiveScriptBuilder 调用 expandSection + setScrollToSectionKey，ScriptPanel 负责展开+滚动
    return sectionKey
  }, [sortedProducts])

  return {
    addProductOpen, setAddProductOpen,
    batchProductOpen, setBatchProductOpen,
    productList, productSearch, setProductSearch,
    productListTotal, productListHasMore, loadMoreProducts,
    selectedProductToAdd, setSelectedProductToAdd,
    addProductTypeLoading, addProductTypeSelected, setAddProductTypeSelected,
    sorting,
    handleSelectProductToAdd, handleConfirmAddProduct,
    handleBatchAdd, handleRemoveProduct, handleBatchRemoveProducts,
    handleMoveProduct, handleSubmitSort, handleUpdateProductType,
    handleProductClick,
    loadProductList,
  }
}
