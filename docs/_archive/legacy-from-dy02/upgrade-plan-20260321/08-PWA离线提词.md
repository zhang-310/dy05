# D6-01: 实时提词面板 PWA 离线增强

> **优先级**: P0 | **复杂度**: M | **维度**: D6 移动端与响应式
> **前置**: 无 | **改动文件**: `LiveRealtimePanel.tsx`, `useRealtimeMonitor.ts`

---

## 问题

主播用平板看提词器，网络波动时提词器断掉 = 直播中断 = 直接 GMV 损失。SSE 连接断开后话术内容消失。

---

## 方案

进入实时面板时，**预加载全部话术到 IndexedDB**。断网时自动切换到本地缓存读取，确保翻页和显示不中断。

---

## 改动

### 1. 新建 IndexedDB 工具 `frontend-react/src/utils/offlineScriptCache.ts`

```typescript
/**
 * 离线话术缓存（IndexedDB）
 * 用于 LiveRealtimePanel 断网降级
 */

const DB_NAME = 'dy01-offline-cache'
const DB_VERSION = 1
const STORE_NAME = 'live-scripts'

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'sessionId' })
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

export interface CachedScriptSlot {
  slotIndex: number
  content: string
  scriptType?: string
  style?: string
  requirement?: string
  durationSeconds?: number
}

export interface CachedSessionScripts {
  sessionId: number | string
  slots: CachedScriptSlot[]
  cachedAt: number
}

/**
 * 缓存场次话术到 IndexedDB
 */
export async function cacheSessionScripts(
  sessionId: number | string,
  slots: CachedScriptSlot[]
): Promise<void> {
  try {
    const db = await openDB()
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const data: CachedSessionScripts = {
      sessionId,
      slots,
      cachedAt: Date.now(),
    }
    store.put(data)
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  } catch {
    // IndexedDB 不可用时静默失败
    console.warn('IndexedDB cache failed')
  }
}

/**
 * 从 IndexedDB 读取缓存的话术
 */
export async function getCachedSessionScripts(
  sessionId: number | string
): Promise<CachedSessionScripts | null> {
  try {
    const db = await openDB()
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const request = store.get(sessionId)
    return new Promise((resolve) => {
      request.onsuccess = () => resolve(request.result ?? null)
      request.onerror = () => resolve(null)
    })
  } catch {
    return null
  }
}

/**
 * 清除过期缓存（超过 24 小时的记录）
 */
export async function clearExpiredCache(): Promise<void> {
  try {
    const db = await openDB()
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const request = store.openCursor()
    const expired = Date.now() - 24 * 60 * 60 * 1000
    request.onsuccess = () => {
      const cursor = request.result
      if (cursor) {
        const data = cursor.value as CachedSessionScripts
        if (data.cachedAt < expired) {
          cursor.delete()
        }
        cursor.continue()
      }
    }
  } catch {
    // 静默失败
  }
}
```

### 2. `frontend-react/src/hooks/useRealtimeMonitor.ts` — 增加离线缓存

在 hook 中增加预加载和离线降级逻辑：

```typescript
import { cacheSessionScripts, getCachedSessionScripts, clearExpiredCache } from '@/utils/offlineScriptCache'

// 在 useRealtimeMonitor 内部

// === 新增: 全量话术本地缓存 ===
const [offlineSlots, setOfflineSlots] = useState<CachedScriptSlot[] | null>(null)
const [isOfflineMode, setIsOfflineMode] = useState(false)

// 连接成功后预加载全部话术到 IndexedDB
useEffect(() => {
  if (!sessionId || connectionStatus !== 'connected') return

  // 请求全部话术列表
  request.post('/live/script/search', {
    sessionId: Number(sessionId),
    page: 0,
    rows: 500, // 获取全部
    sortName: 'sequenceNo',
    sortOrder: 'asc',
  }).then((res) => {
    const list = res?.list ?? res ?? []
    const slots = (list as Record<string, unknown>[]).map((s, i) => ({
      slotIndex: Number(s.sequenceNo ?? i),
      content: String(s.scriptContent ?? ''),
      scriptType: s.scriptType as string | undefined,
      style: s.style as string | undefined,
      requirement: s.requirement as string | undefined,
      durationSeconds: s.durationLimitSec ? Number(s.durationLimitSec) : undefined,
    }))
    // 缓存到 IndexedDB
    cacheSessionScripts(sessionId, slots)
    setOfflineSlots(slots)
  }).catch(() => {
    // 尝试从缓存读取
    getCachedSessionScripts(sessionId).then((cached) => {
      if (cached) setOfflineSlots(cached.slots)
    })
  })

  // 清理过期缓存
  clearExpiredCache()
}, [sessionId, connectionStatus])

// 网络断开时切换到离线模式
useEffect(() => {
  const handleOnline = () => {
    setIsOfflineMode(false)
    reconnect() // 重新连接 SSE
  }
  const handleOffline = () => {
    setIsOfflineMode(true)
  }
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
  return () => {
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
  }
}, [reconnect])

// 断网时的离线翻页
const offlineNextSlot = useCallback(() => {
  if (!offlineSlots) return
  setCurrentSlot((prev) => {
    if (!prev) return prev
    const nextIndex = Math.min(prev.slotIndex + 1, offlineSlots.length - 1)
    const nextData = offlineSlots[nextIndex]
    if (!nextData) return prev
    return {
      slotIndex: nextIndex,
      content: nextData.content,
      remainingSeconds: nextData.durationSeconds ?? 0,
      type: nextData.scriptType ?? '',
      requirement: nextData.requirement,
      totalSlots: offlineSlots.length,
      isLast: nextIndex === offlineSlots.length - 1,
    }
  })
}, [offlineSlots])

// 在返回值中增加离线相关字段
return {
  // ...现有返回值
  isOfflineMode,
  offlineSlots,
  offlineNextSlot,
  offlineSkipToSlot: (index: number) => {
    if (!offlineSlots || !offlineSlots[index]) return
    const data = offlineSlots[index]
    setCurrentSlot({
      slotIndex: index,
      content: data.content,
      remainingSeconds: data.durationSeconds ?? 0,
      type: data.scriptType ?? '',
      requirement: data.requirement,
      totalSlots: offlineSlots.length,
      isLast: index === offlineSlots.length - 1,
    })
  },
}
```

### 3. `frontend-react/src/pages/live/LiveRealtimePanel.tsx` — 离线降级 UI

在面板中增加离线状态提示和降级控制：

```tsx
// 从 useRealtimeMonitor 解构新增字段
const {
  currentSlot, metrics, connectionStatus, isConnected,
  reconnect, nextSlot, skipToSlot,
  isOfflineMode, offlineNextSlot, offlineSkipToSlot,
} = useRealtimeMonitor(sessionId)

// 离线模式提示横幅
{isOfflineMode && (
  <Alert severity="warning" sx={{ mb: 1, py: 0.25 }}>
    <Typography variant="body2">
      🔌 网络已断开，正在使用离线缓存的话术。翻页和显示不受影响。
    </Typography>
  </Alert>
)}

// 翻页按钮使用降级函数
const handleNext = isOfflineMode ? offlineNextSlot : nextSlot
const handleSkip = isOfflineMode ? offlineSkipToSlot : skipToSlot
```

---

## 验收标准

1. ✅ 进入实时面板后话术预加载到 IndexedDB
2. ✅ 断网后 UI 显示"离线模式"提示
3. ✅ 断网后话术仍可翻页和显示
4. ✅ 恢复网络后自动重连 SSE
5. ✅ IndexedDB 缓存 24 小时后自动清理
6. ✅ IndexedDB 不可用时静默降级（不影响在线功能）
7. ✅ `npm run type-check` 通过

## 注意事项

- IndexedDB 在 Safari 隐私模式下可能不可用，所有 IndexedDB 操作都有 try/catch
- `offlineSlots` 的话术来自 `POST /live/script/search`，字段映射需与后端一致
- 预加载时机是 SSE 连接成功后（`connectionStatus === 'connected'`）
- `window.addEventListener('online'/'offline')` 监听浏览器网络状态变化
- 离线模式下 metrics 数据冻结在最后一次更新的值
- 需要确认 `request.post('/live/script/search', ...)` 的响应格式（可能是 `{ total, list }` 或直接数组）
