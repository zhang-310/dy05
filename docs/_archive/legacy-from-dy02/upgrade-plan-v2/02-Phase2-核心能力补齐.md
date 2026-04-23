# Phase 2 — 核心能力补齐（3 周）

> 前置：Phase 1 全部完成
> 目标：补齐情绪引擎、重排、实时驱动、A/B 测试等核心能力
> 预期：6.8 → 7.5

---

## 任务 2.1 — 情绪过山车引擎集成到短视频模块

**问题**：直播模块有 `EmotionCurveEngine`，但短视频模块完全未集成，无法设计情绪曲线。

**文件**：
- `src/main/java/.../module/live/service/impl/EmotionCurveEngine.java` — 现有，需抽取为公共服务
- `src/main/java/.../common/service/EmotionCurveService.java` — 新建接口
- `src/main/java/.../common/service/impl/EmotionCurveServiceImpl.java` — 新建实现
- `src/main/java/.../module/shortvideo/service/impl/ShortVideoAiServiceImpl.java` — 修改
- `frontend-react/src/pages/shortvideo/ScriptPlanningPage.tsx` — 修改

**改动**：

### 抽取公共情绪曲线服务
```java
public interface EmotionCurveService {
    // 解析情绪曲线字符串 "100→80→60→120→90"
    List<EmotionPoint> parseCurve(String curveExpression);

    // 根据视频时长和情绪曲线生成分段情绪标注
    List<EmotionSegment> generateSegments(int durationSeconds, List<EmotionPoint> curve);

    // 推荐情绪曲线模板（基于内容类型）
    List<EmotionCurveTemplate> recommendTemplates(String contentType);

    // 情绪曲线 → BGM 节奏建议
    BgmRhythmSuggestion suggestBgmRhythm(List<EmotionPoint> curve);
}
```

### 预设情绪曲线模板
```java
// 从 BusinessParamConfig 读取，支持配置化
Map<String, String> templates = Map.of(
    "hook_climax",     "90→60→40→80→100→70",    // 开场强→低谷→高潮→收尾
    "slow_build",      "40→50→60→75→90→100",    // 渐进式升温
    "rollercoaster",   "80→40→90→30→100→60",    // 过山车（爆款常用）
    "suspense",        "60→70→50→40→30→100",    // 悬念式（结尾爆发）
    "emotional_wave",  "70→90→50→85→40→95"      // 情感波浪（种草常用）
);
```

### ShortVideoAiServiceImpl 集成
```java
// generateCopy() 方法中：
// 1. 如果 vo.getEmotionCurve() 不为空，解析情绪曲线
// 2. 将情绪分段注入 prompt：
//    "请按以下情绪节奏编写脚本：
//     0-3秒：情绪值90（强烈开场hook）
//     3-8秒：情绪值60（铺垫问题）
//     8-15秒：情绪值40（低谷/痛点）
//     15-22秒：情绪值80（转折/解决方案）
//     22-28秒：情绪值100（高潮/产品展示）
//     28-30秒：情绪值70（行动号召）"
// 3. 同时生成 BGM 节奏建议
```

### 前端 ScriptPlanningPage 新增
```
┌─────────────────────────────────────────┐
│ 情绪曲线设计                              │
│ [预设模板▾] hook高潮型 / 渐进型 / 过山车型  │
│                                          │
│ 100 ─ ●                    ●             │
│  80 ─    ●              ●     ●          │
│  60 ─       ●                            │
│  40 ─          ●                         │
│  20 ─                                    │
│      ─────────────────────────────       │
│      0s   5s   10s  15s  20s  25s  30s   │
│                                          │
│ [可拖拽编辑节点] [重置] [随机生成]          │
└─────────────────────────────────────────┘
```

**验收**：
1. 短视频脚本生成时可选择情绪曲线模板
2. 生成的脚本内容体现情绪节奏变化
3. 同时输出 BGM 节奏建议

---

## 任务 2.2 — Cross-Encoder 重排集成

**问题**：RAG 检索仅有 RRF 融合，无 Cross-Encoder 精排，检索质量不稳定。

**文件**：
- `src/main/java/.../module/ai/service/impl/SearchServiceImpl.java`
- `src/main/java/.../module/ai/service/impl/KbHybridRetrieveServiceImpl.java`（如存在）
- `src/main/resources/application.yml`

**改动**：

### 重排服务实现
```java
// 在 hybridSearch 流程末尾增加重排步骤：
// 1. RRF 融合后取 top-20 候选
// 2. 调用 Cross-Encoder 对 (query, doc) 对打分
//    - 优先使用本地 Ollama 部署的 bge-reranker-v2-m3
//    - fallback 到 LLM 打分（prompt: "请对以下文档与查询的相关性打分 0-10"）
// 3. 按重排分数重新排序，取 top-k 返回

// Cross-Encoder 调用方式（Ollama API）：
// POST http://localhost:11434/api/embeddings
// {"model": "bge-reranker-v2-m3", "prompt": "query: {q} document: {d}"}
// 返回相关性分数

// 如果 Ollama 不可用，降级为 LLM 打分：
// prompt = "请评估以下查询与文档的相关性（0-10分）：\n查询：{q}\n文档：{d}\n只返回数字分数"
```

### application.yml 新增
```yaml
app:
  ai:
    reranker:
      enabled: true
      model: bge-reranker-v2-m3
      endpoint: ${RERANKER_ENDPOINT:http://localhost:11434}
      top-k-before-rerank: 20
      top-k-after-rerank: 5
      fallback-to-llm: true
      timeout-ms: 5000
```

**验收**：
1. 搜索结果经过重排后，top-1 相关性明显提升
2. Ollama 不可用时自动降级到 LLM 打分
3. 重排耗时 < 2 秒（5 条文档）

---

## 任务 2.3 — RAG 自适应权重

**问题**：向量/BM25 融合权重硬编码（70%/30%），不同查询类型应有不同权重。

**文件**：
- `src/main/java/.../module/ai/service/impl/SearchServiceImpl.java`（或 KbHybridRetrieveServiceImpl）
- `src/main/resources/application.yml`

**改动**：

### 查询类型识别 + 动态权重
```java
// 1. 查询类型识别（基于规则）：
//    - 短查询（<5字）→ 偏向 BM25（精确匹配）：vector=0.4, keyword=0.6
//    - 长查询（>20字）→ 偏向向量（语义匹配）：vector=0.8, keyword=0.2
//    - 含专业术语 → 偏向 BM25：vector=0.3, keyword=0.7
//    - 自然语言问句 → 偏向向量：vector=0.75, keyword=0.25
//    - 默认 → vector=0.7, keyword=0.3

// 2. 识别逻辑：
private SearchWeights determineWeights(String query) {
    if (query.length() < 5) return new SearchWeights(0.4, 0.6);
    if (query.length() > 20) return new SearchWeights(0.8, 0.2);
    if (containsTechnicalTerms(query)) return new SearchWeights(0.3, 0.7);
    if (isNaturalQuestion(query)) return new SearchWeights(0.75, 0.25);
    return new SearchWeights(defaultVectorWeight, defaultKeywordWeight);
}

// 3. containsTechnicalTerms：检查是否包含产品成分、品牌名等专业词汇
// 4. isNaturalQuestion：检查是否以"如何/怎么/为什么/什么"等开头
```

### application.yml
```yaml
app:
  ai:
    search:
      adaptive-weights:
        enabled: true
        short-query:
          vector: 0.4
          keyword: 0.6
        long-query:
          vector: 0.8
          keyword: 0.2
        technical:
          vector: 0.3
          keyword: 0.7
        question:
          vector: 0.75
          keyword: 0.25
        default:
          vector: 0.7
          keyword: 0.3
```

**验收**：短查询和长查询的检索结果排序有明显差异

---

## 任务 2.4 — 实时数据驱动话术推荐

**问题**：直播实时面板仅展示数据，不驱动话术调整。

**文件**：
- `src/main/java/.../module/live/service/impl/LiveRealtimePanelServiceImpl.java`（或对应服务）
- `src/main/java/.../module/live/controller/LiveRealtimePanelController.java`
- `frontend-react/src/pages/live/LiveRealtimePanel.tsx`（或 LiveRealtimePanelPage.tsx）

**改动**：

### 后端：实时建议引擎
```java
public class RealtimeSuggestion {
    private String type;        // switch_script / inject_interaction / slow_down / speed_up
    private String reason;      // "停留率下降至 35%，低于阈值 40%"
    private String suggestion;  // "建议切换到互动话术，提问观众使用体验"
    private Long suggestedScriptId;  // 推荐的话术ID（如有）
    private int urgency;        // 1-5 紧急程度
}

// 在实时数据推送中增加建议生成：
// 1. 每 30 秒评估一次实时指标
// 2. 触发规则（从 BusinessParamConfig 读取阈值）：
//    - 停留率 < 40% → switch_script（切换到互动话术）
//    - 互动率 < 2% → inject_interaction（注入互动话术）
//    - 在线人数持续下降 3 分钟 → speed_up（加快节奏）
//    - 转化率 < 1% 且已讲解 > 3 分钟 → switch_script（切换产品）
// 3. 建议通过 SSE 推送到前端
```

### application.yml
```yaml
app:
  business:
    realtime-suggestion:
      enabled: true
      evaluate-interval-seconds: 30
      thresholds:
        min-retention-rate: 40
        min-interaction-rate: 2
        viewer-decline-minutes: 3
        min-conversion-rate: 1
        max-explain-minutes: 3
```

### 前端：建议展示
```
实时面板右侧新增"AI 建议"区域：
┌──────────────────────────────┐
│ 🔴 紧急建议                   │
│ 停留率降至 35%，建议切换互动话术 │
│ [采纳] [忽略]                  │
├──────────────────────────────┤
│ 🟡 一般建议                   │
│ 当前产品讲解已超 3 分钟         │
│ 建议切换下一个产品              │
│ [采纳] [忽略]                  │
└──────────────────────────────┘
```

**验收**：
1. 实时面板在指标异常时自动弹出建议
2. 建议包含具体的话术推荐
3. 用户可采纳或忽略建议

---

## 任务 2.5 — 版本级 A/B 测试框架

**问题**：话术版本无 A/B 测试能力，无法科学对比不同版本效果。

**文件**：
- `src/main/java/.../module/live/entity/LiveScriptAbTest.java` — 新建
- `src/main/java/.../module/live/repository/LiveScriptAbTestRepository.java` — 新建
- `src/main/java/.../module/live/service/LiveScriptAbTestService.java` — 新建
- `src/main/java/.../module/live/service/impl/LiveScriptAbTestServiceImpl.java` — 新建
- `src/main/java/.../module/live/controller/LiveScriptVersionController.java` — 修改

**改动**：

### 建表 SQL
```sql
-- V021__live_script_ab_test.sql
CREATE TABLE IF NOT EXISTS live_script_ab_test (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    script_id BIGINT NOT NULL,
    version_a_id BIGINT NOT NULL,
    version_b_id BIGINT NOT NULL,
    status VARCHAR(16) DEFAULT 'running',  -- running / completed / cancelled
    traffic_split INTEGER DEFAULT 50,       -- A 版本流量占比（%）
    -- 效果指标
    a_impressions INTEGER DEFAULT 0,
    b_impressions INTEGER DEFAULT 0,
    a_conversion_rate DOUBLE PRECISION,
    b_conversion_rate DOUBLE PRECISION,
    a_retention_rate DOUBLE PRECISION,
    b_retention_rate DOUBLE PRECISION,
    a_interaction_rate DOUBLE PRECISION,
    b_interaction_rate DOUBLE PRECISION,
    -- 统计显著性
    p_value DOUBLE PRECISION,
    confidence_level DOUBLE PRECISION,
    winner VARCHAR(1),                      -- A / B / null（未决出）
    -- 元数据
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

### A/B 测试服务
```java
public interface LiveScriptAbTestService {
    // 创建 A/B 测试
    LiveScriptAbTest createTest(Long scriptId, Long versionAId, Long versionBId, int trafficSplit, Long ownerId);

    // 记录曝光（每次使用话术时调用）
    void recordImpression(Long testId, String variant);  // variant: "A" or "B"

    // 更新效果指标（归因计算后调用）
    void updateMetrics(Long testId, String variant, double conversionRate, double retentionRate, double interactionRate);

    // 计算统计显著性（Z-test for proportions）
    SignificanceResult calculateSignificance(Long testId);

    // 自动结束测试（p_value < 0.05 且样本量 > 100）
    void autoComplete(Long testId);

    // 查询测试列表
    PageResultVO<LiveScriptAbTest> listTests(Long ownerId, int page, int rows);
}
```

### 统计显著性计算
```java
// Z-test for two proportions:
// z = (p1 - p2) / sqrt(p_pool * (1 - p_pool) * (1/n1 + 1/n2))
// p_value = 2 * (1 - normalCDF(abs(z)))
// 当 p_value < 0.05 且 min(n1, n2) > 100 时，自动判定胜者
```

**验收**：
1. 可创建 A/B 测试，指定两个版本和流量分配
2. 测试运行中自动累计曝光和效果数据
3. 样本量足够时自动计算 p-value 并判定胜者

---

## 任务 2.6 — 版本语义对比升级

**问题**：版本对比仅用 Levenshtein 编辑距离，无法识别同义表达。

**文件**：
- `src/main/java/.../module/live/service/impl/LiveScriptVersionServiceImpl.java`

**改动**：

### 对比算法升级
```java
// 现有：Levenshtein 编辑距离 → 字符级相似度
// 新增：语义相似度（向量余弦距离）

public VersionComparisonResult compareVersions(Long versionAId, Long versionBId) {
    // 1. 字符级对比（保留现有）
    double editSimilarity = calculateEditSimilarity(contentA, contentB);

    // 2. 语义级对比（新增）
    double semanticSimilarity = 0.0;
    if (vectorService != null) {
        float[] embA = vectorService.embed(contentA);
        float[] embB = vectorService.embed(contentB);
        semanticSimilarity = cosineSimilarity(embA, embB);
    }

    // 3. 效果指标对比（新增）
    EffectivenessComparison effectivenessComparison = compareEffectiveness(versionAId, versionBId);

    // 4. 差异摘要（新增，LLM 生成）
    String diffSummary = null;
    if (llmClient != null && editSimilarity < 0.9) {
        diffSummary = llmClient.chat(
            "请简要总结以下两个话术版本的主要差异（50字以内）：\n版本A：" + contentA + "\n版本B：" + contentB
        );
    }

    return new VersionComparisonResult(
        editSimilarity, semanticSimilarity, effectivenessComparison, diffSummary
    );
}
```

**验收**：版本对比结果包含字符相似度、语义相似度、效果对比、差异摘要四个维度

---

## 任务 2.7 — BGM 智能推荐引擎

**问题**：短视频仅支持 bgmStyle 参数传递，无智能推荐。

**文件**：
- `src/main/java/.../module/shortvideo/service/BgmRecommendService.java` — 新建
- `src/main/java/.../module/shortvideo/service/impl/BgmRecommendServiceImpl.java` — 新建
- `src/main/java/.../module/shortvideo/vo/BgmRecommendVO.java` — 新建

**改动**：

### BGM 推荐服务
```java
public interface BgmRecommendService {
    // 基于情绪曲线推荐 BGM 风格
    List<BgmRecommendVO> recommendByEmotionCurve(List<EmotionPoint> curve);

    // 基于脚本内容推荐 BGM
    List<BgmRecommendVO> recommendByScript(String scriptContent, String contentType);

    // 基于爆款视频的 BGM 分析推荐
    List<BgmRecommendVO> recommendByViralReference(Long viralVideoId);
}

// BgmRecommendVO:
// - style: luxury_piano / energetic_folk / emotional_strings / upbeat_pop / chill_lofi
// - bpmRange: "80-100" / "120-140"
// - mood: 温暖 / 激昂 / 悬疑 / 治愈
// - matchScore: 0-100（匹配度）
// - reason: "脚本情绪曲线呈渐进式升温，推荐节奏逐渐加快的 BGM"
```

### 推荐逻辑
```java
// 情绪曲线 → BGM 映射规则（从 BusinessParamConfig 读取）：
// - 平均情绪值 > 80 → upbeat_pop / energetic_folk（高能量）
// - 平均情绪值 < 50 → emotional_strings / chill_lofi（低能量）
// - 情绪波动大（标准差 > 20）→ 需要节奏变化的 BGM
// - 情绪波动小（标准差 < 10）→ 稳定节奏的 BGM
// - 结尾情绪值 > 开头 → 渐进式 BGM
// - 结尾情绪值 < 开头 → 回落式 BGM
```

**验收**：生成脚本时自动推荐 3 个 BGM 风格，附带匹配度和推荐理由

---

## 任务 2.8 — 多级审批流

**问题**：话术审批仅支持单层（提交→审批），缺少多级审批和条件路由。

**文件**：
- `src/main/java/.../module/live/entity/LiveScriptApproval.java` — 修改
- `src/main/java/.../module/live/service/impl/LiveApprovalServiceImpl.java` — 修改

**改动**：

### 数据库迁移
```sql
-- V022__approval_multi_level.sql
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS approval_level INTEGER DEFAULT 1;
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS max_level INTEGER DEFAULT 1;
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS current_approver_id BIGINT;
ALTER TABLE live_script_approval ADD COLUMN IF NOT EXISTS auto_approve_rule VARCHAR(64);
-- auto_approve_rule: low_risk_auto / high_score_auto / null（需人工审批）
```

### 审批逻辑升级
```java
// 提交审批时：
// 1. 检查话术风险等级（调用 ContentSecurityScanner）
// 2. 低风险 + 历史评分 > 70 → auto_approve（自动通过，max_level=0）
// 3. 中风险 → 单级审批（max_level=1）
// 4. 高风险（含敏感词/合规风险）→ 双级审批（max_level=2，初审+终审）

// 审批通过时：
// 1. 如果 approval_level < max_level → 流转到下一级
// 2. 如果 approval_level == max_level → 最终通过
```

**验收**：
1. 低风险话术自动通过审批
2. 高风险话术需要两级审批
3. 审批流转记录完整

---

## Phase 2 完成标准

- [x] `mvn compile` + `npx tsc --noEmit` 通过
- [x] 短视频脚本生成支持情绪曲线选择，生成内容体现情绪节奏
- [x] RAG 搜索经过 Cross-Encoder 重排，top-1 相关性提升
- [x] 短查询和长查询使用不同的检索权重
- [x] 直播实时面板在指标异常时弹出 AI 建议
- [x] 可创建话术版本 A/B 测试并自动判定胜者
- [x] 版本对比包含语义相似度和效果对比
- [x] 脚本生成时自动推荐 BGM 风格
- [x] 审批流支持自动通过和多级审批
