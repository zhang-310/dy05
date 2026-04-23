# Admin 前端页面全面重构方案

> 产品经理视角 · 多维度深度分析 · 2026-03-22
> 目标：将 120+ 页面的「功能堆砌型」后台，重构为「任务驱动型」高效工作台

---

## 第一部分：现状诊断（10 大核心问题）

### 问题 1：导航信息过载 —— 用户找不到功能

**现状**：AdminLayout 双栏导航，「运营」区下 9 个分组共 50+ 菜单项，「AI」区 17 个平铺菜单项。

```
运营 (50+ items)
├── 核心入口（8）
├── 抖音运营（2）
├── 创作中心·策划与素材（8）
├── 创作中心·成片与发布（6）
├── 创作中心·运营与质量（8）
├── 直播中心（6）
├── 内容库·商品（7）
├── 内容库·话术与文案（5）
└── 内容库·合规与词库（3）
```

**问题**：
- 用户需要展开 9 个折叠组才能扫描所有功能
- 「核心入口」与下方分组存在**路径重复**（如「直播」同时出现在核心入口和直播中心）
- 分组名称抽象（「创作中心·运营与质量」是什么？）
- 新用户看到 50+ 菜单项会产生**认知瘫痪**

**影响**：用户平均需要 3-5 次点击 + 滚动才能到达目标页面。

---

### 问题 2：页面碎片化 —— 同一工作流分散在 N 个页面

**短视频模块**：20 个独立页面，但实际工作流是线性的：

```
策划 → 脚本 → 分镜 → 素材准备 → 素材生产 → 剪辑 → 审核 → 发布 → 数据分析
```

用户要完成一个短视频项目，需要在 8-10 个页面间反复跳转，每次跳转丢失上下文。

**商品模块**：7 个独立页面（商品库 / 就绪度 / 销售历史 / 风格预设 / 效果评分 / 话术版本 / 话术优化），但这些全都是围绕「一个商品」的不同维度，应该在同一个商品详情页内用 Tab 承载。

**AI 模块**：17 个独立页面平铺在一个列表里，没有逻辑分层。运维工具（模型配置/基础设施/调用日志）和业务工具（知识库/创意工坊/行业大脑）混在一起。

---

### 问题 3：Dashboard 缺乏行动力 —— 看了等于没看

**现状**：DashboardPage 仅展示统计数字（用户数/GMV/场次数）+ 20+ 快捷链接按钮。

**问题**：
- 数字没有**对比**（今天 vs 昨天 vs 上周同期）
- 没有**待办事项**（今天有哪些直播？哪些商品话术待生成？）
- 没有**异常告警**（哪个商品话术合规不通过？哪场直播没准备好？）
- 快捷链接 20+ 个铺满屏幕，等于没有快捷

**核心问题**：Dashboard 是「展示型」而非「行动型」。用户看完仍不知道「下一步该做什么」。

---

### 问题 4：直播工作流断裂 —— 最核心的场景体验最差

直播是 GMV 核心场景，但用户操作链路极长：

```
1. 进入「直播」→ 场次列表
2. 点「创建」→ 跳转 LiveSessionFormPage（新页面）
3. 创建成功 → 跳转 LiveSessionDetailPage（又一个新页面）
4. 点「选品」Tab → 需要打开 BatchProductDialog（弹窗）
5. 选品后点「生成话术」→ 跳转 SessionWorkspacePage（第三个新页面）
6. 话术完成 → 回到 DetailPage 检查「就绪度」Tab
7. 开播 → 跳转 LiveRealtimePanel（第四个新页面）
8. 结束 → 跳转 LiveHistoryComparePage 复盘（第五个新页面）
```

**8 步跳 5 个页面**，每次跳转丢失上下文。用户在 DetailPage 和 WorkspacePage 之间反复切换。

---

### 问题 5：组件复用极低 —— 相同功能重复实现

| 重复组件 | 出现次数 | 说明 |
|----------|---------|------|
| ProductEditDrawer | 2 | crud/ProductPage 和 live/ 各一个 |
| 商品选择弹窗 | 3+ | BatchProductDialog、内联选择器、Session 表单 |
| Dialog 状态管理 | 93 处 | 全部用 `useState(false)` 控制开关 |
| 分页请求 | 30+ 处 | 每个页面自己管理 page/rows 状态 |

**ViralLibraryPage 1754 行**，48+ 个 useState，7+ 个弹窗状态 —— 不可维护。

---

### 问题 6：内容类型概念混乱

系统中有三个相似但不同的「内容」概念，用户分不清：

| 概念 | 菜单位置 | 实际含义 |
|------|---------|---------|
| 话术（Script） | 内容库·话术与文案 / 话术库 | 直播用的销售话术 |
| 文案（Copy） | 内容库·话术与文案 / 文案库 | 短视频/图文配文 |
| 脚本（Script Plan） | 创作中心·策划与素材 / 脚本策划 | 短视频拍摄脚本 |

「话术」和「脚本」英文都是 Script，菜单里混在一起。用户搞不清「话术库」和「脚本策划」的区别。

---

### 问题 7：没有「项目」统一视图

用户的真实工作单元是**项目**（一场直播、一个短视频系列、一次带货活动），但系统按**功能模块**组织页面。用户无法：

- 看到「这场直播关联了哪些商品、话术、数据」的全貌
- 看到「这个短视频项目从策划到发布的全流程进度」
- 跨模块追踪同一个目标的所有任务

---

### 问题 8：AI 功能藏太深

AI 是产品核心竞争力，但被放在三级菜单里：

```
点击「AI」主区 → 展开「AI 智能中心」→ 在 17 个选项中找到想要的
```

而且 AI 能力没有**嵌入到业务流程**中。用户需要离开当前工作（如编辑商品话术），跳到 AI 页面生成内容，再手动复制回来。

---

### 问题 9：移动端体验缺失

- 侧边栏在平板上占 264px（双栏），内容区域过窄
- DataGrid 表格无响应式适配
- 弹窗/抽屉在手机上无法全屏
- 直播实时面板（LiveRealtimePanel）本应是移动端核心场景，但未做移动优化

---

### 问题 10：Onboarding 缺失

新用户登录后看到 Dashboard 的 20+ 快捷链接，没有引导。不知道：
- 第一步该做什么（导入商品？配置账号？创建直播？）
- 各模块之间的关系
- AI 功能怎么用

---

## 第二部分：重构方案

### 设计原则

1. **任务驱动，而非功能驱动**：导航按用户工作流组织，不按技术模块
2. **3 次点击到达**：任何功能最多 3 次点击可达
3. **上下文不丢失**：同一工作流内用 Tab/Panel 切换，不跳页面
4. **AI 嵌入而非独立**：AI 能力作为按钮/面板嵌入业务页面
5. **渐进式披露**：新用户看到精简视图，高级功能按需展开

---

### NV-01：重新设计导航架构

**取消双栏导航，改为单栏 + 顶部模块 Tab**

```
┌──────────────────────────────────────────────────────┐
│ 🔷 DY运营    [工作台] [直播] [短视频] [商品] [AI] [⚙]   │  ← 顶部 Tab（5 个核心模块）
├──────────┬───────────────────────────────────────────┤
│ 侧边栏    │                                          │
│ (上下文   │           主内容区                         │
│  子菜单)  │                                          │
│           │                                          │
│ 仅显示    │                                          │
│ 当前模块  │                                          │
│ 的子项    │                                          │
└──────────┴───────────────────────────────────────────┘
```

**顶部 5 个核心模块 Tab**：

| Tab | 侧边栏子项 | 说明 |
|-----|-----------|------|
| **工作台** | 无侧边栏（全屏 Dashboard） | 今日待办 + KPI + 快捷操作 |
| **直播** | 场次管理 / 实时面板 / 历史复盘 / 排行 | 直播全流程 |
| **短视频** | 项目 / 快速生成 / 爆款库 / 素材库 / 日历 / 数据 | 短视频全流程 |
| **商品** | 商品库 / 话术 / 文案 / 违规检测 | 内容+商品合并 |
| **AI** | 知识库 / 智能体 / 创意工坊 / 进化 | AI 业务工具 |
| **⚙ 设置**（齿轮图标） | 用户/角色/配置/日志/监控/模型/API | 系统+AI运维 合并 |

**效果**：
- 50+ 菜单项 → 5 个顶部 Tab + 每个 Tab 下最多 6-8 个侧边栏项
- 用户任何时候只看到**当前模块**的 6-8 个选项，不会信息过载
- 消除重复路径（「直播」只在直播 Tab 下出现一次）

**改动文件**：
- `AdminLayout.tsx` — 重写为顶部 Tab + 单栏侧边栏
- `BaseLayout.tsx` — 支持 topTabs 模式
- `router/index.tsx` — 路由重组

---

### DB-01：重做 Dashboard —— 行动导向的工作台

**取消**：20+ 快捷链接按钮、纯数字展示卡片

**新设计**（3 个区域）：

```
┌────────────────────────────────────────────────────────────┐
│ 区域 1：今日行动 (Action Required)                          │
│ ┌──────────────┬──────────────┬──────────────┐              │
│ │ 🔴 待处理(5)  │ 📋 进行中(3) │ ✅ 已完成(12) │              │
│ └──────────────┴──────────────┴──────────────┘              │
│                                                            │
│ • 直播「春季护肤专场」15:00 开播，3 个商品话术未生成 [去生成→]    │
│ • 短视频「成分科普 #3」素材待审核 [去审核→]                     │
│ • 商品「烟酰胺精华」合规检测不通过 [查看详情→]                   │
│ • 爆款视频「XX」深度分析已完成 [查看结果→]                      │
├────────────────────────────────────────────────────────────┤
│ 区域 2：业务看板 (3 行小卡片)                                 │
│ ┌──────────┬──────────┬──────────┬──────────┐               │
│ │ 今日 GMV  │ 本周直播  │ 内容产出  │ AI 调用   │               │
│ │ ¥128,500  │ 5/8 场   │ 23 条    │ 1,240 次 │               │
│ │ ↑12% 昨日 │ 3场待播   │ ↑8% 上周 │ 配额 68% │               │
│ └──────────┴──────────┴──────────┴──────────┘               │
├────────────────────────────────────────────────────────────┤
│ 区域 3：快速操作 (4 个大按钮)                                 │
│ ┌──────────────┬──────────────┬──────────────┬────────────┐ │
│ │ ＋ 新建直播   │ ＋ 快速视频   │ 📦 导入商品   │ 🤖 AI 助手 │ │
│ │ 场次         │             │             │            │ │
│ └──────────────┴──────────────┴──────────────┴────────────┘ │
└────────────────────────────────────────────────────────────┘
```

**关键改进**：
- 「今日行动」从后端聚合：未完成话术 + 待审核内容 + 合规告警 + 今日直播
- KPI 卡片带**环比对比**（vs 昨日/上周）
- 快速操作只保留 4 个最高频入口
- 底部可选：最近访问的页面（已有 recentVisits store）

**新增后端端点**：
```java
@PostMapping("/dashboard/action-items")
// 返回：待处理事项列表（聚合直播/商品/内容/AI 各模块的待办）
```

**改动文件**：
- `DashboardPage.tsx` — 完全重写
- 新建 `api/dashboard-actions.ts` — 待办聚合 API
- 后端新建 `DashboardActionService.java` — 跨模块聚合待办

---

### LV-01：直播模块重构 —— 一站式工作台

**核心思路**：将 5 个页面合并为 2 个。

**页面 A：直播列表页**（保留，精简）

```
┌──────────────────────────────────────────────┐
│ 直播场次                    [＋ 新建场次]      │
├──────────────────────────────────────────────┤
│ 筛选：[全部] [即将开播] [直播中] [已结束]       │
│                                              │
│ ┌─ 春季护肤专场 ─────────── 今天 15:00 ─────┐ │
│ │ 8 个商品 | 话术 5/8 已生成 | 就绪度 65%    │ │
│ │ [进入工作台→]  [快速提词→]                 │ │
│ └────────────────────────────────────────────┘ │
│ ┌─ 晚间彩妆专场 ─────────── 昨天 20:00 ─────┐ │
│ │ 12 个商品 | GMV ¥52,800 | 转化率 3.2%     │ │
│ │ [查看复盘→]                                │ │
│ └────────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

**改进**：
- 卡片式布局替代 DataGrid（直播场次不多，卡片更直观）
- 每张卡片直接显示**进度摘要**（话术完成度、就绪度）
- 即将开播的场次**置顶 + 高亮**
- 「新建场次」弹出 Dialog 而非跳页面

**页面 B：直播工作台**（合并 DetailPage + WorkspacePage + FormPage）

```
┌──────────────────────────────────────────────────────────┐
│ ← 春季护肤专场    状态：准备中    就绪度：65%  [开播→]     │
├────────┬─────────────────────────────────────────────────┤
│        │                                                 │
│ 步骤   │  当前步骤：选品与话术                              │
│        │                                                 │
│ ① 基础 │  ┌──────────────────┬──────────────────────┐    │
│ ② 选品 │  │ 已选商品 (8)      │ 商品话术              │    │
│ ③ 话术 │  │ ┌──────────────┐ │ ┌──────────────────┐ │    │
│ ④ 合规 │  │ │ 烟酰胺精华   │ │ │ [已生成] 编辑     │ │    │
│ ⑤ 就绪 │  │ │ 玻尿酸面膜   │ │ │ [未生成] AI生成→  │ │    │
│        │  │ │ ...          │ │ │ ...              │ │    │
│ ── ──  │  │ │ [＋ 添加商品] │ │ │                  │ │    │
│ 📊 数据 │  │ └──────────────┘ │ │ [一键全部生成]     │ │    │
│ 💬 AI  │  │                  │ └──────────────────┘ │    │
│ 📋 复盘 │  └──────────────────┴──────────────────────┘    │
│        │                                                 │
│        │  AI 建议面板（右侧抽屉，按需展开）                  │
└────────┴─────────────────────────────────────────────────┘
```

**核心改进**：
- **左侧步骤导航**：① 基础信息 → ② 选品 → ③ 话术生成 → ④ 合规检测 → ⑤ 就绪总览
- **同一页面内完成全流程**：不再跳转 Form/Detail/Workspace 三个页面
- **选品和话术并排展示**：左列选中商品，右列对应话术，一目了然
- **AI 面板为侧边抽屉**：不需要跳到 AI 页面再复制内容
- **数据/复盘 Tab**：直播结束后同页切到数据视图
- **步骤完成状态实时可见**：侧边导航显示每步的完成打勾

**取消的页面**：
- `LiveSessionFormPage.tsx` → 合并到工作台 ① 基础信息 Tab
- `LiveSessionDetailPage.tsx` → 工作台本身
- `SessionWorkspacePage.tsx` → 工作台 ②③ 步骤
- `LiveHistoryComparePage.tsx` → 工作台 📋 复盘 Tab

**保留独立的页面**：
- `LiveRealtimePanel.tsx` — 直播中提词器（全屏独立场景，移动端优先）
- `LiveScriptRankingPage.tsx` — 话术排行（跨场次分析）

**改动文件**：
- 新建 `pages/live/LiveWorkbench.tsx` — 合并工作台（取代 3 个旧页面）
- `LiveSessionPage.tsx` — 改为卡片式布局
- `LiveRealtimePanel.tsx` — 保留，优化移动端
- `router/index.tsx` — 路由简化
- `AdminLayout.tsx` — 直播侧边栏菜单精简

---

### SV-01：短视频模块重构 —— 项目驱动

**核心思路**：20 个页面按使用频率分为 3 层。

**第一层：高频入口**（侧边栏直接可见）

| 菜单项 | 合并后内容 | 原页面 |
|--------|-----------|--------|
| **我的项目** | 项目卡片列表 + 内联进度 | ProjectManagementPage |
| **快速生成** | 一键短视频向导 | QuickGeneratePage |
| **爆款库** | 爆款收藏 + 分析 + 二创 | ViralLibraryPage |
| **素材库** | 素材 + 成片管理 | MaterialLibraryPage |
| **内容日历** | 排期 + 发布 + 日更 | ContentCalendarPage + DailyContentPage + PublishManagementPage |
| **数据分析** | 短视频效果分析 | DataAnalysisPage |

**第二层：项目工作台**（点进项目后的全流程）

```
┌──────────────────────────────────────────────────────────┐
│ ← 项目：「成分科普系列 #3」    进度：拍摄中    [发布→]     │
├────────┬─────────────────────────────────────────────────┤
│        │                                                 │
│ 流程   │  当前：拍摄阶段                                   │
│        │                                                 │
│ ① 策划 │  拍摄任务清单                                     │
│ ② 脚本 │  ┌─────────────────────────────────────────┐    │
│ ③ 分镜 │  │ □ 场景1：开场（外景）    负责人：小王      │    │
│ ④ 拍摄 │  │ ☑ 场景2：产品展示       已完成             │    │
│ ⑤ 剪辑 │  │ □ 场景3：使用演示       待拍摄             │    │
│ ⑥ 审核 │  │ [上传素材] [查看分镜→]                     │    │
│ ⑦ 发布 │  └─────────────────────────────────────────┘    │
│        │                                                 │
│ ── ──  │  AI 分析面板（可折叠）                             │
│ 📊 数据 │  └ 预测播放量：12,000  质量分：85/100              │
└────────┴─────────────────────────────────────────────────┘
```

**同 LiveWorkbench 思路**：项目内全流程一页完成。

**第三层：高级工具**（设置区或侧边栏「更多」折叠）

| 工具 | 原页面 | 处理方式 |
|------|--------|---------|
| 短剧编辑器 | DramaEditorPage | 保留独立页面（复杂编辑器） |
| 工作流编辑器 | WorkflowEditorPage | 保留独立页面（可视化画布） |
| 质量仪表板 | QualityDashboardPage | 合并到数据分析 Tab |
| 视频管理/分类/统计 | DataTablePage ×3 | 合并到素材库 Tab |
| 竞品监控 | CompetitorMonitorPage | 保留（跨模块共享） |
| 效果预测 | ContentEffectPredictPage | 嵌入项目工作台侧边栏 |

**效果**：20 个页面 → 6 个侧边栏入口 + 1 个项目工作台 + 3 个高级工具

**改动文件**：
- 新建 `pages/shortvideo/SvProjectWorkbench.tsx` — 项目全流程工作台
- `ProjectManagementPage.tsx` — 改为卡片式 + 进度摘要
- `ContentCalendarPage.tsx` — 合并日更和发布
- `MaterialLibraryPage.tsx` — 合并视频管理/分类
- `DataAnalysisPage.tsx` — 合并质量仪表板
- 删除 `ShotListDesignPage.tsx`（合并到工作台③分镜步骤）
- 删除 `MaterialPreparationPage.tsx`（合并到工作台④拍摄步骤）
- 删除 `MaterialProductionPage.tsx`（合并到素材库）
- `router/index.tsx` — 路由重组
- `AdminLayout.tsx` — 短视频侧边栏菜单精简

---

### PD-01：商品模块重构 —— 一个商品 = 一个页面

**核心思路**：7 个页面合并为 2 个。

**页面 A：商品库**（列表页，保留 DataGrid）

```
┌──────────────────────────────────────────────────────┐
│ 商品库                     [＋ 新增] [📦 批量导入]     │
├──────────────────────────────────────────────────────┤
│ 筛选：[全部] [就绪✅] [待完善⚠️] [话术未生成🔴]         │
│                                                      │
│ DataGrid：                                           │
│ 图片 | 名称 | 类型 | 价格 | 就绪度 | 话术数 | 效果分    │
│ ...                                                  │
│ 点击行 → 展开商品详情抽屉（右侧 600px）                  │
└──────────────────────────────────────────────────────┘
```

**页面 B：商品详情抽屉**（合并 5 个分散页面）

```
┌──────────────────────────────────────────────────┐
│ 烟酰胺精华液 30ml                 就绪度 ████ 80% │
├──────────────────────────────────────────────────┤
│ [基础] [话术] [效果] [版本] [历史]                  │
│                                                  │
│ 当前 Tab：话术管理                                 │
│ ┌──────────────────────────────────────────────┐ │
│ │ 话术 v3（当前激活）          评分 92  使用 15次  │ │
│ │ 话术 v2                    评分 78  已归档     │ │
│ │                                              │ │
│ │ [🤖 AI 生成新话术]  [风格预设▼]                │ │
│ │ [📊 话术优化建议]                              │ │
│ └──────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
```

**Tab 说明**：

| Tab | 合并原页面 | 内容 |
|-----|-----------|------|
| 基础 | ProductPage 编辑 | 名称/价格/卖点/图片 |
| 话术 | ProductScriptVersionPage + ScriptOptimizationPage | 话术版本列表 + AI 生成 + 优化建议 |
| 效果 | EffectivenessScorePage | 效果评分 + 使用统计 |
| 版本 | ProductScriptVersionPage（版本对比功能） | 话术版本对比/回滚 |
| 历史 | SalesHistoryPage | 销售数据 |

**额外**：
- `StylePresetPage` → 嵌入话术 Tab 的「风格预设」下拉
- `ProductReadinessPage` → 商品库列表页顶部的汇总条
- AI 话术生成按钮直接在话术 Tab 中，不需要跳页面

**改动文件**：
- 新建 `pages/product/ProductDetailDrawer.tsx` — 多 Tab 详情抽屉
- `ProductPage.tsx` — 保留 DataGrid，点击行打开详情抽屉
- 删除 `EffectivenessScorePage.tsx`（合并到详情 Tab）
- 删除 `ProductScriptVersionPage.tsx`（合并到详情 Tab）
- 删除 `StylePresetPage.tsx`（合并到话术 Tab 内）
- `SalesHistoryPage.tsx` → 合并到详情抽屉
- `ProductReadinessPage.tsx` → 合并到列表页汇总条

---

### CT-01：内容库重构 —— 话术/文案/违规 三合一

**核心思路**：消除「话术 vs 文案 vs 脚本」概念混乱。

**统一为「内容库」Tab 页面**：

```
┌──────────────────────────────────────────────────┐
│ 内容库                                            │
├──────────────────────────────────────────────────┤
│ [直播话术] [短视频文案] [模板] [违规词] [合规检测]   │
│                                                  │
│ 当前 Tab：直播话术                                 │
│ 筛选：[全部商品] [风格▼] [状态▼]                    │
│ DataGrid + 右侧预览面板                            │
└──────────────────────────────────────────────────┘
```

**效果**：
- `ScriptPage` + `CopyLibraryPage` + `CopyApprovalPage` + `CopyTemplatePage` + `ViolationWordPage` + `ViolationCheckPage` + `ScriptTemplatePage` + `SlangDictPage` → 1 个内容库 TabPage
- 用 Tab 名称明确区分「直播话术」和「短视频文案」，不再混淆

**改动文件**：
- 新建 `pages/content/ContentLibraryPage.tsx` — 统一 Tab 页面
- 各原页面改为 Tab 子组件
- `router/index.tsx` — 路由合并

---

### AI-01：AI 模块重构 —— 分「业务」和「运维」

**业务工具**（AI 侧边栏可见）：

| 菜单项 | 合并后内容 | 原页面 |
|--------|-----------|--------|
| **知识库** | KB 列表 + 文档管理 + 搜索 | KnowledgeBaseListPage + DocumentsPage + SearchPage |
| **智能体** | Agent 列表 + 对话 | AgentListPage + ChatPage |
| **创意工坊** | 创意生成 + Prompt 实验 | CreativeStudioPage + PromptLabPage |
| **行业大脑** | 行业分析 + 诊断 | IndustryBrainPage + DiagnosisPage |
| **进化中心** | 任务 + 主题 + 爆款 + 知识进化 | 4 个 Evolution 页面合并 |

**运维工具**（⚙ 设置区）：

| 菜单项 | 原页面 |
|--------|--------|
| 模型配置 | ModelsConfigPage |
| 模型基准 | ModelBenchmarkPage |
| AI 监控 | MonitoringPage |
| 调用日志 | CallLogPage |
| 基础设施 | AdminInfraPage |
| Prompt 模板 | PromptTemplatePage |
| 数字人 | DigitalHumanPage |

**效果**：17 个平铺菜单 → 5 个业务入口 + 7 个运维入口（在设置区）

**改动文件**：
- `AdminLayout.tsx` — AI 侧边栏精简
- 新建 `pages/ai/EvolutionHub.tsx` — 合并 4 个进化页面
- AI 运维页面移到 ⚙ 设置区路由

---

### UX-01：全局 AI 助手升级

**现状**：`FloatingAiAssistant` 悬浮按钮已存在，但功能有限。

**升级方案**：

```
┌────────────────────────────────────────┐
│ 🤖 AI 助手                    [最小化] │
├────────────────────────────────────────┤
│ 我在商品「烟酰胺精华」的详情页           │
│ 检测到这个商品还没有生成话术             │
│                                        │
│ 快捷操作：                              │
│ [生成卖点话术]  [合规检查]  [竞品分析]    │
│                                        │
│ 或输入自然语言指令：                      │
│ ┌────────────────────────────────────┐ │
│ │ 帮我为这个商品生成 3 版不同风格话术 │ │
│ └────────────────────────────────────┘ │
└────────────────────────────────────────┘
```

**关键能力**：
- **上下文感知**：知道用户当前在哪个页面、操作什么数据
- **快捷操作**：根据当前上下文推荐 2-3 个最相关的 AI 操作
- **结果直达**：生成结果直接填入当前表单/抽屉，无需手动复制
- **全局可唤起**：`Ctrl+K` 或悬浮按钮

**改动文件**：
- 升级 `components/FloatingAiAssistant.tsx` — 上下文感知 + 快捷操作
- 新建 `hooks/usePageContext.ts` — 获取当前页面上下文
- 新建 `api/ai-assistant.ts` — AI 助手 API

---

### UX-02：通用 UX 提升

#### A. 统一组件库

| 组件 | 替代内容 | 收益 |
|------|---------|------|
| `<ProductPicker>` | 替代 3 处重复的商品选择弹窗 | 减少 300+ 行重复代码 |
| `<EntityDetailDrawer>` | 统一抽屉模式（Tab + Header + Actions） | 标准化详情交互 |
| `<StepWorkbench>` | 统一步骤式工作台（直播/短视频共用） | 核心交互模式统一 |
| `<KpiCard>` | 替代 StatCard，支持环比/趋势 | Dashboard 一致性 |
| `<ActionItemList>` | 待办事项列表组件 | Dashboard + 各模块共用 |

#### B. 状态管理优化

- 新建 `stores/modal.ts` — 统一管理所有弹窗/抽屉状态
- 新建 `stores/pagination.ts` — 分页缓存（切换 Tab 不丢失分页位置）
- 新建 `stores/formDraft.ts` — 表单草稿（离开页面时保存未提交的表单）

#### C. 移动端适配

| 场景 | 方案 |
|------|------|
| 导航 | 底部 Tab Bar（5 个核心模块） |
| 表格 | 列表卡片视图（替代 DataGrid） |
| 弹窗 | 全屏 Sheet |
| 直播提词 | 全屏竖屏模式 + 大字体 |

---

### OB-01：新用户引导流程

```
Step 1: 欢迎页 → 选择角色（管理员/机构/达人）
Step 2: 核心设置 → 导入商品（或跳过）
Step 3: 首次体验 → 引导创建第一场直播或第一个短视频
Step 4: 功能发现 → 侧边栏菜单高亮提示（Tooltip Tour）
```

**改动文件**：
- 升级 `components/onboarding/GettingStartedChecklist.tsx` — 改为 Step 向导
- 新建 `pages/OnboardingPage.tsx` — 首次登录引导页

---

## 第三部分：实施路线图

### 第 1 批（高优先级 · 用户可感知的体验飞跃）

| 编号 | 项目 | 改动范围 | 依赖 |
|------|------|---------|------|
| **NV-01** | 导航重构 | AdminLayout + BaseLayout + router | 无 |
| **DB-01** | Dashboard 重做 | DashboardPage + 后端聚合 API | NV-01 |
| **PD-01** | 商品一页化 | ProductDetailDrawer + 删除 5 页 | NV-01 |

**预期效果**：菜单项从 50+ 降到 25 以内，商品操作从 7 页降到 2 页。

### 第 2 批（核心场景 · GMV 直接相关）

| 编号 | 项目 | 改动范围 | 依赖 |
|------|------|---------|------|
| **LV-01** | 直播工作台 | LiveWorkbench（合并 3 页） | NV-01 |
| **CT-01** | 内容库统一 | ContentLibraryPage（合并 8 页） | NV-01 |
| **UX-02A** | 通用组件 | ProductPicker + EntityDetailDrawer + StepWorkbench | 无 |

**预期效果**：直播准备从 8 步 5 页降到一站式工作台，内容概念不再混淆。

### 第 3 批（短视频 + AI · 完整体验）

| 编号 | 项目 | 改动范围 | 依赖 |
|------|------|---------|------|
| **SV-01** | 短视频项目化 | SvProjectWorkbench + 删除 8 页 | NV-01 + UX-02A |
| **AI-01** | AI 分层 | AI 侧边栏 + EvolutionHub | NV-01 |
| **UX-01** | AI 助手升级 | FloatingAiAssistant + usePageContext | 无 |

**预期效果**：短视频从 20 页降到 6+1 工作台，AI 从 17 个平铺降到 5+7 分层。

### 第 4 批（体验精细化）

| 编号 | 项目 | 改动范围 | 依赖 |
|------|------|---------|------|
| **UX-02B** | 状态管理优化 | 3 个新 store | 无 |
| **UX-02C** | 移动端适配 | 各页面响应式改造 | 全部 |
| **OB-01** | 新用户引导 | OnboardingPage + Tour 组件 | NV-01 |

---

## 第四部分：页面精简对照表

### 合并/删除的页面（-35 个页面）

| 原页面 | 处理方式 | 合并到 |
|--------|---------|--------|
| `LiveSessionFormPage` | 合并 | LiveWorkbench ① 基础 Tab |
| `LiveSessionDetailPage` | 合并 | LiveWorkbench（主体） |
| `SessionWorkspacePage` | 合并 | LiveWorkbench ②③ 步骤 |
| `LiveHistoryComparePage` | 合并 | LiveWorkbench 📋 复盘 Tab |
| `LiveProductPage` | 合并 | LiveWorkbench ② 选品 + 商品库 |
| `LiveRhythmPage` | 合并 | LiveWorkbench AI 面板 |
| `LiveScriptVersionPage` | 合并 | 商品详情抽屉「版本」Tab |
| `EffectivenessScorePage` | 合并 | 商品详情抽屉「效果」Tab |
| `ProductScriptVersionPage` | 合并 | 商品详情抽屉「话术」Tab |
| `StylePresetPage` | 合并 | 商品详情「话术」Tab 下拉 |
| `ProductReadinessPage` | 合并 | 商品库列表页汇总条 |
| `SalesHistoryPage` | 合并 | 商品详情抽屉「历史」Tab |
| `ScriptOptimizationPage` | 合并 | 商品详情「话术」Tab |
| `AdminScriptPage` | 合并 | ContentLibraryPage「直播话术」Tab |
| `AdminScriptTemplatePage` | 合并 | ContentLibraryPage「模板」Tab |
| `CopyLibraryPage` | 合并 | ContentLibraryPage「短视频文案」Tab |
| `CopyApprovalPage` | 合并 | ContentLibraryPage「文案」Tab 行操作 |
| `CopyTemplatePage` | 合并 | ContentLibraryPage「模板」Tab |
| `AdminViolationWordPage` | 合并 | ContentLibraryPage「违规词」Tab |
| `ViolationCheckPage` | 合并 | ContentLibraryPage「合规检测」Tab |
| `SlangDictPage` | 合并 | ContentLibraryPage（话术梗子 Tab） |
| `ScriptPlanningPage` | 合并 | SvProjectWorkbench ② 脚本 |
| `ShotListDesignPage` | 合并 | SvProjectWorkbench ③ 分镜 |
| `MaterialPreparationPage` | 合并 | SvProjectWorkbench ④ 拍摄 |
| `MaterialProductionPage` | 合并 | 素材库 |
| `PublishManagementPage` | 合并 | ContentCalendarPage「发布」Tab |
| `DailyContentPage` | 合并 | ContentCalendarPage「日更」Tab |
| `QualityDashboardPage` | 合并 | DataAnalysisPage「质量」Tab |
| `ShortVideoDashboardPage` | 合并 | Dashboard 短视频卡片 + 项目列表 |
| `EvolutionTasksPage` | 合并 | EvolutionHub Tab |
| `EvolutionTopicPage` | 合并 | EvolutionHub Tab |
| `ViralAnalysisPage` | 合并 | EvolutionHub Tab |
| `KnowledgeEvolutionPage` | 合并 | EvolutionHub Tab |
| `IndustryBrainDiagnosisPage` | 合并 | IndustryBrainPage Tab |
| `CopyPage` | 删除 | 被 ContentLibraryPage 替代 |

### 保留的独立页面（~45 个页面）

| 模块 | 保留页面 | 原因 |
|------|---------|------|
| 核心 | LoginPage, DashboardPage, NotFoundPage | 必需 |
| 直播 | LiveSessionPage, LiveWorkbench, LiveRealtimePanel, LiveScriptRankingPage | 独立场景 |
| 短视频 | ProjectManagementPage, SvProjectWorkbench, QuickGeneratePage, ViralLibraryPage, MaterialLibraryPage, ContentCalendarPage, DataAnalysisPage, DramaEditorPage, WorkflowEditorPage, CompetitorMonitorPage, ContentEffectPredictPage, ShootingTaskPage | 独立场景 |
| 商品 | ProductPage | 列表入口 |
| 内容 | ContentLibraryPage | 合并后的统一入口 |
| AI | KnowledgeBaseListPage, KnowledgeDocumentsPage, KnowledgeSearchPage, AgentListPage, AgentChatPage, CreativeStudioPage, PromptLabPage, IndustryBrainPage, EvolutionHub, AiDashboardPage, DigitalHumanPage | 独立功能 |
| 系统 | UsersPage, RolesPage, ResourcesPage, LoginLogsPage, OperationLogPage, SystemLogPage, ConfigPage, StoragePage, SystemStatusPage, ApiLogPage, SyncLogPage, ExternalApiConfigPage, ExternalApiHealthPage, ComplianceCheckPage | 管理必需 |
| 其他 | DouyinAccountPage, DouyinAccountDetailPage, DouyinPersonaPage, DataTablePage | 基础管理 |

### 最终数字

| 指标 | 重构前 | 重构后 | 减少 |
|------|--------|--------|------|
| 总页面数 | 120+ | ~45 独立 + 组件化 | -63% |
| 导航菜单项 | 50+ | 25 以内 | -50% |
| 直播完整操作步骤 | 8 步跳 5 页 | 一站式工作台 | -80% |
| 商品管理页面 | 7 个 | 1 列表 + 1 抽屉 | -71% |
| AI 菜单层级 | 17 平铺 | 5 业务 + 7 运维 | 分层清晰 |

---

## 第五部分：新增后端支持

### 1. Dashboard 待办聚合 API

```java
@PostMapping("/dashboard/action-items")
// 返回聚合待办：
// - 今日直播 + 话术完成度
// - 待审核内容
// - 合规告警
// - AI 分析完成通知
// - 商品就绪度告警
```

### 2. 商品详情聚合 API

```java
@PostMapping("/product/detail-aggregate")
// 聚合返回：
// - 商品基础信息
// - 话术列表 + 版本
// - 效果评分
// - 销售历史摘要
// - 就绪度评估
```

### 3. 直播工作台聚合 API

```java
@PostMapping("/live/session/workbench")
// 聚合返回：
// - 场次基础信息
// - 已选商品列表 + 话术状态
// - 合规检测结果
// - 就绪度评估
// - AI 建议
```

---

## 附录：导航架构对比图

### 重构前

```
AdminLayout (双栏 264px)
├── 运营 [主区]
│   ├── 核心入口（8 项）
│   ├── 抖音运营（2 项）
│   ├── 创作中心·策划与素材（8 项）
│   ├── 创作中心·成片与发布（6 项）
│   ├── 创作中心·运营与质量（8 项）
│   ├── 直播中心（6 项）
│   ├── 内容库·商品（7 项）
│   ├── 内容库·话术与文案（5 项）
│   └── 内容库·合规与词库（3 项）
├── 系统 [主区]
│   ├── 系统管理（4 项）
│   ├── 日志管理（2 项）
│   ├── 配置管理
│   ├── 存储管理
│   ├── 系统监控（5 项）
│   ├── TianAPI
│   └── 合规检测
└── AI [主区]
    ├── A/B 测试
    ├── AI 智能体
    ├── AI 智能中心（17 项!!）
    └── 企业微信（3 项）
```

### 重构后

```
顶部 Tab (5 个)
├── [工作台]  → 全屏 Dashboard（无侧边栏）
├── [直播]    → 侧边栏 3 项：场次管理 / 提词面板 / 排行
├── [短视频]  → 侧边栏 6 项：项目 / 快速生成 / 爆款库 / 素材库 / 日历 / 数据
├── [商品]    → 侧边栏 4 项：商品库 / 内容库 / 违规检测 / 抖音账号
├── [AI]      → 侧边栏 5 项：知识库 / 智能体 / 创意工坊 / 行业大脑 / 进化中心
└── [⚙ 设置] → 侧边栏分组：
    ├── 用户权限（用户/角色/资源）
    ├── 系统（配置/存储/日志）
    ├── 监控（系统/API/外部API）
    └── AI 运维（模型/基准/监控/日志/Prompt/基础设施）
```
