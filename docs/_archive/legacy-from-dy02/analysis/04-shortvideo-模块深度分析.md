# 04-shortvideo-模块深度分析

> 校正说明（2026-04-10）：
> 本文主体保留早期深度分析内容，其中部分规模统计仍是 `dy01` 阶段口径。
> 当前 `dy02` 实际基线已变化：后端约 `35` 个 controller、`91` 个 service 类、`40` 个 repository、`41` 个 entity、`47` 个后端测试；
> 前端 `front/src/pages/shortvideo` 约 `44` 个页面/组件文件、`28` 个路由入口，页面内测试仍接近空白，属于 P0 升级模块。
> 当前短视频前端已存在少量纯函数测试入口，可优先从工具层和 API 层扩展，再进入工作台、素材、发布链页面级测试。
> 后续升级请以 `docs/analysis/11-逐模块前后端风险矩阵-20260410.md` 与当前代码统计为准。

现在我有足够的信息来生成深度分析报告。

## shortvideo 模块深度分析

### 1. 模块概述

**模块大小：** 143 个 Java 文件，4421 行代码（Service Impl）| 19 个 SQL 文件 | 16 个前端页面组件

**核心定位：** 短视频全生命周期管理（数据同步 → 爆款发现 → AI 创作 → 视频制作 → 发布管理 → 复盘归因）

**技术栈：**
- 后端：44 个 Service/Controller + JPA Specification 动态查询 + RabbitMQ 异步任务
- 前端：React 18 + MUI 6 + TanStack Query + 70+ useQuery/useMutation 调用
- 数据库：PostgreSQL 15+ | 12 张核心表 + 19 个 SQL 迁移脚本

**文档完整性：** 12 份设计文档（00-大纲 ~ 09-数据模型说明）| 需求 30 条 | 接口 58 个 | 用户故事 28 个

---

### 2. 后端实现分析

#### 2.1 Entity 层设计（14 个核心实体）

**优点：**
- ✅ 规范使用 `@SQLRestriction("deleted = 0")` 逻辑删除
- ✅ 统一 `@PrePersist` / `@PreUpdate` 时间戳自动维护
- ✅ `owner_id` + `account_id` 二维数据隔离
- ✅ 支持 JSON 字段存储（tags、metadata、reference_images）
- ✅ 关键字段注释完整（SvProject, SvVideo, SvScript）

**关键实体覆盖：**
- SvProject（项目）、SvVideo（视频）、SvScript（脚本）
- SvPlan（创作方案）、SvPlanAsset（素材版本管理）
- SvViralVideo（爆款库）、SvHotTopic（热点话题）
- SvComment（评论+情感分析）、SvVideoData（每日数据快照）
- SvDramaEpisode / SvDramaCharacter（短剧创作）
- SvCinematicPreset（AI 预设模板）、SvGenerationLog（生成日志）

**缺陷：**
- 🔴 **SvProject 字段过多（22 个）** — 混合了多个关注点：拍摄状态、审核状态、发布状态，应该拆成独立关联表
- 🔴 **缺少 version 字段** — SvProject、SvScript、SvPlan 无乐观锁支持，高并发修改存在覆盖风险
- 🟡 **SvVideoData 缺少索引** — 只有 `(video_id, snapshot_date)` 和 `(snapshot_date)`，无 `view_delta` 降序索引，dashboard 趋势查询性能堪忧

#### 2.2 Repository 层（15 个仓库）

**优点：**
- ✅ JpaSpecificationExecutor 动态查询，分页参数校验
- ✅ 自定义 @Query 避免 N+1（如 `SvGenerationLogRepository` 的分组聚合）
- ✅ 查询方法命名规范（findByOwnerIdAndDeleted 一致风格）
- ✅ 关键字段有 UNIQUE 索引（douyin_video_id, douyin_comment_id）

**缺陷：**
- 🔴 **SvProjectRepository 缺少核心查询** — 无按 status+createTime 的复合查询，SvProjectServiceImpl 第 60 行 validateParams() 被调用了两次（第 37 和 60 行）
- 🔴 **SvVideoRepository.findViewCountsByAccountId()** — 仅返回 List\<Long\>，前端无法获取 video_id，计算均值时全量加载（应该用原生 SQL 聚合）
- 🟡 **SvCommentRepository 无关键词全文搜索** — content TEXT 字段无倒排索引支持

#### 2.3 Service 层（22 个 Service Impl）

**核心实现类：**

| Service | 代码行数 | 质量评估 |
|---------|--------|--------|
| SvVideoServiceImpl | 178 | 良好（Specification 完整，owner_id 强制过滤） |
| SvProjectServiceImpl | 100+ | 良好（search/get/save/delete 完整） |
| ShortVideoDashboardServiceImpl | 80 | 警告（暂无真实数据源，返回占位数据）|
| PublishTimeRecommendationServiceImpl | 75 | 良好（双重查询降级） |
| ShortVideoQuickServiceImpl | 72 | 良好（流程编排清晰，三级联创） |
| ContentCalendarServiceImpl | 复杂 | ⚠️ 待评估 |
| ContentAuditServiceImpl | 极少 | 🔴 **3 个 TODO 未实现** |

**关键问题：**

- 🔴 **ContentAuditServiceImpl 不完整** — 3 个 TODO 注释（图像审核、视频审核、文本审核），均未实现，只有空方法
- 🔴 **SvProjectServiceImpl 第 60 行代码重复** — `vo.validateParams()` 被调用两次（第 37 和 60 行）
- 🔴 **ShortVideoDashboardServiceImpl 数据造假** — `getTrend()` 返回硬编码零值，`getStats()` 无真实数据源
- 🟡 **缺少 @Transactional 保证** — save 操作（如 quickGenerate 创建 project/script/shotList 三级联创）无显式事务管理，一级失败不会回滚后续操作
- 🟡 **N+1 潜在风险** — PublishTimeRecommendationServiceImpl 第 40 行两次查询可以合并，多数 Service 按 ownerIds 查询后无分页保护

#### 2.4 Controller 层（13 个控制器）

**路径规范：** `/api/v1/short-video/*`  
**认证方式：** Bearer Token + DataScopeService 数据隔离  
**响应格式：** RESTResult<T> 统一体（status/message/data/traceId）

**优点：**
- ✅ 所有 API 都有 owner_id / visibleOwnerIds 权限检查
- ✅ @Valid 校验 VO 入参
- ✅ 自动注入 traceId 用于链路追踪

**缺陷：**
- 🟡 **Controller 代码重复** — ShortVideoProjectController 的 list/get/save/delete 4 个方法的权限检查逻辑完全相同，应该提取为拦截器或 AOP
- 🟡 **错误处理单一** — 所有 Controller 返回固定 ErrorCode（如 UNAUTHORIZED、FORBIDDEN、VALIDATION_FAIL），缺少 500 级错误分类（如 INTERNAL_ERROR、SERVICE_UNAVAILABLE）
- 🔴 **ShortVideoUploadController 无文件大小限制** — 上传接口未见 @RequestParam 的 size 校验，且 BOS 路径采用硬编码 {userId}/{date}/{projectId} 格式，迁移或架构变更时脆弱

---

### 3. 数据库设计分析

#### 3.1 表结构（12 张核心表）

```
sv_video (短视频) — 主表
├── sv_video_data (每日数据快照) — 时间序列
├── sv_comment (评论+情感分析)
│
sv_project (项目) — 多类型容器
├── sv_plan_asset (素材版本管理)
│
sv_plan (创作方案)
│
sv_viral_video (爆款库)
sv_hot_topic (热点话题 — 平台级)
sv_category (分类 — 用户级)
sv_script (脚本)
sv_script_template (脚本模板)
```

#### 3.2 索引设计评估

**优点：**
- ✅ 关键查询都有索引：owner_id+publish_time, account_id+publish_time
- ✅ UNIQUE 索引防重：douyin_video_id, douyin_comment_id
- ✅ 爆款排行索引：(is_viral, view_count DESC)
- ✅ 效果归因索引：ai_call_log_id

**缺陷：**
- 🔴 **SvVideoData 缺少性能索引** — dashboard 常用查询 `view_delta DESC` 无索引，折线图需要 O(n*log n) 扫表
- 🔴 **SvComment 缺少情感分析索引** — sentiment 有单独索引，但无 `(video_id, sentiment_score DESC)` 复合索引，查询负面评论需要多次过滤
- 🟡 **SvProject 复合索引冗余** — (owner_id, status, create_time DESC) 和 (account_id, plan_type) 二者无法共用，查询时至少一个走 sequential scan
- 🟡 **SvHotTopic 的 status + heat_score 索引** — status 是字符串（9 字节），heat_score 是 DECIMAL（16 字节），联合索引占用 25 字节，而单独 heat_score DESC 仅 8 字节，查询 ORDER BY heat_score 时反而不用联合索引

#### 3.3 数据隔离策略

**隔离维度：**
1. **owner_id**（用户私有数据）— SvVideo, SvProject, SvScript, SvPlan, SvCategory
2. **account_id**（抖音账号）— SvVideo, SvProject, SvPlan
3. **platform-level**（平台共享）— SvHotTopic, SvViralVideo（可含 owner_id 做个人收藏）

**问题：**
- 🔴 **SvViralVideo 的 owner_id 可选** — 允许 NULL，导致全量列表查询时无法分离用户视图与平台库视图
- 🟡 **SvHotTopic 无 owner_id** — 完全平台级，用户无法个性化定制过期时间、关键词过滤等

#### 3.4 关键设计决策

**优点：**
- ✅ SvVideoData 无 deleted 字段 — 按 snapshot_date 归档，180 天后批量删除（符合时序特征）
- ✅ SvPlanAsset 支持版本管理 — version 字段 + 独立表，编排阶段可多版本迭代
- ✅ SvGenerationLog 完整记录 AI 调用 — ai_provider, quality_score, cost, duration 齐全，用于质量评估和成本分析

**缺陷：**
- 🔴 **SvProject 过度设计** — 一张表承载 viral_clone/daily/soft_ad 三种完全不同的业务流程，导致字段冗余：
  - viral_clone 用：reference_video_id, character_reference_url, scene_reference_url
  - daily 用：persona_id, schedule_date, shoot_status  
  - soft_ad 用：publish_platforms, publish_time
  - 三者共享的只有：title, status, script_id, shot_list_id
  - **建议拆分：** sv_project_base + sv_project_daily + sv_project_viral 三表继承

---

### 4. 前端实现分析

#### 4.1 页面架构（16 个页面）

| 页面 | 组件规模 | 特点 |
|------|--------|------|
| ShortVideoDashboardPage | 100+ | 主仪表板，4 个 useQuery 并行加载 |
| ProjectManagementPage | 80+ | CRUD 表格 + 弹框编辑 |
| ScriptPlanningPage | 复杂 | AI 脚本生成 + 编辑 + 模板 |
| VideoEditingPage | 富应用 | 分镜编辑 + 素材上传 + 预览 |
| MaterialProductionPage | 中等 | AI 生成关键帧/视频/配音 |
| ViralLibraryPage | 中等 | 爆款库浏览 + 收藏 + 拆解 |
| QualityDashboardPage | 复杂 | 质量看板 + 多维度聚合 |
| DramaEditorPage | 复杂 | 短剧创作编辑器 + 多集管理 |

**React Hooks 使用：**
- 70+ 个 `useQuery` 调用（TanStack Query）
- 充分使用 async/await
- 自定义 hooks 不足（代码重复度高）

**优点：**
- ✅ TanStack Query 缓存 + 自动 refetch
- ✅ 错误处理覆盖大多数场景
- ✅ 加载态、空态、异常态有 UI 反馈
- ✅ 分页 + 排序实现完整

**缺陷：**
- 🔴 **ProjectManagementPage 的 fetchData** — 第 62-65 行使用 useCallback，但依赖数组中有 [page, rowsPerPage]，每次翻页都会重新创建，破坏了 callback 缓存意义
- 🔴 **缺少全局错误边界** — 各页面 error state 处理不一致，部分页面无 error catch
- 🟡 **未使用自定义 hooks 复用** — 权限检查、数据加载、错误提示的代码在 16 个页面中重复 100+ 次
- 🟡 **ShortVideoDashboardPage 的趋势图** — 硬编码 `trendOption` 配置，无响应式适配，大屏幕上 y 轴标签会重叠

#### 4.2 API 客户端（shortvideo.ts）

**特点：**
- 100+ 个函数，覆盖所有后端接口
- 统一 request 客户端，自动注入 Bearer Token 和 /api/v1 前缀
- 部分 API 有 timeout 调优（quickGenerate 10 分钟）

**缺陷：**
- 🟡 **缺少类型定义** — 大量 API 返回 `Record<string, unknown>`，而非具体 DTO 类型
- 🟡 **缺少错误重试** — 流量波动时容易超时，无 retry-logic

#### 4.3 类型系统

**缺陷：**
- 🟡 **SvProject 类型定义不完整** — 前端看不到后端的全部字段（如 reviewStatus, reviewComment），导致审核页面需要额外手动映射

---

### 5. 问题清单

#### 🔴 严重问题（影响核心功能或安全）

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 1 | **ContentAuditServiceImpl 三个 TODO 未实现** | service/impl/ | 内容审核完全不可用，发布流程断链 |
| 2 | **SvProject 过度设计，字段冗余** | entity/SvProject.java | viral_clone/daily/soft_ad 三种业务混在一张表，维护困难，索引低效 |
| 3 | **SvVideoData 缺关键索引** | schema.sql:74-75 | Dashboard 趋势查询性能差（O(n*logn)），大账号下可能超时 |
| 4 | **ShortVideoUploadController 无文件大小限制** | controller/ | 恶意用户可上传 GB 级文件，导致 OOM 或存储爆炸 |
| 5 | **ShortVideoDashboardServiceImpl 数据全为零** | service/impl/ | Dashboard 统计数据造假，无法真实反映业务状态 |
| 6 | **缺少 @Transactional 保证** | service/impl/ | quickGenerate 创建 3 级资源，一级失败会孤立 script/shotList（数据库垃圾） |
| 7 | **SvProject 缺少 version 字段（乐观锁）** | entity/ | 高并发编辑项目时，后更新会覆盖先前修改（Last-Write-Wins 问题） |
| 8 | **前端 ProjectManagementPage.fetchData 缓存失效** | pages/shortvideo/ | 翻页时 callback 重新创建，缓存无效，每次翻页都会网络请求（即使用户二次访问同页） |

#### 🟡 建议问题（设计不优，但可兼容）

| # | 问题 | 位置 | 建议 |
|---|------|------|------|
| 1 | SvVideoRepository 计算均值低效 | repository/ | 用原生 SQL `SELECT AVG(view_count)` 替换 List\<Long\> 加载 |
| 2 | SvProjectServiceImpl 第 60 行 validateParams() 重复调用 | service/impl/ | 删除重复，仅第 37 行保留 |
| 3 | Controller 权限检查代码重复 | controller/ | 提取为 @Aspect 或拦截器 |
| 4 | SvCommentRepository 缺全文搜索索引 | repository/ | 考虑集成 Elasticsearch 做评论内容搜索 |
| 5 | SvHotTopic 无 owner_id，无个性化支持 | entity/ | 增加 user_favorites 表，记录用户关注的热点 |
| 6 | SvComment sentiment 索引设计欠佳 | schema.sql | 改为 `(video_id, sentiment_score DESC)` 复合索引 |
| 7 | PublishTimeRecommendationServiceImpl 两次查询可合并 | service/impl/ | 使用单一 UNION 查询，或先检查 count 再决定查询策略 |
| 8 | ContentAuditServiceImpl 应注入审核 SDK | service/impl/ | 整合百度云内容安全 API / 阿里云绿网服务 |
| 9 | 前端缺少自定义 hooks 复用 | pages/shortvideo/ | 提取 useProjectList, useVideoList, useErrorHandler 等 |
| 10 | ShortVideoDashboardPage 趋势图无响应式 | pages/shortvideo/ | 改用 ECharts 的响应式配置或 ResizeObserver |

#### 🟢 良好实现

| # | 优点 | 位置 |
|---|------|------|
| 1 | Entity @SQLRestriction 逻辑删除规范 | entity/ |
| 2 | Service 层 owner_id 强制过滤 | service/impl/ |
| 3 | Repository @Query 避免 N+1 | repository/ |
| 4 | 前端 TanStack Query 缓存管理 | pages/shortvideo/ |
| 5 | API 路由规范 `/api/v1/short-video/*` | controller/ |
| 6 | 设计文档完整（12 份 + 300+ 需求）| docs/ |

---

### 6. 升级建议（优先级排序）

#### 🚨 P0 紧急修复（周内完成）

1. **实现 ContentAuditServiceImpl** （2 天）
   - 集成百度云内容安全 API（图像/视频/文本）
   - 补充 audit 日志表记录审核结果
   - 关键路径：publish API 调用审核，阻止不合规内容发布

2. **添加 SvProject.version 乐观锁** （1 天）
   - Entity 增加 `@Version Long version` 注解
   - Repository save 时自动递增 version
   - 高并发测试验证（压测 100 并发编辑同项目）

3. **修复 ShortVideoUploadController 文件大小限制** （1 天）
   - `@RequestParam` 添加 `@Size(min=1, max=104857600)` 校验（100MB）
   - 上传前前端 JavaScript 预检查
   - 配置 Spring `multipart.max-file-size=100MB`

#### 🔴 P1 高优先级重构（2 周内）

4. **重构 SvProject 为 Project Hierarchy** （3 天）
   ```java
   SvProject (抽象基类/接口)
   ├─ SvProjectDaily (daily 类型)
   ├─ SvProjectViralClone (viral_clone 类型)
   └─ SvProjectSoftAd (soft_ad 类型)
   ```
   迁移脚本：数据按 projectType 拆分到三张表

5. **补充 SvVideoData 关键索引** （1 天）
   ```sql
   CREATE INDEX idx_sv_vd_view_delta ON sv_video_data (snapshot_date DESC, view_delta DESC);
   ```

6. **修复 ShortVideoDashboardServiceImpl 数据来源** （2 天）
   - `getStats()` 从 sv_video 聚合计数、播放量求和
   - `getTrend()` 从 sv_video_data 按日期聚合
   - 补充 cost / ROI 计算（从 SvGenerationLog.cost 字段）

7. **为 quickGenerate 补充 @Transactional** （1 天）
   ```java
   @Transactional(rollbackFor = Exception.class)
   public Map<String, Object> quickGenerate(...) { ... }
   ```

#### 🟡 P2 优化改进（1 个月内）

8. **提取通用权限检查为 AOP** （2 天）
   ```java
   @aspect
   public class DataScopeAspect {
       @Before("@annotation(DataScope)")
       public void checkDataScope(...) { ... }
   }
   ```

9. **集成 Elasticsearch 做评论全文搜索** （3 天）
   - 新建 sv_comment_index
   - 定时同步 SvComment 到 ES
   - 前端 CommentPage 添加关键词搜索框

10. **前端 hooks 提取与复用** （3 天）
    ```typescript
    // hooks/useSvProject.ts
    export function useProjectList(page, rows) { ... }
    
    // hooks/useDataScope.ts
    export function useVisibleUsers() { ... }
    ```

11. **SvHotTopic 增加用户关注表** （2 天）
    ```sql
    CREATE TABLE sv_user_hot_topic_follow (
        id BIGSERIAL,
        owner_id BIGINT NOT NULL,
        topic_id BIGINT NOT NULL,
        notification_enabled BOOLEAN,
        PRIMARY KEY(owner_id, topic_id)
    );
    ```

12. **Dashboard 趋势图响应式适配** （1 天）
    - 使用 ResizeObserver 监听容器大小
    - 根据宽度动态调整 xAxis label rotate

---

### 7. 测试建议

#### 功能测试
- [ ] 快速生成：验证 project/script/shotList 三级资源均创建成功
- [ ] 爆款复刻：验证 owner_id 隔离（A 用户爆款无法被 B 用户复刻）
- [ ] 内容审核：验证不合规内容被拦截（预测试用不合规图片/文本）
- [ ] 发布时间推荐：验证两级查询降级（有推荐数据时用推荐，无数据时用 Top5）

#### 性能测试
- [ ] Dashboard 加载时间：1000 个视频 + 365 条快照数据，TPS 目标 > 50
- [ ] 项目列表分页：10000 条项目，翻到第 100 页，响应时间 < 200ms
- [ ] 并发编辑：100 个用户同时修改同项目，验证 version 冲突处理

#### 安全测试
- [ ] 文件上传：尝试上传 200MB 文件，验证拒绝
- [ ] SQL 注入：脚本描述含 `; DROP TABLE sv_project; --`，验证转义
- [ ] 权限绕过：A 用户 token 尝试删除 B 用户的项目，验证 403

---

### 8. 总体评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 需求覆盖度 | 9/10 | 28 个用户故事已设计，58 个 API 已规划 |
| 代码规范性 | 7/10 | Entity/Service 规范，但 Controller 有重复代码 |
| 数据库设计 | 6/10 | 表结构合理，但索引和数据隔离有缺陷 |
| 后端实现完整度 | 6/10 | 核心 CRUD 完整，但 ContentAudit 未实现，Dashboard 数据造假 |
| 前端实现完整度 | 8/10 | 页面丰富，React Hooks 使用得当，但缺自定义 hooks 复用 |
| 文档完整度 | 9/10 | 12 份设计文档，覆盖全面 |
| **综合评分** | **7.2/10** | 架构清晰，但有 8 个严重隐患待修复 |

---

### 9. 迁移建议（next phase）

**短期（1 个月）：** 修复 P0/P1 问题，确保 ContentAudit、文件上传、数据一致性可靠  
**中期（3 个月）：** 重构 SvProject 继承体系，集成 Elasticsearch，提升查询性能  
**长期（6 个月）：** AI 归因闭环闭合，支持 A/B 测试框架，完整效果评估

---
