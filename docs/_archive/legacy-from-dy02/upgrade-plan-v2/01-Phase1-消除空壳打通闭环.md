# Phase 1 — 消除空壳 + 打通闭环（2 周）

> 目标：将 mock/硬编码/空实现全部替换为真实逻辑，打通"效果→分析→迭代"闭环
> 预期：5.5 → 6.8

---

## 任务 1.1 — 自进化规则引擎真实实现

**问题**：`EvolutionRuleEngineServiceImpl.evaluateUpdateRule()` 返回空 Map，`evaluateArchivalRule()` 和 `evaluateDedupRule()` 逻辑不完整。

**文件**：
- `src/main/java/.../module/ai/service/impl/EvolutionRuleEngineServiceImpl.java`
- `src/main/java/.../module/ai/service/EvolutionRuleEngineService.java`

**改动**：

### evaluateUpdateRule(Long userId)
```java
// 从 ai_knowledge_evolution_rule 读取 UPDATE 类型规则配置
// 查询 ai_kb_document 中 update_time < now() - rule.staleDays 的文档
// 对每个过期文档：
//   1. 检查关联的 LiveScript 效果评分（从 live_script_effectiveness 聚合）
//   2. 如果 avgScore < rule.minScore → 标记需更新
//   3. 返回 Map<docId, {reason, staleDays, avgScore, suggestedAction}>
```

### evaluateArchivalRule(Long userId)
```java
// 查询 ai_kb_document 中满足以下条件的文档：
//   - 最近 90 天无检索命中（从 ai_search_log 统计）
//   - 质量评分 < 30（从 ai_knowledge_quality_score 读取）
//   - 关联话术效果评分持续低于 40
// 返回待归档文档 ID 列表
```

### evaluateDedupRule(Long userId)
```java
// 查询 ai_knowledge_deduplication_group 中 similarity > 0.85 的分组
// 对每组保留 quality_score 最高的文档，其余标记为待去重
// 返回待去重文档 ID 列表
```

**依赖表**：ai_knowledge_evolution_rule, ai_kb_document, ai_knowledge_quality_score, ai_knowledge_deduplication_group, ai_search_log（如不存在需新建）

**验收**：调用 `/api/v1/ai/evolution/evaluate-rules` 返回非空结果

---

## 任务 1.2 — 知识质量评分真实指标

**问题**：`KnowledgeQualityScoringServiceImpl` 的 6 个评分方法返回固定值（75.5, 10, 68.5 等）。

**文件**：
- `src/main/java/.../module/ai/service/impl/KnowledgeQualityScoringServiceImpl.java`

**改动**：

### calculateQualityMetric(docId)
```java
// 从 ai_kb_document 读取：
//   - contentLength（内容长度评分：<100字=30, 100-500=60, 500-2000=80, >2000=90）
//   - chunkCount（分块数评分：1=40, 2-5=70, 5-10=85, >10=90）
//   - hasTitle（有标题+10）
//   - hasMetadata（有元数据+10）
// 加权计算综合质量分
```

### calculateEffectivenessMetric(docId)
```java
// 从 live_script_effectiveness 聚合：
//   - 关联此文档的话术平均效果评分
//   - 如果无关联数据返回 50（中性分）
// 从 ai_search_log 统计：
//   - 此文档被检索命中的次数
//   - 命中后用户是否采纳（如有反馈数据）
```

### getUsageCount(docId)
```java
// 从 ai_search_log 统计最近 30 天此文档被检索命中次数
// 如果 ai_search_log 表不存在，从 sys_log_operation 中按关键词匹配
```

### calculateFreshnessScore(docId)
```java
// 基于 update_time 计算时效性：
//   - 7天内=95, 30天内=80, 90天内=60, 180天内=40, 超过180天=20
// 如果文档类型是"时事热点"，衰减速度加倍
```

### calculateRelevanceScore(docId)
```java
// 从 ai_kb_document 的 kb_id 关联知识库类型
// 检查文档内容与知识库主题的相关度（基于关键词匹配率）
```

### calculateComprehensiveScore(docId)
```java
// 综合评分 = quality×0.25 + effectiveness×0.30 + freshness×0.20 + relevance×0.15 + usage×0.10
// 权重从 BusinessParamConfig 读取
```

**新增配置**（application.yml）：
```yaml
app:
  business:
    knowledge-quality:
      weights:
        quality: 0.25
        effectiveness: 0.30
        freshness: 0.20
        relevance: 0.15
        usage: 0.10
      freshness-decay-days: [7, 30, 90, 180]
      freshness-scores: [95, 80, 60, 40, 20]
```

**验收**：调用质量评分 API，返回值随文档不同而变化（不再是固定值）

---

## 任务 1.3 — 知识进化仪表盘真实数据

**问题**：`KnowledgeEvolutionServiceImpl` 的 7 个私有方法返回硬编码假数据。

**文件**：
- `src/main/java/.../module/ai/service/impl/KnowledgeEvolutionServiceImpl.java`（或对应的 Evolution 相关 ServiceImpl）

**改动**：

### getOverallHealthScore()
```java
// 从 ai_knowledge_quality_score 聚合所有文档的平均综合评分
// 如果无数据返回 0（而非硬编码 82.5）
```

### getKnowledgeCoverage()
```java
// 统计 ai_kb_document 中各类型文档数量
// 对比 ai_evolve_topic 中的主题数量
// 覆盖率 = 有文档覆盖的主题数 / 总主题数
```

### getEvolutionVelocity()
```java
// 统计最近 7 天 ai_evolve_task 中 status=completed 的任务数
// 对比上一个 7 天的数量，计算增长率
```

### getQualityTrend()
```java
// 从 ai_knowledge_quality_score 按周聚合平均分
// 返回最近 8 周的趋势数据
```

**验收**：进化仪表盘页面显示真实数据，刷新后数据随实际情况变化

---

## 任务 1.4 — 话术效果→自动迭代闭环

**问题**：话术低分触发 AI 改进建议后，建议仅存储不执行，无法自动生成新版本。

**文件**：
- `src/main/java/.../module/live/service/impl/LiveScriptAttributionServiceImpl.java`
- `src/main/java/.../module/live/service/impl/LiveScriptVersionServiceImpl.java`
- `src/main/java/.../module/live/service/LiveScriptVersionService.java`

**改动**：

### LiveScriptAttributionServiceImpl.triggerAutoImprovement()
```java
// 现有逻辑：低分 → 调用 LLM 生成改进建议 → 存储到 improvement_suggestion 字段
// 新增逻辑：
// 1. 如果 score < LOW_SCORE_THRESHOLD(40.0)：
//    a. 调用 LLM 生成改进后的完整话术文本（不仅是建议）
//    b. 调用 LiveScriptVersionService.createAutoImprovedVersion(scriptId, improvedContent, reason)
//    c. 新版本 status=draft, versionLabel="AI自动优化-v{n}", source="auto_improve"
//    d. 记录日志：原版本评分、改进原因、新版本ID
// 2. 如果 score < 25.0（极低分）：
//    a. 同时标记原版本为 archived
//    b. 新版本自动设为 active
```

### LiveScriptVersionService 新增方法
```java
LiveScriptVersion createAutoImprovedVersion(Long scriptId, String improvedContent, String reason);
// 创建新版本，versionNo 自增，关联原版本ID（parentVersionId 字段）
// 如果 LiveScriptVersion 表无 parentVersionId 字段，需新增
```

**数据库迁移**：
```sql
-- V020__script_version_auto_improve.sql
ALTER TABLE live_script_version ADD COLUMN IF NOT EXISTS parent_version_id BIGINT;
ALTER TABLE live_script_version ADD COLUMN IF NOT EXISTS source VARCHAR(32) DEFAULT 'manual';
-- source: manual / auto_improve / ab_winner
COMMENT ON COLUMN live_script_version.parent_version_id IS '父版本ID（自动优化时记录来源版本）';
COMMENT ON COLUMN live_script_version.source IS '版本来源：manual=手动, auto_improve=AI自动优化, ab_winner=AB测试胜出';
```

**验收**：
1. 话术评分低于 40 时，自动生成新版本（status=draft）
2. 新版本的 parent_version_id 指向原版本
3. 新版本的 source='auto_improve'

---

## 任务 1.5 — 爆款分析结构化输出

**问题**：`ViralVideoServiceImpl.analyzeViral()` 返回纯文本，前端无法结构化展示和复用。

**文件**：
- `src/main/java/.../module/shortvideo/service/impl/ViralVideoServiceImpl.java`
- `src/main/java/.../module/shortvideo/vo/` — 新增 ViralAnalysisResultVO
- `frontend-react/src/pages/shortvideo/ViralLibraryPage.tsx`

**改动**：

### 新增 ViralAnalysisResultVO
```java
public class ViralAnalysisResultVO {
    private String summary;                    // 一句话总结
    private OpeningAnalysis opening;           // 开场分析（前3秒hook、悬念类型、情绪基调）
    private ClimaxAnalysis climax;             // 高潮分析（高潮时间点、情绪峰值、转折手法）
    private EndingAnalysis ending;             // 结尾分析（行动号召类型、悬念留白、情绪落点）
    private EmotionCurve emotionCurve;         // 情绪曲线（时间点→情绪值数组）
    private BgmAnalysis bgm;                   // BGM分析（风格、节奏BPM、情绪匹配度）
    private List<ViralElement> viralElements;  // 爆款元素列表（元素名、出现时间、效果评分）
    private TransitionAnalysis transitions;    // 转场分析（转场类型列表、节奏评分）
    private CopywritingAnalysis copywriting;   // 文案分析（金句列表、话术技巧、说服力评分）
    private Double viralScore;                 // 爆款综合评分（0-100）
    private List<String> remakeAdvice;         // 二创建议（3-5条）
}
```

### 修改 analyzeViral()
```java
// 现有：调用 LLM → 返回 String
// 改为：
// 1. 构建结构化 prompt，要求 LLM 返回 JSON 格式
// 2. prompt 模板：
//    "请分析以下爆款视频，按 JSON 格式返回：
//     {opening: {hook, suspenseType, emotionBase},
//      climax: {timePoint, emotionPeak, turningTechnique},
//      ending: {ctaType, suspenseCliffhanger, emotionLanding},
//      emotionCurve: [{time, value}...],
//      bgm: {style, bpm, emotionMatch},
//      viralElements: [{name, timePoint, score}...],
//      transitions: [{type, timePoint}...],
//      copywriting: {goldenSentences: [...], techniques: [...], persuasionScore},
//      viralScore, remakeAdvice: [...]}"
// 3. 解析 JSON → ViralAnalysisResultVO
// 4. 同时将结构化结果存入 ai_viral_analysis 表的 analysis_result 字段（JSON格式）
// 5. 如果 JSON 解析失败，降级为纯文本返回（兼容旧逻辑）
```

### 前端展示
```
ViralLibraryPage 的分析结果展示从纯文本改为结构化卡片：
- 情绪曲线图（ECharts 折线图）
- 爆款元素标签云
- 开场/高潮/结尾三段式分析卡片
- BGM 信息卡片
- 二创建议列表
```

**验收**：
1. 分析结果返回结构化 JSON
2. 前端展示情绪曲线图和分段分析卡片
3. 旧的纯文本分析结果仍可正常显示（向后兼容）

---

## 任务 1.6 — 硬编码参数迁移到 BusinessParamConfig

**问题**：多处硬编码的权重、阈值、时间窗口散落在代码中。

**文件**：
- `src/main/java/.../common/config/BusinessParamConfig.java`
- `src/main/resources/application.yml`
- 涉及的 ServiceImpl 文件（见下方清单）

**改动**：

### BusinessParamConfig 新增配置类
```java
@Data
public static class Attribution {
    private double viewerWeight = 0.35;
    private double interactionWeight = 0.35;
    private double conversionWeight = 0.30;
    private double lowScoreThreshold = 40.0;
    private double criticalScoreThreshold = 25.0;
    private int windowSeconds = 30;
}

@Data
public static class Effectiveness {
    private double conversionWeight = 0.4;
    private double likeWeight = 0.3;
    private double commentWeight = 0.2;
    private double completionWeight = 0.1;
    private double maxScore = 10.0;
}

@Data
public static class VersionComparison {
    private double highSimilarityThreshold = 0.95;
    private double lowSimilarityThreshold = 0.60;
}

@Data
public static class AutoImprove {
    private boolean enabled = true;
    private boolean autoArchiveCritical = true;  // 极低分自动归档
}
```

### application.yml 新增
```yaml
app:
  business:
    attribution:
      viewer-weight: 0.35
      interaction-weight: 0.35
      conversion-weight: 0.30
      low-score-threshold: 40.0
      critical-score-threshold: 25.0
      window-seconds: 30
    effectiveness:
      conversion-weight: 0.4
      like-weight: 0.3
      comment-weight: 0.2
      completion-weight: 0.1
    version-comparison:
      high-similarity-threshold: 0.95
      low-similarity-threshold: 0.60
    auto-improve:
      enabled: true
      auto-archive-critical: true
```

### 需修改的 ServiceImpl
| 文件 | 硬编码项 | 替换为 |
|------|---------|--------|
| LiveScriptAttributionServiceImpl | W_VIEWER=0.35 等 | businessParam.getAttribution().getViewerWeight() |
| EffectivenessScoreServiceImpl | 转化0.4/点赞0.3等 | businessParam.getEffectiveness().getConversionWeight() |
| LiveScriptVersionServiceImpl | 95%/60% 阈值 | businessParam.getVersionComparison().getHighSimilarityThreshold() |
| IndustryCausalEngineImpl | BASE_RATE=0.35 | businessParam.getCausalEngine().getBaseRate()（已有，确认使用） |

**验收**：修改 application.yml 中的权重值后，重启服务，对应计算结果随之变化

---

## 任务 1.7 — 搜索日志表（支撑质量评分和进化规则）

**问题**：质量评分和进化规则需要"文档被检索命中次数"数据，但无 ai_search_log 表。

**文件**：
- `sql/ai/search-log-schema.sql` — 新建
- `src/main/resources/db/migration/V020__ai_search_log.sql`
- `src/main/java/.../module/ai/entity/AiSearchLog.java` — 新建
- `src/main/java/.../module/ai/repository/AiSearchLogRepository.java` — 新建
- `src/main/java/.../module/ai/service/impl/SearchServiceImpl.java` — 修改，记录搜索日志

**改动**：

### 建表 SQL
```sql
CREATE TABLE IF NOT EXISTS ai_search_log (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    query_text TEXT NOT NULL,
    query_rewritten TEXT,
    kb_id BIGINT,
    hit_doc_ids TEXT,           -- 命中文档ID列表，逗号分隔
    hit_count INTEGER DEFAULT 0,
    top1_score DOUBLE PRECISION,
    search_type VARCHAR(32),    -- hybrid / vector / keyword
    latency_ms INTEGER,
    user_feedback VARCHAR(16),  -- helpful / not_helpful / null
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX idx_search_log_owner ON ai_search_log(owner_id, create_time);
CREATE INDEX idx_search_log_doc ON ai_search_log(hit_doc_ids);
```

### SearchServiceImpl 修改
```java
// 在 hybridSearch() 方法末尾，异步记录搜索日志：
// asyncExecutor.execute(() -> {
//     AiSearchLog log = new AiSearchLog();
//     log.setOwnerId(userId);
//     log.setQueryText(query);
//     log.setHitDocIds(results.stream().map(r -> String.valueOf(r.getDocId())).collect(joining(",")));
//     log.setHitCount(results.size());
//     log.setTop1Score(results.isEmpty() ? null : results.get(0).getScore());
//     log.setSearchType("hybrid");
//     log.setLatencyMs(elapsed);
//     searchLogRepository.save(log);
// });
```

**验收**：执行搜索后，ai_search_log 表有新记录；质量评分的 usageCount 返回真实数据

---

## Phase 1 完成标准

- [x] `mvn compile` 通过
- [x] `cd frontend-react && npx tsc --noEmit` 通过
- [x] 自进化规则 evaluateUpdateRule 返回非空结果
- [x] 知识质量评分返回动态值（不同文档不同分数）
- [x] 进化仪表盘显示真实数据
- [x] 话术低分自动生成新版本（draft 状态）
- [x] 爆款分析返回结构化 JSON + 前端卡片展示
- [x] 硬编码参数可通过 application.yml 调整
- [x] 搜索日志正常记录
