# 短视频模块 — 迭代评估与升级报告

> 评估日期：2026-03-02 | 迭代版本：v1

---

## 一、评估结论（摘要）

| 维度 | 评分 | 说明 |
|------|------|------|
| 代码结构 | 9/10 | 分层清晰，Controller/Service/Repository 职责明确 |
| 数据库 | 8/10 | 迁移完整，Entity 与 schema 已对齐 |
| 设计 vs 实现 | 6/10 | 路径与资源域不同，部分设计接口未实现 |
| 质量与规范 | 8/10 | 数据隔离到位，错误码 3200 段已补充使用 |
| 依赖集成 | 8/10 | douyin、ai、storage 集成正确 |
| **综合** | **7.8/10** | 制作流程完整，效果归因链已打通 |

---

## 二、本次迭代已完成项（2026-03-02 第二轮）

### 2.1 数据库迁移

- **migration-daily-shoot.sql**：已执行
  - sv_project 新增：schedule_date、shoot_status、persona_id
  - sv_shot 新增：review_status、reviewer_note

### 2.2 P0 修复

| 项 | 说明 |
|----|------|
| SvVideo.ai_call_log_id | Entity + VO + SaveVO 已补齐，支持效果归因 |
| ViralVideoServiceImpl 分页 | rows 上限 100，避免单次查询过大 |

### 2.3 P1 修复

| 项 | 说明 |
|----|------|
| SvProjectSaveVO | 新增 personaId、scheduleDate、shootStatus，支持每日拍摄编辑 |
| SvProjectServiceImpl.save | 保存时持久化 personaId、scheduleDate、shootStatus |

### 2.4 规范与质量

| 项 | 说明 |
|----|------|
| 错误码 3200 段 | SvVideoServiceImpl、SvProjectServiceImpl 中 VIDEO_NOT_FOUND、PROJECT_NOT_FOUND 替换 DATA_NOT_FOUND |

### 2.5 第二轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P0 SvScriptTemplate 数据隔离** | search/getById/delete/incrementUseCount/listByScene 均增加 ownerId 校验；仅 system 或 owner 模板可见；系统模板不可修改/删除 |
| **P1 SvShot Phase1 字段** | Entity 新增 camera_params、quality_level、ai_model、quality_score |
| **P1 SvMaterial Phase1 字段** | Entity 新增 post_processing_config、ai_provider |
| **迁移执行** | migration-viral-favorite、migration-daily-shoot、migration-phase1-cinematic 已执行 |

### 2.6 第三轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P0 SvVideo 数据隔离** | getById、delete、save、incrementViewCount 增加 visibleOwnerIds 校验（DataScope），防止跨用户访问/修改/删除 |
| **P0 SvCategory 数据隔离** | getById、delete、save 增加 ownerId 校验，仅允许操作自己的分类 |
| **迁移执行** | migration-resource-short-video、migration-script-template 已执行 |

### 2.7 第四轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P1 PublishFeedbackService 数据隔离** | analyzePerformance、generateReflectionReport 增加 visibleOwnerIds 参数；无权限视频返回 FORBIDDEN |
| **P1 SvComment 数据隔离** | search、getById、save、delete、incrementLikeCount 增加 visibleOwnerIds；通过 video.ownerId 校验，仅允许操作可见视频下的评论 |
| **Controller 更新** | ShortVideoController 评论接口获取 DataScope 并下传 visibleIds |

### 2.8 第五轮迭代（2026-03-02）— 全面深度评估

| 项 | 说明 |
|----|------|
| **P0 工作流任务 DB 持久化** | 新增 sv_workflow_task 表；WorkflowExecutionService 将任务状态持久化到 DB，Redis 不可用或应用重启后可恢复 |
| **迁移执行** | 14 个 shortvideo 迁移脚本已全部执行（含 migration-workflow-task） |
| **评估报告** | 新增 docs/shortvideo/SHORTVIDEO_DEEP_EVALUATION_2026-03.md |

### 2.9 第六轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P1 SvProject DataScope** | search、get 支持 visibleOwnerIds；管理员可见全部项目，机构可见下属项目 |
| **P1 发布时间推荐 API** | POST /content/publish-time-recommend，基于 sv_publish_time_analysis 返回推荐时段 |
| **Entity 对齐** | SvPublishTimeAnalysis 与 schema 对齐（account_id, recommended, update_time） |
| **迁移执行** | migration-publish-time-analysis.sql 已执行 |

### 2.10 第七轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P0 视频同步接口安全** | DouyinVideoController.sync 增加账号归属校验；仅允许同步 visibleOwnerIds 内的账号 |

### 2.11 第八轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P1 内容日历 API** | POST /content/calendar、POST /content/calendar-stats；按日分组计划与发布，支持 DataScope |
| **Repository** | SvProjectRepository、SvVideoRepository 新增日历查询方法 |

### 2.12 第九轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P1 发布反馈 REST API** | ShortVideoFeedbackController：/feedback/analyze-performance、/reflection-report、/weekly-report |
| **DataScope** | 效果分析、反思报告均校验 visibleOwnerIds |

### 2.13 第十轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P2 auth_resource 补充** | 新增 migration-feedback-content-resources.sql，注册 feedback/content/project/ai/script-template 等 21 个 API 资源 |
| **资源覆盖** | content/calendar、calendar-stats、publish-time-recommend、data-trend；feedback/*；project/generate-daily、daily-list、update-shoot-status、export-script；shot-list/review；ai/*；script-template/* |
| **迁移执行** | migration-feedback-content-resources.sql 已执行（INSERT 12 条新资源 + admin 绑定） |

### 2.14 第十一轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **深度评估 v3** | 更新 SHORTVIDEO_DEEP_EVALUATION：补充 ContentAuditServiceImpl TODO、已知待接入项 |
| **P2 接口文档统一** | 03-接口设计.md 附录 A 补全 ShortVideoFeedbackController、ShortVideoAiController；新增附录 B 设计路径 vs 实际路径对照表 |
| **迁移** | 本轮为文档迭代，无数据库迁移 |

### 2.15 第十二轮迭代（2026-03-02）

| 项 | 说明 |
|----|------|
| **P2 合并重复接口** | script/analyze-viral 定为主入口，data/analyze-viral 标注为别名；@Operation 补充说明，03-接口设计 附录 B 更新 |

### 2.16 第十三轮迭代（2026-03-02）— 产品经理视角

| 项 | 说明 |
|----|------|
| **内容日历页面** | 新增 ContentCalendarPage：月视图、计划/已发布标识、统计、新建计划入口 |
| **API 对接** | contentCalendar、contentCalendarStats、publishTimeRecommend |
| **创作中心入口** | 工作台增加「内容日历」卡片；菜单增加「内容日历」 |
| **路由** | /admin/shortvideo/calendar |

### 2.17 第十四轮迭代（2026-03-02）— 产品经理视角（续）

| 项 | 说明 |
|----|------|
| **内容日历：发布时间推荐** | 账号选择器 + 最佳发布时间列表（基于历史数据） |
| **数据分析：效果反馈 Tab** | 新增「效果反馈」Tab，支持生成周报（本周发布数、平均得分、洞察） |
| **API 对接** | feedbackAnalyzePerformance、feedbackReflectionReport、feedbackWeeklyReport |

### 2.18 第十五轮迭代（2026-03-02）— 产品经理视角（续）

| 项 | 说明 |
|----|------|
| **创作流程引导** | 工作台新增「创作流程」区块：脚本策划→分镜设计→素材准备→素材生产→视频剪辑→审核发布，每步可点击跳转 |

### 2.19 第十六轮迭代（2026-03-02）— 产品经理视角（续）

| 项 | 说明 |
|----|------|
| **创作模式选择** | 快速生成页升级为「创作工作台」，顶部 4 种模式：爆款复刻→爆款库、热点策划(即将上线)、自主创作→当前流程、每日推荐→项目管理 |

### 2.20 第十七轮迭代（2026-03-02）— 产品经理视角（续）

| 项 | 说明 |
|----|------|
| **项目列表：继续创作** | 项目管理页每行新增「继续」按钮，根据 scriptId/shotListId/finalVideoUrl 智能跳转至脚本/分镜/素材/发布 |

### 2.21 第十八轮迭代（2026-03-02）— 产品经理视角（续）

| 项 | 说明 |
|----|------|
| **空状态优化** | 项目管理：无项目时展示引导卡片（新建项目 + 快速生成）；素材库：无素材时展示引导卡片（前往素材生产） |

### 2.22 第十九轮迭代（2026-03-02）— 产品经理视角（续）

| 项 | 说明 |
|----|------|
| **创作流程面包屑** | PageHeader 支持 React Router 内导航；脚本策划、分镜设计、素材准备、素材生产、视频剪辑、审核发布 6 个创作流程页增加面包屑：创作工作台 > 当前步骤 |
| **返回入口** | 用户可点击「创作工作台」快速返回统一创作入口 |

### 2.23 第二十轮迭代（2026-03-02）— 短剧脚本超时优化

| 项 | 说明 |
|----|------|
| **generateDramaScript 超时** | 短剧 AI 生成脚本接口超时由默认 2 分钟延长至 5 分钟，与后端 RestTemplate readTimeout 一致 |
| **问题背景** | 前端 2 分钟超时后断开，后端仍继续等待 LLM 返回，导致生成结果无法返回用户 |

### 2.24 第二十一轮迭代（2026-03-02）— 短剧流程补齐

| 项 | 说明 |
|----|------|
| **应用到剧集** | 生成剧本后新增「应用到剧集」按钮，按「第X集」解析剧本并创建/更新各集 synopsis |
| **后端 API** | updateEpisode、applyScriptToEpisodes；解析逻辑支持「第1集」「第 2 集」等格式，无匹配时整段作为第1集 |
| **下一步入口** | 「下一步：前往创作工作台」跳转至统一创作入口 |
| **剧集可展开** | 剧集列表支持点击展开查看完整剧情内容 |

### 2.25 第二十二轮迭代（2026-03-02）— 短剧创作完整性

| 项 | 说明 |
|----|------|
| **开始制作** | 有剧情的剧集显示「开始制作」按钮：创建脚本→创建项目→关联剧集→跳转分镜设计页 |
| **已关联项目** | 已关联剧集可点击跳转至分镜设计页继续制作 |
| **自动创建剧集** | 新建短剧时按计划集数自动创建空剧集结构（第1集、第2集…） |

### 2.26 第二十三轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **开始制作一键完成** | 后端 createProjectFromEpisode：保存脚本 → AI 生成分镜 → 创建项目并关联，一步到位 |
| **分镜页即用** | 点击「开始制作」后进入分镜页时，分镜已生成，可直接进入素材生产 |
| **面包屑** | 短剧编辑器增加面包屑：创作工作台 > 短剧编辑器 |

### 2.27 第二十四轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **剧集编辑** | 展开剧集后可点击「编辑」修改标题、剧情/剧本、悬念钩子，支持制作前微调 |
| **生成剧本可选参数** | 生成剧本前可填题材、风格，传入 LLM 提升生成针对性 |
| **开始制作加载提示** | 点击「开始制作」时按钮显示「正在生成分镜…」，避免误以为无响应 |

### 2.28 第二十五轮迭代（2026-03-02）— 短剧创作入口

| 项 | 说明 |
|----|------|
| **创作工作台短剧模式** | 创作工作台增加「短剧创作」模式卡片，点击跳转短剧编辑器 |
| **工作台快速创作** | 短视频工作台快速创作区增加「短剧创作」卡片 |

### 2.29 第二十六轮迭代（2026-03-02）— 短剧创作流程引导

| 项 | 说明 |
|----|------|
| **流程步骤指示** | 短剧详情区展示 5 步流程：创建短剧→添加角色→生成剧本→应用到剧集→开始制作，已完成步骤高亮 |
| **进度统计** | 显示「X 集有剧情 · Y 集已制作」便于了解当前进度 |
| **无剧情提示** | 剧集列表有项但均无剧情时，显示「请先生成剧本并应用到剧集」 |

### 2.30 第二十七轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **短剧删除** | 后端 deleteDrama 软删除（短剧 + 剧集 + 角色）；前端短剧详情区「删除短剧」按钮 + 二次确认弹窗 |
| **应用成功提示** | 应用到剧集成功后，提示改为「可点击任意剧集的「开始制作」进入分镜设计，或点击「已关联项目」跳转已有项目」 |

### 2.31 第二十八轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **从分镜页返回短剧** | 后端 getDramaByProjectId：根据 projectId 查询 sv_drama_episode.project_id 获取关联短剧；POST /drama/by-project |
| **分镜页面包屑** | 项目来自短剧时，面包屑为「创作工作台 > 短剧《xxx》 > 分镜设计（第X集）」，点击短剧名可返回短剧编辑器 |
| **短剧编辑器 URL 参数** | 支持 ?dramaId=123 预选短剧，便于从分镜页返回时自动定位 |

### 2.32 第二十九轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **短剧基本信息编辑** | 后端 updateDrama：支持修改标题、简介、类型、计划集数；POST /drama/update |
| **前端编辑入口** | 短剧详情区「编辑短剧」按钮 + 弹窗表单，与新建短剧字段一致 |

### 2.33 第三十轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **角色删除** | 后端 deleteCharacter 软删除；POST /drama/delete-character |
| **前端角色删除** | 角色列表每行右侧「删除」按钮，删除时显示「删除中…」 |

### 2.34 第三十一轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **选择短剧同步 URL** | 点击左侧短剧列表时，URL 更新为 ?dramaId=xxx，便于刷新/分享保持选中状态 |
| **无效选中清空** | 当选中的短剧不在列表中（如已删除）时，自动清空选择并同步 URL |
| **删除/新建后 URL 同步** | 删除短剧后 navigate 至 /drama；新建短剧后自动选中并同步 ?dramaId=xxx |

### 2.35 第三十二轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **角色删除二次确认** | 点击「删除」角色时弹出确认弹窗，避免误删 |

### 2.36 第三十三轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **剧集删除** | 后端 deleteEpisode 软删除；仅允许删除未关联项目(projectId 为空)的剧集 |
| **前端剧集删除** | 展开剧集后，未关联项目的剧集显示「删除」按钮（与编辑并列） |

### 2.37 第三十四轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **左侧短剧列表进度** | listDramas 返回每部短剧的 episodesWithSynopsis、episodesWithProject |
| **列表项副标题** | 显示「类型 · 集数 · X有剧情 Y已制作」，便于快速了解各短剧进度 |

### 2.38 第三十五轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **剧集删除二次确认** | 点击「删除」剧集时弹出确认弹窗，避免误删 |

### 2.39 第三十六轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **角色编辑** | 后端 updateCharacter：支持修改角色名、描述；POST /drama/update-character |
| **前端角色编辑** | 角色列表每行「编辑」按钮 + 弹窗表单 |

### 2.40 第三十七轮迭代（2026-03-02）— 短剧创作完整性（续）

| 项 | 说明 |
|----|------|
| **生成剧本加载态** | 点击「AI 生成剧本」时按钮显示「生成中…」，与「开始制作」一致，避免误以为无响应 |

---

## 三、待后续迭代项

### P0（高优）

| # | 问题 | 建议 |
|---|------|------|
| 1 | 工作流任务状态持久化不足 | Redis 不可用时内存回退，重启丢失。改为 DB 或强制 Redis |
| 2 | 工作流 publish 占位 | 仅 sleep，需接入真实发布流程 |

### P1（重要）

| # | 问题 | 建议 |
|---|------|------|
| 3 | aiAssistNode 占位 | 已接入 LLM，可继续优化 |
| 4 | 视频同步接口缺失 | 实现 POST /videos/sync，从抖音拉取 |
| 5 | ~~发布时间推荐~~ | ✓ 已实现 POST /content/publish-time-recommend |
| 6 | ~~内容日历~~ | ✓ 已实现 POST /content/calendar、/content/calendar-stats |
| 7 | ~~SvProject DataScope~~ | ✓ 已实现 |

### P2（优化）

| # | 问题 | 建议 |
|---|------|------|
| 1 | ~~auth_resource 补充~~ | ✓ 已实现 migration-feedback-content-resources.sql |
| 2 | ~~接口文档统一~~ | ✓ 已补充附录 A/B |
| 3 | ~~合并重复接口~~ | ✓ script/analyze-viral 为主入口，data/analyze-viral 标注为别名 |

---

## 四、迁移执行记录

```bash
# 2026-03-02 已执行（Windows PowerShell）
docker run --rm -v "c:/claude/dy01:/workspace" -w /workspace --network host `
  -e PGPASSWORD=postgresql postgres:15-alpine `
  psql -h host.docker.internal -p 5433 -U postgres -d douyin_operations `
  -f sql/shortvideo/migration-feedback-content-resources.sql
```

---

**报告版本**：v1.0  
**依据**：`docs/modules/shortvideo/*`、`sql/shortvideo/*`、`src/.../shortvideo/**`
