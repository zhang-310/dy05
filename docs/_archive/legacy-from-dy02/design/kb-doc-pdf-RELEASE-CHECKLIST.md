# 知识库 DOC/PDF 升级 — 发布前验收清单

> 依据：`kb-doc-pdf-UPGRADE-PLAN.md`、`kb-doc-pdf-UPGRADE-EVALUATION.md`  
> 用途：发布前按项勾选，确保核心能力与 P2/P3 优化已验收

---

## 一、环境与依赖

| # | 检查项 | 通过 |
|---|--------|------|
| 1 | PostgreSQL、Redis、Elasticsearch、Milvus（可选）已启动 | ☐ |
| 2 | 知识库「话术库」huashu 已存在或可由 KnowledgeBaseInitializer 自动创建 | ☐ |
| 3 | `app.ai.kb.dedup.*`、`app.ai.kb.rag.*`、`app.ai.kb.hybrid-dedup-threshold` 等配置已按需配置 | ☐ |

---

## 二、文件解析与上传

| # | 检查项 | 通过 |
|---|--------|------|
| 4 | 单文件上传：.md / .txt / .doc / .docx / .pdf 可解析并入库 | ☐ |
| 5 | 加密/扫描件 PDF 返回明确错误码（如 4064/4065），不静默失败 | ☐ |
| 6 | 上传时可选「分块策略」：自动 / 通用 / 话术，话术文档按条分块 | ☐ |
| 7 | 上传成功后，该知识库检索缓存已清除（新文档可被非 RAG 检索命中） | ☐ |

---

## 三、目录导入

| # | 检查项 | 通过 |
|---|--------|------|
| 8 | 目录导入支持 .md / .txt / .doc / .docx / .pdf，maxDepth/maxFiles 生效 | ☐ |
| 9 | 目录结构元数据（relativePath、dirCategory、dirSubCategory）写入 doc.metadata | ☐ |
| 10 | 按标题已存在则跳过；**按内容指纹已存在则跳过**（目录导入指纹预检） | ☐ |
| 11 | 导入报告持久化，导入历史可在知识库文档页「导入历史」中查看 | ☐ |
| 12 | 增量导入：按上次导入时间过滤文件，完成后更新 checkpoint | ☐ |

---

## 四、去重与预览

| # | 检查项 | 通过 |
|---|--------|------|
| 13 | 文档级：内容指纹一致时跳过入库或返回已存在文档 | ☐ |
| 14 | Chunk 级：分块后向量去重，skip/downweight 符合配置阈值 | ☐ |
| 15 | 混合检索结果语义去重（hybrid-dedup-threshold，默认 0 关闭） | ☐ |
| 16 | 去重预览接口：粘贴内容可预览 skip/downweight/keep 统计与明细 | ☐ |

---

## 五、RAG 与话术生成

| # | 检查项 | 通过 |
|---|--------|------|
| 17 | 直播开场/产品话术生成时，可勾选「参考话术库」，RAG 从 huashu 检索并注入 Prompt | ☐ |
| 18 | 生成结果返回 ragRefs，前端可展示「参考来源」（标题、相关度、内容预览） | ☐ |
| 19 | 产品话术生成（generate-product-script）结果含 ragRefs，与直播话术一致 | ☐ |
| 20 | 整场一键生成时，各槽位均继承 useKbRef 开关 | ☐ |
| 21 | RAG 检索使用 skipCache=true，新导入文档可被当次生成命中 | ☐ |

---

## 六、前端与体验

| # | 检查项 | 通过 |
|---|--------|------|
| 22 | 知识库文档列表：支持按「目录分类」(dirCategory) 筛选（有目录元数据时显示） | ☐ |
| 23 | 知识库文档列表：标签/类型筛选、排序、分块预览、批量删除正常 | ☐ |
| 24 | 去重预览对话框：粘贴内容 → 预览 → 确认导入流程可用 | ☐ |
| 25 | 直播话术构建页/场次详情话术 Tab：参考话术库开关、生成后参考来源手风琴展示 | ☐ |

---

## 七、错误码与配置

| # | 检查项 | 通过 |
|---|--------|------|
| 26 | `docs/04-错误码注册表.md` 已包含 4064–4070（AI_DOC_* / AI_IMPORT_*） | ☐ |
| 27 | ErrorCode.java 与错误码注册表、前端 error-codes 三处一致 | ☐ |

---

## 八、测试与质量

| # | 检查项 | 通过 |
|---|--------|------|
| 28 | `KnowledgeBaseServiceImplTest`：dedupPreview 高/中/低分对应 skip/downweight/keep | ☐ |
| 29 | `KnowledgeBaseServiceImplTest`：hybridSearch(skipCache=true) 不读 Redis 缓存 | ☐ |
| 30 | `mvn compile`、`mvn test` 通过；前端 `npm run type-check`、`npm run build` 通过 | ☐ |

---

**签字/日期**：________________  
**备注**：若某项不适用（如未启用 Milvus），可在「通过」栏注明「N/A」及原因。
