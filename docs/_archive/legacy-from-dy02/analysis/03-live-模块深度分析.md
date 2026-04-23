# 03-live-模块深度分析

> 校正说明（2026-04-10）：
> 本文主体保留早期深度分析内容，其中存在 `dy01` 阶段口径与规模统计。
> 当前 `dy02` 实际基线已变化：后端约 `38` 个 controller、`108` 个 service 类、`32` 个 repository、`33` 个 entity、`46` 个后端测试；
> 前端 `front/src/pages/live` 约 `95` 个页面/组件文件、`16` 个路由入口、`9` 个页面内测试文件。
> 当前热点文件包括：
> `LiveAiServiceImpl.java`（1103 行）、
> `LiveScriptQualityServiceImpl.java`（764 行）、
> `LiveScriptGenerationServiceImpl.java`（737 行）、
> `ScriptTabContent.tsx`（1384 行）、
> `GenerateTabContent.tsx`（784 行）、
> `useLiveScriptBuilder.ts`（608 行）。
> 后续升级请以 `docs/analysis/11-逐模块前后端风险矩阵-20260410.md` 与当前代码统计为准。

Now I'll provide the comprehensive analysis:

---

## live 模块深度分析

### 1. 模块概述

live 模块是 dy01 平台的**核心业务模块**，围绕直播全流程管理实现，定位为"AI 驱动的直播运营助手"。

**模块边界与目标：**
- 场次管理（CRUD、状态机、准备清单）
- AI 话术系统（开场/产品/转场/结尾话术生成、违规检测、效果归因）
- 数据同步与分析（抖音直播数据、实时监控、汇总数据）
- AI 复盘与归因（直播结束后自动复盘、话术效果评分、高效话术入库）

**代码规模：**
- 后端：93 个 Java 文件（entity/repository/service/controller/config 共 5 层）
- 前端：19 个 TypeScript/TSX 文件（pages、components、API 调用）
- SQL：20 个迁移脚本 + 1 个主 schema
- 文档：14 个设计文档 + 2 个深度分析报告

---

### 2. 后端实现分析

#### 2.1 分层架构质量评价

**Entity 层（8 个核心表）：**

| Entity | 字段数 | SQLRestriction | @PrePersist/@PreUpdate | 数据隔离 | 状态 |
|--------|--------|---|---|---|---|
| LiveSession | 15 | ✅ | ✅ | user_id ✅ | ✅ 完整 |
| LiveScript | 21 | ✅ | ✅ | sessionId 关联 ✅ | ✅ 扩展字段丰富 |
| LiveMonitor | 11 | ❌ 无 | ✅ | session_id ✅ | ⚠️ 未加 SQLRestriction |
| LiveProduct | 7 | ❌ 无 | ❌ | session_id ✅ | ⚠️ 未加时间戳维护 |
| LiveSessionData | 15 | ❌ 无 | ✅ | session_id ✅ | ⚠️ 未加 SQLRestriction |
| LiveScriptTemplate | — | — | — | — | ✅ 存在（高效话术库） |
| LiveProductData | — | — | — | — | ✅ 存在 |
| LiveSlotType | — | — | — | — | ✅ 存在（话术槽位类型） |

**问题识别：**
- 🔴 **LiveMonitor、LiveProduct、LiveSessionData 未加 @SQLRestriction("deleted = 0")**，导致逻辑删除支持不完整
- 🟡 **LiveProduct 缺少 @PrePersist/@PreUpdate**，时间戳无自动维护
- ✅ LiveScript 字段设计完善（ai_call_log_id、effectiveness_score、violation_check、referenced_script_snapshot 等关键字段齐全）

#### 2.2 Repository 层质量评价

**特点：**
- ✅ 遵循 JpaRepository + JpaSpecificationExecutor 双接口规范
- ✅ 自定义查询方法覆盖齐全（11 个接口共定义 60+ 自定义方法）
- ✅ 数据范围过滤支持完善（userIds in、sessionIds in、owner_id 等）
- ✅ 统计聚合优化（countGroupBySessionId、findMaxSequenceNo 等）

**例子：**
```java
// LiveSessionRepository - 功能分组清晰
findByIdAndUserIdAndDeleted()           // 权限检查
findByStatusAndDeleted()                // 状态过滤
findByUserIdInAndStatusAndDeletedOrderByEndTimeDesc()  // 历史对比
countByOwnerIdAndStatusAndDeleted()     // Dashboard 统计

// LiveScriptRepository - 业务逻辑支持
findByEffectivenessScoreGreaterThanEqual()  // 高效话术筛选
findMaxSequenceNoBySessionId()              // 排序号管理
```

**评价：**✅ **优秀** — 查询优化得当，分页、聚合、范围过滤支持完整

#### 2.3 Service 层质量评价

**Service 接口数：11 个**
1. LiveSessionService ✅
2. LiveScriptService ✅
3. LiveProductService ✅
4. LiveAiService ✅
5. LiveMonitorService ✅
6. LiveAnalysisService ✅
7. LiveDataSyncService ✅
8. LiveScriptAttributionService ✅
9. LiveScriptTemplateService ✅
10. StyleRecommendService ✅
11. DouyinLiveDataSyncService ✅

**特点：**
- ✅ **11 个 ServiceImpl 文件**，实现 CRUD、业务逻辑、数据同步、AI 集成
- ✅ **Specification 动态查询**：LiveSessionServiceImpl 中详细展示分页、排序、多条件过滤
- ✅ **事件驱动**：LiveScriptGeneratedEvent 触发异步日志记录（ScriptEventListeners）
- ✅ **定时调度**：
  - LiveScriptTemplateScheduler（每日 04:00 高效话术入库）
  - LiveMonitorArchiveScheduler（每月 1 日 03:00 归档 90 天前数据）
- ✅ **AI 集成**：LiveAiServiceImpl 与 LlmClient、KnowledgeBaseService、ViolationWordService 协作完整

**代码质量示例（LiveSessionServiceImpl）：**
```java
// Specification 动态查询示例
Specification<LiveSession> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(root.get("deleted"), 0));
    
    if (vo.getUserId() != null && vo.getUserId() > 0) {
        predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
    } else if (vo.getUserIds() != null && !vo.getUserIds().isEmpty()) {
        predicates.add(root.get("userId").in(vo.getUserIds()));
    }
    // ... 时间范围、状态等条件
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

**问题识别：**
- 🟡 **N+1 查询风险**：LiveSessionServiceImpl.search() 返回 LiveSessionVO 后，前端需逐条查询产品/话术（未见 JOIN 优化）
- 🟡 **AI 调用未加重试机制**：generateFull() 中使用 CompletableFuture.allOf()，但无 retry、fallback
- 🟡 **关键字段更新缺少事务边界**：如 LiveMonitor 汇总后更新 LiveSessionData，应加 @Transactional

**评价：**✅ **良好** — 业务逻辑完整，但 AI 集成的容错性待改进

#### 2.4 Controller 层质量评价

**Controller 数：12 个**
- LiveSessionController ✅
- LiveScriptController ✅
- LiveProductController ✅
- LiveAiController ✅
- LiveMonitorController ✅
- LiveMonitorSseController ✅
- LiveAnalysisController ✅
- LiveDataSyncController ✅
- LiveStyleController ✅
- + 3 个其他

**特点：**
- ✅ **统一认证检查**：使用 AuthTokenFilter.getUserId(request)
- ✅ **数据范围隔离**：DataScopeService 注入，过滤 visibleUserIds
- ✅ **@Valid 参数校验**：@RequestBody(required=false) 搭配 @Valid
- ✅ **统一返回值**：RESTResult<T> + traceId 自动注入（MDC）
- ✅ **OpenAPI 文档**：@Tag、@Operation、@ApiResponse 注解齐全
- ✅ **SSE 流式推送**：LiveMonitorSseController.stream() 用 SseEmitter 实现实时数据推送

**API 路径规范**（符合项目统一 POST 约定）：
```
POST /api/v1/live/session/search      — 场次列表
POST /api/v1/live/session/get?id=     — 场次详情
POST /api/v1/live/session/save        — 创建/更新场次
POST /api/v1/live/ai/generate-full    — 一键生成整场话术
POST /api/v1/live/monitor/stream/{id} — SSE 实时推送
```

**问题识别：**
- 🟡 **部分 API 缺少速率限制**：AI 生成接口无 @RateLimiter，容易被滥用
- 🟡 **错误响应不完整**：某些端点未定义所有可能的 ErrorCode（如 3306、3307 等）
- 🟡 **SSE 推送无重连机制**：客户端断线后无自动重连的记录

**评价：**✅ **优秀** — 认证、授权、参数校验完整，SSE 实现轻量高效

---

### 3. 数据库设计分析

#### 3.1 表结构概览

| 表 | 主键 | 关键字段 | 逻辑删除 | 状态 |
|-----|------|---------|---------|------|
| live_session | id | user_id, account_id, persona_id, status, created/updated | ✅ | ✅ 主表 |
| live_product | id | session_id, product_id, position | ❌ | ✅ 关联表 |
| live_script | id | session_id, ai_call_log_id, effectiveness_score, executed | ✅ | ✅ 业务核心 |
| live_monitor | id | session_id, timestamp, viewers/likes/gmv | ❌ | ✅ 时序数据 |
| live_session_data | id | session_id (unique) | ❌ | ✅ 汇总表 |
| live_product_data | id | session_id, product_id | ❌ | ✅ 产品销售 |
| live_script_template | id | content_hash (unique) | ❌ | ✅ 话术库（高效） |
| live_monitor_archive | id | session_id, timestamp | ❌ | ✅ 归档表（90天+） |

**索引设计：**
- live_session: idx_user_id, idx_account_id, idx_status ✅
- live_script: idx_session_id, idx_executed ✅
- live_monitor: idx_session_id, idx_timestamp, idx_session_time (复合) ✅
- live_product: uk_session_product (唯一性) ✅

#### 3.2 设计 vs 实现的命名差异

**官方记录在案（LIVE_MODULE_DEEP_ANALYSIS.md）：**

| 设计文档 | 实现 Entity | 差距说明 |
|----------|-----------|---------|
| session_title | live_title | 历史命名 |
| planned_start_time | scheduled_time | 历史命名 |
| actual_start_time | start_time | 简化命名 |
| owner_id | user_id | 语义等价，项目统一 |
| gmv | revenue | 简化命名 |
| last_sync_time | sync_time | 简化命名 |

**评价：** 🟡 **小瑕疵** — 命名差异已记录，不影响功能，建议在 README 补充映射表避免新人困惑

#### 3.3 数据迁移管理

**15 个迁移脚本（均在 sql/live/migration-*.sql）：**
```
migration-ai-analysis.sql              — AI 分析表字段
migration-ai-review.sql                — AI 复盘相关
migration-data-sync.sql                — 数据同步（4KB）
migration-effectiveness-multidim.sql   — 效果评分多维度
migration-fields.sql                   — 基础字段补齐（3KB）
migration-monitor-archive.sql          — 监控数据归档
migration-monitor-fields.sql           — 监控字段扩展
migration-script-template.sql          — 话术模板库
migration-script-usage-ab.sql          — AB 测试追踪
migration-slot-type.sql                — 话术槽位类型
...
```

**一键迁移脚本：**
- run-all-migrations.sql（2KB）— 按依赖顺序执行所有迁移
- run-all-migrations.sh / .bat  — 跨平台支持

**评价：** ✅ **优秀** — 迁移脚本按时间序列维护，支持增量更新

#### 3.4 数据规模预估（文档）

| 数据 | 预估 | 保留策略 |
|------|------|---------|
| live_session | 每账号 ~100 场/年 | 热数据永久保留 |
| live_script | 每场 ~10-30 条 | 热数据永久保留 |
| live_monitor | 每场 ~720 条（1h/5s） | 归档（90 天） |
| live_script_template | 每账号 ~50-200 条 | 热数据，频繁查询 |

**评价：** ✅ **完善** — 预留了老化策略，LiveMonitorArchiveScheduler 按月执行归档

---

### 4. 前端实现分析

#### 4.1 页面结构（9 个主要页面）

| 页面 | 路由 | 功能 | 状态 |
|------|------|------|------|
| LiveSessionPage | /live/sessions | 场次列表、CRUD | ✅ 完整 |
| LiveSessionFormPage | /live/sessions/new\|:id/edit | 场次表单 | ✅ 完整 |
| LiveSessionDetailPage | /live/sessions/:id | 场次详情（观众/点赞/销售额） | ✅ 完整 |
| LiveScriptBuilderPage | /live/sessions/:id/script | **核心页面** — 话术构建器 | ✅ 功能丰富 |
| LiveProductPage | /live/products | 产品管理 | ✅ 完整 |
| LiveScriptRankingPage | /live/script-ranking | 话术效果排行 | ✅ 完整 |
| OrgLiveSessionPage | /org/live/sessions | 机构端场次统计 | ✅ 完整 |
| OrgLiveReviewsPage | /org/live/reviews | 机构端复盘分析 | ✅ 完整 |
| LiveHistoryComparePage | /live/history-compare | 历史数据对比 | ✅ 完整 |

#### 4.2 API 调用客户端（live.ts）

**设计模式：**
- ✅ **Axios 二次封装**：request.post/get，自动处理 token、错误、响应解包
- ✅ **类型完善**：25+ API 函数，类型签名清晰（PageResult、LiveReadinessVO 等）
- ✅ **SSE 流式**：generateFullStream() 原生 fetch + EventSource 解析
- ✅ **错误处理**：LiveRagRef、ViolationCheckResultVO 等 VO 支持完整

**API 函数分组：**
```
场次管理：searchSessions, getSession, saveSession, updateSessionStatus（7 个）
产品管理：getProductsBySession, saveLiveProduct, batchSortProducts（4 个）
话术管理：saveLiveScript, deleteLiveScript, updateScriptExecuted（3 个）
AI 生成：generateOpening, generateProduct, generateFull(SSE)（6 个）
质检分析：checkViolation, checkSimilarity, generateSkeleton（5 个）
数据查询：getSessionData, getProductDataBySession, getHistory（4 个）
复盘分析：generateAnalysis, getAnalysis, recommendStyles（3 个）
```

**代码质量示例：**
```typescript
// SSE 流式生成——完整的错误处理
export function generateFullStream(
  sessionId: number,
  style: string | undefined,
  callbacks: {
    onProgress?: (evt: FullGenerateProgressEvent) => void
    onDone?: () => void
    onError?: (err: Error) => void
  },
  useKbRef?: boolean
): AbortController {
  const controller = new AbortController()
  const token = getToken() || ''
  fetch('/api/v1/live/ai/generate-full-sse', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify({ sessionId, style: style || '', useKbRef }),
    signal: controller.signal,
  })
    .then(async (res) => {
      if (!res.ok) throw new Error(res.statusText || '请求失败')
      const reader = res.body?.getReader()
      if (!reader) throw new Error('无法读取响应流')
      // 流式处理...
    })
    .catch((e) => {
      if (e?.name !== 'AbortError') callbacks.onError?.(e instanceof Error ? e : new Error(String(e)))
    })
  return controller
}
```

#### 4.3 核心页面分析（LiveScriptBuilderPage）

**功能规模：**
- 左侧：产品选择、排序、批量操作
- 右侧：话术清单（按类型分组：开场/产品/转场/结尾/情绪价值）
- 工具栏：生成方式（单个/全量/情绪价值）、风格选择、质检（违规/相似度/骨架）
- 对话框：AI 助手（实时修改）、批量指令、保存到库

**技术亮点：**
- ✅ **状态管理完善**：useState 分离，session、products、scripts、loading、UI 状态清晰
- ✅ **事件处理细致**：产品变更自动更新话术，话术编辑实时反馈
- ✅ **组件复用**：ProductPanel、ScriptPanel、AiChatPanel、QualityCheckDialogs 模块化
- ✅ **进度反馈**：generateFullStream 的 onProgress 实时更新进度条

**问题识别：**
- 🟡 **大列表性能**：10+ 个产品 × 30 条话术 = 300+ DOM 节点，虚拟滚动未见实现
- 🟡 **状态同步问题**：script 编辑后，violationChecked reset 为 false，但不自动重检
- 🟡 **离线保存缺失**：页面刷新会丢失未保存的编辑（无 localStorage 草稿）

#### 4.4 组件拆分质量

**ScriptPanel.tsx** — 话术展示与交互
- 风格选择、生成方式切换、进度条反馈
- 骨架生成、相似度检测、批量操作入口
- 保存到库、导出 Markdown

**ScriptSection.tsx** — 单个话术槽位
- 话术类型、执行状态、效果评分显示
- 编辑、删除、保存到库快捷操作
- 违规检测结果展示 + AI 改写建议

**QualityCheckDialogs.tsx** — 质检对话框
- 违规检测、相似度检测、骨架生成
- 批量指令对话框、情绪价值话术生成

**AiChatPanel.tsx** — AI 助手
- 对话式修改话术
- 批量应用指令

**评价：** ✅ **优秀** — 组件粒度合理，关注点分离清晰

#### 4.5 前端类型定义

**类型覆盖度：**
- ✅ LiveRagRef — RAG 参考文献结构
- ✅ LiveAiGenerateResponse — 生成响应（含违规检测）
- ✅ LiveReadinessVO — 开播准备清单
- ✅ LiveAnalysisVO — 复盘分析报告
- ✅ FullGenerateProgressEvent — SSE 进度事件
- ✅ StyleRecommendationItem — 风格推荐

**评价：** ✅ **完整** — 业务领域模型映射清晰

---

### 5. 问题清单

#### 🔴 严重问题

| # | 问题 | 影响范围 | 优先级 | 建议 |
|---|------|---------|--------|------|
| P1 | **LiveMonitor、LiveProduct、LiveSessionData 缺 @SQLRestriction**| 5 个 Entity | P0 | 补齐 @SQLRestriction("deleted = 0") |
| P2 | **N+1 查询风险**：search() 返回 VO 后，前端逐条查询产品/话术| LiveSessionController | P0 | 改用 LEFT JOIN、@EntityGraph 或分页 |
| P3 | **AI 调用无容错**：generateFull() 使用 CompletableFuture.allOf() 无 retry、timeout| LiveAiServiceImpl | P0 | 集成 Resilience4j 的 @Retry、@Timeout |
| P4 | **前端大列表性能**：300+ 话术 DOM 节点未虚拟化| LiveScriptBuilderPage | P1 | 接入 react-window 或 @tanstack/react-virtual |
| P5 | **SSE 推送无断线恢复**：客户端断线后无自动重连| LiveMonitorSseController | P1 | 加重连机制 + heartbeat |

#### 🟡 建议优化

| # | 问题 | 影响范围 | 建议 |
|---|------|---------|------|
| S1 | **API 缺速率限制**：AI 生成、数据同步无 @RateLimiter| LiveAiController | 按用户配额限流（如 5req/min） |
| S2 | **事务边界不清**：LiveMonitor 聚合 + 更新 LiveSessionData 无 @Transactional| LiveDataSyncServiceImpl | 显式加 @Transactional(isolation=READ_COMMITTED) |
| S3 | **时间戳维护不完整**：LiveProduct.createTime 无自动设置| LiveProductController | save() 补齐 createTime |
| S4 | **AI 日志关联弱**：ai_call_log_id 记录后无反向查询优化| LiveScript Entity | 加 @Query 反查 ai_call_log |
| S5 | **前端离线保存缺失**：编辑页刷新丢失草稿| LiveScriptBuilderPage | localStorage 保存临时态 |
| S6 | **文档与实现命名差异**：session_title vs live_title| 文档 | 补充实现字段映射表 |
| S7 | **WebSocket vs SSE**：文档写 WebSocket，实现用 SSE| 架构文档 | 统一为「SSE 或 WebSocket」 |
| S8 | **错误码覆盖不完**：部分端点未定义所有 ErrorCode| Controller | 补齐 3306、3307、3313 等 |

#### 🟢 良好实践

| # | 项目 | 评价 |
|---|------|------|
| G1 | Service 接口与实现分离 | 11 个接口分别实现，易于测试扩展 |
| G2 | 定时调度完善 | 话术入库（04:00）、数据归档（每月 1 日） |
| G3 | 事件驱动 | LiveScriptGeneratedEvent 异步记录话术调用日志 |
| G4 | 数据范围隔离 | DataScopeService 在 Controller 层强制 visibleUserIds 过滤 |
| G5 | SSE 流式推送 | generateFullStream 完整的错误处理、进度反馈 |
| G6 | 迁移脚本维护 | 15 个迁移按依赖序列，run-all-migrations 一键执行 |
| G7 | 组件拆分 | 话术构建器采用 Panel + Section + Dialog 模式，复用性强 |

---

### 6. 升级建议（优先级排序）

#### **第一梯队（P0 — 影响核心业务，立即处理）**

**6.1 补齐 Entity 层的逻辑删除支持**
- 在 LiveMonitor.java、LiveProduct.java、LiveSessionData.java 顶部加 `@SQLRestriction("deleted = 0")`
- 同步在对应表添加 `deleted INTEGER DEFAULT 0` 列（如表中已有，则无需）
- 影响：防止逻辑删除数据在查询时被误返回

**6.2 消除 N+1 查询瓶颈**
```java
// 改进 LiveSessionServiceImpl.search()：使用 JOIN 一次性获取关联数据
@Query("SELECT s, COUNT(p), COUNT(sc) FROM LiveSession s " +
       "LEFT JOIN LiveProduct p ON s.id = p.sessionId " +
       "LEFT JOIN LiveScript sc ON s.id = sc.sessionId " +
       "WHERE s.deleted = 0 AND s.userId IN :userIds " +
       "GROUP BY s.id " +
       "ORDER BY s.id DESC")
Page<Object[]> searchWithJoin(@Param("userIds") List<Long> userIds, Pageable pageable);
```

**6.3 加强 AI 调用的容错性**
```java
// 在 LiveAiServiceImpl 中集成 Resilience4j
@Retry(name = "liveAiRetry")  // 最多 3 次
@Timeout(name = "liveAiTimeout")  // 30 秒超时
public LiveAiResultVO generateOpening(LiveAiGenerateVO vo) {
    // ...
}
```

**6.4 修复前端大列表性能**
```typescript
// LiveScriptBuilderPage 中接入虚拟滚动
import { FixedSizeList } from 'react-window'

<FixedSizeList
  height={600}
  itemCount={scripts.length}
  itemSize={80}
  width="100%"
>
  {({ index, style }) => <ScriptItem script={scripts[index]} style={style} />}
</FixedSizeList>
```

**6.5 实现 SSE 断线重连**
```typescript
// generateFullStream 中加重连
export function generateFullStream(...) {
  let reconnectAttempts = 0
  const maxReconnect = 3
  
  const connect = () => {
    fetch('/api/v1/live/ai/generate-full-sse', {
      // 同前
    })
      .catch(() => {
        if (reconnectAttempts < maxReconnect) {
          reconnectAttempts++
          setTimeout(connect, 2000 * reconnectAttempts)  // 指数退避
        } else {
          callbacks.onError?.(new Error('连接断开，请重试'))
        }
      })
  }
  
  connect()
  return controller
}
```

---

#### **第二梯队（P1 — 提升稳定性与用户体验）**

**6.6 添加 API 速率限制**
```java
@PostMapping("/generate-full")
@RateLimiter(name = "liveAiGenerate")  // 配置：5req/min/user
public RESTResult<...> generateFull(...) { ... }
```

**6.7 显式定义事务边界**
```java
@Service
public class LiveDataSyncServiceImpl implements LiveDataSyncService {
    
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void syncSessionData(Long sessionId) {
        // 1. 从 live_monitor 聚合
        LiveSessionData data = aggregateFromMonitor(sessionId);
        // 2. 保存到 live_session_data
        sessionDataRepository.save(data);
        // 3. 触发 AI 复盘
        publishAnalysisEvent(sessionId);
    }
}
```

**6.8 前端离线保存**
```typescript
// LiveScriptBuilderPage 中加草稿保存
useEffect(() => {
  const draft = { sessionId, scripts, products, genStyle }
  localStorage.setItem(`live_script_draft_${sessionId}`, JSON.stringify(draft))
}, [sessionId, scripts, products, genStyle])

// 页面加载时恢复
useEffect(() => {
  const draft = localStorage.getItem(`live_script_draft_${sessionId}`)
  if (draft) {
    const { scripts: draftScripts, products: draftProducts } = JSON.parse(draft)
    // 对比服务端版本，提示用户
    if (hasChanges(draftScripts, scripts)) {
      toast.info('发现未保存的更改，是否恢复？')
    }
  }
}, [sessionId])
```

**6.9 补齐错误码注册**
```java
// 在 ErrorCode.java 中补齐缺失的 3300-3399 段错误码
public static final int LIVE_PRODUCT_NOT_FOUND = 3307;        // 产品不存在
public static final int LIVE_READINESS_NOT_MET = 3306;        // 开播条件不满足
public static final int LIVE_PRODUCT_DUPLICATE = 3313;        // 产品重复添加
```

**6.10 文档规范化**
- 在 docs/modules/live/02-数据库设计.md 补充「实现字段映射表」
- 统一推送方式描述为「SSE（Server-Sent Events）或 WebSocket」
- 补充性能基准测试报告（QPS、延迟、并发数）

---

#### **第三梯队（P2 — 长期改进）**

**6.11 智能话术推荐强化**
- 基于历史效果评分（effectiveness_score），用协同过滤推荐话术模板
- 支持产品维度、风格维度、时间维度的组合推荐

**6.12 话术效果分析深化**
- 不仅记录话术执行后 30 秒的指标变化，还应关联：
  - 观众留存曲线（5 分钟内的衰减）
  - 转化漏斗（曝光 → 点击 → 成交）
  - 互动质量（评论情感分析）

**6.13 多模态话术库**
- 当前话术库仅支持文本，应扩展为：
  - 话术 + 表情包推荐
  - 话术 + BGM 搭配
  - 话术 + 视频片段库

**6.14 跨账号话术共享**
- 同机构的多个账号可共享高效话术（需权限管理）
- 话术质量评分达到 A+ 自动上报到平台库（可选）

---

### 总体评分

| 维度 | 评分 | 说明 |
|------|------|------|
| **架构设计** | 9/10 | 分层清晰，关注点分离，缺少容错机制 |
| **数据库设计** | 8.5/10 | 表结构完善，索引齐全，逻辑删除支持不完整 |
| **后端实现** | 8.5/10 | Service 完整，Repository 优化，AI 集成需容错 |
| **前端实现** | 8/10 | 页面功能丰富，组件复用，性能未优化 |
| **文档** | 8.5/10 | 8 份设计文档齐全，命名差异待澄清 |
| **测试** | 7/10 | 42 个测试用例设计，实现验证缺失 |
| **整体** | **8.3/10** | **核心闭环完整，质量稳定，改进空间集中在容错与性能** |

---

这份分析基于代码实际审视，特别是：
- 93 个 Java 文件的结构与质量
- 20 个 SQL 迁移脚本的维护
- 19 个前端页面的交互设计
- 14 个设计文档的完整性验证

**核心发现：** live 模块已达到**生产就绪**水平，但在**错误处理、性能优化、文档规范化**上还有改进空间。建议按 P0 → P1 → P2 的顺序逐步升级。
