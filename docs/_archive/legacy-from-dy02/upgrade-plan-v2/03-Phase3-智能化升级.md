# Phase 3 — 智能化升级（4 周）

> 前置：Phase 1 + Phase 2 全部完成
> 目标：知识图谱、因果推理、动态画像、趋势预测 — 从"工具"升级为"智能体"
> 预期：7.5 → 8.2

---

## 任务 3.1 — 知识图谱落地（GraphRAG）

**问题**：`IndustryKnowledgeGraphServiceImpl` 中 Neo4j 未部署，完全降级为关键词检索，无法做多跳推理。

**方案**：不强依赖 Neo4j，用 PostgreSQL + JSONB 实现轻量级知识图谱，后续可迁移到 Neo4j。

**文件**：
- `sql/ai/knowledge-graph-schema.sql` — 新建
- `src/main/resources/db/migration/V023__ai_knowledge_graph.sql` — 新建
- `src/main/java/.../module/ai/entity/AiGraphNode.java` — 新建
- `src/main/java/.../module/ai/entity/AiGraphEdge.java` — 新建
- `src/main/java/.../module/ai/repository/AiGraphNodeRepository.java` — 新建
- `src/main/java/.../module/ai/repository/AiGraphEdgeRepository.java` — 新建
- `src/main/java/.../module/ai/service/KnowledgeGraphService.java` — 新建（替代 IndustryKnowledgeGraphService）
- `src/main/java/.../module/ai/service/impl/KnowledgeGraphServiceImpl.java` — 新建
- `src/main/java/.../module/ai/service/impl/GraphExtractorService.java` — 新建（LLM 驱动实体抽取）

**建表 SQL**：
```sql
-- V023__ai_knowledge_graph.sql

-- 图节点（实体）
CREATE TABLE IF NOT EXISTS ai_graph_node (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL DEFAULT 0,  -- 0=全局共享
    entity_type VARCHAR(32) NOT NULL,     -- product / ingredient / effect / persona / brand / audience / technique
    entity_name VARCHAR(200) NOT NULL,
    properties JSONB DEFAULT '{}',        -- 扩展属性
    source_doc_id BIGINT,                 -- 来源文档ID
    confidence DOUBLE PRECISION DEFAULT 0.8,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX idx_graph_node_type ON ai_graph_node(entity_type, deleted);
CREATE INDEX idx_graph_node_name ON ai_graph_node(entity_name, deleted);
CREATE UNIQUE INDEX uk_graph_node ON ai_graph_node(owner_id, entity_type, entity_name) WHERE deleted = 0;

-- 图边（关系）
CREATE TABLE IF NOT EXISTS ai_graph_edge (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL DEFAULT 0,
    source_node_id BIGINT NOT NULL REFERENCES ai_graph_node(id),
    target_node_id BIGINT NOT NULL REFERENCES ai_graph_node(id),
    relation_type VARCHAR(64) NOT NULL,   -- contains / treats / suits / competes_with / enhances / contradicts
    weight DOUBLE PRECISION DEFAULT 1.0,
    properties JSONB DEFAULT '{}',
    source_doc_id BIGINT,
    confidence DOUBLE PRECISION DEFAULT 0.8,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX idx_graph_edge_source ON ai_graph_edge(source_node_id, deleted);
CREATE INDEX idx_graph_edge_target ON ai_graph_edge(target_node_id, deleted);
CREATE INDEX idx_graph_edge_relation ON ai_graph_edge(relation_type, deleted);
```

### LLM 驱动实体/关系抽取
```java
public class GraphExtractorService {

    // 从文档内容中抽取实体和关系
    public GraphExtractionResult extract(String content, String docType) {
        // Prompt 模板：
        // "请从以下{docType}文本中抽取实体和关系，返回 JSON：
        //  {
        //    entities: [{type, name, properties}...],
        //    relations: [{source, target, relationType, weight}...]
        //  }
        //
        //  实体类型：product(产品), ingredient(成分), effect(功效),
        //           persona(人设), brand(品牌), audience(受众), technique(话术技巧)
        //  关系类型：contains(包含), treats(针对), suits(适合),
        //           competes_with(竞品), enhances(增强), contradicts(矛盾)
        //
        //  文本：{content}"

        // 解析 LLM 返回的 JSON → GraphExtractionResult
        // 去重：检查 ai_graph_node 是否已存在同名实体
        // 合并：已存在则更新 confidence（取较高值）
    }

    // 批量抽取（知识库导入时触发）
    public void batchExtract(Long kbId, Long ownerId) {
        // 遍历知识库所有文档，逐个抽取
        // 异步执行，进度通过 SSE 推送
    }
}
```

### 图查询服务
```java
public interface KnowledgeGraphService {

    // 单跳查询：查找实体的直接关系
    List<GraphRelation> findRelations(String entityName, String relationType, int limit);

    // 多跳查询：从起点到终点的路径（最多 3 跳）
    List<GraphPath> findPaths(String fromEntity, String toEntity, int maxHops);

    // 子图查询：以实体为中心的 N 跳子图
    GraphSubset getSubgraph(String entityName, int hops);

    // 推理查询：基于图结构回答问题
    // 例："玻尿酸适合什么肤质？" → 玻尿酸 -[treats]→ 干燥 -[suits]→ 干性肌肤
    String reasonQuery(String question, Long ownerId);

    // GraphRAG：图检索 + 向量检索融合
    List<RagRetrieveItemVO> graphEnhancedSearch(String query, Long ownerId, int topK);
}
```

### GraphRAG 融合检索
```java
// graphEnhancedSearch 实现：
// 1. 从 query 中抽取关键实体（LLM 或规则）
// 2. 在图中查找相关实体和关系（1-2 跳）
// 3. 将图上下文注入到向量检索的 query 中：
//    enrichedQuery = query + " 相关实体：" + entities + " 关系：" + relations
// 4. 执行混合检索（向量 + BM25 + 图上下文）
// 5. 结果中标注图来源的额外信息
```

**验收**：
1. 知识库导入时自动抽取实体和关系
2. 可查询"玻尿酸→功效→适合人群"的多跳路径
3. GraphRAG 检索结果比纯向量检索更精准

---

## 任务 3.2 — 因果推理引擎升级

**问题**：`IndustryCausalEngineImpl` 本质是因子乘法，不是真正的因果推理。

**文件**：
- `src/main/java/.../module/ai/service/impl/IndustryCausalEngineImpl.java` — 重构
- `src/main/java/.../module/ai/vo/CausalGraphVO.java` — 新建
- `src/main/java/.../module/ai/vo/CounterfactualResultVO.java` — 新建

**改动**：

### 因果图定义
```java
// 定义因果 DAG（有向无环图）：
// 节点：scriptType, persona, productType, timeSlot, audienceSize, retentionRate, interactionRate, conversionRate
// 边（因果关系）：
//   scriptType → retentionRate (直接因果)
//   persona → interactionRate (直接因果)
//   productType → conversionRate (直接因果)
//   timeSlot → audienceSize (直接因果)
//   audienceSize → interactionRate (直接因果)
//   retentionRate → conversionRate (中介效应)
//   interactionRate → conversionRate (中介效应)

// 因果图存储在 BusinessParamConfig 中：
@Data
public static class CausalGraph {
    private List<CausalEdge> edges;  // [{from, to, strength}]
}
```

### 反事实推理
```java
public CounterfactualResultVO counterfactual(Map<String, Object> currentState, Map<String, Object> intervention) {
    // 1. 当前状态：{scriptType: "种草", persona: "专业", productType: "精华", timeSlot: "20:00"}
    // 2. 干预：{scriptType: "促销"}（如果改变话术类型）
    // 3. 计算过程：
    //    a. 沿因果图传播干预效应
    //    b. scriptType 改变 → retentionRate 变化 → conversionRate 变化
    //    c. 使用因果因子计算每一步的变化量
    // 4. 返回：
    //    - 预测的 conversionRate 变化（+2.3% 或 -1.5%）
    //    - 影响路径（scriptType → retentionRate → conversionRate）
    //    - 置信区间（基于历史数据的方差）
    //    - 建议（"切换到促销话术预计提升转化率 2.3%，建议在 20:00-21:00 时段使用"）

    // LLM 辅助推理（增强因果分析的可解释性）：
    // prompt: "基于以下因果关系和数据，分析如果将{intervention}，对转化率的影响：
    //          当前状态：{currentState}
    //          因果图：{causalGraph}
    //          历史数据：{historicalStats}
    //          请给出预测变化、影响路径和置信度"
}
```

### 因果因子自适应学习
```java
// 定期（每周）从实际数据中更新因果因子：
// 1. 从 live_script_effectiveness 聚合：
//    - 按 scriptType 分组的平均转化率
//    - 按 persona 分组的平均互动率
//    - 按 timeSlot 分组的平均在线人数
// 2. 计算实际因子 = 该组平均值 / 全局平均值
// 3. 与配置因子加权融合：
//    updatedFactor = 0.7 * actualFactor + 0.3 * configFactor
// 4. 更新到 BusinessParamConfig（或数据库）
// 5. 记录更新日志
```

**验收**：
1. 可查询"如果改变话术类型，转化率会如何变化"
2. 返回影响路径和置信区间
3. 因果因子每周自动更新

---

## 任务 3.3 — 用户认知画像动态化

**问题**：`UserCognitiveProfileServiceImpl` 返回固定值（0.8/0.6/0.7），无动态学习。

**文件**：
- `sql/ai/user-profile-schema.sql` — 新建
- `src/main/resources/db/migration/V024__ai_user_cognitive_profile.sql` — 新建
- `src/main/java/.../module/ai/entity/AiUserCognitiveProfile.java` — 新建
- `src/main/java/.../module/ai/repository/AiUserCognitiveProfileRepository.java` — 新建
- `src/main/java/.../module/ai/service/impl/UserCognitiveProfileServiceImpl.java` — 重写

**建表 SQL**：
```sql
-- V024__ai_user_cognitive_profile.sql
CREATE TABLE IF NOT EXISTS ai_user_cognitive_profile (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL UNIQUE,
    -- 内容偏好（从历史生成记录统计）
    preferred_script_types JSONB DEFAULT '{}',    -- {"种草": 0.4, "促销": 0.3, "互动": 0.3}
    preferred_styles JSONB DEFAULT '{}',          -- {"专业": 0.5, "亲切": 0.3, "幽默": 0.2}
    preferred_emotion_curves JSONB DEFAULT '{}',  -- {"rollercoaster": 0.6, "slow_build": 0.4}
    -- 使用行为（从操作日志统计）
    avg_edit_ratio DOUBLE PRECISION DEFAULT 0.5,  -- AI生成后的平均编辑比例（0=全采纳, 1=全改写）
    generation_frequency INTEGER DEFAULT 0,        -- 月均生成次数
    preferred_length VARCHAR(16) DEFAULT 'medium', -- short / medium / long
    -- 效果偏好（从效果数据统计）
    optimization_focus VARCHAR(32) DEFAULT 'balanced', -- conversion / retention / interaction / balanced
    risk_tolerance DOUBLE PRECISION DEFAULT 0.5,       -- 0=保守, 1=激进
    -- 学习状态
    profile_version INTEGER DEFAULT 1,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

### 画像更新逻辑
```java
// 触发时机：
// 1. 用户生成话术后（更新 preferred_script_types, preferred_styles）
// 2. 用户编辑 AI 生成内容后（更新 avg_edit_ratio）
// 3. 话术效果评分出来后（更新 optimization_focus）

// 更新算法（指数移动平均）：
// new_value = alpha * current_observation + (1 - alpha) * old_value
// alpha = 0.3（近期数据权重 30%）

// 示例：用户连续 3 次选择"种草"类型
// preferred_script_types["种草"] = 0.3 * 1.0 + 0.7 * 0.4 = 0.58（从 0.4 上升到 0.58）
```

### 画像应用
```java
// 在 LivePromptBuilder.buildPrompt() 中：
// 1. 读取用户画像
// 2. 如果 avg_edit_ratio > 0.7 → 生成更多候选（用户倾向大幅修改）
// 3. 如果 risk_tolerance < 0.3 → 使用保守的 prompt（避免激进表达）
// 4. 如果 optimization_focus == "conversion" → prompt 强调转化话术
// 5. 将偏好注入 prompt：
//    "用户偏好：{preferred_styles}风格，{preferred_length}长度，
//     关注{optimization_focus}指标，风险偏好{risk_tolerance}"
```

**验收**：
1. 用户画像随使用行为动态更新
2. 不同用户生成的话术风格有差异
3. 画像数据可在前端查看

---

## 任务 3.4 — 趋势预测引擎

**问题**：`TrendMonitorServiceImpl` 仅聚合实时热搜，无预测能力。

**文件**：
- `src/main/java/.../module/ai/service/impl/TrendMonitorServiceImpl.java` — 修改
- `src/main/java/.../module/ai/vo/TrendPredictionVO.java` — 新建

**改动**：

### 趋势生命周期分析
```java
public class TrendLifecycle {
    private String phase;       // emerging / rising / peak / declining / dead
    private double momentum;    // 动量（热度变化率）
    private int estimatedPeakHours;  // 预计达到峰值的小时数
    private double currentHeat;
    private double predictedPeakHeat;
}

// 生命周期判断逻辑：
// 1. 采集最近 24 小时的热度数据（每小时一个点）
// 2. 计算动量 = (当前热度 - 6小时前热度) / 6小时前热度
// 3. 判断阶段：
//    - momentum > 0.5 → emerging（新兴，快速上升）
//    - momentum > 0.1 → rising（上升中）
//    - momentum ∈ [-0.1, 0.1] → peak（峰值附近）
//    - momentum < -0.1 → declining（下降中）
//    - momentum < -0.5 或 currentHeat < 阈值 → dead（已过时）
```

### 热点时间窗口策略
```java
public class HotspotWindow {
    private String windowType;  // golden / silver / bronze / expired
    private int remainingHours; // 剩余有效时间
    private String advice;      // "黄金窗口，建议立即创作"
}

// 窗口判断：
// - golden（黄金，0-6小时）：热点刚爆发，立即创作可获最大流量
// - silver（白银，6-24小时）：热点上升期，仍有较大流量
// - bronze（青铜，24-72小时）：热点稳定期，需差异化角度
// - expired（过期，>72小时）：热点已过，不建议追
```

### LLM 辅助趋势分析
```java
// 对 emerging 阶段的热点，调用 LLM 分析：
// prompt: "以下是一个新兴热点话题：{topic}
//          当前热度：{heat}，上升速度：{momentum}
//          请分析：
//          1. 该话题与护肤品/彩妆直播的关联度（0-10）
//          2. 推荐的内容角度（3个）
//          3. 预计热度持续时间
//          4. 风险提示（是否涉及敏感话题）"
```

**验收**：
1. 热点列表显示生命周期阶段和时间窗口
2. 黄金窗口热点优先展示
3. 新兴热点自动生成内容角度建议

---

## 任务 3.5 — 战略规划服务真实化

**问题**：`StrategicPlanningServiceImpl` 完全硬编码模板，无数据驱动。

**文件**：
- `src/main/java/.../module/ai/service/impl/StrategicPlanningServiceImpl.java` — 重写

**改动**：

### 数据驱动的战略规划
```java
public StrategicPlan generatePlan(Long ownerId) {
    // 1. 收集数据
    AccountStats stats = getAccountStats(ownerId);           // 粉丝数、增长率、内容数
    EffectivenessStats effectiveness = getEffectiveness(ownerId); // 平均转化率、停留率
    ContentAnalysis content = analyzeContent(ownerId);       // 内容类型分布、发布频率
    TrendAnalysis trends = analyzeTrends();                  // 当前热点与账号的匹配度

    // 2. 诊断（基于数据）
    List<Diagnosis> diagnoses = new ArrayList<>();
    if (stats.getGrowthRate() < 0.05) diagnoses.add(new Diagnosis("增长停滞", "月增长率仅" + stats.getGrowthRate()));
    if (effectiveness.getAvgConversion() < 0.02) diagnoses.add(new Diagnosis("转化率低", "平均转化率" + effectiveness.getAvgConversion()));
    if (content.getPostFrequency() < 3) diagnoses.add(new Diagnosis("更新频率低", "周均发布" + content.getPostFrequency() + "条"));

    // 3. LLM 生成个性化建议
    String prompt = buildStrategicPrompt(stats, effectiveness, content, trends, diagnoses);
    String llmAdvice = llmClient.chat(systemPrompt, prompt);

    // 4. 结构化输出
    return new StrategicPlan(
        diagnoses,
        parseGoals(llmAdvice),        // 短期/中期/长期目标
        parseActions(llmAdvice),      // 具体行动项
        parseTimeline(llmAdvice),     // 时间线
        parseMilestones(llmAdvice)    // 里程碑
    );
}
```

**验收**：
1. 战略规划基于账号真实数据生成
2. 不同账号的规划内容不同
3. 包含具体的行动项和时间线

---

## 任务 3.6 — 账号诊断真实数据接入

**问题**：`AccountDiagnosisServiceImpl` 中 clarity/competitiveness 等指标固定为 0.7/0.65。

**文件**：
- `src/main/java/.../module/ai/service/impl/AccountDiagnosisServiceImpl.java` — 修改

**改动**：

### 真实指标计算
```java
// clarity（定位清晰度）：
// 1. 从 dy_persona 读取账号人设描述
// 2. 从最近 30 条内容中提取关键词
// 3. 计算关键词与人设描述的重合度
// 4. 重合度 > 0.7 → clarity = 0.8+
// 5. 重合度 < 0.3 → clarity = 0.3-（定位模糊）

// competitiveness（竞争力）：
// 1. 从 sv_video_data 读取最近 30 天的视频数据
// 2. 计算平均播放量、点赞率、完播率
// 3. 与同类型账号的平均值对比（从 BusinessParamConfig 读取行业基准）
// 4. 高于基准 → competitiveness > 0.7
// 5. 低于基准 → competitiveness < 0.5

// contentQuality（内容质量）：
// 1. 从 ai_knowledge_quality_score 聚合该用户文档的平均质量分
// 2. 从 live_script_effectiveness 聚合话术平均效果分
// 3. 综合评分 = 文档质量 * 0.4 + 话术效果 * 0.6

// growthPotential（增长潜力）：
// 1. 粉丝增长率趋势（最近 3 个月）
// 2. 内容发布频率趋势
// 3. 互动率趋势
// 4. 三项趋势的加权平均
```

### application.yml 行业基准
```yaml
app:
  business:
    industry-benchmark:
      skincare:
        avg-view-count: 5000
        avg-like-rate: 0.05
        avg-completion-rate: 0.35
        avg-share-rate: 0.02
      cosmetics:
        avg-view-count: 8000
        avg-like-rate: 0.06
        avg-completion-rate: 0.30
        avg-share-rate: 0.025
```

**验收**：
1. 账号诊断返回基于真实数据的指标
2. 不同账号的诊断结果不同
3. 指标与行业基准对比有参考意义

---

## Phase 3 完成标准

- [x] `mvn compile` + `npx tsc --noEmit` 通过
- [x] 知识库导入时自动抽取实体和关系到图表
- [x] 可查询多跳关系路径（如"玻尿酸→功效→适合人群"）
- [x] GraphRAG 检索结果比纯向量检索更精准
- [x] 因果推理支持反事实查询（"如果改变X，Y会如何变化"）
- [x] 因果因子每周自动从实际数据更新
- [x] 用户画像随使用行为动态更新
- [x] 热点显示生命周期阶段和时间窗口
- [x] 战略规划基于真实数据生成
- [x] 账号诊断指标来自真实数据
