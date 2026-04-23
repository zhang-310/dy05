# 知识库 DOC/PDF 批量导入 — 升级执行方案

> 依据：`kb-doc-pdf-batch-import.md` v2.3  
> 执行方式：按 Phase 顺序，每步完成后勾选，再进入下一步

---

## 一、总览

| Phase | 目标 | 任务数 | 验收 |
|-------|------|--------|------|
| **Phase 1** | 文件解析 + 目录导入扩展 | 1–16 | DOC/PDF 可解析，目录支持 .doc/.docx/.pdf，话术分块生效 |
| **Phase 2** | 去重机制 | 17–24 | 文档级预检 + chunk 级去重 + 去重预览接口 |
| **Phase 3** | RAG 话术集成 | 25–35 | huashu 库 + buildRagContext + 并行检索 + 话术生成带参考 |
| **Phase 4** | 运维 + 安全 | 36–42 | 导入报告表 + 增量导入 + ContentSecurityScanner |
| **Phase 5** | API + 前端 | 43–53 | upload-file、dedup-preview、前端上传与 RAG 开关 |

---

## 二、Phase 1 — 文件解析 + 目录导入扩展

### 2.1 依赖与解析器

- [x] **1** `pom.xml` 新增：`pdfbox` 3.0.3、`poi-scratchpad` 5.2.5、`tika-core` 2.9.1（可选）
- [x] **2** 新建 `module/ai/util/DocumentParser.java`：`parse(Path)` / `parse(InputStream, fileType)`，支持 .md/.txt/.doc/.docx/.pdf
- [x] **3** DocumentParser：magic bytes 校验（PDF/OLE2/ZIP）
- [x] **4** DocumentParser：加密文件检测（PDF/DOC/DOCX），抛专用错误码 AI_DOC_ENCRYPTED(4064)
- [x] **5** DocumentParser：扫描件检测（PDF 文字过少），抛 AI_DOC_SCAN_ONLY(4065)
- [x] **6** DocumentParser：`parseWithMetadata()`，元数据写入后续 metadata 字段

### 2.2 分块与类型检测

- [x] **7** 新建 `DocumentTypeDetector`：script/mixed/general 自动检测（关键词可配置 app.ai.kb.detect.script-keywords）
- [x] **8** 新建 `ScriptAwareChunker`：按序号/标题/空行切分，30–1000 字块；generalSplit 512 字+标签
- [x] **9** 新建 `MixedDocumentProcessor`：混合文档分段，话术区用 ScriptAwareChunker
- [x] **10** 新建 `ChunkLabeler`：二级 type: / 三级 cat: 标签

### 2.3 目录导入扩展

- [x] **11** `KnowledgeBaseImportServiceImpl`：walk 支持 .doc/.docx/.pdf、maxDepth、maxFiles、排除隐藏目录（. 开头）
- [x] **12** 目录结构元数据：`extractDirMetadata`（relativePath、dirCategory、dirSubCategory）；目录导入时传入 uploadDocument 写入 doc.metadata
- [x] **13** 目录级错误隔离：单文件失败记 errors，不中断（已有）
- [x] **14** `KnowledgeBaseServiceImpl.uploadDocument()`：按 contentType 选择分块策略（script/auto/mixed/general），resolveChunks + labels 入 metadata
- [x] **15** KbDocumentUploadVO 新增 `contentType`（general | script | auto）；上传接口传 contentType
- [x] **16** 目录导入改用 `DocumentParser.parse(file)`，扩展名过滤支持 .doc/.docx/.pdf，fileType 用 detectFileType

**Phase 1 验收**：单文件 DOC/PDF 解析正确；目录导入含 .docx/.pdf 文件可成功；话术文档按条分块。

---

## 三、Phase 2 — 去重机制

- [x] **17** SQL：`ai_kb_document` 新增 `content_fingerprint`、`simhash`、`metadata`（JSONB）及索引（`sql/ai/migration-kb-document-dedup.sql`）
- [x] **18** 文档级预检：MD5 指纹 + SimHash（短文本 < simhash-min-length 跳过），在 uploadDocument 入口；指纹命中直接返回已存在文档
- [x] **19** `application.yml` 新增 `app.ai.kb.dedup.*` 配置
- [x] **20** uploadDocument：分块后逐条向量去重（embedding + search topK=1）；skip（≥skip-threshold）/ downweight（≥downweight-threshold）
- [x] **21** VectorService.batchSearch 已用于 applyChunkDedup 批量向量去重（uploadDocument 分块后批量检索）
- [x] **22** hybridSearch：结果语义去重（applySemanticDedup，余弦≥hybrid-dedup-threshold 去重）；配置 app.ai.kb.hybrid-dedup-threshold（0=关，0.9 建议开启）
- [x] **23** 新增接口 `POST /api/v1/ai/knowledge-base/{kbId}/dedup-preview`
- [x] **24** 测试：重复/近似文档导入行为符合预期（KnowledgeBaseServiceImplTest：dedupPreview 高/中/低分对应 skip/downweight/keep，无权限/库不存在抛异常）

---

## 四、Phase 3 — RAG 话术集成

- [x] **25** KnowledgeBaseInitializer：`createIfAbsent("huashu", ...)`，按名称不写死 ID
- [x] **26** application.yml 新增 `app.ai.kb.rag.*`（top-k、min-score、max-context-chars、token-budget-ratio、parallel-timeout-sec）
- [x] **27** LiveAiServiceImpl：`buildRagContext()`，XML 格式 `<reference_scripts>`，Token 预算
- [x] **28** buildCategoryQueries + queryRewriteService 两阶段查询；多查询 CompletableFuture 并行，5s 超时
- [x] **29** 最低相关度过滤（ragMinScore）
- [x] **30** Token 预算：budget = min(ragMaxContextChars, 预留主 prompt 后剩余）
- [x] **31** KnowledgeBaseService.hybridSearch 重载：metadataFilter、skipCache；resolveKbIdByName
- [x] **32** buildProductScriptPrompt 中注入 RAG（与 buildPrompt 一致）
- [x] **33** 直播 buildPrompt 各类型（含情绪/过渡）均注入 RAG
- [x] **34** RAG 来源归因：buildRagContext 返回 RagContextResult(xml, refs)；LiveAiResultVO.ragRefs、RagRefVO(docId, chunkId, title, contentPreview, score)；doGenerate 设置 ragRefs；boost 反馈沿用 submitFeedback
- [x] **35** 测试：话术生成时 Prompt 含参考案例，且新导入文档可被检索（KnowledgeBaseServiceImplTest：hybridSearch(..., skipCache=true) 不读 Redis 缓存，供 RAG 使用新导入文档）

---

## 五、Phase 4 — 运维 + 安全

- [x] **36** SQL：`kb_import_report`、`kb_import_checkpoint`（含 deleted、update_time）（`sql/ai/migration-kb-import-report-checkpoint.sql`）
- [x] **37** 导入报告持久化（KbImportReport Entity/Repo + importFromPath 结束后写入）
- [x] **38** 增量导入：importIncremental 按 lastImportTime 过滤文件，完成后更新 checkpoint
- [x] **39** 新建 ContentSecurityScanner（隐私/违禁/编码）
- [x] **40** 上传/导入流程中调用安全扫描（不阻断，记入日志）
- [x] **41** application.yml 新增 security 配置（scan-on-upload / scan-on-import）
- [x] **42** 错误码 4064–4070 已在 Phase 1 注册（与设计 4060–4066 对应）

---

## 六、Phase 5 — API + 前端

- [x] **43** Controller：upload-file、upload-files（MultipartFile）
- [x] **44** Controller：dedup-preview
- [x] **45** Controller：import-incremental（当前委托 importFromPath，后续可接 checkpoint）
- [x] **46** Controller：chunks 查看（POST）
- [x] **47** 前端：知识库文件上传（doc/pdf）+ 文档类型选择（KnowledgeDocumentsPage 拖拽上传 + contentType）
- [x] **48** 前端：去重预览对话框（DedupPreviewDialog：粘贴内容 + 预览 + 确认导入）
- [x] **49** 前端：话术生成 RAG 开关 + 参考来源展示（ProductScriptManageDialog Switch；LiveScriptBuilderPage / LiveSessionDetailPage 生成开场/产品话术后展示 ragRefs 手风琴：标题、相关度、内容预览）
- [x] **50** 前端：知识库分类浏览/标签过滤（从 doc.metadata 解析 labels/keywords，标签 Chip 筛选）
- [x] **51** 前端：文档 chunk 预览（表格行「分块预览」+ 弹窗）
- [x] **52** 前端：导入对话框增量导入 + 文档类型（KnowledgeImportModal 增量复选框 + contentType + doc/pdf 拖拽）
- [x] **53** 前端：目录级进度展示（已有 ProgressModal 轮询 import-status）

---

## 七、当前执行步骤

**已完成**：Phase 1 步骤 1–6（pom + DocumentParser + 错误码 4064–4070）。

**说明**：文档解析专用错误码使用 4064–4070（AI_DOC_ENCRYPTED / AI_DOC_SCAN_ONLY / AI_DOC_FAKE_TYPE / AI_DOC_PARSE_FAIL / AI_DOC_TOO_LARGE / AI_IMPORT_DIR_DEPTH / AI_IMPORT_FILE_LIMIT），与设计文档 4060–4066 一一对应，避免与现有 4060–4063 冲突。

**已完成 Phase 3**：huashu 库、rag 配置、buildRagContext（XML + 并行检索 + 最低分过滤 + Token 预算）、hybridSearch 重载（metadataFilter/skipCache）、SearchResult 增加 chunkId/labels、直播与产品话术均注入 RAG。

**已完成 Phase 4 部分 + Phase 5 后端 API**：kb_import_report / kb_import_checkpoint 表、ContentSecurityScanner、上传/导入安全扫描（仅日志）、upload-file/upload-files、dedup-preview、import-incremental、chunks（POST）。

**已完成 Phase 4 收尾 + Phase 5 前端**：37 导入报告持久化、38 增量导入、47–53 前端（上传、chunk 预览、导入弹窗增量+类型、目录进度）。**本轮**：48 去重预览对话框、49 产品话术 RAG 开关、50 标签过滤。

**本轮升级（已完成）**：① 导入报告列表 API（`POST /{kbId}/import-reports`）与知识库文档页「导入历史」手风琴表格已就绪；② 直播话术 RAG 开关已全覆盖：LiveAiGenerateVO.useKbRef、generateWithLlm 按 useKbRef 控制 buildRagContext、整场生成时 `generateAndUpdateSlot` 传递 baseVo.useKbRef；前端 LiveScriptBuilderPage + ScriptPanel「参考话术库」Switch、LiveSessionDetailPage 场次详情话术 Tab 增加「参考话术库」Switch，generateOpening/generateProduct/generateFull 均传 useKbRef。

**本轮升级（已完成）**：① Phase 1 任务 12：目录结构元数据 extractDirMetadata 已实现并在 processOneFile 中调用，dirMeta 传入 uploadDocument 写入 doc.metadata；② Phase 2 任务 21：applyChunkDedup 已使用 vectorService.batchSearch 批量去重；③ Phase 2 任务 22：hybridSearch 已集成 applySemanticDedup（余弦相似度去重），配置 app.ai.kb.hybrid-dedup-threshold（0=关）；④ Phase 3 任务 34：RAG 来源归因已实现（buildRagContext 返回 RagContextResult(xml, refs)、LiveAiResultVO.ragRefs + RagRefVO、doGenerate 设置 ragRefs）；boost 反馈沿用现有 submitFeedback。

**本轮升级（已完成）**：① 任务 24：KnowledgeBaseServiceImplTest 中 dedupPreview 测试（高/中/低相似度对应 skip/downweight/keep，权限与库存在校验）；② 任务 35：hybridSearch(..., skipCache=true) 单元测试，验证不读 Redis 缓存，保证 RAG 可检索新导入文档。

**本轮升级（已完成）**：任务 49 参考来源展示：api/live 增加 LiveRagRef、LiveAiGenerateResponse；直播话术构建页与场次详情话术 Tab 在生成开场/产品话术成功后展示「参考来源」手风琴（标题、相关度 Chip、内容预览）。

**待选后置**：无；Phase 1–5 计划内任务已全部完成。
