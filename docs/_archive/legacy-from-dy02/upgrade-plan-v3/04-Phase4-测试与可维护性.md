# Phase 4 — 测试补全与可维护性（HIGH + MEDIUM）

## 目标

补全前端测试基础设施 + 拆分大组件 + 性能优化。完成后测试覆盖率提升至可持续水平，大组件可维护。

---

## 任务 4.1：完善测试基础设施

**问题**：`test/setup.ts` 仅导入 jest-dom，缺少 fetch/EventSource/localStorage mock；`renderWithProviders` 缺少 Zustand provider。

**涉及文件**：
- `frontend-react/src/test/setup.ts`
- `frontend-react/src/test/utils.tsx`

**修复方案**：

```typescript
// src/test/setup.ts — 补全 mock
import '@testing-library/jest-dom';

// Mock fetch
global.fetch = vi.fn();

// Mock EventSource（SSE 测试需要）
class MockEventSource {
  url: string;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onopen: ((event: Event) => void) | null = null;
  readyState = 0;
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSED = 2;

  constructor(url: string) {
    this.url = url;
    this.readyState = MockEventSource.OPEN;
  }
  close() { this.readyState = MockEventSource.CLOSED; }
  addEventListener = vi.fn();
  removeEventListener = vi.fn();
  dispatchEvent = vi.fn(() => true);
}
global.EventSource = MockEventSource as unknown as typeof EventSource;

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = value; },
    removeItem: (key: string) => { delete store[key]; },
    clear: () => { store = {}; },
    get length() { return Object.keys(store).length; },
    key: (index: number) => Object.keys(store)[index] ?? null,
  };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock matchMedia（MUI 组件需要）
Object.defineProperty(window, 'matchMedia', {
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock IntersectionObserver（懒加载组件需要）
global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// 每个测试后清理
afterEach(() => {
  vi.restoreAllMocks();
  localStorageMock.clear();
});
```

```typescript
// src/test/utils.tsx — 补全 provider
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import { ToastProvider } from '@/contexts/ToastContext';
import type { ReactElement, ReactNode } from 'react';

const theme = createTheme();

interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  initialRoute?: string;
}

function AllProviders({ children }: { children: ReactNode }) {
  return (
    <MemoryRouter>
      <ThemeProvider theme={theme}>
        <ToastProvider>
          {children}
        </ToastProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

export function renderWithProviders(
  ui: ReactElement,
  options?: CustomRenderOptions,
) {
  const { initialRoute, ...renderOptions } = options ?? {};
  return render(ui, {
    wrapper: ({ children }) => (
      <MemoryRouter initialEntries={initialRoute ? [initialRoute] : ['/']}>
        <ThemeProvider theme={theme}>
          <ToastProvider>
            {children}
          </ToastProvider>
        </ThemeProvider>
      </MemoryRouter>
    ),
    ...renderOptions,
  });
}

export * from '@testing-library/react';
export { renderWithProviders as render };
```

**验收**：
- [ ] 现有测试全部通过（`npm run test`）
- [ ] 新测试可使用 `renderWithProviders` 包裹含 toast/router/theme 的组件
- [ ] EventSource mock 可用于 SSE hook 测试

---

## 任务 4.2：补充 API 层测试

**问题**：60+ API 文件零测试，请求参数和响应解包逻辑无覆盖。

**涉及文件**：
- `frontend-react/src/api/__tests__/` — 新建目录

**修复方案**：

创建 API 测试工具和示例测试：

```typescript
// src/api/__tests__/helpers.ts — API 测试辅助
import { vi } from 'vitest';
import type { AxiosResponse } from 'axios';

/**
 * Mock request.post 返回值
 * 注意：响应拦截器已解包 RESTResult.data，所以 mock 直接返回 data 部分
 */
export function mockApiResponse<T>(data: T) {
  return vi.fn().mockResolvedValue(data);
}

export function mockApiError(status: number, message: string) {
  const error = new Error(message) as any;
  error.response = { status, data: { status, message } };
  return vi.fn().mockRejectedValue(error);
}
```

```typescript
// src/api/__tests__/product.test.ts — 示例测试
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { searchProducts, saveProduct, deleteProduct } from '../product';
import request from '@/utils/request';

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}));

describe('product API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('searchProducts 发送正确的请求', async () => {
    const mockData = { total: 1, list: [{ id: 1, name: '测试商品' }] };
    vi.mocked(request.post).mockResolvedValue(mockData);

    const result = await searchProducts({ page: 0, rows: 30, keyword: '测试' });

    expect(request.post).toHaveBeenCalledWith('/product/list', {
      page: 0, rows: 30, keyword: '测试',
    });
    expect(result).toEqual(mockData);
  });

  it('saveProduct 发送正确的请求', async () => {
    vi.mocked(request.post).mockResolvedValue(1);

    const result = await saveProduct({
      name: '新商品', categoryId: 1, price: 99.9,
    });

    expect(request.post).toHaveBeenCalledWith('/product/save', {
      name: '新商品', categoryId: 1, price: 99.9,
    });
    expect(result).toBe(1);
  });

  it('deleteProduct 发送正确的请求', async () => {
    vi.mocked(request.post).mockResolvedValue(undefined);

    await deleteProduct({ id: 1 });

    expect(request.post).toHaveBeenCalledWith('/product/delete', { id: 1 });
  });
});
```

**覆盖策略**：优先为以下模块写测试（按业务重要性）：

| 优先级 | 模块 | 测试文件 | 测试用例数 |
|--------|------|----------|-----------|
| P0 | product, live, script | product.test.ts, live.test.ts, script.test.ts | 各 5-8 |
| P1 | ai, auth | ai.test.ts, auth.test.ts | 各 5-8 |
| P2 | 其余模块 | 各模块 .test.ts | 各 3-5 |

**验收**：
- [ ] P0 模块 API 测试全部通过
- [ ] `npm run test:coverage` 中 `src/api/` 覆盖率 ≥60%

---

## 任务 4.3：补充关键 Hook 测试

**问题**：仅 1 个 hook 测试文件，复杂 hook（SSE、搜索、分页）无测试。

**涉及文件**：
- `frontend-react/src/hooks/__tests__/` — 补充测试

**修复方案**：

```typescript
// src/hooks/__tests__/useHybridSearch.test.ts — 示例
import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useHybridSearch } from '../useHybridSearch';

// Mock API
vi.mock('@/api/search', () => ({
  hybridSearch: vi.fn(),
}));

import { hybridSearch } from '@/api/search';

describe('useHybridSearch', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('初始状态正确', () => {
    const { result } = renderHook(() => useHybridSearch());
    expect(result.current.loading).toBe(false);
    expect(result.current.results).toEqual([]);
    expect(result.current.error).toBeNull();
  });

  it('搜索成功更新结果', async () => {
    const mockResults = [{ id: 1, title: '测试结果' }];
    vi.mocked(hybridSearch).mockResolvedValue({ list: mockResults, total: 1 });

    const { result } = renderHook(() => useHybridSearch());

    act(() => {
      result.current.search('测试关键词');
    });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
      expect(result.current.results).toEqual(mockResults);
    });
  });

  it('搜索失败设置错误状态', async () => {
    vi.mocked(hybridSearch).mockRejectedValue(new Error('网络错误'));

    const { result } = renderHook(() => useHybridSearch());

    act(() => {
      result.current.search('测试');
    });

    await waitFor(() => {
      expect(result.current.error).toBe('网络错误');
      expect(result.current.loading).toBe(false);
    });
  });
});
```

**优先覆盖的 hook**：

| Hook | 测试重点 |
|------|----------|
| useHybridSearch | 搜索/防抖/错误/缓存 |
| useGenerationProgress | SSE 连接/进度更新/断开重连 |
| useEffectivenessData | 数据加载/空状态/错误 |
| useLiveRealtimePanel | 实时数据/WebSocket mock |
| useScriptVersions | 版本列表/切换/比较 |

**验收**：
- [ ] 5 个关键 hook 各有 3+ 测试用例
- [ ] `npm run test:coverage` 中 `src/hooks/` 覆盖率 ≥50%

---

## 任务 4.4：拆分大页面组件

**问题**：`StylePresetPage.tsx`（407 行）、`LiveSessionPage.tsx`（500+ 行）等页面组件过大，表单/表格/对话框全部堆在一个文件。

**涉及文件**：
- `frontend-react/src/pages/product/StylePresetPage.tsx`
- `frontend-react/src/pages/live/LiveSessionPage.tsx`

**修复方案**：

以 `StylePresetPage.tsx` 为例，拆分为：

```
pages/product/
├── StylePresetPage.tsx              # 主页面（<100 行，组合子组件）
├── components/
│   ├── StylePresetTable.tsx         # 表格 + 筛选
│   ├── StylePresetFormDialog.tsx    # 新增/编辑对话框
│   └── useStylePresetPage.ts       # 页面逻辑 hook（数据加载、CRUD 操作）
```

拆分原则：
1. 页面主文件只做布局组合，不超过 100 行
2. 表格/表单/对话框各自独立组件
3. 数据加载和操作逻辑抽到自定义 hook
4. 用 `useMemo` 包裹筛选/排序等计算

```typescript
// pages/product/components/useStylePresetPage.ts — 逻辑 hook
import { useState, useEffect, useMemo, useCallback } from 'react';
import { useToast } from '@/contexts/ToastContext';
import { searchStylePresets, saveStylePreset, deleteStylePreset } from '@/api/product';
import type { StylePresetVO, StylePresetSaveVO } from '@/types/product';

export function useStylePresetPage() {
  const toast = useToast();
  const [data, setData] = useState<StylePresetVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [categoryFilter, setCategoryFilter] = useState('');

  const filteredData = useMemo(
    () => categoryFilter
      ? data.filter((row) => row.category === categoryFilter)
      : data,
    [data, categoryFilter]
  );

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const result = await searchStylePresets();
      setData(result ?? []);
    } catch (err) {
      toast(err instanceof Error ? err.message : '加载失败', 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleSave = useCallback(async (vo: StylePresetSaveVO) => {
    await saveStylePreset(vo);
    toast('保存成功', 'success');
    await loadData();
  }, [loadData, toast]);

  const handleDelete = useCallback(async (id: number) => {
    await deleteStylePreset({ id });
    toast('删除成功', 'success');
    await loadData();
  }, [loadData, toast]);

  return {
    data: filteredData,
    loading,
    categoryFilter,
    setCategoryFilter,
    handleSave,
    handleDelete,
    refresh: loadData,
  };
}
```

```typescript
// pages/product/StylePresetPage.tsx — 精简主页面
import { PageHeader } from '@/components/base/PageHeader';
import { StylePresetTable } from './components/StylePresetTable';
import { StylePresetFormDialog } from './components/StylePresetFormDialog';
import { useStylePresetPage } from './components/useStylePresetPage';

export default function StylePresetPage() {
  const page = useStylePresetPage();
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <>
      <PageHeader title="风格预设" actions={[
        { label: '新增', onClick: () => setDialogOpen(true) },
      ]} />
      <StylePresetTable
        data={page.data}
        loading={page.loading}
        categoryFilter={page.categoryFilter}
        onCategoryChange={page.setCategoryFilter}
        onDelete={page.handleDelete}
      />
      <StylePresetFormDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSave={page.handleSave}
      />
    </>
  );
}
```

**优先拆分的页面**（按行数和复杂度）：

| 页面 | 当前行数 | 拆分后文件数 |
|------|----------|-------------|
| StylePresetPage | 407 | 4 |
| LiveSessionPage | 500+ | 5 |
| ScriptGenerationPage | 300+ | 4 |
| KnowledgeBaseListPage | 300+ | 4 |

**验收**：
- [ ] 拆分后每个文件不超过 200 行
- [ ] 页面主文件不超过 100 行
- [ ] 功能不变，UI 不变
- [ ] `npm run type-check` 通过

---

## 任务 4.5：echarts 懒加载

**问题**：echarts（~6MB）在主 bundle 中，非图表页面也加载。

**涉及文件**：
- `frontend-react/src/pages/` 中使用 echarts 的页面
- `frontend-react/vite.config.ts`（chunk 配置）

**修复方案**：

```typescript
// 方案：按需导入 echarts 模块（而非全量导入）

// 修改前
import * as echarts from 'echarts';

// 修改后 — 按需导入
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import {
  TitleComponent, TooltipComponent, GridComponent,
  LegendComponent, DataZoomComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([
  BarChart, LineChart, PieChart,
  TitleComponent, TooltipComponent, GridComponent,
  LegendComponent, DataZoomComponent,
  CanvasRenderer,
]);
```

如果多个页面使用 echarts，创建统一入口：

```typescript
// src/utils/echarts.ts — 统一按需导入
import * as echarts from 'echarts/core';
// ... 按需注册（同上）
export default echarts;
export type { EChartsOption } from 'echarts';
```

页面中改为：

```typescript
import echarts from '@/utils/echarts';
```

**验收**：
- [ ] `npm run build` 后 echarts chunk 体积减少 50%+
- [ ] 图表页面功能正常
- [ ] 非图表页面不加载 echarts 代码

---

## 任务 4.6：常量去重

**问题**：`STYLE_CODE_LABELS` 在 3+ 个文件中重复定义。

**涉及文件**：

```bash
grep -rn "STYLE_CODE_LABELS\|styleCodeLabels\|STYLE_LABELS" frontend-react/src/ --include="*.ts" --include="*.tsx"
```

**修复方案**：

在 `types/product.ts` 或 `utils/constants.ts` 中定义唯一来源：

```typescript
// src/types/product.ts — 添加常量
export const STYLE_CODE_LABELS: Record<string, string> = {
  professional: '专业讲解',
  casual: '轻松日常',
  emotional: '情感共鸣',
  humorous: '幽默搞笑',
  storytelling: '故事叙述',
} as const;
```

其他文件统一从此处导入：

```typescript
import { STYLE_CODE_LABELS } from '@/types/product';
```

**验收**：
- [ ] `STYLE_CODE_LABELS` 仅在一处定义
- [ ] 所有使用处从同一来源导入
- [ ] `npm run type-check` 通过

---

## Phase 4 完成标准

```bash
cd frontend-react

# 类型检查
npm run type-check

# 全部测试通过
npm run test

# 覆盖率报告
npm run test:coverage
# 目标：src/api/ ≥60%, src/hooks/ ≥50%

# 构建成功 + 体积检查
npm run build
# echarts chunk 应 < 500KB（原 ~1.5MB）

# 手动验证
# 1. 拆分后的页面功能/UI 不变
# 2. 图表页面正常渲染
# 3. 非图表页面加载速度提升
```

---

## 全量完成标准（Phase 1-4）

```bash
# 后端
mvn compile && mvn test

# 前端
cd frontend-react
npm run type-check    # 0 错误
npm run test          # 全部通过
npm run build         # 构建成功

# 安全验证
# 1. 未认证访问 /api/v1/product/list → 401
# 2. 无 CSRF token 的 POST → 403
# 3. application.yml 无明文密钥
# 4. ES 无认证访问 → 401

# 性能验证
# 1. AbTest 列表查询 SQL ≤ 2 条
# 2. EXPLAIN ANALYZE 确认索引生效
# 3. echarts chunk < 500KB

# 类型安全验证
# 1. grep "Record<string, unknown>" src/api/ → 0 结果
# 2. grep "as unknown as" src/ → 0 结果
# 3. grep "catch.*{\s*}" src/hooks/ → 0 结果（无空 catch）
```
