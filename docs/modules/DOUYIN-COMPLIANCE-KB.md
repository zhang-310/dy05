# 抖音违规知识库构建方案

**生成日期**: 2026-05-10  
**优先级**: P0 - 严重（合规风险）  
**目标**: 从抖音官方文档提取违规规则，构建完整的违规检测知识库

---

## 问题严重性

### 为什么这是 P0 严重问题

1. **法律风险**: 违规内容可能导致账号封禁、罚款、法律诉讼
2. **业务风险**: 直播间被封、商品下架、GMV 损失
3. **品牌风险**: 违规行为影响品牌形象和用户信任
4. **用户风险**: 用户使用平台生成的内容违规，平台有连带责任

### 当前状态

❌ **缺失**：
- 无抖音违规规则知识库
- 话术生成无违规检测
- 短视频策划无合规审查
- 素材使用无版权检查

⚠️ **风险**：
- AI 生成的话术可能包含违规内容
- 用户上传的素材可能侵权
- 直播过程中可能出现违规行为

---

## 抖音违规规则分类

### 1. 直播违规规则

#### 1.1 内容违规

**政治敏感**:
- 违规点：涉及国家领导人、政治事件、敏感话题
- 示例：提及政治人物、讨论政治话题
- 处罚：永久封禁

**色情低俗**:
- 违规点：色情、低俗、性暗示内容
- 示例：穿着暴露、性暗示动作、低俗语言
- 处罚：封禁 7-30 天

**暴力血腥**:
- 违规点：暴力、血腥、恐怖内容
- 示例：打架斗殴、血腥画面、恐怖场景
- 处罚：封禁 7-30 天

**虚假宣传**:
- 违规点：夸大功效、虚假承诺、误导消费者
- 示例：
  - "包治百病"
  - "7 天瘦 20 斤"
  - "100% 有效"
  - "国家认证"（无证据）
- 处罚：封禁 3-7 天 + 罚款

**违法违规**:
- 违规点：涉及违法犯罪、赌博、毒品
- 示例：售卖违禁品、教唆犯罪
- 处罚：永久封禁 + 报警

#### 1.2 行为违规

**诱导行为**:
- 违规点：诱导关注、点赞、分享、私信
- 示例：
  - "关注主播送礼物"
  - "点赞破 1000 抽奖"
  - "私信领优惠券"
- 处罚：限流 + 封禁 1-3 天

**刷量作弊**:
- 违规点：刷粉丝、刷观看、刷互动
- 示例：购买僵尸粉、机器人互动
- 处罚：封禁 7-30 天

**违规引流**:
- 违规点：引导用户到站外交易
- 示例：
  - "加微信下单"
  - "淘宝搜索 XXX"
  - 展示二维码、联系方式
- 处罚：封禁 3-7 天

**恶意营销**:
- 违规点：骚扰用户、恶意竞争
- 示例：辱骂竞品、恶意刷屏
- 处罚：封禁 3-7 天

#### 1.3 商品违规

**假货三无**:
- 违规点：售卖假货、三无产品
- 示例：无生产日期、无厂家、无合格证
- 处罚：永久封禁 + 罚款

**禁售商品**:
- 违规点：售卖国家禁止的商品
- 示例：
  - 药品（需资质）
  - 医疗器械（需资质）
  - 烟草
  - 野生动物制品
  - 管制刀具
- 处罚：永久封禁

**虚假发货**:
- 违规点：不发货、虚假物流
- 示例：下单后不发货、物流信息造假
- 处罚：封禁 7-30 天 + 赔偿

**价格欺诈**:
- 违规点：虚假原价、价格误导
- 示例：
  - "原价 999，现价 99"（无证据）
  - 先提价再打折
- 处罚：封禁 3-7 天 + 罚款

---

### 2. 短视频违规规则

#### 2.1 普通短视频违规

**内容违规**（同直播）:
- 政治敏感
- 色情低俗
- 暴力血腥
- 虚假宣传
- 违法违规

**标题违规**:
- 违规点：标题党、误导性标题
- 示例：
  - "震惊！XXX"
  - "不看后悔"
  - "XXX 竟然..."
- 处罚：限流 + 删除视频

**封面违规**:
- 违规点：封面与内容不符、低俗封面
- 示例：性感封面、血腥封面
- 处罚：限流 + 删除视频

**搬运抄袭**:
- 违规点：搬运他人视频、抄袭创意
- 示例：直接搬运、去水印
- 处罚：限流 + 删除视频

#### 2.2 素材违规

**音乐侵权**:
- 违规点：使用未授权音乐
- 示例：使用商业音乐、明星歌曲
- 处罚：删除视频 + 扣分

**图片侵权**:
- 违规点：使用未授权图片
- 示例：使用他人摄影作品、商业图片
- 处罚：删除视频 + 扣分

**视频素材侵权**:
- 违规点：使用未授权视频素材
- 示例：使用电影片段、电视剧片段
- 处罚：删除视频 + 扣分

**字体侵权**:
- 违规点：使用未授权商业字体
- 示例：方正字体、汉仪字体
- 处罚：删除视频 + 扣分

---

## 知识库构建方案

### 数据结构设计

#### 违规规则表（compliance_rule）

```sql
CREATE TABLE compliance_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE,        -- 规则编码（如 LIVE_CONTENT_POLITICAL）
    category VARCHAR(32) NOT NULL,                -- 分类（live/video/material）
    sub_category VARCHAR(32),                     -- 子分类（content/behavior/product）
    rule_name VARCHAR(128) NOT NULL,              -- 规则名称
    description TEXT NOT NULL,                    -- 规则描述
    severity VARCHAR(16) NOT NULL,                -- 严重程度（critical/high/medium/low）
    punishment TEXT,                              -- 处罚措施
    examples TEXT,                                -- 违规示例（JSON 数组）
    keywords TEXT,                                -- 关键词（JSON 数组）
    patterns TEXT,                                -- 正则表达式（JSON 数组）
    embedding VECTOR(1536),                       -- 向量化（用于语义检索）
    source_url TEXT,                              -- 来源 URL
    effective_date DATE,                          -- 生效日期
    status INTEGER DEFAULT 1,                     -- 状态（1=启用 0=禁用）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_compliance_rule_category ON compliance_rule(category, sub_category);
CREATE INDEX idx_compliance_rule_severity ON compliance_rule(severity);
CREATE INDEX idx_compliance_rule_embedding ON compliance_rule USING ivfflat(embedding vector_cosine_ops);
```

#### 违规检测记录表（compliance_check_log）

```sql
CREATE TABLE compliance_check_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_type VARCHAR(32) NOT NULL,            -- 内容类型（script/video/material）
    content_id BIGINT,                            -- 内容 ID
    content_text TEXT,                            -- 检测内容
    check_result VARCHAR(16) NOT NULL,            -- 检测结果（pass/warning/reject）
    matched_rules TEXT,                           -- 匹配的规则（JSON 数组）
    risk_score DECIMAL(5,2),                      -- 风险评分（0-100）
    suggestions TEXT,                             -- 修改建议
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_compliance_check_log_user ON compliance_check_log(user_id, check_time);
CREATE INDEX idx_compliance_check_log_content ON compliance_check_log(content_type, content_id);
```

---

### 数据来源

#### 官方文档

1. **抖音直播规范**:
   - https://www.douyin.com/creator/live/rules
   - 直播内容规范
   - 直播行为规范
   - 商品管理规范

2. **抖音短视频规范**:
   - https://www.douyin.com/creator/video/rules
   - 内容创作规范
   - 版权保护规范
   - 社区公约

3. **抖音电商规范**:
   - https://fxg.jinritemai.com/
   - 商品发布规范
   - 营销推广规范
   - 售后服务规范

#### 数据提取方式

**方式 1：手动整理**（推荐）
- 人工阅读官方文档
- 提取违规点、示例、处罚措施
- 整理为结构化数据
- 录入知识库

**方式 2：爬虫 + AI 提取**
- 爬取官方文档页面
- 使用 LLM 提取结构化信息
- 人工审核校对
- 录入知识库

**方式 3：API 集成**（如果有）
- 调用抖音官方 API
- 获取最新规则
- 自动同步到知识库

---

### 违规检测流程

#### 1. 关键词匹配（快速筛查）

```java
public class KeywordMatcher {
    // 敏感词库
    private Set<String> politicalKeywords = Set.of("政治敏感词...");
    private Set<String> pornKeywords = Set.of("色情词汇...");
    private Set<String> violenceKeywords = Set.of("暴力词汇...");
    
    public List<String> matchKeywords(String content) {
        List<String> matched = new ArrayList<>();
        for (String keyword : allKeywords) {
            if (content.contains(keyword)) {
                matched.add(keyword);
            }
        }
        return matched;
    }
}
```

#### 2. 正则表达式匹配（模式识别）

```java
public class PatternMatcher {
    // 违规模式
    private Pattern phonePattern = Pattern.compile("1[3-9]\\d{9}");
    private Pattern wechatPattern = Pattern.compile("微信|WeChat|VX");
    private Pattern pricePattern = Pattern.compile("原价.*现价");
    
    public List<String> matchPatterns(String content) {
        List<String> matched = new ArrayList<>();
        if (phonePattern.matcher(content).find()) {
            matched.add("包含手机号");
        }
        // ...
        return matched;
    }
}
```

#### 3. 语义检测（AI 理解）

```java
public class SemanticChecker {
    @Resource
    private KnowledgeBaseService kbService;
    
    public ComplianceResult checkSemantic(String content) {
        // 1. 向量化内容
        float[] embedding = embeddingService.embed(content);
        
        // 2. 向量检索相似规则
        List<ComplianceRule> rules = kbService.searchSimilarRules(
            embedding, 
            0.8,  // 相似度阈值
            10    // top-k
        );
        
        // 3. LLM 判断是否违规
        String prompt = String.format(
            "内容：%s\n\n规则：%s\n\n判断是否违规，给出理由和修改建议。",
            content,
            rules.stream().map(ComplianceRule::getDescription).collect(Collectors.joining("\n"))
        );
        
        String result = llmClient.chat(prompt);
        
        return parseResult(result);
    }
}
```

#### 4. 综合评分

```java
public class ComplianceScorer {
    public ComplianceResult score(String content) {
        int score = 100;
        List<String> issues = new ArrayList<>();
        
        // 关键词匹配（-30 分/个）
        List<String> keywords = keywordMatcher.match(content);
        score -= keywords.size() * 30;
        issues.addAll(keywords);
        
        // 模式匹配（-20 分/个）
        List<String> patterns = patternMatcher.match(content);
        score -= patterns.size() * 20;
        issues.addAll(patterns);
        
        // 语义检测（-50 分）
        ComplianceResult semantic = semanticChecker.check(content);
        if (semantic.isViolation()) {
            score -= 50;
            issues.add(semantic.getReason());
        }
        
        // 判定结果
        String result;
        if (score >= 80) {
            result = "pass";
        } else if (score >= 60) {
            result = "warning";
        } else {
            result = "reject";
        }
        
        return new ComplianceResult(result, score, issues);
    }
}
```

---

### 集成到业务流程

#### 1. 话术生成时检测

```java
@Service
public class LiveScriptGenerationService {
    @Resource
    private ComplianceService complianceService;
    
    public LiveScript generateScript(LiveScriptGenerationRequest req) {
        // 1. AI 生成话术
        String content = aiService.generate(req);
        
        // 2. 违规检测
        ComplianceResult result = complianceService.check(
            "script", 
            content
        );
        
        // 3. 处理结果
        if (result.getResult().equals("reject")) {
            // 拒绝：提示用户修改
            throw new BusinessException(
                ErrorCode.COMPLIANCE_VIOLATION,
                "话术包含违规内容：" + result.getIssues()
            );
        } else if (result.getResult().equals("warning")) {
            // 警告：标记风险，给出建议
            script.setViolationChecked(true);
            script.setViolationResult("warning: " + result.getIssues());
            script.setAiSuggestion(result.getSuggestions());
        } else {
            // 通过：正常保存
            script.setViolationChecked(true);
            script.setViolationResult("pass");
        }
        
        return script;
    }
}
```

#### 2. 短视频策划时检测

```java
@Service
public class VideoScriptService {
    @Resource
    private ComplianceService complianceService;
    
    public VideoScript saveScript(VideoScriptSaveVO vo) {
        // 1. 检测标题
        ComplianceResult titleResult = complianceService.check(
            "video_title", 
            vo.getTitle()
        );
        
        // 2. 检测内容
        ComplianceResult contentResult = complianceService.check(
            "video_content", 
            vo.getContent()
        );
        
        // 3. 检测素材
        if (vo.getMaterialIds() != null) {
            for (Long materialId : vo.getMaterialIds()) {
                Material material = materialService.getById(materialId);
                ComplianceResult materialResult = complianceService.checkMaterial(material);
                if (materialResult.getResult().equals("reject")) {
                    throw new BusinessException(
                        ErrorCode.MATERIAL_VIOLATION,
                        "素材违规：" + materialResult.getIssues()
                    );
                }
            }
        }
        
        // 4. 保存
        VideoScript script = new VideoScript();
        // ...
        return videoScriptRepository.save(script);
    }
}
```

#### 3. 实时监控（直播中）

```java
@Service
public class LiveMonitorService {
    @Resource
    private ComplianceService complianceService;
    
    @Scheduled(fixedDelay = 30000) // 每 30 秒检测一次
    public void monitorLiveSessions() {
        // 1. 获取正在直播的场次
        List<LiveSession> sessions = liveSessionService.getOngoingSessions();
        
        for (LiveSession session : sessions) {
            // 2. 获取当前话术
            LiveScript currentScript = liveScriptService.getCurrentScript(session.getId());
            
            // 3. 违规检测
            ComplianceResult result = complianceService.check(
                "live_script", 
                currentScript.getScriptContent()
            );
            
            // 4. 风险预警
            if (result.getResult().equals("reject")) {
                // 发送企业微信预警
                wecomService.sendAlert(
                    session.getUserId(),
                    "直播违规预警",
                    "场次：" + session.getLiveTitle() + "\n" +
                    "话术：" + currentScript.getScriptContent() + "\n" +
                    "违规：" + result.getIssues()
                );
            }
        }
    }
}
```

---

## 实施计划

### 阶段 1：数据收集（1 周）

**任务**:
1. 收集抖音官方文档
2. 整理违规规则清单
3. 提取关键词、示例、处罚措施
4. 建立初始知识库（至少 100 条规则）

**交付物**:
- 违规规则清单（Excel）
- 知识库 SQL 脚本

**工作量**: 2 人日

---

### 阶段 2：知识库构建（1 周）

**任务**:
1. 创建数据库表
2. 录入违规规则
3. 向量化规则描述
4. 建立索引

**交付物**:
- 数据库表结构
- 100+ 条违规规则数据
- 向量索引

**工作量**: 2 人日

---

### 阶段 3：检测引擎开发（2 周）

**任务**:
1. 开发关键词匹配器
2. 开发正则表达式匹配器
3. 开发语义检测器
4. 开发综合评分器
5. 单元测试

**交付物**:
- ComplianceService
- 单元测试（覆盖率 > 80%）

**工作量**: 5 人日

---

### 阶段 4：业务集成（1 周）

**任务**:
1. 集成到话术生成
2. 集成到短视频策划
3. 集成到实时监控
4. 前端展示违规提示

**交付物**:
- 话术生成违规检测
- 短视频策划违规检测
- 实时监控预警

**工作量**: 3 人日

---

### 阶段 5：测试与优化（1 周）

**任务**:
1. 功能测试
2. 性能测试
3. 准确率测试
4. 优化误报率

**交付物**:
- 测试报告
- 性能报告
- 准确率报告（> 90%）

**工作量**: 2 人日

---

## 总工作量

| 阶段 | 任务 | 工作量 | 完成时间 |
|------|------|--------|---------|
| 阶段 1 | 数据收集 | 2 人日 | 1 周 |
| 阶段 2 | 知识库构建 | 2 人日 | 1 周 |
| 阶段 3 | 检测引擎开发 | 5 人日 | 2 周 |
| 阶段 4 | 业务集成 | 3 人日 | 1 周 |
| 阶段 5 | 测试与优化 | 2 人日 | 1 周 |
| **总计** | — | **14 人日** | **约 3 周** |

---

## 验收标准

### 功能验收

- [ ] 知识库包含 100+ 条违规规则
- [ ] 支持关键词匹配
- [ ] 支持正则表达式匹配
- [ ] 支持语义检测
- [ ] 支持综合评分
- [ ] 集成到话术生成
- [ ] 集成到短视频策划
- [ ] 集成到实时监控

### 性能验收

- [ ] 单次检测响应时间 < 500ms
- [ ] 支持并发检测（100 QPS）
- [ ] 向量检索响应时间 < 100ms

### 准确率验收

- [ ] 准确率 > 90%（人工标注 100 条样本）
- [ ] 误报率 < 10%
- [ ] 漏报率 < 5%

---

## 风险与缓解

### 技术风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 规则更新频繁 | 高 | 高 | 建立规则更新机制，定期同步官方文档 |
| 语义检测准确率低 | 高 | 中 | 人工审核 + 持续优化 |
| 性能瓶颈 | 中 | 低 | 缓存 + 异步检测 |

### 业务风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 误报率高影响用户体验 | 高 | 中 | 分级处理（拒绝/警告/通过） |
| 漏报导致违规 | 高 | 低 | 人工复审 + 用户举报 |

---

## 下一步行动

### 立即执行（本周）

1. **收集抖音官方文档**
2. **整理违规规则清单**（至少 100 条）
3. **创建数据库表结构**
4. **录入初始数据**

### 本月目标

1. 完成知识库构建
2. 完成检测引擎开发
3. 完成业务集成
4. 上线违规检测功能

---

**报告生成时间**: 2026-05-10  
**负责人**: 后端 + AI 团队  
**下次审查**: 完成阶段 1 后（1 周后）
