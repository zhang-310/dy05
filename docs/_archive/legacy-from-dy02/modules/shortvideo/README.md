# 短视频模块（shortvideo）

## 模块概述

短视频全链路生产管理：项目管理 → 脚本策划 → 分镜设计 → 关键帧生成 → 视频合成 → 发布管理。集成 AI 脚本生成、爆款分析、素材库、质量评估等功能。

## 后端结构

```
module/shortvideo/
├── config/
│   ├── SvHotTopicSyncScheduler.java          # 热门话题同步调度
│   ├── ViralVideoCollectorScheduler.java     # 爆款视频采集调度
│   ├── VideoGenerationTaskAmqpConfig.java    # 视频生成任务 AMQP 配置
│   ├── VideoGenerationTaskAmqpConsumer.java  # 视频生成任务消费者
│   └── WorkflowTaskExecutorConfig.java       # 工作流执行器配置
│
├── controller/（约 25 个）
│   ├── ShortVideoShootingTaskController.java # 拍摄任务工单（五主播管线 MVP）
│   ├── ShortVideoProjectController.java      # 项目管理
│   ├── ShortVideoScriptController.java       # 脚本管理
│   ├── ShortVideoAiController.java           # AI 脚本生成
│   ├── ShortVideoShotListController.java     # 分镜设计
│   ├── ShortVideoMaterialController.java     # 素材管理
│   ├── ShortVideoMaterialLibraryController.java # 素材库
│   ├── ShortVideoDashboardController.java    # 仪表盘
│   ├── ShortVideoController.java             # 内容日历（content/calendar）
│   ├── QualityDashboardController.java       # 质量评估
│   ├── ViralVideoController.java             # 爆款视频
│   ├── DramaController.java                  # 短剧管理
│   ├── VideoGenerationTaskController.java   # 视频生成
│   ├── ShortVideoPublishController.java      # 发布管理
│   ├── WorkflowController.java               # 工作流
│   ├── SvScriptTemplateController.java       # 脚本模板
│   ├── ContrastVideoTemplateController.java  # 对比视频模板（路径 /api/v1/shortvideo/，无连字符）
│   ├── ShortVideoFeedbackController.java     # 反馈分析
│   └── ...
│
├── entity/（30+ 个）
│   ├── SvShootingTask.java                   # 拍摄任务工单
│   ├── SvProject.java                        # 项目
│   ├── SvScript.java                         # 脚本
│   ├── SvShotList.java                       # 分镜
│   ├── SvShot.java                           # 分镜镜头
│   ├── SvMaterial.java                       # 素材
│   ├── SvVideoData.java                      # 视频数据
│   ├── SvVideo.java                          # 视频
│   ├── SvViralVideo.java                     # 爆款视频
│   ├── SvViralFavorite.java                  # 爆款收藏
│   ├── SvDrama.java                          # 短剧
│   ├── SvDramaEpisode.java                   # 短剧集
│   ├── SvDramaCharacter.java                 # 短剧角色
│   ├── SvPlan.java                           # 计划
│   ├── SvPlanAsset.java                      # 计划资产
│   ├── SvScriptTemplate.java                 # 脚本模板
│   ├── SvCategory.java                       # 分类
│   ├── SvHotTopic.java                       # 热门话题
│   ├── SvDailyBatch.java                     # 每日批次
│   ├── SvPublishTimeAnalysis.java            # 发布时间分析
│   ├── SvWorkflowTask.java                   # 工作流任务
│   ├── SvVideoGenerationTask.java            # 视频生成任务
│   ├── SvVideoGeneration.java                # 视频生成
│   ├── SvGenerationLog.java                  # 生成日志
│   ├── SvCinematicPreset.java                # 电影预设
│   ├── SvSceneCameraMapping.java             # 场景镜头映射
│   ├── SvComment.java                        # 评论
│   └── ...
│
├── service/（35+ 个）
│   ├── ShortVideoProjectService.java         # 项目管理
│   ├── ShortVideoScriptService.java          # 脚本管理
│   ├── ShortVideoAiService.java              # AI 脚本生成（核心）
│   ├── ShotListService.java                  # 分镜
│   ├── MaterialService.java                  # 素材
│   ├── ViralVideoService.java                # 爆款视频
│   ├── DramaService.java                     # 短剧
│   ├── VideoGenerationTaskService.java       # 视频生成
│   ├── ContentCalendarService.java           # 内容日历
│   ├── QualityDashboardService.java          # 质量仪表盘
│   ├── ShortVideoPublishService.java         # 发布
│   └── ...
│
└── vo/（30+ 个）
    ├── ProjectSaveVO / SearchVO / VO
    ├── ScriptSaveVO / SearchVO / VO
    ├── AiCopyGenerateVO                      # AI 文案生成请求
    ├── ShotListSaveVO / VO
    └── ...
```

## 数据库表

| 表名 | 说明 |
|------|------|
| sv_project | 短视频项目 |
| sv_shooting_task | 拍摄任务工单（运营派单 / 摄影师；Flyway **V098**） |
| sv_script | 脚本 |
| sv_shot_list | 分镜列表 |
| sv_material | 素材 |
| sv_video_data | 视频数据 |
| sv_viral_video | 爆款视频 |
| sv_drama | 短剧 |
| sv_drama_episode | 短剧集 |
| sv_drama_character | 短剧角色 |
| sv_content_calendar | 内容日历 |
| sv_script_template | 脚本模板 |
| sv_category | 视频分类 |
| sv_hot_topic | 热门话题 |
| sv_video_generation_task | 视频生成任务 |
| sv_daily_batch | 每日批次 |
| sv_publish_time_analysis | 发布时间分析 |
| sv_workflow_task | 工作流任务 |
| sv_viral_favorite | 爆款收藏 |
| sv_cinematic_preset | 电影预设 |
| sv_generation_log | 生成日志 |
| sv_scene_camera_mapping | 场景镜头映射 |
| sv_plan_asset | 计划资产 |
| sv_video_generation | 视频生成 |
| sv_plan | 计划 |
| sv_comment | 评论 |
| sv_shot | 分镜镜头 |
| sv_video | 视频 |

SQL 文件：`sql/shortvideo/`

> **备注**：ContrastVideoTemplateController 路径为 `/api/v1/shortvideo/`（无连字符），其他 Controller 为 `/api/v1/short-video/`。

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 短视频仪表盘 | `ShortVideoDashboardPage.tsx` | `/admin/shortvideo/dashboard` |
| 项目管理 | `ProjectManagementPage.tsx` | `/admin/shortvideo/project` |
| 拍摄任务 | `ShootingTaskPage.tsx` | `/admin/shortvideo/shooting-tasks` |
| 脚本策划 | `ScriptPlanningPage.tsx` | `/admin/shortvideo/script` |
| 快速生成 | `QuickGeneratePage.tsx` | `/admin/shortvideo/quick` |
| 分镜设计 | `ShotListDesignPage.tsx` | `/admin/shortvideo/shot-list` |
| 素材准备 | `MaterialPreparationPage.tsx` | `/admin/shortvideo/prepare` |
| 素材制作 | `MaterialProductionPage.tsx` | `/admin/shortvideo/material` |
| 素材库 | `MaterialLibraryPage.tsx` | `/admin/shortvideo/library` |
| 爆款库 | `ViralLibraryPage.tsx` | `/admin/shortvideo/viral` |
| 短剧编辑 | `DramaEditorPage.tsx` | `/admin/shortvideo/drama` |
| 视频编辑 | `VideoEditingPage.tsx` | `/admin/shortvideo/edit` |
| 发布管理 | `PublishManagementPage.tsx` | `/admin/shortvideo/publish` |
| 数据分析 | `DataAnalysisPage.tsx` | `/admin/shortvideo/analytics` |
| 内容日历 | `ContentCalendarPage.tsx` | `/admin/shortvideo/calendar` |
| 每日内容 | `DailyContentPage.tsx` | `/admin/shortvideo/daily` |
| 质量仪表盘 | `QualityDashboardPage.tsx` | `/admin/shortvideo/quality-dashboard` |
| 效果预测 | `ContentEffectPredictPage.tsx` | `/admin/shortvideo/predict` |
| 竞品监控 | `CompetitorMonitorPage.tsx` | `/admin/shortvideo/competitor`；机构 **`/org/shortvideo/competitor`**、达人 **`/talent/shortvideo/competitor`**（同页） |
| 工作流 | `WorkflowEditorPage.tsx` | `/admin/shortvideo/workflow` |

## 成片调色（V-4）

- 说明文档：[V-4-成片调色与FFmpeg约束.md](./V-4-成片调色与FFmpeg约束.md)（非 LUT、eq/unsharp、未接 hue/vibrance）

## 前端 API

文件：`api/shortvideo.ts`、`api/daily-content.ts`、`api/competitor.ts`、`api/compliance.ts`

## API 接口清单（非 POST）

| 接口 | 说明 |
|------|------|
| GET /api/v1/short-video/ai/generate-copy-sse | [GET SSE] 流式生成文案 |
| GET /api/v1/short-video/ai/generate-script-sse | [GET SSE] 流式生成脚本 |

## 前端组件

| 组件 | 文件 | 说明 |
|------|------|------|
| ShotCard | `components/shortvideo/ShotCard.tsx` | 分镜卡片 |
| ShotTimeline | `components/shortvideo/ShotTimeline.tsx` | 分镜时间线 |
| SortableShotCard | `components/shortvideo/SortableShotCard.tsx` | 可拖拽分镜 |
| ImageUploader | `components/shortvideo/ImageUploader.tsx` | 图片上传 |
| VideoPlayer | `components/shortvideo/VideoPlayer.tsx` | 视频播放器 |
| VideoTimeline | `components/shortvideo/VideoTimeline.tsx` | 视频时间线 |
| StreamingText | `components/shortvideo/StreamingText.tsx` | 流式文本显示 |
| BatchOperationPanel | `components/shortvideo/BatchOperationPanel.tsx` | 批量操作 |
| BgmPanel | `components/shortvideo/BgmPanel.tsx` | 背景音乐 |
| CameraControlPanel | `components/shortvideo/CameraControlPanel.tsx` | 镜头控制 |
| QualitySelector | `components/shortvideo/QualitySelector.tsx` | 质量选择 |
| ProjectFlowSidebar | `components/shortvideo/ProjectFlowSidebar.tsx` | 项目流程侧边栏 |
| CountUp | `components/shortvideo/CountUp.tsx` | 数字动画 |
| ShortVideoOnboardingOverlay | `components/shortvideo/ShortVideoOnboardingOverlay.tsx` | 新手引导遮罩 |

## 核心业务流程

```
1. 创建项目 (SvProject)
2. 脚本策划
   ├── AI 生成脚本 (ShortVideoAiService)
   ├── 使用脚本模板 (ScriptTemplate)
   └── 手动编写
3. 分镜设计 (SvShotList)
   ├── AI 生成分镜
   └── 手动设计
4. 关键帧生成
   ├── AI 图片生成 (text2img)
   └── 手动上传
5. 视频生成
   ├── 图生视频 (img2video)
   ├── 视频合成 (autoCompose)
   └── 字幕生成 (generateSubtitles)
6. 发布管理
   ├── 平台发布 (publishDouyin)
   └── 内容日历排期
7. 数据分析 & 效果追踪
```
