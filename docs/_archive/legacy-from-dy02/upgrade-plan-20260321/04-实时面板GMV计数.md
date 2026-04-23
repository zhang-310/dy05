# D2-03: 实时面板 GMV 计数器

> **优先级**: P0 | **复杂度**: M | **维度**: D2 直播工作区
> **前置**: 无 | **改动文件**: `LiveRealtimePanel.tsx`, `useRealtimeMonitor.ts`

---

## 问题

LiveRealtimePanel 是直播中的核心工具，当前显示 viewers/peakViewers/likes/comments/shares/productImpressions 等指标，但**没有实时 GMV 数字**。运营无法判断话术效果，不知道要不要换话术。

---

## 方案

在 LiveRealtimePanel 的 metrics 区域顶部增加 **GMV 实时大字计数器**。

### 数据来源

当前 `useRealtimeMonitor` hook 的 metrics 对象结构：
```typescript
metrics: {
  viewers: number
  peakViewers: number
  likes: number
  totalLikes: number
  comments: number
  totalComments: number
  shares: number
  productImpressions: number
  timestamp: number
  dataPoints: number
}
```

SSE 端点: `GET /api/v1/live/monitor/stream/{sessionId}`

**扩展方案**: 在 metrics 中增加 `gmv` 和 `gmvDelta` 字段。

---

## 改动

### 1. `frontend-react/src/hooks/useRealtimeMonitor.ts`

在 metrics 类型和初始值中增加 GMV 字段：

```typescript
// 在 metrics state 的类型/初始值中增加
metrics: {
  // ...现有字段保持不变
  viewers: number
  peakViewers: number
  // ...
  productImpressions: number
  // ↓ 新增
  gmv: number            // 累计 GMV
  gmvDelta: number       // 最近一次增量
  productPurchaseCount: number  // 成交笔数
  timestamp: number
  dataPoints: number
}
```

在 SSE 事件处理中解析 GMV 数据：

```typescript
// 在 metrics-update 事件处理分支中
case 'metrics-update':
case 'data-update': {
  const d = JSON.parse(event.data)
  setMetrics((prev) => ({
    ...prev,
    viewers: d.viewerCount ?? d.viewers ?? prev?.viewers ?? 0,
    // ...现有字段映射保持不变
    // ↓ 新增 GMV 映射
    gmv: d.productPurchaseAmount ?? d.gmv ?? prev?.gmv ?? 0,
    gmvDelta: (d.productPurchaseAmount ?? d.gmv ?? 0) - (prev?.gmv ?? 0),
    productPurchaseCount: d.productPurchaseCount ?? prev?.productPurchaseCount ?? 0,
  }))
  break
}
```

**注意**: 后端 `LiveSessionRealtimeDataVO` 已有 `productPurchaseAmount` 和 `productPurchaseCount` 字段（在 `types/live-realtime.ts` 中定义），SSE 推送时可能已包含这些字段。如果 SSE 未推送，则需要后端在 SSE data-update 事件中加入这两个字段。

#### 备用方案（10 秒轮询）

如果 SSE 不推送 GMV 数据，增加一个定时轮询：

```typescript
// 在 useRealtimeMonitor 内部
useEffect(() => {
  if (!sessionId || connectionStatus !== 'connected') return
  const timer = setInterval(async () => {
    try {
      const data = await request.post('/live/monitor/by-session', { sessionId })
      if (data?.productPurchaseAmount != null) {
        setMetrics((prev) => ({
          ...prev!,
          gmv: Number(data.productPurchaseAmount),
          gmvDelta: Number(data.productPurchaseAmount) - (prev?.gmv ?? 0),
          productPurchaseCount: Number(data.productPurchaseCount ?? 0),
        }))
      }
    } catch { /* 静默失败 */ }
  }, 10000) // 10秒轮询
  return () => clearInterval(timer)
}, [sessionId, connectionStatus])
```

### 2. `frontend-react/src/pages/live/LiveRealtimePanel.tsx`

在 metrics 展示区域**最顶部**添加 GMV 大字计数器：

```tsx
{/* GMV 实时计数器 - 放在 metrics 区域最上方 */}
{metrics && (
  <Box sx={{
    textAlign: 'center',
    py: 2,
    px: 3,
    bgcolor: 'success.50',
    borderRadius: 2,
    mb: 2,
    border: '1px solid',
    borderColor: 'success.200',
  }}>
    <Typography variant="caption" color="text.secondary">
      累计 GMV
    </Typography>
    <Typography
      variant="h3"
      fontWeight={800}
      color="success.main"
      sx={{
        fontFamily: '"Roboto Mono", monospace',
        letterSpacing: 1,
      }}
    >
      ¥{(metrics.gmv ?? 0).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
    </Typography>
    {(metrics.gmvDelta ?? 0) > 0 && (
      <Chip
        size="small"
        color="success"
        label={`+¥${metrics.gmvDelta.toLocaleString()}`}
        sx={{ mt: 0.5, animation: 'fadeIn 0.3s' }}
      />
    )}
    {metrics.productPurchaseCount > 0 && (
      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
        成交 {metrics.productPurchaseCount} 笔
      </Typography>
    )}
  </Box>
)}
```

---

## 验收标准

1. ✅ 直播中实时面板顶部可见累计 GMV 大字显示
2. ✅ GMV 增量时显示 `+¥xxx` 动画
3. ✅ 成交笔数显示
4. ✅ SSE 断连时 GMV 保持最后已知值
5. ✅ `npm run type-check` 通过

## 注意事项

- 后端 `LiveSessionRealtimeDataVO` 已有 `productPurchaseAmount`/`productPurchaseCount` 字段
- 如果 SSE 未推送这些字段，先用 10 秒轮询方案过渡
- GMV 数字使用 monospace 字体确保数字对齐
- `success.50` / `success.200` 是 MUI theme 的色调，确保 theme 中有定义，否则用 `alpha(theme.palette.success.main, 0.08)` 替代
- `metrics` 可能为 null，注意空值保护
