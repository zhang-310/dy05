# 知识库 DOC/PDF 批量导入方案 — 深度评估报告

> 评估日期：2026-03-04
> 评估对象：`docs/design/kb-doc-pdf-batch-import.md` v2.0
> 评估方法：逐层校验方案与当前代码的匹配度 + 识别方案自身可升级点

---

## 一、方案现状校验（方案 vs 代码）

### 1.1 依赖层

| 依赖 | 方案描述 | 实际状态 | 差异 |
|------|---------|---------|------|
| poi-ooxml 5.2.5 | "已有，第 117-122 行" | ✅ 确认存在 | 无 |
| poi-scratchpad | "需新增" | ❌ 缺失 | 符合 |
| PDFBox 3.0.3 | "需新增" | ❌ 缺失 | 符合 |
| tika-core 2.9.1 | "可选" | ❌ 缺失 | 符合 |

**评估：方案准确。** 但有一处需要注意：

> **升级点 A1**：PDFBox 3.0.3 已发布较久，当前最新为 3.0.4+。建议锁定版本时确认与 JDK 21 的兼容性。POI 5.2.5 同理，5.3.0 已发布。如果是全新添加，建议直接使用最新稳定版。

### 1.2 解析层（DocumentParser）

| 能力 | 方案描述 | 实际现状 |
|------|---------|---------|
| MD/TXT 读取 | 保持原有 Files.readString | ✅ 第 194 行确认 |
| DOCX 解析 | XWPFDocument + 表格提取 | ❌ 待实现 |
| DOC 解析 | HWPFDocument | ❌ 待实现 |
| PDF 解析 | PDFBox PDFTextStripper | ❌ 待实现 |
| Magic Bytes 校验 | 手工检测 PDF/DOC/DOCX 头 | ❌ 待实现 |
| 加密文件检测 | EncryptedDocumentException / isEncrypted() | ❌ 待实现 |
| 扫描件检测 | 文字密度 < 20字/页 | ❌ 待实现 |
| 元数据提取 | parseWithMetadata() | ❌ 待实现 |

**评估：方案设计完整。** 发现以下可升级点：

> **升级点 A2**：方案中 DOCX 解析只处理了 `getParagraphs()` 和 `getTables()`，但遗漏了以下 DOCX 内容源：
> - **页眉/页脚**（`getHeaderFooterPolicy()`）— 话术文档可能在页眉标注分类
> - **批注/修订**（`getComments()`）— 购买的文档可能含编辑批注
> - **文本框**（浮动 shape 中的文字）— POI 需通过 `XWPFRun.getCTR()` 提取
>
> 建议：Phase 1 先忽略这些边缘场景，但在 `DocumentParser.parseDocx()` 中预留 `// TODO: 页眉页脚/文本框` 注释。

> **升级点 A3**：方案中 `parsePdf()` 使用 `Loader.loadPDF(file.toFile())`，但 PDFBox 3.x 已废弃 `File` 参数，推荐使用 `Loader.loadPDF(RandomAccessReadBufferedFile(file))`。需验证 3.0.3 的 API 兼容性。

> **升级点 A4**：方案中扫描件检测阈值硬编码为 `< 20 字符`。实际上，部分 PDF 的封面页/目录页文字较少但不是扫描件。建议改为**配置化阈值**：
> ```yaml
> app.ai.kb.parse:
>   scan-detect-min-chars: 20
>   scan-detect-density: 50  # 每页最低字符数
> ```

### 1.3 分块层

| 能力 | 方案描述 | 实际现状 |
|------|---------|---------|
| 通用分块 | 512字符 + 50重叠 | ✅ `splitIntoChunks()` 已实现 |
| 话术感知分块 | ScriptAwareChunker（按序号/标题切分） | ❌ 待实现 |
| 自动类型检测 | DocumentTypeDetector（script/mixed/general） | ❌ 待实现 |
| 混合文档处理 | MixedDocumentProcessor | ❌ 待实现 |
| Chunk 标签打标 | ChunkLabeler | ❌ 待实现 |

**评估：方案设计优秀。** 但发现以下可升级点：

> **升级点 A5**：方案中 `DocumentTypeDetector.detect()` 的关键词列表硬编码了 14 个话术关键词（"姐妹们"、"宝子们"等）。这些关键词有强烈的**时效性和行业偏向**（美妆/食品为主）。建议：
> - 将关键词列表**外置到数据库**（`sys_config` 表，key = `kb.script.keywords`），管理后台可维护
> - 或至少放到 `application.yml` 的配置项中，而非硬编码
> - 新增**服装/家居/数码**等行业的话术关键词

> **升级点 A6**：`ScriptAwareChunker` 的"太短合并"阈值（30字）和"太长回退"阈值（1000字）也应配置化。不同品类的话术长度差异大：
> - 快消品促销话术：50-200字/条
> - 美妆种草话术：200-500字/条
> - 数码产品介绍：500-1500字/条

> **升级点 A7**：`ChunkLabeler` 的标签体系与现有 `classifyByContent()` 完全独立，存在**标签分裂**风险。当前代码的自动分类只有 `douyin`/`zhishi` 两个库，而 ChunkLabeler 打了 12+ 种标签。建议统一为一套标签体系，ChunkLabeler 的标签层级应与知识库分类对齐：
> ```
> 一级标签：知识库归属（douyin/zhishi/huashu）
> 二级标签：内容类型（种草/促销/产品介绍/情绪价值/过渡/互动）
> 三级标签：品类（美妆护肤/食品/服装/家居/数码）
> ```

### 1.4 去重层

| 能力 | 方案描述 | 实际现状 |
|------|---------|---------|
| 标题去重 | existsByKbIdAndTitleAndDeleted | ✅ 已有（第 236 行） |
| MD5 指纹预检 | content_fingerprint 字段 | ❌ 待实现 |
| SimHash 近似预检 | simhash 字段 + 海明距离 | ❌ 待实现 |
| Chunk 级向量去重 | 阈值 0.92 跳过 / 0.80 降权 | ❌ 待实现 |
| 检索时语义去重 | 余弦相似度 > 0.90 去重 | ❌ 待实现 |
| 去重预览 API | dedup-preview 端点 | ❌ 待实现 |

**评估：三级去重设计合理。** 可升级点：

> **升级点 A8**：方案中 SimHash 实现没有给出具体算法。SimHash 在**短文本**（< 500字的话术）上效果较差，因为特征太少、碰撞率高。建议：
> - 对短文本（< 200字）直接跳过 SimHash，只用 MD5 + 向量去重
> - 或改用 **MinHash**（基于 Jaccard 相似度），对短文本效果更好
> - 配置化：`simhash-min-length: 200`

> **升级点 A9**：方案中 chunk 级去重的阈值（0.92 跳过 / 0.80 降权）对**话术场景可能偏低**。话术文档经常有"模板化变体"（换产品名但话术结构相同），建议新增一个"模板检测"层：
> - 将产品名/价格等实体替换为占位符后再算相似度
> - 这样 "姐妹们这套水乳398" 和 "姐妹们这套面膜298" 会被正确识别为同模板变体

> **升级点 A10**：方案的批量去重优化（第四节 4.3）使用 Milvus 批量搜索，但当前 Milvus Java SDK 的 `search()` 方法**不直接支持多向量批量查询**（需要手动拼接 `SearchParam`）。建议明确指出使用 `MilvusServiceClient.search()` 的 `withVectors(List<List<Float>>)` 参数形式，并增加**批量大小限制**（如 50 个/批，避免单次 RPC 超时）。

### 1.5 RAG 集成层

| 能力 | 方案描述 | 实际现状 |
|------|---------|---------|
| hybridSearch 基础能力 | Milvus + ES + RRF 融合 | ✅ 完整实现（第 442-665 行） |
| 查询改写 | queryRewriteService | ✅ 已有（生成 2-3 子查询） |
| boost 加权 | boostFactor 字段 + 反馈更新 | ✅ 已有（第 603-618 行） |
| 缓存 + 熔断 | Redis 缓存 + Resilience4j | ✅ 已有 |
| LiveAiServiceImpl RAG 注入 | buildPrompt() 末尾追加参考 | ❌ 待实现 |
| 多查询扩展 | buildRagQueries() 3 维度 | ❌ 待实现 |
| 最低相关度过滤 | min-score: 0.5 | ❌ 待实现 |
| 元数据过滤 | hybridSearch 新增 filter 参数 | ❌ 待实现 |
| 来源归因 + 反馈 | ragReferences + boost 联动 | ❌ 待实现 |
| huashu 知识库 | ID:4 初始化 | ❌ 仅有 douyin + zhishi |

**评估：RAG 底层就绪，应用层完全缺失。** 关键升级点：

> **升级点 A11（重要）**：方案中 `buildRagContext()` 使用 `knowledgeBaseService.hybridSearch()` 进行检索，但当前 `hybridSearch()` 已有 **Redis 缓存**（TTL 3600秒）。话术生成是实时场景，如果用户刚导入新话术文档就生成，可能命中旧缓存。建议：
> - RAG 检索时传入 `skipCache=true` 参数（需扩展 hybridSearch 签名）
> - 或在文档导入成功后主动清除相关知识库的缓存：`cacheManager.evict("kb:" + kbId + ":*")`

> **升级点 A12（重要）**：方案的多查询扩展（6.2.2）与现有的 `queryRewriteService` 功能重叠。当前代码已有子查询生成（2-3 个），但绑定了"用户已绑定抖音账号"的前置条件。建议：
> - **合并策略**：方案的 `buildRagQueries()` 做品类/卖点维度扩展，`queryRewriteService` 做语义改写
> - 两者结果合并去重后统一检索
> - 避免重复实现子查询逻辑

> **升级点 A13**：方案中 RAG Prompt 注入格式：
> ```
> ---
> 以下是优秀话术案例（仅供参考风格和技巧，不要照搬内容）：
> 案例1：xxx
> ---
> ```
> 这种格式对 LLM 的**指令遵从**可能不够强。建议使用**结构化 XML 标签**包裹，主流 LLM 对此的遵循度更高：
> ```xml
> <reference_scripts>
> <note>仅参考风格和技巧，禁止照搬</note>
> <script id="1" category="促销" score="0.89">话术内容...</script>
> <script id="2" category="种草" score="0.85">话术内容...</script>
> </reference_scripts>
> ```

> **升级点 A14**：方案遗漏了**Token 预算管理**。当前 Tomcat timeout 30 分钟、但 LLM 有 token 上限。建议在 RAG 注入前计算：
> ```
> 可用 RAG Token = LLM 上下文窗口 - 系统 Prompt - 产品信息 - 人设信息 - 安全余量
> ```
> 根据可用 Token 动态调整 `max-context-length`，而非固定 2000 字符。

### 1.6 Controller / API 层

| 端点 | 方案需求 | 实际现状 |
|------|---------|---------|
| POST /{kbId}/document | 纯文本上传 | ✅ 已有 |
| POST /import-from-path | 目录导入 | ✅ 已有 |
| POST /import-from-path-async | 异步导入 | ✅ 已有 |
| POST /import-active-jobs | 活跃任务查询 | ✅ 已有 |
| POST /import-status/{jobId} | 进度查询 | ✅ 已有 |
| POST /{kbId}/search | 混合搜索 | ✅ 已有 |
| POST /feedback | 反馈 | ✅ 已有 |
| POST /{kbId}/upload-file | 单文件上传 | ❌ 待实现 |
| POST /{kbId}/upload-files | 批量文件上传 | ❌ 待实现 |
| POST /{kbId}/dedup-preview | 去重预览 | ❌ 待实现 |
| POST /{kbId}/import-incremental | 增量导入 | ❌ 待实现 |
| GET /{kbId}/documents/{docId}/chunks | chunk 预览 | ❌ 待实现 |

> **升级点 A15**：方案中 `dedup-preview` 接口使用 `GET` 方式查看 chunks，但项目 API 规范是**统一 POST**。需修正为 `POST /{kbId}/documents/{docId}/chunks`。

> **升级点 A16**：方案中文件上传使用 `@RequestParam("file") MultipartFile`，但当前 `application.yml` 的 multipart 配置只设了 `max-file-size: 50MB`。批量上传场景（`upload-files` 接收 `List<MultipartFile>`）需要额外设置 `max-request-size`：
> ```yaml
> spring.servlet.multipart:
>   max-file-size: 50MB
>   max-request-size: 200MB  # 批量上传：4 × 50MB
> ```
> 当前 `max-request-size: 100MB`（application.yml 第 7 行），可能不够。

### 1.7 数据库层

| 表/字段 | 方案需求 | 实际现状 |
|---------|---------|---------|
| ai_kb_document 基础字段 | 已有 | ✅ 完整 |
| ai_kb_document.boost_factor | 已有 | ✅ BigDecimal(5,2) |
| ai_kb_document.source_type | 已有 | ✅ 支持 manual/evolved/viral/live_review |
| ai_kb_document.content_fingerprint | 新增 | ❌ 缺失 |
| ai_kb_document.simhash | 新增 | ❌ 缺失 |
| ai_kb_document.metadata | 新增 JSONB | ❌ 缺失 |
| kb_import_report | 新建表 | ❌ 缺失 |
| kb_import_checkpoint | 新建表 | ❌ 缺失 |
| kb_import_dedup_log | 新建表（可选） | ❌ 缺失 |

> **升级点 A17**：方案中 `source_type` 字段已有值 `manual/evolved/viral/live_review`，方案新增 `"purchased"`。但 Entity 中 `sourceType` 是 `String(32)`，没有枚举约束。建议在方案中明确新值 `purchased` 需要更新前端的下拉选项和后端的校验逻辑。

> **升级点 A18**：`kb_import_report` 表缺少 `deleted` 字段。按照项目规范，**所有表包含 `deleted INTEGER NOT NULL DEFAULT 0`**。方案中 3 张新表都未包含此字段。

> **升级点 A19**：`kb_import_report.errors` 使用 `JSONB` 类型，但项目中 Entity 映射 JSONB 需要 Hibernate 6 的 `@JdbcTypeCode(SqlTypes.JSON)` 注解。方案未提及 Entity 映射方式。

### 1.8 配置层

| 配置项 | 方案需求 | 实际现状 |
|--------|---------|---------|
| app.ai.kb.cache-enabled | 已有 | ✅ |
| app.ai.kb.init-enabled | 已有 | ✅ |
| app.ai.kb.import-parallelism | 已有 | ✅ 默认 6 |
| app.ai.kb.doc-archive-* | 已有 | ✅ |
| app.ai.kb.dedup.* | 新增 7 项 | ❌ 全部缺失 |
| app.ai.kb.rag.* | 新增 8 项 | ❌ 全部缺失 |
| app.ai.kb.security.* | 新增 3 项 | ❌ 全部缺失 |

**评估：配置部分设计完整。无重大升级点。**

---

## 二、方案自身可升级点汇总

### P0 级（影响核心功能正确性）

| # | 升级点 | 描述 | 影响范围 |
|---|--------|------|---------|
| A11 | RAG 缓存穿透 | hybridSearch 有 Redis 缓存，新导入文档可能命中旧缓存 | RAG 检索时效性 |
| A12 | 子查询重复实现 | buildRagQueries 与现有 queryRewriteService 功能重叠 | 检索质量、代码冗余 |
| A18 | 新表缺少 deleted 字段 | 3 张新表均未按项目规范包含逻辑删除字段 | 数据库规范一致性 |

### P1 级（影响质量和健壮性）

| # | 升级点 | 描述 | 影响范围 |
|---|--------|------|---------|
| A3 | PDFBox 3.x API 变更 | Loader.loadPDF(File) 已废弃 | PDF 解析编译 |
| A5 | 话术关键词硬编码 | DocumentTypeDetector 关键词有时效性、行业偏向 | 文档类型检测准确率 |
| A7 | 标签体系分裂 | ChunkLabeler 与 classifyByContent 独立运作 | 标签一致性 |
| A8 | SimHash 短文本效果差 | 话术 < 200 字时碰撞率高 | 去重准确性 |
| A13 | RAG Prompt 格式弱 | 纯文本分隔符对 LLM 指令遵从不足 | 话术生成质量 |
| A14 | Token 预算管理缺失 | 固定 2000 字符不适配不同 LLM 上下文窗口 | Prompt 截断风险 |
| A15 | API 方法不统一 | chunks 查看用 GET，违反统一 POST 规范 | API 一致性 |
| A16 | 批量上传 request-size 不足 | 当前 100MB 不够 4×50MB 批量 | 批量上传失败 |

### P2 级（优化和增强）

| # | 升级点 | 描述 | 影响范围 |
|---|--------|------|---------|
| A1 | 依赖版本过旧 | PDFBox 3.0.3 / POI 5.2.5 有更新版 | 安全补丁 |
| A2 | DOCX 页眉/文本框遗漏 | 未提取浮动内容 | 内容完整性 |
| A4 | 扫描件检测阈值硬编码 | 封面/目录页可能误判 | 用户体验 |
| A6 | 分块阈值不可配置 | 不同品类话术长度差异大 | 分块质量 |
| A9 | 模板化变体检测缺失 | 换产品名的同模板话术无法去重 | 去重精度 |
| A10 | Milvus 批量搜索限制 | 需明确批量大小和超时 | 性能稳定性 |
| A17 | source_type 新值未联动 | 前端/校验未更新 | 数据完整性 |
| A19 | JSONB 映射方式未指定 | Entity 需 @JdbcTypeCode 注解 | 编译正确性 |

---

## 三、新增升级建议（方案未覆盖的能力）

### B1. 导入进度 WebSocket 推送（P2）

当前异步导入使用轮询（`/import-status/{jobId}`）。对于大批量导入（100+ 文件），建议增加 WebSocket 实时推送：

```java
@MessageMapping("/kb/import/{jobId}")
public void subscribeProgress(@DestinationVariable String jobId) {
    // 推送每个文件的处理状态
}
```

### B2. 导入任务队列化（P1）

当前导入在应用线程池中执行。如果同时多人导入大量文件，可能耗尽线程池。建议：
- 大文件（> 10MB）或批量（> 20 文件）自动走 RabbitMQ 异步队列
- 复用现有 RabbitMQ 基础设施

### B3. 知识库容量管理（P1）

方案未涉及知识库容量上限。建议新增：
```yaml
app.ai.kb:
  max-documents-per-kb: 10000
  max-total-size-mb: 5000  # 5GB
  warn-threshold: 0.8       # 80% 时告警
```

### B4. 话术质量评分（P2）

导入后自动对每条话术 chunk 做质量评分（调用 LLM）：
- 语言流畅度
- 逻辑完整性
- 是否含违规用语
- 评分结果存入 metadata，低分话术降低 boost_factor

### B5. 导入回滚能力（P1）

当前无法撤销一次导入。建议：
- `kb_import_report` 记录该批次所有 doc_id
- 新增 `POST /{kbId}/import/{reportId}/rollback` 端点
- 回滚 = 批量 soft delete 该批次所有文档 + 从 Milvus/ES 删除向量

### B6. 文件预览能力（P2）

上传前允许用户预览解析效果：
- `POST /{kbId}/preview-parse`：上传文件，返回解析后的文本 + 分块预览 + 标签预览
- 用户确认后再正式导入

---

## 四、修正建议汇总（直接应用到方案）

以下修正建议可直接合并到 `kb-doc-pdf-batch-import.md`：

### 4.1 第二节（文档解析器）修正

```diff
- try (PDDocument doc = Loader.loadPDF(file.toFile())) {
+ try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBufferedFile(file))) {
```

### 4.2 第三节（话术分块）修正

```diff
  private String contentType;  // "general" | "script" | "auto"
+ // 分块参数配置化
+ private Integer chunkMinLength = 30;   // 最短合并阈值
+ private Integer chunkMaxLength = 1000; // 最长回退阈值
```

### 4.3 第五节（文件上传）修正

```diff
  spring.servlet.multipart:
    max-file-size: 50MB
-   max-request-size: 50MB
+   max-request-size: 200MB  # 支持批量上传 4×50MB
```

### 4.4 第六节（RAG 集成）修正

```diff
  private String buildRagContext(...) {
+     // 导入后清除缓存，确保新文档可被检索
+     // hybridSearch 增加 skipCache 参数
-     List<SearchResult> results = knowledgeBaseService.hybridSearch(kbId, query, ragTopK, userId);
+     List<SearchResult> results = knowledgeBaseService.hybridSearch(kbId, query, ragTopK, userId, null, true);
  }
```

### 4.5 第九节（数据库）修正

```diff
  CREATE TABLE IF NOT EXISTS kb_import_report (
      id BIGSERIAL PRIMARY KEY,
      ...
+     deleted INTEGER NOT NULL DEFAULT 0,
      create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE IF NOT EXISTS kb_import_checkpoint (
      id BIGSERIAL PRIMARY KEY,
      ...
+     deleted INTEGER NOT NULL DEFAULT 0,
      UNIQUE(kb_id, source_path)
  );

  CREATE TABLE IF NOT EXISTS kb_import_dedup_log (
      id BIGSERIAL PRIMARY KEY,
      ...
+     deleted INTEGER NOT NULL DEFAULT 0,
      create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
```

### 4.6 第十一节（改动文件清单）补充

```diff
  | # | 文件 | 改动类型 | 说明 |
  |---|------|---------|------|
+ | 23 | SimHash.java | **新建** | SimHash 算法实现（含短文本最低长度保护） |
+ | 24 | KnowledgeBoostService.java | 修改 | RAG 反馈联动 boost 更新 |
```

---

## 五、总体评估结论

| 维度 | 评分 | 说明 |
|------|------|------|
| **完整性** | 9/10 | 覆盖解析/分块/去重/RAG/API/前端/SQL/配置 8 个层，极少遗漏 |
| **准确性** | 7/10 | 代码行号引用准确，但部分 API 调用方式与实际有出入（PDFBox 3.x、Milvus 批量） |
| **可执行性** | 8/10 | 5 Phase 分步清晰，但缺少与现有 queryRewriteService 的整合说明 |
| **健壮性** | 7/10 | 三级去重 + 安全扫描完善，但缓存穿透、Token 预算、导入回滚未覆盖 |
| **规范符合度** | 7/10 | 新表缺 deleted 字段、chunks API 用 GET 违反统一 POST 规范 |

**总评：方案质量高，架构设计合理，19 处可升级点中 3 处 P0 需在实施前修正。**

建议在开始编码前，先将本评估报告中的 **P0 级修正**（A11/A12/A18）合并到方案中，P1 级在对应 Phase 实施时同步修正。
