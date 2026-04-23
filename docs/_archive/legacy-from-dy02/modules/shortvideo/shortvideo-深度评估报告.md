# 短视频模块（shortvideo）— 深度评估报告

> 评估日期：2026-02-26 | 评估维度：架构师 + 产品经理 + 抖音运营专家

---

## 一、模块概况

短视频模块是平台核心业务模块，覆盖短视频全生命周期：数据分析 → 选题 → AI 创作 → 发布 → 复盘。

- 13 个数据库实体
- 13 个 Service 实现
- 51 个 API 接口
- 8 个前端页面
- 24 个用户故事

---

## 二、三维评估

### 2.1 架构师视角

#### 优点
- 实体设计覆盖面广，从视频管理到爆款库、热点话题、创作计划、AI 生成任务都有
- Service 层职责清晰，每个实体对应独立 Service
- 前端路由按功能分组，10 个路由覆盖创作全流程
- 软删除 + 数据隔离（owner_id）设计规范

#### 问题清单

| # | 级别 | 问题 | 影响 |
|---|------|------|------|
| S1 | S | **抖音数据同步是空壳** — sync() 方法是 TODO 占位符，没有实际调用抖音 API | 核心功能缺失，VideoStatistics 无数据来源 |
| S2 | S | **缺少视频-AI 调用关联** — ShortVideo 和 ai_call_log 之间没有关联字段 | 无法追踪哪条视频是 AI 生成的，效果归因断裂 |
| S3 | S | **VideoStatistics 缺少增量同步机制** — 只有 updateStatistics 手动更新，没有定时从抖音拉取 | 数据不实时，统计分析无意义 |
| A1 | A | **BenchmarkVideo 爆款库是静态数据** — 只有 CRUD，没有自动从抖音发现爆款的机制 | 爆款库需要人工维护，无法自动更新 |
| A2 | A | **HotTopic 热点话题缺少数据源** — 有 expiryTime 字段但没有自动过期和自动发现逻辑 | 热点话题需要人工录入，时效性差 |
| A3 | A | **VideoGeneration 生成任务缺少回调机制** — generationStatus 字段存在但没有异步回调/轮询实现 | AI 视频生成是异步的，前端无法知道何时完成 |
| A4 | A | **ShortVideoContent 和 ShortVideo 关系不清** — 两个实体都有视频信息，职责重叠 | 数据冗余，维护困难 |
| A5 | A | **缺少发布调度功能** — 没有定时发布、最佳发布时间推荐 | 运营效率低，错过最佳发布窗口 |
| A6 | A | **评论管理缺少情感分析** — VideoComment 只有内容和点赞数，没有情感标签 | 无法自动识别负面评论，舆情管理缺失 |
| B1 | B | **VideoStatistics 缺少周/月聚合** — 只有每日快照，没有周/月维度聚合 | 趋势分析需要前端自己计算 |
| B2 | B | **VideoPlan 缺少与 AI 生成的关联** — 创作计划和实际生成的内容之间没有关联 | 无法追踪计划执行率 |
| B3 | B | **VideoAsset 缺少与 storage 模块的集成** — upload() 方法存在但没有调用 storage 模块 | 文件存储逻辑不完整 |
| B4 | B | **前端 VideoStatistics 页面只有表格** — 缺少图表可视化（趋势图、对比图） | 数据展示不直观 |
| B5 | B | **缺少批量操作** — 没有批量删除、批量发布、批量下架 | 管理效率低 |
| B6 | B | **VideoCategory 缺少层级结构** — 只有一级分类，没有父子关系 | 分类体系不够灵活 |

---

### 2.2 产品经理视角

#### 核心用户旅程缺口

```
当前用户旅程（有断裂）：

达人想发一条短视频
    ↓
① 看热点话题 → HotTopic（❌ 数据靠人工录入，不实时）
    ↓
② 参考爆款 → BenchmarkVideo（❌ 静态数据，不自动更新）
    ↓
③ AI 生成文案 → 调用 ai 模块（✅ 可用）
    ↓
④ AI 生成视频 → VideoGeneration（⚠️ 异步无回调）
    ↓
⑤ 发布到抖音 → ❌ 完全缺失，没有发布接口
    ↓
⑥ 数据回流 → VideoStatistics（❌ sync 是空壳）
    ↓
⑦ 效果分析 → ❌ 无法归因到 AI 生成
```

**关键缺失：步骤 ⑤ 发布到抖音 和 ⑥ 数据回流 是整个闭环的断裂点。**

#### 产品级问题

| # | 问题 | 建议 |
|---|------|------|
| P1 | **没有「一键发布到抖音」** | 对接抖音开放平台视频发布 API，实现从平台直接发布 |
| P2 | **没有发布时间推荐** | 基于历史数据分析该账号的最佳发布时间段 |
| P3 | **没有竞品视频分析** | 输入竞品账号 ID，自动拉取其视频数据进行对比分析 |
| P4 | **没有内容日历** | 按日/周维度展示发布计划和实际发布情况 |
| P5 | **没有 A/B 测试支持** | 同一主题生成多版文案，分别发布后对比效果 |
| P6 | **创作流程太分散** | 热点→选题→文案→视频→发布 分散在不同页面，需要统一的创作工作台 |

---

### 2.3 抖音运营专家视角

#### 运营场景覆盖度

| 场景 | 覆盖度 | 说明 |
|------|--------|------|
| 视频数据查看 | ⚠️ 30% | 有表结构但 sync 是空壳 |
| 爆款分析 | ⚠️ 20% | 有爆款库但是静态数据 |
| 热点追踪 | ⚠️ 20% | 有热点表但无自动发现 |
| AI 文案生成 | ✅ 80% | 对接 ai 模块，基本可用 |
| AI 视频生成 | ⚠️ 50% | 有任务表但异步回调未实现 |
| 发布管理 | ❌ 0% | 完全缺失 |
| 数据复盘 | ❌ 0% | 无数据回流，无法复盘 |
| 竞品分析 | ❌ 0% | 完全缺失 |
| 内容规划 | ⚠️ 40% | 有 VideoPlan 但缺少日历视图 |
| 评论管理 | ⚠️ 50% | 有 CRUD 但缺少情感分析和自动回复 |

#### 运营专家核心建议

1. **数据是一切的基础** — 没有抖音数据回流，所有分析功能都是空中楼阁。优先级最高的是把 sync 做实。
2. **爆款不是静态的** — 爆款库应该自动从绑定账号和行业热门中发现，而不是人工录入。
3. **发布时间比内容更重要** — 同样的内容，发布时间差 2 小时，播放量可能差 10 倍。必须有发布时间推荐。
4. **评论区是第二战场** — 评论区的互动直接影响视频推荐权重。需要情感分析 + 智能回复建议。

---

## 三、升级建议（按优先级）

### P0 — 必须立即修复

| # | 升级项 | 说明 |
|---|--------|------|
| U1 | **实现抖音数据同步** | 对接抖音开放平台 API，定时拉取视频列表和统计数据。建议每 6h 全量同步 + 发布后 1h 内高频同步（每 10 分钟） |
| U2 | **视频-AI 关联** | ShortVideo/ShortVideoContent 新增 `ai_call_log_id` 字段，发布时关联 AI 生成记录 |
| U3 | **VideoStatistics 定时同步** | 新增定时任务，每日从抖音 API 拉取所有已发布视频的最新统计数据 |

### P1 — 核心体验提升

| # | 升级项 | 说明 |
|---|--------|------|
| U4 | **爆款自动发现** | 定时扫描 VideoStatistics，播放量 > 账号均值 3x 的自动标记为爆款并入 BenchmarkVideo |
| U5 | **热点自动追踪** | 对接抖音热搜 API（或第三方数据源），自动更新 HotTopic，过期自动下架 |
| U6 | **发布时间推荐** | 分析该账号历史视频的发布时间 vs 播放量，推荐最佳发布时间段 |
| U7 | **评论情感分析** | VideoComment 新增 sentiment 字段（positive/neutral/negative），用 LLM 或规则引擎自动标注 |
| U8 | **VideoGeneration 异步回调** | 实现 RabbitMQ 消息回调 + 前端轮询/WebSocket 通知 |

### P2 — 体验优化

| # | 升级项 | 说明 |
|---|--------|------|
| U9 | **统一创作工作台** | 新增页面：热点→选题→文案→视频→发布 一站式流程 |
| U10 | **内容日历** | 按日/周展示发布计划和实际发布，支持拖拽排期 |
| U11 | **VideoStatistics 图表化** | 前端增加 ECharts 趋势图、对比图 |
| U12 | **批量操作** | 批量删除、批量发布、批量下架 |
| U13 | **ShortVideo 和 ShortVideoContent 合并** | 消除职责重叠，统一为一个实体 |

---

## 四、数据库变更建议

```sql
-- U2: 视频-AI 关联
ALTER TABLE short_video_content ADD COLUMN ai_call_log_id BIGINT;
ALTER TABLE short_video_content ADD COLUMN ai_generated BOOLEAN DEFAULT false;

-- U4: 爆款自动标记
ALTER TABLE benchmark_video ADD COLUMN source VARCHAR(32) DEFAULT 'manual';  -- manual / auto_detected
ALTER TABLE benchmark_video ADD COLUMN original_video_id BIGINT;  -- 关联原始 ShortVideo
ALTER TABLE benchmark_video ADD COLUMN detected_time TIMESTAMP;

-- U5: 热点自动追踪
ALTER TABLE hot_topic ADD COLUMN source VARCHAR(32) DEFAULT 'manual';  -- manual / douyin_hot / trending
ALTER TABLE hot_topic ADD COLUMN douyin_hot_id VARCHAR(128);  -- 抖音热搜 ID
ALTER TABLE hot_topic ADD COLUMN heat_score DECIMAL(10,2);  -- 热度值

-- U7: 评论情感分析
ALTER TABLE video_comment ADD COLUMN sentiment VARCHAR(16);  -- positive / neutral / negative
ALTER TABLE video_comment ADD COLUMN sentiment_score DECIMAL(3,2);  -- 情感分数 0-1

-- U6: 发布时间推荐（新增表）
CREATE TABLE video_publish_time_analysis (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    day_of_week INTEGER NOT NULL,       -- 0=周日 1=周一 ... 6=周六
    hour_of_day INTEGER NOT NULL,       -- 0-23
    avg_view_count BIGINT DEFAULT 0,    -- 该时段发布视频的平均播放量
    video_count INTEGER DEFAULT 0,      -- 该时段发布的视频数
    recommended BOOLEAN DEFAULT false,  -- 是否推荐时段
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_publish_time ON video_publish_time_analysis(account_id, day_of_week, hour_of_day);
```

---

## 五、与 AI 模块的协同升级

短视频模块的升级与 AI 模块 `12-升级路线图.md` 紧密关联：

| AI 模块升级项 | 短视频模块配合 |
|--------------|---------------|
| 效果归因链（P0） | ShortVideoContent 新增 ai_call_log_id，发布时传入 |
| 爆款拆解 Agent（P0） | VideoStatistics 提供爆款检测数据源，BenchmarkVideo 接收拆解结果 |
| 查询改写 + 用户画像（P1） | DouyinAccount + VideoStatistics 提供用户画像数据 |

---

## 六、总评

| 维度 | 当前评分 | 目标评分 | 差距 |
|------|---------|---------|------|
| 数据完整度 | 3/10 | 8/10 | 抖音数据同步是空壳 |
| 功能完整度 | 5/10 | 8/10 | 发布、复盘、竞品分析缺失 |
| AI 集成度 | 4/10 | 8/10 | 有接口但缺少归因闭环 |
| 用户体验 | 5/10 | 8/10 | 流程分散，缺少统一工作台 |
| 运营实用性 | 3/10 | 8/10 | 没有数据就没有运营价值 |

**一句话总结：短视频模块的骨架完整，但「血液」（抖音数据回流）和「神经」（AI 归因闭环）都还没接上。优先把数据同步做实，其他一切才有意义。**

---
