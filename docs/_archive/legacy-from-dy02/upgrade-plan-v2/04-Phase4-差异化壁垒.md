# Phase 4 — 差异化壁垒（持续）

> 前置：Phase 1-3 完成
> 目标：构建竞品难以复制的差异化能力
> 预期：8.2 → 8.5+

---

## 任务 4.1 — 爆款二创模板库

**问题**：无二创能力，爆款分析后无法系统化复用。

**文件**：
- `sql/shortvideo/remake-template-schema.sql` — 新建
- `src/main/resources/db/migration/V025__sv_remake_template.sql` — 新建
- `src/main/java/.../module/shortvideo/entity/SvRemakeTemplate.java` — 新建
- `src/main/java/.../module/shortvideo/repository/SvRemakeTemplateRepository.java` — 新建
- `src/main/java/.../module/shortvideo/service/RemakeTemplateService.java` — 新建
- `src/main/java/.../module/shortvideo/service/impl/RemakeTemplateServiceImpl.java` — 新建
- `src/main/java/.../module/shortvideo/controller/RemakeTemplateController.java` — 新建
- `frontend-react/src/pages/shortvideo/RemakeTemplatePage.tsx` — 新建

**建表 SQL**：
```sql
-- V025__sv_remake_template.sql
CREATE TABLE IF NOT EXISTS sv_remake_template (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL DEFAULT 0,  -- 0=系统预设
    template_name VARCHAR(100) NOT NULL,
    remake_type VARCHAR(32) NOT NULL,     -- form_copy / content_flip / element_remix / dimension_upgrade
    source_viral_id BIGINT,              -- 来源爆款视频ID
    -- 模板结构
    structure_template JSONB NOT NULL,    -- 分段结构模板
    emotion_curve VARCHAR(200),           -- 情绪曲线模板
    bgm_style VARCHAR(64),
    duration_range VARCHAR(32),           -- "15-30" 秒
    -- 二创指导
    adaptation_guide TEXT,                -- 改编指南（如何替换内容保留结构）
    variable_slots JSONB DEFAULT '[]',    -- 可替换的变量槽位 [{slot, description, example}]
    -- 效果统计
    usage_count INTEGER DEFAULT 0,
    avg_viral_score DOUBLE PRECISION,
    -- 分类标签
    content_types JSONB DEFAULT '[]',     -- ["护肤", "彩妆", "测评"]
    tags JSONB DEFAULT '[]',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

### 二创类型定义
```java
// 4 种二创模式：
// 1. form_copy（形式模仿）：保留视频结构和节奏，替换产品和内容
//    - 输入：爆款视频结构 + 新产品信息
//    - 输出：相同结构的新脚本
//
// 2. content_flip（内容翻转）：保留话题，反转观点或角度
//    - 输入：爆款视频主题 + 翻转方向
//    - 输出：反向观点的新脚本
//
// 3. element_remix（元素重组）：提取多个爆款的优秀元素，重新组合
//    - 输入：多个爆款的元素列表
//    - 输出：融合多个爆款元素的新脚本
//
// 4. dimension_upgrade（升维创新）：在爆款基础上增加新维度
//    - 输入：爆款视频 + 新增维度（如专业测评、成分分析）
//    - 输出：升级版脚本
```

### 二创生成服务
```java
public interface RemakeTemplateService {
    // 从爆款分析结果自动生成模板
    SvRemakeTemplate createFromViralAnalysis(Long viralVideoId, String remakeType, Long ownerId);

    // 基于模板生成新脚本
    String generateFromTemplate(Long templateId, Map<String, String> variables, Long ownerId);

    // 推荐适合的二创模板（基于产品类型和目标）
    List<SvRemakeTemplate> recommend(String productType, String contentGoal, int limit);

    // 模板效果统计更新
    void updateTemplateStats(Long templateId, double viralScore);
}
```

### 前端页面
```
二创模板库页面：
┌─────────────────────────────────────────────────┐
│ 二创模板库                          [从爆款生成]  │
├─────────────────────────────────────────────────┤
│ [形式模仿] [内容翻转] [元素重组] [升维创新] [全部] │
├─────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│ │ 模板卡片   │ │ 模板卡片   │ │ 模板卡片   │         │
│ │ 名称       │ │ 名称       │ │ 名称       │         │
│ │ 类型 Chip  │ │ 类型 Chip  │ │ 类型 Chip  │         │
│ │ 使用 42次  │ │ 使用 28次  │ │ 使用 15次  │         │
│ │ 爆款率 73% │ │ 爆款率 65% │ │ 爆款率 58% │         │
│ │ [使用] [详情]│ │ [使用] [详情]│ │ [使用] [详情]│         │
│ └──────────┘ └──────────┘ └──────────┘         │
└─────────────────────────────────────────────────┘
```

**验收**：
1. 可从爆款分析结果一键生成二创模板
2. 4 种二创类型均可正常生成脚本
3. 模板使用次数和爆款率自动统计

---

## 任务 4.2 — BGM 智能匹配引擎（增强版）

**问题**：Phase 2 的 BGM 推荐基于规则，需升级为数据驱动。

**文件**：
- `sql/shortvideo/bgm-library-schema.sql` — 新建
- `src/main/resources/db/migration/V026__sv_bgm_library.sql` — 新建
- `src/main/java/.../module/shortvideo/entity/SvBgmLibrary.java` — 新建
- `src/main/java/.../module/shortvideo/service/impl/BgmRecommendServiceImpl.java` — 升级

**建表 SQL**：
```sql
-- V026__sv_bgm_library.sql
CREATE TABLE IF NOT EXISTS sv_bgm_library (
    id BIGSERIAL PRIMARY KEY,
    bgm_name VARCHAR(200) NOT NULL,
    artist VARCHAR(100),
    style VARCHAR(64) NOT NULL,          -- luxury_piano / energetic_folk / emotional_strings 等
    bpm INTEGER,                          -- 节拍速度
    mood VARCHAR(64),                     -- 温暖 / 激昂 / 悬疑 / 治愈 / 欢快
    energy_level INTEGER,                 -- 1-10 能量等级
    emotion_curve_match VARCHAR(200),     -- 最匹配的情绪曲线模式
    -- 使用统计
    usage_count INTEGER DEFAULT 0,
    avg_viral_score DOUBLE PRECISION,     -- 使用此 BGM 的视频平均爆款分
    best_content_types JSONB DEFAULT '[]', -- 最适合的内容类型
    -- 版权信息
    license_type VARCHAR(32),             -- free / commercial / restricted
    source_platform VARCHAR(32),          -- 抖音 / 网易云 / 自制
    expire_date DATE,
    -- 元数据
    duration_seconds INTEGER,
    tags JSONB DEFAULT '[]',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX idx_bgm_style ON sv_bgm_library(style, deleted);
CREATE INDEX idx_bgm_mood ON sv_bgm_library(mood, deleted);
```

### 数据驱动推荐
```java
// 升级推荐逻辑：
// 1. 基于情绪曲线匹配（Phase 2 已有）
// 2. 新增：基于爆款数据的协同过滤
//    - 统计爆款视频使用的 BGM 风格分布
//    - 相似内容类型的爆款偏好哪些 BGM
//    - 按 avg_viral_score 排序推荐
// 3. 新增：版权过滤
//    - 过滤已过期或受限的 BGM
//    - 优先推荐 free 版权的 BGM
// 4. 新增：去重
//    - 避免推荐用户最近 7 天已使用的 BGM
```

**验收**：
1. BGM 推荐基于爆款数据统计
2. 推荐结果包含版权信息
3. 不重复推荐近期已用的 BGM

---

## 任务 4.3 — 竞品监测系统

**问题**：无竞品追踪和对标分析能力。

**文件**：
- `sql/shortvideo/competitor-schema.sql` — 新建
- `src/main/resources/db/migration/V027__sv_competitor.sql` — 新建
- `src/main/java/.../module/shortvideo/entity/SvCompetitor.java` — 新建
- `src/main/java/.../module/shortvideo/entity/SvCompetitorSnapshot.java` — 新建
- `src/main/java/.../module/shortvideo/service/CompetitorMonitorService.java` — 新建/重写
- `src/main/java/.../module/shortvideo/service/impl/CompetitorMonitorServiceImpl.java` — 新建/重写
- `src/main/java/.../module/shortvideo/controller/CompetitorController.java` — 新建
- `frontend-react/src/pages/shortvideo/CompetitorAnalysisPage.tsx` — 新建

**建表 SQL**：
```sql
-- V027__sv_competitor.sql

-- 竞品账号
CREATE TABLE IF NOT EXISTS sv_competitor (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    competitor_name VARCHAR(100) NOT NULL,
    platform VARCHAR(32) DEFAULT 'douyin',
    account_id VARCHAR(100),              -- 平台账号ID
    account_url VARCHAR(500),
    category VARCHAR(64),                 -- 护肤 / 彩妆 / 美妆工具
    fan_count BIGINT DEFAULT 0,
    notes TEXT,
    is_active BOOLEAN DEFAULT true,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 竞品数据快照（定期采集）
CREATE TABLE IF NOT EXISTS sv_competitor_snapshot (
    id BIGSERIAL PRIMARY KEY,
    competitor_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    fan_count BIGINT,
    fan_delta INTEGER,                    -- 较上次快照的粉丝变化
    video_count INTEGER,
    avg_view_count BIGINT,
    avg_like_rate DOUBLE PRECISION,
    avg_completion_rate DOUBLE PRECISION,
    top_video_titles JSONB DEFAULT '[]',  -- 近期爆款标题
    content_strategy_summary TEXT,        -- LLM 分析的内容策略摘要
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_competitor_snapshot ON sv_competitor_snapshot(competitor_id, snapshot_date);
```

### 竞品分析服务
```java
public interface CompetitorMonitorService {
    // 添加竞品账号
    SvCompetitor addCompetitor(Long ownerId, String name, String accountUrl);

    // 手动触发数据采集（调用 TianAPI 或爬虫）
    void collectSnapshot(Long competitorId);

    // 竞品对标分析（自己 vs 竞品）
    CompetitorBenchmarkVO benchmark(Long ownerId, Long competitorId);

    // 竞品内容策略分析（LLM 分析竞品近期内容趋势）
    String analyzeStrategy(Long competitorId);

    // 竞品动态监测（粉丝异常增长、爆款视频等）
    List<CompetitorAlert> checkAlerts(Long ownerId);
}
```

### 对标分析
```java
// benchmark() 实现：
// 1. 获取自己和竞品的最近快照
// 2. 对比维度：
//    - 粉丝数 & 增长率
//    - 平均播放量
//    - 点赞率 / 完播率
//    - 发布频率
//    - 内容类型分布
// 3. 生成对标报告：
//    - 优势项（自己 > 竞品 20%+）
//    - 劣势项（自己 < 竞品 20%+）
//    - 机会点（竞品未覆盖的内容类型）
// 4. LLM 生成差异化建议
```

**验收**：
1. 可添加竞品账号并采集数据
2. 对标分析显示优劣势对比
3. 竞品异常动态自动告警

---

## 任务 4.4 — 全流程工作流引擎

**问题**：`WorkflowExecutionService` 接口简陋，无可视化编排。

**文件**：
- `src/main/java/.../module/shortvideo/service/impl/WorkflowExecutionServiceImpl.java` — 重写
- `frontend-react/src/pages/shortvideo/WorkflowEditorPage.tsx` — 新建

**改动**：

### 预设工作流模板
```java
// 工作流定义（JSON 格式，存储在 sv_workflow_template 表）：
{
    "name": "一键日更工作流",
    "steps": [
        {"id": "select_topic", "type": "auto", "action": "hot_topic_select", "params": {"count": 3}},
        {"id": "generate_script", "type": "auto", "action": "ai_generate", "depends": ["select_topic"]},
        {"id": "quality_check", "type": "auto", "action": "quality_score", "depends": ["generate_script"],
         "condition": "score >= 60"},
        {"id": "human_review", "type": "manual", "action": "review", "depends": ["quality_check"],
         "condition": "score < 80"},
        {"id": "schedule", "type": "auto", "action": "calendar_schedule", "depends": ["quality_check", "human_review"]},
        {"id": "publish", "type": "auto", "action": "publish", "depends": ["schedule"],
         "condition": "scheduled_time <= now()"}
    ]
}
```

### 工作流执行引擎
```java
// 执行逻辑：
// 1. 解析工作流定义
// 2. 按依赖关系拓扑排序
// 3. 逐步执行：
//    - auto 步骤：自动调用对应服务
//    - manual 步骤：暂停等待人工操作
//    - condition 检查：不满足则跳过或回退
// 4. 进度通过 SSE 实时推送
// 5. 失败步骤支持重试或跳过
```

### 前端可视化
```
工作流编辑器（简化版，基于步骤列表）：
┌─────────────────────────────────────────┐
│ 工作流编辑器                    [保存] [运行] │
├─────────────────────────────────────────┤
│ ① 选题 [自动] ─→ ② 生成 [自动] ─→        │
│ ③ 质检 [自动] ─→ ④ 审核 [人工] ─→        │
│ ⑤ 排期 [自动] ─→ ⑥ 发布 [自动]           │
│                                          │
│ [+ 添加步骤]                              │
├─────────────────────────────────────────┤
│ 执行进度：                                │
│ ✅ 选题完成 → ✅ 生成完成 → 🔄 质检中...    │
└─────────────────────────────────────────┘
```

**验收**：
1. 可创建和编辑工作流模板
2. 工作流按步骤自动执行
3. 人工步骤暂停等待操作
4. 进度实时推送

---

## 任务 4.5 — 跨域知识迁移

**问题**：不同账号/行业的知识库完全隔离，无法共享优质知识。

**文件**：
- `src/main/java/.../module/ai/service/impl/CrossDomainShareServiceImpl.java` — 重写

**改动**：

### 知识迁移服务
```java
// 迁移场景：
// 1. 同行业不同账号：护肤品 A 账号的优质话术 → 护肤品 B 账号
// 2. 跨行业通用知识：通用销售技巧 → 所有行业
// 3. 系统级知识沉淀：高分话术自动进入公共知识库

// 迁移逻辑：
// 1. 筛选候选知识：
//    - 质量评分 > 80
//    - 效果评分 > 70
//    - 使用次数 > 10
// 2. 领域适配：
//    - 通用知识（销售技巧、互动话术）→ 直接迁移
//    - 行业知识（产品成分、功效）→ 需要适配（替换产品名、调整术语）
//    - 个性化知识（人设相关）→ 不迁移
// 3. 适配方式：
//    - LLM 改写：将原知识中的具体产品替换为目标产品
//    - 保留结构：保持话术结构和节奏，替换内容
// 4. 质量验证：
//    - 迁移后的知识需经过质量评分
//    - 低于阈值的不入库
```

### 公共知识库
```java
// 系统自动沉淀机制：
// 1. 定时任务（每周）扫描所有用户的高分知识
// 2. 满足条件的自动复制到 owner_id=0 的公共知识库
// 3. 去除个人信息和敏感内容
// 4. 新用户注册时自动关联公共知识库
```

**验收**：
1. 高分知识自动沉淀到公共知识库
2. 新用户可检索到公共知识
3. 跨账号知识迁移时自动适配产品信息

---

## Phase 4 完成标准

- [x] `mvn compile` + `npx tsc --noEmit` 通过
- [x] 二创模板库包含 4 种类型，可从爆款一键生成模板
- [x] BGM 推荐基于爆款数据统计，包含版权信息
- [x] 竞品监测可添加账号、采集数据、对标分析
- [x] 工作流引擎支持自动+人工混合步骤
- [x] 高分知识自动沉淀到公共知识库
