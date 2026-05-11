# Dashboard 模块架构审查报告

## 1. 模块概述

### 1.1 功能定位

Dashboard 模块是 dy05 平台的**管理驾驶舱**，为管理员和机构用户提供多维度的业务数据统计与可视化展示。作为数据聚合层，Dashboard 模块从各业务模块（直播、短视频、商品、文案、AI 等）汇总关键指标，支持实时监控、趋势分析和决策支持。

### 1.2 核心职责

| 职责 | 说明 |
|------|------|
| **多维度统计** | 用户、视频、直播、短视频、文案、AI 调用、收入等全维度统计 |
| **角色分级展示** | 管理员（全局统计）、机构用户（租户隔离统计） |
| **GMV 分析** | 统一 KPI、直播形式 GMV 分布、商品 GMV 汇总、利润矩阵 |
| **转化漏斗** | 观看 → 点赞 → 进入商品的转化路径分析 |
| **驾驶舱预览** | 场次级别的 GMV、商品线数、状态筛选与 CSV 导出 |
| **缓存优化** | 管理员和机构统计数据缓存 5 分钟，减少数据库压力 |

### 1.3 业务价值

- **决策支持**：为管理层提供实时业务健康度指标（GMV、转化率、AI 成功率）
- **运营监控**：跟踪今日/昨日对比、直播形式效果、商品销售排行
- **数据透明**：机构用户可查看自己的业务数据，支持自主运营优化
- **导出能力**：驾驶舱数据支持 CSV 导出，便于二次分析和报表制作

---

## 2. 架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller 层                           │
│  DashboardController (2 个管理端点 + 7 个 GMV/KPI 端点)      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       Service 层                             │
│  DashboardService (管理员/机构统计，缓存 5 分钟)            │
│  DashboardGmvService (GMV/KPI 聚合，无缓存)                 │
│  CockpitExportService (驾驶舱导出，接口定义)                │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Repository 层（跨模块）                   │
│  AuthUserRepository, DouyinVideoRepository,                 │
│  LiveSessionRepository, LiveProductRepository,              │
│  SvVideoRepository, CopyLibraryRepository,                  │
│  AiCallLogRepository                                        │
└─────────────────────────────────────────────────────────────┘
```

**特点**：
- **无独立数据表**：Dashboard 模块不维护自己的数据表，完全依赖其他模块的 Repository
- **聚合计算**：在 Service 层完成跨模块数据聚合和统计计算
- **缓存分离**：管理统计（低频变化）使用缓存，GMV 统计（实时性要求高）不缓存

### 2.2 核心组件

| 组件 | 类型 | 职责 |
|------|------|------|
| **DashboardController** | Controller | 9 个端点：管理员统计、机构统计、统一 KPI、直播形式 GMV、商品 GMV、驾驶舱预览、利润矩阵、转化漏斗、CSV 导出 |
| **DashboardService** | Service | 管理员和机构统计数据聚合，缓存 5 分钟 |
| **DashboardGmvService** | Service | GMV/KPI 实时统计，支持 lookbackDays 参数 |
| **CockpitExportService** | Interface | 驾驶舱导出接口定义（实现类未扫描到） |
| **17 个 VO 类** | VO | BusinessDashboardVO, UnifiedKpiVO, LiveFormatGmvRowVO, ProductGmvRowVO, CockpitPreviewVO, ConversionFunnelVO, GmvTrendVO, GmvPredictionVO 等 |

### 2.3 数据流

#### 管理员统计流程
```
用户请求 → DashboardController.getAdminStats()
         → DashboardService.getAdminStats() [缓存 5 分钟]
         → 并行查询 7 个 Repository（用户/视频/直播/短视频/文案/AI/商品）
         → 聚合计算（总数/今日新增/成功率/收入）
         → 返回 Map<String, Object>
```

#### GMV 统计流程
```
用户请求 → DashboardController.getLiveFormatGmv(lookbackDays)
         → DashboardGmvService.getLiveFormatGmv()
         → LiveSessionRepository.findByUserIdAndDeleted() [按 userId 过滤]
         → 按 sessionType 分组聚合
         → LiveProductRepository.sumRevenueBySessionId() [逐场次查询 GMV]
         → 返回 { lookbackDays, since, rows: [{ liveFormat, sessionCount, totalGmv }] }
```

#### 驾驶舱预览流程
```
用户请求 → DashboardController.getCockpitPreview(filters)
         → DashboardGmvService.getCockpitPreview()
         → 解析 dateFrom 和 sessionStatus 过滤条件
         → LiveSessionRepository.findByUserIdAndDeleted()
         → 逐场次查询 GMV 和商品线数
         → 返回 { rows: [{ sessionId, liveTitle, status, gmv, productLineCount }] }
```

---

## 3. 技术选型

### 3.1 框架与库

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.7 | 基础框架 |
| Spring Data JPA | 3.3.7 | Repository 层数据访问 |
| Spring Cache | 3.3.7 | 管理统计缓存（@Cacheable） |
| Lombok | 1.18.30 | VO 类简化（@Data, @Builder） |
| SpringDoc OpenAPI | 2.6.0 | API 文档生成 |

### 3.2 存储方案

**无独立存储**：Dashboard 模块不维护自己的数据表，完全依赖其他模块的数据：

| 数据源 | 表 | 用途 |
|--------|-----|------|
| auth 模块 | auth_user | 用户总数、活跃用户、今日新增 |
| douyin 模块 | dy_video | 抖音视频总数、已发布、今日新增 |
| live 模块 | live_session | 直播场次总数、已完成、今日新增、GMV 聚合 |
| live 模块 | live_product | 商品 GMV、销售额、商品线数 |
| shortvideo 模块 | sv_video | 短视频总数、已发布、今日新增 |
| copy 模块 | copy_library | 文案总数、已审核、今日新增 |
| ai 模块 | ai_call_log | AI 调用次数、成功率 |

### 3.3 集成方式

- **Repository 注入**：通过 `@Resource` 注入其他模块的 Repository
- **跨模块查询**：直接调用 Repository 的 `countBy*` 和 `sumBy*` 方法
- **数据隔离**：机构统计通过 `ownerId` 或 `userId` 过滤数据

---

## 4. 数据模型

### 4.1 实体关系

Dashboard 模块**无独立实体**，依赖以下模块的实体：

```
AuthUser (auth_user)
    ↓
DouyinVideo (dy_video) ← ownerId
LiveSession (live_session) ← ownerId/userId
    ↓
LiveProduct (live_product) ← sessionId, revenue
SvVideo (sv_video) ← ownerId
CopyLibrary (copy_library) ← userId
AiCallLog (ai_call_log) ← userId
```

### 4.2 VO 结构

#### AdminStats / OrgStats（管理统计）
```java
Map<String, Object> {
  // 用户维度
  "totalUsers": Long,
  "activeUsers": Long,
  "todayUsers": Long,
  
  // 视频维度
  "totalVideos": Long,
  "publishedVideos": Long,
  "todayVideos": Long,
  
  // 直播维度
  "totalLiveSessions": Long,
  "completedSessions": Long,
  "todaySessions": Long,
  
  // 短视频维度
  "totalShortVideos": Long,
  "publishedShortVideos": Long,
  "todayShortVideos": Long,
  
  // 文案维度
  "totalCopyItems": Long,
  "approvedCopyItems": Long,
  "todayCopyItems": Long,
  
  // AI 维度
  "todayAiCalls": Long,
  "todayAiAttempts": Long,
  "todayAiSuccessRate": BigDecimal,
  
  // 收入维度
  "todayRevenue": BigDecimal
}
```

#### UnifiedKpiVO（统一 KPI）
```java
{
  "content": {
    "shortVideoCount": Long,
    "productScriptCount": Long,
    "kbDocumentCount": Long
  },
  "traffic": {
    "liveViewerSum": Long,
    "shortVideoViewSum": Long
  },
  "conversion": {
    "saleQuantitySinceToday": Long,
    "revenueLinesSinceToday": Long
  },
  "revenue": {
    "todayGmv": BigDecimal,
    "yesterdayGmv": BigDecimal,
    "avgOrderValueToday": BigDecimal
  }
}
```

#### LiveFormatGmvRowVO（直播形式 GMV）
```java
{
  "lookbackDays": Integer,
  "since": String,
  "rows": [
    {
      "liveFormat": String,
      "sessionCount": Long,
      "totalGmv": BigDecimal
    }
  ]
}
```

#### CockpitSessionRowVO（驾驶舱场次）
```java
{
  "sessionId": Long,
  "liveTitle": String,
  "accountId": Long,
  "userId": Long,
  "status": Integer,
  "startTime": String,
  "endTime": String,
  "gmv": BigDecimal,
  "productLineCount": Long
}
```

### 4.3 索引策略

Dashboard 模块依赖其他模块的索引：

| 表 | 索引 | 用途 |
|-----|------|------|
| auth_user | idx_deleted, idx_status_deleted, idx_create_time_deleted | 用户统计查询 |
| dy_video | idx_owner_id_deleted, idx_status_deleted, idx_create_time_deleted | 视频统计查询 |
| live_session | idx_owner_id_deleted, idx_user_id_deleted, idx_status_deleted, idx_create_time_deleted | 直播统计查询 |
| live_product | idx_session_id, idx_owner_id_create_time | GMV 聚合查询 |
| sv_video | idx_owner_id_deleted, idx_create_time_deleted | 短视频统计查询 |
| copy_library | idx_user_id_deleted, idx_create_time_deleted | 文案统计查询 |
| ai_call_log | idx_user_id_create_time_status | AI 调用统计查询 |

---

## 5. API 设计

### 5.1 接口清单

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/v1/dashboard/admin/stats` | POST | 管理员统计 | admin 角色 |
| `/api/v1/dashboard/org/stats` | POST | 机构统计 | 登录用户 |
| `/api/v1/dashboard/kpi-unified` | POST | 统一 KPI | 登录用户 |
| `/api/v1/dashboard/live-format-gmv` | POST | 直播形式 GMV | 登录用户 |
| `/api/v1/dashboard/product-gmv-summary` | POST | 商品 GMV 汇总 | 登录用户 |
| `/api/v1/dashboard/cockpit-preview` | POST | 驾驶舱预览 | 登录用户 |
| `/api/v1/dashboard/profit-matrix-preview` | POST | 利润矩阵预览 | 登录用户 |
| `/api/v1/dashboard/conversion-funnel` | POST | 转化漏斗 | 登录用户 |
| `/api/v1/dashboard/cockpit-export` | POST | 驾驶舱 CSV 导出 | 登录用户 |

### 5.2 请求/响应格式

#### 管理员统计
```http
POST /api/v1/dashboard/admin/stats
Content-Type: application/json



Response:
{
  "status": 200,
  "message": "success",
  "data": {
    "totalUsers": 1000,
    "activeUsers": 800,
    "todayUsers": 50,
    "totalVideos": 5000,
    "publishedVideos": 3000,
    "todayVideos": 100,
    "totalLiveSessions": 2000,
    "completedSessions": 1500,
    "todaySessions": 20,
    "totalShortVideos": 8000,
    "publishedShortVideos": 6000,
    "todayShortVideos": 150,
    "totalCopyItems": 10000,
    "approvedCopyItems": 8000,
    "todayCopyItems": 200,
    "todayAiCalls": 500,
    "todayAiAttempts": 550,
    "todayAiSuccessRate": 90.91,
    "todayRevenue": 1000000.00
  },
  "traceId": "abc123",
  "timestamp": 1715270400000
}
```

#### 统一 KPI
```http
POST /api/v1/dashboard/kpi-unified
Content-Type: application/json

{
  "lookbackDays": 30
}

Response:
{
  "status": 200,
  "data": {
    "content": {
      "shortVideoCount": 0,
      "productScriptCount": 0,
      "kbDocumentCount": 0
    },
    "traffic": {
      "liveViewerSum": 100000,
      "shortVideoViewSum": 0
    },
    "conversion": {
      "saleQuantitySinceToday": 0,
      "revenueLinesSinceToday": 0
    },
    "revenue": {
      "todayGmv": 50000.00,
      "yesterdayGmv": 45000.00,
      "avgOrderValueToday": 0.00
    }
  }
}
```

#### 驾驶舱预览
```http
POST /api/v1/dashboard/cockpit-preview
Content-Type: application/json

{
  "dateFrom": "2026-04-01",
  "sessionStatus": 2
}

Response:
{
  "status": 200,
  "data": {
    "rows": [
      {
        "sessionId": 1,
        "liveTitle": "护肤品专场",
        "accountId": 100,
        "userId": 1,
        "status": 2,
        "startTime": "2026-04-01 19:00:00",
        "endTime": "2026-04-01 21:00:00",
        "gmv": 150000.00,
        "productLineCount": 10
      }
    ],
    "rowCount": 1
  }
}
```

#### 驾驶舱 CSV 导出
```http
POST /api/v1/dashboard/cockpit-export
Content-Type: application/json

{
  "dateFrom": "2026-04-01"
}

Response:
{
  "status": 200,
  "data": {
    "csv": "场次ID,标题,状态,开始时间,结束时间,GMV,商品线数\n1,护肤品专场,2,2026-04-01 19:00:00,2026-04-01 21:00:00,150000.00,10\n",
    "filename": "cockpit_20260509.csv",
    "rowCount": 1
  }
}
```

### 5.3 错误处理

| 错误码 | 说明 | 场景 |
|--------|------|------|
| 2001 | 未登录 | 机构统计接口未传 userId |
| 2003 | 权限不足 | 非管理员访问管理员统计 |
| 5000 | 服务器错误 | Repository 查询异常 |

---

## 6. 安全设计

### 6.1 认证授权

| 层级 | 机制 | 实现 |
|------|------|------|
| **认证** | Bearer Token | `AuthTokenFilter.getUserId(request)` 从请求头提取 userId |
| **角色校验** | 管理员角色 | `AuthTokenFilter.getRoleCode(request)` 检查 `roleCode == "admin"` |
| **注解注入** | `@CurrentUserId` | Controller 方法参数自动注入当前用户 ID |

**管理员统计端点**：
```java
@PostMapping("/admin/stats")
public RESTResult<Map<String, Object>> getAdminStats(HttpServletRequest request) {
    String roleCode = AuthTokenFilter.getRoleCode(request);
    if (!"admin".equals(roleCode)) {
        return RESTResult.error(ErrorCode.PERMISSION_DENIED, "仅管理员可访问");
    }
    // ...
}
```

### 6.2 数据隔离

| 统计类型 | 隔离机制 | 实现 |
|----------|----------|------|
| **管理员统计** | 全局数据 | 无过滤，查询所有 `deleted = 0` 的记录 |
| **机构统计** | 租户隔离 | 通过 `ownerId` 或 `userId` 过滤数据 |
| **GMV 统计** | 用户隔离 | `LiveSessionRepository.findByUserIdAndDeleted(userId, 0)` |

**机构统计示例**：
```java
stats.put("totalVideos", videoRepository.countByOwnerIdAndDeleted(userId, 0));
stats.put("totalLiveSessions", sessionRepository.countByOwnerIdAndDeleted(userId, 0));
```

### 6.3 敏感数据保护

- **无敏感字段**：Dashboard 返回的统计数据均为聚合指标，不包含用户个人信息
- **GMV 数据**：仅返回当前用户有权访问的场次 GMV，不泄露其他用户数据
- **导出限制**：CSV 导出仅包含当前用户的场次数据

---

## 7. 性能设计

### 7.1 缓存策略

| 缓存项 | 缓存键 | TTL | 失效策略 |
|--------|--------|-----|----------|
| **管理员统计** | `dashboard:admin` | 5 分钟 | 时间过期 |
| **机构统计** | `dashboard:org:{userId}` | 5 分钟 | 时间过期 |

**缓存配置**：
```java
@Cacheable(value = "dashboard:admin", unless = "#result == null")
public Map<String, Object> getAdminStats() { ... }

@Cacheable(value = "dashboard:org", key = "#userId", unless = "#result == null")
public Map<String, Object> getOrgStats(Long userId) { ... }
```

**GMV 统计不缓存**：
- 原因：GMV 数据实时性要求高，用户期望看到最新数据
- 优化：通过索引优化查询性能，而非缓存

### 7.2 查询优化

#### N+1 查询问题

**当前实现**（存在 N+1 问题）：
```java
// DashboardGmvService.getLiveFormatGmv()
List<LiveSession> sessions = getSessionsByUser(userId, since);
for (LiveSession s : sessions) {
    BigDecimal gmv = productRepository.sumRevenueBySessionId(s.getId()); // N 次查询
}
```

**优化建议**：
```java
// 批量查询所有场次的 GMV
List<Long> sessionIds = sessions.stream().map(LiveSession::getId).collect(Collectors.toList());
Map<Long, BigDecimal> gmvMap = productRepository.sumRevenueBySessionIds(sessionIds); // 1 次查询
```

#### 分页限制

```java
// 限制最多查询 200 条场次，避免全表扫描
PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createTime"))
```

### 7.3 并发控制

- **无并发写入**：Dashboard 模块只读，无并发写入问题
- **缓存并发**：Spring Cache 默认使用 ConcurrentHashMap，线程安全
- **Repository 并发**：JPA Repository 方法线程安全

---

## 8. 可观测性

### 8.1 日志

**当前状态**：无明确日志记录

**建议增强**：
```java
@Slf4j
@Service
public class DashboardGmvService {
    public Map<String, Object> getLiveFormatGmv(Long userId, int lookbackDays) {
        log.info("getLiveFormatGmv: userId={}, lookbackDays={}", userId, lookbackDays);
        Timestamp since = sinceTimestamp(lookbackDays);
        List<LiveSession> sessions = getSessionsByUser(userId, since);
        log.debug("Found {} sessions for userId={}", sessions.size(), userId);
        // ...
    }
}
```

### 8.2 监控

**当前状态**：无自定义监控指标

**建议增强**：
```java
@Timed(value = "dashboard.gmv.query", description = "GMV 查询耗时")
public Map<String, Object> getLiveFormatGmv(Long userId, int lookbackDays) { ... }

@Counted(value = "dashboard.admin.stats", description = "管理员统计调用次数")
public Map<String, Object> getAdminStats() { ... }
```

### 8.3 追踪

**当前实现**：
```java
RESTResult<Map<String, Object>> r = RESTResult.getSuccess(stats);
r.setTraceId(MDC.get("traceId")); // 从 MDC 获取 traceId
return r;
```

**优点**：所有响应包含 traceId，便于分布式追踪

---

## 9. 架构评分

### 9.1 评分维度

| 维度 | 得分 | 满分 | 说明 |
|------|------|------|------|
| **模块化** | 8 | 10 | 职责清晰，但依赖过多外部 Repository |
| **可扩展性** | 7 | 10 | 新增统计维度需修改 Service，缺乏插件化机制 |
| **性能** | 6 | 10 | 存在 N+1 查询问题，GMV 统计无缓存 |
| **安全性** | 9 | 10 | 角色校验、数据隔离完善，无敏感数据泄露 |
| **可维护性** | 7 | 10 | 代码清晰，但缺乏日志和监控 |
| **测试覆盖** | 8 | 10 | 有集成测试，但缺少单元测试和性能测试 |
| **文档完整性** | 6 | 10 | 有 API 注解，但缺乏架构文档和使用说明 |

**总分**：51 / 70 = **72.9 分**

### 9.2 等级评定

**B 级**（良好）

**理由**：
- ✅ 职责清晰，数据隔离完善，安全性高
- ✅ 缓存策略合理，管理统计缓存 5 分钟
- ✅ 有集成测试覆盖核心端点
- ⚠️ 存在 N+1 查询问题，影响性能
- ⚠️ 缺乏日志和监控，可观测性不足
- ⚠️ 扩展性一般，新增统计维度需修改多处代码

---

## 10. 改进建议

### 10.1 P0 问题（阻塞级，必须修复）

**无 P0 问题**

### 10.2 P1 问题（高优先级，建议修复）

| 问题 | 影响 | 建议方案 |
|------|------|----------|
| **N+1 查询问题** | GMV 统计性能差，200 个场次需 200+ 次数据库查询 | 新增 `LiveProductRepository.sumRevenueBySessionIds(List<Long>)` 批量查询方法 |
| **缺乏日志记录** | 问题排查困难，无法追踪统计查询耗时 | 在 Service 层增加 INFO 和 DEBUG 日志 |
| **缺乏监控指标** | 无法监控统计查询性能和调用频率 | 增加 `@Timed` 和 `@Counted` 注解 |

**N+1 查询优化示例**：
```java
// LiveProductRepository.java
@Query("SELECT lp.sessionId, SUM(lp.revenue) FROM LiveProduct lp " +
       "WHERE lp.sessionId IN :sessionIds AND lp.deleted = 0 " +
       "GROUP BY lp.sessionId")
Map<Long, BigDecimal> sumRevenueBySessionIds(@Param("sessionIds") List<Long> sessionIds);

// DashboardGmvService.java
List<Long> sessionIds = sessions.stream().map(LiveSession::getId).collect(Collectors.toList());
Map<Long, BigDecimal> gmvMap = productRepository.sumRevenueBySessionIds(sessionIds);
for (LiveSession s : sessions) {
    BigDecimal gmv = gmvMap.getOrDefault(s.getId(), BigDecimal.ZERO);
    // ...
}
```

### 10.3 P2 问题（中优先级，可选修复）

| 问题 | 影响 | 建议方案 |
|------|------|----------|
| **GMV 统计无缓存** | 高频查询时数据库压力大 | 增加短期缓存（1 分钟），或使用 Redis 缓存 |
| **缺乏单元测试** | Service 层逻辑未覆盖，重构风险高 | 为 DashboardService 和 DashboardGmvService 增加单元测试 |
| **VO 类型不统一** | 部分端点返回 `Map<String, Object>`，类型不安全 | 将所有端点改为返回强类型 VO |
| **CockpitExportService 未实现** | 接口定义存在但无实现类 | 实现 CockpitExportServiceImpl 或删除接口 |

**GMV 缓存示例**：
```java
@Cacheable(value = "dashboard:gmv", key = "#userId + ':' + #lookbackDays", unless = "#result == null")
public Map<String, Object> getLiveFormatGmv(Long userId, int lookbackDays) { ... }
```

### 10.4 P3 问题（低优先级，长期优化）

| 问题 | 影响 | 建议方案 |
|------|------|----------|
| **依赖过多 Repository** | 模块耦合度高，其他模块变更影响 Dashboard | 引入 DashboardDataProvider 接口，封装跨模块数据访问 |
| **缺乏插件化机制** | 新增统计维度需修改 Service 代码 | 设计统计指标插件系统，支持动态注册 |
| **CSV 导出功能简陋** | 仅支持基础字段，无格式化和多语言 | 引入 Apache POI 或 OpenCSV，支持自定义列和格式 |
| **缺乏前端页面** | 前端 `front/src/pages/dashboard/` 目录为空 | 开发 Dashboard 可视化页面（ECharts 图表） |

**插件化设计示例**：
```java
public interface DashboardMetric {
    String getName();
    Object calculate(Long userId, int lookbackDays);
}

@Service
public class DashboardService {
    @Autowired
    private List<DashboardMetric> metrics; // 自动注入所有实现类
    
    public Map<String, Object> getMetrics(Long userId, int lookbackDays) {
        Map<String, Object> result = new HashMap<>();
        for (DashboardMetric metric : metrics) {
            result.put(metric.getName(), metric.calculate(userId, lookbackDays));
        }
        return result;
    }
}
```

---

## 11. 总结

### 11.1 优势

1. **职责清晰**：Dashboard 作为聚合层，不维护自己的数据表，职责单一
2. **数据隔离完善**：管理员和机构统计分离，租户数据隔离严格
3. **缓存策略合理**：管理统计缓存 5 分钟，平衡实时性和性能
4. **安全性高**：角色校验、数据过滤、无敏感数据泄露
5. **测试覆盖**：有集成测试覆盖 9 个核心端点

### 11.2 劣势

1. **N+1 查询问题**：GMV 统计逐场次查询，性能差
2. **可观测性不足**：缺乏日志、监控和性能指标
3. **扩展性一般**：新增统计维度需修改多处代码
4. **类型不安全**：部分端点返回 `Map<String, Object>`
5. **前端缺失**：无 Dashboard 可视化页面

### 11.3 改进路线图

**短期（1-2 周）**：
1. 修复 N+1 查询问题（P1）
2. 增加日志和监控（P1）
3. 为 GMV 统计增加短期缓存（P2）

**中期（1-2 月）**：
1. 增加单元测试覆盖（P2）
2. 将 `Map<String, Object>` 改为强类型 VO（P2）
3. 开发 Dashboard 前端页面（P3）

**长期（3-6 月）**：
1. 引入 DashboardDataProvider 解耦（P3）
2. 设计统计指标插件系统（P3）
3. 增强 CSV 导出功能（P3）

---

**报告生成时间**：2026-05-09  
**审查人**：Claude Opus 4  
**模块版本**：dy05 (基于 dy02 演进)

