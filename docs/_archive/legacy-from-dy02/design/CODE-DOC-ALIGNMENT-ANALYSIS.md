# 短视频模块 - 代码与文档深度对齐分析

**版本**: v1.0  
**日期**: 2026-03-01  
**分析范围**: 前后端代码 vs `docs/design/`、`docs/01-产品需求总览.md`

---

## 一、分析结论总览

| 维度 | 完成度 | 说明 |
|------|--------|------|
| **后端 API（short-video）** | 95% | 项目/脚本/分镜/素材/剪辑/发布/数据/上传全链路已实现 |
| **前端页面** | 90% | 11 个核心页面已实现，与设计文档基本对齐 |
| **业务组件** | 60% | VideoTimeline 已实现，VideoPlayer/ImageUploader/ShotCard 缺失 |
| **AI 能力** | 75% | 文生图/TTS/剪辑已封装，图生视频待接入 |
| **数据可视化** | 70% | 趋势图、成本统计已实现，用户画像/成本饼图缺失 |
| **交互体验** | 65% | 无 WebSocket 进度、无流式脚本、无分镜拖拽时长 |

**整体对齐度**：约 **82%**。核心业务链路已完成，部分 P2 增强项待升级。

---

## 二、后端 API 对齐矩阵

### 2.1 short-video 模块（设计文档约定路径）

| 设计文档 API | 实现路径 | 状态 | 说明 |
|-------------|----------|------|------|
| Dashboard | | | |
| `/short-video/dashboard/stats` | ✅ 已实现 | 完成 | 数据概览 |
| `/short-video/dashboard/trend` | ✅ 已实现 | 完成 | 播放量趋势 |
| `/short-video/dashboard/projects` | ✅ 已实现 | 完成 | 我的项目 |
| `/short-video/dashboard/cost-breakdown` | ✅ 已实现 | 完成 | 成本分解（P2 已升级） |
| 快速生成 | | | |
| `/short-video/quick/generate` | ✅ 已实现 | 完成 | 一键生成 |
| 项目管理 | | | |
| `/short-video/project/list` | ✅ 已实现 | 完成 | |
| `/short-video/project/get` | ✅ 已实现 | 完成 | |
| `/short-video/project/save` | ✅ 已实现 | 完成 | |
| `/short-video/project/delete` | ✅ 已实现 | 完成 | |
| 脚本 | | | |
| `/short-video/script/list` | ✅ 已实现 | 完成 | |
| `/short-video/script/get` | ✅ 已实现 | 完成 | |
| `/short-video/script/save` | ✅ 已实现 | 完成 | |
| `/short-video/script/delete` | ✅ 已实现 | 完成 | |
| `/short-video/script/generate` | ✅ 已实现 | 完成 | AI 生成脚本 |
| `/short-video/script/analyze-viral` | ✅ 已实现 | 完成 | 爆款脚本分析 |
| 分镜 | | | |
| `/short-video/shot-list/list` | ✅ 已实现 | 完成 | |
| `/short-video/shot-list/get` | ✅ 已实现 | 完成 | |
| `/short-video/shot-list/get-by-script` | ✅ 已实现 | 完成 | |
| `/short-video/shot-list/save` | ✅ 已实现 | 完成 | |
| `/short-video/shot-list/generate` | ✅ 已实现 | 完成 | AI 生成分镜 |
| 素材生产 | | | |
| `/short-video/material/generate-keyframes` | ✅ 已实现 | 完成 | 含 characterReferenceUrl/sceneReferenceUrl |
| `/short-video/material/generate-voice-batch` | ✅ 已实现 | 完成 | |
| `/short-video/material/img2video-batch` | ✅ 端点存在 | 待接入 | 返回「待接入 MiniMax/Kling」 |
| 剪辑 | | | |
| `/short-video/edit/auto-compose` | ✅ 已实现 | 完成 | |
| `/short-video/edit/generate-subtitles` | ✅ 已实现 | 完成 | |
| 发布 | | | |
| `/short-video/publish/generate-title` | ✅ 已实现 | 完成 | |
| `/short-video/publish/ai-review` | ✅ 已实现 | 完成 | |
| `/short-video/publish/publish` | ✅ 已实现 | 待接入 | 需抖音开放平台 |
| 数据 | | | |
| `/short-video/data/collect-hot-videos` | ✅ 已实现 | 完成 | |
| `/short-video/data/analyze-viral` | ✅ 已实现 | 完成 | |
| 素材库 | | | |
| `/short-video/library/list` | ✅ 已实现 | 完成 | |
| `/short-video/library/delete` | ✅ 已实现 | 完成 | |
| 上传 | | | |
| `/short-video/upload/keyframe` | ✅ 已实现 | 完成 | |
| `/short-video/upload/keyframes/batch` | ✅ 已实现 | 完成 | |
| `/short-video/upload/video` | ✅ 已实现 | 完成 | |
| `/short-video/upload/audio` | ✅ 已实现 | 完成 | |
| `/short-video/upload/thumbnail` | ✅ 已实现 | 完成 | |
| `/short-video/upload/final-video` | ✅ 已实现 | 完成 | |
| `/short-video/upload/reference/character` | ✅ 已实现 | 完成 | |
| `/short-video/upload/reference/scene` | ✅ 已实现 | 完成 | |

### 2.2 shortvideo 模块（旧路径，保留兼容）

| API | 用途 | 状态 |
|-----|------|------|
| `/shortvideo/content/search` | 视频内容搜索 | 已实现，与 project 流程分离 |
| `/shortvideo/category/list` | 分类列表 | 已实现 |
| `/shortvideo/viral/*` | 爆款库 CRUD、复刻、推荐 | 已实现 |
| `/shortvideo/ai/*` | AI 生成文案/脚本/标题/方案 | 已实现 |
| `/shortvideo/script-template/*` | 脚本模板 | 已实现 |

---

## 三、前端页面对齐矩阵

### 3.1 设计文档页面 vs 实现

| 设计文档页面 | 路由 | 实现文件 | 状态 | 缺口 |
|-------------|------|----------|------|------|
| 首页 Dashboard | /admin/shortvideo/dashboard | ShortVideoDashboardPage | ✅ | 成本统计卡片可增强 |
| 快速生成 | /admin/shortvideo/quick | QuickGeneratePage | ✅ | 无 WebSocket 进度弹窗 |
| 项目管理 | /admin/shortvideo/project | ProjectManagementPage | ✅ | — |
| 脚本策划 | /admin/shortvideo/script | ScriptPlanningPage | ✅ | 无流式脚本输出 |
| 分镜设计 | /admin/shortvideo/shot-list | ShotListDesignPage | ✅ | 无时间轴拖拽调整时长 |
| 素材准备 | /admin/shortvideo/prepare | MaterialPreparationPage | ✅ | — |
| 素材生产 | /admin/shortvideo/material | MaterialProductionPage | ✅ | 人物/场景参考已支持 |
| 素材库 | /admin/shortvideo/library | MaterialLibraryPage | ✅ | — |
| 视频剪辑 | /admin/shortvideo/edit | VideoEditingPage | ✅ | VideoTimeline 已集成 |
| 审核发布 | /admin/shortvideo/publish | PublishManagementPage | ✅ | 成本统计已支持 |
| 数据分析 | /admin/shortvideo/analytics | DataAnalysisPage | ✅ | 无用户画像 |
| 爆款库 | /admin/shortvideo/viral | ViralLibraryPage | ✅ | — |

### 3.2 设计文档信息架构 vs 实现

| 设计文档 | 实现 | 说明 |
|----------|------|------|
| 创作中心 → 我的项目 | ✅ | 通过 project 路由 |
| 创作中心 → 快速生成 | ✅ | quick 路由 |
| 创作中心 → 素材库 | ✅ | library 路由 |
| 项目详情 → 脚本/分镜/素材/剪辑/发布 | ✅ | ProjectFlowSidebar 串联 |
| 数据分析 → 数据概览/成本统计 | ⚠️ 部分 | 成本分解有，饼图/趋势图无 |
| 爆款库 → 热门视频/一键复刻 | ✅ | viral 路由 |
| 设置 → 账号/平台/套餐 | ❌ | 未在 shortvideo 模块实现 |

---

## 四、业务组件对齐

### 4.1 设计文档组件清单

| 组件 | 设计文档要求 | 实现状态 | 说明 |
|------|-------------|----------|------|
| **VideoPlayer** | 播放、进度、音量、全屏、倍速、逐帧 | ❌ 缺失 | 当前用原生 `<video>` |
| **VideoTimeline** | 多轨道、拖拽排序、拖拽裁剪 | ⚠️ MVP | 单轨道、缩略图+时长、点击预览 |
| **ImageUploader** | 拖拽、多文件、进度、预览 | ❌ 缺失 | 用 MUI + FormData 原生上传 |
| **ShotCard** | 分镜卡片（预览图+场景+台词+操作） | ❌ 缺失 | 分镜列表用简单 Card |
| **ProgressModal** | 生成进度、WebSocket 实时 | ⚠️ 部分 | AI 模块有，shortvideo 未用 |
| **ProjectFlowSidebar** | 工作流侧边栏 | ✅ 已实现 | |
| **ShortVideoOnboardingOverlay** | 新手引导蒙层 | ✅ 已实现 | |
| **DataCard** | 数据卡片 | ⚠️ 部分 | 用 MUI Card，无 CountUp |
| **FilterPanel** | 筛选面板 | ❌ 缺失 | 数据分析页无高级筛选 |

### 4.2 基础组件（设计文档）

| 组件 | 实现 | 说明 |
|------|------|------|
| Button/Input/Select/Modal/Progress/Card | ✅ MUI | 设计规范已定义，实现用 MUI |
| AppLayout/PageHeader/Sidebar | ✅ 已有 | 布局组件存在 |
| Chart（ECharts） | ✅ ReactECharts | Dashboard、DataAnalysis 已用 |
| Timeline | ⚠️ 部分 | VideoTimeline 为视频专用 |

---

## 五、功能细节缺口清单

### 5.1 高优先级（P1）

| 功能 | 文档出处 | 当前状态 | 建议 |
|------|----------|----------|------|
| **图生视频接入** | PRODUCTION-SYSTEM-DESIGN | img2video-batch 返回「待接入」 | 接入 MiniMax 或 Kling |
| **VideoPlayer 组件** | FRONTEND-PRODUCT-DESIGN | 用原生 video | 封装：倍速、全屏、逐帧 |
| **生成进度 WebSocket** | FRONTEND-PRODUCT-DESIGN | 无 | 快速生成/素材生产需实时进度 |
| **分镜时间轴拖拽时长** | FRONTEND-PRODUCT-DESIGN | ShotListDesign 无时间轴 | 在分镜页增加可拖拽时间轴 |

### 5.2 中优先级（P2）

| 功能 | 文档出处 | 当前状态 | 建议 |
|------|----------|----------|------|
| **流式脚本输出** | FRONTEND-PRODUCT-DESIGN | 一次性返回 | StreamingText 组件 |
| **多轨道时间轴** | FRONTEND-PRODUCT-DESIGN | 仅视频轨 | 增加配音/BGM/字幕轨 |
| **ImageUploader** | FRONTEND-PRODUCT-DESIGN | 无 | 拖拽、多文件、进度 |
| **ShotCard** | FRONTEND-PRODUCT-DESIGN | 简单 Card | 统一分镜卡片样式 |
| **成本统计饼图/趋势** | FRONTEND-PRODUCT-DESIGN | 仅数字 | 增加 ECharts 饼图、趋势图 |
| **数据分析用户画像** | FRONTEND-PRODUCT-DESIGN | 无 | 性别/年龄/地区分布 |
| **CountUp 数字动画** | FRONTEND-PRODUCT-DESIGN | 无 | 数据卡片数字滚动 |
| **暗色主题** | FRONTEND-PRODUCT-DESIGN | 可选 | 电影级 UI 规范 |

### 5.3 低优先级（P3）

| 功能 | 文档出处 | 当前状态 | 建议 |
|------|----------|----------|------|
| **抖音开放平台发布** | PRODUCTION-SYSTEM-DESIGN | publish 待接入 | 需平台授权 |
| **ComfyUI 服务** | CINEMA-GRADE-SUPPLEMENT | 参数预留，未调用 | 需 GPU 部署 |
| **Kling 图生视频** | CINEMA-GRADE-SUPPLEMENT | 未接入 | 电影级运镜 |
| **分镜模板快速应用** | FRONTEND-PRODUCT-DESIGN | 有 API，前端未显式用 | 分镜页增加模板选择 |
| **脚本模板按场景** | 产品需求 | SvScriptTemplateController | 前端可增加入口 |
| **快捷键 Ctrl+S 保存** | FRONTEND-PRODUCT-DESIGN | 无 | 脚本/分镜页 |
| **骨架屏/错误边界** | FRONTEND-PRODUCT-DESIGN | 部分 | 统一加载态 |

---

## 六、API 路径双轨说明

当前存在两套路径：

| 路径前缀 | 用途 | 建议 |
|----------|------|------|
| `/api/v1/short-video/*` | 新流程：项目→脚本→分镜→素材→剪辑→发布 | **主路径**，与设计文档一致 |
| `/api/v1/shortvideo/*` | 旧能力：内容搜索、分类、爆款库、AI 生成、脚本模板 | 保留兼容，不强制迁移 |

前端 `api/shortvideo.ts` 已正确区分：
- 新流程调用 `/short-video/*`
- 爆款库、内容搜索、分类调用 `/shortvideo/*`

---

## 七、数据库与 BOS 对齐

| 项目 | 状态 | 说明 |
|------|------|------|
| sv_project, sv_script, sv_shot_list, sv_shot, sv_material | ✅ | 核心 5 表已实现 |
| BOS 路径规范 | ✅ | ShortVideoPathHelper 已对齐 |
| 上传 API 与 BOS 联动 | ✅ | 7 个上传接口 + 生成后自动上传 |
| hot_video_collection | ⚠️ | 设计提及，实现待确认 |

---

## 八、升级建议（按优先级）

### 8.1 立即可做（1-2 人日）

1. **成本统计可视化**：DataAnalysisPage、PublishManagementPage 增加 ECharts 饼图
2. **VideoPlayer 封装**：抽取通用播放器组件，支持倍速、全屏
3. **ProgressModal 复用**：快速生成、素材生产接入 AI 模块的 ProgressModal（轮询模拟）

### 8.2 短期（1-2 周）

4. **图生视频接入**：对接 MiniMax 或 Kling，实现 img2video-batch
5. **分镜页时间轴**：ShotListDesignPage 增加简易时间轴，支持拖拽调整时长
6. **流式脚本**：脚本生成改为 SSE/流式，前端 StreamingText 展示
7. **ImageUploader**：通用图片上传组件，拖拽、多文件、进度

### 8.3 中期（2-4 周）

8. **WebSocket 生成进度**：后端实现 `/progress` WebSocket，前端实时展示
9. **多轨道 VideoTimeline**：配音轨、BGM 轨、字幕轨
10. **数据分析用户画像**：接入画像数据源，ECharts 饼图/柱状图
11. **ShotCard 统一**：分镜列表统一使用 ShotCard 组件
12. **暗色主题**：按设计规范实现电影级暗色 UI

### 8.4 长期（依赖外部）

13. **抖音开放平台**：publish 接口接入
14. **ComfyUI**：GPU 部署后接入人物/场景参考
15. **Kling**：电影级图生视频

---

## 九、文档与代码同步建议

| 文档 | 建议更新 |
|------|----------|
| DESIGN-ALIGNMENT-ANALYSIS.md | 已过时，API/页面状态需按本文档修正 |
| SHORT-VIDEO-FRONTEND-PRODUCT-DESIGN.md | 实现状态表可补充 VideoTimeline、成本统计、ComfyUI 参数 |
| SHORT-VIDEO-P2-UPGRADE-PLAN.md | 实施记录已更新，可增加后续 P3 项 |
| 01-产品需求总览.md | 与当前模块实现一致，无需大改 |

---

**文档版本**: v1.0  
**最后更新**: 2026-03-01
