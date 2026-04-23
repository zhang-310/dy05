# AI 管理后台前端实现指南

> 基于 `docs/product/AI-ADMIN-PRODUCT-DESIGN.md`，在 **Vue 3 + Element Plus** 中实现最终前端页面。

---

## 一、现状 vs 目标

| 模块 | 当前页面 | 状态 | 目标（产品设计） |
|------|----------|------|------------------|
| 数据看板 | 无 | ❌ 缺失 | 概览卡片、调用趋势图、异常告警 |
| 知识库 | KnowledgeBase.vue | ✅ 已有 | 增强：卡片式列表、使用统计、批量操作 |
| 知识进化 | 进化概览在 KnowledgeBase.vue | ⚠️ 分散 | 独立进化页：任务/主题/报告/爆款拆解 |
| 创意工坊 | MediaStudio.vue | ✅ 已有 | 增强：生成历史、智能推荐 |
| 模型配置 | ModelManagement + TaskModelConfig | ✅ 已有 | 已满足 |
| 监控中心 | 无 | ❌ 缺失 | 性能指标、熔断、调用日志、额度 |
| 运维工具 | InfraManagement.vue | ✅ 已有 | 已满足 |
| 知识源 | 无 | ❌ 缺失 | 新建 KnowledgeSource.vue |

---

## 二、实现优先级与工作量

| 优先级 | 页面 | 预估 | 依赖 |
|--------|------|------|------|
| P0 | 数据看板 | 1 天 | 需后端统计 API |
| P0 | 知识源管理 | 0.5 天 | 后端已有 |
| P1 | 知识进化独立页 | 1 天 | 从 KnowledgeBase 拆出 |
| P1 | 监控中心 | 1 天 | 需部分新 API |
| P2 | 知识库增强 | 0.5 天 | 卡片式、批量 |
| P2 | WebSocket 导入进度 | 0.5 天 | 后端 WebSocket |

---

## 三、分步实现方案

### 3.1 数据看板（AiDashboard.vue）

**路由**：`/admin/ai/dashboard`

**页面结构**：
```
el-row（统计卡片）→ el-row（ECharts 趋势图）→ el-row（热门功能 + 告警）
```

**实现要点**：
1. 调用 `GET /dashboard/admin` 或新建 `GET /ai/admin/dashboard/stats`
2. 统计卡片：总调用、今日额度、P95 延迟、成功率（用 `el-statistic`）
3. 趋势图：ECharts 折线图，数据来自 `ai_call_log` 聚合
4. 告警：复用 `checkInfraHealth()`，异常时展示

**后端需补充**（若无）：
- `AiDashboardController`：`/ai/admin/dashboard/stats` 返回 `{ totalCalls, quota, p95Ms, successRate, trendData[] }`

---

### 3.2 知识源管理（KnowledgeSource.vue）

**路由**：`/admin/ai/knowledge-source`

**API**：已有
- `POST /ai/admin/knowledge-source/search`
- `POST /ai/admin/knowledge-source/get`
- `POST /ai/admin/knowledge-source/save`
- `POST /ai/admin/knowledge-source/delete`

**页面结构**：
- 搜索栏：sourceName、sourceType、status
- 表格：el-table，列：sourceName、sourcePath、sourceType、fileCount、indexCount、lastIndexTime、status、操作
- 弹窗：新增/编辑表单

**参考**：`TaskModelConfig.vue` 的表格 + 弹窗模式

---

### 3.3 知识进化独立页（EvolutionCenter.vue）

**路由**：`/admin/ai/evolution-center`

**内容**：将 KnowledgeBase.vue 中的「进化概览 + 主题池」拆出，并整合 EvolutionEngine.vue 的爆款拆解、直播复盘。

**Tab 结构**：
- Tab1：进化概览（ROI 指标 + 评分趋势 + 主题分布）
- Tab2：主题池管理（表格 CRUD）
- Tab3：进化任务列表
- Tab4：爆款拆解 + 直播复盘（来自 EvolutionEngine）

**API**：已有 `getEvolveStatus`、`listEvolveTopics`、`saveEvolveTopic`、`deleteEvolveTopic`、`listEvolveTasks` 等

---

### 3.4 监控中心（AiMonitoring.vue）

**路由**：`/admin/ai/monitoring`

**内容**：
- 性能指标：P50/P99/QPS（`getSearchStats`、`getCacheStats`）
- 熔断状态：`checkInfraHealth` 结果
- 调用日志：需 `POST /ai/call-log/search` 分页
- 额度管理：`getAiQuotaInfo` + 展示

**页面结构**：el-tabs（性能 | 熔断 | 日志 | 额度）

---

### 3.5 导航重组

产品设计为「AI 智能中心」顶级菜单，当前为「AI 管理」子菜单。两种方案：

**方案 A（推荐）**：保持现有 AdminLayout 结构，在「AI 管理」下增加：
- 数据看板
- 知识进化中心（合并进化相关）
- 监控中心
- 知识源管理

**方案 B**：新建「AI 智能中心」布局，按产品设计的 7 大模块重构导航。

---

## 四、通用实现模式

### 4.1 页面模板（Vue 3 Composition API）

```vue
<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>页面标题</span>
          <el-button type="primary" @click="handleAdd">新增</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <!-- 列定义 -->
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listXxx } from '@/api/ai'

const loading = ref(false)
const list = ref([])

async function loadData() {
  loading.value = true
  try {
    list.value = await listXxx()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
```

### 4.2 API 封装位置

- 新增 API 写在 `frontend/src/api/ai.ts`
- 若模块独立，可建 `frontend/src/api/ai-dashboard.ts` 等

### 4.3 路由注册

在 `frontend/src/router/modules/ai.ts` 中增加：

```ts
{
  path: 'ai/dashboard',
  name: 'AiDashboard',
  component: defineAsyncComponent(() => import('@/views/ai/AiDashboard.vue')),
  meta: { title: 'AI 数据看板', roles: ['admin'] },
},
```

### 4.4 菜单注册

在 `frontend/src/layouts/AdminLayout.vue` 的 AI 子菜单中增加对应 `el-menu-item`。

---

## 五、实施顺序建议

1. **知识源管理**（0.5 天）— 后端已有，直接做前端
2. **数据看板**（1 天）— 先做静态卡片 +  mock 数据，再接真实 API
3. **知识进化独立页**（1 天）— 从 KnowledgeBase 抽离 + 整合 EvolutionEngine
4. **监控中心**（1 天）— 复用 InfraManagement 的 health/cache/search API
5. **WebSocket 导入进度**（0.5 天）— 可选，需后端支持

---

## 六、P2 通用组件（已落实）

为提升 AI 管理页面复用性，抽取了以下通用组件，位于 `frontend/src/components/ai/`：

| 组件 | 路径 | 用途 |
|------|------|------|
| MetricCard | `ai/MetricCard.vue` | 指标卡片：图标、数值、标题、趋势文案，支持点击跳转 |
| TrendChart | `ai/TrendChart.vue` | ECharts 折线趋势图，支持空态、loading |
| HealthStatus | `ai/HealthStatus.vue` | 健康/告警列表，支持 list（看板）和 tag（监控中心）两种样式 |

**使用示例**：

```vue
<!-- MetricCard -->
<MetricCard
  icon="📞"
  :value="totalCalls"
  title="总调用"
  trend="↑ 12%"
  trend-type="success"
  to="/admin/ai/monitoring"
/>

<!-- TrendChart -->
<TrendChart
  title="📈 调用趋势"
  :data="callVolumeData"
  :loading="loading"
  empty-text="暂无数据"
  series-name="调用量"
/>

<!-- HealthStatus -->
<HealthStatus
  :list="healthList"
  :loading="loading"
  variant="list"
  empty-text="暂无告警"
/>
```

---

## 七、升级记录（P0/P1/P2）

| 阶段 | 内容 |
|------|------|
| P0 | 调用日志 API + 前端列表、导入进度弹窗（轮询）、错误与空态统一处理 |
| P1 | 进化报告 Markdown 渲染（marked）、数据导出（CSV）、创意工坊 TTS/视频布局升级 |
| P2 | 通用组件抽取（MetricCard、TrendChart、HealthStatus）、文档更新 |

---

## 八、给 Cursor 的指令示例

```
请实现知识源管理前端页面 KnowledgeSource.vue：
1. 路由 /admin/ai/knowledge-source
2. 调用 /ai/admin/knowledge-source 的 search/get/save/delete
3. 表格展示 sourceName、sourcePath、sourceType、fileCount、indexCount、lastIndexTime、status
4. 弹窗表单新增/编辑，参考 TaskModelConfig.vue
5. 在 router/modules/ai.ts 和 AdminLayout.vue 中注册
```

```
请实现 AI 数据看板 AiDashboard.vue：
1. 路由 /admin/ai/dashboard
2. 4 个统计卡片：总调用、今日额度、P95 延迟、成功率
3. ECharts 折线图展示调用趋势（可先用 mock 数据）
4. 底部展示基础设施健康状态（调用 checkInfraHealth）
5. 参考 KnowledgeBase.vue 的 evolve-overview 布局
```
