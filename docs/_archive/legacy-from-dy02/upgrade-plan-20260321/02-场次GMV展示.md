# D2-01 + D2-02: 场次 GMV 展示 + 进度指示器语义化

> **优先级**: P0 | **复杂度**: S | **维度**: D2 直播工作区
> **前置**: 无 | **改动文件**: `frontend-react/src/pages/live/LiveSessionPage.tsx`

---

## D2-01: 场次卡片 GMV 突出展示

### 问题

LiveSessionPage 已结束场次（status=2）在 DataGrid 进度列中仅附带显示 `¥xxx`（第 394 行），不够醒目。卡片视图中 GMV 也只是普通 caption 文字。用户无法一眼识别高 GMV 场次。

### 方案

对 status=2 的已结束场次，在进度/数据列中：
- `totalRevenue > 0` 时用 **大字号绿色** 显示 GMV（`variant="subtitle1"`, `color="success.main"`, `fontWeight={700}`）
- 低于 1000 元的场次用红色警告
- 观看/点赞数据移到第二行小字

### 改动位置

#### DataGrid 进度列 renderCell（约第 361-399 行）

将 `status === 1 || status === 2` 的分支改为：

```tsx
if (status === 1 || status === 2) {
  const v = row.viewers != null ? Number(row.viewers) : 0
  const l = row.likes != null ? Number(row.likes) : 0
  const revenue = row.totalRevenue != null ? Number(row.totalRevenue) : 0

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', py: 0.25 }}>
      {status === 2 && revenue > 0 ? (
        <Typography
          variant="subtitle2"
          fontWeight={700}
          color={revenue >= 1000 ? 'success.main' : 'error.main'}
        >
          ¥{revenue.toLocaleString()}
        </Typography>
      ) : null}
      <Typography variant="caption" color="text.secondary" noWrap>
        {v.toLocaleString()}观 · {l.toLocaleString()}赞
      </Typography>
    </Box>
  )
}
```

#### 卡片视图（约第 588-593 行）

将已有的 caption 行改为：

```tsx
{(status === 1 || status === 2) && (
  <Box sx={{ mt: 0.5 }}>
    {status === 2 && Number(row.totalRevenue ?? 0) > 0 && (
      <Typography
        variant="subtitle1"
        fontWeight={700}
        color={Number(row.totalRevenue) >= 1000 ? 'success.main' : 'error.main'}
      >
        GMV ¥{Number(row.totalRevenue).toLocaleString()}
      </Typography>
    )}
    <Typography variant="caption" color="text.secondary">
      观众 {viewersVal.toLocaleString()} · 点赞 {likesVal.toLocaleString()}
    </Typography>
  </Box>
)}
```

---

## D2-02: 进度指示器语义化

### 问题

`getReadinessProgress()` 返回 `"待选品/待生成话术/准备就绪"` + `"N品·M术"`，新手不理解"品""术"含义。进度条缺少 Tooltip 解释。

### 方案

1. 将 `"N品·M术"` 改为 `"已选 N 品 / 已备 M 段话术"`
2. 进度条增加 Tooltip，显示步骤含义和缺失项
3. 添加步骤图标（🛒→✍️→✅）

### 改动位置

#### getReadinessProgress 函数（第 72-78 行）

```tsx
function getReadinessProgress(row: Record<string, unknown>): {
  step: number; total: number; label: string; detail: string; tooltip: string
} {
  const pc = Number(row.productCount ?? 0)
  const sc = Number(row.scriptCount ?? 0)
  if (pc === 0) return {
    step: 0, total: 3, label: '待选品',
    detail: `已选 ${pc} 品 / 已备 ${sc} 段话术`,
    tooltip: '下一步：进入工作台选择商品'
  }
  if (sc === 0) return {
    step: 1, total: 3, label: '待生成话术',
    detail: `已选 ${pc} 品 / 已备 ${sc} 段话术`,
    tooltip: '下一步：为已选商品生成话术'
  }
  return {
    step: 2, total: 3, label: '准备就绪',
    detail: `已选 ${pc} 品 / 已备 ${sc} 段话术`,
    tooltip: '所有准备完成，可以开始直播'
  }
}
```

#### DataGrid 进度列（status === 0 分支，约第 371-387 行）

```tsx
if (status === 0) {
  const progress = getReadinessProgress(row)
  const pct = (progress.step / progress.total) * 100
  return (
    <Tooltip title={progress.tooltip} arrow>
      <Box sx={{ width: '100%', pr: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.25 }}>
          <Typography
            variant="caption"
            color={progress.step < 2 ? 'text.secondary' : 'success.main'}
            sx={{ fontWeight: progress.step >= 2 ? 600 : 400 }}
          >
            {progress.label}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {progress.detail}
          </Typography>
        </Box>
        <LinearProgress
          variant="determinate"
          value={pct}
          color={progress.step >= 2 ? 'success' : 'primary'}
          sx={{ height: 4, borderRadius: 2 }}
        />
      </Box>
    </Tooltip>
  )
}
```

#### 卡片视图 renderReadinessSteps 函数（约第 495-515 行）

添加 Tooltip 和更语义化的标签：

```tsx
const renderReadinessSteps = (row: Record<string, unknown>) => {
  const progress = getReadinessProgress(row)
  const steps = [
    { label: '选品', icon: '🛒' },
    { label: '话术', icon: '✍️' },
    { label: '就绪', icon: '✅' },
  ]
  return (
    <Tooltip title={progress.tooltip} arrow>
      <Box sx={{ mt: 0.5, mb: 0.5 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          {steps.map((s, i) => (
            <Box key={s.label} sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
              {i > 0 && (
                <Box sx={{ width: 12, height: 1, bgcolor: i <= progress.step ? 'success.main' : 'divider' }} />
              )}
              {i < progress.step ? (
                <CheckCircleOutlineIcon sx={{ fontSize: 14, color: 'success.main' }} />
              ) : (
                <RadioButtonUncheckedIcon
                  sx={{ fontSize: 14, color: i === progress.step ? 'primary.main' : 'text.disabled' }}
                />
              )}
              <Typography
                variant="caption"
                color={i < progress.step ? 'success.main' : i === progress.step ? 'primary.main' : 'text.disabled'}
                sx={{ fontWeight: i === progress.step ? 600 : 400 }}
              >
                {s.icon} {s.label}
              </Typography>
            </Box>
          ))}
        </Box>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.25, display: 'block' }}>
          {progress.detail}
        </Typography>
      </Box>
    </Tooltip>
  )
}
```

---

## 验收标准

1. ✅ 已结束场次（status=2）在 DataGrid 和卡片视图中 GMV 大字号显示
2. ✅ GMV ≥ 1000 绿色，< 1000 红色
3. ✅ "N品·M术" 改为 "已选 N 品 / 已备 M 段话术"
4. ✅ 进度条 hover 显示 Tooltip，说明下一步操作
5. ✅ 卡片视图步骤添加图标（🛒→✍️→✅）
6. ✅ `npm run type-check` 通过

## 注意事项

- `Tooltip` 已在文件顶部导入（第 13 行），无需重复导入
- `getReadinessProgress` 返回类型扩展后，所有调用处需同步更新
- emoji 图标（🛒✍️✅）在 Windows/Mac/Linux 上均可正常显示
