# 导航信息架构原型 — 左侧导航规划

> 文档版本：v1.0 | 2026-03-30
> 对齐：AdminLayout.tsx NAV_GROUPS 重构方案 + 15个产品原型模块

---

## 一、现状问题诊断

### 当前导航缺陷

| 问题 | 现象 | 影响 |
|------|------|------|
| AI中心过载 | 22个子项塞入一组 | 用户无法快速找到功能，认知负荷极高 |
| 业务逻辑割裂 | 内容/直播/商品互相独立，无价值链引导 | 新用户不知道操作顺序 |
| 功能重复 | A/B实验同时在「AI中心」和「运营」 | 入口混乱 |
| 内容组混杂 | 话术脚本+文案库+内容库11项混入同组 | 文案创作与脚本管理职责不清 |
| 运营组杂乱 | Agent/企微/归因/系统工具混为一谈 | 协作工具与运维工具混在一起 |
| 统一KPI孤立 | 独立于首页存在，权重与Dashboard同级 | 用户不清楚什么时候用KPI页 |

---

## 二、设计原则

1. **价值链优先**：导航顺序 = 业务操作顺序（账号→商品→知识→话术→直播→分析→进化）
2. **单组 ≤ 8 项**：超过8项必须拆组
3. **角色感知**：Admin/Org/Talent三角色看到不同导航
4. **高频在前**：每组内按日常使用频率降序排列
5. **AI工具收敛**：AI能力嵌入业务流程，而非单独一个大组

---

## 三、产品原型（ASCII 线框图）

### 3.1 整体导航结构（双栏：图标轨道 + 子菜单面板）

```
┌────────────────────────────────────────────────────────────────────┐
│ 导航结构：72px 图标轨道 + 156px 子菜单面板（悬停展开）              │
├──────┬─────────────────────────────────────────────────────────────┤
│ 轨道 │ 子菜单（选中组展开）                                         │
├──────┼─────────────────────────────────────────────────────────────┤
│ 🏠   │ ─── 首页 ──────────────────────────────────────             │
│      │  运营指挥中心  /admin/dashboard                              │
│      │  统一KPI视图   /admin/kpi                                    │
├──────┼─────────────────────────────────────────────────────────────┤
│ 📺   │ ─── 直播 ──────────────────────────────────────             │
│      │  场次管理      /admin/live/sessions         ← 高频入口       │
│      │  话术排行榜    /admin/live/ranking                           │
│      │  节奏配置      /admin/live/rhythm                            │
│      │  版本对比      /admin/live/history-compare                   │
│      │  ─────────────────────────────                              │
│      │  场次商品      /admin/live/products                          │
│      │  [新建场次]    /admin/live/sessions/create  ← 快捷按钮       │
├──────┼─────────────────────────────────────────────────────────────┤
│ 🛍   │ ─── 商品 ──────────────────────────────────────             │
│      │  商品库        /admin/product/list          ← 高频入口       │
│      │  话术效果      /admin/product/effectiveness                  │
│      │  风格预设      /admin/product/style-presets                  │
│      │  销售记录      /admin/product/sales-history                  │
│      │  上播准备度    /admin/product/readiness                      │
├──────┼─────────────────────────────────────────────────────────────┤
│ ✍   │ ─── 内容创作 ───────────────────────────────────            │
│      │  话术脚本库    /admin/script/list           ← 高频入口       │
│      │  话术模板      /admin/script/templates                       │
│      │  混合搜索      /admin/script/hybrid-search                   │
│      │  脚本生成      /admin/script/generation                      │
│      │  ─────────────────────────────                              │
│      │  文案库        /admin/copy/library                           │
│      │  文案模板      /admin/copy/templates                         │
│      │  文案审批      /admin/copy/approval                          │
├──────┼─────────────────────────────────────────────────────────────┤
│ 🎬   │ ─── 短视频 ─────────────────────────────────────           │
│      │  项目列表      /admin/shortvideo/projects   ← 高频入口       │
│      │  热点选题      /admin/shortvideo/hot-topics                  │
│      │  爆款分析      /admin/shortvideo/viral-analysis              │
│      │  爆款翻拍      /admin/shortvideo/viral-remake                │
│      │  素材中心      /admin/shortvideo/material                    │
│      │  内容日历      /admin/shortvideo/publish                     │
├──────┼─────────────────────────────────────────────────────────────┤
│ 🧠   │ ─── 知识大脑 ───────────────────────────────────           │
│      │  知识库管理    /admin/ai/knowledge          ← 高频入口       │
│      │  知识检索      /admin/ai/knowledge-search                    │
│      │  自进化引擎    /admin/ai/evolution                           │
│      │  进化审核      /admin/ai/evolution-review                    │
│      │  进化主题池    /admin/ai/evolution-topics                    │
│      │  行业大脑      /admin/ai/industry-brain                      │
├──────┼─────────────────────────────────────────────────────────────┤
│ ✨   │ ─── AI 工具 ────────────────────────────────────           │
│      │  Prompt模板    /admin/ai/prompt-templates   ← 高频入口       │
│      │  Prompt实验室  /admin/ai/prompt-lab                          │
│      │  创意工坊      /admin/ai/creative-studio                     │
│      │  数字人        /admin/ai/digital-human                       │
│      │  模型评测      /admin/ai/model-benchmark                     │
│      │  AI调用日志    /admin/ai/call-log                            │
├──────┼─────────────────────────────────────────────────────────────┤
│ 🤖   │ ─── 智能体 & 实验 ────────────────────────────────────      │
│      │  智能体管理    /admin/agent/list            ← 高频入口       │
│      │  A/B实验       /admin/abtest                                  │
├──────┼─────────────────────────────────────────────────────────────┤
│ 📊   │ ─── 数据分析 ───────────────────────────────────           │
│      │  归因分析      /admin/attribution           ← 高频入口       │
│      │  效果排行      /admin/live/ranking                           │
├──────┼─────────────────────────────────────────────────────────────┤
│ 💬   │ ─── 协作通知 ───────────────────────────────────           │
│      │  企微机器人    /admin/wecom/robots          ← 高频入口       │
│      │  推送规则      /admin/wecom/rules                            │
│      │  推送日志      /admin/wecom/logs                             │
├──────┼─────────────────────────────────────────────────────────────┤
│ ⚙    │ ─── 系统运维 ───────────────────────────────────           │
│      │  系统监控      /admin/system/overview       ← 高频入口       │
│      │  告警规则      /admin/system/alert-rules                     │
│      │  API日志       /admin/system/api-log                         │
│      │  外部API       /admin/system/external-api                    │
│      │  合规检测      /admin/system/compliance                      │
│      │  用户管理      /admin/auth/users                             │
│      │  角色权限      /admin/auth/roles                             │
│      │  资源权限      /admin/auth/resources                         │
│      │  审计日志      /admin/log/audit                              │
├──────┼─────────────────────────────────────────────────────────────┤
│ 💳   │ ─── 订阅支付 ───────────────────────────────────           │
│      │  订阅管理      /admin/payment/subscription  ← 高频入口       │
│      │  配额使用      /admin/payment/usage                          │
│      │  订单历史      /admin/payment/orders                         │
└──────┴─────────────────────────────────────────────────────────────┘
```

---

## 四、导航组对比（旧 vs 新）

```
旧结构（10组，部分严重过载）          新结构（11组，单组 ≤ 8 项）
─────────────────────────────────    ─────────────────────────────────
首页 (1项)                     →     首页 (2项：运营+KPI)
统一KPI (1项，冗余)             →     合并进首页
抖音 (3项)                     →     ✂ 移入系统运维（账号绑定低频）
内容 (11项，混乱!)              →     内容创作 (8项) + 短视频 (6项) 分拆
直播 (7项)                     →     直播 (7项，结构优化)
商品 (5项)                     →     商品 (5项，保持)
AI中心 (22项，灾难!)            →     知识大脑(6项) + AI工具(6项) + 智能体&实验(2项)
运营 (4项，混杂)                →     数据分析(2项) + 协作通知(3项)
系统 (14项，混乱!)              →     系统运维(9项，含原auth/log散落项)
支付 (3项)                     →     订阅支付(3项，改名更直观)
```

---

## 五、业务价值链导航感知

```
运营人员每日操作路径 → 导航引导

早上:
  首页 → 查看昨日GMV + 待处理事项
  直播 → 查看今日场次计划
  商品 → 检查上播准备度

直播前:
  内容创作 → 搜索/编辑话术脚本
  知识大脑 → 检索成分知识
  AI工具   → 生成/优化话术

直播中:
  直播 → 进入场次工作台（实时面板）
  协作通知 → 企微推送关键节点

直播后:
  数据分析 → 归因分析复盘
  内容创作 → 优质话术沉淀到文案库
  知识大脑 → 进化审核新产出知识
```

---

## 六、重构后 NAV_GROUPS 定义（TypeScript 伪代码）

```typescript
const NAV_GROUPS: NavGroup[] = [
  // 1. 首页
  { key: 'home', label: '首页', icon: <DashboardIcon />, children: [
    { label: '运营指挥中心', path: '/admin/dashboard' },
    { label: '统一KPI视图',  path: '/admin/kpi' },
  ]},

  // 2. 直播（核心业务）
  { key: 'live', label: '直播', icon: <LiveTvIcon />, children: [
    { label: '场次管理',   path: '/admin/live/sessions' },      // P0
    { label: '场次商品',   path: '/admin/live/products' },
    { label: '话术排行榜', path: '/admin/live/ranking' },
    { label: '节奏配置',   path: '/admin/live/rhythm' },
    { label: '版本对比',   path: '/admin/live/history-compare' },
    // [+新建场次] 作为 FAB 按钮，不占导航位
  ]},

  // 3. 商品
  { key: 'product', label: '商品', icon: <ShoppingBagIcon />, children: [
    { label: '商品库',   path: '/admin/product/list' },         // P0
    { label: '上播准备', path: '/admin/product/readiness' },
    { label: '话术效果', path: '/admin/product/effectiveness' },
    { label: '风格预设', path: '/admin/product/style-presets' },
    { label: '销售记录', path: '/admin/product/sales-history' },
  ]},

  // 4. 内容创作
  { key: 'content', label: '内容', icon: <ArticleIcon />, children: [
    { label: '话术脚本', path: '/admin/script/list' },          // P0
    { label: '话术模板', path: '/admin/script/templates' },
    { label: '混合搜索', path: '/admin/script/hybrid-search' },
    { label: '脚本生成', path: '/admin/script/generation' },
    { label: '文案库',   path: '/admin/copy/library' },
    { label: '文案审批', path: '/admin/copy/approval' },
    { label: '文案模板', path: '/admin/copy/templates' },
  ]},

  // 5. 短视频
  { key: 'shortvideo', label: '短视频', icon: <VideoLibraryIcon />, children: [
    { label: '项目列表', path: '/admin/shortvideo/projects' },  // P0
    { label: '热点选题', path: '/admin/shortvideo/hot-topics' },
    { label: '爆款分析', path: '/admin/shortvideo/viral-analysis' },
    { label: '爆款翻拍', path: '/admin/shortvideo/viral-remake' },
    { label: '素材中心', path: '/admin/shortvideo/material' },
    { label: '内容日历', path: '/admin/shortvideo/publish' },
  ]},

  // 6. 知识大脑
  { key: 'knowledge', label: '知识大脑', icon: <PsychologyIcon />, children: [
    { label: '知识库管理', path: '/admin/ai/knowledge' },       // P0
    { label: '知识检索',   path: '/admin/ai/knowledge-search' },
    { label: '自进化引擎', path: '/admin/ai/evolution' },
    { label: '进化审核',   path: '/admin/ai/evolution-review' },
    { label: '进化主题池', path: '/admin/ai/evolution-topics' },
    { label: '行业大脑',   path: '/admin/ai/industry-brain' },
  ]},

  // 7. AI工具
  { key: 'aitools', label: 'AI工具', icon: <SmartToyIcon />, children: [
    { label: 'Prompt模板', path: '/admin/ai/prompt-templates' }, // P0
    { label: 'Prompt实验室', path: '/admin/ai/prompt-lab' },
    { label: '创意工坊',   path: '/admin/ai/creative-studio' },
    { label: '数字人',     path: '/admin/ai/digital-human' },
    { label: '模型评测',   path: '/admin/ai/model-benchmark' },
    { label: 'AI调用日志', path: '/admin/ai/call-log' },
  ]},

  // 8. 智能体 & 实验
  { key: 'experiment', label: '实验', icon: <ScienceIcon />, children: [
    { label: '智能体',  path: '/admin/agent/list' },
    { label: 'A/B实验', path: '/admin/abtest' },
  ]},

  // 9. 数据分析
  { key: 'analytics', label: '分析', icon: <TrackChangesIcon />, children: [
    { label: '归因分析', path: '/admin/attribution' },
    { label: '效果排行', path: '/admin/live/ranking' },    // 与直播组共享页面
  ]},

  // 10. 协作通知
  { key: 'collab', label: '协作', icon: <BusinessIcon />, children: [
    { label: '企微推送', path: '/admin/wecom/robots' },
    { label: '推送规则', path: '/admin/wecom/rules' },
    { label: '推送日志', path: '/admin/wecom/logs' },
  ]},

  // 11. 系统运维
  { key: 'system', label: '系统', icon: <SettingsIcon />, children: [
    { label: '系统监控', path: '/admin/system/overview' },
    { label: '用户管理', path: '/admin/auth/users' },
    { label: '角色权限', path: '/admin/auth/roles' },
    { label: '审计日志', path: '/admin/log/audit' },
    { label: '告警规则', path: '/admin/system/alert-rules' },
    { label: 'API日志',  path: '/admin/system/api-log' },
    { label: '外部API',  path: '/admin/system/external-api' },
  ]},

  // 12. 订阅支付
  { key: 'payment', label: '订阅', icon: <PaymentIcon />, children: [
    { label: '订阅管理', path: '/admin/payment/subscription' },
    { label: '配额使用', path: '/admin/payment/usage' },
    { label: '订单历史', path: '/admin/payment/orders' },
  ]},
]
```

---

## 七、角色导航差异规范

### 7.1 三角色可见组对照

| 导航组 | Admin | Org管理员 | Talent主播 |
|--------|-------|-----------|------------|
| 首页 | ✅ 全部指标 | ✅ 机构指标 | ✅ 个人指标 |
| 直播 | ✅ 全部场次 | ✅ 机构场次 | ✅ 自己场次 |
| 商品 | ✅ | ✅ | ✅ 只读 |
| 内容创作 | ✅ | ✅ | ✅ 只读+生成 |
| 短视频 | ✅ | ✅ | ✅ 自己项目 |
| 知识大脑 | ✅ | ✅ 只读 | ✅ 只检索 |
| AI工具 | ✅ | ✅ | ✅ 创意工坊 |
| 实验 | ✅ | ✅ 只查看 | ❌ 隐藏 |
| 数据分析 | ✅ | ✅ 机构数据 | ✅ 个人数据 |
| 协作通知 | ✅ | ✅ | ✅ 只查看 |
| 系统运维 | ✅ | ❌ 隐藏 | ❌ 隐藏 |
| 订阅支付 | ✅ | ✅ 只查看 | ❌ 隐藏 |

### 7.2 Org布局（OrgLayout.tsx）菜单

```
首页 → 直播（机构场次）→ 商品（只读）→ 内容创作 → 短视频 → 知识大脑（只读）
→ AI工具 → 数据分析（机构） → 协作通知 → 订阅（只查看）
```

### 7.3 Talent布局（TalentLayout.tsx）菜单

```
首页 → 直播（自己场次）→ 商品（只读）→ 内容创作（生成+只读）
→ 短视频（自己项目）→ 数据分析（个人）→ 协作通知
```

---

## 八、交互规范

### 8.1 双栏布局行为

```
┌────────────────────────────────────────────────────────────────────┐
│ 状态1：收缩态（默认）                                               │
│  图标轨 72px  | 主内容区占满剩余宽度                                │
│  悬停图标 → 显示 Tooltip 标签名                                    │
│                                                                      │
│ 状态2：展开态（点击图标组）                                         │
│  图标轨 72px  | 子菜单面板 156px  | 主内容区                        │
│  子菜单背景：#1e2a3a（深色）/ white（浅色主题）                     │
│  当前激活项：左侧 3px 主色边框 + 背景高亮                           │
│                                                                      │
│ 状态3：移动端（<768px）                                             │
│  Drawer 覆盖式，汉堡菜单触发                                        │
└────────────────────────────────────────────────────────────────────┘
```

### 8.2 快捷操作（FAB）

直播工作台入口过深，在右下角放置 FAB：

```
┌────────────────────────────────────────────────────────────┐
│                                              [📺 开启直播] │  ← FAB 主按钮（主色）
│                                              [+ 新建场次 ] │  ← 展开子选项
│                                              [✨ AI生成   ] │
└────────────────────────────────────────────────────────────┘
```

---

## 九、导航指标（验收标准）

| 指标 | 目标 | 测量方式 |
|------|------|----------|
| 单组子项数 | ≤ 8 项 | 人工检查 |
| 核心功能点击深度 | ≤ 2 次点击 | 路径测试 |
| 新用户找到「新建场次」| ≤ 30 秒 | 可用性测试 |
| 导航组总数 | 10-12 组 | 人工检查 |
| AI中心子项数 | 拆分为 2 组，各 ≤ 6 项 | 人工检查 |

---

## 十、与原型文档对应关系

| 导航组 | 对应原型文档 |
|--------|-------------|
| 首页 | [01-dashboard.md](01-dashboard.md) |
| 直播 | [02-live-workbench.md](02-live-workbench.md) |
| 商品 | [03-products.md](03-products.md) |
| 内容创作 | [06-copy.md](06-copy.md) + [15-script.md](15-script.md) |
| 短视频 | [05-shortvideo.md](05-shortvideo.md) |
| 知识大脑 | [04-knowledge-ai.md](04-knowledge-ai.md) + [08-evolution.md](08-evolution.md) |
| AI工具 | [04-knowledge-ai.md](04-knowledge-ai.md) §3.6 Prompt模板 |
| 实验 | [09-agent.md](09-agent.md) + [10-abtest.md](10-abtest.md) |
| 数据分析 | [07-attribution.md](07-attribution.md) |
| 协作通知 | [14-wecom.md](14-wecom.md) |
| 系统运维 | [13-system.md](13-system.md) |
| 订阅支付 | [12-payment.md](12-payment.md) |