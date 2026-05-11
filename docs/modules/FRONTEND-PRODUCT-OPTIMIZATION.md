# 前端产品体验优化计划

**生成日期**: 2026-05-09  
**优先级**: P0 - 用户体验核心  
**目标**: 从真实用户视角，打通所有业务流程，提升产品可用性

---

## 核心问题诊断

### 1. 类型安全问题（影响开发效率）

**现状**:
- ❌ `as unknown as` 不安全转换：108 处
- ❌ `Record<string, unknown>` 泛型：313 处
- ❌ 空 catch 块：5+ 处
- ❌ 类型定义重复：多处

**影响**:
- 运行时类型错误风险高
- IDE 智能提示失效
- 重构困难

### 2. 数据流不完整（影响功能完整性）

**现状**:
- ❌ LiveScriptVO 缺少 22 个字段（scriptType, style, effectivenessScore 等）
- ❌ 前端无法展示话术类型、风格、效果评分
- ❌ 数据分析功能缺失

**影响**:
- 用户无法看到关键业务数据
- 功能严重不完整
- 产品价值无法体现

### 3. 用户体验问题（影响使用便捷性）

**现状**:
- ❌ 加载状态不统一（部分页面无 Skeleton）
- ❌ 错误提示不友好（技术错误直接暴露）
- ❌ 空状态无引导（用户不知道下一步做什么）
- ❌ 批量操作缺失（需要逐个点击）

**影响**:
- 用户学习成本高
- 操作效率低
- 用户流失率高

---

## 优化路线图

### 阶段 1：修复数据流（1 周）

#### 1.1 后端 VO 字段补全

**LiveScriptVO 补全**（P0）:
```java
// 添加 22 个缺失字段
private String scriptType;        // 话术类型
private String style;              // 话术风格
private Boolean aiGenerated;       // 是否 AI 生成
private Long productId;            // 关联商品
private Double effectivenessScore; // 效果评分
private Integer viewerDelta;       // 观看人数变化
private Integer interactionDelta;  // 互动量变化
private Integer conversionDelta;   // 转化量变化
// ... 其他 14 个字段
```

**工作量**: 0.5 人日

#### 1.2 前端类型定义同步

**更新 `front/src/types/live.ts`**:
```typescript
export interface LiveScript {
  id: number
  sessionId: number
  scriptContent: string
  sequenceNo: number
  
  // 新增字段
  scriptType: string
  style: string
  aiGenerated: boolean
  productId?: number
  effectivenessScore?: number
  viewerDelta?: number
  interactionDelta?: number
  conversionDelta?: number
  // ... 其他字段
}
```

**工作量**: 0.5 人日

#### 1.3 API 调用验证

**验证数据完整性**:
- 调用 `/api/v1/live/script/search` 检查返回字段
- 调用 `/api/v1/live/session/get` 检查关联数据
- 前端 console 验证数据结构

**工作量**: 0.5 人日

**阶段 1 总工作量**: 1.5 人日

---

### 阶段 2：类型安全修复（2 周）

#### 2.1 消除不安全类型转换

**优先级排序**:
1. API 层（313 处 `Record<string, unknown>`）
2. 页面组件（108 处 `as unknown as`）
3. 工具函数（类型守卫）

**修复策略**:
```typescript
// ❌ 错误：不安全转换
const data = res as unknown as ProductVO[]

// ✅ 正确：定义明确类型
interface ApiResponse {
  list: ProductVO[]
  total: number
}
const data: ApiResponse = await request.post('/api/list')
```

**工作量**: 8 人日

#### 2.2 统一错误处理

**创建错误处理工具**:
```typescript
// utils/error-handler.ts
export function handleApiError(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return '操作失败，请稍后重试'
}

// 使用示例
try {
  await api.save(data)
} catch (error) {
  enqueueSnackbar(handleApiError(error), { variant: 'error' })
}
```

**工作量**: 2 人日

**阶段 2 总工作量**: 10 人日

---

### 阶段 3：用户体验升级（3 周）

#### 3.1 统一加载状态

**创建标准 Skeleton 组件**:
```typescript
// components/base/PageSkeleton.tsx
export function TableSkeleton() {
  return (
    <Box>
      <Skeleton variant="rectangular" height={56} sx={{ mb: 2 }} />
      {[...Array(5)].map((_, i) => (
        <Skeleton key={i} variant="rectangular" height={52} sx={{ mb: 1 }} />
      ))}
    </Box>
  )
}
```

**应用到所有列表页**:
- SessionsPage
- ScriptGeneratePage
- AgentMarketPage
- ProductListPage

**工作量**: 3 人日

#### 3.2 优化错误提示

**统一错误提示组件**:
```typescript
// components/base/ErrorAlert.tsx
interface ErrorAlertProps {
  error: unknown
  onRetry?: () => void
}

export function ErrorAlert({ error, onRetry }: ErrorAlertProps) {
  const message = handleApiError(error)
  
  return (
    <Alert severity="error" action={
      onRetry && <Button onClick={onRetry}>重试</Button>
    }>
      {message}
    </Alert>
  )
}
```

**工作量**: 2 人日

#### 3.3 完善空状态

**创建空状态组件**:
```typescript
// components/base/EmptyState.tsx
interface EmptyStateProps {
  title: string
  description: string
  action?: {
    label: string
    onClick: () => void
  }
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <Box textAlign="center" py={8}>
      <Typography variant="h6" gutterBottom>{title}</Typography>
      <Typography color="text.secondary" mb={3}>{description}</Typography>
      {action && (
        <Button variant="contained" onClick={action.onClick}>
          {action.label}
        </Button>
      )}
    </Box>
  )
}
```

**应用场景**:
- 无直播场次：引导创建第一个场次
- 无话术：引导生成话术
- 无智能体：引导浏览市场

**工作量**: 2 人日

#### 3.4 批量操作

**SessionsPage 批量操作**:
```typescript
// 批量删除
const [selected, setSelected] = useState<number[]>([])

const handleBatchDelete = async () => {
  await Promise.all(selected.map(id => liveApi.sessionDelete(id)))
  toast('批量删除成功', 'success')
  invalidate()
}

// 批量修改状态
const handleBatchStart = async () => {
  await Promise.all(selected.map(id => liveApi.sessionStart(id)))
  toast('批量开播成功', 'success')
  invalidate()
}
```

**工作量**: 4 人日

#### 3.5 快捷键支持

**常用快捷键**:
- `Ctrl+N`: 新建场次
- `Ctrl+S`: 保存
- `Ctrl+F`: 搜索
- `Esc`: 关闭弹窗

**工作量**: 2 人日

**阶段 3 总工作量**: 13 人日

---

### 阶段 4：核心流程打通（4 周）

#### 4.1 直播场次完整流程

**用户旅程**:
1. 创建场次 → 填写基本信息
2. 生成话术 → AI 自动生成 + 手动编辑
3. 添加商品 → 关联商品库
4. 预览效果 → 实时预览话术
5. 开始直播 → 实时监控数据
6. 结束直播 → 查看数据报告

**需要打通的功能**:
- ✅ 场次创建（已有）
- ⚠️ 话术生成（需优化 UI）
- ⚠️ 商品关联（需补充字段）
- ❌ 实时预览（缺失）
- ⚠️ 数据监控（需优化）
- ❌ 数据报告（缺失）

**工作量**: 10 人日

#### 4.2 AI 话术工作台

**核心功能**:
1. **实时生成预览**: 边生成边展示，支持中断
2. **批量编辑**: 多选话术批量修改风格、时长
3. **版本对比**: 话术修改历史，支持回滚
4. **效果分析**: 实时展示观看、互动、转化数据
5. **智能推荐**: 基于历史数据推荐高转化话术

**工作量**: 12 人日

#### 4.3 短视频分析与策划

**核心功能**:
1. **竞品分析**: 自动抓取竞品视频，分析爆款要素
2. **热点追踪**: 集成鬼谷易牙，自动推荐选题
3. **脚本策划**: 可视化编辑器，支持分镜头
4. **素材库**: 视频素材管理，标签分类
5. **发布计划**: 自动排期，最佳时间推荐

**工作量**: 15 人日

#### 4.4 智能体市场

**核心功能**:
1. **智能推荐**: 基于用户行为推荐智能体
2. **分类筛选**: 按行业、场景、功能分类
3. **使用教程**: 每个智能体配置教程和示例
4. **评价体系**: 评分、评论、使用次数
5. **收藏夹**: 收藏常用智能体

**工作量**: 8 人日

**阶段 4 总工作量**: 45 人日

---

## 总工作量估算

| 阶段 | 目标 | 工作量 | 完成时间 |
|------|------|--------|---------|
| 阶段 1 | 修复数据流 | 1.5 人日 | 1 周 |
| 阶段 2 | 类型安全修复 | 10 人日 | 2 周 |
| 阶段 3 | 用户体验升级 | 13 人日 | 3 周 |
| 阶段 4 | 核心流程打通 | 45 人日 | 4 周 |
| **总计** | — | **69.5 人日** | **约 14 周（3.5 个月）** |

**并行开发**: 2 人前端团队可缩短至 **7 周（1.75 个月）**

---

## 验收标准

### 数据完整性
- [ ] 所有 VO 字段与 Entity 一致
- [ ] 前端类型定义与后端 VO 一致
- [ ] API 返回数据包含所有必要字段

### 类型安全
- [ ] `npm run type-check` 无错误
- [ ] 无 `any` 类型
- [ ] 无 `as unknown as` 不安全转换
- [ ] 所有 API 调用有明确类型

### 用户体验
- [ ] 所有列表页有 Skeleton 加载状态
- [ ] 所有错误有友好提示和重试按钮
- [ ] 所有空状态有引导文案和操作按钮
- [ ] 核心操作支持批量处理
- [ ] 常用操作支持快捷键

### 功能完整性
- [ ] 直播场次完整流程可用
- [ ] AI 话术工作台功能完整
- [ ] 短视频分析与策划功能完整
- [ ] 智能体市场功能完整

---

## 立即执行（本周）

1. **修复 LiveScriptVO 字段缺失**（P0）
2. **验证 API 数据完整性**
3. **创建前端类型安全修复任务**
4. **建立用户体验组件库**

---

**报告生成时间**: 2026-05-09  
**负责人**: 前端团队  
**下次审查**: 完成阶段 1 后（1 周后）
