> ⚠️ **本文档已废弃** — 内容已拆分为 9 文件标准结构（00-大纲.md ~ 08-测试与验收.md），请以拆分文件为准。本文件仅保留作为历史参考。

# shortvideo 模块设计文档

> 版本：2.2 | 更新日期：2026-02-24 | 阶段：P1

---

## 一、需求分析（Requirements Analysis）

### 1.1 模块定位

shortvideo 模块是平台的核心业务模块之一，围绕用户绑定的抖音账号，提供短视频全生命周期管理：从数据分析、选题策划、AI 创作、AI 视频制作到发布复盘。所有 AI 能力通过调用 ai 模块实现，人设信息来自 douyin 模块，生成的图片/视频/音频存储到 storage 模块。

### 1.2 用户故事（User Stories）

| 编号 | 角色 | 故事 | 验收条件 |
|------|------|------|----------|
| SV-01 | 达人 | 我要能看到我抖音号下所有视频的数据 | 同步视频列表，展示播放/点赞/评论/分享数据 |
| SV-02 | 达人 | 我要能分析单个视频的表现 | AI 分析视频数据，给出评级和改进建议 |
| SV-03 | 达人 | 我要能查看视频数据趋势 | 按时间维度展示数据变化曲线 |
| SV-04 | 达人 | 我要能浏览爆款视频库 | 按领域/标签/热度筛选爆款视频 |
| SV-05 | 达人 | 我要能复刻爆款视频 | 选爆款 → AI 按我的人设生成复刻方案 |
| SV-06 | 达人 | 我要能按热门话题策划视频 | 浏览热点 → AI 推荐选题 → 生成创作方案 |
| SV-07 | 达人 | 我要能自主选题让 AI 帮我创作 | 输入主题 → AI 按人设生成文案和拍摄脚本 |
| SV-08 | 达人 | 我要能管理我的创作方案 | 方案列表、草稿/进行中/已完成状态管理 |
| SV-09 | 达人 | 我要能看到视频发布后的复盘分析 | AI 对比同类/历史数据，推荐改进方向 |
| SV-10 | 达人 | 我要能使用系统脚本模板 | 浏览系统预设脚本模板，快速开始创作 |
| SV-11 | 达人 | 我要能创建自己的脚本模板 | 把好的脚本结构保存为个人模板复用 |
| SV-12 | 达人 | 我要能上传人像让 AI 生成视频 | 上传人像照片 → AI 按分镜生成视频画面 |
| SV-13 | 达人 | 我要能上传背景图让 AI 合成画面 | 上传背景图 + 人像 → AI 合成分镜画面 |
| SV-14 | 达人 | 我要能让 AI 按分镜生成首尾帧图片 | AI 根据脚本分镜描述生成关键帧图片 |
| SV-15 | 达人 | 我要能让 AI 从关键帧生成视频片段 | AI 根据首尾帧图片生成过渡视频 |
| SV-16 | 达人 | 我要能让 AI 把文案生成语音 | 分镜话术文本 → TTS 生成配音 |
| SV-17 | 达人 | 我要能让 AI 自动剪辑成片 | 视频片段 + 配音 → AI 剪辑成完整视频 |
| SV-18 | 管理员 | 我要能管理爆款视频库 | 爆款视频 CRUD，按领域分类 |
| SV-19 | 管理员 | 我要能管理热门话题 | 热门话题 CRUD，状态管理 |
| SV-20 | 管理员 | 我要能管理脚本模板 | 系统预设脚本模板 CRUD |
| SV-21 | 达人 | 我要每天自动生成详细的拍摄脚本 | 选人设+日期 → AI 生成 1-3 条含运镜/景别/演员指导的完整拍摄脚本 |
| SV-22 | 达人 | 我要能审核和微调 AI 生成的拍摄脚本 | 逐个分镜审核（通过/需修改），填写修改备注 |
| SV-23 | 达人 | 我要能按日期查看和管理每日拍摄计划 | 按日期+人设筛选拍摄脚本列表，查看拍摄状态 |
| SV-24 | 达人 | 我要能导出可打印的拍摄脚本 | 导出格式化的脚本，摄影师拿到后可直接执行拍摄 |

### 1.3 功能清单

```
shortvideo 模块
├── 视频数据
│   ├── 同步绑定账号下的视频列表（调用抖音 API）
│   ├── 视频列表（分页、搜索、筛选）
│   ├── 视频数据看板（播放/点赞/评论/分享趋势图）
│   ├── 单视频详情 + AI 数据分析
│   └── 视频分类标签管理
│
├── 爆款库（平台 + 个人收藏）
│   ├── 爆款视频列表（按领域/标签/热度筛选）
│   ├── 爆款视频详情（含 AI 结构拆解）
│   ├── 收藏爆款到个人库
│   ├── AI 爆款拆解（分析成功要素）
│   └── 管理端：爆款视频 CRUD
│
├── 热门话题
│   ├── 热门话题列表（按分类浏览）
│   ├── AI 推荐选题（基于人设和热点）
│   └── 管理端：热门话题 CRUD
│
├── 脚本模板
│   ├── 系统模板列表（管理员维护，按行业/风格/时长分类）
│   ├── 个人模板列表（用户自己保存的模板）
│   ├── 模板详情（分镜结构预览）
│   ├── 从模板创建方案（一键引用模板结构开始创作）
│   ├── 保存为模板（把好的方案脚本保存为个人模板）
│   └── 管理端：系统脚本模板 CRUD
│
├── 视频策划
│   ├── 视频类型选择（广告素材/IP号内容/软广视频）
│   ├── 推广方式选择（千川/随心推/自然流/不推广）
│   ├── 爆款复刻（选爆款 → AI 按人设改编 → 生成方案）
│   ├── 热门策划（选热点 → AI 按人设生成方案）
│   ├── 自主创作（输入主题 → AI 按人设生成文案 + 脚本）
│   ├── 方案管理（草稿/进行中/已完成）
│   └── 方案详情（文案 + 分镜 + 拍摄要点）
│
├── AI 视频制作（方案内的分镜级制作流水线）
│   ├── 素材上传
│   │   ├── 上传人像照片（用于 AI 合成视频画面）
│   │   └── 上传背景图片（用于场景合成）
│   ├── AI 生成关键帧
│   │   ├── 按分镜描述生成首帧图片（调用 ai 模块图像生成）
│   │   ├── 按分镜描述生成尾帧图片
│   │   └── 结合人像+背景合成关键帧
│   ├── AI 生成视频片段
│   │   ├── 根据首尾帧图片生成过渡视频（调用 ai 模块视频生成）
│   │   └── 视频片段存储到云存储
│   ├── AI 生成配音
│   │   ├── 分镜话术文本 → TTS 生成语音（调用 ai 模块 TTS）
│   │   └── 音频存储到云存储
│   ├── AI 剪辑成片
│   │   ├── 视频片段 + 配音 → AI 自动剪辑（调用 ai 模块剪辑）
│   │   └── 成片存储到云存储
│   └── 制作进度管理（每个分镜的制作状态追踪）
│
├── 每日拍摄脚本（真人拍摄场景）
│   ├── AI 每日脚本生成（选人设+日期 → AI 生成 1-3 条详细拍摄脚本）
│   ├── 拍摄脚本详情（运镜/景别/构图/灯光/演员指导/道具/场地/转场/摄影提示）
│   ├── 分镜审核流程（逐镜审核：通过/需修改 + 审核备注）
│   ├── 拍摄排期管理（按日期/人设查看脚本计划）
│   ├── 拍摄状态追踪（未开始/可拍摄/拍摄中/拍摄完成）
│   └── 脚本导出打印（生成摄影师可直接执行的打印格式）
│
└── 复盘分析
    ├── 发布后数据追踪
    ├── AI 复盘报告（vs 同类/vs 历史）
    └── 改进建议
```

### 1.4 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 视频数据同步 | 通过抖音开放平台 API 同步，每次同步最近 30 天数据 |
| BR-02 | 同步频率 | 自动每 6 小时同步一次，用户可手动触发 |
| BR-03 | 爆款定义 | 播放量 > 100 万或点赞率 > 5% 可标记为爆款（可配置） |
| BR-04 | 方案状态 | 草稿 → 进行中 → 已完成，支持从任意状态重新编辑 |
| BR-05 | AI 创作需人设 | 所有 AI 创作功能需要用户有激活的人设，且人设必须属于当前用户（owner_id 校验） |
| BR-06 | 数据隔离 | 用户只能看到自己的视频数据和创作方案 |
| BR-07 | 爆款库权限 | 平台爆款库所有登录用户可浏览，管理端维护 |
| BR-08 | 收藏上限 | 免费用户最多收藏 50 个爆款，专业版无限 |
| BR-09 | 脚本模板类型 | system（系统预设，管理员维护，所有用户可见）/ user（个人模板，仅自己可见） |
| BR-10 | AI 视频制作流程 | 必须按顺序：先生成关键帧 → 再生成视频片段 → 再生成配音 → 最后剪辑成片 |
| BR-11 | 生成资产存储 | AI 生成的图片/视频/音频通过 storage 模块上传到云存储，URL 记录在 sv_plan_asset 中 |
| BR-12 | 分镜编号 | 每个方案的分镜从 1 开始顺序编号，支持增删后重排序 |
| BR-13 | 视频业务分类 | 广告素材（ad_material）：用于千川投放的 KOC 素材；IP号内容（ip_content）：人设号/IP号的自然内容；软广视频（soft_ad）：可千川/随心推推广，也可自然流爆发 |
| BR-14 | 推广方式影响文案 | 不同推广方式影响 AI 文案生成策略：千川素材偏转化导向，自然流偏内容吸引力 |
| BR-15 | 违规检测按场景 | 短视频文案的违规检测使用 scope=video，只匹配 all 和 video_only 的违规词 |
| BR-16 | AI 制作断点续传 | AI 视频制作中某个分镜失败不影响其他分镜，已成功的分镜保留结果。用户可对失败的分镜单独重试（produce_status=failed → 重新提交该分镜） |
| BR-17 | 资产版本控制 | sv_plan_asset 每次重新生成会新增记录（version+1），保留最近 5 个版本，超出自动删除最早版本 |
| **每日拍摄脚本规则** | | |
| BR-18 | 每日脚本配额 | 每个人设每天最多 3 条拍摄脚本（单次可生成 1-3 条） |
| BR-19 | 知识库增强 | 生成脚本时自动检索知识库中的同行业创作技巧作为上下文（调用 ai 模块 knowledge/query，关键词=行业+风格） |
| BR-20 | 脚本详细度 | 每条脚本至少 3 个分镜，每个分镜必须包含：景别、运镜方式、画面描述、台词/旁白 |
| BR-21 | 摄影师微调 | 分镜所有字段均可通过 scene/save 接口编辑（复用现有接口 27），修改后 review_status 重置为 pending |
| BR-22 | 审核流程 | review_status 流转：pending → approved/needs_revision。全部分镜 approved 后方案 shoot_status 自动改为 ready |
| BR-23 | 排期约束 | schedule_date 不可早于当天 |
| BR-24 | 拍摄状态仅限 daily_shoot | shoot_status 字段仅 plan_type=daily_shoot 的方案使用 |

### 1.5 模块依赖

```
依赖关系：
auth ← douyin（账号/人设）← shortvideo
ai（AI生成能力：文案/脚本/图像生成/视频生成/TTS/剪辑）← shortvideo
storage（人像/背景/关键帧/视频/音频/成片等文件）← shortvideo
```

---

## 二、数据库设计（Database Design）

### 2.1 ER 关系

```
dy_account ──→ sv_video (一对多，通过 account_id)
                  │
             sv_video_data (一对多，每日快照)

sv_viral_video (爆款库，平台级)
    │
sv_viral_favorite (用户收藏，多对多)

sv_hot_topic (热门话题，平台级)

sv_script_template (脚本模板，系统+个人)

auth_user ──→ sv_plan (一对多，创作方案)
                  │
             sv_plan_scene (一对多，分镜列表)
                  │
             sv_plan_asset (一对多，生成资产)
```

### 2.2 表结构

#### sv_video — 视频表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID |
| account_id | BIGINT | NOT NULL | — | 关联的抖音账号 ID |
| item_id | VARCHAR(128) | NOT NULL | — | 抖音视频 ID |
| title | VARCHAR(256) | | — | 视频标题 |
| cover_url | VARCHAR(512) | | — | 封面 URL |
| video_url | VARCHAR(512) | | — | 视频播放 URL |
| duration | INTEGER | | 0 | 视频时长（秒） |
| tags | VARCHAR(512) | | — | 标签（JSON 数组） |
| category | VARCHAR(64) | | — | 用户自定义分类 |
| publish_time | TIMESTAMP | | — | 发布时间 |
| play_count | BIGINT | | 0 | 播放量（最新） |
| digg_count | BIGINT | | 0 | 点赞数（最新） |
| comment_count | BIGINT | | 0 | 评论数（最新） |
| share_count | BIGINT | | 0 | 分享数（最新） |
| last_sync_time | TIMESTAMP | | — | 最后同步时间 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(account_id, item_id) WHERE deleted=0`、`(owner_id, publish_time DESC)`

#### sv_video_data — 视频数据快照表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| video_id | BIGINT | NOT NULL | — | 关联 sv_video.id |
| snapshot_date | DATE | NOT NULL | — | 快照日期 |
| play_count | BIGINT | | 0 | 当日播放量 |
| digg_count | BIGINT | | 0 | 当日点赞数 |
| comment_count | BIGINT | | 0 | 当日评论数 |
| share_count | BIGINT | | 0 | 当日分享数 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(video_id, snapshot_date)`、`(video_id, snapshot_date DESC)`

#### sv_viral_video — 爆款视频库

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| title | VARCHAR(256) | NOT NULL | — | 视频标题 |
| author_name | VARCHAR(128) | | — | 作者名称 |
| author_id | VARCHAR(128) | | — | 作者抖音号 |
| cover_url | VARCHAR(512) | | — | 封面 URL |
| video_url | VARCHAR(512) | | — | 视频链接 |
| duration | INTEGER | | 0 | 时长（秒） |
| industry_id | BIGINT | | — | 所属行业 ID（关联 sys_industry.id） |
| tags | VARCHAR(512) | | — | 标签（JSON 数组） |
| play_count | BIGINT | | 0 | 播放量 |
| digg_count | BIGINT | | 0 | 点赞数 |
| comment_count | BIGINT | | 0 | 评论数 |
| share_count | BIGINT | | 0 | 分享数 |
| viral_reason | TEXT | | — | 爆款原因分析（AI 生成或人工填写） |
| structure_analysis | TEXT | | — | 结构拆解（AI 生成，JSON 格式） |
| publish_time | TIMESTAMP | | — | 原视频发布时间 |
| status | INTEGER | NOT NULL | 1 | 1=展示 0=隐藏 |
| sort_order | INTEGER | | 0 | 排序 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(industry_id, status, sort_order)`、`(play_count DESC)`

#### sv_viral_favorite — 爆款收藏表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| user_id | BIGINT | NOT NULL | — | 用户 ID |
| viral_video_id | BIGINT | NOT NULL | — | 爆款视频 ID |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(user_id, viral_video_id)`

#### sv_hot_topic — 热门话题表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| topic_title | VARCHAR(128) | NOT NULL | — | 话题标题 |
| topic_desc | VARCHAR(512) | | — | 话题描述 |
| category | VARCHAR(64) | | — | 分类 |
| heat_index | INTEGER | | 0 | 热度指数 |
| source | VARCHAR(64) | | 'manual' | 来源：manual（人工）/ api（API 抓取） |
| expire_time | TIMESTAMP | | — | 过期时间（热点有时效性） |
| status | INTEGER | NOT NULL | 1 | 1=有效 0=过期 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

#### sv_script_template — 脚本模板表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | | — | 所属用户 ID（NULL=系统模板） |
| template_type | VARCHAR(16) | NOT NULL | 'system' | 类型：system（系统预设）/ user（个人模板） |
| template_name | VARCHAR(128) | NOT NULL | — | 模板名称 |
| description | VARCHAR(512) | | — | 模板描述 |
| industry_id | BIGINT | | — | 适用行业 ID（关联 sys_industry.id） |
| style | VARCHAR(64) | | — | 风格（种草/测评/剧情/口播/教程...） |
| duration_hint | VARCHAR(32) | | — | 建议时长（如"30秒""60秒"） |
| scene_count | INTEGER | | 0 | 分镜数量 |
| scene_structure | TEXT | | — | 分镜结构模板（JSON，见下方说明） |
| tags | VARCHAR(512) | | — | 标签（JSON 数组） |
| use_count | INTEGER | | 0 | 使用次数 |
| sort_order | INTEGER | | 0 | 排序（系统模板用） |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(template_type, industry_id, status) WHERE deleted=0`、`(owner_id, create_time DESC) WHERE deleted=0`

**scene_structure 字段格式示例：**
```json
[
  {
    "sceneNo": 1,
    "title": "开头吸引",
    "duration": "0-3秒",
    "description": "用疑问句/惊讶表情/痛点引出，吸引停留",
    "shotType": "近景",
    "script": ""
  },
  {
    "sceneNo": 2,
    "title": "产品展示",
    "duration": "3-15秒",
    "description": "展示产品外观、使用过程",
    "shotType": "中景/特写",
    "script": ""
  },
  {
    "sceneNo": 3,
    "title": "效果对比",
    "duration": "15-25秒",
    "description": "使用前后对比/效果展示",
    "shotType": "特写",
    "script": ""
  },
  {
    "sceneNo": 4,
    "title": "结尾引导",
    "duration": "25-30秒",
    "description": "总结+引导互动（点赞/关注/评论）",
    "shotType": "近景",
    "script": ""
  }
]
```

#### sv_plan — 创作方案表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| owner_id | BIGINT | NOT NULL | — | 所属用户 ID |
| persona_id | BIGINT | | — | 使用的人设 ID |
| plan_type | VARCHAR(32) | NOT NULL | — | 类型：viral_copy（爆款复刻）/ hot_topic（热门策划）/ original（自主创作）/ daily_shoot（每日拍摄脚本） |
| video_type | VARCHAR(32) | | — | 视频业务类型：ad_material（广告素材/千川投放）/ ip_content（IP号/人设号内容）/ soft_ad（软广视频） |
| promotion_type | VARCHAR(32) | | — | 推广方式：qianchuan（千川推广）/ suixintui（随心推）/ organic（自然流量）/ none（不推广） |
| title | VARCHAR(256) | NOT NULL | — | 方案标题 |
| source_id | BIGINT | | — | 来源 ID（爆款复刻时为 viral_video_id，热门策划时为 hot_topic_id） |
| template_id | BIGINT | | — | 使用的脚本模板 ID |
| topic | VARCHAR(256) | | — | 创作主题 |
| style | VARCHAR(64) | | — | 风格 |
| target_duration | VARCHAR(32) | | — | 目标时长（如"30秒""60秒"） |
| ai_copywriting | TEXT | | — | AI 生成的文案内容 |
| ai_script | TEXT | | — | AI 生成的拍摄脚本（JSON，分镜列表） |
| ai_title_tags | TEXT | | — | AI 推荐的标题和话题标签（JSON） |
| ai_analysis | TEXT | | — | AI 爆款分析（爆款复刻时） |
| portrait_url | VARCHAR(512) | | — | 用户上传的人像照片 URL |
| background_urls | TEXT | | — | 用户上传的背景图片 URL 列表（JSON 数组） |
| final_video_url | VARCHAR(512) | | — | AI 剪辑成片 URL |
| user_notes | TEXT | | — | 用户自己的备注 |
| plan_status | VARCHAR(16) | NOT NULL | 'draft' | 状态：draft / in_progress / producing / completed |
| schedule_date | DATE | | — | 计划拍摄日期（每日脚本的排期日期，仅 daily_shoot 类型使用） |
| shoot_status | VARCHAR(16) | | — | 拍摄状态：not_started（未开始）/ ready（可拍摄）/ shooting（拍摄中）/ shot_done（拍摄完成），仅 daily_shoot 类型使用 |
| linked_video_id | BIGINT | | — | 关联的已发布视频 ID（完成后关联） |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(owner_id, plan_status, create_time DESC)`、`(persona_id) WHERE deleted=0`、`(owner_id, schedule_date DESC) WHERE deleted=0 AND plan_type='daily_shoot'`

> 方案状态说明：draft（策划草稿）→ in_progress（文案/脚本确定，准备制作）→ producing（AI 视频制作中）→ completed（成片完成）

> daily_shoot 类型状态机补充：
> - plan_status: draft → in_progress（脚本确认）
> - shoot_status: not_started → ready（全部分镜审核通过）→ shooting → shot_done

#### sv_plan_scene — 方案分镜表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| plan_id | BIGINT | NOT NULL | — | 关联 sv_plan.id |
| scene_no | INTEGER | NOT NULL | — | 分镜编号（从 1 开始） |
| title | VARCHAR(128) | | — | 分镜标题（如"开头吸引""产品展示"） |
| time_range | VARCHAR(32) | | — | 时间段（如"0-3秒"） |
| duration | INTEGER | | 0 | 时长（秒） |
| shot_type | VARCHAR(32) | | — | 景别：close-up（特写）/ medium（中景）/ wide（远景） |
| scene_desc | TEXT | | — | 画面描述（AI 生成关键帧的依据） |
| script_text | TEXT | | — | 话术/旁白文本（TTS 配音的依据） |
| first_frame_url | VARCHAR(512) | | — | 首帧图片 URL |
| last_frame_url | VARCHAR(512) | | — | 尾帧图片 URL |
| video_clip_url | VARCHAR(512) | | — | 视频片段 URL |
| audio_url | VARCHAR(512) | | — | 配音音频 URL |
| camera_movement | VARCHAR(64) | | — | 运镜方式：push（推）/ pull（拉）/ pan（摇）/ dolly（移）/ follow（跟）/ rise（升）/ descend（降）/ static（固定）/ handheld（手持）/ aerial（航拍） |
| camera_angle | VARCHAR(32) | | — | 拍摄角度：eye_level（平拍）/ high_angle（俯拍）/ low_angle（仰拍）/ dutch（倾斜）/ over_shoulder（过肩） |
| composition | VARCHAR(128) | | — | 构图说明（如"三分法居中""对角线构图"） |
| lighting_note | VARCHAR(256) | | — | 灯光要求（如"自然光从左侧45度""补光灯+环形灯"） |
| actor_direction | TEXT | | — | 出镜人指导（表情、动作、走位、情绪） |
| props | VARCHAR(512) | | — | 该分镜所需道具 |
| location | VARCHAR(128) | | — | 拍摄场景/环境（如"白色桌面""卧室窗前"） |
| wardrobe | VARCHAR(256) | | — | 服装造型要求 |
| bgm_note | VARCHAR(128) | | — | 背景音乐建议 |
| transition | VARCHAR(64) | | — | 转场方式：cut（硬切）/ dissolve（淡入淡出）/ whip_pan（甩镜）/ zoom（缩放转场）/ match_cut（匹配剪辑） |
| shooting_tip | TEXT | | — | 摄影师注意事项/拍摄技巧提示 |
| review_status | VARCHAR(16) | | 'pending' | 审核状态：pending（待审）/ approved（通过）/ needs_revision（需修改），仅 daily_shoot 类型使用 |
| reviewer_note | TEXT | | — | 审核/修改备注（摄影师或负责人填写） |
| produce_status | VARCHAR(16) | | 'pending' | 制作状态：pending / frame_done / video_done / audio_done / completed |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(plan_id, scene_no) WHERE deleted=0`

> produce_status 说明：pending（待制作）→ frame_done（关键帧已生成）→ video_done（视频片段已生成）→ audio_done（配音已生成）→ completed（该分镜完成）

> 拍摄执行字段说明：camera_movement ~ reviewer_note 共 13 个字段为可选字段，仅 daily_shoot 类型方案会使用，不影响现有 AI 视频制作流程（viral_copy/hot_topic/original 类型方案仍使用原有字段）。

#### sv_plan_asset — 方案生成资产表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| plan_id | BIGINT | NOT NULL | — | 关联 sv_plan.id |
| scene_id | BIGINT | | — | 关联 sv_plan_scene.id（NULL 表示方案级资产，如成片） |
| asset_type | VARCHAR(32) | NOT NULL | — | 资产类型：portrait（人像）/ background（背景）/ first_frame / last_frame / video_clip / audio / final_video |
| file_url | VARCHAR(512) | NOT NULL | — | 文件 URL（云存储地址） |
| file_id | BIGINT | | — | 关联 sys_file.id |
| file_size | BIGINT | | 0 | 文件大小（字节） |
| duration | INTEGER | | 0 | 时长（秒，视频/音频用） |
| ai_params | TEXT | | — | AI 生成时的参数（JSON，用于重新生成） |
| version | INTEGER | | 1 | 版本号（同一类型可多次生成） |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(plan_id, asset_type, create_time DESC)`、`(scene_id, asset_type)`

---

## 三、接口设计（API Design）

### 3.1 接口总表

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| **视频数据** | | | | |
| 1 | POST | /api/v1/sv/video/sync | 登录 | 手动触发视频数据同步 |
| 2 | POST | /api/v1/sv/video/list | 登录 | 我的视频列表（分页） |
| 3 | POST | /api/v1/sv/video/get | 登录 | 视频详情 |
| 4 | POST | /api/v1/sv/video/data-trend | 登录 | 视频数据趋势（图表数据） |
| 5 | POST | /api/v1/sv/video/dashboard | 登录 | 视频总览看板 |
| 6 | POST | /api/v1/sv/video/ai-analysis | 登录 | AI 分析单个视频 |
| **爆款库** | | | | |
| 7 | POST | /api/v1/sv/viral/list | 登录 | 爆款视频列表（分页、筛选） |
| 8 | POST | /api/v1/sv/viral/get | 登录 | 爆款视频详情（含 AI 拆解） |
| 9 | POST | /api/v1/sv/viral/favorite | 登录 | 收藏/取消收藏爆款 |
| 10 | POST | /api/v1/sv/viral/my-favorites | 登录 | 我的收藏列表 |
| 11 | POST | /api/v1/sv/viral/ai-analyze | 登录 | AI 拆解爆款视频 |
| **热门话题** | | | | |
| 12 | POST | /api/v1/sv/topic/list | 登录 | 热门话题列表 |
| 13 | POST | /api/v1/sv/topic/ai-recommend | 登录 | AI 推荐选题（基于人设） |
| **脚本模板** | | | | |
| 14 | POST | /api/v1/sv/template/list | 登录 | 脚本模板列表（系统+个人） |
| 15 | POST | /api/v1/sv/template/get | 登录 | 模板详情 |
| 16 | POST | /api/v1/sv/template/save-user | 登录 | 保存个人模板（从方案保存/手动创建） |
| 17 | POST | /api/v1/sv/template/delete-user | 登录 | 删除个人模板 |
| **创作方案** | | | | |
| 18 | POST | /api/v1/sv/plan/list | 登录 | 我的方案列表 |
| 19 | POST | /api/v1/sv/plan/get | 登录 | 方案详情（含分镜和资产） |
| 20 | POST | /api/v1/sv/plan/save | 登录 | 新增/编辑方案 |
| 21 | POST | /api/v1/sv/plan/delete | 登录 | 删除方案 |
| 22 | POST | /api/v1/sv/plan/update-status | 登录 | 更新方案状态 |
| 23 | POST | /api/v1/sv/plan/generate-copy | 登录 | AI 生成文案（爆款复刻/热门/自主） |
| 24 | POST | /api/v1/sv/plan/generate-script | 登录 | AI 生成拍摄脚本（含分镜） |
| 25 | POST | /api/v1/sv/plan/generate-title | 登录 | AI 生成标题和话题标签 |
| **分镜管理** | | | | |
| 26 | POST | /api/v1/sv/scene/list | 登录 | 方案分镜列表 |
| 27 | POST | /api/v1/sv/scene/save | 登录 | 新增/编辑分镜 |
| 28 | POST | /api/v1/sv/scene/delete | 登录 | 删除分镜 |
| 29 | POST | /api/v1/sv/scene/reorder | 登录 | 分镜重排序 |
| **AI 视频制作** | | | | |
| 30 | POST | /api/v1/sv/produce/upload-portrait | 登录 | 上传人像照片 |
| 31 | POST | /api/v1/sv/produce/upload-background | 登录 | 上传背景图片 |
| 32 | POST | /api/v1/sv/produce/generate-frames | 登录 | AI 生成分镜关键帧（首帧+尾帧） |
| 33 | POST | /api/v1/sv/produce/generate-video | 登录 | AI 根据关键帧生成视频片段 |
| 34 | POST | /api/v1/sv/produce/generate-audio | 登录 | AI TTS 生成分镜配音 |
| 35 | POST | /api/v1/sv/produce/generate-final | 登录 | AI 剪辑合成最终成片 |
| 36 | POST | /api/v1/sv/produce/status | 登录 | 查询制作进度 |
| **复盘** | | | | |
| 37 | POST | /api/v1/sv/review/generate | 登录 | AI 生成视频复盘报告 |
| **管理端** | | | | |
| 38 | POST | /api/v1/sv/admin/viral/list | 管理员 | 爆款视频管理列表 |
| 39 | POST | /api/v1/sv/admin/viral/save | 管理员 | 新增/编辑爆款视频 |
| 40 | POST | /api/v1/sv/admin/viral/delete | 管理员 | 删除爆款视频 |
| 41 | POST | /api/v1/sv/admin/topic/list | 管理员 | 热门话题管理列表 |
| 42 | POST | /api/v1/sv/admin/topic/save | 管理员 | 新增/编辑热门话题 |
| 43 | POST | /api/v1/sv/admin/topic/delete | 管理员 | 删除热门话题 |
| 44 | POST | /api/v1/sv/admin/template/list | 管理员 | 系统脚本模板管理列表 |
| 45 | POST | /api/v1/sv/admin/template/save | 管理员 | 新增/编辑系统脚本模板 |
| 46 | POST | /api/v1/sv/admin/template/delete | 管理员 | 删除系统脚本模板 |
| **每日拍摄脚本** | | | | |
| 47 | POST | /api/v1/sv/plan/generate-daily | 登录 | AI 生成每日拍摄脚本（1-3条） |
| 48 | POST | /api/v1/sv/plan/daily-list | 登录 | 每日脚本列表（按日期/人设筛选） |
| 49 | POST | /api/v1/sv/scene/review | 登录 | 分镜审核（标记状态+填写备注） |
| 50 | POST | /api/v1/sv/plan/update-shoot-status | 登录 | 更新拍摄状态 |
| 51 | POST | /api/v1/sv/plan/export-script | 登录 | 导出拍摄脚本（可打印格式） |

### 3.2 关键接口详情

#### 接口 23：AI 生成文案（创作方案核心）

```
POST /api/v1/sv/plan/generate-copy
权限：登录（检查 AI 额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | Long | 是 | 方案 ID |
| planType | String | 是 | viral_copy / hot_topic / original |
| sourceId | Long | 否 | 爆款 ID 或话题 ID（planType 为 viral_copy/hot_topic 时必填） |
| topic | String | 否 | 自主创作时的主题 |
| style | String | 否 | 风格偏好 |
| duration | String | 否 | 目标时长（如"60秒"） |

**业务逻辑：**
1. 获取用户当前激活人设（douyin 模块）
2. 如果是爆款复刻：获取爆款视频详情，调用 ai 模块 `video_plan_viral` 模板
3. 如果是热门策划：获取话题信息，调用 ai 模块 `video_copywriting` 模板
4. 如果是自主创作：直接调用 ai 模块 `video_copywriting` 模板
5. AI 生成结果写入 sv_plan.ai_copywriting
6. 返回生成结果

**响应：**

```json
{
  "status": 200,
  "data": {
    "planId": 1,
    "aiCopywriting": "【AI 生成的文案内容】...",
    "quotaRemaining": 7
  }
}
```

---

#### 接口 32：AI 生成分镜关键帧

```
POST /api/v1/sv/produce/generate-frames
权限：登录（检查 AI 额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | Long | 是 | 方案 ID |
| sceneId | Long | 是 | 分镜 ID |
| portraitUrl | String | 否 | 人像照片 URL（已上传） |
| backgroundUrl | String | 否 | 背景图片 URL（已上传） |

**业务逻辑：**
1. 获取分镜详情（scene_desc、shot_type 等）
2. 组装图像生成 prompt：分镜描述 + 人像 + 背景 + 景别
3. 调用 ai 模块 `video_frame_generate` 模板，生成首帧图片
4. 调用 ai 模块生成尾帧图片
5. 图片通过 storage 模块上传到云存储
6. 更新 sv_plan_scene 的 first_frame_url、last_frame_url
7. 记录到 sv_plan_asset
8. 更新分镜 produce_status → frame_done

**响应：**

```json
{
  "status": 200,
  "data": {
    "sceneId": 1,
    "firstFrameUrl": "https://xxx.bos.baidu.com/sv/frames/xxx-first.png",
    "lastFrameUrl": "https://xxx.bos.baidu.com/sv/frames/xxx-last.png",
    "quotaRemaining": 5
  }
}
```

---

#### 接口 35：AI 剪辑合成最终成片

```
POST /api/v1/sv/produce/generate-final
权限：登录（检查 AI 额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | Long | 是 | 方案 ID |

**业务逻辑：**
1. 检查所有分镜的视频片段和配音是否都已生成
2. 收集所有分镜的 video_clip_url 和 audio_url（按 scene_no 排序）
3. 调用 ai 模块 `video_edit_final` 剪辑合成接口
4. 成片通过 storage 模块上传到云存储
5. 更新 sv_plan.final_video_url
6. 记录到 sv_plan_asset（asset_type=final_video）
7. 更新方案 plan_status → completed

**响应：**

```json
{
  "status": 200,
  "data": {
    "planId": 1,
    "finalVideoUrl": "https://xxx.bos.baidu.com/sv/final/xxx.mp4",
    "duration": 58,
    "fileSize": 15728640,
    "quotaRemaining": 2
  }
}
```

---

#### 接口 47：AI 生成每日拍摄脚本

```
POST /api/v1/sv/plan/generate-daily
权限：登录（检查 AI 额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| personaId | Long | 是 | 人设 ID |
| scheduleDate | String | 是 | 计划拍摄日期（yyyy-MM-dd） |
| count | Integer | 否 | 生成脚本数量（1-3，默认 1） |
| style | String | 否 | 风格偏好（如"种草""测评""剧情""口播"） |
| duration | String | 否 | 目标时长（如"30秒""60秒"，默认"30秒"） |
| topic | String | 否 | 指定主题（为空则 AI 自动选题） |

**业务逻辑：**
1. 校验人设属于当前用户（owner_id）且为激活状态
2. 校验 scheduleDate ≥ 当天
3. 校验该人设该日期已有脚本数 + count ≤ 3
4. 检查 AI 调用额度
5. 获取人设详情 + 所属账号行业信息
6. 检索知识库获取同行业创作技巧上下文（调用 ai 模块知识检索）
7. 组装 shooting_script_daily Prompt，调用 AI 生成
8. 解析 AI 返回的 JSON，创建 sv_plan（plan_type=daily_shoot）+ sv_plan_scene 记录
9. 每条脚本创建一个 sv_plan，分镜写入 sv_plan_scene（含所有拍摄执行字段）
10. 返回创建的方案 ID 列表

**响应：**

```json
{
  "status": 200,
  "data": {
    "plans": [
      {
        "planId": 101,
        "title": "冬季面霜推荐｜3款回购无限次",
        "sceneCount": 5,
        "scheduleDate": "2026-02-25"
      },
      {
        "planId": 102,
        "title": "洗面奶别乱买！成分党教你选",
        "sceneCount": 4,
        "scheduleDate": "2026-02-25"
      }
    ],
    "quotaRemaining": 5
  }
}
```

---

#### 接口 49：分镜审核

```
POST /api/v1/sv/scene/review
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sceneId | Long | 是 | 分镜 ID |
| reviewStatus | String | 是 | approved / needs_revision |
| reviewerNote | String | 否 | 审核备注 |

**业务逻辑：**
1. 校验方案属于当前用户
2. 更新分镜的 review_status 和 reviewer_note
3. 检查该方案所有分镜是否全部 approved → 若是，方案 shoot_status 自动改为 ready

**响应：**

```json
{
  "status": 200,
  "data": {
    "sceneId": 1,
    "reviewStatus": "approved",
    "allApproved": true,
    "shootStatus": "ready"
  }
}
```

---

## 四、页面设计（Frontend Design）

### 4.1 页面清单

| # | 页面 | 路径 | 入口 | 说明 |
|---|------|------|------|------|
| 1 | 视频数据看板 | /talent/sv/dashboard | 侧栏菜单 | 视频总览 + 趋势图 |
| 2 | 我的视频 | /talent/sv/videos | 侧栏菜单 | 视频列表 + 详情 |
| 3 | 爆款库 | /talent/sv/viral | 侧栏菜单 | 爆款浏览 + 收藏 |
| 4 | 热门话题 | /talent/sv/topics | 侧栏菜单 | 话题浏览 |
| 5 | 脚本模板 | /talent/sv/templates | 侧栏菜单 | 系统模板 + 我的模板 |
| 6 | 创作工作台 | /talent/sv/plans | 侧栏菜单 | 方案列表 |
| 7 | 方案详情 | /talent/sv/plans/:id | 方案列表点击 | 方案编辑 + AI 生成 |
| 8 | AI 视频制作 | /talent/sv/production/:planId | 方案详情点击 | 分镜级 AI 视频制作 |
| 9 | 机构视频总览 | /org/sv/videos | 机构侧栏 | 旗下达人视频数据汇总（DataScope） |
| 10 | 爆款管理（管理端） | /admin/sv/viral | 管理端菜单 | 爆款 CRUD |
| 11 | 话题管理（管理端） | /admin/sv/topics | 管理端菜单 | 话题 CRUD |
| 12 | 脚本模板管理（管理端） | /admin/sv/templates | 管理端菜单 | 系统脚本模板 CRUD |
| **每日拍摄脚本** | | | | |
| 12 | 每日拍摄计划 | /talent/sv/daily-shoot | 侧栏菜单 | 日历视图 + 脚本列表 |
| 13 | 拍摄脚本详情 | /talent/sv/shoot-script/:id | 每日拍摄列表点击 | 摄影师视角的脚本查看/编辑 |

### 4.2 关键页面设计

#### 页面 5：脚本模板

```
┌────────────────────────────────────────────────────────┐
│ 脚本模板                                                │
├────────────────────────────────────────────────────────┤
│                                                        │
│  [系统模板] [我的模板]              [新建模板]           │
│                                                        │
│  筛选: 行业[全部▼] 风格[全部▼] 时长[全部▼] [搜索___]   │
│                                                        │
│  ┌────────────────┐  ┌────────────────┐                │
│  │ 30秒种草口播    │  │ 60秒产品测评    │                │
│  │ 美妆 · 种草     │  │ 通用 · 测评     │                │
│  │ 4个分镜         │  │ 6个分镜         │                │
│  │ 已使用 128 次   │  │ 已使用 85 次    │                │
│  │ [预览] [使用]   │  │ [预览] [使用]   │                │
│  └────────────────┘  └────────────────┘                │
│                                                        │
│  ┌────────────────┐  ┌────────────────┐                │
│  │ 15秒悬念开头    │  │ 45秒剧情带货    │                │
│  │ 通用 · 剧情     │  │ 电商 · 剧情     │                │
│  │ 3个分镜         │  │ 5个分镜         │                │
│  │ 已使用 200 次   │  │ 已使用 56 次    │                │
│  │ [预览] [使用]   │  │ [预览] [使用]   │                │
│  └────────────────┘  └────────────────┘                │
│                                                        │
└────────────────────────────────────────────────────────┘
```

#### 页面 7：方案详情（创作核心页面）

```
┌────────────────────────────────────────────────────────┐
│ ← 返回方案列表           方案：秋冬面霜推荐（爆款复刻）  │
├────────────────────────────────────────────────────────┤
│                                                        │
│  状态: [草稿 ▼]  人设: 美妆博主小美  风格: [种草 ▼]     │
│  脚本模板: 30秒种草口播 [更换]                           │
│                                                        │
│  ── 来源（爆款复刻时显示）──                             │
│  ┌──────────────────────────────────────────────┐      │
│  │ [封面] 原爆款：冬季面霜红黑榜...               │      │
│  │ 播放 320万 点赞 15万 评论 8000 分享 5000      │      │
│  │ [查看AI拆解]                                  │      │
│  └──────────────────────────────────────────────┘      │
│                                                        │
│  ── AI 文案 ──                      [重新生成]          │
│  ┌──────────────────────────────────────────────┐      │
│  │ 版本1：                                       │      │
│  │ 标题：冬天烂脸？这3款面霜闭眼入 #面霜推荐     │      │
│  │ 开头：姐妹们！冬天脸又干又痒的快来...         │      │
│  │ 正文：今天给你们分享3款我回购了无数次的...    │      │
│  │ 结尾：觉得有用的双击点个赞...                 │      │
│  └──────────────────────────────────────────────┘      │
│                                                        │
│  ── AI 拍摄脚本 ──                  [生成脚本]          │
│  ┌──────────────────────────────────────────────┐      │
│  │ 分镜1 (0-3s)：近景，手持面霜，表情惊讶       │      │
│  │ 分镜2 (3-8s)：中景，展示产品包装...           │      │
│  │ ...                                           │      │
│  └──────────────────────────────────────────────┘      │
│                                                        │
│  ── AI 标题 & 话题标签 ──           [生成标题]          │
│  ┌──────────────────────────────────────────────┐      │
│  │ 推荐标题：冬天烂脸？这3款面霜闭眼入           │      │
│  │ 推荐标签：#面霜推荐 #护肤 #冬季护肤 #平价好物 │      │
│  └──────────────────────────────────────────────┘      │
│                                                        │
│  ── 我的备注 ──                                        │
│  [多行文本框____________________________________]       │
│                                                        │
│  [保存草稿] [保存为模板] [进入AI制作 →]                  │
└────────────────────────────────────────────────────────┘
```

#### 页面 8：AI 视频制作（分镜级制作工作台）

```
┌────────────────────────────────────────────────────────┐
│ ← 返回方案         AI 视频制作：秋冬面霜推荐            │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ── 素材准备 ──                                        │
│  人像照片: [photo.jpg] [重新上传]                       │
│  背景图片: [bg1.jpg] [bg2.jpg] [+上传]                  │
│                                                        │
│  ── 分镜制作（共4个分镜）──           [一键全部生成]      │
│                                                        │
│  ┌ 分镜1 (0-3s) 开头吸引 ─── 状态: ✅ 已完成 ──────┐   │
│  │                                                   │   │
│  │  画面描述: 近景，手持面霜，表情惊讶               │   │
│  │  话术文本: 姐妹们！冬天脸干到起皮的看这里！       │   │
│  │                                                   │   │
│  │  首帧         尾帧         视频片段    配音        │   │
│  │  [🖼 img]    [🖼 img]    [▶ 3s]    [🔊 3s]     │   │
│  │  [重新生成]  [重新生成]  [重新生成]  [重新生成]   │   │
│  └───────────────────────────────────────────────┘   │
│                                                        │
│  ┌ 分镜2 (3-8s) 产品展示 ─── 状态: 🔄 视频生成中 ──┐   │
│  │                                                   │   │
│  │  画面描述: 中景，桌上排列3款面霜，逐一拿起展示    │   │
│  │  话术文本: 今天给你们分享3款我回购了无数次的...   │   │
│  │                                                   │   │
│  │  首帧         尾帧         视频片段    配音        │   │
│  │  [🖼 img]    [🖼 img]    [⏳ ...]   [待生成]    │   │
│  │  [重新生成]  [重新生成]                           │   │
│  └───────────────────────────────────────────────┘   │
│                                                        │
│  ┌ 分镜3 ... ────────────────────────────────────┐    │
│  └───────────────────────────────────────────────┘    │
│                                                        │
│  ┌ 分镜4 ... ────────────────────────────────────┐    │
│  └───────────────────────────────────────────────┘    │
│                                                        │
│  ── 合成成片 ──                                        │
│  全部分镜完成后可合成:                                  │
│  [AI 剪辑成片]                                         │
│                                                        │
│  成片预览: [▶ 播放 30s | 15MB]  [下载]                  │
│                                                        │
└────────────────────────────────────────────────────────┘
```

#### 页面 12：每日拍摄计划

```
┌────────────────────────────────────────────────────────────┐
│ 每日拍摄计划                          [AI 生成今日脚本]      │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  人设: [美妆博主小美 ▼]    日期: [2026-02-25 📅]           │
│                                                            │
│  ── 2026-02-25（周三）── 已生成 2/3 条 ─────────────────  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ #1 冬季面霜推荐｜3款回购无限次                         │ │
│  │ 5个分镜 · 预计30秒 · 种草口播                         │ │
│  │ 拍摄状态: 🟢 可拍摄（5/5 分镜已审核）                  │ │
│  │ [查看脚本] [编辑] [导出打印]                           │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ #2 洗面奶别乱买！成分党教你选                         │ │
│  │ 4个分镜 · 预计45秒 · 测评                            │ │
│  │ 拍摄状态: 🟡 待审核（2/4 分镜已审核）                  │ │
│  │ [查看脚本] [编辑] [导出打印]                           │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  [+ 再生成 1 条]                                           │
│                                                            │
│  ── 2026-02-24（周二）── 已完成 ─────────────────────    │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ #1 早C晚A护肤公式 · 3个分镜 · ✅ 已拍摄完成           │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

#### 页面 13：拍摄脚本详情（摄影师视角）

```
┌────────────────────────────────────────────────────────────┐
│ ← 返回         冬季面霜推荐｜3款回购无限次    [导出打印]     │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ── 拍摄准备 ──                                            │
│  📍 场地: 白色干净桌面 + 柔光灯背景                         │
│  👗 服装: 浅色休闲装，妆容干净自然                           │
│  🎵 BGM: 轻快日系BGM / 节奏感轻音乐                        │
│  🎬 道具: 3款面霜产品、化妆镜、手持展示板                    │
│                                                            │
│  ── 分镜脚本 ── (5个分镜)    拍摄状态: [可拍摄 ▼]          │
│                                                            │
│  ┌ 第1镜 (0-3秒) 开头吸引 ── ✅ 已审核 ──────────────┐   │
│  │                                                     │   │
│  │  🎥 景别: 近景    运镜: 手持微晃 → 快速推近到面部    │   │
│  │  📐 角度: 平拍    构图: 人物居中偏左                 │   │
│  │  💡 灯光: 正面柔光 + 左侧45°补光                    │   │
│  │                                                     │   │
│  │  📝 台词:                                           │   │
│  │  "姐妹们！冬天脸干到起皮的赶紧看过来！"              │   │
│  │                                                     │   │
│  │  🎭 表演指导:                                       │   │
│  │  表情夸张惊讶 → 快速转认真脸，语速偏快，              │   │
│  │  右手拿起一瓶面霜在脸旁晃动                          │   │
│  │                                                     │   │
│  │  📎 道具: 面霜1瓶                                   │   │
│  │  🔄 转场: 硬切                                      │   │
│  │  💡 摄影提示: 对焦点锁定在眼睛，稳定器跟随模式       │   │
│  │                                                     │   │
│  │  审核: ✅ 通过                          [编辑]       │   │
│  └─────────────────────────────────────────────────┘   │
│                                                            │
│  ┌ 第2镜 (3-10秒) 产品展示 ── 🟡 待审核 ────────────┐    │
│  │                                                     │   │
│  │  🎥 景别: 中景→特写  运镜: 从桌面全景慢推到产品特写  │   │
│  │  📐 角度: 俯拍30°   构图: 产品三角排列              │   │
│  │  💡 灯光: 顶部柔光箱 + 侧面轮廓光                   │   │
│  │  ...                                                │   │
│  │                                                     │   │
│  │  审核: [✅ 通过] [✏️ 需修改]                        │   │
│  │  备注: [___________________________________]        │   │
│  └─────────────────────────────────────────────────┘   │
│                                                            │
│  ┌ 第3镜 ... ──────────────────────────────────────┐     │
│  └─────────────────────────────────────────────────┘     │
│                                                            │
│  [全部通过] [保存修改]                                      │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 五、开发任务拆解（Task Breakdown）

### 5.1 后端任务

| # | 任务 | 输出 | 依赖 |
|---|------|------|------|
| B1 | 编写 SQL | schema.sql + demo.sql | 无 |
| B2 | Entity 层 | SvVideo, SvVideoData, SvViralVideo, SvViralFavorite, SvHotTopic, SvScriptTemplate, SvPlan, SvPlanScene, SvPlanAsset | B1 |
| B3 | Repository 层 | 9 个 Repository | B2 |
| B4 | VO 层 | 各实体的 SearchVO/SaveVO/VO | B2 |
| B5 | SvVideoService | 视频同步（抖音 API）、列表、数据看板 | B3, B4, douyin |
| B6 | SvViralService | 爆款 CRUD、收藏、AI 拆解 | B3, B4, ai |
| B7 | SvTopicService | 热门话题 CRUD | B3, B4 |
| B8 | SvTemplateService | 脚本模板 CRUD（系统+个人） | B3, B4 |
| B9 | SvPlanService | 方案 CRUD、AI 生成（文案/脚本/标题） | B3, B4, ai, douyin |
| B10 | SvSceneService | 分镜 CRUD、排序 | B3, B4 |
| B11 | SvProduceService | AI 视频制作（关键帧/视频/配音/成片） | B10, ai, storage |
| B12 | SvReviewService | AI 视频分析、复盘报告 | B5, ai |
| B13 | SvVideoController | 用户端视频相关接口 | B5, B12 |
| B14 | SvViralController | 用户端爆款相关接口 | B6 |
| B15 | SvTemplateController | 用户端脚本模板接口 | B8 |
| B16 | SvPlanController | 用户端方案+分镜相关接口 | B9, B10 |
| B17 | SvProduceController | 用户端 AI 视频制作接口 | B11 |
| B18 | SvAdminController | 管理端接口（爆款/话题/模板） | B6, B7, B8 |
| B19 | resource-data.sql | shortvideo 模块资源注册 | B18 |
| **每日拍摄脚本** | | | |
| B20 | SQL 变更 | sv_plan_scene 新增 13 个拍摄字段 + sv_plan 新增 2 个字段 + 新增索引 | 无 |
| B21 | Entity 更新 | SvPlanScene 新增字段 + SvPlan 新增字段 | B20 |
| B22 | VO 更新 | SvPlanSceneSaveVO/VO 新增字段 + SvPlanSaveVO/VO 新增字段 | B21 |
| B23 | AI Prompt 模板注册 | shooting_script_daily + AI 任务映射 shooting_script | 无 |
| B24 | SvPlanService.generateDaily() | 每日脚本生成逻辑（调用 AI、解析 JSON、批量创建方案+分镜） | B22, B23 |
| B25 | SvPlanService.dailyList() | 按日期/人设查询每日脚本列表 | B22 |
| B26 | SvSceneService.review() | 分镜审核逻辑（状态更新 + 联动方案 shoot_status） | B22 |
| B27 | SvPlanController 新增接口 | generate-daily、daily-list、update-shoot-status、export-script | B24, B25, B26 |
| B28 | SvSceneController 新增接口 | review | B26 |
| B29 | resource-data.sql 追加 | 新接口的资源注册 | B27, B28 |

### 5.2 前端任务

| # | 任务 | 依赖 |
|---|------|------|
| F1 | 视频数据看板 | B13 |
| F2 | 我的视频列表 | B13 |
| F3 | 爆款库页面 | B14 |
| F4 | 热门话题页面 | B7 |
| F5 | 脚本模板页面 | B15 |
| F6 | 创作工作台（方案列表） | B16 |
| F7 | 方案详情页 | B16 |
| F8 | AI 视频制作页 | B17 |
| F9 | 爆款管理（管理端） | B18 |
| F10 | 话题管理（管理端） | B18 |
| F11 | 脚本模板管理（管理端） | B18 |
| **每日拍摄脚本** | | |
| F12 | 每日拍摄计划页面（/talent/sv/daily-shoot） | B27 |
| F13 | 拍摄脚本详情页面（/talent/sv/shoot-script/:id） | B27, B28 |

---

## 六、测试用例（Test Cases）

### 6.1 冒烟测试

| # | 场景 | 预期 |
|---|------|------|
| S1 | 视频列表 | 200，返回用户视频列表 |
| S2 | 爆款列表 | 200，返回爆款视频 |
| S3 | 创建方案 | 200，返回方案 ID |
| S4 | AI 生成文案 | 200，返回 AI 文案内容 |
| S5 | 脚本模板列表 | 200，返回系统+个人模板 |
| S6 | AI 生成关键帧 | 200，返回首尾帧图片 URL |

### 6.2 功能测试

| # | 场景 | 预期 |
|---|------|------|
| F01 | 同步视频数据 | 视频列表更新，数据快照写入 |
| F02 | 爆款复刻生成文案 | 基于人设 + 爆款信息生成文案 |
| F03 | 热门话题策划 | 基于人设 + 话题生成方案 |
| F04 | 自主创作文案 | 基于人设 + 主题生成文案 |
| F05 | 收藏爆款 | 收藏记录创建，重复收藏取消 |
| F06 | AI 视频分析 | 返回评级 + 分析 + 改进建议 |
| F07 | 方案状态流转 | 草稿 → 进行中 → 制作中 → 已完成 |
| F08 | 数据隔离 | 用户只能看到自己的数据 |
| F09 | 无人设时 AI 创作 | 提示先创建人设 |
| F10 | 系统模板浏览 | 所有用户可见系统模板 |
| F11 | 个人模板 CRUD | 保存/编辑/删除个人模板，数据隔离 |
| F12 | 从模板创建方案 | 模板的分镜结构正确复制到方案 |
| F13 | 上传人像照片 | 文件上传成功，URL 记录到方案 |
| F14 | 上传背景图片 | 支持多张，URL 记录到方案 |
| F15 | AI 生成分镜关键帧 | 首帧+尾帧图片生成并存储到云，URL 更新到分镜 |
| F16 | AI 生成视频片段 | 基于首尾帧生成视频，存储到云 |
| F17 | AI TTS 配音 | 话术文本生成语音，存储到云 |
| F18 | AI 剪辑成片 | 所有片段+配音合成成片，存储到云 |
| F19 | 分镜制作状态追踪 | 每步完成后 produce_status 正确更新 |
| F20 | 成片前检查 | 未完成所有分镜时无法合成成片 |
| **每日拍摄脚本** | | |
| F21 | 选人设 → 生成1条每日脚本 | 200，创建 sv_plan(daily_shoot) + 3-8 个分镜，每个分镜含运镜/景别/台词 |
| F22 | 生成3条每日脚本 | 200，创建3个方案，选题各不相同 |
| F23 | 同一人设同一天已有3条再生成 | 拒绝，提示当日已达上限 |
| F24 | 无激活人设时生成 | 提示先创建人设 |
| F25 | 按日期查询每日脚本列表 | 返回该日期该人设的所有 daily_shoot 方案 |
| F26 | 审核分镜 → 全部通过 | 分镜 review_status=approved，方案 shoot_status 自动变为 ready |
| F27 | 审核分镜 → 部分需修改 | 方案 shoot_status 保持 not_started |
| F28 | 编辑分镜后 review_status 重置 | 修改任意字段后 review_status 回到 pending |
| F29 | 排期日期早于今天 | 拒绝 |
| F30 | 数据隔离 | 只能看到自己的每日脚本 |
| F31 | 导出脚本 | 生成可打印的脚本格式 |

---

## 七、验收标准（Acceptance Criteria）

### 7.1 功能验收

- [ ] 用户可同步绑定账号下的视频列表和数据
- [ ] 视频数据看板正确展示趋势图
- [ ] 用户可浏览爆款库并收藏
- [ ] 用户可创建爆款复刻/热门策划/自主创作方案
- [ ] AI 可按人设生成文案、脚本、标题标签
- [ ] AI 可分析视频数据并给出改进建议
- [ ] 方案状态管理正常（draft → in_progress → producing → completed）
- [ ] 管理员可管理爆款库和热门话题
- [ ] 用户可浏览系统脚本模板并使用
- [ ] 用户可创建/管理个人脚本模板
- [ ] 管理员可管理系统脚本模板
- [ ] 用户可上传人像和背景图
- [ ] AI 可按分镜描述生成关键帧图片
- [ ] AI 可从关键帧生成视频片段
- [ ] AI 可将话术文本生成配音
- [ ] AI 可将视频片段+配音剪辑成片
- [ ] 所有生成资产正确存储到云存储
- [ ] **每日拍摄脚本：AI 可按人设生成含运镜/景别/演员指导的详细拍摄脚本（1-3条）**
- [ ] **每日拍摄脚本：分镜审核流程正常（pending → approved/needs_revision）**
- [ ] **每日拍摄脚本：全部分镜通过后方案 shoot_status 自动变为 ready**
- [ ] **每日拍摄脚本：按日期/人设查看每日脚本列表**
- [ ] **每日拍摄脚本：脚本可导出为摄影师可执行的打印格式**
- [ ] **每日拍摄脚本：编辑分镜后 review_status 重置为 pending**

### 7.2 技术验收

- [ ] 抖音 API 调用有异常处理和重试
- [ ] 视频数据快照每日自动写入
- [ ] AI 生成调用 ai 模块统一接口，不直接调模型
- [ ] 数据隔离（owner_id）
- [ ] 逻辑删除、事务管理
- [ ] AI 视频制作是异步处理（不阻塞请求）
- [ ] 生成资产有版本管理（支持重新生成）
- [ ] 大文件上传有进度反馈

---

## 八、上线检查清单（Release Checklist）

- [ ] `sql/shortvideo/schema.sql` 已执行
- [ ] `sql/shortvideo/demo.sql` 已执行（含系统脚本模板初始数据）
- [ ] `sql/shortvideo/resource-data.sql` 已执行
- [ ] 抖音开放平台"视频列表"API 权限已申请
- [ ] ai 模块的相关 Prompt 模板已就绪（含 video_frame_generate、video_clip_generate、video_tts、video_edit_final）
- [ ] 云存储空间已配置（用于存储关键帧/视频片段/音频/成片）
- [ ] 对应角色已绑定 shortvideo 模块资源
- [ ] 错误码已注册
- [ ] **每日拍摄脚本：sv_plan_scene 新增 13 个字段 + sv_plan 新增 2 个字段的 SQL 已执行**
- [ ] **每日拍摄脚本：ai 模块 shooting_script_daily Prompt 模板 + shooting_script 任务映射已就绪**
- [ ] **每日拍摄脚本：新增接口（47-51）的资源已注册到 resource-data.sql**
- [ ] 冒烟测试全部通过
