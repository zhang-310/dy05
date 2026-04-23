# 智能推荐优化 - 机器学习模型设计

## 当前状态分析

### 现有推荐逻辑（规则引擎）
位置：`StylePresetServiceImpl.recommendStyles()`

当前使用基于规则的推荐系统：
1. **产品分类规则** - 护肤品→专业/温暖，彩妆→热情/时尚
2. **价格区间规则** - 高端(≥500)→专业/优雅，中端(200-500)→温暖/亲和，平价(<200)→热情/活力
3. **卖点关键词规则** - 科技/成分→专业，天然/温和→温暖，网红/爆款→时尚

**局限性**：
- 规则固定，无法学习用户反馈
- 无法捕捉复杂的特征组合
- 推荐准确度依赖人工规则质量
- 无法利用历史效果数据优化

### 现有效果评分系统
位置：`EffectivenessScoreServiceImpl`

已有完整的话术效果评分体系：
- 使用次数得分（0-25分）
- 转化率得分（0-15分）
- 互动得分（点赞+评论，0-10分）
- 基础分（50分）
- 总分范围：0-100分

**优势**：
- 有丰富的历史效果数据可用于训练
- 评分维度全面（使用、转化、互动）
- 已有趋势分析和对比功能

## 机器学习优化方案

### 方案一：协同过滤推荐（推荐）

**原理**：基于用户-商品-风格的历史效果数据，找出相似商品的最佳风格

**优势**：
- 实现简单，无需复杂特征工程
- 可以发现隐藏的风格偏好模式
- 适合冷启动场景（可回退到规则引擎）

**实现步骤**：
1. 构建商品-风格效果矩阵（使用历史评分数据）
2. 计算商品相似度（基于分类、价格、卖点等特征）
3. 为新商品推荐相似商品的高分风格
4. 结合规则引擎作为冷启动策略

**数据需求**：
- 商品特征：分类、价格、卖点、描述
- 历史效果：风格-评分对应关系
- 最少数据量：50个商品 × 3个风格 = 150条记录

### 方案二：梯度提升树（XGBoost/LightGBM）

**原理**：训练分类模型，输入商品特征，输出最佳风格

**优势**：
- 可解释性强，能看到特征重要性
- 处理非线性关系能力强
- 对缺失值和异常值鲁棒

**实现步骤**：
1. 特征工程：提取商品特征向量
2. 标签构建：历史最高分风格作为正样本
3. 模型训练：多分类任务（8-10个风格类别）
4. 在线推理：输入商品特征，输出Top-K风格

**数据需求**：
- 最少训练样本：200个商品 × 平均3个风格 = 600条记录
- 特征维度：10-20维（分类、价格、文本特征等）

### 方案三：深度学习（神经网络）

**原理**：使用文本嵌入+DNN，处理商品描述和卖点文本

**优势**：
- 可以利用商品描述的语义信息
- 端到端学习，无需手工特征工程
- 泛化能力强

**劣势**：
- 需要大量训练数据（>1000样本）
- 训练和推理成本高
- 可解释性差

**不推荐原因**：当前数据量可能不足，且系统复杂度过高

## 推荐实施方案：混合推荐系统

### 架构设计

```
┌─────────────────────────────────────────────┐
│         智能推荐引擎（Hybrid）               │
├─────────────────────────────────────────────┤
│                                             │
│  ┌──────────────┐      ┌─────────────────┐ │
│  │ 规则引擎      │      │ 协同过滤模型     │ │
│  │ (冷启动)      │      │ (主推荐)        │ │
│  └──────────────┘      └─────────────────┘ │
│         │                      │            │
│         └──────────┬───────────┘            │
│                    ▼                        │
│         ┌──────────────────┐                │
│         │  融合策略         │                │
│         │  (加权平均)       │                │
│         └──────────────────┘                │
│                    │                        │
│                    ▼                        │
│         ┌──────────────────┐                │
│         │  Top-K 风格输出   │                │
│         └──────────────────┘                │
└─────────────────────────────────────────────┘
```

### 实现细节

#### 1. 数据准备层
```java
// 新建 Service: StyleRecommendationMLService
public interface StyleRecommendationMLService {

    /**
     * 训练推荐模型（定时任务触发）
     */
    void trainModel(Long userId);

    /**
     * 基于ML模型推荐风格
     */
    List<StyleRecommendationVO> recommendWithML(Long productId, Long userId);

    /**
     * 获取模型性能指标
     */
    ModelMetricsVO getModelMetrics(Long userId);
}
```

#### 2. 特征提取
```java
public class ProductFeatureExtractor {

    public double[] extractFeatures(DyProduct product) {
        // 1. 分类特征（One-Hot编码）
        // 2. 价格特征（归一化）
        // 3. 文本特征（TF-IDF或词嵌入）
        // 4. 历史效果特征（平均评分、使用次数）
        return features;
    }
}
```

#### 3. 协同过滤实现
```java
public class CollaborativeFilteringRecommender {

    /**
     * 计算商品相似度（余弦相似度）
     */
    public double calculateSimilarity(DyProduct p1, DyProduct p2) {
        double[] f1 = extractFeatures(p1);
        double[] f2 = extractFeatures(p2);
        return cosineSimilarity(f1, f2);
    }

    /**
     * 推荐风格
     */
    public List<String> recommend(Long productId, int topK) {
        // 1. 找到最相似的N个商品
        // 2. 获取这些商品的高分风格
        // 3. 按平均评分排序，返回Top-K
    }
}
```

#### 4. 混合推荐策略
```java
public class HybridRecommendationStrategy {

    public List<String> recommend(Long productId, Long userId) {
        // 检查是否有足够的历史数据
        int historyCount = getHistoryCount(productId);

        if (historyCount < THRESHOLD) {
            // 冷启动：使用规则引擎
            return ruleBasedRecommender.recommend(productId);
        } else {
            // 混合推荐：70% ML + 30% 规则
            List<String> mlResults = mlRecommender.recommend(productId);
            List<String> ruleResults = ruleBasedRecommender.recommend(productId);
            return mergeResults(mlResults, ruleResults, 0.7, 0.3);
        }
    }
}
```

### 数据库设计

#### 新增表：style_recommendation_model
```sql
CREATE TABLE style_recommendation_model (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    model_type VARCHAR(50) NOT NULL,  -- 'collaborative_filtering', 'rule_based', 'hybrid'
    model_version VARCHAR(20) NOT NULL,
    model_data TEXT,  -- JSON格式存储模型参数
    training_samples INTEGER,
    accuracy DECIMAL(5,4),
    precision_score DECIMAL(5,4),
    recall_score DECIMAL(5,4),
    f1_score DECIMAL(5,4),
    trained_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_srm_user_active ON style_recommendation_model(user_id, is_active, deleted);
```

#### 新增表：style_recommendation_feedback
```sql
CREATE TABLE style_recommendation_feedback (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    recommended_styles TEXT,  -- JSON数组
    selected_style VARCHAR(50),
    effectiveness_score DECIMAL(5,2),
    user_id BIGINT NOT NULL,
    feedback_type VARCHAR(20),  -- 'implicit', 'explicit'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_srf_product ON style_recommendation_feedback(product_id, deleted);
CREATE INDEX idx_srf_user ON style_recommendation_feedback(user_id, deleted);
```

### 评估指标

1. **准确率（Accuracy）**：推荐的风格中，最终被选择的比例
2. **Top-3命中率**：推荐的前3个风格中，是否包含最终高分风格
3. **平均评分提升**：使用ML推荐后，话术平均评分的提升幅度
4. **A/B测试对比**：规则引擎 vs ML推荐的效果对比

### 实施计划

#### Phase 1: 数据收集与分析（1-2天）
- [ ] 统计现有商品数量和风格分布
- [ ] 分析历史效果数据质量
- [ ] 确定特征工程方案

#### Phase 2: 协同过滤实现（2-3天）
- [ ] 实现商品相似度计算
- [ ] 实现协同过滤推荐算法
- [ ] 单元测试和准确率评估

#### Phase 3: 混合推荐系统（1-2天）
- [ ] 实现混合推荐策略
- [ ] 集成到现有推荐接口
- [ ] 添加A/B测试支持

#### Phase 4: 模型训练与优化（1-2天）
- [ ] 实现定时训练任务
- [ ] 添加模型版本管理
- [ ] 性能监控和日志

#### Phase 5: 前端集成与测试（1天）
- [ ] 前端展示推荐置信度
- [ ] 用户反馈收集
- [ ] 端到端测试

**总计：6-10天**

## 技术栈选择

### Java ML库推荐
1. **Apache Commons Math** - 基础数学计算（相似度、矩阵运算）
2. **Smile (Statistical Machine Intelligence & Learning Engine)** - 轻量级ML库
3. **DL4J (DeepLearning4J)** - 如果需要深度学习（不推荐初期使用）

### 推荐使用：Apache Commons Math + 自实现协同过滤
- 依赖轻量，易于集成
- 可控性强，便于调试
- 满足当前需求

## 风险与应对

### 风险1：训练数据不足
**应对**：
- 保留规则引擎作为冷启动策略
- 使用迁移学习（借鉴其他用户的模型）
- 主动引导用户生成更多样本

### 风险2：模型过拟合
**应对**：
- 使用交叉验证
- 限制模型复杂度
- 定期重新训练

### 风险3：推荐效果不佳
**应对**：
- A/B测试对比规则引擎
- 收集用户反馈持续优化
- 提供手动调整推荐结果的能力

## 下一步行动

1. 确认当前数据量是否满足训练需求
2. 选择实施方案（推荐：混合推荐系统）
3. 开始Phase 1数据收集与分析
