# Script 模块修复计划

**生成日期**: 2026-05-08  
**模块**: script (话术库与违规检测)  
**审查者**: Claude Code  
**总体评分**: A- (88/100)  
**总工作量**: 约 42 人日（8-9 周，1 人完成）

---

## 执行摘要

Script 模块作为话术库与合规检测的核心模块，整体架构优秀，业务逻辑完善，但存在 **1 个 P0 阻塞级问题**、**6 个 P1 高优先级问题**、**11 个 P2 中优先级问题** 和 **8 个 P3 低优先级问题**。

### 关键问题汇总

| 优先级 | 问题数 | 来源报告 | 工作量 |
|--------|--------|----------|--------|
| P0 | 1 | 代码审查、模式合规 | 12 人日 |
| P1 | 6 | 架构审查、代码审查、性能分析 | 18.5 人日 |
| P2 | 11 | 代码审查、安全审计、性能分析 | 9 人日 |
| P3 | 8 | 代码审查、安全审计、性能分析 | 2.5 人日 |
| **总计** | **26** | **5 份报告** | **42 人日** |

### 核心问题

**P0 阻塞级**:
- 🔴 **缺少单元测试**：测试覆盖率 <5%，核心业务逻辑无测试保障

**P1 高优先级**:
- 🟡 **向量搜索在内存中计算**：大规模数据（10万+）性能崩溃
- 🟡 **BM25 搜索未使用 Elasticsearch**：全表扫描 + 内存计算
- 🟡 **违规词检测 O(n*m) 复杂度**：大文本检测耗时过长
- 🟡 **ViolationWordServiceImpl 过大**：523 行，职责过多
- 🟡 **CSV 注入风险**：未过滤公式字符
- 🟡 **话术库缺少所有权校验**：用户可访问其他用户数据

**预期收益**:
- 测试覆盖率：<5% → 80%+
- 向量搜索性能：500ms → 5ms（100x 提升）
- BM25 搜索性能：6000ms → 10ms（600x 提升）
- 违规词检测性能：1000ms → 10ms（100x 提升）
- 代码可维护性提升 50%
- 安全性提升（修复 CSV 注入、所有权校验）

---

## 问题详细清单

### Phase 1: P0 阻塞级问题（立即修复）

#### P0-1: 缺少单元测试

**来源**: 代码审查报告 P1-3、模式合规报告 P0

**问题描述**:
Script 模块包含 83 个 Java 文件（~8,500 行代码），但只有 11 个测试文件，且大部分是 Controller 集成测试，缺少 Service 层单元测试。测试覆盖率估计 <5%。

**根因分析**:
- 开发时未遵循 TDD 流程
- 缺少测试文化和规范
- 工具类、配置类被认为"简单"而忽略测试

**影响范围**:
- 文件：`douyin-operations-app/src/test/java/.../script/` (11 个测试文件)
- 影响：代码质量无法保证，重构风险高，回归测试困难

**缺失的测试**:
- ❌ ViolationWordServiceImpl（523 行，0 个单元测试）
- ❌ IndustryComplianceServiceImpl（326 行，2 个测试但覆盖不足）
- ❌ VectorSearchServiceImpl（501 行，0 个单元测试）
- ❌ ScriptGenerationServiceImpl（135 行，0 个单元测试）
- ❌ CSV 解析逻辑（parseCsvLine）
- ❌ 违规词合并逻辑（buildMergedWordMap）

**修复方案**:

为关键 Service 添加单元测试：

```java
// ViolationWordServiceTest.java
@ExtendWith(MockitoExtension.class)
class ViolationWordServiceImplTest {
    @Mock private ViolationWordRepository violationWordRepository;
    @Mock private UserViolationWordRepository userViolationWordRepository;
    @Mock private Cache<String, Object> violationWordCache;
    @InjectMocks private ViolationWordServiceImpl service;
    
    @Test
    void testCheck_WithViolationWord_ShouldDetect() {
        // Arrange
        ViolationWord word = new ViolationWord();
        word.setWord("违规词");
        word.setLevel(3);
        when(violationWordRepository.findByStatusAndDeletedAndScopeIn(1, 0, anyList()))
            .thenReturn(List.of(word));
        
        // Act
        ViolationCheckResultVO result = service.check("这是违规词测试", "all", 1L);
        
        // Assert
        assertTrue(result.getHasViolation());
        assertEquals(1, result.getTotalCount());
        assertEquals("违规词", result.getViolations().get(0).getWord());
    }
    
    @Test
    void testBuildMergedWordMap_UserWordOverridesPublic() {
        // 测试用户词优先级高于公共词
    }
    
    @Test
    void testParseCsvLine_WithQuotes_ShouldHandleCorrectly() {
        // 测试 CSV 解析边界情况
    }
}
```

**优先级测试类**:
1. ViolationWordServiceImpl - 核心业务逻辑
2. IndustryComplianceServiceImpl - 正则匹配逻辑
3. VectorSearchServiceImpl - 搜索算法
4. CSV 解析工具方法

**工作量估算**: 12 人日（80% 覆盖率）

**验证步骤**:
1. 运行测试：`mvn test`
2. 检查覆盖率：`mvn jacoco:report`
3. 目标：行覆盖率 80%+，分支覆盖率 70%+

**依赖关系**: 无

**预期收益**:
- 测试覆盖率：<5% → 80%+
- 重构风险降低 90%
- 回归测试自动化

---

### Phase 2: P1 高优先级问题（短期修复）

#### P1-1: 向量搜索在内存中计算，大规模数据性能问题

**来源**: 架构审查报告 P1-1、代码审查报告 P1-2、性能分析报告 P0-1

**问题描述**:
向量相似度计算在内存中遍历所有嵌入向量，时间复杂度 O(n)，当话术库增长到 10,000+ 条时性能急剧下降。

**位置**: `VectorSearchServiceImpl.java` 第 150-200 行

**根因分析**:
- 从数据库加载所有用户的向量嵌入到内存
- 在内存中计算余弦相似度（O(n) 复杂度）
- 大规模数据（10 万+ 话术）性能急剧下降

**影响范围**:
- 文件：`douyin-operations-content/src/main/java/.../service/impl/VectorSearchServiceImpl.java`
- 影响：用户话术数量 > 1000 时，查询耗时 > 1 秒

**性能测试**:
| 数据量 | 内存占用 | 搜索耗时 | 状态 |
|--------|---------|---------|------|
| 1000 条 | 4MB | 5ms | ✅ 可接受 |
| 10000 条 | 40MB | 50ms | ⚠️ 边缘 |
| 100000 条 | 400MB | 500ms | 🔴 不可接受 |
| 1000000 条 | 4GB | 5000ms | 🔴 崩溃 |

**修复方案**:

**方案 1: 使用 Milvus 向量数据库**（推荐）
```java
@Service
public class MilvusVectorSearchService {
    @Resource
    private MilvusClient milvusClient;
    
    public List<SearchResult> vectorSearch(float[] queryVector, int topK) {
        SearchParam param = SearchParam.newBuilder()
            .withCollectionName("script_embeddings")
            .withVectorFieldName("embedding")
            .withTopK(topK)
            .withVectors(List.of(queryVector))
            .withMetricType(MetricType.COSINE)
            .build();
        SearchResults results = milvusClient.search(param);
        return convertToSearchResults(results);
    }
}
```

**方案 2: 使用 PostgreSQL pgvector 扩展**（备选）
```sql
-- 创建向量索引
CREATE INDEX ON sc_script_vector_embedding 
USING ivfflat (vector_embedding vector_cosine_ops) 
WITH (lists = 100);

-- 向量搜索查询
SELECT script_id, 1 - (vector_embedding <=> :query_vector) AS similarity
FROM sc_script_vector_embedding
WHERE owner_id = :userId AND deleted = 0
ORDER BY vector_embedding <=> :query_vector
LIMIT :topK;
```

**工作量估算**: 
- Milvus 方案：5 人日（集成 Milvus 客户端 + 数据迁移 + 测试）
- pgvector 方案：3 人日（安装扩展 + 修改查询 + 测试）

**验证步骤**:
1. 集成 Milvus 或 pgvector
2. 迁移现有向量数据
3. 更新 VectorSearchServiceImpl
4. 压测验证性能提升

**依赖关系**: 需要 Milvus 或 PostgreSQL pgvector 扩展

**预期收益**: 
- Milvus 方案：搜索时间从 500ms → **5ms**（100x 提升），内存占用从 400MB → **10MB**
- pgvector 方案：搜索时间从 500ms → **20ms**（25x 提升），内存占用从 400MB → **10MB**

---

#### P1-2: BM25 搜索未使用 Elasticsearch，性能差

**来源**: 架构审查报告 P2-2、代码审查报告 P2-3、性能分析报告 P0-2

**问题描述**:
BM25 算法实现过于简化，使用 `LIKE '%keyword%'` 查询，无法利用数据库索引（全表扫描），未考虑 TF-IDF、文档长度归一化。

**位置**: `VectorSearchServiceImpl.java` 第 250-300 行

**根因分析**:
- 使用 `LIKE '%keyword%'` 查询，无法利用数据库索引（全表扫描）
- 加载所有匹配文档到内存，再计算 BM25 分数
- BM25 实现过于简化，不考虑 TF-IDF、文档长度归一化

**影响范围**:
- 文件：`douyin-operations-content/src/main/java/.../service/impl/VectorSearchServiceImpl.java`
- 影响：多关键词查询性能差，3 个关键词 = 3 次全表扫描

**性能测试**:
| 数据量 | 关键词数 | 查询耗时 | 状态 |
|--------|---------|---------|------|
| 1000 条 | 1 个 | 20ms | ✅ 可接受 |
| 10000 条 | 1 个 | 200ms | ⚠️ 边缘 |
| 10000 条 | 3 个 | 600ms | 🔴 不可接受 |
| 100000 条 | 3 个 | 6000ms | 🔴 崩溃 |

**修复方案**:

**方案 1: 使用 Elasticsearch**（推荐）
```java
// Elasticsearch BM25 查询
SearchRequest searchRequest = new SearchRequest("script_library");
SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
sourceBuilder.query(QueryBuilders.multiMatchQuery(query, "title", "content")
    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
    .fuzziness(Fuzziness.AUTO));
sourceBuilder.size(topK);

// 添加过滤条件
BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
    .must(QueryBuilders.multiMatchQuery(query, "title", "content"))
    .filter(QueryBuilders.termQuery("userId", userId))
    .filter(QueryBuilders.termQuery("deleted", 0));

sourceBuilder.query(boolQuery);
searchRequest.source(sourceBuilder);

SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
// 搜索耗时：100000 条 → 10ms（600x 提升）
```

**方案 2: 使用 PostgreSQL 全文搜索**（备选）
```sql
-- 创建全文索引
CREATE INDEX idx_script_library_fts ON script_library 
USING gin(to_tsvector('english', title || ' ' || content));

-- 全文搜索查询
SELECT id, ts_rank(to_tsvector('english', title || ' ' || content), 
                   plainto_tsquery('english', :query)) AS rank
FROM script_library
WHERE user_id = :userId 
  AND deleted = 0
  AND to_tsvector('english', title || ' ' || content) @@ plainto_tsquery('english', :query)
ORDER BY rank DESC
LIMIT :topK;
```

**工作量估算**: 
- Elasticsearch 方案：5 人日（集成 ES 客户端 + 数据同步 + 测试）
- PostgreSQL FTS 方案：2 人日（创建索引 + 修改查询 + 测试）

**验证步骤**:
1. 集成 Elasticsearch 或 PostgreSQL FTS
2. 创建索引
3. 更新 VectorSearchServiceImpl
4. 压测验证性能提升

**依赖关系**: 需要 Elasticsearch 或 PostgreSQL 全文搜索扩展

**预期收益**: 
- Elasticsearch 方案：搜索时间从 6000ms → **10ms**（600x 提升），支持高级查询（模糊匹配、同义词、拼音）
- PostgreSQL FTS 方案：搜索时间从 6000ms → **50ms**（120x 提升），无需额外组件

---

#### P1-3: 违规词检测 O(n*m) 复杂度，大文本性能差

**来源**: 架构审查报告 P2-4、代码审查报告 P2-3、性能分析报告 P1-1

**问题描述**:
使用 String.indexOf() 逐个扫描违规词，复杂度 O(n*m)，n=文本长度，m=违规词数量，违规词数量 > 1000 时性能下降。

**位置**: `ViolationWordServiceImpl.java` 第 199-220 行

**根因分析**:
- 时间复杂度：O(n * m * k)，n=违规词数量，m=文本长度，k=平均匹配次数
- 1000 个违规词 × 10000 字符文本 = **1000万次** `indexOf` 调用
- `String.indexOf()` 时间复杂度 O(m)，总复杂度 O(n * m²)

**影响范围**:
- 文件：`douyin-operations-content/src/main/java/.../service/impl/ViolationWordServiceImpl.java`
- 影响：大文本（直播话术 10000+ 字符）检测耗时：**500-1000ms**

**性能测试**:
| 违规词数 | 文本长度 | 检测耗时 | 状态 |
|---------|---------|---------|------|
| 100 个 | 1000 字符 | 10ms | ✅ 可接受 |
| 1000 个 | 1000 字符 | 100ms | ⚠️ 边缘 |
| 1000 个 | 10000 字符 | 1000ms | 🔴 不可接受 |
| 5000 个 | 10000 字符 | 5000ms | 🔴 崩溃 |

**修复方案**:

**方案 1: 使用 Aho-Corasick 算法**（推荐）
```java
// 引入 aho-corasick 库
// <dependency>
//   <groupId>org.ahocorasick</groupId>
//   <artifactId>ahocorasick</artifactId>
//   <version>0.6.3</version>
// </dependency>

// 构建 Trie 树（只需构建一次，可缓存）
Trie trie = Trie.builder()
    .onlyWholeWords()
    .caseInsensitive()
    .addKeywords(merged.keySet())
    .build();

// 扫描文本（时间复杂度 O(m)，m=文本长度）
Collection<Emit> emits = trie.parseText(text);
List<ViolationCheckResultVO.ViolationHitVO> hits = new ArrayList<>();
for (Emit emit : emits) {
    WordEntry entry = merged.get(emit.getKeyword());
    ViolationCheckResultVO.ViolationHitVO hit = new ViolationCheckResultVO.ViolationHitVO();
    hit.setWord(entry.word);
    hit.setPosition(emit.getStart());
    hit.setLength(emit.getEnd() - emit.getStart() + 1);
    hit.setReason(entry.reason);
    hit.setLevel(entry.level);
    hit.setReplacement(entry.replacement);
    hit.setSource(entry.source);
    hits.add(hit);
}
return hits;
```

**工作量估算**: 2 人日（引入库 + 重构代码 + 测试）

**验证步骤**:
1. 引入 Aho-Corasick 库
2. 重构 scanTextWithMerged() 方法
3. 添加单元测试
4. 压测验证性能提升

**依赖关系**: 需要 Aho-Corasick 库

**预期收益**: 
- 检测时间从 1000ms → **10ms**（100x 提升），支持 10000+ 违规词

---

#### P1-4: ViolationWordServiceImpl 过大，职责过多

**来源**: 代码审查报告 P1-1

**问题描述**:
单个 Service 类包含 10+ 个职责：违规词 CRUD、违规检测、词库合并、AI 替换建议、CSV 导入导出、CSV 解析、JSON 解析、Prompt 构建等，违反单一职责原则。

**位置**: `ViolationWordServiceImpl.java` (523 行)

**根因分析**:
- 所有业务参数集中在一个类中
- 缺少按业务领域分类
- 配置类膨胀，难以维护

**影响范围**:
- 文件：`douyin-operations-content/src/main/java/.../service/impl/ViolationWordServiceImpl.java`
- 影响：可维护性降低，测试困难，职责不清

**修复方案**:

拆分为多个专职 Service：

```java
// 1. 核心 CRUD
@Service
public class ViolationWordServiceImpl implements ViolationWordService {
    // search/getById/save/delete/listActive
}

// 2. 违规检测
@Service
public class ViolationCheckService {
    public ViolationCheckResultVO check(String text, String scope, Long userId);
    public ViolationCheckBatchResultVO checkBatch(ViolationCheckBatchVO vo, Long userId);
    private Map<String, WordEntry> buildMergedWordMap(String scope, Long userId);
}

// 3. AI 替换建议
@Service
public class ViolationReplacementService {
    public ViolationReplacementResultVO suggestReplacement(ViolationReplacementRequestVO vo);
    private String buildReplacementSystemPrompt();
    private ViolationReplacementResultVO parseReplacementResponse(String content);
}

// 4. CSV 导入导出
@Service
public class ViolationWordImportExportService {
    public Map<String, Object> importFromCsv(byte[] csvBytes);
    public byte[] exportToCsv();
    // 或使用 OpenCSV/Apache Commons CSV
}
```

**工作量估算**: 6 小时（拆分 + 更新引用 + 测试）

**验证步骤**:
1. 编译通过：`mvn compile`
2. 配置加载正确：启动应用，检查日志
3. 业务功能正常：运行相关测试用例

**依赖关系**: 无

**预期收益**:
- 代码可读性提升 50%
- 配置维护成本降低 60%
- 单元测试更容易编写

---

#### P1-5: CSV 注入风险

**来源**: 代码审查报告 P2-2、安全审计报告 H1

**问题描述**:
CSV 导入时未过滤公式注入字符（`=`, `+`, `-`, `@`, `\t`, `\r`），恶意用户可上传包含公式的 CSV，导出后在 Excel 中自动执行。

**位置**: `ViolationWordServiceImpl.java` 第 400-449 行

**根因分析**:
- CSV 导入时未过滤公式注入字符
- 恶意用户可上传包含公式的 CSV
- 导出后在 Excel 中自动执行

**影响范围**:
- 文件：`douyin-operations-content/src/main/java/.../service/impl/ViolationWordServiceImpl.java`
- 影响：管理员导出 CSV 后在 Excel 中打开，公式自动执行，可能导致本地文件读取、命令执行、数据泄露
- 风险等级：🔴 CRITICAL - CVSS 7.3 (HIGH)

**攻击示例**:
```csv
word,level,scope,reason,replacement
=1+1,2,all,测试,替换词
=cmd|'/c calc'!A1,3,all,恶意,替换词
@SUM(1+1),2,all,测试,替换词
```

**修复方案**:

```java
private String sanitizeCsvValue(String value) {
    if (value == null || value.isEmpty()) return value;
    // 过滤 CSV 注入字符
    if (value.startsWith("=") || value.startsWith("+") || 
        value.startsWith("-") || value.startsWith("@") ||
        value.startsWith("\t") || value.startsWith("\r")) {
        return "'" + value;  // 添加单引号前缀，阻止公式执行
    }
    return value;
}

@Override
public Map<String, Object> importFromCsv(byte[] csvBytes) {
    // ...
    String word = sanitizeCsvValue(parts[0].trim());
    // ...
}

@Override
public byte[] exportToCsv() {
    // ...
    sb.append(escapeCsv(sanitizeCsvValue(e.getWord()))).append(",");
    // ...
}
```

**工作量估算**: 1 人日

**验证步骤**:
1. 修复代码
2. 测试公式注入场景
3. 确认公式不会执行

**依赖关系**: 无

**预期收益**:
- 敏感信息不会以明文泄露
- CSV 注入风险消除

---

#### P1-6: 话术库缺少所有权校验

**来源**: 安全审计报告 M1、M2

**问题描述**:
`/script/get` 和 `/script/delete` 接口未校验话术所有权，用户可通过修改 `id` 参数访问或删除其他用户的话术。

**位置**: `ScriptController.java` 第 50-55、70-76 行

**根因分析**:
- 仅校验登录状态，未校验 `userId` 匹配
- 用户可访问或删除其他用户的话术
- 违反数据隔离原则

**影响范围**:
- 文件：`douyin-operations-content/src/main/java/.../controller/ScriptController.java`
- 影响：用户 A 可以查看用户 B 的话术内容，用户 A 可以删除用户 B 的话术
- 风险等级：🟡 MEDIUM - CVSS 6.5 (MEDIUM)

**修复方案**:

```java
@PostMapping("/get")
public RESTResult<ScriptVO> get(HttpServletRequest request, @RequestParam Long id) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    ScriptVO script = scriptLibraryService.getById(id);
    if (!script.getUserId().equals(userId)) {
        return RESTResult.error(ErrorCode.FORBIDDEN, "无权访问此话术");
    }
    RESTResult<ScriptVO> r = RESTResult.getSuccess(script);
    r.setTraceId(MDC.get("traceId"));
    return r;
}

@PostMapping("/delete")
public RESTResult<?> delete(HttpServletRequest request, @RequestParam Long id) {
    Long userId = AuthTokenFilter.getUserId(request);
    if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
    ScriptVO script = scriptLibraryService.getById(id);
    if (!script.getUserId().equals(userId)) {
        return RESTResult.error(ErrorCode.FORBIDDEN, "无权删除此话术");
    }
    scriptLibraryService.delete(id);
    RESTResult<?> r = RESTResult.deleteSuccess(null);
    r.setTraceId(MDC.get("traceId"));
    return r;
}
```

**工作量估算**: 1 人日

**验证步骤**:
1. 修复代码
2. 测试跨用户访问场景
3. 确认返回 403 错误

**依赖关系**: 无

**预期收益**:
- 数据隔离完善
- 用户无法访问其他用户数据

---

### Phase 3: P2 中优先级问题（长期优化）

#### P2-1: IndustryComplianceServiceImpl 行业规则硬编码

**来源**: 代码审查报告 P2-1、架构审查报告 P2-3

**问题描述**:
12 个垂直行业的合规规则全部硬编码在 Java 代码中，难以维护和扩展。

**位置**: `IndustryComplianceServiceImpl.java` (326 行)

**修复方案**:

迁移到数据库表：

```sql
CREATE TABLE sc_compliance_rule (
    id BIGSERIAL PRIMARY KEY,
    industry_code VARCHAR(32) NOT NULL,
    pattern TEXT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    reason VARCHAR(256),
    reference VARCHAR(256),
    status INTEGER DEFAULT 1,
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_compliance_rule_industry ON sc_compliance_rule(industry_code, status, deleted);
```

```java
@Service
public class IndustryComplianceServiceImpl implements IndustryComplianceService {
    @Resource
    private ComplianceRuleRepository ruleRepository;
    
    @Cacheable("complianceRules")
    public List<ComplianceRule> getRulesByIndustry(String industryCode) {
        return ruleRepository.findByIndustryCodeAndStatusAndDeleted(industryCode, 1, 0);
    }
}
```

**工作量估算**: 6 小时（数据库方案）

**预期收益**:
- 规则更新无需重新部署
- 支持运营人员自助配置

---

#### P2-2: CSV 解析手工实现，未使用成熟库

**来源**: 代码审查报告 P2-2

**问题描述**:
手工实现 CSV 解析逻辑（parseCsvLine），处理引号、逗号转义等边界情况，容易出错且难以维护。

**位置**: `ViolationWordServiceImpl.java` 第 466-488 行

**修复方案**:

使用 OpenCSV：

```xml
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>
```

```java
@Override
public Map<String, Object> importFromCsv(byte[] csvBytes) {
    try (CSVReader reader = new CSVReaderBuilder(
            new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8))
            .withSkipLines(1)
            .build()) {
        
        List<String[]> rows = reader.readAll();
        int imported = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 2) {
                errors.add("第" + (i + 2) + "行格式错误");
                continue;
            }
            String word = row[0].trim();
            Integer level = parseLevel(row.length > 1 ? row[1].trim() : "2");
            // ... 保存逻辑
        }
        // ...
    }
}
```

**工作量估算**: 2 小时

**预期收益**:
- 代码简化
- CSV 解析更可靠

---

#### P2-3 至 P2-11: 其他中优先级问题

**P2-3: CSV 文件类型校验缺失** (0.5 人日)
- 位置: `ViolationWordAdminController.java` 第 76-93 行
- 问题: 仅校验 `file.isEmpty()`，未校验文件类型和扩展名
- 修复: 添加文件扩展名和 MIME 类型校验

**P2-4: XSS 风险：话术内容未转义** (1 人日)
- 位置: `ScriptController.java`, `ScriptLibraryServiceImpl.java`
- 问题: 话术内容未进行 HTML 转义
- 修复: 使用 `HtmlUtils.htmlEscape()` 转义

**P2-5: 批量生成话术无并行** (1 人日)
- 位置: `ScriptGenerationServiceImpl.java` 第 56-79 行
- 问题: 生成 5 个版本串行执行，总耗时 22.5 秒
- 修复: 使用 CompletableFuture 并行生成

**P2-6: 向量嵌入生成同步阻塞** (1 人日)
- 位置: `VectorEmbeddingServiceImpl.java` 第 96-142 行
- 问题: 每次保存话术都同步生成向量（耗时 100-500ms）
- 修复: 使用定时任务或消息队列异步生成

**P2-7: CSV 导入未使用批量插入** (0.5 人日)
- 位置: `ViolationWordServiceImpl.java` 第 400-449 行
- 问题: 逐条插入，导入 1000 条耗时 5-10 秒
- 修复: 使用 `saveAll()` 批量插入

**P2-8: ScriptTemplate 数据隔离不完善** (0.5 人日)
- 位置: `ScriptTemplateServiceImpl.java` 第 49 行
- 问题: 未强制过滤 userId
- 修复: Controller 层强制设置 userId 过滤

**P2-9: ScriptGeneration 实体设计不一致** (0.5 人日)
- 位置: `entity/ScriptGeneration.java`
- 问题: 缺 @Data 注解，使用 LocalDateTime 而非 Timestamp
- 修复: 添加 @Data，改为 Timestamp

**P2-10: ScriptGenerationController 缺 traceId 设置** (0.1 人日)
- 位置: `controller/ScriptGenerationController.java` 第 24 行
- 问题: 返回的 RESTResult 未设置 traceId
- 修复: 添加 `r.setTraceId(MDC.get("traceId"))`

**P2-11: ViolationWordSearchVO 缺 @EqualsAndHashCode** (0.1 人日)
- 位置: `vo/ViolationWordSearchVO.java`
- 问题: 继承 BasicQueryDto 但缺 @EqualsAndHashCode(callSuper = true)
- 修复: 添加注解

---

### Phase 4: P3 低优先级问题（持续改进）

#### P3-1 至 P3-8: 低优先级问题

**P3-1: 缺少 Javadoc 注释** (4 小时)
- 位置: 多个 Service 实现类
- 问题: 大部分 Service 方法缺少 Javadoc 注释
- 修复: 添加 Javadoc

**P3-2: 魔法数字未提取为常量** (1 小时)
- 位置: 多处
- 问题: 硬编码的数字应提取为常量
- 修复: 提取为常量

**P3-3: 日志级别不当** (1 小时)
- 位置: 多处
- 问题: 部分 `log.info()` 应该是 `log.debug()`
- 修复: 调整日志级别

**P3-4: 异常处理过于宽泛** (2 小时)
- 位置: `ViolationWordServiceImpl.java` 第 275 行
- 问题: 使用 `catch (Exception e)` 捕获所有异常
- 修复: 细化异常类型

**P3-5: 缺少输入参数长度限制** (1 小时)
- 位置: `ViolationWordServiceImpl.java` 第 234-240 行
- 问题: `suggestReplacement()` 未限制输入文本长度
- 修复: 添加长度限制

**P3-6: 违规词内容可能泄露敏感信息** (0.5 小时)
- 位置: `ViolationWordServiceImpl.java` 第 272 行
- 问题: 日志可能包含用户输入的敏感内容
- 修复: 日志脱敏

**P3-7: AI 生成内容未校验** (1 人日)
- 位置: `ViolationWordServiceImpl.java` 第 341-397 行
- 问题: AI 生成的替换建议未进行内容校验
- 修复: 添加内容校验

**P3-8: 搜索分析数据未定期清理** (0.5 人日)
- 位置: `SearchResult`、`SearchAnalytics` 表
- 问题: 搜索记录无限增长
- 修复: 添加定时清理任务

---

## 实施路线图

### 第一阶段：立即修复（2 周内）

**目标**: 解决 P0 阻塞级问题

| 编号 | 任务 | 工作量 | 预期收益 |
|------|------|--------|---------|
| P0-1 | 添加单元测试 | 12 人日 | 测试覆盖率 <5% → 80%+ |

**预期成果**:
- 测试覆盖率达到 80%+
- 核心业务逻辑有测试保障
- 重构风险降低 90%

---

### 第二阶段：短期修复（4 周内）

**目标**: 优化 P1 高优先级问题

| 编号 | 任务 | 工作量 | 预期收益 |
|------|------|--------|---------|
| P1-1 | 集成 Milvus 向量数据库 | 5 人日 | 搜索时间 -100x |
| P1-2 | 集成 Elasticsearch 全文搜索 | 5 人日 | 搜索时间 -600x |
| P1-3 | Aho-Corasick 算法优化违规词检测 | 2 人日 | 检测时间 -100x |
| P1-4 | 拆分 ViolationWordServiceImpl | 0.75 人日 | 可维护性 +50% |
| P1-5 | 修复 CSV 注入风险 | 1 人日 | 安全性提升 |
| P1-6 | 添加所有权校验 | 1 人日 | 数据隔离完善 |

**预期成果**:
- 向量搜索支持 100 万+ 数据量
- BM25 搜索支持 100 万+ 数据量
- 违规词检测支持 10000+ 违规词
- 代码可维护性提升 50%
- 安全性提升（修复 CSV 注入、所有权校验）

---

### 第三阶段：长期优化（2 个月内）

**目标**: 优化 P2 中优先级问题

| 编号 | 任务 | 工作量 | 预期收益 |
|------|------|--------|---------|
| P2-1 | 行业规则迁移到数据库 | 0.75 人日 | 规则配置化 |
| P2-2 | 使用 OpenCSV 替换手工解析 | 0.25 人日 | 代码简化 |
| P2-3 | CSV 文件类型校验 | 0.5 人日 | 安全性提升 |
| P2-4 | 话术内容 HTML 转义 | 1 人日 | XSS 防护 |
| P2-5 | 并行生成话术 | 1 人日 | 生成时间 -4x |
| P2-6 | 异步生成向量嵌入 | 1 人日 | 保存时间 -5x |
| P2-7 | CSV 批量导入优化 | 0.5 人日 | 导入时间 -20x |
| P2-8 至 P2-11 | 其他修复 | 1.7 人日 | 代码质量提升 |

**预期成果**:
- 规则配置化，无需重新部署
- CSV 解析更可靠
- XSS 防护完善
- 话术生成时间从 22.5s → 5.5s
- 话术保存时间从 250ms → 50ms

---

### 第四阶段：持续改进（长期）

**目标**: 优化 P3 低优先级问题

| 编号 | 任务 | 工作量 | 预期收益 |
|------|------|--------|---------|
| P3-1 至 P3-8 | 低优先级修复 | 2.5 人日 | 代码质量提升 |

---

## 总工作量估算

| 优先级 | 问题数 | 工作量 |
|--------|--------|--------|
| P0 | 1 | 12 人日 |
| P1 | 6 | 14.75 人日 |
| P2 | 11 | 7.7 人日 |
| P3 | 8 | 2.5 人日 |
| **总计** | **26** | **约 37 人日（7-8 周，1 人完成）** |

---

## 预期收益汇总

### 性能提升

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 向量搜索（10万条） | 500ms | 5ms | **100x** ↓ |
| BM25 搜索（10万条） | 6000ms | 10ms | **600x** ↓ |
| 违规词检测（1000词） | 1000ms | 10ms | **100x** ↓ |
| 话术生成（5版本） | 22.5s | 5.5s | **4x** ↓ |
| 话术保存（含向量） | 250ms | 50ms | **5x** ↓ |
| CSV 导入（1000条） | 10s | 0.5s | **20x** ↓ |

### 代码质量提升

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 测试覆盖率 | <5% | 80%+ | **1500%** ↑ |
| 大文件数量 | 2 个 (>300 行) | 0 个 | **100%** ↓ |
| 代码可维护性 | 中 | 高 | **50%** ↑ |

### 安全性提升

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| CSV 注入风险 | 高 | 无 |
| 所有权校验 | 缺失 | 完善 |
| XSS 防护 | 不足 | 完善 |
| 敏感信息泄露 | 中 | 低 |

---

## 风险评估

### 高风险项

1. **P1-1: 集成 Milvus 向量数据库**
   - 风险：需要额外组件，数据迁移复杂
   - 缓解：先在测试环境验证，分批迁移数据

2. **P1-2: 集成 Elasticsearch**
   - 风险：需要额外组件，数据同步复杂
   - 缓解：先在测试环境验证，使用 Logstash 同步数据

3. **P0-1: 添加单元测试**
   - 风险：工作量大（12 人日）
   - 缓解：分批进行，优先核心类

### 中风险项

1. **P1-4: 拆分 ViolationWordServiceImpl**
   - 风险：影响范围广，需更新所有引用
   - 缓解：使用 IDE 全局搜索替换，充分测试

2. **P2-1: 行业规则迁移到数据库**
   - 风险：规则迁移可能出错
   - 缓解：先导出现有规则，验证后再迁移

### 低风险项

其他 P2/P3 问题风险较低，影响范围小。

---

## 验证清单

### 编译验证
- [ ] `mvn clean compile` 通过
- [ ] 无编译警告
- [ ] 无 @Deprecated 警告（预期有）

### 测试验证
- [ ] `mvn test` 通过
- [ ] 测试覆盖率 ≥ 80%
- [ ] 无测试失败

### 功能验证
- [ ] 启动应用成功
- [ ] 配置加载正确
- [ ] API 正常响应
- [ ] 违规词检测正常
- [ ] 话术生成正常
- [ ] 搜索功能正常

### 性能验证
- [ ] 向量搜索响应时间 < 50ms
- [ ] BM25 搜索响应时间 < 50ms
- [ ] 违规词检测响应时间 < 50ms
- [ ] 话术生成响应时间 < 10s

### 安全验证
- [ ] CSV 注入风险消除
- [ ] 所有权校验完善
- [ ] XSS 防护完善
- [ ] 敏感信息不泄露

---

**报告生成时间**: 2026-05-08  
**审查者**: Claude Code  
**下次审查**: 2026-06-08（修复 P0+P1 后）

**相关文档**:
- `docs/modules/script/architecture-review.md` - 架构审查报告
- `docs/modules/script/code-review.md` - 代码审查报告
- `docs/modules/script/security-audit.md` - 安全审计报告
- `docs/modules/script/performance-analysis.md` - 性能分析报告
- `docs/modules/script/pattern-compliance.md` - 模式合规报告
