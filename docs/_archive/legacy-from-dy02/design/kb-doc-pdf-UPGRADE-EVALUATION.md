# 知识库 DOC/PDF 升级方案 — 深度评估报告

> 评估对象：`kb-doc-pdf-UPGRADE-PLAN.md`（Phase 1–5 已全部勾选）  
> 依据：`kb-doc-pdf-batch-import.md` 设计决策、当前代码实现、运维与可维护性

---

## 一、与设计文档/决策的对照

| 决策/设计项 | 计划状态 | 实现情况 | 差距与建议 |
|-------------|----------|----------|------------|
| **D4 去重** | 17–24 已勾选 | 文档指纹 + SimHash + chunk 向量去重 + hybrid 结果语义去重 + dedup-preview 均已实现 | 无缺口 |
| **D5 RAG 注入** | 25–35 已勾选 | buildRagContext 注入 XML、useKbRef 开关、ragRefs 归因与前端展示 | 无缺口 |
| **D9 文件安全** | 36–42 | magic bytes + ContentSecurityScanner，仅记日志不阻断 | 符合「不阻断」设计 |
| **D10 缓存策略** | 计划未单列 | **RAG 检索已用 skipCache=true**，新导入文档可被 RAG 命中；**导入后主动清空知识库 Redis 缓存未实现** | **优化**：上传/目录导入成功后，按 kbId 清除该库的检索缓存（或约定 TTL 1h 内依赖 skipCache） |
| **D11 目录元数据** | 12 已勾选 | extractDirMetadata 已写 doc.metadata（relativePath、dirCategory、dirSubCategory） | **优化**：前端知识库文档列表未提供「按目录分类(dirCategory)」筛选，仅支持 labels/keywords；可增加筛选项以发挥目录元数据价值 |

---

## 二、实现完整性与边界

### 2.1 已对齐的部分

- **Phase 1**：DocumentParser（含 magic bytes、加密/扫描件检测）、DocumentTypeDetector、ScriptAwareChunker、MixedDocumentProcessor、ChunkLabeler、目录 walk 支持 doc/docx/pdf、extractDirMetadata → uploadDocument。
- **Phase 2**：content_fingerprint/simhash/metadata、文档级预检、applyChunkDedup 使用 batchSearch、applySemanticDedup + hybrid-dedup-threshold、dedup-preview 接口；单元测试覆盖 dedupPreview 行为与 skipCache。
- **Phase 3**：huashu 库、rag 配置、buildRagContext（并行检索、最低分、Token 预算）、直播/产品话术 RAG 注入、ragRefs 与前端参考来源展示。
- **Phase 4**：kb_import_report / kb_import_checkpoint、导入报告持久化与增量导入、ContentSecurityScanner、错误码 4064–4070 在 ErrorCode.java 中注册。

### 2.2 发现的缺口与建议

| 项 | 说明 | 建议 |
|----|------|------|
| **错误码文档** | `docs/04-错误码注册表.md` 仅列到 4063，**4064–4070（AI_DOC_* / AI_IMPORT_*）未录入** | 在 4000 段表格中追加 4064–4070，与 ErrorCode.java 保持一致（三处同步） |
| **目录导入去重** | 目录导入仍按 **existsByKbIdAndTitleAndDeleted** 判重，未用 content_fingerprint | 可选：在 processOneFile 内先调一次「内容指纹是否已存在」，再决定是否跳过，减少同内容不同文件名重复入库 |
| **metadataFilter** | hybridSearch 支持 metadataFilter 参数，**buildRagContext 未传**（传 null） | 若后续支持「仅从某目录/某 dirCategory 检索」，可在 RAG 侧构造 metadataFilter（如 dirCategory=话术） |
| **产品话术 ragRefs** | ProductScriptResultVO 无 ragRefs；多风格生成接口未返回参考来源 | 可选增强：产品话术生成结果增加 ragRefs，前端产品话术弹窗展示参考来源，与直播话术一致 |

---

## 三、性能与可扩展性

| 点 | 现状 | 优化建议 |
|----|------|----------|
| **applySemanticDedup** | 结果集每条 re-embedding 再两两余弦，结果多时耗时与调用次数上升 | 已限制在 merged 上且 threshold 默认 0 关闭；若开启且 topK 大，可考虑采样或仅对前 N 条做去重 |
| **RAG 并行** | 多查询 CompletableFuture 并行，5s 超时，chunk 级去重 | 合理；若查询数进一步增加可加「最大并发数」或批次 |
| **目录导入并发** | importParallelism 可配，单文件失败不中断 | 无问题 |
| **批量去重** | applyChunkDedup 已用 batchSearch，dedupPreview 仍逐条 search | dedupPreview 为预览、条数有限，可接受；若需可改为批量 embedding + batchSearch 统一预览 |

---

## 四、可维护性与文档

| 项 | 建议 |
|----|------|
| **配置集中** | app.ai.kb 下 dedup / rag / hybrid-dedup-threshold / security 已在 application.yml，建议在 `docs/` 或 README 中列一页「知识库与 RAG 配置说明」，便于运维与排障 |
| **错误码三处同步** | 设计 4060–4066 与实现 4064–4070 的对应关系、以及 4064–4070 在 `04-错误码注册表.md` 的补全，在升级计划「说明」中已写，建议错误码表补全并注明「文档解析/导入专用」 |
| **Phase 验收清单** | 计划中每 Phase 有验收一句话，可拆成「验收检查清单」（如：单文件 PDF 解析、目录含 docx 导入、去重预览 skip/downweight、RAG 开关与参考来源展示等），便于发布前回归 |

---

## 五、测试与质量

| 项 | 现状 | 建议 |
|----|------|------|
| **单元测试** | KnowledgeBaseServiceImplTest：dedupPreview 行为、hybridSearch(skipCache=true) 不读缓存 | 足够支撑 24/35 验收 |
| **集成/ E2E** | 未发现专门针对「上传 DOC/PDF → 知识库 → 话术生成带 RAG」的端到端测试 | 可选：加一条 E2E 或接口级流程（上传 → 列表可见 → 生成话术且响应含 ragRefs） |
| **安全扫描** | ContentSecurityScanner 仅打日志，不阻断 | 与设计一致；若需审计可考虑「命中写入 kb_import_report.errors 或单独安全日志表」 |

---

## 六、优化项汇总（按优先级）

### P1（建议尽快）

1. **错误码注册表补全**  
   在 `docs/04-错误码注册表.md` 的 4000 段中追加 4064–4070，与 `ErrorCode.java` 一致，便于前后端与支持排查。

### P2（可选增强）

2. **导入后清除知识库检索缓存**  
   上传/目录导入成功后，对该 kbId 的 Redis 检索缓存做失效（如按 key 前缀 `cache:kb:search:{kbId}:*` 删除，或维护 set 再删）；避免非 RAG 检索在 TTL 内仍返回旧结果。

3. **前端按目录分类筛选**  
   知识库文档列表已支持按 labels/keywords 筛选；从 doc.metadata 解析 **dirCategory / relativePath**，增加「按目录分类」下拉或 Tag，便于大量目录导入后的浏览。

### P3（锦上添花）

4. **产品话术 ragRefs**  
   ProductScriptResultVO 增加 ragRefs，多风格生成接口返回；前端产品话术弹窗展示「参考来源」，与直播话术一致。

5. **目录导入预检指纹**  
   processOneFile 内先根据内容指纹判断是否已存在，再决定跳过，减少同内容不同文件名的重复入库。

6. **验收检查清单**  
   将升级计划中的 Phase 验收整理为一页「发布前验收清单」，便于版本发布与回归。

---

## 七、结论

- **计划与设计**：Phase 1–5 的 53 项与设计文档、关键决策（D4/D5/D9/D11）整体对齐，仅 D10 的「导入后主动清缓存」未在计划中单列且未实现，其余为可选增强。
- **实现质量**：核心链路（解析、去重、RAG、报告、前端）完整，错误码与安全策略符合设计；建议补全错误码文档并视需要做 1–2 项 P2 优化，其余按需排期。

若需要，我可以直接改 `docs/04-错误码注册表.md` 补全 4064–4070，或按你指定的优先级实现某一项（如导入后清缓存或前端 dirCategory 筛选）。
