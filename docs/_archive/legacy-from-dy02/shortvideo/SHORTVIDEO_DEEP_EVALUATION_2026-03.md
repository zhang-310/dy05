# 短视频模块 — 全面深度评估报告

> 评估日期：2026-03-02 | 评估版本：v3

---

## 一、评估结论（摘要）

| 维度 | 评分 | 说明 |
|------|------|------|
| 代码结构 | 9/10 | 分层清晰，Controller/Service/Repository 职责明确 |
| 数据库 | 8/10 | 迁移脚本完整，Entity 与 schema 已对齐 |
| 设计 vs 实现 | 6/10 | 设计文档用 GET/shortvideo，实现用 POST/short-video；附录 A/B 提供对照 |
| 质量与规范 | 8.5/10 | 数据隔离到位（Video/Category/Comment/ScriptTemplate/PublishFeedback），错误码 3200 段已使用 |
| 依赖集成 | 8/10 | douyin、ai、storage 集成正确 |
| **综合** | **8.0/10** | 制作流程完整，效果归因链已打通，数据隔离已补全 |

---

## 二、模块结构概览

### 2.1 核心实体与表

| 表名 | 说明 | 数据隔离 |
|------|------|----------|
| sv_video | 视频主表 | visibleOwnerIds ✓ |
| sv_video_data | 视频每日快照 | 通过 video 关联 |
| sv_comment | 评论 | 通过 video.ownerId ✓ |
| sv_project | 制作项目 | ownerId ✓ |
| sv_script | 脚本 | ownerId ✓ |
| sv_shot_list | 分镜列表 | ownerId ✓ |
| sv_shot | 分镜详情 | 通过 shot_list 关联 |
| sv_material | 素材库 | ownerId ✓ |
| sv_category | 分类 | ownerId ✓ |
| sv_script_template | 话术模板 | ownerId + system ✓ |
| sv_drama | 短剧 | ownerId ✓ |
| sv_viral_favorite | 爆款收藏 | ownerId ✓ |
| sv_cinematic_preset | 运镜预设 | ownerId ✓ |
| sv_generation_log | 生成日志 | 通过 project 关联 |

### 2.2 数据隔离覆盖情况

- **已覆盖**：SvVideo、SvCategory、SvComment、SvScriptTemplate、PublishFeedbackService、SvProject、SvScript、SvShotList、DramaService、MaterialLibrary
- **发布反馈 API**：ShortVideoFeedbackController 暴露 analyze-performance、reflection-report、weekly-report
- **DataScope**：SvVideo、SvComment 使用 `visibleOwnerIds`（管理员可见下属）
- **单用户**：SvProject、SvScript、SvCategory 等使用 `ownerId`（仅本人）

---

## 三、待改进项

### P0（高优）

| # | 问题 | 建议 |
|---|------|------|
| 1 | 工作流任务状态持久化不足 | Redis 不可用时内存回退，重启丢失。已新增 sv_workflow_task 表，支持 DB 持久化 |
| 2 | 工作流 publish 占位 | 当前仅标题生成 + AI 审核，未真正发布到抖音。需接入抖音开放平台 |

### P1（重要）

| # | 问题 | 建议 |
|---|------|------|
| 3 | aiAssistNode 占位 | 已接入 LLM 解析意图，可继续优化 |
| 4 | 视频同步接口 | POST /api/v1/douyin/video/sync 已存在；已补全账号归属校验 |
| 5 | 发布时间推荐 | 基于 sv_publish_time_analysis 实现接口 |
| 6 | ~~内容日历~~ | ✓ 已实现 |
| 7 | ~~SvProject DataScope~~ | ✓ 已实现 |

### P2（优化）

| # | 问题 | 建议 |
|---|------|------|
| 8 | ~~接口文档统一~~ | ✓ 已补充附录 B 设计 vs 实际路径对照表 |
| 9 | ~~合并重复接口~~ | ✓ script/analyze-viral 为主入口，data/analyze-viral 标注为别名 |
| 10 | ContentAuditServiceImpl | 图像/视频/文本审核均为 TODO，待接入百度云 API |

---

## 四、迁移脚本清单（shortvideo）

| 脚本 | 说明 | 幂等 |
|------|------|------|
| migration-bos-production.sql | sv_project/script/shot_list/shot/material 等基础表 | ✓ |
| migration-phase2-async-task.sql | sv_video_generation_task | ✓ |
| migration-phase3-drama.sql | sv_drama/episode/character | ✓ |
| migration-phase5-knowledge.sql | sv_cinematic_preset, sv_generation_log, sv_scene_camera_mapping | ✓ |
| migration-daily-shoot.sql | sv_project 新增 schedule_date/shoot_status/persona_id；sv_shot 新增 review_status/reviewer_note | ✓ |
| migration-phase1-cinematic.sql | sv_shot/sv_material 新增运镜、质量、AI 字段 | ✓ |
| migration-shot-end-frame.sql | sv_shot 新增 end_frame_url/end_frame_bos_key | ✓ |
| migration-project-reference-urls.sql | sv_project 新增 character_reference_url/scene_reference_url | ✓ |
| migration-viral-favorite.sql | sv_viral_favorite | ✓ |
| migration-script-template.sql | 话术模板资源 | ✓ |
| migration-resource-short-video.sql | auth_resource 路径统一 | ✓ |
| migration-hot-video-collection.sql | hot_video_collection | ✓ |
| migration-v32-design.sql | sv_drama_character 扩展；sv_shot 音频字段；sv_generation_log 扩展 | ✓ |
| migration-workflow-task.sql | sv_workflow_task 工作流任务持久化 | ✓ |
| migration-publish-time-analysis.sql | sv_publish_time_analysis 表（增量） | ✓ |
| migration-feedback-content-resources.sql | feedback/content/project/ai/script-template 等 API 资源 | ✓ |

---

## 五、双轨模型说明

- **策划模型**：sv_plan + sv_plan_asset（遗留，部分场景）
- **制作模型**：sv_project → sv_script → sv_shot_list → sv_shot → sv_material（主流程）

详见 `docs/modules/shortvideo/09-数据模型说明.md`。

---

## 六、已知 TODO（待接入）

| 位置 | 说明 |
|------|------|
| ContentAuditServiceImpl | 图像/视频/文本审核为 TODO，待接入百度云审核 API |
| ShortVideoPublishController.publish | 工作流 publish 节点为占位，待接入抖音开放平台 |
| DouyinVideoService.syncVideos | 视频同步为 TODO，待接入抖音数据 API |

---

**报告版本**：v3.0  
**依据**：`docs/modules/shortvideo/*`、`sql/shortvideo/*`、`src/.../shortvideo/**`
