# Admin 前端第二轮深度评审 — 设计师 + 产品经理双视角

> 评审时间：2026-03-22 · 基于 Cursor 第一轮升级后的代码现状
> 评审维度：信息架构 / 视觉系统 / 交互模式 / 工作流 / 组件质量 / 移动端 / 可用性

---

## 一、第一轮升级完成度评估

### 已落地项（6/12）

| 编号 | 方案 | 状态 | 质量 |
|------|------|------|------|
| NV-01 | 顶部 6 Tab + 上下文侧栏 | ✅ 已落地 | ⭐⭐⭐⭐ 架构正确 |
| CT-01 | 内容库统一 Tab 页 | ✅ 已落地 | ⭐⭐⭐⭐ 6 Tab + Lazy 加载 |
| LV-01 | 直播工作台 | ⚠️ 名义落地 | ⭐⭐ 仅薄 Tab 包装 |
| DB-01 | Dashboard 待办 | ⚠️ 名义落地 | ⭐⭐ 待办有但粗糙 |
| OB-01 | Onboarding | ✅ 已落地 | ⭐⭐⭐ 三层引导 |
| UX-02B | 状态管理优化 | ✅ 已落地 | ⭐⭐⭐ 3 个新 Store |

### 未落地 / 需深化项（6/12）

| 编号 | 方案 | 状态 | 问题 |
|------|------|------|------|
| PD-01 | 商品一页化 | ❌ 未实施 | 仍然 7 个独立页面 |
| SV-01 | 短视频项目驱动 | ❌ 未实施 | 侧栏仍 24 个菜单项 |
| AI-01 | AI 分层 | ⚠️ 结构做了 | 运维组仍 8 项过多 |
| UX-01 | AI 助手升级 | ❌ 未实施 | 仅显示路径+跳转链接 |
| UX-02A | 通用组件库 | ❌ 未实施 | 无 ProductPicker/StepWorkbench |
| UX-02C | 移动端适配 | ⚠️ 部分 | 底部导航有，表格/弹窗未适配 |

---

## 二、设计师视角 · 8 个维度深度审查

### D1. 视觉层级与信息密度

**问题 1：Dashboard 信息密度过高，层级扁平**

当前 Dashboard 自上而下堆叠了 5 个区块，无视觉节奏：

```
GettingStartedChecklist  ← 可折叠卡片
今日待办卡片组（3列）     ← 小卡片
GMV / 场次（4 列统计卡） ← StatCard
直播中提示 Alert          ← Alert 条
管理统计卡（8 列）        ← StatCard × 8
商品就绪度卡片            ← Card + Chip 组
快捷入口（23 个按钮）     ← 23 个 Button 铺满
```

**设计问题**：
- 7 个区块**无间隔节奏**（全部 mb: 2，视觉上是均匀灰色平板）
- StatCard 重复出现 12 个（上4个GMV + 下8个管理），视觉上无法区分业务指标和系统指标
- 23 个快捷入口按钮**无分类**、无优先级，信息密度等同白噪声
- 没有使用**色块/背景/分隔线**建立视觉区域感

**建议 D1-A**：**三区布局 + 色块区分**

```
┌──────────────────────────────────────────────────────────┐
│ [Hero 区 · 浅蓝渐变背景]                                  │
│                                                          │
│  早上好，管理员 👋                今日 3 场直播             │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐                    │
│  │ GMV  │ │ 场次 │ │ 内容 │ │ AI   │  ← 只保留 4 个 KPI  │
│  │¥128k │ │ 5/8  │ │ 23条 │ │ 1.2k │                    │
│  │↑12%  │ │ 3待播│ │ ↑8% │ │ 68% │                     │
│  └──────┘ └──────┘ └──────┘ └──────┘                    │
│  [正在直播: 春季护肤专场]  [进入→]                          │
└──────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────┐
│ [待办区 · 白底]                                           │
│                                                          │
│  [待处理 5] [进行中 3] [已完成 12]   ← SegmentedControl   │
│                                                          │
│  📋 「春季护肤专场」3 个商品话术未生成        [去生成→]      │
│  ⚠️ 「烟酰胺精华」合规检测不通过              [查看→]       │
│  ✅ 爆款视频「XX」深度分析完成                 [查看→]       │
└──────────────────────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────┐
│ [商品就绪 · 浅灰背景]                                     │
│                                                          │
│  进度条: ████████░░ 80% 就绪   [打开就绪看板→]             │
│  链接 45/50 | 卖点 42/50 | 话术 38/50 | 激活 35/50       │
└──────────────────────────────────────────────────────────┘
```

**关键改动**：
- **删除 23 个快捷入口** — 顶部 Tab 已替代其功能
- **删除 8 个管理统计卡** — 移到「设置」模块的系统监控页
- **Hero 区用渐变背景色**区分于下方白底区域
- **待办区增加分段控制器**（待处理/进行中/已完成）
- **商品就绪改为进度条**而非 5 个 Chip

**改动文件**：
- `DashboardPage.tsx` — 完全重写为三区布局
- 新建 `components/base/HeroSection.tsx` — Hero 区组件
- 新建 `components/base/ActionItemPanel.tsx` — 待办面板（含分段控制器）
- 删除 `buildAdminQuickLinks` / `buildOrgQuickLinks` / `buildTalentQuickLinks`

---

### D2. 侧栏信息过载 — 短视频和商品

**问题**：NV-01 的侧栏虽然按模块分组了，但每个模块内**菜单项仍然太多**：

| 模块 | 侧栏项数 | 分组数 | 问题 |
|------|---------|--------|------|
| 短视频 | **24 项** | 3 组 | 每组 7-10 项，扫描成本高 |
| 商品 | **16 项** | 4 组 | 商品库组 7 项、内容组 5 项 |
| AI | **15 项** | 4 组 | AI 运维组 8 项 |
| 直播 | 6 项 | 1 组 | ✅ 合理 |
| 设置 | 15 项 | 5 组 | ✅ 后台管理可接受 |

**建议 D2-A**：**短视频侧栏精简（24 → 8 + 更多折叠）**

```
项目与生产 ▼
├── 短视频看板         ← 保留
├── 我的项目           ← 保留（点进后进入项目工作台）
├── 快速生成           ← 保留
├── 素材库             ← 保留
└── 更多：策划/分镜/素材准备/素材生产/拍摄任务  ← 折叠隐藏

发布与运营 ▼
├── 内容日历           ← 保留（合并日更+发布）
├── 数据分析           ← 保留
└── 更多：短剧/视频剪辑/质量仪表板  ← 折叠隐藏

增长 ▼
├── 爆款库             ← 保留
├── 竞品监控           ← 保留
└── 更多：效果预测/工作流/视频管理/分类/统计  ← 折叠隐藏
```

每组常用项 2-3 个直接显示，低频项收入「更多」折叠。用户看到 8 个核心入口而非 24 个。

**建议 D2-B**：**商品侧栏精简（16 → 6 + 折叠）**

```
商品库
├── 商品列表           ← 保留（主入口）
├── 商品就绪度         ← 保留
└── 更多：销售历史/风格预设/效果评分/话术版本/话术优化  ← 折叠

内容库
├── 内容库（统一）     ← 保留（主入口）
└── 更多：话术库/话术模板/文案库/话术梗库  ← 折叠（均已 redirect 到内容库 Tab）

合规
├── 违规检测           ← 保留

抖音
├── 账号管理           ← 保留
```

**实现方式**：`adminModuleConfig.tsx` 中为 MenuItemDef 新增 `collapsed?: boolean` 属性，BaseLayout 渲染时对 collapsed 项做「更多▼」折叠。

**改动文件**：
- `layouts/adminModuleConfig.tsx` — 菜单项添加 collapsed 标记
- `layouts/BaseLayout.tsx` — 侧栏渲染支持「更多」折叠

---

### D3. 卡片组件视觉单调

**问题**：全站所有卡片使用相同的 `Card + CardContent` 样式，无视觉差异：
- Dashboard StatCard = 白底 Card
- 待办项 = 白底 Card
- 商品就绪度 = 白底 Card
- 列表行点击 = 白底 Card

所有元素视觉权重相同，用户无法快速区分重要信息。

**建议 D3-A**：**引入 3 级卡片层次**

| 层次 | 样式 | 使用场景 |
|------|------|---------|
| **Hero 卡片** | 渐变背景 + 大字号 + 投影 elevation=4 | KPI 指标、GMV |
| **标准卡片** | 白底 + 细边框 variant="outlined" | 待办项、列表行 |
| **辅助卡片** | 浅灰背景 + 无边框 | 提示信息、折叠内容 |

**新建组件**：

```typescript
// components/base/KpiCard.tsx
interface KpiCardProps {
  title: string
  value: string | number
  trend?: { value: number; label: string } // +12% vs 昨日
  icon: ReactNode
  color: 'primary' | 'success' | 'warning' | 'error'
  onClick?: () => void
  subtitle?: string // 例如 "3 场待播"
}
```

特征：
- 左侧大色块圆角图标区（64×64，非现在的 48×48）
- 主值 `variant="h4"` 加粗（现在是 h5）
- Trend Chip 内置（不再在 DashboardPage 内联构建复杂 JSX）
- subtitle 在 value 下方 `variant="caption"`

**改动文件**：
- 新建 `components/base/KpiCard.tsx`
- `DashboardPage.tsx` — 用 KpiCard 替代 StatCard

---

### D4. 直播工作台过于简陋

**现状**：`LiveWorkbenchPage.tsx` 仅 47 行，只是两个 Tab 包裹已有页面：

```tsx
<Tabs value={tab} onChange={(_, v) => setTab(v)}>
  <Tab label="场次与数据" />
  <Tab label="选品与话术" />
</Tabs>
{tab === 0 && <LiveSessionDetailPage />}
{tab === 1 && <SessionWorkspacePage />}
```

**设计问题**：
- **没有顶部 Header**：不显示场次名称、状态、就绪度进度
- **没有步骤指示器**：用户不知道整体流程走到哪一步
- **两个 Tab 命名抽象**：「场次与数据」和「选品与话术」无法体现工作流阶段
- **直接嵌入两个重型页面**（DetailPage + WorkspacePage），没有真正合并

**建议 D4-A**：**完整的工作台 Header + Stepper**

```
┌──────────────────────────────────────────────────────────┐
│ ← 返回场次列表                                            │
│                                                          │
│ 春季护肤专场                                              │
│ 状态：准备中 | 就绪度 ████████░░ 65% | 开播：今天 15:00   │
│                                                          │
│ ① 基础信息 ──② 选品(8) ──③ 话术(5/8) ──④ 合规 ──⑤ 就绪   │
│    ✅           ✅          🔄 进行中      ⬜       ⬜      │
├──────────────────────────────────────────────────────────┤
│ [主内容区：根据当前步骤切换]                                │
└──────────────────────────────────────────────────────────┘
```

**关键元素**：
- **场次 Header**：名称、状态 Chip、就绪度进度条、开播时间
- **线性 Stepper**：5 步流程，每步显示完成/进行中/未开始
- **步骤可点击跳转**：不必按顺序（但灰色步骤提示「需先完成 xxx」）
- Stepper 自动根据数据库状态计算（有商品 → ②✅，话术全生成 → ③✅）

**改动文件**：
- `LiveWorkbenchPage.tsx` — 重写为 Header + Stepper + 步骤内容
- 新建 `components/live/LiveWorkbenchHeader.tsx` — Header 组件
- 新建 `components/base/WorkflowStepper.tsx` — 通用步骤条（直播/短视频共用）

---

### D5. AI 助手形同虚设

**现状**：`FloatingAiAssistant.tsx` 仅 69 行，功能仅为：
1. 显示当前页面路径
2. 一个「前往 AI 智能体」按钮

```tsx
<Typography variant="caption">当前页：{pathname}{search}</Typography>
<Button onClick={() => navigate(`${prefix}/agent`)}>前往 AI 智能体</Button>
```

这不是「AI 助手」，这是一个跳转链接。

**建议 D5-A**：**上下文感知 AI 面板**

```
┌──────────────────────────────────┐
│ 🤖 AI 助手               [最小化] │
├──────────────────────────────────┤
│ 📍 商品 · 烟酰胺精华液            │
│                                  │
│ 快捷操作：                        │
│ ┌────────────┐ ┌────────────┐   │
│ │ ✨ 生成话术 │ │ 🔍 合规检测 │   │
│ └────────────┘ └────────────┘   │
│ ┌────────────┐ ┌────────────┐   │
│ │ 📊 竞品分析 │ │ 💡 卖点提取 │   │
│ └────────────┘ └────────────┘   │
│                                  │
│ ────── 或 输入指令 ──────         │
│ ┌──────────────────────────────┐ │
│ │ 帮我生成 3 版不同风格话术     │ │
│ └──────────────────────────────┘ │
│                      [发送→]     │
│                                  │
│ [对话历史▼]                       │
└──────────────────────────────────┘
```

**上下文路由规则**：

| 当前页面路径 | 识别上下文 | 推荐操作 |
|-------------|-----------|---------|
| `/product` | 商品列表 | 批量生成话术、导入商品 |
| `/product/*` 详情 | 当前商品 | 生成话术、合规检测、竞品分析 |
| `/live/workbench/*` | 当前直播场次 | 生成话术、就绪检查、节奏建议 |
| `/shortvideo/viral` | 爆款库 | 爆款分析、二创方案、脚本生成 |
| `/ai/knowledge` | 知识库 | 搜索知识、上传文档 |
| 其他 | 通用 | 全局搜索、跳转页面 |

**实现**：
- `usePageContext()` 扩展：解析路径 → 获取当前实体（商品 ID / 场次 ID）
- 按上下文映射到 Agent 的 tool 列表
- 快捷操作按钮直接调 Agent API（不跳页面，结果在面板内展示）
- 文本输入框走 Agent Chat API

**改动文件**：
- `FloatingAiAssistant.tsx` — 完全重写
- `hooks/usePageContext.ts` — 扩展上下文解析
- 新建 `components/AiAssistantPanel.tsx` — 完整面板组件
- 新建 `hooks/useAiContextActions.ts` — 上下文 → 推荐操作映射

---

### D6. 列表页一律 DataGrid — 直播/项目应用卡片

**问题**：直播场次、短视频项目这类「工作单元」仍用 DataGrid 表格展示。表格适合高密度数据浏览（日志、商品），但不适合「少量条目 + 丰富状态」的场景。

**建议 D6-A**：**直播场次列表改为卡片布局**

```
┌────────────────────────────────────────────────────┐
│ 🔴 直播中                                           │
│ ┌─ 春季护肤专场 ──────────────── 今天 15:00 ──────┐  │
│ │ 8 商品 · 话术 5/8 · 就绪 65%                    │  │
│ │ ████████░░                                      │  │
│ │ [进入工作台→]  [实时提词→]                        │  │
│ └──────────────────────────────────────────────────┘  │
│                                                      │
│ 📋 即将开播                                          │
│ ┌─ 晚间彩妆秀 ───────────────── 今天 20:00 ──────┐  │
│ │ 12 商品 · 话术 12/12 · 就绪 100% ✅              │  │
│ │ [进入工作台→]                                    │  │
│ └──────────────────────────────────────────────────┘  │
│                                                      │
│ 📂 已结束 (3)                                  [展开▼] │
└────────────────────────────────────────────────────┘
```

特征：
- 按状态分组（直播中 → 即将开播 → 已结束折叠）
- 每张卡片显示：进度条 + 关键指标 + CTA 按钮
- 已结束场次默认折叠，展开后显示 GMV/转化率等复盘指标

**改动文件**：
- `LiveSessionPage.tsx` — 从 DataGrid 改为卡片分组布局
- 新建 `components/live/LiveSessionCard.tsx`

---

### D7. 颜色系统不统一

**问题**：StatCard 的 `color` 属性使用字符串拼接 `${color}20`（带 alpha），但各处使用不一致：

```typescript
// DashboardPage.tsx 中：
color="primary.main"     // 蓝
color="info.main"        // 浅蓝
color="success.main"     // 绿
color="secondary.main"   // 紫
color="warning.main"     // 橙
```

没有语义对应关系。「登录日志」用 info，「同步日志」也用 info；「GMV」用 success，「在线」用 warning。颜色不传达含义。

**建议 D7-A**：**建立语义色彩映射**

| 语义 | 颜色 | 使用场景 |
|------|------|---------|
| **金额/营收** | `success.main` (绿) | GMV、销售额 |
| **数量/计数** | `primary.main` (蓝) | 用户数、场次数、内容条数 |
| **告警/待处理** | `warning.main` (橙) | 直播中、待审核、合规不通过 |
| **AI/智能** | `secondary.main` (紫) | AI 调用、智能体、知识库 |
| **系统/技术** | `grey.600` (灰) | API 成功率、日志、系统状态 |

**改动文件**：
- 新建 `theme/semanticColors.ts` — 语义色彩常量
- `DashboardPage.tsx` — 应用语义色彩

---

### D8. 表单/弹窗尺寸不统一

**问题**：不同页面的 Dialog/Drawer 尺寸各异：
- ProductEditDrawer: ~600px
- BatchProductDialog: maxWidth="md" (900px)
- QualityCheckDialogs: maxWidth="sm" (600px)
- ViralLibraryPage 内弹窗: 混用 sm/md/lg

**建议 D8-A**：**统一弹窗尺寸规范**

| 类型 | 宽度 | 使用场景 |
|------|------|---------|
| **轻量确认** | maxWidth="xs" (444px) | 删除确认、简单输入 |
| **标准表单** | maxWidth="sm" (600px) | 编辑表单、收藏弹窗 |
| **详情面板** | Drawer 640px | 商品详情、话术详情 |
| **复杂编辑** | maxWidth="md" (900px) | 批量操作、对比视图 |
| **全屏工作** | fullScreen on mobile, maxWidth="lg" on desktop | 编辑器、画布 |

**改动文件**：
- 新建 `theme/dialogSizes.ts` — 尺寸常量
- 各 Dialog 组件统一引用

---

## 三、产品经理视角 · 6 个维度深度审查

### PM1. 商品模块仍然碎片化 — 最大遗留问题

**现状**：商品相关功能仍分散在 7 个独立页面：

```
侧栏「商品库」组（7 项）：
├── 商品列表        /admin/product
├── 商品就绪度      /admin/product/readiness
├── 销售历史        /admin/product/sales-history
├── 风格预设        /admin/product/style-preset
├── 效果评分        /admin/product/effectiveness
├── 话术版本        /admin/product/script-versions
└── 话术优化        /admin/product/script-optimization
```

**用户痛点**：查看一个商品的话术效果，需要：
1. 在「商品列表」找到商品
2. 跳到「效果评分」查看评分
3. 跳到「话术版本」查看版本历史
4. 跳到「话术优化」获取建议
5. 跳到「风格预设」选择风格
6. 回到「商品列表」操作

**建议 PM1-A**：**商品详情多 Tab 抽屉（PD-01 重新实施）**

```
商品列表（DataGrid）── 点击行 → 右侧抽屉 640px ──
                                                  │
                       ┌──────────────────────────┐│
                       │ 烟酰胺精华   就绪 ██ 80%  ││
                       ├──────────────────────────┤│
                       │ [基础] [话术] [效果] [历史]││
                       │                          ││
                       │ 当前 Tab：话术             ││
                       │ v3 (激活) 评分92 用15次   ││
                       │ v2 (归档) 评分78          ││
                       │                          ││
                       │ [🤖 AI 生成] [风格▼]      ││
                       │ [📊 优化建议]              ││
                       └──────────────────────────┘│
```

**Tab 映射**：

| Tab | 内容 | 取代页面 |
|-----|------|---------|
| 基础 | 名称/价格/卖点/图片 | ProductEditDrawer |
| 话术 | 版本列表+AI生成+风格预设+优化建议 | ScriptVersionPage + OptimizationPage + StylePresetPage |
| 效果 | 评分+使用统计 | EffectivenessScorePage |
| 历史 | 销售数据+趋势图 | SalesHistoryPage |

**侧栏变化**：

```
商品库（精简后）
├── 商品列表       ← 保留（主入口，详情走抽屉）
├── 商品就绪度     ← 保留（全局视图，非单商品）
```

5 个页面合并到抽屉 Tab 中，侧栏从 7 项降到 2 项。

**改动文件**：
- 新建 `pages/product/ProductDetailDrawer.tsx` — 多 Tab 详情抽屉
- 新建 `pages/product/tabs/ProductBasicTab.tsx`
- 新建 `pages/product/tabs/ProductScriptTab.tsx` — 话术版本+生成+优化
- 新建 `pages/product/tabs/ProductEffectivenessTab.tsx`
- 新建 `pages/product/tabs/ProductHistoryTab.tsx`
- `ProductPage.tsx` — 行点击打开新抽屉
- `adminModuleConfig.tsx` — 侧栏精简

---

### PM2. 短视频侧栏仍是功能堆砌

**现状**：短视频侧栏 3 组 24 项，用户仍然面对菜单墙。`SvProjectWorkbenchPage` 虽然创建了但侧栏没有引导用户优先使用它。

**建议 PM2-A**：**项目驱动入口 + 工具折叠**

侧栏精简为：

```
核心入口
├── 我的项目        ← 主入口（从项目进入工作台）
├── 快速生成        ← 快捷入口
├── 爆款库          ← 高频
├── 素材库          ← 高频
├── 内容日历        ← 高频
├── 数据分析        ← 高频

高级工具 ▼ (默认折叠)
├── 脚本策划 / 分镜设计 / 素材准备 / 素材生产
├── 短剧编辑器 / 视频剪辑
├── 拍摄任务 / 审核发布 / 一键日更
├── 竞品监控 / 效果预测 / 质量仪表板
├── 工作流编辑器 / 视频管理 / 分类管理 / 统计
```

常显 6 项 + 折叠 14 项。大多数高级功能应通过「项目工作台」的步骤流程进入，而非直接从侧栏选。

**改动文件**：
- `adminModuleConfig.tsx` — 短视频侧栏重组

---

### PM3. 内容库 Tab 内菜单仍有重复路径

**现状**：

ContentLibraryPage 作为统一入口（`/admin/content/library`），同时旧路径通过 redirect 跳转：
```
/admin/script/list      → redirect → /admin/content/library?tab=script
/admin/script/violation  → redirect → /admin/content/library?tab=violation
...
```

但侧栏「内容库·话术与文案」组仍然保留旧菜单项：
```
├── 内容库（统一）    /admin/content/library
├── 话术库           /admin/script/list        ← redirect 回内容库
├── 话术模板         /admin/script/template     ← redirect 回内容库
├── 文案库           /admin/copy               ← redirect 回内容库
├── 话术梗库         /admin/slangdict          ← redirect 回内容库
```

**用户感受**：5 个菜单项，点任何一个都跳到同一个页面的不同 Tab。这非常困惑。

**建议 PM3-A**：**侧栏只保留「内容库」一项**

```
内容库
├── 内容库       /admin/content/library    ← 唯一入口
```

Tab 内的 6 个子页面（话术/模板/文案/违规/检测/梗库）已经足够导航。侧栏不需要重复。

**改动文件**：
- `adminModuleConfig.tsx` — 商品模块侧栏删除重复项

---

### PM4. AI 运维组菜单项过多

**现状**：AI 模块侧栏「AI 运维」组有 8 个子项：

```
AI 运维
├── 模型配置
├── 模型基准
├── 监控中心
├── 调用日志
├── 基础设施
├── Prompt 实验室
├── Prompt 模板
├── 诊断中心
```

**建议 PM4-A**：**合并 + 折叠**

```
AI 运维
├── 模型管理     ← 合并 模型配置 + 模型基准（同页 Tab）
├── 监控与日志   ← 合并 监控中心 + 调用日志 + 基础设施（同页 Tab）
├── Prompt 工具  ← 合并 Prompt 实验室 + Prompt 模板（同页 Tab）
├── 诊断中心     ← 保留
```

8 项 → 4 项。每项内部用 Tab 承载。

**改动文件**：
- 新建 `pages/ai/AiModelManagementPage.tsx` — Tab: 模型配置 + 基准测试
- 新建 `pages/ai/AiOpsMonitorPage.tsx` — Tab: 监控 + 日志 + 基础设施
- 新建 `pages/ai/PromptToolsPage.tsx` — Tab: 实验室 + 模板
- `adminModuleConfig.tsx` — AI 侧栏精简

---

### PM5. 短视频项目工作台需要完整步骤流程

**现状**：`SvProjectWorkbenchPage` 存在但功能和 LiveWorkbenchPage 类似，仅做了薄包装。

**建议 PM5-A**：**与直播工作台统一 Stepper 组件**

```
┌──────────────────────────────────────────────────────────┐
│ ← 返回项目列表                                            │
│ 成分科普系列 #3     状态：拍摄中                           │
│                                                          │
│ ① 策划 ─② 脚本 ─③ 分镜 ─④ 拍摄 ─⑤ 剪辑 ─⑥ 审核 ─⑦ 发布  │
│   ✅       ✅      ✅      🔄      ⬜      ⬜      ⬜       │
├──────────────────────────────────────────────────────────┤
│ [当前步骤内容]                                             │
└──────────────────────────────────────────────────────────┘
```

与直播共用 `WorkflowStepper` 组件，7 步流程，每步加载对应子页面内容。

**改动文件**：
- 新建 `components/base/WorkflowStepper.tsx` — 通用步骤条
- `SvProjectWorkbenchPage.tsx` — 集成 WorkflowStepper + 7 步内容

---

### PM6. 缺少跨模块数据关联

**痛点**：用户看到某个商品效果好，想知道「这个商品在哪些直播场次中被使用？转化率怎样？」没有入口。

**建议 PM6-A**：**商品详情增加「使用记录」Tab**

商品详情抽屉新增第 5 个 Tab「关联」：

| 关联类型 | 展示内容 |
|---------|---------|
| 直播使用 | 哪些场次用过该商品，各场 GMV/转化率 |
| 话术使用 | 各版本话术被使用次数、效果评分趋势 |
| 短视频引用 | 哪些短视频引用了该商品 |
| 爆款关联 | 爆款库中与该商品同品类的视频 |

**改动文件**：
- 新建 `pages/product/tabs/ProductRelationsTab.tsx`
- 后端新增 `POST /product/relations` — 聚合查询商品跨模块关联

---

## 四、实施路线图

### 第 1 批 · 视觉与导航（设计师主导）

| 编号 | 项目 | 改动范围 | 预期效果 |
|------|------|---------|---------|
| D1-A | Dashboard 三区重做 | DashboardPage + 2 新组件 | 删除 23 个快捷按钮，行动导向 |
| D2-A/B | 侧栏折叠精简 | adminModuleConfig + BaseLayout | 短视频 24→8，商品 16→6 |
| D3-A | KpiCard 组件 | 新建 KpiCard | 视觉层级区分 |
| D7-A | 语义色彩 | semanticColors.ts | 颜色传达含义 |
| PM3-A | 内容库去重 | adminModuleConfig | 5 个重复菜单→1 个 |

### 第 2 批 · 核心工作流（产品经理主导）

| 编号 | 项目 | 改动范围 | 预期效果 |
|------|------|---------|---------|
| PM1-A | 商品详情抽屉 | 新建 4 Tab 抽屉 + 删 5 页 | 7 页→2 页 |
| D4-A | 直播工作台完善 | LiveWorkbenchPage 重写 + Stepper | 有进度、有 Header |
| D6-A | 直播卡片列表 | LiveSessionPage 重写 | 按状态分组 |
| PM2-A | 短视频侧栏精简 | adminModuleConfig | 24→6+折叠 |

### 第 3 批 · AI 与体验精化

| 编号 | 项目 | 改动范围 | 预期效果 |
|------|------|---------|---------|
| D5-A | AI 助手升级 | FloatingAiAssistant 重写 | 上下文感知、不跳页 |
| PM4-A | AI 运维合并 | 3 个合并 Tab 页 | 8 项→4 项 |
| PM5-A | 短视频工作台 Stepper | SvProjectWorkbenchPage + WorkflowStepper | 7 步可视化流程 |
| PM6-A | 商品跨模块关联 | 新 Tab + 后端 API | 数据打通 |
| D8-A | 弹窗尺寸规范 | dialogSizes.ts | 统一体验 |

---

## 五、最终数字对比

| 指标 | 第一轮升级后 | 本轮目标 | 变化 |
|------|------------|---------|------|
| Dashboard 快捷按钮 | 23 个 | 0 个（删除） | -100% |
| Dashboard 统计卡 | 12 个 | 4 个 KpiCard | -67% |
| 短视频侧栏项 | 24 个 | 6 + 折叠 | -75% 可见 |
| 商品侧栏项 | 16 个 | 2 + 折叠 | -87% 可见 |
| 商品独立页面 | 7 个 | 2 个（列表+就绪度） | -71% |
| AI 运维项 | 8 个 | 4 个 | -50% |
| 内容库重复项 | 5 个 | 1 个 | -80% |
| 直播工作台深度 | 47 行薄包装 | 完整 Stepper | 质变 |
| AI 助手功能 | 跳转链接 | 上下文+快捷操作+对话 | 质变 |

---

## 附录：关键文件清单

### 需修改

| 文件 | 改动 |
|------|------|
| `src/pages/DashboardPage.tsx` | 三区重做，删除 23 快捷按钮 |
| `src/layouts/adminModuleConfig.tsx` | 侧栏精简（短视频/商品/AI/内容去重） |
| `src/layouts/BaseLayout.tsx` | 支持「更多」折叠渲染 |
| `src/pages/live/LiveWorkbenchPage.tsx` | 重写为 Header + Stepper |
| `src/pages/live/LiveSessionPage.tsx` | 从 DataGrid 改为卡片分组 |
| `src/pages/crud/ProductPage.tsx` | 行点击打开新详情抽屉 |
| `src/components/FloatingAiAssistant.tsx` | 完全重写为上下文感知面板 |
| `src/hooks/usePageContext.ts` | 扩展上下文解析 |
| `src/pages/shortvideo/SvProjectWorkbenchPage.tsx` | 集成 WorkflowStepper |

### 需新建

| 文件 | 说明 |
|------|------|
| `src/components/base/KpiCard.tsx` | Hero 级 KPI 卡片 |
| `src/components/base/HeroSection.tsx` | Dashboard Hero 区 |
| `src/components/base/ActionItemPanel.tsx` | 待办面板（分段控制） |
| `src/components/base/WorkflowStepper.tsx` | 通用步骤条（直播/短视频共用） |
| `src/components/live/LiveSessionCard.tsx` | 直播场次卡片 |
| `src/components/live/LiveWorkbenchHeader.tsx` | 工作台 Header |
| `src/components/AiAssistantPanel.tsx` | AI 助手完整面板 |
| `src/hooks/useAiContextActions.ts` | 上下文→推荐操作映射 |
| `src/theme/semanticColors.ts` | 语义色彩常量 |
| `src/theme/dialogSizes.ts` | 弹窗尺寸常量 |
| `src/pages/product/ProductDetailDrawer.tsx` | 商品多 Tab 详情抽屉 |
| `src/pages/product/tabs/ProductBasicTab.tsx` | 商品基础信息 Tab |
| `src/pages/product/tabs/ProductScriptTab.tsx` | 商品话术 Tab |
| `src/pages/product/tabs/ProductEffectivenessTab.tsx` | 商品效果 Tab |
| `src/pages/product/tabs/ProductHistoryTab.tsx` | 商品历史 Tab |
| `src/pages/product/tabs/ProductRelationsTab.tsx` | 商品关联 Tab |
| `src/pages/ai/AiModelManagementPage.tsx` | AI 模型管理（合并） |
| `src/pages/ai/AiOpsMonitorPage.tsx` | AI 运维监控（合并） |
| `src/pages/ai/PromptToolsPage.tsx` | Prompt 工具（合并） |

### 可删除（合并到抽屉 Tab 后）

| 文件 | 原因 |
|------|------|
| `src/pages/product/EffectivenessScorePage.tsx` | → ProductDetailDrawer 效果 Tab |
| `src/pages/product/ProductScriptVersionPage.tsx` | → ProductDetailDrawer 话术 Tab |
| `src/pages/product/StylePresetPage.tsx` | → ProductDetailDrawer 话术 Tab 内 |
| `src/pages/product/ScriptOptimizationPage.tsx` | → ProductDetailDrawer 话术 Tab 内 |
| `src/pages/crud/SalesHistoryPage.tsx` | → ProductDetailDrawer 历史 Tab |
