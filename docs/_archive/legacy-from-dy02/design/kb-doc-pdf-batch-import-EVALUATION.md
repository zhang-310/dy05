# 知识库 DOC/PDF 批量导入 + 话术 RAG 集成 — 深度评估报告

> 评估对象：`docs/design/kb-doc-pdf-batch-import.md`  
> 评估日期：2026-03-04

---

## 一、功能定位与价值

### 1.1 一句话总结

在现有「仅 MD/TXT + 标题去重 + 固定 512 字分块」的知识库能力上，新增 **DOC/DOCX/PDF 解析、话术感知分块、三级去重、专用话术库与 RAG 注入**，使「购买的话术文档库」可批量入库并在直播/产品话术生成时作为参考，形成「素材 → 知识库 → 生成」闭环。

### 1.2 业务价值

| 维度 | 说明 |
|------|------|
| **用户侧** | 话术/文案包（doc/pdf）可一键导入，无需手工复制；生成话术时自动带出相似案例，提升可用性与风格一致性 |
| **产品侧** | 与「商品&直播升级」中 RAG/风格推荐等能力衔接，强化「数据驱动话术」卖点 |
| **技术侧** | 复用现有 Milvus+ES 混合检索与 BGE-M3 嵌入，扩展面在解析、分块、去重与 Prompt 注入，不改动核心检索架构 |

### 1.3 与现有代码的契合度（核查结果）

| 设计文档声称 | 代码核查结论 |
|--------------|--------------|
| 当前仅支持 .md/.txt | ✅ 已确认：`KnowledgeBaseImportServiceImpl` 第 117–121 行仅 `n.endsWith(".md") \|\| n.endsWith(".txt")`，第 193 行 `Files.readString` 直接读，无 DOC/PDF 解析 |
| uploadDocument 只接收 title+content | ✅ 已确认：`KnowledgeBaseService.uploadDocument(kbId, title, content, fileType, userId)`，Controller 用 `vo.getTitle()`/`vo.getContent()`，无 MultipartFile |
| 分块 512 字符 + 50 重叠 | ✅ 已确认：`KnowledgeBaseServiceImpl` 第 47–49 行 `CHUNK_SIZE=512`, `CHUNK_OVERLAP=50`，`splitIntoChunks` 固定切分 |
| 去重仅 existsByKbIdAndTitleAndDeleted | ✅ 已确认：导入时仅标题去重，无内容指纹/向量去重 |
| hybridSearch 无 metadataFilter/skipCache | ✅ 已确认：接口签名为 `hybridSearch(kbId, query, topK, userId)`，无过滤与缓存控制参数 |
| poi-ooxml 已存在 | ✅ 已确认：pom.xml 约 117–122 行存在 poi-ooxml，用于 Excel；PDFBox/Tika 当前未引入 |

**结论**：设计文档对「当前现状」的描述与代码一致，改造点与入口判断准确。

---

## 二、设计亮点

### 2.1 决策表清晰，可追溯

D1–D11 将「选型与策略」显式列出（如 PDFBox 3.x、话术分块策略、去重三级、专用库 huashu、RAG 缓存策略等），便于评审与后续变更时回溯原因。

### 2.2 话术场景针对性强的分块与检测

- **ScriptAwareChunker**：按序号/标题/空行等切分，避免 512 字把一条话术截断或两条合并，符合「每条 100–400 字」的形态。
- **DocumentTypeDetector**：用分隔符模式 + 段落长度分布 + 话术关键词做 script/mixed/general 判定，减少误用通用分块。
- **ChunkLabeler**：二级（type:种草/促销/…）、三级（cat:美妆/食品/…）标签与 `classifyByContent()` 对齐，利于后续按标签过滤与统计。

### 2.3 去重分层合理

- **文档级**：MD5 指纹 + SimHash（且短文本保护 <200 字跳过）快速预检，避免整文档重复走分块与向量化。
- **Chunk 级**：向量相似度 0.92 跳过、0.80–0.92 降权，阈值可配置。
- **检索级**：结果语义去重（如 0.90 余弦），减少重复片段占满 topK。

配合「去重预览」接口，用户可在正式导入前看到 skip/downweight/keep 分布，体验和可控性都好。

### 2.4 RAG 集成方案克制且可落地

- **注入方式**：Prompt 末尾追加 `<reference_scripts>` 区块，不重构现有 `LivePromptBuilder.buildPrompt()` 主流程，改动面小。
- **多查询**：品类扩展（buildCategoryQueries）+ 复用现有 queryRewriteService，不重复造语义改写轮子。
- **P0 修正**：RAG 检索 skipCache + 导入后清除该 KB 缓存，避免「新导入不可见」问题，与文档中风险表对应。

### 2.5 安全与健壮性

- **Magic bytes**：防 exe 改后缀冒充 pdf/doc，实现简单、收益明确。
- **加密/扫描件**：PDF/DOC/DOCX 加密检测与扫描件（文字过少）检测，给出明确错误或提示，不静默失败。
- **ContentSecurityScanner**：隐私/违禁/编码异常以「警告 + 记录」为主，不阻断导入，便于合规与人工抽检。

### 2.6 运维与可观测

- **kb_import_report**：持久化每次导入的 success/failed/skipped/dedup 等，便于排查与统计。
- **增量导入**：按路径 + 上次导入时间戳只处理新/改文件，适合定期同步话术包。
- **目录级错误隔离**：单目录/单文件失败不拖垮整次导入，与现有 `processOneFile` 的并行与错误收集一致。

---

## 三、风险与缺口

### 3.1 实现与依赖风险

| 风险 | 说明 | 建议 |
|------|------|------|
| **PDFBox 3.x API** | 文档已按 3.x 使用 `Loader.loadPDF(RandomAccessReadBufferedFile)`；若实际引入 2.x 需改 API（如 `PDDocument.load()`） | 在 pom 中明确 `pdfbox 3.0.3`，并在 Phase 1 单测中覆盖 PDF 解析 |
| **POI scratchpad 与 HWPF** | 旧版 .doc 依赖 `poi-scratchpad`，与 poi-ooxml 同版本 5.2.5 需兼容；部分复杂 doc 可能仍有解析差异 | 准备若干真实 .doc 样本做回归，文档中已说明「复杂格式可能部分丢失」 |
| **queryRewriteService 前置条件** | 文档注明「当前绑定用户已绑定抖音账号」，RAG 场景若用系统/后台账号需放宽或传参 | 在实现 buildRagContext 时确认 rewrite 的调用方与权限，必要时为 RAG 单独提供「无账号」改写或跳过改写 |
| **Milvus 批量搜索 API** | 去重优化依赖「批量 embedding + 单次 Milvus 批量 search」；需确认当前 VectorService 是否暴露 batchSearch 或等价能力 | 查阅现有 Milvus 封装，若无则先在 Phase 2 做逐条去重，再补批量优化 |

### 3.2 性能与规模

| 点 | 说明 | 建议 |
|----|------|------|
| **大文件/大目录** | 50MB 单文件、5000 文件上限、max-depth=10 已给出；万级文件时遍历与解析仍可能长时间占用线程与内存 | 目录遍历建议用「流式」或分批列目录，避免一次 `toList()` 全进内存；单文件解析可考虑限流或队列 |
| **去重与向量化顺序** | 文档级预检在分块前，chunk 级去重在「分块后、插入前」；若先批量 embedding 再 batchDedup，需保证与当前「单文档单次 uploadDocument」的事务/幂等语义一致 | 实现时明确：同一文档内 chunk 去重是否允许「部分 skip 部分 insert」，以及失败回滚边界 |
| **RAG 多查询延迟** | 品类扩展 3 条 + 每条 rewrite 2–3 条，约 6–12 次 hybridSearch；skipCache=true 时无缓存，延迟叠加 | 可考虑并行多路检索再合并，或对 RAG 路径做短期缓存（如 5 分钟）降低重复生成时的延迟 |

### 3.3 未覆盖或需后续明确的内容

| 项 | 说明 |
|----|------|
| **ProductScriptServiceImpl 注入点** | 文档 6.2.5 说「同样在调用 liveAiService.generateProductScript() 前注入 RAG」；但产品话术当前由 `LiveProductAiServiceImpl.generateProductScript` 直接调 `LiveAiService`，Prompt 在 Live 侧构建。需明确：RAG 上下文是在 Live 的 buildProductScriptPrompt 里加，还是 Product 侧先查 RAG 再通过 VO 传入。建议统一在 Live 侧 buildProductScriptPrompt 中注入，与 buildPrompt 一致。 |
| **情绪/过渡/短视频的 buildXxxPrompt** | 扩展 RAG 到情绪、过渡、短视频（6.2.6）需在对应 buildEmotionalUserPrompt、buildTransition 等位置增加 RAG 区块；文档未写出具体方法名与代码位置，实现时需对照 LiveAiServiceImpl 现有方法逐一加。 |
| **huashu 知识库的创建时机** | 文档 7.1 写 KnowledgeBaseInitializer 中 ensureKb(4L, "huashu", ...)。若已有库 ID 1–3 被占用或迁移顺序不同，ID 4 可能冲突。建议用「按名称查找或创建」而非写死 ID。 |
| **前端上传与现有知识库页** | 文档 8.1 为「知识库管理页面」新增 Dropzone + 文档类型选择；需确认当前知识库前端的路由与组件结构（如是否已有「上传文档」入口），避免重复或冲突。 |

### 3.4 与项目规范的符合度

| 规范 | 符合情况 |
|------|----------|
| **API 统一 POST** | 文档 8.6 已修正 chunks 接口为 POST，与项目一致。 |
| **表含 deleted/update_time** | 文档 9.2–9.4 与变更日志 v2.1 已为 kb_import_report、kb_import_checkpoint、kb_import_dedup_log 补全 deleted 及 update_time。 |
| **错误码** | 解析/安全校验抛出 VALIDATION_FAIL 等，未新增错误码段；若需细分「文件类型无效」「加密文件」等可考虑在 4xxx 段扩展。 |
| **数据隔离** | 知识库按 userId 归属，uploadDocument 已校验 kb.getUserId().equals(userId)；导入与 RAG 检索均带 userId，符合 owner 隔离。 |

---

## 四、实施复杂度与优先级建议

### 4.1 工作量粗估

| Phase | 内容 | 预估（人天） | 备注 |
|-------|------|--------------|------|
| Phase 1 | 解析器 + 目录扩展 + 分块策略 + 类型检测 | 5–7 | DocumentParser 与三套分块/检测逻辑占大头 |
| Phase 2 | 去重（文档级 + chunk 级 + 预览） | 3–4 | 依赖 Milvus 批量能力与配置项 |
| Phase 3 | RAG（huashu 库 + buildRagContext + 多场景注入） | 4–5 | 与 LiveAiServiceImpl / LivePromptBuilder 耦合，需回归话术生成 |
| Phase 4 | 报告表 + 增量导入 + 安全扫描 | 2–3 | 相对独立 |
| Phase 5 | Controller 新接口 + 前端上传/预览/开关 | 3–4 | 前端工作量依赖现有知识库页结构 |

**整体**：约 17–23 人天（约 3–4 周单人），与文档「分阶段实施」一致。

### 4.2 关键路径与依赖

```
pom（PDFBox/scratchpad）
  → DocumentParser（doc/docx/pdf + magic bytes + 加密/扫描件）
  → KnowledgeBaseImportServiceImpl（扩展后缀 + 调用 DocumentParser + 目录增强）
  → KnowledgeBaseServiceImpl.uploadDocument（分块策略选择 + 文档级预检 + chunk 去重）
  → 表结构迁移（content_fingerprint, simhash, metadata, kb_import_report 等）
  → RAG：huashu 库 + hybridSearch 重载（metadataFilter, skipCache）+ buildRagContext
  → LiveAiServiceImpl / LivePromptBuilder 注入 RAG
  → Controller upload-file / dedup-preview / import-incremental
  → 前端
```

建议：**Phase 1 完成并跑通「DOC/PDF 目录导入 + 话术分块」后再铺开去重与 RAG**，避免同时改解析、分块、检索、Prompt 导致问题难以定位。

### 4.3 优先级建议（在文档 P0/P1/P2 基础上的微调）

| 建议 | 理由 |
|------|------|
| **P0 保持** | 文档类型自动检测（#4）、RAG 两阶段查询与最低分过滤（#10/#11）、skipCache + 导入后清缓存（#21）—— 直接决定「话术文档是否被正确分块」和「新文档是否被检索到」，必须首轮实现。 |
| **P1 中优先** | 扫描件/加密检测（#1/#2）、Magic bytes（#19）、文档级预检（#7）、ChunkLabeler 与标签体系统一（#6/#25）—— 提升健壮性与可运维性，且为 RAG 元数据过滤打基础。 |
| **可后置** | 去重预览（#8）、RAG 来源归因与 boost 反馈（#14）、分类浏览与 chunk 预览（#17/#18）—— 对「能用起来」非必须，可在主流程稳定后迭代。 |

---

## 五、总结与建议

### 5.1 总体评价

- **完整性**：从解析、分块、去重、上传 API、RAG 注入到安全与运维，链条完整；升级项编号与测试用例对应清晰，便于执行与验收。
- **与现状贴合度**：对现有知识库与话术生成链路的描述准确，改造点明确，依赖（POI、现有 hybridSearch、LivePromptBuilder）判断正确。
- **风险控制**：加密/扫描件/伪装文件、缓存与多查询策略、短文本 SimHash 保护等均有考虑，并配有风险表与应对说明。

### 5.2 实施前建议

1. ~~**确认 VectorService/Milvus 能力**~~ → **已采纳（C4）**：方案 v2.3 明确 Phase 2 先逐条去重，后续再封装 batchSearch；见文档四、4.3。
2. ~~**统一 RAG 注入层**~~ → **已采纳（C3）**：RAG 注入统一在 Live 侧 buildXxxPrompt，ProductScriptServiceImpl 不独立做 RAG；见文档六、6.2.5。
3. ~~**huashu 库初始化**~~ → **已采纳（C1）**：改为 createIfAbsent("huashu", ...)，resolveScriptKbId 按名称查找；见文档七、7.1。
4. **queryRewriteService 在 RAG 的用法**：评审结论为误判，源码无账号时 graceful 降级，无需改动。
5. **RAG 多查询并行**：**已采纳（C2）** — 多查询改为 CompletableFuture 并行检索，5s 超时保护；见文档六、6.2.1。
6. **文档解析错误码**：**已采纳（C5）** — 新增 4060–4066 专用错误码；见文档十点五。

### 5.3 建议的验收顺序

1. Phase 1：单文件 DOC/PDF 解析 + 话术分块 + 目录导入（.doc/.docx/.pdf）→ 能在知识库中看到正确分块与标签。
2. Phase 2：文档级 + chunk 级去重 → 重复/近似文档行为符合预期，去重预览接口与文档一致。
3. Phase 3：仅「直播话术」一路 RAG（buildPrompt）→ 能检索到 huashu 并注入 Prompt，生成结果明显受参考影响。
4. 再扩展：产品话术、情绪、过渡、短视频及元数据过滤、反馈 boost 等。

按上述顺序实施并验收，可最大程度控制风险并尽早形成「话术文档 → 知识库 → 生成」的可演示闭环。

---

## 六、v2.3 Cursor 评审采纳确认（2026-03-04）

| # | 建议 | 文档位置 | 核对结果 |
|---|------|----------|----------|
| C1 | huashu 库按名称查找/创建 | 七、7.1；升级项 #32 | ✅ createIfAbsent("huashu")，resolveScriptKbId 按名称查，无写死 ID |
| C2 | RAG 多查询并行检索 | 六、6.2.1；升级项 #33 | ✅ CompletableFuture.runAsync 并行，ConcurrentHashMap/Collections.synchronizedList，5s 超时 |
| C3 | RAG 注入统一在 Live 侧 | 六、6.2.5；升级项 #34 | ✅ 明确 Product 不独立做 RAG，buildProductScriptPrompt 中注入 |
| C4 | Milvus 批量去重降级路径 | 四、4.3；升级项 #35 | ✅ Phase 2 先逐条，后续再封装 batchSearch |
| C5 | 文档解析专用错误码 4060–4066 | 十点五；升级项 #36 | ✅ 7 个错误码表 + ErrorCode/注册表/前端映射说明 |

**未采纳 5 项**：queryRewrite 账号绑定（源码已降级）、目录 toList 内存（5000 Path 可接受）、chunk skip/insert 语义（实现自然满足）、buildXxxPrompt 方法名（实现时定位）、前端组件冲突（实现时确认）— 理由合理，无需改方案。

**结论**：v2.3 已按评审意见完成合并，方案可进入实施阶段。
