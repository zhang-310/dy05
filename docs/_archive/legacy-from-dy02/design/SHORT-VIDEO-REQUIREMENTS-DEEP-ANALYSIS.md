# 短视频文档需求深度分析与迭代计划

**版本**: v1.0  
**日期**: 2026-03-01  
**分析范围**: 四份核心文档 vs 当前实现

---

## 一、文档清单与核心诉求

| 文档 | 核心诉求 | 关键产出 |
|------|----------|----------|
| **SHORT-VIDEO-FRONTEND-PRODUCT-DESIGN** | 3 分钟成片、电影级体验、渐进式引导 | 8 步流程、快速生成、Dashboard、爆款库、数据分析 |
| **SHORT-VIDEO-PRODUCTION-SYSTEM-DESIGN** | 全链路自动化、6 大模块 | 脚本→分镜→素材→剪辑→发布、爆款复刻/日常/软广 |
| **SHORT-VIDEO-CINEMA-GRADE-SUPPLEMENT** | 人物/场景参考、ComfyUI/Kling | 电影级关键帧、图生视频、参考图上传 |
| **DESIGN-ALIGNMENT-ANALYSIS** | 实现与设计对齐 | API 路径、数据库、功能缺口 |

---

## 二、需求分层与缺口矩阵

### 2.1 用户旅程缺口

| 用户故事 | 设计文档 | 当前实现 | 缺口 |
|----------|----------|----------|------|
| 新手 3 步生成 | 选主题→输入内容→选风格→生成 | ✅ 已实现 | 无 |
| 生成进度展示 | 脚本→分镜→素材→合成，WebSocket 实时 | 简化版进度弹窗 | 无 WebSocket，可轮询增强 |
| 专业 8 步流程 | 项目容器 + 左侧流程条 | ProjectFlowSidebar + projectId 联动 | ✅ 已实现 |
| 素材准备 | 人物/场景参考图上传 | MaterialPreparationPage | ✅ 已实现 |
| 爆款复刻 | 爆款库→选视频→一键复刻 | ViralLibraryPage + 爆款复刻卡片跳转 | ✅ 已实现 |
| 数据分析 | 数据概览+趋势+用户画像 | 数据概览 4 卡片+趋势图+热门采集+爆款分析 | 缺用户画像（P2） |
| 首次引导 | 新手引导蒙层 | ShortVideoOnboardingOverlay | ✅ 已实现 |

### 2.2 信息架构缺口

| 设计 | 当前 | 缺口 |
|------|------|------|
| 一级：创作中心、数据分析、爆款库、设置 | 运营→创作中心（含短视频） | ✅ 爆款库已有独立入口 |
| 二级：我的项目、新建项目、素材库、回收站 | 工作台、我的项目、快速生成、… | 素材库无人物/场景/音乐分类 |
| 项目详情→6 子步骤 | projectId + ProjectFlowSidebar | ✅ 已实现 |

### 2.3 页面设计缺口

| 页面 | 设计要点 | 当前 | 缺口 |
|------|----------|------|------|
| Dashboard | 快速创作 3 卡片、数据概览（本周/本月）、我的项目（含播放量） | 有，时间筛选已加，播放量待后端 | 小 |
| 快速生成 | 3 步、进度弹窗 | ✅ | 无 |
| 脚本策划 | 项目关联、AI 生成、手动编写 | 有，ProjectFlowSidebar | ✅ |
| 分镜设计 | 时间轴、分镜列表、AI 生成 | 有，ProjectFlowSidebar | ✅ |
| **素材准备** | 人物参考图、场景参考图 | MaterialPreparationPage | ✅ |
| 素材生产 | 生产概览、按分镜 Tab、单独重新生成 | 有，无生产概览 | 小 |
| 视频剪辑 | 时间轴编辑器 | 有简化版 | P2 |
| 审核发布 | AI 审核、标题/封面候选、成本统计 | 有，缺成本统计 | 小 |
| **爆款库** | 热门视频列表、详情、一键复刻 | ViralLibraryPage | ✅ |
| 数据分析 | 数据概览 4 卡片、趋势图、用户画像 | 数据概览+趋势图+采集+分析 | 缺用户画像（P2） |

### 2.4 电影级方案缺口

| 能力 | 设计 | 当前 | 说明 |
|------|------|------|------|
| 人物/场景参考图 | 上传后用于关键帧生成 | 上传 API 有，生成未接入 | 需 ComfyUI 工作流 |
| ComfyUI | IP-Adapter + ControlNet | 无 | P2，需 GPU |
| Kling 图生视频 | 电影级运镜 | 有 img2video 封装 | 待验证 |

---

## 三、迭代优先级（本次 + 后续）

### 已完成（P0 - 2026-03 首次迭代）

| 序号 | 任务 | 状态 | 交付物 |
|-----|------|------|--------|
| 1 | **专业创作流程整合** | ✅ 完成 | ProjectFlowSidebar、6 步流程条、URL projectId 联动 |
| 2 | **素材准备页** | ✅ 完成 | MaterialPreparationPage、人物/场景参考图上传 |
| 3 | **爆款库页面** | ✅ 完成 | ViralLibraryPage、列表/详情/采集/复刻 |
| 4 | **爆款库 API 对接** | ✅ 完成 | listViralVideos、collectHotVideos、getViralVideo、replicateViral、getRecommendedVirals |

### 已完成（P1 - 2026-03 二次迭代）

| 序号 | 任务 | 状态 | 交付物 |
|-----|------|------|--------|
| 5 | **Dashboard 数据概览时间筛选** | ✅ 完成 | [本周] [本月] Chip 切换，趋势图 days 联动 |
| 6 | **爆款复刻卡片路径修正** | ✅ 完成 | 跳转至 /admin/shortvideo/viral |
| 7 | **继续创作智能跳转** | ✅ 完成 | getContinuePath(stage) 按 stage 跳转对应步骤 |

### 已完成（P1 - 2026-03 三次迭代）

| 序号 | 任务 | 状态 | 交付物 |
|-----|------|------|--------|
| 8 | **数据分析页改造** | ✅ 完成 | 数据概览 Tab（4 卡片 + 本周/本月趋势图）、热门采集、爆款分析 |

### 已完成（P1 - 2026-03 四次迭代）

| 序号 | 任务 | 状态 | 交付物 |
|-----|------|------|--------|
| 9 | **新手引导蒙层** | ✅ 完成 | ShortVideoOnboardingOverlay，首次进入工作台显示「3 步完成你的第一条 AI 视频」 |

### 已完成（P1 - 2026-03 五次迭代）

| 序号 | 任务 | 状态 | 交付物 |
|-----|------|------|--------|
| 10 | **暗色主题** | ✅ 完成 | lightTheme/darkTheme、AppThemeProvider、顶部栏切换按钮、Zustand 持久化 |

### 后续迭代（P2）

| 序号 | 任务 | 说明 |
|-----|------|------|
| 11 | ComfyUI 人物/场景参考 | 需 GPU 部署 |
| 12 | 视频时间轴编辑器 | 多轨道、拖拽 |
| 13 | 成本统计 | 后端+前端 |

---

## 四、技术实现要点

### 4.1 专业创作流程整合

- **方案**：创建 `ProjectFlowLayout` 组件，左侧流程条（6 步），右侧 `Outlet` 渲染子路由
- **路由**：`/admin/shortvideo/project/:id/*` → script | shot-list | prepare | material | edit | publish
- **数据流**：URL 携带 projectId，各子页面从 `useParams` 获取，预加载 project 数据

### 4.2 素材准备页

- **功能**：人物参考图、场景参考图上传（复用 `uploadCharacterReference`、`uploadSceneReference`）
- **路由**：`/admin/shortvideo/prepare` 或纳入 project 流程
- **与素材生产关系**：prepare 完成后，material 页可读取参考图用于生成（后端待接入）

### 4.3 爆款库页面

- **API**：ViralVideoController 路径 `/api/v1/shortvideo/viral`，需前端新增：
  - `listViralVideos(category?, sortBy?, page?, rows?)`
  - `collectViralVideo(vo)`
  - `getViralVideo(id)`
  - `deleteViralVideo(id)`
  - `replicateViral(id)`
  - `getRecommendedVirals(limit?)`
- **页面**：列表、详情、一键复刻按钮

---

## 五、迭代交付物汇总

### 首次迭代（P0）已交付

1. **ProjectFlowSidebar**：左侧 6 步流程条，URL 携带 projectId 跳转
2. **MaterialPreparationPage**：素材准备页（人物/场景参考图上传）
3. **ViralLibraryPage**：爆款库列表 + 详情 + 采集 + 一键复刻
4. **shortvideo API**：viral 相关 6 个接口（list/collect/get/delete/replicate/recommended）
5. **路由与导航**：prepare、viral 路由，AdminLayout 爆款库入口
6. **流程页集成**：ScriptPlanning、ShotListDesign、MaterialPreparation、MaterialProduction、VideoEditing、PublishManagement 均支持 projectId + ProjectFlowSidebar

### 二次迭代（P1）已交付

1. Dashboard 数据概览时间筛选（本周 7 天 / 本月 30 天）
2. 爆款复刻卡片跳转路径修正（/admin/shortvideo/viral）
3. 继续创作智能跳转（按 stage 跳转到 script/shot-list/prepare/material/edit/publish）

### 三次迭代（P1）已交付

1. 数据分析页改造：数据概览 Tab（总视频数、总播放量、总成本、ROI 4 卡片 + 本周/本月趋势图）、热门采集、爆款分析 作为工具 Tab

### 四次迭代（P1）已交付

1. 新手引导蒙层：ShortVideoOnboardingOverlay 组件，首次进入短视频工作台显示 Dialog，文案「3 步完成你的第一条 AI 视频」，[开始创作] 跳转快速生成，[跳过] 关闭并记录 localStorage

### 五次迭代（P1）已交付

1. 暗色主题：`theme/` 模块（lightTheme、darkTheme）、AppThemeProvider 根据 useUIStore.theme 切换，BaseLayout 顶部栏增加亮/暗切换按钮，偏好持久化至 localStorage（ui-storage）

---

**文档版本**: v1.4  
**最后更新**: 2026-03-01
