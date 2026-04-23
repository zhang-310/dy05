# 短视频 P2 升级计划

**版本**: v1.0  
**日期**: 2026-03-01  
**范围**: ComfyUI 人物/场景参考、视频时间轴编辑器、成本统计

---

## 一、升级项概览

| 序号 | 升级项 | 复杂度 | 依赖 | 优先级 |
|-----|--------|--------|------|--------|
| 1 | **成本统计** | 低 | 无 | P2-1 |
| 2 | **ComfyUI 人物/场景参考** | 高 | GPU 服务器、ComfyUI 部署 | P2-2 |
| 3 | **视频时间轴编辑器** | 中 | 无 | P2-3 |

---

## 二、成本统计

### 2.1 设计目标

- 审核发布页展示单项目成本分解
- 数据分析页展示总成本趋势与分解
- 成本项：脚本生成、图像生成、视频生成、配音合成、存储/流量

### 2.2 成本估算规则（参考设计文档）

| 成本项 | 单价 | 说明 |
|--------|------|------|
| 脚本生成 | ¥0.001/次 | DeepSeek API |
| 图像生成 | ¥0 | ComfyUI 自部署 |
| 视频生成 | ¥0.4/段(5s) | Kling 图生视频 |
| 配音合成 | ¥0.01/段 | TTS |
| 存储/流量 | ¥0.01/条 | BOS |

### 2.3 实现方案

**后端**：
- 扩展 `ShortVideoDashboardService.getStats` 返回 `costBreakdown`
- 新增 `GET /short-video/dashboard/cost-breakdown?projectId=` 单项目成本
- 基于 `sv_material` 数量估算（image 数→视频段数，audio 数→配音段数）

**前端**：
- PublishManagementPage：加载项目时展示成本统计卡片
- DataAnalysisPage：数据概览增加成本分解 Tab 或折叠区

---

## 三、ComfyUI 人物/场景参考

### 3.1 设计目标

- 关键帧生成时支持传入人物参考图、场景参考图
- 人物参考 → IP-Adapter 保证人物一致性
- 场景参考 → ControlNet depth 控制场景

### 3.2 前置条件

- GPU 服务器（RTX 4090 24GB 或 A100）
- ComfyUI 部署 + API 暴露
- 模型：IP-Adapter、ControlNet depth

### 3.3 实现方案（分阶段）

**阶段 1：API 参数扩展（无 ComfyUI 时降级）**
- `generateKeyframes` 增加 `characterReferenceUrl`、`sceneReferenceUrl`
- 后端：有 ComfyUI 时调用 ComfyUI，无则继续用 text2img
- 前端：MaterialProductionPage 支持选择已上传的人物/场景参考

**阶段 2：ComfyUI 服务封装**
- 新增 `ComfyUIService`，调用 ComfyUI API
- 工作流：加载人物参考(IP-Adapter) + 场景参考(ControlNet) + 文生图

**阶段 3：素材准备页与素材生产联动**
- MaterialPreparationPage 上传后返回 characterId/sceneId
- MaterialProductionPage 选择 characterId、sceneId，生成时传入对应 URL

---

## 四、视频时间轴编辑器

### 4.1 设计目标

- 多轨道：视频轨、配音轨、BGM 轨、字幕轨
- 拖拽排序、裁剪、分割
- 预览区 + 时间轴联动

### 4.2 技术选型

| 方案 | 优点 | 缺点 |
|------|------|------|
| Fabric.js | 灵活、可定制 | 需自建时间轴逻辑 |
| react-player + 自定义轨道 | 轻量 | 功能有限 |
| 现成库（如 timeline） | 开箱即用 | 可能过重 |

### 4.3 实现方案（MVP）

**MVP 范围**：
- 单轨道：视频片段列表，按分镜顺序展示
- 每段显示缩略图 + 时长
- 支持拖拽调整顺序（可选）
- 预览区播放成片或当前选中片段

**后续增强**：
- 多轨道
- 分割、裁剪
- 转场效果

---

## 五、实施顺序建议

1. **成本统计**（1-2 人日）：无外部依赖，快速交付
2. **ComfyUI API 参数扩展**（0.5 人日）：为后续 ComfyUI 接入预留接口
3. **视频时间轴 MVP**（2-3 人日）：提升剪辑页体验
4. **ComfyUI 服务封装**（需 GPU 环境）：按需部署后实施

---

## 六、实施记录

| 升级项 | 状态 | 说明 |
|--------|------|------|
| 成本统计 | ✅ 已完成 | 后端 `cost-breakdown` API；PublishManagementPage、DataAnalysisPage 展示 |
| ComfyUI 人物/场景参考 | ✅ 已完成 | `generateKeyframes` 增加 `characterReferenceUrl`、`sceneReferenceUrl`；MaterialProductionPage 选择参考图；无 ComfyUI 时降级 text2img |
| 视频时间轴 | ✅ 已完成 | `VideoTimeline` 组件：单轨道、缩略图+时长、点击预览；集成到 VideoEditingPage |

---

## 七、P1-P2 全面升级（2026-03-01）

| 升级项 | 状态 | 说明 |
|--------|------|------|
| 图生视频 | ✅ | 接入 AI 模块 `generateFromFrames`（FFmpeg 首尾帧），MaterialProductionPage 可批量图生视频 |
| VideoPlayer | ✅ | 倍速、全屏、逐帧、音量；集成到 VideoEditingPage、PublishManagementPage |
| 生成进度 | ✅ | QuickGeneratePage 使用 ProgressModal + 模拟进度 |
| 分镜时间轴拖拽 | ✅ | ShotTimeline 组件，Slider 调整每镜时长；ShotListDesignPage 集成 |
| 流式脚本 | ✅ | StreamingText 组件；ScriptPlanningPage 支持流式/完整切换 |
| 多轨道时间轴 | ✅ | VideoTimeline 支持 audioTrack、bgmUrl 参数 |
| ImageUploader | ✅ | 拖拽、多文件、进度、预览；可复用 |
| ShotCard | ✅ | 统一分镜卡片；ShotListDesignPage 使用 |
| 成本统计可视化 | ✅ | ECharts 饼图；PublishManagementPage、DataAnalysisPage |
| 用户画像 | ✅ | 数据分析页年龄分布柱状图（示例数据） |
| CountUp | ✅ | 数字滚动动效；DataAnalysisPage 数据卡片 |
| 暗色主题 | ✅ | 已有 AppThemeProvider 支持 |

---

**文档版本**: v1.2  
**最后更新**: 2026-03-01
