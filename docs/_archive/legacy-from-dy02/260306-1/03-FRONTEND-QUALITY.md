# 03 前端组件与类型安全分析

> 综合评分：55/100（C 级）
> 发现问题：31 项（P0: 5 / P1: 10 / P2: 16）

---

## P0 - 必须修复

### FE-01: 超大组件必须拆分

| 组件 | 行数 | 问题 |
|------|------|------|
| LiveSessionDetailPage.tsx | **1,592** | 13 个 useState + 40 个函数 + 4 个 useEffect |
| LiveScriptBuilderPage.tsx | **1,043** | 产品选择 + 脚本编辑 + AI 对话混合 |
| DramaEditorPage.tsx | **764** | 44 个 useState（状态爆炸） |
| MaterialProductionPage.tsx | **713** | 场景 + 镜头 + BGM + 批量操作 |
| BatchGenerationPanel.tsx | **599** | 表单 + 生成 + 进度跟踪三职责 |
| GenerationHistory.tsx | **534** | 对话框 + 表格 + 筛选 + 分页 |

**修复策略**:
- LiveSessionDetailPage → 拆为 5 个 Tab 子组件 + 1 个自定义 Hook
- DramaEditorPage → 提取 useReducer 替代 44 个 useState

---

### FE-02: `any` 类型和 `Record<string, unknown>` 泛滥

**any 使用**:
- `GenerationHistory.tsx:51,74` — `Record<GenerationTaskStatus, any>`
- `GenerationHistory.tsx:311` — `setStatusFilter(status as any)`

**Record<string, unknown> 泛滥**（12+ 个文件）:
- `ProductPage.tsx:115,128,129,132,139,163`
- `LiveSessionDetailPage.tsx:139-146`
- `LiveScriptBuilderPage.tsx:73-92`
- `FormDialog.tsx:34-35,38,51,59,69`

**修复**: 为每个模块定义具体的 VO 接口替代宽泛类型

---

### FE-03: useEffect cleanup 不完整（内存泄漏风险）

| 文件 | 问题 |
|------|------|
| useGenerationProgress.ts:157 | setInterval 需验证清理 |
| useMonitoringData.ts | setInterval 未见清理代码 |
| usePaymentProcess.ts:151 | setInterval 需 cleanup |
| useGenerationProgress.ts:63 | WebSocket ref 需确保断开 |

---

### FE-04: 错误处理不统一

10 处直接 `console.error` 而非 Toast 通知用户：
- AlertNotificationCenter.tsx:99,108
- GenerationHistory.tsx:83,271,285
- BatchGenerationPanel.tsx:85
- EvolutionOpportunitiesPanel.tsx:269,278
- EvolutionReportView.tsx:116

**另外**:
- `LiveSessionDetailPage.tsx:235` — `.catch(() => {})` 静默吃掉错误
- `LiveSessionDetailPage.tsx:192-195` — `.catch(() => null)` 级联

---

### FE-05: TODO/Mock 代码残留

- `VersionRecommendPanel.tsx:105` — `// TODO: 替换为实际的 API 调用`
- `AiAssistDialog.tsx:91` — `// TODO: 调用后端 aiAssist API`

---

## P1 - 应尽快修复

### FE-06: Props Drilling 严重
- `ProductPanel.tsx:26-46` — 接收 18 个 props
- **修复**: 使用 useContext 或提取自定义 Hook

### FE-07: Zustand Store 不完整
- 仅有 `user.ts` 一个 Store
- 缺少: UI state / 缓存 / 字典数据 Store
- 大量组件自行管理本应全局的状态

### FE-08: 缺少 useMemo/useCallback 优化
- 仅 45/293 个文件使用 useCallback
- useMemo 使用极少
- 18 个 callback props 每次重新创建（ProductPanel）

### FE-09: 列表无虚拟滚动
- VirtualList 组件存在但大多数列表未使用
- GenerationHistory 表格大数据集性能隐患

### FE-10: ErrorBoundary 功能不足
- `ErrorBoundary.tsx:28` — 仅 console.error，无错误上报
- `ErrorBoundary.tsx:45` — 无降级方案，仅重新加载

### FE-11: 路由守卫不完整
- 缺少基于角色的路由权限检查
- 路由参数类型不安全（多次 parseInt + null check）

### FE-12: 可访问性（a11y）严重不足
- 仅 13/293 个文件使用 aria- 属性
- 表格无 aria-rowcount/aria-colcount
- 表单验证错误未关联到输入框

### FE-13: 网络请求无重试机制
- `request.ts:8` — 120s timeout，但无 retry
- 应至少对 GET 请求支持自动重试

### FE-14: Promise 处理不当
- 多处 Promise.all 未处理部分失败
- LiveSessionDetailPage:191-207 部分数据源失败会导致整体污染

### FE-15: 测试覆盖极低
- 仅 8 个 .test.tsx 文件（293 个组件中）
- 关键业务逻辑零测试
- 覆盖率 < 5%

---

## P2 - 优化项

| # | 问题 | 建议 |
|---|------|------|
| FE-16 | key={index} 不稳定 | 使用唯一 ID |
| FE-17 | 图片无懒加载 | 添加 loading="lazy" |
| FE-18 | 路由懒加载写法繁琐 | 创建统一 lazyLoad helper |
| FE-19 | Context 默认值不安全 | ToastContext 返回空函数 |
| FE-20 | console.log 未清理 | 使用日志库或移除 |
| FE-21 | 类型断言滥用 | `as unknown as X` 多重转换 |
| FE-22 | 自定义 Hook 缺少文档 | 补充 JSDoc |
| FE-23 | 格式化函数接收 unknown | 强化类型签名 |
| FE-24 | useCallback 依赖不完整 | 补全依赖数组 |
| FE-25 | 无 Suspense 细粒度边界 | 仅路由级 1 处 |
| FE-26 | 前端无性能监控 | 接入 Core Web Vitals |
| FE-27 | 缺少 ESLint + Prettier | 代码风格无强制 |
| FE-28 | 国际化硬编码中文 | 所有 UI 文案硬编码 |
| FE-29 | 无 Storybook 组件文档 | 组件无可视化文档 |
| FE-30 | 缺少 skeleton loading | 数据加载时无骨架屏 |
| FE-31 | bundle size 未分析 | 需要 bundle analyzer |
