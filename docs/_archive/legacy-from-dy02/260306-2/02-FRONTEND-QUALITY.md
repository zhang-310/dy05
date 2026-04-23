# 02 前端质量分析

## 1. 类型安全

- 生产代码无 `any` 类型 ✓
- 无 `@ts-ignore` / `@ts-expect-error` ✓
- **问题**: 512 处 `Record<string, unknown>` 应定义具体接口类型
- API 层过度使用泛型（ai.ts, live.ts, product.ts）

## 2. 大型组件（需拆分）

| 文件 | 行数 | useState 数 |
|------|------|------------|
| `pages/live/LiveScriptBuilderPage.tsx` | 1043 | 63 |
| `pages/crud/ProductPage.tsx` | 845 | 27 |
| `pages/shortvideo/DramaEditorPage.tsx` | 764 | - |
| `pages/shortvideo/MaterialProductionPage.tsx` | 713 | 14+ |
| `pages/ai/KnowledgeDocumentsPage.tsx` | 640 | - |
| `components/BatchGenerationPanel.tsx` | 599 | - |
| `components/GenerationHistory.tsx` | 541 | - |
| `pages/live/LiveRealtimePanel.tsx` | 534 | - |
| `pages/live/LiveSessionPage.tsx` | 505 | - |

## 3. console.error 替代 toast（45+ 处）

关键文件：
- `pages/system/PerformanceMonitoringPage.tsx` - 6处
- `pages/script/ScriptOptimizationPage.tsx` - 2处
- `pages/payment/RefundManagementPanel.tsx` - 2处
- `api/live-realtime.ts` - 4处
- `components/GenerationHistory.tsx` - 1处
- 及 30+ 其他位置

## 4. LazyRoute 缺少 ErrorBoundary

缺少 ErrorBoundary 的页面路由：
- `/admin/live/sessions` 及创建/编辑
- `/admin/shortvideo/*` 全部
- `/admin/ai/*` 全部
- `/admin/auth/users`
- `/admin/config`、`/admin/storage`
- `/admin/douyin/accounts`

## 5. useEffect cleanup 风险

- `pages/shortvideo/QuickGeneratePage.tsx`: setInterval 组件卸载时可能泄漏
- `pages/shortvideo/MaterialProductionPage.tsx`: pollRef 多次设置前未清理

## 6. 缺少 loading 状态

- `pages/live/LiveSessionPage.tsx:91`: searchAccounts 无 loading
- `pages/crud/SalesHistoryPage.tsx:85,127`: .catch 静默失败
- `pages/crud/ProductPage.tsx:177`: loadProductList 无 loading
