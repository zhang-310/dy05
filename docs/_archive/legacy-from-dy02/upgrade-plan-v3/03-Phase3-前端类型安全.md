# Phase 3 — 前端类型安全与错误处理（CRITICAL + HIGH）

## 目标

消除 3 个 CRITICAL 级类型安全问题 + 5 个 HIGH 级前端问题。完成后前端 API 层有完整类型覆盖，错误不再被静默吞没。

---

## 任务 3.1：API 层类型化（CRITICAL — 最高优先级）

**问题**：60+ 个 API 文件的请求/响应参数全部使用 `Record<string, unknown>`，TypeScript 类型系统形同虚设。

**涉及文件**：
- `frontend-react/src/api/*.ts`（全部）
- `frontend-react/src/types/*.ts`（需新增/补全）

**修复方案**：

以 `api/product.ts` 为示例，展示改造模式：

```typescript
// ===== 修改前 =====
// src/api/product.ts
import request from '@/utils/request';

export function searchProducts(data?: Record<string, unknown>) {
  return request.post('/product/list', data);
}

export function saveProduct(data: Record<string, unknown>) {
  return request.post('/product/save', data);
}

// ===== 修改后 =====
// src/types/product.ts — 新增/补全类型定义
import type { BasicQueryDto, PageResultVO } from './common';

export interface ProductSearchVO extends BasicQueryDto {
  keyword?: string;
  categoryId?: number;
  status?: number;
}

export interface ProductSaveVO {
  id?: number;
  name: string;
  categoryId: number;
  price: number;
  description?: string;
  sellingPoints?: string;
  profitMarginPct?: number;
}

export interface ProductVO {
  id: number;
  name: string;
  categoryId: number;
  categoryName?: string;
  price: number;
  description?: string;
  sellingPoints?: string;
  profitMarginPct?: number;
  status: number;
  ownerId: number;
  createTime: string;
  updateTime: string;
}

// src/types/common.ts — 公共类型（如不存在则新建）
export interface BasicQueryDto {
  page?: number;
  rows?: number;
  sortName?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PageResultVO<T> {
  total: number;
  list: T[];
  pageNum: number;
  pageSize: number;
}

// src/api/product.ts — 类型化
import request from '@/utils/request';
import type { ProductSearchVO, ProductSaveVO, ProductVO } from '@/types/product';
import type { PageResultVO } from '@/types/common';

export function searchProducts(data?: ProductSearchVO) {
  return request.post<PageResultVO<ProductVO>>('/product/list', data);
}

export function saveProduct(data: ProductSaveVO) {
  return request.post<number>('/product/save', data);
}

export function getProduct(data: { id: number }) {
  return request.post<ProductVO>('/product/get', data);
}

export function deleteProduct(data: { id: number }) {
  return request.post<void>('/product/delete', data);
}
```

**执行策略 — 按模块逐步改造**：

优先级排序（按使用频率和业务重要性）：

| 批次 | 模块 | API 文件 | 类型文件 |
|------|------|----------|----------|
| 1 | 核心 | product.ts, live.ts, script.ts | product.ts, live.ts, script.ts |
| 2 | AI | ai.ts, evolution.ts, search.ts | ai.ts, evolution.ts |
| 3 | 业务 | douyin.ts, copy.ts, shortvideo.ts | douyin.ts, copy.ts, shortvideo.ts |
| 4 | 支撑 | config.ts, log.ts, payment.ts, agent.ts | payment.ts, agent.ts |
| 5 | 其余 | abtest.ts, dashboard.ts, upload.ts 等 | 对应类型文件 |

**类型定义来源**：直接参考后端 VO 类（`src/main/java/**/vo/*.java`），字段名 Java camelCase → TypeScript camelCase 一一对应。

**验收**：
- [ ] `grep -rn "Record<string, unknown>" frontend-react/src/api/` 返回 0 结果
- [ ] `npm run type-check` 通过
- [ ] 所有页面功能正常

---

## 任务 3.2：消除不安全类型断言（CRITICAL）

**问题**：多处使用 `as unknown as` 双重断言绕过类型检查。

**涉及文件**：

```bash
# 搜索所有不安全断言
grep -rn "as unknown as" frontend-react/src/ --include="*.ts" --include="*.tsx"
grep -rn "as any" frontend-react/src/ --include="*.ts" --include="*.tsx" | grep -v "// eslint" | grep -v "Toolbar"
```

**修复方案**：

逐个修复，根据场景选择正确方案：

```typescript
// 场景 1：API 响应类型不匹配 — 用任务 3.1 的类型化解决
// 修改前
const result = (data as unknown as { analysisId?: string }).analysisId;
// 修改后（API 已类型化后）
const result = data.analysisId;  // data 已有正确类型

// 场景 2：组件 props 类型不匹配 — 修正 props 定义
// 修改前
const id = scriptId as unknown as object;
// 修改后
const id = String(scriptId);  // 或修正调用方传入正确类型

// 场景 3：第三方库类型不兼容 — 使用类型守卫
// 修改前
const order = data as unknown as OrderVO;
// 修改后
function isOrderVO(data: unknown): data is OrderVO {
  return typeof data === 'object' && data !== null && 'orderId' in data;
}
if (isOrderVO(data)) {
  // data 现在是 OrderVO 类型
}
```

**例外**：以下 `as any` 保留不改（已有约定）：
- DataGrid CustomToolbar 的 `(props: any)` 签名
- `slotProps.toolbar` 的 `as any` 强转

**验收**：
- [ ] `grep -rn "as unknown as" frontend-react/src/` 返回 0 结果
- [ ] `as any` 仅存在于 DataGrid Toolbar 相关代码
- [ ] `npm run type-check` 通过

---

## 任务 3.3：SSE 事件类型化

**问题**：`ai.ts:42-136` 手动解析 SSE 事件为 `Record<string, unknown>` 后直接强转，缺少字段校验。

**涉及文件**：
- `frontend-react/src/api/ai.ts:42-136`
- 其他使用 SSE 的 API 文件

**修复方案**：

```typescript
// src/types/sse.ts — 新建 SSE 事件类型
export type SSEEventType = 'status' | 'chunk' | 'done' | 'error' | 'progress';

export interface SSEBaseEvent {
  type: SSEEventType;
}

export interface SSEStatusEvent extends SSEBaseEvent {
  type: 'status';
  message: string;
}

export interface SSEChunkEvent extends SSEBaseEvent {
  type: 'chunk';
  content: string;
}

export interface SSEDoneEvent extends SSEBaseEvent {
  type: 'done';
  totalTokens?: number;
}

export interface SSEErrorEvent extends SSEBaseEvent {
  type: 'error';
  message: string;
  code?: number;
}

export interface SSEProgressEvent extends SSEBaseEvent {
  type: 'progress';
  current: number;
  total: number;
  message?: string;
}

export type SSEEvent = SSEStatusEvent | SSEChunkEvent | SSEDoneEvent | SSEErrorEvent | SSEProgressEvent;

// src/utils/sse.ts — SSE 事件解析工具
export function parseSSEEvent(raw: string): SSEEvent | null {
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || !('type' in parsed)) {
      return null;
    }
    return parsed as SSEEvent;
  } catch {
    // 纯文本 chunk（非 JSON 格式）
    return { type: 'chunk', content: raw };
  }
}
```

**验收**：
- [ ] SSE 事件解析有类型校验
- [ ] AI 聊天流式输出正常
- [ ] 异常 SSE 数据不会导致前端崩溃

---

## 任务 3.4：修复静默错误吞没（HIGH）

**问题**：10+ 个 hook 的 catch 块为空或仅 console.log，用户完全不知道操作失败。

**涉及文件**：

```bash
# 搜索空 catch 块和仅 console 的 catch
grep -rn "catch.*{" frontend-react/src/hooks/ --include="*.ts" -A 2 | grep -E "(console\.|// |/\*|^\s*\})"
```

**修复方案**：

1. 创建统一错误处理 hook：

```typescript
// src/hooks/useAsyncAction.ts — 新建
import { useState, useCallback } from 'react';
import { useToast } from '@/contexts/ToastContext';

interface AsyncActionState {
  loading: boolean;
  error: string | null;
}

export function useAsyncAction() {
  const toast = useToast();
  const [state, setState] = useState<AsyncActionState>({ loading: false, error: null });

  const execute = useCallback(async <T>(
    action: () => Promise<T>,
    options?: {
      successMessage?: string;
      errorMessage?: string;
      silent?: boolean;  // true = 不弹 toast，仅设置 error state
    }
  ): Promise<T | undefined> => {
    setState({ loading: true, error: null });
    try {
      const result = await action();
      if (options?.successMessage) {
        toast(options.successMessage, 'success');
      }
      setState({ loading: false, error: null });
      return result;
    } catch (err) {
      const message = options?.errorMessage
        ?? (err instanceof Error ? err.message : '操作失败');
      setState({ loading: false, error: message });
      if (!options?.silent) {
        toast(message, 'error');
      }
      return undefined;
    }
  }, [toast]);

  return { ...state, execute };
}
```

2. 逐步替换现有 hook 中的空 catch：

```typescript
// 修改前 — useHybridSearch.ts
try {
  const result = await searchApi(params);
  setResults(result);
} catch (err) {
  // 静默吞没
}

// 修改后
try {
  const result = await searchApi(params);
  setResults(result);
} catch (err) {
  const message = err instanceof Error ? err.message : '搜索失败';
  setError(message);
  toast(message, 'error');
}
```

**验收**：
- [ ] `grep -rn "catch.*{" frontend-react/src/hooks/ -A 1 | grep "^\s*}"` 返回 0 结果（无空 catch）
- [ ] 网络错误时用户看到 toast 提示
- [ ] 错误状态可在 UI 中展示

---

## 任务 3.5：LazyRoute 添加 ErrorBoundary（HIGH）

**问题**：`router/index.tsx:144-149` 懒加载路由没有错误边界，chunk 加载失败会白屏。

**涉及文件**：
- `frontend-react/src/router/index.tsx`

**修复方案**：

```typescript
// src/components/base/RouteErrorBoundary.tsx — 新建
import { Component, type ReactNode } from 'react';
import { Button, Box, Typography } from '@mui/material';

interface Props { children: ReactNode; }
interface State { hasError: boolean; error?: Error; }

export class RouteErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: undefined });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      const isChunkError = this.state.error?.message?.includes('Loading chunk');
      return (
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" gutterBottom>
            {isChunkError ? '页面加载失败，请刷新重试' : '页面出现错误'}
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 2 }}>
            {this.state.error?.message}
          </Typography>
          <Button variant="contained" onClick={this.handleRetry}>
            刷新页面
          </Button>
        </Box>
      );
    }
    return this.props.children;
  }
}
```

```typescript
// router/index.tsx — 包裹 LazyRoute
import { RouteErrorBoundary } from '@/components/base/RouteErrorBoundary';

function LazyRoute({ component: Component }: { component: React.LazyExoticComponent<any> }) {
  return (
    <RouteErrorBoundary>
      <Suspense fallback={<PageLoading />}>
        <Component />
      </Suspense>
    </RouteErrorBoundary>
  );
}
```

**验收**：
- [ ] 模拟 chunk 加载失败（断网后切换路由），显示友好错误页面而非白屏
- [ ] 点击"刷新页面"可恢复
- [ ] 正常路由切换不受影响

---

## 任务 3.6：Zustand Store 添加 Selector（HIGH）

**问题**：组件直接 `useUserStore()` 订阅整个 store，任何字段变化都触发全量重渲染。

**涉及文件**：
- `frontend-react/src/stores/index.ts`
- `frontend-react/src/stores/*.ts`

**修复方案**：

```typescript
// src/stores/user.ts — 添加 selector 导出
import { useShallow } from 'zustand/react/shallow';

// 细粒度 selector
export const useUserId = () => useUserStore((s) => s.userId);
export const useUserName = () => useUserStore((s) => s.username);
export const useUserRole = () => useUserStore((s) => s.role);
export const useIsLoggedIn = () => useUserStore((s) => !!s.token);

// 多字段 selector（用 useShallow 避免引用不等触发重渲染）
export const useUserProfile = () => useUserStore(
  useShallow((s) => ({ userId: s.userId, username: s.username, avatar: s.avatar }))
);
```

页面中使用 selector 替代全量订阅：

```typescript
// 修改前
const { userId, username } = useUserStore();  // 任何字段变化都重渲染

// 修改后
const userId = useUserId();      // 仅 userId 变化时重渲染
const username = useUserName();  // 仅 username 变化时重渲染
// 或
const { userId, username } = useUserProfile();  // 仅这两个字段变化时重渲染
```

**验收**：
- [ ] React DevTools Profiler 确认无不必要的重渲染
- [ ] 功能不变

---

## 任务 3.7：API 错误类型定义（HIGH）

**问题**：`request.ts:30` 的 `extractErrorMessage` 接受 `any`，不校验响应结构。

**涉及文件**：
- `frontend-react/src/utils/request.ts:30-42`

**修复方案**：

```typescript
// src/types/common.ts — 添加错误类型
export interface ApiErrorResponse {
  status: number;
  message: string;
  traceId?: string;
  timestamp?: string;
}

// src/utils/request.ts — 类型化错误提取
import type { ApiErrorResponse } from '@/types/common';
import type { AxiosError } from 'axios';

function isApiErrorResponse(data: unknown): data is ApiErrorResponse {
  return (
    typeof data === 'object' &&
    data !== null &&
    'status' in data &&
    'message' in data &&
    typeof (data as ApiErrorResponse).message === 'string'
  );
}

function extractErrorMessage(error: AxiosError): string {
  if (error.response?.data && isApiErrorResponse(error.response.data)) {
    return error.response.data.message;
  }
  if (error.message) {
    return error.message;
  }
  return '请求失败，请稍后重试';
}
```

**验收**：
- [ ] 后端返回标准错误格式时，前端正确提取 message
- [ ] 后端返回非标准格式时，前端不崩溃，显示兜底消息
- [ ] `npm run type-check` 通过

---

## Phase 3 完成标准

```bash
cd frontend-react

# 类型检查 — 0 错误
npm run type-check

# 测试通过
npm run test

# 构建成功
npm run build

# 手动验证
# 1. 所有页面正常加载
# 2. 网络错误时显示 toast 提示
# 3. chunk 加载失败显示友好错误页
# 4. AI 聊天 SSE 流式输出正常
```
