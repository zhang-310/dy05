# 抖音违规知识库构建完成报告

**完成时间**: 2026-05-10  
**优先级**: P0 - 严重（合规风险）  
**状态**: ✅ 已完成

---

## 已完成内容

### 1. 数据库设计 ✅

**文件**: `sql/compliance/schema.sql`

创建了 4 张表：

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| compliance_rule | 违规规则表 | rule_code, category, sub_category, severity, keywords, patterns, embedding |
| compliance_check_log | 违规检测记录表 | user_id, content_type, check_result, risk_score, matched_rules |
| compliance_keyword | 敏感词库表 | keyword, category, severity, replacement |
| compliance_case | 违规案例库表 | rule_id, case_content, violation_reason, punishment_result |

**特性**：
- 支持向量检索（embedding VECTOR(1536)）
- 逻辑删除（deleted 字段）
- 状态管理（status 字段）
- 时间戳自动维护

### 2. 初始数据 ✅

**文件**: `sql/compliance/init_data.sql`

录入了 **20+ 条违规规则**，覆盖：

**直播违规**（13 条）：
- 内容违规：政治敏感、色情低俗、暴力血腥、虚假宣传、违法违规
- 行为违规：诱导行为、刷量作弊、违规引流、恶意营销
- 商品违规：假货三无、禁售商品、虚假发货、价格欺诈

**短视频违规**（3 条）：
- 标题党、封面违规、搬运抄袭

**素材违规**（4 条）：
- 音乐侵权、图片侵权、视频素材侵权、字体侵权

**文件**: `sql/compliance/init_keywords.sql`

录入了 **50+ 个敏感词**，分为 7 大类：
- political（政治敏感）
- porn（色情低俗）
- violence（暴力血腥）
- fraud（虚假宣传）
- divert（违规引流）
- induce（诱导行为）
- banned（禁售商品）

### 3. 后端实体层 ✅

创建了 4 个 JPA 实体：

| 实体类 | 文件路径 | 特性 |
|--------|---------|------|
| ComplianceRule | common/compliance/entity/ComplianceRule.java | @SQLRestriction, @PrePersist, @PreUpdate |
| ComplianceCheckLog | common/compliance/entity/ComplianceCheckLog.java | @PrePersist |
| ComplianceKeyword | common/compliance/entity/ComplianceKeyword.java | @SQLRestriction, @PrePersist, @PreUpdate |
| ComplianceCase | common/compliance/entity/ComplianceCase.java | @SQLRestriction, @PrePersist, @PreUpdate |

### 4. Repository 层 ✅

创建了 4 个 Repository 接口：

| Repository | 文件路径 | 关键方法 |
|-----------|---------|---------|
| ComplianceRuleRepository | common/compliance/repository/ComplianceRuleRepository.java | findByCategoryAndEnabled, findBySeverityAndEnabled, findAllEnabled |
| ComplianceCheckLogRepository | common/compliance/repository/ComplianceCheckLogRepository.java | countViolationsByUser, countViolationsByContentType |
| ComplianceKeywordRepository | common/compliance/repository/ComplianceKeywordRepository.java | findByCategoryAndEnabled, findAllEnabled |
| ComplianceCaseRepository | common/compliance/repository/ComplianceCaseRepository.java | findByRuleIdAndEnabled |

### 5. Service 层 ✅

**接口**: `common/compliance/service/ComplianceService.java`

**实现**: `common/compliance/service/impl/ComplianceServiceImpl.java`

**三层检测机制**：

1. **关键词匹配**（快速筛查）
   - 遍历所有敏感词
   - 直接字符串匹配
   - 根据严重程度扣分（critical: 50, high: 30, medium: 15, low: 5）

2. **正则表达式匹配**（模式识别）
   - 遍历所有规则的 patterns 字段
   - 正则表达式匹配
   - 捕获违规模式

3. **语义检测**（AI 理解）
   - TODO: 后续集成向量检索 + LLM 判断
   - 预留接口：`checkSemantic(String content)`

**综合评分**：
- 风险评分 0-100
- score >= 50: reject（拒绝）
- score >= 20: warning（警告）
- score < 20: pass（通过）

**缓存机制**：
- @PostConstruct 初始化缓存
- 缓存所有启用的规则和敏感词
- 提供 refreshCache() 方法手动刷新

### 6. Controller 层 ✅

**文件**: `common/compliance/controller/ComplianceController.java`

**API 端点**：

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/v1/compliance/check | POST | 综合检测（关键词 + 正则 + 语义） |
| /api/v1/compliance/check-keywords | POST | 仅关键词匹配 |
| /api/v1/compliance/check-patterns | POST | 仅正则表达式匹配 |
| /api/v1/compliance/check-semantic | POST | 仅语义检测 |

### 7. 业务集成 ✅

**集成点**: `douyin-operations-live/.../LiveScriptGenerationServiceImpl.java`

**集成逻辑**：
```java
// 1. 注入 ComplianceService（可选依赖）
@Autowired(required = false)
private ComplianceService complianceService;

// 2. 在话术生成后立即检测
ComplianceCheckResult complianceResult = complianceService.check(
    session.getUserId(), 
    "script", 
    null, 
    content
);

// 3. 拒绝违规内容
if ("reject".equals(complianceResult.getResult())) {
    throw new BusinessException(ErrorCode.COMPLIANCE_VIOLATION,
        "话术包含违规内容，风险评分: " + complianceResult.getRiskScore());
}

// 4. 保存检测结果到 LiveScript
script.setViolationChecked(1);
script.setViolationResult(complianceResult.getResult());
script.setAiSuggestion(complianceResult.getSuggestions());
```

### 8. 错误码 ✅

**文件**: `common/constant/ErrorCode.java`

新增错误码：
```java
/** 内容违规（通用合规检测失败） */
public static final int COMPLIANCE_VIOLATION = 3148;
```

### 9. VO 层 ✅

**文件**: `common/compliance/vo/ComplianceCheckResult.java`

**返回结构**：
```java
{
  "result": "pass|warning|reject",
  "riskScore": 0-100,
  "matchedRules": [
    {
      "ruleCode": "LIVE_CONTENT_FALSE_AD",
      "ruleName": "虚假宣传",
      "severity": "high",
      "matchReason": "包含敏感词: 包治百病",
      "punishment": "封禁 3-7 天 + 罚款"
    }
  ],
  "suggestions": "内容包含严重违规，建议重新编写",
  "checkDurationMs": 123
}
```

---

## 编译验证 ✅

```bash
mvn compile
# BUILD SUCCESS
# Total time: 44.907 s
```

所有模块编译通过，无错误。

---

## 下一步工作（可选）

### 阶段 2：语义检测集成（3 人日）

1. **向量化规则描述**
   - 使用 OpenAI Embedding API
   - 批量向量化所有规则的 description 字段
   - 存储到 embedding 字段

2. **向量检索**
   - 集成 pgvector 扩展
   - 创建向量索引：`CREATE INDEX idx_compliance_rule_embedding ON compliance_rule USING ivfflat(embedding vector_cosine_ops)`
   - 实现相似度检索：`SELECT * FROM compliance_rule ORDER BY embedding <=> $1 LIMIT 10`

3. **LLM 判断**
   - 构建 Prompt：`内容：{content}\n\n规则：{rules}\n\n判断是否违规，给出理由和修改建议。`
   - 调用 LLM API
   - 解析结果

### 阶段 3：前端集成（2 人日）

1. **违规提示组件**
   - 创建 `ComplianceAlert.tsx`
   - 显示违规规则列表
   - 显示修改建议

2. **集成到话术生成页面**
   - 在 `ScriptGeneratePage.tsx` 中调用 `/api/v1/compliance/check`
   - 显示违规提示
   - 阻止保存违规内容

3. **集成到短视频策划页面**
   - 在 `VideoScriptPage.tsx` 中调用检测 API
   - 检测标题、内容、素材

### 阶段 4：实时监控（2 人日）

1. **定时任务**
   - 每 30 秒检测正在直播的场次
   - 检测当前话术是否违规

2. **企业微信预警**
   - 发现违规立即推送企业微信
   - 包含场次信息、话术内容、违规原因

---

## 验收标准

### 功能验收 ✅

- [x] 知识库包含 20+ 条违规规则
- [x] 支持关键词匹配
- [x] 支持正则表达式匹配
- [x] 支持综合评分
- [x] 集成到话术生成
- [ ] 支持语义检测（TODO）
- [ ] 集成到短视频策划（TODO）
- [ ] 集成到实时监控（TODO）

### 性能验收

- [ ] 单次检测响应时间 < 500ms（待测试）
- [ ] 支持并发检测（100 QPS）（待测试）

### 准确率验收

- [ ] 准确率 > 90%（待人工标注 100 条样本）
- [ ] 误报率 < 10%（待测试）
- [ ] 漏报率 < 5%（待测试）

---

## 总结

✅ **已完成核心功能**：
- 数据库表结构设计
- 初始违规规则和敏感词数据
- 完整的后端实现（Entity + Repository + Service + Controller）
- 集成到直播话术生成流程
- 三层检测机制（关键词 + 正则 + 语义预留）

⚠️ **待完成功能**：
- 语义检测（向量检索 + LLM 判断）
- 前端违规提示组件
- 短视频策划集成
- 实时监控预警

🎯 **核心价值**：
- **法律风险防控**：自动拦截违规内容，避免账号封禁、罚款、法律诉讼
- **业务风险防控**：防止直播间被封、商品下架、GMV 损失
- **品牌风险防控**：保护品牌形象和用户信任
- **用户风险防控**：平台有连带责任，保护用户免受违规内容影响

---

**报告生成时间**: 2026-05-10  
**负责人**: 后端 + AI 团队  
**下次审查**: 集成语义检测后（预计 3 人日）
