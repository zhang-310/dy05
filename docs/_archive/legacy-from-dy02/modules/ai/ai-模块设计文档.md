> ⚠️ **本文档已废弃** — 内容已拆分为标准结构（00-大纲.md ~ 12-升级路线图.md），请以拆分文件为准。本文件仅保留作为历史参考。

# ai 模块设计文档

> 版本：3.0 | 更新日期：2026-02-25 | 阶段：P0

---

## 一、需求分析（Requirements Analysis）

### 1.1 模块定位

ai 模块是整个平台的智能能力引擎，基于现有的本地向量知识库系统（cursor-knowledge-mcp）升级为企业级抖音知识库服务。为 shortvideo、live、script、copy 等模块提供：

1. **抖音领域知识检索** — 混合检索（Milvus 语义 + Elasticsearch 关键词 → RRF 融合 → Cross-Encoder 重排）
2. **AI 内容生成** — 文案、话术、视频方案、分析报告的生成能力
3. **知识自进化引擎** — 6 类自进化 Agent 24h 自动进化更新抖音运营知识，质量评分闭环，主题自扩展，持续积累成为最强抖音运营大脑

**与现有系统的关系：**

```
现有 cursor-knowledge-mcp（本地开发工具）
    ↓ 升级
ai 模块（企业级 SaaS 服务）
    ├── 知识库从单机 → 多租户
    ├── MCP 协议 → REST API
    ├── 单用户 → 平台级（额度管理、调用日志）
    ├── 嵌入管理 → Spring Boot 集成（BGE-M3 1024D 稠密+稀疏向量）
    ├── 向量库 ChromaDB → Milvus 2.4+（分区、HNSW 索引）
    ├── 全文检索 → Elasticsearch 8.x（BM25 + IK 中文分词）
    ├── 缓存层 → Redis 7 Cluster（热点查询/嵌入/分布式锁）
    ├── 消息队列 → RabbitMQ（异步摄入/进化/多媒体）
    └── 自进化引擎 → 6 类自进化 Agent（知识缺口/时效性/质量评分/自动分类/跨域共享/深度进化）
```

**核心理念 —— 最强抖音运营大脑：**

```
知识积累飞轮（技术支撑：RRF 混合检索 + 6 类自进化 Agent）：
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│   主题池 → 上下文收集(Milvus+ES) → LLM 生成专家报告 → 质量评分       │
│     ↑                                                     │          │
│     │         ┌───────────┐                               ↓          │
│     ├─────────┤ 主题自扩展 ├──── 高分报告 → BGE-M3嵌入 → Milvus+ES   │
│     │         └───────────┘                    ↑          │          │
│     │                                  RabbitMQ异步摄入    ↓          │
│     └──────── 低分主题深化 ←── 待深化问题提取              │          │
│                                                                      │
│   6 类 Agent：缺口检测/时效性/质量评分/自动分类/跨域共享/深度进化     │
│   每 24h 一个循环，知识库越来越强，生成质量越来越高                    │
└──────────────────────────────────────────────────────────────────────┘
```

### 1.2 用户故事（User Stories）

| 编号 | 角色 | 故事 | 验收条件 |
|------|------|------|----------|
| AI-01 | 达人/主播 | 我要能用 AI 生成视频文案 | 输入主题+人设 → AI 返回多版文案 |
| AI-02 | 达人/主播 | 我要能用 AI 生成直播话术 | 输入产品+人设+风格 → AI 返回话术 |
| AI-03 | 达人/主播 | 我要能用 AI 分析爆款视频 | 输入视频信息 → AI 返回结构分析 |
| AI-04 | 达人/主播 | 我要能用 AI 分析我的数据 | 输入数据 → AI 返回分析报告+建议 |
| AI-05 | 达人/主播 | 我要能搜索抖音运营知识 | 输入问题 → 返回相关知识条目 |
| AI-06 | 平台管理员 | 我要能管理知识库 | 查看索引状态、手动触发索引、健康检查 |
| AI-07 | 平台管理员 | 我要能管理 AI 模型配置 | 切换模型、调整参数、查看调用统计 |
| AI-08 | 平台管理员 | 我要能管理 Prompt 模板 | Prompt 模板 CRUD，按用途分类 |
| AI-09 | 系统 | AI 调用要有额度限制 | 按套餐限制每日调用次数，超限提示升级 |
| AI-10 | 系统 | AI 主模型不可用时自动降级 | 主模型超时/报错 → 自动切换备用模型 |
| AI-11 | 系统 | 知识库每 24h 自动进化 | 定时任务自动运行进化管线，生成专家报告并入库 |
| AI-12 | 系统 | 进化报告有质量把关 | 11 维度质量评分，低分报告不入库，低分主题自动排队深化 |
| AI-13 | 系统 | 进化主题自动扩展 | 每次进化后从报告中提取新主题，主题池自增长 |
| AI-14 | 平台管理员 | 我要能监控进化状态 | 查看进化任务历史、质量评分趋势、主题池状态 |
| AI-15 | 平台管理员 | 我要能管理进化主题 | 查看/编辑/新增/删除进化主题，调整优先级 |
| AI-16 | 平台管理员 | 我要能手动触发进化 | 一键触发一轮进化（测试/紧急补充知识） |

### 1.3 功能清单

```
ai 模块
├── 知识库引擎
│   ├── 向量知识库管理（Milvus 2.4+ + BGE-M3 嵌入，1024D 稠密+稀疏向量）
│   ├── Elasticsearch 全文检索引擎（BM25 + IK 中文分词）
│   ├── 混合检索（Milvus 语义 + ES 关键词 → RRF 融合 → Cross-Encoder 重排 + Redis 缓存）
│   ├── Redis 缓存层（热点查询缓存 / 嵌入缓存 / 分布式锁）
│   ├── RabbitMQ 异步摄入管道（索引/进化/多媒体队列 + 死信队列）
│   ├── 知识索引（全量/增量/状态查询）
│   └── 知识源管理（添加/删除知识源目录）
│
├── 知识自进化引擎（核心差异化 — 6 类自进化 Agent）
│   ├── 6 类自进化 Agent
│   │   ├── 知识缺口检测 Agent（识别知识盲区，自动补充）
│   │   ├── 时效性检测 Agent（检测过期知识，触发更新）
│   │   ├── 质量评分 Agent（11 维度打分，低分淘汰/深化）
│   │   ├── 自动分类 Agent（新知识自动归类到 13 个分类）
│   │   ├── 跨域共享 Agent（跨项目/子项目知识迁移）
│   │   └── 深度进化 Agent（低分主题深度研究，多轮迭代）
│   │
│   ├── 进化管线（Pipeline）
│   │   ├── 主题采样（从主题池中按分类均衡采样 N 个主题）
│   │   ├── 上下文收集（Milvus ANN + ES BM25 → RRF 融合 + 本地文件 + 可选网络搜索）
│   │   ├── Prompt 组装（基础提示 + 进化角度 + 方法论去重 + 质量提醒）
│   │   ├── LLM 生成专家报告（3 层模型回退，确保 99.5%+ 可用性）
│   │   ├── 报告结构修复（自动补全缺失章节）
│   │   ├── 质量评分（11 维度打分，0-100 分）
│   │   ├── 待深化问题提取（从报告中提取知识盲区问题）
│   │   ├── 主题自扩展（从报告中生成新搜索主题，去重后加入主题池）
│   │   └── 异步索引入库（高分报告推入索引队列，向量化后入库）
│   │
│   ├── 主题池管理
│   │   ├── 初始主题（10 个基础抖音运营主题）
│   │   ├── 主题分类（13 类：直播/AI/垂类/商业化/算法/数据/团队/合规/跨域/品牌/广告/竞争/基础）
│   │   ├── 均衡采样（按分类权重采样，避免偏科）
│   │   ├── 去重机制（相似度 ≥ 0.75 的主题自动合并）
│   │   ├── 池容量管理（最大 1000 个主题，满时轮换尾部）
│   │   └── 优先深化队列（低分主题优先重做，最多 6 个）
│   │
│   ├── 质量评分系统
│   │   ├── 结构完整性（40 分）：方法论(25) + 深化问题(20) + 迭代建议(10) - 三节齐全(+10)
│   │   ├── 方法论质量（5 分）：≥3 条方法论有额外加分
│   │   ├── 深化问题质量（5 分）：≥2 条且含「依据」说明
│   │   ├── 专家级指标（15 分）：失败案例(5) + 可执行 SOP(5) + 行业基准(5)
│   │   ├── 低分反馈（<50 分在下一轮 Prompt 中加质量提醒）
│   │   └── 极低分深化（<25 分主题加入优先深化队列）
│   │
│   ├── 11 种进化角度（每轮随机选择 1 个，促进多样性）
│   │   ├── 失败复盘 / 可执行步骤 / 数据量化 / 行业标准
│   │   ├── 跨域迁移 / 工具实操 / 防踩坑清单 / 因果链
│   │   └── 前提显式 / 反向思考 / 自我纠错
│   │
│   ├── 3 层 LLM 回退
│   │   ├── 第 1 层：Ollama DeepSeek（本地/云端，超时 360s）
│   │   ├── 第 2 层：DeepSeek 官方 API（超时 180s，需 API Key）
│   │   └── 第 3 层：Ollama 备用模型 qwen3:8b（超时 180s，兜底）
│   │
│   └── 进化调度
│       ├── 定时调度（每 24h 自动执行一轮）
│       ├── 手动触发（管理员一键触发）
│       └── 进化历史（任务记录 + 评分趋势 + 主题覆盖率）
│
├── AI 文本生成服务（内部 API，供其他模块调用）
│   ├── 文案生成（短视频文案，按人设/主题/风格）
│   ├── 话术生成（直播话术，按人设/产品/场景）
│   ├── 方案生成（视频创作方案，按爆款/热点/自主）
│   ├── 分析报告（视频数据分析/直播数据分析）
│   ├── 爆款拆解（分析视频结构/亮点/可复制点）
│   └── 违规替换建议（检测到违规词后推荐替换）
│
├── AI 多媒体生成服务（内部 API，供 shortvideo 模块调用）
│   ├── 图像生成（按分镜描述 + 人像 + 背景生成关键帧图片）
│   ├── 视频生成（根据首尾帧图片生成过渡视频片段）
│   ├── 语音合成 TTS（文本转语音，支持多种音色/语速）
│   └── 视频剪辑（多个视频片段 + 配音 → 合成最终成片）
│
├── 模型管理
│   ├── 模型配置（主模型/备用模型/嵌入模型/多媒体模型）
│   ├── 模型回退（3 层回退机制，指数退避重试）
│   └── 模型健康检查
│
├── Prompt 模板
│   ├── 系统 Prompt 模板 CRUD
│   ├── 模板分类（文案/话术/分析/方案/多媒体/进化）
│   ├── 模板变量（{persona}、{product}、{style} 等占位符）
│   └── 模板版本管理
│
├── 额度管理
│   ├── 按用户/套餐的每日调用次数限制
│   ├── 调用次数统计
│   └── 超限提醒
│
└── 管理端
    ├── 知识库状态看板（索引量/文件数/最后更新/健康状态）
    ├── 进化引擎看板（任务历史/评分趋势/主题池/深化队列）
    ├── 进化主题管理（主题池 CRUD、分类权重、优先级调整）
    ├── 模型配置管理
    ├── Prompt 模板管理
    ├── 调用日志查询（用户/类型/耗时/Token数）
    └── 调用统计报表（日/周/月维度）
```

### 1.4 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-01 | 调用额度 | 免费版 10 次/天，专业版无限，企业版无限 + API 调用 |
| BR-02 | 模型回退 | 主模型请求超时（30s）或返回错误时，自动切换备用模型 |
| BR-03 | 知识检索缓存 | 相同查询 1 小时内返回 Redis 缓存结果（TTL 3600s，key: cache:kb:query:{hash}） |
| BR-04 | 生成结果不持久化 | AI 文本生成的结果由调用方（shortvideo/live 模块）负责保存 |
| BR-05 | Prompt 模板占位符 | 支持 `{persona}`、`{product}`、`{style}`、`{topic}`、`{data}` 等变量 |
| BR-06 | 知识自进化 | 每 24h 自动运行一次进化管线，可手动触发 |
| BR-07 | 调用日志 | 每次 AI 调用记录：用户、类型、输入摘要、耗时、Token 消耗 |
| BR-08 | 生成内容自动违规检查 | AI 生成的话术/文案自动经过违规词检测（调用 script 模块） |
| BR-09 | 多媒体生成异步 | 图像/视频/TTS/剪辑生成为异步任务，通过回调或轮询获取结果 |
| BR-10 | 多媒体资产存储 | 生成的图片/视频/音频通过 storage 模块上传到云存储，返回 URL |
| BR-11 | 多媒体模型配置 | 图像/视频/TTS/剪辑各有独立的模型配置，支持切换不同提供商 |
| **进化引擎规则** | | |
| BR-12 | 进化质量门槛 | 质量总分 < 50 分的报告不入库（不推入索引队列），仅记录日志 |
| BR-13 | 低分深化 | 质量总分 < 25 分的主题自动加入优先深化队列（最多 6 个） |
| BR-14 | 质量反馈 | 上一轮评分 < 50 时，下一轮 Prompt 自动追加「质量提醒」 |
| BR-15 | 主题去重 | 新主题与现有主题相似度 ≥ 0.75 时自动去重（SequenceMatcher） |
| BR-16 | 主题池上限 | 主题池最大 1000 个，满时轮换最旧的 12 个主题 |
| BR-17 | 方法论去重 | 提取最近 3 轮报告的方法论作为黑名单，避免重复输出 |
| BR-18 | 进化角度轮换 | 每轮随机选择 1 个进化角度（11 个可选），促进输出多样性 |
| BR-19 | 3 层 LLM 回退 | 回退顺序由 ai_task_model_config 的 primary→fallback→fallback2 决定，不同任务可配置不同的回退链。超时时间优先取 ai_task_model_config.timeout_seconds，为空时取 ai_model_config.timeout_seconds |
| BR-20 | 报告结构强制 | 进化报告必须包含 3 个章节：方法论提炼、待深化问题、可迭代建议 |
| BR-21 | 上下文截断 | 进化上下文最大 32000 字符，防止 LLM 超限 |
| BR-22 | 429 退避 | Ollama 429 限流时指数退避（60s → 90s → 120s） |
| BR-23 | 额度自动初始化 | 用户当天首次 AI 调用时，若 ai_call_quota 无当日记录，自动创建（max_count 根据用户套餐从 sys_config 读取） |
| BR-24 | 跨模块人设校验 | AI 生成时若传入 persona_id，需校验该人设属于当前用户（owner_id 匹配） |
| **企业级基础设施规则** | | |
| BR-25 | RRF 融合权重 | 双路检索融合参数：BM25 权重 1.0，Vector 权重 0.7，k=60。公式：RRF(d) = Σ 1/(k + rank_i(d)) × weight_i |
| BR-26 | Cross-Encoder 重排 | BGE-reranker-v2 对 RRF 融合后的 Top-20 结果重排，返回最终 Top-K |
| BR-27 | RabbitMQ 死信队列 | 消息消费失败 3 次后进入死信队列（DLX），转人工处理。队列：ai.index.dlq / ai.evolve.dlq / ai.media.dlq |
| BR-28 | Milvus 分区策略 | 按 project 字段分区（partition key），每个项目独立分区，支持分区级检索加速 |
| BR-29 | 双写一致性 | Milvus + ES 同步写入，任一写入失败则整体回滚并重试（最多 3 次），确保双写一致性 ≥ 99% |

### 1.5 模块依赖

```
依赖关系：
common ← auth ← ai
                  ↑
    config（模型参数、API Key 等配置）
    storage（知识库文件存储、AI 生成的图片/视频/音频存储）

基础设施依赖：
    Milvus 2.4+（向量数据库，HNSW 索引，按 project 分区）
    Elasticsearch 8.x（全文检索，BM25 + IK 中文分词）
    Redis 7 Cluster（热点查询缓存 / 嵌入缓存 / 分布式锁）
    RabbitMQ（异步索引摄入 / 进化任务 / 多媒体任务队列 + 死信队列）
    BGE-M3 嵌入模型（1024D 稠密 + 稀疏向量，Ollama 或独立推理服务）
    BGE-reranker-v2 重排模型（Cross-Encoder，Ollama 或独立推理服务）

ai 被依赖：
shortvideo → ai（文案生成、方案生成、爆款分析、数据分析、图像生成、视频生成、TTS、剪辑）
live → ai（话术生成、数据分析）
script → ai（违规替换建议）
```

### 1.6 技术架构

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         ai 模块 (Spring Boot)                                │
│                                                                              │
│  ┌─────────────┐   ┌──────────────┐                                          │
│  │ AiController │   │ AiAdminCtrl  │  ← REST API 层                          │
│  └──────┬──────┘   └──────┬───────┘                                          │
│         │                  │                                                  │
│  ┌──────▼──────────────────▼───────┐                                          │
│  │         AiGenerateService        │  ← 文本生成                             │
│  │  (文案/话术/方案/分析/爆款拆解)   │                                          │
│  └──────┬──────────────────────────┘                                          │
│         │                                                                     │
│  ┌──────▼──────────────────────────┐                                          │
│  │        AiMediaService            │  ← 多媒体生成                           │
│  │  (图像生成/视频生成/TTS/剪辑)    │                                          │
│  └──────┬──────────────────────────┘                                          │
│         │                                                                     │
│  ┌──────▼──────────────────────────────────────────────────────────────────┐  │
│  │                    AiEvolveService（知识自进化引擎）                       │  │
│  │                                                                          │  │
│  │  ┌──────────────────────────────────────────────────────────────────┐    │  │
│  │  │              6 类自进化 Agent 编排                                 │    │  │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐                          │    │  │
│  │  │  │知识缺口   │ │时效性检测│ │质量评分   │                          │    │  │
│  │  │  │检测 Agent │ │Agent     │ │Agent     │                          │    │  │
│  │  │  └──────────┘ └──────────┘ └──────────┘                          │    │  │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐                          │    │  │
│  │  │  │自动分类   │ │跨域共享   │ │深度进化   │                          │    │  │
│  │  │  │Agent     │ │Agent     │ │Agent     │                          │    │  │
│  │  │  └──────────┘ └──────────┘ └──────────┘                          │    │  │
│  │  └──────────────────────────────────────────────────────────────────┘    │  │
│  │                                                                          │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                │  │
│  │  │ 主题采样 │→│ 上下文   │→│ LLM 生成 │→│ 质量评分 │                │  │
│  │  │ TopicPool│  │(Milvus+ES│  │ 3层回退  │  │ 11维度   │                │  │
│  │  │          │  │ RRF融合) │  │          │  │          │                │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └────┬─────┘                │  │
│  │                                                  │                      │  │
│  │                              ┌───────────────────▼────────┐             │  │
│  │                              │ ≥50分 → RabbitMQ异步索引入库 │             │  │
│  │                              │ <50分 → 仅记录日志           │             │  │
│  │                              │ <25分 → 加入深化队列         │             │  │
│  │                              └───────────────────┬────────┘             │  │
│  │                                                  │                      │  │
│  │  ┌──────────┐  ┌──────────┐                      │                      │  │
│  │  │ 主题扩展 │←│ 深化提取 │←─────────────────────┘                      │  │
│  │  │ Expand   │  │ Deepen   │                                              │  │
│  │  └──────────┘  └──────────┘                                              │  │
│  └──────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │              AiSearchFusionService（RRF 双路混合检索）                 │    │
│  │                                                                      │    │
│  │    用户查询                                                           │    │
│  │      │                                                               │    │
│  │      ├──→ [Redis 缓存命中?] ──→ 直接返回                             │    │
│  │      │                                                               │    │
│  │      ├──→ BGE-M3 嵌入 ──→ Milvus ANN 语义检索 ──┐                   │    │
│  │      │                                            ├→ RRF 融合 → 重排  │    │
│  │      └──→ ES BM25 关键词检索 ────────────────────┘    (BGE-reranker)  │    │
│  │                                                          │            │    │
│  │                                                     Top-K 结果        │    │
│  │                                                     + 写入 Redis 缓存 │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  AiModel     │  │ AiKnowledge    │  │  AiReranker  │  │  AiCache     │   │
│  │  Service     │  │ Service        │  │  Service     │  │  Service     │   │
│  │  (模型调用)  │  │ (知识检索)     │  │  (重排序)    │  │  (Redis缓存) │   │
│  └──────┬──────┘  └───────┬───────┘  └──────┬──────┘  └──────┬──────┘   │
│         │                  │                  │                │           │
│  ┌──────▼──────┐  ┌───────▼───────┐  ┌──────▼───────┐  ┌────▼────────┐  │
│  │  外部模型   │  │  Milvus 2.4+  │  │  BGE-reranker│  │  Redis 7    │  │
│  │  DeepSeek   │  │  向量数据库   │  │  -v2         │  │  Cluster    │  │
│  │  Ollama     │  │  (HNSW索引)   │  │  Cross-      │  │  (缓存/锁)  │  │
│  │  图像/视频  │  │               │  │  Encoder     │  │             │  │
│  │  /TTS API   │  ├───────────────┤  └──────────────┘  └─────────────┘  │
│  │             │  │  Elasticsearch│                                       │
│  │  BGE-M3    │  │  8.x          │  ┌──────────────┐                     │
│  │  嵌入模型   │  │  (BM25+IK)    │  │  RabbitMQ    │                     │
│  │             │  │               │  │  异步队列    │                     │
│  │             │  │               │  │  (索引/进化   │                     │
│  │             │  │               │  │  /多媒体+DLX)│                     │
│  └─────────────┘  └───────────────┘  └──────────────┘                     │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.7 配置管理（环境变量动态化）

**所有配置通过环境变量管理，支持无需重编译即可切换模型和参数：**

#### 文本生成模型配置

```properties
# DeepSeek API 配置
DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:your-api-key}
DEEPSEEK_API_URL=${DEEPSEEK_API_URL:https://api.deepseek.com/v1}
DEEPSEEK_MODEL=${DEEPSEEK_MODEL:deepseek-chat}
DEEPSEEK_TIMEOUT=${DEEPSEEK_TIMEOUT:30}

# Ollama 本地模型配置
OLLAMA_ENABLED=${OLLAMA_ENABLED:true}
OLLAMA_BASE_URL=${OLLAMA_BASE_URL:http://localhost:11434}
OLLAMA_MODEL=${OLLAMA_MODEL:deepseek-v3.2:cloud}
OLLAMA_TIMEOUT=${OLLAMA_TIMEOUT:360}

# 备用模型
OLLAMA_FALLBACK_MODEL=${OLLAMA_FALLBACK_MODEL:qwen3:8b}
OLLAMA_FALLBACK_TIMEOUT=${OLLAMA_FALLBACK_TIMEOUT:180}
```

#### 嵌入模型配置

```properties
# BGE-M3 嵌入模型（支持动态切换维度）
EMBEDDING_MODEL=${EMBEDDING_MODEL:bge-m3:latest}
EMBEDDING_DIM=${EMBEDDING_DIM:1024}
EMBEDDING_BATCH_SIZE=${EMBEDDING_BATCH_SIZE:32}
EMBEDDING_CACHE_ENABLED=${EMBEDDING_CACHE_ENABLED:true}
EMBEDDING_CACHE_TTL=${EMBEDDING_CACHE_TTL:7200}

# 重排模型
RERANKER_MODEL=${RERANKER_MODEL:bge-reranker-v2:latest}
RERANKER_TIMEOUT=${RERANKER_TIMEOUT:60}
```

#### 向量库配置

```properties
# Milvus 配置
MILVUS_HOST=${MILVUS_HOST:localhost}
MILVUS_PORT=${MILVUS_PORT:19631}
MILVUS_COLLECTION=${MILVUS_COLLECTION:kb_knowledge}
MILVUS_INDEX_TYPE=${MILVUS_INDEX_TYPE:HNSW}
MILVUS_METRIC_TYPE=${MILVUS_METRIC_TYPE:COSINE}
MILVUS_NLIST=${MILVUS_NLIST:1024}
MILVUS_EF=${MILVUS_EF:128}
```

#### 全文检索配置

```properties
# Elasticsearch 配置
ES_URIS=${ES_URIS:http://localhost:9200}
ES_INDEX=${ES_INDEX:kb_knowledge}
ES_SHARDS=${ES_SHARDS:3}
ES_REPLICAS=${ES_REPLICAS:1}
ES_ANALYZER=${ES_ANALYZER:ik_max_word}
```

#### 缓存配置

```properties
# Redis 缓存
REDIS_HOST=${REDIS_HOST:localhost}
REDIS_PORT=${REDIS_PORT:6379}
REDIS_PASSWORD=${REDIS_PASSWORD:}
REDIS_CLUSTER_ENABLED=${REDIS_CLUSTER_ENABLED:false}

# 缓存 TTL
CACHE_QUERY_TTL=${CACHE_QUERY_TTL:3600}
CACHE_EMBED_TTL=${CACHE_EMBED_TTL:7200}
CACHE_LOCK_TTL=${CACHE_LOCK_TTL:600}
```

#### 消息队列配置

```properties
# RabbitMQ 配置
RABBITMQ_HOST=${RABBITMQ_HOST:localhost}
RABBITMQ_PORT=${RABBITMQ_PORT:5672}
RABBITMQ_USER=${RABBITMQ_USER:guest}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:guest}
RABBITMQ_VHOST=${RABBITMQ_VHOST:/}

# 队列配置
RABBITMQ_INDEX_QUEUE=${RABBITMQ_INDEX_QUEUE:ai.index}
RABBITMQ_EVOLVE_QUEUE=${RABBITMQ_EVOLVE_QUEUE:ai.evolve}
RABBITMQ_MEDIA_QUEUE=${RABBITMQ_MEDIA_QUEUE:ai.media}
```

#### 进化引擎配置

```properties
# 进化任务调度
EVOLVE_ENABLED=${EVOLVE_ENABLED:true}
EVOLVE_CRON=${EVOLVE_CRON:0 0 6 * * ?}  # 每天 06:00 执行
EVOLVE_QUERY_MAX=${EVOLVE_QUERY_MAX:10}  # 每轮采样主题数
EVOLVE_CONTEXT_MAX_CHARS=${EVOLVE_CONTEXT_MAX_CHARS:32000}
EVOLVE_ANGLE_COUNT=${EVOLVE_ANGLE_COUNT:11}

# 质量评分阈值
EVOLVE_QUALITY_THRESHOLD=${EVOLVE_QUALITY_THRESHOLD:50}
EVOLVE_DEEPEN_THRESHOLD=${EVOLVE_DEEPEN_THRESHOLD:25}
EVOLVE_DEEPEN_QUEUE_MAX=${EVOLVE_DEEPEN_QUEUE_MAX:6}

# 主题池管理
EVOLVE_TOPIC_POOL_MAX=${EVOLVE_TOPIC_POOL_MAX:1000}
EVOLVE_TOPIC_SIMILARITY_THRESHOLD=${EVOLVE_TOPIC_SIMILARITY_THRESHOLD:0.75}
EVOLVE_TOPIC_ROTATION_SIZE=${EVOLVE_TOPIC_ROTATION_SIZE:12}

# 混合检索权重
SEARCH_BM25_WEIGHT=${SEARCH_BM25_WEIGHT:1.0}
SEARCH_VECTOR_WEIGHT=${SEARCH_VECTOR_WEIGHT:0.7}
SEARCH_RRF_K=${SEARCH_RRF_K:60}
```

#### 多媒体生成配置

```properties
# 图像生成
IMAGE_GEN_ENABLED=${IMAGE_GEN_ENABLED:true}
IMAGE_GEN_URL=${IMAGE_GEN_URL:http://localhost:8080/api/image}
IMAGE_GEN_TIMEOUT=${IMAGE_GEN_TIMEOUT:120}
IMAGE_GEN_WIDTH=${IMAGE_GEN_WIDTH:1080}
IMAGE_GEN_HEIGHT=${IMAGE_GEN_HEIGHT:1920}

# 视频生成
VIDEO_GEN_ENABLED=${VIDEO_GEN_ENABLED:true}
VIDEO_GEN_URL=${VIDEO_GEN_URL:http://localhost:8080/api/video}
VIDEO_GEN_TIMEOUT=${VIDEO_GEN_TIMEOUT:300}

# TTS 语音合成
TTS_ENABLED=${TTS_ENABLED:true}
TTS_URL=${TTS_URL:http://localhost:8080/api/tts}
TTS_TIMEOUT=${TTS_TIMEOUT:60}

# 视频剪辑
VIDEO_EDIT_ENABLED=${VIDEO_EDIT_ENABLED:true}
VIDEO_EDIT_URL=${VIDEO_EDIT_URL:http://localhost:8080/api/edit}
VIDEO_EDIT_TIMEOUT=${VIDEO_EDIT_TIMEOUT:600}
```

#### 知识库配置

```properties
# 知识源扫描
KNOWLEDGE_SOURCE_PATH=${KNOWLEDGE_SOURCE_PATH:D:/docs}
KNOWLEDGE_SCAN_INTERVAL=${KNOWLEDGE_SCAN_INTERVAL:3600}
KNOWLEDGE_CHUNK_SIZE=${KNOWLEDGE_CHUNK_SIZE:512}
KNOWLEDGE_CHUNK_OVERLAP=${KNOWLEDGE_CHUNK_OVERLAP:50}
KNOWLEDGE_MAX_FILE_SIZE=${KNOWLEDGE_MAX_FILE_SIZE:104857600}  # 100MB
KNOWLEDGE_SUPPORTED_TYPES=${KNOWLEDGE_SUPPORTED_TYPES:pdf,md,txt,docx}
```

**配置切换示例：**

```bash
# 切换嵌入模型维度（从 1024 → 768）
export EMBEDDING_DIM=768
docker restart dy-app

# 切换文本生成模型（从 deepseek-v3.2:cloud → qwen3:8b）
export OLLAMA_MODEL=qwen3:8b
docker restart dy-app

# 调整进化任务频率（从每天 06:00 → 每 12 小时）
export EVOLVE_CRON="0 0 */12 * * ?"
docker restart dy-app

# 启用/禁用进化引擎
export EVOLVE_ENABLED=false
docker restart dy-app
```

---

### 1.8 进化管线详细流程

```
进化管线单轮执行流程（每 24h 一次）：

① 主题采样
   ├── 加载主题池 (ai_evolve_topic)
   ├── 加载优先深化队列（priority > 0 的主题）
   ├── 按分类均衡采样 N 个主题（N = EVOLVE_QUERY_MAX，默认 10）
   └── 输出：List<String> queries

② 上下文收集
   ├── 对每个主题做 Milvus ANN 语义检索（BGE-M3 嵌入，top_k=12）
   ├── 对每个主题做 ES BM25 关键词检索（IK 分词，top_k=12）
   ├── RRF 融合双路结果（BM25 权重 1.0，Vector 权重 0.7，k=60）
   ├── BGE-reranker-v2 Cross-Encoder 重排 Top-20 → Top-12
   ├── 每个来源最多 2 个片段（防止单源垄断）
   ├── 可选：本地文件补充（最新 25 个文件，4KB/文件）
   ├── 可选：网络搜索补充（热点话题 + DuckDuckGo）
   ├── 总上下文截断到 32000 字符
   └── 输出：String context

③ Prompt 组装
   ├── 基础提示（角色设定 + 输出格式要求）
   ├── 随机选择 1 个进化角度（11 选 1）
   ├── 方法论去重黑名单（最近 3 轮报告的方法论，前 5 条）
   ├── 上轮结论参考（方法论 + 迭代建议，≤600 字符）
   ├── 质量提醒（如果上轮 < 50 分，追加提醒文本）
   └── 输出：String prompt

④ LLM 生成
   ├── 第 1 层：Ollama DeepSeek (timeout=360s, retry=3, backoff=5s*2^n)
   ├── 第 1 层 429 限流：退避 60s → 90s → 120s
   ├── 第 1 层失败 → 第 2 层：DeepSeek API (timeout=180s)
   ├── 第 2 层 401/402 → 第 3 层：Ollama qwen3:8b (timeout=180s)
   ├── 生成内容 < 300 字符 → 重试 1 次
   ├── 缺少「## 方法论提炼」→ 重试 1 次
   └── 输出：String report

⑤ 报告结构修复
   ├── 检查 3 个必要章节：方法论提炼 / 待深化问题 / 可迭代建议
   ├── 缺失章节自动补空标题
   └── 输出：String repairedReport

⑥ 质量评分（11 维度，100 分制）
   ├── 结构分（40 分）
   │   ├── 有方法论章节：+25 分（≥3 条额外 +5）
   │   ├── 有深化问题章节：+20 分（≥2 条额外 +5）
   │   └── 有迭代建议章节：+10 分
   ├── 完整性奖励（10 分）
   │   └── 三个章节齐全：+10 分
   ├── 专家级指标（15 分）
   │   ├── 含失败案例/防踩坑：+5 分
   │   ├── 含可执行 SOP/步骤清单：+5 分
   │   └── 含行业基准/数据量化：+5 分
   └── 输出：{total: 0-100, details: {...}}

⑦ 入库决策
   ├── 总分 ≥ 50：写入文件 → 推入索引队列 → 异步向量化入库
   ├── 总分 25-49：仅记录日志，下轮加质量提醒
   └── 总分 < 25：记录日志 + 主题加入优先深化队列

⑧ 待深化问题提取
   ├── 从报告「## 待深化问题」章节提取问题列表
   └── 追加到 pending_deepen 表，供下轮主题扩展使用

⑨ 主题自扩展
   ├── 加载待深化问题（P0 优先）
   ├── LLM 从报告中提取 3-5 个新搜索主题
   ├── 去重（相似度 ≥ 0.75 跳过）
   ├── 合并到主题池（≤1000 上限，超出轮换尾部 12 个）
   └── 更新 ai_evolve_topic 表

⑩ 异步索引
   ├── 高分报告推入 RabbitMQ 索引队列（exchange: ai.index, routing_key: ai.index.report）
   ├── RabbitMQ Consumer 消费消息
   ├── 文本分块（800 字符/块，120 字符重叠）
   ├── BGE-M3 嵌入（1024D 稠密向量 + 稀疏向量）
   ├── Milvus + ES 双写（向量写入 Milvus，全文写入 ES，双写一致性保障）
   ├── 写入 Redis 嵌入缓存（cache:kb:embed:{hash}，TTL 7200s）
   ├── 失败消息重试 3 次后进入死信队列（ai.index.dlq）
   └── 502 错误重试（90s 退避 + 1 次重试）

⑪ 6 类自进化 Agent（贯穿进化管线各阶段）
   ├── 知识缺口检测 Agent：分析报告中「待深化问题」，识别知识盲区
   │   └── 输出：新主题建议 + 知识缺口报告
   ├── 时效性检测 Agent：检测 Milvus 中超过 90 天未更新的知识条目
   │   └── 输出：过期知识列表 → 触发重新进化
   ├── 质量评分 Agent：11 维度打分（步骤 ⑥），低分淘汰/深化
   │   └── 输出：质量分数 + 评分详情
   ├── 自动分类 Agent：对新生成的知识自动归类到 13 个分类
   │   └── 输出：分类标签 + 置信度
   ├── 跨域共享 Agent：识别可跨项目/子项目复用的通用知识
   │   └── 输出：共享知识候选列表
   └── 深度进化 Agent：对低分主题进行多轮深度研究（最多 3 轮迭代）
       └── 输出：深化报告（质量目标 ≥ 60 分）
```

---

## 二、数据库设计（Database Design）

### 2.1 ER 关系

```
ai_prompt_template (Prompt 模板)
ai_model_config (模型注册表 — 纯模型定义)
ai_task_model_config (任务-模型映射 — 每个 AI 任务绑定主/备模型)
    └── ai_model_config (主模型/备用模型1/备用模型2)
ai_call_log (调用日志)
ai_call_quota (调用额度)
ai_knowledge_source (知识源)
kb_document (知识文档元数据)
kb_feedback (知识反馈)
ai_evolve_topic (进化主题池)
ai_evolve_task (进化任务)
ai_evolve_report (进化报告) ── ai_evolve_task (一对一)
ai_index_queue (索引队列 — 过渡表，目标迁移到 RabbitMQ) ── ai_evolve_report (一对一，可选)
```

> 注意：知识库向量数据存储在 Milvus 2.4+（独立向量数据库），全文索引存储在 Elasticsearch 8.x，热点缓存存储在 Redis 7 Cluster，均不在 PostgreSQL 中。异步消息通过 RabbitMQ 传递。

### 2.2 表结构

#### ai_prompt_template — Prompt 模板表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| template_code | VARCHAR(128) | NOT NULL | — | 模板编码（唯一标识）如 `video_copywriting`、`live_script_opening` |
| template_name | VARCHAR(128) | NOT NULL | — | 模板名称 |
| category | VARCHAR(64) | NOT NULL | — | 分类：copywriting / script / analysis / plan / rewrite |
| system_prompt | TEXT | | — | System Prompt 内容 |
| user_prompt | TEXT | NOT NULL | — | User Prompt 模板（含占位符） |
| variables | VARCHAR(512) | | — | 支持的变量列表（JSON 数组，如 ["persona","topic","style"]） |
| model_code | VARCHAR(64) | | — | 指定使用的模型编码（为空则使用默认模型） |
| max_tokens | INTEGER | | 2000 | 最大生成 Token 数 |
| temperature | DECIMAL(3,2) | | 0.70 | 生成温度 |
| version | INTEGER | NOT NULL | 1 | 模板版本号 |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(template_code, version) WHERE deleted=0`

#### ai_model_config — 模型注册表（纯模型定义，不含任务绑定）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| model_code | VARCHAR(64) | NOT NULL | — | 模型编码：deepseek_v3、ollama_deepseek、ollama_qwen3、bge_m3_embed、bge_reranker、sd_xl、video_gen_v1、tts_v1、edit_v1 |
| model_name | VARCHAR(128) | NOT NULL | — | 模型显示名称 |
| model_type | VARCHAR(32) | NOT NULL | — | 类型：generate（文本生成）/ embed（嵌入）/ rerank（重排序）/ image（图像生成）/ video（视频生成）/ tts（语音合成）/ edit（视频剪辑） |
| provider | VARCHAR(32) | NOT NULL | — | 提供商：deepseek / ollama / openai / custom |
| api_url | VARCHAR(512) | NOT NULL | — | API 地址 |
| api_key | VARCHAR(512) | | — | API Key（加密存储） |
| model_id | VARCHAR(128) | | — | 模型 ID（如 deepseek-chat、deepseek-v3.2:cloud、qwen3:8b） |
| timeout_seconds | INTEGER | | 30 | 请求超时秒数 |
| max_tokens | INTEGER | | 4000 | 默认最大 Token |
| extra_params | TEXT | | — | 额外参数（JSON，如 {"temperature": 0.7, "top_p": 0.9}） |
| description | VARCHAR(256) | | — | 模型说明 |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(model_code) WHERE deleted=0`、`(model_type, status) WHERE deleted=0`

#### ai_task_model_config — 任务-模型映射表（每个 AI 任务绑定主/备模型）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| task_code | VARCHAR(64) | NOT NULL | — | 任务编码（见下方任务清单） |
| task_name | VARCHAR(128) | NOT NULL | — | 任务名称（中文） |
| task_group | VARCHAR(32) | NOT NULL | — | 任务分组：text_generate / embed / rerank / media / evolve |
| primary_model_id | BIGINT | | — | 主模型 ID（关联 ai_model_config.id） |
| fallback_model_id | BIGINT | | — | 备用模型 1 ID |
| fallback2_model_id | BIGINT | | — | 备用模型 2 ID（兜底） |
| timeout_seconds | INTEGER | | — | 任务级超时（秒），为空时使用模型默认超时。进化任务建议 360s，多媒体任务建议 600s |
| max_retries | INTEGER | | 1 | 每层模型最大重试次数（指数退避，默认 1 次） |
| description | VARCHAR(256) | | — | 任务说明 |
| sort_order | INTEGER | | 0 | 排序 |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(task_code) WHERE deleted=0`、`(task_group, sort_order) WHERE deleted=0`

**AI 任务清单：**

| 任务编码 | 任务名称 | 分组 | 说明 |
|----------|----------|------|------|
| copywriting | 短视频文案 | text_generate | 生成短视频文案/标题 |
| script_opening | 开场话术 | text_generate | 直播开场话术生成 |
| script_product | 产品话术 | text_generate | 产品讲解话术生成 |
| script_closing | 结尾话术 | text_generate | 直播结尾话术生成 |
| video_plan | 视频策划 | text_generate | 爆款复刻/选题策划方案 |
| video_analysis | 视频分析 | text_generate | 视频数据分析报告 |
| live_analysis | 直播分析 | text_generate | 直播数据分析报告 |
| violation_replace | 违规替换 | text_generate | 违规词 AI 替换建议 |
| knowledge_embed | 知识嵌入 | embed | 知识库文本向量嵌入（BGE-M3 1024D） |
| knowledge_rerank | 知识重排序 | rerank | 混合检索结果 Cross-Encoder 重排序 |
| knowledge_evolve | 知识进化 | evolve | 进化报告 LLM 生成（主） |
| evolve_expand | 主题扩展 | evolve | 进化主题自动扩展 |
| image_gen | 图像生成 | media | 分镜关键帧图像生成 |
| video_gen | 视频生成 | media | 首尾帧→视频片段生成 |
| tts_gen | 语音合成 | media | 话术文本 TTS 配音 |
| video_edit | 视频剪辑 | media | 片段+配音合成成片 |
| shooting_script | 拍摄脚本生成 | text_generate | 每日详细拍摄脚本生成（1-3条，含运镜/景别/演员指导） |

#### ai_call_log — AI 调用日志表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| user_id | BIGINT | NOT NULL | — | 调用用户 ID |
| call_type | VARCHAR(64) | NOT NULL | — | 调用类型：copywriting / script / analysis / plan / knowledge_query |
| template_code | VARCHAR(128) | | — | 使用的 Prompt 模板编码 |
| model_code | VARCHAR(64) | | — | 实际使用的模型编码 |
| input_summary | VARCHAR(512) | | — | 输入摘要（截取前 500 字） |
| output_length | INTEGER | | — | 输出字符数 |
| prompt_tokens | INTEGER | | — | Prompt Token 数 |
| completion_tokens | INTEGER | | — | 生成 Token 数 |
| total_tokens | INTEGER | | — | 总 Token 数 |
| duration_ms | BIGINT | | — | 耗时（毫秒） |
| status | INTEGER | NOT NULL | 1 | 1=成功 0=失败 |
| error_message | VARCHAR(512) | | — | 失败原因 |
| is_fallback | INTEGER | NOT NULL | 0 | 是否走了备用模型 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(user_id, create_time DESC)`、`(call_type, create_time DESC)`、`(user_id, call_type, create_time DESC)`（额度查询优化）、`(model_code, status, create_time DESC)`（模型调用统计）

#### ai_call_quota — AI 调用额度表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| user_id | BIGINT | NOT NULL | — | 用户 ID |
| quota_date | DATE | NOT NULL | — | 日期 |
| used_count | INTEGER | NOT NULL | 0 | 已用次数 |
| max_count | INTEGER | NOT NULL | 10 | 最大次数（根据套餐） |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(user_id, quota_date)`

#### ai_knowledge_source — 知识源管理表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| source_name | VARCHAR(128) | NOT NULL | — | 知识源名称 |
| source_path | VARCHAR(512) | NOT NULL | — | 知识源路径（文件目录） |
| source_type | VARCHAR(32) | NOT NULL | 'local' | 类型：local（本地）/ remote（远程） |
| file_count | INTEGER | | 0 | 文件数量 |
| index_count | INTEGER | | 0 | 已索引数量 |
| last_index_time | TIMESTAMP | | — | 最后索引时间 |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

#### ai_evolve_topic — 进化主题池表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| topic | VARCHAR(256) | NOT NULL | — | 主题短语 |
| category | VARCHAR(32) | | 'basic' | 分类：live / ai / vertical / commercial / algorithm / data / team / compliance / cross_domain / brand / ad / competition / basic |
| priority | INTEGER | NOT NULL | 100 | 优先级（数值越小越优先，0=最高优先） |
| source | VARCHAR(16) | NOT NULL | 'initial' | 来源：initial（初始）/ expanded（自动扩展）/ deepened（低分深化）/ manual（手动添加） |
| used_count | INTEGER | | 0 | 已使用次数 |
| last_used_time | TIMESTAMP | | — | 最后使用时间 |
| score_avg | DECIMAL(5,2) | | — | 该主题产出报告的平均得分 |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(category, priority, status) WHERE deleted=0`、`(source, create_time DESC) WHERE deleted=0`

#### ai_evolve_task — 进化任务表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| task_no | VARCHAR(64) | NOT NULL | — | 任务编号（如 evolve_20260224_120530） |
| topic_ids | VARCHAR(512) | | — | 本轮采样的主题 ID（JSON 数组） |
| topic_texts | TEXT | | — | 本轮采样的主题文本（JSON 数组，便于展示） |
| evolve_angle | VARCHAR(32) | | — | 本轮进化角度（11 选 1） |
| gather_mode | VARCHAR(16) | | 'hybrid' | 上下文收集方式：hybrid（Milvus+ES 混合）/ milvus / es / file / mixed |
| context_length | INTEGER | | — | 上下文长度（字符数） |
| model_used | VARCHAR(64) | | — | 实际使用的模型编码 |
| fallback_tier | INTEGER | | 1 | 回退层级：1=主模型 2=DeepSeek API 3=备用模型 |
| prompt_tokens | INTEGER | | — | Prompt Token 数 |
| completion_tokens | INTEGER | | — | 生成 Token 数 |
| total_tokens | INTEGER | | — | 总 Token 数 |
| duration_ms | BIGINT | | — | 总耗时（毫秒） |
| score_total | INTEGER | | — | 质量总分（0-100） |
| score_detail | TEXT | | — | 质量评分详情（JSON） |
| expanded_count | INTEGER | | 0 | 本轮扩展的新主题数 |
| deepened_count | INTEGER | | 0 | 本轮深化排队的主题数 |
| had_quality_hint | INTEGER | NOT NULL | 0 | 是否追加了质量提醒 |
| status | VARCHAR(16) | NOT NULL | 'pending' | 状态：pending / gathering / generating / scoring / expanding / indexing / completed / failed |
| error_message | VARCHAR(512) | | — | 失败原因 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(task_no)`、`(status, create_time DESC)`、`(score_total, create_time DESC)`

#### ai_evolve_report — 进化报告表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| task_id | BIGINT | NOT NULL | — | 关联进化任务 ID |
| report_title | VARCHAR(256) | | — | 报告标题（自动生成） |
| methodology_section | TEXT | | — | 方法论提炼内容 |
| deepen_section | TEXT | | — | 待深化问题内容 |
| iterate_section | TEXT | | — | 可迭代建议内容 |
| full_content | TEXT | NOT NULL | — | 完整报告内容（Markdown） |
| methodology_count | INTEGER | | 0 | 方法论条目数 |
| deepen_count | INTEGER | | 0 | 深化问题数 |
| has_failure_case | INTEGER | NOT NULL | 0 | 是否含失败案例 |
| has_sop | INTEGER | NOT NULL | 0 | 是否含可执行 SOP |
| has_benchmark | INTEGER | NOT NULL | 0 | 是否含行业基准数据 |
| index_status | VARCHAR(16) | NOT NULL | 'pending' | 索引状态：pending / queued / indexing / indexed / skipped / failed |
| indexed_time | TIMESTAMP | | — | 索引完成时间 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(task_id)`、`(index_status, create_time DESC)`

#### ai_index_queue — 索引队列表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| source_type | VARCHAR(32) | NOT NULL | — | 来源类型：evolved（进化报告）/ knowledge（知识文件）/ manual（手动上传） |
| source_id | BIGINT | | — | 来源 ID（如 ai_evolve_report.id） |
| content | TEXT | NOT NULL | — | 待索引内容 |
| priority | INTEGER | NOT NULL | 1 | 优先级（数值越小越优先） |
| status | VARCHAR(16) | NOT NULL | 'pending' | 状态：pending / processing / done / failed |
| error_message | VARCHAR(512) | | — | 失败原因 |
| retry_count | INTEGER | NOT NULL | 0 | 重试次数 |
| processing_since | TIMESTAMP | | — | 开始处理时间（超过 10 分钟视为超时） |
| done_time | TIMESTAMP | | — | 完成时间 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(status, priority, create_time) WHERE status='pending'`

**索引队列重试策略：**
- 最大重试次数：3 次（retry_count ≥ 3 时标记为 failed，人工处理）
- 重试间隔：指数退避（30s → 60s → 120s）
- 超时检测：processing_since 超过 10 分钟的记录，重置为 pending 并 retry_count+1
- 消费线程：indexTaskExecutor 每 30s 轮询一次 pending 队列（过渡方案，目标迁移到 RabbitMQ Consumer）

#### kb_document — 知识文档元数据表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| doc_id | VARCHAR(128) | NOT NULL | — | 文档唯一标识（UUID，关联 Milvus/ES） |
| project | VARCHAR(64) | NOT NULL | 'default' | 所属项目（Milvus 分区键） |
| sub_project | VARCHAR(64) | | — | 子项目 |
| doc_type | VARCHAR(32) | NOT NULL | 'article' | 文档类型：article / evolved / manual / faq |
| title | VARCHAR(256) | | — | 文档标题 |
| source_path | VARCHAR(512) | | — | 来源路径 |
| chunk_count | INTEGER | | 0 | 分块数量 |
| quality_score | DECIMAL(5,2) | | — | 质量评分（0-100） |
| boost_factor | DECIMAL(3,2) | | 1.00 | 检索提权因子（默认 1.0，高质量文档可设为 1.2-1.5） |
| milvus_synced | INTEGER | NOT NULL | 0 | Milvus 同步状态：0=未同步 1=已同步 |
| es_synced | INTEGER | NOT NULL | 0 | ES 同步状态：0=未同步 1=已同步 |
| last_sync_time | TIMESTAMP | | — | 最后同步时间 |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(doc_id) WHERE deleted=0`、`(project, doc_type, status) WHERE deleted=0`、`(quality_score DESC) WHERE deleted=0`

#### kb_feedback — 知识反馈表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| query | VARCHAR(512) | NOT NULL | — | 用户查询内容 |
| doc_id | VARCHAR(128) | NOT NULL | — | 反馈的文档 ID（关联 kb_document.doc_id） |
| rating | INTEGER | NOT NULL | — | 评分：1=有用 -1=无用 0=不确定 |
| comment | VARCHAR(512) | | — | 用户评论 |
| user_id | BIGINT | NOT NULL | — | 反馈用户 ID |
| search_mode | VARCHAR(16) | | — | 检索模式：hybrid / semantic / keyword |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(doc_id, create_time DESC)`、`(user_id, create_time DESC)`、`(query, create_time DESC)`

```sql
-- ==============================
-- 模型注册表（纯模型定义）
-- ==============================
INSERT INTO ai_model_config (model_code, model_name, model_type, provider, api_url, model_id, timeout_seconds, max_tokens, description) VALUES
-- 文本生成模型
('deepseek_v3',       'DeepSeek V3（API）',   'generate', 'deepseek', 'https://api.deepseek.com/v1',  'deepseek-chat',           30,  4000, '官方 DeepSeek API，速度快，成本低'),
('ollama_deepseek',   'Ollama DeepSeek 本地', 'generate', 'ollama',   'http://localhost:11434',       'deepseek-v3.2:cloud',     360, 4096, 'Ollama 本地部署 DeepSeek，无需 API Key'),
('ollama_qwen3',      'Ollama Qwen3 本地',    'generate', 'ollama',   'http://localhost:11434',       'qwen3:8b',                180, 4096, 'Ollama 本地 Qwen3 兜底模型'),
-- 嵌入模型
('bge_m3_embed',      'BGE-M3 嵌入',          'embed',    'ollama',   'http://localhost:11434',       'bge-m3:latest',           120, 0,    'BGE-M3 1024D 稠密+稀疏向量嵌入'),
-- 重排序模型
('bge_reranker',      'BGE-reranker-v2',      'rerank',   'ollama',   'http://localhost:11434',       'bge-reranker-v2:latest',  60,  0,    'BGE-reranker-v2 Cross-Encoder 重排序'),
-- 多媒体模型
('image_gen_sdxl',    'SDXL 图像生成',        'image',    'custom',   'http://localhost:8080/api/image', 'sd-xl',                120, 0,    'Stable Diffusion XL 图像生成'),
('video_gen_v1',      '视频生成 V1',          'video',    'custom',   'http://localhost:8080/api/video', 'video-gen-v1',          300, 0,    '首尾帧→视频片段'),
('tts_cosyvoice',     'CosyVoice TTS',        'tts',      'custom',   'http://localhost:8080/api/tts',   'cosyvoice-v1',          60,  0,    '中文语音合成，支持多音色'),
('video_edit_v1',     '视频剪辑 V1',          'edit',     'custom',   'http://localhost:8080/api/edit',   'edit-v1',               600, 0,    '视频片段+配音→合成成片');

-- ==============================
-- 任务-模型映射（每个 AI 任务绑定主/备模型）
-- ==============================
-- primary_model_id / fallback_model_id / fallback2_model_id 引用 ai_model_config.id
-- 以下用 (SELECT id FROM ...) 来关联

INSERT INTO ai_task_model_config (task_code, task_name, task_group, primary_model_id, fallback_model_id, fallback2_model_id, description, sort_order) VALUES
-- 文本生成任务
('copywriting',       '短视频文案生成',  'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_qwen3'),
  '生成短视频文案/标题/话题标签', 1),
('script_opening',    '直播开场话术',    'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_qwen3'),
  '直播开场话术生成', 2),
('script_product',    '产品讲解话术',    'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_qwen3'),
  '产品讲解话术生成', 3),
('script_closing',    '直播结尾话术',    'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_qwen3'),
  '直播结尾话术生成', 4),
('video_plan',        '视频策划方案',    'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  NULL,
  '爆款复刻/选题策划方案', 5),
('video_analysis',    '视频数据分析',    'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  NULL,
  '视频表现分析报告', 6),
('live_analysis',     '直播数据分析',    'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  NULL,
  '直播复盘分析报告', 7),
('violation_replace', '违规词替换建议',  'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_qwen3'),
  NULL,
  '违规词 AI 替换建议', 8),
-- 嵌入任务
('knowledge_embed',   '知识库嵌入',      'embed',
  (SELECT id FROM ai_model_config WHERE model_code='bge_m3_embed'),
  NULL, NULL,
  '知识文本向量嵌入（BGE-M3 1024D 稠密+稀疏）', 1),
-- 重排序任务
('knowledge_rerank',  '知识库重排序',    'rerank',
  (SELECT id FROM ai_model_config WHERE model_code='bge_reranker'),
  NULL, NULL,
  '混合检索结果 Cross-Encoder 重排序（BGE-reranker-v2）', 2),
-- 进化任务
('knowledge_evolve',  '知识进化报告',    'evolve',
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_qwen3'),
  '知识自进化 LLM 生成专家报告（3层回退）', 1),
('evolve_expand',     '进化主题扩展',    'evolve',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_qwen3'),
  NULL,
  '从报告中提取新搜索主题', 2),
-- 多媒体任务
('image_gen',         '分镜图像生成',    'media',
  (SELECT id FROM ai_model_config WHERE model_code='image_gen_sdxl'),
  NULL, NULL,
  '按分镜描述+人像+背景生成关键帧', 1),
('video_gen',         '视频片段生成',    'media',
  (SELECT id FROM ai_model_config WHERE model_code='video_gen_v1'),
  NULL, NULL,
  '首尾帧→过渡视频片段', 2),
('tts_gen',           'TTS 语音合成',    'media',
  (SELECT id FROM ai_model_config WHERE model_code='tts_cosyvoice'),
  NULL, NULL,
  '话术文本→语音配音', 3),
('video_edit',        '视频剪辑合成',    'media',
  (SELECT id FROM ai_model_config WHERE model_code='video_edit_v1'),
  NULL, NULL,
  '片段+配音→最终成片', 4);
-- 拍摄脚本任务（timeout 设 60s 因为生成 1-3 条完整脚本的 token 量较大）
INSERT INTO ai_task_model_config (
  task_code, task_name, task_group,
  primary_model_id, fallback_model_id, fallback2_model_id,
  timeout_seconds, description, sort_order
) VALUES (
  'shooting_script', '拍摄脚本生成', 'text_generate',
  (SELECT id FROM ai_model_config WHERE model_code='deepseek_v3'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_deepseek'),
  (SELECT id FROM ai_model_config WHERE model_code='ollama_qwen3'),
  60, '每日详细拍摄脚本生成（1-3条，含运镜/景别/演员指导）', 9
);

-- 默认 Prompt 模板
INSERT INTO ai_prompt_template (template_code, template_name, category, system_prompt, user_prompt, variables, max_tokens, temperature) VALUES
('video_copywriting', '短视频文案生成', 'copywriting',
 '你是一个专业的抖音短视频文案创作者。根据用户的人设定位和创作需求，生成吸引人的短视频文案。',
 '人设信息：\n{persona}\n\n创作主题：{topic}\n\n风格要求：{style}\n\n时长：{duration}\n\n请生成3个版本的短视频文案，每个版本包含：\n1. 标题（20字以内，带话题标签）\n2. 开头（吸引注意力的前3秒文案）\n3. 正文内容\n4. 结尾引导（点赞/关注/评论）',
 '["persona","topic","style","duration"]', 3000, 0.80),

('live_script_opening', '直播开场话术', 'script',
 '你是一个专业的直播话术编写者。根据主播的人设和直播主题，生成自然、有感染力的开场话术。',
 '主播人设：\n{persona}\n\n直播主题：{topic}\n\n风格：{style}\n\n请生成一段2-3分钟的直播开场话术，要求：\n1. 开头打招呼，营造亲切氛围\n2. 介绍今天的直播内容\n3. 引导互动（点关注/扣1）\n4. 过渡到第一个产品/话题',
 '["persona","topic","style"]', 2000, 0.75),

('live_script_product', '产品讲解话术', 'script',
 '你是一个专业的直播带货话术编写者。根据产品信息和主播人设，生成有说服力的产品讲解话术。',
 '主播人设：\n{persona}\n\n产品信息：\n{product}\n\n风格：{style}\n\n请生成一段3-5分钟的产品讲解话术，包含：\n1. 产品引入（痛点引起共鸣）\n2. 产品介绍（卖点+使用场景）\n3. 价格锚定（原价 vs 直播价）\n4. 限时限量（紧迫感）\n5. 下单引导',
 '["persona","product","style"]', 3000, 0.75),

('live_script_closing', '直播结尾话术', 'script',
 '你是一个专业的直播话术编写者。根据主播人设生成温暖的结尾话术。',
 '主播人设：\n{persona}\n\n今日直播概要：{summary}\n\n风格：{style}\n\n请生成一段1-2分钟的直播结尾话术：\n1. 感谢观看和互动\n2. 总结今天的重点\n3. 预告下次直播\n4. 引导关注/加粉丝团',
 '["persona","summary","style"]', 1500, 0.75),

('video_analysis', '视频数据分析', 'analysis',
 '你是一个专业的抖音数据分析师。根据视频的播放数据，给出专业的分析和改进建议。',
 '视频信息：\n{video_info}\n\n数据指标：\n{data}\n\n同类视频平均表现：\n{benchmark}\n\n请分析：\n1. 整体表现评级（S/A/B/C/D）\n2. 数据亮点（哪些指标好）\n3. 改进空间（哪些指标差）\n4. 具体改进建议（标题/封面/内容/发布时间）\n5. 下一条视频建议',
 '["video_info","data","benchmark"]', 2000, 0.60),

('live_analysis', '直播数据分析', 'analysis',
 '你是一个专业的抖音直播数据分析师。根据直播场次数据给出复盘分析和改进建议。',
 '直播信息：\n{live_info}\n\n数据指标：\n{data}\n\n历史平均表现：\n{benchmark}\n\n请分析：\n1. 整体表现评级\n2. 流量分析（观看人数、停留时长、峰值时段）\n3. 互动分析（评论、点赞、分享）\n4. 转化分析（GMV、转化率、客单价）\n5. 分产品表现\n6. 改进建议',
 '["live_info","data","benchmark"]', 3000, 0.60),

('video_plan_viral', '爆款复刻方案', 'plan',
 '你是一个专业的短视频策划师。分析爆款视频的成功要素，结合用户人设生成复刻方案。',
 '爆款视频信息：\n{viral_video}\n\n用户人设：\n{persona}\n\n请生成复刻方案：\n1. 爆款分析（成功因素拆解）\n2. 改编思路（如何结合用户人设）\n3. 视频结构（分镜/时间线）\n4. 文案脚本\n5. 拍摄要点\n6. 标题和话题标签建议',
 '["viral_video","persona"]', 4000, 0.80),

('video_frame_generate', '分镜关键帧生成', 'media',
 '你是一个专业的视频画面描述专家。根据分镜信息生成适合 AI 图像生成的精确画面描述。',
 '分镜信息：\n场景描述：{scene_desc}\n景别：{shot_type}\n人物特征：{portrait_desc}\n背景环境：{background_desc}\n\n请生成：\n1. 首帧画面描述（英文，适合 Stable Diffusion 生成）\n2. 尾帧画面描述（英文，与首帧有动态过渡关系）',
 '["scene_desc","shot_type","portrait_desc","background_desc"]', 1000, 0.70),

('video_tts_style', '配音风格指导', 'media',
 '你是一个专业的配音指导。根据分镜话术和人设风格，给出 TTS 参数建议。',
 '人设风格：{persona_style}\n话术内容：{script_text}\n\n请建议：\n1. 推荐音色\n2. 推荐语速（0.5-2.0）\n3. 情感基调\n4. 重音标注',
 '["persona_style","script_text"]', 500, 0.60),

('evolve_report', '知识进化报告生成', 'evolve',
 '你是一位抖音行业运营专家，同时具备方法论提炼、失败复盘和可执行方案设计能力。',
 '以下是本轮进化的上下文资料：\n\n{context}\n\n进化角度要求：{evolve_angle}\n\n{quality_hint}\n{methodology_blacklist}\n{last_round_ref}\n\n请严格按以下 3 个章节输出：\n\n## 方法论提炼\n- 至少 3 条，推荐 5 条\n- 格式：1 句话概念 + 1 句话实操\n- 必须包含「谁在什么场景下做什么」\n- 用具体数据：「前3秒」「转化率>X%」\n- 含可执行 SOP（步骤 1-2-3 或检查清单）\n- 如有失败案例，用 [防踩坑] 标签\n\n## 待深化问题\n- 至少 3 个触及知识盲区的问题\n- 每个问题必须包含「依据：xxx」说明为何需要深化\n- 避免与已有问题重复\n\n## 可迭代建议\n- 至少 2 条可操作建议\n- 优先含指标/阈值/A/B测试\n- 关联行业基准',
 '["context","evolve_angle","quality_hint","methodology_blacklist","last_round_ref"]', 4096, 0.75),

('evolve_expand', '进化主题扩展', 'evolve',
 '你是一个抖音运营知识体系架构师。根据最新进化报告，提取 3-5 个新的搜索主题短语。',
 '最新进化报告：\n{report}\n\n{pending_deepen}\n\n领域提示：抖音运营相关（直播/短视频/电商/数据分析/AI工具/平台规则/垂类策略/商业化）\n\n请输出 3-5 个新搜索主题短语，每行一条，每条 10 字以内，格式如：\n抖音 直播 话术 优化\n短视频 黄金3秒 留存率\n...\n\n要求：\n1. 如有 P0 待深化问题，必须优先转化\n2. 避免与已有主题重复\n3. 覆盖不同分类维度',
 '["report","pending_deepen"]', 500, 0.80),

('shooting_script_daily', '每日拍摄脚本生成', 'plan',
 '你是一个资深的抖音短视频拍摄导演，精通各种运镜技法和拍摄手法。你需要根据人设定位生成详细的拍摄脚本，脚本必须足够详细，让摄影师拿到后可以直接执行拍摄，只需微调即可。',
 '人设信息：\n{persona}\n\n行业领域：{industry}\n创作风格：{style}\n目标时长：{duration}\n需要生成的脚本数量：{count}\n\n{knowledge_context}\n\n请为该人设生成 {count} 条完整的短视频拍摄脚本，以 JSON 数组格式输出。\n\n每条脚本的结构：\n{\n  \"title\": \"视频标题（20字以内）\",\n  \"tags\": [\"话题标签1\", \"话题标签2\"],\n  \"concept\": \"选题角度和核心卖点（1-2句话）\",\n  \"target_duration\": \"目标时长如30秒\",\n  \"props_summary\": \"整条视频所需道具汇总\",\n  \"wardrobe_summary\": \"服装造型总体要求\",\n  \"location_summary\": \"拍摄场地总体要求\",\n  \"bgm_suggestion\": \"背景音乐风格/曲名建议\",\n  \"scenes\": [\n    {\n      \"scene_no\": 1,\n      \"title\": \"分镜标题如开头吸引\",\n      \"time_range\": \"0-3秒\",\n      \"duration\": 3,\n      \"shot_type\": \"景别：close-up/medium/wide/full\",\n      \"camera_movement\": \"运镜方式（推/拉/摇/移/跟/升/降/固定/手持）+ 具体说明\",\n      \"camera_angle\": \"拍摄角度（平拍/俯拍/仰拍/过肩）\",\n      \"composition\": \"构图说明\",\n      \"scene_desc\": \"详细画面描述：出镜人的表情、动作、手势、走位\",\n      \"script_text\": \"出镜人要说的完整台词（口语化，贴合人设风格）\",\n      \"actor_direction\": \"导演对出镜人的指导：情绪、语速、眼神、肢体语言\",\n      \"props\": \"该分镜需要的具体道具\",\n      \"location\": \"拍摄场景/环境描述\",\n      \"lighting_note\": \"灯光要求\",\n      \"transition\": \"到下一镜的转场方式\",\n      \"shooting_tip\": \"摄影师拍摄提示（对焦点、稳定器设置、特殊技巧等）\"\n    }\n  ]\n}\n\n要求：\n1. 每条脚本 3-8 个分镜，时间段连续衔接\n2. 台词必须口语化、贴合人设风格，不要书面语\n3. 运镜方式要具体（不只写\"推\"，要写\"从产品慢推到脸部表情\"）\n4. 开头3秒必须有强吸引力（悬念/冲突/痛点/惊讶）\n5. 每条脚本的选题角度要有差异化，不要雷同\n6. 考虑拍摄可执行性，不要设计过于复杂的运镜或场景',
 '["persona","industry","style","duration","count","knowledge_context"]', 6000, 0.85);

-- 默认进化主题池
INSERT INTO ai_evolve_topic (topic, category, priority, source) VALUES
('抖音运营 直播 话术 粉丝互动', 'live', 100, 'initial'),
('短视频 脚本 黄金3秒 内容创作', 'basic', 100, 'initial'),
('数据分析 关键指标 优化决策', 'data', 100, 'initial'),
('算法推荐 流量 爆款 运营', 'algorithm', 100, 'initial'),
('商业化 广告 电商 知识付费 变现', 'commercial', 100, 'initial'),
('AI AIGC 豆包 剪映AI 数字人', 'ai', 100, 'initial'),
('AI 辅助创作 大模型 短视频 直播', 'ai', 100, 'initial'),
('垂类策略 美妆 知识科普 剧情 户外', 'vertical', 100, 'initial'),
('剪映 工具链 发布质检 平台规则', 'compliance', 100, 'initial'),
('账号矩阵 团队管理 中高级战术', 'team', 100, 'initial');
```

---

### 2.3 Milvus Collection + ES Index + Redis 设计

#### Milvus Collection Schema

```
Collection: kb_knowledge
├── Fields:
│   ├── id (INT64, PK, auto_id=true)
│   ├── doc_id (VARCHAR(128), NOT NULL)          — 文档 ID，关联 kb_document
│   ├── chunk_id (VARCHAR(128), NOT NULL)         — 分块 ID（doc_id + "_" + chunk_index）
│   ├── project (VARCHAR(64), partition_key)       — 项目分区键
│   ├── sub_project (VARCHAR(64))                  — 子项目
│   ├── doc_type (VARCHAR(32))                     — 文档类型
│   ├── dense_vector (FLOAT_VECTOR[1024])          — BGE-M3 稠密向量
│   ├── sparse_vector (SPARSE_FLOAT_VECTOR)        — BGE-M3 稀疏向量
│   ├── content (VARCHAR(2000))                    — 文本内容（用于返回展示）
│   ├── source (VARCHAR(512))                      — 来源路径
│   ├── quality_score (FLOAT)                      — 质量评分
│   ├── boost_factor (FLOAT, default=1.0)          — 检索提权因子
│   └── create_time (INT64)                        — 创建时间戳
│
├── Index:
│   ├── dense_vector: HNSW (metric=COSINE, M=16, efConstruction=256)
│   ├── sparse_vector: SPARSE_INVERTED_INDEX (metric=IP)
│   └── 分区策略：按 project 字段自动分区
│
└── Search Params:
    ├── HNSW: ef=128（召回阶段）
    ├── 相似度阈值: cosine ≥ 0.6
    └── top_k: 20（送入 RRF 融合前）
```

#### Elasticsearch Index Mapping

```json
{
  "index": "kb_knowledge",
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_smart_analyzer": {
          "type": "custom",
          "tokenizer": "ik_smart"
        },
        "ik_max_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "doc_id":       { "type": "keyword" },
      "chunk_id":     { "type": "keyword" },
      "project":      { "type": "keyword" },
      "sub_project":  { "type": "keyword" },
      "doc_type":     { "type": "keyword" },
      "content": {
        "type": "text",
        "analyzer": "ik_max_analyzer",
        "search_analyzer": "ik_smart_analyzer"
      },
      "title": {
        "type": "text",
        "analyzer": "ik_max_analyzer",
        "search_analyzer": "ik_smart_analyzer",
        "boost": 2.0
      },
      "source":       { "type": "keyword" },
      "quality_score": { "type": "float" },
      "boost_factor": { "type": "float" },
      "create_time":  { "type": "date" }
    }
  }
}
```

#### Redis Key 设计

| Key 模式 | 类型 | TTL | 说明 |
|----------|------|-----|------|
| `cache:kb:query:{sha256(query+topK+searchMode)}` | STRING (JSON) | 3600s | 检索结果缓存 |
| `cache:kb:embed:{sha256(text)}` | STRING (binary) | 7200s | BGE-M3 嵌入向量缓存 |
| `lock:kb:index:{doc_id}` | STRING | 600s | 索引写入分布式锁（防止重复索引） |
| `lock:kb:evolve:{task_no}` | STRING | 1800s | 进化任务分布式锁（防止重复触发） |
| `stats:kb:cache:hit` | STRING (counter) | — | 缓存命中次数（INCR） |
| `stats:kb:cache:miss` | STRING (counter) | — | 缓存未命中次数（INCR） |
| `stats:kb:search:total` | STRING (counter) | — | 总检索次数 |

---

## 三、接口设计（API Design）

### 3.1 接口总表

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| **用户端** | | | | |
| 1 | POST | /api/v1/ai/generate | 登录 | 通用 AI 文本生成（按模板） |
| 2 | POST | /api/v1/ai/knowledge/query | 登录 | 知识库混合搜索（默认 hybrid 模式） |
| 2b | POST | /api/v1/ai/knowledge/hybrid-search | 登录 | 混合检索（Milvus 语义 + ES 关键词 → RRF → 重排） |
| 2c | POST | /api/v1/ai/knowledge/keyword-search | 登录 | 纯关键词检索（ES BM25） |
| 3 | POST | /api/v1/ai/quota/info | 登录 | 查询当前用户今日额度 |
| **多媒体生成（内部调用为主）** | | | | |
| 4 | POST | /api/v1/ai/media/generate-image | 登录 | AI 图像生成（分镜关键帧） |
| 5 | POST | /api/v1/ai/media/generate-video | 登录 | AI 视频生成（首尾帧→视频片段） |
| 6 | POST | /api/v1/ai/media/generate-tts | 登录 | AI 语音合成 TTS |
| 7 | POST | /api/v1/ai/media/generate-edit | 登录 | AI 视频剪辑合成 |
| 8 | POST | /api/v1/ai/media/task-status | 登录 | 查询异步生成任务状态 |
| **管理端** | | | | |
| 9 | POST | /api/v1/ai/admin/knowledge/status | 管理员 | 知识库状态 |
| 10 | POST | /api/v1/ai/admin/knowledge/index | 管理员 | 触发索引（全量/增量） |
| 11 | POST | /api/v1/ai/admin/model/list | 管理员 | 模型注册表列表（按类型分组） |
| 12 | POST | /api/v1/ai/admin/model/save | 管理员 | 新增/编辑模型 |
| 13 | POST | /api/v1/ai/admin/model/delete | 管理员 | 删除模型 |
| 14 | POST | /api/v1/ai/admin/model/test | 管理员 | 测试模型连通性（发送测试请求） |
| **任务-模型映射** | | | | |
| 15 | POST | /api/v1/ai/admin/task-model/list | 管理员 | 任务-模型映射列表（按分组） |
| 16 | POST | /api/v1/ai/admin/task-model/save | 管理员 | 编辑任务的主/备模型绑定 |
| 17 | POST | /api/v1/ai/admin/task-model/models-by-type | 管理员 | 按模型类型获取可选模型（下拉框数据源） |
| **Prompt 模板** | | | | |
| 18 | POST | /api/v1/ai/admin/prompt/list | 管理员 | Prompt 模板列表 |
| 19 | POST | /api/v1/ai/admin/prompt/get | 管理员 | Prompt 模板详情 |
| 20 | POST | /api/v1/ai/admin/prompt/save | 管理员 | 新增/编辑 Prompt 模板 |
| 21 | POST | /api/v1/ai/admin/prompt/delete | 管理员 | 删除 Prompt 模板 |
| **日志与统计** | | | | |
| 22 | POST | /api/v1/ai/admin/log/list | 管理员 | 调用日志列表 |
| 23 | POST | /api/v1/ai/admin/log/stats | 管理员 | 调用统计报表 |
| **知识源** | | | | |
| 24 | POST | /api/v1/ai/admin/source/list | 管理员 | 知识源列表 |
| 25 | POST | /api/v1/ai/admin/source/save | 管理员 | 新增/编辑知识源 |
| **进化引擎管理** | | | | |
| 26 | POST | /api/v1/ai/admin/evolve/status | 管理员 | 进化引擎状态（最近任务/评分趋势/主题池统计） |
| 27 | POST | /api/v1/ai/admin/evolve/trigger | 管理员 | 手动触发一轮进化 |
| 28 | POST | /api/v1/ai/admin/evolve/task-list | 管理员 | 进化任务历史列表（分页） |
| 29 | POST | /api/v1/ai/admin/evolve/task-detail | 管理员 | 进化任务详情（含报告内容、评分详情） |
| 30 | POST | /api/v1/ai/admin/evolve/topic/list | 管理员 | 主题池列表（按分类/优先级/来源筛选） |
| 31 | POST | /api/v1/ai/admin/evolve/topic/save | 管理员 | 新增/编辑主题 |
| 32 | POST | /api/v1/ai/admin/evolve/topic/delete | 管理员 | 删除主题 |
| 33 | POST | /api/v1/ai/admin/evolve/topic/import | 管理员 | 批量导入主题（CSV） |
| 34 | POST | /api/v1/ai/admin/evolve/score-trend | 管理员 | 质量评分趋势（日/周/月） |
| 35 | POST | /api/v1/ai/admin/index-queue/list | 管理员 | 索引队列列表 |
| 36 | POST | /api/v1/ai/admin/index-queue/retry | 管理员 | 重试失败的索引任务 |
| **基础设施管理** | | | | |
| 37 | POST | /api/v1/ai/admin/infra/health | 管理员 | 基础设施健康检查（Milvus/ES/Redis/RabbitMQ） |
| 38 | POST | /api/v1/ai/admin/cache/clear | 管理员 | 清除 Redis 缓存（按类型：query/embed/all） |

### 3.2 关键接口详情

#### 接口 1：通用 AI 生成

```
POST /api/v1/ai/generate
权限：登录（检查额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateCode | String | 是 | Prompt 模板编码 |
| variables | Map\<String, String\> | 是 | 模板变量键值对 |

**请求示例：**

```json
{
  "templateCode": "video_copywriting",
  "variables": {
    "persona": "定位：平价美妆分享\n风格：亲切\n受众：18-30岁女性",
    "topic": "秋冬平价面霜推荐",
    "style": "种草安利",
    "duration": "60秒"
  }
}
```

**响应：**

```json
{
  "status": 200,
  "data": {
    "content": "【AI 生成的文案内容】...",
    "modelUsed": "deepseek_v3",
    "promptTokens": 350,
    "completionTokens": 1200,
    "durationMs": 3500,
    "quotaRemaining": 7
  }
}
```

**业务逻辑：**
1. 检查用户今日调用额度
2. 查询 Prompt 模板（按 template_code + 最新 version）
3. 用 variables 替换模板中的占位符
4. 可选：检索知识库获取相关上下文，拼入 prompt
5. 调用主模型生成
6. 如果主模型超时/失败，自动切换备用模型
7. 记录调用日志（ai_call_log）
8. 更新调用额度（ai_call_quota）
9. 返回生成结果

**错误码：** 4001（AI 调用额度已用完）、4002（AI 模型不可用）、4003（Prompt 模板不存在）

---

#### 接口 2：知识库搜索（默认混合检索）

```
POST /api/v1/ai/knowledge/query
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | String | 是 | 搜索内容 |
| topK | Integer | 否 | 返回条数，默认 10 |
| searchMode | String | 否 | 检索模式：hybrid（默认）/ semantic / keyword |

**响应：**

```json
{
  "status": 200,
  "data": {
    "results": [
      {
        "content": "知识条目内容...",
        "source": "douyin/daily/2026-02-20.md",
        "score": 0.85,
        "bm25Score": 12.5,
        "vectorScore": 0.82,
        "rerankerScore": 0.91,
        "searchMode": "hybrid"
      }
    ],
    "totalFound": 10,
    "searchMode": "hybrid",
    "cacheHit": false,
    "durationMs": 800
  }
}
```

---

#### 接口 2b：混合检索（详细版）

```
POST /api/v1/ai/knowledge/hybrid-search
权限：登录
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | String | 是 | 搜索内容 |
| topK | Integer | 否 | 最终返回条数，默认 10 |
| project | String | 否 | 项目过滤（Milvus 分区） |
| docType | String | 否 | 文档类型过滤 |
| minScore | Float | 否 | 最低相关性分数阈值 |
| bm25Weight | Float | 否 | BM25 权重（默认 1.0） |
| vectorWeight | Float | 否 | 向量权重（默认 0.7） |

**响应：**

```json
{
  "status": 200,
  "data": {
    "results": [
      {
        "content": "知识条目内容...",
        "source": "douyin/daily/2026-02-20.md",
        "docId": "abc123",
        "chunkId": "abc123_0",
        "project": "default",
        "docType": "evolved",
        "bm25Score": 12.5,
        "vectorScore": 0.82,
        "rrfScore": 0.035,
        "rerankerScore": 0.91,
        "qualityScore": 78.5
      }
    ],
    "totalFound": 10,
    "milvusHits": 20,
    "esHits": 20,
    "rrfFusedCount": 30,
    "rerankedCount": 20,
    "cacheHit": false,
    "durationMs": 1200,
    "durationBreakdown": {
      "embedMs": 150,
      "milvusMs": 200,
      "esMs": 180,
      "rrfMs": 10,
      "rerankMs": 400
    }
  }
}
```

---

#### 接口 37：基础设施健康检查

```
POST /api/v1/ai/admin/infra/health
权限：管理员
```

**响应：**

```json
{
  "status": 200,
  "data": {
    "overall": "healthy",
    "components": {
      "milvus": {
        "status": "healthy",
        "version": "2.4.x",
        "collections": 1,
        "totalEntities": 87000,
        "partitions": 3,
        "latencyMs": 15
      },
      "elasticsearch": {
        "status": "healthy",
        "version": "8.x",
        "indices": 1,
        "totalDocs": 87000,
        "storageSize": "1.2GB",
        "latencyMs": 10
      },
      "redis": {
        "status": "healthy",
        "version": "7.x",
        "connectedClients": 12,
        "usedMemory": "256MB",
        "cacheHitRate": "68.5%",
        "latencyMs": 2
      },
      "rabbitmq": {
        "status": "healthy",
        "version": "3.x",
        "queues": {
          "ai.index": {"messages": 2, "consumers": 1},
          "ai.evolve": {"messages": 0, "consumers": 1},
          "ai.media": {"messages": 0, "consumers": 1},
          "ai.index.dlq": {"messages": 0}
        },
        "latencyMs": 5
      }
    }
  }
}
```

---

#### 接口 38：缓存管理

```
POST /api/v1/ai/admin/cache/clear
权限：管理员
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cacheType | String | 是 | 缓存类型：query（检索缓存）/ embed（嵌入缓存）/ all（全部清除） |
| project | String | 否 | 指定项目（为空则清除所有项目的缓存） |

**响应：**

```json
{
  "status": 200,
  "data": {
    "cacheType": "query",
    "clearedKeys": 156,
    "message": "已清除 156 条检索缓存"
  }
}
```

---

#### 接口 4：AI 图像生成

```
POST /api/v1/ai/media/generate-image
权限：登录（检查额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| prompt | String | 是 | 图像描述（分镜画面描述） |
| portraitUrl | String | 否 | 人像照片 URL（用于 AI 合成人物画面） |
| backgroundUrl | String | 否 | 背景图片 URL |
| width | Integer | 否 | 图像宽度，默认 1080 |
| height | Integer | 否 | 图像高度，默认 1920 |
| style | String | 否 | 画面风格（realistic/anime/illustration） |

**响应：**

```json
{
  "status": 200,
  "data": {
    "taskId": "img_20260224_001",
    "status": "completed",
    "imageUrl": "https://xxx.bos.baidu.com/ai/images/xxx.png",
    "durationMs": 8000,
    "quotaRemaining": 5
  }
}
```

---

#### 接口 6：AI 语音合成 TTS

```
POST /api/v1/ai/media/generate-tts
权限：登录（检查额度）
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| text | String | 是 | 要合成的文本 |
| voice | String | 否 | 音色选择（默认 female_1） |
| speed | Float | 否 | 语速（0.5-2.0，默认 1.0） |
| volume | Float | 否 | 音量（0.5-2.0，默认 1.0） |

**响应：**

```json
{
  "status": 200,
  "data": {
    "taskId": "tts_20260224_001",
    "status": "completed",
    "audioUrl": "https://xxx.bos.baidu.com/ai/audio/xxx.mp3",
    "duration": 5,
    "durationMs": 3000,
    "quotaRemaining": 4
  }
}
```

---

#### 接口 22：进化引擎状态

```
POST /api/v1/ai/admin/evolve/status
权限：管理员
```

**响应：**

```json
{
  "status": 200,
  "data": {
    "lastTask": {
      "taskNo": "evolve_20260224_060000",
      "scoreTotal": 72,
      "status": "completed",
      "createTime": "2026-02-24 06:00:00"
    },
    "stats": {
      "totalTasks": 156,
      "avgScore": 68.5,
      "avgScoreLast7d": 71.2,
      "completedCount": 150,
      "failedCount": 6,
      "reportsIndexed": 142
    },
    "topicPool": {
      "totalTopics": 487,
      "enabledTopics": 480,
      "categoryDistribution": {
        "live": 62, "ai": 45, "vertical": 38,
        "commercial": 35, "algorithm": 42, "data": 40,
        "team": 28, "compliance": 32, "cross_domain": 25,
        "brand": 30, "ad": 35, "competition": 28, "basic": 40
      },
      "deepenQueueCount": 3
    },
    "indexQueue": {
      "pending": 2,
      "processing": 1,
      "failed": 0
    },
    "nextScheduledRun": "2026-02-25 06:00:00"
  }
}
```

---

#### 接口 23：手动触发进化

```
POST /api/v1/ai/admin/evolve/trigger
权限：管理员
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| topicIds | List\<Long\> | 否 | 指定主题 ID（为空则自动采样） |
| evolveAngle | String | 否 | 指定进化角度（为空则随机） |

**响应：**

```json
{
  "status": 200,
  "data": {
    "taskNo": "evolve_20260224_143000",
    "message": "进化任务已启动，可在任务历史中查看进度"
  }
}
```

**业务逻辑：**
1. 创建 ai_evolve_task 记录（status=pending）
2. 异步执行进化管线
3. 管线各阶段更新 status（gathering → generating → scoring → expanding → indexing → completed）
4. 异常时 status=failed，记录 error_message

---

## 四、知识库系统详细设计（Knowledge Base System Design）

### 4.1 知识库导入与处理管道

#### 4.1.1 文档导入流程

```
文档源 (D:\docs)
    ↓
[文档扫描服务]
    ├─ 按日期分类扫描
    ├─ 支持格式：PDF、MD、TXT、DOCX
    ├─ 文件大小限制：100MB
    └─ 生成 document_id（UUID）
    ↓
[文档解析 & 预处理]
    ├─ 格式转换（PDF/DOCX → 纯文本）
    ├─ 文本提取（Apache Tika）
    ├─ 元数据提取（标题、作者、日期等）
    ├─ 编码检测与转换（UTF-8）
    └─ 特殊字符清理
    ↓
[文档分块处理]
    ├─ 分块大小：512 tokens
    ├─ 重叠大小：50 tokens
    ├─ 生成 chunk_id（doc_id + "_" + chunk_index）
    └─ 计算 token_count
    ↓
[消息队列] (RabbitMQ)
    ├─ 队列：ai.index
    ├─ 消息格式：{document_id, chunks[], source_type}
    └─ 优先级：normal / high / low
    ↓
并行处理
    ├─ [ES索引服务] → Elasticsearch
    │   ├─ 索引名：kb_knowledge
    │   ├─ 分析器：ik_max_word（分词）
    │   └─ 字段：doc_id, chunk_id, content, title, source, quality_score
    │
    └─ [向量化服务] → Milvus
        ├─ 模型：BGE-M3（1024D 稠密+稀疏向量）
        ├─ 集合：kb_knowledge
        ├─ 分区：按 project 字段
        └─ 索引：HNSW（cosine 相似度）
    ↓
[索引完成]
    ├─ 更新 kb_document.status = 'completed'
    ├─ 记录索引时间
    └─ 发送成功通知
```

#### 4.1.2 文档处理表结构

**kb_document 表（知识文档元数据）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| doc_id | VARCHAR(128) | 文档唯一ID（UUID） |
| title | VARCHAR(255) | 文档标题 |
| description | TEXT | 文档描述 |
| category | VARCHAR(100) | 分类（按日期或主题） |
| file_path | VARCHAR(512) | 原始文件路径 |
| file_type | VARCHAR(50) | 文件类型（pdf/md/txt/docx） |
| file_size | BIGINT | 文件大小（字节） |
| upload_date | DATE | 上传日期 |
| status | VARCHAR(50) | 状态（pending/processing/completed/failed） |
| error_message | TEXT | 错误信息 |
| chunk_count | INTEGER | 分块数量 |
| token_count | BIGINT | 总token数 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |
| created_by | BIGINT | 创建者ID |

**document_chunks 表（文档分块）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| document_id | BIGINT | 关联 kb_document.id |
| chunk_index | INTEGER | 分块序号 |
| content | TEXT | 分块内容 |
| token_count | INTEGER | 该分块的token数 |
| vector_id | VARCHAR(255) | Milvus中的向量ID |
| es_doc_id | VARCHAR(255) | Elasticsearch中的文档ID |
| quality_score | FLOAT | 质量评分（0-100） |
| created_at | TIMESTAMP | 创建时间 |

### 4.2 知识库查询系统

#### 4.2.1 混合检索流程（RRF融合）

```
用户查询
    ↓
[查询预处理]
    ├─ 文本清理
    ├─ 分词（IK分词）
    └─ 查询扩展（可选）
    ↓
并行执行
    ├─ [向量检索] (Milvus)
    │   ├─ 模型：BGE-M3 嵌入
    │   ├─ 查询向量维度：1024D
    │   ├─ 相似度阈值：≥ 0.6
    │   ├─ 返回：top_k=20
    │   └─ 结果：[(doc_id, chunk_id, similarity_score), ...]
    │
    └─ [关键词检索] (Elasticsearch)
        ├─ 查询方式：BM25
        ├─ 分析器：ik_smart（搜索）
        ├─ 返回：top_k=20
        └─ 结果：[(doc_id, chunk_id, bm25_score), ...]
    ↓
[RRF融合]
    ├─ 公式：RRF_score = 1/(k + rank)
    ├─ k值：60（标准值）
    ├─ 融合权重：
    │   ├─ 向量检索权重：0.7
    │   └─ 关键词检索权重：0.3
    └─ 合并结果，按融合分数排序
    ↓
[Cross-Encoder重排]
    ├─ 模型：BGE-reranker-v2
    ├─ 输入：(query, candidate_text) 对
    ├─ 输出：重排分数（0-1）
    └─ 返回：top_k=10（最终结果）
    ↓
[缓存存储]
    ├─ Key：cache:kb:query:{sha256(query+topK+searchMode)}
    ├─ Value：JSON格式的检索结果
    ├─ TTL：3600秒（1小时）
    └─ 命中率统计
    ↓
[结果返回]
    ├─ 格式：[{doc_id, chunk_id, content, score, source}, ...]
    ├─ 包含元数据：标题、来源、质量评分
    └─ 响应时间：< 3秒（P99）
```

#### 4.2.2 查询接口详细设计

**混合搜索接口**

```
POST /api/v1/ai/knowledge/hybrid-search

请求体：
{
    "query": "抖音短视频如何提高播放量",
    "top_k": 10,
    "search_mode": "hybrid",  // hybrid / vector / keyword
    "filters": {
        "category": "运营技巧",
        "date_range": ["2025-01-01", "2026-02-25"]
    },
    "threshold": 0.5  // 相似度阈值
}

响应：
{
    "status": 200,
    "data": {
        "total": 150,
        "items": [
            {
                "doc_id": "doc_001",
                "chunk_id": "doc_001_5",
                "title": "短视频运营指南",
                "content": "提高播放量的关键因素包括...",
                "source": "D:/docs/2025-02/运营指南.pdf",
                "quality_score": 85,
                "vector_score": 0.92,
                "keyword_score": 0.78,
                "combined_score": 0.87,
                "rank": 1
            },
            ...
        ],
        "search_time_ms": 1250,
        "cache_hit": false
    }
}
```

**纯关键词检索接口**

```
POST /api/v1/ai/knowledge/keyword-search

请求体：
{
    "query": "抖音 播放量 算法",
    "top_k": 20,
    "filters": {
        "doc_type": "pdf"
    }
}

响应：
{
    "status": 200,
    "data": {
        "total": 200,
        "items": [
            {
                "doc_id": "doc_002",
                "chunk_id": "doc_002_3",
                "title": "抖音算法解析",
                "content": "抖音推荐算法基于...",
                "bm25_score": 12.5,
                "rank": 1
            },
            ...
        ]
    }
}
```

### 4.3 嵌入向量缓存管理

#### 4.3.1 缓存策略

| 缓存类型 | Key模式 | TTL | 说明 |
|---------|--------|-----|------|
| 查询结果缓存 | `cache:kb:query:{hash}` | 3600s | 热点查询结果 |
| 嵌入向量缓存 | `cache:kb:embed:{hash}` | 7200s | BGE-M3嵌入向量 |
| 分布式锁 | `lock:kb:index:{doc_id}` | 600s | 防止重复索引 |
| 进化任务锁 | `lock:kb:evolve:{task_no}` | 1800s | 防止重复进化 |

#### 4.3.2 缓存命中率优化

```
目标：
- Phase 1：> 50% 缓存命中率
- Phase 2：> 70% 缓存命中率

优化策略：
1. 热点查询预热（定期更新热门查询缓存）
2. 嵌入向量复用（相同文本使用缓存向量）
3. 查询规范化（统一查询格式以提高缓存命中）
4. 缓存预热（系统启动时预加载常用查询）
```

### 4.4 知识库索引管理

#### 4.4.1 索引队列处理

```
ai_index_queue 表结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| source_type | VARCHAR(32) | 来源类型（evolved/knowledge/manual） |
| source_id | BIGINT | 来源ID |
| content | TEXT | 待索引内容 |
| priority | INTEGER | 优先级（1=高, 2=中, 3=低） |
| status | VARCHAR(16) | 状态（pending/processing/done/failed） |
| retry_count | INTEGER | 重试次数 |
| error_message | TEXT | 错误信息 |
| processing_since | TIMESTAMP | 处理开始时间 |
| created_at | TIMESTAMP | 创建时间 |

处理流程：
1. 消费线程每30秒轮询一次pending队列
2. 取出优先级最高的任务
3. 更新status为processing
4. 执行ES和Milvus双写
5. 成功则status=done，失败则status=failed
6. 超时检测：processing_since超过10分钟的记录重置为pending
7. 最大重试次数：3次
```

#### 4.4.2 全量索引与增量索引

```
全量索引：
- 触发条件：系统初始化、知识库重建
- 流程：扫描所有kb_document → 生成chunks → 批量入队 → 并行处理
- 预期时间：取决于文档数量和大小

增量索引：
- 触发条件：新文档上传、进化报告生成
- 流程：仅处理新增/修改的文档 → 生成chunks → 入队处理
- 优先级：high（优先处理新增内容）
```

---

## 五、AI直播分析与话术系统（Live Analysis & Script System）

### 5.1 AI直播实时分析与诊断

#### 5.1.1 实时数据采集

```
直播进行中
    ↓
[实时数据采集]
    ├─ 观看人数、新增粉丝、点赞数
    ├─ 评论内容、评论情感、评论热度
    ├─ 转化数据（下单数、客单价、GMV）
    ├─ 观众行为（进入、离开、互动时间点）
    └─ 直播间热力图（哪些时间段热度高）
    ↓
[实时分析引擎]
    ├─ 转化率分析（实时转化率、峰值转化率）
    ├─ 观众流失分析（何时观众离开、离开原因）
    ├─ 互动热度分析（互动高峰、互动冷点）
    ├─ 产品热度分析（哪个产品热度高、转化好）
    └─ 话术效果分析（哪句话术效果好、哪句冷场）
    ↓
[AI诊断引擎]
    ├─ 问题识别（转化率低、观众流失、冷场）
    ├─ 根因分析（为什么转化率低、为什么观众离开）
    ├─ 实时建议（现在应该说什么、应该做什么）
    └─ 优化方案（如何改进话术、如何提高转化）
```

#### 5.1.2 直播诊断指标体系

| 指标 | 说明 | 目标值 | 告警阈值 |
|------|------|--------|---------|
| 转化率 | 下单人数/观看人数 | > 5% | < 2% |
| 客单价 | 总GMV/下单人数 | > 100元 | < 50元 |
| 观众留存率 | 平均观看时长/直播时长 | > 30% | < 10% |
| 互动率 | 互动人数/观看人数 | > 20% | < 5% |
| 点赞率 | 点赞数/观看人数 | > 10% | < 2% |
| 评论热度 | 评论数/观看人数 | > 5% | < 1% |
| 粉丝转化率 | 新增粉丝/观看人数 | > 3% | < 0.5% |
| 人均观看时长 | 总观看时长/观看人数 | > 5分钟 | < 1分钟 |

#### 5.1.3 AI实时建议引擎

```
直播进行中，AI持续监测
    ↓
场景1：转化率突然下降
    AI分析：观众评论中出现"太贵了"、"没有优惠"
    AI建议：
    1. 立即推出限时优惠（如"前100名送礼"）
    2. 切换话术，强调产品价值而不是价格
    3. 展示其他观众的购买反馈（社会证明）
    ↓
场景2：观众大量流失
    AI分析：节奏拖沓、内容重复、缺乏互动
    AI建议：
    1. 增加互动环节（抽奖、问答）
    2. 切换产品，展示新品
    3. 制造悬念（"接下来有一个重磅福利"）
    ↓
场景3：冷场
    AI分析：评论减少、点赞下降、互动停滞
    AI建议：
    1. 讲故事，制造情感共鸣
    2. 邀请观众互动（"评论区扣1"）
    3. 展示限量产品，制造紧迫感
```

### 5.2 AI直播话术生成系统

#### 5.2.1 话术生成流程

```
直播前准备
    ↓
[输入信息]
    ├─ 产品信息（名称、价格、特点、卖点、库存）
    ├─ 目标客户（年龄、性别、消费能力、痛点）
    ├─ 竞品信息（竞品价格、竞品卖点、竞争优势）
    ├─ 直播目标（销售目标、粉丝增长目标、GMV目标）
    └─ 直播时长、产品数量、促销策略
    ↓
[AI话术引擎]
    ├─ 开场话术（吸引注意、建立信任、引入产品）
    ├─ 产品介绍话术（产品特点、产品优势、使用场景）
    ├─ 价格话术（价格合理性、价值对标、促销力度）
    ├─ 促销话术（限时优惠、赠品、返利、积分）
    ├─ 互动话术（提问、投票、抽奖、评论互动）
    ├─ 转化话术（CTA、紧迫感、社会证明、保障承诺）
    ├─ 异议处理话术（价格异议、质量异议、信任异议）
    └─ 结尾话术（感谢、预告、粉丝福利、下次预告）
    ↓
[话术优化]
    ├─ 根据直播进度动态调整
    ├─ 根据观众反应实时优化
    ├─ 学习成功直播的话术模式
    └─ 持续改进话术效果
```

#### 5.2.2 话术模板体系

**开场话术模板：**
```
"大家好，欢迎来到我们的直播间！
我是{主播名}，今天给大家带来的是{产品名}。
这个产品有多火呢？{数据支撑}
我们已经为{数字}个用户解决了{问题}
今天直播间有{优惠信息}，错过就没有了！
先点个关注，不要错过任何优惠哦~"
```

**产品介绍话术模板：**
```
"这个产品最大的特点就是{核心卖点}
为什么这么说呢？因为{原因1}、{原因2}、{原因3}
我们的用户反馈是{用户评价}
对比{竞品}，我们的优势是{优势1}、{优势2}
这个价格在市场上是{价格定位}"
```

**转化话术模板：**
```
"现在下单的朋友，我们送{赠品}
而且还有{保障承诺}
已经有{数字}个朋友下单了
你还在犹豫什么呢？
{紧迫感话术}
赶快点击购买吧！"
```

**异议处理话术模板：**
```
价格异议："觉得贵？我来给你算一笔账...{价值拆解}"
质量异议："我们有{质量保障}，而且{售后政策}"
信任异议："我们已经服务了{用户数}个客户，好评率{好评率}"
```

#### 5.2.3 动态话术调整

```
直播进行中
    ↓
[实时监测]
    ├─ 观众评论情感分析（正面/中立/负面）
    ├─ 转化率实时监测（上升/下降/平稳）
    ├─ 观众流失监测（加速/减缓/稳定）
    └─ 互动热度监测（高/中/低）
    ↓
[AI判断]
    ├─ 观众兴趣点在哪里？→ 强化该方向
    ├─ 观众的主要疑虑是什么？→ 针对性解答
    ├─ 哪个产品热度最高？→ 重点推介
    └─ 现在应该强调什么？→ 动态切换话术
    ↓
[动态调整策略]
    ├─ 转化率低 → 强调价值、推出优惠、增加社会证明
    ├─ 观众流失 → 增加互动、制造悬念、切换产品
    ├─ 冷场 → 讲故事、制造情感共鸣、发起互动
    ├─ 某产品热度高 → 重点推介、加大优惠、延长讲解
    └─ 观众有疑虑 → 针对性解答、提供保障、展示案例
```

### 5.3 直播数据分析数据库设计

#### live_analysis — 直播分析记录表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| live_session_id | BIGINT | NOT NULL | — | 关联直播场次 ID |
| analysis_type | VARCHAR(32) | NOT NULL | — | 分析类型：realtime / post_live / comparison |
| total_viewers | INTEGER | | 0 | 总观看人数 |
| peak_viewers | INTEGER | | 0 | 峰值观看人数 |
| avg_watch_duration | INTEGER | | 0 | 平均观看时长（秒） |
| total_likes | INTEGER | | 0 | 总点赞数 |
| total_comments | INTEGER | | 0 | 总评论数 |
| total_shares | INTEGER | | 0 | 总转发数 |
| new_followers | INTEGER | | 0 | 新增粉丝数 |
| conversion_rate | DECIMAL(5,2) | | 0 | 转化率（%） |
| total_gmv | DECIMAL(12,2) | | 0 | 总GMV |
| avg_order_value | DECIMAL(10,2) | | 0 | 客单价 |
| total_orders | INTEGER | | 0 | 总订单数 |
| sentiment_positive | DECIMAL(5,2) | | 0 | 正面评论比例（%） |
| sentiment_negative | DECIMAL(5,2) | | 0 | 负面评论比例（%） |
| ai_suggestions | TEXT | | — | AI建议（JSON数组） |
| ai_diagnosis | TEXT | | — | AI诊断结果（JSON） |
| hot_products | TEXT | | — | 热门产品排名（JSON数组） |
| timeline_data | TEXT | | — | 时间线数据（JSON，每分钟指标） |
| created_at | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(live_session_id)`、`(analysis_type, created_at DESC)`

#### live_script_template — 直播话术模板表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| template_name | VARCHAR(128) | NOT NULL | — | 模板名称 |
| script_type | VARCHAR(32) | NOT NULL | — | 话术类型：opening / product / price / promotion / interaction / conversion / objection / closing |
| category | VARCHAR(64) | | — | 适用品类（美妆/食品/服装/数码等） |
| content | TEXT | NOT NULL | — | 话术内容（含变量占位符） |
| variables | VARCHAR(512) | | — | 支持的变量列表（JSON数组） |
| effectiveness_score | DECIMAL(5,2) | | 0 | 效果评分（基于历史数据） |
| usage_count | INTEGER | | 0 | 使用次数 |
| avg_conversion_rate | DECIMAL(5,2) | | 0 | 平均转化率（%） |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(script_type, category, effectiveness_score DESC)`

---

## 六、AI短视频脚本与视频生成系统（Video Script & Generation System）

### 6.1 AI短视频脚本生成

#### 6.1.1 脚本生成流程

```
选题确定
    ↓
[输入信息]
    ├─ 选题（主题、热点、关键词）
    ├─ 目标受众（年龄、性别、兴趣、痛点）
    ├─ 内容类型（教育、娱乐、带货、互动、种草）
    ├─ 视频时长（15秒、30秒、60秒、3分钟）
    ├─ 转化目标（点赞、评论、转发、下单、关注）
    └─ 品牌信息（品牌调性、产品卖点、竞品差异）
    ↓
[AI脚本引擎]
    ├─ 创意方向生成（3-5个创意方向）
    ├─ 脚本结构设计（开头、中间、结尾）
    ├─ 视觉描述（分镜、画面、特效、转场）
    ├─ 音频描述（配音、音效、背景音乐）
    ├─ 字幕文案（关键信息、情感表达）
    └─ 互动设计（评论引导、转发引导、关注引导）
    ↓
[脚本优化]
    ├─ 优化完成率（开头吸引力、节奏紧凑度）
    ├─ 优化点赞率（情感共鸣、视觉冲击）
    ├─ 优化转发率（有价值、有趣、有启发）
    ├─ 优化转化率（清晰CTA、紧迫感、社会证明）
    └─ 优化评论率（制造讨论点、提出问题）
```

#### 6.1.2 AI创意方向生成

```
输入：选题"抖音短视频如何提高播放量"
    ↓
AI生成多个创意方向：

创意1：对比类
- 低播放量视频 vs 高播放量视频的对比
- 强调差异、激励观众改进

创意2：教育类
- 分享5个提高播放量的秘诀
- 建立专业形象、获得信任

创意3：故事类
- 讲述从0到100万播放的经历
- 制造情感共鸣、获得点赞

创意4：互动类
- 提问"你的视频播放量是多少"
- 引导评论、增加互动

创意5：数据类
- 用数据图表展示播放量增长
- 增加可信度、获得转发

用户选择创意方向 → AI生成详细脚本 → 生成分镜
```

#### 6.1.3 脚本模板体系

**教育类脚本模板：**
```
[0-2秒] 开头 — 吸引注意
- 视觉：制造悬念、视觉冲击
- 文案："你知道吗？{惊人事实}"
- 目的：吸引注意、制造好奇

[2-20秒] 中间 — 传递价值
- 视觉：展示解决方案、演示过程
- 文案：分3个要点讲解
- 目的：传递价值、建立信任

[20-30秒] 结尾 — 引导转化
- 视觉：总结、CTA
- 文案："想了解更多？评论区告诉我"
- 目的：引导互动、转化
```

**带货类脚本模板：**
```
[0-2秒] 开头 — 制造欲望
- 视觉：产品展示、使用场景
- 文案："这个{产品}改变了我的{生活}"
- 目的：制造欲望、吸引注意

[2-20秒] 中间 — 建立需求
- 视觉：产品特点展示、对比演示、使用效果
- 文案：强调卖点、解决痛点、用户证言
- 目的：建立需求、提高转化意愿

[20-30秒] 结尾 — 促进转化
- 视觉：产品、价格、优惠
- 文案："现在下单{优惠}，链接在评论区"
- 目的：明确CTA、促进转化
```

**故事类脚本模板：**
```
[0-3秒] 开头 — 设置悬念
- 视觉：情境展示
- 文案："那天，{事件发生}..."
- 目的：设置悬念、吸引观看

[3-25秒] 中间 — 故事发展
- 视觉：故事情节展开
- 文案：起承转合、情感递进
- 目的：制造情感共鸣

[25-30秒] 结尾 — 情感升华
- 视觉：结局、反转
- 文案："{感悟/金句}"
- 目的：情感共鸣、引导点赞转发
```

### 6.2 AI视频生成完整链路

#### 6.2.1 从分镜到完整视频

```
脚本确定
    ↓
[Step 1: 分镜设计]
    ├─ 根据脚本自动生成分镜脚本
    ├─ 每个镜头：时长、内容描述、画面构图
    ├─ 转场方式：切换、淡入淡出、滑动、缩放
    ├─ 字幕位置、字体、颜色、动画
    └─ 音频配置：配音、音效、背景音乐
    ↓
[Step 2: 视觉内容生成]
    ├─ AI绘画：根据分镜描述生成关键帧图像
    │   ├─ 模型：Stable Diffusion XL / DALL-E 3
    │   ├─ 风格：写实/动漫/插画/3D
    │   ├─ 分辨率：1080x1920（竖屏）
    │   └─ 人物一致性：LoRA/IP-Adapter
    │
    ├─ AI视频生成：关键帧→视频片段
    │   ├─ 模型：Runway Gen-3 / Pika / Kling
    │   ├─ 首尾帧插值：生成流畅过渡
    │   ├─ 运动控制：镜头运动、人物动作
    │   └─ 时长：每段3-5秒
    │
    ├─ AI数字人：虚拟主播/数字人播报
    │   ├─ 模型：HeyGen / D-ID / SadTalker
    │   ├─ 口型同步：配音→口型动画
    │   ├─ 表情控制：情感表达
    │   └─ 动作控制：手势、姿态
    │
    └─ AI特效：转场、滤镜、贴纸、动画
        ├─ 转场效果：淡入淡出、滑动、缩放、旋转
        ├─ 滤镜效果：色调、对比度、饱和度
        ├─ 贴纸动画：表情、文字、图标
        └─ 粒子效果：光效、烟雾、火花
    ↓
[Step 3: 音频内容生成]
    ├─ AI配音（TTS）
    │   ├─ 模型：ChatTTS / CosyVoice / Edge-TTS
    │   ├─ 多种音色：男声/女声/童声/方言
    │   ├─ 情感控制：开心/严肃/激动/温柔
    │   └─ 语速控制：快/中/慢
    │
    ├─ AI背景音乐
    │   ├─ 从音乐库匹配（按风格、节奏、情感）
    │   ├─ AI生成（Suno / MusicGen）
    │   └─ 音量自动调节（配音时降低）
    │
    └─ AI音效
        ├─ 环境音效（自然、城市、室内）
        ├─ 动作音效（点击、滑动、弹出）
        └─ 情感音效（惊喜、紧张、欢快）
    ↓
[Step 4: 视频合成]
    ├─ 合并视觉内容和音频
    ├─ 时间轴对齐（音视频同步）
    ├─ 添加转场效果
    ├─ 添加字幕（位置、字体、动画）
    ├─ 色彩校正、亮度调整
    ├─ 添加水印、Logo
    └─ 视频渲染（H.264/H.265，1080p/4K）
    ↓
[Step 5: 质量检查与优化]
    ├─ AI预测完成率（基于开头、节奏、内容）
    ├─ AI预测点赞率（基于情感、视觉、内容）
    ├─ AI预测转发率（基于价值、趣味、启发）
    ├─ 检查清晰度、音质、字幕准确性
    └─ 如果评分低 → 自动优化或重新生成
    ↓
[Step 6: 多版本输出]
    ├─ 竖屏版本 1080x1920（抖音、快手）
    ├─ 横屏版本 1920x1080（B站、YouTube）
    ├─ 方形版本 1080x1080（小红书、微信）
    ├─ 15秒精简版（快速浏览）
    ├─ 30秒标准版（标准版本）
    └─ 60秒完整版（完整内容）
```

#### 6.2.2 视频生成数据库设计

##### video_generation_task — 视频生成任务表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| task_no | VARCHAR(64) | NOT NULL | — | 任务编号（唯一） |
| script_id | BIGINT | | — | 关联脚本 ID |
| task_type | VARCHAR(32) | NOT NULL | — | 任务类型：full_video / clip / image / tts / music |
| status | VARCHAR(32) | NOT NULL | 'pending' | 状态：pending / storyboard / generating_visual / generating_audio / compositing / quality_check / completed / failed |
| input_params | TEXT | NOT NULL | — | 输入参数（JSON：脚本、分镜、风格等） |
| storyboard_data | TEXT | | — | 分镜数据（JSON：每个镜头的描述、时长、画面） |
| visual_assets | TEXT | | — | 视觉资产（JSON：图像URL、视频片段URL） |
| audio_assets | TEXT | | — | 音频资产（JSON：配音URL、音乐URL、音效URL） |
| output_url | VARCHAR(512) | | — | 最终视频URL |
| output_urls | TEXT | | — | 多版本输出URL（JSON：竖屏、横屏、方形等） |
| duration_seconds | INTEGER | | 0 | 视频时长（秒） |
| resolution | VARCHAR(32) | | '1080x1920' | 分辨率 |
| quality_score | DECIMAL(5,2) | | 0 | AI质量评分（0-100） |
| predicted_completion_rate | DECIMAL(5,2) | | 0 | 预测完成率（%） |
| predicted_like_rate | DECIMAL(5,2) | | 0 | 预测点赞率（%） |
| processing_time_ms | BIGINT | | 0 | 处理耗时（毫秒） |
| error_message | TEXT | | — | 错误信息 |
| retry_count | INTEGER | | 0 | 重试次数 |
| created_by | BIGINT | | — | 创建者 |
| created_at | TIMESTAMP | | CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`UNIQUE(task_no)`、`(status, created_at DESC)`、`(created_by, created_at DESC)`

##### video_script — 短视频脚本表

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGSERIAL | PK | 自增 | 主键 |
| title | VARCHAR(255) | NOT NULL | — | 脚本标题 |
| topic | VARCHAR(255) | NOT NULL | — | 选题/主题 |
| script_type | VARCHAR(32) | NOT NULL | — | 脚本类型：education / entertainment / commerce / interaction / story |
| target_audience | TEXT | | — | 目标受众（JSON） |
| target_duration | INTEGER | | 30 | 目标时长（秒） |
| creative_direction | TEXT | | — | 创意方向描述 |
| script_content | TEXT | NOT NULL | — | 脚本内容（JSON：开头/中间/结尾，每段含文案、视觉描述、音频描述） |
| storyboard | TEXT | | — | 分镜脚本（JSON：每个镜头的详细描述） |
| conversion_goal | VARCHAR(64) | | — | 转化目标：like / comment / share / order / follow |
| brand_info | TEXT | | — | 品牌信息（JSON） |
| ai_quality_score | DECIMAL(5,2) | | 0 | AI质量评分 |
| usage_count | INTEGER | | 0 | 使用次数 |
| avg_completion_rate | DECIMAL(5,2) | | 0 | 平均完成率（%） |
| avg_like_rate | DECIMAL(5,2) | | 0 | 平均点赞率（%） |
| status | INTEGER | NOT NULL | 1 | 1=启用 0=禁用 |
| deleted | INTEGER | NOT NULL | 0 | 逻辑删除 |
| created_by | BIGINT | | — | 创建者 |
| create_time | TIMESTAMP | | CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | | CURRENT_TIMESTAMP | |

索引：`(script_type, ai_quality_score DESC)`、`(topic)`、`(created_by, create_time DESC)`

#### 6.2.3 视频生成API设计

| # | 方法 | 路径 | 权限 | 说明 |
|---|------|------|------|------|
| 1 | POST | /api/v1/ai/video/script/generate | 登录 | AI生成短视频脚本（输入选题→输出脚本+分镜） |
| 2 | POST | /api/v1/ai/video/script/creative-directions | 登录 | AI生成创意方向（输入选题→输出3-5个创意方向） |
| 3 | POST | /api/v1/ai/video/storyboard/generate | 登录 | 根据脚本生成分镜 |
| 4 | POST | /api/v1/ai/video/generate | 登录 | 从分镜生成完整视频（异步任务） |
| 5 | POST | /api/v1/ai/video/task-status | 登录 | 查询视频生成任务状态 |
| 6 | POST | /api/v1/ai/video/optimize | 登录 | AI优化已生成的视频 |
| 7 | POST | /api/v1/ai/video/multi-version | 登录 | 生成多版本（竖屏/横屏/方形/多时长） |
| 8 | POST | /api/v1/ai/video/predict-performance | 登录 | AI预测视频效果（完成率、点赞率等） |
| 9 | POST | /api/v1/ai/admin/video/task-list | 管理员 | 视频生成任务列表 |
| 10 | POST | /api/v1/ai/admin/video/script-list | 管理员 | 脚本模板列表 |

---

## 五、页面设计（Frontend Design）

### 5.1 页面清单

| # | 页面 | 路径 | 入口 | 说明 |
|---|------|------|------|------|
| 1 | 知识库状态 | /admin/ai/knowledge.html | 管理端菜单 | 知识库看板 + 索引操作 |
| 2 | 模型注册管理 | /admin/ai/model-list.html | 管理端菜单 | 模型注册表 CRUD（按类型分组） |
| 3 | 任务-模型配置 | /admin/ai/task-model.html | 管理端菜单 | 每个 AI 任务选择主/备模型 |
| 4 | Prompt 模板 | /admin/ai/prompt-list.html | 管理端菜单 | 模板列表 + 编辑 |
| 5 | 调用日志 | /admin/ai/call-log.html | 管理端菜单 | 调用记录查询 |
| 6 | 调用统计 | /admin/ai/call-stats.html | 管理端菜单 | 按日/周/月统计 |
| 7 | 进化引擎看板 | /admin/ai/evolve-dashboard.html | 管理端菜单 | 进化状态/评分趋势/主题分布 |
| 8 | 进化任务历史 | /admin/ai/evolve-task-list.html | 管理端菜单 | 任务列表+详情+报告查看 |
| 9 | 进化主题管理 | /admin/ai/evolve-topic-list.html | 管理端菜单 | 主题池 CRUD + 批量导入 |
| 10 | 索引队列监控 | /admin/ai/index-queue.html | 管理端菜单 | RabbitMQ 队列状态 + 死信队列 + 失败重试 |

> 注：用户端的 AI 功能嵌入在 shortvideo 和 live 模块的页面中，不单独设 AI 操作页面。

### 4.2 关键页面设计

#### 页面 3：任务-模型配置（核心管理页面）

```
┌────────────────────────────────────────────────────────────────────┐
│ AI 任务-模型配置                                                    │
│ 说明：为每个 AI 任务分配主模型和备用模型，系统按顺序尝试调用         │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ── 文本生成 ──                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ 任务           │ 主模型          │ 备用模型1       │ 备用模型2│  │
│  ├────────────────┼─────────────────┼────────────────┼─────────┤  │
│  │ 短视频文案生成  │ [DeepSeek V3 ▼] │ [Ollama DS  ▼] │ [Qwen3▼]│  │
│  │ 直播开场话术    │ [DeepSeek V3 ▼] │ [Ollama DS  ▼] │ [Qwen3▼]│  │
│  │ 产品讲解话术    │ [DeepSeek V3 ▼] │ [Ollama DS  ▼] │ [Qwen3▼]│  │
│  │ 直播结尾话术    │ [DeepSeek V3 ▼] │ [Ollama DS  ▼] │ [Qwen3▼]│  │
│  │ 视频策划方案    │ [DeepSeek V3 ▼] │ [Ollama DS  ▼] │ [无    ]│  │
│  │ 视频数据分析    │ [DeepSeek V3 ▼] │ [Ollama DS  ▼] │ [无    ]│  │
│  │ 直播数据分析    │ [DeepSeek V3 ▼] │ [Ollama DS  ▼] │ [无    ]│  │
│  │ 违规词替换建议  │ [DeepSeek V3 ▼] │ [Qwen3      ▼] │ [无    ]│  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ── 知识嵌入 ──                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ 知识库嵌入      │ [千问嵌入    ▼] │ [无           ] │ [无    ]│  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ── 进化引擎 ──                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ 知识进化报告    │ [Ollama DS  ▼] │ [DeepSeek V3 ▼] │ [Qwen3▼]│  │
│  │ 进化主题扩展    │ [DeepSeek V3 ▼] │ [Qwen3      ▼] │ [无    ]│  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ── 多媒体生成 ──                                                  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ 分镜图像生成    │ [SDXL 图像  ▼] │ [无           ] │ [无    ]│  │
│  │ 视频片段生成    │ [视频生成V1 ▼] │ [无           ] │ [无    ]│  │
│  │ TTS 语音合成    │ [CosyVoice  ▼] │ [无           ] │ [无    ]│  │
│  │ 视频剪辑合成    │ [视频剪辑V1 ▼] │ [无           ] │ [无    ]│  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  注：下拉框只显示该任务兼容的模型类型                               │
│  （文本任务→generate 类型模型，嵌入任务→embed 模型，以此类推）      │
│                                                                    │
│                                                [保存全部配置]       │
└────────────────────────────────────────────────────────────────────┘
```

#### 页面 1：知识库状态看板

```
┌────────────────────────────────────────────────────────────────┐
│ 知识库管理                                                      │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  │
│  │ 知识文件数 │  │ 已索引数  │  │ Milvus    │  │ ES 文档数  │  │
│  │   8,794   │  │   8,700   │  │ 87,000 向量│  │   87,000  │  │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘  │
│                                                                │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐                  │
│  │ Redis 缓存 │  │ 缓存命中率│  │ 向量库大小 │                  │
│  │  256 MB   │  │   68.5%   │  │   770 MB  │                  │
│  └───────────┘  └───────────┘  └───────────┘                  │
│                                                                │
│  最后索引时间: 2026-02-25 06:00:00                              │
│  自进化状态: ✓ 运行中（每24h自动更新）                           │
│  嵌入模型: BGE-M3 (1024D 稠密+稀疏)                            │
│  重排模型: BGE-reranker-v2 (Cross-Encoder)                     │
│  检索模式: 混合检索（Milvus 语义 + ES 关键词 → RRF → 重排）     │
│                                                                │
│  [全量索引] [增量索引] [健康检查] [清除缓存]                     │
│                                                                │
│  ── Milvus 集合统计 ──                                         │
│  | Collection | 向量数  | 分区数 | 索引类型 | 状态 |            │
│  | kb_knowledge| 87,000 | 3      | HNSW    | 正常 |            │
│                                                                │
│  ── ES 索引统计 ──                                              │
│  | Index       | 文档数  | 存储大小 | 分片数 | 状态 |           │
│  | kb_knowledge| 87,000 | 1.2GB   | 3      | 正常 |           │
│                                                                │
│  ── 知识源列表 ──                                               │
│  | 名称       | 路径           | 文件数 | 状态 |                │
│  | 抖音知识库 | /knowledge/... | 9,048  | 启用 |                │
│  | 项目文档   | /projects/...  | 4      | 启用 |                │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

#### 页面 3：Prompt 模板管理

```
┌────────────────────────────────────────────────────┐
│ Prompt 模板管理                      [新增模板]     │
├────────────────────────────────────────────────────┤
│ [分类筛选: 全部 ▼]  [搜索___]  [搜索]              │
├────────────────────────────────────────────────────┤
│ | 编码 | 名称 | 分类 | 模型 | 版本 | 状态 | 操作   │
│ | video_copywriting | 短视频文案 | 文案 | 默认 | v1 | 启用 | 编辑 测试 │
│ | live_script_opening | 开场话术 | 话术 | 默认 | v1 | 启用 | 编辑 测试 │
│ ...                                                │
├────────────────────────────────────────────────────┤
│ 编辑侧滑窗：                                       │
│ ┌────────────────────────────────────────┐         │
│ │ 模板编码 [______________]              │         │
│ │ 模板名称 [______________]              │         │
│ │ 分类     [文案 ▼]                      │         │
│ │ System Prompt:                         │         │
│ │ [多行文本框___________________]        │         │
│ │ User Prompt:                           │         │
│ │ [多行文本框___________________]        │         │
│ │ 支持变量: {persona} {topic} {style}    │         │
│ │ 温度     [0.75]                        │         │
│ │ 最大Token [2000]                       │         │
│ │          [保存] [测试生成]             │         │
│ └────────────────────────────────────────┘         │
└────────────────────────────────────────────────────┘
```

#### 页面 6：进化引擎看板

```
┌────────────────────────────────────────────────────────────────┐
│ 知识自进化引擎                                 [手动触发进化]   │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  │
│  │ 累计进化   │  │ 平均评分   │  │ 已入库报告 │  │ 主题池数量 │  │
│  │   156 轮  │  │   68.5 分  │  │   142 篇   │  │   487 个  │  │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘  │
│                                                                │
│  ── 最近 7 天评分趋势 ──                                       │
│  100│                                                          │
│   80│  ─── ●── ●── ●── ●── ●── ●                              │
│   60│                                                          │
│   40│                                                          │
│   20│                                                          │
│    0└──┬──┬──┬──┬──┬──┬──                                     │
│        2/18 2/19 2/20 2/21 2/22 2/23 2/24                     │
│                                                                │
│  ── 主题分类分布 ──                                             │
│  直播 ████████████ 62    AI █████████ 45                       │
│  垂类 ████████ 38         商业化 ███████ 35                    │
│  算法 ████████ 42         数据 ████████ 40                     │
│  团队 ██████ 28           合规 ██████ 32                       │
│  ...                                                           │
│                                                                │
│  ── 最近进化任务 ──                                             │
│  | 任务编号 | 时间 | 角度 | 模型 | 评分 | 状态 |              │
│  | evolve_0224_060000 | 2/24 06:00 | 数据量化 | deepseek | 72 | ✓ 已完成 |  │
│  | evolve_0223_060000 | 2/23 06:00 | 失败复盘 | deepseek | 65 | ✓ 已完成 |  │
│  | evolve_0222_060000 | 2/22 06:00 | 工具实操 | ollama   | 45 | ⚠ 低分    |  │
│  ...                                                           │
│                                                                │
│  ── 深化队列 ──                                                │
│  | 主题 | 上次评分 | 优先级 |                                  │
│  | 抖音直播 违规限流 | 23 | P0 |                               │
│  | AI数字人 合规风险 | 18 | P0 |                               │
│                                                                │
│  下次自动进化: 2026-02-25 06:00:00                              │
│  索引队列: 2 pending / 0 failed                                │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 六、开发任务拆解（Task Breakdown）

### 6.1 后端任务

| # | 任务 | 输出 | 依赖 |
|---|------|------|------|
| B1 | 编写 SQL | schema.sql + demo.sql（含默认模型、Prompt、进化主题） | 无 |
| B2 | Entity 层 | AiPromptTemplate, AiModelConfig, AiTaskModelConfig, AiCallLog, AiCallQuota, AiKnowledgeSource, AiEvolveTopic, AiEvolveTask, AiEvolveReport, AiIndexQueue | B1 |
| B3 | Repository 层 | 10 个 Repository 接口 | B2 |
| B4 | VO 层 | GenerateRequestVO, GenerateResultVO, KnowledgeQueryVO, MediaGenerateVO, EvolveStatusVO, EvolveTaskDetailVO, EvolveTopicVO, ScoreDetailVO, TaskModelVO 等 | B2 |
| B5 | AiModelService | 模型注册表管理、模型调用（HTTP 客户端）、3 层回退机制 | B3, B4 |
| B5b | AiTaskModelService | 任务-模型映射管理、按 task_code 查主/备模型 | B3, B5 |
| B6 | AiKnowledgeService | 知识库检索（对接 Milvus + ES + Redis）、索引管理 | B3 |
| B7 | AiPromptService | Prompt 模板 CRUD、变量替换 | B3, B4 |
| B8 | AiQuotaService | 额度检查、额度更新 | B3 |
| B9 | AiGenerateService | 统一文本生成入口（组装 prompt → 调模型 → 记日志） | B5, B6, B7, B8 |
| B10 | AiMediaService | 多媒体生成服务（图像生成/视频生成/TTS/剪辑） | B5, B8, storage |
| **进化引擎** | | | |
| B11 | AiEvolveTopicService | 主题池管理（采样/去重/扩展/深化队列/轮换） | B3, B4 |
| B12 | AiEvolveContextService | 上下文收集（Milvus ANN + ES BM25 → RRF 融合 + 文件读取 + 截断） | B6 |
| B13 | AiEvolvePromptBuilder | Prompt 组装（基础 + 角度 + 黑名单 + 质量提醒） | B7 |
| B14 | AiEvolveScoreCalculator | 质量评分（11 维度打分 + 报告结构修复） | 无 |
| B15 | AiEvolveExpandService | 主题自扩展（从报告提取新主题 + 去重 + 合并） | B5, B11 |
| B16 | AiIndexQueueService | 索引队列管理（入队/出队/重试/超时检测） | B3 |
| B17 | AiIndexWorker | RabbitMQ Consumer（消费索引队列 → 分块 → BGE-M3 嵌入 → Milvus + ES 双写） | B6, B16 |
| B18 | AiEvolveService | 进化管线主服务（编排 B11-B17 各步骤） | B11-B17 |
| B19 | AiEvolveScheduler | 定时调度（@Scheduled 每 24h 触发 AiEvolveService） | B18 |
| **API 层** | | | |
| B20 | AiController | 用户端：文本生成、多媒体生成、知识搜索、额度查询 | B9, B10 |
| B21 | AiAdminController | 管理端：知识库/模型/模板/日志/统计 | B5, B6, B7 |
| B22 | AiEvolveAdminController | 管理端：进化状态/任务列表/主题管理/手动触发/评分趋势/索引队列 | B18, B11, B16 |
| B23 | resource-data.sql | ai 模块菜单/API/按钮资源注册 | B22 |
| **企业级基础设施服务** | | | |
| B24 | AiSearchFusionService | RRF 双路检索融合（Milvus 语义 + ES 关键词 → RRF 权重融合 → 结果合并） | B6 |
| B25 | AiRerankerService | BGE-reranker-v2 Cross-Encoder 重排（Top-20 → Top-K） | B5 |
| B26 | AiCacheService | Redis 缓存管理（查询缓存/嵌入缓存/分布式锁/命中率统计） | B3 |
| B27 | AiMessageQueueService | RabbitMQ 生产/消费管理（索引/进化/多媒体队列 + 死信队列配置） | B3 |
| B28 | AiInfraHealthService | 基础设施健康检查（Milvus/ES/Redis/RabbitMQ 连通性 + 指标采集） | B6, B26, B27 |

### 5.2 前端任务

| # | 任务 | 依赖 |
|---|------|------|
| F1 | 知识库状态看板 | B21 |
| F2 | 模型注册管理页（按类型分组展示 + CRUD） | B21 |
| F3 | 任务-模型配置页（按任务分组 + 主/备模型下拉选择） | B22 |
| F4 | Prompt 模板管理页 | B21 |
| F5 | 调用日志页 | B21 |
| F6 | 调用统计页 | B21 |
| F7 | 进化引擎看板（评分趋势/主题分布/深化队列） | B22 |
| F8 | 进化任务历史页（任务列表 + 报告查看） | B22 |
| F9 | 进化主题管理页（主题池 CRUD + 导入） | B22 |
| F10 | 索引队列监控页 | B22 |

---

## 七、测试用例（Test Cases）

### 7.1 冒烟测试

| # | 场景 | 操作 | 预期 |
|---|------|------|------|
| S1 | AI 文本生成 | POST /ai/generate | 200，返回生成内容 |
| S2 | 知识检索 | POST /ai/knowledge/query | 200，返回知识条目 |
| S3 | 额度查询 | POST /ai/quota/info | 200，返回额度信息 |
| S4 | 模型列表 | POST /ai/admin/model/list | 200，返回模型配置 |
| S5 | AI 图像生成 | POST /ai/media/generate-image | 200，返回图片 URL |
| S6 | AI TTS | POST /ai/media/generate-tts | 200，返回音频 URL |
| S7 | 进化状态 | POST /ai/admin/evolve/status | 200，返回进化引擎状态 |
| S8 | 主题池列表 | POST /ai/admin/evolve/topic/list | 200，返回主题列表 |
| S9 | 手动触发进化 | POST /ai/admin/evolve/trigger | 200，任务创建成功 |

### 6.2 功能测试

| # | 场景 | 预期 |
|---|------|------|
| F01 | 正常生成文案 | 返回生成内容，调用日志记录，额度减 1 |
| F02 | 额度用完后生成 | 返回 4001 错误 |
| F03 | 主模型超时 | 自动切换备用模型，is_fallback=1 |
| F04 | 所有模型不可用 | 返回 4002 错误 |
| F05 | Prompt 模板不存在 | 返回 4003 错误 |
| F06 | 知识库检索缓存 | 相同查询第二次更快（命中 Redis 缓存，验证 cache:kb:query:{hash} 存在） |
| F07 | 触发全量索引 | 索引状态更新，计数正确 |
| F08 | Prompt 变量替换 | 所有占位符正确替换为实际值 |
| F09 | AI 图像生成 | 返回有效图片 URL，图片可访问 |
| F10 | AI 视频生成 | 根据首尾帧生成视频片段 |
| F11 | AI TTS 语音合成 | 文本正确转为语音文件 |
| F12 | AI 视频剪辑 | 多片段+配音合成完整视频 |
| F13 | 多媒体任务状态查询 | 异步任务状态正确返回 |
| **进化引擎** | | |
| F14 | 进化管线完整执行 | 手动触发 → 各阶段状态更新 → completed，报告写入 |
| F15 | 质量评分准确 | 含 3 个章节 + 失败案例 + SOP + 基准 → 评分 ≥ 75 |
| F16 | 低分不入库 | 质量 < 50 的报告 index_status=skipped |
| F17 | 高分入库 | 质量 ≥ 50 的报告推入 RabbitMQ 索引队列，Consumer 消费后 Milvus + ES 双写，最终 indexed |
| F18 | 低分深化队列 | 质量 < 25 的主题 priority 被调高（加入深化队列） |
| F19 | 质量提醒生效 | 上轮 < 50 分，下一轮 had_quality_hint=1 |
| F20 | 主题自扩展 | 进化后 expanded_count > 0，新主题出现在主题池 |
| F21 | 主题去重 | 新主题与现有主题相似度 ≥ 0.75 时不入池 |
| F22 | 主题池上限 | 池已满 1000 时，新主题替换尾部旧主题 |
| F23 | 方法论去重 | 连续 3 轮输出的方法论不重复 |
| F24 | 进化角度多样性 | 连续 11 轮覆盖所有角度 |
| F25 | 3 层 LLM 回退 | 模拟第 1 层失败 → 自动切到第 2 层 → 进化成功 |
| F26 | 429 限流退避 | 模拟 429 → 等待退避后重试成功 |
| F27 | 索引队列消费 | RabbitMQ Consumer 处理消息 → 内容分块 → BGE-M3 嵌入 → Milvus + ES 双写 |
| F28 | 索引超时重置 | processing 超过 10 分钟 → 自动重置为 pending |
| F29 | 管理员主题 CRUD | 新增/编辑/删除主题正确 |
| F30 | 管理员批量导入 | CSV 导入主题，重复跳过 |
| F31 | 评分趋势查询 | 返回最近 N 天的评分趋势数据 |
| **企业级基础设施测试** | | |
| F32 | Milvus 向量写入/检索 | BGE-M3 嵌入写入 Milvus → ANN 检索返回正确结果，cosine 相似度 ≥ 0.6 |
| F33 | ES BM25 检索 | 中文文本写入 ES（IK 分词）→ BM25 检索返回正确结果，关键词命中 |
| F34 | RRF 融合排序 | Milvus 语义结果 + ES 关键词结果 → RRF 融合后排序正确（权重 BM25:1.0, Vector:0.7, k=60） |
| F35 | Cross-Encoder 重排 | BGE-reranker-v2 对 Top-20 结果重排，Top-K 结果相关性优于 RRF 排序 |
| F36 | Redis 缓存命中/失效 | 首次查询写入 Redis 缓存 → 相同查询命中缓存 → TTL 过期后缓存失效 → 重新查询 |
| F37 | RabbitMQ 消息确认 + 死信队列 | 消息正常 ACK → Consumer 处理成功。模拟 3 次失败 → 消息进入死信队列（ai.index.dlq） |
| F38 | BGE-M3 嵌入维度验证 | BGE-M3 输出 1024D 稠密向量 + 稀疏向量，维度与 Milvus Collection schema 匹配 |
| F39 | Milvus + ES 双写一致性 | 写入 1000 条文档 → Milvus 和 ES 文档数一致 → 随机抽样 10 条内容一致 |
| **基础设施降级测试** | | |
| F40 | Redis 不可用降级 | Redis 连接断开 → 检索跳过缓存直接查 Milvus+ES → 结果正确返回（延迟增加但不报错） |
| F41 | Milvus 不可用降级 | Milvus 连接超时 → 自动降级为纯 ES BM25 检索 → searchMode 标记为 keyword |
| F42 | ES 不可用降级 | ES 连接超时 → 自动降级为纯 Milvus 语义检索 → searchMode 标记为 semantic |
| F43 | RabbitMQ 不可用降级 | RabbitMQ 连接断开 → 索引消息降级写入 ai_index_queue 表 → PostgreSQL 队列兜底 |
| F44 | 混合检索权重边界 | bm25Weight=0 → 退化为纯语义检索；vectorWeight=0 → 退化为纯关键词检索 |

### 6.3 边界测试

| # | 场景 | 预期 |
|---|------|------|
| E01 | 超长输入（>5000 字） | 自动截断或报错 |
| E02 | 空 variables | 占位符保留原样或报错 |
| E03 | 并发生成请求 | 额度不超卖 |
| E04 | 模型返回空内容 | 重试一次或返回错误 |

---

## 八、验收标准（Acceptance Criteria）

### 8.1 功能验收

- [ ] AI 可按 Prompt 模板 + 变量生成内容
- [ ] 知识库语义搜索正常返回相关结果
- [ ] 免费用户每日限制 10 次 AI 调用
- [ ] 主模型不可用时自动切换备用模型（3 层回退）
- [ ] 管理员可查看知识库状态和触发索引
- [ ] 管理员可管理模型配置
- [ ] 管理员可 CRUD Prompt 模板
- [ ] 调用日志完整记录
- [ ] AI 可根据分镜描述生成关键帧图片
- [ ] AI 可根据首尾帧生成视频片段
- [ ] AI 可将文本转为语音（TTS）
- [ ] AI 可将视频片段+配音合成最终视频
- [ ] **进化引擎 24h 自动执行，无人值守**
- [ ] **进化报告质量评分正确（11 维度）**
- [ ] **低分报告不入库，高分报告自动索引**
- [ ] **主题池自动扩展，去重有效**
- [ ] **低分主题自动排队深化**
- [ ] **管理员可查看进化状态、任务历史、评分趋势**
- [ ] **管理员可管理主题池（CRUD + 批量导入）**
- [ ] **管理员可手动触发进化**
- [ ] **索引队列正常消费，失败可重试**

### 8.2 技术验收

- [ ] 模型调用有超时控制（默认 30s，进化 360s，多媒体生成可达 600s）
- [ ] 知识检索有缓存（TTL 1h）
- [ ] API Key 加密存储
- [ ] 额度更新是原子操作（防并发超卖）
- [ ] AI 文本响应时间 < 10s（普通生成）
- [ ] 多媒体生成为异步任务，支持状态查询
- [ ] 生成的文件通过 storage 模块上传到云存储
- [ ] **3 层 LLM 回退正常工作，可用性 ≥ 99.5%**
- [ ] **进化管线单轮执行 < 10 分钟**
- [ ] **质量评分计算 < 1s**
- [ ] **索引队列 processing 超时自动重置（10 分钟）**
- [ ] **嵌入 502 错误有退避重试（90s + 1 次重试）**
- [ ] **主题去重相似度计算正确（SequenceMatcher）**
- [ ] **进化历史可追溯（任务记录 + 报告存档 + 评分日志）**
- [ ] **企业级基础设施指标：**
- [ ] Milvus 写入吞吐量 ≥ 100 docs/s
- [ ] 混合检索 P99 < 3s（Phase 1）、< 2s（Phase 2）
- [ ] Redis 缓存命中率 > 50%（Phase 1）、> 70%（Phase 2）
- [ ] BGE-M3 嵌入延迟 ≤ 500ms/batch
- [ ] RabbitMQ 消息处理成功率 ≥ 99.9%
- [ ] Milvus + ES 双写一致性 ≥ 99%

---

## 九、上线检查清单（Release Checklist）

- [ ] `sql/ai/schema.sql` 已执行（含进化引擎 4 张表）
- [ ] `sql/ai/demo.sql` 已执行（默认模型配置 + Prompt 模板 + 10 个初始进化主题）
- [ ] `sql/ai/resource-data.sql` 已执行
- [ ] Milvus 2.4+ 集群已部署（kb_knowledge Collection 已创建，向量维度 1024，cosine 相似度，HNSW 索引）
- [ ] Elasticsearch 8.x 已部署（IK 分词插件已安装，kb_knowledge 索引已创建）
- [ ] Redis 7 已部署（缓存策略 TTL 已配置：query 3600s / embed 7200s / lock 600s）
- [ ] RabbitMQ 已部署（ai.index / ai.evolve / ai.media 队列 + 死信队列 ai.*.dlq 已配置）
- [ ] BGE-M3 嵌入模型已部署（Ollama 或独立推理服务，1024D 稠密+稀疏向量）
- [ ] BGE-reranker-v2 重排模型已部署（Ollama 或独立推理服务，Cross-Encoder）
- [ ] 文本生成模型 API 已配置（DeepSeek API Key 或 Ollama 本地模型）
- [ ] 图像生成服务已部署并配置
- [ ] 视频生成服务已部署并配置
- [ ] TTS 语音合成服务已部署并配置
- [ ] 视频剪辑服务已部署并配置
- [ ] 云存储已配置（用于多媒体资产）
- [ ] 知识库已完成首次全量索引
- [ ] **进化定时任务已启用（@Scheduled cron 配置正确）**
- [ ] **进化 LLM 3 层回退配置正确（Ollama + DeepSeek API + 备用）**
- [ ] **索引 Worker 已启动（消费 ai_index_queue）**
- [ ] **RabbitMQ Consumer 进程已启动（监听 ai.index / ai.evolve / ai.media 队列）**
- [ ] **初始进化主题池已导入（10 个基础主题）**
- [ ] **手动触发进化测试通过（完整管线跑通）**
- [ ] 错误码 4001-4013 已在 ErrorCode.java 中
- [ ] 对应角色已绑定 ai 模块资源（含进化管理菜单）
- [ ] 冒烟测试 S1-S9 全部通过
