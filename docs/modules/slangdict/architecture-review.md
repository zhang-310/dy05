# slangdict 模块架构审查报告

**模块名称**: slangdict (行业俚语词典)  
**审查日期**: 2026-05-09  
**审查人**: Claude (Architecture Reviewer)  
**模块优先级**: P3

---

## 1. 模块概述

### 1.1 功能定位

slangdict（行业俚语词典）模块为直播带货场景提供创意表达管理能力，支持主播使用有趣的暗语、梗和创意表达来增强产品记忆点和直播趣味性。

### 1.2 核心职责

1. **梗条目管理**: 创建、编辑、删除、搜索行业俚语/暗语/梗
2. **产品关联**: 将梗条目与具体商品绑定，建立产品别名体系
3. **AI 生成**: 基于产品信息自动生成创意表达候选
4. **话术集成**: 为直播话术生成提供创意表达上下文
5. **分类管理**: 支持按类别（product_alias/catchphrase/slang/humor/general）和使用场景（带货/暖场/互动/转场）组织

### 1.3 业务价值

- **增强记忆点**: 通过创意表达让产品更容易被观众记住
- **提升趣味性**: 有趣的梗和暗语增加直播娱乐性
- **降低违规风险**: 使用暗语替代敏感词汇
- **知识沉淀**: 积累行业特色表达，形成团队话术资产
- **AI 辅助**: 自动生成创意表达，降低创作门槛

---

## 2. 架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────┐
│  Controller 层 (SlangDictController)                     │
│  - REST API 端点 (8个)                                   │
│  - 认证授权检查                                          │
│  - 数据权限过滤                                          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Service 层 (SlangDictService / SlangDictServiceImpl)    │
│  - 业务逻辑处理                                          │
│  - JPA Specification 动态查询                            │
│  - AI 生成集成                                           │
│  - 话术上下文构建                                        │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Repository 层                                           │
│  - SdEntryRepository (梗条目)                            │
│  - SdProductMappingRepository (产品关联)                 │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Entity 层                                               │
│  - SdEntry (梗条目实体)                                  │
│  - SdProductMapping (产品关联实体)                       │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  Database 层 (PostgreSQL)                                │
│  - sd_entry (梗条目表)                                   │
│  - sd_product_mapping (产品关联表)                       │
└─────────────────────────────────────────────────────────┘
```

### 2.2 核心组件

| 组件 | 职责 | 关键方法 |
|------|------|----------|
| **SlangDictController** | REST API 入口 | search, get, save, delete, byProduct, bindProduct, unbindProduct, aiGenerate |
| **SlangDictServiceImpl** | 业务逻辑实现 | search, getById, save, delete, getByProductId, bindProduct, unbindProduct, aiGeneratePhrases, buildSlangContextForPrompt |
| **SdEntryRepository** | 梗条目数据访问 | findByIdAndDeleted, JpaSpecificationExecutor |
| **SdProductMappingRepository** | 产品关联数据访问 | findByProductIdAndUserIdAndDeleted, findByEntryIdAndDeleted, softDeleteByEntryAndProduct |
| **LlmClient** | AI 模型调用 | chatWithFallback |

### 2.3 数据流

#### 2.3.1 梗条目搜索流程

```
用户请求 → Controller (认证+数据权限) → Service (构建 Specification) 
→ Repository (JPA 动态查询) → Database → Entity → VO → 响应
```

#### 2.3.2 AI 生成梗流程

```
用户请求 → Controller (认证) → Service (查询产品信息) 
→ LlmClient (调用 AI 模型) → 解析响应 → 返回候选梗列表
```

#### 2.3.3 产品关联流程

```
用户请求 → Controller (认证) → Service (检查重复+创建关联) 
→ MappingRepository → Database → 响应
```

---

## 3. 技术选型

### 3.1 框架与库

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.7 | 应用框架 |
| Spring Data JPA | 3.3.x | ORM 框架 |
| Hibernate | 6.x | JPA 实现 |
| Jakarta Validation | 3.0 | 参数校验 |
| Lombok | 1.18.x | 代码生成 |
| SpringDoc OpenAPI | 2.6.0 | API 文档 |

### 3.2 存储方案

- **主存储**: PostgreSQL 15+
- **表结构**: 2 张表（sd_entry, sd_product_mapping）
- **索引策略**: user_id, category, status, product_id, entry_id
- **逻辑删除**: deleted 字段 + @SQLRestriction

### 3.3 集成方式

| 集成对象 | 方式 | 说明 |
|----------|------|------|
| **AI 模块** | LlmClient | 调用 AI 模型生成创意表达 |
| **Product 模块** | DyProductRepository | 查询商品信息用于 AI 生成 |
| **Live 模块** | buildSlangContextForPrompt | 为直播话术生成提供梗上下文 |
| **Auth 模块** | AuthTokenFilter + DataScopeResolver | 认证授权与数据权限 |

---

## 4. 数据模型

### 4.1 实体关系

```
┌─────────────────┐         ┌──────────────────────┐         ┌─────────────┐
│   SdEntry       │         │  SdProductMapping    │         │  DyProduct  │
│  (梗条目)       │◄────────│   (产品关联)         │────────►│  (商品)     │
├─────────────────┤  1:N    ├──────────────────────┤  N:1    ├─────────────┤
│ id (PK)         │         │ id (PK)              │         │ id (PK)     │
│ user_id         │         │ entry_id (FK)        │         │ ...         │
│ phrase          │         │ product_id (FK)      │         └─────────────┘
│ meaning         │         │ user_id              │
│ category        │         │ deleted              │
│ usage_scene     │         │ create_time          │
│ example         │         └──────────────────────┘
│ source          │
│ use_count       │
│ status          │
│ deleted         │
│ create_time     │
│ update_time     │
└─────────────────┘
```

### 4.2 表结构

#### 4.2.1 sd_entry (梗条目表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| user_id | BIGINT | NOT NULL | 所属用户 |
| phrase | VARCHAR(256) | NOT NULL | 梗/暗语 |
| meaning | VARCHAR(512) | | 真实含义 |
| category | VARCHAR(64) | | 分类：product_alias/catchphrase/slang/humor/general |
| usage_scene | VARCHAR(128) | | 适用场景：带货/暖场/互动/转场 |
| example | TEXT | | 使用示例/完整话术片段 |
| source | VARCHAR(128) | | 来源：自创/同行学习/AI生成 |
| use_count | INTEGER | DEFAULT 0 | 使用次数 |
| status | INTEGER | DEFAULT 1 | 状态：1=启用 0=禁用 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除标记 |
| create_time | TIMESTAMP | | 创建时间 |
| update_time | TIMESTAMP | | 更新时间 |

#### 4.2.2 sd_product_mapping (产品关联表)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 主键 |
| entry_id | BIGINT | NOT NULL | 梗条目 ID |
| product_id | BIGINT | NOT NULL | 商品 ID |
| user_id | BIGINT | NOT NULL | 所属用户 |
| deleted | INTEGER | DEFAULT 0 | 逻辑删除标记 |
| create_time | TIMESTAMP | | 创建时间 |

### 4.3 索引策略

| 索引名 | 表 | 字段 | 类型 | 说明 |
|--------|-----|------|------|------|
| idx_sd_entry_user | sd_entry | user_id | B-Tree | 用户数据隔离 |
| idx_sd_entry_category | sd_entry | category | B-Tree | 按分类查询 |
| idx_sd_entry_status | sd_entry | status | B-Tree | 按状态过滤 |
| idx_sd_pm_entry | sd_product_mapping | entry_id | B-Tree | 查询梗的关联产品 |
| idx_sd_pm_product | sd_product_mapping | product_id | B-Tree | 查询产品的关联梗 |
| idx_sd_pm_user | sd_product_mapping | user_id | B-Tree | 用户数据隔离 |

---

## 5. API 设计

### 5.1 接口清单

| 端点 | 方法 | 功能 | 认证 |
|------|------|------|------|
| `/api/v1/slangdict/entry/search` | POST | 分页搜索梗条目 | ✅ |
| `/api/v1/slangdict/entry/get` | POST | 获取梗条目详情 | ✅ |
| `/api/v1/slangdict/entry/save` | POST | 新增/更新梗条目 | ✅ |
| `/api/v1/slangdict/entry/delete` | POST | 删除梗条目 | ✅ |
| `/api/v1/slangdict/entry/by-product` | POST | 按产品ID查关联梗 | ✅ |
| `/api/v1/slangdict/entry/bind-product` | POST | 绑定梗到产品 | ✅ |
| `/api/v1/slangdict/entry/unbind-product` | POST | 解绑梗与产品 | ✅ |
| `/api/v1/slangdict/entry/ai-generate` | POST | AI 为产品生成候选梗 | ✅ |

### 5.2 请求/响应格式

#### 5.2.1 搜索梗条目

**请求**: `POST /api/v1/slangdict/entry/search`

```json
{
  "page": 0,
  "rows": 20,
  "sortName": "createTime",
  "sortOrder": "desc",
  "keyword": "搜索关键词",
  "category": "product_alias",
  "usageScene": "带货",
  "status": 1,
  "productId": 123
}
```

**响应**: `RESTResult<PageResultVO<SdEntryVO>>`

```json
{
  "status": 200,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "userId": 1,
        "phrase": "给小弟准备一套别墅",
        "meaning": "推荐高端男士内裤",
        "category": "product_alias",
        "usageScene": "带货",
        "example": "兄弟们，今天给小弟准备一套别墅，舒适透气...",
        "source": "自创",
        "useCount": 15,
        "status": 1,
        "createTime": "2026-05-01T10:00:00",
        "updateTime": "2026-05-09T15:00:00",
        "productIds": [123, 456]
      }
    ],
    "pageNum": 0,
    "pageSize": 20
  },
  "traceId": "abc123"
}
```

#### 5.2.2 AI 生成梗

**请求**: `POST /api/v1/slangdict/entry/ai-generate?productId=123&count=5`

**响应**: `RESTResult<List<String>>`

```json
{
  "status": 200,
  "message": "success",
  "data": [
    "\"给小弟准备一套别墅\" → 推荐高端男士内裤",
    "\"今天给兄弟们安排个豪宅\" → 推荐舒适内裤套装",
    "\"这个必须给家人们囤一波\" → 推荐多件装内裤"
  ],
  "traceId": "def456"
}
```

### 5.3 错误处理

| 错误码 | 说明 | HTTP 状态 |
|--------|------|-----------|
| 200 | 成功 | 200 |
| 204 | 删除/更新成功 | 200 |
| 2001 | 未登录 | 200 |
| 3001 | 参数校验失败 | 200 |
| 4001 | 数据不存在 | 200 |
| 5001 | AI 模型不可用 | 200 |
| 5002 | AI 生成失败 | 200 |

**错误响应示例**:

```json
{
  "status": 2001,
  "message": "未登录",
  "data": null,
  "traceId": "xyz789"
}
```

---

## 6. 安全设计

### 6.1 认证授权

- **认证方式**: Bearer Token (JWT)
- **认证实现**: `AuthTokenFilter.getUserId(request)`
- **授权检查**: 所有接口强制登录（userId == null 返回 2001）
- **数据权限**: 通过 `DataScopeResolver` 获取可见用户 ID 列表

### 6.2 数据隔离

#### 6.2.1 用户级隔离

```java
// Service 层强制过滤
if (vo.getUserId() != null && vo.getUserId() > 0) {
    predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
} else if (vo.getUserIds() != null && !vo.getUserIds().isEmpty()) {
    predicates.add(root.get("userId").in(vo.getUserIds()));
}
```

#### 6.2.2 逻辑删除

- Entity 层: `@SQLRestriction("deleted = 0")`
- Repository 层: `findByIdAndDeleted(id, 0)`
- Service 层: 删除操作设置 `deleted = 1`

### 6.3 敏感数据保护

- **无敏感字段**: 模块不涉及密码、支付、个人隐私等敏感数据
- **内容审核**: 梗内容由用户自行管理，建议集成违禁词检测
- **AI 生成内容**: 提示词中明确要求"不要违禁词、不要低俗"

### 6.4 输入校验

- **参数校验**: `@Valid` + `@NotBlank` (phrase 字段必填)
- **分页参数**: `BasicQueryDto.validateParams()` 自动修正非法值
- **ID 校验**: Service 层检查 `id == null || id <= 0`

---

## 7. 性能设计

### 7.1 缓存策略

**当前状态**: ❌ 未实现缓存

**建议缓存点**:
1. **热门梗条目**: 按 use_count 排序的 Top 100
2. **产品关联梗**: `getByProductId(productId, userId)` 结果缓存 5 分钟
3. **AI 模型配置**: `resolveModels()` 结果缓存 10 分钟

### 7.2 查询优化

#### 7.2.1 索引使用

- ✅ user_id 索引: 数据隔离查询
- ✅ category 索引: 按分类过滤
- ✅ status 索引: 按状态过滤
- ✅ product_id 索引: 产品关联查询

#### 7.2.2 分页查询

- ✅ 使用 JPA Pageable
- ✅ 分页上限 1000 (BasicQueryDto 自动限制)
- ✅ 默认每页 30 条

#### 7.2.3 N+1 查询预防

- ⚠️ `getById()` 方法存在 N+1 问题:
  ```java
  // 查询 entry (1 次)
  SdEntry entity = entryRepository.findByIdAndDeleted(id, 0);
  // 查询 mappings (1 次)
  List<SdProductMapping> mappings = mappingRepository.findByEntryIdAndDeleted(id, 0);
  ```
  **影响**: 单条查询，影响较小

- ✅ `search()` 方法无 N+1 问题（不加载关联产品）

### 7.3 并发控制

- **乐观锁**: ❌ 未实现（Entity 无 @Version 字段）
- **悲观锁**: ❌ 未使用
- **并发场景**: 
  - 多用户同时编辑同一梗条目 → 后提交覆盖前提交（Last Write Wins）
  - 多用户同时绑定产品 → `bindProduct()` 有重复检查，安全

---

## 8. 可观测性

### 8.1 日志

- **日志框架**: SLF4J + Logback
- **日志级别**: INFO (生产), DEBUG (开发)
- **关键日志点**:
  - ❌ 无业务日志（建议添加）

**建议添加日志**:
```java
log.info("搜索梗条目: userId={}, keyword=, category={}", userId, keyword, category);
log.info("AI 生成梗: productId={}, count={}, result={}", productId, count, result.size());
log.warn("梗条目不存在: id={}", id);
```

### 8.2 监控

- **Trace ID**: ✅ 所有响应包含 `traceId` (MDC)
- **性能指标**: ❌ 无自定义 Metrics
- **健康检查**: ✅ Spring Boot Actuator

**建议添加 Metrics**:
- `slangdict.entry.search.duration` - 搜索耗时
- `slangdict.ai.generate.duration` - AI 生成耗时
- `slangdict.ai.generate.success_rate` - AI 生成成功率

### 8.3 追踪

- **分布式追踪**: ✅ OpenTelemetry 集成（项目级）
- **调用链**: Controller → Service → Repository → Database
- **外部调用**: LlmClient (AI 模型调用)

---

## 9. 架构评分

### 9.1 评分维度

| 维度 | 得分 | 满分 | 说明 |
|------|------|------|------|
| **分层清晰度** | 10 | 10 | Controller/Service/Repository/Entity 分层清晰 |
| **代码质量** | 9 | 10 | 代码规范，逻辑清晰，缺少日志 |
| **安全性** | 8 | 10 | 认证授权完善，缺少内容审核 |
| **性能** | 6 | 10 | 无缓存，存在优化空间 |
| **可扩展性** | 9 | 10 | 接口设计合理，易于扩展 |
| **可测试性** | 10 | 10 | 单元测试覆盖完整（12 个测试用例） |
| **可观测性** | 6 | 10 | 有 Trace ID，缺少日志和 Metrics |
| **文档完整性** | 9 | 10 | Swagger 注解完整，缺少业务文档 |
| **数据模型** | 9 | 10 | 表结构合理，索引完善 |
| **集成设计** | 8 | 10 | AI 集成良好，缺少违禁词检测 |

**总分**: 84 / 100

**等级**: B+ (良好)

### 9.2 优势

1. ✅ **架构清晰**: 标准的 Spring Boot 分层架构，职责明确
2. ✅ **数据隔离**: 用户级数据隔离实现完善
3. ✅ **AI 集成**: AI 生成梗功能设计合理，支持模型降级
4. ✅ **测试覆盖**: 单元测试覆盖所有 API 端点
5. ✅ **产品关联**: 梗与产品的多对多关联设计灵活
6. ✅ **话术集成**: `buildSlangContextForPrompt()` 为直播话术提供上下文

### 9.3 不足

1. ❌ **无缓存**: 热门梗、产品关联梗等高频查询无缓存
2. ❌ **无日志**: Service 层缺少业务日志
3. ❌ **无 Metrics**: 缺少性能监控指标
4. ❌ **无内容审核**: 梗内容未集成违禁词检测
5. ⚠️ **前后端不一致**: 前端 API 路径与后端不匹配（见 10.1）

---

## 10. 改进建议

### 10.1 P0 - 阻塞级（必须修复）

#### 10.1.1 前后端 API 路径不一致

**问题**: 前端调用路径与后端不匹配

| 前端路径 | 后端路径 | 状态 |
|----------|----------|------|
| `/slangdict/list` | `/api/v1/slangdict/entry/search` | ❌ 不匹配 |
| `/slangdict/save` | `/api/v1/slangdict/entry/save` | ❌ 不匹配 |
| `/slangdict/delete` | `/api/v1/slangdict/entry/delete` | ❌ 不匹配 |
| `/slangdict/enable` | ❌ 后端无此接口 | ❌ 缺失 |
| `/slangdict/disable` | ❌ 后端无此接口 | ❌ 缺失 |

**影响**: 前端功能完全不可用

**修复方案**:
1. 修改前端 `slangdict.ts` 路径为 `/api/v1/slangdict/entry/*`
2. 修改前端字段映射（term → phrase, definition → meaning）
3. 后端添加 `/enable` 和 `/disable` 接口，或前端改用 `/save` 接口更新 status

**文件**:
- `front/src/api/slangdict.ts`
- `front/src/pages/slangdict/SlangDictPage.tsx`

---

### 10.2 P1 - 高优先级（强烈建议）

#### 10.2.1 添加缓存层

**问题**: 高频查询无缓存，性能有优化空间

**方案**:
```java
@Cacheable(value = "slangdict:product", key = "#productId + ':' + #userId")
public List<SdEntryVO> getByProductId(Long productId, Long userId) {
    // ...
}

@CacheEvict(value = "slangdict:product", allEntries = true)
public void bindProduct(Long entryId, Long productId, Long userId) {
    // ...
}
```

**预期收益**: 产品关联梗查询性能提升 80%+

#### 10.2.2 添加业务日志

**问题**: Service 层无日志，问题排查困难

**方案**:
```java
log.info("搜索梗条目: userId={}, keyword={}, total={}", userId, vo.getKeyword(), page.getTotalElements());
log.info("AI 生成梗: productId={}, count={}, success={}", productId, count, result.size());
log.warn("梗条目不存在: id={}", id);
```

#### 10.2.3 集成违禁词检测

**问题**: 梗内容未审核，存在合规风险

**方案**:
```java
@Resource private ScriptComplianceService complianceService;

public Long save(SdEntrySaveVO vo, Long userId) {
    // 检测违禁词
    ComplianceCheckResult result = complianceService.check(vo.getPhrase() + " " + vo.getMeaning());
    if (!result.isPass()) {
        throw new BusinessException(ErrorCode.CONTENT_VIOLATION, "内容包含违禁词: " + result.getViolations());
    }
    // ...
}
```

---

### 10.3 P2 - 中优先级（建议优化）

#### 10.3.1 添加性能监控

**方案**:
```java
@Timed(value = "slangdict.search", description = "梗条目搜索耗时")
public PageResultVO<SdEntryVO> search(SdEntrySearchVO vo) {
    // ...
}

@Timed(value = "slangdict.ai.generate", description = "AI 生成梗耗时")
public List<String> aiGeneratePhrases(Long productId, Long userId, int count) {
    // ...
}
```

#### 10.3.2 优化 AI 生成提示词

**当前提示词**: 简单直接，但缺少行业特色

**优化方案**:
```java
String prompt = String.format("""
    为以下产品生成 %d 条创意暗语/梗：
    产品名称：%s
    产品描述：%s
    价格：%s元
    行业：护肤品/彩妆
    
    参考风格：
    - "给小弟准备一套别墅" → 推荐高端男士内裤
    - "今天给家人们安排个豪宅" → 推荐舒适内裤套装
    
    要求：
    1. 每行一条，格式："暗语" → 真实含义
    2. 有趣、有记忆点、适合直播场景
    3. 口语化，朗朗上口
    4. 不要违禁词、不要低俗
    5. 结合产品特点和价格定位
    
    直接输出，不要序号和额外说明。
    """, count, productName, description, price);
```

#### 10.3.3 添加使用统计

**方案**: 在话术生成时自动更新 `use_count`

```java
public void incrementUseCount(Long entryId) {
    entryRepository.findByIdAndDeleted(entryId, 0).ifPresent(entry -> {
        entry.setUseCount(entry.getUseCount() + 1);
        entryRepository.save(entry);
    });
}
```

---

### 10.4 P3 - 低优先级（可选优化）

#### 10.4.1 添加批量操作

**方案**: 支持批量导入、批量删除、批量绑定产品

```java
@PostMapping("/batch-save")
public RESTResult<Void> batchSave(@RequestBody List<SdEntrySaveVO> list) {
    // ...
}

@PostMapping("/batch-delete")
public RESTResult<Void> batchDelete(@RequestBody List<Long> ids) {
    // ...
}
```

#### 10.4.2 添加梗推荐

**方案**: 基于产品类别推荐相似产品的热门梗

```java
@PostMapping("/recommend")
public RESTResult<List<SdEntryVO>> recommend(@RequestParam Long productId) {
    // 1. 查询产品类别
    // 2. 查询同类别产品的高 use_count 梗
    // 3. 返回 Top 10
}
```

#### 10.4.3 添加梗效果分析

**方案**: 记录梗在直播中的使用效果（观看人数变化、互动量、转化率）

```java
@PostMapping("/effect-report")
public RESTResult<SlangEffectVO> effectReport(@RequestParam Long entryId) {
    // 查询使用该梗的直播场次
    // 统计平均观看人数、互动量、转化率
    // 与未使用该梗的场次对比
}
```

---

## 11. 总结

### 11.1 架构成熟度

slangdict 模块架构设计**良好**（B+ 级），具备以下特点：

- ✅ 分层清晰，职责明确
- ✅ 数据隔离完善
- ✅ AI 集成合理
- ✅ 测试覆盖完整
- ⚠️ 性能优化空间较大
- ⚠️ 可观测性待提升
- ❌ 前后端不一致（P0 问题）

### 11.2 生产就绪度

**当前状态**: ⚠️ 不建议直接上线

**阻塞问题**:
1. 前后端 API 路径不一致（P0）
2. 缺少缓存（P1）
3. 缺少违禁词检测（P1）

**上线前必须完成**:
1. 修复前后端 API 不一致问题
2. 添加缓存层
3. 集成违禁词检测
4. 添加业务日志

### 11.3 后续演进方向

1. **短期**（1-2 周）: 修复 P0/P1 问题，完善监控
2. **中期**（1-2 月）: 添加梗推荐、效果分析功能
3. **长期**（3-6 月）: 基于使用数据优化 AI 生成算法，建立行业梗库

---

**审查完成日期**: 2026-05-09  
**下次审查建议**: 2026-06-09（修复 P0/P1 问题后）

